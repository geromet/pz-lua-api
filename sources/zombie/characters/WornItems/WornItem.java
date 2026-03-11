/*
 * Decompiled with CFR 0.152.
 */
package zombie.characters.WornItems;

import zombie.UsedFromLua;
import zombie.inventory.InventoryItem;
import zombie.scripting.objects.ItemBodyLocation;

@UsedFromLua
public final class WornItem {
    private final ItemBodyLocation itemBodyLocation;
    private final InventoryItem item;

    public WornItem(ItemBodyLocation itemBodyLocation, InventoryItem item) {
        if (item == null) {
            throw new NullPointerException("item is null");
        }
        this.itemBodyLocation = itemBodyLocation;
        this.item = item;
    }

    public ItemBodyLocation getLocation() {
        return this.itemBodyLocation;
    }

    public InventoryItem getItem() {
        return this.item;
    }
}

