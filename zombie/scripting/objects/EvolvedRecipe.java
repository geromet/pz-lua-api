/*
 * Decompiled with CFR 0.152.
 */
package zombie.scripting.objects;

import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.Stack;
import java.util.stream.Collectors;
import zombie.UsedFromLua;
import zombie.characters.IsoGameCharacter;
import zombie.characters.IsoPlayer;
import zombie.characters.skills.PerkFactory;
import zombie.core.Translator;
import zombie.core.logger.LoggerManager;
import zombie.debug.DebugLog;
import zombie.entity.components.fluids.FluidCategory;
import zombie.entity.components.fluids.FluidContainer;
import zombie.inventory.InventoryItem;
import zombie.inventory.InventoryItemFactory;
import zombie.inventory.ItemContainer;
import zombie.inventory.types.Food;
import zombie.inventory.types.HandWeapon;
import zombie.network.GameClient;
import zombie.network.GameServer;
import zombie.network.PacketTypes;
import zombie.network.packets.INetworkPacket;
import zombie.scripting.ScriptManager;
import zombie.scripting.ScriptType;
import zombie.scripting.objects.BaseScriptObject;
import zombie.scripting.objects.ItemRecipe;
import zombie.scripting.objects.ItemTag;
import zombie.scripting.objects.UniqueRecipe;
import zombie.util.StringUtils;

@UsedFromLua
public final class EvolvedRecipe
extends BaseScriptObject {
    private static final DecimalFormat DECIMAL_FORMAT = (DecimalFormat)NumberFormat.getInstance(Locale.US);
    public String name;
    public String displayName;
    private String originalname;
    public int maxItems;
    public final Map<String, ItemRecipe> itemsList = new HashMap<String, ItemRecipe>();
    public String resultItem;
    public String baseItem;
    public boolean cookable;
    public boolean addIngredientIfCooked;
    public boolean canAddSpicesEmpty;
    public String addIngredientSound;
    public boolean hidden;
    public boolean allowFrozenItem;
    public String template;
    public Float minimumWater = Float.valueOf(0.0f);

    public EvolvedRecipe(String name) {
        super(ScriptType.EvolvedRecipe);
        this.name = name;
    }

    @Override
    public void Load(String name, String token) throws Exception {
        String[] waypoint = token.split("[{}]");
        String[] coords = waypoint[1].split(",");
        this.LoadCommonBlock(token);
        this.Load(name, coords);
    }

    private void Load(String name, String[] strArray) {
        this.displayName = Translator.getRecipeName(name);
        this.originalname = name;
        for (int i = 0; i < strArray.length; ++i) {
            if (strArray[i].trim().isEmpty() || !strArray[i].contains("=")) continue;
            String[] split = strArray[i].split("=", 2);
            String key = split[0].trim();
            String value = split[1].trim();
            if (key.equals("BaseItem")) {
                this.baseItem = value;
                continue;
            }
            if (key.equals("Name")) {
                this.displayName = Translator.getRecipeName(value);
                this.originalname = value;
                continue;
            }
            if (key.equals("ResultItem")) {
                this.resultItem = value;
                continue;
            }
            if (key.equals("Cookable")) {
                this.cookable = true;
                continue;
            }
            if (key.equals("MaxItems")) {
                this.maxItems = Integer.parseInt(value);
                continue;
            }
            if (key.equals("AddIngredientIfCooked")) {
                this.addIngredientIfCooked = Boolean.parseBoolean(value);
                continue;
            }
            if (key.equals("AddIngredientSound")) {
                this.addIngredientSound = StringUtils.discardNullOrWhitespace(value);
                continue;
            }
            if (key.equals("CanAddSpicesEmpty")) {
                this.canAddSpicesEmpty = Boolean.parseBoolean(value);
                continue;
            }
            if (key.equals("IsHidden")) {
                this.hidden = Boolean.parseBoolean(value);
                continue;
            }
            if (key.equals("AllowFrozenItem")) {
                this.allowFrozenItem = Boolean.parseBoolean(value);
                continue;
            }
            if (key.equals("Template")) {
                this.template = value;
                continue;
            }
            if (!key.equals("MinimumWater")) continue;
            this.minimumWater = Float.valueOf(Float.parseFloat(value));
        }
        if (this.template == null) {
            this.template = name;
        }
    }

    public boolean needToBeCooked(InventoryItem itemTest) {
        ItemRecipe itemRecipe = this.getItemRecipe(itemTest);
        if (itemRecipe == null) {
            return true;
        }
        return itemRecipe.cooked.booleanValue() == itemTest.isCooked() || itemRecipe.cooked.booleanValue() == itemTest.isBurnt() || itemRecipe.cooked == false;
    }

    public ArrayList<InventoryItem> getItemsCanBeUse(IsoGameCharacter chr, InventoryItem baseItem, ArrayList<ItemContainer> containers) {
        int cookingLvl = chr.getPerkLevel(PerkFactory.Perks.Cooking);
        if (containers == null) {
            containers = new ArrayList();
        }
        ArrayList<InventoryItem> result = new ArrayList<InventoryItem>();
        if (!baseItem.haveExtraItems() && this.getMinimumWater() > 0.0f && !this.hasMinimumWater(baseItem)) {
            return result;
        }
        if (!baseItem.haveExtraItems() && this.getMinimumWater() == 0.0f && baseItem.getFluidContainer() != null && !baseItem.getFluidContainer().isEmpty()) {
            return result;
        }
        Iterator<String> it = this.itemsList.keySet().iterator();
        if (!containers.contains(chr.getInventory())) {
            containers.add(chr.getInventory());
        }
        while (it.hasNext()) {
            String type = it.next();
            for (ItemContainer itemContainer : containers) {
                this.checkItemCanBeUse(itemContainer, type, baseItem, cookingLvl, result, chr);
            }
        }
        if (baseItem.haveExtraItems() && baseItem.getExtraItems().size() >= 3) {
            IsoPlayer player = (IsoPlayer)chr;
            for (int c = 0; c < containers.size(); ++c) {
                ItemContainer container = containers.get(c);
                for (int i = 0; i < container.getItems().size(); ++i) {
                    Food food;
                    InventoryItem item = container.getItems().get(i);
                    boolean noUseInRecipes = false;
                    if (player != null && item.isNoRecipes(player)) {
                        noUseInRecipes = true;
                    }
                    if (!(item instanceof Food) || (food = (Food)item).getPoisonLevelForRecipe() < 0 || !chr.isKnownPoison(item) || result.contains(item) || noUseInRecipes) continue;
                    result.add(item);
                }
            }
        }
        return result;
    }

    public boolean isItemUsableInRecipe(IsoGameCharacter chr, InventoryItem baseItem, Integer id) {
        return this.getItemsCanBeUse(chr, baseItem, null).stream().map(InventoryItem::getID).collect(Collectors.toCollection(ArrayList::new)).contains(id);
    }

    /*
     * Enabled aggressive block sorting
     */
    private void checkItemCanBeUse(ItemContainer itemContainer, String type, InventoryItem baseItem, int cookingLvl, ArrayList<InventoryItem> result, IsoGameCharacter chr) {
        ArrayList<InventoryItem> itemsInChr = itemContainer.getItemsFromType(type);
        IsoPlayer player = (IsoPlayer)chr;
        int i = 0;
        while (true) {
            boolean ok;
            InventoryItem item;
            block22: {
                block23: {
                    block19: {
                        Food itemFood;
                        block21: {
                            block20: {
                                if (i >= itemsInChr.size()) {
                                    return;
                                }
                                item = itemsInChr.get(i);
                                ok = false;
                                if (!(item instanceof Food)) break block19;
                                itemFood = (Food)item;
                                if (this.itemsList.get((Object)type).use == -1) break block19;
                                if (!itemFood.isSpice()) break block20;
                                if (this.isResultItem(baseItem)) {
                                    ok = !this.isSpiceAdded(baseItem, item);
                                } else if (this.canAddSpicesEmpty) {
                                    ok = true;
                                }
                                if (itemFood.isBurnt()) {
                                    ok = false;
                                    break block21;
                                } else if (itemFood.isRotten() && cookingLvl < 7) {
                                    ok = false;
                                }
                                break block21;
                            }
                            if (!baseItem.haveExtraItems() || baseItem.extraItems.size() < this.maxItems) {
                                if (itemFood.isBurnt()) {
                                    ok = false;
                                } else if (!itemFood.isRotten() || cookingLvl >= 7) {
                                    ok = true;
                                }
                            }
                        }
                        if (itemFood.isFrozen() && !this.allowFrozenItem) {
                            ok = false;
                        }
                        if (itemFood.isbDangerousUncooked() && !itemFood.isCooked() && !((InventoryItem)InventoryItemFactory.CreateItem(this.resultItem)).isCookable()) {
                            // empty if block
                        }
                        break block22;
                    }
                    if (!item.isSpice()) break block23;
                    if (this.isResultItem(baseItem)) {
                        ok = !this.isSpiceAdded(baseItem, item);
                        break block22;
                    } else if (this.canAddSpicesEmpty) {
                        ok = true;
                    }
                    break block22;
                }
                ok = true;
            }
            if (player != null && item.isNoRecipes(player)) {
                ok = false;
            }
            ItemRecipe itemRecipe = this.getItemRecipe(item);
            if (ok) {
                result.add(item);
            }
            ++i;
        }
    }

    public InventoryItem addItem(InventoryItem baseItem, InventoryItem usedItem, IsoGameCharacter chr) {
        Food food;
        int cookingLvl = chr.getPerkLevel(PerkFactory.Perks.Cooking);
        ItemContainer usedItemContainer = usedItem.getContainer();
        if (!this.isResultItem(baseItem)) {
            InventoryItem previousItem = baseItem instanceof Food ? baseItem : null;
            Object item = InventoryItemFactory.CreateItem(this.resultItem);
            if (item != null) {
                if ((double)baseItem.getColorRed() != 1.0 || (double)baseItem.getColorGreen() != 1.0 || (double)baseItem.getColorBlue() != 1.0) {
                    ((InventoryItem)item).setColorRed(baseItem.getColorRed());
                    ((InventoryItem)item).setColorGreen(baseItem.getColorGreen());
                    ((InventoryItem)item).setColorBlue(baseItem.getColorBlue());
                }
                if (baseItem.getModelIndex() != -1) {
                    ((InventoryItem)item).setModelIndex(baseItem.getModelIndex());
                }
                if (baseItem instanceof HandWeapon) {
                    ((InventoryItem)item).setConditionFrom(baseItem);
                    ((InventoryItem)item).getModData().rawset("condition:" + baseItem.getType(), (Object)((double)baseItem.getCondition() / (double)baseItem.getConditionMax()));
                }
                InventoryItem oldBaseItem = baseItem;
                baseItem = item;
                if (baseItem instanceof Food) {
                    Food food2 = (Food)baseItem;
                    food2.setCalories(0.0f);
                    food2.setCarbohydrates(0.0f);
                    food2.setProteins(0.0f);
                    food2.setLipids(0.0f);
                    baseItem.setIsCookable(this.cookable);
                    if (previousItem != null) {
                        food2.setHungChange(((Food)previousItem).getHungChange());
                        food2.setBaseHunger(((Food)previousItem).getBaseHunger());
                    } else {
                        food2.setHungChange(0.0f);
                        food2.setBaseHunger(0.0f);
                    }
                    if (oldBaseItem instanceof Food && oldBaseItem.getOffAgeMax() != 1000000000 && baseItem.getOffAgeMax() != 1000000000) {
                        float age = oldBaseItem.getAge() / (float)oldBaseItem.getOffAgeMax();
                        baseItem.setAge((float)baseItem.getOffAgeMax() * age);
                    }
                    if (previousItem instanceof Food) {
                        Food prevFood = (Food)previousItem;
                        food2.setTainted(prevFood.isTainted());
                        food2.setCalories(prevFood.getCalories());
                        food2.setProteins(prevFood.getProteins());
                        food2.setLipids(prevFood.getLipids());
                        food2.setCarbohydrates(prevFood.getCarbohydrates());
                        food2.setThirstChange(prevFood.getThirstChange());
                    }
                }
                baseItem.setUnhappyChange(0.0f);
                baseItem.setBoredomChange(0.0f);
                ((InventoryItem)item).setCondition(oldBaseItem.getCondition(), false);
                ((InventoryItem)item).setFavorite(oldBaseItem.isFavorite());
                chr.getInventory().Remove(oldBaseItem);
                chr.getInventory().AddItem(baseItem);
                if (GameServer.server) {
                    GameServer.sendReplaceItemInContainer(chr.getInventory(), oldBaseItem, baseItem);
                }
            }
        }
        if (this.itemsList.get(usedItem.getType()) != null && this.itemsList.get((Object)usedItem.getType()).use > -1) {
            if (usedItem instanceof Food) {
                DecimalFormat df;
                boolean herbalTea;
                Food usedItemFood = (Food)usedItem;
                float usedHunger = (float)this.itemsList.get((Object)usedItem.getType()).use.intValue() / 100.0f;
                Food baseItemFood = (Food)baseItem;
                boolean bl = herbalTea = baseItemFood.hasTag(ItemTag.HERBAL_TEA) && usedItemFood.hasTag(ItemTag.HERBAL_TEA);
                if (usedItemFood.isSpice() && baseItem instanceof Food) {
                    Food food3 = (Food)baseItem;
                    if (baseItem instanceof Food && herbalTea) {
                        baseItemFood.setFoodSicknessChange(baseItemFood.getFoodSicknessChange() + usedItemFood.getFoodSicknessChange());
                        baseItemFood.setPainReduction(baseItemFood.getPainReduction() + usedItemFood.getPainReduction());
                        baseItemFood.setFluReduction(baseItemFood.getFluReduction() + usedItemFood.getFluReduction());
                        baseItemFood.setStressChange(baseItemFood.getStressChange() + usedItemFood.getStressChange());
                        baseItemFood.setReduceInfectionPower(baseItemFood.getReduceInfectionPower() + usedItemFood.getReduceInfectionPower());
                        if (usedItemFood.getEnduranceChange() > 0.0f) {
                            baseItemFood.setEnduranceChange(baseItemFood.getEnduranceChange() + usedItemFood.getEnduranceChange());
                        }
                        if (baseItemFood.getFoodSicknessChange() > 12) {
                            baseItemFood.setFoodSicknessChange(12);
                        }
                        if (usedItemFood.hasTag(ItemTag.BOOSTS_FLU_RECOVERY)) {
                            baseItemFood.setFluReduction(baseItemFood.getFluReduction() + 5);
                        }
                    }
                    this.useSpice(usedItemFood, food3, usedHunger, cookingLvl, chr);
                    return baseItem;
                }
                boolean useAll = false;
                if (usedItemFood.isRotten()) {
                    df = DECIMAL_FORMAT;
                    df.setRoundingMode(RoundingMode.HALF_EVEN);
                    if (cookingLvl == 7 || cookingLvl == 8) {
                        usedHunger = Float.parseFloat(df.format(Math.abs(usedItemFood.getBaseHunger() - (usedItemFood.getBaseHunger() - 0.05f * usedItemFood.getBaseHunger()))).replace(",", "."));
                    } else if (cookingLvl == 9 || cookingLvl == 10) {
                        usedHunger = Float.parseFloat(df.format(Math.abs(usedItemFood.getBaseHunger() - (usedItemFood.getBaseHunger() - 0.1f * usedItemFood.getBaseHunger()))).replace(",", "."));
                    }
                    useAll = true;
                }
                if (Math.abs(usedItemFood.getHungerChange()) < usedHunger) {
                    df = DECIMAL_FORMAT;
                    df.setRoundingMode(RoundingMode.DOWN);
                    usedHunger = Math.abs(Float.parseFloat(df.format(usedItemFood.getHungerChange()).replace(",", ".")));
                    useAll = true;
                }
                if (baseItem instanceof Food) {
                    float realUsedHunger;
                    float percentageUsed;
                    Food food4 = (Food)baseItem;
                    baseItemFood.setHungChange(baseItemFood.getHungChange() - usedHunger);
                    baseItemFood.setBaseHunger(baseItemFood.getBaseHunger() - usedHunger);
                    if (usedItemFood.isbDangerousUncooked() && !usedItemFood.isCooked() && !usedItemFood.isBurnt()) {
                        baseItemFood.setbDangerousUncooked(true);
                    }
                    int changer = 0;
                    if (baseItem.extraItems != null) {
                        for (int i = 0; i < baseItem.extraItems.size(); ++i) {
                            if (!baseItem.extraItems.get(i).equals(usedItem.getFullType())) continue;
                            ++changer;
                        }
                    }
                    if (baseItem.extraItems != null && baseItem.extraItems.size() - 2 > cookingLvl) {
                        changer += baseItem.extraItems.size() - 2 - cookingLvl * 3;
                    }
                    if ((percentageUsed = Math.abs((realUsedHunger = usedHunger - (float)(3 * cookingLvl) / 100.0f * usedHunger) / usedItemFood.getHungChange())) > 1.0f) {
                        percentageUsed = 1.0f;
                    }
                    baseItem.setUnhappyChange(food4.getUnhappyChangeUnmodified() - (float)(5 - changer * 5));
                    if (baseItem.getUnhappyChange() > 25.0f) {
                        baseItem.setUnhappyChange(25.0f);
                    }
                    float nutritionBoost = (float)cookingLvl / 15.0f + 1.0f;
                    baseItemFood.setCalories(baseItemFood.getCalories() + usedItemFood.getCalories() * nutritionBoost * percentageUsed);
                    baseItemFood.setProteins(baseItemFood.getProteins() + usedItemFood.getProteins() * nutritionBoost * percentageUsed);
                    baseItemFood.setCarbohydrates(baseItemFood.getCarbohydrates() + usedItemFood.getCarbohydrates() * nutritionBoost * percentageUsed);
                    baseItemFood.setLipids(baseItemFood.getLipids() + usedItemFood.getLipids() * nutritionBoost * percentageUsed);
                    float thirstChange = usedItemFood.getThirstChangeUnmodified() * nutritionBoost * percentageUsed;
                    if (!usedItemFood.hasTag(ItemTag.DRIED_FOOD)) {
                        baseItemFood.setThirstChange(baseItemFood.getThirstChangeUnmodified() + thirstChange);
                    }
                    if (usedItemFood.isCooked()) {
                        realUsedHunger = (float)((double)realUsedHunger / 1.3);
                    }
                    usedItemFood.setHungChange(usedItemFood.getHungChange() + realUsedHunger);
                    usedItemFood.setBaseHunger(usedItemFood.getBaseHunger() + realUsedHunger);
                    usedItemFood.setThirstChange(usedItemFood.getThirstChange() - thirstChange);
                    usedItemFood.setUnhappyChange(usedItemFood.getUnhappyChange() - usedItemFood.getUnhappyChange() * percentageUsed);
                    usedItemFood.setCalories(usedItemFood.getCalories() - usedItemFood.getCalories() * percentageUsed);
                    usedItemFood.setProteins(usedItemFood.getProteins() - usedItemFood.getProteins() * percentageUsed);
                    usedItemFood.setCarbohydrates(usedItemFood.getCarbohydrates() - usedItemFood.getCarbohydrates() * percentageUsed);
                    usedItemFood.setLipids(usedItemFood.getLipids() - usedItemFood.getLipids() * percentageUsed);
                    if (baseItemFood.hasTag(ItemTag.ALCOHOLIC_BEVERAGE) && usedItemFood.isAlcoholic()) {
                        baseItemFood.setAlcoholic(true);
                    }
                    if (herbalTea) {
                        baseItemFood.setFoodSicknessChange(baseItemFood.getFoodSicknessChange() + usedItemFood.getFoodSicknessChange());
                        baseItemFood.setPainReduction(baseItemFood.getPainReduction() + usedItemFood.getPainReduction());
                        baseItemFood.setFluReduction(baseItemFood.getFluReduction() + usedItemFood.getFluReduction());
                        baseItemFood.setStressChange(baseItemFood.getStressChange() + usedItemFood.getStressChange());
                        baseItemFood.setReduceInfectionPower(baseItemFood.getReduceInfectionPower() + usedItemFood.getReduceInfectionPower());
                        if (baseItemFood.getFoodSicknessChange() > 12) {
                            baseItemFood.setFoodSicknessChange(12);
                        }
                        if (usedItemFood.hasTag(ItemTag.BOOSTS_FLU_RECOVERY)) {
                            baseItemFood.setFluReduction(baseItemFood.getFluReduction() + 5);
                        }
                    }
                    if ((double)usedItemFood.getHungerChange() >= -0.02 || useAll) {
                        usedItem.UseAndSync();
                    }
                    if (usedItemFood.getFatigueChange() < 0.0f) {
                        baseItem.setFatigueChange(usedItemFood.getFatigueChange() * percentageUsed);
                        usedItemFood.setFatigueChange(usedItemFood.getFatigueChange() - usedItemFood.getFatigueChange() * percentageUsed);
                    }
                }
            } else {
                if (usedItem.getScriptItem().isSpice() && baseItem instanceof Food) {
                    Food food5 = (Food)baseItem;
                    this.useSpice(usedItem, food5, 1, cookingLvl, chr);
                    return baseItem;
                }
                usedItem.UseAndSync();
            }
            baseItem.addExtraItem(usedItem.getFullType());
            if (GameServer.server) {
                if (baseItem.getContainer().getParent() instanceof IsoPlayer) {
                    INetworkPacket.send((IsoPlayer)baseItem.getContainer().getParent(), PacketTypes.PacketType.ItemStats, baseItem.getContainer(), baseItem);
                } else {
                    INetworkPacket.sendToRelative(PacketTypes.PacketType.ItemStats, chr.getX(), chr.getY(), baseItem.getContainer(), this);
                }
            }
        }
        if (usedItem instanceof Food && (food = (Food)usedItem).getPoisonPower() > 0) {
            this.addPoison(usedItem, baseItem, chr, usedItemContainer);
        }
        this.checkUniqueRecipe(baseItem);
        if (GameServer.server) {
            GameServer.addXp((IsoPlayer)chr, PerkFactory.Perks.Cooking, 3.0f);
        } else if (!GameClient.client) {
            chr.getXp().AddXP(PerkFactory.Perks.Cooking, 3.0f);
        }
        return baseItem;
    }

    private void checkUniqueRecipe(InventoryItem baseItem) {
        if (baseItem instanceof Food) {
            Food food = (Food)baseItem;
            Stack<UniqueRecipe> uniqueRecipe = ScriptManager.instance.getAllUniqueRecipes();
            for (int i = 0; i < uniqueRecipe.size(); ++i) {
                ArrayList<Integer> usedIndex = new ArrayList<Integer>();
                UniqueRecipe recipe = (UniqueRecipe)uniqueRecipe.get(i);
                if (!recipe.getBaseRecipe().equals(baseItem.getType())) continue;
                boolean findAll2 = true;
                for (int j = 0; j < recipe.getItems().size(); ++j) {
                    boolean ok = false;
                    for (int x = 0; x < food.getExtraItems().size(); ++x) {
                        if (usedIndex.contains(x) || !food.getExtraItems().get(x).equals(recipe.getItems().get(j))) continue;
                        ok = true;
                        usedIndex.add(x);
                        break;
                    }
                    if (ok) continue;
                    findAll2 = false;
                    break;
                }
                if (food.getExtraItems().size() != recipe.getItems().size() || !findAll2) continue;
                food.setName(recipe.getName());
                food.setBaseHunger(food.getBaseHunger() - (float)recipe.getHungerBonus() / 100.0f);
                food.setHungChange(food.getBaseHunger());
                food.setBoredomChange(food.getBoredomChangeUnmodified() - (float)recipe.getBoredomBonus());
                food.setUnhappyChange(food.getUnhappyChangeUnmodified() - (float)recipe.getHapinessBonus());
                food.setCustomName(true);
            }
        }
    }

    private void addPoison(InventoryItem usedItem, InventoryItem baseItem, IsoGameCharacter chr, ItemContainer usedItemContainer) {
        Food usedItemFood = (Food)usedItem;
        if (baseItem instanceof Food) {
            Food baseItemFood = (Food)baseItem;
            if (baseItemFood.getPoisonDetectionLevel() == -1) {
                baseItemFood.setPoisonDetectionLevel(0);
            }
            baseItemFood.setPoisonDetectionLevel(baseItemFood.getPoisonDetectionLevel() + usedItemFood.getPoisonDetectionLevel());
            if (baseItemFood.getPoisonDetectionLevel() > 10) {
                baseItemFood.setPoisonDetectionLevel(10);
            }
            int usedPoisonPower = usedItemFood.getPoisonPower();
            int newBaseItemFoodPoisonPower = baseItemFood.getPoisonPower() + usedPoisonPower;
            baseItemFood.setPoisonPower(baseItemFood.getPoisonPower() + usedPoisonPower);
            usedItemFood.setPoisonPower(usedItemFood.getPoisonPower() - usedPoisonPower);
            baseItemFood.getModData().rawset("addedPoisonBy", (Object)chr.getFullName());
            String debugStr = String.format("Char %s poisoned item %s with power %d", chr.getName(), baseItem.getDisplayName(), newBaseItemFoodPoisonPower);
            DebugLog.Objects.debugln(debugStr);
            LoggerManager.getLogger("user").write(debugStr);
            if (GameServer.server) {
                if (baseItem.getContainer().getParent() instanceof IsoPlayer) {
                    INetworkPacket.send((IsoPlayer)baseItem.getContainer().getParent(), PacketTypes.PacketType.ItemStats, baseItem.getContainer(), baseItem);
                } else {
                    INetworkPacket.sendToRelative(PacketTypes.PacketType.ItemStats, chr.getX(), chr.getY(), baseItem.getContainer(), baseItem);
                }
                INetworkPacket.sendToRelative(PacketTypes.PacketType.SyncItemModData, (int)chr.getX(), (float)((int)chr.getY()), baseItem);
                if (usedItemContainer.getParent() instanceof IsoPlayer) {
                    INetworkPacket.send((IsoPlayer)usedItemContainer.getParent(), PacketTypes.PacketType.ItemStats, usedItemContainer, usedItem);
                } else {
                    INetworkPacket.sendToRelative(PacketTypes.PacketType.ItemStats, chr.getX(), chr.getY(), usedItemContainer, usedItem);
                }
            }
        }
    }

    private void useSpice(Food usedSpice, Food baseItem, float usedHunger, int cookingLvl, IsoGameCharacter chr) {
        if (!this.isSpiceAdded(baseItem, usedSpice)) {
            float percentageUsed;
            if (baseItem.spices == null) {
                baseItem.spices = new ArrayList();
            }
            baseItem.spices.add(usedSpice.getFullType());
            float realUsedHunger = usedHunger;
            if (usedSpice.isRotten()) {
                DecimalFormat df = DECIMAL_FORMAT;
                df.setRoundingMode(RoundingMode.HALF_EVEN);
                if (cookingLvl == 7 || cookingLvl == 8) {
                    usedHunger = Float.parseFloat(df.format(Math.abs(usedSpice.getBaseHunger() - (usedSpice.getBaseHunger() - 0.05f * usedSpice.getBaseHunger()))).replace(",", "."));
                } else if (cookingLvl == 9 || cookingLvl == 10) {
                    usedHunger = Float.parseFloat(df.format(Math.abs(usedSpice.getBaseHunger() - (usedSpice.getBaseHunger() - 0.1f * usedSpice.getBaseHunger()))).replace(",", "."));
                }
            }
            if ((percentageUsed = Math.abs(usedHunger / usedSpice.getHungChange())) > 1.0f) {
                percentageUsed = 1.0f;
            }
            float nutritionBoost = (float)cookingLvl / 15.0f + 1.0f;
            baseItem.setUnhappyChange(baseItem.getUnhappyChangeUnmodified() - usedHunger * 200.0f);
            baseItem.setBoredomChange(baseItem.getBoredomChangeUnmodified() - usedHunger * 200.0f);
            baseItem.setCalories(baseItem.getCalories() + usedSpice.getCalories() * nutritionBoost * percentageUsed);
            baseItem.setProteins(baseItem.getProteins() + usedSpice.getProteins() * nutritionBoost * percentageUsed);
            baseItem.setCarbohydrates(baseItem.getCarbohydrates() + usedSpice.getCarbohydrates() * nutritionBoost * percentageUsed);
            baseItem.setLipids(baseItem.getLipids() + usedSpice.getLipids() * nutritionBoost * percentageUsed);
            percentageUsed = Math.abs(realUsedHunger / usedSpice.getHungChange());
            if (percentageUsed > 1.0f) {
                percentageUsed = 1.0f;
            }
            usedSpice.setCalories(usedSpice.getCalories() - usedSpice.getCalories() * percentageUsed);
            usedSpice.setProteins(usedSpice.getProteins() - usedSpice.getProteins() * percentageUsed);
            usedSpice.setCarbohydrates(usedSpice.getCarbohydrates() - usedSpice.getCarbohydrates() * percentageUsed);
            usedSpice.setLipids(usedSpice.getLipids() - usedSpice.getLipids() * percentageUsed);
            usedSpice.setHungChange(usedSpice.getHungChange() + realUsedHunger);
            if ((double)usedSpice.getHungerChange() > -0.01) {
                usedSpice.UseAndSync();
            }
            if (GameServer.server) {
                if (baseItem.getContainer().getParent() instanceof IsoPlayer) {
                    INetworkPacket.send((IsoPlayer)baseItem.getContainer().getParent(), PacketTypes.PacketType.ItemStats, baseItem.getContainer(), baseItem);
                } else {
                    INetworkPacket.sendToRelative(PacketTypes.PacketType.ItemStats, chr.getX(), chr.getY(), baseItem.getContainer(), baseItem);
                }
            }
        }
    }

    private void useSpice(InventoryItem usedSpice, Food baseItem, int uses, int cookingLvl, IsoGameCharacter chr) {
        if (!this.isSpiceAdded(baseItem, usedSpice)) {
            if (baseItem.spices == null) {
                baseItem.spices = new ArrayList();
            }
            baseItem.spices.add(usedSpice.getFullType());
            usedSpice.UseAndSync();
            if (GameServer.server) {
                if (baseItem.getContainer().getParent() instanceof IsoPlayer) {
                    INetworkPacket.send((IsoPlayer)baseItem.getContainer().getParent(), PacketTypes.PacketType.ItemStats, baseItem.getContainer(), baseItem);
                } else {
                    INetworkPacket.sendToRelative(PacketTypes.PacketType.ItemStats, chr.getX(), chr.getY(), baseItem.getContainer(), baseItem);
                }
            }
        }
    }

    public ItemRecipe getItemRecipe(InventoryItem usedItem) {
        return this.itemsList.get(usedItem.getType());
    }

    public String getName() {
        return this.displayName;
    }

    public String getOriginalname() {
        return this.originalname;
    }

    public String getUntranslatedName() {
        return this.name;
    }

    public String getBaseItem() {
        return this.baseItem;
    }

    public float getMinimumWater() {
        return this.minimumWater.floatValue();
    }

    public boolean hasMinimumWater(InventoryItem item) {
        if (item.getFluidContainer() == null) {
            return false;
        }
        FluidContainer fluidCont = item.getFluidContainer();
        if (!fluidCont.isAllCategory(FluidCategory.Water)) {
            return false;
        }
        float water = fluidCont.getFilledRatio();
        return !(water < this.getMinimumWater());
    }

    public Map<String, ItemRecipe> getItemsList() {
        return this.itemsList;
    }

    public ArrayList<ItemRecipe> getPossibleItems() {
        ArrayList<ItemRecipe> result = new ArrayList<ItemRecipe>();
        for (ItemRecipe recipe : this.itemsList.values()) {
            result.add(recipe);
        }
        return result;
    }

    public String getResultItem() {
        if (!this.resultItem.contains(".")) {
            return this.resultItem;
        }
        return this.resultItem.split("\\.")[1];
    }

    public String getFullResultItem() {
        return this.resultItem;
    }

    public boolean isCookable() {
        return this.cookable;
    }

    public int getMaxItems() {
        return this.maxItems;
    }

    public boolean isResultItem(InventoryItem item) {
        if (item == null) {
            return false;
        }
        return this.getResultItem().equals(item.getType());
    }

    public boolean isSpiceAdded(InventoryItem baseItem, InventoryItem spiceItem) {
        Food food;
        block6: {
            block5: {
                if (!this.isResultItem(baseItem)) {
                    return false;
                }
                if (!(baseItem instanceof Food)) break block5;
                food = (Food)baseItem;
                if (spiceItem.isSpice()) break block6;
            }
            return false;
        }
        ArrayList<String> spices = food.getSpices();
        if (spices == null) {
            return false;
        }
        return spices.contains(spiceItem.getFullType());
    }

    public String getAddIngredientSound() {
        return this.addIngredientSound;
    }

    public void setIsHidden(boolean hide) {
        this.hidden = hide;
    }

    public boolean isHidden() {
        return this.hidden;
    }

    public boolean isAllowFrozenItem() {
        return this.allowFrozenItem;
    }

    public void setAllowFrozenItem(boolean allow) {
        this.allowFrozenItem = allow;
    }

    static {
        DECIMAL_FORMAT.applyPattern("#.##");
    }
}

