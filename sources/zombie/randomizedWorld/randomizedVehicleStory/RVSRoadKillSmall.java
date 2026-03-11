/*
 * Decompiled with CFR 0.152.
 */
package zombie.randomizedWorld.randomizedVehicleStory;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import zombie.UsedFromLua;
import zombie.characters.animals.AnimalDefinitions;
import zombie.characters.animals.IsoAnimal;
import zombie.characters.animals.datas.AnimalBreed;
import zombie.core.random.Rand;
import zombie.debug.DebugLog;
import zombie.iso.IsoChunk;
import zombie.iso.IsoDirections;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoWorld;
import zombie.iso.Vector2;
import zombie.iso.zones.Zone;
import zombie.randomizedWorld.randomizedVehicleStory.RandomizedVehicleStoryBase;
import zombie.randomizedWorld.randomizedVehicleStory.VehicleStorySpawner;

@UsedFromLua
public final class RVSRoadKillSmall
extends RandomizedVehicleStoryBase {
    public RVSRoadKillSmall() {
        this.name = "Roadkill - Small Animal Run Over By Vehicle";
        this.minZoneWidth = 4;
        this.minZoneHeight = 11;
        this.setChance(10);
        this.needsRuralVegetation = true;
        this.notTown = true;
    }

    public static ArrayList<String> getBreeds() {
        ArrayList<String> result = new ArrayList<String>();
        result.add("appalachian");
        result.add("cottontail");
        result.add("swamp");
        result.add("grey");
        result.add("meleagris");
        return result;
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
        spawner.addElement("corpse", 0.0f, 2.5f - (float)(debug ? 7 : Rand.Next(4, 7)), v.getDirection() + (float)Math.PI, 1.0f, 2.0f);
        spawner.setParameter("zone", zone);
        ArrayList<String> breeds = RVSRoadKillSmall.getBreeds();
        String breed = breeds.get(Rand.Next(breeds.size()));
        spawner.setParameter("breed", breed);
        return true;
    }

    @Override
    public void spawnElement(VehicleStorySpawner spawner, VehicleStorySpawner.Element element) {
        IsoGridSquare square = element.square;
        if (square == null) {
            return;
        }
        switch (element.id) {
            case "corpse": {
                AnimalDefinitions adef;
                LinkedHashMap<String, String> breedFemale = new LinkedHashMap<String, String>();
                LinkedHashMap<String, String> breedMale = new LinkedHashMap<String, String>();
                breedFemale.put("appalachian", "rabdoe");
                breedFemale.put("cottontail", "rabdoe");
                breedFemale.put("swamp", "rabdoe");
                breedFemale.put("meleagris", "turkeyhen");
                breedMale.put("appalachian", "rabbuck");
                breedMale.put("cottontail", "rabbuck");
                breedMale.put("swamp", "rabbuck");
                breedMale.put("meleagris", "gobblers");
                String breed = spawner.getParameterString("breed");
                String type = (String)breedFemale.get(breed);
                if (Rand.NextBool(2)) {
                    type = (String)breedMale.get(breed);
                }
                if ((adef = AnimalDefinitions.getDef(type)) == null) {
                    DebugLog.General.warn("can't spawn animal type \"", type, "\"");
                    break;
                }
                AnimalBreed breed1 = adef.getBreedByName(breed);
                if (breed1 == null) {
                    DebugLog.General.warn("can't spawn animal type/breed \"", type, "\" / \"", breed, "\"");
                    break;
                }
                IsoAnimal animal = new IsoAnimal(IsoWorld.instance.getCell(), (int)element.position.x, (int)element.position.y, 0, type, breed);
                animal.randomizeAge();
                animal.setHealth(0.0f);
                this.addTrailOfBlood(element.position.x, element.position.y, element.z, element.direction, 1);
                break;
            }
        }
    }
}

