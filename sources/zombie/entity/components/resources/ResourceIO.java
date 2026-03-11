/*
 * Decompiled with CFR 0.152.
 */
package zombie.entity.components.resources;

import zombie.UsedFromLua;

@UsedFromLua
public enum ResourceIO {
    Input(1),
    Output(2),
    Any(3);

    private final byte id;

    private ResourceIO(byte id) {
        this.id = id;
    }

    public byte getId() {
        return this.id;
    }

    public static ResourceIO fromId(byte id) {
        for (ResourceIO val : ResourceIO.values()) {
            if (val.id != id) continue;
            return val;
        }
        return null;
    }
}

