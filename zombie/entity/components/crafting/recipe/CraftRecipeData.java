/*
 * Decompiled with CFR 0.152.
 */
package zombie.entity.components.crafting.recipe;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import se.krka.kahlua.vm.KahluaTable;
import zombie.GameWindow;
import zombie.Lua.LuaManager;
import zombie.UsedFromLua;
import zombie.characters.IsoGameCharacter;
import zombie.characters.IsoPlayer;
import zombie.core.Color;
import zombie.core.Core;
import zombie.core.utils.ByteBlock;
import zombie.debug.DebugLog;
import zombie.entity.components.crafting.CraftMode;
import zombie.entity.components.crafting.CraftRecipeMonitor;
import zombie.entity.components.crafting.CraftUtil;
import zombie.entity.components.crafting.InputFlag;
import zombie.entity.components.crafting.OutputFlag;
import zombie.entity.components.crafting.recipe.CraftRecipeManager;
import zombie.entity.components.crafting.recipe.ItemDataList;
import zombie.entity.components.fluids.FluidConsume;
import zombie.entity.components.fluids.FluidContainer;
import zombie.entity.components.fluids.FluidSample;
import zombie.entity.components.resources.Resource;
import zombie.entity.components.resources.ResourceIO;
import zombie.entity.components.resources.ResourceItem;
import zombie.entity.components.resources.ResourceType;
import zombie.inventory.CompressIdenticalItems;
import zombie.inventory.InventoryItem;
import zombie.inventory.ItemContainer;
import zombie.inventory.ItemUser;
import zombie.inventory.types.Food;
import zombie.inventory.types.HandWeapon;
import zombie.inventory.types.Radio;
import zombie.network.GameClient;
import zombie.network.GameServer;
import zombie.scripting.ScriptManager;
import zombie.scripting.entity.components.crafting.CraftRecipe;
import zombie.scripting.entity.components.crafting.InputScript;
import zombie.scripting.entity.components.crafting.OutputScript;
import zombie.scripting.objects.CraftRecipeTag;
import zombie.scripting.objects.Item;
import zombie.scripting.objects.ItemTag;
import zombie.util.list.PZArrayUtil;

@UsedFromLua
public class CraftRecipeData {
    private static final float FLOAT_EPSILON = 1.0E-5f;
    private static final int MAX_CRAFT_COUNT = 100;
    private static final ArrayDeque<CraftRecipeData> DATA_POOL = new ArrayDeque();
    private IsoGameCharacter character;
    private CraftRecipeMonitor craftRecipeMonitor;
    private CraftRecipe recipe;
    public final ArrayList<InputScriptData> inputs = new ArrayList();
    private final ArrayList<OutputScriptData> outputs = new ArrayList();
    private final HashSet<Resource> usedResources = new HashSet();
    private final HashSet<InventoryItem> usedItems = new HashSet();
    private boolean allowInputResources = true;
    private boolean allowInputItems;
    private boolean allowOutputResources = true;
    private boolean allowOutputItems;
    private CraftMode craftMode = CraftMode.Automation;
    private boolean hasConsumedInputs;
    private boolean hasTestedInputs;
    private final ItemDataList toOutputItems = new ItemDataList(32);
    private final HashSet<InventoryItem> consumedUsedItems = new HashSet();
    private final ArrayList<InventoryItem> allViableItems = new ArrayList();
    private final ArrayList<Resource> allViableResources = new ArrayList();
    private final HashMap<CraftRecipe.LuaCall, Object> luaFunctionMap = new HashMap();
    private static String luaOnTestCacheString;
    private static Object luaOnTestCacheObject;
    private float targetVariableInputRatio = Float.MAX_VALUE;
    private float calculatedVariableInputRatio = 1.0f;
    private final HashMap<InputScript, HashSet<InventoryItem>> variableInputOverfilledItems = new HashMap();
    private final HashMap<InputScript, HashMap<Resource, ArrayList<InventoryItem>>> variableInputOverfilledResources = new HashMap();
    private int eatPercentage;
    private double elapsedTime;
    private KahluaTable modData;

    public static CraftRecipeData Alloc(CraftMode craftMode, boolean allowInputResources, boolean allowInputItems, boolean allowOutputResources, boolean allowOutputItems) {
        CraftRecipeData data = DATA_POOL.poll();
        if (data == null) {
            data = new CraftRecipeData();
        }
        data.craftMode = craftMode;
        data.allowInputResources = allowInputResources;
        data.allowInputItems = allowInputItems;
        data.allowOutputResources = allowOutputResources;
        data.allowOutputItems = allowOutputItems;
        return data;
    }

    public static void Release(CraftRecipeData data) {
        data.reset();
        DATA_POOL.add(data);
    }

    private CraftRecipeData() {
    }

    public CraftRecipeData(CraftMode craftMode, boolean allowInputResources, boolean allowInputItems, boolean allowOutputResources, boolean allowOutputItems) {
        this.craftMode = craftMode;
        this.allowInputResources = allowInputResources;
        this.allowInputItems = allowInputItems;
        this.allowOutputResources = allowOutputResources;
        this.allowOutputItems = allowOutputItems;
    }

    public void setMonitor(CraftRecipeMonitor monitor) {
        this.craftRecipeMonitor = monitor;
    }

    public boolean isAllowInputItems() {
        return this.allowInputItems;
    }

    public boolean isAllowOutputItems() {
        return this.allowOutputItems;
    }

    public boolean isAllowInputResources() {
        return this.allowInputResources;
    }

    public boolean isAllowOutputResources() {
        return this.allowOutputResources;
    }

    public ItemDataList getToOutputItems() {
        return this.toOutputItems;
    }

    public void reset() {
        this.allowInputResources = true;
        this.allowInputItems = false;
        this.allowOutputResources = true;
        this.allowOutputItems = false;
        this.craftMode = CraftMode.Automation;
        this.toOutputItems.reset();
        this.elapsedTime = 0.0;
        this.clearRecipe();
        this.craftRecipeMonitor = null;
    }

    private void clearRecipe() {
        this.recipe = null;
        for (int i = 0; i < this.inputs.size(); ++i) {
            InputScriptData input = this.inputs.get(i);
            InputScriptData.Release(input);
        }
        this.inputs.clear();
        for (int i = 0; i < this.outputs.size(); ++i) {
            OutputScriptData output = this.outputs.get(i);
            OutputScriptData.Release(output);
        }
        this.outputs.clear();
        this.luaFunctionMap.clear();
        luaOnTestCacheString = null;
        luaOnTestCacheObject = null;
        this.clearCaches();
        this.allViableItems.clear();
        this.allViableResources.clear();
        this.clearTargetVariableInputRatio();
    }

    private void clearCaches() {
        int i;
        if (!this.inputs.isEmpty()) {
            for (i = 0; i < this.inputs.size(); ++i) {
                InputScriptData input = this.inputs.get(i);
                input.clearCache();
            }
        }
        if (!this.outputs.isEmpty()) {
            for (i = 0; i < this.outputs.size(); ++i) {
                OutputScriptData output = this.outputs.get(i);
                output.clearCache();
            }
        }
        this.hasConsumedInputs = false;
        this.hasTestedInputs = false;
        this.usedResources.clear();
        this.usedItems.clear();
        this.toOutputItems.clear();
        if (this.modData != null) {
            this.modData.wipe();
        }
        this.calculatedVariableInputRatio = 1.0f;
        this.variableInputOverfilledItems.clear();
    }

    public void setCharacter(IsoGameCharacter character) {
        this.character = character;
    }

    public IsoGameCharacter getCharacter() {
        return this.character;
    }

    public void setRecipe(CraftRecipe recipe) {
        if (recipe == null) {
            this.clearRecipe();
        } else if (this.recipe != recipe) {
            CacheData data;
            int i;
            this.clearRecipe();
            this.recipe = recipe;
            for (i = 0; i < recipe.getInputs().size(); ++i) {
                data = InputScriptData.Alloc(this, recipe.getInputs().get(i));
                this.inputs.add((InputScriptData)data);
            }
            for (i = 0; i < recipe.getOutputs().size(); ++i) {
                data = OutputScriptData.Alloc(this, recipe.getOutputs().get(i));
                this.outputs.add((OutputScriptData)data);
            }
        } else {
            this.clearCaches();
        }
    }

    public CraftRecipe getRecipe() {
        return this.recipe;
    }

    public InputScriptData getDataForInputScript(InputScript script) {
        for (int i = 0; i < this.inputs.size(); ++i) {
            InputScriptData data = this.inputs.get(i);
            if (data.inputScript == script) {
                return data;
            }
            if (!data.inputScript.hasConsumeFromItem() || data.inputScript.getConsumeFromItemScript() != script) continue;
            return data;
        }
        return null;
    }

    protected OutputScriptData getDataForOutputScript(OutputScript script) {
        for (int i = 0; i < this.outputs.size(); ++i) {
            OutputScriptData data = this.outputs.get(i);
            if (data.outputScript != script) continue;
            return data;
        }
        return null;
    }

    public InventoryItem getFirstManualInputFor(InputScript inputScript) {
        if (this.recipe == null || !this.recipe.containsIO(inputScript)) {
            return null;
        }
        InputScriptData data = this.getDataForInputScript(inputScript);
        return data.getFirstInputItem();
    }

    public boolean canOfferInputItem(InventoryItem inventoryItem) {
        return this.canOfferInputItem(inventoryItem, false);
    }

    public boolean canOfferInputItem(InventoryItem inventoryItem, boolean verbose) {
        for (int i = 0; i < this.inputs.size(); ++i) {
            InputScriptData data = this.inputs.get(i);
            if (!this.canOfferInputItem(data.inputScript, inventoryItem, verbose)) continue;
            return true;
        }
        return false;
    }

    public boolean canOfferInputItem(InputScript inputScript, InventoryItem item) {
        return this.canOfferInputItem(inputScript, item, false);
    }

    public boolean canOfferInputItem(InputScript inputScript, InventoryItem item, boolean verbose) {
        Objects.requireNonNull(inputScript);
        Objects.requireNonNull(item);
        if (this.recipe == null) {
            return false;
        }
        if (!this.recipe.containsIO(inputScript)) {
            if (verbose) {
                DebugLog.CraftLogic.warn("Input script not part of current recipe.");
            }
            return false;
        }
        InputScriptData data = this.getDataForInputScript(inputScript);
        if (data == null) {
            if (verbose) {
                DebugLog.CraftLogic.warn("Data is null for input script");
            }
            return false;
        }
        if (data.getInputScript().getResourceType() != ResourceType.Item) {
            if (verbose) {
                DebugLog.CraftLogic.warn("Cannot offer items to input scripts that are not ResourceType.Item");
            }
            return false;
        }
        return data.acceptsInputItem(item);
    }

    public boolean offerAndReplaceInputItem(InventoryItem inventoryItem) {
        for (int i = 0; i < this.inputs.size(); ++i) {
            if (!this.offerAndReplaceInputItem(this.inputs.get(i), inventoryItem)) continue;
            return true;
        }
        return false;
    }

    public boolean offerAndReplaceInputItem(InputScriptData data, InventoryItem inventoryItem) {
        if (this.canOfferInputItem(data.inputScript, inventoryItem, false) && !this.containsInputItem(inventoryItem)) {
            if (data.inputScript.isExclusive() && data.getFirstInputItem() != null && !data.getFirstInputItem().getFullType().equals(inventoryItem.getFullType())) {
                while (data.getLastInputItem() != null) {
                    data.removeInputItem(data.getLastInputItem());
                }
            }
            if (data.isInputItemsSatisfied() && data.isInputItemsSatisifiedToMaximum()) {
                data.removeInputItem(data.getLastInputItem());
            }
            return data.addInputItem(inventoryItem);
        }
        return false;
    }

    public boolean offerInputItem(InputScript inputScript, InventoryItem item) {
        return this.offerInputItem(inputScript, item, false);
    }

    public boolean offerInputItem(InputScript inputScript, InventoryItem item, boolean verbose) {
        if (this.canOfferInputItem(inputScript, item, verbose) && !this.containsInputItem(item)) {
            InputScriptData data = this.getDataForInputScript(inputScript);
            if (inputScript.isExclusive() && data.getFirstInputItem() != null && !data.getFirstInputItem().getFullType().equals(item.getFullType())) {
                return false;
            }
            if (!data.isInputItemsSatisfied() || !data.isInputItemsSatisifiedToMaximum()) {
                return data.addInputItem(item);
            }
        }
        return false;
    }

    public boolean containsInputItem(InventoryItem inventoryItem) {
        for (int i = 0; i < this.inputs.size(); ++i) {
            if (!this.containsInputItem(this.inputs.get(i), inventoryItem)) continue;
            return true;
        }
        return false;
    }

    public boolean containsInputItem(InputScriptData data, InventoryItem inventoryItem) {
        return data.inputScript.getResourceType() == ResourceType.Item && data.inputItems.contains(inventoryItem);
    }

    public boolean removeInputItem(InventoryItem inventoryItem) {
        for (int i = 0; i < this.inputs.size(); ++i) {
            InputScriptData data = this.inputs.get(i);
            if (!data.removeInputItem(inventoryItem)) continue;
            return true;
        }
        return false;
    }

    public boolean areAllInputItemsSatisfied() {
        for (int i = 0; i < this.inputs.size(); ++i) {
            if (this.inputs.get(i).isInputItemsSatisfied()) continue;
            return false;
        }
        return true;
    }

    public boolean luaCallOnTest() {
        return true;
    }

    private boolean initLuaFunctions() {
        if (this.recipe == null) {
            return false;
        }
        for (CraftRecipe.LuaCall luaCall : CraftRecipe.LuaCall.values()) {
            if (!this.recipe.hasLuaCall(luaCall)) continue;
            Object functionObject = LuaManager.getFunctionObject(this.recipe.getLuaCallString(luaCall), null);
            if (functionObject != null) {
                this.luaFunctionMap.put(luaCall, functionObject);
                continue;
            }
            DebugLog.CraftLogic.warn("Could not find lua function: " + this.recipe.getLuaCallString(luaCall));
        }
        return true;
    }

    public void luaCallOnStart() {
        this.luaCallOnStart(null);
    }

    public void luaCallOnStart(IsoGameCharacter character) {
        if (!this.initLuaFunctions()) {
            return;
        }
        Object functionObject = this.luaFunctionMap.get((Object)CraftRecipe.LuaCall.OnStart);
        if (functionObject != null) {
            LuaManager.caller.protectedCallVoid(LuaManager.thread, functionObject, this, character);
        }
    }

    public void luaCallOnUpdate() {
        if (!this.initLuaFunctions()) {
            return;
        }
        Object functionObject = this.luaFunctionMap.get((Object)CraftRecipe.LuaCall.OnUpdate);
        if (functionObject != null) {
            LuaManager.caller.protectedCallVoid(LuaManager.thread, functionObject, this);
        }
    }

    public void luaCallOnCreate() {
        this.luaCallOnCreate(null);
    }

    public void luaCallOnCreate(IsoGameCharacter character) {
        if (!this.initLuaFunctions()) {
            return;
        }
        Object functionObject = this.luaFunctionMap.get((Object)CraftRecipe.LuaCall.OnCreate);
        if (functionObject != null) {
            LuaManager.caller.protectedCallVoid(LuaManager.thread, functionObject, this, character);
        }
    }

    public void luaCallOnFailed() {
        if (!this.initLuaFunctions()) {
            return;
        }
        Object functionObject = this.luaFunctionMap.get((Object)CraftRecipe.LuaCall.OnFailed);
        if (functionObject != null) {
            LuaManager.caller.protectedCallVoid(LuaManager.thread, functionObject, this);
        }
    }

    public boolean canPerform(IsoGameCharacter character, List<Resource> inputResources, List<InventoryItem> overrideInputItems, boolean forceTestAll, ArrayList<ItemContainer> containers) {
        if (!CraftRecipeManager.isValidRecipeForCharacter(this.recipe, character, this.craftRecipeMonitor, containers)) {
            return false;
        }
        return this.consumeInputsInternal(character, true, inputResources, overrideInputItems, forceTestAll, false) && this.createOutputsInternal(true, null, character);
    }

    public boolean perform(IsoGameCharacter character, List<Resource> inputResources, List<InventoryItem> overrideInputItems, ArrayList<ItemContainer> containers) {
        if (!CraftRecipeManager.isValidRecipeForCharacter(this.recipe, character, this.craftRecipeMonitor, containers)) {
            return false;
        }
        boolean success = this.consumeInputsInternal(character, false, inputResources, overrideInputItems);
        if (success) {
            this.luaCallOnStart(character);
            if (this.createOutputsInternal(false, null, character)) {
                if (this.recipe.xpAward != null) {
                    this.addXP(character);
                }
                ((IsoPlayer)character).getPlayerCraftHistory().addCraftHistoryCraftedEvent(this.recipe.getName());
                return true;
            }
        }
        return false;
    }

    private void addXP(IsoGameCharacter character) {
        this.recipe.addXP(character);
    }

    public void processDestroyAndUsedItems(IsoGameCharacter character) {
        ArrayList<InputScript> recipeinputs = this.recipe.getInputs();
        float[] requiredTally = new float[recipeinputs.size()];
        for (int i = 0; i < recipeinputs.size(); ++i) {
            requiredTally[i] = recipeinputs.get(i).isVariableAmount() ? (float)recipeinputs.get(i).getIntAmount() * this.calculatedVariableInputRatio : (float)recipeinputs.get(i).getIntAmount();
        }
        for (InventoryItem inventoryItem : this.consumedUsedItems) {
            Item item = inventoryItem.getScriptItem();
            DebugLog.CraftLogic.println("post process -> Sync using item: " + inventoryItem.getFullType());
            for (int j = 0; j < recipeinputs.size(); ++j) {
                InputScriptData inputData = this.getDataForInputScript(recipeinputs.get(j));
                if (inputData.hasAppliedItem(inventoryItem)) {
                    InputScript input = inputData.getInputScript();
                    if (!input.hasFlag(InputFlag.Unseal) || inventoryItem.getFluidContainer() != null) {
                        // empty if block
                    }
                    if (!recipeinputs.get(j).isItemCount() && (recipeinputs.get(j).hasConsumeFromItem() || inventoryItem.getFluidContainer() != null)) continue;
                    this.processKeepInputItem(inputData, character, false, inventoryItem);
                    if (requiredTally[j] > 1.0E-5f) {
                        float reqtally = (float)Math.ceil(requiredTally[j]);
                        boolean destroy = recipeinputs.get(j).isDestroy();
                        if (recipeinputs.get(j).hasFlag(InputFlag.DontReplace) && (inventoryItem.getReplaceOnUse() != null || inventoryItem.getScriptItem().getReplaceOnDeplete() != null)) {
                            destroy = true;
                        }
                        if (recipeinputs.get(j).isItemCount()) {
                            int toConsume = inventoryItem.getCurrentUses();
                            ItemUser.UseItem(inventoryItem, true, false, toConsume, recipeinputs.get(j).isKeep(), destroy);
                            float itemscale = 1.0f / recipeinputs.get(j).getRelativeScale(item.getFullName());
                            int n = j;
                            requiredTally[n] = requiredTally[n] - itemscale;
                        } else {
                            float itemscale = recipeinputs.get(j).getRelativeScale(item.getFullName());
                            int toConsume = (int)(reqtally * itemscale);
                            toConsume = Math.min(toConsume, (int)reqtally);
                            int consumed = ItemUser.UseItem(inventoryItem, true, false, toConsume, recipeinputs.get(j).isKeep(), destroy);
                            int n = j;
                            requiredTally[n] = requiredTally[n] - (float)consumed / itemscale;
                        }
                    }
                }
                if (!GameServer.server) continue;
                GameServer.sendItemStats(inventoryItem);
            }
        }
    }

    public int getPossibleCraftCount(List<Resource> inputResources, List<InventoryItem> inputItems, List<Resource> consumedResources, List<InventoryItem> consumedItems, boolean limitItemsToAppliedItems) {
        int inputResourcePossibleCraftCount;
        int inputItemsPossibleCraftCount;
        consumedItems.clear();
        consumedResources.clear();
        int possibleCraftCount = 100;
        for (int i = 0; i < this.inputs.size() && (possibleCraftCount = Math.min(possibleCraftCount, (inputItemsPossibleCraftCount = this.getInputCraftCount(this.inputs.get(i), inputItems, consumedItems, limitItemsToAppliedItems)) + (inputResourcePossibleCraftCount = this.getResourceCraftCount(this.inputs.get(i), inputResources, consumedResources, limitItemsToAppliedItems)))) != 0; ++i) {
        }
        return possibleCraftCount;
    }

    private int getInputCraftCount(InputScriptData inputData, List<InventoryItem> inputItems, List<InventoryItem> consumedItems, boolean limitItemsToAppliedItems) {
        float floatEpsilon = 1.0E-5f;
        InputScript inputScript = inputData.getInputScript();
        double inputFilledAmount = 0.0;
        int createToItemSlots = 0;
        if (inputItems != null) {
            for (int j = 0; j < inputItems.size(); ++j) {
                InventoryItem invItem = inputItems.get(j);
                boolean canUse = inputScript.canUseItem(invItem, this.getCharacter());
                if (limitItemsToAppliedItems) {
                    boolean bl = canUse = canUse && inputData.hasAppliedItemType(invItem.getScriptItem());
                }
                if (!canUse) continue;
                double itemScale = inputData.getInputScript().getRelativeScale(invItem.getFullType());
                int uses = inputData.getInputScript().isItemCount() ? 1 : invItem.getCurrentUses();
                double scaledUses = (double)uses / itemScale;
                if (!inputScript.isItemCount()) {
                    CraftRecipe.IOScript script;
                    if (inputScript.hasConsumeFromItem()) {
                        script = inputScript.getConsumeFromItemScript();
                        switch (((InputScript)script).getResourceType()) {
                            case Fluid: {
                                scaledUses = invItem.getFluidContainer().getAmount();
                            }
                        }
                    }
                    if (inputScript.hasCreateToItem()) {
                        script = inputScript.getCreateToItemScript();
                        switch (((OutputScript)script).getResourceType()) {
                            case Fluid: {
                                float availableSlots = invItem.getFluidContainer().getFreeCapacity() / ((OutputScript)script).getAmount();
                                if (((OutputScript)script).hasFlag(OutputFlag.RespectCapacity)) {
                                    createToItemSlots += (int)Math.floor(availableSlots);
                                    break;
                                }
                                createToItemSlots += (int)Math.ceil(availableSlots);
                            }
                        }
                    }
                }
                inputFilledAmount += scaledUses;
                consumedItems.add(invItem);
            }
        }
        inputFilledAmount += (double)1.0E-5f;
        if (inputScript.isKeep() && !inputScript.hasConsumeFromItem() && !inputScript.hasCreateToItem()) {
            int inputReqdAmount = inputScript.getIntAmount();
            if (inputFilledAmount < (double)inputReqdAmount) {
                return 0;
            }
            return 100;
        }
        float inputReqdAmount = inputScript.getAmount();
        if (inputScript.hasConsumeFromItem() && !inputScript.isItemCount()) {
            InputScript script = inputScript.getConsumeFromItemScript();
            switch (script.getResourceType()) {
                case Fluid: {
                    inputReqdAmount = script.getAmount();
                }
            }
        }
        int thisItemPossibleCraftCount = (int)Math.floor(inputFilledAmount / (double)inputReqdAmount);
        if (inputScript.hasCreateToItem() && !inputScript.isItemCount()) {
            thisItemPossibleCraftCount = createToItemSlots;
        }
        return thisItemPossibleCraftCount;
    }

    private int getResourceCraftCount(InputScriptData inputData, List<Resource> inputResources, List<Resource> consumedResources, boolean limitItemsToAppliedItems) {
        int possibleCraftCount = 0;
        block3: for (int j = 0; j < inputResources.size(); ++j) {
            Resource resource = inputResources.get(j);
            switch (resource.getType()) {
                case Item: {
                    ArrayList<InventoryItem> allItems = ((ResourceItem)resource).getStoredItems();
                    ArrayList<InventoryItem> consumedItems = new ArrayList<InventoryItem>();
                    possibleCraftCount += this.getInputCraftCount(inputData, allItems, consumedItems, limitItemsToAppliedItems);
                    if (consumedItems.isEmpty()) continue block3;
                    consumedResources.add(resource);
                }
            }
        }
        return Math.min(possibleCraftCount, 100);
    }

    public boolean canConsumeInputs(List<Resource> inputResources, List<InventoryItem> overrideInputItems, boolean forceTestAll, boolean clearAllViable) {
        return this.consumeInputsInternal(this.getCharacter(), true, inputResources, overrideInputItems, forceTestAll, clearAllViable);
    }

    public boolean canConsumeInputs(List<Resource> inputResources) {
        return this.consumeInputsInternal(this.getCharacter(), true, inputResources, null);
    }

    public boolean consumeInputs(List<Resource> inputResources) {
        return this.consumeInputsInternal(this.getCharacter(), false, inputResources, null);
    }

    public boolean consumeOnTickInputs(List<Resource> inputResources) {
        if (this.recipe == null) {
            return false;
        }
        if (!this.recipe.hasOnTickInputs()) {
            return true;
        }
        return this.consumeRecipeInputsOnTick(inputResources);
    }

    public boolean canCreateOutputs(List<Resource> outputResources) {
        return this.createOutputsInternal(true, outputResources, null);
    }

    public boolean createOutputs(List<Resource> outputResources) {
        return this.createOutputsInternal(false, outputResources, null);
    }

    public boolean canCreateOutputs(List<Resource> outputResources, IsoGameCharacter character) {
        return this.createOutputsInternal(true, outputResources, character);
    }

    public boolean createOutputs(List<Resource> outputResources, IsoGameCharacter character) {
        return this.createOutputsInternal(false, outputResources, character);
    }

    public boolean createOnTickOutputs(List<Resource> outputResources) {
        if (this.recipe == null) {
            return false;
        }
        if (!this.recipe.hasOnTickOutputs()) {
            return true;
        }
        return this.createRecipeOutputsOnTick(outputResources);
    }

    private boolean consumeInputsInternal(IsoGameCharacter character, boolean testOnly, List<Resource> inputResources, List<InventoryItem> overrideInputItems) {
        return this.consumeInputsInternal(character, testOnly, inputResources, overrideInputItems, false, false);
    }

    private boolean consumeInputsInternal(IsoGameCharacter character, boolean testOnly, List<Resource> inputResources, List<InventoryItem> overrideInputItems, boolean forceTestAll, boolean clearAllViable) {
        IsoPlayer player;
        if (!testOnly && GameClient.client) {
            throw new RuntimeException("Cannot call with testOnly==false on client.");
        }
        if (this.hasConsumedInputs) {
            return true;
        }
        if (this.recipe != null && !this.recipe.canBeDoneInDark() && character instanceof IsoPlayer && (player = (IsoPlayer)character).tooDarkToRead()) {
            return false;
        }
        this.clearCaches();
        boolean success = this.consumeRecipeInputs(testOnly, inputResources, overrideInputItems, forceTestAll, clearAllViable, character);
        if (success) {
            this.hasConsumedInputs = !testOnly;
            this.hasTestedInputs = testOnly;
        }
        return success;
    }

    private boolean createOutputsInternal(boolean testOnly, List<Resource> outputResources, IsoGameCharacter character) {
        if (!testOnly && GameClient.client) {
            throw new RuntimeException("Cannot call with testOnly==false on client.");
        }
        if (!testOnly && !this.hasConsumedInputs) {
            if (Core.debug) {
                throw new RuntimeException("createOutputs requires consumeInputs to be called first");
            }
            return false;
        }
        if (testOnly && !this.hasTestedInputs && !this.hasConsumedInputs) {
            if (Core.debug) {
                throw new RuntimeException("(test) createOutputs requires consumeInputs to be called first");
            }
            return false;
        }
        boolean success = this.createRecipeOutputs(testOnly, outputResources, character);
        if (!testOnly) {
            this.hasConsumedInputs = false;
        } else {
            this.hasTestedInputs = false;
        }
        if (!testOnly && success) {
            this.destroyAllSurvivingDestroyInputs();
        }
        return success;
    }

    private boolean consumeRecipeInputsOnTick(List<Resource> inputResources) {
        if (this.recipe == null) {
            return false;
        }
        this.usedResources.clear();
        boolean failed = false;
        for (int i = 0; i < this.inputs.size(); ++i) {
            InputScriptData data = this.inputs.get(i);
            if (!data.inputScript.isApplyOnTick() || data.inputScript.getResourceType() == ResourceType.Item || data.inputScript.hasFlag(InputFlag.HandcraftOnly) && this.craftMode != CraftMode.Handcraft || data.inputScript.hasFlag(InputFlag.AutomationOnly) && this.craftMode != CraftMode.Automation) continue;
            boolean consumed = false;
            if (this.allowInputResources && inputResources != null && !inputResources.isEmpty()) {
                consumed = this.consumeInputFromResources(data.inputScript, inputResources, false, null, this.getCharacter());
            }
            if (consumed) continue;
            failed = true;
            break;
        }
        return !failed;
    }

    private boolean consumeRecipeInputs(boolean testOnly, List<Resource> inputResources, List<InventoryItem> overrideInputItems, boolean forceTestAll, boolean clearAllViable, IsoGameCharacter character) {
        int i;
        if (this.recipe == null) {
            return false;
        }
        this.usedResources.clear();
        this.usedItems.clear();
        if (clearAllViable) {
            this.allViableResources.clear();
            this.allViableItems.clear();
        }
        if (this.craftRecipeMonitor != null) {
            this.craftRecipeMonitor.log("[ConsumeRecipeInputs]");
            this.craftRecipeMonitor.open();
            this.craftRecipeMonitor.log("test = " + testOnly);
            this.craftRecipeMonitor.log("overrideInputItems = " + (overrideInputItems != null));
        }
        boolean failed = false;
        this.calculatedVariableInputRatio = this.isVariableAmount() ? this.targetVariableInputRatio : 1.0f;
        for (i = 0; i < this.inputs.size(); ++i) {
            List<InventoryItem> items;
            InputScriptData data = this.inputs.get(i);
            if (this.craftRecipeMonitor != null) {
                this.craftRecipeMonitor.log("[" + i + "] input, line = \"" + data.inputScript.getOriginalLine().trim() + "\"");
            }
            if (data.inputScript.hasFlag(InputFlag.HandcraftOnly) && this.craftMode != CraftMode.Handcraft) {
                if (this.craftRecipeMonitor == null) continue;
                this.craftRecipeMonitor.log("-> skipping line, 'handcraft' only");
                continue;
            }
            if (data.inputScript.hasFlag(InputFlag.AutomationOnly) && this.craftMode != CraftMode.Automation) {
                if (this.craftRecipeMonitor == null) continue;
                this.craftRecipeMonitor.log("-> skipping line, 'automation' only");
                continue;
            }
            if (data.inputScript.isApplyOnTick()) {
                if (this.craftRecipeMonitor == null) continue;
                this.craftRecipeMonitor.log("-> skipping line, onTick = true");
                continue;
            }
            boolean consumed = false;
            List<InventoryItem> list = items = overrideInputItems != null ? overrideInputItems : data.inputItems;
            if (this.allowInputItems && !items.isEmpty() && data.inputScript.getResourceType() == ResourceType.Item && (consumed = this.consumeInputFromItems(data.inputScript, items, testOnly, data, false, character)) && this.craftRecipeMonitor != null) {
                this.craftRecipeMonitor.success("consumed from supplied items list");
            }
            if (!consumed && this.allowInputResources && inputResources != null && !inputResources.isEmpty()) {
                consumed = this.consumeInputFromResources(data.inputScript, inputResources, testOnly, data, character);
            }
            data.cachedCanConsume = consumed;
            if (consumed) continue;
            failed = true;
            if (!forceTestAll) break;
        }
        if (failed) {
            if (this.craftRecipeMonitor != null) {
                this.craftRecipeMonitor.warn("NOT CONSUMED!");
                this.craftRecipeMonitor.close();
            }
            return false;
        }
        if (!testOnly) {
            for (int i2 = 0; i2 < this.inputs.size(); ++i2) {
                int itemsToAdd;
                InputScript script = this.inputs.get((int)i2).inputScript;
                if (this.variableInputOverfilledItems.containsKey(script)) {
                    itemsToAdd = (int)Math.ceil(this.calculatedVariableInputRatio * script.getAmount()) - script.getIntAmount();
                    for (InventoryItem item : this.variableInputOverfilledItems.get(script)) {
                        if (itemsToAdd <= 0) break;
                        this.consumedUsedItems.add(item);
                        --itemsToAdd;
                    }
                    if (Core.debug && itemsToAdd > 0) {
                        throw new RuntimeException("Calculated ratio calls for more items than we have. This should not be possible");
                    }
                }
                if (!this.variableInputOverfilledResources.containsKey(script)) continue;
                itemsToAdd = (int)Math.ceil(this.calculatedVariableInputRatio * script.getAmount()) - script.getIntAmount();
                block3: for (Resource res : this.variableInputOverfilledResources.get(script).keySet()) {
                    for (InventoryItem item : this.variableInputOverfilledResources.get(script).get(res)) {
                        InventoryItem removedItem;
                        if (itemsToAdd <= 0) continue block3;
                        if (!(res.getType() != ResourceType.Item || script.isKeep() && !res.canMoveItemsToOutput() || (removedItem = ((ResourceItem)res).removeItem(item)) != null && removedItem == item || !Core.debug)) {
                            throw new RuntimeException("Item didn't get removed.");
                        }
                        --itemsToAdd;
                    }
                }
                if (!Core.debug || itemsToAdd <= 0) continue;
                throw new RuntimeException("Calculated ratio calls for more items than we have. This should not be possible");
            }
        }
        if (this.allowInputResources && inputResources != null && !inputResources.isEmpty()) {
            for (i = 0; i < inputResources.size(); ++i) {
                Resource resource = inputResources.get(i);
                if (resource.getType() != ResourceType.Item || resource.isEmpty() || this.usedResources.contains(resource)) continue;
                if (this.craftRecipeMonitor != null) {
                    this.craftRecipeMonitor.warn("CANCEL, not all [Resource] items could be consumed!");
                    this.craftRecipeMonitor.close();
                }
                return false;
            }
        }
        if (this.craftRecipeMonitor != null) {
            this.craftRecipeMonitor.log("[ALL PASSED] returning");
            this.craftRecipeMonitor.close();
        }
        return true;
    }

    public boolean OnTestItem(InventoryItem inventoryItem) {
        return this.recipe.OnTestItem(inventoryItem, this.getCharacter());
    }

    private boolean consumeInputFromItems(InputScript input, List<InventoryItem> items, boolean testOnly, CacheData cacheData, boolean clearUsed, IsoGameCharacter character) {
        if (input == null || input.getResourceType() != ResourceType.Item) {
            return false;
        }
        if (items.isEmpty()) {
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
        if (clearUsed) {
            this.usedItems.clear();
        }
        HashSet<InventoryItem> tempUsedItems = new HashSet<InventoryItem>();
        HashSet<InventoryItem> tempAllViableItems = new HashSet<InventoryItem>();
        HashSet<InventoryItem> tempConsumedUsedItems = new HashSet<InventoryItem>();
        HashSet<InventoryItem> tempVariableOverfillUsedItems = new HashSet<InventoryItem>();
        float requiredAmount = targetRequiredAmount;
        float allowedAmount = maxAllowedAmount;
        List<Item> inputItems = input.getPossibleInputItems();
        for (int j = 0; j < inputItems.size(); ++j) {
            for (int i = 0; i < items.size(); ++i) {
                InventoryItem inventoryItem = items.get(i);
                if (!inventoryItem.getScriptItem().getFullName().equals(inputItems.get(j).getFullName()) || this.usedItems.contains(inventoryItem) || tempUsedItems.contains(inventoryItem)) continue;
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
                if (!this.allViableItems.contains(inventoryItem)) {
                    tempAllViableItems.add(inventoryItem);
                }
                if (!testOnly) {
                    if (requiredAmount > 1.0E-5f) {
                        tempConsumedUsedItems.add(inventoryItem);
                    } else {
                        tempVariableOverfillUsedItems.add(inventoryItem);
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
                if ((allowedAmount -= uses) <= 1.0E-5f) break;
            }
            if (requiredAmount <= 1.0E-5f) {
                this.usedItems.addAll(tempUsedItems);
                this.consumedUsedItems.addAll(tempConsumedUsedItems);
                this.allViableItems.addAll(tempAllViableItems);
                if (input.isVariableAmount()) {
                    float filledAmount = Math.min(targetRequiredAmount - requiredAmount, maxAllowedAmount);
                    float filledRatio = filledAmount / targetRequiredAmount;
                    this.calculatedVariableInputRatio = Math.min(this.calculatedVariableInputRatio, filledRatio);
                    this.variableInputOverfilledItems.put(input, tempVariableOverfillUsedItems);
                }
                return true;
            }
            if (!input.isExclusive()) continue;
            requiredAmount = targetRequiredAmount;
            allowedAmount = maxAllowedAmount;
            tempUsedItems.clear();
            tempAllViableItems.clear();
            tempConsumedUsedItems.clear();
            tempVariableOverfillUsedItems.clear();
        }
        return false;
    }

    private boolean consumeInputFromResources(InputScript input, List<Resource> resources, boolean testOnly, CacheData cacheData, IsoGameCharacter character) {
        if (input == null || resources == null || resources.isEmpty()) {
            return false;
        }
        if (cacheData == null) {
            throw new RuntimeException("Input requires cache data.");
        }
        HashSet<Resource> tempUsedResources = new HashSet<Resource>(this.usedResources);
        if (CraftRecipeManager.consumeInputFromResources(input, resources, testOnly, cacheData, character, tempUsedResources)) {
            for (Resource resource : tempUsedResources) {
                if (this.usedResources.contains(resource)) continue;
                this.usedResources.add(resource);
                this.allViableResources.add(resource);
                if (this.craftRecipeMonitor == null) continue;
                this.craftRecipeMonitor.success("consumed by resource: " + resource.getId());
            }
            return true;
        }
        cacheData.softReset();
        return false;
    }

    private boolean createRecipeOutputsOnTick(List<Resource> outputResources) {
        if (this.recipe == null) {
            return false;
        }
        this.usedResources.clear();
        boolean failed = false;
        for (int i = 0; i < this.outputs.size(); ++i) {
            OutputScriptData data = this.outputs.get(i);
            if (!data.outputScript.isApplyOnTick() || data.outputScript.getResourceType() == ResourceType.Item || data.outputScript.hasFlag(OutputFlag.HandcraftOnly) && this.craftMode != CraftMode.Handcraft || data.outputScript.hasFlag(OutputFlag.AutomationOnly) && this.craftMode != CraftMode.Automation) continue;
            boolean created = false;
            if (this.allowOutputResources && outputResources != null && !outputResources.isEmpty()) {
                created = this.createOutputToResources(data.outputScript, outputResources, false, null);
            }
            if (created) continue;
            failed = true;
            break;
        }
        return !failed;
    }

    public boolean createRecipeOutputs(boolean testOnly, List<Resource> outputResources, IsoGameCharacter character) {
        ArrayList<InventoryItem> researchInputs;
        if (this.recipe == null) {
            return false;
        }
        this.toOutputItems.clear();
        this.usedResources.clear();
        if (this.craftRecipeMonitor != null) {
            this.craftRecipeMonitor.log("[CreateRecipeOutputs]");
            this.craftRecipeMonitor.open();
            this.craftRecipeMonitor.log("test = " + testOnly);
        }
        boolean failed = false;
        for (int i = 0; i < this.outputs.size(); ++i) {
            OutputScriptData data = this.outputs.get(i);
            if (this.craftRecipeMonitor != null) {
                this.craftRecipeMonitor.log("[" + i + "] output, line = \"" + data.outputScript.getOriginalLine().trim() + "\"");
            }
            if (data.outputScript.hasFlag(OutputFlag.HandcraftOnly) && this.craftMode != CraftMode.Handcraft) {
                if (this.craftRecipeMonitor == null) continue;
                this.craftRecipeMonitor.log("-> skipping line, 'handcraft' only");
                continue;
            }
            if (data.outputScript.hasFlag(OutputFlag.AutomationOnly) && this.craftMode != CraftMode.Automation) {
                if (this.craftRecipeMonitor == null) continue;
                this.craftRecipeMonitor.log("-> skipping line, 'automation' only");
                continue;
            }
            if (data.outputScript.isApplyOnTick()) {
                if (this.craftRecipeMonitor == null) continue;
                this.craftRecipeMonitor.log("-> skipping line, onTick = true");
                continue;
            }
            boolean created = false;
            if (data.outputScript.getResourceType() == ResourceType.Item) {
                created = this.createOutputItems(data.outputScript, this.toOutputItems, testOnly, data, character);
            }
            if (!created && this.allowOutputResources && outputResources != null && !outputResources.isEmpty()) {
                created = this.createOutputToResources(data.outputScript, outputResources, testOnly, data);
            }
            if (created) continue;
            failed = true;
            break;
        }
        if (!failed) {
            if (this.craftRecipeMonitor != null) {
                this.craftRecipeMonitor.log("[Collecting keep items]");
            }
            this.collectKeepItems(this.toOutputItems, testOnly);
        }
        if (!failed && this.allowOutputResources && this.toOutputItems.size() > 0 && outputResources != null && !outputResources.isEmpty()) {
            if (this.craftRecipeMonitor != null) {
                this.craftRecipeMonitor.log("[Distribute items to outputs]");
            }
            this.distributeItemsToResources(outputResources, this.toOutputItems, testOnly);
        }
        if (!failed && !this.allowOutputItems && this.toOutputItems.hasUnprocessed()) {
            if (this.craftRecipeMonitor != null) {
                this.craftRecipeMonitor.warn("FAILED: unable to offload all created items to output resources!");
            }
            failed = true;
        }
        if (failed) {
            if (this.craftRecipeMonitor != null) {
                this.craftRecipeMonitor.warn("NOT CREATED!");
                this.craftRecipeMonitor.close();
            }
            return false;
        }
        if (this.craftRecipeMonitor != null) {
            this.craftRecipeMonitor.log("[ALL PASSED] returning");
            this.craftRecipeMonitor.close();
        }
        if (character != null && this.getFirstInputItemWithFlag("ResearchInput") != null && !(researchInputs = this.getAllInputItemsWithFlag("ResearchInput")).isEmpty()) {
            for (int j = 0; j < researchInputs.size(); ++j) {
                InventoryItem researchInput = researchInputs.get(j);
                if (researchInput.getResearchableRecipes(character).isEmpty()) continue;
                researchInput.researchRecipes(character);
            }
        }
        return true;
    }

    private void processKeepInputItem(InputScriptData inputData, IsoGameCharacter character, boolean testOnly, InventoryItem inventoryItem) {
        CraftRecipe recipe = inputData.getRecipeData().getRecipe();
        if (inputData.getInputScript().isKeep() && !testOnly) {
            int skill;
            DebugLog.CraftLogic.debugln("Recipe is " + inputData.getRecipeData().getRecipe().getName());
            DebugLog.CraftLogic.debugln("Item is " + inventoryItem.getType());
            InputScript input = inputData.getInputScript();
            if (input.hasFlag(InputFlag.MayDegradeHeavy)) {
                skill = 0;
                if (character != null) {
                    skill = recipe.getHighestRelevantSkillLevel(character) + inventoryItem.getMaintenanceMod(character);
                }
                inventoryItem.damageCheck(skill, 1.0f, false);
            } else if (input.hasFlag(InputFlag.MayDegrade)) {
                skill = 0;
                if (character != null) {
                    skill = recipe.getHighestRelevantSkillLevel(character) + inventoryItem.getMaintenanceMod(character);
                }
                inventoryItem.damageCheck(skill, 2.0f, false);
            } else if (input.hasFlag(InputFlag.MayDegradeLight)) {
                skill = 0;
                if (character != null) {
                    skill = recipe.getHighestRelevantSkillLevel(character) + inventoryItem.getMaintenanceMod(character);
                }
                inventoryItem.damageCheck(skill, 3.0f, false);
            } else if (input.hasFlag(InputFlag.MayDegradeVeryLight)) {
                skill = 0;
                if (character != null) {
                    skill = recipe.getHighestRelevantSkillLevel(character) + inventoryItem.getMaintenanceMod(character);
                }
                inventoryItem.damageCheck(skill, 6.0f, false);
            }
            if (input.hasFlag(InputFlag.SharpnessCheck)) {
                skill = 0;
                if (character != null) {
                    skill = recipe.getHighestRelevantSkillLevel(character) + inventoryItem.getMaintenanceMod(character);
                }
                if (!inventoryItem.hasSharpness()) {
                    inventoryItem.damageCheck(skill, 6.0f, false);
                } else {
                    inventoryItem.sharpnessCheck(skill, 1.0f, false);
                }
            }
            if (input.hasFlag(InputFlag.Unseal) && inventoryItem.getFluidContainer() != null) {
                inventoryItem.getFluidContainer().unseal();
            }
            if (inventoryItem.hasTag(ItemTag.BREAK_ON_SMITHING) && this.recipe.isSmithing()) {
                inventoryItem.setCondition(0, true);
            }
            inventoryItem.syncItemFields();
        }
    }

    private boolean createOutputItems(OutputScript output, ItemDataList items, boolean testOnly, CacheData cacheData, IsoGameCharacter character) {
        if (output == null || output.getResourceType() != ResourceType.Item) {
            return false;
        }
        if (output.getIntAmount() <= 0) {
            return false;
        }
        Item outputItem = output.getItem(this);
        if (outputItem == null) {
            return false;
        }
        int createQty = output.getIntAmount();
        if (output.isVariableAmount()) {
            float ratio = testOnly ? this.calculatedVariableInputRatio : this.targetVariableInputRatio;
            createQty = Math.min((int)Math.ceil(output.getAmount() * ratio), output.getIntMaxAmount());
        }
        if (testOnly) {
            for (int i = 0; i < createQty; ++i) {
                items.addItem(outputItem);
            }
            return true;
        }
        if (this.recipe.hasTag(CraftRecipeTag.REMOVE_RESULT_ITEMS)) {
            return true;
        }
        for (int i = 0; i < createQty; ++i) {
            InventoryItem inheritFoodAgeItem;
            InventoryItem inheritNameItem;
            InventoryItem inheritWeightItem;
            InventoryItem copyClothingItem;
            InventoryItem inheritFavoriteItem;
            InventoryItem inheritUsesAndEmptyItem;
            InventoryItem inheritUsesItem;
            InventoryItem inheritSharpnessItem;
            InventoryItem inheritHeadConditionItem;
            InventoryItem isHeadPartItem;
            InventoryItem inheritConditionItem;
            InventoryItem inheritModelVariationItem;
            if (!CraftRecipeManager.createOutputItem(output, outputItem, testOnly, cacheData, character)) {
                DebugLog.CraftLogic.warn("Failed to create output item for: " + output.getOriginalLine());
                if (this.craftRecipeMonitor != null) {
                    this.craftRecipeMonitor.warn("Failed to create item: " + outputItem.getFullName());
                }
                return true;
            }
            if (cacheData.getMostRecentItem() == null) continue;
            InventoryItem item = cacheData.getMostRecentItem();
            items.addItem(item);
            InventoryItem inheritColorItem = this.getFirstInputItemWithFlag(InputFlag.InheritColor);
            if (inheritColorItem != null) {
                item.setColorRed(inheritColorItem.getColorRed());
                item.setColorGreen(inheritColorItem.getColorGreen());
                item.setColorBlue(inheritColorItem.getColorBlue());
                item.setColor(new Color(inheritColorItem.getColorRed(), inheritColorItem.getColorGreen(), inheritColorItem.getColorBlue()));
                item.setCustomColor(true);
            }
            if ((inheritModelVariationItem = this.getFirstInputItemWithFlag(InputFlag.InheritModelVariation)) != null) {
                item.setModelIndex(inheritModelVariationItem.getModelIndex());
                if (inheritModelVariationItem.getVisual() != null && item.getVisual() != null) {
                    item.getVisual().setTextureChoice(inheritModelVariationItem.getVisual().getTextureChoice());
                }
                item.synchWithVisual();
            }
            if ((inheritConditionItem = this.getFirstInputItemWithFlag(InputFlag.InheritCondition)) != null && !item.hasTag(ItemTag.DONT_INHERIT_CONDITION) && !output.hasFlag(OutputFlag.DontInheritCondition)) {
                item.setConditionFrom(inheritConditionItem);
                item.setHaveBeenRepaired(inheritConditionItem.getHaveBeenRepaired());
            }
            if ((isHeadPartItem = this.getFirstInputItemWithFlag(InputFlag.IsHeadPart)) != null && item.hasHeadCondition()) {
                item.setHeadConditionFromCondition(isHeadPartItem);
                if (item.hasSharpness() && isHeadPartItem.hasSharpness()) {
                    item.setSharpnessFrom(isHeadPartItem);
                }
                item.setTimesHeadRepaired(this.getFirstInputItemWithFlag(InputFlag.IsHeadPart).getHaveBeenRepaired());
            }
            if ((inheritHeadConditionItem = this.getFirstInputItemWithFlag(InputFlag.InheritHeadCondition)) != null && item.hasHeadCondition()) {
                item.setHeadCondition(inheritHeadConditionItem.getHeadCondition());
                if (item.hasSharpness() && inheritHeadConditionItem.hasSharpness()) {
                    item.setSharpnessFrom(inheritHeadConditionItem);
                }
                item.setTimesRepaired(inheritHeadConditionItem.getTimesHeadRepaired());
            } else if (inheritHeadConditionItem != null && !item.hasHeadCondition()) {
                item.setConditionFromHeadCondition(inheritHeadConditionItem);
                if (item.hasSharpness() && inheritHeadConditionItem.hasSharpness()) {
                    item.setSharpnessFrom(inheritHeadConditionItem);
                }
                item.setTimesRepaired(inheritHeadConditionItem.getTimesHeadRepaired());
            }
            InventoryItem inheritEquipped = this.getFirstInputItemWithFlag(InputFlag.InheritEquipped);
            if (inheritEquipped != null && inheritEquipped.isEquipped()) {
                if (character.isPrimaryHandItem(inheritEquipped)) {
                    character.setPrimaryHandItem(item);
                }
                if (character.isSecondaryHandItem(inheritEquipped)) {
                    character.setSecondaryHandItem(item);
                }
            }
            if ((inheritSharpnessItem = this.getFirstInputItemWithFlag(InputFlag.InheritSharpness)) != null && inheritSharpnessItem.hasSharpness() && item.hasSharpness()) {
                item.setSharpnessFrom(inheritSharpnessItem);
            }
            if ((inheritUsesItem = this.getFirstInputItemWithFlag(InputFlag.InheritUses)) != null) {
                if (item instanceof Radio) {
                    Radio radio = (Radio)item;
                    radio.getDeviceData().setPower(item.getCurrentUsesFloat());
                    radio.getDeviceData().setHasBattery(true);
                } else {
                    item.setCurrentUsesFrom(inheritUsesItem);
                }
            }
            if ((inheritUsesAndEmptyItem = this.getFirstInputItemWithFlag(InputFlag.InheritUsesAndEmpty)) != null) {
                item.setCurrentUsesFrom(inheritUsesAndEmptyItem);
                inheritUsesAndEmptyItem.setCurrentUsesFloat(0.0f);
            }
            if (inheritUsesAndEmptyItem instanceof Radio) {
                Radio radio = (Radio)inheritUsesAndEmptyItem;
                float power = radio.getDeviceData().getPower();
                int uses = (int)((float)item.getMaxUses() * power);
                item.setCurrentUses(uses);
                radio.getDeviceData().setPower(0.0f);
                radio.getDeviceData().setHasBattery(false);
                radio.getDeviceData().setIsTurnedOn(false);
                radio.getDeviceData().transmitBatteryChangeServer();
            }
            if ((inheritFavoriteItem = this.getFirstInputItemWithFlag(InputFlag.InheritFavorite)) != null) {
                item.setFavorite(inheritFavoriteItem.isFavorite());
            }
            InventoryItem inheritAmmunitionItem = this.getFirstInputItemWithFlag(InputFlag.InheritAmmunition);
            if (item instanceof HandWeapon) {
                HandWeapon weapon = (HandWeapon)item;
                if (inheritAmmunitionItem instanceof HandWeapon) {
                    HandWeapon handWeapon = (HandWeapon)inheritAmmunitionItem;
                    weapon.inheritAmmunition(handWeapon);
                }
            }
            if ((copyClothingItem = this.getFirstInputItemWithFlag(InputFlag.CopyClothing)) != null && item.getClothingItem() != null && copyClothingItem.getClothingItem() != null) {
                item.copyClothing(copyClothingItem);
            }
            if ((inheritWeightItem = this.getFirstInputItemWithFlag(InputFlag.InheritWeight)) != null) {
                item.setWeight(inheritWeightItem.getWeight() / 2.0f);
                item.setActualWeight(inheritWeightItem.getActualWeight() / 2.0f);
            }
            InventoryItem inheritFreezingItem = this.getFirstInputItemWithFlag(InputFlag.InheritFreezingTime);
            if (item instanceof Food) {
                Food newFoodItem = (Food)item;
                if (inheritFreezingItem instanceof Food) {
                    Food oldFoodItem = (Food)inheritFreezingItem;
                    newFoodItem.copyFrozenFrom(oldFoodItem);
                }
            }
            if ((inheritNameItem = this.getFirstInputItemWithFlag(InputFlag.InheritName)) != null) {
                item.setName(inheritNameItem.getDisplayName());
                item.setCustomName(true);
            }
            InventoryItem inheritFoodItem = this.getFirstInputItemWithFlag(InputFlag.InheritFood);
            if (item instanceof Food) {
                Food newFoodItem = (Food)item;
                if (inheritFoodItem instanceof Food) {
                    Food oldFoodItem = (Food)inheritFoodItem;
                    newFoodItem.copyFoodFromSplit(oldFoodItem, output.getIntAmount());
                }
            }
            InventoryItem inheritCookedItem = this.getFirstInputItemWithFlag(InputFlag.InheritCooked);
            if (item instanceof Food) {
                Food newFoodItem = (Food)item;
                if (inheritCookedItem instanceof Food) {
                    Food oldFoodItem = (Food)inheritCookedItem;
                    newFoodItem.copyCookedBurntFrom(oldFoodItem);
                }
            }
            if ((inheritFoodAgeItem = this.getFirstInputItemWithFlag(InputFlag.InheritFoodAge)) != null && item.isFood()) {
                float oldestAge = 0.0f;
                Food oldFood = null;
                for (int j = 0; j < this.getAllInputItemsWithFlag(InputFlag.InheritFoodAge).size(); ++j) {
                    InventoryItem foodInput = this.getAllInputItemsWithFlag(InputFlag.InheritFoodAge).get(j);
                    if (foodInput.getAge() > oldestAge) {
                        oldestAge = foodInput.getAge();
                        oldFood = (Food)foodInput;
                        ((Food)item).copyAgeFrom((Food)foodInput);
                    }
                    if (oldFood == null) continue;
                    ((Food)item).copyAgeFrom(oldFood);
                }
            }
            if (this.craftRecipeMonitor == null) continue;
            this.craftRecipeMonitor.log("created item = " + item.getFullType());
        }
        return true;
    }

    private void collectKeepItems(ItemDataList items, boolean testOnly) {
        for (int i = 0; i < this.inputs.size(); ++i) {
            InputScriptData input = this.inputs.get(i);
            if (input.inputScript.getResourceType() != ResourceType.Item || !input.isMoveToOutputs() || input.getAppliedItemsCount() == 0) continue;
            for (int j = 0; j < input.getAppliedItemsCount(); ++j) {
                items.addItem(input.getAppliedItem(j), true);
                if (this.craftRecipeMonitor == null) continue;
                this.craftRecipeMonitor.log("item = " + input.getAppliedItem(j).getFullType());
            }
        }
    }

    private void distributeItemsToResources(List<Resource> outputResources, ItemDataList items, boolean testOnly) {
        if (outputResources == null || items == null || outputResources.isEmpty() || items.size() == 0) {
            return;
        }
        if (this.craftRecipeMonitor != null) {
            this.craftRecipeMonitor.log("items = " + items.size());
            this.craftRecipeMonitor.log("hasUnprocessed = " + items.hasUnprocessed());
        }
        boolean doEmpty = false;
        int doubleSize = outputResources.size() * 2;
        for (int i = 0; i < doubleSize; ++i) {
            Resource resource;
            int index = i;
            if (!items.hasUnprocessed()) break;
            if (index >= outputResources.size()) {
                doEmpty = true;
                index -= outputResources.size();
            }
            if ((resource = outputResources.get(index)).getType() != ResourceType.Item || resource.isFull() || !doEmpty && resource.isEmpty() || doEmpty && !resource.isEmpty()) continue;
            int resourceCapacity = resource.getFreeItemCapacity();
            if (this.craftRecipeMonitor != null) {
                this.craftRecipeMonitor.log("testing resource '" + resource.getId() + "'");
                this.craftRecipeMonitor.log("capacity = " + resourceCapacity);
            }
            for (int j = 0; j < items.size() && resourceCapacity > 0 && items.hasUnprocessed(); ++j) {
                if (items.isProcessed(j)) continue;
                if (!testOnly) {
                    InventoryItem inventoryItem = items.getInventoryItem(j);
                    assert (inventoryItem != null);
                    if (this.craftRecipeMonitor != null) {
                        this.craftRecipeMonitor.log("-> testing item = " + inventoryItem.getFullType());
                    }
                    if (!CraftUtil.canResourceFitItem(resource, inventoryItem)) continue;
                    if (this.craftRecipeMonitor != null) {
                        this.craftRecipeMonitor.success("-> offloaded item '" + inventoryItem.getFullType() + "' to resource: " + resource.getId());
                    }
                    resource.offerItem(inventoryItem, true, true, true);
                    items.setProcessed(j);
                    --resourceCapacity;
                    continue;
                }
                Item item = items.getItem(j);
                assert (item != null);
                if (this.craftRecipeMonitor != null) {
                    this.craftRecipeMonitor.log("-> testing item = " + item.getFullName());
                }
                if (!CraftUtil.canResourceFitItem(resource, item)) continue;
                if (this.craftRecipeMonitor != null) {
                    this.craftRecipeMonitor.success("-> offloaded item '" + item.getFullName() + "' to resource: " + resource.getId());
                }
                items.setProcessed(j);
                --resourceCapacity;
            }
        }
    }

    private boolean createOutputToResources(OutputScript output, List<Resource> resources, boolean testOnly, CacheData cacheData) {
        if (output == null || resources == null || resources.isEmpty()) {
            return false;
        }
        Resource resource = null;
        switch (output.getResourceType()) {
            case Fluid: {
                resource = CraftUtil.findResourceOrEmpty(ResourceIO.Output, resources, output.getFluid(), output.getAmount(), null, this.usedResources);
                break;
            }
            case Energy: {
                resource = CraftUtil.findResourceOrEmpty(ResourceIO.Output, resources, output.getEnergy(), output.getAmount(), null, this.usedResources);
            }
        }
        if (resource != null) {
            boolean success = CraftRecipeManager.createOutputToResource(output, resource, testOnly, cacheData);
            if (!success) {
                cacheData.softReset();
            } else {
                this.usedResources.add(resource);
                if (this.craftRecipeMonitor != null) {
                    this.craftRecipeMonitor.success("created by resource: " + resource.getId());
                }
                return true;
            }
        }
        return false;
    }

    public void save(ByteBuffer output) throws IOException {
        ByteBlock block = ByteBlock.Start(output, ByteBlock.Mode.Save);
        output.put(this.recipe != null ? (byte)1 : 0);
        if (this.recipe != null) {
            GameWindow.WriteString(output, this.recipe.getScriptObjectFullType());
            output.putLong(this.recipe.getScriptVersion());
        }
        output.putDouble(this.elapsedTime);
        output.putInt(this.inputs.size());
        for (int i = 0; i < this.inputs.size(); ++i) {
            InputScriptData data = this.inputs.get(i);
            data.saveInputs(output);
        }
        output.put(this.modData != null && !this.modData.isEmpty() ? (byte)1 : 0);
        if (this.modData != null && !this.modData.isEmpty()) {
            this.modData.save(output);
        }
        output.put(this.hasConsumedInputs ? (byte)1 : 0);
        output.putFloat(this.targetVariableInputRatio);
        ByteBlock.End(output, block);
    }

    public boolean load(ByteBuffer input, int worldVersion, CraftRecipe recipe, boolean recipeInvalidated) throws IOException {
        ByteBlock block = ByteBlock.Start(input, ByteBlock.Mode.Load);
        block.safelyForceSkipOnEnd(true);
        boolean valid = true;
        if (!recipeInvalidated) {
            try {
                int size;
                if (worldVersion < 238 || input.get() != 0) {
                    String recipeName = GameWindow.ReadString(input);
                    recipe = ScriptManager.instance.getCraftRecipe(recipeName);
                    long scriptVersion = input.getLong();
                    if (recipe == null || scriptVersion != recipe.getScriptVersion()) {
                        valid = false;
                        DebugLog.General.warn("CraftRecipe '" + recipeName + "' is null (" + (recipe == null) + ", or has script version mismatch. Cancelling current craft.");
                    }
                }
                this.setRecipe(recipe);
                if (worldVersion >= 238) {
                    this.elapsedTime = input.getDouble();
                }
                if ((size = input.getInt()) != this.inputs.size()) {
                    DebugLog.CraftLogic.warn("Recipe inputs changed or mismatch with saved data.");
                    valid = false;
                } else {
                    for (int i = 0; i < size; ++i) {
                        InputScriptData data = this.inputs.get(i);
                        data.loadInputs(input, worldVersion);
                    }
                }
                if (input.get() != 0) {
                    if (this.modData == null) {
                        this.modData = LuaManager.platform.newTable();
                    }
                    this.modData.load(input, worldVersion);
                }
                boolean bl = this.hasConsumedInputs = input.get() != 0;
                if (worldVersion >= 235) {
                    this.calculatedVariableInputRatio = this.targetVariableInputRatio = input.getFloat();
                }
            }
            catch (Exception e) {
                e.printStackTrace();
                this.setRecipe(null);
                valid = false;
            }
        }
        if (recipeInvalidated || !valid) {
            this.setRecipe(null);
        }
        ByteBlock.End(input, block);
        return valid;
    }

    public KahluaTable getModData() {
        if (this.modData == null) {
            this.modData = LuaManager.platform.newTable();
        }
        return this.modData;
    }

    public String getModelHandOne() {
        return this.getModel(true);
    }

    public String getModelHandTwo() {
        return this.getModel(false);
    }

    private String getModel(boolean isHandOne) {
        if (this.recipe == null) {
            return null;
        }
        for (int i = 0; i < this.inputs.size(); ++i) {
            InputScriptData input = this.inputs.get(i);
            if (input.inputScript.getResourceType() != ResourceType.Item || (!isHandOne || !input.inputScript.isProp1()) && (isHandOne || !input.inputScript.isProp2()) || input.getMostRecentItem() == null) continue;
            return input.getMostRecentItem().getStaticModel();
        }
        if (this.recipe != null && this.recipe.getTimedActionScript() != null) {
            if (isHandOne) {
                return this.recipe.getTimedActionScript().getProp1();
            }
            return this.recipe.getTimedActionScript().getProp2();
        }
        return null;
    }

    public ArrayList<InventoryItem> getAllConsumedItems() {
        return this.getAllConsumedItems(new ArrayList<InventoryItem>());
    }

    public ArrayList<InventoryItem> getAllRecordedConsumedItems() {
        return this.getAllRecordedConsumedItems(new ArrayList<InventoryItem>());
    }

    public ArrayList<InventoryItem> getAllConsumedItems(ArrayList<InventoryItem> list) {
        return this.getAllConsumedItems(list, false);
    }

    public ArrayList<InventoryItem> getAllRecordedConsumedItems(ArrayList<InventoryItem> list) {
        return this.getAllConsumedItems(list, false, true);
    }

    public ArrayList<InventoryItem> getAllConsumedItems(ArrayList<InventoryItem> list, boolean includeKeep) {
        return this.getAllConsumedItems(list, includeKeep, false);
    }

    public ArrayList<InventoryItem> getAllConsumedItems(ArrayList<InventoryItem> list, boolean includeKeep, boolean onlyRecorded) {
        for (int i = 0; i < this.inputs.size(); ++i) {
            InputScriptData input = this.inputs.get(i);
            if (input.inputScript.getResourceType() != ResourceType.Item || input.inputScript.isKeep() && !includeKeep || onlyRecorded && !input.inputScript.isRecordInput()) continue;
            input.addAppliedItemsToList(list);
        }
        return list;
    }

    public ArrayList<InventoryItem> getAllKeepInputItems() {
        return this.getAllKeepInputItems(new ArrayList<InventoryItem>());
    }

    public ArrayList<InventoryItem> getAllKeepInputItems(ArrayList<InventoryItem> list) {
        for (int i = 0; i < this.inputs.size(); ++i) {
            InputScriptData input = this.inputs.get(i);
            if (input.inputScript.getResourceType() != ResourceType.Item || !input.inputScript.isKeep()) continue;
            input.addAppliedItemsToList(list);
        }
        return list;
    }

    public ArrayList<InventoryItem> getAllInputItemsWithFlag(InputFlag flag) {
        return this.getAllInputItemsWithFlag(flag.name());
    }

    public ArrayList<InventoryItem> getAllInputItemsWithFlag(String flag) {
        ArrayList<InventoryItem> list = new ArrayList<InventoryItem>();
        for (int i = 0; i < this.inputs.size(); ++i) {
            InputScriptData input = this.inputs.get(i);
            if (input.inputScript.getResourceType() != ResourceType.Item) continue;
            InputFlag flag2 = InputFlag.valueOf(flag);
            if (!input.getInputScript().hasFlag(flag2)) continue;
            input.addAppliedItemsToList(list);
        }
        return list;
    }

    public ArrayList<InventoryItem> getInputItems(Integer index) {
        if (this.inputs.get(index) != null) {
            InputScriptData input = this.inputs.get(index);
            if (input.inputScript.getResourceType() == ResourceType.Item) {
                return input.inputItems;
            }
        }
        return null;
    }

    public InventoryItem getFirstInputItemWithFlag(InputFlag flag) {
        return this.getFirstInputItemWithFlag(flag.name());
    }

    public InventoryItem getFirstInputItemWithFlag(String flag) {
        for (int i = 0; i < this.inputs.size(); ++i) {
            InventoryItem item;
            InputScriptData input = this.inputs.get(i);
            if (input.inputScript.getResourceType() != ResourceType.Item) continue;
            InputFlag flag2 = InputFlag.valueOf(flag);
            if (!input.getInputScript().hasFlag(flag2) || (item = input.getFirstAppliedItem()) == null) continue;
            return item;
        }
        return null;
    }

    public InventoryItem getFirstInputItemWithTag(ItemTag itemTag) {
        for (int i = 0; i < this.inputs.size(); ++i) {
            InventoryItem item;
            InputScriptData input = this.inputs.get(i);
            if (input.inputScript.getResourceType() != ResourceType.Item || (item = input.getFirstAppliedItem()) == null || !item.hasTag(itemTag)) continue;
            return item;
        }
        return null;
    }

    public ArrayList<InventoryItem> getAllInputItems() {
        ArrayList<InventoryItem> list = new ArrayList<InventoryItem>();
        for (int i = 0; i < this.inputs.size(); ++i) {
            InputScriptData input = this.inputs.get(i);
            if (input.inputScript.getResourceType() != ResourceType.Item) continue;
            input.addAppliedItemsToList(list);
        }
        return list;
    }

    public HashSet<String> getAppliedInputItemTypes(HashSet<String> appliedItemTypes) {
        ArrayList<InventoryItem> currentInputs = this.getAllInputItems();
        for (int i = 0; i < currentInputs.size(); ++i) {
            appliedItemTypes.add(currentInputs.get(i).getFullType());
        }
        return appliedItemTypes;
    }

    public ArrayList<InventoryItem> getAllDestroyInputItems() {
        ArrayList<InventoryItem> list = new ArrayList<InventoryItem>();
        for (int i = 0; i < this.inputs.size(); ++i) {
            InputScriptData input = this.inputs.get(i);
            if (input.inputScript.getResourceType() != ResourceType.Item || !input.isDestroy()) continue;
            input.addAppliedItemsToList(list);
        }
        return list;
    }

    public ArrayList<InventoryItem> getAllPutBackInputItems() {
        ArrayList<InventoryItem> list = new ArrayList<InventoryItem>();
        for (int i = 0; i < this.inputs.size(); ++i) {
            InputScriptData input = this.inputs.get(i);
            if (input.inputScript.getResourceType() != ResourceType.Item || input.getInputScript().dontPutBack()) continue;
            input.addAppliedItemsToList(list);
        }
        return list;
    }

    public ArrayList<InventoryItem> getAllNotKeepInputItems() {
        ArrayList<InventoryItem> list = new ArrayList<InventoryItem>();
        for (int i = 0; i < this.inputs.size(); ++i) {
            InputScriptData input = this.inputs.get(i);
            if (input.inputScript.getResourceType() != ResourceType.Item || input.inputScript.isKeep()) continue;
            input.addAppliedItemsToList(list);
        }
        return list;
    }

    public InventoryItem getFirstCreatedItem() {
        if (!this.getAllCreatedItems().isEmpty()) {
            return this.getAllCreatedItems().get(0);
        }
        return null;
    }

    public ArrayList<InventoryItem> getAllCreatedItems() {
        return this.getAllCreatedItems(new ArrayList<InventoryItem>());
    }

    public ArrayList<InventoryItem> getAllCreatedItems(ArrayList<InventoryItem> list) {
        for (int i = 0; i < this.outputs.size(); ++i) {
            OutputScriptData output = this.outputs.get(i);
            if (output.outputScript.getResourceType() != ResourceType.Item) continue;
            output.addAppliedItemsToList(list);
        }
        return list;
    }

    public FluidSample getFirstInputFluidWithFlag(InputFlag flag) {
        return this.getFirstInputFluidWithFlag(flag.name());
    }

    public FluidSample getFirstInputFluidWithFlag(String flag) {
        for (int i = 0; i < this.inputs.size(); ++i) {
            InputScriptData input = this.inputs.get(i);
            if (input.getInputScript().getConsumeFromItemScript() == null) continue;
            InputFlag flag2 = InputFlag.valueOf(flag);
            InputScript childScript = input.getInputScript().getConsumeFromItemScript();
            if (!childScript.hasFlag(flag2)) continue;
            InputScriptData data = this.getDataForInputScript(childScript);
            FluidSample sample = data.fluidSample;
            if (sample == null) continue;
            return sample;
        }
        return null;
    }

    public int getAllViableItemsCount() {
        return this.allViableItems.size();
    }

    public InventoryItem getViableItem(int index) {
        return this.allViableItems.get(index);
    }

    public int getAllViableResourcesCount() {
        return this.allViableResources.size();
    }

    public Resource getViableResource(int index) {
        return this.allViableResources.get(index);
    }

    private void destroyAllSurvivingDestroyInputs() {
        ArrayList<InventoryItem> list = this.getAllDestroyInputItems();
        for (int i = 0; i < list.size(); ++i) {
            InventoryItem item = list.get(i);
            if (item == null) continue;
            DebugLog.CraftLogic.debugln("Destroying surviving destroy input item " + item.getFullType());
            ItemUser.RemoveItem(item);
        }
    }

    public boolean isVariableAmount() {
        return this.inputs.stream().anyMatch(inputScriptData -> inputScriptData.inputScript.isVariableAmount());
    }

    public float getVariableInputRatio() {
        if (this.calculatedVariableInputRatio == Float.MAX_VALUE) {
            return 1.0f;
        }
        return this.calculatedVariableInputRatio;
    }

    public void setTargetVariableInputRatio(float target) {
        this.targetVariableInputRatio = target;
    }

    public void clearTargetVariableInputRatio() {
        this.targetVariableInputRatio = Float.MAX_VALUE;
    }

    public void addOverfilledResource(InputScript input, HashMap<Resource, ArrayList<InventoryItem>> resources) {
        this.variableInputOverfilledResources.put(input, resources);
    }

    public float getCalculatedVariableInputRatio() {
        return this.calculatedVariableInputRatio;
    }

    public void setCalculatedVariableInputRatio(float value) {
        this.calculatedVariableInputRatio = value;
    }

    public ArrayList<InventoryItem> getManualInputsFor(InputScript inputScript, ArrayList<InventoryItem> list) {
        if (this.getRecipe() == null || !this.getRecipe().containsIO(inputScript)) {
            return list;
        }
        InputScriptData data = this.getDataForInputScript(inputScript);
        if (inputScript.getResourceType() == ResourceType.Item) {
            data.getManualInputItems(list);
            for (InventoryItem inventoryItem : list) {
                DebugLog.CraftLogic.println("get m-input: " + inventoryItem.getFullType());
            }
        }
        return list;
    }

    public void clearManualInputs() {
        this.clearManualInputs(null);
    }

    public void clearManualInputs(InputScriptData input) {
        for (int i = 0; i < this.inputs.size(); ++i) {
            InputScriptData data = this.inputs.get(i);
            if (input != null && input != data) continue;
            while (data.getLastInputItem() != null) {
                data.removeInputItem(data.getLastInputItem());
            }
        }
    }

    public boolean setManualInputsFor(InputScript inputScript, ArrayList<InventoryItem> list) {
        if (this.getRecipe() == null || !this.getRecipe().containsIO(inputScript)) {
            return false;
        }
        InputScriptData data = this.getDataForInputScript(inputScript);
        if (inputScript.getResourceType() == ResourceType.Item) {
            while (data.getLastInputItem() != null) {
                data.removeInputItem(data.getLastInputItem());
            }
            for (int i = 0; i < list.size(); ++i) {
                InventoryItem inputItem = list.get(i);
                if (inputItem.getContainer() == null && (inputItem.getWorldItem() == null || inputItem.getWorldItem().getWorldObjectIndex() == -1) || this.containsInputItem(inputItem) || !data.addInputItem(inputItem)) continue;
                DebugLog.CraftLogic.println("add m-input: " + list.get(i).getFullType());
            }
            return data.isInputItemsSatisfied();
        }
        return false;
    }

    public void populateInputs(List<InventoryItem> inputItems, List<Resource> resources, boolean clearExisting) {
        if (this.getRecipe() != null) {
            ArrayList<InputScript> inputScripts = this.getRecipe().getInputs();
            block0: for (int i = 0; i < inputScripts.size(); ++i) {
                InputScript inputScript = inputScripts.get(i);
                InputScriptData inputScriptData = this.getDataForInputScript(inputScript);
                if (clearExisting) {
                    while (inputScriptData.getLastInputItem() != null) {
                        inputScriptData.removeInputItem(inputScriptData.getLastInputItem());
                    }
                }
                if (inputScriptData.isInputItemsSatisfied()) continue;
                for (int j = 0; j < inputItems.size(); ++j) {
                    this.offerInputItem(inputScript, inputItems.get(j));
                    if (inputScriptData.isInputItemsSatisfied()) continue block0;
                }
            }
        }
    }

    public void setEatPercentage(int percentage) {
        percentage = Math.min(percentage, 100);
        this.eatPercentage = percentage = Math.max(percentage, 0);
    }

    public int getEatPercentage() {
        return this.eatPercentage;
    }

    public double getElapsedTime() {
        return this.elapsedTime;
    }

    public void setElapsedTime(double elapsedTime) {
        this.elapsedTime = elapsedTime;
    }

    public boolean isFinished() {
        return this.getElapsedTime() >= (double)this.getRecipe().getTime();
    }

    @UsedFromLua
    public static class InputScriptData
    extends CacheData {
        private CraftRecipeData recipeData;
        private InputScript inputScript;
        private final ArrayList<InventoryItem> inputItems = new ArrayList();
        private static final ArrayDeque<InputScriptData> pool = new ArrayDeque();

        private static InputScriptData Alloc(CraftRecipeData recipeData, InputScript inputScript) {
            InputScriptData data = pool.poll();
            if (data == null) {
                data = new InputScriptData();
            }
            data.recipeData = recipeData;
            data.inputScript = inputScript;
            return data;
        }

        private static void Release(InputScriptData data) {
        }

        @Override
        protected CraftRecipeData getRecipeData() {
            return this.recipeData;
        }

        private void reset() {
            this.clearCache();
            this.inputScript = null;
            this.inputItems.clear();
            this.recipeData = null;
            this.cachedCanConsume = false;
        }

        public InputScript getInputScript() {
            return this.inputScript;
        }

        public boolean isCachedCanConsume() {
            return this.cachedCanConsume;
        }

        public void getManualInputItems(ArrayList<InventoryItem> list) {
            list.addAll(this.inputItems);
        }

        public int getInputItemCount() {
            return this.inputItems.size();
        }

        public int getInputItemUses() {
            if (this.getInputScript().isItemCount()) {
                return this.getInputItemCount();
            }
            int totalUses = 0;
            for (int i = 0; i < this.inputItems.size(); ++i) {
                if (this.inputItems.get(i) == null) continue;
                totalUses += this.inputItems.get(i).getCurrentUses();
            }
            return totalUses;
        }

        public float getInputItemFluidUses() {
            float uses = 0.0f;
            for (int i = 0; i < this.inputItems.size(); ++i) {
                FluidContainer fluidContainer;
                if (this.inputItems.get(i) == null || (fluidContainer = this.inputItems.get(i).getFluidContainer()) == null) continue;
                uses += fluidContainer.getAmount();
            }
            return uses;
        }

        public InventoryItem getFirstInputItem() {
            if (!this.inputItems.isEmpty()) {
                return this.inputItems.get(0);
            }
            return null;
        }

        public InventoryItem getLastInputItem() {
            if (!this.inputItems.isEmpty()) {
                return this.inputItems.get(this.inputItems.size() - 1);
            }
            return null;
        }

        public boolean isInputItemsSatisfied() {
            if (this.inputScript.getResourceType() == ResourceType.Item && !this.inputItems.isEmpty()) {
                return this.recipeData.consumeInputFromItems(this.inputScript, this.inputItems, true, null, true, this.recipeData.getCharacter());
            }
            return false;
        }

        public boolean isInputItemsSatisifiedToMaximum() {
            if (this.inputScript.getResourceType() == ResourceType.Item && !this.inputItems.isEmpty() && this.recipeData.consumeInputFromItems(this.inputScript, this.inputItems, true, null, true, this.recipeData.getCharacter())) {
                int usedItems = this.recipeData.usedItems.size();
                int overfilledItems = this.recipeData.variableInputOverfilledItems.containsKey(this.inputScript) ? this.recipeData.variableInputOverfilledItems.get(this.inputScript).size() : 0;
                int targetFillAmount = (int)Math.min(this.inputScript.getAmount() * this.recipeData.targetVariableInputRatio, (float)this.inputScript.getIntMaxAmount());
                return usedItems + overfilledItems >= targetFillAmount;
            }
            return false;
        }

        public boolean acceptsInputItem(InventoryItem inventoryItem) {
            if (this.inputScript.getResourceType() == ResourceType.Item) {
                if (!this.inputItems.isEmpty() && this.inputItems.contains(inventoryItem)) {
                    return false;
                }
                return CraftRecipeManager.consumeInputItem(this.inputScript, inventoryItem, true, null, this.recipeData.getCharacter());
            }
            return false;
        }

        public boolean addInputItem(InventoryItem inventoryItem) {
            if (this.inputScript.getResourceType() == ResourceType.Item) {
                if (!(!this.acceptsInputItem(inventoryItem) || this.isInputItemsSatisfied() && this.isInputItemsSatisifiedToMaximum())) {
                    this.inputItems.add(inventoryItem);
                    return true;
                }
            } else {
                DebugLog.CraftLogic.warn("input script does not accept items, line=" + this.inputScript.getOriginalLine());
            }
            return false;
        }

        public boolean removeInputItem(InventoryItem item) {
            return this.inputItems.remove(item);
        }

        public void verifyInputItems(ArrayList<InventoryItem> playerItems) {
            for (int i = this.inputItems.size() - 1; i >= 0; --i) {
                InventoryItem inventoryItem = this.inputItems.get(i);
                boolean accepts = false;
                if (this.inputScript.getResourceType() == ResourceType.Item) {
                    accepts = CraftRecipeManager.consumeInputItem(this.inputScript, inventoryItem, true, null, this.recipeData.getCharacter());
                }
                if (this.inputScript.isKeep() && playerItems.contains(inventoryItem) && accepts) continue;
                DebugLog.CraftLogic.println(" :: REMOVING ITEM: " + inventoryItem.getFullType() + " [0]=" + (!this.inputScript.isKeep() || !this.inputScript.isTool()) + ", [1]=" + !playerItems.contains(inventoryItem) + ", [2]=" + !accepts);
                this.inputItems.remove(i);
            }
        }

        public boolean isDestroy() {
            return this.getInputScript().isDestroy();
        }
    }

    @UsedFromLua
    public static class OutputScriptData
    extends CacheData {
        private static final ArrayDeque<OutputScriptData> pool = new ArrayDeque();
        private CraftRecipeData recipeData;
        private OutputScript outputScript;

        private static OutputScriptData Alloc(CraftRecipeData recipeData, OutputScript outputScript) {
            OutputScriptData data = pool.poll();
            if (data == null) {
                data = new OutputScriptData();
            }
            data.recipeData = recipeData;
            data.outputScript = outputScript;
            return data;
        }

        private static void Release(OutputScriptData data) {
        }

        @Override
        protected CraftRecipeData getRecipeData() {
            return this.recipeData;
        }

        public OutputScript getOutputScript() {
            return this.outputScript;
        }
    }

    @UsedFromLua
    public static abstract class CacheData {
        protected InventoryItem mostRecentItem;
        private final ArrayList<InventoryItem> appliedItems = new ArrayList();
        private boolean moveToOutputs;
        protected float usesConsumed;
        protected float fluidConsumed;
        protected float energyConsumed;
        protected FluidSample fluidSample = FluidSample.Alloc();
        protected FluidConsume fluidConsume = FluidConsume.Alloc();
        protected float usesCreated;
        protected float fluidCreated;
        protected float energyCreated;
        protected boolean cachedCanConsume;

        protected void addAppliedItem(InventoryItem inventoryItem) {
            assert (!this.appliedItems.contains(inventoryItem)) : "Item already added to applied list.";
            this.appliedItems.add(inventoryItem);
            this.mostRecentItem = inventoryItem;
        }

        public int getAppliedItemsCount() {
            return this.appliedItems.size();
        }

        public boolean hasAppliedItem(InventoryItem item) {
            return this.appliedItems.contains(item);
        }

        public boolean hasAppliedItemType(Item item) {
            for (int i = 0; i < this.appliedItems.size(); ++i) {
                if (this.appliedItems.get(i).getScriptItem() != item) continue;
                return true;
            }
            return false;
        }

        public InventoryItem getMostRecentItem() {
            return this.mostRecentItem;
        }

        protected void setMostRecentItemNull() {
            this.mostRecentItem = null;
        }

        public InventoryItem getAppliedItem(int index) {
            return this.appliedItems.get(index);
        }

        public InventoryItem getFirstAppliedItem() {
            if (this.appliedItems.isEmpty()) {
                return null;
            }
            return this.appliedItems.get(0);
        }

        public void addAppliedItemsToList(ArrayList<InventoryItem> items) {
            PZArrayUtil.addAll(items, this.appliedItems);
        }

        protected abstract CraftRecipeData getRecipeData();

        protected void clearCache() {
            this.moveToOutputs = false;
            this.mostRecentItem = null;
            this.appliedItems.clear();
            this.usesConsumed = 0.0f;
            this.fluidConsumed = 0.0f;
            this.energyConsumed = 0.0f;
            this.fluidSample.clear();
            this.fluidConsume.clear();
            this.usesCreated = 0.0f;
            this.fluidCreated = 0.0f;
            this.energyCreated = 0.0f;
        }

        public boolean isMoveToOutputs() {
            return this.moveToOutputs;
        }

        public void setMoveToOutputs(boolean b) {
            this.moveToOutputs = b;
        }

        protected void softReset() {
            this.softResetInput();
            this.softResetOutput();
        }

        protected void softResetInput() {
            this.mostRecentItem = null;
            if (!this.appliedItems.isEmpty()) {
                this.appliedItems.clear();
            }
            this.fluidSample.clear();
            this.fluidConsume.clear();
            this.usesConsumed = 0.0f;
            this.fluidConsumed = 0.0f;
            this.energyConsumed = 0.0f;
        }

        protected void softResetOutput() {
            if (!this.appliedItems.isEmpty()) {
                this.appliedItems.clear();
            }
            this.usesCreated = 0.0f;
            this.fluidCreated = 0.0f;
            this.energyCreated = 0.0f;
        }

        protected void saveInputs(ByteBuffer output) throws IOException {
            output.put(this.moveToOutputs ? (byte)1 : 0);
            output.putFloat(this.usesConsumed);
            output.putFloat(this.fluidConsumed);
            output.putFloat(this.energyConsumed);
            FluidSample.Save(this.fluidSample, output);
            FluidConsume.Save(this.fluidConsume, output);
            if (this.appliedItems.size() == 1) {
                CompressIdenticalItems.save(output, this.appliedItems.get(0));
            } else {
                CompressIdenticalItems.save(output, this.appliedItems, null);
            }
            CompressIdenticalItems.save(output, this.mostRecentItem);
            output.put(this.cachedCanConsume ? (byte)1 : 0);
        }

        protected void loadInputs(ByteBuffer input, int worldVersion) throws IOException {
            this.moveToOutputs = input.get() != 0;
            this.usesConsumed = input.getFloat();
            this.fluidConsumed = input.getFloat();
            this.energyConsumed = input.getFloat();
            FluidSample.Load(this.fluidSample, input, worldVersion);
            FluidConsume.Load(this.fluidConsume, input, worldVersion);
            this.appliedItems.clear();
            CompressIdenticalItems.load(input, worldVersion, this.appliedItems, null);
            ArrayList<InventoryItem> mostRecentItemArray = new ArrayList<InventoryItem>();
            CompressIdenticalItems.load(input, worldVersion, mostRecentItemArray, null);
            if (!mostRecentItemArray.isEmpty()) {
                this.mostRecentItem = mostRecentItemArray.get(0);
            }
            this.cachedCanConsume = input.get() != 0;
        }
    }
}

