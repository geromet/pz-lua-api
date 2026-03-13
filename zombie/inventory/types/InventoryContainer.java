/*
 * Decompiled with CFR 0.152.
 */
package zombie.inventory.types;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashSet;
import zombie.UsedFromLua;
import zombie.ZomboidGlobals;
import zombie.characterTextures.BloodBodyPartType;
import zombie.characterTextures.BloodClothingType;
import zombie.characters.IsoGameCharacter;
import zombie.core.Translator;
import zombie.core.math.PZMath;
import zombie.debug.DebugLog;
import zombie.inventory.InventoryItem;
import zombie.inventory.ItemContainer;
import zombie.scripting.objects.ItemBodyLocation;
import zombie.scripting.objects.ItemType;
import zombie.ui.ObjectTooltip;
import zombie.ui.TextManager;

@UsedFromLua
public final class InventoryContainer
extends InventoryItem {
    ItemContainer container = new ItemContainer();
    int capacity;
    int weightReduction;
    private ItemBodyLocation canBeEquipped;
    private static final int MAX_CAPACITY_BAG = 50;

    public InventoryContainer(String module, String name, String itemType, String texName) {
        super(module, name, itemType, texName);
        this.container.containingItem = this;
        this.container.type = itemType;
        this.container.inventoryContainer = this;
        this.itemType = ItemType.CONTAINER;
    }

    @Override
    public boolean IsInventoryContainer() {
        return true;
    }

    @Override
    public String getCategory() {
        if (this.mainCategory != null) {
            return this.mainCategory;
        }
        return "Container";
    }

    public ItemContainer getInventory() {
        return this.container;
    }

    @Override
    public void save(ByteBuffer output, boolean net) throws IOException {
        super.save(output, net);
        output.putInt(this.container.id);
        output.putInt(this.weightReduction);
        this.container.save(output);
    }

    @Override
    public void load(ByteBuffer input, int worldVersion) throws IOException {
        super.load(input, worldVersion);
        int con = input.getInt();
        this.setWeightReduction(input.getInt());
        if (this.container == null) {
            this.container = new ItemContainer();
        }
        this.container.clear();
        this.container.containingItem = this;
        this.container.setWeightReduction(this.weightReduction);
        this.container.capacity = this.capacity;
        this.container.id = con;
        this.container.load(input, worldVersion);
        this.synchWithVisual();
    }

    public int getCapacity() {
        int capacity = this.container.getCapacity();
        if ((float)capacity > 50.0f - this.actualWeight) {
            capacity = (int)(50.0f - this.actualWeight);
        }
        return capacity;
    }

    public void setCapacity(int capacity) {
        this.capacity = capacity;
        if (this.container == null) {
            this.container = new ItemContainer();
        }
        if ((float)capacity > 50.0f - this.actualWeight) {
            DebugLog.General.warn("Attempting to set capacity of " + String.valueOf(this) + "over maximum capacity of " + (50.0f - this.actualWeight));
        }
        this.container.setCapacity(capacity);
    }

    public float getMaxItemSize() {
        return this.getScriptItem().getMaxItemSize();
    }

    public float getInventoryWeight() {
        if (this.getInventory() == null) {
            return 0.0f;
        }
        float total = 0.0f;
        ArrayList<InventoryItem> items = this.getInventory().getItems();
        for (int i = 0; i < items.size(); ++i) {
            InventoryItem item = items.get(i);
            if (this.isEquipped() || this.isFakeEquipped()) {
                total += item.getEquippedWeight();
                continue;
            }
            total += item.getUnequippedWeight();
        }
        return total;
    }

    public int getEffectiveCapacity(IsoGameCharacter chr) {
        int capacity = this.container.getEffectiveCapacity(chr);
        if ((float)capacity > 50.0f - this.actualWeight) {
            capacity = (int)(50.0f - this.actualWeight);
        }
        return capacity;
    }

    public int getWeightReduction() {
        return this.weightReduction;
    }

    public void setWeightReduction(int weightReduction) {
        weightReduction = Math.min(weightReduction, 100);
        this.weightReduction = weightReduction = Math.max(weightReduction, 0);
        this.container.setWeightReduction(weightReduction);
    }

    @Override
    public void updateAge() {
        ArrayList<InventoryItem> items = this.getInventory().getItems();
        for (int i = 0; i < items.size(); ++i) {
            items.get(i).updateAge();
        }
    }

    @Override
    public void DoTooltip(ObjectTooltip tooltipUI) {
        tooltipUI.render();
        super.DoTooltip(tooltipUI);
        int y = tooltipUI.getHeight().intValue();
        y -= tooltipUI.padBottom;
        if (tooltipUI.getWidth() < 160.0) {
            tooltipUI.setWidth(160.0);
        }
        if (!this.getItemContainer().getItems().isEmpty()) {
            int x = tooltipUI.padLeft;
            y += 4;
            HashSet<String> names = new HashSet<String>();
            int iconSize = Math.max(16, TextManager.instance.getFontHeight(tooltipUI.getFont()));
            for (int i = this.getItemContainer().getItems().size() - 1; i >= 0; --i) {
                InventoryItem item = this.getItemContainer().getItems().get(i);
                if (item.getName() != null) {
                    if (names.contains(item.getName())) continue;
                    names.add(item.getName());
                }
                tooltipUI.DrawTextureScaledAspect(item.getTex(), x, y, iconSize, iconSize, 1.0, 1.0, 1.0, 1.0);
                if ((float)((x += iconSize + 1) + iconSize) > tooltipUI.width - (float)tooltipUI.padRight) break;
            }
            y += iconSize;
        }
        tooltipUI.setHeight(y += tooltipUI.padBottom);
    }

    @Override
    public void DoTooltip(ObjectTooltip tooltipUI, ObjectTooltip.Layout layout) {
        float totalBlood;
        ObjectTooltip.LayoutItem item;
        float br = 0.0f;
        float bg = 0.6f;
        float bb = 0.0f;
        float ba = 0.7f;
        if (this.getEffectiveCapacity(tooltipUI.getCharacter()) != 0) {
            item = layout.addItem();
            item.setLabel(Translator.getText("Tooltip_container_Capacity") + ":", 1.0f, 1.0f, 1.0f, 1.0f);
            item.setValueRightNoPlus(this.getEffectiveCapacity(tooltipUI.getCharacter()));
        }
        if (this.getWeightReduction() != 0) {
            item = layout.addItem();
            item.setLabel(Translator.getText("Tooltip_container_Weight_Reduction") + ":", 1.0f, 1.0f, 1.0f, 1.0f);
            item.setValueRightNoPlus(this.getWeightReduction());
        }
        if (this.getMaxItemSize() != 0.0f) {
            item = layout.addItem();
            item.setLabel(Translator.getText("Tooltip_container_Max_Item_Size") + ":", 1.0f, 1.0f, 1.0f, 1.0f);
            item.setValueRightNoPlus(this.getMaxItemSize());
        }
        if (this.getBloodClothingType() != null && (totalBlood = this.getBloodLevel()) != 0.0f) {
            item = layout.addItem();
            item.setLabel(Translator.getText("Tooltip_clothing_bloody") + ":", 1.0f, 1.0f, 0.8f, 1.0f);
            item.setProgress(totalBlood, 0.0f, 0.6f, 0.0f, 0.7f);
        }
    }

    @Override
    public void setBloodLevel(float delta) {
        ArrayList<BloodBodyPartType> coveredParts = BloodClothingType.getCoveredParts(this.getBloodClothingType());
        for (int i = 0; i < coveredParts.size(); ++i) {
            this.setBlood(coveredParts.get(i), PZMath.clamp(delta, 0.0f, 100.0f));
        }
    }

    @Override
    public float getBloodLevel() {
        ArrayList<BloodBodyPartType> coveredParts = BloodClothingType.getCoveredParts(this.getBloodClothingType());
        float totalBlood = 0.0f;
        for (int i = 0; i < coveredParts.size(); ++i) {
            totalBlood += this.getBlood(coveredParts.get(i));
        }
        return totalBlood;
    }

    public float getDirtiness() {
        ArrayList<BloodBodyPartType> coveredParts = BloodClothingType.getCoveredParts(this.getBloodClothingType());
        float totalDirt = 0.0f;
        for (int i = 0; i < coveredParts.size(); ++i) {
            totalDirt += this.getDirt((BloodBodyPartType)((Object)coveredParts.get(i)));
        }
        return totalDirt;
    }

    public void setCanBeEquipped(ItemBodyLocation canBeEquipped) {
        this.canBeEquipped = canBeEquipped;
    }

    @Override
    public ItemBodyLocation canBeEquipped() {
        return this.canBeEquipped;
    }

    public ItemContainer getItemContainer() {
        return this.container;
    }

    public void setItemContainer(ItemContainer cont) {
        this.container = cont;
    }

    @Override
    public float getContentsWeight() {
        return this.getInventory().getContentsWeight();
    }

    @Override
    public float getEquippedWeight() {
        float weightReduction = 1.0f;
        if (this.getWeightReduction() > 0) {
            weightReduction = 1.0f - (float)this.getWeightReduction() / 100.0f;
        }
        return this.getActualWeight() * (float)ZomboidGlobals.equippedOrWornEncumbranceMultiplier + this.getContentsWeight() * weightReduction;
    }

    public String getClothingExtraSubmenu() {
        return this.scriptItem.clothingExtraSubmenu;
    }

    @Override
    public void reset() {
        super.reset();
        this.container.reset();
    }

    public boolean isEmpty() {
        return this.container.isEmpty();
    }
}

