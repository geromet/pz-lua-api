/*
 * Decompiled with CFR 0.152.
 */
package zombie.scripting.entity.components.crafting;

import zombie.UsedFromLua;
import zombie.core.Core;
import zombie.debug.DebugLog;
import zombie.entity.ComponentType;
import zombie.entity.components.crafting.recipe.CraftRecipeManager;
import zombie.scripting.ScriptLoadMode;
import zombie.scripting.ScriptParser;
import zombie.scripting.entity.ComponentScript;
import zombie.util.StringUtils;

@UsedFromLua
public class MashingLogicScript
extends ComponentScript {
    private String recipeTagQuery;
    private String resourceFluidId;
    private String inputsGroupName;

    private MashingLogicScript() {
        super(ComponentType.MashingLogic);
    }

    public String getRecipeTagQuery() {
        return this.recipeTagQuery;
    }

    public String getResourceFluidID() {
        return this.resourceFluidId;
    }

    public String getInputsGroupName() {
        return this.inputsGroupName;
    }

    protected void copyFrom(ComponentScript other) {
    }

    @Override
    public void PreReload() {
        this.recipeTagQuery = null;
        this.inputsGroupName = null;
        this.resourceFluidId = null;
    }

    @Override
    public void OnScriptsLoaded(ScriptLoadMode loadMode) throws Exception {
        super.OnScriptsLoaded(loadMode);
        if (StringUtils.isNullOrWhitespace(this.recipeTagQuery)) {
            return;
        }
        this.recipeTagQuery = CraftRecipeManager.FormatAndRegisterRecipeTagsQuery(this.recipeTagQuery);
        if (Core.debug && StringUtils.isNullOrWhitespace(this.inputsGroupName) || StringUtils.isNullOrWhitespace(this.resourceFluidId)) {
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
            if (key.equalsIgnoreCase("inputGroup")) {
                this.inputsGroupName = val;
                continue;
            }
            if (!key.equalsIgnoreCase("fluidID")) continue;
            this.resourceFluidId = val;
        }
        for (ScriptParser.Block child : block.children) {
            if (child.type.equalsIgnoreCase("someType")) continue;
            DebugLog.General.error("Unknown block '" + child.type + "' in entity script: " + this.getName());
        }
    }
}

