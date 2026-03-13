/*
 * Decompiled with CFR 0.152.
 */
package zombie.randomizedWorld.randomizedVehicleStory;

import zombie.UsedFromLua;
import zombie.core.random.Rand;
import zombie.iso.IsoChunk;
import zombie.iso.IsoDirections;
import zombie.iso.IsoGridSquare;
import zombie.iso.Vector2;
import zombie.iso.zones.Zone;
import zombie.randomizedWorld.randomizedVehicleStory.RandomizedVehicleStoryBase;
import zombie.randomizedWorld.randomizedVehicleStory.VehicleStorySpawner;
import zombie.vehicles.BaseVehicle;

@UsedFromLua
public final class RVSCarCrash
extends RandomizedVehicleStoryBase {
    public RVSCarCrash() {
        this.name = "Basic Car Crash";
        this.minZoneWidth = 5;
        this.minZoneHeight = 7;
        this.setChance(250);
    }

    @Override
    public void randomizeVehicleStory(Zone zone, IsoChunk chunk) {
        this.callVehicleStorySpawner(zone, chunk, 0.0f);
    }

    @Override
    public boolean initVehicleStorySpawner(Zone zone, IsoChunk chunk, boolean debug) {
        VehicleStorySpawner spawner = VehicleStorySpawner.getInstance();
        spawner.clear();
        float randAngle = 0.5235988f;
        if (debug) {
            randAngle = 0.0f;
        }
        Vector2 v = IsoDirections.N.ToVector();
        v.rotate(Rand.Next(-randAngle, randAngle));
        spawner.addElement("vehicle1", 0.0f, 1.0f, v.getDirection(), 2.0f, 5.0f);
        boolean east = Rand.NextBool(2);
        v = east ? IsoDirections.E.ToVector() : IsoDirections.W.ToVector();
        v.rotate(Rand.Next(-randAngle, randAngle));
        spawner.addElement("vehicle2", 0.0f, -2.5f, v.getDirection(), 2.0f, 5.0f);
        spawner.setParameter("zone", zone);
        spawner.setParameter("smashed", Rand.NextBool(3));
        spawner.setParameter("east", east);
        return true;
    }

    @Override
    public void spawnElement(VehicleStorySpawner spawner, VehicleStorySpawner.Element element) {
        IsoGridSquare square = element.square;
        if (square == null) {
            return;
        }
        float z = element.z;
        Zone zone = spawner.getParameter("zone", Zone.class);
        boolean smashed = spawner.getParameterBoolean("smashed");
        boolean vehicle2East = spawner.getParameterBoolean("east");
        switch (element.id) {
            case "vehicle1": 
            case "vehicle2": {
                BaseVehicle vehicle = this.addVehicle(zone, element.position.x, element.position.y, z, element.direction, "bad", null, null, null, true);
                if (vehicle == null) break;
                vehicle.setAlarmed(false);
                if (smashed) {
                    String location = "Front";
                    if ("vehicle2".equals(element.id)) {
                        location = vehicle2East ? "Right" : "Left";
                    }
                    vehicle = vehicle.setSmashed(location);
                    vehicle.setBloodIntensity(location, 1.0f);
                }
                if (!"vehicle1".equals(element.id) || Rand.Next(10) >= 4) break;
                String victimOutfit = null;
                if (vehicle.getZombieType() != null) {
                    victimOutfit = vehicle.getRandomZombieType();
                }
                this.addZombiesOnVehicle(Rand.Next(2, 5), victimOutfit, null, vehicle);
            }
        }
    }
}

