/*
 * Decompiled with CFR 0.152.
 */
package zombie.scripting.entity.components.test;

import zombie.UsedFromLua;
import zombie.debug.DebugLog;
import zombie.entity.ComponentType;
import zombie.scripting.ScriptParser;
import zombie.scripting.entity.ComponentScript;

@UsedFromLua
public class TestComponentScript
extends ComponentScript {
    private TestComponentScript() {
        super(ComponentType.TestComponent);
    }

    protected void copyFrom(ComponentScript other) {
    }

    @Override
    protected void load(ScriptParser.Block block) throws Exception {
        for (ScriptParser.Value value : block.values) {
            String key = value.getKey().trim();
            String val = value.getValue().trim();
            if (!key.isEmpty() && !val.isEmpty() && !key.equalsIgnoreCase("someKey") && !key.equalsIgnoreCase("someOtherKey")) continue;
        }
        for (ScriptParser.Block child : block.children) {
            if (child.type.equalsIgnoreCase("someType")) continue;
            DebugLog.General.error("Unknown block '" + child.type + "' in entity script: " + this.getName());
        }
    }
}

