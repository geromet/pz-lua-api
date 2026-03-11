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
public final class RBHairSalon
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
                        if (!Rand.NextBool(3) || !(obj.getSurfaceOffsetNoTable() > 0.0f) || sq.hasWater() || obj.hasFluid() || obj.getProperties().get("BedType") != null || !obj.hasAdjacentCanStandSquare()) continue;
                        RBHairSalon.trySpawnStoryItem(RBBasic.getHairSalonClutterItem(), sq, 0.5f, 0.5f, obj.getSurfaceOffsetNoTable() / 96.0f);
                    }
                }
            }
        }
    }

    public boolean roomValid(IsoGridSquare sq) {
        return sq.getRoom() != null && "aesthetic".equals(sq.getRoom().getName());
    }

    @Override
    public boolean isValid(BuildingDef def, boolean force) {
        return def.getRoom("aesthetic") != null || force;
    }

    public RBHairSalon() {
        this.name = "Hair Salon";
        this.setAlwaysDo(true);
    }
}

