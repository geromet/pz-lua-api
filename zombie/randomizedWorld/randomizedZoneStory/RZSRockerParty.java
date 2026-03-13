/*
 * Decompiled with CFR 0.152.
 */
package zombie.randomizedWorld.randomizedZoneStory;

import java.util.ArrayList;
import java.util.Objects;
import zombie.UsedFromLua;
import zombie.core.random.Rand;
import zombie.inventory.InventoryItemFactory;
import zombie.inventory.types.InventoryContainer;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoWorld;
import zombie.iso.objects.IsoRadio;
import zombie.iso.sprite.IsoSpriteManager;
import zombie.iso.zones.Zone;
import zombie.network.GameServer;
import zombie.randomizedWorld.randomizedZoneStory.RandomizedZoneStoryBase;
import zombie.vehicles.BaseVehicle;

@UsedFromLua
public class RZSRockerParty
extends RandomizedZoneStoryBase {
    public RZSRockerParty() {
        this.name = "Rocker Kids Partying";
        this.chance = 5;
        this.minZoneHeight = 6;
        this.minZoneWidth = 6;
        this.zoneType.add(RandomizedZoneStoryBase.ZoneType.Forest.toString());
        this.zoneType.add(RandomizedZoneStoryBase.ZoneType.Beach.toString());
        this.zoneType.add(RandomizedZoneStoryBase.ZoneType.Lake.toString());
        this.setUnique(true);
    }

    public static ArrayList<String> getForestClutter() {
        ArrayList<String> result = new ArrayList<String>();
        result.add("Base.Whiskey");
        result.add("Base.BeerBottle");
        result.add("Base.BeerCan");
        result.add("Base.HottieZ");
        result.add("Base.WhiskeyEmpty");
        result.add("Base.BeerCanEmpty");
        result.add("Base.BeerEmpty");
        result.add("Base.PlasticCup");
        result.add("Base.SmashedBottle");
        result.add("Base.CigaretteSingle");
        result.add("Base.GuitarAcoustic");
        result.add("Base.Cooler_Beer");
        result.add("Base.CanPipe");
        return result;
    }

    public static ArrayList<String> getBagClutter() {
        ArrayList<String> result = new ArrayList<String>();
        result.add("Base.Whiskey");
        result.add("Base.BeerCan");
        result.add("Base.BeerBottle");
        result.add("Base.CigaretteSingle");
        result.add("Base.LighterDisposable");
        result.add("Base.CigaretteRollingPapers");
        return result;
    }

    public static ArrayList<String> getFireClutter() {
        ArrayList<String> result = new ArrayList<String>();
        result.add("Base.BeerCanEmpty");
        result.add("Base.BeerEmpty");
        result.add("Base.SmashedBottle");
        return result;
    }

    @Override
    public void randomizeZoneStory(Zone zone) {
        int midX = zone.pickedXForZoneStory;
        int midY = zone.pickedYForZoneStory;
        ArrayList<String> clutter = RZSRockerParty.getForestClutter();
        ArrayList<String> bagClutter = RZSRockerParty.getBagClutter();
        ArrayList<String> fireClutter = RZSRockerParty.getFireClutter();
        IsoGridSquare midSq = RZSRockerParty.getSq(midX, midY, zone.z);
        this.cleanAreaForStory(this, zone);
        int partyType = Rand.Next(4);
        this.cleanSquareAndNeighbors(midSq);
        this.addCampfireOrPit(midSq);
        this.addItemOnGround(RZSRockerParty.getSq(midX, midY, zone.z), fireClutter.get(Rand.Next(fireClutter.size())));
        IsoRadio obj = new IsoRadio(IsoWorld.instance.getCell(), RZSRockerParty.getSq(midX + 2, midY, zone.z), IsoSpriteManager.instance.getSprite("appliances_radio_01_8"));
        if (GameServer.server) {
            RZSRockerParty.getSq(midX + 2, midY, zone.z).transmitAddObjectToSquare(obj, -1);
        } else {
            RZSRockerParty.getSq(midX + 2, midY, zone.z).AddTileObject(obj);
        }
        obj.setRenderYOffset(0.0f);
        if (!Rand.NextBool(4)) {
            int randX = Rand.Next(-1, 2);
            int randY = Rand.Next(-1, 2);
            this.addSleepingBagOrTentWestEast(midX + randX - 3, midY + randY, zone.z);
            if (Rand.Next(100) < 70) {
                this.addSleepingBagOrTentNorthSouth(midX + randX, midY + randY - 3, zone.z);
            }
            if (Rand.Next(100) < 70) {
                this.addSleepingBagOrTentNorthSouth(midX + randX + 3, midY + randY - 2, zone.z);
            }
        }
        String outfit = "Rocker";
        String backpack = "Base.Bag_Schoolbag";
        String truck = "Base.PickUpTruck";
        if (partyType == 1) {
            outfit = "Punk";
            backpack = "Base.Bag_Schoolbag_Patches";
            truck = "Base.VanSeats";
        } else if (partyType == 2) {
            outfit = "Redneck";
            backpack = "Base.Cooler";
            truck = "Base.OffRoad";
        } else if (partyType == 3) {
            outfit = "Backpacker";
            backpack = "Base.Bag_Schoolbag_Travel";
            truck = "Base.VanSeats";
        }
        this.addZombiesOnSquare(Rand.Next(3, 8), outfit, 50, this.getRandomExtraFreeSquare(this, zone));
        if (Rand.Next(3) != 0) {
            BaseVehicle vehicle;
            int x2 = zone.x;
            int y2 = zone.y;
            if (Rand.Next(2) == 0) {
                x2 += zone.getWidth();
            }
            if (Rand.Next(2) == 0) {
                y2 += zone.getHeight();
            }
            if ((vehicle = this.addVehicle(zone, RZSRockerParty.getSq(x2, y2, zone.z), null, null, truck, null, null, null)) != null) {
                vehicle.addKeyToWorld();
                if (vehicle.getPassengerDoor(0) != null && Objects.requireNonNull(vehicle.getPassengerDoor(0)).getDoor() != null) {
                    Objects.requireNonNull(vehicle.getPassengerDoor(0)).getDoor().setLocked(false);
                }
                vehicle.setAlarmed(false);
                if (partyType != 3 && Rand.NextBool(2)) {
                    int palletNbr = Rand.Next(1, 3);
                    for (int i = 0; i < palletNbr; ++i) {
                        IsoGridSquare square = this.getRandomExtraFreeSquare(this, zone);
                        if (square.getX() == midX && square.getY() == midY) continue;
                        this.addTileObject(square, "construction_01_5");
                    }
                }
            }
        }
        int nbOfItem = Rand.Next(3, 7);
        for (int i = 0; i < nbOfItem; ++i) {
            this.addItemOnGround(RZSRockerParty.getRandomExtraFreeUnoccupiedSquare(this, zone), clutter.get(Rand.Next(clutter.size())));
        }
        if (Rand.Next(2) == 0) {
            InventoryContainer bag = (InventoryContainer)InventoryItemFactory.CreateItem(backpack);
            nbOfItem = Rand.Next(2, 5);
            for (int i = 0; i < nbOfItem; ++i) {
                bag.getItemContainer().AddItem(bagClutter.get(Rand.Next(bagClutter.size())));
            }
            this.addItemOnGround(RZSRockerParty.getRandomExtraFreeUnoccupiedSquare(this, zone), bag);
        }
    }
}

