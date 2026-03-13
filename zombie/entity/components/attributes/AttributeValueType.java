/*
 * Decompiled with CFR 0.152.
 */
package zombie.entity.components.attributes;

import java.util.EnumSet;
import zombie.UsedFromLua;

@UsedFromLua
public enum AttributeValueType {
    Boolean(0),
    String(1),
    Float(2),
    Double(3),
    Byte(4),
    Short(5),
    Int(6),
    Long(7),
    Enum(8),
    EnumSet(9),
    EnumStringSet(10);

    private static final EnumSet<AttributeValueType> numerics;
    private static final EnumSet<AttributeValueType> decimals;
    private final byte byteIndex;

    private AttributeValueType(byte index) {
        this.byteIndex = index;
    }

    public int getByteIndex() {
        return this.byteIndex;
    }

    public static boolean IsNumeric(AttributeValueType valueType) {
        return numerics.contains((Object)valueType);
    }

    public static boolean IsDecimal(AttributeValueType valueType) {
        return decimals.contains((Object)valueType);
    }

    public static AttributeValueType fromByteIndex(int value) {
        return ((AttributeValueType[])AttributeValueType.class.getEnumConstants())[value];
    }

    public static AttributeValueType valueOfIgnoreCase(String s) {
        if (s == null) {
            return null;
        }
        for (AttributeValueType type : AttributeValueType.values()) {
            if (!type.name().equalsIgnoreCase(s)) continue;
            return type;
        }
        return null;
    }

    static {
        numerics = java.util.EnumSet.of(Float, new AttributeValueType[]{Double, Byte, Short, Int, Long});
        decimals = java.util.EnumSet.of(Float, Double);
    }
}

