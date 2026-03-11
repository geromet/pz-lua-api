/*
 * Decompiled with CFR 0.152.
 */
package zombie.randomizedWorld.randomizedVehicleStory;

import zombie.UsedFromLua;
import zombie.core.math.PZMath;
import zombie.core.random.Rand;
import zombie.inventory.InventoryItem;
import zombie.iso.IsoChunk;
import zombie.iso.IsoDirections;
import zombie.iso.IsoGridSquare;
import zombie.iso.Vector2;
import zombie.iso.zones.Zone;
import zombie.randomizedWorld.randomizedBuilding.RandomizedBuildingBase;
import zombie.randomizedWorld.randomizedVehicleStory.RandomizedVehicleStoryBase;
import zombie.randomizedWorld.randomizedVehicleStory.VehicleStorySpawner;
import zombie.vehicles.BaseVehicle;
import zombie.vehicles.VehiclePart;

@UsedFromLua
public final class RVSChangingTire
extends RandomizedVehicleStoryBase {
    public RVSChangingTire() {
        this.name = "Changing Tire";
        this.minZoneWidth = 5;
        this.minZoneHeight = 5;
        this.setChance(30);
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
        boolean removeRightWheel = Rand.NextBool(2);
        if (debug) {
            removeRightWheel = true;
        }
        int r = removeRightWheel ? 1 : -1;
        Vector2 v = IsoDirections.N.ToVector();
        spawner.addElement("vehicle1", (float)r * -1.5f, 0.0f, v.getDirection(), 2.0f, 5.0f);
        spawner.addElement("tire1", (float)r * 0.0f, 0.0f, 0.0f, 1.0f, 1.0f);
        spawner.addElement("tool1", (float)r * 0.8f, -0.2f, 0.0f, 1.0f, 1.0f);
        spawner.addElement("tool2", (float)r * 1.2f, 0.2f, 0.0f, 1.0f, 1.0f);
        spawner.addElement("tire2", (float)r * 2.0f, 0.0f, 0.0f, 1.0f, 1.0f);
        spawner.setParameter("zone", zone);
        spawner.setParameter("removeRightWheel", removeRightWheel);
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
        boolean removeRightWheel = spawner.getParameterBoolean("removeRightWheel");
        BaseVehicle vehicle1 = spawner.getParameter("vehicle1", BaseVehicle.class);
        switch (element.id) {
            case "tire1": {
                if (vehicle1 == null) break;
                InventoryItem newTire = RandomizedBuildingBase.trySpawnStoryItem("Base.ModernTire" + vehicle1.getScript().getMechanicType(), square, xoff, yoff, 0.0f);
                if (newTire != null) {
                    newTire.setItemCapacity(newTire.getMaxCapacity());
                }
                this.addBloodSplat(square, Rand.Next(10, 20));
                break;
            }
            case "tire2": {
                InventoryItem oldTire;
                if (vehicle1 == null || (oldTire = RandomizedBuildingBase.trySpawnStoryItem("Base.OldTire" + vehicle1.getScript().getMechanicType(), square, xoff, yoff, 0.0f)) == null) break;
                oldTire.setCondition(0, false);
                break;
            }
            case "tool1": {
                if (Rand.Next(2) == 0) {
                    RandomizedBuildingBase.trySpawnStoryItem("Base.LugWrench", square, xoff, yoff, 0.0f);
                    break;
                }
                RandomizedBuildingBase.trySpawnStoryItem("Base.TireIron", square, xoff, yoff, 0.0f);
                break;
            }
            case "tool2": {
                RandomizedBuildingBase.trySpawnStoryItem("Base.Jack", square, xoff, yoff, 0.0f);
                break;
            }
            case "vehicle1": {
                vehicle1 = this.addVehicle(zone, element.position.x, element.position.y, z, element.direction, "good", null, null, null, true);
                if (vehicle1 == null) break;
                vehicle1.setAlarmed(false);
                vehicle1.setGeneralPartCondition(0.7f, 40.0f);
                vehicle1.setRust(0.0f);
                VehiclePart part = vehicle1.getPartById(removeRightWheel ? "TireRearRight" : "TireRearLeft");
                vehicle1.setTireRemoved(part.getWheelIndex(), true);
                part.setModelVisible("InflatedTirePlusWheel", false);
                part.setInventoryItem(null);
                this.addZombiesOnVehicle(2, null, null, vehicle1);
                spawner.setParameter("vehicle1", vehicle1);
                break;
            }
        }
    }
}

