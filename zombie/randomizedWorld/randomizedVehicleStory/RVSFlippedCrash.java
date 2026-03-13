/*
 * Decompiled with CFR 0.152.
 */
package zombie.randomizedWorld.randomizedVehicleStory;

import java.util.ArrayList;
import zombie.UsedFromLua;
import zombie.characters.IsoZombie;
import zombie.core.random.Rand;
import zombie.iso.IsoChunk;
import zombie.iso.IsoDirections;
import zombie.iso.IsoGridSquare;
import zombie.iso.Vector2;
import zombie.iso.objects.IsoDeadBody;
import zombie.iso.zones.Zone;
import zombie.randomizedWorld.randomizedVehicleStory.RandomizedVehicleStoryBase;
import zombie.randomizedWorld.randomizedVehicleStory.VehicleStorySpawner;
import zombie.vehicles.BaseVehicle;

@UsedFromLua
public final class RVSFlippedCrash
extends RandomizedVehicleStoryBase {
    public RVSFlippedCrash() {
        this.name = "Flipped Crash";
        this.minZoneWidth = 8;
        this.minZoneHeight = 8;
        this.setChance(40);
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
        spawner.addElement("vehicle1", 0.0f, 0.0f, v.getDirection(), 2.0f, 5.0f);
        spawner.setParameter("zone", zone);
        spawner.setParameter("burnt", Rand.NextBool(5));
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
        boolean burnt = spawner.getParameterBoolean("burnt");
        switch (element.id) {
            case "vehicle1": {
                BaseVehicle vehicle1 = this.addVehicleFlipped(zone, element.position.x, element.position.y, z + 0.25f, element.direction, burnt ? "normalburnt" : "bad", null, null, null);
                if (vehicle1 == null) break;
                vehicle1.setAlarmed(false);
                int rand = Rand.Next(4);
                String area = null;
                switch (rand) {
                    case 0: {
                        area = "Front";
                        break;
                    }
                    case 1: {
                        area = "Rear";
                        break;
                    }
                    case 2: {
                        area = "Left";
                        break;
                    }
                    case 3: {
                        area = "Right";
                    }
                }
                vehicle1 = vehicle1.setSmashed(area);
                vehicle1.setBloodIntensity("Front", Rand.Next(0.3f, 1.0f));
                vehicle1.setBloodIntensity("Rear", Rand.Next(0.3f, 1.0f));
                vehicle1.setBloodIntensity("Left", Rand.Next(0.3f, 1.0f));
                vehicle1.setBloodIntensity("Right", Rand.Next(0.3f, 1.0f));
                ArrayList<IsoZombie> zedList = this.addZombiesOnVehicle(Rand.Next(2, 4), null, null, vehicle1);
                if (zedList == null) break;
                for (int i = 0; i < zedList.size(); ++i) {
                    IsoZombie zombie = zedList.get(i);
                    this.addBloodSplat(zombie.getSquare(), Rand.Next(10, 20));
                    if (burnt) {
                        zombie.setSkeleton(true);
                        zombie.getHumanVisual().setSkinTextureIndex(0);
                    } else {
                        zombie.DoCorpseInventory();
                        if (Rand.NextBool(10)) {
                            zombie.setFakeDead(true);
                            zombie.crawling = true;
                            zombie.setCanWalk(false);
                            zombie.setCrawlerType(1);
                        }
                    }
                    new IsoDeadBody(zombie, false);
                }
                break;
            }
        }
    }
}

