/*
 * Decompiled with CFR 0.152.
 */
package zombie.entity.components.resources;

import zombie.UsedFromLua;

@UsedFromLua
public enum ResourceType {
    Item(1),
    Fluid(2),
    Energy(3),
    Any(0);

    private final byte id;

    private ResourceType(byte id) {
        this.id = id;
    }

    public byte getId() {
        return this.id;
    }

    public static ResourceType fromId(byte id) {
        for (ResourceType val : ResourceType.values()) {
            if (val.id != id) continue;
            return val;
        }
        return null;
    }
}

