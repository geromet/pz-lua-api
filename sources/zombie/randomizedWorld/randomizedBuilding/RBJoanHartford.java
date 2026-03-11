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
public final class RBJoanHartford
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
        RoomDef room = def.getRoom("joanstudio");
        ArrayList<IsoZombie> zeds = this.addZombies(def, 1, "Joan", 100, room);
        if (zeds == null || zeds.isEmpty()) {
            return;
        }
        IsoZombie joan = zeds.get(0);
        if (joan == null) {
            return;
        }
        joan.getHumanVisual().setSkinTextureIndex(1);
        SurvivorDesc desc = joan.getDescriptor();
        if (desc == null) {
            return;
        }
        desc.setForename("Joan");
        desc.setSurname("Hartford");
        Object pressID = InventoryItemFactory.CreateItem("Base.PressID");
        if (pressID == null) {
            return;
        }
        ((InventoryItem)pressID).nameAfterDescriptor(desc);
        joan.addItemToSpawnAtDeath((InventoryItem)pressID);
    }

    public boolean roomValid(IsoGridSquare sq) {
        return sq.getRoom() != null && "joanstudio".equals(sq.getRoom().getName());
    }

    @Override
    public boolean isValid(BuildingDef def, boolean force) {
        return def.getRoom("joanstudio") != null;
    }

    public RBJoanHartford() {
        this.name = "JoanHartford";
        this.reallyAlwaysForce = true;
        this.setAlwaysDo(true);
    }
}

