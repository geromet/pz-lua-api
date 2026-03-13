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
public final class RVSBanditRoad
extends RandomizedVehicleStoryBase {
    public RVSBanditRoad() {
        this.name = "Bandits on Road";
        this.minZoneWidth = 7;
        this.minZoneHeight = 9;
        this.setMinimumDays(30);
        this.setChance(30);
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
        spawner.addElement("vehicle1", 0.0f, 2.0f, v.getDirection(), 2.0f, 5.0f);
        boolean east = Rand.NextBool(2);
        v = east ? IsoDirections.E.ToVector() : IsoDirections.W.ToVector();
        v.rotate(Rand.Next(-randAngle, randAngle));
        float vehicle2X = 0.0f;
        float vehicle2Y = -1.5f;
        spawner.addElement("vehicle2", 0.0f, -1.5f, v.getDirection(), 2.0f, 5.0f);
        int numCorpses = Rand.Next(3, 6);
        for (int i = 0; i < numCorpses; ++i) {
            float x = Rand.Next(-3.0f, 3.0f);
            float y = Rand.Next(-4.5f, 1.5f);
            spawner.addElement("corpse", x, y, Rand.Next(0.0f, (float)Math.PI * 2), 1.0f, 2.0f);
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
        float z = element.z;
        Zone zone = spawner.getParameter("zone", Zone.class);
        switch (element.id) {
            case "corpse": {
                BaseVehicle vehicle1 = spawner.getParameter("vehicle1", BaseVehicle.class);
                if (vehicle1 == null) break;
                RVSBanditRoad.createRandomDeadBody(element.position.x, element.position.y, element.z, element.direction, false, 6, 0, null);
                this.addTrailOfBlood(element.position.x, element.position.y, element.z, Vector2.getDirection(element.position.x - vehicle1.getX(), element.position.y - vehicle1.getY()), 15);
                break;
            }
            case "vehicle1": {
                BaseVehicle vehicle1 = this.addVehicle(zone, element.position.x, element.position.y, z, element.direction, "bad", null, null, null, true);
                if (vehicle1 == null) break;
                vehicle1.setAlarmed(false);
                vehicle1 = vehicle1.setSmashed("Front");
                this.addZombiesOnVehicle(Rand.Next(3, 6), "Bandit", null, vehicle1);
                spawner.setParameter("vehicle1", vehicle1);
                break;
            }
            case "vehicle2": {
                BaseVehicle vehicle2 = this.addVehicle(zone, element.position.x, element.position.y, z, element.direction, "bad", null, null, null, true);
                if (vehicle2 == null) break;
                vehicle2.setAlarmed(false);
                this.addZombiesOnVehicle(Rand.Next(3, 5), null, null, vehicle2);
                spawner.setParameter("vehicle2", vehicle2);
                break;
            }
        }
    }
}

