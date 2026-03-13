/*
 * Decompiled with CFR 0.152.
 */
package zombie.scripting.entity.components.attributes;

import java.util.HashMap;
import java.util.Map;
import zombie.UsedFromLua;
import zombie.core.Core;
import zombie.debug.DebugLog;
import zombie.debug.objects.DebugClassFields;
import zombie.entity.ComponentType;
import zombie.entity.components.attributes.Attribute;
import zombie.entity.components.attributes.AttributeContainer;
import zombie.entity.components.attributes.AttributeType;
import zombie.scripting.ScriptLoadMode;
import zombie.scripting.ScriptParser;
import zombie.scripting.entity.ComponentScript;

@DebugClassFields
@UsedFromLua
public class AttributesScript
extends ComponentScript {
    private final HashMap<String, String> kvPairs = new HashMap();
    private AttributeContainer container;
    private boolean hasCreatedContainer;

    private AttributesScript() {
        super(ComponentType.Attributes);
    }

    @Override
    public void PreReload() {
        this.kvPairs.clear();
        this.container = null;
        this.hasCreatedContainer = false;
    }

    @Override
    public void OnScriptsLoaded(ScriptLoadMode loadMode) throws Exception {
        super.OnScriptsLoaded(loadMode);
        this.createTemplateContainer();
    }

    private void createTemplateContainer() {
        if (this.hasCreatedContainer) {
            return;
        }
        if (!this.kvPairs.isEmpty()) {
            try {
                this.container = (AttributeContainer)ComponentType.Attributes.CreateComponent();
                for (Map.Entry<String, String> entry : this.kvPairs.entrySet()) {
                    AttributeType attributeType = Attribute.TypeFromName(entry.getKey());
                    if (attributeType != null && this.container.putFromScript(attributeType, entry.getValue())) continue;
                    if (Core.debug) {
                        throw new RuntimeException("Attribute '" + String.valueOf(attributeType) + "' could not be added.");
                    }
                    DebugLog.General.error("WARNING: Item - > Attribute '" + String.valueOf(attributeType) + "' could not be added.");
                }
            }
            catch (Exception e) {
                e.printStackTrace();
                this.container = null;
            }
        }
        this.hasCreatedContainer = true;
    }

    public AttributeContainer getTemplateContainer() {
        this.createTemplateContainer();
        return this.container;
    }

    protected void copyFrom(ComponentScript componentScript) {
        AttributesScript other = (AttributesScript)componentScript;
        this.kvPairs.putAll(other.kvPairs);
    }

    @Override
    protected void load(ScriptParser.Block block) throws Exception {
        super.load(block);
        for (ScriptParser.Value value : block.values) {
            String k = value.getKey().trim();
            String v = value.getValue().trim();
            this.parseKeyValue(k, v);
        }
    }

    @Override
    protected boolean parseKeyValue(String k, String v) {
        AttributeType attributeType = Attribute.TypeFromName(k);
        if (attributeType != null) {
            this.kvPairs.put(k, v);
            return true;
        }
        DebugLog.General.error("Unknown attribute, key = " + k + ", value = " + v);
        return false;
    }
}

