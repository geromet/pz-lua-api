/*
 * Decompiled with CFR 0.152.
 */
package zombie.scripting.objects;

import zombie.UsedFromLua;
import zombie.scripting.objects.Registries;
import zombie.scripting.objects.RegistryReset;
import zombie.scripting.objects.ResourceLocation;

@UsedFromLua
public class CharacterProfession {
    public static final CharacterProfession BURGLAR = CharacterProfession.registerBase("burglar");
    public static final CharacterProfession BURGER_FLIPPER = CharacterProfession.registerBase("burgerflipper");
    public static final CharacterProfession CARPENTER = CharacterProfession.registerBase("carpenter");
    public static final CharacterProfession CHEF = CharacterProfession.registerBase("chef");
    public static final CharacterProfession CONSTRUCTION_WORKER = CharacterProfession.registerBase("constructionworker");
    public static final CharacterProfession DOCTOR = CharacterProfession.registerBase("doctor");
    public static final CharacterProfession ELECTRICIAN = CharacterProfession.registerBase("electrician");
    public static final CharacterProfession ENGINEER = CharacterProfession.registerBase("engineer");
    public static final CharacterProfession FARMER = CharacterProfession.registerBase("farmer");
    public static final CharacterProfession FIRE_OFFICER = CharacterProfession.registerBase("fireofficer");
    public static final CharacterProfession FISHERMAN = CharacterProfession.registerBase("fisherman");
    public static final CharacterProfession FITNESS_INSTRUCTOR = CharacterProfession.registerBase("fitnessInstructor");
    public static final CharacterProfession LUMBERJACK = CharacterProfession.registerBase("lumberjack");
    public static final CharacterProfession MECHANICS = CharacterProfession.registerBase("mechanics");
    public static final CharacterProfession METALWORKER = CharacterProfession.registerBase("metalworker");
    public static final CharacterProfession NURSE = CharacterProfession.registerBase("nurse");
    public static final CharacterProfession PARK_RANGER = CharacterProfession.registerBase("parkranger");
    public static final CharacterProfession POLICE_OFFICER = CharacterProfession.registerBase("policeofficer");
    public static final CharacterProfession RANCHER = CharacterProfession.registerBase("rancher");
    public static final CharacterProfession REPAIRMAN = CharacterProfession.registerBase("repairman");
    public static final CharacterProfession SECURITY_GUARD = CharacterProfession.registerBase("securityguard");
    public static final CharacterProfession SMITHER = CharacterProfession.registerBase("smither");
    public static final CharacterProfession TAILOR = CharacterProfession.registerBase("tailor");
    public static final CharacterProfession UNEMPLOYED = CharacterProfession.registerBase("unemployed");
    public static final CharacterProfession VETERAN = CharacterProfession.registerBase("veteran");

    private CharacterProfession() {
    }

    public static CharacterProfession get(ResourceLocation id) {
        return Registries.CHARACTER_PROFESSION.get(id);
    }

    public String getName() {
        return Registries.CHARACTER_PROFESSION.getLocation(this).getPath();
    }

    public String toString() {
        return Registries.CHARACTER_PROFESSION.getLocation(this).toString();
    }

    public static CharacterProfession register(String id) {
        return CharacterProfession.register(false, id);
    }

    private static CharacterProfession registerBase(String id) {
        return CharacterProfession.register(true, id);
    }

    private static CharacterProfession register(boolean allowDefaultNamespace, String id) {
        return Registries.CHARACTER_PROFESSION.register(RegistryReset.createLocation(id, allowDefaultNamespace), new CharacterProfession());
    }
}

