/*
 * Decompiled with CFR 0.152.
 */
package zombie.entity.components.resources;

import zombie.UsedFromLua;
import zombie.core.Color;
import zombie.core.Colors;
import zombie.entity.util.enums.EnumBitStore;
import zombie.entity.util.enums.IOEnum;

@UsedFromLua
public enum ResourceChannel implements IOEnum
{
    NO_CHANNEL(0, 0, Colors.Black),
    Channel_Red(1, 1, Colors.Crimson),
    Channel_Yellow(2, 2, Colors.Gold),
    Channel_Blue(3, 4, Colors.DodgerBlue),
    Channel_Orange(4, 8, Colors.Orange),
    Channel_Green(5, 16, Colors.LimeGreen),
    Channel_Purple(6, 32, Colors.MediumSlateBlue),
    Channel_Cyan(7, 64, Colors.Cyan),
    Channel_Magenta(8, 128, Colors.Magenta);

    public static final EnumBitStore<ResourceChannel> BitStoreAll;
    private final byte id;
    private final int bits;
    private final Color color;

    private ResourceChannel(byte id, int bits, Color color) {
        this.id = id;
        this.bits = bits;
        this.color = color;
    }

    @Override
    public byte getByteId() {
        return this.id;
    }

    @Override
    public int getBits() {
        return this.bits;
    }

    public Color getColor() {
        return this.color;
    }

    public static ResourceChannel fromId(byte id) {
        for (ResourceChannel val : ResourceChannel.values()) {
            if (val.id != id) continue;
            return val;
        }
        return null;
    }

    static {
        BitStoreAll = EnumBitStore.allOf(ResourceChannel.class);
    }
}

