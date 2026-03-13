/*
 * Decompiled with CFR 0.152.
 */
package zombie.randomizedWorld.randomizedDeadSurvivor;

import zombie.UsedFromLua;
import zombie.VirtualZombieManager;
import zombie.ZombieSpawnRecorder;
import zombie.characters.IsoZombie;
import zombie.core.random.Rand;
import zombie.iso.BuildingDef;
import zombie.iso.IsoDirections;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoWorld;
import zombie.iso.RoomDef;
import zombie.iso.areas.IsoRoom;
import zombie.iso.objects.IsoBarricade;
import zombie.iso.objects.IsoDeadBody;
import zombie.iso.objects.IsoDoor;
import zombie.iso.objects.interfaces.BarricadeAble;
import zombie.network.GameServer;
import zombie.randomizedWorld.randomizedDeadSurvivor.RandomizedDeadSurvivorBase;

@UsedFromLua
public final class RDSZombieLockedBathroom
extends RandomizedDeadSurvivorBase {
    @Override
    public void randomizeDeadSurvivor(BuildingDef def) {
        IsoDeadBody body = null;
        for (int i = 0; i < def.rooms.size(); ++i) {
            IsoRoom isoRoom;
            IsoGridSquare g;
            RoomDef room = def.rooms.get(i);
            if (!"bathroom".equals(room.name)) continue;
            if (IsoWorld.getZombiesEnabled() && (g = IsoWorld.instance.currentCell.getGridSquare(room.getX(), room.getY(), room.getZ())) != null && g.getRoom() != null && (g = (isoRoom = g.getRoom()).getRandomFreeSquare()) != null) {
                VirtualZombieManager.instance.choices.clear();
                VirtualZombieManager.instance.choices.add(g);
                IsoZombie zombie = VirtualZombieManager.instance.createRealZombieAlways(IsoDirections.getRandom(), false);
                ZombieSpawnRecorder.instance.record(zombie, this.getClass().getSimpleName());
            }
            for (int x = room.x - 1; x < room.x2 + 1; ++x) {
                for (int y = room.y - 1; y < room.y2 + 1; ++y) {
                    IsoBarricade barricade;
                    IsoDoor door;
                    IsoGridSquare sq = IsoWorld.instance.getCell().getGridSquare(x, y, room.getZ());
                    if (sq == null || (door = sq.getIsoDoor()) == null || !this.isDoorToRoom(door, room)) continue;
                    if (door.IsOpen()) {
                        door.ToggleDoor(null);
                    }
                    if ((barricade = IsoBarricade.AddBarricadeToObject((BarricadeAble)door, sq.getRoom().def == room)) != null) {
                        barricade.addPlank(null, null);
                        if (GameServer.server) {
                            barricade.transmitCompleteItemToClients();
                        }
                    }
                    body = this.addDeadBodyTheOtherSide(door);
                    break;
                }
                if (body != null) break;
            }
            if (body != null) {
                body.setPrimaryHandItem(this.addWeapon("Base.Pistol", true));
            }
            return;
        }
    }

    private boolean isDoorToRoom(IsoDoor door, RoomDef roomDef) {
        if (door == null || roomDef == null) {
            return false;
        }
        IsoGridSquare sqInside = door.getSquare();
        IsoGridSquare sqOpposite = door.getOppositeSquare();
        if (sqInside == null || sqOpposite == null) {
            return false;
        }
        return sqInside.getRoomID() == roomDef.id != (sqOpposite.getRoomID() == roomDef.id);
    }

    private boolean checkIsBathroom(IsoGridSquare sq) {
        return sq.getRoom() != null && "bathroom".equals(sq.getRoom().getName());
    }

    private IsoDeadBody addDeadBodyTheOtherSide(IsoDoor obj) {
        IsoGridSquare sq;
        if (obj.north) {
            sq = IsoWorld.instance.getCell().getGridSquare(obj.getX(), obj.getY(), obj.getZ());
            if (this.checkIsBathroom(sq)) {
                sq = IsoWorld.instance.getCell().getGridSquare(obj.getX(), obj.getY() - 1.0f, obj.getZ());
            }
        } else {
            sq = IsoWorld.instance.getCell().getGridSquare(obj.getX(), obj.getY(), obj.getZ());
            if (this.checkIsBathroom(sq)) {
                sq = IsoWorld.instance.getCell().getGridSquare(obj.getX() - 1.0f, obj.getY(), obj.getZ());
            }
        }
        return RDSZombieLockedBathroom.createRandomDeadBody(sq.getX(), sq.getY(), sq.getZ(), null, Rand.Next(5, 10));
    }

    public RDSZombieLockedBathroom() {
        this.name = "Locked in Bathroom";
        this.setChance(5);
    }
}

