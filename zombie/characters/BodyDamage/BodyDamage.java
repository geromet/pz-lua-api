/*
 * Decompiled with CFR 0.152.
 */
package zombie.characters.BodyDamage;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Objects;
import zombie.FliesSound;
import zombie.GameTime;
import zombie.GameWindow;
import zombie.Lua.LuaEventManager;
import zombie.Lua.LuaManager;
import zombie.SandboxOptions;
import zombie.UsedFromLua;
import zombie.WorldSoundManager;
import zombie.ZomboidGlobals;
import zombie.audio.MusicIntensityConfig;
import zombie.audio.parameters.ParameterZombieState;
import zombie.characterTextures.BloodBodyPartType;
import zombie.characters.BodyDamage.BodyPart;
import zombie.characters.BodyDamage.BodyPartLast;
import zombie.characters.BodyDamage.BodyPartType;
import zombie.characters.BodyDamage.Metabolics;
import zombie.characters.BodyDamage.Thermoregulator;
import zombie.characters.CharacterStat;
import zombie.characters.ClothingWetness;
import zombie.characters.IsoGameCharacter;
import zombie.characters.IsoLivingCharacter;
import zombie.characters.IsoPlayer;
import zombie.characters.IsoSurvivor;
import zombie.characters.IsoZombie;
import zombie.characters.Stats;
import zombie.characters.animals.IsoAnimal;
import zombie.core.Core;
import zombie.core.SpriteRenderer;
import zombie.core.logger.LoggerManager;
import zombie.core.math.PZMath;
import zombie.core.random.Rand;
import zombie.debug.DebugLog;
import zombie.inventory.InventoryItem;
import zombie.inventory.types.DrainableComboItem;
import zombie.inventory.types.Food;
import zombie.inventory.types.HandWeapon;
import zombie.inventory.types.Literature;
import zombie.inventory.types.WeaponType;
import zombie.iso.CorpseCount;
import zombie.iso.IsoGridSquare;
import zombie.iso.weather.ClimateManager;
import zombie.network.GameClient;
import zombie.network.GameServer;
import zombie.network.ServerOptions;
import zombie.scripting.objects.CharacterTrait;
import zombie.scripting.objects.ItemTag;
import zombie.scripting.objects.MoodleType;
import zombie.scripting.objects.WeaponCategory;
import zombie.util.StringUtils;
import zombie.util.Type;
import zombie.vehicles.BaseVehicle;
import zombie.vehicles.VehiclePart;
import zombie.vehicles.VehicleWindow;

@UsedFromLua
public final class BodyDamage {
    private static final String behindStr = "BEHIND";
    private static final String leftStr = "LEFT";
    private static final String rightStr = "RIGHT";
    private final ArrayList<BodyPart> bodyParts = new ArrayList(18);
    private final ArrayList<BodyPartLast> bodyPartsLastState = new ArrayList(18);
    private int damageModCount = 60;
    private float infectionGrowthRate = 0.001f;
    private boolean isInfected;
    private float infectionTime = -1.0f;
    private float infectionMortalityDuration = -1.0f;
    public boolean isFakeInfected;
    private float overallBodyHealth = 100.0f;
    private float standardHealthAddition = 0.002f;
    private float reducedHealthAddition = 0.0013f;
    private float severlyReducedHealthAddition = 8.0E-4f;
    private float sleepingHealthAddition = 0.02f;
    private float healthFromFood = 0.015f;
    private float healthReductionFromSevereBadMoodles = 0.0165f;
    private int standardHealthFromFoodTime = 1600;
    private float healthFromFoodTimer;
    private float boredomDecreaseFromReading = 0.5f;
    private float initialThumpPain = 14.0f;
    private float initialScratchPain = 18.0f;
    private float initialBitePain = 25.0f;
    private float initialWoundPain = 80.0f;
    private float continualPainIncrease = 0.001f;
    private float painReductionFromMeds = 30.0f;
    private float standardPainReductionWhenWell = 0.01f;
    private int oldNumZombiesVisible;
    private int currentNumZombiesVisible;
    private float panicIncreaseValue = 7.0f;
    private final float panicIncreaseValueFrame = 0.035f;
    private float panicReductionValue = 0.06f;
    private float drunkIncreaseValue = 400.0f;
    private float drunkReductionValue = 0.0042f;
    private boolean isOnFire;
    private boolean burntToDeath;
    private float catchACold;
    private boolean hasACold;
    private float coldStrength;
    private float coldProgressionRate = 0.0112f;
    private float timeToSneezeOrCough = -1.0f;
    private final int smokerSneezeTimerMin = 43200;
    private final int smokerSneezeTimerMax = 129600;
    private int mildColdSneezeTimerMin = 600;
    private int mildColdSneezeTimerMax = 800;
    private int coldSneezeTimerMin = 300;
    private int coldSneezeTimerMax = 600;
    private int nastyColdSneezeTimerMin = 200;
    private int nastyColdSneezeTimerMax = 300;
    private int sneezeCoughActive;
    private int sneezeCoughTime;
    private int sneezeCoughDelay = 25;
    private float coldDamageStage;
    private final IsoGameCharacter parentChar;
    private final Stats stats;
    private int remotePainLevel;
    private boolean reduceFakeInfection;
    private float painReduction;
    private float coldReduction;
    private Thermoregulator thermoregulator;
    public static final float InfectionLevelToZombify = 0.001f;
    private boolean wasDraggingCorpse;
    private boolean startedDraggingCorpse;

    public BodyDamage(IsoGameCharacter parentCharacter) {
        this.bodyParts.add(new BodyPart(BodyPartType.Hand_L, parentCharacter));
        this.bodyParts.add(new BodyPart(BodyPartType.Hand_R, parentCharacter));
        this.bodyParts.add(new BodyPart(BodyPartType.ForeArm_L, parentCharacter));
        this.bodyParts.add(new BodyPart(BodyPartType.ForeArm_R, parentCharacter));
        this.bodyParts.add(new BodyPart(BodyPartType.UpperArm_L, parentCharacter));
        this.bodyParts.add(new BodyPart(BodyPartType.UpperArm_R, parentCharacter));
        this.bodyParts.add(new BodyPart(BodyPartType.Torso_Upper, parentCharacter));
        this.bodyParts.add(new BodyPart(BodyPartType.Torso_Lower, parentCharacter));
        this.bodyParts.add(new BodyPart(BodyPartType.Head, parentCharacter));
        this.bodyParts.add(new BodyPart(BodyPartType.Neck, parentCharacter));
        this.bodyParts.add(new BodyPart(BodyPartType.Groin, parentCharacter));
        this.bodyParts.add(new BodyPart(BodyPartType.UpperLeg_L, parentCharacter));
        this.bodyParts.add(new BodyPart(BodyPartType.UpperLeg_R, parentCharacter));
        this.bodyParts.add(new BodyPart(BodyPartType.LowerLeg_L, parentCharacter));
        this.bodyParts.add(new BodyPart(BodyPartType.LowerLeg_R, parentCharacter));
        this.bodyParts.add(new BodyPart(BodyPartType.Foot_L, parentCharacter));
        this.bodyParts.add(new BodyPart(BodyPartType.Foot_R, parentCharacter));
        for (BodyPart part : this.bodyParts) {
            this.bodyPartsLastState.add(new BodyPartLast());
        }
        this.RestoreToFullHealth();
        this.parentChar = parentCharacter;
        this.stats = this.parentChar.getStats();
        if (this.parentChar instanceof IsoPlayer) {
            this.thermoregulator = new Thermoregulator(this);
        }
        this.setBodyPartsLastState();
    }

    public BodyPart getBodyPart(BodyPartType type) {
        return this.bodyParts.get(BodyPartType.ToIndex(type));
    }

    public BodyPartLast getBodyPartsLastState(BodyPartType type) {
        return this.bodyPartsLastState.get(BodyPartType.ToIndex(type));
    }

    public void setBodyPartsLastState() {
        for (int n = 0; n < this.getBodyParts().size(); ++n) {
            BodyPart p = this.getBodyParts().get(n);
            BodyPartLast pls = this.bodyPartsLastState.get(n);
            pls.copy(p);
        }
    }

    public void load(ByteBuffer input, int worldVersion) throws IOException {
        for (int n = 0; n < this.getBodyParts().size(); ++n) {
            BodyPart p = this.getBodyParts().get(n);
            p.setCut(input.get() != 0, false);
            p.SetBitten(input.get() != 0);
            p.setScratched(input.get() != 0, false);
            p.setBandaged(input.get() != 0, 0.0f);
            p.setBleeding(input.get() != 0);
            p.setDeepWounded(input.get() != 0);
            p.SetFakeInfected(input.get() != 0);
            p.SetInfected(input.get() != 0);
            p.SetHealth(input.getFloat());
            if (p.bandaged()) {
                p.setBandageLife(input.getFloat());
            }
            p.setInfectedWound(input.get() != 0);
            if (p.isInfectedWound()) {
                p.setWoundInfectionLevel(input.getFloat());
            }
            p.setCutTime(input.getFloat());
            p.setBiteTime(input.getFloat());
            p.setScratchTime(input.getFloat());
            p.setBleedingTime(input.getFloat());
            p.setAlcoholLevel(input.getFloat());
            p.setAdditionalPain(input.getFloat());
            p.setDeepWoundTime(input.getFloat());
            p.setHaveGlass(input.get() != 0);
            p.setGetBandageXp(input.get() != 0);
            p.setStitched(input.get() != 0);
            p.setStitchTime(input.getFloat());
            p.setGetStitchXp(input.get() != 0);
            p.setGetSplintXp(input.get() != 0);
            p.setFractureTime(input.getFloat());
            p.setSplint(input.get() != 0, 0.0f);
            if (p.isSplint()) {
                p.setSplintFactor(input.getFloat());
            }
            p.setHaveBullet(input.get() != 0, 0);
            p.setBurnTime(input.getFloat());
            p.setNeedBurnWash(input.get() != 0);
            p.setLastTimeBurnWash(input.getFloat());
            p.setSplintItem(GameWindow.ReadString(input));
            p.setBandageType(GameWindow.ReadString(input));
            p.setCutTime(input.getFloat());
            p.setWetness(input.getFloat());
            p.setStiffness(input.getFloat());
            if (worldVersion < 227) continue;
            p.setComfreyFactor(input.getFloat());
            p.setGarlicFactor(input.getFloat());
            p.setPlantainFactor(input.getFloat());
        }
        this.setBodyPartsLastState();
        this.loadMainFields(input, worldVersion);
        if (input.get() != 0) {
            if (this.thermoregulator != null) {
                this.thermoregulator.load(input, worldVersion);
            } else {
                Thermoregulator thermos = new Thermoregulator(this);
                thermos.load(input, worldVersion);
                DebugLog.log("Couldnt load Thermoregulator, == null");
            }
        }
    }

    public void save(ByteBuffer output) throws IOException {
        for (int n = 0; n < this.getBodyParts().size(); ++n) {
            BodyPart p = this.getBodyParts().get(n);
            output.put(p.isCut() ? (byte)1 : 0);
            output.put(p.bitten() ? (byte)1 : 0);
            output.put(p.scratched() ? (byte)1 : 0);
            output.put(p.bandaged() ? (byte)1 : 0);
            output.put(p.bleeding() ? (byte)1 : 0);
            output.put(p.deepWounded() ? (byte)1 : 0);
            output.put(p.IsFakeInfected() ? (byte)1 : 0);
            output.put(p.IsInfected() ? (byte)1 : 0);
            output.putFloat(p.getHealth());
            if (p.bandaged()) {
                output.putFloat(p.getBandageLife());
            }
            output.put(p.isInfectedWound() ? (byte)1 : 0);
            if (p.isInfectedWound()) {
                output.putFloat(p.getWoundInfectionLevel());
            }
            output.putFloat(p.getCutTime());
            output.putFloat(p.getBiteTime());
            output.putFloat(p.getScratchTime());
            output.putFloat(p.getBleedingTime());
            output.putFloat(p.getAlcoholLevel());
            output.putFloat(p.getAdditionalPain());
            output.putFloat(p.getDeepWoundTime());
            output.put(p.haveGlass() ? (byte)1 : 0);
            output.put(p.isGetBandageXp() ? (byte)1 : 0);
            output.put(p.stitched() ? (byte)1 : 0);
            output.putFloat(p.getStitchTime());
            output.put(p.isGetStitchXp() ? (byte)1 : 0);
            output.put(p.isGetSplintXp() ? (byte)1 : 0);
            output.putFloat(p.getFractureTime());
            output.put(p.isSplint() ? (byte)1 : 0);
            if (p.isSplint()) {
                output.putFloat(p.getSplintFactor());
            }
            output.put(p.haveBullet() ? (byte)1 : 0);
            output.putFloat(p.getBurnTime());
            output.put(p.isNeedBurnWash() ? (byte)1 : 0);
            output.putFloat(p.getLastTimeBurnWash());
            GameWindow.WriteString(output, p.getSplintItem());
            GameWindow.WriteString(output, p.getBandageType());
            output.putFloat(p.getCutTime());
            output.putFloat(p.getWetness());
            output.putFloat(p.getStiffness());
            output.putFloat(p.getComfreyFactor());
            output.putFloat(p.getGarlicFactor());
            output.putFloat(p.getPlantainFactor());
        }
        this.saveMainFields(output);
        output.put(this.thermoregulator != null ? (byte)1 : 0);
        if (this.thermoregulator != null) {
            this.thermoregulator.save(output);
        }
    }

    public void saveMainFields(ByteBuffer output) {
        output.putFloat(this.getCatchACold());
        output.put(this.isHasACold() ? (byte)1 : 0);
        output.putFloat(this.getColdStrength());
        output.putInt((int)this.getTimeToSneezeOrCough());
        output.put(this.isReduceFakeInfection() ? (byte)1 : 0);
        output.putFloat(this.healthFromFoodTimer);
        output.putFloat(this.painReduction);
        output.putFloat(this.coldReduction);
        output.putFloat(this.infectionTime);
        output.putFloat(this.infectionMortalityDuration);
        output.putFloat(this.coldDamageStage);
    }

    public void loadMainFields(ByteBuffer input, int worldVersion) {
        this.setCatchACold(input.getFloat());
        this.setHasACold(input.get() != 0);
        this.setColdStrength(input.getFloat());
        if (worldVersion >= 222) {
            this.setTimeToSneezeOrCough(input.getInt());
        }
        this.setReduceFakeInfection(input.get() != 0);
        this.setHealthFromFoodTimer(input.getFloat());
        this.painReduction = input.getFloat();
        this.coldReduction = input.getFloat();
        this.infectionTime = input.getFloat();
        this.infectionMortalityDuration = input.getFloat();
        this.coldDamageStage = input.getFloat();
        this.calculateOverallHealth();
    }

    public boolean IsFakeInfected() {
        return this.isIsFakeInfected();
    }

    public void OnFire(boolean onFire) {
        this.setIsOnFire(onFire);
    }

    public boolean IsOnFire() {
        return this.isIsOnFire();
    }

    public boolean WasBurntToDeath() {
        return this.isBurntToDeath();
    }

    public void IncreasePanicFloat(float delta) {
        float del = 1.0f;
        if (this.parentChar.getBetaEffect() > 0.0f) {
            if ((del -= this.parentChar.getBetaDelta()) > 1.0f) {
                del = 1.0f;
            }
            if (del < 0.0f) {
                del = 0.0f;
            }
        }
        if (this.parentChar.hasTrait(CharacterTrait.COWARDLY)) {
            del *= 2.0f;
        }
        if (this.parentChar.hasTrait(CharacterTrait.BRAVE)) {
            del *= 0.3f;
        }
        if (this.parentChar.hasTrait(CharacterTrait.DESENSITIZED)) {
            del *= 0.15f;
        }
        this.stats.add(CharacterStat.PANIC, this.getPanicIncreaseValueFrame() * delta * del);
        if (this.parentChar.hasTrait(CharacterTrait.DESENSITIZED)) {
            this.stats.reset(CharacterStat.PANIC);
        }
    }

    public void IncreasePanic(int numNewZombiesSeen) {
        if (this.parentChar.getVehicle() != null) {
            numNewZombiesSeen /= 2;
        }
        float del = 1.0f;
        if (this.parentChar.getBetaEffect() > 0.0f) {
            if ((del -= this.parentChar.getBetaDelta()) > 1.0f) {
                del = 1.0f;
            }
            if (del < 0.0f) {
                del = 0.0f;
            }
        }
        if (this.parentChar.hasTrait(CharacterTrait.COWARDLY)) {
            del *= 2.0f;
        }
        if (this.parentChar.hasTrait(CharacterTrait.BRAVE)) {
            del *= 0.3f;
        }
        if (this.parentChar.hasTrait(CharacterTrait.DESENSITIZED)) {
            del *= 0.15f;
        }
        this.stats.add(CharacterStat.PANIC, this.getPanicIncreaseValue() * (float)numNewZombiesSeen * del);
        if (this.parentChar.hasTrait(CharacterTrait.DESENSITIZED)) {
            this.stats.reset(CharacterStat.PANIC);
        }
    }

    public void ReducePanic() {
        if (this.stats.isAtMinimum(CharacterStat.PANIC)) {
            return;
        }
        float delta = this.getPanicReductionValue() * GameTime.getInstance().getThirtyFPSMultiplier();
        int monthSurvived = PZMath.fastfloor((int)this.parentChar.getHoursSurvived() / 24 / 30);
        if (monthSurvived > 5) {
            monthSurvived = 5;
        }
        delta += this.getPanicReductionValue() * (float)monthSurvived;
        if (this.parentChar.isAsleep()) {
            delta *= 2.0f;
        }
        this.stats.remove(CharacterStat.PANIC, delta);
    }

    public void UpdateDraggingCorpse() {
        boolean isDraggingCorpse = this.parentChar.isDraggingCorpse();
        if (isDraggingCorpse != this.getWasDraggingCorpse()) {
            this.startedDraggingCorpse = isDraggingCorpse;
            this.setWasDraggingCorpse(isDraggingCorpse);
        } else {
            this.startedDraggingCorpse = false;
        }
    }

    public void UpdatePanicState() {
        int numVisibleZombies = this.stats.numVisibleZombies;
        int oldNumZombiesVisible = this.getOldNumZombiesVisible();
        this.setOldNumZombiesVisible(numVisibleZombies);
        int inNumNewZombies = numVisibleZombies - oldNumZombiesVisible;
        if (this.parentChar.hasTrait(CharacterTrait.DESENSITIZED)) {
            this.stats.reset(CharacterStat.PANIC);
            return;
        }
        int increasePanicCount = 0;
        if (inNumNewZombies > 0) {
            increasePanicCount += inNumNewZombies;
        }
        if (increasePanicCount > 0) {
            this.IncreasePanic(increasePanicCount);
        } else {
            this.ReducePanic();
        }
    }

    public void JustDrankBooze(Food food, float percentage) {
        float del = 1.0f;
        if (food.getBaseHunger() != 0.0f) {
            percentage = food.getHungChange() * percentage / food.getBaseHunger() * 2.0f;
        }
        del *= percentage;
        if (food.getName().toLowerCase().contains("beer") || food.hasTag(ItemTag.LOW_ALCOHOL)) {
            del *= 0.25f;
        }
        if (this.stats.get(CharacterStat.HUNGER) > 0.8f) {
            del *= 1.25f;
        } else if (this.stats.get(CharacterStat.HUNGER) > 0.6f) {
            del *= 1.1f;
        }
        this.stats.add(CharacterStat.INTOXICATION, this.getDrunkIncreaseValue() * del);
        this.parentChar.SleepingTablet(0.02f * percentage);
        this.parentChar.BetaAntiDepress(0.4f * percentage);
        this.parentChar.BetaBlockers(0.2f * percentage);
        this.parentChar.PainMeds(0.2f * percentage);
    }

    public void JustDrankBoozeFluid(float alcohol) {
        float del = 1.0f;
        del *= alcohol;
        if (this.stats.get(CharacterStat.HUNGER) > 0.8f) {
            del *= 1.1f;
        } else if (this.stats.get(CharacterStat.HUNGER) > 0.6f) {
            del *= 1.25f;
        }
        this.stats.add(CharacterStat.INTOXICATION, this.getDrunkIncreaseValue() * del);
        this.parentChar.SleepingTablet(0.02f * alcohol);
        this.parentChar.BetaAntiDepress(0.4f * alcohol);
        this.parentChar.BetaBlockers(0.2f * alcohol);
        this.parentChar.PainMeds(0.2f * alcohol);
    }

    public void JustTookPill(InventoryItem pill) {
        if ("PillsBeta".equals(pill.getType())) {
            if (this.parentChar != null && this.stats.get(CharacterStat.INTOXICATION) > 10.0f) {
                this.parentChar.BetaBlockers(0.15f);
            } else {
                this.parentChar.BetaBlockers(0.3f);
            }
        } else if ("PillsAntiDep".equals(pill.getType())) {
            if (this.parentChar != null && this.stats.get(CharacterStat.INTOXICATION) > 10.0f) {
                this.parentChar.BetaAntiDepress(0.15f);
            } else {
                this.parentChar.BetaAntiDepress(0.3f);
            }
        } else if ("PillsSleepingTablets".equals(pill.getType())) {
            this.parentChar.SleepingTablet(0.1f);
            IsoGameCharacter isoGameCharacter = this.parentChar;
            if (isoGameCharacter instanceof IsoPlayer) {
                IsoPlayer isoPlayer = (IsoPlayer)isoGameCharacter;
                isoPlayer.setSleepingPillsTaken(isoPlayer.getSleepingPillsTaken() + 1);
            }
        } else if ("Pills".equals(pill.getType())) {
            if (this.parentChar != null && this.stats.get(CharacterStat.INTOXICATION) > 10.0f) {
                this.parentChar.PainMeds(0.15f);
            } else {
                this.parentChar.PainMeds(0.45f);
            }
        } else if ("PillsVitamins".equals(pill.getType())) {
            if (this.parentChar != null && this.stats.get(CharacterStat.INTOXICATION) > 10.0f) {
                this.stats.add(CharacterStat.FATIGUE, pill.getFatigueChange() / 2.0f);
            } else {
                this.stats.add(CharacterStat.FATIGUE, pill.getFatigueChange());
            }
        }
        this.stats.add(CharacterStat.STRESS, pill.getStressChange());
        DrainableComboItem pill2 = (DrainableComboItem)pill;
        Object functionObj = LuaManager.getFunctionObject(pill2.getOnEat());
        if (functionObj != null) {
            LuaManager.caller.pcallvoid(LuaManager.thread, functionObj, pill, this.parentChar);
        }
        pill.UseAndSync();
    }

    public void JustAteFood(Food newFood, float percentage) {
        this.JustAteFood(newFood, percentage, false);
    }

    public void JustAteFood(Food newFood, float percentage, boolean useUtensil) {
        Object debugStr;
        IsoPlayer isoPlayer;
        float poisonPower;
        if (newFood.getPoisonPower() > 0) {
            poisonPower = (float)newFood.getPoisonPower() * percentage;
            if (this.parentChar.hasTrait(CharacterTrait.IRON_GUT) && !Objects.equals(newFood.getType(), "Bleach")) {
                poisonPower /= 2.0f;
            }
            if (this.parentChar.hasTrait(CharacterTrait.WEAK_STOMACH)) {
                poisonPower *= 2.0f;
            }
            this.stats.add(CharacterStat.POISON, poisonPower);
            this.stats.add(CharacterStat.PAIN, (float)newFood.getPoisonPower() * percentage / 6.0f);
            IsoGameCharacter isoGameCharacter = this.parentChar;
            if (isoGameCharacter instanceof IsoPlayer) {
                isoPlayer = (IsoPlayer)isoGameCharacter;
                debugStr = String.format("Player %s just ate poisoned food %s with poison power %f", isoPlayer.getDisplayName(), newFood.getDisplayName(), Float.valueOf(poisonPower));
                DebugLog.Objects.debugln(debugStr);
                LoggerManager.getLogger("user").write((String)debugStr);
            }
        }
        if (newFood.isTainted()) {
            poisonPower = 20.0f * percentage;
            this.stats.add(CharacterStat.POISON, poisonPower);
            this.stats.add(CharacterStat.PAIN, 10.0f * percentage / 6.0f);
            debugStr = this.parentChar;
            if (debugStr instanceof IsoPlayer) {
                isoPlayer = (IsoPlayer)debugStr;
                debugStr = String.format("Player %s just ate tainted food %s with poison power %f", isoPlayer.getDisplayName(), newFood.getDisplayName(), Float.valueOf(poisonPower));
                DebugLog.Objects.debugln(debugStr);
                LoggerManager.getLogger("user").write((String)debugStr);
            }
        }
        if (newFood.getReduceInfectionPower() > 0.0f) {
            this.parentChar.setReduceInfectionPower(newFood.getReduceInfectionPower());
        }
        float modifier = 1.0f;
        if (useUtensil) {
            modifier = newFood.getBoredomChange() * percentage < 0.0f ? 1.25f : 0.75f;
            DebugLog.log("boredomChange %modifier from using an eating utensil: " + modifier);
        }
        this.stats.add(CharacterStat.BOREDOM, newFood.getBoredomChange() * percentage * modifier);
        modifier = 1.0f;
        if (useUtensil) {
            modifier = newFood.getUnhappyChange() * percentage < 0.0f ? 1.25f : 0.75f;
            DebugLog.log("unhappyChange %modifier from using an eating utensil: " + modifier);
        }
        this.stats.add(CharacterStat.UNHAPPINESS, newFood.getUnhappyChange() * percentage * modifier);
        if (newFood.isAlcoholic()) {
            this.JustDrankBooze(newFood, percentage);
        }
        if (this.stats.isAtMinimum(CharacterStat.HUNGER)) {
            float hungerChange = Math.abs(newFood.getHungerChange()) * percentage;
            this.setHealthFromFoodTimer((int)(this.getHealthFromFoodTimer() + hungerChange * this.getHealthFromFoodTimeByHunger()));
            if (newFood.isCooked()) {
                this.setHealthFromFoodTimer((int)(this.getHealthFromFoodTimer() + hungerChange * this.getHealthFromFoodTimeByHunger()));
            }
            if (this.getHealthFromFoodTimer() > 11000.0f) {
                this.setHealthFromFoodTimer(11000.0f);
            }
        }
        if ("Tutorial".equals(Core.getInstance().getGameMode())) {
            return;
        }
        if (!newFood.isCooked() && newFood.isbDangerousUncooked()) {
            this.setHealthFromFoodTimer(0.0f);
            int illnessChance = 75;
            if (newFood.hasTag(ItemTag.EGG)) {
                illnessChance = 5;
            }
            if (this.parentChar.hasTrait(CharacterTrait.IRON_GUT)) {
                illnessChance /= 2;
                if (newFood.hasTag(ItemTag.EGG)) {
                    illnessChance = 0;
                }
            }
            if (this.parentChar.hasTrait(CharacterTrait.WEAK_STOMACH)) {
                illnessChance *= 2;
            }
            if (illnessChance > 0 && !this.isInfected() && !newFood.isBurnt()) {
                this.stats.add(CharacterStat.POISON, 15.0f * percentage);
            }
        }
        if (newFood.getAge() >= (float)newFood.getOffAgeMax()) {
            float offness = newFood.getAge() - (float)newFood.getOffAgeMax();
            if (offness == 0.0f) {
                offness = 1.0f;
            }
            if (offness > 5.0f) {
                offness = 5.0f;
            }
            int illnessChance = newFood.getOffAgeMax() > newFood.getOffAge() ? (int)(offness / (float)(newFood.getOffAgeMax() - newFood.getOffAge()) * 100.0f) : 100;
            if (this.parentChar.hasTrait(CharacterTrait.IRON_GUT)) {
                illnessChance /= 2;
            }
            if (this.parentChar.hasTrait(CharacterTrait.WEAK_STOMACH)) {
                illnessChance *= 2;
            }
            if (!this.isInfected()) {
                if (Rand.Next(100) < illnessChance) {
                    float poisonPower2 = 5.0f * Math.abs(newFood.getHungChange() * 10.0f) * percentage;
                    this.stats.add(CharacterStat.POISON, poisonPower2);
                    IsoGameCharacter isoGameCharacter = this.parentChar;
                    if (isoGameCharacter instanceof IsoPlayer) {
                        IsoPlayer isoPlayer2 = (IsoPlayer)isoGameCharacter;
                        String debugStr2 = String.format("Player %s just ate spoiled food %s with poison power %f", isoPlayer2.getDisplayName(), newFood.getDisplayName(), Float.valueOf(poisonPower2));
                        DebugLog.Objects.debugln(debugStr2);
                        LoggerManager.getLogger("user").write(debugStr2);
                    }
                } else {
                    this.stats.add(CharacterStat.POISON, 2.0f * Math.abs(newFood.getHungChange() * 10.0f) * percentage);
                }
            }
        }
    }

    public void JustAteFood(Food newFood) {
        this.JustAteFood(newFood, 100.0f);
    }

    private float getHealthFromFoodTimeByHunger() {
        return 13000.0f;
    }

    public void JustReadSomething(Literature literature) {
        this.stats.add(CharacterStat.BOREDOM, literature.getBoredomChange());
        this.stats.add(CharacterStat.UNHAPPINESS, literature.getUnhappyChange());
    }

    public void JustTookPainMeds() {
        this.stats.remove(CharacterStat.PAIN, this.getPainReductionFromMeds());
    }

    public void UpdateWetness() {
        VehicleWindow window;
        VehiclePart windshield;
        boolean isOutside;
        IsoGridSquare square = this.parentChar.getCurrentSquare();
        BaseVehicle vehicle = this.parentChar.getVehicle();
        boolean bl = isOutside = square == null || !square.isInARoom() && !square.haveRoof;
        if (vehicle != null && vehicle.hasRoof(vehicle.getSeat(this.parentChar))) {
            isOutside = false;
        }
        ClothingWetness clothingWetness = this.parentChar.getClothingWetness();
        float wetnessIncrease = 0.0f;
        float wetnessDecrease = 0.0f;
        float windshieldMod = 0.0f;
        if (vehicle != null && ClimateManager.getInstance().isRaining() && (windshield = vehicle.getPartById("Windshield")) != null && (window = windshield.getWindow()) != null && window.isDestroyed()) {
            float val = ClimateManager.getInstance().getRainIntensity();
            val *= val;
            if ((val *= vehicle.getCurrentSpeedKmHour() / 50.0f) < 0.1f) {
                val = 0.0f;
            }
            if (val > 1.0f) {
                val = 1.0f;
            }
            windshieldMod = val * 3.0f;
            wetnessIncrease = val;
        }
        if (isOutside && (this.parentChar.isAsleep() || this.parentChar.isSitOnGround() || this.parentChar.isSittingOnFurniture() || this.parentChar.isResting()) && this.parentChar.getBed() != null && this.parentChar.getBed().getSprite() != null && this.parentChar.getBed().isTent()) {
            isOutside = false;
        }
        if (isOutside && ClimateManager.getInstance().isRaining()) {
            float val = ClimateManager.getInstance().getRainIntensity();
            if (val < 0.1f) {
                val = 0.0f;
            }
            wetnessIncrease = val;
        } else if (!isOutside || !ClimateManager.getInstance().isRaining()) {
            float temperature = ClimateManager.getInstance().getAirTemperatureForCharacter(this.parentChar);
            float val = 0.1f;
            if (temperature > 5.0f) {
                val += (temperature - 5.0f) / 10.0f;
            }
            if ((val -= windshieldMod) < 0.0f) {
                val = 0.0f;
            }
            wetnessDecrease = val;
        }
        if (clothingWetness != null) {
            clothingWetness.updateWetness(wetnessIncrease, wetnessDecrease);
            if (GameServer.server) {
                this.parentChar.getClothingWetnessSync().update();
            }
        }
        float currentWetness = this.stats.get(CharacterStat.WETNESS);
        float averageWetness = 0.0f;
        if (!this.bodyParts.isEmpty()) {
            for (BodyPart bodyPart : this.bodyParts) {
                averageWetness += bodyPart.getWetness();
            }
            averageWetness /= (float)this.bodyParts.size();
        }
        float mergeFactor = 0.1f;
        float targetWetness = averageWetness + (currentWetness - averageWetness) * 0.1f;
        if (!this.bodyParts.isEmpty()) {
            for (BodyPart bodyPart : this.bodyParts) {
                bodyPart.setWetness(targetWetness);
            }
        }
        this.stats.set(CharacterStat.WETNESS, targetWetness);
        float delta = 0.0f;
        if (this.thermoregulator != null) {
            delta = this.thermoregulator.getCatchAColdDelta();
        }
        if (!this.isHasACold() && delta > 0.1f) {
            if (this.parentChar.hasTrait(CharacterTrait.PRONE_TO_ILLNESS)) {
                delta *= 1.7f;
            }
            if (this.parentChar.hasTrait(CharacterTrait.RESILIENT)) {
                delta *= 0.45f;
            }
            if (this.parentChar.hasTrait(CharacterTrait.OUTDOORSMAN)) {
                delta *= 0.25f;
            }
            this.setCatchACold(this.getCatchACold() + (float)ZomboidGlobals.catchAColdIncreaseRate * delta * GameTime.instance.getMultiplier());
            if (this.getCatchACold() >= 100.0f) {
                this.setCatchACold(0.0f);
                this.setHasACold(true);
                this.setColdStrength(20.0f);
                this.setTimeToSneezeOrCough(0.0f);
            }
        }
        if (delta <= 0.1f) {
            this.setCatchACold(this.getCatchACold() - (float)ZomboidGlobals.catchAColdDecreaseRate);
            if (this.getCatchACold() <= 0.0f) {
                this.setCatchACold(0.0f);
            }
        }
    }

    public void TriggerSneezeCough() {
        boolean smoker;
        if (this.getSneezeCoughActive() > 0) {
            return;
        }
        boolean bl = smoker = this.parentChar.getMoodles().getMoodleLevel(MoodleType.HAS_A_COLD) < 1 && this.parentChar.hasTrait(CharacterTrait.SMOKER);
        if (Rand.Next(100) > 50 && !smoker) {
            this.setSneezeCoughActive(1);
        } else {
            this.setSneezeCoughActive(2);
        }
        if (this.parentChar.getMoodles().getMoodleLevel(MoodleType.HAS_A_COLD) == 2) {
            this.setSneezeCoughActive(1);
        }
        this.setSneezeCoughTime(this.getSneezeCoughDelay());
        if (this.parentChar.getMoodles().getMoodleLevel(MoodleType.HAS_A_COLD) == 4) {
            this.setTimeToSneezeOrCough(this.getNastyColdSneezeTimerMin() + Rand.Next(this.getNastyColdSneezeTimerMax() - this.getNastyColdSneezeTimerMin()));
        } else if (this.parentChar.getMoodles().getMoodleLevel(MoodleType.HAS_A_COLD) == 3) {
            this.setTimeToSneezeOrCough(this.getColdSneezeTimerMin() + Rand.Next(this.getColdSneezeTimerMax() - this.getColdSneezeTimerMin()));
        } else if (this.parentChar.getMoodles().getMoodleLevel(MoodleType.HAS_A_COLD) == 2) {
            this.setTimeToSneezeOrCough(this.getMildColdSneezeTimerMin() + Rand.Next(this.getMildColdSneezeTimerMax() - this.getMildColdSneezeTimerMin()));
        } else if (smoker) {
            this.setTimeToSneezeOrCough(this.getSmokerSneezeTimerMin() + Rand.Next(this.getSmokerSneezeTimerMax() - this.getSmokerSneezeTimerMin()));
        }
        boolean tissueConsumed = false;
        if (this.parentChar.getPrimaryHandItem() != null && (this.parentChar.getPrimaryHandItem().getType().equals("Tissue") || this.parentChar.getPrimaryHandItem().getType().equals("ToiletPaper") || this.parentChar.getPrimaryHandItem().hasTag(ItemTag.MUFFLE_SNEEZE))) {
            if (this.parentChar.getPrimaryHandItem().getCurrentUses() > 0) {
                this.parentChar.getPrimaryHandItem().setCurrentUses(this.parentChar.getPrimaryHandItem().getCurrentUses() - 1);
                if (this.parentChar.getPrimaryHandItem().getCurrentUses() <= 0) {
                    this.parentChar.getPrimaryHandItem().Use();
                }
                tissueConsumed = true;
            }
        } else if (this.parentChar.getSecondaryHandItem() != null && (this.parentChar.getSecondaryHandItem().getType().equals("Tissue") || this.parentChar.getSecondaryHandItem().getType().equals("ToiletPaper") || this.parentChar.getSecondaryHandItem().hasTag(ItemTag.MUFFLE_SNEEZE)) && this.parentChar.getSecondaryHandItem().getCurrentUses() > 0) {
            this.parentChar.getSecondaryHandItem().setCurrentUses(this.parentChar.getSecondaryHandItem().getCurrentUses() - 1);
            if (this.parentChar.getSecondaryHandItem().getCurrentUses() <= 0) {
                this.parentChar.getSecondaryHandItem().Use();
            }
            tissueConsumed = true;
        }
        if (tissueConsumed) {
            this.setSneezeCoughActive(this.getSneezeCoughActive() + 2);
        } else {
            int dist = 20;
            int vol = 20;
            if (this.getSneezeCoughActive() == 1) {
                dist = 20;
                vol = 25;
            }
            if (this.getSneezeCoughActive() == 2) {
                dist = 35;
                vol = 40;
            }
            WorldSoundManager.WorldSound sneeze = WorldSoundManager.instance.addSound(this.parentChar, PZMath.fastfloor(this.parentChar.getX()), PZMath.fastfloor(this.parentChar.getY()), PZMath.fastfloor(this.parentChar.getZ()), dist, vol, false);
            sneeze.stressAnimals = false;
        }
    }

    public int IsSneezingCoughing() {
        return this.getSneezeCoughActive();
    }

    public void UpdateCold() {
        if (this.isHasACold()) {
            boolean recovering = true;
            IsoGridSquare sq = this.parentChar.getCurrentSquare();
            if (sq == null || !sq.isInARoom() || this.parentChar.getMoodles().getMoodleLevel(MoodleType.WET) > 0 || this.parentChar.getMoodles().getMoodleLevel(MoodleType.HYPOTHERMIA) >= 1 || this.stats.get(CharacterStat.FATIGUE) > 0.5f || this.stats.get(CharacterStat.HUNGER) > 0.25f || this.stats.get(CharacterStat.THIRST) > 0.25f) {
                recovering = false;
            }
            if (this.getColdReduction() > 0.0f) {
                recovering = true;
                this.setColdReduction(this.getColdReduction() - 0.005f * GameTime.instance.getMultiplier());
                if (this.getColdReduction() < 0.0f) {
                    this.setColdReduction(0.0f);
                }
            }
            if (recovering) {
                float delta = 1.0f;
                if (this.parentChar.hasTrait(CharacterTrait.PRONE_TO_ILLNESS)) {
                    delta = 0.5f;
                }
                if (this.parentChar.hasTrait(CharacterTrait.RESILIENT)) {
                    delta = 1.5f;
                }
                this.setColdStrength(this.getColdStrength() - this.getColdProgressionRate() * delta * GameTime.instance.getMultiplier());
                if (this.getColdReduction() > 0.0f) {
                    this.setColdStrength(this.getColdStrength() - this.getColdProgressionRate() * delta * GameTime.instance.getMultiplier());
                }
                if (this.getColdStrength() < 0.0f) {
                    this.setColdStrength(0.0f);
                    this.setHasACold(false);
                    this.setCatchACold(0.0f);
                }
            } else {
                float delta = 1.0f;
                if (this.parentChar.hasTrait(CharacterTrait.PRONE_TO_ILLNESS)) {
                    delta = 1.2f;
                }
                if (this.parentChar.hasTrait(CharacterTrait.RESILIENT)) {
                    delta = 0.8f;
                }
                this.setColdStrength(this.getColdStrength() + this.getColdProgressionRate() * delta * GameTime.instance.getMultiplier());
                if (this.getColdStrength() > 100.0f) {
                    this.setColdStrength(100.0f);
                }
            }
            if (this.getSneezeCoughTime() > 0) {
                this.setSneezeCoughTime(this.getSneezeCoughTime() - 1);
                if (this.getSneezeCoughTime() == 0) {
                    this.setSneezeCoughActive(0);
                }
            }
            if (this.parentChar.getMoodles().getMoodleLevel(MoodleType.HAS_A_COLD) > 1 && this.getTimeToSneezeOrCough() >= 0.0f && !this.parentChar.IsSpeaking()) {
                this.setTimeToSneezeOrCough(this.getTimeToSneezeOrCough() - 1.0f);
                if (this.getTimeToSneezeOrCough() <= 0.0f) {
                    this.TriggerSneezeCough();
                }
            }
        } else if (this.parentChar.hasTrait(CharacterTrait.SMOKER)) {
            if (this.getSneezeCoughTime() > 0) {
                this.setSneezeCoughTime(this.getSneezeCoughTime() - 1);
                if (this.getSneezeCoughTime() == 0) {
                    this.setSneezeCoughActive(0);
                }
            }
            if (this.getTimeToSneezeOrCough() >= 0.0f) {
                if (!this.parentChar.IsSpeaking()) {
                    this.setTimeToSneezeOrCough(this.getTimeToSneezeOrCough() - GameTime.instance.getGameWorldSecondsSinceLastUpdate());
                    if (this.getTimeToSneezeOrCough() <= 0.0f) {
                        this.TriggerSneezeCough();
                    }
                }
            } else {
                this.setTimeToSneezeOrCough(this.getSmokerSneezeTimerMin() + Rand.Next(this.getSmokerSneezeTimerMax() - this.getSmokerSneezeTimerMin()));
            }
        }
    }

    public float getColdStrength() {
        if (this.isHasACold()) {
            return this.coldStrength;
        }
        return 0.0f;
    }

    public void AddDamage(BodyPartType bodyPart, float val) {
        this.getBodyParts().get(BodyPartType.ToIndex(bodyPart)).AddDamage(val);
    }

    public void AddGeneralHealth(float val) {
        int numDamagedParts = 0;
        for (int i = 0; i < BodyPartType.ToIndex(BodyPartType.MAX); ++i) {
            if (!(this.getBodyParts().get(i).getHealth() < 100.0f)) continue;
            ++numDamagedParts;
        }
        if (numDamagedParts > 0) {
            float healthPerPart = val / (float)numDamagedParts;
            for (int i = 0; i < BodyPartType.ToIndex(BodyPartType.MAX); ++i) {
                if (!(this.getBodyParts().get(i).getHealth() < 100.0f)) continue;
                this.getBodyParts().get(i).AddHealth(healthPerPart);
            }
        }
    }

    public void ReduceGeneralHealth(float val) {
        if (this.getOverallBodyHealth() <= 10.0f) {
            this.parentChar.forceAwake();
        }
        if (val <= 0.0f) {
            return;
        }
        float healthPerPart = val / (float)BodyPartType.ToIndex(BodyPartType.MAX);
        for (int i = 0; i < BodyPartType.ToIndex(BodyPartType.MAX); ++i) {
            this.getBodyParts().get(i).ReduceHealth(healthPerPart / BodyPartType.getDamageModifyer(i));
        }
    }

    public void AddDamage(int bodyPartIndex, float val) {
        this.getBodyParts().get(bodyPartIndex).AddDamage(val);
    }

    public void splatBloodFloorBig() {
        this.parentChar.splatBloodFloorBig();
        this.parentChar.splatBloodFloorBig();
        this.parentChar.splatBloodFloorBig();
    }

    private static boolean isSpikedPart(IsoGameCharacter owner, IsoGameCharacter target, int partIndex) {
        boolean behind = !owner.isAimAtFloor() ? owner.isBehind(target) : target.isFallOnFront();
        boolean spikedPart = behind ? target.bodyPartIsSpikedBehind(partIndex) : target.bodyPartIsSpiked(partIndex);
        return spikedPart;
    }

    public static void damageFromSpikedArmor(IsoGameCharacter owner, IsoGameCharacter target, int partIndex, HandWeapon weapon) {
        IsoLivingCharacter isoLivingCharacter;
        boolean shove;
        boolean bl = shove = owner instanceof IsoLivingCharacter && (isoLivingCharacter = (IsoLivingCharacter)owner).isDoShove();
        if (owner != null && (shove || WeaponType.getWeaponType(weapon) == WeaponType.KNIFE)) {
            boolean spikedSecondary;
            boolean spikedPart = BodyDamage.isSpikedPart(owner, target, partIndex);
            boolean spikedFoot = spikedPart && owner.isAimAtFloor() && shove;
            boolean spikedPrimary = spikedPart && !spikedFoot && (owner.getPrimaryHandItem() == null || owner.getPrimaryHandItem() instanceof HandWeapon);
            boolean bl2 = spikedSecondary = spikedPart && !spikedFoot && (owner.getSecondaryHandItem() == null || owner.getSecondaryHandItem() instanceof HandWeapon) && shove;
            if (spikedFoot) {
                target.addBlood(BloodBodyPartType.FromIndex(partIndex), true, false, false);
                owner.spikePart(BodyPartType.Foot_R);
            }
            if (spikedPrimary) {
                target.addBlood(BloodBodyPartType.FromIndex(partIndex), true, false, false);
                owner.spikePart(BodyPartType.Hand_R);
            }
            if (spikedSecondary) {
                target.addBlood(BloodBodyPartType.FromIndex(partIndex), true, false, false);
                owner.spikePart(BodyPartType.Hand_L);
            }
        }
    }

    public void applyDamageFromWeapon(int partIndex, float damage, int damageType, float pain) {
        BodyPart part = this.getBodyPart(BodyPartType.FromIndex(partIndex));
        switch (damageType) {
            case 1: {
                part.generateDeepWound();
                break;
            }
            case 2: 
            case 4: {
                part.setCut(true);
                break;
            }
            case 3: 
            case 5: {
                part.setScratched(true, true);
                break;
            }
            case 6: {
                part.setHaveBullet(true, 0);
            }
        }
        this.AddDamage(partIndex, damage);
        this.stats.add(CharacterStat.PAIN, pain);
        if (GameServer.server) {
            this.parentChar.getNetworkCharacterAI().syncDamage();
        }
    }

    public void DamageFromWeapon(HandWeapon weapon, int partIndex) {
        IsoPlayer player;
        if (GameClient.client && (player = Type.tryCastTo(this.parentChar, IsoPlayer.class)) != null && !player.isLocalPlayer()) {
            return;
        }
        int damageType = 0;
        boolean blunt = false;
        boolean blade = false;
        boolean bullet = false;
        if (weapon.isOfWeaponCategory(WeaponCategory.BLUNT) || weapon.isOfWeaponCategory(WeaponCategory.SMALL_BLUNT)) {
            blunt = true;
        } else if (!weapon.isAimedFirearm()) {
            blade = true;
        } else {
            bullet = true;
        }
        if (partIndex == -1) {
            partIndex = Rand.Next(BodyPartType.ToIndex(BodyPartType.Hand_L), BodyPartType.ToIndex(BodyPartType.MAX));
        }
        BodyPart part = this.getBodyPart(BodyPartType.FromIndex(partIndex));
        float def = this.parentChar.getBodyPartClothingDefense(part.getIndex(), blade, bullet);
        if ((float)Rand.Next(100) < def) {
            boolean spikedPart;
            IsoPlayer owner = weapon.getUsingPlayer();
            if (owner != null && WeaponType.getWeaponType(weapon) == WeaponType.KNIFE && !weapon.hasTag(ItemTag.HANDGUARD) && (spikedPart = BodyDamage.isSpikedPart(owner, this.parentChar, partIndex))) {
                this.parentChar.addBlood(BloodBodyPartType.FromIndex(partIndex), true, false, false);
                owner.spikePart(BodyPartType.Hand_R);
            }
            this.parentChar.addHoleFromZombieAttacks(BloodBodyPartType.FromIndex(partIndex), false);
            this.parentChar.playWeaponHitArmourSound(partIndex, bullet);
            return;
        }
        this.parentChar.addHole(BloodBodyPartType.FromIndex(partIndex));
        this.parentChar.splatBloodFloorBig();
        this.parentChar.splatBloodFloorBig();
        this.parentChar.splatBloodFloorBig();
        float pain = 0.0f;
        if (blade) {
            if (Rand.NextBool(6)) {
                damageType = 1;
                part.generateDeepWound();
            } else if (Rand.NextBool(3)) {
                damageType = 2;
                part.setCut(true);
            } else {
                damageType = 3;
                part.setScratched(true, true);
            }
            pain = this.getInitialScratchPain() * BodyPartType.getPainModifyer(partIndex);
        } else if (blunt) {
            if (Rand.NextBool(4)) {
                damageType = 4;
                part.setCut(true);
            } else {
                damageType = 5;
                part.setScratched(true, true);
            }
            pain = this.getInitialThumpPain() * BodyPartType.getPainModifyer(partIndex);
        } else if (bullet) {
            damageType = 6;
            part.setHaveBullet(true, 0);
            pain = this.getInitialBitePain() * BodyPartType.getPainModifyer(partIndex);
        }
        float damage = Rand.Next(weapon.getMinDamage(), weapon.getMaxDamage()) * 15.0f;
        if (partIndex == BodyPartType.ToIndex(BodyPartType.Head)) {
            damage *= 4.0f;
        }
        if (partIndex == BodyPartType.ToIndex(BodyPartType.Neck)) {
            damage *= 4.0f;
        }
        if (partIndex == BodyPartType.ToIndex(BodyPartType.Torso_Upper)) {
            damage *= 2.0f;
        }
        if (GameServer.server) {
            damage = weapon.isRanged() ? (float)((double)damage * ServerOptions.getInstance().pvpFirearmDamageModifier.getValue()) : (float)((double)damage * ServerOptions.getInstance().pvpMeleeDamageModifier.getValue());
        }
        BodyDamage.damageFromSpikedArmor(weapon.getUsingPlayer(), this.parentChar, partIndex, weapon);
        this.applyDamageFromWeapon(partIndex, damage, damageType, pain);
        this.parentChar.playWeaponHitArmourSound(partIndex, bullet);
    }

    public boolean AddRandomDamageFromZombie(IsoZombie zombie, String hitReaction) {
        IsoGameCharacter isoGameCharacter;
        boolean behind;
        int partIndex;
        if (StringUtils.isNullOrEmpty(hitReaction)) {
            hitReaction = "Bite";
        }
        this.parentChar.setVariable("hitpvp", false);
        int painType = 0;
        int baseChance = 15 + this.parentChar.getMeleeCombatMod();
        int baseBiteChance = 85;
        int baseLacerationChance = 65;
        String dotSide = this.parentChar.testDotSide(zombie);
        boolean isBehind = dotSide.equals(behindStr);
        boolean isLeftOrRight = dotSide.equals(leftStr) || dotSide.equals(rightStr);
        int zombiesAttacking = this.parentChar.getSurroundingAttackingZombies();
        zombiesAttacking = Math.max(zombiesAttacking, 1);
        baseChance -= (zombiesAttacking - 1) * 10;
        baseBiteChance -= (zombiesAttacking - 1) * 30;
        baseLacerationChance -= (zombiesAttacking - 1) * 15;
        int neededZedToDragDown = 3;
        if (SandboxOptions.instance.lore.strength.getValue() == 1) {
            neededZedToDragDown = 2;
        }
        if (SandboxOptions.instance.lore.strength.getValue() == 3) {
            neededZedToDragDown = 6;
        }
        if (this.parentChar.hasTrait(CharacterTrait.THICK_SKINNED)) {
            baseChance = (int)((double)baseChance * 1.3);
        }
        if (this.parentChar.hasTrait(CharacterTrait.THIN_SKINNED)) {
            baseChance = (int)((double)baseChance / 1.3);
        }
        int dragDownZeds = this.parentChar.getSurroundingAttackingZombies(SandboxOptions.instance.lore.zombiesCrawlersDragDown.getValue());
        if (!"EndDeath".equals(this.parentChar.getHitReaction())) {
            if (!this.parentChar.isGodMod() && dragDownZeds >= neededZedToDragDown && SandboxOptions.instance.lore.zombiesDragDown.getValue() && !this.parentChar.isSitOnGround()) {
                baseBiteChance = 0;
                baseLacerationChance = 0;
                baseChance = 0;
                this.parentChar.setHitReaction("EndDeath");
                this.parentChar.setDeathDragDown(true);
            } else {
                this.parentChar.setHitReaction(hitReaction);
            }
        }
        if (isBehind) {
            baseChance -= 15;
            baseBiteChance -= 25;
            baseLacerationChance -= 35;
            if (SandboxOptions.instance.rearVulnerability.getValue() == 1) {
                baseChance += 15;
                baseBiteChance += 25;
                baseLacerationChance += 35;
            }
            if (SandboxOptions.instance.rearVulnerability.getValue() == 2) {
                baseChance += 7;
                baseBiteChance += 17;
                baseLacerationChance += 23;
            }
            if (zombiesAttacking > 2) {
                baseBiteChance -= 15;
                baseLacerationChance -= 15;
            }
        }
        if (isLeftOrRight) {
            baseChance -= 30;
            baseBiteChance -= 7;
            baseLacerationChance -= 27;
            if (SandboxOptions.instance.rearVulnerability.getValue() == 1) {
                baseChance += 30;
                baseBiteChance += 7;
                baseLacerationChance += 27;
            }
            if (SandboxOptions.instance.rearVulnerability.getValue() == 2) {
                baseChance += 15;
                baseBiteChance += 4;
                baseLacerationChance += 15;
            }
        }
        if (!zombie.crawling) {
            partIndex = Rand.Next(10) == 0 ? Rand.Next(BodyPartType.ToIndex(BodyPartType.Hand_L), BodyPartType.ToIndex(BodyPartType.Groin) + 1) : Rand.Next(BodyPartType.ToIndex(BodyPartType.Hand_L), BodyPartType.ToIndex(BodyPartType.Neck) + 1);
            float chanceToGetNeck = 10.0f * (float)zombiesAttacking;
            if (isBehind) {
                chanceToGetNeck += 5.0f;
            }
            if (isLeftOrRight) {
                chanceToGetNeck += 2.0f;
            }
            if (isBehind && (float)Rand.Next(100) < chanceToGetNeck) {
                partIndex = BodyPartType.ToIndex(BodyPartType.Neck);
            }
            if (partIndex == BodyPartType.ToIndex(BodyPartType.Head) || partIndex == BodyPartType.ToIndex(BodyPartType.Neck)) {
                int percent = 70;
                if (isBehind) {
                    percent = 90;
                }
                if (isLeftOrRight) {
                    percent = 80;
                }
                if (Rand.Next(100) > percent) {
                    boolean done = false;
                    while (!done) {
                        done = true;
                        partIndex = Rand.Next(BodyPartType.ToIndex(BodyPartType.Torso_Lower) + 1);
                        if (partIndex != BodyPartType.ToIndex(BodyPartType.Head) && partIndex != BodyPartType.ToIndex(BodyPartType.Neck) && partIndex != BodyPartType.ToIndex(BodyPartType.Groin)) continue;
                        done = false;
                    }
                }
            }
        } else if (Rand.Next(2) == 0) {
            partIndex = Rand.Next(10) == 0 ? Rand.Next(BodyPartType.ToIndex(BodyPartType.Groin), BodyPartType.ToIndex(BodyPartType.MAX)) : Rand.Next(BodyPartType.ToIndex(BodyPartType.UpperLeg_L), BodyPartType.ToIndex(BodyPartType.MAX));
        } else {
            return false;
        }
        if (zombie.inactive) {
            baseChance += 20;
            baseBiteChance += 20;
            baseLacerationChance += 20;
        }
        float damage = (float)Rand.Next(1000) / 1000.0f;
        damage *= (float)(Rand.Next(10) + 10);
        if (GameServer.server && this.parentChar instanceof IsoPlayer || Core.debug && this.parentChar instanceof IsoPlayer) {
            DebugLog.DetailedInfo.trace("zombie did " + damage + " dmg to " + ((IsoPlayer)this.parentChar).getDisplayName() + " on body part " + BodyPartType.getDisplayName(BodyPartType.FromIndex(partIndex)));
        }
        boolean holeDone = false;
        boolean scratchOrBite = true;
        boolean bl = behind = isBehind || this.parentChar.isFallOnFront();
        if (Rand.Next(100) > baseChance) {
            boolean spikedPart = behind ? this.parentChar.bodyPartIsSpikedBehind(partIndex) : this.parentChar.bodyPartIsSpiked(partIndex);
            zombie.scratch = true;
            this.parentChar.helmetFall(partIndex == BodyPartType.ToIndex(BodyPartType.Neck) || partIndex == BodyPartType.ToIndex(BodyPartType.Head));
            if (Rand.Next(100) > baseLacerationChance) {
                zombie.scratch = false;
                zombie.laceration = true;
            }
            if (Rand.Next(100) > baseBiteChance && !zombie.cantBite()) {
                zombie.scratch = false;
                zombie.laceration = false;
                scratchOrBite = false;
            }
            if (zombie.scratch) {
                defense = this.parentChar.getBodyPartClothingDefense(partIndex, false, false);
                zombie.parameterZombieState.setState(ParameterZombieState.State.AttackScratch);
                if (this.getHealth() > 0.0f) {
                    this.parentChar.getEmitter().playSoundImpl("ZombieScratch", null);
                }
                if (this.getHealth() > 0.0f && spikedPart) {
                    if (Rand.NextBool(2)) {
                        this.parentChar.addBlood(BloodBodyPartType.FromIndex(partIndex), true, false, false);
                        zombie.spikePart(BodyPartType.Hand_L);
                    } else {
                        this.parentChar.addBlood(BloodBodyPartType.FromIndex(partIndex), true, false, false);
                        zombie.spikePart(BodyPartType.Hand_R);
                    }
                }
                if ((float)Rand.Next(100) < defense) {
                    this.parentChar.addHoleFromZombieAttacks(BloodBodyPartType.FromIndex(partIndex), scratchOrBite);
                    return false;
                }
                boolean addedHole = this.parentChar.addHole(BloodBodyPartType.FromIndex(partIndex), true);
                if (addedHole) {
                    this.parentChar.getEmitter().playSoundImpl("ZombieRipClothing", null);
                }
                holeDone = true;
                painType = 1;
                IsoGameCharacter isoGameCharacter2 = this.parentChar;
                if (isoGameCharacter2 instanceof IsoPlayer) {
                    IsoPlayer isoPlayer = (IsoPlayer)isoGameCharacter2;
                    DebugLog.DetailedInfo.trace("zombie scratched %s in body location %s", new Object[]{isoPlayer.getUsername(), BloodBodyPartType.FromIndex(partIndex)});
                    isoPlayer.playerVoiceSound("PainFromScratch");
                    if (GameClient.client) {
                        GameClient.sendZombieHit(zombie, isoPlayer, true, hitReaction);
                        return true;
                    }
                }
                this.AddDamage(partIndex, damage);
                this.SetScratched(partIndex, true);
                this.parentChar.addBlood(BloodBodyPartType.FromIndex(partIndex), true, false, true);
            } else if (zombie.laceration) {
                defense = this.parentChar.getBodyPartClothingDefense(partIndex, false, false);
                zombie.parameterZombieState.setState(ParameterZombieState.State.AttackLacerate);
                if (this.getHealth() > 0.0f) {
                    this.parentChar.getEmitter().playSoundImpl("ZombieScratch", null);
                }
                if (this.getHealth() > 0.0f && spikedPart) {
                    if (Rand.NextBool(2)) {
                        this.parentChar.addBlood(BloodBodyPartType.FromIndex(partIndex), true, false, false);
                        zombie.spikePart(BodyPartType.Hand_L);
                    } else {
                        this.parentChar.addBlood(BloodBodyPartType.FromIndex(partIndex), true, false, false);
                        zombie.spikePart(BodyPartType.Hand_R);
                    }
                }
                if ((float)Rand.Next(100) < defense) {
                    this.parentChar.addHoleFromZombieAttacks(BloodBodyPartType.FromIndex(partIndex), scratchOrBite);
                    return false;
                }
                boolean addedHole = this.parentChar.addHole(BloodBodyPartType.FromIndex(partIndex), true);
                if (addedHole) {
                    this.parentChar.getEmitter().playSoundImpl("ZombieRipClothing", null);
                }
                holeDone = true;
                painType = 1;
                IsoGameCharacter isoGameCharacter3 = this.parentChar;
                if (isoGameCharacter3 instanceof IsoPlayer) {
                    IsoPlayer isoPlayer = (IsoPlayer)isoGameCharacter3;
                    DebugLog.DetailedInfo.trace("zombie laceration %s in body location %s", new Object[]{isoPlayer.getUsername(), BloodBodyPartType.FromIndex(partIndex)});
                    isoPlayer.playerVoiceSound("PainFromLacerate");
                    if (GameClient.client) {
                        GameClient.sendZombieHit(zombie, isoPlayer, true, hitReaction);
                        return true;
                    }
                }
                this.AddDamage(partIndex, damage);
                this.SetCut(partIndex, true);
                this.parentChar.addBlood(BloodBodyPartType.FromIndex(partIndex), true, false, true);
            } else {
                defense = this.parentChar.getBodyPartClothingDefense(partIndex, true, false);
                zombie.parameterZombieState.setState(ParameterZombieState.State.AttackBite);
                if (this.getHealth() > 0.0f) {
                    String soundName = zombie.getBiteSoundName();
                    if (partIndex == BodyPartType.ToIndex(BodyPartType.Neck)) {
                        soundName = "NeckBite";
                    }
                    this.parentChar.getEmitter().playSoundImpl(soundName, null);
                }
                if ((float)Rand.Next(100) < defense) {
                    this.parentChar.addHoleFromZombieAttacks(BloodBodyPartType.FromIndex(partIndex), scratchOrBite);
                    if (spikedPart) {
                        this.parentChar.addBlood(BloodBodyPartType.FromIndex(partIndex), false, true, false);
                        zombie.spikePart(BodyPartType.Head);
                    }
                    return false;
                }
                boolean addedHole = this.parentChar.addHole(BloodBodyPartType.FromIndex(partIndex), true);
                if (addedHole) {
                    this.parentChar.getEmitter().playSoundImpl("ZombieRipClothing", null);
                }
                holeDone = true;
                painType = 2;
                IsoGameCharacter isoGameCharacter4 = this.parentChar;
                if (isoGameCharacter4 instanceof IsoPlayer) {
                    IsoPlayer isoPlayer = (IsoPlayer)isoGameCharacter4;
                    DebugLog.DetailedInfo.trace("zombie bite %s in body location %s", new Object[]{isoPlayer.getUsername(), BloodBodyPartType.FromIndex(partIndex)});
                    isoPlayer.playerVoiceSound("PainFromBite");
                    if (GameClient.client) {
                        GameClient.sendZombieHit(zombie, isoPlayer, true, hitReaction);
                        return true;
                    }
                }
                this.AddDamage(partIndex, damage);
                this.SetBitten(partIndex, true);
                if (partIndex == BodyPartType.ToIndex(BodyPartType.Neck)) {
                    this.parentChar.addBlood(BloodBodyPartType.FromIndex(partIndex), false, true, true);
                    this.parentChar.addBlood(BloodBodyPartType.FromIndex(partIndex), false, true, true);
                    this.parentChar.addBlood(BloodBodyPartType.Torso_Upper, false, true, false);
                    this.parentChar.splatBloodFloorBig();
                    this.parentChar.splatBloodFloorBig();
                    this.parentChar.splatBloodFloorBig();
                }
                this.parentChar.addBlood(BloodBodyPartType.FromIndex(partIndex), false, true, true);
                this.parentChar.splatBloodFloorBig();
                this.parentChar.splatBloodFloorBig();
                this.parentChar.splatBloodFloorBig();
                if (spikedPart) {
                    this.parentChar.addBlood(BloodBodyPartType.FromIndex(partIndex), false, true, false);
                    zombie.spikePart(BodyPartType.Head);
                    zombie.Kill(null);
                }
            }
        }
        if (!holeDone) {
            this.parentChar.addHoleFromZombieAttacks(BloodBodyPartType.FromIndex(partIndex), scratchOrBite);
        }
        switch (painType) {
            case 0: {
                this.stats.add(CharacterStat.PAIN, this.getInitialThumpPain() * BodyPartType.getPainModifyer(partIndex));
                break;
            }
            case 1: {
                this.stats.add(CharacterStat.PAIN, this.getInitialScratchPain() * BodyPartType.getPainModifyer(partIndex));
                break;
            }
            case 2: {
                this.stats.add(CharacterStat.PAIN, this.getInitialBitePain() * BodyPartType.getPainModifyer(partIndex));
            }
        }
        if (GameServer.server && (isoGameCharacter = this.parentChar) instanceof IsoPlayer) {
            IsoPlayer isoPlayer = (IsoPlayer)isoGameCharacter;
            isoPlayer.getNetworkCharacterAI().syncDamage();
        }
        return true;
    }

    public boolean doesBodyPartHaveInjury(BodyPartType part) {
        return this.getBodyParts().get(BodyPartType.ToIndex(part)).HasInjury();
    }

    public boolean doBodyPartsHaveInjuries(BodyPartType partA, BodyPartType partB) {
        return this.doesBodyPartHaveInjury(partA) || this.doesBodyPartHaveInjury(partB);
    }

    public boolean isBodyPartBleeding(BodyPartType part) {
        return this.getBodyPart(part).getBleedingTime() > 0.0f;
    }

    public boolean areBodyPartsBleeding(BodyPartType partA, BodyPartType partB) {
        return this.isBodyPartBleeding(partA) || this.isBodyPartBleeding(partB);
    }

    public void DrawUntexturedQuad(int x, int y, int width, int height, float r, float g, float b, float a) {
        SpriteRenderer.instance.renderi(null, x, y, width, height, r, g, b, a, null);
    }

    public float getBodyPartHealth(BodyPartType bodyPart) {
        return this.getBodyParts().get(BodyPartType.ToIndex(bodyPart)).getHealth();
    }

    public float getBodyPartHealth(int bodyPartIndex) {
        return this.getBodyParts().get(bodyPartIndex).getHealth();
    }

    public String getBodyPartName(BodyPartType bodyPart) {
        return BodyPartType.ToString(bodyPart);
    }

    public String getBodyPartName(int bodyPartIndex) {
        return BodyPartType.ToString(BodyPartType.FromIndex(bodyPartIndex));
    }

    public float getHealth() {
        return this.getOverallBodyHealth();
    }

    public float getApparentInfectionLevel() {
        float infectionLevel = Math.max(this.stats.get(CharacterStat.ZOMBIE_FEVER), this.stats.get(CharacterStat.ZOMBIE_INFECTION));
        return Math.max(this.stats.get(CharacterStat.FOOD_SICKNESS), infectionLevel);
    }

    public int getNumPartsBleeding() {
        int bleedingParts = 0;
        for (int i = 0; i < BodyPartType.ToIndex(BodyPartType.MAX); ++i) {
            if (!this.getBodyParts().get(i).bleeding()) continue;
            ++bleedingParts;
        }
        return bleedingParts;
    }

    public boolean isNeckBleeding() {
        return this.getBodyPart(BodyPartType.Neck).bleeding();
    }

    public int getNumPartsScratched() {
        int scratchedParts = 0;
        for (int i = 0; i < BodyPartType.ToIndex(BodyPartType.MAX); ++i) {
            if (!this.getBodyParts().get(i).scratched()) continue;
            ++scratchedParts;
        }
        return scratchedParts;
    }

    public int getNumPartsBitten() {
        int bittenParts = 0;
        for (int i = 0; i < BodyPartType.ToIndex(BodyPartType.MAX); ++i) {
            if (!this.getBodyParts().get(i).bitten()) continue;
            ++bittenParts;
        }
        return bittenParts;
    }

    public boolean HasInjury() {
        for (int i = 0; i < BodyPartType.ToIndex(BodyPartType.MAX); ++i) {
            if (!this.getBodyParts().get(i).HasInjury()) continue;
            return true;
        }
        return false;
    }

    public boolean IsBandaged(BodyPartType bodyPart) {
        return this.getBodyParts().get(BodyPartType.ToIndex(bodyPart)).bandaged();
    }

    public boolean IsDeepWounded(BodyPartType bodyPart) {
        return this.getBodyParts().get(BodyPartType.ToIndex(bodyPart)).deepWounded();
    }

    public boolean IsBandaged(int bodyPartIndex) {
        return this.getBodyParts().get(bodyPartIndex).bandaged();
    }

    public boolean IsBitten(BodyPartType bodyPart) {
        return this.getBodyParts().get(BodyPartType.ToIndex(bodyPart)).bitten();
    }

    public boolean IsBitten(int bodyPartIndex) {
        return this.getBodyParts().get(bodyPartIndex).bitten();
    }

    public boolean IsBleeding(BodyPartType bodyPart) {
        return this.getBodyParts().get(BodyPartType.ToIndex(bodyPart)).bleeding();
    }

    public boolean IsBleeding(int bodyPartIndex) {
        return this.getBodyParts().get(bodyPartIndex).bleeding();
    }

    public boolean IsBleedingStemmed(BodyPartType bodyPart) {
        return this.getBodyParts().get(BodyPartType.ToIndex(bodyPart)).IsBleedingStemmed();
    }

    public boolean IsBleedingStemmed(int bodyPartIndex) {
        return this.getBodyParts().get(bodyPartIndex).IsBleedingStemmed();
    }

    public boolean IsCauterized(BodyPartType bodyPart) {
        return this.getBodyParts().get(BodyPartType.ToIndex(bodyPart)).IsCauterized();
    }

    public boolean IsCauterized(int bodyPartIndex) {
        return this.getBodyParts().get(bodyPartIndex).IsCauterized();
    }

    public boolean IsInfected() {
        return this.isInfected;
    }

    public boolean IsInfected(BodyPartType bodyPart) {
        return this.getBodyParts().get(BodyPartType.ToIndex(bodyPart)).IsInfected();
    }

    public boolean IsInfected(int bodyPartIndex) {
        return this.getBodyParts().get(bodyPartIndex).IsInfected();
    }

    public boolean IsFakeInfected(int bodyPartIndex) {
        return this.getBodyParts().get(bodyPartIndex).IsFakeInfected();
    }

    public void DisableFakeInfection(int bodyPartIndex) {
        this.getBodyParts().get(bodyPartIndex).DisableFakeInfection();
    }

    public boolean IsScratched(BodyPartType bodyPart) {
        return this.getBodyParts().get(BodyPartType.ToIndex(bodyPart)).scratched();
    }

    public boolean IsCut(BodyPartType bodyPart) {
        return this.getBodyParts().get(BodyPartType.ToIndex(bodyPart)).getCutTime() > 0.0f;
    }

    public boolean IsScratched(int bodyPartIndex) {
        return this.getBodyParts().get(bodyPartIndex).scratched();
    }

    public boolean IsStitched(BodyPartType bodyPart) {
        return this.getBodyParts().get(BodyPartType.ToIndex(bodyPart)).stitched();
    }

    public boolean IsStitched(int bodyPartIndex) {
        return this.getBodyParts().get(bodyPartIndex).stitched();
    }

    public boolean IsWounded(BodyPartType bodyPart) {
        return this.getBodyParts().get(BodyPartType.ToIndex(bodyPart)).deepWounded();
    }

    public boolean IsWounded(int bodyPartIndex) {
        return this.getBodyParts().get(bodyPartIndex).deepWounded();
    }

    public void RestoreToFullHealth() {
        for (int i = 0; i < BodyPartType.ToIndex(BodyPartType.MAX); ++i) {
            this.getBodyParts().get(i).RestoreToFullHealth();
        }
        if (this.parentChar != null && this.parentChar.getStats() != null) {
            this.stats.resetStats();
        }
        if (this.parentChar != null) {
            this.parentChar.setCorpseSicknessRate(0.0f);
        }
        this.setInfected(false);
        this.setIsFakeInfected(false);
        this.setOverallBodyHealth(100.0f);
        this.setCatchACold(0.0f);
        this.setHasACold(false);
        this.setColdStrength(0.0f);
        this.setSneezeCoughActive(0);
        this.setSneezeCoughTime(0);
        this.setInfectionTime(-1.0f);
        this.setInfectionMortalityDuration(-1.0f);
        if (this.thermoregulator != null) {
            this.thermoregulator.reset();
        }
        MusicIntensityConfig.getInstance().restoreToFullHealth(this.parentChar);
    }

    public void SetBandaged(int bodyPartIndex, boolean bandaged, float bandageLife, boolean isAlcoholic, String bandageType) {
        this.getBodyParts().get(bodyPartIndex).setBandaged(bandaged, bandageLife, isAlcoholic, bandageType);
    }

    public void SetBitten(BodyPartType bodyPart, boolean bitten) {
        this.getBodyParts().get(BodyPartType.ToIndex(bodyPart)).SetBitten(bitten);
    }

    public void SetBitten(int bodyPartIndex, boolean bitten) {
        this.getBodyParts().get(bodyPartIndex).SetBitten(bitten);
    }

    public void SetBitten(int bodyPartIndex, boolean bitten, boolean infected) {
        this.getBodyParts().get(bodyPartIndex).SetBitten(bitten, infected);
    }

    public void SetBleeding(BodyPartType bodyPart, boolean bleeding) {
        this.getBodyParts().get(BodyPartType.ToIndex(bodyPart)).setBleeding(bleeding);
    }

    public void SetBleeding(int bodyPartIndex, boolean bleeding) {
        this.getBodyParts().get(bodyPartIndex).setBleeding(bleeding);
    }

    public void SetBleedingStemmed(BodyPartType bodyPart, boolean bleedingStemmed) {
        this.getBodyParts().get(BodyPartType.ToIndex(bodyPart)).SetBleedingStemmed(bleedingStemmed);
    }

    public void SetBleedingStemmed(int bodyPartIndex, boolean bleedingStemmed) {
        this.getBodyParts().get(bodyPartIndex).SetBleedingStemmed(bleedingStemmed);
    }

    public void SetCauterized(BodyPartType bodyPart, boolean cauterized) {
        this.getBodyParts().get(BodyPartType.ToIndex(bodyPart)).SetCauterized(cauterized);
    }

    public void SetCauterized(int bodyPartIndex, boolean cauterized) {
        this.getBodyParts().get(bodyPartIndex).SetCauterized(cauterized);
    }

    public BodyPart setScratchedWindow() {
        if (GameClient.client) {
            return null;
        }
        int bodyPart = Rand.Next(BodyPartType.ToIndex(BodyPartType.Hand_L), BodyPartType.ToIndex(BodyPartType.ForeArm_R) + 1);
        this.getBodyPart(BodyPartType.FromIndex(bodyPart)).AddDamage(10.0f);
        this.getBodyPart(BodyPartType.FromIndex(bodyPart)).SetScratchedWindow(true);
        return this.getBodyPart(BodyPartType.FromIndex(bodyPart));
    }

    public void SetScratched(BodyPartType bodyPart, boolean scratched) {
        this.getBodyParts().get(BodyPartType.ToIndex(bodyPart)).setScratched(scratched, false);
    }

    public void SetScratched(int bodyPartIndex, boolean scratched) {
        this.getBodyParts().get(bodyPartIndex).setScratched(scratched, false);
    }

    public void SetScratchedFromWeapon(int bodyPartIndex, boolean scratched) {
        this.getBodyParts().get(bodyPartIndex).SetScratchedWeapon(scratched);
    }

    public void SetCut(int bodyPartIndex, boolean cut) {
        this.getBodyParts().get(bodyPartIndex).setCut(cut, false);
    }

    public void SetWounded(BodyPartType bodyPart, boolean wounded) {
        this.getBodyParts().get(BodyPartType.ToIndex(bodyPart)).setDeepWounded(wounded);
    }

    public void SetWounded(int bodyPartIndex, boolean wounded) {
        this.getBodyParts().get(bodyPartIndex).setDeepWounded(wounded);
    }

    public void ShowDebugInfo() {
        if (this.getDamageModCount() > 0) {
            this.setDamageModCount(this.getDamageModCount() - 1);
        }
    }

    public void UpdateBoredom() {
        if (this.parentChar instanceof IsoSurvivor) {
            return;
        }
        if (this.parentChar instanceof IsoPlayer && this.parentChar.asleep) {
            return;
        }
        if (this.parentChar.getCurrentSquare().isInARoom() || this.parentChar.getIdleSquareTime() >= 1800.0f) {
            if (this.parentChar.isCurrentlyIdle()) {
                this.stats.add(CharacterStat.BOREDOM, (float)(ZomboidGlobals.boredomIncreaseRate * (double)this.stats.get(CharacterStat.IDLENESS) * (double)GameTime.instance.getMultiplier()));
            } else {
                this.stats.add(CharacterStat.BOREDOM, (float)(ZomboidGlobals.boredomIncreaseRate / 10.0 * (double)this.stats.get(CharacterStat.IDLENESS) * (double)GameTime.instance.getMultiplier()));
            }
            if (this.parentChar.IsSpeaking() && !this.parentChar.callOut) {
                this.stats.remove(CharacterStat.BOREDOM, (float)(ZomboidGlobals.boredomDecreaseRate * (double)GameTime.instance.getMultiplier()));
            }
            if (this.parentChar.getNumSurvivorsInVicinity() > 0) {
                this.stats.remove(CharacterStat.BOREDOM, (float)(ZomboidGlobals.boredomDecreaseRate * (double)0.1f * (double)GameTime.instance.getMultiplier()));
            }
            if (this.parentChar.isCurrentlyBusy() && this.stats.get(CharacterStat.IDLENESS) < 0.1f) {
                this.stats.remove(CharacterStat.BOREDOM, (float)(ZomboidGlobals.boredomDecreaseRate * 0.5 * (double)GameTime.instance.getMultiplier()));
            }
        } else if (this.parentChar.getVehicle() != null) {
            float speed = this.parentChar.getVehicle().getCurrentSpeedKmHour();
            if (Math.abs(speed) <= 0.1f) {
                if (this.parentChar.isReading()) {
                    this.stats.add(CharacterStat.BOREDOM, (float)(ZomboidGlobals.boredomIncreaseRate / 5.0 * (double)GameTime.instance.getMultiplier()));
                } else {
                    this.stats.add(CharacterStat.BOREDOM, (float)(ZomboidGlobals.boredomIncreaseRate * (double)GameTime.instance.getMultiplier()));
                }
            } else {
                this.stats.remove(CharacterStat.BOREDOM, (float)(ZomboidGlobals.boredomDecreaseRate * 0.5 * (double)GameTime.instance.getMultiplier()));
            }
        } else {
            this.stats.remove(CharacterStat.BOREDOM, (float)(ZomboidGlobals.boredomDecreaseRate * (double)0.1f * (double)GameTime.instance.getMultiplier()));
        }
        if (this.stats.get(CharacterStat.INTOXICATION) > 20.0f) {
            this.stats.remove(CharacterStat.BOREDOM, (float)(ZomboidGlobals.boredomDecreaseRate * 2.0 * (double)GameTime.instance.getMultiplier()));
        }
        if (this.stats.get(CharacterStat.PANIC) > 5.0f) {
            this.stats.reset(CharacterStat.BOREDOM);
        }
        if (this.parentChar.getMoodles().getMoodleLevel(MoodleType.BORED) > 1 && !this.parentChar.isReading()) {
            this.stats.add(CharacterStat.UNHAPPINESS, (float)(ZomboidGlobals.unhappinessIncrease * (double)this.parentChar.getMoodles().getMoodleLevel(MoodleType.BORED) * (double)GameTime.instance.getMultiplier()));
        }
        if (this.parentChar.getMoodles().getMoodleLevel(MoodleType.STRESS) > 1 && !this.parentChar.isReading()) {
            this.stats.add(CharacterStat.UNHAPPINESS, (float)(ZomboidGlobals.unhappinessIncrease / 2.0 * (double)this.parentChar.getMoodles().getMoodleLevel(MoodleType.STRESS) * (double)GameTime.instance.getMultiplier()));
        }
        if (this.parentChar.hasTrait(CharacterTrait.SMOKER)) {
            this.parentChar.setTimeSinceLastSmoke(this.parentChar.getTimeSinceLastSmoke() + 1.0E-4f * GameTime.instance.getMultiplier());
            if (this.parentChar.getTimeSinceLastSmoke() > 1.0f) {
                double lastTimeSmoke = (float)PZMath.fastfloor(this.parentChar.getTimeSinceLastSmoke() / 10.0f) + 1.0f;
                if (lastTimeSmoke > 10.0) {
                    lastTimeSmoke = 10.0;
                }
                this.stats.add(CharacterStat.NICOTINE_WITHDRAWAL, (float)(ZomboidGlobals.stressFromBiteOrScratch / 8.0 * lastTimeSmoke * (double)GameTime.instance.getMultiplier()));
            }
        }
    }

    public void UpdateStrength() {
        IsoGameCharacter isoGameCharacter;
        int numStrengthReducers = 0;
        if (this.parentChar.getMoodles().getMoodleLevel(MoodleType.HUNGRY) == 2) {
            ++numStrengthReducers;
        }
        if (this.parentChar.getMoodles().getMoodleLevel(MoodleType.HUNGRY) == 3) {
            numStrengthReducers += 2;
        }
        if (this.parentChar.getMoodles().getMoodleLevel(MoodleType.HUNGRY) == 4) {
            numStrengthReducers += 2;
        }
        if (this.parentChar.getMoodles().getMoodleLevel(MoodleType.THIRST) == 2) {
            ++numStrengthReducers;
        }
        if (this.parentChar.getMoodles().getMoodleLevel(MoodleType.THIRST) == 3) {
            numStrengthReducers += 2;
        }
        if (this.parentChar.getMoodles().getMoodleLevel(MoodleType.THIRST) == 4) {
            numStrengthReducers += 2;
        }
        if (this.parentChar.getMoodles().getMoodleLevel(MoodleType.SICK) == 2) {
            ++numStrengthReducers;
        }
        if (this.parentChar.getMoodles().getMoodleLevel(MoodleType.SICK) == 3) {
            numStrengthReducers += 2;
        }
        if (this.parentChar.getMoodles().getMoodleLevel(MoodleType.SICK) == 4) {
            numStrengthReducers += 3;
        }
        if (this.parentChar.getMoodles().getMoodleLevel(MoodleType.BLEEDING) == 2) {
            ++numStrengthReducers;
        }
        if (this.parentChar.getMoodles().getMoodleLevel(MoodleType.BLEEDING) == 3) {
            ++numStrengthReducers;
        }
        if (this.parentChar.getMoodles().getMoodleLevel(MoodleType.BLEEDING) == 4) {
            ++numStrengthReducers;
        }
        if (this.parentChar.getMoodles().getMoodleLevel(MoodleType.INJURED) == 2) {
            ++numStrengthReducers;
        }
        if (this.parentChar.getMoodles().getMoodleLevel(MoodleType.INJURED) == 3) {
            numStrengthReducers += 2;
        }
        if (this.parentChar.getMoodles().getMoodleLevel(MoodleType.INJURED) == 4) {
            numStrengthReducers += 3;
        }
        this.parentChar.setMaxWeight((int)((float)this.parentChar.getMaxWeightBase() * this.parentChar.getWeightMod()) - numStrengthReducers);
        if (this.parentChar.getMaxWeight() < 0) {
            this.parentChar.setMaxWeight(0);
        }
        if ((isoGameCharacter = this.parentChar) instanceof IsoPlayer) {
            IsoPlayer isoPlayer = (IsoPlayer)isoGameCharacter;
            this.parentChar.setMaxWeight((int)((float)this.parentChar.getMaxWeight() * isoPlayer.getMaxWeightDelta()));
        }
    }

    public float pickMortalityDuration() {
        float del = 1.0f;
        if (this.parentChar.hasTrait(CharacterTrait.RESILIENT)) {
            del = 1.25f;
        }
        if (this.parentChar.hasTrait(CharacterTrait.PRONE_TO_ILLNESS)) {
            del = 0.75f;
        }
        return switch (SandboxOptions.instance.lore.mortality.getValue()) {
            case 1 -> 0.0f;
            case 2 -> Rand.Next(0.0f, 30.0f) / 3600.0f * del;
            case 3 -> Rand.Next(0.5f, 1.0f) / 60.0f * del;
            case 4 -> Rand.Next(3.0f, 12.0f) * del;
            case 5 -> Rand.Next(2.0f, 3.0f) * 24.0f * del;
            case 6 -> Rand.Next(1.0f, 2.0f) * 7.0f * 24.0f * del;
            case 7 -> -1.0f;
            default -> -1.0f;
        };
    }

    public void Update() {
        int i;
        IsoPlayer player;
        if (this.parentChar instanceof IsoZombie || this.parentChar.isAnimal()) {
            return;
        }
        if (GameClient.client && (player = Type.tryCastTo(this.parentChar, IsoPlayer.class)) != null && player.isAlive()) {
            if (!player.isLocalPlayer()) {
                this.RestoreToFullHealth();
            }
            return;
        }
        if (this.parentChar.isGodMod()) {
            this.RestoreToFullHealth();
            ((IsoPlayer)this.parentChar).bleedingLevel = 0;
            return;
        }
        float lastPain = this.stats.get(CharacterStat.PAIN);
        int n = this.getNumPartsBleeding() * 2;
        n += this.getNumPartsScratched();
        if (this.getHealth() >= 60.0f && (n += this.getNumPartsBitten() * 6) <= 3) {
            n = 0;
        }
        ((IsoPlayer)this.parentChar).bleedingLevel = (byte)n;
        if (n > 0) {
            float bleedChance = 1.0f / (float)n * 200.0f * GameTime.instance.getInvMultiplier();
            if ((float)Rand.Next((int)bleedChance) < bleedChance * 0.3f) {
                this.parentChar.splatBloodFloor();
            }
            if (Rand.Next((int)bleedChance) == 0) {
                this.parentChar.splatBloodFloor();
            }
        }
        if (this.thermoregulator != null) {
            this.thermoregulator.update();
        }
        this.UpdateDraggingCorpse();
        this.UpdateWetness();
        this.UpdateCold();
        this.UpdateBoredom();
        this.UpdateStrength();
        this.UpdatePanicState();
        this.UpdateTemperatureState();
        this.UpdateDiscomfort();
        this.UpdateIllness();
        if (this.getOverallBodyHealth() == 0.0f) {
            return;
        }
        if (!this.isInfected()) {
            for (int i2 = 0; i2 < BodyPartType.ToIndex(BodyPartType.MAX); ++i2) {
                if (!this.IsInfected(i2)) continue;
                this.setInfected(true);
                if (!this.IsFakeInfected(i2)) continue;
                this.DisableFakeInfection(i2);
                this.stats.set(CharacterStat.ZOMBIE_INFECTION, this.stats.get(CharacterStat.ZOMBIE_FEVER));
                this.stats.reset(CharacterStat.ZOMBIE_FEVER);
                this.setIsFakeInfected(false);
                this.setReduceFakeInfection(false);
            }
            if (this.isInfected() && this.getInfectionTime() < 0.0f && SandboxOptions.instance.lore.mortality.getValue() != 7) {
                this.setInfectionTime(this.getCurrentTimeForInfection());
                this.setInfectionMortalityDuration(this.pickMortalityDuration());
            }
        }
        if (!this.isInfected() && !this.isIsFakeInfected()) {
            for (int i3 = 0; i3 < BodyPartType.ToIndex(BodyPartType.MAX); ++i3) {
                if (!this.IsFakeInfected(i3)) continue;
                this.setIsFakeInfected(true);
                break;
            }
        }
        if (this.isIsFakeInfected() && !this.isReduceFakeInfection() && this.parentChar.getReduceInfectionPower() == 0.0f) {
            this.stats.add(CharacterStat.ZOMBIE_FEVER, this.getInfectionGrowthRate() * GameTime.instance.getMultiplier());
            if (this.stats.isAtMaximum(CharacterStat.ZOMBIE_FEVER)) {
                this.setReduceFakeInfection(true);
            }
        }
        this.stats.remove(CharacterStat.INTOXICATION, this.getDrunkReductionValue() * GameTime.instance.getMultiplier());
        float healthToAdd = 0.0f;
        if (this.getHealthFromFoodTimer() > 0.0f) {
            healthToAdd += this.getHealthFromFood() * GameTime.instance.getMultiplier();
            this.setHealthFromFoodTimer(this.getHealthFromFoodTimer() - 1.0f * GameTime.instance.getMultiplier());
        }
        int reduced = 0;
        if (this.parentChar.getMoodles().getMoodleLevel(MoodleType.HUNGRY) == 2 || this.parentChar.getMoodles().getMoodleLevel(MoodleType.SICK) == 2 || this.parentChar.getMoodles().getMoodleLevel(MoodleType.THIRST) == 2) {
            reduced = 1;
        }
        if (this.parentChar.getMoodles().getMoodleLevel(MoodleType.HUNGRY) == 3 || this.parentChar.getMoodles().getMoodleLevel(MoodleType.SICK) == 3 || this.parentChar.getMoodles().getMoodleLevel(MoodleType.THIRST) == 3) {
            reduced = 2;
        }
        if (this.parentChar.getMoodles().getMoodleLevel(MoodleType.HUNGRY) == 4 || this.parentChar.getMoodles().getMoodleLevel(MoodleType.THIRST) == 4) {
            reduced = 3;
        }
        if (this.parentChar.isAsleep()) {
            reduced = -1;
        }
        switch (reduced) {
            case 0: {
                healthToAdd += this.getStandardHealthAddition() * GameTime.instance.getMultiplier();
                break;
            }
            case 1: {
                healthToAdd += this.getReducedHealthAddition() * GameTime.instance.getMultiplier();
                break;
            }
            case 2: {
                healthToAdd += this.getSeverlyReducedHealthAddition() * GameTime.instance.getMultiplier();
                break;
            }
            case 3: {
                healthToAdd += 0.0f;
            }
        }
        if (this.parentChar.isAsleep()) {
            healthToAdd = GameClient.client ? (healthToAdd += 15.0f * GameTime.instance.getGameWorldSecondsSinceLastUpdate() / 3600.0f) : (healthToAdd += this.getSleepingHealthAddition() * GameTime.instance.getMultiplier());
            if (this.parentChar.getMoodles().getMoodleLevel(MoodleType.HUNGRY) == 4 || this.parentChar.getMoodles().getMoodleLevel(MoodleType.THIRST) == 4) {
                healthToAdd = 0.0f;
            }
        }
        this.AddGeneralHealth(healthToAdd);
        float reductionAmmount = 0.0f;
        float poisonDamage = 0.0f;
        float hungryDamage = 0.0f;
        float sickDamage = 0.0f;
        float bleedingDamage = 0.0f;
        float thirstDamage = 0.0f;
        float heavyLoadDamage = 0.0f;
        float poison = this.stats.get(CharacterStat.POISON);
        if (poison > 0.0f) {
            if (poison > 10.0f && this.parentChar.getMoodles().getMoodleLevel(MoodleType.SICK) >= 1) {
                poisonDamage = 0.0035f * Math.min(poison / 10.0f, 3.0f) * GameTime.instance.getMultiplier();
                reductionAmmount += poisonDamage;
            }
            float decreaseWithWellFed = 0.0f;
            if (this.parentChar.getMoodles().getMoodleLevel(MoodleType.FOOD_EATEN) > 0) {
                decreaseWithWellFed = 1.5E-4f * (float)this.parentChar.getMoodles().getMoodleLevel(MoodleType.FOOD_EATEN);
            }
            this.stats.remove(CharacterStat.POISON, (float)((double)decreaseWithWellFed + ZomboidGlobals.poisonLevelDecrease * (double)GameTime.instance.getMultiplier()));
            this.stats.add(CharacterStat.FOOD_SICKNESS, this.getInfectionGrowthRate() * (2.0f + (float)Math.round(this.stats.get(CharacterStat.POISON) / 10.0f)) * GameTime.instance.getMultiplier());
        }
        if (this.parentChar.getMoodles().getMoodleLevel(MoodleType.HUNGRY) == 4) {
            hungryDamage = this.getHealthReductionFromSevereBadMoodles() / 50.0f * GameTime.instance.getMultiplier();
            reductionAmmount += hungryDamage;
        }
        if (this.parentChar.getMoodles().getMoodleLevel(MoodleType.SICK) == 4) {
            if (this.stats.get(CharacterStat.FOOD_SICKNESS) > this.stats.get(CharacterStat.ZOMBIE_INFECTION)) {
                sickDamage = this.getHealthReductionFromSevereBadMoodles() * GameTime.instance.getMultiplier();
                reductionAmmount += sickDamage;
            } else if (SandboxOptions.instance.woundInfectionFactor.getValue() > 0.0 && this.getGeneralWoundInfectionLevel() > this.stats.get(CharacterStat.ZOMBIE_INFECTION)) {
                sickDamage = this.getHealthReductionFromSevereBadMoodles() * GameTime.instance.getMultiplier();
                reductionAmmount += sickDamage;
            }
        }
        if (this.parentChar.getMoodles().getMoodleLevel(MoodleType.BLEEDING) == 4) {
            bleedingDamage = this.getHealthReductionFromSevereBadMoodles() * GameTime.instance.getMultiplier();
            reductionAmmount += bleedingDamage;
        }
        if (this.parentChar.getMoodles().getMoodleLevel(MoodleType.THIRST) == 4) {
            thirstDamage = this.getHealthReductionFromSevereBadMoodles() / 10.0f * GameTime.instance.getMultiplier();
            reductionAmmount += thirstDamage;
        }
        if (this.parentChar.getMoodles().getMoodleLevel(MoodleType.HEAVY_LOAD) > 2 && this.parentChar.getVehicle() == null && !this.parentChar.isAsleep() && !this.parentChar.isSitOnGround() && !this.parentChar.isSittingOnFurniture() && this.getThermoregulator().getMetabolicTarget() != Metabolics.SeatedResting.getMet() && this.getHealth() > 75.0f && Rand.Next(Rand.AdjustForFramerate(10)) == 0) {
            heavyLoadDamage = this.getHealthReductionFromSevereBadMoodles() / ((float)(5 - this.parentChar.getMoodles().getMoodleLevel(MoodleType.HEAVY_LOAD)) / 10.0f) * GameTime.instance.getMultiplier();
            reductionAmmount += heavyLoadDamage;
            this.parentChar.addBackMuscleStrain(heavyLoadDamage / 2.0f);
        }
        this.ReduceGeneralHealth(reductionAmmount);
        if (poisonDamage > 0.0f) {
            LuaEventManager.triggerEvent("OnPlayerGetDamage", this.parentChar, "POISON", Float.valueOf(poisonDamage));
        }
        if (hungryDamage > 0.0f) {
            LuaEventManager.triggerEvent("OnPlayerGetDamage", this.parentChar, "HUNGRY", Float.valueOf(hungryDamage));
        }
        if (sickDamage > 0.0f) {
            LuaEventManager.triggerEvent("OnPlayerGetDamage", this.parentChar, "SICK", Float.valueOf(sickDamage));
        }
        if (bleedingDamage > 0.0f) {
            LuaEventManager.triggerEvent("OnPlayerGetDamage", this.parentChar, "BLEEDING", Float.valueOf(bleedingDamage));
        }
        if (thirstDamage > 0.0f) {
            LuaEventManager.triggerEvent("OnPlayerGetDamage", this.parentChar, "THIRST", Float.valueOf(thirstDamage));
        }
        if (heavyLoadDamage > 0.0f) {
            LuaEventManager.triggerEvent("OnPlayerGetDamage", this.parentChar, "HEAVYLOAD", Float.valueOf(heavyLoadDamage));
        }
        if (this.parentChar.getPainEffect() > 0.0f) {
            this.stats.remove(CharacterStat.PAIN, 0.023333333f * GameTime.getInstance().getThirtyFPSMultiplier());
            this.parentChar.setPainEffect(this.parentChar.getPainEffect() - GameTime.getInstance().getThirtyFPSMultiplier());
        } else {
            this.parentChar.setPainDelta(0.0f);
            float pain = 0.0f;
            for (int i4 = 0; i4 < BodyPartType.ToIndex(BodyPartType.MAX); ++i4) {
                pain += this.getBodyParts().get(i4).getPain() * BodyPartType.getPainModifyer(i4);
            }
            if ((pain -= this.getPainReduction()) > this.stats.get(CharacterStat.PAIN)) {
                this.stats.add(CharacterStat.PAIN, (pain - this.stats.get(CharacterStat.PAIN)) / 500.0f);
            } else {
                this.stats.set(CharacterStat.PAIN, pain);
            }
        }
        this.setPainReduction(this.getPainReduction() - 0.005f * GameTime.getInstance().getMultiplier());
        if (this.getPainReduction() < 0.0f) {
            this.setPainReduction(0.0f);
        }
        if (this.isInfected()) {
            int mortality = SandboxOptions.instance.lore.mortality.getValue();
            if (mortality == 1) {
                this.ReduceGeneralHealth(110.0f);
                LuaEventManager.triggerEvent("OnPlayerGetDamage", this.parentChar, "INFECTION", 110);
                this.stats.set(CharacterStat.ZOMBIE_INFECTION, CharacterStat.ZOMBIE_INFECTION.getMaximumValue());
            } else if (mortality != 7) {
                float worldAgeHours = this.getCurrentTimeForInfection();
                if (this.infectionMortalityDuration < 0.0f) {
                    this.infectionMortalityDuration = this.pickMortalityDuration();
                }
                if (this.infectionTime < 0.0f) {
                    this.infectionTime = worldAgeHours;
                }
                if (this.infectionTime > worldAgeHours) {
                    this.infectionTime = worldAgeHours;
                }
                float percentMortality = Math.min((worldAgeHours - this.infectionTime) / this.infectionMortalityDuration, 1.0f);
                this.stats.set(CharacterStat.ZOMBIE_INFECTION, percentMortality * 100.0f);
                if (percentMortality == 1.0f) {
                    this.ReduceGeneralHealth(110.0f);
                    LuaEventManager.triggerEvent("OnPlayerGetDamage", this.parentChar, "INFECTION", 110);
                } else {
                    percentMortality *= percentMortality;
                    percentMortality *= percentMortality;
                    float maxHealth = (1.0f - percentMortality) * 100.0f;
                    float excessHealth = this.getOverallBodyHealth() - maxHealth;
                    if (excessHealth > 0.0f && maxHealth <= 99.0f) {
                        this.ReduceGeneralHealth(excessHealth);
                        LuaEventManager.triggerEvent("OnPlayerGetDamage", this.parentChar, "INFECTION", Float.valueOf(excessHealth));
                    }
                }
            }
        }
        for (i = 0; i < BodyPartType.ToIndex(BodyPartType.MAX); ++i) {
            this.getBodyParts().get(i).DamageUpdate();
        }
        this.calculateOverallHealth();
        if (this.getOverallBodyHealth() <= 0.0f) {
            if (this.isIsOnFire()) {
                this.setBurntToDeath(true);
                for (i = 0; i < BodyPartType.ToIndex(BodyPartType.MAX); ++i) {
                    this.getBodyParts().get(i).SetHealth(Rand.Next(90));
                }
            } else {
                this.setBurntToDeath(false);
            }
        }
        if (this.isReduceFakeInfection() && this.getOverallBodyHealth() > 0.0f) {
            this.stats.remove(CharacterStat.ZOMBIE_FEVER, this.getInfectionGrowthRate() * GameTime.instance.getMultiplier() * 2.0f);
        }
        if (this.parentChar.getReduceInfectionPower() > 0.0f && this.getOverallBodyHealth() > 0.0f) {
            this.stats.remove(CharacterStat.ZOMBIE_FEVER, this.getInfectionGrowthRate() * GameTime.instance.getMultiplier());
            this.parentChar.setReduceInfectionPower(this.parentChar.getReduceInfectionPower() - this.getInfectionGrowthRate() * GameTime.instance.getMultiplier());
            if (this.parentChar.getReduceInfectionPower() < 0.0f) {
                this.parentChar.setReduceInfectionPower(0.0f);
            }
        }
        if (this.stats.get(CharacterStat.ZOMBIE_FEVER) <= 0.0f) {
            for (i = 0; i < BodyPartType.ToIndex(BodyPartType.MAX); ++i) {
                this.getBodyParts().get(i).SetFakeInfected(false);
            }
            this.setIsFakeInfected(false);
            this.stats.reset(CharacterStat.ZOMBIE_FEVER);
            this.setReduceFakeInfection(false);
        }
        if (lastPain == this.stats.get(CharacterStat.PAIN)) {
            this.stats.remove(CharacterStat.PAIN, 0.25f * GameTime.getInstance().getThirtyFPSMultiplier());
        }
    }

    public void calculateOverallHealth() {
        float totalDamage = 0.0f;
        for (int i = 0; i < BodyPartType.ToIndex(BodyPartType.MAX); ++i) {
            BodyPart bodyPart = this.getBodyParts().get(i);
            totalDamage += (100.0f - bodyPart.getHealth()) * BodyPartType.getDamageModifyer(i);
        }
        if ((totalDamage += this.getDamageFromPills()) > 100.0f) {
            totalDamage = 100.0f;
        }
        this.setOverallBodyHealth(100.0f - totalDamage);
    }

    public static float getSicknessFromCorpsesRate(int corpseCount) {
        if (SandboxOptions.instance.decayingCorpseHealthImpact.getValue() == 1) {
            return 0.0f;
        }
        if (corpseCount > 5) {
            float inc = (float)ZomboidGlobals.foodSicknessDecrease * 0.07f;
            switch (SandboxOptions.instance.decayingCorpseHealthImpact.getValue()) {
                case 2: {
                    inc = (float)ZomboidGlobals.foodSicknessDecrease * 0.01f;
                    break;
                }
                case 4: {
                    inc = (float)ZomboidGlobals.foodSicknessDecrease * 0.11f;
                    break;
                }
                case 5: {
                    inc = (float)ZomboidGlobals.foodSicknessDecrease;
                }
            }
            int cap = Math.min(corpseCount - 5, FliesSound.maxCorpseCount - 5);
            return inc * (float)cap;
        }
        return 0.0f;
    }

    private void UpdateIllness() {
        float rate;
        if (SandboxOptions.instance.decayingCorpseHealthImpact.getValue() != 1 && (rate = this.GetBaseCorpseSickness()) > 0.0f) {
            float defense = this.parentChar.getCorpseSicknessDefense(rate, false);
            if (defense > 0.0f) {
                float multiplier = Math.max(0.0f, 1.0f - defense / 100.0f);
                rate *= multiplier;
            }
            if (this.parentChar.hasTrait(CharacterTrait.RESILIENT)) {
                rate *= 0.75f;
            } else if (this.parentChar.hasTrait(CharacterTrait.PRONE_TO_ILLNESS)) {
                rate *= 1.25f;
            }
            if (rate > 0.0f) {
                this.stats.add(CharacterStat.FOOD_SICKNESS, rate * GameTime.getInstance().getMultiplier());
                this.parentChar.setCorpseSicknessRate(rate);
                return;
            }
        }
        this.parentChar.setCorpseSicknessRate(0.0f);
        if (this.stats.isAtMinimum(CharacterStat.POISON) && this.stats.isAboveMinimum(CharacterStat.FOOD_SICKNESS)) {
            this.stats.remove(CharacterStat.FOOD_SICKNESS, (float)ZomboidGlobals.foodSicknessDecrease * GameTime.getInstance().getMultiplier());
        }
    }

    public float GetBaseCorpseSickness() {
        return BodyDamage.getSicknessFromCorpsesRate(CorpseCount.instance.getCorpseCount(this.parentChar));
    }

    private void UpdateTemperatureState() {
        float delta = 0.06f;
        IsoGameCharacter isoGameCharacter = this.parentChar;
        if (isoGameCharacter instanceof IsoPlayer) {
            IsoPlayer isoPlayer = (IsoPlayer)isoGameCharacter;
            if (this.coldDamageStage > 0.0f) {
                float maxHealth = 100.0f - this.coldDamageStage * 100.0f;
                if (maxHealth <= 0.0f) {
                    this.parentChar.setHealth(0.0f);
                    return;
                }
                if (this.overallBodyHealth > maxHealth) {
                    this.ReduceGeneralHealth(this.overallBodyHealth - maxHealth);
                }
            }
            isoPlayer.setMoveSpeed(0.06f);
        }
    }

    private float getDamageFromPills() {
        IsoGameCharacter isoGameCharacter = this.parentChar;
        if (isoGameCharacter instanceof IsoPlayer) {
            IsoPlayer player = (IsoPlayer)isoGameCharacter;
            if (player.getSleepingPillsTaken() == 10) {
                return 40.0f;
            }
            if (player.getSleepingPillsTaken() == 11) {
                return 80.0f;
            }
            if (player.getSleepingPillsTaken() >= 12) {
                return 100.0f;
            }
        }
        return 0.0f;
    }

    public boolean UseBandageOnMostNeededPart() {
        int highestScore = 0;
        BodyPart part = null;
        for (int n = 0; n < this.getBodyParts().size(); ++n) {
            int score = 0;
            if (this.getBodyParts().get(n).bandaged()) continue;
            if (this.getBodyParts().get(n).bleeding()) {
                score += 100;
            }
            if (this.getBodyParts().get(n).scratched()) {
                score += 50;
            }
            if (this.getBodyParts().get(n).bitten()) {
                score += 50;
            }
            if (score <= highestScore) continue;
            highestScore = score;
            part = this.getBodyParts().get(n);
        }
        if (highestScore > 0 && part != null) {
            part.setBandaged(true, 10.0f);
            return true;
        }
        return false;
    }

    public ArrayList<BodyPart> getBodyParts() {
        return this.bodyParts;
    }

    public int getDamageModCount() {
        return this.damageModCount;
    }

    public void setDamageModCount(int damageModCount) {
        this.damageModCount = damageModCount;
    }

    public float getInfectionGrowthRate() {
        return this.infectionGrowthRate;
    }

    public void setInfectionGrowthRate(float infectionGrowthRate) {
        this.infectionGrowthRate = infectionGrowthRate;
    }

    public boolean isInfected() {
        return this.isInfected;
    }

    public void setInfected(boolean infected) {
        this.isInfected = infected;
    }

    public float getInfectionTime() {
        return this.infectionTime;
    }

    public void setInfectionTime(float worldHours) {
        this.infectionTime = worldHours;
    }

    public float getInfectionMortalityDuration() {
        return this.infectionMortalityDuration;
    }

    public void setInfectionMortalityDuration(float worldHours) {
        this.infectionMortalityDuration = worldHours;
    }

    private float getCurrentTimeForInfection() {
        if (this.parentChar instanceof IsoPlayer) {
            return (float)this.parentChar.getHoursSurvived();
        }
        return (float)GameTime.getInstance().getWorldAgeHours();
    }

    @Deprecated
    public boolean isInf() {
        return this.isInfected;
    }

    @Deprecated
    public void setInf(boolean inf) {
        this.isInfected = inf;
    }

    public boolean isIsFakeInfected() {
        return this.isFakeInfected;
    }

    public void setIsFakeInfected(boolean isFakeInfected) {
        this.isFakeInfected = isFakeInfected;
        this.getBodyParts().get(0).SetFakeInfected(isFakeInfected);
    }

    public float getOverallBodyHealth() {
        return this.overallBodyHealth;
    }

    public void setOverallBodyHealth(float overallBodyHealth) {
        this.overallBodyHealth = overallBodyHealth;
    }

    public float getStandardHealthAddition() {
        return this.standardHealthAddition;
    }

    public void setStandardHealthAddition(float standardHealthAddition) {
        this.standardHealthAddition = standardHealthAddition;
    }

    public float getReducedHealthAddition() {
        return this.reducedHealthAddition;
    }

    public void setReducedHealthAddition(float reducedHealthAddition) {
        this.reducedHealthAddition = reducedHealthAddition;
    }

    public float getSeverlyReducedHealthAddition() {
        return this.severlyReducedHealthAddition;
    }

    public void setSeverlyReducedHealthAddition(float severlyReducedHealthAddition) {
        this.severlyReducedHealthAddition = severlyReducedHealthAddition;
    }

    public float getSleepingHealthAddition() {
        return this.sleepingHealthAddition;
    }

    public void setSleepingHealthAddition(float sleepingHealthAddition) {
        this.sleepingHealthAddition = sleepingHealthAddition;
    }

    public float getHealthFromFood() {
        return this.healthFromFood;
    }

    public void setHealthFromFood(float healthFromFood) {
        this.healthFromFood = healthFromFood;
    }

    public float getHealthReductionFromSevereBadMoodles() {
        return this.healthReductionFromSevereBadMoodles;
    }

    public void setHealthReductionFromSevereBadMoodles(float healthReductionFromSevereBadMoodles) {
        this.healthReductionFromSevereBadMoodles = healthReductionFromSevereBadMoodles;
    }

    public int getStandardHealthFromFoodTime() {
        return this.standardHealthFromFoodTime;
    }

    public void setStandardHealthFromFoodTime(int standardHealthFromFoodTime) {
        this.standardHealthFromFoodTime = standardHealthFromFoodTime;
    }

    public float getHealthFromFoodTimer() {
        return this.healthFromFoodTimer;
    }

    public void setHealthFromFoodTimer(float healthFromFoodTimer) {
        this.healthFromFoodTimer = healthFromFoodTimer;
    }

    public float getBoredomDecreaseFromReading() {
        return this.boredomDecreaseFromReading;
    }

    public void setBoredomDecreaseFromReading(float boredomDecreaseFromReading) {
        this.boredomDecreaseFromReading = boredomDecreaseFromReading;
    }

    public float getInitialThumpPain() {
        return this.initialThumpPain;
    }

    public void setInitialThumpPain(float initialThumpPain) {
        this.initialThumpPain = initialThumpPain;
    }

    public float getInitialScratchPain() {
        return this.initialScratchPain;
    }

    public void setInitialScratchPain(float initialScratchPain) {
        this.initialScratchPain = initialScratchPain;
    }

    public float getInitialBitePain() {
        return this.initialBitePain;
    }

    public void setInitialBitePain(float initialBitePain) {
        this.initialBitePain = initialBitePain;
    }

    public float getInitialWoundPain() {
        return this.initialWoundPain;
    }

    public void setInitialWoundPain(float initialWoundPain) {
        this.initialWoundPain = initialWoundPain;
    }

    public float getContinualPainIncrease() {
        return this.continualPainIncrease;
    }

    public void setContinualPainIncrease(float continualPainIncrease) {
        this.continualPainIncrease = continualPainIncrease;
    }

    public float getPainReductionFromMeds() {
        return this.painReductionFromMeds;
    }

    public void setPainReductionFromMeds(float painReductionFromMeds) {
        this.painReductionFromMeds = painReductionFromMeds;
    }

    public float getStandardPainReductionWhenWell() {
        return this.standardPainReductionWhenWell;
    }

    public void setStandardPainReductionWhenWell(float standardPainReductionWhenWell) {
        this.standardPainReductionWhenWell = standardPainReductionWhenWell;
    }

    public int getOldNumZombiesVisible() {
        return this.oldNumZombiesVisible;
    }

    public void setOldNumZombiesVisible(int oldNumZombiesVisible) {
        this.oldNumZombiesVisible = oldNumZombiesVisible;
    }

    public boolean getWasDraggingCorpse() {
        return this.wasDraggingCorpse;
    }

    public void setWasDraggingCorpse(boolean wasDraggingCorpse) {
        this.wasDraggingCorpse = wasDraggingCorpse;
    }

    public int getCurrentNumZombiesVisible() {
        return this.currentNumZombiesVisible;
    }

    public void setCurrentNumZombiesVisible(int currentNumZombiesVisible) {
        this.currentNumZombiesVisible = currentNumZombiesVisible;
    }

    public float getPanicIncreaseValue() {
        return this.panicIncreaseValue;
    }

    public float getPanicIncreaseValueFrame() {
        return 0.035f;
    }

    public void setPanicIncreaseValue(float panicIncreaseValue) {
        if (this.parentChar.hasTrait(CharacterTrait.DESENSITIZED)) {
            this.panicIncreaseValue = 0.0f;
            return;
        }
        this.panicIncreaseValue = panicIncreaseValue;
    }

    public float getPanicReductionValue() {
        return this.panicReductionValue;
    }

    public void setPanicReductionValue(float panicReductionValue) {
        this.panicReductionValue = panicReductionValue;
    }

    public float getDrunkIncreaseValue() {
        return this.drunkIncreaseValue;
    }

    public void setDrunkIncreaseValue(float drunkIncreaseValue) {
        this.drunkIncreaseValue = drunkIncreaseValue;
    }

    public float getDrunkReductionValue() {
        return this.drunkReductionValue;
    }

    public void setDrunkReductionValue(float drunkReductionValue) {
        this.drunkReductionValue = drunkReductionValue;
    }

    public boolean isIsOnFire() {
        return this.isOnFire;
    }

    public void setIsOnFire(boolean isOnFire) {
        this.isOnFire = isOnFire;
    }

    public boolean isBurntToDeath() {
        return this.burntToDeath;
    }

    public void setBurntToDeath(boolean burntToDeath) {
        this.burntToDeath = burntToDeath;
    }

    public float getCatchACold() {
        return this.catchACold;
    }

    public void setCatchACold(float catchACold) {
        this.catchACold = catchACold;
    }

    public boolean isHasACold() {
        return this.hasACold;
    }

    public void setHasACold(boolean hasACold) {
        this.hasACold = hasACold;
    }

    public void setColdStrength(float coldStrength) {
        this.coldStrength = coldStrength;
    }

    public float getColdProgressionRate() {
        return this.coldProgressionRate;
    }

    public void setColdProgressionRate(float coldProgressionRate) {
        this.coldProgressionRate = coldProgressionRate;
    }

    public float getTimeToSneezeOrCough() {
        return this.timeToSneezeOrCough;
    }

    public void setTimeToSneezeOrCough(float timeToSneezeOrCough) {
        this.timeToSneezeOrCough = timeToSneezeOrCough;
    }

    public int getSmokerSneezeTimerMin() {
        return 43200;
    }

    public int getSmokerSneezeTimerMax() {
        return 129600;
    }

    public int getMildColdSneezeTimerMin() {
        return this.mildColdSneezeTimerMin;
    }

    public void setMildColdSneezeTimerMin(int mildColdSneezeTimerMin) {
        this.mildColdSneezeTimerMin = mildColdSneezeTimerMin;
    }

    public int getMildColdSneezeTimerMax() {
        return this.mildColdSneezeTimerMax;
    }

    public void setMildColdSneezeTimerMax(int mildColdSneezeTimerMax) {
        this.mildColdSneezeTimerMax = mildColdSneezeTimerMax;
    }

    public int getColdSneezeTimerMin() {
        return this.coldSneezeTimerMin;
    }

    public void setColdSneezeTimerMin(int coldSneezeTimerMin) {
        this.coldSneezeTimerMin = coldSneezeTimerMin;
    }

    public int getColdSneezeTimerMax() {
        return this.coldSneezeTimerMax;
    }

    public void setColdSneezeTimerMax(int coldSneezeTimerMax) {
        this.coldSneezeTimerMax = coldSneezeTimerMax;
    }

    public int getNastyColdSneezeTimerMin() {
        return this.nastyColdSneezeTimerMin;
    }

    public void setNastyColdSneezeTimerMin(int nastyColdSneezeTimerMin) {
        this.nastyColdSneezeTimerMin = nastyColdSneezeTimerMin;
    }

    public int getNastyColdSneezeTimerMax() {
        return this.nastyColdSneezeTimerMax;
    }

    public void setNastyColdSneezeTimerMax(int nastyColdSneezeTimerMax) {
        this.nastyColdSneezeTimerMax = nastyColdSneezeTimerMax;
    }

    public int getSneezeCoughActive() {
        return this.sneezeCoughActive;
    }

    public void setSneezeCoughActive(int sneezeCoughActive) {
        this.sneezeCoughActive = sneezeCoughActive;
    }

    public int getSneezeCoughTime() {
        return this.sneezeCoughTime;
    }

    public void setSneezeCoughTime(int sneezeCoughTime) {
        this.sneezeCoughTime = sneezeCoughTime;
    }

    public int getSneezeCoughDelay() {
        return this.sneezeCoughDelay;
    }

    public void setSneezeCoughDelay(int sneezeCoughDelay) {
        this.sneezeCoughDelay = sneezeCoughDelay;
    }

    public IsoGameCharacter getParentChar() {
        return this.parentChar;
    }

    public boolean isReduceFakeInfection() {
        return this.reduceFakeInfection;
    }

    public void setReduceFakeInfection(boolean reduceFakeInfection) {
        this.reduceFakeInfection = reduceFakeInfection;
    }

    public void AddRandomDamage() {
        BodyPart bodyPart = this.getBodyParts().get(Rand.Next(this.getBodyParts().size()));
        switch (Rand.Next(4)) {
            case 0: {
                bodyPart.generateDeepWound();
                if (Rand.Next(4) != 0) break;
                bodyPart.setInfectedWound(true);
                break;
            }
            case 1: {
                bodyPart.generateDeepShardWound();
                if (Rand.Next(4) != 0) break;
                bodyPart.setInfectedWound(true);
                break;
            }
            case 2: {
                bodyPart.setFractureTime(Rand.Next(30, 50));
                break;
            }
            case 3: {
                bodyPart.setBurnTime(Rand.Next(30, 50));
            }
        }
    }

    public float getPainReduction() {
        return this.painReduction;
    }

    public void setPainReduction(float painReduction) {
        this.painReduction = painReduction;
    }

    public float getColdReduction() {
        return this.coldReduction;
    }

    public void setColdReduction(float coldReduction) {
        this.coldReduction = coldReduction;
    }

    public int getRemotePainLevel() {
        return this.remotePainLevel;
    }

    public void setRemotePainLevel(int painLevel) {
        this.remotePainLevel = painLevel;
    }

    public float getColdDamageStage() {
        return this.coldDamageStage;
    }

    public void setColdDamageStage(float coldDamageStage) {
        this.coldDamageStage = coldDamageStage;
    }

    public Thermoregulator getThermoregulator() {
        return this.thermoregulator;
    }

    public void decreaseBodyWetness(float amount) {
        if (!this.bodyParts.isEmpty()) {
            for (int i = 0; i < this.bodyParts.size(); ++i) {
                BodyPart bp = this.bodyParts.get(i);
                bp.setWetness(bp.getWetness() - amount);
            }
        }
        this.stats.remove(CharacterStat.WETNESS, amount);
    }

    public void increaseBodyWetness(float amount) {
        if (!this.bodyParts.isEmpty()) {
            for (int i = 0; i < this.bodyParts.size(); ++i) {
                BodyPart bp = this.bodyParts.get(i);
                bp.setWetness(bp.getWetness() + amount);
            }
        }
        this.stats.add(CharacterStat.WETNESS, amount);
    }

    public void DamageFromAnimal(IsoAnimal wielder) {
        boolean behind;
        boolean spikedPart;
        float damage = wielder.calcDamage();
        String dotSide = this.parentChar.testDotSide(wielder);
        boolean isBehind = dotSide.equals(behindStr);
        this.parentChar.setHitFromBehind(isBehind);
        if (GameClient.client) {
            return;
        }
        boolean painType = true;
        boolean doDamage = true;
        int partIndex = Rand.Next(BodyPartType.ToIndex(BodyPartType.Hand_L), BodyPartType.ToIndex(BodyPartType.MAX));
        boolean blade = true;
        boolean bullet = false;
        BodyPart part = this.getBodyPart(BodyPartType.FromIndex(partIndex));
        float def = this.parentChar.getBodyPartClothingDefense(part.getIndex(), true, false);
        if ((float)Rand.Next(100) < def) {
            doDamage = false;
            this.parentChar.addHoleFromZombieAttacks(BloodBodyPartType.FromIndex(partIndex), false);
        }
        if (!doDamage) {
            return;
        }
        this.parentChar.addHole(BloodBodyPartType.FromIndex(partIndex));
        this.parentChar.splatBloodFloorBig();
        this.parentChar.splatBloodFloorBig();
        this.parentChar.splatBloodFloorBig();
        if (wielder.adef.canDoLaceration && Rand.NextBool(6)) {
            part.generateDeepWound();
        } else if (wielder.adef.canDoLaceration && Rand.NextBool(3)) {
            part.setCut(true);
        } else if (Rand.NextBool(2)) {
            part.setScratched(true, true);
        }
        if (partIndex == BodyPartType.ToIndex(BodyPartType.Head)) {
            damage *= 4.0f;
        }
        if (partIndex == BodyPartType.ToIndex(BodyPartType.Neck)) {
            damage *= 4.0f;
        }
        if (partIndex == BodyPartType.ToIndex(BodyPartType.Torso_Upper)) {
            damage *= 2.0f;
        }
        this.AddDamage(partIndex, damage);
        switch (1) {
            case 0: {
                this.stats.add(CharacterStat.PAIN, this.getInitialThumpPain() * BodyPartType.getPainModifyer(partIndex));
                break;
            }
            case 1: {
                this.stats.add(CharacterStat.PAIN, this.getInitialScratchPain() * BodyPartType.getPainModifyer(partIndex));
                break;
            }
            case 2: {
                this.stats.add(CharacterStat.PAIN, this.getInitialBitePain() * BodyPartType.getPainModifyer(partIndex));
            }
        }
        if (GameServer.server) {
            this.parentChar.getNetworkCharacterAI().syncDamage();
        }
        if (spikedPart = (behind = !wielder.isAimAtFloor() ? wielder.isBehind(this.parentChar) : this.parentChar.isFallOnFront()) ? this.parentChar.bodyPartIsSpikedBehind(partIndex) : this.parentChar.bodyPartIsSpiked(partIndex)) {
            this.parentChar.addBlood(BloodBodyPartType.FromIndex(partIndex), true, false, false);
            wielder.spikePart(BodyPartType.Head);
        }
    }

    public float getGeneralWoundInfectionLevel() {
        if (SandboxOptions.instance.woundInfectionFactor.getValue() <= 0.0) {
            return 0.0f;
        }
        float woundInfectionLevel = 0.0f;
        if (!this.bodyParts.isEmpty()) {
            for (int i = 0; i < this.bodyParts.size(); ++i) {
                BodyPart bp = this.bodyParts.get(i);
                if (!bp.isInfectedWound()) continue;
                woundInfectionLevel += bp.getWoundInfectionLevel();
            }
        }
        woundInfectionLevel *= 10.0f;
        woundInfectionLevel *= (float)SandboxOptions.instance.woundInfectionFactor.getValue();
        woundInfectionLevel = Math.min(woundInfectionLevel, 100.0f);
        return woundInfectionLevel;
    }

    public void UpdateDiscomfort() {
        float draggingCorpseMod = this.parentChar.isDraggingCorpse() ? 0.3f : 0.0f;
        float clothingMod = this.parentChar.getClothingDiscomfortModifier();
        float bedMod = 0.0f;
        if (this.parentChar.isAsleep()) {
            switch (this.parentChar.getBedType()) {
                case "badBed": {
                    bedMod = 0.3f;
                    break;
                }
                case "badBedPillow": {
                    bedMod = 0.2f;
                    break;
                }
                case "floor": {
                    bedMod = 0.5f;
                    break;
                }
                case "floorPillow": {
                    bedMod = 0.4f;
                }
            }
        }
        float drunkMod = 1.0f - 0.5f * (this.stats.get(CharacterStat.INTOXICATION) / 100.0f);
        float hypoMod = 0.1f * (float)this.parentChar.getMoodles().getMoodleLevel(MoodleType.HYPOTHERMIA);
        float hyperMod = 0.1f * (float)this.parentChar.getMoodles().getMoodleLevel(MoodleType.HYPERTHERMIA);
        float wetMod = 0.1f * (float)this.parentChar.getMoodles().getMoodleLevel(MoodleType.WET);
        float vehicleMod = this.parentChar.getVehicleDiscomfortModifier();
        float discomfortMod = 0.0f;
        discomfortMod += bedMod;
        discomfortMod += clothingMod;
        discomfortMod += draggingCorpseMod;
        discomfortMod += hypoMod;
        discomfortMod += hyperMod;
        discomfortMod += wetMod;
        discomfortMod += vehicleMod;
        float discomfortTarget = PZMath.clamp(discomfortMod *= drunkMod, 0.0f, 1.0f) * 100.0f;
        float discomfortStepRate = 0.005f * GameTime.instance.getMultiplier();
        if (discomfortTarget > this.stats.get(CharacterStat.DISCOMFORT)) {
            discomfortStepRate *= 0.025f;
        }
        if (this.parentChar.isAsleep()) {
            this.stats.set(CharacterStat.DISCOMFORT, discomfortTarget);
        } else {
            float discomfort = this.stats.get(CharacterStat.DISCOMFORT);
            if (!PZMath.equal(discomfort, discomfortTarget, discomfortStepRate)) {
                this.stats.set(CharacterStat.DISCOMFORT, PZMath.lerp(discomfort, discomfortTarget, discomfortStepRate));
            } else if (discomfort != discomfortTarget) {
                this.stats.set(CharacterStat.DISCOMFORT, discomfortTarget);
            }
        }
        if (this.parentChar.getMoodles().getMoodleLevel(MoodleType.UNCOMFORTABLE) >= 1 && this.stats.get(CharacterStat.STRESS) < CharacterStat.STRESS.getMaximumValue()) {
            float discomfortMalus = discomfortMod > 1.0f ? 3.0f + (discomfortMod - 1.0f) * 9.0f : 3.0f;
            this.stats.add(CharacterStat.STRESS, (float)(ZomboidGlobals.stressFromDiscomfort * (double)(this.stats.get(CharacterStat.DISCOMFORT) * discomfortMalus) * (double)GameTime.instance.getMultiplier() * (double)GameTime.instance.getDeltaMinutesPerDay()));
        }
    }

    public void addStiffness(BodyPart part, float stiffness) {
        part.addStiffness(stiffness);
    }

    public void addStiffness(BodyPartType partType, float stiffness) {
        BodyPart part = this.getBodyPart(partType);
        part.addStiffness(stiffness);
    }
}

