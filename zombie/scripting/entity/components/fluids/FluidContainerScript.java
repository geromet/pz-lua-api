/*
 * Decompiled with CFR 0.152.
 */
package zombie.scripting.entity.components.fluids;

import java.util.ArrayList;
import zombie.UsedFromLua;
import zombie.core.Color;
import zombie.core.math.PZMath;
import zombie.core.random.Rand;
import zombie.debug.DebugLog;
import zombie.debug.objects.DebugClassFields;
import zombie.entity.ComponentType;
import zombie.entity.components.fluids.Fluid;
import zombie.entity.components.fluids.FluidFilter;
import zombie.entity.components.fluids.FluidUtil;
import zombie.scripting.ScriptLoadMode;
import zombie.scripting.ScriptParser;
import zombie.scripting.entity.ComponentScript;
import zombie.scripting.objects.FluidFilterScript;
import zombie.util.StringUtils;

@DebugClassFields
@UsedFromLua
public class FluidContainerScript
extends ComponentScript {
    private static final String TAG_OVERRIDE = "override";
    private FluidFilterScript whitelist;
    private FluidFilterScript blacklist;
    private String containerName = "FluidContainer";
    private float capacity = 1.0f;
    private boolean initialAmountSet;
    private float initialAmountMin;
    private float initialAmountMax = 1.0f;
    private ArrayList<FluidScript> initialFluids;
    private boolean initialFluidsIsRandom;
    private boolean inputLocked;
    private boolean canEmpty = true;
    private boolean hiddenAmount;
    private float rainCatcher;
    private boolean fillsWithCleanWater;
    private String customDrinkSound = "DrinkingFromGeneric";

    private FluidContainerScript() {
        super(ComponentType.FluidContainer);
    }

    @Override
    public void PreReload() {
        this.whitelist = null;
        this.blacklist = null;
        this.containerName = "FluidContainer";
        this.capacity = 1.0f;
        this.initialAmountSet = false;
        this.initialAmountMin = 0.0f;
        this.initialAmountMax = 1.0f;
        this.initialFluids = null;
        this.initialFluidsIsRandom = false;
        this.inputLocked = false;
        this.canEmpty = true;
        this.hiddenAmount = false;
        this.rainCatcher = 0.0f;
        this.fillsWithCleanWater = false;
        this.customDrinkSound = "DrinkingFromGeneric";
    }

    @Override
    public void OnScriptsLoaded(ScriptLoadMode loadMode) throws Exception {
        super.OnScriptsLoaded(loadMode);
        this.capacity = PZMath.max(this.capacity, FluidUtil.getMinContainerCapacity());
        if (this.initialAmountMin > this.initialAmountMax) {
            float max = this.initialAmountMax;
            this.initialAmountMax = this.initialAmountMin;
            this.initialAmountMin = max;
        }
        this.initialAmountMin = PZMath.clamp(this.initialAmountMin, 0.0f, 1.0f);
        this.initialAmountMax = PZMath.clamp(this.initialAmountMax, 0.0f, 1.0f);
        this.initialAmountMin *= this.capacity;
        this.initialAmountMax *= this.capacity;
        if (!this.initialFluidsIsRandom && this.initialFluids != null && !this.initialFluids.isEmpty()) {
            PZMath.normalize(this.initialFluids, FluidScript::getPercentage, FluidScript::setPercentage);
        }
        if (this.whitelist != null) {
            this.whitelist.createFilter().seal();
        }
        if (this.blacklist != null) {
            this.blacklist.createFilter().seal();
        }
    }

    protected void copyFrom(ComponentScript componentScript) {
        FluidContainerScript other = (FluidContainerScript)componentScript;
        this.containerName = other.containerName;
        this.capacity = other.capacity;
        this.initialFluidsIsRandom = other.initialFluidsIsRandom;
        this.initialAmountSet = other.initialAmountSet;
        this.initialAmountMin = other.initialAmountMin;
        this.initialAmountMax = other.initialAmountMax;
        this.inputLocked = other.inputLocked;
        this.canEmpty = other.canEmpty;
        this.rainCatcher = other.rainCatcher;
        this.fillsWithCleanWater = other.fillsWithCleanWater;
        this.hiddenAmount = other.hiddenAmount;
        this.customDrinkSound = other.customDrinkSound;
        if (other.initialFluids != null) {
            this.initialFluids = new ArrayList();
            for (FluidScript fsOther : other.initialFluids) {
                FluidScript fs = this.getOrCreateFluidScript(fsOther.fluidType);
                fs.percentage = fsOther.percentage;
                if (fsOther.customColor == null) continue;
                fs.customColor = new Color();
                fs.customColor.set(fsOther.customColor);
            }
        }
        if (other.whitelist != null) {
            this.whitelist = other.whitelist.copy();
        }
        if (other.blacklist != null) {
            this.blacklist = other.blacklist.copy();
        }
    }

    @Override
    protected void load(ScriptParser.Block block) throws Exception {
        super.load(block);
        for (ScriptParser.BlockElement element : block.elements) {
            boolean isOverride;
            if (element.asValue() != null) {
                String s = element.asValue().string;
                if (s.trim().isEmpty() || !s.contains("=")) continue;
                String[] split = s.split("=");
                String k = split[0].trim();
                String v = split[1].trim();
                if (k.equalsIgnoreCase("Capacity")) {
                    this.capacity = Float.parseFloat(v);
                    continue;
                }
                if (k.equalsIgnoreCase("ContainerName")) {
                    if (StringUtils.containsWhitespace(v)) {
                        DebugLog.General.error("Sanitizing container name '" + v + "', name may not contain whitespaces.");
                        v = StringUtils.removeWhitespace(v);
                    }
                    this.containerName = v;
                    continue;
                }
                if (k.equalsIgnoreCase("InitialPercent")) {
                    this.initialAmountMax = this.initialAmountMin = Float.parseFloat(v);
                    this.initialAmountSet = true;
                    continue;
                }
                if (k.equalsIgnoreCase("InitialPercentMin")) {
                    this.initialAmountMin = Float.parseFloat(v);
                    this.initialAmountSet = true;
                    continue;
                }
                if (k.equalsIgnoreCase("InitialPercentMax")) {
                    this.initialAmountMax = Float.parseFloat(v);
                    this.initialAmountSet = true;
                    continue;
                }
                if (k.equalsIgnoreCase("PickRandomFluid")) {
                    this.initialFluidsIsRandom = v.equalsIgnoreCase("true");
                    continue;
                }
                if (k.equalsIgnoreCase("InputLocked")) {
                    this.inputLocked = v.equalsIgnoreCase("true");
                    continue;
                }
                if (k.equalsIgnoreCase("Opened")) {
                    this.canEmpty = v.equalsIgnoreCase("true");
                    continue;
                }
                if (k.equalsIgnoreCase("HiddenAmount")) {
                    this.hiddenAmount = v.equalsIgnoreCase("true");
                    continue;
                }
                if (k.equalsIgnoreCase("RainFactor")) {
                    this.rainCatcher = Float.parseFloat(v);
                    continue;
                }
                if (k.equalsIgnoreCase("FillsWithCleanWater")) {
                    this.fillsWithCleanWater = v.equalsIgnoreCase("true");
                    continue;
                }
                if (!k.equalsIgnoreCase("CustomDrinkSound")) continue;
                this.customDrinkSound = v;
                continue;
            }
            ScriptParser.Block child = element.asBlock();
            boolean bl = isOverride = block.id != null && block.id.equalsIgnoreCase(TAG_OVERRIDE);
            if (child.type.equalsIgnoreCase("fluids")) {
                this.loadBlockFluids(child, isOverride);
                continue;
            }
            if (!child.type.equalsIgnoreCase("whitelist") && !child.type.equalsIgnoreCase("blacklist")) continue;
            boolean isWhiteList = child.type.equalsIgnoreCase("whitelist");
            FluidFilterScript filter = !isOverride && isWhiteList && this.whitelist != null ? this.whitelist : (!isOverride && !isWhiteList && this.blacklist != null ? this.blacklist : FluidFilterScript.GetAnonymous(isWhiteList));
            filter.LoadAnonymousFromBlock(child);
            if (isWhiteList) {
                this.whitelist = filter;
                continue;
            }
            this.blacklist = filter;
        }
    }

    private void loadBlockFluids(ScriptParser.Block block, boolean isOverride) {
        if (isOverride && this.initialFluids != null) {
            this.initialFluids.clear();
        }
        for (ScriptParser.Value value : block.values) {
            String k = value.getKey().trim();
            String v = value.getValue().trim();
            if (!k.equalsIgnoreCase("Fluid")) continue;
            FluidScript fs = this.readFluid(v);
            if (this.initialFluids == null) {
                this.initialFluids = new ArrayList();
            }
            this.initialFluids.add(fs);
        }
        for (ScriptParser.Block child : block.children) {
            if (!child.type.equalsIgnoreCase("Fluid")) continue;
            FluidScript fs = this.readFluidAsBlock(child);
            if (fs != null) {
                if (this.initialFluids == null) {
                    this.initialFluids = new ArrayList();
                }
                this.initialFluids.add(fs);
                continue;
            }
            DebugLog.General.warn("Unable to read fluid block.");
        }
    }

    private FluidScript getOrCreateFluidScript(String fluidType) {
        return new FluidScript(fluidType);
    }

    private FluidScript readFluidAsBlock(ScriptParser.Block block) {
        String v;
        String k;
        FluidScript fs = null;
        for (ScriptParser.Value value : block.values) {
            k = value.getKey().trim();
            v = value.getValue().trim();
            if (!"type".equalsIgnoreCase(k)) continue;
            fs = this.getOrCreateFluidScript(v);
        }
        if (fs == null) {
            return null;
        }
        for (ScriptParser.Value value : block.values) {
            k = value.getKey().trim();
            v = value.getValue().trim();
            if ("percentage".equalsIgnoreCase(k)) {
                fs.percentage = Float.parseFloat(v);
                continue;
            }
            if ("r".equalsIgnoreCase(k)) {
                if (fs.customColor == null) {
                    fs.customColor = new Color(1.0f, 1.0f, 1.0f);
                }
                fs.customColor.r = Float.parseFloat(v);
                continue;
            }
            if ("g".equalsIgnoreCase(k)) {
                if (fs.customColor == null) {
                    fs.customColor = new Color(1.0f, 1.0f, 1.0f);
                }
                fs.customColor.g = Float.parseFloat(v);
                continue;
            }
            if (!"b".equalsIgnoreCase(k)) continue;
            if (fs.customColor == null) {
                fs.customColor = new Color(1.0f, 1.0f, 1.0f);
            }
            fs.customColor.b = Float.parseFloat(v);
        }
        return fs;
    }

    private FluidScript readFluid(String v) {
        String[] opt = v.split(":");
        FluidScript fs = this.getOrCreateFluidScript(opt[0]);
        if (opt.length > 1) {
            fs.percentage = Float.parseFloat(opt[1]);
        }
        if (opt.length == 5) {
            float r = Float.parseFloat(opt[2]);
            float g = Float.parseFloat(opt[3]);
            float b = Float.parseFloat(opt[4]);
            fs.customColor = new Color(r, g, b);
        }
        return fs;
    }

    public FluidFilter getWhitelistCopy() {
        if (this.whitelist != null) {
            return this.whitelist.getFilter().copy();
        }
        return null;
    }

    public FluidFilter getBlacklistCopy() {
        if (this.blacklist != null) {
            return this.blacklist.getFilter().copy();
        }
        return null;
    }

    public String getContainerName() {
        return this.containerName;
    }

    public String getCustomDrinkSound() {
        return this.customDrinkSound;
    }

    public float getCapacity() {
        return this.capacity;
    }

    public float getInitialAmount() {
        if (!this.initialAmountSet) {
            return this.capacity;
        }
        if (this.initialAmountMin == this.initialAmountMax) {
            return this.initialAmountMin;
        }
        return Rand.Next(this.initialAmountMin, this.initialAmountMax);
    }

    public ArrayList<FluidScript> getInitialFluids() {
        return this.initialFluids;
    }

    public boolean isInitialFluidsIsRandom() {
        return this.initialFluidsIsRandom;
    }

    public boolean getInputLocked() {
        return this.inputLocked;
    }

    public boolean getCanEmpty() {
        return this.canEmpty;
    }

    public boolean isHiddenAmount() {
        return this.hiddenAmount;
    }

    public float getRainCatcher() {
        return this.rainCatcher;
    }

    public boolean isFilledWithCleanWater() {
        return this.fillsWithCleanWater;
    }

    @DebugClassFields
    @UsedFromLua
    public static class FluidScript {
        private final String fluidType;
        private float percentage = 1.0f;
        private Color customColor;
        private Fluid fluid;

        private FluidScript(String fluidType) {
            this.fluidType = fluidType;
        }

        public String getFluidType() {
            return this.fluidType;
        }

        public Fluid getFluid() {
            if (this.fluid == null) {
                this.fluid = Fluid.Get(this.fluidType);
                if (this.fluid == null) {
                    DebugLog.General.warn("Cannot find fluid '" + this.fluidType + "' in fluid script.");
                }
            }
            return this.fluid;
        }

        protected void setPercentage(float f) {
            this.percentage = f;
        }

        public float getPercentage() {
            return this.percentage;
        }

        public Color getCustomColor() {
            return this.customColor;
        }
    }
}

