/*
 * Decompiled with CFR 0.152.
 */
package zombie.randomizedWorld.randomizedDeadSurvivor;

import zombie.UsedFromLua;
import zombie.characters.IsoPlayer;
import zombie.characters.animals.IsoAnimal;
import zombie.core.random.Rand;
import zombie.core.stash.StashSystem;
import zombie.iso.BuildingDef;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoWorld;
import zombie.iso.RoomDef;
import zombie.iso.SpawnPoints;
import zombie.iso.objects.IsoDeadBody;
import zombie.network.GameClient;
import zombie.network.GameServer;
import zombie.randomizedWorld.randomizedDeadSurvivor.RDSRatInfested;
import zombie.randomizedWorld.randomizedDeadSurvivor.RandomizedDeadSurvivorBase;

@UsedFromLua
public final class RDSRatKing
extends RandomizedDeadSurvivorBase {
    public RDSRatKing() {
        this.name = "Rat King";
        this.setChance(1);
        this.setUnique(true);
        this.isRat = true;
    }

    @Override
    public void randomizeDeadSurvivor(BuildingDef def) {
        int i;
        int min;
        RoomDef room;
        String roomType = "bedroom";
        int roll = Rand.Next(3);
        if (roll == 0) {
            roomType = "kitchen";
        }
        if (roll == 1) {
            roomType = "livingroom";
        }
        if ((room = this.getRoom(def, roomType)) == null) {
            room = this.getRoom(def, "kitchen");
        }
        if (room == null) {
            room = this.getRoom(def, "livingroom");
        }
        if (room == null) {
            room = this.getRoom(def, "bedroom");
        }
        if (room == null) {
            return;
        }
        this.addItemOnGround(room.getFreeSquare(), "Base.RatKing");
        int max = room.getIsoRoom().getSquares().size();
        if (max > 21) {
            max = 21;
        }
        if ((min = max / 2) < 1) {
            min = 1;
        }
        if (min > 9) {
            min = 9;
        }
        int nbrOfRats = Rand.Next(min, max);
        for (int i2 = 0; i2 < nbrOfRats; ++i2) {
            IsoGridSquare square = room.getFreeUnoccupiedSquare();
            if (square == null || !square.isFree(true)) continue;
            IsoAnimal animal = Rand.NextBool(2) ? new IsoAnimal(IsoWorld.instance.getCell(), square.getX(), square.getY(), square.getZ(), "rat", "grey") : new IsoAnimal(IsoWorld.instance.getCell(), square.getX(), square.getY(), square.getZ(), "ratfemale", "grey");
            animal.randomizeAge();
            IsoDeadBody deadAnimal = new IsoDeadBody(animal, false);
            deadAnimal.addToWorld();
        }
        int nbrOfPoops = Rand.Next(min, max);
        for (i = 0; i < nbrOfPoops; ++i) {
            IsoGridSquare square = room.getFreeSquare();
            if (square == null || square.isOutside() || square.getRoom() == null || !square.hasRoomDef()) continue;
            this.addItemOnGround(square, "Base.Dung_Rat");
        }
        RDSRatInfested.ratRoom(room);
        for (i = 0; i < def.rooms.size(); ++i) {
            RDSRatInfested.ratRoom(def.rooms.get(i));
        }
    }

    @Override
    public boolean isValid(BuildingDef def, boolean force) {
        this.debugLine = "";
        if (GameClient.client) {
            return false;
        }
        if (!force && !Rand.NextBool(100)) {
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
        if (def.isAllExplored() && !force) {
            return false;
        }
        if (this.getRoom(def, "kitchen") == null && this.getRoom(def, "bedroom") == null) {
            return false;
        }
        if (!force) {
            for (int i = 0; i < GameServer.Players.size(); ++i) {
                IsoPlayer player = GameServer.Players.get(i);
                if (player.getSquare() == null || player.getSquare().getBuilding() == null || player.getSquare().getBuilding().def != def) continue;
                return false;
            }
        }
        if (def.getRooms().size() > 100) {
            this.debugLine = "Building is too large";
            return false;
        }
        return true;
    }
}

