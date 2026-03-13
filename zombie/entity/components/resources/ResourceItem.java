/*
 * Decompiled with CFR 0.152.
 */
package zombie.entity.components.resources;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import zombie.GameWindow;
import zombie.UsedFromLua;
import zombie.core.Core;
import zombie.core.Translator;
import zombie.core.math.PZMath;
import zombie.debug.DebugLog;
import zombie.debug.DebugOptions;
import zombie.debug.objects.DebugClassFields;
import zombie.entity.components.crafting.CraftUtil;
import zombie.entity.components.fluids.FluidContainer;
import zombie.entity.components.resources.Resource;
import zombie.entity.components.resources.ResourceBlueprint;
import zombie.entity.components.resources.ResourceType;
import zombie.inventory.CompressIdenticalItems;
import zombie.inventory.InventoryItem;
import zombie.inventory.InventoryItemFactory;
import zombie.inventory.ItemContainer;
import zombie.inventory.ItemFilter;
import zombie.inventory.types.DrainableComboItem;
import zombie.network.GameClient;
import zombie.network.GameServer;
import zombie.scripting.entity.components.crafting.InputScript;
import zombie.scripting.objects.Item;
import zombie.ui.ObjectTooltip;

@DebugClassFields
@UsedFromLua
public class ResourceItem
extends Resource {
    private final ItemFilter itemFilter = new ItemFilter();
    private final ArrayList<InventoryItem> storedItems = new ArrayList();
    private float capacity;
    private boolean stackAnyItem;

    protected ResourceItem() {
    }

    @Override
    void loadBlueprint(ResourceBlueprint bp) {
        super.loadBlueprint(bp);
        this.capacity = bp.getCapacity();
        this.stackAnyItem = bp.isStackAnyItem();
        if (this.getFilterName() != null) {
            this.itemFilter.setFilterScript(this.getFilterName());
        }
    }

    public ItemFilter getItemFilter() {
        return this.itemFilter;
    }

    public int storedSize() {
        return this.storedItems.size();
    }

    public boolean isStackAnyItem() {
        return this.stackAnyItem;
    }

    @Override
    public void DoTooltip(ObjectTooltip tooltipUI) {
        ObjectTooltip.Layout layout = tooltipUI.beginLayout();
        layout.setMinLabelWidth(80);
        int y = tooltipUI.padTop;
        this.DoTooltip(tooltipUI, layout);
        y = layout.render(tooltipUI.padLeft, y, tooltipUI);
        tooltipUI.endLayout(layout);
        layout = tooltipUI.beginLayout();
        layout.setMinLabelWidth(80);
        if (!this.isEmpty()) {
            HashSet<Item> renderedItems = new HashSet<Item>();
            int lineSpacing = tooltipUI.getLineSpacing();
            int tooltipY = y;
            for (int i = 0; i < this.storedSize(); ++i) {
                InventoryItem inventoryItem = this.peekItem(i);
                if (inventoryItem == null || renderedItems.contains(inventoryItem.getScriptItem())) continue;
                inventoryItem.DoTooltipEmbedded(tooltipUI, layout, tooltipY);
                tooltipY += lineSpacing;
                renderedItems.add(inventoryItem.getScriptItem());
            }
        }
        if (Core.debug && DebugOptions.instance.entityDebugUi.getValue()) {
            this.DoDebugTooltip(tooltipUI, layout);
        }
        y = layout.render(tooltipUI.padLeft, PZMath.max(y, layout.offsetY), tooltipUI);
        tooltipUI.endLayout(layout);
        tooltipUI.setHeight(y += tooltipUI.padBottom);
        if (tooltipUI.getWidth() < 150.0) {
            tooltipUI.setWidth(150.0);
        }
    }

    @Override
    public void DoTooltip(ObjectTooltip tooltipUI, ObjectTooltip.Layout layout) {
        if (layout == null) {
            return;
        }
        super.DoTooltip(tooltipUI, layout);
        ObjectTooltip.LayoutItem item = layout.addItem();
        item.setLabel(Translator.getEntityText("EC_Stored") + ":", 1.0f, 1.0f, 0.8f, 1.0f);
        item.setValue(this.getItemAmount() + "/" + this.getItemCapacity(), 1.0f, 1.0f, 1.0f, 1.0f);
    }

    @Override
    protected void DoDebugTooltip(ObjectTooltip tooltipUI, ObjectTooltip.Layout layout) {
        if (layout == null || !Core.debug) {
            return;
        }
        super.DoDebugTooltip(tooltipUI, layout);
        float a = 0.7f;
        ObjectTooltip.LayoutItem item = layout.addItem();
        item.setLabel("UsesAmount:", 0.7f, 0.7f, 0.56f, 1.0f);
        item.setValue("" + this.getItemUsesAmount(), 0.7f, 0.7f, 0.7f, 1.0f);
        item = layout.addItem();
        item.setLabel("FluidAmount:", 0.7f, 0.7f, 0.56f, 1.0f);
        item.setValue("" + this.getFluidAmount(), 0.7f, 0.7f, 0.7f, 1.0f);
        item = layout.addItem();
        item.setLabel("EnergyAmount:", 0.7f, 0.7f, 0.56f, 1.0f);
        item.setValue("" + this.getEnergyAmount(), 0.7f, 0.7f, 0.7f, 1.0f);
    }

    @Override
    public boolean isFull() {
        return (float)this.storedItems.size() >= this.capacity;
    }

    @Override
    public boolean isEmpty() {
        return this.storedItems.isEmpty();
    }

    @Override
    public int getItemAmount() {
        return this.storedItems.size();
    }

    public int getItemAmount(Item itemType) {
        int count = 0;
        for (InventoryItem invItem : this.storedItems) {
            if (invItem == null || invItem.getScriptItem() != itemType) continue;
            ++count;
        }
        return count;
    }

    @Override
    public float getItemUses(InputScript inputScript) {
        float itemUses = 0.0f;
        boolean isItemCount = inputScript != null && inputScript.isItemCount();
        ResourceType resourceType = inputScript != null ? inputScript.getResourceType() : ResourceType.Item;
        for (InventoryItem item : this.storedItems) {
            switch (resourceType) {
                case Item: {
                    itemUses += isItemCount ? 1.0f : (float)item.getCurrentUses();
                    break;
                }
                case Fluid: {
                    FluidContainer fc = item.getFluidContainer();
                    if (fc == null) break;
                    itemUses += fc.getAmount();
                }
            }
        }
        return itemUses;
    }

    @Override
    public float getFluidAmount() {
        InventoryItem itemObj = this.peekItem();
        if (itemObj == null) {
            return 0.0f;
        }
        if (itemObj.getFluidContainer() != null) {
            return itemObj.getFluidContainer().getAmount();
        }
        return 0.0f;
    }

    @Override
    public float getEnergyAmount() {
        DrainableComboItem comboItem;
        InventoryItem itemObj = this.peekItem();
        if (itemObj == null) {
            return 0.0f;
        }
        if (itemObj instanceof DrainableComboItem && (comboItem = (DrainableComboItem)itemObj).isEnergy()) {
            return comboItem.getCurrentUsesFloat();
        }
        return 0.0f;
    }

    @Override
    public float getItemUsesAmount() {
        InventoryItem itemObj = this.peekItem();
        if (itemObj == null) {
            return 0.0f;
        }
        if (itemObj instanceof DrainableComboItem) {
            return itemObj.getCurrentUsesFloat();
        }
        return 0.0f;
    }

    @Override
    public int getItemCapacity() {
        return (int)this.capacity;
    }

    @Override
    public float getFluidCapacity() {
        InventoryItem itemObj = this.peekItem();
        if (itemObj == null) {
            return 0.0f;
        }
        if (itemObj.getFluidContainer() != null) {
            return itemObj.getFluidContainer().getCapacity();
        }
        return 0.0f;
    }

    @Override
    public float getEnergyCapacity() {
        return this.getItemUsesCapacity();
    }

    @Override
    public float getItemUsesCapacity() {
        return 1.0f;
    }

    @Override
    public int getFreeItemCapacity() {
        return (int)this.capacity - this.storedItems.size();
    }

    @Override
    public float getFreeFluidCapacity() {
        InventoryItem itemObj = this.peekItem();
        if (itemObj == null) {
            return 0.0f;
        }
        if (itemObj.getFluidContainer() != null) {
            return itemObj.getFluidContainer().getFreeCapacity();
        }
        return 0.0f;
    }

    @Override
    public float getFreeEnergyCapacity() {
        DrainableComboItem comboItem;
        InventoryItem itemObj = this.peekItem();
        if (itemObj == null) {
            return 0.0f;
        }
        if (itemObj instanceof DrainableComboItem && (comboItem = (DrainableComboItem)itemObj).isEnergy()) {
            return this.getEnergyCapacity() - comboItem.getCurrentUsesFloat();
        }
        return 0.0f;
    }

    @Override
    public float getFreeItemUsesCapacity() {
        InventoryItem itemObj = this.peekItem();
        if (itemObj == null) {
            return 0.0f;
        }
        if (itemObj instanceof DrainableComboItem) {
            return this.getItemUsesCapacity() - itemObj.getCurrentUsesFloat();
        }
        return 0.0f;
    }

    @Override
    public boolean containsItem(InventoryItem item) {
        return this.storedItems.contains(item);
    }

    @Override
    public boolean acceptsItem(InventoryItem item, boolean ignoreFilters) {
        if (item == null) {
            return false;
        }
        if (!this.isLocked() && !this.isFull()) {
            boolean canStack;
            boolean bl = canStack = CraftUtil.canItemsStack(this.peekItem(), item, true) || this.stackAnyItem;
            if (canStack && !this.containsItem(item)) {
                return ignoreFilters || this.itemFilter.allows(item);
            }
        }
        return false;
    }

    @Override
    public boolean canStackItem(InventoryItem item) {
        boolean canStack;
        if (item == null) {
            return false;
        }
        if (this.isEmpty()) {
            return this.itemFilter.allows(item);
        }
        boolean bl = canStack = CraftUtil.canItemsStack(this.peekItem(), item, true) || this.stackAnyItem;
        if (!this.isFull() && canStack) {
            return this.itemFilter.allows(item);
        }
        return false;
    }

    @Override
    public boolean canStackItem(Item item) {
        boolean canStack;
        if (item == null) {
            return false;
        }
        if (this.isEmpty()) {
            return this.itemFilter.allows(item);
        }
        Item peekItem = this.peekItem().getScriptItem();
        boolean bl = canStack = CraftUtil.canItemsStack(peekItem, item, true) || this.stackAnyItem;
        if (!this.isFull() && canStack) {
            return this.itemFilter.allows(item);
        }
        return false;
    }

    @Override
    public InventoryItem offerItem(InventoryItem item, boolean ignoreFilters) {
        return this.offerItem(item, ignoreFilters, false, true);
    }

    @Override
    public InventoryItem offerItem(InventoryItem item, boolean ignoreFilters, boolean force, boolean syncEntity) {
        if (GameClient.client && syncEntity) {
            DebugLog.General.warn("Not allowed on client");
            return item;
        }
        if (force && (item == null || this.containsItem(item) || this.isFull())) {
            return item;
        }
        if (force || this.acceptsItem(item, ignoreFilters)) {
            ItemContainer container = item.getContainer();
            if (container != null && !this.isTemporary()) {
                if (InventoryItem.RemoveFromContainer(item)) {
                    if (GameServer.server) {
                        GameServer.sendRemoveItemFromContainer(container, item);
                    }
                } else {
                    if (Core.debug) {
                        throw new RuntimeException("WARNING OfferItem -> item not removed from container.");
                    }
                    return item;
                }
            }
            this.storedItems.add(item);
            this.setDirty();
            if (syncEntity && GameServer.server) {
                this.sync();
            }
            return null;
        }
        return item;
    }

    public ArrayList<InventoryItem> offerItems(List<InventoryItem> items) {
        return this.offerItems(items, false);
    }

    public ArrayList<InventoryItem> offerItems(List<InventoryItem> items, boolean ignoreFilters) {
        ArrayList<InventoryItem> added = new ArrayList<InventoryItem>();
        if (GameClient.client) {
            DebugLog.General.warn("Not allowed on client");
            return added;
        }
        for (InventoryItem item : items) {
            if (this.isFull()) break;
            if (this.offerItem(item, ignoreFilters) != null) continue;
            added.add(item);
        }
        return added;
    }

    public ArrayList<InventoryItem> removeAllItems(ArrayList<InventoryItem> list) {
        return this.removeAllItems(list, null);
    }

    public ArrayList<InventoryItem> removeAllItems(ArrayList<InventoryItem> list, Item itemType) {
        if (list == null) {
            list = new ArrayList();
        }
        if (GameClient.client) {
            DebugLog.General.warn("Not allowed on client");
            return list;
        }
        if (!this.isLocked() && !this.storedItems.isEmpty()) {
            if (itemType != null) {
                for (int i = this.storedItems.size() - 1; i >= 0; --i) {
                    InventoryItem inventoryItem = this.storedItems.get(i);
                    if (inventoryItem == null || inventoryItem.getScriptItem() != itemType) continue;
                    list.add(inventoryItem);
                    this.storedItems.remove(i);
                }
            } else {
                list.addAll(this.storedItems);
                this.storedItems.clear();
            }
        }
        if (!list.isEmpty()) {
            this.setDirty();
        }
        return list;
    }

    @Override
    public InventoryItem pollItem() {
        return this.pollItem(false, true);
    }

    @Override
    public InventoryItem pollItem(boolean force, boolean syncEntity) {
        if (GameClient.client) {
            DebugLog.General.warn("Not allowed on client");
            return null;
        }
        if (!(!force && this.isLocked() || this.storedItems.isEmpty())) {
            InventoryItem item = this.storedItems.remove(this.storedItems.size() - 1);
            this.setDirty();
            if (syncEntity && GameServer.server) {
                this.sync();
            }
            return item;
        }
        return null;
    }

    @Override
    public InventoryItem peekItem() {
        return this.peekItem(0);
    }

    public InventoryItem removeItem(InventoryItem item) {
        if (GameClient.client) {
            DebugLog.General.warn("Not allowed on client");
            return null;
        }
        if (!this.containsItem(item)) {
            return null;
        }
        this.storedItems.remove(item);
        this.setDirty();
        if (GameServer.server) {
            this.sync();
        }
        return item;
    }

    @Override
    public InventoryItem peekItem(int offset) {
        if (!this.storedItems.isEmpty()) {
            int index = this.storedItems.size() - 1;
            if (offset > 0 && (index -= offset) < 0) {
                return null;
            }
            return this.storedItems.get(index);
        }
        return null;
    }

    public ArrayList<InventoryItem> getStoredItems() {
        return this.storedItems;
    }

    public ArrayList<InventoryItem> getStoredItemsOfType(Item itemType) {
        ArrayList<InventoryItem> output = new ArrayList<InventoryItem>();
        for (InventoryItem inventoryItem : this.storedItems) {
            if (inventoryItem == null || output.contains(inventoryItem) || inventoryItem.getScriptItem() != itemType) continue;
            output.add(inventoryItem);
        }
        return output;
    }

    public ArrayList<Item> getUniqueItems() {
        ArrayList<Item> output = new ArrayList<Item>();
        for (InventoryItem inventoryItem : this.storedItems) {
            if (inventoryItem == null || output.contains(inventoryItem.getScriptItem())) continue;
            output.add(inventoryItem.getScriptItem());
        }
        return output;
    }

    @Override
    public void tryTransferTo(Resource target) {
        if (this.isEmpty() || target == null || target.isFull()) {
            return;
        }
        if (target instanceof ResourceItem) {
            ResourceItem resourceItem = (ResourceItem)target;
            this.transferTo(resourceItem, this.getItemAmount());
        }
    }

    @Override
    public void tryTransferTo(Resource target, float amount) {
        if (this.isEmpty() || target == null || target.isFull()) {
            return;
        }
        if (target instanceof ResourceItem) {
            ResourceItem resourceItem = (ResourceItem)target;
            this.transferTo(resourceItem, (int)amount);
        }
    }

    public void transferTo(ResourceItem target, int transferAmount) {
        if (this.isEmpty() || target == null || target.isFull()) {
            return;
        }
        int amount = PZMath.min(PZMath.min(transferAmount, this.getItemAmount()), target.getFreeItemCapacity());
        if (amount <= 0) {
            return;
        }
        for (int i = 0; i < amount; ++i) {
            InventoryItem item = this.pollItem();
            if (target.offerItem(item) == null) continue;
            this.storedItems.add(item);
            break;
        }
        this.setDirty();
    }

    @Override
    public void clear() {
        if (GameClient.client) {
            DebugLog.General.warn("Not allowed on client");
            return;
        }
        this.storedItems.clear();
        this.setDirty();
    }

    @Override
    protected void reset() {
        super.reset();
        this.storedItems.clear();
        this.capacity = 0.0f;
        this.itemFilter.setFilterScript(null);
        this.stackAnyItem = false;
    }

    @Override
    public void saveSync(ByteBuffer output) throws IOException {
        super.saveSync(output);
        output.putFloat(this.capacity);
        output.put(this.stackAnyItem ? (byte)1 : 0);
        output.putInt(this.storedItems.size());
        if (!this.storedItems.isEmpty()) {
            String type = this.storedItems.get(0).getFullType();
            GameWindow.WriteString(output, type);
            for (int i = 0; i < this.storedItems.size(); ++i) {
                InventoryItem item = this.storedItems.get(i);
                if (!item.getFullType().equals(type)) {
                    throw new IOException("Type mismatch '" + item.getFullType() + "' vs '" + type + "'");
                }
                item.saveWithSize(output, true);
            }
        }
    }

    @Override
    public void loadSync(ByteBuffer input, int worldVersion) throws IOException {
        boolean loaded;
        int posReset;
        String expectedType;
        int size;
        block8: {
            super.loadSync(input, worldVersion);
            this.capacity = input.getFloat();
            if (worldVersion >= 238) {
                boolean bl = this.stackAnyItem = input.get() != 0;
            }
            if ((size = input.getInt()) == 0) {
                this.storedItems.clear();
                return;
            }
            expectedType = GameWindow.ReadString(input);
            posReset = input.position();
            loaded = false;
            if (this.storedItems.size() == size) {
                try {
                    loaded = this.tryLoadSyncItems(input, worldVersion, size, expectedType, false);
                }
                catch (Exception e) {
                    if (!Core.debug) break block8;
                    DebugLog.General.warn("Unable to load items (may be ignored)");
                }
            }
        }
        if (!loaded) {
            try {
                input.position(posReset);
                this.tryLoadSyncItems(input, worldVersion, size, expectedType, true);
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public boolean tryLoadSyncItems(ByteBuffer input, int worldVersion, int size, String type, boolean forceCreate) throws IOException {
        if (forceCreate) {
            this.storedItems.clear();
        }
        for (int i = 0; i < size; ++i) {
            Object item;
            if (forceCreate) {
                item = InventoryItemFactory.CreateItem(type);
            } else {
                item = this.storedItems.get(i);
                if (!((InventoryItem)item).getFullType().equals(type)) {
                    throw new IOException("Type mismatch '" + ((InventoryItem)item).getFullType() + "' vs '" + type + "'");
                }
            }
            InventoryItem.loadItem(input, worldVersion, true, (InventoryItem)item);
        }
        return true;
    }

    @Override
    public void save(ByteBuffer output) throws IOException {
        super.save(output);
        output.putFloat(this.capacity);
        output.put(this.stackAnyItem ? (byte)1 : 0);
        if (this.storedItems.size() == 1) {
            CompressIdenticalItems.save(output, this.storedItems.get(0));
        } else {
            CompressIdenticalItems.save(output, this.storedItems, null);
        }
    }

    @Override
    public void load(ByteBuffer input, int worldVersion) throws IOException {
        super.load(input, worldVersion);
        this.capacity = input.getFloat();
        if (worldVersion >= 238) {
            this.stackAnyItem = input.get() != 0;
        }
        this.storedItems.clear();
        CompressIdenticalItems.load(input, worldVersion, this.storedItems, null);
        if (this.getFilterName() != null) {
            this.itemFilter.setFilterScript(this.getFilterName());
        } else {
            this.itemFilter.setFilterScript(null);
        }
    }
}

