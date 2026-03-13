/*
 * Decompiled with CFR 0.152.
 */
package zombie.ai.states;

import zombie.UsedFromLua;
import zombie.ai.State;
import zombie.audio.parameters.ParameterZombieState;
import zombie.characters.HitReactionNetworkAI;
import zombie.characters.IsoGameCharacter;
import zombie.characters.IsoZombie;
import zombie.core.skinnedmodel.advancedanimation.AnimEvent;
import zombie.core.skinnedmodel.advancedanimation.AnimLayer;
import zombie.core.skinnedmodel.animation.AnimationTrack;
import zombie.network.GameClient;

@UsedFromLua
public final class StaggerBackState
extends State {
    private static final StaggerBackState INSTANCE = new StaggerBackState();

    public static StaggerBackState instance() {
        return INSTANCE;
    }

    private StaggerBackState() {
        super(false, false, false, false);
    }

    @Override
    public void enter(IsoGameCharacter owner) {
        owner.setStateEventDelayTimer(this.getMaxStaggerTime(owner));
        if (GameClient.client && HitReactionNetworkAI.isEnabled(owner)) {
            owner.setDeferredMovementEnabled(false);
        }
    }

    @Override
    public void execute(IsoGameCharacter owner) {
        if (owner.hasAnimationPlayer()) {
            owner.getAnimationPlayer().setTargetToAngle();
        }
        owner.setForwardDirectionFromIsoDirection();
    }

    @Override
    public void exit(IsoGameCharacter owner) {
        if (owner.isZombie()) {
            ((IsoZombie)owner).setStaggerBack(false);
        }
        owner.setShootable(true);
        if (GameClient.client && HitReactionNetworkAI.isEnabled(owner)) {
            owner.setDeferredMovementEnabled(true);
        }
    }

    public float getMaxStaggerTime(IsoGameCharacter owner) {
        float time = 35.0f * owner.getHitForce() * owner.getStaggerTimeMod();
        if (time < 20.0f) {
            time = 20.0f;
        } else if (time > 30.0f) {
            time = 30.0f;
        }
        return time;
    }

    @Override
    public void animEvent(IsoGameCharacter owner, AnimLayer layer, AnimationTrack track, AnimEvent event) {
        if (event.eventName.equalsIgnoreCase("FallOnFront")) {
            owner.setFallOnFront(Boolean.parseBoolean(event.parameterValue));
        }
        if (event.eventName.equalsIgnoreCase("SetState")) {
            IsoZombie zombie = (IsoZombie)owner;
            zombie.parameterZombieState.setState(ParameterZombieState.State.Pushed);
        }
    }
}

