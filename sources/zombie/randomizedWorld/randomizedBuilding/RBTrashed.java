/*
 * Decompiled with CFR 0.152.
 */
package zombie.randomizedWorld.randomizedBuilding;

import java.util.ArrayList;
import java.util.Objects;
import zombie.SandboxOptions;
import zombie.UsedFromLua;
import zombie.characters.IsoPlayer;
import zombie.core.properties.IsoPropertyType;
import zombie.core.random.Rand;
import zombie.core.stash.StashSystem;
import zombie.inventory.InventoryItem;
import zombie.inventory.InventoryItemFactory;
import zombie.inventory.ItemPickerJava;
import zombie.inventory.types.DrainableComboItem;
import zombie.inventory.types.Moveable;
import zombie.iso.BuildingDef;
import zombie.iso.IsoCell;
import zombie.iso.IsoDirections;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoWorld;
import zombie.iso.RoomDef;
import zombie.iso.SpawnPoints;
import zombie.iso.areas.IsoBuilding;
import zombie.iso.objects.IsoDoor;
import zombie.iso.objects.IsoWindow;
import zombie.network.GameClient;
import zombie.network.GameServer;
import zombie.randomizedWorld.randomizedBuilding.RandomizedBuildingBase;

@UsedFromLua
public final class RBTrashed
extends RandomizedBuildingBase {
    private static final IsoDirections[] DIRECTIONS = IsoDirections.values();

    public RBTrashed() {
        this.name = "Trashed Building";
        this.setChance(5);
        this.setAlwaysDo(true);
    }

    @Override
    public void randomizeBuilding(BuildingDef def) {
        this.trashHouse(def);
    }

    @Override
    public boolean isValid(BuildingDef def, boolean force) {
        this.debugLine = "";
        if (GameClient.client) {
            return false;
        }
        if (StashSystem.isStashBuilding(def)) {
            this.debugLine = "Stash buildings are invalid";
            return false;
        }
        if (SpawnPoints.instance.isSpawnBuilding(def)) {
            this.debugLine = "Spawn houses are invalid";
            return false;
        }
        if (def.isAllExplored() && !force) {
            return false;
        }
        if (!force) {
            IsoGridSquare sq = IsoCell.getInstance().getGridSquare(def.x, def.y, 0);
            int chance = this.getChance(sq);
            if (Rand.Next(100) > chance) {
                return false;
            }
            for (int i = 0; i < GameServer.Players.size(); ++i) {
                IsoPlayer player = GameServer.Players.get(i);
                if (player.getSquare() == null || player.getSquare().getBuilding() == null || player.getSquare().getBuilding().def != def) continue;
                return false;
            }
        }
        if (SandboxOptions.instance.getCurrentLootedChance() < 1 && !force) {
            return false;
        }
        int max = SandboxOptions.instance.maximumLootedBuildingRooms.getValue();
        if (def.getRooms().size() > max) {
            this.debugLine = "Building is too large, maximum " + max + " rooms";
            return false;
        }
        return true;
    }

    public IsoGridSquare getFloorSquare(ArrayList<IsoGridSquare> squares, IsoGridSquare square, RoomDef room, IsoBuilding building) {
        if (!Rand.NextBool(3)) {
            return square.getRandomAdjacentFreeSameRoom();
        }
        if (!Rand.NextBool(5)) {
            return Objects.requireNonNull(Objects.requireNonNull(building).getRandomRoom()).getRoomDef().getExtraFreeSquare();
        }
        return room.getExtraFreeSquare();
    }

    /*
     * Unable to fully structure code
     */
    public void trashHouse(BuildingDef def) {
        cell = IsoWorld.instance.currentCell;
        trashFactor = 40 + SandboxOptions.instance.getCurrentLootedChance();
        if (trashFactor > 90) {
            trashFactor = 90;
        }
        baseTrashFactor = trashFactor;
        graff = Rand.NextBool(2);
        removedBar = false;
        removedItems = new ArrayList<InventoryItem>();
        addOnGroundItems = new ArrayList<AddItemOnGround>();
        for (x = def.x - 1; x < def.x2 + 1; ++x) {
            for (y = def.y - 1; y < def.y2 + 1; ++y) {
                for (z = -32; z < 31; ++z) {
                    block77: {
                        block76: {
                            trashFactor = baseTrashFactor;
                            canTrash = false;
                            if (z < 0) {
                                depth = z * -1 + 1;
                                trashFactor /= depth;
                            }
                            sq = cell.getGridSquare(x, y, z);
                            if (graff && this.isValidGraffSquare(sq, true, false) && Rand.Next(500) <= trashFactor) {
                                this.graffSquare(sq, true);
                            }
                            if (graff && this.isValidGraffSquare(sq, false, false) && Rand.Next(500) <= trashFactor) {
                                this.graffSquare(sq, false);
                            }
                            if (sq == null || z != 0 || sq.getRoom() != null) break block76;
                            for (o = 0; o < sq.getObjects().size(); ++o) {
                                obj = sq.getObjects().get(o);
                                if (!(!(obj instanceof IsoDoor) || (door = (IsoDoor)obj).getProperties().has(IsoPropertyType.DOUBLE_DOOR) || door.getProperties().has(IsoPropertyType.GARAGE_DOOR) || door.isBarricaded() || door.IsOpen())) {
                                    if (z == 0 && door.isLocked()) {
                                        door.destroy();
                                    } else if (Rand.Next(200) <= trashFactor) {
                                        door.destroy();
                                    } else if (Rand.Next(10) <= trashFactor) {
                                        door.ToggleDoorSilent();
                                        if (door.isLocked()) {
                                            door.setLocked(false);
                                        }
                                    } else {
                                        door.setLocked(false);
                                    }
                                } else if (obj instanceof IsoDoor && (door = (IsoDoor)obj).getProperties().has(IsoPropertyType.GARAGE_DOOR) && door.isLocked() && !door.IsOpen()) {
                                    door.destroy();
                                }
                                if (sq.getZ() == 0 && obj instanceof IsoWindow && (window = (IsoWindow)obj).isLocked() && !window.IsOpen()) {
                                    window.smashWindow(true, false);
                                    window.addBrokenGlass(Rand.NextBool(2));
                                    continue;
                                }
                                if (!(obj instanceof IsoWindow)) continue;
                                window = (IsoWindow)obj;
                                if (Rand.Next(100) > trashFactor || window.IsOpen()) continue;
                                window.smashWindow(true, false);
                                window.addBrokenGlass(Rand.NextBool(2));
                            }
                            break block77;
                        }
                        if (sq == null || sq.getRoom() == null || sq.getRoom().getRoomDef().isKidsRoom()) break block77;
                        building = sq.getBuilding();
                        room = sq.getRoom().getRoomDef();
                        kidsRoom = room != null && room.isKidsRoom() != false;
                        canTrash = kidsRoom == false && RandomizedBuildingBase.is1x1AreaClear(sq) != false && sq.hasFloor() != false && sq.isOutside() == false;
                        squares = new ArrayList<IsoGridSquare>();
                        for (i = 0; i < RBTrashed.DIRECTIONS.length; ++i) {
                            testSq = sq.getAdjacentSquare(RBTrashed.DIRECTIONS[i]);
                            if (testSq == null || !testSq.isExtraFreeSquare() || testSq.getRoom() == null || testSq.getRoom() != sq.getRoom()) continue;
                            squares.add(testSq);
                        }
                        if (graff && this.isValidGraffSquare(sq, true, false) && Rand.Next(500) <= trashFactor) {
                            this.graffSquare(sq, true);
                        }
                        if (graff && this.isValidGraffSquare(sq, false, false) && Rand.Next(500) <= trashFactor) {
                            this.graffSquare(sq, false);
                        }
                        for (o = 0; o < sq.getObjects().size(); ++o) {
                            obj = sq.getObjects().get(o);
                            if (!(!(obj instanceof IsoDoor) || (door = (IsoDoor)obj).getProperties().has(IsoPropertyType.DOUBLE_DOOR) || door.getProperties().has(IsoPropertyType.GARAGE_DOOR) || door.isBarricaded() || door.IsOpen())) {
                                if (z == 0 && door.isLocked()) {
                                    door.destroy();
                                } else if (Rand.Next(200) <= trashFactor) {
                                    door.destroy();
                                } else if (Rand.Next(10) <= trashFactor) {
                                    door.ToggleDoorSilent();
                                    if (door.isLocked()) {
                                        door.setLocked(false);
                                    }
                                } else {
                                    door.setLocked(false);
                                }
                            } else if (obj instanceof IsoDoor && (door = (IsoDoor)obj).getProperties().has(IsoPropertyType.GARAGE_DOOR) && door.isLocked() && !door.IsOpen()) {
                                door.destroy();
                            }
                            if (!(obj instanceof IsoWindow)) ** GOTO lbl-1000
                            window = (IsoWindow)obj;
                            if (Rand.Next(100) <= trashFactor && !window.IsOpen()) {
                                window.smashWindow(true, false);
                                window.addBrokenGlass(Rand.NextBool(2));
                            } else if (sq.getZ() == 0 && obj instanceof IsoWindow && (window = (IsoWindow)obj).isLocked() && !window.IsOpen()) {
                                window.setIsLocked(false);
                            }
                            if (obj.getContainer() != null && obj.getContainer().getItems() != null && !obj.getSprite().getProperties().has(IsoPropertyType.IS_TRASH_CAN)) {
                                removedItems.clear();
                                addOnGroundItems.clear();
                                for (k = 0; k < obj.getContainer().getItems().size(); ++k) {
                                    item = obj.getContainer().getItems().get(k);
                                    if (Rand.Next(200) < trashFactor && !Objects.equals(item.getType(), "VHS_Home")) {
                                        if (item.getReplaceOnUseFullType() != null && obj.getSquare().getRoom() != null) {
                                            square = obj.getSquare().getRandomAdjacentFreeSameRoom();
                                            if (square == null || Rand.NextBool(3)) {
                                                square = obj.getSquare().getRoom().getRoomDef().getExtraFreeSquare();
                                            }
                                            if (square == null || Rand.NextBool(5)) {
                                                square = Objects.requireNonNull(Objects.requireNonNull(obj.getSquare().getBuilding()).getRandomRoom()).getRoomDef().getExtraFreeSquare();
                                            }
                                            if (square != null && !square.isOutside() && square.getRoom() != null && square.hasRoomDef()) {
                                                this.addItemOnGround(square, item.getReplaceOnUseFullType());
                                            }
                                        } else if (item instanceof DrainableComboItem && (drainableComboItem = (DrainableComboItem)item).getReplaceOnDepleteFullType() != null && obj.getSquare().getRoom() != null && (square = this.getFloorSquare(squares, sq, room, building)) != null && !square.isOutside() && square.getRoom() != null && square.hasRoomDef()) {
                                            this.addItemOnGround(square, drainableComboItem.getReplaceOnDepleteFullType());
                                        }
                                        removedItems.add(item);
                                        continue;
                                    }
                                    if (Rand.Next(100) >= trashFactor || item instanceof Moveable || (square = this.getFloorSquare(squares, sq, room, building)) == null || square.isOutside() || square.getRoom() == null || !square.hasRoomDef()) continue;
                                    ItemPickerJava.trashItemLooted(item);
                                    removedItems.add(item);
                                    addOnGroundItems.add(new AddItemOnGround(square, item));
                                }
                                if (!removedItems.isEmpty()) {
                                    if (GameServer.server) {
                                        GameServer.sendRemoveItemsFromContainer(obj.getContainer(), removedItems);
                                    }
                                    for (InventoryItem item : removedItems) {
                                        obj.getContainer().DoRemoveItem(item);
                                    }
                                }
                                if (!addOnGroundItems.isEmpty()) {
                                    for (AddItemOnGround aiog : addOnGroundItems) {
                                        this.addItemOnGround(aiog.square, aiog.item, false);
                                    }
                                }
                                ItemPickerJava.updateOverlaySprite(obj);
                                obj.getContainer().setExplored(true);
                            }
                            if (obj.getContainerByIndex(1) != null && obj.getContainerByIndex(1).getItems() != null) {
                                removedItems.clear();
                                items = obj.getContainerByIndex(1).getItems();
                                for (k = 0; k < items.size(); ++k) {
                                    if (Rand.Next(100) >= 80) continue;
                                    removedItems.add((InventoryItem)items.get(k));
                                }
                                if (!removedItems.isEmpty()) {
                                    if (GameServer.server) {
                                        GameServer.sendRemoveItemsFromContainer(obj.getContainerByIndex(1), removedItems);
                                    }
                                    for (InventoryItem item : removedItems) {
                                        obj.getContainerByIndex(1).DoRemoveItem(item);
                                    }
                                }
                                ItemPickerJava.updateOverlaySprite(obj);
                                obj.getContainerByIndex(1).setExplored(true);
                            }
                            if (removedBar || z != 0 || obj.getSprite() == null || obj.getSprite().getName() == null || !Objects.equals(obj.getSprite().getName(), "location_shop_mall_01_18") && !Objects.equals(obj.getSprite().getName(), "location_shop_mall_01_19")) continue;
                            sq.RemoveTileObject(obj);
                            sq.RecalcProperties();
                            sq.RecalcAllWithNeighbours(true);
                            if (sq.getWindow() != null) {
                                sq.getWindow().smashWindow(true, false);
                            }
                            removedBar = true;
                        }
                    }
                    if (sq == null) continue;
                    if (canTrash) {
                        if (Rand.Next(500) <= trashFactor) {
                            this.trashSquare(sq);
                        }
                    } else if (z == 0 && sq.isOutside() && RandomizedBuildingBase.is1x1AreaClear(sq) && Rand.Next(2000) <= trashFactor) {
                        this.trashSquare(sq);
                    }
                    if (z != 0 || !sq.isOutside() || !RandomizedBuildingBase.is2x2AreaClear(sq) || Rand.Next(10000) > trashFactor) continue;
                    sq.addCorpse();
                }
            }
        }
        for (i = 0; i < def.rooms.size(); ++i) {
            room = def.rooms.get(i);
            square = room.getExtraFreeSquare();
            chance = Math.min(baseTrashFactor, room.getIsoRoom().getSquares().size());
            chance = Math.max(chance, baseTrashFactor);
            if (room == null || square == null || Rand.Next(1000) > chance || !RandomizedBuildingBase.is2x2AreaClear(square)) continue;
            square.addCorpse();
        }
        room = def.getRandomRoom(4, true);
        freeSQ = RBTrashed.getRandomSquareForCorpse(room);
        if (room != null && freeSQ != null && def.getRoomsNumber() > 2 && def.getArea() >= 100 && Rand.NextBool(100)) {
            zombieType = "Bandit";
            if (!graff && Rand.NextBool(3)) {
                zombieType = "PrivateMilitia";
            } else if (!graff && Rand.NextBool(3)) {
                switch (Rand.Next(5)) {
                    case 1: {
                        v0 = "Survivalist02";
                        break;
                    }
                    case 2: {
                        v0 = "Survivalist03";
                        break;
                    }
                    case 3: {
                        v0 = "Survivalist04";
                        break;
                    }
                    case 4: {
                        v0 = "Survivalist05";
                        break;
                    }
                    default: {
                        v0 = "Survivalist";
                    }
                }
                zombieType = v0;
            }
            corpse = Rand.NextBool(2);
            zombies = this.addZombiesOnSquare(1, zombieType, null, freeSQ);
            if (zombies != null && zombies.get(0) != null) {
                keyType = "Base.Key1";
                houseKey = InventoryItemFactory.CreateItem("Base.Key1");
                if (houseKey != null) {
                    houseKey.setKeyId(def.getKeyId());
                    zombies.get(0).addItemToSpawnAtDeath((InventoryItem)houseKey);
                }
                if (corpse) {
                    freeSQ.createCorpse(zombies.get(0));
                }
            }
            for (i = 0; i < def.rooms.size(); ++i) {
                room = def.rooms.get(i);
                square = room.getExtraFreeSquare();
                if (square == null || !Rand.NextBool(100) || !RandomizedBuildingBase.is2x2AreaClear(square)) continue;
                if (Rand.NextBool(10)) {
                    corpse = Rand.NextBool(2);
                }
                zombies = this.addZombiesOnSquare(1, zombieType, null, square);
                if (!corpse) continue;
                square.createCorpse(zombies.get(0));
            }
        }
        def.setAllExplored(true);
        def.alarmed = false;
    }

    private record AddItemOnGround(IsoGridSquare square, InventoryItem item) {
    }
}

