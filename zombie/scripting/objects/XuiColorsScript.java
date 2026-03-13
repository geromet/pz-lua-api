/*
 * Decompiled with CFR 0.152.
 */
package zombie.scripting.objects;

import java.util.HashMap;
import java.util.Map;
import zombie.UsedFromLua;
import zombie.core.Color;
import zombie.scripting.ScriptParser;
import zombie.scripting.ScriptType;
import zombie.scripting.objects.BaseScriptObject;

@UsedFromLua
public class XuiColorsScript
extends BaseScriptObject {
    private String name;
    private final Map<String, Color> colorMap = new HashMap<String, Color>();

    public XuiColorsScript() {
        super(ScriptType.XuiColor);
    }

    public String getName() {
        return this.name;
    }

    public Map<String, Color> getColorMap() {
        return this.colorMap;
    }

    @Override
    public void Load(String name, String totalFile) throws Exception {
        this.name = name;
        ScriptParser.Block block = ScriptParser.parse(totalFile);
        block = block.children.get(0);
        this.LoadCommonBlock(block);
        this.LoadColorsBlock(block);
    }

    protected void LoadColorsBlock(ScriptParser.Block block) throws Exception {
        for (ScriptParser.Value value : block.values) {
            String key = value.getKey().trim();
            String val = value.getValue().trim();
            if (key.isEmpty() || val.isEmpty()) continue;
            Color color = new Color();
            String[] split = val.split(":");
            if (split.length > 1 && split[0].trim().equalsIgnoreCase("rgb")) {
                block13: for (i = 1; i < split.length; ++i) {
                    switch (i) {
                        case 1: {
                            color.r = Float.parseFloat(split[i].trim()) / 255.0f;
                            continue block13;
                        }
                        case 2: {
                            color.g = Float.parseFloat(split[i].trim()) / 255.0f;
                            continue block13;
                        }
                        case 3: {
                            color.b = Float.parseFloat(split[i].trim()) / 255.0f;
                            continue block13;
                        }
                        case 4: {
                            color.a = Float.parseFloat(split[i].trim()) / 255.0f;
                        }
                    }
                }
            } else {
                block14: for (i = 0; i < split.length; ++i) {
                    switch (i) {
                        case 0: {
                            color.r = Float.parseFloat(split[i].trim());
                            continue block14;
                        }
                        case 1: {
                            color.g = Float.parseFloat(split[i].trim());
                            continue block14;
                        }
                        case 2: {
                            color.b = Float.parseFloat(split[i].trim());
                            continue block14;
                        }
                        case 3: {
                            color.a = Float.parseFloat(split[i].trim());
                        }
                    }
                }
            }
            this.colorMap.put(key, color);
        }
    }
}

