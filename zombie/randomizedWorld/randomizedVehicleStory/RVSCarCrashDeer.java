/*
 * Decompiled with CFR 0.152.
 */
package zombie.randomizedWorld.randomizedVehicleStory;

import zombie.UsedFromLua;
import zombie.characters.animals.IsoAnimal;
import zombie.core.random.Rand;
import zombie.iso.IsoChunk;
import zombie.iso.IsoDirections;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoWorld;
import zombie.iso.Vector2;
import zombie.iso.zones.Zone;
import zombie.randomizedWorld.randomizedVehicleStory.RandomizedVehicleStoryBase;
import zombie.randomizedWorld.randomizedVehicleStory.VehicleStorySpawner;
import zombie.vehicles.BaseVehicle;

@UsedFromLua
public final class RVSCarCrashDeer
extends RandomizedVehicleStoryBase {
    public RVSCarCrashDeer() {
        this.name = "Car Crash Deer";
        this.minZoneWidth = 5;
        this.minZoneHeight = 11;
        this.setChance(10);
        this.needsRuralVegetation = true;
        this.notTown = true;
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
                String deer = "doe";
                if (Rand.Next(2) == 0) {
                    deer = "buck";
                }
                IsoAnimal animal = new IsoAnimal(IsoWorld.instance.getCell(), (int)element.position.x, (int)element.position.y, 0, deer, "whitetailed");
                animal.randomizeAge();
                animal.setHealth(0.0f);
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

