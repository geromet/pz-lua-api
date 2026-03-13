/*
 * Decompiled with CFR 0.152.
 */
package zombie.iso;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import zombie.UsedFromLua;
import zombie.characters.IsoGameCharacter;
import zombie.characters.IsoPlayer;
import zombie.characters.IsoSurvivor;
import zombie.core.Core;
import zombie.core.PerformanceSettings;
import zombie.core.logger.ExceptionLogger;
import zombie.core.math.PZMath;
import zombie.entity.ComponentType;
import zombie.entity.components.spriteconfig.SpriteConfig;
import zombie.entity.components.ui.UiConfig;
import zombie.input.Mouse;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoMovingObject;
import zombie.iso.IsoObject;
import zombie.iso.IsoUtils;
import zombie.iso.IsoWorld;
import zombie.iso.SpriteDetails.IsoFlagType;
import zombie.iso.SpriteDetails.IsoObjectType;
import zombie.iso.Vector2;
import zombie.iso.fboRenderChunk.FBORenderObjectPicker;
import zombie.iso.objects.IsoCurtain;
import zombie.iso.objects.IsoDeadBody;
import zombie.iso.objects.IsoDoor;
import zombie.iso.objects.IsoGenerator;
import zombie.iso.objects.IsoLightSwitch;
import zombie.iso.objects.IsoThumpable;
import zombie.iso.objects.IsoTree;
import zombie.iso.objects.IsoWaveSignal;
import zombie.iso.objects.IsoWindow;
import zombie.iso.objects.IsoWindowFrame;
import zombie.iso.sprite.IsoSprite;
import zombie.scripting.ui.XuiSkin;
import zombie.util.Type;
import zombie.vehicles.BaseVehicle;

@UsedFromLua
public final class IsoObjectPicker {
    public static final IsoObjectPicker Instance = new IsoObjectPicker();
    static final ArrayList<ClickObject> choices = new ArrayList();
    static final Vector2 tempo = new Vector2();
    static final Vector2 tempo2 = new Vector2();
    public static final Comparator<ClickObject> comp = new Comparator<ClickObject>(){

        @Override
        public int compare(ClickObject a, ClickObject b) {
            int bScore;
            int aScore = a.getScore();
            if (aScore > (bScore = b.getScore())) {
                return 1;
            }
            if (aScore < bScore) {
                return -1;
            }
            if (a.tile != null && a.tile.square != null && b.tile != null && a.tile.square == b.tile.square) {
                return a.tile.getObjectIndex() - b.tile.getObjectIndex();
            }
            return 0;
        }
    };
    public ClickObject[] clickObjectStore = new ClickObject[15000];
    public int count;
    public int counter;
    public int maxcount;
    public final ArrayList<ClickObject> thisFrame = new ArrayList();
    public boolean dirty = true;
    public float xOffSinceDirty;
    public float yOffSinceDirty;
    public boolean wasDirty;
    ClickObject lastPickObject;
    float lx;
    float ly;

    public IsoObjectPicker getInstance() {
        return Instance;
    }

    public void Add(int x, int y, int width, int height, IsoGridSquare gridSquare, IsoObject tile, boolean flip, float scaleX, float scaleY) {
        boolean bl;
        if ((float)(x + width) <= this.lx - 32.0f || (float)x >= this.lx + 32.0f || (float)(y + height) <= this.ly - 32.0f || (float)y >= this.ly + 32.0f) {
            return;
        }
        if (this.thisFrame.size() >= 15000) {
            return;
        }
        if (tile.noPicking) {
            return;
        }
        if (tile instanceof IsoSurvivor) {
            bl = false;
        }
        if (tile instanceof IsoDoor) {
            bl = false;
        }
        if (x > Core.getInstance().getOffscreenWidth(0)) {
            return;
        }
        if (y > Core.getInstance().getOffscreenHeight(0)) {
            return;
        }
        if (x + width < 0) {
            return;
        }
        if (y + height < 0) {
            return;
        }
        ClickObject obj = this.clickObjectStore[this.thisFrame.size()];
        this.thisFrame.add(obj);
        this.count = this.thisFrame.size();
        obj.x = x;
        obj.y = y;
        obj.width = width;
        obj.height = height;
        obj.square = gridSquare;
        obj.tile = tile;
        obj.flip = flip;
        obj.scaleX = scaleX;
        obj.scaleY = scaleY;
        if (obj.tile instanceof IsoGameCharacter) {
            obj.flip = false;
        }
        if (this.count > this.maxcount) {
            this.maxcount = this.count;
        }
    }

    public void Init() {
        this.thisFrame.clear();
        this.lastPickObject = null;
        for (int n = 0; n < 15000; ++n) {
            this.clickObjectStore[n] = new ClickObject();
        }
    }

    public ClickObject ContextPick(int screenX, int screenY) {
        if (PerformanceSettings.fboRenderChunk) {
            return FBORenderObjectPicker.getInstance().ContextPick(screenX, screenY);
        }
        float x = (float)screenX * Core.getInstance().getZoom(0);
        float y = (float)screenY * Core.getInstance().getZoom(0);
        choices.clear();
        ++this.counter;
        for (int n = this.thisFrame.size() - 1; n >= 0; --n) {
            IsoThumpable isoThumpable;
            IsoObject isoObject;
            ClickObject obj = this.thisFrame.get(n);
            if (obj.tile instanceof IsoPlayer && obj.tile == IsoPlayer.players[0] || obj.tile.sprite != null && (obj.tile.getTargetAlpha(0) == 0.0f || !(!obj.tile.sprite.properties.has(IsoFlagType.cutW) && !obj.tile.sprite.properties.has(IsoFlagType.cutN) || obj.tile instanceof IsoWindow || (isoObject = obj.tile) instanceof IsoThumpable && (isoThumpable = (IsoThumpable)isoObject).isDoor()) && obj.tile.getTargetAlpha(0) < 1.0f)) continue;
            if (obj.tile == null || obj.tile.sprite != null) {
                // empty if block
            }
            if (!(x > (float)obj.x) || !(y > (float)obj.y) || !(x <= (float)(obj.x + obj.width)) || !(y <= (float)(obj.y + obj.height)) || obj.tile instanceof IsoPlayer) continue;
            if (obj.scaleX != 1.0f || obj.scaleY != 1.0f) {
                float x1 = (float)obj.x + (x - (float)obj.x) / obj.scaleX;
                float y1 = (float)obj.y + (y - (float)obj.y) / obj.scaleY;
                if (!obj.tile.isMaskClicked((int)(x1 - (float)obj.x), (int)(y1 - (float)obj.y), obj.flip)) continue;
                if (obj.tile.rerouteMask != null) {
                    obj.tile = obj.tile.rerouteMask;
                }
                obj.lx = PZMath.fastfloor(x) - obj.x;
                obj.ly = PZMath.fastfloor(y) - obj.y;
                this.lastPickObject = obj;
                choices.add(obj);
                continue;
            }
            if (!obj.tile.isMaskClicked((int)(x - (float)obj.x), (int)(y - (float)obj.y), obj.flip)) continue;
            if (obj.tile.rerouteMask != null) {
                obj.tile = obj.tile.rerouteMask;
            }
            obj.lx = PZMath.fastfloor(x) - obj.x;
            obj.ly = PZMath.fastfloor(y) - obj.y;
            this.lastPickObject = obj;
            choices.add(obj);
        }
        if (choices.isEmpty()) {
            return null;
        }
        for (int i = 0; i < choices.size(); ++i) {
            ClickObject choice = choices.get(i);
            choice.score = choice.calculateScore();
        }
        try {
            Collections.sort(choices, comp);
        }
        catch (IllegalArgumentException ex) {
            if (Core.debug) {
                ExceptionLogger.logException(ex);
            }
            return null;
        }
        return choices.get(choices.size() - 1);
    }

    public ClickObject Pick(int xx, int yy) {
        float x = xx;
        float y = yy;
        float width = Core.getInstance().getScreenWidth();
        float height = Core.getInstance().getScreenHeight();
        float targetScreenWidth = width * Core.getInstance().getZoom(0);
        float targetScreenHeight = height * Core.getInstance().getZoom(0);
        float offscreenBWidth = Core.getInstance().getOffscreenWidth(0);
        float offscreenBHeight = Core.getInstance().getOffscreenHeight(0);
        float delX = offscreenBWidth / targetScreenWidth;
        float delY = offscreenBHeight / targetScreenHeight;
        x -= width / 2.0f;
        y -= height / 2.0f;
        x /= delX;
        y /= delY;
        x += width / 2.0f;
        y += height / 2.0f;
        ++this.counter;
        for (int n = this.thisFrame.size() - 1; n >= 0; --n) {
            ClickObject obj = this.thisFrame.get(n);
            if (obj.tile.square != null) {
                // empty if block
            }
            if (obj.tile instanceof IsoPlayer || obj.tile.sprite != null && obj.tile.getTargetAlpha(0) == 0.0f) continue;
            if (obj.tile == null || obj.tile.sprite != null) {
                // empty if block
            }
            if (!(x > (float)obj.x) || !(y > (float)obj.y) || !(x <= (float)(obj.x + obj.width)) || !(y <= (float)(obj.y + obj.height))) continue;
            if (obj.tile instanceof IsoSurvivor) {
                boolean dd = false;
                continue;
            }
            if (!obj.tile.isMaskClicked((int)(x - (float)obj.x), (int)(y - (float)obj.y), obj.flip)) continue;
            if (obj.tile.rerouteMask != null) {
                obj.tile = obj.tile.rerouteMask;
            }
            obj.lx = PZMath.fastfloor(x) - obj.x;
            obj.ly = PZMath.fastfloor(y) - obj.y;
            this.lastPickObject = obj;
            return obj;
        }
        return null;
    }

    public void StartRender() {
        float x = Mouse.getX();
        float y = Mouse.getY();
        if (x != this.lx || y != this.ly) {
            this.dirty = true;
        }
        this.lx = x;
        this.ly = y;
        if (this.dirty) {
            this.thisFrame.clear();
            this.count = 0;
            this.wasDirty = true;
            this.dirty = false;
            this.xOffSinceDirty = 0.0f;
            this.yOffSinceDirty = 0.0f;
        } else {
            this.wasDirty = false;
        }
    }

    public IsoMovingObject PickTarget(int xx, int yy) {
        float x = xx;
        float y = yy;
        float width = Core.getInstance().getScreenWidth();
        float height = Core.getInstance().getScreenHeight();
        float targetScreenWidth = width * Core.getInstance().getZoom(0);
        float targetScreenHeight = height * Core.getInstance().getZoom(0);
        float offscreenBWidth = Core.getInstance().getOffscreenWidth(0);
        float offscreenBHeight = Core.getInstance().getOffscreenHeight(0);
        float delX = offscreenBWidth / targetScreenWidth;
        float delY = offscreenBHeight / targetScreenHeight;
        x -= width / 2.0f;
        y -= height / 2.0f;
        x /= delX;
        y /= delY;
        x += width / 2.0f;
        y += height / 2.0f;
        ++this.counter;
        for (int n = this.thisFrame.size() - 1; n >= 0; --n) {
            IsoObject isoObject;
            ClickObject obj = this.thisFrame.get(n);
            if (obj.tile.square != null) {
                // empty if block
            }
            if (obj.tile == IsoPlayer.getInstance() || obj.tile.sprite != null && obj.tile.getTargetAlpha() == 0.0f) continue;
            if (obj.tile == null || obj.tile.sprite != null) {
                // empty if block
            }
            if (!(x > (float)obj.x) || !(y > (float)obj.y) || !(x <= (float)(obj.x + obj.width)) || !(y <= (float)(obj.y + obj.height)) || !((isoObject = obj.tile) instanceof IsoMovingObject)) continue;
            IsoMovingObject isoMovingObject = (IsoMovingObject)isoObject;
            if (!obj.tile.isMaskClicked(PZMath.fastfloor(x - (float)obj.x), PZMath.fastfloor(y - (float)obj.y), obj.flip)) continue;
            if (obj.tile.rerouteMask != null) {
                // empty if block
            }
            obj.lx = PZMath.fastfloor(x - (float)obj.x);
            obj.ly = PZMath.fastfloor(y - (float)obj.y);
            this.lastPickObject = obj;
            return isoMovingObject;
        }
        return null;
    }

    public IsoObject PickDoor(int screenX, int screenY, boolean bTransparent) {
        if (PerformanceSettings.fboRenderChunk) {
            return FBORenderObjectPicker.getInstance().PickDoor(screenX, screenY, bTransparent);
        }
        float x = (float)screenX * Core.getInstance().getZoom(0);
        float y = (float)screenY * Core.getInstance().getZoom(0);
        int playerIndex = IsoPlayer.getPlayerIndex();
        for (int n = this.thisFrame.size() - 1; n >= 0; --n) {
            int ly;
            int lx;
            ClickObject obj = this.thisFrame.get(n);
            if (!(obj.tile instanceof IsoDoor) || obj.tile.getTargetAlpha(playerIndex) == 0.0f || bTransparent != obj.tile.getTargetAlpha(playerIndex) < 1.0f || !(x >= (float)obj.x) || !(y >= (float)obj.y) || !(x < (float)(obj.x + obj.width)) || !(y < (float)(obj.y + obj.height)) || !obj.tile.isMaskClicked(lx = PZMath.fastfloor(x - (float)obj.x), ly = PZMath.fastfloor(y - (float)obj.y), obj.flip)) continue;
            return obj.tile;
        }
        return null;
    }

    public IsoObject PickWindow(int screenX, int screenY) {
        if (PerformanceSettings.fboRenderChunk) {
            return FBORenderObjectPicker.getInstance().PickWindow(screenX, screenY);
        }
        float x = (float)screenX * Core.getInstance().getZoom(0);
        float y = (float)screenY * Core.getInstance().getZoom(0);
        for (int n = this.thisFrame.size() - 1; n >= 0; --n) {
            int ty;
            int ly;
            ClickObject obj = this.thisFrame.get(n);
            if (!(obj.tile instanceof IsoWindow) && !(obj.tile instanceof IsoCurtain) || obj.tile.sprite != null && obj.tile.getTargetAlpha() == 0.0f || !(x >= (float)obj.x) || !(y >= (float)obj.y) || !(x < (float)(obj.x + obj.width)) || !(y < (float)(obj.y + obj.height))) continue;
            int lx = PZMath.fastfloor(x - (float)obj.x);
            if (obj.tile.isMaskClicked(lx, ly = PZMath.fastfloor(y - (float)obj.y), obj.flip)) {
                return obj.tile;
            }
            if (!(obj.tile instanceof IsoWindow)) continue;
            boolean above = false;
            boolean below = false;
            for (ty = ly; ty >= 0; --ty) {
                if (!obj.tile.isMaskClicked(lx, ty)) continue;
                above = true;
                break;
            }
            for (ty = ly; ty < obj.height; ++ty) {
                if (!obj.tile.isMaskClicked(lx, ty)) continue;
                below = true;
                break;
            }
            if (!above || !below) continue;
            return obj.tile;
        }
        return null;
    }

    public IsoObject PickWindowFrame(int screenX, int screenY) {
        if (PerformanceSettings.fboRenderChunk) {
            return FBORenderObjectPicker.getInstance().PickWindowFrame(screenX, screenY);
        }
        float x = (float)screenX * Core.getInstance().getZoom(0);
        float y = (float)screenY * Core.getInstance().getZoom(0);
        for (int n = this.thisFrame.size() - 1; n >= 0; --n) {
            int ty;
            int ly;
            ClickObject obj = this.thisFrame.get(n);
            if (!(obj.tile instanceof IsoWindowFrame) || obj.tile.sprite != null && obj.tile.getTargetAlpha() == 0.0f || !(x >= (float)obj.x) || !(y >= (float)obj.y) || !(x < (float)(obj.x + obj.width)) || !(y < (float)(obj.y + obj.height))) continue;
            int lx = PZMath.fastfloor(x - (float)obj.x);
            if (obj.tile.isMaskClicked(lx, ly = PZMath.fastfloor(y - (float)obj.y), obj.flip)) {
                return obj.tile;
            }
            boolean above = false;
            boolean below = false;
            for (ty = ly; ty >= 0; --ty) {
                if (!obj.tile.isMaskClicked(lx, ty)) continue;
                above = true;
                break;
            }
            for (ty = ly; ty < obj.height; ++ty) {
                if (!obj.tile.isMaskClicked(lx, ty)) continue;
                below = true;
                break;
            }
            if (!above || !below) continue;
            return obj.tile;
        }
        return null;
    }

    public IsoObject PickThumpable(int screenX, int screenY) {
        if (PerformanceSettings.fboRenderChunk) {
            return FBORenderObjectPicker.getInstance().PickThumpable(screenX, screenY);
        }
        float x = (float)screenX * Core.getInstance().getZoom(0);
        float y = (float)screenY * Core.getInstance().getZoom(0);
        for (int n = this.thisFrame.size() - 1; n >= 0; --n) {
            int ly;
            int lx;
            ClickObject obj = this.thisFrame.get(n);
            IsoObject isoObject = obj.tile;
            if (!(isoObject instanceof IsoThumpable)) continue;
            IsoThumpable thump = (IsoThumpable)isoObject;
            if (obj.tile.sprite != null && obj.tile.getTargetAlpha() == 0.0f || !(x >= (float)obj.x) || !(y >= (float)obj.y) || !(x < (float)(obj.x + obj.width)) || !(y < (float)(obj.y + obj.height)) || !obj.tile.isMaskClicked(lx = (int)(x - (float)obj.x), ly = (int)(y - (float)obj.y), obj.flip)) continue;
            return obj.tile;
        }
        return null;
    }

    public IsoObject PickHoppable(int screenX, int screenY) {
        if (PerformanceSettings.fboRenderChunk) {
            return FBORenderObjectPicker.getInstance().PickHoppable(screenX, screenY);
        }
        float x = (float)screenX * Core.getInstance().getZoom(0);
        float y = (float)screenY * Core.getInstance().getZoom(0);
        for (int n = this.thisFrame.size() - 1; n >= 0; --n) {
            int ly;
            int lx;
            ClickObject obj = this.thisFrame.get(n);
            if (!obj.tile.isHoppable() || obj.tile.sprite != null && obj.tile.getTargetAlpha() == 0.0f || !(x >= (float)obj.x) || !(y >= (float)obj.y) || !(x < (float)(obj.x + obj.width)) || !(y < (float)(obj.y + obj.height)) || !obj.tile.isMaskClicked(lx = (int)(x - (float)obj.x), ly = (int)(y - (float)obj.y), obj.flip)) continue;
            return obj.tile;
        }
        return null;
    }

    public IsoObject PickCorpse(int screenX, int screenY) {
        if (PerformanceSettings.fboRenderChunk) {
            return FBORenderObjectPicker.getInstance().PickCorpse(screenX, screenY);
        }
        float x = (float)screenX * Core.getInstance().getZoom(0);
        float y = (float)screenY * Core.getInstance().getZoom(0);
        for (int n = this.thisFrame.size() - 1; n >= 0; --n) {
            IsoDeadBody isoDeadBody;
            ClickObject obj = this.thisFrame.get(n);
            if (!(x >= (float)obj.x) || !(y >= (float)obj.y) || !(x < (float)(obj.x + obj.width)) || !(y < (float)(obj.y + obj.height)) || obj.tile.getTargetAlpha() < 1.0f) continue;
            if (obj.tile.isMaskClicked((int)(x - (float)obj.x), (int)(y - (float)obj.y), obj.flip) && !(obj.tile instanceof IsoWindow)) {
                return null;
            }
            IsoObject isoObject = obj.tile;
            if (!(isoObject instanceof IsoDeadBody) || !(isoDeadBody = (IsoDeadBody)isoObject).isMouseOver(x, y)) continue;
            return obj.tile;
        }
        return null;
    }

    public IsoObject PickTree(int screenX, int screenY) {
        if (PerformanceSettings.fboRenderChunk) {
            return FBORenderObjectPicker.getInstance().PickTree(screenX, screenY);
        }
        float x = (float)screenX * Core.getInstance().getZoom(0);
        float y = (float)screenY * Core.getInstance().getZoom(0);
        for (int n = this.thisFrame.size() - 1; n >= 0; --n) {
            int ly;
            int lx;
            ClickObject obj = this.thisFrame.get(n);
            if (!(obj.tile instanceof IsoTree) || obj.tile.sprite != null && obj.tile.getTargetAlpha() == 0.0f || !(x >= (float)obj.x) || !(y >= (float)obj.y) || !(x < (float)(obj.x + obj.width)) || !(y < (float)(obj.y + obj.height)) || !obj.tile.isMaskClicked(lx = (int)(x - (float)obj.x), ly = (int)(y - (float)obj.y), obj.flip)) continue;
            return obj.tile;
        }
        return null;
    }

    public BaseVehicle PickVehicle(int screenX, int screenY) {
        int z = PZMath.fastfloor(IsoPlayer.players[0].getZ());
        float worldX = IsoUtils.XToIso(screenX, screenY, z);
        float worldY = IsoUtils.YToIso(screenX, screenY, z);
        for (int i = 0; i < IsoWorld.instance.currentCell.getVehicles().size(); ++i) {
            BaseVehicle vehicle = IsoWorld.instance.currentCell.getVehicles().get(i);
            if (!vehicle.isInBounds(worldX, worldY)) continue;
            return vehicle;
        }
        return null;
    }

    public static final class ClickObject {
        public int height;
        public IsoGridSquare square;
        public IsoObject tile;
        public int width;
        public int x;
        public int y;
        public int lx;
        public int ly;
        public float scaleX;
        public float scaleY;
        public boolean flip;
        public int score;

        public int calculateScore() {
            float score = 1.0f;
            IsoPlayer player = IsoPlayer.getInstance();
            IsoGridSquare playerSq = player.getCurrentSquare();
            IsoObjectPicker.tempo.x = (float)this.square.getX() + 0.5f;
            IsoObjectPicker.tempo.y = (float)this.square.getY() + 0.5f;
            IsoObjectPicker.tempo.x -= player.getX();
            IsoObjectPicker.tempo.y -= player.getY();
            tempo.normalize();
            Vector2 vecB = player.getVectorFromDirection(tempo2);
            float angle = vecB.dot(tempo);
            score += Math.abs(angle * 4.0f);
            IsoGridSquare square = this.square;
            IsoObject object = this.tile;
            IsoSprite sprite = object.sprite;
            IsoDoor door = Type.tryCastTo(object, IsoDoor.class);
            IsoThumpable thumpable = Type.tryCastTo(object, IsoThumpable.class);
            if (door != null || thumpable != null && thumpable.isDoor()) {
                score += 6.0f;
                if (door != null && door.isAdjacentToSquare(playerSq) || thumpable != null && thumpable.isAdjacentToSquare(playerSq)) {
                    score += 1.0f;
                }
                if (player.getZ() > (float)square.getZ()) {
                    score -= 1000.0f;
                }
            } else if (object instanceof IsoWindow) {
                score += 4.0f;
                if (player.getZ() > (float)square.getZ()) {
                    score -= 1000.0f;
                }
            } else {
                score = playerSq != null && square.getRoom() == playerSq.getRoom() ? (score += 1.0f) : (score -= 100000.0f);
                if (player.getZ() > (float)square.getZ()) {
                    score -= 1000.0f;
                }
                if (object instanceof IsoPlayer) {
                    score -= 100000.0f;
                } else if (object instanceof IsoThumpable && object.getTargetAlpha() < 0.99f && !this.isInteractiveEntity(object) && (object.getTargetAlpha() < 0.5f || object.getContainer() == null)) {
                    score -= 100000.0f;
                }
                if (object instanceof IsoCurtain) {
                    score += 3.0f;
                } else if (object instanceof IsoLightSwitch) {
                    score += 20.0f;
                } else if (sprite.properties.has(IsoFlagType.bed)) {
                    score += 2.0f;
                } else if (object.container != null) {
                    score += 10.0f;
                } else if (object instanceof IsoGenerator) {
                    score += 11.0f;
                } else if (object instanceof IsoWaveSignal) {
                    score += 20.0f;
                } else if (thumpable != null && thumpable.getLightSource() != null) {
                    score += 3.0f;
                } else if (sprite.properties.has(IsoFlagType.waterPiped)) {
                    score += 3.0f;
                } else if (sprite.properties.has(IsoFlagType.solidfloor)) {
                    score -= 100.0f;
                } else if (sprite.getTileType() == IsoObjectType.WestRoofB) {
                    score -= 100.0f;
                } else if (sprite.getTileType() == IsoObjectType.WestRoofM) {
                    score -= 100.0f;
                } else if (sprite.getTileType() == IsoObjectType.WestRoofT) {
                    score -= 100.0f;
                } else if (sprite.properties.has(IsoFlagType.cutW) || sprite.properties.has(IsoFlagType.cutN)) {
                    score -= 2.0f;
                } else if (this.isInteractiveEntity(object)) {
                    score += 2.0f;
                }
            }
            float dist = IsoUtils.DistanceManhatten((float)square.getX() + 0.5f, (float)square.getY() + 0.5f, player.getX(), player.getY());
            return (int)(score -= dist / 2.0f);
        }

        public int getScore() {
            return this.score;
        }

        public boolean contains(float x, float y) {
            return x >= (float)this.x && y >= (float)this.y && x < (float)(this.x + this.width) && y < (float)(this.y + this.height);
        }

        private boolean isInteractiveEntity(IsoObject object) {
            IsoObject master;
            SpriteConfig spriteConfig = object.getSpriteConfig();
            IsoObject isoObject = master = spriteConfig == null ? null : spriteConfig.getMultiSquareMaster();
            if (master != null && master != object) {
                return this.isInteractiveEntity(master);
            }
            UiConfig uiConfig = (UiConfig)object.getComponent(ComponentType.UiConfig);
            if (uiConfig == null || !uiConfig.isUiEnabled()) {
                return false;
            }
            XuiSkin.EntityUiStyle uiStyle = uiConfig.getEntityUiStyle();
            if (uiStyle != null && uiStyle.getLuaCanOpenWindow() != null) {
                return true;
            }
            return uiStyle != null && uiStyle.getLuaWindowClass() != null;
        }
    }
}

