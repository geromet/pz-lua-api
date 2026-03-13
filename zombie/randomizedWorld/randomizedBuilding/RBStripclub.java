/*
 * Decompiled with CFR 0.152.
 */
package zombie.randomizedWorld.randomizedBuilding;

import java.util.ArrayList;
import zombie.UsedFromLua;
import zombie.core.random.Rand;
import zombie.iso.BuildingDef;
import zombie.iso.IsoCell;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoObject;
import zombie.iso.IsoWorld;
import zombie.iso.RoomDef;
import zombie.randomizedWorld.randomizedBuilding.RandomizedBuildingBase;

@UsedFromLua
public final class RBStripclub
extends RandomizedBuildingBase {
    @Override
    public void randomizeBuilding(BuildingDef def) {
        def.alarmed = false;
        def.setHasBeenVisited(true);
        def.setAllExplored(true);
        IsoCell cell = IsoWorld.instance.currentCell;
        boolean maleVenue = Rand.NextBool(20);
        ArrayList<Integer> alreadyAddedClothes = new ArrayList<Integer>();
        for (int x = def.x - 1; x < def.x2 + 1; ++x) {
            for (int y = def.y - 1; y < def.y2 + 1; ++y) {
                for (int z = -32; z < 31; ++z) {
                    IsoGridSquare sq = cell.getGridSquare(x, y, z);
                    if (sq == null) continue;
                    block17: for (int i = 0; i < sq.getObjects().size(); ++i) {
                        int j;
                        int money;
                        IsoObject obj = sq.getObjects().get(i);
                        if (Rand.NextBool(2) && "location_restaurant_pizzawhirled_01_16".equals(obj.getSprite().getName())) {
                            money = Rand.Next(1, 4);
                            for (j = 0; j < money; ++j) {
                                if (Rand.NextBool(8)) {
                                    RBStripclub.trySpawnStoryItem("MoneyBundle", sq, Rand.Next(0.0f, 0.5f), Rand.Next(0.0f, 0.5f), 0.0f);
                                    continue;
                                }
                                RBStripclub.trySpawnStoryItem("Money", sq, Rand.Next(0.0f, 0.5f), Rand.Next(0.0f, 0.5f), 0.0f);
                            }
                            int clothing = Rand.Next(1, 4);
                            block19: for (int j2 = 0; j2 < clothing; ++j2) {
                                int clotheType = Rand.Next(1, 7);
                                while (alreadyAddedClothes.contains(clotheType)) {
                                    clotheType = Rand.Next(1, 7);
                                }
                                switch (clotheType) {
                                    case 1: {
                                        RBStripclub.trySpawnStoryItem(maleVenue ? "Trousers" : "TightsFishnet_Ground", sq, Rand.Next(0.0f, 0.5f), Rand.Next(0.0f, 0.5f), 0.0f);
                                        alreadyAddedClothes.add(1);
                                        continue block19;
                                    }
                                    case 2: {
                                        RBStripclub.trySpawnStoryItem("Vest_DefaultTEXTURE_TINT", sq, Rand.Next(0.0f, 0.5f), Rand.Next(0.0f, 0.5f), 0.0f);
                                        alreadyAddedClothes.add(2);
                                        continue block19;
                                    }
                                    case 3: {
                                        RBStripclub.trySpawnStoryItem(maleVenue ? "Jacket_Fireman" : "BunnySuitBlack", sq, Rand.Next(0.0f, 0.5f), Rand.Next(0.0f, 0.5f), 0.0f);
                                        alreadyAddedClothes.add(3);
                                        continue block19;
                                    }
                                    case 4: {
                                        RBStripclub.trySpawnStoryItem(maleVenue ? "Hat_Cowboy" : "Garter", sq, Rand.Next(0.0f, 0.5f), Rand.Next(0.0f, 0.5f), 0.0f);
                                        alreadyAddedClothes.add(4);
                                        continue block19;
                                    }
                                    case 5: {
                                        if (!maleVenue) {
                                            RBStripclub.trySpawnStoryItem("StockingsBlack", sq, Rand.Next(0.0f, 0.5f), Rand.Next(0.0f, 0.5f), 0.0f);
                                        }
                                        alreadyAddedClothes.add(5);
                                    }
                                }
                            }
                        }
                        if (!obj.hasAdjacentCanStandSquare() || !"furniture_tables_high_01_16".equals(obj.getSprite().getName()) && !"furniture_tables_high_01_17".equals(obj.getSprite().getName()) && !"furniture_tables_high_01_18".equals(obj.getSprite().getName())) continue;
                        money = Rand.Next(1, 4);
                        for (j = 0; j < money; ++j) {
                            RBStripclub.trySpawnStoryItem("Money", sq, Rand.Next(0.5f, 1.0f), Rand.Next(0.5f, 1.0f), obj.getSurfaceOffsetNoTable() / 96.0f);
                        }
                        if (Rand.NextBool(3)) {
                            this.addWorldItem("CigaretteSingle", sq, obj);
                            if (Rand.NextBool(2)) {
                                this.addWorldItem("Lighter", sq, obj);
                            }
                        }
                        int alcohol = Rand.Next(7);
                        switch (alcohol) {
                            case 0: {
                                RBStripclub.trySpawnStoryItem("Whiskey", sq, Rand.Next(0.5f, 1.0f), Rand.Next(0.5f, 1.0f), obj.getSurfaceOffsetNoTable() / 96.0f);
                                continue block17;
                            }
                            case 1: {
                                RBStripclub.trySpawnStoryItem("Champagne", sq, Rand.Next(0.5f, 1.0f), Rand.Next(0.5f, 1.0f), obj.getSurfaceOffsetNoTable() / 96.0f);
                                continue block17;
                            }
                            case 2: {
                                RBStripclub.trySpawnStoryItem("Champagne", sq, Rand.Next(0.5f, 1.0f), Rand.Next(0.5f, 1.0f), obj.getSurfaceOffsetNoTable() / 96.0f);
                                continue block17;
                            }
                            case 3: {
                                RBStripclub.trySpawnStoryItem("BeerImported", sq, Rand.Next(0.5f, 1.0f), Rand.Next(0.5f, 1.0f), obj.getSurfaceOffsetNoTable() / 96.0f);
                                continue block17;
                            }
                            case 4: {
                                RBStripclub.trySpawnStoryItem("BeerBottle", sq, Rand.Next(0.5f, 1.0f), Rand.Next(0.5f, 1.0f), obj.getSurfaceOffsetNoTable() / 96.0f);
                            }
                        }
                    }
                }
            }
        }
        RoomDef room = def.getRoom("stripclub");
        if (maleVenue) {
            this.addZombies(def, Rand.Next(2, 4), "WaiterStripper", 0, room);
            this.addZombies(def, 1, "PoliceStripper", 0, room);
            this.addZombies(def, 1, "FiremanStripper", 0, room);
            this.addZombies(def, 1, "CowboyStripper", 0, room);
            this.addZombies(def, Rand.Next(9, 15), null, 100, room);
        } else {
            this.addZombies(def, Rand.Next(2, 4), "WaiterStripper", 100, room);
            this.addZombies(def, Rand.Next(2, 5), "StripperNaked", 100, room);
            this.addZombies(def, Rand.Next(2, 5), "StripperBlack", 100, room);
            this.addZombies(def, Rand.Next(2, 5), "StripperWhite", 100, room);
            this.addZombies(def, Rand.Next(9, 15), null, 0, room);
        }
    }

    @Override
    public boolean isValid(BuildingDef def, boolean force) {
        return def.getRoom("stripclub") != null;
    }

    public RBStripclub() {
        this.name = "Stripclub";
        this.setAlwaysDo(true);
    }
}

