/*
 * Decompiled with CFR 0.152.
 */
package zombie.randomizedWorld.randomizedVehicleStory;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Objects;
import org.joml.Vector2f;
import zombie.SandboxOptions;
import zombie.UsedFromLua;
import zombie.core.Core;
import zombie.core.math.PZMath;
import zombie.core.properties.PropertyContainer;
import zombie.core.random.Rand;
import zombie.inventory.ItemPickerJava;
import zombie.iso.IsoCell;
import zombie.iso.IsoChunk;
import zombie.iso.IsoDirections;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoObject;
import zombie.iso.IsoWorld;
import zombie.iso.Vector2;
import zombie.iso.zones.Zone;
import zombie.network.GameServer;
import zombie.network.ServerMap;
import zombie.randomizedWorld.RandomizedWorldBase;
import zombie.randomizedWorld.randomizedVehicleStory.VehicleStorySpawnData;
import zombie.randomizedWorld.randomizedVehicleStory.VehicleStorySpawner;
import zombie.vehicles.BaseVehicle;

@UsedFromLua
public class RandomizedVehicleStoryBase
extends RandomizedWorldBase {
    private int chance;
    private static int totalChance;
    private static final HashMap<RandomizedVehicleStoryBase, Integer> rvsMap;
    protected boolean horizontalZone;
    protected int zoneWidth;
    public static int baseChance;
    protected int minX;
    protected int minY;
    protected int maxX;
    protected int maxY;
    protected int minZoneWidth;
    protected int minZoneHeight;
    protected boolean needsPavement;
    protected boolean needsDirt;
    protected boolean needsRegion;
    protected boolean needsFarmland;
    protected boolean needsRuralVegetation;
    protected boolean notTown;

    public static void initAllRVSMapChance(Zone zone, IsoChunk chunk) {
        totalChance = 0;
        rvsMap.clear();
        for (int i = 0; i < IsoWorld.instance.getRandomizedVehicleStoryList().size(); ++i) {
            RandomizedVehicleStoryBase rvs = IsoWorld.instance.getRandomizedVehicleStoryList().get(i);
            if (!rvs.isValid(zone, chunk, false) || !rvs.isTimeValid(false)) continue;
            totalChance += rvs.getChance();
            rvsMap.put(rvs, rvs.getChance());
        }
    }

    public static boolean doRandomStory(Zone zone, IsoChunk chunk, boolean force) {
        if (!chunk.vehicles.isEmpty()) {
            return false;
        }
        if (chunk.hasWaterSquare()) {
            return false;
        }
        int chance = 1000;
        switch (SandboxOptions.instance.vehicleStoryChance.getValue()) {
            case 1: {
                return false;
            }
            case 2: {
                chance = 2000;
                break;
            }
            case 4: {
                chance = 600;
                break;
            }
            case 5: {
                chance = 350;
                break;
            }
            case 6: {
                chance = 100;
                break;
            }
            case 7: {
                chance = 0;
            }
        }
        int roll = Rand.Next(0, chance);
        if (roll <= baseChance) {
            RandomizedVehicleStoryBase.initAllRVSMapChance(zone, chunk);
            RandomizedVehicleStoryBase rvs = RandomizedVehicleStoryBase.getRandomStory();
            if (rvs == null) {
                return false;
            }
            VehicleStorySpawnData spawnData = rvs.initSpawnDataForChunk(zone, chunk);
            chunk.setRandomVehicleStoryToSpawnLater(spawnData);
            return true;
        }
        return false;
    }

    private static RandomizedVehicleStoryBase getRandomStory() {
        int choice = Rand.Next(totalChance);
        Iterator<RandomizedVehicleStoryBase> it = rvsMap.keySet().iterator();
        int subTotal = 0;
        while (it.hasNext()) {
            RandomizedVehicleStoryBase testTable = it.next();
            if (choice >= (subTotal += rvsMap.get(testTable).intValue())) continue;
            return testTable;
        }
        return null;
    }

    public int getMinZoneWidth() {
        return this.minZoneWidth <= 0 ? 10 : this.minZoneWidth;
    }

    public int getMinZoneHeight() {
        return this.minZoneHeight <= 0 ? 5 : this.minZoneHeight;
    }

    public void randomizeVehicleStory(Zone zone, IsoChunk chunk) {
    }

    public IsoGridSquare getCenterOfChunk(Zone zone, IsoChunk chunk) {
        int x;
        int y;
        int minX = Math.max(zone.x, chunk.wx * 8);
        int minY = Math.max(zone.y, chunk.wy * 8);
        int maxX = Math.min(zone.x + zone.w, (chunk.wx + 1) * 8);
        int maxY = Math.min(zone.y + zone.h, (chunk.wy + 1) * 8);
        if (this.horizontalZone) {
            y = (zone.y + zone.y + zone.h) / 2;
            x = (minX + maxX) / 2;
        } else {
            y = (minY + maxY) / 2;
            x = (zone.x + zone.x + zone.w) / 2;
        }
        return IsoCell.getInstance().getGridSquare(x, y, zone.z);
    }

    public boolean isValid(Zone zone, IsoChunk chunk, boolean force) {
        IsoGridSquare centerSq;
        int centerY;
        boolean fail;
        IsoGridSquare sq;
        int j;
        int i;
        int x = zone.x;
        int y = zone.y;
        int x2 = zone.x + zone.w;
        int y2 = zone.y + zone.h;
        for (int xDel = x; xDel < x2; ++xDel) {
            for (int yDel = y; yDel < y2; ++yDel) {
                IsoGridSquare sq2 = IsoWorld.instance.getCell().getGridSquare(xDel, yDel, zone.z);
                if (sq2 == null || !sq2.isWaterSquare()) continue;
                return false;
            }
        }
        this.horizontalZone = false;
        this.zoneWidth = 0;
        this.debugLine = "";
        if (!force && zone.hourLastSeen != 0) {
            return false;
        }
        if (!force && zone.haveConstruction) {
            return false;
        }
        if (!"Nav".equals(zone.getType())) {
            this.debugLine = this.debugLine + "Not a 'Nav' zone.";
            return false;
        }
        this.minX = Math.max(zone.x, chunk.wx * 8);
        this.minY = Math.max(zone.y, chunk.wy * 8);
        this.maxX = Math.min(zone.x + zone.w, (chunk.wx + 1) * 8);
        this.maxY = Math.min(zone.y + zone.h, (chunk.wy + 1) * 8);
        if (!this.getSpawnPoint(zone, chunk, null)) {
            return false;
        }
        if (this.getCenterOfChunk(zone, chunk) != null) {
            IsoGridSquare centerSq2 = this.getCenterOfChunk(zone, chunk);
            int centerX = centerSq2.getX();
            int centerY2 = centerSq2.getY();
            boolean valid = false;
            block2: for (i = -5; i < 5; ++i) {
                for (j = -5; j < 5; ++j) {
                    sq = RandomizedVehicleStoryBase.getSq(centerX + i, centerY2 + j, centerSq2.getZ());
                    if (sq == null || sq.getZone() != zone) continue;
                    IsoObject floor = sq.getFloor();
                    PropertyContainer props = floor.getProperties();
                    if (props != null && props.has("FootstepMaterial") && (Objects.equals(props.get("FootstepMaterial"), "Gravel") || Objects.equals(props.get("FootstepMaterial"), "Sand"))) {
                        valid = true;
                        continue block2;
                    }
                    String floorName = floor.getSprite().getName();
                    if (!floorName.contains("street")) continue;
                    valid = true;
                    continue block2;
                }
            }
            if (!valid) {
                this.debugLine = "Requires a paved, gravel or sand road";
                return false;
            }
        }
        if (this.needsPavement) {
            IsoGridSquare centerSq3 = this.getCenterOfChunk(zone, chunk);
            if (centerSq3 == null) {
                return false;
            }
            IsoObject floor = centerSq3.getFloor();
            String floorName = floor.getSprite().getName();
            if (!floorName.startsWith("blends_street") || floorName.startsWith("blends_street_01_16") || floorName.startsWith("blends_street_01_21") || floorName.startsWith("blends_street_01_48") || floorName.startsWith("blends_street_01_53") || floorName.startsWith("blends_street_01_54") || floorName.startsWith("blends_street_01_55")) {
                this.debugLine = "Requires pavement";
                return false;
            }
        }
        if (this.needsDirt) {
            IsoGridSquare centerSq4 = this.getCenterOfChunk(zone, chunk);
            if (centerSq4 == null) {
                return false;
            }
            IsoObject floor = centerSq4.getFloor();
            String floorName = floor.getSprite().getName();
            if (!(!floorName.startsWith("blends_street") || floorName.startsWith("blends_street_01_16") || floorName.startsWith("blends_street_01_21") || floorName.startsWith("blends_street_01_48") || floorName.startsWith("blends_street_01_53") || floorName.startsWith("blends_street_01_54") || floorName.startsWith("blends_street_01_55"))) {
                this.debugLine = "Requires dirt";
                return false;
            }
        }
        if (this.needsRegion) {
            IsoGridSquare centerSq5 = this.getCenterOfChunk(zone, chunk);
            if (centerSq5 == null) {
                return false;
            }
            String region = ItemPickerJava.getSquareRegion(centerSq5);
            if (Objects.equals(region, "General")) {
                this.debugLine = "Requires a municipal region";
                return false;
            }
        }
        if (this.needsFarmland) {
            fail = true;
            IsoGridSquare centerSq6 = this.getCenterOfChunk(zone, chunk);
            if (centerSq6 != null) {
                int centerX = centerSq6.getX();
                centerY = centerSq6.getY();
                block4: for (i = -15; i < 15 && fail; ++i) {
                    for (j = -15; j < 15; ++j) {
                        if (!fail || (sq = RandomizedVehicleStoryBase.getSq(centerX + i * 10, centerY + j * 10, 0)) == null || sq.getZoneType() == null || !Objects.equals(sq.getZoneType(), "Farm") && !Objects.equals(sq.getZoneType(), "FarmLand") && !Objects.equals(sq.getZoneType(), "Ranch") && !ItemPickerJava.squareHasZone(sq, "Ranch")) continue;
                        fail = false;
                        continue block4;
                    }
                }
            }
            if (fail) {
                this.debugLine = "Requires nearby farmland";
                return false;
            }
        }
        if (this.needsRuralVegetation) {
            fail = true;
            IsoGridSquare centerSq7 = this.getCenterOfChunk(zone, chunk);
            if (centerSq7 != null) {
                int centerX = centerSq7.getX();
                centerY = centerSq7.getY();
                block6: for (i = -15; i < 15 && fail; ++i) {
                    for (j = -15; j < 15; ++j) {
                        if (!fail || (sq = RandomizedVehicleStoryBase.getSq(centerX + i * 10, centerY + j * 10, 0)) == null || sq.getZoneType() == null || !Objects.equals(sq.getZoneType(), "Farm") && !Objects.equals(sq.getZoneType(), "FarmLand") && !Objects.equals(sq.getZoneType(), "Forest") && !Objects.equals(sq.getZoneType(), "DeepForest")) continue;
                        fail = false;
                        continue block6;
                    }
                }
            }
            if (fail) {
                this.debugLine = "Requires nearby rural vegetation";
                return false;
            }
        }
        if (this.notTown && (centerSq = this.getCenterOfChunk(zone, chunk)) != null) {
            int centerX = centerSq.getX();
            int centerY3 = centerSq.getY();
            for (int i2 = -15; i2 < 15; ++i2) {
                for (int j2 = -15; j2 < 15; ++j2) {
                    IsoGridSquare sq3 = RandomizedVehicleStoryBase.getSq(centerX + i2 * 10, centerY3 + j2 * 10, 0);
                    if (sq3 == null || sq3.getZoneType() == null || !Objects.equals(sq3.getZoneType(), "TownZone") && !Objects.equals(sq3.getZoneType(), "TrailerPark")) continue;
                    this.debugLine = "Requires rural location";
                    return false;
                }
            }
        }
        return true;
    }

    public VehicleStorySpawnData initSpawnDataForChunk(Zone zone, IsoChunk chunk) {
        int minZoneWidth = this.getMinZoneWidth();
        int minZoneHeight = this.getMinZoneHeight();
        float[] xyd = new float[3];
        if (!this.getSpawnPoint(zone, chunk, xyd)) {
            return null;
        }
        float centerX = xyd[0];
        float centerY = xyd[1];
        float direction = xyd[2];
        int[] aabb = new int[4];
        VehicleStorySpawner.getInstance().getAABB(centerX, centerY, minZoneWidth, minZoneHeight, direction, aabb);
        return new VehicleStorySpawnData(this, zone, centerX, centerY, direction, aabb[0], aabb[1], aabb[2], aabb[3]);
    }

    public boolean getSpawnPoint(Zone zone, IsoChunk chunk, float[] result) {
        if (!(zone.isRectangle() || zone.isPolyline() && zone.polylineWidth > 0)) {
            this.debugLine = this.debugLine + "Zone isn't a rectangle or a polyline";
            return false;
        }
        return this.getRectangleSpawnPoint(zone, chunk, result) || this.getPolylineSpawnPoint(zone, chunk, result);
    }

    public boolean getRectangleSpawnPoint(Zone zone, IsoChunk chunk, float[] result) {
        if (!zone.isRectangle()) {
            return false;
        }
        int minZoneWidth = this.getMinZoneWidth();
        int minZoneHeight = this.getMinZoneHeight();
        if (zone.w > 30 && zone.h < 15) {
            this.horizontalZone = true;
            this.zoneWidth = zone.h;
            if (zone.getWidth() < minZoneHeight || zone.getHeight() < minZoneWidth) {
                this.debugLine = "Horizontal street is too small, WxH = %dx%d, required = %dx%d".formatted(zone.getWidth(), zone.getHeight(), minZoneHeight, minZoneWidth);
                return false;
            }
            if (result == null) {
                return true;
            }
            float x1 = zone.getX();
            float x2 = zone.getX() + zone.getWidth();
            float y = (float)zone.getY() + (float)zone.getHeight() / 2.0f;
            result[0] = PZMath.clamp((float)(chunk.wx * 8) + 4.0f, x1 + (float)minZoneHeight / 2.0f, x2 - (float)minZoneHeight / 2.0f);
            result[1] = y;
            result[2] = Vector2.getDirection(x2 - x1, 0.0f);
            return true;
        }
        if (zone.h > 30 && zone.w < 15) {
            float x;
            this.horizontalZone = false;
            this.zoneWidth = zone.w;
            if (zone.getWidth() < minZoneWidth || zone.getHeight() < minZoneHeight) {
                this.debugLine = "Vertical street is too small, WxH = %dx%d, required = %dx%d".formatted(zone.getWidth(), zone.getHeight(), minZoneWidth, minZoneHeight);
                return false;
            }
            if (result == null) {
                return true;
            }
            float y1 = zone.getY();
            float y2 = zone.getY() + zone.getHeight();
            result[0] = x = (float)zone.getX() + (float)zone.getWidth() / 2.0f;
            result[1] = PZMath.clamp((float)(chunk.wy * 8) + 4.0f, y1 + (float)minZoneHeight / 2.0f, y2 - (float)minZoneHeight / 2.0f);
            result[2] = Vector2.getDirection(0.0f, y1 - y2);
            return true;
        }
        this.debugLine = "Zone too small or too large";
        return false;
    }

    public boolean getPolylineSpawnPoint(Zone zone, IsoChunk chunk, float[] result) {
        float zy2;
        float dy;
        if (!zone.isPolyline() || zone.polylineWidth <= 0) {
            return false;
        }
        int minZoneWidth = this.getMinZoneWidth();
        int minZoneHeight = this.getMinZoneHeight();
        if (zone.polylineWidth < minZoneWidth) {
            this.debugLine = "Polyline zone is too narrow, width=%d required=%d".formatted(zone.polylineWidth, minZoneWidth);
            return false;
        }
        double[] t1t2 = new double[2];
        int segment = zone.getClippedSegmentOfPolyline(chunk.wx * 8, chunk.wy * 8, (chunk.wx + 1) * 8, (chunk.wy + 1) * 8, t1t2);
        if (segment == -1) {
            return false;
        }
        double t1 = t1t2[0];
        double t2 = t1t2[1];
        float dxy = zone.polylineWidth % 2 == 0 ? 0.0f : 0.5f;
        float zx1 = (float)zone.points.get(segment * 2) + dxy;
        float zy1 = (float)zone.points.get(segment * 2 + 1) + dxy;
        float zx2 = (float)zone.points.get(segment * 2 + 2) + dxy;
        float dx = zx2 - zx1;
        float segmentLength = Vector2f.length(dx, dy = (zy2 = (float)zone.points.get(segment * 2 + 3) + dxy) - zy1);
        if (segmentLength < (float)minZoneHeight) {
            this.debugLine = "Polyline segment too short, length: %.2f required: %d".formatted(Float.valueOf(segmentLength), minZoneHeight);
            return false;
        }
        this.zoneWidth = zone.polylineWidth;
        if (result == null) {
            return true;
        }
        float f = (float)minZoneHeight / 2.0f / segmentLength;
        float f1 = PZMath.max((float)t1 - f, f);
        float f2 = PZMath.min((float)t2 + f, 1.0f - f);
        float segmentX1 = zx1 + dx * f1;
        float segmentY1 = zy1 + dy * f1;
        float segmentX2 = zx1 + dx * f2;
        float segmentY2 = zy1 + dy * f2;
        float t = Rand.Next(0.0f, 1.0f);
        if (Core.debug) {
            t = (float)(System.currentTimeMillis() / 20L % 360L) / 360.0f;
        }
        result[0] = segmentX1 + (segmentX2 - segmentX1) * t;
        result[1] = segmentY1 + (segmentY2 - segmentY1) * t;
        result[2] = Vector2.getDirection(dx, dy);
        return true;
    }

    public boolean isFullyStreamedIn(int x1, int y1, int x2, int y2) {
        int chunksPerWidth = 8;
        int wx1 = x1 / 8;
        int wy1 = y1 / 8;
        int wx2 = (x2 - 1) / 8;
        int wy2 = (y2 - 1) / 8;
        for (int wy = wy1; wy <= wy2; ++wy) {
            for (int wx = wx1; wx <= wx2; ++wx) {
                if (this.isChunkLoaded(wx, wy)) continue;
                return false;
            }
        }
        return true;
    }

    public boolean isChunkLoaded(int wx, int wy) {
        IsoChunk chunk = GameServer.server ? ServerMap.instance.getChunk(wx, wy) : IsoWorld.instance.currentCell.getChunk(wx, wy);
        return chunk != null && chunk.loaded;
    }

    public boolean initVehicleStorySpawner(Zone zone, IsoChunk chunk, boolean debug) {
        return false;
    }

    public boolean callVehicleStorySpawner(Zone zone, IsoChunk chunk, float additionalRotationRadians) {
        float[] xyd = new float[3];
        if (!this.getSpawnPoint(zone, chunk, xyd)) {
            return false;
        }
        this.initVehicleStorySpawner(zone, chunk, false);
        VehicleStorySpawner spawner = VehicleStorySpawner.getInstance();
        float angleRadians = xyd[2];
        if (Rand.NextBool(2)) {
            angleRadians += (float)Math.PI;
        }
        angleRadians += additionalRotationRadians;
        spawner.spawn(xyd[0], xyd[1], 0.0f, angleRadians += 1.5707964f, this::spawnElement);
        return true;
    }

    public void spawnElement(VehicleStorySpawner spawner, VehicleStorySpawner.Element element) {
    }

    public BaseVehicle[] addSmashedOverlay(BaseVehicle v1, BaseVehicle v2, int xOffset, int yOffset, boolean horizontalZone, boolean addBlood) {
        String secondCarArea;
        String firstCarArea;
        IsoDirections firstCarDir = v1.getDir();
        IsoDirections secondCarDir = v2.getDir();
        if (!horizontalZone) {
            firstCarArea = "Front";
            secondCarArea = secondCarDir == IsoDirections.W ? (firstCarDir == IsoDirections.S ? "Right" : "Left") : (firstCarDir == IsoDirections.S ? "Left" : "Right");
        } else {
            firstCarArea = firstCarDir == IsoDirections.S ? (xOffset > 0 ? "Left" : "Right") : (xOffset < 0 ? "Left" : "Right");
            secondCarArea = "Front";
        }
        v1 = v1.setSmashed(firstCarArea);
        v2 = v2.setSmashed(secondCarArea);
        if (addBlood) {
            v1.setBloodIntensity(firstCarArea, 1.0f);
            v2.setBloodIntensity(secondCarArea, 1.0f);
        }
        return new BaseVehicle[]{v1, v2};
    }

    public int getChance() {
        return this.chance;
    }

    public void setChance(int chance) {
        this.chance = chance;
    }

    public int getMinimumDays() {
        return this.minimumDays;
    }

    public void setMinimumDays(int minimumDays) {
        this.minimumDays = minimumDays;
    }

    public void registerCustomOutfits() {
    }

    public static IsoGridSquare getRandomFreeUnoccupiedSquare(RandomizedVehicleStoryBase rvs, Zone zone, IsoGridSquare sq1) {
        IsoGridSquare sq = null;
        for (int index = 0; index < 1000; ++index) {
            int randX = Rand.Next(sq1.getX() - rvs.minZoneWidth / 2, sq1.getX() + rvs.minZoneWidth / 2);
            int randY = Rand.Next(sq1.getY() - rvs.minZoneHeight / 2, sq1.getY() + rvs.minZoneHeight / 2);
            boolean good = true;
            for (int i = -1; i < 1; ++i) {
                for (int j = -1; j < 1; ++j) {
                    sq = RandomizedVehicleStoryBase.getSq(randX + i, randY + j, zone.z);
                    if (sq != null && sq.isFree(true) && !sq.isVehicleIntersecting()) continue;
                    good = false;
                }
            }
            if (!good) continue;
            return sq;
        }
        return null;
    }

    static {
        rvsMap = new HashMap();
        baseChance = 25;
    }
}

