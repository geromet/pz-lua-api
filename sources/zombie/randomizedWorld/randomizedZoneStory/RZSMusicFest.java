/*
 * Decompiled with CFR 0.152.
 */
package zombie.randomizedWorld.randomizedZoneStory;

import zombie.UsedFromLua;
import zombie.core.random.Rand;
import zombie.iso.zones.Zone;
import zombie.randomizedWorld.randomizedZoneStory.RandomizedZoneStoryBase;

@UsedFromLua
public class RZSMusicFest
extends RandomizedZoneStoryBase {
    public RZSMusicFest() {
        this.name = "Music Festival";
        this.chance = 100;
        this.zoneType.add(RandomizedZoneStoryBase.ZoneType.MusicFest.toString());
        this.alwaysDo = true;
    }

    @Override
    public void randomizeZoneStory(Zone zone) {
        int nbrOfItems = Rand.Next(20, 50);
        block6: for (int i = 0; i < nbrOfItems; ++i) {
            int item = Rand.Next(0, 4);
            switch (item) {
                case 0: {
                    this.addItemOnGround(this.getRandomFreeSquareFullZone(this, zone), "Base.BeerCan");
                    continue block6;
                }
                case 1: {
                    this.addItemOnGround(this.getRandomFreeSquareFullZone(this, zone), "Base.BeerBottle");
                    continue block6;
                }
                case 2: {
                    this.addItemOnGround(this.getRandomFreeSquareFullZone(this, zone), "Base.BeerCanEmpty");
                    continue block6;
                }
                case 3: {
                    this.addItemOnGround(this.getRandomFreeSquareFullZone(this, zone), "Base.BeerEmpty");
                }
            }
        }
    }
}

