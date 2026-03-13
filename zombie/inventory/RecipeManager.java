/*
 * Decompiled with CFR 0.152.
 */
package zombie.inventory;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.Stack;
import zombie.Lua.LuaEventManager;
import zombie.Lua.LuaManager;
import zombie.UsedFromLua;
import zombie.characters.IsoGameCharacter;
import zombie.characters.skills.PerkFactory;
import zombie.core.Core;
import zombie.debug.DebugLog;
import zombie.entity.ComponentType;
import zombie.entity.components.fluids.Fluid;
import zombie.inventory.InventoryItem;
import zombie.inventory.ItemContainer;
import zombie.inventory.recipemanager.ItemRecipe;
import zombie.inventory.recipemanager.RecipeMonitor;
import zombie.inventory.types.DrainableComboItem;
import zombie.inventory.types.Food;
import zombie.inventory.types.Moveable;
import zombie.scripting.ScriptManager;
import zombie.scripting.objects.EvolvedRecipe;
import zombie.scripting.objects.Item;
import zombie.scripting.objects.MovableRecipe;
import zombie.scripting.objects.Recipe;
import zombie.scripting.objects.ScriptModule;
import zombie.util.StringUtils;

@UsedFromLua
public class RecipeManager {
    private static final ArrayList<Recipe> RecipeList = new ArrayList();

    public static void ScriptsLoaded() {
        ArrayList<Recipe> recipes = ScriptManager.instance.getAllRecipes();
        HashSet<String> reported = new HashSet<String>();
        for (Recipe recipe : recipes) {
            for (Recipe.Source source2 : recipe.getSource()) {
                for (int i = 0; i < source2.getItems().size(); ++i) {
                    String sourceType = source2.getItems().get(i);
                    if (sourceType.startsWith("Fluid.")) {
                        source2.getItems().set(i, sourceType);
                        continue;
                    }
                    if ("Water".equals(sourceType) || sourceType.contains(".") || sourceType.startsWith("[")) continue;
                    Item scriptItem = RecipeManager.resolveItemModuleDotType(recipe, sourceType, reported, "recipe source");
                    if (scriptItem == null) {
                        source2.getItems().set(i, "???." + sourceType);
                        continue;
                    }
                    source2.getItems().set(i, scriptItem.getFullName());
                }
            }
            if (recipe.getResults().isEmpty()) continue;
            for (Recipe.Result result : recipe.getResults()) {
                if (result.getModule() != null) continue;
                Item scriptItem = RecipeManager.resolveItemModuleDotType(recipe, result.getType(), reported, "recipe result");
                if (scriptItem == null) {
                    result.module = "???";
                    continue;
                }
                result.module = scriptItem.getModule().getName();
            }
        }
    }

    private static Item resolveItemModuleDotType(Recipe recipe, String sourceType, Set<String> reported, String errorMsg) {
        ScriptModule module = recipe.getModule();
        Item scriptItem = module.getItem(sourceType);
        if (scriptItem != null && !scriptItem.getObsolete()) {
            return scriptItem;
        }
        for (int i = 0; i < ScriptManager.instance.moduleList.size(); ++i) {
            ScriptModule module1 = ScriptManager.instance.moduleList.get(i);
            scriptItem = module1.getItem(sourceType);
            if (scriptItem == null || scriptItem.getObsolete()) continue;
            String moduleName = recipe.getModule().getName();
            if (!reported.contains(moduleName)) {
                reported.add(moduleName);
                DebugLog.Recipe.warn("WARNING: module \"%s\" may have forgot to import module Base", moduleName);
            }
            return scriptItem;
        }
        DebugLog.Recipe.warn("ERROR: can't find %s \"%s\" in recipe \"%s\"", errorMsg, sourceType, recipe.getOriginalname());
        return null;
    }

    public static void LoadedAfterLua() {
        ArrayList<Item> scriptItems = new ArrayList<Item>();
        ArrayList<Recipe> recipes = ScriptManager.instance.getAllRecipes();
        for (int i = 0; i < recipes.size(); ++i) {
            Recipe recipe = recipes.get(i);
            DebugLog.Recipe.debugln("Checking Recipe " + recipe.name);
            for (Recipe.Source source2 : recipe.getSource()) {
                ScriptManager.resolveGetItemTypes(source2.getItems(), scriptItems);
            }
        }
        scriptItems.clear();
    }

    private static void testLuaFunction(Recipe recipe, String functionName, String varName) {
        if (StringUtils.isNullOrWhitespace(functionName)) {
            return;
        }
        Object functionObject = LuaManager.getFunctionObject(functionName);
        if (functionObject == null) {
            DebugLog.Recipe.error("no such function %s = \"%s\" in recipe \"%s\"", varName, functionName, recipe.name);
        }
    }

    public static int getKnownRecipesNumber(IsoGameCharacter chr) {
        int result = 0;
        ArrayList<Recipe> recipes = ScriptManager.instance.getAllRecipes();
        for (int i = 0; i < recipes.size(); ++i) {
            Recipe recipe = recipes.get(i);
            if (!chr.isRecipeKnown(recipe)) continue;
            ++result;
        }
        return result;
    }

    public static ArrayList<Recipe> getUniqueRecipeItems(InventoryItem item, IsoGameCharacter chr, ArrayList<ItemContainer> containers) {
        Recipe recipe;
        RecipeList.clear();
        ArrayList<Recipe> allRecipes = ScriptManager.instance.getAllRecipes();
        for (int i = 0; i < allRecipes.size(); ++i) {
            recipe = allRecipes.get(i);
            if (!RecipeManager.IsRecipeValid(recipe, chr, item, containers)) continue;
            RecipeList.add(recipe);
        }
        if (item instanceof Moveable) {
            Moveable moveable = (Moveable)item;
            if (RecipeList.isEmpty() && moveable.getWorldSprite() != null) {
                if (item.type != null && item.type.equalsIgnoreCase(moveable.getWorldSprite())) {
                    recipe = new MovableRecipe();
                    LuaEventManager.triggerEvent("OnDynamicMovableRecipe", moveable.getWorldSprite(), recipe, item, chr);
                    if (((MovableRecipe)recipe).isValid() && RecipeManager.IsRecipeValid(recipe, chr, item, containers)) {
                        RecipeList.add(recipe);
                    }
                } else {
                    DebugLog.Recipe.warn("RecipeManager -> Cannot create recipe for this movable item: " + item.getFullType());
                }
            }
        }
        return RecipeList;
    }

    public static boolean IsRecipeValid(Recipe recipe, IsoGameCharacter chr, InventoryItem item, ArrayList<ItemContainer> containers) {
        if (Core.debug) {
            // empty if block
        }
        if (recipe.result == null) {
            return false;
        }
        if (!chr.isRecipeKnown(recipe)) {
            return false;
        }
        if (item != null && !RecipeManager.validateRecipeContainsSourceItem(recipe, item)) {
            return false;
        }
        if (!RecipeManager.validateHasAllRequiredItems(recipe, chr, item, containers)) {
            return false;
        }
        if (!RecipeManager.validateHasRequiredSkill(recipe, chr)) {
            return false;
        }
        if (!RecipeManager.validateNearIsoObject(recipe, chr)) {
            return false;
        }
        if (!RecipeManager.validateHasHeat(recipe, item, containers, chr)) {
            return false;
        }
        for (Recipe.Source source2 : recipe.getSource()) {
            if (source2.keep) continue;
            boolean exist = false;
            for (String itemType : source2.getItems()) {
                if (containers == null) {
                    for (InventoryItem item1 : chr.getInventory().getItems()) {
                        if (item1.getFullType().equals(itemType) && RecipeManager.validateCanPerform(recipe, chr, item1)) {
                            exist = true;
                            continue;
                        }
                        if (!itemType.startsWith("Fluid.") || !RecipeManager.validateCanPerform(recipe, chr, item1)) continue;
                        String fluidType = itemType.substring(6);
                        Fluid fluid = Fluid.Get(fluidType);
                        if (!item1.hasComponent(ComponentType.FluidContainer) || !item1.getFluidContainer().contains(fluid) || !(item1.getFluidContainer().getAmount() >= source2.use)) continue;
                        exist = true;
                    }
                } else {
                    for (ItemContainer container : containers) {
                        for (InventoryItem item1 : container.getItems()) {
                            if (item1.getFullType().equals(itemType) && RecipeManager.validateCanPerform(recipe, chr, item1)) {
                                exist = true;
                                continue;
                            }
                            if (!itemType.startsWith("Fluid.") || !RecipeManager.validateCanPerform(recipe, chr, item1)) continue;
                            String fluidType = itemType.substring(6);
                            Fluid fluid = Fluid.Get(fluidType);
                            if (!item1.hasComponent(ComponentType.FluidContainer) || !item1.getFluidContainer().contains(fluid) || !(item1.getFluidContainer().getAmount() >= source2.use)) continue;
                            exist = true;
                        }
                        if (!exist) continue;
                    }
                }
                if (!exist) continue;
            }
            if (exist) continue;
            return false;
        }
        return true;
    }

    public static void printDebugRecipeValid(Recipe recipe, IsoGameCharacter chr, InventoryItem item, ArrayList<ItemContainer> containers) {
        Object s;
        if (RecipeMonitor.canLog()) {
            RecipeMonitor.LogBlanc();
        }
        if (RecipeMonitor.canLog()) {
            RecipeMonitor.Log("[DebugTestRecipeValid]", RecipeMonitor.colHeader);
        }
        if (RecipeMonitor.canLog()) {
            RecipeMonitor.IncTab();
        }
        boolean valid = true;
        if (recipe.result == null) {
            s = "invalid: recipe result is null.";
            DebugLog.Recipe.warn(s);
            if (RecipeMonitor.canLog()) {
                RecipeMonitor.Log((String)s, RecipeMonitor.colNeg);
            }
            valid = false;
        }
        if (!chr.isRecipeKnown(recipe)) {
            s = "invalid: recipe not known.";
            DebugLog.Recipe.warn(s);
            if (RecipeMonitor.canLog()) {
                RecipeMonitor.Log((String)s, RecipeMonitor.colNeg);
            }
            valid = false;
        }
        if (item != null && !RecipeManager.validateRecipeContainsSourceItem(recipe, item)) {
            s = "invalid: recipe does not contain source item.";
            DebugLog.Recipe.warn(s);
            if (RecipeMonitor.canLog()) {
                RecipeMonitor.Log((String)s, RecipeMonitor.colNeg);
            }
            valid = false;
        }
        if (!RecipeManager.validateHasAllRequiredItems(recipe, chr, item, containers)) {
            s = "invalid: recipe does not have all required items.";
            DebugLog.Recipe.warn(s);
            if (RecipeMonitor.canLog()) {
                RecipeMonitor.Log((String)s, RecipeMonitor.colNeg);
            }
            valid = false;
        }
        if (!RecipeManager.validateHasRequiredSkill(recipe, chr)) {
            s = "invalid: character does not have required skill.";
            DebugLog.Recipe.warn(s);
            if (RecipeMonitor.canLog()) {
                RecipeMonitor.Log((String)s, RecipeMonitor.colNeg);
            }
            valid = false;
        }
        if (!RecipeManager.validateNearIsoObject(recipe, chr)) {
            s = "invalid: recipe is not near required IsoObject.";
            DebugLog.Recipe.warn(s);
            if (RecipeMonitor.canLog()) {
                RecipeMonitor.Log((String)s, RecipeMonitor.colNeg);
            }
            valid = false;
        }
        if (!RecipeManager.validateHasHeat(recipe, item, containers, chr)) {
            s = "invalid: recipe heat validation failed.";
            DebugLog.Recipe.warn(s);
            if (RecipeMonitor.canLog()) {
                RecipeMonitor.Log((String)s, RecipeMonitor.colNeg);
            }
            valid = false;
        }
        if (!RecipeManager.validateCanPerform(recipe, chr, item)) {
            s = "invalid: recipe can perform failed.";
            DebugLog.Recipe.warn(s);
            if (RecipeMonitor.canLog()) {
                RecipeMonitor.Log((String)s, RecipeMonitor.colNeg);
            }
            valid = false;
        }
        s = "recipe overall valid: " + valid;
        DebugLog.Recipe.println((String)s);
        if (RecipeMonitor.canLog()) {
            RecipeMonitor.DecTab();
        }
        if (RecipeMonitor.canLog()) {
            RecipeMonitor.Log((String)s, valid ? RecipeMonitor.colPos : RecipeMonitor.colNeg);
        }
    }

    private static boolean validateNearIsoObject(Recipe recipe, IsoGameCharacter chr) {
        return false;
    }

    private static boolean validateCanPerform(Recipe recipe, IsoGameCharacter chr, InventoryItem item) {
        return false;
    }

    private static boolean validateHasRequiredSkill(Recipe recipe, IsoGameCharacter chr) {
        if (recipe.getRequiredSkillCount() > 0) {
            for (int i = 0; i < recipe.getRequiredSkillCount(); ++i) {
                Recipe.RequiredSkill skill = recipe.getRequiredSkill(i);
                if (chr.getPerkLevel(skill.getPerk()) >= skill.getLevel()) continue;
                return false;
            }
        }
        return true;
    }

    public static boolean validateRecipeContainsSourceItem(Recipe recipe, InventoryItem item) {
        for (int i = 0; i < recipe.source.size(); ++i) {
            Recipe.Source source2 = recipe.getSource().get(i);
            for (int j = 0; j < source2.getItems().size(); ++j) {
                String sourceFullType = source2.getItems().get(j);
                if (sourceFullType.startsWith("Fluid.") && item.hasComponent(ComponentType.FluidContainer)) {
                    return true;
                }
                if (!sourceFullType.equals(item.getFullType())) continue;
                return true;
            }
        }
        return false;
    }

    private static boolean validateHasAllRequiredItems(Recipe recipe, IsoGameCharacter chr, InventoryItem selectedItem, ArrayList<ItemContainer> containers) {
        ArrayList<InventoryItem> items = RecipeManager.getAvailableItemsNeeded(recipe, chr, containers, selectedItem, null);
        return !items.isEmpty();
    }

    public static boolean validateHasHeat(Recipe recipe, InventoryItem item, ArrayList<ItemContainer> containers, IsoGameCharacter chr) {
        if (recipe.getHeat() != 0.0f) {
            InventoryItem drainableItem = null;
            for (InventoryItem i : RecipeManager.getAvailableItemsNeeded(recipe, chr, containers, item, null)) {
                if (!(i instanceof DrainableComboItem)) continue;
                drainableItem = i;
                break;
            }
            if (drainableItem != null) {
                for (ItemContainer container : containers) {
                    for (InventoryItem i : container.getItems()) {
                        if (!i.getName().equals(drainableItem.getName()) || !(recipe.getHeat() < 0.0f ? i.getInvHeat() <= recipe.getHeat() : recipe.getHeat() > 0.0f && i.getInvHeat() + 1.0f >= recipe.getHeat())) continue;
                        return true;
                    }
                }
            }
            return false;
        }
        return true;
    }

    public static ArrayList<InventoryItem> getAvailableItemsAll(Recipe recipe, IsoGameCharacter chr, ArrayList<ItemContainer> containers, InventoryItem selectedItem, ArrayList<InventoryItem> ignoreItems) {
        RecipeMonitor.suspend();
        ItemRecipe itemRecipe = ItemRecipe.Alloc(recipe, chr, containers, selectedItem, ignoreItems, true);
        ArrayList<InventoryItem> items = itemRecipe.getSourceItems();
        ItemRecipe.Release(itemRecipe);
        RecipeMonitor.resume();
        return items;
    }

    public static ArrayList<InventoryItem> getAvailableItemsNeeded(Recipe recipe, IsoGameCharacter chr, ArrayList<ItemContainer> containers, InventoryItem selectedItem, ArrayList<InventoryItem> ignoreItems) {
        RecipeMonitor.suspend();
        ItemRecipe itemRecipe = ItemRecipe.Alloc(recipe, chr, containers, selectedItem, ignoreItems, false);
        ArrayList<InventoryItem> items = itemRecipe.getSourceItems();
        ItemRecipe.Release(itemRecipe);
        RecipeMonitor.resume();
        return items;
    }

    public static ArrayList<InventoryItem> getSourceItemsAll(Recipe recipe, int sourceIndex, IsoGameCharacter chr, ArrayList<ItemContainer> containers, InventoryItem selectedItem, ArrayList<InventoryItem> ignoreItems) {
        RecipeMonitor.suspend();
        ItemRecipe itemRecipe = ItemRecipe.Alloc(recipe, chr, containers, selectedItem, ignoreItems, true);
        ArrayList<InventoryItem> items = itemRecipe.getSourceItems(sourceIndex);
        ItemRecipe.Release(itemRecipe);
        RecipeMonitor.resume();
        return items;
    }

    public static ArrayList<InventoryItem> getSourceItemsNeeded(Recipe recipe, int sourceIndex, IsoGameCharacter chr, ArrayList<ItemContainer> containers, InventoryItem selectedItem, ArrayList<InventoryItem> ignoreItems) {
        RecipeMonitor.suspend();
        ItemRecipe itemRecipe = ItemRecipe.Alloc(recipe, chr, containers, selectedItem, ignoreItems, false);
        ArrayList<InventoryItem> items = itemRecipe.getSourceItems(sourceIndex);
        ItemRecipe.Release(itemRecipe);
        RecipeMonitor.resume();
        return items;
    }

    public static int getNumberOfTimesRecipeCanBeDone(Recipe recipe, IsoGameCharacter chr, ArrayList<ItemContainer> containers, InventoryItem selectedItem) {
        return ItemRecipe.getNumberOfTimesRecipeCanBeDone(recipe, chr, containers, selectedItem);
    }

    public static ArrayList<InventoryItem> PerformMakeItem(Recipe recipe, InventoryItem selectedItem, IsoGameCharacter chr, ArrayList<ItemContainer> containers) {
        RecipeMonitor.StartMonitor();
        RecipeMonitor.setRecipe(recipe);
        ItemRecipe itemRecipe = ItemRecipe.Alloc(recipe, chr, containers, selectedItem, null, false);
        ArrayList<InventoryItem> results = itemRecipe.perform();
        ItemRecipe.Release(itemRecipe);
        return results;
    }

    public static ArrayList<EvolvedRecipe> getAllEvolvedRecipes() {
        Stack<EvolvedRecipe> allRecipes = ScriptManager.instance.getAllEvolvedRecipes();
        ArrayList<EvolvedRecipe> result = new ArrayList<EvolvedRecipe>();
        for (int i = 0; i < allRecipes.size(); ++i) {
            result.add((EvolvedRecipe)allRecipes.get(i));
        }
        return result;
    }

    public static ArrayList<EvolvedRecipe> getEvolvedRecipe(InventoryItem baseItem, IsoGameCharacter chr, ArrayList<ItemContainer> containers, boolean need1ingredient) {
        Food food;
        ArrayList<EvolvedRecipe> result = new ArrayList<EvolvedRecipe>();
        if (baseItem instanceof Food && (food = (Food)baseItem).isRotten() && chr.getPerkLevel(PerkFactory.Perks.Cooking) < 7) {
            return result;
        }
        Stack<EvolvedRecipe> allRecipes = ScriptManager.instance.getAllEvolvedRecipes();
        for (int i = 0; i < allRecipes.size(); ++i) {
            EvolvedRecipe recipe = (EvolvedRecipe)allRecipes.get(i);
            if (baseItem.isCooked() && !recipe.addIngredientIfCooked || !baseItem.getFullType().equals(recipe.baseItem) && !baseItem.getFullType().equals(recipe.resultItem) || baseItem.getType().equals("WaterPot") && (double)baseItem.getCurrentUsesFloat() < 0.75) continue;
            if (need1ingredient) {
                Food food2;
                ArrayList<InventoryItem> items = recipe.getItemsCanBeUse(chr, baseItem, containers);
                if (items.isEmpty()) continue;
                if (baseItem instanceof Food && (food2 = (Food)baseItem).isFrozen()) {
                    if (!recipe.isAllowFrozenItem()) continue;
                    result.add(recipe);
                    continue;
                }
                result.add(recipe);
                continue;
            }
            result.add(recipe);
        }
        return result;
    }

    public static Recipe getDismantleRecipeFor(String item) {
        RecipeList.clear();
        ArrayList<Recipe> allRecipes = ScriptManager.instance.getAllRecipes();
        for (int i = 0; i < allRecipes.size(); ++i) {
            Recipe recipe = allRecipes.get(i);
            ArrayList<Recipe.Source> sources = recipe.getSource();
            if (sources.isEmpty()) continue;
            for (int k = 0; k < sources.size(); ++k) {
                Recipe.Source source2 = sources.get(k);
                for (int m = 0; m < source2.getItems().size(); ++m) {
                    if (!source2.getItems().get(m).equalsIgnoreCase(item) || !recipe.name.toLowerCase().startsWith("dismantle ")) continue;
                    return recipe;
                }
            }
        }
        return null;
    }

    public static InventoryItem GetMovableRecipeTool(boolean isPrimary, Recipe recipe, InventoryItem selectedItem, IsoGameCharacter chr, ArrayList<ItemContainer> containers) {
        Recipe.Source source2;
        if (!(recipe instanceof MovableRecipe)) {
            return null;
        }
        MovableRecipe movableRecipe = (MovableRecipe)recipe;
        Recipe.Source source3 = source2 = isPrimary ? movableRecipe.getPrimaryTools() : movableRecipe.getSecondaryTools();
        if (source2 == null || source2.getItems() == null || source2.getItems().isEmpty()) {
            return null;
        }
        RecipeMonitor.suspend();
        ItemRecipe itemRecipe = ItemRecipe.Alloc(recipe, chr, containers, selectedItem, null, false);
        if (itemRecipe.getSourceItems() == null || itemRecipe.getSourceItems().isEmpty()) {
            ItemRecipe.Release(itemRecipe);
            RecipeMonitor.resume();
            return null;
        }
        ArrayList<InventoryItem> items = itemRecipe.getSourceItems();
        ItemRecipe.Release(itemRecipe);
        RecipeMonitor.resume();
        for (int i = 0; i < items.size(); ++i) {
            InventoryItem item = items.get(i);
            for (int j = 0; j < source2.getItems().size(); ++j) {
                if (!item.getFullType().equalsIgnoreCase(source2.getItems().get(j))) continue;
                return item;
            }
        }
        return null;
    }

    public static boolean HasAllRequiredItems(Recipe recipe, IsoGameCharacter chr, InventoryItem selectedItem, ArrayList<ItemContainer> containers) {
        return RecipeManager.validateHasAllRequiredItems(recipe, chr, selectedItem, containers);
    }

    public static boolean isAllItemsUsableRotten(Recipe recipe, IsoGameCharacter chr, InventoryItem selectedItem, ArrayList<ItemContainer> containers) {
        if (chr.getPerkLevel(PerkFactory.Perks.Cooking) >= 7) {
            return true;
        }
        ArrayList<InventoryItem> items = RecipeManager.getAvailableItemsNeeded(recipe, chr, containers, selectedItem, null);
        for (InventoryItem item : items) {
            Food food;
            if (!(item instanceof Food) || !(food = (Food)item).isRotten()) continue;
            return false;
        }
        return true;
    }

    public static boolean hasHeat(Recipe recipe, InventoryItem item, ArrayList<ItemContainer> containers, IsoGameCharacter chr) {
        return RecipeManager.validateHasHeat(recipe, item, containers, chr);
    }

    private static void DebugPrintAllRecipes() {
    }

    @Deprecated
    public static boolean IsItemDestroyed(String itemToUse, Recipe recipe) {
        DebugLog.Recipe.error("Method is deprecated.");
        return false;
    }

    @Deprecated
    public static float UseAmount(String sourceFullType, Recipe recipe, IsoGameCharacter chr) {
        DebugLog.Recipe.error("Method is deprecated.");
        return 0.0f;
    }

    @Deprecated
    public static boolean DoesWipeUseDelta(String itemToUse, String itemToMake) {
        DebugLog.Recipe.error("Method is deprecated.");
        return true;
    }

    @Deprecated
    public static boolean DoesUseItemUp(String itemToUse, Recipe recipe) {
        DebugLog.Recipe.error("Method is deprecated.");
        return false;
    }
}

