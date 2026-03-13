/*
 * Decompiled with CFR 0.152.
 */
package zombie.scripting.entity.components.crafting;

import java.util.List;
import zombie.UsedFromLua;
import zombie.debug.DebugLog;
import zombie.entity.ComponentType;
import zombie.entity.components.crafting.recipe.CraftRecipeManager;
import zombie.entity.components.resources.ResourceChannel;
import zombie.entity.util.enums.EnumBitStore;
import zombie.scripting.ScriptLoadMode;
import zombie.scripting.ScriptParser;
import zombie.scripting.entity.ComponentScript;
import zombie.scripting.entity.components.crafting.CraftRecipe;
import zombie.util.StringUtils;

@UsedFromLua
public class CraftBenchScript
extends ComponentScript {
    private final EnumBitStore<ResourceChannel> fluidInputChannels = EnumBitStore.noneOf(ResourceChannel.class);
    private final EnumBitStore<ResourceChannel> energyInputChannels = EnumBitStore.noneOf(ResourceChannel.class);
    private String recipeTagQuery;

    private CraftBenchScript() {
        super(ComponentType.CraftBench);
    }

    public String getRecipeTagQuery() {
        return this.recipeTagQuery;
    }

    public List<CraftRecipe> getRecipes() {
        return CraftRecipeManager.queryRecipes(this.recipeTagQuery);
    }

    public EnumBitStore<ResourceChannel> getFluidInputChannels() {
        return this.fluidInputChannels;
    }

    public EnumBitStore<ResourceChannel> getEnergyInputChannels() {
        return this.energyInputChannels;
    }

    protected void copyFrom(ComponentScript other) {
    }

    @Override
    public void OnScriptsLoaded(ScriptLoadMode loadMode) throws Exception {
        super.OnScriptsLoaded(loadMode);
        if (StringUtils.isNullOrWhitespace(this.recipeTagQuery)) {
            throw new Exception("Recipe tag query null or whitespace.");
        }
        this.recipeTagQuery = CraftRecipeManager.FormatAndRegisterRecipeTagsQuery(this.recipeTagQuery);
    }

    @Override
    protected void load(ScriptParser.Block block) throws Exception {
        for (ScriptParser.Value value : block.values) {
            ResourceChannel channel;
            int i;
            String[] split;
            String key = value.getKey().trim();
            String val = value.getValue().trim();
            if (key.isEmpty() || val.isEmpty()) continue;
            if (key.equalsIgnoreCase("recipes")) {
                this.recipeTagQuery = val;
                continue;
            }
            if (key.equalsIgnoreCase("fluidInputChannels")) {
                split = val.split(";");
                for (i = 0; i < split.length; ++i) {
                    channel = ResourceChannel.valueOf(split[i]);
                    this.fluidInputChannels.add(channel);
                }
                continue;
            }
            if (!key.equalsIgnoreCase("energyInputChannels")) continue;
            split = val.split(";");
            for (i = 0; i < split.length; ++i) {
                channel = ResourceChannel.valueOf(split[i]);
                this.energyInputChannels.add(channel);
            }
        }
        for (ScriptParser.Block child : block.children) {
            if (child.type.equalsIgnoreCase("craftProcessor")) continue;
            DebugLog.General.error("Unknown block '" + child.type + "' in entity script: " + this.getName());
        }
    }
}

