/*
 * Decompiled with CFR 0.152.
 */
package zombie.inventory.types;

import zombie.AttackType;
import zombie.UsedFromLua;
import zombie.characters.IsoGameCharacter;
import zombie.inventory.InventoryItem;
import zombie.inventory.types.HandWeapon;
import zombie.util.list.WeightedList;

@UsedFromLua
public enum WeaponType {
    UNARMED("", WeaponType.list().add(AttackType.NONE, 10), true, false),
    TWO_HANDED("2handed", WeaponType.list().add(AttackType.DEFAULT, 20).add(AttackType.OVERHEAD, 10).add(AttackType.UPPERCUT, 10), true, false),
    ONE_HANDED("1handed", WeaponType.list().add(AttackType.DEFAULT, 20).add(AttackType.OVERHEAD, 10).add(AttackType.UPPERCUT, 10), true, false),
    HEAVY("heavy", WeaponType.list().add(AttackType.DEFAULT, 20).add(AttackType.OVERHEAD, 10), true, false),
    KNIFE("knife", WeaponType.list().add(AttackType.DEFAULT, 20).add(AttackType.OVERHEAD, 10).add(AttackType.UPPERCUT, 10), true, false),
    SPEAR("spear", WeaponType.list().add(AttackType.DEFAULT, 10), true, false),
    HANDGUN("handgun", WeaponType.list().add(AttackType.NONE, 10), false, true),
    FIREARM("firearm", WeaponType.list().add(AttackType.NONE, 10), false, true),
    THROWING("throwing", WeaponType.list().add(AttackType.NONE, 10), false, true),
    CHAINSAW("chainsaw", WeaponType.list().add(AttackType.DEFAULT, 10), true, false);

    private final String type;
    private final WeightedList<AttackType> possibleAttack;
    private final boolean canMiss;
    private final boolean isRanged;

    private static WeightedList<AttackType> list() {
        return new WeightedList<AttackType>();
    }

    private WeaponType(String type, WeightedList<AttackType> possibleAttack, boolean canMiss, boolean isRanged) {
        this.type = type;
        this.possibleAttack = possibleAttack;
        this.canMiss = canMiss;
        this.isRanged = isRanged;
    }

    public static WeaponType getWeaponType(HandWeapon weapon) {
        String swing = weapon.getSwingAnim();
        if (swing.equalsIgnoreCase("Stab")) {
            return KNIFE;
        }
        if (swing.equalsIgnoreCase("Heavy")) {
            return HEAVY;
        }
        if (swing.equalsIgnoreCase("Throw")) {
            return THROWING;
        }
        if (weapon.isRanged()) {
            if (weapon.isTwoHandWeapon()) {
                return FIREARM;
            }
            return HANDGUN;
        }
        if (weapon.isTwoHandWeapon()) {
            if (swing.equalsIgnoreCase("Spear")) {
                return SPEAR;
            }
            if ("Chainsaw".equals(weapon.getType())) {
                return CHAINSAW;
            }
            return TWO_HANDED;
        }
        return ONE_HANDED;
    }

    public static WeaponType getWeaponType(IsoGameCharacter chr) {
        return WeaponType.getWeaponType(chr, chr.getPrimaryHandItem(), chr.getSecondaryHandItem());
    }

    public static WeaponType getWeaponType(IsoGameCharacter chr, InventoryItem inv1, InventoryItem inv2) {
        if (chr == null) {
            return null;
        }
        WeaponType result = null;
        chr.setVariable("rangedWeapon", false);
        if (inv1 != null && inv1 instanceof HandWeapon) {
            HandWeapon handWeapon = (HandWeapon)inv1;
            if (inv1.getSwingAnim().equalsIgnoreCase("Stab")) {
                return KNIFE;
            }
            if (inv1.getSwingAnim().equalsIgnoreCase("Heavy")) {
                return HEAVY;
            }
            if (inv1.getSwingAnim().equalsIgnoreCase("Throw")) {
                chr.setVariable("rangedWeapon", true);
                return THROWING;
            }
            if (!handWeapon.isRanged()) {
                result = ONE_HANDED;
                if (inv1 == inv2 && inv1.isTwoHandWeapon()) {
                    result = TWO_HANDED;
                    if (inv1.getSwingAnim().equalsIgnoreCase("Spear")) {
                        return SPEAR;
                    }
                    if ("Chainsaw".equals(inv1.getType())) {
                        return CHAINSAW;
                    }
                }
            } else {
                result = HANDGUN;
                if (inv1 == inv2 && inv1.isTwoHandWeapon()) {
                    result = FIREARM;
                }
            }
        }
        if (result == null) {
            result = UNARMED;
        }
        chr.setVariable("rangedWeapon", result == HANDGUN || result == FIREARM);
        return result;
    }

    public String getType() {
        return this.type;
    }

    public WeightedList<AttackType> getPossibleAttack() {
        return this.possibleAttack;
    }

    public boolean isCanMiss() {
        return this.canMiss;
    }

    public boolean isRanged() {
        return this.isRanged;
    }
}

