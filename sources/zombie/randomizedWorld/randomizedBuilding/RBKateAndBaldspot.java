/*
 * Decompiled with CFR 0.152.
 */
package zombie.randomizedWorld.randomizedBuilding;

import java.util.ArrayList;
import zombie.UsedFromLua;
import zombie.characterTextures.BloodBodyPartType;
import zombie.characters.IsoZombie;
import zombie.core.ImmutableColor;
import zombie.core.skinnedmodel.visual.HumanVisual;
import zombie.core.skinnedmodel.visual.ItemVisual;
import zombie.inventory.InventoryItem;
import zombie.inventory.InventoryItemFactory;
import zombie.iso.BuildingDef;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoObject;
import zombie.randomizedWorld.randomizedBuilding.RandomizedBuildingBase;

@UsedFromLua
public class RBKateAndBaldspot
extends RandomizedBuildingBase {
    public RBKateAndBaldspot() {
        this.name = "K&B story";
        this.setChance(0);
        this.setUnique(true);
    }

    @Override
    public void randomizeBuilding(BuildingDef def) {
        def.alarmed = false;
        def.setHasBeenVisited(true);
        def.setAllExplored(true);
        ArrayList<IsoZombie> zeds = this.addZombiesOnSquare(1, "Kate", 100, RBKateAndBaldspot.getSq(10746, 9412, 1));
        if (zeds == null || zeds.isEmpty()) {
            return;
        }
        IsoZombie kate = zeds.get(0);
        HumanVisual visu = (HumanVisual)kate.getVisual();
        visu.setHairModel("Rachel");
        visu.setHairColor(new ImmutableColor(0.83f, 0.67f, 0.27f));
        for (int i = 0; i < kate.getItemVisuals().size(); ++i) {
            ItemVisual itemVisu = (ItemVisual)kate.getItemVisuals().get(i);
            if (!itemVisu.getClothingItemName().equals("Skirt_Knees")) continue;
            itemVisu.setTint(new ImmutableColor(0.54f, 0.54f, 0.54f));
        }
        kate.getHumanVisual().setSkinTextureIndex(1);
        kate.addBlood(BloodBodyPartType.LowerLeg_L, true, true, true);
        kate.addBlood(BloodBodyPartType.LowerLeg_L, true, true, true);
        kate.addBlood(BloodBodyPartType.UpperLeg_L, true, true, true);
        kate.addBlood(BloodBodyPartType.UpperLeg_L, true, true, true);
        kate.setCrawler(true);
        kate.setCanWalk(false);
        kate.setCrawlerType(1);
        kate.resetModelNextFrame();
        zeds = this.addZombiesOnSquare(1, "Bob", 0, RBKateAndBaldspot.getSq(10747, 9412, 1));
        if (zeds == null || zeds.isEmpty()) {
            return;
        }
        IsoZombie bob = zeds.get(0);
        visu = (HumanVisual)bob.getVisual();
        visu.setHairModel("Baldspot");
        visu.setHairColor(new ImmutableColor(0.337f, 0.173f, 0.082f));
        visu.setBeardModel("");
        for (int i = 0; i < bob.getItemVisuals().size(); ++i) {
            ItemVisual itemVisu = (ItemVisual)bob.getItemVisuals().get(i);
            if (itemVisu.getClothingItemName().equals("Trousers_DefaultTEXTURE_TINT")) {
                itemVisu.setTint(new ImmutableColor(0.54f, 0.54f, 0.54f));
            }
            if (!itemVisu.getClothingItemName().equals("Shirt_FormalTINT")) continue;
            itemVisu.setTint(new ImmutableColor(0.63f, 0.71f, 0.82f));
        }
        bob.getHumanVisual().setSkinTextureIndex(1);
        bob.resetModelNextFrame();
        bob.addItemToSpawnAtDeath((InventoryItem)InventoryItemFactory.CreateItem("KatePic"));
        bob.addItemToSpawnAtDeath((InventoryItem)InventoryItemFactory.CreateItem("RippedSheets"));
        bob.addItemToSpawnAtDeath((InventoryItem)InventoryItemFactory.CreateItem("Pills"));
        Object hammer = InventoryItemFactory.CreateItem("Hammer");
        ((InventoryItem)hammer).setCondition(1, false);
        bob.addItemToSpawnAtDeath((InventoryItem)hammer);
        bob.addItemToSpawnAtDeath((InventoryItem)InventoryItemFactory.CreateItem("Nails"));
        bob.addItemToSpawnAtDeath((InventoryItem)InventoryItemFactory.CreateItem("Plank"));
        zeds = this.addZombiesOnSquare(1, "Raider", 0, RBKateAndBaldspot.getSq(10745, 9411, 0));
        if (zeds == null || zeds.isEmpty()) {
            return;
        }
        IsoZombie raider = zeds.get(0);
        visu = (HumanVisual)raider.getVisual();
        visu.setHairModel("Crewcut");
        visu.setHairColor(new ImmutableColor(0.37f, 0.27f, 0.23f));
        visu.setBeardModel("Goatee");
        for (int i = 0; i < raider.getItemVisuals().size(); ++i) {
            ItemVisual itemVisu = (ItemVisual)raider.getItemVisuals().get(i);
            if (itemVisu.getClothingItemName().equals("Trousers_DefaultTEXTURE_TINT")) {
                itemVisu.setTint(new ImmutableColor(0.54f, 0.54f, 0.54f));
            }
            if (!itemVisu.getClothingItemName().equals("Vest_DefaultTEXTURE_TINT")) continue;
            itemVisu.setTint(new ImmutableColor(0.22f, 0.25f, 0.27f));
        }
        raider.getHumanVisual().setSkinTextureIndex(1);
        Object shotgun = InventoryItemFactory.CreateItem("Shotgun");
        ((InventoryItem)shotgun).setCondition(0, false);
        raider.setAttachedItem("Rifle On Back", (InventoryItem)shotgun);
        Object bat = InventoryItemFactory.CreateItem("BaseballBat");
        ((InventoryItem)bat).setCondition(1, false);
        raider.addItemToSpawnAtDeath((InventoryItem)bat);
        raider.addItemToSpawnAtDeath((InventoryItem)InventoryItemFactory.CreateItem("ShotgunShells"));
        raider.resetModelNextFrame();
        this.addItemOnGround(RBKateAndBaldspot.getSq(10747, 9412, 1), (InventoryItem)InventoryItemFactory.CreateItem("Pillow"));
        IsoGridSquare burntSq = RBKateAndBaldspot.getSq(10745, 9410, 0);
        burntSq.Burn();
        burntSq = RBKateAndBaldspot.getSq(10745, 9411, 0);
        burntSq.Burn();
        burntSq = RBKateAndBaldspot.getSq(10746, 9411, 0);
        burntSq.Burn();
        burntSq = RBKateAndBaldspot.getSq(10745, 9410, 0);
        burntSq.Burn();
        burntSq = RBKateAndBaldspot.getSq(10745, 9412, 0);
        burntSq.Burn();
        burntSq = RBKateAndBaldspot.getSq(10747, 9410, 0);
        burntSq.Burn();
        burntSq = RBKateAndBaldspot.getSq(10746, 9409, 0);
        burntSq.Burn();
        burntSq = RBKateAndBaldspot.getSq(10745, 9409, 0);
        burntSq.Burn();
        burntSq = RBKateAndBaldspot.getSq(10744, 9410, 0);
        burntSq.Burn();
        burntSq = RBKateAndBaldspot.getSq(10747, 9411, 0);
        burntSq.Burn();
        burntSq = RBKateAndBaldspot.getSq(10746, 9412, 0);
        burntSq.Burn();
        IsoGridSquare ovenSq = RBKateAndBaldspot.getSq(10746, 9410, 0);
        for (int i = 0; i < ovenSq.getObjects().size(); ++i) {
            IsoObject oven = ovenSq.getObjects().get(i);
            if (oven.getContainer() == null) continue;
            Object soup = InventoryItemFactory.CreateItem("PotOfSoup");
            ((InventoryItem)soup).setCooked(true);
            ((InventoryItem)soup).setBurnt(true);
            oven.getContainer().AddItem((InventoryItem)soup);
            break;
        }
        this.addBarricade(RBKateAndBaldspot.getSq(10747, 9417, 0), 3);
        this.addBarricade(RBKateAndBaldspot.getSq(10745, 9417, 0), 3);
        this.addBarricade(RBKateAndBaldspot.getSq(10744, 9413, 0), 3);
        this.addBarricade(RBKateAndBaldspot.getSq(10744, 9412, 0), 3);
        this.addBarricade(RBKateAndBaldspot.getSq(10752, 9413, 0), 3);
    }

    @Override
    public boolean isValid(BuildingDef def, boolean force) {
        this.debugLine = "";
        if (def.x == 10744 && def.y == 9409) {
            return true;
        }
        this.debugLine = "Need to be the K&B house";
        return false;
    }
}

