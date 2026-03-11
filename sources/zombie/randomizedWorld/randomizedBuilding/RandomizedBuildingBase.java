/*
 * Decompiled with CFR 0.152.
 */
package zombie.randomizedWorld.randomizedBuilding;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Objects;
import se.krka.kahlua.vm.KahluaTable;
import zombie.Lua.LuaManager;
import zombie.SandboxOptions;
import zombie.UsedFromLua;
import zombie.VirtualZombieManager;
import zombie.ZombieSpawnRecorder;
import zombie.characters.IsoGameCharacter;
import zombie.characters.IsoPlayer;
import zombie.characters.IsoZombie;
import zombie.characters.SurvivorDesc;
import zombie.core.Core;
import zombie.core.random.Rand;
import zombie.core.skinnedmodel.visual.HumanVisual;
import zombie.core.skinnedmodel.visual.IHumanVisual;
import zombie.core.skinnedmodel.visual.ItemVisuals;
import zombie.core.stash.StashSystem;
import zombie.debug.DebugLog;
import zombie.inventory.InventoryItem;
import zombie.inventory.InventoryItemFactory;
import zombie.inventory.ItemContainer;
import zombie.inventory.ItemPickerJava;
import zombie.inventory.ItemSpawner;
import zombie.inventory.types.Food;
import zombie.inventory.types.HandWeapon;
import zombie.inventory.types.InventoryContainer;
import zombie.inventory.types.WeaponPart;
import zombie.iso.BuildingDef;
import zombie.iso.IsoCell;
import zombie.iso.IsoDirections;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoObject;
import zombie.iso.IsoWorld;
import zombie.iso.RoomDef;
import zombie.iso.SpawnPoints;
import zombie.iso.areas.IsoBuilding;
import zombie.iso.objects.IsoBarricade;
import zombie.iso.objects.IsoDoor;
import zombie.iso.objects.IsoWindow;
import zombie.iso.objects.interfaces.BarricadeAble;
import zombie.network.GameClient;
import zombie.network.GameServer;
import zombie.randomizedWorld.RandomizedWorldBase;
import zombie.randomizedWorld.randomizedBuilding.RBKateAndBaldspot;
import zombie.scripting.objects.ItemTag;
import zombie.util.StringUtils;

@UsedFromLua
public class RandomizedBuildingBase
extends RandomizedWorldBase {
    private int chance;
    private static int totalChance;
    private static final HashMap<RandomizedBuildingBase, Integer> rbMap;
    protected static final int KBBuildingX = 10744;
    protected static final int KBBuildingY = 9409;
    private boolean alwaysDo;
    public static int maximumRoomCount;
    private static final HashMap<String, String> weaponsList;

    public void randomizeBuilding(BuildingDef def) {
        def.alarmed = false;
    }

    public void init() {
        if (!weaponsList.isEmpty()) {
            return;
        }
        weaponsList.put("Base.Shotgun", "Base.ShotgunShellsBox");
        weaponsList.put("Base.Pistol", "Base.Bullets9mmBox");
        weaponsList.put("Base.Pistol2", "Base.Bullets45Box");
        weaponsList.put("Base.Pistol3", "Base.Bullets44Box");
        weaponsList.put("Base.VarmintRifle", "Base.556Box");
        weaponsList.put("Base.HuntingRifle", "Base.308Box");
    }

    public static void initAllRBMapChance() {
        for (int i = 0; i < IsoWorld.instance.getRandomizedBuildingList().size(); ++i) {
            totalChance += IsoWorld.instance.getRandomizedBuildingList().get(i).getChance();
            rbMap.put(IsoWorld.instance.getRandomizedBuildingList().get(i), IsoWorld.instance.getRandomizedBuildingList().get(i).getChance());
        }
    }

    public boolean isValid(BuildingDef def, boolean force) {
        this.debugLine = "";
        if (GameClient.client) {
            return false;
        }
        if (StashSystem.isStashBuilding(def)) {
            this.debugLine = "Stash buildings are invalid";
            return false;
        }
        if (def.isAllExplored() && !force) {
            return false;
        }
        if (!GameServer.server) {
            if (!force && IsoPlayer.getInstance().getSquare() != null && IsoPlayer.getInstance().getSquare().getBuilding() != null && IsoPlayer.getInstance().getSquare().getBuilding().def == def) {
                this.customizeStartingHouse(IsoPlayer.getInstance().getSquare().getBuilding().def);
                return false;
            }
        } else if (!force) {
            for (int i = 0; i < GameServer.Players.size(); ++i) {
                IsoPlayer player = GameServer.Players.get(i);
                if (player.getSquare() == null || player.getSquare().getBuilding() == null || player.getSquare().getBuilding().def != def) continue;
                return false;
            }
        }
        boolean bedroom = false;
        boolean kitchen = false;
        boolean bathroom = false;
        for (int i = 0; i < def.rooms.size(); ++i) {
            RoomDef room = def.rooms.get(i);
            if ("bedroom".equals(room.name)) {
                bedroom = true;
            }
            if ("kitchen".equals(room.name) || "livingroom".equals(room.name)) {
                kitchen = true;
            }
            if (!"bathroom".equals(room.name)) continue;
            bathroom = true;
        }
        if (!bedroom) {
            this.debugLine = this.debugLine + "no bedroom ";
        }
        if (!bathroom) {
            this.debugLine = this.debugLine + "no bathroom ";
        }
        if (!kitchen) {
            this.debugLine = this.debugLine + "no living room or kitchen ";
        }
        return bedroom && bathroom && kitchen;
    }

    private void customizeStartingHouse(BuildingDef def) {
    }

    public int getMinimumDays() {
        return this.minimumDays;
    }

    public void setMinimumDays(int minimumDays) {
        this.minimumDays = minimumDays;
    }

    public int getMinimumRooms() {
        return this.minimumRooms;
    }

    public void setMinimumRooms(int minimumRooms) {
        this.minimumRooms = minimumRooms;
    }

    public static void ChunkLoaded(IsoBuilding building) {
        boolean debugSpam = false;
        if (!GameClient.client && building.def != null && !building.def.seen && building.def.isFullyStreamedIn()) {
            int i;
            boolean tooManyRooms;
            int roomCount = building.rooms.size();
            boolean bl = tooManyRooms = roomCount > maximumRoomCount;
            if (GameServer.server && GameServer.Players.isEmpty()) {
                return;
            }
            for (int i2 = 0; i2 < roomCount; ++i2) {
                if (!building.rooms.get((int)i2).def.explored) continue;
                return;
            }
            building.def.seen = true;
            if (!building.def.isAnyChunkNewlyLoaded()) {
                return;
            }
            ArrayList<RandomizedBuildingBase> forcedStory = new ArrayList<RandomizedBuildingBase>();
            if (!tooManyRooms) {
                for (i = 0; i < IsoWorld.instance.getRandomizedBuildingList().size(); ++i) {
                    RandomizedBuildingBase testTable = IsoWorld.instance.getRandomizedBuildingList().get(i);
                    if (testTable.reallyAlwaysForce && testTable.isValid(building.def, false)) {
                        testTable.randomizeBuilding(building.def);
                        continue;
                    }
                    if (!testTable.isAlwaysDo() || !testTable.isValid(building.def, false)) continue;
                    forcedStory.add(testTable);
                }
            }
            if (tooManyRooms) {
                DebugLog.log("Building is too large for a  Building Story with " + roomCount + " rooms  at " + building.def.x + ", " + building.def.y + " and is rejected.");
                return;
            }
            if (building.def.x == 10744 && building.def.y == 9409 && Rand.Next(100) < 31) {
                RBKateAndBaldspot rb = new RBKateAndBaldspot();
                ((RandomizedBuildingBase)rb).randomizeBuilding(building.def);
                return;
            }
            if (!forcedStory.isEmpty()) {
                for (i = 0; i < forcedStory.size(); ++i) {
                    RandomizedBuildingBase rb = (RandomizedBuildingBase)forcedStory.get(i);
                    if (rb == null) continue;
                    rb.randomizeBuilding(building.def);
                }
            }
            if (SpawnPoints.instance.isSpawnBuilding(building.getDef())) {
                return;
            }
            RandomizedBuildingBase rb = IsoWorld.instance.getRBBasic();
            if ("Tutorial".equals(Core.gameMode)) {
                return;
            }
            try {
                int chance = 10;
                switch (SandboxOptions.instance.survivorHouseChance.getValue()) {
                    case 1: {
                        return;
                    }
                    case 2: {
                        chance -= 5;
                        break;
                    }
                    case 4: {
                        chance += 5;
                        break;
                    }
                    case 5: {
                        chance += 10;
                        break;
                    }
                    case 6: {
                        chance += 20;
                    }
                }
                if (SandboxOptions.instance.survivorHouseChance.getValue() == 7 || Rand.Next(100) <= chance) {
                    if (totalChance == 0) {
                        RandomizedBuildingBase.initAllRBMapChance();
                    }
                    if ((rb = RandomizedBuildingBase.getRandomStory()) == null) {
                        return;
                    }
                }
                if (rb.isValid(building.def, false) && rb.isTimeValid(false)) {
                    rb.randomizeBuilding(building.def);
                }
            }
            catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    public int getChance() {
        return this.getChance(null);
    }

    public int getChance(IsoGridSquare sq) {
        if (Objects.equals(this.name, "Rat Infested House")) {
            int ratFactor = SandboxOptions.instance.getCurrentRatIndex() / 10;
            if (ratFactor < 0) {
                ratFactor = 1;
            }
            return ratFactor;
        }
        if (Objects.equals(this.name, "Trashed Building")) {
            return SandboxOptions.instance.getCurrentLootedChance(sq);
        }
        return this.chance;
    }

    public void setChance(int chance) {
        this.chance = chance;
    }

    public boolean isAlwaysDo() {
        return this.alwaysDo;
    }

    public void setAlwaysDo(boolean alwaysDo) {
        this.alwaysDo = alwaysDo;
    }

    private static RandomizedBuildingBase getRandomStory() {
        int choice = Rand.Next(totalChance);
        Iterator<RandomizedBuildingBase> it = rbMap.keySet().iterator();
        int subTotal = 0;
        while (it.hasNext()) {
            RandomizedBuildingBase testTable = it.next();
            if (choice >= (subTotal += rbMap.get(testTable).intValue())) continue;
            return testTable;
        }
        return null;
    }

    @Override
    public ArrayList<IsoZombie> addZombiesOnSquare(int totalZombies, String outfit, Integer femaleChance, IsoGridSquare square) {
        if (IsoWorld.getZombiesDisabled() || "Tutorial".equals(Core.gameMode)) {
            return null;
        }
        ArrayList<IsoZombie> result = new ArrayList<IsoZombie>();
        for (int j = 0; j < totalZombies; ++j) {
            VirtualZombieManager.instance.choices.clear();
            VirtualZombieManager.instance.choices.add(square);
            IsoZombie zombie = VirtualZombieManager.instance.createRealZombieAlways(IsoDirections.getRandom(), false);
            if (zombie == null) continue;
            if ("Kate".equals(outfit) || "Bob".equals(outfit) || "Raider".equals(outfit)) {
                zombie.doDirtBloodEtc = false;
            }
            if (femaleChance != null) {
                zombie.setFemaleEtc(Rand.Next(100) < femaleChance);
            }
            if (outfit != null) {
                zombie.dressInPersistentOutfit(outfit);
                zombie.dressInRandomOutfit = false;
            } else {
                zombie.dressInRandomOutfit = true;
            }
            result.add(zombie);
        }
        ZombieSpawnRecorder.instance.record(result, this.getClass().getSimpleName());
        return result;
    }

    public ArrayList<IsoZombie> addZombies(BuildingDef def, int totalZombies, String outfit, Integer femaleChance, RoomDef room) {
        IsoGridSquare sq;
        boolean randomizeRoom = room == null;
        ArrayList<IsoZombie> result = new ArrayList<IsoZombie>();
        if (IsoWorld.getZombiesDisabled() || "Tutorial".equals(Core.gameMode)) {
            return result;
        }
        if (room == null) {
            room = this.getRandomRoom(def, 6);
        }
        int min = 2;
        int max = room.area / 2;
        if (totalZombies == 0) {
            if (SandboxOptions.instance.zombies.getValue() == 1) {
                max += 4;
            } else if (SandboxOptions.instance.zombies.getValue() == 2) {
                max += 3;
            } else if (SandboxOptions.instance.zombies.getValue() == 3) {
                max += 2;
            } else if (SandboxOptions.instance.zombies.getValue() == 5) {
                max -= 4;
            }
            if (max > 8) {
                max = 8;
            }
            if (max < min) {
                max = min + 1;
            }
        } else {
            max = min = totalZombies;
        }
        int rand = Rand.Next(min, max);
        for (int j = 0; j < rand && (sq = RandomizedBuildingBase.getRandomSpawnSquare(room)) != null; ++j) {
            VirtualZombieManager.instance.choices.clear();
            VirtualZombieManager.instance.choices.add(sq);
            IsoZombie zombie = VirtualZombieManager.instance.createRealZombieAlways(IsoDirections.getRandom(), false);
            if (zombie == null) continue;
            if (femaleChance != null) {
                zombie.setFemaleEtc(Rand.Next(100) < femaleChance);
            }
            if (outfit != null) {
                zombie.dressInPersistentOutfit(outfit);
                zombie.dressInRandomOutfit = false;
            } else {
                zombie.dressInRandomOutfit = true;
            }
            result.add(zombie);
            if (!randomizeRoom) continue;
            room = this.getRandomRoom(def, 6);
        }
        ZombieSpawnRecorder.instance.record(result, this.getClass().getSimpleName());
        return result;
    }

    public HandWeapon addRandomRangedWeapon(ItemContainer container, boolean addBulletsInGun, boolean addBoxInContainer, boolean attachPart) {
        ArrayList<String> weapons;
        String selectedWeapon;
        HandWeapon weapon;
        if (weaponsList == null || weaponsList.isEmpty()) {
            this.init();
        }
        if ((weapon = this.addWeapon(selectedWeapon = (weapons = new ArrayList<String>(weaponsList.keySet())).get(Rand.Next(0, weapons.size())), addBulletsInGun)) == null) {
            return null;
        }
        if (addBoxInContainer) {
            container.addItem((InventoryItem)InventoryItemFactory.CreateItem(weaponsList.get(selectedWeapon)));
        }
        if (attachPart) {
            KahluaTable weaponDistrib = (KahluaTable)LuaManager.env.rawget("WeaponUpgrades");
            if (weaponDistrib == null) {
                return null;
            }
            KahluaTable weaponUpgrade = (KahluaTable)weaponDistrib.rawget(weapon.getType());
            if (weaponUpgrade == null) {
                return null;
            }
            int upgrades = Rand.Next(1, weaponUpgrade.len() + 1);
            for (int u = 1; u <= upgrades; ++u) {
                int r = Rand.Next(weaponUpgrade.len()) + 1;
                WeaponPart part = (WeaponPart)InventoryItemFactory.CreateItem((String)weaponUpgrade.rawget(r));
                if (part == null || part.getScriptItem().obsolete) continue;
                weapon.attachWeaponPart(part);
            }
        }
        return weapon;
    }

    public void spawnItemsInContainers(BuildingDef def, String distribName, int chance) {
        ArrayList<ItemContainer> container = new ArrayList<ItemContainer>();
        ItemPickerJava.ItemPickerRoom contDistrib = ItemPickerJava.rooms.get(distribName);
        IsoCell cell = IsoWorld.instance.currentCell;
        for (int x = def.x - 1; x < def.x2 + 1; ++x) {
            for (int y = def.y - 1; y < def.y2 + 1; ++y) {
                for (int z = -32; z < 31; ++z) {
                    IsoGridSquare sq = cell.getGridSquare(x, y, z);
                    if (sq == null) continue;
                    for (int o = 0; o < sq.getObjects().size(); ++o) {
                        IsoObject obj = sq.getObjects().get(o);
                        if (Rand.Next(100) > chance || obj.getContainer() == null || sq.getRoom() == null || sq.getRoom().getName() == null || !contDistrib.containers.containsKey(obj.getContainer().getType())) continue;
                        obj.getContainer().clear();
                        container.add(obj.getContainer());
                        obj.getContainer().setExplored(true);
                    }
                }
            }
        }
        for (int i = 0; i < container.size(); ++i) {
            ItemContainer cont = (ItemContainer)container.get(i);
            ItemPickerJava.fillContainerType(contDistrib, cont, "", null);
            ItemPickerJava.updateOverlaySprite(cont.getParent());
            if (!GameServer.server) continue;
            GameServer.sendItemsInContainer(cont.getParent(), cont);
        }
    }

    protected void removeAllZombies(BuildingDef def) {
        for (int x = def.x - 1; x < def.x + def.x2 + 1; ++x) {
            for (int y = def.y - 1; y < def.y + def.y2 + 1; ++y) {
                for (int z = -32; z < 31; ++z) {
                    IsoGridSquare sq = RandomizedBuildingBase.getSq(x, y, z);
                    if (sq == null) continue;
                    for (int i = 0; i < sq.getMovingObjects().size(); ++i) {
                        sq.getMovingObjects().remove(i);
                        --i;
                    }
                }
            }
        }
    }

    public IsoWindow getWindow(IsoGridSquare sq) {
        for (int o = 0; o < sq.getObjects().size(); ++o) {
            IsoObject obj = sq.getObjects().get(o);
            if (!(obj instanceof IsoWindow)) continue;
            IsoWindow isoWindow = (IsoWindow)obj;
            return isoWindow;
        }
        return null;
    }

    public IsoDoor getDoor(IsoGridSquare sq) {
        for (int o = 0; o < sq.getObjects().size(); ++o) {
            IsoObject obj = sq.getObjects().get(o);
            if (!(obj instanceof IsoDoor)) continue;
            IsoDoor isoDoor = (IsoDoor)obj;
            return isoDoor;
        }
        return null;
    }

    public void addBarricade(IsoGridSquare sq, int numPlanks) {
        for (int o = 0; o < sq.getObjects().size(); ++o) {
            IsoWindow isoWindow;
            int b;
            boolean addOpposite;
            IsoBarricade barricade;
            IsoGridSquare outside;
            IsoObject obj = sq.getObjects().get(o);
            if (obj instanceof IsoDoor) {
                IsoDoor isoDoor = (IsoDoor)obj;
                if (!isoDoor.isBarricadeAllowed()) continue;
                IsoGridSquare isoGridSquare = outside = sq.getRoom() == null ? sq : isoDoor.getOppositeSquare();
                if (outside != null && outside.getRoom() == null && (barricade = IsoBarricade.AddBarricadeToObject((BarricadeAble)isoDoor, addOpposite = outside != sq)) != null) {
                    for (b = 0; b < numPlanks; ++b) {
                        barricade.addPlank(null, null);
                    }
                    if (GameServer.server) {
                        barricade.transmitCompleteItemToClients();
                    }
                }
            }
            if (!(obj instanceof IsoWindow) || !(isoWindow = (IsoWindow)obj).isBarricadeAllowed() || (barricade = IsoBarricade.AddBarricadeToObject((BarricadeAble)isoWindow, addOpposite = (outside = sq.getRoom() == null ? sq : isoWindow.getOppositeSquare()) != sq)) == null) continue;
            for (b = 0; b < numPlanks; ++b) {
                barricade.addPlank(null, null);
            }
            if (!GameServer.server) continue;
            barricade.transmitCompleteItemToClients();
        }
    }

    public InventoryItem addWorldItem(String item, IsoGridSquare sq, float xoffset, float yoffset, float zoffset) {
        return this.addWorldItem(item, sq, xoffset, yoffset, zoffset, 0);
    }

    public InventoryItem addWorldItem(String item, IsoGridSquare sq, float xoffset, float yoffset, float zoffset, boolean randomRotation) {
        if (randomRotation) {
            return this.addWorldItem(item, sq, xoffset, yoffset, zoffset, Rand.Next(360));
        }
        return this.addWorldItem(item, sq, xoffset, yoffset, zoffset, 0);
    }

    public InventoryItem addWorldItem(String item, IsoGridSquare sq, float xoffset, float yoffset, float zoffset, int worldZ) {
        if (item == null || sq == null) {
            return null;
        }
        if (SandboxOptions.instance.removeStoryLoot.getValue() && ItemPickerJava.getLootModifier(item) == 0.0f) {
            return null;
        }
        Object invItem = InventoryItemFactory.CreateItem(item);
        if (invItem != null) {
            ((InventoryItem)invItem).setAutoAge();
            ((InventoryItem)invItem).setWorldZRotation(worldZ);
            if (((InventoryItem)invItem).hasTag(ItemTag.SPAWN_COOKED)) {
                Object functionObj;
                ((InventoryItem)invItem).setCooked(true);
                if (!StringUtils.isNullOrEmpty(((Food)invItem).getOnCooked()) && (functionObj = LuaManager.getFunctionObject(((Food)invItem).getOnCooked())) != null) {
                    LuaManager.caller.pcallvoid(LuaManager.thread, functionObj, invItem);
                }
            }
            if (invItem instanceof HandWeapon) {
                HandWeapon handWeapon = (HandWeapon)invItem;
                if (Rand.Next(100) < 90) {
                    handWeapon.randomizeFirearmAsLoot();
                }
            }
            if (((InventoryItem)invItem).hasTag(ItemTag.SHOW_CONDITION) || invItem instanceof HandWeapon || ((InventoryItem)invItem).hasSharpness()) {
                ((InventoryItem)invItem).randomizeGeneralCondition();
            }
            ((InventoryItem)invItem).unsealIfNotFull();
            if (invItem instanceof InventoryContainer) {
                InventoryContainer inventoryContainer = (InventoryContainer)invItem;
                inventoryContainer.getItemContainer().setExplored(true);
            }
            return ItemSpawner.spawnItem(invItem, sq, xoffset, yoffset, zoffset);
        }
        return null;
    }

    public InventoryItem addWorldItem(String item, IsoGridSquare sq, IsoObject obj) {
        return this.addWorldItem(item, sq, obj, false);
    }

    public InventoryItem addWorldItem(String item, IsoGridSquare sq, IsoObject obj, boolean randomRotation) {
        Object invItem;
        if (item == null || sq == null || !sq.hasAdjacentCanStandSquare()) {
            return null;
        }
        if (SandboxOptions.instance.removeStoryLoot.getValue() && ItemPickerJava.getLootModifier(item) == 0.0f) {
            return null;
        }
        float z = 0.0f;
        if (obj != null) {
            z = obj.getSurfaceOffsetNoTable() / 96.0f;
        }
        if ((invItem = InventoryItemFactory.CreateItem(item)) != null) {
            ((InventoryItem)invItem).setAutoAge();
            if (randomRotation) {
                ((InventoryItem)invItem).randomizeWorldZRotation();
            }
            return ItemSpawner.spawnItem(invItem, sq, Rand.Next(0.3f, 0.9f), Rand.Next(0.3f, 0.9f), z);
        }
        return null;
    }

    public boolean isTableFor3DItems(IsoObject obj, IsoGridSquare sq) {
        return sq.hasAdjacentCanStandSquare() && obj.getSurfaceOffsetNoTable() > 0.0f && obj.getContainer() == null && !sq.hasWater() && !obj.hasFluid() && obj.getProperties().get("BedType") == null;
    }

    public InventoryItem trySpawnStoryItem(String itemType, IsoGridSquare square, IsoObject obj) {
        if (SandboxOptions.instance.removeStoryLoot.getValue() && ItemPickerJava.getLootModifier(itemType) == 0.0f) {
            return null;
        }
        return this.addWorldItem("PlasticFork", square, obj);
    }

    static {
        rbMap = new HashMap();
        maximumRoomCount = 500;
        weaponsList = new HashMap();
    }

    public static final class HumanCorpse
    extends IsoGameCharacter
    implements IHumanVisual {
        final HumanVisual humanVisual = new HumanVisual(this);
        final ItemVisuals itemVisuals = new ItemVisuals();
        public boolean isSkeleton;

        public HumanCorpse(IsoCell cell, float x, float y, float z) {
            super(cell, x, y, z);
            cell.getObjectList().remove(this);
            cell.getAddList().remove(this);
        }

        @Override
        public void dressInNamedOutfit(String outfitName) {
            this.getHumanVisual().dressInNamedOutfit(outfitName, this.itemVisuals);
            this.getHumanVisual().synchWithOutfit(this.getHumanVisual().getOutfit());
        }

        @Override
        public HumanVisual getHumanVisual() {
            return this.humanVisual;
        }

        @Override
        public HumanVisual getVisual() {
            return this.humanVisual;
        }

        @Override
        public void Dressup(SurvivorDesc desc) {
            this.wornItems.setFromItemVisuals(this.itemVisuals);
            this.wornItems.addItemsToItemContainer(this.inventory);
        }

        @Override
        public boolean isSkeleton() {
            return this.isSkeleton;
        }
    }
}

