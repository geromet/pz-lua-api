/*
 * Decompiled with CFR 0.152.
 */
package zombie.randomizedWorld.randomizedDeadSurvivor;

import zombie.UsedFromLua;
import zombie.characters.IsoGameCharacter;
import zombie.core.random.Rand;
import zombie.inventory.InventoryItem;
import zombie.inventory.InventoryItemFactory;
import zombie.iso.BuildingDef;
import zombie.iso.RoomDef;
import zombie.iso.objects.IsoDeadBody;
import zombie.randomizedWorld.randomizedDeadSurvivor.RandomizedDeadSurvivorBase;

@UsedFromLua
public final class RDSSuicidePact
extends RandomizedDeadSurvivorBase {
    public RDSSuicidePact() {
        this.name = "Suicide Pact";
        this.setChance(7);
        this.setMinimumDays(60);
    }

    @Override
    public void randomizeDeadSurvivor(BuildingDef def) {
        RoomDef room = this.getLivingRoomOrKitchen(def);
        IsoGameCharacter zombie = RDSSuicidePact.createRandomZombieForCorpse(room);
        if (zombie == null) {
            return;
        }
        zombie.addVisualDamage("ZedDmg_HEAD_Bullet");
        IsoDeadBody body = RDSSuicidePact.createBodyFromZombie(zombie);
        if (body == null) {
            return;
        }
        this.addBloodSplat(body.getSquare(), 4);
        body.setPrimaryHandItem(this.addWeapon("Base.Pistol", true));
        zombie = RDSSuicidePact.createRandomZombieForCorpse(room);
        if (zombie == null) {
            return;
        }
        zombie.addVisualDamage("ZedDmg_HEAD_Bullet");
        body = RDSSuicidePact.createBodyFromZombie(zombie);
        if (body == null) {
            return;
        }
        this.addBloodSplat(body.getSquare(), 4);
        if (Rand.Next(2) == 0) {
            Object note = InventoryItemFactory.CreateItem("Base.Note");
            if (Rand.Next(2) == 0) {
                RDSSuicidePact.trySpawnStoryItem(note, body.getSquare(), Rand.Next(0.5f, 1.0f), Rand.Next(0.5f, 1.0f), 0.0f);
            } else {
                body.getContainer().addItem((InventoryItem)note);
                this.trySpawnStoryItem((InventoryItem)note, body.getContainer());
            }
        }
    }
}

