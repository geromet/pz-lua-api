/*
 * Decompiled with CFR 0.152.
 */
package zombie;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import se.krka.kahlua.vm.KahluaTable;
import se.krka.kahlua.vm.KahluaTableIterator;
import zombie.GameTime;
import zombie.GameWindow;
import zombie.Lua.LuaManager;
import zombie.UsedFromLua;
import zombie.VirtualZombieManager;
import zombie.ZomboidFileSystem;
import zombie.config.BooleanConfigOption;
import zombie.config.ConfigFile;
import zombie.config.ConfigOption;
import zombie.config.DoubleConfigOption;
import zombie.config.EnumConfigOption;
import zombie.config.IntegerConfigOption;
import zombie.config.StringConfigOption;
import zombie.core.Core;
import zombie.core.Translator;
import zombie.core.logger.ExceptionLogger;
import zombie.core.random.Rand;
import zombie.debug.DebugLog;
import zombie.inventory.ItemPickerJava;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoWorld;
import zombie.iso.SliceY;
import zombie.network.GameClient;
import zombie.network.GameServer;
import zombie.network.ServerSettingsManager;
import zombie.sandbox.CustomBooleanSandboxOption;
import zombie.sandbox.CustomDoubleSandboxOption;
import zombie.sandbox.CustomEnumSandboxOption;
import zombie.sandbox.CustomIntegerSandboxOption;
import zombie.sandbox.CustomSandboxOption;
import zombie.sandbox.CustomSandboxOptions;
import zombie.sandbox.CustomStringSandboxOption;

@UsedFromLua
public final class SandboxOptions {
    public static final SandboxOptions instance = new SandboxOptions();
    public static final int FIRST_YEAR = 1993;
    public int speed = 3;
    private final ArrayList<SandboxOption> options = new ArrayList();
    private final HashMap<String, SandboxOption> optionByName = new HashMap();
    public final EnumSandboxOption zombies = this.newEnumOption("Zombies", 6, 4).setTranslation("ZombieCount");
    public final EnumSandboxOption distribution = this.newEnumOption("Distribution", 2, 1).setTranslation("ZombieDistribution");
    public final BooleanSandboxOption zombieVoronoiNoise = this.newBooleanOption("ZombieVoronoiNoise", true);
    public final EnumSandboxOption zombieRespawn = this.newEnumOption("ZombieRespawn", 4, 2).setTranslation("ZombieRespawn");
    public final BooleanSandboxOption zombieMigrate = this.newBooleanOption("ZombieMigrate", true).setTranslation("ZombieMigrate");
    public final EnumSandboxOption dayLength = this.newEnumOption("DayLength", 27, 4);
    public final EnumSandboxOption startYear = this.newEnumOption("StartYear", 100, 1);
    public final EnumSandboxOption startMonth = this.newEnumOption("StartMonth", 12, 7);
    public final EnumSandboxOption startDay = this.newEnumOption("StartDay", 31, 23);
    public final EnumSandboxOption startTime = this.newEnumOption("StartTime", 9, 2);
    public final EnumSandboxOption dayNightCycle = this.newEnumOption("DayNightCycle", 3, 1).setValueTranslation("DayNightCycle");
    public final EnumSandboxOption climateCycle = this.newEnumOption("ClimateCycle", 6, 1).setValueTranslation("ClimateCycle");
    public final EnumSandboxOption fogCycle = this.newEnumOption("FogCycle", 3, 1).setValueTranslation("FogCycle");
    public final EnumSandboxOption waterShut = this.newEnumOption("WaterShut", 9, 2).setValueTranslation("Shutoff");
    public final EnumSandboxOption elecShut = this.newEnumOption("ElecShut", 9, 2);
    public final EnumSandboxOption alarmDecay = this.newEnumOption("AlarmDecay", 6, 2).setValueTranslation("Shutoff");
    public final IntegerSandboxOption waterShutModifier = this.newIntegerOption("WaterShutModifier", -1, Integer.MAX_VALUE, 14).setTranslation("WaterShut");
    public final IntegerSandboxOption elecShutModifier = this.newIntegerOption("ElecShutModifier", -1, Integer.MAX_VALUE, 14).setTranslation("ElecShut");
    public final IntegerSandboxOption alarmDecayModifier = this.newIntegerOption("AlarmDecayModifier", -1, Integer.MAX_VALUE, 14).setTranslation("AlarmDecay");
    public final DoubleSandboxOption foodLootNew = this.newDoubleOption("FoodLootNew", 0.0, 4.0, 0.6);
    public final DoubleSandboxOption literatureLootNew = this.newDoubleOption("LiteratureLootNew", 0.0, 4.0, 0.6);
    public final DoubleSandboxOption skillBookLoot = this.newDoubleOption("SkillBookLoot", 0.0, 4.0, 0.6);
    public final DoubleSandboxOption recipeResourceLoot = this.newDoubleOption("RecipeResourceLoot", 0.0, 4.0, 0.6);
    public final DoubleSandboxOption medicalLootNew = this.newDoubleOption("MedicalLootNew", 0.0, 4.0, 0.6);
    public final DoubleSandboxOption survivalGearsLootNew = this.newDoubleOption("SurvivalGearsLootNew", 0.0, 4.0, 0.6);
    public final DoubleSandboxOption cannedFoodLootNew = this.newDoubleOption("CannedFoodLootNew", 0.0, 4.0, 0.6);
    public final DoubleSandboxOption weaponLootNew = this.newDoubleOption("WeaponLootNew", 0.0, 4.0, 0.6);
    public final DoubleSandboxOption rangedWeaponLootNew = this.newDoubleOption("RangedWeaponLootNew", 0.0, 4.0, 0.6);
    public final DoubleSandboxOption ammoLootNew = this.newDoubleOption("AmmoLootNew", 0.0, 4.0, 0.6);
    public final DoubleSandboxOption mechanicsLootNew = this.newDoubleOption("MechanicsLootNew", 0.0, 4.0, 0.6);
    public final DoubleSandboxOption otherLootNew = this.newDoubleOption("OtherLootNew", 0.0, 4.0, 0.6);
    public final DoubleSandboxOption clothingLootNew = this.newDoubleOption("ClothingLootNew", 0.0, 4.0, 0.6);
    public final DoubleSandboxOption containerLootNew = this.newDoubleOption("ContainerLootNew", 0.0, 4.0, 0.6);
    public final DoubleSandboxOption keyLootNew = this.newDoubleOption("KeyLootNew", 0.0, 4.0, 0.6);
    public final DoubleSandboxOption mediaLootNew = this.newDoubleOption("MediaLootNew", 0.0, 4.0, 0.6);
    public final DoubleSandboxOption mementoLootNew = this.newDoubleOption("MementoLootNew", 0.0, 4.0, 0.6);
    public final DoubleSandboxOption cookwareLootNew = this.newDoubleOption("CookwareLootNew", 0.0, 4.0, 0.6);
    public final DoubleSandboxOption materialLootNew = this.newDoubleOption("MaterialLootNew", 0.0, 4.0, 0.6);
    public final DoubleSandboxOption farmingLootNew = this.newDoubleOption("FarmingLootNew", 0.0, 4.0, 0.6);
    public final DoubleSandboxOption toolLootNew = this.newDoubleOption("ToolLootNew", 0.0, 4.0, 0.6);
    public final DoubleSandboxOption rollsMultiplier = this.newDoubleOption("RollsMultiplier", 0.1, 100.0, 1.0);
    public final StringSandboxOption lootItemRemovalList = this.newStringOption("LootItemRemovalList", "", -1);
    public final BooleanSandboxOption removeStoryLoot = this.newBooleanOption("RemoveStoryLoot", false);
    public final BooleanSandboxOption removeZombieLoot = this.newBooleanOption("RemoveZombieLoot", false);
    public final IntegerSandboxOption zombiePopLootEffect = this.newIntegerOption("ZombiePopLootEffect", 0, 20, 10).setTranslation("ZombiePopLootEffect");
    public final DoubleSandboxOption insaneLootFactor = this.newDoubleOption("InsaneLootFactor", 0.0, 0.2, 0.05);
    public final DoubleSandboxOption extremeLootFactor = this.newDoubleOption("ExtremeLootFactor", 0.05, 0.6, 0.2);
    public final DoubleSandboxOption rareLootFactor = this.newDoubleOption("RareLootFactor", 0.2, 1.0, 0.6);
    public final DoubleSandboxOption normalLootFactor = this.newDoubleOption("NormalLootFactor", 0.6, 2.0, 1.0);
    public final DoubleSandboxOption commonLootFactor = this.newDoubleOption("CommonLootFactor", 1.0, 3.0, 2.0);
    public final DoubleSandboxOption abundantLootFactor = this.newDoubleOption("AbundantLootFactor", 2.0, 4.0, 3.0);
    public final EnumSandboxOption temperature = this.newEnumOption("Temperature", 5, 3).setTranslation("WorldTemperature");
    public final EnumSandboxOption rain = this.newEnumOption("Rain", 5, 3).setTranslation("RainAmount");
    public final EnumSandboxOption erosionSpeed = this.newEnumOption("ErosionSpeed", 5, 3);
    public final IntegerSandboxOption erosionDays = this.newIntegerOption("ErosionDays", -1, 36500, 0);
    public final EnumSandboxOption farming = this.newEnumOption("Farming", 5, 3).setTranslation("FarmingSpeed");
    public final EnumSandboxOption compostTime = this.newEnumOption("CompostTime", 8, 2);
    public final EnumSandboxOption statsDecrease = this.newEnumOption("StatsDecrease", 5, 3).setTranslation("StatDecrease");
    public final EnumSandboxOption natureAbundance = this.newEnumOption("NatureAbundance", 5, 3).setTranslation("NatureAmount");
    public final EnumSandboxOption alarm = this.newEnumOption("Alarm", 6, 4).setTranslation("HouseAlarmFrequency");
    public final EnumSandboxOption lockedHouses = this.newEnumOption("LockedHouses", 6, 4).setTranslation("LockedHouseFrequency");
    public final BooleanSandboxOption starterKit = this.newBooleanOption("StarterKit", false);
    public final BooleanSandboxOption nutrition = this.newBooleanOption("Nutrition", false);
    public final EnumSandboxOption foodRotSpeed = this.newEnumOption("FoodRotSpeed", 5, 3).setTranslation("FoodSpoil");
    public final EnumSandboxOption fridgeFactor = this.newEnumOption("FridgeFactor", 6, 3).setTranslation("FridgeEffect");
    public final IntegerSandboxOption seenHoursPreventLootRespawn = this.newIntegerOption("SeenHoursPreventLootRespawn", 0, Integer.MAX_VALUE, 0);
    public final IntegerSandboxOption hoursForLootRespawn = this.newIntegerOption("HoursForLootRespawn", 0, Integer.MAX_VALUE, 0);
    public final IntegerSandboxOption maxItemsForLootRespawn = this.newIntegerOption("MaxItemsForLootRespawn", 0, Integer.MAX_VALUE, 5);
    public final BooleanSandboxOption constructionPreventsLootRespawn = this.newBooleanOption("ConstructionPreventsLootRespawn", true);
    public final StringSandboxOption worldItemRemovalList = this.newStringOption("WorldItemRemovalList", "Base.Hat,Base.Glasses,Base.Dung_Turkey,Base.Dung_Chicken,Base.Dung_Cow,Base.Dung_Deer,Base.Dung_Mouse,Base.Dung_Pig,Base.Dung_Rabbit,Base.Dung_Rat,Base.Dung_Sheep", -1);
    public final DoubleSandboxOption hoursForWorldItemRemoval = this.newDoubleOption("HoursForWorldItemRemoval", 0.0, 2.147483647E9, 24.0);
    public final BooleanSandboxOption itemRemovalListBlacklistToggle = this.newBooleanOption("ItemRemovalListBlacklistToggle", false);
    public final EnumSandboxOption timeSinceApo = this.newEnumOption("TimeSinceApo", 13, 1);
    public final EnumSandboxOption plantResilience = this.newEnumOption("PlantResilience", 5, 3);
    public final EnumSandboxOption plantAbundance = this.newEnumOption("PlantAbundance", 5, 3).setValueTranslation("FarmingAmount");
    public final EnumSandboxOption endRegen = this.newEnumOption("EndRegen", 5, 3).setTranslation("EnduranceRegen");
    public final EnumSandboxOption helicopter = this.newEnumOption("Helicopter", 4, 2).setValueTranslation("HelicopterFreq");
    public final EnumSandboxOption metaEvent = this.newEnumOption("MetaEvent", 3, 2).setValueTranslation("MetaEventFreq");
    public final EnumSandboxOption sleepingEvent = this.newEnumOption("SleepingEvent", 3, 1).setValueTranslation("MetaEventFreq");
    public final DoubleSandboxOption generatorFuelConsumption = this.newDoubleOption("GeneratorFuelConsumption", 0.0, 100.0, 0.1);
    public final EnumSandboxOption generatorSpawning = this.newEnumOption("GeneratorSpawning", 7, 5);
    public final EnumSandboxOption annotatedMapChance = this.newEnumOption("AnnotatedMapChance", 6, 4);
    public final IntegerSandboxOption characterFreePoints = this.newIntegerOption("CharacterFreePoints", -100, 100, 0);
    public final EnumSandboxOption constructionBonusPoints = this.newEnumOption("ConstructionBonusPoints", 5, 3);
    public final EnumSandboxOption nightDarkness = this.newEnumOption("NightDarkness", 4, 3);
    public final EnumSandboxOption nightLength = this.newEnumOption("NightLength", 5, 3);
    public final BooleanSandboxOption boneFracture = this.newBooleanOption("BoneFracture", true);
    public final EnumSandboxOption injurySeverity = this.newEnumOption("InjurySeverity", 3, 2);
    public final DoubleSandboxOption hoursForCorpseRemoval = this.newDoubleOption("HoursForCorpseRemoval", -1.0, 2.147483647E9, -1.0);
    public final EnumSandboxOption decayingCorpseHealthImpact = this.newEnumOption("DecayingCorpseHealthImpact", 5, 3);
    public final BooleanSandboxOption zombieHealthImpact = this.newBooleanOption("ZombieHealthImpact", false);
    public final EnumSandboxOption bloodLevel = this.newEnumOption("BloodLevel", 5, 3);
    public final EnumSandboxOption clothingDegradation = this.newEnumOption("ClothingDegradation", 4, 3);
    public final BooleanSandboxOption fireSpread = this.newBooleanOption("FireSpread", true);
    public final IntegerSandboxOption daysForRottenFoodRemoval = this.newIntegerOption("DaysForRottenFoodRemoval", -1, Integer.MAX_VALUE, -1);
    public final BooleanSandboxOption allowExteriorGenerator = this.newBooleanOption("AllowExteriorGenerator", true);
    public final EnumSandboxOption maxFogIntensity = this.newEnumOption("MaxFogIntensity", 4, 1);
    public final EnumSandboxOption maxRainFxIntensity = this.newEnumOption("MaxRainFxIntensity", 3, 1);
    public final BooleanSandboxOption enableSnowOnGround = this.newBooleanOption("EnableSnowOnGround", true);
    public final BooleanSandboxOption attackBlockMovements = this.newBooleanOption("AttackBlockMovements", true);
    public final EnumSandboxOption survivorHouseChance = this.newEnumOption("SurvivorHouseChance", 7, 3);
    public final EnumSandboxOption vehicleStoryChance = this.newEnumOption("VehicleStoryChance", 7, 3).setValueTranslation("SurvivorHouseChance");
    public final EnumSandboxOption zoneStoryChance = this.newEnumOption("ZoneStoryChance", 7, 3).setValueTranslation("SurvivorHouseChance");
    public final BooleanSandboxOption allClothesUnlocked = this.newBooleanOption("AllClothesUnlocked", false);
    public final BooleanSandboxOption enableTaintedWaterText = this.newBooleanOption("EnableTaintedWaterText", true);
    public final BooleanSandboxOption enableVehicles = this.newBooleanOption("EnableVehicles", true);
    public final EnumSandboxOption carSpawnRate = this.newEnumOption("CarSpawnRate", 5, 4);
    public final DoubleSandboxOption zombieAttractionMultiplier = this.newDoubleOption("ZombieAttractionMultiplier", 0.0, 100.0, 1.0);
    public final BooleanSandboxOption vehicleEasyUse = this.newBooleanOption("VehicleEasyUse", false);
    public final EnumSandboxOption initialGas = this.newEnumOption("InitialGas", 6, 3);
    public final BooleanSandboxOption fuelStationGasInfinite = this.newBooleanOption("FuelStationGasInfinite", false);
    public final DoubleSandboxOption fuelStationGasMin = this.newDoubleOption("FuelStationGasMin", 0.0, 1.0, 0.0);
    public final DoubleSandboxOption fuelStationGasMax = this.newDoubleOption("FuelStationGasMax", 0.0, 1.0, 0.7);
    public final IntegerSandboxOption fuelStationGasEmptyChance = this.newIntegerOption("FuelStationGasEmptyChance", 0, 100, 20);
    public final EnumSandboxOption lockedCar = this.newEnumOption("LockedCar", 6, 4);
    public final DoubleSandboxOption carGasConsumption = this.newDoubleOption("CarGasConsumption", 0.0, 100.0, 1.0);
    public final EnumSandboxOption carGeneralCondition = this.newEnumOption("CarGeneralCondition", 5, 3);
    public final EnumSandboxOption carDamageOnImpact = this.newEnumOption("CarDamageOnImpact", 5, 3);
    public final EnumSandboxOption damageToPlayerFromHitByACar = this.newEnumOption("DamageToPlayerFromHitByACar", 5, 1);
    public final BooleanSandboxOption trafficJam = this.newBooleanOption("TrafficJam", true);
    public final EnumSandboxOption carAlarm = this.newEnumOption("CarAlarm", 6, 4).setTranslation("CarAlarmFrequency");
    public final BooleanSandboxOption playerDamageFromCrash = this.newBooleanOption("PlayerDamageFromCrash", true);
    public final DoubleSandboxOption sirenShutoffHours = this.newDoubleOption("SirenShutoffHours", 0.0, 168.0, 0.0);
    public final EnumSandboxOption chanceHasGas = this.newEnumOption("ChanceHasGas", 3, 2);
    public final EnumSandboxOption recentlySurvivorVehicles = this.newEnumOption("RecentlySurvivorVehicles", 4, 3);
    public final BooleanSandboxOption multiHitZombies = this.newBooleanOption("MultiHitZombies", false);
    public final EnumSandboxOption rearVulnerability = this.newEnumOption("RearVulnerability", 3, 3);
    public final BooleanSandboxOption sirenEffectsZombies = this.newBooleanOption("SirenEffectsZombies", true);
    public final EnumSandboxOption animalStatsModifier = this.newEnumOption("AnimalStatsModifier", 6, 4).setValueTranslation("AnimalSpeed");
    public final EnumSandboxOption animalMetaStatsModifier = this.newEnumOption("AnimalMetaStatsModifier", 6, 4).setValueTranslation("AnimalSpeed");
    public final EnumSandboxOption animalPregnancyTime = this.newEnumOption("AnimalPregnancyTime", 6, 2).setValueTranslation("AnimalSpeed");
    public final EnumSandboxOption animalAgeModifier = this.newEnumOption("AnimalAgeModifier", 6, 3).setValueTranslation("AnimalSpeed");
    public final EnumSandboxOption animalMilkIncModifier = this.newEnumOption("AnimalMilkIncModifier", 6, 3).setValueTranslation("AnimalSpeed");
    public final EnumSandboxOption animalWoolIncModifier = this.newEnumOption("AnimalWoolIncModifier", 6, 3).setValueTranslation("AnimalSpeed");
    public final EnumSandboxOption animalRanchChance = this.newEnumOption("AnimalRanchChance", 7, 7).setValueTranslation("AnimalRanchChance");
    public final IntegerSandboxOption animalGrassRegrowTime = this.newIntegerOption("AnimalGrassRegrowTime", 1, 9999, 240);
    public final BooleanSandboxOption animalMetaPredator = this.newBooleanOption("AnimalMetaPredator", false);
    public final BooleanSandboxOption animalMatingSeason = this.newBooleanOption("AnimalMatingSeason", true);
    public final EnumSandboxOption animalEggHatch = this.newEnumOption("AnimalEggHatch", 6, 3).setValueTranslation("AnimalSpeed");
    public final BooleanSandboxOption animalSoundAttractZombies = this.newBooleanOption("AnimalSoundAttractZombies", false);
    public final EnumSandboxOption animalTrackChance = this.newEnumOption("AnimalTrackChance", 6, 4).setValueTranslation("HouseAlarmFrequency");
    public final EnumSandboxOption animalPathChance = this.newEnumOption("AnimalPathChance", 6, 4).setValueTranslation("HouseAlarmFrequency");
    public final IntegerSandboxOption maximumRatIndex = this.newIntegerOption("MaximumRatIndex", 0, 50, 25);
    public final IntegerSandboxOption daysUntilMaximumRatIndex = this.newIntegerOption("DaysUntilMaximumRatIndex", 0, 365, 90);
    public final EnumSandboxOption metaKnowledge = this.newEnumOption("MetaKnowledge", 3, 3);
    public final BooleanSandboxOption seeNotLearntRecipe = this.newBooleanOption("SeeNotLearntRecipe", true);
    public final IntegerSandboxOption maximumLootedBuildingRooms = this.newIntegerOption("MaximumLootedBuildingRooms", 0, 200, 50);
    public final EnumSandboxOption enablePoisoning = this.newEnumOption("EnablePoisoning", 3, 1);
    public final EnumSandboxOption maggotSpawn = this.newEnumOption("MaggotSpawn", 3, 1);
    public final DoubleSandboxOption lightBulbLifespan = this.newDoubleOption("LightBulbLifespan", 0.0, 1000.0, 1.0);
    public final EnumSandboxOption fishAbundance = this.newEnumOption("FishAbundance", 5, 3).setTranslation("FishAmount");
    public final IntegerSandboxOption levelForMediaXpCutoff = this.newIntegerOption("LevelForMediaXPCutoff", 0, 10, 3);
    public final IntegerSandboxOption levelForDismantleXpCutoff = this.newIntegerOption("LevelForDismantleXPCutoff", 0, 10, 0);
    public final IntegerSandboxOption bloodSplatLifespanDays = this.newIntegerOption("BloodSplatLifespanDays", 0, 365, 0);
    public final IntegerSandboxOption literatureCooldown = this.newIntegerOption("LiteratureCooldown", 1, 365, 90);
    public final EnumSandboxOption negativeTraitsPenalty = this.newEnumOption("NegativeTraitsPenalty", 4, 1);
    public final DoubleSandboxOption minutesPerPage = this.newDoubleOption("MinutesPerPage", 0.0, 60.0, 2.0);
    public final BooleanSandboxOption killInsideCrops = this.newBooleanOption("KillInsideCrops", true);
    public final BooleanSandboxOption plantGrowingSeasons = this.newBooleanOption("PlantGrowingSeasons", true);
    public final BooleanSandboxOption placeDirtAboveground = this.newBooleanOption("PlaceDirtAboveground", false);
    public final DoubleSandboxOption farmingSpeedNew = this.newDoubleOption("FarmingSpeedNew", 0.1, 100.0, 1.0);
    public final DoubleSandboxOption farmingAmountNew = this.newDoubleOption("FarmingAmountNew", 0.1, 10.0, 1.0);
    public final IntegerSandboxOption maximumLooted = this.newIntegerOption("MaximumLooted", 0, 200, 50);
    public final IntegerSandboxOption daysUntilMaximumLooted = this.newIntegerOption("DaysUntilMaximumLooted", 0, 3650, 90);
    public final DoubleSandboxOption ruralLooted = this.newDoubleOption("RuralLooted", 0.0, 2.0, 0.5);
    public final IntegerSandboxOption maximumDiminishedLoot = this.newIntegerOption("MaximumDiminishedLoot", 0, 100, 0);
    public final IntegerSandboxOption daysUntilMaximumDiminishedLoot = this.newIntegerOption("DaysUntilMaximumDiminishedLoot", 0, 3650, 3650);
    public final DoubleSandboxOption muscleStrainFactor = this.newDoubleOption("MuscleStrainFactor", 0.0, 10.0, 1.0);
    public final DoubleSandboxOption discomfortFactor = this.newDoubleOption("DiscomfortFactor", 0.0, 10.0, 1.0);
    public final DoubleSandboxOption woundInfectionFactor = this.newDoubleOption("WoundInfectionFactor", 0.0, 10.0, 0.0);
    public final BooleanSandboxOption noBlackClothes = this.newBooleanOption("NoBlackClothes", true);
    public final BooleanSandboxOption easyClimbing = this.newBooleanOption("EasyClimbing", false);
    public final IntegerSandboxOption maximumFireFuelHours = this.newIntegerOption("MaximumFireFuelHours", 1, 168, 8);
    public final BooleanSandboxOption firearmUseDamageChance = this.newBooleanOption("FirearmUseDamageChance", true);
    public final DoubleSandboxOption firearmNoiseMultiplier = this.newDoubleOption("FirearmNoiseMultiplier", 0.2, 2.0, 1.0);
    public final DoubleSandboxOption firearmJamMultiplier = this.newDoubleOption("FirearmJamMultiplier", 0.0, 10.0, 0.0);
    public final DoubleSandboxOption firearmMoodleMultiplier = this.newDoubleOption("FirearmMoodleMultiplier", 0.0, 10.0, 1.0);
    public final DoubleSandboxOption firearmWeatherMultiplier = this.newDoubleOption("FirearmWeatherMultiplier", 0.0, 10.0, 1.0);
    public final BooleanSandboxOption firearmHeadGearEffect = this.newBooleanOption("FirearmHeadGearEffect", true);
    public final DoubleSandboxOption clayLakeChance = this.newDoubleOption("ClayLakeChance", 0.0, 1.0, 0.05);
    public final DoubleSandboxOption clayRiverChance = this.newDoubleOption("ClayRiverChance", 0.0, 1.0, 0.05);
    public final IntegerSandboxOption generatorTileRange = this.newIntegerOption("GeneratorTileRange", 1, 100, 20);
    public final IntegerSandboxOption generatorVerticalPowerRange = this.newIntegerOption("GeneratorVerticalPowerRange", 1, 15, 3);
    private final ArrayList<SandboxOption> customOptions = new ArrayList();
    public final Basement basement = new Basement(this);
    public final Map map = new Map(this);
    public final ZombieLore lore = new ZombieLore(this);
    public final ZombieConfig zombieConfig = new ZombieConfig(this);
    public final MultiplierConfig multipliersConfig = new MultiplierConfig(this);
    private static final int SANDBOX_VERSION = 6;
    private final HashSet<String> lootItemRemovalSet = new HashSet();
    private String lootItemRemovalString;
    private final HashSet<String> worldItemRemovalSet = new HashSet();
    private String worldItemRemovalString;

    public SandboxOptions() {
        CustomSandboxOptions.instance.initInstance(this);
        File defines = ZomboidFileSystem.instance.getMediaFile("lua/shared/defines.lua");
        LuaManager.RunLua(defines.getAbsolutePath());
        this.loadGameFile("Apocalypse");
        this.setDefaultsToCurrentValues();
    }

    public static SandboxOptions getInstance() {
        return instance;
    }

    public void toLua() {
        KahluaTable vars = (KahluaTable)LuaManager.env.rawget("SandboxVars");
        for (int i = 0; i < this.options.size(); ++i) {
            this.options.get(i).toTable(vars);
        }
    }

    public void updateFromLua() {
        if (Core.gameMode.equals("LastStand")) {
            GameTime.instance.multiplierBias = 1.2f;
        }
        KahluaTable tab = (KahluaTable)LuaManager.env.rawget("SandboxVars");
        for (int i = 0; i < this.options.size(); ++i) {
            this.options.get(i).fromTable(tab);
        }
        GameTime.instance.multiplierBias = switch (this.speed) {
            case 1 -> 0.8f;
            case 2 -> 0.9f;
            case 4 -> 1.1f;
            case 5 -> 1.2f;
            default -> 1.0f;
        };
        VirtualZombieManager.instance.maxRealZombies = switch (this.zombies.getValue()) {
            case 1 -> 400;
            case 2 -> 350;
            case 3 -> 300;
            case 5 -> 100;
            case 6 -> 0;
            default -> 200;
        };
        VirtualZombieManager.instance.maxRealZombies = 1;
        this.applySettings();
    }

    public void initSandboxVars() {
        KahluaTable vars = (KahluaTable)LuaManager.env.rawget("SandboxVars");
        for (int i = 0; i < this.options.size(); ++i) {
            SandboxOption option = this.options.get(i);
            option.fromTable(vars);
            option.toTable(vars);
        }
    }

    public int randomWaterShut(int waterShutoffModifier) {
        return switch (waterShutoffModifier) {
            case 2 -> Rand.Next(0, 30);
            case 3 -> Rand.Next(0, 60);
            case 4 -> Rand.Next(0, 180);
            case 5 -> Rand.Next(0, 360);
            case 6 -> Rand.Next(0, 1800);
            case 7 -> Rand.Next(60, 180);
            case 8 -> Rand.Next(180, 360);
            case 9 -> Integer.MAX_VALUE;
            default -> -1;
        };
    }

    public int randomElectricityShut(int electricityShutoffModifier) {
        return switch (electricityShutoffModifier) {
            case 2 -> Rand.Next(14, 30);
            case 3 -> Rand.Next(14, 60);
            case 4 -> Rand.Next(14, 180);
            case 5 -> Rand.Next(14, 360);
            case 6 -> Rand.Next(14, 1800);
            case 7 -> Rand.Next(60, 180);
            case 8 -> Rand.Next(180, 360);
            case 9 -> Integer.MAX_VALUE;
            default -> -1;
        };
    }

    public int randomAlarmDecay(int alarmDecayModifier) {
        return switch (alarmDecayModifier) {
            case 2 -> Rand.Next(0, 30);
            case 3 -> Rand.Next(0, 60);
            case 4 -> Rand.Next(0, 180);
            case 5 -> Rand.Next(0, 360);
            case 6 -> Rand.Next(0, 1800);
            default -> 0;
        };
    }

    public int getTemperatureModifier() {
        return this.temperature.getValue();
    }

    public int getRainModifier() {
        return this.rain.getValue();
    }

    public int getErosionSpeed() {
        return this.erosionSpeed.getValue();
    }

    public int getWaterShutModifier() {
        return this.waterShutModifier.getValue();
    }

    public int getElecShutModifier() {
        return this.elecShutModifier.getValue();
    }

    public int getTimeSinceApo() {
        return this.timeSinceApo.getValue();
    }

    public double getEnduranceRegenMultiplier() {
        return switch (this.endRegen.getValue()) {
            case 1 -> 1.8;
            case 2 -> 1.3;
            case 4 -> 0.7;
            case 5 -> 0.4;
            default -> 1.0;
        };
    }

    public double getStatsDecreaseMultiplier() {
        return switch (this.statsDecrease.getValue()) {
            case 1 -> 2.0;
            case 2 -> 1.6;
            case 4 -> 0.8;
            case 5 -> 0.65;
            default -> 1.0;
        };
    }

    public int getDayLengthMinutes() {
        int value = this.dayLength.getValue();
        return switch (value) {
            case 1 -> 15;
            case 2 -> 30;
            case 3 -> 60;
            case 4 -> 90;
            default -> (value - 3) * 60;
        };
    }

    public int getDayLengthMinutesDefault() {
        int defaultValue = this.dayLength.getDefaultValue();
        return switch (defaultValue) {
            case 1 -> 15;
            case 2 -> 30;
            case 3 -> 60;
            case 4 -> 90;
            default -> (defaultValue - 3) * 60;
        };
    }

    public int getCompostHours() {
        return switch (this.compostTime.getValue()) {
            case 1 -> 168;
            case 2 -> 336;
            case 3 -> 504;
            case 4 -> 672;
            case 5 -> 1008;
            case 6 -> 1344;
            case 7 -> 1680;
            case 8 -> 2016;
            default -> 336;
        };
    }

    public void applySettings() {
        GameTime.instance.setStartYear(this.getFirstYear() + this.startYear.getValue() - 1);
        GameTime.instance.setStartMonth(this.startMonth.getValue() - 1);
        GameTime.instance.setStartDay(this.startDay.getValue() - 1);
        GameTime.instance.setMinutesPerDay(this.getDayLengthMinutes());
        GameTime.instance.setStartTimeOfDay(switch (this.startTime.getValue()) {
            case 1 -> 7.0f;
            case 3 -> 12.0f;
            case 4 -> 14.0f;
            case 5 -> 17.0f;
            case 6 -> 21.0f;
            case 7 -> 0.0f;
            case 8 -> 2.0f;
            case 9 -> 5.0f;
            default -> 9.0f;
        });
    }

    public void save(ByteBuffer output) throws IOException {
        output.put((byte)83);
        output.put((byte)65);
        output.put((byte)78);
        output.put((byte)68);
        output.putInt(244);
        output.putInt(6);
        output.putInt(this.options.size());
        for (int i = 0; i < this.options.size(); ++i) {
            SandboxOption option = this.options.get(i);
            GameWindow.WriteString(output, option.asConfigOption().getName());
            GameWindow.WriteString(output, option.asConfigOption().getValueAsString());
        }
        GameWindow.WriteString(output, LuaManager.GlobalObject.getWorld().getPreset());
    }

    public void load(ByteBuffer input) throws IOException {
        input.mark();
        input.get();
        input.get();
        input.get();
        input.get();
        input.getInt();
        int version = input.getInt();
        int count = input.getInt();
        for (int i = 0; i < count; ++i) {
            String name = GameWindow.ReadString(input);
            String value = GameWindow.ReadString(input);
            name = this.upgradeOptionName(name, version);
            value = this.upgradeOptionValue(name, value, version);
            SandboxOption option = this.optionByName.get(name);
            if (option == null) {
                DebugLog.log("ERROR unknown SandboxOption \"" + name + "\"");
                continue;
            }
            option.asConfigOption().parse(value);
        }
        LuaManager.GlobalObject.getWorld().setPreset(GameWindow.ReadString(input));
    }

    public int getFirstYear() {
        return 1993;
    }

    private static String[] parseName(String name) {
        String[] ss;
        String[] ret = new String[]{null, name};
        if (name.contains(".") && (ss = name.split("\\.")).length == 2) {
            ret[0] = ss[0];
            ret[1] = ss[1];
        }
        return ret;
    }

    private BooleanSandboxOption newBooleanOption(String name, boolean defaultValue) {
        return new BooleanSandboxOption(this, name, defaultValue);
    }

    private DoubleSandboxOption newDoubleOption(String name, double min, double max, double defaultValue) {
        return new DoubleSandboxOption(this, name, min, max, defaultValue);
    }

    private EnumSandboxOption newEnumOption(String name, int numValues, int defaultValue) {
        return new EnumSandboxOption(this, name, numValues, defaultValue);
    }

    private IntegerSandboxOption newIntegerOption(String name, int min, int max, int defaultValue) {
        return new IntegerSandboxOption(this, name, min, max, defaultValue);
    }

    private StringSandboxOption newStringOption(String name, String defaultValue, int maxLength) {
        return new StringSandboxOption(this, name, defaultValue, maxLength);
    }

    protected SandboxOptions addOption(SandboxOption option) {
        this.options.add(option);
        this.optionByName.put(option.asConfigOption().getName(), option);
        return this;
    }

    public int getNumOptions() {
        return this.options.size();
    }

    public SandboxOption getOptionByIndex(int index) {
        return this.options.get(index);
    }

    public SandboxOption getOptionByName(String name) {
        return this.optionByName.get(name);
    }

    public void set(String name, Object o) {
        if (name == null || o == null) {
            throw new IllegalArgumentException();
        }
        SandboxOption option = this.optionByName.get(name);
        if (option == null) {
            throw new IllegalArgumentException("unknown SandboxOption \"" + name + "\"");
        }
        option.asConfigOption().setValueFromObject(o);
    }

    public void copyValuesFrom(SandboxOptions other) {
        if (other == null) {
            throw new NullPointerException();
        }
        for (int i = 0; i < this.options.size(); ++i) {
            this.options.get(i).asConfigOption().setValueFromObject(other.options.get(i).asConfigOption().getValueAsObject());
        }
    }

    public void resetToDefault() {
        for (int i = 0; i < this.options.size(); ++i) {
            this.options.get(i).asConfigOption().resetToDefault();
        }
    }

    public void setDefaultsToCurrentValues() {
        for (int i = 0; i < this.options.size(); ++i) {
            this.options.get(i).asConfigOption().setDefaultToCurrentValue();
        }
    }

    public SandboxOptions newCopy() {
        SandboxOptions copy = new SandboxOptions();
        copy.copyValuesFrom(this);
        return copy;
    }

    public static boolean isValidPresetName(String name) {
        if (name == null || name.isEmpty()) {
            return false;
        }
        return !name.contains("/") && !name.contains("\\") && !name.contains(":") && !name.contains(";") && !name.contains("\"") && !name.contains(".");
    }

    private boolean readTextFile(String fileName, boolean isPreset) {
        ConfigFile configFile = new ConfigFile();
        if (configFile.read(fileName)) {
            int i;
            int version = configFile.getVersion();
            HashSet<String> fixZombieLore = null;
            if (isPreset && version == 1) {
                fixZombieLore = new HashSet<String>();
                for (i = 0; i < this.options.size(); ++i) {
                    if (!"ZombieLore".equals(this.options.get(i).getTableName())) continue;
                    fixZombieLore.add(this.options.get(i).getShortName());
                }
            }
            for (i = 0; i < configFile.getOptions().size(); ++i) {
                ConfigOption configOption = configFile.getOptions().get(i);
                Object optionName = configOption.getName();
                String optionValue = configOption.getValueAsString();
                if (fixZombieLore != null && fixZombieLore.contains(optionName)) {
                    optionName = "ZombieLore." + (String)optionName;
                }
                if (isPreset && version == 1) {
                    if ("WaterShutModifier".equals(optionName)) {
                        optionName = "WaterShut";
                    } else if ("ElecShutModifier".equals(optionName)) {
                        optionName = "ElecShut";
                    }
                }
                optionName = this.upgradeOptionName((String)optionName, version);
                optionValue = this.upgradeOptionValue((String)optionName, optionValue, version);
                SandboxOption option = this.optionByName.get(optionName);
                if (option == null) continue;
                option.asConfigOption().parse(optionValue);
            }
            return true;
        }
        return false;
    }

    private boolean writeTextFile(String fileName, int version) {
        ConfigFile configFile = new ConfigFile();
        ArrayList<ConfigOption> configOptions = new ArrayList<ConfigOption>();
        for (SandboxOption option : this.options) {
            configOptions.add(option.asConfigOption());
        }
        return configFile.write(fileName, version, configOptions);
    }

    public boolean loadServerTextFile(String serverName) {
        return this.readTextFile(ServerSettingsManager.instance.getNameInSettingsFolder(serverName + "_sandbox.ini"), false);
    }

    public boolean loadServerLuaFile(String serverName) {
        boolean read = this.readLuaFile(ServerSettingsManager.instance.getNameInSettingsFolder(serverName + "_SandboxVars.lua"));
        return read;
    }

    public boolean saveServerLuaFile(String serverName) {
        return this.writeLuaFile(ServerSettingsManager.instance.getNameInSettingsFolder(serverName + "_SandboxVars.lua"), false);
    }

    public boolean loadPresetFile(String presetName) {
        return this.readTextFile(LuaManager.getSandboxCacheDir() + File.separator + presetName + ".cfg", true);
    }

    public boolean savePresetFile(String presetName) {
        if (!SandboxOptions.isValidPresetName(presetName)) {
            return false;
        }
        return this.writeTextFile(LuaManager.getSandboxCacheDir() + File.separator + presetName + ".cfg", 6);
    }

    public boolean loadGameFile(String presetName) {
        File file = ZomboidFileSystem.instance.getMediaFile("lua/shared/Sandbox/" + presetName + ".lua");
        if (!file.exists()) {
            return true;
        }
        try {
            LuaManager.loaded.remove(file.getAbsolutePath().replace("\\", "/"));
            Object result = LuaManager.RunLua(file.getAbsolutePath());
            if (result instanceof KahluaTable) {
                KahluaTable kahluaTable = (KahluaTable)result;
                for (int i = 0; i < this.options.size(); ++i) {
                    this.options.get(i).fromTable(kahluaTable);
                }
                return true;
            }
            throw new RuntimeException(file.getName() + " must return a SandboxVars table");
        }
        catch (Exception ex) {
            ExceptionLogger.logException(ex);
            return false;
        }
    }

    public boolean saveGameFile(String presetName) {
        if (!Core.debug) {
            return false;
        }
        return this.writeLuaFile("media/lua/shared/Sandbox/" + presetName + ".lua", true);
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    private void saveCurrentGameBinFile() {
        File file = ZomboidFileSystem.instance.getFileInCurrentSave("map_sand.bin");
        try (FileOutputStream fos = new FileOutputStream(file);
             BufferedOutputStream bos = new BufferedOutputStream(fos);){
            Object object = SliceY.SliceBufferLock;
            synchronized (object) {
                SliceY.SliceBuffer.clear();
                this.save(SliceY.SliceBuffer);
                bos.write(SliceY.SliceBuffer.array(), 0, SliceY.SliceBuffer.position());
            }
        }
        catch (Exception ex) {
            ExceptionLogger.logException(ex);
        }
    }

    public void handleOldZombiesFile1() {
        if (GameServer.server) {
            return;
        }
        ConfigFile configFile = new ConfigFile();
        String fileName = ZomboidFileSystem.instance.getFileNameInCurrentSave("zombies.ini");
        if (configFile.read(fileName)) {
            for (int i = 0; i < configFile.getOptions().size(); ++i) {
                ConfigOption configOption = configFile.getOptions().get(i);
                SandboxOption option = this.optionByName.get("ZombieConfig." + configOption.getName());
                if (option == null) continue;
                option.asConfigOption().parse(configOption.getValueAsString());
            }
        }
    }

    public void handleOldZombiesFile2() {
        if (GameServer.server) {
            return;
        }
        String fileName = ZomboidFileSystem.instance.getFileNameInCurrentSave("zombies.ini");
        File file = new File(fileName);
        if (!file.exists()) {
            return;
        }
        try {
            DebugLog.DetailedInfo.trace("deleting " + file.getAbsolutePath());
            file.delete();
            this.saveCurrentGameBinFile();
        }
        catch (Exception ex) {
            ExceptionLogger.logException(ex);
        }
    }

    public void handleOldServerZombiesFile() {
        if (!GameServer.server) {
            return;
        }
        if (this.loadServerZombiesFile(GameServer.serverName)) {
            String fileName = ServerSettingsManager.instance.getNameInSettingsFolder(GameServer.serverName + "_zombies.ini");
            try {
                File file = new File(fileName);
                DebugLog.DetailedInfo.trace("deleting " + file.getAbsolutePath());
                file.delete();
                this.saveServerLuaFile(GameServer.serverName);
            }
            catch (Exception ex) {
                ExceptionLogger.logException(ex);
            }
        }
    }

    public boolean loadServerZombiesFile(String serverName) {
        ConfigFile configFile = new ConfigFile();
        String fileName = ServerSettingsManager.instance.getNameInSettingsFolder(serverName + "_zombies.ini");
        if (configFile.read(fileName)) {
            for (int i = 0; i < configFile.getOptions().size(); ++i) {
                ConfigOption configOption = configFile.getOptions().get(i);
                SandboxOption option = this.optionByName.get("ZombieConfig." + configOption.getName());
                if (option == null) continue;
                option.asConfigOption().parse(configOption.getValueAsString());
            }
            return true;
        }
        return false;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    private boolean readLuaFile(String fileName) {
        File file = new File(fileName).getAbsoluteFile();
        if (!file.exists()) {
            return false;
        }
        Object oldObj = LuaManager.env.rawget("SandboxVars");
        KahluaTable oldTable = null;
        if (oldObj instanceof KahluaTable) {
            KahluaTable kahluaTable;
            oldTable = kahluaTable = (KahluaTable)oldObj;
        }
        LuaManager.env.rawset("SandboxVars", null);
        try {
            LuaManager.loaded.remove(file.getAbsolutePath().replace("\\", "/"));
            LuaManager.RunLua(file.getAbsolutePath());
            Object newObj = LuaManager.env.rawget("SandboxVars");
            if (newObj == null) {
                boolean bl = false;
                return bl;
            }
            if (newObj instanceof KahluaTable) {
                KahluaTable newTable = (KahluaTable)newObj;
                int version = 0;
                Object versionObj = newTable.rawget("VERSION");
                if (versionObj != null) {
                    if (versionObj instanceof Double) {
                        Double d = (Double)versionObj;
                        version = d.intValue();
                    } else {
                        DebugLog.log("ERROR: VERSION=\"" + String.valueOf(versionObj) + "\" in " + fileName);
                    }
                    newTable.rawset("VERSION", null);
                }
                newTable = this.upgradeLuaTable("", newTable, version);
                for (int i = 0; i < this.options.size(); ++i) {
                    this.options.get(i).fromTable(newTable);
                }
            }
            boolean bl = true;
            return bl;
        }
        catch (Exception ex) {
            ExceptionLogger.logException(ex);
            boolean bl = false;
            return bl;
        }
        finally {
            if (oldTable != null) {
                LuaManager.env.rawset("SandboxVars", (Object)oldTable);
            }
        }
    }

    private boolean writeLuaFile(String fileName, boolean isDeveloperFile) {
        File file = new File(fileName).getAbsoluteFile();
        DebugLog.log("writing " + fileName);
        try (FileWriter fw = new FileWriter(file);){
            HashMap tables = new HashMap();
            ArrayList<String> tableNames = new ArrayList<String>();
            tables.put("", new ArrayList());
            for (SandboxOption option : this.options) {
                if (option.getTableName() == null) {
                    ((ArrayList)tables.get("")).add(option);
                    continue;
                }
                if (tables.get(option.getTableName()) == null) {
                    tables.put(option.getTableName(), new ArrayList());
                    tableNames.add(option.getTableName());
                }
                ((ArrayList)tables.get(option.getTableName())).add(option);
            }
            String lineSep = System.lineSeparator();
            if (isDeveloperFile) {
                fw.write("return {" + lineSep);
            } else {
                fw.write("SandboxVars = {" + lineSep);
            }
            fw.write("    VERSION = 6," + lineSep);
            for (SandboxOption option : (ArrayList)tables.get("")) {
                if (!isDeveloperFile) {
                    String tooltip = option.asConfigOption().getTooltip();
                    if (tooltip != null) {
                        tooltip = tooltip.replace("\\n", " ").replace("\\\"", "\"");
                        tooltip = tooltip.replaceAll("\n", lineSep + "    -- ");
                        fw.write("    -- " + tooltip + lineSep);
                    }
                    if (option instanceof EnumSandboxOption) {
                        EnumSandboxOption enumOption = (EnumSandboxOption)option;
                        for (int i = 1; i <= enumOption.getNumValues(); ++i) {
                            try {
                                String subOptionTranslated = enumOption.getValueTranslationByIndexOrNull(i);
                                if (subOptionTranslated == null) continue;
                                fw.write("    -- " + i + " = " + subOptionTranslated.replace("\\\"", "\"") + lineSep);
                                continue;
                            }
                            catch (Exception ex) {
                                ExceptionLogger.logException(ex);
                            }
                        }
                    }
                }
                fw.write("    " + option.asConfigOption().getName() + " = " + option.asConfigOption().getValueAsLuaString() + "," + lineSep);
            }
            for (String tableName : tableNames) {
                fw.write("    " + tableName + " = {" + lineSep);
                for (SandboxOption option : (ArrayList)tables.get(tableName)) {
                    if (!isDeveloperFile) {
                        String tooltip = option.asConfigOption().getTooltip();
                        if (tooltip != null) {
                            tooltip = tooltip.replace("\\n", " ").replace("\\\"", "\"");
                            tooltip = tooltip.replaceAll("\n", lineSep + "        -- ");
                            fw.write("        -- " + tooltip + lineSep);
                        }
                        if (option instanceof EnumSandboxOption) {
                            EnumSandboxOption enumOption = (EnumSandboxOption)option;
                            for (int i = 1; i <= enumOption.getNumValues(); ++i) {
                                try {
                                    String subOptionTranslated = enumOption.getValueTranslationByIndexOrNull(i);
                                    if (subOptionTranslated == null) continue;
                                    fw.write("        -- " + i + " = " + subOptionTranslated + lineSep);
                                    continue;
                                }
                                catch (Exception ex) {
                                    ExceptionLogger.logException(ex);
                                }
                            }
                        }
                    }
                    fw.write("        " + option.getShortName() + " = " + option.asConfigOption().getValueAsLuaString() + "," + lineSep);
                }
                fw.write("    }," + lineSep);
            }
            fw.write("}" + lineSep);
        }
        catch (Exception ex) {
            ExceptionLogger.logException(ex);
            return false;
        }
        return true;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public void load() {
        File file = ZomboidFileSystem.instance.getFileInCurrentSave("map_sand.bin");
        try (FileInputStream fis2 = new FileInputStream(file);){
            try (BufferedInputStream bis = new BufferedInputStream(fis2);){
                Object object = SliceY.SliceBufferLock;
                synchronized (object) {
                    SliceY.SliceBuffer.clear();
                    int numBytes = bis.read(SliceY.SliceBuffer.array());
                    SliceY.SliceBuffer.limit(numBytes);
                    this.load(SliceY.SliceBuffer);
                    this.handleOldZombiesFile1();
                    this.applySettings();
                    this.toLua();
                }
            }
            return;
        }
        catch (FileNotFoundException fis2) {
        }
        catch (Exception ex) {
            ExceptionLogger.logException(ex);
        }
        this.resetToDefault();
        this.updateFromLua();
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public void loadCurrentGameBinFile() {
        File inFile = ZomboidFileSystem.instance.getFileInCurrentSave("map_sand.bin");
        try (FileInputStream inStream = new FileInputStream(inFile);
             BufferedInputStream input = new BufferedInputStream(inStream);){
            Object object = SliceY.SliceBufferLock;
            synchronized (object) {
                SliceY.SliceBuffer.clear();
                int numBytes = input.read(SliceY.SliceBuffer.array());
                SliceY.SliceBuffer.limit(numBytes);
                this.load(SliceY.SliceBuffer);
            }
            this.toLua();
        }
        catch (Exception ex) {
            ExceptionLogger.logException(ex);
        }
    }

    private String upgradeOptionName(String optionName, int version) {
        return optionName;
    }

    private String upgradeOptionValue(String optionName, String optionValue, int version) {
        int value;
        if (version < 3 && "DayLength".equals(optionName)) {
            this.dayLength.parse(optionValue);
            if (this.dayLength.getValue() == 8) {
                this.dayLength.setValue(14);
            } else if (this.dayLength.getValue() == 9) {
                this.dayLength.setValue(26);
            }
            optionValue = this.dayLength.getValueAsString();
        }
        if (version < 4 && "CarSpawnRate".equals(optionName)) {
            try {
                value = (int)Double.parseDouble(optionValue);
                if (value > 1) {
                    optionValue = Integer.toString(value + 1);
                }
            }
            catch (NumberFormatException ex) {
                ex.printStackTrace();
            }
        }
        if (version < 5) {
            if ("FoodLoot".equals(optionName) || "CannedFoodLoot".equals(optionName) || "LiteratureLoot".equals(optionName) || "SurvivalGearsLoot".equals(optionName) || "MedicalLoot".equals(optionName) || "WeaponLoot".equals(optionName) || "RangedWeaponLoot".equals(optionName) || "AmmoLoot".equals(optionName) || "MechanicsLoot".equals(optionName) || "OtherLoot".equals(optionName)) {
                try {
                    value = (int)Double.parseDouble(optionValue);
                    if (value > 0) {
                        optionValue = Integer.toString(value + 2);
                    }
                }
                catch (NumberFormatException ex) {
                    ex.printStackTrace();
                }
            }
            if ("RecentlySurvivorVehicles".equals(optionName)) {
                try {
                    int value2 = (int)Double.parseDouble(optionValue);
                    if (value2 > 0) {
                        optionValue = Integer.toString(value2 + 1);
                    }
                }
                catch (NumberFormatException ex) {
                    ex.printStackTrace();
                }
            }
        }
        if (version < 6 && "DayLength".equals(optionName)) {
            this.dayLength.parse(optionValue);
            if (this.dayLength.getValue() > 3) {
                this.dayLength.setValue(this.dayLength.getValue() + 1);
            }
        }
        return optionValue;
    }

    private KahluaTable upgradeLuaTable(String prefix, KahluaTable table, int version) {
        KahluaTable newTable = LuaManager.platform.newTable();
        KahluaTableIterator it = table.iterator();
        while (it.advance()) {
            if (it.getKey() instanceof String) {
                if (it.getValue() instanceof KahluaTable) {
                    KahluaTable newTable1 = this.upgradeLuaTable(prefix + String.valueOf(it.getKey()) + ".", (KahluaTable)it.getValue(), version);
                    newTable.rawset(it.getKey(), (Object)newTable1);
                    continue;
                }
                String optionName = this.upgradeOptionName(prefix + String.valueOf(it.getKey()), version);
                String optionValue = this.upgradeOptionValue(optionName, it.getValue().toString(), version);
                newTable.rawset(optionName.replace(prefix, ""), (Object)optionValue);
                continue;
            }
            throw new IllegalStateException("expected a String key");
        }
        return newTable;
    }

    public void sendToServer() {
        if (GameClient.client) {
            GameClient.instance.sendSandboxOptionsToServer(this);
        }
    }

    public void newCustomOption(CustomSandboxOption customSandboxOption) {
        if (customSandboxOption instanceof CustomBooleanSandboxOption) {
            CustomBooleanSandboxOption booleanOption = (CustomBooleanSandboxOption)customSandboxOption;
            this.addCustomOption(new BooleanSandboxOption(this, booleanOption.id, booleanOption.defaultValue), customSandboxOption);
            return;
        }
        if (customSandboxOption instanceof CustomDoubleSandboxOption) {
            CustomDoubleSandboxOption doubleOption = (CustomDoubleSandboxOption)customSandboxOption;
            this.addCustomOption(new DoubleSandboxOption(this, doubleOption.id, doubleOption.min, doubleOption.max, doubleOption.defaultValue), customSandboxOption);
            return;
        }
        if (customSandboxOption instanceof CustomEnumSandboxOption) {
            CustomEnumSandboxOption enumOption = (CustomEnumSandboxOption)customSandboxOption;
            EnumSandboxOption sandboxOption = new EnumSandboxOption(this, enumOption.id, enumOption.numValues, enumOption.defaultValue);
            if (enumOption.valueTranslation != null) {
                sandboxOption.setValueTranslation(enumOption.valueTranslation);
            }
            this.addCustomOption(sandboxOption, customSandboxOption);
            return;
        }
        if (customSandboxOption instanceof CustomIntegerSandboxOption) {
            CustomIntegerSandboxOption integerOption = (CustomIntegerSandboxOption)customSandboxOption;
            this.addCustomOption(new IntegerSandboxOption(this, integerOption.id, integerOption.min, integerOption.max, integerOption.defaultValue), customSandboxOption);
            return;
        }
        if (customSandboxOption instanceof CustomStringSandboxOption) {
            CustomStringSandboxOption stringOption = (CustomStringSandboxOption)customSandboxOption;
            this.addCustomOption(new StringSandboxOption(this, stringOption.id, stringOption.defaultValue, -1), customSandboxOption);
            return;
        }
        throw new IllegalArgumentException("unhandled CustomSandboxOption " + String.valueOf(customSandboxOption));
    }

    private void addCustomOption(SandboxOption option, CustomSandboxOption custom) {
        option.setCustom();
        if (custom.page != null) {
            option.setPageName(custom.page);
        }
        if (custom.translation != null) {
            option.setTranslation(custom.translation);
        }
        this.customOptions.add(option);
    }

    private void removeCustomOptions() {
        this.options.removeAll(this.customOptions);
        for (SandboxOption option : this.customOptions) {
            this.optionByName.remove(option.asConfigOption().getName());
        }
        this.customOptions.clear();
    }

    public static void Reset() {
        instance.removeCustomOptions();
    }

    public boolean getAllClothesUnlocked() {
        return this.allClothesUnlocked.getValue();
    }

    public int getCurrentRatIndex() {
        int currentRatFactor;
        int maxRat = SandboxOptions.instance.maximumRatIndex.getValue();
        int daysUntilMax = SandboxOptions.instance.daysUntilMaximumRatIndex.getValue();
        if (maxRat <= 0) {
            return 0;
        }
        if (daysUntilMax <= 0) {
            return maxRat;
        }
        int days = (int)((float)GameTime.getInstance().getWorldAgeHours() / 24.0f) + (SandboxOptions.instance.timeSinceApo.getValue() - 1) * 30;
        if (days <= 0) {
            days = 1;
        }
        if (days > daysUntilMax) {
            days = daysUntilMax;
        }
        if ((currentRatFactor = maxRat * days / daysUntilMax) <= 0) {
            currentRatFactor = 1;
        }
        return currentRatFactor;
    }

    public int getCurrentLootedChance() {
        return this.getCurrentLootedChance(null);
    }

    public int getCurrentLootedChance(IsoGridSquare square) {
        int maxLooted = SandboxOptions.instance.maximumLooted.getValue();
        int daysUntilMax = SandboxOptions.instance.daysUntilMaximumLooted.getValue();
        if (maxLooted <= 0) {
            return 0;
        }
        if (daysUntilMax <= 0) {
            return maxLooted;
        }
        int days = (int)(GameTime.getInstance().getWorldAgeHours() / 24.0) + (SandboxOptions.instance.timeSinceApo.getValue() - 1) * 30;
        if (days <= 0) {
            days = 1;
        }
        if (days > daysUntilMax) {
            days = daysUntilMax;
        }
        int currentLootedChance = maxLooted * days / daysUntilMax;
        if (square != null && ItemPickerJava.getSquareRegion(square) == null) {
            currentLootedChance *= (int)SandboxOptions.instance.ruralLooted.getValue();
        }
        if (square != null && Objects.equals(square.getSquareZombiesType(), "Rich")) {
            currentLootedChance = (int)((double)currentLootedChance * 1.5);
        }
        if (currentLootedChance <= 0) {
            currentLootedChance = 1;
        }
        return currentLootedChance;
    }

    public int getCurrentDiminishedLootPercentage() {
        return this.getCurrentDiminishedLootPercentage(null);
    }

    public int getCurrentDiminishedLootPercentage(IsoGridSquare square) {
        int maxLooted = SandboxOptions.instance.maximumDiminishedLoot.getValue();
        int daysUntilMax = SandboxOptions.instance.daysUntilMaximumDiminishedLoot.getValue();
        if (maxLooted <= 0) {
            return 0;
        }
        if (daysUntilMax <= 0) {
            return maxLooted;
        }
        int days = (int)(GameTime.getInstance().getWorldAgeHours() / 24.0) + (SandboxOptions.instance.timeSinceApo.getValue() - 1) * 30;
        if (days <= 0) {
            days = 1;
        }
        if (days > daysUntilMax) {
            days = daysUntilMax;
        }
        int currentLooted = maxLooted * days / daysUntilMax;
        if (square != null && ItemPickerJava.getSquareRegion(square) == null) {
            currentLooted *= (int)SandboxOptions.instance.ruralLooted.getValue();
        }
        if (currentLooted < 0) {
            currentLooted = 0;
        }
        if (currentLooted > 100) {
            currentLooted = 100;
        }
        return currentLooted;
    }

    public float getCurrentLootMultiplier() {
        return this.getCurrentLootMultiplier(null);
    }

    public float getCurrentLootMultiplier(IsoGridSquare square) {
        return 1.0f - (float)this.getCurrentDiminishedLootPercentage(square) / 100.0f;
    }

    public boolean isUnstableScriptNameSpam() {
        return true;
    }

    public boolean doesPowerGridExist() {
        return this.doesPowerGridExist(0);
    }

    public boolean doesPowerGridExist(int offset) {
        return IsoWorld.instance.getWorldAgeDays() <= (float)(SandboxOptions.getInstance().getElecShutModifier() + offset);
    }

    public boolean lootItemRemovalListContains(String itemType) {
        if (this.lootItemRemovalString != this.lootItemRemovalList.getValue()) {
            this.lootItemRemovalString = this.lootItemRemovalList.getValue();
            Set<String> listOfStrings = this.lootItemRemovalList.getSplitCSVList();
            this.lootItemRemovalSet.clear();
            this.lootItemRemovalSet.addAll(listOfStrings);
        }
        return this.lootItemRemovalSet.contains(itemType);
    }

    public boolean worldItemRemovalListContains(String itemType) {
        if (this.worldItemRemovalString != this.worldItemRemovalList.getValue()) {
            this.worldItemRemovalString = this.worldItemRemovalList.getValue();
            Set<String> listOfStrings = this.worldItemRemovalList.getSplitCSVList();
            this.worldItemRemovalSet.clear();
            this.worldItemRemovalSet.addAll(listOfStrings);
        }
        return this.worldItemRemovalSet.contains(itemType);
    }

    @UsedFromLua
    public static class EnumSandboxOption
    extends EnumConfigOption
    implements SandboxOption {
        protected String translation;
        protected String tableName;
        protected String shortName;
        protected boolean custom;
        protected String pageName;
        protected String valueTranslation;

        public EnumSandboxOption(SandboxOptions owner, String name, int numValues, int defaultValue) {
            super(name, numValues, defaultValue);
            String[] ss = SandboxOptions.parseName(name);
            this.tableName = ss[0];
            this.shortName = ss[1];
            owner.addOption(this);
        }

        @Override
        public EnumConfigOption asConfigOption() {
            return this;
        }

        @Override
        public String getShortName() {
            return this.shortName;
        }

        @Override
        public String getTableName() {
            return this.tableName;
        }

        @Override
        public EnumSandboxOption setTranslation(String translation) {
            this.translation = translation;
            return this;
        }

        @Override
        public String getTranslatedName() {
            return Translator.getText("Sandbox_" + (this.translation == null ? this.getShortName() : this.translation));
        }

        @Override
        public String getTooltip() {
            String s2;
            String s1 = Translator.getTextOrNull("Sandbox_" + (this.translation == null ? this.getShortName() : this.translation) + "_tooltip");
            String value = this.getValueTranslationByIndexOrNull(this.defaultValue);
            String string = s2 = value == null ? null : Translator.getText("Sandbox_Default", value);
            if (s1 == null) {
                return s2;
            }
            if (s2 == null) {
                return s1;
            }
            return s1 + "\\n" + s2;
        }

        @Override
        public void fromTable(KahluaTable table) {
            Object o;
            if (this.tableName != null) {
                Object table2 = table.rawget(this.tableName);
                if (table2 instanceof KahluaTable) {
                    KahluaTable kahluaTable;
                    table = kahluaTable = (KahluaTable)table2;
                } else {
                    return;
                }
            }
            if ((o = table.rawget(this.getShortName())) != null) {
                this.setValueFromObject(o);
            }
        }

        @Override
        public void toTable(KahluaTable table) {
            if (this.tableName != null) {
                Object o = table.rawget(this.tableName);
                if (o instanceof KahluaTable) {
                    KahluaTable kahluaTable;
                    table = kahluaTable = (KahluaTable)o;
                } else {
                    KahluaTable table2 = LuaManager.platform.newTable();
                    table.rawset(this.tableName, (Object)table2);
                    table = table2;
                }
            }
            table.rawset(this.getShortName(), this.getValueAsObject());
        }

        @Override
        public void setCustom() {
            this.custom = true;
        }

        @Override
        public boolean isCustom() {
            return this.custom;
        }

        @Override
        public EnumSandboxOption setPageName(String pageName) {
            this.pageName = pageName;
            return this;
        }

        @Override
        public String getPageName() {
            return this.pageName;
        }

        public EnumSandboxOption setValueTranslation(String translation) {
            this.valueTranslation = translation;
            return this;
        }

        public String getValueTranslation() {
            return this.valueTranslation != null ? this.valueTranslation : (this.translation == null ? this.getShortName() : this.translation);
        }

        public String getValueTranslationByIndex(int index) {
            if (index < 1 || index > this.getNumValues()) {
                throw new ArrayIndexOutOfBoundsException();
            }
            return Translator.getText("Sandbox_" + this.getValueTranslation() + "_option" + index);
        }

        public String getValueTranslationByIndexOrNull(int index) {
            if (index < 1 || index > this.getNumValues()) {
                throw new ArrayIndexOutOfBoundsException();
            }
            return Translator.getTextOrNull("Sandbox_" + this.getValueTranslation() + "_option" + index);
        }
    }

    @UsedFromLua
    public static class BooleanSandboxOption
    extends BooleanConfigOption
    implements SandboxOption {
        protected String translation;
        protected String tableName;
        protected String shortName;
        protected boolean custom;
        protected String pageName;

        public BooleanSandboxOption(SandboxOptions owner, String name, boolean defaultValue) {
            super(name, defaultValue);
            String[] ss = SandboxOptions.parseName(name);
            this.tableName = ss[0];
            this.shortName = ss[1];
            owner.addOption(this);
        }

        @Override
        public BooleanConfigOption asConfigOption() {
            return this;
        }

        @Override
        public String getShortName() {
            return this.shortName;
        }

        @Override
        public String getTableName() {
            return this.tableName;
        }

        @Override
        public BooleanSandboxOption setTranslation(String translation) {
            this.translation = translation;
            return this;
        }

        @Override
        public String getTranslatedName() {
            return Translator.getText("Sandbox_" + (this.translation == null ? this.getShortName() : this.translation));
        }

        @Override
        public String getTooltip() {
            return Translator.getTextOrNull("Sandbox_" + (this.translation == null ? this.getShortName() : this.translation) + "_tooltip");
        }

        @Override
        public void fromTable(KahluaTable table) {
            Object o;
            if (this.tableName != null) {
                Object table2 = table.rawget(this.tableName);
                if (table2 instanceof KahluaTable) {
                    KahluaTable kahluaTable;
                    table = kahluaTable = (KahluaTable)table2;
                } else {
                    return;
                }
            }
            if ((o = table.rawget(this.getShortName())) != null) {
                this.setValueFromObject(o);
            }
        }

        @Override
        public void toTable(KahluaTable table) {
            if (this.tableName != null) {
                Object o = table.rawget(this.tableName);
                if (o instanceof KahluaTable) {
                    KahluaTable kahluaTable;
                    table = kahluaTable = (KahluaTable)o;
                } else {
                    KahluaTable table2 = LuaManager.platform.newTable();
                    table.rawset(this.tableName, (Object)table2);
                    table = table2;
                }
            }
            table.rawset(this.getShortName(), this.getValueAsObject());
        }

        @Override
        public void setCustom() {
            this.custom = true;
        }

        @Override
        public boolean isCustom() {
            return this.custom;
        }

        @Override
        public BooleanSandboxOption setPageName(String pageName) {
            this.pageName = pageName;
            return this;
        }

        @Override
        public String getPageName() {
            return this.pageName;
        }
    }

    @UsedFromLua
    public static class IntegerSandboxOption
    extends IntegerConfigOption
    implements SandboxOption {
        protected String translation;
        protected String tableName;
        protected String shortName;
        protected boolean custom;
        protected String pageName;

        public IntegerSandboxOption(SandboxOptions owner, String name, int min, int max, int defaultValue) {
            super(name, min, max, defaultValue);
            String[] ss = SandboxOptions.parseName(name);
            this.tableName = ss[0];
            this.shortName = ss[1];
            owner.addOption(this);
        }

        @Override
        public IntegerConfigOption asConfigOption() {
            return this;
        }

        @Override
        public String getShortName() {
            return this.shortName;
        }

        @Override
        public String getTableName() {
            return this.tableName;
        }

        @Override
        public IntegerSandboxOption setTranslation(String translation) {
            this.translation = translation;
            return this;
        }

        @Override
        public String getTranslatedName() {
            return Translator.getText("Sandbox_" + (this.translation == null ? this.getShortName() : this.translation));
        }

        @Override
        public String getTooltip() {
            String s1 = "ZombieConfig".equals(this.tableName) ? Translator.getTextOrNull("Sandbox_" + (this.translation == null ? this.getShortName() : this.translation) + "_help") : Translator.getTextOrNull("Sandbox_" + (this.translation == null ? this.getShortName() : this.translation) + "_tooltip");
            String s2 = Translator.getText("Sandbox_MinMaxDefault", this.min, this.max, this.defaultValue);
            if (s1 == null) {
                return s2;
            }
            if (s2 == null) {
                return s1;
            }
            return s1 + "\\n" + s2;
        }

        @Override
        public void fromTable(KahluaTable table) {
            Object o;
            if (this.tableName != null) {
                Object table2 = table.rawget(this.tableName);
                if (table2 instanceof KahluaTable) {
                    KahluaTable kahluaTable;
                    table = kahluaTable = (KahluaTable)table2;
                } else {
                    return;
                }
            }
            if ((o = table.rawget(this.getShortName())) != null) {
                this.setValueFromObject(o);
            }
        }

        @Override
        public void toTable(KahluaTable table) {
            if (this.tableName != null) {
                Object o = table.rawget(this.tableName);
                if (o instanceof KahluaTable) {
                    KahluaTable kahluaTable;
                    table = kahluaTable = (KahluaTable)o;
                } else {
                    KahluaTable table2 = LuaManager.platform.newTable();
                    table.rawset(this.tableName, (Object)table2);
                    table = table2;
                }
            }
            table.rawset(this.getShortName(), this.getValueAsObject());
        }

        @Override
        public void setCustom() {
            this.custom = true;
        }

        @Override
        public boolean isCustom() {
            return this.custom;
        }

        @Override
        public IntegerSandboxOption setPageName(String pageName) {
            this.pageName = pageName;
            return this;
        }

        @Override
        public String getPageName() {
            return this.pageName;
        }
    }

    @UsedFromLua
    public static class DoubleSandboxOption
    extends DoubleConfigOption
    implements SandboxOption {
        protected String translation;
        protected String tableName;
        protected String shortName;
        protected boolean custom;
        protected String pageName;

        public DoubleSandboxOption(SandboxOptions owner, String name, double min, double max, double defaultValue) {
            super(name, min, max, defaultValue);
            String[] ss = SandboxOptions.parseName(name);
            this.tableName = ss[0];
            this.shortName = ss[1];
            owner.addOption(this);
        }

        @Override
        public DoubleConfigOption asConfigOption() {
            return this;
        }

        @Override
        public String getShortName() {
            return this.shortName;
        }

        @Override
        public String getTableName() {
            return this.tableName;
        }

        @Override
        public DoubleSandboxOption setTranslation(String translation) {
            this.translation = translation;
            return this;
        }

        @Override
        public String getTranslatedName() {
            return Translator.getText("Sandbox_" + (this.translation == null ? this.getShortName() : this.translation));
        }

        @Override
        public String getTooltip() {
            String s1 = "ZombieConfig".equals(this.tableName) ? Translator.getTextOrNull("Sandbox_" + (this.translation == null ? this.getShortName() : this.translation) + "_help") : Translator.getTextOrNull("Sandbox_" + (this.translation == null ? this.getShortName() : this.translation) + "_tooltip");
            String s2 = Translator.getText("Sandbox_MinMaxDefault", String.format("%.02f", this.min), String.format("%.02f", this.max), String.format("%.02f", this.defaultValue));
            if (s1 == null) {
                return s2;
            }
            if (s2 == null) {
                return s1;
            }
            return s1 + "\\n" + s2;
        }

        @Override
        public void fromTable(KahluaTable table) {
            Object o;
            if (this.tableName != null) {
                Object table2 = table.rawget(this.tableName);
                if (table2 instanceof KahluaTable) {
                    KahluaTable kahluaTable;
                    table = kahluaTable = (KahluaTable)table2;
                } else {
                    return;
                }
            }
            if ((o = table.rawget(this.getShortName())) != null) {
                this.setValueFromObject(o);
            }
        }

        @Override
        public void toTable(KahluaTable table) {
            if (this.tableName != null) {
                Object o = table.rawget(this.tableName);
                if (o instanceof KahluaTable) {
                    KahluaTable kahluaTable;
                    table = kahluaTable = (KahluaTable)o;
                } else {
                    KahluaTable table2 = LuaManager.platform.newTable();
                    table.rawset(this.tableName, (Object)table2);
                    table = table2;
                }
            }
            table.rawset(this.getShortName(), this.getValueAsObject());
        }

        @Override
        public void setCustom() {
            this.custom = true;
        }

        @Override
        public boolean isCustom() {
            return this.custom;
        }

        @Override
        public DoubleSandboxOption setPageName(String pageName) {
            this.pageName = pageName;
            return this;
        }

        @Override
        public String getPageName() {
            return this.pageName;
        }
    }

    @UsedFromLua
    public static class StringSandboxOption
    extends StringConfigOption
    implements SandboxOption {
        protected String translation;
        protected String tableName;
        protected String shortName;
        protected boolean custom;
        protected String pageName;

        public StringSandboxOption(SandboxOptions owner, String name, String defaultValue, int maxLength) {
            super(name, defaultValue, maxLength);
            String[] ss = SandboxOptions.parseName(name);
            this.tableName = ss[0];
            this.shortName = ss[1];
            owner.addOption(this);
        }

        @Override
        public StringConfigOption asConfigOption() {
            return this;
        }

        @Override
        public String getShortName() {
            return this.shortName;
        }

        @Override
        public String getTableName() {
            return this.tableName;
        }

        @Override
        public StringSandboxOption setTranslation(String translation) {
            this.translation = translation;
            return this;
        }

        @Override
        public String getTranslatedName() {
            return Translator.getText("Sandbox_" + (this.translation == null ? this.getShortName() : this.translation));
        }

        @Override
        public String getTooltip() {
            return Translator.getTextOrNull("Sandbox_" + (this.translation == null ? this.getShortName() : this.translation) + "_tooltip");
        }

        @Override
        public void fromTable(KahluaTable table) {
            Object o;
            if (this.tableName != null) {
                Object table2 = table.rawget(this.tableName);
                if (table2 instanceof KahluaTable) {
                    KahluaTable kahluaTable;
                    table = kahluaTable = (KahluaTable)table2;
                } else {
                    return;
                }
            }
            if ((o = table.rawget(this.getShortName())) != null) {
                this.setValueFromObject(o);
            }
        }

        @Override
        public void toTable(KahluaTable table) {
            if (this.tableName != null) {
                Object o = table.rawget(this.tableName);
                if (o instanceof KahluaTable) {
                    KahluaTable kahluaTable;
                    table = kahluaTable = (KahluaTable)o;
                } else {
                    KahluaTable table2 = LuaManager.platform.newTable();
                    table.rawset(this.tableName, (Object)table2);
                    table = table2;
                }
            }
            table.rawset(this.getShortName(), this.getValueAsObject());
        }

        @Override
        public void setCustom() {
            this.custom = true;
        }

        @Override
        public boolean isCustom() {
            return this.custom;
        }

        @Override
        public StringSandboxOption setPageName(String pageName) {
            this.pageName = pageName;
            return this;
        }

        @Override
        public String getPageName() {
            return this.pageName;
        }
    }

    public final class Basement {
        public final EnumSandboxOption spawnFrequency;
        final /* synthetic */ SandboxOptions this$0;

        public Basement(SandboxOptions this$0) {
            SandboxOptions sandboxOptions = this$0;
            Objects.requireNonNull(sandboxOptions);
            this.this$0 = sandboxOptions;
            this.spawnFrequency = this.this$0.newEnumOption("Basement.SpawnFrequency", 7, 4).setTranslation("BasementSpawnFrequency");
        }
    }

    public final class Map {
        public final BooleanSandboxOption allowMiniMap;
        public final BooleanSandboxOption allowWorldMap;
        public final BooleanSandboxOption mapAllKnown;
        public final BooleanSandboxOption mapNeedsLight;
        final /* synthetic */ SandboxOptions this$0;

        public Map(SandboxOptions this$0) {
            SandboxOptions sandboxOptions = this$0;
            Objects.requireNonNull(sandboxOptions);
            this.this$0 = sandboxOptions;
            this.allowMiniMap = this.this$0.newBooleanOption("Map.AllowMiniMap", false);
            this.allowWorldMap = this.this$0.newBooleanOption("Map.AllowWorldMap", true);
            this.mapAllKnown = this.this$0.newBooleanOption("Map.MapAllKnown", false);
            this.mapNeedsLight = this.this$0.newBooleanOption("Map.MapNeedsLight", true);
        }
    }

    public final class ZombieLore {
        public final EnumSandboxOption speed;
        public final IntegerSandboxOption sprinterPercentage;
        public final EnumSandboxOption strength;
        public final EnumSandboxOption toughness;
        public final EnumSandboxOption transmission;
        public final EnumSandboxOption mortality;
        public final EnumSandboxOption reanimate;
        public final EnumSandboxOption cognition;
        public final IntegerSandboxOption doorOpeningPercentage;
        public final EnumSandboxOption crawlUnderVehicle;
        public final EnumSandboxOption memory;
        public final EnumSandboxOption sight;
        public final EnumSandboxOption hearing;
        public final BooleanSandboxOption spottedLogic;
        public final BooleanSandboxOption thumpNoChasing;
        public final BooleanSandboxOption thumpOnConstruction;
        public final EnumSandboxOption activeOnly;
        public final BooleanSandboxOption triggerHouseAlarm;
        public final BooleanSandboxOption zombiesDragDown;
        public final BooleanSandboxOption zombiesCrawlersDragDown;
        public final BooleanSandboxOption zombiesFenceLunge;
        public final DoubleSandboxOption zombiesArmorFactor;
        public final IntegerSandboxOption zombiesMaxDefense;
        public final IntegerSandboxOption chanceOfAttachedWeapon;
        public final DoubleSandboxOption zombiesFallDamage;
        public final EnumSandboxOption disableFakeDead;
        public final EnumSandboxOption playerSpawnZombieRemoval;
        public final IntegerSandboxOption fenceThumpersRequired;
        public final DoubleSandboxOption fenceDamageMultiplier;
        final /* synthetic */ SandboxOptions this$0;

        public ZombieLore(SandboxOptions this$0) {
            SandboxOptions sandboxOptions = this$0;
            Objects.requireNonNull(sandboxOptions);
            this.this$0 = sandboxOptions;
            this.speed = this.this$0.newEnumOption("ZombieLore.Speed", 4, 2).setTranslation("ZSpeed");
            this.sprinterPercentage = this.this$0.newIntegerOption("ZombieLore.SprinterPercentage", 0, 100, 33).setTranslation("ZSprinterPercentage");
            this.strength = this.this$0.newEnumOption("ZombieLore.Strength", 4, 2).setTranslation("ZStrength");
            this.toughness = this.this$0.newEnumOption("ZombieLore.Toughness", 4, 2).setTranslation("ZToughness");
            this.transmission = this.this$0.newEnumOption("ZombieLore.Transmission", 4, 1).setTranslation("ZTransmission");
            this.mortality = this.this$0.newEnumOption("ZombieLore.Mortality", 7, 5).setTranslation("ZInfectionMortality");
            this.reanimate = this.this$0.newEnumOption("ZombieLore.Reanimate", 6, 3).setTranslation("ZReanimateTime");
            this.cognition = this.this$0.newEnumOption("ZombieLore.Cognition", 4, 3).setTranslation("ZCognition");
            this.doorOpeningPercentage = this.this$0.newIntegerOption("ZombieLore.DoorOpeningPercentage", 0, 100, 33).setTranslation("ZDoorOpeningPercentage");
            this.crawlUnderVehicle = this.this$0.newEnumOption("ZombieLore.CrawlUnderVehicle", 7, 5).setTranslation("ZCrawlUnderVehicle");
            this.memory = this.this$0.newEnumOption("ZombieLore.Memory", 6, 2).setTranslation("ZMemory");
            this.sight = this.this$0.newEnumOption("ZombieLore.Sight", 5, 2).setTranslation("ZSight");
            this.hearing = this.this$0.newEnumOption("ZombieLore.Hearing", 5, 2).setTranslation("ZHearing");
            this.spottedLogic = this.this$0.newBooleanOption("ZombieLore.SpottedLogic", true);
            this.thumpNoChasing = this.this$0.newBooleanOption("ZombieLore.ThumpNoChasing", false);
            this.thumpOnConstruction = this.this$0.newBooleanOption("ZombieLore.ThumpOnConstruction", true);
            this.activeOnly = this.this$0.newEnumOption("ZombieLore.ActiveOnly", 3, 1).setTranslation("ActiveOnly");
            this.triggerHouseAlarm = this.this$0.newBooleanOption("ZombieLore.TriggerHouseAlarm", false);
            this.zombiesDragDown = this.this$0.newBooleanOption("ZombieLore.ZombiesDragDown", true);
            this.zombiesCrawlersDragDown = this.this$0.newBooleanOption("ZombieLore.ZombiesCrawlersDragDown", false);
            this.zombiesFenceLunge = this.this$0.newBooleanOption("ZombieLore.ZombiesFenceLunge", true);
            this.zombiesArmorFactor = this.this$0.newDoubleOption("ZombieLore.ZombiesArmorFactor", 0.0, 100.0, 2.0);
            this.zombiesMaxDefense = this.this$0.newIntegerOption("ZombieLore.ZombiesMaxDefense", 0, 100, 85);
            this.chanceOfAttachedWeapon = this.this$0.newIntegerOption("ZombieLore.ChanceOfAttachedWeapon", 0, 100, 6);
            this.zombiesFallDamage = this.this$0.newDoubleOption("ZombieLore.ZombiesFallDamage", 0.0, 100.0, 1.0);
            this.disableFakeDead = this.this$0.newEnumOption("ZombieLore.DisableFakeDead", 3, 1);
            this.playerSpawnZombieRemoval = this.this$0.newEnumOption("ZombieLore.PlayerSpawnZombieRemoval", 4, 1).setTranslation("ZSpawnRemoval");
            this.fenceThumpersRequired = this.this$0.newIntegerOption("ZombieLore.FenceThumpersRequired", -1, 100, 50);
            this.fenceDamageMultiplier = this.this$0.newDoubleOption("ZombieLore.FenceDamageMultiplier", 0.01f, 100.0, 1.0);
        }
    }

    public final class ZombieConfig {
        public final DoubleSandboxOption populationMultiplier;
        public final DoubleSandboxOption populationStartMultiplier;
        public final DoubleSandboxOption populationPeakMultiplier;
        public final IntegerSandboxOption populationPeakDay;
        public final DoubleSandboxOption respawnHours;
        public final DoubleSandboxOption respawnUnseenHours;
        public final DoubleSandboxOption respawnMultiplier;
        public final DoubleSandboxOption redistributeHours;
        public final IntegerSandboxOption followSoundDistance;
        public final IntegerSandboxOption rallyGroupSize;
        public final IntegerSandboxOption rallyGroupSizeVariance;
        public final IntegerSandboxOption rallyTravelDistance;
        public final IntegerSandboxOption rallyGroupSeparation;
        public final IntegerSandboxOption rallyGroupRadius;
        public final IntegerSandboxOption zombiesCountBeforeDeletion;
        final /* synthetic */ SandboxOptions this$0;

        public ZombieConfig(SandboxOptions this$0) {
            SandboxOptions sandboxOptions = this$0;
            Objects.requireNonNull(sandboxOptions);
            this.this$0 = sandboxOptions;
            this.populationMultiplier = this.this$0.newDoubleOption("ZombieConfig.PopulationMultiplier", 0.0, 4.0, 0.65f);
            this.populationStartMultiplier = this.this$0.newDoubleOption("ZombieConfig.PopulationStartMultiplier", 0.0, 4.0, 1.0);
            this.populationPeakMultiplier = this.this$0.newDoubleOption("ZombieConfig.PopulationPeakMultiplier", 0.0, 4.0, 1.5);
            this.populationPeakDay = this.this$0.newIntegerOption("ZombieConfig.PopulationPeakDay", 1, 365, 28);
            this.respawnHours = this.this$0.newDoubleOption("ZombieConfig.RespawnHours", 0.0, 8760.0, 72.0);
            this.respawnUnseenHours = this.this$0.newDoubleOption("ZombieConfig.RespawnUnseenHours", 0.0, 8760.0, 16.0);
            this.respawnMultiplier = this.this$0.newDoubleOption("ZombieConfig.RespawnMultiplier", 0.0, 1.0, 0.1);
            this.redistributeHours = this.this$0.newDoubleOption("ZombieConfig.RedistributeHours", 0.0, 8760.0, 12.0);
            this.followSoundDistance = this.this$0.newIntegerOption("ZombieConfig.FollowSoundDistance", 10, 1000, 100);
            this.rallyGroupSize = this.this$0.newIntegerOption("ZombieConfig.RallyGroupSize", 0, 1000, 20);
            this.rallyGroupSizeVariance = this.this$0.newIntegerOption("ZombieConfig.RallyGroupSizeVariance", 0, 100, 50);
            this.rallyTravelDistance = this.this$0.newIntegerOption("ZombieConfig.RallyTravelDistance", 5, 50, 20);
            this.rallyGroupSeparation = this.this$0.newIntegerOption("ZombieConfig.RallyGroupSeparation", 5, 25, 15);
            this.rallyGroupRadius = this.this$0.newIntegerOption("ZombieConfig.RallyGroupRadius", 1, 10, 3);
            this.zombiesCountBeforeDeletion = this.this$0.newIntegerOption("ZombieConfig.ZombiesCountBeforeDelete", 10, 500, 300);
        }
    }

    public final class MultiplierConfig {
        public final DoubleSandboxOption xpMultiplierGlobal;
        public final BooleanSandboxOption xpMultiplierGlobalToggle;
        public final DoubleSandboxOption xpMultiplierFitness;
        public final DoubleSandboxOption xpMultiplierStrength;
        public final DoubleSandboxOption xpMultiplierSprinting;
        public final DoubleSandboxOption xpMultiplierLightfoot;
        public final DoubleSandboxOption xpMultiplierNimble;
        public final DoubleSandboxOption xpMultiplierSneak;
        public final DoubleSandboxOption xpMultiplierAxe;
        public final DoubleSandboxOption xpMultiplierBlunt;
        public final DoubleSandboxOption xpMultiplierSmallBlunt;
        public final DoubleSandboxOption xpMultiplierLongBlade;
        public final DoubleSandboxOption xpMultiplierSmallBlade;
        public final DoubleSandboxOption xpMultiplierSpear;
        public final DoubleSandboxOption xpMultiplierMaintenance;
        public final DoubleSandboxOption xpMultiplierWoodwork;
        public final DoubleSandboxOption xpMultiplierCooking;
        public final DoubleSandboxOption xpMultiplierFarming;
        public final DoubleSandboxOption xpMultiplierDoctor;
        public final DoubleSandboxOption xpMultiplierElectricity;
        public final DoubleSandboxOption xpMultiplierMetalWelding;
        public final DoubleSandboxOption xpMultiplierMechanics;
        public final DoubleSandboxOption xpMultiplierTailoring;
        public final DoubleSandboxOption xpMultiplierAiming;
        public final DoubleSandboxOption xpMultiplierReloading;
        public final DoubleSandboxOption xpMultiplierFishing;
        public final DoubleSandboxOption xpMultiplierTrapping;
        public final DoubleSandboxOption xpMultiplierPlantScavenging;
        public final DoubleSandboxOption xpMultiplierFlintKnapping;
        public final DoubleSandboxOption xpMultiplierMasonry;
        public final DoubleSandboxOption xpMultiplierPottery;
        public final DoubleSandboxOption xpMultiplierCarving;
        public final DoubleSandboxOption xpMultiplierHusbandry;
        public final DoubleSandboxOption xpMultiplierTracking;
        public final DoubleSandboxOption xpMultiplierBlacksmith;
        public final DoubleSandboxOption xpMultiplierButchering;
        public final DoubleSandboxOption xpMultiplierGlassmaking;
        final /* synthetic */ SandboxOptions this$0;

        public MultiplierConfig(SandboxOptions this$0) {
            SandboxOptions sandboxOptions = this$0;
            Objects.requireNonNull(sandboxOptions);
            this.this$0 = sandboxOptions;
            this.xpMultiplierGlobal = this.this$0.newDoubleOption("MultiplierConfig.Global", 0.0, 1000.0, 1.0);
            this.xpMultiplierGlobalToggle = this.this$0.newBooleanOption("MultiplierConfig.GlobalToggle", true);
            this.xpMultiplierFitness = this.this$0.newDoubleOption("MultiplierConfig.Fitness", 0.0, 1000.0, 1.0);
            this.xpMultiplierStrength = this.this$0.newDoubleOption("MultiplierConfig.Strength", 0.0, 1000.0, 1.0);
            this.xpMultiplierSprinting = this.this$0.newDoubleOption("MultiplierConfig.Sprinting", 0.0, 1000.0, 1.0);
            this.xpMultiplierLightfoot = this.this$0.newDoubleOption("MultiplierConfig.Lightfoot", 0.0, 1000.0, 1.0);
            this.xpMultiplierNimble = this.this$0.newDoubleOption("MultiplierConfig.Nimble", 0.0, 1000.0, 1.0);
            this.xpMultiplierSneak = this.this$0.newDoubleOption("MultiplierConfig.Sneak", 0.0, 1000.0, 1.0);
            this.xpMultiplierAxe = this.this$0.newDoubleOption("MultiplierConfig.Axe", 0.0, 1000.0, 1.0);
            this.xpMultiplierBlunt = this.this$0.newDoubleOption("MultiplierConfig.Blunt", 0.0, 1000.0, 1.0);
            this.xpMultiplierSmallBlunt = this.this$0.newDoubleOption("MultiplierConfig.SmallBlunt", 0.0, 1000.0, 1.0);
            this.xpMultiplierLongBlade = this.this$0.newDoubleOption("MultiplierConfig.LongBlade", 0.0, 1000.0, 1.0);
            this.xpMultiplierSmallBlade = this.this$0.newDoubleOption("MultiplierConfig.SmallBlade", 0.0, 1000.0, 1.0);
            this.xpMultiplierSpear = this.this$0.newDoubleOption("MultiplierConfig.Spear", 0.0, 1000.0, 1.0);
            this.xpMultiplierMaintenance = this.this$0.newDoubleOption("MultiplierConfig.Maintenance", 0.0, 1000.0, 1.0);
            this.xpMultiplierWoodwork = this.this$0.newDoubleOption("MultiplierConfig.Woodwork", 0.0, 1000.0, 1.0);
            this.xpMultiplierCooking = this.this$0.newDoubleOption("MultiplierConfig.Cooking", 0.0, 1000.0, 1.0);
            this.xpMultiplierFarming = this.this$0.newDoubleOption("MultiplierConfig.Farming", 0.0, 1000.0, 1.0);
            this.xpMultiplierDoctor = this.this$0.newDoubleOption("MultiplierConfig.Doctor", 0.0, 1000.0, 1.0);
            this.xpMultiplierElectricity = this.this$0.newDoubleOption("MultiplierConfig.Electricity", 0.0, 1000.0, 1.0);
            this.xpMultiplierMetalWelding = this.this$0.newDoubleOption("MultiplierConfig.MetalWelding", 0.0, 1000.0, 1.0);
            this.xpMultiplierMechanics = this.this$0.newDoubleOption("MultiplierConfig.Mechanics", 0.0, 1000.0, 1.0);
            this.xpMultiplierTailoring = this.this$0.newDoubleOption("MultiplierConfig.Tailoring", 0.0, 1000.0, 1.0);
            this.xpMultiplierAiming = this.this$0.newDoubleOption("MultiplierConfig.Aiming", 0.0, 1000.0, 1.0);
            this.xpMultiplierReloading = this.this$0.newDoubleOption("MultiplierConfig.Reloading", 0.0, 1000.0, 1.0);
            this.xpMultiplierFishing = this.this$0.newDoubleOption("MultiplierConfig.Fishing", 0.0, 1000.0, 1.0);
            this.xpMultiplierTrapping = this.this$0.newDoubleOption("MultiplierConfig.Trapping", 0.0, 1000.0, 1.0);
            this.xpMultiplierPlantScavenging = this.this$0.newDoubleOption("MultiplierConfig.PlantScavenging", 0.0, 1000.0, 1.0);
            this.xpMultiplierFlintKnapping = this.this$0.newDoubleOption("MultiplierConfig.FlintKnapping", 0.0, 1000.0, 1.0);
            this.xpMultiplierMasonry = this.this$0.newDoubleOption("MultiplierConfig.Masonry", 0.0, 1000.0, 1.0);
            this.xpMultiplierPottery = this.this$0.newDoubleOption("MultiplierConfig.Pottery", 0.0, 1000.0, 1.0);
            this.xpMultiplierCarving = this.this$0.newDoubleOption("MultiplierConfig.Carving", 0.0, 1000.0, 1.0);
            this.xpMultiplierHusbandry = this.this$0.newDoubleOption("MultiplierConfig.Husbandry", 0.0, 1000.0, 1.0);
            this.xpMultiplierTracking = this.this$0.newDoubleOption("MultiplierConfig.Tracking", 0.0, 1000.0, 1.0);
            this.xpMultiplierBlacksmith = this.this$0.newDoubleOption("MultiplierConfig.Blacksmith", 0.0, 1000.0, 1.0);
            this.xpMultiplierButchering = this.this$0.newDoubleOption("MultiplierConfig.Butchering", 0.0, 1000.0, 1.0);
            this.xpMultiplierGlassmaking = this.this$0.newDoubleOption("MultiplierConfig.Glassmaking", 0.0, 1000.0, 1.0);
        }
    }

    public static interface SandboxOption {
        public ConfigOption asConfigOption();

        public String getShortName();

        public String getTableName();

        public SandboxOption setTranslation(String var1);

        public String getTranslatedName();

        public String getTooltip();

        public void fromTable(KahluaTable var1);

        public void toTable(KahluaTable var1);

        public void setCustom();

        public boolean isCustom();

        public SandboxOption setPageName(String var1);

        public String getPageName();
    }
}

