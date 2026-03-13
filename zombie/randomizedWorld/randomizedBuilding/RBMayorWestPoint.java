/*
 * Decompiled with CFR 0.152.
 */
package zombie.randomizedWorld.randomizedBuilding;

import java.util.ArrayList;
import zombie.UsedFromLua;
import zombie.characters.IsoZombie;
import zombie.characters.SurvivorDesc;
import zombie.iso.BuildingDef;
import zombie.iso.IsoCell;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoWorld;
import zombie.randomizedWorld.randomizedBuilding.RBBasic;
import zombie.randomizedWorld.randomizedBuilding.RandomizedBuildingBase;

@UsedFromLua
public final class RBMayorWestPoint
extends RandomizedBuildingBase {
    @Override
    public void randomizeBuilding(BuildingDef def) {
        IsoCell cell = IsoWorld.instance.currentCell;
        for (int x = def.x - 1; x < def.x2 + 1; ++x) {
            for (int y = def.y - 1; y < def.y2 + 1; ++y) {
                for (int z = -32; z < 31; ++z) {
                    IsoGridSquare sq = cell.getGridSquare(x, y, z);
                    if (sq == null || !this.roomValid(sq)) continue;
                    RBBasic.doOfficeStuff(sq);
                }
            }
        }
        ArrayList<IsoZombie> zeds = this.addZombies(def, 1, "Mayor_West_point", 0, null);
        if (zeds.isEmpty()) {
            return;
        }
        IsoZombie dean = zeds.get(0);
        dean.getHumanVisual().setSkinTextureIndex(1);
        SurvivorDesc desc = dean.getDescriptor();
        if (desc == null) {
            return;
        }
        desc.setForename("Richard");
        desc.setSurname("Rosenwald");
    }

    public boolean roomValid(IsoGridSquare sq) {
        return sq.getRoom() != null && "mayorwestpointoffice".equals(sq.getRoom().getName());
    }

    @Override
    public boolean isValid(BuildingDef def, boolean force) {
        return def.getRoom("mayorwestpointoffice") != null;
    }

    public RBMayorWestPoint() {
        this.name = "MayorWestPoint";
        this.reallyAlwaysForce = true;
        this.setAlwaysDo(true);
    }
}

