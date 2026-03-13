/*
 * Decompiled with CFR 0.152.
 */
package zombie.ai.states;

import zombie.UsedFromLua;
import zombie.ai.State;
import zombie.audio.parameters.ParameterZombieState;
import zombie.characters.IsoGameCharacter;
import zombie.characters.IsoPlayer;
import zombie.characters.IsoZombie;
import zombie.core.Core;
import zombie.core.math.PZMath;
import zombie.core.random.Rand;
import zombie.core.skinnedmodel.advancedanimation.AnimEvent;
import zombie.core.skinnedmodel.advancedanimation.AnimLayer;
import zombie.core.skinnedmodel.animation.AnimationTrack;
import zombie.iso.objects.RainManager;

@UsedFromLua
public final class ZombieIdleState
extends State {
    private static final ZombieIdleState INSTANCE = new ZombieIdleState();
    public static final State.Param<Long> TICK_COUNT = State.Param.ofLong("tick_count", 0L);

    public static ZombieIdleState instance() {
        return INSTANCE;
    }

    private ZombieIdleState() {
        super(false, false, false, false);
    }

    @Override
    public void enter(IsoGameCharacter owner) {
        IsoZombie zombie = (IsoZombie)owner;
        zombie.soundSourceTarget = null;
        zombie.soundAttract = 0.0f;
        zombie.movex = 0.0f;
        zombie.movey = 0.0f;
        zombie.setStateEventDelayTimer(this.pickRandomWanderInterval());
        owner.set(TICK_COUNT, 0L);
    }

    @Override
    public void execute(IsoGameCharacter owner) {
        long tickCount;
        IsoZombie zombie = (IsoZombie)owner;
        zombie.movex = 0.0f;
        zombie.movey = 0.0f;
        if (Core.lastStand) {
            IsoPlayer lowest = null;
            float lowestDst = 1000000.0f;
            for (int n = 0; n < IsoPlayer.numPlayers; ++n) {
                if (IsoPlayer.players[n] == null || !(IsoPlayer.players[n].DistTo(zombie) < lowestDst) || IsoPlayer.players[n].isDead()) continue;
                lowestDst = IsoPlayer.players[n].DistTo(zombie);
                lowest = IsoPlayer.players[n];
            }
            if (lowest != null) {
                zombie.pathToCharacter(lowest);
            }
            return;
        }
        if (!zombie.isReanimatedForGrappleOnly()) {
            if (zombie.crawling) {
                zombie.setOnFloor(true);
            } else {
                zombie.setOnFloor(false);
            }
        }
        if ((tickCount = owner.get(TICK_COUNT).longValue()) == 2L) {
            zombie.parameterZombieState.setState(ParameterZombieState.State.Idle);
        }
        owner.set(TICK_COUNT, tickCount + 1L);
        if (zombie.indoorZombie) {
            return;
        }
        if (zombie.isUseless()) {
            return;
        }
        if (!zombie.isReanimatedForGrappleOnly() && zombie.getStateEventDelayTimer() <= 0.0f) {
            zombie.setStateEventDelayTimer(this.pickRandomWanderInterval());
            int x = PZMath.fastfloor(zombie.getX()) + Rand.Next(8) - 4;
            int y = PZMath.fastfloor(zombie.getY()) + Rand.Next(8) - 4;
            if (zombie.getCell().getGridSquare((double)x, (double)y, zombie.getZ()) != null && zombie.getCell().getGridSquare((double)x, (double)y, zombie.getZ()).isFree(true)) {
                zombie.pathToLocation(x, y, PZMath.fastfloor(zombie.getZ()));
                zombie.allowRepathDelay = 200.0f;
            }
        }
        zombie.networkAi.mindSync.zombieIdleUpdate();
    }

    @Override
    public void exit(IsoGameCharacter owner) {
    }

    @Override
    public void animEvent(IsoGameCharacter owner, AnimLayer layer, AnimationTrack track, AnimEvent event) {
    }

    private float pickRandomWanderInterval() {
        float interval = Rand.Next(400, 1000);
        if (!RainManager.isRaining().booleanValue()) {
            interval *= 1.5f;
        }
        return interval;
    }
}

