/*
 * Decompiled with CFR 0.152.
 */
package zombie.ai.states.player;

import zombie.UsedFromLua;
import zombie.ai.State;
import zombie.characters.IsoGameCharacter;

@UsedFromLua
public class PlayerMilkAnimalState
extends State {
    private static final PlayerMilkAnimalState INSTANCE = new PlayerMilkAnimalState();
    public static final State.Param<Boolean> MILK_ANIMAL = State.Param.ofBool("milk_animal", false);
    public static final State.Param<String> ANIMAL = State.Param.ofString("animal", "");
    public static final State.Param<Float> ANIMAL_SIZE = State.Param.ofFloat("animal_size", 0.0f);
    public static final State.Param<String> MILK_ANIM = State.Param.ofString("milk_anim", "");

    public static PlayerMilkAnimalState instance() {
        return INSTANCE;
    }

    private PlayerMilkAnimalState() {
        super(true, true, false, false);
    }

    @Override
    public void enter(IsoGameCharacter owner) {
        this.setParams(owner, State.Stage.Enter);
    }

    @Override
    public void exit(IsoGameCharacter owner) {
        this.setParams(owner, State.Stage.Exit);
    }

    @Override
    public void setParams(IsoGameCharacter owner, State.Stage stage) {
        if (owner.isLocal()) {
            owner.set(MILK_ANIMAL, owner.getVariableBoolean("milkanimal"));
            owner.set(ANIMAL, owner.getVariableString("animal"));
            owner.set(ANIMAL_SIZE, Float.valueOf(owner.getVariableFloat("AnimalSizeY", 0.01f)));
            owner.set(MILK_ANIM, owner.getVariableString("milkAnim"));
        } else {
            owner.setVariable("milkanimal", (boolean)owner.get(MILK_ANIMAL));
            owner.setVariable("animal", owner.get(ANIMAL));
            owner.setVariable("AnimalSizeY", owner.get(ANIMAL_SIZE).floatValue());
            owner.setVariable("milkAnim", owner.get(MILK_ANIM));
        }
        super.setParams(owner, stage);
    }
}

