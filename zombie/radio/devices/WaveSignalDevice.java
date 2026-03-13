/*
 * Decompiled with CFR 0.152.
 */
package zombie.radio.devices;

import zombie.Lua.LuaEventManager;
import zombie.UsedFromLua;
import zombie.characters.IsoPlayer;
import zombie.chat.ChatManager;
import zombie.inventory.InventoryItem;
import zombie.iso.IsoGridSquare;
import zombie.iso.objects.IsoWaveSignal;
import zombie.radio.ZomboidRadio;
import zombie.radio.devices.DeviceData;
import zombie.scripting.objects.CharacterTrait;
import zombie.ui.UIFont;
import zombie.vehicles.VehiclePart;

@UsedFromLua
public interface WaveSignalDevice {
    public DeviceData getDeviceData();

    public void setDeviceData(DeviceData var1);

    public float getDelta();

    public void setDelta(float var1);

    public IsoGridSquare getSquare();

    public float getX();

    public float getY();

    public float getZ();

    public void AddDeviceText(String var1, float var2, float var3, float var4, String var5, String var6, int var7);

    public boolean HasPlayerInRange();

    default public void AddDeviceText(IsoPlayer player, String line, float r, float g, float b, String guid, String codes, int distance) {
        if (this.getDeviceData() != null && this.getDeviceData().getDeviceVolume() > 0.0f) {
            if (!ZomboidRadio.isStaticSound(line)) {
                this.getDeviceData().doReceiveSignal(distance);
            }
            if (player != null && player.isLocalPlayer() && !player.hasTrait(CharacterTrait.DEAF)) {
                if (this.getDeviceData().getParent() instanceof InventoryItem && player.isEquipped((InventoryItem)((Object)this.getDeviceData().getParent()))) {
                    player.getChatElement().addChatLine(line, r, g, b, UIFont.Medium, this.getDeviceData().getDeviceVolumeRange(), "default", true, true, true, false, false, true);
                } else if (this.getDeviceData().getParent() instanceof IsoWaveSignal) {
                    ((IsoWaveSignal)this.getDeviceData().getParent()).getChatElement().addChatLine(line, r, g, b, UIFont.Medium, this.getDeviceData().getDeviceVolumeRange(), "default", true, true, true, true, true, true);
                } else if (this.getDeviceData().getParent() instanceof VehiclePart) {
                    ((VehiclePart)this.getDeviceData().getParent()).getChatElement().addChatLine(line, r, g, b, UIFont.Medium, this.getDeviceData().getDeviceVolumeRange(), "default", true, true, true, true, true, true);
                }
                if (ZomboidRadio.isStaticSound(line)) {
                    ChatManager.getInstance().showStaticRadioSound(line);
                } else {
                    ChatManager.getInstance().showRadioMessage(line, this.getDeviceData().getChannel());
                }
                if (codes != null) {
                    LuaEventManager.triggerEvent("OnDeviceText", guid, codes, -1, -1, -1, line, this);
                }
            }
        }
    }
}

