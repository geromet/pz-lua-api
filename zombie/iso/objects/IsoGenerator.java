/*
 * Decompiled with CFR 0.152.
 */
package zombie.iso.objects;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import zombie.GameTime;
import zombie.SandboxOptions;
import zombie.UsedFromLua;
import zombie.WorldSoundManager;
import zombie.audio.BaseSoundEmitter;
import zombie.core.Translator;
import zombie.core.logger.ExceptionLogger;
import zombie.core.math.PZMath;
import zombie.core.network.ByteBufferReader;
import zombie.core.network.ByteBufferWriter;
import zombie.core.properties.IsoPropertyType;
import zombie.core.properties.PropertyContainer;
import zombie.core.random.Rand;
import zombie.inventory.InventoryItem;
import zombie.inventory.ItemContainer;
import zombie.inventory.types.Food;
import zombie.iso.IsoCell;
import zombie.iso.IsoChunk;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoObject;
import zombie.iso.IsoUtils;
import zombie.iso.IsoWorld;
import zombie.iso.SpriteDetails.IsoFlagType;
import zombie.iso.objects.IsoCarBatteryCharger;
import zombie.iso.objects.IsoClothingDryer;
import zombie.iso.objects.IsoClothingWasher;
import zombie.iso.objects.IsoCombinationWasherDryer;
import zombie.iso.objects.IsoFireManager;
import zombie.iso.objects.IsoLightSwitch;
import zombie.iso.objects.IsoRadio;
import zombie.iso.objects.IsoStackedWasherDryer;
import zombie.iso.objects.IsoStove;
import zombie.iso.objects.IsoTelevision;
import zombie.iso.objects.IsoWorldInventoryObject;
import zombie.iso.sprite.IsoSpriteManager;
import zombie.network.GameClient;
import zombie.network.GameServer;
import zombie.network.ServerMap;
import zombie.scripting.ScriptManager;
import zombie.scripting.objects.Item;

@UsedFromLua
public class IsoGenerator
extends IsoObject {
    private static final int MaximumGeneratorCondition = 100;
    private static final float MaximumGeneratorFuel = 10.0f;
    private static final int GeneratorMinimumCondition = 0;
    private static final int GeneratorCriticalCondition = 20;
    private static final int GeneratorWarningCondition = 30;
    private static final int GeneratorLowCondition = 40;
    private static final int GeneratorBackfireChanceCritical = 5;
    private static final int GeneratorBackfireChanceWarning = 10;
    private static final int GeneratorBackfireChanceLow = 15;
    private static final int GeneratorFireChance = 10;
    private static final int GeneratorExplodeChance = 20;
    private static final float GeneratorSoundOffset = 0.5f;
    private static final int GeneratorDefaultSoundRadius = 40;
    private static final int GeneratorDefaultSoundVolume = 60;
    private static final int GeneratorConditionLowerChanceDefault = 30;
    private static final float GeneratorBasePowerConsumption = 0.02f;
    private static int generatorVerticalPowerRange = 3;
    private static final int IsoGeneratorFireStartingEnergy = 1000;
    private static int generatorChunkRange = 2;
    private static final int GeneratorMinZ = -32;
    private static final int GeneratorMaxZ = 31;
    private static final float ClothingAppliancePowerConsumption = 0.09f;
    private static final float TelevisionPowerConsumption = 0.03f;
    private static final float RadioPowerConsumption = 0.01f;
    private static final float StovePowerConsumption = 0.09f;
    private static final float FridgeFreezerPowerConsumption = 0.13f;
    private static final float SingleFridgeOrFreezerPowerConsumption = 0.08f;
    private static final float LightSwitchPowerConsumption = 0.002f;
    private static final float PipedFuelPowerConsumption = 0.03f;
    private static final float BatteryChargerPowerConsumption = 0.05f;
    private static final float StackedWasherDryerPowerConsumption = 0.9f;
    public float fuel;
    public boolean activated;
    public int condition;
    private int lastHour = -1;
    public boolean connected;
    private boolean updateSurrounding;
    private final HashMap<String, String> itemsPowered = new HashMap();
    private float totalPowerUsing;
    private static final ArrayList<IsoGenerator> AllGenerators = new ArrayList();
    private static int generatorRadius = 20;
    private static final int GENERATOR_SOUND_RADIUS = 20;
    private static final int GENERATOR_SOUND_VOLUME = 20;
    private static final DecimalFormat decimalFormat = new DecimalFormat("#.#### L/h");
    private static final DecimalFormat decimalFormatB = new DecimalFormat(" (#.#### L/h)");

    public IsoGenerator(IsoCell cell) {
        super(cell);
        this.setGeneratorRange();
    }

    public IsoGenerator(InventoryItem item, IsoCell cell, IsoGridSquare sq) {
        super(cell, sq, IsoSpriteManager.instance.getSprite(item.getScriptItem().getWorldObjectSprite()));
        String sprite = item.getScriptItem().getWorldObjectSprite();
        this.setInfoFromItem(item);
        this.sprite = IsoSpriteManager.instance.getSprite(sprite);
        this.square = sq;
        sq.AddSpecialObject(this);
        if (GameServer.server) {
            this.transmitCompleteItemToClients();
        }
        this.setGeneratorRange();
    }

    public IsoGenerator(InventoryItem item, IsoCell cell, IsoGridSquare sq, boolean remote) {
        super(cell, sq, IsoSpriteManager.instance.getSprite(item.getScriptItem().getWorldObjectSprite()));
        String sprite = item.getScriptItem().getWorldObjectSprite();
        this.setInfoFromItem(item);
        this.sprite = IsoSpriteManager.instance.getSprite(sprite);
        this.square = sq;
        sq.AddSpecialObject(this);
        if (GameClient.client && !remote) {
            this.transmitCompleteItemToServer();
        }
        this.setGeneratorRange();
    }

    private void setGeneratorRange() {
        generatorVerticalPowerRange = SandboxOptions.getInstance().generatorVerticalPowerRange.getValue();
        generatorRadius = SandboxOptions.getInstance().generatorTileRange.getValue();
        generatorChunkRange = generatorRadius / 10 + 1;
    }

    public void setInfoFromItem(InventoryItem item) {
        this.condition = item.getCondition();
        if (item.getModData().rawget("fuel") instanceof Double) {
            this.fuel = ((Double)item.getModData().rawget("fuel")).floatValue();
        }
        this.getModData().rawset("generatorFullType", (Object)String.valueOf(item.getFullType()));
    }

    @Override
    public void update() {
        if (this.updateSurrounding && this.getSquare() != null) {
            this.setSurroundingElectricity();
            this.updateSurrounding = false;
        }
        if (this.isActivated()) {
            String itemType;
            Object object;
            if (!(GameServer.server || this.emitter != null && this.emitter.isPlaying(this.getSoundPrefix() + "Loop"))) {
                this.playGeneratorSound("Loop");
            }
            if (GameClient.client) {
                this.emitter.tick();
                return;
            }
            Item item = null;
            if (this.getModData().rawget("generatorFullType") != null && (object = this.getModData().rawget("generatorFullType")) instanceof String && ScriptManager.instance.getItem(itemType = (String)object) != null) {
                item = ScriptManager.instance.getItem(itemType);
            }
            int soundRadius = 20;
            int soundVolume = 20;
            if (item != null) {
                if (item.getSoundRadius() > 0) {
                    soundRadius = item.getSoundRadius();
                }
                if (item.getSoundVolume() > 0) {
                    soundVolume = item.getSoundVolume();
                }
            }
            if (this.getSquare().getRoom() != null) {
                soundRadius /= 2;
            }
            WorldSoundManager.instance.addSoundRepeating((Object)this, PZMath.fastfloor(this.getX()), PZMath.fastfloor(this.getY()), PZMath.fastfloor(this.getZ()), soundRadius, soundVolume, false);
            if ((int)GameTime.getInstance().getWorldAgeHours() != this.lastHour) {
                if (!this.getSquare().getProperties().has(IsoFlagType.exterior) && this.getSquare().getBuilding() != null) {
                    this.getSquare().getBuilding().setToxic(this.isActivated());
                }
                int elapsedHours = (int)GameTime.getInstance().getWorldAgeHours() - this.lastHour;
                float subtractFuel = 0.0f;
                int subtractCondition = 0;
                int conditionLowerChance = item != null ? item.getConditionLowerChance() : 30;
                for (int i = 0; i < elapsedHours; ++i) {
                    float lowerFuel = this.totalPowerUsing;
                    lowerFuel = (float)((double)lowerFuel * SandboxOptions.instance.generatorFuelConsumption.getValue());
                    subtractFuel += lowerFuel;
                    if (Rand.Next(conditionLowerChance) == 0) {
                        subtractCondition += Rand.Next(2) + 1;
                    }
                    if (this.fuel - subtractFuel <= 0.0f || this.condition - subtractCondition <= 0) break;
                }
                this.fuel -= subtractFuel;
                if (this.fuel <= 0.0f) {
                    this.setActivated(false);
                    this.fuel = 0.0f;
                }
                this.condition -= subtractCondition;
                if (this.condition <= 0) {
                    this.setActivated(false);
                    this.condition = 0;
                }
                boolean bBackfire = false;
                if (this.condition <= 20) {
                    bBackfire = Rand.Next(5) == 0;
                } else if (this.condition <= 30) {
                    bBackfire = Rand.Next(10) == 0;
                } else if (this.condition <= 40) {
                    boolean bl = bBackfire = Rand.Next(15) == 0;
                }
                if (bBackfire) {
                    if (GameServer.server) {
                        GameServer.PlayWorldSoundServer(this.getSoundPrefix() + "Backfire", this.getSquare(), 40.0f, -1);
                    } else {
                        this.playGeneratorSound("Backfire");
                    }
                    WorldSoundManager.instance.addSound(this, this.square.getX(), this.square.getY(), this.square.getZ(), 40, 60, false, 0.0f, 15.0f);
                }
                if (this.condition <= 20) {
                    if (Rand.Next(10) == 0) {
                        IsoFireManager.StartFire(this.getCell(), this.square, true, 1000);
                        this.condition = 0;
                        this.setActivated(false);
                    } else if (Rand.Next(20) == 0) {
                        this.explode(this.square);
                        this.condition = 0;
                        this.setActivated(false);
                    }
                }
                this.lastHour = (int)GameTime.getInstance().getWorldAgeHours();
                if (GameServer.server) {
                    this.sync();
                }
            }
        }
        if (this.emitter != null) {
            this.emitter.tick();
        }
    }

    public void setSurroundingElectricity() {
        this.itemsPowered.clear();
        this.totalPowerUsing = 0.02f;
        if (this.square == null || this.square.chunk == null) {
            return;
        }
        int chunkX = this.square.chunk.wx;
        int chunkY = this.square.chunk.wy;
        for (int dy = -generatorChunkRange; dy <= generatorChunkRange; ++dy) {
            for (int dx = -generatorChunkRange; dx <= generatorChunkRange; ++dx) {
                IsoChunk chunk;
                IsoChunk isoChunk = chunk = GameServer.server ? ServerMap.instance.getChunk(chunkX + dx, chunkY + dy) : IsoWorld.instance.currentCell.getChunk(chunkX + dx, chunkY + dy);
                if (chunk == null || !this.touchesChunk(chunk)) continue;
                if (this.isActivated()) {
                    chunk.addGeneratorPos(this.square.x, this.square.y, this.square.z);
                    continue;
                }
                chunk.removeGeneratorPos(this.square.x, this.square.y, this.square.z);
            }
        }
        boolean allowExteriorGenerator = SandboxOptions.getInstance().allowExteriorGenerator.getValue();
        int minX = this.square.getX() - generatorRadius;
        int maxX = this.square.getX() + generatorRadius;
        int minY = this.square.getY() - generatorRadius;
        int maxY = this.square.getY() + generatorRadius;
        int minZ = Math.max(-32, this.getSquare().getZ() - generatorVerticalPowerRange);
        int maxZ = Math.min(31, this.getSquare().getZ() + generatorVerticalPowerRange);
        for (int z = minZ; z < maxZ; ++z) {
            for (int x = minX; x <= maxX; ++x) {
                for (int y = minY; y <= maxY; ++y) {
                    IsoGridSquare sq;
                    if (IsoUtils.DistanceToSquared((float)x + 0.5f, (float)y + 0.5f, (float)this.getSquare().getX() + 0.5f, (float)this.getSquare().getY() + 0.5f) > (float)(generatorRadius * generatorRadius) || (sq = this.getCell().getGridSquare(x, y, z)) == null) continue;
                    for (int i = 0; i < sq.getObjects().size(); ++i) {
                        IsoCarBatteryCharger isoCarBatteryCharger;
                        boolean bFreezer;
                        IsoStove isoStove;
                        IsoRadio isoRadio;
                        IsoTelevision isoTelevision;
                        IsoCombinationWasherDryer isoCombinationWasherDryer;
                        IsoClothingWasher isoClothingWasher;
                        IsoClothingDryer isoClothingDryer;
                        IsoObject obj = sq.getObjects().get(i);
                        if (obj == null || obj instanceof IsoWorldInventoryObject) continue;
                        if (obj instanceof IsoClothingDryer && (isoClothingDryer = (IsoClothingDryer)obj).isActivated()) {
                            this.addPoweredItem(obj, 0.09f);
                        }
                        if (obj instanceof IsoClothingWasher && (isoClothingWasher = (IsoClothingWasher)obj).isActivated()) {
                            this.addPoweredItem(obj, 0.09f);
                        }
                        if (obj instanceof IsoCombinationWasherDryer && (isoCombinationWasherDryer = (IsoCombinationWasherDryer)obj).isActivated()) {
                            this.addPoweredItem(obj, 0.09f);
                        }
                        if (obj instanceof IsoStackedWasherDryer) {
                            IsoStackedWasherDryer swd = (IsoStackedWasherDryer)obj;
                            float power = 0.0f;
                            if (swd.isDryerActivated()) {
                                power += 0.9f;
                            }
                            if (swd.isWasherActivated()) {
                                power += 0.9f;
                            }
                            if (power > 0.0f) {
                                this.addPoweredItem(obj, power);
                            }
                        }
                        if (obj instanceof IsoTelevision && (isoTelevision = (IsoTelevision)obj).getDeviceData().getIsTurnedOn()) {
                            this.addPoweredItem(obj, 0.03f);
                        }
                        if (obj instanceof IsoRadio && (isoRadio = (IsoRadio)obj).getDeviceData().getIsTurnedOn() && !isoRadio.getDeviceData().getIsBatteryPowered()) {
                            this.addPoweredItem(obj, 0.01f);
                        }
                        if (obj instanceof IsoStove && (isoStove = (IsoStove)obj).Activated()) {
                            this.addPoweredItem(obj, 0.09f);
                        }
                        boolean bFridge = obj.getContainerByType("fridge") != null;
                        boolean bl = bFreezer = obj.getContainerByType("freezer") != null;
                        if (bFridge && bFreezer) {
                            this.addPoweredItem(obj, 0.13f);
                        } else if (bFridge || bFreezer) {
                            this.addPoweredItem(obj, 0.08f);
                        }
                        if (obj instanceof IsoLightSwitch) {
                            IsoLightSwitch isoLightSwitch = (IsoLightSwitch)obj;
                            if (isoLightSwitch.activated && !isoLightSwitch.streetLight) {
                                this.addPoweredItem(obj, 0.002f);
                            }
                        }
                        if (obj instanceof IsoCarBatteryCharger && (isoCarBatteryCharger = (IsoCarBatteryCharger)obj).isActivated()) {
                            this.addPoweredItem(obj, 0.05f);
                        }
                        if (obj.getPipedFuelAmount() > 0) {
                            this.addPoweredItem(obj, 0.03f);
                        }
                        obj.checkHaveElectricity();
                    }
                }
            }
        }
    }

    private void addPoweredItem(IsoObject obj, float powerConsumption) {
        PropertyContainer props;
        String name = Translator.getText("IGUI_VehiclePartCatOther");
        if (obj.getPipedFuelAmount() > 0) {
            name = Translator.getText("IGUI_GasPump");
        }
        if ((props = obj.getProperties()) != null && props.has(IsoPropertyType.CUSTOM_NAME)) {
            Object customName = "Moveable Object";
            if (props.has(IsoPropertyType.CUSTOM_NAME)) {
                customName = props.has(IsoPropertyType.GROUP_NAME) ? props.get(IsoPropertyType.GROUP_NAME) + " " + props.get(IsoPropertyType.CUSTOM_NAME) : props.get(IsoPropertyType.CUSTOM_NAME);
            }
            name = Translator.getMoveableDisplayName((String)customName);
        }
        if (obj instanceof IsoLightSwitch) {
            name = Translator.getText("IGUI_Lights");
        }
        if (obj instanceof IsoCarBatteryCharger) {
            name = Translator.getText("IGUI_VehiclePartCatOther");
        }
        int nbr = 1;
        for (String test : this.itemsPowered.keySet()) {
            if (!test.startsWith(name)) continue;
            nbr = Integer.parseInt(test.replaceAll("[\\D]", ""));
            this.totalPowerUsing -= powerConsumption * (float)nbr;
            ++nbr;
            this.itemsPowered.remove(test);
            break;
        }
        this.itemsPowered.put(name + " x" + nbr, decimalFormatB.format((double)(powerConsumption * (float)nbr) * SandboxOptions.instance.generatorFuelConsumption.getValue()));
        this.totalPowerUsing += powerConsumption * (float)nbr;
    }

    private void updateFridgeFreezerItems(IsoObject object) {
        for (int i = 0; i < object.getContainerCount(); ++i) {
            ItemContainer container = object.getContainerByIndex(i);
            if (!"fridge".equals(container.getType()) && !"freezer".equals(container.getType())) continue;
            ArrayList<InventoryItem> items = container.getItems();
            for (int j = 0; j < items.size(); ++j) {
                InventoryItem item = items.get(j);
                if (!(item instanceof Food)) continue;
                item.updateAge();
            }
        }
    }

    private void updateFridgeFreezerItems(IsoGridSquare square) {
        int objCount = square.getObjects().size();
        IsoObject[] objects = square.getObjects().getElements();
        for (int i = 0; i < objCount; ++i) {
            IsoObject object = objects[i];
            this.updateFridgeFreezerItems(object);
        }
    }

    private void updateFridgeFreezerItems() {
        if (this.square == null) {
            return;
        }
        int minX = this.square.getX() - generatorRadius;
        int maxX = this.square.getX() + generatorRadius;
        int minY = this.square.getY() - generatorRadius;
        int maxY = this.square.getY() + generatorRadius;
        int minZ = Math.max(-32, this.square.getZ() - generatorVerticalPowerRange);
        int maxZ = Math.min(31, this.square.getZ() + generatorVerticalPowerRange);
        for (int z = minZ; z < maxZ; ++z) {
            for (int x = minX; x <= maxX; ++x) {
                for (int y = minY; y <= maxY; ++y) {
                    IsoGridSquare square1;
                    if (!(IsoUtils.DistanceToSquared(x, y, this.square.x, this.square.y) <= (float)(generatorRadius * generatorRadius)) || (square1 = this.getCell().getGridSquare(x, y, z)) == null) continue;
                    this.updateFridgeFreezerItems(square1);
                }
            }
        }
    }

    @Override
    public void load(ByteBuffer input, int worldVersion, boolean isDebugSave) throws IOException {
        super.load(input, worldVersion, isDebugSave);
        this.connected = input.get() != 0;
        this.activated = input.get() != 0;
        this.fuel = Math.min(input.getFloat(), 10.0f);
        this.condition = input.getInt();
        this.lastHour = input.getInt();
        this.updateSurrounding = true;
    }

    @Override
    public void save(ByteBuffer output, boolean isDebugSave) throws IOException {
        super.save(output, isDebugSave);
        output.put(this.isConnected() ? (byte)1 : 0);
        output.put(this.isActivated() ? (byte)1 : 0);
        output.putFloat(this.getFuel());
        output.putInt(this.getCondition());
        output.putInt(this.lastHour);
    }

    public void remove() {
        if (this.getSquare() == null) {
            return;
        }
        this.getSquare().transmitRemoveItemFromSquare(this);
    }

    @Override
    public void addToWorld() {
        this.getCell().addToProcessIsoObject(this);
        if (!AllGenerators.contains(this)) {
            AllGenerators.add(this);
        }
    }

    @Override
    public void removeFromWorld() {
        AllGenerators.remove(this);
        if (this.emitter != null) {
            this.emitter.stopAll();
            IsoWorld.instance.returnOwnershipOfEmitter(this.emitter);
            this.emitter = null;
        }
        super.removeFromWorld();
    }

    @Override
    public String getObjectName() {
        return "IsoGenerator";
    }

    public double getBasePowerConsumption() {
        return (double)0.02f * SandboxOptions.instance.generatorFuelConsumption.getValue();
    }

    public String getBasePowerConsumptionString() {
        return decimalFormat.format((double)0.02f * SandboxOptions.instance.generatorFuelConsumption.getValue());
    }

    public float getFuel() {
        return this.fuel;
    }

    public float getFuelPercentage() {
        return this.fuel / 10.0f * 100.0f;
    }

    public float getMaxFuel() {
        return 10.0f;
    }

    public void setFuel(float fuel) {
        this.fuel = Math.max(0.0f, Math.min(fuel, 10.0f));
        if (GameServer.server) {
            this.sync();
        }
        if (GameClient.client) {
            this.sync();
        }
    }

    public boolean isActivated() {
        return this.activated;
    }

    public void setActivated(boolean activated) {
        if (activated == this.activated) {
            return;
        }
        if (!this.getSquare().getProperties().has(IsoFlagType.exterior) && this.getSquare().getBuilding() != null) {
            this.getSquare().getBuilding().setToxic(activated);
        }
        if (activated) {
            this.lastHour = (int)GameTime.getInstance().getWorldAgeHours();
            this.playGeneratorSound("Starting");
        } else {
            this.stopAllSounds();
            this.playGeneratorSound("Stopping");
        }
        try {
            this.updateFridgeFreezerItems();
        }
        catch (Throwable t) {
            ExceptionLogger.logException(t);
        }
        this.activated = activated;
        this.setSurroundingElectricity();
        if (GameClient.client) {
            this.sync();
        }
        if (GameServer.server) {
            this.sync();
        }
    }

    public void failToStart() {
        if (GameServer.server) {
            return;
        }
        this.playGeneratorSound("FailedToStart");
    }

    public int getCondition() {
        return this.condition;
    }

    public void setCondition(int condition) {
        this.condition = Math.max(0, Math.min(condition, 100));
        if (GameServer.server) {
            this.sync();
        }
        if (GameClient.client) {
            this.sync();
        }
    }

    public boolean isConnected() {
        return this.connected;
    }

    public void setConnected(boolean connected) {
        this.connected = connected;
        if (GameClient.client) {
            this.sync();
        }
        if (GameServer.server) {
            this.sync();
        }
    }

    @Override
    public void syncIsoObjectSend(ByteBufferWriter b) {
        b.putInt(this.square.getX());
        b.putInt(this.square.getY());
        b.putInt(this.square.getZ());
        b.putByte(this.getObjectIndex());
        b.putBoolean(true);
        b.putBoolean(false);
        b.putFloat(this.fuel);
        b.putInt(this.condition);
        b.putBoolean(this.activated);
        b.putBoolean(this.connected);
    }

    @Override
    public void syncIsoObjectReceive(ByteBufferReader bb) {
        float fuel = bb.getFloat();
        int condition = bb.getInt();
        boolean activated = bb.getBoolean();
        boolean connected = bb.getBoolean();
        this.fuel = fuel;
        this.condition = condition;
        this.connected = connected;
        if (this.activated != activated) {
            try {
                this.updateFridgeFreezerItems();
            }
            catch (Throwable t) {
                ExceptionLogger.logException(t);
            }
            this.activated = activated;
            if (activated) {
                this.lastHour = (int)GameTime.getInstance().getWorldAgeHours();
                if (GameClient.client) {
                    this.playGeneratorSound("Starting");
                }
            } else if (GameClient.client) {
                this.stopAllSounds();
                this.playGeneratorSound("Stopping");
            }
            this.setSurroundingElectricity();
        }
    }

    private boolean touchesChunk(IsoChunk chunk) {
        IsoGridSquare square = this.getSquare();
        assert (square != null);
        if (square == null) {
            return false;
        }
        int minX = chunk.wx * 8;
        int minY = chunk.wy * 8;
        int maxX = minX + 8 - 1;
        int maxY = minY + 8 - 1;
        if (square.x - generatorRadius > maxX) {
            return false;
        }
        if (square.x + generatorRadius < minX) {
            return false;
        }
        if (square.y - generatorRadius > maxY) {
            return false;
        }
        return square.y + generatorRadius >= minY;
    }

    public static void chunkLoaded(IsoChunk chunk) {
        chunk.checkForMissingGenerators();
        for (int dy = -generatorChunkRange; dy <= generatorChunkRange; ++dy) {
            for (int dx = -generatorChunkRange; dx <= generatorChunkRange; ++dx) {
                IsoChunk chunk1;
                if (dx == 0 && dy == 0) continue;
                IsoChunk isoChunk = chunk1 = GameServer.server ? ServerMap.instance.getChunk(chunk.wx + dx, chunk.wy + dy) : IsoWorld.instance.currentCell.getChunk(chunk.wx + dx, chunk.wy + dy);
                if (chunk1 == null) continue;
                chunk1.checkForMissingGenerators();
            }
        }
        for (int i = 0; i < AllGenerators.size(); ++i) {
            IsoGenerator generator = AllGenerators.get(i);
            if (generator.updateSurrounding || !generator.touchesChunk(chunk)) continue;
            generator.updateSurrounding = true;
        }
    }

    public static void updateSurroundingNow() {
        for (int i = 0; i < AllGenerators.size(); ++i) {
            IsoGenerator generator = AllGenerators.get(i);
            if (!generator.updateSurrounding || generator.getSquare() == null) continue;
            generator.updateSurrounding = false;
            generator.setSurroundingElectricity();
        }
    }

    public static void updateGenerator(IsoGridSquare sq) {
        if (sq == null) {
            return;
        }
        for (int i = 0; i < AllGenerators.size(); ++i) {
            float distSq;
            IsoGenerator generator = AllGenerators.get(i);
            if (generator.getSquare() == null || !((distSq = IsoUtils.DistanceToSquared((float)sq.x + 0.5f, (float)sq.y + 0.5f, (float)generator.getSquare().getX() + 0.5f, (float)generator.getSquare().getY() + 0.5f)) <= (float)(generatorRadius * generatorRadius))) continue;
            generator.updateSurrounding = true;
        }
    }

    public static void Reset() {
        assert (AllGenerators.isEmpty());
        AllGenerators.clear();
    }

    public static boolean isPoweringSquare(int generatorX, int generatorY, int generatorZ, int x, int y, int z) {
        int minZ = Math.max(-32, generatorZ - generatorVerticalPowerRange);
        int maxZ = Math.min(31, generatorZ + generatorVerticalPowerRange);
        if (z < minZ || z > maxZ) {
            return false;
        }
        return IsoUtils.DistanceToSquared((float)generatorX + 0.5f, (float)generatorY + 0.5f, (float)x + 0.5f, (float)y + 0.5f) <= (float)(generatorRadius * generatorRadius);
    }

    public ArrayList<String> getItemsPowered() {
        ArrayList<String> result = new ArrayList<String>();
        for (String test : this.itemsPowered.keySet()) {
            result.add(test + this.itemsPowered.get(test));
        }
        result.sort(String::compareToIgnoreCase);
        return result;
    }

    public float getTotalPowerUsing() {
        return (float)((double)this.totalPowerUsing * SandboxOptions.instance.generatorFuelConsumption.getValue());
    }

    public String getTotalPowerUsingString() {
        return decimalFormat.format((double)this.totalPowerUsing * SandboxOptions.instance.generatorFuelConsumption.getValue());
    }

    public void setTotalPowerUsing(float totalPowerUsing) {
        this.totalPowerUsing = totalPowerUsing;
    }

    public String getSoundPrefix() {
        if (this.getSprite() == null) {
            return "Generator";
        }
        PropertyContainer props = this.getSprite().getProperties();
        if (props.has("GeneratorSound")) {
            return props.get("GeneratorSound");
        }
        return "Generator";
    }

    private void stopAllSounds() {
        if (GameServer.server) {
            return;
        }
        if (this.emitter == null) {
            return;
        }
        this.emitter.stopAll();
    }

    private void playGeneratorSound(String suffix) {
        if (GameServer.server) {
            return;
        }
        if (this.emitter == null) {
            this.emitter = IsoWorld.instance.getFreeEmitter((float)this.getXi() + 0.5f, (float)this.getYi() + 0.5f, this.getZi());
            IsoWorld.instance.takeOwnershipOfEmitter(this.emitter);
        }
        this.playGeneratorSound(this.emitter, suffix);
    }

    private void playGeneratorSound(BaseSoundEmitter emitter, String suffix) {
        emitter.playSoundImpl(this.getSoundPrefix() + suffix, this);
    }

    private void explode(IsoGridSquare isoGridSquare) {
        IsoFireManager.explode(isoGridSquare.getCell(), isoGridSquare, 100000);
    }
}

