/*
 * Decompiled with CFR 0.152.
 */
package zombie.entity.components.crafting;

import java.util.HashMap;
import zombie.UsedFromLua;
import zombie.entity.util.enums.IOEnum;

@UsedFromLua
public enum StartMode implements IOEnum
{
    Manual(1, 1),
    Automatic(2, 2),
    Passive(3, 4);

    private static final HashMap<Byte, StartMode> cache;
    final byte id;
    final int bits;

    private StartMode(byte id, int bits) {
        this.id = id;
        this.bits = bits;
    }

    @Override
    public byte getByteId() {
        return this.id;
    }

    public static StartMode fromByteId(byte id) {
        return cache.get(id);
    }

    @Override
    public int getBits() {
        return this.bits;
    }

    static {
        cache = new HashMap();
        for (StartMode value : StartMode.values()) {
            cache.put(value.id, value);
        }
    }
}

