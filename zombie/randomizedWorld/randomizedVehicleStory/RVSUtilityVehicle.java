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
import zombie.iso.Vector2;
import zombie.iso.zones.Zone;
import zombie.randomizedWorld.randomizedBuilding.RandomizedBuildingBase;
import zombie.randomizedWorld.randomizedVehicleStory.RandomizedVehicleStoryBase;
import zombie.randomizedWorld.randomizedVehicleStory.VehicleStorySpawner;
import zombie.util.list.PZArrayUtil;
import zombie.vehicles.BaseVehicle;

@UsedFromLua
public final class RVSUtilityVehicle
extends RandomizedVehicleStoryBase {
    private final Params params = new Params();

    public RVSUtilityVehicle() {
        this.name = "Utility Vehicle";
        this.minZoneWidth = 8;
        this.minZoneHeight = 9;
        this.setChance(70);
    }

    @Override
    public void randomizeVehicleStory(Zone zone, IsoChunk chunk) {
        this.callVehicleStorySpawner(zone, chunk, 0.0f);
    }

    public void doUtilityVehicle(Zone zone, IsoChunk chunk, String zoneName, String scriptName, String outfits, Integer femaleChance, String vehicleDistrib, ArrayList<String> items, int nbrOfItem, boolean addTrailer) {
        this.params.zoneName = zoneName;
        this.params.scriptName = scriptName;
        this.params.outfits = outfits;
        this.params.femaleChance = femaleChance;
        this.params.vehicleDistrib = vehicleDistrib;
        this.params.items = items;
        this.params.nbrOfItem = nbrOfItem;
        this.params.addTrailer = addTrailer;
    }

    @Override
    public boolean initVehicleStorySpawner(Zone zone, IsoChunk chunk, boolean debug) {
        int utilityType = Rand.Next(0, 7);
        switch (utilityType) {
            case 0: {
                this.doUtilityVehicle(zone, chunk, null, "Base.VanUtility", "ConstructionWorker", 0, "ConstructionWorker", this.getUtilityToolClutter(), Rand.Next(0, 3), true);
                break;
            }
            case 1: {
                this.doUtilityVehicle(zone, chunk, "police", null, "Police", null, null, null, 0, false);
                break;
            }
            case 2: {
                this.doUtilityVehicle(zone, chunk, "fire", null, "Fireman", null, null, null, 0, false);
                break;
            }
            case 3: {
                this.doUtilityVehicle(zone, chunk, "ranger", null, "Ranger", null, null, null, 0, true);
                break;
            }
            case 4: {
                this.doUtilityVehicle(zone, chunk, "carpenter", null, "ConstructionWorker", 0, "Carpenter", this.getCarpentryToolClutter(), Rand.Next(2, 6), true);
                break;
            }
            case 5: {
                this.doUtilityVehicle(zone, chunk, "postal", null, "Postal", null, null, null, 0, false);
                break;
            }
            case 6: {
                this.doUtilityVehicle(zone, chunk, "fossoil", null, "Fossoil", null, null, null, 0, false);
            }
        }
        VehicleStorySpawner spawner = VehicleStorySpawner.getInstance();
        spawner.clear();
        Vector2 v = IsoDirections.N.ToVector();
        float randAngle = 0.5235988f;
        if (debug) {
            randAngle = 0.0f;
        }
        v.rotate(Rand.Next(-randAngle, randAngle));
        float vehicleY = -2.0f;
        int vehicleLength = 5;
        spawner.addElement("vehicle1", 0.0f, -2.0f, v.getDirection(), 2.0f, 5.0f);
        if (this.params.addTrailer && Rand.NextBool(7)) {
            int trailerLength = 3;
            spawner.addElement("trailer", 0.0f, 3.0f, v.getDirection(), 2.0f, 3.0f);
        }
        if (this.params.items != null) {
            for (int i = 0; i < this.params.nbrOfItem; ++i) {
                spawner.addElement("tool", Rand.Next(-3.5f, 3.5f), Rand.Next(-3.5f, 3.5f), 0.0f, 1.0f, 1.0f);
            }
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
        BaseVehicle vehicle1 = spawner.getParameter("vehicle1", BaseVehicle.class);
        switch (element.id) {
            case "tool": {
                if (vehicle1 == null) break;
                float xoff = PZMath.max(element.position.x - (float)square.x, 0.001f);
                float yoff = PZMath.max(element.position.y - (float)square.y, 0.001f);
                float zoff = 0.0f;
                RandomizedBuildingBase.trySpawnStoryItem(PZArrayUtil.pickRandom(this.params.items), square, xoff, yoff, 0.0f);
                break;
            }
            case "trailer": {
                if (vehicle1 == null) break;
                this.addTrailer(vehicle1, zone, square.getChunk(), this.params.zoneName, this.params.vehicleDistrib, Rand.NextBool(1) ? "Base.Trailer" : "Base.TrailerCover");
                break;
            }
            case "vehicle1": {
                vehicle1 = this.addVehicle(zone, element.position.x, element.position.y, z, element.direction, this.params.zoneName, this.params.scriptName, null, this.params.vehicleDistrib, true);
                if (vehicle1 == null) break;
                vehicle1.setAlarmed(false);
                String victimOutfit = this.params.outfits;
                if (vehicle1.getZombieType() != null) {
                    victimOutfit = vehicle1.getRandomZombieType();
                }
                this.addZombiesOnVehicle(Rand.Next(2, 5), victimOutfit, this.params.femaleChance, vehicle1);
                break;
            }
        }
    }

    private static final class Params {
        String zoneName;
        String scriptName;
        String outfits;
        Integer femaleChance;
        String vehicleDistrib;
        ArrayList<String> items;
        int nbrOfItem;
        boolean addTrailer;

        private Params() {
        }
    }
}

