/*
 * Decompiled with CFR 0.152.
 */
package zombie.randomizedWorld.randomizedZoneStory;

import java.util.ArrayList;
import zombie.UsedFromLua;
import zombie.characters.IsoZombie;
import zombie.characters.SurvivorDesc;
import zombie.inventory.InventoryItem;
import zombie.inventory.InventoryItemFactory;
import zombie.iso.IsoGridSquare;
import zombie.iso.zones.Zone;
import zombie.randomizedWorld.randomizedZoneStory.RandomizedZoneStoryBase;

@UsedFromLua
public class RZJackieJaye
extends RandomizedZoneStoryBase {
    public RZJackieJaye() {
        this.name = "JackieJaye";
        this.chance = 100;
        this.zoneType.add(RandomizedZoneStoryBase.ZoneType.JackieJaye.toString());
        this.alwaysDo = true;
    }

    @Override
    public void randomizeZoneStory(Zone zone) {
        IsoGridSquare sq = this.getRandomExtraFreeSquare(this, zone);
        ArrayList<IsoZombie> zeds = this.addZombiesOnSquare(1, "Jackie_Jaye", 100, sq);
        if (zeds == null || zeds.isEmpty()) {
            return;
        }
        IsoZombie jackie = zeds.get(0);
        jackie.getHumanVisual().setSkinTextureIndex(1);
        SurvivorDesc desc = jackie.getDescriptor();
        if (desc == null) {
            return;
        }
        desc.setForename("Jackie");
        desc.setSurname("Jaye");
        Object pressID = InventoryItemFactory.CreateItem("Base.PressID");
        if (pressID == null) {
            return;
        }
        ((InventoryItem)pressID).nameAfterDescriptor(desc);
        jackie.addItemToSpawnAtDeath((InventoryItem)pressID);
    }
}

