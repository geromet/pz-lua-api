/*
 * Decompiled with CFR 0.152.
 */
package zombie.tileDepth;

import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import org.joml.Vector3f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.system.MemoryUtil;
import zombie.UsedFromLua;
import zombie.core.math.PZMath;
import zombie.core.opengl.RenderThread;
import zombie.core.textures.Texture;
import zombie.iso.sprite.IsoSprite;
import zombie.iso.sprite.IsoSpriteManager;
import zombie.tileDepth.TileDepthTextureManager;
import zombie.tileDepth.TileGeometryFile;
import zombie.tileDepth.TileGeometryManager;
import zombie.tileDepth.TileGeometryUtils;
import zombie.tileDepth.TilesetDepthTexture;
import zombie.vehicles.UI3DScene;

@UsedFromLua
public final class TileDepthTexture {
    private final TilesetDepthTexture tileset;
    private final int index;
    private final int width;
    private final int height;
    private float[] pixels;
    private final String name;
    private Texture texture;
    private boolean empty = true;
    private static final float[] s_clampedPixels = new float[8192];
    private static boolean clampedPixelsInit;
    static TileGeometryFile.Polygon floorPolygon;

    public TileDepthTexture(TilesetDepthTexture tileset, int tileIndex) {
        this.tileset = tileset;
        this.index = tileIndex;
        this.width = tileset.getTileWidth();
        this.height = tileset.getTileHeight();
        this.name = this.tileset.getName() + "_" + tileIndex;
    }

    public TilesetDepthTexture getTileset() {
        return this.tileset;
    }

    public int getIndex() {
        return this.index;
    }

    public int getColumn() {
        return this.getIndex() % this.tileset.getColumns();
    }

    public int getRow() {
        return this.getIndex() / this.tileset.getColumns();
    }

    public String getName() {
        return this.name;
    }

    public int getWidth() {
        return this.width;
    }

    public int getHeight() {
        return this.height;
    }

    public boolean isEmpty() {
        return this.empty;
    }

    public float[] getPixels() {
        return this.pixels;
    }

    public void setPixel(int x, int y, float pixel) {
        this.allocPixelsIfNeeded();
        this.pixels[this.index((int)x, (int)y)] = pixel;
    }

    public float getPixel(int x, int y) {
        if (this.pixels == null) {
            return -1.0f;
        }
        return this.pixels[this.index(x, y)];
    }

    public void setMinPixel(int x, int y, float pixel) {
        int index1 = this.index(x, y);
        this.allocPixelsIfNeeded();
        this.pixels[index1] = PZMath.min(this.pixels[index1], pixel);
    }

    public void setPixels(int x, int y, int w, int h, float pixel) {
        if (w <= 0 || h <= 0) {
            return;
        }
        int x1 = x;
        int y1 = y;
        int x2 = x + w - 1;
        int y2 = y + h - 1;
        x1 = PZMath.clamp(x1, 0, this.getWidth() - 1);
        y1 = PZMath.clamp(y1, 0, this.getHeight() - 1);
        x2 = PZMath.clamp(x2, 0, this.getWidth() - 1);
        y2 = PZMath.clamp(y2, 0, this.getHeight() - 1);
        for (y = y1; y <= y2; ++y) {
            for (x = x1; x <= x2; ++x) {
                this.setPixel(x, y, pixel);
            }
        }
    }

    public void replacePixels(int x, int y, int w, int h, float oldPixel, float newPixel) {
        if (w <= 0 || h <= 0) {
            return;
        }
        int x1 = x;
        int y1 = y;
        int x2 = x + w - 1;
        int y2 = y + h - 1;
        x1 = PZMath.clamp(x1, 0, this.getWidth() - 1);
        y1 = PZMath.clamp(y1, 0, this.getHeight() - 1);
        x2 = PZMath.clamp(x2, 0, this.getWidth() - 1);
        y2 = PZMath.clamp(y2, 0, this.getHeight() - 1);
        for (y = y1; y <= y2; ++y) {
            for (x = x1; x <= x2; ++x) {
                if (this.getPixel(x, y) != oldPixel) continue;
                this.setPixel(x, y, newPixel);
            }
        }
    }

    public int index(int x, int y) {
        return x + y * this.width;
    }

    void allocPixelsIfNeeded() {
        if (this.pixels == null) {
            this.pixels = new float[this.width * this.height];
            Arrays.fill(this.pixels, -1.0f);
        }
    }

    @Deprecated
    void load(float[] pixels, BufferedImage bufferedImage, int left, int top) {
        this.empty = true;
        for (int y = 0; y < this.height; ++y) {
            for (int x = 0; x < this.width; ++x) {
                int argb = bufferedImage.getRGB(left + x, top + y);
                int a = argb >> 24 & 0xFF;
                int b = argb & 0xFF;
                float f = pixels[x + y * this.width] = a == 0 ? -1.0f : (float)b / 255.0f;
                if (!this.empty || a == 0) continue;
                this.empty = false;
            }
        }
        if (this.empty) {
            this.pixels = null;
        } else {
            IsoSprite sprite;
            this.allocPixelsIfNeeded();
            System.arraycopy(pixels, 0, this.pixels, 0, this.pixels.length);
            if (TileDepthTextureManager.getInstance().isLoadingFinished() && (sprite = IsoSpriteManager.instance.namedMap.get(this.getName())) != null) {
                sprite.depthTexture = this;
            }
        }
        this.updateGPUTexture();
    }

    void load(float[] pixels, ByteBuffer bb, int stride, int left, int top) {
        int bytesPerPixel = 4;
        this.empty = true;
        for (int y = 0; y < this.height; ++y) {
            int bbRowStart = (top + y) * stride;
            for (int x = 0; x < this.width; ++x) {
                int bbPixelStart = bbRowStart + (left + x) * 4;
                int a = bb.get(bbPixelStart + 3) & 0xFF;
                int b = bb.get(bbPixelStart + 2) & 0xFF;
                float f = pixels[x + y * this.width] = a == 0 ? -1.0f : (float)b / 255.0f;
                if (!this.empty || a == 0) continue;
                this.empty = false;
            }
        }
        if (this.empty) {
            this.pixels = null;
        } else {
            IsoSprite sprite;
            this.allocPixelsIfNeeded();
            System.arraycopy(pixels, 0, this.pixels, 0, this.pixels.length);
            if (TileDepthTextureManager.getInstance().isLoadingFinished() && (sprite = IsoSpriteManager.instance.namedMap.get(this.getName())) != null) {
                sprite.depthTexture = this;
            }
            this.updateGPUTexture();
        }
    }

    BufferedImage setBufferedImage(BufferedImage bufferedImage, int left, int top) {
        int[] rowARGB = new int[this.width];
        for (int y = 0; y < this.height; ++y) {
            for (int x = 0; x < this.width; ++x) {
                float pixel = this.getPixel(x, y);
                if (pixel >= 0.0f) {
                    pixel = PZMath.min(pixel, 1.0f);
                    int rgb = (int)Math.floor(pixel * 255.0f) & 0xFF;
                    int a = 255;
                    rowARGB[x] = 0xFF000000 | rgb << 16 | rgb << 8 | rgb;
                    continue;
                }
                rowARGB[x] = 0;
            }
            bufferedImage.setRGB(left, top + y, this.width, 1, rowARGB, 0, this.width);
        }
        return bufferedImage;
    }

    private float clampPixelToUpperFloor(int x, int y, float pixel) {
        TileDepthTexture.initClampedPixels();
        return PZMath.max(pixel, s_clampedPixels[x + y * 128]);
    }

    private static void initClampedPixels() {
        if (clampedPixelsInit) {
            return;
        }
        clampedPixelsInit = true;
        float top = 2.44949f;
        float pushTopDown = 0.011764706f;
        Vector3f planePoint = new Vector3f(0.0f, 2.44949f, 0.0f);
        for (int y = 0; y < 64; ++y) {
            for (int x = 0; x < 128; ++x) {
                float pixel = TileGeometryUtils.getNormalizedDepthOnPlaneAt((float)x + 0.5f, (float)y + 0.5f, UI3DScene.GridPlane.XZ, planePoint);
                if (pixel >= 0.0f) {
                    pixel += 0.011764706f;
                }
                TileDepthTexture.s_clampedPixels[x + y * 128] = pixel;
            }
        }
    }

    public void save() throws Exception {
        this.getTileset().save();
    }

    public boolean fileExists() {
        return this.getTileset().fileExists();
    }

    public Texture getTexture() {
        if (this.empty && this.texture == null) {
            return TileDepthTextureManager.getInstance().getEmptyDepthTexture(this.width, this.height);
        }
        if (this.texture == null) {
            this.texture = new Texture(this.width, this.height, "DEPTH_" + this.getName(), 0);
            this.updateGPUTexture();
        }
        return this.texture;
    }

    public void updateGPUTexture() {
        if (this.texture == null) {
            this.texture = new Texture(this.width, this.height, "DEPTH_" + this.getName(), 0);
        }
        RenderThread.queueInvokeOnRenderContext(() -> {
            Texture.lastTextureID = this.texture.getID();
            GL11.glBindTexture(3553, Texture.lastTextureID);
            GL11.glTexParameteri(3553, 10241, 9728);
            GL11.glTexParameteri(3553, 10240, 9728);
            boolean bpp = true;
            ByteBuffer pixels = MemoryUtil.memAlloc(this.getWidth() * this.getHeight() * 1);
            pixels.position(this.getWidth() * this.getHeight() * 1);
            boolean bClamp = true;
            if (this.tileset.getName().startsWith("roofs_")) {
                bClamp = false;
            }
            this.empty = true;
            for (int y = 0; y < this.getHeight(); ++y) {
                for (int x = 0; x < this.getWidth(); ++x) {
                    float pixel = this.getPixel(x, y);
                    if (pixel >= 0.0f && y < 64 && bClamp) {
                        pixel = this.clampPixelToUpperFloor(x, y, pixel);
                    }
                    if (pixel < 0.0f) {
                        pixels.put(x * 1 + y * this.getWidth() * 1, (byte)0);
                    } else {
                        int rgb = (int)Math.floor((pixel = PZMath.min(pixel, 1.0f)) * 255.0f) & 0xFF;
                        if (rgb == 0) {
                            rgb = 1;
                        }
                        byte b = (byte)rgb;
                        pixels.put(x * 1 + y * this.getWidth() * 1, b);
                    }
                    if (!(pixel >= 0.0f) || !this.empty) continue;
                    this.empty = false;
                }
            }
            if (this.empty) {
                boolean bl = true;
            }
            pixels.flip();
            GL11.glTexImage2D(3553, 0, 6403, this.getWidth(), this.getHeight(), 0, 6403, 5121, pixels);
            MemoryUtil.memFree(pixels);
            if (this.tileset != null && !this.tileset.isKeepPixels()) {
                this.pixels = null;
            }
        });
    }

    void recalculateDepth() {
        ArrayList<TileGeometryFile.Geometry> geometries = TileGeometryManager.getInstance().getGeometry("game", this.getTileset().getName(), this.getColumn(), this.getRow());
        if (geometries == null || geometries.isEmpty()) {
            return;
        }
        float[] pixels = new float[32768];
        Arrays.fill(pixels, 1000.0f);
        for (int i = 0; i < geometries.size(); ++i) {
            TileGeometryFile.Geometry geometry = geometries.get(i);
            if (geometry.isPolygon()) {
                ((TileGeometryFile.Polygon)geometry).rasterize((tileX, tileY) -> {
                    if (tileX < 0 || tileX >= 128 || tileY < 0 || tileY >= 256) {
                        return;
                    }
                    float newPixel = geometry.getNormalizedDepthAt(tileX, tileY);
                    if (newPixel >= 0.0f) {
                        pixels[this.index((int)tileX, (int)tileY)] = PZMath.min(pixels[this.index(tileX, tileY)], newPixel);
                    }
                });
                continue;
            }
            for (int y = 0; y < this.getHeight(); ++y) {
                for (int x = 0; x < this.getWidth(); ++x) {
                    float newPixel;
                    float pixel = this.getPixel(x, y);
                    if (pixel < 0.0f || !((newPixel = geometry.getNormalizedDepthAt((float)x + 0.5f, (float)y + 0.5f)) >= 0.0f)) continue;
                    pixels[this.index((int)x, (int)y)] = PZMath.min(pixels[this.index(x, y)], newPixel);
                }
            }
        }
        for (int y = 0; y < this.getHeight(); ++y) {
            for (int x = 0; x < this.getWidth(); ++x) {
                float pixel = pixels[this.index(x, y)];
                if (pixel == 1000.0f) continue;
                this.setPixel(x, y, pixel);
            }
        }
        this.updateGPUTexture();
    }

    TileGeometryFile.Geometry getOrCreateFloorPolygon() {
        if (floorPolygon == null) {
            floorPolygon = new TileGeometryFile.Polygon();
            TileDepthTexture.floorPolygon.plane = TileGeometryFile.Plane.XZ;
            TileDepthTexture.floorPolygon.rotate.set(270.0f, 0.0f, 0.0f);
            TileDepthTexture.floorPolygon.points.add(-1.0f);
            TileDepthTexture.floorPolygon.points.add(-1.0f);
            TileDepthTexture.floorPolygon.points.add(1.0f);
            TileDepthTexture.floorPolygon.points.add(-1.0f);
            TileDepthTexture.floorPolygon.points.add(1.0f);
            TileDepthTexture.floorPolygon.points.add(1.0f);
            TileDepthTexture.floorPolygon.points.add(-1.0f);
            TileDepthTexture.floorPolygon.points.add(1.0f);
            TileDepthTexture.floorPolygon.translate.set(0.0f, 0.0125f, 0.0f);
            floorPolygon.triangulate2();
        }
        return floorPolygon;
    }

    void recalculateShadowDepth() {
        ArrayList<TileGeometryFile.Geometry> geometries = TileGeometryManager.getInstance().getGeometry("game", this.getTileset().getName(), this.getColumn(), this.getRow());
        if (geometries == null || geometries.isEmpty()) {
            return;
        }
        float[] pixels = new float[32768];
        Arrays.fill(pixels, 1000.0f);
        for (int y = 0; y < this.getHeight(); ++y) {
            for (int x = 0; x < this.getWidth(); ++x) {
                float pixel = this.getPixel(x, y);
                if (!(pixel < 0.0f)) continue;
                pixels[this.index((int)x, (int)y)] = 1111.0f;
            }
        }
        TileGeometryFile.Geometry floorPolygon = this.getOrCreateFloorPolygon();
        for (int i = 0; i < geometries.size(); ++i) {
            TileGeometryFile.Geometry geometry = geometries.get(i);
            if (geometry.isPolygon()) {
                ((TileGeometryFile.Polygon)geometry).rasterize((tileX, tileY) -> {
                    if (tileX < 0 || tileX >= 128 || tileY < 0 || tileY >= 256) {
                        return;
                    }
                    if (pixels[this.index(tileX, tileY)] == 1111.0f) {
                        return;
                    }
                    float newPixel = geometry.getNormalizedDepthAt((float)tileX + 0.5f, (float)tileY + 0.5f);
                    if (newPixel >= 0.0f) {
                        pixels[this.index((int)tileX, (int)tileY)] = 1111.0f;
                        return;
                    }
                    pixels[this.index((int)tileX, (int)tileY)] = 2222.0f;
                });
                continue;
            }
            for (int y = 0; y < this.getHeight(); ++y) {
                for (int x = 0; x < this.getWidth(); ++x) {
                    if (pixels[this.index(x, y)] == 1111.0f) continue;
                    float newPixel = geometry.getNormalizedDepthAt((float)x + 0.5f, (float)y + 0.5f);
                    pixels[this.index((int)x, (int)y)] = newPixel >= 0.0f ? 1111.0f : 2222.0f;
                }
            }
        }
        TileDepthTexture.floorPolygon.rasterize((tileX, tileY) -> {
            if (tileX < 0 || tileX >= 128 || tileY < 0 || tileY >= 256) {
                return;
            }
            float pixel = pixels[this.index(tileX, tileY)];
            if (pixel != 2222.0f) {
                return;
            }
            float newPixel = floorPolygon.getNormalizedDepthAt((float)tileX + 0.5f, (float)tileY + 0.5f);
            pixels[this.index((int)tileX, (int)tileY)] = newPixel >= 0.0f ? newPixel : 1000.0f;
        });
        for (int y = 0; y < this.getHeight(); ++y) {
            for (int x = 0; x < this.getWidth(); ++x) {
                float pixel = pixels[this.index(x, y)];
                if (!(pixel < 1000.0f)) continue;
                this.setPixel(x, y, pixel);
            }
        }
        this.updateGPUTexture();
    }

    public void reload() throws Exception {
        this.getTileset().reload();
    }

    public void Reset() {
    }
}

