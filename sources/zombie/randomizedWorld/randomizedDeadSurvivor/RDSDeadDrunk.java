/*
 * Decompiled with CFR 0.152.
 */
package zombie.randomizedWorld.randomizedDeadSurvivor;

import java.util.ArrayList;
import zombie.UsedFromLua;
import zombie.core.random.Rand;
import zombie.inventory.InventoryItem;
import zombie.inventory.InventoryItemFactory;
import zombie.iso.BuildingDef;
import zombie.iso.RoomDef;
import zombie.iso.objects.IsoDeadBody;
import zombie.randomizedWorld.randomizedDeadSurvivor.RandomizedDeadSurvivorBase;

@UsedFromLua
public final class RDSDeadDrunk
extends RandomizedDeadSurvivorBase {
    final ArrayList<String> alcoholList = new ArrayList();

    public RDSDeadDrunk() {
        this.name = "Dead Drunk";
        this.setChance(10);
        this.alcoholList.add("Base.Whiskey");
        this.alcoholList.add("Base.WhiskeyEmpty");
        this.alcoholList.add("Base.Wine");
        this.alcoholList.add("Base.WineEmpty");
        this.alcoholList.add("Base.Wine2");
        this.alcoholList.add("Base.Wine2Empty");
        this.alcoholList.add("Base.WineBox");
    }

    @Override
    public void randomizeDeadSurvivor(BuildingDef def) {
        RoomDef room = this.getLivingRoomOrKitchen(def);
        IsoDeadBody body = RDSDeadDrunk.createRandomDeadBody(room, 0);
        if (body == null) {
            return;
        }
        int bleach = Rand.Next(2, 4);
        for (int b = 0; b < bleach; ++b) {
            Object whiskey = InventoryItemFactory.CreateItem(this.alcoholList.get(Rand.Next(0, this.alcoholList.size())));
            RDSDeadDrunk.trySpawnStoryItem(whiskey, body.getSquare(), Rand.Next(0.5f, 1.0f), Rand.Next(0.5f, 1.0f), 0.0f);
            def.alarmed = false;
        }
        body.setPrimaryHandItem((InventoryItem)InventoryItemFactory.CreateItem("Base.WhiskeyEmpty"));
    }
}

