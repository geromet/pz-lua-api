/*
 * Decompiled with CFR 0.152.
 */
package zombie.randomizedWorld.randomizedZoneStory;

import java.util.ArrayList;
import zombie.UsedFromLua;
import zombie.characters.IsoZombie;
import zombie.core.random.Rand;
import zombie.inventory.InventoryItemFactory;
import zombie.inventory.types.InventoryContainer;
import zombie.iso.IsoGridSquare;
import zombie.iso.objects.IsoDeadBody;
import zombie.iso.zones.Zone;
import zombie.randomizedWorld.randomizedZoneStory.RZSForestCamp;
import zombie.randomizedWorld.randomizedZoneStory.RandomizedZoneStoryBase;

@UsedFromLua
public class RZSForestCampEaten
extends RandomizedZoneStoryBase {
    public RZSForestCampEaten() {
        this.name = "Forest Camp Eaten";
        this.chance = 10;
        this.minZoneHeight = 6;
        this.minZoneWidth = 10;
        this.minimumDays = 30;
        this.zoneType.add(RandomizedZoneStoryBase.ZoneType.Forest.toString());
    }

    @Override
    public void randomizeZoneStory(Zone zone) {
        IsoDeadBody body;
        ArrayList<IsoZombie> zombies;
        int i;
        int midX = zone.pickedXForZoneStory;
        int midY = zone.pickedYForZoneStory;
        ArrayList<String> clutter = RZSForestCamp.getForestClutter();
        ArrayList<String> coolerClutter = RZSForestCamp.getCoolerClutter();
        ArrayList<String> fireClutter = RZSForestCamp.getFireClutter();
        IsoGridSquare midSq = RZSForestCampEaten.getSq(midX, midY, zone.z);
        this.cleanAreaForStory(this, zone);
        this.cleanSquareAndNeighbors(midSq);
        this.addCampfireOrPit(midSq);
        this.addItemOnGround(RZSForestCampEaten.getSq(midX, midY, zone.z), fireClutter.get(Rand.Next(fireClutter.size())));
        int randX = 0;
        boolean randY = false;
        this.addRandomTentNorthSouth(midX - 4, midY + 0 - 2, zone.z);
        this.addRandomTentNorthSouth(midX - 3 + (randX += Rand.Next(1, 3)), midY + 0 - 2, zone.z);
        this.addRandomTentNorthSouth(midX - 2 + (randX += Rand.Next(1, 3)), midY + 0 - 2, zone.z);
        if (Rand.NextBool(1)) {
            this.addRandomTentNorthSouth(midX - 1 + (randX += Rand.Next(1, 3)), midY + 0 - 2, zone.z);
        }
        if (Rand.NextBool(2)) {
            this.addRandomTentNorthSouth(midX + (randX += Rand.Next(1, 3)), midY + 0 - 2, zone.z);
        }
        InventoryContainer cooler = (InventoryContainer)InventoryItemFactory.CreateItem("Base.Cooler");
        int nbOfItem = Rand.Next(2, 5);
        for (i = 0; i < nbOfItem; ++i) {
            cooler.getItemContainer().AddItem(coolerClutter.get(Rand.Next(coolerClutter.size())));
        }
        this.addItemOnGround(this.getRandomExtraFreeSquare(this, zone), cooler);
        nbOfItem = Rand.Next(3, 7);
        for (i = 0; i < nbOfItem; ++i) {
            this.addItemOnGround(this.getRandomExtraFreeSquare(this, zone), clutter.get(Rand.Next(clutter.size())));
        }
        String outfit = "Camper";
        if (Rand.NextBool(2)) {
            outfit = "Backpacker";
        }
        IsoZombie eatingZed = (zombies = this.addZombiesOnSquare(1, outfit, null, this.getRandomExtraFreeSquare(this, zone))).isEmpty() ? null : zombies.get(0);
        int randCorpse = Rand.Next(3, 7);
        for (int i2 = 0; i2 < randCorpse; ++i2) {
            body = RZSForestCampEaten.createRandomDeadBody(this.getRandomExtraFreeSquare(this, zone), null, Rand.Next(5, 10), 0, outfit);
            if (body == null) continue;
            this.addBloodSplat(body.getSquare(), 10);
        }
        body = RZSForestCampEaten.createRandomDeadBody(RZSForestCampEaten.getSq(midX, midY + 3, zone.z), null, Rand.Next(5, 10), 0, outfit);
        if (body != null) {
            this.addBloodSplat(body.getSquare(), 10);
            if (eatingZed != null) {
                eatingZed.faceLocationF(body.getX(), body.getY());
                eatingZed.setX(body.getX() + 1.0f);
                eatingZed.setY(body.getY());
                eatingZed.setEatBodyTarget(body, true);
            }
        }
    }
}

