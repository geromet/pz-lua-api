/*
 * Decompiled with CFR 0.152.
 */
package zombie.randomizedWorld.randomizedBuilding;

import zombie.UsedFromLua;
import zombie.core.random.Rand;
import zombie.iso.BuildingDef;
import zombie.iso.IsoCell;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoObject;
import zombie.iso.IsoWorld;
import zombie.randomizedWorld.randomizedBuilding.RBBasic;
import zombie.randomizedWorld.randomizedBuilding.RandomizedBuildingBase;

@UsedFromLua
public final class RBClinic
extends RandomizedBuildingBase {
    @Override
    public void randomizeBuilding(BuildingDef def) {
        IsoCell cell = IsoWorld.instance.currentCell;
        for (int x = def.x - 1; x < def.x2 + 1; ++x) {
            for (int y = def.y - 1; y < def.y2 + 1; ++y) {
                for (int z = -32; z < 31; ++z) {
                    IsoGridSquare sq = cell.getGridSquare(x, y, z);
                    if (sq == null || !this.roomValid(sq)) continue;
                    for (int i = 0; i < sq.getObjects().size(); ++i) {
                        IsoObject obj = sq.getObjects().get(i);
                        if (!Rand.NextBool(2) || !(obj.getSurfaceOffsetNoTable() > 0.0f) || obj.getContainer() != null || sq.hasWater() || obj.hasFluid() || !obj.hasAdjacentCanStandSquare()) continue;
                        int nbrItem = Rand.Next(1, 3);
                        for (int j = 0; j < nbrItem; ++j) {
                            RBClinic.trySpawnStoryItem(RBBasic.getMedicallutterItem(), sq, Rand.Next(0.4f, 0.6f), Rand.Next(0.4f, 0.6f), obj.getSurfaceOffsetNoTable() / 96.0f);
                        }
                    }
                }
            }
        }
    }

    public boolean roomValid(IsoGridSquare sq) {
        return sq.getRoom() != null && ("hospitalroom".equals(sq.getRoom().getName()) || "clinic".equals(sq.getRoom().getName()) || "medical".equals(sq.getRoom().getName()));
    }

    @Override
    public boolean isValid(BuildingDef def, boolean force) {
        return def.getRoom("medical") != null || def.getRoom("clinic") != null || force;
    }

    public RBClinic() {
        this.name = "Clinic (Vet, Doctor..)";
        this.setAlwaysDo(true);
    }
}

