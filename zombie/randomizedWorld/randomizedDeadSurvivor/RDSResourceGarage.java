/*
 * Decompiled with CFR 0.152.
 */
package zombie.randomizedWorld.randomizedDeadSurvivor;

import zombie.UsedFromLua;
import zombie.characters.IsoPlayer;
import zombie.core.stash.StashSystem;
import zombie.iso.BuildingDef;
import zombie.iso.RoomDef;
import zombie.iso.SpawnPoints;
import zombie.network.GameClient;
import zombie.network.GameServer;
import zombie.randomizedWorld.randomizedDeadSurvivor.RandomizedDeadSurvivorBase;

@UsedFromLua
public final class RDSResourceGarage
extends RandomizedDeadSurvivorBase {
    public RDSResourceGarage() {
        this.name = "Resource Garage";
        this.setChance(10);
    }

    @Override
    public void randomizeDeadSurvivor(BuildingDef def) {
        RoomDef room = this.getRoom(def, "garagestorage");
        if (room == null) {
            room = this.getRoom(def, "shed");
        }
        if (room == null) {
            room = this.getRoom(def, "garage");
        }
        if (room == null) {
            room = this.getRoom(def, "farmstorage");
        }
        if (room == null) {
            return;
        }
        room.getIsoRoom().spawnRandomWorkstation();
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
        boolean garageStorage = false;
        for (int i = 0; i < def.rooms.size(); ++i) {
            RoomDef room = def.rooms.get(i);
            if (!"garagestorage".equals(room.name) && !"shed".equals(room.name) && !"garage".equals(room.name) && !"farmstorage".equals(room.name) || room.area < 9) continue;
            garageStorage = true;
            break;
        }
        if (!garageStorage) {
            this.debugLine = "No shed/garage or is too small";
        }
        return garageStorage;
    }
}

