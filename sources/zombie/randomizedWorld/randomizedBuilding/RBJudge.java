/*
 * Decompiled with CFR 0.152.
 */
package zombie.randomizedWorld.randomizedBuilding;

import java.util.ArrayList;
import zombie.UsedFromLua;
import zombie.characters.IsoZombie;
import zombie.characters.SurvivorDesc;
import zombie.inventory.InventoryItem;
import zombie.inventory.InventoryItemFactory;
import zombie.iso.BuildingDef;
import zombie.iso.IsoCell;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoWorld;
import zombie.iso.RoomDef;
import zombie.randomizedWorld.randomizedBuilding.RBBasic;
import zombie.randomizedWorld.randomizedBuilding.RandomizedBuildingBase;

@UsedFromLua
public final class RBJudge
extends RandomizedBuildingBase {
    @Override
    public void randomizeBuilding(BuildingDef def) {
        IsoCell cell = IsoWorld.instance.currentCell;
        for (int x = def.x - 1; x < def.x2 + 1; ++x) {
            for (int y = def.y - 1; y < def.y2 + 1; ++y) {
                for (int z = -32; z < 31; ++z) {
                    IsoGridSquare sq = cell.getGridSquare(x, y, z);
                    if (sq == null || !this.roomValid(sq)) continue;
                    RBBasic.doJudgeStuff(sq);
                }
            }
        }
        RoomDef room = def.getRoom("judgematthassset");
        ArrayList<IsoZombie> zeds = this.addZombies(def, 1, "Judge_Matt_Hass", 0, room);
        if (zeds == null || zeds.isEmpty()) {
            return;
        }
        IsoZombie judge = (IsoZombie)zeds.getFirst();
        judge.getHumanVisual().setSkinTextureIndex(1);
        SurvivorDesc desc = judge.getDescriptor();
        if (desc == null) {
            return;
        }
        desc.setForename("Matt");
        desc.setSurname("Hass");
        Object revolver = InventoryItemFactory.CreateItem("Base.Revolver_Long");
        if (revolver == null) {
            return;
        }
        judge.addItemToSpawnAtDeath((InventoryItem)revolver);
    }

    public boolean roomValid(IsoGridSquare sq) {
        return sq.getRoom() != null && "judgematthassset".equals(sq.getRoom().getName());
    }

    @Override
    public boolean isValid(BuildingDef def, boolean force) {
        return def.getRoom("judgematthassset") != null;
    }

    public RBJudge() {
        this.name = "JudgeMattHass";
        this.reallyAlwaysForce = true;
        this.setAlwaysDo(true);
    }
}

