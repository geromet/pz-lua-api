/*
 * Decompiled with CFR 0.152.
 */
package zombie.randomizedWorld.randomizedVehicleStory;

import java.util.ArrayList;
import zombie.UsedFromLua;
import zombie.core.math.PZMath;
import zombie.core.random.Rand;
import zombie.iso.IsoChunk;
import zombie.iso.IsoDirections;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoObject;
import zombie.iso.Vector2;
import zombie.iso.zones.Zone;
import zombie.randomizedWorld.randomizedBuilding.RandomizedBuildingBase;
import zombie.randomizedWorld.randomizedVehicleStory.RandomizedVehicleStoryBase;
import zombie.randomizedWorld.randomizedVehicleStory.VehicleStorySpawner;
import zombie.vehicles.BaseVehicle;

@UsedFromLua
public final class RVSConstructionSite
extends RandomizedVehicleStoryBase {
    private final ArrayList<String> tools;

    public RVSConstructionSite() {
        this.name = "Construction Site";
        this.minZoneWidth = 6;
        this.minZoneHeight = 6;
        this.setChance(30);
        this.tools = new ArrayList();
        this.tools.add("Base.PickAxe");
        this.tools.add("Base.Shovel");
        this.tools.add("Base.Shovel2");
        this.tools.add("Base.Hammer");
        this.tools.add("Base.LeadPipe");
        this.tools.add("Base.PipeWrench");
        this.tools.add("Base.Sledgehammer");
        this.tools.add("Base.Sledgehammer2");
        this.needsPavement = true;
    }

    @Override
    public void randomizeVehicleStory(Zone zone, IsoChunk chunk) {
        this.callVehicleStorySpawner(zone, chunk, 0.0f);
    }

    @Override
    public boolean initVehicleStorySpawner(Zone zone, IsoChunk chunk, boolean debug) {
        VehicleStorySpawner spawner = VehicleStorySpawner.getInstance();
        spawner.clear();
        boolean manholeOnRight = Rand.NextBool(2);
        if (debug) {
            manholeOnRight = true;
        }
        int r = manholeOnRight ? 1 : -1;
        Vector2 v = IsoDirections.N.ToVector();
        float randAngle = 0.5235988f;
        if (debug) {
            randAngle = 0.0f;
        }
        v.rotate(Rand.Next(-randAngle, randAngle));
        spawner.addElement("vehicle1", (float)(-r) * 2.0f, 0.0f, v.getDirection(), 2.0f, 5.0f);
        float direction = 0.0f;
        spawner.addElement("manhole", (float)r * 1.5f, 1.5f, direction, 3.0f, 3.0f);
        int nbrOfTools = Rand.Next(0, 3);
        for (int i = 0; i < nbrOfTools; ++i) {
            direction = 0.0f;
            spawner.addElement("tool", (float)r * Rand.Next(0.0f, 3.0f), -Rand.Next(0.7f, 2.3f), direction, 1.0f, 1.0f);
        }
        spawner.setParameter("zone", zone);
        return true;
    }

    @Override
    public void spawnElement(VehicleStorySpawner spawner, VehicleStorySpawner.Element element) {
        IsoGridSquare square = element.square;
        if (square == null) {
            return;
        }
        float xoff = PZMath.max(element.position.x - (float)square.x, 0.001f);
        float yoff = PZMath.max(element.position.y - (float)square.y, 0.001f);
        float zoff = 0.0f;
        float z = element.z;
        Zone zone = spawner.getParameter("zone", Zone.class);
        switch (element.id) {
            case "manhole": {
                square.AddTileObject(IsoObject.getNew(square, "street_decoration_01_15", null, false));
                IsoGridSquare sq = square.getAdjacentSquare(IsoDirections.E);
                if (sq != null) {
                    sq.AddTileObject(IsoObject.getNew(sq, "street_decoration_01_26", null, false));
                }
                if ((sq = square.getAdjacentSquare(IsoDirections.W)) != null) {
                    sq.AddTileObject(IsoObject.getNew(sq, "street_decoration_01_26", null, false));
                }
                if ((sq = square.getAdjacentSquare(IsoDirections.S)) != null) {
                    sq.AddTileObject(IsoObject.getNew(sq, "street_decoration_01_26", null, false));
                }
                if ((sq = square.getAdjacentSquare(IsoDirections.N)) == null) break;
                sq.AddTileObject(IsoObject.getNew(sq, "street_decoration_01_26", null, false));
                break;
            }
            case "tool": {
                String itemType = this.tools.get(Rand.Next(this.tools.size()));
                RandomizedBuildingBase.trySpawnStoryItem(itemType, square, xoff, yoff, 0.0f);
                break;
            }
            case "vehicle1": {
                ArrayList<String> vehicles = new ArrayList<String>();
                vehicles.add("Base.PickUpTruck");
                vehicles.add("Base.VanUtility");
                String vehicleType = (String)vehicles.get(Rand.Next(vehicles.size()));
                BaseVehicle vehicle1 = this.addVehicle(zone, element.position.x, element.position.y, z, element.direction, null, vehicleType, null, "ConstructionWorker", true);
                if (vehicle1 == null) break;
                vehicle1.setAlarmed(false);
                String victimOutfit = "ConstructionWorker";
                if (vehicle1.getZombieType() != null) {
                    victimOutfit = vehicle1.getRandomZombieType();
                }
                this.addZombiesOnVehicle(Rand.Next(2, 5), victimOutfit, 0, vehicle1);
                this.addZombiesOnVehicle(1, "Foreman", 0, vehicle1);
                spawner.setParameter("vehicle1", vehicle1);
                break;
            }
        }
    }
}

