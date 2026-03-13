/*
 * Decompiled with CFR 0.152.
 */
package zombie.scripting.objects;

import generation.builders.validation.TranslationKeyValidator;
import zombie.UsedFromLua;
import zombie.core.Core;
import zombie.scripting.objects.Registries;
import zombie.scripting.objects.RegistryReset;
import zombie.scripting.objects.ResourceLocation;

@UsedFromLua
public class Flier {
    public static final Flier A1_HAY = Flier.registerBase("A1Hay");
    public static final Flier AMZ_STEEL = Flier.registerBase("AMZSteel");
    public static final Flier ALS_AUTO_SHOP = Flier.registerBase("AlsAutoShop");
    public static final Flier AMERICAN_TIRE = Flier.registerBase("AmericanTire");
    public static final Flier BEEF_CHUNK = Flier.registerBase("BeefChunk");
    public static final Flier BENS_CABIN = Flier.registerBase("BensCabin");
    public static final Flier BRANDENBURG_FD = Flier.registerBase("BrandenburgFD");
    public static final Flier BROOKS_LIBRARY = Flier.registerBase("BrooksLibrary");
    public static final Flier BROTT_AUCTION = Flier.registerBase("BrottAuction");
    public static final Flier CABIN_FOR_RENT_DIXIE = Flier.registerBase("CabinforRentDixie");
    public static final Flier CAR_FIXATION = Flier.registerBase("CarFixation");
    public static final Flier CAT_ON_A_HOT_TIN_GRILL = Flier.registerBase("CatonaHotTinGrill");
    public static final Flier CIRCUITAL_HEALING = Flier.registerBase("CircuitalHealing");
    public static final Flier DRAG_RACING_TRACK = Flier.registerBase("DragRacingTrack");
    public static final Flier DU_CASE_APARTMENTS = Flier.registerBase("DuCaseApartments");
    public static final Flier EP_TOOLS_LV = Flier.registerBase("EPToolsLV");
    public static final Flier EKRON_COLLEGE = Flier.registerBase("EkronCollege");
    public static final Flier ELVEE_ARENA = Flier.registerBase("ElveeArena");
    public static final Flier FALLAS_LAKE_CHURCH = Flier.registerBase("FallasLakeChurch");
    public static final Flier FARMERS_MARKET = Flier.registerBase("FarmersMarket");
    public static final Flier FARMING_AND_RURAL_SUPPLY_DOE_VALLEY = Flier.registerBase("FarmingAndRuralSupplyDoeValley");
    public static final Flier FASHION_A_BELLE = Flier.registerBase("FashionaBelle");
    public static final Flier FIVE_ALARM_CHILI = Flier.registerBase("FiveAlarmChili");
    public static final Flier FOSSOIL_1 = Flier.registerBase("Fossoil1");
    public static final Flier FOSSOIL_2 = Flier.registerBase("Fossoil2");
    public static final Flier FOSSOIL_3 = Flier.registerBase("Fossoil3");
    public static final Flier FOSSOIL_4 = Flier.registerBase("Fossoil4");
    public static final Flier FOSSOIL_5 = Flier.registerBase("Fossoil5");
    public static final Flier FOSSOIL_6 = Flier.registerBase("Fossoil6");
    public static final Flier FOSSOIL_7 = Flier.registerBase("Fossoil7");
    public static final Flier FOSSOIL_8 = Flier.registerBase("Fossoil8");
    public static final Flier FOURTH_OF_JULY_CELEBRATION_DIXIE_MOBILE_PARK = Flier.registerBase("FourthofJulyCelebrationDixieMobilePark");
    public static final Flier GNOME_SWEET_GNOME = Flier.registerBase("GnomeSweetGnome");
    public static final Flier GOLDEN_SUNSET = Flier.registerBase("GoldenSunset");
    public static final Flier GREENES_JOB_AD_EKRON = Flier.registerBase("GreenesJobAdEkron");
    public static final Flier GUNS_UNLIMITED_ECHO_CREEK = Flier.registerBase("GunsUnlimitedEchoCreek");
    public static final Flier HIGH_STREET_APARTMENTS = Flier.registerBase("HighStreetApartments");
    public static final Flier HIT_VIDS_JOB_AD_MARCH_RIDGE = Flier.registerBase("HitVidsJobAdMarchRidge");
    public static final Flier HOBBS_AND_PERKINS_HARDWARE = Flier.registerBase("HobbsandPerkinsHardware");
    public static final Flier HOUSE_FOR_SALE_787 = Flier.registerBase("HouseforSale787");
    public static final Flier HOUSE_FOR_SALE_799 = Flier.registerBase("HouseforSale799");
    public static final Flier HOUSE_FOR_SALE_818 = Flier.registerBase("HouseforSale818");
    public static final Flier HOUSE_FOR_SALE_845 = Flier.registerBase("HouseforSale845");
    public static final Flier HOUSE_FOR_SALE_851 = Flier.registerBase("HouseforSale851");
    public static final Flier HOUSE_FOR_SALE_855 = Flier.registerBase("HouseforSale855");
    public static final Flier HOUSE_FOR_SALE_860 = Flier.registerBase("HouseforSale860");
    public static final Flier HOUSE_FOR_SALE_867 = Flier.registerBase("HouseforSale867");
    public static final Flier HOUSE_FOR_SALE_895 = Flier.registerBase("HouseforSale895");
    public static final Flier HOUSE_FOR_SALE_903 = Flier.registerBase("HouseforSale903");
    public static final Flier HOUSE_FOR_SALE_907 = Flier.registerBase("HouseforSale907");
    public static final Flier HOUSE_FOR_SALE_912 = Flier.registerBase("HouseforSale912");
    public static final Flier HOUSE_FOR_SALE_915 = Flier.registerBase("HouseforSale915");
    public static final Flier HOUSE_FOR_SALE_919 = Flier.registerBase("HouseforSale919");
    public static final Flier HOUSE_FOR_SALE_922 = Flier.registerBase("HouseforSale922");
    public static final Flier HOUSE_FOR_SALE_929 = Flier.registerBase("HouseforSale929");
    public static final Flier HOUSE_FOR_SALE_930 = Flier.registerBase("HouseforSale930");
    public static final Flier HOUSE_FOR_SALE_934 = Flier.registerBase("HouseforSale934");
    public static final Flier HOUSE_FOR_SALE_943 = Flier.registerBase("HouseforSale943");
    public static final Flier IRVINGTON_GUN_CLUB = Flier.registerBase("IrvingtonGunClub");
    public static final Flier KNOX_BANK_JOB_AD_ROSEWOOD = Flier.registerBase("KnoxBankJobAdRosewood");
    public static final Flier KNOX_GUN_OWNERS_CLUB_GET_TOGETHER = Flier.registerBase("KnoxGunOwnersClubGetTogether");
    public static final Flier KNOX_PACK_KITCHENS = Flier.registerBase("KnoxPackKitchens");
    public static final Flier LVFD = Flier.registerBase("LVFD");
    public static final Flier LVPD_HQ = Flier.registerBase("LVPDHQ");
    public static final Flier LEAFHILL_HEIGHTS = Flier.registerBase("LeafhillHeights");
    public static final Flier LECTROMAX_MANUFACTURING_JOB_AD = Flier.registerBase("LectromaxManufacturingJobAd");
    public static final Flier LENNYS_CAR_REPAIR = Flier.registerBase("LennysCarRepair");
    public static final Flier LOUISVILLE_BRUISER = Flier.registerBase("LouisvilleBruiser");
    public static final Flier LOVE_DUET = Flier.registerBase("LoveDuet");
    public static final Flier LOWRY_COURT = Flier.registerBase("LowryCourt");
    public static final Flier MAD_DANS_DEN = Flier.registerBase("MadDansDen");
    public static final Flier MAIL_CARRIER_AD_EKRON = Flier.registerBase("MailCarrierAdEkron");
    public static final Flier MARCH_RIDGE_SCHOOL_JOB_AD = Flier.registerBase("MarchRidgeSchoolJobAd");
    public static final Flier MCCOY_LOGGING_CORP = Flier.registerBase("McCoyLoggingCorp");
    public static final Flier MEADSHIRE_ESTATES = Flier.registerBase("MeadshireEstate");
    public static final Flier MULDRAUGH_BAKE_SALE = Flier.registerBase("MuldraughBakeSale");
    public static final Flier MULDRAUGH_PD = Flier.registerBase("MuldraughPD");
    public static final Flier MUSIC_FEST_93 = Flier.registerBase("MusicFest93");
    public static final Flier NAILS_AND_NUTS = Flier.registerBase("NailsAndNuts");
    public static final Flier NOLANS_USED_CARS = Flier.registerBase("NolansUsedCars");
    public static final Flier OLD_CGE_CORP_BUILDING = Flier.registerBase("OldCGECorpBuilding");
    public static final Flier ONYX_DRIVE_IN_THEATER = Flier.registerBase("OnyxDriveinTheater");
    public static final Flier OVO_FARMS = Flier.registerBase("OvoFarms");
    public static final Flier PILEO_CREPE_JOB_AD_CROSS_ROADS_MALL = Flier.registerBase("PileoCrepeJobAdCrossRoadsMall");
    public static final Flier PIZZA_WHIRLED_JOB_AD_ROSEWOOD = Flier.registerBase("PizzaWhirledJobAdRosewood");
    public static final Flier PREMISES_FOR_LEASE_863 = Flier.registerBase("PremisesforLease863");
    public static final Flier PREMISES_WITH_APARTMENTS_FOR_LEASE_LISTING_NO_891 = Flier.registerBase("PremiseswithApartmentsforLeaselistingno891");
    public static final Flier READY_PREP = Flier.registerBase("ReadyPrep");
    public static final Flier RED_OAK_APARTMENTS = Flier.registerBase("RedOakApartments");
    public static final Flier RIVERSIDE_INDEPENDENCE_DAY_PARTY_ALL_WELCOME = Flier.registerBase("RiversideIndependenceDayPartyAllWelcome");
    public static final Flier RIVERSIDE_PD = Flier.registerBase("RiversidePD");
    public static final Flier ROSEWOOD_FD = Flier.registerBase("RosewoodFD");
    public static final Flier ROXYS_ROLLER_RINK = Flier.registerBase("RoxysRollerRink");
    public static final Flier RUSTY_RIFLE = Flier.registerBase("RustyRifle");
    public static final Flier SAMMIES = Flier.registerBase("Sammies");
    public static final Flier SPIFFOS_HIRING_DIXIE = Flier.registerBase("SpiffosHiringDixie");
    public static final Flier SPIFFOS_HIRING_LOUISVILLE = Flier.registerBase("SpiffosHiringLouisville");
    public static final Flier SPIFFOS_HIRING_WEST_POINT = Flier.registerBase("SpiffosHiringWestPoint");
    public static final Flier STUART_AND_LOG_SCRAPYARD = Flier.registerBase("StuartandLogScrapyard");
    public static final Flier SUNSET_PINES_FUNERAL_HOME = Flier.registerBase("SunsetPinesFuneralHome");
    public static final Flier SURE_FITNESS_BOXING_CLUB = Flier.registerBase("SureFitnessBoxingClub");
    public static final Flier TACO_DEL_PANCHO = Flier.registerBase("TacodelPancho");
    public static final Flier THE_SEA_SHANTY = Flier.registerBase("TheSeaShanty");
    public static final Flier THE_WIZARDS_KEEP = Flier.registerBase("TheWizardsKeep");
    public static final Flier TWIGGYS = Flier.registerBase("Twiggys");
    public static final Flier U_STORE_IT_LOUISVILLE = Flier.registerBase("UStoreItLouisville");
    public static final Flier U_STORE_IT_MULDRAUGH = Flier.registerBase("UStoreItMuldraugh");
    public static final Flier U_STORE_IT_RIVERSIDE = Flier.registerBase("UStoreItRiverside");
    public static final Flier UPSCALE_MOBILITY = Flier.registerBase("UpscaleMobility");
    public static final Flier WPDIY = Flier.registerBase("WPDIY");
    public static final Flier WP_TOWN_HALL = Flier.registerBase("WPTownHall");
    public static final Flier YOUR_LOCAL_SHELTER_BRANDENBURG = Flier.registerBase("YourLocalShelterBrandenburg");
    private final String translationKey;
    private final String translationInfoKey;
    private final String translationTextKey;

    private Flier(String id) {
        this.translationKey = "Print_Media_" + id + "_title";
        this.translationInfoKey = "Print_Media_" + id + "_info";
        this.translationTextKey = "Print_Text_" + id + "_info";
    }

    public static Flier get(ResourceLocation id) {
        return Registries.FLIER.get(id);
    }

    public String toString() {
        return Registries.FLIER.getLocation(this).getPath();
    }

    public String getTranslationKey() {
        return this.translationKey;
    }

    public String getTranslationInfoKey() {
        return this.translationInfoKey;
    }

    public String getTranslationTextKey() {
        return this.translationTextKey;
    }

    public static Flier register(String id) {
        return Flier.register(false, id);
    }

    private static Flier registerBase(String id) {
        return Flier.register(true, id);
    }

    private static Flier register(boolean allowDefaultNamespace, String id) {
        return Registries.FLIER.register(RegistryReset.createLocation(id, allowDefaultNamespace), new Flier(id));
    }

    static {
        if (Core.IS_DEV) {
            for (Flier flier : Registries.FLIER) {
                TranslationKeyValidator.of(flier.translationKey);
                TranslationKeyValidator.of(flier.translationInfoKey);
                TranslationKeyValidator.of(flier.translationTextKey);
            }
        }
    }
}

