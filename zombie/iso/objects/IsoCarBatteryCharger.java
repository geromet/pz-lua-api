/*
 * Decompiled with CFR 0.152.
 */
package zombie.iso.objects;

import java.io.IOException;
import java.nio.ByteBuffer;
import zombie.GameTime;
import zombie.UsedFromLua;
import zombie.core.Core;
import zombie.core.PerformanceSettings;
import zombie.core.network.ByteBufferReader;
import zombie.core.network.ByteBufferWriter;
import zombie.core.opengl.Shader;
import zombie.core.skinnedmodel.model.ItemModelRenderer;
import zombie.core.skinnedmodel.model.WorldItemModelDrawer;
import zombie.core.textures.ColorInfo;
import zombie.core.textures.Texture;
import zombie.inventory.InventoryItem;
import zombie.inventory.types.DrainableComboItem;
import zombie.iso.IItemProvider;
import zombie.iso.IsoCamera;
import zombie.iso.IsoCell;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoObject;
import zombie.iso.IsoWorld;
import zombie.iso.fboRenderChunk.FBORenderCell;
import zombie.iso.fboRenderChunk.FBORenderChunkManager;
import zombie.iso.objects.IsoGenerator;
import zombie.iso.sprite.IsoSprite;
import zombie.iso.sprite.IsoSpriteManager;
import zombie.network.GameServer;
import zombie.scripting.objects.SoundKey;

@UsedFromLua
public class IsoCarBatteryCharger
extends IsoObject
implements IItemProvider {
    protected InventoryItem item;
    protected InventoryItem battery;
    protected boolean activated;
    protected float lastUpdate = -1.0f;
    protected float chargeRate = 0.16666667f;
    protected IsoSprite chargerSprite;
    protected IsoSprite batterySprite;
    protected long sound;

    public IsoCarBatteryCharger(IsoCell cell) {
        super(cell);
    }

    public IsoCarBatteryCharger(InventoryItem item, IsoCell cell, IsoGridSquare square) {
        super(cell, square, (IsoSprite)null);
        if (item == null) {
            throw new NullPointerException("item is null");
        }
        this.item = item;
    }

    @Override
    public String getObjectName() {
        return "IsoCarBatteryCharger";
    }

    @Override
    public void load(ByteBuffer bb, int worldVersion, boolean isDebugSave) throws IOException {
        super.load(bb, worldVersion, isDebugSave);
        if (bb.get() != 0) {
            try {
                this.item = InventoryItem.loadItem(bb, worldVersion);
            }
            catch (Exception ex) {
                ex.printStackTrace();
            }
        }
        if (bb.get() != 0) {
            try {
                this.battery = InventoryItem.loadItem(bb, worldVersion);
            }
            catch (Exception ex) {
                ex.printStackTrace();
            }
        }
        this.activated = bb.get() != 0;
        this.lastUpdate = bb.getFloat();
        this.chargeRate = bb.getFloat();
    }

    @Override
    public void save(ByteBuffer bb, boolean isDebugSave) throws IOException {
        super.save(bb, isDebugSave);
        if (this.item == null) {
            assert (false);
            bb.put((byte)0);
        } else {
            bb.put((byte)1);
            this.item.saveWithSize(bb, false);
        }
        if (this.battery == null) {
            bb.put((byte)0);
        } else {
            bb.put((byte)1);
            this.battery.saveWithSize(bb, false);
        }
        bb.put(this.activated ? (byte)1 : 0);
        bb.putFloat(this.lastUpdate);
        bb.putFloat(this.chargeRate);
    }

    @Override
    public void addToWorld() {
        super.addToWorld();
        this.getCell().addToProcessIsoObject(this);
    }

    @Override
    public void removeFromWorld() {
        this.stopChargingSound();
        super.removeFromWorld();
    }

    @Override
    public void update() {
        float elapsedHours;
        boolean isPowered;
        super.update();
        if (!(this.battery instanceof DrainableComboItem)) {
            this.battery = null;
        }
        if (this.battery == null) {
            this.lastUpdate = -1.0f;
            this.setActivated(false);
            this.stopChargingSound();
            return;
        }
        boolean bl = isPowered = this.square != null && (this.square.haveElectricity() || this.square.hasGridPower() && this.square.getRoom() != null);
        if (!isPowered) {
            this.setActivated(false);
        }
        if (!this.activated) {
            this.lastUpdate = -1.0f;
            this.stopChargingSound();
            return;
        }
        this.startChargingSound();
        DrainableComboItem battery = (DrainableComboItem)this.battery;
        if (battery.getCurrentUsesFloat() >= 1.0f) {
            return;
        }
        float worldAgeHours = (float)GameTime.getInstance().getWorldAgeHours();
        if (this.lastUpdate < 0.0f) {
            this.lastUpdate = worldAgeHours;
        }
        if (this.lastUpdate > worldAgeHours) {
            this.lastUpdate = worldAgeHours;
        }
        if ((elapsedHours = worldAgeHours - this.lastUpdate) > 0.0f) {
            battery.setCurrentUses((int)((float)battery.getMaxUses() * Math.min(1.0f, battery.getCurrentUsesFloat() + this.chargeRate * elapsedHours)));
            this.lastUpdate = worldAgeHours;
        }
    }

    @Override
    public void render(float x, float y, float z, ColorInfo col, boolean bDoChild, boolean bWallLightingPass, Shader shader) {
        if (PerformanceSettings.fboRenderChunk && Core.getInstance().isOption3DGroundItem()) {
            return;
        }
        this.chargerSprite = this.configureSprite(this.item, this.chargerSprite);
        if (this.chargerSprite.hasNoTextures()) {
            return;
        }
        Texture tex = this.chargerSprite.getTextureForCurrentFrame(this.dir, this);
        if (tex == null) {
            return;
        }
        float dx = (float)tex.getWidthOrig() * this.chargerSprite.def.getScaleX() / 2.0f;
        float dy = (float)tex.getHeightOrig() * this.chargerSprite.def.getScaleY() * 3.0f / 4.0f;
        this.offsetY = 0.0f;
        this.offsetX = 0.0f;
        this.setAlpha(IsoCamera.frameState.playerIndex, 1.0f);
        float xoff = 0.5f;
        float yoff = 0.5f;
        float zoff = 0.0f;
        this.sx = 0.0f;
        this.item.setWorldZRotation(315.0f);
        ItemModelRenderer.RenderStatus status = WorldItemModelDrawer.renderMain(this.getItem(), this.getSquare(), this.getRenderSquare(), this.getX() + 0.5f, this.getY() + 0.5f, this.getZ() + 0.0f, -1.0f, -1.0f, true);
        if (status == ItemModelRenderer.RenderStatus.NoModel || status == ItemModelRenderer.RenderStatus.Failed) {
            this.chargerSprite.render(this, x + 0.5f, y + 0.5f, z + 0.0f, this.dir, this.offsetX + dx + (float)(8 * Core.tileScale), this.offsetY + dy + (float)(4 * Core.tileScale), col, true);
        }
        if (status == ItemModelRenderer.RenderStatus.Loading && PerformanceSettings.fboRenderChunk && FBORenderChunkManager.instance.isCaching()) {
            FBORenderCell.instance.handleDelayedLoading(this);
        }
        if (this.battery != null) {
            this.batterySprite = this.configureSprite(this.battery, this.batterySprite);
            if (this.batterySprite != null && !this.batterySprite.hasNoTextures()) {
                this.sx = 0.0f;
                this.getBattery().setWorldZRotation(90.0f);
                status = WorldItemModelDrawer.renderMain(this.getBattery(), this.getSquare(), this.getRenderSquare(), this.getX() + 0.75f, this.getY() + 0.75f, this.getZ() + 0.0f, -1.0f, -1.0f, true);
                if (status == ItemModelRenderer.RenderStatus.NoModel || status == ItemModelRenderer.RenderStatus.Failed) {
                    this.batterySprite.render(this, x + 0.5f, y + 0.5f, z + 0.0f, this.dir, this.offsetX + dx - 8.0f + (float)Core.tileScale, this.offsetY + dy - (float)(4 * Core.tileScale), col, true);
                }
                if (status == ItemModelRenderer.RenderStatus.Loading && PerformanceSettings.fboRenderChunk && FBORenderChunkManager.instance.isCaching()) {
                    FBORenderCell.instance.handleDelayedLoading(this);
                }
            }
        }
    }

    @Override
    public void renderObjectPicker(float x, float y, float z, ColorInfo lightInfo) {
    }

    @Override
    public boolean hasAnimatedAttachments() {
        return Core.getInstance().isOption3DGroundItem();
    }

    @Override
    public void renderAnimatedAttachments(float x, float y, float z, ColorInfo col) {
        this.chargerSprite = this.configureSprite(this.item, this.chargerSprite);
        if (this.chargerSprite.hasNoTextures()) {
            return;
        }
        Texture tex = this.chargerSprite.getTextureForCurrentFrame(this.dir, this);
        if (tex == null) {
            return;
        }
        float dx = (float)tex.getWidthOrig() * this.chargerSprite.def.getScaleX() / 2.0f;
        float dy = (float)tex.getHeightOrig() * this.chargerSprite.def.getScaleY() * 3.0f / 4.0f;
        this.offsetY = 0.0f;
        this.offsetX = 0.0f;
        this.setAlpha(IsoCamera.frameState.playerIndex, 1.0f);
        float xoff = 0.5f;
        float yoff = 0.5f;
        float zoff = 0.0f;
        this.sx = 0.0f;
        this.item.setWorldZRotation(315.0f);
        ItemModelRenderer.RenderStatus status = WorldItemModelDrawer.renderMain(this.getItem(), this.getSquare(), this.getRenderSquare(), this.getX() + 0.5f, this.getY() + 0.5f, this.getZ() + 0.0f, -1.0f, -1.0f, true);
        if (status == ItemModelRenderer.RenderStatus.NoModel || status == ItemModelRenderer.RenderStatus.Failed) {
            this.chargerSprite.render(this, x + 0.5f, y + 0.5f, z + 0.0f, this.dir, this.offsetX + dx + (float)(8 * Core.tileScale), this.offsetY + dy + (float)(4 * Core.tileScale), col, true);
        }
        if (status == ItemModelRenderer.RenderStatus.Loading && PerformanceSettings.fboRenderChunk && FBORenderChunkManager.instance.isCaching()) {
            FBORenderCell.instance.handleDelayedLoading(this);
        }
        if (this.battery != null) {
            this.batterySprite = this.configureSprite(this.battery, this.batterySprite);
            if (this.batterySprite != null && !this.batterySprite.hasNoTextures()) {
                this.sx = 0.0f;
                this.getBattery().setWorldZRotation(90.0f);
                status = WorldItemModelDrawer.renderMain(this.getBattery(), this.getSquare(), this.getRenderSquare(), this.getX() + 0.75f, this.getY() + 0.75f, this.getZ() + 0.0f, -1.0f, -1.0f, true);
                if (status == ItemModelRenderer.RenderStatus.NoModel || status == ItemModelRenderer.RenderStatus.Failed) {
                    this.batterySprite.render(this, x + 0.5f, y + 0.5f, z + 0.0f, this.dir, this.offsetX + dx - 8.0f + (float)Core.tileScale, this.offsetY + dy - (float)(4 * Core.tileScale), col, true);
                }
                if (status == ItemModelRenderer.RenderStatus.Loading && PerformanceSettings.fboRenderChunk && FBORenderChunkManager.instance.isCaching()) {
                    FBORenderCell.instance.handleDelayedLoading(this);
                }
            }
        }
    }

    private IsoSprite configureSprite(InventoryItem item, IsoSprite sprite) {
        Texture tex;
        String name = item.getWorldTexture();
        try {
            tex = Texture.getSharedTexture(name);
            if (tex == null) {
                name = item.getTex().getName();
            }
        }
        catch (Exception ex) {
            name = "media/inventory/world/WItem_Sack.png";
        }
        tex = Texture.getSharedTexture(name);
        boolean setScale = false;
        if (sprite == null) {
            sprite = IsoSprite.CreateSprite(IsoSpriteManager.instance);
        }
        if (sprite.currentAnim != null || sprite.texture != tex) {
            sprite.LoadSingleTexture(name);
            setScale = true;
        }
        if (setScale) {
            if (item.getScriptItem() == null) {
                sprite.def.scaleAspect(tex.getWidthOrig(), tex.getHeightOrig(), 16 * Core.tileScale, 16 * Core.tileScale);
            } else if (this.battery != null && this.battery.getScriptItem() != null) {
                float scale = this.battery.getScriptItem().scaleWorldIcon * ((float)Core.tileScale / 2.0f);
                sprite.def.setScale(scale, scale);
            }
        }
        return sprite;
    }

    @Override
    public void syncIsoObjectSend(ByteBufferWriter b) {
        b.putInt(this.square.getX());
        b.putInt(this.square.getY());
        b.putInt(this.square.getZ());
        b.putByte(this.getObjectIndex());
        b.putBoolean(true);
        b.putBoolean(false);
        if (b.putBoolean(this.battery != null)) {
            try {
                this.battery.saveWithSize(b.bb, false);
            }
            catch (IOException ex) {
                ex.printStackTrace();
            }
        }
        b.putBoolean(this.activated);
        b.putFloat(this.chargeRate);
    }

    @Override
    public void syncIsoObjectReceive(ByteBufferReader bb) {
        if (bb.getBoolean()) {
            try {
                this.battery = InventoryItem.loadItem(bb.bb, 244);
            }
            catch (Exception ex) {
                ex.printStackTrace();
            }
        } else {
            this.battery = null;
        }
        this.activated = bb.getBoolean();
        this.chargeRate = bb.getFloat();
    }

    @Override
    public InventoryItem getItem() {
        return this.item;
    }

    public InventoryItem getBattery() {
        return this.battery;
    }

    public void setBattery(InventoryItem battery) {
        if (battery != null) {
            if (!(battery instanceof DrainableComboItem)) {
                throw new IllegalArgumentException("battery isn't DrainableComboItem");
            }
            if (this.battery != null) {
                throw new IllegalStateException("battery already inserted");
            }
        }
        this.battery = battery;
        this.invalidateRenderChunkLevel(256L);
    }

    public boolean isActivated() {
        return this.activated;
    }

    public void setActivated(boolean activated) {
        this.activated = activated;
        IsoGenerator.updateGenerator(this.square);
    }

    public float getChargeRate() {
        return this.chargeRate;
    }

    public void setChargeRate(float chargeRate) {
        if (chargeRate <= 0.0f) {
            throw new IllegalArgumentException("chargeRate <= 0.0f");
        }
        this.chargeRate = chargeRate;
    }

    private void startChargingSound() {
        if (GameServer.server) {
            return;
        }
        if (this.getObjectIndex() == -1) {
            return;
        }
        if (this.sound == -1L) {
            return;
        }
        if (this.emitter == null) {
            this.emitter = IsoWorld.instance.getFreeEmitter((float)this.square.x + 0.5f, (float)this.square.y + 0.5f, this.square.z);
            IsoWorld.instance.takeOwnershipOfEmitter(this.emitter);
        }
        if (!this.emitter.isPlaying(this.sound)) {
            this.sound = this.emitter.playSoundImpl(SoundKey.CAR_BATTERY_CHARGER_RUNNING.getSoundName(), (IsoObject)null);
            if (this.sound == 0L) {
                this.sound = -1L;
            }
        }
        this.emitter.tick();
    }

    private void stopChargingSound() {
        if (GameServer.server) {
            return;
        }
        if (this.emitter == null) {
            return;
        }
        this.emitter.stopOrTriggerSound(this.sound);
        this.sound = 0L;
        IsoWorld.instance.returnOwnershipOfEmitter(this.emitter);
        this.emitter = null;
    }

    public Texture getTexture() {
        return this.chargerSprite.getTextureForCurrentFrame(this.getDir(), this);
    }
}

