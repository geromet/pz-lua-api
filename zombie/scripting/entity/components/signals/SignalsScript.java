/*
 * Decompiled with CFR 0.152.
 */
package zombie.scripting.entity.components.signals;

import zombie.UsedFromLua;
import zombie.entity.ComponentType;
import zombie.scripting.entity.ComponentScript;

@UsedFromLua
public class SignalsScript
extends ComponentScript {
    private SignalsScript() {
        super(ComponentType.Signals);
    }

    protected void copyFrom(ComponentScript other) {
    }
}

