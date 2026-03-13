/*
 * Decompiled with CFR 0.152.
 */
package zombie.iso.objects;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import zombie.GameTime;
import zombie.GameWindow;
import zombie.Lua.LuaEventManager;
import zombie.UsedFromLua;
import zombie.characters.IsoPlayer;
import zombie.characters.Talker;
import zombie.chat.ChatElement;
import zombie.chat.ChatElementOwner;
import zombie.core.Core;
import zombie.core.properties.PropertyContainer;
import zombie.core.random.Rand;
import zombie.inventory.InventoryItemFactory;
import zombie.inventory.types.Radio;
import zombie.iso.IsoCamera;
import zombie.iso.IsoCell;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoLightSource;
import zombie.iso.IsoObject;
import zombie.iso.IsoUtils;
import zombie.iso.IsoWorld;
import zombie.iso.objects.IsoRadio;
import zombie.iso.objects.IsoTelevision;
import zombie.iso.sprite.IsoSprite;
import zombie.network.GameClient;
import zombie.network.GameServer;
import zombie.radio.ZomboidRadio;
import zombie.radio.devices.DeviceData;
import zombie.radio.devices.PresetEntry;
import zombie.radio.devices.WaveSignalDevice;
import zombie.scripting.objects.CharacterTrait;
import zombie.ui.UIFont;

@UsedFromLua
public class IsoWaveSignal
extends IsoObject
implements WaveSignalDevice,
ChatElementOwner,
Talker {
    protected IsoLightSource lightSource;
    protected boolean lightWasRemoved;
    protected int lightSourceRadius = 4;
    protected float nextLightUpdate;
    protected float lightUpdateCnt;
    protected DeviceData deviceData;
    protected boolean displayRange;
    protected boolean hasPlayerInRange;
    protected GameTime gameTime;
    protected ChatElement chatElement;
    protected String talkerType = "device";
    protected static Map<String, DeviceData> deviceDataCache = new HashMap<String, DeviceData>();

    public IsoWaveSignal(IsoCell cell) {
        super(cell);
        this.init(true);
    }

    public IsoWaveSignal(IsoCell cell, IsoGridSquare sq, IsoSprite spr) {
        super(cell, sq, spr);
        this.init(false);
    }

    protected void init(boolean objectFromBinary) {
        this.chatElement = new ChatElement(this, 5, this.talkerType);
        this.gameTime = GameTime.getInstance();
        if (!objectFromBinary) {
            PropertyContainer props;
            if (this.sprite != null && this.sprite.getProperties() != null && (props = this.sprite.getProperties()).has("CustomItem") && props.get("CustomItem") != null) {
                this.deviceData = this.cloneDeviceDataFromItem(props.get("CustomItem"));
            }
            if (!GameClient.client && this.deviceData != null) {
                this.deviceData.generatePresets();
                this.deviceData.setDeviceVolume(Rand.Next(0.1f, 1.0f));
                this.deviceData.setRandomChannel();
                if (Rand.Next(100) <= 35 && !"Tutorial".equals(Core.gameMode)) {
                    this.deviceData.setTurnedOnRaw(true);
                    if (this instanceof IsoRadio) {
                        this.deviceData.setInitialPower();
                        if (this.deviceData.getIsBatteryPowered() && this.deviceData.getPower() <= 0.0f) {
                            this.deviceData.setTurnedOnRaw(false);
                        }
                    }
                }
            }
        }
        if (this.deviceData == null) {
            this.deviceData = new DeviceData(this);
        }
        this.deviceData.setParent(this);
    }

    public DeviceData cloneDeviceDataFromItem(String itemfull) {
        if (itemfull != null) {
            Radio radio;
            DeviceData d;
            if (deviceDataCache.containsKey(itemfull) && deviceDataCache.get(itemfull) != null) {
                return deviceDataCache.get(itemfull).getClone();
            }
            Object item = InventoryItemFactory.CreateItem(itemfull);
            if (item != null && item instanceof Radio && (d = (radio = (Radio)item).getDeviceData()) != null) {
                deviceDataCache.put(itemfull, d);
                return d.getClone();
            }
        }
        return null;
    }

    public boolean hasChatToDisplay() {
        return this.chatElement.getHasChatToDisplay();
    }

    @Override
    public boolean HasPlayerInRange() {
        return this.hasPlayerInRange;
    }

    @Override
    public float getDelta() {
        if (this.deviceData != null) {
            return this.deviceData.getPower();
        }
        return 0.0f;
    }

    @Override
    public void setDelta(float delta) {
        if (this.deviceData != null) {
            this.deviceData.setPower(delta);
        }
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
    public boolean IsSpeaking() {
        return this.chatElement.IsSpeaking();
    }

    @Override
    public String getTalkerType() {
        return this.chatElement.getTalkerType();
    }

    public void setTalkerType(String type) {
        this.talkerType = type == null ? "" : type;
        this.chatElement.setTalkerType(this.talkerType);
    }

    @Override
    public String getSayLine() {
        return this.chatElement.getSayLine();
    }

    @Override
    public void Say(String line) {
        this.AddDeviceText(line, 1.0f, 1.0f, 1.0f, null, null, -1, false);
    }

    @Override
    public void AddDeviceText(String line, float r, float g, float b, String guid, String codes, int distance) {
        this.AddDeviceText(line, r, g, b, guid, codes, distance, true);
    }

    public void AddDeviceText(String line, int r, int g, int b, String guid, String codes, int distance) {
        this.AddDeviceText(line, (float)r / 255.0f, (float)g / 255.0f, (float)b / 255.0f, guid, codes, distance, true);
    }

    public void AddDeviceText(String line, int r, int g, int b, String guid, String codes, int distance, boolean attractZombies) {
        this.AddDeviceText(line, (float)r / 255.0f, (float)g / 255.0f, (float)b / 255.0f, guid, codes, distance, attractZombies);
    }

    public void AddDeviceText(String line, float r, float g, float b, String guid, String codes, int distance, boolean attractZombies) {
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
    public void renderlast() {
        if (this.chatElement.getHasChatToDisplay()) {
            if (this.getDeviceData() != null && !this.getDeviceData().getIsTurnedOn()) {
                this.chatElement.clear(IsoCamera.frameState.playerIndex);
                return;
            }
            IsoPlayer player = IsoPlayer.getInstance();
            if (this instanceof IsoRadio && player.hasTrait(CharacterTrait.DEAF)) {
                this.chatElement.clear(IsoCamera.frameState.playerIndex);
                return;
            }
            if (this instanceof IsoTelevision && player.hasTrait(CharacterTrait.DEAF) && this.square != null && !this.square.isSeen(IsoPlayer.getPlayerIndex())) {
                this.chatElement.clear(IsoCamera.frameState.playerIndex);
                return;
            }
            float sx = IsoUtils.XToScreen(this.getX(), this.getY(), this.getZ(), 0);
            float sy = IsoUtils.YToScreen(this.getX(), this.getY(), this.getZ(), 0);
            sx = sx - IsoCamera.getOffX() - this.offsetX;
            sy = sy - IsoCamera.getOffY() - this.offsetY;
            sx += (float)(32 * Core.tileScale);
            sy += (float)(50 * Core.tileScale);
            sx /= Core.getInstance().getZoom(IsoPlayer.getPlayerIndex());
            sy /= Core.getInstance().getZoom(IsoPlayer.getPlayerIndex());
            this.chatElement.renderBatched(IsoPlayer.getPlayerIndex(), (int)(sx += IsoCamera.cameras[IsoCamera.frameState.playerIndex].fixJigglyModelsX), (int)(sy += IsoCamera.cameras[IsoCamera.frameState.playerIndex].fixJigglyModelsY));
        }
    }

    public void renderlastold2() {
        if (this.chatElement.getHasChatToDisplay()) {
            float sx = IsoUtils.XToScreen(this.getX(), this.getY(), this.getZ(), 0);
            float sy = IsoUtils.YToScreen(this.getX(), this.getY(), this.getZ(), 0);
            sx = sx - IsoCamera.getOffX() - this.offsetX;
            sy = sy - IsoCamera.getOffY() - this.offsetY;
            sx += 28.0f;
            sy += 180.0f;
            sx /= Core.getInstance().getZoom(IsoPlayer.getPlayerIndex());
            sy /= Core.getInstance().getZoom(IsoPlayer.getPlayerIndex());
            this.chatElement.renderBatched(IsoPlayer.getPlayerIndex(), (int)(sx += (float)IsoCamera.getScreenLeft(IsoPlayer.getPlayerIndex())), (int)(sy += (float)IsoCamera.getScreenTop(IsoPlayer.getPlayerIndex())));
        }
    }

    protected boolean playerWithinBounds(IsoPlayer player, float dist) {
        if (player == null) {
            return false;
        }
        return (player.getX() > this.getX() - dist || player.getX() < this.getX() + dist) && (player.getY() > this.getY() - dist || player.getY() < this.getY() + dist);
    }

    @Override
    public void update() {
        if (this.deviceData == null) {
            return;
        }
        if (!GameServer.server && !GameClient.client || GameServer.server) {
            this.deviceData.update(true, this.hasPlayerInRange);
        } else {
            this.deviceData.updateSimple();
        }
        if (!GameServer.server) {
            this.hasPlayerInRange = false;
            if (this.deviceData.getIsTurnedOn()) {
                IsoPlayer player = IsoPlayer.getInstance();
                if (this.playerWithinBounds(player, (float)this.deviceData.getDeviceVolumeRange() * 0.6f)) {
                    this.hasPlayerInRange = true;
                }
                this.updateLightSource();
            } else {
                this.removeLightSourceFromWorld();
            }
            this.chatElement.setHistoryRange((float)this.deviceData.getDeviceVolumeRange() * 0.6f);
            this.chatElement.update();
        } else {
            this.hasPlayerInRange = false;
        }
    }

    protected void updateLightSource() {
    }

    @Override
    protected void removeLightSourceFromWorld() {
        if (this.lightSource != null) {
            IsoWorld.instance.currentCell.removeLamppost(this.lightSource);
            this.lightSource = null;
        }
    }

    @Override
    public void load(ByteBuffer input, int worldVersion, boolean isDebugSave) throws IOException {
        super.load(input, worldVersion, isDebugSave);
        if (this.deviceData == null) {
            this.deviceData = new DeviceData(this);
        }
        if (input.get() != 0) {
            this.deviceData.load(input, worldVersion, true);
        }
        this.deviceData.setParent(this);
    }

    @Override
    public void save(ByteBuffer output, boolean isDebugSave) throws IOException {
        super.save(output, isDebugSave);
        if (this.deviceData != null) {
            output.put((byte)1);
            this.deviceData.save(output, true);
        } else {
            output.put((byte)0);
        }
    }

    @Override
    public void addToWorld() {
        ZomboidRadio.getInstance().RegisterDevice(this);
        if (this.getCell() != null) {
            this.getCell().addToStaticUpdaterObjectList(this);
        }
        super.addToWorld();
    }

    @Override
    public void removeFromWorld() {
        ZomboidRadio.getInstance().UnRegisterDevice(this);
        this.removeLightSourceFromWorld();
        this.lightSource = null;
        if (this.deviceData != null) {
            this.deviceData.cleanSoundsAndEmitter();
        }
        super.removeFromWorld();
    }

    @Override
    public void removeFromSquare() {
        super.removeFromSquare();
        this.square = null;
    }

    @Override
    public void saveState(ByteBuffer bb) throws IOException {
        if (this.deviceData == null) {
            return;
        }
        ArrayList<PresetEntry> presets = this.deviceData.getDevicePresets().getPresets();
        bb.putInt(presets.size());
        for (int i = 0; i < presets.size(); ++i) {
            PresetEntry preset = presets.get(i);
            GameWindow.WriteString(bb, preset.getName());
            bb.putInt(preset.getFrequency());
        }
        bb.put((byte)(this.deviceData.getIsTurnedOn() ? 1 : 0));
        bb.putInt(this.deviceData.getChannel());
        bb.putFloat(this.deviceData.getDeviceVolume());
    }

    @Override
    public void loadState(ByteBuffer bb) throws IOException {
        ArrayList<PresetEntry> presets = this.deviceData.getDevicePresets().getPresets();
        int numPresets = bb.getInt();
        for (int i = 0; i < numPresets; ++i) {
            String name = GameWindow.ReadString(bb);
            int frequency = bb.getInt();
            if (i < presets.size()) {
                PresetEntry preset = presets.get(i);
                preset.setName(name);
                preset.setFrequency(frequency);
                continue;
            }
            this.deviceData.getDevicePresets().addPreset(name, frequency);
        }
        while (presets.size() > numPresets) {
            this.deviceData.getDevicePresets().removePreset(numPresets);
        }
        this.deviceData.setTurnedOnRaw(bb.get() != 0);
        this.deviceData.setChannelRaw(bb.getInt());
        this.deviceData.setDeviceVolumeRaw(bb.getFloat());
    }

    public ChatElement getChatElement() {
        return this.chatElement;
    }

    public static void Reset() {
        deviceDataCache.clear();
    }
}

