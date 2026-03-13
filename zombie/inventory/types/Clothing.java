/*
 * Decompiled with CFR 0.152.
 */
package zombie.inventory.types;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import zombie.GameTime;
import zombie.GameWindow;
import zombie.Lua.LuaEventManager;
import zombie.SandboxOptions;
import zombie.UsedFromLua;
import zombie.characterTextures.BloodBodyPartType;
import zombie.characterTextures.BloodClothingType;
import zombie.characters.IsoGameCharacter;
import zombie.characters.IsoPlayer;
import zombie.characters.WornItems.WornItem;
import zombie.characters.WornItems.WornItems;
import zombie.characters.skills.PerkFactory;
import zombie.core.Color;
import zombie.core.Core;
import zombie.core.Translator;
import zombie.core.math.PZMath;
import zombie.core.random.Rand;
import zombie.core.skinnedmodel.visual.ItemVisual;
import zombie.core.textures.ColorInfo;
import zombie.debug.DebugOptions;
import zombie.inventory.InventoryItem;
import zombie.inventory.InventoryItemFactory;
import zombie.iso.IsoObject;
import zombie.iso.IsoWorld;
import zombie.iso.objects.IsoClothingDryer;
import zombie.iso.objects.IsoClothingWasher;
import zombie.iso.objects.IsoCombinationWasherDryer;
import zombie.iso.objects.IsoDeadBody;
import zombie.iso.weather.ClimateManager;
import zombie.network.GameServer;
import zombie.network.PacketTypes;
import zombie.network.packets.INetworkPacket;
import zombie.scripting.ScriptManager;
import zombie.scripting.objects.Item;
import zombie.scripting.objects.ItemBodyLocation;
import zombie.scripting.objects.ItemTag;
import zombie.scripting.objects.ItemType;
import zombie.ui.ObjectTooltip;
import zombie.util.StringUtils;
import zombie.util.Type;
import zombie.util.io.BitHeader;
import zombie.util.io.BitHeaderRead;
import zombie.util.io.BitHeaderWrite;
import zombie.vehicles.BaseVehicle;
import zombie.vehicles.VehiclePart;
import zombie.vehicles.VehicleWindow;

@UsedFromLua
public class Clothing
extends InventoryItem {
    private float temperature;
    private float insulation;
    private float windresistance;
    private float waterResistance;
    private HashMap<Integer, ClothingPatch> patches;
    protected String spriteName;
    protected String palette;
    public float bloodLevel;
    private float dirtyness;
    private float wetness;
    private float weightWet;
    private float lastWetnessUpdate;
    private final String dirtyString = Translator.getText("IGUI_ClothingName_Dirty");
    private final String bloodyString = Translator.getText("IGUI_ClothingName_Bloody");
    private final String wetString = Translator.getText("IGUI_ClothingName_Wet");
    private final String soakedString = Translator.getText("IGUI_ClothingName_Soaked");
    private final String wornString = Translator.getText("IGUI_ClothingName_Worn");
    private final String brokenString = Translator.getText("Tooltip_broken");
    private int conditionLowerChance = 10;
    private float stompPower = 1.0f;
    private float runSpeedModifier = 1.0f;
    private float combatSpeedModifier = 1.0f;
    private Boolean removeOnBroken = false;
    private Boolean canHaveHoles = true;
    private float biteDefense;
    private float scratchDefense;
    private float bulletDefense;
    public static final int CONDITION_PER_HOLES = 3;
    private float neckProtectionModifier = 1.0f;
    private int chanceToFall;

    @Override
    public String getCategory() {
        if (this.mainCategory != null) {
            return this.mainCategory;
        }
        return "Clothing";
    }

    public Clothing(String module, String name, String itemType, String texName, String palette, String spriteName) {
        super(module, name, itemType, texName);
        this.spriteName = spriteName;
        this.col = new Color(Rand.Next(255), Rand.Next(255), Rand.Next(255));
        this.palette = palette;
        this.lastWetnessUpdate = (float)GameTime.getInstance().getWorldAgeHours();
        this.itemType = ItemType.CLOTHING;
    }

    public Clothing(String module, String name, String itemType, Item item, String palette, String spriteName) {
        super(module, name, itemType, item);
        this.spriteName = spriteName;
        this.col = new Color(Rand.Next(255), Rand.Next(255), Rand.Next(255));
        this.palette = palette;
        this.lastWetnessUpdate = (float)GameTime.getInstance().getWorldAgeHours();
        this.itemType = ItemType.CLOTHING;
    }

    @Override
    public boolean IsClothing() {
        return true;
    }

    public void Unwear() {
        this.Unwear(false);
    }

    public void Unwear(boolean drop) {
        IsoObject isoObject;
        if (!this.isWorn()) {
            return;
        }
        if (this.container != null && (isoObject = this.container.parent) instanceof IsoGameCharacter) {
            IsoGameCharacter c = (IsoGameCharacter)isoObject;
            c.removeWornItem(this);
            if (c instanceof IsoPlayer) {
                LuaEventManager.triggerEvent("OnClothingUpdated", c);
            }
            if (drop && c.getSquare() != null && c.getVehicle() == null) {
                c.getInventory().Remove(this);
                c.getSquare().AddWorldInventoryItem(this, (float)(Rand.Next(100) / 100), (float)(Rand.Next(100) / 100), 0.0f);
                LuaEventManager.triggerEvent("OnContainerUpdate");
            }
            IsoWorld.instance.currentCell.addToProcessItemsRemove(this);
        }
    }

    @Override
    public void DoTooltip(ObjectTooltip tooltipUI, ObjectTooltip.Layout layout) {
        float f;
        ObjectTooltip.LayoutItem item;
        ColorInfo g2Bgrad = new ColorInfo();
        ColorInfo b2Ggrad = new ColorInfo();
        float tr = 1.0f;
        float tg = 1.0f;
        float tb = 0.8f;
        float ta = 1.0f;
        ColorInfo highlightGood = Core.getInstance().getGoodHighlitedColor();
        ColorInfo highlightBad = Core.getInstance().getBadHighlitedColor();
        float goodR = highlightGood.getR();
        float goodG = highlightGood.getG();
        float goodB = highlightGood.getB();
        float badR = highlightBad.getR();
        float badG = highlightBad.getG();
        float badB = highlightBad.getB();
        if (!this.isCosmetic()) {
            item = layout.addItem();
            item.setLabel(Translator.getText("Tooltip_weapon_Condition") + ":", 1.0f, 1.0f, 0.8f, 1.0f);
            f = (float)this.condition / (float)this.conditionMax;
            Core.getInstance().getBadHighlitedColor().interp(Core.getInstance().getGoodHighlitedColor(), f, g2Bgrad);
            item.setProgress(f, g2Bgrad.getR(), g2Bgrad.getG(), g2Bgrad.getB(), 1.0f);
            item = layout.addItem();
            item.setLabel(Translator.getText("Tooltip_item_Insulation") + ": ", 1.0f, 1.0f, 0.8f, 1.0f);
            f = this.getInsulation();
            Core.getInstance().getBadHighlitedColor().interp(Core.getInstance().getGoodHighlitedColor(), f, g2Bgrad);
            item.setProgress(f, g2Bgrad.getR(), g2Bgrad.getG(), g2Bgrad.getB(), 1.0f);
            f = this.getWindresistance();
            if (f > 0.0f) {
                item = layout.addItem();
                item.setLabel(Translator.getText("Tooltip_item_Windresist") + ": ", 1.0f, 1.0f, 0.8f, 1.0f);
                Core.getInstance().getBadHighlitedColor().interp(Core.getInstance().getGoodHighlitedColor(), f, g2Bgrad);
                item.setProgress(f, g2Bgrad.getR(), g2Bgrad.getG(), g2Bgrad.getB(), 1.0f);
            }
            if ((f = this.getWaterResistance()) > 0.0f) {
                item = layout.addItem();
                item.setLabel(Translator.getText("Tooltip_item_Waterresist") + ": ", 1.0f, 1.0f, 0.8f, 1.0f);
                Core.getInstance().getBadHighlitedColor().interp(Core.getInstance().getGoodHighlitedColor(), f, g2Bgrad);
                item.setProgress(f, g2Bgrad.getR(), g2Bgrad.getG(), g2Bgrad.getB(), 1.0f);
            }
        }
        if (this.bloodLevel != 0.0f) {
            item = layout.addItem();
            item.setLabel(Translator.getText("Tooltip_clothing_bloody") + ":", 1.0f, 1.0f, 0.8f, 1.0f);
            f = this.bloodLevel / 100.0f;
            Core.getInstance().getGoodHighlitedColor().interp(Core.getInstance().getBadHighlitedColor(), f, b2Ggrad);
            item.setProgress(f, b2Ggrad.getR(), b2Ggrad.getG(), b2Ggrad.getB(), 1.0f);
        }
        if (this.dirtyness >= 1.0f) {
            item = layout.addItem();
            item.setLabel(Translator.getText("Tooltip_clothing_dirty") + ":", 1.0f, 1.0f, 0.8f, 1.0f);
            f = this.dirtyness / 100.0f;
            Core.getInstance().getGoodHighlitedColor().interp(Core.getInstance().getBadHighlitedColor(), f, b2Ggrad);
            item.setProgress(f, b2Ggrad.getR(), b2Ggrad.getG(), b2Ggrad.getB(), 1.0f);
        }
        if (this.wetness != 0.0f) {
            item = layout.addItem();
            item.setLabel(Translator.getText("Tooltip_clothing_wet") + ":", 1.0f, 1.0f, 0.8f, 1.0f);
            f = this.wetness / 100.0f;
            Core.getInstance().getGoodHighlitedColor().interp(Core.getInstance().getBadHighlitedColor(), f, b2Ggrad);
            item.setProgress(f, b2Ggrad.getR(), b2Ggrad.getG(), b2Ggrad.getB(), 1.0f);
        }
        int numHoles = 0;
        ItemVisual itemVisual = this.getVisual();
        for (int i = 0; i < BloodBodyPartType.MAX.index(); ++i) {
            if (!(itemVisual.getHole(BloodBodyPartType.FromIndex(i)) > 0.0f)) continue;
            ++numHoles;
        }
        if (numHoles > 0) {
            item = layout.addItem();
            item.setLabel(Translator.getText("Tooltip_clothing_holes") + ":", 1.0f, 1.0f, 0.8f, 1.0f);
            item.setValueRightNoPlus(numHoles);
        }
        if (!this.isEquipped() && tooltipUI.getCharacter() != null) {
            float newBulletDefense;
            float newScratchDefense;
            float previousBiteDefense = 0.0f;
            float previousScratchDefense = 0.0f;
            float previousBulletDefense = 0.0f;
            WornItems wornItems = tooltipUI.getCharacter().getWornItems();
            for (int x = 0; x < wornItems.size(); ++x) {
                WornItem wornItem = wornItems.get(x);
                if (!wornItem.getItem().IsClothing() || wornItem.getLocation() == null || !this.getBodyLocation().equals(wornItem.getLocation()) && !wornItems.getBodyLocationGroup().isExclusive(this.getBodyLocation(), wornItem.getLocation())) continue;
                previousBiteDefense += ((Clothing)wornItem.getItem()).getBiteDefense();
                previousScratchDefense += ((Clothing)wornItem.getItem()).getScratchDefense();
                previousBulletDefense += ((Clothing)wornItem.getItem()).getBulletDefense();
            }
            float newBiteDefense = this.getBiteDefense();
            if (newBiteDefense != previousBiteDefense) {
                item = layout.addItem();
                if (newBiteDefense > 0.0f || previousBiteDefense > 0.0f) {
                    item.setLabel(Translator.getText("Tooltip_BiteDefense") + ":", 1.0f, 1.0f, 0.8f, 1.0f);
                    if (newBiteDefense > previousBiteDefense) {
                        item.setValue((int)newBiteDefense + " (+" + (int)(newBiteDefense - previousBiteDefense) + ")", Core.getInstance().getGoodHighlitedColor().getR(), Core.getInstance().getGoodHighlitedColor().getG(), Core.getInstance().getGoodHighlitedColor().getB(), 1.0f);
                    } else {
                        item.setValue((int)newBiteDefense + " (-" + (int)(previousBiteDefense - newBiteDefense) + ")", Core.getInstance().getBadHighlitedColor().getR(), Core.getInstance().getBadHighlitedColor().getG(), Core.getInstance().getBadHighlitedColor().getB(), 1.0f);
                    }
                }
            } else if (this.getBiteDefense() != 0.0f) {
                item = layout.addItem();
                item.setLabel(Translator.getText("Tooltip_BiteDefense") + ":", 1.0f, 1.0f, 0.8f, 1.0f);
                item.setValueRightNoPlus((int)this.getBiteDefense());
            }
            if ((newScratchDefense = this.getScratchDefense()) != previousScratchDefense) {
                item = layout.addItem();
                if (newScratchDefense > 0.0f || previousScratchDefense > 0.0f) {
                    item.setLabel(Translator.getText("Tooltip_ScratchDefense") + ":", 1.0f, 1.0f, 0.8f, 1.0f);
                    if (newScratchDefense > previousScratchDefense) {
                        item.setValue((int)newScratchDefense + " (+" + (int)(newScratchDefense - previousScratchDefense) + ")", Core.getInstance().getGoodHighlitedColor().getR(), Core.getInstance().getGoodHighlitedColor().getG(), Core.getInstance().getGoodHighlitedColor().getB(), 1.0f);
                    } else {
                        item.setValue((int)newScratchDefense + " (-" + (int)(previousScratchDefense - newScratchDefense) + ")", Core.getInstance().getBadHighlitedColor().getR(), Core.getInstance().getBadHighlitedColor().getG(), Core.getInstance().getBadHighlitedColor().getB(), 1.0f);
                    }
                }
            } else if (this.getScratchDefense() != 0.0f) {
                item = layout.addItem();
                item.setLabel(Translator.getText("Tooltip_ScratchDefense") + ":", 1.0f, 1.0f, 0.8f, 1.0f);
                item.setValueRightNoPlus((int)this.getScratchDefense());
            }
            if ((newBulletDefense = this.getBulletDefense()) != previousBulletDefense) {
                item = layout.addItem();
                if (newBulletDefense > 0.0f || previousBulletDefense > 0.0f) {
                    item.setLabel(Translator.getText("Tooltip_BulletDefense") + ":", 1.0f, 1.0f, 0.8f, 1.0f);
                    if (newBulletDefense > previousBulletDefense) {
                        item.setValue((int)newBulletDefense + " (+" + (int)(newBulletDefense - previousBulletDefense) + ")", Core.getInstance().getGoodHighlitedColor().getR(), Core.getInstance().getGoodHighlitedColor().getG(), Core.getInstance().getGoodHighlitedColor().getB(), 1.0f);
                    } else {
                        item.setValue((int)newBulletDefense + " (-" + (int)(previousBulletDefense - newBulletDefense) + ")", Core.getInstance().getBadHighlitedColor().getR(), Core.getInstance().getBadHighlitedColor().getG(), Core.getInstance().getBadHighlitedColor().getB(), 1.0f);
                    }
                }
            } else if (this.getBulletDefense() != 0.0f) {
                item = layout.addItem();
                item.setLabel(Translator.getText("Tooltip_BulletDefense") + ":", 1.0f, 1.0f, 0.8f, 1.0f);
                item.setValueRightNoPlus((int)this.getBulletDefense());
            }
        } else {
            if (this.getBiteDefense() != 0.0f) {
                item = layout.addItem();
                item.setLabel(Translator.getText("Tooltip_BiteDefense") + ":", 1.0f, 1.0f, 0.8f, 1.0f);
                item.setValueRightNoPlus((int)this.getBiteDefense());
            }
            if (this.getScratchDefense() != 0.0f) {
                item = layout.addItem();
                item.setLabel(Translator.getText("Tooltip_ScratchDefense") + ":", 1.0f, 1.0f, 0.8f, 1.0f);
                item.setValueRightNoPlus((int)this.getScratchDefense());
            }
            if (this.getBulletDefense() != 0.0f) {
                item = layout.addItem();
                item.setLabel(Translator.getText("Tooltip_BulletDefense") + ":", 1.0f, 1.0f, 0.8f, 1.0f);
                item.setValueRightNoPlus((int)this.getBulletDefense());
            }
        }
        if (this.hasTag(ItemTag.GAS_MASK) || this.hasTag(ItemTag.RESPIRATOR)) {
            if (this.hasFilter()) {
                String filterName = ScriptManager.instance.getItem(this.getFilterType()).getDisplayName();
                item = layout.addItem();
                item.setLabel(Translator.getText(filterName) + ":", 1.0f, 1.0f, 0.8f, 1.0f);
                float filterValue = this.getUsedDelta();
                Core.getInstance().getBadHighlitedColor().interp(Core.getInstance().getGoodHighlitedColor(), filterValue, g2Bgrad);
                item.setProgress(filterValue, g2Bgrad.getR(), g2Bgrad.getG(), g2Bgrad.getB(), 1.0f);
            } else {
                item = layout.addItem();
                item.setLabel(Translator.getText("Tooltip_NoFilter"), 1.0f, 1.0f, 0.8f, 1.0f);
            }
        } else if (this.hasTag(ItemTag.GAS_MASK_NO_FILTER) || this.hasTag(ItemTag.RESPIRATOR_NO_FILTER)) {
            item = layout.addItem();
            item.setLabel(Translator.getText("Tooltip_NoFilter"), 1.0f, 1.0f, 0.8f, 1.0f);
        }
        if (this.hasTag(ItemTag.SCBA)) {
            if (this.hasTank()) {
                String tankName = ScriptManager.instance.getItem(this.getTankType()).getDisplayName();
                item = layout.addItem();
                item.setLabel(Translator.getText(tankName) + ":", 1.0f, 1.0f, 0.8f, 1.0f);
                float tankValue = this.getUsedDelta();
                Core.getInstance().getBadHighlitedColor().interp(Core.getInstance().getGoodHighlitedColor(), tankValue, g2Bgrad);
                item.setProgress(tankValue, g2Bgrad.getR(), g2Bgrad.getG(), g2Bgrad.getB(), 1.0f);
            } else {
                item = layout.addItem();
                item.setLabel(Translator.getText("Tooltip_NoTank"), 1.0f, 1.0f, 0.8f, 1.0f);
            }
        } else if (this.hasTag(ItemTag.SCBANO_TANK)) {
            item = layout.addItem();
            item.setLabel(Translator.getText("Tooltip_NoTank"), 1.0f, 1.0f, 0.8f, 1.0f);
        }
        if (this.getRunSpeedModifier() != 1.0f) {
            item = layout.addItem();
            item.setLabel(Translator.getText("Tooltip_RunSpeedModifier") + ":", 1.0f, 1.0f, 0.8f, 1.0f);
            if (this.getRunSpeedModifier() > 1.0f) {
                item.setProgress(this.getRunSpeedModifier() - 1.0f, goodR, goodG, goodB, 1.0f);
            } else {
                item.setProgress(1.0f - this.getRunSpeedModifier(), badR, badG, badB, 1.0f);
            }
        }
        if (this.getCombatSpeedModifier() != 1.0f) {
            item = layout.addItem();
            item.setLabel(Translator.getText("Tooltip_CombatSpeedModifier") + ":", 1.0f, 1.0f, 0.8f, 1.0f);
            if (this.getRunSpeedModifier() > 1.0f) {
                item.setProgress(this.getCombatSpeedModifier() - 1.0f, goodR, goodG, goodB, 1.0f);
            } else {
                item.setProgress(1.0f - this.getCombatSpeedModifier(), badR, badG, badB, 1.0f);
            }
        }
        if (Core.debug && DebugOptions.instance.tooltipInfo.getValue()) {
            if (this.bloodLevel != 0.0f) {
                item = layout.addItem();
                item.setLabel("DBG: bloodLevel:", 1.0f, 1.0f, 0.8f, 1.0f);
                int value = (int)Math.ceil(this.bloodLevel);
                item.setValueRight(value, false);
            }
            if (this.dirtyness != 0.0f) {
                item = layout.addItem();
                item.setLabel("DBG: dirtyness:", 1.0f, 1.0f, 0.8f, 1.0f);
                int value = (int)Math.ceil(this.dirtyness);
                item.setValueRight(value, false);
            }
            if (this.wetness != 0.0f) {
                item = layout.addItem();
                item.setLabel("DBG: wetness:", 1.0f, 1.0f, 0.8f, 1.0f);
                int value = (int)Math.ceil(this.wetness);
                item.setValueRight(value, false);
            }
        }
    }

    public boolean isDirty() {
        return this.dirtyness > 15.0f;
    }

    @Override
    public boolean isBloody() {
        return this.getBloodlevel() > 25.0f;
    }

    @Override
    public String getName() {
        return this.getName(null);
    }

    @Override
    public String getName(IsoPlayer player) {
        Object prefix = "";
        if (this.isDirty()) {
            prefix = (String)prefix + this.dirtyString + ", ";
        }
        if (this.isBloody()) {
            prefix = (String)prefix + this.bloodyString + ", ";
        }
        if (this.getWetness() >= 100.0f) {
            prefix = (String)prefix + this.soakedString + ", ";
        } else if (this.getWetness() > 25.0f) {
            prefix = (String)prefix + this.wetString + ", ";
        }
        if (this.isBroken()) {
            prefix = (String)prefix + this.brokenString + ", ";
        } else if ((float)this.getCondition() < (float)this.getConditionMax() / 3.0f) {
            prefix = (String)prefix + this.wornString + ", ";
        }
        if (((String)prefix).length() > 2) {
            prefix = ((String)prefix).substring(0, ((String)prefix).length() - 2);
        }
        prefix = ((String)prefix).trim();
        if (this.getFluidContainer() != null) {
            return this.getFluidContainer().getUiName();
        }
        if (((String)prefix).isEmpty()) {
            return this.name;
        }
        return Translator.getText("IGUI_ClothingNaming", prefix, this.name);
    }

    @Override
    public void update() {
        if (this.isActivated() && !this.isWorn()) {
            this.setActivated(false);
        }
    }

    public void updateWetness() {
        this.updateWetness(false);
    }

    public void updateWetness(boolean bIgnoreEquipped) {
        if (!bIgnoreEquipped && this.isEquipped()) {
            return;
        }
        if (this.getBloodClothingType() == null) {
            this.setWetness(0.0f);
            return;
        }
        float worldAgeHours = (float)GameTime.getInstance().getWorldAgeHours();
        if (this.lastWetnessUpdate < 0.0f) {
            this.lastWetnessUpdate = worldAgeHours;
        } else if (this.lastWetnessUpdate > worldAgeHours) {
            this.lastWetnessUpdate = worldAgeHours;
        }
        float elapsed = worldAgeHours - this.lastWetnessUpdate;
        if (elapsed < 0.016666668f) {
            return;
        }
        this.lastWetnessUpdate = worldAgeHours;
        if (this.hasTag(ItemTag.BREAK_WHEN_WET) && this.getWetness() >= 100.0f) {
            this.setCondition(0);
        }
        switch (this.getWetDryState().ordinal()) {
            case 0: {
                break;
            }
            case 1: {
                if (!(this.getWetness() > 0.0f)) break;
                float dryAmount = elapsed * 20.0f;
                if (this.isEquipped()) {
                    dryAmount *= 2.0f;
                }
                this.setWetness(this.getWetness() - dryAmount);
                break;
            }
            case 2: {
                if (!(this.getWetness() < 100.0f)) break;
                float intensity = ClimateManager.getInstance().getRainIntensity();
                if (intensity < 0.1f) {
                    intensity = 0.0f;
                }
                float wetAmount = intensity * elapsed * 100.0f;
                this.setWetness(this.getWetness() + wetAmount);
            }
        }
    }

    public float getBulletDefense() {
        if (this.getCondition() <= 0) {
            return 0.0f;
        }
        return this.bulletDefense;
    }

    public void setBulletDefense(float bulletDefense) {
        this.bulletDefense = Math.min(bulletDefense, 100.0f);
    }

    private WetDryState getWetDryState() {
        if (this.getWorldItem() == null) {
            IsoClothingWasher isoClothingWasher;
            IsoClothingDryer isoClothingDryer;
            if (this.container == null) {
                return WetDryState.Invalid;
            }
            IsoObject isoObject = this.container.parent;
            if (isoObject instanceof IsoDeadBody) {
                IsoDeadBody chr = (IsoDeadBody)isoObject;
                if (chr.getSquare() == null) {
                    return WetDryState.Invalid;
                }
                if (chr.getSquare().isInARoom()) {
                    return WetDryState.Dryer;
                }
                if (ClimateManager.getInstance().isRaining()) {
                    return WetDryState.Wetter;
                }
                return WetDryState.Dryer;
            }
            isoObject = this.container.parent;
            if (isoObject instanceof IsoGameCharacter) {
                IsoGameCharacter chr = (IsoGameCharacter)isoObject;
                if (chr.getCurrentSquare() == null) {
                    return WetDryState.Invalid;
                }
                if (chr.getCurrentSquare().isInARoom() || chr.getCurrentSquare().haveRoof) {
                    return WetDryState.Dryer;
                }
                if (ClimateManager.getInstance().isRaining()) {
                    VehicleWindow window;
                    VehiclePart windshield;
                    if (!this.isEquipped()) {
                        return WetDryState.Dryer;
                    }
                    if ((chr.isAsleep() || chr.isResting()) && chr.getBed() != null && (chr.getBed().isTent() || "Tent".equalsIgnoreCase(chr.getBed().getName()) || "Shelter".equalsIgnoreCase(chr.getBed().getName()))) {
                        return WetDryState.Dryer;
                    }
                    BaseVehicle vehicle = chr.getVehicle();
                    if (vehicle != null && vehicle.hasRoof(vehicle.getSeat(chr)) && (windshield = vehicle.getPartById("Windshield")) != null && (window = windshield.getWindow()) != null && window.isHittable()) {
                        return WetDryState.Dryer;
                    }
                    return WetDryState.Wetter;
                }
                return WetDryState.Dryer;
            }
            if (this.container.parent == null) {
                return WetDryState.Dryer;
            }
            isoObject = this.container.parent;
            if (isoObject instanceof IsoClothingDryer && (isoClothingDryer = (IsoClothingDryer)isoObject).isActivated()) {
                return WetDryState.Invalid;
            }
            isoObject = this.container.parent;
            if (isoObject instanceof IsoClothingWasher && (isoClothingWasher = (IsoClothingWasher)isoObject).isActivated()) {
                return WetDryState.Invalid;
            }
            IsoCombinationWasherDryer washerDryer = Type.tryCastTo(this.container.parent, IsoCombinationWasherDryer.class);
            if (washerDryer != null && washerDryer.isActivated()) {
                return WetDryState.Invalid;
            }
            return WetDryState.Dryer;
        }
        if (this.getWorldItem().getSquare() == null) {
            return WetDryState.Invalid;
        }
        if (this.getWorldItem().getSquare().isInARoom()) {
            return WetDryState.Dryer;
        }
        if (ClimateManager.getInstance().isRaining()) {
            return WetDryState.Wetter;
        }
        return WetDryState.Dryer;
    }

    public void flushWetness() {
        if (this.lastWetnessUpdate < 0.0f) {
            return;
        }
        this.updateWetness(true);
        this.lastWetnessUpdate = -1.0f;
    }

    @Override
    public boolean finishupdate() {
        if (this.container == null || !(this.container.parent instanceof IsoGameCharacter)) {
            return true;
        }
        return !this.isEquipped();
    }

    public void Use(boolean bCrafting, boolean bInContainer) {
        if (this.uses <= 1) {
            this.Unwear();
        }
        this.Use(bCrafting, bInContainer, false);
    }

    @Override
    public boolean CanStack(InventoryItem item) {
        return this.ModDataMatches(item) && this.palette == null && ((Clothing)item).palette == null || this.palette.equals(((Clothing)item).palette);
    }

    public static Clothing CreateFromSprite(String sprite) {
        try {
            return (Clothing)InventoryItemFactory.CreateItem(sprite, 1.0f);
        }
        catch (Exception ignored) {
            return null;
        }
    }

    @Override
    public void save(ByteBuffer output, boolean net) throws IOException {
        super.save(output, net);
        BitHeaderWrite bits = BitHeader.allocWrite(BitHeader.HeaderSize.Byte, output);
        if (this.getSpriteName() != null) {
            bits.addFlags(1);
            GameWindow.WriteString(output, this.getSpriteName());
        }
        if (this.dirtyness != 0.0f) {
            bits.addFlags(2);
            output.putFloat(this.dirtyness);
        }
        if (this.bloodLevel != 0.0f) {
            bits.addFlags(4);
            output.putFloat(this.bloodLevel);
        }
        if (this.wetness != 0.0f) {
            bits.addFlags(8);
            output.putFloat(this.wetness);
        }
        if (this.lastWetnessUpdate != 0.0f) {
            bits.addFlags(16);
            output.putFloat(this.lastWetnessUpdate);
        }
        if (this.patches != null) {
            bits.addFlags(32);
            output.put((byte)this.patches.size());
            for (int partIndex : this.patches.keySet()) {
                output.put((byte)partIndex);
                this.patches.get(partIndex).save(output, false);
            }
        }
        bits.write();
        bits.release();
    }

    @Override
    public void load(ByteBuffer input, int worldVersion) throws IOException {
        super.load(input, worldVersion);
        BitHeaderRead bits = BitHeader.allocRead(BitHeader.HeaderSize.Byte, input);
        if (!bits.equals(0)) {
            if (bits.hasFlags(1)) {
                this.setSpriteName(GameWindow.ReadString(input));
            }
            if (bits.hasFlags(2)) {
                this.dirtyness = input.getFloat();
            }
            if (bits.hasFlags(4)) {
                this.bloodLevel = input.getFloat();
            }
            if (bits.hasFlags(8)) {
                this.wetness = input.getFloat();
            }
            if (bits.hasFlags(16)) {
                this.lastWetnessUpdate = input.getFloat();
            }
            if (bits.hasFlags(32)) {
                int patchNbr = input.get();
                for (int i = 0; i < patchNbr; ++i) {
                    byte partIndex = input.get();
                    ClothingPatch patch = new ClothingPatch();
                    patch.load(input, worldVersion);
                    if (this.patches == null) {
                        this.patches = new HashMap();
                    }
                    this.patches.put(Integer.valueOf(partIndex), patch);
                }
            }
        }
        bits.release();
        this.synchWithVisual();
        this.lastWetnessUpdate = (float)GameTime.getInstance().getWorldAgeHours();
    }

    public String getSpriteName() {
        return this.spriteName;
    }

    public void setSpriteName(String spriteName) {
        this.spriteName = spriteName;
    }

    public String getPalette() {
        if (this.palette == null) {
            return "Trousers_White";
        }
        return this.palette;
    }

    public void setPalette(String palette) {
        this.palette = palette;
    }

    public float getTemperature() {
        return this.temperature;
    }

    public void setTemperature(float temperature) {
        this.temperature = temperature;
    }

    public void setDirtiness(float delta) {
        this.dirtyness = PZMath.clamp(delta, 0.0f, 100.0f);
    }

    @Override
    public void setBloodLevel(float delta) {
        this.bloodLevel = PZMath.clamp(delta, 0.0f, 100.0f);
    }

    public float getDirtiness() {
        return this.dirtyness;
    }

    public float getBloodlevel() {
        return this.bloodLevel;
    }

    public float getBloodlevelForPart(BloodBodyPartType part) {
        return this.getVisual().getBlood(part);
    }

    @Override
    public float getBloodLevel() {
        return this.bloodLevel;
    }

    public float getBloodLevelForPart(BloodBodyPartType part) {
        return this.getVisual().getBlood(part);
    }

    @Override
    public float getWeight() {
        float weight = this.getActualWeight();
        float weightWet = this.getWeightWet();
        if (weightWet <= 0.0f) {
            weightWet = weight * 1.25f;
        }
        return PZMath.lerp(weight, weightWet, this.getWetness() / 100.0f);
    }

    public void setWetness(float percent) {
        this.wetness = PZMath.clamp(percent, 0.0f, 100.0f);
    }

    @Override
    public float getWetness() {
        return this.wetness;
    }

    public float getWeightWet() {
        return this.weightWet;
    }

    public void setWeightWet(float weight) {
        this.weightWet = weight;
    }

    @Override
    public int getConditionLowerChance() {
        return this.conditionLowerChance;
    }

    public void setConditionLowerChance(int conditionLowerChance) {
        this.conditionLowerChance = conditionLowerChance;
    }

    @Override
    public void setCondition(int condition) {
        this.setCondition(condition, true);
        if (condition <= 0) {
            this.setBroken(true);
            if (this.getContainer() != null) {
                this.getContainer().setDrawDirty(true);
            }
            if (this.isWorn() && this.isRemoveOnBroken().booleanValue()) {
                this.Unwear(true);
            }
        }
    }

    public float getClothingDirtynessIncreaseLevel() {
        if (SandboxOptions.instance.clothingDegradation.getValue() == 2) {
            return 2.5E-4f;
        }
        if (SandboxOptions.instance.clothingDegradation.getValue() == 4) {
            return 0.025f;
        }
        return 0.0025f;
    }

    public float getInsulation() {
        return this.insulation;
    }

    public void setInsulation(float insulation) {
        this.insulation = insulation;
    }

    public float getStompPower() {
        return this.stompPower;
    }

    public void setStompPower(float stompPower) {
        this.stompPower = stompPower;
    }

    public float getRunSpeedModifier() {
        return this.runSpeedModifier;
    }

    public void setRunSpeedModifier(float runSpeedModifier) {
        this.runSpeedModifier = runSpeedModifier;
    }

    public float getCombatSpeedModifier() {
        return this.combatSpeedModifier;
    }

    public void setCombatSpeedModifier(float combatSpeedModifier) {
        this.combatSpeedModifier = combatSpeedModifier;
    }

    public Boolean isRemoveOnBroken() {
        return this.removeOnBroken;
    }

    public void setRemoveOnBroken(Boolean removeOnBroken) {
        this.removeOnBroken = removeOnBroken;
    }

    public Boolean getCanHaveHoles() {
        return this.canHaveHoles;
    }

    public void setCanHaveHoles(Boolean canHaveHoles) {
        this.canHaveHoles = canHaveHoles;
    }

    public boolean isCosmetic() {
        return this.getScriptItem().isCosmetic();
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName() + "{ clothingItemName=\"" + this.getClothingItemName() + "\" }";
    }

    public float getBiteDefense() {
        if (this.getCondition() <= 0) {
            return 0.0f;
        }
        return this.biteDefense;
    }

    public void setBiteDefense(float biteDefense) {
        this.biteDefense = Math.min(biteDefense, 100.0f);
    }

    public float getScratchDefense() {
        if (this.getCondition() <= 0) {
            return 0.0f;
        }
        return this.scratchDefense;
    }

    public void setScratchDefense(float scratchDefense) {
        this.scratchDefense = Math.min(scratchDefense, 100.0f);
    }

    public float getNeckProtectionModifier() {
        return this.neckProtectionModifier;
    }

    public void setNeckProtectionModifier(float neckProtectionModifier) {
        this.neckProtectionModifier = neckProtectionModifier;
    }

    public int getChanceToFall() {
        return this.chanceToFall;
    }

    public void setChanceToFall(int chanceToFall) {
        this.chanceToFall = chanceToFall;
    }

    public float getWindresistance() {
        return this.windresistance;
    }

    public void setWindresistance(float windresistance) {
        this.windresistance = windresistance;
    }

    public float getWaterResistance() {
        return this.waterResistance;
    }

    public void setWaterResistance(float waterResistance) {
        this.waterResistance = waterResistance;
    }

    public int getHolesNumber() {
        if (this.getVisual() != null) {
            return this.getVisual().getHolesNumber();
        }
        return 0;
    }

    public int getPatchesNumber() {
        return this.patches == null ? 0 : this.patches.size();
    }

    public float getDefForPart(BloodBodyPartType part, boolean bite, boolean bullet) {
        if (this.getVisual().getHole(part) > 0.0f) {
            return 0.0f;
        }
        ClothingPatch patch = this.getPatchType(part);
        float defense = this.getScratchDefense();
        if (bite) {
            defense = this.getBiteDefense();
        }
        if (bullet) {
            defense = this.getBulletDefense();
        }
        if (part == BloodBodyPartType.Neck && this.getScriptItem().neckProtectionModifier < 1.0f) {
            defense *= this.getScriptItem().neckProtectionModifier;
        }
        if (patch != null) {
            int patchDefense = patch.scratchDefense;
            if (bite) {
                patchDefense = patch.biteDefense;
            }
            if (bullet) {
                patchDefense = patch.biteDefense;
            }
            defense = !patch.hasHole ? (defense += (float)patchDefense) : (float)patchDefense;
        }
        return defense;
    }

    public static int getBiteDefenseFromItem(IsoGameCharacter chr, InventoryItem fabric) {
        int tailorLvl = Math.max(1, chr.getPerkLevel(PerkFactory.Perks.Tailoring));
        ClothingPatchFabricType type = ClothingPatchFabricType.fromType(fabric.getFabricType());
        if (type.maxBiteDef > 0) {
            return (int)Math.max(1.0f, (float)type.maxBiteDef * ((float)tailorLvl / 10.0f));
        }
        return 0;
    }

    public static int getScratchDefenseFromItem(IsoGameCharacter chr, InventoryItem fabric) {
        int tailorLvl = Math.max(1, chr.getPerkLevel(PerkFactory.Perks.Tailoring));
        ClothingPatchFabricType type = ClothingPatchFabricType.fromType(fabric.getFabricType());
        return (int)Math.max(1.0f, (float)type.maxScratchDef * ((float)tailorLvl / 10.0f));
    }

    public ClothingPatch getPatchType(BloodBodyPartType part) {
        if (this.patches != null) {
            return this.patches.get(part.index());
        }
        return null;
    }

    public void removePatch(BloodBodyPartType part) {
        IsoPlayer player;
        IsoPlayer isoPlayer;
        if (this.patches == null) {
            return;
        }
        this.getVisual().removePatch(part.index());
        ClothingPatch patch = this.patches.get(part.index());
        if (patch != null && patch.hasHole) {
            this.getVisual().setHole(part);
            this.setCondition(this.getCondition() - patch.conditionGain, false);
        }
        this.patches.remove(part.index());
        if (GameServer.server && (isoPlayer = this.getPlayer()) instanceof IsoPlayer && (player = isoPlayer).isEquippedClothing(this)) {
            INetworkPacket.sendToAll(PacketTypes.PacketType.SyncClothing, player);
            INetworkPacket.sendToAll(PacketTypes.PacketType.SyncVisuals, player);
        }
    }

    public void removeAllPatches() {
        if (this.patches == null) {
            return;
        }
        ItemVisual itemVisual = this.getVisual();
        for (int i = 0; i < BloodBodyPartType.MAX.index(); ++i) {
            BloodBodyPartType part = BloodBodyPartType.FromIndex(i);
            ClothingPatch patch = this.patches.remove(part.index());
            if (patch == null || itemVisual == null) continue;
            if (patch.hasHole) {
                itemVisual.setHole(part);
            }
            itemVisual.removePatch(i);
        }
    }

    public boolean canFullyRestore(IsoGameCharacter chr, BloodBodyPartType part, InventoryItem fabric) {
        return chr.getPerkLevel(PerkFactory.Perks.Tailoring) > 7 && fabric.getFabricType().equals(this.getFabricType()) && this.getVisual().getHole(part) > 0.0f;
    }

    public void fullyRestore() {
        this.setCondition(this.getConditionMax());
        this.setDirtiness(0.0f);
        this.setBloodLevel(0.0f);
        for (int i = 0; i < BloodBodyPartType.MAX.index(); ++i) {
            BloodBodyPartType part = BloodBodyPartType.FromIndex(i);
            if (this.patches != null) {
                this.getVisual().removePatch(i);
                this.patches.remove(part.index());
            }
            if (this.getVisual().getHole(part) != 0.0f) {
                this.getVisual().removeHole(i);
            }
            this.getVisual().setBlood(part, 0.0f);
        }
        if (GameServer.server) {
            INetworkPacket.sendToAll(PacketTypes.PacketType.SyncClothing, this.getOwner());
        }
    }

    public void addPatchForSync(int partIdx, int tailorLvl, int fabricType, boolean hasHole) {
        if (this.patches == null) {
            this.patches = new HashMap();
        }
        ClothingPatch patch = new ClothingPatch(tailorLvl, fabricType, hasHole);
        this.patches.put(partIdx, patch);
    }

    public void addPatch(IsoGameCharacter chr, BloodBodyPartType part, InventoryItem fabric) {
        IsoPlayer player;
        ClothingPatchFabricType type = ClothingPatchFabricType.fromType(fabric.getFabricType());
        if (this.canFullyRestore(chr, part, fabric)) {
            this.getVisual().removeHole(part.index());
            this.setCondition((int)((float)this.getCondition() + this.getCondLossPerHole()), false);
            return;
        }
        if (type == ClothingPatchFabricType.Cotton) {
            this.getVisual().setBasicPatch(part);
        } else if (type == ClothingPatchFabricType.Denim) {
            this.getVisual().setDenimPatch(part);
        } else {
            this.getVisual().setLeatherPatch(part);
        }
        if (this.patches == null) {
            this.patches = new HashMap();
        }
        int tailorLvl = Math.max(1, chr.getPerkLevel(PerkFactory.Perks.Tailoring));
        float hole = this.getVisual().getHole(part);
        float conditionGain = this.getCondLossPerHole();
        if (tailorLvl < 3) {
            conditionGain -= 2.0f;
        } else if (tailorLvl < 6) {
            conditionGain -= 1.0f;
        }
        ClothingPatch patch = new ClothingPatch(tailorLvl, type.index, hole > 0.0f);
        if (hole > 0.0f) {
            conditionGain = Math.max(1.0f, conditionGain);
            this.setCondition((int)((float)this.getCondition() + conditionGain), false);
            patch.conditionGain = (int)conditionGain;
        }
        this.patches.put(part.index(), patch);
        this.getVisual().removeHole(part.index());
        if (GameServer.server && chr instanceof IsoPlayer && (player = (IsoPlayer)chr).isEquippedClothing(this)) {
            INetworkPacket.sendToAll(PacketTypes.PacketType.SyncClothing, chr);
            INetworkPacket.sendToAll(PacketTypes.PacketType.SyncVisuals, chr);
        }
    }

    public ArrayList<BloodBodyPartType> getCoveredParts() {
        ArrayList<BloodClothingType> types = this.getScriptItem().getBloodClothingType();
        return BloodClothingType.getCoveredParts(types);
    }

    public int getNbrOfCoveredParts() {
        ArrayList<BloodClothingType> types = this.getScriptItem().getBloodClothingType();
        return BloodClothingType.getCoveredPartCount(types);
    }

    public float getCondLossPerHole() {
        int numberOfCoveredParts = this.getNbrOfCoveredParts();
        float minCondLoss = PZMath.max(1, this.getConditionMax() / numberOfCoveredParts);
        return minCondLoss;
    }

    public void copyPatchesTo(Clothing newClothing) {
        newClothing.patches = this.patches;
    }

    public String getClothingExtraSubmenu() {
        return this.scriptItem.clothingExtraSubmenu;
    }

    public boolean canBe3DRender() {
        if (!StringUtils.isNullOrEmpty(this.getWorldStaticItem())) {
            return true;
        }
        return "Bip01_Head".equalsIgnoreCase(this.getClothingItem().attachBone) && (!this.isCosmetic() || this.isBodyLocation(ItemBodyLocation.EYES));
    }

    @Override
    public boolean isWorn() {
        IsoGameCharacter isoGameCharacter;
        IsoObject isoObject;
        return this.container != null && (isoObject = this.container.parent) instanceof IsoGameCharacter && (isoGameCharacter = (IsoGameCharacter)isoObject).getWornItems().contains(this);
    }

    public void addRandomHole() {
        if (!this.getCanHaveHoles().booleanValue() || this.getCoveredParts() == null) {
            return;
        }
        ArrayList<BloodBodyPartType> parts = this.getCoveredParts();
        BloodBodyPartType part = parts.get(Rand.Next(parts.size()));
        int holes = 0;
        if (this.getVisual().getHole(part) <= 0.0f) {
            this.getVisual().setHole(part);
            ++holes;
        }
        this.setCondition(this.getCondition() - (int)((float)holes * this.getCondLossPerHole()), false);
    }

    public void addRandomDirt() {
        if (this.getCoveredParts() == null) {
            return;
        }
        ArrayList<BloodBodyPartType> parts = this.getCoveredParts();
        BloodBodyPartType part = parts.get(Rand.Next(parts.size()));
        int roll = Rand.Next(100) + 1;
        float dirtLevelPart = (float)roll / 100.0f;
        this.getVisual().setDirt(part, dirtLevelPart);
        BloodClothingType.calcTotalDirtLevel(this);
    }

    public void addRandomBlood() {
        if (this.getCoveredParts() == null) {
            return;
        }
        ArrayList<BloodBodyPartType> parts = this.getCoveredParts();
        BloodBodyPartType part = parts.get(Rand.Next(parts.size()));
        int roll = Rand.Next(100) + 1;
        float bloodLevelPart = (float)roll / 100.0f;
        this.getVisual().setBlood(part, bloodLevelPart);
        BloodClothingType.calcTotalBloodLevel(this);
    }

    public void randomizeCondition(int wetChance, int dirtChance, int bloodChance, int holeChance) {
        if (!this.isCosmetic()) {
            int parts;
            if (Rand.Next(100) < wetChance) {
                this.setWetness(Rand.Next(0, 100));
            }
            if ((parts = this.getNbrOfCoveredParts()) >= 1) {
                int i;
                if (Rand.Next(100) < dirtChance) {
                    for (i = 0; i < parts; ++i) {
                        this.addRandomDirt();
                    }
                }
                if (Rand.Next(100) < bloodChance) {
                    for (i = 0; i < parts; ++i) {
                        this.addRandomBlood();
                    }
                }
                if (Rand.Next(100) < holeChance) {
                    if (this.getCanHaveHoles().booleanValue()) {
                        int rolls = Rand.Next(parts + 1);
                        for (int i2 = 0; i2 < rolls; ++i2) {
                            this.addRandomHole();
                        }
                    } else {
                        this.setCondition(Rand.Next(this.getCondition()) + 1, false);
                    }
                }
            }
        }
    }

    public boolean hasFilter() {
        if (!this.hasTag(ItemTag.GAS_MASK) && !this.hasTag(ItemTag.RESPIRATOR) || this.getModData().rawget("filterType") == null || this.getModData().rawget("filterType") == "none") {
            return false;
        }
        return this.getFilterType() != null;
    }

    public void setNoFilter() {
        if (!this.hasTag(ItemTag.GAS_MASK) && !this.hasTag(ItemTag.RESPIRATOR)) {
            return;
        }
        this.getModData().rawset("filterType", null);
    }

    public String getFilterType() {
        if (this.getModData().rawget("filterType") == null || this.getModData().rawget("filterType") == "none") {
            return null;
        }
        return (String)this.getModData().rawget("filterType");
    }

    public void setFilterType(String filterType) {
        this.getModData().rawset("filterType", (Object)filterType);
    }

    public boolean hasTank() {
        if (!this.hasTag(ItemTag.SCBA) || this.getModData().rawget("tankType") == null || this.getModData().rawget("tankType") == "none") {
            return false;
        }
        return this.getTankType() != null;
    }

    public void setNoTank() {
        if (!this.hasTag(ItemTag.SCBA)) {
            return;
        }
        this.getModData().rawset("tankType", null);
    }

    public String getTankType() {
        if (this.getModData().rawget("tankType") == null || this.getModData().rawget("tankType") == "none") {
            return null;
        }
        return (String)this.getModData().rawget("tankType");
    }

    public void setTankType(String tankType) {
        this.getModData().rawset("tankType", (Object)tankType);
    }

    public float getUsedDelta() {
        if (this.getModData().rawget("usedDelta") == null) {
            this.setUsedDelta(0.0f);
        }
        return (float)((Double)this.getModData().rawget("usedDelta")).doubleValue();
    }

    public void setUsedDelta(float usedDelta) {
        if (usedDelta < 0.0f) {
            usedDelta = 0.0f;
        }
        this.getModData().rawset("usedDelta", (Object)usedDelta);
    }

    @Override
    public float getUseDelta() {
        return this.getScriptItem().getUseDelta();
    }

    public void drainGasMask() {
        this.drainGasMask(1.0f);
    }

    public void drainGasMask(float rate) {
        if (!this.hasTag(ItemTag.GAS_MASK) && !this.hasTag(ItemTag.RESPIRATOR) || !this.hasFilter() || this.getUsedDelta() <= 0.0f) {
            return;
        }
        float rate2 = rate * ScriptManager.instance.getItem(this.getFilterType()).getUseDelta() * GameTime.getInstance().getMultiplier();
        this.setUsedDelta(this.getUsedDelta() - rate2);
        if (this.getUsedDelta() < 0.0f) {
            this.setUsedDelta(0.0f);
        }
    }

    public void drainSCBA() {
        if (this.getUsedDelta() <= 0.0f) {
            this.setActivated(false);
        }
        if (!this.hasTag(ItemTag.SCBA) || !this.hasTank() || !this.isActivated() || this.getUsedDelta() <= 0.0f) {
            return;
        }
        float rate = 0.001f;
        float rate2 = 0.001f * ScriptManager.instance.getItem(this.getTankType()).getUseDelta() * GameTime.getInstance().getMultiplier();
        this.setUsedDelta(this.getUsedDelta() - rate2);
        if (this.getUsedDelta() < 0.0f) {
            this.setUsedDelta(0.0f);
        }
        if (this.getUsedDelta() <= 0.0f) {
            this.setActivated(false);
        }
    }

    public float getCorpseSicknessDefense() {
        if (this.getCondition() <= 0) {
            return 0.0f;
        }
        float defense = this.getScriptItem().getCorpseSicknessDefense();
        if (this.hasFilter()) {
            float value = 25.0f;
            if (this.getUsedDelta() > 0.0f) {
                value = 100.0f;
            }
            if (value > defense) {
                defense = value;
            }
        }
        if (this.hasFilter() && 25.0f > defense) {
            defense = 25.0f;
        }
        return defense;
    }

    private static enum WetDryState {
        Invalid,
        Dryer,
        Wetter;

    }

    @UsedFromLua
    public static class ClothingPatch {
        public int tailorLvl;
        public int fabricType;
        public int scratchDefense;
        public int biteDefense;
        public boolean hasHole;
        public int conditionGain;

        public String getFabricTypeName() {
            return Translator.getText("IGUI_FabricType_" + this.fabricType);
        }

        public int getScratchDefense() {
            return this.scratchDefense;
        }

        public int getBiteDefense() {
            return this.biteDefense;
        }

        public int getFabricType() {
            return this.fabricType;
        }

        public ClothingPatch() {
        }

        public ClothingPatch(int tailorLvl, int fabricType, boolean hasHole) {
            this.tailorLvl = tailorLvl;
            this.fabricType = fabricType;
            this.hasHole = hasHole;
            ClothingPatchFabricType type = ClothingPatchFabricType.fromIndex(fabricType);
            this.scratchDefense = (int)Math.max(1.0f, (float)type.maxScratchDef * ((float)tailorLvl / 10.0f));
            if (type.maxBiteDef > 0) {
                this.biteDefense = (int)Math.max(1.0f, (float)type.maxBiteDef * ((float)tailorLvl / 10.0f));
            }
        }

        public void save(ByteBuffer output, boolean net) throws IOException {
            output.put((byte)this.tailorLvl);
            output.put((byte)this.fabricType);
            output.put((byte)this.scratchDefense);
            output.put((byte)this.biteDefense);
            output.put(this.hasHole ? (byte)1 : 0);
            output.putShort((short)this.conditionGain);
        }

        public void load(ByteBuffer input, int worldVersion) throws IOException {
            this.tailorLvl = input.get();
            this.fabricType = input.get();
            this.scratchDefense = input.get();
            this.biteDefense = input.get();
            this.hasHole = input.get() != 0;
            this.conditionGain = input.getShort();
        }

        @Deprecated
        public void save_old(ByteBuffer output, boolean net) throws IOException {
            output.putInt(this.tailorLvl);
            output.putInt(this.fabricType);
            output.putInt(this.scratchDefense);
            output.putInt(this.biteDefense);
            output.put(this.hasHole ? (byte)1 : 0);
            output.putInt(this.conditionGain);
        }

        @Deprecated
        public void load_old(ByteBuffer input, int worldVersion, boolean net) throws IOException {
            this.tailorLvl = input.getInt();
            this.fabricType = input.getInt();
            this.scratchDefense = input.getInt();
            this.biteDefense = input.getInt();
            this.hasHole = input.get() != 0;
            this.conditionGain = input.getInt();
        }
    }

    @UsedFromLua
    public static enum ClothingPatchFabricType {
        Cotton(1, "Cotton", 5, 0),
        Denim(2, "Denim", 10, 5),
        Leather(3, "Leather", 20, 10);

        public final int index;
        public final String type;
        public final int maxScratchDef;
        public final int maxBiteDef;

        private ClothingPatchFabricType(int index, String type, int maxScratchDef, int maxBiteDef) {
            this.index = index;
            this.type = type;
            this.maxScratchDef = maxScratchDef;
            this.maxBiteDef = maxBiteDef;
        }

        public String getType() {
            return this.type;
        }

        public static ClothingPatchFabricType fromType(String type) {
            if (StringUtils.isNullOrEmpty(type)) {
                return null;
            }
            if (ClothingPatchFabricType.Cotton.type.equals(type)) {
                return Cotton;
            }
            if (ClothingPatchFabricType.Denim.type.equals(type)) {
                return Denim;
            }
            if (ClothingPatchFabricType.Leather.type.equals(type)) {
                return Leather;
            }
            return null;
        }

        public static ClothingPatchFabricType fromIndex(int index) {
            if (index == 1) {
                return Cotton;
            }
            if (index == 2) {
                return Denim;
            }
            if (index == 3) {
                return Leather;
            }
            return null;
        }
    }
}

