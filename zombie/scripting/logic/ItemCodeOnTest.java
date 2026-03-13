/*
 * Decompiled with CFR 0.152.
 */
package zombie.scripting.logic;

import java.util.ArrayList;
import zombie.UsedFromLua;
import zombie.characters.IsoGameCharacter;
import zombie.inventory.InventoryItem;
import zombie.inventory.types.HandWeapon;
import zombie.inventory.types.WeaponPart;
import zombie.scripting.objects.ItemTag;

@UsedFromLua
public class ItemCodeOnTest {
    public static boolean hasScrewdriver(IsoGameCharacter character, HandWeapon weapon, WeaponPart part) {
        if (character == null) {
            return true;
        }
        ArrayList<InventoryItem> allTag = character.getInventory().getAllTag(ItemTag.SCREWDRIVER);
        for (InventoryItem item : allTag) {
            if (!item.isBroken()) continue;
            return false;
        }
        return true;
    }
}

