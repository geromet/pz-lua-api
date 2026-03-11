/*
 * Decompiled with CFR 0.152.
 */
package zombie.scripting.objects;

import zombie.UsedFromLua;
import zombie.scripting.objects.Registries;
import zombie.scripting.objects.RegistryReset;
import zombie.scripting.objects.ResourceLocation;

@UsedFromLua
public class CharacterTrait {
    public static final CharacterTrait ADRENALINE_JUNKIE = CharacterTrait.registerBase("AdrenalineJunkie");
    public static final CharacterTrait AGORAPHOBIC = CharacterTrait.registerBase("Agoraphobic");
    public static final CharacterTrait ALL_THUMBS = CharacterTrait.registerBase("AllThumbs");
    public static final CharacterTrait ARTISAN = CharacterTrait.registerBase("Artisan");
    public static final CharacterTrait ASTHMATIC = CharacterTrait.registerBase("Asthmatic");
    public static final CharacterTrait ATHLETIC = CharacterTrait.registerBase("Athletic");
    public static final CharacterTrait AXEMAN = CharacterTrait.registerBase("Axeman");
    public static final CharacterTrait BASEBALL_PLAYER = CharacterTrait.registerBase("BaseballPlayer");
    public static final CharacterTrait BLACKSMITH = CharacterTrait.registerBase("Blacksmith");
    public static final CharacterTrait BLACKSMITH2 = CharacterTrait.registerBase("Blacksmith2");
    public static final CharacterTrait BRAVE = CharacterTrait.registerBase("Brave");
    public static final CharacterTrait BRAWLER = CharacterTrait.registerBase("Brawler");
    public static final CharacterTrait BURGLAR = CharacterTrait.registerBase("Burglar");
    public static final CharacterTrait CLAUSTROPHOBIC = CharacterTrait.registerBase("Claustrophobic");
    public static final CharacterTrait CLUMSY = CharacterTrait.registerBase("Clumsy");
    public static final CharacterTrait CONSPICUOUS = CharacterTrait.registerBase("Conspicuous");
    public static final CharacterTrait COOK = CharacterTrait.registerBase("Cook");
    public static final CharacterTrait COOK2 = CharacterTrait.registerBase("Cook2");
    public static final CharacterTrait COWARDLY = CharacterTrait.registerBase("Cowardly");
    public static final CharacterTrait CRAFTY = CharacterTrait.registerBase("Crafty");
    public static final CharacterTrait DEAF = CharacterTrait.registerBase("Deaf");
    public static final CharacterTrait DESENSITIZED = CharacterTrait.registerBase("Desensitized");
    public static final CharacterTrait DEXTROUS = CharacterTrait.registerBase("Dextrous");
    public static final CharacterTrait DISORGANIZED = CharacterTrait.registerBase("Disorganized");
    public static final CharacterTrait EAGLE_EYED = CharacterTrait.registerBase("EagleEyed");
    public static final CharacterTrait EMACIATED = CharacterTrait.registerBase("Emaciated");
    public static final CharacterTrait FAST_HEALER = CharacterTrait.registerBase("FastHealer");
    public static final CharacterTrait FAST_LEARNER = CharacterTrait.registerBase("FastLearner");
    public static final CharacterTrait FAST_READER = CharacterTrait.registerBase("FastReader");
    public static final CharacterTrait FEEBLE = CharacterTrait.registerBase("Feeble");
    public static final CharacterTrait FIRST_AID = CharacterTrait.registerBase("FirstAid");
    public static final CharacterTrait FISHING = CharacterTrait.registerBase("Fishing");
    public static final CharacterTrait FIT = CharacterTrait.registerBase("Fit");
    public static final CharacterTrait GARDENER = CharacterTrait.registerBase("Gardener");
    public static final CharacterTrait GRACEFUL = CharacterTrait.registerBase("Graceful");
    public static final CharacterTrait GYMNAST = CharacterTrait.registerBase("Gymnast");
    public static final CharacterTrait HANDY = CharacterTrait.registerBase("Handy");
    public static final CharacterTrait HARD_OF_HEARING = CharacterTrait.registerBase("HardOfHearing");
    public static final CharacterTrait HEARTY_APPETITE = CharacterTrait.registerBase("HeartyAppetite");
    public static final CharacterTrait HEMOPHOBIC = CharacterTrait.registerBase("Hemophobic");
    public static final CharacterTrait HERBALIST = CharacterTrait.registerBase("Herbalist");
    public static final CharacterTrait HIKER = CharacterTrait.registerBase("Hiker");
    public static final CharacterTrait HIGH_THIRST = CharacterTrait.registerBase("HighThirst");
    public static final CharacterTrait HUNTER = CharacterTrait.registerBase("Hunter");
    public static final CharacterTrait ILLITERATE = CharacterTrait.registerBase("Illiterate");
    public static final CharacterTrait INCONSPICUOUS = CharacterTrait.registerBase("Inconspicuous");
    public static final CharacterTrait INVENTIVE = CharacterTrait.registerBase("Inventive");
    public static final CharacterTrait IRON_GUT = CharacterTrait.registerBase("IronGut");
    public static final CharacterTrait INSOMNIAC = CharacterTrait.registerBase("Insomniac");
    public static final CharacterTrait JOGGER = CharacterTrait.registerBase("Jogger");
    public static final CharacterTrait KEEN_HEARING = CharacterTrait.registerBase("KeenHearing");
    public static final CharacterTrait LIGHT_EATER = CharacterTrait.registerBase("LightEater");
    public static final CharacterTrait LOW_THIRST = CharacterTrait.registerBase("LowThirst");
    public static final CharacterTrait MARKSMAN = CharacterTrait.registerBase("Marksman");
    public static final CharacterTrait MASON = CharacterTrait.registerBase("Mason");
    public static final CharacterTrait MECHANICS = CharacterTrait.registerBase("Mechanics");
    public static final CharacterTrait MECHANICS2 = CharacterTrait.registerBase("Mechanics2");
    public static final CharacterTrait NEEDS_LESS_SLEEP = CharacterTrait.registerBase("NeedsLessSleep");
    public static final CharacterTrait NEEDS_MORE_SLEEP = CharacterTrait.registerBase("NeedsMoreSleep");
    public static final CharacterTrait NIGHT_OWL = CharacterTrait.registerBase("NightOwl");
    public static final CharacterTrait NIGHT_VISION = CharacterTrait.registerBase("NightVision");
    public static final CharacterTrait NUTRITIONIST = CharacterTrait.registerBase("Nutritionist");
    public static final CharacterTrait NUTRITIONIST2 = CharacterTrait.registerBase("Nutritionist2");
    public static final CharacterTrait OBESE = CharacterTrait.registerBase("Obese");
    public static final CharacterTrait ORGANIZED = CharacterTrait.registerBase("Organized");
    public static final CharacterTrait OUTDOORSMAN = CharacterTrait.registerBase("Outdoorsman");
    public static final CharacterTrait OUT_OF_SHAPE = CharacterTrait.registerBase("Out of Shape");
    public static final CharacterTrait OVERWEIGHT = CharacterTrait.registerBase("Overweight");
    public static final CharacterTrait PACIFIST = CharacterTrait.registerBase("Pacifist");
    public static final CharacterTrait PRONE_TO_ILLNESS = CharacterTrait.registerBase("ProneToIllness");
    public static final CharacterTrait RESILIENT = CharacterTrait.registerBase("Resilient");
    public static final CharacterTrait SCOUT = CharacterTrait.registerBase("FormerScout");
    public static final CharacterTrait SHORT_SIGHTED = CharacterTrait.registerBase("ShortSighted");
    public static final CharacterTrait SLOW_HEALER = CharacterTrait.registerBase("SlowHealer");
    public static final CharacterTrait SLOW_LEARNER = CharacterTrait.registerBase("SlowLearner");
    public static final CharacterTrait SLOW_READER = CharacterTrait.registerBase("SlowReader");
    public static final CharacterTrait SMOKER = CharacterTrait.registerBase("Smoker");
    public static final CharacterTrait SPEED_DEMON = CharacterTrait.registerBase("SpeedDemon");
    public static final CharacterTrait STOUT = CharacterTrait.registerBase("Stout");
    public static final CharacterTrait STRONG = CharacterTrait.registerBase("Strong");
    public static final CharacterTrait SUNDAY_DRIVER = CharacterTrait.registerBase("SundayDriver");
    public static final CharacterTrait TAILOR = CharacterTrait.registerBase("Tailor");
    public static final CharacterTrait THICK_SKINNED = CharacterTrait.registerBase("ThickSkinned");
    public static final CharacterTrait THIN_SKINNED = CharacterTrait.registerBase("ThinSkinned");
    public static final CharacterTrait UNDERWEIGHT = CharacterTrait.registerBase("Underweight");
    public static final CharacterTrait UNFIT = CharacterTrait.registerBase("Unfit");
    public static final CharacterTrait VERY_UNDERWEIGHT = CharacterTrait.registerBase("Very Underweight");
    public static final CharacterTrait WEAK = CharacterTrait.registerBase("Weak");
    public static final CharacterTrait WEAK_STOMACH = CharacterTrait.registerBase("WeakStomach");
    public static final CharacterTrait WEIGHT_GAIN = CharacterTrait.registerBase("WeightGain");
    public static final CharacterTrait WEIGHT_LOSS = CharacterTrait.registerBase("WeightLoss");
    public static final CharacterTrait WHITTLER = CharacterTrait.registerBase("Whittler");
    public static final CharacterTrait WILDERNESS_KNOWLEDGE = CharacterTrait.registerBase("WildernessKnowledge");

    private CharacterTrait() {
    }

    public static CharacterTrait get(ResourceLocation id) {
        return Registries.CHARACTER_TRAIT.get(id);
    }

    public String getName() {
        return Registries.CHARACTER_TRAIT.getLocation(this).getPath();
    }

    public String toString() {
        return Registries.CHARACTER_TRAIT.getLocation(this).toString();
    }

    public static CharacterTrait register(String id) {
        return CharacterTrait.register(false, id);
    }

    private static CharacterTrait registerBase(String id) {
        return CharacterTrait.register(true, id);
    }

    private static CharacterTrait register(boolean allowDefaultNamespace, String id) {
        return Registries.CHARACTER_TRAIT.register(RegistryReset.createLocation(id, allowDefaultNamespace), new CharacterTrait());
    }
}

