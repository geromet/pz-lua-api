/*
 * Decompiled with CFR 0.152.
 */
package zombie.scripting.logic;

import java.util.List;
import se.krka.kahlua.j2se.KahluaTableImpl;
import zombie.UsedFromLua;
import zombie.characters.IsoGameCharacter;
import zombie.entity.ComponentType;
import zombie.entity.components.fluids.Fluid;
import zombie.inventory.InventoryItem;
import zombie.inventory.types.Clothing;
import zombie.inventory.types.DrainableComboItem;
import zombie.inventory.types.Food;
import zombie.inventory.types.InventoryContainer;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoObject;
import zombie.iso.objects.IsoBarbecue;
import zombie.iso.objects.IsoFireplace;
import zombie.iso.objects.IsoThumpable;
import zombie.scripting.logic.RecipeCodeHelper;
import zombie.scripting.objects.Item;
import zombie.scripting.objects.ItemTag;
import zombie.util.list.PZArrayList;

@UsedFromLua
public class RecipeCodeOnTest
extends RecipeCodeHelper {
    public static boolean cutFish(InventoryItem item, IsoGameCharacter character) {
        if (item instanceof Food) {
            Food food = (Food)item;
            return food.getActualWeight() > 0.6f;
        }
        return true;
    }

    public static boolean breakGlass(InventoryItem item, IsoGameCharacter character) {
        return item.getFluidContainer() == null || item.getFluidContainer().isEmpty();
    }

    public static boolean hotFluidContainer(InventoryItem item, IsoGameCharacter character) {
        return !item.hasComponent(ComponentType.FluidContainer) || item.getItemHeat() > 1.6f;
    }

    public static boolean cutFillet(InventoryItem item, IsoGameCharacter character) {
        if (item instanceof Food) {
            Food foodItem = (Food)item;
            return foodItem.getActualWeight() > 1.0f;
        }
        return true;
    }

    public static boolean purifyWater(InventoryItem item, IsoGameCharacter character) {
        if (item.hasComponent(ComponentType.FluidContainer)) {
            return item.getFluidContainer().contains(Fluid.TaintedWater);
        }
        return true;
    }

    public static boolean canAddToPack(InventoryItem item, IsoGameCharacter character) {
        if (!(item instanceof DrainableComboItem)) {
            return true;
        }
        DrainableComboItem drainable = (DrainableComboItem)item;
        if (item.hasTag(ItemTag.PACKED)) {
            return drainable.getCurrentUsesFloat() < 1.0f;
        }
        return true;
    }

    public static boolean genericPacking(InventoryItem item, IsoGameCharacter character) {
        InventoryContainer cont;
        Clothing clothing;
        Food food;
        if (item instanceof Food && (food = (Food)item).isNormalAndFullFood()) {
            return false;
        }
        if (item.getBloodLevel() > 0.0f) {
            return false;
        }
        if (item instanceof Clothing && (clothing = (Clothing)item).getDirtiness() > 0.0f) {
            return false;
        }
        if (item instanceof InventoryContainer && !(cont = (InventoryContainer)item).isEmpty()) {
            return false;
        }
        if (item.getCondition() != item.getConditionMax()) {
            return false;
        }
        Item scriptItem = item.getScriptItem();
        return item.getColorRed() == scriptItem.getColorRed() && item.getColorGreen() == scriptItem.getColorGreen() && item.getColorBlue() == scriptItem.getColorBlue();
    }

    public static boolean scratchTicket(InventoryItem item, IsoGameCharacter character) {
        return !((KahluaTableImpl)item.getModData()).rawgetBool("scratched");
    }

    public static boolean haveFilter(InventoryItem item, IsoGameCharacter character) {
        return ((Clothing)item).hasFilter();
    }

    public static boolean noFilter(InventoryItem item, IsoGameCharacter character) {
        if (item instanceof Clothing) {
            Clothing clothing = (Clothing)item;
            return !clothing.hasFilter();
        }
        return true;
    }

    public static boolean haveOxygenTank(InventoryItem item, IsoGameCharacter character) {
        return ((Clothing)item).hasTank();
    }

    public static boolean noOxygenTank(InventoryItem item, IsoGameCharacter character) {
        if (item instanceof Clothing) {
            Clothing clothing = (Clothing)item;
            return !clothing.hasTank();
        }
        return true;
    }

    public static boolean openFire(InventoryItem item, IsoGameCharacter character) {
        List<IsoGridSquare> squares = character.getCurrentSquare().getRadius(2);
        for (int i = 0; i < squares.size(); ++i) {
            PZArrayList<IsoObject> objects = squares.get(i).getObjects();
            for (int i1 = 0; i1 < objects.size(); ++i1) {
                IsoObject obj = objects.get(i1);
                if (!(obj instanceof IsoFireplace || obj instanceof IsoBarbecue ? obj.isLit() : obj instanceof IsoThumpable && ((KahluaTableImpl)obj.getModData()).rawgetBool("isLit"))) continue;
                return true;
            }
        }
        return character.getSquare().hasAdjacentFireObject();
    }

    public static boolean copyKey(InventoryItem item, IsoGameCharacter character) {
        return !item.hasTag(ItemTag.BUILDING_KEY) || item.getKeyId() != -1;
    }
}

