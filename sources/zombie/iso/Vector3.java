/*
 * Decompiled with CFR 0.152.
 */
package zombie.iso;

import zombie.UsedFromLua;
import zombie.iso.Vector2;

@UsedFromLua
public final class Vector3
implements Cloneable {
    public float x;
    public float y;
    public float z;

    public Vector3() {
        this.x = 0.0f;
        this.y = 0.0f;
        this.z = 0.0f;
    }

    public Vector3(Vector3 other) {
        this.x = other.x;
        this.y = other.y;
        this.z = other.z;
    }

    public Vector3(float x, float y, float z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public static Vector2 fromLengthDirection(float length, float direction) {
        Vector2 v = new Vector2();
        v.setLengthAndDirection(direction, length);
        return v;
    }

    public static float dot(float x, float y, float tx, float ty) {
        return x * tx + y * ty;
    }

    public void rotate(float rad) {
        double rx = (double)this.x * Math.cos(rad) - (double)this.y * Math.sin(rad);
        double ry = (double)this.x * Math.sin(rad) + (double)this.y * Math.cos(rad);
        this.x = (float)rx;
        this.y = (float)ry;
    }

    public void rotatey(float rad) {
        double rx = (double)this.x * Math.cos(rad) - (double)this.z * Math.sin(rad);
        double ry = (double)this.x * Math.sin(rad) + (double)this.z * Math.cos(rad);
        this.x = (float)rx;
        this.z = (float)ry;
    }

    public Vector2 add(Vector2 other) {
        return new Vector2(this.x + other.x, this.y + other.y);
    }

    public Vector3 addToThis(Vector2 other) {
        this.x += other.x;
        this.y += other.y;
        return this;
    }

    public Vector3 addToThis(Vector3 other) {
        this.x += other.x;
        this.y += other.y;
        this.z += other.z;
        return this;
    }

    public Vector3 div(float scalar) {
        this.x /= scalar;
        this.y /= scalar;
        this.z /= scalar;
        return this;
    }

    public Vector3 aimAt(Vector2 other) {
        this.setLengthAndDirection(this.angleTo(other), this.getLength());
        return this;
    }

    public float angleTo(Vector2 other) {
        return (float)Math.atan2(other.y - this.y, other.x - this.x);
    }

    public Vector3 clone() {
        return new Vector3(this);
    }

    public float distanceTo(Vector2 other) {
        return (float)Math.sqrt(Math.pow(other.x - this.x, 2.0) + Math.pow(other.y - this.y, 2.0));
    }

    public float distanceTo(Vector3 other) {
        return (float)Math.sqrt(Math.pow(other.x - this.x, 2.0) + Math.pow(other.y - this.y, 2.0) + Math.pow(other.z - this.z, 2.0));
    }

    public float distanceTo(float x, float y, float z) {
        return (float)Math.sqrt(Math.pow(x - this.x, 2.0) + Math.pow(y - this.y, 2.0) + Math.pow(z - this.z, 2.0));
    }

    public float dot(Vector2 other) {
        return this.x * other.x + this.y * other.y;
    }

    public float dot3d(Vector3 other) {
        return this.x * other.x + this.y * other.y + this.z * other.z;
    }

    public boolean equals(Object other) {
        if (other instanceof Vector3) {
            Vector3 v = (Vector3)other;
            return v.x == this.x && v.y == this.y && v.z == this.z;
        }
        return false;
    }

    public float getDirection() {
        return (float)Math.atan2(this.x, this.y);
    }

    public Vector3 setDirection(float direction) {
        this.setLengthAndDirection(direction, this.getLength());
        return this;
    }

    public float getLength() {
        float lengthSq = this.getLengthSq();
        return (float)Math.sqrt(lengthSq);
    }

    public float getLengthSq() {
        return this.x * this.x + this.y * this.y + this.z * this.z;
    }

    public Vector3 setLength(float length) {
        this.normalize();
        this.x *= length;
        this.y *= length;
        this.z *= length;
        return this;
    }

    public void normalize() {
        float length = this.getLength();
        if (length == 0.0f) {
            this.x = 0.0f;
            this.y = 0.0f;
            this.z = 0.0f;
        } else {
            this.x /= length;
            this.y /= length;
            this.z /= length;
        }
    }

    public Vector3 set(Vector3 other) {
        this.x = other.x;
        this.y = other.y;
        this.z = other.z;
        return this;
    }

    public Vector3 set(float x, float y, float z) {
        this.x = x;
        this.y = y;
        this.z = z;
        return this;
    }

    public Vector3 setLengthAndDirection(float direction, float length) {
        this.x = (float)(Math.cos(direction) * (double)length);
        this.y = (float)(Math.sin(direction) * (double)length);
        return this;
    }

    public String toString() {
        return String.format("Vector2 (X: %f, Y: %f) (L: %f, D:%f)", Float.valueOf(this.x), Float.valueOf(this.y), Float.valueOf(this.getLength()), Float.valueOf(this.getDirection()));
    }

    public Vector3 sub(Vector3 val, Vector3 out) {
        return Vector3.sub(this, val, out);
    }

    public static Vector3 sub(Vector3 a, Vector3 b, Vector3 out) {
        out.set(a.x - b.x, a.y - b.y, a.z - b.z);
        return out;
    }
}

