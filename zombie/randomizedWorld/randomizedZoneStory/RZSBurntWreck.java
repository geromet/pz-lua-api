/*
 * Decompiled with CFR 0.152.
 */
package zombie.randomizedWorld.randomizedZoneStory;

import java.util.ArrayList;
import zombie.UsedFromLua;
import zombie.core.random.Rand;
import zombie.iso.zones.Zone;
import zombie.randomizedWorld.randomizedZoneStory.RandomizedZoneStoryBase;

@UsedFromLua
public class RZSBurntWreck
extends RandomizedZoneStoryBase {
    public RZSBurntWreck() {
        this.name = "Burnt Wreck";
        this.chance = 5;
        this.minZoneHeight = 4;
        this.minZoneWidth = 4;
        this.zoneType.add(RandomizedZoneStoryBase.ZoneType.Forest.toString());
    }

    public static ArrayList<String> getForestClutter() {
        ArrayList<String> result = new ArrayList<String>();
        result.add("Base.BeerCanEmpty");
        result.add("Base.BeerEmpty");
        result.add("Base.SmashedBottle");
        return result;
    }

    @Override
    public void randomizeZoneStory(Zone zone) {
        ArrayList<String> clutter = RZSBurntWreck.getForestClutter();
        this.cleanAreaForStory(this, zone);
        int x = zone.x;
        int y = zone.y;
        if (Rand.Next(2) == 0) {
            x += zone.getWidth();
        }
        if (Rand.Next(2) == 0) {
            y += zone.getHeight();
        }
        this.addVehicle(zone, RZSBurntWreck.getSq(x, y, zone.z), null, "normalburnt", null, null, null, null);
        if (Rand.Next(2) == 0) {
            int nbOfItem = Rand.Next(2, 5);
            for (int i = 0; i < nbOfItem; ++i) {
                this.addItemOnGround(this.getRandomExtraFreeSquare(this, zone), clutter.get(Rand.Next(clutter.size())));
            }
        }
    }
}

