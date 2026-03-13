/*
 * Decompiled with CFR 0.152.
 */
package zombie.audio;

import zombie.UsedFromLua;
import zombie.audio.MusicThreatConfig;

@UsedFromLua
public final class MusicThreatStatus {
    private final String id;
    private float intensity;

    public MusicThreatStatus(String label, float intensity) {
        this.id = label;
        this.intensity = intensity;
    }

    public String getId() {
        return this.id;
    }

    public float getIntensity() {
        if (MusicThreatConfig.getInstance().isStatusIntensityOverridden(this.getId())) {
            return MusicThreatConfig.getInstance().getStatusIntensityOverride(this.getId());
        }
        return this.intensity;
    }

    public void setIntensity(float value) {
        this.intensity = value;
    }
}

