/*
 * Decompiled with CFR 0.152.
 */
package zombie.iso.objects;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import se.krka.kahlua.vm.KahluaTable;
import zombie.GameTime;
import zombie.Lua.LuaEventManager;
import zombie.SandboxOptions;
import zombie.UsedFromLua;
import zombie.core.Core;
import zombie.core.PerformanceSettings;
import zombie.core.math.PZMath;
import zombie.core.network.ByteBufferReader;
import zombie.core.network.ByteBufferWriter;
import zombie.core.opengl.Shader;
import zombie.core.properties.IsoObjectChange;
import zombie.core.random.Rand;
import zombie.core.textures.ColorInfo;
import zombie.core.textures.Texture;
import zombie.iso.IsoCamera;
import zombie.iso.IsoCell;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoHeatSource;
import zombie.iso.IsoLightSource;
import zombie.iso.IsoMovingObject;
import zombie.iso.IsoObject;
import zombie.iso.IsoWorld;
import zombie.iso.SpriteDetails.IsoFlagType;
import zombie.iso.areas.SafeHouse;
import zombie.iso.objects.IsoFireManager;
import zombie.iso.objects.IsoWorldInventoryObject;
import zombie.iso.objects.RainManager;
import zombie.iso.sprite.IsoSprite;
import zombie.iso.sprite.IsoSpriteInstance;
import zombie.network.GameClient;
import zombie.network.GameServer;
import zombie.network.ServerOptions;
import zombie.ui.TutorialManager;

@UsedFromLua
public class IsoFire
extends IsoObject {
    public static final int NUM_FRAMES_FIRE = 30;
    public static final int NUM_FRAMES_SMOKE = 30;
    private static final ColorInfo tempColorInfo = new ColorInfo();
    private static final int DefaultEnergyRequirement = 30;
    private static final int VegetationEnergyBonus = -15;
    private static final int InteriorEnergyRequirement = 40;
    private static final int TutorialEnergyDivisor = 4;
    public static final int MaxLife = 3000;
    public static final int MinLife = 800;
    public int age;
    public int energy;
    public int life;
    public int lifeStage;
    public int lifeStageDuration;
    public int lifeStageTimer;
    public int spreadDelay;
    public int spreadTimer;
    public int numFlameParticles;
    public boolean perm;
    public boolean smoke;
    public IsoLightSource lightSource;
    public int lightRadius = 1;
    public float lightOscillator;
    private IsoHeatSource heatSource;
    private long savedUpdateTime = -1L;
    private float accum;
    private short[] soffX = new short[2];
    private short[] soffY = new short[2];

    public IsoFire(IsoCell cell) {
        super(cell);
    }

    public IsoFire(IsoCell cell, IsoGridSquare gridSquare) {
        super(cell);
        this.square = gridSquare;
        this.perm = true;
    }

    @Override
    public String getObjectName() {
        return "Fire";
    }

    @Override
    public void save(ByteBuffer output, boolean isDebugSave) throws IOException {
        ArrayList anims = this.attachedAnimSprite;
        this.attachedAnimSprite = null;
        super.save(output, isDebugSave);
        this.attachedAnimSprite = anims;
        this.sprite = null;
        output.putInt(this.life);
        output.putInt(this.spreadDelay);
        output.putInt(this.lifeStage - 1);
        output.putInt(this.lifeStageTimer);
        output.putInt(this.lifeStageDuration);
        output.putInt(this.energy);
        output.putInt(this.numFlameParticles);
        output.putInt(this.spreadTimer);
        output.putInt(this.age);
        output.put((byte)(this.perm ? 1 : 0));
        output.put((byte)this.lightRadius);
        output.put((byte)(this.smoke ? 1 : 0));
        output.putLong(GameTime.getInstance().getCalender().getTimeInMillis());
    }

    @Override
    public void load(ByteBuffer b, int worldVersion, boolean isDebugSave) throws IOException {
        super.load(b, worldVersion, isDebugSave);
        this.sprite = null;
        this.life = b.getInt();
        this.spreadDelay = b.getInt();
        this.lifeStage = b.getInt();
        this.lifeStageTimer = b.getInt();
        this.lifeStageDuration = b.getInt();
        this.energy = b.getInt();
        this.numFlameParticles = b.getInt();
        this.spreadTimer = b.getInt();
        this.age = b.getInt();
        this.perm = b.get() != 0;
        this.lightRadius = b.get() & 0xFF;
        boolean bl = this.smoke = b.get() != 0;
        if (worldVersion >= 242) {
            this.savedUpdateTime = b.getLong();
        }
        if (this.perm) {
            this.AttachAnim("Fire", "01", 30, 0.5f, 1 * Core.tileScale, -1 * Core.tileScale, true, 0, false, 0.7f, IsoFireManager.FIRE_TINT_MOD, true);
            return;
        }
        if (this.numFlameParticles == 0) {
            this.numFlameParticles = 1;
        }
        switch (this.lifeStage) {
            case -1: {
                this.lifeStage = 0;
                for (int i = 0; i < this.numFlameParticles; ++i) {
                    this.AttachAnim("Fire", "01", 30, 0.5f, 0, 0, true, 0, false, 0.7f, IsoFireManager.FIRE_TINT_MOD, true);
                }
                break;
            }
            case 0: {
                this.lifeStage = 1;
                this.lifeStageTimer = this.lifeStageDuration;
                this.AttachAnim("Fire", "02", 30, 0.5f, 0, 0, true, 0, false, 0.7f, IsoFireManager.FIRE_TINT_MOD, true);
                break;
            }
            case 1: {
                this.lifeStage = 2;
                this.lifeStageTimer = this.lifeStageDuration;
                this.AttachAnim("Smoke", "01", 30, IsoFireManager.smokeAnimDelay, 0, 0, true, 0, false, 0.7f, IsoFireManager.smokeTintMod, true);
                this.AttachAnim("Fire", "03", 30, 0.5f, 0, 0, true, 0, false, 0.7f, IsoFireManager.FIRE_TINT_MOD, true);
                break;
            }
            case 2: {
                this.lifeStage = 3;
                this.lifeStageTimer = this.lifeStageDuration / 3;
                this.RemoveAttachedAnims();
                this.AttachAnim("Smoke", "02", 30, IsoFireManager.smokeAnimDelay, 0, 0, true, 0, false, 0.7f, IsoFireManager.smokeTintMod, true);
                this.AttachAnim("Fire", "02", 30, 0.5f, 0, 0, true, 0, false, 0.7f, IsoFireManager.FIRE_TINT_MOD, true);
                break;
            }
            case 3: {
                this.lifeStage = 4;
                this.lifeStageTimer = this.lifeStageDuration / 3;
                this.RemoveAttachedAnims();
                if (this.smoke) {
                    this.AttachAnim("Smoke", "03", 30, IsoFireManager.smokeAnimDelay, 0, 0, true, 0, false, 0.7f, IsoFireManager.smokeTintMod, true);
                    break;
                }
                this.AttachAnim("Smoke", "03", 30, IsoFireManager.smokeAnimDelay, 0, 0, true, 0, false, 0.7f, IsoFireManager.smokeTintMod, true);
                this.AttachAnim("Fire", "01", 30, 0.5f, 0, 0, true, 0, false, 0.7f, IsoFireManager.FIRE_TINT_MOD, true);
                break;
            }
            case 4: {
                this.lifeStage = 5;
                this.lifeStageTimer = this.lifeStageDuration / 3;
                this.RemoveAttachedAnims();
                this.AttachAnim("Smoke", "01", 30, IsoFireManager.smokeAnimDelay, 0, 0, true, 0, false, 0.7f, IsoFireManager.smokeTintMod, true);
            }
        }
        if (this.square != null) {
            if (this.lifeStage < 4) {
                this.square.getProperties().set(IsoFlagType.burning);
            } else {
                this.square.getProperties().set(IsoFlagType.smoke);
            }
        }
    }

    public IsoFire(IsoCell cell, IsoGridSquare gridSquare, boolean canBurnAnywhere, int startingEnergy, int setLife, boolean isSmoke) {
        this.square = gridSquare;
        this.DirtySlice();
        this.square.getProperties().set(IsoFlagType.smoke);
        this.AttachAnim("Smoke", "03", 30, IsoFireManager.smokeAnimDelay, 0, 0, true, 0, false, 0.7f, IsoFireManager.smokeTintMod);
        this.life = 800 + Rand.Next(2200);
        if (setLife > 0) {
            this.life = setLife;
        }
        this.lifeStage = 4;
        this.lifeStageTimer = this.lifeStageDuration = this.life / 4;
        this.energy = startingEnergy;
        this.smoke = isSmoke;
    }

    public IsoFire(IsoCell cell, IsoGridSquare gridSquare, boolean canBurnAnywhere, int startingEnergy, int setLife) {
        this.square = gridSquare;
        this.DirtySlice();
        this.numFlameParticles = 1;
        for (int i = 0; i < this.numFlameParticles; ++i) {
            this.AttachAnim("Fire", "01", 30, 0.5f, 0, 0, true, 0, false, 0.7f, IsoFireManager.FIRE_TINT_MOD, true);
        }
        this.life = 800 + Rand.Next(2200);
        if (setLife > 0) {
            this.life = setLife;
        }
        if (this.square.getProperties() != null && !this.square.getProperties().has(IsoFlagType.vegitation) && this.square.getFloor() != null) {
            this.life -= this.square.getFloor().getSprite().firerequirement * 100;
            if (this.life < 600) {
                this.life = Rand.Next(300, 600);
            }
        }
        this.spreadDelay = this.spreadTimer = Rand.Next(this.life - this.life / 2);
        this.lifeStage = 0;
        this.lifeStageTimer = this.lifeStageDuration = this.life / 5;
        if (TutorialManager.instance.active) {
            this.lifeStageDuration *= 2;
            this.life *= 2;
        }
        if (TutorialManager.instance.active) {
            this.spreadDelay = this.spreadTimer /= 4;
        }
        gridSquare.getProperties().set(IsoFlagType.burning);
        this.energy = startingEnergy;
        if (this.square.getProperties().has(IsoFlagType.vegitation)) {
            this.energy += 50;
        }
        LuaEventManager.triggerEvent("OnNewFire", this);
    }

    public IsoFire(IsoCell cell, IsoGridSquare gridSquare, boolean canBurnAnywhere, int startingEnergy) {
        this(cell, gridSquare, canBurnAnywhere, startingEnergy, 0);
    }

    public static boolean CanAddSmoke(IsoGridSquare gridSquare, boolean canBurnAnywhere) {
        return IsoFire.CanAddFire(gridSquare, canBurnAnywhere, true);
    }

    public static boolean CanAddFire(IsoGridSquare gridSquare, boolean canBurnAnywhere) {
        return IsoFire.CanAddFire(gridSquare, canBurnAnywhere, false);
    }

    public static boolean CanAddFire(IsoGridSquare gridSquare, boolean canBurnAnywhere, boolean smoke) {
        if (!smoke && (GameServer.server || GameClient.client) && ServerOptions.instance.noFire.getValue()) {
            return false;
        }
        if (gridSquare == null || gridSquare.getObjects().isEmpty()) {
            return false;
        }
        if (gridSquare.has(IsoFlagType.water)) {
            return false;
        }
        if (!canBurnAnywhere && gridSquare.getProperties().has(IsoFlagType.burntOut)) {
            return false;
        }
        if (gridSquare.getProperties().has(IsoFlagType.burning) || gridSquare.getProperties().has(IsoFlagType.smoke)) {
            return false;
        }
        if (!canBurnAnywhere && !IsoFire.Fire_IsSquareFlamable(gridSquare)) {
            return false;
        }
        return smoke || !GameServer.server && !GameClient.client || SafeHouse.getSafeHouse(gridSquare) == null || ServerOptions.instance.safehouseAllowFire.getValue();
    }

    public static boolean Fire_IsSquareFlamable(IsoGridSquare gridSquare) {
        return !gridSquare.getProperties().has(IsoFlagType.unflamable);
    }

    public void Spread() {
        if (GameClient.client) {
            return;
        }
        if (!SandboxOptions.instance.fireSpread.getValue()) {
            return;
        }
        if (this.getCell() == null) {
            return;
        }
        if (this.square == null) {
            return;
        }
        if (this.lifeStage >= 4) {
            return;
        }
        IsoGridSquare newSquare = null;
        int numSpreads = Rand.Next(3) + 1;
        if (Rand.Next(50) == 0) {
            numSpreads += 15;
        }
        if (TutorialManager.instance.active) {
            numSpreads += 15;
        }
        for (int i = 0; i < numSpreads; ++i) {
            int newSquareEnergyRequirement;
            int spreadDirection = Rand.Next(13);
            switch (spreadDirection) {
                case 0: {
                    newSquare = this.getCell().getGridSquare(this.square.getX(), this.square.getY() - 1, this.square.getZ());
                    break;
                }
                case 1: {
                    newSquare = this.getCell().getGridSquare(this.square.getX() + 1, this.square.getY() - 1, this.square.getZ());
                    break;
                }
                case 2: {
                    newSquare = this.getCell().getGridSquare(this.square.getX() + 1, this.square.getY(), this.square.getZ());
                    break;
                }
                case 3: {
                    newSquare = this.getCell().getGridSquare(this.square.getX() + 1, this.square.getY() + 1, this.square.getZ());
                    break;
                }
                case 4: {
                    newSquare = this.getCell().getGridSquare(this.square.getX(), this.square.getY() + 1, this.square.getZ());
                    break;
                }
                case 5: {
                    newSquare = this.getCell().getGridSquare(this.square.getX() - 1, this.square.getY() + 1, this.square.getZ());
                    break;
                }
                case 6: {
                    newSquare = this.getCell().getGridSquare(this.square.getX() - 1, this.square.getY(), this.square.getZ());
                    break;
                }
                case 7: {
                    newSquare = this.getCell().getGridSquare(this.square.getX() - 1, this.square.getY() - 1, this.square.getZ());
                    break;
                }
                case 8: {
                    newSquare = this.getCell().getGridSquare(this.square.getX() - 1, this.square.getY() - 1, this.square.getZ() - 1);
                    break;
                }
                case 9: {
                    newSquare = this.getCell().getGridSquare(this.square.getX() - 1, this.square.getY(), this.square.getZ() - 1);
                    break;
                }
                case 10: {
                    newSquare = this.getCell().getGridSquare(this.square.getX(), this.square.getY() - 1, this.square.getZ() - 1);
                    break;
                }
                case 11: {
                    newSquare = this.getCell().getGridSquare(this.square.getX(), this.square.getY(), this.square.getZ() - 1);
                    break;
                }
                case 12: {
                    newSquare = this.getCell().getGridSquare(this.square.getX(), this.square.getY(), this.square.getZ() + 1);
                }
            }
            if (!IsoFire.CanAddFire(newSquare, false) || this.energy < (newSquareEnergyRequirement = this.getSquaresEnergyRequirement(newSquare))) continue;
            this.energy -= newSquareEnergyRequirement;
            if (GameServer.server) {
                this.sendObjectChange(IsoObjectChange.ENERGY);
            }
            if (RainManager.isRaining().booleanValue()) {
                return;
            }
            int energy = newSquare.getProperties().has(IsoFlagType.exterior) ? this.energy : newSquareEnergyRequirement * 2;
            IsoFireManager.StartFire(this.getCell(), newSquare, false, energy);
        }
    }

    public boolean TestCollide(IsoMovingObject obj, IsoGridSquare passedObjectSquare) {
        return this.square == passedObjectSquare;
    }

    @Override
    public IsoObject.VisionResult TestVision(IsoGridSquare from, IsoGridSquare to) {
        return IsoObject.VisionResult.NoEffect;
    }

    @Override
    public void update() {
        if (this.getObjectIndex() == -1) {
            return;
        }
        if (!GameServer.server) {
            IsoFireManager.updateSound(this);
        }
        if (this.lifeStage < 4) {
            this.square.getProperties().set(IsoFlagType.burning);
        } else {
            this.square.getProperties().set(IsoFlagType.smoke);
        }
        if (!this.smoke && this.lifeStage < 5) {
            this.square.BurnTick();
        }
        int n = this.attachedAnimSprite.size();
        for (int i = 0; i < n; ++i) {
            IsoSpriteInstance s = (IsoSpriteInstance)this.attachedAnimSprite.get(i);
            IsoSprite sp = s.parentSprite;
            s.update();
            if (!sp.hasAnimation()) continue;
            float dt = GameTime.instance.getMultipliedSecondsSinceLastUpdate() * 60.0f;
            s.frame += s.animFrameIncrease * dt;
            if ((int)s.frame < sp.currentAnim.frames.size() || !sp.loop || !s.looped) continue;
            s.frame = 0.0f;
        }
        float lightR = 0.61f;
        float lightG = 0.175f;
        float lightB = 0.0f;
        if (!this.smoke && !GameServer.server && this.lightSource == null) {
            this.lightSource = new IsoLightSource(this.square.getX(), this.square.getY(), this.square.getZ(), 0.61f, 0.175f, 0.0f, this.perm ? this.lightRadius : 5);
            IsoWorld.instance.currentCell.addLamppost(this.lightSource);
        }
        if (this.lightSource != null && this.lifeStage == 5) {
            this.lightSource.r = 0.61f * this.getAlpha(IsoCamera.frameState.playerIndex);
            this.lightSource.g = 0.175f * this.getAlpha(IsoCamera.frameState.playerIndex);
            this.lightSource.b = 0.0f * this.getAlpha(IsoCamera.frameState.playerIndex);
        }
        if (this.perm) {
            if (this.heatSource == null) {
                this.heatSource = new IsoHeatSource(this.square.x, this.square.y, this.square.z, this.lightRadius, 35);
                IsoWorld.instance.currentCell.addHeatSource(this.heatSource);
            } else {
                this.heatSource.setRadius(this.lightRadius);
            }
            return;
        }
        this.updateSinceLastLoaded();
        this.updateFromTimer(GameTime.getInstance().getThirtyFPSMultiplier());
    }

    private void updateSinceLastLoaded() {
        if (this.savedUpdateTime == -1L) {
            return;
        }
        long worldAgeMillis = GameTime.getInstance().getCalender().getTimeInMillis();
        if (this.savedUpdateTime > worldAgeMillis) {
            this.savedUpdateTime = worldAgeMillis;
        }
        float delta30FPS = (float)(worldAgeMillis - this.savedUpdateTime) / 30.0f;
        this.savedUpdateTime = -1L;
        this.updateFromTimer(delta30FPS);
    }

    public void updateFromTimer(float timer) {
        this.accum += timer;
        if (this.square == null) {
            return;
        }
        while (this.accum > 1.0f) {
            this.accum -= 1.0f;
            ++this.age;
            if (this.lifeStageTimer > 0) {
                --this.lifeStageTimer;
                if (this.lifeStageTimer <= 0) {
                    switch (this.lifeStage) {
                        case 0: {
                            this.lifeStage = 1;
                            this.lifeStageTimer = this.lifeStageDuration;
                            this.square.Burn();
                            if (this.lightSource == null) break;
                            this.setLightRadius(5);
                            break;
                        }
                        case 1: {
                            this.lifeStage = 2;
                            this.lifeStageTimer = this.lifeStageDuration;
                            this.RemoveAttachedAnims();
                            this.AttachAnim("Smoke", "02", 30, IsoFireManager.smokeAnimDelay, 0, 0, true, 0, false, 0.7f, IsoFireManager.smokeTintMod, true);
                            this.AttachAnim("Fire", "02", 30, 0.5f, 0, 0, true, 0, false, 0.7f, IsoFireManager.FIRE_TINT_MOD, true);
                            this.square.Burn();
                            if (this.lightSource == null) break;
                            this.setLightRadius(8);
                            break;
                        }
                        case 2: {
                            this.lifeStage = 3;
                            this.lifeStageTimer = this.lifeStageDuration;
                            this.RemoveAttachedAnims();
                            this.AttachAnim("Smoke", "03", 30, IsoFireManager.smokeAnimDelay, 0, 0, true, 0, false, 0.7f, IsoFireManager.smokeTintMod, true);
                            this.AttachAnim("Fire", "03", 30, 0.5f, 0, 0, true, 0, false, 0.7f, IsoFireManager.FIRE_TINT_MOD, true);
                            if (this.lightSource == null) break;
                            this.setLightRadius(12);
                            break;
                        }
                        case 3: {
                            this.lifeStage = 4;
                            this.lifeStageTimer = this.lifeStageDuration / 2;
                            this.RemoveAttachedAnims();
                            this.AttachAnim("Smoke", "02", 30, IsoFireManager.smokeAnimDelay, 0, 0, true, 0, false, 0.7f, IsoFireManager.smokeTintMod, true);
                            this.AttachAnim("Fire", "02", 30, 0.5f, 0, 0, true, 0, false, 0.7f, IsoFireManager.FIRE_TINT_MOD, true);
                            if (this.lightSource == null) break;
                            this.setLightRadius(8);
                            break;
                        }
                        case 4: {
                            this.lifeStage = 5;
                            this.lifeStageTimer = this.lifeStageDuration / 2;
                            this.RemoveAttachedAnims();
                            this.AttachAnim("Smoke", "01", 30, IsoFireManager.smokeAnimDelay, 0, 0, true, 0, false, 0.7f, IsoFireManager.smokeTintMod, true);
                            if (this.lightSource == null) break;
                            this.setLightRadius(1);
                        }
                    }
                }
            }
            if (this.life <= 0) {
                this.extinctFire();
                break;
            }
            --this.life;
            if (this.lifeStage > 0 && this.spreadTimer > 0) {
                --this.spreadTimer;
                if (this.spreadTimer <= 0) {
                    if (this.lifeStage != 5) {
                        this.Spread();
                    }
                    this.spreadTimer = this.spreadDelay;
                }
            }
            if (this.energy > 0) continue;
            this.extinctFire();
            break;
        }
    }

    @Override
    public void render(float x, float y, float z, ColorInfo col, boolean bDoChild, boolean bWallLightingPass, Shader shader) {
        IsoSprite sprite;
        int i;
        x += 0.5f;
        y += 0.5f;
        this.setAlphaToTarget(IsoCamera.frameState.playerIndex);
        this.sx = 0.0f;
        this.offsetX = 32 * Core.tileScale;
        this.offsetY = 96 * Core.tileScale;
        float scale = (float)Core.tileScale / 2.0f;
        for (i = 0; i < this.attachedAnimSprite.size(); ++i) {
            Texture tex;
            sprite = ((IsoSpriteInstance)this.attachedAnimSprite.get((int)i)).parentSprite;
            if (sprite == null || sprite.currentAnim == null || sprite.def == null || (tex = sprite.getTextureForCurrentFrame(this.getDir(), this)) == null) continue;
            if (this.soffX.length <= i) {
                this.soffX = Arrays.copyOf(this.soffX, i + 1);
                this.soffY = Arrays.copyOf(this.soffY, i + 1);
            }
            this.soffX[i] = sprite.soffX;
            this.soffY[i] = sprite.soffY;
            sprite.soffX = (short)(sprite.soffX + (short)(this.offsetX - (float)tex.getWidthOrig() / 2.0f * scale));
            sprite.soffY = (short)(sprite.soffY + (short)(this.offsetY - (float)tex.getHeightOrig() * scale));
            ((IsoSpriteInstance)this.attachedAnimSprite.get(i)).setScale(scale, scale);
        }
        if (PerformanceSettings.fboRenderChunk) {
            bDoChild = false;
        }
        super.render(x, y, z, col, bDoChild, bWallLightingPass, shader);
        for (i = 0; i < this.attachedAnimSprite.size(); ++i) {
            sprite = ((IsoSpriteInstance)this.attachedAnimSprite.get((int)i)).parentSprite;
            if (sprite == null || sprite.currentAnim == null || sprite.def == null) continue;
            sprite.soffX = this.soffX[i];
            sprite.soffY = this.soffY[i];
        }
    }

    public void extinctFire() {
        this.square.getProperties().unset(IsoFlagType.burning);
        this.square.getProperties().unset(IsoFlagType.smoke);
        this.RemoveAttachedAnims();
        this.square.getObjects().remove(this);
        this.square.RemoveTileObject(this);
        this.setLife(0);
        this.removeFromWorld();
    }

    int getSquaresEnergyRequirement(IsoGridSquare testSquare) {
        int energyRequirementVal = 30;
        if (testSquare.getProperties().has(IsoFlagType.vegitation)) {
            energyRequirementVal = 15;
        }
        if (!testSquare.getProperties().has(IsoFlagType.exterior)) {
            energyRequirementVal = 40;
        }
        if (testSquare.getFloor() != null && testSquare.getFloor().getSprite() != null) {
            energyRequirementVal = testSquare.getFloor().getSprite().firerequirement;
        }
        if (TutorialManager.instance.active) {
            return energyRequirementVal / 4;
        }
        return energyRequirementVal;
    }

    public void setSpreadDelay(int spreadDelay) {
        this.spreadDelay = spreadDelay;
    }

    public int getSpreadDelay() {
        return this.spreadDelay;
    }

    public void setLife(int life) {
        this.life = life;
    }

    public int getLife() {
        return this.life;
    }

    public int getEnergy() {
        return this.energy;
    }

    public boolean isPermanent() {
        return this.perm;
    }

    public void setLifeStage(int lifeStage) {
        if (!this.perm) {
            return;
        }
        this.RemoveAttachedAnims();
        switch (lifeStage) {
            case 0: {
                this.AttachAnim("Fire", "01", 30, 0.5f, 0, 0, true, 0, false, 0.7f, IsoFireManager.FIRE_TINT_MOD, true);
                break;
            }
            case 1: {
                this.AttachAnim("Smoke", "02", 30, IsoFireManager.smokeAnimDelay, 0, 0, true, 0, false, 0.7f, IsoFireManager.smokeTintMod, true);
                this.AttachAnim("Fire", "02", 30, 0.5f, 0, 0, true, 0, false, 0.7f, IsoFireManager.FIRE_TINT_MOD, true);
                break;
            }
            case 2: {
                this.AttachAnim("Smoke", "03", 30, IsoFireManager.smokeAnimDelay, 0, 0, true, 0, false, 0.7f, IsoFireManager.smokeTintMod, true);
                this.AttachAnim("Fire", "03", 30, 0.5f, 0, 0, true, 0, false, 0.7f, IsoFireManager.FIRE_TINT_MOD, true);
                break;
            }
            case 3: {
                this.AttachAnim("Smoke", "02", 30, IsoFireManager.smokeAnimDelay, 0, 0, true, 0, false, 0.7f, IsoFireManager.smokeTintMod, true);
                this.AttachAnim("Fire", "02", 30, 0.5f, 0, 0, true, 0, false, 0.7f, IsoFireManager.FIRE_TINT_MOD, true);
                break;
            }
            case 4: {
                this.AttachAnim("Smoke", "01", 30, IsoFireManager.smokeAnimDelay, 0, 0, true, 0, false, 0.7f, IsoFireManager.smokeTintMod, true);
            }
        }
    }

    public void setLightRadius(int radius) {
        this.lightRadius = radius;
        if (this.lightSource != null && radius != this.lightSource.getRadius()) {
            this.getCell().removeLamppost(this.lightSource);
            this.lightSource = new IsoLightSource(this.square.getX(), this.square.getY(), this.square.getZ(), IsoFire.fireColor.r, IsoFire.fireColor.g, IsoFire.fireColor.b, this.lightRadius);
            this.getCell().getLamppostPositions().add(this.lightSource);
            IsoGridSquare.recalcLightTime = -1.0f;
            ++Core.dirtyGlobalLightsCount;
            GameTime.instance.lightSourceUpdate = 100.0f;
        }
    }

    public int getLightRadius() {
        return this.lightRadius;
    }

    @Override
    public void addToWorld() {
        if (this.perm) {
            this.getCell().addToStaticUpdaterObjectList(this);
        } else {
            IsoFireManager.Add(this);
        }
    }

    @Override
    public void removeFromWorld() {
        if (!this.perm) {
            IsoFireManager.Remove(this);
        }
        IsoFireManager.stopSound(this);
        if (this.lightSource != null) {
            this.getCell().removeLamppost(this.lightSource);
            this.lightSource = null;
        }
        if (this.heatSource != null) {
            this.getCell().removeHeatSource(this.heatSource);
            this.heatSource = null;
        }
        super.removeFromWorld();
    }

    @Override
    public void saveChange(IsoObjectChange change, KahluaTable tbl, ByteBufferWriter bb) {
        super.saveChange(change, tbl, bb);
        if (change == IsoObjectChange.ENERGY) {
            bb.putInt(this.energy);
        } else if (change == IsoObjectChange.LIGHT_RADIUS) {
            bb.putInt(this.getLightRadius());
        }
    }

    @Override
    public void loadChange(IsoObjectChange change, ByteBufferReader bb) {
        super.loadChange(change, bb);
        if (change == IsoObjectChange.ENERGY) {
            this.energy = bb.getInt();
        } else if (change == IsoObjectChange.LIGHT_RADIUS) {
            int radius = bb.getInt();
            this.setLightRadius(radius);
        }
    }

    public boolean isCampfire() {
        if (this.getSquare() == null) {
            return false;
        }
        IsoObject[] objects = this.getSquare().getObjects().getElements();
        int n = this.getSquare().getObjects().size();
        for (int i = 1; i < n; ++i) {
            IsoObject obj = objects[i];
            if (obj instanceof IsoWorldInventoryObject || !"Campfire".equalsIgnoreCase(obj.getName())) continue;
            return true;
        }
        return false;
    }

    @Override
    public boolean hasAnimatedAttachments() {
        return this.attachedAnimSprite != null && !this.attachedAnimSprite.isEmpty();
    }

    @Override
    public void renderAnimatedAttachments(float x, float y, float z, ColorInfo col) {
        if (this.attachedAnimSprite == null) {
            return;
        }
        this.sx = 0.0f;
        float offsetX = this.offsetX;
        float offsetY = this.offsetY + (float)(16 * Core.tileScale);
        float scale = (float)Core.tileScale / 2.0f;
        if (this.isCampfire()) {
            scale *= 0.75f;
        }
        for (int i = 0; i < this.attachedAnimSprite.size(); ++i) {
            Texture tex;
            IsoSpriteInstance spriteInstance = (IsoSpriteInstance)this.attachedAnimSprite.get(i);
            IsoSprite sprite = spriteInstance.parentSprite;
            if (sprite == null || sprite.def == null || (tex = sprite.getTextureForCurrentFrame(this.dir, this)) == null) continue;
            if (sprite.name != null && sprite.name.startsWith("Fire")) {
                tempColorInfo.set(1.0f, 1.0f, 1.0f, col.a);
            } else {
                if (this.lifeStage == 5) {
                    float duration = (float)this.lifeStageDuration / 2.0f;
                    float f = 1.0f - (float)this.lifeStageTimer / duration;
                    float alpha = f == 0.0f ? 0.0f : PZMath.pow(2.0f, 10.0f * f - 10.0f);
                    this.setAlphaAndTarget(IsoCamera.frameState.playerIndex, 1.0f - alpha);
                }
                tempColorInfo.set(col);
            }
            short soffX = sprite.soffX;
            short soffY = sprite.soffY;
            sprite.soffX = (short)((float)(soffX + 32 * Core.tileScale) - (float)tex.getWidthOrig() / 2.0f * scale);
            sprite.soffY = (short)((float)(soffY + 128 * Core.tileScale) - (float)tex.getHeightOrig() * scale);
            spriteInstance.setScale(scale, scale);
            spriteInstance.getParentSprite().render(spriteInstance, this, x + 0.0f, y + 0.0f, z, this.dir, offsetX, offsetY, tempColorInfo, true);
            sprite.soffX = soffX;
            sprite.soffY = soffY;
        }
    }
}

