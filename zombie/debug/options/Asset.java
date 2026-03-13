/*
 * Decompiled with CFR 0.152.
 */
package zombie.debug.options;

import zombie.debug.BooleanDebugOption;
import zombie.debug.options.OptionGroup;

public class Asset
extends OptionGroup {
    public final BooleanDebugOption slowLoad = this.newOption("SlowLoad", false);
    public final BooleanDebugOption checkItemTexAndNames = this.newOption("CheckItemTexAndNames", false);
}

