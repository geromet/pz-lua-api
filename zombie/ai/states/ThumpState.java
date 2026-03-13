/*
 * Decompiled with CFR 0.152.
 */
package zombie.ai.states;

import zombie.GameTime;
import zombie.SoundManager;
import zombie.UsedFromLua;
import zombie.ai.State;
import zombie.characters.IsoGameCharacter;
import zombie.characters.IsoPlayer;
import zombie.characters.IsoZombie;
import zombie.characters.ZombieThumpManager;
import zombie.core.PerformanceSettings;
import zombie.core.math.PZMath;
import zombie.core.random.Rand;
import zombie.core.skinnedmodel.advancedanimation.AnimEvent;
import zombie.core.skinnedmodel.advancedanimation.AnimLayer;
import zombie.core.skinnedmodel.animation.AnimationTrack;
import zombie.iso.IsoChunk;
import zombie.iso.IsoDirections;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoMovingObject;
import zombie.iso.IsoObject;
import zombie.iso.IsoWorld;
import zombie.iso.LosUtil;
import zombie.iso.objects.IsoBarricade;
import zombie.iso.objects.IsoDoor;
import zombie.iso.objects.IsoThumpable;
import zombie.iso.objects.IsoWindow;
import zombie.iso.objects.IsoWindowFrame;
import zombie.iso.objects.interfaces.BarricadeAble;
import zombie.iso.objects.interfaces.Thumpable;
import zombie.network.GameClient;
import zombie.network.GameServer;
import zombie.network.PacketTypes;
import zombie.network.ServerMap;
import zombie.network.ServerOptions;
import zombie.network.packets.INetworkPacket;
import zombie.scripting.objects.SoundKey;
import zombie.util.StringUtils;
import zombie.util.Type;
import zombie.vehicles.BaseVehicle;

@UsedFromLua
public final class ThumpState
extends State {
    private static final ThumpState INSTANCE = new ThumpState();

    public static ThumpState instance() {
        return INSTANCE;
    }

    private ThumpState() {
        super(false, false, false, false);
    }

    @Override
    public void enter(IsoGameCharacter owner) {
        if (!GameClient.client || owner.isLocal()) {
            switch (Rand.Next(3)) {
                case 0: {
                    owner.setVariable("ThumpType", "DoorClaw");
                    break;
                }
                case 1: {
                    owner.setVariable("ThumpType", "Door");
                    break;
                }
                case 2: {
                    owner.setVariable("ThumpType", "DoorBang");
                }
            }
        }
        if (GameClient.client && owner.isLocal()) {
            INetworkPacket.send(PacketTypes.PacketType.Thump, owner);
        }
    }

    @Override
    public void execute(IsoGameCharacter owner) {
        IsoGridSquare sq2;
        IsoThumpable door;
        IsoGridSquare sq;
        IsoWindow isoWindow;
        boolean fastForward;
        IsoZombie zombie = (IsoZombie)owner;
        Thumpable thump = owner.getThumpTarget();
        if (thump == null) {
            this.exit(owner);
            return;
        }
        if (thump instanceof IsoObject) {
            IsoObject isoObject = (IsoObject)thump;
            if (isoObject.getSquare() == null) {
                this.exit(owner);
                return;
            }
            owner.faceThisObject(isoObject);
        }
        this.slideAwayFromEdge(owner, thump);
        boolean bl = fastForward = GameServer.server && GameServer.fastForward || !GameServer.server && IsoPlayer.allPlayersAsleep();
        if (fastForward || owner.getActionContext().hasEventOccurred("thumpframe")) {
            boolean listener;
            owner.getActionContext().clearEvent("thumpframe");
            owner.setTimeThumping(owner.getTimeThumping() + 1);
            if (zombie.timeSinceSeenFlesh < 5.0f) {
                owner.setTimeThumping(0);
            }
            int count = 1;
            if (owner.getCurrentSquare() != null) {
                count = owner.getCurrentSquare().getMovingObjects().size();
            }
            for (int n = 0; n < count && this.isThumpTargetValid(owner, owner.getThumpTarget()); ++n) {
                owner.getThumpTarget().Thump(owner);
            }
            Thumpable thumpableFor = owner.getThumpTarget() == null ? null : owner.getThumpTarget().getThumpableFor(owner);
            boolean bl2 = listener = GameServer.server || SoundManager.instance.isListenerInRange(owner.getX(), owner.getY(), 20.0f);
            if (listener && !IsoPlayer.allPlayersAsleep()) {
                if (thumpableFor instanceof IsoWindow) {
                    zombie.setThumpFlag(Rand.Next(3) == 0 ? 2 : 3);
                    zombie.setThumpCondition(thumpableFor.getThumpCondition());
                    if (!GameServer.server) {
                        ZombieThumpManager.instance.addCharacter(zombie);
                    }
                } else if (thumpableFor != null) {
                    String thumpSound = "ZombieThumpGeneric";
                    IsoBarricade barricade = Type.tryCastTo(thumpableFor, IsoBarricade.class);
                    if (barricade != null && (barricade.isMetal() || barricade.isMetalBar())) {
                        thumpSound = "ZombieThumpMetal";
                    } else if (barricade != null && barricade.getNumPlanks() > 0) {
                        if (owner.isVariable("ThumpType", "DoorClaw")) {
                            thumpSound = "ZombieThumpWood";
                        }
                    } else if (thumpableFor instanceof IsoDoor) {
                        IsoDoor door2 = (IsoDoor)thumpableFor;
                        thumpSound = door2.getThumpSound();
                        if ("WoodDoor".equalsIgnoreCase(door2.getSoundPrefix()) && owner.isVariable("ThumpType", "DoorClaw")) {
                            thumpSound = "ZombieThumpWood";
                        }
                    } else if (thumpableFor instanceof IsoThumpable) {
                        IsoThumpable thumpable = (IsoThumpable)thumpableFor;
                        thumpSound = thumpable.getThumpSound();
                        if (thumpable.isDoor() && "WoodDoor".equalsIgnoreCase(thumpable.getSoundPrefix()) && owner.isVariable("ThumpType", "DoorClaw")) {
                            thumpSound = "ZombieThumpWood";
                        }
                    } else if (thumpableFor instanceof IsoObject) {
                        String soundName;
                        IsoObject object = (IsoObject)thumpableFor;
                        if (object.sprite != null && object.sprite.getProperties().has("ThumpSound") && !StringUtils.isNullOrWhitespace(soundName = object.sprite.getProperties().get("ThumpSound"))) {
                            thumpSound = soundName;
                        }
                    }
                    if (SoundKey.ZOMBIE_THUMP_GENERIC.getSoundName().equals(thumpSound)) {
                        zombie.setThumpFlag(1);
                    } else if (SoundKey.ZOMBIE_THUMP_WINDOW.getSoundName().equals(thumpSound)) {
                        zombie.setThumpFlag(3);
                    } else if (SoundKey.ZOMBIE_THUMP_WINDOW_EXTRA.getSoundName().equals(thumpSound)) {
                        zombie.setThumpFlag(2);
                    } else if (SoundKey.ZOMBIE_THUMP_METAL.getSoundName().equals(thumpSound)) {
                        zombie.setThumpFlag(4);
                    } else if (SoundKey.ZOMBIE_THUMP_GARAGE_DOOR.getSoundName().equals(thumpSound)) {
                        zombie.setThumpFlag(5);
                    } else if (SoundKey.ZOMBIE_THUMP_CHAINLINK_FENCE.getSoundName().equals(thumpSound)) {
                        zombie.setThumpFlag(6);
                    } else if (SoundKey.ZOMBIE_THUMP_METAL_POLE_FENCE.getSoundName().equals(thumpSound) || "ZombieThumpMetalPoleGate".equals(thumpSound)) {
                        zombie.setThumpFlag(7);
                    } else if (SoundKey.ZOMBIE_THUMP_WOOD.getSoundName().equals(thumpSound)) {
                        zombie.setThumpFlag(8);
                    } else {
                        zombie.setThumpFlag(1);
                    }
                    zombie.setThumpCondition(thumpableFor.getThumpCondition());
                    if (!GameServer.server) {
                        ZombieThumpManager.instance.addCharacter(zombie);
                    }
                }
            }
        }
        if (this.isThumpTargetValid(owner, owner.getThumpTarget())) {
            return;
        }
        owner.setThumpTarget(null);
        owner.setTimeThumping(0);
        if (thump instanceof IsoWindow && (isoWindow = (IsoWindow)thump).canClimbThrough(owner)) {
            owner.climbThroughWindow(isoWindow);
            return;
        }
        if (thump instanceof IsoDoor) {
            IsoGridSquare sq22;
            IsoDoor door3 = (IsoDoor)thump;
            if ((door3.open || thump.isDestroyed()) && this.lungeThroughDoor(zombie, sq = door3.getSquare(), sq22 = door3.getOppositeSquare())) {
                return;
            }
        }
        if (thump instanceof IsoThumpable && (door = (IsoThumpable)thump).isDoor() && (door.open || thump.isDestroyed()) && this.lungeThroughDoor(zombie, sq = door.getSquare(), sq2 = door.getInsideSquare())) {
            return;
        }
        if (zombie.lastTargetSeenX != -1) {
            owner.pathToLocation(zombie.lastTargetSeenX, zombie.lastTargetSeenY, zombie.lastTargetSeenZ);
        }
    }

    @Override
    public void exit(IsoGameCharacter owner) {
        owner.setThumpTarget(null);
        ((IsoZombie)owner).setThumpTimer(200);
        if (GameClient.client && owner.isLocal()) {
            INetworkPacket.send(PacketTypes.PacketType.Thump, owner);
        }
    }

    @Override
    public void animEvent(IsoGameCharacter owner, AnimLayer layer, AnimationTrack track, AnimEvent event) {
        if (event.eventName.equalsIgnoreCase("ThumpFrame")) {
            // empty if block
        }
    }

    private void slideAwayFromEdge(IsoGameCharacter owner, Thumpable target) {
        if (target == null) {
            this.exit(owner);
            return;
        }
        boolean bSolid = false;
        boolean bNorth = false;
        if (target instanceof BaseVehicle) {
            return;
        }
        if (target instanceof IsoObject) {
            IsoObject object = (IsoObject)target;
            IsoGridSquare square = object.getSquare();
            if (target instanceof IsoBarricade) {
                IsoBarricade barricade = (IsoBarricade)target;
                bNorth = barricade.getDir() == IsoDirections.N || barricade.getDir() == IsoDirections.S;
            } else if (target instanceof IsoDoor) {
                IsoDoor door = (IsoDoor)target;
                bNorth = door.getNorth();
            } else if (target instanceof IsoThumpable) {
                IsoThumpable thumpable = (IsoThumpable)target;
                bSolid = thumpable.isBlockAllTheSquare();
                bNorth = thumpable.getNorth();
            } else if (target instanceof IsoWindow) {
                IsoWindow window = (IsoWindow)target;
                bNorth = window.getNorth();
            } else if (target instanceof IsoWindowFrame) {
                IsoWindowFrame windowFrame = (IsoWindowFrame)target;
                bNorth = windowFrame.getNorth();
            }
            float dist = 0.4f;
            Thumpable thumpable = target.getThumpableFor(owner);
            if (thumpable instanceof IsoBarricade) {
                BarricadeAble barricadeAble;
                IsoBarricade barricade = (IsoBarricade)thumpable;
                if (target instanceof BarricadeAble && IsoBarricade.GetBarricadeForCharacter(barricadeAble = (BarricadeAble)((Object)target), owner) == thumpable) {
                    dist = 0.47f;
                }
            }
            if (square == null) {
                this.exit(owner);
                return;
            }
            if (bSolid) {
                if (owner.getY() < (float)square.y) {
                    this.slideAwayFromEdgeN(owner, square.y, dist);
                } else if (owner.getY() > (float)(square.y + 1)) {
                    this.slideAwayFromEdgeS(owner, square.y + 1, dist);
                }
                if (owner.getX() < (float)square.x) {
                    this.slideAwayFromEdgeW(owner, square.x, dist);
                } else if (owner.getX() > (float)(square.x + 1)) {
                    this.slideAwayFromEdgeE(owner, square.x + 1, dist);
                }
            } else if (bNorth) {
                if (owner.getY() < (float)square.y) {
                    this.slideAwayFromEdgeN(owner, square.y, dist);
                } else {
                    this.slideAwayFromEdgeS(owner, square.y, dist);
                }
            } else if (owner.getX() < (float)square.x) {
                this.slideAwayFromEdgeW(owner, square.x, dist);
            } else {
                this.slideAwayFromEdgeE(owner, square.x, dist);
            }
        }
    }

    private void slideAwayFromEdgeN(IsoGameCharacter owner, int squareY, float dist) {
        if (owner.getY() > (float)squareY - dist) {
            owner.setNextY((float)squareY - dist);
        }
    }

    private void slideAwayFromEdgeS(IsoGameCharacter owner, int squareY, float dist) {
        if (owner.getY() < (float)squareY + dist) {
            owner.setNextY((float)squareY + dist);
        }
    }

    private void slideAwayFromEdgeW(IsoGameCharacter owner, int squareX, float dist) {
        if (owner.getX() > (float)squareX - dist) {
            owner.setNextX((float)squareX - dist);
        }
    }

    private void slideAwayFromEdgeE(IsoGameCharacter owner, int squareX, float dist) {
        if (owner.getX() < (float)squareX + dist) {
            owner.setNextX((float)squareX + dist);
        }
    }

    private IsoPlayer findPlayer(int x1, int x2, int y1, int y2, int z) {
        for (int y = y1; y <= y2; ++y) {
            for (int x = x1; x <= x2; ++x) {
                IsoGridSquare sq = IsoWorld.instance.currentCell.getGridSquare(x, y, z);
                if (sq == null) continue;
                for (int i = 0; i < sq.getMovingObjects().size(); ++i) {
                    IsoPlayer isoPlayer;
                    IsoMovingObject o = sq.getMovingObjects().get(i);
                    if (!(o instanceof IsoPlayer) || (isoPlayer = (IsoPlayer)o).isGhostMode()) continue;
                    return isoPlayer;
                }
            }
        }
        return null;
    }

    private boolean lungeThroughDoor(IsoZombie z, IsoGridSquare sq, IsoGridSquare sq2) {
        if (sq == null || sq2 == null) {
            return false;
        }
        boolean north = sq.getY() > sq2.getY();
        IsoGridSquare sq3 = null;
        IsoMovingObject player = null;
        if (z.getCurrentSquare() == sq) {
            sq3 = sq2;
            player = north ? this.findPlayer(sq2.getX() - 1, sq2.getX() + 1, sq2.getY() - 1, sq2.getY(), sq2.getZ()) : this.findPlayer(sq2.getX() - 1, sq2.getX(), sq2.getY() - 1, sq2.getY() + 1, sq2.getZ());
        } else if (z.getCurrentSquare() == sq2) {
            sq3 = sq;
            player = north ? this.findPlayer(sq.getX() - 1, sq.getX() + 1, sq.getY(), sq.getY() + 1, sq.getZ()) : this.findPlayer(sq.getX(), sq.getX() + 1, sq.getY() - 1, sq.getY() + 1, sq.getZ());
        }
        if (player != null && !LosUtil.lineClearCollide(sq3.getX(), sq3.getY(), sq3.getZ(), PZMath.fastfloor(player.getX()), PZMath.fastfloor(player.getY()), PZMath.fastfloor(player.getZ()), false)) {
            z.setTarget(player);
            z.vectorToTarget.x = player.getX();
            z.vectorToTarget.y = player.getY();
            z.vectorToTarget.x -= z.getX();
            z.vectorToTarget.y -= z.getY();
            z.timeSinceSeenFlesh = 0.0f;
            z.setThumpTarget(null);
            return true;
        }
        return false;
    }

    public static int getFastForwardDamageMultiplier() {
        GameTime gt = GameTime.getInstance();
        if (GameServer.server) {
            return (int)(GameServer.fastForward ? ServerOptions.instance.fastForwardMultiplier.getValue() / (double)gt.getDeltaMinutesPerDay() : 1.0);
        }
        if (GameClient.client) {
            return (int)(GameClient.fastForward ? ServerOptions.instance.fastForwardMultiplier.getValue() / (double)gt.getDeltaMinutesPerDay() : 1.0);
        }
        if (IsoPlayer.allPlayersAsleep()) {
            return (int)(200.0f * (30.0f / (float)PerformanceSettings.getLockFPS()) / 1.6f);
        }
        return (int)gt.getTrueMultiplier();
    }

    private boolean isThumpTargetValid(IsoGameCharacter owner, Thumpable thumpable) {
        IsoChunk chunk;
        if (thumpable == null) {
            return false;
        }
        if (thumpable.isDestroyed()) {
            return false;
        }
        if (!(thumpable instanceof IsoObject)) {
            return false;
        }
        IsoObject obj = (IsoObject)thumpable;
        if (thumpable instanceof BaseVehicle) {
            return obj.getMovingObjectIndex() != -1;
        }
        if (obj.getObjectIndex() == -1) {
            return false;
        }
        int wx = obj.getSquare().getX() / 8;
        int wy = obj.getSquare().getY() / 8;
        IsoChunk isoChunk = chunk = GameServer.server ? ServerMap.instance.getChunk(wx, wy) : IsoWorld.instance.currentCell.getChunk(wx, wy);
        if (chunk == null) {
            return false;
        }
        return thumpable.getThumpableFor(owner) != null;
    }
}

