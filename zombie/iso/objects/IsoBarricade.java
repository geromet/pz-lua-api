/*
 * Decompiled with CFR 0.152.
 */
package zombie.iso.objects;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import se.krka.kahlua.vm.KahluaTable;
import zombie.GameTime;
import zombie.Lua.LuaEventManager;
import zombie.SoundManager;
import zombie.UsedFromLua;
import zombie.WorldSoundManager;
import zombie.ai.states.ThumpState;
import zombie.audio.parameters.ParameterMeleeHitSurface;
import zombie.characters.IsoGameCharacter;
import zombie.characters.IsoPlayer;
import zombie.characters.IsoZombie;
import zombie.core.Core;
import zombie.core.math.PZMath;
import zombie.core.network.ByteBufferReader;
import zombie.core.network.ByteBufferWriter;
import zombie.core.opengl.Shader;
import zombie.core.properties.IsoObjectChange;
import zombie.core.raknet.UdpConnection;
import zombie.core.textures.ColorInfo;
import zombie.inventory.InventoryItem;
import zombie.inventory.InventoryItemFactory;
import zombie.inventory.types.HandWeapon;
import zombie.iso.IHasHealth;
import zombie.iso.IsoCamera;
import zombie.iso.IsoCell;
import zombie.iso.IsoDirections;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoMovingObject;
import zombie.iso.IsoObject;
import zombie.iso.IsoWorld;
import zombie.iso.LosUtil;
import zombie.iso.SpriteDetails.IsoObjectType;
import zombie.iso.Vector2;
import zombie.iso.fboRenderChunk.FBORenderCell;
import zombie.iso.objects.IsoCurtain;
import zombie.iso.objects.IsoWindow;
import zombie.iso.objects.RenderEffectType;
import zombie.iso.objects.interfaces.BarricadeAble;
import zombie.iso.objects.interfaces.Thumpable;
import zombie.iso.sprite.IsoSpriteManager;
import zombie.network.GameClient;
import zombie.network.GameServer;
import zombie.network.PacketTypes;
import zombie.util.Type;
import zombie.util.list.PZArrayList;

@UsedFromLua
public class IsoBarricade
extends IsoObject
implements Thumpable,
IHasHealth {
    public static final int MAX_PLANKS = 4;
    public static final int PLANK_HEALTH = 1000;
    public static final int METAL_BAR_HEALTH = 3000;
    public static final int METAL_HEALTH = 5000;
    public static final int METAL_HEALTH_DAMAGED = 2500;
    private final int[] plankHealth = new int[4];
    private int metalHealth;
    private int metalBarHealth;

    public IsoBarricade(IsoCell cell) {
        super(cell);
    }

    public IsoBarricade(IsoGridSquare gridSquare, IsoDirections dir) {
        this.square = gridSquare;
        this.dir = dir;
    }

    @Override
    public String getObjectName() {
        return "Barricade";
    }

    public void addPlank(IsoGameCharacter chr) {
        Object plank = InventoryItemFactory.CreateItem("Base.Plank");
        this.addPlank(chr, (InventoryItem)plank);
    }

    public void addPlank(IsoGameCharacter chr, InventoryItem plank) {
        if (!this.canAddPlank()) {
            return;
        }
        int plankHealth = 1000;
        if (plank != null) {
            plankHealth = (int)((float)plank.getCondition() / (float)plank.getConditionMax() * 1000.0f);
        }
        if (chr != null) {
            plankHealth = (int)((float)plankHealth * chr.getBarricadeStrengthMod());
        }
        for (int i = 0; i < 4; ++i) {
            if (this.plankHealth[i] > 0) continue;
            this.plankHealth[i] = plankHealth;
            break;
        }
        this.updateSprite();
        IsoBarricade.recalculateLighting();
    }

    public InventoryItem removePlank(IsoGameCharacter chr) {
        if (this.getNumPlanks() <= 0) {
            return null;
        }
        InventoryItem item = null;
        for (int i = 3; i >= 0; --i) {
            if (this.plankHealth[i] <= 0) continue;
            float f = Math.min((float)this.plankHealth[i] / 1000.0f, 1.0f);
            item = (InventoryItem)InventoryItemFactory.CreateItem("Base.Plank");
            item.setCondition((int)Math.max((float)item.getConditionMax() * f, 1.0f));
            this.plankHealth[i] = 0;
            break;
        }
        if (this.getNumPlanks() <= 0) {
            if (this.square != null) {
                if (GameServer.server) {
                    this.square.transmitRemoveItemFromSquare(this);
                } else {
                    this.square.RemoveTileObject(this);
                }
            }
        } else {
            this.updateSprite();
            IsoBarricade.recalculateLighting();
        }
        return item;
    }

    public int getNumPlanks() {
        int count = 0;
        for (int i = 0; i < 4; ++i) {
            if (this.plankHealth[i] <= 0) continue;
            ++count;
        }
        return count;
    }

    public boolean canAddPlank() {
        return !this.isMetal() && this.getNumPlanks() < 4 && !this.isMetalBar();
    }

    public void addMetalBar(IsoGameCharacter chr, InventoryItem metalBar) {
        if (this.getNumPlanks() > 0) {
            return;
        }
        if (this.metalHealth > 0) {
            return;
        }
        if (this.metalBarHealth > 0) {
            return;
        }
        this.metalBarHealth = 3000;
        if (metalBar != null) {
            this.metalBarHealth = (int)((float)metalBar.getCondition() / (float)metalBar.getConditionMax() * 5000.0f);
        }
        if (chr != null) {
            this.metalBarHealth = (int)((float)this.metalBarHealth * chr.getMetalBarricadeStrengthMod());
        }
        this.updateSprite();
        IsoBarricade.recalculateLighting();
    }

    public InventoryItem removeMetalBar(IsoGameCharacter chr) {
        if (this.metalBarHealth <= 0) {
            return null;
        }
        float f = Math.min((float)this.metalBarHealth / 3000.0f, 1.0f);
        this.metalBarHealth = 0;
        Object item = InventoryItemFactory.CreateItem("Base.MetalBar");
        ((InventoryItem)item).setCondition((int)Math.min((float)((InventoryItem)item).getConditionMax() * f, 1.0f));
        if (this.square != null) {
            if (GameServer.server) {
                this.square.transmitRemoveItemFromSquare(this);
            } else {
                this.square.RemoveTileObject(this);
            }
        }
        return item;
    }

    public void addMetal(IsoGameCharacter chr, InventoryItem metal) {
        if (this.getNumPlanks() > 0) {
            return;
        }
        if (this.metalHealth > 0) {
            return;
        }
        this.metalHealth = 5000;
        if (metal != null) {
            this.metalHealth = (int)((float)metal.getCondition() / (float)metal.getConditionMax() * 5000.0f);
        }
        if (chr != null) {
            this.metalHealth = (int)((float)this.metalHealth * chr.getMetalBarricadeStrengthMod());
        }
        this.updateSprite();
        IsoBarricade.recalculateLighting();
    }

    public boolean isMetalBar() {
        return this.metalBarHealth > 0;
    }

    public InventoryItem removeMetal(IsoGameCharacter chr) {
        if (this.metalHealth <= 0) {
            return null;
        }
        float f = Math.min((float)this.metalHealth / 5000.0f, 1.0f);
        this.metalHealth = 0;
        Object item = InventoryItemFactory.CreateItem("Base.SheetMetal");
        ((InventoryItem)item).setCondition((int)Math.max((float)((InventoryItem)item).getConditionMax() * f, 1.0f));
        if (this.square != null) {
            if (GameServer.server) {
                this.square.transmitRemoveItemFromSquare(this);
            } else {
                this.square.RemoveTileObject(this);
            }
        }
        return item;
    }

    public boolean isMetal() {
        return this.metalHealth > 0;
    }

    public boolean isBlockVision() {
        return this.isMetal() || this.getNumPlanks() > 2;
    }

    private void chooseSprite() {
        IsoSpriteManager spriteManager = IsoSpriteManager.instance;
        if (this.metalHealth > 0) {
            int damageOffset = this.metalHealth <= 2500 ? 2 : 0;
            String tileset = "constructedobjects_01";
            switch (this.dir) {
                case W: {
                    this.sprite = spriteManager.getSprite("constructedobjects_01_" + (24 + damageOffset));
                    break;
                }
                case N: {
                    this.sprite = spriteManager.getSprite("constructedobjects_01_" + (25 + damageOffset));
                    break;
                }
                case E: {
                    this.sprite = spriteManager.getSprite("constructedobjects_01_" + (28 + damageOffset));
                    break;
                }
                case S: {
                    this.sprite = spriteManager.getSprite("constructedobjects_01_" + (29 + damageOffset));
                    break;
                }
                default: {
                    this.sprite.LoadFramesNoDirPageSimple("media/ui/missing-tile.png");
                }
            }
            return;
        }
        if (this.metalBarHealth > 0) {
            String tileset = "constructedobjects_01";
            switch (this.dir) {
                case W: {
                    this.sprite = spriteManager.getSprite("constructedobjects_01_55");
                    break;
                }
                case N: {
                    this.sprite = spriteManager.getSprite("constructedobjects_01_53");
                    break;
                }
                case E: {
                    this.sprite = spriteManager.getSprite("constructedobjects_01_52");
                    break;
                }
                case S: {
                    this.sprite = spriteManager.getSprite("constructedobjects_01_54");
                    break;
                }
                default: {
                    this.sprite.LoadFramesNoDirPageSimple("media/ui/missing-tile.png");
                }
            }
            return;
        }
        int numPlanks = this.getNumPlanks();
        if (numPlanks <= 0) {
            this.sprite = spriteManager.getSprite("media/ui/missing-tile.png");
            return;
        }
        String tileset = "carpentry_01";
        switch (this.dir) {
            case W: {
                this.sprite = spriteManager.getSprite("carpentry_01_" + (8 + (numPlanks - 1) * 2));
                break;
            }
            case N: {
                this.sprite = spriteManager.getSprite("carpentry_01_" + (9 + (numPlanks - 1) * 2));
                break;
            }
            case E: {
                this.sprite = spriteManager.getSprite("carpentry_01_" + (0 + (numPlanks - 1) * 2));
                break;
            }
            case S: {
                this.sprite = spriteManager.getSprite("carpentry_01_" + (1 + (numPlanks - 1) * 2));
                break;
            }
            default: {
                this.sprite.LoadFramesNoDirPageSimple("media/ui/missing-tile.png");
            }
        }
    }

    @Override
    public boolean isDestroyed() {
        return this.metalHealth <= 0 && this.getNumPlanks() <= 0 && this.metalBarHealth <= 0;
    }

    @Override
    public IsoObject.VisionResult TestVision(IsoGridSquare from, IsoGridSquare to) {
        if (this.metalHealth <= 0 && this.getNumPlanks() <= 2) {
            return IsoObject.VisionResult.NoEffect;
        }
        if (from == this.square) {
            if (this.dir == IsoDirections.N && to.getY() < from.getY()) {
                return IsoObject.VisionResult.Blocked;
            }
            if (this.dir == IsoDirections.S && to.getY() > from.getY()) {
                return IsoObject.VisionResult.Blocked;
            }
            if (this.dir == IsoDirections.W && to.getX() < from.getX()) {
                return IsoObject.VisionResult.Blocked;
            }
            if (this.dir == IsoDirections.E && to.getX() > from.getX()) {
                return IsoObject.VisionResult.Blocked;
            }
        } else if (to == this.square && from != this.square) {
            return this.TestVision(to, from);
        }
        return IsoObject.VisionResult.NoEffect;
    }

    @Override
    public void Thump(IsoMovingObject thumper) {
        if (this.isDestroyed()) {
            return;
        }
        if (thumper instanceof IsoZombie) {
            IsoZombie isoZombie = (IsoZombie)thumper;
            int numPlanks = this.getNumPlanks();
            boolean metalOK = this.metalHealth > 2500;
            int mult = ThumpState.getFastForwardDamageMultiplier();
            this.Damage(isoZombie.strength * mult);
            if (numPlanks != this.getNumPlanks()) {
                ((IsoGameCharacter)thumper).getEmitter().playSound("BreakBarricadePlank");
                if (GameServer.server) {
                    GameServer.PlayWorldSoundServer("BreakBarricadePlank", false, thumper.getCurrentSquare(), 0.2f, 20.0f, 1.1f, true);
                }
            }
            if (this.isDestroyed()) {
                if (this.getSquare().getBuilding() != null) {
                    this.getSquare().getBuilding().forceAwake();
                }
                this.square.transmitRemoveItemFromSquare(this);
                if (!GameServer.server) {
                    this.square.RemoveTileObject(this);
                }
            } else if ((numPlanks != this.getNumPlanks() || metalOK && this.metalHealth < 2500) && GameServer.server) {
                this.sendObjectChange(IsoObjectChange.STATE);
            }
            if (!this.isDestroyed()) {
                this.setRenderEffect(RenderEffectType.Hit_Door, true);
            }
            WorldSoundManager.instance.addSound(thumper, this.square.getX(), this.square.getY(), this.square.getZ(), 20, 20, true, 4.0f, 15.0f);
        }
    }

    @Override
    public Thumpable getThumpableFor(IsoGameCharacter chr) {
        if (this.isDestroyed()) {
            return null;
        }
        return this;
    }

    @Override
    public Vector2 getFacingPosition(Vector2 pos) {
        if (this.square == null) {
            return pos.set(0.0f, 0.0f);
        }
        if (this.dir == IsoDirections.N) {
            return pos.set(this.getX() + 0.5f, this.getY());
        }
        if (this.dir == IsoDirections.S) {
            return pos.set(this.getX() + 0.5f, this.getY() + 1.0f);
        }
        if (this.dir == IsoDirections.W) {
            return pos.set(this.getX(), this.getY() + 0.5f);
        }
        if (this.dir == IsoDirections.E) {
            return pos.set(this.getX() + 1.0f, this.getY() + 0.5f);
        }
        return pos.set(this.getX(), this.getY() + 0.5f);
    }

    @Override
    public void WeaponHit(IsoGameCharacter owner, HandWeapon weapon) {
        String sound;
        if (this.isDestroyed()) {
            return;
        }
        IsoPlayer player = Type.tryCastTo(owner, IsoPlayer.class);
        if (GameClient.client) {
            return;
        }
        LuaEventManager.triggerEvent("OnWeaponHitThumpable", owner, weapon, this);
        String string = sound = this.isMetal() || this.isMetalBar() ? "HitBarricadeMetal" : "HitBarricadePlank";
        if (player != null) {
            player.setMeleeHitSurface(this.isMetal() || this.isMetalBar() ? ParameterMeleeHitSurface.Material.Metal : ParameterMeleeHitSurface.Material.Wood);
        }
        SoundManager.instance.PlayWorldSound(sound, false, this.getSquare(), 1.0f, 20.0f, 2.0f, false);
        if (GameServer.server) {
            GameServer.PlayWorldSoundServer(sound, false, this.getSquare(), 1.0f, 20.0f, 2.0f, false);
        }
        if (weapon != null) {
            this.Damage((float)weapon.getDoorDamage() * 5.0f);
        } else {
            this.Damage(100.0f);
        }
        WorldSoundManager.instance.addSound(owner, this.square.getX(), this.square.getY(), this.square.getZ(), 20, 20, false, 0.0f, 15.0f);
        if (this.isDestroyed()) {
            if (owner != null) {
                String sound2 = sound.equals("HitBarricadeMetal") ? "BreakBarricadeMetal" : "BreakBarricadePlank";
                owner.getEmitter().playSound(sound2);
                if (GameServer.server) {
                    GameServer.PlayWorldSoundServer(sound2, false, owner.getCurrentSquare(), 0.2f, 20.0f, 1.1f, true);
                }
            }
            this.square.transmitRemoveItemFromSquare(this);
            if (!GameServer.server) {
                this.square.RemoveTileObject(this);
            }
        }
        if (!this.isDestroyed()) {
            this.setRenderEffect(RenderEffectType.Hit_Door, true);
        }
    }

    @Override
    public void Damage(float amount) {
        if ("Tutorial".equals(Core.gameMode)) {
            return;
        }
        if (this.metalHealth > 0) {
            this.metalHealth = (int)((float)this.metalHealth - amount);
            if (this.metalHealth <= 0) {
                this.metalHealth = 0;
                this.chooseSprite();
            }
            if (GameServer.server) {
                this.sync();
            }
            return;
        }
        if (this.metalBarHealth > 0) {
            this.metalBarHealth = (int)((float)this.metalBarHealth - amount);
            if (this.metalBarHealth <= 0) {
                this.metalBarHealth = 0;
                this.chooseSprite();
            }
            if (GameServer.server) {
                this.sync();
            }
            return;
        }
        for (int i = 3; i >= 0; --i) {
            if (this.plankHealth[i] <= 0) continue;
            int n = i;
            this.plankHealth[n] = (int)((float)this.plankHealth[n] - amount);
            if (this.plankHealth[i] > 0) break;
            this.plankHealth[i] = 0;
            this.chooseSprite();
            break;
        }
        if (GameServer.server) {
            this.sync();
        }
    }

    @Override
    public void syncIsoObjectSend(ByteBufferWriter b) {
        super.syncIsoObjectSend(b);
        b.putInt(this.metalHealth);
        b.putInt(this.metalBarHealth);
        for (int i = 0; i < 4; ++i) {
            b.putInt(this.plankHealth[i]);
        }
    }

    @Override
    public void syncIsoObjectReceive(ByteBufferReader bb) {
        super.syncIsoObjectReceive(bb);
        this.metalHealth = bb.getInt();
        this.metalBarHealth = bb.getInt();
        for (int i = 0; i < 4; ++i) {
            this.plankHealth[i] = bb.getInt();
        }
    }

    @Override
    public void syncIsoObject(boolean bRemote, byte val, UdpConnection source2, ByteBufferReader bb) {
        if (GameClient.client && bRemote) {
            this.syncIsoObjectReceive(bb);
        }
        if (GameServer.server) {
            for (UdpConnection connection : GameServer.udpEngine.connections) {
                if (source2 != null && connection.getConnectedGUID() == source2.getConnectedGUID()) continue;
                ByteBufferWriter b = connection.startPacket();
                PacketTypes.PacketType.SyncIsoObject.doPacket(b);
                this.syncIsoObjectSend(b);
                PacketTypes.PacketType.SyncIsoObject.send(connection);
            }
        }
    }

    @Override
    public float getThumpCondition() {
        if (this.metalHealth > 0) {
            return (float)PZMath.clamp(this.metalHealth, 0, 5000) / 5000.0f;
        }
        if (this.metalBarHealth > 0) {
            return (float)PZMath.clamp(this.metalBarHealth, 0, 3000) / 3000.0f;
        }
        for (int i = 3; i >= 0; --i) {
            if (this.plankHealth[i] <= 0) continue;
            return (float)PZMath.clamp(this.plankHealth[i], 0, 1000) / 1000.0f;
        }
        return 0.0f;
    }

    @Override
    public void setHealth(int health) {
        if (this.metalHealth > 0) {
            this.metalHealth = PZMath.clamp(health, 0, 5000);
        }
        if (this.metalBarHealth > 0) {
            this.metalBarHealth = PZMath.clamp(health, 0, 3000);
        }
        for (int i = 3; i >= 0; --i) {
            if (this.plankHealth[i] <= 0) continue;
            this.plankHealth[i] = PZMath.clamp(health, 0, 1000);
        }
    }

    @Override
    public int getHealth() {
        if (this.metalHealth > 0) {
            return this.metalHealth;
        }
        if (this.metalBarHealth > 0) {
            return this.metalBarHealth;
        }
        int totalHealth = 0;
        int numPlanks = 0;
        for (int i = 3; i >= 0; --i) {
            if (this.plankHealth[i] <= 0) continue;
            ++numPlanks;
            totalHealth += this.plankHealth[i];
        }
        if (totalHealth > 0 && numPlanks > 0) {
            return totalHealth / numPlanks;
        }
        return 0;
    }

    @Override
    public int getMaxHealth() {
        if (this.metalHealth > 0) {
            return 5000;
        }
        if (this.metalBarHealth > 0) {
            return 3000;
        }
        int totalHealth = 0;
        int numPlanks = 0;
        for (int i = 3; i >= 0; --i) {
            if (this.plankHealth[i] <= 0) continue;
            ++numPlanks;
            totalHealth += 1000;
        }
        if (totalHealth > 0 && numPlanks > 0) {
            return totalHealth / numPlanks;
        }
        return 5000;
    }

    @Override
    public void load(ByteBuffer input, int worldVersion, boolean isDebugSave) throws IOException {
        byte dirIndex = input.get();
        this.dir = IsoDirections.fromIndex(dirIndex);
        int numPlanks = input.get();
        for (int i = 0; i < numPlanks; ++i) {
            short plankHealth = input.getShort();
            if (i >= 4) continue;
            this.plankHealth[i] = plankHealth;
        }
        this.metalHealth = input.getShort();
        this.metalBarHealth = input.getShort();
        this.chooseSprite();
    }

    @Override
    public void save(ByteBuffer output, boolean isDebugSave) throws IOException {
        output.put((byte)1);
        output.put(IsoObject.factoryGetClassID(this.getObjectName()));
        output.put((byte)this.dir.ordinal());
        output.put((byte)4);
        for (int i = 0; i < 4; ++i) {
            output.putShort((short)this.plankHealth[i]);
        }
        output.putShort((short)this.metalHealth);
        output.putShort((short)this.metalBarHealth);
    }

    @Override
    public void saveChange(IsoObjectChange change, KahluaTable tbl, ByteBufferWriter bb) {
        if (change == IsoObjectChange.STATE) {
            for (int i = 0; i < 4; ++i) {
                bb.putShort(this.plankHealth[i]);
            }
            bb.putShort(this.metalHealth);
            bb.putShort(this.metalBarHealth);
        }
    }

    @Override
    public void loadChange(IsoObjectChange change, ByteBufferReader bb) {
        if (change == IsoObjectChange.STATE) {
            for (int i = 0; i < 4; ++i) {
                this.plankHealth[i] = bb.getShort();
            }
            this.metalHealth = bb.getShort();
            this.metalBarHealth = bb.getShort();
            this.updateSprite();
            IsoBarricade.recalculateLighting();
        }
    }

    public BarricadeAble getBarricadedObject() {
        block5: {
            PZArrayList<IsoObject> specials;
            block4: {
                int index = this.getObjectIndex();
                if (index == -1) {
                    return null;
                }
                specials = this.getSquare().getObjects();
                if (this.getDir() != IsoDirections.W && this.getDir() != IsoDirections.N) break block4;
                boolean north = this.getDir() == IsoDirections.N;
                for (int i = index - 1; i >= 0; --i) {
                    BarricadeAble barricadeAble;
                    IsoObject obj = specials.get(i);
                    if (!(obj instanceof BarricadeAble) || north != (barricadeAble = (BarricadeAble)((Object)obj)).getNorth()) continue;
                    return barricadeAble;
                }
                break block5;
            }
            if (this.getDir() != IsoDirections.E && this.getDir() != IsoDirections.S) break block5;
            boolean north = this.getDir() == IsoDirections.S;
            int x = this.getSquare().getX() + (this.getDir() == IsoDirections.E ? 1 : 0);
            int y = this.getSquare().getY() + (this.getDir() == IsoDirections.S ? 1 : 0);
            IsoGridSquare sq = this.getCell().getGridSquare((double)x, (double)y, this.getZ());
            if (sq != null) {
                specials = sq.getObjects();
                for (int i = specials.size() - 1; i >= 0; --i) {
                    BarricadeAble barricadeAble;
                    IsoObject obj = specials.get(i);
                    if (!(obj instanceof BarricadeAble) || north != (barricadeAble = (BarricadeAble)((Object)obj)).getNorth()) continue;
                    return barricadeAble;
                }
            }
        }
        return null;
    }

    @Override
    public void render(float x, float y, float z, ColorInfo col, boolean bDoAttached, boolean bWallLightingPass, Shader shader) {
        int playerIndex = IsoCamera.frameState.playerIndex;
        BarricadeAble barricadeAble = this.getBarricadedObject();
        if (barricadeAble != null && this.square.lighting[playerIndex].targetDarkMulti() <= barricadeAble.getSquare().lighting[playerIndex].targetDarkMulti()) {
            col = barricadeAble.getSquare().lighting[playerIndex].lightInfo();
            this.setTargetAlpha(playerIndex, FBORenderCell.instance.calculateWindowTargetAlpha(playerIndex, (IsoObject)((Object)barricadeAble), barricadeAble.getOppositeSquare(), barricadeAble.getNorth()));
        }
        super.render(x, y, z, col, bDoAttached, bWallLightingPass, shader);
    }

    public static IsoBarricade GetBarricadeOnSquare(IsoGridSquare square, IsoDirections dir) {
        if (square == null) {
            return null;
        }
        for (int i = 0; i < square.getSpecialObjects().size(); ++i) {
            IsoBarricade barricade;
            IsoObject obj = square.getSpecialObjects().get(i);
            if (!(obj instanceof IsoBarricade) || (barricade = (IsoBarricade)obj).getDir() != dir) continue;
            return barricade;
        }
        return null;
    }

    public static IsoBarricade GetBarricadeForCharacter(BarricadeAble obj, IsoGameCharacter chr) {
        if (obj == null || obj.getSquare() == null) {
            return null;
        }
        if (chr != null && (obj.getNorth() ? chr.getY() < (float)obj.getSquare().getY() : chr.getX() < (float)obj.getSquare().getX())) {
            return IsoBarricade.GetBarricadeOnSquare(obj.getOppositeSquare(), obj.getNorth() ? IsoDirections.S : IsoDirections.E);
        }
        return IsoBarricade.GetBarricadeOnSquare(obj.getSquare(), obj.getNorth() ? IsoDirections.N : IsoDirections.W);
    }

    public static IsoBarricade GetBarricadeOppositeCharacter(BarricadeAble obj, IsoGameCharacter chr) {
        if (obj == null || obj.getSquare() == null) {
            return null;
        }
        if (chr != null && (obj.getNorth() ? chr.getY() < (float)obj.getSquare().getY() : chr.getX() < (float)obj.getSquare().getX())) {
            return IsoBarricade.GetBarricadeOnSquare(obj.getSquare(), obj.getNorth() ? IsoDirections.N : IsoDirections.W);
        }
        return IsoBarricade.GetBarricadeOnSquare(obj.getOppositeSquare(), obj.getNorth() ? IsoDirections.S : IsoDirections.E);
    }

    public static IsoBarricade AddBarricadeToObject(BarricadeAble to, boolean addOpposite) {
        IsoDirections dir;
        IsoGridSquare square;
        IsoGridSquare isoGridSquare = square = addOpposite ? to.getOppositeSquare() : to.getSquare();
        if (to.getNorth()) {
            dir = addOpposite ? IsoDirections.S : IsoDirections.N;
        } else {
            IsoDirections isoDirections = dir = addOpposite ? IsoDirections.E : IsoDirections.W;
        }
        if (square == null || dir == null) {
            return null;
        }
        IsoBarricade barricade = IsoBarricade.GetBarricadeOnSquare(square, dir);
        if (barricade != null) {
            return barricade;
        }
        barricade = new IsoBarricade(square, dir);
        int index = -1;
        for (int i = 0; i < square.getObjects().size(); ++i) {
            IsoObject obj = square.getObjects().get(i);
            if (!(obj instanceof IsoCurtain)) continue;
            IsoCurtain curtain = (IsoCurtain)obj;
            if (curtain.getType() == IsoObjectType.curtainW && dir == IsoDirections.W) {
                index = i;
            } else if (curtain.getType() == IsoObjectType.curtainN && dir == IsoDirections.N) {
                index = i;
            } else if (curtain.getType() == IsoObjectType.curtainE && dir == IsoDirections.E) {
                index = i;
            } else if (curtain.getType() == IsoObjectType.curtainS && dir == IsoDirections.S) {
                index = i;
            }
            if (index != -1) break;
        }
        square.AddSpecialObject(barricade, index);
        for (int pn = 0; pn < IsoPlayer.numPlayers; ++pn) {
            LosUtil.cachecleared[pn] = true;
        }
        IsoGridSquare.setRecalcLightTime(-1.0f);
        GameTime.instance.lightSourceUpdate = 100.0f;
        return barricade;
    }

    public static IsoBarricade AddBarricadeToObject(BarricadeAble to, IsoGameCharacter chr) {
        if (to == null || to.getSquare() == null || chr == null) {
            return null;
        }
        if (to.getNorth()) {
            boolean addOpposite = chr.getY() < (float)to.getSquare().getY();
            return IsoBarricade.AddBarricadeToObject(to, addOpposite);
        }
        boolean addOpposite = chr.getX() < (float)to.getSquare().getX();
        return IsoBarricade.AddBarricadeToObject(to, addOpposite);
    }

    public boolean canAttackBypassIsoBarricade(IsoGameCharacter isoGameCharacter, HandWeapon handWeapon) {
        if (handWeapon == null) {
            return false;
        }
        if (this.isDestroyed()) {
            return true;
        }
        if (handWeapon.isAimedFirearm()) {
            return !this.isBlockVision();
        }
        return handWeapon.canAttackPierceTransparentWall(isoGameCharacter, handWeapon);
    }

    public static void barricadeCurrentCellWithMetalPlate() {
        ArrayList<IsoWindow> isoWindowList = IsoWorld.instance.currentCell.getWindowList();
        for (IsoWindow isoWindow : isoWindowList.toArray(new IsoWindow[0])) {
            isoWindow.addBarricadesDebug(0, true);
            IsoBarricade isoBarricade = isoWindow.getBarricadeOnSameSquare();
            if (isoBarricade != null) {
                isoBarricade.setNumberOfPlanks(0);
                isoBarricade.metalHealth = 5000;
                isoBarricade.updateSprite();
            }
            if ((isoBarricade = isoWindow.getBarricadeOnOppositeSquare()) == null) continue;
            isoBarricade.setNumberOfPlanks(0);
            isoBarricade.metalHealth = 5000;
            isoBarricade.updateSprite();
        }
        IsoBarricade.recalculateLighting();
    }

    public static void barricadeCurrentCellWithMetalBars() {
        ArrayList<IsoWindow> isoWindowList = IsoWorld.instance.currentCell.getWindowList();
        for (IsoWindow isoWindow : isoWindowList.toArray(new IsoWindow[0])) {
            isoWindow.addBarricadesDebug(0, true);
            IsoBarricade isoBarricade = isoWindow.getBarricadeOnSameSquare();
            if (isoBarricade != null) {
                isoBarricade.setNumberOfPlanks(0);
                isoBarricade.metalBarHealth = 3000;
                isoBarricade.updateSprite();
            }
            if ((isoBarricade = isoWindow.getBarricadeOnOppositeSquare()) == null) continue;
            isoBarricade.setNumberOfPlanks(0);
            isoBarricade.metalBarHealth = 3000;
            isoBarricade.updateSprite();
        }
        IsoBarricade.recalculateLighting();
    }

    public static void barricadeCurrentCellWithPlanks(int numberOfPlanks) {
        ArrayList<IsoWindow> isoWindowList = IsoWorld.instance.currentCell.getWindowList();
        for (IsoWindow isoWindow : isoWindowList.toArray(new IsoWindow[0])) {
            isoWindow.addBarricadesDebug(numberOfPlanks, false);
            IsoBarricade isoBarricade = isoWindow.getBarricadeOnSameSquare();
            if (isoBarricade != null) {
                isoBarricade.setNumberOfPlanks(numberOfPlanks);
                isoBarricade.updateSprite();
            }
            if ((isoBarricade = isoWindow.getBarricadeOnOppositeSquare()) == null) continue;
            isoBarricade.setNumberOfPlanks(numberOfPlanks);
            isoBarricade.updateSprite();
        }
        IsoBarricade.recalculateLighting();
    }

    private void setNumberOfPlanks(int numberOfPlanks) {
        this.metalBarHealth = 0;
        this.metalHealth = 0;
        for (int i = 0; i < 4; ++i) {
            this.plankHealth[i] = i < numberOfPlanks ? 1000 : 0;
        }
    }

    private static void recalculateLighting() {
        if (!GameServer.server) {
            for (int pn = 0; pn < IsoPlayer.numPlayers; ++pn) {
                LosUtil.cachecleared[pn] = true;
            }
            IsoGridSquare.setRecalcLightTime(-1.0f);
            GameTime.instance.lightSourceUpdate = 100.0f;
        }
    }

    private void updateSprite() {
        this.chooseSprite();
        if (this.square != null) {
            this.square.RecalcProperties();
        }
        this.invalidateRenderChunkLevel(256L);
    }

    public void addFromCraftRecipe(IsoGameCharacter chr, ArrayList<InventoryItem> items) {
        for (int i = 0; i < items.size(); ++i) {
            InventoryItem item = items.get(i);
            if (item.getFullType().equals("Base.Plank")) {
                this.addPlank(chr, item);
                continue;
            }
            if (item.getFullType().equals("Base.MetalBar") || item.getFullType().equals("Base.IronBar") || item.getFullType().equals("Base.SteelBar")) {
                this.addMetalBar(chr, item);
                continue;
            }
            if (!item.getFullType().equals("Base.SheetMetal")) continue;
            this.addMetal(chr, item);
        }
    }

    public float getLightTransmission() {
        if (this.isMetal()) {
            return 0.0f;
        }
        if (this.isMetalBar()) {
            return 1.0f;
        }
        return 1.0f - (float)this.getNumPlanks() * 0.15f;
    }
}

