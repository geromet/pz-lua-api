/*
 * Decompiled with CFR 0.152.
 */
package zombie.characterTextures;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.jspecify.annotations.Nullable;
import zombie.SandboxOptions;
import zombie.UsedFromLua;
import zombie.characterTextures.BloodBodyPartType;
import zombie.core.random.Rand;
import zombie.core.skinnedmodel.population.OutfitRNG;
import zombie.core.skinnedmodel.visual.HumanVisual;
import zombie.core.skinnedmodel.visual.ItemVisual;
import zombie.inventory.types.Clothing;
import zombie.scripting.objects.Item;
import zombie.util.Type;
import zombie.util.list.PZArrayUtil;

@UsedFromLua
public enum BloodClothingType {
    Apron(BloodBodyPartType.Torso_Upper, BloodBodyPartType.Torso_Lower, BloodBodyPartType.UpperLeg_L, BloodBodyPartType.UpperLeg_R),
    ShirtNoSleeves(BloodBodyPartType.Torso_Upper, BloodBodyPartType.Torso_Lower, BloodBodyPartType.Back),
    JumperNoSleeves(ShirtNoSleeves, new BloodBodyPartType[0]),
    Shirt(ShirtNoSleeves, BloodBodyPartType.UpperArm_L, BloodBodyPartType.UpperArm_R),
    ShirtLongSleeves(Shirt, BloodBodyPartType.ForeArm_L, BloodBodyPartType.ForeArm_R),
    Jumper(ShirtLongSleeves, new BloodBodyPartType[0]),
    Jacket(ShirtLongSleeves, BloodBodyPartType.Neck),
    LongJacket(ShirtLongSleeves, BloodBodyPartType.Neck, BloodBodyPartType.Groin, BloodBodyPartType.UpperLeg_L, BloodBodyPartType.UpperLeg_R),
    ShortsShort(BloodBodyPartType.Groin, BloodBodyPartType.UpperLeg_L, BloodBodyPartType.UpperLeg_R),
    Trousers(ShortsShort, BloodBodyPartType.LowerLeg_L, BloodBodyPartType.LowerLeg_R),
    Shoes(BloodBodyPartType.Foot_L, BloodBodyPartType.Foot_R),
    FullHelmet(BloodBodyPartType.Head),
    Bag(BloodBodyPartType.Back),
    Hands(BloodBodyPartType.Hand_L, BloodBodyPartType.Hand_R),
    Head(BloodBodyPartType.Head),
    Neck(BloodBodyPartType.Neck),
    Groin(BloodBodyPartType.Groin),
    UpperBody(BloodBodyPartType.Torso_Upper),
    LowerBody(BloodBodyPartType.Torso_Lower),
    LowerLegs(BloodBodyPartType.LowerLeg_L, BloodBodyPartType.LowerLeg_R),
    UpperLegs(BloodBodyPartType.UpperLeg_L, BloodBodyPartType.UpperLeg_R),
    LowerArms(BloodBodyPartType.ForeArm_L, BloodBodyPartType.ForeArm_R),
    UpperArms(BloodBodyPartType.UpperArm_L, BloodBodyPartType.UpperArm_R),
    Hand_L(BloodBodyPartType.Hand_L),
    Hand_R(BloodBodyPartType.Hand_R),
    ForeArm_L(BloodBodyPartType.ForeArm_L),
    ForeArm_R(BloodBodyPartType.ForeArm_R),
    UpperArm_L(BloodBodyPartType.UpperArm_L),
    UpperArm_R(BloodBodyPartType.UpperArm_R),
    UpperLeg_L(BloodBodyPartType.UpperLeg_L),
    UpperLeg_R(BloodBodyPartType.UpperLeg_R),
    LowerLeg_L(BloodBodyPartType.LowerLeg_L),
    LowerLeg_R(BloodBodyPartType.LowerLeg_R),
    Foot_L(BloodBodyPartType.Foot_L),
    Foot_R(BloodBodyPartType.Foot_R);

    private static final BloodClothingType[] VALUES;
    private static final Map<String, BloodClothingType> BY_NAME;
    private final List<BloodBodyPartType> coveredParts = new ArrayList<BloodBodyPartType>();
    private static final ArrayList<BloodBodyPartType> bodyParts;

    private BloodClothingType(BloodBodyPartType ... coveredParts) {
        this((BloodClothingType)null, coveredParts);
    }

    private BloodClothingType(BloodClothingType bloodClothingType, BloodBodyPartType ... bloodBodyPartTypes) {
        if (bloodClothingType != null) {
            this.coveredParts.addAll(bloodClothingType.coveredParts);
        }
        this.coveredParts.addAll(Arrays.asList(bloodBodyPartTypes));
    }

    public static @Nullable BloodClothingType fromString(String str) {
        return BY_NAME.get(str);
    }

    public static ArrayList<BloodBodyPartType> getCoveredParts(@Nullable ArrayList<BloodClothingType> bloodClothingType) {
        return BloodClothingType.getCoveredParts(bloodClothingType, new ArrayList<BloodBodyPartType>());
    }

    public static ArrayList<BloodBodyPartType> getCoveredParts(@Nullable ArrayList<BloodClothingType> bloodClothingType, ArrayList<BloodBodyPartType> result) {
        if (bloodClothingType != null) {
            for (int j = 0; j < bloodClothingType.size(); ++j) {
                BloodClothingType testBlood = bloodClothingType.get(j);
                PZArrayUtil.addAll(result, testBlood.coveredParts);
            }
        }
        return result;
    }

    public static int getCoveredPartCount(@Nullable ArrayList<BloodClothingType> bloodClothingType) {
        int count = 0;
        if (bloodClothingType != null) {
            for (int i = 0; i < bloodClothingType.size(); ++i) {
                BloodClothingType testBlood = bloodClothingType.get(i);
                count += testBlood.coveredParts.size();
            }
        }
        return count;
    }

    public static void addBlood(int count, HumanVisual humanVisual, ArrayList<ItemVisual> itemVisuals, boolean allLayers) {
        for (int i = 0; i < count; ++i) {
            BloodBodyPartType part = BloodBodyPartType.FromIndex(Rand.Next(0, BloodBodyPartType.MAX.index()));
            BloodClothingType.addBlood(part, humanVisual, itemVisuals, allLayers);
        }
    }

    public static void addBlood(BloodBodyPartType part, HumanVisual humanVisual, ArrayList<ItemVisual> itemVisuals, boolean allLayers) {
        float intensity = switch (SandboxOptions.instance.clothingDegradation.getValue()) {
            case 2 -> OutfitRNG.Next(0.001f, 0.01f);
            case 3 -> OutfitRNG.Next(0.05f, 0.1f);
            case 4 -> OutfitRNG.Next(0.01f, 0.05f);
            default -> 0.0f;
        };
        BloodClothingType.addBlood(part, intensity, humanVisual, itemVisuals, allLayers);
    }

    public static void addDirt(BloodBodyPartType part, HumanVisual humanVisual, ArrayList<ItemVisual> itemVisuals, boolean allLayers) {
        float intensity = switch (SandboxOptions.instance.clothingDegradation.getValue()) {
            case 2 -> OutfitRNG.Next(0.001f, 0.01f);
            case 3 -> OutfitRNG.Next(0.05f, 0.1f);
            case 4 -> OutfitRNG.Next(0.01f, 0.05f);
            default -> 0.0f;
        };
        BloodClothingType.addDirt(part, intensity, humanVisual, itemVisuals, allLayers);
    }

    public static void addHole(BloodBodyPartType part, HumanVisual humanVisual, ArrayList<ItemVisual> itemVisuals) {
        BloodClothingType.addHole(part, humanVisual, itemVisuals, false);
    }

    public static boolean addHole(BloodBodyPartType part, HumanVisual humanVisual, ArrayList<ItemVisual> itemVisuals, boolean allLayers) {
        ItemVisual itemHit = null;
        boolean addedHole = false;
        for (int i = itemVisuals.size() - 1; i >= 0; --i) {
            ArrayList<BloodClothingType> types;
            ItemVisual itemVisual = itemVisuals.get(i);
            Item scriptItem = itemVisual.getScriptItem();
            if (scriptItem == null || itemVisual.getInventoryItem() != null && itemVisual.getInventoryItem().isBroken() || (types = scriptItem.getBloodClothingType()) == null) continue;
            for (int j = 0; j < types.size(); ++j) {
                BloodClothingType bloodClothingType = scriptItem.getBloodClothingType().get(j);
                if (!bloodClothingType.coveredParts.contains((Object)part) || itemVisual.getHole(part) != 0.0f) continue;
                itemHit = itemVisual;
                break;
            }
            if (itemHit == null) continue;
            Clothing clothing = Type.tryCastTo(itemHit.getInventoryItem(), Clothing.class);
            if (clothing != null && scriptItem.canHaveHoles.booleanValue()) {
                itemHit.setHole(part);
                clothing.removePatch(part);
                clothing.setCondition((int)((float)clothing.getCondition() - clothing.getCondLossPerHole()));
                addedHole = true;
            } else if (clothing != null && !scriptItem.canHaveHoles.booleanValue() && Rand.NextBool(clothing.getConditionLowerChance())) {
                clothing.setCondition(clothing.getCondition() - 1);
            } else if (clothing == null && scriptItem.canHaveHoles.booleanValue()) {
                itemHit.setHole(part);
                addedHole = true;
            }
            if (!allLayers) break;
            itemHit = null;
        }
        if (itemHit == null || allLayers) {
            humanVisual.setHole(part);
        }
        return addedHole;
    }

    public static void addBasicPatch(BloodBodyPartType part, HumanVisual humanVisual, ArrayList<ItemVisual> itemVisuals) {
        ItemVisual itemHit = null;
        for (int i = itemVisuals.size() - 1; i >= 0; --i) {
            ArrayList<BloodClothingType> types;
            ItemVisual itemVisual = itemVisuals.get(i);
            Item scriptItem = itemVisual.getScriptItem();
            if (scriptItem == null || (types = scriptItem.getBloodClothingType()) == null) continue;
            for (int j = 0; j < types.size(); ++j) {
                BloodClothingType bloodClothingType = types.get(j);
                if (!bloodClothingType.coveredParts.contains((Object)part) || itemVisual.getBasicPatch(part) != 0.0f) continue;
                itemHit = itemVisual;
                break;
            }
            if (itemHit != null) break;
        }
        if (itemHit != null) {
            itemHit.removeHole(BloodBodyPartType.ToIndex(part));
            itemHit.setBasicPatch(part);
        }
    }

    public static void addDirt(BloodBodyPartType part, float intensity, HumanVisual humanVisual, ArrayList<ItemVisual> itemVisuals, boolean allLayers) {
        block9: {
            ItemVisual itemHit;
            block7: {
                block8: {
                    itemHit = null;
                    if (allLayers) break block7;
                    for (int i = itemVisuals.size() - 1; i >= 0; --i) {
                        ArrayList<BloodClothingType> types;
                        ItemVisual itemVisual = itemVisuals.get(i);
                        Item scriptItem = itemVisual.getScriptItem();
                        if (scriptItem == null || (types = scriptItem.getBloodClothingType()) == null) continue;
                        for (int j = 0; j < types.size(); ++j) {
                            BloodClothingType bloodClothingType = types.get(j);
                            if (!bloodClothingType.coveredParts.contains((Object)part) || itemVisual.getHole(part) != 0.0f) continue;
                            itemHit = itemVisual;
                            break;
                        }
                        if (itemHit != null) break;
                    }
                    if (itemHit == null) break block8;
                    if (!(intensity > 0.0f)) break block9;
                    itemHit.setDirt(part, itemHit.getDirt(part) + intensity);
                    if (!(itemHit.getInventoryItem() instanceof Clothing)) break block9;
                    BloodClothingType.calcTotalDirtLevel((Clothing)itemHit.getInventoryItem());
                    break block9;
                }
                float current = humanVisual.getDirt(part);
                humanVisual.setDirt(part, current + 0.05f);
                break block9;
            }
            float current = humanVisual.getDirt(part);
            humanVisual.setDirt(part, current + 0.05f);
            float currentDirt = humanVisual.getDirt(part);
            if (Rand.NextBool(Math.abs((int)(currentDirt * 100.0f) - 100))) {
                return;
            }
            for (int i = 0; i < itemVisuals.size(); ++i) {
                ArrayList<BloodClothingType> types;
                itemHit = null;
                ItemVisual itemVisual = itemVisuals.get(i);
                Item scriptItem = itemVisual.getScriptItem();
                if (scriptItem == null || (types = scriptItem.getBloodClothingType()) == null) continue;
                for (int j = 0; j < types.size(); ++j) {
                    BloodClothingType bloodClothingType = types.get(j);
                    if (!bloodClothingType.coveredParts.contains((Object)part) || itemVisual.getHole(part) != 0.0f) continue;
                    itemHit = itemVisual;
                    break;
                }
                if (itemHit == null) continue;
                if (intensity > 0.0f) {
                    itemHit.setDirt(part, itemHit.getDirt(part) + intensity);
                    if (itemHit.getInventoryItem() instanceof Clothing) {
                        BloodClothingType.calcTotalDirtLevel((Clothing)itemHit.getInventoryItem());
                    }
                    currentDirt = itemHit.getDirt(part);
                }
                if (Rand.NextBool(Math.abs((int)(currentDirt * 100.0f) - 100))) break;
            }
        }
    }

    public static void addBlood(BloodBodyPartType part, float intensity, HumanVisual humanVisual, ArrayList<ItemVisual> itemVisuals, boolean allLayers) {
        block9: {
            ItemVisual itemHit;
            block7: {
                block8: {
                    itemHit = null;
                    if (allLayers) break block7;
                    for (int i = itemVisuals.size() - 1; i >= 0; --i) {
                        ArrayList<BloodClothingType> types;
                        ItemVisual itemVisual = itemVisuals.get(i);
                        Item scriptItem = itemVisual.getScriptItem();
                        if (scriptItem == null || (types = scriptItem.getBloodClothingType()) == null) continue;
                        for (int j = 0; j < types.size(); ++j) {
                            BloodClothingType bloodClothingType = types.get(j);
                            if (!bloodClothingType.coveredParts.contains((Object)part) || itemVisual.getHole(part) != 0.0f) continue;
                            itemHit = itemVisual;
                            break;
                        }
                        if (itemHit != null) break;
                    }
                    if (itemHit == null) break block8;
                    if (!(intensity > 0.0f)) break block9;
                    itemHit.setBlood(part, itemHit.getBlood(part) + intensity);
                    if (!(itemHit.getInventoryItem() instanceof Clothing)) break block9;
                    BloodClothingType.calcTotalBloodLevel((Clothing)itemHit.getInventoryItem());
                    break block9;
                }
                float current = humanVisual.getBlood(part);
                humanVisual.setBlood(part, current + 0.05f);
                break block9;
            }
            float current = humanVisual.getBlood(part);
            humanVisual.setBlood(part, current + 0.05f);
            float currentBlood = humanVisual.getBlood(part);
            if (OutfitRNG.NextBool(Math.abs((int)(currentBlood * 100.0f) - 100))) {
                return;
            }
            for (int i = 0; i < itemVisuals.size(); ++i) {
                ArrayList<BloodClothingType> types;
                itemHit = null;
                ItemVisual itemVisual = itemVisuals.get(i);
                Item scriptItem = itemVisual.getScriptItem();
                if (scriptItem == null || (types = scriptItem.getBloodClothingType()) == null) continue;
                for (int j = 0; j < types.size(); ++j) {
                    BloodClothingType bloodClothingType = types.get(j);
                    if (!bloodClothingType.coveredParts.contains((Object)part) || itemVisual.getHole(part) != 0.0f) continue;
                    itemHit = itemVisual;
                    break;
                }
                if (itemHit == null) continue;
                if (intensity > 0.0f) {
                    itemHit.setBlood(part, itemHit.getBlood(part) + intensity);
                    if (itemHit.getInventoryItem() instanceof Clothing) {
                        BloodClothingType.calcTotalBloodLevel((Clothing)itemHit.getInventoryItem());
                    }
                    currentBlood = itemHit.getBlood(part);
                }
                if (OutfitRNG.NextBool(Math.abs((int)(currentBlood * 100.0f) - 100))) break;
            }
        }
    }

    public static synchronized void calcTotalBloodLevel(Clothing clothing) {
        ItemVisual itemVisual = clothing.getVisual();
        if (itemVisual == null) {
            clothing.setBloodLevel(0.0f);
            return;
        }
        ArrayList<BloodClothingType> types = clothing.getBloodClothingType();
        if (types == null) {
            clothing.setBloodLevel(0.0f);
            return;
        }
        bodyParts.clear();
        BloodClothingType.getCoveredParts(types, bodyParts);
        if (bodyParts.isEmpty()) {
            clothing.setBloodLevel(0.0f);
            return;
        }
        float total = 0.0f;
        for (int i = 0; i < bodyParts.size(); ++i) {
            total += itemVisual.getBlood(bodyParts.get(i)) * 100.0f;
        }
        clothing.setBloodLevel(total / (float)bodyParts.size());
    }

    public static synchronized void calcTotalDirtLevel(Clothing clothing) {
        ItemVisual itemVisual = clothing.getVisual();
        if (itemVisual == null) {
            clothing.setDirtiness(0.0f);
            return;
        }
        ArrayList<BloodClothingType> types = clothing.getBloodClothingType();
        if (types == null) {
            clothing.setDirtiness(0.0f);
            return;
        }
        bodyParts.clear();
        BloodClothingType.getCoveredParts(types, bodyParts);
        if (bodyParts.isEmpty()) {
            clothing.setDirtiness(0.0f);
            return;
        }
        float total = 0.0f;
        for (int i = 0; i < bodyParts.size(); ++i) {
            total += itemVisual.getDirt(bodyParts.get(i)) * 100.0f;
        }
        clothing.setDirtiness(total / (float)bodyParts.size());
    }

    static {
        VALUES = BloodClothingType.values();
        BY_NAME = new HashMap<String, BloodClothingType>();
        for (BloodClothingType type : VALUES) {
            BY_NAME.put(type.name(), type);
        }
        bodyParts = new ArrayList();
    }
}

