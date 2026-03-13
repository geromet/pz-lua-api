/*
 * Decompiled with CFR 0.152.
 */
package zombie.entity.energy;

import java.util.HashMap;
import java.util.HashSet;
import zombie.UsedFromLua;
import zombie.core.Core;

@UsedFromLua
public enum EnergyType {
    None(-1),
    Electric(1),
    Mechanical(2),
    Thermal(3),
    Steam(4),
    VoidEnergy(126),
    Modded(127);

    private static final HashSet<String> energyNames;
    private static final HashMap<Byte, EnergyType> energyIdMap;
    private static final HashMap<String, EnergyType> energyNameMap;
    private final byte id;
    private String lowerCache;

    private EnergyType(byte typeID) {
        this.id = typeID;
    }

    public byte getId() {
        return this.id;
    }

    public String toStringLower() {
        if (this.lowerCache != null) {
            return this.lowerCache;
        }
        this.lowerCache = this.toString().toLowerCase();
        return this.lowerCache;
    }

    public static boolean containsNameLowercase(String name) {
        return energyNames.contains(name.toLowerCase());
    }

    public static EnergyType FromId(byte id) {
        return energyIdMap.get(id);
    }

    public static EnergyType FromNameLower(String name) {
        return energyNameMap.get(name.toLowerCase());
    }

    static {
        energyNames = new HashSet();
        energyIdMap = new HashMap();
        energyNameMap = new HashMap();
        for (EnergyType type : EnergyType.values()) {
            if (Core.debug && energyIdMap.containsKey(type.id)) {
                throw new IllegalStateException("ID duplicate in EnergyType");
            }
            energyNames.add(type.toString().toLowerCase());
            energyNameMap.put(type.toString().toLowerCase(), type);
            energyIdMap.put(type.id, type);
        }
    }
}

