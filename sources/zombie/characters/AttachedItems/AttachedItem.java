/*
 * Decompiled with CFR 0.152.
 */
package zombie.characters.AttachedItems;

import zombie.UsedFromLua;
import zombie.inventory.InventoryItem;

@UsedFromLua
public final class AttachedItem {
    protected final String location;
    protected final InventoryItem item;

    public AttachedItem(String location, InventoryItem item) {
        if (location == null) {
            throw new NullPointerException("location is null");
        }
        if (location.isEmpty()) {
            throw new IllegalArgumentException("location is empty");
        }
        if (item == null) {
            throw new NullPointerException("item is null");
        }
        this.location = location;
        this.item = item;
    }

    public String getLocation() {
        return this.location;
    }

    public InventoryItem getItem() {
        return this.item;
    }
}

