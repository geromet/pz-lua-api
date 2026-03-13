/*
 * Decompiled with CFR 0.152.
 */
package zombie.scripting.objects;

import zombie.UsedFromLua;
import zombie.scripting.objects.Registries;
import zombie.scripting.objects.RegistryReset;
import zombie.scripting.objects.ResourceLocation;

@UsedFromLua
public class MoodleType {
    public static final MoodleType ENDURANCE = MoodleType.registerBase("Endurance");
    public static final MoodleType TIRED = MoodleType.registerBase("Tired");
    public static final MoodleType HUNGRY = MoodleType.registerBase("Hungry");
    public static final MoodleType PANIC = MoodleType.registerBase("Panic");
    public static final MoodleType SICK = MoodleType.registerBase("Sick");
    public static final MoodleType BORED = MoodleType.registerBase("Bored");
    public static final MoodleType UNHAPPY = MoodleType.registerBase("Unhappy");
    public static final MoodleType BLEEDING = MoodleType.registerBase("Bleeding");
    public static final MoodleType WET = MoodleType.registerBase("Wet");
    public static final MoodleType HAS_A_COLD = MoodleType.registerBase("HasACold");
    public static final MoodleType ANGRY = MoodleType.registerBase("Angry");
    public static final MoodleType STRESS = MoodleType.registerBase("Stress");
    public static final MoodleType THIRST = MoodleType.registerBase("Thirst");
    public static final MoodleType INJURED = MoodleType.registerBase("Injured");
    public static final MoodleType PAIN = MoodleType.registerBase("Pain");
    public static final MoodleType HEAVY_LOAD = MoodleType.registerBase("HeavyLoad");
    public static final MoodleType DRUNK = MoodleType.registerBase("Drunk");
    public static final MoodleType DEAD = MoodleType.registerBase("Dead");
    public static final MoodleType ZOMBIE = MoodleType.registerBase("Zombie");
    public static final MoodleType HYPERTHERMIA = MoodleType.registerBase("Hyperthermia");
    public static final MoodleType HYPOTHERMIA = MoodleType.registerBase("Hypothermia");
    public static final MoodleType WINDCHILL = MoodleType.registerBase("Windchill");
    public static final MoodleType CANT_SPRINT = MoodleType.registerBase("CantSprint");
    public static final MoodleType UNCOMFORTABLE = MoodleType.registerBase("Uncomfortable");
    public static final MoodleType NOXIOUS_SMELL = MoodleType.registerBase("NoxiousSmell");
    public static final MoodleType FOOD_EATEN = MoodleType.registerBase("FoodEaten");
    private final String translationName;

    private MoodleType(String translationName) {
        this.translationName = translationName;
    }

    public static MoodleType get(ResourceLocation id) {
        return Registries.MOODLE_TYPE.get(id);
    }

    public String toString() {
        return Registries.MOODLE_TYPE.getLocation(this).toString();
    }

    public String getTranslationName() {
        return this.translationName;
    }

    public static MoodleType register(String id) {
        return MoodleType.register(false, id);
    }

    private static MoodleType registerBase(String id) {
        return MoodleType.register(true, id);
    }

    private static MoodleType register(boolean allowDefaultNamespace, String id) {
        return Registries.MOODLE_TYPE.register(RegistryReset.createLocation(id, allowDefaultNamespace), new MoodleType(id));
    }
}

