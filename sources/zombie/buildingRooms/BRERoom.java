/*
 * Decompiled with CFR 0.152.
 */
package zombie.buildingRooms;

import zombie.UsedFromLua;
import zombie.buildingRooms.BREBuilding;
import zombie.buildingRooms.BuildingRoomsEditor;
import zombie.iso.RoomDef;

@UsedFromLua
public final class BRERoom {
    final BREBuilding building;
    final RoomDef roomDef = new RoomDef(0L, "");

    BRERoom(BREBuilding building) {
        this.building = building;
        this.roomDef.explored = true;
        this.roomDef.doneSpawn = true;
    }

    public int getLevel() {
        return this.roomDef.level;
    }

    public String getName() {
        return this.roomDef.getName();
    }

    public void setName(String name) {
        this.roomDef.setName(name);
    }

    public void addRectangle(int x, int y, int w, int h) {
        if (w < 1 || h < 1) {
            return;
        }
        RoomDef.RoomRect rect = new RoomDef.RoomRect(x, y, w, h);
        this.roomDef.rects.add(rect);
        BuildingRoomsEditor.getInstance().callLua("AfterAddRectangle", this.building, this);
    }

    public void removeRectangle(int index) {
        this.roomDef.rects.remove(index);
        BuildingRoomsEditor.getInstance().callLua("AfterRemoveRectangle", this.building, this, index);
    }

    public int getRectangleCount() {
        return this.roomDef.rects.size();
    }

    public RoomDef.RoomRect getRectangle(int index) {
        return this.roomDef.rects.get(index);
    }

    public boolean contains(int x, int y, int z) {
        if (z != this.getLevel()) {
            return false;
        }
        return this.roomDef.intersects(x, y, 1, 1);
    }

    public int hitTest(int squareX, int squareY) {
        for (int i = 0; i < this.roomDef.rects.size(); ++i) {
            RoomDef.RoomRect rect = this.roomDef.rects.get(i);
            if (!rect.contains(squareX, squareY)) continue;
            return i;
        }
        return -1;
    }

    public boolean intersects(int x, int y, int w, int h) {
        return this.roomDef.intersects(x, y, w, h);
    }

    public boolean isAdjacent(int x, int y, int w, int h) {
        return this.roomDef.isAdjacent(x, y, w, h);
    }

    public BRERoom copyFrom(RoomDef roomDef2) {
        this.roomDef.copyFrom(roomDef2);
        return this;
    }

    public boolean isValid() {
        return !this.roomDef.getRects().isEmpty();
    }
}

