/*
 * Decompiled with CFR 0.152.
 */
package zombie.iso.objects;

import java.io.IOException;
import java.nio.ByteBuffer;
import org.joml.Vector3f;
import zombie.GameTime;
import zombie.Lua.LuaEventManager;
import zombie.UsedFromLua;
import zombie.characters.IsoGameCharacter;
import zombie.characters.IsoPlayer;
import zombie.core.Core;
import zombie.core.PerformanceSettings;
import zombie.core.math.PZMath;
import zombie.core.network.ByteBufferReader;
import zombie.core.network.ByteBufferWriter;
import zombie.core.opengl.Shader;
import zombie.core.properties.PropertyContainer;
import zombie.core.raknet.UdpConnection;
import zombie.core.textures.ColorInfo;
import zombie.inventory.InventoryItem;
import zombie.iso.ICurtain;
import zombie.iso.IsoCamera;
import zombie.iso.IsoCell;
import zombie.iso.IsoDirections;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoObject;
import zombie.iso.IsoUtils;
import zombie.iso.LosUtil;
import zombie.iso.SpriteDetails.IsoObjectType;
import zombie.iso.SpriteModel;
import zombie.iso.Vector2;
import zombie.iso.objects.IsoBarricade;
import zombie.iso.objects.IsoDoor;
import zombie.iso.objects.IsoThumpable;
import zombie.iso.objects.IsoWindow;
import zombie.iso.objects.IsoWorldInventoryObject;
import zombie.iso.objects.interfaces.BarricadeAble;
import zombie.iso.sprite.IsoSprite;
import zombie.iso.sprite.IsoSpriteManager;
import zombie.network.GameClient;
import zombie.network.GameServer;
import zombie.network.PacketTypes;
import zombie.util.Type;
import zombie.util.list.PZArrayList;

@UsedFromLua
public class IsoCurtain
extends IsoObject
implements ICurtain {
    public boolean barricaded;
    public Integer barricadeMaxStrength = 0;
    public Integer barricadeStrength = 0;
    public Integer health = 1000;
    public boolean locked;
    public Integer maxHealth = 1000;
    public Integer pushedMaxStrength = 0;
    public Integer pushedStrength = 0;
    private IsoSprite closedSprite;
    public boolean north;
    public boolean open;
    private IsoSprite openSprite;
    private final boolean destroyed = false;

    public void removeSheet(IsoGameCharacter chr) {
        this.square.transmitRemoveItemFromSquare(this);
        InventoryItem item = chr.getInventory().AddItem("Base.Sheet");
        if (GameServer.server) {
            GameServer.sendAddItemToContainer(chr.getInventory(), item);
        }
        for (int pn = 0; pn < IsoPlayer.numPlayers; ++pn) {
            LosUtil.cachecleared[pn] = true;
        }
        GameTime.instance.lightSourceUpdate = 100.0f;
        IsoGridSquare.setRecalcLightTime(-1.0f);
    }

    public IsoCurtain(IsoCell cell, IsoGridSquare gridSquare, IsoSprite gid, boolean north, boolean spriteclosed) {
        this.outlineOnMouseover = true;
        this.pushedMaxStrength = this.pushedStrength = Integer.valueOf(2500);
        if (spriteclosed) {
            this.openSprite = IsoSprite.getSprite(IsoSpriteManager.instance, gid, 4);
            this.closedSprite = gid;
        } else {
            this.closedSprite = IsoSprite.getSprite(IsoSpriteManager.instance, gid, -4);
            this.openSprite = gid;
        }
        this.open = true;
        this.sprite = this.openSprite;
        this.square = gridSquare;
        this.north = north;
        this.DirtySlice();
    }

    public IsoCurtain(IsoCell cell, IsoGridSquare gridSquare, String gid, boolean north) {
        this.outlineOnMouseover = true;
        this.pushedMaxStrength = this.pushedStrength = Integer.valueOf(2500);
        this.closedSprite = IsoSprite.getSprite(IsoSpriteManager.instance, gid, -4);
        this.openSprite = IsoSprite.getSprite(IsoSpriteManager.instance, gid, 0);
        this.open = true;
        this.sprite = this.openSprite;
        this.square = gridSquare;
        this.north = north;
        this.DirtySlice();
    }

    public IsoCurtain(IsoCell cell) {
        super(cell);
    }

    @Override
    public String getObjectName() {
        return "Curtain";
    }

    @Override
    public Vector2 getFacingPosition(Vector2 pos) {
        if (this.square == null) {
            return pos.set(0.0f, 0.0f);
        }
        if (this.getType() == IsoObjectType.curtainS) {
            return pos.set(this.getX() + 0.5f, this.getY() + 1.0f);
        }
        if (this.getType() == IsoObjectType.curtainE) {
            return pos.set(this.getX() + 1.0f, this.getY() + 0.5f);
        }
        if (this.north) {
            return pos.set(this.getX() + 0.5f, this.getY());
        }
        return pos.set(this.getX(), this.getY() + 0.5f);
    }

    @Override
    public void load(ByteBuffer input, int worldVersion, boolean isDebugSave) throws IOException {
        super.load(input, worldVersion, isDebugSave);
        this.open = input.get() != 0;
        this.north = input.get() != 0;
        this.health = input.getInt();
        this.barricadeStrength = input.getInt();
        if (this.open) {
            this.closedSprite = IsoSprite.getSprite(IsoSpriteManager.instance, input.getInt());
            this.openSprite = this.sprite;
        } else {
            this.openSprite = IsoSprite.getSprite(IsoSpriteManager.instance, input.getInt());
            this.closedSprite = this.sprite;
        }
    }

    @Override
    public void save(ByteBuffer output, boolean isDebugSave) throws IOException {
        super.save(output, isDebugSave);
        output.put(this.open ? (byte)1 : 0);
        output.put(this.north ? (byte)1 : 0);
        output.putInt(this.health);
        output.putInt(this.barricadeStrength);
        if (this.open) {
            output.putInt(this.closedSprite.id);
        } else {
            output.putInt(this.openSprite.id);
        }
    }

    public boolean getNorth() {
        return this.north;
    }

    public boolean IsOpen() {
        return this.open;
    }

    @Override
    public boolean onMouseLeftClick(int x, int y) {
        return false;
    }

    public boolean canInteractWith(IsoGameCharacter chr) {
        if (chr == null || chr.getCurrentSquare() == null) {
            return false;
        }
        IsoGridSquare chrSq = chr.getCurrentSquare();
        return (this.isAdjacentToSquare(chrSq) || chrSq == this.getOppositeSquare()) && !this.getSquare().isBlockedTo(chrSq);
    }

    public IsoGridSquare getOppositeSquare() {
        if (this.getType() == IsoObjectType.curtainN) {
            return this.getCell().getGridSquare(this.getX(), this.getY() - 1.0f, this.getZ());
        }
        if (this.getType() == IsoObjectType.curtainS) {
            return this.getCell().getGridSquare(this.getX(), this.getY() + 1.0f, this.getZ());
        }
        if (this.getType() == IsoObjectType.curtainW) {
            return this.getCell().getGridSquare(this.getX() - 1.0f, this.getY(), this.getZ());
        }
        if (this.getType() == IsoObjectType.curtainE) {
            return this.getCell().getGridSquare(this.getX() + 1.0f, this.getY(), this.getZ());
        }
        return null;
    }

    public boolean isAdjacentToSquare(IsoGridSquare square1, IsoGridSquare square2) {
        if (square1 == null || square2 == null) {
            return false;
        }
        if (this.getType() == IsoObjectType.curtainN || this.getType() == IsoObjectType.curtainS) {
            return square1.y == square2.y && Math.abs(square1.x - square2.x) <= 1;
        }
        return square1.x == square2.x && Math.abs(square1.y - square2.y) <= 1;
    }

    public boolean isAdjacentToSquare(IsoGridSquare square2) {
        return this.isAdjacentToSquare(this.getSquare(), square2);
    }

    @Override
    public IsoObject.VisionResult TestVision(IsoGridSquare from, IsoGridSquare to) {
        if (to.getZ() != from.getZ()) {
            return IsoObject.VisionResult.NoEffect;
        }
        if (from == this.square && (this.getType() == IsoObjectType.curtainW || this.getType() == IsoObjectType.curtainN) || from != this.square && (this.getType() == IsoObjectType.curtainE || this.getType() == IsoObjectType.curtainS)) {
            if (this.north && to.getY() < from.getY() && !this.open) {
                return IsoObject.VisionResult.Blocked;
            }
            if (!this.north && to.getX() < from.getX() && !this.open) {
                return IsoObject.VisionResult.Blocked;
            }
        } else {
            if (this.north && to.getY() > from.getY() && !this.open) {
                return IsoObject.VisionResult.Blocked;
            }
            if (!this.north && to.getX() > from.getX() && !this.open) {
                return IsoObject.VisionResult.Blocked;
            }
        }
        return IsoObject.VisionResult.NoEffect;
    }

    public void ToggleDoor(IsoGameCharacter chr) {
        if (this.barricaded) {
            return;
        }
        this.DirtySlice();
        if (this.locked && chr != null && chr.getCurrentSquare().getRoom() == null && !this.open) {
            return;
        }
        this.open = !this.open;
        this.sprite = this.closedSprite;
        if (this.open) {
            this.sprite = this.openSprite;
            if (chr != null) {
                chr.playSound(this.getSoundPrefix() + "Open");
            }
        } else if (chr != null) {
            chr.playSound(this.getSoundPrefix() + "Close");
        }
        this.square.RecalcAllWithNeighbours(true);
        this.syncIsoObject(false, this.open ? (byte)1 : 0, null);
        this.invalidateVispolyChunkLevel();
        this.invalidateRenderChunkLevel(256L);
    }

    public void ToggleDoorSilent() {
        if (this.barricaded) {
            return;
        }
        this.DirtySlice();
        for (int pn = 0; pn < IsoPlayer.numPlayers; ++pn) {
            LosUtil.cachecleared[pn] = true;
        }
        GameTime.instance.lightSourceUpdate = 100.0f;
        IsoGridSquare.setRecalcLightTime(-1.0f);
        this.open = !this.open;
        this.sprite = this.closedSprite;
        if (this.open) {
            this.sprite = this.openSprite;
        }
        this.syncIsoObject(false, this.open ? (byte)1 : 0, null);
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    @Override
    public void render(float x, float y, float z, ColorInfo col, boolean bDoAttached, boolean bWallLightingPass, Shader shader) {
        if (!PerformanceSettings.fboRenderChunk) {
            int playerIndex = IsoCamera.frameState.playerIndex;
            IsoObject attachedTo = this.getObjectAttachedTo();
            if (attachedTo != null && this.getSquare().getTargetDarkMulti(playerIndex) <= attachedTo.getSquare().getTargetDarkMulti(playerIndex)) {
                col = attachedTo.getSquare().lighting[playerIndex].lightInfo();
                this.setTargetAlpha(playerIndex, attachedTo.getTargetAlpha(playerIndex));
            }
        }
        IsoDirections dir = switch (this.getType()) {
            case IsoObjectType.curtainN -> IsoDirections.N;
            case IsoObjectType.curtainS -> IsoDirections.S;
            case IsoObjectType.curtainW -> IsoDirections.W;
            case IsoObjectType.curtainE -> IsoDirections.E;
            default -> null;
        };
        Vector3f curtainOffset = null;
        float closestDistSq = Float.MAX_VALUE;
        for (int i = 0; i < this.getSquare().getObjects().size(); ++i) {
            Vector3f curtainOffset1;
            IsoSprite sprite;
            IsoObject object = this.getSquare().getObjects().get(i);
            if (object instanceof IsoWorldInventoryObject || (sprite = object.getSprite()) == null || sprite.getTileType() == IsoObjectType.curtainN || sprite.getTileType() == IsoObjectType.curtainS || sprite.getTileType() == IsoObjectType.curtainW || sprite.getTileType() == IsoObjectType.curtainE || (curtainOffset1 = sprite.getCurtainOffset()) == null) continue;
            float distSq = IsoUtils.DistanceToSquared(curtainOffset1.x(), curtainOffset1.z(), 0.0f, 0.0f);
            if (curtainOffset == null) {
                curtainOffset = curtainOffset1;
                closestDistSq = distSq;
                continue;
            }
            if (!(distSq < closestDistSq)) continue;
            curtainOffset = curtainOffset1;
            closestDistSq = distSq;
        }
        if (PerformanceSettings.fboRenderChunk && curtainOffset != null && this.getSpriteModel() != null) {
            float ox = curtainOffset.x();
            float oy = curtainOffset.z();
            float oz = curtainOffset.y() / 2.44949f;
            if (IsoBarricade.GetBarricadeOnSquare(this.getSquare(), dir) != null) {
                ox = PZMath.max(ox, (float)dir.dx() * 0.4f);
                oy = PZMath.max(oy, (float)dir.dy() * 0.37f);
            }
            SpriteModel spriteModel1 = this.getSpriteModel();
            float x1 = spriteModel1.getTranslate().x();
            float y1 = spriteModel1.getTranslate().y();
            float z1 = spriteModel1.getTranslate().z();
            try {
                spriteModel1.getTranslate().set(0.0f);
                super.render(x + ox, y + oy, z + oz, col, bDoAttached, bWallLightingPass, shader);
            }
            finally {
                spriteModel1.getTranslate().set(x1, y1, z1);
            }
            this.sx = 0.0f;
            return;
        }
        if (IsoBarricade.GetBarricadeOnSquare(this.getSquare(), dir) != null) {
            float offsetX1 = this.offsetX;
            float offsetY1 = this.offsetY;
            this.offsetX += IsoUtils.XToScreen((float)dir.dx() * 0.03f, (float)dir.dy() * 0.03f, 0.0f, 0);
            this.offsetY += IsoUtils.YToScreen((float)dir.dx() * 0.03f, (float)dir.dy() * 0.03f, 0.0f, 0);
            super.render(x, y, z, col, bDoAttached, bWallLightingPass, shader);
            this.offsetX = offsetX1;
            this.offsetY = offsetY1;
            this.sx = 0.0f;
            return;
        }
        super.render(x, y, z, col, bDoAttached, bWallLightingPass, shader);
        this.sx = 0.0f;
    }

    @Override
    public void syncIsoObjectSend(ByteBufferWriter b) {
        b.putInt(this.square.getX());
        b.putInt(this.square.getY());
        b.putInt(this.square.getZ());
        b.putByte(this.square.getObjects().indexOf(this));
        b.putBoolean(true);
        b.putBoolean(this.open);
    }

    @Override
    public void syncIsoObject(boolean bRemote, byte val, UdpConnection source2, ByteBufferReader bb) {
        this.syncIsoObject(bRemote, val, source2);
    }

    public void syncIsoObject(boolean bRemote, byte val, UdpConnection source2) {
        if (this.square == null) {
            System.out.println("ERROR: " + this.getClass().getSimpleName() + " square is null");
            return;
        }
        if (this.getObjectIndex() == -1) {
            System.out.println("ERROR: " + this.getClass().getSimpleName() + " not found on square " + this.square.getX() + "," + this.square.getY() + "," + this.square.getZ());
            return;
        }
        if (GameClient.client && !bRemote) {
            ByteBufferWriter b = GameClient.connection.startPacket();
            PacketTypes.PacketType.SyncIsoObject.doPacket(b);
            this.syncIsoObjectSend(b);
            PacketTypes.PacketType.SyncIsoObject.send(GameClient.connection);
        } else {
            if (bRemote) {
                if (val == 1) {
                    this.open = true;
                    this.sprite = this.openSprite;
                } else {
                    this.open = false;
                    this.sprite = this.closedSprite;
                }
            }
            if (GameServer.server) {
                for (UdpConnection connection : GameServer.udpEngine.connections) {
                    ByteBufferWriter b = connection.startPacket();
                    PacketTypes.PacketType.SyncIsoObject.doPacket(b);
                    this.syncIsoObjectSend(b);
                    PacketTypes.PacketType.SyncIsoObject.send(connection);
                }
            }
        }
        this.square.RecalcProperties();
        this.square.RecalcAllWithNeighbours(true);
        for (int pn = 0; pn < IsoPlayer.numPlayers; ++pn) {
            LosUtil.cachecleared[pn] = true;
        }
        IsoGridSquare.setRecalcLightTime(-1.0f);
        GameTime.instance.lightSourceUpdate = 100.0f;
        ++Core.dirtyGlobalLightsCount;
        LuaEventManager.triggerEvent("OnContainerUpdate");
        if (this.square != null) {
            this.square.RecalcProperties();
        }
        this.invalidateVispolyChunkLevel();
        this.invalidateRenderChunkLevel(256L);
        this.flagForHotSave();
    }

    public IsoObject getObjectAttachedTo() {
        block4: {
            IsoGridSquare sq;
            PZArrayList<IsoObject> objects;
            block3: {
                int index = this.getObjectIndex();
                if (index == -1) {
                    return null;
                }
                objects = this.getSquare().getObjects();
                if (this.getType() != IsoObjectType.curtainW && this.getType() != IsoObjectType.curtainN) break block3;
                boolean north = this.getType() == IsoObjectType.curtainN;
                for (int i = index - 1; i >= 0; --i) {
                    BarricadeAble obj = Type.tryCastTo(objects.get(i), BarricadeAble.class);
                    if (obj == null || north != obj.getNorth()) continue;
                    return objects.get(i);
                }
                break block4;
            }
            if (this.getType() != IsoObjectType.curtainE && this.getType() != IsoObjectType.curtainS || (sq = this.getOppositeSquare()) == null) break block4;
            boolean north = this.getType() == IsoObjectType.curtainS;
            objects = sq.getObjects();
            for (int i = objects.size() - 1; i >= 0; --i) {
                BarricadeAble obj = Type.tryCastTo(objects.get(i), BarricadeAble.class);
                if (obj == null || north != obj.getNorth()) continue;
                return objects.get(i);
            }
        }
        return null;
    }

    public String getSoundPrefix() {
        if (this.closedSprite == null) {
            return "CurtainShort";
        }
        PropertyContainer props = this.closedSprite.getProperties();
        if (props.has("CurtainSound")) {
            return "Curtain" + props.get("CurtainSound");
        }
        return "CurtainShort";
    }

    public static boolean isSheet(IsoObject curtain) {
        if (curtain instanceof IsoDoor) {
            IsoDoor isoDoor = (IsoDoor)curtain;
            curtain = isoDoor.HasCurtains();
        }
        if (curtain instanceof IsoThumpable) {
            IsoThumpable isoThumpable = (IsoThumpable)curtain;
            curtain = isoThumpable.HasCurtains();
        }
        if (curtain instanceof IsoWindow) {
            IsoWindow isoWindow = (IsoWindow)curtain;
            curtain = isoWindow.HasCurtains();
        }
        if (curtain == null || curtain.getSprite() == null) {
            return false;
        }
        IsoSprite sprite = curtain.getSprite();
        if (sprite.getProperties().has("CurtainSound")) {
            return "Sheet".equals(sprite.getProperties().get("CurtainSound"));
        }
        return false;
    }

    @Override
    public boolean isCurtainOpen() {
        return this.IsOpen();
    }
}

