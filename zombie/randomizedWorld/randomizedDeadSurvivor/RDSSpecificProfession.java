/*
 * Decompiled with CFR 0.152.
 */
package zombie.randomizedWorld.randomizedDeadSurvivor;

import java.util.ArrayList;
import zombie.UsedFromLua;
import zombie.core.random.Rand;
import zombie.inventory.ItemPickerJava;
import zombie.iso.BuildingDef;
import zombie.iso.IsoGridSquare;
import zombie.iso.objects.IsoDeadBody;
import zombie.randomizedWorld.randomizedDeadSurvivor.RandomizedDeadSurvivorBase;
import zombie.util.list.PZArrayUtil;

@UsedFromLua
public final class RDSSpecificProfession
extends RandomizedDeadSurvivorBase {
    private final ArrayList<String> specificProfessionDistribution = new ArrayList();

    @Override
    public void randomizeDeadSurvivor(BuildingDef def) {
        String profession = PZArrayUtil.pickRandom(this.specificProfessionDistribution);
        ItemPickerJava.ItemPickerRoom prof = ItemPickerJava.rooms.get(profession);
        String outfit = prof.outfit;
        IsoGridSquare sq = def.getFreeSquareInRoom();
        if (sq == null) {
            return;
        }
        IsoDeadBody body = outfit != null && Rand.Next(2) == 0 ? RDSSpecificProfession.createRandomDeadBody(sq, null, 0, 0, outfit) : RDSSpecificProfession.createRandomDeadBody(sq.getX(), sq.getY(), sq.getZ(), null, 0);
        if (body == null) {
            return;
        }
        ItemPickerJava.rollItem(prof.containers.get("body"), body.getContainer(), true, null, null);
    }

    public RDSSpecificProfession() {
        this.specificProfessionDistribution.add("Carpenter");
        this.specificProfessionDistribution.add("Electrician");
        this.specificProfessionDistribution.add("Farmer");
        this.specificProfessionDistribution.add("Nurse");
        this.specificProfessionDistribution.add("Chef");
    }
}

