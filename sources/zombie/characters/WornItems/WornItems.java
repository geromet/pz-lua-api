/*
 * Decompiled with CFR 0.152.
 */
package zombie.characters.WornItems;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import zombie.GameWindow;
import zombie.UsedFromLua;
import zombie.characters.WornItems.BodyLocationGroup;
import zombie.characters.WornItems.WornItem;
import zombie.core.Color;
import zombie.core.ImmutableColor;
import zombie.core.skinnedmodel.visual.ItemVisual;
import zombie.core.skinnedmodel.visual.ItemVisuals;
import zombie.core.textures.Texture;
import zombie.inventory.InventoryItem;
import zombie.inventory.InventoryItemFactory;
import zombie.inventory.ItemContainer;
import zombie.inventory.types.Clothing;
import zombie.scripting.objects.ItemBodyLocation;
import zombie.scripting.objects.ResourceLocation;

@UsedFromLua
public final class WornItems {
    private final BodyLocationGroup group;
    private final List<WornItem> items = new ArrayList<WornItem>();

    public WornItems(BodyLocationGroup group) {
        this.group = group;
    }

    public WornItems(WornItems other) {
        this.group = other.group;
        this.copyFrom(other);
    }

    public void copyFrom(WornItems other) {
        if (this.group != other.group) {
            throw new RuntimeException("group=" + this.group.getId() + " other.group=" + other.group.getId());
        }
        this.items.clear();
        this.items.addAll(other.items);
    }

    public BodyLocationGroup getBodyLocationGroup() {
        return this.group;
    }

    public WornItem get(int index) {
        return this.items.get(index);
    }

    public void setItem(ItemBodyLocation location, InventoryItem item) {
        int index;
        if (!this.group.isMultiItem(location) && (index = this.indexOf(location)) != -1) {
            this.items.remove(index);
        }
        for (int i = 0; i < this.items.size(); ++i) {
            WornItem wornItem = this.items.get(i);
            if (!this.group.isExclusive(location, wornItem.getLocation())) continue;
            this.items.remove(i--);
        }
        if (item == null) {
            return;
        }
        this.remove(item);
        int insertAt = this.items.size();
        for (int i = 0; i < this.items.size(); ++i) {
            WornItem wornItem1 = this.items.get(i);
            if (this.group.indexOf(wornItem1.getLocation()) <= this.group.indexOf(location)) continue;
            insertAt = i;
            break;
        }
        WornItem wornItem = new WornItem(location, item);
        this.items.add(insertAt, wornItem);
    }

    public InventoryItem getItem(ItemBodyLocation location) {
        int index = this.indexOf(location);
        if (index == -1) {
            return null;
        }
        return this.items.get(index).getItem();
    }

    public InventoryItem getItemByIndex(int index) {
        if (index < 0 || index >= this.items.size()) {
            return null;
        }
        return this.items.get(index).getItem();
    }

    public void remove(InventoryItem item) {
        int index = this.indexOf(item);
        if (index == -1) {
            return;
        }
        this.items.remove(index);
    }

    public void clear() {
        this.items.clear();
    }

    public ItemBodyLocation getLocation(InventoryItem item) {
        int index = this.indexOf(item);
        if (index == -1) {
            return null;
        }
        return this.items.get(index).getLocation();
    }

    public boolean contains(InventoryItem item) {
        return this.indexOf(item) != -1;
    }

    public int size() {
        return this.items.size();
    }

    public boolean isEmpty() {
        return this.items.isEmpty();
    }

    public void forEach(Consumer<WornItem> c) {
        for (int i = 0; i < this.items.size(); ++i) {
            c.accept(this.items.get(i));
        }
    }

    public void setFromItemVisuals(ItemVisuals itemVisuals) {
        this.clear();
        for (int i = 0; i < itemVisuals.size(); ++i) {
            ItemVisual itemVisual = (ItemVisual)itemVisuals.get(i);
            String itemType = itemVisual.getItemType();
            Object item = InventoryItemFactory.CreateItem(itemType);
            if (item == null) continue;
            if (((InventoryItem)item).getVisual() != null) {
                ((InventoryItem)item).getVisual().copyFrom(itemVisual);
                ((InventoryItem)item).synchWithVisual();
            }
            if (item instanceof Clothing) {
                this.setItem(((InventoryItem)item).getBodyLocation(), (InventoryItem)item);
                continue;
            }
            this.setItem(((InventoryItem)item).canBeEquipped(), (InventoryItem)item);
        }
    }

    public void getItemVisuals(ItemVisuals itemVisuals) {
        itemVisuals.clear();
        for (int i = 0; i < this.items.size(); ++i) {
            InventoryItem item = this.items.get(i).getItem();
            ItemVisual itemVisual = item.getVisual();
            if (itemVisual == null) continue;
            itemVisual.setInventoryItem(item);
            itemVisuals.add(itemVisual);
        }
    }

    public void addItemsToItemContainer(ItemContainer container) {
        for (int i = 0; i < this.items.size(); ++i) {
            InventoryItem item = this.items.get(i).getItem();
            int totalHoles = item.getVisual().getHolesNumber();
            item.setConditionNoSound(item.getConditionMax() - totalHoles * 3);
            container.AddItem(item);
        }
    }

    private int indexOf(ItemBodyLocation location) {
        for (int i = 0; i < this.items.size(); ++i) {
            WornItem item = this.items.get(i);
            if (!item.getLocation().equals(location)) continue;
            return i;
        }
        return -1;
    }

    private int indexOf(InventoryItem item) {
        for (int i = 0; i < this.items.size(); ++i) {
            WornItem wornItem = this.items.get(i);
            if (wornItem.getItem() != item) continue;
            return i;
        }
        return -1;
    }

    public void save(ByteBuffer output) throws IOException {
        int size = this.items.size();
        output.putShort((short)size);
        for (int i = 0; i < size; ++i) {
            WornItem wornItem = this.items.get(i);
            GameWindow.WriteString(output, wornItem.getLocation().toString());
            GameWindow.WriteString(output, wornItem.getItem().getType());
            GameWindow.WriteString(output, wornItem.getItem().getTex().getName());
            wornItem.getItem().col.save(output);
            output.putInt(wornItem.getItem().getVisual().getBaseTexture());
            output.putInt(wornItem.getItem().getVisual().getTextureChoice());
            ImmutableColor colorTint = wornItem.getItem().getVisual().getTint();
            output.putFloat(colorTint.r);
            output.putFloat(colorTint.g);
            output.putFloat(colorTint.b);
            output.putFloat(colorTint.a);
        }
    }

    public void load(ByteBuffer input, int worldVersion) throws IOException {
        int size = input.getShort();
        this.items.clear();
        for (int i = 0; i < size; ++i) {
            String location = GameWindow.ReadString(input);
            String type = GameWindow.ReadString(input);
            String tex = GameWindow.ReadString(input);
            Color color = new Color();
            color.load(input, worldVersion);
            int baseTexture = input.getInt();
            int textureChoice = input.getInt();
            ImmutableColor colorTint = new ImmutableColor(input.getFloat(), input.getFloat(), input.getFloat(), input.getFloat());
            Object item = InventoryItemFactory.CreateItem(type);
            if (item == null) continue;
            ((InventoryItem)item).setTexture(Texture.trygetTexture(tex));
            if (((InventoryItem)item).getTex() == null) {
                ((InventoryItem)item).setTexture(Texture.getSharedTexture("media/inventory/Question_On.png"));
            }
            Object worldTexture = tex.replace("Item_", "media/inventory/world/WItem_");
            worldTexture = (String)worldTexture + ".png";
            ((InventoryItem)item).setWorldTexture((String)worldTexture);
            ((InventoryItem)item).setColor(color);
            ((InventoryItem)item).getVisual().tint = new ImmutableColor(color);
            ((InventoryItem)item).getVisual().setBaseTexture(baseTexture);
            ((InventoryItem)item).getVisual().setTextureChoice(textureChoice);
            ((InventoryItem)item).getVisual().setTint(colorTint);
            this.items.add(new WornItem(ItemBodyLocation.get(ResourceLocation.of(location)), (InventoryItem)item));
        }
    }
}

