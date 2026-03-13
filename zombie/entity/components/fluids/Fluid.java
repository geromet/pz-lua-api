/*
 * Decompiled with CFR 0.152.
 */
package zombie.entity.components.fluids;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import zombie.GameWindow;
import zombie.UsedFromLua;
import zombie.core.Color;
import zombie.core.Core;
import zombie.debug.DebugType;
import zombie.debug.objects.DebugClass;
import zombie.debug.objects.DebugField;
import zombie.debug.objects.DebugMethod;
import zombie.debug.objects.DebugNonRecursive;
import zombie.entity.ComponentType;
import zombie.entity.components.fluids.FluidCategory;
import zombie.entity.components.fluids.FluidFilter;
import zombie.entity.components.fluids.FluidInstance;
import zombie.entity.components.fluids.FluidProperties;
import zombie.entity.components.fluids.FluidType;
import zombie.entity.components.fluids.PoisonEffect;
import zombie.entity.components.fluids.PoisonInfo;
import zombie.entity.components.fluids.SealedFluidProperties;
import zombie.scripting.ScriptLoadMode;
import zombie.scripting.ScriptManager;
import zombie.scripting.objects.FluidDefinitionScript;
import zombie.scripting.objects.Item;

@DebugClass
@UsedFromLua
public class Fluid {
    private static boolean hasInitialized;
    private static final HashMap<FluidType, Fluid> fluidEnumMap;
    private static final HashMap<String, Fluid> fluidStringMap;
    private static final HashMap<String, Fluid> cacheStringMap;
    private static final HashMap<FluidDefinitionScript, Fluid> scriptToFluidMap;
    private static final ArrayList<Fluid> allFluids;
    public static final Fluid Water;
    public static final Fluid TaintedWater;
    public static final Fluid Petrol;
    public static final Fluid Alcohol;
    public static final Fluid PoisonPotent;
    public static final Fluid Beer;
    public static final Fluid Whiskey;
    public static final Fluid SodaPop;
    public static final Fluid Coffee;
    public static final Fluid Tea;
    public static final Fluid Wine;
    public static final Fluid Bleach;
    public static final Fluid Blood;
    public static final Fluid Honey;
    public static final Fluid Mead;
    public static final Fluid Acid;
    public static final Fluid SpiffoJuice;
    public static final Fluid SecretFlavoring;
    public static final Fluid CarbonatedWater;
    public static final Fluid CleaningLiquid;
    public static final Fluid CowMilk;
    public static final Fluid SheepMilk;
    public static final Fluid AnimalBlood;
    public static final Fluid AnimalGrease;
    public static final Fluid Dye;
    public static final Fluid HairDye;
    public static final Fluid AnimalMilk;
    @DebugNonRecursive
    private FluidDefinitionScript script;
    @DebugField
    private final FluidType fluidType;
    @DebugField
    private final String fluidTypeStr;
    @DebugField
    private final Color color = new Color(1.0f, 1.0f, 1.0f, 1.0f);
    @DebugField
    private ImmutableSet<FluidCategory> categories;
    private String categoriesCacheStr;
    @DebugField
    private FluidFilter blendWhitelist;
    @DebugField
    private FluidFilter blendBlacklist;
    @DebugField
    private PoisonInfo poisonInfo;
    @DebugField
    private SealedFluidProperties properties;

    private static Fluid addFluid(FluidType type) {
        if (fluidEnumMap.containsKey((Object)type)) {
            throw new RuntimeException("Fluid defined twice: " + String.valueOf((Object)type));
        }
        Fluid fluid = new Fluid(type);
        fluidEnumMap.put(type, fluid);
        return fluid;
    }

    public static Fluid Get(FluidType type) {
        if (Core.debug && !hasInitialized) {
            throw new RuntimeException("Fluids have not yet been initialized!");
        }
        return fluidEnumMap.get((Object)type);
    }

    public static Fluid Get(String name) {
        if (Core.debug && !hasInitialized) {
            throw new RuntimeException("Fluids have not yet been initialized!");
        }
        return fluidStringMap.get(name);
    }

    public static ArrayList<Fluid> getAllFluids() {
        if (Core.debug && !hasInitialized) {
            throw new RuntimeException("Fluids have not yet been initialized!");
        }
        return allFluids;
    }

    public static ArrayList<Item> getAllFluidItemsDebug() {
        if (Core.debug && !hasInitialized) {
            throw new RuntimeException("Fluids have not yet been initialized!");
        }
        ArrayList<Item> items = ScriptManager.instance.getAllItems();
        ArrayList<Item> fluidItems = new ArrayList<Item>();
        for (Item item : items) {
            if (!item.containsComponent(ComponentType.FluidContainer)) continue;
            fluidItems.add(item);
        }
        return fluidItems;
    }

    public static boolean FluidsInitialized() {
        return hasInitialized;
    }

    public static void Init(ScriptLoadMode loadMode) throws Exception {
        Fluid fluid;
        DebugType.Fluid.println("*************************************");
        DebugType.Fluid.println("* Fluid: initialize Fluids.         *");
        DebugType.Fluid.println("*************************************");
        ArrayList<FluidDefinitionScript> scripts = ScriptManager.instance.getAllFluidDefinitionScripts();
        cacheStringMap.clear();
        scriptToFluidMap.clear();
        allFluids.clear();
        if (loadMode == ScriptLoadMode.Reload) {
            cacheStringMap.putAll(fluidStringMap);
            fluidStringMap.clear();
        }
        for (FluidDefinitionScript fluidDefinitionScript : scripts) {
            if (fluidDefinitionScript.getFluidType() == FluidType.Modded) {
                DebugType.Fluid.println(fluidDefinitionScript.getModID() + " = " + fluidDefinitionScript.getFluidTypeString());
                fluid = cacheStringMap.get(fluidDefinitionScript.getFluidTypeString());
                if (fluid == null) {
                    fluid = new Fluid(fluidDefinitionScript.getFluidTypeString());
                }
                fluid.setScript(fluidDefinitionScript);
                fluidStringMap.put(fluidDefinitionScript.getFluidTypeString(), fluid);
                scriptToFluidMap.put(fluidDefinitionScript, fluid);
                allFluids.add(fluid);
                continue;
            }
            DebugType.Fluid.println(fluidDefinitionScript.getModID() + " = " + String.valueOf((Object)fluidDefinitionScript.getFluidType()));
            fluid = fluidEnumMap.get((Object)fluidDefinitionScript.getFluidType());
            if (fluid == null) {
                if (!Core.debug) continue;
                throw new Exception("Fluid not found: " + String.valueOf((Object)fluidDefinitionScript.getFluidType()));
            }
            fluid.setScript(fluidDefinitionScript);
            scriptToFluidMap.put(fluidDefinitionScript, fluid);
            allFluids.add(fluid);
        }
        for (Map.Entry entry : fluidEnumMap.entrySet()) {
            if (Core.debug && ((Fluid)entry.getValue()).script == null) {
                throw new Exception("Fluid has no script set: " + String.valueOf(entry.getKey()));
            }
            fluidStringMap.put(((FluidType)((Object)entry.getKey())).toString(), (Fluid)entry.getValue());
        }
        cacheStringMap.clear();
        hasInitialized = true;
        for (FluidDefinitionScript fluidDefinitionScript : scripts) {
            fluid = scriptToFluidMap.get(fluidDefinitionScript);
            if (fluid == null) continue;
            if (fluidDefinitionScript.getBlendWhitelist() != null) {
                fluid.blendWhitelist = fluidDefinitionScript.getBlendWhitelist().createFilter();
                fluid.blendWhitelist.seal();
                DebugType.Fluid.debugln("[Created fluid blend whitelist: " + fluid.getFluidTypeString() + "]");
                DebugType.Fluid.debugln(fluid.blendWhitelist);
            }
            if (fluidDefinitionScript.getBlendBlackList() == null) continue;
            fluid.blendBlacklist = fluidDefinitionScript.getBlendBlackList().createFilter();
            fluid.blendBlacklist.seal();
            DebugType.Fluid.debugln("[Created fluid blend blacklist: " + fluid.getFluidTypeString() + "]");
            DebugType.Fluid.debugln(fluid.blendBlacklist);
        }
        DebugType.Fluid.println("*************************************");
    }

    public static void PreReloadScripts() {
        hasInitialized = false;
    }

    public static void Reset() {
        fluidStringMap.clear();
        scriptToFluidMap.clear();
        hasInitialized = false;
    }

    public static void saveFluid(Fluid fluid, ByteBuffer output) {
        output.put(fluid != null ? (byte)1 : 0);
        if (fluid == null) {
            return;
        }
        if (fluid.fluidType == FluidType.Modded) {
            output.put((byte)1);
            GameWindow.WriteString(output, fluid.fluidTypeStr);
        } else {
            output.put((byte)0);
            output.put(fluid.fluidType.getId());
        }
    }

    public static Fluid loadFluid(ByteBuffer input, int worldVersion) {
        Fluid fluid;
        if (input.get() == 0) {
            return null;
        }
        if (input.get() != 0) {
            String fluidTypeString = GameWindow.ReadString(input);
            fluid = Fluid.Get(fluidTypeString);
        } else {
            FluidType fluidType = FluidType.FromId(input.get());
            fluid = Fluid.Get(fluidType);
        }
        return fluid;
    }

    private Fluid(FluidType fluidType) {
        this.fluidType = Objects.requireNonNull(fluidType);
        this.fluidTypeStr = fluidType.toString();
    }

    private Fluid(String fluidTypeStr) {
        this.fluidType = FluidType.Modded;
        this.fluidTypeStr = Objects.requireNonNull(fluidTypeStr);
    }

    private void setScript(FluidDefinitionScript script) {
        this.script = Objects.requireNonNull(script);
        this.color.set(script.getColor());
        this.categories = Sets.immutableEnumSet(script.getCategories());
        this.categoriesCacheStr = this.categories.toString();
        if (script.hasPropertiesSet()) {
            FluidProperties props = new FluidProperties();
            props.setEffects(script.getFatigueChange(), script.getHungerChange(), script.getStressChange(), script.getThirstChange(), script.getUnhappyChange(), script.getAlcohol(), script.getPoisonMaxEffect().getPlayerEffect());
            props.setNutrients(script.getCalories(), script.getCarbohydrates(), script.getLipids(), script.getProteins());
            props.setAlcohol(script.getAlcohol());
            props.setReductions(script.getFluReduction(), script.getPainReduction(), script.getEnduranceChange(), script.getFoodSicknessChange());
            this.properties = props.getSealedFluidProperties();
        } else {
            this.properties = null;
        }
        this.poisonInfo = script.getPoisonMaxEffect() != PoisonEffect.None ? new PoisonInfo(this, script.getPoisonMinAmount(), script.getPoisonDiluteRatio(), script.getPoisonMaxEffect()) : null;
    }

    @DebugMethod
    public boolean isVanilla() {
        return this.script != null && this.script.isVanilla();
    }

    public String toString() {
        return this.fluidTypeStr;
    }

    public FluidInstance getInstance() {
        return FluidInstance.Alloc(this);
    }

    public FluidType getFluidType() {
        return this.fluidType;
    }

    public String getFluidTypeString() {
        return this.fluidTypeStr;
    }

    public Color getColor() {
        return this.color;
    }

    public ImmutableSet<FluidCategory> getCategories() {
        return this.categories;
    }

    public boolean isCategory(FluidCategory category) {
        return this.categories.contains((Object)category);
    }

    public String getDisplayName() {
        if (this.script != null) {
            return this.script.getDisplayName();
        }
        return "<unknown_fluid>";
    }

    public String getTranslatedName() {
        return this.getDisplayName();
    }

    public String getTranslatedNameLower() {
        return this.getDisplayName().toLowerCase();
    }

    public boolean canBlendWith(Fluid fluid) {
        if (fluid == this) {
            return true;
        }
        return !(this.blendWhitelist != null && !this.blendWhitelist.allows(fluid) || this.blendBlacklist != null && !this.blendBlacklist.allows(fluid));
    }

    public SealedFluidProperties getProperties() {
        return this.properties;
    }

    public PoisonInfo getPoisonInfo() {
        return this.poisonInfo;
    }

    public boolean isPoisonous() {
        return this.poisonInfo != null;
    }

    public FluidDefinitionScript getScript() {
        return this.script;
    }

    static {
        fluidEnumMap = new HashMap();
        fluidStringMap = new HashMap();
        cacheStringMap = new HashMap();
        scriptToFluidMap = new HashMap();
        allFluids = new ArrayList();
        Water = Fluid.addFluid(FluidType.Water);
        TaintedWater = Fluid.addFluid(FluidType.TaintedWater);
        Petrol = Fluid.addFluid(FluidType.Petrol);
        Alcohol = Fluid.addFluid(FluidType.RubbingAlcohol);
        PoisonPotent = Fluid.addFluid(FluidType.PoisonPotent);
        Beer = Fluid.addFluid(FluidType.Beer);
        Whiskey = Fluid.addFluid(FluidType.Whiskey);
        SodaPop = Fluid.addFluid(FluidType.SodaPop);
        Coffee = Fluid.addFluid(FluidType.Coffee);
        Tea = Fluid.addFluid(FluidType.Tea);
        Wine = Fluid.addFluid(FluidType.Wine);
        Bleach = Fluid.addFluid(FluidType.Bleach);
        Blood = Fluid.addFluid(FluidType.Blood);
        Honey = Fluid.addFluid(FluidType.Honey);
        Mead = Fluid.addFluid(FluidType.Mead);
        Acid = Fluid.addFluid(FluidType.Acid);
        SpiffoJuice = Fluid.addFluid(FluidType.SpiffoJuice);
        SecretFlavoring = Fluid.addFluid(FluidType.SecretFlavoring);
        CarbonatedWater = Fluid.addFluid(FluidType.CarbonatedWater);
        CleaningLiquid = Fluid.addFluid(FluidType.CleaningLiquid);
        CowMilk = Fluid.addFluid(FluidType.CowMilk);
        SheepMilk = Fluid.addFluid(FluidType.SheepMilk);
        AnimalBlood = Fluid.addFluid(FluidType.AnimalBlood);
        AnimalGrease = Fluid.addFluid(FluidType.AnimalGrease);
        Dye = Fluid.addFluid(FluidType.Dye);
        HairDye = Fluid.addFluid(FluidType.HairDye);
        AnimalMilk = Fluid.addFluid(FluidType.AnimalMilk);
    }
}

