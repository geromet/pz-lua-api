/*
 * Decompiled with CFR 0.152.
 */
package zombie.scripting.entity.components.lua;

import zombie.UsedFromLua;
import zombie.entity.ComponentType;
import zombie.scripting.entity.ComponentScript;

@UsedFromLua
public class LuaComponentScript
extends ComponentScript {
    private LuaComponentScript() {
        super(ComponentType.Lua);
    }

    protected void copyFrom(ComponentScript other) {
    }
}

