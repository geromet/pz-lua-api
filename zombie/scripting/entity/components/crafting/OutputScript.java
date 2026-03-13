/*
 * Decompiled with CFR 0.152.
 */
package zombie.scripting.entity.components.crafting;

import java.util.ArrayList;
import java.util.EnumSet;
import zombie.UsedFromLua;
import zombie.core.Core;
import zombie.core.math.PZMath;
import zombie.debug.DebugLog;
import zombie.debug.objects.DebugClassFields;
import zombie.entity.components.crafting.FluidMatchMode;
import zombie.entity.components.crafting.ItemApplyMode;
import zombie.entity.components.crafting.OutputFlag;
import zombie.entity.components.crafting.recipe.CraftRecipeData;
import zombie.entity.components.crafting.recipe.OutputMapper;
import zombie.entity.components.fluids.Fluid;
import zombie.entity.components.fluids.FluidContainer;
import zombie.entity.components.resources.ResourceType;
import zombie.entity.energy.Energy;
import zombie.inventory.InventoryItem;
import zombie.inventory.types.DrainableComboItem;
import zombie.scripting.ScriptLoadMode;
import zombie.scripting.ScriptParser;
import zombie.scripting.entity.components.crafting.CraftRecipe;
import zombie.scripting.objects.Item;
import zombie.util.StringUtils;

@DebugClassFields
@UsedFromLua
public class OutputScript
extends CraftRecipe.IOScript {
    private static final ArrayList<Item> _emptyItems = new ArrayList();
    private final ResourceType type;
    private String loadedFluid;
    private String loadedEnergy;
    private Fluid fluid;
    private Energy energy;
    private float amount;
    private float maxamount;
    private float chance = 1.0f;
    private boolean applyOnTick;
    @Deprecated
    private int shapedIndex = -1;
    private ItemApplyMode itemApplyMode = ItemApplyMode.Normal;
    private FluidMatchMode fluidMatchMode = FluidMatchMode.Exact;
    private String originalLine = "";
    protected OutputScript createToItemScript;
    private final EnumSet<OutputFlag> flags = EnumSet.noneOf(OutputFlag.class);
    private OutputMapper outputMapper;
    private final ArrayList<Fluid> possibleFluids = new ArrayList();
    private final ArrayList<Energy> possiblyEnergies = new ArrayList();

    private OutputScript(CraftRecipe parentRecipe, ResourceType type) {
        super(parentRecipe);
        this.type = type;
    }

    private boolean typeCheck(ResourceType type) {
        return this.type == type;
    }

    protected boolean isValid() {
        if (this.type == ResourceType.Item) {
            return !this.outputMapper.getResultItems().isEmpty();
        }
        if (this.type == ResourceType.Fluid) {
            return this.fluid != null;
        }
        if (this.type == ResourceType.Energy) {
            return this.energy != null;
        }
        return false;
    }

    public boolean hasCreateToItem() {
        return this.createToItemScript != null;
    }

    public OutputScript getCreateToItemScript() {
        return this.createToItemScript;
    }

    public boolean hasFlag(OutputFlag flag) {
        return this.flags.contains((Object)flag);
    }

    @Deprecated
    public boolean isReplaceInput() {
        return false;
    }

    public String getOriginalLine() {
        return this.originalLine;
    }

    public ResourceType getResourceType() {
        return this.type;
    }

    public float getChance() {
        return this.chance;
    }

    public int getIntAmount() {
        return (int)this.amount;
    }

    public float getAmount() {
        return this.amount;
    }

    public int getIntMaxAmount() {
        return (int)this.maxamount;
    }

    public float getMaxAmount() {
        return this.maxamount;
    }

    public boolean isVariableAmount() {
        return this.amount != this.maxamount;
    }

    @Deprecated
    public int getShapedIndex() {
        return this.shapedIndex;
    }

    public boolean isApplyOnTick() {
        return this.applyOnTick;
    }

    public boolean isHandcraftOnly() {
        return this.flags.contains((Object)OutputFlag.HandcraftOnly);
    }

    public boolean isAutomationOnly() {
        return this.flags.contains((Object)OutputFlag.AutomationOnly);
    }

    public ArrayList<Item> getPossibleResultItems() {
        if (this.outputMapper == null) {
            DebugLog.General.warn("This output does not have items! returning empty list.");
            return _emptyItems;
        }
        return this.outputMapper.getResultItems();
    }

    public ArrayList<Fluid> getPossibleResultFluids() {
        if (this.type == ResourceType.Fluid && this.possibleFluids.isEmpty() && this.fluid != null) {
            this.possibleFluids.add(this.fluid);
        }
        return this.possibleFluids;
    }

    public ArrayList<Energy> getPossibleResultEnergies() {
        if (this.type == ResourceType.Energy && this.possiblyEnergies.isEmpty() && this.energy != null) {
            this.possiblyEnergies.add(this.energy);
        }
        return this.possiblyEnergies;
    }

    public OutputMapper getOutputMapper() {
        return this.outputMapper;
    }

    public Item getItem(CraftRecipeData recipeData) {
        if (this.outputMapper == null) {
            DebugLog.General.warn("This output does not have items! returning null.");
            return null;
        }
        return this.outputMapper.getOutputItem(recipeData);
    }

    public Fluid getFluid() {
        return this.fluid;
    }

    public Energy getEnergy() {
        return this.energy;
    }

    public ItemApplyMode getItemApplyMode() {
        return this.itemApplyMode;
    }

    public FluidMatchMode getFluidMatchMode() {
        return this.fluidMatchMode;
    }

    public boolean isFluidExact() {
        return this.typeCheck(ResourceType.Fluid) && this.fluidMatchMode == FluidMatchMode.Exact;
    }

    public boolean isFluidPrimary() {
        return this.typeCheck(ResourceType.Fluid) && this.fluidMatchMode == FluidMatchMode.Primary;
    }

    public boolean isFluidAnything() {
        return this.typeCheck(ResourceType.Fluid) && this.fluidMatchMode == FluidMatchMode.Anything;
    }

    @Deprecated
    public boolean isCreateUses() {
        return this.typeCheck(ResourceType.Item);
    }

    public boolean containsItem(Item item) {
        return true;
    }

    public boolean containsFluid(Fluid fluid) {
        return this.typeCheck(ResourceType.Fluid) && this.fluid != null && this.fluid.equals(fluid);
    }

    public boolean containsEnergy(Energy energy) {
        return this.typeCheck(ResourceType.Energy) && this.energy != null && this.energy.equals(energy);
    }

    public boolean isFluidMatch(FluidContainer container) {
        if (!this.typeCheck(ResourceType.Fluid) || container == null) {
            return false;
        }
        if (container.isEmpty()) {
            return true;
        }
        boolean fluidMatch = this.isFluidExact() ? !container.isMixture() && this.containsFluid(container.getPrimaryFluid()) : (this.isFluidPrimary() ? this.containsFluid(container.getPrimaryFluid()) : true);
        return fluidMatch;
    }

    public boolean isEnergyMatch(DrainableComboItem item) {
        return this.typeCheck(ResourceType.Energy) && item != null && item.isEnergy() && this.isEnergyMatch(item.getEnergy());
    }

    public boolean isEnergyMatch(Energy energy) {
        if (!this.typeCheck(ResourceType.Energy)) {
            return false;
        }
        return this.containsEnergy(energy);
    }

    protected static OutputScript LoadBlock(CraftRecipe parentRecipe, ScriptParser.Block block) throws Exception {
        OutputScript output = null;
        for (ScriptParser.Value value : block.values) {
            if (StringUtils.isNullOrWhitespace(value.string) || value.string.contains("=") || !StringUtils.containsWhitespace(value.string)) continue;
            DebugLog.General.warn("Cannot load: " + value.string + ", recipe:" + parentRecipe.getScriptObjectFullType());
            String s = value.string.trim();
            output = OutputScript.Load(parentRecipe, s);
        }
        if (output == null) {
            DebugLog.General.warn("Cannot load output block. " + parentRecipe.getScriptObjectFullType());
        }
        for (ScriptParser.Value value : block.values) {
            String key = value.getKey().trim();
            String val = value.getValue().trim();
            if (!key.isEmpty() && !val.isEmpty() && !key.equalsIgnoreCase("something")) continue;
        }
        return output;
    }

    protected static OutputScript Load(CraftRecipe parentRecipe, String line) throws Exception {
        return OutputScript.Load(parentRecipe, line, false);
    }

    protected static OutputScript Load(CraftRecipe parentRecipe, String line, boolean isInternal) throws Exception {
        float upperamount;
        float baseamount;
        ResourceType type;
        if (StringUtils.isNullOrWhitespace(line)) {
            return null;
        }
        String[] elems = line.trim().split("\\s+");
        StringUtils.trimArray(elems);
        if (elems[0].equalsIgnoreCase("item")) {
            type = ResourceType.Item;
        } else if (elems[0].equalsIgnoreCase("fluid")) {
            type = ResourceType.Fluid;
        } else if (elems[0].equalsIgnoreCase("energy")) {
            type = ResourceType.Energy;
        } else {
            throw new Exception("unknown type in craftrecipe: " + elems[0]);
        }
        OutputScript output = new OutputScript(parentRecipe, type);
        output.originalLine = line.trim();
        String outputAmount = elems[1];
        if (outputAmount.contains("variable")) {
            String valueString = outputAmount.substring(outputAmount.indexOf("[") + 1, outputAmount.indexOf("]"));
            String[] values2 = valueString.split(":");
            baseamount = PZMath.max(0.0f, Float.parseFloat(values2[0]));
            upperamount = PZMath.max(0.0f, Float.parseFloat(values2[1]));
        } else {
            upperamount = baseamount = PZMath.max(0.0f, Float.parseFloat(outputAmount));
        }
        output.amount = baseamount;
        output.maxamount = upperamount;
        String result = elems[2];
        if (type == ResourceType.Item) {
            if (!isInternal) {
                if (result.startsWith("mapper:")) {
                    result = result.substring(result.indexOf(":") + 1);
                    output.outputMapper = parentRecipe.getOutputMapper(result);
                    if (output.outputMapper == null) {
                        throw new Exception("Could not find output mapper: " + result);
                    }
                } else {
                    output.outputMapper = new OutputMapper(result);
                    output.outputMapper.setDefaultOutputEntree(result);
                }
            } else if (!"Uses".equalsIgnoreCase(result) && Core.debug) {
                throw new Exception("Parameter with index=2 should be 'Uses'.");
            }
        } else if (type == ResourceType.Fluid) {
            output.loadedFluid = result;
        } else if (type == ResourceType.Energy) {
            output.loadedEnergy = result;
        }
        block4: for (int i = 3; i < elems.length; ++i) {
            String s = elems[i];
            if (s.startsWith("chance:")) {
                s = s.substring(s.indexOf(":") + 1);
                output.chance = PZMath.clamp(Float.parseFloat(s), 0.0f, 1.0f);
                continue;
            }
            if (s.startsWith("shapedIndex:")) {
                s = s.substring(s.indexOf(":") + 1);
                output.shapedIndex = Integer.parseInt(s);
                continue;
            }
            if (s.startsWith("apply:")) {
                if (isInternal) {
                    throw new Exception("Cannot apply 'onTick' on 'itemCreate' ('+' lines).");
                }
                if ((s = s.substring(s.indexOf(":") + 1)).equalsIgnoreCase("onTick")) {
                    if (type == ResourceType.Item) {
                        throw new Exception("Cannot apply 'onTick' on item.");
                    }
                    if (type == ResourceType.Fluid) {
                        throw new Exception("Cannot apply 'onTick' on fluid.");
                    }
                    output.applyOnTick = true;
                    continue;
                }
                throw new Exception("Apply Error");
            }
            if (s.startsWith("mode:")) {
                s = s.substring(s.indexOf(":") + 1);
                switch (type) {
                    case Item: {
                        continue block4;
                    }
                    case Fluid: {
                        if (s.equalsIgnoreCase("exact")) {
                            output.fluidMatchMode = FluidMatchMode.Exact;
                            continue block4;
                        }
                        if (s.equalsIgnoreCase("primary")) {
                            output.fluidMatchMode = FluidMatchMode.Primary;
                            continue block4;
                        }
                        if (s.equalsIgnoreCase("anything")) {
                            output.fluidMatchMode = FluidMatchMode.Anything;
                            continue block4;
                        }
                        throw new Exception("Invalid fluid mode Error");
                    }
                    default: {
                        DebugLog.General.warn("Cannot set mode for type = " + String.valueOf((Object)type));
                        if (!Core.debug) continue block4;
                        throw new Exception("Mode Error");
                    }
                }
            }
            if (s.startsWith("flags")) {
                s = s.substring(s.indexOf("[") + 1, s.indexOf("]"));
                String[] split = s.split(";");
                for (int z = 0; z < split.length; ++z) {
                    String entry = split[z];
                    OutputFlag flag = OutputFlag.valueOf(entry);
                    output.flags.add(flag);
                }
                continue;
            }
            throw new Exception("unknown recipe param: " + s);
        }
        return output;
    }

    public void OnScriptsLoaded(ScriptLoadMode loadMode) throws Exception {
    }

    /*
     * Enabled force condition propagation
     * Lifted jumps to return sites
     */
    protected void OnPostWorldDictionaryInit() throws Exception {
        if (this.createToItemScript != null) {
            this.createToItemScript.OnPostWorldDictionaryInit();
            if (this.getIntAmount() != 1 || this.isVariableAmount()) {
                throw new Exception("Lines prior to a '+' line should have 1 item amount. line: " + this.originalLine);
            }
            if (this.type != ResourceType.Item) {
                throw new Exception("Lines prior to a '+' line should be of resource type Item. line: " + this.originalLine);
            }
            if (this.applyOnTick) {
                throw new Exception("Lines prior to a '+' line should not be apply on tick. line: " + this.originalLine);
            }
            this.itemApplyMode = ItemApplyMode.Normal;
        }
        if (this.type == ResourceType.Item) {
            if (this.outputMapper == null) {
                throw new Exception("No outputMapper set. line: " + this.originalLine);
            }
            this.outputMapper.OnPostWorldDictionaryInit(this.getParentRecipe().getName());
        } else if (this.type == ResourceType.Fluid) {
            Fluid fluid = Fluid.Get(this.loadedFluid);
            if (fluid == null) throw new Exception("Fluid not found: " + this.loadedFluid + ", line: " + this.originalLine);
            this.fluid = fluid;
        } else if (this.type == ResourceType.Energy) {
            Energy energy = Energy.Get(this.loadedEnergy);
            if (energy == null) throw new Exception("Energy not found: " + this.loadedEnergy + ", line: " + this.originalLine);
            this.energy = energy;
        }
        if (this.isValid()) return;
        throw new Exception("Invalid output. line: " + this.originalLine);
    }

    public boolean canOutputItem(InventoryItem item) {
        if (item.getScriptItem() == null) {
            return false;
        }
        return this.canOutputItem(item.getScriptItem());
    }

    public boolean canOutputItem(Item item) {
        return this.getPossibleResultItems().contains(item);
    }
}

