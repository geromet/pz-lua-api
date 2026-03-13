/*
 * Decompiled with CFR 0.152.
 */
package zombie.randomizedWorld.randomizedZoneStory;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import zombie.UsedFromLua;
import zombie.characters.animals.IsoAnimal;
import zombie.core.random.Rand;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoWorld;
import zombie.iso.zones.Zone;
import zombie.randomizedWorld.randomizedZoneStory.RandomizedZoneStoryBase;

@UsedFromLua
public class RZSEscapedHerd
extends RandomizedZoneStoryBase {
    public RZSEscapedHerd() {
        this.name = "Escaped Herd";
        this.chance = 5;
        this.minZoneHeight = 8;
        this.minZoneWidth = 8;
        this.zoneType.add(RandomizedZoneStoryBase.ZoneType.Forest.toString());
        this.minimumDays = 30;
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
    public void randomizeZoneStory(Zone zone) {
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
        ArrayList<String> breeds = RZSEscapedHerd.getBreeds();
        String breed = breeds.get(Rand.Next(breeds.size()));
        String type = (String)breedMale.get(breed);
        IsoGridSquare square = RZSEscapedHerd.getRandomExtraFreeUnoccupiedSquare(this, zone);
        IsoAnimal animal = new IsoAnimal(IsoWorld.instance.getCell(), square.getX(), square.getY(), square.getZ(), type, breed);
        animal.addToWorld();
        animal.randomizeAge();
        type = (String)breedFemale.get(breed);
        int moreAnimals = Rand.Next(1, 6);
        for (int i = 0; i < moreAnimals; ++i) {
            square = RZSEscapedHerd.getRandomExtraFreeUnoccupiedSquare(this, zone);
            animal = new IsoAnimal(IsoWorld.instance.getCell(), square.getX(), square.getY(), square.getZ(), type, breed);
            animal.addToWorld();
            animal.randomizeAge();
            if (!Rand.NextBool(2)) continue;
            animal.setStateEventDelayTimer(0.0f);
        }
    }
}

