/*
 * Decompiled with CFR 0.152.
 */
package zombie.entity.components.crafting.recipe;

import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import zombie.UsedFromLua;
import zombie.ZomboidFileSystem;
import zombie.characters.IsoGameCharacter;
import zombie.characters.IsoPlayer;
import zombie.core.Core;
import zombie.core.math.PZMath;
import zombie.core.random.Rand;
import zombie.debug.DebugLog;
import zombie.entity.components.crafting.CraftRecipeMonitor;
import zombie.entity.components.crafting.InputFlag;
import zombie.entity.components.crafting.OutputFlag;
import zombie.entity.components.crafting.recipe.CraftRecipeData;
import zombie.entity.components.crafting.recipe.HandcraftLogic;
import zombie.entity.components.fluids.FluidConsume;
import zombie.entity.components.fluids.FluidContainer;
import zombie.entity.components.fluids.FluidSample;
import zombie.entity.components.resources.Resource;
import zombie.entity.components.resources.ResourceEnergy;
import zombie.entity.components.resources.ResourceFluid;
import zombie.entity.components.resources.ResourceItem;
import zombie.entity.components.resources.ResourceType;
import zombie.inventory.InventoryItem;
import zombie.inventory.InventoryItemFactory;
import zombie.inventory.ItemContainer;
import zombie.inventory.types.DrainableComboItem;
import zombie.inventory.types.InventoryContainer;
import zombie.network.GameClient;
import zombie.scripting.ScriptManager;
import zombie.scripting.entity.components.crafting.CraftRecipe;
import zombie.scripting.entity.components.crafting.InputScript;
import zombie.scripting.entity.components.crafting.OutputScript;
import zombie.scripting.objects.CraftRecipeTag;
import zombie.scripting.objects.Item;
import zombie.util.StringUtils;
import zombie.util.TaggedObjectManager;
import zombie.util.list.PZArrayUtil;
import zombie.util.list.PZUnmodifiableList;

@UsedFromLua
public class CraftRecipeManager {
    private static final float FLOAT_EPSILON = 1.0E-5f;
    private static boolean initialized;
    private static TaggedObjectManager<CraftRecipe> craftRecipeTagManager;
    private static List<CraftRecipe> unmodifiableAllRecipes;
    private static final Map<IsoPlayer, CraftRecipeData> playerCraftDataMap;
    private static final ArrayList<CraftRecipe> RecipeList;

    public static void Reset() {
        playerCraftDataMap.clear();
        craftRecipeTagManager.clear();
        craftRecipeTagManager = null;
        unmodifiableAllRecipes = null;
        initialized = false;
    }

    public static void Init() {
        craftRecipeTagManager = new TaggedObjectManager<CraftRecipe>(new CraftRecipeListProvider());
        craftRecipeTagManager.registerObjectsFromBackingList();
        initialized = true;
        CraftRecipeManager.LogAllRecipesToFile();
    }

    public static String FormatAndRegisterRecipeTagsQuery(String tagQueryString) throws Exception {
        if (!initialized && Core.debug) {
            throw new RuntimeException("Not initialized.");
        }
        return craftRecipeTagManager.formatAndRegisterQueryString(tagQueryString);
    }

    public static String sanitizeTagQuery(String tagQueryString) {
        return craftRecipeTagManager.formatQueryString(tagQueryString);
    }

    public static List<CraftRecipe> getRecipesForTag(String category) {
        if (!initialized && Core.debug) {
            throw new RuntimeException("Not initialized.");
        }
        return craftRecipeTagManager.getListForTag(category);
    }

    public static List<String> getAllRecipeTags() {
        if (!initialized && Core.debug) {
            throw new RuntimeException("Not initialized.");
        }
        return craftRecipeTagManager.getRegisteredTags();
    }

    public static List<String> getTagGroups() {
        if (!initialized && Core.debug) {
            throw new RuntimeException("Not initialized.");
        }
        ArrayList<String> list = new ArrayList<String>();
        craftRecipeTagManager.getRegisteredTagGroups(list);
        return list;
    }

    public static void debugPrintTagManager() {
        if (!initialized && Core.debug) {
            throw new RuntimeException("Not initialized.");
        }
        craftRecipeTagManager.debugPrint();
    }

    public static ArrayList<String> debugPrintTagManagerLines() {
        if (!initialized && Core.debug) {
            throw new RuntimeException("Not initialized.");
        }
        ArrayList<String> list = new ArrayList<String>();
        craftRecipeTagManager.debugPrint(list);
        return list;
    }

    public static void LogAllRecipesToFile() {
        List<CraftRecipe> recipes = CraftRecipeManager.queryRecipes("*");
        List<String> tags = CraftRecipeManager.getAllRecipeTags();
        try {
            int i;
            String outputPath = ZomboidFileSystem.instance.getCacheDirSub("Crafting");
            ZomboidFileSystem.ensureFolderExists(outputPath);
            FileWriter writer = new FileWriter(outputPath + File.separator + "AllRecipes.txt");
            writer.write("Recipe and Tag reference\n\n");
            writer.write("Available Tags:\n");
            for (i = 0; i < tags.size(); ++i) {
                writer.write(i + ": \t" + tags.get(i) + "\n");
            }
            writer.write("\nAll Recipes:\n");
            for (i = 0; i < recipes.size(); ++i) {
                writer.write(i + ": \t" + recipes.get(i).getName() + "\n");
            }
            writer.flush();
            writer.close();
        }
        catch (Exception exception) {
            // empty catch block
        }
    }

    public static List<CraftRecipe> queryRecipes(String tagQueryString) {
        if (!initialized && Core.debug) {
            throw new RuntimeException("Not initialized.");
        }
        if (!StringUtils.isNullOrWhitespace(tagQueryString) && tagQueryString.equals("*")) {
            if (unmodifiableAllRecipes == null) {
                unmodifiableAllRecipes = PZUnmodifiableList.wrap(ScriptManager.instance.getAllCraftRecipes());
            }
            return unmodifiableAllRecipes;
        }
        return craftRecipeTagManager.queryTaggedObjects(tagQueryString);
    }

    public static List<CraftRecipe> queryRecipes(CraftRecipeTag ... craftRecipeTag) {
        StringBuilder query = new StringBuilder();
        for (int i = 0; i < craftRecipeTag.length; ++i) {
            query.append(craftRecipeTag[i].toString()).append(";");
        }
        return CraftRecipeManager.queryRecipes(query.toString());
    }

    public static List<CraftRecipe> populateRecipeList(String tagQueryString, List<CraftRecipe> listToPopulate, boolean clearList) {
        if (!initialized && Core.debug) {
            throw new RuntimeException("Not initialized.");
        }
        return craftRecipeTagManager.populateList(tagQueryString, listToPopulate, null, clearList);
    }

    public static List<CraftRecipe> populateRecipeList(String tagQueryString, List<CraftRecipe> listToPopulate, List<CraftRecipe> sourceList, boolean clearList) {
        if (!initialized && Core.debug) {
            throw new RuntimeException("Not initialized.");
        }
        return craftRecipeTagManager.populateList(tagQueryString, listToPopulate, sourceList, clearList);
    }

    public static List<CraftRecipe> filterRecipeList(String filterString, List<CraftRecipe> listToPopulate) {
        return CraftRecipeManager.filterRecipeList(filterString, listToPopulate, ScriptManager.instance.getAllCraftRecipes());
    }

    public static List<CraftRecipe> filterRecipeList(String filterString, List<CraftRecipe> listToPopulate, List<CraftRecipe> sourceList) {
        if (listToPopulate != null) {
            listToPopulate.clear();
        }
        if (sourceList == null || listToPopulate == null) {
            DebugLog.General.error("one of list parameters is null.");
            return listToPopulate;
        }
        if (StringUtils.isNullOrWhitespace(filterString)) {
            listToPopulate.addAll(sourceList);
            return listToPopulate;
        }
        FilterMode filterMode = FilterMode.Name;
        if (filterString.startsWith("@")) {
            filterMode = FilterMode.ModName;
            filterString = filterString.substring(1);
        } else if (filterString.startsWith("$")) {
            filterMode = FilterMode.Tags;
            filterString = filterString.substring(1);
        }
        filterString = filterString.toLowerCase();
        switch (filterMode.ordinal()) {
            case 0: {
                for (int i = 0; i < sourceList.size(); ++i) {
                    CraftRecipe recipe = sourceList.get(i);
                    if (recipe.getTranslationName() == null || !recipe.getTranslationName().toLowerCase().contains(filterString)) continue;
                    listToPopulate.add(recipe);
                }
                break;
            }
            case 1: {
                for (int i = 0; i < sourceList.size(); ++i) {
                    CraftRecipe recipe = sourceList.get(i);
                    if (recipe.getModName() == null || !recipe.getModName().toLowerCase().contains(filterString)) continue;
                    listToPopulate.add(recipe);
                }
                break;
            }
            case 2: {
                StringBuilder query = new StringBuilder();
                for (int i = 0; i < craftRecipeTagManager.getRegisteredTags().size(); ++i) {
                    String tag = craftRecipeTagManager.getRegisteredTags().get(i);
                    if (!tag.contains(filterString)) continue;
                    if (!query.isEmpty()) {
                        query.append(";");
                    }
                    query.append(tag);
                }
                String queryString = query.toString();
                craftRecipeTagManager.filterList(queryString, listToPopulate, sourceList, true);
            }
        }
        return listToPopulate;
    }

    public static CraftRecipeData getCraftDataForPlayer(IsoPlayer player) {
        CraftRecipeData data = playerCraftDataMap.get(player);
        if (data == null) {
            playerCraftDataMap.put(player, data);
        }
        throw new RuntimeException("not implemented");
    }

    public static ArrayList<InventoryItem> getAllItemsFromContainers(ArrayList<ItemContainer> containers, ArrayList<InventoryItem> items) {
        items.clear();
        for (int i = 0; i < containers.size(); ++i) {
            PZArrayUtil.addAll(items, containers.get(i).getItems());
        }
        return items;
    }

    public static ArrayList<InventoryItem> getAllValidItemsForRecipe(CraftRecipe recipe, ArrayList<InventoryItem> sourceItems, ArrayList<InventoryItem> filteredItems, IsoGameCharacter character) {
        for (int i = 0; i < sourceItems.size(); ++i) {
            InventoryItem inventoryItem = sourceItems.get(i);
            if (!CraftRecipeManager.isItemValidForRecipe(recipe, inventoryItem, character)) continue;
            filteredItems.add(inventoryItem);
        }
        return filteredItems;
    }

    public static InputScript getValidInputScriptForItem(CraftRecipe recipe, InventoryItem inventoryItem, IsoGameCharacter character) {
        for (int i = 0; i < recipe.getInputs().size(); ++i) {
            InputScript input = recipe.getInputs().get(i);
            if (input.getResourceType() != ResourceType.Item || !CraftRecipeManager.isItemValidForInputScript(input, inventoryItem, character)) continue;
            return input;
        }
        return null;
    }

    public static ArrayList<InputScript> getAllValidInputScriptsForItem(CraftRecipe recipe, InventoryItem inventoryItem, IsoGameCharacter character) {
        ArrayList<InputScript> output = new ArrayList<InputScript>();
        for (int i = 0; i < recipe.getInputs().size(); ++i) {
            InputScript input = recipe.getInputs().get(i);
            if (input.getResourceType() != ResourceType.Item || !CraftRecipeManager.isItemValidForInputScript(input, inventoryItem, character)) continue;
            output.add(input);
        }
        return output;
    }

    public static boolean isItemToolForRecipe(CraftRecipe recipe, InventoryItem inventoryItem, IsoGameCharacter character) {
        InputScript input = CraftRecipeManager.getValidInputScriptForItem(recipe, inventoryItem, character);
        return input != null && (input.hasFlag(InputFlag.ToolLeft) || input.hasFlag(InputFlag.ToolRight));
    }

    public static boolean isItemValidForRecipe(CraftRecipe recipe, InventoryItem inventoryItem, IsoGameCharacter character) {
        InputScript input = CraftRecipeManager.getValidInputScriptForItem(recipe, inventoryItem, character);
        return input != null;
    }

    public static boolean isItemValidForInputScript(InputScript input, InventoryItem inventoryItem, IsoGameCharacter character) {
        if (input.getResourceType() == ResourceType.Item) {
            return CraftRecipeManager.consumeInputItem(input, inventoryItem, true, null, character);
        }
        return false;
    }

    public static boolean isValidRecipeForCharacter(CraftRecipe recipe, IsoGameCharacter character, CraftRecipeMonitor monitor, ArrayList<ItemContainer> containers) {
        if (recipe == null) {
            DebugLog.CraftLogic.debugln("Recipe is null");
            return false;
        }
        if (character != null && !CraftRecipeManager.validateHasRequiredSkill(recipe, character, containers)) {
            if (monitor != null) {
                monitor.log("Player doesn't have required skill for " + recipe.getScriptObjectFullType());
            }
            DebugLog.CraftLogic.debugln("Player doesn't have required skill for " + recipe.getScriptObjectFullType());
            return false;
        }
        if (character != null && recipe.needToBeLearn() && !character.isRecipeKnown(recipe, true)) {
            if (monitor != null) {
                monitor.log("Player doesn't know recipe " + recipe.getScriptObjectFullType());
            }
            DebugLog.CraftLogic.debugln("Player doesn't know recipe " + recipe.getScriptObjectFullType());
            return false;
        }
        return true;
    }

    private static boolean validateHasRequiredSkill(CraftRecipe recipe, IsoGameCharacter chr, ArrayList<ItemContainer> containers) {
        if (recipe.getRequiredSkillCount() > 0) {
            for (int i = 0; i < recipe.getRequiredSkillCount(); ++i) {
                CraftRecipe.RequiredSkill skill = recipe.getRequiredSkill(i);
                if (chr.getPerkLevel(skill.getPerk()) >= skill.getLevel()) continue;
                if (containers != null) {
                    if (recipe.validateBenefitFromRecipeAtHand(chr, containers)) continue;
                    return false;
                }
                return false;
            }
        }
        return true;
    }

    public static boolean hasPlayerLearnedRecipe(CraftRecipe recipe, IsoGameCharacter character) {
        return recipe.hasPlayerLearned(character);
    }

    public static boolean hasPlayerRequiredSkill(CraftRecipe.RequiredSkill requiredSkill, IsoGameCharacter character) {
        if (requiredSkill == null || character == null) {
            return false;
        }
        return character.getPerkLevel(requiredSkill.getPerk()) >= requiredSkill.getLevel();
    }

    public static int getAutoCraftCountItems(CraftRecipe recipe, ArrayList<InventoryItem> allItems) {
        int worstInputCount = 0;
        for (int i = 0; i < recipe.getInputs().size(); ++i) {
            InputScript input = recipe.getInputs().get(i);
            if (input.getResourceType() != ResourceType.Item) continue;
            int currentInputCount = 0;
            int fullConsumeCount = 0;
            for (int j = 0; j < allItems.size(); ++j) {
                InventoryItem inventoryItem = allItems.get(j);
                if (!CraftRecipeManager.consumeInputItem(input, inventoryItem, true, null, null)) continue;
                if (!input.isKeep()) {
                    ++fullConsumeCount;
                    continue;
                }
                if (input.hasConsumeFromItem()) {
                    switch (input.getResourceType()) {
                        case Item: {
                            currentInputCount += inventoryItem.getCurrentUses() / (int)input.getAmount();
                            break;
                        }
                        case Fluid: {
                            currentInputCount += PZMath.fastfloor(inventoryItem.getFluidContainer().getAmount() / input.getAmount());
                            break;
                        }
                        case Energy: {
                            currentInputCount += inventoryItem.getCurrentUses() / (int)input.getAmount();
                        }
                    }
                    continue;
                }
                ++currentInputCount;
            }
            if (fullConsumeCount > 0) {
                int consumes = PZMath.fastfloor((float)fullConsumeCount / (float)input.getIntAmount());
                worstInputCount = PZMath.min(worstInputCount, consumes);
            } else if (currentInputCount > 0) {
                worstInputCount = PZMath.min(worstInputCount, currentInputCount);
            }
            if (worstInputCount == 0) break;
        }
        return PZMath.max(0, worstInputCount);
    }

    private static boolean validateInputScript(InputScript inputScript, ResourceType resourceType, boolean testOnly) {
        if (inputScript == null || inputScript.getResourceType() != resourceType) {
            if (Core.debug) {
                throw new RuntimeException("Wrong InputScript.ResourceType for call or null, input=" + String.valueOf(inputScript));
            }
            return false;
        }
        if (Core.debug && !testOnly && GameClient.client) {
            throw new RuntimeException("Recipes can only be tested on client, input=" + String.valueOf(inputScript));
        }
        return true;
    }

    private static boolean validateOutputScript(OutputScript outputScript, ResourceType resourceType, boolean testOnly) {
        if (outputScript == null || outputScript.getResourceType() != resourceType) {
            if (Core.debug) {
                throw new RuntimeException("Wrong OutputScript.ResourceType for call or null, output=" + String.valueOf(outputScript));
            }
            return false;
        }
        if (Core.debug && !testOnly && GameClient.client) {
            throw new RuntimeException("Recipes can only be tested on client, output=" + String.valueOf(outputScript));
        }
        return true;
    }

    protected static boolean consumeInputFromResources(InputScript input, List<Resource> resources, boolean testOnly, CraftRecipeData.CacheData cacheData, IsoGameCharacter character, Set<Resource> usedResources) {
        if (input == null || resources == null) {
            return false;
        }
        switch (input.getResourceType()) {
            case Item: {
                return CraftRecipeManager.consumeInputItem(input, resources, testOnly, cacheData, character, usedResources);
            }
            case Fluid: {
                return CraftRecipeManager.consumeInputFluid(input, resources, testOnly, cacheData);
            }
            case Energy: {
                return CraftRecipeManager.consumeInputEnergy(input, resources, testOnly, cacheData);
            }
        }
        return false;
    }

    protected static boolean consumeInputItem(InputScript input, List<Resource> resources, boolean testOnly, CraftRecipeData.CacheData cacheData, IsoGameCharacter character, Set<Resource> usedResources) {
        if (!CraftRecipeManager.validateInputScript(input, ResourceType.Item, testOnly)) {
            return false;
        }
        if (resources == null || resources.isEmpty()) {
            return false;
        }
        float targetRequiredAmount = input.getIntAmount();
        float maxAllowedAmount = input.getIntMaxAmount();
        if (input.hasConsumeFromItem()) {
            InputScript consumeScript = input.getConsumeFromItemScript();
            targetRequiredAmount = consumeScript.getAmount();
            maxAllowedAmount = consumeScript.getMaxAmount();
        }
        if (targetRequiredAmount <= 0.0f) {
            return false;
        }
        HashSet<InventoryItem> tempUsedItems = new HashSet<InventoryItem>();
        HashSet<Resource> tempUsedResources = new HashSet<Resource>();
        HashMap<InventoryItem, Resource> tempConsumedUsedItems = new HashMap<InventoryItem, Resource>();
        HashMap<Resource, ArrayList<InventoryItem>> tempVariableOverfillUsedItems = new HashMap<Resource, ArrayList<InventoryItem>>();
        float requiredAmount = targetRequiredAmount;
        float allowedAmount = maxAllowedAmount;
        boolean canMoveItemsToOutput = true;
        List<Item> inputItems = input.getPossibleInputItems();
        for (int j = 0; j < inputItems.size(); ++j) {
            block7: for (int idx = 0; idx < resources.size(); ++idx) {
                Resource resource = resources.get(idx);
                if (resource == null || resource.isEmpty() || input.getResourceType() != resource.getType() || usedResources.contains(resource) || tempUsedResources.contains(resource)) continue;
                for (int i = 0; i < resource.getItemAmount(); ++i) {
                    InventoryItem inventoryItem = resource.peekItem(i);
                    if (!inventoryItem.getScriptItem().getFullName().equals(inputItems.get(j).getFullName()) || tempUsedItems.contains(inventoryItem)) continue;
                    float preUsedFluidAvail = 0.0f;
                    if (!testOnly && input.hasConsumeFromItem()) {
                        InputScript consumeScript = input.getConsumeFromItemScript();
                        switch (consumeScript.getResourceType()) {
                            case Fluid: {
                                preUsedFluidAvail = inventoryItem.getFluidContainer().getAmount();
                            }
                        }
                    }
                    if (!CraftRecipeManager.consumeInputItem(input, inventoryItem, testOnly, cacheData, character)) continue;
                    tempUsedItems.add(inventoryItem);
                    tempUsedResources.add(resource);
                    if (!testOnly) {
                        if (requiredAmount > 1.0E-5f) {
                            tempConsumedUsedItems.put(inventoryItem, resource);
                            canMoveItemsToOutput = canMoveItemsToOutput && resource.canMoveItemsToOutput();
                        } else {
                            tempVariableOverfillUsedItems.computeIfAbsent(resource, k -> new ArrayList()).add(inventoryItem);
                        }
                    }
                    float uses = input.isItemCount() ? 1.0f : (float)inventoryItem.getCurrentUses();
                    float scale = input.getRelativeScale(inventoryItem.getFullType());
                    float requses = uses / scale;
                    if (input.hasConsumeFromItem()) {
                        InputScript script = input.getConsumeFromItemScript();
                        switch (script.getResourceType()) {
                            case Fluid: {
                                if (!testOnly) {
                                    float postUsedFluidAvail = inventoryItem.getFluidContainer().getAmount();
                                    requses = uses = preUsedFluidAvail - postUsedFluidAvail;
                                    break;
                                }
                                requses = uses = inventoryItem.getFluidContainer().getAmount();
                            }
                        }
                    }
                    uses = Math.min(requses, uses);
                    requiredAmount -= uses;
                    if ((allowedAmount -= uses) <= 1.0E-5f) continue block7;
                }
            }
            if (requiredAmount <= 1.0E-5f) {
                if (cacheData != null && !canMoveItemsToOutput) {
                    cacheData.setMoveToOutputs(false);
                }
                if (!(testOnly || input.isKeep() && !canMoveItemsToOutput)) {
                    for (Map.Entry entry : tempConsumedUsedItems.entrySet()) {
                        InventoryItem item = (InventoryItem)entry.getKey();
                        ResourceItem resourceItem = (ResourceItem)entry.getValue();
                        InventoryItem removedItem = resourceItem.removeItem(item);
                        if (removedItem != null && removedItem == item || !Core.debug) continue;
                        throw new RuntimeException("Item didn't get removed.");
                    }
                    cacheData.getRecipeData().addOverfilledResource(input, tempVariableOverfillUsedItems);
                }
                if (input.isVariableAmount()) {
                    float filledAmount = Math.min(targetRequiredAmount - requiredAmount, maxAllowedAmount);
                    float filledRatio = filledAmount / targetRequiredAmount;
                    cacheData.getRecipeData().setCalculatedVariableInputRatio(Math.min(cacheData.getRecipeData().getCalculatedVariableInputRatio(), filledRatio));
                }
                usedResources.addAll(tempUsedResources);
                return true;
            }
            if (!input.isExclusive()) continue;
            requiredAmount = targetRequiredAmount;
            allowedAmount = maxAllowedAmount;
            tempUsedItems.clear();
            tempUsedResources.clear();
            tempVariableOverfillUsedItems.clear();
        }
        return false;
    }

    protected static boolean consumeInputItem(InputScript input, InventoryItem inventoryItem, boolean testOnly, CraftRecipeData.CacheData cacheData, IsoGameCharacter character) {
        CraftRecipe.IOScript script;
        if (!CraftRecipeManager.validateInputScript(input, ResourceType.Item, testOnly)) {
            return false;
        }
        boolean success = CraftRecipeManager.consumeInputItemInternal(input, inventoryItem, testOnly, cacheData, character);
        boolean moveToOutput = input.isKeep();
        if (success && input.hasConsumeFromItem()) {
            script = input.getConsumeFromItemScript();
            switch (((InputScript)script).getResourceType()) {
                case Item: {
                    success = CraftRecipeManager.consumeInputItemUsesInternal((InputScript)script, inventoryItem, testOnly, cacheData);
                    if (!success || input.isDestroy()) break;
                    moveToOutput = (float)inventoryItem.getCurrentUses() < input.getAmount();
                    break;
                }
                case Fluid: {
                    success = CraftRecipeManager.consumeInputFluidInternal((InputScript)script, inventoryItem.getFluidContainer(), testOnly, cacheData);
                    if (!success) break;
                    moveToOutput = inventoryItem.getFluidContainer().getAmount() < input.getAmount();
                    break;
                }
                case Energy: {
                    success = CraftRecipeManager.consumeInputEnergyFromItemInternal((InputScript)script, inventoryItem, testOnly, cacheData);
                    if (!success) break;
                    boolean bl = moveToOutput = (float)inventoryItem.getCurrentUses() < input.getAmount();
                }
            }
        }
        if (success && input.hasCreateToItem()) {
            script = input.getCreateToItemScript();
            switch (((OutputScript)script).getResourceType()) {
                case Item: {
                    success = CraftRecipeManager.createOutputItemUsesInternal((OutputScript)script, inventoryItem, testOnly, cacheData);
                    break;
                }
                case Fluid: {
                    success = CraftRecipeManager.createOutputFluidInternal((OutputScript)script, inventoryItem.getFluidContainer(), testOnly, cacheData);
                    break;
                }
                case Energy: {
                    success = CraftRecipeManager.createOutputEnergyToItemInternal((OutputScript)script, inventoryItem, testOnly, cacheData);
                }
            }
        }
        if (success) {
            if (cacheData != null) {
                cacheData.setMoveToOutputs(moveToOutput);
            }
            return true;
        }
        if (cacheData != null) {
            cacheData.softReset();
        }
        return false;
    }

    private static boolean consumeInputItemInternal(InputScript input, InventoryItem inventoryItem, boolean testOnly, CraftRecipeData.CacheData cacheData, IsoGameCharacter character) {
        Item item = inventoryItem.getScriptItem();
        if (item == null) {
            return false;
        }
        if (input.containsItem(item)) {
            InventoryContainer bag;
            String inputItemType = inventoryItem.getType();
            if (!input.getParentRecipe().OnTestItem(inventoryItem, character)) {
                return false;
            }
            if (!input.doesItemPassRoutineStatusTests(inventoryItem, character)) {
                DebugLog.CraftLogic.debugln(inputItemType + " fails routine status test for " + input.getParentRecipe().getName() + " - " + input.getOriginalLine());
                return false;
            }
            if (!input.doesItemPassClothingTypeStatusTests(inventoryItem)) {
                DebugLog.CraftLogic.debugln(inputItemType + " fails clothing status test for " + input.getParentRecipe().getName() + " - " + input.getOriginalLine());
                return false;
            }
            if (!input.doesItemPassSharpnessStatusTests(inventoryItem)) {
                DebugLog.CraftLogic.debugln(inputItemType + " fails sharpness status test for " + input.getParentRecipe().getName() + " - " + input.getOriginalLine());
                return false;
            }
            if (!input.doesItemPassDamageStatusTests(inventoryItem)) {
                DebugLog.CraftLogic.debugln(inputItemType + " fails damage status test for " + input.getParentRecipe().getName() + " - " + input.getOriginalLine());
                return false;
            }
            if (!input.doesItemPassIsOrNotEmptyAndFullTests(inventoryItem)) {
                DebugLog.CraftLogic.debugln(inputItemType + " fails empty/full status test for " + input.getParentRecipe().getName() + " - " + input.getOriginalLine());
                return false;
            }
            if (!input.doesItemPassFoodAndCookingTests(inventoryItem)) {
                DebugLog.CraftLogic.debugln(inputItemType + " fails food/cooking status test for " + input.getParentRecipe().getName() + " - " + input.getOriginalLine());
                return false;
            }
            if (input.hasFlag(InputFlag.IsEmptyContainer) && inventoryItem instanceof InventoryContainer && !(bag = (InventoryContainer)inventoryItem).isEmpty()) {
                DebugLog.CraftLogic.debugln(inputItemType + " fails empty bag status test for " + input.getParentRecipe().getName() + " - " + input.getOriginalLine());
            }
            if (cacheData != null) {
                cacheData.addAppliedItem(inventoryItem);
            }
            return true;
        }
        return false;
    }

    private static boolean consumeInputItemUsesInternal(InputScript input, InventoryItem inventoryItem, boolean testOnly, CraftRecipeData.CacheData cacheData) {
        if (inventoryItem instanceof DrainableComboItem) {
            DrainableComboItem comboItem = (DrainableComboItem)inventoryItem;
            Item item = inventoryItem.getScriptItem();
            if (item == null || !input.containsItem(item)) {
                return false;
            }
            if (input.hasFlag(InputFlag.IsFull) && !comboItem.isFullUses()) {
                return false;
            }
            int useAmount = 0;
            if (input.getAmount() > 0.0f) {
                useAmount = (int)input.getAmount();
            }
            if (comboItem.getCurrentUses() >= useAmount) {
                if (!testOnly && !input.isKeep()) {
                    comboItem.setCurrentUses(comboItem.getCurrentUses() - useAmount);
                }
                if (cacheData != null) {
                    cacheData.usesConsumed = useAmount;
                }
                return true;
            }
        }
        return false;
    }

    private static boolean consumeInputFluid(InputScript input, List<Resource> resources, boolean testOnly, CraftRecipeData.CacheData cacheData) {
        if (!CraftRecipeManager.validateInputScript(input, ResourceType.Fluid, testOnly)) {
            return false;
        }
        for (Resource resource : resources) {
            if (resource == null || resource.getType() != ResourceType.Fluid || resource.isEmpty() || !CraftRecipeManager.consumeInputFluidInternal(input, ((ResourceFluid)resource).getFluidContainer(), testOnly, cacheData)) continue;
            return true;
        }
        return false;
    }

    private static boolean consumeInputFluidInternal(InputScript input, FluidContainer fc, boolean testOnly, CraftRecipeData.CacheData cacheData) {
        if (fc != null && !fc.isEmpty()) {
            InputScript parentScript;
            if (input.hasFlag(InputFlag.IsFull) && !fc.isFull()) {
                return false;
            }
            float requiredFluid = input.getAmount();
            InputScript inputScript = parentScript = input.hasParentScript() ? input.getParentScript() : input;
            if (parentScript.isItemCount() && fc.getAmount() < requiredFluid) {
                return false;
            }
            if (cacheData != null) {
                requiredFluid = input.getAmount() - cacheData.fluidConsumed;
            }
            if (input.isFluidMatch(fc)) {
                float fluidToTake = Math.min(requiredFluid, fc.getAmount());
                if (!testOnly) {
                    if (cacheData != null) {
                        FluidSample newSample = fc.createFluidSample(fluidToTake);
                        cacheData.fluidSample.combineWith(newSample);
                        newSample.release();
                    }
                    if (!input.isKeep()) {
                        if (cacheData != null) {
                            FluidConsume newConsume = fc.removeFluid(fluidToTake, true);
                            cacheData.fluidConsume.combineWith(newConsume);
                            newConsume.release();
                        } else {
                            fc.removeFluid(fluidToTake, false);
                        }
                        if (fc.getGameEntity() != null) {
                            fc.getGameEntity().sendSyncEntity(null);
                        }
                    }
                }
                if (cacheData != null) {
                    cacheData.fluidConsumed += fluidToTake;
                }
                return true;
            }
        }
        return false;
    }

    private static boolean consumeInputEnergy(InputScript input, List<Resource> resources, boolean testOnly, CraftRecipeData.CacheData cacheData) {
        if (!CraftRecipeManager.validateInputScript(input, ResourceType.Energy, testOnly)) {
            return false;
        }
        for (Resource resource : resources) {
            if (resource == null || resource.getType() != ResourceType.Energy || resource.isEmpty()) continue;
            ResourceEnergy resourceEnergy = (ResourceEnergy)resource;
            if (input.hasFlag(InputFlag.IsFull) && !resource.isFull() || !(resourceEnergy.getEnergyAmount() >= input.getAmount()) || !input.isEnergyMatch(resourceEnergy.getEnergy())) continue;
            if (!input.isKeep() && !testOnly) {
                resourceEnergy.setEnergyAmount(resource.getEnergyAmount() - input.getAmount());
            }
            if (cacheData != null) {
                cacheData.energyConsumed = input.getAmount();
            }
            return true;
        }
        return false;
    }

    private static boolean consumeInputEnergyFromItemInternal(InputScript input, InventoryItem inventoryItem, boolean testOnly, CraftRecipeData.CacheData cacheData) {
        if (inventoryItem instanceof DrainableComboItem) {
            int useAmount;
            DrainableComboItem comboItem = (DrainableComboItem)inventoryItem;
            if (input.hasFlag(InputFlag.IsFull) && !comboItem.isFullUses()) {
                return false;
            }
            int n = useAmount = input.getAmount() > 0.0f ? (int)input.getAmount() : 1;
            if (comboItem.getCurrentUses() >= useAmount && input.isEnergyMatch(comboItem)) {
                if (!testOnly && !input.isKeep()) {
                    comboItem.setCurrentUses(comboItem.getCurrentUses() - useAmount);
                }
                if (cacheData != null) {
                    cacheData.energyConsumed = useAmount;
                }
                return true;
            }
        }
        return false;
    }

    protected static boolean createOutputToResource(OutputScript output, Resource resource, boolean testOnly, CraftRecipeData.CacheData cacheData) {
        if (output == null || resource == null || output.getResourceType() != resource.getType()) {
            return false;
        }
        switch (output.getResourceType()) {
            case Fluid: {
                return CraftRecipeManager.createOutputFluid(output, resource, testOnly, cacheData);
            }
            case Energy: {
                return CraftRecipeManager.createOutputEnergy(output, resource, testOnly, cacheData);
            }
        }
        return false;
    }

    protected static boolean createOutputItem(OutputScript output, Item item, boolean testOnly, CraftRecipeData.CacheData cacheData, IsoGameCharacter character) {
        if (!CraftRecipeManager.validateOutputScript(output, ResourceType.Item, testOnly)) {
            return false;
        }
        boolean success = CraftRecipeManager.createOutputItemInternal(output, item, testOnly, cacheData, character);
        if (success && !testOnly && output.hasCreateToItem() && cacheData.getMostRecentItem() != null) {
            OutputScript script = output.getCreateToItemScript();
            boolean b = false;
            InventoryItem inventoryItem = cacheData.getMostRecentItem();
            assert (inventoryItem != null);
            switch (script.getResourceType()) {
                case Item: {
                    b = CraftRecipeManager.createOutputItemUsesInternal(script, inventoryItem, testOnly, cacheData);
                    break;
                }
                case Fluid: {
                    b = CraftRecipeManager.createOutputFluidInternal(script, inventoryItem.getFluidContainer(), testOnly, cacheData);
                    break;
                }
                case Energy: {
                    b = CraftRecipeManager.createOutputEnergyToItemInternal(script, inventoryItem, testOnly, cacheData);
                }
            }
            if (!b) {
                DebugLog.General.warn("unable to create uses/fluid/energy to item: " + (item != null ? item.getFullName() : "unknown"));
            }
        }
        if (success) {
            return true;
        }
        cacheData.softReset();
        return false;
    }

    private static boolean createOutputItemInternal(OutputScript output, Item item, boolean testOnly, CraftRecipeData.CacheData data, IsoGameCharacter character) {
        if (!testOnly && output.getChance() < 1.0f && Rand.Next(0.0f, 1.0f) > output.getChance()) {
            return true;
        }
        if (item == null || item.getFullName() == null) {
            return false;
        }
        if (!testOnly) {
            Object inventoryItem = InventoryItemFactory.CreateItem(item.getFullName());
            if (inventoryItem != null) {
                if (data != null) {
                    data.addAppliedItem((InventoryItem)inventoryItem);
                }
                if (output.hasFlag(OutputFlag.IsBlunt) && ((InventoryItem)inventoryItem).hasSharpness()) {
                    ((InventoryItem)inventoryItem).setSharpness(0.0f);
                }
                if (output.hasFlag(OutputFlag.HasOneUse)) {
                    ((InventoryItem)inventoryItem).setCurrentUses(1);
                }
                if (output.hasFlag(OutputFlag.HasNoUses)) {
                    ((InventoryItem)inventoryItem).setCurrentUses(0);
                }
                if (output.hasFlag(OutputFlag.SetActivated)) {
                    ((InventoryItem)inventoryItem).setCurrentUses(0);
                }
                if (output.hasFlag(OutputFlag.EquipSecondary) && character != null) {
                    if (character.getPrimaryHandItem() == character.getSecondaryHandItem()) {
                        character.setPrimaryHandItem(null);
                    }
                    character.setSecondaryHandItem((InventoryItem)inventoryItem);
                }
            } else {
                DebugLog.General.warn("Failed to create item: " + item.getFullName());
            }
        }
        return true;
    }

    private static boolean createOutputItemUsesInternal(OutputScript output, InventoryItem targetItem, boolean testOnly, CraftRecipeData.CacheData cacheData) {
        if (targetItem instanceof DrainableComboItem) {
            DrainableComboItem comboItem = (DrainableComboItem)targetItem;
            int stored = targetItem.getCurrentUses();
            if (stored > 0 && output.hasFlag(OutputFlag.ForceEmpty)) {
                stored = 0;
                if (!testOnly) {
                    comboItem.setCurrentUses(0);
                }
            }
            if (stored > 0 && output.hasFlag(OutputFlag.IsEmpty)) {
                return false;
            }
            int createAmount = output.getAmount() > 0.0f ? (int)output.getAmount() : 1;
            int amount = PZMath.min(createAmount, comboItem.getMaxUses() - stored);
            if (output.hasFlag(OutputFlag.AlwaysFill)) {
                amount = comboItem.getMaxUses() - stored;
            }
            if (amount <= 0) {
                return false;
            }
            if (amount < createAmount && output.hasFlag(OutputFlag.RespectCapacity)) {
                return false;
            }
            if (!testOnly && output.getChance() < 1.0f && Rand.Next(0.0f, 1.0f) > output.getChance()) {
                return true;
            }
            if (!testOnly) {
                comboItem.setCurrentUses(PZMath.min(comboItem.getCurrentUses() + amount, comboItem.getMaxUses()));
            }
            if (cacheData != null) {
                cacheData.usesCreated = amount;
            }
            return true;
        }
        return false;
    }

    private static boolean createOutputFluid(OutputScript output, Resource resource, boolean testOnly, CraftRecipeData.CacheData cacheData) {
        if (!CraftRecipeManager.validateOutputScript(output, ResourceType.Fluid, testOnly)) {
            return false;
        }
        if (resource == null || resource.getType() != ResourceType.Fluid || resource.isFull()) {
            return false;
        }
        return CraftRecipeManager.createOutputFluidInternal(output, ((ResourceFluid)resource).getFluidContainer(), testOnly, cacheData);
    }

    private static boolean createOutputFluidInternal(OutputScript output, FluidContainer fc, boolean testOnly, CraftRecipeData.CacheData cacheData) {
        if (output.getFluid() == null) {
            return false;
        }
        if (fc != null && !fc.isFull()) {
            float stored = fc.getAmount();
            if (stored > 0.0f && output.hasFlag(OutputFlag.ForceEmpty)) {
                stored = 0.0f;
                if (!testOnly) {
                    fc.Empty();
                }
            }
            if (stored > 0.0f && output.hasFlag(OutputFlag.IsEmpty)) {
                return false;
            }
            float amount = PZMath.min(output.getAmount(), fc.getCapacity() - stored);
            if (output.hasFlag(OutputFlag.AlwaysFill)) {
                amount = fc.getCapacity() - stored;
            }
            if (amount <= 0.0f) {
                return false;
            }
            if (amount < output.getAmount() && output.hasFlag(OutputFlag.RespectCapacity)) {
                return false;
            }
            if (!fc.canAddFluid(output.getFluid())) {
                return false;
            }
            if (!testOnly && output.getChance() < 1.0f && Rand.Next(0.0f, 1.0f) > output.getChance()) {
                return true;
            }
            if (!testOnly) {
                fc.addFluid(output.getFluid(), amount);
            }
            if (cacheData != null) {
                cacheData.fluidCreated = amount;
            }
            return true;
        }
        return false;
    }

    private static boolean createOutputEnergy(OutputScript output, Resource resource, boolean testOnly, CraftRecipeData.CacheData cacheData) {
        if (!CraftRecipeManager.validateOutputScript(output, ResourceType.Energy, testOnly)) {
            return false;
        }
        if (resource == null || resource.isFull()) {
            return false;
        }
        ResourceEnergy resourceEnergy = (ResourceEnergy)resource;
        if (resourceEnergy.getEnergy() == null || !resourceEnergy.getEnergy().equals(output.getEnergy())) {
            return false;
        }
        float stored = resource.getEnergyAmount();
        if (stored > 0.0f && output.hasFlag(OutputFlag.ForceEmpty)) {
            stored = 0.0f;
            if (!testOnly) {
                resourceEnergy.setEnergyAmount(0.0f);
            }
        }
        if (stored > 0.0f && output.hasFlag(OutputFlag.IsEmpty)) {
            return false;
        }
        float amount = PZMath.min(output.getAmount(), resourceEnergy.getEnergyCapacity() - stored);
        if (output.hasFlag(OutputFlag.AlwaysFill)) {
            amount = resourceEnergy.getEnergyCapacity() - stored;
        }
        if (amount < 0.0f) {
            return false;
        }
        if (amount < output.getAmount() && output.hasFlag(OutputFlag.RespectCapacity)) {
            return false;
        }
        if (!testOnly && output.getChance() < 1.0f && Rand.Next(0.0f, 1.0f) > output.getChance()) {
            return true;
        }
        if (!testOnly) {
            resourceEnergy.setEnergyAmount(resource.getEnergyAmount() + amount);
        }
        if (cacheData != null) {
            cacheData.energyCreated = amount;
        }
        return true;
    }

    private static boolean createOutputEnergyToItemInternal(OutputScript output, InventoryItem targetItem, boolean testOnly, CraftRecipeData.CacheData cacheData) {
        if (targetItem instanceof DrainableComboItem) {
            DrainableComboItem comboItem = (DrainableComboItem)targetItem;
            if (!comboItem.isEnergy() || comboItem.getEnergy() == null || !comboItem.getEnergy().equals(output.getEnergy())) {
                return false;
            }
            int stored = comboItem.getCurrentUses();
            if (stored > 0 && output.hasFlag(OutputFlag.ForceEmpty)) {
                stored = 0;
                if (!testOnly) {
                    comboItem.setCurrentUses(0);
                }
            }
            if (stored > 0 && output.hasFlag(OutputFlag.IsEmpty)) {
                return false;
            }
            int createAmount = output.getAmount() > 0.0f ? (int)output.getAmount() : 1;
            int amount = PZMath.min(createAmount, comboItem.getMaxUses() - stored);
            if (output.hasFlag(OutputFlag.AlwaysFill)) {
                amount = comboItem.getMaxUses() - stored;
            }
            if (amount <= 0) {
                return false;
            }
            if (amount < createAmount && output.hasFlag(OutputFlag.RespectCapacity)) {
                return false;
            }
            if (!testOnly && output.getChance() < 1.0f && Rand.Next(0.0f, 1.0f) > output.getChance()) {
                return true;
            }
            if (!testOnly) {
                comboItem.setCurrentUses(PZMath.min(comboItem.getCurrentUses() + amount, comboItem.getMaxUses()));
            }
            if (cacheData != null) {
                cacheData.energyCreated = amount;
            }
            return true;
        }
        return false;
    }

    public static ArrayList<CraftRecipe> getUniqueRecipeItems(InventoryItem item, IsoGameCharacter chr, ArrayList<ItemContainer> containers) {
        RecipeList.clear();
        List<CraftRecipe> allRecipes = CraftRecipeManager.queryRecipes(CraftRecipeTag.IN_HAND_CRAFT, CraftRecipeTag.ANY_SURFACE_CRAFT);
        HandcraftLogic logic = new HandcraftLogic(chr, null, null);
        logic.setIsoObject(logic.findCraftSurface(chr, 2));
        logic.setContainers(containers);
        for (int i = 0; i < allRecipes.size(); ++i) {
            CraftRecipe recipe = allRecipes.get(i);
            if (!CraftRecipeManager.isValidRecipeForCharacter(recipe, chr, null, containers) || CraftRecipeManager.getValidInputScriptForItem(recipe, item, chr) == null || !recipe.OnTestItem(item, chr)) continue;
            logic.setRecipeFromContextClick(recipe, item);
            if (!logic.canPerformCurrentRecipe()) continue;
            RecipeList.add(recipe);
        }
        return RecipeList;
    }

    static {
        playerCraftDataMap = new HashMap<IsoPlayer, CraftRecipeData>();
        RecipeList = new ArrayList();
    }

    private static class CraftRecipeListProvider
    implements TaggedObjectManager.BackingListProvider<CraftRecipe> {
        private CraftRecipeListProvider() {
        }

        @Override
        public ArrayList<CraftRecipe> getTaggedObjectList() {
            return ScriptManager.instance.getAllCraftRecipes();
        }
    }

    public static enum FilterMode {
        Name,
        ModName,
        Tags,
        InputName,
        OutputName;

    }
}

