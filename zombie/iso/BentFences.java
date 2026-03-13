/*
 * Decompiled with CFR 0.152.
 */
package zombie.iso;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import se.krka.kahlua.j2se.KahluaTableImpl;
import se.krka.kahlua.vm.KahluaTable;
import se.krka.kahlua.vm.KahluaTableIterator;
import zombie.MapCollisionData;
import zombie.SandboxOptions;
import zombie.SoundManager;
import zombie.UsedFromLua;
import zombie.characters.IsoPlayer;
import zombie.characters.IsoZombie;
import zombie.core.math.PZMath;
import zombie.core.random.Rand;
import zombie.inventory.InventoryItem;
import zombie.inventory.ItemContainer;
import zombie.iso.BrokenFences;
import zombie.iso.IsoCell;
import zombie.iso.IsoDirections;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoMovingObject;
import zombie.iso.IsoObject;
import zombie.iso.IsoWorld;
import zombie.iso.SpriteDetails.IsoFlagType;
import zombie.iso.areas.isoregion.IsoRegions;
import zombie.iso.sprite.IsoSprite;
import zombie.iso.sprite.IsoSpriteManager;
import zombie.network.GameServer;
import zombie.pathfind.PolygonalMap2;
import zombie.scripting.objects.ResourceLocation;
import zombie.scripting.objects.SoundKey;
import zombie.util.Type;
import zombie.util.list.PZArrayUtil;

@UsedFromLua
public class BentFences {
    private static final BentFences instance = new BentFences();
    private final ArrayList<Entry> entries = new ArrayList();
    private final HashSet<String> collapsedTiles = new HashSet();
    private final HashSet<String> debrisTiles = new HashSet();
    private final HashMap<String, ArrayList<Entry>> fenceMap = new HashMap();

    public static BentFences getInstance() {
        return instance;
    }

    private ArrayList<String> tableToTiles(KahluaTableImpl tiles, ArrayList<String> result) {
        if (tiles == null) {
            return result;
        }
        KahluaTableIterator it = tiles.iterator();
        while (it.advance()) {
            result.add(it.getValue().toString());
        }
        return result;
    }

    private ArrayList<String> tableToTiles(KahluaTable table, String key) {
        ArrayList<String> tiles = new ArrayList<String>();
        return this.tableToTiles((KahluaTableImpl)table.rawget(key), tiles);
    }

    public void addFenceTiles(int version, KahluaTableImpl tiles) {
        KahluaTableIterator it = tiles.iterator();
        while (it.advance()) {
            KahluaTableImpl value = (KahluaTableImpl)it.getValue();
            Entry entry = new Entry();
            entry.dir = IsoDirections.valueOf(value.rawgetStr("dir"));
            entry.health = value.rawgetInt("health") != -1 ? value.rawgetInt("health") : 100;
            entry.collapsedOffset = value.rawgetInt("collapsedOffset");
            entry.collapsedSizeX = value.rawgetInt("collapsedSizeX");
            entry.collapsedSizeY = value.rawgetInt("collapsedSizeY");
            entry.doSmash = value.rawgetBool("doSmash");
            KahluaTableImpl stageTable = (KahluaTableImpl)value.rawget("stages");
            KahluaTableImpl collapsedTable = (KahluaTableImpl)value.rawget("collapsed");
            KahluaTableImpl debrisTable = (KahluaTableImpl)value.rawget("debris");
            if (stageTable == null) continue;
            KahluaTableIterator stageit = stageTable.iterator();
            while (stageit.advance()) {
                KahluaTableImpl nextStage = (KahluaTableImpl)stageit.getValue();
                entry.stages.put(nextStage.rawgetInt("stage"), this.tableToTiles((KahluaTable)nextStage, "tiles"));
            }
            if (collapsedTable != null) {
                PZArrayUtil.addAll(entry.collapsed, this.tableToTiles((KahluaTable)value, "collapsed"));
                if (entry.collapsedSizeX * entry.collapsedSizeY != entry.collapsed.size()) continue;
                this.collapsedTiles.addAll(entry.collapsed);
            }
            if (debrisTable != null) {
                PZArrayUtil.addAll(entry.debris, this.tableToTiles((KahluaTable)value, "debris"));
                this.debrisTiles.addAll(entry.debris);
            }
            if (entry.stages.isEmpty()) continue;
            entry.length = entry.stages.get(0).size();
            this.entries.add(entry);
            for (int i = 0; i < entry.stages.size(); ++i) {
                ArrayList<String> sprites = entry.stages.get(i);
                for (String spriteName : sprites) {
                    ArrayList<Entry> entries = this.fenceMap.get(spriteName);
                    if (entries == null) {
                        entries = new ArrayList();
                        this.fenceMap.put(spriteName, entries);
                    }
                    entries.add(entry);
                }
            }
        }
    }

    public boolean isBentObject(IsoObject obj) {
        return this.getBendStage(obj, this.getEntryForObject(obj)) > 0;
    }

    public boolean isUnbentObject(IsoObject obj) {
        Entry entry = this.getEntryForObject(obj);
        if (entry == null) {
            return false;
        }
        return this.getBendStage(obj, entry) < entry.stages.size() - 1;
    }

    public boolean isUnbentObject(IsoObject obj, IsoDirections dir) {
        Entry entry = this.getEntryForObject(obj);
        if (entry == null || entry.isNorth() && dir != IsoDirections.N && dir != IsoDirections.S) {
            return false;
        }
        return this.getBendStage(obj, entry) < entry.stages.size() - 1;
    }

    private Entry getEntryForObject(IsoObject obj) {
        return this.getEntryForObject(obj, null);
    }

    private Entry getEntryForObject(IsoObject obj, IsoDirections dir) {
        if (obj == null || obj.sprite == null || obj.sprite.name == null) {
            return null;
        }
        if (!obj.getProperties().has(IsoFlagType.collideN) && !obj.getProperties().has(IsoFlagType.collideW)) {
            boolean bl = true;
        }
        ArrayList<Entry> entries = this.fenceMap.get(obj.sprite.name);
        Entry matched = null;
        if (entries != null) {
            for (int i = 0; i < entries.size(); ++i) {
                Entry entry = entries.get(i);
                if (dir != null && entry.dir != dir) continue;
                if (dir == null) {
                    if (!this.isValidObject(obj, entry) || matched != null && matched.length >= entry.length) continue;
                    matched = entry;
                    continue;
                }
                if (!this.isValidObject(obj, entry) || this.getBendStage(obj, entry) >= entry.stages.size() && !entry.collapsed.isEmpty() && !this.checkCanCollapse(obj, dir, entry) || matched != null && matched.length >= entry.length) continue;
                matched = entry;
            }
        }
        return matched;
    }

    private int getBendStage(IsoObject obj, Entry entry) {
        if (obj == null || obj.sprite == null || obj.sprite.name == null || entry == null) {
            return -1;
        }
        for (int i = 0; i < entry.stages.size(); ++i) {
            if (!entry.stages.get(i).contains(obj.sprite.name)) continue;
            return i;
        }
        return -1;
    }

    private int getTileIndex(ArrayList<String> tiles, IsoObject obj, Entry entry) {
        return this.getTileIndex(tiles, obj, entry, false);
    }

    private int getTileIndex(ArrayList<String> tiles, IsoObject obj, Entry entry, boolean forceFirst) {
        int index = -1;
        int lastIndex = -1;
        String spriteName = obj.getSpriteName();
        int centerTile = tiles.size() / 2;
        if (spriteName != null) {
            for (int i = 0; i < tiles.size(); ++i) {
                if (!tiles.get(i).equals(spriteName) || !this.isValidSpan(tiles, obj, entry, i)) continue;
                if (forceFirst || i == centerTile) {
                    return i;
                }
                lastIndex = index;
                index = i;
            }
        }
        return this.getBendStage(obj, entry) == 0 ? (lastIndex == -1 ? index : lastIndex) : index;
    }

    private boolean isValidSpan(ArrayList<String> tiles, IsoObject obj, Entry entry, int index) {
        IsoCell cell = IsoWorld.instance.currentCell;
        for (int i = 0; i < tiles.size(); ++i) {
            int y;
            int x = obj.square.x + (entry.isNorth() ? i - index : 0);
            IsoGridSquare square = cell.getGridSquare(x, y = obj.square.y + (entry.isNorth() ? 0 : i - index), obj.square.z);
            if (square == null) {
                return false;
            }
            if (index == i || this.getObjectForEntry(square, tiles, i) != null) continue;
            return false;
        }
        return true;
    }

    private boolean isValidObject(IsoObject obj, Entry entry) {
        int bendStage = this.getBendStage(obj, entry);
        if (bendStage == -1) {
            return false;
        }
        ArrayList<String> tiles = entry.stages.get(bendStage);
        return this.getTileIndex(tiles, obj, entry) != -1;
    }

    IsoObject getObjectForEntry(IsoGridSquare square, ArrayList<String> tiles, int index) {
        for (int i = 0; i < square.getObjects().size(); ++i) {
            IsoObject obj = square.getObjects().get(i);
            if (obj.sprite == null || obj.sprite.name == null || !tiles.get(index).equals(obj.sprite.name)) continue;
            return obj;
        }
        return null;
    }

    public boolean checkCanCollapse(IsoObject obj, IsoDirections dir, Entry entry) {
        if (entry.collapsed.isEmpty()) {
            return false;
        }
        IsoCell cell = IsoWorld.instance.currentCell;
        int bendStage = this.getBendStage(obj, entry);
        ArrayList<String> tiles = entry.stages.get(bendStage);
        int index = this.getTileIndex(tiles, obj, entry);
        boolean north = entry.isNorth();
        int revX = dir == IsoDirections.W ? -1 : 1;
        int revY = dir == IsoDirections.N ? -1 : 1;
        int offXY = entry.collapsedOffset;
        int offsetX = obj.square.x + (north ? offXY - index : (dir == IsoDirections.W ? -1 : 0));
        int offsetY = obj.square.y + (north ? (dir == IsoDirections.N ? -1 : 0) : offXY - index);
        for (int x = 0; x < entry.collapsedSizeX; ++x) {
            for (int y = 0; y < entry.collapsedSizeY; ++y) {
                IsoGridSquare square = cell.getGridSquare(offsetX + x * revX, offsetY + y * revY, obj.square.z);
                if (square == null || !square.TreatAsSolidFloor()) {
                    return false;
                }
                for (int i = square.getObjects().size() - 1; i > 0; --i) {
                    IsoObject isoObject = square.getObjects().get(i);
                    if (!(isoObject instanceof IsoObject)) continue;
                    IsoObject object = isoObject;
                    if (object.isStairsObject()) {
                        return false;
                    }
                    if (!object.isWall() || !BentFences.isWallBlockingObjectOnTile(dir, entry, x, y, object)) continue;
                    return false;
                }
            }
        }
        return true;
    }

    private static boolean isWallBlockingObjectOnTile(IsoDirections dir, Entry entry, int x, int y, IsoObject object) {
        if (dir == IsoDirections.E) {
            return x != 0;
        }
        if (dir == IsoDirections.S) {
            return y != 0;
        }
        if (dir == IsoDirections.W) {
            if (x + 1 == entry.collapsedSizeX) {
                return !object.isWallW();
            }
        } else if (dir == IsoDirections.N && y + 1 == entry.collapsedSizeY) {
            return !object.isWallN();
        }
        return true;
    }

    private void emptyContainerIfPresent(IsoObject object, IsoGridSquare square) {
        ItemContainer contents = object.getContainer();
        if (contents != null && !contents.isEmpty()) {
            for (int j = 0; j < contents.getItems().size(); ++j) {
                InventoryItem item = contents.getItems().get(j);
                if (item == null) continue;
                square.AddWorldInventoryItem(item, Rand.Next(0.0f, 0.5f), Rand.Next(0.0f, 0.5f), 0.0f);
            }
        }
    }

    public void collapse(IsoObject obj, IsoDirections dir, Entry entry, int index) {
        int i;
        Object square;
        IsoCell cell = IsoWorld.instance.currentCell;
        boolean north = entry.isNorth();
        int revX = dir == IsoDirections.W ? -1 : 1;
        int revY = dir == IsoDirections.N ? -1 : 1;
        int offXY = entry.collapsedOffset;
        int offsetX = obj.square.x + (north ? offXY - index : (dir == IsoDirections.W ? -1 : 0));
        int offsetY = obj.square.y + (north ? (dir == IsoDirections.N ? -1 : 0) : offXY - index);
        Iterator<String> collapsed = entry.collapsed.iterator();
        ArrayList<IsoMovingObject> tripZombies = new ArrayList<IsoMovingObject>();
        ArrayList<IsoMovingObject> crushZombies = new ArrayList<IsoMovingObject>();
        for (int x = 0; x < entry.collapsedSizeX; ++x) {
            for (int y = 0; y < entry.collapsedSizeY; ++y) {
                square = cell.getGridSquare(offsetX + x * revX, offsetY + y * revY, obj.square.z);
                if (square == null) continue;
                if (north && y == 0 || !north && x == 0) {
                    int xOff = 0;
                    int yOff = 0;
                    switch (dir) {
                        case N: {
                            yOff = 1;
                            break;
                        }
                        case S: {
                            yOff = -1;
                            break;
                        }
                        case E: {
                            xOff = -1;
                            break;
                        }
                        case W: {
                            xOff = 1;
                        }
                    }
                    IsoGridSquare square2 = cell.getGridSquare(offsetX + xOff, offsetY + yOff, obj.square.z);
                    if (square2 != null) {
                        tripZombies.addAll(square2.getMovingObjects());
                    }
                }
                crushZombies.addAll(((IsoGridSquare)square).getMovingObjects());
                for (int i2 = ((IsoGridSquare)square).getObjects().size() - 1; i2 > 0; --i2) {
                    if (BrokenFences.getInstance().isBreakableObject(((IsoGridSquare)square).getObjects().get(i2))) {
                        BrokenFences.getInstance().destroyFence(((IsoGridSquare)square).getObjects().get(i2), dir);
                        continue;
                    }
                    IsoObject object = Type.tryCastTo(((IsoGridSquare)square).getObjects().get(i2), IsoObject.class);
                    if (object == null || object.isWall() || object.isFloor() || object.sprite == null || object.sprite.name == null || this.fenceMap.get(object.sprite.name) != null || this.collapsedTiles.contains(object.sprite.name)) continue;
                    this.emptyContainerIfPresent(object, (IsoGridSquare)square);
                    ((IsoGridSquare)square).transmitRemoveItemFromSquare(object);
                    BrokenFences.getInstance().addItems(object, (IsoGridSquare)square);
                }
                String spriteName = collapsed.next();
                IsoSprite sprite = IsoSpriteManager.instance.getSprite(spriteName);
                sprite.name = spriteName;
                IsoObject collapsedObj = IsoObject.getNew((IsoGridSquare)square, spriteName, null, false);
                ((IsoGridSquare)square).transmitAddObjectToSquare(collapsedObj, ((IsoGridSquare)square).getObjects().size() + 1);
                BentFences.doSquareUpdateTasks((IsoGridSquare)square);
            }
        }
        for (i = 0; i < tripZombies.size(); ++i) {
            IsoZombie zombie;
            square = tripZombies.get(i);
            if (!(square instanceof IsoZombie) || (zombie = (IsoZombie)square).isObjectBehind(obj)) continue;
            zombie.setOnFloor(true);
            zombie.setFallOnFront(true);
            zombie.setHitReaction("Floor");
        }
        for (i = 0; i < crushZombies.size(); ++i) {
            square = crushZombies.get(i);
            if (square instanceof IsoZombie) {
                IsoZombie zombie = (IsoZombie)square;
                zombie.setOnFloor(true);
                zombie.setFallOnFront(zombie.isObjectBehind(obj));
                zombie.setHitReaction("Floor");
                continue;
            }
            IsoPlayer player = Type.tryCastTo((IsoMovingObject)crushZombies.get(i), IsoPlayer.class);
            if (player == null) continue;
            player.setForceSprint(false);
            player.setBumpType("left");
            player.setVariable("BumpDone", false);
            player.setVariable("BumpFall", true);
            player.setBumpFallType(player.isObjectBehind(obj) ? "pushedBehind" : "pushedFront");
            player.setVariable("TripObstacleType", 1.0f);
        }
    }

    public void removeCollapsedTiles(IsoObject obj, IsoDirections dir, Entry entry, int index) {
        IsoCell cell = IsoWorld.instance.currentCell;
        boolean north = entry.isNorth();
        int revX = dir == IsoDirections.W ? -1 : 1;
        int revY = dir == IsoDirections.N ? -1 : 1;
        int offXY = entry.collapsedOffset;
        int offsetX = obj.square.x + (north ? offXY - index : (dir == IsoDirections.W ? -1 : 0));
        int offsetY = obj.square.y + (north ? (dir == IsoDirections.N ? -1 : 0) : offXY - index);
        for (int x = 0; x < entry.collapsedSizeX; ++x) {
            for (int y = 0; y < entry.collapsedSizeY; ++y) {
                IsoGridSquare square = cell.getGridSquare(offsetX + x * revX, offsetY + y * revY, obj.square.z);
                if (square == null) continue;
                for (int i = square.getObjects().size() - 1; i > 0; --i) {
                    IsoObject object = Type.tryCastTo(square.getObjects().get(i), IsoObject.class);
                    if (object != null && object.sprite != null && object.sprite.name != null && this.collapsedTiles.contains(object.sprite.name)) {
                        square.transmitRemoveItemFromSquare(object);
                    }
                    BentFences.doSquareUpdateTasks(square);
                }
            }
        }
    }

    public void smashFence(IsoObject obj, IsoDirections dir) {
        this.smashFence(obj, dir, -1);
    }

    public void smashFence(IsoObject obj, IsoDirections dir, int index) {
        int useIndex;
        Entry entry = this.getEntryForObject(obj);
        if (entry == null) {
            return;
        }
        ArrayList<String> tiles = entry.stages.get(0);
        if (tiles == null) {
            return;
        }
        int bendStage = this.getBendStage(obj, entry);
        int n = useIndex = index != -1 ? index : this.getTileIndex(tiles, obj, entry, true);
        if (bendStage != 0) {
            this.resetFence(obj);
        }
        IsoCell cell = IsoWorld.instance.currentCell;
        for (int i = 1; i < tiles.size() - 1; ++i) {
            IsoObject fenceObj;
            int y;
            int x = obj.square.x + (entry.isNorth() ? i - useIndex : 0);
            IsoGridSquare square = cell.getGridSquare(x, y = obj.square.y + (entry.isNorth() ? 0 : i - useIndex), obj.square.z);
            if (square == null || (fenceObj = this.getObjectForEntry(square, tiles, i)) == null && (fenceObj = BrokenFences.getInstance().getBreakableObject(square, entry.isNorth())) == null) continue;
            String spriteName = tiles.get(i);
            IsoSprite sprite = IsoSpriteManager.instance.getSprite(spriteName);
            sprite.name = spriteName;
            fenceObj.setSprite(sprite);
            fenceObj.transmitUpdatedSprite();
            if (spriteName != null && BrokenFences.getInstance().isBreakableSprite(spriteName)) {
                BrokenFences.getInstance().destroyFence(fenceObj, dir);
                continue;
            }
            BrokenFences.getInstance().addItems(fenceObj, square);
            square.transmitRemoveItemFromSquare(fenceObj);
            BentFences.doSquareUpdateTasks(square);
        }
    }

    public void swapTiles(IsoObject obj, IsoDirections dir, boolean bending) {
        this.swapTiles(obj, dir, bending, -1);
    }

    public void swapTiles(IsoObject obj, IsoDirections dir, boolean bending, int forceStage) {
        int i;
        boolean doDebris;
        int bendStage;
        Entry entry = this.getEntryForObject(obj, dir);
        if (entry == null) {
            if (bending) {
                entry = this.getEntryForObject(obj);
                if (entry == null) {
                    return;
                }
            } else {
                return;
            }
        }
        if (dir != null) {
            if (entry.isNorth() && dir != IsoDirections.N && dir != IsoDirections.S) {
                return;
            }
            if (!entry.isNorth() && dir != IsoDirections.W && dir != IsoDirections.E) {
                return;
            }
        }
        int n = bendStage = forceStage != -1 ? forceStage : this.getBendStage(obj, entry);
        if (bendStage == -1 || !bending && bendStage == 0) {
            return;
        }
        boolean doSmash = bending && bendStage > 0 && entry.dir != dir;
        boolean doCollapse = !doSmash && bending && bendStage + 1 >= entry.stages.size() - 1;
        boolean doSmashCollapse = doCollapse && !this.checkCanCollapse(obj, dir, entry) && entry.doSmash;
        boolean doRemoveCollapsedTiles = !bending && bendStage + 1 == entry.stages.size();
        boolean bl = doDebris = doCollapse && !entry.debris.isEmpty();
        if (bendStage == entry.stages.size() && bending) {
            return;
        }
        ArrayList<String> tiles = entry.stages.get(bendStage);
        ArrayList<String> baseTiles = entry.stages.get(0);
        ArrayList<String> swapTiles = entry.stages.get(bendStage + (bending ? 1 : -1));
        if (tiles == null || swapTiles == null || baseTiles == null || tiles.size() != swapTiles.size()) {
            return;
        }
        IsoCell cell = IsoWorld.instance.currentCell;
        int index = this.getTileIndex(tiles, obj, entry);
        ArrayList<IsoGridSquare> squaresToUpdate = new ArrayList<IsoGridSquare>();
        for (i = 0; i < tiles.size(); ++i) {
            IsoObject fenceObject;
            int y;
            int x = obj.square.x + (entry.isNorth() ? i - index : 0);
            IsoGridSquare square = cell.getGridSquare(x, y = obj.square.y + (entry.isNorth() ? 0 : i - index), obj.square.z);
            if (square == null || (fenceObject = this.getObjectForEntry(square, tiles, i)) == null) continue;
            if (fenceObject.getProperties().has("CornerNorthWall")) {
                BentFences.splitCorner(fenceObject, square, entry);
            }
            if (bending) {
                for (int j = square.getObjects().size() - 1; j > 0; --j) {
                    IsoObject testObj = square.getObjects().get(j);
                    if (testObj == null || testObj.sprite == null) continue;
                    boolean isSign = false;
                    if (testObj.sprite.getProperties().has(IsoFlagType.attachedN)) {
                        isSign = true;
                    }
                    if (testObj.sprite.getProperties().has(IsoFlagType.attachedW)) {
                        isSign = true;
                    }
                    if (testObj.sprite.getProperties().has(IsoFlagType.attachedS)) {
                        isSign = true;
                    }
                    if (testObj.sprite.getProperties().has(IsoFlagType.attachedE)) {
                        isSign = true;
                    }
                    if (!isSign) continue;
                    square.transmitRemoveItemFromSquare(testObj);
                    BrokenFences.getInstance().addItems(testObj, square);
                    square.invalidateRenderChunkLevel(384L);
                }
            }
            String spriteName = doSmash || doSmashCollapse ? baseTiles.get(i) : swapTiles.get(i);
            IsoSprite sprite = IsoSpriteManager.instance.getSprite(spriteName);
            sprite.name = spriteName;
            fenceObject.setSprite(sprite);
            fenceObject.transmitUpdatedSprite();
            if (doDebris) {
                this.addDebrisObject(fenceObject, dir, entry);
            }
            squaresToUpdate.add(square);
            fenceObject.setDamage((short)entry.health);
        }
        for (i = 0; i < squaresToUpdate.size(); ++i) {
            BentFences.doSquareUpdateTasks((IsoGridSquare)squaresToUpdate.get(i));
        }
        if (dir != null) {
            if (doSmash || doSmashCollapse) {
                this.smashFence(obj, dir, index);
            } else if (doCollapse) {
                this.collapse(obj, dir, entry, index);
            }
        } else if (doRemoveCollapsedTiles) {
            this.removeCollapsedTiles(obj, entry.dir, entry, index);
        }
    }

    private static void splitCorner(IsoObject obj, IsoGridSquare square, Entry entry) {
        String cornerNorth = obj.getProperties().get("CornerNorthWall");
        String cornerWest = obj.getProperties().get("CornerWestWall");
        if (cornerNorth == null || cornerWest == null) {
            return;
        }
        IsoSprite sprite = IsoSpriteManager.instance.getSprite(entry.isNorth() ? cornerNorth : cornerWest);
        sprite.name = entry.isNorth() ? cornerNorth : cornerWest;
        obj.setSprite(sprite);
        obj.transmitUpdatedSprite();
        IsoObject obj2 = IsoObject.getNew(square, entry.isNorth() ? cornerWest : cornerNorth, null, false);
        square.transmitAddObjectToSquare(obj2, square.getObjects().size() + 1);
        BentFences.doSquareUpdateTasks(square);
    }

    public void bendFence(IsoObject obj, IsoDirections dir) {
        Entry entry = this.getEntryForObject(obj, dir);
        if (!this.isValidObject(obj, entry)) {
            switch (dir) {
                case N: {
                    entry = this.getEntryForObject(obj, IsoDirections.W);
                    dir = IsoDirections.W;
                    break;
                }
                case S: {
                    entry = this.getEntryForObject(obj, IsoDirections.E);
                    dir = IsoDirections.E;
                    break;
                }
                case W: {
                    entry = this.getEntryForObject(obj, IsoDirections.N);
                    dir = IsoDirections.N;
                    break;
                }
                case E: {
                    entry = this.getEntryForObject(obj, IsoDirections.S);
                    dir = IsoDirections.S;
                }
            }
        }
        if (entry == null) {
            return;
        }
        int bendStageOld = this.getBendStage(obj, entry);
        SoundKey thumpSound = SoundKey.get(ResourceLocation.of(obj.getProperties().get("ThumpSound")));
        this.swapTiles(obj, dir, true);
        int bendStageNew = this.getBendStage(obj, this.getEntryForObject(obj));
        if (bendStageNew <= bendStageOld) {
            return;
        }
        SoundKey soundName = SoundKey.BREAK_OBJECT;
        if (SoundKey.ZOMBIE_THUMP_CHAINLINK_FENCE == thumpSound) {
            soundName = bendStageNew == 1 ? SoundKey.ZOMBIE_THUMP_CHAINLINK_FENCE_DAMAGE_LOW : (bendStageNew < entry.stages.size() - 1 ? SoundKey.ZOMBIE_THUMP_CHAINLINK_FENCE_DAMAGE_HIGH : SoundKey.ZOMBIE_THUMP_CHAINLINK_FENCE_DAMAGE_COLLAPSE);
        } else if (SoundKey.ZOMBIE_THUMP_METAL_POLE_FENCE == thumpSound) {
            soundName = bendStageNew == 1 ? SoundKey.ZOMBIE_THUMP_METAL_POLE_FENCE_DAMAGE_LOW : (bendStageNew < entry.stages.size() - 1 ? SoundKey.ZOMBIE_THUMP_METAL_POLE_FENCE_DAMAGE_HIGH : SoundKey.ZOMBIE_THUMP_METAL_POLE_FENCE_DAMAGE_COLLAPSE);
        } else if (SoundKey.ZOMBIE_THUMP_WOOD == thumpSound && bendStageNew >= entry.stages.size()) {
            soundName = SoundKey.ZOMBIE_THUMP_WOOD_COLLAPSE;
        }
        IsoGridSquare square = obj.getSquare();
        if (GameServer.server) {
            GameServer.PlayWorldSoundServer(soundName.toString(), false, square, 1.0f, 20.0f, 1.0f, true);
        } else {
            SoundManager.instance.PlayWorldSound(soundName.toString(), square, 1.0f, 20.0f, 1.0f, true);
        }
    }

    public void unbendFence(IsoObject obj) {
        this.swapTiles(obj, null, false);
    }

    public void resetFence(IsoObject obj) {
        Entry entry = this.getEntryForObject(obj);
        if (entry == null) {
            return;
        }
        int bendStage = this.getBendStage(obj, entry);
        if (bendStage == -1) {
            return;
        }
        ArrayList<String> tiles = entry.stages.get(bendStage);
        ArrayList<String> baseTiles = entry.stages.get(0);
        if (tiles == null || baseTiles == null || tiles.size() != baseTiles.size()) {
            return;
        }
        IsoCell cell = IsoWorld.instance.currentCell;
        int index = this.getTileIndex(tiles, obj, entry);
        for (int i = 0; i < tiles.size(); ++i) {
            IsoObject fenceObject;
            int y;
            int x = obj.square.x + (entry.isNorth() ? i - index : 0);
            IsoGridSquare square = cell.getGridSquare(x, y = obj.square.y + (entry.isNorth() ? 0 : i - index), obj.square.z);
            if (square == null || (fenceObject = this.getObjectForEntry(square, tiles, i)) == null) continue;
            if (fenceObject.getProperties().has("CornerNorthWall")) {
                BentFences.splitCorner(fenceObject, square, entry);
            }
            String spriteName = baseTiles.get(i);
            IsoSprite sprite = IsoSpriteManager.instance.getSprite(spriteName);
            sprite.name = spriteName;
            fenceObject.setSprite(sprite);
            fenceObject.transmitUpdatedSprite();
            BentFences.doSquareUpdateTasks(square);
            fenceObject.setDamage((short)entry.health);
        }
        if (bendStage + 1 == entry.stages.size()) {
            this.removeCollapsedTiles(obj, entry.dir, entry, index);
        }
    }

    public boolean isBendableFence(IsoObject obj) {
        return this.isBentObject(obj) || this.isUnbentObject(obj);
    }

    public ThumpData getThumpData(IsoObject obj) {
        Entry entry = this.getEntryForObject(obj);
        if (entry == null) {
            return new ThumpData();
        }
        return this.getThumpData(obj, entry);
    }

    public ThumpData getThumpData(IsoObject obj, Entry entry) {
        ThumpData thumpData = new ThumpData();
        thumpData.health = entry.health;
        int n = 0;
        int s = 0;
        int e = 0;
        int w = 0;
        IsoCell cell = IsoWorld.instance.currentCell;
        int bendStage = this.getBendStage(obj, entry);
        ArrayList<String> tiles = entry.stages.get(bendStage);
        thumpData.bendStage = bendStage;
        thumpData.stages = entry.stages.size();
        if (!this.isEnabled()) {
            thumpData.thumpersToDamage = -1;
            thumpData.damageMultiplier = BentFences.getFenceDamageMultiplier();
            return thumpData;
        }
        int thumpersToDamage = BentFences.getThumpersRequired();
        if (bendStage > 0) {
            thumpData.directionBent = entry.dir;
            thumpersToDamage = PZMath.max(BentFences.getThumpersRequired() / (bendStage + 1), 1);
        }
        thumpData.thumpersToDamage = thumpersToDamage;
        thumpData.damageMultiplier = BentFences.getFenceDamageMultiplier();
        int index = this.getTileIndex(tiles, obj, entry);
        for (int i = 0; i < tiles.size(); ++i) {
            int y;
            int x = obj.square.x + (entry.isNorth() ? i - index : 0);
            IsoGridSquare square = cell.getGridSquare(x, y = obj.square.y + (entry.isNorth() ? 0 : i - index), obj.square.z);
            if (square == null) continue;
            if (entry.isNorth()) {
                thumpData.totalThumpers += square.getZombieCount();
                s += square.getZombieCount();
                IsoGridSquare nSquare = cell.getGridSquare(x, y - 1, obj.square.z);
                if (nSquare != null) {
                    n += nSquare.getZombieCount();
                    thumpData.totalThumpers += nSquare.getZombieCount();
                    if (nSquare.getN() != null) {
                        n += nSquare.getN().getZombieCount();
                        thumpData.totalThumpers += nSquare.getN().getZombieCount();
                    }
                }
                if (square.getS() != null) {
                    s += square.getS().getZombieCount();
                    thumpData.totalThumpers += square.getS().getZombieCount();
                }
                thumpData.directionToBend = n > s ? IsoDirections.S : IsoDirections.N;
                continue;
            }
            thumpData.totalThumpers += square.getZombieCount();
            e += square.getZombieCount();
            IsoGridSquare wSquare = cell.getGridSquare(x - 1, y, obj.square.z);
            if (wSquare != null) {
                w += wSquare.getZombieCount();
                thumpData.totalThumpers += wSquare.getZombieCount();
                if (wSquare.getW() != null) {
                    w += wSquare.getW().getZombieCount();
                    thumpData.totalThumpers += wSquare.getW().getZombieCount();
                }
            }
            if (square.getE() != null) {
                e += square.getE().getZombieCount();
                thumpData.totalThumpers += square.getE().getZombieCount();
            }
            thumpData.directionToBend = e > w ? IsoDirections.W : IsoDirections.E;
        }
        return thumpData;
    }

    public IsoObject getCollapsedFence(IsoGridSquare square) {
        if (square != null) {
            for (int i = 0; i < square.getObjects().size(); ++i) {
                IsoObject obj = square.getObjects().get(i);
                if (obj == null || obj.sprite == null || obj.sprite.name == null || !this.collapsedTiles.contains(obj.sprite.name)) continue;
                return obj;
            }
        }
        return null;
    }

    public void checkDamageHoppableFence(IsoMovingObject thumper, IsoGridSquare sq, IsoGridSquare oppositeSq) {
        IsoObject hoppable;
        if (this.isEnabled() && thumper instanceof IsoZombie && (hoppable = sq.getHoppableTo(oppositeSq)) != null && this.isBendableFence(hoppable)) {
            ThumpData thumpData = this.getThumpData(hoppable);
            hoppable.setDamage((short)((float)hoppable.getDamage() - (float)Rand.Next(0, 1) * thumpData.damageMultiplier * thumpData.damageModifier));
            if (hoppable.getDamage() <= 0) {
                BentFences.getInstance().bendFence(hoppable, thumpData.directionBent);
                hoppable.setDamage((short)thumpData.health);
            }
        }
    }

    private static void doSquareUpdateTasks(IsoGridSquare square) {
        square.RecalcAllWithNeighbours(true);
        MapCollisionData.instance.squareChanged(square);
        PolygonalMap2.instance.squareChanged(square);
        IsoRegions.squareChanged(square);
        square.invalidateRenderChunkLevel(384L);
    }

    private void addDebrisObject(IsoObject obj, IsoDirections dir, Entry entry) {
        if (entry == null || obj.isWall() || entry.debris.isEmpty()) {
            return;
        }
        String spriteName = PZArrayUtil.pickRandom(entry.debris);
        IsoGridSquare square = obj.getSquare();
        if (square != null) {
            if (dir == IsoDirections.N || dir == IsoDirections.W) {
                square = square.getAdjacentSquare(dir);
            }
            if (square != null && square.TreatAsSolidFloor()) {
                IsoObject objNew = IsoObject.getNew(square, spriteName, null, false);
                square.transmitAddObjectToSquare(objNew, square == obj.getSquare() ? obj.getObjectIndex() : -1);
                BrokenFences.getInstance().addItems(obj, square);
            }
        }
    }

    private static int getThumpersRequired() {
        return SandboxOptions.instance.lore.fenceThumpersRequired.getValue();
    }

    private static float getFenceDamageMultiplier() {
        return (float)SandboxOptions.instance.lore.fenceDamageMultiplier.getValue();
    }

    public boolean isEnabled() {
        return BentFences.getThumpersRequired() >= 1;
    }

    public static void init() {
    }

    public void Reset() {
        this.entries.clear();
        this.fenceMap.clear();
    }

    public static final class Entry {
        IsoDirections dir;
        int health = 100;
        final HashMap<Integer, ArrayList<String>> stages = new HashMap();
        final ArrayList<String> collapsed = new ArrayList();
        final ArrayList<String> debris = new ArrayList();
        int length = -1;
        int collapsedOffset = -1;
        int collapsedSizeX = -1;
        int collapsedSizeY = -1;
        boolean doSmash = true;

        boolean isNorth() {
            return this.dir == IsoDirections.N || this.dir == IsoDirections.S;
        }
    }

    public static final class ThumpData {
        int totalThumpers;
        int bendStage;
        IsoDirections directionToBend;
        IsoDirections directionBent;
        int health = 100;
        int stages;
        int thumpersToDamage = 100;
        final float damageModifier = 0.1f;
        float damageMultiplier = 1.0f;
    }
}

