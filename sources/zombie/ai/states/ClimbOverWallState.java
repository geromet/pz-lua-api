/*
 * Decompiled with CFR 0.152.
 */
package zombie.ai.states;

import fmod.fmod.FMODManager;
import java.util.Map;
import zombie.GameTime;
import zombie.SandboxOptions;
import zombie.UsedFromLua;
import zombie.ZomboidGlobals;
import zombie.ai.State;
import zombie.characters.CharacterStat;
import zombie.characters.IsoGameCharacter;
import zombie.characters.IsoPlayer;
import zombie.characters.IsoZombie;
import zombie.characters.skills.PerkFactory;
import zombie.core.Core;
import zombie.core.math.PZMath;
import zombie.core.properties.PropertyContainer;
import zombie.core.random.Rand;
import zombie.core.skinnedmodel.advancedanimation.AnimEvent;
import zombie.core.skinnedmodel.advancedanimation.AnimLayer;
import zombie.core.skinnedmodel.animation.AnimationTrack;
import zombie.debug.DebugLog;
import zombie.iso.IsoDirections;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoObject;
import zombie.iso.IsoWorld;
import zombie.iso.SpriteDetails.IsoFlagType;
import zombie.iso.SpriteDetails.IsoObjectType;
import zombie.scripting.objects.MoodleType;
import zombie.util.Type;

@UsedFromLua
public final class ClimbOverWallState
extends State {
    private static final ClimbOverWallState INSTANCE = new ClimbOverWallState();
    public static final State.Param<Integer> START_X = State.Param.ofInt("start_x", 0);
    public static final State.Param<Integer> START_Y = State.Param.ofInt("start_y", 0);
    public static final State.Param<Integer> Z = State.Param.ofInt("z", 0);
    public static final State.Param<Integer> END_X = State.Param.ofInt("end_x", 0);
    public static final State.Param<Integer> END_Y = State.Param.ofInt("end_y", 0);
    public static final State.Param<IsoDirections> DIR = State.Param.of("dir", IsoDirections.class);
    public static final State.Param<Boolean> STRUGGLE = State.Param.ofBool("struggle", false);
    public static final State.Param<Boolean> SUCCESS = State.Param.ofBool("success", false);
    static final int FENCE_TYPE_WOOD = 0;
    static final int FENCE_TYPE_METAL = 1;
    static final int FENCE_TYPE_METAL_BARS = 2;

    public static ClimbOverWallState instance() {
        return INSTANCE;
    }

    private ClimbOverWallState() {
        super(true, false, true, false);
    }

    @Override
    public void enter(IsoGameCharacter owner) {
        owner.setIgnoreMovement(true);
        owner.setHideWeaponModel(true);
        owner.getStats().remove(CharacterStat.ENDURANCE, (float)(ZomboidGlobals.runningEnduranceReduce * 1200.0));
        IsoPlayer player = (IsoPlayer)owner;
        boolean struggle = player.isClimbOverWallStruggle();
        if (struggle) {
            owner.getStats().remove(CharacterStat.ENDURANCE, (float)(ZomboidGlobals.runningEnduranceReduce * 500.0));
        }
        boolean success = player.isClimbOverWallSuccess();
        owner.setVariable("ClimbFenceFinished", false);
        owner.setVariable("ClimbFenceStarted", false);
        if (owner.isLocal()) {
            owner.setVariable("ClimbFenceOutcome", success ? "success" : "fail");
            owner.setVariable("ClimbFenceStruggle", struggle);
        } else {
            owner.setVariable("ClimbFenceOutcome", owner.get(SUCCESS) != false ? "success" : "fail");
            owner.setVariable("ClimbFenceStruggle", (boolean)owner.get(STRUGGLE));
        }
        if (player.isLocalPlayer()) {
            player.triggerMusicIntensityEvent("ClimbWall");
        }
        if (!success && owner.getPathFindBehavior2() != null) {
            owner.getPathFindBehavior2().reset();
            owner.getPathFindBehavior2().cancel();
            owner.setPath2(null);
        }
    }

    @Override
    public void execute(IsoGameCharacter owner) {
        IsoDirections dir = owner.get(DIR);
        owner.setAnimated(true);
        owner.setDir(dir);
        float skillFactor = (float)(owner.getPerkLevel(PerkFactory.Perks.Nimble) + owner.getPerkLevel(PerkFactory.Perks.Strength) * 2) / 3.0f;
        owner.addBothArmMuscleStrain((float)(0.02 * (double)GameTime.instance.getMultiplier() * (double)(owner.getMoodles().getMoodleLevel(MoodleType.HEAVY_LOAD) + 1)) * ((15.0f - skillFactor) / 10.0f) * (GameTime.instance.getMultiplier() / 0.8f));
        boolean climbFenceStarted = owner.getVariableBoolean("ClimbFenceStarted");
        if (!climbFenceStarted) {
            int startX = owner.get(START_X);
            int startY = owner.get(START_Y);
            float dxy = 0.15f;
            float idealX = owner.getX();
            float idealY = owner.getY();
            switch (dir) {
                case N: {
                    idealY = (float)startY + 0.15f;
                    break;
                }
                case S: {
                    idealY = (float)(startY + 1) - 0.15f;
                    break;
                }
                case W: {
                    idealX = (float)startX + 0.15f;
                    break;
                }
                case E: {
                    idealX = (float)(startX + 1) - 0.15f;
                }
            }
            float mult = GameTime.getInstance().getThirtyFPSMultiplier() / 8.0f;
            owner.setX(owner.getX() + (idealX - owner.getX()) * mult);
            owner.setY(owner.getY() + (idealY - owner.getY()) * mult);
        }
    }

    @Override
    public void exit(IsoGameCharacter owner) {
        owner.clearVariable("ClimbingFence");
        owner.clearVariable("ClimbFenceFinished");
        owner.clearVariable("ClimbFenceOutcome");
        owner.clearVariable("ClimbFenceStarted");
        owner.clearVariable("ClimbFenceStruggle");
        owner.clearVariable("PlayerVoiceSound");
        owner.setIgnoreMovement(false);
        owner.setHideWeaponModel(false);
        if (owner instanceof IsoZombie) {
            IsoZombie isoZombie = (IsoZombie)owner;
            isoZombie.networkAi.isClimbing = false;
        }
    }

    @Override
    public void animEvent(IsoGameCharacter owner, AnimLayer layer, AnimationTrack track, AnimEvent event) {
        IsoPlayer player = Type.tryCastTo(owner, IsoPlayer.class);
        if (event.eventName.equalsIgnoreCase("PlayFenceSound")) {
            IsoObject fence = this.getFence(owner);
            if (fence == null) {
                return;
            }
            int fenceType = this.getFenceType(fence);
            long instance = owner.getEmitter().playSoundImpl(event.parameterValue, null);
            owner.getEmitter().setParameterValue(instance, FMODManager.instance.getParameterDescription("FenceTypeHigh"), fenceType);
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
    }

    @Override
    public boolean isIgnoreCollide(IsoGameCharacter owner, int fromX, int fromY, int fromZ, int toX, int toY, int toZ) {
        if (!SUCCESS.get(owner).booleanValue()) {
            return false;
        }
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

    private IsoObject getClimbableWallN(IsoGridSquare square) {
        IsoObject[] objects = square.getObjects().getElements();
        int n = square.getObjects().size();
        for (int i = 0; i < n; ++i) {
            IsoObject object = objects[i];
            PropertyContainer props = object.getProperties();
            if (props == null || props.has(IsoFlagType.CantClimb) || object.getType() != IsoObjectType.wall || !props.has(IsoFlagType.collideN) || props.has(IsoFlagType.HoppableN)) continue;
            return object;
        }
        return null;
    }

    private IsoObject getClimbableWallW(IsoGridSquare square) {
        IsoObject[] objects = square.getObjects().getElements();
        int n = square.getObjects().size();
        for (int i = 0; i < n; ++i) {
            IsoObject object = objects[i];
            PropertyContainer props = object.getProperties();
            if (props == null || props.has(IsoFlagType.CantClimb) || object.getType() != IsoObjectType.wall || !props.has(IsoFlagType.collideW) || props.has(IsoFlagType.HoppableW)) continue;
            return object;
        }
        return null;
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
        IsoDirections dir = owner.get(DIR);
        return switch (dir) {
            case IsoDirections.N -> this.getClimbableWallN(startSq);
            case IsoDirections.E -> this.getClimbableWallW(endSq);
            case IsoDirections.W -> this.getClimbableWallW(startSq);
            case IsoDirections.S -> this.getClimbableWallN(endSq);
            default -> null;
        };
    }

    private int getFenceType(IsoObject fence) {
        if (fence.getSprite() == null) {
            return 0;
        }
        PropertyContainer props = fence.getSprite().getProperties();
        String typeStr = props.get("FenceTypeHigh");
        if (typeStr != null) {
            return switch (typeStr) {
                case "Wood" -> 0;
                case "Metal" -> 1;
                case "MetalGate" -> 2;
                default -> 0;
            };
        }
        return 0;
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
        owner.set(START_X, startX);
        owner.set(START_Y, startY);
        owner.set(Z, z);
        owner.set(END_X, endX);
        owner.set(END_Y, endY);
        owner.set(DIR, dir);
        IsoPlayer player = (IsoPlayer)owner;
        if (player.isLocalPlayer()) {
            if (SandboxOptions.instance.easyClimbing.getValue()) {
                player.setClimbOverWallStruggle(false);
                player.setClimbOverWallSuccess(true);
            } else {
                int struggleChance = owner.getClimbingFailChanceInt();
                DebugLog.log("ClimbWall actual struggleChance 1 in " + struggleChance / 2);
                boolean struggle = Rand.NextBool(struggleChance / 2);
                if ("Tutorial".equals(Core.gameMode)) {
                    struggle = false;
                }
                DebugLog.log("ClimbWall struggle? " + struggle);
                DebugLog.log("ClimbWall failure chance 1 in " + struggleChance);
                boolean success = false;
                if (struggleChance > 0) {
                    success = !Rand.NextBool(struggleChance);
                } else if (owner.getMoodles().getMoodleLevel(MoodleType.HEAVY_LOAD) == 0) {
                    int chance = Math.max(1, owner.getPerkLevel(PerkFactory.Perks.Strength));
                    DebugLog.log("ClimbWall bonus " + (chance + 1) + " of success when base chance is 0 when encumbered");
                    success = Rand.Next(100) <= chance;
                }
                DebugLog.log("ClimbWall success? " + success);
                player.setClimbOverWallStruggle(struggle);
                player.setClimbOverWallSuccess(success);
            }
        }
        owner.set(STRUGGLE, player.isClimbOverWallStruggle());
        owner.set(SUCCESS, player.isClimbOverWallSuccess());
    }

    @Override
    public boolean isProcessedOnEnter() {
        return true;
    }

    @Override
    public void processOnEnter(IsoGameCharacter owner, Map<Object, Object> delegate) {
        owner.getStats().remove(CharacterStat.ENDURANCE, (float)(ZomboidGlobals.runningEnduranceReduce * 1200.0));
        if (STRUGGLE.fromDelegate(delegate).booleanValue()) {
            owner.getStats().remove(CharacterStat.ENDURANCE, (float)(ZomboidGlobals.runningEnduranceReduce * 500.0));
        }
    }
}

