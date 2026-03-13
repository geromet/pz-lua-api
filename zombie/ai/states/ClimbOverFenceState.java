/*
 * Decompiled with CFR 0.152.
 */
package zombie.ai.states;

import fmod.fmod.FMODManager;
import java.util.Map;
import se.krka.kahlua.vm.KahluaTableIterator;
import zombie.GameTime;
import zombie.SoundManager;
import zombie.UsedFromLua;
import zombie.ZomboidGlobals;
import zombie.ai.State;
import zombie.ai.states.PathFindState;
import zombie.ai.states.WalkTowardNetworkState;
import zombie.ai.states.WalkTowardState;
import zombie.audio.parameters.ParameterCharacterMovementSpeed;
import zombie.audio.parameters.ParameterZombieState;
import zombie.characters.BodyDamage.BodyPart;
import zombie.characters.BodyDamage.BodyPartType;
import zombie.characters.CharacterStat;
import zombie.characters.IsoGameCharacter;
import zombie.characters.IsoPlayer;
import zombie.characters.IsoZombie;
import zombie.characters.MoveDeltaModifiers;
import zombie.characters.skills.PerkFactory;
import zombie.core.math.PZMath;
import zombie.core.properties.PropertyContainer;
import zombie.core.random.Rand;
import zombie.core.skinnedmodel.advancedanimation.AnimEvent;
import zombie.debug.DebugOptions;
import zombie.iso.IsoDirections;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoMovingObject;
import zombie.iso.IsoObject;
import zombie.iso.IsoWorld;
import zombie.iso.SpriteDetails.IsoFlagType;
import zombie.iso.objects.IsoThumpable;
import zombie.scripting.objects.CharacterTrait;
import zombie.scripting.objects.MoodleType;
import zombie.util.StringUtils;
import zombie.util.Type;

@UsedFromLua
public final class ClimbOverFenceState
extends State {
    private static final ClimbOverFenceState INSTANCE = new ClimbOverFenceState();
    public static final State.Param<Integer> START_X = State.Param.ofInt("start_x", 0);
    public static final State.Param<Integer> START_Y = State.Param.ofInt("start_y", 0);
    public static final State.Param<Integer> Z = State.Param.ofInt("z", 0);
    public static final State.Param<Integer> END_X = State.Param.ofInt("end_x", 0);
    public static final State.Param<Integer> END_Y = State.Param.ofInt("end_y", 0);
    public static final State.Param<IsoDirections> DIR = State.Param.of("dir", IsoDirections.class);
    public static final State.Param<Boolean> ZOMBIE_ON_FLOOR = State.Param.ofBool("zombie_on_floor", false);
    public static final State.Param<State> PREV_STATE = State.Param.of("prev_state", State.class);
    public static final State.Param<Boolean> SCRATCH = State.Param.ofBool("scratch", false);
    public static final State.Param<Boolean> COUNTER = State.Param.ofBool("counter", false);
    public static final State.Param<Boolean> SOLID_FLOOR = State.Param.ofBool("solid_floor", false);
    public static final State.Param<Boolean> SHEET_ROPE = State.Param.ofBool("sheet_rope", false);
    public static final State.Param<Boolean> RUN = State.Param.ofBool("run", false);
    public static final State.Param<Boolean> SPRINT = State.Param.ofBool("sprint", false);
    public static final State.Param<Boolean> COLLIDABLE = State.Param.ofBool("collidable", false);
    public static final State.Param<String> OUTCOME = State.Param.ofString("outcome", "success");
    static final int FENCE_TYPE_WOOD = 0;
    static final int FENCE_TYPE_METAL = 1;
    static final int FENCE_TYPE_SANDBAG = 2;
    static final int FENCE_TYPE_GRAVELBAG = 3;
    static final int FENCE_TYPE_BARBWIRE = 4;
    static final int FENCE_TYPE_ROADBLOCK = 5;
    static final int FENCE_TYPE_METAL_BARS = 6;
    static final int TRIP_WOOD = 0;
    static final int TRIP_METAL = 1;
    static final int TRIP_SANDBAG = 2;
    static final int TRIP_GRAVELBAG = 3;
    static final int TRIP_BARBWIRE = 4;
    public static final int TRIP_TREE = 5;
    public static final int TRIP_ZOMBIE = 6;
    public static final int COLLIDE_WITH_WALL = 7;
    public static final int TRIP_METAL_BARS = 8;
    public static final int TRIP_WINDOW = 9;

    public static ClimbOverFenceState instance() {
        return INSTANCE;
    }

    private ClimbOverFenceState() {
        super(true, false, true, false);
        this.addAnimEventListener("CheckAttack", this::OnAnimEvent_CheckAttack);
        this.addAnimEventListener("VaultSprintFallLanded", this::OnAnimEvent_VaultSprintFallLanded);
        this.addAnimEventListener("FallenOnKnees", this::OnAnimEvent_FallenOnKnees);
        this.addAnimEventListener("OnFloor", this::OnAnimEvent_OnFloor);
        this.addAnimEventListener("PlayFenceSound", this::OnAnimEvent_PlayFenceSound);
        this.addAnimEventListener("PlayerVoiceSound", this::OnAnimEvent_PlayerVoiceSound);
        this.addAnimEventListener("PlayTripSound", this::OnAnimEvent_PlayTripSound);
        this.addAnimEventListener("SetCollidable", this::OnAnimEvent_SetCollidable);
        this.addAnimEventListener("SetState", this::OnAnimEvent_SetState);
        this.addAnimEventListener("VaultOverStarted", this::OnAnimEvent_VaultOverStarted);
    }

    @Override
    public void enter(IsoGameCharacter owner) {
        IsoPlayer player = Type.tryCastTo(owner, IsoPlayer.class);
        owner.setVariable("FenceLungeX", 0.0f);
        owner.setVariable("FenceLungeY", 0.0f);
        owner.setIgnoreMovement(true);
        if (owner.get(RUN).booleanValue()) {
            owner.setVariable("VaultOverRun", true);
            owner.getStats().remove(CharacterStat.ENDURANCE, (float)(ZomboidGlobals.runningEnduranceReduce * 300.0));
        } else if (owner.get(SPRINT).booleanValue()) {
            owner.setVariable("VaultOverSprint", true);
            owner.getStats().remove(CharacterStat.ENDURANCE, (float)(ZomboidGlobals.runningEnduranceReduce * 700.0));
        }
        boolean isCounter = owner.get(COUNTER);
        owner.setVariable("ClimbingFence", true);
        owner.setVariable("ClimbFenceStarted", false);
        owner.setVariable("ClimbFenceFinished", false);
        owner.setVariable("ClimbFenceOutcome", isCounter ? "obstacle" : "success");
        owner.clearVariable("ClimbFenceFlopped");
        if ((owner.getVariableBoolean("VaultOverRun") || owner.getVariableBoolean("VaultOverSprint")) && this.shouldFallAfterVaultOver(owner)) {
            owner.setVariable("ClimbFenceOutcome", "fall");
        }
        IsoZombie zombie = Type.tryCastTo(owner, IsoZombie.class);
        if (!isCounter && zombie != null && zombie.shouldDoFenceLunge()) {
            owner.setVariable("ClimbFenceOutcome", "lunge");
            this.setLungeXVars(zombie);
        }
        if (!owner.get(SOLID_FLOOR).booleanValue()) {
            owner.setVariable("ClimbFenceOutcome", "falling");
        }
        if (!(owner instanceof IsoZombie) && owner.get(SHEET_ROPE).booleanValue()) {
            owner.setVariable("ClimbFenceOutcome", "rope");
        }
        if (player != null && player.isLocalPlayer()) {
            player.dirtyRecalcGridStackTime = 20.0f;
            player.triggerMusicIntensityEvent("HopFence");
        }
        if (owner.isLocal()) {
            owner.set(OUTCOME, owner.getVariableString("ClimbFenceOutcome"));
        } else {
            owner.setVariable("ClimbFenceOutcome", owner.get(OUTCOME));
        }
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

    @Override
    public void execute(IsoGameCharacter owner) {
        IsoDirections dir = owner.get(DIR);
        int endX = owner.get(END_X);
        int endY = owner.get(END_Y);
        owner.setAnimated(true);
        if (dir == IsoDirections.N) {
            owner.setDir(IsoDirections.N);
        } else if (dir == IsoDirections.S) {
            owner.setDir(IsoDirections.S);
        } else if (dir == IsoDirections.W) {
            owner.setDir(IsoDirections.W);
        } else if (dir == IsoDirections.E) {
            owner.setDir(IsoDirections.E);
        }
        String climbFenceOutcome = owner.getVariableString("ClimbFenceOutcome");
        if (!"lunge".equals(climbFenceOutcome)) {
            float dxy = 0.05f;
            if (dir == IsoDirections.N || dir == IsoDirections.S) {
                owner.setX(owner.setNextX(PZMath.clamp(owner.getX(), (float)endX + 0.05f, (float)(endX + 1) - 0.05f)));
            } else if (dir == IsoDirections.W || dir == IsoDirections.E) {
                owner.setY(owner.setNextY(PZMath.clamp(owner.getY(), (float)endY + 0.05f, (float)(endY + 1) - 0.05f)));
            }
        }
        if (!(!owner.getVariableBoolean("ClimbFenceStarted") || "back".equals(climbFenceOutcome) || "fallback".equals(climbFenceOutcome) || "lunge".equalsIgnoreCase(climbFenceOutcome) || "obstacle".equals(climbFenceOutcome) || "obstacleEnd".equals(climbFenceOutcome))) {
            float x = owner.get(START_X).intValue();
            float y = owner.get(START_Y).intValue();
            switch (dir) {
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
            if (PZMath.fastfloor(owner.getX()) != PZMath.fastfloor(x) && (dir == IsoDirections.W || dir == IsoDirections.E)) {
                this.slideX(owner, x);
            }
            if (PZMath.fastfloor(owner.getY()) != PZMath.fastfloor(y) && (dir == IsoDirections.N || dir == IsoDirections.S)) {
                this.slideY(owner, y);
            }
        }
        if (owner instanceof IsoZombie) {
            boolean isDown = owner.get(ZOMBIE_ON_FLOOR);
            owner.setOnFloor(isDown);
            owner.setKnockedDown(isDown);
            owner.setFallOnFront(isDown);
        }
        if (owner.getVariableBoolean("ClimbFenceStarted") && owner.isVariable("ClimbFenceOutcome", "fall")) {
            owner.setbFalling(true);
        }
    }

    @Override
    public void exit(IsoGameCharacter owner) {
        IsoPlayer player = Type.tryCastTo(owner, IsoPlayer.class);
        if (player != null && "fall".equals(owner.getVariableString("ClimbFenceOutcome"))) {
            owner.setSprinting(false);
        }
        owner.clearVariable("ClimbingFence");
        owner.clearVariable("ClimbFenceFinished");
        owner.clearVariable("ClimbFenceOutcome");
        owner.clearVariable("ClimbFenceStarted");
        owner.clearVariable("ClimbFenceFlopped");
        owner.clearVariable("PlayerVoiceSound");
        owner.ClearVariable("VaultOverSprint");
        owner.ClearVariable("VaultOverRun");
        owner.setIgnoreMovement(false);
        IsoZombie zombie = Type.tryCastTo(owner, IsoZombie.class);
        if (zombie != null) {
            zombie.allowRepathDelay = 0.0f;
            State prevState = owner.get(PREV_STATE);
            if (prevState == PathFindState.instance()) {
                if (owner.getPathFindBehavior2().getTargetChar() == null) {
                    owner.setVariable("bPathfind", true);
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
        if (zombie != null) {
            zombie.networkAi.isClimbing = false;
        }
    }

    private void OnAnimEvent_VaultOverStarted(IsoGameCharacter owner) {
        if (owner.isVariable("ClimbFenceOutcome", "fall")) {
            owner.reportEvent("EventFallClimb");
            owner.setVariable("BumpDone", true);
            owner.setFallOnFront(true);
        }
    }

    private void OnAnimEvent_SetState(IsoGameCharacter owner, AnimEvent event) {
        IsoPlayer player = Type.tryCastTo(owner, IsoPlayer.class);
        IsoZombie zombie = Type.tryCastTo(owner, IsoZombie.class);
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

    private void OnAnimEvent_SetCollidable(IsoGameCharacter owner, AnimEvent event) {
        owner.set(COLLIDABLE, Boolean.parseBoolean(event.parameterValue));
    }

    private void OnAnimEvent_PlayTripSound(IsoGameCharacter owner, AnimEvent event) {
        if (!SoundManager.instance.isListenerInRange(owner.getX(), owner.getY(), 10.0f)) {
            return;
        }
        IsoObject fence = this.getFence(owner);
        if (fence == null) {
            return;
        }
        int tripType = this.getTripType(fence);
        long instance = owner.getEmitter().playSoundImpl(event.parameterValue, null);
        ParameterCharacterMovementSpeed parameter = ((IsoPlayer)owner).getParameterCharacterMovementSpeed();
        owner.getEmitter().setParameterValue(instance, parameter.getParameterDescription(), parameter.calculateCurrentValue());
        owner.getEmitter().setParameterValue(instance, FMODManager.instance.getParameterDescription("TripObstacleType"), tripType);
    }

    private void OnAnimEvent_PlayerVoiceSound(IsoGameCharacter owner, AnimEvent event) {
        IsoPlayer player = Type.tryCastTo(owner, IsoPlayer.class);
        if (owner.getVariableBoolean("PlayerVoiceSound")) {
            return;
        }
        if (player == null) {
            return;
        }
        owner.setVariable("PlayerVoiceSound", true);
        player.playerVoiceSound(event.parameterValue);
    }

    private void OnAnimEvent_PlayFenceSound(IsoGameCharacter owner, AnimEvent event) {
        if (!SoundManager.instance.isListenerInRange(owner.getX(), owner.getY(), 10.0f)) {
            return;
        }
        IsoObject fence = this.getFence(owner);
        if (fence == null) {
            return;
        }
        if (owner instanceof IsoZombie) {
            long instance = owner.getEmitter().playSoundImpl(event.parameterValue, null);
            int tripType = this.getTripType(fence);
            owner.getEmitter().setParameterValue(instance, FMODManager.instance.getParameterDescription("TripObstacleType"), tripType);
            return;
        }
        int fenceType = this.getFenceType(fence);
        long instance = owner.getEmitter().playSoundImpl(event.parameterValue, null);
        if (owner instanceof IsoPlayer) {
            IsoPlayer isoPlayer = (IsoPlayer)owner;
            ParameterCharacterMovementSpeed parameter = isoPlayer.getParameterCharacterMovementSpeed();
            owner.getEmitter().setParameterValue(instance, parameter.getParameterDescription(), parameter.calculateCurrentValue());
        }
        owner.getEmitter().setParameterValue(instance, FMODManager.instance.getParameterDescription("FenceTypeLow"), fenceType);
    }

    private void OnAnimEvent_OnFloor(IsoGameCharacter owner, AnimEvent event) {
        owner.set(ZOMBIE_ON_FLOOR, Boolean.parseBoolean(event.parameterValue));
        if (Boolean.parseBoolean(event.parameterValue)) {
            this.setLungeXVars((IsoZombie)owner);
            IsoObject fence = this.getFence(owner);
            if (this.countZombiesClimbingOver(fence) >= 2) {
                fence.damage = (short)(fence.damage - Rand.Next(7, 12) / (this.isMetalFence(fence) ? 2 : 1));
                if (fence.damage <= 0) {
                    fence.destroyFence(owner.get(DIR));
                }
            }
            owner.setVariable("ClimbFenceFlopped", true);
        }
    }

    private void OnAnimEvent_FallenOnKnees(IsoGameCharacter owner) {
        owner.fallenOnKnees();
    }

    private void OnAnimEvent_VaultSprintFallLanded(IsoGameCharacter owner) {
        owner.dropHandItems();
        owner.fallenOnKnees();
    }

    private void OnAnimEvent_CheckAttack(IsoGameCharacter owner) {
        IsoMovingObject isoMovingObject;
        IsoZombie zombie = Type.tryCastTo(owner, IsoZombie.class);
        if (zombie != null && (isoMovingObject = zombie.target) instanceof IsoGameCharacter) {
            IsoGameCharacter isoGameCharacter = (IsoGameCharacter)isoMovingObject;
            isoGameCharacter.attackFromWindowsLunge(zombie);
        }
    }

    @Override
    public void getDeltaModifiers(IsoGameCharacter owner, MoveDeltaModifiers modifiers) {
        boolean hasPath = owner.getPath2() != null;
        boolean isPlayer = owner instanceof IsoPlayer;
        if (hasPath && isPlayer) {
            modifiers.setMaxTurnDelta(2.0f);
        }
    }

    @Override
    public boolean isIgnoreCollide(IsoGameCharacter owner, int fromX, int fromY, int fromZ, int toX, int toY, int toZ) {
        int startX = owner.get(START_X);
        int startY = owner.get(START_Y);
        int endX = owner.get(END_X);
        int endY = owner.get(END_Y);
        int z = owner.get(Z);
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

    private void slideX(IsoGameCharacter owner, float x) {
        float dx = 0.05f * GameTime.getInstance().getThirtyFPSMultiplier();
        dx = x > owner.getX() ? Math.min(dx, x - owner.getX()) : Math.max(-dx, x - owner.getX());
        owner.setX(owner.getX() + dx);
        owner.setNextX(owner.getX());
    }

    private void slideY(IsoGameCharacter owner, float y) {
        float dy = 0.05f * GameTime.getInstance().getThirtyFPSMultiplier();
        dy = y > owner.getY() ? Math.min(dy, y - owner.getY()) : Math.max(-dy, y - owner.getY());
        owner.setY(owner.getY() + dy);
        owner.setNextY(owner.getY());
    }

    private IsoObject getFence(IsoGameCharacter owner) {
        int startX = owner.get(START_X);
        int startY = owner.get(START_Y);
        int z = owner.get(Z);
        IsoGridSquare startSq = IsoWorld.instance.currentCell.getGridSquare(startX, startY, z);
        int endX = owner.get(END_X);
        int endY = owner.get(END_Y);
        IsoGridSquare endSq = IsoWorld.instance.currentCell.getGridSquare(endX, endY, z);
        if (startSq == null || endSq == null) {
            return null;
        }
        return startSq.getHoppableTo(endSq);
    }

    private int getFenceType(IsoObject fence) {
        if (fence.getSprite() == null) {
            return 0;
        }
        PropertyContainer props = fence.getSprite().getProperties();
        String typeStr = props.get("FenceTypeLow");
        if (typeStr != null) {
            if ("Sandbag".equals(typeStr) && fence.getName() != null && StringUtils.containsIgnoreCase(fence.getName(), "Gravel")) {
                typeStr = "Gravelbag";
            }
            return switch (typeStr) {
                case "Wood" -> 0;
                case "Metal" -> 1;
                case "Sandbag" -> 2;
                case "Gravelbag" -> 3;
                case "Barbwire" -> 4;
                case "RoadBlock" -> 5;
                case "MetalGate" -> 6;
                default -> 0;
            };
        }
        typeStr = props.get("FenceTypeHigh");
        if (typeStr != null) {
            return switch (typeStr) {
                case "Wood" -> 0;
                case "Metal" -> 1;
                case "MetalBars" -> 8;
                default -> 0;
            };
        }
        return 0;
    }

    private int getTripType(IsoObject fence) {
        if (fence.getSprite() == null) {
            return 0;
        }
        PropertyContainer props = fence.getSprite().getProperties();
        String typeStr = props.get("FenceTypeLow");
        if (typeStr != null) {
            if ("Sandbag".equals(typeStr) && fence.getName() != null && StringUtils.containsIgnoreCase(fence.getName(), "Gravel")) {
                typeStr = "Gravelbag";
            }
            return switch (typeStr) {
                case "Wood" -> 0;
                case "Metal" -> 1;
                case "Sandbag" -> 2;
                case "Gravelbag" -> 3;
                case "Barbwire" -> 4;
                case "MetalGate" -> 8;
                default -> 0;
            };
        }
        typeStr = props.get("FenceTypeHigh");
        if (typeStr != null) {
            return switch (typeStr) {
                case "Wood" -> 0;
                case "Metal" -> 1;
                case "MetalBars" -> 8;
                default -> 0;
            };
        }
        return 0;
    }

    private boolean shouldFallAfterVaultOver(IsoGameCharacter owner) {
        BodyPart part;
        if (DebugOptions.instance.character.debug.alwaysTripOverFence.getValue()) {
            return true;
        }
        float chance = 0.0f;
        if (owner.getVariableBoolean("VaultOverSprint")) {
            chance = 10.0f;
        }
        if (owner.getMoodles() != null) {
            chance += (float)(owner.getMoodles().getMoodleLevel(MoodleType.ENDURANCE) * 10);
            chance += (float)(owner.getMoodles().getMoodleLevel(MoodleType.DRUNK) * 10);
            chance += (float)(owner.getMoodles().getMoodleLevel(MoodleType.HEAVY_LOAD) * 13);
            chance += (float)(owner.getMoodles().getMoodleLevel(MoodleType.PAIN) * 5);
        }
        if ((part = owner.getBodyDamage().getBodyPart(BodyPartType.Torso_Lower)).getAdditionalPain(true) > 20.0f) {
            chance += (part.getAdditionalPain(true) - 20.0f) / 10.0f;
        }
        if (owner.hasTrait(CharacterTrait.CLUMSY)) {
            chance += 10.0f;
        }
        if (owner.hasTrait(CharacterTrait.GRACEFUL)) {
            chance -= 10.0f;
        }
        if (owner.hasTrait(CharacterTrait.VERY_UNDERWEIGHT)) {
            chance += 20.0f;
        }
        if (owner.hasTrait(CharacterTrait.VERY_UNDERWEIGHT)) {
            chance += 10.0f;
        }
        if (owner.hasTrait(CharacterTrait.OBESE)) {
            chance += 20.0f;
        }
        if (owner.hasTrait(CharacterTrait.OVERWEIGHT)) {
            chance += 10.0f;
        }
        return (float)Rand.Next(100) < (chance -= (float)owner.getPerkLevel(PerkFactory.Perks.Fitness));
    }

    private int countZombiesClimbingOver(IsoObject fence) {
        if (fence == null || fence.getSquare() == null) {
            return 0;
        }
        int count = 0;
        IsoGridSquare square = fence.getSquare();
        count += this.countZombiesClimbingOver(fence, square);
        square = fence.getProperties().has(IsoFlagType.HoppableN) ? square.getAdjacentSquare(IsoDirections.N) : square.getAdjacentSquare(IsoDirections.W);
        return count += this.countZombiesClimbingOver(fence, square);
    }

    private int countZombiesClimbingOver(IsoObject fence, IsoGridSquare square) {
        if (square == null) {
            return 0;
        }
        int count = 0;
        for (int i = 0; i < square.getMovingObjects().size(); ++i) {
            IsoZombie zombie = Type.tryCastTo(square.getMovingObjects().get(i), IsoZombie.class);
            if (zombie == null || zombie.target == null || !zombie.isCurrentState(this) || this.getFence(zombie) != fence) continue;
            ++count;
        }
        return count;
    }

    private boolean isMetalFence(IsoObject fence) {
        if (fence == null || fence.getProperties() == null) {
            return false;
        }
        PropertyContainer props = fence.getProperties();
        String material = props.get("Material");
        String material2 = props.get("Material2");
        String material3 = props.get("Material3");
        if ("MetalBars".equals(material) || "MetalBars".equals(material2) || "MetalBars".equals(material3)) {
            return true;
        }
        if ("MetalWire".equals(material) || "MetalWire".equals(material2) || "MetalWire".equals(material3)) {
            return true;
        }
        if (fence instanceof IsoThumpable && fence.hasModData()) {
            KahluaTableIterator iter = fence.getModData().iterator();
            while (iter.advance()) {
                String key = Type.tryCastTo(iter.getKey(), String.class);
                if (key == null || !key.contains("MetalPipe")) continue;
                return true;
            }
        }
        return false;
    }

    public void setParams(IsoGameCharacter owner, IsoDirections dir) {
        int x = owner.getSquare().getX();
        int y = owner.getSquare().getY();
        int z = owner.getSquare().getZ();
        int startX = x;
        int startY = y;
        int endX = x;
        int endY = y;
        switch (dir) {
            case N: {
                --endY;
                break;
            }
            case S: {
                ++endY;
                break;
            }
            case W: {
                --endX;
                break;
            }
            case E: {
                ++endX;
                break;
            }
            default: {
                throw new IllegalArgumentException("invalid direction");
            }
        }
        IsoGridSquare oppositeSq = IsoWorld.instance.currentCell.getGridSquare(endX, endY, z);
        boolean scratch = false;
        boolean isCounter = oppositeSq != null && oppositeSq.has(IsoFlagType.solidtrans);
        boolean isFloor = oppositeSq != null && oppositeSq.TreatAsSolidFloor();
        boolean isSheetRope = oppositeSq != null && owner.canClimbDownSheetRope(oppositeSq);
        owner.set(START_X, startX);
        owner.set(START_Y, startY);
        owner.set(Z, z);
        owner.set(END_X, endX);
        owner.set(END_Y, endY);
        owner.set(DIR, dir);
        owner.set(ZOMBIE_ON_FLOOR, false);
        owner.set(PREV_STATE, owner.getCurrentState());
        owner.set(SCRATCH, false);
        owner.set(COUNTER, isCounter);
        owner.set(SOLID_FLOOR, isFloor);
        owner.set(SHEET_ROPE, isSheetRope);
        owner.set(RUN, owner.isRunning());
        owner.set(SPRINT, owner.isSprinting());
        owner.set(COLLIDABLE, false);
    }

    @Override
    public boolean canRagdoll(IsoGameCharacter owner) {
        IsoZombie ownerZombie;
        if (owner.getVariableBoolean("ClimbingFence", false)) {
            return false;
        }
        return !(owner instanceof IsoZombie) || !(ownerZombie = (IsoZombie)owner).isOnFloor();
    }

    @Override
    public boolean isProcessedOnEnter() {
        return true;
    }

    @Override
    public void processOnEnter(IsoGameCharacter owner, Map<Object, Object> delegate) {
        if (RUN.fromDelegate(delegate).booleanValue()) {
            owner.getStats().remove(CharacterStat.ENDURANCE, (float)(ZomboidGlobals.runningEnduranceReduce * 300.0));
        } else if (SPRINT.fromDelegate(delegate).booleanValue()) {
            owner.getStats().remove(CharacterStat.ENDURANCE, (float)(ZomboidGlobals.runningEnduranceReduce * 700.0));
        }
    }
}

