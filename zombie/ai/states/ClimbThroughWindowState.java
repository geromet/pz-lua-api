/*
 * Decompiled with CFR 0.152.
 */
package zombie.ai.states;

import fmod.fmod.FMODManager;
import java.util.Map;
import zombie.GameTime;
import zombie.SoundManager;
import zombie.UsedFromLua;
import zombie.ai.State;
import zombie.ai.states.ClimbThroughWindowPositioningParams;
import zombie.ai.states.PathFindState;
import zombie.ai.states.WalkTowardNetworkState;
import zombie.ai.states.WalkTowardState;
import zombie.audio.parameters.ParameterZombieState;
import zombie.characterTextures.BloodBodyPartType;
import zombie.characters.CharacterStat;
import zombie.characters.IsoGameCharacter;
import zombie.characters.IsoPlayer;
import zombie.characters.IsoZombie;
import zombie.characters.MoveDeltaModifiers;
import zombie.characters.skills.PerkFactory;
import zombie.core.math.PZMath;
import zombie.core.random.Rand;
import zombie.core.skinnedmodel.advancedanimation.AnimEvent;
import zombie.core.skinnedmodel.advancedanimation.AnimLayer;
import zombie.core.skinnedmodel.animation.AnimationTrack;
import zombie.iso.IsoDirections;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoMovingObject;
import zombie.iso.IsoObject;
import zombie.iso.IsoWorld;
import zombie.iso.SpriteDetails.IsoFlagType;
import zombie.iso.objects.IsoThumpable;
import zombie.iso.objects.IsoWindow;
import zombie.iso.objects.IsoWindowFrame;
import zombie.scripting.objects.MoodleType;
import zombie.util.Pool;
import zombie.util.Type;

@UsedFromLua
public final class ClimbThroughWindowState
extends State {
    private static final ClimbThroughWindowState INSTANCE = new ClimbThroughWindowState();
    public static final State.Param<ClimbThroughWindowPositioningParams> PARAMS = State.Param.ofSupplier("params", ClimbThroughWindowPositioningParams.class, ClimbThroughWindowPositioningParams::alloc);
    public static final State.Param<State> PREV_STATE = State.Param.of("prev_state", State.class);
    public static final State.Param<Boolean> ZOMBIE_ON_FLOOR = State.Param.ofBool("zombie_on_floor", false);
    public static final State.Param<String> OUTCOME = State.Param.ofString("outcome", "success");
    public static final State.Param<Boolean> SCRATCHED = State.Param.ofBool("scratched", false);

    public static ClimbThroughWindowState instance() {
        return INSTANCE;
    }

    private ClimbThroughWindowState() {
        super(true, false, true, false);
    }

    @Override
    public void enter(IsoGameCharacter owner) {
        IsoPlayer player = Type.tryCastTo(owner, IsoPlayer.class);
        owner.setIgnoreMovement(true);
        owner.setHideWeaponModel(true);
        ClimbThroughWindowPositioningParams positioningParams = owner.get(PARAMS);
        boolean isCounter = positioningParams.isCounter;
        owner.setVariable("ClimbWindowStarted", false);
        owner.setVariable("ClimbWindowEnd", false);
        owner.setVariable("ClimbWindowFinished", false);
        owner.clearVariable("ClimbWindowGetUpBack");
        owner.clearVariable("ClimbWindowGetUpFront");
        owner.setVariable("ClimbWindowOutcome", isCounter ? "obstacle" : "success");
        owner.clearVariable("ClimbWindowFlopped");
        IsoZombie zombie = Type.tryCastTo(owner, IsoZombie.class);
        if (!isCounter && zombie != null && zombie.shouldDoFenceLunge()) {
            this.setLungeXVars(zombie);
            owner.setVariable("ClimbWindowOutcome", "lunge");
        }
        if (!positioningParams.isFloor) {
            owner.setVariable("ClimbWindowOutcome", "fall");
        }
        if (!(owner instanceof IsoZombie) && positioningParams.isSheetRope) {
            owner.setVariable("ClimbWindowOutcome", "rope");
        }
        if (player != null && player.isLocalPlayer()) {
            player.dirtyRecalcGridStackTime = 20.0f;
            player.triggerMusicIntensityEvent("ClimbThroughWindow");
        }
        this.setParams(owner, State.Stage.Enter);
    }

    @Override
    public void execute(IsoGameCharacter owner) {
        float endY;
        float endX;
        ClimbThroughWindowPositioningParams positioningParams = owner.get(PARAMS);
        IsoPlayer player = Type.tryCastTo(owner, IsoPlayer.class);
        if (this.isWindowClosing(owner)) {
            return;
        }
        owner.setDir(positioningParams.climbDir);
        String climbWindowOutcome = owner.getVariableString("ClimbWindowOutcome");
        if (owner instanceof IsoZombie) {
            int oppositeZ;
            int oppositeY;
            int oppositeX;
            IsoGridSquare oppositeSq;
            boolean isDown = owner.get(ZOMBIE_ON_FLOOR);
            if (!owner.isFallOnFront() && isDown && (oppositeSq = IsoWorld.instance.currentCell.getGridSquare(oppositeX = positioningParams.oppositeX, oppositeY = positioningParams.oppositeY, oppositeZ = positioningParams.z)) != null && oppositeSq.getBrokenGlass() != null) {
                owner.addBlood(BloodBodyPartType.Head, true, true, true);
                owner.addBlood(BloodBodyPartType.Head, true, true, true);
                owner.addBlood(BloodBodyPartType.Head, true, true, true);
                owner.addBlood(BloodBodyPartType.Head, true, true, true);
                owner.addBlood(BloodBodyPartType.Head, true, true, true);
                owner.addBlood(BloodBodyPartType.Neck, true, true, true);
                owner.addBlood(BloodBodyPartType.Neck, true, true, true);
                owner.addBlood(BloodBodyPartType.Neck, true, true, true);
                owner.addBlood(BloodBodyPartType.Neck, true, true, true);
                owner.addBlood(BloodBodyPartType.Torso_Upper, true, true, true);
                owner.addBlood(BloodBodyPartType.Torso_Upper, true, true, true);
                owner.addBlood(BloodBodyPartType.Torso_Upper, true, true, true);
            }
            owner.setOnFloor(isDown);
            owner.setKnockedDown(isDown);
            owner.setFallOnFront(isDown);
        }
        if (!owner.getVariableBoolean("ClimbWindowStarted")) {
            ClimbThroughWindowState.slideCharacterToWindowOpening(owner, positioningParams);
        }
        if (owner instanceof IsoPlayer && climbWindowOutcome.equalsIgnoreCase("obstacle") && owner.DistToSquared(endX = (float)positioningParams.endX + 0.5f, endY = (float)positioningParams.endY + 0.5f) < 0.5625f) {
            owner.setVariable("ClimbWindowOutcome", "obstacleEnd");
        }
        if (!(!(owner instanceof IsoPlayer) || owner.getVariableBoolean("ClimbWindowEnd") || "fallfront".equals(climbWindowOutcome) || "back".equals(climbWindowOutcome) || "fallback".equals(climbWindowOutcome))) {
            int oppositeX = positioningParams.oppositeX;
            int oppositeY = positioningParams.oppositeY;
            int oppositeZ = positioningParams.z;
            IsoGridSquare oppositeSq = IsoWorld.instance.currentCell.getGridSquare(oppositeX, oppositeY, oppositeZ);
            if (oppositeSq != null) {
                this.checkForFallingBack(oppositeSq, owner);
                if (oppositeSq != owner.getSquare() && oppositeSq.TreatAsSolidFloor()) {
                    this.checkForFallingFront(owner.getSquare(), owner);
                }
            }
            if (owner.getMoodles().getMoodleLevel(MoodleType.DRUNK) > 1 && owner.getVariableString("ClimbWindowOutcome").equals(climbWindowOutcome) && (float)Rand.Next(2000) < owner.getStats().get(CharacterStat.INTOXICATION)) {
                if (Rand.NextBool(2)) {
                    owner.setVariable("ClimbWindowOutcome", "fallback");
                } else {
                    owner.setVariable("ClimbWindowOutcome", "fallfront");
                }
            }
        }
        if (!(!owner.getVariableBoolean("ClimbWindowStarted") || "back".equals(climbWindowOutcome) || "fallback".equals(climbWindowOutcome) || "lunge".equals(climbWindowOutcome) || "obstacle".equals(climbWindowOutcome) || "obstacleEnd".equals(climbWindowOutcome))) {
            float x = positioningParams.startX;
            float y = positioningParams.startY;
            switch (positioningParams.climbDir) {
                case N: {
                    y -= 0.1f;
                    break;
                }
                case S: {
                    y += 1.1f;
                    break;
                }
                case W: {
                    x -= 0.1f;
                    break;
                }
                case E: {
                    x += 1.1f;
                }
            }
            if (PZMath.fastfloor(owner.getX()) != PZMath.fastfloor(x) && (positioningParams.climbDir == IsoDirections.W || positioningParams.climbDir == IsoDirections.E)) {
                ClimbThroughWindowState.slideX(owner, x);
            }
            if (PZMath.fastfloor(owner.getY()) != PZMath.fastfloor(y) && (positioningParams.climbDir == IsoDirections.N || positioningParams.climbDir == IsoDirections.S)) {
                ClimbThroughWindowState.slideY(owner, y);
            }
        }
        if (owner.getVariableBoolean("ClimbWindowStarted") && positioningParams.scratch) {
            positioningParams.scratch = false;
            owner.getBodyDamage().setScratchedWindow();
            if (player != null) {
                player.playerVoiceSound("PainFromGlassCut");
            }
        }
        if (owner.getVariableBoolean("ClimbWindowStarted") && owner.isVariable("ClimbWindowOutcome", "fall")) {
            owner.setbFalling(true);
        }
    }

    public static void slideCharacterToWindowOpening(IsoGameCharacter character, ClimbThroughWindowPositioningParams positioningParams) {
        IsoDirections climbDir = positioningParams.climbDir;
        if (climbDir == IsoDirections.N || climbDir == IsoDirections.S) {
            float startX = (float)positioningParams.startX + 0.5f;
            if (character.getX() != startX) {
                ClimbThroughWindowState.slideX(character, startX);
            }
        }
        if (climbDir == IsoDirections.W || climbDir == IsoDirections.E) {
            float startY = (float)positioningParams.startY + 0.5f;
            if (character.getY() != startY) {
                ClimbThroughWindowState.slideY(character, startY);
            }
        }
    }

    private void checkForFallingBack(IsoGridSquare sq, IsoGameCharacter owner) {
        for (int i = 0; i < sq.getMovingObjects().size(); ++i) {
            IsoMovingObject movingObj = sq.getMovingObjects().get(i);
            IsoZombie zombie = Type.tryCastTo(movingObj, IsoZombie.class);
            if (zombie == null || zombie.isOnFloor() || zombie.isSitAgainstWall()) continue;
            if (zombie.isVariable("AttackOutcome", "success") || Rand.Next(5 + owner.getPerkLevel(PerkFactory.Perks.Fitness)) == 0) {
                owner.setVariable("ClimbWindowOutcome", "fallback");
                continue;
            }
            owner.setVariable("ClimbWindowOutcome", "back");
        }
    }

    private void checkForFallingFront(IsoGridSquare sq, IsoGameCharacter owner) {
        for (int i = 0; i < sq.getMovingObjects().size(); ++i) {
            IsoMovingObject movingObj = sq.getMovingObjects().get(i);
            IsoZombie zombie = Type.tryCastTo(movingObj, IsoZombie.class);
            if (zombie == null || zombie.isOnFloor() || zombie.isSitAgainstWall() || !zombie.isVariable("AttackOutcome", "success")) continue;
            owner.setVariable("ClimbWindowOutcome", "fallfront");
        }
    }

    @Override
    public void exit(IsoGameCharacter owner) {
        owner.setIgnoreMovement(false);
        owner.setHideWeaponModel(false);
        if (owner.isVariable("ClimbWindowOutcome", "fall") || owner.isVariable("ClimbWindowOutcome", "fallback") || owner.isVariable("ClimbWindowOutcome", "fallfront")) {
            owner.setHitReaction("");
        }
        owner.clearVariable("ClimbWindowFinished");
        owner.clearVariable("ClimbWindowOutcome");
        owner.clearVariable("ClimbWindowStarted");
        owner.clearVariable("ClimbWindowFlopped");
        owner.clearVariable("PlayerVoiceSound");
        if (owner instanceof IsoZombie) {
            owner.setOnFloor(false);
            owner.setKnockedDown(false);
        }
        if (owner instanceof IsoZombie) {
            IsoZombie zombie = (IsoZombie)owner;
            zombie.allowRepathDelay = 0.0f;
            State prevState = owner.get(PREV_STATE);
            if (prevState == PathFindState.instance()) {
                if (owner.getPathFindBehavior2().getTargetChar() == null) {
                    owner.setVariable("bPathFind", true);
                    owner.setVariable("bMoving", false);
                } else if (zombie.isTargetLocationKnown()) {
                    owner.pathToCharacter(owner.getPathFindBehavior2().getTargetChar());
                } else if (zombie.lastTargetSeenX != -1) {
                    owner.pathToLocation(zombie.lastTargetSeenX, zombie.lastTargetSeenY, zombie.lastTargetSeenZ);
                }
            } else if (prevState == WalkTowardState.instance() || prevState == WalkTowardNetworkState.instance()) {
                owner.setVariable("bPathFind", false);
                owner.setVariable("bMoving", true);
            }
        }
        if (owner instanceof IsoZombie) {
            IsoZombie isoZombie = (IsoZombie)owner;
            isoZombie.networkAi.isClimbing = false;
        }
        Pool.tryRelease(owner.get(PARAMS));
        owner.clear(this);
    }

    public static void slideX(IsoGameCharacter owner, float x) {
        float dx = 0.05f * GameTime.getInstance().getThirtyFPSMultiplier();
        dx = x > owner.getX() ? Math.min(dx, x - owner.getX()) : Math.max(-dx, x - owner.getX());
        owner.setX(owner.getX() + dx);
        owner.setNextX(owner.getX());
    }

    public static void slideY(IsoGameCharacter owner, float y) {
        float dy = 0.05f * GameTime.getInstance().getThirtyFPSMultiplier();
        dy = y > owner.getY() ? Math.min(dy, y - owner.getY()) : Math.max(-dy, y - owner.getY());
        owner.setY(owner.getY() + dy);
        owner.setNextY(owner.getY());
    }

    @Override
    public void animEvent(IsoGameCharacter owner, AnimLayer layer, AnimationTrack track, AnimEvent event) {
        IsoMovingObject isoMovingObject;
        IsoPlayer player = Type.tryCastTo(owner, IsoPlayer.class);
        IsoZombie zombie = Type.tryCastTo(owner, IsoZombie.class);
        if (event.eventName.equalsIgnoreCase("CheckAttack") && zombie != null && (isoMovingObject = zombie.target) instanceof IsoGameCharacter) {
            IsoGameCharacter isoGameCharacter = (IsoGameCharacter)isoMovingObject;
            isoGameCharacter.attackFromWindowsLunge(zombie);
        }
        if (event.eventName.equalsIgnoreCase("OnFloor") && zombie != null) {
            boolean bOnFloor = Boolean.parseBoolean(event.parameterValue);
            owner.set(ZOMBIE_ON_FLOOR, bOnFloor);
            if (bOnFloor) {
                this.setLungeXVars(zombie);
                IsoThumpable windows = Type.tryCastTo(this.getWindow(owner), IsoThumpable.class);
                if (windows != null && windows.getSquare() != null && zombie.target != null) {
                    windows.health -= Rand.Next(10, 20);
                    if (windows.health <= 0) {
                        windows.destroy();
                    }
                }
                owner.setVariable("ClimbWindowFlopped", true);
            }
        }
        if (event.eventName.equalsIgnoreCase("PlayerVoiceSound")) {
            if (owner.getVariableBoolean("PlayerVoiceSound")) {
                return;
            }
            if (player == null) {
                return;
            }
            owner.setVariable("PlayerVoiceSound", true);
            player.playerVoiceSound(event.parameterValue);
        }
        if (event.eventName.equalsIgnoreCase("PlayWindowSound")) {
            if (!SoundManager.instance.isListenerInRange(owner.getX(), owner.getY(), 10.0f)) {
                return;
            }
            long instance = owner.getEmitter().playSoundImpl(event.parameterValue, null);
            owner.getEmitter().setParameterValue(instance, FMODManager.instance.getParameterDescription("TripObstacleType"), 9.0f);
        }
        if (event.eventName.equalsIgnoreCase("SetState")) {
            if (zombie == null) {
                return;
            }
            try {
                ParameterZombieState.State state = ParameterZombieState.State.valueOf(event.parameterValue);
                zombie.parameterZombieState.setState(state);
            }
            catch (IllegalArgumentException illegalArgumentException) {
                // empty catch block
            }
        }
    }

    @Override
    public boolean isIgnoreCollide(IsoGameCharacter owner, int fromX, int fromY, int fromZ, int toX, int toY, int toZ) {
        ClimbThroughWindowPositioningParams positioningParams = owner.get(PARAMS);
        int startX = positioningParams.startX;
        int startY = positioningParams.startY;
        int endX = positioningParams.endX;
        int endY = positioningParams.endY;
        int z = positioningParams.z;
        if (z != fromZ || z != toZ) {
            return false;
        }
        int x1 = PZMath.min(startX, endX);
        int y1 = PZMath.min(startY, endY);
        int x2 = PZMath.max(startX, endX);
        int y2 = PZMath.max(startY, endY);
        int x3 = PZMath.min(fromX, toX);
        int y3 = PZMath.min(fromY, toY);
        int x4 = PZMath.max(fromX, toX);
        int y4 = PZMath.max(fromY, toY);
        return x1 <= x3 && y1 <= y3 && x2 >= x4 && y2 >= y4;
    }

    public IsoObject getWindow(IsoGameCharacter owner) {
        if (!owner.isCurrentState(this)) {
            return null;
        }
        ClimbThroughWindowPositioningParams positioningParams = owner.get(PARAMS);
        int startX = positioningParams.startX;
        int startY = positioningParams.startY;
        int z = positioningParams.z;
        IsoGridSquare startSq = IsoWorld.instance.currentCell.getGridSquare(startX, startY, z);
        int endX = positioningParams.endX;
        int endY = positioningParams.endY;
        IsoGridSquare endSq = IsoWorld.instance.currentCell.getGridSquare(endX, endY, z);
        if (startSq == null || endSq == null) {
            return null;
        }
        IsoObject obj = startSq.getWindowTo(endSq);
        if (obj == null) {
            obj = startSq.getWindowThumpableTo(endSq);
        }
        if (obj == null) {
            obj = startSq.getHoppableTo(endSq);
        }
        return obj;
    }

    public boolean isWindowClosing(IsoGameCharacter owner) {
        if (owner.getVariableBoolean("ClimbWindowStarted")) {
            return false;
        }
        ClimbThroughWindowPositioningParams positioningParams = owner.get(PARAMS);
        int startX = positioningParams.startX;
        int startY = positioningParams.startY;
        int z = positioningParams.z;
        IsoGridSquare startSq = IsoWorld.instance.currentCell.getGridSquare(startX, startY, z);
        if (owner.getCurrentSquare() != startSq) {
            return false;
        }
        IsoObject isoObject = this.getWindow(owner);
        if (!(isoObject instanceof IsoWindow)) {
            return false;
        }
        IsoWindow window = (IsoWindow)isoObject;
        IsoGameCharacter chrClosing = window.getFirstCharacterClosing();
        if (chrClosing == null || !chrClosing.isVariable("CloseWindowOutcome", "success")) {
            return false;
        }
        if (owner.isZombie()) {
            owner.setHitReaction("HeadLeft");
        } else {
            owner.setVariable("ClimbWindowFinished", true);
        }
        return true;
    }

    @Override
    public void getDeltaModifiers(IsoGameCharacter owner, MoveDeltaModifiers modifiers) {
        boolean hasPath = owner.getPath2() != null;
        boolean isPlayer = owner instanceof IsoPlayer;
        if (hasPath && isPlayer) {
            modifiers.setMaxTurnDelta(2.0f);
        }
        if (isPlayer && owner.getVariableBoolean("isTurning")) {
            modifiers.setMaxTurnDelta(2.0f);
        }
    }

    public static boolean isFreeSquare(IsoGridSquare square) {
        return square != null && square.TreatAsSolidFloor() && !square.has(IsoFlagType.solid) && !square.has(IsoFlagType.solidtrans);
    }

    public static boolean isObstacleSquare(IsoGridSquare square) {
        return square != null && square.TreatAsSolidFloor() && !square.has(IsoFlagType.solid) && square.has(IsoFlagType.solidtrans) && !square.has(IsoFlagType.water);
    }

    public static IsoGridSquare getFreeSquareAfterObstacles(IsoGridSquare square, IsoDirections dir) {
        IsoGridSquare square1;
        while ((square1 = square.getAdjacentSquare(dir)) != null && !square.isSomethingTo(square1) && square.getWindowFrameTo(square1) == null && square.getWindowThumpableTo(square1) == null) {
            if (ClimbThroughWindowState.isFreeSquare(square1)) {
                return square1;
            }
            if (!ClimbThroughWindowState.isObstacleSquare(square1)) {
                return null;
            }
            square = square1;
        }
        return null;
    }

    private void setLungeXVars(IsoZombie zombie) {
        IsoMovingObject target = zombie.getTarget();
        if (target == null) {
            return;
        }
        zombie.setVariable("FenceLungeX", 0.0f);
        zombie.setVariable("FenceLungeY", 0.0f);
        float lungeX = 0.0f;
        float forwardX = zombie.getForwardDirectionX();
        float forwardY = zombie.getForwardDirectionY();
        PZMath.SideOfLine side = PZMath.testSideOfLine(zombie.getX(), zombie.getY(), zombie.getX() + forwardX, zombie.getY() + forwardY, target.getX(), target.getY());
        float angleRad = (float)Math.acos(zombie.getDotWithForwardDirection(target.getX(), target.getY()));
        float angleDeg = PZMath.clamp(PZMath.radToDeg(angleRad), 0.0f, 90.0f);
        switch (side) {
            case Left: {
                lungeX = -angleDeg / 90.0f;
                break;
            }
            case OnLine: {
                lungeX = 0.0f;
                break;
            }
            case Right: {
                lungeX = angleDeg / 90.0f;
            }
        }
        zombie.setVariable("FenceLungeX", lungeX);
    }

    public boolean isPastInnerEdgeOfSquare(IsoGameCharacter owner, int x, int y, IsoDirections moveDir) {
        if (moveDir == IsoDirections.N) {
            return owner.getY() < (float)(y + 1) - 0.3f;
        }
        if (moveDir == IsoDirections.S) {
            return owner.getY() > (float)y + 0.3f;
        }
        if (moveDir == IsoDirections.W) {
            return owner.getX() < (float)(x + 1) - 0.3f;
        }
        if (moveDir == IsoDirections.E) {
            return owner.getX() > (float)x + 0.3f;
        }
        throw new IllegalArgumentException("unhandled direction");
    }

    public boolean isPastOuterEdgeOfSquare(IsoGameCharacter owner, int x, int y, IsoDirections moveDir) {
        if (moveDir == IsoDirections.N) {
            return owner.getY() < (float)y - 0.3f;
        }
        if (moveDir == IsoDirections.S) {
            return owner.getY() > (float)(y + 1) + 0.3f;
        }
        if (moveDir == IsoDirections.W) {
            return owner.getX() < (float)x - 0.3f;
        }
        if (moveDir == IsoDirections.E) {
            return owner.getX() > (float)(x + 1) + 0.3f;
        }
        throw new IllegalArgumentException("unhandled direction");
    }

    public void setParams(IsoGameCharacter owner, IsoObject obj) {
        ClimbThroughWindowPositioningParams climbParams = owner.get(PARAMS);
        ClimbThroughWindowState.getClimbThroughWindowPositioningParams(owner, obj, climbParams);
        if (climbParams.windowObject == null) {
            throw new IllegalArgumentException("No valid climb-throuwh portal found. Expected thumpable, window, or window-frame");
        }
        owner.set(ZOMBIE_ON_FLOOR, false);
        owner.set(PREV_STATE, climbParams.climbingCharacter.getCurrentState());
        owner.set(SCRATCHED, climbParams.scratch);
    }

    public static void getClimbThroughWindowPositioningParams(IsoGameCharacter climbingCharacter, IsoObject windowObject, ClimbThroughWindowPositioningParams climbParams) {
        IsoDirections dir;
        boolean north;
        boolean scratch = false;
        if (windowObject instanceof IsoWindow) {
            IsoWindow window = (IsoWindow)windowObject;
            climbParams.canClimb = window.canClimbThrough(climbingCharacter);
            north = window.isNorth();
            if (climbingCharacter instanceof IsoPlayer && window.isDestroyed() && !window.isGlassRemoved() && Rand.Next(2) == 0) {
                scratch = true;
            }
        } else if (windowObject instanceof IsoThumpable) {
            IsoThumpable thumpable = (IsoThumpable)windowObject;
            climbParams.canClimb = thumpable.canClimbThrough(climbingCharacter);
            north = thumpable.north;
            if (climbingCharacter instanceof IsoPlayer && thumpable.getName().equals("Barbed Fence") && Rand.Next(101) > 75) {
                scratch = true;
            }
        } else if (windowObject instanceof IsoWindowFrame) {
            IsoWindowFrame windowFrame = (IsoWindowFrame)windowObject;
            climbParams.canClimb = true;
            north = windowFrame.getNorth();
        } else {
            climbParams.canClimb = false;
            climbParams.climbingCharacter = climbingCharacter;
            climbParams.windowObject = null;
            return;
        }
        int x = windowObject.getSquare().getX();
        int y = windowObject.getSquare().getY();
        int z = windowObject.getSquare().getZ();
        int startX = x;
        int startY = y;
        int oppositeX = x;
        int oppositeY = y;
        if (north) {
            if ((float)y < climbingCharacter.getY()) {
                --oppositeY;
                dir = IsoDirections.N;
            } else {
                --startY;
                dir = IsoDirections.S;
            }
        } else if ((float)x < climbingCharacter.getX()) {
            --oppositeX;
            dir = IsoDirections.W;
        } else {
            --startX;
            dir = IsoDirections.E;
        }
        IsoGridSquare oppositeSq = IsoWorld.instance.currentCell.getGridSquare(oppositeX, oppositeY, z);
        boolean isCounter = oppositeSq != null && oppositeSq.has(IsoFlagType.solidtrans);
        boolean isFloor = oppositeSq != null && oppositeSq.TreatAsSolidFloor();
        boolean isSheetRope = oppositeSq != null && climbingCharacter.canClimbDownSheetRope(oppositeSq);
        int endX = oppositeX;
        int endY = oppositeY;
        if (isCounter && climbingCharacter.isZombie()) {
            IsoGridSquare square = oppositeSq.getAdjacentSquare(dir);
            if (!ClimbThroughWindowState.isFreeSquare(square) || oppositeSq.isSomethingTo(square) || oppositeSq.getWindowFrameTo(square) != null || oppositeSq.getWindowThumpableTo(square) != null) {
                isCounter = false;
            } else {
                endX = square.x;
                endY = square.y;
            }
        }
        if (isCounter && !climbingCharacter.isZombie()) {
            IsoGridSquare freeSq = ClimbThroughWindowState.getFreeSquareAfterObstacles(oppositeSq, dir);
            if (freeSq == null) {
                isCounter = false;
            } else {
                endX = freeSq.x;
                endY = freeSq.y;
            }
        }
        climbParams.climbDir = dir;
        climbParams.climbingCharacter = climbingCharacter;
        climbParams.windowObject = windowObject;
        climbParams.startX = startX;
        climbParams.startY = startY;
        climbParams.z = z;
        climbParams.oppositeX = oppositeX;
        climbParams.oppositeY = oppositeY;
        climbParams.endX = endX;
        climbParams.endY = endY;
        climbParams.scratch = scratch;
        climbParams.isCounter = isCounter;
        climbParams.isFloor = isFloor;
        climbParams.isSheetRope = isSheetRope;
    }

    public ClimbThroughWindowPositioningParams getPositioningParams(IsoGameCharacter owner) {
        return owner.get(PARAMS);
    }

    @Override
    public void setParams(IsoGameCharacter owner, State.Stage stage) {
        if (owner.isLocal()) {
            owner.set(OUTCOME, owner.getVariableString("ClimbWindowOutcome"));
        } else {
            owner.setVariable("ClimbWindowOutcome", owner.get(OUTCOME));
        }
        super.setParams(owner, stage);
    }

    @Override
    public boolean isProcessedOnEnter() {
        return true;
    }

    @Override
    public void processOnEnter(IsoGameCharacter owner, Map<Object, Object> delegate) {
        if (SCRATCHED.fromDelegate(delegate).booleanValue()) {
            owner.getBodyDamage().setScratchedWindow();
        }
    }

    @Override
    public boolean canRagdoll(IsoGameCharacter owner) {
        IsoZombie ownerZombie;
        if (owner.getVariableBoolean("ClimbWindowStarted", false)) {
            return false;
        }
        return !(owner instanceof IsoZombie) || !(ownerZombie = (IsoZombie)owner).isOnFloor();
    }
}

