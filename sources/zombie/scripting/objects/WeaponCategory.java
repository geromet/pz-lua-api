/*
 * Decompiled with CFR 0.152.
 */
package zombie.scripting.objects;

import zombie.UsedFromLua;
import zombie.scripting.objects.Registries;
import zombie.scripting.objects.RegistryReset;
import zombie.scripting.objects.ResourceLocation;

@UsedFromLua
public class WeaponCategory {
    public static final WeaponCategory AXE = WeaponCategory.registerBase("Axe");
    public static final WeaponCategory BLUNT = WeaponCategory.registerBase("Blunt");
    public static final WeaponCategory IMPROVISED = WeaponCategory.registerBase("Improvised");
    public static final WeaponCategory LONG_BLADE = WeaponCategory.registerBase("LongBlade");
    public static final WeaponCategory SMALL_BLADE = WeaponCategory.registerBase("SmallBlade");
    public static final WeaponCategory SMALL_BLUNT = WeaponCategory.registerBase("SmallBlunt");
    public static final WeaponCategory SPEAR = WeaponCategory.registerBase("Spear");
    public static final WeaponCategory UNARMED = WeaponCategory.registerBase("Unarmed");
    private final String translationName;

    private WeaponCategory(String translationName) {
        this.translationName = translationName;
    }

    public static WeaponCategory get(ResourceLocation id) {
        return Registries.WEAPON_CATEGORY.get(id);
    }

    public String toString() {
        return Registries.WEAPON_CATEGORY.getLocation(this).toString();
    }

    public String getTranslationName() {
        return this.translationName;
    }

    public static WeaponCategory register(String id) {
        return WeaponCategory.register(false, id);
    }

    private static WeaponCategory registerBase(String id) {
        return WeaponCategory.register(true, id);
    }

    private static WeaponCategory register(boolean allowDefaultNamespace, String id) {
        return Registries.WEAPON_CATEGORY.register(RegistryReset.createLocation(id, allowDefaultNamespace), new WeaponCategory(id));
    }
}

