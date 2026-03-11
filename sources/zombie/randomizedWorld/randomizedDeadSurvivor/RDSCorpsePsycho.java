/*
 * Decompiled with CFR 0.152.
 */
package zombie.randomizedWorld.randomizedDeadSurvivor;

import java.util.ArrayList;
import zombie.UsedFromLua;
import zombie.characters.IsoZombie;
import zombie.core.random.Rand;
import zombie.inventory.InventoryItemFactory;
import zombie.inventory.ItemPickerJava;
import zombie.inventory.types.InventoryContainer;
import zombie.iso.BuildingDef;
import zombie.iso.RoomDef;
import zombie.iso.objects.IsoDeadBody;
import zombie.randomizedWorld.randomizedDeadSurvivor.RandomizedDeadSurvivorBase;

@UsedFromLua
public final class RDSCorpsePsycho
extends RandomizedDeadSurvivorBase {
    public RDSCorpsePsycho() {
        this.name = "Corpse Psycho";
        this.setChance(1);
        this.setMinimumDays(120);
        this.setUnique(true);
    }

    @Override
    public void randomizeDeadSurvivor(BuildingDef def) {
        RoomDef room = this.getRoom(def, "kitchen");
        if (room == null) {
            room = this.getRoom(def, "livingroom");
        }
        if (room == null) {
            room = this.getRoomNoKids(def, "bedroom");
        }
        if (room == null) {
            return;
        }
        int nbrOfZed = Rand.Next(3, 7);
        for (int i = 0; i < nbrOfZed; ++i) {
            IsoDeadBody body = RDSCorpsePsycho.createRandomDeadBody(room, Rand.Next(5, 10));
            if (body == null) continue;
            this.addBloodSplat(body.getCurrentSquare(), Rand.Next(7, 12));
        }
        ArrayList<IsoZombie> zombies = this.addZombies(def, 1, "MadScientist", null, room);
        if (zombies.isEmpty()) {
            return;
        }
        InventoryContainer bag = (InventoryContainer)InventoryItemFactory.CreateItem("Base.Bag_DoctorBag");
        ItemPickerJava.rollContainerItem(bag, null, ItemPickerJava.getItemPickerContainers().get("Bag_BurglarBag"));
        this.addItemOnGround(def.getFreeSquareInRoom(), bag);
        for (int i = 0; i < 8; ++i) {
            zombies.get(0).addBlood(null, false, true, false);
        }
        def.alarmed = false;
    }
}

