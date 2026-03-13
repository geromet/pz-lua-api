/*
 * Decompiled with CFR 0.152.
 */
package zombie.entity.components.resources;

import java.util.HashMap;
import zombie.UsedFromLua;
import zombie.entity.util.enums.IOEnum;

@UsedFromLua
public enum ResourceFlag implements IOEnum
{
    AutoDecay(3, 4),
    Temporary(4, 8);

    private static final HashMap<Byte, ResourceFlag> cache;
    final byte id;
    final int bits;

    private ResourceFlag(byte id, int bits) {
        this.id = id;
        this.bits = bits;
    }

    @Override
    public byte getByteId() {
        return this.id;
    }

    public static ResourceFlag fromByteId(byte id) {
        return cache.get(id);
    }

    @Override
    public int getBits() {
        return this.bits;
    }

    static {
        cache = new HashMap();
        for (ResourceFlag value : ResourceFlag.values()) {
            cache.put(value.id, value);
        }
    }
}

