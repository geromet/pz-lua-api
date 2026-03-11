/*
 * Decompiled with CFR 0.152.
 */
package zombie.vehicles;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import se.krka.kahlua.vm.KahluaTable;
import zombie.GameWindow;
import zombie.Lua.LuaEventManager;
import zombie.Lua.LuaManager;
import zombie.SandboxOptions;
import zombie.UsedFromLua;
import zombie.characters.IsoGameCharacter;
import zombie.characters.IsoPlayer;
import zombie.chat.ChatElement;
import zombie.chat.ChatElementOwner;
import zombie.core.math.PZMath;
import zombie.core.random.Rand;
import zombie.entity.GameEntity;
import zombie.entity.GameEntityType;
import zombie.inventory.InventoryItem;
import zombie.inventory.InventoryItemFactory;
import zombie.inventory.ItemContainer;
import zombie.inventory.types.Drainable;
import zombie.iso.IsoGridSquare;
import zombie.network.GameClient;
import zombie.network.GameServer;
import zombie.radio.ZomboidRadio;
import zombie.radio.devices.DeviceData;
import zombie.radio.devices.WaveSignalDevice;
import zombie.scripting.objects.VehicleScript;
import zombie.ui.UIFont;
import zombie.util.StringUtils;
import zombie.vehicles.BaseVehicle;
import zombie.vehicles.VehicleDoor;
import zombie.vehicles.VehicleLight;
import zombie.vehicles.VehicleType;
import zombie.vehicles.VehicleWindow;

@UsedFromLua
public final class VehiclePart
extends GameEntity
implements ChatElementOwner,
WaveSignalDevice {
    protected BaseVehicle vehicle;
    protected boolean created;
    protected String partId;
    protected VehicleScript.Part scriptPart;
    protected ItemContainer container;
    protected InventoryItem item;
    private KahluaTable modData;
    private float lastUpdated = -1.0f;
    protected short updateFlags;
    protected VehiclePart parent;
    protected VehicleDoor door;
    protected VehicleWindow window;
    protected ArrayList<VehiclePart> children;
    protected String category;
    protected int condition = -1;
    protected boolean specificItem = true;
    private float wheelFriction;
    private int mechanicSkillInstaller;
    private float suspensionDamping;
    private float suspensionCompression;
    private float engineLoudness;
    private float durability;
    protected VehicleLight light;
    protected DeviceData deviceData;
    protected ChatElement chatElement;
    private boolean hasPlayerInRange;

    public VehiclePart(BaseVehicle vehicle) {
        this.vehicle = vehicle;
    }

    public BaseVehicle getVehicle() {
        return this.vehicle;
    }

    public void setScriptPart(VehicleScript.Part scriptPart) {
        this.scriptPart = scriptPart;
    }

    public VehicleScript.Part getScriptPart() {
        return this.scriptPart;
    }

    public ItemContainer getItemContainer() {
        return this.container;
    }

    public void setItemContainer(ItemContainer container) {
        if (container != null) {
            container.parent = this.getVehicle();
            container.vehiclePart = this;
        }
        this.container = container;
    }

    public boolean hasModData() {
        return this.modData != null && !this.modData.isEmpty();
    }

    public KahluaTable getModData() {
        if (this.modData == null) {
            this.modData = LuaManager.platform.newTable();
        }
        return this.modData;
    }

    public float getLastUpdated() {
        return this.lastUpdated;
    }

    public void setLastUpdated(float hours) {
        this.lastUpdated = hours;
    }

    public String getId() {
        if (this.scriptPart == null) {
            return this.partId;
        }
        return this.scriptPart.id;
    }

    public int getIndex() {
        return this.vehicle.parts.indexOf(this);
    }

    public String getArea() {
        if (this.scriptPart == null) {
            return null;
        }
        return this.scriptPart.area;
    }

    public ArrayList<String> getItemType() {
        if (this.scriptPart == null) {
            return null;
        }
        return this.scriptPart.itemType;
    }

    public KahluaTable getTable(String id) {
        if (this.scriptPart == null || this.scriptPart.tables == null) {
            return null;
        }
        KahluaTable table = this.scriptPart.tables.get(id);
        if (table == null) {
            return null;
        }
        return LuaManager.copyTable(table);
    }

    public <T extends InventoryItem> T getInventoryItem() {
        return (T)this.item;
    }

    public void setInventoryItem(InventoryItem item, int mechanicSkill) {
        this.item = item;
        this.doInventoryItemStats(item, mechanicSkill);
        this.getVehicle().updateTotalMass();
        this.getVehicle().doDamageOverlay = true;
        if (this.isSetAllModelsVisible()) {
            this.setAllModelsVisible(item != null);
        }
        this.getVehicle().updatePartStats();
        if (!GameServer.server) {
            this.getVehicle().updateBulletStats();
        }
    }

    public void setInventoryItem(InventoryItem item) {
        this.setInventoryItem(item, 0);
    }

    public boolean isInventoryItemUninstalled() {
        return this.getItemType() != null && !this.getItemType().isEmpty() && this.getInventoryItem() == null;
    }

    public boolean isSetAllModelsVisible() {
        return this.scriptPart != null && this.scriptPart.setAllModelsVisible;
    }

    public void setAllModelsVisible(boolean visible) {
        if (this.scriptPart == null || this.scriptPart.models == null || this.scriptPart.models.isEmpty()) {
            return;
        }
        for (int i = 0; i < this.scriptPart.models.size(); ++i) {
            VehicleScript.Model scriptModel = this.scriptPart.models.get(i);
            this.vehicle.setModelVisible(this, scriptModel, visible);
        }
    }

    public void doInventoryItemStats(InventoryItem newItem, int mechanicSkill) {
        if (newItem != null) {
            if (this.isContainer()) {
                if (newItem.getMaxCapacity() > 0 && this.getScriptPart().container.conditionAffectsCapacity) {
                    this.setContainerCapacity((int)VehiclePart.getNumberByCondition(newItem.getMaxCapacity(), newItem.getCondition(), 5.0f));
                } else if (newItem.getMaxCapacity() > 0) {
                    this.setContainerCapacity(newItem.getMaxCapacity());
                }
                this.setContainerContentAmount(newItem.getItemCapacity());
            }
            this.setSuspensionCompression(VehiclePart.getNumberByCondition(newItem.getSuspensionCompression(), newItem.getCondition(), 0.6f));
            this.setSuspensionDamping(VehiclePart.getNumberByCondition(newItem.getSuspensionDamping(), newItem.getCondition(), 0.6f));
            if (newItem.getEngineLoudness() > 0.0f) {
                this.setEngineLoudness(VehiclePart.getNumberByCondition(newItem.getEngineLoudness(), newItem.getCondition(), 10.0f));
            }
            this.setCondition(newItem.getCondition());
            this.setMechanicSkillInstaller(mechanicSkill);
            if (newItem.getDurability() > 0.0f) {
                this.setDurability(newItem.getDurability());
            }
        } else {
            if (this.scriptPart != null && this.scriptPart.container != null) {
                if (this.scriptPart.container.capacity > 0) {
                    this.setContainerCapacity(this.scriptPart.container.capacity);
                } else {
                    this.setContainerCapacity(0);
                }
            }
            this.setMechanicSkillInstaller(0);
            this.setContainerContentAmount(0.0f);
            this.setSuspensionCompression(0.0f);
            this.setSuspensionDamping(0.0f);
            this.setWheelFriction(0.0f);
            this.setEngineLoudness(0.0f);
        }
    }

    public void setRandomCondition(InventoryItem item) {
        VehicleType type = VehicleType.getTypeFromName(this.getVehicle().getVehicleType());
        if (this.getVehicle().isGoodCar()) {
            int max = 100;
            if (item != null) {
                max = item.getConditionMax();
            }
            this.setCondition(Rand.Next(max - max / 3, max));
            if (item != null) {
                item.setCondition(this.getCondition(), false);
            }
            return;
        }
        int max = 100;
        if (item != null) {
            max = item.getConditionMax();
        }
        if (type != null) {
            max = PZMath.fastfloor((float)max * type.getRandomBaseVehicleQuality());
        }
        float cond = 100.0f;
        if (item != null) {
            int chanceToSpawnDamaged = item.getChanceToSpawnDamaged();
            if (type != null) {
                chanceToSpawnDamaged += type.chanceToPartDamage;
            }
            if (chanceToSpawnDamaged > 0 && Rand.Next(100) < chanceToSpawnDamaged) {
                cond = Rand.Next(max - max / 2, max);
            }
        } else {
            int chanceToSpawnDamaged = 30;
            if (type != null) {
                chanceToSpawnDamaged += type.chanceToPartDamage;
            }
            if (Rand.Next(100) < chanceToSpawnDamaged) {
                cond = Rand.Next((float)max * 0.5f, (float)max);
            }
        }
        switch (SandboxOptions.instance.carGeneralCondition.getValue()) {
            case 1: {
                cond -= Rand.Next(cond * 0.3f, Rand.Next(cond * 0.3f, cond * 0.9f));
                break;
            }
            case 2: {
                cond -= Rand.Next(cond * 0.1f, cond * 0.3f);
                break;
            }
            case 4: {
                cond += Rand.Next(cond * 0.2f, cond * 0.4f);
                break;
            }
            case 5: {
                cond += Rand.Next(cond * 0.5f, cond * 0.9f);
            }
        }
        cond = Math.max(0.0f, cond);
        cond = Math.min(100.0f, cond);
        this.setCondition((int)cond);
        if (item != null) {
            item.setCondition(this.getCondition(), false);
        }
    }

    public void setGeneralCondition(InventoryItem item, float baseQuality, float chanceToSpawnDamaged) {
        int max = 100;
        max = (int)((float)max * baseQuality);
        float cond = 100.0f;
        if (item != null) {
            int chanceToSpawnDamagedTot = item.getChanceToSpawnDamaged();
            if ((chanceToSpawnDamagedTot += (int)chanceToSpawnDamaged) > 0 && Rand.Next(100) < chanceToSpawnDamagedTot) {
                cond = Rand.Next(max - max / 2, max);
            }
        } else {
            int chanceToSpawnDamagedTot = 30;
            if (Rand.Next(100) < (chanceToSpawnDamagedTot += (int)chanceToSpawnDamaged)) {
                cond = Rand.Next((float)max * 0.5f, (float)max);
            }
        }
        switch (SandboxOptions.instance.carGeneralCondition.getValue()) {
            case 1: {
                cond -= Rand.Next(cond * 0.3f, Rand.Next(cond * 0.3f, cond * 0.9f));
                break;
            }
            case 2: {
                cond -= Rand.Next(cond * 0.1f, cond * 0.3f);
                break;
            }
            case 4: {
                cond += Rand.Next(cond * 0.2f, cond * 0.4f);
                break;
            }
            case 5: {
                cond += Rand.Next(cond * 0.5f, cond * 0.9f);
            }
        }
        cond = Math.max(0.0f, cond);
        cond = Math.min(100.0f, cond);
        this.setCondition((int)cond);
        if (item != null) {
            item.setCondition(this.getCondition(), false);
        }
    }

    public static float getNumberByCondition(float number, float cond, float min) {
        cond += 20.0f * (100.0f - cond) / 100.0f;
        float condDelta = cond / 100.0f;
        return (float)Math.round(Math.max(min, number * condDelta) * 100.0f) / 100.0f;
    }

    public boolean isContainer() {
        if (this.scriptPart == null) {
            return false;
        }
        return this.scriptPart.container != null;
    }

    public int getContainerCapacity() {
        return this.getContainerCapacity(null);
    }

    public int getContainerCapacity(IsoGameCharacter chr) {
        if (!this.isContainer()) {
            return 0;
        }
        if (this.getItemContainer() != null) {
            if (chr == null) {
                return this.getItemContainer().getCapacity();
            }
            return this.getItemContainer().getEffectiveCapacity(chr);
        }
        if (this.getInventoryItem() != null) {
            if (this.scriptPart.container.conditionAffectsCapacity) {
                return (int)VehiclePart.getNumberByCondition(((InventoryItem)this.getInventoryItem()).getMaxCapacity(), this.getCondition(), 5.0f);
            }
            return ((InventoryItem)this.getInventoryItem()).getMaxCapacity();
        }
        return this.scriptPart.container.capacity;
    }

    public void setContainerCapacity(int cap) {
        if (!this.isContainer()) {
            return;
        }
        if (this.getItemContainer() != null) {
            this.getItemContainer().capacity = cap;
        }
    }

    public String getContainerContentType() {
        if (!this.isContainer()) {
            return null;
        }
        return this.scriptPart.container.contentType;
    }

    public float getContainerContentAmount() {
        Object object;
        if (!this.isContainer()) {
            return 0.0f;
        }
        if (this.hasModData() && (object = this.getModData().rawget("contentAmount")) instanceof Double) {
            Double d = (Double)object;
            return d.floatValue();
        }
        return 0.0f;
    }

    public void setContainerContentAmount(float amount) {
        this.setContainerContentAmount(amount, false, false);
    }

    public void setContainerContentAmount(float amount, boolean force, boolean noUpdateMass) {
        if (!this.isContainer()) {
            return;
        }
        int cap = this.scriptPart.container.capacity;
        if (this.getInventoryItem() != null) {
            cap = ((InventoryItem)this.getInventoryItem()).getMaxCapacity();
        }
        if (!force) {
            amount = Math.min(amount, (float)cap);
        }
        amount = Math.max(amount, 0.0f);
        this.getModData().rawset("contentAmount", (Object)amount);
        if (this.getInventoryItem() != null) {
            ((InventoryItem)this.getInventoryItem()).setItemCapacity(amount);
        }
        if (!noUpdateMass) {
            this.getVehicle().updateTotalMass();
        }
    }

    public int getContainerSeatNumber() {
        if (!this.isContainer()) {
            return -1;
        }
        return this.scriptPart.container.seat;
    }

    public boolean isSeat() {
        VehicleScript.Part part = this.scriptPart;
        if (part == null) {
            return false;
        }
        if (part.container == null) {
            return false;
        }
        if (part.container.seatId == null) {
            return false;
        }
        if (part.container.seatId.isEmpty()) {
            return false;
        }
        return part.container.seat > -1;
    }

    public boolean isVehicleTrunk() {
        String partId = this.getId();
        if (partId == null) {
            return false;
        }
        return StringUtils.containsIgnoreCase(partId, "TruckBed");
    }

    public String getLuaFunction(String name) {
        if (this.scriptPart == null || this.scriptPart.luaFunctions == null) {
            return null;
        }
        return this.scriptPart.luaFunctions.get(name);
    }

    protected VehicleScript.Model getScriptModelById(String id) {
        if (this.scriptPart == null || this.scriptPart.models == null) {
            return null;
        }
        for (int i = 0; i < this.scriptPart.models.size(); ++i) {
            VehicleScript.Model scriptModel = this.scriptPart.models.get(i);
            if (!id.equals(scriptModel.id)) continue;
            return scriptModel;
        }
        return null;
    }

    public void setModelVisible(String id, boolean visible) {
        VehicleScript.Model model = this.getScriptModelById(id);
        if (model == null) {
            return;
        }
        this.vehicle.setModelVisible(this, model, visible);
    }

    public VehiclePart getParent() {
        return this.parent;
    }

    public void addChild(VehiclePart child) {
        if (this.children == null) {
            this.children = new ArrayList();
        }
        this.children.add(child);
    }

    public int getChildCount() {
        if (this.children == null) {
            return 0;
        }
        return this.children.size();
    }

    public VehiclePart getChild(int index) {
        if (this.children == null || index < 0 || index >= this.children.size()) {
            return null;
        }
        return this.children.get(index);
    }

    public VehicleDoor getDoor() {
        return this.door;
    }

    public VehicleDoor getEnclosingDoor() {
        if (this.door == null && this.parent != null) {
            return this.parent.getEnclosingDoor();
        }
        return this.door;
    }

    public VehicleWindow getWindow() {
        return this.window;
    }

    public VehiclePart getChildWindow() {
        for (int i = 0; i < this.getChildCount(); ++i) {
            VehiclePart child = this.getChild(i);
            if (child.getWindow() == null) continue;
            return child;
        }
        return null;
    }

    public VehicleWindow findWindow() {
        VehiclePart windowPart = this.getChildWindow();
        return windowPart == null ? null : windowPart.getWindow();
    }

    public VehicleScript.Anim getAnimById(String id) {
        if (this.scriptPart == null || this.scriptPart.anims == null) {
            return null;
        }
        for (int i = 0; i < this.scriptPart.anims.size(); ++i) {
            VehicleScript.Anim anim = this.scriptPart.anims.get(i);
            if (!anim.id.equals(id)) continue;
            return anim;
        }
        return null;
    }

    public void save(ByteBuffer output) throws IOException {
        GameWindow.WriteString(output, this.getId());
        output.put((byte)(this.created ? 1 : 0));
        output.putFloat(this.lastUpdated);
        if (this.getInventoryItem() == null) {
            output.put((byte)0);
        } else {
            output.put((byte)1);
            ((InventoryItem)this.getInventoryItem()).saveWithSize(output, false);
        }
        if (this.getItemContainer() == null) {
            output.put((byte)0);
        } else {
            output.put((byte)1);
            this.getItemContainer().save(output);
        }
        if (!this.hasModData() || this.getModData().isEmpty()) {
            output.put((byte)0);
        } else {
            output.put((byte)1);
            this.getModData().save(output);
        }
        if (this.getDeviceData() == null) {
            output.put((byte)0);
        } else {
            output.put((byte)1);
            this.getDeviceData().save(output, false);
        }
        if (this.light == null) {
            output.put((byte)0);
        } else {
            output.put((byte)1);
            this.light.save(output);
        }
        if (this.door == null) {
            output.put((byte)0);
        } else {
            output.put((byte)1);
            this.door.save(output);
        }
        if (this.window == null) {
            output.put((byte)0);
        } else {
            output.put((byte)1);
            this.window.save(output);
        }
        output.putInt(this.condition);
        output.putFloat(this.wheelFriction);
        output.putInt(this.mechanicSkillInstaller);
        output.putFloat(this.suspensionCompression);
        output.putFloat(this.suspensionDamping);
        if (!this.requiresEntitySave()) {
            output.put((byte)0);
        } else {
            output.put((byte)1);
            this.saveEntity(output);
        }
    }

    public void load(ByteBuffer input, int worldVersion) throws IOException {
        this.partId = GameWindow.ReadString(input);
        this.created = input.get() != 0;
        this.lastUpdated = input.getFloat();
        if (input.get() != 0) {
            this.item = InventoryItem.loadItem(input, worldVersion);
        }
        if (input.get() != 0) {
            if (this.container == null) {
                this.container = new ItemContainer();
                this.container.parent = this.getVehicle();
                this.container.vehiclePart = this;
            }
            this.container.getItems().clear();
            this.container.id = 0;
            this.container.load(input, worldVersion);
        }
        if (input.get() != 0) {
            this.getModData().load(input, worldVersion);
        }
        if (input.get() != 0) {
            if (this.getDeviceData() == null) {
                this.createSignalDevice();
            }
            this.getDeviceData().load(input, worldVersion, false);
        }
        if (input.get() != 0) {
            if (this.light == null) {
                this.light = new VehicleLight();
            }
            this.light.load(input, worldVersion);
        }
        if (input.get() != 0) {
            if (this.door == null) {
                this.door = new VehicleDoor(this);
            }
            this.door.load(input, worldVersion);
        }
        if (input.get() != 0) {
            if (this.window == null) {
                this.window = new VehicleWindow(this);
            }
            this.window.load(input, worldVersion);
        }
        this.setCondition(input.getInt());
        this.setWheelFriction(input.getFloat());
        this.setMechanicSkillInstaller(input.getInt());
        this.setSuspensionCompression(input.getFloat());
        this.setSuspensionDamping(input.getFloat());
        if (worldVersion >= 200 && input.get() != 0) {
            this.loadEntity(input, worldVersion);
        }
    }

    public int getWheelIndex() {
        if (this.scriptPart == null || this.scriptPart.wheel == null) {
            return -1;
        }
        for (int i = 0; i < this.vehicle.script.getWheelCount(); ++i) {
            VehicleScript.Wheel scriptWheel = this.vehicle.script.getWheel(i);
            if (!this.scriptPart.wheel.equals(scriptWheel.id)) continue;
            return i;
        }
        return -1;
    }

    public void createSpotLight(float xOffset, float yOffset, float dist, float intensity, float dot, int focusing) {
        this.light = this.light == null ? new VehicleLight() : this.light;
        this.light.offset.set(xOffset, yOffset, 0.0f);
        this.light.dist = dist;
        this.light.intensity = intensity;
        this.light.dot = dot;
        this.light.focusing = focusing;
    }

    public void createSpotLightColor(float xOffset, float yOffset, float dist, float intensity, float dot, int focusing, float r, float g, float b) {
        this.createSpotLight(xOffset, yOffset, dist, intensity, dot, focusing);
        this.light.r = r;
        this.light.g = g;
        this.light.b = b;
    }

    public VehicleLight getLight() {
        return this.light;
    }

    public float getLightDistance() {
        return this.light == null ? 0.0f : PZMath.lerp(this.light.dist * 0.5f, this.light.dist, (float)this.getCondition() / 100.0f);
    }

    public float getLightIntensity() {
        return this.light == null ? 0.0f : PZMath.lerp(this.light.intensity * 0.5f, this.light.intensity, (float)this.getCondition() / 100.0f);
    }

    public float getLightFocusing() {
        return this.light == null ? 0.0f : (float)(20 + (int)(80.0f * (1.0f - (float)this.getCondition() / 100.0f)));
    }

    public void setLightActive(boolean active) {
        if (this.light == null || this.light.active == active) {
            return;
        }
        this.light.active = active;
        if (GameServer.server) {
            this.vehicle.updateFlags = (short)(this.vehicle.updateFlags | 8);
        }
    }

    public DeviceData createSignalDevice() {
        if (this.deviceData == null) {
            this.deviceData = new DeviceData(this);
        }
        if (this.chatElement == null) {
            this.chatElement = new ChatElement(this, 5, "device");
        }
        return this.deviceData;
    }

    public boolean hasDevicePower() {
        return this.vehicle.getBatteryCharge() > 0.0f;
    }

    @Override
    public DeviceData getDeviceData() {
        return this.deviceData;
    }

    @Override
    public void setDeviceData(DeviceData data) {
        if (data == null) {
            data = new DeviceData(this);
        }
        if (this.deviceData != null) {
            this.deviceData.cleanSoundsAndEmitter();
        }
        this.deviceData = data;
        this.deviceData.setParent(this);
    }

    @Override
    public float getDelta() {
        if (this.deviceData != null) {
            return this.deviceData.getPower();
        }
        return 0.0f;
    }

    @Override
    public void setDelta(float d) {
        if (this.deviceData != null) {
            this.deviceData.setPower(d);
        }
    }

    @Override
    public float getX() {
        return this.vehicle.getX();
    }

    @Override
    public float getY() {
        return this.vehicle.getY();
    }

    @Override
    public float getZ() {
        return this.vehicle.getZ();
    }

    @Override
    public IsoGridSquare getSquare() {
        return this.vehicle.getSquare();
    }

    @Override
    public void AddDeviceText(String line, float r, float g, float b, String guid, String codes, int distance) {
        if (this.deviceData != null && this.deviceData.getIsTurnedOn()) {
            if (!ZomboidRadio.isStaticSound(line)) {
                this.deviceData.doReceiveSignal(distance);
            }
            if (this.deviceData.getDeviceVolume() > 0.0f) {
                this.chatElement.addChatLine(line, r, g, b, UIFont.Medium, this.deviceData.getDeviceVolumeRange(), "default", true, true, true, true, true, true);
                if (codes != null) {
                    LuaEventManager.triggerEvent("OnDeviceText", guid, codes, Float.valueOf(this.getX()), Float.valueOf(this.getY()), Float.valueOf(this.getZ()), line, this);
                }
            }
        }
    }

    @Override
    public boolean HasPlayerInRange() {
        return this.hasPlayerInRange;
    }

    private boolean playerWithinBounds(IsoPlayer player, float dist) {
        if (player == null || player.isDead()) {
            return false;
        }
        return (player.getX() > this.getX() - dist || this.getX() < this.getX() + dist) && (player.getY() > this.getY() - dist || this.getY() < this.getY() + dist);
    }

    public void updateSignalDevice() {
        if (this.deviceData == null) {
            return;
        }
        if (this.deviceData.getIsTurnedOn() && this.isInventoryItemUninstalled()) {
            this.deviceData.setIsTurnedOn(false);
        }
        if (GameClient.client) {
            this.deviceData.updateSimple();
        } else {
            this.deviceData.update(true, this.hasPlayerInRange);
        }
        if (!GameServer.server) {
            this.hasPlayerInRange = false;
            if (this.deviceData.getIsTurnedOn()) {
                for (int i = 0; i < IsoPlayer.numPlayers; ++i) {
                    IsoPlayer player = IsoPlayer.players[i];
                    if (!this.playerWithinBounds(player, (float)this.deviceData.getDeviceVolumeRange() * 0.6f)) continue;
                    this.hasPlayerInRange = true;
                    break;
                }
            }
            this.chatElement.setHistoryRange((float)this.deviceData.getDeviceVolumeRange() * 0.6f);
            this.chatElement.update();
        } else {
            this.hasPlayerInRange = false;
        }
    }

    public String getCategory() {
        return this.category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public int getCondition() {
        return this.condition;
    }

    public void setCondition(int condition) {
        condition = Math.min(100, condition);
        condition = Math.max(0, condition);
        if (this.getVehicle().getDriverRegardlessOfTow() != null) {
            if (this.condition > 60 && condition < 60 && condition > 40) {
                LuaEventManager.triggerEvent("OnVehicleDamageTexture", this.getVehicle().getDriverRegardlessOfTow());
            }
            if (this.condition > 40 && condition < 40) {
                LuaEventManager.triggerEvent("OnVehicleDamageTexture", this.getVehicle().getDriverRegardlessOfTow());
            }
        }
        this.condition = condition;
        if (this.getInventoryItem() != null) {
            ((InventoryItem)this.getInventoryItem()).setCondition(condition, false);
        }
        this.getVehicle().doDamageOverlay = true;
        if (condition <= 0 && "lightbar".equals(this.getId())) {
            this.getVehicle().lightbarLightsMode.set(0);
            this.getVehicle().setLightbarSirenMode(0);
        }
        if (this.scriptPart != null && this.scriptPart.id != null && this.scriptPart.id.equals("TrailerTrunk")) {
            this.getItemContainer().setCapacity(Math.max(80, condition));
        }
    }

    public void damage(int amount) {
        if (this.getWindow() != null) {
            this.getWindow().damage(amount);
            return;
        }
        this.setCondition(this.getCondition() - amount);
        this.getVehicle().transmitPartCondition(this);
    }

    public boolean isSpecificItem() {
        return this.specificItem;
    }

    public void setSpecificItem(boolean specificItem) {
        this.specificItem = specificItem;
    }

    public float getWheelFriction() {
        return this.wheelFriction;
    }

    public void setWheelFriction(float wheelFriction) {
        this.wheelFriction = wheelFriction;
    }

    public int getMechanicSkillInstaller() {
        return this.mechanicSkillInstaller;
    }

    public void setMechanicSkillInstaller(int mechanicSkillInstaller) {
        this.mechanicSkillInstaller = mechanicSkillInstaller;
    }

    public float getSuspensionDamping() {
        return this.suspensionDamping;
    }

    public void setSuspensionDamping(float suspensionDamping) {
        this.suspensionDamping = suspensionDamping;
    }

    public float getSuspensionCompression() {
        return this.suspensionCompression;
    }

    public void setSuspensionCompression(float suspensionCompression) {
        this.suspensionCompression = suspensionCompression;
    }

    public float getEngineLoudness() {
        return this.engineLoudness;
    }

    public void setEngineLoudness(float engineLoudness) {
        this.engineLoudness = engineLoudness;
    }

    public void repair() {
        Object item;
        String itemType;
        VehicleScript vehicleScript = this.vehicle.getScript();
        float contentAmount = this.getContainerContentAmount();
        if (this.isInventoryItemUninstalled() && (itemType = this.getItemType().get(Rand.Next(this.getItemType().size()))) != null && !itemType.isEmpty() && (item = InventoryItemFactory.CreateItem(itemType)) != null) {
            this.setInventoryItem((InventoryItem)item);
            if (((InventoryItem)item).getMaxCapacity() > 0) {
                ((InventoryItem)item).setItemCapacity(((InventoryItem)item).getMaxCapacity());
            }
            this.vehicle.transmitPartItem(this);
            this.callLuaVoid(this.getLuaFunction("init"), this.vehicle, this);
        }
        if (this.getDoor() != null && this.getDoor().isLockBroken()) {
            this.getDoor().setLockBroken(false);
            this.vehicle.transmitPartDoor(this);
        }
        if (this.getCondition() != 100) {
            this.setCondition(100);
            if (this.getInventoryItem() != null) {
                this.doInventoryItemStats((InventoryItem)this.getInventoryItem(), this.getMechanicSkillInstaller());
                this.vehicle.transmitPartItem(this);
            }
            this.vehicle.transmitPartCondition(this);
        }
        if (this.isContainer() && this.getItemContainer() == null && contentAmount != (float)this.getContainerCapacity()) {
            this.setContainerContentAmount(this.getContainerCapacity());
            this.vehicle.transmitPartModData(this);
        }
        if (this.getInventoryItem() instanceof Drainable && ((InventoryItem)this.getInventoryItem()).getCurrentUsesFloat() < 1.0f) {
            ((InventoryItem)this.getInventoryItem()).setCurrentUses(((InventoryItem)this.getInventoryItem()).getMaxUses());
            this.vehicle.transmitPartUsedDelta(this);
        }
        if ("Engine".equalsIgnoreCase(this.getId())) {
            int quality = 100;
            int loudness = (int)((double)vehicleScript.getEngineLoudness() * SandboxOptions.getInstance().zombieAttractionMultiplier.getValue());
            int power = (int)vehicleScript.getEngineForce();
            this.vehicle.setEngineFeature(100, loudness, power);
            this.vehicle.transmitEngine();
        }
        this.vehicle.updatePartStats();
        this.vehicle.updateBulletStats();
    }

    private void callLuaVoid(String functionName, Object arg1, Object arg2) {
        Object functionObj = LuaManager.getFunctionObject(functionName);
        if (functionObj == null) {
            return;
        }
        LuaManager.caller.protectedCallVoid(LuaManager.thread, functionObj, arg1, arg2);
    }

    public ChatElement getChatElement() {
        return this.chatElement;
    }

    @Override
    public GameEntityType getGameEntityType() {
        return GameEntityType.VehiclePart;
    }

    @Override
    public boolean isEntityValid() {
        return true;
    }

    @Override
    public long getEntityNetID() {
        long a = (long)this.getVehicle().vehicleId << 31;
        long b = this.getIndex();
        return a + b;
    }

    public void setDurability(float durability) {
        if (durability > 0.0f) {
            this.durability = durability;
        }
    }

    public float getDurability() {
        return this.durability;
    }

    public String getMechanicArea() {
        return this.scriptPart.getMechanicArea();
    }

    public void setFlag(short flag) {
        this.updateFlags = (short)(this.updateFlags | flag);
    }

    public boolean getFlag(short flag) {
        return (this.updateFlags & flag) != 0;
    }

    public void clearFlags() {
        this.updateFlags = 0;
    }
}

