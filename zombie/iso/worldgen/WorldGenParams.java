/*
 * Decompiled with CFR 0.152.
 */
package zombie.iso.worldgen;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Random;
import zombie.GameWindow;
import zombie.UsedFromLua;
import zombie.ZomboidFileSystem;
import zombie.core.Core;
import zombie.core.logger.ExceptionLogger;
import zombie.debug.DebugLog;
import zombie.network.GameClient;

@UsedFromLua
public class WorldGenParams {
    private static final byte[] FILE_MAGIC = new byte[]{87, 71, 69, 78};
    @UsedFromLua
    public static final WorldGenParams INSTANCE = new WorldGenParams();
    private String seedString = "";
    private int seed;
    private int minXCell = -250;
    private int minYCell = -250;
    private int maxXCell = 250;
    private int maxYCell = 250;

    private WorldGenParams() {
    }

    public String getSeedString() {
        return this.seedString;
    }

    public void setSeedString(String seedString) {
        this.seedString = seedString;
        this.seed = seedString.hashCode();
    }

    public long getSeed() {
        return this.seed;
    }

    public Random getRandom(int wx, int wy) {
        return this.getRandom(wx, wy, 0L);
    }

    public Random getRandom(int wx, int wy, long offset) {
        Random tmpRnd = new Random((long)this.seed + offset);
        long wxRnd = tmpRnd.nextLong();
        long wyRnd = tmpRnd.nextLong();
        tmpRnd.setSeed((long)wx * wxRnd ^ (long)wy * wyRnd ^ (long)this.seed);
        return tmpRnd;
    }

    public int getMinXCell() {
        return this.minXCell;
    }

    public void setMinXCell(int minXCell) {
        this.minXCell = minXCell;
    }

    public int getMinYCell() {
        return this.minYCell;
    }

    public void setMinYCell(int minYCell) {
        this.minYCell = minYCell;
    }

    public int getMaxXCell() {
        return this.maxXCell;
    }

    public void setMaxXCell(int maxXCell) {
        this.maxXCell = maxXCell;
    }

    public int getMaxYCell() {
        return this.maxYCell;
    }

    public void setMaxYCell(int maxYCell) {
        this.maxYCell = maxYCell;
    }

    public void save() {
        if (GameClient.client || Core.getInstance().isNoSave()) {
            return;
        }
        DebugLog.log("Saving worldgen params");
        try {
            ByteBuffer bb = ByteBuffer.allocate(10000);
            bb.put(FILE_MAGIC);
            bb.putInt(244);
            GameWindow.WriteString(bb, this.getSeedString());
            bb.putInt(this.getMinXCell());
            bb.putInt(this.getMinYCell());
            bb.putInt(this.getMaxXCell());
            bb.putInt(this.getMaxYCell());
            bb.flip();
            File path = new File(ZomboidFileSystem.instance.getFileNameInCurrentSave("map_worldgen.bin"));
            FileOutputStream output = new FileOutputStream(path);
            output.getChannel().truncate(0L);
            output.write(bb.array(), 0, bb.limit());
            output.flush();
            output.close();
        }
        catch (Exception ex) {
            ExceptionLogger.logException(ex);
        }
    }

    public Result load() {
        if (GameClient.client) {
            return Result.CLIENT;
        }
        DebugLog.log("Loading worldgen params");
        File path = new File(ZomboidFileSystem.instance.getFileNameInCurrentSave("map_worldgen.bin"));
        if (!path.exists()) {
            return Result.NOT_PRESENT;
        }
        try (FileInputStream inStream = new FileInputStream(path);){
            ByteBuffer bb = ByteBuffer.allocate((int)path.length());
            bb.clear();
            int len = inStream.read(bb.array());
            bb.limit(len);
            byte[] magic = new byte[4];
            bb.get(magic);
            if (!Arrays.equals(magic, FILE_MAGIC)) {
                throw new IOException(path.getAbsolutePath() + " does not appear to be map_worldgen.bin");
            }
            int worldVersion = bb.getInt();
            this.setSeedString(GameWindow.ReadString(bb));
            this.setMinXCell(bb.getInt());
            this.setMinYCell(bb.getInt());
            this.setMaxXCell(bb.getInt());
            this.setMaxYCell(bb.getInt());
        }
        catch (Exception ex) {
            ExceptionLogger.logException(ex);
        }
        return Result.DONE;
    }

    public static enum Result {
        CLIENT,
        NOT_PRESENT,
        DONE;

    }
}

