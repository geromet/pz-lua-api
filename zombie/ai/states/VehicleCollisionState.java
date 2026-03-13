/*
 * Decompiled with CFR 0.152.
 */
package zombie.ai.states;

import zombie.UsedFromLua;
import zombie.ai.State;
import zombie.characters.IsoGameCharacter;

@UsedFromLua
public final class VehicleCollisionState
extends State {
    private static final VehicleCollisionState INSTANCE = new VehicleCollisionState();

    public static VehicleCollisionState instance() {
        return INSTANCE;
    }

    private VehicleCollisionState() {
        super(false, false, false, false);
    }

    @Override
    public void enter(IsoGameCharacter owner) {
        owner.setStateEventDelayTimer(0.0f);
    }

    @Override
    public void execute(IsoGameCharacter owner) {
    }
}

