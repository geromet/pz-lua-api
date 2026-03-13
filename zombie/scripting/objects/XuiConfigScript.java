/*
 * Decompiled with CFR 0.152.
 */
package zombie.scripting.objects;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import zombie.UsedFromLua;
import zombie.debug.DebugLog;
import zombie.scripting.ScriptParser;
import zombie.scripting.ScriptType;
import zombie.scripting.objects.BaseScriptObject;
import zombie.scripting.ui.XuiLuaStyle;
import zombie.scripting.ui.XuiVarType;
import zombie.util.StringUtils;

@UsedFromLua
public class XuiConfigScript
extends BaseScriptObject {
    private final Map<XuiVarType, ArrayList<String>> varConfigs = new HashMap<XuiVarType, ArrayList<String>>();

    public XuiConfigScript() {
        super(ScriptType.XuiConfig);
        for (XuiVarType varType : XuiLuaStyle.ALLOWED_VAR_TYPES) {
            this.varConfigs.put(varType, new ArrayList());
        }
    }

    public Map<XuiVarType, ArrayList<String>> getVarConfigs() {
        return this.varConfigs;
    }

    @Override
    public void reset() {
        for (Map.Entry<XuiVarType, ArrayList<String>> entry : this.varConfigs.entrySet()) {
            entry.getValue().clear();
        }
    }

    @Override
    public void Load(String name, String body) throws Exception {
        ScriptParser.Block block = ScriptParser.parse(body);
        block = block.children.get(0);
        this.LoadCommonBlock(block);
        for (ScriptParser.Block child : block.children) {
            XuiVarType varType = XuiVarType.valueOf(child.type);
            if (!XuiLuaStyle.ALLOWED_VAR_TYPES.contains((Object)varType)) {
                throw new Exception("VarType not allowed: " + String.valueOf((Object)varType));
            }
            this.LoadVarTypeBlock(varType, child);
        }
    }

    private void LoadVarTypeBlock(XuiVarType varType, ScriptParser.Block block) throws Exception {
        for (ScriptParser.Value value : block.values) {
            if (StringUtils.isNullOrWhitespace(value.string)) continue;
            String key = value.string.trim();
            if (!this.otherTypesContainsKey(key, varType)) {
                ArrayList<String> list = this.varConfigs.get((Object)varType);
                if (list.contains(key)) continue;
                list.add(key);
                continue;
            }
            throw new Exception("Key '" + key + "' duplicate from another value block. this var type = " + String.valueOf((Object)varType));
        }
    }

    private boolean otherTypesContainsKey(String key, XuiVarType ignoreType) {
        for (Map.Entry<XuiVarType, ArrayList<String>> entry : this.varConfigs.entrySet()) {
            if (entry.getKey() == ignoreType || !entry.getValue().contains(key)) continue;
            DebugLog.General.warn("Duplicate key '" + key + "' in var type: " + String.valueOf((Object)entry.getKey()));
            return true;
        }
        return false;
    }
}

