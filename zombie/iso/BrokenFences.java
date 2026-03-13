/*
 * Decompiled with CFR 0.152.
 */
package zombie.iso;

import gnu.trove.map.hash.THashMap;
import java.util.ArrayList;
import se.krka.kahlua.j2se.KahluaTableImpl;
import se.krka.kahlua.vm.KahluaTable;
import se.krka.kahlua.vm.KahluaTableIterator;
import zombie.MapCollisionData;
import zombie.SoundManager;
import zombie.UsedFromLua;
import zombie.core.properties.PropertyContainer;
import zombie.core.random.Rand;
import zombie.inventory.InventoryItem;
import zombie.inventory.InventoryItemFactory;
import zombie.iso.IsoDirections;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoObject;
import zombie.iso.SpriteDetails.IsoFlagType;
import zombie.iso.areas.isoregion.IsoRegions;
import zombie.iso.objects.IsoThumpable;
import zombie.iso.sprite.IsoSprite;
import zombie.iso.sprite.IsoSpriteManager;
import zombie.network.GameServer;
import zombie.pathfind.PolygonalMap2;
import zombie.scripting.objects.SoundKey;
import zombie.util.list.PZArrayUtil;

@UsedFromLua
public class BrokenFences {
    private static final BrokenFences instance = new BrokenFences();
    private final THashMap<String, Tile> unbrokenMap = new THashMap();
    private final THashMap<String, Tile> brokenLeftMap = new THashMap();
    private final THashMap<String, Tile> brokenRightMap = new THashMap();
    private final THashMap<String, Tile> allMap = new THashMap();

    public static BrokenFences getInstance() {
        return instance;
    }

    private ArrayList<String> tableToTiles(KahluaTableImpl tiles) {
        if (tiles == null) {
            return null;
        }
        ArrayList<String> result = null;
        KahluaTableIterator it = tiles.iterator();
        while (it.advance()) {
            if (result == null) {
                result = new ArrayList<String>();
            }
            result.add(it.getValue().toString());
        }
        return result;
    }

    private ArrayList<String> tableToTiles(KahluaTable table, String key) {
        return this.tableToTiles((KahluaTableImpl)table.rawget(key));
    }

    public void addBrokenTiles(KahluaTableImpl tiles) {
        KahluaTableIterator it = tiles.iterator();
        while (it.advance()) {
            String key = it.getKey().toString();
            if ("VERSION".equalsIgnoreCase(key)) continue;
            KahluaTableImpl value = (KahluaTableImpl)it.getValue();
            Tile tile = new Tile();
            tile.self = this.tableToTiles(value, "self");
            tile.left = this.tableToTiles(value, "left");
            tile.right = this.tableToTiles(value, "right");
            this.unbrokenMap.put(key, tile);
            PZArrayUtil.forEach(tile.left, s -> this.brokenLeftMap.put((String)s, tile));
            PZArrayUtil.forEach(tile.right, s -> this.brokenRightMap.put((String)s, tile));
        }
        this.allMap.putAll(this.unbrokenMap);
        this.allMap.putAll(this.brokenLeftMap);
        this.allMap.putAll(this.brokenRightMap);
    }

    public void addDebrisTiles(KahluaTableImpl tiles) {
        KahluaTableIterator it = tiles.iterator();
        while (it.advance()) {
            String key = it.getKey().toString();
            if ("VERSION".equalsIgnoreCase(key)) continue;
            KahluaTableImpl value = (KahluaTableImpl)it.getValue();
            Tile tile = this.unbrokenMap.get(key);
            if (tile == null) {
                throw new IllegalArgumentException("addDebrisTiles() with unknown tile");
            }
            tile.debrisN = this.tableToTiles(value, "north");
            tile.debrisS = this.tableToTiles(value, "south");
            tile.debrisW = this.tableToTiles(value, "west");
            tile.debrisE = this.tableToTiles(value, "east");
        }
    }

    public void setDestroyed(IsoObject obj) {
        obj.RemoveAttachedAnims();
        obj.getSquare().removeBlood(false, true);
        this.updateSprite(obj, true, true);
    }

    public void setDamagedLeft(IsoObject obj) {
        this.updateSprite(obj, true, false);
    }

    public void setDamagedRight(IsoObject obj) {
        this.updateSprite(obj, false, true);
    }

    public void updateSprite(IsoObject obj, boolean brokenLeft, boolean brokenRight) {
        if (!this.isBreakableObject(obj)) {
            return;
        }
        Tile tile = this.allMap.get(obj.sprite.name);
        String spriteName = null;
        if (brokenLeft && brokenRight) {
            spriteName = tile.pickRandom(tile.self);
        } else if (brokenLeft) {
            spriteName = tile.pickRandom(tile.left);
        } else if (brokenRight) {
            spriteName = tile.pickRandom(tile.right);
        }
        if (spriteName != null) {
            IsoSprite sprite = IsoSpriteManager.instance.getSprite(spriteName);
            sprite.name = spriteName;
            obj.setSprite(sprite);
            obj.transmitUpdatedSprite();
            obj.getSquare().RecalcAllWithNeighbours(true);
            MapCollisionData.instance.squareChanged(obj.getSquare());
            PolygonalMap2.instance.squareChanged(obj.getSquare());
            IsoRegions.squareChanged(obj.getSquare());
            obj.invalidateRenderChunkLevel(256L);
        }
    }

    private boolean isNW(IsoObject obj) {
        PropertyContainer props = obj.getProperties();
        return props.has(IsoFlagType.collideN) && props.has(IsoFlagType.collideW);
    }

    private void damageAdjacent(IsoGridSquare square, IsoDirections dirAdjacent, IsoDirections dirBreak) {
        boolean breakRight;
        IsoGridSquare adjacent = square.getAdjacentSquare(dirAdjacent);
        if (adjacent == null) {
            return;
        }
        boolean north = dirAdjacent == IsoDirections.W || dirAdjacent == IsoDirections.E;
        IsoObject obj = this.getBreakableObject(adjacent, north);
        if (obj == null) {
            return;
        }
        boolean breakLeft = dirAdjacent == IsoDirections.N || dirAdjacent == IsoDirections.E;
        boolean bl = breakRight = dirAdjacent == IsoDirections.S || dirAdjacent == IsoDirections.W;
        if (this.isNW(obj) && (dirAdjacent == IsoDirections.S || dirAdjacent == IsoDirections.E)) {
            return;
        }
        if (breakLeft && this.isBrokenRight(obj)) {
            this.destroyFence(obj, dirBreak);
            return;
        }
        if (breakRight && this.isBrokenLeft(obj)) {
            this.destroyFence(obj, dirBreak);
            return;
        }
        this.updateSprite(obj, breakLeft, breakRight);
    }

    public void destroyFence(IsoObject obj, IsoDirections dir) {
        if (!this.isBreakableObject(obj)) {
            return;
        }
        IsoGridSquare square = obj.getSquare();
        String soundName = "BreakObject";
        if (obj instanceof IsoThumpable) {
            IsoThumpable thumpable = (IsoThumpable)obj;
            soundName = thumpable.getBreakSound();
        }
        String materialStr = obj.getProperties().get("ThumpSound");
        if (SoundKey.ZOMBIE_THUMP_WOOD.toString().equals(materialStr)) {
            soundName = SoundKey.ZOMBIE_THUMP_WOOD_COLLAPSE.toString();
        }
        if (GameServer.server) {
            GameServer.PlayWorldSoundServer(soundName, false, square, 1.0f, 20.0f, 1.0f, true);
        } else {
            SoundManager.instance.PlayWorldSound(soundName, square, 1.0f, 20.0f, 1.0f, true);
        }
        boolean north = obj.getProperties().has(IsoFlagType.collideN);
        boolean west = obj.getProperties().has(IsoFlagType.collideW);
        if (obj instanceof IsoThumpable) {
            IsoObject objNew = IsoObject.getNew();
            objNew.setSquare(square);
            objNew.setSprite(obj.getSprite());
            int index = obj.getObjectIndex();
            square.transmitRemoveItemFromSquare(obj);
            square.transmitAddObjectToSquare(objNew, index);
            obj = objNew;
        }
        this.addDebrisObject(obj, dir);
        this.setDestroyed(obj);
        if (north && west) {
            this.damageAdjacent(square, IsoDirections.S, dir);
            this.damageAdjacent(square, IsoDirections.E, dir);
        } else if (north) {
            this.damageAdjacent(square, IsoDirections.W, dir);
            this.damageAdjacent(square, IsoDirections.E, dir);
        } else if (west) {
            this.damageAdjacent(square, IsoDirections.N, dir);
            this.damageAdjacent(square, IsoDirections.S, dir);
        }
        square.RecalcAllWithNeighbours(true);
        MapCollisionData.instance.squareChanged(square);
        PolygonalMap2.instance.squareChanged(square);
        IsoRegions.squareChanged(square);
        square.invalidateRenderChunkLevel(384L);
    }

    private boolean isUnbroken(IsoObject obj) {
        if (obj == null || obj.sprite == null || obj.sprite.name == null) {
            return false;
        }
        return this.unbrokenMap.contains(obj.sprite.name);
    }

    private boolean isBrokenLeft(IsoObject obj) {
        if (obj == null || obj.sprite == null || obj.sprite.name == null) {
            return false;
        }
        return this.brokenLeftMap.contains(obj.sprite.name);
    }

    private boolean isBrokenRight(IsoObject obj) {
        if (obj == null || obj.sprite == null || obj.sprite.name == null) {
            return false;
        }
        return this.brokenRightMap.contains(obj.sprite.name);
    }

    public boolean isBreakableObject(IsoObject obj) {
        if (obj == null || obj.sprite == null || obj.sprite.name == null) {
            return false;
        }
        return this.allMap.containsKey(obj.sprite.name);
    }

    public boolean isBreakableSprite(String spriteName) {
        return this.allMap.containsKey(spriteName);
    }

    public IsoObject getBreakableObject(IsoGridSquare square, boolean north) {
        for (int n = 0; n < square.objects.size(); ++n) {
            IsoObject obj = square.objects.get(n);
            if (!this.isBreakableObject(obj) || (!north || !obj.getProperties().has(IsoFlagType.collideN)) && (north || !obj.getProperties().has(IsoFlagType.collideW))) continue;
            return obj;
        }
        return null;
    }

    public void addItems(IsoObject obj, IsoGridSquare square) {
        PropertyContainer props = obj.getProperties();
        if (props == null) {
            return;
        }
        String material = props.get("Material");
        String material2 = props.get("Material2");
        String material3 = props.get("Material3");
        if ("Wood".equals(material) || "Wood".equals(material2) || "Wood".equals(material3)) {
            square.AddWorldInventoryItem((InventoryItem)InventoryItemFactory.CreateItem("Base.UnusableWood"), Rand.Next(0.0f, 0.5f), Rand.Next(0.0f, 0.5f), 0.0f);
            if (Rand.NextBool(5)) {
                square.AddWorldInventoryItem((InventoryItem)InventoryItemFactory.CreateItem("Base.UnusableWood"), Rand.Next(0.0f, 0.5f), Rand.Next(0.0f, 0.5f), 0.0f);
            }
        }
        if (("MetalBars".equals(material) || "MetalBars".equals(material2) || "MetalBars".equals(material3)) && Rand.NextBool(2)) {
            square.AddWorldInventoryItem((InventoryItem)InventoryItemFactory.CreateItem("Base.MetalBar"), Rand.Next(0.0f, 0.5f), Rand.Next(0.0f, 0.5f), 0.0f);
        }
        if (("MetalWire".equals(material) || "MetalWire".equals(material2) || "MetalWire".equals(material3)) && Rand.NextBool(3)) {
            square.AddWorldInventoryItem((InventoryItem)InventoryItemFactory.CreateItem("Base.Wire"), Rand.Next(0.0f, 0.5f), Rand.Next(0.0f, 0.5f), 0.0f);
        }
        if (("MetalPlates".equals(material) || "MetalPlates".equals(material2) || "MetalPlates".equals(material3)) && Rand.NextBool(2)) {
            square.AddWorldInventoryItem((InventoryItem)InventoryItemFactory.CreateItem("Base.SheetMetal"), Rand.Next(0.0f, 0.5f), Rand.Next(0.0f, 0.5f), 0.0f);
            square.AddWorldInventoryItem((InventoryItem)InventoryItemFactory.CreateItem("Base.NutsBolts"), Rand.Next(0.0f, 0.5f), Rand.Next(0.0f, 0.5f), 0.0f);
        }
        if (("MetalScrap".equals(material) || "MetalScrap".equals(material2) || "MetalScrap".equals(material3)) && Rand.NextBool(2)) {
            square.AddWorldInventoryItem((InventoryItem)InventoryItemFactory.CreateItem("Base.SteelChunk"), Rand.Next(0.0f, 0.5f), Rand.Next(0.0f, 0.5f), 0.0f);
            square.AddWorldInventoryItem((InventoryItem)InventoryItemFactory.CreateItem("Base.SteelPiece"), Rand.Next(0.0f, 0.5f), Rand.Next(0.0f, 0.5f), 0.0f);
        }
        if (("Nails".equals(material) || "Nails".equals(material2) || "Nails".equals(material3)) && Rand.NextBool(2)) {
            square.AddWorldInventoryItem((InventoryItem)InventoryItemFactory.CreateItem("Base.Nails"), Rand.Next(0.0f, 0.5f), Rand.Next(0.0f, 0.5f), 0.0f);
        }
        if (("Screws".equals(material) || "Screws".equals(material2) || "Screws".equals(material3)) && Rand.NextBool(2)) {
            square.AddWorldInventoryItem((InventoryItem)InventoryItemFactory.CreateItem("Base.Screws"), Rand.Next(0.0f, 0.5f), Rand.Next(0.0f, 0.5f), 0.0f);
        }
    }

    private void addDebrisObject(IsoObject obj, IsoDirections dir) {
        String spriteName;
        if (!this.isBreakableObject(obj)) {
            return;
        }
        Tile tile = this.allMap.get(obj.sprite.name);
        IsoGridSquare square = obj.getSquare();
        switch (dir) {
            case N: {
                spriteName = tile.pickRandom(tile.debrisN);
                square = square.getAdjacentSquare(dir);
                break;
            }
            case S: {
                spriteName = tile.pickRandom(tile.debrisS);
                break;
            }
            case W: {
                spriteName = tile.pickRandom(tile.debrisW);
                square = square.getAdjacentSquare(dir);
                break;
            }
            case E: {
                spriteName = tile.pickRandom(tile.debrisE);
                break;
            }
            default: {
                throw new IllegalArgumentException("invalid direction");
            }
        }
        if (spriteName != null && square != null && square.TreatAsSolidFloor()) {
            IsoObject objNew = IsoObject.getNew(square, spriteName, null, false);
            square.transmitAddObjectToSquare(objNew, square == obj.getSquare() ? obj.getObjectIndex() : -1);
            this.addItems(obj, square);
        }
    }

    public void Reset() {
        this.unbrokenMap.clear();
        this.brokenLeftMap.clear();
        this.brokenRightMap.clear();
        this.allMap.clear();
    }

    private static final class Tile {
        ArrayList<String> self;
        ArrayList<String> left;
        ArrayList<String> right;
        ArrayList<String> debrisN;
        ArrayList<String> debrisS;
        ArrayList<String> debrisW;
        ArrayList<String> debrisE;

        private Tile() {
        }

        String pickRandom(ArrayList<String> choices) {
            if (choices == null) {
                return null;
            }
            return PZArrayUtil.pickRandom(choices);
        }
    }
}

