/*
 * Decompiled with CFR 0.152.
 */
package zombie.ai.states;

import zombie.GameTime;
import zombie.UsedFromLua;
import zombie.ai.State;
import zombie.characters.IsoGameCharacter;
import zombie.core.math.PZMath;

@UsedFromLua
public final class VehicleCollisionOnGroundState
extends State {
    private static final VehicleCollisionOnGroundState INSTANCE = new VehicleCollisionOnGroundState();

    public static VehicleCollisionOnGroundState instance() {
        return INSTANCE;
    }

    private VehicleCollisionOnGroundState() {
        super(false, false, false, false);
    }

    @Override
    public void enter(IsoGameCharacter owner) {
        owner.setStateEventDelayTimer(VehicleCollisionOnGroundState.getMaxReactTime(owner));
    }

    @Override
    public void execute(IsoGameCharacter owner) {
        if (owner.isVehicleCollision()) {
            float currentTimer = owner.getStateEventDelayTimer();
            float newGeneratedTimer = VehicleCollisionOnGroundState.getMaxReactTime(owner);
            float newTimer = PZMath.min(newGeneratedTimer, currentTimer);
            owner.setStateEventDelayTimer(newTimer);
        }
    }

    public static float getMaxReactTime(IsoGameCharacter owner) {
        float timeMin = 0.1f;
        float timeMax = 0.2f;
        float forceToTime = 2.0f;
        float hitForce = owner.getHitForce();
        float staggerTimeMod = owner.getStaggerTimeMod();
        float timeRaw = 2.0f * hitForce * staggerTimeMod;
        float time = PZMath.clamp(timeRaw, 0.1f, 0.2f);
        return GameTime.getInstance().getMultiplierFromTimeDelta(time);
    }
}

