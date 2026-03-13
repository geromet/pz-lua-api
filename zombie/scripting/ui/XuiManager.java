/*
 * Decompiled with CFR 0.152.
 */
package zombie.scripting.ui;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import zombie.UsedFromLua;
import zombie.core.Color;
import zombie.core.Colors;
import zombie.debug.DebugLog;
import zombie.scripting.ScriptManager;
import zombie.scripting.ScriptType;
import zombie.scripting.objects.XuiColorsScript;
import zombie.scripting.objects.XuiConfigScript;
import zombie.scripting.objects.XuiLayoutScript;
import zombie.scripting.objects.XuiSkinScript;
import zombie.scripting.ui.XuiLuaStyle;
import zombie.scripting.ui.XuiScript;
import zombie.scripting.ui.XuiScriptType;
import zombie.scripting.ui.XuiSkin;

@UsedFromLua
public class XuiManager {
    private static final String DEFAULT_SKIN_NAME = "default";
    public static final EnumSet<ScriptType> XUI_SCRIPT_TYPES = EnumSet.of(ScriptType.XuiConfig, new ScriptType[]{ScriptType.XuiLayout, ScriptType.XuiStyle, ScriptType.XuiDefaultStyle, ScriptType.XuiColor, ScriptType.XuiSkin});
    private static final Map<String, XuiLayoutScript> layoutScriptsMap = new HashMap<String, XuiLayoutScript>();
    private static final Map<String, XuiLayoutScript> stylesScriptsMap = new HashMap<String, XuiLayoutScript>();
    private static final Map<String, XuiLayoutScript> defaultStylesScriptsMap = new HashMap<String, XuiLayoutScript>();
    private static final ArrayList<XuiScript> combinedList = new ArrayList();
    private static final ArrayList<XuiScript> xuiLayoutsList = new ArrayList();
    private static final ArrayList<XuiScript> xuiStylesList = new ArrayList();
    private static final ArrayList<XuiScript> xuiDefaultStylesList = new ArrayList();
    private static final Map<String, XuiScript> xuiLayouts = new HashMap<String, XuiScript>();
    private static final Map<String, XuiScript> xuiStyles = new HashMap<String, XuiScript>();
    private static final Map<String, XuiScript> xuiDefaultStyles = new HashMap<String, XuiScript>();
    private static final Map<String, XuiSkin> xuiSkins = new HashMap<String, XuiSkin>();
    private static XuiSkin xuiDefaultSkin;
    private static boolean parseOnce;
    private static boolean hasParsedOnce;

    public static String getDefaultSkinName() {
        return DEFAULT_SKIN_NAME;
    }

    public static ArrayList<XuiScript> GetCombinedScripts() {
        return combinedList;
    }

    public static ArrayList<XuiScript> GetAllLayouts() {
        return xuiLayoutsList;
    }

    public static ArrayList<XuiScript> GetAllStyles() {
        return xuiStylesList;
    }

    public static ArrayList<XuiScript> GetAllDefaultStyles() {
        return xuiDefaultStylesList;
    }

    public static XuiLayoutScript GetLayoutScript(String name) {
        if (name == null) {
            return null;
        }
        return layoutScriptsMap.get(name);
    }

    public static XuiLayoutScript GetStyleScript(String name) {
        if (name == null) {
            return null;
        }
        return stylesScriptsMap.get(name);
    }

    public static XuiLayoutScript GetDefaultStyleScript(String name) {
        if (name == null) {
            return null;
        }
        return defaultStylesScriptsMap.get(name);
    }

    public static XuiScript GetLayout(String name) {
        if (name == null) {
            return null;
        }
        return xuiLayouts.get(name);
    }

    public static XuiScript GetStyle(String style) {
        if (style == null) {
            return null;
        }
        return xuiStyles.get(style);
    }

    public static XuiScript GetDefaultStyle(String luaClass) {
        if (luaClass == null) {
            return null;
        }
        return xuiDefaultStyles.get(luaClass);
    }

    public static XuiSkin GetDefaultSkin() {
        return xuiDefaultSkin;
    }

    public static XuiSkin GetSkin(String name) {
        XuiSkin skin = xuiSkins.get(name);
        if (skin == null) {
            if (name != null) {
                DebugLog.General.warn("Skin not found: " + name);
            }
            skin = xuiDefaultSkin;
        }
        return skin;
    }

    private static void reset() {
        layoutScriptsMap.clear();
        stylesScriptsMap.clear();
        defaultStylesScriptsMap.clear();
        combinedList.clear();
        xuiLayoutsList.clear();
        xuiStylesList.clear();
        xuiDefaultStylesList.clear();
        xuiLayouts.clear();
        xuiStyles.clear();
        xuiDefaultStyles.clear();
        for (XuiSkin skin : xuiSkins.values()) {
            skin.setInvalidated(true);
        }
        xuiSkins.clear();
        xuiDefaultSkin = null;
        XuiLuaStyle.Reset();
    }

    public static void setParseOnce(boolean b) {
        parseOnce = b;
        hasParsedOnce = false;
    }

    public static void ParseScripts() throws Exception {
        if (parseOnce && hasParsedOnce) {
            return;
        }
        hasParsedOnce = true;
        XuiManager.reset();
        ArrayList<XuiConfigScript> configs = ScriptManager.instance.getAllXuiConfigScripts();
        XuiLuaStyle.ReadConfigs(configs);
        ArrayList<XuiColorsScript> colors = ScriptManager.instance.getAllXuiColors();
        ArrayList<XuiLayoutScript> styles = ScriptManager.instance.getAllXuiStyles();
        ArrayList<XuiLayoutScript> defaultStyles = ScriptManager.instance.getAllXuiDefaultStyles();
        ArrayList<XuiLayoutScript> layouts = ScriptManager.instance.getAllXuiLayouts();
        ArrayList<XuiSkinScript> skins = ScriptManager.instance.getAllXuiSkinScripts();
        for (XuiColorsScript xuiColorsScript : colors) {
            for (Map.Entry<String, Color> entry : xuiColorsScript.getColorMap().entrySet()) {
                Colors.ColNfo nfo;
                if (Colors.GetColorInfo(entry.getKey()) != null && (nfo = Colors.GetColorInfo(entry.getKey())).getColorSet() == Colors.ColorSet.Game) {
                    nfo.getColor().set(entry.getValue());
                    continue;
                }
                if (Colors.GetColorByName(entry.getKey()) == null) {
                    Colors.AddGameColor(entry.getKey(), entry.getValue());
                    continue;
                }
                DebugLog.General.error("Color '" + entry.getKey() + "' is already defined in Colors.java");
            }
        }
        for (XuiLayoutScript xuiLayoutScript : defaultStyles) {
            if (xuiLayoutScript.getName() != null) {
                defaultStylesScriptsMap.put(xuiLayoutScript.getName(), xuiLayoutScript);
            }
            XuiManager.registerLayout(xuiLayoutScript);
        }
        for (XuiLayoutScript xuiLayoutScript : styles) {
            if (xuiLayoutScript.getName() != null) {
                stylesScriptsMap.put(xuiLayoutScript.getName(), xuiLayoutScript);
            }
            XuiManager.registerLayout(xuiLayoutScript);
        }
        for (XuiLayoutScript xuiLayoutScript : layouts) {
            if (xuiLayoutScript.getName() != null) {
                layoutScriptsMap.put(xuiLayoutScript.getName(), xuiLayoutScript);
            }
            XuiManager.registerLayout(xuiLayoutScript);
        }
        for (XuiLayoutScript xuiLayoutScript : defaultStyles) {
            XuiManager.parseLayout(xuiLayoutScript);
        }
        for (XuiLayoutScript xuiLayoutScript : styles) {
            XuiManager.parseLayout(xuiLayoutScript);
        }
        for (XuiLayoutScript xuiLayoutScript : layouts) {
            XuiManager.parseLayout(xuiLayoutScript);
        }
        combinedList.addAll(xuiLayoutsList);
        combinedList.addAll(xuiStylesList);
        combinedList.addAll(xuiDefaultStylesList);
        for (XuiSkinScript xuiSkinScript : skins) {
            String name = xuiSkinScript.getScriptObjectName();
            if (!xuiSkinScript.getModule().getName().equals("Base")) {
                DebugLog.General.warn("XuiSkin '" + xuiSkinScript.getScriptObjectFullType() + "' ignored, skin needs to be module Base.");
                continue;
            }
            XuiSkin skin = new XuiSkin(name, xuiSkinScript);
            xuiSkins.put(name, skin);
        }
        for (XuiSkin xuiSkin : xuiSkins.values()) {
            xuiSkin.Load();
        }
        xuiDefaultSkin = XuiManager.GetSkin(DEFAULT_SKIN_NAME);
    }

    private static void registerLayout(XuiLayoutScript layoutScript) {
        try {
            layoutScript.preParse();
            XuiScript script = layoutScript.getXuiScript();
            if (script != null) {
                if (script.getScriptType() == XuiScriptType.Layout) {
                    xuiLayoutsList.add(script);
                    xuiLayouts.put(layoutScript.getName(), script);
                } else if (script.getScriptType() == XuiScriptType.Style) {
                    xuiStylesList.add(script);
                    xuiStyles.put(layoutScript.getName(), script);
                } else if (script.getScriptType() == XuiScriptType.DefaultStyle) {
                    xuiDefaultStylesList.add(script);
                    xuiDefaultStyles.put(layoutScript.getName(), script);
                }
            } else {
                DebugLog.General.error("No XuiScript in XuiConfig: " + layoutScript.getName());
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void parseLayout(XuiLayoutScript layoutScript) {
        try {
            layoutScript.parseScript();
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }
}

