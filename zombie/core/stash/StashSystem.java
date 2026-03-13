/*
 * Decompiled with CFR 0.152.
 */
package zombie.core.stash;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Objects;
import se.krka.kahlua.j2se.KahluaTableImpl;
import se.krka.kahlua.vm.KahluaTable;
import se.krka.kahlua.vm.KahluaTableIterator;
import zombie.GameTime;
import zombie.GameWindow;
import zombie.Lua.LuaManager;
import zombie.SandboxOptions;
import zombie.UsedFromLua;
import zombie.VirtualZombieManager;
import zombie.ZombieSpawnRecorder;
import zombie.characters.IsoZombie;
import zombie.core.Translator;
import zombie.core.random.Rand;
import zombie.core.stash.Stash;
import zombie.core.stash.StashAnnotation;
import zombie.core.stash.StashBuilding;
import zombie.core.stash.StashContainer;
import zombie.debug.DebugLog;
import zombie.inventory.InventoryItem;
import zombie.inventory.InventoryItemFactory;
import zombie.inventory.ItemContainer;
import zombie.inventory.ItemPickerJava;
import zombie.inventory.ItemSpawner;
import zombie.inventory.types.HandWeapon;
import zombie.inventory.types.InventoryContainer;
import zombie.inventory.types.MapItem;
import zombie.iso.BuildingDef;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoObject;
import zombie.iso.IsoWorld;
import zombie.iso.RoomDef;
import zombie.iso.objects.IsoDeadBody;
import zombie.iso.objects.IsoDoor;
import zombie.iso.objects.IsoThumpable;
import zombie.iso.objects.IsoTrap;
import zombie.iso.objects.IsoWindow;
import zombie.iso.objects.interfaces.BarricadeAble;
import zombie.network.GameClient;
import zombie.network.GameServer;
import zombie.util.Type;
import zombie.worldMap.symbols.WorldMapBaseSymbol;

@UsedFromLua
public final class StashSystem {
    public static ArrayList<Stash> allStashes;
    public static ArrayList<StashBuilding> possibleStashes;
    public static ArrayList<StashBuilding> buildingsToDo;
    private static final ArrayList<String> possibleTrap;
    private static ArrayList<String> alreadyReadMap;

    public static void init() {
        if (possibleStashes == null) {
            StashSystem.initAllStashes();
            buildingsToDo = new ArrayList();
            possibleTrap.add("Base.FlameTrapSensorV1");
            possibleTrap.add("Base.SmokeBombSensorV1");
            possibleTrap.add("Base.NoiseTrapSensorV1");
            possibleTrap.add("Base.NoiseTrapSensorV2");
            possibleTrap.add("Base.AerosolbombSensorV1");
        }
    }

    public static void initAllStashes() {
        allStashes = new ArrayList();
        possibleStashes = new ArrayList();
        KahluaTable stashTable = (KahluaTable)LuaManager.env.rawget("StashDescriptions");
        KahluaTableIterator it = stashTable.iterator();
        while (it.advance()) {
            KahluaTableImpl stashDesc = (KahluaTableImpl)it.getValue();
            Stash stashObj = new Stash(stashDesc.rawgetStr("name"));
            stashObj.load(stashDesc);
            allStashes.add(stashObj);
        }
    }

    public static ArrayList<Stash> getAllStashes() {
        return allStashes;
    }

    public static ArrayList<String> getAlreadyReadMap() {
        return alreadyReadMap;
    }

    public static void checkStashItem(InventoryItem item) {
        if (GameClient.client || possibleStashes.isEmpty()) {
            return;
        }
        int chance = 60;
        if (item.getStashChance() > 0) {
            chance = item.getStashChance();
        }
        switch (SandboxOptions.instance.annotatedMapChance.getValue()) {
            case 1: {
                return;
            }
            case 2: {
                chance += 15;
                break;
            }
            case 3: {
                chance += 10;
                break;
            }
            case 5: {
                chance -= 10;
                break;
            }
            case 6: {
                chance -= 20;
            }
        }
        if (Rand.Next(100) > 100 - chance) {
            return;
        }
        ArrayList<Stash> correctStashes = new ArrayList<Stash>();
        for (int i = 0; i < allStashes.size(); ++i) {
            Stash stash = allStashes.get(i);
            if (!stash.item.equals(item.getFullType()) || !StashSystem.checkSpecificSpawnProperties(stash, item)) continue;
            boolean isPossible = false;
            for (int j = 0; j < possibleStashes.size(); ++j) {
                BuildingDef def;
                StashBuilding stashBuilding = possibleStashes.get(j);
                if (stashBuilding == null || IsoWorld.instance.getMetaGrid().getRoomAt(stash.buildingX, stash.buildingY, 0) == null || Objects.requireNonNull(IsoWorld.instance.getMetaGrid().getRoomAt(stash.buildingX, stash.buildingY, 0)).getBuilding() == null || (def = Objects.requireNonNull(IsoWorld.instance.getMetaGrid().getRoomAt(stash.buildingX, stash.buildingY, 0)).getBuilding()) == null) continue;
                boolean explored = def.isHasBeenVisited();
                if (!stashBuilding.stashName.equals(stash.name) || explored) continue;
                isPossible = true;
                break;
            }
            if (!isPossible) continue;
            correctStashes.add(stash);
        }
        if (correctStashes.isEmpty()) {
            return;
        }
        Stash stash = (Stash)correctStashes.get(Rand.Next(0, correctStashes.size()));
        StashSystem.doStashItem(stash, item);
    }

    public static void doStashItem(Stash stash, InventoryItem item) {
        if (stash.customName != null) {
            item.setName(stash.customName);
        }
        if ("Map".equals(stash.type)) {
            if (!(item instanceof MapItem)) {
                throw new IllegalArgumentException(String.valueOf(item) + " is not a MapItem");
            }
            MapItem mapItem = (MapItem)item;
            if (stash.annotations != null) {
                for (int i = 0; i < stash.annotations.size(); ++i) {
                    WorldMapBaseSymbol symbol;
                    StashAnnotation annotation = stash.annotations.get(i);
                    if (annotation.symbol != null) {
                        symbol = mapItem.getSymbols().addTexture(annotation.symbol, annotation.x, annotation.y, 0.5f, 0.5f, 0.666f, annotation.r, annotation.g, annotation.b, 1.0f);
                    } else if (annotation.text != null && Translator.getTextOrNull(annotation.text) != null) {
                        symbol = mapItem.getSymbols().addUntranslatedText(annotation.text, "note", annotation.x, annotation.y, annotation.r, annotation.g, annotation.b, 1.0f);
                    } else {
                        if (annotation.text == null) continue;
                        symbol = mapItem.getSymbols().addTranslatedText(annotation.text, "note", annotation.x, annotation.y, annotation.r, annotation.g, annotation.b, 1.0f);
                    }
                    if (!Float.isNaN(annotation.anchorX) && !Float.isNaN(annotation.anchorY)) {
                        symbol.setAnchor(annotation.anchorX, annotation.anchorY);
                    }
                    if (Float.isNaN(annotation.rotation)) continue;
                    symbol.setRotation(annotation.rotation);
                }
            }
            StashSystem.removeFromPossibleStash(stash);
            item.setStashMap(stash.name);
        }
    }

    public static void prepareBuildingStash(String stashName) {
        if (stashName == null) {
            return;
        }
        Stash stash = StashSystem.getStash(stashName);
        if (stash != null && !alreadyReadMap.contains(stashName)) {
            alreadyReadMap.add(stashName);
            buildingsToDo.add(new StashBuilding(stash.name, stash.buildingX, stash.buildingY));
            RoomDef roomDef = IsoWorld.instance.getMetaGrid().getRoomAt(stash.buildingX, stash.buildingY, 0);
            if (roomDef != null && roomDef.getBuilding() != null && roomDef.getBuilding().isFullyStreamedIn()) {
                StashSystem.doBuildingStash(roomDef.getBuilding());
            }
        }
    }

    private static boolean checkSpecificSpawnProperties(Stash stash, InventoryItem item) {
        if (stash.spawnOnlyOnZed && (item.getContainer() == null || !(item.getContainer().getParent() instanceof IsoDeadBody))) {
            return false;
        }
        return (stash.minDayToSpawn <= -1 || GameTime.instance.getDaysSurvived() >= stash.minDayToSpawn) && (stash.maxDayToSpawn <= -1 || GameTime.instance.getDaysSurvived() <= stash.maxDayToSpawn);
    }

    private static void removeFromPossibleStash(Stash stash) {
        for (int i = 0; i < possibleStashes.size(); ++i) {
            StashBuilding possibleStash = possibleStashes.get(i);
            if (possibleStash.buildingX != stash.buildingX || possibleStash.buildingY != stash.buildingY) continue;
            possibleStashes.remove(i);
            --i;
        }
    }

    public static void doBuildingStash(BuildingDef def) {
        if (buildingsToDo == null) {
            StashSystem.init();
        }
        for (int i = 0; i < buildingsToDo.size(); ++i) {
            ItemPickerJava.ItemPickerRoom buildingLoot;
            StashBuilding stashBuilding = buildingsToDo.get(i);
            if (stashBuilding.buildingX <= def.x || stashBuilding.buildingX >= def.x2 || stashBuilding.buildingY <= def.y || stashBuilding.buildingY >= def.y2) continue;
            if (def.hasBeenVisited) {
                buildingsToDo.remove(i);
                --i;
                continue;
            }
            Stash stash = StashSystem.getStash(stashBuilding.stashName);
            if (stash == null || (buildingLoot = ItemPickerJava.rooms.get(stash.spawnTable)) == null) continue;
            def.setAllExplored(true);
            StashSystem.doSpecificBuildingProperties(stash, def);
            for (int x = def.x - 1; x < def.x2 + 1; ++x) {
                for (int y = def.y - 1; y < def.y2 + 1; ++y) {
                    for (int z = -32; z < 31; ++z) {
                        IsoGridSquare sq = IsoWorld.instance.currentCell.getGridSquare(x, y, z);
                        if (sq == null) continue;
                        for (int o = 0; o < sq.getObjects().size(); ++o) {
                            IsoObject obj = sq.getObjects().get(o);
                            if (obj.getContainer() != null && sq.getRoom() != null && sq.getRoom().getBuilding().getDef() == def && sq.getRoom().getName() != null && buildingLoot.containers.containsKey(obj.getContainer().getType())) {
                                ItemPickerJava.ItemPickerRoom originalLoot = ItemPickerJava.rooms.get(sq.getRoom().getName());
                                boolean clearIt = false;
                                if (originalLoot == null || !originalLoot.containers.containsKey(obj.getContainer().getType())) {
                                    obj.getContainer().clear();
                                    clearIt = true;
                                }
                                ItemPickerJava.fillContainerType(buildingLoot, obj.getContainer(), "", null);
                                ItemPickerJava.updateOverlaySprite(obj);
                                if (clearIt) {
                                    obj.getContainer().setExplored(true);
                                }
                            }
                            BarricadeAble barricadeAble = Type.tryCastTo(obj, BarricadeAble.class);
                            if (stash.barricades <= -1 || barricadeAble == null || !barricadeAble.isBarricadeAllowed() || Rand.Next(100) >= stash.barricades) continue;
                            if (obj instanceof IsoDoor) {
                                IsoDoor isoDoor = (IsoDoor)obj;
                                isoDoor.addRandomBarricades();
                                continue;
                            }
                            if (!(obj instanceof IsoWindow)) continue;
                            IsoWindow isoWindow = (IsoWindow)obj;
                            isoWindow.addRandomBarricades();
                        }
                    }
                }
            }
            buildingsToDo.remove(i);
            --i;
        }
    }

    private static void doSpecificBuildingProperties(Stash stash, BuildingDef def) {
        if (stash.containers != null) {
            ArrayList<RoomDef> possibleRooms = new ArrayList<RoomDef>();
            for (int i = 0; i < stash.containers.size(); ++i) {
                StashContainer stashCont = stash.containers.get(i);
                IsoGridSquare sq = null;
                if (!"all".equals(stashCont.room)) {
                    for (int j = 0; j < def.rooms.size(); ++j) {
                        RoomDef room = def.rooms.get(j);
                        if (!stashCont.room.equals(room.name)) continue;
                        possibleRooms.add(room);
                    }
                } else {
                    sq = stashCont.contX > -1 && stashCont.contY > -1 && stashCont.contZ > -1 ? IsoWorld.instance.getCell().getGridSquare(stashCont.contX, stashCont.contY, stashCont.contZ) : def.getFreeSquareInRoom();
                }
                if (!possibleRooms.isEmpty()) {
                    RoomDef room = (RoomDef)possibleRooms.get(Rand.Next(0, possibleRooms.size()));
                    sq = room.getFreeSquare();
                }
                if (sq != null) {
                    if (stashCont.containerItem != null && !stashCont.containerItem.isEmpty()) {
                        ItemPickerJava.ItemPickerRoom spawnTable = ItemPickerJava.rooms.get(stash.spawnTable);
                        if (spawnTable == null) {
                            DebugLog.log("Container distribution " + stash.spawnTable + " not found");
                            return;
                        }
                        Object item = InventoryItemFactory.CreateItem(stashCont.containerItem);
                        if (item == null) {
                            DebugLog.General.error("Item " + stashCont.containerItem + " Doesn't exist.");
                            return;
                        }
                        ItemPickerJava.ItemPickerContainer containerDist = spawnTable.containers.get(((InventoryItem)item).getType());
                        if (containerDist == null) {
                            DebugLog.General.error("ContainerDist " + ((InventoryItem)item).getType() + " Doesn't exist. (" + stash.spawnTable + ")");
                            return;
                        }
                        ItemPickerJava.rollContainerItem((InventoryContainer)item, null, containerDist);
                        ItemSpawner.spawnItem(item, sq, 0.0f, 0.0f, 0.0f);
                        continue;
                    }
                    IsoThumpable cont = new IsoThumpable(sq.getCell(), sq, stashCont.containerSprite, false, null);
                    cont.setIsThumpable(false);
                    cont.container = new ItemContainer(stashCont.containerType, sq, cont);
                    sq.AddSpecialObject(cont);
                    sq.RecalcAllWithNeighbours(true);
                    continue;
                }
                DebugLog.log("No free room was found to spawn special container for stash " + stash.name);
            }
        }
        if (stash.minTrapToSpawn > -1) {
            for (int i = stash.minTrapToSpawn; i < stash.maxTrapToSpawn; ++i) {
                IsoGridSquare sq = def.getFreeSquareInRoom();
                if (sq == null) continue;
                HandWeapon trap = (HandWeapon)InventoryItemFactory.CreateItem(possibleTrap.get(Rand.Next(0, possibleTrap.size())));
                IsoTrap isotrap = new IsoTrap(trap, sq.getCell(), sq);
                sq.AddTileObject(isotrap);
                if (!GameServer.server) continue;
                isotrap.transmitCompleteItemToClients();
            }
        }
        if (stash.zombies > -1) {
            for (int i = 0; i < def.rooms.size(); ++i) {
                RoomDef room = def.rooms.get(i);
                if (!IsoWorld.getZombiesEnabled()) continue;
                boolean min = true;
                int zedInRoom = 0;
                for (int j = 0; j < room.area; ++j) {
                    if (Rand.Next(100) >= stash.zombies) continue;
                    ++zedInRoom;
                }
                if (SandboxOptions.instance.zombies.getValue() == 1) {
                    zedInRoom += 4;
                } else if (SandboxOptions.instance.zombies.getValue() == 2) {
                    zedInRoom += 3;
                } else if (SandboxOptions.instance.zombies.getValue() == 3) {
                    zedInRoom += 2;
                } else if (SandboxOptions.instance.zombies.getValue() == 5) {
                    zedInRoom -= 4;
                }
                if (zedInRoom > room.area / 2) {
                    zedInRoom = room.area / 2;
                }
                if (zedInRoom < 1) {
                    zedInRoom = 1;
                }
                ArrayList<IsoZombie> zombies = VirtualZombieManager.instance.addZombiesToMap(zedInRoom, room, false);
                ZombieSpawnRecorder.instance.record(zombies, "StashSystem");
            }
        }
    }

    public static Stash getStash(String stashName) {
        for (int i = 0; i < allStashes.size(); ++i) {
            Stash stash = allStashes.get(i);
            if (!stash.name.equals(stashName)) continue;
            return stash;
        }
        return null;
    }

    public static void visitedBuilding(BuildingDef def) {
        if (GameClient.client) {
            return;
        }
        for (int i = 0; i < possibleStashes.size(); ++i) {
            StashBuilding stash = possibleStashes.get(i);
            if (stash.buildingX <= def.x || stash.buildingX >= def.x2 || stash.buildingY <= def.y || stash.buildingY >= def.y2) continue;
            possibleStashes.remove(i);
            --i;
        }
    }

    public static void load(ByteBuffer input, int worldVersion) {
        StashSystem.init();
        alreadyReadMap = new ArrayList();
        possibleStashes = new ArrayList();
        buildingsToDo = new ArrayList();
        int nPossibleStash = input.getInt();
        for (int i = 0; i < nPossibleStash; ++i) {
            possibleStashes.add(new StashBuilding(GameWindow.ReadString(input), input.getInt(), input.getInt()));
        }
        int nBuildingsToDo = input.getInt();
        for (int i = 0; i < nBuildingsToDo; ++i) {
            buildingsToDo.add(new StashBuilding(GameWindow.ReadString(input), input.getInt(), input.getInt()));
        }
        int nAlreadyReadMap = input.getInt();
        for (int i = 0; i < nAlreadyReadMap; ++i) {
            alreadyReadMap.add(GameWindow.ReadString(input));
        }
    }

    public static void save(ByteBuffer output) {
        StashBuilding stashBuilding;
        int i;
        if (allStashes == null) {
            return;
        }
        output.putInt(possibleStashes.size());
        for (i = 0; i < possibleStashes.size(); ++i) {
            stashBuilding = possibleStashes.get(i);
            GameWindow.WriteString(output, stashBuilding.stashName);
            output.putInt(stashBuilding.buildingX);
            output.putInt(stashBuilding.buildingY);
        }
        output.putInt(buildingsToDo.size());
        for (i = 0; i < buildingsToDo.size(); ++i) {
            stashBuilding = buildingsToDo.get(i);
            GameWindow.WriteString(output, stashBuilding.stashName);
            output.putInt(stashBuilding.buildingX);
            output.putInt(stashBuilding.buildingY);
        }
        output.putInt(alreadyReadMap.size());
        for (i = 0; i < alreadyReadMap.size(); ++i) {
            GameWindow.WriteString(output, alreadyReadMap.get(i));
        }
    }

    public static ArrayList<StashBuilding> getPossibleStashes() {
        return possibleStashes;
    }

    public static void reinit() {
        possibleStashes = null;
        alreadyReadMap = new ArrayList();
        StashSystem.init();
    }

    public static void Reset() {
        allStashes = null;
        possibleStashes = null;
        buildingsToDo = null;
        possibleTrap.clear();
        alreadyReadMap.clear();
    }

    public static boolean isStashBuilding(BuildingDef def) {
        StashBuilding stashBuilding;
        int i;
        if (possibleStashes != null) {
            for (i = 0; i < possibleStashes.size(); ++i) {
                stashBuilding = possibleStashes.get(i);
                if (stashBuilding.buildingX <= def.x || stashBuilding.buildingX >= def.x2 || stashBuilding.buildingY <= def.y || stashBuilding.buildingY >= def.y2) continue;
                return true;
            }
        }
        if (buildingsToDo != null) {
            for (i = 0; i < buildingsToDo.size(); ++i) {
                stashBuilding = buildingsToDo.get(i);
                if (stashBuilding.buildingX <= def.x || stashBuilding.buildingX >= def.x2 || stashBuilding.buildingY <= def.y || stashBuilding.buildingY >= def.y2) continue;
                return true;
            }
        }
        return false;
    }

    static {
        possibleTrap = new ArrayList();
        alreadyReadMap = new ArrayList();
    }
}

