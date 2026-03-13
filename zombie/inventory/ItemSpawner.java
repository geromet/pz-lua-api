/*
 * Decompiled with CFR 0.152.
 */
package zombie.inventory;

import java.util.ArrayList;
import java.util.List;
import zombie.Lua.LuaEventManager;
import zombie.UsedFromLua;
import zombie.inventory.InventoryItem;
import zombie.inventory.InventoryItemFactory;
import zombie.inventory.ItemContainer;
import zombie.inventory.ItemPickerJava;
import zombie.inventory.types.InventoryContainer;
import zombie.iso.InstanceTracker;
import zombie.iso.IsoGridSquare;

@UsedFromLua
public abstract class ItemSpawner {
    private static void inc(InventoryItem item, int count) {
        if (item == null) {
            return;
        }
        InstanceTracker.adj("Item Spawns", item.getFullType(), count);
    }

    public static List<InventoryItem> spawnItems(InventoryItem item, int count, ItemContainer container) {
        ArrayList<InventoryItem> items = container.AddItems(item, count);
        ItemSpawner.inc((InventoryItem)items.get(0), items.size());
        return items;
    }

    public static List<InventoryItem> spawnItems(String itemType, int count, ItemContainer container) {
        ArrayList<InventoryItem> items = container.AddItems(itemType, count);
        ItemSpawner.inc((InventoryItem)items.get(0), items.size());
        return items;
    }

    public static InventoryItem spawnItem(InventoryItem item, IsoGridSquare square, float x, float y, float z, boolean fill) {
        if (item == null) {
            return null;
        }
        square.AddWorldInventoryItem(item, x, y, z);
        if (item.getWorldItem() != null) {
            item.getWorldItem().setIgnoreRemoveSandbox(true);
        }
        ItemSpawner.inc(item, 1);
        if (fill && item instanceof InventoryContainer) {
            InventoryContainer inventoryContainer = (InventoryContainer)item;
            if (ItemPickerJava.containers.containsKey(item.getType())) {
                ItemPickerJava.rollContainerItem(inventoryContainer, null, ItemPickerJava.getItemPickerContainers().get(item.getType()));
                LuaEventManager.triggerEvent("OnFillContainer", "Container", item.getType(), inventoryContainer.getItemContainer());
            }
        }
        item.setAutoAge();
        return item;
    }

    public static InventoryItem spawnItem(InventoryItem item, IsoGridSquare square, float x, float y, float z) {
        return ItemSpawner.spawnItem(item, square, x, y, z, true);
    }

    public static InventoryItem spawnItem(InventoryItem item, IsoGridSquare square) {
        return ItemSpawner.spawnItem(item, square, 0.0f, 0.0f, 0.0f, true);
    }

    public static InventoryItem spawnItem(InventoryItem item, IsoGridSquare square, boolean fill) {
        return ItemSpawner.spawnItem(item, square, 0.0f, 0.0f, 0.0f, fill);
    }

    public static InventoryItem spawnItem(String itemType, IsoGridSquare square, float x, float y, float z, boolean fill) {
        return ItemSpawner.spawnItem(InventoryItemFactory.CreateItem(itemType), square, x, y, z, fill);
    }

    public static InventoryItem spawnItem(String itemType, IsoGridSquare square, float x, float y, float z) {
        return ItemSpawner.spawnItem(InventoryItemFactory.CreateItem(itemType), square, x, y, z, true);
    }

    public static InventoryItem spawnItem(InventoryItem item, ItemContainer container, boolean fill) {
        if (!container.isItemAllowed(item)) {
            return null;
        }
        container.AddItem(item);
        ItemSpawner.inc(item, 1);
        if (fill && item instanceof InventoryContainer) {
            InventoryContainer inventoryContainer = (InventoryContainer)item;
            if (ItemPickerJava.containers.containsKey(item.getType())) {
                ItemPickerJava.rollContainerItem(inventoryContainer, null, ItemPickerJava.getItemPickerContainers().get(item.getType()));
                LuaEventManager.triggerEvent("OnFillContainer", "Container", item.getType(), inventoryContainer.getItemContainer());
            }
        }
        item.setAutoAge();
        item.setAutoAge();
        return item;
    }

    public static InventoryItem spawnItem(InventoryItem item, ItemContainer container) {
        return ItemSpawner.spawnItem(item, container, true);
    }

    public static InventoryItem spawnItem(String itemType, ItemContainer container, boolean fill) {
        return ItemSpawner.spawnItem(InventoryItemFactory.CreateItem(itemType), container, fill);
    }

    public static InventoryItem spawnItem(String itemType, ItemContainer container) {
        return ItemSpawner.spawnItem(InventoryItemFactory.CreateItem(itemType), container, true);
    }
}

