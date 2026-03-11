/*
 * Decompiled with CFR 0.152.
 */
package zombie.iso.objects.interfaces;

import java.util.ArrayList;
import zombie.UsedFromLua;
import zombie.characters.IsoGameCharacter;
import zombie.entity.components.crafting.recipe.CraftRecipeData;
import zombie.inventory.InventoryItem;
import zombie.inventory.InventoryItemFactory;
import zombie.iso.IsoGridSquare;
import zombie.iso.objects.IsoBarricade;

@UsedFromLua
public interface BarricadeAble {
    public boolean isBarricaded();

    public boolean isBarricadeAllowed();

    public IsoBarricade getBarricadeOnSameSquare();

    public IsoBarricade getBarricadeOnOppositeSquare();

    public IsoBarricade getBarricadeForCharacter(IsoGameCharacter var1);

    public IsoBarricade getBarricadeOppositeCharacter(IsoGameCharacter var1);

    public IsoGridSquare getSquare();

    public IsoGridSquare getOppositeSquare();

    public boolean getNorth();

    default public IsoBarricade addBarricadesFromCraftRecipe(IsoGameCharacter chr, ArrayList<InventoryItem> items, CraftRecipeData craftRecipeData, boolean opposite) {
        IsoBarricade barricade;
        if (items.isEmpty()) {
            String type = "Base.Plank";
            if (craftRecipeData.getRecipe().canUseItem("Base.SheetMetal")) {
                type = "Base.SheetMetal";
            } else if (craftRecipeData.getRecipe().canUseItem("Base.MetalBar")) {
                type = "Base.MetalBar";
            }
            Object item = InventoryItemFactory.CreateItem(type);
            items.add((InventoryItem)item);
        }
        if ((barricade = IsoBarricade.AddBarricadeToObject(this, opposite)) != null) {
            barricade.addFromCraftRecipe(chr, items);
        }
        return barricade;
    }
}

