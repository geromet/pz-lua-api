/*
 * Decompiled with CFR 0.152.
 */
package zombie.scripting.ui;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import zombie.UsedFromLua;
import zombie.core.Color;
import zombie.core.Colors;
import zombie.core.Core;
import zombie.core.Translator;
import zombie.core.textures.Texture;
import zombie.debug.DebugLog;
import zombie.scripting.objects.XuiConfigScript;
import zombie.scripting.ui.XuiAutoApply;
import zombie.scripting.ui.XuiSkin;
import zombie.scripting.ui.XuiVarType;
import zombie.ui.UIFont;

@UsedFromLua
public class XuiLuaStyle {
    public static final EnumSet<XuiVarType> ALLOWED_VAR_TYPES = EnumSet.of(XuiVarType.String, new XuiVarType[]{XuiVarType.StringList, XuiVarType.TranslateString, XuiVarType.Double, XuiVarType.Boolean, XuiVarType.FontType, XuiVarType.Color, XuiVarType.Texture});
    private static final Map<String, XuiVar<?, ?>> varRegisteryMap = new HashMap();
    private final String xuiLuaClass;
    private final String xuiStyleName;
    protected XuiSkin xuiSkin;
    protected HashMap<String, XuiVar<?, ?>> varsMap = new HashMap();
    protected ArrayList<XuiVar<?, ?>> vars = new ArrayList();

    private static void addStaticVar(XuiVar<?, ?> var) {
        if (var == null) {
            throw new RuntimeException("Var is null");
        }
        if (var.getLuaTableKey() == null) {
            throw new RuntimeException("Var key is null");
        }
        if (varRegisteryMap.containsKey(var.getLuaTableKey())) {
            throw new RuntimeException("Key already exists: " + var.getLuaTableKey());
        }
        varRegisteryMap.put(var.getLuaTableKey(), var);
    }

    private static XuiVar<?, ?> getStaticVar(String name) {
        return varRegisteryMap.get(name);
    }

    public static void ReadConfigs(ArrayList<XuiConfigScript> configs) throws Exception {
        HashMap<XuiVarType, HashSet<String>> parsed = new HashMap<XuiVarType, HashSet<String>>();
        for (XuiVarType varType : ALLOWED_VAR_TYPES) {
            parsed.put(varType, new HashSet());
        }
        for (XuiConfigScript config : configs) {
            XuiLuaStyle.parseConfig(parsed, config);
        }
    }

    private static void parseConfig(Map<XuiVarType, HashSet<String>> parsed, XuiConfigScript config) throws Exception {
        Map<XuiVarType, ArrayList<String>> varConfigs = config.getVarConfigs();
        for (Map.Entry<XuiVarType, ArrayList<String>> entry : varConfigs.entrySet()) {
            XuiVarType varType = entry.getKey();
            if (!ALLOWED_VAR_TYPES.contains((Object)varType)) {
                throw new Exception("Var type not allowed: " + String.valueOf((Object)varType));
            }
            block11: for (String key : entry.getValue()) {
                if (XuiLuaStyle.otherTypesContainsKey(parsed, key, varType)) {
                    throw new Exception("Duplicate key '" + key + "' in var type: " + String.valueOf((Object)entry.getKey()) + ", and type: " + String.valueOf((Object)varType));
                }
                parsed.get((Object)varType).add(key);
                if (varRegisteryMap.containsKey(key)) continue;
                switch (varType) {
                    case String: {
                        XuiLuaStyle.addStaticVar(new XuiString(null, key));
                        continue block11;
                    }
                    case StringList: {
                        XuiLuaStyle.addStaticVar(new XuiStringList(null, key));
                        continue block11;
                    }
                    case TranslateString: {
                        XuiLuaStyle.addStaticVar(new XuiTranslateString(null, key));
                        continue block11;
                    }
                    case Double: {
                        XuiLuaStyle.addStaticVar(new XuiDouble(null, key));
                        continue block11;
                    }
                    case Boolean: {
                        XuiLuaStyle.addStaticVar(new XuiBoolean(null, key));
                        continue block11;
                    }
                    case FontType: {
                        XuiLuaStyle.addStaticVar(new XuiFontType(null, key));
                        continue block11;
                    }
                    case Color: {
                        XuiLuaStyle.addStaticVar(new XuiColor(null, key));
                        continue block11;
                    }
                    case Texture: {
                        XuiLuaStyle.addStaticVar(new XuiTexture(null, key));
                        continue block11;
                    }
                }
                throw new Exception("No handler for: " + String.valueOf((Object)varType));
            }
        }
    }

    private static boolean otherTypesContainsKey(Map<XuiVarType, HashSet<String>> parsed, String key, XuiVarType ignoreType) throws Exception {
        for (Map.Entry<XuiVarType, HashSet<String>> entry : parsed.entrySet()) {
            if (entry.getKey() == ignoreType || !entry.getValue().contains(key)) continue;
            return true;
        }
        return false;
    }

    public static void Reset() {
        varRegisteryMap.clear();
    }

    protected XuiLuaStyle(String xuiLuaClass, String xuiStyleName) {
        this.xuiLuaClass = xuiLuaClass;
        this.xuiStyleName = xuiStyleName;
    }

    public String getXuiLuaClass() {
        return this.xuiLuaClass;
    }

    public String getXuiStyleName() {
        return this.xuiStyleName;
    }

    public XuiVar<?, ?> getVar(String key) {
        return this.varsMap.get(key);
    }

    private void addVar(String key, XuiVar<?, ?> var) {
        if (key == null) {
            throw new RuntimeException("Key is null");
        }
        if (var == null) {
            throw new RuntimeException("Var is null");
        }
        if (var.getLuaTableKey() == null) {
            throw new RuntimeException("Var key is null");
        }
        if (this.varsMap.containsKey(var.getLuaTableKey()) || this.vars.contains(var)) {
            throw new RuntimeException("Var already added: " + var.getLuaTableKey());
        }
        this.varsMap.put(key, var);
        this.vars.add(var);
    }

    public ArrayList<XuiVar<?, ?>> getVars() {
        return this.vars;
    }

    public boolean loadVar(String key, String val) throws Exception {
        XuiVar<?, ?> var = this.varsMap.get(key);
        if (var == null) {
            XuiVar<?, ?> registered = varRegisteryMap.get(key);
            if (registered == null || !registered.acceptsKey(key)) {
                this.logInfo();
                throw new Exception("Variable '" + key + "' is not registered or key typo. [registered=" + String.valueOf(registered) + "]");
            }
            var = registered.copy(this);
            this.addVar(key, var);
        }
        if (val != null && var.acceptsKey(key)) {
            return var.load(key, val);
        }
        if (val == null && var.acceptsKey(key)) {
            var.setValue(null);
            return true;
        }
        return false;
    }

    public void copyVarsFrom(XuiLuaStyle other) {
        this.vars.clear();
        this.varsMap.clear();
        for (int i = 0; i < other.vars.size(); ++i) {
            XuiVar<?, ?> var = other.vars.get(i);
            XuiVar<?, ?> copy = var.copy(this);
            this.addVar(copy.getLuaTableKey(), copy);
        }
    }

    public String toString() {
        String orig = super.toString();
        return "XuiLuaStyle [class=" + this.xuiLuaClass + ", styleName=" + this.xuiStyleName + ",  u=" + orig + "]";
    }

    protected void logWithInfo(String s) {
        DebugLog.General.debugln(s);
        this.logInfo();
    }

    protected void warnWithInfo(String s) {
        DebugLog.General.debugln(s);
        this.logInfo();
    }

    protected void errorWithInfo(String s) {
        DebugLog.General.error(s);
        this.logInfo();
    }

    private void logInfo() {
        DebugLog.log(this.toString());
    }

    protected void debugPrint(String prefix) {
        for (XuiVar<?, ?> var : this.vars) {
            DebugLog.log(prefix + "-> " + var.getLuaTableKey() + " = " + var.getValueString());
        }
    }

    @UsedFromLua
    public static abstract class XuiVar<T, C extends XuiVar<?, ?>> {
        private int uiOrder = 1000;
        protected final XuiVarType type;
        protected final XuiLuaStyle parent;
        protected boolean valueSet;
        protected XuiAutoApply autoApply = XuiAutoApply.IfSet;
        protected T defaultValue;
        protected T value;
        protected final String luaTableKey;

        protected XuiVar(XuiVarType type, XuiLuaStyle parent, String key) {
            this(type, parent, key, null);
        }

        protected XuiVar(XuiVarType type, XuiLuaStyle parent, String key, T defaultVal) {
            this.type = Objects.requireNonNull(type);
            this.parent = parent;
            this.luaTableKey = Objects.requireNonNull(key);
            this.defaultValue = defaultVal;
        }

        protected abstract XuiVar<T, C> copy(XuiLuaStyle var1);

        protected XuiVar<T, C> copyValuesTo(XuiVar<T, C> c) {
            c.uiOrder = this.uiOrder;
            c.valueSet = this.valueSet;
            c.autoApply = this.autoApply;
            c.defaultValue = this.defaultValue;
            c.value = this.value;
            return c;
        }

        public XuiVarType getType() {
            return this.type;
        }

        public int setUiOrder(int order) {
            this.uiOrder = order;
            return this.uiOrder;
        }

        public int getUiOrder() {
            return this.uiOrder;
        }

        protected void setDefaultValue(T value) {
            this.defaultValue = value;
        }

        protected T getDefaultValue() {
            return this.defaultValue;
        }

        public void setValue(T value) {
            this.value = value;
            this.valueSet = true;
        }

        public void setAutoApplyMode(XuiAutoApply autoApplyMode) {
            this.autoApply = autoApplyMode;
        }

        public XuiAutoApply getAutoApplyMode() {
            return this.autoApply;
        }

        public String getLuaTableKey() {
            return this.luaTableKey;
        }

        protected String getScriptKey() {
            return this.luaTableKey;
        }

        public boolean isValueSet() {
            return this.valueSet;
        }

        public T value() {
            return this.valueSet ? this.value : this.defaultValue;
        }

        public String getValueString() {
            return this.value() != null ? this.value().toString() : "null";
        }

        protected boolean acceptsKey(String key) {
            return this.luaTableKey.equals(key);
        }

        protected abstract void fromString(String var1);

        protected boolean load(String key, String val) {
            if (this.acceptsKey(key)) {
                this.fromString(val);
                return true;
            }
            return false;
        }
    }

    @UsedFromLua
    public static class XuiString
    extends XuiVar<String, XuiString> {
        protected XuiString(XuiLuaStyle parent, String key) {
            super(XuiVarType.String, parent, key);
        }

        protected XuiString(XuiLuaStyle parent, String key, String defaultVal) {
            super(XuiVarType.String, parent, key, defaultVal);
        }

        @Override
        protected void fromString(String val) {
            this.setValue(val);
        }

        protected XuiString copy(XuiLuaStyle parent) {
            XuiString c = new XuiString(parent, this.luaTableKey, (String)this.defaultValue);
            this.copyValuesTo(c);
            return c;
        }
    }

    @UsedFromLua
    public static class XuiStringList
    extends XuiVar<ArrayList<String>, XuiStringList> {
        protected XuiStringList(XuiLuaStyle parent, String key) {
            super(XuiVarType.StringList, parent, key, new ArrayList());
        }

        protected XuiStringList(XuiLuaStyle parent, String key, ArrayList<String> defaultVal) {
            super(XuiVarType.StringList, parent, key, defaultVal);
        }

        @Override
        protected void fromString(String val) {
            try {
                String[] split = val.split(":");
                ArrayList<String> list = new ArrayList<String>(split.length);
                for (int i = 0; i < split.length; ++i) {
                    list.add(split[i].trim());
                }
                this.setValue(list);
            }
            catch (Exception e) {
                this.parent.logInfo();
                e.printStackTrace();
            }
        }

        protected XuiStringList copy(XuiLuaStyle parent) {
            XuiStringList c = new XuiStringList(parent, this.luaTableKey, (ArrayList)this.defaultValue);
            this.copyValuesTo(c);
            return c;
        }
    }

    @UsedFromLua
    public static class XuiTranslateString
    extends XuiVar<String, XuiTranslateString> {
        protected XuiTranslateString(XuiLuaStyle parent, String key) {
            super(XuiVarType.TranslateString, parent, key);
        }

        protected XuiTranslateString(XuiLuaStyle parent, String key, String defaultVal) {
            super(XuiVarType.TranslateString, parent, key, defaultVal);
        }

        @Override
        public String value() {
            if (super.value() == null) {
                return null;
            }
            return Translator.getText((String)super.value());
        }

        @Override
        protected void fromString(String val) {
            this.setValue(val);
        }

        @Override
        public String getValueString() {
            return super.value() != null ? (String)super.value() : "null";
        }

        protected XuiTranslateString copy(XuiLuaStyle parent) {
            XuiTranslateString c = new XuiTranslateString(parent, this.luaTableKey, (String)this.defaultValue);
            this.copyValuesTo(c);
            return c;
        }
    }

    @UsedFromLua
    public static class XuiDouble
    extends XuiVar<Double, XuiDouble> {
        protected XuiDouble(XuiLuaStyle parent, String key) {
            super(XuiVarType.Double, parent, key, 0.0);
        }

        protected XuiDouble(XuiLuaStyle parent, String key, double defaultVal) {
            super(XuiVarType.Double, parent, key, defaultVal);
        }

        @Override
        protected void fromString(String val) {
            try {
                this.setValue(Double.parseDouble(val));
            }
            catch (Exception e) {
                this.parent.logInfo();
                e.printStackTrace();
            }
        }

        protected XuiDouble copy(XuiLuaStyle parent) {
            XuiDouble c = new XuiDouble(parent, this.luaTableKey, (Double)this.defaultValue);
            this.copyValuesTo(c);
            return c;
        }
    }

    @UsedFromLua
    public static class XuiBoolean
    extends XuiVar<Boolean, XuiBoolean> {
        protected XuiBoolean(XuiLuaStyle parent, String key) {
            super(XuiVarType.Boolean, parent, key, false);
        }

        protected XuiBoolean(XuiLuaStyle parent, String key, boolean defaultVal) {
            super(XuiVarType.Boolean, parent, key, defaultVal);
        }

        @Override
        protected void fromString(String val) {
            try {
                this.setValue(Boolean.parseBoolean(val));
            }
            catch (Exception e) {
                this.parent.logInfo();
                e.printStackTrace();
            }
        }

        protected XuiBoolean copy(XuiLuaStyle parent) {
            XuiBoolean c = new XuiBoolean(parent, this.luaTableKey, (Boolean)this.defaultValue);
            this.copyValuesTo(c);
            return c;
        }
    }

    @UsedFromLua
    public static class XuiFontType
    extends XuiVar<UIFont, XuiFontType> {
        protected XuiFontType(XuiLuaStyle parent, String key) {
            super(XuiVarType.FontType, parent, key, UIFont.Small);
        }

        protected XuiFontType(XuiLuaStyle parent, String key, UIFont defaultVal) {
            super(XuiVarType.FontType, parent, key, defaultVal);
        }

        @Override
        protected void fromString(String val) {
            try {
                if (val.startsWith("UIFont.")) {
                    val = val.substring(val.indexOf(".") + 1);
                }
                this.setValue(UIFont.valueOf(val));
            }
            catch (Exception e) {
                this.parent.logInfo();
                e.printStackTrace();
            }
        }

        protected XuiFontType copy(XuiLuaStyle parent) {
            XuiFontType c = new XuiFontType(parent, this.luaTableKey, (UIFont)((Object)this.defaultValue));
            this.copyValuesTo(c);
            return c;
        }
    }

    @UsedFromLua
    public static class XuiColor
    extends XuiVar<Color, XuiColor> {
        protected XuiColor(XuiLuaStyle parent, String key) {
            super(XuiVarType.Color, parent, key);
        }

        protected XuiColor(XuiLuaStyle parent, String key, Color defaultVal) {
            super(XuiVarType.Color, parent, key, defaultVal);
        }

        @Override
        protected void fromString(String val) {
            try {
                Color color = null;
                if (this.parent.xuiSkin != null) {
                    color = this.parent.xuiSkin.color(val);
                }
                if (color == null) {
                    color = Colors.GetColorByName(val);
                }
                if (color == null && val.contains(":")) {
                    color = new Color();
                    String[] split = val.split(":");
                    if (split.length < 3) {
                        this.parent.errorWithInfo("Warning color has <3 values. color: " + val);
                    }
                    if (split.length > 1 && split[0].trim().equalsIgnoreCase("rgb")) {
                        block14: for (int i = 1; i < split.length; ++i) {
                            switch (i) {
                                case 1: {
                                    color.r = Float.parseFloat(split[i].trim()) / 255.0f;
                                    continue block14;
                                }
                                case 2: {
                                    color.g = Float.parseFloat(split[i].trim()) / 255.0f;
                                    continue block14;
                                }
                                case 3: {
                                    color.b = Float.parseFloat(split[i].trim()) / 255.0f;
                                    continue block14;
                                }
                                case 4: {
                                    color.a = Float.parseFloat(split[i].trim()) / 255.0f;
                                }
                            }
                        }
                    } else {
                        block15: for (int i = 0; i < split.length; ++i) {
                            switch (i) {
                                case 0: {
                                    color.r = Float.parseFloat(split[i].trim());
                                    continue block15;
                                }
                                case 1: {
                                    color.g = Float.parseFloat(split[i].trim());
                                    continue block15;
                                }
                                case 2: {
                                    color.b = Float.parseFloat(split[i].trim());
                                    continue block15;
                                }
                                case 3: {
                                    color.a = Float.parseFloat(split[i].trim());
                                }
                            }
                        }
                    }
                }
                if (color == null) {
                    throw new Exception("Could not read color: " + val);
                }
                this.setValue(color);
            }
            catch (Exception e) {
                if (Core.debug) {
                    this.parent.logInfo();
                    e.printStackTrace();
                }
                DebugLog.General.warn("Could not read color: " + val);
            }
        }

        public float getR() {
            return this.value() != null ? ((Color)this.value()).r : 1.0f;
        }

        public float getG() {
            return this.value() != null ? ((Color)this.value()).g : 1.0f;
        }

        public float getB() {
            return this.value() != null ? ((Color)this.value()).b : 1.0f;
        }

        public float getA() {
            return this.value() != null ? ((Color)this.value()).a : 1.0f;
        }

        @Override
        public String getValueString() {
            return this.getR() + ", " + this.getG() + ", " + this.getB() + ", " + this.getA();
        }

        protected XuiColor copy(XuiLuaStyle parent) {
            XuiColor c = new XuiColor(parent, this.luaTableKey, (Color)this.defaultValue);
            this.copyValuesTo(c);
            if (this.value != null) {
                c.value = new Color((Color)this.value);
            }
            return c;
        }
    }

    @UsedFromLua
    public static class XuiTexture
    extends XuiVar<String, XuiTexture> {
        protected XuiTexture(XuiLuaStyle parent, String key) {
            super(XuiVarType.Texture, parent, key);
        }

        protected XuiTexture(XuiLuaStyle parent, String key, String defaultVal) {
            super(XuiVarType.Texture, parent, key, defaultVal);
        }

        public Texture getTexture() {
            if (this.value() != null) {
                Texture tex = Texture.getSharedTexture((String)this.value());
                if (tex != null) {
                    return tex;
                }
                if (Core.debug) {
                    DebugLog.General.warn("Could not find texture for: " + (String)this.value());
                }
            }
            return null;
        }

        @Override
        protected void fromString(String val) {
            this.setValue(val);
        }

        protected XuiTexture copy(XuiLuaStyle parent) {
            XuiTexture c = new XuiTexture(parent, this.luaTableKey, (String)this.defaultValue);
            this.copyValuesTo(c);
            return c;
        }
    }
}

