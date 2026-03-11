/*
 * Decompiled with CFR 0.152.
 */
package zombie.scripting.objects;

import zombie.UsedFromLua;
import zombie.scripting.objects.Registries;
import zombie.scripting.objects.RegistryReset;
import zombie.scripting.objects.ResourceLocation;

@UsedFromLua
public class ItemType {
    public static final ItemType ALARM_CLOCK = ItemType.registerBase("AlarmClock");
    public static final ItemType ALARM_CLOCK_CLOTHING = ItemType.registerBase("AlarmClockClothing");
    public static final ItemType ANIMAL = ItemType.registerBase("Animal");
    public static final ItemType CLOTHING = ItemType.registerBase("Clothing");
    public static final ItemType CONTAINER = ItemType.registerBase("Container");
    public static final ItemType DRAINABLE = ItemType.registerBase("Drainable");
    public static final ItemType FOOD = ItemType.registerBase("Food");
    public static final ItemType KEY = ItemType.registerBase("Key");
    public static final ItemType KEY_RING = ItemType.registerBase("KeyRing");
    public static final ItemType LITERATURE = ItemType.registerBase("Literature");
    public static final ItemType MAP = ItemType.registerBase("Map");
    public static final ItemType MOVEABLE = ItemType.registerBase("Moveable");
    public static final ItemType NORMAL = ItemType.registerBase("Normal");
    public static final ItemType RADIO = ItemType.registerBase("Radio");
    public static final ItemType WEAPON = ItemType.registerBase("Weapon");
    public static final ItemType WEAPON_PART = ItemType.registerBase("WeaponPart");

    private ItemType() {
    }

    public static ItemType get(ResourceLocation id) {
        return Registries.ITEM_TYPE.get(id);
    }

    public String toString() {
        return Registries.ITEM_TYPE.getLocation(this).toString();
    }

    public static ItemType register(String id) {
        return ItemType.register(false, id);
    }

    private static ItemType registerBase(String id) {
        return ItemType.register(true, id);
    }

    private static ItemType register(boolean allowDefaultNamespace, String id) {
        return Registries.ITEM_TYPE.register(RegistryReset.createLocation(id, allowDefaultNamespace), new ItemType());
    }
}

