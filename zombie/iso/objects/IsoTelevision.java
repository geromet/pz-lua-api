/*
 * Decompiled with CFR 0.152.
 */
package zombie.iso.objects;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import zombie.GameTime;
import zombie.UsedFromLua;
import zombie.characters.IsoPlayer;
import zombie.core.Core;
import zombie.core.properties.IsoPropertyType;
import zombie.core.random.Rand;
import zombie.iso.IsoCell;
import zombie.iso.IsoDirections;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoLightSource;
import zombie.iso.IsoWorld;
import zombie.iso.LightingJNI;
import zombie.iso.objects.IsoWaveSignal;
import zombie.iso.sprite.IsoSprite;
import zombie.iso.sprite.IsoSpriteManager;

@UsedFromLua
public class IsoTelevision
extends IsoWaveSignal {
    private Screens currentScreen = Screens.OFFSCREEN;
    private int spriteIndex;
    private boolean hasSetupScreens;
    private boolean tickIsLightUpdate;
    protected ArrayList<IsoSprite> screenSprites = new ArrayList();
    private IsoSprite cacheObjectSprite;
    protected IsoDirections facing;

    public IsoTelevision(IsoCell cell) {
        super(cell);
    }

    public IsoTelevision(IsoCell cell, IsoGridSquare sq, IsoSprite spr) {
        super(cell, sq, spr);
    }

    @Override
    public String getObjectName() {
        return "Television";
    }

    private void setupDefaultScreens() {
        this.hasSetupScreens = true;
        this.cacheObjectSprite = this.sprite;
        if (this.screenSprites.isEmpty()) {
            for (int i = 16; i <= 64; i += 16) {
                IsoSprite sprite = IsoSprite.getSprite(IsoSpriteManager.instance, this.sprite.getName(), i);
                if (sprite == null) continue;
                this.addTvScreenSprite(sprite);
            }
        }
        this.facing = null;
        if (this.sprite != null && this.sprite.getProperties().has(IsoPropertyType.FACING)) {
            String facing;
            switch (facing = this.sprite.getProperties().get(IsoPropertyType.FACING)) {
                case "N": {
                    this.facing = IsoDirections.N;
                    break;
                }
                case "S": {
                    this.facing = IsoDirections.S;
                    break;
                }
                case "W": {
                    this.facing = IsoDirections.W;
                    break;
                }
                case "E": {
                    this.facing = IsoDirections.E;
                }
            }
        }
    }

    @Override
    public void update() {
        super.update();
        if (this.cacheObjectSprite != null && this.cacheObjectSprite != this.sprite) {
            this.hasSetupScreens = false;
            this.screenSprites.clear();
            this.currentScreen = Screens.OFFSCREEN;
            this.nextLightUpdate = 0.0f;
        }
        if (!this.hasSetupScreens) {
            this.setupDefaultScreens();
        }
        this.updateTvScreen();
    }

    @Override
    protected void updateLightSource() {
        this.tickIsLightUpdate = false;
        if (this.lightSource == null) {
            this.lightSource = new IsoLightSource(this.square.getX(), this.square.getY(), this.square.getZ(), 0.0f, 0.0f, 1.0f, this.lightSourceRadius);
            this.lightWasRemoved = true;
        }
        if (this.lightWasRemoved) {
            IsoWorld.instance.currentCell.addLamppost(this.lightSource);
            IsoGridSquare.recalcLightTime = -1.0f;
            ++Core.dirtyGlobalLightsCount;
            GameTime.instance.lightSourceUpdate = 100.0f;
            this.lightWasRemoved = false;
        }
        this.lightUpdateCnt += GameTime.getInstance().getMultiplier();
        if (this.lightUpdateCnt >= this.nextLightUpdate) {
            float interval;
            float intensBase = 0.0f;
            if (!this.hasChatToDisplay()) {
                intensBase = 0.6f;
                interval = Rand.Next(200.0f, 400.0f);
            } else {
                interval = Rand.Next(15.0f, 300.0f);
            }
            float intensity = Rand.Next(intensBase, 1.0f);
            this.tickIsLightUpdate = true;
            float r = 0.58f + 0.25f * intensity;
            float gb = Rand.Next(0.65f, 0.85f);
            int radius = 1 + (int)((float)(this.lightSourceRadius - 1) * intensity);
            IsoGridSquare.recalcLightTime = -1.0f;
            GameTime.instance.lightSourceUpdate = 100.0f;
            this.lightSource.setRadius(radius);
            this.lightSource.setR(r);
            this.lightSource.setG(gb);
            this.lightSource.setB(gb);
            if (LightingJNI.init && this.lightSource.id != 0) {
                LightingJNI.setLightColor(this.lightSource.id, this.lightSource.getR(), this.lightSource.getG(), this.lightSource.getB());
            }
            this.lightUpdateCnt = 0.0f;
            this.nextLightUpdate = interval;
        }
    }

    private void setScreen(Screens screen) {
        if (screen == Screens.OFFSCREEN) {
            this.currentScreen = Screens.OFFSCREEN;
            if (this.overlaySprite != null) {
                this.overlaySprite = null;
            }
            return;
        }
        if (this.currentScreen != screen || screen == Screens.ALTERNATESCREEN) {
            this.currentScreen = screen;
            IsoSprite sprite = null;
            switch (screen.ordinal()) {
                case 1: {
                    if (this.screenSprites.isEmpty()) break;
                    sprite = this.screenSprites.get(0);
                    break;
                }
                case 2: {
                    if (this.screenSprites.size() <= 1) break;
                    sprite = this.screenSprites.get(1);
                    break;
                }
                case 3: {
                    if (this.screenSprites.size() < 2) break;
                    if (this.screenSprites.size() == 2) {
                        sprite = this.screenSprites.get(1);
                        break;
                    }
                    ++this.spriteIndex;
                    if (this.spriteIndex < 1) {
                        this.spriteIndex = 1;
                    }
                    if (this.spriteIndex > this.screenSprites.size() - 1) {
                        this.spriteIndex = 1;
                    }
                    sprite = this.screenSprites.get(this.spriteIndex);
                }
            }
            this.overlaySprite = sprite;
        }
    }

    protected void updateTvScreen() {
        IsoSprite overlaySprite1 = this.overlaySprite;
        if (this.deviceData != null && this.deviceData.getIsTurnedOn() && !this.screenSprites.isEmpty()) {
            if (this.deviceData.isReceivingSignal() || this.deviceData.isPlayingMedia()) {
                if (this.tickIsLightUpdate || this.currentScreen != Screens.ALTERNATESCREEN) {
                    this.setScreen(Screens.ALTERNATESCREEN);
                }
            } else {
                this.setScreen(Screens.TESTSCREEN);
            }
        } else if (this.currentScreen != Screens.OFFSCREEN) {
            this.setScreen(Screens.OFFSCREEN);
        }
        if (overlaySprite1 != this.overlaySprite) {
            this.invalidateRenderChunkLevel(256L);
        }
    }

    public void addTvScreenSprite(IsoSprite sprite) {
        this.screenSprites.add(sprite);
    }

    public void clearTvScreenSprites() {
        this.screenSprites.clear();
    }

    public void removeTvScreenSprite(IsoSprite sprite) {
        this.screenSprites.remove(sprite);
    }

    @Override
    public void load(ByteBuffer input, int worldVersion, boolean isDebugSave) throws IOException {
        super.load(input, worldVersion, isDebugSave);
        this.overlaySprite = null;
    }

    public boolean isFacing(IsoPlayer player) {
        if (player == null || !player.isLocalPlayer()) {
            return false;
        }
        if (this.getObjectIndex() == -1) {
            return false;
        }
        if (!this.square.isCanSee(player.playerIndex)) {
            return false;
        }
        if (this.facing == null) {
            return false;
        }
        switch (this.facing) {
            case N: {
                if (player.getY() >= (float)this.square.y) {
                    return false;
                }
                return player.dir == IsoDirections.SW || player.dir == IsoDirections.S || player.dir == IsoDirections.SE;
            }
            case S: {
                if (player.getY() < (float)(this.square.y + 1)) {
                    return false;
                }
                return player.dir == IsoDirections.NW || player.dir == IsoDirections.N || player.dir == IsoDirections.NE;
            }
            case W: {
                if (player.getX() >= (float)this.square.x) {
                    return false;
                }
                return player.dir == IsoDirections.SE || player.dir == IsoDirections.E || player.dir == IsoDirections.NE;
            }
            case E: {
                if (player.getX() < (float)(this.square.x + 1)) {
                    return false;
                }
                return player.dir == IsoDirections.SW || player.dir == IsoDirections.W || player.dir == IsoDirections.NW;
            }
        }
        return false;
    }

    private static enum Screens {
        OFFSCREEN,
        TESTSCREEN,
        DEFAULTSCREEN,
        ALTERNATESCREEN;

    }
}

