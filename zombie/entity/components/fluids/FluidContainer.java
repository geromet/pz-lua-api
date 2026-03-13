/*
 * Decompiled with CFR 0.152.
 */
package zombie.entity.components.fluids;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Locale;
import zombie.GameWindow;
import zombie.SandboxOptions;
import zombie.UsedFromLua;
import zombie.characters.IsoGameCharacter;
import zombie.characters.IsoPlayer;
import zombie.core.Color;
import zombie.core.Core;
import zombie.core.Translator;
import zombie.core.math.PZMath;
import zombie.core.network.ByteBufferReader;
import zombie.core.random.Rand;
import zombie.core.textures.ColorInfo;
import zombie.debug.DebugLog;
import zombie.debug.objects.DebugClassFields;
import zombie.entity.Component;
import zombie.entity.ComponentType;
import zombie.entity.GameEntity;
import zombie.entity.components.fluids.Fluid;
import zombie.entity.components.fluids.FluidCategory;
import zombie.entity.components.fluids.FluidConsume;
import zombie.entity.components.fluids.FluidFilter;
import zombie.entity.components.fluids.FluidInstance;
import zombie.entity.components.fluids.FluidSample;
import zombie.entity.components.fluids.FluidType;
import zombie.entity.components.fluids.FluidUtil;
import zombie.entity.components.fluids.PoisonEffect;
import zombie.entity.components.fluids.PoisonInfo;
import zombie.entity.components.fluids.SealedFluidProperties;
import zombie.entity.network.EntityPacketType;
import zombie.inventory.InventoryItem;
import zombie.inventory.ItemContainer;
import zombie.inventory.types.Moveable;
import zombie.iso.objects.IsoWorldInventoryObject;
import zombie.network.IConnection;
import zombie.scripting.entity.ComponentScript;
import zombie.scripting.entity.components.fluids.FluidContainerScript;
import zombie.scripting.objects.Item;
import zombie.scripting.objects.ItemTag;
import zombie.ui.ObjectTooltip;
import zombie.util.StringUtils;
import zombie.util.io.BitHeader;
import zombie.util.io.BitHeaderRead;
import zombie.util.io.BitHeaderWrite;

@DebugClassFields
@UsedFromLua
public class FluidContainer
extends Component {
    public static final int MAX_FLUIDS = 8;
    public static final String DEF_CONTAINER_NAME = "FluidContainer";
    private static final Color colorDef = Color.white;
    private float capacity = 1.0f;
    private FluidFilter whitelist;
    private FluidFilter blacklist;
    private float temperature = 22.0f;
    private final ArrayList<FluidInstance> fluids = new ArrayList(8);
    private final SealedFluidProperties propertiesCache = new SealedFluidProperties();
    private float amountCache;
    private final Color color = new Color().set(colorDef);
    private String containerName = "FluidContainer";
    private String translatedContainerName;
    private String nameCache;
    private String customDrinkSound;
    private boolean cacheInvalidated;
    private boolean inputLocked;
    private boolean canPlayerEmpty = true;
    private boolean hiddenAmount;
    private float rainCatcher;
    private boolean fillsWithCleanWater;
    private static final ArrayList<FluidInstance> tempFluidUI = new ArrayList();
    private static final DecimalFormat df = new DecimalFormat("#.##", DecimalFormatSymbols.getInstance(Locale.ENGLISH));

    public static FluidContainer CreateContainer() {
        return (FluidContainer)ComponentType.FluidContainer.CreateComponent();
    }

    public static void DisposeContainer(FluidContainer container) {
        ComponentType.ReleaseComponent(container);
    }

    private FluidContainer() {
        super(ComponentType.FluidContainer);
    }

    protected void readFromScript(ComponentScript componentScript) {
        super.readFromScript(componentScript);
        FluidContainerScript script = (FluidContainerScript)componentScript;
        this.whitelist = script.getWhitelistCopy();
        this.blacklist = script.getBlacklistCopy();
        this.containerName = script.getContainerName();
        this.customDrinkSound = script.getCustomDrinkSound();
        this.rainCatcher = script.getRainCatcher();
        this.fillsWithCleanWater = script.isFilledWithCleanWater();
        this.inputLocked = false;
        this.canPlayerEmpty = true;
        this.setCapacity(script.getCapacity());
        if (script.getInitialFluids() != null && !script.getInitialFluids().isEmpty() && script.getInitialAmount() > 0.0f) {
            float initialAmount = script.getInitialAmount();
            if (script.getInitialFluids().size() == 1) {
                this.addInitialFluid(initialAmount, script.getInitialFluids().get(0), script.getName());
            } else if (script.isInitialFluidsIsRandom()) {
                int rnd = Rand.Next(script.getInitialFluids().size());
                this.addInitialFluid(initialAmount, script.getInitialFluids().get(rnd), script.getName());
            } else {
                for (int i = 0; i < script.getInitialFluids().size(); ++i) {
                    FluidContainerScript.FluidScript fs = script.getInitialFluids().get(i);
                    this.addInitialFluid(initialAmount, fs, script.getName());
                }
            }
        }
        this.inputLocked = script.getInputLocked();
        this.canPlayerEmpty = script.getCanEmpty();
        this.hiddenAmount = script.isHiddenAmount();
    }

    private void addInitialFluid(float initialAmount, FluidContainerScript.FluidScript fs, String scriptName) {
        Fluid fluid = fs.getFluid();
        if (fluid != null) {
            float amount = initialAmount * fs.getPercentage();
            FluidInstance fluidInstance = FluidInstance.Alloc(fluid);
            if (fs.getCustomColor() != null) {
                fluidInstance.setColor(fs.getCustomColor());
            }
            this.addFluid(fluidInstance, amount);
            FluidInstance.Release(fluidInstance);
        } else {
            DebugLog.log("FluidContainer -> initial Fluid '" + fs.getFluidType() + "' not found! [" + scriptName + "]");
        }
    }

    @Override
    protected void reset() {
        super.reset();
        this.Empty();
        this.capacity = 1.0f;
        this.whitelist = null;
        this.blacklist = null;
        this.temperature = 22.0f;
        this.propertiesCache.clear();
        this.amountCache = 0.0f;
        this.color.set(colorDef);
        this.containerName = DEF_CONTAINER_NAME;
        this.translatedContainerName = null;
        this.nameCache = null;
        this.cacheInvalidated = false;
        this.inputLocked = false;
        this.canPlayerEmpty = true;
        this.hiddenAmount = false;
        this.rainCatcher = 0.0f;
        this.fillsWithCleanWater = false;
        this.customDrinkSound = null;
    }

    public FluidContainer copy() {
        FluidContainer copy = FluidContainer.CreateContainer();
        copy.capacity = this.capacity;
        for (FluidInstance fi : this.fluids) {
            copy.addFluid(fi, fi.getAmount());
        }
        if (this.whitelist != null) {
            copy.whitelist = this.whitelist.copy();
        }
        if (this.blacklist != null) {
            copy.blacklist = this.blacklist.copy();
        }
        copy.containerName = this.containerName;
        copy.recalculateCaches(true);
        return copy;
    }

    public void copyFluidsFrom(FluidContainer other) {
        FluidInstance fi;
        int i;
        for (i = this.fluids.size() - 1; i >= 0; --i) {
            fi = this.fluids.get(i);
            FluidInstance.Release(fi);
        }
        this.fluids.clear();
        this.recalculateCaches(true);
        for (i = 0; i < other.fluids.size(); ++i) {
            fi = other.fluids.get(i);
            this.addFluid(fi, fi.getAmount());
        }
        this.recalculateCaches(true);
    }

    public String getCustomDrinkSound() {
        return this.customDrinkSound;
    }

    public void setInputLocked(boolean b) {
        this.inputLocked = b;
    }

    public boolean isInputLocked() {
        return this.inputLocked;
    }

    public boolean canPlayerEmpty() {
        return !this.isMultiTileMoveable() && this.canPlayerEmpty;
    }

    public void setCanPlayerEmpty(boolean b) {
        this.canPlayerEmpty = b;
    }

    public float getRainCatcher() {
        Moveable mov;
        IsoWorldInventoryObject obj;
        GameEntity gameEntity = this.getOwner();
        if (gameEntity instanceof IsoWorldInventoryObject && (gameEntity = (obj = (IsoWorldInventoryObject)gameEntity).getItem()) instanceof Moveable && (mov = (Moveable)gameEntity).getSpriteGrid() != null) {
            return 0.0f;
        }
        return this.rainCatcher;
    }

    public void setRainCatcher(float rainCatcher) {
        this.rainCatcher = rainCatcher;
    }

    public boolean isFilledWithCleanWater() {
        return this.fillsWithCleanWater;
    }

    public boolean isHiddenAmount() {
        return this.hiddenAmount;
    }

    @Override
    public void DoTooltip(ObjectTooltip tooltipUI) {
        ObjectTooltip.Layout layout = tooltipUI.beginLayout();
        layout.setMinLabelWidth(80);
        int y = tooltipUI.padTop;
        this.DoTooltip(tooltipUI, layout);
        y = layout.render(tooltipUI.padLeft, y, tooltipUI);
        tooltipUI.endLayout(layout);
        tooltipUI.setHeight(y += tooltipUI.padBottom);
        if (tooltipUI.getWidth() < 150.0) {
            tooltipUI.setWidth(150.0);
        }
    }

    @Override
    public void DoTooltip(ObjectTooltip tooltipUI, ObjectTooltip.Layout layout) {
        int i;
        if (layout == null) {
            return;
        }
        IsoGameCharacter player = tooltipUI.getCharacter();
        ObjectTooltip.LayoutItem item = layout.addItem();
        item.setLabel(Translator.getFluidText("Fluid_Amount") + ":", 1.0f, 1.0f, 0.8f, 1.0f);
        item.setValue(FluidUtil.getFractionFormatted(this.getAmount(), this.getCapacity()), 1.0f, 1.0f, 1.0f, 1.0f);
        if (this.isEmpty()) {
            return;
        }
        if (this.fluids.size() == 1) {
            FluidInstance fi = this.fluids.get(0);
            item = layout.addItem();
            item.setLabel(fi.getFluid().getTranslatedName() + ":", 1.0f, 1.0f, 0.8f, 1.0f);
            float f = fi.getAmount() / this.getCapacity();
            item.setProgress(f, fi.getColor().r, fi.getColor().g, fi.getColor().b, 1.0f);
        } else if (this.fluids.size() == 2 && this.fluids.get(0).getTranslatedName().equals(this.fluids.get(1).getTranslatedName())) {
            FluidInstance fi = this.fluids.get(0);
            item = layout.addItem();
            item.setLabel(fi.getFluid().getTranslatedName() + ":", 1.0f, 1.0f, 0.8f, 1.0f);
            float f = (fi.getAmount() + this.fluids.get(1).getAmount()) / this.getCapacity();
            item.setProgress(f, fi.getColor().r, fi.getColor().g, fi.getColor().b, 1.0f);
        } else {
            item = layout.addItem();
            item.setLabel(Translator.getFluidText("Fluid_Mixture") + ":", 1.0f, 1.0f, 0.8f, 1.0f);
            float f = this.getAmount() / this.getCapacity();
            item.setProgress(f, this.getColor().r, this.getColor().g, this.getColor().b, 1.0f);
            item = layout.addItem();
            item.setLabel(Translator.getFluidText("Fluid_Fluids") + ":", 1.0f, 1.0f, 0.8f, 1.0f);
            tempFluidUI.clear();
            for (int i2 = 0; i2 < this.fluids.size(); ++i2) {
                tempFluidUI.add(this.fluids.get(i2));
            }
            tempFluidUI.sort(Comparator.comparing(FluidInstance::getTranslatedName));
            for (i = 0; i < tempFluidUI.size(); ++i) {
                FluidInstance fi = tempFluidUI.get(i);
                item = layout.addItem();
                item.setLabel(fi.getFluid().getTranslatedName() + ":", 0.7f, 0.7f, 0.4f, 1.0f);
                f = fi.getAmount() / this.getAmount();
                item.setProgress(f, fi.getColor().r, fi.getColor().g, fi.getColor().b, 1.0f);
            }
        }
        if (this.isPoisonous() && this.getPoisonEffect() != PoisonEffect.None) {
            ColorInfo badColor = Core.getInstance().getBadHighlitedColor();
            item = layout.addItem();
            item.setLabel(Translator.getText("Tooltip_tainted"), badColor.getR(), badColor.getG(), badColor.getB(), 1.0f);
        }
        if (Core.debug) {
            if (this.propertiesCache.hasProperties()) {
                item = layout.addItem();
                item.setLabel(Translator.getFluidText("Fluid_Properties") + ":", 1.0f, 1.0f, 0.8f, 1.0f);
                float amountMilli = this.getAmount() * 1000.0f;
                this.doToolTipProp(layout, "Thirst", this.propertiesCache.getThirstChange() / 1000.0f * amountMilli);
                this.doToolTipProp(layout, "Hunger", this.propertiesCache.getHungerChange() / 1000.0f * amountMilli);
                this.doToolTipProp(layout, "Stress", this.propertiesCache.getStressChange() / 1000.0f * amountMilli);
                this.doToolTipProp(layout, "Unhappy", this.propertiesCache.getUnhappyChange() / 1000.0f * amountMilli);
                this.doToolTipProp(layout, "Fatigue", this.propertiesCache.getFatigueChange() / 1000.0f * amountMilli);
                this.doToolTipProp(layout, "Calories", this.propertiesCache.getCalories() / 1000.0f * amountMilli);
                this.doToolTipProp(layout, "Carbohydrates", this.propertiesCache.getCarbohydrates() / 1000.0f * amountMilli);
                this.doToolTipProp(layout, "Lipids", this.propertiesCache.getLipids() / 1000.0f * amountMilli);
                this.doToolTipProp(layout, "Proteins", this.propertiesCache.getProteins() / 1000.0f * amountMilli);
                this.doToolTipProp(layout, "Alcohol", this.propertiesCache.getAlcohol());
            }
            if (this.isPoisonous()) {
                String poison = Translator.getFluidText("Fluid_Poison") + " " + Translator.getFluidText("Fluid_Effect") + " " + Translator.getFluidText("Fluid_Per");
                poison = poison + " " + FluidUtil.getAmountFormatted(this.getAmount());
                item = layout.addItem();
                item.setLabel(poison + ":", 1.0f, 1.0f, 0.8f, 1.0f);
                PoisonEffect effect = this.getPoisonEffect();
                item = layout.addItem();
                item.setLabel(Translator.getFluidText("Fluid_Effect") + ":", 0.7f, 0.7f, 0.4f, 1.0f);
                item.setValue(Translator.getFluidText("Fluid_Poison_" + String.valueOf((Object)effect)), 0.85f, 0.85f, 0.85f, 1.0f);
            }
            item = layout.addItem();
            item.setLabel("Categories:", 1.0f, 1.0f, 0.8f, 1.0f);
            ArrayList<String> cats = new ArrayList<String>();
            for (i = 0; i < this.fluids.size(); ++i) {
                FluidInstance fi = this.fluids.get(i);
                for (FluidCategory cat : fi.getFluid().getCategories()) {
                    if (cats.contains(cat.toString())) continue;
                    cats.add(cat.toString());
                }
            }
            Collections.sort(cats);
            for (String s : cats) {
                item = layout.addItem();
                item.setLabel(s, 0.7f, 0.7f, 0.4f, 1.0f);
            }
        }
    }

    private void doToolTipProp(ObjectTooltip.Layout layout, String transKey, float value) {
        if (this.getAmount() == 0.0f) {
            return;
        }
        value /= this.getAmount();
        ObjectTooltip.LayoutItem item = layout.addItem();
        item.setLabel(Translator.getFluidText("Fluid_Prop_" + transKey) + ":", 0.7f, 0.7f, 0.4f, 1.0f);
        if (transKey.equals("Alcohol")) {
            value = (int)(value * 100.0f);
            item.setValue(df.format(value) + "%", 0.85f, 0.85f, 0.85f, 1.0f);
        } else {
            item.setValue(df.format(value), 0.85f, 0.85f, 0.85f, 1.0f);
        }
    }

    public String getContainerName() {
        return this.containerName;
    }

    public void setContainerName(String name) {
        if (StringUtils.containsWhitespace(name)) {
            DebugLog.General.error("Sanitizing container name '" + name + "', name may not contain whitespaces.");
            name = StringUtils.removeWhitespace(name);
        }
        this.containerName = name;
    }

    public String getTranslatedContainerName() {
        if (this.translatedContainerName == null) {
            this.translatedContainerName = Translator.getFluidText("Fluid_Container_" + this.containerName);
        }
        return this.translatedContainerName;
    }

    public String getUiName() {
        if (this.cacheInvalidated || this.nameCache == null) {
            this.recalculateCaches();
            String containerName = this.getTranslatedContainerName();
            if (this.fluids.isEmpty()) {
                this.nameCache = Translator.getText("Fluid_HoldingNone", containerName);
                InventoryItem item = null;
                if (this.getOwner() instanceof InventoryItem) {
                    item = (InventoryItem)this.getOwner();
                } else if (this.getOwner() instanceof IsoWorldInventoryObject) {
                    item = ((IsoWorldInventoryObject)this.getOwner()).getItem();
                }
                if (item != null && item.hasTag(ItemTag.OMIT_EMPTY_FROM_NAME)) {
                    this.nameCache = containerName;
                }
            } else if (this.fluids.size() == 1) {
                this.nameCache = Translator.getText("Fluid_HoldingOneType", containerName, this.fluids.get(0).getFluid().getTranslatedName());
                if (this.getPrimaryFluid() == Fluid.TaintedWater && SandboxOptions.instance.enableTaintedWaterText.getValue()) {
                    this.nameCache = this.nameCache + " " + Translator.getFluidText("Fluid_Tainted");
                }
                if (!this.canPlayerEmpty) {
                    this.nameCache = this.nameCache + " " + Translator.getFluidText("Fluid_Sealed");
                }
            } else if (this.fluids.size() == 2 && this.isPoisonous()) {
                this.nameCache = containerName + " " + Translator.getFluidText("Fluid_Of") + " ";
                FluidInstance a = this.fluids.get(0);
                FluidInstance b = this.fluids.get(1);
                this.nameCache = !a.getFluid().isCategory(FluidCategory.Poisons) ? Translator.getText("Fluid_HoldingOneType", containerName, a.getFluid().getTranslatedName()) : Translator.getText("Fluid_HoldingOneType", containerName, b.getFluid().getTranslatedName());
                if (this.contains(Fluid.TaintedWater) && this.getPoisonEffect() != PoisonEffect.None) {
                    this.nameCache = this.nameCache + " " + Translator.getFluidText("Fluid_Tainted");
                }
            } else if (this.fluids.size() == 2 && !this.isPoisonous()) {
                FluidInstance a = this.fluids.get(0);
                FluidInstance b = this.fluids.get(1);
                FluidInstance fluid = a.getAmount() < b.getAmount() ? a : b;
                this.nameCache = Translator.getText("Fluid_HoldingOneType", containerName, fluid.getFluid().getTranslatedName());
                if (!a.getTranslatedName().equals(b.getTranslatedName())) {
                    FluidInstance fluidB = a.getAmount() < b.getAmount() ? b : a;
                    this.nameCache = Translator.getText("Fluid_HoldingTwoTypes", containerName, fluid.getFluid().getTranslatedName(), fluidB.getFluid().getTranslatedName());
                }
                if (this.contains(Fluid.TaintedWater) && this.getPoisonEffect() != PoisonEffect.None) {
                    this.nameCache = this.nameCache + " " + Translator.getFluidText("Fluid_Tainted");
                }
            } else {
                boolean allBeverages = true;
                int alcoholics = 0;
                for (int i = 0; i < this.fluids.size(); ++i) {
                    FluidInstance fi = this.fluids.get(i);
                    if (!fi.getFluid().isCategory(FluidCategory.Beverage)) {
                        allBeverages = fi.getFluid().isCategory(FluidCategory.Poisons);
                    }
                    if (!fi.getFluid().isCategory(FluidCategory.Beverage) || !fi.getFluid().isCategory(FluidCategory.Alcoholic)) continue;
                    ++alcoholics;
                }
                this.nameCache = allBeverages ? (alcoholics >= 2 ? Translator.getText("Fluid_HoldingOneType", containerName, Translator.getFluidText("Fluid_Cocktail")) : Translator.getText("Fluid_HoldingOneType", containerName, Translator.getFluidText("Fluid_Mixed_Beverages"))) : Translator.getText("Fluid_HoldingOneType", containerName, Translator.getFluidText("Fluid_Mixed_Fluids"));
            }
        }
        return this.nameCache;
    }

    public SealedFluidProperties getProperties() {
        this.recalculateCaches();
        return this.propertiesCache;
    }

    public boolean isEmpty() {
        return this.getAmount() == 0.0f;
    }

    public boolean isFull() {
        return PZMath.equal(this.getAmount(), this.capacity, 1.0E-4f);
    }

    public float getCapacity() {
        return this.capacity;
    }

    public float getFreeCapacity() {
        float free = this.capacity - this.getAmount();
        if (PZMath.equal(free, 0.0f, 1.0E-4f)) {
            return 0.0f;
        }
        return free;
    }

    public float getFilledRatio() {
        if (this.isFull()) {
            return 1.0f;
        }
        if (this.isEmpty()) {
            return 0.0f;
        }
        return this.capacity != 0.0f ? this.getAmount() / this.capacity : 0.0f;
    }

    protected void invalidateColor() {
        this.cacheInvalidated = true;
    }

    public Color getColor() {
        this.recalculateCaches();
        return this.color;
    }

    public float getAmount() {
        this.recalculateCaches();
        return this.amountCache;
    }

    private void recalculateCaches() {
        this.recalculateCaches(false, false);
    }

    private void recalculateCaches(boolean force) {
        this.recalculateCaches(force, false);
    }

    private void recalculateCaches(boolean force, boolean removeInvalid) {
        if (this.cacheInvalidated || force) {
            ItemContainer itemContainer;
            InventoryItem item;
            GameEntity multi2;
            FluidInstance fluid;
            int i;
            this.amountCache = 0.0f;
            float r = 0.0f;
            float g = 0.0f;
            float b = 0.0f;
            for (i = this.fluids.size() - 1; i >= 0; --i) {
                fluid = this.fluids.get(i);
                if (removeInvalid && this.removeFluidInstanceIfEmpty(fluid)) continue;
                this.amountCache += fluid.getAmount();
            }
            this.propertiesCache.clear();
            if (this.amountCache > 0.0f) {
                for (i = 0; i < this.fluids.size(); ++i) {
                    fluid = this.fluids.get(i);
                    fluid.setPercentage(fluid.getAmount() / this.amountCache);
                    r += fluid.getColor().r * fluid.getPercentage();
                    g += fluid.getColor().g * fluid.getPercentage();
                    b += fluid.getColor().b * fluid.getPercentage();
                    if (fluid.getFluid().getProperties() == null) continue;
                    float multi2 = this.amountCache * fluid.getPercentage();
                    this.propertiesCache.addFromMultiplied(fluid.getFluid().getProperties(), multi2);
                }
            }
            if (this.amountCache > 0.0f) {
                this.color.set(r, g, b);
            } else {
                this.color.set(colorDef);
            }
            if (this.getCapacity() - this.amountCache < 0.001f) {
                this.amountCache = this.getCapacity();
            }
            this.nameCache = null;
            this.cacheInvalidated = false;
            if (this.owner != null && (multi2 = this.owner) instanceof InventoryItem && (item = (InventoryItem)multi2).isInPlayerInventory() && (itemContainer = item.getContainer()) != null) {
                itemContainer.setDrawDirty(true);
                itemContainer.setDirty(true);
            }
            if (this.getGameEntity() != null) {
                this.getGameEntity().onFluidContainerUpdate();
            }
        }
    }

    private float getPoisonAmount() {
        float poisonAmount = 0.0f;
        for (int i = 0; i < this.fluids.size(); ++i) {
            FluidInstance fluid = this.fluids.get(i);
            if (!fluid.getFluid().isPoisonous()) continue;
            poisonAmount += fluid.getAmount();
        }
        return poisonAmount;
    }

    public float getPoisonRatio() {
        float poisonAmount = this.getPoisonAmount();
        if (poisonAmount > 0.0f) {
            float amount = this.getAmount();
            if (amount > 0.0f && amount >= poisonAmount) {
                return poisonAmount / amount;
            }
            return 1.0f;
        }
        return 0.0f;
    }

    public boolean isPoisonous() {
        for (int i = 0; i < this.fluids.size(); ++i) {
            if (!this.fluids.get(i).getFluid().isPoisonous()) continue;
            return true;
        }
        return false;
    }

    public PoisonEffect getPoisonEffect() {
        PoisonEffect effect = PoisonEffect.None;
        for (int i = 0; i < this.fluids.size(); ++i) {
            PoisonInfo poison;
            PoisonEffect poisonEffect;
            FluidInstance fluid = this.fluids.get(i);
            if (fluid.getFluid().getPoisonInfo() == null || (poisonEffect = (poison = fluid.getFluid().getPoisonInfo()).getPoisonEffect(this.getAmount() * fluid.getPercentage(), fluid.getPercentage())).getLevel() <= effect.getLevel()) continue;
            effect = poisonEffect;
        }
        return effect;
    }

    public boolean isTainted() {
        for (int i = 0; i < this.fluids.size(); ++i) {
            if (this.fluids.get(i).getFluid().getFluidType() != FluidType.TaintedWater) continue;
            return true;
        }
        return false;
    }

    public void setCapacity(float capacity) {
        float oldCapacity = this.capacity;
        this.capacity = PZMath.max(capacity, 0.05f);
        if (this.getAmount() > this.capacity) {
            for (int i = this.fluids.size() - 1; i >= 0; --i) {
                FluidInstance fluid = this.fluids.get(i);
                float ratio = fluid.getAmount() / oldCapacity;
                fluid.setAmount(this.capacity * ratio);
                this.removeFluidInstanceIfEmpty(fluid);
            }
        }
        this.cacheInvalidated = true;
    }

    public void adjustAmount(float newAmount) {
        if (this.isEmpty()) {
            return;
        }
        newAmount = PZMath.clamp(newAmount, 0.0f, this.getCapacity());
        float ratio = newAmount / this.getAmount();
        for (int i = this.fluids.size() - 1; i >= 0; --i) {
            FluidInstance fluid = this.fluids.get(i);
            float fluidAmount = fluid.getAmount() * ratio;
            fluid.setAmount(fluidAmount);
            this.removeFluidInstanceIfEmpty(fluid);
        }
        this.cacheInvalidated = true;
    }

    public void adjustSpecificFluidAmount(Fluid fluid, float newAmount) {
        if (this.isEmpty()) {
            return;
        }
        newAmount = PZMath.clamp(newAmount, 0.0f, this.getCapacity());
        for (int i = this.fluids.size() - 1; i >= 0; --i) {
            FluidInstance tempFluid = this.fluids.get(i);
            if (tempFluid.getFluid() != fluid) continue;
            tempFluid.setAmount(newAmount);
            this.removeFluidInstanceIfEmpty(tempFluid);
        }
        this.cacheInvalidated = true;
    }

    public float getSpecificFluidAmount(Fluid fluid) {
        if (this.isEmpty()) {
            return 0.0f;
        }
        FluidInstance test = null;
        for (int i = 0; i < this.fluids.size(); ++i) {
            if (this.fluids.get(i).getFluid() != fluid) continue;
            test = this.fluids.get(i);
        }
        if (test == null) {
            return 0.0f;
        }
        return test.getAmount();
    }

    public FluidSample createFluidSample() {
        return this.createFluidSample(this.getAmount());
    }

    public FluidSample createFluidSample(float scaleAmount) {
        FluidSample sample = FluidSample.Alloc();
        for (int i = 0; i < this.fluids.size(); ++i) {
            FluidInstance fluidInstance = this.fluids.get(i);
            sample.addFluid(fluidInstance);
        }
        if (scaleAmount != this.getAmount()) {
            sample.scaleToAmount(scaleAmount);
        }
        return sample.seal();
    }

    public FluidSample createFluidSample(FluidSample sample, float scaleAmount) {
        sample.clear();
        for (int i = 0; i < this.fluids.size(); ++i) {
            FluidInstance fluidInstance = this.fluids.get(i);
            sample.addFluid(fluidInstance);
        }
        if (scaleAmount != this.getAmount()) {
            sample.scaleToAmount(scaleAmount);
        }
        return sample.seal();
    }

    public boolean isPureFluid(Fluid fluid) {
        return this.fluids.size() == 1 && this.fluids.get(0).getFluid() == fluid;
    }

    public boolean isPrimaryFluidType(FluidType fluidType) {
        Fluid fluid = this.getPrimaryFluid();
        return fluid != null && fluid.getFluidType() == fluidType;
    }

    public boolean isPrimaryFluidType(String fluidType) {
        Fluid fluid = this.getPrimaryFluid();
        return fluid != null && StringUtils.equals(fluid.getFluidTypeString(), fluidType);
    }

    public Fluid getPrimaryFluid() {
        if (this.isEmpty()) {
            return null;
        }
        if (this.fluids.size() == 1) {
            return this.fluids.get(0).getFluid();
        }
        FluidInstance primary = null;
        for (int i = 0; i < this.fluids.size(); ++i) {
            FluidInstance test = this.fluids.get(i);
            if (primary != null && !(test.getAmount() > primary.getAmount())) continue;
            primary = test;
        }
        return primary.getFluid();
    }

    public float getPrimaryFluidAmount() {
        if (this.isEmpty()) {
            return 0.0f;
        }
        if (this.fluids.size() == 1) {
            return this.fluids.get(0).getAmount();
        }
        FluidInstance primary = null;
        for (int i = 0; i < this.fluids.size(); ++i) {
            FluidInstance test = this.fluids.get(i);
            if (primary != null && !(test.getAmount() > primary.getAmount())) continue;
            primary = test;
        }
        return primary.getAmount();
    }

    public boolean isPrimaryFluid(Fluid fluid) {
        return this.getPrimaryFluid() == fluid;
    }

    public boolean isWaterSource() {
        return !this.isEmpty() && (this.isPrimaryFluid(Fluid.Water) || this.isPrimaryFluid(Fluid.TaintedWater) || this.isPrimaryFluid(Fluid.CarbonatedWater));
    }

    public boolean isPerceivedFluidToPlayer(Fluid fluid, IsoGameCharacter character) {
        return !this.isEmpty();
    }

    public boolean isMixture() {
        return this.fluids.size() > 1;
    }

    public FluidFilter getWhitelist() {
        return this.whitelist;
    }

    public FluidFilter getBlacklist() {
        return this.blacklist;
    }

    public void Empty() {
        this.Empty(true);
    }

    public void Empty(boolean bRecalculate) {
        for (int i = 0; i < this.fluids.size(); ++i) {
            FluidInstance.Release(this.fluids.get(i));
        }
        this.fluids.clear();
        this.propertiesCache.clear();
        this.amountCache = 0.0f;
        this.color.set(colorDef);
        if (bRecalculate) {
            this.recalculateCaches(true);
        }
    }

    private FluidInstance getFluidInstance(Fluid fluid) {
        if (this.fluids.isEmpty()) {
            return null;
        }
        for (int i = 0; i < this.fluids.size(); ++i) {
            FluidInstance fluidInstance = this.fluids.get(i);
            if (fluidInstance.getFluid() != fluid) continue;
            return fluidInstance;
        }
        return null;
    }

    public boolean canAddFluid(Fluid fluid) {
        if (this.fluids.size() >= 8 && !this.contains(fluid) || this.inputLocked || !this.canPlayerEmpty) {
            return false;
        }
        for (int i = 0; i < this.fluids.size(); ++i) {
            FluidInstance fluidInstance = this.fluids.get(i);
            if (fluidInstance.getFluid().canBlendWith(fluid) && fluid.canBlendWith(fluidInstance.getFluid())) continue;
            return false;
        }
        return !(this.whitelist != null && !this.whitelist.allows(fluid) || this.blacklist != null && !this.blacklist.allows(fluid));
    }

    public void addFluid(String fluidType, float amount) {
        this.addFluid(Fluid.Get(fluidType), amount);
    }

    public void addFluid(FluidType fluidType, float amount) {
        this.addFluid(Fluid.Get(fluidType), amount);
    }

    public void addFluid(Fluid fluid, float amount) {
        if (fluid != null) {
            FluidInstance fluidInstance = fluid.getInstance();
            this.addFluid(fluidInstance, amount);
            FluidInstance.Release(fluidInstance);
        }
    }

    private void addFluid(FluidInstance addInstance, float add) {
        if ((add = PZMath.max(0.0f, add)) > 0.0f && !this.isFull() && this.canAddFluid(addInstance.getFluid())) {
            float available = this.capacity - this.getAmount();
            add = PZMath.min(add, available);
            FluidInstance fluidInstance = this.getFluidInstance(addInstance.getFluid());
            if (fluidInstance == null) {
                fluidInstance = addInstance.getFluid().getInstance();
                fluidInstance.setParent(this);
                fluidInstance.setColor(addInstance.getColor());
                this.fluids.add(fluidInstance);
            } else if (fluidInstance.getFluid().isCategory(FluidCategory.Colors) && !addInstance.getColor().equals(fluidInstance.getColor())) {
                fluidInstance.mixColor(addInstance.getColor(), add);
            }
            fluidInstance.setAmount(fluidInstance.getAmount() + add);
            this.cacheInvalidated = true;
        }
    }

    public void removeFluid() {
        this.removeFluid(this.getAmount(), false);
    }

    public FluidConsume removeFluid(boolean createFluidConsume) {
        return this.removeFluid(this.getAmount(), createFluidConsume);
    }

    public void removeFluid(float remove) {
        this.removeFluid(remove, false);
    }

    public FluidConsume removeFluid(float remove, boolean createFluidConsume) {
        return this.removeFluid(remove, createFluidConsume, null);
    }

    public FluidConsume removeFluid(float remove, boolean createFluidConsume, FluidConsume fluidConsume) {
        FluidConsume consume = fluidConsume;
        if (consume != null) {
            consume.clear();
        } else if (createFluidConsume) {
            consume = FluidConsume.Alloc();
        }
        remove = PZMath.max(0.0f, remove);
        if (remove > 0.0f && !this.isEmpty()) {
            remove = PZMath.min(remove, this.getAmount());
            if (consume != null) {
                consume.setAmount(remove);
            }
            for (int i = this.fluids.size() - 1; i >= 0; --i) {
                FluidInstance fluid = this.fluids.get(i);
                float fluidRemoveAmount = remove * fluid.getPercentage();
                if (consume != null) {
                    if (fluid.getFluid().getPoisonInfo() != null) {
                        PoisonInfo poison = fluid.getFluid().getPoisonInfo();
                        PoisonEffect effect = poison.getPoisonEffect(fluidRemoveAmount, fluid.getPercentage());
                        consume.setPoisonEffect(effect);
                    }
                    if (fluid.getFluid().getProperties() != null) {
                        SealedFluidProperties props = fluid.getFluid().getProperties();
                        consume.addFromMultiplied(props, fluidRemoveAmount);
                    }
                }
                fluid.setAmount(fluid.getAmount() - fluidRemoveAmount);
                this.removeFluidInstanceIfEmpty(fluid);
            }
            this.cacheInvalidated = true;
        }
        return consume;
    }

    private boolean removeFluidInstanceIfEmpty(FluidInstance fluid) {
        if (PZMath.equal(fluid.getAmount(), 0.0f, 1.0E-4f)) {
            this.fluids.remove(fluid);
            FluidInstance.Release(fluid);
            return true;
        }
        return false;
    }

    public boolean contains(Fluid fluid) {
        for (int i = 0; i < this.fluids.size(); ++i) {
            if (this.fluids.get(i).getFluid() != fluid) continue;
            return true;
        }
        return false;
    }

    public float getRatioForFluid(Fluid fluid) {
        for (int i = 0; i < this.fluids.size(); ++i) {
            if (this.fluids.get(i).getFluid() != fluid) continue;
            return this.fluids.get(i).getPercentage();
        }
        return 0.0f;
    }

    public boolean isCategory(FluidCategory category) {
        for (int i = 0; i < this.fluids.size(); ++i) {
            FluidInstance fi = this.fluids.get(i);
            if (!fi.getFluid().isCategory(category)) continue;
            return true;
        }
        return false;
    }

    public boolean isAllCategory(FluidCategory category) {
        for (int i = 0; i < this.fluids.size(); ++i) {
            FluidInstance fi = this.fluids.get(i);
            if (fi.getFluid().isCategory(category)) continue;
            return false;
        }
        return true;
    }

    public void transferTo(FluidContainer other) {
        this.transferTo(other, this.getAmount());
    }

    public void transferTo(FluidContainer other, float amount) {
        FluidContainer.Transfer(this, other, amount);
    }

    public void transferFrom(FluidContainer other) {
        this.transferFrom(other, other.getAmount());
    }

    public void transferFrom(FluidContainer other, float amount) {
        FluidContainer.Transfer(other, this, amount);
    }

    public static String GetTransferReason(FluidContainer source2, FluidContainer target) {
        return FluidContainer.GetTransferReason(source2, target, false);
    }

    public static String GetTransferReason(FluidContainer source2, FluidContainer target, boolean testFirst) {
        if (testFirst && FluidContainer.CanTransfer(source2, target)) {
            return Translator.getFluidText("Fluid_Reason_Allowed");
        }
        if (source2 == null) {
            return Translator.getFluidText("Fluid_Reason_Source_Null");
        }
        if (target == null) {
            return Translator.getFluidText("Fluid_Reason_Target_Null");
        }
        if (source2 == target) {
            return Translator.getFluidText("Fluid_Reason_Equal");
        }
        if (source2.isEmpty()) {
            return Translator.getFluidText("Fluid_Reason_Source_Empty");
        }
        if (target.isFull()) {
            return Translator.getFluidText("Fluid_Reason_Target_Full");
        }
        if (target.isInputLocked()) {
            return Translator.getFluidText("Fluid_Reason_Target_Locked");
        }
        if (target.whitelist != null || target.blacklist != null) {
            boolean allows = true;
            for (int i = 0; i < source2.fluids.size(); ++i) {
                FluidInstance fluid = source2.fluids.get(i);
                if (target.whitelist != null && !target.whitelist.allows(fluid.getFluid())) {
                    allows = false;
                    break;
                }
                if (target.blacklist == null || target.blacklist.allows(fluid.getFluid())) continue;
                allows = false;
                break;
            }
            if (!allows) {
                return Translator.getFluidText("Fluid_Reason_Target_Filter");
            }
        }
        return Translator.getFluidText("Fluid_Reason_Mixing_Locked");
    }

    public static boolean CanTransfer(FluidContainer source2, FluidContainer target) {
        IsoPlayer player;
        InventoryItem item;
        if (source2 == null || target == null) {
            return false;
        }
        if (source2 == target) {
            return false;
        }
        GameEntity gameEntity = target.getOwner();
        if (gameEntity instanceof InventoryItem && (item = (InventoryItem)gameEntity).getContainer() != null && (gameEntity = item.getContainer().getParent()) instanceof IsoPlayer && (player = (IsoPlayer)gameEntity).hasFullInventory()) {
            return false;
        }
        if (!(source2.isEmpty() || target.isFull() || target.inputLocked)) {
            int equalFluids = 0;
            for (int i = 0; i < source2.fluids.size(); ++i) {
                FluidInstance fluid = source2.fluids.get(i);
                if (!target.canAddFluid(fluid.getFluid())) {
                    return false;
                }
                if (!target.contains(fluid.getFluid())) continue;
                ++equalFluids;
            }
            int totalFluids = equalFluids + source2.fluids.size() - equalFluids + target.fluids.size() - equalFluids;
            return totalFluids <= 8;
        }
        return false;
    }

    public static void Transfer(FluidContainer source2, FluidContainer target) {
        FluidContainer.Transfer(source2, target, source2.getAmount());
    }

    public static void Transfer(FluidContainer source2, FluidContainer target, float amount) {
        FluidContainer.Transfer(source2, target, amount, false);
    }

    public static void Transfer(FluidContainer source2, FluidContainer target, float amount, boolean keepSource) {
        if (FluidContainer.CanTransfer(source2, target)) {
            float transfer = PZMath.min(amount, source2.getAmount());
            float available = target.getCapacity() - target.getAmount();
            transfer = PZMath.min(transfer, available);
            for (int i = source2.fluids.size() - 1; i >= 0; --i) {
                FluidInstance fluid = source2.fluids.get(i);
                float fluidTransfer = transfer * fluid.getPercentage();
                target.addFluid(fluid, fluidTransfer);
                if (keepSource) continue;
                fluid.setAmount(fluid.getAmount() - fluidTransfer);
                source2.removeFluidInstanceIfEmpty(fluid);
            }
            if (!keepSource) {
                source2.recalculateCaches(true);
            }
            target.recalculateCaches(true);
        }
    }

    @Override
    protected boolean onReceivePacket(ByteBufferReader input, EntityPacketType type, IConnection senderConnection) throws IOException {
        switch (type) {
            default: 
        }
        return false;
    }

    @Override
    protected void saveSyncData(ByteBuffer output) throws IOException {
        this.save(output);
    }

    @Override
    protected void loadSyncData(ByteBuffer input) throws IOException {
        this.load(input, 244);
    }

    @Override
    public void save(ByteBuffer output) throws IOException {
        super.save(output);
        BitHeaderWrite header = BitHeader.allocWrite(BitHeader.HeaderSize.Short, output);
        if (this.capacity != 1.0f) {
            header.addFlags(1);
            output.putFloat(this.capacity);
        }
        if (!this.fluids.isEmpty()) {
            header.addFlags(2);
            if (this.fluids.size() == 1) {
                header.addFlags(4);
                FluidInstance.save(this.fluids.get(0), output);
            } else {
                output.put((byte)this.fluids.size());
                for (int i = 0; i < this.fluids.size(); ++i) {
                    FluidInstance fi = this.fluids.get(i);
                    FluidInstance.save(fi, output);
                }
            }
        }
        if (this.whitelist != null) {
            header.addFlags(8);
            this.whitelist.save(output);
        }
        if (this.blacklist != null) {
            header.addFlags(16);
            this.blacklist.save(output);
        }
        if (this.inputLocked) {
            header.addFlags(32);
        }
        if (this.canPlayerEmpty) {
            header.addFlags(64);
        }
        if (!this.containerName.equals(DEF_CONTAINER_NAME)) {
            header.addFlags(128);
            GameWindow.WriteString(output, this.containerName);
        }
        if (this.hiddenAmount) {
            header.addFlags(256);
        }
        if (this.rainCatcher > 0.0f) {
            header.addFlags(512);
            output.putFloat(this.rainCatcher);
        }
        header.write();
        header.release();
    }

    @Override
    public void load(ByteBuffer input, int worldVersion) throws IOException {
        super.load(input, worldVersion);
        this.amountCache = 0.0f;
        this.capacity = 1.0f;
        this.whitelist = null;
        this.blacklist = null;
        this.inputLocked = false;
        this.canPlayerEmpty = false;
        this.hiddenAmount = false;
        this.Empty(false);
        BitHeaderRead header = BitHeader.allocRead(BitHeader.HeaderSize.Short, input);
        if (header.hasFlags(1)) {
            this.capacity = input.getFloat();
        }
        if (header.hasFlags(2)) {
            if (header.hasFlags(4)) {
                FluidInstance fi = FluidInstance.load(input, worldVersion);
                if (fi.getFluid() != null) {
                    fi.setParent(this);
                    this.fluids.add(fi);
                }
            } else {
                int count = input.get();
                for (int i = 0; i < count; ++i) {
                    FluidInstance fi = FluidInstance.load(input, worldVersion);
                    if (fi.getFluid() == null) continue;
                    fi.setParent(this);
                    this.fluids.add(fi);
                }
            }
        }
        if (header.hasFlags(8)) {
            this.whitelist = new FluidFilter();
            this.whitelist.load(input, worldVersion);
        }
        if (header.hasFlags(16)) {
            this.blacklist = new FluidFilter();
            this.blacklist.load(input, worldVersion);
        }
        this.inputLocked = header.hasFlags(32);
        this.canPlayerEmpty = header.hasFlags(64);
        this.containerName = header.hasFlags(128) ? GameWindow.ReadString(input) : DEF_CONTAINER_NAME;
        this.hiddenAmount = header.hasFlags(256);
        if (header.hasFlags(512)) {
            this.rainCatcher = input.getFloat();
        }
        header.release();
        this.recalculateCaches(true);
    }

    public void unsealIfNotFull() {
        if (this.getFreeCapacity() > 0.0f) {
            this.unseal();
        }
    }

    public void unseal() {
        InventoryItem item = null;
        if (this.getOwner() instanceof InventoryItem) {
            item = (InventoryItem)this.getOwner();
        } else if (this.getOwner() instanceof IsoWorldInventoryObject) {
            item = ((IsoWorldInventoryObject)this.getOwner()).getItem();
        }
        this.setCanPlayerEmpty(true);
        FluidContainer copy = this.copy();
        this.copyFluidsFrom(copy);
        assert (item != null);
        item.sendSyncEntity(null);
        FluidContainer.DisposeContainer(copy);
    }

    @Override
    public boolean isQualifiesForMetaStorage() {
        return this.getRainCatcher() > 0.0f;
    }

    public void setWhitelist(FluidFilter ff) {
        this.whitelist = ff;
    }

    public boolean isTaintedStatusKnown() {
        if (this.getPrimaryFluid() == Fluid.Bleach) {
            return true;
        }
        if (!SandboxOptions.instance.enableTaintedWaterText.getValue()) {
            return false;
        }
        return (this.contains(Fluid.TaintedWater) || this.contains(Fluid.Bleach)) && this.getPoisonEffect() != PoisonEffect.None;
    }

    public void setNonSavedFieldsFromItemScript(InventoryItem item) {
        Item itemScript = item.getScriptItem();
        FluidContainerScript componentScript = (FluidContainerScript)itemScript.getComponentScriptFor(ComponentType.FluidContainer);
        if (componentScript == null) {
            return;
        }
        this.customDrinkSound = componentScript.getCustomDrinkSound();
        this.fillsWithCleanWater = componentScript.isFilledWithCleanWater();
    }

    public boolean isMultiTileMoveable() {
        Moveable mov2;
        InventoryItem item;
        Moveable mov;
        IsoWorldInventoryObject obj;
        GameEntity gameEntity = this.getOwner();
        return gameEntity instanceof IsoWorldInventoryObject && (gameEntity = (obj = (IsoWorldInventoryObject)gameEntity).getItem()) instanceof Moveable && (mov = (Moveable)gameEntity).getSpriteGrid() != null || (gameEntity = this.getOwner()) instanceof InventoryItem && (item = (InventoryItem)gameEntity) instanceof Moveable && (mov2 = (Moveable)item).getSpriteGrid() != null;
    }
}

