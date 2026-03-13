/*
 * Decompiled with CFR 0.152.
 */
package zombie.config;

import zombie.UsedFromLua;
import zombie.config.ConfigOption;
import zombie.debug.DebugLog;

@UsedFromLua
public class DoubleConfigOption
extends ConfigOption {
    protected double value;
    protected double defaultValue;
    protected double min;
    protected double max;

    public DoubleConfigOption(String name, double min, double max, double defaultValue) {
        super(name);
        if (defaultValue < min || defaultValue > max) {
            throw new IllegalArgumentException();
        }
        this.value = defaultValue;
        this.defaultValue = defaultValue;
        this.min = min;
        this.max = max;
    }

    @Override
    public String getType() {
        return "double";
    }

    public double getMin() {
        return this.min;
    }

    public double getMax() {
        return this.max;
    }

    @Override
    public void resetToDefault() {
        this.setValue(this.defaultValue);
    }

    @Override
    public void setDefaultToCurrentValue() {
        this.defaultValue = this.value;
    }

    @Override
    public void parse(String s) {
        try {
            double v = Double.parseDouble(s);
            this.setValue(v);
        }
        catch (NumberFormatException ex) {
            DebugLog.log("ERROR DoubleConfigOption.parse() \"" + this.name + "\" string=" + s + "\"");
        }
    }

    @Override
    public String getValueAsString() {
        return String.valueOf(this.value);
    }

    @Override
    public void setValueFromObject(Object o) {
        if (o instanceof Double) {
            Double d = (Double)o;
            this.setValue(d);
        } else if (o instanceof String) {
            String s = (String)o;
            this.parse(s);
        }
    }

    @Override
    public Object getValueAsObject() {
        return this.value;
    }

    @Override
    public boolean isValidString(String s) {
        try {
            double v = Double.parseDouble(s);
            return v >= this.min && v <= this.max;
        }
        catch (NumberFormatException ex) {
            return false;
        }
    }

    public void setValue(double value) {
        if (value < this.min) {
            DebugLog.log("ERROR: DoubleConfigOption.setValue() \"" + this.name + "\" " + value + " is less than min=" + this.min);
            return;
        }
        if (value > this.max) {
            DebugLog.log("ERROR: DoubleConfigOption.setValue() \"" + this.name + "\" " + value + " is greater than max=" + this.max);
            return;
        }
        this.value = value;
    }

    public double getValue() {
        return this.value;
    }

    public double getDefaultValue() {
        return this.defaultValue;
    }

    public void setDefault(double value) {
        this.defaultValue = value;
    }

    @Override
    public String getTooltip() {
        return String.valueOf(this.value);
    }

    @Override
    public ConfigOption makeCopy() {
        DoubleConfigOption copy = new DoubleConfigOption(this.name, this.min, this.max, this.defaultValue);
        copy.value = this.value;
        return copy;
    }
}

