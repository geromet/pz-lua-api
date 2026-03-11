/*
 * Decompiled with CFR 0.152.
 */
package zombie.randomizedWorld.randomizedZoneStory;

import zombie.UsedFromLua;
import zombie.core.random.Rand;
import zombie.iso.IsoGridSquare;
import zombie.iso.zones.Zone;
import zombie.randomizedWorld.randomizedZoneStory.RandomizedZoneStoryBase;

@UsedFromLua
public class RZSCharcoalBurner
extends RandomizedZoneStoryBase {
    public RZSCharcoalBurner() {
        this.name = "Charcoal Burner";
        this.chance = 1;
        this.minZoneHeight = 9;
        this.minZoneWidth = 9;
        this.zoneType.add(RandomizedZoneStoryBase.ZoneType.Forest.toString());
        this.setUnique(true);
    }

    @Override
    public void randomizeZoneStory(Zone zone) {
        IsoGridSquare sq3;
        this.cleanAreaForStory(this, zone);
        int midX = zone.pickedXForZoneStory;
        int midY = zone.pickedYForZoneStory;
        IsoGridSquare midSq = RZSCharcoalBurner.getSq(midX, midY, zone.z);
        if (midSq == null) {
            return;
        }
        this.cleanSquareAndNeighbors(midSq);
        this.addCharcoalBurner(midSq);
        IsoGridSquare sq = this.getRandomExtraFreeSquare(this, zone);
        if (sq == null) {
            return;
        }
        this.dirtBomb(sq);
        if (!sq.isAdjacentTo(midSq)) {
            this.cleanSquareAndNeighbors(sq);
        }
        for (int i = 0; i < Rand.Next(10); ++i) {
            this.addItemOnGround(sq, "Base.Log");
        }
        IsoGridSquare sq2 = sq.getRandomAdjacent();
        if (sq2 != null && Rand.NextBool(2)) {
            this.dirtBomb(sq2);
            String axe = switch (Rand.Next(3)) {
                default -> "Base.GardenSaw";
                case 0 -> "Base.GardenSaw";
                case 1 -> "Base.HandAxeForged";
                case 2 -> "Base.WoodAxeForged";
            };
            this.addItemOnGround(sq2, axe);
        }
        if ((sq3 = this.getRandomExtraFreeSquare(this, zone)) == null) {
            return;
        }
        this.dirtBomb(sq3);
        if (!(sq3.isAdjacentTo(midSq) || sq3.isAdjacentTo(sq) || sq3.isAdjacentTo(sq2))) {
            this.cleanSquareAndNeighbors(sq3);
        }
        for (int i = 0; i < Rand.Next(51); ++i) {
            this.addItemOnGround(sq3, "Base.CharcoalCrafted");
        }
    }
}

