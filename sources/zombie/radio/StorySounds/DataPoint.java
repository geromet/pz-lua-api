/*
 * Decompiled with CFR 0.152.
 */
package zombie.radio.StorySounds;

import zombie.UsedFromLua;

@UsedFromLua
public final class DataPoint {
    protected float time;
    protected float intensity;

    public DataPoint(float time, float intensity) {
        this.setTime(time);
        this.setIntensity(intensity);
    }

    public float getTime() {
        return this.time;
    }

    public void setTime(float time) {
        if (time < 0.0f) {
            time = 0.0f;
        }
        if (time > 1.0f) {
            time = 1.0f;
        }
        this.time = time;
    }

    public float getIntensity() {
        return this.intensity;
    }

    public void setIntensity(float intensity) {
        if (intensity < 0.0f) {
            intensity = 0.0f;
        }
        if (intensity > 1.0f) {
            intensity = 1.0f;
        }
        this.intensity = intensity;
    }
}

