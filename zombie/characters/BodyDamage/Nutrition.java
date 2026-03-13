/*
 * Decompiled with CFR 0.152.
 */
package zombie.characters.BodyDamage;

import java.nio.ByteBuffer;
import zombie.GameTime;
import zombie.Lua.LuaEventManager;
import zombie.SandboxOptions;
import zombie.UsedFromLua;
import zombie.ai.states.ClimbOverFenceState;
import zombie.ai.states.ClimbThroughWindowState;
import zombie.ai.states.SwipeStatePlayer;
import zombie.characters.CharacterTimedActions.BaseAction;
import zombie.characters.IsoPlayer;
import zombie.characters.skills.PerkFactory;
import zombie.network.GameClient;
import zombie.scripting.objects.CharacterTrait;

@UsedFromLua
public final class Nutrition {
    private final IsoPlayer parent;
    private float carbohydrates;
    private float lipids;
    private float proteins;
    private float calories;
    private final float carbohydratesDecreraseFemale = 0.0035f;
    private final float carbohydratesDecreraseMale = 0.0035f;
    private final float lipidsDecreraseFemale = 0.00113f;
    private final float lipidsDecreraseMale = 0.00113f;
    private final float proteinsDecreraseFemale = 8.6E-4f;
    private final float proteinsDecreraseMale = 8.6E-4f;
    private final float caloriesDecreraseFemaleNormal = 0.016f;
    private final float caloriesDecreaseMaleNormal = 0.016f;
    private final float caloriesDecreraseFemaleExercise = 0.13f;
    private final float caloriesDecreaseMaleExercise = 0.13f;
    private final float caloriesDecreraseFemaleSleeping = 0.003f;
    private final float caloriesDecreaseMaleSleeping = 0.003f;
    private final int caloriesToGainWeightMale = 1000;
    private final int caloriesToGainWeightMaxMale = 4000;
    private final int caloriesToGainWeightFemale = 1000;
    private final int caloriesToGainWeightMaxFemale = 4000;
    private final int caloriesDecreaseMax = 2500;
    private final float weightGain = 1.3E-5f;
    private final float weightLoss = 8.5E-6f;
    private double weight = 60.0;
    private int updatedWeight;
    private final boolean isFemale = false;
    private float caloriesMax;
    private float caloriesMin;
    private boolean incWeight;
    private boolean incWeightLot;
    private boolean decWeight;

    public Nutrition(IsoPlayer parent) {
        this.parent = parent;
        this.setWeight(80.0);
        this.setCalories(800.0f);
    }

    public void update() {
        if (!SandboxOptions.instance.nutrition.getValue()) {
            return;
        }
        if (this.parent == null || this.parent.isDead()) {
            return;
        }
        if (this.parent.isGodMod()) {
            return;
        }
        if (!GameClient.client) {
            this.setCarbohydrates(this.getCarbohydrates() - 0.0035f * GameTime.getInstance().getGameWorldSecondsSinceLastUpdate());
            this.setLipids(this.getLipids() - 0.00113f * GameTime.getInstance().getGameWorldSecondsSinceLastUpdate());
            this.setProteins(this.getProteins() - 8.6E-4f * GameTime.getInstance().getGameWorldSecondsSinceLastUpdate());
            this.updateCalories();
        }
        this.updateWeight();
    }

    private void updateCalories() {
        float modifier = 1.0f;
        if (!this.parent.getCharacterActions().isEmpty()) {
            modifier = ((BaseAction)this.parent.getCharacterActions().get((int)0)).caloriesModifier;
        }
        if (this.parent.isCurrentState(SwipeStatePlayer.instance()) || this.parent.isCurrentState(ClimbOverFenceState.instance()) || this.parent.isCurrentState(ClimbThroughWindowState.instance())) {
            modifier = 8.0f;
        }
        float coldMulti = 1.0f;
        if (this.parent.getBodyDamage() != null && this.parent.getBodyDamage().getThermoregulator() != null) {
            coldMulti = (float)this.parent.getBodyDamage().getThermoregulator().getEnergyMultiplier();
        }
        float caloriesDelta = (float)(this.getWeight() / 80.0);
        if (this.parent.IsRunning() && this.parent.isPlayerMoving()) {
            modifier = 1.0f;
            this.setCalories(this.getCalories() - 0.13f * modifier * caloriesDelta * GameTime.getInstance().getGameWorldSecondsSinceLastUpdate());
        } else if (this.parent.isSprinting() && this.parent.isPlayerMoving()) {
            modifier = 1.3f;
            this.setCalories(this.getCalories() - 0.13f * modifier * caloriesDelta * GameTime.getInstance().getGameWorldSecondsSinceLastUpdate());
        } else if (this.parent.isPlayerMoving()) {
            modifier = 0.6f;
            this.setCalories(this.getCalories() - 0.13f * modifier * caloriesDelta * GameTime.getInstance().getGameWorldSecondsSinceLastUpdate());
        } else if (this.parent.isAsleep()) {
            this.setCalories(this.getCalories() - 0.003f * modifier * coldMulti * caloriesDelta * GameTime.getInstance().getGameWorldSecondsSinceLastUpdate());
        } else {
            this.setCalories(this.getCalories() - 0.016f * modifier * coldMulti * caloriesDelta * GameTime.getInstance().getGameWorldSecondsSinceLastUpdate());
        }
        if (this.getCalories() > this.caloriesMax) {
            this.caloriesMax = this.getCalories();
        }
        if (this.getCalories() < this.caloriesMin) {
            this.caloriesMin = this.getCalories();
        }
    }

    private void updateWeight() {
        double weight;
        this.setIncWeight(false);
        this.setIncWeightLot(false);
        this.setDecWeight(false);
        float caloriesToGainWeight = 1000.0f;
        float caloriesToGainWeightMax = 4000.0f;
        if (this.getWeight() < 90.0 && this.parent.hasTrait(CharacterTrait.WEIGHT_GAIN)) {
            caloriesToGainWeight = 700.0f;
        }
        if (this.getWeight() > 70.0 && this.parent.hasTrait(CharacterTrait.WEIGHT_LOSS)) {
            caloriesToGainWeight = 1800.0f;
        }
        float caloriesDiff = (float)((this.getWeight() - 80.0) * 40.0);
        caloriesToGainWeight += caloriesDiff;
        float caloriesToLoseWeight = (float)((this.getWeight() - 70.0) * 30.0);
        if (caloriesToLoseWeight > 0.0f) {
            caloriesToLoseWeight = 0.0f;
        }
        if (this.getCalories() > caloriesToGainWeight) {
            this.setIncWeight(true);
            float delta = this.getCalories() / caloriesToGainWeightMax;
            if (delta > 1.0f) {
                delta = 1.0f;
            }
            float realWeightGain = 1.3E-5f;
            if (this.getCarbohydrates() > 700.0f || this.getLipids() > 700.0f) {
                realWeightGain *= 3.0f;
                this.setIncWeightLot(true);
            } else if (this.getCarbohydrates() > 400.0f || this.getLipids() > 400.0f) {
                realWeightGain *= 2.0f;
                this.setIncWeightLot(true);
            }
            weight = this.getWeight() + (double)(realWeightGain * delta * GameTime.getInstance().getGameWorldSecondsSinceLastUpdate());
        } else if (this.getCalories() < caloriesToLoseWeight) {
            this.setDecWeight(true);
            float delta = Math.abs(this.getCalories()) / 2500.0f;
            if (delta > 1.0f) {
                delta = 1.0f;
            }
            weight = this.getWeight() - (double)(8.5E-6f * delta * GameTime.getInstance().getGameWorldSecondsSinceLastUpdate());
        } else {
            weight = this.getWeight();
        }
        if (!GameClient.client) {
            this.setWeight(weight);
            ++this.updatedWeight;
            if (this.updatedWeight >= 2000) {
                this.applyTraitFromWeight();
                this.updatedWeight = 0;
            }
        }
    }

    public void save(ByteBuffer output) {
        output.putFloat(this.getCalories());
        output.putFloat(this.getProteins());
        output.putFloat(this.getLipids());
        output.putFloat(this.getCarbohydrates());
        output.putFloat((float)this.getWeight());
    }

    public void load(ByteBuffer input) {
        this.setCalories(input.getFloat());
        this.setProteins(input.getFloat());
        this.setLipids(input.getFloat());
        this.setCarbohydrates(input.getFloat());
        this.setWeight(input.getFloat());
    }

    public void applyWeightFromTraits() {
        if (this.parent.hasTrait(CharacterTrait.EMACIATED)) {
            this.setWeight(50.0);
        }
        if (this.parent.hasTrait(CharacterTrait.VERY_UNDERWEIGHT)) {
            this.setWeight(60.0);
        }
        if (this.parent.hasTrait(CharacterTrait.UNDERWEIGHT)) {
            this.setWeight(70.0);
        }
        if (this.parent.hasTrait(CharacterTrait.OVERWEIGHT)) {
            this.setWeight(95.0);
        }
        if (this.parent.hasTrait(CharacterTrait.OBESE)) {
            this.setWeight(105.0);
        }
    }

    public void applyTraitFromWeight() {
        this.parent.getCharacterTraits().remove(CharacterTrait.UNDERWEIGHT);
        this.parent.getCharacterTraits().remove(CharacterTrait.VERY_UNDERWEIGHT);
        this.parent.getCharacterTraits().remove(CharacterTrait.EMACIATED);
        this.parent.getCharacterTraits().remove(CharacterTrait.OVERWEIGHT);
        this.parent.getCharacterTraits().remove(CharacterTrait.OBESE);
        if (this.getWeight() >= 100.0) {
            this.parent.getCharacterTraits().add(CharacterTrait.OBESE);
        }
        if (this.getWeight() >= 85.0 && this.getWeight() < 100.0) {
            this.parent.getCharacterTraits().add(CharacterTrait.OVERWEIGHT);
        }
        if (this.getWeight() > 65.0 && this.getWeight() <= 75.0) {
            this.parent.getCharacterTraits().add(CharacterTrait.UNDERWEIGHT);
        }
        if (this.getWeight() > 50.0 && this.getWeight() <= 65.0) {
            this.parent.getCharacterTraits().add(CharacterTrait.VERY_UNDERWEIGHT);
        }
        if (this.getWeight() <= 50.0) {
            this.parent.getCharacterTraits().add(CharacterTrait.EMACIATED);
        }
    }

    public boolean characterHaveWeightTrouble() {
        return this.parent.hasTrait(CharacterTrait.EMACIATED) || this.parent.hasTrait(CharacterTrait.OBESE) || this.parent.hasTrait(CharacterTrait.VERY_UNDERWEIGHT) || this.parent.hasTrait(CharacterTrait.VERY_UNDERWEIGHT) || this.parent.hasTrait(CharacterTrait.OVERWEIGHT);
    }

    public boolean canAddFitnessXp() {
        if (this.parent.getPerkLevel(PerkFactory.Perks.Fitness) >= 9 && this.characterHaveWeightTrouble()) {
            return false;
        }
        if (this.parent.getPerkLevel(PerkFactory.Perks.Fitness) >= 6) {
            return !this.parent.hasTrait(CharacterTrait.EMACIATED) && !this.parent.hasTrait(CharacterTrait.OBESE) && !this.parent.hasTrait(CharacterTrait.VERY_UNDERWEIGHT);
        }
        return true;
    }

    public float getCarbohydrates() {
        return this.carbohydrates;
    }

    public void setCarbohydrates(float carbohydrates) {
        if (carbohydrates < -500.0f) {
            carbohydrates = -500.0f;
        }
        if (carbohydrates > 1000.0f) {
            carbohydrates = 1000.0f;
        }
        this.carbohydrates = carbohydrates;
    }

    public float getProteins() {
        return this.proteins;
    }

    public void setProteins(float proteins) {
        if (proteins < -500.0f) {
            proteins = -500.0f;
        }
        if (proteins > 1000.0f) {
            proteins = 1000.0f;
        }
        this.proteins = proteins;
    }

    public float getCalories() {
        return this.calories;
    }

    public void setCalories(float calories) {
        if (calories < -2200.0f) {
            calories = -2200.0f;
        }
        if (calories > 3700.0f) {
            calories = 3700.0f;
        }
        this.calories = calories;
    }

    public float getLipids() {
        return this.lipids;
    }

    public void setLipids(float lipids) {
        if (lipids < -500.0f) {
            lipids = -500.0f;
        }
        if (lipids > 1000.0f) {
            lipids = 1000.0f;
        }
        this.lipids = lipids;
    }

    public double getWeight() {
        return this.weight;
    }

    public void setWeight(double weight) {
        if (weight < 35.0) {
            weight = 35.0;
            float lowWeightDamage = this.parent.getBodyDamage().getHealthReductionFromSevereBadMoodles() * GameTime.instance.getMultiplier();
            this.parent.getBodyDamage().ReduceGeneralHealth(lowWeightDamage);
            LuaEventManager.triggerEvent("OnPlayerGetDamage", this.parent, "LOWWEIGHT", Float.valueOf(lowWeightDamage));
        }
        this.weight = weight;
    }

    public boolean isIncWeight() {
        return this.incWeight;
    }

    public void setIncWeight(boolean incWeight) {
        this.incWeight = incWeight;
    }

    public boolean isIncWeightLot() {
        return this.incWeightLot;
    }

    public void setIncWeightLot(boolean incWeightLot) {
        this.incWeightLot = incWeightLot;
    }

    public boolean isDecWeight() {
        return this.decWeight;
    }

    public void setDecWeight(boolean decWeight) {
        this.decWeight = decWeight;
    }
}

