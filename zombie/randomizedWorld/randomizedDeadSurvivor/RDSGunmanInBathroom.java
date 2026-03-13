/*
 * Decompiled with CFR 0.152.
 */
package zombie.randomizedWorld.randomizedDeadSurvivor;

import zombie.UsedFromLua;
import zombie.core.random.Rand;
import zombie.inventory.InventoryItem;
import zombie.inventory.InventoryItemFactory;
import zombie.inventory.ItemContainer;
import zombie.inventory.types.HandWeapon;
import zombie.iso.BuildingDef;
import zombie.iso.RoomDef;
import zombie.iso.objects.IsoDeadBody;
import zombie.randomizedWorld.randomizedDeadSurvivor.RandomizedDeadSurvivorBase;
import zombie.scripting.objects.ItemKey;

@UsedFromLua
public final class RDSGunmanInBathroom
extends RandomizedDeadSurvivorBase {
    private static final int PROBABILITY_RIFLE_HUNTING = 50;
    private static final int PROBABILITY_RIFLE_M14 = 85;
    private static final int PROBABILITY_SHOTGUN = 20;
    private static final int PROBABILITY_PISTOL1 = 20;
    private static final int PROBABILITY_PISTOL2 = 40;
    private static final int PROBABILITY_PISTOL3 = 95;
    private static final int PROBABILITY_REVOLVER = 65;
    private static final int PROBABILITY_REVOLVER_LONG = 85;
    private static final int PROBABILITY_SPAWN_RIFLE = 20;
    private static final int PROBABILITY_SPAWN_AMMO = 60;
    private static final int PROBABILITY_SPAWN_CLIP = 60;

    @Override
    public void randomizeDeadSurvivor(BuildingDef def) {
        Object ammoBox;
        String gunType;
        RoomDef room = this.getRoom(def, "bathroom");
        IsoDeadBody body = RDSGunmanInBathroom.createRandomDeadBody(room, Rand.Next(5, 10));
        if (body == null) {
            return;
        }
        HandWeapon gun = RDSGunmanInBathroom.gunPicker(Rand.NextBool(20));
        if (gun == null) {
            return;
        }
        if (gun.usesExternalMagazine() && !gun.isContainsClip()) {
            gun.setContainsClip(true);
        }
        if ((gunType = gun.getType()).contains("Shotgun") || gunType.contains("Hunting") || gunType.contains("Varmint") || gunType.contains("Revolver")) {
            gun.setSpentRoundChambered(true);
            gun.setCurrentAmmoCount(gun.getMaxAmmo() - 1);
        } else {
            gun.setRoundChambered(true);
            gun.setCurrentAmmoCount(gun.getMaxAmmo() - 2);
        }
        gun.setBloodLevel(Rand.Next(0.5f, 1.0f));
        ItemContainer bodyContainer = body.getContainer();
        if (Rand.Next(100) >= 60) {
            ammoBox = InventoryItemFactory.CreateItem(gun.getAmmoBox());
            bodyContainer.AddItem((InventoryItem)ammoBox);
        }
        if (gun.usesExternalMagazine()) {
            if (Rand.Next(100) >= 60) {
                int clipCount = Rand.NextInclusive(1, 3);
                for (int i = 0; i < clipCount; ++i) {
                    Object clip = InventoryItemFactory.CreateItem(gun.getMagazineType());
                    ((InventoryItem)clip).setCurrentAmmoCount(((InventoryItem)clip).getMaxAmmo());
                    bodyContainer.AddItem((InventoryItem)clip);
                }
            }
        } else if (Rand.Next(100) >= 60) {
            ammoBox = InventoryItemFactory.CreateItem(gun.getAmmoBox());
            bodyContainer.AddItem((InventoryItem)ammoBox);
        }
        body.setPrimaryHandItem(gun);
        body.getSquare().splatBlood(4, 1.0f);
    }

    private static HandWeapon gunPicker(boolean isRifle) {
        if (isRifle) {
            return (HandWeapon)InventoryItemFactory.CreateItem(RDSGunmanInBathroom.riflePicker(Rand.Next(100)));
        }
        return (HandWeapon)InventoryItemFactory.CreateItem(RDSGunmanInBathroom.pistolPicker(Rand.Next(100)));
    }

    private static ItemKey riflePicker(int roll) {
        if (roll >= 85) {
            return ItemKey.Weapon.ASSAULT_RIFLE_2;
        }
        if (roll >= 50) {
            return ItemKey.Weapon.HUNTING_RIFLE;
        }
        if (roll >= 20) {
            return ItemKey.Weapon.SHOTGUN;
        }
        return ItemKey.Weapon.VARMINT_RIFLE;
    }

    private static ItemKey pistolPicker(int roll) {
        if (roll >= 95) {
            return ItemKey.Weapon.PISTOL_3;
        }
        if (roll >= 85) {
            return ItemKey.Weapon.REVOLVER_LONG;
        }
        if (roll >= 65) {
            return ItemKey.Weapon.REVOLVER;
        }
        if (roll >= 40) {
            return ItemKey.Weapon.PISTOL_2;
        }
        if (roll >= 20) {
            return ItemKey.Weapon.PISTOL;
        }
        return ItemKey.Weapon.REVOLVER_SHORT;
    }

    public RDSGunmanInBathroom() {
        this.name = "Bathroom Gunman";
        this.setChance(5);
    }
}

