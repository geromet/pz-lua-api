/*
 * Decompiled with CFR 0.152.
 */
package zombie.iso;

import zombie.UsedFromLua;
import zombie.core.math.PZMath;

@UsedFromLua
public final class Vector2
implements Cloneable {
    public float x;
    public float y;

    public Vector2() {
        this.x = 0.0f;
        this.y = 0.0f;
    }

    public Vector2(Vector2 other) {
        this.x = other.x;
        this.y = other.y;
    }

    public Vector2(float x, float y) {
        this.x = x;
        this.y = y;
    }

    public static Vector2 fromLengthDirection(float length, float direction) {
        Vector2 v = new Vector2();
        v.setLengthAndDirection(direction, length);
        return v;
    }

    public static float dot(float x, float y, float tx, float ty) {
        return x * tx + y * ty;
    }

    public static Vector2 addScaled(Vector2 a, Vector2 b, float scale, Vector2 result) {
        result.set(a.x + b.x * scale, a.y + b.y * scale);
        return result;
    }

    public void rotate(float radians) {
        double rx = (double)this.x * Math.cos(radians) - (double)this.y * Math.sin(radians);
        double ry = (double)this.x * Math.sin(radians) + (double)this.y * Math.cos(radians);
        this.x = (float)rx;
        this.y = (float)ry;
    }

    public Vector2 add(Vector2 other) {
        this.x += other.x;
        this.y += other.y;
        return this;
    }

    public Vector2 aimAt(Vector2 other) {
        this.setLengthAndDirection(this.angleTo(other), this.getLength());
        return this;
    }

    public float angleTo(Vector2 other) {
        return (float)Math.atan2(other.y - this.y, other.x - this.x);
    }

    public float angleBetween(Vector2 other) {
        float dot = this.dot(other) / (this.getLength() * other.getLength());
        if (dot < -1.0f) {
            dot = -1.0f;
        }
        if (dot > 1.0f) {
            dot = 1.0f;
        }
        return (float)Math.acos(dot);
    }

    public Vector2 clone() {
        return new Vector2(this);
    }

    public float distanceTo(Vector2 other) {
        return (float)Math.sqrt(Math.pow(other.x - this.x, 2.0) + Math.pow(other.y - this.y, 2.0));
    }

    public float dot(Vector2 other) {
        return this.x * other.x + this.y * other.y;
    }

    public float dot(float otherX, float otherY) {
        return this.x * otherX + this.y * otherY;
    }

    public boolean equals(Object other) {
        if (other instanceof Vector2) {
            Vector2 v = (Vector2)other;
            return v.x == this.x && v.y == this.y;
        }
        return false;
    }

    public float getDirection() {
        return Vector2.getDirection(this.x, this.y);
    }

    public static float getDirection(float x, float y) {
        float angle = (float)Math.atan2(y, x);
        return PZMath.wrap(angle, (float)(-Math.PI), (float)Math.PI);
    }

    @Deprecated
    public float getDirectionNeg() {
        return (float)Math.atan2(this.x, this.y);
    }

    public Vector2 setDirection(float directionRadians) {
        this.setLengthAndDirection(directionRadians, this.getLength());
        return this;
    }

    public float getLength() {
        return (float)Math.sqrt(this.x * this.x + this.y * this.y);
    }

    public float getLengthSquared() {
        return this.x * this.x + this.y * this.y;
    }

    public Vector2 setLength(float length) {
        this.normalize();
        this.x *= length;
        this.y *= length;
        return this;
    }

    public float normalize() {
        float lengthSq = this.getLengthSquared();
        if (PZMath.equal(lengthSq, 1.0f, 1.0E-5f)) {
            return 1.0f;
        }
        if (lengthSq == 0.0f) {
            this.x = 0.0f;
            this.y = 0.0f;
            return 0.0f;
        }
        float length = (float)Math.sqrt(lengthSq);
        this.x /= length;
        this.y /= length;
        return length;
    }

    public Vector2 set(Vector2 other) {
        this.x = other.x;
        this.y = other.y;
        return this;
    }

    public Vector2 set(float x, float y) {
        this.x = x;
        this.y = y;
        return this;
    }

    public Vector2 setLengthAndDirection(float direction, float length) {
        this.x = (float)(Math.cos(direction) * (double)length);
        this.y = (float)(Math.sin(direction) * (double)length);
        return this;
    }

    public String toString() {
        return String.format("Vector2 (X: %f, Y: %f) (L: %f, D:%f)", Float.valueOf(this.x), Float.valueOf(this.y), Float.valueOf(this.getLength()), Float.valueOf(this.getDirection()));
    }

    public float getX() {
        return this.x;
    }

    public void setX(float x) {
        this.x = x;
    }

    public float getY() {
        return this.y;
    }

    public void setY(float y) {
        this.y = y;
    }

    public int floorX() {
        return PZMath.fastfloor(this.getX());
    }

    public int floorY() {
        return PZMath.fastfloor(this.getY());
    }

    public void tangent() {
        double nx = (double)this.x * Math.cos(Math.toRadians(90.0)) - (double)this.y * Math.sin(Math.toRadians(90.0));
        double ny = (double)this.x * Math.sin(Math.toRadians(90.0)) + (double)this.y * Math.cos(Math.toRadians(90.0));
        this.x = (float)nx;
        this.y = (float)ny;
    }

    public void scale(float scale) {
        Vector2.scale(this, scale);
    }

    public static Vector2 scale(Vector2 val, float scale) {
        val.x *= scale;
        val.y *= scale;
        return val;
    }

    public static Vector2 moveTowards(Vector2 currentVector, Vector2 targetVector, float maxDistanceDelta) {
        float toVectorX = targetVector.x - currentVector.x;
        float toVectorY = targetVector.y - currentVector.y;
        float sqDistance = toVectorX * toVectorX + toVectorY * toVectorY;
        if (sqDistance == 0.0f || maxDistanceDelta >= 0.0f && sqDistance <= maxDistanceDelta * maxDistanceDelta) {
            return targetVector;
        }
        float distance = (float)Math.sqrt(sqDistance);
        return new Vector2(currentVector.x + toVectorX / distance * maxDistanceDelta, currentVector.y + toVectorY / distance * maxDistanceDelta);
    }
}

