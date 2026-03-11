/*
 * Decompiled with CFR 0.152.
 */
package zombie.ui;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import zombie.IndieGL;
import zombie.UsedFromLua;
import zombie.characters.IsoPlayer;
import zombie.characters.IsoZombie;
import zombie.core.Core;
import zombie.core.PerformanceSettings;
import zombie.core.SpriteRenderer;
import zombie.core.textures.Texture;
import zombie.iso.BuildingDef;
import zombie.iso.IsoCamera;
import zombie.iso.IsoMetaCell;
import zombie.iso.IsoMetaGrid;
import zombie.iso.IsoMovingObject;
import zombie.iso.IsoUtils;
import zombie.iso.IsoWorld;
import zombie.iso.RoomDef;
import zombie.network.GameClient;
import zombie.popman.ZombiePopulationManager;
import zombie.ui.UIElement;

@UsedFromLua
public final class RadarPanel
extends UIElement {
    private final int playerIndex;
    private float xPos;
    private float yPos;
    private float offx;
    private float offy;
    private float zoom;
    private float draww;
    private float drawh;
    private final Texture mask;
    private final Texture border;
    private final ArrayList<ZombiePos> zombiePos = new ArrayList();
    private final ZombiePosPool zombiePosPool = new ZombiePosPool();
    private int zombiePosFrameCount;
    private final boolean[] zombiePosOccupied = new boolean[360];

    public RadarPanel(int playerIndex) {
        this.setX(IsoCamera.getScreenLeft(playerIndex) + 20);
        this.setY(IsoCamera.getScreenTop(playerIndex) + IsoCamera.getScreenHeight(playerIndex) - 120 - 20);
        this.setWidth(120.0);
        this.setHeight(120.0);
        this.mask = Texture.getSharedTexture("media/ui/RadarMask.png");
        this.border = Texture.getSharedTexture("media/ui/RadarBorder.png");
        this.playerIndex = playerIndex;
    }

    @Override
    public void update() {
        int dy = 0;
        if (IsoPlayer.players[this.playerIndex] != null && IsoPlayer.players[this.playerIndex].getJoypadBind() != -1) {
            dy = -72;
        }
        this.setX(IsoCamera.getScreenLeft(this.playerIndex) + 20);
        this.setY((double)(IsoCamera.getScreenTop(this.playerIndex) + IsoCamera.getScreenHeight(this.playerIndex)) - this.getHeight() - 20.0 + (double)dy);
    }

    @Override
    public void render() {
        if (!this.isVisible().booleanValue()) {
            return;
        }
        if (IsoPlayer.players[this.playerIndex] == null) {
            return;
        }
        if (GameClient.client) {
            return;
        }
        this.draww = this.getWidth().intValue();
        this.drawh = this.getHeight().intValue();
        this.xPos = IsoPlayer.players[this.playerIndex].getX();
        this.yPos = IsoPlayer.players[this.playerIndex].getY();
        this.offx = this.getAbsoluteX().intValue();
        this.offy = this.getAbsoluteY().intValue();
        this.zoom = 3.0f;
        this.stencilOn();
        SpriteRenderer.instance.render(null, this.offx, this.offy, this.getWidth().intValue(), this.drawh, 0.0f, 0.2f, 0.0f, 0.66f, null);
        this.renderBuildings();
        this.renderRect(this.xPos - 0.5f, this.yPos - 0.5f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f);
        this.stencilOff();
        this.renderZombies();
        SpriteRenderer.instance.render(this.border, this.offx - 4.0f, this.offy - 4.0f, this.draww + 8.0f, this.drawh + 8.0f, 1.0f, 1.0f, 1.0f, 0.25f, null);
    }

    private void stencilOn() {
        IndieGL.glStencilMask(255);
        IndieGL.glClear(1280);
        IndieGL.enableStencilTest();
        IndieGL.glStencilFunc(519, 128, 255);
        IndieGL.glStencilOp(7680, 7680, 7681);
        IndieGL.enableAlphaTest();
        IndieGL.glAlphaFunc(516, 0.1f);
        IndieGL.glColorMask(false, false, false, false);
        SpriteRenderer.instance.renderi(this.mask, (int)this.x, (int)this.y, (int)this.width, (int)this.height, 1.0f, 1.0f, 1.0f, 1.0f, null);
        IndieGL.glColorMask(true, true, true, true);
        IndieGL.glAlphaFunc(516, 0.0f);
        IndieGL.glStencilFunc(514, 128, 128);
        IndieGL.glStencilOp(7680, 7680, 7680);
    }

    private void stencilOff() {
        IndieGL.glAlphaFunc(519, 0.0f);
        IndieGL.disableStencilTest();
        IndieGL.disableAlphaTest();
        IndieGL.glStencilFunc(519, 255, 255);
        IndieGL.glStencilOp(7680, 7680, 7680);
        IndieGL.glClear(1280);
    }

    private void renderBuildings() {
        IsoMetaGrid metaGrid = IsoWorld.instance.metaGrid;
        int minX = (int)((this.xPos - 100.0f) / 256.0f) - metaGrid.minX;
        int minY = (int)((this.yPos - 100.0f) / 256.0f) - metaGrid.minY;
        int maxX = (int)((this.xPos + 100.0f) / 256.0f) - metaGrid.minX;
        int maxY = (int)((this.yPos + 100.0f) / 256.0f) - metaGrid.minY;
        minX = Math.max(minX, 0);
        minY = Math.max(minY, 0);
        maxX = Math.min(maxX, metaGrid.gridX() - 1);
        maxY = Math.min(maxY, metaGrid.gridY() - 1);
        for (int xx = minX; xx <= maxX; ++xx) {
            for (int y = minY; y <= maxY; ++y) {
                IsoMetaCell metaCell;
                if (!metaGrid.hasCell(xx, y) || (metaCell = metaGrid.getCell(xx, y)) == null) continue;
                for (int n = 0; n < metaCell.buildings.size(); ++n) {
                    BuildingDef def = metaCell.buildings.get(n);
                    for (int r = 0; r < def.rooms.size(); ++r) {
                        if (def.rooms.get((int)r).level > 0) continue;
                        ArrayList<RoomDef.RoomRect> rects = def.rooms.get(r).getRects();
                        for (int rr = 0; rr < rects.size(); ++rr) {
                            RoomDef.RoomRect rect = rects.get(rr);
                            this.renderRect(rect.getX(), rect.getY(), rect.getW(), rect.getH(), 0.5f, 0.5f, 0.8f, 0.3f);
                        }
                    }
                }
            }
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     * Enabled aggressive block sorting
     * Enabled unnecessary exception pruning
     * Enabled aggressive exception aggregation
     * Converted monitor instructions to comments
     * Lifted jumps to return sites
     */
    private void renderZombies() {
        int i;
        float x0 = this.offx + this.draww / 2.0f;
        float y0 = this.offy + this.drawh / 2.0f;
        float radius = this.draww / 2.0f;
        float screenWH = 0.5f * this.zoom;
        if (++this.zombiePosFrameCount >= PerformanceSettings.getLockFPS() / 5) {
            Object zombie;
            this.zombiePosFrameCount = 0;
            this.zombiePosPool.release(this.zombiePos);
            this.zombiePos.clear();
            Arrays.fill(this.zombiePosOccupied, false);
            ArrayList<IsoZombie> zombies = IsoWorld.instance.currentCell.getZombieList();
            for (i = 0; i < zombies.size(); ++i) {
                float y1;
                zombie = zombies.get(i);
                float x1 = this.worldToScreenX(((IsoMovingObject)zombie).getX());
                float dist = IsoUtils.DistanceToSquared(x0, y0, x1, y1 = this.worldToScreenY(((IsoMovingObject)zombie).getY()));
                if (dist > radius * radius) {
                    double radians = Math.atan2(y1 - y0, x1 - x0) + Math.PI;
                    double degrees = (Math.toDegrees(radians) + 180.0) % 360.0;
                    this.zombiePosOccupied[(int)degrees] = true;
                    continue;
                }
                this.zombiePos.add(this.zombiePosPool.alloc(((IsoMovingObject)zombie).getX(), ((IsoMovingObject)zombie).getY()));
            }
            if (Core.lastStand) {
                if (ZombiePopulationManager.instance.radarXy == null) {
                    ZombiePopulationManager.instance.radarXy = new float[2048];
                }
                float[] zpos = ZombiePopulationManager.instance.radarXy;
                zombie = zpos;
                // MONITORENTER : zpos
                for (int i2 = 0; i2 < ZombiePopulationManager.instance.radarCount; ++i2) {
                    float y1;
                    float zx = zpos[i2 * 2 + 0];
                    float zy = zpos[i2 * 2 + 1];
                    float x1 = this.worldToScreenX(zx);
                    float dist = IsoUtils.DistanceToSquared(x0, y0, x1, y1 = this.worldToScreenY(zy));
                    if (dist > radius * radius) {
                        double radians = Math.atan2(y1 - y0, x1 - x0) + Math.PI;
                        double degrees = (Math.toDegrees(radians) + 180.0) % 360.0;
                        this.zombiePosOccupied[(int)degrees] = true;
                        continue;
                    }
                    this.zombiePos.add(this.zombiePosPool.alloc(zx, zy));
                }
                ZombiePopulationManager.instance.radarRenderFlag = true;
                // MONITOREXIT : zombie
            }
        }
        int size = this.zombiePos.size();
        for (i = 0; i < size; ++i) {
            ZombiePos zpos = this.zombiePos.get(i);
            this.renderRect(zpos.x - 0.5f, zpos.y - 0.5f, 1.0f, 1.0f, 1.0f, 0.0f, 0.0f, 1.0f);
        }
        i = 0;
        while (i < this.zombiePosOccupied.length) {
            if (this.zombiePosOccupied[i]) {
                double radians = Math.toRadians((float)i / (float)this.zombiePosOccupied.length * 360.0f);
                SpriteRenderer.instance.render(null, x0 + (radius + 1.0f) * (float)Math.cos(radians) - screenWH, y0 + (radius + 1.0f) * (float)Math.sin(radians) - screenWH, 1.0f * this.zoom, 1.0f * this.zoom, 1.0f, 1.0f, 0.0f, 1.0f, null);
            }
            ++i;
        }
    }

    private float worldToScreenX(float x) {
        x -= this.xPos;
        x *= this.zoom;
        x += this.offx;
        return x += this.draww / 2.0f;
    }

    private float worldToScreenY(float y) {
        y -= this.yPos;
        y *= this.zoom;
        y += this.offy;
        return y += this.drawh / 2.0f;
    }

    private void renderRect(float x, float y, float w, float h, float r, float g, float b, float a) {
        float x1 = this.worldToScreenX(x);
        float y1 = this.worldToScreenY(y);
        float x2 = this.worldToScreenX(x + w);
        float y2 = this.worldToScreenY(y + h);
        w = x2 - x1;
        h = y2 - y1;
        if (x1 >= this.offx + this.draww || x2 < this.offx || y1 >= this.offy + this.drawh || y2 < this.offy) {
            return;
        }
        SpriteRenderer.instance.render(null, x1, y1, w, h, r, g, b, a, null);
    }

    private static class ZombiePosPool {
        private final ArrayDeque<ZombiePos> pool = new ArrayDeque();

        private ZombiePosPool() {
        }

        public ZombiePos alloc(float x, float y) {
            return this.pool.isEmpty() ? new ZombiePos(x, y) : this.pool.pop().set(x, y);
        }

        public void release(Collection<ZombiePos> other) {
            this.pool.addAll(other);
        }
    }

    private static final class ZombiePos {
        public float x;
        public float y;

        public ZombiePos(float x, float y) {
            this.x = x;
            this.y = y;
        }

        public ZombiePos set(float x, float y) {
            this.x = x;
            this.y = y;
            return this;
        }
    }
}

