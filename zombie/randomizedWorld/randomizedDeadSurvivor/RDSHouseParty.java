/*
 * Decompiled with CFR 0.152.
 */
package zombie.randomizedWorld.randomizedDeadSurvivor;

import zombie.UsedFromLua;
import zombie.characters.IsoPlayer;
import zombie.core.random.Rand;
import zombie.core.stash.StashSystem;
import zombie.iso.BuildingDef;
import zombie.iso.RoomDef;
import zombie.iso.SpawnPoints;
import zombie.network.GameClient;
import zombie.network.GameServer;
import zombie.randomizedWorld.randomizedDeadSurvivor.RandomizedDeadSurvivorBase;

@UsedFromLua
public final class RDSHouseParty
extends RandomizedDeadSurvivorBase {
    public RDSHouseParty() {
        this.name = "House Party";
        this.setChance(4);
    }

    @Override
    public boolean isValid(BuildingDef def, boolean force) {
        this.debugLine = "";
        if (GameClient.client) {
            return false;
        }
        if (def.isAllExplored() && !force) {
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
        if (!force) {
            for (int i = 0; i < GameServer.Players.size(); ++i) {
                IsoPlayer player = GameServer.Players.get(i);
                if (player.getSquare() == null || player.getSquare().getBuilding() == null || player.getSquare().getBuilding().def != def) continue;
                return false;
            }
        }
        if (this.getRoom(def, "livingroom") != null) {
            return true;
        }
        this.debugLine = "No living room";
        return false;
    }

    @Override
    public void randomizeDeadSurvivor(BuildingDef def) {
        RoomDef room = this.getRoom(def, "livingroom");
        this.addZombies(def, Rand.Next(5, 8), "Party", null, room);
        this.addRandomItemsOnGround(room, this.getHousePartyClutter(), Rand.Next(4, 7));
        def.alarmed = false;
    }
}

