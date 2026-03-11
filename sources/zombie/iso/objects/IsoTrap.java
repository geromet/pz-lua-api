/*
 * Decompiled with CFR 0.152.
 */
package zombie.iso.objects;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import zombie.CombatManager;
import zombie.GameTime;
import zombie.GameWindow;
import zombie.Lua.LuaEventManager;
import zombie.SandboxOptions;
import zombie.UsedFromLua;
import zombie.WorldSoundManager;
import zombie.ai.states.ZombieIdleState;
import zombie.audio.BaseSoundEmitter;
import zombie.characters.BodyDamage.BodyPart;
import zombie.characters.BodyDamage.BodyPartType;
import zombie.characters.IsoGameCharacter;
import zombie.characters.IsoPlayer;
import zombie.characters.IsoZombie;
import zombie.characters.animals.IsoAnimal;
import zombie.core.Core;
import zombie.core.PerformanceSettings;
import zombie.core.math.PZMath;
import zombie.core.opengl.Shader;
import zombie.core.random.Rand;
import zombie.core.skinnedmodel.model.ItemModelRenderer;
import zombie.core.skinnedmodel.model.WorldItemModelDrawer;
import zombie.core.textures.ColorInfo;
import zombie.core.textures.Texture;
import zombie.core.utils.GameTimer;
import zombie.core.utils.OnceEvery;
import zombie.inventory.InventoryItem;
import zombie.inventory.InventoryItemFactory;
import zombie.inventory.types.HandWeapon;
import zombie.iso.IItemProvider;
import zombie.iso.IsoCamera;
import zombie.iso.IsoCell;
import zombie.iso.IsoDirections;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoMovingObject;
import zombie.iso.IsoObject;
import zombie.iso.IsoUtils;
import zombie.iso.IsoWorld;
import zombie.iso.LosUtil;
import zombie.iso.areas.NonPvpZone;
import zombie.iso.fboRenderChunk.FBORenderChunk;
import zombie.iso.fboRenderChunk.FBORenderChunkManager;
import zombie.iso.fboRenderChunk.FBORenderItems;
import zombie.iso.fboRenderChunk.FBORenderLevels;
import zombie.iso.objects.IsoFire;
import zombie.iso.objects.IsoFireManager;
import zombie.iso.sprite.IsoSprite;
import zombie.iso.sprite.IsoSpriteManager;
import zombie.network.GameClient;
import zombie.network.GameServer;
import zombie.network.IConnection;
import zombie.network.PacketTypes;
import zombie.network.ServerOptions;
import zombie.network.packets.INetworkPacket;
import zombie.util.StringUtils;
import zombie.util.Type;

@UsedFromLua
public class IsoTrap
extends IsoObject
implements IItemProvider {
    private static final int MaximumTrapRadius = 15;
    private static final int MaximumChance = 100;
    private static final int MaximumExplosivePower = 100;
    private static final int ExplosionDamageDivisor = 20;
    private static final float ExplosionDamageVarianceMultiplier = 2.0f;
    private static final OnceEvery SENSOR_TIMER = new OnceEvery(1.0f);
    private int timerBeforeExplosion;
    private int sensorRange;
    private int fireRange;
    private int fireStartingChance;
    private int fireStartingEnergy;
    private int explosionPower;
    private int explosionRange;
    private int smokeRange;
    private int noiseRange;
    private int noiseDuration;
    private float noiseStartTime;
    private float lastWorldSoundTime;
    private float extraDamage;
    private int remoteControlId = -1;
    private String countDownSound;
    private String explosionSound;
    private HandWeapon weapon;
    private IsoGameCharacter attacker;
    private boolean instantExplosion;
    private final GameTimer beep = new GameTimer(1);
    private int explosionDuration;
    private float explosionStartTime;

    public IsoTrap(IsoCell cell) {
        super(cell);
    }

    public IsoTrap(HandWeapon weapon, IsoCell cell, IsoGridSquare sq) {
        this.IsoTrap(null, weapon, cell, sq);
    }

    public IsoTrap(IsoGameCharacter attacker, HandWeapon weapon, IsoCell cell, IsoGridSquare sq) {
        this.IsoTrap(attacker, weapon, cell, sq);
    }

    private void IsoTrap(IsoGameCharacter attacker, HandWeapon weapon, IsoCell cell, IsoGridSquare sq) {
        this.square = sq;
        this.initSprite(weapon);
        this.setSensorRange(weapon.getSensorRange());
        this.setFireRange(weapon.getFireRange());
        this.setFireStartingEnergy(weapon.getFireStartingEnergy());
        this.setFireStartingChance(weapon.getFireStartingChance());
        this.setExplosionPower(weapon.getExplosionPower());
        this.setExplosionRange(weapon.getExplosionRange());
        this.setSmokeRange(weapon.getSmokeRange());
        this.setNoiseRange(weapon.getNoiseRange());
        this.setNoiseDuration(weapon.getNoiseDuration());
        this.setExtraDamage(weapon.getExtraDamage());
        this.setRemoteControlID(weapon.getRemoteControlID());
        this.setCountDownSound(weapon.getCountDownSound());
        this.setExplosionSound(weapon.getExplosionSound());
        this.setTimerBeforeExplosion(weapon.getExplosionTimer());
        this.setInstantExplosion(weapon.isInstantExplosion());
        this.setExplosionDuration(weapon.getExplosionDuration());
        if (weapon.getAttackTargetSquare(null) == null) {
            weapon.setAttackTargetSquare(sq);
        }
        this.weapon = weapon;
        this.attacker = attacker;
    }

    private void initSprite(HandWeapon weapon) {
        if (weapon == null) {
            return;
        }
        String texName = weapon.getPlacedSprite() != null && !weapon.getPlacedSprite().isEmpty() ? weapon.getPlacedSprite() : (weapon.getTex() != null && weapon.getTex().getName() != null ? weapon.getTex().getName() : "media/inventory/world/WItem_Sack.png");
        this.sprite = IsoSpriteManager.instance.namedMap.get(texName);
        if (this.sprite == null) {
            this.sprite = IsoSprite.CreateSprite(IsoSpriteManager.instance);
            this.sprite.LoadSingleTexture(texName);
        }
        Texture tex = this.sprite.getTextureForCurrentFrame(this.getDir());
        if (texName.startsWith("Item_") && tex != null) {
            if (weapon.getScriptItem() == null) {
                this.sprite.def.scaleAspect(tex.getWidthOrig(), tex.getHeightOrig(), 16 * Core.tileScale, 16 * Core.tileScale);
            } else {
                float scale = weapon.getScriptItem().scaleWorldIcon * ((float)Core.tileScale / 2.0f);
                this.sprite.def.setScale(scale, scale);
            }
        }
    }

    @Override
    public void update() {
        if (!GameClient.client && this.timerBeforeExplosion > 0 && this.beep.check()) {
            --this.timerBeforeExplosion;
            if (this.timerBeforeExplosion == 0) {
                this.triggerExplosion(this.getSensorRange() > 0);
            } else if (this.getObjectIndex() != -1 && this.weapon.getType().endsWith("Triggered")) {
                String sound = "TrapTimerLoop";
                if (!StringUtils.isNullOrWhitespace(this.getCountDownSound())) {
                    sound = this.getCountDownSound();
                } else if (this.timerBeforeExplosion == 1) {
                    sound = "TrapTimerExpired";
                }
                if (GameServer.server) {
                    GameServer.PlayWorldSoundServer(sound, false, this.square, 1.0f, 10.0f, 1.0f, true);
                } else if (!GameClient.client) {
                    this.getOrCreateEmitter();
                    this.emitter.playSound(sound);
                }
            }
        }
        if (this.updateExplosionDuration()) {
            return;
        }
        this.updateVictimsInSensorRange();
        this.updateSounds();
    }

    private boolean updateExplosionDuration() {
        if (!this.isExploding()) {
            return false;
        }
        float worldAge = (float)GameTime.getInstance().getWorldAgeHours();
        this.explosionStartTime = PZMath.min(this.explosionStartTime, worldAge);
        float dayLengthScale = 60.0f / (float)SandboxOptions.getInstance().getDayLengthMinutes();
        float minutesPerHour = 60.0f;
        if (worldAge - this.explosionStartTime > (float)this.getExplosionDuration() / 60.0f * dayLengthScale) {
            this.explosionStartTime = 0.0f;
            if (this.emitter != null) {
                this.emitter.stopAll();
            }
            if (GameServer.server) {
                GameServer.RemoveItemFromMap(this);
            } else if (!GameClient.client) {
                this.removeFromWorld();
                this.removeFromSquare();
            }
            return true;
        }
        if (this.getSmokeRange() > 0 && this.getObjectIndex() != -1) {
            int radius = this.getSmokeRange();
            int x = this.getSquare().getX() - radius;
            while ((float)x <= this.getX() + (float)radius) {
                for (int y = this.getSquare().getY() - radius; y <= this.getSquare().getY() + radius; ++y) {
                    IsoGridSquare explodedSquare = this.getCell().getGridSquare((double)x, (double)y, this.getZ());
                    this.refreshSmokeBombSmoke(explodedSquare);
                }
                ++x;
            }
        }
        return false;
    }

    private void refreshSmokeBombSmoke(IsoGridSquare explodedSquare) {
        if (explodedSquare == null) {
            return;
        }
        IsoFire fire = explodedSquare.getFire();
        if (fire == null || !fire.smoke) {
            return;
        }
        int radius = this.getSmokeRange();
        if (IsoUtils.DistanceTo((float)explodedSquare.getX() + 0.5f, (float)explodedSquare.getY() + 0.5f, this.getX() + 0.5f, this.getY() + 0.5f) > (float)radius) {
            return;
        }
        LosUtil.TestResults testResults = LosUtil.lineClear(this.getCell(), PZMath.fastfloor(this.getX()), PZMath.fastfloor(this.getY()), PZMath.fastfloor(this.getZ()), explodedSquare.getX(), explodedSquare.getY(), explodedSquare.getZ(), false);
        if (testResults == LosUtil.TestResults.Blocked || testResults == LosUtil.TestResults.ClearThroughClosedDoor) {
            return;
        }
        if (NonPvpZone.getNonPvpZone(explodedSquare.getX(), explodedSquare.getY()) != null) {
            return;
        }
        fire.energy = 40;
        fire.life = Rand.Next(800, 3000) / 5;
        fire.lifeStage = 4;
        fire.lifeStageTimer = fire.lifeStageDuration = fire.life / 4;
        this.smoke(explodedSquare);
    }

    private void updateVictimsInSensorRange() {
        if (GameClient.client) {
            return;
        }
        if (this.isExploding()) {
            return;
        }
        if (this.getSquare() == null || this.getSensorRange() <= 0) {
            return;
        }
        if (IsoWorld.instance.currentCell == null) {
            return;
        }
        if (!SENSOR_TIMER.Check()) {
            return;
        }
        ArrayList<IsoMovingObject> objects = IsoWorld.instance.currentCell.getObjectList();
        for (int i = 0; i < objects.size(); ++i) {
            LosUtil.TestResults testResults;
            IsoMovingObject movingObject = objects.get(i);
            IsoGameCharacter character = Type.tryCastTo(movingObject, IsoGameCharacter.class);
            if (character != null && character.isInvisible() || movingObject.getZi() != this.getSquare().getZ() || IsoUtils.DistanceToSquared(movingObject.getX(), movingObject.getY(), this.getX() + 0.5f, this.getY() + 0.5f) > (float)(this.getSensorRange() * this.getSensorRange()) || (testResults = LosUtil.lineClear(this.getSquare().getCell(), this.getSquare().getX(), this.getSquare().getY(), this.getSquare().getZ(), movingObject.getXi(), movingObject.getYi(), movingObject.getZi(), false)) == LosUtil.TestResults.Blocked || testResults == LosUtil.TestResults.ClearThroughClosedDoor) continue;
            if (GameServer.server) {
                IConnection excluded = null;
                Object TRIGGER_EXPLOSION = null;
                INetworkPacket.sendToAll(PacketTypes.PacketType.AddExplosiveTrap, excluded, TRIGGER_EXPLOSION, this.getSquare());
            }
            this.triggerExplosion();
            break;
        }
    }

    private void updateSounds() {
        if (this.noiseStartTime > 0.0f) {
            float worldAge = (float)GameTime.getInstance().getWorldAgeHours();
            this.noiseStartTime = PZMath.min(this.noiseStartTime, worldAge);
            this.lastWorldSoundTime = PZMath.min(this.lastWorldSoundTime, worldAge);
            float dayLengthScale = 60.0f / (float)SandboxOptions.getInstance().getDayLengthMinutes();
            float minutesPerHour = 60.0f;
            if (worldAge - this.noiseStartTime > (float)this.getNoiseDuration() / 60.0f * dayLengthScale) {
                this.noiseStartTime = 0.0f;
                if (this.emitter != null) {
                    this.emitter.stopAll();
                }
            } else {
                BaseSoundEmitter emitter;
                if (!(GameServer.server || this.emitter != null && this.emitter.isPlaying(this.getExplosionSound()) || (emitter = this.getOrCreateEmitter()) == null)) {
                    emitter.playSoundImpl(this.getExplosionSound(), (IsoObject)null);
                }
                if (!GameClient.client && worldAge - this.lastWorldSoundTime > 0.016666668f * dayLengthScale && this.getObjectIndex() != -1) {
                    this.lastWorldSoundTime = worldAge;
                    WorldSoundManager.instance.addSoundRepeating(null, this.getSquare().getX(), this.getSquare().getY(), this.getSquare().getZ(), this.getNoiseRange(), 1, true);
                }
            }
        }
        if (this.emitter != null) {
            this.emitter.tick();
        }
    }

    @Override
    public IsoGridSquare getRenderSquare() {
        if (this.getSquare() == null) {
            return null;
        }
        int chunksPerWidth = 8;
        if (PZMath.coordmodulo(this.square.x, 8) == 0 && PZMath.coordmodulo(this.square.y, 8) == 7) {
            return this.square.getAdjacentSquare(IsoDirections.S);
        }
        if (PZMath.coordmodulo(this.square.x, 8) == 7 && PZMath.coordmodulo(this.square.y, 8) == 0) {
            return this.square.getAdjacentSquare(IsoDirections.E);
        }
        return this.getSquare();
    }

    @Override
    public void render(float x, float y, float z, ColorInfo col, boolean bDoChild, boolean bWallLightingPass, Shader shader) {
        if (Core.getInstance().isOption3DGroundItem() && ItemModelRenderer.itemHasModel(this.getItem())) {
            if (PerformanceSettings.fboRenderChunk && FBORenderChunkManager.instance.isCaching()) {
                int playerIndex = IsoCamera.frameState.playerIndex;
                IsoGridSquare renderSquare = this.getRenderSquare();
                FBORenderLevels renderLevels = renderSquare.chunk.getRenderLevels(playerIndex);
                FBORenderChunk renderChunk = renderLevels.getFBOForLevel(renderSquare.z, Core.getInstance().getZoom(playerIndex));
                FBORenderItems.getInstance().render(renderChunk.index, this);
                return;
            }
            ItemModelRenderer.RenderStatus status = WorldItemModelDrawer.renderMain(this.getItem(), this.getSquare(), this.getRenderSquare(), this.getX() + 0.5f, this.getY() + 0.5f, this.getZ(), 0.0f, -1.0f, true);
            if (status == ItemModelRenderer.RenderStatus.Loading || status == ItemModelRenderer.RenderStatus.Ready) {
                return;
            }
        }
        if (this.sprite.hasNoTextures()) {
            return;
        }
        Texture tex = this.sprite.getTextureForCurrentFrame(this.dir, this);
        if (tex == null) {
            return;
        }
        if (tex.getName().startsWith("Item_")) {
            float dx = (float)tex.getWidthOrig() * this.sprite.def.getScaleX() / 2.0f;
            float dy = (float)tex.getHeightOrig() * this.sprite.def.getScaleY() * 3.0f / 4.0f;
            this.setAlphaAndTarget(1.0f);
            this.offsetX = 0.0f;
            this.offsetY = 0.0f;
            this.sx = 0.0f;
            this.sprite.render(this, x + 0.5f, y + 0.5f, z, this.dir, this.offsetX + dx, this.offsetY + dy, col, true);
        } else {
            this.offsetX = 32 * Core.tileScale;
            this.offsetY = 96 * Core.tileScale;
            this.sx = 0.0f;
            super.render(x, y, z, col, bDoChild, bWallLightingPass, shader);
        }
    }

    public void place() {
        if (this.getSquare() == null || this.getHandWeapon() == null) {
            return;
        }
        if (GameClient.client) {
            return;
        }
        this.getSquare().AddTileObject(this);
        if (GameServer.server) {
            INetworkPacket.sendToAll(PacketTypes.PacketType.AddExplosiveTrap, null, this.getHandWeapon(), this.getSquare());
        }
        if (this.getHandWeapon().isInstantExplosion()) {
            this.triggerExplosion();
        }
    }

    @Deprecated
    public void triggerExplosion(boolean sensor) {
        if (sensor) {
            return;
        }
        this.triggerExplosion();
    }

    public void triggerExplosion() {
        LuaEventManager.triggerEvent("OnThrowableExplode", this, this.square);
        this.timerBeforeExplosion = 0;
        if (this.getExplosionSound() != null) {
            this.playExplosionSound();
        }
        if (this.getNoiseRange() > 0) {
            WorldSoundManager.instance.addSound(null, this.getXi(), this.getYi(), this.getZi(), this.getNoiseRange(), 1);
        } else if (this.getExplosionSound() != null) {
            WorldSoundManager.instance.addSound(null, this.getXi(), this.getYi(), this.getZi(), 50, 1);
        }
        if (this.getExplosionRange() > 0) {
            this.drawCircleExplosion(this.square, this.getExplosionRange(), ExplosionMode.Explosion);
        }
        if (this.getFireRange() > 0) {
            this.drawCircleExplosion(this.square, this.getFireRange(), ExplosionMode.Fire);
        }
        if (this.getSmokeRange() > 0) {
            this.drawCircleExplosion(this.square, this.getSmokeRange(), ExplosionMode.Smoke);
        }
        if (this.getExplosionDuration() > 0) {
            if (this.explosionStartTime == 0.0f) {
                this.explosionStartTime = (float)GameTime.getInstance().getWorldAgeHours();
            }
            return;
        }
        if (this.weapon == null || !this.weapon.canBeReused()) {
            if (GameServer.server) {
                GameServer.RemoveItemFromMap(this);
            } else {
                this.removeFromWorld();
                this.removeFromSquare();
            }
        }
    }

    private BaseSoundEmitter getOrCreateEmitter() {
        if (this.getObjectIndex() == -1) {
            return null;
        }
        if (this.emitter == null) {
            this.emitter = IsoWorld.instance.getFreeEmitter(this.getX() + 0.5f, this.getY() + 0.5f, this.getZ());
            IsoWorld.instance.takeOwnershipOfEmitter(this.emitter);
        }
        return this.emitter;
    }

    public void playExplosionSound() {
        if (StringUtils.isNullOrWhitespace(this.getExplosionSound())) {
            return;
        }
        if (this.getObjectIndex() == -1) {
            if (!GameServer.server && this.isInstantExplosion()) {
                BaseSoundEmitter emitter1 = IsoWorld.instance.getFreeEmitter(this.getX() + 0.5f, this.getY() + 0.5f, this.getZ());
                emitter1.playSoundImpl(this.getExplosionSound(), (IsoObject)null);
            }
            return;
        }
        if (this.getNoiseRange() > 0 && (float)this.getNoiseDuration() > 0.0f) {
            this.noiseStartTime = (float)GameTime.getInstance().getWorldAgeHours();
        }
        if (GameServer.server) {
            if (this.getNoiseRange() > 0 && (float)this.getNoiseDuration() > 0.0f) {
                GameServer.PlayWorldSoundServer(this.getExplosionSound(), this.getSquare(), this.getNoiseRange(), this.getObjectIndex());
            } else {
                GameServer.PlayWorldSoundServer(this.getExplosionSound(), false, this.getSquare(), 0.0f, 50.0f, 1.0f, false);
            }
            return;
        }
        this.getOrCreateEmitter();
        if (!this.emitter.isPlaying(this.getExplosionSound())) {
            this.emitter.playSoundImpl(this.getExplosionSound(), (IsoObject)null);
        }
        if ("SmokeBombLoop".equalsIgnoreCase(this.getExplosionSound())) {
            this.emitter.playSoundImpl("SmokeBombExplode", (IsoObject)null);
        }
    }

    @Override
    public void load(ByteBuffer input, int worldVersion, boolean isDebugSave) throws IOException {
        InventoryItem item;
        boolean hasItem;
        super.load(input, worldVersion, isDebugSave);
        this.sensorRange = input.getInt();
        this.fireStartingChance = input.getInt();
        this.fireStartingEnergy = input.getInt();
        this.fireRange = input.getInt();
        this.explosionPower = input.getInt();
        this.explosionRange = input.getInt();
        if (worldVersion >= 219) {
            this.explosionDuration = input.getInt();
            this.explosionStartTime = input.getFloat();
        }
        this.smokeRange = input.getInt();
        this.noiseRange = input.getInt();
        this.noiseDuration = input.getInt();
        this.noiseStartTime = input.getFloat();
        this.extraDamage = input.getFloat();
        this.remoteControlId = input.getInt();
        this.timerBeforeExplosion = input.getInt();
        this.countDownSound = GameWindow.ReadString(input);
        this.explosionSound = GameWindow.ReadString(input);
        if ("bigExplosion".equals(this.explosionSound)) {
            this.explosionSound = "BigExplosion";
        }
        if ("smallExplosion".equals(this.explosionSound)) {
            this.explosionSound = "SmallExplosion";
        }
        if ("feedback".equals(this.explosionSound)) {
            this.explosionSound = "NoiseTrapExplosion";
        }
        boolean bl = hasItem = input.get() != 0;
        if (hasItem && (item = InventoryItem.loadItem(input, worldVersion)) instanceof HandWeapon) {
            HandWeapon handWeapon;
            this.weapon = handWeapon = (HandWeapon)item;
            this.initSprite(this.weapon);
        }
    }

    @Override
    public void save(ByteBuffer output, boolean isDebugSave) throws IOException {
        super.save(output, isDebugSave);
        output.putInt(this.sensorRange);
        output.putInt(this.fireStartingChance);
        output.putInt(this.fireStartingEnergy);
        output.putInt(this.fireRange);
        output.putInt(this.explosionPower);
        output.putInt(this.explosionRange);
        output.putInt(this.explosionDuration);
        output.putFloat(this.explosionStartTime);
        output.putInt(this.smokeRange);
        output.putInt(this.noiseRange);
        output.putInt(this.noiseDuration);
        output.putFloat(this.noiseStartTime);
        output.putFloat(this.extraDamage);
        output.putInt(this.remoteControlId);
        output.putInt(this.timerBeforeExplosion);
        GameWindow.WriteString(output, this.countDownSound);
        GameWindow.WriteString(output, this.explosionSound);
        if (this.weapon != null) {
            output.put((byte)1);
            this.weapon.saveWithSize(output, false);
        } else {
            output.put((byte)0);
        }
    }

    @Override
    public void addToWorld() {
        this.getCell().addToProcessIsoObject(this);
    }

    @Override
    public void removeFromWorld() {
        if (this.emitter != null) {
            if (this.noiseStartTime > 0.0f) {
                this.emitter.stopAll();
            }
            IsoWorld.instance.returnOwnershipOfEmitter(this.emitter);
            this.emitter = null;
        }
        super.removeFromWorld();
    }

    public int getTimerBeforeExplosion() {
        return this.timerBeforeExplosion;
    }

    public void setTimerBeforeExplosion(int timerBeforeExplosion) {
        this.timerBeforeExplosion = timerBeforeExplosion;
    }

    public int getSensorRange() {
        return this.sensorRange;
    }

    public void setSensorRange(int sensorRange) {
        this.sensorRange = sensorRange;
    }

    public int getFireRange() {
        return this.fireRange;
    }

    public void setFireRange(int fireRange) {
        this.fireRange = fireRange;
    }

    public int getFireStartingEnergy() {
        return this.fireStartingEnergy;
    }

    public void setFireStartingEnergy(int fireStartingEnergy) {
        this.fireStartingEnergy = fireStartingEnergy;
    }

    public int getFireStartingChance() {
        return this.fireStartingChance;
    }

    public void setFireStartingChance(int fireStartingChance) {
        this.fireStartingChance = fireStartingChance;
    }

    public int getExplosionPower() {
        return this.explosionPower;
    }

    public void setExplosionPower(int explosionPower) {
        this.explosionPower = explosionPower;
    }

    public int getNoiseDuration() {
        return this.noiseDuration;
    }

    public void setNoiseDuration(int noiseDuration) {
        this.noiseDuration = noiseDuration;
    }

    public int getNoiseRange() {
        return this.noiseRange;
    }

    public void setNoiseRange(int noiseRange) {
        this.noiseRange = noiseRange;
    }

    public int getExplosionRange() {
        return this.explosionRange;
    }

    public void setExplosionRange(int explosionRange) {
        this.explosionRange = explosionRange;
    }

    public int getSmokeRange() {
        return this.smokeRange;
    }

    public void setSmokeRange(int smokeRange) {
        this.smokeRange = smokeRange;
    }

    public float getExtraDamage() {
        return this.extraDamage;
    }

    public void setExtraDamage(float extraDamage) {
        this.extraDamage = extraDamage;
    }

    @Override
    public String getObjectName() {
        return "IsoTrap";
    }

    public int getRemoteControlID() {
        return this.remoteControlId;
    }

    public void setRemoteControlID(int remoteControlId) {
        this.remoteControlId = remoteControlId;
    }

    public String getCountDownSound() {
        return this.countDownSound;
    }

    public void setCountDownSound(String sound) {
        this.countDownSound = sound;
    }

    public String getExplosionSound() {
        return this.explosionSound;
    }

    public void setExplosionSound(String explosionSound) {
        this.explosionSound = explosionSound;
    }

    public int getExplosionDuration() {
        return this.explosionDuration;
    }

    public void setExplosionDuration(int minutes) {
        this.explosionDuration = minutes;
    }

    public boolean isExploding() {
        return this.explosionDuration > 0 && this.explosionStartTime > 0.0f;
    }

    @Override
    public InventoryItem getItem() {
        return this.weapon;
    }

    @UsedFromLua
    public static void triggerRemote(IsoPlayer player, int remoteID, int range) {
        int px = player.getXi();
        int py = player.getYi();
        int pz = player.getZi();
        int minZ = Math.max(pz - range / 2, 0);
        int maxZ = Math.min(pz + range / 2, 8);
        IsoCell cell = IsoWorld.instance.currentCell;
        for (int z = minZ; z < maxZ; ++z) {
            for (int y = py - range; y < py + range; ++y) {
                for (int x = px - range; x < px + range; ++x) {
                    IsoGridSquare sq = cell.getGridSquare(x, y, z);
                    if (sq == null) continue;
                    for (int i = sq.getObjects().size() - 1; i >= 0; --i) {
                        IsoTrap isoTrap;
                        IsoObject o = sq.getObjects().get(i);
                        if (!(o instanceof IsoTrap) || (isoTrap = (IsoTrap)o).getRemoteControlID() != remoteID) continue;
                        isoTrap.triggerExplosion();
                    }
                }
            }
        }
    }

    public boolean isInstantExplosion() {
        return this.instantExplosion;
    }

    public void setInstantExplosion(boolean instantExplosion) {
        this.instantExplosion = instantExplosion;
    }

    public HandWeapon getHandWeapon() {
        return this.weapon;
    }

    public IsoGameCharacter getAttacker() {
        return this.attacker;
    }

    private void drawCircleExplosion(IsoGridSquare square, int radius, ExplosionMode explosionMode) {
        radius = Math.min(radius, 15);
        int fireStartingPower = this.getFireStartingEnergy();
        for (int x = square.getX() - radius; x <= square.getX() + radius; ++x) {
            for (int y = square.getY() - radius; y <= square.getY() + radius; ++y) {
                boolean startFire;
                IsoGridSquare explodedSquare;
                LosUtil.TestResults testResults;
                if (IsoUtils.DistanceTo((float)x + 0.5f, (float)y + 0.5f, (float)square.getX() + 0.5f, (float)square.getY() + 0.5f) > (float)radius || (testResults = LosUtil.lineClear(square.getCell(), PZMath.fastfloor(this.getX()), PZMath.fastfloor(this.getY()), PZMath.fastfloor(this.getZ()), x, y, square.getZ(), false)) == LosUtil.TestResults.Blocked || testResults == LosUtil.TestResults.ClearThroughClosedDoor || (explodedSquare = this.getCell().getGridSquare(x, y, square.getZ())) == null || NonPvpZone.getNonPvpZone(explodedSquare.getX(), explodedSquare.getY()) != null) continue;
                boolean bl = startFire = Rand.Next(100) < this.getFireStartingChance();
                if (explosionMode == ExplosionMode.Smoke) {
                    if (startFire) {
                        IsoFireManager.StartSmoke(this.getCell(), explodedSquare, true, fireStartingPower, 0);
                    }
                    this.smoke(explodedSquare);
                }
                if (explosionMode == ExplosionMode.Explosion) {
                    boolean burnSquare;
                    boolean bl2 = burnSquare = Rand.Next(100) < this.getFireStartingChance();
                    if (!GameClient.client && this.getExplosionPower() > 0 && burnSquare) {
                        explodedSquare.Burn();
                    }
                    this.explosion(explodedSquare);
                    if (startFire) {
                        IsoFireManager.StartFire(this.getCell(), explodedSquare, true, fireStartingPower);
                    }
                }
                if (explosionMode != ExplosionMode.Fire || !startFire) continue;
                IsoFireManager.StartFire(this.getCell(), explodedSquare, true, fireStartingPower);
            }
        }
    }

    private void explosion(IsoGridSquare isoGridSquare) {
        if (GameServer.server && this.isInstantExplosion()) {
            return;
        }
        IsoGameCharacter attackingIsoGameCharacter = this.getAttacker() != null ? this.getAttacker() : IsoWorld.instance.currentCell.getFakeZombieForHit();
        for (int i = 0; i < isoGridSquare.getMovingObjects().size(); ++i) {
            IsoZombie isoZombie;
            IsoMovingObject moving = isoGridSquare.getMovingObjects().get(i);
            if (moving instanceof IsoPlayer && !ServerOptions.getInstance().pvp.getValue() || !(moving instanceof IsoGameCharacter)) continue;
            IsoGameCharacter isoGameCharacter = (IsoGameCharacter)moving;
            if (GameServer.server || !(moving instanceof IsoZombie) || (isoZombie = (IsoZombie)moving).isLocal()) {
                int fireStartingChance;
                int explosionPower = Math.min(this.getExplosionPower(), 100);
                moving.Hit(this.getHandWeapon(), attackingIsoGameCharacter, Rand.Next((float)explosionPower / 20.0f, (float)explosionPower / 20.0f * 2.0f) + this.getExtraDamage(), false, 1.0f);
                if (!GameServer.server && !(isoGameCharacter instanceof IsoAnimal)) {
                    CombatManager.getInstance().processInstantExplosion(isoGameCharacter, this);
                }
                if ((fireStartingChance = this.getFireStartingChance()) > 0 && !(moving instanceof IsoZombie)) {
                    int burnAttempts = BodyPartType.MAX.ordinal();
                    for (int j = 0; j < burnAttempts; ++j) {
                        if (Rand.Next(100) >= fireStartingChance) continue;
                        BodyPart bodypart = isoGameCharacter.getBodyDamage().getBodyPart(BodyPartType.FromIndex(j));
                        bodypart.setBurned();
                    }
                }
            }
            if (!GameClient.client || !(moving instanceof IsoZombie) || !(isoZombie = (IsoZombie)moving).isRemoteZombie()) continue;
            moving.Hit((HandWeapon)InventoryItemFactory.CreateItem("Base.Axe"), attackingIsoGameCharacter, 0.0f, true, 0.0f);
        }
    }

    private void smoke(IsoGridSquare isoGridSquare) {
        for (int i = 0; i < isoGridSquare.getMovingObjects().size(); ++i) {
            IsoMovingObject moving = isoGridSquare.getMovingObjects().get(i);
            if (!(moving instanceof IsoZombie)) continue;
            IsoZombie isoZombie = (IsoZombie)moving;
            isoZombie.setTarget(null);
            isoZombie.changeState(ZombieIdleState.instance());
        }
    }

    public static enum ExplosionMode {
        Explosion,
        Fire,
        Smoke;

    }
}

