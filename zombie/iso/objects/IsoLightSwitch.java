/*
 * Decompiled with CFR 0.152.
 */
package zombie.iso.objects;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import zombie.GameTime;
import zombie.GameWindow;
import zombie.SandboxOptions;
import zombie.UsedFromLua;
import zombie.characters.IsoGameCharacter;
import zombie.characters.IsoPlayer;
import zombie.core.Color;
import zombie.core.Core;
import zombie.core.math.PZMath;
import zombie.core.network.ByteBufferReader;
import zombie.core.network.ByteBufferWriter;
import zombie.core.properties.IsoPropertyType;
import zombie.core.raknet.UdpConnection;
import zombie.core.random.Rand;
import zombie.inventory.InventoryItem;
import zombie.inventory.InventoryItemFactory;
import zombie.inventory.types.DrainableComboItem;
import zombie.inventory.types.Moveable;
import zombie.iso.IsoCamera;
import zombie.iso.IsoCell;
import zombie.iso.IsoChunk;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoLightSource;
import zombie.iso.IsoObject;
import zombie.iso.IsoRoomLight;
import zombie.iso.IsoWorld;
import zombie.iso.LightingJNI;
import zombie.iso.RoomID;
import zombie.iso.areas.IsoRoom;
import zombie.iso.objects.IsoGenerator;
import zombie.iso.sprite.IsoSprite;
import zombie.network.GameClient;
import zombie.network.GameServer;
import zombie.network.PacketTypes;
import zombie.network.packets.INetworkPacket;

@UsedFromLua
public class IsoLightSwitch
extends IsoObject {
    public boolean activated;
    public final ArrayList<IsoLightSource> lights = new ArrayList();
    public boolean lightRoom;
    public long roomId = -1L;
    public boolean streetLight;
    private boolean canBeModified;
    private boolean useBattery;
    private boolean hasBattery;
    private String bulbItem = "Base.LightBulb";
    private float power;
    private float delta = 2.5E-4f;
    private float primaryR = 1.0f;
    private float primaryG = 1.0f;
    private float primaryB = 1.0f;
    private static final ArrayList<IsoObject> s_tempObjects = new ArrayList();
    protected long lastMinuteStamp = -1L;
    protected int bulbBurnMinutes = -1;
    protected int lastMin;
    protected int nextBreakUpdate = 60;

    @Override
    public String getObjectName() {
        return "LightSwitch";
    }

    public IsoLightSwitch(IsoCell cell) {
        super(cell);
    }

    public IsoLightSwitch(IsoCell cell, IsoGridSquare sq, IsoSprite gid, long roomId) {
        super(cell, sq, gid);
        this.roomId = roomId;
        if (gid != null && gid.getProperties().has(IsoPropertyType.RED_LIGHT)) {
            if (gid.getProperties().has("IsMoveAble")) {
                this.canBeModified = true;
            }
            this.primaryR = Float.parseFloat(gid.getProperties().get(IsoPropertyType.RED_LIGHT)) / 255.0f;
            this.primaryG = Float.parseFloat(gid.getProperties().get(IsoPropertyType.GREEN_LIGHT)) / 255.0f;
            this.primaryB = Float.parseFloat(gid.getProperties().get(IsoPropertyType.BLUE_LIGHT)) / 255.0f;
        } else {
            this.lightRoom = true;
        }
        this.streetLight = gid != null && gid.getProperties().has("streetlight");
        IsoRoom room = this.square.getRoom();
        if (room != null && this.lightRoom) {
            if (!sq.haveElectricity() && !sq.hasGridPower()) {
                room.def.lightsActive = false;
            }
            this.activated = room.def.lightsActive;
            room.lightSwitches.add(this);
        } else {
            this.activated = true;
        }
    }

    public void addLightSourceFromSprite() {
        if (this.sprite != null && this.sprite.getProperties().has(IsoPropertyType.RED_LIGHT)) {
            float r = Float.parseFloat(this.sprite.getProperties().get(IsoPropertyType.RED_LIGHT)) / 255.0f;
            float g = Float.parseFloat(this.sprite.getProperties().get(IsoPropertyType.GREEN_LIGHT)) / 255.0f;
            float b = Float.parseFloat(this.sprite.getProperties().get(IsoPropertyType.BLUE_LIGHT)) / 255.0f;
            this.activated = false;
            this.setActive(true, true);
            int radius = 10;
            if (this.sprite.getProperties().has(IsoPropertyType.LIGHT_RADIUS) && Integer.parseInt(this.sprite.getProperties().get(IsoPropertyType.LIGHT_RADIUS)) > 0) {
                radius = Integer.parseInt(this.sprite.getProperties().get(IsoPropertyType.LIGHT_RADIUS));
            }
            IsoLightSource l = new IsoLightSource(this.square.getX(), this.square.getY(), this.square.getZ(), r, g, b, radius);
            l.active = this.activated;
            l.hydroPowered = true;
            l.switches.add(this);
            this.lights.add(l);
        }
    }

    public boolean getCanBeModified() {
        return this.canBeModified;
    }

    public void setCanBeModified(boolean val) {
        this.canBeModified = val;
    }

    public float getPower() {
        return this.power;
    }

    public void setPower(float power) {
        this.power = power;
    }

    public void setDelta(float delta) {
        this.delta = delta;
    }

    public float getDelta() {
        return this.delta;
    }

    public void setUseBattery(boolean b) {
        this.setActive(false);
        this.useBattery = b;
        if (GameServer.server) {
            this.syncCustomizedSettings(null);
        }
    }

    public void setUseBatteryDirect(boolean b) {
        this.useBattery = b;
    }

    public boolean getUseBattery() {
        return this.useBattery;
    }

    public boolean getHasBattery() {
        return this.hasBattery;
    }

    public void setHasBattery(boolean val) {
        this.hasBattery = val;
    }

    public void setHasBatteryRaw(boolean b) {
        this.hasBattery = b;
    }

    public void addBattery(IsoGameCharacter chr, InventoryItem battery) {
        if (this.canBeModified && this.useBattery && !this.hasBattery && battery != null && battery.getFullType().equals("Base.Battery")) {
            this.power = battery.getCurrentUsesFloat();
            this.hasBattery = true;
            chr.removeFromHands(battery);
            chr.getInventory().Remove(battery);
            if (GameServer.server) {
                GameServer.sendRemoveItemFromContainer(chr.getInventory(), battery);
                this.syncCustomizedSettings(null);
            }
        }
    }

    public DrainableComboItem removeBattery(IsoGameCharacter chr) {
        DrainableComboItem battery;
        if (this.canBeModified && this.useBattery && this.hasBattery && (battery = (DrainableComboItem)InventoryItemFactory.CreateItem("Base.Battery")) != null) {
            this.hasBattery = false;
            battery.setCurrentUses(this.power >= 0.0f ? (int)((float)battery.getMaxUses() * this.power) : 0);
            this.power = 0.0f;
            this.setActive(false, false, true);
            chr.getInventory().AddItem(battery);
            if (GameServer.server) {
                GameServer.sendAddItemToContainer(chr.getInventory(), battery);
                this.syncCustomizedSettings(null);
            }
            return battery;
        }
        return null;
    }

    public boolean hasLightBulb() {
        return this.bulbItem != null;
    }

    public String getBulbItem() {
        return this.bulbItem;
    }

    public void setBulbItemRaw(String item) {
        this.bulbItem = item;
    }

    public void addLightBulb(IsoGameCharacter chr, InventoryItem bulb) {
        IsoLightSource light;
        if (!this.hasLightBulb() && bulb != null && bulb.getType().startsWith("LightBulb") && (light = this.getPrimaryLight()) != null) {
            this.setPrimaryR(bulb.getColorRed());
            this.setPrimaryG(bulb.getColorGreen());
            this.setPrimaryB(bulb.getColorBlue());
            this.bulbItem = bulb.getFullType();
            chr.removeFromHands(bulb);
            chr.getInventory().Remove(bulb);
            if (GameServer.server) {
                GameServer.sendRemoveItemFromContainer(chr.getInventory(), bulb);
                this.syncCustomizedSettings(null);
            }
        }
    }

    public InventoryItem removeLightBulb(IsoGameCharacter chr) {
        Object bulb;
        IsoLightSource light = this.getPrimaryLight();
        if (light != null && this.hasLightBulb() && (bulb = InventoryItemFactory.CreateItem(this.bulbItem)) != null) {
            ((InventoryItem)bulb).setColorRed(this.getPrimaryR());
            ((InventoryItem)bulb).setColorGreen(this.getPrimaryG());
            ((InventoryItem)bulb).setColorBlue(this.getPrimaryB());
            ((InventoryItem)bulb).setColor(new Color(light.r, light.g, light.b));
            this.bulbItem = null;
            chr.getInventory().AddItem((InventoryItem)bulb);
            if (GameServer.server) {
                GameServer.sendAddItemToContainer(chr.getInventory(), bulb);
            }
            this.setActive(false, false, true);
            if (GameServer.server) {
                this.syncCustomizedSettings(null);
            }
            return bulb;
        }
        return null;
    }

    private IsoLightSource getPrimaryLight() {
        if (!this.lights.isEmpty()) {
            return this.lights.get(0);
        }
        return null;
    }

    public float getPrimaryR() {
        return this.getPrimaryLight() != null ? this.getPrimaryLight().r : this.primaryR;
    }

    public float getPrimaryG() {
        return this.getPrimaryLight() != null ? this.getPrimaryLight().g : this.primaryG;
    }

    public float getPrimaryB() {
        return this.getPrimaryLight() != null ? this.getPrimaryLight().b : this.primaryB;
    }

    public void setPrimaryR(float r) {
        this.primaryR = r;
        if (this.getPrimaryLight() != null) {
            this.getPrimaryLight().r = r;
        }
    }

    public void setPrimaryG(float g) {
        this.primaryG = g;
        if (this.getPrimaryLight() != null) {
            this.getPrimaryLight().g = g;
        }
    }

    public void setPrimaryB(float b) {
        this.primaryB = b;
        if (this.getPrimaryLight() != null) {
            this.getPrimaryLight().b = b;
        }
    }

    @Override
    public void load(ByteBuffer input, int worldVersion, boolean isDebugSave) throws IOException {
        IsoRoom room;
        super.load(input, worldVersion, isDebugSave);
        boolean bl = this.lightRoom = input.get() != 0;
        if (worldVersion >= 206) {
            this.roomId = input.getLong();
        } else {
            int roomIndex = input.getInt();
            this.roomId = RoomID.makeID(this.square.x / 256, this.square.y / 256, roomIndex);
        }
        this.activated = input.get() != 0;
        boolean bl2 = this.canBeModified = input.get() != 0;
        if (this.canBeModified) {
            this.useBattery = input.get() != 0;
            this.hasBattery = input.get() != 0;
            this.bulbItem = input.get() != 0 ? GameWindow.ReadString(input) : null;
            this.power = input.getFloat();
            this.delta = input.getFloat();
            this.setPrimaryR(input.getFloat());
            this.setPrimaryG(input.getFloat());
            this.setPrimaryB(input.getFloat());
        }
        this.lastMinuteStamp = input.getLong();
        this.bulbBurnMinutes = input.getInt();
        boolean bl3 = this.streetLight = this.sprite != null && this.sprite.getProperties().has("streetlight");
        if (this.square == null) {
            return;
        }
        if (GameClient.client) {
            this.switchLight(this.activated);
        }
        if ((room = this.square.getRoom()) != null && this.lightRoom) {
            this.activated = room.def.lightsActive;
            room.lightSwitches.add(this);
        } else {
            float r = 0.9f;
            float g = 0.8f;
            float b = 0.7f;
            if (this.sprite != null && this.sprite.getProperties().has(IsoPropertyType.RED_LIGHT)) {
                if (this.canBeModified) {
                    r = this.primaryR;
                    g = this.primaryG;
                    b = this.primaryB;
                } else {
                    r = Float.parseFloat(this.sprite.getProperties().get(IsoPropertyType.RED_LIGHT)) / 255.0f;
                    g = Float.parseFloat(this.sprite.getProperties().get(IsoPropertyType.GREEN_LIGHT)) / 255.0f;
                    b = Float.parseFloat(this.sprite.getProperties().get(IsoPropertyType.BLUE_LIGHT)) / 255.0f;
                    this.primaryR = r;
                    this.primaryG = g;
                    this.primaryB = b;
                }
            }
            int radius = 8;
            if (this.sprite.getProperties().has(IsoPropertyType.LIGHT_RADIUS) && Integer.parseInt(this.sprite.getProperties().get(IsoPropertyType.LIGHT_RADIUS)) > 0) {
                radius = Integer.parseInt(this.sprite.getProperties().get(IsoPropertyType.LIGHT_RADIUS));
            }
            IsoLightSource l = new IsoLightSource(this.getXi(), this.getYi(), this.getZi(), r, g, b, radius);
            l.wasActive = l.active = this.activated;
            l.hydroPowered = true;
            l.switches.add(this);
            this.lights.add(l);
        }
    }

    @Override
    public void save(ByteBuffer output, boolean isDebugSave) throws IOException {
        super.save(output, isDebugSave);
        output.put(this.lightRoom ? (byte)1 : 0);
        output.putLong(this.roomId);
        output.put(this.activated ? (byte)1 : 0);
        output.put(this.canBeModified ? (byte)1 : 0);
        if (this.canBeModified) {
            output.put(this.useBattery ? (byte)1 : 0);
            output.put(this.hasBattery ? (byte)1 : 0);
            output.put(this.hasLightBulb() ? (byte)1 : 0);
            if (this.hasLightBulb()) {
                GameWindow.WriteString(output, this.bulbItem);
            }
            output.putFloat(this.power);
            output.putFloat(this.delta);
            output.putFloat(this.getPrimaryR());
            output.putFloat(this.getPrimaryG());
            output.putFloat(this.getPrimaryB());
        }
        output.putLong(this.lastMinuteStamp);
        output.putInt(this.bulbBurnMinutes);
    }

    @Override
    public boolean onMouseLeftClick(int x, int y) {
        return false;
    }

    public boolean canSwitchLight() {
        if (this.bulbItem != null) {
            boolean electricityAround = this.hasElectricityAround();
            if (!this.useBattery && electricityAround || this.canBeModified && this.useBattery && this.hasBattery && this.power > 0.0f) {
                return true;
            }
        }
        return false;
    }

    private boolean hasElectricityAround() {
        boolean electricityAround;
        if (this.getObjectIndex() == -1) {
            return false;
        }
        boolean isPreElecShut = this.hasGridPower();
        boolean bl = isPreElecShut ? this.isBuildingSquare(this.square) || this.hasFasciaAdjacentToBuildingSquare(this.square) || this.streetLight : (electricityAround = this.square.haveElectricity());
        if (!electricityAround && this.getCell() != null) {
            for (int dz = 0; dz >= -1 && this.getSquare().getZ() + dz >= -32; --dz) {
                for (int dx = -1; dx < 2; ++dx) {
                    for (int dy = -1; dy < 2; ++dy) {
                        IsoGridSquare gs;
                        if (dx == 0 && dy == 0 && dz == 0 || (gs = this.getCell().getGridSquare(this.getSquare().getX() + dx, this.getSquare().getY() + dy, this.getSquare().getZ() + dz)) == null) continue;
                        if (isPreElecShut && (this.isBuildingSquare(gs) || this.hasFasciaAdjacentToBuildingSquare(gs))) {
                            return true;
                        }
                        if (!gs.haveElectricity()) continue;
                        return true;
                    }
                }
            }
        }
        return electricityAround;
    }

    private boolean isBuildingSquare(IsoGridSquare square) {
        if (square == null) {
            return false;
        }
        if (square.getRoom() != null) {
            return true;
        }
        return square.getRoofHideBuilding() != null;
    }

    private boolean hasFasciaAdjacentToBuildingSquare(IsoGridSquare square) {
        IsoObject[] objects = square.getObjects().getElements();
        int n = square.getObjects().size();
        for (int i = 0; i < n; ++i) {
            IsoObject object = objects[i];
            if (!object.isFascia()) continue;
            IsoGridSquare square1 = object.getFasciaAttachedSquare();
            return this.isBuildingSquare(square1);
        }
        return false;
    }

    public boolean setActive(boolean active) {
        return this.setActive(active, false, false);
    }

    public boolean setActive(boolean active, boolean setActiveBoolOnly) {
        return this.setActive(active, setActiveBoolOnly, false);
    }

    public boolean setActive(boolean active, boolean setActiveBoolOnly, boolean ignoreSwitchCheck) {
        if (this.bulbItem == null) {
            active = false;
        }
        if (active == this.activated) {
            return this.activated;
        }
        if (this.square.getRoom() == null && !this.canBeModified) {
            return this.activated;
        }
        if (ignoreSwitchCheck || this.canSwitchLight()) {
            this.activated = active;
            if (!setActiveBoolOnly) {
                this.switchLight(this.activated);
                LightingJNI.doInvalidateGlobalLights(IsoPlayer.getPlayerIndex());
                this.syncIsoObject(false, this.activated ? (byte)1 : 0, null);
            }
        }
        return this.activated;
    }

    public boolean toggle() {
        return this.setActive(!this.activated);
    }

    public void switchLight(boolean activated) {
        int i;
        if (this.lightRoom && this.square.getRoom() != null) {
            this.square.getRoom().def.lightsActive = activated;
            for (i = 0; i < this.square.getRoom().lightSwitches.size(); ++i) {
                this.square.getRoom().lightSwitches.get((int)i).activated = activated;
            }
            if (GameServer.server) {
                int cellY;
                long roomID = this.square.getRoom().def.id;
                int cellX = PZMath.fastfloor(this.square.getX() / 256);
                if (!RoomID.isSameCell(roomID, cellX, cellY = PZMath.fastfloor(this.square.getY() / 256))) {
                    GameServer.sendMetaGrid(RoomID.getCellX(roomID), RoomID.getCellY(roomID), RoomID.getIndex(roomID));
                } else {
                    GameServer.sendMetaGrid(cellX, cellY, RoomID.getIndex(roomID));
                }
            }
        }
        for (int n = 0; n < this.lights.size(); ++n) {
            IsoLightSource s = this.lights.get(n);
            s.active = activated;
        }
        this.getSpriteGridObjects(s_tempObjects);
        if (!s_tempObjects.isEmpty()) {
            for (i = 0; i < s_tempObjects.size(); ++i) {
                IsoObject object = s_tempObjects.get(i);
                if (object == this) continue;
                if (object instanceof IsoLightSwitch) {
                    IsoLightSwitch lightSwitch = (IsoLightSwitch)object;
                    if (lightSwitch.isActivated() == activated) continue;
                    lightSwitch.setActive(activated);
                    continue;
                }
                if (object.getLightSource() == null) continue;
                object.checkLightSourceActive();
            }
        }
        if (!GameServer.server) {
            IsoGridSquare.recalcLightTime = -1.0f;
            ++Core.dirtyGlobalLightsCount;
            GameTime.instance.lightSourceUpdate = 100.0f;
            LightingJNI.doInvalidateGlobalLights(IsoPlayer.getPlayerIndex());
            if (this.hasAnimatedAttachments()) {
                this.invalidateRenderChunkLevel(1024L);
            }
        }
        IsoGenerator.updateGenerator(this.getSquare());
    }

    public void getCustomSettingsFromItem(InventoryItem item) {
        Moveable i;
        if (item instanceof Moveable && (i = (Moveable)item).isLight()) {
            this.useBattery = i.isLightUseBattery();
            this.hasBattery = i.isLightHasBattery();
            this.bulbItem = i.getLightBulbItem();
            this.power = i.getLightPower();
            this.delta = i.getLightDelta();
            this.setPrimaryR(i.getLightR());
            this.setPrimaryG(i.getLightG());
            this.setPrimaryB(i.getLightB());
        }
    }

    public void setCustomSettingsToItem(InventoryItem item) {
        if (item instanceof Moveable) {
            Moveable i = (Moveable)item;
            i.setLightUseBattery(this.useBattery);
            i.setLightHasBattery(this.hasBattery);
            i.setLightBulbItem(this.bulbItem);
            i.setLightPower(this.power);
            i.setLightDelta(this.delta);
            i.setLightR(this.primaryR);
            i.setLightG(this.primaryG);
            i.setLightB(this.primaryB);
        }
    }

    public void syncCustomizedSettings(UdpConnection source2) {
        if (GameClient.client) {
            INetworkPacket.send(PacketTypes.PacketType.SyncCustomLightSettings, this);
        } else if (GameServer.server) {
            INetworkPacket.sendToAll(PacketTypes.PacketType.SyncCustomLightSettings, source2, this);
        }
    }

    @Override
    public void syncIsoObjectSend(ByteBufferWriter b) {
        b.putInt(this.square.getX());
        b.putInt(this.square.getY());
        b.putInt(this.square.getZ());
        b.putByte(this.square.getObjects().indexOf(this));
        b.putBoolean(true);
        b.putBoolean(this.activated);
    }

    @Override
    public void syncIsoObject(boolean bRemote, byte val, UdpConnection source2, ByteBufferReader bb) {
        this.syncIsoObject(bRemote, val, source2);
    }

    public void syncIsoObject(boolean bRemote, byte val, UdpConnection source2) {
        if (this.square == null) {
            System.out.println("ERROR: " + this.getClass().getSimpleName() + " square is null");
            return;
        }
        if (this.getObjectIndex() == -1) {
            System.out.println("ERROR: " + this.getClass().getSimpleName() + " not found on square " + this.square.getX() + "," + this.square.getY() + "," + this.square.getZ());
            return;
        }
        if (GameServer.server) {
            for (UdpConnection connection : GameServer.udpEngine.connections) {
                ByteBufferWriter b;
                if (source2 != null) {
                    if (connection.getConnectedGUID() == source2.getConnectedGUID()) continue;
                    b = connection.startPacket();
                    PacketTypes.PacketType.SyncIsoObject.doPacket(b);
                    this.syncIsoObjectSend(b);
                    PacketTypes.PacketType.SyncIsoObject.send(connection);
                    continue;
                }
                if (!connection.isRelevantTo(this.square.x, this.square.y)) continue;
                b = connection.startPacket();
                PacketTypes.PacketType.SyncIsoObject.doPacket(b);
                b.putInt(this.square.getX());
                b.putInt(this.square.getY());
                b.putInt(this.square.getZ());
                int i = this.square.getObjects().indexOf(this);
                if (i != -1) {
                    b.putByte(i);
                } else {
                    b.putByte(this.square.getObjects().size());
                }
                b.putBoolean(true);
                b.putBoolean(this.activated);
                PacketTypes.PacketType.SyncIsoObject.send(connection);
            }
        } else if (GameClient.client && !bRemote) {
            ByteBufferWriter b = GameClient.connection.startPacket();
            PacketTypes.PacketType.SyncIsoObject.doPacket(b);
            this.syncIsoObjectSend(b);
            PacketTypes.PacketType.SyncIsoObject.send(GameClient.connection);
        } else if (bRemote) {
            if (val == 1) {
                this.switchLight(true);
                this.activated = true;
            } else {
                this.switchLight(false);
                this.activated = false;
            }
        }
        this.flagForHotSave();
    }

    @Override
    public void update() {
        if (GameServer.server || !GameClient.client) {
            boolean serverUpdateClients = false;
            if (!this.activated) {
                this.lastMinuteStamp = -1L;
            }
            if (!this.lightRoom && this.canBeModified && this.activated) {
                if (this.lastMinuteStamp == -1L) {
                    this.lastMinuteStamp = GameTime.instance.getMinutesStamp();
                }
                if (GameTime.instance.getMinutesStamp() > this.lastMinuteStamp) {
                    double bulbLife;
                    boolean hasBatteryPower;
                    if (this.bulbBurnMinutes == -1) {
                        int elecDays = SandboxOptions.instance.getElecShutModifier() * 24 * 60;
                        if (this.square.isNoPower()) {
                            elecDays = 0;
                        }
                        this.bulbBurnMinutes = this.lastMinuteStamp < (long)elecDays ? (int)this.lastMinuteStamp : elecDays;
                    }
                    long diff = GameTime.instance.getMinutesStamp() - this.lastMinuteStamp;
                    this.lastMinuteStamp = GameTime.instance.getMinutesStamp();
                    boolean doLightBulbUpdate = false;
                    boolean electricityAround = this.hasElectricityAround();
                    boolean bl = hasBatteryPower = this.useBattery && this.hasBattery && this.power > 0.0f;
                    if (hasBatteryPower || !this.useBattery && electricityAround) {
                        doLightBulbUpdate = true;
                    }
                    if ((bulbLife = SandboxOptions.instance.lightBulbLifespan.getValue()) <= 0.0) {
                        doLightBulbUpdate = false;
                    }
                    if (this.activated && this.hasLightBulb() && doLightBulbUpdate) {
                        this.bulbBurnMinutes = (int)((long)this.bulbBurnMinutes + diff);
                    }
                    this.nextBreakUpdate = (int)((long)this.nextBreakUpdate - diff);
                    if (this.nextBreakUpdate <= 0) {
                        if (this.activated && this.hasLightBulb() && doLightBulbUpdate) {
                            int thresh;
                            int rand;
                            int multiplier = (int)(1000.0 * bulbLife);
                            if (multiplier < 1) {
                                multiplier = 1;
                            }
                            if ((rand = Rand.Next(0, multiplier)) < (thresh = this.bulbBurnMinutes / 10000)) {
                                this.bulbBurnMinutes = 0;
                                this.setActive(false, true, true);
                                this.bulbItem = null;
                                IsoWorld.instance.getFreeEmitter().playSound("LightbulbBurnedOut", this.square);
                                serverUpdateClients = true;
                                if (Core.debug) {
                                    System.out.println("broke bulb at x=" + this.getX() + ", y=" + this.getY() + ", z=" + this.getZ());
                                }
                            }
                        }
                        this.nextBreakUpdate = 60;
                    }
                    if (this.activated && hasBatteryPower && this.hasLightBulb()) {
                        float pmod = this.power - this.power % 0.01f;
                        this.power -= this.delta * (float)diff;
                        if (this.power < 0.0f) {
                            this.power = 0.0f;
                        }
                        if (diff == 1L || this.power < pmod) {
                            serverUpdateClients = true;
                        }
                    }
                }
                if (this.useBattery && this.activated && (this.power <= 0.0f || !this.hasBattery)) {
                    this.power = 0.0f;
                    this.setActive(false, true, true);
                    serverUpdateClients = true;
                }
            }
            if (this.activated && !this.hasLightBulb()) {
                this.setActive(false, true, true);
                serverUpdateClients = true;
            }
            if (serverUpdateClients && GameServer.server) {
                this.syncCustomizedSettings(null);
            }
        }
    }

    public boolean isActivated() {
        return this.activated;
    }

    public void setActivated(boolean val) {
        this.activated = val;
    }

    @Override
    public void addToWorld() {
        if (!this.activated) {
            this.lastMinuteStamp = -1L;
        }
        if (!this.lightRoom && !this.lights.isEmpty()) {
            for (int i = 0; i < this.lights.size(); ++i) {
                IsoWorld.instance.currentCell.getLamppostPositions().add(this.lights.get(i));
            }
        }
        if (this.getCell() != null && this.canBeModified && !this.lightRoom && (!GameServer.server && !GameClient.client || GameServer.server)) {
            this.getCell().addToStaticUpdaterObjectList(this);
        }
        this.checkAmbientSound();
    }

    @Override
    public void removeFromWorld() {
        IsoRoom room;
        if (!this.lightRoom && !this.lights.isEmpty()) {
            for (int i = 0; i < this.lights.size(); ++i) {
                this.lights.get(i).setActive(false);
                IsoWorld.instance.currentCell.removeLamppost(this.lights.get(i));
            }
            this.lights.clear();
        }
        if (this.square != null && this.lightRoom && (room = this.square.getRoom()) != null) {
            room.lightSwitches.remove(this);
        }
        this.clearOnOverlay();
        super.removeFromWorld();
    }

    public static void chunkLoaded(IsoChunk chunk) {
        ArrayList<IsoRoom> rooms = new ArrayList<IsoRoom>();
        for (int x = 0; x < 8; ++x) {
            for (int y = 0; y < 8; ++y) {
                for (int z = chunk.minLevel; z <= chunk.maxLevel; ++z) {
                    IsoRoom r;
                    IsoGridSquare sq = chunk.getGridSquare(x, y, z);
                    if (sq == null || (r = sq.getRoom()) == null || !r.hasLightSwitches() || rooms.contains(r)) continue;
                    rooms.add(r);
                }
            }
        }
        for (int i = 0; i < rooms.size(); ++i) {
            IsoRoom room = (IsoRoom)rooms.get(i);
            room.createLights(room.def.lightsActive);
            for (int j = 0; j < room.roomLights.size(); ++j) {
                IsoRoomLight roomLight = room.roomLights.get(j);
                if (chunk.roomLights.contains(roomLight)) continue;
                chunk.roomLights.add(roomLight);
            }
        }
    }

    public ArrayList<IsoLightSource> getLights() {
        return this.lights;
    }

    @Override
    public boolean shouldShowOnOverlay() {
        boolean bShowOverlay;
        int playerIndex = IsoCamera.frameState.playerIndex;
        if (this.getSquare() == null || !this.getSquare().isSeen(playerIndex)) {
            return false;
        }
        boolean electricityAround = this.hasElectricityAround();
        boolean hasBatteryPower = this.useBattery && this.hasBattery && this.power > 0.0f;
        boolean bl = bShowOverlay = this.isActivated() && (hasBatteryPower || !this.useBattery && electricityAround);
        if (this.lightRoom) {
            boolean bl2 = bShowOverlay = !bShowOverlay && electricityAround;
        }
        if (this.streetLight && (GameTime.getInstance().getNight() < 0.5f || !this.hasGridPower())) {
            bShowOverlay = false;
        }
        return bShowOverlay;
    }
}

