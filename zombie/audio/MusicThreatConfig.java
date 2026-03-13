/*
 * Decompiled with CFR 0.152.
 */
package zombie.audio;

import java.util.ArrayList;
import java.util.HashMap;
import se.krka.kahlua.j2se.KahluaTableImpl;
import se.krka.kahlua.vm.KahluaTableIterator;
import zombie.UsedFromLua;
import zombie.core.math.PZMath;
import zombie.util.StringUtils;

@UsedFromLua
public final class MusicThreatConfig {
    private static MusicThreatConfig instance;
    private final ArrayList<Status> statusList = new ArrayList();
    private final HashMap<String, Status> statusById = new HashMap();

    public static MusicThreatConfig getInstance() {
        if (instance == null) {
            instance = new MusicThreatConfig();
        }
        return instance;
    }

    public void initStatuses(KahluaTableImpl statusesTable) {
        this.statusList.clear();
        this.statusById.clear();
        KahluaTableIterator it = statusesTable.iterator();
        while (it.advance()) {
            String key = it.getKey().toString();
            if ("VERSION".equalsIgnoreCase(key)) continue;
            KahluaTableImpl statusTable = (KahluaTableImpl)it.getValue();
            Status status = new Status();
            status.id = StringUtils.discardNullOrWhitespace(statusTable.rawgetStr("id"));
            status.intensity = statusTable.rawgetFloat("intensity");
            if (status.id == null || status.intensity <= 0.0f) continue;
            if (this.statusById.containsKey(status.id)) {
                this.statusList.remove(this.statusById.get(status.id));
            }
            this.statusList.add(status);
            this.statusById.put(status.id, status);
        }
    }

    public int getStatusCount() {
        return this.statusList.size();
    }

    public String getStatusIdByIndex(int index) {
        return this.statusList.get((int)index).id;
    }

    public float getStatusIntensityByIndex(int index) {
        return this.statusList.get((int)index).intensity;
    }

    public float getStatusIntensity(String id) {
        Status status = this.statusById.get(id);
        if (status == null) {
            return 0.0f;
        }
        return status.intensity;
    }

    public void setStatusIntensityOverride(String id, float intensity) {
        Status status = this.statusById.get(id);
        if (status == null) {
            return;
        }
        status.intensityOverride = intensity < 0.0f ? Float.NaN : PZMath.clamp(intensity, 0.0f, 1.0f);
    }

    public float getStatusIntensityOverride(String id) {
        Status status = this.statusById.get(id);
        if (status == null) {
            return 0.0f;
        }
        return status.intensityOverride;
    }

    public boolean isStatusIntensityOverridden(String id) {
        Status status = this.statusById.get(id);
        if (status == null) {
            return false;
        }
        return !Float.isNaN(status.intensityOverride);
    }

    private static final class Status {
        String id;
        float intensity;
        float intensityOverride = Float.NaN;

        private Status() {
        }
    }
}

