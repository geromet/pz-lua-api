/*
 * Decompiled with CFR 0.152.
 */
package zombie.characters.animals;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import se.krka.kahlua.j2se.KahluaTableImpl;
import se.krka.kahlua.vm.KahluaTableIterator;
import zombie.GameTime;
import zombie.Lua.LuaManager;
import zombie.UsedFromLua;
import zombie.characters.animals.AnimalAllele;
import zombie.characters.animals.AnimalGenomeDefinitions;
import zombie.characters.animals.IsoAnimal;
import zombie.characters.animals.datas.AnimalBreed;
import zombie.characters.animals.datas.AnimalGrowStage;
import zombie.core.random.Rand;
import zombie.core.skinnedmodel.ModelManager;
import zombie.core.skinnedmodel.model.Model;
import zombie.debug.DebugLog;
import zombie.util.StringUtils;

@UsedFromLua
public class AnimalDefinitions {
    public String animalTypeStr;
    public Model bodyModel;
    public Model bodyModelSkel;
    public Model bodyModelFleece;
    public String bodyModelStr;
    public String bodyModelSkelNoHeadStr;
    public Model bodyModelSkelNoHead;
    public Model bodyModelHeadless;
    public String bodyModelFleeceStr;
    public String bodyModelSkelStr;
    public String bodyModelHeadlessStr;
    public String textureSkeleton;
    public String textureSkeletonBloody;
    public String textureRotten;
    public String textureSkinned;
    public String animset;
    public String mate;
    public float shadoww;
    public float shadowfm;
    public float shadowbm;
    public float turnDelta = 0.8f;
    public float animalSize;
    public float minSize;
    public float maxSize;
    public int minAge;
    public int minEnclosureSize;
    public String babyType;
    public int minAgeForBaby;
    public int maxAgeGeriatric;
    public boolean udder;
    public boolean female;
    public boolean male;
    public ArrayList<AnimalGrowStage> stages;
    public ArrayList<AnimalBreed> breeds;
    public ArrayList<AnimalAllele> genome;
    public boolean alwaysFleeHumans = true;
    public boolean fleeZombies = true;
    public boolean canBeAttached;
    public boolean canBeTransported;
    public float hungerMultiplier;
    public float thirstMultiplier;
    public float healthLossMultiplier = 0.05f;
    public float wanderMul = 400.0f;
    public int idleTypeNbr;
    public int eatingTypeNbr;
    public int sittingTypeNbr;
    public boolean eatFromMother;
    public boolean periodicRun;
    public int pregnantPeriod;
    public boolean eatGrass;
    public boolean sitRandomly;
    public ArrayList<String> eatTypeTrough;
    public boolean canBeMilked;
    public int minBaby = 1;
    public int maxBaby = 1;
    public int idleEmoteChance = 1000;
    public int eggsPerDay;
    public String eggType;
    public int fertilizedTimeMax;
    public int timeToHatch;
    public boolean canBePicked = true;
    public ArrayList<String> hutches;
    public int enterHutchTime;
    public int exitHutchTime;
    public ArrayList<String> genes;
    public float minMilk;
    public float maxMilk;
    public float maxWool;
    public float minWeight = 10.0f;
    public float maxWeight = 100.0f;
    public String carcassItem;
    public int attackDist = 1;
    public int attackTimer = 1000;
    public boolean dontAttackOtherMale;
    public boolean canBeFeedByHand;
    public float baseDmg = 0.5f;
    public String milkAnimPreset;
    public ArrayList<String> feedByHandType;
    public float trailerBaseSize;
    public boolean canBePet;
    public boolean attackBack;
    public float collisionSize;
    public float baseEncumbrance = 1.0f;
    public int matingPeriodStart;
    public int matingPeriodEnd;
    public int timeBeforeNextPregnancy;
    public float thirstHungerTrigger = 0.1f;
    public boolean collidable = true;
    public boolean canThump = true;
    public boolean wild;
    public int spottingDist = 10;
    public String group;
    public boolean canBeAlerted;
    public String dung;
    public boolean attackIfStressed;
    public int happyAnim;
    public String ropeBone = "Bip01_Head";
    public int minClutchSize = -1;
    public int maxClutchSize = -1;
    public int layEggPeriodStart = -1;
    public boolean stressAboveGround;
    public boolean canClimbStairs;
    public boolean stressUnderRain;
    public boolean canClimbFences;
    public boolean needMom = true;
    public boolean canBeDomesticated = true;
    public int dungChancePerDay = 50;
    public float hungerBoost = 1.0f;
    public float thirstBoost = 1.0f;
    public float distToEat = 2.0f;
    public boolean knockdownAttack;
    public int minBodyPart;
    public boolean canDoLaceration;
    public float maxBlood;
    public float minBlood;
    public boolean litterEatTogether;
    public boolean addTrackingXp = true;
    public float corpseSize = 1.0f;
    public float corpseLength = 1.0f;
    public float idleSoundRadius;
    public float idleSoundVolume;
    private float wildFleeTimeUntilDeadTimer;
    public boolean canBeKilledWithoutWeapon;
    public static HashMap<String, AnimalDefinitions> animalDefs;

    public static HashMap<String, AnimalDefinitions> getAnimalDefs() {
        if (animalDefs == null) {
            AnimalDefinitions.loadAnimalDefinitions();
        }
        return animalDefs;
    }

    public static ArrayList<AnimalDefinitions> getAnimalDefsArray() {
        if (animalDefs == null) {
            AnimalDefinitions.loadAnimalDefinitions();
        }
        ArrayList<AnimalDefinitions> result = new ArrayList<AnimalDefinitions>();
        for (AnimalDefinitions def : animalDefs.values()) {
            result.add(def);
        }
        return result;
    }

    public static void loadAnimalDefinitions() {
        DebugLog.General.println("loadAnimalDefinitions start");
        if (AnimalGenomeDefinitions.fullGenomeDef == null) {
            AnimalGenomeDefinitions.loadGenomeDefinition();
        }
        animalDefs = new HashMap();
        KahluaTableImpl definitions = (KahluaTableImpl)LuaManager.env.rawget("AnimalDefinitions");
        if (definitions == null) {
            return;
        }
        KahluaTableImpl animals = (KahluaTableImpl)definitions.rawget("animals");
        KahluaTableIterator iterator2 = animals.iterator();
        while (iterator2.advance()) {
            AnimalDefinitions def = new AnimalDefinitions();
            def.animalTypeStr = iterator2.getKey().toString();
            def.stages = new ArrayList();
            def.breeds = new ArrayList();
            def.genome = new ArrayList();
            KahluaTableIterator it2 = ((KahluaTableImpl)iterator2.getValue()).iterator();
            while (it2.advance()) {
                String key = it2.getKey().toString();
                Object value = it2.getValue();
                String valueStr = value.toString().trim();
                if ("bodyModel".equalsIgnoreCase(key)) {
                    def.bodyModelStr = value.toString();
                }
                if ("bodyModelFleece".equalsIgnoreCase(key)) {
                    def.bodyModelFleeceStr = value.toString();
                }
                if ("bodyModelHeadless".equalsIgnoreCase(key)) {
                    def.bodyModelHeadlessStr = value.toString();
                }
                if ("bodyModelSkelNoHead".equalsIgnoreCase(key)) {
                    def.bodyModelSkelNoHeadStr = value.toString();
                }
                if ("textureSkeleton".equalsIgnoreCase(key)) {
                    def.textureSkeleton = value.toString();
                }
                if ("textureSkeletonBloody".equalsIgnoreCase(key)) {
                    def.textureSkeletonBloody = value.toString();
                }
                if ("textureRotten".equalsIgnoreCase(key)) {
                    def.textureRotten = value.toString();
                }
                if ("textureSkinned".equalsIgnoreCase(key)) {
                    def.textureSkinned = value.toString();
                }
                if ("bodyModelSkel".equalsIgnoreCase(key)) {
                    def.bodyModelSkelStr = value.toString();
                }
                if ("carcassItem".equalsIgnoreCase(key)) {
                    def.carcassItem = valueStr;
                }
                if ("corpseSize".equalsIgnoreCase(key)) {
                    def.corpseSize = Float.parseFloat(valueStr);
                }
                if ("corpseLength".equalsIgnoreCase(key)) {
                    def.corpseLength = Float.parseFloat(valueStr);
                }
                if ("idleSoundRadius".equalsIgnoreCase(key)) {
                    def.idleSoundRadius = Float.parseFloat(valueStr);
                }
                if ("idleSoundVolume".equalsIgnoreCase(key)) {
                    def.idleSoundVolume = Float.parseFloat(valueStr);
                }
                if ("wildFleeTimeUntilDeadTimer".equalsIgnoreCase(key)) {
                    def.wildFleeTimeUntilDeadTimer = Float.parseFloat(valueStr);
                }
                if ("maxBlood".equalsIgnoreCase(key)) {
                    def.maxBlood = Float.parseFloat(valueStr);
                }
                if ("minBlood".equalsIgnoreCase(key)) {
                    def.minBlood = Float.parseFloat(valueStr);
                }
                if ("milkAnimPreset".equalsIgnoreCase(key)) {
                    def.milkAnimPreset = valueStr;
                }
                if ("animset".equalsIgnoreCase(key)) {
                    def.animset = valueStr;
                }
                if ("ropeBone".equalsIgnoreCase(key)) {
                    def.ropeBone = valueStr;
                }
                if ("mate".equalsIgnoreCase(key)) {
                    def.mate = valueStr;
                }
                if ("dung".equalsIgnoreCase(key)) {
                    def.dung = valueStr;
                }
                if ("shadoww".equalsIgnoreCase(key)) {
                    def.shadoww = Float.parseFloat(valueStr);
                }
                if ("shadowfm".equalsIgnoreCase(key)) {
                    def.shadowfm = Float.parseFloat(valueStr);
                }
                if ("shadowbm".equalsIgnoreCase(key)) {
                    def.shadowbm = Float.parseFloat(valueStr);
                }
                if ("turnDelta".equalsIgnoreCase(key)) {
                    def.turnDelta = Float.parseFloat(valueStr);
                }
                if ("animalSize".equalsIgnoreCase(key)) {
                    def.animalSize = Float.parseFloat(valueStr);
                }
                if ("minSize".equalsIgnoreCase(key)) {
                    def.minSize = Float.parseFloat(valueStr);
                }
                if ("maxSize".equalsIgnoreCase(key)) {
                    def.maxSize = Float.parseFloat(valueStr);
                }
                if ("hungerMultiplier".equalsIgnoreCase(key)) {
                    def.hungerMultiplier = Float.parseFloat(valueStr);
                }
                if ("thirstMultiplier".equalsIgnoreCase(key)) {
                    def.thirstMultiplier = Float.parseFloat(valueStr);
                }
                if ("healthLossMultiplier".equalsIgnoreCase(key)) {
                    def.healthLossMultiplier = Float.parseFloat(valueStr);
                }
                if ("wanderMul".equalsIgnoreCase(key)) {
                    def.wanderMul = Float.parseFloat(valueStr);
                }
                if ("minEnclosureSize".equalsIgnoreCase(key)) {
                    def.minEnclosureSize = Float.valueOf(valueStr).intValue();
                }
                if ("happyAnim".equalsIgnoreCase(key)) {
                    def.happyAnim = Float.valueOf(valueStr).intValue();
                }
                if ("minAge".equalsIgnoreCase(key)) {
                    def.minAge = Float.valueOf(valueStr).intValue();
                }
                if ("minAgeForBaby".equalsIgnoreCase(key)) {
                    def.minAgeForBaby = Float.valueOf(valueStr).intValue();
                }
                if ("attackDist".equalsIgnoreCase(key)) {
                    def.attackDist = Float.valueOf(valueStr).intValue();
                }
                if ("attackTimer".equalsIgnoreCase(key)) {
                    def.attackTimer = Float.valueOf(valueStr).intValue();
                }
                if ("timeBeforeNextPregnancy".equalsIgnoreCase(key)) {
                    def.timeBeforeNextPregnancy = Float.valueOf(valueStr).intValue();
                }
                if ("spottingDist".equalsIgnoreCase(key)) {
                    def.spottingDist = Float.valueOf(valueStr).intValue();
                }
                if ("dungChancePerDay".equalsIgnoreCase(key)) {
                    def.dungChancePerDay = Float.valueOf(valueStr).intValue();
                }
                if ("minBodyPart".equalsIgnoreCase(key)) {
                    def.minBodyPart = Float.valueOf(valueStr).intValue();
                }
                if ("minClutchSize".equalsIgnoreCase(key)) {
                    def.minClutchSize = Float.valueOf(valueStr).intValue();
                }
                if ("maxClutchSize".equalsIgnoreCase(key)) {
                    def.maxClutchSize = Float.valueOf(valueStr).intValue();
                }
                if ("layEggPeriodStart".equalsIgnoreCase(key)) {
                    def.layEggPeriodStart = Float.valueOf(valueStr).intValue();
                }
                if ("matingPeriodStart".equalsIgnoreCase(key)) {
                    def.matingPeriodStart = Float.valueOf(valueStr).intValue();
                }
                if ("matingPeriodEnd".equalsIgnoreCase(key)) {
                    def.matingPeriodEnd = Float.valueOf(valueStr).intValue();
                }
                if ("babyType".equalsIgnoreCase(key)) {
                    def.babyType = valueStr;
                }
                if ("group".equalsIgnoreCase(key)) {
                    def.group = valueStr;
                }
                if ("eggType".equalsIgnoreCase(key)) {
                    def.eggType = valueStr;
                }
                if ("maxAgeGeriatric".equalsIgnoreCase(key)) {
                    def.maxAgeGeriatric = Float.valueOf(valueStr).intValue();
                }
                if ("idleEmoteChance".equalsIgnoreCase(key)) {
                    def.idleEmoteChance = Float.valueOf(valueStr).intValue();
                }
                if ("eggsPerDay".equalsIgnoreCase(key)) {
                    def.eggsPerDay = Float.valueOf(valueStr).intValue();
                }
                if ("fertilizedTimeMax".equalsIgnoreCase(key)) {
                    def.fertilizedTimeMax = Float.valueOf(valueStr).intValue();
                }
                if ("timeToHatch".equalsIgnoreCase(key)) {
                    def.timeToHatch = Float.valueOf(valueStr).intValue();
                }
                if ("enterHutchTime".equalsIgnoreCase(key)) {
                    def.enterHutchTime = Float.valueOf(valueStr).intValue();
                }
                if ("exitHutchTime".equalsIgnoreCase(key)) {
                    def.exitHutchTime = Float.valueOf(valueStr).intValue();
                }
                if ("idleTypeNbr".equalsIgnoreCase(key)) {
                    def.idleTypeNbr = Float.valueOf(valueStr).intValue();
                }
                if ("eatingTypeNbr".equalsIgnoreCase(key)) {
                    def.eatingTypeNbr = Float.valueOf(valueStr).intValue();
                }
                if ("sittingTypeNbr".equalsIgnoreCase(key)) {
                    def.sittingTypeNbr = Float.valueOf(valueStr).intValue();
                }
                if ("pregnantPeriod".equalsIgnoreCase(key)) {
                    def.pregnantPeriod = Float.valueOf(valueStr).intValue();
                }
                if ("minWeight".equalsIgnoreCase(key)) {
                    def.minWeight = Float.parseFloat(valueStr);
                }
                if ("maxWeight".equalsIgnoreCase(key)) {
                    def.maxWeight = Float.parseFloat(valueStr);
                }
                if ("litterEatTogether".equalsIgnoreCase(key)) {
                    def.litterEatTogether = Boolean.parseBoolean(valueStr);
                }
                if ("udder".equalsIgnoreCase(key)) {
                    def.udder = Boolean.parseBoolean(valueStr);
                }
                if ("female".equalsIgnoreCase(key)) {
                    def.female = Boolean.parseBoolean(valueStr);
                }
                if ("male".equalsIgnoreCase(key)) {
                    def.male = Boolean.parseBoolean(valueStr);
                }
                if ("addTrackingXp".equalsIgnoreCase(key)) {
                    def.addTrackingXp = Boolean.parseBoolean(valueStr);
                }
                if ("fleeZombies".equalsIgnoreCase(key)) {
                    def.fleeZombies = Boolean.parseBoolean(valueStr);
                }
                if ("stressAboveGround".equalsIgnoreCase(key)) {
                    def.stressAboveGround = Boolean.parseBoolean(valueStr);
                }
                if ("stressUnderRain".equalsIgnoreCase(key)) {
                    def.stressUnderRain = Boolean.parseBoolean(valueStr);
                }
                if ("canClimbFences".equalsIgnoreCase(key)) {
                    def.canClimbFences = Boolean.parseBoolean(valueStr);
                }
                if ("needMom".equalsIgnoreCase(key)) {
                    def.needMom = Boolean.parseBoolean(valueStr);
                }
                if ("canBeDomesticated".equalsIgnoreCase(key)) {
                    def.canBeDomesticated = Boolean.parseBoolean(valueStr);
                }
                if ("knockdownAttack".equalsIgnoreCase(key)) {
                    def.knockdownAttack = Boolean.parseBoolean(valueStr);
                }
                if ("canDoLaceration".equalsIgnoreCase(key)) {
                    def.canDoLaceration = Boolean.parseBoolean(valueStr);
                }
                if ("canClimbStairs".equalsIgnoreCase(key)) {
                    def.canClimbStairs = Boolean.parseBoolean(valueStr);
                }
                if ("canBeAlerted".equalsIgnoreCase(key)) {
                    def.canBeAlerted = Boolean.parseBoolean(valueStr);
                }
                if ("attackIfStressed".equalsIgnoreCase(key)) {
                    def.attackIfStressed = Boolean.parseBoolean(valueStr);
                }
                if ("alwaysFleeHumans".equalsIgnoreCase(key)) {
                    def.alwaysFleeHumans = Boolean.parseBoolean(valueStr);
                }
                if ("canBeAttached".equalsIgnoreCase(key)) {
                    def.canBeAttached = Boolean.parseBoolean(valueStr);
                }
                if ("canBeTransported".equalsIgnoreCase(key)) {
                    def.canBeTransported = Boolean.parseBoolean(valueStr);
                }
                if ("eatFromMother".equalsIgnoreCase(key)) {
                    def.eatFromMother = Boolean.parseBoolean(valueStr);
                }
                if ("periodicRun".equalsIgnoreCase(key)) {
                    def.periodicRun = Boolean.parseBoolean(valueStr);
                }
                if ("eatGrass".equalsIgnoreCase(key)) {
                    def.eatGrass = Boolean.parseBoolean(valueStr);
                }
                if ("sitRandomly".equalsIgnoreCase(key)) {
                    def.sitRandomly = Boolean.parseBoolean(valueStr);
                }
                if ("canBeMilked".equalsIgnoreCase(key)) {
                    def.canBeMilked = Boolean.parseBoolean(valueStr);
                }
                if ("canBePicked".equalsIgnoreCase(key)) {
                    def.canBePicked = Boolean.parseBoolean(valueStr);
                }
                if ("collidable".equalsIgnoreCase(key)) {
                    def.collidable = Boolean.parseBoolean(valueStr);
                }
                if ("canThump".equalsIgnoreCase(key)) {
                    def.canThump = Boolean.parseBoolean(valueStr);
                }
                if ("wild".equalsIgnoreCase(key)) {
                    def.wild = Boolean.parseBoolean(valueStr);
                }
                if ("dontAttackOtherMale".equalsIgnoreCase(key)) {
                    def.dontAttackOtherMale = Boolean.parseBoolean(valueStr);
                }
                if ("canBePet".equalsIgnoreCase(key)) {
                    def.canBePet = Boolean.parseBoolean(valueStr);
                }
                if ("attackBack".equalsIgnoreCase(key)) {
                    def.attackBack = Boolean.parseBoolean(valueStr);
                }
                if ("canBeFeedByHand".equalsIgnoreCase(key)) {
                    def.canBeFeedByHand = Boolean.parseBoolean(valueStr);
                }
                if ("eatTypeTrough".equalsIgnoreCase(key)) {
                    def.eatTypeTrough = new ArrayList<String>(Arrays.asList(valueStr.split(",")));
                }
                if ("hutches".equalsIgnoreCase(key)) {
                    def.hutches = new ArrayList<String>(Arrays.asList(valueStr.split(",")));
                }
                if ("feedByHandType".equalsIgnoreCase(key)) {
                    def.feedByHandType = new ArrayList<String>(Arrays.asList(valueStr.split(",")));
                }
                if ("collisionSize".equalsIgnoreCase(key)) {
                    def.collisionSize = Float.parseFloat(valueStr);
                }
                if ("thirstHungerTrigger".equalsIgnoreCase(key)) {
                    def.thirstHungerTrigger = Float.parseFloat(valueStr);
                }
                if ("hungerBoost".equalsIgnoreCase(key)) {
                    def.hungerBoost = Float.parseFloat(valueStr);
                }
                if ("thirstBoost".equalsIgnoreCase(key)) {
                    def.thirstBoost = Float.parseFloat(valueStr);
                }
                if ("distToEat".equalsIgnoreCase(key)) {
                    def.distToEat = Float.parseFloat(valueStr);
                }
                if ("babyNbr".equalsIgnoreCase(key)) {
                    String[] split = valueStr.split(",");
                    def.minBaby = Integer.parseInt(split[0]);
                    def.maxBaby = Integer.parseInt(split[1]);
                }
                if ("maxMilk".equalsIgnoreCase(key)) {
                    def.maxMilk = Float.parseFloat(valueStr);
                }
                if ("baseDmg".equalsIgnoreCase(key)) {
                    def.baseDmg = Float.parseFloat(valueStr);
                }
                if ("minMilk".equalsIgnoreCase(key)) {
                    def.minMilk = Float.parseFloat(valueStr);
                }
                if ("trailerBaseSize".equalsIgnoreCase(key)) {
                    def.trailerBaseSize = Float.parseFloat(valueStr);
                }
                if ("maxWool".equalsIgnoreCase(key)) {
                    def.maxWool = Float.parseFloat(valueStr);
                }
                if ("baseEncumbrance".equalsIgnoreCase(key)) {
                    def.baseEncumbrance = Float.parseFloat(valueStr);
                }
                if ("stages".equalsIgnoreCase(key)) {
                    AnimalDefinitions.loadStages(def, (KahluaTableImpl)value);
                }
                if ("breeds".equalsIgnoreCase(key)) {
                    AnimalDefinitions.loadBreeds(def, (KahluaTableImpl)value);
                }
                if ("genes".equalsIgnoreCase(key)) {
                    AnimalDefinitions.loadGenes(def, (KahluaTableImpl)value);
                }
                if (!"canBeKilledWithoutWeapon".equalsIgnoreCase(key)) continue;
                def.canBeKilledWithoutWeapon = Boolean.parseBoolean(valueStr);
            }
            animalDefs.put(def.animalTypeStr, def);
        }
        for (AnimalDefinitions def : animalDefs.values()) {
            def.bodyModel = ModelManager.instance.getLoadedModel(def.bodyModelStr);
            if (!StringUtils.isNullOrEmpty(def.bodyModelFleeceStr)) {
                def.bodyModelFleece = ModelManager.instance.getLoadedModel(def.bodyModelFleeceStr);
            }
            if (!StringUtils.isNullOrEmpty(def.bodyModelSkelStr)) {
                def.bodyModelSkel = ModelManager.instance.getLoadedModel(def.bodyModelSkelStr);
            }
            if (!StringUtils.isNullOrEmpty(def.bodyModelHeadlessStr)) {
                def.bodyModelHeadless = ModelManager.instance.getLoadedModel(def.bodyModelHeadlessStr);
            }
            if (StringUtils.isNullOrEmpty(def.bodyModelSkelNoHeadStr)) continue;
            def.bodyModelSkelNoHead = ModelManager.instance.getLoadedModel(def.bodyModelSkelNoHeadStr);
        }
        DebugLog.General.println("loadAnimalDefinitions end");
    }

    private static void loadGenes(AnimalDefinitions def, KahluaTableImpl table) {
        KahluaTableIterator it = table.iterator();
        def.genes = new ArrayList();
        while (it.advance()) {
            String value = it.getValue().toString().trim();
            if (def.genes.contains(value)) continue;
            def.genes.add(value);
        }
    }

    private static void loadBreeds(AnimalDefinitions def, KahluaTableImpl table) {
        KahluaTableIterator it = table.iterator();
        while (it.advance()) {
            Object value = it.getValue();
            AnimalBreed breed = new AnimalBreed();
            breed.name = it.getKey().toString();
            KahluaTableIterator it2 = ((KahluaTableImpl)value).iterator();
            while (it2.advance()) {
                String key = it2.getKey().toString();
                Object value2 = it2.getValue();
                String valueStr = value2.toString();
                if ("texture".equalsIgnoreCase(key)) {
                    breed.texture = new ArrayList<String>(Arrays.asList(valueStr.split(",")));
                }
                if ("textureMale".equalsIgnoreCase(key)) {
                    breed.textureMale = valueStr;
                }
                if ("textureBaby".equalsIgnoreCase(key)) {
                    breed.textureBaby = valueStr;
                }
                if ("milkType".equalsIgnoreCase(key)) {
                    breed.milkType = valueStr;
                }
                if ("woolType".equalsIgnoreCase(key)) {
                    breed.woolType = valueStr;
                }
                if ("forcedGenes".equalsIgnoreCase(key)) {
                    breed.loadForcedGenes((KahluaTableImpl)value2);
                }
                if ("invIconMale".equalsIgnoreCase(key)) {
                    breed.invIconMale = valueStr;
                }
                if ("invIconFemale".equalsIgnoreCase(key)) {
                    breed.invIconFemale = valueStr;
                }
                if ("invIconBaby".equalsIgnoreCase(key)) {
                    breed.invIconBaby = valueStr;
                }
                if ("invIconMaleDead".equalsIgnoreCase(key)) {
                    breed.invIconMaleDead = valueStr;
                }
                if ("invIconFemaleDead".equalsIgnoreCase(key)) {
                    breed.invIconFemaleDead = valueStr;
                }
                if ("invIconBabyDead".equalsIgnoreCase(key)) {
                    breed.invIconBabyDead = valueStr;
                }
                if ("invIconMaleSkel".equalsIgnoreCase(key)) {
                    breed.invIconMaleSkel = valueStr;
                }
                if ("invIconFemaleSkel".equalsIgnoreCase(key)) {
                    breed.invIconFemaleSkel = valueStr;
                }
                if ("invIconBabySkel".equalsIgnoreCase(key)) {
                    breed.invIconBabySkel = valueStr;
                }
                if ("leather".equalsIgnoreCase(key)) {
                    breed.leather = valueStr;
                }
                if ("headItem".equalsIgnoreCase(key)) {
                    breed.headItem = valueStr;
                }
                if ("featherItem".equalsIgnoreCase(key)) {
                    breed.featherItem = valueStr;
                }
                if ("maxFeather".equalsIgnoreCase(key)) {
                    breed.maxFeather = Float.valueOf(valueStr).intValue();
                }
                if ("sounds".equalsIgnoreCase(key)) {
                    breed.loadSounds((KahluaTableImpl)value2);
                }
                if (!"rottenTexture".equalsIgnoreCase(key)) continue;
                breed.rottenTexture = valueStr;
            }
            def.breeds.add(breed);
        }
    }

    private static void loadStages(AnimalDefinitions def, KahluaTableImpl table) {
        KahluaTableIterator it = table.iterator();
        while (it.advance()) {
            Object value = it.getValue();
            AnimalGrowStage stage = new AnimalGrowStage();
            stage.stage = it.getKey().toString();
            KahluaTableIterator it2 = ((KahluaTableImpl)value).iterator();
            while (it2.advance()) {
                String key = it2.getKey().toString();
                Object value2 = it2.getValue();
                String valueStr = value2.toString();
                if ("ageToGrow".equalsIgnoreCase(key)) {
                    stage.ageToGrow = Float.valueOf(valueStr).intValue();
                }
                if ("nextStage".equalsIgnoreCase(key)) {
                    stage.nextStage = valueStr;
                }
                if (!"nextStageMale".equalsIgnoreCase(key)) continue;
                stage.nextStageMale = valueStr;
            }
            def.stages.add(stage);
        }
    }

    public AnimalBreed getBreedByName(String breedName) {
        for (int i = 0; i < this.breeds.size(); ++i) {
            if (!this.breeds.get((int)i).name.equals(breedName)) continue;
            return this.breeds.get(i);
        }
        return null;
    }

    public AnimalBreed getRandomBreed() {
        return this.breeds.get(Rand.Next(0, this.breeds.size()));
    }

    public static AnimalDefinitions getDef(IsoAnimal animal) {
        return animalDefs.get(animal.getAnimalType());
    }

    public static AnimalDefinitions getDef(String animalType) {
        return animalDefs.get(animalType);
    }

    public ArrayList<AnimalBreed> getBreeds() {
        return this.breeds;
    }

    public String getAnimalType() {
        return this.animalTypeStr;
    }

    public String getBodyModelStr() {
        return this.bodyModelStr;
    }

    public boolean isInsideHutchTime(Integer hour) {
        int exitHutchTime;
        int enterHutchTime;
        if (hour == null || hour < 0) {
            hour = GameTime.getInstance().getHour();
        }
        if ((enterHutchTime = this.enterHutchTime) < (exitHutchTime = this.exitHutchTime)) {
            return hour >= enterHutchTime && hour < exitHutchTime;
        }
        return hour < exitHutchTime || hour >= enterHutchTime;
    }

    public boolean isOutsideHutchTime() {
        return !this.isInsideHutchTime(null);
    }

    public String getGroup() {
        return this.group;
    }

    public static void Reset() {
        animalDefs = null;
        AnimalGenomeDefinitions.fullGenomeDef = null;
    }

    public boolean canBeSkeleton() {
        return this.textureSkeleton != null && this.bodyModelSkel != null;
    }

    public int getMinBaby() {
        return this.minBaby;
    }

    public int getMaxBaby() {
        return this.maxBaby;
    }

    public String getBabyType() {
        return this.babyType;
    }

    public float getWildFleeTimeUntilDeadTimer() {
        return this.wildFleeTimeUntilDeadTimer;
    }
}

