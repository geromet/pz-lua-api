/*
 * Decompiled with CFR 0.152.
 */
package zombie.characters;

import java.util.HashMap;
import java.util.Map;
import zombie.UsedFromLua;
import zombie.core.math.PZMath;

@UsedFromLua
public class CharacterStat {
    public static final Map<String, CharacterStat> REGISTRY = new HashMap<String, CharacterStat>();
    public static final CharacterStat ANGER = CharacterStat.register("Anger", 0.0f, 1.0f, 0.0f);
    public static final CharacterStat BOREDOM = CharacterStat.register("Boredom", 0.0f, 100.0f, 0.0f);
    public static final CharacterStat DISCOMFORT = CharacterStat.register("Discomfort", 0.0f, 100.0f, 0.0f);
    public static final CharacterStat ENDURANCE = CharacterStat.register("Endurance", 0.0f, 1.0f, 1.0f);
    public static final CharacterStat FATIGUE = CharacterStat.register("Fatigue", 0.0f, 1.0f, 0.0f);
    public static final CharacterStat FITNESS = CharacterStat.register("Fitness", -1.0f, 1.0f, 0.0f);
    public static final CharacterStat FOOD_SICKNESS = CharacterStat.register("FoodSickness", 0.0f, 100.0f, 0.0f);
    public static final CharacterStat HUNGER = CharacterStat.register("Hunger", 0.0f, 1.0f, 0.0f);
    public static final CharacterStat IDLENESS = CharacterStat.register("Idleness", 0.0f, 1.0f, 0.0f);
    public static final CharacterStat INTOXICATION = CharacterStat.register("Intoxication", 0.0f, 100.0f, 0.0f);
    public static final CharacterStat MORALE = CharacterStat.register("Morale", 0.0f, 1.0f, 1.0f);
    public static final CharacterStat NICOTINE_WITHDRAWAL = CharacterStat.register("NicotineWithdrawal", 0.0f, 0.51f, 0.0f);
    public static final CharacterStat PAIN = CharacterStat.register("Pain", 0.0f, 100.0f, 0.0f);
    public static final CharacterStat PANIC = CharacterStat.register("Panic", 0.0f, 100.0f, 0.0f);
    public static final CharacterStat POISON = CharacterStat.register("Poison", 0.0f, 100.0f, 0.0f);
    public static final CharacterStat SANITY = CharacterStat.register("Sanity", 0.0f, 1.0f, 1.0f);
    public static final CharacterStat SICKNESS = CharacterStat.register("Sickness", 0.0f, 1.0f, 0.0f);
    public static final CharacterStat STRESS = CharacterStat.register("Stress", 0.0f, 1.0f, 0.0f);
    public static final CharacterStat TEMPERATURE = CharacterStat.register("Temperature", 20.0f, 40.0f, 37.0f);
    public static final CharacterStat THIRST = CharacterStat.register("Thirst", 0.0f, 1.0f, 0.0f);
    public static final CharacterStat UNHAPPINESS = CharacterStat.register("Unhappiness", 0.0f, 100.0f, 0.0f);
    public static final CharacterStat WETNESS = CharacterStat.register("Wetness", 0.0f, 100.0f, 0.0f);
    public static final CharacterStat ZOMBIE_FEVER = CharacterStat.register("ZombieFever", 0.0f, 100.0f, 0.0f);
    public static final CharacterStat ZOMBIE_INFECTION = CharacterStat.register("ZombieInfection", 0.0f, 100.0f, 0.0f);
    public static final CharacterStat[] ORDERED_STATS = new CharacterStat[]{ANGER, BOREDOM, DISCOMFORT, ENDURANCE, FATIGUE, FITNESS, FOOD_SICKNESS, HUNGER, IDLENESS, INTOXICATION, MORALE, NICOTINE_WITHDRAWAL, PAIN, PANIC, POISON, SANITY, SICKNESS, STRESS, TEMPERATURE, THIRST, UNHAPPINESS, WETNESS, ZOMBIE_FEVER, ZOMBIE_INFECTION};
    private final String id;
    private final float minimumValue;
    private final float maximumValue;
    private final float defaultValue;

    private CharacterStat(String id, float minimumValue, float maximumValue, float defaultValue) {
        this.id = id;
        this.minimumValue = minimumValue;
        this.maximumValue = maximumValue;
        this.defaultValue = defaultValue;
    }

    public static CharacterStat register(String id, float minimumValue, float maximumValue, float defaultValue) {
        return REGISTRY.computeIfAbsent(id, key -> new CharacterStat((String)key, minimumValue, maximumValue, defaultValue));
    }

    public static CharacterStat getById(String id) {
        return REGISTRY.get(id);
    }

    public String getId() {
        return this.id;
    }

    public float getMinimumValue() {
        return this.minimumValue;
    }

    public float getMaximumValue() {
        return this.maximumValue;
    }

    public float clamp(float value) {
        return PZMath.clamp(value, this.minimumValue, this.maximumValue);
    }

    public float getDefaultValue() {
        return this.defaultValue;
    }

    public boolean isAtMinimum(float value) {
        return value <= this.minimumValue;
    }

    public boolean isAtMaximum(float value) {
        return value >= this.maximumValue;
    }

    public String toString() {
        return "CharacterStat{id='" + this.id + "', min=" + this.minimumValue + ", max=" + this.maximumValue + ", default=" + this.defaultValue + "}";
    }
}

