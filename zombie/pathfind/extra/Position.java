/*
 * Decompiled with CFR 0.152.
 */
package zombie.pathfind.extra;

import java.util.EnumMap;
import zombie.iso.worldgen.utils.SquareCoord;
import zombie.pathfind.extra.BorderStatus;
import zombie.pathfind.extra.Direction;

public record Position(SquareCoord coords, Direction direction, int distance, int tick, EnumMap<Direction, BorderStatus> walls) {
    public int x() {
        return this.coords.x();
    }

    public int y() {
        return this.coords.y();
    }

    public int z() {
        return this.coords.z();
    }

    public int manhattan(SquareCoord coords) {
        return Math.abs(this.coords.x() - coords.x()) + Math.abs(this.coords.y() - coords.y());
    }

    public BorderStatus isWall(Direction direction) {
        return this.walls.get((Object)direction);
    }
}

