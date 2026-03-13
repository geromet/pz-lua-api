/*
 * Decompiled with CFR 0.152.
 */
package zombie.iso;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import zombie.SandboxOptions;
import zombie.UsedFromLua;
import zombie.core.random.Rand;
import zombie.iso.BuildingDef;
import zombie.iso.IsoWorld;
import zombie.iso.RoomDef;
import zombie.iso.zones.Zone;

@UsedFromLua
public final class IsoMetaChunk {
    public static final float zombiesMinPerChunk = 0.06f;
    public static final float zombiesFullPerChunk = 12.0f;
    private byte zombieIntensity;
    private Zone[] zones;
    private int zonesSize;
    private RoomDef[] rooms;
    private int roomsSize;

    public int getZonesSize() {
        return this.zonesSize;
    }

    public void compactZoneArray() {
        if (this.zones == null || this.zonesSize == this.zones.length) {
            return;
        }
        this.zones = Arrays.copyOf(this.zones, this.zonesSize);
    }

    public void compactRoomDefArray() {
        if (this.rooms == null || this.roomsSize == this.rooms.length) {
            return;
        }
        this.rooms = Arrays.copyOf(this.rooms, this.roomsSize);
    }

    public boolean doesHaveForaging() {
        if (this.zones == null) {
            return false;
        }
        List<String> foragingZones = IsoWorld.instance.getBiomeMap().getForagingZones();
        return Arrays.stream(this.zones).anyMatch(z -> z != null && foragingZones.contains(z.type));
    }

    public boolean doesHaveZone(String zone) {
        if (this.zones == null) {
            return false;
        }
        return Arrays.stream(this.zones).anyMatch(z -> z != null && z.type.equalsIgnoreCase(zone));
    }

    public int getRoomsSize() {
        return this.roomsSize;
    }

    public float getZombieIntensity(boolean bRandom) {
        float zombieIntensity = this.zombieIntensity & 0xFF;
        if (SandboxOptions.instance.distribution.getValue() == 2) {
            zombieIntensity = 128.0f;
        }
        zombieIntensity *= 0.5f;
        if (SandboxOptions.instance.zombies.getValue() == 1) {
            zombieIntensity *= 4.0f;
        } else if (SandboxOptions.instance.zombies.getValue() == 2) {
            zombieIntensity *= 3.0f;
        } else if (SandboxOptions.instance.zombies.getValue() == 3) {
            zombieIntensity *= 2.0f;
        } else if (SandboxOptions.instance.zombies.getValue() == 5) {
            zombieIntensity *= 0.35f;
        } else if (SandboxOptions.instance.zombies.getValue() == 6) {
            zombieIntensity = 0.0f;
        }
        float delta = zombieIntensity / 255.0f;
        float dif = 11.94f;
        zombieIntensity = 0.06f + (dif *= delta);
        if (!bRandom) {
            return zombieIntensity;
        }
        float chance = delta * 10.0f;
        if (Rand.Next(3) == 0) {
            return 0.0f;
        }
        chance *= 0.5f;
        int random = 1000;
        if (SandboxOptions.instance.zombies.getValue() == 1) {
            random = (int)((float)random / 2.0f);
        } else if (SandboxOptions.instance.zombies.getValue() == 2) {
            random = (int)((float)random / 1.7f);
        } else if (SandboxOptions.instance.zombies.getValue() == 3) {
            random = (int)((float)random / 1.5f);
        } else if (SandboxOptions.instance.zombies.getValue() == 5) {
            random = (int)((float)random * 1.5f);
        }
        if ((float)Rand.Next(random) < chance && IsoWorld.getZombiesEnabled() && (zombieIntensity = 120.0f) > 12.0f) {
            zombieIntensity = 12.0f;
        }
        return zombieIntensity;
    }

    public float getZombieIntensity() {
        return this.getZombieIntensity(true);
    }

    public void setZombieIntensity(byte zombieIntensity) {
        this.zombieIntensity = zombieIntensity;
    }

    public float getLootZombieIntensity() {
        float zombieIntensity = this.zombieIntensity & 0xFF;
        float delta = zombieIntensity / 255.0f;
        float dif = 11.94f;
        zombieIntensity = 0.06f + (dif *= delta);
        float chance = delta * 10.0f;
        if ((float)Rand.Next(300) <= chance) {
            zombieIntensity = 120.0f;
        }
        if (IsoWorld.getZombiesDisabled()) {
            return 400.0f;
        }
        return zombieIntensity;
    }

    public int getUnadjustedZombieIntensity() {
        return this.zombieIntensity & 0xFF;
    }

    public void addZone(Zone zone) {
        if (this.zones == null) {
            this.zones = new Zone[4];
        }
        if (this.zonesSize == this.zones.length) {
            Zone[] newZones = new Zone[this.zones.length + 4];
            System.arraycopy(this.zones, 0, newZones, 0, this.zonesSize);
            this.zones = newZones;
        }
        this.zones[this.zonesSize++] = zone;
    }

    public void removeZone(Zone zone) {
        if (this.zones == null) {
            return;
        }
        for (int i = 0; i < this.zonesSize; ++i) {
            if (this.zones[i] != zone) continue;
            while (i < this.zonesSize - 1) {
                this.zones[i] = this.zones[i + 1];
                ++i;
            }
            this.zones[this.zonesSize - 1] = null;
            --this.zonesSize;
            break;
        }
    }

    public Zone getZone(int index) {
        if (index < 0 || index >= this.zonesSize) {
            return null;
        }
        return this.zones[index];
    }

    public Zone getZoneAt(int x, int y, int z) {
        if (this.zones == null || this.zonesSize <= 0) {
            return null;
        }
        Zone highest = null;
        for (int i = this.zonesSize - 1; i >= 0; --i) {
            Zone zone = this.zones[i];
            if (!zone.contains(x, y, z)) continue;
            if (zone.isPreferredZoneForSquare) {
                return zone;
            }
            if (highest != null) continue;
            highest = zone;
        }
        return highest;
    }

    public ArrayList<Zone> getZonesAt(int x, int y, int z, ArrayList<Zone> result) {
        for (int i = 0; i < this.zonesSize; ++i) {
            Zone zone = this.zones[i];
            if (!zone.contains(x, y, z)) continue;
            result.add(zone);
        }
        return result;
    }

    public ArrayList<Zone> getZonesAt(int x, int y, int z) {
        return this.getZonesAt(x, y, z, new ArrayList<Zone>());
    }

    public Zone getZoneAt(int x, int y, int z, String zone) {
        return this.getZonesAt(x, y, z).stream().filter(i -> i != null && Objects.equals(i.type, zone)).findFirst().orElse(null);
    }

    public void getZonesUnique(Set<Zone> result) {
        for (int i = 0; i < this.zonesSize; ++i) {
            Zone zone = this.zones[i];
            result.add(zone);
        }
    }

    public void getZonesIntersecting(int x, int y, int z, int w, int h, ArrayList<Zone> result) {
        for (int i = 0; i < this.zonesSize; ++i) {
            Zone zone = this.zones[i];
            if (result.contains(zone) || !zone.intersects(x, y, z, w, h)) continue;
            result.add(zone);
        }
    }

    public void clearZones() {
        if (this.zones != null) {
            for (int i = 0; i < this.zones.length; ++i) {
                this.zones[i] = null;
            }
        }
        this.zones = null;
        this.zonesSize = 0;
    }

    public void clearRooms() {
        if (this.rooms != null) {
            for (int i = 0; i < this.rooms.length; ++i) {
                this.rooms[i] = null;
            }
        }
        this.rooms = null;
        this.roomsSize = 0;
    }

    public void addRoom(RoomDef room) {
        if (this.rooms == null) {
            this.rooms = new RoomDef[8];
        }
        if (this.roomsSize == this.rooms.length) {
            RoomDef[] newRooms = new RoomDef[this.rooms.length + 8];
            System.arraycopy(this.rooms, 0, newRooms, 0, this.roomsSize);
            this.rooms = newRooms;
        }
        this.rooms[this.roomsSize++] = room;
    }

    public void removeRoom(RoomDef room) {
        if (this.rooms == null) {
            return;
        }
        for (int i = 0; i < this.roomsSize; ++i) {
            if (this.rooms[i] != room) continue;
            while (i < this.roomsSize - 1) {
                this.rooms[i] = this.rooms[i + 1];
                ++i;
            }
            this.rooms[this.roomsSize - 1] = null;
            --this.roomsSize;
            break;
        }
    }

    public RoomDef getRoomAt(int x, int y, int z) {
        RoomDef nonUserDefined = null;
        for (int i = this.roomsSize - 1; i >= 0; --i) {
            RoomDef room = this.rooms[i];
            if (room.isEmptyOutside() || room.level != z || !room.contains(x, y)) continue;
            if (room.userDefined) {
                return room;
            }
            if (nonUserDefined != null) continue;
            nonUserDefined = room;
        }
        return nonUserDefined;
    }

    public RoomDef getEmptyOutsideAt(int x, int y, int z) {
        for (int i = 0; i < this.roomsSize; ++i) {
            RoomDef room = this.rooms[i];
            if (!room.isEmptyOutside() || room.level != z || !room.contains(x, y)) continue;
            return room;
        }
        return null;
    }

    public BuildingDef getAssociatedBuildingAt(int x, int y) {
        for (int i = 0; i < this.roomsSize; ++i) {
            RoomDef room = this.rooms[i];
            if (room.getBuilding().getMaxLevel() < 0) continue;
            if (room.isEmptyOutside()) {
                // empty if block
            }
            for (int n = 0; n < room.rects.size(); ++n) {
                RoomDef.RoomRect rr = room.rects.get(n);
                if (rr.getX() - 1 > x || rr.getY() - 1 > y || x > rr.getX2() || y > rr.getY2()) continue;
                return room.building;
            }
        }
        return null;
    }

    public void getBuildingsIntersecting(int x, int y, int w, int h, ArrayList<BuildingDef> result) {
        for (int i = 0; i < this.roomsSize; ++i) {
            RoomDef roomDef = this.rooms[i];
            if (roomDef.isEmptyOutside() && !roomDef.getBuilding().getRooms().isEmpty() || result.contains(roomDef.building) || !roomDef.intersects(x, y, w, h)) continue;
            result.add(roomDef.building);
        }
    }

    public void getRoomsIntersecting(int x, int y, int w, int h, ArrayList<RoomDef> result) {
        for (int i = 0; i < this.roomsSize; ++i) {
            RoomDef roomDef = this.rooms[i];
            if (roomDef.isEmptyOutside() && !roomDef.getBuilding().getRooms().isEmpty() || result.contains(roomDef) || !roomDef.intersects(x, y, w, h)) continue;
            result.add(roomDef);
        }
    }

    public void Dispose() {
        if (this.rooms != null) {
            Arrays.fill(this.rooms, null);
        }
        if (this.zones != null) {
            Arrays.fill(this.zones, null);
        }
    }
}

