/*
 * Decompiled with CFR 0.152.
 */
package zombie.iso.objects;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Objects;
import zombie.GameTime;
import zombie.Lua.LuaEventManager;
import zombie.SandboxOptions;
import zombie.UsedFromLua;
import zombie.WorldSoundManager;
import zombie.ai.states.ThumpState;
import zombie.audio.parameters.ParameterMeleeHitSurface;
import zombie.characters.IsoGameCharacter;
import zombie.characters.IsoPlayer;
import zombie.characters.IsoZombie;
import zombie.core.math.PZMath;
import zombie.core.properties.IsoPropertyType;
import zombie.inventory.InventoryItem;
import zombie.inventory.InventoryItemFactory;
import zombie.inventory.ItemContainer;
import zombie.inventory.types.Food;
import zombie.inventory.types.HandWeapon;
import zombie.iso.IHasHealth;
import zombie.iso.IsoCell;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoMovingObject;
import zombie.iso.IsoObject;
import zombie.iso.IsoWorld;
import zombie.iso.objects.interfaces.Thumpable;
import zombie.iso.sprite.IsoSprite;
import zombie.iso.sprite.IsoSpriteManager;
import zombie.iso.weather.ClimateManager;
import zombie.network.GameClient;
import zombie.network.GameServer;
import zombie.scripting.objects.ItemTag;
import zombie.util.Type;

@UsedFromLua
public class IsoCompost
extends IsoObject
implements Thumpable,
IHasHealth {
    private static final int MaximumThumpDamage = 8;
    private static final float MaximumCompost = 100.0f;
    private static final float NoWeaponCompostDamage = 50.0f;
    private static final int DefaultCapacity = 30;
    private float compost;
    private float lastUpdated = -1.0f;
    private int health = 100;
    private int maxHealth = 100;
    private float partialThumpDmg;

    public IsoCompost(IsoCell cell) {
        super(cell);
    }

    public IsoCompost(IsoCell cell, IsoGridSquare sq, String sprite) {
        this(cell, sq, IsoSpriteManager.instance.getSprite(sprite));
    }

    public IsoCompost(IsoCell cell, IsoGridSquare sq, IsoSprite sprite) {
        this.sprite = sprite;
        this.square = sq;
        this.container = new ItemContainer();
        this.container.setType("composter");
        this.container.setParent(this);
        this.container.explored = true;
        int capacity = PZMath.tryParseInt(this.sprite.getProperties().get(IsoPropertyType.CONTAINER_CAPACITY), 30);
        this.container.setCapacity(capacity);
    }

    @Override
    public void update() {
        InventoryItem item;
        int i;
        if (GameClient.client || this.container == null) {
            return;
        }
        float worldAgeHours = (float)GameTime.getInstance().getWorldAgeHours();
        if (this.lastUpdated < 0.0f) {
            this.lastUpdated = worldAgeHours;
        } else if (this.lastUpdated > worldAgeHours) {
            this.lastUpdated = worldAgeHours;
        }
        float elapsedHours = worldAgeHours - this.lastUpdated;
        if (elapsedHours <= 0.0f) {
            return;
        }
        this.lastUpdated = worldAgeHours;
        int compostHours = SandboxOptions.instance.getCompostHours();
        int wormCount = 0;
        for (i = 0; i < this.container.getItems().size(); ++i) {
            Food food;
            item = this.container.getItems().get(i);
            if (!(item instanceof Food) || !Objects.equals((food = (Food)item).getFullType(), "Base.Worm") || !food.isFresh()) continue;
            food.setAge(0.0f);
            ++wormCount;
        }
        for (i = 0; i < this.container.getItems().size(); ++i) {
            boolean tooCold;
            item = this.container.getItems().get(i);
            boolean isCompostable = item.hasTag(ItemTag.IS_COMPOSTABLE);
            if (!(item instanceof Food)) continue;
            Food food = (Food)item;
            if (item.hasTag(ItemTag.CANT_COMPOST)) continue;
            if (!(!GameServer.server || Objects.equals(food.getFullType(), "Base.Worm") && food.isFresh())) {
                food.updateAge();
            }
            if (!food.isRotten() && !isCompostable) continue;
            if (this.getCompost() < 100.0f) {
                food.setRottenTime(0.0f);
                food.setCompostTime(food.getCompostTime() + elapsedHours);
            }
            if (!(food.getCompostTime() >= (float)compostHours)) continue;
            float compostValue = Math.abs(food.getHungChange()) * 2.0f;
            if (compostValue == 0.0f) {
                compostValue = Math.abs(food.getWeight()) * 10.0f;
            }
            this.setCompost(this.getCompost() + compostValue);
            if (this.getCompost() > 100.0f) {
                this.setCompost(100.0f);
            }
            if (GameServer.server) {
                GameServer.sendCompost(this, null);
                GameServer.sendRemoveItemFromContainer(this.container, item);
            }
            boolean bl = tooCold = "Winter".equals(ClimateManager.getInstance().getSeasonName()) && this.isOutside();
            if (wormCount >= 2 && !tooCold) {
                Object worm = InventoryItemFactory.CreateItem("Base.Worm");
                this.container.AddItem((InventoryItem)worm);
                if (GameServer.server && worm != null) {
                    GameServer.sendAddItemToContainer(this.container, worm);
                }
            }
            item.setCurrentUses(1);
            item.Use();
            IsoWorld.instance.currentCell.addToProcessItemsRemove(item);
        }
        this.updateSprite();
    }

    public void updateSprite() {
        if (this.getCompost() >= 10.0f && this.sprite.getName().equals("camping_01_19")) {
            this.sprite = IsoSpriteManager.instance.getSprite("camping_01_20");
            this.transmitUpdatedSpriteToClients();
        } else if (this.getCompost() < 10.0f && this.sprite.getName().equals("camping_01_20")) {
            this.sprite = IsoSpriteManager.instance.getSprite("camping_01_19");
            this.transmitUpdatedSpriteToClients();
        } else if (this.getCompost() >= 10.0f && this.sprite.getName().equals("carpentry_02_116")) {
            this.sprite = IsoSpriteManager.instance.getSprite("carpentry_02_117");
            this.transmitUpdatedSpriteToClients();
        } else if (this.getCompost() < 10.0f && this.sprite.getName().equals("carpentry_02_117")) {
            this.sprite = IsoSpriteManager.instance.getSprite("carpentry_02_116");
            this.transmitUpdatedSpriteToClients();
        }
    }

    public void syncCompost() {
        if (GameClient.client) {
            GameClient.sendCompost(this);
        } else if (GameServer.server) {
            GameServer.sendCompost(this, null);
        }
    }

    @Override
    public void sync() {
        this.syncCompost();
    }

    @Override
    public void load(ByteBuffer input, int worldVersion, boolean isDebugSave) throws IOException {
        super.load(input, worldVersion, isDebugSave);
        if (this.container != null) {
            this.container.setType("composter");
        }
        this.compost = input.getFloat();
        this.lastUpdated = input.getFloat();
        if (worldVersion >= 213) {
            this.health = input.getInt();
            this.maxHealth = input.getInt();
        }
    }

    @Override
    public void save(ByteBuffer output, boolean isDebugSave) throws IOException {
        super.save(output, isDebugSave);
        output.putFloat(this.compost);
        output.putFloat(this.lastUpdated);
        output.putInt(this.health);
        output.putInt(this.maxHealth);
    }

    @Override
    public String getObjectName() {
        return "IsoCompost";
    }

    public float getCompost() {
        return this.compost;
    }

    public void setCompost(float compost) {
        this.compost = PZMath.clamp(compost, 0.0f, 100.0f);
    }

    public void remove() {
        if (this.getSquare() == null) {
            return;
        }
        this.getSquare().transmitRemoveItemFromSquare(this);
    }

    @Override
    public void addToWorld() {
        this.getCell().addToProcessIsoObject(this);
    }

    @Override
    public Thumpable getThumpableFor(IsoGameCharacter chr) {
        if (this.isDestroyed()) {
            return null;
        }
        return this;
    }

    @Override
    public void setHealth(int health) {
        this.health = health;
    }

    @Override
    public int getHealth() {
        return this.health;
    }

    public void setMaxHealth(int maxHealth) {
        this.maxHealth = maxHealth;
    }

    @Override
    public int getMaxHealth() {
        return this.maxHealth;
    }

    private void dropContainedItems() {
        ArrayList<InventoryItem> items = new ArrayList<InventoryItem>();
        for (int i = 0; i < this.getContainerCount(); ++i) {
            ItemContainer container = this.getContainerByIndex(i);
            items.clear();
            items.addAll(container.getItems());
            container.removeItemsFromProcessItems();
            container.removeAllItems();
            for (int j = 0; j < items.size(); ++j) {
                this.getSquare().AddWorldInventoryItem((InventoryItem)items.get(j), 0.0f, 0.0f, 0.0f);
            }
        }
    }

    @Override
    public void Thump(IsoMovingObject thumper) {
        IsoGameCharacter isoGameCharacter;
        Thumpable thumpable;
        if (!SandboxOptions.instance.lore.thumpOnConstruction.getValue()) {
            return;
        }
        if (thumper instanceof IsoGameCharacter && (thumpable = this.getThumpableFor(isoGameCharacter = (IsoGameCharacter)thumper)) == null) {
            return;
        }
        if (thumper instanceof IsoZombie) {
            int totalThumpers = thumper.getSurroundingThumpers();
            int max = 8;
            if (totalThumpers >= 8) {
                int amount = 1 * ThumpState.getFastForwardDamageMultiplier();
                this.health -= amount;
            } else {
                this.partialThumpDmg += (float)totalThumpers / 8.0f * (float)ThumpState.getFastForwardDamageMultiplier();
                if ((float)((int)this.partialThumpDmg) > 0.0f) {
                    int amount = (int)this.partialThumpDmg;
                    this.health -= amount;
                    this.partialThumpDmg -= (float)amount;
                }
            }
            WorldSoundManager.instance.addSound(thumper, this.square.getX(), this.square.getY(), this.square.getZ(), 20, 20, true, 4.0f, 15.0f);
        }
        if (this.isDestroyed()) {
            String breakSound = "BreakObject";
            ((IsoGameCharacter)thumper).getEmitter().playSound("BreakObject", this);
            if (GameServer.server) {
                GameServer.PlayWorldSoundServer((IsoGameCharacter)thumper, "BreakObject", false, thumper.getCurrentSquare(), 0.2f, 20.0f, 1.1f, true);
            }
            WorldSoundManager.instance.addSound(null, this.square.getX(), this.square.getY(), this.square.getZ(), 10, 20, true, 4.0f, 15.0f);
            thumper.setThumpTarget(null);
            if (this.getObjectIndex() != -1) {
                this.addItemsFromProperties();
                this.dropContainedItems();
                this.square.transmitRemoveItemFromSquare(this);
            }
        }
    }

    @Override
    public void WeaponHit(IsoGameCharacter owner, HandWeapon weapon) {
        if (this.isDestroyed()) {
            return;
        }
        IsoPlayer player = Type.tryCastTo(owner, IsoPlayer.class);
        if (GameClient.client) {
            return;
        }
        LuaEventManager.triggerEvent("OnWeaponHitThumpable", owner, weapon, this);
        if (player != null) {
            player.setMeleeHitSurface(ParameterMeleeHitSurface.Material.Wood);
        }
        owner.getEmitter().playSound(weapon.getDoorHitSound(), this);
        if (GameServer.server) {
            GameServer.PlayWorldSoundServer(owner, weapon.getDoorHitSound(), false, this.getSquare(), 1.0f, 20.0f, 2.0f, false);
        }
        if (weapon != null) {
            this.Damage(weapon.getDoorDamage());
        } else {
            this.Damage(50.0f);
        }
        WorldSoundManager.instance.addSound(owner, this.square.getX(), this.square.getY(), this.square.getZ(), 20, 20, false, 0.0f, 15.0f);
        if (this.isDestroyed()) {
            if (owner != null) {
                String sound2 = "BreakObject";
                owner.getEmitter().playSound("BreakObject");
                if (GameServer.server) {
                    GameServer.PlayWorldSoundServer("BreakObject", false, owner.getCurrentSquare(), 0.2f, 20.0f, 1.1f, true);
                }
            }
            this.addItemsFromProperties();
            this.dropContainedItems();
            this.square.transmitRemoveItemFromSquare(this);
            if (!GameServer.server) {
                this.square.RemoveTileObject(this);
            }
        }
    }

    @Override
    public void Damage(float amount) {
        this.DirtySlice();
        this.health = (int)((float)this.health - amount);
    }

    @Override
    public boolean isDestroyed() {
        return this.health <= 0;
    }

    @Override
    public float getThumpCondition() {
        if (this.getMaxHealth() <= 0) {
            return 0.0f;
        }
        return (float)PZMath.clamp(this.getHealth(), 0, this.getMaxHealth()) / (float)this.getMaxHealth();
    }
}

