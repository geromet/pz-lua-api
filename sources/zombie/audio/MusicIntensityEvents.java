/*
 * Decompiled with CFR 0.152.
 */
package zombie.audio;

import java.util.ArrayList;
import zombie.UsedFromLua;
import zombie.audio.MusicIntensityEvent;
import zombie.core.math.PZMath;
import zombie.util.StringUtils;

@UsedFromLua
public final class MusicIntensityEvents {
    private final ArrayList<MusicIntensityEvent> events = new ArrayList();
    private long updateTimeMs = -1L;
    private float intensity;

    public MusicIntensityEvent addEvent(String id, float intensity, long durationMS, boolean bMultiple) {
        MusicIntensityEvent event;
        if (!bMultiple && (event = this.findEventById(id)) != null) {
            event.setElapsedTime(0L);
            return event;
        }
        event = new MusicIntensityEvent(id, intensity, durationMS);
        this.events.add(event);
        return event;
    }

    public void clear() {
        this.events.clear();
    }

    public int getEventCount() {
        return this.events.size();
    }

    public MusicIntensityEvent getEventByIndex(int index) {
        return this.events.get(index);
    }

    public MusicIntensityEvent findEventById(String id) {
        for (int i = 0; i < this.events.size(); ++i) {
            MusicIntensityEvent event = this.events.get(i);
            if (!StringUtils.equalsIgnoreCase(event.getId(), id)) continue;
            return event;
        }
        return null;
    }

    private float calculateIntensity() {
        float intensity = 50.0f;
        for (int i = 0; i < this.events.size(); ++i) {
            MusicIntensityEvent event = this.events.get(i);
            intensity += event.getIntensity();
        }
        return PZMath.clamp(intensity, 0.0f, 100.0f);
    }

    public void update() {
        long currentTimeMS = System.currentTimeMillis();
        long elapsedTimeMS = currentTimeMS - this.updateTimeMs;
        this.updateTimeMs = currentTimeMS;
        for (int i = 0; i < this.events.size(); ++i) {
            MusicIntensityEvent event = this.events.get(i);
            if (event.getDuration() <= 0L && event.getElapsedTime() > 0L) {
                this.events.remove(i--);
                continue;
            }
            event.setElapsedTime(event.getElapsedTime() + elapsedTimeMS);
            if (event.getDuration() <= 0L || event.getElapsedTime() < event.getDuration()) continue;
            this.events.remove(i--);
        }
        this.intensity = this.calculateIntensity();
    }

    public float getIntensity() {
        return this.intensity;
    }
}

