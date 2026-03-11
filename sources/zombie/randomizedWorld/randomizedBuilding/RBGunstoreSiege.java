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
import zombie.iso.IsoCell;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoObject;
import zombie.iso.IsoWorld;
import zombie.iso.SpawnPoints;
import zombie.iso.objects.IsoBarricade;
import zombie.iso.objects.IsoDoor;
import zombie.iso.objects.IsoWindow;
import zombie.iso.objects.interfaces.BarricadeAble;
import zombie.network.GameServer;
import zombie.randomizedWorld.randomizedBuilding.RandomizedBuildingBase;
import zombie.vehicles.BaseVehicle;

@UsedFromLua
public final class RBGunstoreSiege
extends RandomizedBuildingBase {
    @Override
    public void randomizeBuilding(BuildingDef def) {
        def.alarmed = false;
        IsoCell cell = IsoWorld.instance.currentCell;
        for (int x = def.x - 1; x < def.x2 + 1; ++x) {
            for (int y = def.y - 1; y < def.y2 + 1; ++y) {
                for (int z = -32; z < 31; ++z) {
                    IsoGridSquare sq = cell.getGridSquare(x, y, z);
                    if (sq == null) continue;
                    for (int o = 0; o < sq.getObjects().size(); ++o) {
                        int b;
                        int numPlanks;
                        boolean addOpposite;
                        IsoBarricade barricade;
                        IsoGridSquare outside;
                        IsoDoor isoDoor;
                        IsoObject obj = sq.getObjects().get(o);
                        if (obj instanceof IsoDoor && (isoDoor = (IsoDoor)obj).isBarricadeAllowed() && !SpawnPoints.instance.isSpawnBuilding(def)) {
                            IsoGridSquare isoGridSquare = outside = sq.getRoom() == null ? sq : isoDoor.getOppositeSquare();
                            if (outside != null && outside.getRoom() == null && (barricade = IsoBarricade.AddBarricadeToObject((BarricadeAble)isoDoor, addOpposite = outside == sq)) != null) {
                                if (Rand.Next(2) == 0) {
                                    numPlanks = Rand.Next(1, 4);
                                    for (b = 0; b < numPlanks; ++b) {
                                        barricade.addPlank(null, null);
                                    }
                                } else {
                                    barricade.addMetal(null, null);
                                }
                                if (GameServer.server) {
                                    barricade.transmitCompleteItemToClients();
                                }
                            }
                        }
                        if (!(obj instanceof IsoWindow)) continue;
                        IsoWindow isoWindow = (IsoWindow)obj;
                        IsoGridSquare isoGridSquare = outside = sq.getRoom() == null ? sq : isoWindow.getOppositeSquare();
                        if (isoWindow.isBarricadeAllowed() && z == 0 && outside != null && outside.getRoom() == null) {
                            addOpposite = outside != sq;
                            barricade = IsoBarricade.AddBarricadeToObject((BarricadeAble)isoWindow, addOpposite);
                            if (barricade == null) continue;
                            if (Rand.Next(2) == 0) {
                                numPlanks = Rand.Next(1, 4);
                                for (b = 0; b < numPlanks; ++b) {
                                    barricade.addPlank(null, null);
                                }
                            } else {
                                barricade.addMetal(null, null);
                            }
                            if (!GameServer.server) continue;
                            barricade.transmitCompleteItemToClients();
                            continue;
                        }
                        isoWindow.addSheet(null);
                        isoWindow.HasCurtains().ToggleDoor(null);
                    }
                }
            }
        }
        def.alarmed = false;
        int zombType = Rand.Next(3);
        String zombOutfit = "Hunter";
        switch (zombType) {
            case 1: {
                zombOutfit = "Survivalist03";
                break;
            }
            case 2: {
                zombOutfit = "Veteran";
            }
        }
        ArrayList<IsoZombie> zeds = this.addZombies(def, 1, zombOutfit, 0, null);
        InventoryContainer bag = (InventoryContainer)InventoryItemFactory.CreateItem("Base.Bag_WeaponBag");
        ItemPickerJava.rollContainerItem(bag, null, ItemPickerJava.getItemPickerContainers().get(bag.getType()));
        this.addItemOnGround(def.getFreeSquareInRoom(), bag);
        if (Rand.Next(2) == 0) {
            InventoryContainer bag2 = (InventoryContainer)InventoryItemFactory.CreateItem("Base.Bag_SurvivorBag");
            ItemPickerJava.rollContainerItem(bag2, null, ItemPickerJava.getItemPickerContainers().get(bag2.getType()));
            this.addItemOnGround(def.getFreeSquareInRoom(), bag2);
        }
        if (Rand.Next(2) == 0) {
            InventoryContainer bag3 = (InventoryContainer)InventoryItemFactory.CreateItem("Base.Bag_FoodCanned");
            ItemPickerJava.rollContainerItem(bag3, null, ItemPickerJava.getItemPickerContainers().get(bag3.getType()));
            this.addItemOnGround(def.getFreeSquareInRoom(), bag3);
        }
        if (Rand.Next(2) == 0) {
            InventoryContainer bag4 = (InventoryContainer)InventoryItemFactory.CreateItem("Base.Bag_MedicalBag");
            ItemPickerJava.rollContainerItem(bag4, null, ItemPickerJava.getItemPickerContainers().get(bag4.getType()));
            this.addItemOnGround(def.getFreeSquareInRoom(), bag4);
        }
        if (Rand.Next(2) == 0) {
            InventoryContainer bag5 = (InventoryContainer)InventoryItemFactory.CreateItem("Base.Bag_ProtectiveCaseBulkyAmmo");
            ItemPickerJava.rollContainerItem(bag5, null, ItemPickerJava.getItemPickerContainers().get(bag5.getType()));
            this.addItemOnGround(def.getFreeSquareInRoom(), bag5);
        }
        if (Rand.Next(2) == 0) {
            String vehicleType = "Base.OffRoad";
            int zomb = Rand.Next(9);
            if (zomb == 0) {
                vehicleType = "Base.OffRoad";
            } else if (zomb == 1) {
                vehicleType = "Base.PickUpVan";
            } else if (zomb == 2) {
                vehicleType = "Base.PickUpVanLightsFire";
            } else if (zomb == 3) {
                vehicleType = "Base.PickUpTruck";
            } else if (zomb == 4) {
                vehicleType = "Base.PickUpTruckLightsRanger";
            } else if (zomb == 5) {
                vehicleType = "Base.StepVan";
            } else if (zomb == 6) {
                vehicleType = "Base.SUV";
            } else if (zomb == 7) {
                vehicleType = "Base.Van";
            } else if (zomb == 8) {
                vehicleType = "Base.VanSeats";
            }
            BaseVehicle vehicle = this.spawnCarOnNearestNav(vehicleType, def, "Survivalist");
            if (vehicle != null) {
                vehicle.setAlarmed(false);
                if (!zeds.isEmpty()) {
                    zeds.get(Rand.Next(zeds.size())).addItemToSpawnAtDeath(vehicle.createVehicleKey());
                }
            }
        }
    }

    @Override
    public boolean isValid(BuildingDef def, boolean force) {
        if (SpawnPoints.instance.isSpawnBuilding(def)) {
            this.debugLine = "Spawn houses are invalid";
            return false;
        }
        if (def.getRooms().size() > 20) {
            return false;
        }
        return def.getRoom("gunstore") != null;
    }

    public RBGunstoreSiege() {
        this.name = "Barricaded Gunstore";
        this.setChance(10);
        this.setUnique(true);
    }
}

