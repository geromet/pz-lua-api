/*
 * Decompiled with CFR 0.152.
 */
package zombie.scripting.entity.components.crafting;

import java.util.ArrayList;
import zombie.UsedFromLua;
import zombie.core.Core;
import zombie.debug.DebugLog;
import zombie.entity.ComponentType;
import zombie.entity.components.crafting.StartMode;
import zombie.entity.components.crafting.recipe.CraftRecipeManager;
import zombie.scripting.ScriptLoadMode;
import zombie.scripting.ScriptParser;
import zombie.scripting.entity.ComponentScript;
import zombie.util.StringUtils;

@UsedFromLua
public class CraftLogicScript
extends ComponentScript {
    private String recipeTagQuery;
    private StartMode startMode = StartMode.Manual;
    private String inputsGroupName;
    private String outputsGroupName;
    private String actionAnim;

    private CraftLogicScript() {
        super(ComponentType.CraftLogic);
    }

    protected CraftLogicScript(ComponentType type) {
        super(type);
    }

    public String getRecipeTagQuery() {
        return this.recipeTagQuery;
    }

    public StartMode getStartMode() {
        return this.startMode;
    }

    public String getInputsGroupName() {
        return this.inputsGroupName;
    }

    public String getOutputsGroupName() {
        return this.outputsGroupName;
    }

    public String getActionAnim() {
        return this.actionAnim;
    }

    @Deprecated
    public ArrayList<Object> getCraftProcessorScripts() {
        return new ArrayList<Object>();
    }

    protected void copyFrom(ComponentScript other) {
    }

    @Override
    public void PreReload() {
        this.recipeTagQuery = null;
        this.startMode = StartMode.Manual;
        this.inputsGroupName = null;
        this.outputsGroupName = null;
    }

    @Override
    public void OnScriptsLoaded(ScriptLoadMode loadMode) throws Exception {
        super.OnScriptsLoaded(loadMode);
        if (StringUtils.isNullOrWhitespace(this.recipeTagQuery)) {
            return;
        }
        this.recipeTagQuery = CraftRecipeManager.FormatAndRegisterRecipeTagsQuery(this.recipeTagQuery);
        if (Core.debug && StringUtils.isNullOrWhitespace(this.inputsGroupName) || StringUtils.isNullOrWhitespace(this.outputsGroupName)) {
            // empty if block
        }
    }

    @Override
    protected void load(ScriptParser.Block block) throws Exception {
        for (ScriptParser.Value value : block.values) {
            String key = value.getKey().trim();
            String val = value.getValue().trim();
            if (key.isEmpty() || val.isEmpty()) continue;
            if (key.equalsIgnoreCase("recipes")) {
                this.recipeTagQuery = val;
                continue;
            }
            if (key.equalsIgnoreCase("startMode")) {
                this.startMode = StartMode.valueOf(val);
                continue;
            }
            if (key.equalsIgnoreCase("inputGroup")) {
                this.inputsGroupName = val;
                continue;
            }
            if (key.equalsIgnoreCase("outputGroup")) {
                this.outputsGroupName = val;
                continue;
            }
            if (!key.equalsIgnoreCase("actionAnim")) continue;
            this.actionAnim = val;
        }
        for (ScriptParser.Block child : block.children) {
            if (child.type.equalsIgnoreCase("craftProcessor")) {
                DebugLog.General.warn("Block craft processor is deprecated.");
                continue;
            }
            DebugLog.General.error("Unknown block '" + child.type + "' in entity script: " + this.getName());
        }
    }
}

