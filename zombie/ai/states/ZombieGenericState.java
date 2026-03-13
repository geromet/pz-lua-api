/*
 * Decompiled with CFR 0.152.
 */
package zombie.ai.states;

import zombie.UsedFromLua;
import zombie.ai.State;
import zombie.characters.IsoGameCharacter;

@UsedFromLua
public final class ZombieGenericState
extends State {
    private static final ZombieGenericState INSTANCE = new ZombieGenericState();

    public static ZombieGenericState instance() {
        return INSTANCE;
    }

    private ZombieGenericState() {
        super(false, false, false, false);
    }

    @Override
    public void enter(IsoGameCharacter owner) {
    }

    @Override
    public void execute(IsoGameCharacter owner) {
    }

    @Override
    public void exit(IsoGameCharacter owner) {
    }
}

