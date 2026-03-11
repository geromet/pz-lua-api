/*
 * Decompiled with CFR 0.152.
 */
package zombie.entity.components.attributes;

import java.nio.ByteBuffer;
import zombie.GameWindow;
import zombie.UsedFromLua;
import zombie.debug.DebugLog;
import zombie.entity.components.attributes.Attribute;
import zombie.entity.components.attributes.AttributeFactory;
import zombie.entity.components.attributes.AttributeType;
import zombie.entity.components.attributes.AttributeUtil;
import zombie.entity.components.attributes.AttributeValueType;
import zombie.entity.components.attributes.EnumStringObj;
import zombie.entity.util.enums.IOEnum;

@UsedFromLua
public abstract class AttributeInstance<C extends AttributeInstance<C, T>, T extends AttributeType> {
    protected T type;

    protected AttributeInstance() {
    }

    protected abstract void setType(T var1);

    public final T getType() {
        return this.type;
    }

    public final AttributeValueType getValueType() {
        return ((AttributeType)this.type).getValueType();
    }

    public final java.lang.String getNameUI() {
        return ((AttributeType)this.type).getNameUI();
    }

    public final boolean isHiddenUI() {
        return ((AttributeType)this.type).isHiddenUI();
    }

    public boolean isRequiresValidation() {
        return false;
    }

    public final boolean isReadOnly() {
        return ((AttributeType)this.type).isReadOnly();
    }

    protected boolean canSetValue() {
        if (this.isReadOnly()) {
            DebugLog.General.error("Trying to set value on a read-only attribute [" + java.lang.String.valueOf(this) + "]");
            return false;
        }
        return true;
    }

    public abstract java.lang.String stringValue();

    public abstract boolean setValueFromScriptString(java.lang.String var1);

    public abstract boolean equalTo(C var1);

    public abstract C copy();

    public boolean isDisplayAsBar() {
        return false;
    }

    public float getDisplayAsBarUnit() {
        return 0.0f;
    }

    public float getFloatValue() {
        return 0.0f;
    }

    public int getIntValue() {
        return 0;
    }

    protected void reset() {
        this.type = null;
    }

    protected abstract void release();

    public abstract void save(ByteBuffer var1);

    public abstract void load(ByteBuffer var1);

    public java.lang.String toString() {
        return "Attribute." + java.lang.String.valueOf(this.type != null ? this.type : "NOT_SET") + " [value = " + this.stringValue() + ", valueType = " + java.lang.String.valueOf(this.type != null ? ((AttributeType)this.type).getValueType() : "NOT_SET") + ", hidden = " + this.isHiddenUI() + ", req_val = " + this.isRequiresValidation() + ", read-only = " + this.isReadOnly() + "]";
    }

    @UsedFromLua
    public static class Long
    extends Numeric<Long, AttributeType.Long> {
        private long value;

        @Override
        protected void setType(AttributeType.Long type) {
            this.type = type;
            this.value = (java.lang.Long)type.getInitialValue();
        }

        public long getValue() {
            return this.value;
        }

        public void setValue(long value) {
            if (this.canSetValue()) {
                this.value = ((AttributeType.Long)this.type).validate(value);
            }
        }

        @Override
        public float floatValue() {
            return this.value;
        }

        @Override
        public void fromFloat(float f) {
            this.setValue((long)f);
        }

        @Override
        public java.lang.String stringValue() {
            return java.lang.Long.toString(this.value);
        }

        @Override
        public boolean setValueFromScriptString(java.lang.String val) {
            try {
                this.value = ((AttributeType.Long)this.type).validate(java.lang.Long.parseLong(val));
                return true;
            }
            catch (Exception e) {
                e.printStackTrace();
                this.value = (java.lang.Long)((AttributeType.Long)this.type).getInitialValue();
                return false;
            }
        }

        @Override
        public boolean equalTo(Long other) {
            if (this.type == other.type) {
                return this.value == other.value;
            }
            return true;
        }

        @Override
        public Long copy() {
            Long copy = AttributeFactory.AllocAttributeLong();
            copy.setType((AttributeType.Long)this.type);
            copy.value = this.value;
            return copy;
        }

        @Override
        protected void release() {
            AttributeFactory.Release(this);
        }

        @Override
        public void save(ByteBuffer output) {
            output.putLong(this.value);
        }

        @Override
        public void load(ByteBuffer input) {
            this.value = input.getLong();
        }
    }

    @UsedFromLua
    public static class Int
    extends Numeric<Int, AttributeType.Int> {
        private int value;

        @Override
        protected void setType(AttributeType.Int type) {
            this.type = type;
            this.value = (Integer)type.getInitialValue();
        }

        public int getValue() {
            return this.value;
        }

        public void setValue(int value) {
            if (this.canSetValue()) {
                this.value = ((AttributeType.Int)this.type).validate(value);
            }
        }

        @Override
        public float floatValue() {
            return this.value;
        }

        @Override
        public void fromFloat(float f) {
            this.setValue((int)f);
        }

        @Override
        public java.lang.String stringValue() {
            return Integer.toString(this.value);
        }

        @Override
        public boolean setValueFromScriptString(java.lang.String val) {
            try {
                this.value = ((AttributeType.Int)this.type).validate(Integer.parseInt(val));
                return true;
            }
            catch (Exception e) {
                e.printStackTrace();
                this.value = (Integer)((AttributeType.Int)this.type).getInitialValue();
                return false;
            }
        }

        @Override
        public boolean equalTo(Int other) {
            if (this.type == other.type) {
                return this.value == other.value;
            }
            return true;
        }

        @Override
        public Int copy() {
            Int copy = AttributeFactory.AllocAttributeInt();
            copy.setType((AttributeType.Int)this.type);
            copy.value = this.value;
            return copy;
        }

        @Override
        protected void release() {
            AttributeFactory.Release(this);
        }

        @Override
        public void save(ByteBuffer output) {
            output.putInt(this.value);
        }

        @Override
        public void load(ByteBuffer input) {
            this.value = input.getInt();
        }
    }

    @UsedFromLua
    public static class Short
    extends Numeric<Short, AttributeType.Short> {
        private short value;

        @Override
        protected void setType(AttributeType.Short type) {
            this.type = type;
            this.value = (java.lang.Short)type.getInitialValue();
        }

        public short getValue() {
            return this.value;
        }

        public void setValue(short value) {
            if (this.canSetValue()) {
                this.value = ((AttributeType.Short)this.type).validate(value);
            }
        }

        @Override
        public float floatValue() {
            return this.value;
        }

        @Override
        public void fromFloat(float f) {
            this.setValue((short)f);
        }

        @Override
        public java.lang.String stringValue() {
            return java.lang.Short.toString(this.value);
        }

        @Override
        public boolean setValueFromScriptString(java.lang.String val) {
            try {
                this.value = ((AttributeType.Short)this.type).validate(java.lang.Short.parseShort(val));
                return true;
            }
            catch (Exception e) {
                e.printStackTrace();
                this.value = (java.lang.Short)((AttributeType.Short)this.type).getInitialValue();
                return false;
            }
        }

        @Override
        public boolean equalTo(Short other) {
            if (this.type == other.type) {
                return this.value == other.value;
            }
            return true;
        }

        @Override
        public Short copy() {
            Short copy = AttributeFactory.AllocAttributeShort();
            copy.setType((AttributeType.Short)this.type);
            copy.value = this.value;
            return copy;
        }

        @Override
        protected void release() {
            AttributeFactory.Release(this);
        }

        @Override
        public void save(ByteBuffer output) {
            output.putShort(this.value);
        }

        @Override
        public void load(ByteBuffer input) {
            this.value = input.getShort();
        }
    }

    @UsedFromLua
    public static class Byte
    extends Numeric<Byte, AttributeType.Byte> {
        private byte value;

        @Override
        protected void setType(AttributeType.Byte type) {
            this.type = type;
            this.value = (java.lang.Byte)type.getInitialValue();
        }

        public byte getValue() {
            return this.value;
        }

        public void setValue(byte value) {
            if (this.canSetValue()) {
                this.value = ((AttributeType.Byte)this.type).validate(value);
            }
        }

        @Override
        public float floatValue() {
            return this.value;
        }

        @Override
        public void fromFloat(float f) {
            this.setValue((byte)f);
        }

        @Override
        public java.lang.String stringValue() {
            return java.lang.Byte.toString(this.value);
        }

        @Override
        public boolean setValueFromScriptString(java.lang.String val) {
            try {
                this.value = ((AttributeType.Byte)this.type).validate(java.lang.Byte.parseByte(val));
                return true;
            }
            catch (Exception e) {
                e.printStackTrace();
                this.value = (java.lang.Byte)((AttributeType.Byte)this.type).getInitialValue();
                return false;
            }
        }

        @Override
        public boolean equalTo(Byte other) {
            if (this.type == other.type) {
                return this.value == other.value;
            }
            return true;
        }

        @Override
        public Byte copy() {
            Byte copy = AttributeFactory.AllocAttributeByte();
            copy.setType((AttributeType.Byte)this.type);
            copy.value = this.value;
            return copy;
        }

        @Override
        protected void release() {
            AttributeFactory.Release(this);
        }

        @Override
        public void save(ByteBuffer output) {
            output.put(this.value);
        }

        @Override
        public void load(ByteBuffer input) {
            this.value = input.get();
        }
    }

    @UsedFromLua
    public static class Double
    extends Numeric<Double, AttributeType.Double> {
        private double value;

        @Override
        protected void setType(AttributeType.Double type) {
            this.type = type;
            this.value = (java.lang.Double)type.getInitialValue();
        }

        public double getValue() {
            return this.value;
        }

        public void setValue(double value) {
            if (this.canSetValue()) {
                this.value = ((AttributeType.Double)this.type).validate(value);
            }
        }

        @Override
        public float floatValue() {
            if (this.value < -3.4028234663852886E38 || this.value > 3.4028234663852886E38) {
                DebugLog.General.error("Attribute '" + java.lang.String.valueOf(this.type) + "' double value exceeds float bounds.");
            }
            return (float)this.value;
        }

        @Override
        public void fromFloat(float f) {
            this.setValue(f);
        }

        @Override
        public java.lang.String stringValue() {
            return java.lang.Double.toString(this.value);
        }

        @Override
        public boolean setValueFromScriptString(java.lang.String val) {
            try {
                this.value = ((AttributeType.Double)this.type).validate(java.lang.Double.parseDouble(val));
                return true;
            }
            catch (Exception e) {
                e.printStackTrace();
                this.value = (java.lang.Double)((AttributeType.Double)this.type).getInitialValue();
                return false;
            }
        }

        @Override
        public boolean equalTo(Double other) {
            if (this.type == other.type) {
                return this.value == other.value;
            }
            return true;
        }

        @Override
        public Double copy() {
            Double copy = AttributeFactory.AllocAttributeDouble();
            copy.setType((AttributeType.Double)this.type);
            copy.value = this.value;
            return copy;
        }

        @Override
        protected void release() {
            AttributeFactory.Release(this);
        }

        @Override
        public void save(ByteBuffer output) {
            output.putDouble(this.value);
        }

        @Override
        public void load(ByteBuffer input) {
            this.value = input.getDouble();
        }
    }

    @UsedFromLua
    public static class Float
    extends Numeric<Float, AttributeType.Float> {
        private float value;

        @Override
        protected void setType(AttributeType.Float type) {
            this.type = type;
            this.value = ((java.lang.Float)type.getInitialValue()).floatValue();
        }

        public float getValue() {
            return this.value;
        }

        public void setValue(float value) {
            if (this.canSetValue()) {
                this.value = ((AttributeType.Float)this.type).validate(java.lang.Float.valueOf(value)).floatValue();
            }
        }

        @Override
        public float floatValue() {
            return this.value;
        }

        @Override
        public void fromFloat(float f) {
            this.setValue(f);
        }

        @Override
        public java.lang.String stringValue() {
            return java.lang.Float.toString(this.value);
        }

        @Override
        public boolean setValueFromScriptString(java.lang.String val) {
            try {
                this.value = ((AttributeType.Float)this.type).validate(java.lang.Float.valueOf(java.lang.Float.parseFloat(val))).floatValue();
                return true;
            }
            catch (Exception e) {
                e.printStackTrace();
                this.value = ((java.lang.Float)((AttributeType.Float)this.type).getInitialValue()).floatValue();
                return false;
            }
        }

        @Override
        public boolean equalTo(Float other) {
            if (this.type == other.type) {
                return this.value == other.value;
            }
            return true;
        }

        @Override
        public Float copy() {
            Float copy = AttributeFactory.AllocAttributeFloat();
            copy.setType((AttributeType.Float)this.type);
            copy.value = this.value;
            return copy;
        }

        @Override
        protected void release() {
            AttributeFactory.Release(this);
        }

        @Override
        public void save(ByteBuffer output) {
            output.putFloat(this.value);
        }

        @Override
        public void load(ByteBuffer input) {
            this.value = input.getFloat();
        }
    }

    @UsedFromLua
    public static abstract class Numeric<C extends Numeric<C, T>, T extends AttributeType.Numeric<T, ?>>
    extends AttributeInstance<C, T> {
        public abstract float floatValue();

        public abstract void fromFloat(float var1);

        @Override
        public boolean isRequiresValidation() {
            return ((AttributeType.Numeric)this.type).isRequiresValidation();
        }

        @Override
        public boolean isDisplayAsBar() {
            if (((AttributeType.Numeric)this.type).getDisplayAsBar() != Attribute.UI.DisplayAsBar.Never) {
                return ((AttributeType.Numeric)this.type).getVars() != null;
            }
            return false;
        }

        @Override
        public float getDisplayAsBarUnit() {
            if (((AttributeType.Numeric)this.type).getVars() != null) {
                float min = ((Number)((AttributeType.Numeric)this.type).getVars().min).floatValue();
                float max = ((Number)((AttributeType.Numeric)this.type).getVars().max).floatValue();
                float val = this.floatValue();
                return (val - min) / (max - min);
            }
            return 0.0f;
        }

        @Override
        public float getFloatValue() {
            return this.floatValue();
        }

        @Override
        public int getIntValue() {
            return (int)this.floatValue();
        }
    }

    @UsedFromLua
    public static class String
    extends AttributeInstance<String, AttributeType.String> {
        private java.lang.String value;

        @Override
        protected void setType(AttributeType.String type) {
            this.type = type;
            this.value = type.getInitialValue();
        }

        public java.lang.String getValue() {
            return this.value;
        }

        public void setValue(java.lang.String value) {
            if (this.canSetValue()) {
                this.value = value;
            }
        }

        @Override
        public java.lang.String stringValue() {
            return this.value;
        }

        @Override
        public boolean setValueFromScriptString(java.lang.String val) {
            try {
                this.value = val;
                return true;
            }
            catch (Exception e) {
                e.printStackTrace();
                this.value = ((AttributeType.String)this.type).getInitialValue();
                return false;
            }
        }

        @Override
        public boolean equalTo(String other) {
            if (this.type == other.type) {
                return this.value.equals(other.value);
            }
            return true;
        }

        @Override
        public String copy() {
            String copy = AttributeFactory.AllocAttributeString();
            copy.setType((AttributeType.String)this.type);
            copy.value = this.value;
            return copy;
        }

        @Override
        protected void release() {
            AttributeFactory.Release(this);
        }

        @Override
        public void save(ByteBuffer output) {
            GameWindow.WriteString(output, this.value);
        }

        @Override
        public void load(ByteBuffer input) {
            this.value = GameWindow.ReadString(input);
        }
    }

    @UsedFromLua
    public static class Bool
    extends AttributeInstance<Bool, AttributeType.Bool> {
        private boolean value;

        @Override
        protected void setType(AttributeType.Bool type) {
            this.type = type;
            this.value = type.getInitialValue();
        }

        public boolean getValue() {
            return this.value;
        }

        public void setValue(boolean value) {
            if (this.canSetValue()) {
                this.value = value;
            }
        }

        @Override
        public java.lang.String stringValue() {
            return Boolean.toString(this.value);
        }

        @Override
        public boolean setValueFromScriptString(java.lang.String val) {
            try {
                this.value = Boolean.parseBoolean(val);
                return true;
            }
            catch (Exception e) {
                e.printStackTrace();
                this.value = ((AttributeType.Bool)this.type).getInitialValue();
                return false;
            }
        }

        @Override
        public boolean equalTo(Bool other) {
            if (this.type == other.type) {
                return this.value == other.value;
            }
            return true;
        }

        @Override
        public Bool copy() {
            Bool copy = AttributeFactory.AllocAttributeBool();
            copy.setType((AttributeType.Bool)this.type);
            copy.value = this.value;
            return copy;
        }

        @Override
        protected void release() {
            AttributeFactory.Release(this);
        }

        @Override
        public void save(ByteBuffer output) {
            output.put(this.value ? (byte)1 : 0);
        }

        @Override
        public void load(ByteBuffer input) {
            this.value = input.get() != 0;
        }
    }

    @UsedFromLua
    public static class EnumStringSet<E extends java.lang.Enum<E>>
    extends AttributeInstance<EnumStringSet<E>, AttributeType.EnumStringSet<E>> {
        private final EnumStringObj<E> value = new EnumStringObj();

        @Override
        protected void setType(AttributeType.EnumStringSet<E> type) {
            this.type = type;
            this.value.initialize(type.getEnumClass());
            this.value.addAll(true, type.getInitialValue());
        }

        public EnumStringObj<E> getValue() {
            return this.value;
        }

        public void setValue(EnumStringObj<E> value) {
            if (this.canSetValue()) {
                this.value.addAll(true, value);
            }
        }

        @Override
        public java.lang.String stringValue() {
            return this.value.toString();
        }

        @Override
        public boolean setValueFromScriptString(java.lang.String val) {
            try {
                this.value.clear();
                if (val.contains(";")) {
                    java.lang.String[] split;
                    for (java.lang.String s : split = val.split(";")) {
                        if (AttributeUtil.isEnumString(s)) {
                            this.addEnumValueFromString(s);
                            continue;
                        }
                        this.addStringValue(s);
                    }
                } else if (AttributeUtil.isEnumString(val)) {
                    this.addEnumValueFromString(val);
                } else {
                    this.addStringValue(val);
                }
                return true;
            }
            catch (Exception e) {
                e.printStackTrace();
                return false;
            }
        }

        public void addEnumValueFromString(java.lang.String val) {
            Object e = ((AttributeType.EnumStringSet)this.type).enumValueFromString(val);
            if (e == null) {
                throw new NullPointerException("Attribute.EnumSet Cannot read Enum script value '" + val + "'.");
            }
            this.value.add(e);
        }

        public boolean removeEnumValueFromString(java.lang.String val) {
            Object e = ((AttributeType.EnumStringSet)this.type).enumValueFromString(val);
            if (e != null) {
                return this.value.remove(e);
            }
            throw new NullPointerException("Attribute.EnumSet Cannot read Enum script value '" + val + "'.");
        }

        public void addStringValue(java.lang.String val) {
            this.value.add(val);
        }

        public boolean removeStringValue(java.lang.String val) {
            return this.value.remove(val);
        }

        public void clear() {
            this.value.clear();
        }

        @Override
        public boolean equalTo(EnumStringSet<E> other) {
            if (this.type == other.type) {
                return this.value.equals(other.value);
            }
            return true;
        }

        @Override
        public EnumStringSet<E> copy() {
            EnumStringSet copy = AttributeFactory.AllocAttributeEnumStringSet();
            copy.setType((AttributeType.EnumStringSet)this.type);
            copy.value.initialize(((AttributeType.EnumStringSet)this.type).getEnumClass());
            copy.value.addAll(this.value);
            return copy;
        }

        @Override
        protected void release() {
            AttributeFactory.Release(this);
        }

        @Override
        protected void reset() {
            super.reset();
            this.value.reset();
        }

        @Override
        public void save(ByteBuffer output) {
            output.put((byte)this.value.getEnumValues().size());
            for (java.lang.Enum val : this.value.getEnumValues()) {
                output.put(((IOEnum)((Object)val)).getByteId());
            }
            output.put((byte)this.value.getStringValues().size());
            for (int i = 0; i < this.value.getStringValues().size(); ++i) {
                GameWindow.WriteString(output, this.value.getStringValues().get(i));
            }
        }

        @Override
        public void load(ByteBuffer input) {
            int i;
            if (!this.value.isEmpty()) {
                this.value.clear();
            }
            int size = input.get();
            for (i = 0; i < size; ++i) {
                byte id = input.get();
                Object val = ((AttributeType.EnumStringSet)this.type).enumValueFromByteID(id);
                if (val != null) {
                    this.value.add(((AttributeType.EnumStringSet)this.type).enumValueFromByteID(id));
                    continue;
                }
                DebugLog.General.error("Could not load value for EnumStringSet attribute '" + java.lang.String.valueOf(this.type) + "'.");
            }
            size = input.get();
            for (i = 0; i < size; ++i) {
                java.lang.String s = GameWindow.ReadString(input);
                this.value.add(s);
            }
        }
    }

    @UsedFromLua
    public static class EnumSet<E extends java.lang.Enum<E>>
    extends AttributeInstance<EnumSet<E>, AttributeType.EnumSet<E>> {
        private java.util.EnumSet<E> value;

        @Override
        protected void setType(AttributeType.EnumSet<E> type) {
            this.type = type;
            this.value = java.util.EnumSet.copyOf(type.getInitialValue());
        }

        public java.util.EnumSet<E> getValue() {
            return this.value;
        }

        public void setValue(java.util.EnumSet<E> value) {
            if (this.canSetValue()) {
                this.value = value;
            }
        }

        @Override
        public java.lang.String stringValue() {
            return this.value.toString();
        }

        @Override
        public boolean setValueFromScriptString(java.lang.String val) {
            try {
                if (!this.value.isEmpty()) {
                    this.value.clear();
                }
                if (val.contains(";")) {
                    java.lang.String[] split;
                    for (java.lang.String s : split = val.split(";")) {
                        this.addValueFromString(s);
                    }
                } else {
                    this.addValueFromString(val);
                }
                return true;
            }
            catch (Exception e) {
                DebugLog.General.error("Error in script string '" + val + "'");
                e.printStackTrace();
                return false;
            }
        }

        public void addValueFromString(java.lang.String val) {
            Object e = ((AttributeType.EnumSet)this.type).enumValueFromString(val);
            if (e == null) {
                throw new NullPointerException("Attribute.EnumSet Cannot read script value '" + val + "'.");
            }
            this.value.add(e);
        }

        public boolean removeValueFromString(java.lang.String val) {
            Object e = ((AttributeType.EnumSet)this.type).enumValueFromString(val);
            if (e != null) {
                return this.value.remove(e);
            }
            throw new NullPointerException("Attribute.EnumSet Cannot read script value '" + val + "'.");
        }

        public void clear() {
            this.value.clear();
        }

        @Override
        public boolean equalTo(EnumSet<E> other) {
            if (this.type == other.type) {
                return this.value.equals(other.value);
            }
            return true;
        }

        @Override
        public EnumSet<E> copy() {
            EnumSet copy = AttributeFactory.AllocAttributeEnumSet();
            copy.setType((AttributeType.EnumSet)this.type);
            copy.value.addAll(this.value);
            return copy;
        }

        @Override
        protected void release() {
            AttributeFactory.Release(this);
        }

        @Override
        protected void reset() {
            super.reset();
            this.value = null;
        }

        @Override
        public void save(ByteBuffer output) {
            output.put((byte)this.value.size());
            for (java.lang.Enum val : this.value) {
                output.put(((IOEnum)((Object)val)).getByteId());
            }
        }

        @Override
        public void load(ByteBuffer input) {
            if (!this.value.isEmpty()) {
                this.value.clear();
            }
            int size = input.get();
            for (int i = 0; i < size; ++i) {
                byte id = input.get();
                Object val = ((AttributeType.EnumSet)this.type).enumValueFromByteID(id);
                if (val != null) {
                    this.value.add(((AttributeType.EnumSet)this.type).enumValueFromByteID(id));
                    continue;
                }
                DebugLog.General.error("Could not load value for EnumSet attribute '" + java.lang.String.valueOf(this.type) + "'.");
            }
        }
    }

    @UsedFromLua
    public static class Enum<E extends java.lang.Enum<E>>
    extends AttributeInstance<Enum<E>, AttributeType.Enum<E>> {
        private E value;

        @Override
        protected void setType(AttributeType.Enum<E> type) {
            this.type = type;
            this.value = type.getInitialValue();
        }

        public E getValue() {
            return this.value;
        }

        public void setValue(E value) {
            if (this.canSetValue()) {
                this.value = value;
            }
        }

        @Override
        public java.lang.String stringValue() {
            return ((java.lang.Enum)this.value).toString();
        }

        @Override
        public boolean setValueFromScriptString(java.lang.String val) {
            this.value = ((AttributeType.Enum)this.type).enumValueFromString(val);
            if (this.value == null) {
                this.value = ((AttributeType.Enum)this.type).getInitialValue();
            }
            return true;
        }

        @Override
        public boolean equalTo(Enum<E> other) {
            if (this.type == other.type) {
                return this.value == other.value;
            }
            return true;
        }

        @Override
        public Enum<E> copy() {
            Enum copy = AttributeFactory.AllocAttributeEnum();
            copy.setType((AttributeType.Enum)this.type);
            copy.value = this.value;
            return copy;
        }

        @Override
        protected void release() {
            AttributeFactory.Release(this);
        }

        @Override
        protected void reset() {
            super.reset();
            this.value = null;
        }

        @Override
        public void save(ByteBuffer output) {
            output.put(((IOEnum)this.value).getByteId());
        }

        @Override
        public void load(ByteBuffer input) {
            byte id = input.get();
            this.value = ((AttributeType.Enum)this.type).enumValueFromByteID(id);
            if (this.value == null) {
                DebugLog.General.error("Could not load value for Enum attribute '" + java.lang.String.valueOf(this.type) + "', setting default.");
                this.value = ((AttributeType.Enum)this.type).getInitialValue();
            }
        }
    }
}

