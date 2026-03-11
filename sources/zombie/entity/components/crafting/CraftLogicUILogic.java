/*
 * Decompiled with CFR 0.152.
 */
package zombie.entity.components.crafting;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import se.krka.kahlua.vm.KahluaTable;
import zombie.Lua.LuaManager;
import zombie.UsedFromLua;
import zombie.characters.IsoPlayer;
import zombie.core.Core;
import zombie.core.Translator;
import zombie.core.textures.Texture;
import zombie.debug.DebugLog;
import zombie.entity.ComponentType;
import zombie.entity.GameEntity;
import zombie.entity.components.crafting.BaseCraftingLogic;
import zombie.entity.components.crafting.CraftLogic;
import zombie.entity.components.crafting.recipe.CraftRecipeData;
import zombie.entity.components.crafting.recipe.CraftRecipeListNodeCollection;
import zombie.entity.components.crafting.recipe.CraftRecipeManager;
import zombie.entity.components.crafting.recipe.InputItemNode;
import zombie.entity.components.crafting.recipe.InputItemNodeCollection;
import zombie.entity.components.crafting.recipe.ItemDataList;
import zombie.entity.components.resources.Resource;
import zombie.entity.components.resources.ResourceItem;
import zombie.inventory.InventoryItem;
import zombie.inventory.ItemContainer;
import zombie.scripting.ScriptManager;
import zombie.scripting.entity.GameEntityScript;
import zombie.scripting.entity.components.crafting.CraftRecipe;
import zombie.scripting.entity.components.crafting.CraftRecipeComponentScript;
import zombie.scripting.entity.components.crafting.InputScript;
import zombie.scripting.objects.Item;
import zombie.ui.ObjectTooltip;
import zombie.ui.UIFont;
import zombie.ui.UIManager;

@UsedFromLua
public class CraftLogicUILogic {
    private final IsoPlayer player;
    private final GameEntity entity;
    private final CraftLogic component;
    private CraftRecipe selectedRecipe;
    private final CraftRecipeListNodeCollection filteredRecipeList = new CraftRecipeListNodeCollection();
    private final RecipeComparator recipeComparator;
    private String filterString;
    InputItemNodeCollection inputItemNodeCollection = new InputItemNodeCollection(true, false);
    InputItemNodeCollection resourceItemNodeCollection = new InputItemNodeCollection(false, true);
    protected final HashMap<String, ArrayList<BaseCraftingLogic.CraftEventHandler>> events = new HashMap();
    private boolean showManualSelectInputs;
    private InputScript manualSelectInputScriptFilter;
    private KahluaTable manualSelectItemSlot;
    private boolean cachedCanStart;
    private boolean cachedCanStartDirty = true;
    private int cachedPossibleCraftCount = -1;
    private final List<InventoryItem> selectedInventoryItems = new ArrayList<InventoryItem>();
    private final ArrayList<ItemContainer> containers = new ArrayList();
    protected final ArrayList<InventoryItem> allItems = new ArrayList();

    public CraftLogicUILogic(IsoPlayer player, GameEntity entity, CraftLogic component) {
        this.player = player;
        this.entity = entity;
        this.component = component;
        this.recipeComparator = new RecipeComparator(player);
        this.inputItemNodeCollection.setCharacter(player);
        this.registerEvent("onRecipeChanged");
        this.registerEvent("onRebuildInputItemNodes");
        this.registerEvent("onInputsChanged");
        this.registerEvent("onUpdateRecipeList");
        this.registerEvent("onShowManualSelectChanged");
        this.registerEvent("onResourceSlotContentsChanged");
        this.filterRecipeList(this.filterString, null, true);
        this.selectedRecipe = component.getCurrentRecipe();
        if (this.selectedRecipe == null) {
            this.selectedRecipe = this.filteredRecipeList.getFirstRecipe();
            component.getCraftTestData().setRecipe(this.selectedRecipe);
        }
        this.inputItemNodeCollection.setRecipe(this.selectedRecipe);
        this.resourceItemNodeCollection.setRecipe(this.selectedRecipe);
        this.setSortModeInternal(this.getRecipeSortMode());
    }

    public CraftLogic getCraftLogic() {
        return this.component;
    }

    public GameEntity getEntity() {
        return this.entity;
    }

    public void setRecipe(CraftRecipe recipe) {
        if (recipe == this.selectedRecipe) {
            return;
        }
        this.selectedRecipe = recipe;
        this.component.getCraftTestData().setRecipe(recipe);
        this.inputItemNodeCollection.setRecipe(recipe);
        this.resourceItemNodeCollection.setRecipe(recipe);
        this.cachedCanStartDirty = true;
        this.selectedInventoryItems.clear();
        this.triggerEvent("onRecipeChanged", recipe);
        this.triggerEvent("onRebuildInputItemNodes", new Object[0]);
    }

    public CraftRecipe getRecipe() {
        return this.selectedRecipe;
    }

    public CraftRecipeListNodeCollection getRecipeList() {
        return this.filteredRecipeList;
    }

    public boolean cachedCanStart(IsoPlayer player) {
        if (this.cachedCanStartDirty) {
            this.cachedCanStart = this.component.canStartWithInventoryItems(player, this.selectedInventoryItems);
            this.cachedCanStartDirty = false;
        }
        return this.cachedCanStart;
    }

    private void registerEvent(String eventName) {
        this.events.put(eventName, new ArrayList());
    }

    public void addEventListener(String event, Object function) {
        this.addEventListener(event, function, null);
    }

    public void addEventListener(String event, Object function, Object targetTable) {
        if (this.events.containsKey(event)) {
            this.events.get(event).add(new BaseCraftingLogic.CraftEventHandler(function, targetTable));
            if (Core.debug && this.events.get(event).size() > 10) {
                throw new RuntimeException("Sanity check, event '" + event + "' has >10 listeners");
            }
        } else {
            DebugLog.General.warn("Event '" + event + "' is unknown.");
        }
    }

    protected void triggerEvent(String event, Object ... args2) {
        if (this.events.containsKey(event)) {
            ArrayList<BaseCraftingLogic.CraftEventHandler> handlers = this.events.get(event);
            for (int i = 0; i < handlers.size(); ++i) {
                BaseCraftingLogic.CraftEventHandler handler = handlers.get(i);
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

    public Texture getEntityIcon() {
        GameEntityScript script = ScriptManager.instance.getGameEntityScript(this.entity.getEntityFullTypeDebug());
        CraftRecipeComponentScript recipeScript = (CraftRecipeComponentScript)script.getComponentScriptFor(ComponentType.CraftRecipe);
        return recipeScript.getIconTexture();
    }

    public void setSelectedRecipeStyle(String style) {
        String modString = this.entity.getEntityFullTypeDebug() + "RecipeView";
        this.player.getModData().rawset(modString, (Object)style);
    }

    public String getSelectedRecipeStyle() {
        String modString = this.entity.getEntityFullTypeDebug() + "RecipeView";
        return (String)this.player.getModData().rawget(modString);
    }

    public void setRecipeSortMode(String sortMode) {
        String modString = this.entity.getEntityFullTypeDebug() + "RecipeSort";
        this.player.getModData().rawset(modString, (Object)sortMode);
        this.setSortModeInternal(sortMode);
    }

    public String getRecipeSortMode() {
        String modString = this.entity.getEntityFullTypeDebug() + "RecipeSort";
        String sortMode = (String)this.player.getModData().rawget(modString);
        if (sortMode == null) {
            sortMode = "RecipeName";
        }
        return sortMode;
    }

    protected void setSortModeInternal(String sortMode) {
        switch (sortMode) {
            case "LastUsed": {
                this.recipeComparator.compareMode = RecipeComparator.CompareMode.LAST_USED;
                break;
            }
            case "MostUsed": {
                this.recipeComparator.compareMode = RecipeComparator.CompareMode.MOST_USED;
                break;
            }
            default: {
                this.recipeComparator.compareMode = RecipeComparator.CompareMode.NAME;
            }
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
        ArrayList<CraftRecipe> recipesKnown = new ArrayList<CraftRecipe>();
        if (player == null) {
            player = LuaManager.GlobalObject.getSpecificPlayer(0);
        }
        if (player == null) {
            recipesKnown.addAll(this.component.getRecipes());
        } else {
            for (int i = 0; i < this.component.getRecipes().size(); ++i) {
                CraftRecipe testRecipe = this.component.getRecipes().get(i);
                if (testRecipe.needToBeLearn() && !CraftRecipeManager.hasPlayerLearnedRecipe(testRecipe, player)) continue;
                recipesKnown.add(testRecipe);
            }
        }
        boolean bl = filterChanged = this.filterString != null && !this.filterString.equals(filter) || filter != null && !filter.equals(this.filterString);
        if (filterChanged || force) {
            this.filterString = filter;
            BaseCraftingLogic.filterAndSortRecipeList(filter, null, this.filteredRecipeList, recipesKnown, player, this.recipeComparator);
            this.triggerEvent("onUpdateRecipeList", this.filteredRecipeList);
        }
    }

    public void sortRecipeList() {
        this.filterRecipeList(this.filterString, null, true);
    }

    public int getPossibleCraftCount(boolean forceRecache) {
        if (forceRecache || this.cachedPossibleCraftCount == -1) {
            CraftRecipeData recipeData = this.getCraftLogic().getCraftTestData();
            ArrayList<InventoryItem> selectedItems = this.getRecipeData().getAllInputItems();
            this.cachedPossibleCraftCount = recipeData.getPossibleCraftCount(this.getCraftLogic().getInputResources(), selectedItems, new ArrayList<Resource>(), new ArrayList<InventoryItem>(), false);
            int freeOutputSlots = this.getCraftLogic().getFreeOutputSlotCount();
            this.cachedPossibleCraftCount = Math.min(this.cachedPossibleCraftCount, freeOutputSlots);
        }
        return this.cachedPossibleCraftCount;
    }

    public KahluaTable getItemsInProgress() {
        KahluaTable output = LuaManager.platform.newTable();
        if (this.component.isRunning()) {
            for (CraftRecipeData craftRecipeData : this.component.getAllInProgressCraftData()) {
                ArrayList<InventoryItem> inputItems = craftRecipeData.getAllInputItems();
                for (int i = 0; i < inputItems.size(); ++i) {
                    InventoryItem item = inputItems.get(i);
                    if (item == null) continue;
                    KahluaTable subTable = (KahluaTable)output.rawget(craftRecipeData);
                    if (subTable == null) {
                        subTable = LuaManager.platform.newTable();
                        output.rawset(craftRecipeData, (Object)subTable);
                    }
                    int currentCount = subTable.rawget(item.getScriptItem()) != null ? (Integer)subTable.rawget(item.getScriptItem()) : 0;
                    subTable.rawset(item.getScriptItem(), (Object)(currentCount + 1));
                }
            }
        }
        return output;
    }

    public ArrayList<Texture> getStatusIconsForItemInProgress(InventoryItem item, CraftRecipeData craftRecipeData) {
        if (this.component.isRunning()) {
            return this.component.getStatusIconsForInputItem(item, craftRecipeData);
        }
        return null;
    }

    public KahluaTable getOutputItems() {
        KahluaTable output = LuaManager.platform.newTable();
        if (this.cachedCanStart(this.player)) {
            ItemDataList outputItems = this.component.getCraftTestData().getToOutputItems();
            for (int i = 0; i < outputItems.size(); ++i) {
                Item item = outputItems.getItem(i);
                if (item == null) continue;
                double count = output.rawget(item) != null ? (Double)output.rawget(item) : 0.0;
                output.rawset(item, (Object)(count + 1.0));
            }
        }
        return output;
    }

    public boolean shouldShowManualSelectInputs() {
        return this.showManualSelectInputs;
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

    public KahluaTable getManualSelectItemSlot() {
        return this.manualSelectItemSlot;
    }

    public void setManualSelectInputScriptFilter(InputScript script, KahluaTable itemSlot) {
        this.manualSelectInputScriptFilter = script;
        this.manualSelectItemSlot = itemSlot;
        ArrayList<InventoryItem> storedItems = new ArrayList<InventoryItem>();
        if (itemSlot != null) {
            ResourceItem resource = (ResourceItem)itemSlot.rawget("resource");
            storedItems = resource.getStoredItems();
        }
        this.resourceItemNodeCollection.setItems(storedItems);
        this.triggerEvent("onRebuildInputItemNodes", new Object[0]);
    }

    public CraftRecipeData getRecipeData() {
        return this.getCraftLogic().getCraftTestData();
    }

    public ArrayList<InputItemNode> getInputItemNodes() {
        return this.inputItemNodeCollection.getInputItemNodes();
    }

    public ArrayList<InputItemNode> getInputItemNodesForInput(InputScript input) {
        return this.inputItemNodeCollection.getInputItemNodesForInput(input);
    }

    public ArrayList<InputItemNode> getResourceItemNodes() {
        return this.resourceItemNodeCollection.getInputItemNodes();
    }

    public void onResourceSlotContentsChanged() {
        ArrayList<InventoryItem> storedItems = new ArrayList<InventoryItem>();
        if (this.manualSelectItemSlot != null) {
            ResourceItem resource = (ResourceItem)this.manualSelectItemSlot.rawget("resource");
            storedItems = resource.getStoredItems();
        }
        this.resourceItemNodeCollection.setItems(storedItems);
        this.cachedCanStartDirty = true;
        this.cachedPossibleCraftCount = this.getPossibleCraftCount(true);
        this.triggerEvent("onResourceSlotContentsChanged", new Object[0]);
    }

    public void setCraftQuantity(int quantity) {
    }

    public void setContainers(ArrayList<ItemContainer> containersToUse) {
        this.containers.clear();
        this.containers.addAll(containersToUse);
        this.allItems.clear();
        CraftRecipeManager.getAllItemsFromContainers(this.containers, this.allItems);
        this.inputItemNodeCollection.setItems(this.allItems);
        this.cachedCanStartDirty = true;
        for (int i = this.selectedInventoryItems.size() - 1; i >= 0; --i) {
            InventoryItem inventoryItem = this.selectedInventoryItems.get(i);
            if (this.allItems.contains(inventoryItem)) continue;
            this.getRecipeData().removeInputItem(inventoryItem);
            this.selectedInventoryItems.remove(i);
        }
        this.triggerEvent("onRebuildInputItemNodes", new Object[0]);
    }

    public ArrayList<ItemContainer> getContainers() {
        return this.containers;
    }

    public void doProgressSlotTooltip(KahluaTable itemSlot, ObjectTooltip tooltipUI) {
        boolean offsetY = false;
        tooltipUI.render();
        UIFont font = tooltipUI.getFont();
        int lineSpacing = tooltipUI.getLineSpacing();
        int y = tooltipUI.padTop + 0;
        String s = Translator.getText("EC_CraftLogic_Progress");
        tooltipUI.DrawText(font, s, tooltipUI.padLeft, y, 1.0, 1.0, 0.8f, 1.0);
        tooltipUI.adjustWidth(tooltipUI.padLeft, s);
        y += lineSpacing + 5;
        ObjectTooltip.Layout layout = tooltipUI.beginLayout();
        layout.setMinLabelWidth(80);
        Resource resource = (Resource)itemSlot.rawget("resource");
        CraftRecipeData recipeData = (CraftRecipeData)itemSlot.rawget("craftRecipeData");
        this.getCraftLogic().doProgressTooltip(layout, resource, recipeData);
        y = layout.render(tooltipUI.padLeft, y, tooltipUI);
        tooltipUI.endLayout(layout);
        tooltipUI.setHeight(y += tooltipUI.padBottom);
        if (tooltipUI.getWidth() < 150.0) {
            tooltipUI.setWidth(150.0);
        }
    }

    public void doPreviewSlotTooltip(KahluaTable itemSlot, ObjectTooltip tooltipUI) {
        boolean offsetY = false;
        tooltipUI.render();
        UIFont font = tooltipUI.getFont();
        int lineSpacing = tooltipUI.getLineSpacing();
        Item scriptItem = (Item)itemSlot.rawget("storedScriptItem");
        int y = tooltipUI.padTop + 0;
        String s = scriptItem != null ? scriptItem.getDisplayName() : Translator.getText("EC_CraftLogicTooltip_NoOutput");
        tooltipUI.DrawText(font, s, tooltipUI.padLeft, y, 1.0, 1.0, 0.8f, 1.0);
        tooltipUI.adjustWidth(tooltipUI.padLeft, s);
        y += lineSpacing;
        tooltipUI.setHeight(y += tooltipUI.padBottom);
        if (tooltipUI.getWidth() < 150.0) {
            tooltipUI.setWidth(150.0);
        }
    }

    public void offerInputItem(InventoryItem inventoryItem) {
        ArrayList<InventoryItem> itemsToTest = new ArrayList<InventoryItem>(this.selectedInventoryItems);
        itemsToTest.add(inventoryItem);
        if (this.getCraftLogic().willInputsAccommodate(itemsToTest) && this.getCraftLogic().canStartWithInventoryItems(this.player, itemsToTest) && this.getRecipeData().offerInputItem(this.manualSelectInputScriptFilter, inventoryItem)) {
            this.selectedInventoryItems.add(inventoryItem);
            this.cachedCanStartDirty = true;
            this.cachedPossibleCraftCount = this.getPossibleCraftCount(true);
            this.triggerEvent("onInputsChanged", new Object[0]);
        }
    }

    public void removeInputItem(InventoryItem inventoryItem) {
        this.getRecipeData().removeInputItem(inventoryItem);
        this.selectedInventoryItems.remove(inventoryItem);
        this.cachedCanStartDirty = true;
        this.cachedPossibleCraftCount = this.getPossibleCraftCount(true);
        this.triggerEvent("onInputsChanged", new Object[0]);
    }

    public void clearManualInputsFor(CraftRecipeData.InputScriptData input) {
        ArrayList<InventoryItem> existingLineItems = this.getRecipeData().getManualInputsFor(input.getInputScript(), new ArrayList<InventoryItem>());
        this.getRecipeData().clearManualInputs(input);
        for (InventoryItem inventoryItem : existingLineItems) {
            this.selectedInventoryItems.remove(inventoryItem);
        }
        this.cachedCanStartDirty = true;
        this.cachedPossibleCraftCount = this.getPossibleCraftCount(true);
        this.triggerEvent("onInputsChanged", new Object[0]);
    }

    public KahluaTable getInventoryItemsToTransfer() {
        KahluaTable table = LuaManager.platform.newTable();
        for (InventoryItem inventoryItem : this.selectedInventoryItems) {
            table.rawset(table.size() + 1, (Object)inventoryItem);
        }
        return table;
    }

    public boolean cachedCanPerformCurrentRecipe() {
        return this.cachedCanStart(this.player);
    }

    public boolean areAllInputItemsSatisfied() {
        return this.component.getCraftTestData().areAllInputItemsSatisfied();
    }

    public static class RecipeComparator
    implements Comparator<CraftRecipe> {
        private final IsoPlayer player;
        public CompareMode compareMode = CompareMode.NAME;

        public RecipeComparator(IsoPlayer player) {
            this.player = player;
        }

        @Override
        public int compare(CraftRecipe v1, CraftRecipe v2) {
            int nameCompareResult = v1.getTranslationName().compareTo(v2.getTranslationName());
            switch (this.compareMode.ordinal()) {
                case 1: {
                    double v1LastCraftTime = this.player.getPlayerCraftHistory().getCraftHistoryFor(v1.getName()).getLastCraftTime();
                    double v2LastCraftTime = this.player.getPlayerCraftHistory().getCraftHistoryFor(v2.getName()).getLastCraftTime();
                    int lastUsedCompareResult = Double.compare(v2LastCraftTime, v1LastCraftTime);
                    if (lastUsedCompareResult == 0) {
                        return nameCompareResult;
                    }
                    return lastUsedCompareResult;
                }
                case 2: {
                    int v1CraftCount = this.player.getPlayerCraftHistory().getCraftHistoryFor(v1.getName()).getCraftCount();
                    int v2CraftCount = this.player.getPlayerCraftHistory().getCraftHistoryFor(v2.getName()).getCraftCount();
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
}

