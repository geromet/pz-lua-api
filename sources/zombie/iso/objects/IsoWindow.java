/*
 * Decompiled with CFR 0.152.
 */
package zombie.iso.objects;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import zombie.AmbientStreamManager;
import zombie.Lua.LuaEventManager;
import zombie.SandboxOptions;
import zombie.SoundManager;
import zombie.UsedFromLua;
import zombie.WorldSoundManager;
import zombie.ai.states.ThumpState;
import zombie.audio.parameters.ParameterMeleeHitSurface;
import zombie.characters.IsoGameCharacter;
import zombie.characters.IsoLivingCharacter;
import zombie.characters.IsoPlayer;
import zombie.characters.IsoZombie;
import zombie.characters.animals.IsoAnimal;
import zombie.core.Core;
import zombie.core.math.PZMath;
import zombie.core.network.ByteBufferReader;
import zombie.core.network.ByteBufferWriter;
import zombie.core.properties.IsoObjectChange;
import zombie.core.properties.IsoPropertyType;
import zombie.core.properties.PropertyContainer;
import zombie.core.random.Rand;
import zombie.inventory.InventoryItem;
import zombie.inventory.types.HandWeapon;
import zombie.iso.IsoCell;
import zombie.iso.IsoDirections;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoMovingObject;
import zombie.iso.IsoObject;
import zombie.iso.IsoWorld;
import zombie.iso.RoomDef;
import zombie.iso.SpriteDetails.IsoFlagType;
import zombie.iso.SpriteDetails.IsoObjectType;
import zombie.iso.Vector2;
import zombie.iso.areas.IsoRoom;
import zombie.iso.areas.SafeHouse;
import zombie.iso.objects.IsoBarricade;
import zombie.iso.objects.IsoCurtain;
import zombie.iso.objects.interfaces.BarricadeAble;
import zombie.iso.objects.interfaces.Thumpable;
import zombie.iso.sprite.IsoSprite;
import zombie.iso.sprite.IsoSpriteManager;
import zombie.network.GameClient;
import zombie.network.GameServer;
import zombie.network.ServerOptions;
import zombie.pathfind.PolygonalMap2;
import zombie.util.Type;

@UsedFromLua
public class IsoWindow
extends IsoObject
implements BarricadeAble,
Thumpable {
    private static final int SinglePaneWindowMaxHealth = 50;
    private static final int DoublePaneWindowMaxHealth = 100;
    public static final float WeaponDoorDamageModifier = 5.0f;
    public static final float NoWeaponDoorDamage = 100.0f;
    private final WindowType type = WindowType.SinglePane;
    private int health = 50;
    private int maxHealth = 50;
    private boolean north;
    private boolean locked;
    private boolean permaLocked;
    private boolean open;
    private boolean destroyed;
    private boolean glassRemoved;
    private IsoSprite openSprite;
    private IsoSprite closedSprite;
    private IsoSprite smashedSprite;
    private IsoSprite glassRemovedSprite;

    public IsoWindow(IsoCell cell) {
        super(cell);
    }

    public IsoWindow(IsoCell cell, IsoGridSquare gridSquare, IsoSprite gid, boolean north) {
        gid.getProperties().unset(IsoFlagType.cutN);
        gid.getProperties().unset(IsoFlagType.cutW);
        int openOffset = 0;
        if (gid.getProperties().has(IsoPropertyType.OPEN_TILE_OFFSET)) {
            openOffset = Integer.parseInt(gid.getProperties().get(IsoPropertyType.OPEN_TILE_OFFSET));
        }
        this.permaLocked = gid.getProperties().has(IsoPropertyType.WINDOW_LOCKED);
        int smashedOffset = 0;
        if (gid.getProperties().has(IsoPropertyType.SMASHED_TILE_OFFSET)) {
            smashedOffset = Integer.parseInt(gid.getProperties().get(IsoPropertyType.SMASHED_TILE_OFFSET));
        }
        this.closedSprite = gid;
        if (north) {
            this.closedSprite.getProperties().set(IsoFlagType.cutN);
            this.closedSprite.getProperties().set(IsoFlagType.windowN);
        } else {
            this.closedSprite.getProperties().set(IsoFlagType.cutW);
            this.closedSprite.getProperties().set(IsoFlagType.windowW);
        }
        this.openSprite = IsoSprite.getSprite(IsoSpriteManager.instance, gid, openOffset);
        this.smashedSprite = IsoSprite.getSprite(IsoSpriteManager.instance, gid, smashedOffset);
        if (this.closedSprite.getProperties().has(IsoPropertyType.GLASS_REMOVED_OFFSET)) {
            int glassRemovedOffset = Integer.parseInt(this.closedSprite.getProperties().get(IsoPropertyType.GLASS_REMOVED_OFFSET));
            this.glassRemovedSprite = IsoSprite.getSprite(IsoSpriteManager.instance, this.closedSprite, glassRemovedOffset);
        } else {
            this.glassRemovedSprite = this.smashedSprite;
        }
        if (this.smashedSprite != this.closedSprite && this.smashedSprite != null) {
            this.smashedSprite.AddProperties(this.closedSprite);
            this.smashedSprite.setTileType(this.closedSprite.getTileType());
        }
        if (this.openSprite != this.closedSprite && this.openSprite != null) {
            this.openSprite.AddProperties(this.closedSprite);
            this.openSprite.setTileType(this.closedSprite.getTileType());
        }
        if (this.glassRemovedSprite != this.closedSprite && this.glassRemovedSprite != null) {
            this.glassRemovedSprite.AddProperties(this.closedSprite);
            this.glassRemovedSprite.setTileType(this.closedSprite.getTileType());
        }
        this.sprite = this.closedSprite;
        IsoObject wall = gridSquare.getWall(north);
        if (wall != null) {
            wall.rerouteCollide = this;
        }
        this.square = gridSquare;
        this.north = north;
        switch (this.type.ordinal()) {
            case 0: {
                this.health = 50;
                this.maxHealth = 50;
                break;
            }
            case 1: {
                this.health = 100;
                this.maxHealth = 100;
            }
        }
        LockedHouseFrequency frequency = LockedHouseFrequency.fromValue(SandboxOptions.instance.lockedHouses.getValue());
        int randLock = frequency.getLockChance();
        if (randLock > LockedHouseFrequency.Never.lockChance) {
            this.locked = Rand.Next(100) < randLock;
        }
    }

    @Override
    public String getObjectName() {
        return "Window";
    }

    public IsoCurtain HasCurtains() {
        IsoCurtain curtain;
        IsoGridSquare toCheck = this.getOppositeSquare();
        if (toCheck != null && (curtain = toCheck.getCurtain(this.getNorth() ? IsoObjectType.curtainS : IsoObjectType.curtainE)) != null) {
            return curtain;
        }
        return this.getSquare().getCurtain(this.getNorth() ? IsoObjectType.curtainN : IsoObjectType.curtainW);
    }

    public IsoGridSquare getIndoorSquare() {
        if (this.square.getRoom() != null) {
            return this.square;
        }
        IsoGridSquare sq = this.north ? IsoWorld.instance.currentCell.getGridSquare(this.square.getX(), this.square.getY() - 1, this.square.getZ()) : IsoWorld.instance.currentCell.getGridSquare(this.square.getX() - 1, this.square.getY(), this.square.getZ());
        if (sq != null && sq.getRoom() != null) {
            return sq;
        }
        return null;
    }

    public IsoGridSquare getAddSheetSquare(IsoGameCharacter chr) {
        if (chr == null || chr.getCurrentSquare() == null) {
            return null;
        }
        IsoGridSquare sqChr = chr.getCurrentSquare();
        IsoGridSquare sqThis = this.getSquare();
        if (this.north) {
            if (sqChr.getY() < sqThis.getY()) {
                return this.getCell().getGridSquare(sqThis.x, sqThis.y - 1, sqThis.z);
            }
        } else if (sqChr.getX() < sqThis.getX()) {
            return this.getCell().getGridSquare(sqThis.x - 1, sqThis.y, sqThis.z);
        }
        return sqThis;
    }

    @Override
    public void AttackObject(IsoGameCharacter owner) {
        super.AttackObject(owner);
        IsoObject o = this.square.getWall(this.north);
        if (o != null) {
            o.AttackObject(owner);
        }
    }

    public IsoGridSquare getInsideSquare() {
        if (this.square == null) {
            return null;
        }
        if (this.north) {
            return this.getCell().getGridSquare(this.square.getX(), this.square.getY() - 1, this.square.getZ());
        }
        return this.getCell().getGridSquare(this.square.getX() - 1, this.square.getY(), this.square.getZ());
    }

    @Override
    public IsoGridSquare getOppositeSquare() {
        return this.getInsideSquare();
    }

    public boolean isExterior() {
        IsoGridSquare sq = this.getSquare();
        IsoGridSquare sqOpposite = this.getOppositeSquare();
        if (sqOpposite == null) {
            return false;
        }
        return sq.isInARoom() != sqOpposite.isInARoom();
    }

    @Override
    public void WeaponHit(IsoGameCharacter owner, HandWeapon weapon) {
        IsoPlayer player = Type.tryCastTo(owner, IsoPlayer.class);
        Thumpable thumpable = this.getThumpableFor(owner);
        if (GameClient.client) {
            if (player != null && weapon != ((IsoLivingCharacter)owner).bareHands && !this.isInvincible() && !(thumpable instanceof IsoBarricade)) {
                this.health = 0;
            }
            return;
        }
        if (thumpable == null) {
            return;
        }
        if (thumpable instanceof IsoBarricade) {
            thumpable.WeaponHit(owner, weapon);
            return;
        }
        LuaEventManager.triggerEvent("OnWeaponHitThumpable", owner, weapon, this);
        if (weapon == ((IsoLivingCharacter)owner).bareHands) {
            if (player != null) {
                player.setMeleeHitSurface(ParameterMeleeHitSurface.Material.Glass);
                player.getEmitter().playSound(weapon.getDoorHitSound(), this);
            }
            return;
        }
        if (weapon != null) {
            this.damage((float)weapon.getDoorDamage() * 5.0f, owner);
        } else {
            this.damage(100.0f, owner);
        }
        this.DirtySlice();
        if (weapon != null && weapon.getDoorHitSound() != null) {
            if (player != null) {
                player.setMeleeHitSurface(ParameterMeleeHitSurface.Material.Glass);
            }
            owner.getEmitter().playSound(weapon.getDoorHitSound(), this);
            if (GameServer.server) {
                GameServer.PlayWorldSoundServer(owner, weapon.getDoorHitSound(), false, this.getSquare(), 1.0f, 20.0f, 2.0f, false);
            }
        }
        WorldSoundManager.instance.addSound(owner, this.square.getX(), this.square.getY(), this.square.getZ(), 20, 20, false, 0.0f, 15.0f);
        if (player != null && weapon != ((IsoLivingCharacter)owner).bareHands && !this.isInvincible()) {
            this.health = 0;
        }
        if (!this.isDestroyed() && this.health <= 0) {
            this.smashWindow();
            this.addBrokenGlass(owner);
        }
    }

    public void smashWindow(boolean bRemote, boolean doAlarm) {
        if (this.destroyed) {
            return;
        }
        if (GameClient.client && !bRemote) {
            GameClient.instance.smashWindow(this);
        }
        if (!bRemote) {
            if (GameServer.server) {
                GameServer.PlayWorldSoundServer("SmashWindow", false, this.square, 0.2f, 20.0f, 1.1f, true);
            } else {
                SoundManager.instance.PlayWorldSound("SmashWindow", this.square, 0.2f, 20.0f, 1.0f, true);
            }
            WorldSoundManager.instance.addSound(null, this.square.getX(), this.square.getY(), this.square.getZ(), 10, 20, true, 4.0f, 15.0f);
        }
        this.destroyed = true;
        this.sprite = this.smashedSprite;
        if (this.getAttachedAnimSprite() != null) {
            this.getSquare().removeBlood(false, true);
            for (int i = 0; i < this.getAttachedAnimSprite().size(); ++i) {
                IsoSprite sprite = this.getAttachedAnimSprite().get((int)i).parentSprite;
                if (sprite == null || !sprite.getProperties().has("AttachedToGlass")) continue;
                this.getAttachedAnimSprite().remove(i);
                --i;
            }
        }
        this.getSquare().removeLightSwitch();
        if (doAlarm) {
            this.handleAlarm();
        }
        if (GameServer.server && !bRemote) {
            GameServer.smashWindow(this);
        }
        this.square.InvalidateSpecialObjectPaths();
        PolygonalMap2.instance.squareChanged(this.square);
    }

    public void smashWindow(boolean bRemote) {
        this.smashWindow(bRemote, true);
    }

    public void smashWindow() {
        this.smashWindow(false, true);
    }

    public void addBrokenGlass(IsoMovingObject chr) {
        if (chr == null) {
            return;
        }
        if (this.getSquare() == null) {
            return;
        }
        if (this.getNorth()) {
            this.addBrokenGlass(chr.getY() >= (float)this.getSquare().getY());
        } else {
            this.addBrokenGlass(chr.getX() >= (float)this.getSquare().getX());
        }
    }

    public void addBrokenGlass(boolean onOppositeSquare) {
        IsoGridSquare square1;
        IsoGridSquare isoGridSquare = square1 = onOppositeSquare ? this.getOppositeSquare() : this.getSquare();
        if (square1 != null) {
            square1.addBrokenGlass();
        }
    }

    private void handleAlarm() {
        if (GameClient.client) {
            return;
        }
        IsoGridSquare sq = this.getIndoorSquare();
        if (sq == null) {
            return;
        }
        IsoRoom r = sq.getRoom();
        RoomDef def = r.def;
        if (def.building.alarmed && !GameClient.client) {
            AmbientStreamManager.instance.doAlarm(def);
        }
    }

    @Override
    public boolean isDestroyed() {
        return this.destroyed;
    }

    @Override
    public boolean TestCollide(IsoMovingObject obj, IsoGridSquare from, IsoGridSquare to) {
        if (from == this.square) {
            if (this.north && to.getY() < from.getY()) {
                if (obj != null) {
                    obj.collideWith(this);
                }
                return true;
            }
            if (!this.north && to.getX() < from.getX()) {
                if (obj != null) {
                    obj.collideWith(this);
                }
                return true;
            }
        } else {
            if (this.north && to.getY() > from.getY()) {
                if (obj != null) {
                    obj.collideWith(this);
                }
                return true;
            }
            if (!this.north && to.getX() > from.getX()) {
                if (obj != null) {
                    obj.collideWith(this);
                }
                return true;
            }
        }
        return false;
    }

    @Override
    public IsoObject.VisionResult TestVision(IsoGridSquare from, IsoGridSquare to) {
        if (to.getZ() != from.getZ()) {
            return IsoObject.VisionResult.NoEffect;
        }
        if (from == this.square) {
            if (this.north && to.getY() < from.getY()) {
                return IsoObject.VisionResult.Unblocked;
            }
            if (!this.north && to.getX() < from.getX()) {
                return IsoObject.VisionResult.Unblocked;
            }
        } else {
            if (this.north && to.getY() > from.getY()) {
                return IsoObject.VisionResult.Unblocked;
            }
            if (!this.north && to.getX() > from.getX()) {
                return IsoObject.VisionResult.Unblocked;
            }
        }
        return IsoObject.VisionResult.NoEffect;
    }

    @Override
    public void Thump(IsoMovingObject thumper) {
        IsoZombie isoZombie;
        if (thumper instanceof IsoGameCharacter) {
            IsoGameCharacter isoGameCharacter = (IsoGameCharacter)thumper;
            Thumpable thumpable = this.getThumpableFor(isoGameCharacter);
            if (thumpable == null) {
                return;
            }
            if (thumpable != this) {
                thumpable.Thump(thumper);
                return;
            }
        }
        if (thumper instanceof IsoZombie) {
            isoZombie = (IsoZombie)thumper;
            if (!(isoZombie.cognition != 1 || this.canClimbThrough(isoZombie) || this.isInvincible() || this.locked && (thumper.getCurrentSquare() == null || thumper.getCurrentSquare().has(IsoFlagType.exterior)))) {
                this.ToggleWindow((IsoGameCharacter)thumper);
                if (this.canClimbThrough(isoZombie)) {
                    return;
                }
            }
            int mult = ThumpState.getFastForwardDamageMultiplier();
            this.DirtySlice();
            this.damage(isoZombie.strength * mult, thumper);
            WorldSoundManager.instance.addSound(thumper, this.square.getX(), this.square.getY(), this.square.getZ(), 20, 20, true, 4.0f, 15.0f);
        }
        if (!this.isDestroyed() && this.health <= 0) {
            if (this.getSquare().getBuilding() != null) {
                this.getSquare().getBuilding().forceAwake();
            }
            if (GameServer.server) {
                GameServer.smashWindow(this);
                GameServer.PlayWorldSoundServer((IsoGameCharacter)thumper, "SmashWindow", false, thumper.getCurrentSquare(), 0.2f, 20.0f, 1.1f, true);
            }
            ((IsoGameCharacter)thumper).getEmitter().playSound("SmashWindow", this);
            WorldSoundManager.instance.addSound(null, this.square.getX(), this.square.getY(), this.square.getZ(), 10, 20, true, 4.0f, 15.0f);
            thumper.setThumpTarget(null);
            this.destroyed = true;
            this.sprite = this.smashedSprite;
            this.square.InvalidateSpecialObjectPaths();
            this.addBrokenGlass(thumper);
            if (thumper instanceof IsoZombie && this.getThumpableFor(isoZombie = (IsoZombie)thumper) != null) {
                thumper.setThumpTarget(this.getThumpableFor(isoZombie));
            }
        }
    }

    @Override
    public Thumpable getThumpableFor(IsoGameCharacter chr) {
        IsoBarricade barricade = this.getBarricadeForCharacter(chr);
        if (barricade != null) {
            return barricade;
        }
        if (this.isDestroyed() || this.IsOpen()) {
            barricade = this.getBarricadeOppositeCharacter(chr);
            return barricade;
        }
        return this;
    }

    @Override
    public float getThumpCondition() {
        return (float)PZMath.clamp(this.health, 0, this.maxHealth) / (float)this.maxHealth;
    }

    @Override
    public void load(ByteBuffer input, int worldVersion, boolean isDebugSave) throws IOException {
        super.load(input, worldVersion, isDebugSave);
        this.open = input.get() != 0;
        this.north = input.get() != 0;
        this.health = input.getInt();
        this.locked = input.get() != 0;
        this.permaLocked = input.get() != 0;
        this.destroyed = input.get() != 0;
        boolean bl = this.glassRemoved = input.get() != 0;
        if (input.get() != 0) {
            this.openSprite = IsoSprite.getSprite(IsoSpriteManager.instance, input.getInt());
        }
        if (input.get() != 0) {
            this.closedSprite = IsoSprite.getSprite(IsoSpriteManager.instance, input.getInt());
        }
        if (input.get() != 0) {
            this.smashedSprite = IsoSprite.getSprite(IsoSpriteManager.instance, input.getInt());
        }
        if (input.get() != 0) {
            this.glassRemovedSprite = IsoSprite.getSprite(IsoSpriteManager.instance, input.getInt());
        }
        this.maxHealth = input.getInt();
        if (this.closedSprite != null) {
            if (this.north) {
                this.closedSprite.getProperties().set(IsoFlagType.cutN);
                this.closedSprite.getProperties().set(IsoFlagType.windowN);
            } else {
                this.closedSprite.getProperties().set(IsoFlagType.cutW);
                this.closedSprite.getProperties().set(IsoFlagType.windowW);
            }
            if (this.smashedSprite != this.closedSprite && this.smashedSprite != null) {
                this.smashedSprite.AddProperties(this.closedSprite);
                this.smashedSprite.setTileType(this.closedSprite.getTileType());
            }
            if (this.openSprite != this.closedSprite && this.openSprite != null) {
                this.openSprite.AddProperties(this.closedSprite);
                this.openSprite.setTileType(this.closedSprite.getTileType());
            }
            if (this.glassRemovedSprite != this.closedSprite && this.glassRemovedSprite != null) {
                this.glassRemovedSprite.AddProperties(this.closedSprite);
                this.glassRemovedSprite.setTileType(this.closedSprite.getTileType());
            }
        }
    }

    @Override
    public void addToWorld() {
        super.addToWorld();
        this.getCell().addToWindowList(this);
    }

    @Override
    public void removeFromWorld() {
        super.removeFromWorld();
        this.getCell().removeFromWindowList(this);
    }

    @Override
    public void save(ByteBuffer output, boolean isDebugSave) throws IOException {
        super.save(output, isDebugSave);
        output.put(this.open ? (byte)1 : 0);
        output.put(this.north ? (byte)1 : 0);
        output.putInt(this.health);
        output.put(this.locked ? (byte)1 : 0);
        output.put(this.permaLocked ? (byte)1 : 0);
        output.put(this.destroyed ? (byte)1 : 0);
        output.put(this.glassRemoved ? (byte)1 : 0);
        if (this.openSprite != null) {
            output.put((byte)1);
            output.putInt(this.openSprite.id);
        } else {
            output.put((byte)0);
        }
        if (this.closedSprite != null) {
            output.put((byte)1);
            output.putInt(this.closedSprite.id);
        } else {
            output.put((byte)0);
        }
        if (this.smashedSprite != null) {
            output.put((byte)1);
            output.putInt(this.smashedSprite.id);
        } else {
            output.put((byte)0);
        }
        if (this.glassRemovedSprite != null) {
            output.put((byte)1);
            output.putInt(this.glassRemovedSprite.id);
        } else {
            output.put((byte)0);
        }
        output.putInt(this.maxHealth);
    }

    @Override
    public void saveState(ByteBuffer bb) throws IOException {
        bb.put((byte)(this.locked ? 1 : 0));
    }

    @Override
    public void loadState(ByteBuffer bb) throws IOException {
        boolean locked;
        boolean bl = locked = bb.get() != 0;
        if (locked != this.locked) {
            this.locked = locked;
        }
    }

    public void openCloseCurtain(IsoGameCharacter chr) {
        if (chr == IsoPlayer.getInstance()) {
            IsoGridSquare s = this.square;
            if (this.north) {
                if (s.getRoom() == null) {
                    s = this.getCell().getGridSquare(s.getX(), s.getY() - 1, s.getZ());
                }
            } else if (s.getRoom() == null) {
                s = this.getCell().getGridSquare(s.getX() - 1, s.getY(), s.getZ());
            }
            if (s != null) {
                for (int n = 0; n < s.getSpecialObjects().size(); ++n) {
                    if (!(s.getSpecialObjects().get(n) instanceof IsoCurtain)) continue;
                    ((IsoCurtain)s.getSpecialObjects().get(n)).ToggleDoorSilent();
                    return;
                }
            }
        }
    }

    public void removeSheet(IsoGameCharacter chr) {
        IsoGridSquare sq;
        if (this.north) {
            s = this.square;
            if (s.getRoom() == null) {
                s = this.getCell().getGridSquare(s.getX(), s.getY() - 1, s.getZ());
            }
            sq = s;
        } else {
            s = this.square;
            if (s.getRoom() == null) {
                s = this.getCell().getGridSquare(s.getX() - 1, s.getY(), s.getZ());
            }
            sq = s;
        }
        for (int n = 0; n < sq.getSpecialObjects().size(); ++n) {
            IsoObject o = sq.getSpecialObjects().get(n);
            if (!(o instanceof IsoCurtain)) continue;
            sq.transmitRemoveItemFromSquare(o);
            if (chr == null) break;
            InventoryItem item = chr.getInventory().AddItem(o.getName());
            if (!GameServer.server) break;
            GameServer.sendAddItemToContainer(chr.getInventory(), item);
            break;
        }
    }

    public void addSheet(IsoGameCharacter chr) {
        IsoGridSquare sq;
        IsoObjectType curtainType;
        IsoGridSquare s = this.square;
        if (this.north) {
            curtainType = IsoObjectType.curtainN;
            if (chr != null) {
                if (chr.getY() < this.getY()) {
                    s = this.getCell().getGridSquare(s.getX(), s.getY() - 1, s.getZ());
                    curtainType = IsoObjectType.curtainS;
                }
            } else if (s.getRoom() == null) {
                s = this.getCell().getGridSquare(s.getX(), s.getY() - 1, s.getZ());
                curtainType = IsoObjectType.curtainS;
            }
        } else {
            curtainType = IsoObjectType.curtainW;
            if (chr != null) {
                if (chr.getX() < this.getX()) {
                    s = this.getCell().getGridSquare(s.getX() - 1, s.getY(), s.getZ());
                    curtainType = IsoObjectType.curtainE;
                }
            } else if (s.getRoom() == null) {
                s = this.getCell().getGridSquare(s.getX() - 1, s.getY(), s.getZ());
                curtainType = IsoObjectType.curtainE;
            }
        }
        if ((sq = s).getCurtain(curtainType) != null) {
            return;
        }
        int gid = 16;
        if (curtainType == IsoObjectType.curtainE) {
            ++gid;
        }
        if (curtainType == IsoObjectType.curtainS) {
            gid += 3;
        }
        if (curtainType == IsoObjectType.curtainN) {
            gid += 2;
        }
        IsoCurtain c = new IsoCurtain(this.getCell(), sq, "fixtures_windows_curtains_01_" + (gid += 4), this.north);
        sq.AddSpecialTileObject(c);
        if (!c.open) {
            c.ToggleDoorSilent();
        }
        if (!GameClient.client && chr != null) {
            InventoryItem item = chr.getInventory().FindAndReturn("Sheet");
            chr.getInventory().Remove(item);
            if (GameServer.server) {
                GameServer.sendRemoveItemFromContainer(chr.getInventory(), item);
            }
        }
        if (GameServer.server) {
            c.transmitCompleteItemToClients();
        }
    }

    public void ToggleWindow(IsoGameCharacter chr) {
        IsoPlayer player = Type.tryCastTo(chr, IsoPlayer.class);
        this.DirtySlice();
        IsoGridSquare.setRecalcLightTime(-1.0f);
        if (this.permaLocked) {
            return;
        }
        if (this.destroyed) {
            return;
        }
        if (chr != null && this.getBarricadeForCharacter(chr) != null) {
            return;
        }
        this.locked = false;
        this.open = !this.open;
        this.sprite = this.closedSprite;
        this.square.InvalidateSpecialObjectPaths();
        if (this.open) {
            if (!(chr instanceof IsoZombie) || SandboxOptions.getInstance().lore.triggerHouseAlarm.getValue()) {
                this.handleAlarm();
            }
            this.sprite = this.openSprite;
        }
        this.square.RecalcProperties();
        this.sync(this.open ? 1 : 0);
        PolygonalMap2.instance.squareChanged(this.square);
        LuaEventManager.triggerEvent("OnContainerUpdate");
        if (player != null && player.isLocalPlayer()) {
            player.triggerMusicIntensityEvent(this.open ? "WindowOpen" : "WindowClose");
        }
    }

    @Override
    public void syncIsoObjectSend(ByteBufferWriter b) {
        b.putInt(this.square.getX());
        b.putInt(this.square.getY());
        b.putInt(this.square.getZ());
        b.putByte(this.getObjectIndex());
        b.putBoolean(true);
        b.putBoolean(false);
        b.putBoolean(this.open);
        b.putBoolean(this.destroyed);
        b.putBoolean(this.locked);
        b.putBoolean(this.permaLocked);
        b.putBoolean(this.glassRemoved);
        b.putInt(this.health);
    }

    @Override
    public void syncIsoObjectReceive(ByteBufferReader bb) {
        this.open = bb.getBoolean();
        this.destroyed = bb.getBoolean();
        this.locked = bb.getBoolean();
        this.permaLocked = bb.getBoolean();
        this.glassRemoved = bb.getBoolean();
        this.health = bb.getInt();
        this.sprite = this.destroyed ? (this.glassRemoved ? this.glassRemovedSprite : this.smashedSprite) : (this.open ? this.openSprite : this.closedSprite);
        this.square.RecalcProperties();
        LuaEventManager.triggerEvent("OnContainerUpdate");
    }

    public static boolean isTopOfSheetRopeHere(IsoGridSquare sq) {
        if (sq == null) {
            return false;
        }
        return sq.has(IsoFlagType.climbSheetTopN) || sq.has(IsoFlagType.climbSheetTopS) || sq.has(IsoFlagType.climbSheetTopW) || sq.has(IsoFlagType.climbSheetTopE);
    }

    public static boolean isTopOfSheetRopeHere(IsoGridSquare sq, boolean north) {
        if (sq == null) {
            return false;
        }
        if (north) {
            if (sq.has(IsoFlagType.climbSheetTopN)) {
                return true;
            }
            return sq.getAdjacentSquare(IsoDirections.N) != null && sq.getAdjacentSquare(IsoDirections.N).has(IsoFlagType.climbSheetTopS);
        }
        if (sq.has(IsoFlagType.climbSheetTopW)) {
            return true;
        }
        return sq.getAdjacentSquare(IsoDirections.W) != null && sq.getAdjacentSquare(IsoDirections.W).has(IsoFlagType.climbSheetTopE);
    }

    @Override
    public boolean haveSheetRope() {
        return IsoWindow.isTopOfSheetRopeHere(this.square, this.north);
    }

    public static boolean isSheetRopeHere(IsoGridSquare sq) {
        if (sq == null) {
            return false;
        }
        return sq.has(IsoFlagType.climbSheetTopW) || sq.has(IsoFlagType.climbSheetTopN) || sq.has(IsoFlagType.climbSheetTopE) || sq.has(IsoFlagType.climbSheetTopS) || sq.has(IsoFlagType.climbSheetW) || sq.has(IsoFlagType.climbSheetN) || sq.has(IsoFlagType.climbSheetE) || sq.has(IsoFlagType.climbSheetS);
    }

    public static boolean canClimbHere(IsoGridSquare sq) {
        if (sq == null) {
            return false;
        }
        if (sq.getProperties().has(IsoFlagType.solid)) {
            return false;
        }
        if (sq.has(IsoObjectType.stairsBN) || sq.has(IsoObjectType.stairsMN) || sq.has(IsoObjectType.stairsTN)) {
            return false;
        }
        return !sq.has(IsoObjectType.stairsBW) && !sq.has(IsoObjectType.stairsMW) && !sq.has(IsoObjectType.stairsTW);
    }

    public static int countAddSheetRope(IsoGridSquare sq, boolean north) {
        if (IsoWindow.isTopOfSheetRopeHere(sq, north)) {
            return 0;
        }
        IsoCell cell = IsoWorld.instance.currentCell;
        if (sq.TreatAsSolidFloor()) {
            if (north) {
                IsoGridSquare sqn = cell.getOrCreateGridSquare(sq.getX(), sq.getY() - 1, sq.getZ());
                if (sqn == null || sqn.TreatAsSolidFloor() || IsoWindow.isSheetRopeHere(sqn) || !IsoWindow.canClimbHere(sqn)) {
                    return 0;
                }
                sq = sqn;
            } else {
                IsoGridSquare sqe = cell.getOrCreateGridSquare(sq.getX() - 1, sq.getY(), sq.getZ());
                if (sqe == null || sqe.TreatAsSolidFloor() || IsoWindow.isSheetRopeHere(sqe) || !IsoWindow.canClimbHere(sqe)) {
                    return 0;
                }
                sq = sqe;
            }
        }
        int count = 1;
        while (sq != null) {
            if (!IsoWindow.canClimbHere(sq)) {
                return 0;
            }
            if (sq.TreatAsSolidFloor()) {
                return count;
            }
            if (sq.getZ() == sq.getChunk().getMinLevel()) {
                return count;
            }
            sq = cell.getOrCreateGridSquare(sq.getX(), sq.getY(), sq.getZ() - 1);
            ++count;
        }
        return 0;
    }

    @Override
    public int countAddSheetRope() {
        return IsoWindow.countAddSheetRope(this.square, this.north);
    }

    public static boolean canAddSheetRope(IsoGridSquare sq, boolean north) {
        return IsoWindow.countAddSheetRope(sq, north) != 0;
    }

    @Override
    public boolean canAddSheetRope() {
        if (!this.canClimbThrough(null)) {
            return false;
        }
        return IsoWindow.canAddSheetRope(this.square, this.north);
    }

    @Override
    public boolean addSheetRope(IsoPlayer player, String itemType) {
        if (!this.canAddSheetRope()) {
            return false;
        }
        return IsoWindow.addSheetRope(player, this.square, this.north, itemType);
    }

    public static boolean addSheetRope(IsoPlayer player, IsoGridSquare sq, boolean north, String itemType) {
        boolean bLast = false;
        int n = 0;
        int i = 0;
        if (north) {
            i = 1;
        }
        boolean south = false;
        boolean east = false;
        IsoGridSquare sqe = null;
        IsoGridSquare sqn = null;
        IsoCell cell = IsoWorld.instance.currentCell;
        if (sq.TreatAsSolidFloor()) {
            if (!north) {
                sqe = cell.getGridSquare(sq.getX() - 1, sq.getY(), sq.getZ());
                if (sqe != null) {
                    east = true;
                    i = 3;
                }
            } else {
                sqn = cell.getGridSquare(sq.getX(), sq.getY() - 1, sq.getZ());
                if (sqn != null) {
                    south = true;
                    i = 4;
                }
            }
        }
        while (sq != null && (GameServer.server || player.getInventory().contains(itemType))) {
            ArrayList<InventoryItem> items;
            Object d = "crafted_01_" + i;
            if (n > 0) {
                d = east ? "crafted_01_10" : (south ? "crafted_01_13" : "crafted_01_" + (i + 8));
            }
            IsoObject sheetTop = new IsoObject(cell, sq, (String)d);
            sheetTop.setName(itemType);
            sheetTop.sheetRope = true;
            sq.getObjects().add(sheetTop);
            sheetTop.transmitCompleteItemToClients();
            sq.haveSheetRope = true;
            if (south && n == 0) {
                sq = sqn;
                sheetTop = new IsoObject(cell, sq, "crafted_01_5");
                sheetTop.setName(itemType);
                sheetTop.sheetRope = true;
                sq.getObjects().add(sheetTop);
                sheetTop.transmitCompleteItemToClients();
            }
            if (east && n == 0) {
                sq = sqe;
                sheetTop = new IsoObject(cell, sq, "crafted_01_2");
                sheetTop.setName(itemType);
                sheetTop.sheetRope = true;
                sq.getObjects().add(sheetTop);
                sheetTop.transmitCompleteItemToClients();
            }
            sq.RecalcProperties();
            sq.getProperties().unset(IsoFlagType.solidtrans);
            if (n == 0 && !sq.getProperties().has("TieSheetRope")) {
                items = player.getInventory().RemoveAll("Nails", 1);
                if (GameServer.server) {
                    GameServer.sendRemoveItemsFromContainer(player.getInventory(), items);
                }
            }
            items = player.getInventory().RemoveAll(itemType, 1);
            if (GameServer.server) {
                GameServer.sendRemoveItemsFromContainer(player.getInventory(), items);
            }
            ++n;
            if (bLast) break;
            if ((sq = cell.getOrCreateGridSquare(sq.getX(), sq.getY(), sq.getZ() - 1)) != null && sq.TreatAsSolidFloor()) {
                bLast = true;
            }
            sq.invalidateRenderChunkLevel(64L);
        }
        return true;
    }

    @Override
    public boolean removeSheetRope(IsoPlayer player) {
        if (!this.haveSheetRope()) {
            return false;
        }
        return IsoWindow.removeSheetRope(player, this.square, this.north);
    }

    /*
     * Enabled force condition propagation
     * Lifted jumps to return sites
     */
    public static boolean removeSheetRope(IsoPlayer player, IsoGridSquare square, boolean north) {
        IsoFlagType type2;
        IsoFlagType type1;
        if (square == null) {
            return false;
        }
        IsoGridSquare sq = square;
        sq.haveSheetRope = false;
        if (north) {
            if (square.has(IsoFlagType.climbSheetTopN)) {
                type1 = IsoFlagType.climbSheetTopN;
                type2 = IsoFlagType.climbSheetN;
            } else {
                if (square.getAdjacentSquare(IsoDirections.N) == null || !square.getAdjacentSquare(IsoDirections.N).has(IsoFlagType.climbSheetTopS)) return false;
                type1 = IsoFlagType.climbSheetTopS;
                type2 = IsoFlagType.climbSheetS;
                tile = "crafted_01_4";
                for (i = 0; i < sq.getObjects().size(); ++i) {
                    IsoObject o = sq.getObjects().get(i);
                    if (o.sprite == null || o.sprite.getName() == null || !o.sprite.getName().equals(tile)) continue;
                    sq.transmitRemoveItemFromSquare(o);
                    break;
                }
                sq = square.getAdjacentSquare(IsoDirections.N);
            }
        } else if (square.has(IsoFlagType.climbSheetTopW)) {
            type1 = IsoFlagType.climbSheetTopW;
            type2 = IsoFlagType.climbSheetW;
        } else {
            if (square.getAdjacentSquare(IsoDirections.W) == null || !square.getAdjacentSquare(IsoDirections.W).has(IsoFlagType.climbSheetTopE)) return false;
            type1 = IsoFlagType.climbSheetTopE;
            type2 = IsoFlagType.climbSheetE;
            tile = "crafted_01_3";
            for (i = 0; i < sq.getObjects().size(); ++i) {
                IsoObject o = sq.getObjects().get(i);
                if (o.sprite == null || o.sprite.getName() == null || !o.sprite.getName().equals(tile)) continue;
                sq.transmitRemoveItemFromSquare(o);
                break;
            }
            sq = square.getAdjacentSquare(IsoDirections.W);
        }
        while (sq != null) {
            boolean removed = false;
            for (int i = 0; i < sq.getObjects().size(); ++i) {
                IsoObject o = sq.getObjects().get(i);
                if (o.getProperties() == null || !o.getProperties().has(type1) && !o.getProperties().has(type2)) continue;
                sq.transmitRemoveItemFromSquare(o);
                if (GameServer.server) {
                    if (player != null) {
                        player.sendObjectChange(IsoObjectChange.ADD_ITEM_OF_TYPE, "type", o.getName());
                    }
                } else if (player != null) {
                    player.getInventory().AddItem(o.getName());
                }
                removed = true;
                break;
            }
            if (!removed || sq.getZ() == sq.getChunk().getMinLevel()) return true;
            sq = sq.getCell().getGridSquare(sq.getX(), sq.getY(), sq.getZ() - 1);
        }
        return true;
    }

    @Override
    public void Damage(float amount) {
        this.damage(amount);
    }

    private void damage(float amount) {
        if (this.isInvincible() || "Tutorial".equals(Core.gameMode)) {
            return;
        }
        this.DirtySlice();
        this.health -= (int)amount;
        if (this.health < 0) {
            this.health = 0;
        }
        if (!this.isDestroyed() && this.health == 0) {
            this.smashWindow(false, SandboxOptions.getInstance().lore.triggerHouseAlarm.getValue());
            if (this.getSquare().getBuilding() != null) {
                this.getSquare().getBuilding().forceAwake();
            }
        }
    }

    private void damage(float amount, IsoMovingObject chr) {
        if (this.isInvincible() || "Tutorial".equals(Core.gameMode)) {
            return;
        }
        this.health -= (int)amount;
        if (this.health < 0) {
            this.health = 0;
        }
        if (!this.isDestroyed() && this.health == 0) {
            boolean doAlarm = !(chr instanceof IsoZombie) || SandboxOptions.getInstance().lore.triggerHouseAlarm.getValue();
            this.smashWindow(false, doAlarm);
            this.addBrokenGlass(chr);
        }
    }

    public boolean isLocked() {
        return this.locked;
    }

    public boolean isSmashed() {
        return this.destroyed;
    }

    public boolean isInvincible() {
        if (this.square == null || !this.square.has(IsoFlagType.makeWindowInvincible)) {
            return false;
        }
        int index = this.getObjectIndex();
        if (index != -1) {
            IsoObject[] objects = this.square.getObjects().getElements();
            int size = this.square.getObjects().size();
            for (int i = 0; i < size; ++i) {
                IsoObject obj;
                PropertyContainer properties;
                if (i == index || (properties = (obj = objects[i]).getProperties()) == null || !properties.has(this.getNorth() ? IsoFlagType.cutN : IsoFlagType.cutW) || !properties.has(IsoFlagType.makeWindowInvincible)) continue;
                return true;
            }
        }
        return this.sprite != null && this.sprite.getProperties().has(IsoFlagType.makeWindowInvincible);
    }

    @Override
    public IsoBarricade getBarricadeOnSameSquare() {
        return IsoBarricade.GetBarricadeOnSquare(this.square, this.north ? IsoDirections.N : IsoDirections.W);
    }

    @Override
    public IsoBarricade getBarricadeOnOppositeSquare() {
        return IsoBarricade.GetBarricadeOnSquare(this.getOppositeSquare(), this.north ? IsoDirections.S : IsoDirections.E);
    }

    @Override
    public boolean isBarricaded() {
        IsoBarricade barricade = this.getBarricadeOnSameSquare();
        if (barricade == null) {
            barricade = this.getBarricadeOnOppositeSquare();
        }
        return barricade != null;
    }

    @Override
    public boolean isBarricadeAllowed() {
        return true;
    }

    @Override
    public IsoBarricade getBarricadeForCharacter(IsoGameCharacter chr) {
        return IsoBarricade.GetBarricadeForCharacter(this, chr);
    }

    @Override
    public IsoBarricade getBarricadeOppositeCharacter(IsoGameCharacter chr) {
        return IsoBarricade.GetBarricadeOppositeCharacter(this, chr);
    }

    @Override
    public boolean getNorth() {
        return this.north;
    }

    @Override
    public Vector2 getFacingPosition(Vector2 pos) {
        if (this.square == null) {
            return pos.set(0.0f, 0.0f);
        }
        if (this.north) {
            return pos.set(this.getX() + 0.5f, this.getY());
        }
        return pos.set(this.getX(), this.getY() + 0.5f);
    }

    public void setIsLocked(boolean lock) {
        this.locked = lock;
    }

    public IsoSprite getOpenSprite() {
        return this.openSprite;
    }

    public void setOpenSprite(IsoSprite sprite) {
        this.openSprite = sprite;
    }

    public void setSmashed(boolean destroyed) {
        if (destroyed) {
            this.destroyed = true;
            this.sprite = this.smashedSprite;
        } else {
            this.destroyed = false;
            this.sprite = this.open ? this.openSprite : this.closedSprite;
            this.health = this.maxHealth;
        }
        this.glassRemoved = false;
    }

    public IsoSprite getSmashedSprite() {
        return this.smashedSprite;
    }

    public void setSmashedSprite(IsoSprite sprite) {
        this.smashedSprite = sprite;
    }

    public void setPermaLocked(Boolean permaLock) {
        this.permaLocked = permaLock;
    }

    public boolean isPermaLocked() {
        return this.permaLocked;
    }

    public static boolean canClimbThroughHelper(IsoGameCharacter chr, IsoGridSquare sq, IsoGridSquare oppositeSq, boolean north) {
        IsoPlayer isoPlayer;
        if (chr instanceof IsoAnimal) {
            return false;
        }
        IsoGridSquare testSquare = sq;
        float dx = 0.5f;
        float dy = 0.5f;
        if (north) {
            if (chr.getY() >= (float)sq.getY()) {
                testSquare = oppositeSq;
                dy = 0.7f;
            } else {
                dy = 0.3f;
            }
        } else if (chr.getX() >= (float)sq.getX()) {
            testSquare = oppositeSq;
            dx = 0.7f;
        } else {
            dx = 0.3f;
        }
        if (testSquare == null) {
            return false;
        }
        if (testSquare.isSolid()) {
            return false;
        }
        if (testSquare.has(IsoFlagType.water)) {
            return false;
        }
        if (!(chr.canClimbDownSheetRope(testSquare) || testSquare.HasStairsBelow() || PolygonalMap2.instance.canStandAt((float)testSquare.x + dx, (float)testSquare.y + dy, testSquare.z, null, 19))) {
            return !testSquare.TreatAsSolidFloor();
        }
        return !GameClient.client || !(chr instanceof IsoPlayer) || SafeHouse.isSafeHouse(testSquare, (isoPlayer = (IsoPlayer)chr).getUsername(), true) == null || ServerOptions.instance.safehouseAllowTrepass.getValue();
    }

    public boolean canClimbThrough(IsoGameCharacter chr) {
        if (this.square == null || this.isInvincible()) {
            return false;
        }
        if (this.isBarricaded()) {
            return false;
        }
        if (chr != null && !IsoWindow.canClimbThroughHelper(chr, this.getSquare(), this.getOppositeSquare(), this.north)) {
            return false;
        }
        IsoGameCharacter chrClosing = this.getFirstCharacterClosing();
        if (chrClosing != null && chrClosing.isVariable("CloseWindowOutcome", "success")) {
            return false;
        }
        if (this.health <= 0 || this.destroyed) {
            return true;
        }
        return this.open;
    }

    public IsoGameCharacter getFirstCharacterClimbingThrough() {
        IsoGameCharacter chr = this.getFirstCharacterClimbingThrough(this.getSquare());
        if (chr != null) {
            return chr;
        }
        return this.getFirstCharacterClimbingThrough(this.getOppositeSquare());
    }

    public IsoGameCharacter getFirstCharacterClimbingThrough(IsoGridSquare square) {
        if (square == null) {
            return null;
        }
        for (int i = 0; i < square.getMovingObjects().size(); ++i) {
            IsoGameCharacter chr = Type.tryCastTo(square.getMovingObjects().get(i), IsoGameCharacter.class);
            if (chr == null || !chr.isClimbingThroughWindow(this)) continue;
            return chr;
        }
        return null;
    }

    public IsoGameCharacter getFirstCharacterClosing() {
        IsoGameCharacter chr = this.getFirstCharacterClosing(this.getSquare());
        if (chr != null) {
            return chr;
        }
        return this.getFirstCharacterClosing(this.getOppositeSquare());
    }

    public IsoGameCharacter getFirstCharacterClosing(IsoGridSquare square) {
        if (square == null) {
            return null;
        }
        for (int i = 0; i < square.getMovingObjects().size(); ++i) {
            IsoGameCharacter chr = Type.tryCastTo(square.getMovingObjects().get(i), IsoGameCharacter.class);
            if (chr == null || !chr.isClosingWindow(this)) continue;
            return chr;
        }
        return null;
    }

    public boolean isGlassRemoved() {
        return this.glassRemoved;
    }

    public void setGlassRemoved(boolean removed) {
        if (!this.destroyed) {
            return;
        }
        if (removed) {
            this.sprite = this.glassRemovedSprite;
            this.glassRemoved = true;
        } else {
            this.sprite = this.smashedSprite;
            this.glassRemoved = false;
        }
        if (this.getObjectIndex() != -1) {
            PolygonalMap2.instance.squareChanged(this.square);
        }
    }

    public void removeBrokenGlass() {
        if (GameClient.client) {
            GameClient.instance.removeBrokenGlass(this);
        } else {
            this.setGlassRemoved(true);
        }
    }

    public IsoBarricade addBarricadesDebug(int numPlanks, boolean metal) {
        IsoGridSquare outside = this.square.getRoom() == null ? this.square : this.getOppositeSquare();
        boolean addOpposite = outside != this.square;
        IsoBarricade barricade = IsoBarricade.AddBarricadeToObject((BarricadeAble)this, addOpposite);
        if (barricade != null) {
            for (int b = 0; b < numPlanks; ++b) {
                if (metal) {
                    barricade.addMetalBar(null, null);
                    continue;
                }
                barricade.addPlank(null, null);
            }
        }
        return barricade;
    }

    public void addRandomBarricades() {
        IsoGridSquare outside;
        IsoGridSquare isoGridSquare = outside = this.square.getRoom() == null ? this.square : this.getOppositeSquare();
        if (this.getZ() == 0.0f && outside != null && outside.getRoom() == null) {
            boolean addOpposite = outside != this.square;
            IsoBarricade barricade = IsoBarricade.AddBarricadeToObject((BarricadeAble)this, addOpposite);
            if (barricade != null) {
                int numPlanks = Rand.Next(1, 4);
                for (int b = 0; b < numPlanks; ++b) {
                    barricade.addPlank(null, null);
                }
                if (GameServer.server) {
                    barricade.transmitCompleteItemToClients();
                }
            }
        } else {
            this.addSheet(null);
            this.HasCurtains().ToggleDoor(null);
        }
    }

    public int getHealth() {
        return this.health;
    }

    public boolean IsOpen() {
        return this.open;
    }

    public boolean isNorth() {
        return this.north;
    }

    @Override
    public boolean onMouseLeftClick(int x, int y) {
        return true;
    }

    public boolean canAttackBypassIsoBarricade(IsoGameCharacter isoGameCharacter, HandWeapon handWeapon) {
        IsoBarricade isoBarricade = this.getBarricadeForCharacter(isoGameCharacter);
        if (isoBarricade == null) {
            return true;
        }
        return isoBarricade.canAttackBypassIsoBarricade(isoGameCharacter, handWeapon);
    }

    @Override
    public void reset() {
        this.sprite = this.closedSprite;
        this.destroyed = false;
        this.glassRemoved = false;
        switch (this.type.ordinal()) {
            case 0: {
                this.health = 50;
                this.maxHealth = 50;
                break;
            }
            case 1: {
                this.health = 100;
                this.maxHealth = 100;
            }
        }
    }

    public static void resetCurrentCellWindows() {
        ArrayList<IsoWindow> isoWindowList = IsoWorld.instance.currentCell.getWindowList();
        for (IsoWindow isoWindow : isoWindowList.toArray(new IsoWindow[0])) {
            isoWindow.reset();
        }
    }

    public static enum WindowType {
        SinglePane,
        DoublePane;

    }

    private static enum LockedHouseFrequency {
        Never(0, 0),
        ExtremelyRare(1, 5),
        Rare(2, 10),
        Sometimes(3, 50),
        Often(4, 60),
        VeryOften(5, 70);

        private final int value;
        private final int lockChance;

        private LockedHouseFrequency(int value, int lockChance) {
            this.value = value;
            this.lockChance = lockChance;
        }

        public int getLockChance() {
            return this.lockChance;
        }

        public static LockedHouseFrequency fromValue(int value) {
            for (LockedHouseFrequency freq : LockedHouseFrequency.values()) {
                if (freq.value != value) continue;
                return freq;
            }
            return VeryOften;
        }
    }
}

