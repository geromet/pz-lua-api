/*
 * Decompiled with CFR 0.152.
 */
package zombie.ai.states;

import java.util.Map;
import zombie.GameTime;
import zombie.IndieGL;
import zombie.SandboxOptions;
import zombie.UsedFromLua;
import zombie.ai.State;
import zombie.characters.IsoGameCharacter;
import zombie.characters.IsoPlayer;
import zombie.characters.skills.PerkFactory;
import zombie.core.math.PZMath;
import zombie.core.random.Rand;
import zombie.debug.DebugLog;
import zombie.iso.IsoCell;
import zombie.iso.IsoDirections;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoObject;
import zombie.iso.IsoUtils;
import zombie.iso.IsoWorld;
import zombie.iso.SpriteDetails.IsoFlagType;
import zombie.iso.objects.IsoThumpable;
import zombie.iso.objects.IsoWindow;
import zombie.iso.objects.IsoWindowFrame;
import zombie.scripting.objects.CharacterTrait;
import zombie.scripting.objects.MoodleType;
import zombie.ui.TextManager;
import zombie.ui.UIFont;

@UsedFromLua
public final class ClimbSheetRopeState
extends State {
    private static final ClimbSheetRopeState INSTANCE = new ClimbSheetRopeState();
    public static final float FallChanceBase = 1.0f;
    public static final float FallChanceMultiplier = 10.0f;
    private static final float FallChanceScale = 100.0f;
    public static final float ClimbSpeed = 0.16f;
    public static final float ClimbSlowdown = 0.5f;
    public static final State.Param<Float> SPEED = State.Param.ofFloat("speed", 0.0f);
    public static final State.Param<Boolean> CLIMB = State.Param.ofBool("climb", false);
    private int numberOfFallingChecks;

    public static ClimbSheetRopeState instance() {
        return INSTANCE;
    }

    private ClimbSheetRopeState() {
        super(true, true, true, true);
    }

    @Override
    public void enter(IsoGameCharacter isoGameCharacter) {
        isoGameCharacter.setIgnoreMovement(true);
        isoGameCharacter.setHideWeaponModel(true);
        isoGameCharacter.setbClimbing(true);
        isoGameCharacter.setVariable("ClimbRope", true);
        this.setParams(isoGameCharacter, State.Stage.Enter);
        ClimbSheetRopeState.createClimbData(isoGameCharacter);
        ClimbSheetRopeState.calculateClimb(isoGameCharacter);
        this.numberOfFallingChecks = 0;
    }

    @Override
    public void execute(IsoGameCharacter isoGameCharacter) {
        IsoPlayer isoPlayer;
        ClimbData climbData = isoGameCharacter.getClimbData();
        ClimbSheetRopeState.applyIdealDirection(isoGameCharacter);
        float climbSpeed = isoGameCharacter.getClimbRopeSpeed(false);
        if (!isoGameCharacter.isLocal()) {
            climbSpeed = isoGameCharacter.get(SPEED, Float.valueOf(isoGameCharacter.getClimbRopeSpeed(false))).floatValue();
        }
        isoGameCharacter.getSpriteDef().animFrameIncrease = climbSpeed;
        float currentClimbHeight = isoGameCharacter.getZ() + climbSpeed / 10.0f * GameTime.instance.getMultiplier();
        isoGameCharacter.setZ(currentClimbHeight);
        if (!(currentClimbHeight >= climbData.targetFallHeight) && currentClimbHeight > (float)climbData.targetClimbHeight) {
            this.finishClimbing(isoGameCharacter);
            return;
        }
        ClimbSheetRopeState.fallChanceCalculation(isoGameCharacter);
        boolean canFallCheck = isoGameCharacter.getClimbRopeTime() > climbData.fallChance * 10.0f;
        boolean fall = Rand.NextBool((int)climbData.fallChance);
        if (canFallCheck) {
            DebugLog.Action.println("Checking For Fall #%d", this.numberOfFallingChecks++);
        }
        if (!IsoWindow.isSheetRopeHere(isoGameCharacter.getCurrentSquare())) {
            isoGameCharacter.setCollidable(true);
            isoGameCharacter.setbClimbing(false);
            isoGameCharacter.setbFalling(true);
            isoGameCharacter.clearVariable("ClimbRope");
        } else if (canFallCheck && !SandboxOptions.instance.easyClimbing.getValue() && fall && isoGameCharacter.isLocal()) {
            isoGameCharacter.fallFromRope();
        }
        float skillFactor = (float)(isoGameCharacter.getPerkLevel(PerkFactory.Perks.Nimble) + Math.max(isoGameCharacter.getPerkLevel(PerkFactory.Perks.Strength), isoGameCharacter.getPerkLevel(PerkFactory.Perks.Fitness)) * 2) / 3.0f;
        isoGameCharacter.addBothArmMuscleStrain((float)(0.02 * (double)GameTime.instance.getMultiplier() * (double)(isoGameCharacter.getMoodles().getMoodleLevel(MoodleType.HEAVY_LOAD) + 1)) * ((15.0f - skillFactor) / 10.0f) * (GameTime.instance.getMultiplier() / 0.8f));
        if (isoGameCharacter instanceof IsoPlayer && (isoPlayer = (IsoPlayer)isoGameCharacter).isLocalPlayer()) {
            isoPlayer.dirtyRecalcGridStackTime = 2.0f;
        }
    }

    @Override
    public void exit(IsoGameCharacter isoGameCharacter) {
        isoGameCharacter.setCollidable(true);
        isoGameCharacter.setIgnoreMovement(false);
        isoGameCharacter.setHideWeaponModel(false);
        isoGameCharacter.setbClimbing(false);
        isoGameCharacter.clearVariable("ClimbRope");
        this.setParams(isoGameCharacter, State.Stage.Exit);
    }

    @Override
    public void setParams(IsoGameCharacter isoGameCharacter, State.Stage stage) {
        if (isoGameCharacter.isLocal()) {
            isoGameCharacter.set(SPEED, Float.valueOf(isoGameCharacter.getClimbRopeSpeed(false)));
            isoGameCharacter.set(CLIMB, isoGameCharacter.isClimbing());
        }
        super.setParams(isoGameCharacter, stage);
    }

    public static void createClimbData(IsoGameCharacter isoGameCharacter) {
        if (isoGameCharacter.getClimbData() == null) {
            isoGameCharacter.setClimbData(new ClimbData());
        }
    }

    private static ClimbStatus calculateClimbOutcome(IsoGameCharacter isoGameCharacter) {
        ClimbData climbData = isoGameCharacter.getClimbData();
        IsoGridSquare exitDirectionIsqGridSquare = climbData.targetGridSquare.getAdjacentSquare(isoGameCharacter.dir);
        if (!exitDirectionIsqGridSquare.TreatAsSolidFloor()) {
            return ClimbStatus.Blocked;
        }
        IsoWindow window = climbData.targetGridSquare.getWindowTo(exitDirectionIsqGridSquare);
        if (window != null) {
            if (!window.IsOpen()) {
                window.ToggleWindow(isoGameCharacter);
            }
            if (!window.canClimbThrough(isoGameCharacter)) {
                return ClimbStatus.Blocked;
            }
            climbData.climbTargetIsoObject = window;
            return ClimbStatus.OpenWindow;
        }
        IsoThumpable isoThumpable = climbData.targetGridSquare.getWindowThumpableTo(exitDirectionIsqGridSquare);
        if (isoThumpable != null) {
            if (!isoThumpable.canClimbThrough(isoGameCharacter)) {
                return ClimbStatus.Blocked;
            }
            climbData.climbTargetIsoObject = isoThumpable;
            return ClimbStatus.OpenWindow;
        }
        isoThumpable = climbData.targetGridSquare.getHoppableThumpableTo(exitDirectionIsqGridSquare);
        if (isoThumpable != null) {
            if (!IsoWindow.canClimbThroughHelper(isoGameCharacter, climbData.targetGridSquare, exitDirectionIsqGridSquare, isoGameCharacter.dir == IsoDirections.N || isoGameCharacter.dir == IsoDirections.S)) {
                return ClimbStatus.Blocked;
            }
            return ClimbStatus.Fence;
        }
        IsoWindowFrame isoWindowFrame = climbData.targetGridSquare.getWindowFrameTo(exitDirectionIsqGridSquare);
        if (isoWindowFrame != null) {
            if (!isoWindowFrame.canClimbThrough(isoGameCharacter)) {
                return ClimbStatus.Blocked;
            }
            climbData.climbTargetIsoObject = isoWindowFrame;
            return ClimbStatus.WindowFrame;
        }
        IsoObject hoppableWall = climbData.targetGridSquare.getWallHoppableTo(exitDirectionIsqGridSquare);
        if (hoppableWall != null) {
            if (!IsoWindow.canClimbThroughHelper(isoGameCharacter, climbData.targetGridSquare, exitDirectionIsqGridSquare, isoGameCharacter.dir == IsoDirections.N || isoGameCharacter.dir == IsoDirections.S)) {
                return ClimbStatus.Blocked;
            }
            return ClimbStatus.Fence;
        }
        return ClimbStatus.Undefined;
    }

    private void finishClimbing(IsoGameCharacter isoGameCharacter) {
        ClimbData climbData = isoGameCharacter.getClimbData();
        isoGameCharacter.setZ(climbData.targetClimbHeight);
        isoGameCharacter.setCurrent(climbData.targetGridSquare);
        isoGameCharacter.setCollidable(true);
        switch (climbData.exitBlocked.ordinal()) {
            case 1: {
                isoGameCharacter.climbDownSheetRope();
                break;
            }
            case 2: {
                isoGameCharacter.climbThroughWindow(climbData.climbTargetIsoObject);
                break;
            }
            case 3: {
                isoGameCharacter.climbThroughWindowFrame((IsoWindowFrame)climbData.climbTargetIsoObject);
                break;
            }
            case 4: {
                isoGameCharacter.climbOverFence(isoGameCharacter.dir);
                break;
            }
        }
    }

    public static void setIdealDirection(IsoGameCharacter isoGameCharacter) {
        ClimbData climbData = isoGameCharacter.getClimbData();
        if (isoGameCharacter.getCurrentSquare().getProperties().has(IsoFlagType.climbSheetN) || isoGameCharacter.getCurrentSquare().getProperties().has(IsoFlagType.climbSheetTopN)) {
            isoGameCharacter.setDir(IsoDirections.N);
            climbData.idealx = 0.54f;
            climbData.idealy = 0.39f;
        }
        if (isoGameCharacter.getCurrentSquare().getProperties().has(IsoFlagType.climbSheetS) || isoGameCharacter.getCurrentSquare().getProperties().has(IsoFlagType.climbSheetTopS)) {
            isoGameCharacter.setDir(IsoDirections.S);
            climbData.idealx = 0.118f;
            climbData.idealy = 0.5756f;
        }
        if (isoGameCharacter.getCurrentSquare().getProperties().has(IsoFlagType.climbSheetW) || isoGameCharacter.getCurrentSquare().getProperties().has(IsoFlagType.climbSheetTopW)) {
            isoGameCharacter.setDir(IsoDirections.W);
            climbData.idealx = 0.4f;
            climbData.idealy = 0.7f;
        }
        if (isoGameCharacter.getCurrentSquare().getProperties().has(IsoFlagType.climbSheetE) || isoGameCharacter.getCurrentSquare().getProperties().has(IsoFlagType.climbSheetTopE)) {
            isoGameCharacter.setDir(IsoDirections.E);
            climbData.idealx = 0.5417f;
            climbData.idealy = 0.3144f;
        }
    }

    public static void applyIdealDirection(IsoGameCharacter isoGameCharacter) {
        float dif;
        ClimbData climbData = isoGameCharacter.getClimbData();
        float ox = isoGameCharacter.getX() - (float)PZMath.fastfloor(isoGameCharacter.getX());
        float oy = isoGameCharacter.getY() - (float)PZMath.fastfloor(isoGameCharacter.getY());
        if (ox != climbData.idealx) {
            dif = (climbData.idealx - ox) / 4.0f;
            isoGameCharacter.setX((float)PZMath.fastfloor(isoGameCharacter.getX()) + (ox += dif));
        }
        if (oy != climbData.idealy) {
            dif = (climbData.idealy - oy) / 4.0f;
            isoGameCharacter.setY((float)PZMath.fastfloor(isoGameCharacter.getY()) + (oy += dif));
        }
        isoGameCharacter.setNextX(isoGameCharacter.getX());
        isoGameCharacter.setNextY(isoGameCharacter.getY());
    }

    private static void calculateClimb(IsoGameCharacter isoGameCharacter) {
        ClimbSheetRopeState.setIdealDirection(isoGameCharacter);
        ClimbSheetRopeState.applyIdealDirection(isoGameCharacter);
        ClimbData climbData = isoGameCharacter.getClimbData();
        int maxLevel = isoGameCharacter.getCurrentSquare().getChunk().getMaxLevel();
        IsoCell cell = IsoWorld.instance.getCell();
        for (int z = PZMath.fastfloor(isoGameCharacter.getZ()); z <= maxLevel; ++z) {
            IsoGridSquare isoGridSquare = cell.getGridSquare(isoGameCharacter.getX(), isoGameCharacter.getY(), (double)z);
            if (!IsoWindow.isTopOfSheetRopeHere(isoGridSquare)) continue;
            climbData.targetGridSquare = isoGridSquare;
            climbData.targetClimbHeight = z;
            climbData.exitBlocked = ClimbSheetRopeState.calculateClimbOutcome(isoGameCharacter);
            break;
        }
        ClimbSheetRopeState.fallChanceCalculation(isoGameCharacter);
    }

    private static float fallChanceCalculation(IsoGameCharacter isoGameCharacter) {
        ClimbData climbData = isoGameCharacter.getClimbData();
        climbData.fallChance = isoGameCharacter.getClimbingFailChanceFloat() + 1.0f;
        isoGameCharacter.setClimbRopeTime(isoGameCharacter.getClimbRopeTime() + GameTime.instance.getMultiplier());
        climbData.fallChance *= 100.0f;
        climbData.fallChance = climbData.fallChance / (GameTime.instance.getMultiplier() < 1.0f ? 1.0f : (float)((int)GameTime.instance.getMultiplier()));
        return climbData.fallChance;
    }

    public void debug(IsoGameCharacter isoGameCharacter) {
        ClimbData debugClimbData = isoGameCharacter.getClimbData();
        IsoGridSquare currentIsoGridSquare = isoGameCharacter.getCurrentSquare();
        if (currentIsoGridSquare.haveSheetRope) {
            IndieGL.glBlendFunc(770, 771);
            IndieGL.disableDepthTest();
            IndieGL.StartShader(0);
            int sx = (int)IsoUtils.XToScreenExact(currentIsoGridSquare.getX(), currentIsoGridSquare.getY(), currentIsoGridSquare.getZ(), 0);
            int sy = (int)IsoUtils.YToScreenExact(currentIsoGridSquare.getX(), currentIsoGridSquare.getY(), (float)currentIsoGridSquare.getZ() + 1.5f, 0);
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Climb Sheet Rope").append("\n");
            stringBuilder.append("Fail Chance: ").append(isoGameCharacter.getClimbingFailChanceFloat()).append("\n");
            stringBuilder.append("Fall Chance: ").append(debugClimbData.fallChance).append("\n");
            stringBuilder.append("Distance: ").append(debugClimbData.targetClimbHeight + 1).append("\n");
            stringBuilder.append("Climbing Skills:").append("\n");
            float chance = (float)isoGameCharacter.getPerkLevel(PerkFactory.Perks.Strength) * 2.0f;
            stringBuilder.append(String.format("   %s: %d %.2f%%%n", PerkFactory.Perks.Strength.name, isoGameCharacter.getPerkLevel(PerkFactory.Perks.Strength), Float.valueOf(chance)));
            chance = (float)isoGameCharacter.getPerkLevel(PerkFactory.Perks.Fitness) * 2.0f;
            stringBuilder.append(String.format("   %s: %d %.2f%%%n", PerkFactory.Perks.Fitness.name, isoGameCharacter.getPerkLevel(PerkFactory.Perks.Fitness), Float.valueOf(chance)));
            chance = (float)isoGameCharacter.getPerkLevel(PerkFactory.Perks.Nimble) * 2.0f;
            stringBuilder.append(String.format("   %s: %d %.2f%%%n", PerkFactory.Perks.Nimble.name, isoGameCharacter.getPerkLevel(PerkFactory.Perks.Nimble), Float.valueOf(chance)));
            stringBuilder.append("Climbing Bonus:").append("\n");
            chance = !isoGameCharacter.isWearingAwkwardGloves() && isoGameCharacter.isWearingGloves() ? 4.0f : 0.0f;
            stringBuilder.append(String.format("   %s: %.2f%%%n", "Wearing Gloves", Float.valueOf(chance)));
            chance = isoGameCharacter.hasTrait(CharacterTrait.DEXTROUS) ? 4.0f : 0.0f;
            stringBuilder.append(String.format("   %s: %.2f%%%n", CharacterTrait.DEXTROUS.toString(), Float.valueOf(chance)));
            chance = isoGameCharacter.hasTrait(CharacterTrait.BURGLAR) ? 4.0f : 0.0f;
            stringBuilder.append(String.format("   %s: %.2f%%%n", CharacterTrait.BURGLAR.toString(), Float.valueOf(chance)));
            chance = isoGameCharacter.hasTrait(CharacterTrait.GYMNAST) ? 4.0f : 0.0f;
            stringBuilder.append(String.format("   %s: %.2f%%%n", CharacterTrait.GYMNAST.toString(), Float.valueOf(chance)));
            stringBuilder.append("Climbing Penalty:").append("\n");
            chance = (float)isoGameCharacter.getMoodles().getMoodleLevel(MoodleType.ENDURANCE) * -5.0f;
            stringBuilder.append(String.format("   %s: %d %.2f%%%n", MoodleType.ENDURANCE.toString(), isoGameCharacter.getMoodles().getMoodleLevel(MoodleType.ENDURANCE), Float.valueOf(chance)));
            chance = (float)isoGameCharacter.getMoodles().getMoodleLevel(MoodleType.DRUNK) * -8.0f;
            stringBuilder.append(String.format("   %s: %d %.2f%%%n", MoodleType.DRUNK.toString(), isoGameCharacter.getMoodles().getMoodleLevel(MoodleType.DRUNK), Float.valueOf(chance)));
            chance = (float)isoGameCharacter.getMoodles().getMoodleLevel(MoodleType.HEAVY_LOAD) * -8.0f;
            stringBuilder.append(String.format("   %s: %d %.2f%%%n", MoodleType.HEAVY_LOAD.toString(), isoGameCharacter.getMoodles().getMoodleLevel(MoodleType.HEAVY_LOAD), Float.valueOf(chance)));
            chance = (float)isoGameCharacter.getMoodles().getMoodleLevel(MoodleType.PAIN) * -5.0f;
            stringBuilder.append(String.format("   %s: %d %.2f%%%n", MoodleType.PAIN.toString(), isoGameCharacter.getMoodles().getMoodleLevel(MoodleType.PAIN), Float.valueOf(chance)));
            chance = isoGameCharacter.hasTrait(CharacterTrait.OBESE) ? -25.0f : 0.0f;
            stringBuilder.append(String.format("   %s: %.2f%%%n", CharacterTrait.OBESE.toString(), Float.valueOf(chance)));
            chance = isoGameCharacter.hasTrait(CharacterTrait.OVERWEIGHT) ? -15.0f : 0.0f;
            stringBuilder.append(String.format("   %s: %.2f%%%n", CharacterTrait.OVERWEIGHT.toString(), Float.valueOf(chance)));
            chance = isoGameCharacter.hasTrait(CharacterTrait.CLUMSY) ? 2.0f : 0.0f;
            stringBuilder.append(String.format("   %s: %s%n", CharacterTrait.CLUMSY.toString(), chance == 2.0f ? "Half" : ""));
            chance = isoGameCharacter.isWearingAwkwardGloves() ? 2.0f : 0.0f;
            stringBuilder.append(String.format("   %s: %s%n", "Wearing Awkward Gloves", chance == 2.0f ? "Half" : ""));
            chance = isoGameCharacter.hasTrait(CharacterTrait.ALL_THUMBS) ? -4.0f : 0.0f;
            stringBuilder.append(String.format("   %s: %.2f%%%n", CharacterTrait.ALL_THUMBS.toString(), Float.valueOf(chance)));
            chance = isoGameCharacter.nearbyZombieClimbPenalty();
            stringBuilder.append(String.format("   %s: %.2f%%%n", "Nearby Zombies", Float.valueOf(chance)));
            TextManager.instance.DrawString(UIFont.NewMedium, sx, sy, 1.5, stringBuilder.toString(), 1.0, 1.0, 1.0, 1.0);
        }
    }

    @Override
    public boolean isProcessedOnEnter() {
        return true;
    }

    @Override
    public void processOnEnter(IsoGameCharacter owner, Map<Object, Object> delegate) {
        owner.setbClimbing(CLIMB.fromDelegate(delegate));
    }

    @Override
    public boolean isProcessedOnExit() {
        return true;
    }

    @Override
    public void processOnExit(IsoGameCharacter owner, Map<Object, Object> delegate) {
        owner.setbClimbing(CLIMB.fromDelegate(delegate));
    }

    public static class ClimbData {
        public int targetClimbHeight;
        public float fallChance;
        public float idealx;
        public float idealy;
        public float targetFallHeight = Float.MAX_VALUE;
        public IsoObject climbTargetIsoObject;
        public IsoGridSquare targetGridSquare;
        public ClimbStatus exitBlocked;
    }

    public static enum ClimbStatus {
        Undefined,
        Blocked,
        OpenWindow,
        WindowFrame,
        Fence;

    }
}

