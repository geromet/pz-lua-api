/*
 * Decompiled with CFR 0.152.
 */
package zombie.tileDepth;

import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Paths;
import java.util.Arrays;
import javax.imageio.ImageIO;
import org.lwjgl.system.MemoryUtil;
import zombie.UsedFromLua;
import zombie.core.logger.ExceptionLogger;
import zombie.core.math.PZMath;
import zombie.core.textures.PNGDecoder;
import zombie.core.textures.Texture;
import zombie.iso.sprite.IsoSprite;
import zombie.iso.sprite.IsoSpriteManager;
import zombie.tileDepth.TileDepthTexture;
import zombie.tileDepth.TileDepthTextures;

@UsedFromLua
public final class TilesetDepthTexture {
    private final TileDepthTextures owner;
    private final String name;
    private final int columns;
    private final int rows;
    private final TileDepthTexture[] tiles;
    private final boolean is2x;
    private int fileExists = -1;
    private boolean keepPixels;

    public TilesetDepthTexture(TileDepthTextures owner, String name, int columns, int rows, boolean b2x) {
        this.owner = owner;
        this.name = name;
        this.columns = columns;
        this.rows = rows;
        this.tiles = new TileDepthTexture[this.columns * this.rows];
        this.is2x = b2x;
    }

    public int getColumns() {
        return this.columns;
    }

    public int getRows() {
        return this.rows;
    }

    public boolean is2x() {
        return this.is2x;
    }

    public void setKeepPixels(boolean bKeepPixels) {
        this.keepPixels = bKeepPixels;
    }

    public boolean isKeepPixels() {
        return this.keepPixels;
    }

    public TileDepthTexture getOrCreateTile(int index) {
        if (index < 0 || index >= this.tiles.length) {
            return null;
        }
        TileDepthTexture tile = this.tiles[index];
        if (tile == null) {
            tile = this.createTile(index);
        }
        return tile;
    }

    private TileDepthTexture createTile(int index) {
        TileDepthTexture tile;
        this.tiles[index] = tile = new TileDepthTexture(this, index);
        return tile;
    }

    public TileDepthTexture getOrCreateTile(int col, int row) {
        return this.getOrCreateTile(this.tileIndex(col, row));
    }

    public String getName() {
        return this.name;
    }

    public int getTileWidth() {
        return 64 * (this.is2x ? 2 : 1);
    }

    public int getTileHeight() {
        return 128 * (this.is2x ? 2 : 1);
    }

    public int getWidth() {
        return this.columns * this.getTileWidth();
    }

    public int getHeight() {
        return this.rows * this.getTileHeight();
    }

    public int getTileCount() {
        return this.columns * this.rows;
    }

    private int tileIndex(int col, int row) {
        return col + row * this.columns;
    }

    boolean isEmpty() {
        for (int row = 0; row < this.rows; ++row) {
            for (int col = 0; col < this.columns; ++col) {
                TileDepthTexture tile = this.tiles[this.tileIndex(col, row)];
                if (tile == null || tile.isEmpty()) continue;
                return false;
            }
        }
        return true;
    }

    BufferedImage getBufferedImage() {
        BufferedImage bufferedImage = new BufferedImage(this.getWidth(), this.getHeight(), 2);
        for (int row = 0; row < this.rows; ++row) {
            for (int col = 0; col < this.columns; ++col) {
                TileDepthTexture tile = this.tiles[this.tileIndex(col, row)];
                if (tile == null) continue;
                tile.setBufferedImage(bufferedImage, col * this.getTileWidth(), row * this.getTileHeight());
            }
        }
        return bufferedImage;
    }

    void writeImageToFile(BufferedImage bufferedImage, String fileName) throws Exception {
        File file = new File(fileName);
        ImageIO.write((RenderedImage)bufferedImage, "png", file);
    }

    public String getRelativeFileName() {
        return "media/depthmaps/DEPTH_" + this.getName() + ".png";
    }

    public String getAbsoluteFileName() {
        return this.owner.mediaAbsPath + File.separator + "depthmaps" + File.separator + "DEPTH_" + this.getName() + ".png";
    }

    public void load() throws Exception {
        try (FileInputStream fis = new FileInputStream(this.getAbsoluteFileName());
             BufferedInputStream bis = new BufferedInputStream(fis);){
            PNGDecoder pngDecoder = new PNGDecoder(bis, false);
            int bytesPerPixel = 4;
            int stride = pngDecoder.getWidth() * 4;
            ByteBuffer bb = MemoryUtil.memAlloc(stride * pngDecoder.getHeight());
            pngDecoder.decode(bb, stride, pngDecoder.getHeight(), PNGDecoder.Format.RGBA, 1229209940);
            float[] tempPixels = new float[this.getTileWidth() * this.getTileHeight()];
            for (int row = 0; row < this.rows; ++row) {
                for (int col = 0; col < this.columns; ++col) {
                    TileDepthTexture tile = this.getOrCreateTile(col, row);
                    if (col >= pngDecoder.getWidth() / this.getTileWidth() || row >= pngDecoder.getHeight() / this.getTileHeight()) continue;
                    tile.load(tempPixels, bb, stride, col * this.getTileWidth(), row * this.getTileHeight());
                }
            }
            MemoryUtil.memFree(bb);
        }
    }

    public void save() throws Exception {
        if (this.isEmpty()) {
            if (Files.exists(Paths.get(this.getAbsoluteFileName(), new String[0]), new LinkOption[0])) {
                this.removeFile();
            }
            return;
        }
        BufferedImage bufferedImage = this.getBufferedImage();
        this.writeImageToFile(bufferedImage, this.getAbsoluteFileName());
        this.fileExists = -1;
    }

    public boolean fileExists() {
        if (this.fileExists == -1) {
            this.fileExists = Files.exists(Paths.get(this.getAbsoluteFileName(), new String[0]), new LinkOption[0]) ? 1 : 0;
        }
        return this.fileExists == 1;
    }

    public void removeFile() {
        try {
            Files.delete(Paths.get(this.getAbsoluteFileName(), new String[0]));
        }
        catch (Exception ex) {
            ExceptionLogger.logException(ex);
        }
    }

    public Texture getTexture() {
        return Texture.getSharedTexture(this.getRelativeFileName());
    }

    public void reload() throws Exception {
        if (this.fileExists()) {
            this.load();
        }
    }

    public void mergeTileset(TilesetDepthTexture other) {
        int rows = PZMath.min(this.rows, other.rows);
        int columns = PZMath.min(this.columns, other.columns);
        for (int row = 0; row < rows; ++row) {
            for (int col = 0; col < columns; ++col) {
                int indexSelf = col + row * this.columns;
                int indexOther = col + row * other.columns;
                if (this.tiles[indexSelf] != null && !this.tiles[indexSelf].isEmpty() || other.tiles[indexOther] == null || other.tiles[indexOther].isEmpty()) continue;
                this.tiles[indexSelf] = other.tiles[indexOther];
            }
        }
    }

    public void initSprites() {
        for (int i = 0; i < this.getTileCount(); ++i) {
            IsoSprite sprite;
            TileDepthTexture tile = this.tiles[i];
            if (tile == null || (sprite = IsoSpriteManager.instance.namedMap.get(tile.getName())) == null || sprite.depthTexture != null || tile.isEmpty()) continue;
            sprite.depthTexture = tile;
        }
    }

    void recalculateDepth() {
        for (int row = 0; row < this.rows; ++row) {
            for (int col = 0; col < this.columns; ++col) {
                TileDepthTexture tile = this.tiles[this.tileIndex(col, row)];
                if (tile == null || tile.isEmpty()) continue;
                tile.recalculateDepth();
            }
        }
    }

    public void recalculateShadowDepth() {
        for (int row = 0; row < this.rows; ++row) {
            for (int col = 0; col < this.columns; ++col) {
                TileDepthTexture tile = this.tiles[this.tileIndex(col, row)];
                if (tile == null || tile.isEmpty()) continue;
                tile.recalculateShadowDepth();
            }
        }
    }

    public void clearTiles() {
        Arrays.fill(this.tiles, null);
    }

    public void Reset() {
        for (int i = 0; i < this.tiles.length; ++i) {
            TileDepthTexture tile = this.tiles[i];
            if (tile == null) continue;
            this.tiles[i] = null;
            tile.Reset();
        }
    }
}

