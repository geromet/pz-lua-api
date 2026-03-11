/*
 * Decompiled with CFR 0.152.
 */
package zombie.entity;

import java.util.HashMap;
import zombie.UsedFromLua;
import zombie.entity.util.enums.IOEnum;

@UsedFromLua
public enum GameEntityType implements IOEnum
{
    IsoObject(1, 1),
    InventoryItem(2, 2),
    VehiclePart(3, 4),
    IsoMovingObject(4, 8),
    Template(5, 16),
    MetaEntity(5, 32);

    private static final HashMap<Byte, GameEntityType> map;
    private final byte id;
    private final int bits;

    private GameEntityType(byte id, int bits) {
        this.id = id;
        this.bits = bits;
    }

    public byte getId() {
        return this.id;
    }

    public static GameEntityType FromID(byte id) {
        return map.get(id);
    }

    @Override
    public byte getByteId() {
        return this.id;
    }

    @Override
    public int getBits() {
        return this.bits;
    }

    static {
        map = new HashMap();
        for (GameEntityType type : GameEntityType.values()) {
            map.put(type.id, type);
        }
    }
}

