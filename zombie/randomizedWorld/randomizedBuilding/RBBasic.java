/*
 * Decompiled with CFR 0.152.
 */
package zombie.randomizedWorld.randomizedBuilding;

import gnu.trove.map.hash.TIntObjectHashMap;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import zombie.SandboxOptions;
import zombie.UsedFromLua;
import zombie.characters.IsoZombie;
import zombie.core.Core;
import zombie.core.properties.IsoPropertyType;
import zombie.core.random.Rand;
import zombie.debug.DebugLog;
import zombie.entity.GameEntity;
import zombie.entity.components.fluids.FluidType;
import zombie.inventory.InventoryItem;
import zombie.inventory.InventoryItemFactory;
import zombie.inventory.ItemContainer;
import zombie.inventory.ItemPickerJava;
import zombie.inventory.types.InventoryContainer;
import zombie.iso.BuildingDef;
import zombie.iso.IsoCell;
import zombie.iso.IsoDirections;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoObject;
import zombie.iso.IsoWorld;
import zombie.iso.RoomDef;
import zombie.iso.objects.IsoCurtain;
import zombie.iso.objects.IsoDeadBody;
import zombie.iso.objects.IsoDoor;
import zombie.iso.objects.IsoRadio;
import zombie.iso.objects.IsoStove;
import zombie.iso.objects.IsoTelevision;
import zombie.iso.objects.IsoThumpable;
import zombie.iso.objects.IsoWindow;
import zombie.iso.sprite.IsoSprite;
import zombie.iso.sprite.IsoSpriteGrid;
import zombie.network.GameClient;
import zombie.network.GameServer;
import zombie.popman.ObjectPool;
import zombie.randomizedWorld.randomizedBuilding.RandomizedBuildingBase;
import zombie.randomizedWorld.randomizedBuilding.TableStories.RBTableStoryBase;
import zombie.randomizedWorld.randomizedDeadSurvivor.RDSBandPractice;
import zombie.randomizedWorld.randomizedDeadSurvivor.RDSBanditRaid;
import zombie.randomizedWorld.randomizedDeadSurvivor.RDSBathroomZed;
import zombie.randomizedWorld.randomizedDeadSurvivor.RDSBedroomZed;
import zombie.randomizedWorld.randomizedDeadSurvivor.RDSBleach;
import zombie.randomizedWorld.randomizedDeadSurvivor.RDSCorpsePsycho;
import zombie.randomizedWorld.randomizedDeadSurvivor.RDSDeadDrunk;
import zombie.randomizedWorld.randomizedDeadSurvivor.RDSDevouredByRats;
import zombie.randomizedWorld.randomizedDeadSurvivor.RDSFootballNight;
import zombie.randomizedWorld.randomizedDeadSurvivor.RDSGrouchos;
import zombie.randomizedWorld.randomizedDeadSurvivor.RDSGunmanInBathroom;
import zombie.randomizedWorld.randomizedDeadSurvivor.RDSGunslinger;
import zombie.randomizedWorld.randomizedDeadSurvivor.RDSHenDo;
import zombie.randomizedWorld.randomizedDeadSurvivor.RDSHockeyPsycho;
import zombie.randomizedWorld.randomizedDeadSurvivor.RDSHouseParty;
import zombie.randomizedWorld.randomizedDeadSurvivor.RDSPokerNight;
import zombie.randomizedWorld.randomizedDeadSurvivor.RDSPoliceAtHouse;
import zombie.randomizedWorld.randomizedDeadSurvivor.RDSPrisonEscape;
import zombie.randomizedWorld.randomizedDeadSurvivor.RDSPrisonEscapeWithPolice;
import zombie.randomizedWorld.randomizedDeadSurvivor.RDSRPGNight;
import zombie.randomizedWorld.randomizedDeadSurvivor.RDSRatInfested;
import zombie.randomizedWorld.randomizedDeadSurvivor.RDSRatKing;
import zombie.randomizedWorld.randomizedDeadSurvivor.RDSResourceGarage;
import zombie.randomizedWorld.randomizedDeadSurvivor.RDSSkeletonPsycho;
import zombie.randomizedWorld.randomizedDeadSurvivor.RDSSpecificProfession;
import zombie.randomizedWorld.randomizedDeadSurvivor.RDSStagDo;
import zombie.randomizedWorld.randomizedDeadSurvivor.RDSStudentNight;
import zombie.randomizedWorld.randomizedDeadSurvivor.RDSSuicidePact;
import zombie.randomizedWorld.randomizedDeadSurvivor.RDSTinFoilHat;
import zombie.randomizedWorld.randomizedDeadSurvivor.RDSZombieLockedBathroom;
import zombie.randomizedWorld.randomizedDeadSurvivor.RDSZombiesEating;
import zombie.randomizedWorld.randomizedDeadSurvivor.RandomizedDeadSurvivorBase;
import zombie.scripting.objects.ItemKey;
import zombie.vehicles.BaseVehicle;
import zombie.vehicles.VehiclePart;

@UsedFromLua
public final class RBBasic
extends RandomizedBuildingBase {
    private final ArrayList<String> specificProfessionDistribution = new ArrayList();
    private final Map<String, String> specificProfessionRoomDistribution = new HashMap<String, String>();
    private final Map<String, String> plankStash = new HashMap<String, String>();
    private final ArrayList<RandomizedDeadSurvivorBase> deadSurvivorsStory = new ArrayList();
    private int totalChanceRds;
    private static final HashMap<RandomizedDeadSurvivorBase, Integer> rdsMap = new HashMap();
    private static final ArrayList<String> uniqueRDSSpawned = new ArrayList();
    private ArrayList<IsoObject> tablesDone = new ArrayList();
    private boolean doneTable;
    private static final ObjectPool<TIntObjectHashMap<String>> s_clutterCopyPool = new ObjectPool<TIntObjectHashMap>(TIntObjectHashMap::new);
    private static final int TABLE_STORY_CHANCE = 10;

    @Override
    public void randomizeBuilding(BuildingDef def) {
        RoomDef roomDef;
        this.tablesDone = new ArrayList();
        IsoCell cell = IsoWorld.instance.currentCell;
        boolean doPlankStash = Rand.NextBool(100);
        if ((this.getRoom(def, "kitchen") == null || this.getRoom(def, "hall") == null) && Rand.NextBool(20)) {
            roomDef = null;
            IsoGridSquare sq = null;
            if (Rand.NextBool(2)) {
                roomDef = this.getRoom(def, "hall");
            }
            if (roomDef == null) {
                roomDef = this.getRoom(def, "kitchen");
            }
            if (roomDef != null) {
                sq = roomDef.getExtraFreeSquare();
            }
            if (sq != null) {
                this.addItemOnGround(sq, "Base.WaterDish");
                if (Rand.NextBool(3) && (sq = roomDef.getExtraFreeSquare()) != null) {
                    String item = "Base.CatToy";
                    int rand = Rand.Next(6);
                    switch (rand) {
                        case 0: {
                            item = "Base.CatFoodBag";
                            break;
                        }
                        case 1: {
                            item = "Base.CatTreats";
                            break;
                        }
                        case 2: {
                            item = "Base.DogChew";
                            break;
                        }
                        case 3: {
                            item = "Base.DogFoodBag";
                            break;
                        }
                        case 4: {
                            item = "Base.Leash";
                            break;
                        }
                        case 5: {
                            item = "Base.DogTag_Pet";
                        }
                    }
                    this.addItemOnGround(sq, item);
                }
            }
        }
        roomDef = null;
        boolean kidsRoom = false;
        boolean didFireplaceStuff = false;
        for (int x = def.x - 1; x < def.x2 + 1; ++x) {
            for (int y = def.y - 1; y < def.y2 + 1; ++y) {
                for (int z = -32; z < 31; ++z) {
                    IsoGridSquare sq = cell.getGridSquare(x, y, z);
                    if (sq == null) continue;
                    if (doPlankStash && sq.getFloor() != null && this.plankStash.containsKey(sq.getFloor().getSprite().getName())) {
                        IsoThumpable cont = new IsoThumpable(sq.getCell(), sq, this.plankStash.get(sq.getFloor().getSprite().getName()), false, null);
                        cont.setIsThumpable(false);
                        cont.container = new ItemContainer("plankstash", sq, cont);
                        sq.AddSpecialObject(cont);
                        sq.RecalcAllWithNeighbours(true);
                        doPlankStash = false;
                    }
                    if (!didFireplaceStuff && sq.hasFireplace()) {
                        if (Rand.NextBool(4)) {
                            this.addItemOnGround(sq, "Base.Tongs");
                            didFireplaceStuff = true;
                        }
                        if (Rand.NextBool(4)) {
                            this.addItemOnGround(sq, "Base.Bellows");
                            didFireplaceStuff = true;
                        }
                        if (Rand.NextBool(4)) {
                            this.addItemOnGround(sq, "Base.FireplacePoker");
                            didFireplaceStuff = true;
                        }
                    }
                    for (int o = 0; o < sq.getObjects().size(); ++o) {
                        String foodString;
                        IsoDoor door;
                        IsoObject obj = sq.getObjects().get(o);
                        if (Rand.Next(100) <= 65 && obj instanceof IsoDoor && !(door = (IsoDoor)obj).getProperties().has(IsoPropertyType.DOUBLE_DOOR) && !door.getProperties().has(IsoPropertyType.GARAGE_DOOR) && !door.isExterior()) {
                            door.ToggleDoorSilent();
                            door.sync(1);
                        }
                        if (obj instanceof IsoWindow) {
                            IsoCurtain curtain;
                            IsoWindow window = (IsoWindow)obj;
                            if (Rand.NextBool(80)) {
                                def.alarmed = false;
                                window.ToggleWindow(null);
                            }
                            if ((curtain = window.HasCurtains()) != null && Rand.NextBool(15)) {
                                curtain.ToggleDoorSilent();
                            }
                        }
                        if (SandboxOptions.instance.survivorHouseChance.getValue() == 1) continue;
                        if (Rand.Next(100) < 15 && obj.getContainer() != null && obj.getContainer().getType().equals("stove") && (foodString = this.getOvenFoodClutterItem()) != null) {
                            InventoryItem food = obj.getContainer().AddItem(foodString);
                            food.setCooked(true);
                            food.setAutoAge();
                        }
                        if (this.tablesDone.contains(obj) || !obj.getProperties().isTable() || obj.getContainer() != null || this.doneTable || !obj.hasAdjacentCanStandSquare()) continue;
                        this.checkForTableSpawn(def, obj);
                    }
                    if (SandboxOptions.instance.survivorHouseChance.getValue() == 1 || sq.getRoom() == null) continue;
                    if (roomDef == null && sq.getRoom().getRoomDef() != null) {
                        roomDef = sq.getRoom().getRoomDef();
                    } else if (sq.getRoom().getRoomDef() != null && roomDef != sq.getRoom().getRoomDef()) {
                        roomDef = sq.getRoom().getRoomDef();
                        kidsRoom = "kidsbedroom".equals(sq.getRoom().getName()) ? true : ("bedroom".equals(sq.getRoom().getName()) ? roomDef.isKidsRoom() : false);
                    }
                    if (kidsRoom) {
                        this.doKidsBedroomStuff(sq);
                        continue;
                    }
                    if ("kitchen".equals(sq.getRoom().getName())) {
                        this.doKitchenStuff(sq);
                        continue;
                    }
                    if ("barcountertwiggy".equals(sq.getRoom().getName())) {
                        RBBasic.doTwiggyStuff(sq);
                        continue;
                    }
                    if ("bathroom".equals(sq.getRoom().getName())) {
                        this.doBathroomStuff(sq);
                        continue;
                    }
                    if ("bedroom".equals(sq.getRoom().getName())) {
                        this.doBedroomStuff(sq);
                        continue;
                    }
                    if ("cafe".equals(sq.getRoom().getName())) {
                        RBBasic.doCafeStuff(sq);
                        continue;
                    }
                    if ("gigamart".equals(sq.getRoom().getName())) {
                        RBBasic.doGroceryStuff(sq);
                        continue;
                    }
                    if ("grocery".equals(sq.getRoom().getName())) {
                        RBBasic.doGroceryStuff(sq);
                        continue;
                    }
                    if ("livingroom".equals(sq.getRoom().getName())) {
                        this.doLivingRoomStuff(sq);
                        continue;
                    }
                    if ("office".equals(sq.getRoom().getName())) {
                        RBBasic.doOfficeStuff(sq);
                        continue;
                    }
                    if ("jackiejayestudio".equals(sq.getRoom().getName())) {
                        RBBasic.doOfficeStuff(sq);
                        continue;
                    }
                    if ("judgematthassset".equals(sq.getRoom().getName())) {
                        RBBasic.doJudgeStuff(sq);
                        continue;
                    }
                    if ("mayorwestpointoffice".equals(sq.getRoom().getName())) {
                        RBBasic.doOfficeStuff(sq);
                        continue;
                    }
                    if ("nolansoffice".equals(sq.getRoom().getName())) {
                        RBBasic.doNolansOfficeStuff(sq);
                        continue;
                    }
                    if ("woodcraftset".equals(sq.getRoom().getName())) {
                        RBBasic.doWoodcraftStuff(sq);
                        continue;
                    }
                    if ("laundry".equals(sq.getRoom().getName())) {
                        this.doLaundryStuff(sq);
                        continue;
                    }
                    if ("hall".equals(sq.getRoom().getName())) {
                        RBBasic.doGeneralRoom(sq, this.getHallClutter());
                        continue;
                    }
                    if (!"garageStorage".equals(sq.getRoom().getName()) && !"garage".equals(sq.getRoom().getName())) continue;
                    RBBasic.doGeneralRoom(sq, this.getGarageStorageClutter());
                }
            }
        }
        if (Rand.Next(100) < 25) {
            this.addRandomDeadSurvivorStory(def);
            def.setAllExplored(true);
            def.alarmed = false;
        }
        this.doneTable = false;
    }

    public void forceVehicleDistribution(BaseVehicle vehicle, String distribution) {
        ItemPickerJava.VehicleDistribution distro = ItemPickerJava.VehicleDistributions.get(distribution);
        ItemPickerJava.ItemPickerRoom distro2 = distro.normal;
        for (int i = 0; i < vehicle.getPartCount(); ++i) {
            VehiclePart part = vehicle.getPartByIndex(i);
            if (part.getItemContainer() == null) continue;
            if (GameServer.server && GameServer.softReset) {
                part.getItemContainer().setExplored(false);
            }
            if (part.getItemContainer().explored) continue;
            part.getItemContainer().clear();
            this.randomizeContainer(part, distro2);
            part.getItemContainer().setExplored(true);
        }
    }

    private void randomizeContainer(VehiclePart part, ItemPickerJava.ItemPickerRoom distro2) {
        if (GameClient.client) {
            return;
        }
        if (distro2 == null) {
            return;
        }
        ItemPickerJava.fillContainerType(distro2, part.getItemContainer(), "", null);
    }

    private void doLivingRoomStuff(IsoGridSquare sq) {
        this.doLivingRoomStuff(sq, this.getLivingroomClutter());
    }

    private void doLivingRoomStuff(IsoGridSquare sq, ArrayList<String> clutterArray) {
        String item;
        if (!sq.hasAdjacentCanStandSquare()) {
            return;
        }
        if (clutterArray == null) {
            clutterArray = this.getLivingroomClutter();
        }
        IsoObject objToAdd = null;
        boolean tv = false;
        for (int o = 0; o < sq.getObjects().size(); ++o) {
            boolean table;
            IsoObject obj = sq.getObjects().get(o);
            if (obj instanceof IsoRadio || obj instanceof IsoTelevision) {
                tv = true;
                break;
            }
            boolean bl = table = obj.getProperties().get("BedType") == null && obj.getSurfaceOffsetNoTable() > 0.0f && obj.getSurfaceOffsetNoTable() < 30.0f;
            if (!table || !Rand.NextBool(5)) continue;
            objToAdd = obj;
        }
        if (!tv && objToAdd != null && (item = RBBasic.getClutterItem(clutterArray)) != null) {
            RBBasic.trySpawnStoryItem(item, objToAdd, true);
        }
    }

    private void doBedroomStuff(IsoGridSquare sq) {
        for (int o = 0; o < sq.getObjects().size(); ++o) {
            String item;
            IsoObject obj = sq.getObjects().get(o);
            if (obj.getSprite() == null || obj.getSprite().getName() == null) continue;
            boolean bed = obj.getSprite().getName().contains("bedding") && obj.getProperties().get("BedType") != null;
            boolean sidetable = obj.getContainer() != null && "sidetable".equals(obj.getContainer().getType());
            boolean pillow = false;
            IsoDirections facing = this.getFacing(obj.getSprite());
            IsoSpriteGrid grid = obj.getSprite().getSpriteGrid();
            if (bed && facing != null && grid != null) {
                int gridPosX = grid.getSpriteGridPosX(obj.getSprite());
                int gridPosY = grid.getSpriteGridPosY(obj.getSprite());
                if (facing == IsoDirections.E && gridPosX == 0 && (gridPosY == 0 || gridPosY == 1)) {
                    pillow = true;
                }
                if (facing == IsoDirections.W && gridPosX == 1 && (gridPosY == 0 || gridPosY == 1)) {
                    pillow = true;
                }
                if (facing == IsoDirections.N && (gridPosX == 0 || gridPosX == 1) && gridPosY == 1) {
                    pillow = true;
                }
                if (facing == IsoDirections.S && (gridPosX == 0 || gridPosX == 1) && gridPosY == 0) {
                    pillow = true;
                }
            }
            int roll = 7;
            if (pillow) {
                roll = 3;
            }
            if (bed && Rand.NextBool(roll)) {
                if (pillow) {
                    String item2 = "Base.Pillow";
                    if (Rand.NextBool(100)) {
                        item2 = this.getPillowClutterItem();
                    }
                    if (facing == IsoDirections.E) {
                        this.addWorldItem(item2, sq, 0.42f, Rand.Next(0.34f, 0.74f), obj.getSurfaceOffsetNoTable() / 96.0f, Rand.Next(85, 95));
                        return;
                    }
                    if (facing == IsoDirections.W) {
                        this.addWorldItem(item2, sq, 0.64f, Rand.Next(0.34f, 0.74f), obj.getSurfaceOffsetNoTable() / 96.0f, Rand.Next(265, 275));
                        return;
                    }
                    if (facing == IsoDirections.N) {
                        this.addWorldItem(item2, sq, Rand.Next(0.44f, 0.64f), 0.67f, obj.getSurfaceOffsetNoTable() / 96.0f, Rand.Next(355, 365));
                        return;
                    }
                    if (facing != IsoDirections.S) continue;
                    this.addWorldItem(item2, sq, Rand.Next(0.44f, 0.64f), 0.42f, obj.getSurfaceOffsetNoTable() / 96.0f, Rand.Next(175, 185));
                    return;
                }
                String item3 = this.getBedClutterItem();
                RBBasic.trySpawnStoryItem(item3, obj, true);
                return;
            }
            if (!sidetable || !Rand.NextBool(7) || (item = this.getSidetableClutterItem()) == null || facing == null) continue;
            RBBasic.trySpawnStoryItem(item, obj, true);
            return;
        }
    }

    private void doKidsBedroomStuff(IsoGridSquare sq) {
        for (int o = 0; o < sq.getObjects().size(); ++o) {
            String item;
            IsoObject obj = sq.getObjects().get(o);
            if (obj.getSprite() == null || obj.getSprite().getName() == null) continue;
            boolean bed = obj.getSprite().getName().contains("bedding") && obj.getProperties().get("BedType") != null;
            boolean sidetable = obj.getContainer() != null && "sidetable".equals(obj.getContainer().getType());
            boolean pillow = false;
            IsoDirections facing = this.getFacing(obj.getSprite());
            IsoSpriteGrid grid = obj.getSprite().getSpriteGrid();
            if (bed && facing != null && grid != null) {
                int gridPosX = grid.getSpriteGridPosX(obj.getSprite());
                int gridPosY = grid.getSpriteGridPosY(obj.getSprite());
                if (facing == IsoDirections.E && gridPosX == 0 && (gridPosY == 0 || gridPosY == 1)) {
                    pillow = true;
                }
                if (facing == IsoDirections.W && gridPosX == 1 && (gridPosY == 0 || gridPosY == 1)) {
                    pillow = true;
                }
                if (facing == IsoDirections.N && (gridPosX == 0 || gridPosX == 1) && gridPosY == 1) {
                    pillow = true;
                }
                if (facing == IsoDirections.S && (gridPosX == 0 || gridPosX == 1) && gridPosY == 0) {
                    pillow = true;
                }
            }
            int roll = 7;
            if (pillow) {
                roll = 3;
            }
            if (bed && Rand.NextBool(roll)) {
                if (pillow) {
                    String item2 = "Base.Pillow";
                    if (Rand.NextBool(20)) {
                        item2 = this.getKidClutterItem();
                    }
                    if (facing == IsoDirections.E) {
                        this.addWorldItem(item2, sq, 0.42f, Rand.Next(0.34f, 0.74f), obj.getSurfaceOffsetNoTable() / 96.0f, Rand.Next(85, 95));
                        return;
                    }
                    if (facing == IsoDirections.W) {
                        this.addWorldItem(item2, sq, 0.64f, Rand.Next(0.34f, 0.74f), obj.getSurfaceOffsetNoTable() / 96.0f, Rand.Next(265, 275));
                        return;
                    }
                    if (facing == IsoDirections.N) {
                        this.addWorldItem(item2, sq, Rand.Next(0.44f, 0.64f), 0.67f, obj.getSurfaceOffsetNoTable() / 96.0f, Rand.Next(355, 365));
                        return;
                    }
                    if (facing != IsoDirections.S) continue;
                    this.addWorldItem(item2, sq, Rand.Next(0.44f, 0.64f), 0.42f, obj.getSurfaceOffsetNoTable() / 96.0f, Rand.Next(175, 185));
                    return;
                }
                String item3 = this.getBedClutterItem();
                if (Rand.NextBool(3)) {
                    item3 = this.getKidClutterItem();
                }
                RBBasic.trySpawnStoryItem(item3, obj, true);
                return;
            }
            if (!sidetable || !Rand.NextBool(7) || (item = this.getKidClutterItem()) == null || facing == null) continue;
            RBBasic.trySpawnStoryItem(item, obj, true);
            return;
        }
    }

    private void doKitchenStuff(IsoGridSquare sq) {
        TIntObjectHashMap<String> counterClutter = this.getClutterCopy(this.getKitchenCounterClutter(), s_clutterCopyPool.alloc());
        TIntObjectHashMap<String> sinkClutter = this.getClutterCopy(this.getKitchenSinkClutter(), s_clutterCopyPool.alloc());
        TIntObjectHashMap<String> stoveClutter = this.getClutterCopy(this.getKitchenStoveClutter(), s_clutterCopyPool.alloc());
        if (Rand.NextBool(100)) {
            sinkClutter.put(sinkClutter.size() + 1, "Base.PotScrubberFrog");
        }
        this.doKitchenStuff(sq, counterClutter, sinkClutter, stoveClutter);
        s_clutterCopyPool.release(counterClutter);
        s_clutterCopyPool.release(sinkClutter);
        s_clutterCopyPool.release(stoveClutter);
    }

    private void doKitchenStuff(IsoGridSquare sq, TIntObjectHashMap<String> counterClutter, TIntObjectHashMap<String> sinkClutter, TIntObjectHashMap<String> stoveClutter) {
        boolean bReleaseStove;
        boolean bReleaseCounter = counterClutter == null;
        boolean bReleaseSink = sinkClutter == null;
        boolean bl = bReleaseStove = stoveClutter == null;
        if (counterClutter == null) {
            counterClutter = this.getClutterCopy(this.getKitchenCounterClutter(), s_clutterCopyPool.alloc());
        }
        if (sinkClutter == null) {
            sinkClutter = this.getClutterCopy(this.getKitchenSinkClutter(), s_clutterCopyPool.alloc());
        }
        if (stoveClutter == null) {
            stoveClutter = this.getClutterCopy(this.getKitchenStoveClutter(), s_clutterCopyPool.alloc());
        }
        boolean kitchenSink = false;
        boolean counter = false;
        boolean microwave = false;
        for (int o = 0; o < sq.getObjects().size(); ++o) {
            IsoDirections facing;
            IsoObject obj = sq.getObjects().get(o);
            if (obj.getSprite() == null || obj.getSprite().getName() == null) continue;
            if (!microwave && obj.getContainer() != null && "microwave".equals(obj.getContainer().getType()) && Rand.NextBool(4)) {
                ItemContainer container = obj.getContainer();
                if (!container.isEmpty()) {
                    container.removeAllItems();
                }
                Object mug = InventoryItemFactory.CreateItem(ItemKey.Normal.MUGL);
                ((GameEntity)mug).getFluidContainer().addFluid(FluidType.Coffee, ((GameEntity)mug).getFluidContainer().getCapacity());
                container.addItem((InventoryItem)mug);
                microwave = true;
            }
            if (!kitchenSink && obj.getSprite().getName().contains("sink") && Rand.NextBool(4)) {
                facing = this.getFacing(obj.getSprite());
                if (facing == null) continue;
                if (Rand.NextBool(100)) {
                    this.generateSinkClutter(facing, obj, sq, sinkClutter);
                }
                kitchenSink = true;
                continue;
            }
            if (!counter && obj.getContainer() != null && "counter".equals(obj.getContainer().getType()) && Rand.NextBool(6)) {
                IsoDirections facing2;
                boolean doIt = true;
                for (int o2 = 0; o2 < sq.getObjects().size(); ++o2) {
                    IsoObject obj2 = sq.getObjects().get(o2);
                    if ((obj2.getSprite() == null || obj2.getSprite().getName() == null || !obj2.getSprite().getName().contains("sink")) && !(obj2 instanceof IsoStove) && !(obj2 instanceof IsoRadio)) continue;
                    doIt = false;
                    break;
                }
                if (!doIt || (facing2 = this.getFacing(obj.getSprite())) == null) continue;
                this.generateCounterClutter(facing2, obj, sq, counterClutter);
                counter = true;
                continue;
            }
            if (!(obj instanceof IsoStove) || obj.getContainer() == null || !"stove".equals(obj.getContainer().getType()) || !Rand.NextBool(4) || (facing = this.getFacing(obj.getSprite())) == null) continue;
            this.generateKitchenStoveClutter(facing, obj, sq, stoveClutter);
        }
        if (bReleaseCounter) {
            s_clutterCopyPool.release(counterClutter);
        }
        if (bReleaseSink) {
            s_clutterCopyPool.release(sinkClutter);
        }
        if (bReleaseStove) {
            s_clutterCopyPool.release(stoveClutter);
        }
    }

    private void doBathroomStuff(IsoGridSquare sq) {
        TIntObjectHashMap<String> clutterCopy = this.getClutterCopy(this.getBathroomSinkClutter(), s_clutterCopyPool.alloc());
        this.doBathroomStuff(sq, clutterCopy);
        s_clutterCopyPool.release(clutterCopy);
    }

    private void doBathroomStuff(IsoGridSquare sq, TIntObjectHashMap<String> sinkClutter) {
        boolean bReleaseClutter;
        boolean bl = bReleaseClutter = sinkClutter == null;
        if (sinkClutter == null) {
            sinkClutter = this.getClutterCopy(this.getBathroomSinkClutter(), s_clutterCopyPool.alloc());
        }
        boolean sink2 = false;
        boolean counter = false;
        boolean toilet = false;
        for (int o = 0; o < sq.getObjects().size(); ++o) {
            IsoDirections facing;
            IsoDirections facing2;
            IsoObject obj = sq.getObjects().get(o);
            if (obj.getSprite() == null || obj.getSprite().getName() == null) continue;
            if (obj.getSprite().getProperties().has(IsoPropertyType.CUSTOM_NAME) && obj.getSprite().getProperties().get(IsoPropertyType.CUSTOM_NAME).contains("Toilet") && Rand.NextBool(5)) {
                facing2 = this.getFacing(obj.getSprite());
                String itemType = "Base.Plunger";
                if (Rand.NextBool(2)) {
                    itemType = "Base.ToiletBrush";
                }
                if (Rand.NextBool(10)) {
                    itemType = "Base.ToiletPaper";
                }
                if (facing2 != null) {
                    float position2;
                    if (facing2 == IsoDirections.E) {
                        RBBasic.trySpawnStoryItem(itemType, sq, 0.16f, 0.84f, 0.0f);
                    }
                    if (facing2 == IsoDirections.W) {
                        position2 = 0.16f;
                        if (Rand.NextBool(2)) {
                            position2 = 0.84f;
                        }
                        RBBasic.trySpawnStoryItem(itemType, sq, 0.84f, position2, 0.0f);
                    }
                    if (facing2 == IsoDirections.N) {
                        position2 = 0.16f;
                        if (Rand.NextBool(2)) {
                            position2 = 0.84f;
                        }
                        RBBasic.trySpawnStoryItem(itemType, sq, position2, 0.84f, 0.0f);
                    }
                    if (facing2 != IsoDirections.S) break;
                    RBBasic.trySpawnStoryItem(itemType, sq, 0.84f, 0.16f, 0.0f);
                    break;
                }
            }
            if (!sink2 && !counter && obj.getSprite().getName().contains("sink") && Rand.NextBool(5) && obj.getSurfaceOffsetNoTable() > 0.0f) {
                facing2 = this.getFacing(obj.getSprite());
                if (facing2 == null) continue;
                this.generateSinkClutter(facing2, obj, sq, sinkClutter);
                sink2 = true;
                continue;
            }
            if (sink2 || counter || obj.getContainer() == null || !"counter".equals(obj.getContainer().getType()) || !Rand.NextBool(5)) continue;
            boolean doIt = true;
            for (int o2 = 0; o2 < sq.getObjects().size(); ++o2) {
                IsoObject obj2 = sq.getObjects().get(o2);
                if ((obj2.getSprite() == null || obj2.getSprite().getName() == null || !obj2.getSprite().getName().contains("sink")) && !(obj2 instanceof IsoStove) && !(obj2 instanceof IsoRadio)) continue;
                doIt = false;
                break;
            }
            if (!doIt || (facing = this.getFacing(obj.getSprite())) == null) continue;
            this.generateCounterClutter(facing, obj, sq, sinkClutter);
            counter = true;
        }
        if (bReleaseClutter) {
            s_clutterCopyPool.release(sinkClutter);
        }
    }

    private void generateKitchenStoveClutter(IsoDirections facing, IsoObject obj, IsoGridSquare sq) {
        TIntObjectHashMap<String> clutterCopy = this.getClutterCopy(this.getKitchenStoveClutter(), s_clutterCopyPool.alloc());
        this.generateKitchenStoveClutter(facing, obj, sq, clutterCopy);
        s_clutterCopyPool.release(clutterCopy);
    }

    private void generateKitchenStoveClutter(IsoDirections facing, IsoObject obj, IsoGridSquare sq, TIntObjectHashMap<String> stoveClutter) {
        boolean bReleaseClutter;
        boolean bl = bReleaseClutter = stoveClutter == null;
        if (stoveClutter == null) {
            stoveClutter = this.getClutterCopy(this.getKitchenStoveClutter(), s_clutterCopyPool.alloc());
        }
        int pos = Rand.Next(1, 3);
        String item = stoveClutter.get(Rand.Next(1, stoveClutter.size()));
        if (bReleaseClutter) {
            s_clutterCopyPool.release(stoveClutter);
        }
        if (facing == IsoDirections.W) {
            switch (pos) {
                case 1: {
                    this.addWorldItem(item, sq, 0.5703125f, 0.8046875f, obj.getSurfaceOffsetNoTable() / 96.0f, true);
                    break;
                }
                case 2: {
                    this.addWorldItem(item, sq, 0.5703125f, 0.2578125f, obj.getSurfaceOffsetNoTable() / 96.0f, true);
                }
            }
        }
        if (facing == IsoDirections.E) {
            switch (pos) {
                case 1: {
                    this.addWorldItem(item, sq, 0.5f, 0.7890625f, obj.getSurfaceOffsetNoTable() / 96.0f, true);
                    break;
                }
                case 2: {
                    this.addWorldItem(item, sq, 0.5f, 0.1875f, obj.getSurfaceOffsetNoTable() / 96.0f, true);
                }
            }
        }
        if (facing == IsoDirections.S) {
            switch (pos) {
                case 1: {
                    this.addWorldItem(item, sq, 0.3125f, 0.53125f, obj.getSurfaceOffsetNoTable() / 96.0f, true);
                    break;
                }
                case 2: {
                    this.addWorldItem(item, sq, 0.875f, 0.53125f, obj.getSurfaceOffsetNoTable() / 96.0f, true);
                }
            }
        }
        if (facing == IsoDirections.N) {
            switch (pos) {
                case 1: {
                    this.addWorldItem(item, sq, 0.3203f, 0.523475f, obj.getSurfaceOffsetNoTable() / 96.0f, true);
                    break;
                }
                case 2: {
                    this.addWorldItem(item, sq, 0.8907f, 0.523475f, obj.getSurfaceOffsetNoTable() / 96.0f, true);
                }
            }
        }
    }

    private void generateCounterClutter(IsoDirections facing, IsoObject obj, IsoGridSquare sq, TIntObjectHashMap<String> itemMap) {
        int max = Math.min(5, itemMap.size() + 1);
        int nbrItem = Rand.Next(1, max);
        ArrayList<Integer> positions = new ArrayList<Integer>();
        for (int i = 0; i < nbrItem; ++i) {
            int rand = Rand.Next(1, 5);
            boolean added = false;
            while (!added) {
                if (!positions.contains(rand)) {
                    positions.add(rand);
                    added = true;
                    continue;
                }
                rand = Rand.Next(1, 5);
            }
            if (positions.size() != 4) continue;
        }
        ArrayList<String> alreadyAdded = new ArrayList<String>();
        block26: for (int i = 0; i < positions.size(); ++i) {
            int pos = (Integer)positions.get(i);
            int randItem = Rand.Next(1, itemMap.size() + 1);
            String item = null;
            while (item == null) {
                item = itemMap.get(randItem);
                if (!alreadyAdded.contains(item)) continue;
                item = null;
                randItem = Rand.Next(1, itemMap.size() + 1);
            }
            if (item == null) continue;
            alreadyAdded.add(item);
            if (facing == IsoDirections.S) {
                switch (pos) {
                    case 1: {
                        this.addWorldItem(item, sq, 0.138f, Rand.Next(0.2f, 0.523f), obj.getSurfaceOffsetNoTable() / 96.0f, true);
                        break;
                    }
                    case 2: {
                        this.addWorldItem(item, sq, 0.383f, Rand.Next(0.2f, 0.523f), obj.getSurfaceOffsetNoTable() / 96.0f, true);
                        break;
                    }
                    case 3: {
                        this.addWorldItem(item, sq, 0.633f, Rand.Next(0.2f, 0.523f), obj.getSurfaceOffsetNoTable() / 96.0f, true);
                        break;
                    }
                    case 4: {
                        this.addWorldItem(item, sq, 0.78f, Rand.Next(0.2f, 0.523f), obj.getSurfaceOffsetNoTable() / 96.0f, true);
                    }
                }
            }
            if (facing == IsoDirections.N) {
                switch (pos) {
                    case 1: {
                        this.trySpawnStoryItem(item, sq, 0.133f, Rand.Next(0.53125f, 0.9375f), obj.getSurfaceOffsetNoTable() / 96.0f, true);
                        break;
                    }
                    case 2: {
                        this.trySpawnStoryItem(item, sq, 0.38f, Rand.Next(0.53125f, 0.9375f), obj.getSurfaceOffsetNoTable() / 96.0f, true);
                        break;
                    }
                    case 3: {
                        this.trySpawnStoryItem(item, sq, 0.625f, Rand.Next(0.53125f, 0.9375f), obj.getSurfaceOffsetNoTable() / 96.0f, true);
                        break;
                    }
                    case 4: {
                        this.trySpawnStoryItem(item, sq, 0.92f, Rand.Next(0.53125f, 0.9375f), obj.getSurfaceOffsetNoTable() / 96.0f, true);
                    }
                }
            }
            if (facing == IsoDirections.E) {
                switch (pos) {
                    case 1: {
                        this.trySpawnStoryItem(item, sq, Rand.Next(0.226f, 0.593f), 0.14f, obj.getSurfaceOffsetNoTable() / 96.0f, true);
                        break;
                    }
                    case 2: {
                        this.trySpawnStoryItem(item, sq, Rand.Next(0.226f, 0.593f), 0.33f, obj.getSurfaceOffsetNoTable() / 96.0f, true);
                        break;
                    }
                    case 3: {
                        this.trySpawnStoryItem(item, sq, Rand.Next(0.226f, 0.593f), 0.64f, obj.getSurfaceOffsetNoTable() / 96.0f, true);
                        break;
                    }
                    case 4: {
                        this.trySpawnStoryItem(item, sq, Rand.Next(0.226f, 0.593f), 0.92f, obj.getSurfaceOffsetNoTable() / 96.0f, true);
                    }
                }
            }
            if (facing != IsoDirections.W) continue;
            switch (pos) {
                case 1: {
                    this.trySpawnStoryItem(item, sq, Rand.Next(0.5859375f, 0.9f), 0.21875f, obj.getSurfaceOffsetNoTable() / 96.0f, true);
                    continue block26;
                }
                case 2: {
                    this.trySpawnStoryItem(item, sq, Rand.Next(0.5859375f, 0.9f), 0.421875f, obj.getSurfaceOffsetNoTable() / 96.0f, true);
                    continue block26;
                }
                case 3: {
                    this.trySpawnStoryItem(item, sq, Rand.Next(0.5859375f, 0.9f), 0.71f, obj.getSurfaceOffsetNoTable() / 96.0f, true);
                    continue block26;
                }
                case 4: {
                    this.trySpawnStoryItem(item, sq, Rand.Next(0.5859375f, 0.9f), 0.9175f, obj.getSurfaceOffsetNoTable() / 96.0f, true);
                }
            }
        }
    }

    private void generateSinkClutter(IsoDirections facing, IsoObject obj, IsoGridSquare sq, TIntObjectHashMap<String> itemMap) {
        int max = Math.min(4, itemMap.size() + 1);
        int nbrItem = Rand.Next(1, max);
        ArrayList<Integer> positions = new ArrayList<Integer>();
        for (int i = 0; i < nbrItem; ++i) {
            int rand = Rand.Next(1, 5);
            boolean added = false;
            while (!added) {
                if (!positions.contains(rand)) {
                    positions.add(rand);
                    added = true;
                    continue;
                }
                rand = Rand.Next(1, 5);
            }
            if (positions.size() != 4) continue;
        }
        ArrayList<String> alreadyAdded = new ArrayList<String>();
        block26: for (int i = 0; i < positions.size(); ++i) {
            int pos = (Integer)positions.get(i);
            int randItem = Rand.Next(1, itemMap.size() + 1);
            String item = null;
            while (item == null) {
                item = itemMap.get(randItem);
                if (!alreadyAdded.contains(item)) continue;
                item = null;
                randItem = Rand.Next(1, itemMap.size() + 1);
            }
            if (item == null) continue;
            alreadyAdded.add(item);
            if (facing == IsoDirections.S) {
                switch (pos) {
                    case 1: {
                        this.addWorldItem(item, sq, 0.71875f, 0.125f, obj.getSurfaceOffsetNoTable() / 96.0f, true);
                        break;
                    }
                    case 2: {
                        this.addWorldItem(item, sq, 0.0935f, 0.21875f, obj.getSurfaceOffsetNoTable() / 96.0f, true);
                        break;
                    }
                    case 3: {
                        this.addWorldItem(item, sq, 0.1328125f, 0.589375f, obj.getSurfaceOffsetNoTable() / 96.0f, true);
                        break;
                    }
                    case 4: {
                        this.addWorldItem(item, sq, 0.7890625f, 0.589375f, obj.getSurfaceOffsetNoTable() / 96.0f, true);
                    }
                }
            }
            if (facing == IsoDirections.N) {
                switch (pos) {
                    case 1: {
                        this.addWorldItem(item, sq, 0.921875f, 0.921875f, obj.getSurfaceOffsetNoTable() / 96.0f, true);
                        break;
                    }
                    case 2: {
                        this.addWorldItem(item, sq, 0.1640625f, 0.8984375f, obj.getSurfaceOffsetNoTable() / 96.0f, true);
                        break;
                    }
                    case 3: {
                        this.addWorldItem(item, sq, 0.021875f, 0.5f, obj.getSurfaceOffsetNoTable() / 96.0f, true);
                        break;
                    }
                    case 4: {
                        this.addWorldItem(item, sq, 0.8671875f, 0.5f, obj.getSurfaceOffsetNoTable() / 96.0f, true);
                    }
                }
            }
            if (facing == IsoDirections.E) {
                switch (pos) {
                    case 1: {
                        this.addWorldItem(item, sq, 0.234375f, 0.859375f, obj.getSurfaceOffsetNoTable() / 96.0f, true);
                        break;
                    }
                    case 2: {
                        this.addWorldItem(item, sq, 0.59375f, 0.875f, obj.getSurfaceOffsetNoTable() / 96.0f, true);
                        break;
                    }
                    case 3: {
                        this.addWorldItem(item, sq, 0.53125f, 0.125f, obj.getSurfaceOffsetNoTable() / 96.0f, true);
                        break;
                    }
                    case 4: {
                        this.addWorldItem(item, sq, 0.210937f, 0.1328125f, obj.getSurfaceOffsetNoTable() / 96.0f, true);
                    }
                }
            }
            if (facing != IsoDirections.W) continue;
            switch (pos) {
                case 1: {
                    this.addWorldItem(item, sq, 0.515625f, 0.109375f, obj.getSurfaceOffsetNoTable() / 96.0f, true);
                    continue block26;
                }
                case 2: {
                    this.addWorldItem(item, sq, 0.578125f, 0.890625f, obj.getSurfaceOffsetNoTable() / 96.0f, true);
                    continue block26;
                }
                case 3: {
                    this.addWorldItem(item, sq, 0.8828125f, 0.8984375f, obj.getSurfaceOffsetNoTable() / 96.0f, true);
                    continue block26;
                }
                case 4: {
                    this.addWorldItem(item, sq, 0.8671875f, 0.1653125f, obj.getSurfaceOffsetNoTable() / 96.0f, true);
                }
            }
        }
    }

    private IsoDirections getFacing(IsoSprite sprite) {
        if (sprite != null && sprite.getProperties().has(IsoPropertyType.FACING)) {
            String facing;
            switch (facing = sprite.getProperties().get(IsoPropertyType.FACING)) {
                case "N": {
                    return IsoDirections.N;
                }
                case "S": {
                    return IsoDirections.S;
                }
                case "W": {
                    return IsoDirections.W;
                }
                case "E": {
                    return IsoDirections.E;
                }
            }
        }
        return null;
    }

    private void checkForTableSpawn(BuildingDef def, IsoObject table1) {
        if (table1.getSquare().getRoom() != null && Rand.NextBool(10)) {
            RBTableStoryBase tableStory = RBTableStoryBase.getRandomStory(table1);
            tableStory.initTables(table1);
            if (tableStory.isValid(table1.getSquare(), false)) {
                tableStory.randomizeBuilding(def);
                this.doneTable = true;
            }
        }
    }

    private IsoObject checkForTable(IsoGridSquare sq, IsoObject table1) {
        if (this.tablesDone.contains(table1) || sq == null) {
            return null;
        }
        for (int o = 0; o < sq.getObjects().size(); ++o) {
            IsoObject obj = sq.getObjects().get(o);
            if (this.tablesDone.contains(obj) || !obj.getProperties().isTable() || obj.getProperties().getSurface() != 34 || obj.getContainer() != null || obj == table1) continue;
            return obj;
        }
        return null;
    }

    public void doProfessionStory(BuildingDef def, String professionChoosed) {
        this.spawnItemsInContainers(def, professionChoosed, 70);
    }

    private void addRandomDeadSurvivorStory(BuildingDef def) {
        this.initRDSMap(def);
        int choice = Rand.Next(this.totalChanceRds);
        Iterator<RandomizedDeadSurvivorBase> it = rdsMap.keySet().iterator();
        int subTotal = 0;
        while (it.hasNext()) {
            RandomizedDeadSurvivorBase testTable = it.next();
            if (choice >= (subTotal += rdsMap.get(testTable).intValue())) continue;
            testTable.randomizeDeadSurvivor(def);
            if (!testTable.isUnique()) break;
            RBBasic.getUniqueRDSSpawned().add(testTable.getName());
            break;
        }
    }

    private void initRDSMap(BuildingDef def) {
        this.totalChanceRds = 0;
        rdsMap.clear();
        for (int i = 0; i < this.deadSurvivorsStory.size(); ++i) {
            boolean noRats;
            RandomizedDeadSurvivorBase story = this.deadSurvivorsStory.get(i);
            boolean bl = noRats = SandboxOptions.instance.maximumRatIndex.getValue() <= 0;
            if (!story.isValid(def, false) || !story.isTimeValid(false) || story.isUnique() && RBBasic.getUniqueRDSSpawned().contains(story.getName()) || noRats && story.isRat()) continue;
            this.totalChanceRds += this.deadSurvivorsStory.get(i).getChance();
            rdsMap.put(this.deadSurvivorsStory.get(i), this.deadSurvivorsStory.get(i).getChance());
        }
    }

    public void doRandomDeadSurvivorStory(BuildingDef buildingDef, RandomizedDeadSurvivorBase dsDef) {
        dsDef.randomizeDeadSurvivor(buildingDef);
    }

    public RBBasic() {
        this.name = "RBBasic";
        this.deadSurvivorsStory.add(new RDSBleach());
        this.deadSurvivorsStory.add(new RDSGunslinger());
        this.deadSurvivorsStory.add(new RDSGunmanInBathroom());
        this.deadSurvivorsStory.add(new RDSZombieLockedBathroom());
        this.deadSurvivorsStory.add(new RDSDeadDrunk());
        this.deadSurvivorsStory.add(new RDSSpecificProfession());
        this.deadSurvivorsStory.add(new RDSZombiesEating());
        this.deadSurvivorsStory.add(new RDSBanditRaid());
        this.deadSurvivorsStory.add(new RDSBandPractice());
        this.deadSurvivorsStory.add(new RDSBathroomZed());
        this.deadSurvivorsStory.add(new RDSBedroomZed());
        this.deadSurvivorsStory.add(new RDSFootballNight());
        this.deadSurvivorsStory.add(new RDSHenDo());
        this.deadSurvivorsStory.add(new RDSStagDo());
        this.deadSurvivorsStory.add(new RDSStudentNight());
        this.deadSurvivorsStory.add(new RDSPokerNight());
        this.deadSurvivorsStory.add(new RDSSuicidePact());
        this.deadSurvivorsStory.add(new RDSPrisonEscape());
        this.deadSurvivorsStory.add(new RDSPrisonEscapeWithPolice());
        this.deadSurvivorsStory.add(new RDSSkeletonPsycho());
        this.deadSurvivorsStory.add(new RDSCorpsePsycho());
        this.deadSurvivorsStory.add(new RDSPoliceAtHouse());
        this.deadSurvivorsStory.add(new RDSHouseParty());
        this.deadSurvivorsStory.add(new RDSTinFoilHat());
        this.deadSurvivorsStory.add(new RDSHockeyPsycho());
        this.deadSurvivorsStory.add(new RDSDevouredByRats());
        this.deadSurvivorsStory.add(new RDSRPGNight());
        this.deadSurvivorsStory.add(new RDSRatKing());
        this.deadSurvivorsStory.add(new RDSRatInfested());
        this.deadSurvivorsStory.add(new RDSResourceGarage());
        this.deadSurvivorsStory.add(new RDSGrouchos());
        this.specificProfessionDistribution.add("Carpenter");
        this.specificProfessionDistribution.add("Electrician");
        this.specificProfessionDistribution.add("Farmer");
        this.specificProfessionDistribution.add("Nurse");
        this.specificProfessionDistribution.add("Chef");
        this.specificProfessionRoomDistribution.put("Carpenter", "kitchen");
        this.specificProfessionRoomDistribution.put("Electrician", "kitchen");
        this.specificProfessionRoomDistribution.put("Farmer", "kitchen");
        this.specificProfessionRoomDistribution.put("Nurse", "kitchen;bathroom");
        this.specificProfessionRoomDistribution.put("Chef", "kitchen");
        this.plankStash.put("floors_interior_tilesandwood_01_40", "floors_interior_tilesandwood_01_56");
        this.plankStash.put("floors_interior_tilesandwood_01_41", "floors_interior_tilesandwood_01_57");
        this.plankStash.put("floors_interior_tilesandwood_01_42", "floors_interior_tilesandwood_01_58");
        this.plankStash.put("floors_interior_tilesandwood_01_43", "floors_interior_tilesandwood_01_59");
        this.plankStash.put("floors_interior_tilesandwood_01_44", "floors_interior_tilesandwood_01_60");
        this.plankStash.put("floors_interior_tilesandwood_01_45", "floors_interior_tilesandwood_01_61");
        this.plankStash.put("floors_interior_tilesandwood_01_46", "floors_interior_tilesandwood_01_62");
        this.plankStash.put("floors_interior_tilesandwood_01_47", "floors_interior_tilesandwood_01_63");
        this.plankStash.put("floors_interior_tilesandwood_01_52", "floors_interior_tilesandwood_01_68");
    }

    public ArrayList<RandomizedDeadSurvivorBase> getSurvivorStories() {
        return this.deadSurvivorsStory;
    }

    public ArrayList<String> getSurvivorProfession() {
        return this.specificProfessionDistribution;
    }

    public static ArrayList<String> getUniqueRDSSpawned() {
        return uniqueRDSSpawned;
    }

    public void doProfessionBuilding(BuildingDef def, String professionChoosed, ItemPickerJava.ItemPickerRoom prof) {
        boolean corpseOrZombie;
        boolean doBag;
        Integer vehicleChance = Integer.valueOf(prof.vehicleChance);
        Integer femaleChance = Integer.valueOf(prof.femaleOdds);
        if (Core.debug) {
            DebugLog.log("Profession Female Chance: " + femaleChance);
        }
        if (prof.vehicleChance == null) {
            vehicleChance = 50;
        }
        if (prof.vehicles == null) {
            vehicleChance = null;
        }
        String vehicleDistribution = null;
        if (prof.vehicleDistribution != null) {
            vehicleDistribution = prof.vehicleDistribution;
        }
        if (Core.debug) {
            DebugLog.log("Profession House Initialized for " + professionChoosed + " at X: " + (def.x + def.x2) / 2 + ", Y: " + (def.y + def.y2) / 2);
        }
        String outfit = prof.outfit;
        IsoDeadBody body = null;
        Object zombie = null;
        IsoZombie zomb = null;
        BaseVehicle profVehicle = null;
        IsoGridSquare sq = def.getFreeSquareInRoom();
        boolean bl = doBag = Rand.Next(2) == 0;
        if (prof.bagType == null) {
            doBag = false;
        }
        boolean didBag = false;
        InventoryContainer bag = null;
        if (doBag) {
            DebugLog.log("Trying to spawn Profession Bag: " + prof.bagType);
            bag = (InventoryContainer)InventoryItemFactory.CreateItem(prof.bagType);
            if (bag != null) {
                DebugLog.log("Profession Bag Spawned: " + prof.bagType);
                if (prof.bagTable != null) {
                    ItemPickerJava.rollContainerItem(bag, null, ItemPickerJava.getItemPickerContainers().get(prof.bagTable));
                } else {
                    ItemPickerJava.rollContainerItem(bag, null, ItemPickerJava.getItemPickerContainers().get(bag.getType()));
                }
                String keyType = "Base.Key1";
                Object key2 = InventoryItemFactory.CreateItem("Base.Key1");
                if (key2 != null) {
                    ((InventoryItem)key2).setKeyId(def.getKeyId());
                    ItemPickerJava.KeyNamer.nameKey(key2, sq);
                }
                if (bag.getItemContainer() != null) {
                    bag.getItemContainer().addItem((InventoryItem)key2);
                }
            }
        }
        String vehicleType = null;
        if (prof.vehicle != null) {
            vehicleType = prof.vehicle;
        }
        if (vehicleType != null && vehicleChance != null && Rand.Next(100) < vehicleChance) {
            if (Core.debug) {
                DebugLog.log("Trying to spawn Profession vehicle: " + vehicleType);
            }
            if ((profVehicle = this.spawnCarOnNearestNav(vehicleType, def, vehicleDistribution)) != null && Core.debug) {
                DebugLog.log("Profession Vehicle " + profVehicle.getScriptName() + " for " + professionChoosed + " at X: " + profVehicle.getX() + ", Y: " + profVehicle.getY());
            }
        }
        boolean bl2 = corpseOrZombie = profVehicle == null;
        if (Rand.Next(2) == 0) {
            corpseOrZombie = true;
        }
        if (corpseOrZombie && sq != null) {
            boolean isFemale = Rand.Next(100) < femaleChance;
            femaleChance = isFemale ? Integer.valueOf(100) : Integer.valueOf(0);
            if (isFemale && prof.outfitFemale != null) {
                outfit = prof.outfitFemale;
            }
            if (!isFemale && prof.outfitMale != null) {
                outfit = prof.outfitMale;
            }
            if ((body = outfit != null ? RBBasic.createRandomDeadBody(sq, null, false, 0, 0, outfit, femaleChance) : RBBasic.createRandomDeadBody(sq, null, false, 0, 0, null, femaleChance)) != null) {
                if (Core.debug) {
                    DebugLog.log("Profession Corpse for " + professionChoosed + " at X: " + body.getX() + ", Y: " + body.getY() + ", Z: " + body.getZ() + " in Outfit " + outfit);
                }
                String keyType = "Base.Key1";
                InventoryItem key = body.getItemContainer().AddItem("Base.Key1");
                if (key != null) {
                    key.setKeyId(def.getKeyId());
                    ItemPickerJava.KeyNamer.nameKey(key, sq);
                }
                ItemPickerJava.rollItem(prof.containers.get("body"), body.getContainer(), true, null, null);
            }
            if (body != null && doBag && !didBag && bag != null) {
                if (sq.getRoom() != null && Objects.requireNonNull(body.getSquare().getRoom()).getRandomFreeSquare() != null) {
                    this.addItemOnGround(sq.getRoom().getRandomFreeSquare(), bag);
                } else {
                    this.addItemOnGround(body.getSquare(), bag);
                }
                didBag = true;
                if (Core.debug) {
                    DebugLog.log("Profession Bag Spawned With Corpse");
                }
            }
        }
        if (profVehicle != null) {
            InventoryItem key = profVehicle.createVehicleKey();
            if (zomb != null) {
                zomb.addItemToSpawnAtDeath(key);
            }
            if (body != null) {
                body.getContainer().AddItem(key);
            }
            if (zomb == null && body == null && sq != null) {
                ItemContainer cont = Objects.requireNonNull(sq.getBuilding()).getRandomContainerSingle("sidetable");
                if (cont == null) {
                    cont = sq.getBuilding().getRandomContainerSingle("dresser");
                }
                if (cont == null) {
                    cont = sq.getBuilding().getRandomContainerSingle("counter");
                }
                if (cont == null) {
                    cont = sq.getBuilding().getRandomContainerSingle("wardrobe");
                }
                if (cont != null) {
                    if (doBag && !didBag && bag != null) {
                        cont.addItem(bag);
                        if (bag.getItemContainer() != null) {
                            bag.getItemContainer().addItem(key);
                        } else {
                            profVehicle.putKeyToContainer(cont, cont.getParent().getSquare(), cont.getParent());
                        }
                        didBag = true;
                        if (Core.debug) {
                            DebugLog.log("Profession Bag and Profession Vehicle Key Spawned in " + cont.getType() + " at X: " + cont.getParent().getX() + ", Y: " + cont.getParent().getY() + ", Z: " + cont.getParent().getZ());
                        }
                    } else {
                        profVehicle.putKeyToContainer(cont, cont.getParent().getSquare(), cont.getParent());
                        if (Core.debug) {
                            DebugLog.log("Profession Vehicle Key Spawned in " + cont.getType() + " at X: " + cont.getParent().getX() + ", Y: " + cont.getParent().getY() + ", Z: " + cont.getParent().getZ());
                        }
                    }
                } else if (doBag && !didBag && bag != null) {
                    this.addItemOnGround(sq, bag);
                    if (bag.getItemContainer() != null) {
                        bag.getItemContainer().addItem(key);
                    } else {
                        profVehicle.putKeyToWorld(sq);
                    }
                    didBag = true;
                    if (Core.debug) {
                        DebugLog.log("Profession Bag and Profession Vehicle Key Spawned at X: " + sq.getX() + ", Y: " + sq.getY() + ", Z: " + sq.getZ());
                    }
                } else {
                    profVehicle.putKeyToWorld(sq);
                    if (Core.debug) {
                        DebugLog.log("Profession Vehicle Key Spawned at X: " + sq.getX() + ", Y: " + sq.getY() + ", Z: " + sq.getZ());
                    }
                }
            }
        }
        if (body != null && Rand.Next(2) == 0) {
            body.reanimateNow();
            if (Core.debug) {
                DebugLog.log("Profession Corpse promoted to Zombie at X: " + body.getX() + ", Y: " + body.getY() + ", Z: " + body.getZ());
            }
        }
        if (zomb != null && Rand.Next(2) == 0) {
            zomb.Kill(null, true);
            if (Core.debug) {
                DebugLog.log("Profession Zombie promoted to Corpse at X: " + zomb.getX() + ", Y: " + zomb.getY() + ", Z: " + zomb.getZ());
            }
        }
        if (doBag && !didBag && bag != null && sq != null) {
            ItemContainer cont = Objects.requireNonNull(sq.getBuilding()).getRandomContainer("sidetable");
            if (cont == null) {
                cont = sq.getBuilding().getRandomContainerSingle("dresser");
            }
            if (cont == null) {
                cont = sq.getBuilding().getRandomContainerSingle("wardrobe");
            }
            if (cont == null) {
                cont = sq.getBuilding().getRandomContainerSingle("counter");
            }
            if (cont != null) {
                cont.addItem(bag);
                if (Core.debug) {
                    DebugLog.log("Profession Bag Spawned in " + cont.getType() + " at X: " + cont.getParent().getX() + ", Y: " + cont.getParent().getY() + ", Z: " + cont.getParent().getZ());
                }
            } else {
                this.addItemOnGround(sq, bag);
                if (Core.debug) {
                    DebugLog.log("Profession Bag Spawned at X: " + sq.getX() + ", Y: " + sq.getY() + ", Z: " + sq.getZ());
                }
            }
        }
    }

    public static void doOfficeStuff(IsoGridSquare sq) {
        for (int i = 0; i < sq.getObjects().size(); ++i) {
            IsoObject obj = sq.getObjects().get(i);
            if (!obj.isTableSurface() || !Rand.NextBool(2) || sq.getObjects().size() != 2 || obj.getProperties().get("BedType") != null || !obj.isTableSurface() || obj.getContainer() != null && !"desk".equals(obj.getContainer().getType())) continue;
            if (Rand.Next(100) < 66) {
                RBBasic.trySpawnStoryItem(RBBasic.getOfficePenClutterItem(), sq, Rand.Next(0.4f, 0.8f), Rand.Next(0.4f, 0.8f), obj.getSurfaceOffsetNoTable() / 96.0f);
            }
            if (Rand.Next(100) < 66) {
                RBBasic.trySpawnStoryItem(RBBasic.getOfficePaperworkClutterItem(), sq, Rand.Next(0.4f, 0.8f), Rand.Next(0.4f, 0.8f), obj.getSurfaceOffsetNoTable() / 96.0f);
            }
            if (Rand.Next(100) < 66) {
                RBBasic.trySpawnStoryItem(RBBasic.getOfficeOtherClutterItem(), sq, Rand.Next(0.4f, 0.8f), Rand.Next(0.4f, 0.8f), obj.getSurfaceOffsetNoTable() / 96.0f);
            }
            if (Rand.Next(100) >= 20) continue;
            RBBasic.trySpawnStoryItem(RBBasic.getOfficeTreatClutterItem(), sq, Rand.Next(0.4f, 0.8f), Rand.Next(0.4f, 0.8f), obj.getSurfaceOffsetNoTable() / 96.0f);
        }
    }

    public static void doNolansOfficeStuff(IsoGridSquare sq) {
        for (int i = 0; i < sq.getObjects().size(); ++i) {
            IsoObject obj = sq.getObjects().get(i);
            if (!obj.isTableSurface() || Rand.Next(100) >= 80 || obj.getProperties().get("BedType") != null || obj.getContainer() != null && !"desk".equals(obj.getContainer().getType())) continue;
            RBBasic.trySpawnStoryItem(RBBasic.getOfficeCarDealerClutterItem(), sq, Rand.Next(0.4f, 0.8f), Rand.Next(0.4f, 0.8f), obj.getSurfaceOffsetNoTable() / 96.0f);
            if (Rand.Next(100) >= 50) continue;
            RBBasic.trySpawnStoryItem(RBBasic.getOfficeCarDealerClutterItem(), sq, Rand.Next(0.4f, 0.8f), Rand.Next(0.4f, 0.8f), obj.getSurfaceOffsetNoTable() / 96.0f);
            RBBasic.trySpawnStoryItem(RBBasic.getOfficeCarDealerClutterItem(), sq, Rand.Next(0.4f, 0.8f), Rand.Next(0.4f, 0.8f), obj.getSurfaceOffsetNoTable() / 96.0f);
        }
    }

    public static void doCafeStuff(IsoGridSquare sq) {
        for (int i = 0; i < sq.getObjects().size(); ++i) {
            IsoObject obj = sq.getObjects().get(i);
            if (!obj.isTableSurface()) continue;
            if (Rand.NextBool(3)) {
                RBBasic.trySpawnStoryItem("MugWhite", sq, Rand.Next(0.4f, 0.8f), Rand.Next(0.4f, 0.8f), obj.getSurfaceOffsetNoTable() / 96.0f);
            }
            if (Rand.Next(100) >= 40 || sq.getObjects().size() != 2 || obj.getProperties().get("BedType") != null || obj.getContainer() != null && !"desk".equals(obj.getContainer().getType())) continue;
            RBBasic.trySpawnStoryItem(RBBasic.getCafeClutterItem(), sq, Rand.Next(0.4f, 0.8f), Rand.Next(0.4f, 0.8f), obj.getSurfaceOffsetNoTable() / 96.0f);
            if (Rand.Next(100) >= 40) continue;
            RBBasic.trySpawnStoryItem(RBBasic.getCafeClutterItem(), sq, Rand.Next(0.4f, 0.8f), Rand.Next(0.4f, 0.8f), obj.getSurfaceOffsetNoTable() / 96.0f);
        }
    }

    public static void doGigamartStuff(IsoGridSquare sq) {
        for (int i = 0; i < sq.getObjects().size(); ++i) {
            IsoObject obj = sq.getObjects().get(i);
            if (!obj.isTableSurface() || Rand.Next(100) >= 30 || sq.getObjects().size() != 2 || obj.getContainer() == null) continue;
            RBBasic.trySpawnStoryItem(RBBasic.getGigamartClutterItem(), sq, Rand.Next(0.4f, 0.8f), Rand.Next(0.4f, 0.8f), obj.getSurfaceOffsetNoTable() / 96.0f);
            if (Rand.Next(100) >= 40) continue;
            RBBasic.trySpawnStoryItem(RBBasic.getGigamartClutterItem(), sq, Rand.Next(0.4f, 0.8f), Rand.Next(0.4f, 0.8f), obj.getSurfaceOffsetNoTable() / 96.0f);
        }
    }

    public static void doGroceryStuff(IsoGridSquare sq) {
        for (int i = 0; i < sq.getObjects().size(); ++i) {
            IsoObject obj = sq.getObjects().get(i);
            if (!obj.isTableSurface() || Rand.Next(100) >= 20 || sq.getObjects().size() != 2 || obj.getContainer() == null) continue;
            RBBasic.trySpawnStoryItem(RBBasic.getGroceryClutterItem(), sq, Rand.Next(0.4f, 0.8f), Rand.Next(0.4f, 0.8f), obj.getSurfaceOffsetNoTable() / 96.0f);
            if (Rand.Next(100) >= 40) continue;
            RBBasic.trySpawnStoryItem(RBBasic.getGroceryClutterItem(), sq, Rand.Next(0.4f, 0.8f), Rand.Next(0.4f, 0.8f), obj.getSurfaceOffsetNoTable() / 96.0f);
        }
    }

    public static void doGeneralRoom(IsoGridSquare sq, ArrayList<String> clutter) {
        for (int i = 0; i < sq.getObjects().size(); ++i) {
            IsoObject obj = sq.getObjects().get(i);
            if (!obj.isTableSurface() || sq.getObjects().size() > 3 || !Rand.NextBool(3)) continue;
            String item = RBBasic.getClutterItem(clutter);
            RBBasic.trySpawnStoryItem(item, obj, true);
        }
    }

    private void doLaundryStuff(IsoGridSquare sq) {
        TIntObjectHashMap<String> clutter = this.getClutterCopy(this.getLaundryRoomClutter(), s_clutterCopyPool.alloc());
        boolean kitchenSink = false;
        boolean counter = false;
        for (int o = 0; o < sq.getObjects().size(); ++o) {
            boolean isCounter;
            IsoObject obj = sq.getObjects().get(o);
            IsoDirections facing = this.getFacing(obj.getSprite());
            boolean bl = isCounter = obj.getContainer() != null && "counter".equals(obj.getContainer().getType());
            if (obj.getSprite() == null || obj.getSprite().getName() == null) continue;
            if (!kitchenSink && obj.getSprite().getName().contains("sink") && Rand.NextBool(4)) {
                if (facing == null) continue;
                this.generateSinkClutter(facing, obj, sq, clutter);
                kitchenSink = true;
                continue;
            }
            if (!counter && isCounter && Rand.NextBool(6)) {
                boolean doIt = true;
                for (int o2 = 0; o2 < sq.getObjects().size(); ++o2) {
                    IsoObject obj2 = sq.getObjects().get(o2);
                    if ((obj2.getSprite() == null || obj2.getSprite().getName() == null || !obj2.getSprite().getName().contains("sink")) && !(obj2 instanceof IsoStove) && !(obj2 instanceof IsoRadio)) continue;
                    doIt = false;
                    break;
                }
                if (!doIt || facing == null) continue;
                this.generateCounterClutter(facing, obj, sq, clutter);
                counter = true;
                continue;
            }
            if (isCounter || !obj.isTableSurface() || sq.getObjects().size() > 3 || !Rand.NextBool(3)) continue;
            String item = this.getLaundryRoomClutterItem();
            RBBasic.trySpawnStoryItem(item, obj, true);
        }
        s_clutterCopyPool.release(clutter);
    }

    public static void doJudgeStuff(IsoGridSquare sq) {
        for (int i = 0; i < sq.getObjects().size(); ++i) {
            IsoObject obj = sq.getObjects().get(i);
            if (!obj.isTableSurface() || Rand.Next(100) >= 80 || obj.getProperties().get("BedType") != null || obj.getContainer() != null && !"desk".equals(obj.getContainer().getType())) continue;
            RBBasic.trySpawnStoryItem(RBBasic.getJudgeClutterItem(), sq, Rand.Next(0.4f, 0.8f), Rand.Next(0.4f, 0.8f), obj.getSurfaceOffsetNoTable() / 96.0f);
            if (Rand.Next(100) >= 50) continue;
            RBBasic.trySpawnStoryItem(RBBasic.getJudgeClutterItem(), sq, Rand.Next(0.4f, 0.8f), Rand.Next(0.4f, 0.8f), obj.getSurfaceOffsetNoTable() / 96.0f);
            RBBasic.trySpawnStoryItem(RBBasic.getJudgeClutterItem(), sq, Rand.Next(0.4f, 0.8f), Rand.Next(0.4f, 0.8f), obj.getSurfaceOffsetNoTable() / 96.0f);
        }
    }

    public static void doTwiggyStuff(IsoGridSquare sq) {
        for (int i = 0; i < sq.getObjects().size(); ++i) {
            IsoObject obj = sq.getObjects().get(i);
            if (!obj.isTableSurface() || Rand.Next(100) >= 50 || obj.getProperties().get("BedType") != null || obj.getContainer() != null && !"counter".equals(obj.getContainer().getType())) continue;
            RBBasic.trySpawnStoryItem(RBBasic.getTwiggyClutterItem(), sq, Rand.Next(0.4f, 0.8f), Rand.Next(0.4f, 0.8f), obj.getSurfaceOffsetNoTable() / 96.0f);
            if (Rand.Next(100) >= 30) continue;
            RBBasic.trySpawnStoryItem(RBBasic.getTwiggyClutterItem(), sq, Rand.Next(0.4f, 0.8f), Rand.Next(0.4f, 0.8f), obj.getSurfaceOffsetNoTable() / 96.0f);
        }
    }

    public static void doWoodcraftStuff(IsoGridSquare sq) {
        for (int i = 0; i < sq.getObjects().size(); ++i) {
            IsoObject obj = sq.getObjects().get(i);
            if (!obj.isTableSurface() || Rand.Next(100) >= 80 || obj.getProperties().get("BedType") != null || obj.getContainer() != null && !"desk".equals(obj.getContainer().getType())) continue;
            RBBasic.trySpawnStoryItem(RBBasic.getWoodcraftClutterItem(), sq, Rand.Next(0.4f, 0.8f), Rand.Next(0.4f, 0.8f), obj.getSurfaceOffsetNoTable() / 96.0f);
            if (Rand.Next(100) >= 50) continue;
            RBBasic.trySpawnStoryItem(RBBasic.getWoodcraftClutterItem(), sq, Rand.Next(0.4f, 0.8f), Rand.Next(0.4f, 0.8f), obj.getSurfaceOffsetNoTable() / 96.0f);
            RBBasic.trySpawnStoryItem(RBBasic.getWoodcraftClutterItem(), sq, Rand.Next(0.4f, 0.8f), Rand.Next(0.4f, 0.8f), obj.getSurfaceOffsetNoTable() / 96.0f);
        }
    }
}

