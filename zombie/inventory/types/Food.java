/*
 * Decompiled with CFR 0.152.
 */
package zombie.inventory.types;

import fmod.fmod.FMODManager;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import se.krka.kahlua.j2se.KahluaTableImpl;
import zombie.GameTime;
import zombie.GameWindow;
import zombie.Lua.LuaEventManager;
import zombie.Lua.LuaManager;
import zombie.SandboxOptions;
import zombie.UsedFromLua;
import zombie.audio.BaseSoundEmitter;
import zombie.audio.SoundInstanceLimiter;
import zombie.audio.SoundLimiterParams;
import zombie.characters.IsoGameCharacter;
import zombie.characters.IsoPlayer;
import zombie.characters.SurvivorDesc;
import zombie.characters.animals.AnimalDefinitions;
import zombie.characters.animals.AnimalGene;
import zombie.characters.animals.IsoAnimal;
import zombie.characters.skills.PerkFactory;
import zombie.core.Core;
import zombie.core.Translator;
import zombie.core.properties.IsoPropertyType;
import zombie.core.random.Rand;
import zombie.core.textures.ColorInfo;
import zombie.core.textures.Texture;
import zombie.core.utils.UpdateLimit;
import zombie.debug.DebugLog;
import zombie.debug.DebugOptions;
import zombie.inventory.InventoryItem;
import zombie.inventory.InventoryItemFactory;
import zombie.inventory.ItemContainer;
import zombie.inventory.ItemSoundManager;
import zombie.inventory.types.AnimalInventoryItem;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoObject;
import zombie.iso.IsoWorld;
import zombie.iso.objects.IsoCompost;
import zombie.iso.objects.IsoFireManager;
import zombie.iso.objects.IsoFireplace;
import zombie.iso.objects.IsoHutch;
import zombie.iso.objects.IsoWorldInventoryObject;
import zombie.network.GameClient;
import zombie.network.GameServer;
import zombie.network.PacketTypes;
import zombie.network.packets.INetworkPacket;
import zombie.scripting.ScriptManager;
import zombie.scripting.objects.CharacterTrait;
import zombie.scripting.objects.Item;
import zombie.scripting.objects.ItemTag;
import zombie.scripting.objects.ItemType;
import zombie.scripting.objects.ModelScript;
import zombie.ui.ObjectTooltip;
import zombie.util.StringUtils;
import zombie.util.Type;
import zombie.util.io.BitHeader;
import zombie.util.io.BitHeaderRead;
import zombie.util.io.BitHeaderWrite;
import zombie.vehicles.BaseVehicle;

@UsedFromLua
public final class Food
extends InventoryItem {
    private static final UpdateLimit updateAgeRate = new UpdateLimit(1000L);
    protected boolean badCold;
    protected boolean goodHot;
    private static final float MIN_HEAT = 0.2f;
    private static final float MAX_HEAT = 3.0f;
    protected float heat = 1.0f;
    protected float endChange;
    protected float hungChange;
    protected String useOnConsume;
    protected boolean rotten;
    protected boolean dangerousUncooked;
    protected int lastCookMinute;
    public float thirstChange;
    public boolean poison;
    private List<String> replaceOnCooked;
    private float baseHunger;
    public ArrayList<String> spices;
    private boolean isSpice;
    private boolean isTainted;
    private int poisonDetectionLevel = -1;
    private int poisonLevelForRecipe;
    private int useForPoison;
    private int poisonPower;
    private String foodType;
    private String customEatSound;
    private boolean removeNegativeEffectOnCooked;
    private String chef;
    private String onCooked;
    private String worldTextureCooked;
    private String worldTextureRotten;
    private String worldTextureOverdone;
    private int fluReduction;
    private int foodSicknessChange;
    private float painReduction;
    private String herbalistType;
    private float carbohydrates;
    private float lipids;
    private float proteins;
    private float calories;
    private boolean packaged;
    private float freezingTime;
    private boolean frozen;
    private boolean canBeFrozen = true;
    protected float lastFrozenUpdate = -1.0f;
    public static final float FreezerAgeMultiplier = 0.0f;
    private String replaceOnRotten;
    private final boolean forceFoodTypeAsName = false;
    private float rottenTime;
    private float compostTime;
    private String onEat;
    private boolean badInMicrowave;
    private boolean cookedInMicrowave;
    private long cookingSound;
    private int cookingParameter = -1;
    private String soundLimiterGroupID_Burning;
    private int milkQty;
    private String milkType;
    private boolean fertilized;
    private int fertilizedTime;
    private int timeToHatch;
    private String animalHatch;
    private String animalHatchBreed;
    private long lastEggTimeCheck;
    public int motherId;
    public HashMap<String, AnimalGene> eggGenome;
    private long lastUpdateTime;
    private float temperatureTimeAccum;
    private static final int COOKING_STATE_COOKING = 0;
    private static final int COOKING_STATE_BURNING = 1;
    static final short SLP_COOKING_SOUND = 0;
    static final short SLP_COOKING_PARAMETER = 1;

    @Override
    public String getCategory() {
        if (this.mainCategory != null) {
            return this.mainCategory;
        }
        return "Food";
    }

    public Food(String module, String name, String itemType, String texName) {
        super(module, name, itemType, texName);
        Texture.warnFailFindTexture = false;
        this.texturerotten = Texture.trygetTexture(texName + "Rotten");
        String rottenSuffix = "Rotten.png";
        if (this.texturerotten == null) {
            this.texturerotten = Texture.trygetTexture(texName + "Spoiled");
            if (this.texturerotten != null) {
                rottenSuffix = "Spoiled.png";
            }
        }
        if (this.texturerotten == null) {
            this.texturerotten = Texture.trygetTexture(texName + "_Rotten");
            if (this.texturerotten != null) {
                rottenSuffix = "_Rotten.png";
            }
        }
        this.textureCooked = Texture.trygetTexture(texName + "Cooked");
        String cookedSuffix = "Cooked.png";
        if (this.textureCooked == null) {
            this.textureCooked = Texture.trygetTexture(texName + "_Cooked");
            if (this.textureCooked != null) {
                cookedSuffix = "_Cooked.png";
            }
        }
        this.textureBurnt = Texture.trygetTexture(texName + "Overdone");
        String burntSuffix = "Overdone.png";
        if (this.textureBurnt == null) {
            this.textureBurnt = Texture.trygetTexture(texName + "Burnt");
            if (this.textureBurnt != null) {
                burntSuffix = "Burnt.png";
            }
        }
        if (this.textureBurnt == null) {
            this.textureBurnt = Texture.trygetTexture(texName + "_Burnt");
            if (this.textureBurnt != null) {
                burntSuffix = "_Burnt.png";
            }
        }
        Texture.warnFailFindTexture = true;
        if (this.texturerotten == null) {
            this.texturerotten = this.texture;
        }
        if (this.textureCooked == null) {
            this.textureCooked = this.texture;
        }
        if (this.textureBurnt == null) {
            this.textureBurnt = this.texture;
        }
        this.worldTextureCooked = this.worldTexture.replace(".png", cookedSuffix);
        this.worldTextureOverdone = this.worldTexture.replace(".png", burntSuffix);
        this.worldTextureRotten = this.worldTexture.replace(".png", rottenSuffix);
        this.itemType = ItemType.FOOD;
        this.lastUpdateTime = System.currentTimeMillis();
    }

    public Food(String module, String name, String itemType, Item item) {
        super(module, name, itemType, item);
        String texName = item.itemName;
        Texture.warnFailFindTexture = false;
        this.texture = item.normalTexture;
        if (item.specialTextures.isEmpty()) {
            boolean bl = false;
        }
        if (!item.specialTextures.isEmpty()) {
            this.texturerotten = item.specialTextures.get(0);
        }
        if (item.specialTextures.size() > 1) {
            this.textureCooked = item.specialTextures.get(1);
        }
        if (item.specialTextures.size() > 2) {
            this.textureBurnt = item.specialTextures.get(2);
        }
        Texture.warnFailFindTexture = true;
        if (this.texturerotten == null) {
            this.texturerotten = this.texture;
        }
        if (this.textureCooked == null) {
            this.textureCooked = this.texture;
        }
        if (this.textureBurnt == null) {
            this.textureBurnt = this.texture;
        }
        if (!item.specialWorldTextureNames.isEmpty()) {
            this.worldTextureRotten = item.specialWorldTextureNames.get(0);
        }
        if (item.specialWorldTextureNames.size() > 1) {
            this.worldTextureCooked = item.specialWorldTextureNames.get(1);
        }
        if (item.specialWorldTextureNames.size() > 2) {
            this.worldTextureOverdone = item.specialWorldTextureNames.get(2);
        }
        this.itemType = ItemType.FOOD;
        this.lastUpdateTime = System.currentTimeMillis();
    }

    @Override
    public boolean IsFood() {
        return true;
    }

    public boolean checkEggHatch(IsoHutch hutch) {
        if (!this.isFertilized()) {
            return false;
        }
        int x = 0;
        int y = 0;
        int z = 0;
        if (!StringUtils.isNullOrEmpty(this.animalHatch)) {
            if (this.lastEggTimeCheck != (long)GameTime.getInstance().getHour()) {
                ++this.fertilizedTime;
            }
            if (this.fertilizedTime >= this.timeToHatch) {
                this.fertilizedTime = 0;
                boolean inInv = false;
                boolean inCar = false;
                if (hutch == null) {
                    if (this.getWorldItem() != null) {
                        x = this.getWorldItem().getXi();
                        y = this.getWorldItem().getYi();
                        z = this.getWorldItem().getZi();
                    }
                    if (this.getContainer() != null && !"floor".equalsIgnoreCase(this.getContainer().getType())) {
                        inInv = true;
                    }
                    if (this.getContainer() != null && this.getContainer().getParent() instanceof BaseVehicle && ((BaseVehicle)this.getContainer().getParent()).getAnimalTrailerSize() > 0.0f) {
                        x = this.getContainer().getParent().getXi();
                        y = this.getContainer().getParent().getYi();
                        z = this.getContainer().getParent().getZi();
                        inCar = true;
                    }
                    if (this.getContainer() != null && this.getContainer().getParent() != null && this.getContainer().getParent() instanceof IsoPlayer) {
                        x = this.getContainer().getParent().getXi();
                        y = this.getContainer().getParent().getYi();
                        z = this.getContainer().getParent().getZi();
                        inInv = true;
                    }
                    if (x == 0 && y == 0 && !inInv) {
                        return false;
                    }
                }
                AnimalDefinitions def = AnimalDefinitions.getDef(this.animalHatch);
                IsoAnimal baby = new IsoAnimal(IsoWorld.instance.getCell(), x, y, z, this.animalHatch, def.getBreedByName(this.animalHatchBreed));
                baby.fullGenome = this.eggGenome;
                baby.attachBackToMother = this.motherId;
                AnimalGene.checkGeneticDisorder(baby);
                if (inInv) {
                    AnimalInventoryItem animalInv = (AnimalInventoryItem)InventoryItemFactory.CreateItem("Base.Animal");
                    animalInv.setAnimal(baby);
                    baby.removeFromWorld();
                    baby.removeFromSquare();
                    this.getContainer().AddItem(animalInv);
                    this.getContainer().Remove(this);
                } else if (hutch != null) {
                    hutch.addAnimalInside(baby);
                } else if (inCar) {
                    ((BaseVehicle)this.getContainer().getParent()).addAnimalInTrailer(baby);
                    this.getContainer().Remove(this);
                } else {
                    baby.addToWorld();
                    this.getWorldItem().removeFromWorld();
                    this.getWorldItem().removeFromSquare();
                }
                return true;
            }
            this.lastEggTimeCheck = GameTime.getInstance().getHour();
        }
        return false;
    }

    @Override
    public void update() {
        if (this.hasTag(ItemTag.ALREADY_COOKED)) {
            this.setCooked(true);
        }
        this.updateTemperature();
        this.checkEggHatch(null);
        ItemContainer outermostContainer = this.getOutermostContainer();
        if (outermostContainer != null) {
            if (this.isCookable && !this.isFrozen()) {
                if (this.heat > 1.6f) {
                    this.setFertilized(false);
                    int currentCookMinute = GameTime.getInstance().getMinutes();
                    if (currentCookMinute != this.lastCookMinute) {
                        if (GameServer.server) {
                            GameServer.sendItemStats(this);
                        }
                        this.lastCookMinute = currentCookMinute;
                        float dt = this.heat / 1.5f;
                        if (outermostContainer.getTemprature() <= 1.6f) {
                            dt *= 0.05f;
                        }
                        this.cookingTime += dt;
                        if (this.shouldPlayCookingSound()) {
                            ItemSoundManager.addItem(this);
                        }
                        if (this.isTainted && this.cookingTime > Math.min(this.minutesToCook, 10.0f)) {
                            this.isTainted = false;
                        }
                        if (!this.isCooked() && !this.burnt && (this.cookingTime > this.minutesToCook || this.cookingTime > this.minutesToBurn)) {
                            if (this.getReplaceOnCooked() != null && !this.isRotten()) {
                                for (int i = 0; i < this.getReplaceOnCooked().size(); ++i) {
                                    Food food;
                                    InventoryItem newFood = this.container.AddItem(this.getReplaceOnCooked().get(i));
                                    if (newFood == null) continue;
                                    newFood.copyConditionStatesFrom(this);
                                    if (!(newFood instanceof Food) || this instanceof Food) {
                                        // empty if block
                                    }
                                    if (newFood instanceof Food && (food = (Food)newFood).isBadInMicrowave() && this.container.isMicrowave()) {
                                        newFood.setUnhappyChange(5.0f);
                                        newFood.setBoredomChange(5.0f);
                                        food.cookedInMicrowave = true;
                                    }
                                    if (!GameServer.server) continue;
                                    GameServer.sendAddItemToContainer(this.container, newFood);
                                }
                                if (GameServer.server) {
                                    GameServer.sendRemoveItemFromContainer(this.container, this);
                                }
                                this.container.Remove(this);
                                IsoWorld.instance.currentCell.addToProcessItemsRemove(this);
                                return;
                            }
                            this.setCooked(true);
                            if (this.getScriptItem().removeUnhappinessWhenCooked) {
                                this.setUnhappyChange(0.0f);
                            }
                            if (this.type.equals("RicePot") || this.type.equals("PastaPot") || this.type.equals("RicePan") || this.type.equals("PastaPan") || this.type.equals("WaterPotRice") || this.type.equals("WaterPotPasta") || this.type.equals("WaterSaucepanRice") || this.type.equals("WaterSaucepanPasta") || this.type.equals("RiceBowl") || this.type.equals("PastaBowl")) {
                                this.setAge(0.0f);
                                this.setOffAge(1);
                                this.setOffAgeMax(2);
                            }
                            if (this.isRemoveNegativeEffectOnCooked()) {
                                if (this.thirstChange > 0.0f) {
                                    this.setThirstChange(0.0f);
                                }
                                if (this.unhappyChange > 0.0f) {
                                    this.setUnhappyChange(0.0f);
                                }
                                if (this.boredomChange > 0.0f) {
                                    this.setBoredomChange(0.0f);
                                }
                            }
                            if (!StringUtils.isNullOrEmpty(this.getOnCooked())) {
                                if (this.getOnCooked().contains(".")) {
                                    String[] split = this.getOnCooked().split("\\.");
                                    LuaManager.caller.protectedCallVoid(LuaManager.thread, ((KahluaTableImpl)LuaManager.env.rawget(split[0])).rawget(split[1]), this);
                                } else {
                                    LuaManager.caller.protectedCallVoid(LuaManager.thread, LuaManager.env.rawget(this.getOnCooked()), this);
                                }
                            }
                            if (this.isBadInMicrowave() && this.container.isMicrowave()) {
                                this.setUnhappyChange(5.0f);
                                this.setBoredomChange(5.0f);
                                this.cookedInMicrowave = true;
                            }
                            if (!(this.chef == null || this.chef.isEmpty() || this.hasTag(ItemTag.NO_COOKING_XP) || this.isRotten())) {
                                if (GameServer.server) {
                                    IsoPlayer player = GameServer.getPlayerByUserNameForCommand(this.chef);
                                    GameServer.addXp(player, PerkFactory.Perks.Cooking, 10.0f);
                                } else if (!GameClient.client) {
                                    for (int playerIndex = 0; playerIndex < IsoPlayer.numPlayers; ++playerIndex) {
                                        IsoPlayer player = IsoPlayer.players[playerIndex];
                                        if (player == null || player.isDead() || !this.chef.equals(player.getFullName())) continue;
                                        player.getXp().AddXP(PerkFactory.Perks.Cooking, 10.0f);
                                        break;
                                    }
                                }
                            }
                        }
                        if (this.cookingTime > this.minutesToBurn) {
                            this.burnt = true;
                            this.setCooked(false);
                        }
                        if (GameServer.server) {
                            GameServer.sendItemStats(this);
                        }
                        if (this.container != null && this.container.getParent() != null && this.container.getParent().hasGridPower() && this.burnt && this.cookingTime >= 50.0f && this.cookingTime >= this.minutesToCook * 2.0f + this.minutesToBurn / 2.0f && Rand.Next(Rand.AdjustForFramerate(200)) == 0) {
                            boolean isCampfire;
                            boolean bl = isCampfire = this.container != null && this.container.getParent() != null && this.container.getParent().getName() != null && this.container.getParent().getName().equals("Campfire");
                            if (!isCampfire && this.container != null && this.container.getParent() != null && this.container.getParent() instanceof IsoFireplace) {
                                isCampfire = true;
                            }
                            if (this.container != null && this.container.sourceGrid != null && !isCampfire) {
                                IsoFireManager.StartFire(this.container.sourceGrid.getCell(), this.container.sourceGrid, true, 500000);
                                this.isCookable = false;
                            }
                        }
                    }
                }
            } else {
                int currentCookMinute;
                if (GameServer.server) {
                    this.updateAge(false);
                }
                if (this.isTainted && this.heat > 1.6f && !this.isFrozen() && (currentCookMinute = GameTime.getInstance().getMinutes()) != this.lastCookMinute) {
                    this.lastCookMinute = currentCookMinute;
                    float dt = 1.0f;
                    if (outermostContainer.getTemprature() <= 1.6f) {
                        dt = (float)((double)dt * 0.2);
                    }
                    this.cookingTime += dt;
                    if (this.cookingTime > 10.0f) {
                        this.isTainted = false;
                    }
                    if (GameServer.server) {
                        GameServer.sendItemStats(this);
                    }
                }
            }
        }
        this.updateRotting(outermostContainer);
        this.lastUpdateTime = System.currentTimeMillis();
    }

    @Override
    public String getSoundLimiterGroupID() {
        if (this.soundLimiterGroupID_Burning == null) {
            this.soundLimiterGroupID_Burning = this.getCookingSound() + "Burning";
        }
        boolean burning = this.cookingTime > this.minutesToCook;
        return burning ? this.soundLimiterGroupID_Burning : this.getCookingSound();
    }

    @Override
    public void registerWithSoundLimiter(SoundInstanceLimiter limiter) {
        if (this.shouldPlayCookingSound()) {
            ItemContainer outermostContainer = this.getOutermostContainer();
            IsoGridSquare square = outermostContainer.getParent().getSquare();
            limiter.register(this, this.getSoundLimiterGroupID(), (float)square.getX() + 0.5f, (float)square.getY() + 0.5f, square.getZ());
        }
    }

    @Override
    public void updateSound(BaseSoundEmitter emitter, SoundLimiterParams params) {
        params.putIfAbsent((short)0, 0L);
        params.putIfAbsent((short)1, -1);
        this.cookingSound = (Long)params.get((short)0);
        this.cookingParameter = (Integer)params.get((short)1);
        if (this.shouldPlayCookingSound()) {
            ItemContainer outermostContainer = this.getOutermostContainer();
            IsoGridSquare square = outermostContainer.getParent().getSquare();
            emitter.setPos((float)square.getX() + 0.5f, (float)square.getY() + 0.5f, square.getZ());
            if (emitter.isPlaying(this.cookingSound)) {
                this.setCookingParameter(emitter);
                params.put((short)1, this.cookingParameter);
                return;
            }
            this.cookingSound = emitter.playSoundImpl(this.getCookingSound(), (IsoObject)null);
            this.setCookingParameter(emitter);
            params.put((short)0, this.cookingSound);
            params.put((short)1, this.cookingParameter);
        } else {
            emitter.stopOrTriggerSound(this.cookingSound);
            this.cookingSound = 0L;
            this.cookingParameter = -1;
            ItemSoundManager.removeItem(this);
        }
    }

    private boolean shouldPlayCookingSound() {
        if (GameServer.server) {
            return false;
        }
        if (StringUtils.isNullOrWhitespace(this.getCookingSound())) {
            return false;
        }
        ItemContainer container = this.getOutermostContainer();
        if (container == null || container.getParent() == null || container.getParent().getObjectIndex() == -1 || container.getTemprature() <= 1.6f) {
            return false;
        }
        return this.isCookable() && !this.isFrozen() && this.getHeat() > 1.6f;
    }

    private void setCookingParameter(BaseSoundEmitter emitter) {
        int cookingParam;
        boolean burning = this.cookingTime > this.minutesToCook;
        int n = cookingParam = burning ? 1 : 0;
        if (cookingParam != this.cookingParameter) {
            this.cookingParameter = cookingParam;
            emitter.setParameterValue(this.cookingSound, FMODManager.instance.getParameterDescription("CookingState"), this.cookingParameter);
        }
    }

    public void updateClientCookingSounds() {
        if (!GameClient.client || !this.shouldPlayCookingSound()) {
            return;
        }
        ItemSoundManager.addItem(this);
    }

    private void updateTemperature() {
        ItemContainer outermostContainer;
        float temp;
        float dt;
        this.temperatureTimeAccum += GameTime.getInstance().getThirtyFPSMultiplier();
        if (this.temperatureTimeAccum < 10.0f) {
            return;
        }
        float f = dt = GameServer.server ? (float)(System.currentTimeMillis() - this.lastUpdateTime) / 50.0f : 1.0f;
        if (this.isFertilized() && this.heat < 0.5f) {
            this.setFertilized(false);
        }
        float f2 = temp = (outermostContainer = this.getOutermostContainer()) == null ? 1.0f : outermostContainer.getTemprature();
        if (this.heat > temp) {
            this.heat -= 0.001f * dt * this.temperatureTimeAccum;
            if (this.heat < Math.max(0.2f, temp)) {
                this.heat = Math.max(0.2f, temp);
            }
        }
        if (this.heat < temp) {
            this.heat += temp / 1000.0f * dt * this.temperatureTimeAccum;
            if (this.heat > Math.min(3.0f, temp)) {
                this.heat = Math.min(3.0f, temp);
            }
        }
        this.temperatureTimeAccum = 0.0f;
    }

    /*
     * Enabled aggressive block sorting
     */
    private void updateRotting(ItemContainer outermostContainer) {
        if ((double)this.offAgeMax == 1.0E9) {
            return;
        }
        if (GameClient.client) {
            return;
        }
        if (this.replaceOnRotten != null && !this.replaceOnRotten.isEmpty()) {
            this.updateAge();
            if (this.isRotten()) {
                InventoryItem newItem = InventoryItemFactory.CreateItem(this.getModule() + "." + this.replaceOnRotten, this);
                if (newItem == null) {
                    DebugLog.General.warn("ReplaceOnRotten = " + this.replaceOnRotten + " doesn't exist for " + this.getFullType());
                    this.destroyThisItem();
                    return;
                }
                newItem.setAge(this.getAge());
                newItem.copyConditionStatesFrom(this);
                IsoWorldInventoryObject worldInvObj = this.getWorldItem();
                if (worldInvObj != null && worldInvObj.getSquare() != null) {
                    IsoGridSquare square = worldInvObj.getSquare();
                    if (!GameServer.server) {
                        worldInvObj.item = newItem;
                        newItem.setWorldItem(worldInvObj);
                        worldInvObj.updateSprite();
                        IsoWorld.instance.currentCell.addToProcessItemsRemove(this);
                        LuaEventManager.triggerEvent("OnContainerUpdate");
                        return;
                    }
                    square.AddWorldInventoryItem(newItem, worldInvObj.xoff, worldInvObj.yoff, worldInvObj.zoff, true);
                } else if (this.container != null) {
                    this.container.AddItem(newItem);
                    if (GameServer.server) {
                        GameServer.sendAddItemToContainer(this.container, newItem);
                    }
                }
                this.destroyThisItem();
                return;
            }
        }
        if (SandboxOptions.instance.daysForRottenFoodRemoval.getValue() >= 0) {
            if (outermostContainer != null && outermostContainer.parent instanceof IsoCompost) {
                return;
            }
            this.updateAge(false);
            if (this.getAge() > (float)(this.getOffAgeMax() + SandboxOptions.instance.daysForRottenFoodRemoval.getValue())) {
                this.destroyThisItem();
            }
        }
    }

    private float getFridgeFactor() {
        return switch (SandboxOptions.instance.fridgeFactor.getValue()) {
            case 1 -> 0.4f;
            case 2 -> 0.3f;
            case 4 -> 0.1f;
            case 5 -> 0.03f;
            case 6 -> 0.0f;
            default -> 0.2f;
        };
    }

    private float getFoodRotSpeed() {
        return switch (SandboxOptions.instance.foodRotSpeed.getValue()) {
            case 1 -> 1.7f;
            case 2 -> 1.4f;
            case 4 -> 0.7f;
            case 5 -> 0.4f;
            default -> 1.0f;
        };
    }

    @Override
    public void updateAge() {
        this.updateAge(updateAgeRate.Check());
    }

    public void updateAge(boolean bSendItemStats) {
        float elapsedHours = (float)GameTime.getInstance().getWorldAgeHours();
        ItemContainer outermostContainer = this.getOutermostContainer();
        this.updateFreezing(outermostContainer, elapsedHours);
        if (this.lastAged < 0.0f) {
            this.lastAged = elapsedHours;
        } else if (this.lastAged > elapsedHours) {
            this.lastAged = elapsedHours;
        }
        if (elapsedHours > this.lastAged) {
            boolean rotten2;
            double ageIncrease = elapsedHours - this.lastAged;
            if (outermostContainer != null && this.heat != outermostContainer.getTemprature()) {
                if (ageIncrease < 0.3333333432674408) {
                    if (!IsoWorld.instance.getCell().getProcessItems().contains(this)) {
                        this.heat = GameTime.instance.Lerp(this.heat, outermostContainer.getTemprature(), (float)ageIncrease / 0.33333334f);
                        IsoWorld.instance.getCell().addToProcessItems(this);
                    }
                } else {
                    this.heat = outermostContainer.getTemprature();
                }
            }
            if (this.isFrozen()) {
                ageIncrease *= 0.0;
            } else if (outermostContainer != null && (outermostContainer.getType().equals("fridge") || outermostContainer.getType().equals("freezer"))) {
                if (outermostContainer.getSourceGrid() != null && outermostContainer.getSourceGrid().haveElectricity()) {
                    ageIncrease *= (double)this.getFridgeFactor();
                } else if (SandboxOptions.instance.getElecShutModifier() > -1 && this.lastAged < (float)(SandboxOptions.instance.getElecShutModifier() * 24)) {
                    float hoursWithElectricity = Math.min((float)(SandboxOptions.instance.getElecShutModifier() * 24), elapsedHours);
                    ageIncrease = (hoursWithElectricity - this.lastAged) * this.getFridgeFactor();
                    if (elapsedHours > (float)(SandboxOptions.instance.getElecShutModifier() * 24)) {
                        ageIncrease += (double)(elapsedHours - (float)(SandboxOptions.instance.getElecShutModifier() * 24));
                    }
                }
            }
            boolean fresh = !this.burnt && this.offAge < 1000000000 && this.age < (float)this.offAge;
            boolean rotten = !this.burnt && this.offAgeMax < 1000000000 && this.age >= (float)this.offAgeMax;
            this.age = (float)((double)this.age + ageIncrease * (double)this.getFoodRotSpeed() / 24.0);
            this.lastAged = elapsedHours;
            boolean fresh2 = !this.burnt && this.offAge < 1000000000 && this.age < (float)this.offAge;
            boolean bl = rotten2 = !this.burnt && this.offAgeMax < 1000000000 && this.age >= (float)this.offAgeMax;
            if (!(GameServer.server || fresh == fresh2 && rotten == rotten2)) {
                LuaEventManager.triggerEvent("OnContainerUpdate", this);
            }
            if (bSendItemStats && GameServer.server) {
                GameServer.sendItemStats(this);
            }
        }
    }

    @Override
    public void setAutoAge() {
        int electShutModifier;
        ItemContainer outermostContainer = this.getOutermostContainer();
        float worldAgeDays = (float)GameTime.getInstance().getWorldAgeHours() / 24.0f;
        float ageIncrease = worldAgeDays += (float)((SandboxOptions.instance.timeSinceApo.getValue() - 1) * 30);
        boolean isFridge = false;
        boolean isFreezer = false;
        if (outermostContainer != null) {
            isFridge = outermostContainer.getType().equals("fridge");
            isFreezer = outermostContainer.getType().equals("freezer");
            if (outermostContainer.getParent() != null && outermostContainer.getParent().getSprite() != null) {
                isFridge = outermostContainer.getParent().getProperties().has(IsoPropertyType.IS_FRIDGE);
            }
            if (isFreezer) {
                isFridge = false;
            }
        }
        if (outermostContainer != null && (isFridge || isFreezer) && (electShutModifier = SandboxOptions.instance.elecShutModifier.getValue()) > -1) {
            float daysWithElectricity = Math.min((float)electShutModifier, worldAgeDays);
            if (isFridge || !this.canBeFrozen()) {
                ageIncrease -= daysWithElectricity;
                ageIncrease += daysWithElectricity * this.getFridgeFactor();
            } else {
                float daysFrozen = daysWithElectricity;
                float freezingTime = 100.0f;
                if (worldAgeDays > daysWithElectricity) {
                    float elapsedHours = (worldAgeDays - daysWithElectricity) * 24.0f;
                    float turnToHour = 1440.0f / GameTime.getInstance().getMinutesPerDay() * 60.0f * 5.0f;
                    float heatingModifier = 0.0095999995f;
                    if ((freezingTime -= 0.0095999995f * turnToHour * elapsedHours) > 0.0f) {
                        daysFrozen += elapsedHours / 24.0f;
                    } else {
                        float thawHours = 100.0f / (0.0095999995f * turnToHour);
                        daysFrozen += thawHours / 24.0f;
                        freezingTime = 0.0f;
                    }
                }
                ageIncrease -= daysFrozen;
                ageIncrease += daysFrozen * 0.0f;
                this.setFreezingTime(freezingTime);
            }
        }
        this.age = ageIncrease * this.getFoodRotSpeed();
        this.lastFrozenUpdate = this.lastAged = (float)GameTime.getInstance().getWorldAgeHours();
        if (outermostContainer != null) {
            this.setHeat(outermostContainer.getTemprature());
        }
    }

    private void updateFreezing(ItemContainer outermostContainer, float worldAgeHours) {
        if (this.lastFrozenUpdate < 0.0f) {
            this.lastFrozenUpdate = worldAgeHours;
        } else if (this.lastFrozenUpdate > worldAgeHours) {
            this.lastFrozenUpdate = worldAgeHours;
        }
        if (worldAgeHours > this.lastFrozenUpdate) {
            float elapsedHours = worldAgeHours - this.lastFrozenUpdate;
            float hoursToFreeze = 4.0f;
            float hoursToThaw = 1.5f;
            if (this.isFreezing()) {
                this.setFertilized(false);
                this.setFreezingTime(this.getFreezingTime() + elapsedHours / 4.0f * 100.0f);
            }
            if (this.isThawing()) {
                float localHoursToThaw = 1.5f;
                if (outermostContainer != null && "fridge".equals(outermostContainer.getType()) && outermostContainer.isPowered()) {
                    localHoursToThaw *= 2.0f;
                }
                if (outermostContainer != null && outermostContainer.getTemprature() > 1.0f) {
                    localHoursToThaw /= 6.0f;
                }
                this.setFreezingTime(this.getFreezingTime() - elapsedHours / localHoursToThaw * 100.0f);
            }
            this.lastFrozenUpdate = worldAgeHours;
        }
    }

    @Override
    public float getActualWeight() {
        if (this.haveExtraItems()) {
            String fullType;
            Item emptyItem;
            float hungChange = this.getHungChange();
            float baseHunger = this.getBaseHunger();
            float usedDelta = baseHunger == 0.0f ? 0.0f : hungChange / baseHunger;
            float emptyItemWeight = 0.0f;
            if (this.getReplaceOnUse() != null && (emptyItem = ScriptManager.instance.getItem(fullType = this.getReplaceOnUseFullType())) != null) {
                emptyItemWeight = emptyItem.getActualWeight();
            }
            float actualWeight = super.getActualWeight() + this.getExtraItemsWeight();
            return (actualWeight - emptyItemWeight) * usedDelta + emptyItemWeight;
        }
        if (this.getReplaceOnUse() != null && !this.isCustomWeight()) {
            String fullType = this.getReplaceOnUseFullType();
            Item emptyItem = ScriptManager.instance.getItem(fullType);
            if (emptyItem != null) {
                float usedDelta = 1.0f;
                if (this.getScriptItem().getHungerChange() < 0.0f) {
                    usedDelta = this.getHungChange() * 100.0f / this.getScriptItem().getHungerChange();
                } else if (this.getScriptItem().getThirstChange() < 0.0f) {
                    usedDelta = this.getThirstChange() * 100.0f / this.getScriptItem().getThirstChange();
                }
                return (this.getScriptItem().getActualWeight() - emptyItem.getActualWeight()) * usedDelta + emptyItem.getActualWeight();
            }
        } else if (!this.isCustomWeight()) {
            float usedDelta = 1.0f;
            if (this.getScriptItem().getHungerChange() < 0.0f) {
                usedDelta = this.getHungChange() * 100.0f / this.getScriptItem().getHungerChange();
            } else if (this.getScriptItem().getThirstChange() < 0.0f) {
                usedDelta = this.getThirstChange() * 100.0f / this.getScriptItem().getThirstChange();
            }
            return this.getScriptItem().getActualWeight() * usedDelta;
        }
        return super.getActualWeight();
    }

    @Override
    public float getWeight() {
        if (this.getReplaceOnUse() != null) {
            return this.getActualWeight();
        }
        return super.getWeight();
    }

    @Override
    public void save(ByteBuffer output, boolean net) throws IOException {
        super.save(output, net);
        output.putFloat(this.age);
        output.putFloat(this.lastAged);
        BitHeaderWrite header = BitHeader.allocWrite(BitHeader.HeaderSize.Byte, output);
        if (this.calories != 0.0f || this.proteins != 0.0f || this.lipids != 0.0f || this.carbohydrates != 0.0f) {
            header.addFlags(1);
            output.putFloat(this.calories);
            output.putFloat(this.proteins);
            output.putFloat(this.lipids);
            output.putFloat(this.carbohydrates);
        }
        if (this.hungChange != 0.0f) {
            header.addFlags(2);
            output.putFloat(this.hungChange);
        }
        if (this.baseHunger != 0.0f) {
            header.addFlags(4);
            output.putFloat(this.baseHunger);
        }
        if (this.unhappyChange != 0.0f) {
            header.addFlags(8);
            output.putFloat(this.unhappyChange);
        }
        if (this.boredomChange != 0.0f) {
            header.addFlags(16);
            output.putFloat(this.boredomChange);
        }
        if (this.thirstChange != 0.0f) {
            header.addFlags(32);
            output.putFloat(this.thirstChange);
        }
        BitHeaderWrite bits = BitHeader.allocWrite(BitHeader.HeaderSize.Integer, output);
        if (this.heat != 1.0f) {
            bits.addFlags(1);
            output.putFloat(this.heat);
        }
        if (this.lastCookMinute != 0) {
            bits.addFlags(2);
            output.putInt(this.lastCookMinute);
        }
        if (this.cookingTime != 0.0f) {
            bits.addFlags(4);
            output.putFloat(this.cookingTime);
        }
        if (this.cooked) {
            bits.addFlags(8);
        }
        if (this.burnt) {
            bits.addFlags(16);
        }
        if (this.isCookable) {
            bits.addFlags(32);
        }
        if (this.dangerousUncooked) {
            bits.addFlags(64);
        }
        if (this.poisonDetectionLevel != -1) {
            bits.addFlags(128);
            output.put((byte)this.poisonDetectionLevel);
        }
        if (this.spices != null) {
            bits.addFlags(256);
            output.put((byte)this.spices.size());
            for (String spice : this.spices) {
                GameWindow.WriteString(output, spice);
            }
        }
        if (this.poisonPower != 0) {
            bits.addFlags(512);
            output.put((byte)this.poisonPower);
        }
        if (this.chef != null) {
            bits.addFlags(1024);
            GameWindow.WriteString(output, this.chef);
        }
        if ((double)this.offAge != 1.0E9) {
            bits.addFlags(2048);
            output.putInt(this.offAge);
        }
        if ((double)this.offAgeMax != 1.0E9) {
            bits.addFlags(4096);
            output.putInt(this.offAgeMax);
        }
        if (this.painReduction != 0.0f) {
            bits.addFlags(8192);
            output.putFloat(this.painReduction);
        }
        if (this.fluReduction != 0) {
            bits.addFlags(16384);
            output.putInt(this.fluReduction);
        }
        if (this.foodSicknessChange != 0) {
            bits.addFlags(32768);
            output.putInt(this.foodSicknessChange);
        }
        if (this.poison) {
            bits.addFlags(65536);
        }
        if (this.useForPoison != 0) {
            bits.addFlags(131072);
            output.putShort((short)this.useForPoison);
        }
        if (this.freezingTime != 0.0f) {
            bits.addFlags(262144);
            output.putFloat(this.freezingTime);
        }
        if (this.isFrozen()) {
            bits.addFlags(524288);
        }
        if (this.lastFrozenUpdate != 0.0f) {
            bits.addFlags(0x100000);
            output.putFloat(this.lastFrozenUpdate);
        }
        if (this.rottenTime != 0.0f) {
            bits.addFlags(0x200000);
            output.putFloat(this.rottenTime);
        }
        if (this.compostTime != 0.0f) {
            bits.addFlags(0x400000);
            output.putFloat(this.compostTime);
        }
        if (this.cookedInMicrowave) {
            bits.addFlags(0x800000);
        }
        if (this.fatigueChange != 0.0f) {
            bits.addFlags(0x1000000);
            output.putFloat(this.fatigueChange);
        }
        if (this.endChange != 0.0f) {
            bits.addFlags(0x2000000);
            output.putFloat(this.endChange);
        }
        if (this.milkQty > 0) {
            bits.addFlags(0x4000000);
            output.putInt(this.milkQty);
            GameWindow.WriteString(output, this.milkType);
        }
        if (this.isFertilized()) {
            bits.addFlags(0x8000000);
            output.putInt(this.timeToHatch);
            output.putInt(this.fertilizedTime);
            GameWindow.WriteString(output, this.animalHatch);
            GameWindow.WriteString(output, this.animalHatchBreed);
            ArrayList<String> genes = new ArrayList<String>(this.eggGenome.keySet());
            output.putInt(this.eggGenome.size());
            for (int i = 0; i < genes.size(); ++i) {
                String gene = genes.get(i);
                this.eggGenome.get(gene).save(output, false);
            }
            output.putInt(this.motherId);
        }
        if (this.stressChange != 0.0f) {
            bits.addFlags(0x10000000);
            output.putFloat(this.stressChange);
        }
        if (!bits.equals(0)) {
            header.addFlags(64);
            bits.write();
        } else {
            output.position(bits.getStartPosition());
        }
        header.write();
        header.release();
        bits.release();
    }

    @Override
    public void load(ByteBuffer input, int worldVersion) throws IOException {
        super.load(input, worldVersion);
        this.calories = 0.0f;
        this.proteins = 0.0f;
        this.lipids = 0.0f;
        this.carbohydrates = 0.0f;
        this.hungChange = 0.0f;
        this.baseHunger = 0.0f;
        this.unhappyChange = 0.0f;
        this.boredomChange = 0.0f;
        this.thirstChange = 0.0f;
        this.heat = 1.0f;
        this.lastCookMinute = 0;
        this.cookingTime = 0.0f;
        this.cooked = false;
        this.burnt = false;
        this.isCookable = false;
        this.dangerousUncooked = false;
        this.poisonDetectionLevel = -1;
        this.spices = null;
        this.poisonPower = 0;
        this.chef = null;
        this.offAge = 1000000000;
        this.offAgeMax = 1000000000;
        this.painReduction = 0.0f;
        this.fluReduction = 0;
        this.foodSicknessChange = 0;
        this.poison = false;
        this.useForPoison = 0;
        this.freezingTime = 0.0f;
        this.frozen = false;
        this.lastFrozenUpdate = 0.0f;
        this.rottenTime = 0.0f;
        this.compostTime = 0.0f;
        this.cookedInMicrowave = false;
        this.fatigueChange = 0.0f;
        this.stressChange = 0.0f;
        this.endChange = 0.0f;
        this.age = input.getFloat();
        this.lastAged = input.getFloat();
        BitHeaderRead header = BitHeader.allocRead(BitHeader.HeaderSize.Byte, input);
        if (!header.equals(0)) {
            if (header.hasFlags(1)) {
                this.calories = input.getFloat();
                this.proteins = input.getFloat();
                this.lipids = input.getFloat();
                this.carbohydrates = input.getFloat();
            }
            if (header.hasFlags(2)) {
                this.hungChange = input.getFloat();
            }
            if (header.hasFlags(4)) {
                this.baseHunger = input.getFloat();
            }
            if (header.hasFlags(8)) {
                this.unhappyChange = input.getFloat();
            }
            if (header.hasFlags(16)) {
                this.boredomChange = input.getFloat();
            }
            if (header.hasFlags(32)) {
                this.thirstChange = input.getFloat();
            }
            if (header.hasFlags(64)) {
                int i;
                BitHeaderRead bits = BitHeader.allocRead(BitHeader.HeaderSize.Integer, input);
                if (bits.hasFlags(1)) {
                    this.heat = input.getFloat();
                }
                if (bits.hasFlags(2)) {
                    this.lastCookMinute = input.getInt();
                }
                if (bits.hasFlags(4)) {
                    this.cookingTime = input.getFloat();
                }
                this.cooked = bits.hasFlags(8);
                this.burnt = bits.hasFlags(16);
                this.isCookable = bits.hasFlags(32);
                this.dangerousUncooked = bits.hasFlags(64);
                if (bits.hasFlags(128)) {
                    this.poisonDetectionLevel = input.get();
                }
                if (bits.hasFlags(256)) {
                    this.spices = new ArrayList();
                    int size = input.get();
                    for (i = 0; i < size; ++i) {
                        String fullType = GameWindow.ReadString(input);
                        this.spices.add(fullType);
                    }
                }
                if (bits.hasFlags(512)) {
                    this.poisonPower = input.get();
                }
                if (bits.hasFlags(1024)) {
                    this.chef = GameWindow.ReadString(input);
                }
                if (bits.hasFlags(2048)) {
                    this.offAge = input.getInt();
                }
                if (bits.hasFlags(4096)) {
                    this.offAgeMax = input.getInt();
                }
                if (bits.hasFlags(8192)) {
                    this.painReduction = input.getFloat();
                }
                if (bits.hasFlags(16384)) {
                    this.fluReduction = input.getInt();
                }
                if (bits.hasFlags(32768)) {
                    this.foodSicknessChange = input.getInt();
                }
                this.poison = bits.hasFlags(65536);
                if (bits.hasFlags(131072)) {
                    this.useForPoison = input.getShort();
                }
                if (bits.hasFlags(262144)) {
                    this.freezingTime = input.getFloat();
                }
                this.setFrozen(bits.hasFlags(524288));
                if (bits.hasFlags(0x100000)) {
                    this.lastFrozenUpdate = input.getFloat();
                }
                if (bits.hasFlags(0x200000)) {
                    this.rottenTime = input.getFloat();
                }
                if (bits.hasFlags(0x400000)) {
                    this.compostTime = input.getFloat();
                }
                this.cookedInMicrowave = bits.hasFlags(0x800000);
                if (bits.hasFlags(0x1000000)) {
                    this.fatigueChange = input.getFloat();
                }
                if (bits.hasFlags(0x2000000)) {
                    this.endChange = input.getFloat();
                }
                if (bits.hasFlags(0x4000000)) {
                    this.milkQty = input.getInt();
                    this.milkType = GameWindow.ReadString(input);
                }
                if (bits.hasFlags(0x8000000)) {
                    this.timeToHatch = input.getInt();
                    this.fertilizedTime = input.getInt();
                    this.animalHatch = GameWindow.ReadString(input);
                    this.animalHatchBreed = GameWindow.ReadString(input);
                    int genomeSize = input.getInt();
                    this.eggGenome = new HashMap();
                    for (i = 0; i < genomeSize; ++i) {
                        AnimalGene gene = new AnimalGene();
                        gene.load(input, worldVersion, false);
                        this.eggGenome.put(gene.name, gene);
                    }
                    this.motherId = input.getInt();
                    this.setFertilized(true);
                }
                if (bits.hasFlags(0x10000000)) {
                    this.stressChange = input.getFloat();
                }
                bits.release();
            }
        }
        header.release();
        if (GameServer.server && this.lastAged == -1.0f) {
            this.lastAged = (float)GameTime.getInstance().getWorldAgeHours();
        }
    }

    @Override
    public boolean finishupdate() {
        if (this.container == null && (this.getWorldItem() == null || this.getWorldItem().getSquare() == null)) {
            return true;
        }
        if (this.isCookable) {
            return false;
        }
        if (this.container != null && (this.heat != this.container.getTemprature() || this.container.isTemperatureChanging())) {
            return false;
        }
        if (this.isTainted && this.container != null && this.container.getTemprature() > 1.0f) {
            return false;
        }
        if ((!GameClient.client || this.isInLocalPlayerInventory()) && (double)this.offAgeMax != 1.0E9) {
            if (this.replaceOnRotten != null && !this.replaceOnRotten.isEmpty()) {
                return false;
            }
            if (SandboxOptions.instance.daysForRottenFoodRemoval.getValue() != -1) {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean shouldUpdateInWorld() {
        if (!GameClient.client && (double)this.offAgeMax != 1.0E9) {
            if (this.replaceOnRotten != null && !this.replaceOnRotten.isEmpty()) {
                return true;
            }
            if (SandboxOptions.instance.daysForRottenFoodRemoval.getValue() != -1) {
                return true;
            }
        }
        if (this.getHeat() != 1.0f) {
            return true;
        }
        return !GameClient.client && this.isFertilized();
    }

    @Override
    public String getName() {
        return this.getName(null);
    }

    @Override
    public String getName(IsoPlayer player) {
        Object foodPrefix = "";
        if (this.isFertilized()) {
            foodPrefix = (String)foodPrefix + this.freshString + ", ";
        }
        if (this.burnt) {
            foodPrefix = (String)foodPrefix + this.burntString + ", ";
        } else if (!this.isFertilized() && this.offAge < 1000000000 && this.age < (float)this.offAge) {
            foodPrefix = (String)foodPrefix + this.freshString + ", ";
        } else if (!this.isFertilized() && this.offAgeMax < 1000000000 && this.age >= (float)this.offAgeMax) {
            foodPrefix = (String)foodPrefix + this.offString + ", ";
        } else if (!this.isFertilized() && this.offAgeMax < 1000000000 && this.age >= (float)this.offAge) {
            foodPrefix = (String)foodPrefix + this.staleString + ", ";
        }
        if (this.isCooked() && !this.burnt && !this.hasTag(ItemTag.HIDE_COOKED)) {
            foodPrefix = this.hasTag(ItemTag.GRILLED) ? (String)foodPrefix + this.grilledString + ", " : (this.hasTag(ItemTag.TOASTABLE) ? (String)foodPrefix + this.toastedString + ", " : (String)foodPrefix + this.cookedString + ", ");
        } else if (this.isCookable && !this.burnt && !this.hasTag(ItemTag.HIDE_COOKED) && !this.hasTag(ItemTag.HIDE_UNCOOKED)) {
            foodPrefix = (String)foodPrefix + this.unCookedString + ", ";
        }
        if (this.isFrozen()) {
            foodPrefix = (String)foodPrefix + this.frozenString + ", ";
        }
        if (((String)foodPrefix).length() > 2) {
            foodPrefix = ((String)foodPrefix).substring(0, ((String)foodPrefix).length() - 2);
        }
        if (((String)(foodPrefix = ((String)foodPrefix).trim())).isEmpty()) {
            return this.name;
        }
        return Translator.getText("IGUI_FoodNaming", foodPrefix, this.name);
    }

    @Override
    public void DoTooltip(ObjectTooltip tooltipUI, ObjectTooltip.Layout layout) {
        boolean canReadPackage;
        int value;
        ObjectTooltip.LayoutItem item;
        ColorInfo g2Bgrad = new ColorInfo();
        ColorInfo highlightGood = Core.getInstance().getGoodHighlitedColor();
        ColorInfo highlightBad = Core.getInstance().getBadHighlitedColor();
        float goodR = highlightGood.getR();
        float goodG = highlightGood.getG();
        float goodB = highlightGood.getB();
        float badR = highlightBad.getR();
        float badG = highlightBad.getG();
        float badB = highlightBad.getB();
        if (this.getHungerChange() != 0.0f && !this.hasTag(ItemTag.HIDE_HUNGER_CHANGE)) {
            item = layout.addItem();
            item.setLabel(Translator.getText("Tooltip_food_Hunger") + ":", 1.0f, 1.0f, 0.8f, 1.0f);
            value = (int)(this.getHungerChange() * 100.0f);
            item.setValueRight(value, false);
        }
        if (this.getThirstChange() != 0.0f) {
            item = layout.addItem();
            item.setLabel(Translator.getText("Tooltip_food_Thirst") + ":", 1.0f, 1.0f, 0.8f, 1.0f);
            float value2 = this.getThirstChange() * -2.0f;
            if (value2 > 0.0f) {
                item.setProgress(value2, goodR, goodG, goodB, 1.0f);
            } else {
                item.setProgress(value2 * -1.0f, badR, badG, badB, 1.0f);
            }
        }
        if (this.getEnduranceChange() != 0.0f) {
            item = layout.addItem();
            value = (int)(this.getEnduranceChange() * 100.0f);
            item.setLabel(Translator.getText("Tooltip_food_Endurance") + ":", 1.0f, 1.0f, 0.8f, 1.0f);
            if (value > 0) {
                item.setProgress((float)value / 10.0f, goodR, goodG, goodB, 1.0f);
            } else {
                item.setProgress((float)(value * -1) / 10.0f, badR, badG, badB, 1.0f);
            }
        }
        if (this.getStressChange() != 0.0f) {
            item = layout.addItem();
            float value3 = this.getStressChange() * 2.0f;
            item.setLabel(Translator.getText("Tooltip_food_Stress") + ":", 1.0f, 1.0f, 0.8f, 1.0f);
            if (this.getStressChange() < 0.0f) {
                item.setProgress(value3 * -1.0f, goodR, goodG, goodB, 1.0f);
            } else {
                item.setProgress(value3, badR, badG, badB, 1.0f);
            }
        }
        if (this.getBoredomChange() != 0.0f) {
            item = layout.addItem();
            float value4 = this.getBoredomChange() * -0.02f;
            item.setLabel(Translator.getText("Tooltip_food_Boredom") + ":", 1.0f, 1.0f, 0.8f, 1.0f);
            if (value4 > 0.0f) {
                item.setProgress(value4, goodR, goodG, goodB, 1.0f);
            } else {
                item.setProgress(value4 * -1.0f, badR, badG, badB, 1.0f);
            }
        }
        if (this.getUnhappyChange() != 0.0f) {
            item = layout.addItem();
            float value5 = this.getUnhappyChange() * -0.02f;
            item.setLabel(Translator.getText("Tooltip_food_Unhappiness") + ":", 1.0f, 1.0f, 0.8f, 1.0f);
            if (value5 > 0.0f) {
                item.setProgress(value5, goodR, goodG, goodB, 1.0f);
            } else {
                item.setProgress(value5 * -1.0f, badR, badG, badB, 1.0f);
            }
        }
        if (this.isIsCookable() && !this.isFrozen() && !this.burnt && (double)this.getHeat() > 1.6) {
            float ct = this.getCookingTime();
            float mtc = this.getMinutesToCook();
            float mtb = this.getMinutesToBurn();
            float f = ct / mtc;
            ColorInfo highlight = Core.getInstance().getGoodHighlitedColor();
            float br = highlight.getR();
            float bg = highlight.getG();
            float bb = highlight.getB();
            float ba = 1.0f;
            float tr = highlight.getR();
            float tg = highlight.getG();
            float tb = highlight.getB();
            String s = Translator.getText("IGUI_invpanel_Cooking");
            if (ct > mtc) {
                highlight = Core.getInstance().getBadHighlitedColor();
                s = Translator.getText("IGUI_invpanel_Burning");
                tr = highlight.getR();
                tg = highlight.getG();
                tb = highlight.getB();
                f = (ct - mtc) / (mtb - mtc);
                br = highlight.getR();
                bg = highlight.getG();
                bb = highlight.getB();
            }
            item = layout.addItem();
            item.setLabel(s + ": ", tr, tg, tb, 1.0f);
            item.setProgress(f, br, bg, bb, 1.0f);
        }
        if (this.getFreezingTime() < 100.0f && this.getFreezingTime() > 0.0f) {
            float f = this.getFreezingTime() / 100.0f;
            float br = 0.0f;
            float bg = 0.6f;
            float bb = 0.0f;
            float ba = 0.7f;
            float tr = 1.0f;
            float tg = 1.0f;
            float tb = 0.8f;
            item = layout.addItem();
            item.setLabel(Translator.getText("IGUI_invpanel_FreezingTime") + ": ", 1.0f, 1.0f, 0.8f, 1.0f);
            item.setProgress(f, 0.0f, 0.6f, 0.0f, 0.7f);
        }
        if (Core.debug && this.isFertilized()) {
            item = layout.addItem();
            item.setLabel("Fertilized :", 1.0f, 1.0f, 0.8f, 1.0f);
            item.setValue(this.fertilizedTime + "/" + this.timeToHatch, 1.0f, 1.0f, 1.0f, 1.0f);
        } else if (this.isFertilized() && Double.valueOf(this.timeToHatch) / Double.valueOf(this.fertilizedTime) < 4.0) {
            item = layout.addItem();
            item.setLabel(Translator.getText("Tooltip_food_fertilized"), 0.5f, 1.0f, 0.4f, 1.0f);
        }
        IsoGameCharacter character = tooltipUI.getCharacter();
        IsoPlayer player = Type.tryCastTo(character, IsoPlayer.class);
        boolean proCook = character != null & character.getPerkLevel(PerkFactory.Perks.Cooking) > 4;
        boolean illiterate = character != null && character.hasTrait(CharacterTrait.ILLITERATE);
        boolean tooDark = player != null && player.tooDarkToRead();
        boolean noLabel = this.getModData().rawget("NoLabel") != null;
        boolean bl = canReadPackage = this.isPackaged() && character != null && !illiterate && !tooDark && !noLabel;
        if (proCook && this.isIsCookable()) {
            item = layout.addItem();
            item.setLabel(Translator.getText("Tooltip_food_MinutesToCook") + ":", 1.0f, 1.0f, 0.8f, 1.0f);
            item.setValueRightNoPlus(this.getMinutesToCook());
        }
        if (Core.debug && DebugOptions.instance.tooltipInfo.getValue() || canReadPackage || character != null && (character.hasTrait(CharacterTrait.NUTRITIONIST) || character.hasTrait(CharacterTrait.NUTRITIONIST2))) {
            item = layout.addItem();
            item.setLabel(Translator.getText("Tooltip_food_Nutrition") + ":", 1.0f, 1.0f, 0.8f, 1.0f);
            item = layout.addItem();
            item.setLabel(Translator.getText("Tooltip_food_Calories") + ":", 1.0f, 1.0f, 0.8f, 1.0f);
            item.setValueRightNoPlus(this.getCalories());
            item = layout.addItem();
            item.setLabel(Translator.getText("Tooltip_food_Carbs") + ":", 1.0f, 1.0f, 0.8f, 1.0f);
            item.setValueRightNoPlus(this.getCarbohydrates());
            item = layout.addItem();
            item.setLabel(Translator.getText("Tooltip_food_Prots") + ":", 1.0f, 1.0f, 0.8f, 1.0f);
            item.setValueRightNoPlus(this.getProteins());
            item = layout.addItem();
            item.setLabel(Translator.getText("Tooltip_food_Fat") + ":", 1.0f, 1.0f, 0.8f, 1.0f);
            item.setValueRightNoPlus(this.getLipids());
        } else if (this.isPackaged() && illiterate) {
            item = layout.addItem();
            item.setLabel(Translator.getText("Tooltip_food_Nutrition") + ":", 1.0f, 1.0f, 0.8f, 1.0f);
            item = layout.addItem();
            item.setLabel(Translator.getText("ContextMenu_Illiterate"), 1.0f, 1.0f, 0.8f, 1.0f);
        } else if (this.isPackaged() && tooDark) {
            item = layout.addItem();
            item.setLabel(Translator.getText("Tooltip_food_Nutrition") + ":", 1.0f, 1.0f, 0.8f, 1.0f);
            item = layout.addItem();
            item.setLabel(Translator.getText("ContextMenu_TooDark"), 1.0f, 1.0f, 0.8f, 1.0f);
        }
        if (this.isbDangerousUncooked() && !this.isCooked() && !this.isBurnt()) {
            item = layout.addItem();
            item.setLabel(Translator.getText("Tooltip_food_Dangerous_uncooked"), Core.getInstance().getBadHighlitedColor().getR(), Core.getInstance().getBadHighlitedColor().getG(), Core.getInstance().getBadHighlitedColor().getB(), 1.0f);
            if (this.hasTag(ItemTag.EGG)) {
                item.setLabel(Translator.getText("Tooltip_food_SlightDanger_uncooked"), 1.0f, 0.0f, 0.0f, 1.0f);
            }
        }
        if (this.getScriptItem().removeUnhappinessWhenCooked && !this.isCooked() && !this.isBurnt()) {
            item = layout.addItem();
            item.setLabel(Translator.getText("Tooltip_food_CookToRemoveUnhappiness"), Core.getInstance().getBadHighlitedColor().getR(), Core.getInstance().getBadHighlitedColor().getG(), Core.getInstance().getBadHighlitedColor().getB(), 1.0f);
        }
        if ((this.isGoodHot() || this.isBadCold()) && this.heat < 1.3f) {
            item = layout.addItem();
            item.setLabel(Translator.getText("Tooltip_food_BetterHot"), 1.0f, 0.9f, 0.9f, 1.0f);
        }
        if (this.cookedInMicrowave) {
            item = layout.addItem();
            item.setLabel(Translator.getText("Tooltip_food_CookedInMicrowave"), 1.0f, 0.9f, 0.9f, 1.0f);
        }
        if (!StringUtils.isNullOrEmpty(this.getMilkType())) {
            item = layout.addItem();
            item.setLabel(Translator.getText("Tooltip_food_MilkType") + ": ", 1.0f, 0.9f, 0.9f, 1.0f);
            item.setValue(Translator.getText("Tooltip_food_" + this.getMilkType()), 1.0f, 0.9f, 0.9f, 1.0f);
        }
        if (Core.debug && DebugOptions.instance.tooltipInfo.getValue()) {
            item = layout.addItem();
            item.setLabel("DBG: BaseHunger", 0.0f, 1.0f, 0.0f, 1.0f);
            item.setValueRight((int)(this.getBaseHunger() * 100.0f), false);
            item = layout.addItem();
            item.setLabel("DBG: Age", 0.0f, 1.0f, 0.0f, 1.0f);
            item.setValueRightNoPlus(this.getAge() * 24.0f);
            if ((double)this.getOffAgeMax() != 1.0E9) {
                item = layout.addItem();
                item.setLabel("DBG: Age Fresh", 0.0f, 1.0f, 0.0f, 1.0f);
                item.setValueRightNoPlus((float)this.getOffAge() * 24.0f);
                item = layout.addItem();
                item.setLabel("DBG: Age Rotten", 0.0f, 1.0f, 0.0f, 1.0f);
                item.setValueRightNoPlus(this.getOffAgeMax() * 24);
            }
            item = layout.addItem();
            item.setLabel("DBG: Heat", 0.0f, 1.0f, 0.0f, 1.0f);
            item.setValueRightNoPlus(this.getHeat());
            item = layout.addItem();
            item.setLabel("DBG: Freeze Time", 0.0f, 1.0f, 0.0f, 1.0f);
            item.setValueRightNoPlus(this.getFreezingTime());
            item = layout.addItem();
            item.setLabel("DBG: Compost Time", 0.0f, 1.0f, 0.0f, 1.0f);
            item.setValueRightNoPlus(this.getCompostTime());
        }
    }

    public float getEnduranceChange() {
        if (this.burnt) {
            return this.endChange / 3.0f;
        }
        if (this.age >= (float)this.offAge && this.age < (float)this.offAgeMax) {
            return this.endChange / 2.0f;
        }
        if (this.isCooked()) {
            return this.endChange * 2.0f;
        }
        return this.endChange;
    }

    public void setEnduranceChange(float endChange) {
        this.endChange = endChange;
    }

    @Override
    public float getUnhappyChange() {
        boolean goodFrozen;
        float result = this.unhappyChange;
        if (this.isFertilized()) {
            return result;
        }
        boolean bl = goodFrozen = "Icecream".equals(this.getType()) || this.hasTag(ItemTag.GOOD_FROZEN);
        if (this.isFrozen() && !goodFrozen) {
            result += 30.0f;
        }
        if (this.burnt) {
            result += 20.0f;
        }
        if (this.age >= (float)this.offAge && this.age < (float)this.offAgeMax) {
            result += 10.0f;
        }
        if (this.age >= (float)this.offAgeMax) {
            result += 20.0f;
        }
        if (this.isBadCold() && this.isCookable && this.isCooked() && this.heat < 1.3f) {
            result += 2.0f;
        }
        if (this.isGoodHot() && this.isCookable && this.isCooked() && this.heat > 1.3f) {
            result -= 2.0f;
        }
        return result;
    }

    @Override
    public float getBoredomChange() {
        boolean goodFrozen;
        float result = this.boredomChange;
        if (this.isFertilized()) {
            return result;
        }
        boolean bl = goodFrozen = "Icecream".equals(this.getType()) || this.hasTag(ItemTag.GOOD_FROZEN);
        if (this.isFrozen() && !goodFrozen) {
            result += 30.0f;
        }
        if (this.burnt) {
            result += 20.0f;
        }
        if (this.age >= (float)this.offAge && this.age < (float)this.offAgeMax) {
            result += 10.0f;
        }
        if (this.age >= (float)this.offAgeMax) {
            result += 20.0f;
        }
        return result;
    }

    public float getHungerChange() {
        float hungChange = this.hungChange;
        if (hungChange != 0.0f) {
            if (this.isCooked()) {
                return hungChange * 1.3f;
            }
            float sign = hungChange < 0.0f ? -1.0f : 1.0f;
            float absHungChange = Math.abs(hungChange);
            if (this.burnt) {
                return Math.max(absHungChange / 3.0f, 0.01f) * sign;
            }
            if (this.age >= (float)this.offAge && this.age < (float)this.offAgeMax) {
                return Math.max(absHungChange / 1.3f, 0.01f) * sign;
            }
            if (this.age >= (float)this.offAgeMax) {
                return Math.max(absHungChange / 2.2f, 0.01f) * sign;
            }
        }
        return hungChange;
    }

    @Override
    public float getStressChange() {
        if (this.isFertilized()) {
            return this.stressChange;
        }
        if (this.burnt) {
            return this.stressChange / 4.0f;
        }
        if (this.age >= (float)this.offAge && this.age < (float)this.offAgeMax) {
            return this.stressChange / 1.3f;
        }
        if (this.age >= (float)this.offAgeMax) {
            return this.stressChange / 2.0f;
        }
        if (this.isCooked()) {
            return this.stressChange * 1.3f;
        }
        return this.stressChange;
    }

    public float getBoredomChangeUnmodified() {
        return this.boredomChange;
    }

    public float getEnduranceChangeUnmodified() {
        return this.endChange;
    }

    public float getStressChangeUnmodified() {
        return this.stressChange;
    }

    public float getThirstChangeUnmodified() {
        return this.thirstChange;
    }

    public float getUnhappyChangeUnmodified() {
        return this.unhappyChange;
    }

    @Override
    public float getScore(SurvivorDesc desc) {
        float score = 0.0f;
        return score -= this.getHungerChange() * 100.0f;
    }

    public boolean isBadCold() {
        return this.badCold;
    }

    public void setBadCold(boolean bBadCold) {
        this.badCold = bBadCold;
    }

    public boolean isGoodHot() {
        return this.goodHot;
    }

    public void setGoodHot(boolean bGoodHot) {
        this.goodHot = bGoodHot;
    }

    public boolean isCookedInMicrowave() {
        return this.cookedInMicrowave;
    }

    public void setCookedInMicrowave(boolean b) {
        this.cookedInMicrowave = b;
    }

    public float getHeat() {
        return this.heat;
    }

    @Override
    public float getInvHeat() {
        if (this.heat > 1.0f) {
            return (this.heat - 1.0f) / 2.0f;
        }
        return 1.0f - (this.heat - 0.2f) / 0.8f;
    }

    public void setHeat(float heat) {
        this.heat = heat;
    }

    public float getEndChange() {
        return this.endChange;
    }

    public void setEndChange(float endChange) {
        this.endChange = endChange;
    }

    @Deprecated
    public float getBaseHungChange() {
        return this.getHungChange();
    }

    public float getHungChange() {
        return this.hungChange;
    }

    public void setHungChange(float hungChange) {
        this.hungChange = hungChange;
    }

    public String getUseOnConsume() {
        return this.useOnConsume;
    }

    public void setUseOnConsume(String useOnConsume) {
        this.useOnConsume = useOnConsume;
    }

    public boolean isRotten() {
        if (this.isFertilized()) {
            return false;
        }
        return this.age >= (float)this.offAgeMax;
    }

    public boolean isFresh() {
        if (this.isFertilized()) {
            return true;
        }
        return this.age < (float)this.offAge;
    }

    public void setRotten(boolean rotten) {
        this.rotten = rotten;
    }

    public boolean isbDangerousUncooked() {
        return this.dangerousUncooked;
    }

    public void setbDangerousUncooked(boolean dangerousUncooked) {
        this.dangerousUncooked = dangerousUncooked;
    }

    public int getLastCookMinute() {
        return this.lastCookMinute;
    }

    public void setLastCookMinute(int lastCookMinute) {
        this.lastCookMinute = lastCookMinute;
    }

    public float getThirstChange() {
        float thirstChange = this.thirstChange;
        if (this.burnt) {
            return thirstChange / 5.0f;
        }
        if (this.isCooked()) {
            return thirstChange / 2.0f;
        }
        return thirstChange;
    }

    public void setThirstChange(float thirstChange) {
        this.thirstChange = thirstChange;
    }

    public void setReplaceOnCooked(List<String> replaceOnCooked) {
        this.replaceOnCooked = replaceOnCooked;
    }

    public List<String> getReplaceOnCooked() {
        return this.replaceOnCooked;
    }

    public float getBaseHunger() {
        return this.baseHunger;
    }

    public void setBaseHunger(float baseHunger) {
        this.baseHunger = baseHunger;
    }

    @Override
    public boolean isSpice() {
        return this.isSpice;
    }

    public void setSpice(boolean isSpice) {
        this.isSpice = isSpice;
    }

    public boolean isPoison() {
        return this.poison;
    }

    public int getPoisonDetectionLevel() {
        return this.poisonDetectionLevel;
    }

    public void setPoisonDetectionLevel(int poisonDetectionLevel) {
        this.poisonDetectionLevel = poisonDetectionLevel;
    }

    public int getPoisonLevelForRecipe() {
        return this.poisonLevelForRecipe;
    }

    public void setPoisonLevelForRecipe(Integer poisonLevelForRecipe) {
        this.poisonLevelForRecipe = poisonLevelForRecipe;
    }

    public int getUseForPoison() {
        return this.useForPoison;
    }

    public void setUseForPoison(int useForPoison) {
        this.useForPoison = useForPoison;
    }

    public int getPoisonPower() {
        return this.poisonPower;
    }

    public void setPoisonPower(int poisonPower) {
        this.poisonPower = poisonPower;
    }

    public String getFoodType() {
        return this.foodType;
    }

    public void setFoodType(String foodType) {
        this.foodType = foodType;
    }

    public boolean isRemoveNegativeEffectOnCooked() {
        return this.removeNegativeEffectOnCooked;
    }

    public void setRemoveNegativeEffectOnCooked(boolean removeNegativeEffectOnCooked) {
        this.removeNegativeEffectOnCooked = removeNegativeEffectOnCooked;
    }

    public String getCookingSound() {
        return this.getScriptItem().getCookingSound();
    }

    public String getCustomEatSound() {
        return this.customEatSound;
    }

    public void setCustomEatSound(String customEatSound) {
        this.customEatSound = customEatSound;
    }

    public String getChef() {
        return this.chef;
    }

    public void setChef(String chef) {
        this.chef = chef;
    }

    public String getOnCooked() {
        return this.onCooked;
    }

    public void setOnCooked(String onCooked) {
        this.onCooked = onCooked;
    }

    public String getHerbalistType() {
        return this.herbalistType;
    }

    public void setHerbalistType(String type) {
        this.herbalistType = type;
    }

    public ArrayList<String> getSpices() {
        return this.spices;
    }

    public void setSpices(ArrayList<String> spices) {
        if (spices == null || spices.isEmpty()) {
            if (this.spices != null) {
                this.spices.clear();
            }
            return;
        }
        if (this.spices == null) {
            this.spices = new ArrayList<String>(spices);
        } else {
            this.spices.clear();
            this.spices.addAll(spices);
        }
    }

    @Override
    public Texture getTex() {
        if (this.burnt) {
            return this.textureBurnt;
        }
        if (this.age >= (float)this.offAgeMax) {
            return this.texturerotten;
        }
        if (this.isCooked()) {
            return this.textureCooked;
        }
        return super.getTex();
    }

    @Override
    public String getWorldTexture() {
        if (this.burnt) {
            return this.worldTextureOverdone;
        }
        if (this.age >= (float)this.offAgeMax) {
            return this.worldTextureRotten;
        }
        if (this.isCooked()) {
            return this.worldTextureCooked;
        }
        return this.worldTexture;
    }

    @Override
    public String getStaticModel() {
        ModelScript modelScript;
        if (this.isBurnt() && (modelScript = ScriptManager.instance.getModelScript(super.getStaticModel() + "Burnt")) != null) {
            return modelScript.getName();
        }
        if (this.isRotten() && (modelScript = ScriptManager.instance.getModelScript(super.getStaticModel() + "Rotten")) != null) {
            return modelScript.getName();
        }
        if (this.isCooked() && (modelScript = ScriptManager.instance.getModelScript(super.getStaticModel() + "Cooked")) != null) {
            return modelScript.getName();
        }
        return super.getStaticModel();
    }

    @Override
    public int getFoodSicknessChange() {
        if (this.burnt) {
            return (int)((float)this.foodSicknessChange / 3.0f);
        }
        if (this.age >= (float)this.offAge && this.age < (float)this.offAgeMax) {
            return (int)((float)this.foodSicknessChange / 1.3f);
        }
        if (this.age >= (float)this.offAgeMax) {
            return (int)((float)this.foodSicknessChange / 2.2f);
        }
        if (this.isCooked()) {
            return (int)((float)this.foodSicknessChange * 1.3f);
        }
        return this.foodSicknessChange;
    }

    @Override
    public void setFoodSicknessChange(int foodSicknessChange) {
        this.foodSicknessChange = foodSicknessChange;
    }

    public int getFluReduction() {
        return this.fluReduction;
    }

    public void setFluReduction(int fluReduction) {
        this.fluReduction = fluReduction;
    }

    public float getPainReduction() {
        return this.painReduction;
    }

    public void setPainReduction(float painReduction) {
        this.painReduction = painReduction;
    }

    public float getCarbohydrates() {
        return this.carbohydrates;
    }

    public void setCarbohydrates(float carbohydrates) {
        this.carbohydrates = carbohydrates;
    }

    public float getLipids() {
        return this.lipids;
    }

    public void setLipids(float lipids) {
        this.lipids = lipids;
    }

    public float getProteins() {
        return this.proteins;
    }

    public void setProteins(float proteins) {
        this.proteins = proteins;
    }

    public float getCalories() {
        return this.calories;
    }

    public void setCalories(float calories) {
        this.calories = calories;
    }

    public boolean isPackaged() {
        return this.packaged;
    }

    public void setPackaged(boolean packaged) {
        this.packaged = packaged;
    }

    public float getFreezingTime() {
        return this.freezingTime;
    }

    public void setFreezingTime(float freezingTime) {
        if (freezingTime >= 100.0f) {
            this.setFrozen(true);
            freezingTime = 100.0f;
        } else if (freezingTime <= 0.0f) {
            freezingTime = 0.0f;
            this.setFrozen(false);
        }
        this.freezingTime = freezingTime;
    }

    public void freeze() {
        this.setFreezingTime(100.0f);
    }

    public boolean isFrozen() {
        return this.frozen;
    }

    public void setFrozen(boolean frozen) {
        this.frozen = frozen;
    }

    public boolean canBeFrozen() {
        return this.canBeFrozen;
    }

    public void setCanBeFrozen(boolean canBeFrozen) {
        this.canBeFrozen = canBeFrozen;
    }

    public boolean isFreezing() {
        if (!this.canBeFrozen() || this.getFreezingTime() >= 100.0f || this.getOutermostContainer() == null || !"freezer".equals(this.getOutermostContainer().getType())) {
            return false;
        }
        return this.getOutermostContainer().isPowered();
    }

    public boolean isThawing() {
        if (!this.canBeFrozen() || this.getFreezingTime() <= 0.0f) {
            return false;
        }
        if (this.getOutermostContainer() == null || !"freezer".equals(this.getOutermostContainer().getType())) {
            return true;
        }
        return !this.getOutermostContainer().isPowered();
    }

    @Override
    public int getMaxUses() {
        if (this.getBaseHunger() == 0.0f) {
            return 1;
        }
        return (int)Math.abs(this.getBaseHunger() * 100.0f);
    }

    @Override
    public int getCurrentUses() {
        if (this.getBaseHunger() == 0.0f) {
            return super.getCurrentUses();
        }
        return (int)Math.abs(this.getHungChange() * 100.0f);
    }

    @Override
    public void setCurrentUses(int newuses) {
        newuses = Math.max(0, newuses);
        if (this.getBaseHunger() == 0.0f) {
            super.setCurrentUses(newuses);
        } else {
            this.consumeHunger((float)(this.getCurrentUses() - newuses) / 100.0f);
        }
    }

    @Override
    public float getCurrentUsesFloat() {
        if (this.getBaseHunger() == 0.0f) {
            return super.getCurrentUsesFloat();
        }
        return Math.abs(this.getHungChange());
    }

    @Override
    public void syncItemFields() {
        ItemContainer outer = this.getOutermostContainer();
        if (outer != null && outer.getParent() instanceof IsoPlayer) {
            if (GameClient.client) {
                INetworkPacket.send(PacketTypes.PacketType.ItemStats, this.getContainer(), this);
            } else if (GameServer.server) {
                INetworkPacket.send((IsoPlayer)outer.getParent(), PacketTypes.PacketType.ItemStats, this.getContainer(), this);
            }
        }
    }

    public String getReplaceOnRotten() {
        return this.replaceOnRotten;
    }

    public void setReplaceOnRotten(String replaceOnRotten) {
        this.replaceOnRotten = replaceOnRotten;
    }

    public void multiplyFoodValues(float percentage) {
        this.setBoredomChange(this.getBoredomChangeUnmodified() * percentage);
        this.setUnhappyChange(this.getUnhappyChangeUnmodified() * percentage);
        this.setHungChange(this.getHungChange() * percentage);
        this.setFluReduction((int)((float)this.getFluReduction() * percentage));
        this.setThirstChange(this.getThirstChangeUnmodified() * percentage);
        this.setPainReduction(this.getPainReduction() * percentage);
        this.setFoodSicknessChange((int)((float)this.getFoodSicknessChange() * percentage));
        this.setEndChange(this.getEnduranceChangeUnmodified() * percentage);
        this.setStressChange(this.getStressChangeUnmodified() * percentage);
        this.setFatigueChange(this.getFatigueChange() * percentage);
        this.setCalories(this.getCalories() * percentage);
        this.setCarbohydrates(this.getCarbohydrates() * percentage);
        this.setProteins(this.getProteins() * percentage);
        this.setLipids(this.getLipids() * percentage);
        this.setPoisonPower((int)((float)this.getPoisonPower() * percentage));
    }

    public float getRottenTime() {
        return this.rottenTime;
    }

    public void setRottenTime(float time) {
        this.rottenTime = time;
    }

    public float getCompostTime() {
        return this.compostTime;
    }

    public void setCompostTime(float compostTime) {
        this.compostTime = compostTime;
    }

    public String getOnEat() {
        return this.onEat;
    }

    public void setOnEat(String onEat) {
        this.onEat = onEat;
    }

    public boolean isBadInMicrowave() {
        return this.badInMicrowave;
    }

    public void setBadInMicrowave(boolean badInMicrowave) {
        this.badInMicrowave = badInMicrowave;
    }

    public boolean isTainted() {
        return this.isTainted;
    }

    public void setTainted(boolean tainted) {
        this.isTainted = tainted;
    }

    private void destroyThisItem() {
        IsoWorldInventoryObject worldInvObj = this.getWorldItem();
        if (worldInvObj != null && worldInvObj.getSquare() != null) {
            if (GameServer.server) {
                GameServer.RemoveItemFromMap(worldInvObj);
            } else {
                worldInvObj.removeFromWorld();
                worldInvObj.removeFromSquare();
            }
            this.setWorldItem(null);
        } else if (this.container != null) {
            IsoObject parent = this.container.getParent();
            if (GameServer.server) {
                GameServer.sendRemoveItemFromContainer(this.container, this);
                this.container.Remove(this);
            } else {
                this.container.Remove(this);
            }
            IsoWorld.instance.currentCell.addToProcessItemsRemove(this);
            LuaManager.updateOverlaySprite(parent);
        }
        if (!GameServer.server) {
            LuaEventManager.triggerEvent("OnContainerUpdate");
        }
    }

    public void setMilkQty(int qty) {
        this.milkQty = qty;
    }

    public int getMilkQty() {
        return this.milkQty;
    }

    public void setMilkType(String type) {
        this.milkType = type;
    }

    public String getMilkType() {
        return this.milkType;
    }

    public boolean isFertilized() {
        return this.fertilized;
    }

    public void setFertilized(boolean fertilized) {
        this.fertilized = fertilized;
    }

    public String getAnimalHatch() {
        return this.animalHatch;
    }

    public void setAnimalHatch(String animalHatch) {
        this.animalHatch = animalHatch;
    }

    public String getAnimalHatchBreed() {
        return this.animalHatchBreed;
    }

    public void setAnimalHatchBreed(String animalHatchBreed) {
        this.animalHatchBreed = animalHatchBreed;
    }

    public int getTimeToHatch() {
        return this.timeToHatch;
    }

    public void setTimeToHatch(int timeToHatch) {
        float mod = 1.0f;
        switch (SandboxOptions.instance.animalEggHatch.getValue()) {
            case 1: {
                mod = 0.1f;
                break;
            }
            case 2: {
                mod = 0.5f;
                break;
            }
            case 3: {
                mod = 0.7f;
                break;
            }
            case 5: {
                mod = 2.5f;
                break;
            }
            case 6: {
                mod = 10.0f;
            }
        }
        this.timeToHatch = (int)((float)timeToHatch * mod);
    }

    public boolean isNormalAndFullFood() {
        if (this.isTainted() || this.isRotten() || this.isFertilized()) {
            return false;
        }
        if (this.getSpices() != null) {
            return false;
        }
        if (this.getCompostTime() != 0.0f) {
            return false;
        }
        Item scriptItem = this.getScriptItem();
        if (scriptItem == null) {
            return false;
        }
        if ((float)this.getPoisonPower() != scriptItem.getPoisonPower()) {
            return false;
        }
        return this.isWholeFoodItem() && this.isUncooked();
    }

    public boolean isWholeFoodItem() {
        Item scriptItem = this.getScriptItem();
        if (scriptItem == null) {
            return false;
        }
        if (this.getHungChange() * 100.0f != scriptItem.getHungerChange()) {
            return false;
        }
        if (this.getUnhappyChange() != scriptItem.getUnhappyChange()) {
            return false;
        }
        if (this.getBoredomChange() != scriptItem.getBoredomChange()) {
            return false;
        }
        if (this.getThirstChange() * 100.0f != scriptItem.getThirstChange()) {
            return false;
        }
        Food test = (Food)InventoryItemFactory.CreateItem(this.getFullType());
        if (test == null) {
            return false;
        }
        if (this.getCalories() != test.getCalories()) {
            return false;
        }
        if (this.getProteins() != test.getProteins()) {
            return false;
        }
        if (this.getLipids() != test.getLipids()) {
            return false;
        }
        if (this.getCarbohydrates() != test.getCarbohydrates()) {
            return false;
        }
        if (this.getPainReduction() != test.getPainReduction()) {
            return false;
        }
        if (this.getFluReduction() != test.getFluReduction()) {
            return false;
        }
        if (this.getFoodSicknessChange() != test.getFoodSicknessChange()) {
            return false;
        }
        return this.getFatigueChange() == test.getFatigueChange();
    }

    public boolean isUncooked() {
        return !this.isCooked() && !this.isBurnt();
    }

    @Override
    public void OnAddedToContainer(ItemContainer container) {
        if (GameServer.server) {
            this.updateAge();
        }
    }

    @Override
    public void OnBeforeRemoveFromContainer(ItemContainer container) {
        if (GameServer.server) {
            this.updateAge();
        }
    }

    public void setFertilizedTime(int time) {
        this.fertilizedTime = time;
    }

    @Override
    public void inheritFoodAgeFrom(InventoryItem otherItem) {
        if (!otherItem.isFood()) {
            return;
        }
        Food otherFood = (Food)otherItem;
        if (otherFood.canAge() && this.canAge()) {
            float age = otherFood.getAge() / (float)otherFood.getOffAgeMax();
            this.setAge((float)this.getOffAgeMax() * age);
        }
    }

    @Override
    public void inheritOlderFoodAge(InventoryItem otherItem) {
        float age;
        if (!otherItem.isFood()) {
            return;
        }
        Food otherFood = (Food)otherItem;
        if (otherFood.canAge() && this.canAge() && (age = otherFood.getAge() / (float)otherFood.getOffAgeMax()) > (float)this.getOffAgeMax() * age) {
            this.inheritFoodAgeFrom(otherFood);
        }
    }

    public boolean hasAnimalParts() {
        if (this.getModData().rawget("parts") != null) {
            return (Boolean)this.getModData().rawget("parts");
        }
        return false;
    }

    public boolean isAnimalSkeleton() {
        return "true".equals(this.getModData().rawget("skeleton"));
    }

    public boolean canAge() {
        return this.getOffAgeMax() != 1000000000;
    }

    @Override
    public boolean isFood() {
        return true;
    }

    public void copyFrozenFrom(Food otherFood) {
        if (!this.canBeFrozen()) {
            return;
        }
        this.setFreezingTime(otherFood.getFreezingTime());
    }

    public void copyCookedBurntFrom(Food otherFood) {
        float otherCookingTime = otherFood.getCookingTime();
        float otherMinutesToCook = otherFood.getMinutesToCook();
        float otherMinutesToBurn = otherFood.getMinutesToBurn();
        if (otherFood.isBurnt()) {
            this.setBurnt(true);
            float ratio = otherCookingTime / otherMinutesToBurn;
            float newTime = this.getMinutesToCook() * ratio;
            if (newTime >= this.getMinutesToBurn()) {
                this.setCookingTime(newTime);
            }
            if (otherFood.isCookedInMicrowave()) {
                this.setCookedInMicrowave(true);
            }
        } else if (otherFood.isCooked()) {
            this.setCooked(true);
            float ratio = otherCookingTime / otherMinutesToCook;
            float newTime = this.getMinutesToCook() * ratio;
            if (newTime >= this.getMinutesToCook()) {
                this.setCookingTime(newTime);
            }
            if (otherFood.isCookedInMicrowave()) {
                this.setCookedInMicrowave(true);
            }
        } else if (otherCookingTime > 0.0f) {
            float ratio = otherCookingTime / otherMinutesToCook;
            float newTime = this.getMinutesToCook() * ratio;
            if (newTime < this.getMinutesToCook()) {
                this.setCookingTime(newTime);
            }
        }
    }

    public void copyTemperatureFrom(Food otherFood) {
        this.setHeat(otherFood.getHeat());
    }

    public void copyPoisonFrom(Food otherFood) {
        if (otherFood.isTainted) {
            this.setTainted(true);
        }
        if (otherFood.getPoisonDetectionLevel() > -1) {
            this.setPoisonDetectionLevel(otherFood.getPoisonDetectionLevel());
        }
        if (otherFood.getPoisonPower() > 0) {
            this.setPoisonPower(otherFood.getPoisonPower());
        }
    }

    public void copyAgeFrom(Food otherFood) {
        if (!this.canAge()) {
            return;
        }
        if (otherFood.getAge() >= (float)otherFood.getOffAgeMax()) {
            float ratio = otherFood.getAge() / (float)otherFood.getOffAgeMax();
            float newAge = (float)this.getOffAgeMax() * ratio;
            if (newAge >= (float)this.getOffAgeMax()) {
                this.setAge(newAge);
            }
        } else if (otherFood.getAge() >= (float)otherFood.getOffAge()) {
            float ratio = otherFood.getAge() / (float)otherFood.getOffAge();
            float newAge = (float)this.getOffAge() * ratio;
            if (newAge >= (float)this.getOffAge()) {
                this.setAge(newAge);
            }
        } else if (otherFood.getAge() > 0.0f) {
            float ratio = otherFood.getAge() / (float)otherFood.getOffAge();
            float newAge = (float)this.getOffAge() * ratio;
            if (newAge < (float)this.getOffAge()) {
                this.setAge(newAge);
            }
        }
    }

    public void copyNutritionFrom(Food otherFood) {
        this.copyNutritionFromSplit(otherFood, 1);
    }

    public void copyNutritionFromSplit(Food otherFood, int split) {
        this.copyNutritionFromRatio(otherFood, 1.0f / (float)split);
    }

    public void copyNutritionFromRatio(Food otherFood, float ratio) {
        this.setBaseHunger(otherFood.getBaseHunger() * ratio);
        this.setHungChange(otherFood.getHungChange() * ratio);
        this.setCarbohydrates(otherFood.getCarbohydrates() * ratio);
        this.setLipids(otherFood.getLipids() * ratio);
        this.setProteins(otherFood.getProteins() * ratio);
        this.setCalories(otherFood.getCalories() * ratio);
        this.setUnhappyChange(otherFood.getUnhappyChange() * ratio);
        this.setThirstChange(otherFood.getThirstChange() * ratio);
    }

    public void copyFoodFrom(Food otherFood) {
        this.copyFoodFromSplit(otherFood, 1);
    }

    public void copyExtraItems(Food otherFood) {
        if (otherFood.haveExtraItems()) {
            for (String extraItem : otherFood.getExtraItems()) {
                this.addExtraItem(extraItem);
            }
        }
    }

    public void copyFoodFromSplit(Food otherFood, int split) {
        this.copyNutritionFromSplit(otherFood, split);
        this.copyFrozenFrom(otherFood);
        this.copyCookedBurntFrom(otherFood);
        this.copyTemperatureFrom(otherFood);
        this.copyPoisonFrom(otherFood);
        this.copyAgeFrom(otherFood);
        this.copyExtraItems(otherFood);
    }

    public void consumeHunger(float realUsedHunger) {
        float percentageUsed = Math.abs(realUsedHunger / this.getHungChange());
        this.multiplyFoodValues(1.0f - percentageUsed);
    }
}

