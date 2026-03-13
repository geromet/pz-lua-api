/*
 * Decompiled with CFR 0.152.
 */
package zombie.ui;

import zombie.UsedFromLua;

@UsedFromLua
public final class UITransition {
    private float duration = 100.0f;
    private float elapsed;
    private float frac;
    private boolean fadeOut;
    private boolean ignoreUpdateTime;
    private long updateTimeMs;
    private static long currentTimeMS;
    private static long elapsedTimeMS;

    public static void UpdateAll() {
        long current = System.currentTimeMillis();
        elapsedTimeMS = current - currentTimeMS;
        currentTimeMS = current;
    }

    public void init(float duration, boolean fadeOut) {
        this.duration = Math.max(duration, 1.0f);
        this.elapsed = this.frac >= 1.0f ? 0.0f : (this.fadeOut != fadeOut ? (1.0f - this.frac) * this.duration : this.frac * this.duration);
        this.fadeOut = fadeOut;
    }

    public void update() {
        long msDuration;
        if (!this.ignoreUpdateTime && this.updateTimeMs != 0L && this.updateTimeMs + (msDuration = (long)this.duration) < currentTimeMS) {
            this.elapsed = this.duration;
        }
        this.updateTimeMs = currentTimeMS;
        this.frac = this.elapsed / this.duration;
        this.elapsed = Math.min(this.elapsed + (float)elapsedTimeMS, this.duration);
    }

    public float fraction() {
        return this.fadeOut ? 1.0f - this.frac : this.frac;
    }

    public void setFadeIn(boolean fadeIn) {
        if (fadeIn) {
            if (this.fadeOut) {
                this.init(100.0f, false);
            }
        } else if (!this.fadeOut) {
            this.init(200.0f, true);
        }
    }

    public void reset() {
        this.elapsed = 0.0f;
    }

    public void setIgnoreUpdateTime(boolean ignore) {
        this.ignoreUpdateTime = ignore;
    }

    public float getElapsed() {
        return this.elapsed;
    }

    public void setElapsed(float elapsed) {
        this.elapsed = elapsed;
    }
}

