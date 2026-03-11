/*
 * Decompiled with CFR 0.152.
 */
package zombie.randomizedWorld.randomizedDeadSurvivor;

import java.util.ArrayList;
import zombie.UsedFromLua;
import zombie.characters.IsoPlayer;
import zombie.core.random.Rand;
import zombie.core.stash.StashSystem;
import zombie.iso.BuildingDef;
import zombie.iso.IsoGridSquare;
import zombie.iso.RoomDef;
import zombie.iso.SpawnPoints;
import zombie.network.GameClient;
import zombie.network.GameServer;
import zombie.randomizedWorld.randomizedDeadSurvivor.RandomizedDeadSurvivorBase;
import zombie.util.list.PZArrayUtil;

@UsedFromLua
public final class RDSBandPractice
extends RandomizedDeadSurvivorBase {
    private final ArrayList<String> instrumentsList = new ArrayList();

    public RDSBandPractice() {
        this.name = "Band Practice";
        this.setChance(10);
        this.setMaximumDays(60);
        this.instrumentsList.add("GuitarAcoustic");
        this.instrumentsList.add("GuitarElectric");
        this.instrumentsList.add("GuitarElectric");
        this.instrumentsList.add("GuitarElectric");
        this.instrumentsList.add("GuitarElectricBass");
        this.instrumentsList.add("GuitarElectricBass");
        this.instrumentsList.add("GuitarElectricBass");
        this.instrumentsList.add("Harmonica");
        this.instrumentsList.add("Microphone");
        this.instrumentsList.add("Bag_ProtectiveCaseBulky_Audio");
        this.instrumentsList.add("Speaker");
    }

    @Override
    public void randomizeDeadSurvivor(BuildingDef def) {
        this.spawnItemsInContainers(def, "BandPractice", 90);
        RoomDef room = this.getRoom(def, "garagestorage");
        if (room == null) {
            room = this.getRoom(def, "shed");
        }
        if (room == null) {
            room = this.getRoom(def, "garage");
        }
        this.addZombies(def, Rand.Next(2, 4), "Rocker", 20, room);
        IsoGridSquare sq = RDSBandPractice.getRandomSpawnSquare(room);
        if (sq == null) {
            return;
        }
        RDSBandPractice.trySpawnStoryItem(PZArrayUtil.pickRandom(this.instrumentsList), sq, Rand.Next(0.0f, 0.5f), Rand.Next(0.0f, 0.5f), 0.0f);
        if (Rand.Next(4) == 0) {
            RDSBandPractice.trySpawnStoryItem(PZArrayUtil.pickRandom(this.instrumentsList), sq, Rand.Next(0.0f, 0.5f), Rand.Next(0.0f, 0.5f), 0.0f);
        }
        if (Rand.Next(4) == 0) {
            RDSBandPractice.trySpawnStoryItem(PZArrayUtil.pickRandom(this.instrumentsList), sq, Rand.Next(0.0f, 0.5f), Rand.Next(0.0f, 0.5f), 0.0f);
        }
        def.alarmed = false;
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
            if (!"garagestorage".equals(room.name) && !"shed".equals(room.name) && !"garage".equals(room.name) || room.area < 9) continue;
            garageStorage = true;
            break;
        }
        if (!garageStorage) {
            this.debugLine = "No shed/garage or is too small";
        }
        return garageStorage;
    }
}

