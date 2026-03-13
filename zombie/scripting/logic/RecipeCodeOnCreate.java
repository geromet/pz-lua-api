/*
 * Decompiled with CFR 0.152.
 */
package zombie.scripting.logic;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import se.krka.kahlua.j2se.KahluaTableImpl;
import se.krka.kahlua.vm.KahluaTable;
import se.krka.kahlua.vm.KahluaTableIterator;
import zombie.GameTime;
import zombie.Lua.LuaManager;
import zombie.UsedFromLua;
import zombie.ZomboidGlobals;
import zombie.characters.BodyDamage.BodyPartType;
import zombie.characters.HaloTextHelper;
import zombie.characters.IsoGameCharacter;
import zombie.characters.IsoPlayer;
import zombie.characters.skills.PerkFactory;
import zombie.core.Color;
import zombie.core.Translator;
import zombie.core.math.PZMath;
import zombie.core.random.Rand;
import zombie.core.skinnedmodel.visual.ItemVisual;
import zombie.core.textures.Texture;
import zombie.entity.GameEntity;
import zombie.entity.components.crafting.InputFlag;
import zombie.entity.components.crafting.recipe.CraftRecipeData;
import zombie.entity.components.fluids.Fluid;
import zombie.entity.components.fluids.FluidSample;
import zombie.entity.components.fluids.FluidType;
import zombie.inventory.InventoryItem;
import zombie.inventory.InventoryItemFactory;
import zombie.inventory.ItemContainer;
import zombie.inventory.types.Clothing;
import zombie.inventory.types.DrainableComboItem;
import zombie.inventory.types.Food;
import zombie.inventory.types.HandWeapon;
import zombie.inventory.types.Key;
import zombie.inventory.types.Literature;
import zombie.inventory.types.Radio;
import zombie.inventory.types.WeaponPart;
import zombie.network.GameServer;
import zombie.network.ServerOptions;
import zombie.radio.devices.DeviceData;
import zombie.scripting.entity.components.crafting.InputScript;
import zombie.scripting.logic.RecipeCodeHelper;
import zombie.scripting.objects.CharacterTrait;
import zombie.scripting.objects.ItemKey;
import zombie.scripting.objects.ItemTag;
import zombie.scripting.objects.ModelKey;
import zombie.scripting.objects.MoodleType;
import zombie.util.StringUtils;

@UsedFromLua
public class RecipeCodeOnCreate
extends RecipeCodeHelper {
    private static final List<ItemKey> mysteryCans = List.of(ItemKey.Food.CANNED_BOLOGNESE_OPEN, ItemKey.Food.CANNED_CARROTS_OPEN, ItemKey.Food.CANNED_CHILI_OPEN, ItemKey.Food.CANNED_CORN_OPEN, ItemKey.Food.CANNED_FRUIT_COCKTAIL_OPEN, ItemKey.Food.CANNED_PEACHES_OPEN, ItemKey.Food.CANNED_PEAS_OPEN, ItemKey.Food.CANNED_PINEAPPLE_OPEN, ItemKey.Food.CANNED_POTATO_OPEN, ItemKey.Food.CANNED_TOMATO_OPEN, ItemKey.Food.DOGFOOD_OPEN, ItemKey.Food.OPEN_BEANS);

    public static void makeCoffee(CraftRecipeData data, IsoGameCharacter character) {
        Object object = data.getAllCreatedItems().getFirst();
        if (object instanceof Food) {
            Food result = (Food)object;
            result.setName(Translator.getText("ContextMenu_FoodType_Coffee") + " " + Translator.getText("ContextMenu_EvolvedRecipe_HotDrink"));
            result.addExtraItem(ItemKey.Food.COFFEE_2);
            result.setCooked(true);
            result.setHeat(2.5f);
            result.setBaseHunger(-0.05f);
            result.setHungChange(-0.05f);
            result.setFatigueChange(-0.25f);
            data.getAllConsumedItems().set(0, result);
        }
    }

    public static void refillBlowTorch(CraftRecipeData data, IsoGameCharacter character) {
        Object object = data.getAllCreatedItems().getFirst();
        if (object instanceof DrainableComboItem) {
            DrainableComboItem newTorch = (DrainableComboItem)object;
            object = data.getAllConsumedItems().getFirst();
            if (object instanceof DrainableComboItem) {
                DrainableComboItem oldTorch = (DrainableComboItem)object;
                object = data.getAllKeepInputItems().getFirst();
                if (object instanceof DrainableComboItem) {
                    DrainableComboItem propaneTank = (DrainableComboItem)object;
                    newTorch.setCurrentUsesFloat(oldTorch.getCurrentUsesFloat());
                    newTorch.setCondition(oldTorch.getCondition());
                    double maxPropaneInTorch = (double)newTorch.getMaxUses() * ZomboidGlobals.refillBlowtorchPropaneAmount;
                    double currentPropaneInTorch = (double)newTorch.getCurrentUsesFloat() * maxPropaneInTorch;
                    double propaneToTransfer = Math.min(maxPropaneInTorch - currentPropaneInTorch, (double)propaneTank.getCurrentUses());
                    if (propaneToTransfer > 0.0) {
                        newTorch.setCurrentUsesFloat((float)((currentPropaneInTorch + propaneToTransfer) / maxPropaneInTorch));
                        propaneTank.setCurrentUses((int)Math.round((double)propaneTank.getCurrentUses() - propaneToTransfer));
                    }
                }
            }
        }
    }

    public static void refillLighter(CraftRecipeData data, IsoGameCharacter character) {
        Object object = data.getAllKeepInputItems().getFirst();
        if (object instanceof DrainableComboItem) {
            DrainableComboItem lighter = (DrainableComboItem)object;
            object = data.getAllConsumedItems().getFirst();
            if (object instanceof DrainableComboItem) {
                DrainableComboItem lighterFluid = (DrainableComboItem)object;
                lighter.setCurrentUsesFloat(lighterFluid.getCurrentUsesFloat() + 0.3f);
            }
        }
    }

    public static void setEcruColor(CraftRecipeData data, IsoGameCharacter character) {
        InventoryItem result = (InventoryItem)data.getAllCreatedItems().getFirst();
        RecipeCodeOnCreate.setColor(result, new Color(0.76f, 0.7f, 0.5f));
    }

    public static void torchBatteryInsert(CraftRecipeData data, IsoGameCharacter character) {
        DrainableComboItem battery = (DrainableComboItem)data.getAllConsumedItems().getFirst();
        Object item = data.getAllKeepInputItems().getFirst();
        if (item instanceof DrainableComboItem) {
            DrainableComboItem flashlight = (DrainableComboItem)item;
            flashlight.setCurrentUsesFrom(battery);
        } else if (item instanceof WeaponPart) {
            WeaponPart flashlight = (WeaponPart)item;
            flashlight.setCurrentUsesFrom(battery);
        } else if (item instanceof Radio) {
            Radio radio = (Radio)item;
            radio.getDeviceData().setPower(battery.getCurrentUsesFloat());
            radio.getDeviceData().setHasBattery(true);
            radio.getDeviceData().transmitBatteryChangeServer();
        }
    }

    public static void dismantleFlashlight(CraftRecipeData data, IsoGameCharacter character) {
        for (InventoryItem item : data.getAllConsumedItems()) {
            DrainableComboItem torch;
            if (!(item instanceof DrainableComboItem) || !(torch = (DrainableComboItem)item).hasTag(ItemTag.FLASHLIGHT) && !torch.hasTag(ItemTag.USES_BATTERY) || !(torch.getCurrentUsesFloat() > 0.0f)) continue;
            DrainableComboItem battery = (DrainableComboItem)((Object)RecipeCodeOnCreate.addItemToCharacterInventory(character, ItemKey.Drainable.BATTERY));
            battery.setCurrentUsesFloat(torch.getCurrentUsesFloat());
            battery.syncItemFields();
        }
    }

    public static void inheritColorFromMaterial(CraftRecipeData data, IsoGameCharacter character) {
        Color color = RecipeCodeOnCreate.findInheritedColor(data);
        if (color == null) {
            return;
        }
        ArrayList<InventoryItem> results = data.getAllCreatedItems();
        if (results.isEmpty()) {
            results = data.getAllInputItemsWithFlag(InputFlag.IsNotWorn);
        }
        for (InventoryItem result : results) {
            RecipeCodeOnCreate.setColor(result, color);
        }
    }

    private static Color findInheritedColor(CraftRecipeData data) {
        InventoryItem item = data.getFirstInputItemWithFlag(InputFlag.InheritColor);
        if (item != null) {
            return item.getColor();
        }
        FluidSample fluid = data.getFirstInputFluidWithFlag(InputFlag.InheritColor);
        return fluid != null ? fluid.getColor() : null;
    }

    public static void shotgunSawnoff(CraftRecipeData data, IsoGameCharacter character) {
        HandWeapon result = (HandWeapon)data.getAllCreatedItems().getFirst();
        List items = RecipeCodeOnCreate.getConsumedItems(data, ItemKey.Weapon.SHOTGUN, ItemKey.Weapon.DOUBLE_BARREL_SHOTGUN);
        Iterator iterator2 = items.iterator();
        if (iterator2.hasNext()) {
            HandWeapon weapon = (HandWeapon)iterator2.next();
            result.copyModData(result.getModData());
            for (WeaponPart part : weapon.getAllWeaponParts()) {
                RecipeCodeOnCreate.tryAttachPart(result, part, character);
            }
            return;
        }
    }

    private static void tryAttachPart(HandWeapon weapon, WeaponPart part, IsoGameCharacter player) {
        if (part.canAttach(null, weapon)) {
            weapon.attachWeaponPart(player, part);
        } else if (player != null) {
            player.getInventory().addItem(part);
        }
    }

    public static void inheritFoodNameBowl(CraftRecipeData data, IsoGameCharacter character) {
        List<Food> foodInputs = RecipeCodeOnCreate.getConsumedItems(data, Food.class);
        for (Food foodInput : foodInputs) {
            List<Food> foodOutputs = RecipeCodeOnCreate.getCreatedItems(data, Food.class);
            for (Food foodOutput : foodOutputs) {
                if (!foodInput.isCustomName() && !StringUtils.isNullOrEmpty(foodInput.getEvolvedRecipeName())) {
                    foodOutput.setName(Translator.getText("Tooltip_food_Bowl", foodInput.getEvolvedRecipeName()));
                } else {
                    String itemName = foodInput.getDisplayName();
                    if (itemName.contains("Pasta") || itemName.contains("Rice")) {
                        itemName = itemName.contains("Pasta") ? "Pasta" : "Rice";
                    }
                    foodOutput.setName(Translator.getText("Tooltip_food_Bowl", itemName));
                }
                foodOutput.setCustomName(true);
            }
        }
    }

    public static void inheritFoodDisplayName(CraftRecipeData data, IsoGameCharacter character) {
        for (InventoryItem item : data.getAllConsumedItems()) {
            Food foodInput;
            if (!(item instanceof Food) || StringUtils.isNullOrEmpty((foodInput = (Food)item).getDisplayName())) continue;
            for (InventoryItem result : data.getAllCreatedItems()) {
                if (!(result instanceof Food)) continue;
                Food foodOutput = (Food)result;
                foodOutput.setName(Translator.getText("Tooltip_food_Slice", foodInput.getDisplayName()));
                foodOutput.setCustomName(true);
            }
        }
    }

    public static void cutFish(CraftRecipeData data, IsoGameCharacter character) {
        Food fish = (Food)RecipeCodeOnCreate.getConsumedItems(data, Food.class).getFirst();
        if (fish != null) {
            float fishWeight = fish.getActualWeight();
            float hunger = Math.max(fish.getBaseHunger(), fish.getHungChange());
            float gutWeightMult = 0.1f;
            float roeWeightMult = 0.0f;
            int roeChance = 10;
            String fishName = fish.getDisplayName();
            int currentMonth = GameTime.getInstance().getMonth();
            if (currentMonth == 3) {
                roeChance = 2;
            } else if (currentMonth == 2 || currentMonth == 4) {
                roeChance = 4;
            }
            if (Rand.NextBool(roeChance)) {
                roeWeightMult = fishName.contains(Translator.getText("IGUI_Fish_Legendary")) ? 0.15f : (fishName.contains(Translator.getText("IGUI_Fish_Big")) ? 0.1f : 0.05f);
            }
            if (!fish.isCooked()) {
                Food guts = (Food)InventoryItemFactory.CreateItem(ItemKey.Food.FISH_GUTS);
                float gutHunger = Math.max(guts.getBaseHunger(), guts.getHungChange());
                guts.setWeight(fishWeight * 0.1f);
                guts.setBaseHunger(gutHunger);
                guts.setHungChange(gutHunger);
                RecipeCodeOnCreate.addItemToCharacterInventory(character, guts);
                if (roeWeightMult > 0.0f) {
                    Food roe = (Food)InventoryItemFactory.CreateItem(ItemKey.Food.FISH_ROE_SAC);
                    float roeHunger = hunger * roeWeightMult;
                    roe.setWeight(fishWeight * roeWeightMult);
                    roe.setBaseHunger(roeHunger);
                    roe.setHungChange(roeHunger);
                    RecipeCodeOnCreate.addItemToCharacterInventory(character, roe);
                }
            }
            for (InventoryItem result : data.getAllCreatedItems()) {
                if (!(result instanceof Food)) continue;
                Food resultFood = (Food)result;
                resultFood.setBaseHunger(hunger / 2.0f);
                resultFood.setHungChange(hunger / 2.0f);
                resultFood.setActualWeight(fishWeight * (0.1f + roeWeightMult) / 2.0f);
                resultFood.setWeight(resultFood.getActualWeight());
                resultFood.setCustomWeight(true);
                resultFood.setCarbohydrates(fish.getCarbohydrates() / 2.0f);
                resultFood.setLipids(fish.getLipids() / 2.0f);
                resultFood.setProteins(fish.getProteins() / 2.0f);
                resultFood.setCalories(fish.getCalories() / 2.0f);
                resultFood.setCooked(fish.isCooked());
            }
        }
    }

    public static void makeJar(CraftRecipeData data, IsoGameCharacter character) {
        RecipeCodeOnCreate.copyFoodValuesFromList(data, RecipeCodeOnCreate.getConsumedItems(data, Food.class));
        RecipeCodeOnCreate.modifyLidCondition(data, character);
        ((InventoryItem)data.getAllCreatedItems().getFirst()).setCustomWeight(true);
        ((InventoryItem)data.getAllCreatedItems().getFirst()).setWeight(0.8f);
    }

    private static void modifyLidCondition(CraftRecipeData data, IsoGameCharacter character) {
        InventoryItem item = (InventoryItem)RecipeCodeOnCreate.getConsumedItems(data, ItemKey.Normal.JAR_LID).getFirst();
        InventoryItem result = (InventoryItem)data.getAllCreatedItems().getFirst();
        KahluaTableImpl mData = (KahluaTableImpl)result.getModData();
        if (Rand.Next(11) >= character.getPerkLevel(PerkFactory.Perks.Cooking)) {
            mData.rawset("LidCondition", (Object)(item.getCondition() - 1));
        } else {
            mData.rawset("LidCondition", (Object)item.getCondition());
        }
        result.syncItemFields();
    }

    public static void applyLidCondition(CraftRecipeData data, IsoGameCharacter character) {
        Object lid = InventoryItemFactory.CreateItem(ItemKey.Normal.JAR_LID);
        KahluaTableImpl mData = (KahluaTableImpl)((InventoryItem)data.getAllConsumedItems().getFirst()).getModData();
        int cond = mData.rawgetInt("LidCondition");
        if (cond == -1) {
            cond = 9;
        }
        ((InventoryItem)lid).setCondition(cond);
        RecipeCodeOnCreate.addItemToCharacterInventory(character, lid);
    }

    public static void makeSushi(CraftRecipeData data, IsoGameCharacter character) {
        List<Food> consumedRiceItems = RecipeCodeHelper.getConsumedItems(data, ItemTag.RICE_RECIPE);
        if (!consumedRiceItems.isEmpty()) {
            float riceNeeded = 0.0f;
            float riceAmount = 0.0f;
            float MINIMUM_HUNGER_VALUE = 0.01f;
            for (InputScript recipeInput : data.getRecipe().getInputs()) {
                if (!recipeInput.isCookedFoodItem() || !recipeInput.getItemTags().contains(ItemTag.RICE_RECIPE)) continue;
                riceNeeded = recipeInput.getAmount() / 100.0f;
                break;
            }
            for (Food riceContainer : consumedRiceItems) {
                riceAmount = Math.abs(riceContainer.getHungChange());
                if (riceAmount - riceNeeded <= 0.01f) {
                    String riceContainerReturnType = riceContainer.getReplaceOnUseFullType();
                    if (riceContainerReturnType != null) {
                        Object emptyRiceContainer = InventoryItemFactory.CreateItem(riceContainerReturnType);
                        ((InventoryItem)emptyRiceContainer).setCondition(riceContainer.getCondition());
                        RecipeCodeOnCreate.addItemToCharacterInventory(character, emptyRiceContainer);
                    }
                    riceNeeded -= riceAmount;
                } else {
                    riceNeeded = 0.0f;
                }
                if (!(riceNeeded <= 0.01f)) continue;
                break;
            }
        }
    }

    public static void name_muffins(CraftRecipeData data, IsoGameCharacter character) {
        String muffinName = ((InventoryItem)data.getAllConsumedItems().getFirst()).getDisplayName();
        if (((InventoryItem)data.getAllConsumedItems().getFirst()).haveExtraItems()) {
            for (InventoryItem inventoryItem : RecipeCodeOnCreate.getCreatedItems(data, Food.class)) {
                inventoryItem.setName(muffinName);
            }
        }
    }

    public static void cutSmallAnimal(CraftRecipeData data, IsoGameCharacter character) {
        List<Food> animals = RecipeCodeOnCreate.getConsumedItems(data, Food.class);
        if (!animals.isEmpty()) {
            Food animal = (Food)animals.getFirst();
            for (InventoryItem result : data.getAllCreatedItems()) {
                if (!(result instanceof Food)) continue;
                Food resultFood = (Food)result;
                resultFood.setBaseHunger(animal.getHungChange());
                resultFood.setHungChange(animal.getHungChange());
                resultFood.setCustomWeight(true);
                resultFood.setWeight(animal.getWeight() * 0.7f);
                resultFood.setActualWeight(animal.getActualWeight() * 0.7f);
                resultFood.setLipids(animal.getLipids() * 0.75f);
                resultFood.setProteins(animal.getProteins() * 0.75f);
                resultFood.setCalories(animal.getCalories() * 0.75f);
                resultFood.setCarbohydrates(animal.getCarbohydrates() * 0.75f);
                resultFood.setUnhappyChange(animal.getUnhappyChange() * 0.75f);
            }
        }
    }

    public static void createLogStack(CraftRecipeData data, IsoGameCharacter character) {
        List<InventoryItem> ropeItems = RecipeCodeOnCreate.getConsumedItems(data, ItemTag.ROPE);
        KahluaTableImpl table = (KahluaTableImpl)LuaManager.platform.newTable();
        for (InventoryItem ropeItem : ropeItems) {
            table.rawset(table.delegate.size(), (Object)ropeItem.getFullType());
        }
        ((InventoryItem)data.getAllCreatedItems().getFirst()).getModData().rawset("ropeItems", (Object)table);
    }

    public static void splitLogStack(CraftRecipeData data, IsoGameCharacter character) {
        KahluaTableImpl ropeItems = (KahluaTableImpl)((InventoryItem)data.getAllConsumedItems().getFirst()).getModData().rawget("ropeItems");
        KahluaTableIterator it = ropeItems.iterator();
        while (it.advance()) {
            Object rope = InventoryItemFactory.CreateItem((String)it.getValue());
            RecipeCodeOnCreate.addItemToCharacterInventory(character, rope);
        }
    }

    public static void dismantleMiscElectronics(CraftRecipeData data, IsoGameCharacter character) {
        for (InventoryItem item : data.getAllConsumedItems()) {
            DrainableComboItem battery;
            Radio radio;
            if (item instanceof Radio && (radio = (Radio)item).getDeviceData().getIsBatteryPowered() && radio.getCurrentUsesFloat() > 0.0f) {
                battery = (DrainableComboItem)((Object)RecipeCodeOnCreate.addItemToCharacterInventory(character, ItemKey.Drainable.BATTERY));
                battery.setCurrentUsesFloat(radio.getCurrentUsesFloat());
                battery.syncItemFields();
                continue;
            }
            if (item.hasTag(ItemTag.USES_BATTERY) && item.getCurrentUsesFloat() > 0.0f) {
                battery = (DrainableComboItem)((Object)RecipeCodeOnCreate.addItemToCharacterInventory(character, ItemKey.Drainable.BATTERY));
                battery.setCurrentUsesFloat(item.getCurrentUsesFloat());
                battery.syncItemFields();
                continue;
            }
            if (!item.is(ItemKey.Normal.REMOTE) || Rand.Next(100) >= 60) continue;
            battery = (DrainableComboItem)((Object)RecipeCodeOnCreate.addItemToCharacterInventory(character, ItemKey.Drainable.BATTERY));
            battery.setCurrentUsesFloat(Rand.Next(0.01f, 1.0f));
            battery.syncItemFields();
        }
    }

    public static void fixFishingRope(CraftRecipeData data, IsoGameCharacter character) {
        InventoryItem result = (InventoryItem)data.getAllCreatedItems().getFirst();
        for (InventoryItem item : data.getAllConsumedItems()) {
            if (item.hasTag(ItemTag.FISHING_LINE)) {
                result.getModData().rawset("fishing_LineType", (Object)item.getFullType());
            }
            if (!item.hasTag(ItemTag.FISHING_HOOK)) continue;
            result.getModData().rawset("fishing_HookType", (Object)item.getFullType());
        }
    }

    public static void makeOmelette(CraftRecipeData data, IsoGameCharacter character) {
        RecipeCodeOnCreate.copyFoodValuesFromList(data, RecipeCodeOnCreate.getConsumedItems(data, ItemTag.EGG));
    }

    private static void copyFoodValuesFromList(CraftRecipeData data, List<Food> foodList) {
        float hunger = 0.0f;
        float calories = 0.0f;
        float carbs = 0.0f;
        float protein = 0.0f;
        float lipids = 0.0f;
        for (Food food : foodList) {
            if (food.isSpice() && !food.is(ItemKey.Food.FISH_ROE)) continue;
            hunger += food.getBaseHunger();
            calories += food.getCalories();
            carbs += food.getCarbohydrates();
            protein += food.getProteins();
            lipids += food.getLipids();
        }
        Food result = (Food)data.getAllCreatedItems().getFirst();
        result.setBaseHunger(hunger);
        result.setHungChange(hunger);
        result.setCalories(calories);
        result.setCarbohydrates(carbs);
        result.setProteins(protein);
        result.setLipids(lipids);
    }

    public static void dismantleElectronics(CraftRecipeData data, IsoGameCharacter character) {
        for (InventoryItem electronic : data.getAllConsumedItems()) {
            if (electronic.is(ItemKey.Radio.RADIO_RED, ItemKey.Radio.RADIO_BLACK)) {
                RecipeCodeOnCreate.dismantleRadio(data, character);
            }
            if (electronic.is(ItemKey.Radio.WALKIE_TALKIE_1, ItemKey.Radio.WALKIE_TALKIE_2, ItemKey.Radio.WALKIE_TALKIE_3, ItemKey.Radio.WALKIE_TALKIE_4, ItemKey.Radio.WALKIE_TALKIE_5, ItemKey.Radio.MAN_PACK_RADIO, ItemKey.Radio.HAM_RADIO_1, ItemKey.Radio.HAM_RADIO_2)) {
                RecipeCodeOnCreate.dismantleRadioTwoWay(data, character);
            }
            if (!electronic.is(ItemKey.Radio.TV_ANTIQUE, ItemKey.Radio.TV_WIDE_SCREEN, ItemKey.Radio.TV_BLACK)) continue;
            RecipeCodeOnCreate.dismantleRadioTV(data, character);
        }
    }

    private static void dismantleRadioTwoWay(CraftRecipeData data, IsoGameCharacter character) {
        int success = 50 + character.getPerkLevel(PerkFactory.Perks.Electricity) * 5;
        if (Rand.Next(100) < success) {
            RecipeCodeOnCreate.addItemToCharacterInventory(character, ItemKey.Normal.RADIO_TRANSMITTER);
        }
        if (Rand.Next(100) < success) {
            RecipeCodeOnCreate.addItemToCharacterInventory(character, ItemKey.Normal.LIGHT_BULB_GREEN);
        }
        RecipeCodeOnCreate.dismantleRadio(data, character);
    }

    private static void dismantleRadio(CraftRecipeData data, IsoGameCharacter character) {
        int success = 50 + character.getPerkLevel(PerkFactory.Perks.Electricity) * 5;
        RecipeCodeOnCreate.getRadioBaseItems(character);
        if (Rand.Next(100) < success) {
            RecipeCodeOnCreate.addItemToCharacterInventory(character, ItemKey.Normal.AMPLIFIER);
        }
        if (Rand.Next(100) < success) {
            RecipeCodeOnCreate.addItemToCharacterInventory(character, ItemKey.Normal.LIGHT_BULB);
        }
        if (Rand.Next(100) < success) {
            RecipeCodeOnCreate.addItemToCharacterInventory(character, ItemKey.Normal.RADIO_RECEIVER);
        }
        List<Radio> items = RecipeCodeOnCreate.getConsumedItems(data, Radio.class);
        for (Radio item : items) {
            item.getDeviceData().getBattery(character.getInventory());
            item.getDeviceData().getHeadphones(character.getInventory());
        }
    }

    private static void getRadioBaseItems(IsoGameCharacter character) {
        int rand = Rand.Next(3);
        block4: for (int i = 0; i < rand; ++i) {
            int randItem = Rand.Next(3);
            switch (randItem) {
                case 0: {
                    RecipeCodeOnCreate.addItemToCharacterInventory(character, ItemKey.Normal.ELECTRONICS_SCRAP);
                    continue block4;
                }
                case 1: {
                    RecipeCodeOnCreate.addItemToCharacterInventory(character, ItemKey.Normal.ELECTRIC_WIRE);
                    continue block4;
                }
                default: {
                    RecipeCodeOnCreate.addItemToCharacterInventory(character, ItemKey.Normal.ALUMINUM_FRAGMENTS);
                }
            }
        }
    }

    private static void dismantleRadioTV(CraftRecipeData data, IsoGameCharacter character) {
        int success = 50 + character.getPerkLevel(PerkFactory.Perks.Electricity) * 5;
        RecipeCodeOnCreate.getRadioBaseItems(character);
        if (Rand.Next(100) < success) {
            RecipeCodeOnCreate.addItemToCharacterInventory(character, ItemKey.Normal.AMPLIFIER);
        }
        if (Rand.Next(100) < success) {
            RecipeCodeOnCreate.addItemToCharacterInventory(character, ItemKey.Normal.LIGHT_BULB);
        }
        if (!((InventoryItem)data.getAllConsumedItems().getFirst()).is(ItemKey.Radio.TV_ANTIQUE)) {
            if (Rand.Next(100) < success) {
                RecipeCodeOnCreate.addItemToCharacterInventory(character, ItemKey.Normal.LIGHT_BULB_RED);
            }
            if (Rand.Next(100) < success) {
                RecipeCodeOnCreate.addItemToCharacterInventory(character, ItemKey.Normal.LIGHT_BULB_GREEN);
            }
        }
    }

    private static float getRandomRadioValue(float valMin, float valMax, int perkLevel) {
        float range = valMax - valMin;
        return Rand.Next(range * ((float)(perkLevel - 1) / 10.0f), range * ((float)perkLevel / 10.0f)) + valMin;
    }

    public static void radioCraft(CraftRecipeData data, IsoGameCharacter character) {
        InventoryItem created = (InventoryItem)data.getAllCreatedItems().getFirst();
        if (created instanceof Radio) {
            Radio result = (Radio)created;
            DeviceData deviceData = result.getDeviceData();
            int perk = character.getPerkLevel(PerkFactory.Perks.Electricity);
            int perkInvert = 10 - perk + 1;
            float actualWeight = result.getScriptItem().getActualWeight();
            if (actualWeight <= 3.0f) {
                result.setActualWeight(RecipeCodeOnCreate.getRandomRadioValue(1.5f, 3.0f, perk));
            } else {
                result.setActualWeight(actualWeight);
            }
            result.setWeight(result.getActualWeight());
            result.setCustomWeight(true);
            deviceData.setUseDelta(RecipeCodeOnCreate.getRandomRadioValue(0.007f, 0.3f, perkInvert));
            deviceData.setBaseVolumeRange(RecipeCodeOnCreate.getRandomRadioValue(8.0f, 16.0f, perk));
            deviceData.setMinChannelRange(PZMath.fastfloor(RecipeCodeOnCreate.getRandomRadioValue(200.0f, 88000.0f, perkInvert)));
            deviceData.setMaxChannelRange(PZMath.fastfloor(RecipeCodeOnCreate.getRandomRadioValue(108000.0f, 1000000.0f, perk)));
            deviceData.setTransmitRange(PZMath.fastfloor(RecipeCodeOnCreate.getRandomRadioValue(500.0f, 5000.0f, perk)));
            deviceData.setHasBattery(false);
            deviceData.setPower(0.0f);
            deviceData.transmitBatteryChange();
            if (perk == 10 && Rand.Next(100) < 25) {
                deviceData.setIsHighTier(true);
                deviceData.setTransmitRange(PZMath.fastfloor(RecipeCodeOnCreate.getRandomRadioValue(5500.0f, 7500.0f, perk)));
                deviceData.setUseDelta(RecipeCodeOnCreate.getRandomRadioValue(0.002f, 0.007f, perk));
            }
        }
    }

    public static void ripClothing(CraftRecipeData data, IsoGameCharacter character) {
        Clothing clothing = (Clothing)RecipeCodeOnCreate.getConsumedItems(data, Clothing.class).getFirst();
        if (clothing == null || StringUtils.isNullOrEmpty(clothing.getFabricType())) {
            return;
        }
        KahluaTableImpl def = (KahluaTableImpl)LuaManager.env.rawget("ClothingRecipesDefinitions");
        String material = def.rawgetTable("FabricType").rawgetTable(clothing.getFabricType()).rawgetStr("material");
        String materialDirty = def.rawgetTable("FabricType").rawgetTable(clothing.getFabricType()).rawgetStr("materialDirty");
        int maxMaterial = Math.max(clothing.getNbrOfCoveredParts() - (clothing.getHolesNumber() + clothing.getPatchesNumber()), 1);
        int minMaterial = maxMaterial == 1 ? 1 : 2;
        int nbr = Rand.NextInclusive(minMaterial, maxMaterial);
        nbr = PZMath.clamp(nbr + character.getPerkLevel(PerkFactory.Perks.Tailoring) / 2, 1, maxMaterial);
        for (int i = 0; i < nbr; ++i) {
            boolean dirty = (float)(Rand.Next(99) + 1) <= clothing.getDirtiness() + clothing.getBloodLevel();
            RecipeCodeOnCreate.addItemToCharacterInventory(character, InventoryItemFactory.CreateItem(!dirty ? material : materialDirty));
        }
        if (clothing.hasTag(ItemTag.BUCKLE)) {
            RecipeCodeOnCreate.addItemToCharacterInventory(character, ItemKey.Normal.BUCKLE);
        }
    }

    public static void makeMilkFromPowder(CraftRecipeData data, IsoGameCharacter character) {
        InventoryItem bucket = (InventoryItem)data.getAllKeepInputItems().getFirst();
        bucket.getFluidContainer().addFluid(FluidType.AnimalMilk, bucket.getFluidContainer().getCapacity());
        bucket.sendSyncEntity(null);
    }

    public static void purifyWater(CraftRecipeData data, IsoGameCharacter character) {
        InventoryItem bucket = (InventoryItem)data.getAllKeepInputItems().getFirst();
        float taintedAmount = bucket.getFluidContainer().getSpecificFluidAmount(Fluid.TaintedWater);
        float amount = Math.min(1.0f, taintedAmount);
        bucket.getFluidContainer().adjustSpecificFluidAmount(Fluid.TaintedWater, taintedAmount - amount);
        bucket.getFluidContainer().addFluid(Fluid.Water, amount);
        bucket.sendSyncEntity(null);
    }

    public static void carveSpear(CraftRecipeData data, IsoGameCharacter character) {
        InventoryItem createdSpear = (InventoryItem)data.getAllCreatedItems().getFirst();
        RecipeCodeOnCreate.setRandomSpearCondition(character, createdSpear, 2);
    }

    public static void fireHardenSpear(CraftRecipeData data, IsoGameCharacter character) {
        InventoryItem result = (InventoryItem)data.getAllCreatedItems().getFirst();
        result.copyConditionStatesFrom((InventoryItem)data.getAllConsumedItems().getFirst());
        RecipeCodeOnCreate.setRandomSpearCondition(character, result, result.getCondition());
    }

    private static void setRandomSpearCondition(IsoGameCharacter character, InventoryItem result, int defaultCond) {
        int conditionMax = defaultCond + character.getPerkLevel(PerkFactory.Perks.Carving);
        conditionMax = Rand.Next(conditionMax, conditionMax + 2);
        result.setCondition(PZMath.clamp(conditionMax, 2, result.getConditionMax()));
    }

    public static void dismantleSpear(CraftRecipeData data, IsoGameCharacter character) {
        InventoryItem spear = (InventoryItem)data.getAllConsumedItems().getFirst();
        for (InventoryItem result : data.getAllCreatedItems()) {
            if (result.is(ItemKey.Weapon.LONG_STICK)) {
                result.setConditionFrom(spear);
                continue;
            }
            if (result.is(ItemKey.Weapon.LONG_STICK_BROKEN)) continue;
            result.setConditionFromHeadCondition(spear);
        }
    }

    public static void openCan(CraftRecipeData data, IsoGameCharacter character) {
        if (character == null) {
            return;
        }
        InventoryItem opener = null;
        boolean canOpener = false;
        boolean chippedStone = false;
        for (InventoryItem item : data.getAllConsumedItems()) {
            HandWeapon handWeapon;
            if (item.is(ItemKey.Normal.SHARPED_STONE)) {
                chippedStone = true;
            }
            if (item.hasTag(ItemTag.CAN_OPENER)) {
                canOpener = true;
            }
            if (item instanceof HandWeapon || item.hasTag(ItemTag.SHARP_KNIFE) || item.is(ItemKey.Normal.SHARPED_STONE)) {
                opener = item;
                opener.checkSyncItemFields(opener.damageCheck());
            }
            if (canOpener) continue;
            int woundChance = 3;
            if (chippedStone) {
                ++woundChance;
            }
            if (character.hasTrait(CharacterTrait.DEXTROUS)) {
                woundChance -= 2;
            }
            if (character.hasTrait(CharacterTrait.CLUMSY)) {
                woundChance += 2;
            }
            if (character.getPerkLevel(PerkFactory.Perks.SmallBlade) > 5) {
                woundChance -= 2;
            } else if (character.getPerkLevel(PerkFactory.Perks.SmallBlade) > 3) {
                --woundChance;
            } else if (character.getPerkLevel(PerkFactory.Perks.Cooking) < 1) {
                ++woundChance;
            }
            int roll = 20;
            if (woundChance < 1) {
                woundChance = 1;
                roll = 30;
            }
            if (Rand.Next(roll) > woundChance) continue;
            HandWeapon weapon = opener instanceof HandWeapon ? (handWeapon = (HandWeapon)opener) : (HandWeapon)InventoryItemFactory.CreateItem(ItemKey.Weapon.KITCHEN_KNIFE);
            character.getBodyDamage().DamageFromWeapon(weapon, BodyPartType.ToIndex(BodyPartType.Hand_L));
        }
    }

    public static void openMysteryCan(CraftRecipeData data, IsoGameCharacter character) {
        Food food = (Food)character.getInventory().addItem(Rand.Next(mysteryCans));
        food.setTexture(Texture.getSharedTexture("Item_CannedUnlabeled_Open"));
        food.setWorldStaticModel(ModelKey.TIN_CAN_EMPTY);
        food.setStaticModel(ModelKey.MYSTERY_CAN_OPEN);
        food.getModData().rawset("NoLabel", (Object)true);
        if (GameServer.server) {
            GameServer.sendAddItemToContainer(character.getInventory(), food);
            KahluaTableImpl table = (KahluaTableImpl)LuaManager.platform.newTable();
            table.rawset("itemId", (Object)food.getID());
            GameServer.sendServerCommand((IsoPlayer)character, "recipe", "OpenMysteryCan", table);
        }
    }

    public static void openMysteryCanKnife(CraftRecipeData data, IsoGameCharacter character) {
        RecipeCodeOnCreate.openCan(data, character);
        RecipeCodeOnCreate.openMysteryCan(data, character);
    }

    public static void openWaterCan(CraftRecipeData data, IsoGameCharacter character) {
        Object waterCan = character.getInventory().addItem(ItemKey.Normal.WATER_RATION_CAN_EMPTY);
        ((GameEntity)waterCan).getFluidContainer().addFluid(FluidType.Water, 0.3f);
        if (GameServer.server) {
            GameServer.sendAddItemToContainer(character.getInventory(), waterCan);
        }
    }

    public static void openWaterCanKnife(CraftRecipeData data, IsoGameCharacter character) {
        RecipeCodeOnCreate.openCan(data, character);
        RecipeCodeOnCreate.openWaterCan(data, character);
    }

    public static void openDentedCan(CraftRecipeData data, IsoGameCharacter character) {
        Food result = (Food)character.getInventory().addItem(Rand.Next(mysteryCans));
        result.setTexture(Texture.getSharedTexture("Item_CannedUnlabeled_Gross"));
        ModelKey modelName = ModelKey.DENTED_CAN_OPEN;
        result.getModData().rawset("NoLabel", (Object)true);
        if (Rand.NextBool(10)) {
            result.setAge(result.getOffAgeMax());
            result.setRotten(true);
            modelName = ModelKey.DENTED_CAN_OPEN_GROSS;
        } else if (!Rand.NextBool(10)) {
            result.setAge(Rand.Next(result.getOffAge(), result.getOffAgeMax()));
        }
        if (!result.isFresh() && Rand.NextBool(4)) {
            result.setPoisonPower(Rand.Next(10));
            result.setPoisonDetectionLevel(Rand.Next(5));
        }
        result.setStaticModel(modelName);
        result.setWorldStaticModel(modelName);
        if (GameServer.server) {
            GameServer.sendAddItemToContainer(character.getInventory(), result);
            KahluaTableImpl table = (KahluaTableImpl)LuaManager.platform.newTable();
            table.rawset("itemId", (Object)result.getID());
            table.rawset("modelName", (Object)modelName.toString());
            GameServer.sendServerCommand((IsoPlayer)character, "recipe", "OpenDentedCan", table);
        }
    }

    public static void openDentedCanKnife(CraftRecipeData data, IsoGameCharacter character) {
        RecipeCodeOnCreate.openCan(data, character);
        RecipeCodeOnCreate.openDentedCan(data, character);
    }

    public static void addToPack(CraftRecipeData data, IsoGameCharacter character) {
        DrainableComboItem item = (DrainableComboItem)RecipeCodeOnCreate.getKeepItems(data, DrainableComboItem.class).getFirst();
        item.setCurrentUses(item.getCurrentUses() + 1);
    }

    public static void drawRandomCard(CraftRecipeData data, IsoGameCharacter character) {
        String card = Translator.getText(ServerOptions.getRandomCard());
        HaloTextHelper.addGoodText((IsoPlayer)character, card);
        if (GameServer.server) {
            KahluaTableImpl table = (KahluaTableImpl)LuaManager.platform.newTable();
            table.rawset("onlineID", (Object)character.getOnlineID());
            table.rawset("type", (Object)4.0);
            table.rawset("text", (Object)card);
            GameServer.sendServerCommand((IsoPlayer)character, "recipe", "SayText", table);
        }
    }

    public static void rollDice(CraftRecipeData data, IsoGameCharacter character) {
        InventoryItem dice = (InventoryItem)data.getAllKeepInputItems().getFirst();
        String diceName = Translator.getText("IGUI_RollDice", dice.getDisplayName());
        int roll = 0;
        if (dice.hasTag(ItemTag.D4)) {
            roll = Rand.NextInclusive(1, 4);
        } else if (dice.hasTag(ItemTag.D6)) {
            roll = Rand.NextInclusive(1, 6);
        } else if (dice.hasTag(ItemTag.D8)) {
            roll = Rand.NextInclusive(1, 8);
        } else if (dice.hasTag(ItemTag.D10)) {
            roll = Rand.NextInclusive(1, 10);
        } else if (dice.hasTag(ItemTag.D12)) {
            roll = Rand.NextInclusive(1, 12);
        } else if (dice.hasTag(ItemTag.D20)) {
            roll = Rand.NextInclusive(1, 20);
        } else if (dice.hasTag(ItemTag.D00)) {
            roll = Rand.NextInclusive(1, 100);
        }
        HaloTextHelper.addGoodText((IsoPlayer)character, String.valueOf(roll));
        if (GameServer.server) {
            KahluaTableImpl table = (KahluaTableImpl)LuaManager.platform.newTable();
            table.rawset("onlineID", (Object)character.getOnlineID());
            table.rawset("type", (Object)1.0);
            table.rawset("rollText", (Object)String.valueOf(roll));
            table.rawset("diceNameText", (Object)diceName);
            GameServer.sendServerCommand((IsoPlayer)character, "recipe", "SayText", table);
        }
    }

    public static void dismantleFishingNet(CraftRecipeData data, IsoGameCharacter character) {
        ((InventoryItem)data.getAllCreatedItems().getFirst()).setCurrentUses(Rand.NextInclusive(1, 5));
    }

    public static void refillHurricaneLantern(CraftRecipeData data, IsoGameCharacter character) {
        ArrayList<InventoryItem> items = data.getAllConsumedItems();
        items.addAll(data.getAllKeepInputItems());
        DrainableComboItem result = (DrainableComboItem)RecipeCodeOnCreate.getCreatedItems(data, DrainableComboItem.class).getFirst();
        InventoryItem petrol = (InventoryItem)RecipeCodeOnCreate.getKeepItems(data, Fluid.Petrol).getFirst();
        DrainableComboItem lantern = (DrainableComboItem)RecipeCodeOnCreate.getConsumedItems(data, ItemKey.Drainable.LANTERN_HURRICANE, ItemKey.Drainable.LANTERN_HURRICANE_COPPER, ItemKey.Drainable.LANTERN_HURRICANE_FORGED, ItemKey.Drainable.LANTERN_HURRICANE_GOLD).getFirst();
        int usePer100ml = result.getMaxUses() / 10;
        result.setCurrentUses(lantern.getCurrentUses() + usePer100ml);
        while (result.getCurrentUses() < result.getMaxUses() && petrol.getFluidContainer().getAmount() >= 0.1f) {
            result.setCurrentUses(Math.min(result.getCurrentUses() + usePer100ml, result.getMaxUses()));
            petrol.getFluidContainer().adjustAmount(petrol.getFluidContainer().getAmount() - 0.1f);
        }
    }

    public static void scratchTicket(CraftRecipeData data, IsoGameCharacter character) {
        Literature result = (Literature)RecipeCodeOnCreate.getCreatedItems(data, Literature.class).getFirst();
        result.getModData().rawset("scratched", (Object)true);
        if (Rand.NextBool(5)) {
            RecipeCodeOnCreate.scratchTicketWinner((IsoPlayer)character, result);
        } else {
            result.setName(Translator.getText("IGUI_ScratchingTicketNameLoser", result.getDisplayName()));
            result.setTexture(Texture.getSharedTexture("Item_ScratchTicket_Loser"));
            result.setWorldStaticModel(ModelKey.SCRATCH_TICKET_LOSER);
        }
    }

    public static void removeGasFilter(CraftRecipeData data, IsoGameCharacter character) {
        RecipeCodeOnCreate.removeTankOrFilter(character, (Clothing)RecipeCodeOnCreate.getKeepItems(data, Clothing.class).getFirst(), (DrainableComboItem)InventoryItemFactory.CreateItem(((Clothing)RecipeCodeOnCreate.getKeepItems(data, Clothing.class).getFirst()).getFilterType()), true);
    }

    public static void removeOxygenTank(CraftRecipeData data, IsoGameCharacter character) {
        RecipeCodeOnCreate.removeTankOrFilter(character, (Clothing)RecipeCodeOnCreate.getKeepItems(data, Clothing.class).getFirst(), (DrainableComboItem)InventoryItemFactory.CreateItem(((Clothing)RecipeCodeOnCreate.getKeepItems(data, Clothing.class).getFirst()).getTankType()), false);
    }

    private static void removeTankOrFilter(IsoGameCharacter character, Clothing maskOrSuit, DrainableComboItem tankOrFilter, boolean isFilter) {
        Clothing withoutDrainable;
        tankOrFilter.setCurrentUsesFloat(maskOrSuit.getUsedDelta());
        RecipeCodeOnCreate.addItemToCharacterInventory(character, tankOrFilter);
        maskOrSuit.setCurrentUses(0);
        if (isFilter) {
            maskOrSuit.setNoFilter();
        } else {
            maskOrSuit.setNoTank();
        }
        if (!StringUtils.isNullOrEmpty(maskOrSuit.getWithoutDrainable()) && (withoutDrainable = (Clothing)InventoryItemFactory.CreateItem(maskOrSuit.getWithoutDrainable())) != null) {
            maskOrSuit.setWorldStaticModel(withoutDrainable.getWorldStaticModel());
            maskOrSuit.setStaticModel(withoutDrainable.getStaticModel());
        }
    }

    public static void addGasFilter(CraftRecipeData data, IsoGameCharacter character) {
        RecipeCodeOnCreate.attachTankOrFilter((Clothing)data.getAllKeepInputItems().getFirst(), (InventoryItem)data.getAllDestroyInputItems().getFirst(), true);
    }

    public static void addOxygenTank(CraftRecipeData data, IsoGameCharacter character) {
        RecipeCodeOnCreate.attachTankOrFilter((Clothing)data.getAllKeepInputItems().getFirst(), (InventoryItem)data.getAllDestroyInputItems().getFirst(), false);
    }

    private static void attachTankOrFilter(Clothing maskOrSuit, InventoryItem tankOrFilter, boolean isFilter) {
        if (isFilter) {
            maskOrSuit.setFilterType(tankOrFilter.getFullType());
        } else {
            maskOrSuit.setTankType(tankOrFilter.getFullType());
        }
        maskOrSuit.setUsedDelta(tankOrFilter.getCurrentUsesFloat());
        maskOrSuit.setCurrentUsesFloat(maskOrSuit.getUsedDelta());
        maskOrSuit.setWorldStaticModel(maskOrSuit.getScriptItem().getWorldStaticModel());
        maskOrSuit.setStaticModel(maskOrSuit.getScriptItem().getStaticModel());
    }

    public static void scrapJewellery(CraftRecipeData data, IsoGameCharacter character) {
        ItemContainer inv = character.getInventory();
        for (InventoryItem item : data.getAllConsumedItems()) {
            if (item.hasTag(ItemTag.DIAMOND_JEWELLERY)) {
                inv.addItem(ItemKey.Normal.DIAMOND);
            }
            if (item.hasTag(ItemTag.TWO_DIAMOND_JEWELLERY)) {
                inv.addItems(ItemKey.Normal.DIAMOND, 2);
            }
            if (item.hasTag(ItemTag.EMERALD_JEWELLERY)) {
                inv.addItem(ItemKey.Normal.EMERALD);
            }
            if (item.hasTag(ItemTag.TWO_EMERALD_JEWELLERY)) {
                inv.addItems(ItemKey.Normal.EMERALD, 2);
            }
            if (item.hasTag(ItemTag.RUBY_JEWELLERY)) {
                inv.addItem(ItemKey.Normal.RUBY);
            }
            if (item.hasTag(ItemTag.TWO_RUBY_JEWELLERY)) {
                inv.addItems(ItemKey.Normal.RUBY, 2);
            }
            if (item.hasTag(ItemTag.SAPPHIRE_JEWELLERY)) {
                inv.addItem(ItemKey.Normal.SAPPHIRE);
            }
            if (item.hasTag(ItemTag.TWO_SAPPHIRE_JEWELLERY)) {
                inv.addItems(ItemKey.Normal.SAPPHIRE, 2);
            }
            if (!item.hasTag(ItemTag.AMETHYST_JEWELLERY)) continue;
            inv.addItem(ItemKey.Normal.AMETHYST);
        }
    }

    public static void replaceSawBlade(CraftRecipeData data, IsoGameCharacter character) {
        InventoryItem saw = data.getAllConsumedItems().get(0);
        Object newBlade = InventoryItemFactory.CreateItem(data.getAllConsumedItems().get(1).getFullType());
        ((InventoryItem)newBlade).setCondition(saw.getCondition());
        RecipeCodeOnCreate.addItemToCharacterInventory(character, newBlade);
    }

    public static void minorCondition(CraftRecipeData data, IsoGameCharacter character) {
        PerkFactory.Perk highestSkill = data.getRecipe().getHighestRelevantSkill(character);
        if (highestSkill == null) {
            highestSkill = data.getRecipe().getHighestRelevantSkillFromXpAward(character);
        }
        int skill = character.getPerkLevel(highestSkill);
        for (InventoryItem result : data.getAllCreatedItems()) {
            int condPerc = PZMath.clamp(Rand.Next(5 + skill * 10, 10 + skill * 20), 5, 100);
            int cond = Math.round(Math.max((float)result.getConditionMax() * ((float)condPerc / 100.0f), (float)result.getConditionMax() / 2.0f));
            if (skill >= 10) {
                cond = result.getConditionMax();
            }
            result.setCondition(cond);
        }
    }

    public static void sharpenBlade(CraftRecipeData data, IsoGameCharacter character) {
        InventoryItem item;
        int damageChance = 10;
        InventoryItem whestone = (InventoryItem)RecipeCodeOnCreate.getKeepItems(data, ItemTag.WHETSTONE, ItemTag.FILE).getFirst();
        if (whestone.hasTag(ItemTag.FILE)) {
            damageChance = 20;
        }
        if ((item = data.getFirstInputItemWithFlag(InputFlag.IsSharpenable)) == null) {
            item = data.getFirstInputItemWithFlag(InputFlag.IsDamaged);
        }
        RecipeCodeOnCreate.sharpenBladeGeneric(data, character, item, damageChance, whestone);
    }

    public static void sharpenBladeGrindstone(CraftRecipeData data, IsoGameCharacter character) {
        RecipeCodeOnCreate.sharpenBladeGeneric(data, character, data.getFirstInputItemWithFlag(InputFlag.IsSharpenable), 5, null);
    }

    private static void sharpenBladeGeneric(CraftRecipeData data, IsoGameCharacter character, InventoryItem item, int damageChance, InventoryItem whestone) {
        damageChance -= character.getPerkLevel(PerkFactory.Perks.Maintenance);
        float sharpenMax = item.getMaxSharpness();
        if (item.hasSharpness()) {
            while (!(!(item.getSharpness() < sharpenMax) || item.isBroken() || whestone != null && whestone.isBroken())) {
                item.setSharpness(item.getSharpness() + 0.1f);
                if (Rand.Next(100) <= damageChance) {
                    if (item.hasHeadCondition()) {
                        item.setHeadCondition(item.getHeadCondition() - 1);
                    } else {
                        item.incrementCondition(-1);
                    }
                    sharpenMax = item.getMaxSharpness();
                }
                if (whestone == null || !Rand.NextBool(whestone.getConditionLowerChance())) continue;
                whestone.incrementCondition(-1);
            }
        } else {
            while (!(item.getCondition() >= item.getConditionMax() || whestone != null && whestone.isBroken())) {
                item.incrementCondition(1);
                if (whestone == null || !Rand.NextBool(whestone.getConditionLowerChance())) continue;
                whestone.incrementCondition(-1);
            }
        }
        item.setSharpness(Math.max(item.getSharpness(), sharpenMax));
        item.syncItemFields();
        if (whestone != null) {
            whestone.syncItemFields();
        }
    }

    public static void genericFixing(CraftRecipeData data, IsoGameCharacter character) {
        RecipeCodeOnCreate.genericFixer(data, character, 1, data.getFirstInputItemWithFlag(InputFlag.IsDamaged));
    }

    public static void genericBetterFixing(CraftRecipeData data, IsoGameCharacter character) {
        RecipeCodeOnCreate.genericFixer(data, character, 2, data.getFirstInputItemWithFlag(InputFlag.IsDamaged));
    }

    public static void genericEvenBetterFixing(CraftRecipeData data, IsoGameCharacter character) {
        RecipeCodeOnCreate.genericFixer(data, character, 3, data.getFirstInputItemWithFlag(InputFlag.IsDamaged));
    }

    private static void genericFixer(CraftRecipeData data, IsoGameCharacter character, int factor, InventoryItem item) {
        int skill = character.getPerkLevel(PerkFactory.Perks.Maintenance);
        int timesRepaired = item.getHaveBeenRepaired();
        item.setHaveBeenRepaired(timesRepaired + 1);
        int failChance = 25 - factor * 5;
        failChance = skill > 0 ? (failChance -= skill * 5) : (failChance += 10);
        failChance += timesRepaired * 2;
        failChance = PZMath.clamp(failChance, 0, 95);
        if (Rand.Next(100) <= failChance) {
            item.incrementCondition(-1);
            item.syncItemFields();
            return;
        }
        if (timesRepaired < 1) {
            timesRepaired = 1;
        }
        float percentFixed = ((float)(factor * 10) * (1.0f / (float)timesRepaired) + (float)Math.min(skill * 5, 25)) / 100.0f;
        float amountFixed = (float)(item.getConditionMax() - item.getCondition()) * percentFixed;
        amountFixed = Math.max(1.0f, amountFixed);
        item.incrementCondition((int)amountFixed);
        item.syncItemFields();
    }

    public static void sliceAnimalHead(CraftRecipeData data, IsoGameCharacter character) {
        Food head = (Food)RecipeCodeOnCreate.getConsumedItems(data, ItemTag.ANIMAL_HEAD).getFirst();
        if (head.isRotten()) {
            return;
        }
        RecipeCodeOnCreate.addItemToCharacterInventory(character, ItemKey.Food.ANIMAL_BRAIN);
    }

    public static void cutChicken(CraftRecipeData data, IsoGameCharacter character) {
        Food chicken = (Food)RecipeCodeOnCreate.getConsumedItems(data, Food.class).getFirst();
        float totalHunger = 0.0f;
        float wholeChickenHunger = Math.max(chicken.getBaseHunger(), chicken.getHungChange());
        List<Food> createdItems = RecipeCodeOnCreate.getCreatedItems(data, Food.class);
        for (Food result : createdItems) {
            totalHunger += Math.max(result.getBaseHunger(), result.getHungChange());
        }
        float ratio = wholeChickenHunger / totalHunger;
        for (Food result : createdItems) {
            result.setBaseHunger(result.getBaseHunger() * ratio);
            result.setHungChange(result.getHungChange() * ratio);
            result.setActualWeight(result.getActualWeight() * 0.9f * ratio);
            result.setWeight(result.getActualWeight());
            result.setCustomWeight(true);
            result.setCarbohydrates(result.getCarbohydrates() * ratio);
            result.setLipids(result.getLipids() * ratio);
            result.setProteins(result.getProteins() * ratio);
            result.setCalories(result.getCalories() * ratio);
            result.setCooked(chicken.isCooked());
        }
    }

    public static void placeInBox(CraftRecipeData data, IsoGameCharacter character) {
        InventoryItem box = (InventoryItem)data.getAllCreatedItems().getFirst();
        List<DrainableComboItem> consumedItems = RecipeCodeOnCreate.getConsumedItems(data, DrainableComboItem.class);
        for (int i = 0; i < consumedItems.size(); ++i) {
            DrainableComboItem item = consumedItems.get(i);
            box.getModData().rawset("drainable" + i, (Object)Float.valueOf(item.getCurrentUsesFloat()));
        }
    }

    public static void unpackBox(CraftRecipeData data, IsoGameCharacter character) {
        InventoryItem box = (InventoryItem)data.getAllConsumedItems().getFirst();
        List<DrainableComboItem> createdItems = RecipeCodeOnCreate.getCreatedItems(data, DrainableComboItem.class);
        KahluaTableImpl modData = (KahluaTableImpl)box.getModData();
        for (int i = 0; i < createdItems.size(); ++i) {
            DrainableComboItem item = createdItems.get(i);
            if (modData.rawget("drainable" + i) == null) continue;
            item.setCurrentUsesFloat(modData.rawgetFloat("drainable" + i));
        }
    }

    public static void placeItemTypeInBox(CraftRecipeData data, IsoGameCharacter character) {
        for (int i = 0; i < RecipeCodeOnCreate.getConsumedItems(data, InventoryItem.class).size(); ++i) {
            InventoryItem packedItem = RecipeCodeOnCreate.getConsumedItems(data, InventoryItem.class).get(i);
            ((InventoryItem)data.getAllCreatedItems().getFirst()).getModData().rawset("packedItem" + i, (Object)packedItem.getFullType());
            ((InventoryItem)data.getAllCreatedItems().getFirst()).getModData().rawset("numberOfPackedItems", (Object)RecipeCodeOnCreate.getConsumedItems(data, InventoryItem.class).size());
        }
    }

    public static void unpackItemTypeFromBox(CraftRecipeData data, IsoGameCharacter character) {
        KahluaTableImpl modData = (KahluaTableImpl)((InventoryItem)data.getAllConsumedItems().getFirst()).getModData();
        int numberOfItems = modData.rawgetInt("numberOfPackedItems");
        if (numberOfItems > 0) {
            String unpackedItem;
            data.getToOutputItems().clear();
            for (int i = 0; i < numberOfItems && (unpackedItem = modData.rawgetStr("packedItem" + i)) != null && !unpackedItem.isEmpty(); ++i) {
                Object item = InventoryItemFactory.CreateItem(unpackedItem);
                if (item == null) continue;
                RecipeCodeOnCreate.addItemToCharacterInventory(character, item);
            }
        }
    }

    public static void knappFlake(CraftRecipeData data, IsoGameCharacter character) {
        int skill = character.getPerkLevel(PerkFactory.Perks.FlintKnapping);
        if (Rand.Next(11) < skill) {
            RecipeCodeOnCreate.addItemToCharacterInventory(character, ItemKey.Normal.SHARPED_STONE);
        }
    }

    public static void slightlyMoreDurable(CraftRecipeData data, IsoGameCharacter character) {
        if (!RecipeCodeOnCreate.getConsumedItems(data, ItemTag.INFERIOR_BINDING).isEmpty()) {
            ((InventoryItem)data.getAllCreatedItems().getFirst()).incrementCondition(1);
        }
    }

    public static void untieHeadband(CraftRecipeData data, IsoGameCharacter character) {
        Clothing clothing;
        Object object = data.getAllConsumedItems().getFirst();
        if (object instanceof Clothing && ((clothing = (Clothing)object).isDirty() || clothing.isBloody())) {
            RecipeCodeOnCreate.removeItemFromCharacterInventory(character, (InventoryItem)data.getAllCreatedItems().getFirst());
            RecipeCodeOnCreate.addItemToCharacterInventory(character, ItemKey.Normal.LEATHER_STRIPS_DIRTY);
        }
    }

    public static void smeltIronOrSteelSmall(CraftRecipeData data, IsoGameCharacter character) {
        RecipeCodeOnCreate.smeltIronOrSteel(data, character, 1);
    }

    public static void smeltIronOrSteelMedium(CraftRecipeData data, IsoGameCharacter character) {
        RecipeCodeOnCreate.smeltIronOrSteel(data, character, 2);
    }

    public static void smeltIronOrSteelMediumPlus(CraftRecipeData data, IsoGameCharacter character) {
        RecipeCodeOnCreate.smeltIronOrSteel(data, character, 3);
    }

    public static void smeltIronOrSteelLarge(CraftRecipeData data, IsoGameCharacter character) {
        RecipeCodeOnCreate.smeltIronOrSteel(data, character, 4);
    }

    public static void smeltIronOrSteelIngot(CraftRecipeData data, IsoGameCharacter character) {
        RecipeCodeOnCreate.smeltIronOrSteel(data, character, 12);
    }

    private static void smeltIronOrSteel(CraftRecipeData data, IsoGameCharacter character, int add) {
        int uses = 0;
        List<DrainableComboItem> consumedItems = RecipeCodeOnCreate.getConsumedItems(data, DrainableComboItem.class);
        if (!consumedItems.isEmpty()) {
            uses = ((DrainableComboItem)consumedItems.getFirst()).getCurrentUses();
        }
        DrainableComboItem result = (DrainableComboItem)RecipeCodeOnCreate.getCreatedItems(data, DrainableComboItem.class).getFirst();
        result.setCurrentUses(Math.min(result.getMaxUses(), uses + add));
    }

    public static void sewHideJacket(CraftRecipeData data, IsoGameCharacter character) {
        InventoryItem result = (InventoryItem)data.getAllCreatedItems().getFirst();
        ItemVisual newVisual = result.getVisual();
        int skill = character.getPerkLevel(PerkFactory.Perks.Tailoring);
        if (skill < 6 || Rand.Next(21) >= skill) {
            newVisual.setBaseTexture(0);
            newVisual.setTextureChoice(0);
            return;
        }
        newVisual.setBaseTexture(1);
        newVisual.setTextureChoice(1);
    }

    public static void pickAramidThread(CraftRecipeData data, IsoGameCharacter character) {
        Clothing item = (Clothing)RecipeCodeOnCreate.getConsumedItems(data, ItemTag.PICK_ARAMID_THREAD).getFirst();
        int nbrOfCoveredParts = Math.max(1, item.getNbrOfCoveredParts() - (item.getHolesNumber() + item.getPatchesNumber()));
        int skill = character.getPerkLevel(PerkFactory.Perks.Tailoring) / 2;
        int nbr = Rand.Next(nbrOfCoveredParts, nbrOfCoveredParts * 2 + skill);
        nbr = PZMath.clamp(nbr, 1, 10);
        ((InventoryItem)data.getAllCreatedItems().getFirst()).setCurrentUses(nbr);
        if (item.hasTag(ItemTag.BUCKET)) {
            RecipeCodeOnCreate.addItemToCharacterInventory(character, ItemKey.Normal.BUCKLE);
        }
    }

    public static void openAndEat(CraftRecipeData data, IsoGameCharacter character) {
        if ((float)data.getEatPercentage() <= 0.0f || character.getMoodles().getMoodleLevel(MoodleType.FOOD_EATEN) >= 3) {
            return;
        }
        if (GameServer.server) {
            KahluaTable table = LuaManager.platform.newTable();
            table.rawset("onlineID", (Object)character.getOnlineID());
            table.rawset("itemId", (Object)((InventoryItem)data.getAllCreatedItems().getFirst()).getID());
            table.rawset("eatPercentage", (Object)((double)data.getEatPercentage() / 100.0));
            GameServer.sendServerCommand((IsoPlayer)character, "recipe", "openAndEat", table);
            return;
        }
        KahluaTableImpl eatFoodActionLua = (KahluaTableImpl)LuaManager.env.rawget("ISEatFoodAction");
        Object[] eatFoodActionCallback = LuaManager.caller.pcall(LuaManager.thread, eatFoodActionLua.rawget("new"), eatFoodActionLua, character, data.getAllCreatedItems().getFirst(), data.getEatPercentage() / 100);
        if (((Boolean)eatFoodActionCallback[0]).booleanValue()) {
            LuaManager.caller.pcall(LuaManager.thread, ((KahluaTableImpl)LuaManager.env.rawget("ISTimedActionQueue")).rawget("add"), eatFoodActionCallback[1]);
        }
    }

    public static void copyKey(CraftRecipeData data, IsoGameCharacter character) {
        Key sourceKey = (Key)RecipeCodeOnCreate.getInputItems(data, ItemTag.BUILDING_KEY).getFirst();
        data.getFirstCreatedItem().setKeyId(sourceKey.getKeyId());
    }

    public static void openMacAndCheese(CraftRecipeData data, IsoGameCharacter isoGameCharacter) {
        Food macaroni = (Food)RecipeCodeOnCreate.getCreatedItems(data, ItemTag.PASTA).getFirst();
        macaroni.copyFoodFromSplit(macaroni, 6);
    }
}

