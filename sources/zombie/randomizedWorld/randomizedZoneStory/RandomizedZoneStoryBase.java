/*
 * Decompiled with CFR 0.152.
 */
package zombie.randomizedWorld.randomizedZoneStory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import zombie.SandboxOptions;
import zombie.UsedFromLua;
import zombie.core.random.Rand;
import zombie.iso.IsoChunk;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoMovingObject;
import zombie.iso.IsoObject;
import zombie.iso.IsoWorld;
import zombie.iso.objects.IsoDeadBody;
import zombie.iso.zones.Zone;
import zombie.network.GameServer;
import zombie.network.ServerMap;
import zombie.randomizedWorld.RandomizedWorldBase;

@UsedFromLua
public class RandomizedZoneStoryBase
extends RandomizedWorldBase {
    public boolean alwaysDo;
    public static final int baseChance = 15;
    public static int totalChance;
    public static final String zoneStory = "ZoneStory";
    public int chance;
    protected int minZoneWidth;
    protected int minZoneHeight;
    public final ArrayList<String> zoneType = new ArrayList();
    private static final HashMap<RandomizedZoneStoryBase, Integer> rzsMap;

    public static boolean isValidForStory(Zone zone, boolean force) {
        if (zone.pickedXForZoneStory > 0 && zone.pickedYForZoneStory > 0 && zone.pickedRzStory != null && RandomizedZoneStoryBase.checkCanSpawnStory(zone, force)) {
            zone.pickedRzStory.randomizeZoneStory(zone);
            zone.pickedRzStory = null;
            zone.pickedXForZoneStory = 0;
            zone.pickedYForZoneStory = 0;
        }
        if (!force && zone.hourLastSeen != 0) {
            return false;
        }
        if (!force && zone.haveConstruction) {
            return false;
        }
        if (!zoneStory.equals(zone.type)) {
            return false;
        }
        RandomizedZoneStoryBase.doRandomStory(zone);
        return true;
    }

    public static void initAllRZSMapChance(Zone zone) {
        totalChance = 0;
        rzsMap.clear();
        for (int i = 0; i < IsoWorld.instance.getRandomizedZoneList().size(); ++i) {
            RandomizedZoneStoryBase rvs = IsoWorld.instance.getRandomizedZoneList().get(i);
            if (!rvs.isValid(zone, false) || !rvs.isTimeValid(false)) continue;
            totalChance += rvs.chance;
            rzsMap.put(rvs, rvs.chance);
        }
    }

    public boolean isValid(Zone zone, boolean force) {
        int x = zone.x;
        int y = zone.y;
        int x2 = zone.x + zone.w;
        int y2 = zone.y + zone.h;
        for (int xDel = x; xDel < x2; ++xDel) {
            for (int yDel = y; yDel < y2; ++yDel) {
                IsoGridSquare sq = IsoWorld.instance.getCell().getGridSquare(xDel, yDel, zone.z);
                if (sq == null || !sq.isWaterSquare()) continue;
                return false;
            }
        }
        boolean zoneValid = false;
        for (int i = 0; i < this.zoneType.size(); ++i) {
            if (!this.zoneType.get(i).equals(zone.name)) continue;
            zoneValid = true;
            break;
        }
        return zoneValid && zone.w >= this.minZoneWidth && zone.h >= this.minZoneHeight;
    }

    private static boolean doRandomStory(Zone zone) {
        int minX;
        ++zone.hourLastSeen;
        int chance = 6;
        switch (SandboxOptions.instance.zoneStoryChance.getValue()) {
            case 1: {
                return false;
            }
            case 2: {
                chance = 2;
                break;
            }
            case 4: {
                chance = 12;
                break;
            }
            case 5: {
                chance = 20;
                break;
            }
            case 6: {
                chance = 40;
                break;
            }
            case 7: {
                chance = 100;
            }
        }
        RandomizedZoneStoryBase rzs = null;
        for (int i = 0; i < IsoWorld.instance.getRandomizedZoneList().size(); ++i) {
            RandomizedZoneStoryBase rvs = IsoWorld.instance.getRandomizedZoneList().get(i);
            if (!rvs.alwaysDo || !rvs.isValid(zone, false) || !rvs.isTimeValid(false)) continue;
            rzs = rvs;
        }
        if (rzs != null) {
            minX = zone.x;
            int minY = zone.y;
            int maxX = zone.x + zone.w - rzs.minZoneWidth / 2;
            int maxY = zone.y + zone.h - rzs.minZoneHeight / 2;
            zone.pickedXForZoneStory = Rand.Next(minX, maxX + 1);
            zone.pickedYForZoneStory = Rand.Next(minY, maxY + 1);
            zone.pickedRzStory = rzs;
            return true;
        }
        if (Rand.Next(100) < chance) {
            RandomizedZoneStoryBase.initAllRZSMapChance(zone);
            rzs = RandomizedZoneStoryBase.getRandomStory();
            if (rzs == null) {
                return false;
            }
            minX = zone.x;
            int minY = zone.y;
            int maxX = zone.x + zone.w - rzs.minZoneWidth / 2;
            int maxY = zone.y + zone.h - rzs.minZoneHeight / 2;
            zone.pickedXForZoneStory = Rand.Next(minX, maxX + 1);
            zone.pickedYForZoneStory = Rand.Next(minY, maxY + 1);
            zone.pickedRzStory = rzs;
            return true;
        }
        return false;
    }

    public IsoGridSquare getRandomFreeSquare(RandomizedZoneStoryBase rzs, Zone zone) {
        return this.getRandomFreeSquare(rzs, zone, null);
    }

    public IsoGridSquare getRandomFreeSquare(RandomizedZoneStoryBase rzs, Zone zone, IsoGridSquare notSquare) {
        for (int index = 0; index < 1000; ++index) {
            int randY;
            int randX = Rand.Next(zone.pickedXForZoneStory - rzs.minZoneWidth / 2, zone.pickedXForZoneStory + rzs.minZoneWidth / 2);
            IsoGridSquare sq = RandomizedZoneStoryBase.getSq(randX, randY = Rand.Next(zone.pickedYForZoneStory - rzs.minZoneHeight / 2, zone.pickedYForZoneStory + rzs.minZoneHeight / 2), zone.z);
            if (sq == null || !sq.isFree(false) || notSquare != null && sq == notSquare || sq.isVehicleIntersecting()) continue;
            return sq;
        }
        return null;
    }

    public IsoGridSquare getRandomExtraFreeSquare(RandomizedZoneStoryBase rzs, Zone zone) {
        for (int index = 0; index < 1000; ++index) {
            int randY;
            int randX = Rand.Next(zone.pickedXForZoneStory - rzs.minZoneWidth / 2, zone.pickedXForZoneStory + rzs.minZoneWidth / 2);
            IsoGridSquare sq = RandomizedZoneStoryBase.getSq(randX, randY = Rand.Next(zone.pickedYForZoneStory - rzs.minZoneHeight / 2, zone.pickedYForZoneStory + rzs.minZoneHeight / 2), zone.z);
            if (sq == null || !sq.isFree(false) || sq.getObjects().size() >= 2 || sq.isVehicleIntersecting()) continue;
            return sq;
        }
        return null;
    }

    public static IsoGridSquare getRandomFreeUnoccupiedSquare(RandomizedZoneStoryBase rzs, Zone zone) {
        for (int index = 0; index < 1000; ++index) {
            int randY;
            int randX = Rand.Next(zone.pickedXForZoneStory - rzs.minZoneWidth / 2, zone.pickedXForZoneStory + rzs.minZoneWidth / 2);
            IsoGridSquare sq = RandomizedZoneStoryBase.getSq(randX, randY = Rand.Next(zone.pickedYForZoneStory - rzs.minZoneHeight / 2, zone.pickedYForZoneStory + rzs.minZoneHeight / 2), zone.z);
            if (sq == null || !sq.isFree(true) || sq.isVehicleIntersecting()) continue;
            return sq;
        }
        return null;
    }

    public static IsoGridSquare getRandomExtraFreeUnoccupiedSquare(RandomizedZoneStoryBase rzs, Zone zone) {
        for (int index = 0; index < 1000; ++index) {
            int randY;
            int randX = Rand.Next(zone.pickedXForZoneStory - rzs.minZoneWidth / 2, zone.pickedXForZoneStory + rzs.minZoneWidth / 2);
            IsoGridSquare sq = RandomizedZoneStoryBase.getSq(randX, randY = Rand.Next(zone.pickedYForZoneStory - rzs.minZoneHeight / 2, zone.pickedYForZoneStory + rzs.minZoneHeight / 2), zone.z);
            if (sq == null || !sq.isFree(true) || sq.getObjects().size() >= 2 || sq.isVehicleIntersecting()) continue;
            return sq;
        }
        return null;
    }

    public IsoGridSquare getRandomFreeSquareFullZone(RandomizedZoneStoryBase rzs, Zone zone) {
        for (int index = 0; index < 1000; ++index) {
            int randY;
            int randX = Rand.Next(zone.x, zone.x + zone.w);
            IsoGridSquare sq = RandomizedZoneStoryBase.getSq(randX, randY = Rand.Next(zone.y, zone.y + zone.h), zone.z);
            if (sq == null || !sq.isFree(false)) continue;
            return sq;
        }
        return null;
    }

    private static RandomizedZoneStoryBase getRandomStory() {
        int choice = Rand.Next(totalChance);
        Iterator<RandomizedZoneStoryBase> it = rzsMap.keySet().iterator();
        int subTotal = 0;
        while (it.hasNext()) {
            RandomizedZoneStoryBase testTable = it.next();
            if (choice >= (subTotal += rzsMap.get(testTable).intValue())) continue;
            return testTable;
        }
        return null;
    }

    private static boolean checkCanSpawnStory(Zone zone, boolean force) {
        int minX = zone.pickedXForZoneStory - zone.pickedRzStory.minZoneWidth / 2 - 2;
        int minY = zone.pickedYForZoneStory - zone.pickedRzStory.minZoneHeight / 2 - 2;
        int maxX = zone.pickedXForZoneStory + zone.pickedRzStory.minZoneWidth / 2 + 2;
        int maxY = zone.pickedYForZoneStory + zone.pickedRzStory.minZoneHeight / 2 + 2;
        int chunkMinX = minX / 8;
        int chunkMinY = minY / 8;
        int chunkMaxX = maxX / 8;
        int chunkMaxY = maxY / 8;
        for (int cy = chunkMinY; cy <= chunkMaxY; ++cy) {
            for (int cx = chunkMinX; cx <= chunkMaxX; ++cx) {
                IsoChunk chunk;
                IsoChunk isoChunk = chunk = GameServer.server ? ServerMap.instance.getChunk(cx, cy) : IsoWorld.instance.currentCell.getChunk(cx, cy);
                if (chunk != null && chunk.loaded) continue;
                return false;
            }
        }
        return true;
    }

    public void randomizeZoneStory(Zone zone) {
    }

    public boolean isValid() {
        return true;
    }

    public void cleanAreaForStory(RandomizedZoneStoryBase rzs, Zone zone) {
        int x = zone.pickedXForZoneStory - rzs.minZoneWidth / 2 - 1;
        int y = zone.pickedYForZoneStory - rzs.minZoneHeight / 2 - 1;
        int x2 = zone.pickedXForZoneStory + rzs.minZoneWidth / 2 + 1;
        int y2 = zone.pickedYForZoneStory + rzs.minZoneHeight / 2 + 1;
        for (int xDel = x; xDel < x2; ++xDel) {
            for (int yDel = y; yDel < y2; ++yDel) {
                IsoObject obj;
                int i;
                IsoGridSquare sq = IsoWorld.instance.getCell().getGridSquare(xDel, yDel, zone.z);
                if (sq == null) continue;
                sq.removeBlood(false, false);
                for (i = sq.getObjects().size() - 1; i >= 0; --i) {
                    obj = sq.getObjects().get(i);
                    if (sq.getFloor() == obj) continue;
                    if (obj.getSprite() != null && obj.getSprite().getName() != null && obj.getSprite().getName().contains("vegetation")) {
                        sq.RemoveTileObject(obj);
                    }
                    if (obj.getSprite() == null || obj.getSprite().getProperties() == null || obj.getSprite().getProperties().get("tree") == null) continue;
                    sq.RemoveTileObject(obj);
                }
                for (i = sq.getSpecialObjects().size() - 1; i >= 0; --i) {
                    obj = sq.getSpecialObjects().get(i);
                    sq.RemoveTileObject(obj);
                }
                for (i = sq.getStaticMovingObjects().size() - 1; i >= 0; --i) {
                    IsoMovingObject isoMovingObject = sq.getStaticMovingObjects().get(i);
                    if (!(isoMovingObject instanceof IsoDeadBody)) continue;
                    IsoDeadBody deadBody = (IsoDeadBody)isoMovingObject;
                    sq.removeCorpse(deadBody, false);
                }
                sq.RecalcProperties();
                sq.RecalcAllWithNeighbours(true);
            }
        }
    }

    public static void cleanSquareForStory(IsoGridSquare sq) {
        IsoObject obj;
        int i;
        if (sq == null) {
            return;
        }
        sq.removeBlood(false, false);
        for (i = sq.getObjects().size() - 1; i >= 0; --i) {
            obj = sq.getObjects().get(i);
            if (sq.getFloor() == obj || obj.getSprite() == null || obj.getSprite().getProperties() == null || obj.getSprite().getName() == null) continue;
            sq.RemoveTileObject(obj);
        }
        for (i = sq.getSpecialObjects().size() - 1; i >= 0; --i) {
            obj = sq.getSpecialObjects().get(i);
            sq.RemoveTileObject(obj);
        }
        for (i = sq.getStaticMovingObjects().size() - 1; i >= 0; --i) {
            IsoMovingObject isoMovingObject = sq.getStaticMovingObjects().get(i);
            if (!(isoMovingObject instanceof IsoDeadBody)) continue;
            IsoDeadBody deadBody = (IsoDeadBody)isoMovingObject;
            sq.removeCorpse(deadBody, false);
        }
        sq.RecalcProperties();
        sq.RecalcAllWithNeighbours(true);
    }

    public int getMinimumWidth() {
        return this.minZoneWidth;
    }

    public int getMinimumHeight() {
        return this.minZoneHeight;
    }

    static {
        rzsMap = new HashMap();
    }

    public static enum ZoneType {
        Forest,
        Beach,
        Lake,
        Baseball,
        MusicFestStage,
        MusicFest,
        NewsStory,
        Duke,
        FrankHemingway,
        KirstyKormick,
        SirTwiggy,
        JackieJaye;

    }
}

