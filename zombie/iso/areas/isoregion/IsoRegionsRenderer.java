/*
 * Decompiled with CFR 0.152.
 */
package zombie.iso.areas.isoregion;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import zombie.MapCollisionData;
import zombie.UsedFromLua;
import zombie.ZomboidFileSystem;
import zombie.characters.IsoPlayer;
import zombie.config.BooleanConfigOption;
import zombie.config.ConfigFile;
import zombie.config.ConfigOption;
import zombie.core.Color;
import zombie.core.Colors;
import zombie.core.Core;
import zombie.core.SpriteRenderer;
import zombie.core.math.PZMath;
import zombie.core.utils.Bits;
import zombie.iso.BuildingDef;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoMetaCell;
import zombie.iso.IsoMetaGrid;
import zombie.iso.IsoWorld;
import zombie.iso.RoomDef;
import zombie.iso.areas.isoregion.IsoRegions;
import zombie.iso.areas.isoregion.data.DataChunk;
import zombie.iso.areas.isoregion.data.DataRoot;
import zombie.iso.areas.isoregion.regions.IsoChunkRegion;
import zombie.iso.areas.isoregion.regions.IsoWorldRegion;
import zombie.iso.objects.IsoThumpable;
import zombie.ui.TextManager;
import zombie.ui.UIElement;
import zombie.ui.UIFont;

@UsedFromLua
public class IsoRegionsRenderer {
    private final List<DataChunk> tempChunkList = new ArrayList<DataChunk>();
    private final List<String> debugLines = new ArrayList<String>();
    private float xPos;
    private float yPos;
    private float offx;
    private float offy;
    private float zoom;
    private float draww;
    private float drawh;
    private boolean hasSelected;
    private boolean validSelection;
    private int selectedX;
    private int selectedY;
    private int selectedZ;
    private final HashSet<Integer> drawnCells = new HashSet();
    private boolean editSquareInRange;
    private int editSquareX;
    private int editSquareY;
    private final ArrayList<ConfigOption> editOptions = new ArrayList();
    private boolean editingEnabled;
    private final BooleanDebugOption editWallN = new BooleanDebugOption(this.editOptions, "Edit.WallN", false);
    private final BooleanDebugOption editWallW = new BooleanDebugOption(this.editOptions, "Edit.WallW", false);
    private final BooleanDebugOption editDoorN = new BooleanDebugOption(this.editOptions, "Edit.DoorN", false);
    private final BooleanDebugOption editDoorW = new BooleanDebugOption(this.editOptions, "Edit.DoorW", false);
    private final BooleanDebugOption editFloor = new BooleanDebugOption(this.editOptions, "Edit.Floor", false);
    private final ArrayList<ConfigOption> zLevelOptions = new ArrayList();
    private final BooleanDebugOption zLevelPlayer = new BooleanDebugOption(this.zLevelOptions, "zLevel.Player", true);
    private final BooleanDebugOption zLevel0 = new BooleanDebugOption(this.zLevelOptions, "zLevel.0", false, 0);
    private final BooleanDebugOption zLevel1 = new BooleanDebugOption(this.zLevelOptions, "zLevel.1", false, 1);
    private final BooleanDebugOption zLevel2 = new BooleanDebugOption(this.zLevelOptions, "zLevel.2", false, 2);
    private final BooleanDebugOption zLevel3 = new BooleanDebugOption(this.zLevelOptions, "zLevel.3", false, 3);
    private final BooleanDebugOption zLevel4 = new BooleanDebugOption(this.zLevelOptions, "zLevel.4", false, 4);
    private final BooleanDebugOption zLevel5 = new BooleanDebugOption(this.zLevelOptions, "zLevel.5", false, 5);
    private final BooleanDebugOption zLevel6 = new BooleanDebugOption(this.zLevelOptions, "zLevel.6", false, 6);
    private final BooleanDebugOption zLevel7 = new BooleanDebugOption(this.zLevelOptions, "zLevel.7", false, 7);
    private static final int VERSION = 1;
    private final ArrayList<ConfigOption> options = new ArrayList();
    private final BooleanDebugOption cellGrid = new BooleanDebugOption(this.options, "CellGrid", true);
    private final BooleanDebugOption metaGridBuildings = new BooleanDebugOption(this.options, "MetaGrid.Buildings", true);
    private final BooleanDebugOption isoRegionRender = new BooleanDebugOption(this.options, "IsoRegion.Render", true);
    private final BooleanDebugOption isoRegionRenderChunks = new BooleanDebugOption(this.options, "IsoRegion.RenderChunks", false);
    private final BooleanDebugOption isoRegionRenderChunksPlus = new BooleanDebugOption(this.options, "IsoRegion.RenderChunksPlus", false);

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

    public void renderStringUI(float x, float y, String str, Color c) {
        this.renderStringUI(x, y, str, c.r, c.g, c.b, c.a);
    }

    public void renderStringUI(float x, float y, String str, double r, double g, double b, double a) {
        float tx = this.offx + x;
        float ty = this.offy + y;
        SpriteRenderer.instance.render(null, tx - 2.0f, ty - 2.0f, TextManager.instance.MeasureStringX(UIFont.Small, str) + 4, TextManager.instance.font.getLineHeight() + 4, 0.0f, 0.0f, 0.0f, 0.75f, null);
        TextManager.instance.DrawString(tx, ty, str, r, g, b, a);
    }

    public void renderString(float x, float y, String str, double r, double g, double b, double a) {
        float tx = this.worldToScreenX(x);
        float ty = this.worldToScreenY(y);
        SpriteRenderer.instance.render(null, tx - 2.0f, ty - 2.0f, TextManager.instance.MeasureStringX(UIFont.Small, str) + 4, TextManager.instance.font.getLineHeight() + 4, 0.0f, 0.0f, 0.0f, 0.75f, null);
        TextManager.instance.DrawString(tx, ty, str, r, g, b, a);
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
        SpriteRenderer.instance.render(null, x1, y1, w, h, r, g, b, a, null);
    }

    public void renderLine(float x1, float y1, float x2, float y2, float r, float g, float b, float a) {
        float sx1 = this.worldToScreenX(x1);
        float sy1 = this.worldToScreenY(y1);
        float sx2 = this.worldToScreenX(x2);
        float sy2 = this.worldToScreenY(y2);
        if (sx1 >= (float)Core.getInstance().getScreenWidth() && sx2 >= (float)Core.getInstance().getScreenWidth() || sy1 >= (float)Core.getInstance().getScreenHeight() && sy2 >= (float)Core.getInstance().getScreenHeight() || sx1 < 0.0f && sx2 < 0.0f || sy1 < 0.0f && sy2 < 0.0f) {
            return;
        }
        SpriteRenderer.instance.renderline(null, (int)sx1, (int)sy1, (int)sx2, (int)sy2, r, g, b, a);
    }

    public void outlineRect(float x, float y, float w, float h, float r, float g, float b, float a) {
        this.renderLine(x, y, x + w, y, r, g, b, a);
        this.renderLine(x + w, y, x + w, y + h, r, g, b, a);
        this.renderLine(x, y + h, x + w, y + h, r, g, b, a);
        this.renderLine(x, y, x, y + h, r, g, b, a);
    }

    public void renderCellInfo(int cellX, int cellY, int effectivePopulation, int targetPopulation, float lastRepopTime) {
        float tx = this.worldToScreenX(cellX * 256) + 4.0f;
        float ty = this.worldToScreenY(cellY * 256) + 4.0f;
        String str = effectivePopulation + " / " + targetPopulation;
        if (lastRepopTime > 0.0f) {
            str = str + String.format(" %.2f", Float.valueOf(lastRepopTime));
        }
        SpriteRenderer.instance.render(null, tx - 2.0f, ty - 2.0f, TextManager.instance.MeasureStringX(UIFont.Small, str) + 4, TextManager.instance.font.getLineHeight() + 4, 0.0f, 0.0f, 0.0f, 0.75f, null);
        TextManager.instance.DrawString(tx, ty, str, 1.0, 1.0, 1.0, 1.0);
    }

    public void renderZombie(float x, float y, float r, float g, float b) {
        float zombieSize = 1.0f / this.zoom + 0.5f;
        this.renderRect(x - zombieSize / 2.0f, y - zombieSize / 2.0f, zombieSize, zombieSize, r, g, b, 1.0f);
    }

    public void renderSquare(float x, float y, float r, float g, float b, float alpha) {
        float zombieSize = 1.0f;
        this.renderRect(x, y, 1.0f, 1.0f, r, g, b, alpha);
    }

    public void renderEntity(float size, float x, float y, float r, float g, float b, float a) {
        float zombieSize = size / this.zoom + 0.5f;
        this.renderRect(x - zombieSize / 2.0f, y - zombieSize / 2.0f, zombieSize, zombieSize, r, g, b, a);
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public void render(UIElement ui, float zoom, float xPos, float yPos) {
        Object object = MapCollisionData.instance.renderLock;
        synchronized (object) {
            this._render(ui, zoom, xPos, yPos);
        }
    }

    private void debugLine(String str) {
        this.debugLines.add(str);
    }

    public void recalcSurroundings() {
        IsoRegions.forceRecalcSurroundingChunks();
    }

    public boolean hasChunkRegion(int x, int y) {
        int z = this.getZLevel();
        DataRoot root = IsoRegions.getDataRoot();
        return root.getIsoChunkRegion(x, y, z) != null;
    }

    public IsoChunkRegion getChunkRegion(int x, int y) {
        int z = this.getZLevel();
        DataRoot root = IsoRegions.getDataRoot();
        return root.getIsoChunkRegion(x, y, z);
    }

    public void setSelected(int x, int y) {
        this.setSelectedWorld((int)this.uiToWorldX(x), (int)this.uiToWorldY(y));
    }

    public void setSelectedWorld(int x, int y) {
        this.selectedZ = this.getZLevel();
        this.hasSelected = true;
        this.selectedX = x;
        this.selectedY = y;
    }

    public void unsetSelected() {
        this.hasSelected = false;
    }

    public boolean isHasSelected() {
        return this.hasSelected;
    }

    private void _render(UIElement ui, float zoom, float xPos, float yPos) {
        DataRoot root;
        this.debugLines.clear();
        this.drawnCells.clear();
        this.draww = ui.getWidth().intValue();
        this.drawh = ui.getHeight().intValue();
        this.xPos = xPos;
        this.yPos = yPos;
        this.offx = ui.getAbsoluteX().intValue();
        this.offy = ui.getAbsoluteY().intValue();
        this.zoom = zoom;
        this.debugLine("Zoom: " + zoom);
        this.debugLine("zLevel: " + this.getZLevel());
        IsoMetaGrid metaGrid = IsoWorld.instance.metaGrid;
        int minCellX = (int)(this.uiToWorldX(0.0f) / 256.0f) - metaGrid.minX;
        int minCellY = (int)(this.uiToWorldY(0.0f) / 256.0f) - metaGrid.minY;
        int maxCellX = (int)(this.uiToWorldX(this.draww) / 256.0f) + 1 - metaGrid.minX;
        int maxCellY = (int)(this.uiToWorldY(this.drawh) / 256.0f) + 1 - metaGrid.minY;
        minCellX = PZMath.clamp(minCellX, 0, metaGrid.getWidth() - 1);
        minCellY = PZMath.clamp(minCellY, 0, metaGrid.getHeight() - 1);
        maxCellX = PZMath.clamp(maxCellX, 0, metaGrid.getWidth() - 1);
        maxCellY = PZMath.clamp(maxCellY, 0, metaGrid.getHeight() - 1);
        float cc = Math.max(1.0f - zoom / 2.0f, 0.1f);
        IsoChunkRegion selChunkRegion = null;
        IsoWorldRegion selWorldRegion = null;
        this.validSelection = false;
        if (this.isoRegionRender.getValue()) {
            IsoPlayer player = IsoPlayer.getInstance();
            root = IsoRegions.getDataRoot();
            this.tempChunkList.clear();
            root.getAllChunks(this.tempChunkList);
            this.debugLine("DataChunks: " + this.tempChunkList.size());
            this.debugLine("IsoChunkRegions: " + root.regionManager.getChunkRegionCount());
            this.debugLine("IsoWorldRegions: " + root.regionManager.getWorldRegionCount());
            if (this.hasSelected) {
                selChunkRegion = root.getIsoChunkRegion(this.selectedX, this.selectedY, this.selectedZ);
                selWorldRegion = root.getIsoWorldRegion(this.selectedX, this.selectedY, this.selectedZ);
                if (!(selWorldRegion == null || selWorldRegion.isEnclosed() || this.isoRegionRenderChunks.getValue() && this.isoRegionRenderChunksPlus.getValue())) {
                    selWorldRegion = null;
                    selChunkRegion = null;
                }
                if (selChunkRegion != null) {
                    this.validSelection = true;
                }
            }
            for (int i = 0; i < this.tempChunkList.size(); ++i) {
                DataChunk chunk = this.tempChunkList.get(i);
                int cWX = chunk.getChunkX() * 8;
                int cWY = chunk.getChunkY() * 8;
                if (!(zoom > 0.1f)) continue;
                float x1 = this.worldToScreenX(cWX);
                float y1 = this.worldToScreenY(cWY);
                float x2 = this.worldToScreenX(cWX + 8);
                float y2 = this.worldToScreenY(cWY + 8);
                if (x1 >= this.offx + this.draww || x2 < this.offx || y1 >= this.offy + this.drawh || y2 < this.offy) continue;
                this.renderRect(cWX, cWY, 8.0f, 8.0f, 0.0f, cc, 0.0f, 1.0f);
            }
        }
        if (this.metaGridBuildings.getValue()) {
            float alphaMod = PZMath.clamp(0.3f * (zoom / 5.0f), 0.15f, 0.3f);
            for (int xx = minCellX; xx < maxCellX; ++xx) {
                for (int y = minCellY; y < maxCellY; ++y) {
                    IsoMetaCell metaCell;
                    if (!metaGrid.hasCell(xx, y) || (metaCell = metaGrid.getCell(xx, y)) == null) continue;
                    for (int n = 0; n < metaCell.buildings.size(); ++n) {
                        BuildingDef def = metaCell.buildings.get(n);
                        for (int r = 0; r < def.rooms.size(); ++r) {
                            if (def.rooms.get((int)r).level > 0) continue;
                            ArrayList<RoomDef.RoomRect> rects = def.rooms.get(r).getRects();
                            for (int rr = 0; rr < rects.size(); ++rr) {
                                RoomDef.RoomRect rect = rects.get(rr);
                                if (def.alarmed) {
                                    this.renderRect(rect.getX(), rect.getY(), rect.getW(), rect.getH(), 0.8f * alphaMod, 0.8f * alphaMod, 0.5f * alphaMod, 1.0f);
                                    continue;
                                }
                                this.renderRect(rect.getX(), rect.getY(), rect.getW(), rect.getH(), 0.5f * alphaMod, 0.5f * alphaMod, 0.8f * alphaMod, 1.0f);
                            }
                        }
                    }
                }
            }
        }
        if (this.isoRegionRender.getValue()) {
            int zLevel = this.getZLevel();
            root = IsoRegions.getDataRoot();
            this.tempChunkList.clear();
            root.getAllChunks(this.tempChunkList);
            for (int i = 0; i < this.tempChunkList.size(); ++i) {
                DataChunk chunk = this.tempChunkList.get(i);
                int cWX = chunk.getChunkX() * 8;
                int cWY = chunk.getChunkY() * 8;
                if (zoom <= 0.1f) {
                    int cellx = cWX / 256;
                    int celly = cWY / 256;
                    int cellID = IsoRegions.hash(cellx, celly);
                    if (this.drawnCells.contains(cellID)) continue;
                    this.drawnCells.add(cellID);
                    this.renderRect(cellx * 256, celly * 256, 256.0f, 256.0f, 0.0f, cc, 0.0f, 1.0f);
                    continue;
                }
                if (zoom < 1.0f) continue;
                float x1 = this.worldToScreenX(cWX);
                float y1 = this.worldToScreenY(cWY);
                float x2 = this.worldToScreenX(cWX + 8);
                float y2 = this.worldToScreenY(cWY + 8);
                if (x1 >= this.offx + this.draww || x2 < this.offx || y1 >= this.offy + this.drawh || y2 < this.offy) continue;
                for (int x = 0; x < 8; ++x) {
                    for (int y = 0; y < 8; ++y) {
                        int minZ;
                        for (int z = minZ = zLevel > 0 ? zLevel - 1 : zLevel; z <= zLevel; ++z) {
                            IsoWorldRegion isoWorldRegion;
                            float alphaMod = z < zLevel ? 0.25f : 1.0f;
                            byte flags = chunk.getSquare(x, y, z);
                            if (flags < 0) continue;
                            IsoChunkRegion isoChunkRegion = chunk.getIsoChunkRegion(x, y, z);
                            if (isoChunkRegion != null) {
                                if (zoom > 6.0f && this.isoRegionRenderChunks.getValue() && this.isoRegionRenderChunksPlus.getValue()) {
                                    col = isoChunkRegion.getColor();
                                    float renderAlpha = 1.0f;
                                    if (selChunkRegion != null && isoChunkRegion != selChunkRegion) {
                                        renderAlpha = 0.25f;
                                    }
                                    this.renderSquare(cWX + x, cWY + y, col.r, col.g, col.b, renderAlpha * alphaMod);
                                } else {
                                    isoWorldRegion = isoChunkRegion.getIsoWorldRegion();
                                    if (isoWorldRegion != null && isoWorldRegion.isEnclosed()) {
                                        float renderAlpha = 1.0f;
                                        if (this.isoRegionRenderChunks.getValue()) {
                                            col = isoChunkRegion.getColor();
                                            if (selChunkRegion != null && isoChunkRegion != selChunkRegion) {
                                                renderAlpha = 0.25f;
                                            }
                                        } else {
                                            col = isoWorldRegion.getColor();
                                            if (selWorldRegion != null && isoWorldRegion != selWorldRegion) {
                                                renderAlpha = 0.25f;
                                            }
                                        }
                                        this.renderSquare(cWX + x, cWY + y, col.r, col.g, col.b, renderAlpha * alphaMod);
                                    }
                                }
                            }
                            if (z > 0 && z == zLevel) {
                                boolean test;
                                isoChunkRegion = chunk.getIsoChunkRegion(x, y, z);
                                isoWorldRegion = isoChunkRegion != null ? isoChunkRegion.getIsoWorldRegion() : null;
                                boolean bl = test = isoChunkRegion == null || isoWorldRegion == null || !isoWorldRegion.isEnclosed();
                                if (test && Bits.hasFlags(flags, 16)) {
                                    this.renderSquare(cWX + x, cWY + y, 0.5f, 0.5f, 0.5f, 1.0f);
                                }
                            }
                            if (Bits.hasFlags(flags, 1) || Bits.hasFlags(flags, 4)) {
                                this.renderRect(cWX + x, cWY + y, 1.0f, 0.1f, 1.0f, 1.0f, 1.0f, 1.0f * alphaMod);
                            }
                            if (!Bits.hasFlags(flags, 2) && !Bits.hasFlags(flags, 8)) continue;
                            this.renderRect(cWX + x, cWY + y, 0.1f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f * alphaMod);
                        }
                    }
                }
            }
        }
        if (this.cellGrid.getValue()) {
            float lineAlphaMod = 1.0f;
            if (zoom < 0.1f) {
                lineAlphaMod = Math.max(zoom / 0.1f, 0.25f);
            }
            for (int y = minCellY; y <= maxCellY; ++y) {
                this.renderLine(metaGrid.minX * 256, (metaGrid.minY + y) * 256, (metaGrid.maxX + 1) * 256, (metaGrid.minY + y) * 256, 1.0f, 1.0f, 1.0f, 0.15f * lineAlphaMod);
                if (zoom > 1.0f) {
                    for (int i = 1; i < 32; ++i) {
                        this.renderLine(metaGrid.minX * 256, (metaGrid.minY + y) * 256 + i * 8, (metaGrid.maxX + 1) * 256, (metaGrid.minY + y) * 256 + i * 8, 1.0f, 1.0f, 1.0f, 0.0325f);
                    }
                    continue;
                }
                if (!(zoom > 0.15f)) continue;
                this.renderLine(metaGrid.minX * 256, (metaGrid.minY + y) * 256 + 100, (metaGrid.maxX + 1) * 256, (metaGrid.minY + y) * 256 + 100, 1.0f, 1.0f, 1.0f, 0.075f);
                this.renderLine(metaGrid.minX * 256, (metaGrid.minY + y) * 256 + 200, (metaGrid.maxX + 1) * 256, (metaGrid.minY + y) * 256 + 200, 1.0f, 1.0f, 1.0f, 0.075f);
            }
            for (int x = minCellX; x <= maxCellX; ++x) {
                this.renderLine((metaGrid.minX + x) * 256, metaGrid.minY * 256, (metaGrid.minX + x) * 256, (metaGrid.maxY + 1) * 256, 1.0f, 1.0f, 1.0f, 0.15f * lineAlphaMod);
                if (zoom > 1.0f) {
                    for (int i = 1; i < 32; ++i) {
                        this.renderLine((metaGrid.minX + x) * 256 + i * 8, metaGrid.minY * 256, (metaGrid.minX + x) * 256 + i * 8, (metaGrid.maxY + 1) * 256, 1.0f, 1.0f, 1.0f, 0.0325f);
                    }
                    continue;
                }
                if (!(zoom > 0.15f)) continue;
                this.renderLine((metaGrid.minX + x) * 256 + 100, metaGrid.minY * 256, (metaGrid.minX + x) * 256 + 100, (metaGrid.maxY + 1) * 256, 1.0f, 1.0f, 1.0f, 0.075f);
                this.renderLine((metaGrid.minX + x) * 256 + 200, metaGrid.minY * 256, (metaGrid.minX + x) * 256 + 200, (metaGrid.maxY + 1) * 256, 1.0f, 1.0f, 1.0f, 0.075f);
            }
        }
        for (int i = 0; i < IsoPlayer.numPlayers; ++i) {
            IsoPlayer players = IsoPlayer.players[i];
            if (players == null) continue;
            this.renderZombie(players.getX(), players.getY(), 0.0f, 0.5f, 0.0f);
        }
        if (this.isEditingEnabled()) {
            float g;
            float r = this.editSquareInRange ? 0.0f : 1.0f;
            float f = g = this.editSquareInRange ? 1.0f : 0.0f;
            if (this.editWallN.getValue() || this.editDoorN.getValue()) {
                this.renderRect(this.editSquareX, this.editSquareY, 1.0f, 0.25f, r, g, 0.0f, 0.5f);
                this.renderRect(this.editSquareX, this.editSquareY, 1.0f, 0.05f, r, g, 0.0f, 1.0f);
                this.renderRect(this.editSquareX, this.editSquareY, 0.05f, 0.25f, r, g, 0.0f, 1.0f);
                this.renderRect(this.editSquareX, (float)this.editSquareY + 0.2f, 1.0f, 0.05f, r, g, 0.0f, 1.0f);
                this.renderRect((float)this.editSquareX + 0.95f, this.editSquareY, 0.05f, 0.25f, r, g, 0.0f, 1.0f);
            } else if (this.editWallW.getValue() || this.editDoorW.getValue()) {
                this.renderRect(this.editSquareX, this.editSquareY, 0.25f, 1.0f, r, g, 0.0f, 0.5f);
                this.renderRect(this.editSquareX, this.editSquareY, 0.25f, 0.05f, r, g, 0.0f, 1.0f);
                this.renderRect(this.editSquareX, this.editSquareY, 0.05f, 1.0f, r, g, 0.0f, 1.0f);
                this.renderRect(this.editSquareX, (float)this.editSquareY + 0.95f, 0.25f, 0.05f, r, g, 0.0f, 1.0f);
                this.renderRect((float)this.editSquareX + 0.2f, this.editSquareY, 0.05f, 1.0f, r, g, 0.0f, 1.0f);
            } else {
                this.renderRect(this.editSquareX, this.editSquareY, 1.0f, 1.0f, r, g, 0.0f, 0.5f);
                this.renderRect(this.editSquareX, this.editSquareY, 1.0f, 0.05f, r, g, 0.0f, 1.0f);
                this.renderRect(this.editSquareX, this.editSquareY, 0.05f, 1.0f, r, g, 0.0f, 1.0f);
                this.renderRect(this.editSquareX, (float)this.editSquareY + 0.95f, 1.0f, 0.05f, r, g, 0.0f, 1.0f);
                this.renderRect((float)this.editSquareX + 0.95f, this.editSquareY, 0.05f, 1.0f, r, g, 0.0f, 1.0f);
            }
        }
        if (selChunkRegion != null) {
            this.debugLine("- ChunkRegion -");
            this.debugLine("ID: " + selChunkRegion.getID());
            this.debugLine("Squares: " + selChunkRegion.getSquareSize());
            this.debugLine("Roofs: " + selChunkRegion.getRoofCnt());
            this.debugLine("Neighbors: " + selChunkRegion.getNeighborCount());
            this.debugLine("ConnectedNeighbors: " + selChunkRegion.getConnectedNeighbors().size());
            this.debugLine("FullyEnclosed: " + selChunkRegion.getIsEnclosed());
        }
        if (selWorldRegion != null) {
            this.debugLine("- WorldRegion -");
            this.debugLine("ID: " + selWorldRegion.getID());
            this.debugLine("Squares: " + selWorldRegion.getSquareSize());
            this.debugLine("Roofs: " + selWorldRegion.getRoofCnt());
            this.debugLine("IsFullyRoofed: " + selWorldRegion.isFullyRoofed());
            this.debugLine("RoofPercentage: " + selWorldRegion.getRoofedPercentage());
            this.debugLine("IsEnclosed: " + selWorldRegion.isEnclosed());
            this.debugLine("Neighbors: " + selWorldRegion.getNeighbors().size());
            this.debugLine("ChunkRegionCount: " + selWorldRegion.size());
        }
        int y = 15;
        for (int i = 0; i < this.debugLines.size(); ++i) {
            this.renderStringUI(10.0f, y, this.debugLines.get(i), Colors.CornFlowerBlue);
            y += TextManager.instance.getFontHeight(UIFont.Small);
        }
    }

    public void setEditSquareCoord(int x, int y) {
        this.editSquareX = x;
        this.editSquareY = y;
        this.editSquareInRange = false;
        if (this.editCoordInRange(x, y)) {
            this.editSquareInRange = true;
        }
    }

    private boolean editCoordInRange(int x, int y) {
        IsoGridSquare gs = IsoWorld.instance.getCell().getGridSquare(x, y, 0);
        return gs != null;
    }

    public void editSquare(int x, int y) {
        if (this.isEditingEnabled()) {
            int z = this.getZLevel();
            IsoGridSquare gs = IsoWorld.instance.getCell().getGridSquare(x, y, z);
            DataRoot root = IsoRegions.getDataRoot();
            byte flags = root.getSquareFlags(x, y, z);
            if (this.editCoordInRange(x, y)) {
                if (gs == null && (gs = IsoWorld.instance.getCell().createNewGridSquare(x, y, z, true)) == null) {
                    return;
                }
                this.editSquareInRange = true;
                block12: for (int i = 0; i < this.editOptions.size(); ++i) {
                    BooleanDebugOption setting = (BooleanDebugOption)this.editOptions.get(i);
                    if (!setting.getValue()) continue;
                    switch (setting.getName()) {
                        case "Edit.WallW": 
                        case "Edit.WallN": {
                            IsoThumpable thumpable;
                            if (setting.getName().equals("Edit.WallN")) {
                                if (flags > 0 && Bits.hasFlags(flags, 1)) {
                                    return;
                                }
                                thumpable = new IsoThumpable(IsoWorld.instance.getCell(), gs, "walls_exterior_wooden_01_25", true, null);
                            } else {
                                if (flags > 0 && Bits.hasFlags(flags, 2)) {
                                    return;
                                }
                                thumpable = new IsoThumpable(IsoWorld.instance.getCell(), gs, "walls_exterior_wooden_01_24", true, null);
                            }
                            thumpable.setMaxHealth(100);
                            thumpable.setName("Wall Debug");
                            thumpable.setBreakSound("BreakObject");
                            gs.AddSpecialObject(thumpable);
                            gs.RecalcAllWithNeighbours(true);
                            thumpable.transmitCompleteItemToServer();
                            if (gs.getZone() == null) continue block12;
                            gs.getZone().setHaveConstruction(true);
                            continue block12;
                        }
                        case "Edit.DoorW": 
                        case "Edit.DoorN": {
                            IsoThumpable thumpable;
                            if (setting.getName().equals("Edit.DoorN")) {
                                if (flags > 0 && Bits.hasFlags(flags, 1)) {
                                    return;
                                }
                                thumpable = new IsoThumpable(IsoWorld.instance.getCell(), gs, "walls_exterior_wooden_01_35", true, null);
                            } else {
                                if (flags > 0 && Bits.hasFlags(flags, 2)) {
                                    return;
                                }
                                thumpable = new IsoThumpable(IsoWorld.instance.getCell(), gs, "walls_exterior_wooden_01_34", true, null);
                            }
                            thumpable.setMaxHealth(100);
                            thumpable.setName("Door Frame Debug");
                            thumpable.setBreakSound("BreakObject");
                            gs.AddSpecialObject(thumpable);
                            gs.RecalcAllWithNeighbours(true);
                            thumpable.transmitCompleteItemToServer();
                            if (gs.getZone() == null) continue block12;
                            gs.getZone().setHaveConstruction(true);
                            continue block12;
                        }
                        case "Edit.Floor": {
                            if (flags > 0 && Bits.hasFlags(flags, 16)) {
                                return;
                            }
                            if (z == 0) {
                                return;
                            }
                            gs.addFloor("carpentry_02_56");
                            if (gs.getZone() == null) continue block12;
                            gs.getZone().setHaveConstruction(true);
                        }
                    }
                }
            } else {
                this.editSquareInRange = false;
            }
        }
    }

    public boolean isEditingEnabled() {
        return this.editingEnabled;
    }

    public void editRotate() {
        if (this.editWallN.getValue()) {
            this.editWallN.setValue(false);
            this.editWallW.setValue(true);
        } else if (this.editWallW.getValue()) {
            this.editWallW.setValue(false);
            this.editWallN.setValue(true);
        }
        if (this.editDoorN.getValue()) {
            this.editDoorN.setValue(false);
            this.editDoorW.setValue(true);
        } else if (this.editDoorW.getValue()) {
            this.editDoorW.setValue(false);
            this.editDoorN.setValue(true);
        }
    }

    public ConfigOption getEditOptionByName(String name) {
        for (int i = 0; i < this.editOptions.size(); ++i) {
            ConfigOption setting = this.editOptions.get(i);
            if (!setting.getName().equals(name)) continue;
            return setting;
        }
        return null;
    }

    public int getEditOptionCount() {
        return this.editOptions.size();
    }

    public ConfigOption getEditOptionByIndex(int index) {
        return this.editOptions.get(index);
    }

    public void setEditOption(int index, boolean b) {
        for (int i = 0; i < this.editOptions.size(); ++i) {
            BooleanDebugOption setting = (BooleanDebugOption)this.editOptions.get(i);
            if (i != index) {
                setting.setValue(false);
                continue;
            }
            setting.setValue(b);
            this.editingEnabled = b;
        }
    }

    public int getZLevel() {
        if (this.zLevelPlayer.getValue()) {
            return PZMath.fastfloor(IsoPlayer.getInstance().getZ());
        }
        for (int i = 0; i < this.zLevelOptions.size(); ++i) {
            BooleanDebugOption setting = (BooleanDebugOption)this.zLevelOptions.get(i);
            if (!setting.getValue()) continue;
            return setting.zLevel;
        }
        return 0;
    }

    public ConfigOption getZLevelOptionByName(String name) {
        for (int i = 0; i < this.zLevelOptions.size(); ++i) {
            ConfigOption setting = this.zLevelOptions.get(i);
            if (!setting.getName().equals(name)) continue;
            return setting;
        }
        return null;
    }

    public int getZLevelOptionCount() {
        return this.zLevelOptions.size();
    }

    public ConfigOption getZLevelOptionByIndex(int index) {
        return this.zLevelOptions.get(index);
    }

    public void setZLevelOption(int index, boolean b) {
        for (int i = 0; i < this.zLevelOptions.size(); ++i) {
            BooleanDebugOption setting = (BooleanDebugOption)this.zLevelOptions.get(i);
            if (i != index) {
                setting.setValue(false);
                continue;
            }
            setting.setValue(b);
        }
        if (!b) {
            this.zLevelPlayer.setValue(true);
        }
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
        String fileName = ZomboidFileSystem.instance.getCacheDir() + File.separator + "isoregions-options.ini";
        ConfigFile configFile = new ConfigFile();
        configFile.write(fileName, 1, this.options);
    }

    public void load() {
        ConfigFile configFile = new ConfigFile();
        String fileName = ZomboidFileSystem.instance.getCacheDir() + File.separator + "isoregions-options.ini";
        if (configFile.read(fileName)) {
            for (int i = 0; i < configFile.getOptions().size(); ++i) {
                ConfigOption configOption = configFile.getOptions().get(i);
                ConfigOption myOption = this.getOptionByName(configOption.getName());
                if (myOption == null) continue;
                myOption.parse(configOption.getValueAsString());
            }
        }
    }

    @UsedFromLua
    public static class BooleanDebugOption
    extends BooleanConfigOption {
        private final int index;
        private int zLevel;

        public BooleanDebugOption(ArrayList<ConfigOption> optionList, String name, boolean defaultValue, int zLevel) {
            super(name, defaultValue);
            this.index = optionList.size();
            this.zLevel = zLevel;
            optionList.add(this);
        }

        public BooleanDebugOption(ArrayList<ConfigOption> optionList, String name, boolean defaultValue) {
            super(name, defaultValue);
            this.index = optionList.size();
            optionList.add(this);
        }

        public int getIndex() {
            return this.index;
        }
    }
}

