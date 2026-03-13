/*
 * Decompiled with CFR 0.152.
 */
package zombie.randomizedWorld.randomizedDeadSurvivor;

import zombie.UsedFromLua;
import zombie.core.random.Rand;
import zombie.inventory.InventoryItem;
import zombie.inventory.InventoryItemFactory;
import zombie.iso.BuildingDef;
import zombie.iso.RoomDef;
import zombie.iso.objects.IsoDeadBody;
import zombie.randomizedWorld.randomizedDeadSurvivor.RandomizedDeadSurvivorBase;
import zombie.scripting.objects.ItemKey;

@UsedFromLua
public final class RDSBleach
extends RandomizedDeadSurvivorBase {
    public RDSBleach() {
        this.name = "Suicide by Bleach";
        this.setChance(10);
        this.setMinimumDays(60);
    }

    @Override
    public void randomizeDeadSurvivor(BuildingDef def) {
        RoomDef room = this.getLivingRoomOrKitchen(def);
        IsoDeadBody body = RDSBleach.createRandomDeadBody(room, 0);
        if (body == null) {
            return;
        }
        if (Rand.NextBool(2)) {
            int bleach = Rand.Next(1, 3);
            for (int b = 0; b < bleach; ++b) {
                InventoryItem emptyBleach = ((InventoryItem)InventoryItemFactory.CreateItem(ItemKey.Normal.BLEACH)).emptyLiquid();
                RDSBleach.trySpawnStoryItem(emptyBleach, body.getSquare(), Rand.Next(0.5f, 1.0f), Rand.Next(0.5f, 1.0f), 0.0f);
            }
            body.setPrimaryHandItem(((InventoryItem)InventoryItemFactory.CreateItem(ItemKey.Normal.BLEACH)).emptyLiquid());
        } else {
            Object poison = InventoryItemFactory.CreateItem(ItemKey.Drainable.RAT_POISON);
            ((InventoryItem)poison).setCurrentUses(((InventoryItem)poison).getMaxUses() / 2);
            RDSBleach.trySpawnStoryItem(poison, body.getSquare(), Rand.Next(0.5f, 1.0f), Rand.Next(0.5f, 1.0f), 0.0f);
        }
        if (Rand.Next(2) == 0) {
            Object note = InventoryItemFactory.CreateItem(ItemKey.Literature.NOTE);
            if (Rand.Next(2) == 0) {
                RDSBleach.trySpawnStoryItem(note, body.getSquare(), Rand.Next(0.5f, 1.0f), Rand.Next(0.5f, 1.0f), 0.0f);
            } else {
                body.getContainer().addItem((InventoryItem)note);
            }
        }
        def.alarmed = false;
    }
}

