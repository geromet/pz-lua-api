/*
 * Decompiled with CFR 0.152.
 */
package zombie.iso.areas.isoregion;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import zombie.UsedFromLua;
import zombie.ZomboidFileSystem;
import zombie.characters.IsoPlayer;
import zombie.core.Color;
import zombie.core.Colors;
import zombie.core.Core;
import zombie.core.math.PZMath;
import zombie.core.network.ByteBufferReader;
import zombie.core.raknet.UdpConnection;
import zombie.iso.IsoChunk;
import zombie.iso.IsoChunkMap;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoWorld;
import zombie.iso.SpriteDetails.IsoFlagType;
import zombie.iso.SpriteDetails.IsoObjectType;
import zombie.iso.areas.isoregion.IsoRegionWorker;
import zombie.iso.areas.isoregion.IsoRegionsLogger;
import zombie.iso.areas.isoregion.data.DataChunk;
import zombie.iso.areas.isoregion.data.DataRoot;
import zombie.iso.areas.isoregion.data.DataSquarePos;
import zombie.iso.areas.isoregion.regions.IChunkRegion;
import zombie.iso.areas.isoregion.regions.IWorldRegion;
import zombie.iso.areas.isoregion.regions.IsoWorldRegion;
import zombie.network.GameClient;
import zombie.network.GameServer;

@UsedFromLua
public final class IsoRegions {
    public static final int SINGLE_CHUNK_PACKET_SIZE = 2076;
    public static final int CHUNKS_DATA_PACKET_SIZE = 65536;
    public static boolean printD;
    public static final int CELL_DIM = 256;
    public static final int CELL_CHUNK_DIM = 32;
    public static final int CHUNK_DIM = 8;
    public static final int CHUNK_MAX_Z = 32;
    public static final byte BIT_EMPTY = 0;
    public static final byte BIT_WALL_N = 1;
    public static final byte BIT_WALL_W = 2;
    public static final byte BIT_PATH_WALL_N = 4;
    public static final byte BIT_PATH_WALL_W = 8;
    public static final byte BIT_HAS_FLOOR = 16;
    public static final byte BIT_STAIRCASE = 32;
    public static final byte BIT_HAS_ROOF = 64;
    public static final byte DIR_NONE = -1;
    public static final byte DIR_N = 0;
    public static final byte DIR_W = 1;
    public static final byte DIR_2D_NW = 2;
    public static final byte DIR_S = 2;
    public static final byte DIR_E = 3;
    public static final byte DIR_2D_MAX = 4;
    public static final byte DIR_TOP = 4;
    public static final byte DIR_BOT = 5;
    public static final byte DIR_MAX = 6;
    protected static final int CHUNK_LOAD_DIMENSIONS = 7;
    protected static boolean debugLoadAllChunks;
    public static final String FILE_PRE = "datachunk_";
    public static final String FILE_SEP = "_";
    public static final String FILE_EXT = ".bin";
    public static final String FILE_DIR = "isoregiondata";
    private static final int SQUARE_CHANGE_WARN_THRESHOLD = 20;
    private static int squareChangePerTick;
    private static String cacheDir;
    private static File cacheDirFile;
    private static File headDataFile;
    private static final Map<Integer, File> chunkFileNames;
    private static IsoRegionWorker regionWorker;
    private static DataRoot dataRoot;
    private static IsoRegionsLogger logger;
    protected static int lastChunkX;
    protected static int lastChunkY;
    private static byte previousFlags;

    public static File getHeaderFile() {
        return headDataFile;
    }

    public static File getDirectory() {
        return cacheDirFile;
    }

    public static File getChunkFile(int chunkX, int chunkY) {
        File f;
        int hashID = IsoRegions.hash(chunkX, chunkY);
        if (chunkFileNames.containsKey(hashID) && (f = chunkFileNames.get(hashID)) != null) {
            return chunkFileNames.get(hashID);
        }
        String filename = cacheDir + FILE_PRE + chunkX + FILE_SEP + chunkY + FILE_EXT;
        File f2 = new File(filename);
        chunkFileNames.put(hashID, f2);
        return f2;
    }

    public static byte GetOppositeDir(byte dir) {
        if (dir == 0) {
            return 2;
        }
        if (dir == 1) {
            return 3;
        }
        if (dir == 2) {
            return 0;
        }
        if (dir == 3) {
            return 1;
        }
        if (dir == 4) {
            return 5;
        }
        if (dir == 5) {
            return 4;
        }
        return -1;
    }

    public static void setDebugLoadAllChunks(boolean b) {
        debugLoadAllChunks = b;
    }

    public static boolean isDebugLoadAllChunks() {
        return debugLoadAllChunks;
    }

    public static int hash(int x, int y) {
        return y << 16 ^ x;
    }

    protected static DataRoot getDataRoot() {
        return dataRoot;
    }

    public static void init() {
        if (!Core.debug) {
            printD = false;
            DataSquarePos.debugPool = false;
        }
        logger = new IsoRegionsLogger(printD);
        chunkFileNames.clear();
        cacheDir = ZomboidFileSystem.instance.getFileNameInCurrentSave(FILE_DIR) + File.separator;
        cacheDirFile = new File(cacheDir);
        if (!cacheDirFile.exists()) {
            cacheDirFile.mkdir();
        }
        String filename = cacheDir + "RegionHeader.bin";
        headDataFile = new File(filename);
        previousFlags = 0;
        dataRoot = new DataRoot();
        regionWorker = new IsoRegionWorker();
        regionWorker.create();
        regionWorker.load();
    }

    public static IsoRegionsLogger getLogger() {
        return logger;
    }

    public static void log(String str) {
        logger.log(str);
    }

    public static void log(String str, Color col) {
        logger.log(str, col);
    }

    public static void warn(String str) {
        logger.warn(str);
    }

    public static void reset() {
        previousFlags = 0;
        regionWorker.stop();
        regionWorker = null;
        dataRoot = null;
        chunkFileNames.clear();
    }

    public static void receiveServerUpdatePacket(ByteBufferReader input) {
        if (regionWorker == null) {
            logger.warn("IsoRegion cannot receive server packet, regionWorker == null.");
            return;
        }
        if (GameClient.client) {
            regionWorker.readServerUpdatePacket(input);
        }
    }

    public static void receiveClientRequestFullDataChunks(ByteBufferReader input, UdpConnection conn) {
        if (regionWorker == null) {
            logger.warn("IsoRegion cannot receive client packet, regionWorker == null.");
            return;
        }
        if (GameServer.server) {
            regionWorker.readClientRequestFullUpdatePacket(input, conn);
        }
    }

    public static void update() {
        if (Core.debug && squareChangePerTick > 20) {
            logger.warn("IsoRegion Warning -> " + squareChangePerTick + " squares have been changed in one tick.");
        }
        squareChangePerTick = 0;
        if (IsoRegionWorker.isRequestingBufferSwap.get()) {
            logger.log("IsoRegion Swapping DataRoot");
            DataRoot root = dataRoot;
            dataRoot = regionWorker.getRootBuffer();
            regionWorker.setRootBuffer(root);
            IsoRegionWorker.isRequestingBufferSwap.set(false);
            if (!GameServer.server) {
                IsoRegions.clientResetCachedRegionReferences();
                dataRoot.clientProcessBuildings();
            }
        }
        if (!GameClient.client && !GameServer.server && debugLoadAllChunks && Core.debug) {
            int cx = PZMath.fastfloor(IsoPlayer.getInstance().getX()) / 8;
            int cy = PZMath.fastfloor(IsoPlayer.getInstance().getY()) / 8;
            if (lastChunkX != cx || lastChunkY != cy) {
                lastChunkX = cx;
                lastChunkY = cy;
                regionWorker.readSurroundingChunks(cx, cy, IsoChunkMap.chunkGridWidth - 2, true);
            }
        }
        regionWorker.update();
        logger.update();
    }

    protected static void forceRecalcSurroundingChunks() {
        if (!Core.debug || GameClient.client) {
            return;
        }
        logger.log("[DEBUG] Forcing a full load/recalculate of chunks surrounding player.", Colors.Gold);
        int cx = PZMath.fastfloor(IsoPlayer.getInstance().getX()) / 8;
        int cy = PZMath.fastfloor(IsoPlayer.getInstance().getY()) / 8;
        regionWorker.readSurroundingChunks(cx, cy, IsoChunkMap.chunkGridWidth - 2, true, true);
    }

    public static byte getSquareFlags(int x, int y, int z) {
        return dataRoot.getSquareFlags(x, y, z);
    }

    public static IWorldRegion getIsoWorldRegion(int x, int y, int z) {
        return dataRoot.getIsoWorldRegion(x, y, z);
    }

    public static List<IsoWorldRegion> getIsoWorldRegionsInCell(int cellX, int cellY, ArrayList<IsoWorldRegion> worldRegions) {
        return IsoRegions.getDataRoot().getIsoWorldRegionsInCell(cellX, cellY, worldRegions);
    }

    public static DataChunk getDataChunk(int chunkx, int chunky) {
        return dataRoot.getDataChunk(chunkx, chunky);
    }

    public static IChunkRegion getChunkRegion(int x, int y, int z) {
        return dataRoot.getIsoChunkRegion(x, y, z);
    }

    public static void ResetAllDataDebug() {
        if (!Core.debug) {
            return;
        }
        if (GameServer.server || GameClient.client) {
            return;
        }
        regionWorker.addDebugResetJob();
    }

    private static void clientResetCachedRegionReferences() {
        if (GameServer.server) {
            return;
        }
        boolean chunkMinX = false;
        boolean chunkMinY = false;
        int chunkMaxX = IsoChunkMap.chunkGridWidth;
        int chunkMaxY = IsoChunkMap.chunkGridWidth;
        for (int playerIndex = 0; playerIndex < IsoPlayer.numPlayers; ++playerIndex) {
            IsoChunkMap cm = IsoWorld.instance.getCell().getChunkMap(playerIndex);
            if (cm == null || cm.ignore) {
                return;
            }
            for (int xx = 0; xx < chunkMaxX; ++xx) {
                for (int yy = 0; yy < chunkMaxY; ++yy) {
                    IsoChunk c = cm.getChunk(xx, yy);
                    if (c == null) continue;
                    for (int z = 0; z < c.squares.length; ++z) {
                        for (int p = 0; p < c.squares[0].length; ++p) {
                            IsoGridSquare sq = c.squares[z][p];
                            if (sq == null) continue;
                            sq.setIsoWorldRegion(null);
                        }
                    }
                }
            }
        }
    }

    public static void setPreviousFlags(IsoGridSquare gs) {
        previousFlags = IsoRegions.calculateSquareFlags(gs);
    }

    public static void squareChanged(IsoGridSquare gs) {
        IsoRegions.squareChanged(gs, false);
    }

    public static void squareChanged(IsoGridSquare gs, boolean isRemoval) {
        if (GameClient.client) {
            return;
        }
        if (gs == null) {
            return;
        }
        byte flags = IsoRegions.calculateSquareFlags(gs);
        if (flags == previousFlags) {
            return;
        }
        regionWorker.addSquareChangedJob(gs.getX(), gs.getY(), gs.getZ(), isRemoval, flags);
        ++squareChangePerTick;
        previousFlags = 0;
    }

    protected static byte calculateSquareFlags(IsoGridSquare gs) {
        int flags = 0;
        if (gs != null) {
            if (gs.has(IsoFlagType.solidfloor)) {
                flags |= 0x10;
            }
            if (gs.has(IsoFlagType.cutN) || gs.has(IsoObjectType.doorFrN)) {
                flags |= 1;
                if (gs.has(IsoFlagType.WindowN) || gs.has(IsoFlagType.windowN) || gs.has(IsoFlagType.DoorWallN)) {
                    flags |= 4;
                }
            }
            if (!gs.has(IsoFlagType.WallSE) && (gs.has(IsoFlagType.cutW) || gs.has(IsoObjectType.doorFrW))) {
                flags |= 2;
                if (gs.has(IsoFlagType.WindowW) || gs.has(IsoFlagType.windowW) || gs.has(IsoFlagType.DoorWallW)) {
                    flags |= 8;
                }
            }
            if (gs.HasStairsNorth() || gs.HasStairsWest()) {
                flags |= 0x20;
            }
        }
        return (byte)flags;
    }

    protected static IsoRegionWorker getRegionWorker() {
        return regionWorker;
    }

    static {
        chunkFileNames = new HashMap<Integer, File>();
        lastChunkX = -1;
        lastChunkY = -1;
        previousFlags = 0;
    }
}

