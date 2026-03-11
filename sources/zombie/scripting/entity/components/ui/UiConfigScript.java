/*
 * Decompiled with CFR 0.152.
 */
package zombie.scripting.entity.components.ui;

import zombie.UsedFromLua;
import zombie.debug.DebugLog;
import zombie.entity.ComponentType;
import zombie.entity.GameEntity;
import zombie.scripting.ScriptParser;
import zombie.scripting.entity.ComponentScript;
import zombie.scripting.ui.XuiManager;
import zombie.scripting.ui.XuiSkin;

@UsedFromLua
public class UiConfigScript
extends ComponentScript {
    private String xuiSkinName;
    private String entityStyle;
    private boolean uiEnabled = true;

    private UiConfigScript() {
        super(ComponentType.UiConfig);
    }

    public String getXuiSkinName() {
        return this.xuiSkinName;
    }

    public String getEntityStyle() {
        return this.entityStyle;
    }

    public boolean isUiEnabled() {
        return this.uiEnabled;
    }

    public String getDisplayNameDebug() {
        XuiSkin skin = XuiManager.GetSkin(this.xuiSkinName);
        if (skin != null) {
            return skin.getEntityDisplayName(this.entityStyle);
        }
        return GameEntity.getDefaultEntityDisplayName();
    }

    protected void copyFrom(ComponentScript other) {
    }

    @Override
    public void PreReload() {
        this.xuiSkinName = null;
        this.entityStyle = null;
        this.uiEnabled = true;
    }

    @Override
    protected void load(ScriptParser.Block block) throws Exception {
        for (ScriptParser.Value value : block.values) {
            String key = value.getKey().trim();
            String val = value.getValue().trim();
            if (key.isEmpty() || val.isEmpty()) continue;
            if (key.equalsIgnoreCase("xuiSkin")) {
                this.xuiSkinName = val;
                continue;
            }
            if (key.equalsIgnoreCase("entityStyle")) {
                this.entityStyle = val;
                continue;
            }
            if (!key.equalsIgnoreCase("uiEnabled")) continue;
            this.uiEnabled = Boolean.parseBoolean(val);
        }
        for (ScriptParser.Block child : block.children) {
            if (child.type.equalsIgnoreCase("someType")) continue;
            DebugLog.General.error("Unknown block '" + child.type + "' in entity script: " + this.getName());
        }
    }
}

