/*
 * Decompiled with CFR 0.152.
 */
package zombie.scripting.entity.components.parts;

import zombie.UsedFromLua;
import zombie.entity.ComponentType;
import zombie.scripting.entity.ComponentScript;

@UsedFromLua
public class PartsScript
extends ComponentScript {
    private PartsScript() {
        super(ComponentType.Parts);
    }

    protected void copyFrom(ComponentScript other) {
    }
}

