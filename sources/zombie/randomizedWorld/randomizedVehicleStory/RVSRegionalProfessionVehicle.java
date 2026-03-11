/*
 * Decompiled with CFR 0.152.
 */
package zombie.randomizedWorld.randomizedVehicleStory;

import zombie.Lua.LuaManager;
import zombie.UsedFromLua;
import zombie.core.random.Rand;
import zombie.inventory.ItemPickerJava;
import zombie.iso.IsoChunk;
import zombie.iso.IsoDirections;
import zombie.iso.IsoGridSquare;
import zombie.iso.Vector2;
import zombie.iso.zones.Zone;
import zombie.randomizedWorld.randomizedVehicleStory.RandomizedVehicleStoryBase;
import zombie.randomizedWorld.randomizedVehicleStory.VehicleStorySpawner;

@UsedFromLua
public final class RVSRegionalProfessionVehicle
extends RandomizedVehicleStoryBase {
    public RVSRegionalProfessionVehicle() {
        this.name = "Regional Profession Vehicle - Will not always spawn a vehicle due to unique vehicle control";
        this.minZoneWidth = 2;
        this.minZoneHeight = 5;
        this.setChance(30);
        this.needsRegion = true;
    }

    @Override
    public void randomizeVehicleStory(Zone zone, IsoChunk chunk) {
        this.callVehicleStorySpawner(zone, chunk, 0.0f);
    }

    @Override
    public boolean initVehicleStorySpawner(Zone zone, IsoChunk chunk, boolean debug) {
        VehicleStorySpawner spawner = VehicleStorySpawner.getInstance();
        spawner.clear();
        Vector2 v = IsoDirections.N.ToVector();
        float randAngle = 0.5235988f;
        if (debug) {
            randAngle = 0.0f;
        }
        v.rotate(Rand.Next(-randAngle, randAngle));
        spawner.addElement("vehicle1", 0.0f, 0.0f, v.getDirection(), 2.0f, 5.0f);
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
                String region = ItemPickerJava.getSquareRegion(square);
                Object functionObj = LuaManager.getFunctionObject("ProfessionVehicles.OnCreateRegion");
                if (functionObj == null) break;
                LuaManager.caller.pcallvoid(LuaManager.thread, functionObj, region, square, (Object)IsoDirections.fromAngle(element.direction));
            }
        }
    }
}

