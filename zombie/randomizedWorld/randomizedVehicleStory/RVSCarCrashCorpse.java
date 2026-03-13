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
public final class RVSCarCrashCorpse
extends RandomizedVehicleStoryBase {
    public RVSCarCrashCorpse() {
        this.name = "Basic Car Crash Corpse";
        this.minZoneWidth = 6;
        this.minZoneHeight = 11;
        this.setChance(100);
    }

    @Override
    public void randomizeVehicleStory(Zone zone, IsoChunk chunk) {
        float randAngle = 0.5235988f;
        this.callVehicleStorySpawner(zone, chunk, Rand.Next(-0.5235988f, 0.5235988f));
    }

    @Override
    public boolean initVehicleStorySpawner(Zone zone, IsoChunk chunk, boolean debug) {
        VehicleStorySpawner spawner = VehicleStorySpawner.getInstance();
        spawner.clear();
        Vector2 v = IsoDirections.N.ToVector();
        float vehicleY = 2.5f;
        spawner.addElement("vehicle1", 0.0f, 2.5f, v.getDirection(), 2.0f, 5.0f);
        spawner.addElement("corpse", 0.0f, 2.5f - (float)(debug ? 7 : Rand.Next(4, 7)), v.getDirection() + (float)Math.PI, 1.0f, 2.0f);
        spawner.setParameter("zone", zone);
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
        BaseVehicle vehicle1 = spawner.getParameter("vehicle1", BaseVehicle.class);
        switch (element.id) {
            case "corpse": {
                if (vehicle1 == null) break;
                RVSCarCrashCorpse.createRandomDeadBody(element.position.x, element.position.y, element.z, element.direction, false, 35, 30, null);
                this.addTrailOfBlood(element.position.x, element.position.y, element.z, element.direction, 15);
                break;
            }
            case "vehicle1": {
                vehicle1 = this.addVehicle(zone, element.position.x, element.position.y, z, element.direction, "bad", null, null, null, true);
                if (vehicle1 == null) break;
                vehicle1.setAlarmed(false);
                vehicle1 = vehicle1.setSmashed("Front");
                vehicle1.setBloodIntensity("Front", 1.0f);
                spawner.setParameter("vehicle1", vehicle1);
            }
        }
    }
}

