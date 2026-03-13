/*
 * Decompiled with CFR 0.152.
 */
package zombie.inventory.types;

import java.util.List;
import zombie.GameTime;
import zombie.Lua.LuaManager;
import zombie.UsedFromLua;
import zombie.characters.IsoGameCharacter;
import zombie.characters.IsoPlayer;
import zombie.core.math.PZMath;
import zombie.core.random.Rand;
import zombie.entity.energy.Energy;
import zombie.interfaces.IUpdater;
import zombie.inventory.InventoryItem;
import zombie.inventory.InventoryItemFactory;
import zombie.inventory.ItemContainer;
import zombie.inventory.ItemUser;
import zombie.inventory.types.Drainable;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoObject;
import zombie.iso.IsoWorld;
import zombie.iso.objects.IsoBarbecue;
import zombie.iso.objects.IsoWorldInventoryObject;
import zombie.iso.objects.RainManager;
import zombie.network.GameClient;
import zombie.network.GameServer;
import zombie.network.PacketTypes;
import zombie.network.packets.INetworkPacket;
import zombie.scripting.ScriptManager;
import zombie.scripting.objects.Item;
import zombie.scripting.objects.ItemKey;
import zombie.scripting.objects.ItemTag;
import zombie.util.StringUtils;

@UsedFromLua
public final class DrainableComboItem
extends InventoryItem
implements Drainable,
IUpdater {
    private boolean useWhileEquiped = true;
    private boolean useWhileUnequiped;
    private int ticksPerEquipUse = 30;
    private float useDelta = 0.03125f;
    private float ticks;
    private String replaceOnDeplete;
    private String replaceOnDepleteFullType;
    public List<String> replaceOnCooked;
    private String onCooked;
    private boolean canConsolidate = true;
    private float weightEmpty;
    private static final float MIN_HEAT = 0.2f;
    private static final float MAX_HEAT = 3.0f;
    private String onEat;
    private int lastUpdateMinutes = -1;
    private float heat = 1.0f;
    private int lastCookMinute;

    public DrainableComboItem(String module, String name, String itemType, String texName) {
        super(module, name, itemType, texName);
    }

    public DrainableComboItem(String module, String name, String itemType, Item item) {
        super(module, name, itemType, item);
    }

    @Override
    public boolean IsDrainable() {
        return true;
    }

    @Override
    public int getMaxUses() {
        return (int)Math.floor(1.0f / this.useDelta);
    }

    @Override
    public void setCurrentUses(int newuses) {
        this.uses = newuses;
        this.updateWeight();
    }

    @Deprecated
    public void setUsedDelta(float delta) {
        this.setCurrentUsesFloat(delta);
    }

    @Override
    public void setCurrentUsesFloat(float newUses) {
        newUses = PZMath.clamp(newUses, 0.0f, 1.0f);
        this.uses = Math.round(newUses / this.useDelta);
        this.updateWeight();
    }

    @Override
    public float getCurrentUsesFloat() {
        return (float)this.uses * this.useDelta;
    }

    @Override
    public void render() {
    }

    @Override
    public void renderlast() {
    }

    @Override
    public boolean shouldUpdateInWorld() {
        return !GameServer.server && this.heat != 1.0f;
    }

    @Override
    public void update() {
        IsoWorldInventoryObject worldItem;
        IsoGridSquare sq;
        float temp;
        ItemContainer outermostContainer = this.getOutermostContainer();
        if (outermostContainer != null) {
            int currentCookMinute;
            temp = outermostContainer.getTemprature();
            if (this.heat > temp) {
                this.heat -= 0.001f * GameTime.instance.getMultiplier();
                if (this.heat < Math.max(0.2f, temp)) {
                    this.heat = Math.max(0.2f, temp);
                }
            }
            if (this.heat < temp) {
                this.heat += temp / 1000.0f * GameTime.instance.getMultiplier();
                if (this.heat > Math.min(3.0f, temp)) {
                    this.heat = Math.min(3.0f, temp);
                }
            }
            if (this.isCookable && this.heat > 1.6f && (currentCookMinute = GameTime.getInstance().getMinutes()) != this.lastCookMinute) {
                float timeToCook;
                this.lastCookMinute = currentCookMinute;
                float dt = this.heat / 1.5f;
                if (outermostContainer.getTemprature() <= 1.6f) {
                    dt *= 0.05f;
                }
                if ((timeToCook = this.cookingTime) < 1.0f) {
                    timeToCook = 10.0f;
                }
                timeToCook += dt;
                if (!this.isCooked() && timeToCook > this.minutesToCook) {
                    this.setCooked(true);
                    if (this.getReplaceOnCooked() != null) {
                        for (int i = 0; i < this.getReplaceOnCooked().size(); ++i) {
                            InventoryItem newItem = this.container.AddItem(this.getReplaceOnCooked().get(i));
                            if (newItem == null) continue;
                            if (newItem instanceof DrainableComboItem) {
                                newItem.setCurrentUses(this.getCurrentUses());
                            }
                            newItem.copyConditionStatesFrom(this);
                            if (!GameServer.server) continue;
                            GameServer.sendAddItemToContainer(this.container, newItem);
                        }
                        if (GameServer.server) {
                            GameServer.sendRemoveItemFromContainer(this.container, this);
                        }
                        this.container.Remove(this);
                        IsoWorld.instance.currentCell.addToProcessItemsRemove(this);
                        return;
                    }
                    if (this.getOnCooked() != null) {
                        LuaManager.caller.protectedCall(LuaManager.thread, LuaManager.env.rawget(this.getOnCooked()), this);
                        return;
                    }
                }
                if (this.cookingTime > this.minutesToBurn) {
                    this.burnt = true;
                    this.setCooked(false);
                }
            }
        }
        if (this.container == null && this.heat != 1.0f) {
            temp = 1.0f;
            if (this.heat > 1.0f) {
                this.heat -= 0.001f * GameTime.instance.getMultiplier();
                if (this.heat < 1.0f) {
                    this.heat = 1.0f;
                }
            }
            if (this.heat < 1.0f) {
                this.heat += 0.001f * GameTime.instance.getMultiplier();
                if (this.heat > 1.0f) {
                    this.heat = 1.0f;
                }
            }
        }
        if (this.useWhileEquiped && this.uses > 0) {
            IsoObject dt;
            IsoGameCharacter p = null;
            if (this.container != null && (dt = this.container.parent) instanceof IsoPlayer) {
                IsoPlayer isoPlayer = (IsoPlayer)dt;
                p = isoPlayer;
            }
            if (p != null && (this.canBeActivated() && this.isActivated() || !this.canBeActivated()) && (p.isHandItem(this) || p.isAttachedItem(this))) {
                int minutes = GameTime.instance.getMinutes() / 10 * 10;
                if (minutes != this.lastUpdateMinutes) {
                    if (this.lastUpdateMinutes > -1) {
                        this.Use();
                    }
                    this.lastUpdateMinutes = minutes;
                }
            } else if (p != null && this.canBeActivated() && this.isActivated() && !p.isHandItem(this) && !p.isAttachedItem(this)) {
                this.setActivated(false);
                this.playDeactivateSound();
            } else if (p == null && this.canBeActivated() && this.isActivated()) {
                this.setActivated(false);
                this.playDeactivateSound();
            }
        }
        if (this.useWhileUnequiped && this.uses > 0 && (this.canBeActivated() && this.isActivated() || !this.canBeActivated())) {
            this.ticks += GameTime.instance.getMultiplier();
            while (this.ticks >= (float)this.ticksPerEquipUse) {
                this.ticks -= (float)this.ticksPerEquipUse;
                if (this.uses <= 0) continue;
                this.Use();
            }
        }
        if (this.getCurrentUses() <= 0 && this.getReplaceOnDeplete() == null && !this.isKeepOnDeplete() && this.container != null) {
            IsoObject minutes = this.container.parent;
            if (minutes instanceof IsoGameCharacter) {
                IsoGameCharacter chr = (IsoGameCharacter)minutes;
                chr.removeFromHands(this);
            }
            this.container.items.remove(this);
            this.container.setDirty(true);
            this.container.setDrawDirty(true);
            if (GameServer.server) {
                GameServer.sendRemoveItemFromContainer(this.container, this);
            }
            this.container = null;
        }
        if (this.getCurrentUses() <= 0 && this.getReplaceOnDeplete() != null) {
            String s = this.getReplaceOnDepleteFullType();
            if (this.container != null) {
                InventoryItem item = this.container.AddItem(s);
                IsoObject timeToCook = this.container.parent;
                if (timeToCook instanceof IsoGameCharacter) {
                    IsoGameCharacter chr = (IsoGameCharacter)timeToCook;
                    if (chr.getPrimaryHandItem() == this) {
                        chr.setPrimaryHandItem(item);
                    }
                    if (chr.getSecondaryHandItem() == this) {
                        chr.setSecondaryHandItem(item);
                    }
                }
                item.copyConditionStatesFrom(this);
                ItemContainer lastContainer = this.container;
                this.container.Remove(this);
                if (GameServer.server) {
                    GameServer.sendReplaceItemInContainer(lastContainer, this, item);
                }
            }
        }
        if (!GameServer.server && this.getWorldItem() != null && RainManager.isRaining().booleanValue() && this.is(ItemKey.Drainable.BATH_TOWEL, ItemKey.Drainable.DISH_CLOTH) && (sq = (worldItem = this.getWorldItem()).getSquare()).isOutside()) {
            float useDelta = this.getUseDelta();
            if (useDelta > 0.002f) {
                this.setUseDelta(useDelta - RainManager.getRainIntensity() / 1000.0f * GameTime.getInstance().getMultiplier());
            } else {
                Object item = InventoryItemFactory.CreateItem(this.getReplaceOnDeplete());
                sq.AddWorldInventoryItem((InventoryItem)item, worldItem.getOffX(), worldItem.getOffY(), worldItem.getOffZ());
                sq.transmitRemoveItemFromSquare(worldItem);
                ((InventoryItem)item).setWorldZRotation(this.getWorldZRotation());
            }
        }
    }

    @Override
    public void Use() {
        this.Use(false, false, false);
    }

    @Override
    public void Use(boolean bCrafting, boolean bInContainer, boolean bNeedSync) {
        if (this.getWorldItem() != null) {
            ItemUser.UseItem(this);
            if (GameServer.server && bNeedSync) {
                this.syncItemFields();
            }
            return;
        }
        --this.uses;
        if (this.uses <= 0) {
            if (this.getReplaceOnDeplete() != null) {
                InventoryItem item;
                String s = this.getReplaceOnDepleteFullType();
                if (this.container != null && (item = this.container.AddItem(s)) != null) {
                    IsoObject isoObject = this.container.parent;
                    if (isoObject instanceof IsoGameCharacter) {
                        IsoGameCharacter chr = (IsoGameCharacter)isoObject;
                        if (chr.getPrimaryHandItem() == this) {
                            chr.setPrimaryHandItem(item);
                        }
                        if (chr.getSecondaryHandItem() == this) {
                            chr.setSecondaryHandItem(item);
                        }
                    }
                    item.copyConditionStatesFrom(this);
                    ItemContainer lastContainer = this.container;
                    this.container.Remove(this);
                    if (GameServer.server) {
                        GameServer.sendReplaceItemInContainer(lastContainer, this, item);
                    }
                }
            } else {
                if (this.isKeepOnDeplete()) {
                    if (bNeedSync) {
                        this.syncItemFields();
                    }
                    return;
                }
                if (this.container != null && this.isDisappearOnUse()) {
                    IsoObject isoObject = this.container.parent;
                    if (isoObject instanceof IsoGameCharacter) {
                        IsoGameCharacter chr = (IsoGameCharacter)isoObject;
                        chr.removeFromHands(this);
                    }
                    this.container.items.remove(this);
                    this.container.setDirty(true);
                    this.container.setDrawDirty(true);
                    if (GameServer.server && bNeedSync) {
                        GameServer.sendRemoveItemFromContainer(this.container, this);
                    }
                    this.container = null;
                }
            }
        }
        this.updateWeight();
        if (bNeedSync) {
            this.syncItemFields();
        }
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

    public void updateWeight() {
        if (this.getReplaceOnDeplete() != null) {
            if (this.getCurrentUsesFloat() >= 1.0f) {
                this.setCustomWeight(true);
                this.setActualWeight(this.getScriptItem().getActualWeight());
                this.setWeight(this.getActualWeight());
                return;
            }
            Item emptyItem = ScriptManager.instance.getItem(this.replaceOnDepleteFullType);
            if (emptyItem != null) {
                this.setCustomWeight(true);
                this.setActualWeight((this.getScriptItem().getActualWeight() - emptyItem.getActualWeight()) * this.getCurrentUsesFloat() + emptyItem.getActualWeight());
                this.setWeight(this.getActualWeight());
            }
        }
        if (this.getWeightEmpty() != 0.0f) {
            this.setCustomWeight(true);
            this.setActualWeight((this.getScriptItem().getActualWeight() - this.weightEmpty) * this.getCurrentUsesFloat() + this.weightEmpty);
        }
    }

    public float getWeightEmpty() {
        return this.weightEmpty;
    }

    public void setWeightEmpty(float weight) {
        this.weightEmpty = weight;
    }

    public boolean isUseWhileEquiped() {
        return this.useWhileEquiped;
    }

    public void setUseWhileEquiped(boolean bUseWhileEquiped) {
        this.useWhileEquiped = bUseWhileEquiped;
    }

    public boolean isUseWhileUnequiped() {
        return this.useWhileUnequiped;
    }

    public void setUseWhileUnequiped(boolean bUseWhileUnequiped) {
        this.useWhileUnequiped = bUseWhileUnequiped;
    }

    public int getTicksPerEquipUse() {
        return this.ticksPerEquipUse;
    }

    public void setTicksPerEquipUse(int ticksPerEquipUse) {
        this.ticksPerEquipUse = ticksPerEquipUse;
    }

    @Override
    public float getUseDelta() {
        return this.useDelta;
    }

    @Override
    public void setUseDelta(float useDelta) {
        this.useDelta = useDelta;
    }

    public float getTicks() {
        return this.ticks;
    }

    public void setTicks(float ticks) {
        this.ticks = ticks;
    }

    public void setReplaceOnDeplete(String replaceOnDeplete) {
        this.replaceOnDeplete = replaceOnDeplete;
        this.replaceOnDepleteFullType = this.getReplaceOnDepleteFullType();
    }

    public String getReplaceOnDeplete() {
        return this.replaceOnDeplete;
    }

    public String getReplaceOnDepleteFullType() {
        return StringUtils.moduleDotType(this.getModule(), this.replaceOnDeplete);
    }

    public void setHeat(float heat) {
        this.heat = PZMath.clamp(heat, 0.0f, 3.0f);
    }

    public float getHeat() {
        return this.heat;
    }

    @Override
    public float getInvHeat() {
        return (1.0f - this.heat) / 3.0f;
    }

    @Override
    public boolean finishupdate() {
        if (this.container != null) {
            if (this.heat != this.container.getTemprature() || this.container.isTemperatureChanging()) {
                return false;
            }
            if (this.container.type.equals("campfire") || this.container.parent instanceof IsoBarbecue) {
                return false;
            }
        }
        return true;
    }

    public boolean canConsolidate() {
        return this.canConsolidate;
    }

    public void setCanConsolidate(boolean canConsolidate) {
        this.canConsolidate = canConsolidate;
    }

    public List<String> getReplaceOnCooked() {
        return this.replaceOnCooked;
    }

    public void setReplaceOnCooked(List<String> replaceOnCooked) {
        this.replaceOnCooked = replaceOnCooked;
    }

    public String getOnCooked() {
        return this.onCooked;
    }

    public void setOnCooked(String onCooked) {
        this.onCooked = onCooked;
    }

    public String getOnEat() {
        return this.onEat;
    }

    public void setOnEat(String onEat) {
        this.onEat = onEat;
    }

    public boolean isEnergy() {
        return this.getEnergy() != null;
    }

    public Energy getEnergy() {
        return null;
    }

    public boolean isFullUses() {
        return this.uses >= this.getMaxUses();
    }

    public boolean isEmptyUses() {
        return this.getCurrentUsesFloat() <= 0.0f;
    }

    public void randomizeUses() {
        if (this.getMaxUses() == 1) {
            return;
        }
        int amount = Rand.Next(this.getMaxUses()) + 1;
        if (this.hasTag(ItemTag.LESS_FULL)) {
            amount = Math.min(amount, Rand.Next(this.getMaxUses()) + 1);
        }
        if (amount > this.getMaxUses()) {
            return;
        }
        if (amount <= 0) {
            return;
        }
        this.setCurrentUses(amount);
    }
}

