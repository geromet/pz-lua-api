/*
 * Decompiled with CFR 0.152.
 */
package zombie.randomizedWorld.randomizedDeadSurvivor;

import java.util.ArrayList;
import java.util.Objects;
import zombie.SandboxOptions;
import zombie.UsedFromLua;
import zombie.characters.IsoPlayer;
import zombie.characters.animals.IsoAnimal;
import zombie.core.random.Rand;
import zombie.core.stash.StashSystem;
import zombie.inventory.InventoryItem;
import zombie.inventory.InventoryItemFactory;
import zombie.inventory.ItemPickerJava;
import zombie.inventory.types.Clothing;
import zombie.inventory.types.Food;
import zombie.iso.BuildingDef;
import zombie.iso.IsoCell;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoObject;
import zombie.iso.IsoWorld;
import zombie.iso.RoomDef;
import zombie.iso.SpawnPoints;
import zombie.iso.objects.IsoDeadBody;
import zombie.network.GameClient;
import zombie.network.GameServer;
import zombie.randomizedWorld.randomizedDeadSurvivor.RandomizedDeadSurvivorBase;

@UsedFromLua
public final class RDSRatInfested
extends RandomizedDeadSurvivorBase {
    public RDSRatInfested() {
        this.name = "Rat Infested";
        this.setChance(1);
        this.isRat = true;
    }

    @Override
    public void randomizeDeadSurvivor(BuildingDef def) {
        for (int i = 0; i < def.rooms.size(); ++i) {
            RDSRatInfested.ratRoom(def.rooms.get(i));
        }
        def.alarmed = false;
    }

    @Override
    public boolean isValid(BuildingDef def, boolean force) {
        this.debugLine = "";
        if (GameClient.client) {
            return false;
        }
        if (SpawnPoints.instance.isSpawnBuilding(def)) {
            this.debugLine = "Spawn houses are invalid";
            return false;
        }
        if (StashSystem.isStashBuilding(def)) {
            this.debugLine = "Stash buildings are invalid";
            return false;
        }
        if (def.isAllExplored() && !force) {
            return false;
        }
        if (!force) {
            for (int i = 0; i < GameServer.Players.size(); ++i) {
                IsoPlayer player = GameServer.Players.get(i);
                if (player.getSquare() == null || player.getSquare().getBuilding() == null || player.getSquare().getBuilding().def != def) continue;
                return false;
            }
        }
        if (def.getRooms().size() > 100) {
            this.debugLine = "Building is too large";
        }
        return true;
    }

    public static void ratRoom(RoomDef def) {
        int min;
        IsoCell cell = IsoWorld.instance.currentCell;
        int trashFactor = SandboxOptions.instance.getCurrentRatIndex();
        if (trashFactor < 1) {
            return;
        }
        if (trashFactor > 90) {
            trashFactor = 90;
        }
        for (int x = def.x - 1; x < def.x2 + 1; ++x) {
            for (int y = def.y - 1; y < def.y2 + 1; ++y) {
                for (int z = -32; z < 31; ++z) {
                    IsoGridSquare sq = cell.getGridSquare(x, y, z);
                    if (sq == null) continue;
                    for (int o = 0; o < sq.getObjects().size(); ++o) {
                        IsoObject obj = sq.getObjects().get(o);
                        if (obj.getContainer() != null && obj.getContainer().getItems() != null && !Objects.equals(obj.getContainer().getType(), "fridge") && !Objects.equals(obj.getContainer().getType(), "freezer")) {
                            for (int k = 0; k < obj.getContainer().getItems().size(); ++k) {
                                boolean ratted;
                                if (Rand.Next(100) >= trashFactor || obj.getSquare().getRoom() == null) continue;
                                IsoGridSquare square = obj.getSquare().getRoom().getRoomDef().getFreeSquare();
                                InventoryItem item = obj.getContainer().getItems().get(k);
                                boolean bl = ratted = (item instanceof Food || item instanceof Clothing) && !Objects.equals(item.getType(), "DeadRat") && !Objects.equals(item.getType(), "Dung_Rat");
                                if (square == null || !ratted || square.isOutside() || square.getRoom() == null || !square.hasRoomDef()) continue;
                                ItemPickerJava.trashItemRats(item);
                                if (Rand.NextBool(2)) {
                                    obj.getContainer().getItems().remove(k);
                                    --k;
                                    RDSRatInfested.addItemOnGroundStatic(square, item);
                                }
                                if (!Rand.NextBool(2)) continue;
                                obj.getContainer().addItem((InventoryItem)InventoryItemFactory.CreateItem("Base.Dung_Rat"));
                            }
                            ItemPickerJava.updateOverlaySprite(obj);
                            obj.getContainer().setExplored(true);
                        }
                        if (obj.getContainer() != null && Rand.Next(100) < trashFactor) {
                            obj.getContainer().addItem((InventoryItem)InventoryItemFactory.CreateItem("Base.Dung_Rat"));
                        }
                        if (obj.getContainer() == null || Rand.Next(200) >= trashFactor) continue;
                        Object rat = InventoryItemFactory.CreateItem("Base.DeadRat");
                        ((InventoryItem)rat).setAutoAge();
                        obj.getContainer().addItem((InventoryItem)rat);
                    }
                }
            }
        }
        int max = def.getIsoRoom().getSquares().size() / 3;
        if (max > trashFactor) {
            max = trashFactor;
        }
        if (max > 10) {
            max = 10;
        }
        if ((min = max / 2) < 1) {
            min = 1;
        }
        if (max < 1) {
            max = 1;
        }
        if (Rand.Next(100) < trashFactor) {
            ArrayList<IsoGridSquare> usedSquares = new ArrayList<IsoGridSquare>();
            int nbrOfRats = Rand.Next(min, max);
            for (int i = 0; i < nbrOfRats; ++i) {
                IsoGridSquare square = def.getFreeUnoccupiedSquare();
                String breed = "grey";
                if (def.getBuilding() != null && def.getBuilding().getRoom("laboratory") != null && !Rand.NextBool(3)) {
                    breed = "white";
                }
                if (square == null || !square.isFree(true) || usedSquares.contains(square)) continue;
                IsoAnimal animal = Rand.NextBool(2) ? new IsoAnimal(IsoWorld.instance.getCell(), square.getX(), square.getY(), square.getZ(), "rat", breed) : new IsoAnimal(IsoWorld.instance.getCell(), square.getX(), square.getY(), square.getZ(), "ratfemale", breed);
                animal.addToWorld();
                animal.randomizeAge();
                if (Rand.NextBool(3)) {
                    animal.setStateEventDelayTimer(0.0f);
                    continue;
                }
                usedSquares.add(square);
            }
        }
        int nbrOfPoops = Rand.Next(min, max);
        for (int i = 0; i < nbrOfPoops; ++i) {
            IsoGridSquare square = def.getFreeSquare();
            if (square == null || square.isOutside() || square.getRoom() == null || !square.hasRoomDef()) continue;
            RDSRatInfested.addItemOnGroundStatic(square, "Base.Dung_Rat");
        }
        if (Rand.Next(200) < trashFactor) {
            IsoGridSquare square = def.getFreeUnoccupiedSquare();
            String breed = "grey";
            if (def.getBuilding() != null && def.getBuilding().getRoom("laboratory") != null && !Rand.NextBool(3)) {
                breed = "white";
            }
            if (square != null && square.isFree(true)) {
                IsoAnimal animal = Rand.NextBool(2) ? new IsoAnimal(IsoWorld.instance.getCell(), square.getX(), square.getY(), square.getZ(), "rat", breed) : new IsoAnimal(IsoWorld.instance.getCell(), square.getX(), square.getY(), square.getZ(), "ratfemale", breed);
                animal.randomizeAge();
                IsoDeadBody deadAnimal = new IsoDeadBody(animal, false);
                deadAnimal.addToWorld();
            }
        }
    }
}

