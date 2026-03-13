/*
 * Decompiled with CFR 0.152.
 */
package zombie.randomizedWorld.randomizedDeadSurvivor;

import java.util.ArrayList;
import zombie.UsedFromLua;
import zombie.characters.animals.IsoAnimal;
import zombie.core.random.Rand;
import zombie.iso.BuildingDef;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoWorld;
import zombie.iso.RoomDef;
import zombie.iso.objects.IsoDeadBody;
import zombie.randomizedWorld.randomizedDeadSurvivor.RandomizedDeadSurvivorBase;

@UsedFromLua
public final class RDSDevouredByRats
extends RandomizedDeadSurvivorBase {
    public RDSDevouredByRats() {
        this.name = "Devoured By Rats";
        this.setChance(1);
        this.setMinimumDays(30);
        this.setUnique(true);
        this.isRat = true;
    }

    @Override
    public void randomizeDeadSurvivor(BuildingDef def) {
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
        if ((room = this.getRoomNoKids(def, roomType)) == null) {
            room = this.getRoom(def, "kitchen");
        }
        if (room == null) {
            room = this.getRoom(def, "livingroom");
        }
        if (room == null) {
            room = this.getRoomNoKids(def, "bedroom");
        }
        if (room == null) {
            return;
        }
        int nbrOfSkel = Rand.Next(1, 4);
        for (int i = 0; i < nbrOfSkel; ++i) {
            IsoDeadBody body = this.createSkeletonCorpse(room);
            if (body == null) continue;
            body.getHumanVisual().setSkinTextureIndex(2);
            this.addBloodSplat(body.getCurrentSquare(), Rand.Next(7, 12));
        }
        ArrayList<IsoGridSquare> usedSquares = new ArrayList<IsoGridSquare>();
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
        for (int i = 0; i < nbrOfRats; ++i) {
            IsoGridSquare square = room.getFreeUnoccupiedSquare();
            String breed = "grey";
            if (this.getRoom(def, "laboratory") != null && !Rand.NextBool(3)) {
                breed = "white";
            }
            if (square == null || !square.isFree(true) || usedSquares.contains(square)) continue;
            IsoAnimal animal = Rand.NextBool(2) ? new IsoAnimal(IsoWorld.instance.getCell(), square.getX(), square.getY(), square.getZ(), "rat", breed) : new IsoAnimal(IsoWorld.instance.getCell(), square.getX(), square.getY(), square.getZ(), "ratfemale", breed);
            animal.addToWorld();
            animal.randomizeAge();
            if (Rand.NextBool(3)) {
                animal.setStateEventDelayTimer(0.0f);
                continue;
            }
            usedSquares.add(square);
        }
        int nbrOfPoops = Rand.Next(min, max);
        for (int i = 0; i < nbrOfPoops; ++i) {
            IsoGridSquare square = room.getFreeSquare();
            if (square == null || square.isOutside() || square.getRoom() == null || !square.hasRoomDef()) continue;
            this.addItemOnGround(square, "Base.Dung_Rat");
        }
        def.alarmed = false;
    }
}

