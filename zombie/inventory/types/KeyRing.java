/*
 * Decompiled with CFR 0.152.
 */
package zombie.inventory.types;

import java.util.ArrayList;
import zombie.UsedFromLua;
import zombie.inventory.InventoryItem;
import zombie.inventory.types.Key;
import zombie.scripting.objects.ItemType;

@UsedFromLua
public final class KeyRing
extends InventoryItem {
    private final ArrayList<Key> keys = new ArrayList();

    public KeyRing(String module, String name, String type, String tex) {
        super(module, name, type, tex);
        this.itemType = ItemType.KEY_RING;
    }

    public void addKey(Key key) {
        this.keys.add(key);
    }

    public boolean containsKeyId(int keyId) {
        for (int i = 0; i < this.keys.size(); ++i) {
            if (this.keys.get(i).getKeyId() != keyId) continue;
            return true;
        }
        return false;
    }

    @Override
    public String getCategory() {
        if (this.mainCategory != null) {
            return this.mainCategory;
        }
        return "Key Ring";
    }

    public ArrayList<Key> getKeys() {
        return this.keys;
    }

    public void setKeys(ArrayList<Key> keys2) {
        keys2.clear();
        this.keys.addAll(keys2);
    }
}

