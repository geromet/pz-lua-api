/*
 * Decompiled with CFR 0.152.
 */
package zombie.scripting.entity.components.contextmenuconfig;

import java.util.ArrayList;
import zombie.UsedFromLua;
import zombie.debug.DebugLog;
import zombie.debug.objects.DebugClassFields;
import zombie.entity.ComponentType;
import zombie.scripting.ScriptParser;
import zombie.scripting.entity.ComponentScript;

@DebugClassFields
@UsedFromLua
public class ContextMenuConfigScript
extends ComponentScript {
    protected ArrayList<EntryScript> entries = new ArrayList();

    private ContextMenuConfigScript() {
        super(ComponentType.ContextMenuConfig);
    }

    protected void copyFrom(ComponentScript other) {
    }

    @Override
    protected void load(ScriptParser.Block block) throws Exception {
        super.load(block);
        for (ScriptParser.Value value : block.values) {
            String key = value.getKey().trim();
            String val = value.getValue().trim();
            if (!key.isEmpty() && !val.isEmpty()) continue;
        }
        for (ScriptParser.Block child : block.children) {
            if (child.type.equalsIgnoreCase("contextEntry")) {
                EntryScript entry = this.LoadEntry(child);
                this.entries.add(entry);
                continue;
            }
            DebugLog.General.error("Unknown block '" + child.type + "' in entity script: " + this.getName());
        }
    }

    private EntryScript LoadEntry(ScriptParser.Block block) {
        EntryScript script = new EntryScript();
        for (ScriptParser.Value value : block.values) {
            String k = value.getKey().trim();
            String v = value.getValue().trim();
            if (k.equalsIgnoreCase("menu")) {
                script.menu = v;
                continue;
            }
            if (k.equalsIgnoreCase("icon")) {
                script.icon = v;
                continue;
            }
            if (k.equalsIgnoreCase("customSubmenu")) {
                script.customSubmenu = v;
                continue;
            }
            if (k.equalsIgnoreCase("customFunction")) {
                script.customFunction = v;
                continue;
            }
            if (k.equalsIgnoreCase("openWindow")) {
                script.openWindow = v;
                continue;
            }
            if (k.equalsIgnoreCase("timedAction")) {
                script.timedAction = v;
                continue;
            }
            if (k.equalsIgnoreCase("extraParam")) {
                script.extraParam = v;
                continue;
            }
            if (k.equalsIgnoreCase("allowDistance")) {
                script.allowDistance = v.equalsIgnoreCase("true");
                continue;
            }
            if (!k.equalsIgnoreCase("time")) continue;
            script.time = Integer.parseInt(v);
        }
        return script;
    }

    public ArrayList<EntryScript> getEntries() {
        return this.entries;
    }

    @DebugClassFields
    @UsedFromLua
    public static class EntryScript {
        private String menu;
        private String icon;
        private String timedAction;
        private String openWindow;
        private String customSubmenu;
        private String customFunction;
        private String extraParam;
        private boolean allowDistance;
        private int time = 5;

        public String getMenu() {
            return this.menu;
        }

        public String getIcon() {
            return this.icon;
        }

        public String getTimedAction() {
            return this.timedAction;
        }

        public String getOpenWindow() {
            return this.openWindow;
        }

        public String getCustomSubmenu() {
            return this.customSubmenu;
        }

        public String getCustomFunction() {
            return this.customFunction;
        }

        public String getExtraParam() {
            return this.extraParam;
        }

        public boolean getAllowDistance() {
            return this.allowDistance;
        }

        public int getTime() {
            return this.time;
        }
    }
}

