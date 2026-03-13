/*
 * Decompiled with CFR 0.152.
 */
package zombie.randomizedWorld.randomizedVehicleStory;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import zombie.UsedFromLua;
import zombie.characters.animals.IsoAnimal;
import zombie.core.math.PZMath;
import zombie.core.random.Rand;
import zombie.iso.IsoChunk;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoWorld;
import zombie.iso.zones.Zone;
import zombie.randomizedWorld.randomizedVehicleStory.RandomizedVehicleStoryBase;
import zombie.randomizedWorld.randomizedVehicleStory.VehicleStorySpawner;

@UsedFromLua
public final class RVSAnimalOnRoad
extends RandomizedVehicleStoryBase {
    public RVSAnimalOnRoad() {
        this.name = "Animal On Road";
        this.minZoneWidth = 2;
        this.minZoneHeight = 2;
        this.setChance(10);
        this.setMinimumDays(30);
        this.needsFarmland = true;
    }

    public static ArrayList<String> getBreeds() {
        ArrayList<String> result = new ArrayList<String>();
        result.add("rhodeisland");
        result.add("leghorn");
        result.add("angus");
        result.add("simmental");
        result.add("holstein");
        result.add("landrace");
        result.add("largeblack");
        result.add("suffolk");
        result.add("rambouillet");
        result.add("friesian");
        return result;
    }

    @Override
    public void randomizeVehicleStory(Zone zone, IsoChunk chunk) {
        this.callVehicleStorySpawner(zone, chunk, 0.0f);
    }

    @Override
    public boolean initVehicleStorySpawner(Zone zone, IsoChunk chunk, boolean debug) {
        VehicleStorySpawner spawner = VehicleStorySpawner.getInstance();
        spawner.clear();
        spawner.addElement("animal", 0.0f, 0.0f, 0.0f, 1.0f, 1.0f);
        spawner.setParameter("zone", zone);
        ArrayList<String> breeds = RVSAnimalOnRoad.getBreeds();
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
        float z = element.z;
        switch (element.id) {
            case "animal": {
                LinkedHashMap<String, String> breedFemale = new LinkedHashMap<String, String>();
                LinkedHashMap<String, String> breedMale = new LinkedHashMap<String, String>();
                breedFemale.put("rhodeisland", "hen");
                breedFemale.put("leghorn", "hen");
                breedFemale.put("angus", "cow");
                breedFemale.put("simmental", "cow");
                breedFemale.put("holstein", "cow");
                breedFemale.put("landrace", "sow");
                breedFemale.put("largeblack", "sow");
                breedFemale.put("suffolk", "ewe");
                breedFemale.put("rambouillet", "ewe");
                breedFemale.put("friesian", "ewe");
                breedMale.put("rhodeisland", "cockerel");
                breedMale.put("leghorn", "cockerel");
                breedMale.put("angus", "bull");
                breedMale.put("simmental", "bull");
                breedMale.put("holstein", "bull");
                breedMale.put("landrace", "boar");
                breedMale.put("largeblack", "boar");
                breedMale.put("suffolk", "ram");
                breedMale.put("rambouillet", "ram");
                breedMale.put("friesian", "ram");
                if (square == null) break;
                String breed = spawner.getParameterString("breed");
                String type = (String)breedFemale.get(breed);
                if (Rand.NextBool(5)) {
                    type = (String)breedMale.get(breed);
                }
                IsoAnimal animal = new IsoAnimal(IsoWorld.instance.getCell(), PZMath.fastfloor(element.position.x), PZMath.fastfloor(element.position.y), PZMath.fastfloor(z), type, breed);
                animal.addToWorld();
                animal.randomizeAge();
                break;
            }
        }
    }
}

