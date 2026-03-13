/*
 * Decompiled with CFR 0.152.
 */
package zombie.randomizedWorld.randomizedVehicleStory;

import zombie.UsedFromLua;
import zombie.core.random.Rand;
import zombie.inventory.InventoryItem;
import zombie.inventory.InventoryItemFactory;
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
public final class RVSPlonkies
extends RandomizedVehicleStoryBase {
    public RVSPlonkies() {
        this.name = "Plonkies";
        this.minZoneWidth = 5;
        this.minZoneHeight = 5;
        this.setChance(1);
        this.setUnique(true);
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
        boolean damageRightWheel = Rand.NextBool(2);
        Vector2 v = IsoDirections.N.ToVector();
        float randAngle = 0.5235988f;
        if (debug) {
            randAngle = 0.0f;
        }
        v.rotate(Rand.Next(-randAngle, randAngle));
        spawner.addElement("vehicle1", (float)(-r) * 2.0f, 0.0f, v.getDirection(), 2.0f, 5.0f);
        float direction = 0.0f;
        int nbrOfBags = Rand.Next(2, 5);
        for (int i = 0; i < nbrOfBags; ++i) {
            spawner.addElement("bag", (float)r * Rand.Next(0.0f, 3.0f), -Rand.Next(0.7f, 2.3f), 0.0f, 1.0f, 1.0f);
        }
        spawner.setParameter("damageRightWheel", damageRightWheel);
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
        boolean damageRightWheel = spawner.getParameterBoolean("damageRightWheel");
        BaseVehicle vehicle1 = spawner.getParameter("vehicle1", BaseVehicle.class);
        switch (element.id) {
            case "bag": {
                if (vehicle1 == null) break;
                String itemType = "Base.Plonkies";
                Object bag = InventoryItemFactory.CreateItem("Base.Plonkies");
                this.addItemOnGround(square, (InventoryItem)bag);
                break;
            }
            case "vehicle1": {
                vehicle1 = this.addVehicle(zone, element.position.x, element.position.y, z, element.direction, null, "StepVan_Plonkies", null, null, true);
                if (vehicle1 == null) break;
                vehicle1.setAlarmed(false);
                vehicle1.setGeneralPartCondition(0.7f, 40.0f);
                vehicle1.setRust(0.0f);
                VehiclePart part = vehicle1.getPartById(damageRightWheel ? "TireRearRight" : "TireRearLeft");
                part.setCondition(0);
                VehiclePart part2 = vehicle1.getPartById("GasTank");
                part2.setContainerContentAmount(0.0f);
                String outfit = "PlonkiesGuy";
                this.addZombiesOnVehicle(1, "PlonkiesGuy", 0, vehicle1);
                spawner.setParameter("vehicle1", vehicle1);
                vehicle1.addKeyToWorld();
                break;
            }
        }
    }
}

