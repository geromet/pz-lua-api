/*
 * Decompiled with CFR 0.152.
 */
package zombie.scripting.objects;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import zombie.UsedFromLua;
import zombie.core.Core;
import zombie.core.Translator;
import zombie.debug.DebugLog;
import zombie.entity.ComponentType;
import zombie.scripting.ScriptParser;
import zombie.scripting.ScriptType;
import zombie.scripting.objects.BaseScriptObject;
import zombie.scripting.objects.XuiColorsScript;
import zombie.util.StringUtils;

@UsedFromLua
public class XuiSkinScript
extends BaseScriptObject {
    private static final String protectedDefaultName = "default";
    private final ArrayList<String> imports = new ArrayList();
    private final EntityUiScript defaultEntityUiScript = new EntityUiScript();
    private final Map<String, EntityUiScript> entityUiScriptMap = new HashMap<String, EntityUiScript>();
    private final Map<String, StyleInfoScript> styleInfoMap = new HashMap<String, StyleInfoScript>();
    private final XuiColorsScript colorsScript = new XuiColorsScript();

    public XuiSkinScript() {
        super(ScriptType.XuiSkin);
    }

    public ArrayList<String> getImports() {
        return this.imports;
    }

    public EntityUiScript getDefaultEntityUiScript() {
        return this.defaultEntityUiScript;
    }

    public final Map<String, EntityUiScript> getEntityUiScriptMap() {
        return this.entityUiScriptMap;
    }

    public Map<String, StyleInfoScript> getStyleInfoMap() {
        return this.styleInfoMap;
    }

    public XuiColorsScript getColorsScript() {
        return this.colorsScript;
    }

    @Override
    public void reset() {
        this.imports.clear();
        this.defaultEntityUiScript.reset();
        this.entityUiScriptMap.clear();
        this.styleInfoMap.clear();
        this.colorsScript.getColorMap().clear();
    }

    @Override
    public void Load(String name, String body) throws Exception {
        ScriptParser.Block block = ScriptParser.parse(body);
        block = block.children.get(0);
        this.LoadCommonBlock(block);
        for (ScriptParser.Value value : block.values) {
            String key = value.getKey().trim();
            String val = value.getValue().trim();
            if (key.isEmpty() || val.isEmpty()) continue;
            DebugLog.General.warn("Unknown line in script: " + value.string);
        }
        for (ScriptParser.Block child : block.children) {
            if ("imports".equalsIgnoreCase(child.type)) {
                this.LoadImports(child);
                continue;
            }
            if ("entity".equalsIgnoreCase(child.type)) {
                this.LoadEntityBlock(child);
                continue;
            }
            if ("colors".equalsIgnoreCase(child.type)) {
                this.colorsScript.LoadColorsBlock(child);
                continue;
            }
            this.LoadStyleBlock(child);
        }
    }

    private void LoadStyleBlock(ScriptParser.Block block) throws Exception {
        Map<String, String> map;
        if (!this.styleInfoMap.containsKey(block.type)) {
            this.styleInfoMap.put(block.type, new StyleInfoScript());
        }
        StyleInfoScript styleInfo = this.styleInfoMap.get(block.type);
        if (StringUtils.isNullOrWhitespace(block.id)) {
            map = styleInfo.defaultStyleBlock;
        } else {
            if (block.id.equalsIgnoreCase(protectedDefaultName)) {
                throw new Exception("Default is protected and cannot be used as style name.");
            }
            if (block.id.contains(".")) {
                throw new Exception("Style name may not contain '.' (dot).");
            }
            if (!styleInfo.styleBlocks.containsKey(block.id)) {
                styleInfo.styleBlocks.put(block.id, new HashMap());
            }
            map = styleInfo.styleBlocks.get(block.id);
        }
        for (ScriptParser.Value value : block.values) {
            String key = value.getKey().trim();
            String val = value.getValue().trim();
            if (key.isEmpty()) continue;
            map.put(key, (val = StringUtils.trimSurroundingQuotes(val)).isEmpty() || val.equalsIgnoreCase("nil") || val.equalsIgnoreCase("null") ? null : val);
        }
    }

    private void LoadImports(ScriptParser.Block block) throws Exception {
        for (ScriptParser.Value value : block.values) {
            String s = value.string != null ? value.string.trim() : "";
            if (StringUtils.isNullOrWhitespace(s)) continue;
            if (s.equalsIgnoreCase(this.getScriptObjectName())) {
                DebugLog.General.warn("Cannot import self: " + this.getScriptObjectName());
                if (Core.debug) {
                    throw new Exception("Cannot import self: " + this.getScriptObjectName());
                }
            }
            this.imports.add(s);
        }
    }

    private void LoadEntityBlock(ScriptParser.Block block) throws Exception {
        EntityUiScript entityUiScript;
        if (StringUtils.isNullOrWhitespace(block.id)) {
            entityUiScript = this.defaultEntityUiScript;
        } else {
            if (block.id.equalsIgnoreCase(protectedDefaultName)) {
                throw new Exception("Default is protected and cannot be used as style name.");
            }
            entityUiScript = this.entityUiScriptMap.get(block.id);
            if (entityUiScript == null) {
                entityUiScript = new EntityUiScript();
                this.entityUiScriptMap.put(block.id, entityUiScript);
            }
        }
        for (ScriptParser.Value value : block.values) {
            String key = value.getKey().trim();
            String val = value.getValue().trim();
            if (key.isEmpty() || val.isEmpty()) continue;
            if (key.equalsIgnoreCase("luaWindowClass")) {
                entityUiScript.luaWindowClass = val;
                continue;
            }
            if (key.equalsIgnoreCase("xuiStyle")) {
                entityUiScript.xuiStyle = val;
                continue;
            }
            if (key.equalsIgnoreCase("luaCanOpenWindow")) {
                entityUiScript.luaCanOpenWindow = val;
                continue;
            }
            if (key.equalsIgnoreCase("luaOpenWindow")) {
                entityUiScript.luaOpenWindow = val;
                continue;
            }
            if (key.equalsIgnoreCase("displayName")) {
                String newVal = val.replace(" ", "");
                String translatedText = Translator.getRecipeName(newVal);
                if (!translatedText.equalsIgnoreCase(newVal)) {
                    val = translatedText;
                }
                entityUiScript.displayName = val;
                continue;
            }
            if (key.equalsIgnoreCase("description")) {
                entityUiScript.description = val;
                continue;
            }
            if (key.equalsIgnoreCase("buildDescription")) {
                entityUiScript.buildDescription = val;
                continue;
            }
            if (key.equalsIgnoreCase("icon")) {
                entityUiScript.iconPath = val;
                continue;
            }
            if (key.equalsIgnoreCase("clearComponents")) {
                entityUiScript.clearComponents = val.equalsIgnoreCase("true");
                continue;
            }
            DebugLog.General.warn("Unknown line in script: " + value.string);
        }
        for (ScriptParser.Block child : block.children) {
            if (!"components".equalsIgnoreCase(child.type)) continue;
            this.LoadComponents(child, entityUiScript);
        }
    }

    private void LoadComponents(ScriptParser.Block block, EntityUiScript entityUiScript) throws Exception {
        for (ScriptParser.Value value : block.values) {
            ComponentUiScript uiInfo;
            String key = value.getKey().trim();
            String val = value.getValue().trim();
            if (key.isEmpty() || val.isEmpty()) continue;
            ComponentType componentType = ComponentType.valueOf(key);
            String luaClass = val;
            String style = null;
            if (val.contains(":")) {
                String[] s = val.split(":");
                luaClass = s[0];
                style = s[1];
            }
            if ((uiInfo = entityUiScript.componentUiScriptMap.get((Object)componentType)) == null) {
                uiInfo = new ComponentUiScript();
                entityUiScript.componentUiScriptMap.put(componentType, uiInfo);
            }
            uiInfo.luaPanelClass = luaClass;
            if (style == null) continue;
            uiInfo.xuiStyle = style;
        }
        for (ScriptParser.Block child : block.children) {
            this.LoadComponentBlock(child, entityUiScript);
        }
    }

    private void LoadComponentBlock(ScriptParser.Block block, EntityUiScript entityUiScript) throws Exception {
        ComponentType componentType = ComponentType.valueOf(block.type);
        ComponentUiScript uiInfo = entityUiScript.componentUiScriptMap.get((Object)componentType);
        if (uiInfo == null) {
            uiInfo = new ComponentUiScript();
            entityUiScript.componentUiScriptMap.put(componentType, uiInfo);
        }
        for (ScriptParser.Value value : block.values) {
            String key = value.getKey().trim();
            String val = value.getValue().trim();
            if (key.isEmpty() || val.isEmpty()) continue;
            if (key.equalsIgnoreCase("luaPanelClass")) {
                uiInfo.luaPanelClass = val;
                continue;
            }
            if (key.equalsIgnoreCase("xuiStyle")) {
                uiInfo.xuiStyle = val;
                continue;
            }
            if (key.equalsIgnoreCase("displayName")) {
                uiInfo.displayName = val;
                continue;
            }
            if (key.equalsIgnoreCase("icon")) {
                uiInfo.iconPath = val;
                continue;
            }
            if (key.equalsIgnoreCase("listOrderZ")) {
                uiInfo.listOrderZ = Integer.parseInt(val);
                uiInfo.listOrderZset = true;
                continue;
            }
            if (key.equalsIgnoreCase("enabled")) {
                uiInfo.enabled = val.equalsIgnoreCase("true");
                uiInfo.enabledSet = true;
                continue;
            }
            DebugLog.General.warn("Unknown line in script: " + value.string);
        }
    }

    public static class EntityUiScript {
        private String luaWindowClass;
        private String xuiStyle;
        private String luaCanOpenWindow;
        private String luaOpenWindow;
        private String displayName;
        private String description;
        private String buildDescription;
        private String iconPath;
        private boolean clearComponents;
        private final Map<ComponentType, ComponentUiScript> componentUiScriptMap = new HashMap<ComponentType, ComponentUiScript>();

        public String getLuaWindowClass() {
            return this.luaWindowClass;
        }

        public String getXuiStyle() {
            return this.xuiStyle;
        }

        public String getLuaCanOpenWindow() {
            return this.luaCanOpenWindow;
        }

        public String getLuaOpenWindow() {
            return this.luaOpenWindow;
        }

        public String getDisplayName() {
            return this.displayName;
        }

        public String getDescription() {
            return this.description;
        }

        public String getBuildDescription() {
            return this.buildDescription;
        }

        public String getIconPath() {
            return this.iconPath;
        }

        public boolean isClearComponents() {
            return this.clearComponents;
        }

        public Map<ComponentType, ComponentUiScript> getComponentUiScriptMap() {
            return this.componentUiScriptMap;
        }

        protected void reset() {
            this.luaWindowClass = null;
            this.xuiStyle = null;
            this.luaCanOpenWindow = null;
            this.luaOpenWindow = null;
            this.displayName = null;
            this.description = null;
            this.buildDescription = null;
            this.iconPath = null;
            this.clearComponents = false;
            this.componentUiScriptMap.clear();
        }
    }

    public static class StyleInfoScript {
        private final HashMap<String, String> defaultStyleBlock = new HashMap();
        private final Map<String, HashMap<String, String>> styleBlocks = new HashMap<String, HashMap<String, String>>();

        public HashMap<String, String> getDefaultStyleBlock() {
            return this.defaultStyleBlock;
        }

        public Map<String, HashMap<String, String>> getStyleBlocks() {
            return this.styleBlocks;
        }
    }

    public static class ComponentUiScript {
        private String luaPanelClass;
        private String xuiStyle;
        private String displayName;
        private String iconPath;
        private boolean listOrderZset;
        private int listOrderZ;
        private boolean enabledSet;
        private boolean enabled = true;

        public String getLuaPanelClass() {
            return this.luaPanelClass;
        }

        public String getXuiStyle() {
            return this.xuiStyle;
        }

        public String getDisplayName() {
            return this.displayName;
        }

        public String getIconPath() {
            return this.iconPath;
        }

        public boolean isListOrderZ() {
            return this.listOrderZset;
        }

        public int getListOrderZ() {
            return this.listOrderZ;
        }

        public boolean isEnabledSet() {
            return this.enabledSet;
        }

        public boolean isEnabled() {
            return this.enabled;
        }
    }
}

