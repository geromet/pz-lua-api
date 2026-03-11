/*
 * Decompiled with CFR 0.152.
 */
package zombie.audio;

import java.util.ArrayList;
import zombie.UsedFromLua;
import zombie.audio.MusicThreatConfig;
import zombie.audio.MusicThreatStatus;
import zombie.characters.IsoPlayer;
import zombie.core.math.PZMath;
import zombie.scripting.objects.MoodleType;
import zombie.util.StringUtils;

@UsedFromLua
public final class MusicThreatStatuses {
    private final IsoPlayer player;
    private final ArrayList<MusicThreatStatus> statuses = new ArrayList();
    private float intensity;

    public MusicThreatStatuses(IsoPlayer player) {
        this.player = player;
    }

    public MusicThreatStatus setStatus(String id, float intensity) {
        MusicThreatStatus event = this.findStatusById(id);
        if (event != null) {
            event.setIntensity(intensity);
            return event;
        }
        event = new MusicThreatStatus(id, intensity);
        this.statuses.add(event);
        return event;
    }

    public void clear() {
        this.statuses.clear();
    }

    public int getStatusCount() {
        return this.statuses.size();
    }

    public MusicThreatStatus getStatusByIndex(int index) {
        return this.statuses.get(index);
    }

    public MusicThreatStatus findStatusById(String id) {
        for (int i = 0; i < this.statuses.size(); ++i) {
            MusicThreatStatus status = this.statuses.get(i);
            if (!StringUtils.equalsIgnoreCase(status.getId(), id)) continue;
            return status;
        }
        return null;
    }

    private float calculateIntensity() {
        float intensity = 0.0f;
        for (int i = 0; i < this.statuses.size(); ++i) {
            MusicThreatStatus status = this.statuses.get(i);
            intensity += status.getIntensity() * MusicThreatConfig.getInstance().getStatusIntensity(status.getId());
        }
        return intensity;
    }

    public void update() {
        this.setStatus("MoodlePanic", (float)this.player.getMoodleLevel(MoodleType.PANIC) / 4.0f);
        this.setStatus("PlayerHealth", 1.0f - this.player.getHealth());
        this.setStatus("ZombiesVisible", PZMath.clamp((float)this.player.getStats().musicZombiesVisible / 100.0f, 0.0f, 1.0f));
        this.setStatus("ZombiesTargeting.DistantNotMoving", PZMath.clamp((float)this.player.getStats().musicZombiesTargetingDistantNotMoving / 100.0f, 0.0f, 1.0f));
        this.setStatus("ZombiesTargeting.NearbyNotMoving", PZMath.clamp((float)this.player.getStats().musicZombiesTargetingNearbyNotMoving / 100.0f, 0.0f, 1.0f));
        this.setStatus("ZombiesTargeting.DistantMoving", PZMath.clamp((float)this.player.getStats().musicZombiesTargetingDistantMoving / 100.0f, 0.0f, 1.0f));
        this.setStatus("ZombiesTargeting.NearbyMoving", PZMath.clamp((float)this.player.getStats().musicZombiesTargetingNearbyMoving / 100.0f, 0.0f, 1.0f));
        for (int i = 0; i < this.statuses.size(); ++i) {
            MusicThreatStatus musicThreatStatus = this.statuses.get(i);
        }
        this.intensity = this.calculateIntensity();
    }

    public float getIntensity() {
        return this.intensity;
    }
}

