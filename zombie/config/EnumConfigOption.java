/*
 * Decompiled with CFR 0.152.
 */
package zombie.config;

import zombie.UsedFromLua;
import zombie.config.IntegerConfigOption;

@UsedFromLua
public class EnumConfigOption
extends IntegerConfigOption {
    public EnumConfigOption(String name, int numValues, int defaultValue) {
        super(name, 1, numValues, defaultValue);
    }

    @Override
    public String getType() {
        return "enum";
    }

    public int getNumValues() {
        return this.max;
    }
}

