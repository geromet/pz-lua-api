/*
 * Decompiled with CFR 0.152.
 */
package zombie.randomizedWorld.randomizedVehicleStory;

import java.util.ArrayList;
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
public final class RVSTrailerCrash
extends RandomizedVehicleStoryBase {
    public RVSTrailerCrash() {
        this.name = "Trailer Crash";
        this.minZoneWidth = 5;
        this.minZoneHeight = 12;
        this.setChance(45);
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
        float vehicle1X = 0.0f;
        float vehicle1Y = -1.5f;
        spawner.addElement("vehicle1", 0.0f, -1.5f, v.getDirection(), 2.0f, 5.0f);
        int trailerLength = 4;
        spawner.addElement("trailer", 0.0f, 4.0f, v.getDirection(), 2.0f, 4.0f);
        boolean east = Rand.NextBool(2);
        v = east ? IsoDirections.E.ToVector() : IsoDirections.W.ToVector();
        v.rotate(Rand.Next(-randAngle, randAngle));
        float vehicle2X = 0.0f;
        float vehicle2Y = -5.0f;
        spawner.addElement("vehicle2", 0.0f, -5.0f, v.getDirection(), 2.0f, 5.0f);
        spawner.setParameter("zone", zone);
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
        boolean east = spawner.getParameterBoolean("east");
        switch (element.id) {
            case "vehicle1": {
                BaseVehicle trailer;
                BaseVehicle vehicle1 = this.addVehicle(zone, element.position.x, element.position.y, z, element.direction, null, "Base.PickUpVan", null, null, true);
                if (vehicle1 == null) break;
                vehicle1.setAlarmed(false);
                vehicle1 = vehicle1.setSmashed("Front");
                ArrayList<String> trailers = new ArrayList<String>();
                trailers.add("Base.Trailer");
                trailers.add("Base.TrailerCover");
                trailers.add("Base.Trailer_Livestock");
                String trailerType = (String)trailers.get(Rand.Next(trailers.size()));
                if (Rand.NextBool(6)) {
                    trailerType = "Base.TrailerAdvert";
                }
                if ((trailer = this.addTrailer(vehicle1, zone, square.getChunk(), null, null, trailerType)) != null && Rand.NextBool(3)) {
                    trailer.setAngles(trailer.getAngleX(), Rand.Next(90.0f, 110.0f), trailer.getAngleZ());
                }
                if (Rand.Next(10) < 4) {
                    this.addZombiesOnVehicle(Rand.Next(2, 5), null, null, vehicle1);
                }
                spawner.setParameter("vehicle1", vehicle1);
                break;
            }
            case "vehicle2": {
                BaseVehicle vehicle2 = this.addVehicle(zone, element.position.x, element.position.y, z, element.direction, "bad", null, null, null);
                if (vehicle2 == null) break;
                vehicle2.setAlarmed(false);
                String location = east ? "Right" : "Left";
                vehicle2 = vehicle2.setSmashed(location);
                vehicle2.setBloodIntensity(location, 1.0f);
                spawner.setParameter("vehicle2", vehicle2);
                break;
            }
        }
    }
}

