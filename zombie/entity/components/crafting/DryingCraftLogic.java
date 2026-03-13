/*
 * Decompiled with CFR 0.152.
 */
package zombie.entity.components.crafting;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import zombie.UsedFromLua;
import zombie.core.Translator;
import zombie.core.math.PZMath;
import zombie.core.textures.Texture;
import zombie.debug.DebugType;
import zombie.entity.ComponentType;
import zombie.entity.EntitySimulation;
import zombie.entity.GameEntity;
import zombie.entity.components.crafting.CraftLogic;
import zombie.entity.components.crafting.CraftUtil;
import zombie.entity.components.crafting.recipe.CraftRecipeData;
import zombie.entity.components.resources.Resource;
import zombie.inventory.InventoryItem;
import zombie.iso.weather.ClimateManager;
import zombie.network.GameServer;
import zombie.ui.ObjectTooltip;
import zombie.ui.TextManager;
import zombie.ui.UIFont;

@UsedFromLua
public class DryingCraftLogic
extends CraftLogic {
    private final HashMap<CraftRecipeData, Double> previousTickElapsedTimes = new HashMap();
    private final HashMap<CraftRecipeData, Double> temporaryWetnesses = new HashMap();
    private final ArrayList<Texture> activeStatusIcons = new ArrayList();
    private static final Texture WET_ICON;
    private static final Texture HOT_ICON;
    private static final Texture COLD_ICON;

    private static String getIconSize(String path) {
        double fontScale = (double)TextManager.instance.getFontHeight(UIFont.Small) / 19.0;
        double iconScale = Math.max(1.0, fontScale - Math.floor(fontScale) < 0.5 ? Math.floor(fontScale) : Math.ceil(fontScale));
        if (iconScale <= 1.0) {
            return String.format(path, "12");
        }
        if (iconScale <= 2.0) {
            return String.format(path, "24");
        }
        return String.format(path, "48");
    }

    private DryingCraftLogic() {
        super(ComponentType.DryingCraftLogic);
    }

    @Override
    public void onStart() {
        this.previousTickElapsedTimes.put(this.craftData, 0.0);
        this.temporaryWetnesses.put(this.craftData, 0.0);
        super.onStart();
    }

    @Override
    public void onUpdate(CraftRecipeData craftRecipeData) {
        double thisTickElapsedTime = EntitySimulation.getGameSecondsPerTick();
        if (ClimateManager.getInstance().getPrecipitationIntensity() > 0.0f && this.getGameEntity().isOutside()) {
            double snowModifier = 1.0;
            if (ClimateManager.getInstance().getPrecipitationIsSnow()) {
                snowModifier = 0.5;
            }
            double rainAmount = 0.001 * (double)ClimateManager.getInstance().getPrecipitationIntensity() * snowModifier * EntitySimulation.getGameSecondsPerTick();
            double wetness = Math.min(this.temporaryWetnesses.get(craftRecipeData) + rainAmount, 1.0);
            this.temporaryWetnesses.put(craftRecipeData, wetness);
        } else if (this.temporaryWetnesses.get(craftRecipeData) > 0.0) {
            double dryingAmount = 2.0E-4 * (double)this.getDryingFactor() * EntitySimulation.getGameSecondsPerTick();
            double wetness = Math.max(this.temporaryWetnesses.get(craftRecipeData) - dryingAmount, 0.0);
            this.temporaryWetnesses.put(craftRecipeData, wetness);
        }
        if (this.temporaryWetnesses.get(craftRecipeData) > 0.0) {
            craftRecipeData.setElapsedTime(this.previousTickElapsedTimes.get(craftRecipeData));
        } else {
            double modifiedTickElapsedTime = thisTickElapsedTime * (double)this.getDryingFactor();
            double modifiedElapsedTime = Math.min(this.previousTickElapsedTimes.get(craftRecipeData) + modifiedTickElapsedTime, (double)craftRecipeData.getRecipe().getTime());
            craftRecipeData.setElapsedTime(modifiedElapsedTime);
        }
        this.previousTickElapsedTimes.put(craftRecipeData, craftRecipeData.getElapsedTime());
        super.onUpdate(craftRecipeData);
    }

    private float getDryingFactor() {
        GameEntity entity = this.getGameEntity();
        float temperature = CraftUtil.getEntityTemperature(entity);
        temperature = PZMath.clamp(temperature, 0.0f, 40.0f);
        if ((temperature /= 20.0f) < 1.0f) {
            temperature *= temperature;
        }
        return temperature;
    }

    @Override
    public void doProgressTooltip(ObjectTooltip.Layout layout, Resource resource, CraftRecipeData craftRecipeData) {
        double wetness = this.temporaryWetnesses.containsKey(craftRecipeData) ? this.temporaryWetnesses.get(craftRecipeData) : 0.0;
        super.doProgressTooltip(layout, resource, craftRecipeData);
        if (!this.isRunning()) {
            return;
        }
        float dryingFactor = this.getDryingFactor();
        boolean paused = wetness > 0.0 || dryingFactor <= 0.0f;
        String temperatureText = Translator.getText("EC_CraftLogicTooltip_TemperatureNormal");
        if (dryingFactor <= 0.0f) {
            temperatureText = Translator.getText("EC_CraftLogicTooltip_TemperatureFrozen");
        }
        if (dryingFactor > 0.0f && (double)dryingFactor < 0.9) {
            temperatureText = Translator.getText("EC_CraftLogicTooltip_TemperatureCold");
        }
        if ((double)dryingFactor > 1.1 && dryingFactor < 2.0f) {
            temperatureText = Translator.getText("EC_CraftLogicTooltip_TemperatureWarm");
        }
        if (dryingFactor >= 2.0f) {
            temperatureText = Translator.getText("EC_CraftLogicTooltip_TemperatureHot");
        }
        ObjectTooltip.LayoutItem item = layout.addItem();
        item.setLabel(Translator.getText("EC_CraftLogicTooltip_Temperature") + ":", 1.0f, 1.0f, 1.0f, 1.0f);
        item.setValue(temperatureText, 1.0f, 1.0f, 0.8f, 1.0f);
        if (wetness > 0.0) {
            String wetnessText = String.format(Locale.ENGLISH, "%.0f%%", wetness * 100.0);
            item = layout.addItem();
            item.setLabel(Translator.getText("EC_CraftLogicTooltip_Wetness") + ":", 1.0f, 1.0f, 1.0f, 1.0f);
            item.setValue(wetnessText, 1.0f, 1.0f, 0.8f, 1.0f);
        }
        Object dryingRateText = String.format(Locale.ENGLISH, "%1.1fx", Float.valueOf(dryingFactor));
        if (paused) {
            dryingRateText = Translator.getText("EC_CraftLogicTooltip_Paused");
            ArrayList<String> reasons = new ArrayList<String>();
            if (dryingFactor <= 0.0f) {
                reasons.add(Translator.getText("EC_CraftLogicTooltip_TemperatureCold"));
            }
            if (wetness > 0.0) {
                reasons.add(Translator.getText("EC_CraftLogicTooltip_Wet"));
            }
            dryingRateText = (String)dryingRateText + " (" + String.join((CharSequence)", ", reasons) + ")";
        }
        item = layout.addItem();
        item.setLabel(Translator.getText("EC_CraftLogicTooltip_DryingRate") + ":", 1.0f, 1.0f, 1.0f, 1.0f);
        item.setValue((String)dryingRateText, 1.0f, 1.0f, 0.8f, 1.0f);
    }

    @Override
    public ArrayList<Texture> getStatusIconsForInputItem(InventoryItem item, CraftRecipeData craftRecipeData) {
        double wetness = this.temporaryWetnesses.containsKey(craftRecipeData) ? this.temporaryWetnesses.get(craftRecipeData) : 0.0;
        this.activeStatusIcons.clear();
        float dryingFactor = this.getDryingFactor();
        if ((double)dryingFactor >= 1.35) {
            this.activeStatusIcons.add(HOT_ICON);
        }
        if (dryingFactor <= 0.0f) {
            this.activeStatusIcons.add(COLD_ICON);
        }
        if (wetness > 0.0) {
            this.activeStatusIcons.add(WET_ICON);
        }
        return this.activeStatusIcons;
    }

    @Override
    protected void save(ByteBuffer output) throws IOException {
        super.save(output);
        output.putInt(this.getAllInProgressCraftData().size());
        for (CraftRecipeData key : this.getAllInProgressCraftData()) {
            double wetness = this.temporaryWetnesses.containsKey(key) ? this.temporaryWetnesses.get(key) : 0.0;
            output.putDouble(wetness);
        }
    }

    @Override
    protected void load(ByteBuffer input, int worldVersion) throws IOException {
        super.load(input, worldVersion);
        int wetnessCount = input.getInt();
        int inProgressCount = this.getAllInProgressCraftData().size();
        if (wetnessCount == inProgressCount) {
            for (CraftRecipeData key : this.getAllInProgressCraftData()) {
                this.temporaryWetnesses.put(key, input.getDouble());
                this.previousTickElapsedTimes.put(key, key.getElapsedTime());
            }
        } else {
            DebugType.CraftLogic.error("DryingCraftLogic:load() - Saved wetness table size does not match saved in progress recipe table size. Zeroing wetness values.");
        }
    }

    static {
        if (GameServer.server) {
            WET_ICON = null;
            HOT_ICON = null;
            COLD_ICON = null;
        } else {
            WET_ICON = Texture.getTexture(DryingCraftLogic.getIconSize("media/ui/Entity/SlotStatus/wet_%s.png"));
            HOT_ICON = Texture.getTexture(DryingCraftLogic.getIconSize("media/ui/Entity/SlotStatus/hot_%s.png"));
            COLD_ICON = Texture.getTexture(DryingCraftLogic.getIconSize("media/ui/Entity/SlotStatus/frozen_%s.png"));
        }
    }
}

