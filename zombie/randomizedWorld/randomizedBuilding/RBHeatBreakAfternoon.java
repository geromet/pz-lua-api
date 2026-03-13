/*
 * Decompiled with CFR 0.152.
 */
package zombie.randomizedWorld.randomizedBuilding;

import java.util.ArrayList;
import zombie.UsedFromLua;
import zombie.characters.IsoZombie;
import zombie.core.random.Rand;
import zombie.inventory.InventoryItemFactory;
import zombie.inventory.ItemPickerJava;
import zombie.inventory.types.InventoryContainer;
import zombie.iso.BuildingDef;
import zombie.iso.IsoGridSquare;
import zombie.iso.RoomDef;
import zombie.iso.SpawnPoints;
import zombie.randomizedWorld.randomizedBuilding.RandomizedBuildingBase;
import zombie.vehicles.BaseVehicle;

@UsedFromLua
public final class RBHeatBreakAfternoon
extends RandomizedBuildingBase {
    @Override
    public void randomizeBuilding(BuildingDef def) {
        BaseVehicle v;
        def.alarmed = false;
        RoomDef room = def.getRoom("bank");
        if (room != null) {
            String outfit = "BankRobber";
            if (Rand.NextBool(2)) {
                outfit = "BankRobberSuit";
            }
            this.addZombies(def, Rand.Next(3, 5), outfit, 0, room);
            InventoryContainer bag = (InventoryContainer)InventoryItemFactory.CreateItem("Base.Bag_MoneyBag");
            ItemPickerJava.rollContainerItem(bag, null, ItemPickerJava.getItemPickerContainers().get(bag.getType()));
            IsoGridSquare sq = room.getFreeSquare();
            if (sq != null) {
                this.addItemOnGround(sq, bag);
            }
            if (Rand.Next(2) == 0) {
                bag = (InventoryContainer)InventoryItemFactory.CreateItem("Base.Bag_MoneyBag");
                ItemPickerJava.rollContainerItem(bag, null, ItemPickerJava.getItemPickerContainers().get(bag.getType()));
                sq = room.getFreeSquare();
                if (sq != null) {
                    this.addItemOnGround(sq, bag);
                }
            }
            if (Rand.Next(2) == 0) {
                bag = (InventoryContainer)InventoryItemFactory.CreateItem("Base.Bag_MoneyBag");
                ItemPickerJava.rollContainerItem(bag, null, ItemPickerJava.getItemPickerContainers().get(bag.getType()));
                sq = room.getFreeSquare();
                if (sq != null) {
                    this.addItemOnGround(sq, bag);
                }
            }
            if (Rand.Next(2) == 0) {
                bag = (InventoryContainer)InventoryItemFactory.CreateItem("Base.Bag_MoneyBag");
                ItemPickerJava.rollContainerItem(bag, null, ItemPickerJava.getItemPickerContainers().get(bag.getType()));
                sq = room.getFreeSquare();
                if (sq != null) {
                    this.addItemOnGround(sq, bag);
                }
            }
            this.addZombies(def, Rand.Next(2, 4), "Police", null, room);
        }
        if ((v = this.spawnCarOnNearestNav("Base.StepVan_LouisvilleSWAT", def)) == null) {
            return;
        }
        v.setAlarmed(false);
        IsoGridSquare sq = v.getSquare().getCell().getGridSquare(v.getSquare().x - 2, v.getSquare().y - 2, 0);
        ArrayList<IsoZombie> zeds = this.addZombiesOnSquare(Rand.Next(3, 6), "Police_SWAT", null, sq);
        if (zeds.isEmpty()) {
            return;
        }
        zeds.get(Rand.Next(zeds.size())).addItemToSpawnAtDeath(v.createVehicleKey());
    }

    @Override
    public boolean isValid(BuildingDef def, boolean force) {
        if (SpawnPoints.instance.isSpawnBuilding(def)) {
            this.debugLine = "Spawn houses are invalid";
            return false;
        }
        return def.getRoom("bank") != null;
    }

    public RBHeatBreakAfternoon() {
        this.name = "Bank Robbery";
        this.setChance(10);
        this.setUnique(true);
    }
}

