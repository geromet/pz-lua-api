/*
 * Decompiled with CFR 0.152.
 */
package zombie.ai.states;

import java.util.Map;
import zombie.GameTime;
import zombie.SandboxOptions;
import zombie.UsedFromLua;
import zombie.ai.State;
import zombie.ai.states.ClimbSheetRopeState;
import zombie.characters.IsoGameCharacter;
import zombie.characters.IsoPlayer;
import zombie.characters.skills.PerkFactory;
import zombie.core.math.PZMath;
import zombie.core.random.Rand;
import zombie.debug.DebugLog;
import zombie.iso.IsoCell;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoWorld;
import zombie.iso.SpriteDetails.IsoFlagType;
import zombie.iso.objects.IsoWindow;
import zombie.scripting.objects.MoodleType;

@UsedFromLua
public final class ClimbDownSheetRopeState
extends State {
    private static final ClimbDownSheetRopeState INSTANCE = new ClimbDownSheetRopeState();
    private static final float ClimbDownFallChanceScale = 300.0f;
    public static final State.Param<Float> SPEED = State.Param.ofFloat("speed", 0.0f);
    public static final State.Param<Boolean> CLIMB = State.Param.ofBool("climb", false);
    private int numberOfFallingChecks;

    public static ClimbDownSheetRopeState instance() {
        return INSTANCE;
    }

    private ClimbDownSheetRopeState() {
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
        ClimbDownSheetRopeState.calculateClimbDown(isoGameCharacter);
        this.numberOfFallingChecks = 0;
    }

    @Override
    public void execute(IsoGameCharacter isoGameCharacter) {
        IsoPlayer isoPlayer;
        ClimbSheetRopeState.ClimbData climbData = isoGameCharacter.getClimbData();
        ClimbSheetRopeState.applyIdealDirection(isoGameCharacter);
        float climbSpeed = isoGameCharacter.getClimbRopeSpeed(true);
        if (!isoGameCharacter.isLocal()) {
            climbSpeed = isoGameCharacter.get(SPEED, Float.valueOf(isoGameCharacter.getClimbRopeSpeed(true))).floatValue();
        }
        isoGameCharacter.getSpriteDef().animFrameIncrease = climbSpeed;
        int minLevel = isoGameCharacter.getCurrentSquare().getChunk().getMinLevel();
        float currentClimbHeight = isoGameCharacter.getZ() - climbSpeed / 10.0f * GameTime.instance.getMultiplier();
        currentClimbHeight = Math.max(currentClimbHeight, (float)minLevel);
        isoGameCharacter.setZ(currentClimbHeight);
        if (currentClimbHeight <= (float)climbData.targetClimbHeight) {
            this.finishClimbing(isoGameCharacter);
            return;
        }
        ClimbDownSheetRopeState.fallChanceCalculation(isoGameCharacter);
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
        isoGameCharacter.addBothArmMuscleStrain((float)(0.007 * (double)GameTime.instance.getMultiplier() * (double)(isoGameCharacter.getMoodles().getMoodleLevel(MoodleType.HEAVY_LOAD) + 1)) * ((15.0f - skillFactor) / 10.0f) * (GameTime.instance.getMultiplier() / 0.8f));
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
            isoGameCharacter.set(SPEED, Float.valueOf(isoGameCharacter.getClimbRopeSpeed(true)));
            isoGameCharacter.set(CLIMB, isoGameCharacter.isClimbing());
        }
        super.setParams(isoGameCharacter, stage);
    }

    private static void calculateClimbDown(IsoGameCharacter isoGameCharacter) {
        ClimbSheetRopeState.setIdealDirection(isoGameCharacter);
        ClimbSheetRopeState.applyIdealDirection(isoGameCharacter);
        ClimbSheetRopeState.ClimbData climbData = isoGameCharacter.getClimbData();
        int minLevel = isoGameCharacter.getCurrentSquare().getChunk().getMinLevel();
        IsoCell cell = IsoWorld.instance.getCell();
        for (int z = PZMath.fastfloor(isoGameCharacter.getZ()); z >= minLevel; --z) {
            IsoGridSquare isoGridSquare = cell.getGridSquare(isoGameCharacter.getX(), isoGameCharacter.getY(), (double)z);
            if (!isoGridSquare.has(IsoFlagType.solidtrans) && !isoGridSquare.TreatAsSolidFloor()) continue;
            climbData.targetGridSquare = isoGridSquare;
            climbData.targetClimbHeight = z;
            break;
        }
        ClimbDownSheetRopeState.fallChanceCalculation(isoGameCharacter);
    }

    private static float fallChanceCalculation(IsoGameCharacter isoGameCharacter) {
        ClimbSheetRopeState.ClimbData climbData = isoGameCharacter.getClimbData();
        climbData.fallChance = isoGameCharacter.getClimbingFailChanceFloat() + 1.0f;
        isoGameCharacter.setClimbRopeTime(isoGameCharacter.getClimbRopeTime() + GameTime.instance.getMultiplier());
        climbData.fallChance *= 300.0f;
        climbData.fallChance = climbData.fallChance / (GameTime.instance.getMultiplier() < 1.0f ? 1.0f : (float)((int)GameTime.instance.getMultiplier()));
        return climbData.fallChance;
    }

    private void finishClimbing(IsoGameCharacter isoGameCharacter) {
        ClimbSheetRopeState.ClimbData climbData = isoGameCharacter.getClimbData();
        isoGameCharacter.setZ(climbData.targetClimbHeight);
        isoGameCharacter.clear(this);
        isoGameCharacter.clearVariable("ClimbRope");
        isoGameCharacter.setCollidable(true);
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
}

