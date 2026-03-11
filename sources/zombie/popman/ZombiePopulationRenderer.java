/*
 * Decompiled with CFR 0.152.
 */
package zombie.popman;

import java.io.File;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Objects;
import zombie.GameWindow;
import zombie.MapCollisionData;
import zombie.UsedFromLua;
import zombie.ZomboidFileSystem;
import zombie.ai.states.WalkTowardState;
import zombie.characters.IsoPlayer;
import zombie.characters.IsoZombie;
import zombie.config.BooleanConfigOption;
import zombie.config.ConfigFile;
import zombie.config.ConfigOption;
import zombie.core.Core;
import zombie.core.SpriteRenderer;
import zombie.core.Translator;
import zombie.core.math.PZMath;
import zombie.core.opengl.VBORenderer;
import zombie.core.stash.StashSystem;
import zombie.core.textures.TextureDraw;
import zombie.input.Mouse;
import zombie.iso.BuildingDef;
import zombie.iso.IsoCell;
import zombie.iso.IsoChunk;
import zombie.iso.IsoChunkMap;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoMetaCell;
import zombie.iso.IsoMetaGrid;
import zombie.iso.IsoMovingObject;
import zombie.iso.IsoWorld;
import zombie.iso.LotHeader;
import zombie.iso.RoomDef;
import zombie.iso.SpawnPoints;
import zombie.network.GameClient;
import zombie.popman.MPDebugInfo;
import zombie.ui.TextManager;
import zombie.ui.UIElement;
import zombie.ui.UIFont;
import zombie.vehicles.VehiclesDB2;

@UsedFromLua
public final class ZombiePopulationRenderer {
    private float xPos;
    private float yPos;
    private float offx;
    private float offy;
    private float zoom;
    private float draww;
    private float drawh;
    static final byte RENDER_RECT_FILLED = 0;
    static final byte RENDER_RECT_OUTLINE = 1;
    static final byte RENDER_LINE = 2;
    static final byte RENDER_CIRCLE = 3;
    static final byte RENDER_TEXT = 4;
    private final Drawer[] drawers = new Drawer[3];
    private DrawerImpl currentDrawer;
    private final DrawerImpl textDrawer = new DrawerImpl();
    private static final int VERSION = 1;
    private final ArrayList<ConfigOption> options = new ArrayList();
    private final BooleanDebugOption cellGrid = new BooleanDebugOption(this, "CellGrid.256x256", true);
    private final BooleanDebugOption cellGrid300 = new BooleanDebugOption(this, "CellGrid.300x300", true);
    private final BooleanDebugOption cellInfo = new BooleanDebugOption(this, "CellInfo", true);
    private final BooleanDebugOption metaGridBuildings = new BooleanDebugOption(this, "MetaGrid.Buildings", true);
    private final BooleanDebugOption zombiesReal = new BooleanDebugOption(this, "Zombies.Real", true);
    private final BooleanDebugOption zombiesStanding = new BooleanDebugOption(this, "Zombies.Standing", true);
    private final BooleanDebugOption zombiesMoving = new BooleanDebugOption(this, "Zombies.Moving", true);
    private final BooleanDebugOption mcdObstacles = new BooleanDebugOption(this, "MapCollisionData.Obstacles", true);
    private final BooleanDebugOption mcdRegularChunkOutlines = new BooleanDebugOption(this, "MapCollisionData.RegularChunkOutlines", true);
    private final BooleanDebugOption mcdRooms = new BooleanDebugOption(this, "MapCollisionData.Rooms", true);
    private final BooleanDebugOption vehicles = new BooleanDebugOption(this, "Vehicles", true);
    private final BooleanDebugOption zombieIntensity = new BooleanDebugOption(this, "ZombieIntensity", false);

    private native void n_render(float var1, int var2, int var3, float var4, float var5, int var6, int var7);

    private native void n_setWallFollowerStart(int var1, int var2);

    private native void n_setWallFollowerEnd(int var1, int var2);

    private native void n_wallFollowerMouseMove(int var1, int var2);

    private native void n_setDebugOption(String var1, String var2);

    public ZombiePopulationRenderer() {
        for (int i = 0; i < this.drawers.length; ++i) {
            this.drawers[i] = new Drawer();
        }
    }

    public float worldToScreenX(float x) {
        x -= this.xPos;
        x *= this.zoom;
        x += this.offx;
        return x += this.draww / 2.0f;
    }

    public float worldToScreenY(float y) {
        y -= this.yPos;
        y *= this.zoom;
        y += this.offy;
        return y += this.drawh / 2.0f;
    }

    public float uiToWorldX(float x) {
        x -= this.draww / 2.0f;
        x /= this.zoom;
        return x += this.xPos;
    }

    public float uiToWorldY(float y) {
        y -= this.drawh / 2.0f;
        y /= this.zoom;
        return y += this.yPos;
    }

    public void renderString(float x, float y, String str, double r, double g, double b, double a) {
        float tx = this.worldToScreenX(x);
        float ty = this.worldToScreenY(y);
        this.currentDrawer.renderRectFilledUI(tx - 2.0f, ty - 2.0f, TextManager.instance.MeasureStringX(UIFont.Small, str) + 4, TextManager.instance.font.getLineHeight() + 4, 0.0f, 0.0f, 0.0f, 0.75f);
        this.textDrawer.renderStringUI(tx, ty, str, r, g, b, a);
    }

    public void renderRect(float x, float y, float w, float h, float r, float g, float b, float a) {
        float x1 = this.worldToScreenX(x);
        float y1 = this.worldToScreenY(y);
        float x2 = this.worldToScreenX(x + w);
        float y2 = this.worldToScreenY(y + h);
        w = x2 - x1;
        h = y2 - y1;
        if (x1 >= this.offx + this.draww || x2 < this.offx || y1 >= this.offy + this.drawh || y2 < this.offy) {
            return;
        }
        this.currentDrawer.renderRectFilledUI(x1, y1, w, h, r, g, b, a);
    }

    public void renderLine(float x1, float y1, float x2, float y2, float r, float g, float b, float a) {
        float sx1 = this.worldToScreenX(x1);
        float sy1 = this.worldToScreenY(y1);
        float sx2 = this.worldToScreenX(x2);
        float sy2 = this.worldToScreenY(y2);
        if (sx1 >= (float)Core.getInstance().getScreenWidth() && sx2 >= (float)Core.getInstance().getScreenWidth() || sy1 >= (float)Core.getInstance().getScreenHeight() && sy2 >= (float)Core.getInstance().getScreenHeight() || sx1 < 0.0f && sx2 < 0.0f || sy1 < 0.0f && sy2 < 0.0f) {
            return;
        }
        this.currentDrawer.renderLineUI(sx1, sy1, sx2, sy2, r, g, b, a);
    }

    public void renderCircle(float x, float y, float radius, float r, float g, float b, float a) {
        int segments = 32;
        double lx = (double)x + (double)radius * Math.cos(Math.toRadians(0.0));
        double ly = (double)y + (double)radius * Math.sin(Math.toRadians(0.0));
        for (int i = 1; i <= 32; ++i) {
            double cx = (double)x + (double)radius * Math.cos(Math.toRadians((float)i * 360.0f / 32.0f));
            double cy = (double)y + (double)radius * Math.sin(Math.toRadians((float)i * 360.0f / 32.0f));
            int sx1 = (int)this.worldToScreenX((float)lx);
            int sy1 = (int)this.worldToScreenY((float)ly);
            int sx2 = (int)this.worldToScreenX((float)cx);
            int sy2 = (int)this.worldToScreenY((float)cy);
            this.currentDrawer.renderLineUI(sx1, sy1, sx2, sy2, r, g, b, a);
            lx = cx;
            ly = cy;
        }
    }

    public void renderZombie(float x, float y, float r, float g, float b) {
        if (this.zoom < 0.2f) {
            return;
        }
        float zombieSize = 1.0f / this.zoom + 0.5f;
        this.renderRect(x - zombieSize / 2.0f, y - zombieSize / 2.0f, zombieSize, zombieSize, r, g, b, 1.0f);
    }

    public void renderVehicle(int sqlid, float x, float y, float r, float g, float b) {
        float zombieSize = 2.0f / this.zoom + 0.5f;
        this.renderRect(x - zombieSize / 2.0f, y - zombieSize / 2.0f, zombieSize, zombieSize, r, g, b, 1.0f);
        this.renderString(x, y, String.format("%d", sqlid), r, g, b, 1.0);
    }

    public void outlineRect(float x, float y, float w, float h, float r, float g, float b, float a) {
        this.renderLine(x, y, x + w, y, r, g, b, a);
        this.renderLine(x + w, y, x + w, y + h, r, g, b, a);
        this.renderLine(x, y + h, x + w, y + h, r, g, b, a);
        this.renderLine(x, y, x, y + h, r, g, b, a);
    }

    public void renderCellInfo(int cellX, int cellY, int effectivePopulation, int targetPopulation, float lastRepopTime) {
        if (!this.cellInfo.getValue()) {
            return;
        }
        float tx = this.worldToScreenX(cellX * 256) + 4.0f;
        float ty = this.worldToScreenY(cellY * 256) + 4.0f;
        String newLine = System.getProperty("line.separator");
        String str = Translator.getText("IGUI_ZombiePopulation_PopulationEffective") + ": " + effectivePopulation + newLine + Translator.getText("IGUI_ZombiePopulation_PopulationTarget") + ": " + targetPopulation;
        if (lastRepopTime > 0.0f) {
            str = str + newLine + Translator.getText("IGUI_ZombiePopulation_LastRepopTime") + ": " + String.format(" %.2f", Float.valueOf(lastRepopTime));
        }
        this.currentDrawer.renderRectFilledUI(tx - 2.0f, ty - 2.0f, TextManager.instance.MeasureStringX(UIFont.Small, str) + 4, TextManager.instance.MeasureStringY(UIFont.Small, str) + 4, 0.0f, 0.0f, 0.0f, 0.75f);
        this.textDrawer.renderStringUI(tx, ty, str, 1.0, 1.0, 1.0, 1.0);
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public void render(UIElement ui, float zoom, float xPos, float yPos) {
        int stateIndex = SpriteRenderer.instance.getMainStateIndex();
        this.currentDrawer = this.drawers[stateIndex].impl;
        this.currentDrawer.renderBuffer.clear();
        this.textDrawer.renderBuffer.clear();
        Object object = MapCollisionData.instance.renderLock;
        synchronized (object) {
            this._render(ui, zoom, xPos, yPos);
            this.currentDrawer.renderBuffer.flip();
            SpriteRenderer.instance.drawGeneric(this.drawers[stateIndex]);
        }
        this.renderAllText(ui);
    }

    private void renderAllText(UIElement ui) {
        ByteBuffer bb = this.textDrawer.renderBuffer;
        if (bb.position() == 0) {
            return;
        }
        bb.flip();
        while (bb.position() < bb.limit()) {
            byte e = bb.get();
            switch (e) {
                case 0: {
                    float x = bb.getFloat();
                    float y = bb.getFloat();
                    float w = bb.getFloat();
                    float h = bb.getFloat();
                    float r = bb.getFloat();
                    float g = bb.getFloat();
                    float b = bb.getFloat();
                    float a = bb.getFloat();
                    x = (float)((double)x - ui.getAbsoluteX());
                    y = (float)((double)y - ui.getAbsoluteY());
                    ui.DrawTextureScaledColor(null, Double.valueOf(x), Double.valueOf(y), Double.valueOf(w), Double.valueOf(h), Double.valueOf(r), Double.valueOf(g), Double.valueOf(b), Double.valueOf(a));
                    break;
                }
                case 4: {
                    float x = bb.getFloat();
                    float y = bb.getFloat();
                    String str = GameWindow.ReadString(bb);
                    float r = bb.getFloat();
                    float g = bb.getFloat();
                    float b = bb.getFloat();
                    float a = bb.getFloat();
                    TextManager.instance.DrawString(x, y, str, r, g, b, a);
                    break;
                }
            }
        }
    }

    private void _render(UIElement ui, float zoom, float xPos, float yPos) {
        boolean bShowZombieIntensity;
        this.draww = ui.getWidth().intValue();
        this.drawh = ui.getHeight().intValue();
        this.xPos = xPos;
        this.yPos = yPos;
        this.offx = ui.getAbsoluteX().intValue();
        this.offy = ui.getAbsoluteY().intValue();
        this.zoom = zoom;
        IsoCell isoCell = IsoWorld.instance.currentCell;
        IsoChunkMap chunkMap = IsoWorld.instance.currentCell.chunkMap[0];
        IsoMetaGrid metaGrid = IsoWorld.instance.metaGrid;
        int minCellX = (int)(this.uiToWorldX(0.0f) / 256.0f);
        int minCellY = (int)(this.uiToWorldY(0.0f) / 256.0f);
        int maxCellX = (int)(this.uiToWorldX(this.draww) / 256.0f) + 1;
        int maxCellY = (int)(this.uiToWorldY(this.drawh) / 256.0f) + 1;
        minCellX = PZMath.clamp(minCellX, metaGrid.getMinX(), metaGrid.getMaxX());
        minCellY = PZMath.clamp(minCellY, metaGrid.getMinY(), metaGrid.getMaxY());
        maxCellX = PZMath.clamp(maxCellX, metaGrid.getMinX(), metaGrid.getMaxX());
        maxCellY = PZMath.clamp(maxCellY, metaGrid.getMinY(), metaGrid.getMaxY());
        if (this.metaGridBuildings.getValue()) {
            for (int xx = minCellX; xx <= maxCellX; ++xx) {
                for (int y = minCellY; y <= maxCellY; ++y) {
                    IsoMetaCell metaCell;
                    if (!metaGrid.hasCell(xx - metaGrid.minX, y - metaGrid.minY) || (metaCell = metaGrid.getCell(xx - metaGrid.minX, y - metaGrid.minY)) == null) continue;
                    for (int n = 0; n < metaCell.buildings.size(); ++n) {
                        RoomDef.RoomRect rect;
                        int rr;
                        ArrayList<RoomDef.RoomRect> rects;
                        int r;
                        BuildingDef def = metaCell.buildings.get(n);
                        boolean stash = StashSystem.isStashBuilding(def);
                        boolean spawn = SpawnPoints.instance.isSpawnBuilding(def);
                        for (r = 0; r < def.rooms.size(); ++r) {
                            if (def.rooms.get((int)r).level > 0) continue;
                            rects = def.rooms.get(r).getRects();
                            for (rr = 0; rr < rects.size(); ++rr) {
                                rect = rects.get(rr);
                                if (stash && spawn) {
                                    this.renderRect(rect.getX(), rect.getY(), rect.getW(), rect.getH(), 0.8f, 0.5f, 0.8f, 0.9f);
                                    continue;
                                }
                                if (stash) {
                                    this.renderRect(rect.getX(), rect.getY(), rect.getW(), rect.getH(), 0.8f, 0.5f, 0.5f, 0.6f);
                                    continue;
                                }
                                if (def.alarmed) {
                                    this.renderRect(rect.getX(), rect.getY(), rect.getW(), rect.getH(), 0.8f, 0.8f, 0.5f, 0.3f);
                                    continue;
                                }
                                if (spawn) {
                                    this.renderRect(rect.getX(), rect.getY(), rect.getW(), rect.getH(), 0.5f, 0.8f, 0.5f, 0.6f);
                                    continue;
                                }
                                this.renderRect(rect.getX(), rect.getY(), rect.getW(), rect.getH(), 0.5f, 0.5f, 0.8f, 0.3f);
                            }
                        }
                        for (r = 0; r < def.getEmptyOutside().size(); ++r) {
                            if (def.getEmptyOutside().get((int)r).level > 0) continue;
                            rects = def.getEmptyOutside().get(r).getRects();
                            for (rr = 0; rr < rects.size(); ++rr) {
                                rect = rects.get(rr);
                                if (stash && spawn) {
                                    this.renderRect(rect.getX(), rect.getY(), rect.getW(), rect.getH(), 0.8f, 0.5f, 0.8f, 0.9f);
                                    continue;
                                }
                                if (stash) {
                                    this.renderRect(rect.getX(), rect.getY(), rect.getW(), rect.getH(), 0.8f, 0.5f, 0.5f, 0.6f);
                                    continue;
                                }
                                if (def.alarmed) {
                                    this.renderRect(rect.getX(), rect.getY(), rect.getW(), rect.getH(), 0.8f, 0.8f, 0.5f, 0.3f);
                                    continue;
                                }
                                if (spawn) {
                                    this.renderRect(rect.getX(), rect.getY(), rect.getW(), rect.getH(), 0.5f, 0.8f, 0.5f, 0.6f);
                                    continue;
                                }
                                this.renderRect(rect.getX(), rect.getY(), rect.getW(), rect.getH(), 0.5f, 0.5f, 0.8f, 0.3f);
                            }
                        }
                    }
                }
            }
        }
        if (Core.debug) {
            // empty if block
        }
        if (this.cellGrid.getValue()) {
            float cellSizeInPixels = 256.0f * zoom;
            int dxy = 1;
            while (cellSizeInPixels * (float)dxy < 10.0f) {
                dxy += 2;
            }
            minCellX -= PZMath.coordmodulo(minCellX, dxy);
            minCellY -= PZMath.coordmodulo(minCellY, dxy);
            minCellX = PZMath.max(minCellX, IsoWorld.instance.metaGrid.minX);
            for (int y = minCellY = PZMath.max(minCellY, IsoWorld.instance.metaGrid.minY); y <= maxCellY; y += dxy) {
                this.renderLine(minCellX * 256, y * 256, (maxCellX + 1) * 256, y * 256, 1.0f, 1.0f, 1.0f, 0.15f);
            }
            for (int x = minCellX; x <= maxCellX; x += dxy) {
                this.renderLine(x * 256, minCellY * 256, x * 256, (maxCellY + 1) * 256, 1.0f, 1.0f, 1.0f, 0.15f);
            }
        }
        if (this.cellGrid300.getValue()) {
            int minCellX2 = (int)(this.uiToWorldX(0.0f) / 300.0f);
            int minCellY2 = (int)(this.uiToWorldY(0.0f) / 300.0f);
            int maxCellX2 = (int)(this.uiToWorldX(this.draww) / 300.0f) + 1;
            int maxCellY2 = (int)(this.uiToWorldY(this.drawh) / 300.0f) + 1;
            float cellSizeInPixels = 300.0f * zoom;
            int dxy = 1;
            while (cellSizeInPixels * (float)dxy < 10.0f) {
                dxy += 2;
            }
            minCellX2 -= PZMath.coordmodulo(minCellX2, dxy);
            minCellY2 -= PZMath.coordmodulo(minCellY2, dxy);
            int minCell300X = PZMath.fastfloor((float)IsoWorld.instance.metaGrid.minX * 256.0f / 300.0f);
            int minCell300Y = PZMath.fastfloor((float)IsoWorld.instance.metaGrid.minY * 256.0f / 300.0f);
            int maxCell300X = PZMath.fastfloor((float)(IsoWorld.instance.metaGrid.maxX + 1) * 256.0f / 300.0f) + 1;
            int maxCell300Y = PZMath.fastfloor((float)(IsoWorld.instance.metaGrid.maxY + 1) * 256.0f / 300.0f) + 1;
            minCellX2 = PZMath.max(minCellX2, minCell300X);
            minCellY2 = PZMath.max(minCellY2, minCell300Y);
            maxCellX2 = PZMath.min(maxCellX2, maxCell300X);
            maxCellY2 = PZMath.min(maxCellY2, maxCell300Y);
            for (int y = minCellY2; y <= maxCellY2; y += dxy) {
                this.renderLine(minCellX2 * 300, y * 300, maxCellX2 * 300, y * 300, 0.0f, 1.0f, 1.0f, 0.25f);
            }
            for (int x = minCellX2; x <= maxCellX2; x += dxy) {
                this.renderLine(x * 300, minCellY2 * 300, x * 300, maxCellY2 * 300, 0.0f, 1.0f, 1.0f, 0.25f);
            }
            this.outlineRect(minCell300X * 300, minCell300Y * 300, (maxCell300X - minCell300X) * 300, (maxCell300Y - minCell300Y) * 300, 0.0f, 1.0f, 1.0f, 0.25f);
        }
        boolean bl = bShowZombieIntensity = this.zombieIntensity.getValue() && zoom > 0.5f;
        if (bShowZombieIntensity) {
            int cellY;
            for (int cellY2 = minCellY; cellY2 <= maxCellY; ++cellY2) {
                for (int cellX = minCellX; cellX <= maxCellX; ++cellX) {
                    IsoMetaCell metaCell;
                    if (!metaGrid.hasCell(cellX - metaGrid.minX, cellY2 - metaGrid.minY) || (metaCell = metaGrid.getCell(cellX - metaGrid.minX, cellY2 - metaGrid.minY)) == null || metaCell.info == null) continue;
                    for (int chunkY = 0; chunkY < 32; ++chunkY) {
                        for (int chunkX = 0; chunkX < 32; ++chunkX) {
                            int intensity = LotHeader.getZombieIntensityForChunk(metaCell.info, chunkX, chunkY);
                            if (intensity <= 0) continue;
                            float intensityF = (float)intensity / 255.0f;
                            float r = intensityF = PZMath.min(1.0f, intensityF + 0.1f);
                            float g = 0.0f;
                            float b = 0.0f;
                            float a = 0.9f;
                            this.renderRect(cellX * 256 + chunkX * 8, cellY2 * 256 + chunkY * 8, 8.0f, 8.0f, r, 0.0f, 0.0f, 0.9f);
                        }
                    }
                }
            }
            double mouseX = (double)Mouse.getXA() - ui.getAbsoluteX();
            double mouseY = (double)Mouse.getYA() - ui.getAbsoluteY();
            float worldX = this.uiToWorldX((float)mouseX);
            float worldY = this.uiToWorldY((float)mouseY);
            int cellX = PZMath.fastfloor(worldX / 256.0f);
            IsoMetaCell metaCell = metaGrid.getCellData(cellX, cellY = PZMath.fastfloor(worldY / 256.0f));
            if (metaCell != null && metaCell.info != null) {
                int chunkX = (int)(worldX - (float)(cellX * 256)) / 8;
                int chunkY = (int)(worldY - (float)(cellY * 256)) / 8;
                this.outlineRect(cellX * 256 + chunkX * 8, cellY * 256 + chunkY * 8, 8.0f, 8.0f, 1.0f, 1.0f, 1.0f, 1.0f);
                int intensity = LotHeader.getZombieIntensityForChunk(metaCell.info, chunkX, chunkY);
                intensity = PZMath.max(intensity, 0);
                Object intensityStr = String.format(Translator.getText("IGUI_ZombiePopulation_Intensity") + ": %d", intensity);
                IsoChunk chunk = IsoWorld.instance.currentCell.getChunkForGridSquare(PZMath.fastfloor(worldX), PZMath.fastfloor(worldY), 0);
                if (chunk != null) {
                    int total = 0;
                    for (int z = chunk.getMinLevel(); z <= chunk.getMaxLevel(); ++z) {
                        IsoGridSquare[] squares = chunk.getSquaresForLevel(z);
                        for (int i = 0; i < 64; ++i) {
                            IsoGridSquare square = squares[i];
                            if (square == null) continue;
                            for (int j = 0; j < square.getMovingObjects().size(); ++j) {
                                IsoMovingObject mov = square.getMovingObjects().get(j);
                                if (!(mov instanceof IsoZombie)) continue;
                                ++total;
                            }
                        }
                    }
                    if (total > 0) {
                        intensityStr = (String)intensityStr + "\n" + String.format(Translator.getText("IGUI_ZombiePopulation_Zombies") + ": %d", total);
                    }
                }
                float textX = this.worldToScreenX(cellX * 256 + chunkX * 8);
                float textY = this.worldToScreenY(cellY * 256 + chunkY * 8 + 8) + 1.0f;
                this.textDrawer.renderRectFilledUI(textX - 4.0f, textY, TextManager.instance.MeasureStringX(UIFont.Small, (String)intensityStr) + 8, TextManager.instance.MeasureStringY(UIFont.Small, (String)intensityStr), 0.0f, 0.0f, 0.0f, 0.75f);
                this.textDrawer.renderStringUI(textX, textY, (String)intensityStr, 1.0, 1.0, 1.0, 1.0);
            }
        }
        if (this.zombiesReal.getValue()) {
            for (int i = 0; i < IsoWorld.instance.currentCell.getZombieList().size(); ++i) {
                IsoZombie z = IsoWorld.instance.currentCell.getZombieList().get(i);
                float red = 1.0f;
                float green = 1.0f;
                float blue = 0.0f;
                if (z.isReanimatedPlayer()) {
                    red = 0.0f;
                }
                this.renderZombie(z.getX(), z.getY(), red, 1.0f, 0.0f);
                if (z.getCurrentState() != WalkTowardState.instance()) continue;
                this.renderLine(z.getX(), z.getY(), z.getPathTargetX(), z.getPathTargetY(), 1.0f, 1.0f, 1.0f, 0.5f);
            }
        }
        for (int i = 0; i < IsoPlayer.numPlayers; ++i) {
            IsoPlayer player = IsoPlayer.players[i];
            if (player == null) continue;
            this.renderZombie(player.getX(), player.getY(), 0.0f, 0.5f, 0.0f);
        }
        if (GameClient.client) {
            MPDebugInfo.instance.render(this, zoom);
            return;
        }
        if (this.vehicles.getValue()) {
            VehiclesDB2.instance.renderDebug(this);
        }
        this.n_render(zoom, (int)this.offx, (int)this.offy, xPos, yPos, (int)this.draww, (int)this.drawh);
    }

    public void setWallFollowerStart(int x, int y) {
        if (GameClient.client) {
            return;
        }
        this.n_setWallFollowerStart(x, y);
    }

    public void setWallFollowerEnd(int x, int y) {
        if (GameClient.client) {
            return;
        }
        this.n_setWallFollowerEnd(x, y);
    }

    public void wallFollowerMouseMove(int x, int y) {
        if (GameClient.client) {
            return;
        }
        this.n_wallFollowerMouseMove(x, y);
    }

    public ConfigOption getOptionByName(String name) {
        for (int i = 0; i < this.options.size(); ++i) {
            ConfigOption setting = this.options.get(i);
            if (!setting.getName().equals(name)) continue;
            return setting;
        }
        return null;
    }

    public int getOptionCount() {
        return this.options.size();
    }

    public ConfigOption getOptionByIndex(int index) {
        return this.options.get(index);
    }

    public void setBoolean(String name, boolean value) {
        ConfigOption setting = this.getOptionByName(name);
        if (setting instanceof BooleanConfigOption) {
            BooleanConfigOption booleanConfigOption = (BooleanConfigOption)setting;
            booleanConfigOption.setValue(value);
        }
    }

    public boolean getBoolean(String name) {
        ConfigOption setting = this.getOptionByName(name);
        if (setting instanceof BooleanConfigOption) {
            BooleanConfigOption booleanConfigOption = (BooleanConfigOption)setting;
            return booleanConfigOption.getValue();
        }
        return false;
    }

    public void save() {
        String fileName = ZomboidFileSystem.instance.getCacheDir() + File.separator + "popman-options.ini";
        ConfigFile configFile = new ConfigFile();
        configFile.write(fileName, 1, this.options);
        for (int i = 0; i < this.options.size(); ++i) {
            ConfigOption option = this.options.get(i);
            this.n_setDebugOption(option.getName(), option.getValueAsString());
        }
    }

    public void load() {
        int i;
        ConfigFile configFile = new ConfigFile();
        String fileName = ZomboidFileSystem.instance.getCacheDir() + File.separator + "popman-options.ini";
        if (configFile.read(fileName)) {
            for (i = 0; i < configFile.getOptions().size(); ++i) {
                ConfigOption configOption = configFile.getOptions().get(i);
                ConfigOption myOption = this.getOptionByName(configOption.getName());
                if (myOption == null) continue;
                myOption.parse(configOption.getValueAsString());
            }
        }
        for (i = 0; i < this.options.size(); ++i) {
            ConfigOption option = this.options.get(i);
            this.n_setDebugOption(option.getName(), option.getValueAsString());
        }
    }

    private static final class Drawer
    extends TextureDraw.GenericDrawer {
        final DrawerImpl impl = new DrawerImpl();

        private Drawer() {
        }

        @Override
        public void render() {
            VBORenderer vbor = VBORenderer.getInstance();
            ByteBuffer bb = this.impl.renderBuffer;
            float z = 0.0f;
            while (bb.position() < bb.limit()) {
                byte e = bb.get();
                switch (e) {
                    case 0: {
                        float x = bb.getFloat();
                        float y = bb.getFloat();
                        float w = bb.getFloat();
                        float h = bb.getFloat();
                        float r = bb.getFloat();
                        float g = bb.getFloat();
                        float b = bb.getFloat();
                        float a = bb.getFloat();
                        vbor.startRun(vbor.formatPositionColor);
                        vbor.setMode(7);
                        vbor.addQuad(x, y, x + w, y + h, 0.0f, r, g, b, a);
                        vbor.endRun();
                        break;
                    }
                    case 2: {
                        float x1 = bb.getFloat();
                        float y1 = bb.getFloat();
                        float x2 = bb.getFloat();
                        float y2 = bb.getFloat();
                        float r = bb.getFloat();
                        float g = bb.getFloat();
                        float b = bb.getFloat();
                        float a = bb.getFloat();
                        vbor.startRun(vbor.formatPositionColor);
                        vbor.setMode(1);
                        vbor.addLine(x1, y1, 0.0f, x2, y2, 0.0f, r, g, b, a);
                        vbor.endRun();
                        break;
                    }
                    case 4: {
                        float x = bb.getFloat();
                        float y = bb.getFloat();
                        String str = GameWindow.ReadString(bb);
                        float r = bb.getFloat();
                        float g = bb.getFloat();
                        float b = bb.getFloat();
                        float a = bb.getFloat();
                        break;
                    }
                }
            }
            vbor.flush();
        }
    }

    private static class DrawerImpl {
        ByteBuffer renderBuffer = ByteBuffer.allocate(1024);

        private DrawerImpl() {
        }

        private void reserve(int nBytes) {
            if (this.renderBuffer.position() + nBytes <= this.renderBuffer.capacity()) {
                return;
            }
            ByteBuffer bb = ByteBuffer.allocate(this.renderBuffer.capacity() * 2);
            this.renderBuffer.flip();
            bb.put(this.renderBuffer);
            this.renderBuffer = bb;
        }

        private void renderBufferByte(byte v) {
            this.reserve(1);
            this.renderBuffer.put(v);
        }

        private void renderBufferFloat(double v) {
            this.reserve(4);
            this.renderBuffer.putFloat((float)v);
        }

        private void renderBufferFloat(float v) {
            this.reserve(4);
            this.renderBuffer.putFloat(v);
        }

        private void renderBufferInt(int v) {
            this.reserve(4);
            this.renderBuffer.putInt(v);
        }

        private void renderBufferString(String v) {
            ByteBuffer utf = GameWindow.getEncodedBytesUTF(v);
            this.reserve(2 + utf.position());
            this.renderBuffer.putShort((short)utf.position());
            utf.flip();
            this.renderBuffer.put(utf);
        }

        private void renderLineUI(float x1, float y1, float x2, float y2, float r, float g, float b, float a) {
            this.renderBufferByte((byte)2);
            this.renderBufferFloat(x1);
            this.renderBufferFloat(y1);
            this.renderBufferFloat(x2);
            this.renderBufferFloat(y2);
            this.renderBufferFloat(r);
            this.renderBufferFloat(g);
            this.renderBufferFloat(b);
            this.renderBufferFloat(a);
        }

        private void renderRectFilledUI(float x, float y, float w, float h, float r, float g, float b, float a) {
            this.renderBufferByte((byte)0);
            this.renderBufferFloat(x);
            this.renderBufferFloat(y);
            this.renderBufferFloat(w);
            this.renderBufferFloat(h);
            this.renderBufferFloat(r);
            this.renderBufferFloat(g);
            this.renderBufferFloat(b);
            this.renderBufferFloat(a);
        }

        private void renderStringUI(float x, float y, String str, double r, double g, double b, double a) {
            this.renderBufferByte((byte)4);
            this.renderBufferFloat(x);
            this.renderBufferFloat(y);
            this.renderBufferString(str);
            this.renderBufferFloat(r);
            this.renderBufferFloat(g);
            this.renderBufferFloat(b);
            this.renderBufferFloat(a);
        }
    }

    @UsedFromLua
    public class BooleanDebugOption
    extends BooleanConfigOption {
        public BooleanDebugOption(ZombiePopulationRenderer this$0, String name, boolean defaultValue) {
            Objects.requireNonNull(this$0);
            super(name, defaultValue);
            this$0.options.add(this);
        }
    }
}

