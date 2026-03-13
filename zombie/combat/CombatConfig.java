/*
 * Decompiled with CFR 0.152.
 */
package zombie.combat;

import java.util.EnumMap;
import java.util.Map;
import zombie.UsedFromLua;
import zombie.combat.CombatConfigCategory;
import zombie.combat.CombatConfigKey;

@UsedFromLua
public class CombatConfig {
    private final EnumMap<CombatConfigKey, Float> values = new EnumMap(CombatConfigKey.class);

    public CombatConfig() {
        for (CombatConfigKey combatConfigKey : CombatConfigKey.values()) {
            this.values.put(combatConfigKey, Float.valueOf(combatConfigKey.getDefaultValue()));
        }
    }

    public float get(CombatConfigKey combatConfigKey) {
        return this.values.get((Object)combatConfigKey).floatValue();
    }

    public void set(CombatConfigKey combatConfigKey, float value) {
        value = Math.max(combatConfigKey.getMinimum(), Math.min(combatConfigKey.getMaximum(), value));
        this.values.put(combatConfigKey, Float.valueOf(value));
    }

    public Map<CombatConfigKey, Float> getByCategory(CombatConfigCategory combatConfigCategory) {
        EnumMap<CombatConfigKey, Float> map = new EnumMap<CombatConfigKey, Float>(CombatConfigKey.class);
        for (Map.Entry<CombatConfigKey, Float> entry : this.values.entrySet()) {
            if (!entry.getKey().getCategory().equals((Object)combatConfigCategory)) continue;
            map.put(entry.getKey(), entry.getValue());
        }
        return map;
    }
}

