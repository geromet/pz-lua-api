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
public final class VehicleCollisionMinorStaggerState
extends State {
    private static final VehicleCollisionMinorStaggerState INSTANCE = new VehicleCollisionMinorStaggerState();

    public static VehicleCollisionMinorStaggerState instance() {
        return INSTANCE;
    }

    private VehicleCollisionMinorStaggerState() {
        super(false, false, false, false);
    }

    @Override
    public void enter(IsoGameCharacter owner) {
        owner.setStateEventDelayTimer(VehicleCollisionMinorStaggerState.getMaxStaggerTime(owner));
    }

    @Override
    public void execute(IsoGameCharacter owner) {
        if (owner.isVehicleCollision()) {
            float currentTimer = owner.getStateEventDelayTimer();
            float newGeneratedTimer = VehicleCollisionMinorStaggerState.getMaxStaggerTime(owner);
            float newTimer = PZMath.min(newGeneratedTimer, currentTimer);
            owner.setStateEventDelayTimer(newTimer);
        }
    }

    public static float getMaxStaggerTime(IsoGameCharacter owner) {
        float timeMin = 0.1f;
        float timeMax = 0.2f;
        float forceToTime = 3.0f;
        float hitForce = owner.getHitForce();
        float staggerTimeMod = owner.getStaggerTimeMod();
        float timeRaw = 3.0f * hitForce * staggerTimeMod;
        float time = PZMath.clamp(timeRaw, 0.1f, 0.2f);
        return GameTime.getInstance().getMultiplierFromTimeDelta(time);
    }
}

