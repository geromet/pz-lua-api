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
import zombie.vehicles.VehiclePart;

@UsedFromLua
public final class RVSPoliceBlockade
extends RandomizedVehicleStoryBase {
    public RVSPoliceBlockade() {
        this.name = "Police Blockade";
        this.minZoneWidth = 8;
        this.minZoneHeight = 8;
        this.setChance(30);
        this.setMaximumDays(30);
    }

    @Override
    public void randomizeVehicleStory(Zone zone, IsoChunk chunk) {
        this.callVehicleStorySpawner(zone, chunk, 0.0f);
    }

    @Override
    public boolean initVehicleStorySpawner(Zone zone, IsoChunk chunk, boolean debug) {
        VehicleStorySpawner spawner = VehicleStorySpawner.getInstance();
        spawner.clear();
        float randAngle = 0.17453292f;
        if (debug) {
            randAngle = 0.0f;
        }
        float xOffset = 1.5f;
        float yOffset = 1.0f;
        if (this.zoneWidth >= 10) {
            xOffset = 2.5f;
            yOffset = 0.0f;
        }
        IsoDirections firstCarDir = Rand.NextBool(2) ? IsoDirections.W : IsoDirections.E;
        Vector2 v = firstCarDir.ToVector();
        v.rotate(Rand.Next(-randAngle, randAngle));
        spawner.addElement("vehicle1", -xOffset, yOffset, v.getDirection(), 2.0f, 5.0f);
        v = firstCarDir.Rot180().ToVector();
        v.rotate(Rand.Next(-randAngle, randAngle));
        spawner.addElement("vehicle2", xOffset, -yOffset, v.getDirection(), 2.0f, 5.0f);
        String scriptName = "Base.CarLightsPolice";
        if (Rand.NextBool(3)) {
            scriptName = "Base.PickUpVanLightsPolice";
        }
        spawner.setParameter("zone", zone);
        spawner.setParameter("script", scriptName);
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
        String scriptName = spawner.getParameterString("script");
        switch (element.id) {
            case "vehicle1": 
            case "vehicle2": {
                BaseVehicle vehicle = this.addVehicle(zone, element.position.x, element.position.y, z, element.direction, null, scriptName, null, null, true);
                if (vehicle == null) break;
                vehicle.setAlarmed(false);
                if (Rand.NextBool(3)) {
                    vehicle.setHeadlightsOn(true);
                    vehicle.setLightbarLightsMode(2);
                    VehiclePart battery = vehicle.getBattery();
                    if (battery != null) {
                        battery.setLastUpdated(0.0f);
                    }
                }
                String outfit = "Police";
                if (vehicle.getZombieType() != null) {
                    outfit = vehicle.getRandomZombieType();
                }
                this.addZombiesOnVehicle(Rand.Next(2, 4), outfit, null, vehicle);
            }
        }
    }
}

