/*
 * Decompiled with CFR 0.152.
 */
package zombie.randomizedWorld.randomizedDeadSurvivor;

import java.util.ArrayList;
import zombie.UsedFromLua;
import zombie.characterTextures.BloodBodyPartType;
import zombie.characters.IsoZombie;
import zombie.core.math.PZMath;
import zombie.core.random.Rand;
import zombie.iso.BuildingDef;
import zombie.iso.IsoDirections;
import zombie.iso.RoomDef;
import zombie.iso.objects.IsoDeadBody;
import zombie.randomizedWorld.randomizedDeadSurvivor.RandomizedDeadSurvivorBase;

@UsedFromLua
public final class RDSHockeyPsycho
extends RandomizedDeadSurvivorBase {
    public RDSHockeyPsycho() {
        this.name = "Hockey Psycho (friday 13th!)";
        this.setUnique(true);
        this.setChance(1);
    }

    @Override
    public void randomizeDeadSurvivor(BuildingDef def) {
        RoomDef room = this.getLivingRoomOrKitchen(def);
        ArrayList<IsoZombie> zeds = this.addZombies(def, 1, "HockeyPsycho", 0, room);
        if (zeds != null && !zeds.isEmpty()) {
            IsoZombie zed = zeds.get(0);
            zed.addBlood(BloodBodyPartType.Head, true, true, true);
            for (int i = 0; i < 10; ++i) {
                zed.addBlood(null, true, false, true);
                zed.addDirt(null, Rand.Next(0, 3), true);
            }
        }
        for (int i = 0; i < 10; ++i) {
            IsoDeadBody body = RDSHockeyPsycho.createRandomDeadBody(this.getRandomRoom(def, 2), Rand.Next(5, 20));
            if (body == null) continue;
            this.addTraitOfBlood(IsoDirections.getRandom(), 15, PZMath.fastfloor(body.getX()), PZMath.fastfloor(body.getY()), PZMath.fastfloor(body.getZ()));
            this.addTraitOfBlood(IsoDirections.getRandom(), 15, PZMath.fastfloor(body.getX()), PZMath.fastfloor(body.getY()), PZMath.fastfloor(body.getZ()));
            this.addTraitOfBlood(IsoDirections.getRandom(), 15, PZMath.fastfloor(body.getX()), PZMath.fastfloor(body.getY()), PZMath.fastfloor(body.getZ()));
        }
        def.alarmed = false;
    }
}

