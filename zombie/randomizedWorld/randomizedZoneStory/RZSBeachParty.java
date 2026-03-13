/*
 * Decompiled with CFR 0.152.
 */
package zombie.randomizedWorld.randomizedZoneStory;

import java.util.ArrayList;
import zombie.UsedFromLua;
import zombie.core.random.Rand;
import zombie.inventory.InventoryItemFactory;
import zombie.inventory.types.InventoryContainer;
import zombie.iso.IsoGridSquare;
import zombie.iso.zones.Zone;
import zombie.randomizedWorld.randomizedZoneStory.RZSForestCamp;
import zombie.randomizedWorld.randomizedZoneStory.RandomizedZoneStoryBase;

@UsedFromLua
public class RZSBeachParty
extends RandomizedZoneStoryBase {
    public RZSBeachParty() {
        this.name = "Beach Party";
        this.chance = 10;
        this.minZoneHeight = 13;
        this.minZoneWidth = 13;
        this.zoneType.add(RandomizedZoneStoryBase.ZoneType.Beach.toString());
        this.zoneType.add(RandomizedZoneStoryBase.ZoneType.Lake.toString());
    }

    @Override
    public void randomizeZoneStory(Zone zone) {
        int i;
        int i2;
        IsoGridSquare sq;
        int i3;
        int midX = zone.pickedXForZoneStory;
        int midY = zone.pickedYForZoneStory;
        ArrayList<String> coolerClutter = RZSForestCamp.getCoolerClutter();
        IsoGridSquare midSq = RZSBeachParty.getSq(midX, midY, zone.z);
        if (Rand.NextBool(2)) {
            this.cleanSquareAndNeighbors(midSq);
            int roll = Rand.Next(2);
            switch (roll) {
                case 0: {
                    this.addTileObject(midSq, "camping_01_6");
                    break;
                }
                case 1: {
                    this.addCookingPit(midSq);
                }
            }
        }
        int chairNbr = Rand.Next(1, 4);
        for (i3 = 0; i3 < chairNbr; ++i3) {
            int chairType = Rand.Next(4) + 1;
            switch (chairType) {
                case 1: {
                    chairType = 25;
                    break;
                }
                case 2: {
                    chairType = 26;
                    break;
                }
                case 3: {
                    chairType = 28;
                    break;
                }
                case 4: {
                    chairType = 31;
                }
            }
            sq = this.getRandomExtraFreeSquare(this, zone);
            if (sq == null) continue;
            this.addTileObject(sq, "furniture_seating_outdoor_01_" + chairType);
            if (chairType == 25) {
                sq = RZSBeachParty.getSq(sq.x, sq.y + 1, sq.z);
                this.addTileObject(sq, "furniture_seating_outdoor_01_24");
                continue;
            }
            if (chairType == 26) {
                sq = RZSBeachParty.getSq(sq.x + 1, sq.y, sq.z);
                this.addTileObject(sq, "furniture_seating_outdoor_01_27");
                continue;
            }
            if (chairType == 28) {
                sq = RZSBeachParty.getSq(sq.x, sq.y - 1, sq.z);
                this.addTileObject(sq, "furniture_seating_outdoor_01_29");
                continue;
            }
            sq = RZSBeachParty.getSq(sq.x - 1, sq.y, sq.z);
            this.addTileObject(sq, "furniture_seating_outdoor_01_30");
        }
        chairNbr = Rand.Next(1, 3);
        for (i3 = 0; i3 < chairNbr; ++i3) {
            sq = this.getRandomExtraFreeSquare(this, zone);
            if (sq == null) continue;
            this.addTileObject(sq, "furniture_seating_outdoor_01_" + Rand.Next(16, 20));
        }
        InventoryContainer cooler = (InventoryContainer)InventoryItemFactory.CreateItem("Base.Cooler");
        int nbOfItem = Rand.Next(4, 8);
        for (i2 = 0; i2 < nbOfItem; ++i2) {
            cooler.getItemContainer().AddItem(coolerClutter.get(Rand.Next(coolerClutter.size())));
        }
        sq = this.getRandomExtraFreeSquare(this, zone);
        if (sq != null) {
            this.addItemOnGround(this.getRandomExtraFreeSquare(this, zone), cooler);
        }
        nbOfItem = Rand.Next(3, 7);
        for (i2 = 0; i2 < nbOfItem; ++i2) {
            sq = this.getRandomExtraFreeSquare(this, zone);
            if (sq == null) continue;
            this.addItemOnGround(this.getRandomExtraFreeSquare(this, zone), this.getBeachPartyClutterItem());
        }
        int randZed = Rand.Next(3, 8);
        for (i = 0; i < randZed; ++i) {
            sq = this.getRandomExtraFreeSquare(this, zone);
            if (sq == null) continue;
            this.addZombiesOnSquare(1, "Swimmer", null, this.getRandomExtraFreeSquare(this, zone));
        }
        randZed = Rand.Next(1, 3);
        for (i = 0; i < randZed; ++i) {
            sq = this.getRandomExtraFreeSquare(this, zone);
            if (sq == null) continue;
            this.addZombiesOnSquare(1, "Tourist", null, this.getRandomExtraFreeSquare(this, zone));
        }
    }
}

