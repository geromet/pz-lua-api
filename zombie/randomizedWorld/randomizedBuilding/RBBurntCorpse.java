/*
 * Decompiled with CFR 0.152.
 */
package zombie.randomizedWorld.randomizedBuilding;

import java.util.ArrayList;
import zombie.UsedFromLua;
import zombie.characters.IsoZombie;
import zombie.core.random.Rand;
import zombie.core.stash.StashSystem;
import zombie.iso.BuildingDef;
import zombie.iso.IsoCell;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoWorld;
import zombie.iso.SpawnPoints;
import zombie.iso.objects.IsoDeadBody;
import zombie.randomizedWorld.randomizedBuilding.RandomizedBuildingBase;

@UsedFromLua
public final class RBBurntCorpse
extends RandomizedBuildingBase {
    @Override
    public void randomizeBuilding(BuildingDef def) {
        def.alarmed = false;
        def.setHasBeenVisited(true);
        IsoCell cell = IsoWorld.instance.currentCell;
        for (int x = def.x - 1; x < def.x2 + 1; ++x) {
            for (int y = def.y - 1; y < def.y2 + 1; ++y) {
                for (int z = -32; z < 31; ++z) {
                    IsoGridSquare sq = cell.getGridSquare(x, y, z);
                    if (sq == null || Rand.Next(100) >= 60) continue;
                    sq.Burn(false);
                }
            }
        }
        def.setAllExplored(true);
        def.alarmed = false;
        ArrayList<IsoZombie> zedList = this.addZombies(def, Rand.Next(3, 7), null, null, null);
        if (zedList == null) {
            return;
        }
        for (int i = 0; i < zedList.size(); ++i) {
            IsoZombie zombie = zedList.get(i);
            zombie.setSkeleton(true);
            zombie.getHumanVisual().setSkinTextureIndex(0);
            new IsoDeadBody(zombie, false);
        }
    }

    @Override
    public boolean isValid(BuildingDef def, boolean force) {
        if (!super.isValid(def, force)) {
            return false;
        }
        if (def.getRooms().size() > 20) {
            return false;
        }
        if (SpawnPoints.instance.isSpawnBuilding(def)) {
            this.debugLine = "Spawn houses are invalid";
            return false;
        }
        if (StashSystem.isStashBuilding(def)) {
            this.debugLine = "Stash buildings are invalid";
            return false;
        }
        return true;
    }

    public RBBurntCorpse() {
        this.name = "Burnt with corpses";
        this.setChance(3);
    }
}

