/*
 * Decompiled with CFR 0.152.
 */
package zombie.core.skinnedmodel.visual;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import zombie.UsedFromLua;
import zombie.core.skinnedmodel.population.ClothingItem;
import zombie.core.skinnedmodel.visual.ItemVisual;

@UsedFromLua
public final class ItemVisuals
extends ArrayList<ItemVisual> {
    public void save(ByteBuffer output) throws IOException {
        output.putShort((short)this.size());
        for (int i = 0; i < this.size(); ++i) {
            ((ItemVisual)this.get(i)).save(output);
        }
    }

    public void load(ByteBuffer input, int worldVersion) throws IOException {
        this.clear();
        int count = input.getShort();
        for (int i = 0; i < count; ++i) {
            ItemVisual itemVisual = new ItemVisual();
            itemVisual.load(input, worldVersion);
            this.add(itemVisual);
        }
    }

    public ItemVisual findHat() {
        for (int i = 0; i < this.size(); ++i) {
            ItemVisual itemVisual = (ItemVisual)this.get(i);
            ClothingItem clothingItem = itemVisual.getClothingItem();
            if (clothingItem == null || !clothingItem.isHat()) continue;
            return itemVisual;
        }
        return null;
    }

    public ItemVisual findMask() {
        for (int i = 0; i < this.size(); ++i) {
            ItemVisual itemVisual = (ItemVisual)this.get(i);
            ClothingItem clothingItem = itemVisual.getClothingItem();
            if (clothingItem == null || !clothingItem.isMask()) continue;
            return itemVisual;
        }
        return null;
    }

    private boolean contains(String itemType) {
        for (ItemVisual item : this) {
            if (!item.getItemType().equals(itemType)) continue;
            return true;
        }
        return false;
    }

    public String getDescription() {
        Object s = "{ \"ItemVisuals\" : [ ";
        for (int i = 0; i < this.size(); ++i) {
            s = (String)s + ((ItemVisual)this.get(i)).getDescription();
            if (i >= this.size() - 1) continue;
            s = (String)s + ", ";
        }
        s = (String)s + " ] }";
        return s;
    }
}

