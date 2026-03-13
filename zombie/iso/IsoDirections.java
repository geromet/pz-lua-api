/*
 * Decompiled with CFR 0.152.
 */
package zombie.iso;

import zombie.UsedFromLua;
import zombie.core.math.PZMath;
import zombie.core.random.Rand;
import zombie.iso.Vector2;

@UsedFromLua
public enum IsoDirections {
    N(0, -1),
    NW(-1, -1),
    W(-1, 0),
    SW(-1, 1),
    S(0, 1),
    SE(1, 1),
    E(1, 0),
    NE(1, -1);

    private static final IsoDirections[] VALUES;
    private static final Vector2 TEMP;
    private final int dx;
    private final int dy;
    private final float angle;
    private final Vector2 vector;

    private IsoDirections(int dx, int dy) {
        this.dx = dx;
        this.dy = dy;
        this.vector = new Vector2(dx, dy);
        this.vector.normalize();
        this.angle = (float)this.ordinal() * ((float)Math.PI * 2) / 8.0f;
    }

    public static IsoDirections fromString(String str) {
        return IsoDirections.valueOf(str.trim().toUpperCase());
    }

    public static IsoDirections fromIndex(int index) {
        return VALUES[index & 7];
    }

    public IsoDirections RotLeft() {
        return VALUES[this.ordinal() + 1 & 7];
    }

    public IsoDirections RotLeft(int times) {
        return IsoDirections.fromIndex(this.ordinal() + times);
    }

    public IsoDirections RotRight() {
        return VALUES[this.ordinal() - 1 & 7];
    }

    public IsoDirections RotRight(int times) {
        return IsoDirections.fromIndex(this.ordinal() - times);
    }

    public IsoDirections Rot180() {
        return VALUES[this.ordinal() + 4 & 7];
    }

    public static IsoDirections fromAngle(Vector2 v) {
        return IsoDirections.fromAngle(v.x, v.y);
    }

    public static IsoDirections fromAngle(float dx, float dy) {
        return IsoDirections.safeFromAngle((float)Math.atan2(dy, dx));
    }

    public static IsoDirections fromAngle(float angleRadians) {
        return IsoDirections.safeFromAngle(PZMath.wrap(angleRadians, (float)(-Math.PI), (float)Math.PI));
    }

    private static IsoDirections safeFromAngle(float preClampedAngleRadians) {
        return VALUES[6 - (int)(8.5f + 1.2732395f * preClampedAngleRadians) & 7];
    }

    public static IsoDirections cardinalFromAngle(Vector2 v) {
        return IsoDirections.cardinalFromAngle(v.x, v.y);
    }

    public static IsoDirections cardinalFromAngle(float dx, float dy) {
        return IsoDirections.safeCardinalFromAngle((float)Math.atan2(dy, dx));
    }

    public static IsoDirections cardinalFromAngle(float angleRadians) {
        return IsoDirections.safeCardinalFromAngle(PZMath.wrap(angleRadians, (float)(-Math.PI), (float)Math.PI));
    }

    private static IsoDirections safeCardinalFromAngle(float preClampedAngleRadians) {
        return VALUES[2 * (3 - (int)(4.5f + 0.63661975f * preClampedAngleRadians) & 3)];
    }

    public int dx() {
        return this.dx;
    }

    public int dy() {
        return this.dy;
    }

    public Vector2 ToVector() {
        return this.ToVector(TEMP);
    }

    public Vector2 ToVector(Vector2 result) {
        result.set(this.vector);
        return result;
    }

    public float toAngle() {
        return this.angle;
    }

    public float toAngleDegrees() {
        return this.angle * 57.295776f;
    }

    public static IsoDirections getRandom() {
        return VALUES[Rand.Next(8)];
    }

    static {
        VALUES = IsoDirections.values();
        TEMP = new Vector2();
    }
}

