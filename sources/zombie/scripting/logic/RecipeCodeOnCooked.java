/*
 * Decompiled with CFR 0.152.
 */
package zombie.scripting.logic;

import zombie.UsedFromLua;
import zombie.inventory.types.Food;
import zombie.scripting.ScriptManager;
import zombie.scripting.logic.RecipeCodeHelper;
import zombie.scripting.objects.ItemKey;

@UsedFromLua
public class RecipeCodeOnCooked
extends RecipeCodeHelper {
    public static void cannedFood(Food food) {
        float aged = food.getAge() / (float)food.getOffAgeMax();
        food.setOffAgeMax(1560);
        food.setOffAge(730);
        food.setAge((float)food.getOffAgeMax() * aged);
    }

    public static void nameCakePrep(Food cake) {
        cake.setCustomName(true);
        cake.setName(ScriptManager.instance.getItem(ItemKey.Food.CAKE_RAW.toString()).getDisplayName());
    }
}

