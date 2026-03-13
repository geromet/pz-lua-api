/*
 * Decompiled with CFR 0.152.
 */
package zombie.entity.components.crafting;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import se.krka.kahlua.integration.LuaReturn;
import se.krka.kahlua.vm.KahluaTable;
import zombie.Lua.LuaManager;
import zombie.UsedFromLua;
import zombie.characters.IsoGameCharacter;
import zombie.characters.IsoPlayer;
import zombie.core.Core;
import zombie.debug.DebugLog;
import zombie.entity.components.crafting.CraftBench;
import zombie.entity.components.crafting.CraftMode;
import zombie.entity.components.crafting.recipe.CraftRecipeData;
import zombie.entity.components.crafting.recipe.CraftRecipeListNodeCollection;
import zombie.entity.components.crafting.recipe.CraftRecipeManager;
import zombie.entity.components.crafting.recipe.InputItemNode;
import zombie.entity.components.crafting.recipe.InputItemNodeCollection;
import zombie.entity.components.fluids.Fluid;
import zombie.entity.components.fluids.FluidContainer;
import zombie.entity.components.fluids.FluidType;
import zombie.entity.components.resources.Resource;
import zombie.entity.components.resources.ResourceType;
import zombie.inventory.InventoryItem;
import zombie.inventory.ItemContainer;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoObject;
import zombie.iso.objects.IsoThumpable;
import zombie.iso.objects.IsoWorldInventoryObject;
import zombie.scripting.entity.components.crafting.CraftBenchScript;
import zombie.scripting.entity.components.crafting.CraftRecipe;
import zombie.scripting.entity.components.crafting.InputScript;
import zombie.scripting.entity.components.crafting.OutputScript;
import zombie.scripting.objects.CraftRecipeTag;
import zombie.scripting.objects.Item;
import zombie.ui.UIManager;
import zombie.util.StringUtils;
import zombie.util.list.PZArrayUtil;

@UsedFromLua
public abstract class BaseCraftingLogic {
    protected final IsoGameCharacter player;
    protected final CraftBench craftBench;
    protected IsoObject isoObject;
    protected final HashMap<String, ArrayList<CraftEventHandler>> events = new HashMap();
    protected final ArrayList<InventoryItem> allItems = new ArrayList();
    protected final ArrayList<ItemContainer> containers = new ArrayList();
    protected final ArrayList<Resource> sourceResources = new ArrayList();
    protected String filterString;
    protected String categoryFilterString;
    protected final ArrayList<CraftRecipe> completeRecipeList = new ArrayList();
    protected final CraftRecipeListNodeCollection filteredRecipeList = new CraftRecipeListNodeCollection();
    protected final CraftRecipeData recipeData;
    protected final CraftRecipeData testRecipeData;
    private boolean manualSelectInputs;
    private boolean showManualSelectInputs;
    protected InputScript manualSelectInputScriptFilter;
    protected final HashSet<String> manualInputAllowedItemTypes = new HashSet();
    protected int cachedPossibleCraftCount;
    private final boolean limitAutoFillToCurrentlyFilledItems = true;
    private final ArrayList<Resource> multicraftConsumedResources = new ArrayList();
    private final ArrayList<InventoryItem> multicraftConsumedItems = new ArrayList();
    protected InputItemNodeCollection inputItemNodeCollection = new InputItemNodeCollection(true, false);
    protected final CachedRecipeComparator cachedRecipeComparator;
    protected final ArrayList<CachedRecipeInfo> cachedRecipeInfos = new ArrayList();
    protected final HashMap<CraftRecipe, CachedRecipeInfo> cachedRecipeInfoMap = new HashMap();
    protected boolean cachedRecipeInfosDirty;
    protected boolean cachedCanPerform;
    protected boolean cachedCanPerformDirty = true;
    protected float targetVariableInputRatio = 1.0f;
    private static final ThreadLocal<HashMap<String, String>> TL_getFavouriteModDataString = ThreadLocal.withInitial(HashMap::new);

    public BaseCraftingLogic(IsoGameCharacter player, CraftBench craftBench) {
        this.player = player;
        this.craftBench = craftBench;
        this.cachedRecipeComparator = new CachedRecipeComparator(this);
        this.inputItemNodeCollection.setCharacter(player);
        this.testRecipeData = new CraftRecipeData(CraftMode.Handcraft, craftBench != null, true, false, true);
        this.testRecipeData.setCharacter(player);
        this.recipeData = new CraftRecipeData(CraftMode.Handcraft, craftBench != null, true, false, true);
        this.recipeData.setCharacter(player);
        this.registerEvent("onShowManualSelectChanged");
        this.registerEvent("onRebuildInputItemNodes");
        this.registerEvent("onManualSelectChanged");
        this.registerEvent("onInputsChanged");
        this.registerEvent("onUpdateRecipeList");
        this.registerEvent("onSetRecipeList");
        this.registerEvent("onUpdateContainers");
    }

    public ArrayList<String> getCategoryList() {
        ArrayList<String> categories = new ArrayList<String>();
        for (int i = 0; i < this.completeRecipeList.size(); ++i) {
            if (categories.contains(this.completeRecipeList.get(i).getCategory())) continue;
            categories.add(this.completeRecipeList.get(i).getCategory());
        }
        categories.sort(String::compareToIgnoreCase);
        return categories;
    }

    protected void registerEvent(String eventName) {
        this.events.put(eventName, new ArrayList());
    }

    public void addEventListener(String event, Object function) {
        this.addEventListener(event, function, null);
    }

    public void addEventListener(String event, Object function, Object targetTable) {
        if (this.events.containsKey(event)) {
            this.events.get(event).add(new CraftEventHandler(function, targetTable));
            if (Core.debug && this.events.get(event).size() > 10) {
                throw new RuntimeException("Sanity check, event '" + event + "' has >10 listeners");
            }
        } else {
            DebugLog.General.warn("Event '" + event + "' is unknown.");
        }
    }

    protected void triggerEvent(String event, Object ... args2) {
        if (this.events.containsKey(event)) {
            ArrayList<CraftEventHandler> handlers = this.events.get(event);
            for (int i = 0; i < handlers.size(); ++i) {
                CraftEventHandler handler = handlers.get(i);
                try {
                    if (handler.targetTable != null) {
                        Object[] params = new Object[args2.length + 1];
                        System.arraycopy(args2, 0, params, 1, args2.length);
                        params[0] = handler.targetTable;
                        LuaManager.caller.protectedCallVoid(UIManager.getDefaultThread(), handler.function, params);
                        continue;
                    }
                    LuaManager.caller.protectedCallVoid(UIManager.getDefaultThread(), handler.function, args2);
                    continue;
                }
                catch (Exception e) {
                    e.printStackTrace();
                }
            }
        } else {
            DebugLog.General.warn("Event '" + event + "' is unknown.");
        }
    }

    public void filterRecipeList(String filter, String categoryFilter) {
        this.filterRecipeList(filter, categoryFilter, false);
    }

    public void filterRecipeList(String filter, String categoryFilter, boolean force) {
        this.filterRecipeList(filter, categoryFilter, force, LuaManager.GlobalObject.getSpecificPlayer(0));
    }

    public void filterRecipeList(String filter, String categoryFilter, boolean force, IsoPlayer player) {
        boolean filterChanged;
        ArrayList<Object> recipesKnown = new ArrayList();
        if (player == null) {
            player = LuaManager.GlobalObject.getSpecificPlayer(0);
        }
        if (player == null) {
            recipesKnown = this.completeRecipeList;
        } else {
            for (int i = 0; i < this.completeRecipeList.size(); ++i) {
                CraftRecipe testRecipe = this.completeRecipeList.get(i);
                if (testRecipe.needToBeLearn() && !CraftRecipeManager.hasPlayerLearnedRecipe(testRecipe, player)) continue;
                recipesKnown.add(testRecipe);
            }
        }
        boolean categoryChanged = this.categoryFilterString != null && !this.categoryFilterString.equals(categoryFilter) || categoryFilter != null && !categoryFilter.equals(this.categoryFilterString);
        boolean bl = filterChanged = this.filterString != null && !this.filterString.equals(filter) || filter != null && !filter.equals(this.filterString);
        if (categoryChanged || filterChanged || force) {
            this.filterString = filter;
            this.categoryFilterString = categoryFilter;
            this.rebuildCachedRecipeInfo();
            BaseCraftingLogic.filterAndSortRecipeList(filter, this.categoryFilterString, this.filteredRecipeList, recipesKnown, player, this.cachedRecipeComparator);
            this.filteredRecipeList.setInitialExpandedStates(this, player.isBuildCheat());
            this.triggerEvent("onUpdateRecipeList", this.filteredRecipeList);
        }
    }

    private static FilterStringMatchType getFilterStringMatchType(String filterString, String recipeName, String[] filterStringParts, boolean matchExactly) {
        if (recipeName.equals(filterString)) {
            return FilterStringMatchType.EXACT;
        }
        if (matchExactly) {
            return FilterStringMatchType.NONE;
        }
        if (recipeName.startsWith(filterString)) {
            return FilterStringMatchType.STARTSWITH;
        }
        String[] recipeNameParts = recipeName.split(" ");
        boolean allPartsExactMatch = true;
        boolean allPartsPartialMatch = true;
        for (int i = 0; i < filterStringParts.length; ++i) {
            boolean wordMatchExact = false;
            boolean wordMatchPartial = false;
            for (int j = 0; j < recipeNameParts.length; ++j) {
                if (recipeNameParts[j].startsWith(filterStringParts[i])) {
                    wordMatchPartial = true;
                }
                if (!recipeNameParts[j].equals(filterStringParts[i])) continue;
                wordMatchExact = true;
                break;
            }
            if (!wordMatchExact) {
                allPartsExactMatch = false;
            }
            if (wordMatchPartial) continue;
            allPartsPartialMatch = false;
            break;
        }
        if (allPartsExactMatch) {
            return FilterStringMatchType.PARTS_ALL_EXACT;
        }
        if (allPartsPartialMatch) {
            return FilterStringMatchType.PARTS_ALL_STARTWITH;
        }
        if (recipeName.contains(filterString)) {
            return FilterStringMatchType.CONTAINS;
        }
        return FilterStringMatchType.NONE;
    }

    public static CraftRecipeListNodeCollection filterAndSortRecipeList(String filterString, String categoryFilterString, CraftRecipeListNodeCollection listToPopulate, List<CraftRecipe> sourceList, IsoPlayer player, Comparator<CraftRecipe> sortComparator) {
        CraftRecipe recipe;
        List<CraftRecipe> categoryFilteredSourceList;
        if (listToPopulate != null) {
            listToPopulate.clear();
        }
        if (sourceList == null || listToPopulate == null) {
            DebugLog.General.error("one of list parameters is null.");
            return listToPopulate;
        }
        if (StringUtils.isNullOrWhitespace(filterString) && StringUtils.isNullOrWhitespace(categoryFilterString)) {
            listToPopulate.addAll(sourceList);
            listToPopulate.sort(sortComparator);
            return listToPopulate;
        }
        if (!StringUtils.isNullOrWhitespace(categoryFilterString)) {
            categoryFilteredSourceList = new ArrayList<CraftRecipe>();
            for (int i = 0; i < sourceList.size(); ++i) {
                recipe = sourceList.get(i);
                if (recipe == null) continue;
                if (categoryFilterString.equals("*")) {
                    if (!player.isFavouriteRecipe(recipe)) continue;
                    categoryFilteredSourceList.add(recipe);
                    continue;
                }
                if (!recipe.getCategory().equalsIgnoreCase(categoryFilterString)) continue;
                categoryFilteredSourceList.add(recipe);
            }
        } else {
            categoryFilteredSourceList = sourceList;
        }
        boolean fullName = false;
        if (!StringUtils.isNullOrWhitespace(filterString)) {
            CraftRecipeManager.FilterMode filterMode = CraftRecipeManager.FilterMode.Name;
            if (filterString.startsWith("!")) {
                filterString = filterString.replaceFirst("!", "");
                fullName = true;
            }
            if (filterString.startsWith("@")) {
                filterMode = CraftRecipeManager.FilterMode.ModName;
                filterString = filterString.substring(1);
            } else if (filterString.startsWith("$")) {
                filterMode = CraftRecipeManager.FilterMode.Tags;
                filterString = filterString.substring(1);
            } else if (filterString.contains("-@-")) {
                String[] split = filterString.split("-@-");
                filterString = split[0];
                if ("InputName".equalsIgnoreCase(split[1])) {
                    filterMode = CraftRecipeManager.FilterMode.InputName;
                }
                if ("OutputName".equalsIgnoreCase(split[1])) {
                    filterMode = CraftRecipeManager.FilterMode.OutputName;
                }
            }
            FluidType fluidSearch = FluidType.FromNameLower(filterString.toLowerCase());
            filterString = filterString.toLowerCase();
            String[] filterStringParts = filterString.split(" ");
            ArrayList<CraftRecipe> resultsExact = new ArrayList<CraftRecipe>();
            ArrayList<CraftRecipe> resultsStartsWith = new ArrayList<CraftRecipe>();
            ArrayList<CraftRecipe> resultsAllPartExact = new ArrayList<CraftRecipe>();
            ArrayList<CraftRecipe> resultsAllPartStartsWith = new ArrayList<CraftRecipe>();
            ArrayList<CraftRecipe> resultsContains = new ArrayList<CraftRecipe>();
            if (filterMode == CraftRecipeManager.FilterMode.Tags) {
                int i;
                ArrayList<String> approvedTags = new ArrayList<String>();
                HashMap<String, FilterStringMatchType> approvedTagMatchType = new HashMap<String, FilterStringMatchType>();
                for (i = 0; i < CraftRecipeManager.getAllRecipeTags().size(); ++i) {
                    String tag = CraftRecipeManager.getAllRecipeTags().get(i);
                    FilterStringMatchType matchType = BaseCraftingLogic.getFilterStringMatchType(filterString, CraftRecipeManager.getAllRecipeTags().get(i), filterStringParts, false);
                    if (matchType == FilterStringMatchType.NONE) continue;
                    approvedTags.add(tag);
                    approvedTagMatchType.put(tag, matchType);
                }
                block43: for (i = 0; i < categoryFilteredSourceList.size(); ++i) {
                    recipe = categoryFilteredSourceList.get(i);
                    FilterStringMatchType bestMatch = FilterStringMatchType.NONE;
                    for (int j = 0; j < approvedTags.size(); ++j) {
                        FilterStringMatchType matchType;
                        String tag = (String)approvedTags.get(i);
                        if (recipe == null || !recipe.hasTag(CraftRecipeTag.fromValue(tag)) || (matchType = (FilterStringMatchType)((Object)approvedTagMatchType.get(tag))).compareTo(bestMatch) >= 0) continue;
                        bestMatch = matchType;
                    }
                    switch (bestMatch.ordinal()) {
                        case 0: {
                            resultsExact.add(recipe);
                            continue block43;
                        }
                        case 1: {
                            resultsStartsWith.add(recipe);
                            continue block43;
                        }
                        case 2: {
                            resultsAllPartExact.add(recipe);
                            continue block43;
                        }
                        case 3: {
                            resultsAllPartStartsWith.add(recipe);
                            continue block43;
                        }
                        case 4: {
                            resultsContains.add(recipe);
                        }
                    }
                }
            } else {
                block45: for (int i = 0; i < categoryFilteredSourceList.size(); ++i) {
                    recipe = categoryFilteredSourceList.get(i);
                    block7 : switch (filterMode) {
                        case Name: {
                            if (recipe == null || recipe.getTranslationName() == null) continue block45;
                            FilterStringMatchType matchType = BaseCraftingLogic.getFilterStringMatchType(filterString, recipe.getTranslationName().toLowerCase(), filterStringParts, false);
                            switch (matchType.ordinal()) {
                                case 0: {
                                    resultsExact.add(recipe);
                                    break;
                                }
                                case 1: {
                                    resultsStartsWith.add(recipe);
                                    break;
                                }
                                case 2: {
                                    resultsAllPartExact.add(recipe);
                                    break;
                                }
                                case 3: {
                                    resultsAllPartStartsWith.add(recipe);
                                    break;
                                }
                                case 4: {
                                    resultsContains.add(recipe);
                                }
                            }
                            continue block45;
                        }
                        case InputName: {
                            FilterStringMatchType matchType;
                            Item item;
                            CraftRecipe.IOScript input;
                            if (recipe == null) continue block45;
                            FilterStringMatchType bestMatch = FilterStringMatchType.NONE;
                            if (fluidSearch != null) {
                                for (CraftRecipe.IOScript ioLine : recipe.getIoLines()) {
                                    InputScript inputScript;
                                    if (!(ioLine instanceof InputScript) || (inputScript = (InputScript)ioLine).getResourceType() != ResourceType.Fluid) continue;
                                    for (Fluid possibleInputFluid : inputScript.getPossibleInputFluids()) {
                                        FilterStringMatchType matchType2 = BaseCraftingLogic.getFilterStringMatchType(filterString, possibleInputFluid.getTranslatedName().toLowerCase(), filterStringParts, fullName);
                                        if (matchType2.compareTo(bestMatch) >= 0) continue;
                                        bestMatch = matchType2;
                                    }
                                }
                            } else {
                                for (int x = 0; x < recipe.getInputs().size(); ++x) {
                                    input = recipe.getInputs().get(x);
                                    for (int j = 0; j < ((InputScript)input).getPossibleInputItems().size(); ++j) {
                                        item = ((InputScript)input).getPossibleInputItems().get(j);
                                        matchType = BaseCraftingLogic.getFilterStringMatchType(filterString, fullName ? item.getFullName().toLowerCase() : item.getDisplayName().toLowerCase(), filterStringParts, fullName);
                                        if (matchType.compareTo(bestMatch) >= 0) continue;
                                        bestMatch = matchType;
                                    }
                                }
                            }
                            switch (bestMatch.ordinal()) {
                                case 0: {
                                    resultsExact.add(recipe);
                                    break;
                                }
                                case 1: {
                                    resultsStartsWith.add(recipe);
                                    break;
                                }
                                case 2: {
                                    resultsAllPartExact.add(recipe);
                                    break;
                                }
                                case 3: {
                                    resultsAllPartStartsWith.add(recipe);
                                    break;
                                }
                                case 4: {
                                    resultsContains.add(recipe);
                                }
                            }
                            continue block45;
                        }
                        case OutputName: {
                            FilterStringMatchType matchType;
                            Item item;
                            CraftRecipe.IOScript input;
                            if (recipe == null) continue block45;
                            FilterStringMatchType bestMatch = FilterStringMatchType.NONE;
                            for (int x = 0; x < recipe.getOutputs().size(); ++x) {
                                input = recipe.getOutputs().get(x);
                                if (fluidSearch != null) {
                                    for (CraftRecipe.IOScript ioLine : recipe.getIoLines()) {
                                        OutputScript outputScript;
                                        if (!(ioLine instanceof OutputScript) || (outputScript = (OutputScript)ioLine).getResourceType() != ResourceType.Fluid) continue;
                                        for (Fluid possibleInputFluid : outputScript.getPossibleResultFluids()) {
                                            FilterStringMatchType matchType3 = BaseCraftingLogic.getFilterStringMatchType(filterString, possibleInputFluid.getTranslatedName().toLowerCase(), filterStringParts, fullName);
                                            if (matchType3.compareTo(bestMatch) >= 0) continue;
                                            bestMatch = matchType3;
                                        }
                                    }
                                    continue;
                                }
                                if (((OutputScript)input).getOutputMapper() == null || ((OutputScript)input).getOutputMapper().getResultItems() == null) continue;
                                for (int j = 0; j < ((OutputScript)input).getOutputMapper().getResultItems().size(); ++j) {
                                    item = ((OutputScript)input).getOutputMapper().getResultItems().get(j);
                                    matchType = BaseCraftingLogic.getFilterStringMatchType(filterString, fullName ? item.getFullName().toLowerCase() : item.getDisplayName().toLowerCase(), filterStringParts, fullName);
                                    if (matchType.compareTo(bestMatch) >= 0) continue;
                                    bestMatch = matchType;
                                }
                            }
                            switch (bestMatch.ordinal()) {
                                case 0: {
                                    resultsExact.add(recipe);
                                    break;
                                }
                                case 1: {
                                    resultsStartsWith.add(recipe);
                                    break;
                                }
                                case 2: {
                                    resultsAllPartExact.add(recipe);
                                    break;
                                }
                                case 3: {
                                    resultsAllPartStartsWith.add(recipe);
                                    break;
                                }
                                case 4: {
                                    resultsContains.add(recipe);
                                }
                            }
                            continue block45;
                        }
                        case ModName: {
                            if (recipe == null || recipe.getModName() == null) continue block45;
                            FilterStringMatchType matchType = BaseCraftingLogic.getFilterStringMatchType(filterString, recipe.getModName().toLowerCase(), filterStringParts, false);
                            switch (matchType.ordinal()) {
                                case 0: {
                                    resultsExact.add(recipe);
                                    break block7;
                                }
                                case 1: {
                                    resultsStartsWith.add(recipe);
                                    break block7;
                                }
                                case 2: {
                                    resultsAllPartExact.add(recipe);
                                    break block7;
                                }
                                case 3: {
                                    resultsAllPartStartsWith.add(recipe);
                                    break block7;
                                }
                                case 4: {
                                    resultsContains.add(recipe);
                                }
                            }
                        }
                    }
                }
            }
            resultsExact.sort(sortComparator);
            resultsStartsWith.sort(sortComparator);
            resultsAllPartExact.sort(sortComparator);
            resultsAllPartStartsWith.sort(sortComparator);
            resultsContains.sort(sortComparator);
            listToPopulate.addAll(resultsExact);
            listToPopulate.addAll(resultsStartsWith);
            listToPopulate.addAll(resultsAllPartExact);
            listToPopulate.addAll(resultsAllPartStartsWith);
            listToPopulate.addAll(resultsContains);
        } else {
            listToPopulate.addAll(categoryFilteredSourceList);
            listToPopulate.sort(sortComparator);
        }
        return listToPopulate;
    }

    public void sortRecipeList() {
        this.filterRecipeList(this.filterString, this.categoryFilterString, true);
    }

    protected void setSortModeInternal(String sortMode) {
        switch (sortMode) {
            case "LastUsed": {
                this.cachedRecipeComparator.compareMode = CachedRecipeComparator.CompareMode.LAST_USED;
                break;
            }
            case "MostUsed": {
                this.cachedRecipeComparator.compareMode = CachedRecipeComparator.CompareMode.MOST_USED;
                break;
            }
            default: {
                this.cachedRecipeComparator.compareMode = CachedRecipeComparator.CompareMode.NAME;
            }
        }
    }

    public void setRecipe(CraftRecipe recipe) {
        this.recipeData.setRecipe(recipe);
        this.recipeData.canConsumeInputs(this.sourceResources, this.allItems, true, true);
        this.inputItemNodeCollection.setRecipe(recipe);
        this.manualInputAllowedItemTypes.clear();
        this.autoPopulateInputs();
        this.triggerEvent("onRecipeChanged", recipe);
        this.triggerEvent("onRebuildInputItemNodes", new Object[0]);
    }

    public void setRecipes(List<CraftRecipe> recipes) {
        this.completeRecipeList.clear();
        this.filteredRecipeList.clear();
        PZArrayUtil.addAll(this.completeRecipeList, recipes);
        this.cachedRecipeInfosDirty = true;
        this.filterRecipeList(this.filterString, this.categoryFilterString, true);
        this.triggerEvent("onSetRecipeList", this.filteredRecipeList);
    }

    public CraftRecipe getRecipe() {
        return this.recipeData.getRecipe();
    }

    public boolean setContainers(ArrayList<ItemContainer> containersToUse) {
        HashSet<InventoryItem> oldItems = new HashSet<InventoryItem>(this.allItems);
        this.containers.clear();
        PZArrayUtil.addAll(this.containers, containersToUse);
        this.allItems.clear();
        CraftRecipeManager.getAllItemsFromContainers(this.containers, this.allItems);
        boolean hasChanged = false;
        if (this.allItems.size() != oldItems.size()) {
            hasChanged = true;
        } else {
            for (InventoryItem item : this.allItems) {
                if (oldItems.contains(item)) continue;
                hasChanged = true;
                break;
            }
        }
        if (hasChanged) {
            ArrayList<InventoryItem> currentRecipeInputs = this.recipeData.getAllInputItems();
            this.recipeData.canConsumeInputs(this.sourceResources, this.allItems, true, true);
            for (int i = currentRecipeInputs.size() - 1; i >= 0; --i) {
                if (this.allItems.contains(currentRecipeInputs.get(i))) continue;
                currentRecipeInputs.remove(i);
            }
            this.populateInputs(currentRecipeInputs, this.sourceResources, true);
            this.inputItemNodeCollection.setItems(this.allItems);
            this.cachedRecipeInfosDirty = true;
            this.cachedCanPerformDirty = true;
            this.triggerEvent("onUpdateContainers", new Object[0]);
            this.triggerEvent("onRebuildInputItemNodes", new Object[0]);
        }
        return hasChanged;
    }

    public ArrayList<ItemContainer> getContainers() {
        return this.containers;
    }

    public void refresh() {
        if (this.getRecipe() == null) {
            return;
        }
        for (int i = 0; i < this.getRecipe().getInputs().size(); ++i) {
            InputScript inputScript = this.getRecipe().getInputs().get(i);
            CraftRecipeData.InputScriptData data = this.recipeData.getDataForInputScript(inputScript);
            data.verifyInputItems(this.allItems);
        }
        ArrayList<InventoryItem> currentRecipeInputs = this.recipeData.getAllInputItems();
        this.recipeData.canConsumeInputs(this.sourceResources, this.allItems, true, true);
        this.populateInputs(currentRecipeInputs, this.sourceResources, true);
        this.autoPopulateInputs();
    }

    public void setTargetVariableInputRatio(float target) {
        this.recipeData.setTargetVariableInputRatio(target);
        this.cachedCanPerformDirty = true;
    }

    public void clearTargetVariableInputRatio() {
        this.recipeData.clearTargetVariableInputRatio();
        this.cachedCanPerformDirty = true;
    }

    public float getVariableInputRatio() {
        return this.recipeData.getVariableInputRatio();
    }

    public static String getFavouriteModDataString(CraftRecipe recipe) {
        if (recipe != null) {
            return BaseCraftingLogic.getFavouriteModDataString(recipe.getName());
        }
        return null;
    }

    public static String getFavouriteModDataString(String recipe) {
        if (recipe != null) {
            return TL_getFavouriteModDataString.get().computeIfAbsent(recipe, s -> "recipeFavourite:" + s);
        }
        return null;
    }

    protected void setSelectedRecipeStyle(String panel, String style) {
        String modString = panel + "RecipeView";
        this.player.getModData().rawset(modString, (Object)style);
    }

    protected String getSelectedRecipeStyle(String panel) {
        String modString = panel + "RecipeView";
        return (String)this.player.getModData().rawget(modString);
    }

    protected void setRecipeSortMode(String panel, String sortMode) {
        String modString = panel + "RecipeSort";
        this.player.getModData().rawset(modString, (Object)sortMode);
        this.setSortModeInternal(sortMode);
    }

    protected String getRecipeSortMode(String panel) {
        String modString = panel + "RecipeSort";
        String sortMode = (String)this.player.getModData().rawget(modString);
        if (sortMode == null) {
            sortMode = "RecipeName";
        }
        return sortMode;
    }

    protected void setLastSelectedRecipe(String panel, CraftRecipe recipe) {
        String modString = panel + "LastRecipe";
        String recipeString = recipe == null ? null : recipe.getScriptObjectFullType();
        this.player.getModData().rawset(modString, (Object)recipeString);
    }

    protected CraftRecipe getLastSelectedRecipe(String panel) {
        return null;
    }

    protected void setLastManualInputMode(String panel, boolean manualInputs) {
        String modString = panel + "ManualInputs";
        this.player.getModData().rawset(modString, (Object)(manualInputs ? "true" : "false"));
    }

    protected boolean getLastManualInputMode(String panel) {
        String modString = panel + "ManualInputs";
        Object modDataValue = this.player.getModData().rawget(modString);
        if (modDataValue instanceof String) {
            String modDataStringValue = (String)modDataValue;
            return modDataStringValue.equals("true");
        }
        return false;
    }

    public static KahluaTable callLuaObject(String func, Object params) {
        Object obj;
        LuaReturn luaReturn;
        Object functionObject = LuaManager.getFunctionObject(func);
        if (functionObject != null && (luaReturn = LuaManager.caller.protectedCall(LuaManager.thread, functionObject, params)).isSuccess() && !luaReturn.isEmpty() && (obj = luaReturn.getFirst()) instanceof KahluaTable) {
            KahluaTable kahluaTable = (KahluaTable)obj;
            return kahluaTable;
        }
        return null;
    }

    public static boolean callLuaBool(String func, Object params) {
        Object functionObject = LuaManager.getFunctionObject(func);
        if (functionObject != null) {
            Boolean luaResult = LuaManager.caller.protectedCallBoolean(LuaManager.thread, functionObject, params);
            return luaResult == Boolean.TRUE;
        }
        return false;
    }

    public static void callLua(String func, Object params) {
        Object functionObject = LuaManager.getFunctionObject(func);
        if (functionObject != null) {
            LuaManager.caller.protectedCallVoid(LuaManager.thread, functionObject, params);
        }
    }

    public static void callLua(String func, Object params, Object params2) {
        Object functionObject = LuaManager.getFunctionObject(func);
        if (functionObject != null) {
            LuaManager.caller.protectedCallVoid(LuaManager.thread, functionObject, params, params2);
        }
    }

    public static void callLua(String func, Object params, Object params2, Object params3) {
        Object functionObject = LuaManager.getFunctionObject(func);
        if (functionObject != null) {
            LuaManager.caller.protectedCallVoid(LuaManager.thread, functionObject, params, params2, params3);
        }
    }

    public String getModelHandOne() {
        return this.recipeData.getModelHandOne();
    }

    public String getModelHandTwo() {
        return this.recipeData.getModelHandTwo();
    }

    public boolean isContainersAccessible(List<ItemContainer> containers) {
        for (int i = 0; i < containers.size(); ++i) {
            IsoThumpable thumpable;
            IsoObject isoObject;
            ItemContainer container = containers.get(i);
            ItemContainer outer = container.getOutermostContainer();
            if (outer.getVehiclePart() != null && outer.getVehiclePart().getVehicle() != null && !outer.getVehiclePart().getVehicle().canAccessContainer(outer.getVehiclePart().getIndex(), this.player)) {
                DebugLog.CraftLogic.debugln("Can't craft: can't access vehicle container");
                return false;
            }
            if (outer.getVehiclePart() == null && outer.getSquare() != null && outer.getSquare().DistToProper(this.player) > 2.5f) {
                DebugLog.CraftLogic.debugln("Can't craft: container too far");
                return false;
            }
            if (outer.getParent() == null || !((isoObject = outer.getParent()) instanceof IsoThumpable) || !(thumpable = (IsoThumpable)isoObject).isLockedToCharacter(this.player)) continue;
            DebugLog.CraftLogic.debugln("Can't craft: thumpable container is locked to the character");
            return false;
        }
        return true;
    }

    public boolean updateFloorContainer(ArrayList<ItemContainer> containers) {
        IsoGridSquare sq = this.player.square;
        HashSet<InventoryItem> oldItems = new HashSet<InventoryItem>(this.allItems);
        if (sq != null) {
            ItemContainer floorContainer = null;
            for (int i = 0; i < containers.size(); ++i) {
                ItemContainer container = containers.get(i);
                if (!container.getType().equals("floor")) continue;
                floorContainer = container;
                floorContainer.clear();
                break;
            }
            for (int x = sq.getX() - 1; x <= sq.getX() + 1; ++x) {
                for (int y = sq.getY() - 1; y <= sq.getY() + 1; ++y) {
                    IsoGridSquare sqToCheck = sq.getCell().getGridSquare(x, y, sq.z);
                    if (sqToCheck == null) continue;
                    for (int i = 0; i < sqToCheck.getWorldObjects().size(); ++i) {
                        IsoWorldInventoryObject wo = sqToCheck.getWorldObjects().get(i);
                        if (wo == null || wo.getItem() == null) continue;
                        if (floorContainer == null) {
                            floorContainer = new ItemContainer("floor", sq, null);
                            containers.add(floorContainer);
                        }
                        floorContainer.getItems().add(wo.getItem());
                    }
                }
            }
        }
        this.allItems.clear();
        CraftRecipeManager.getAllItemsFromContainers(containers, this.allItems);
        if (this.allItems.size() != oldItems.size()) {
            return true;
        }
        for (InventoryItem item : this.allItems) {
            if (oldItems.contains(item)) continue;
            return true;
        }
        return false;
    }

    protected CachedRecipeInfo createCachedRecipeInfo(CraftRecipe recipe, ArrayList<ItemContainer> containers) {
        Objects.requireNonNull(recipe);
        CachedRecipeInfo info = CachedRecipeInfo.Alloc(recipe);
        info.isValid = CraftRecipeManager.isValidRecipeForCharacter(recipe, this.player, null, containers);
        this.testRecipeData.setRecipe(recipe);
        info.canPerform = this.testRecipeData.canPerform(this.player, this.sourceResources, this.allItems, false, containers);
        info.available = info.isValid && info.canPerform;
        this.cachedRecipeInfos.add(info);
        this.cachedRecipeInfoMap.put(recipe, info);
        return info;
    }

    protected void rebuildCachedRecipeInfo() {
        int i;
        if (!this.cachedRecipeInfosDirty) {
            return;
        }
        for (i = 0; i < this.cachedRecipeInfos.size(); ++i) {
            CachedRecipeInfo info = this.cachedRecipeInfos.get(i);
            CachedRecipeInfo.Release(info);
        }
        this.cachedRecipeInfos.clear();
        this.cachedRecipeInfoMap.clear();
        for (i = 0; i < this.completeRecipeList.size(); ++i) {
            CraftRecipe recipe = this.completeRecipeList.get(i);
            this.createCachedRecipeInfo(recipe, null);
        }
        this.cachedRecipeInfosDirty = false;
    }

    public CachedRecipeInfo getCachedRecipeInfo(CraftRecipe recipe) {
        if (this.cachedRecipeInfosDirty) {
            this.rebuildCachedRecipeInfo();
        }
        Objects.requireNonNull(recipe);
        CachedRecipeInfo info = this.cachedRecipeInfoMap.get(recipe);
        if (info == null) {
            info = this.createCachedRecipeInfo(recipe, null);
        }
        return info;
    }

    public boolean areAllInputItemsSatisfied() {
        return this.recipeData.areAllInputItemsSatisfied();
    }

    public boolean isCharacterInRangeOfWorkbench() {
        return true;
    }

    public boolean hasRequiredWorkstation() {
        if (this.recipeData.getRecipe() == null) {
            return false;
        }
        if (!this.recipeData.getRecipe().requiresSpecificWorkstation()) {
            return true;
        }
        if (!this.isCharacterInRangeOfWorkbench()) {
            return false;
        }
        if (this.isoObject != null && this.isoObject.getEntityScript() != null && this.isoObject.getEntityScript().getComponentScripts() != null && this.craftBench != null) {
            CraftBenchScript bench = (CraftBenchScript)this.isoObject.getEntityScript().getComponentScriptFor(this.craftBench.getComponentType());
            return bench != null && bench.getRecipes().contains(this.recipeData.getRecipe());
        }
        return false;
    }

    public boolean cachedCanPerformCurrentRecipe() {
        if (this.cachedCanPerformDirty) {
            return this.canPerformCurrentRecipe();
        }
        return this.cachedCanPerform;
    }

    public boolean canPerformCurrentRecipe() {
        this.cachedCanPerformDirty = false;
        this.cachedCanPerform = this.recipeData.canPerform(this.player, this.sourceResources, this.isManualSelectInputs() ? null : this.allItems, true, this.containers);
        if (this.recipeData.getRecipe() != null && this.recipeData.getRecipe().isAnySurfaceCraft() && !this.isCharacterInRangeOfWorkbench()) {
            this.cachedCanPerform = false;
        }
        if (this.recipeData.getRecipe() != null && this.recipeData.getRecipe().requiresSpecificWorkstation() && !this.hasRequiredWorkstation()) {
            this.cachedCanPerform = false;
        }
        return this.cachedCanPerform;
    }

    public boolean isManualSelectInputs() {
        return this.manualSelectInputs;
    }

    public void setManualSelectInputs(boolean b) {
        if (this.manualSelectInputs != b) {
            this.manualSelectInputs = b;
            this.triggerEvent("onManualSelectChanged", this.manualSelectInputs);
        }
    }

    public boolean shouldShowManualSelectInputs() {
        return this.manualSelectInputs && this.showManualSelectInputs;
    }

    public void setShowManualSelectInputs(boolean b) {
        if (this.showManualSelectInputs != b) {
            this.showManualSelectInputs = b;
            this.triggerEvent("onShowManualSelectChanged", this.shouldShowManualSelectInputs());
        }
    }

    public InputScript getManualSelectInputScriptFilter() {
        return this.manualSelectInputScriptFilter;
    }

    public void setManualSelectInputScriptFilter(InputScript script) {
        this.manualSelectInputScriptFilter = script;
        this.triggerEvent("onRebuildInputItemNodes", new Object[0]);
    }

    public void clearManualInputs() {
        this.recipeData.clearManualInputs();
    }

    public void clearManualInputsFor(CraftRecipeData.InputScriptData input) {
        this.recipeData.clearManualInputs(input);
    }

    public boolean setManualInputsFor(InputScript inputScript, ArrayList<InventoryItem> list) {
        if (this.isManualSelectInputs()) {
            return this.recipeData.setManualInputsFor(inputScript, list);
        }
        return false;
    }

    public ArrayList<InventoryItem> getManualInputsFor(InputScript inputScript, ArrayList<InventoryItem> list) {
        if (!this.isManualSelectInputs()) {
            return list;
        }
        return this.recipeData.getManualInputsFor(inputScript, list);
    }

    public void copyManualInputsFrom(BaseCraftingLogic logic) {
        if (logic != null && logic != this && logic.isManualSelectInputs() && logic.getRecipe() == this.getRecipe()) {
            this.setManualSelectInputs(true);
            this.clearManualInputs();
            ArrayList<InputScript> inputScripts = logic.getRecipe().getInputs();
            for (int i = 0; i < inputScripts.size(); ++i) {
                InputScript inputScript = inputScripts.get(i);
                if (inputScript.getResourceType() != ResourceType.Item) continue;
                this.setManualInputsFor(inputScript, logic.getManualInputsFor(inputScript, new ArrayList<InventoryItem>()));
            }
            this.recipeData.canConsumeInputs(this.sourceResources, null, true, false);
            this.cachedPossibleCraftCount = this.getPossibleCraftCount(true);
        }
    }

    public void updateManualInputAllowedItemTypes() {
        this.manualInputAllowedItemTypes.clear();
        if (this.isManualSelectInputs()) {
            this.recipeData.getAppliedInputItemTypes(this.manualInputAllowedItemTypes);
        }
    }

    public void populateInputs(List<InventoryItem> inputItems, List<Resource> resources, boolean clearExisting) {
        if (this.recipeData != null) {
            if (!this.manualInputAllowedItemTypes.isEmpty()) {
                ArrayList<InventoryItem> filteredList = new ArrayList<InventoryItem>();
                for (int i = 0; i < inputItems.size(); ++i) {
                    if (!this.manualInputAllowedItemTypes.contains(inputItems.get(i).getFullType())) continue;
                    filteredList.add(inputItems.get(i));
                }
                inputItems = filteredList;
            }
            this.recipeData.populateInputs(inputItems, resources, clearExisting);
            this.cachedCanPerformDirty = true;
        }
    }

    public void autoPopulateInputs() {
        this.populateInputs(this.getAllViableInputInventoryItems(), this.getAllViableInputResources(), false);
        this.recipeData.canConsumeInputs(this.sourceResources, null, true, false);
        this.cachedPossibleCraftCount = this.getPossibleCraftCount(true);
        this.triggerEvent("onInputsChanged", new Object[0]);
    }

    public ArrayList<InputItemNode> getInputItemNodes() {
        return this.inputItemNodeCollection.getInputItemNodes();
    }

    public ArrayList<InputItemNode> getInputItemNodesForInput(InputScript input) {
        return this.inputItemNodeCollection.getInputItemNodesForInput(input);
    }

    public int getInputCount(InputScript inputScript) {
        return (int)this.getInputUses(inputScript);
    }

    public float getInputUses(InputScript inputScript) {
        if (this.recipeData.getRecipe() == null || !this.recipeData.getRecipe().containsIO(inputScript)) {
            return 0.0f;
        }
        CraftRecipeData.InputScriptData data = this.recipeData.getDataForInputScript(inputScript);
        if (data != null) {
            if (inputScript.getResourceType() == ResourceType.Fluid) {
                return data.getInputItemFluidUses();
            }
            return data.getInputItemUses();
        }
        return 0.0f;
    }

    public boolean isInputSatisfied(InputScript inputScript) {
        if (this.recipeData.getRecipe() == null || !this.recipeData.getRecipe().containsIO(inputScript)) {
            return false;
        }
        CraftRecipeData.InputScriptData data = this.recipeData.getDataForInputScript(inputScript);
        if (inputScript.getResourceType() == ResourceType.Item && this.isManualSelectInputs()) {
            return data.isInputItemsSatisfied();
        }
        return data.isCachedCanConsume();
    }

    public List<Fluid> getSatisfiedInputFluids(InputScript inputScript) {
        ArrayList<Fluid> output = new ArrayList<Fluid>();
        if (this.recipeData.getRecipe() == null || !this.recipeData.getRecipe().containsIO(inputScript)) {
            return output;
        }
        CraftRecipeData.InputScriptData data = this.recipeData.getDataForInputScript(inputScript);
        if (data == null) {
            CraftRecipe parentRecipe = inputScript.getParentRecipe();
            for (int i = 0; i < parentRecipe.getInputs().size(); ++i) {
                if (parentRecipe.getInputs().get(i).getConsumeFromItemScript() != inputScript) continue;
                data = this.recipeData.getDataForInputScript(parentRecipe.getInputs().get(i));
                break;
            }
        }
        if (data == null) {
            return output;
        }
        for (int i = 0; i < data.getAppliedItemsCount(); ++i) {
            FluidContainer fluidContainer = data.getAppliedItem(i).getFluidContainer();
            if (fluidContainer == null || fluidContainer.getPrimaryFluid() == null) continue;
            output.add(fluidContainer.getPrimaryFluid());
        }
        return output;
    }

    public List<Item> getSatisfiedInputItems(InputScript inputScript) {
        ArrayList<Item> output = new ArrayList<Item>();
        if (this.recipeData.getRecipe() == null || !this.recipeData.getRecipe().containsIO(inputScript)) {
            return output;
        }
        CraftRecipeData.InputScriptData data = this.recipeData.getDataForInputScript(inputScript);
        for (int i = 0; i < data.getAppliedItemsCount(); ++i) {
            output.add(data.getAppliedItem(i).getScriptItem());
        }
        return output;
    }

    public List<InventoryItem> getSatisfiedInputInventoryItems(InputScript inputScript) {
        ArrayList<InventoryItem> output = new ArrayList<InventoryItem>();
        if (this.recipeData.getRecipe() == null || !this.recipeData.getRecipe().containsIO(inputScript)) {
            return output;
        }
        CraftRecipeData.InputScriptData data = this.recipeData.getDataForInputScript(inputScript);
        for (int i = 0; i < data.getAppliedItemsCount(); ++i) {
            output.add(data.getAppliedItem(i));
        }
        return output;
    }

    public List<InventoryItem> getAllViableInputInventoryItems() {
        ArrayList<InventoryItem> output = new ArrayList<InventoryItem>();
        if (this.recipeData.getRecipe() == null) {
            return output;
        }
        for (int i = 0; i < this.recipeData.getAllViableItemsCount(); ++i) {
            output.add(this.recipeData.getViableItem(i));
        }
        return output;
    }

    public List<Resource> getAllViableInputResources() {
        ArrayList<Resource> output = new ArrayList<Resource>();
        if (this.recipeData.getRecipe() == null) {
            return output;
        }
        for (int i = 0; i < this.recipeData.getAllViableResourcesCount(); ++i) {
            output.add(this.recipeData.getViableResource(i));
        }
        return output;
    }

    public boolean offerInputItem(InventoryItem item) {
        boolean success;
        if (this.getManualSelectInputScriptFilter() != null) {
            CraftRecipeData.InputScriptData data = this.recipeData.getDataForInputScript(this.getManualSelectInputScriptFilter());
            success = this.recipeData.offerAndReplaceInputItem(data, item);
        } else {
            success = this.recipeData.offerAndReplaceInputItem(item);
        }
        if (success) {
            this.recipeData.canConsumeInputs(this.sourceResources, null, true, false);
            this.cachedPossibleCraftCount = this.getPossibleCraftCount(true);
            this.cachedCanPerformDirty = true;
            this.triggerEvent("onInputsChanged", new Object[0]);
        }
        return success;
    }

    public boolean removeInputItem(InventoryItem item) {
        boolean success = this.recipeData.removeInputItem(item);
        if (success) {
            this.recipeData.canConsumeInputs(this.sourceResources, null, true, false);
            this.cachedPossibleCraftCount = this.getPossibleCraftCount(true);
            this.cachedCanPerformDirty = true;
            this.triggerEvent("onInputsChanged", new Object[0]);
        }
        return success;
    }

    public int getPossibleCraftCount(boolean forceRecache) {
        if (forceRecache) {
            this.cachedPossibleCraftCount = this.recipeData != null ? this.recipeData.getPossibleCraftCount(this.sourceResources, this.allItems, this.multicraftConsumedResources, this.multicraftConsumedItems, this.isManualSelectInputs()) : 0;
        }
        return this.cachedPossibleCraftCount;
    }

    public ArrayList<Resource> getMulticraftConsumedResources() {
        return this.multicraftConsumedResources;
    }

    public ArrayList<InventoryItem> getMulticraftConsumedItems() {
        return this.multicraftConsumedItems;
    }

    public ArrayList<InventoryItem> getMulticraftConsumedItemsFor(InputScript inputScript, ArrayList<InventoryItem> list) {
        int i;
        ArrayList<Item> appliedItemTypes = new ArrayList<Item>();
        CraftRecipeData.InputScriptData inputScriptData = this.recipeData.getDataForInputScript(inputScript);
        for (i = 0; i < inputScriptData.getAppliedItemsCount(); ++i) {
            InventoryItem appliedItem = inputScriptData.getAppliedItem(i);
            if (!appliedItemTypes.contains(appliedItem.getScriptItem())) {
                appliedItemTypes.add(appliedItem.getScriptItem());
            }
            if (!this.multicraftConsumedItems.contains(appliedItem)) continue;
            list.add(appliedItem);
        }
        for (i = 0; i < this.multicraftConsumedItems.size(); ++i) {
            InventoryItem itemToAssess = this.multicraftConsumedItems.get(i);
            if (list.contains(itemToAssess) || !inputScript.canUseItem(itemToAssess, this.player) || !appliedItemTypes.contains(itemToAssess.getScriptItem())) continue;
            list.add(this.multicraftConsumedItems.get(i));
        }
        return list;
    }

    public static class CachedRecipeComparator
    implements Comparator<CraftRecipe> {
        private final BaseCraftingLogic logic;
        public CompareMode compareMode = CompareMode.NAME;

        public CachedRecipeComparator(BaseCraftingLogic logic) {
            this.logic = logic;
        }

        @Override
        public int compare(CraftRecipe v1, CraftRecipe v2) {
            boolean canPerformV2;
            boolean isValidV2;
            IsoPlayer player = (IsoPlayer)this.logic.player;
            boolean isBuildCheat = player.isBuildCheat();
            CachedRecipeInfo infoV1 = this.logic.getCachedRecipeInfo(v1);
            CachedRecipeInfo infoV2 = this.logic.getCachedRecipeInfo(v2);
            boolean isValidV1 = infoV1.isValid() || isBuildCheat;
            boolean bl = isValidV2 = infoV2.isValid() || isBuildCheat;
            if (isValidV1 && !isValidV2) {
                return -1;
            }
            if (!isValidV1 && isValidV2) {
                return 1;
            }
            boolean canPerformV1 = infoV1.isCanPerform() || isBuildCheat;
            boolean bl2 = canPerformV2 = infoV2.isCanPerform() || isBuildCheat;
            if (canPerformV1 && !canPerformV2) {
                return -1;
            }
            if (!canPerformV1 && canPerformV2) {
                return 1;
            }
            int nameCompareResult = v1.getTranslationName().compareTo(v2.getTranslationName());
            switch (this.compareMode.ordinal()) {
                case 1: {
                    double v1LastCraftTime = player.getPlayerCraftHistory().getCraftHistoryFor(v1.getName()).getLastCraftTime();
                    double v2LastCraftTime = player.getPlayerCraftHistory().getCraftHistoryFor(v2.getName()).getLastCraftTime();
                    int lastUsedCompareResult = Double.compare(v2LastCraftTime, v1LastCraftTime);
                    if (lastUsedCompareResult == 0) {
                        return nameCompareResult;
                    }
                    return lastUsedCompareResult;
                }
                case 2: {
                    int v1CraftCount = player.getPlayerCraftHistory().getCraftHistoryFor(v1.getName()).getCraftCount();
                    int v2CraftCount = player.getPlayerCraftHistory().getCraftHistoryFor(v2.getName()).getCraftCount();
                    int mostUsedCompareResult = Integer.compare(v2CraftCount, v1CraftCount);
                    if (mostUsedCompareResult == 0) {
                        return nameCompareResult;
                    }
                    return mostUsedCompareResult;
                }
            }
            return v1.getTranslationName().compareTo(v2.getTranslationName());
        }

        public static enum CompareMode {
            NAME,
            LAST_USED,
            MOST_USED;

        }
    }

    protected static class CraftEventHandler {
        public final Object function;
        public final Object targetTable;

        public CraftEventHandler(Object function, Object targetTable) {
            this.function = Objects.requireNonNull(function);
            this.targetTable = targetTable;
        }
    }

    static enum FilterStringMatchType {
        EXACT,
        STARTSWITH,
        PARTS_ALL_EXACT,
        PARTS_ALL_STARTWITH,
        CONTAINS,
        NONE;

    }

    @UsedFromLua
    public static class CachedRecipeInfo {
        private static final ArrayDeque<CachedRecipeInfo> pool = new ArrayDeque();
        private CraftRecipe recipe;
        private boolean isValid;
        private boolean canPerform;
        private boolean available;

        private static CachedRecipeInfo Alloc(CraftRecipe recipe) {
            CachedRecipeInfo info = pool.poll();
            if (info == null) {
                info = new CachedRecipeInfo();
            }
            info.recipe = recipe;
            return info;
        }

        private static void Release(CachedRecipeInfo info) {
            info.reset();
            assert (!pool.contains(info));
            pool.offer(info);
        }

        public CraftRecipe getRecipe() {
            return this.recipe;
        }

        public boolean isValid() {
            return this.isValid;
        }

        public boolean isCanPerform() {
            return this.canPerform;
        }

        public boolean isAvailable() {
            return this.available;
        }

        public void overrideCanPerform(boolean value) {
            this.canPerform = value;
        }

        private void reset() {
            this.recipe = null;
            this.isValid = false;
            this.canPerform = false;
            this.available = false;
        }
    }
}

