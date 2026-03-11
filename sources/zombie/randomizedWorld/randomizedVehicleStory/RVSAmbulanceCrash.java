/*
 * Decompiled with CFR 0.152.
 */
package zombie.randomizedWorld.randomizedVehicleStory;

import java.util.ArrayList;
import zombie.UsedFromLua;
import zombie.characters.BodyDamage.BodyPartType;
import zombie.characters.IsoZombie;
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
public final class RVSAmbulanceCrash
extends RandomizedVehicleStoryBase {
    public RVSAmbulanceCrash() {
        this.name = "Ambulance Crash";
        this.minZoneWidth = 5;
        this.minZoneHeight = 7;
        this.setChance(50);
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
        switch (element.id) {
            case "vehicle1": {
                BaseVehicle vehicle1 = this.addVehicle(zone, element.position.x, element.position.y, z, element.direction, null, "Base.VanAmbulance", null, null, true);
                if (vehicle1 == null) break;
                vehicle1.setAlarmed(false);
                this.addZombiesOnVehicle(Rand.Next(1, 3), "AmbulanceDriver", null, vehicle1);
                ArrayList<IsoZombie> zeds = this.addZombiesOnVehicle(Rand.Next(1, 3), "HospitalPatient", null, vehicle1);
                for (int i = 0; i < zeds.size(); ++i) {
                    for (int j = 0; j < 7; ++j) {
                        if (!Rand.NextBool(2)) continue;
                        zeds.get(i).addVisualBandage(BodyPartType.getRandom(), true);
                    }
                }
                break;
            }
            case "vehicle2": {
                BaseVehicle vehicle2 = this.addVehicle(zone, element.position.x, element.position.y, z, element.direction, "bad", null, null, null);
                if (vehicle2 == null) break;
                vehicle2.setAlarmed(false);
                break;
            }
        }
    }
}

