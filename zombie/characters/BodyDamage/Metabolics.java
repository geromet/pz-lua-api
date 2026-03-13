/*
 * Decompiled with CFR 0.152.
 */
package zombie.characters.BodyDamage;

import zombie.UsedFromLua;

@UsedFromLua
public enum Metabolics {
    Sleeping(0.8f),
    SeatedResting(1.0f),
    StandingAtRest(1.1f),
    SedentaryActivity(1.2f),
    Default(1.5f),
    DrivingCar(1.4f),
    LightDomestic(1.6f),
    HeavyDomestic(2.0f),
    DefaultExercise(3.0f),
    UsingTools(2.5f),
    LightWork(3.2f),
    MediumWork(3.9f),
    DiggingSpade(5.5f),
    HeavyWork(6.0f),
    ForestryAxe(8.0f),
    Walking2kmh(1.9f),
    Walking5kmh(3.1f),
    Running10kmh(6.9f),
    Running15kmh(9.5f),
    JumpFence(4.0f),
    ClimbRope(8.0f),
    Fitness(6.0f),
    FitnessHeavy(9.0f),
    MAX(10.3f);

    private final float met;

    private Metabolics(float met) {
        this.met = met;
    }

    public float getMet() {
        return this.met;
    }

    public float getWm2() {
        return Metabolics.MetToWm2(this.met);
    }

    public float getW() {
        return Metabolics.MetToW(this.met);
    }

    public float getBtuHr() {
        return Metabolics.MetToBtuHr(this.met);
    }

    public static float MetToWm2(float met) {
        return 58.0f * met;
    }

    public static float MetToW(float met) {
        return Metabolics.MetToWm2(met) * 1.8f;
    }

    public static float MetToBtuHr(float met) {
        return 356.0f * met;
    }
}

