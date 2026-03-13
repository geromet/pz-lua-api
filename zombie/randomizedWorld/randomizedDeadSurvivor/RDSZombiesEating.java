/*
 * Decompiled with CFR 0.152.
 */
package zombie.randomizedWorld.randomizedDeadSurvivor;

import zombie.UsedFromLua;
import zombie.VirtualZombieManager;
import zombie.core.random.Rand;
import zombie.iso.BuildingDef;
import zombie.iso.IsoWorld;
import zombie.iso.RoomDef;
import zombie.iso.objects.IsoDeadBody;
import zombie.randomizedWorld.randomizedDeadSurvivor.RandomizedDeadSurvivorBase;

@UsedFromLua
public final class RDSZombiesEating
extends RandomizedDeadSurvivorBase {
    public RDSZombiesEating() {
        this.name = "Eating zombies";
        this.setChance(7);
        this.setMaximumDays(60);
    }

    @Override
    public boolean isValid(BuildingDef def, boolean force) {
        return IsoWorld.getZombiesEnabled() && super.isValid(def, force);
    }

    @Override
    public void randomizeDeadSurvivor(BuildingDef def) {
        RoomDef room = this.getLivingRoomOrKitchen(def);
        IsoDeadBody body = RDSZombiesEating.createRandomDeadBody(room, Rand.Next(5, 10));
        if (body == null) {
            return;
        }
        VirtualZombieManager.instance.createEatingZombies(body, Rand.Next(1, 3));
        RoomDef kitchen = this.getRoom(def, "kitchen");
        RoomDef livingroom = this.getRoom(def, "livingroom");
        if ("kitchen".equals(room.name) && livingroom != null && Rand.Next(3) == 0) {
            body = RDSZombiesEating.createRandomDeadBody(livingroom, Rand.Next(5, 10));
            if (body == null) {
                return;
            }
            VirtualZombieManager.instance.createEatingZombies(body, Rand.Next(1, 3));
        }
        if ("livingroom".equals(room.name) && kitchen != null && Rand.Next(3) == 0) {
            body = RDSZombiesEating.createRandomDeadBody(kitchen, Rand.Next(5, 10));
            if (body == null) {
                return;
            }
            VirtualZombieManager.instance.createEatingZombies(body, Rand.Next(1, 3));
        }
        def.alarmed = false;
    }
}

