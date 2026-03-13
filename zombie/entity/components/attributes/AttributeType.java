/*
 * Decompiled with CFR 0.152.
 */
package zombie.entity.components.attributes;

import java.util.Objects;
import zombie.UsedFromLua;
import zombie.core.Translator;
import zombie.debug.DebugLog;
import zombie.entity.components.attributes.Attribute;
import zombie.entity.components.attributes.AttributeUtil;
import zombie.entity.components.attributes.AttributeValueType;
import zombie.entity.components.attributes.EnumStringObj;
import zombie.entity.util.enums.IOEnum;
import zombie.util.StringUtils;

@UsedFromLua
public abstract class AttributeType {
    private static final int MAX_ID = 8128;
    private final short id;
    private final java.lang.String name;
    private final java.lang.String translateKey;
    private final java.lang.String tooltipOverride;
    private final Attribute.UI.Display optionDisplay;
    private final Attribute.UI.DisplayAsBar optionDisplayAsBar;
    private final boolean readOnly;

    protected AttributeType(short id, java.lang.String name, boolean readOnly, Attribute.UI.Display display, java.lang.String tooltipOverride) {
        this(id, name, readOnly, display, Attribute.UI.DisplayAsBar.Default, tooltipOverride);
    }

    protected AttributeType(short id, java.lang.String name, boolean readOnly, Attribute.UI.Display display, Attribute.UI.DisplayAsBar asBar, java.lang.String tooltipOverride) {
        if (id < 0 || id > 8128) {
            throw new RuntimeException("AttributeType Id may not exceed '8128' or be less than zero.");
        }
        if (name == null) {
            throw new RuntimeException("AttributeType name cannot be null.");
        }
        if (StringUtils.containsWhitespace(name)) {
            DebugLog.General.error("Sanitizing AttributeType name '" + name + "', name may not contain whitespaces.");
            name = StringUtils.removeWhitespace(name);
        }
        this.id = id;
        this.name = name;
        this.optionDisplay = display;
        this.optionDisplayAsBar = asBar;
        this.tooltipOverride = tooltipOverride;
        this.translateKey = "Attribute_Type_" + this.name;
        this.readOnly = readOnly;
    }

    public short id() {
        return this.id;
    }

    public boolean isReadOnly() {
        return this.readOnly;
    }

    public java.lang.String toString() {
        return this.getName();
    }

    public java.lang.String getName() {
        return this.name;
    }

    public abstract AttributeValueType getValueType();

    public boolean isNumeric() {
        return AttributeValueType.IsNumeric(this.getValueType());
    }

    public boolean isDecimal() {
        return AttributeValueType.IsDecimal(this.getValueType());
    }

    public boolean isHiddenUI() {
        return this.optionDisplay == Attribute.UI.Display.Hidden;
    }

    protected Attribute.UI.DisplayAsBar getDisplayAsBar() {
        return this.optionDisplayAsBar;
    }

    public java.lang.String getTranslateKey() {
        return this.translateKey;
    }

    private java.lang.String getTranslatedName() {
        java.lang.String s = Translator.getAttributeTextOrNull(this.translateKey);
        if (s != null) {
            return s;
        }
        return this.getName();
    }

    public java.lang.String getNameUI() {
        java.lang.String s;
        if (this.tooltipOverride != null && (s = Translator.getAttributeTextOrNull(this.tooltipOverride)) != null) {
            return s;
        }
        return this.getTranslatedName();
    }

    @UsedFromLua
    public static class Long
    extends Numeric<Long, java.lang.Long> {
        protected Long(short id, java.lang.String name, long initialValue) {
            super(id, name, initialValue, false, Attribute.UI.Display.Visible, Attribute.UI.DisplayAsBar.Default, null);
        }

        protected Long(short id, java.lang.String name, long initialValue, boolean readOnly, Attribute.UI.Display display, Attribute.UI.DisplayAsBar asBar, java.lang.String tooltipOverride) {
            super(id, name, initialValue, readOnly, display, asBar, tooltipOverride);
        }

        @Override
        public AttributeValueType getValueType() {
            return AttributeValueType.Long;
        }

        @Override
        public java.lang.Long validate(java.lang.Long value) {
            if (this.isRequiresValidation() && this.getVars() != null) {
                value = Math.min(value, (java.lang.Long)this.getVars().max);
                value = Math.max(value, (java.lang.Long)this.getVars().min);
            }
            return value;
        }

        @Override
        public java.lang.Long getMin() {
            return this.getVars() != null ? (java.lang.Long)this.getVars().min : java.lang.Long.MIN_VALUE;
        }

        @Override
        public java.lang.Long getMax() {
            return this.getVars() != null ? (java.lang.Long)this.getVars().max : java.lang.Long.MAX_VALUE;
        }

        @Override
        protected boolean withinBounds(java.lang.Long value) {
            if (this.getVars() != null) {
                return value >= (java.lang.Long)this.getVars().min && value <= (java.lang.Long)this.getVars().max;
            }
            return true;
        }
    }

    @UsedFromLua
    public static class Int
    extends Numeric<Int, Integer> {
        protected Int(short id, java.lang.String name, int initialValue) {
            super(id, name, initialValue, false, Attribute.UI.Display.Visible, Attribute.UI.DisplayAsBar.Default, null);
        }

        protected Int(short id, java.lang.String name, int initialValue, boolean readOnly, Attribute.UI.Display display, Attribute.UI.DisplayAsBar asBar, java.lang.String tooltipOverride) {
            super(id, name, initialValue, readOnly, display, asBar, tooltipOverride);
        }

        @Override
        public AttributeValueType getValueType() {
            return AttributeValueType.Int;
        }

        @Override
        public Integer validate(Integer value) {
            if (this.isRequiresValidation() && this.getVars() != null) {
                value = Math.min(value, (Integer)this.getVars().max);
                value = Math.max(value, (Integer)this.getVars().min);
            }
            return value;
        }

        @Override
        public Integer getMin() {
            return this.getVars() != null ? (Integer)this.getVars().min : Integer.MIN_VALUE;
        }

        @Override
        public Integer getMax() {
            return this.getVars() != null ? (Integer)this.getVars().max : Integer.MAX_VALUE;
        }

        @Override
        protected boolean withinBounds(Integer value) {
            if (this.getVars() != null) {
                return value >= (Integer)this.getVars().min && value <= (Integer)this.getVars().max;
            }
            return true;
        }
    }

    @UsedFromLua
    public static class Short
    extends Numeric<Short, java.lang.Short> {
        protected Short(short id, java.lang.String name, short initialValue) {
            super(id, name, initialValue, false, Attribute.UI.Display.Visible, Attribute.UI.DisplayAsBar.Default, null);
        }

        protected Short(short id, java.lang.String name, short initialValue, boolean readOnly, Attribute.UI.Display display, Attribute.UI.DisplayAsBar asBar, java.lang.String tooltipOverride) {
            super(id, name, initialValue, readOnly, display, asBar, tooltipOverride);
        }

        @Override
        public AttributeValueType getValueType() {
            return AttributeValueType.Short;
        }

        @Override
        public java.lang.Short validate(java.lang.Short value) {
            if (this.isRequiresValidation() && this.getVars() != null) {
                value = (short)Math.min(value.shortValue(), ((java.lang.Short)this.getVars().max).shortValue());
                value = (short)Math.max(value.shortValue(), ((java.lang.Short)this.getVars().min).shortValue());
            }
            return value;
        }

        @Override
        public java.lang.Short getMin() {
            return this.getVars() != null ? (java.lang.Short)this.getVars().min : (short)java.lang.Short.MIN_VALUE;
        }

        @Override
        public java.lang.Short getMax() {
            return this.getVars() != null ? (java.lang.Short)this.getVars().max : (short)java.lang.Short.MAX_VALUE;
        }

        @Override
        protected boolean withinBounds(java.lang.Short value) {
            if (this.getVars() != null) {
                return value >= (java.lang.Short)this.getVars().min && value <= (java.lang.Short)this.getVars().max;
            }
            return true;
        }
    }

    @UsedFromLua
    public static class Byte
    extends Numeric<Byte, java.lang.Byte> {
        protected Byte(short id, java.lang.String name, byte initialValue) {
            super(id, name, initialValue, false, Attribute.UI.Display.Visible, Attribute.UI.DisplayAsBar.Default, null);
        }

        protected Byte(short id, java.lang.String name, byte initialValue, boolean readOnly, Attribute.UI.Display display, Attribute.UI.DisplayAsBar asBar, java.lang.String tooltipOverride) {
            super(id, name, initialValue, readOnly, display, asBar, tooltipOverride);
        }

        @Override
        public AttributeValueType getValueType() {
            return AttributeValueType.Byte;
        }

        @Override
        public java.lang.Byte validate(java.lang.Byte value) {
            if (this.isRequiresValidation() && this.getVars() != null) {
                value = (byte)Math.min(value.byteValue(), ((java.lang.Byte)this.getVars().max).byteValue());
                value = (byte)Math.max(value.byteValue(), ((java.lang.Byte)this.getVars().min).byteValue());
            }
            return value;
        }

        @Override
        public java.lang.Byte getMin() {
            return this.getVars() != null ? (java.lang.Byte)this.getVars().min : (byte)-128;
        }

        @Override
        public java.lang.Byte getMax() {
            return this.getVars() != null ? (java.lang.Byte)this.getVars().max : (byte)127;
        }

        @Override
        protected boolean withinBounds(java.lang.Byte value) {
            if (this.getVars() != null) {
                return value >= (java.lang.Byte)this.getVars().min && value <= (java.lang.Byte)this.getVars().max;
            }
            return true;
        }
    }

    @UsedFromLua
    public static class Double
    extends Numeric<Double, java.lang.Double> {
        protected Double(short id, java.lang.String name, double initialValue) {
            super(id, name, initialValue, false, Attribute.UI.Display.Visible, Attribute.UI.DisplayAsBar.Default, null);
        }

        protected Double(short id, java.lang.String name, double initialValue, boolean readOnly, Attribute.UI.Display display, Attribute.UI.DisplayAsBar asBar, java.lang.String tooltipOverride) {
            super(id, name, initialValue, readOnly, display, asBar, tooltipOverride);
        }

        @Override
        public AttributeValueType getValueType() {
            return AttributeValueType.Double;
        }

        @Override
        public java.lang.Double validate(java.lang.Double value) {
            if (this.isRequiresValidation() && this.getVars() != null) {
                value = Math.min(value, (java.lang.Double)this.getVars().max);
                value = Math.max(value, (java.lang.Double)this.getVars().min);
            }
            return value;
        }

        @Override
        public java.lang.Double getMin() {
            return this.getVars() != null ? (java.lang.Double)this.getVars().min : java.lang.Double.MIN_VALUE;
        }

        @Override
        public java.lang.Double getMax() {
            return this.getVars() != null ? (java.lang.Double)this.getVars().max : java.lang.Double.MAX_VALUE;
        }

        @Override
        protected boolean withinBounds(java.lang.Double value) {
            if (this.getVars() != null) {
                return value >= (java.lang.Double)this.getVars().min && value <= (java.lang.Double)this.getVars().max;
            }
            return true;
        }
    }

    @UsedFromLua
    public static class Float
    extends Numeric<Float, java.lang.Float> {
        protected Float(short id, java.lang.String name, float initialValue) {
            super(id, name, java.lang.Float.valueOf(initialValue), false, Attribute.UI.Display.Visible, Attribute.UI.DisplayAsBar.Default, null);
        }

        protected Float(short id, java.lang.String name, float initialValue, boolean readOnly, Attribute.UI.Display display, Attribute.UI.DisplayAsBar asBar, java.lang.String tooltipOverride) {
            super(id, name, java.lang.Float.valueOf(initialValue), readOnly, display, asBar, tooltipOverride);
        }

        @Override
        public java.lang.Float validate(java.lang.Float value) {
            if (this.isRequiresValidation() && this.getVars() != null) {
                value = java.lang.Float.valueOf(Math.min(value.floatValue(), ((java.lang.Float)this.getVars().max).floatValue()));
                value = java.lang.Float.valueOf(Math.max(value.floatValue(), ((java.lang.Float)this.getVars().min).floatValue()));
            }
            return value;
        }

        @Override
        public java.lang.Float getMin() {
            return java.lang.Float.valueOf(this.getVars() != null ? ((java.lang.Float)this.getVars().min).floatValue() : java.lang.Float.MIN_VALUE);
        }

        @Override
        public java.lang.Float getMax() {
            return java.lang.Float.valueOf(this.getVars() != null ? ((java.lang.Float)this.getVars().max).floatValue() : java.lang.Float.MAX_VALUE);
        }

        @Override
        protected boolean withinBounds(java.lang.Float value) {
            if (this.getVars() != null) {
                return value.floatValue() >= ((java.lang.Float)this.getVars().min).floatValue() && value.floatValue() <= ((java.lang.Float)this.getVars().max).floatValue();
            }
            return true;
        }
    }

    @UsedFromLua
    public static abstract class Numeric<C extends Numeric<C, T>, T extends Number>
    extends AttributeType {
        private final T initialValue;
        private NumericVars<T> vars;
        private boolean requiresValidation;

        protected Numeric(short id, java.lang.String name, T initialValue, boolean readOnly, Attribute.UI.Display display, Attribute.UI.DisplayAsBar asBar, java.lang.String tooltipOverride) {
            super(id, name, readOnly, display, asBar, tooltipOverride);
            this.initialValue = (Number)Objects.requireNonNull(initialValue);
        }

        @Override
        public AttributeValueType getValueType() {
            return AttributeValueType.Float;
        }

        protected boolean isRequiresValidation() {
            return this.requiresValidation;
        }

        protected NumericVars<T> getVars() {
            return this.vars;
        }

        public T getInitialValue() {
            return this.initialValue;
        }

        protected final Numeric<C, T> setBounds(T min, T max) {
            if (((Number)min).doubleValue() < 0.0 || ((Number)min).doubleValue() >= ((Number)max).doubleValue() || ((Number)max).doubleValue() <= 0.0) {
                throw new IllegalArgumentException("Illegal 'Bounds' on Attribute [" + java.lang.String.valueOf(this) + "]");
            }
            this.requiresValidation = true;
            if (this.vars == null) {
                this.vars = new NumericVars<T>(min, max);
            }
            if (!this.withinBounds(this.initialValue)) {
                throw new IllegalArgumentException("Initialvalue outside set bounds.");
            }
            return this;
        }

        public boolean hasBounds() {
            return this.requiresValidation;
        }

        public abstract T validate(T var1);

        public abstract T getMin();

        public abstract T getMax();

        protected abstract boolean withinBounds(T var1);

        protected static class NumericVars<T> {
            protected final T min;
            protected final T max;

            protected NumericVars(T min, T max) {
                this.min = min;
                this.max = max;
            }
        }
    }

    @UsedFromLua
    public static class String
    extends AttributeType {
        private final java.lang.String initialValue;

        protected String(short id, java.lang.String name, java.lang.String initialValue) {
            super(id, name, false, Attribute.UI.Display.Visible, Attribute.UI.DisplayAsBar.Never, null);
            this.initialValue = Objects.requireNonNull(initialValue);
        }

        protected String(short id, java.lang.String name, java.lang.String initialValue, boolean readOnly, Attribute.UI.Display display, java.lang.String tooltipOverride) {
            super(id, name, readOnly, display, Attribute.UI.DisplayAsBar.Never, tooltipOverride);
            this.initialValue = Objects.requireNonNull(initialValue);
        }

        @Override
        public AttributeValueType getValueType() {
            return AttributeValueType.String;
        }

        public java.lang.String getInitialValue() {
            return this.initialValue;
        }
    }

    @UsedFromLua
    public static class Bool
    extends AttributeType {
        private final boolean initialValue;

        protected Bool(short id, java.lang.String name, boolean initialValue) {
            super(id, name, false, Attribute.UI.Display.Visible, Attribute.UI.DisplayAsBar.Never, null);
            this.initialValue = initialValue;
        }

        protected Bool(short id, java.lang.String name, boolean initialValue, boolean readOnly, Attribute.UI.Display display, java.lang.String tooltipOverride) {
            super(id, name, readOnly, display, Attribute.UI.DisplayAsBar.Never, tooltipOverride);
            this.initialValue = initialValue;
        }

        @Override
        public AttributeValueType getValueType() {
            return AttributeValueType.Boolean;
        }

        public boolean getInitialValue() {
            return this.initialValue;
        }
    }

    @UsedFromLua
    public static class EnumStringSet<E extends java.lang.Enum<E>>
    extends AttributeType {
        private final Class<E> enumClass;
        private final EnumStringObj<E> initialValue;

        protected EnumStringSet(short id, java.lang.String name, Class<E> clazz) {
            super(id, name, false, Attribute.UI.Display.Visible, Attribute.UI.DisplayAsBar.Never, null);
            this.enumClass = Objects.requireNonNull(clazz);
            this.initialValue = new EnumStringObj();
            this.initialValue.initialize(this.enumClass);
        }

        protected EnumStringSet(short id, java.lang.String name, Class<E> clazz, boolean readOnly, Attribute.UI.Display display, java.lang.String tooltipOverride) {
            super(id, name, readOnly, display, Attribute.UI.DisplayAsBar.Never, tooltipOverride);
            this.enumClass = Objects.requireNonNull(clazz);
            this.initialValue = new EnumStringObj();
            this.initialValue.initialize(this.enumClass);
        }

        @Override
        public AttributeValueType getValueType() {
            return AttributeValueType.EnumStringSet;
        }

        public EnumStringObj<E> getInitialValue() {
            return this.initialValue;
        }

        public E enumValueFromString(java.lang.String s) {
            return AttributeUtil.enumValueFromScriptString(this.enumClass, s);
        }

        public E enumValueFromByteID(byte id) {
            for (java.lang.Enum option : (java.lang.Enum[])this.enumClass.getEnumConstants()) {
                if (((IOEnum)((Object)option)).getByteId() != id) continue;
                return (E)option;
            }
            return null;
        }

        protected Class<E> getEnumClass() {
            return this.enumClass;
        }
    }

    @UsedFromLua
    public static class EnumSet<E extends java.lang.Enum<E>>
    extends AttributeType {
        private final Class<E> enumClass;
        private final java.util.EnumSet<E> initialValue;

        protected EnumSet(short id, java.lang.String name, Class<E> clazz) {
            super(id, name, false, Attribute.UI.Display.Visible, Attribute.UI.DisplayAsBar.Never, null);
            this.enumClass = Objects.requireNonNull(clazz);
            this.initialValue = java.util.EnumSet.noneOf(this.enumClass);
        }

        protected EnumSet(short id, java.lang.String name, Class<E> clazz, boolean readOnly, Attribute.UI.Display display, java.lang.String tooltipOverride) {
            super(id, name, readOnly, display, Attribute.UI.DisplayAsBar.Never, tooltipOverride);
            this.enumClass = Objects.requireNonNull(clazz);
            this.initialValue = java.util.EnumSet.noneOf(this.enumClass);
        }

        @Override
        public AttributeValueType getValueType() {
            return AttributeValueType.EnumSet;
        }

        public java.util.EnumSet<E> getInitialValue() {
            return this.initialValue;
        }

        public E enumValueFromString(java.lang.String s) {
            return AttributeUtil.enumValueFromScriptString(this.enumClass, s);
        }

        public E enumValueFromByteID(byte id) {
            for (java.lang.Enum option : (java.lang.Enum[])this.enumClass.getEnumConstants()) {
                if (((IOEnum)((Object)option)).getByteId() != id) continue;
                return (E)option;
            }
            return null;
        }

        protected Class<E> getEnumClass() {
            return this.enumClass;
        }
    }

    @UsedFromLua
    public static class Enum<E extends java.lang.Enum<E>>
    extends AttributeType {
        private final Class<E> enumClass;
        private final E initialValue;

        protected Enum(short id, java.lang.String name, E initialValue) {
            super(id, name, false, Attribute.UI.Display.Visible, Attribute.UI.DisplayAsBar.Never, null);
            this.initialValue = (java.lang.Enum)Objects.requireNonNull(initialValue);
            this.enumClass = ((java.lang.Enum)this.initialValue).getDeclaringClass();
        }

        protected Enum(short id, java.lang.String name, E initialValue, boolean readOnly, Attribute.UI.Display display, java.lang.String tooltipOverride) {
            super(id, name, readOnly, display, Attribute.UI.DisplayAsBar.Never, tooltipOverride);
            this.initialValue = (java.lang.Enum)Objects.requireNonNull(initialValue);
            this.enumClass = ((java.lang.Enum)this.initialValue).getDeclaringClass();
        }

        @Override
        public AttributeValueType getValueType() {
            return AttributeValueType.Enum;
        }

        public E getInitialValue() {
            return this.initialValue;
        }

        public E enumValueFromString(java.lang.String s) {
            return AttributeUtil.enumValueFromScriptString(this.enumClass, s);
        }

        public E enumValueFromByteID(byte id) {
            Class clazz = ((java.lang.Enum)this.initialValue).getDeclaringClass();
            for (java.lang.Enum option : (java.lang.Enum[])clazz.getEnumConstants()) {
                if (((IOEnum)((Object)option)).getByteId() != id) continue;
                return (E)option;
            }
            return null;
        }
    }
}

