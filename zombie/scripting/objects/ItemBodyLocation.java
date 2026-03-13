/*
 * Decompiled with CFR 0.152.
 */
package zombie.scripting.objects;

import zombie.UsedFromLua;
import zombie.scripting.objects.Registries;
import zombie.scripting.objects.RegistryReset;
import zombie.scripting.objects.ResourceLocation;

@UsedFromLua
public class ItemBodyLocation {
    public static final ItemBodyLocation AMMO_STRAP = ItemBodyLocation.registerBase("AmmoStrap");
    public static final ItemBodyLocation ANKLE_HOLSTER = ItemBodyLocation.registerBase("AnkleHolster");
    public static final ItemBodyLocation BACK = ItemBodyLocation.registerBase("Back");
    public static final ItemBodyLocation BANDAGE = ItemBodyLocation.registerBase("Bandage");
    public static final ItemBodyLocation BATH_ROBE = ItemBodyLocation.registerBase("BathRobe");
    public static final ItemBodyLocation BELLY_BUTTON = ItemBodyLocation.registerBase("BellyButton");
    public static final ItemBodyLocation BELT = ItemBodyLocation.registerBase("Belt");
    public static final ItemBodyLocation BELT_EXTRA = ItemBodyLocation.registerBase("BeltExtra");
    public static final ItemBodyLocation BODY_COSTUME = ItemBodyLocation.registerBase("BodyCostume");
    public static final ItemBodyLocation BOILERSUIT = ItemBodyLocation.registerBase("Boilersuit");
    public static final ItemBodyLocation CALF_LEFT = ItemBodyLocation.registerBase("Calf_Left");
    public static final ItemBodyLocation CALF_LEFT_TEXTURE = ItemBodyLocation.registerBase("Calf_Left_Texture");
    public static final ItemBodyLocation CALF_RIGHT = ItemBodyLocation.registerBase("Calf_Right");
    public static final ItemBodyLocation CALF_RIGHT_TEXTURE = ItemBodyLocation.registerBase("Calf_Right_Texture");
    public static final ItemBodyLocation CODPIECE = ItemBodyLocation.registerBase("Codpiece");
    public static final ItemBodyLocation CUIRASS = ItemBodyLocation.registerBase("Cuirass");
    public static final ItemBodyLocation DRESS = ItemBodyLocation.registerBase("Dress");
    public static final ItemBodyLocation EARS = ItemBodyLocation.registerBase("Ears");
    public static final ItemBodyLocation EAR_TOP = ItemBodyLocation.registerBase("EarTop");
    public static final ItemBodyLocation ELBOW_LEFT = ItemBodyLocation.registerBase("Elbow_Left");
    public static final ItemBodyLocation ELBOW_RIGHT = ItemBodyLocation.registerBase("Elbow_Right");
    public static final ItemBodyLocation EYES = ItemBodyLocation.registerBase("Eyes");
    public static final ItemBodyLocation FANNY_PACK_BACK = ItemBodyLocation.registerBase("FannyPackBack");
    public static final ItemBodyLocation FANNY_PACK_FRONT = ItemBodyLocation.registerBase("FannyPackFront");
    public static final ItemBodyLocation FORE_ARM_LEFT = ItemBodyLocation.registerBase("ForeArm_Left");
    public static final ItemBodyLocation FORE_ARM_RIGHT = ItemBodyLocation.registerBase("ForeArm_Right");
    public static final ItemBodyLocation FULL_HAT = ItemBodyLocation.registerBase("FullHat");
    public static final ItemBodyLocation FULL_ROBE = ItemBodyLocation.registerBase("FullRobe");
    public static final ItemBodyLocation FULL_SUIT = ItemBodyLocation.registerBase("FullSuit");
    public static final ItemBodyLocation FULL_SUIT_HEAD = ItemBodyLocation.registerBase("FullSuitHead");
    public static final ItemBodyLocation FULL_SUIT_HEAD_SCBA = ItemBodyLocation.registerBase("FullSuitHeadSCBA");
    public static final ItemBodyLocation FULL_TOP = ItemBodyLocation.registerBase("FullTop");
    public static final ItemBodyLocation GAITER_LEFT = ItemBodyLocation.registerBase("Gaiter_Left");
    public static final ItemBodyLocation GAITER_RIGHT = ItemBodyLocation.registerBase("Gaiter_Right");
    public static final ItemBodyLocation GORGET = ItemBodyLocation.registerBase("Gorget");
    public static final ItemBodyLocation HANDS = ItemBodyLocation.registerBase("Hands");
    public static final ItemBodyLocation HANDS_LEFT = ItemBodyLocation.registerBase("HandsLeft");
    public static final ItemBodyLocation HANDS_RIGHT = ItemBodyLocation.registerBase("HandsRight");
    public static final ItemBodyLocation HAT = ItemBodyLocation.registerBase("Hat");
    public static final ItemBodyLocation JACKET = ItemBodyLocation.registerBase("Jacket");
    public static final ItemBodyLocation JACKET_BULKY = ItemBodyLocation.registerBase("Jacket_Bulky");
    public static final ItemBodyLocation JACKET_DOWN = ItemBodyLocation.registerBase("Jacket_Down");
    public static final ItemBodyLocation JACKET_HAT = ItemBodyLocation.registerBase("JacketHat");
    public static final ItemBodyLocation JACKET_HAT_BULKY = ItemBodyLocation.registerBase("JacketHat_Bulky");
    public static final ItemBodyLocation JACKET_SUIT = ItemBodyLocation.registerBase("JacketSuit");
    public static final ItemBodyLocation JERSEY = ItemBodyLocation.registerBase("Jersey");
    public static final ItemBodyLocation KNEE_LEFT = ItemBodyLocation.registerBase("Knee_Left");
    public static final ItemBodyLocation KNEE_RIGHT = ItemBodyLocation.registerBase("Knee_Right");
    public static final ItemBodyLocation LEFT_MIDDLE_FINGER = ItemBodyLocation.registerBase("Left_MiddleFinger");
    public static final ItemBodyLocation LEFT_RING_FINGER = ItemBodyLocation.registerBase("Left_RingFinger");
    public static final ItemBodyLocation LEFT_ARM = ItemBodyLocation.registerBase("LeftArm");
    public static final ItemBodyLocation LEFT_EYE = ItemBodyLocation.registerBase("LeftEye");
    public static final ItemBodyLocation LEFT_WRIST = ItemBodyLocation.registerBase("LeftWrist");
    public static final ItemBodyLocation LEGS1 = ItemBodyLocation.registerBase("Legs1");
    public static final ItemBodyLocation LEGS5 = ItemBodyLocation.registerBase("Legs5");
    public static final ItemBodyLocation LONG_DRESS = ItemBodyLocation.registerBase("LongDress");
    public static final ItemBodyLocation LONG_SKIRT = ItemBodyLocation.registerBase("LongSkirt");
    public static final ItemBodyLocation MAKE_UP_EYES = ItemBodyLocation.registerBase("MakeUp_Eyes");
    public static final ItemBodyLocation MAKE_UP_EYES_SHADOW = ItemBodyLocation.registerBase("MakeUp_EyesShadow");
    public static final ItemBodyLocation MAKE_UP_FULL_FACE = ItemBodyLocation.registerBase("MakeUp_FullFace");
    public static final ItemBodyLocation MAKE_UP_LIPS = ItemBodyLocation.registerBase("MakeUp_Lips");
    public static final ItemBodyLocation MASK = ItemBodyLocation.registerBase("Mask");
    public static final ItemBodyLocation MASK_EYES = ItemBodyLocation.registerBase("MaskEyes");
    public static final ItemBodyLocation MASK_FULL = ItemBodyLocation.registerBase("MaskFull");
    public static final ItemBodyLocation NECK = ItemBodyLocation.registerBase("Neck");
    public static final ItemBodyLocation NECK_TEXTURE = ItemBodyLocation.registerBase("Neck_Texture");
    public static final ItemBodyLocation NECKLACE = ItemBodyLocation.registerBase("Necklace");
    public static final ItemBodyLocation NECKLACE_LONG = ItemBodyLocation.registerBase("Necklace_Long");
    public static final ItemBodyLocation NOSE = ItemBodyLocation.registerBase("Nose");
    public static final ItemBodyLocation PANTS = ItemBodyLocation.registerBase("Pants");
    public static final ItemBodyLocation PANTS_SKINNY = ItemBodyLocation.registerBase("Pants_Skinny");
    public static final ItemBodyLocation PANTS_EXTRA = ItemBodyLocation.registerBase("PantsExtra");
    public static final ItemBodyLocation RIGHT_MIDDLE_FINGER = ItemBodyLocation.registerBase("Right_MiddleFinger");
    public static final ItemBodyLocation RIGHT_RING_FINGER = ItemBodyLocation.registerBase("Right_RingFinger");
    public static final ItemBodyLocation RIGHT_ARM = ItemBodyLocation.registerBase("RightArm");
    public static final ItemBodyLocation RIGHT_EYE = ItemBodyLocation.registerBase("RightEye");
    public static final ItemBodyLocation RIGHT_WRIST = ItemBodyLocation.registerBase("RightWrist");
    public static final ItemBodyLocation SATCHEL = ItemBodyLocation.registerBase("Satchel");
    public static final ItemBodyLocation SCARF = ItemBodyLocation.registerBase("Scarf");
    public static final ItemBodyLocation SCBA = ItemBodyLocation.registerBase("SCBA");
    public static final ItemBodyLocation SCBANOTANK = ItemBodyLocation.registerBase("SCBAnotank");
    public static final ItemBodyLocation SHIRT = ItemBodyLocation.registerBase("Shirt");
    public static final ItemBodyLocation SHOES = ItemBodyLocation.registerBase("Shoes");
    public static final ItemBodyLocation SHORT_PANTS = ItemBodyLocation.registerBase("ShortPants");
    public static final ItemBodyLocation SHORT_SLEEVE_SHIRT = ItemBodyLocation.registerBase("ShortSleeveShirt");
    public static final ItemBodyLocation SHORTS_SHORT = ItemBodyLocation.registerBase("ShortsShort");
    public static final ItemBodyLocation SHOULDER_HOLSTER = ItemBodyLocation.registerBase("ShoulderHolster");
    public static final ItemBodyLocation SHOULDERPAD_LEFT = ItemBodyLocation.registerBase("ShoulderpadLeft");
    public static final ItemBodyLocation SHOULDERPAD_RIGHT = ItemBodyLocation.registerBase("ShoulderpadRight");
    public static final ItemBodyLocation SKIRT = ItemBodyLocation.registerBase("Skirt");
    public static final ItemBodyLocation SOCKS = ItemBodyLocation.registerBase("Socks");
    public static final ItemBodyLocation SPORT_SHOULDERPAD = ItemBodyLocation.registerBase("SportShoulderpad");
    public static final ItemBodyLocation SPORT_SHOULDERPAD_ON_TOP = ItemBodyLocation.registerBase("SportShoulderpadOnTop");
    public static final ItemBodyLocation SWEATER = ItemBodyLocation.registerBase("Sweater");
    public static final ItemBodyLocation SWEATER_HAT = ItemBodyLocation.registerBase("SweaterHat");
    public static final ItemBodyLocation TAIL = ItemBodyLocation.registerBase("Tail");
    public static final ItemBodyLocation TANK_TOP = ItemBodyLocation.registerBase("TankTop");
    public static final ItemBodyLocation THIGH_LEFT = ItemBodyLocation.registerBase("Thigh_Left");
    public static final ItemBodyLocation THIGH_RIGHT = ItemBodyLocation.registerBase("Thigh_Right");
    public static final ItemBodyLocation TORSO1LEGS1 = ItemBodyLocation.registerBase("Torso1Legs1");
    public static final ItemBodyLocation TORSO1 = ItemBodyLocation.registerBase("Torso1");
    public static final ItemBodyLocation TORSO_EXTRA = ItemBodyLocation.registerBase("TorsoExtra");
    public static final ItemBodyLocation TORSO_EXTRA_VEST = ItemBodyLocation.registerBase("TorsoExtraVest");
    public static final ItemBodyLocation TORSO_EXTRA_VEST_BULLET = ItemBodyLocation.registerBase("TorsoExtraVestBullet");
    public static final ItemBodyLocation TSHIRT = ItemBodyLocation.registerBase("Tshirt");
    public static final ItemBodyLocation UNDERWEAR = ItemBodyLocation.registerBase("Underwear");
    public static final ItemBodyLocation UNDERWEAR_BOTTOM = ItemBodyLocation.registerBase("UnderwearBottom");
    public static final ItemBodyLocation UNDERWEAR_EXTRA1 = ItemBodyLocation.registerBase("UnderwearExtra1");
    public static final ItemBodyLocation UNDERWEAR_EXTRA2 = ItemBodyLocation.registerBase("UnderwearExtra2");
    public static final ItemBodyLocation UNDERWEAR_TOP = ItemBodyLocation.registerBase("UnderwearTop");
    public static final ItemBodyLocation VEST_TEXTURE = ItemBodyLocation.registerBase("VestTexture");
    public static final ItemBodyLocation WEBBING = ItemBodyLocation.registerBase("Webbing");
    public static final ItemBodyLocation WOUND = ItemBodyLocation.registerBase("Wound");
    public static final ItemBodyLocation ZED_DMG = ItemBodyLocation.registerBase("ZedDmg");
    private final String translationName;

    private ItemBodyLocation(String translationName) {
        this.translationName = translationName;
    }

    public static ItemBodyLocation get(ResourceLocation id) {
        return Registries.ITEM_BODY_LOCATION.get(id);
    }

    public String toString() {
        return Registries.ITEM_BODY_LOCATION.getLocation(this).toString();
    }

    public String getTranslationName() {
        return this.translationName;
    }

    public static ItemBodyLocation register(String id) {
        return ItemBodyLocation.register(false, id);
    }

    private static ItemBodyLocation registerBase(String id) {
        return ItemBodyLocation.register(true, id);
    }

    private static ItemBodyLocation register(boolean allowDefaultNamespace, String id) {
        return Registries.ITEM_BODY_LOCATION.register(RegistryReset.createLocation(id, allowDefaultNamespace), new ItemBodyLocation(id));
    }
}

