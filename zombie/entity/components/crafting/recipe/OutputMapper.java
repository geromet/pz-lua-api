/*
 * Decompiled with CFR 0.152.
 */
package zombie.entity.components.crafting.recipe;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import zombie.UsedFromLua;
import zombie.entity.components.crafting.recipe.CraftRecipeData;
import zombie.scripting.ScriptManager;
import zombie.scripting.entity.components.crafting.CraftRecipe;
import zombie.scripting.entity.components.crafting.InputScript;
import zombie.scripting.objects.Item;
import zombie.util.StringUtils;

@UsedFromLua
public class OutputMapper {
    private static final ArrayList<Item> _emptyItems = new ArrayList();
    private final ArrayList<Item> resultItems = new ArrayList();
    private final ArrayList<OutputEntree> entrees = new ArrayList();
    private final HashMap<Item, OutputEntree> entreeMap = new HashMap();
    private OutputEntree defaultOutputEntree;
    private final ArrayList<InputScript> inputScripts = new ArrayList();
    private final HashSet<InputScript> matchedInputs = new HashSet();
    private final String name;

    public OutputMapper(String name) {
        this.name = name;
    }

    public boolean isEmpty() {
        return this.defaultOutputEntree == null && this.entrees.isEmpty();
    }

    private void clear() {
        this.resultItems.clear();
        this.entrees.clear();
        this.entreeMap.clear();
        this.defaultOutputEntree = null;
        this.inputScripts.clear();
    }

    public ArrayList<Item> getResultItems() {
        return this.resultItems;
    }

    public ArrayList<Item> getPatternForResult(Item result) {
        if (this.entreeMap.containsKey(result)) {
            return this.entreeMap.get((Object)result).pattern;
        }
        return _emptyItems;
    }

    public void registerInputScript(InputScript inputScript) {
        this.inputScripts.add(inputScript);
    }

    public void setDefaultOutputEntree(String item) {
        this.defaultOutputEntree = new OutputEntree();
        this.defaultOutputEntree.fullType = item;
    }

    public void addOutputEntree(String result, String[] items) {
        OutputEntree entree = new OutputEntree();
        entree.fullType = result;
        entree.patternFullTypes.addAll(Arrays.asList(items));
        this.entrees.add(entree);
    }

    public void addOutputEntree(String result, ArrayList<String> items) {
        OutputEntree entree = new OutputEntree();
        entree.fullType = result;
        entree.patternFullTypes.addAll(items);
        this.entrees.add(entree);
    }

    private Item getItem(String fullType) throws Exception {
        return this.getItem(fullType, null);
    }

    private Item getItem(String fullType, String recipe) throws Exception {
        if (StringUtils.isNullOrWhitespace(fullType)) {
            throw new Exception(recipe + " item not found, full type invalid: " + fullType);
        }
        Item item = ScriptManager.instance.getItem(fullType);
        if (item == null) {
            throw new Exception(recipe + " item not found: " + fullType);
        }
        if (recipe != null && ScriptManager.instance.getCraftRecipe(recipe) != null) {
            CraftRecipe craftRecipe = ScriptManager.instance.getCraftRecipe(recipe);
            if (craftRecipe.getCategory() != null && !craftRecipe.getCategory().equalsIgnoreCase("packing") && !craftRecipe.getCategory().equalsIgnoreCase("ammunition")) {
                item.setIsCraftRecipeProduct();
            } else if (craftRecipe.getCategory() == null) {
                item.setIsCraftRecipeProduct();
            }
            if (craftRecipe.canBeResearched()) {
                item.addResearchableRecipe(craftRecipe);
            }
        }
        return item;
    }

    public void OnPostWorldDictionaryInit() throws Exception {
        this.OnPostWorldDictionaryInit(null);
    }

    public void OnPostWorldDictionaryInit(String recipe) throws Exception {
        if (this.defaultOutputEntree != null) {
            this.defaultOutputEntree.result = this.getItem(this.defaultOutputEntree.fullType, recipe);
            this.resultItems.add(this.defaultOutputEntree.result);
        }
        for (int i = 0; i < this.entrees.size(); ++i) {
            Item item;
            OutputEntree entree = this.entrees.get(i);
            entree.result = item = this.getItem(entree.fullType, recipe);
            this.resultItems.add(entree.result);
            this.entreeMap.put(item, entree);
            for (int j = 0; j < entree.patternFullTypes.size(); ++j) {
                Item patternItem = this.getItem(entree.patternFullTypes.get(j));
                entree.pattern.add(patternItem);
            }
        }
        assert (!this.resultItems.isEmpty());
    }

    public Item getOutputItem(CraftRecipeData recipeData) {
        return this.getOutputItem(recipeData, false);
    }

    public Item getOutputItem(CraftRecipeData recipeData, boolean testManualInputs) {
        if (!this.entrees.isEmpty() && recipeData != null) {
            for (int i = 0; i < this.entrees.size(); ++i) {
                OutputEntree entree = this.entrees.get(i);
                if (entree.pattern.isEmpty()) continue;
                this.matchedInputs.clear();
                boolean isMatch = true;
                for (int j = 0; j < entree.pattern.size(); ++j) {
                    Item patternItem = entree.pattern.get(j);
                    boolean isItemMatch = false;
                    for (int k = 0; k < this.inputScripts.size(); ++k) {
                        InputScript inputScript = this.inputScripts.get(k);
                        if (this.matchedInputs.contains(inputScript) || !this.matchItem(recipeData, inputScript, patternItem, testManualInputs)) continue;
                        this.matchedInputs.add(inputScript);
                        isItemMatch = true;
                        break;
                    }
                    if (isItemMatch) continue;
                    isMatch = false;
                    break;
                }
                if (!isMatch) continue;
                return entree.result;
            }
        }
        if (this.defaultOutputEntree != null) {
            if (testManualInputs) {
                for (int k = 0; k < this.inputScripts.size(); ++k) {
                    InputScript inputScript = this.inputScripts.get(k);
                    if (recipeData.getFirstManualInputFor(inputScript) != null) continue;
                    return null;
                }
            }
            return this.defaultOutputEntree.result;
        }
        return null;
    }

    private boolean matchItem(CraftRecipeData recipeData, InputScript inputScript, Item item, boolean testManualInputs) {
        CraftRecipeData.InputScriptData data = recipeData.getDataForInputScript(inputScript);
        if (data != null) {
            if (testManualInputs) {
                if (data.getFirstInputItem() != null && data.getFirstInputItem().getScriptItem() != null) {
                    return data.getFirstInputItem().getScriptItem() != null && data.getFirstInputItem().getScriptItem().equals(item);
                }
            } else if (data.getMostRecentItem() != null) {
                return data.getMostRecentItem().getScriptItem() != null && data.getMostRecentItem().getScriptItem().equals(item);
            }
        }
        return false;
    }

    public ArrayList<OutputEntree> getEntrees() {
        return this.entrees;
    }

    public static class OutputEntree {
        private String fullType;
        public Item result;
        private final ArrayList<String> patternFullTypes = new ArrayList();
        public final ArrayList<Item> pattern = new ArrayList();
    }
}

