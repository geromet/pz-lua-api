/*
 * Decompiled with CFR 0.152.
 */
package zombie.scripting.objects;

import zombie.UsedFromLua;
import zombie.scripting.ScriptType;
import zombie.scripting.objects.BaseScriptObject;
import zombie.scripting.objects.ScriptModule;
import zombie.scripting.objects.VehicleScript;

@UsedFromLua
public final class VehicleTemplate
extends BaseScriptObject {
    public String name;
    public String body;
    public VehicleScript script;

    public VehicleTemplate(ScriptModule module, String name, String body) {
        super(ScriptType.VehicleTemplate);
        this.setModule(module);
        this.name = name;
        this.body = body;
    }

    public VehicleScript getScript() {
        if (this.script == null) {
            this.script = new VehicleScript();
            this.script.setModule(this.getModule());
            try {
                this.script.Load(this.name, this.body);
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }
        return this.script;
    }
}

