/*
 * Decompiled with CFR 0.152.
 */
package zombie.entity.components.fluids;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashSet;
import zombie.GameWindow;
import zombie.UsedFromLua;
import zombie.core.Core;
import zombie.core.Translator;
import zombie.debug.DebugLog;
import zombie.debug.objects.DebugClassFields;
import zombie.entity.components.fluids.Fluid;
import zombie.entity.components.fluids.FluidCategory;
import zombie.entity.components.fluids.FluidType;
import zombie.scripting.ScriptManager;
import zombie.scripting.objects.FluidFilterScript;

@DebugClassFields
@UsedFromLua
public class FluidFilter {
    private FluidFilterScript filterScript;
    private final EnumSet<FluidCategory> categories = EnumSet.noneOf(FluidCategory.class);
    private final EnumSet<FluidType> fluidEnums = EnumSet.noneOf(FluidType.class);
    private final HashSet<String> fluidStrings = new HashSet();
    private FilterType filterType = FilterType.Whitelist;
    private boolean isSealed;
    private String cachedFilterDisplayName;
    private String cachedFilterTooltipText;

    public void setFilterScript(String filterScriptName) {
        if (filterScriptName == null) {
            this.filterScript = null;
        } else if (this.filterScript == null || !filterScriptName.equalsIgnoreCase(this.filterScript.getScriptObjectFullType())) {
            FluidFilterScript script = ScriptManager.instance.getFluidFilter(filterScriptName);
            if (script == null) {
                DebugLog.General.warn("FluidFilter '" + filterScriptName + "' not found.");
            }
            this.filterScript = script;
        }
    }

    public String toString() {
        Object s = super.toString();
        if (this.categories.isEmpty() && this.fluidEnums.isEmpty() && this.fluidStrings.isEmpty()) {
            s = (String)s + "{EMPTY_FILTER}";
            return s;
        }
        s = (String)s + "{";
        s = (String)s + "<cats=" + String.valueOf(this.categories) + ">";
        s = (String)s + "<fluids=" + String.valueOf(this.fluidEnums) + ">";
        s = (String)s + "<strings=" + this.fluidStrings.size() + ">";
        s = (String)s + "}";
        return s;
    }

    public void seal() {
        this.isSealed = true;
    }

    public boolean isSealed() {
        return this.isSealed;
    }

    public FluidFilter copy() {
        FluidFilter copy = new FluidFilter();
        copy.filterType = this.filterType;
        copy.categories.addAll(this.categories);
        copy.fluidEnums.addAll(this.fluidEnums);
        copy.fluidStrings.addAll(this.fluidStrings);
        return copy;
    }

    public FilterType getFilterType() {
        return this.filterType;
    }

    public FluidFilter setFilterType(FilterType filterType) {
        if (this.isSealed) {
            DebugLog.log("FluidFilter -> attempting setFilterType on sealed filter.");
            return this;
        }
        this.filterType = filterType;
        return this;
    }

    public FluidFilter add(FluidCategory category) {
        if (this.isSealed) {
            DebugLog.log("FluidFilter -> attempting add on sealed filter.");
            return this;
        }
        this.categories.add(category);
        return this;
    }

    public FluidFilter remove(FluidCategory category) {
        if (this.isSealed) {
            DebugLog.log("FluidFilter -> attempting remove on sealed filter.");
            return this;
        }
        this.categories.remove((Object)category);
        return this;
    }

    public boolean contains(FluidCategory category) {
        return this.categories.contains((Object)category);
    }

    public FluidFilter add(FluidType fluid) {
        if (this.isSealed) {
            DebugLog.log("FluidFilter -> attempting add on sealed filter.");
            return this;
        }
        if (fluid == FluidType.Modded) {
            DebugLog.log("Cannot add enum FluidType.Modded to fluid filter, add the fluid string (getFluidTypeString) instead.");
            return this;
        }
        if (!this.fluidEnums.contains((Object)fluid)) {
            this.fluidEnums.add(fluid);
            this.fluidStrings.add(fluid.toString());
        }
        return this;
    }

    public FluidFilter add(Fluid fluid) {
        if (fluid.getFluidType() != FluidType.Modded) {
            return this.add(fluid.getFluidType());
        }
        return this.add(fluid.getFluidTypeString());
    }

    public FluidFilter add(String fluid) {
        if (this.isSealed) {
            DebugLog.log("FluidFilter -> attempting add on sealed filter.");
            return this;
        }
        if (!this.fluidStrings.contains(fluid)) {
            if (Fluid.Get(fluid) != null) {
                this.fluidStrings.add(fluid);
            } else {
                DebugLog.log("FluidFilter.add -> fluid '" + fluid + "' is not a valid registered fluid.");
            }
        }
        return this;
    }

    public FluidFilter remove(FluidType fluid) {
        if (this.isSealed) {
            DebugLog.log("FluidFilter -> attempting remove on sealed filter.");
            return this;
        }
        this.fluidEnums.remove((Object)fluid);
        this.fluidStrings.remove(fluid.toString());
        return this;
    }

    public FluidFilter remove(Fluid fluid) {
        if (fluid.getFluidType() != FluidType.Modded) {
            return this.remove(fluid.getFluidType());
        }
        return this.remove(fluid.getFluidTypeString());
    }

    public FluidFilter remove(String fluid) {
        if (this.isSealed) {
            DebugLog.log("FluidFilter -> attempting remove on sealed filter.");
            return this;
        }
        this.fluidStrings.remove(fluid);
        return this;
    }

    public boolean contains(FluidType fluid) {
        return this.fluidEnums.contains((Object)fluid);
    }

    public boolean contains(Fluid fluid) {
        if (fluid.getFluidType() != FluidType.Modded) {
            return this.contains(fluid.getFluidType());
        }
        return this.contains(fluid.getFluidTypeString());
    }

    public boolean contains(String fluid) {
        return this.fluidStrings.contains(fluid);
    }

    public boolean allows(FluidType fluidType) {
        Fluid fluid = Fluid.Get(fluidType);
        return this.allows(fluid);
    }

    public boolean allows(Fluid fluid) {
        boolean fluidMatch;
        if (fluid == null) {
            return false;
        }
        boolean typeMatch = this.fluidEnums.isEmpty() && this.fluidStrings.isEmpty();
        boolean categoryMatch = this.categories.isEmpty();
        if (fluid.getFluidType() != FluidType.Modded && this.fluidEnums.contains((Object)fluid.getFluidType()) || fluid.getFluidType() == FluidType.Modded && this.fluidStrings.contains(fluid.getFluidTypeString())) {
            typeMatch = true;
        }
        for (FluidCategory category : this.categories) {
            if (!fluid.isCategory(category)) continue;
            categoryMatch = true;
            break;
        }
        boolean bl = fluidMatch = typeMatch && categoryMatch;
        return this.filterType == FilterType.Whitelist ? fluidMatch : !fluidMatch;
    }

    public boolean allows(String fluidString) {
        Fluid fluid = Fluid.Get(fluidString);
        return this.allows(fluid);
    }

    public String getFilterDisplayName() {
        if (this.cachedFilterDisplayName != null) {
            return this.cachedFilterDisplayName;
        }
        if (this.categories.isEmpty() && this.fluidEnums.isEmpty() && this.fluidStrings.isEmpty()) {
            this.cachedFilterDisplayName = Translator.getText("Fluid_Name_Any");
            return this.cachedFilterDisplayName;
        }
        ArrayList<String> fluidNames = new ArrayList<String>();
        ArrayList<String> fluidCategories = new ArrayList<String>();
        for (FluidType fluidType : this.fluidEnums) {
            if (fluidNames.contains(fluidType.getDisplayName())) continue;
            fluidNames.add(fluidType.getDisplayName());
        }
        for (String string : this.fluidStrings) {
            String name = Fluid.Get(string).getDisplayName();
            if (fluidNames.contains(name)) continue;
            fluidNames.add(name);
        }
        for (FluidCategory fluidCategory : this.categories) {
            fluidCategories.add(fluidCategory.name());
        }
        this.cachedFilterDisplayName = "";
        if (!fluidNames.isEmpty()) {
            fluidNames.sort(String::compareTo);
            this.cachedFilterDisplayName = fluidNames.size() == 1 ? (String)fluidNames.get(0) : Translator.getText("Fluid_And_Others", fluidNames.get(0));
            return this.cachedFilterDisplayName;
        }
        if (fluidCategories.isEmpty()) {
            this.cachedFilterDisplayName = Translator.getText("Fluid_Name_Any");
        } else {
            fluidCategories.sort(String::compareTo);
            this.cachedFilterDisplayName = fluidCategories.size() == 1 ? Translator.getText("Fluid_Any", fluidCategories.get(0)) : Translator.getText("Fluid_Any_Others", fluidCategories.get(0));
        }
        return this.cachedFilterDisplayName;
    }

    public String getFilterTooltipText() {
        String name;
        if (this.cachedFilterTooltipText != null) {
            return this.cachedFilterTooltipText;
        }
        if (this.categories.isEmpty() && this.fluidEnums.isEmpty() && this.fluidStrings.isEmpty()) {
            this.cachedFilterTooltipText = Translator.getText("Fluid_Name_Any");
            return this.cachedFilterTooltipText;
        }
        ArrayList<String> fluidNames = new ArrayList<String>();
        ArrayList<String> fluidCategories = new ArrayList<String>();
        for (FluidType fluidType : this.fluidEnums) {
            name = fluidType.getDisplayName();
            if (!fluidNames.stream().noneMatch(entry -> entry.equals(name))) continue;
            fluidNames.add(fluidType.getDisplayName());
        }
        for (String string : this.fluidStrings) {
            name = Fluid.Get(string).getDisplayName();
            if (!fluidNames.stream().noneMatch(entry -> entry.equals(name))) continue;
            fluidNames.add(name);
        }
        for (FluidCategory fluidCategory : this.categories) {
            fluidCategories.add(fluidCategory.name());
        }
        this.cachedFilterTooltipText = "";
        if (!fluidNames.isEmpty()) {
            this.cachedFilterTooltipText = this.cachedFilterTooltipText + (String)fluidNames.get(0);
            for (int i = 1; i < fluidNames.size(); ++i) {
                this.cachedFilterTooltipText = this.cachedFilterTooltipText + "\n" + (String)fluidNames.get(i);
            }
            return this.cachedFilterTooltipText;
        }
        if (fluidCategories.isEmpty()) {
            this.cachedFilterTooltipText = Translator.getText("Fluid_Name_Any");
        } else {
            this.cachedFilterTooltipText = (String)fluidCategories.get(0);
            for (int i = 1; i < fluidCategories.size(); ++i) {
                this.cachedFilterTooltipText = this.cachedFilterTooltipText + "\n" + (String)fluidCategories.get(i);
            }
        }
        return this.cachedFilterTooltipText;
    }

    public void save(ByteBuffer output) throws IOException {
        output.put(this.filterType == FilterType.Whitelist ? (byte)1 : 0);
        output.put((byte)this.fluidEnums.size());
        this.fluidEnums.forEach(e -> output.put(e.getId()));
        output.put((byte)this.fluidStrings.size());
        this.fluidStrings.forEach(s -> GameWindow.WriteString(output, s));
        output.put((byte)this.categories.size());
        this.categories.forEach(c -> output.put(c.getId()));
    }

    public void load(ByteBuffer input, int worldVersion) throws IOException {
        int i;
        if (this.isSealed) {
            DebugLog.log("FluidFilter -> Warning loading on a sealed fluid filter!");
            if (Core.debug) {
                throw new RuntimeException("Loading on a sealed fluid filter!");
            }
        }
        this.fluidEnums.clear();
        this.fluidStrings.clear();
        this.categories.clear();
        this.filterType = input.get() != 0 ? FilterType.Whitelist : FilterType.Blacklist;
        int count = input.get();
        if (count > 0) {
            for (i = 0; i < count; ++i) {
                byte id = input.get();
                FluidType type = FluidType.FromId(id);
                if (type == null) continue;
                this.fluidEnums.add(type);
            }
        }
        if ((count = input.get()) > 0) {
            for (i = 0; i < count; ++i) {
                String type = GameWindow.ReadString(input);
                if (Fluid.Get(type) == null) continue;
                this.fluidStrings.add(type);
            }
        }
        if ((count = input.get()) > 0) {
            for (i = 0; i < count; ++i) {
                byte id = input.get();
                FluidCategory category = FluidCategory.FromId(id);
                if (category == null) continue;
                this.categories.add(category);
            }
        }
    }

    @UsedFromLua
    public static enum FilterType {
        Whitelist,
        Blacklist;

    }
}

