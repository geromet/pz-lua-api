/*
 * Decompiled with CFR 0.152.
 */
package zombie.entity.components.crafting;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import zombie.GameWindow;
import zombie.UsedFromLua;
import zombie.characters.IsoPlayer;
import zombie.core.Translator;
import zombie.core.network.ByteBufferReader;
import zombie.core.textures.Texture;
import zombie.core.utils.UpdateLimit;
import zombie.debug.DebugLog;
import zombie.debug.objects.DebugClassFields;
import zombie.entity.Component;
import zombie.entity.ComponentType;
import zombie.entity.GameEntityNetwork;
import zombie.entity.components.crafting.CraftMode;
import zombie.entity.components.crafting.CraftRecipeMonitor;
import zombie.entity.components.crafting.CraftUtil;
import zombie.entity.components.crafting.StartMode;
import zombie.entity.components.crafting.recipe.CraftRecipeData;
import zombie.entity.components.crafting.recipe.CraftRecipeManager;
import zombie.entity.components.crafting.recipe.ItemDataList;
import zombie.entity.components.resources.Resource;
import zombie.entity.components.resources.ResourceBlueprint;
import zombie.entity.components.resources.ResourceChannel;
import zombie.entity.components.resources.ResourceFactory;
import zombie.entity.components.resources.ResourceFlag;
import zombie.entity.components.resources.ResourceGroup;
import zombie.entity.components.resources.ResourceIO;
import zombie.entity.components.resources.ResourceItem;
import zombie.entity.components.resources.ResourceType;
import zombie.entity.components.resources.Resources;
import zombie.entity.components.spriteconfig.SpriteOverlayConfig;
import zombie.entity.events.ComponentEvent;
import zombie.entity.network.EntityPacketData;
import zombie.entity.network.EntityPacketType;
import zombie.entity.util.enums.EnumBitStore;
import zombie.inventory.InventoryItem;
import zombie.network.GameClient;
import zombie.network.GameServer;
import zombie.network.IConnection;
import zombie.network.fields.character.PlayerID;
import zombie.scripting.entity.ComponentScript;
import zombie.scripting.entity.components.crafting.CraftLogicScript;
import zombie.scripting.entity.components.crafting.CraftRecipe;
import zombie.scripting.objects.Item;
import zombie.scripting.objects.SpriteOverlayConfigKey;
import zombie.ui.ObjectTooltip;
import zombie.util.StringUtils;
import zombie.util.io.BitHeader;
import zombie.util.io.BitHeaderRead;
import zombie.util.io.BitHeaderWrite;
import zombie.util.list.PZUnmodifiableList;

@DebugClassFields
@UsedFromLua
public class CraftLogic
extends Component {
    private static final List<CraftRecipe> _emptyRecipeList = PZUnmodifiableList.wrap(new ArrayList());
    private static final List<Resource> _emptyResourceList = PZUnmodifiableList.wrap(new ArrayList());
    private String recipeTagQuery;
    private List<CraftRecipe> recipes;
    private StartMode startMode = StartMode.Manual;
    private boolean doAutomaticCraftCheck = true;
    private boolean startRequested;
    private boolean stopRequested;
    private IsoPlayer requestingPlayer;
    protected CraftRecipeData craftData;
    private final CraftRecipeData craftTestData;
    private String inputsGroupName;
    private String outputsGroupName;
    private String actionAnimOverride;
    private final ArrayList<CraftRecipeData> craftDataInProgress = new ArrayList();
    UpdateLimit limit = new UpdateLimit(1000L);

    private CraftLogic() {
        super(ComponentType.CraftLogic);
        this.craftData = CraftRecipeData.Alloc(CraftMode.Automation, true, false, true, false);
        this.craftTestData = CraftRecipeData.Alloc(CraftMode.Automation, true, false, true, false);
    }

    protected CraftLogic(ComponentType type) {
        super(type);
        this.craftData = CraftRecipeData.Alloc(CraftMode.Automation, true, false, true, false);
        this.craftTestData = CraftRecipeData.Alloc(CraftMode.Automation, true, false, true, false);
    }

    protected void readFromScript(ComponentScript componentScript) {
        super.readFromScript(componentScript);
        CraftLogicScript script = (CraftLogicScript)componentScript;
        this.startMode = script.getStartMode();
        this.recipeTagQuery = null;
        this.setRecipeTagQuery(script.getRecipeTagQuery());
        this.inputsGroupName = script.getInputsGroupName();
        this.outputsGroupName = script.getOutputsGroupName();
        this.actionAnimOverride = script.getActionAnim();
        this.doAutomaticCraftCheck = this.startMode == StartMode.Automatic;
    }

    @Override
    public boolean isValid() {
        return super.isValid() && this.owner.hasComponent(ComponentType.Resources);
    }

    @Override
    protected void reset() {
        super.reset();
        this.recipeTagQuery = null;
        this.recipes = null;
        this.craftData.reset();
        this.craftTestData.reset();
        this.startMode = StartMode.Manual;
        this.doAutomaticCraftCheck = true;
        this.startRequested = false;
        this.stopRequested = false;
        this.requestingPlayer = null;
        this.inputsGroupName = null;
        this.outputsGroupName = null;
        this.actionAnimOverride = null;
        this.craftDataInProgress.clear();
    }

    public StartMode getStartMode() {
        return this.startMode;
    }

    public boolean isStartRequested() {
        return this.startRequested;
    }

    void setStartRequested(boolean b) {
        this.startRequested = b;
    }

    public boolean isStopRequested() {
        return this.stopRequested;
    }

    void setStopRequested(boolean b) {
        this.stopRequested = b;
    }

    public IsoPlayer getRequestingPlayer() {
        return this.requestingPlayer;
    }

    void setRequestingPlayer(IsoPlayer player) {
        this.requestingPlayer = player;
    }

    public boolean isDoAutomaticCraftCheck() {
        return this.doAutomaticCraftCheck;
    }

    void setDoAutomaticCraftCheck(boolean b) {
        this.doAutomaticCraftCheck = b;
    }

    CraftRecipeData getPendingCraftData() {
        return this.craftData;
    }

    CraftRecipeData getFirstInProgressCraftData() {
        return !this.craftDataInProgress.isEmpty() ? this.craftDataInProgress.get(0) : null;
    }

    ArrayList<CraftRecipeData> getAllInProgressCraftData() {
        return this.craftDataInProgress;
    }

    boolean isCraftingMixedRecipes() {
        Item firstOutputItem = null;
        List<Resource> outputResources = this.getOutputResources();
        for (Resource resource : outputResources) {
            Object inventoryItem;
            if (!(resource instanceof ResourceItem)) continue;
            if (firstOutputItem == null && (inventoryItem = resource.peekItem()) != null) {
                firstOutputItem = ((InventoryItem)inventoryItem).getScriptItem();
            }
            if (firstOutputItem == null) continue;
            inventoryItem = ((ResourceItem)resource).getStoredItems().iterator();
            while (inventoryItem.hasNext()) {
                InventoryItem inventoryItem2 = (InventoryItem)inventoryItem.next();
                if (firstOutputItem.equals(inventoryItem2.getScriptItem())) continue;
                return true;
            }
        }
        if (!this.craftDataInProgress.isEmpty()) {
            CraftRecipe firstRecipe = ((CraftRecipeData)this.craftDataInProgress.getFirst()).getRecipe();
            for (CraftRecipeData recipeData : this.craftDataInProgress) {
                if (!firstRecipe.equals(recipeData.getRecipe())) {
                    return true;
                }
                if (firstOutputItem == null) continue;
                for (int i = 0; i < recipeData.getToOutputItems().size(); ++i) {
                    Item item = recipeData.getToOutputItems().getItem(i);
                    if (firstOutputItem.equals(item)) continue;
                    return true;
                }
            }
        }
        return false;
    }

    public int getActiveCraftCount() {
        return this.craftDataInProgress.size();
    }

    CraftRecipeData getCraftTestData() {
        return this.craftTestData;
    }

    public String getInputsGroupName() {
        return this.inputsGroupName;
    }

    public String getOutputsGroupName() {
        return this.outputsGroupName;
    }

    public String getActionAnimOverride() {
        return this.actionAnimOverride;
    }

    public String getRecipeTagQuery() {
        return this.recipeTagQuery;
    }

    public void setRecipeTagQuery(String recipeTagQuery) {
        if (this.recipeTagQuery == null || !this.recipeTagQuery.equalsIgnoreCase(recipeTagQuery)) {
            this.recipeTagQuery = recipeTagQuery;
            this.recipes = null;
            if (!StringUtils.isNullOrWhitespace(this.recipeTagQuery)) {
                this.recipes = CraftRecipeManager.queryRecipes(recipeTagQuery);
            }
        }
    }

    public ArrayList<CraftRecipe> getRecipes(ArrayList<CraftRecipe> list) {
        list.clear();
        if (this.recipes != null) {
            list.addAll(this.recipes);
        }
        return list;
    }

    public List<CraftRecipe> getRecipes() {
        if (this.recipes != null) {
            return this.recipes;
        }
        return _emptyRecipeList;
    }

    public List<Resource> getInputResources() {
        ResourceGroup group;
        Resources resources = (Resources)this.getComponent(ComponentType.Resources);
        if (resources != null && (group = resources.getResourceGroup(this.inputsGroupName)) != null) {
            return group.getResources();
        }
        return _emptyResourceList;
    }

    public List<Resource> getOutputResources() {
        ResourceGroup group;
        Resources resources = (Resources)this.getComponent(ComponentType.Resources);
        if (resources != null && (group = resources.getResourceGroup(this.outputsGroupName)) != null) {
            return group.getResources();
        }
        return _emptyResourceList;
    }

    public boolean isRunning() {
        return !this.craftDataInProgress.isEmpty();
    }

    public CraftRecipe getCurrentRecipe() {
        return this.getCraftTestData().getRecipe();
    }

    public double getProgress(CraftRecipeData craftRecipeData) {
        if (craftRecipeData == null || craftRecipeData.getElapsedTime() == 0.0) {
            return 0.0;
        }
        if (craftRecipeData.getElapsedTime() >= (double)craftRecipeData.getRecipe().getTime()) {
            return 1.0;
        }
        return craftRecipeData.getElapsedTime() / (double)craftRecipeData.getRecipe().getTime();
    }

    public void setRecipe(CraftRecipe recipe) {
        if (this.craftData.getRecipe() != recipe) {
            this.doAutomaticCraftCheck = this.startMode == StartMode.Automatic;
            this.craftData.setRecipe(recipe);
            this.craftTestData.setRecipe(recipe);
        }
    }

    public CraftRecipe getPossibleRecipe() {
        List<Resource> inputs = this.getInputResources();
        List<Resource> outputs = this.getOutputResources();
        if (inputs == null || inputs.isEmpty()) {
            return null;
        }
        return CraftUtil.getPossibleRecipe(this.craftTestData, this.recipes, inputs, outputs);
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public CraftRecipeMonitor debugCanStart(IsoPlayer player) {
        try {
            DebugLog.General.println("=== Starting debug canStart test ===");
            CraftRecipeMonitor monitor = CraftRecipeMonitor.Create();
            if (!this.isValid()) {
                monitor.warn("Unable to start (not valid).");
                monitor.close();
                CraftRecipeMonitor craftRecipeMonitor = monitor.seal();
                return craftRecipeMonitor;
            }
            if (this.startMode == StartMode.Manual && !this.owner.isUsingPlayer(player)) {
                monitor.warn("Player is not the using player.");
                monitor.close();
                CraftRecipeMonitor craftRecipeMonitor = monitor.seal();
                return craftRecipeMonitor;
            }
            monitor.logCraftLogic(this);
            List<Resource> inputs = this.getInputResources();
            List<Resource> outputs = this.getOutputResources();
            monitor.logResources(inputs, outputs);
            CraftRecipeMonitor craftRecipeMonitor = CraftUtil.debugCanStart(player, this.craftTestData, this.recipes, inputs, outputs, monitor);
            return craftRecipeMonitor;
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        finally {
            this.craftData.setMonitor(null);
            this.craftTestData.setMonitor(null);
        }
        return null;
    }

    public boolean canStart(IsoPlayer player) {
        return this.canStart(StartMode.Manual, player);
    }

    protected boolean canStart(StartMode startMode, IsoPlayer player) {
        if (this.isInvalidStartMode(startMode, player)) {
            return false;
        }
        List<Resource> inputs = this.getInputResources();
        if (inputs == null || inputs.isEmpty() || !this.willOutputsAccommodate(this.craftTestData.getToOutputItems())) {
            return false;
        }
        return CraftUtil.canPerformRecipe(this.craftTestData.getRecipe(), this.craftTestData, inputs, this.getOutputResources());
    }

    private boolean isInvalidStartMode(StartMode startMode, IsoPlayer player) {
        return !this.isValid() || this.startMode == StartMode.Passive || this.startMode != startMode || this.startMode == StartMode.Manual && !this.owner.isUsingPlayer(player);
    }

    public boolean canStartWithInventoryItems(IsoPlayer player, List<InventoryItem> selectedInventoryItems) {
        if (this.isInvalidStartMode(this.startMode, player)) {
            return false;
        }
        ArrayList<Resource> inputs = new ArrayList<Resource>(this.getInputResources());
        List<Resource> outputs = this.getOutputResources();
        if (inputs.isEmpty() && (selectedInventoryItems == null || selectedInventoryItems.isEmpty())) {
            return false;
        }
        ResourceItem temporarySelectedInventoryItemsResource = null;
        if (selectedInventoryItems != null && !selectedInventoryItems.isEmpty() && this.willInputsAccommodate(selectedInventoryItems)) {
            ResourceBlueprint temporaryResourceBlueprint = ResourceBlueprint.alloc("temp", ResourceType.Item, ResourceIO.Input, selectedInventoryItems.size(), null, ResourceChannel.NO_CHANNEL, EnumBitStore.of(ResourceFlag.Temporary));
            temporarySelectedInventoryItemsResource = (ResourceItem)ResourceFactory.createResource(temporaryResourceBlueprint);
            ResourceBlueprint.release(temporaryResourceBlueprint);
            for (InventoryItem item : selectedInventoryItems) {
                temporarySelectedInventoryItemsResource.offerItem(item, true, true, false);
            }
            inputs.add(temporarySelectedInventoryItemsResource);
        }
        boolean craftSuccess = CraftUtil.canPerformRecipe(this.craftTestData.getRecipe(), this.craftTestData, inputs, outputs);
        if (temporarySelectedInventoryItemsResource != null) {
            ResourceFactory.releaseResource(temporarySelectedInventoryItemsResource);
        }
        return craftSuccess && this.willOutputsAccommodate(this.craftTestData.getToOutputItems());
    }

    public boolean willInputsAccommodate(List<InventoryItem> inventoryItems) {
        if (inventoryItems == null || inventoryItems.isEmpty()) {
            return true;
        }
        List<Resource> inputs = this.getInputResources();
        HashMap<ResourceItem, Integer> resourceCapacityMap = new HashMap<ResourceItem, Integer>();
        HashMap<ResourceItem, Item> resourceItemMap = new HashMap<ResourceItem, Item>();
        for (Resource input : inputs) {
            if (!(input instanceof ResourceItem)) continue;
            ResourceItem resourceItem = (ResourceItem)input;
            resourceCapacityMap.put(resourceItem, input.getFreeItemCapacity());
            Item firstResourceItem = input.isEmpty() ? null : input.peekItem().getScriptItem();
            resourceItemMap.put(resourceItem, firstResourceItem);
        }
        int allocatedItems = 0;
        block1: for (InventoryItem inventoryItem : inventoryItems) {
            for (Map.Entry entry : resourceCapacityMap.entrySet()) {
                ResourceItem resource = (ResourceItem)entry.getKey();
                int resourceFreeItemCapacity = (Integer)entry.getValue();
                Item resourceFirstItem = (Item)resourceItemMap.get(resource);
                if (resourceFreeItemCapacity <= 0 || !this.canStackItem(resource, resourceFirstItem, inventoryItem.getScriptItem())) continue;
                ++allocatedItems;
                resourceCapacityMap.put(resource, resourceFreeItemCapacity - 1);
                continue block1;
            }
        }
        return allocatedItems == inventoryItems.size();
    }

    private boolean canStackItem(ResourceItem resource, Item firstItem, Item newItem) {
        if (newItem == null) {
            return false;
        }
        if (firstItem == null) {
            return resource.getItemFilter().allows(newItem);
        }
        boolean canStack = resource.isStackAnyItem() || CraftUtil.canItemsStack(firstItem, newItem, true);
        return canStack && resource.getItemFilter().allows(newItem);
    }

    private boolean willOutputsAccommodate(ItemDataList pendingItems) {
        return this.getFreeOutputSlotCount() >= pendingItems.size();
    }

    public int getFreeOutputSlotCount() {
        int pendingOutputCount = 0;
        for (CraftRecipeData craftRecipeData : this.getAllInProgressCraftData()) {
            pendingOutputCount += craftRecipeData.getToOutputItems().size();
        }
        int resourceFreeItemSlotCount = 0;
        for (Resource resource : this.getOutputResources()) {
            resourceFreeItemSlotCount += resource.getFreeItemCapacity();
        }
        return resourceFreeItemSlotCount - pendingOutputCount;
    }

    public void start(IsoPlayer player) {
        if (this.startMode == StartMode.Manual && !this.owner.isUsingPlayer(player)) {
            return;
        }
        if (GameClient.client) {
            if (this.canStart(StartMode.Manual, player)) {
                this.sendStartRequest(player);
            }
        } else {
            this.startRequested = true;
            this.requestingPlayer = player;
        }
    }

    public void stop(IsoPlayer player) {
        this.stop(player, false);
    }

    public void stop(IsoPlayer player, boolean force) {
        if (!this.isValid()) {
            return;
        }
        if (this.startMode == StartMode.Manual && !force && !this.owner.isUsingPlayer(player)) {
            return;
        }
        if (GameClient.client) {
            this.sendStopRequest(player);
        } else {
            this.stopRequested = true;
            this.requestingPlayer = player;
        }
    }

    protected void forceStopInternal() {
        if (!GameClient.client) {
            this.stopRequested = true;
            this.requestingPlayer = null;
        }
    }

    public void onStart() {
        this.craftData.setTargetVariableInputRatio(this.craftData.getVariableInputRatio());
        this.craftData.createRecipeOutputs(true, null, null);
        this.craftDataInProgress.add(this.craftData);
        this.craftData = CraftRecipeData.Alloc(CraftMode.Automation, true, false, true, false);
        this.updateSpriteOverlay(0);
    }

    public void onUpdate(CraftRecipeData craftRecipeData) {
        this.updateSpriteOverlay((int)(this.getProgress(this.getFirstInProgressCraftData()) * 100.0));
        if (GameServer.server && this.limit.Check()) {
            this.sendCraftLogicSync();
        }
    }

    public void onStop(CraftRecipeData craftRecipeData, boolean isCancelled) {
        if (isCancelled) {
            this.clearSpriteOverlay();
        } else {
            this.updateSpriteOverlay(100);
        }
    }

    public void finaliseRecipe(CraftRecipeData craftRecipeData) {
        this.craftDataInProgress.remove(craftRecipeData);
        CraftRecipeData.Release(craftRecipeData);
    }

    @Override
    protected void onComponentEvent(ComponentEvent event) {
        Component component = event.getSender();
        if (component instanceof Resources) {
            Resources resources = (Resources)component;
            switch (event.getEventType()) {
                case OnContentsChanged: {
                    ArrayList<Resource> outputs = new ArrayList<Resource>();
                    resources.getResources(outputs, ResourceIO.Output, ResourceType.Item);
                    if (this.getFirstInProgressCraftData() != null || !outputs.stream().allMatch(Resource::isEmpty)) break;
                    this.clearSpriteOverlay();
                }
            }
        }
    }

    private void clearSpriteOverlay() {
        SpriteOverlayConfig overlayConfig = (SpriteOverlayConfig)this.getGameEntity().getComponent(ComponentType.SpriteOverlayConfig);
        if (overlayConfig != null) {
            overlayConfig.clearStyle();
        }
    }

    private void updateSpriteOverlay(int percentageComplete) {
        SpriteOverlayConfig overlayConfig = (SpriteOverlayConfig)this.getGameEntity().getComponent(ComponentType.SpriteOverlayConfig);
        if (overlayConfig != null) {
            ArrayList<String> availableStyles = overlayConfig.getAvailableStyles();
            String bestStyle = this.getBestStyle(percentageComplete, availableStyles);
            overlayConfig.setStyle(bestStyle);
        }
    }

    private String getBestStyleName(List<String> availableStyles) {
        CraftRecipeData oldestRecipeData;
        if (this.isCraftingMixedRecipes()) {
            for (String style : availableStyles) {
                if (!style.startsWith(SpriteOverlayConfigKey.DEFAULT.toString())) continue;
                return SpriteOverlayConfigKey.DEFAULT.toString();
            }
        }
        if ((oldestRecipeData = this.getFirstInProgressCraftData()) != null && oldestRecipeData.getRecipe() != null) {
            return oldestRecipeData.getRecipe().getOverlayMapper().getStyle(availableStyles, oldestRecipeData);
        }
        return null;
    }

    private String getBestStyle(int percentageComplete, List<String> availableStyles) {
        String bestStyle = null;
        String styleName = this.getBestStyleName(availableStyles);
        if (styleName != null) {
            ArrayList<String> viableStyles = new ArrayList<String>();
            for (String style : availableStyles) {
                if (!style.startsWith(styleName)) continue;
                viableStyles.add(style);
            }
            int bestDiff = 101;
            for (String style : viableStyles) {
                int diff;
                String[] parts = style.split("_");
                int progressComponent = 0;
                if (parts.length > 1) {
                    progressComponent = Integer.parseInt(parts[1]);
                }
                if ((diff = percentageComplete - progressComponent) >= bestDiff || diff < 0) continue;
                bestStyle = style;
                bestDiff = diff;
            }
        }
        return bestStyle;
    }

    @Override
    public void dumpContentsInSquare() {
        if (GameClient.client) {
            return;
        }
        if (this.isRunning()) {
            ArrayList<InventoryItem> consumedItems = new ArrayList<InventoryItem>();
            for (CraftRecipeData craftData : this.craftDataInProgress) {
                craftData.getAllConsumedItems(consumedItems, true);
            }
            for (InventoryItem item : consumedItems) {
                this.owner.getSquare().AddWorldInventoryItem(item, 0.0f, 0.0f, 0.0f);
            }
            this.forceStopInternal();
        }
    }

    public void returnConsumedItemsToResourcesOrSquare(CraftRecipeData craftRecipeData) {
        if (GameClient.client) {
            return;
        }
        if (this.isRunning()) {
            ArrayList<InventoryItem> consumedItems = new ArrayList<InventoryItem>();
            craftRecipeData.getAllConsumedItems(consumedItems, true);
            ArrayList<Resource> resources = new ArrayList<Resource>();
            resources.addAll(this.getInputResources());
            resources.addAll(this.getOutputResources());
            for (InventoryItem item : consumedItems) {
                for (Resource resource : resources) {
                    if (resource.offerItem(item) != null) continue;
                    item = null;
                    break;
                }
                if (item == null) continue;
                this.owner.getSquare().AddWorldInventoryItem(item, 0.0f, 0.0f, 0.0f);
            }
        }
    }

    @Override
    public boolean isNoContainerOrEmpty() {
        return !this.isRunning();
    }

    @Override
    protected boolean onReceivePacket(ByteBufferReader input, EntityPacketType type, IConnection senderConnection) throws IOException {
        switch (type) {
            case CraftLogicStartRequest: {
                this.receiveStartRequest(input, senderConnection);
                return true;
            }
            case CraftLogicStopRequest: {
                this.receiveStopRequest(input, senderConnection);
                return true;
            }
            case CraftLogicSync: {
                this.receiveCraftLogicSync(input, senderConnection);
                return true;
            }
        }
        return false;
    }

    public void sendStartRequest(IsoPlayer player) {
        if (!GameClient.client) {
            return;
        }
        EntityPacketData data = GameEntityNetwork.createPacketData(EntityPacketType.CraftLogicStartRequest);
        data.bb.put(player != null ? (byte)1 : 0);
        if (player != null) {
            PlayerID playerID = new PlayerID();
            playerID.set(player);
            playerID.write(data.bb);
        }
        if (this.craftTestData == null || this.craftTestData.getRecipe() == null) {
            GameWindow.WriteString(data.bb, "");
        } else {
            GameWindow.WriteString(data.bb, this.craftTestData.getRecipe().getName());
        }
        this.sendClientPacket(data);
    }

    protected void receiveStartRequest(ByteBufferReader input, IConnection senderConnection) throws IOException {
        String recipeName;
        if (!GameServer.server) {
            return;
        }
        IsoPlayer player = null;
        if (input.getBoolean()) {
            PlayerID playerID = new PlayerID();
            playerID.parse(input, senderConnection);
            player = playerID.getPlayer();
            if (player == null) {
                throw new IOException("Player not found.");
            }
        }
        if (!(recipeName = input.getUTF()).isEmpty()) {
            CraftRecipe recipe = this.getRecipes().stream().filter(craftRecipe -> craftRecipe.getName().equals(recipeName)).findFirst().orElse(null);
            this.setRecipe(recipe);
        }
        this.start(player);
    }

    public void sendStopRequest(IsoPlayer player) {
        if (!GameClient.client) {
            return;
        }
        EntityPacketData data = GameEntityNetwork.createPacketData(EntityPacketType.CraftLogicStopRequest);
        data.bb.put(player != null ? (byte)1 : 0);
        if (player != null) {
            PlayerID playerID = new PlayerID();
            playerID.set(player);
            playerID.write(data.bb);
        }
        this.sendClientPacket(data);
    }

    protected void receiveStopRequest(ByteBufferReader input, IConnection senderConnection) throws IOException {
        if (!GameServer.server) {
            return;
        }
        IsoPlayer player = null;
        if (input.getBoolean()) {
            PlayerID playerID = new PlayerID();
            playerID.parse(input, senderConnection);
            player = playerID.getPlayer();
            if (player == null) {
                throw new IOException("Player not found.");
            }
        }
        this.stop(player);
    }

    public void sendCraftLogicSync() {
        if (!GameServer.server) {
            return;
        }
        EntityPacketData data = GameEntityNetwork.createPacketData(EntityPacketType.CraftLogicSync);
        try {
            this.save(data.bb);
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }
        if (this.craftTestData == null || this.craftTestData.getRecipe() == null) {
            GameWindow.WriteString(data.bb, "");
        } else {
            GameWindow.WriteString(data.bb, this.craftTestData.getRecipe().getName());
        }
        this.sendServerPacket(data, null);
    }

    protected void receiveCraftLogicSync(ByteBufferReader input, IConnection senderConnection) throws IOException {
        if (!GameClient.client) {
            return;
        }
        this.load(input.bb, 244);
        String recipeName = input.getUTF();
        if (!(recipeName.isEmpty() || this.craftData.getRecipe() != null && recipeName.equals(this.craftData.getRecipe().getName()))) {
            CraftRecipe recipe = this.getRecipes().stream().filter(craftRecipe -> craftRecipe.getName().equals(recipeName)).findFirst().orElse(null);
            this.setRecipe(recipe);
        }
    }

    public void doProgressTooltip(ObjectTooltip.Layout layout, Resource resource, CraftRecipeData craftRecipeData) {
        if (!this.isRunning()) {
            ObjectTooltip.LayoutItem item = layout.addItem();
            item.setLabel(Translator.getText("EC_CraftLogicTooltip_NoCraftInProgress"), 1.0f, 1.0f, 1.0f, 1.0f);
            return;
        }
        if (craftRecipeData != null) {
            ObjectTooltip.LayoutItem item = layout.addItem();
            item.setLabel(Translator.getText("EC_CraftLogicTooltip_Progress") + ":", 1.0f, 1.0f, 1.0f, 1.0f);
            int progress = (int)(this.getProgress(craftRecipeData) * 100.0);
            item.setValue(String.format(Locale.ENGLISH, "%d%%", progress), 1.0f, 1.0f, 0.8f, 1.0f);
            item = layout.addItem();
            item.setLabel(Translator.getText("EC_CraftLogicTooltip_TimeRemaining") + ":", 1.0f, 1.0f, 1.0f, 1.0f);
            int timeRemaining = craftRecipeData.getRecipe().getTime() - (int)craftRecipeData.getElapsedTime();
            int ss = timeRemaining % 60;
            int mm = timeRemaining / 60 % 60;
            int hh = timeRemaining / 3600 % 24;
            int dd = Math.floorDiv(timeRemaining, 86400);
            item.setValue(String.format(Locale.ENGLISH, "%02dd %02dh %02dm %02ds", dd, hh, mm, ss), 1.0f, 1.0f, 0.8f, 1.0f);
        }
    }

    public ArrayList<Texture> getStatusIconsForInputItem(InventoryItem item, CraftRecipeData craftRecipeData) {
        return null;
    }

    @Override
    protected void saveSyncData(ByteBuffer output) throws IOException {
        this.save(output);
    }

    @Override
    protected void loadSyncData(ByteBuffer input) throws IOException {
        this.load(input, 244);
    }

    @Override
    protected void save(ByteBuffer output) throws IOException {
        super.save(output);
        BitHeaderWrite header = BitHeader.allocWrite(BitHeader.HeaderSize.Byte, output);
        if (this.recipeTagQuery != null) {
            header.addFlags(1);
            GameWindow.WriteString(output, this.recipeTagQuery);
        }
        if (this.startMode != StartMode.Manual) {
            header.addFlags(2);
            output.put(this.startMode.getByteId());
        }
        if (this.inputsGroupName != null) {
            header.addFlags(4);
            GameWindow.WriteString(output, this.inputsGroupName);
        }
        if (this.outputsGroupName != null) {
            header.addFlags(8);
            GameWindow.WriteString(output, this.outputsGroupName);
        }
        if (this.isRunning()) {
            header.addFlags(16);
            this.saveInProgessCraftData(output);
        }
        if (this.actionAnimOverride != null) {
            header.addFlags(32);
            GameWindow.WriteString(output, this.actionAnimOverride);
        }
        header.write();
        header.release();
    }

    protected void saveInProgessCraftData(ByteBuffer output) throws IOException {
        output.putInt(this.getAllInProgressCraftData().size());
        for (int i = 0; i < this.getAllInProgressCraftData().size(); ++i) {
            CraftRecipeData craftRecipeData = this.getAllInProgressCraftData().get(i);
            craftRecipeData.save(output);
        }
    }

    @Override
    protected void load(ByteBuffer input, int worldVersion) throws IOException {
        super.load(input, worldVersion);
        BitHeaderRead header = BitHeader.allocRead(BitHeader.HeaderSize.Byte, input);
        this.recipeTagQuery = null;
        this.startMode = StartMode.Manual;
        this.inputsGroupName = null;
        this.outputsGroupName = null;
        if (header.hasFlags(1)) {
            String loadedRecipeTagQuery = GameWindow.ReadString(input);
            this.setRecipeTagQuery(loadedRecipeTagQuery);
        }
        if (header.hasFlags(2)) {
            this.startMode = StartMode.fromByteId(input.get());
        }
        if (header.hasFlags(4)) {
            this.inputsGroupName = GameWindow.ReadString(input);
        }
        if (header.hasFlags(8)) {
            this.outputsGroupName = GameWindow.ReadString(input);
        }
        if (header.hasFlags(16)) {
            this.loadInProgressCraftData(input, worldVersion);
        } else {
            this.craftDataInProgress.clear();
        }
        if (header.hasFlags(32)) {
            this.actionAnimOverride = GameWindow.ReadString(input);
        }
        header.release();
        this.doAutomaticCraftCheck = true;
    }

    protected void loadInProgressCraftData(ByteBuffer input, int worldVersion) throws IOException {
        int i;
        boolean recipeInvalidated = false;
        int numberOfInProgressCrafts = 1;
        if (worldVersion >= 238) {
            numberOfInProgressCrafts = input.getInt();
        }
        for (i = 0; i < this.craftDataInProgress.size() - numberOfInProgressCrafts; ++i) {
            this.craftDataInProgress.removeFirst();
        }
        for (i = 0; i < numberOfInProgressCrafts; ++i) {
            CraftRecipeData craftRecipeData = null;
            boolean needToAdd = false;
            if (i < this.craftDataInProgress.size()) {
                craftRecipeData = this.craftDataInProgress.get(i);
            } else {
                craftRecipeData = CraftRecipeData.Alloc(CraftMode.Automation, true, false, true, false);
                needToAdd = true;
            }
            if (!craftRecipeData.load(input, worldVersion, null, recipeInvalidated)) {
                recipeInvalidated = true;
            }
            if (recipeInvalidated) {
                CraftRecipeData.Release(craftRecipeData);
                continue;
            }
            craftRecipeData.createRecipeOutputs(true, null, null);
            if (!needToAdd) continue;
            this.craftDataInProgress.add(craftRecipeData);
        }
    }
}

