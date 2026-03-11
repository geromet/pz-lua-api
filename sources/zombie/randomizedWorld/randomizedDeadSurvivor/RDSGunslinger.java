/*
 * Decompiled with CFR 0.152.
 */
package zombie.randomizedWorld.randomizedDeadSurvivor;

import zombie.UsedFromLua;
import zombie.core.random.Rand;
import zombie.iso.BuildingDef;
import zombie.iso.IsoGridSquare;
import zombie.iso.objects.IsoDeadBody;
import zombie.randomizedWorld.randomizedDeadSurvivor.RandomizedDeadSurvivorBase;

@UsedFromLua
public final class RDSGunslinger
extends RandomizedDeadSurvivorBase {
    @Override
    public void randomizeDeadSurvivor(BuildingDef def) {
        IsoGridSquare sq = def.getFreeSquareInRoom();
        if (sq == null || sq.getRoom() != null && sq.getRoom().getRoomDef() != null && !sq.getRoom().getRoomDef().isKidsRoom()) {
            return;
        }
        IsoDeadBody body = RDSGunslinger.createRandomDeadBody(sq.getX(), sq.getY(), sq.getZ(), null, 0);
        if (body == null) {
            return;
        }
        body.setPrimaryHandItem(this.addRandomRangedWeapon(body.getContainer(), true, false, false));
        int nbOfWeapons = Rand.Next(1, 4);
        for (int i = 0; i < nbOfWeapons; ++i) {
            body.getContainer().AddItem(this.addRandomRangedWeapon(body.getContainer(), true, true, true));
        }
    }

    public RDSGunslinger() {
        this.name = "Gunslinger";
        this.setChance(5);
    }
}

