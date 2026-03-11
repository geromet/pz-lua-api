/*
 * Decompiled with CFR 0.152.
 */
package zombie.iso;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.locks.ReentrantLock;
import zombie.GameProfiler;
import zombie.GameTime;
import zombie.UsedFromLua;
import zombie.characters.IsoGameCharacter;
import zombie.characters.IsoPlayer;
import zombie.core.Color;
import zombie.core.Core;
import zombie.core.PerformanceSettings;
import zombie.core.logger.ExceptionLogger;
import zombie.core.math.PZMath;
import zombie.core.physics.WorldSimulation;
import zombie.core.profiling.AbstractPerformanceProfileProbe;
import zombie.core.profiling.PerformanceProfileProbe;
import zombie.core.textures.ColorInfo;
import zombie.core.utils.UpdateLimit;
import zombie.debug.DebugLog;
import zombie.debug.DebugOptions;
import zombie.iso.ChunkSaveWorker;
import zombie.iso.IsoCamera;
import zombie.iso.IsoCell;
import zombie.iso.IsoChunk;
import zombie.iso.IsoFloorBloodSplat;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoWorld;
import zombie.iso.LightingJNI;
import zombie.iso.LightingThread;
import zombie.iso.WorldReuserThread;
import zombie.iso.WorldStreamer;
import zombie.iso.areas.IsoRoom;
import zombie.iso.sprite.IsoSprite;
import zombie.iso.sprite.IsoSpriteManager;
import zombie.network.GameClient;
import zombie.network.GameServer;
import zombie.network.ServerMap;
import zombie.ui.TextManager;
import zombie.vehicles.BaseVehicle;
import zombie.vehicles.VehicleCache;
import zombie.vehicles.VehicleManager;

@UsedFromLua
public final class IsoChunkMap {
    public static final int LEVELS = 64;
    public static final int GROUND_LEVEL = 32;
    public static final int TOP_LEVEL = 31;
    public static final int BOTTOM_LEVEL = -32;
    public static final int OLD_CHUNKS_PER_WIDTH = 10;
    public static final int CHUNKS_PER_WIDTH = 8;
    public static final int CHUNK_SIZE_IN_SQUARES = 8;
    public static final HashMap<Integer, IsoChunk> SharedChunks = new HashMap();
    public static int mpWorldXa;
    public static int mpWorldYa;
    public static int mpWorldZa;
    public static int worldXa;
    public static int worldYa;
    public static int worldZa;
    public static final int[] SWorldX;
    public static final int[] SWorldY;
    public static final ConcurrentLinkedQueue<IsoChunk> chunkStore;
    public static final ReentrantLock bSettingChunk;
    private static final int START_CHUNK_GRID_WIDTH = 13;
    public static int chunkGridWidth;
    public static int chunkWidthInTiles;
    private static final ColorInfo inf;
    private static final ArrayList<ArrayList<IsoFloorBloodSplat>> splatByType;
    public int playerId;
    public boolean ignore;
    public int worldX = IsoChunkMap.chunkMapSquareToChunkMapChunkXY(worldXa);
    public int worldY = IsoChunkMap.chunkMapSquareToChunkMapChunkXY(worldYa);
    public final ArrayList<String> filenameServerRequests = new ArrayList();
    protected IsoChunk[] chunksSwapB;
    protected IsoChunk[] chunksSwapA;
    boolean readBufferA = true;
    int xMinTiles = -1;
    int yMinTiles = -1;
    int xMaxTiles = -1;
    int yMaxTiles = -1;
    private final IsoCell cell;
    private final UpdateLimit checkVehiclesFrequency = new UpdateLimit(3000L);
    private final UpdateLimit hotSaveFrequency = new UpdateLimit(1000L);
    public int maxHeight;
    public int minHeight;
    public static final PerformanceProfileProbe ppp_update;

    public IsoChunkMap(IsoCell cell) {
        this.cell = cell;
        WorldReuserThread.instance.finished = false;
        this.chunksSwapB = new IsoChunk[chunkGridWidth * chunkGridWidth];
        this.chunksSwapA = new IsoChunk[chunkGridWidth * chunkGridWidth];
    }

    public static void CalcChunkWidth() {
        float dely;
        if (DebugOptions.instance.worldChunkMap13x13.getValue()) {
            chunkGridWidth = 13;
            chunkWidthInTiles = chunkGridWidth * 8;
            return;
        }
        if (DebugOptions.instance.worldChunkMap11x11.getValue()) {
            chunkGridWidth = 11;
            chunkWidthInTiles = chunkGridWidth * 8;
            return;
        }
        if (DebugOptions.instance.worldChunkMap9x9.getValue()) {
            chunkGridWidth = 9;
            chunkWidthInTiles = chunkGridWidth * 8;
            return;
        }
        if (DebugOptions.instance.worldChunkMap7x7.getValue()) {
            chunkGridWidth = 7;
            chunkWidthInTiles = chunkGridWidth * 8;
            return;
        }
        if (DebugOptions.instance.worldChunkMap5x5.getValue()) {
            chunkGridWidth = 5;
            chunkWidthInTiles = chunkGridWidth * 8;
            return;
        }
        float delx = (float)Core.getInstance().getScreenWidth() / 1920.0f;
        float del = Math.max(delx, dely = (float)Core.getInstance().getScreenHeight() / 1080.0f);
        if (del > 1.0f) {
            del = 1.0f;
        }
        if ((chunkGridWidth = (int)((double)(13.0f * del) * 1.5)) / 2 * 2 == chunkGridWidth) {
            ++chunkGridWidth;
        }
        chunkGridWidth = PZMath.min(chunkGridWidth, 19);
        chunkWidthInTiles = chunkGridWidth * 8;
    }

    public static void setWorldStartPos(int x, int y) {
        IsoChunkMap.SWorldX[IsoPlayer.getPlayerIndex()] = IsoChunkMap.chunkMapSquareToChunkMapChunkXY(x);
        IsoChunkMap.SWorldY[IsoPlayer.getPlayerIndex()] = IsoChunkMap.chunkMapSquareToChunkMapChunkXY(y);
    }

    public void Dispose() {
        IsoChunk.loadGridSquare.clear();
        this.chunksSwapA = null;
        this.chunksSwapB = null;
    }

    public void setInitialPos(int wx, int wy) {
        this.worldX = wx;
        this.worldY = wy;
        this.xMinTiles = -1;
        this.xMaxTiles = -1;
        this.yMinTiles = -1;
        this.yMaxTiles = -1;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public void processAllLoadGridSquare() {
        IsoChunk chunk = IsoChunk.loadGridSquare.poll();
        while (chunk != null) {
            bSettingChunk.lock();
            try {
                boolean loaded = false;
                for (int n = 0; n < IsoPlayer.numPlayers; ++n) {
                    IsoChunkMap cm = IsoWorld.instance.currentCell.chunkMap[n];
                    if (cm.ignore || !cm.setChunkDirect(chunk, false)) continue;
                    loaded = true;
                }
                if (!loaded) {
                    WorldReuserThread.instance.addReuseChunk(chunk);
                } else {
                    chunk.doLoadGridsquare();
                }
            }
            finally {
                bSettingChunk.unlock();
            }
            chunk = IsoChunk.loadGridSquare.poll();
        }
    }

    public void update() {
        try (AbstractPerformanceProfileProbe abstractPerformanceProfileProbe = ppp_update.profile();){
            this.updateInternal();
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    private void updateInternal() {
        IsoChunk chunk;
        boolean bChanged = false;
        int count = IsoChunk.loadGridSquare.size();
        if (count != 0) {
            count = 1 + count * 3 / chunkGridWidth;
        }
        while (count > 0) {
            chunk = IsoChunk.loadGridSquare.poll();
            if (chunk != null) {
                boolean loaded = false;
                for (int n = 0; n < IsoPlayer.numPlayers; ++n) {
                    IsoChunkMap cm = IsoWorld.instance.currentCell.chunkMap[n];
                    if (cm.ignore || !cm.setChunkDirect(chunk, false)) continue;
                    loaded = true;
                }
                if (!loaded) {
                    WorldReuserThread.instance.addReuseChunk(chunk);
                    --count;
                    continue;
                }
                chunk.loaded = true;
                bSettingChunk.lock();
                try {
                    List<VehicleCache> vehicles;
                    try (GameProfiler.ProfileArea n = GameProfiler.getInstance().profile("IsoChunk.doLoadGridsquare");){
                        chunk.doLoadGridsquare();
                        bChanged = true;
                    }
                    if (GameClient.client && (vehicles = VehicleCache.vehicleGet(chunk.wx, chunk.wy)) != null) {
                        for (VehicleCache vehicle : vehicles) {
                            VehicleManager.instance.sendVehicleRequest(vehicle.id, (short)1);
                        }
                    }
                }
                finally {
                    bSettingChunk.unlock();
                }
                for (int i = 0; i < IsoPlayer.numPlayers; ++i) {
                    IsoPlayer player = IsoPlayer.players[i];
                    if (player == null) continue;
                    player.dirtyRecalcGridStackTime = 20.0f;
                }
            }
            --count;
        }
        if (bChanged) {
            this.calculateZExtentsForChunkMap();
        }
        if (this.hotSaveFrequency.Check()) {
            for (int y = 0; y < chunkGridWidth; ++y) {
                for (int x = 0; x < chunkGridWidth; ++x) {
                    chunk = this.getChunk(x, y);
                    if (chunk == null) continue;
                    chunk.update();
                    if (GameClient.client || GameServer.server || !chunk.requiresHotSave || ChunkSaveWorker.instance.toSaveQueue.size() >= 10) continue;
                    ChunkSaveWorker.instance.AddHotSave(chunk);
                    chunk.requiresHotSave = false;
                }
            }
        }
        if (this.checkVehiclesFrequency.Check() && GameClient.client) {
            this.checkVehicles();
        }
    }

    private void checkVehicles() {
        for (int y = 0; y < chunkGridWidth; ++y) {
            for (int x = 0; x < chunkGridWidth; ++x) {
                List<VehicleCache> vehicles;
                IsoChunk chunk = this.getChunk(x, y);
                if (chunk == null || !chunk.loaded || (vehicles = VehicleCache.vehicleGet(chunk.wx, chunk.wy)) == null || chunk.vehicles.size() == vehicles.size()) continue;
                for (int i = 0; i < vehicles.size(); ++i) {
                    short id = vehicles.get((int)i).id;
                    boolean hasID = false;
                    for (int k = 0; k < chunk.vehicles.size(); ++k) {
                        if (chunk.vehicles.get(k).getId() != id) continue;
                        hasID = true;
                        break;
                    }
                    if (hasID || VehicleManager.instance.getVehicleByID(id) != null) continue;
                    VehicleManager.instance.sendVehicleRequest(id, (short)1);
                }
            }
        }
    }

    public void checkIntegrity() {
        IsoWorld.instance.currentCell.chunkMap[0].xMinTiles = -1;
        for (int x = IsoWorld.instance.currentCell.chunkMap[0].getWorldXMinTiles(); x < IsoWorld.instance.currentCell.chunkMap[0].getWorldXMaxTiles(); ++x) {
            for (int y = IsoWorld.instance.currentCell.chunkMap[0].getWorldYMinTiles(); y < IsoWorld.instance.currentCell.chunkMap[0].getWorldYMaxTiles(); ++y) {
                IsoGridSquare grid = IsoWorld.instance.currentCell.getGridSquare(x, y, 0);
                if (grid == null || grid.getX() == x && grid.getY() == y) continue;
                IsoChunk ch = new IsoChunk(IsoWorld.instance.currentCell);
                ch.refs.add(IsoWorld.instance.currentCell.chunkMap[0]);
                WorldStreamer.instance.addJob(ch, x / 8, y / 8, false);
                while (!ch.loaded) {
                    try {
                        Thread.sleep(13L);
                    }
                    catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    public void checkIntegrityThread() {
        IsoWorld.instance.currentCell.chunkMap[0].xMinTiles = -1;
        for (int x = IsoWorld.instance.currentCell.chunkMap[0].getWorldXMinTiles(); x < IsoWorld.instance.currentCell.chunkMap[0].getWorldXMaxTiles(); ++x) {
            for (int y = IsoWorld.instance.currentCell.chunkMap[0].getWorldYMinTiles(); y < IsoWorld.instance.currentCell.chunkMap[0].getWorldYMaxTiles(); ++y) {
                IsoGridSquare grid = IsoWorld.instance.currentCell.getGridSquare(x, y, 0);
                if (grid != null && (grid.getX() != x || grid.getY() != y)) {
                    IsoChunk ch = new IsoChunk(IsoWorld.instance.currentCell);
                    ch.refs.add(IsoWorld.instance.currentCell.chunkMap[0]);
                    WorldStreamer.instance.addJobInstant(ch, x, y, x / 8, y / 8);
                }
                if (grid == null) continue;
            }
        }
    }

    public void LoadChunk(int wx, int wy, int x, int y) {
        if (SharedChunks.containsKey((wx << 16) + wy)) {
            IsoChunk chunk = SharedChunks.get((wx << 16) + wy);
            chunk.setCache();
            this.setChunk(x, y, chunk);
            chunk.refs.add(this);
        } else {
            IsoChunk chunk = chunkStore.poll();
            if (chunk == null) {
                chunk = new IsoChunk(this.cell);
            }
            chunk.assignLoadID();
            SharedChunks.put((wx << 16) + wy, chunk);
            chunk.refs.add(this);
            WorldStreamer.instance.addJob(chunk, wx, wy, false);
        }
    }

    public IsoChunk LoadChunkForLater(int wx, int wy, int x, int y) {
        IsoChunk chunk;
        if (!IsoWorld.instance.getMetaGrid().isValidChunk(wx, wy)) {
            return null;
        }
        if (SharedChunks.containsKey((wx << 16) + wy)) {
            chunk = SharedChunks.get((wx << 16) + wy);
            if (!chunk.refs.contains(this)) {
                chunk.refs.add(this);
                chunk.checkLightingLater_OnePlayer_AllLevels(this.playerId);
            }
            if (!chunk.loaded) {
                return chunk;
            }
            this.setChunk(x, y, chunk);
        } else {
            chunk = chunkStore.poll();
            if (chunk == null) {
                chunk = new IsoChunk(this.cell);
            }
            chunk.assignLoadID();
            SharedChunks.put((wx << 16) + wy, chunk);
            chunk.refs.add(this);
            WorldStreamer.instance.addJob(chunk, wx, wy, true);
        }
        return chunk;
    }

    public IsoChunk getChunkForGridSquare(int worldSquareX, int worldSquareY) {
        int chunkMapSquareX = this.worldSquareToChunkMapSquareX(worldSquareX);
        int chunkMapSquareY = this.worldSquareToChunkMapSquareY(worldSquareY);
        if (this.isChunkMapSquareOutOfRangeXY(chunkMapSquareX) || this.isChunkMapSquareOutOfRangeXY(chunkMapSquareY)) {
            return null;
        }
        int chunkMapChunkX = IsoChunkMap.chunkMapSquareToChunkMapChunkXY(chunkMapSquareX);
        int chunkMapChunkY = IsoChunkMap.chunkMapSquareToChunkMapChunkXY(chunkMapSquareY);
        return this.getChunk(chunkMapChunkX, chunkMapChunkY);
    }

    public IsoChunk getChunkCurrent(int x, int y) {
        if (x < 0 || x >= chunkGridWidth || y < 0 || y >= chunkGridWidth) {
            return null;
        }
        if (!this.readBufferA) {
            return this.chunksSwapA[chunkGridWidth * y + x];
        }
        return this.chunksSwapB[chunkGridWidth * y + x];
    }

    public void setGridSquare(IsoGridSquare square, int worldSquareX, int worldSquareY, int worldSquareZ) {
        IsoChunk c;
        assert (square == null || square.x == worldSquareX && square.y == worldSquareY && square.z == worldSquareZ);
        int chunkMapSquareX = this.worldSquareToChunkMapSquareX(worldSquareX);
        int chunkMapSquareY = this.worldSquareToChunkMapSquareY(worldSquareY);
        if (!GameServer.server && (this.isChunkMapSquareOutOfRangeXY(chunkMapSquareX) || this.isChunkMapSquareOutOfRangeXY(chunkMapSquareY) || this.isWorldSquareOutOfRangeZ(worldSquareZ))) {
            return;
        }
        if (GameServer.server) {
            int chunkMapChunkX = IsoChunkMap.chunkMapSquareToChunkMapChunkXY(worldSquareX);
            int chunkMapChunkY = IsoChunkMap.chunkMapSquareToChunkMapChunkXY(worldSquareY);
            c = ServerMap.instance.getChunk(chunkMapChunkX, chunkMapChunkY);
        } else {
            int chunkMapChunkX = IsoChunkMap.chunkMapSquareToChunkMapChunkXY(chunkMapSquareX);
            int chunkMapChunkY = IsoChunkMap.chunkMapSquareToChunkMapChunkXY(chunkMapSquareY);
            c = this.getChunk(chunkMapChunkX, chunkMapChunkY);
        }
        if (c == null) {
            return;
        }
        c.setSquare(this.chunkMapSquareToChunkSquareXY(chunkMapSquareX), this.chunkMapSquareToChunkSquareXY(chunkMapSquareY), worldSquareZ, square);
    }

    public IsoGridSquare getGridSquare(int worldSquareX, int worldSquareY, int worldSquareZ) {
        int chunkMapSquareX = this.worldSquareToChunkMapSquareX(worldSquareX);
        int chunkMapSquareY = this.worldSquareToChunkMapSquareY(worldSquareY);
        return this.getGridSquareDirect(chunkMapSquareX, chunkMapSquareY, worldSquareZ);
    }

    public IsoGridSquare getGridSquareDirect(int chunkMapSquareX, int chunkMapSquareY, int worldSquareZ) {
        int chunkMapChunkY;
        if (this.isChunkMapSquareOutOfRangeXY(chunkMapSquareX) || this.isChunkMapSquareOutOfRangeXY(chunkMapSquareY) || this.isWorldSquareOutOfRangeZ(worldSquareZ)) {
            return null;
        }
        int chunkMapChunkX = IsoChunkMap.chunkMapSquareToChunkMapChunkXY(chunkMapSquareX);
        IsoChunk c = this.getChunk(chunkMapChunkX, chunkMapChunkY = IsoChunkMap.chunkMapSquareToChunkMapChunkXY(chunkMapSquareY));
        if (c == null) {
            return null;
        }
        if (!c.loaded) {
            return null;
        }
        int chunkSquareX = this.chunkMapSquareToChunkSquareXY(chunkMapSquareX);
        int chunkSquareY = this.chunkMapSquareToChunkSquareXY(chunkMapSquareY);
        return c.getGridSquare(chunkSquareX, chunkSquareY, worldSquareZ);
    }

    private int chunkMapSquareToChunkSquareXY(int chunkMapSquareXY) {
        return chunkMapSquareXY % 8;
    }

    private static int chunkMapSquareToChunkMapChunkXY(int chunkMapSquareXY) {
        return chunkMapSquareXY / 8;
    }

    private boolean isChunkMapSquareOutOfRangeXY(int chunkMapSquareXY) {
        return chunkMapSquareXY < 0 || chunkMapSquareXY >= this.getWidthInTiles();
    }

    private boolean isWorldSquareOutOfRangeZ(int tileZ) {
        return tileZ < -32 || tileZ > 31;
    }

    private int worldSquareToChunkMapSquareX(int worldSquareX) {
        return worldSquareX - (this.worldX - chunkGridWidth / 2) * 8;
    }

    private int worldSquareToChunkMapSquareY(int worldSquareY) {
        return worldSquareY - (this.worldY - chunkGridWidth / 2) * 8;
    }

    public IsoChunk getChunk(int chunkMapChunkX, int chunkMapChunkY) {
        if (chunkMapChunkX < 0 || chunkMapChunkX >= chunkGridWidth || chunkMapChunkY < 0 || chunkMapChunkY >= chunkGridWidth) {
            return null;
        }
        if (this.readBufferA) {
            return this.chunksSwapA[chunkGridWidth * chunkMapChunkY + chunkMapChunkX];
        }
        return this.chunksSwapB[chunkGridWidth * chunkMapChunkY + chunkMapChunkX];
    }

    public IsoChunk[] getChunks() {
        return this.readBufferA ? this.chunksSwapA : this.chunksSwapB;
    }

    private void setChunk(int x, int y, IsoChunk c) {
        if (!this.readBufferA) {
            this.chunksSwapA[IsoChunkMap.chunkGridWidth * y + x] = c;
        } else {
            this.chunksSwapB[IsoChunkMap.chunkGridWidth * y + x] = c;
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public boolean setChunkDirect(IsoChunk c, boolean bRequireLock) {
        long start = System.nanoTime();
        if (bRequireLock) {
            bSettingChunk.lock();
        }
        long start2 = System.nanoTime();
        int x = c.wx - this.worldX;
        int y = c.wy - this.worldY;
        x += chunkGridWidth / 2;
        y += chunkGridWidth / 2;
        if (c.jobType == IsoChunk.JobType.Convert) {
            x = 0;
            y = 0;
        }
        if (c.refs.isEmpty() || x < 0 || y < 0 || x >= chunkGridWidth || y >= chunkGridWidth) {
            if (c.refs.contains(this)) {
                c.refs.remove(this);
                if (c.refs.isEmpty()) {
                    SharedChunks.remove((c.wx << 16) + c.wy);
                }
            }
            if (bRequireLock) {
                bSettingChunk.unlock();
            }
            return false;
        }
        try {
            if (this.readBufferA) {
                this.chunksSwapA[IsoChunkMap.chunkGridWidth * y + x] = c;
            } else {
                this.chunksSwapB[IsoChunkMap.chunkGridWidth * y + x] = c;
            }
            c.loaded = true;
            if (c.jobType == IsoChunk.JobType.None) {
                c.setCache();
                c.updateBuildings();
            }
            double duration1 = (double)(System.nanoTime() - start2) / 1000000.0;
            double duration2 = (double)(System.nanoTime() - start) / 1000000.0;
            if (LightingThread.debugLockTime && duration2 > 10.0) {
                DebugLog.log("setChunkDirect time " + duration1 + "/" + duration2 + " ms");
            }
        }
        finally {
            if (bRequireLock) {
                bSettingChunk.unlock();
            }
        }
        return true;
    }

    public void drawDebugChunkMap() {
        int x = 64;
        for (int n = 0; n < chunkGridWidth; ++n) {
            int y = 0;
            for (int m = 0; m < chunkGridWidth; ++m) {
                IsoGridSquare gr;
                y += 64;
                IsoChunk ch = this.getChunk(n, m);
                if (ch == null || (gr = ch.getGridSquare(0, 0, 0)) != null) continue;
                TextManager.instance.DrawString(x, y, "wx:" + ch.wx + " wy:" + ch.wy);
            }
            x += 128;
        }
    }

    private void LoadLeft() {
        this.xMinTiles = -1;
        this.yMinTiles = -1;
        this.xMaxTiles = -1;
        this.yMaxTiles = -1;
        this.Left();
        WorldSimulation.instance.scrollGroundLeft(this.playerId);
        this.xMinTiles = -1;
        this.yMinTiles = -1;
        this.xMaxTiles = -1;
        this.yMaxTiles = -1;
        for (int y = -(chunkGridWidth / 2); y <= chunkGridWidth / 2; ++y) {
            this.LoadChunkForLater(this.worldX - chunkGridWidth / 2, this.worldY + y, 0, y + chunkGridWidth / 2);
        }
        this.SwapChunkBuffers();
        this.xMinTiles = -1;
        this.yMinTiles = -1;
        this.xMaxTiles = -1;
        this.yMaxTiles = -1;
        this.UpdateCellCache();
        LightingThread.instance.scrollLeft(this.playerId);
    }

    public void SwapChunkBuffers() {
        for (int n = 0; n < chunkGridWidth * chunkGridWidth; ++n) {
            if (this.readBufferA) {
                this.chunksSwapA[n] = null;
                continue;
            }
            this.chunksSwapB[n] = null;
        }
        this.xMaxTiles = -1;
        this.xMinTiles = -1;
        this.yMaxTiles = -1;
        this.yMinTiles = -1;
        this.readBufferA = !this.readBufferA;
    }

    private void setChunk(int n, IsoChunk c) {
        if (!this.readBufferA) {
            this.chunksSwapA[n] = c;
        } else {
            this.chunksSwapB[n] = c;
        }
    }

    private IsoChunk getChunk(int n) {
        if (this.readBufferA) {
            return this.chunksSwapA[n];
        }
        return this.chunksSwapB[n];
    }

    private void LoadRight() {
        this.xMinTiles = -1;
        this.yMinTiles = -1;
        this.xMaxTiles = -1;
        this.yMaxTiles = -1;
        this.Right();
        WorldSimulation.instance.scrollGroundRight(this.playerId);
        this.xMinTiles = -1;
        this.yMinTiles = -1;
        this.xMaxTiles = -1;
        this.yMaxTiles = -1;
        for (int y = -(chunkGridWidth / 2); y <= chunkGridWidth / 2; ++y) {
            this.LoadChunkForLater(this.worldX + chunkGridWidth / 2, this.worldY + y, chunkGridWidth - 1, y + chunkGridWidth / 2);
        }
        this.SwapChunkBuffers();
        this.xMinTiles = -1;
        this.yMinTiles = -1;
        this.xMaxTiles = -1;
        this.yMaxTiles = -1;
        this.UpdateCellCache();
        LightingThread.instance.scrollRight(this.playerId);
    }

    private void LoadUp() {
        this.xMinTiles = -1;
        this.yMinTiles = -1;
        this.xMaxTiles = -1;
        this.yMaxTiles = -1;
        this.Up();
        WorldSimulation.instance.scrollGroundUp(this.playerId);
        this.xMinTiles = -1;
        this.yMinTiles = -1;
        this.xMaxTiles = -1;
        this.yMaxTiles = -1;
        for (int x = -(chunkGridWidth / 2); x <= chunkGridWidth / 2; ++x) {
            this.LoadChunkForLater(this.worldX + x, this.worldY - chunkGridWidth / 2, x + chunkGridWidth / 2, 0);
        }
        this.SwapChunkBuffers();
        this.xMinTiles = -1;
        this.yMinTiles = -1;
        this.xMaxTiles = -1;
        this.yMaxTiles = -1;
        this.UpdateCellCache();
        LightingThread.instance.scrollUp(this.playerId);
    }

    private void LoadDown() {
        this.xMinTiles = -1;
        this.yMinTiles = -1;
        this.xMaxTiles = -1;
        this.yMaxTiles = -1;
        this.Down();
        WorldSimulation.instance.scrollGroundDown(this.playerId);
        this.xMinTiles = -1;
        this.yMinTiles = -1;
        this.xMaxTiles = -1;
        this.yMaxTiles = -1;
        for (int x = -(chunkGridWidth / 2); x <= chunkGridWidth / 2; ++x) {
            this.LoadChunkForLater(this.worldX + x, this.worldY + chunkGridWidth / 2, x + chunkGridWidth / 2, chunkGridWidth - 1);
        }
        this.SwapChunkBuffers();
        this.xMinTiles = -1;
        this.yMinTiles = -1;
        this.xMaxTiles = -1;
        this.yMaxTiles = -1;
        this.UpdateCellCache();
        LightingThread.instance.scrollDown(this.playerId);
    }

    private void UpdateCellCache() {
    }

    private void Up() {
        for (int x = 0; x < chunkGridWidth; ++x) {
            for (int y = chunkGridWidth - 1; y > 0; --y) {
                int wy;
                int wx;
                IsoChunk ch = this.getChunk(x, y);
                if (ch == null && y == chunkGridWidth - 1 && (ch = SharedChunks.get(((wx = this.worldX - chunkGridWidth / 2 + x) << 16) + (wy = this.worldY - chunkGridWidth / 2 + y))) != null) {
                    if (ch.refs.contains(this)) {
                        ch.refs.remove(this);
                        if (ch.refs.isEmpty()) {
                            SharedChunks.remove((ch.wx << 16) + ch.wy);
                        }
                    }
                    ch = null;
                }
                if (ch != null && y == chunkGridWidth - 1) {
                    ch.refs.remove(this);
                    if (ch.refs.isEmpty()) {
                        SharedChunks.remove((ch.wx << 16) + ch.wy);
                        ch.removeFromWorld();
                        ChunkSaveWorker.instance.Add(ch);
                    }
                }
                this.setChunk(x, y, this.getChunk(x, y - 1));
            }
            this.setChunk(x, 0, null);
        }
        --this.worldY;
    }

    private void Down() {
        for (int x = 0; x < chunkGridWidth; ++x) {
            for (int y = 0; y < chunkGridWidth - 1; ++y) {
                int wy;
                int wx;
                IsoChunk ch = this.getChunk(x, y);
                if (ch == null && y == 0 && (ch = SharedChunks.get(((wx = this.worldX - chunkGridWidth / 2 + x) << 16) + (wy = this.worldY - chunkGridWidth / 2 + y))) != null) {
                    if (ch.refs.contains(this)) {
                        ch.refs.remove(this);
                        if (ch.refs.isEmpty()) {
                            SharedChunks.remove((ch.wx << 16) + ch.wy);
                        }
                    }
                    ch = null;
                }
                if (ch != null && y == 0) {
                    ch.refs.remove(this);
                    if (ch.refs.isEmpty()) {
                        SharedChunks.remove((ch.wx << 16) + ch.wy);
                        ch.removeFromWorld();
                        ChunkSaveWorker.instance.Add(ch);
                    }
                }
                this.setChunk(x, y, this.getChunk(x, y + 1));
            }
            this.setChunk(x, chunkGridWidth - 1, null);
        }
        ++this.worldY;
    }

    private void Left() {
        for (int y = 0; y < chunkGridWidth; ++y) {
            for (int x = chunkGridWidth - 1; x > 0; --x) {
                int wy;
                int wx;
                IsoChunk ch = this.getChunk(x, y);
                if (ch == null && x == chunkGridWidth - 1 && (ch = SharedChunks.get(((wx = this.worldX - chunkGridWidth / 2 + x) << 16) + (wy = this.worldY - chunkGridWidth / 2 + y))) != null) {
                    if (ch.refs.contains(this)) {
                        ch.refs.remove(this);
                        if (ch.refs.isEmpty()) {
                            SharedChunks.remove((ch.wx << 16) + ch.wy);
                        }
                    }
                    ch = null;
                }
                if (ch != null && x == chunkGridWidth - 1) {
                    ch.refs.remove(this);
                    if (ch.refs.isEmpty()) {
                        SharedChunks.remove((ch.wx << 16) + ch.wy);
                        ch.removeFromWorld();
                        ChunkSaveWorker.instance.Add(ch);
                    }
                }
                this.setChunk(x, y, this.getChunk(x - 1, y));
            }
            this.setChunk(0, y, null);
        }
        --this.worldX;
    }

    private void Right() {
        for (int y = 0; y < chunkGridWidth; ++y) {
            for (int x = 0; x < chunkGridWidth - 1; ++x) {
                int wy;
                int wx;
                IsoChunk ch = this.getChunk(x, y);
                if (ch == null && x == 0 && (ch = SharedChunks.get(((wx = this.worldX - chunkGridWidth / 2 + x) << 16) + (wy = this.worldY - chunkGridWidth / 2 + y))) != null) {
                    if (ch.refs.contains(this)) {
                        ch.refs.remove(this);
                        if (ch.refs.isEmpty()) {
                            SharedChunks.remove((ch.wx << 16) + ch.wy);
                        }
                    }
                    ch = null;
                }
                if (ch != null && x == 0) {
                    ch.refs.remove(this);
                    if (ch.refs.isEmpty()) {
                        SharedChunks.remove((ch.wx << 16) + ch.wy);
                        ch.removeFromWorld();
                        ChunkSaveWorker.instance.Add(ch);
                    }
                }
                this.setChunk(x, y, this.getChunk(x + 1, y));
            }
            this.setChunk(chunkGridWidth - 1, y, null);
        }
        ++this.worldX;
    }

    public int getWorldXMin() {
        return this.worldX - chunkGridWidth / 2;
    }

    public int getWorldYMin() {
        return this.worldY - chunkGridWidth / 2;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public void ProcessChunkPos(IsoGameCharacter chr) {
        float x1 = chr.getX();
        float y1 = chr.getY();
        int z = PZMath.fastfloor(chr.getZ());
        if (IsoPlayer.getInstance() != null && IsoPlayer.getInstance().getVehicle() != null) {
            IsoPlayer p = IsoPlayer.getInstance();
            BaseVehicle v = p.getVehicle();
            float s = v.getCurrentSpeedKmHour() / 5.0f;
            if (!p.isDriving()) {
                s = Math.min(s * 2.0f, 20.0f);
            }
            x1 += (float)Math.round(p.getForwardDirectionX() * s);
            y1 += (float)Math.round(p.getForwardDirectionY() * s);
        }
        int x = PZMath.fastfloor(x1 / 8.0f);
        int y = PZMath.fastfloor(y1 / 8.0f);
        if (x == this.worldX && y == this.worldY) {
            return;
        }
        long start = System.nanoTime();
        bSettingChunk.lock();
        long start2 = System.nanoTime();
        boolean changed = false;
        try {
            if (Math.abs(x - this.worldX) >= chunkGridWidth || Math.abs(y - this.worldY) >= chunkGridWidth) {
                if (LightingJNI.init) {
                    LightingJNI.teleport(this.playerId, x - chunkGridWidth / 2, y - chunkGridWidth / 2);
                }
                this.Unload();
                IsoPlayer player = IsoPlayer.players[this.playerId];
                player.removeFromSquare();
                player.square = null;
                this.worldX = x;
                this.worldY = y;
                if (!GameServer.server) {
                    WorldSimulation.instance.activateChunkMap(this.playerId);
                }
                int minwx = this.worldX - chunkGridWidth / 2;
                int minwy = this.worldY - chunkGridWidth / 2;
                int maxwx = this.worldX + chunkGridWidth / 2;
                int maxwy = this.worldY + chunkGridWidth / 2;
                for (int xx = minwx; xx <= maxwx; ++xx) {
                    for (int yy = minwy; yy <= maxwy; ++yy) {
                        this.LoadChunkForLater(xx, yy, xx - minwx, yy - minwy);
                    }
                }
                this.SwapChunkBuffers();
                this.UpdateCellCache();
                IsoCell cell = IsoWorld.instance.getCell();
                if (!cell.getObjectList().contains(player) && !cell.getAddList().contains(player)) {
                    cell.getAddList().add(player);
                }
                changed = true;
            } else if (x != this.worldX) {
                if (x < this.worldX) {
                    this.LoadLeft();
                } else {
                    this.LoadRight();
                }
                changed = true;
            } else if (y != this.worldY) {
                if (y < this.worldY) {
                    this.LoadUp();
                } else {
                    this.LoadDown();
                }
                changed = true;
            }
        }
        finally {
            bSettingChunk.unlock();
            if (changed) {
                this.calculateZExtentsForChunkMap();
            }
        }
        double duration1 = (double)(System.nanoTime() - start2) / 1000000.0;
        double duration2 = (double)(System.nanoTime() - start) / 1000000.0;
        if (LightingThread.debugLockTime && duration2 > 10.0) {
            DebugLog.log("ProcessChunkPos time " + duration1 + "/" + duration2 + " ms");
        }
    }

    public void calculateZExtentsForChunkMap() {
        int max = 0;
        int min = 0;
        for (int xx = 0; xx < this.chunksSwapA.length; ++xx) {
            for (int yy = 0; yy < this.chunksSwapA.length; ++yy) {
                IsoChunk c = this.getChunk(xx, yy);
                if (c == null) continue;
                max = Math.max(c.maxLevel, max);
                min = Math.min(min, c.minLevel);
            }
        }
        this.maxHeight = max;
        this.minHeight = min;
    }

    public IsoRoom getRoom(int iD) {
        return null;
    }

    public int getWidthInTiles() {
        return chunkWidthInTiles;
    }

    public int getWorldXMinTiles() {
        if (this.xMinTiles != -1) {
            return this.xMinTiles;
        }
        this.xMinTiles = this.getWorldXMin() * 8;
        return this.xMinTiles;
    }

    public int getWorldYMinTiles() {
        if (this.yMinTiles != -1) {
            return this.yMinTiles;
        }
        this.yMinTiles = this.getWorldYMin() * 8;
        return this.yMinTiles;
    }

    public int getWorldXMaxTiles() {
        if (this.xMaxTiles != -1) {
            return this.xMaxTiles;
        }
        this.xMaxTiles = this.getWorldXMin() * 8 + this.getWidthInTiles();
        return this.xMaxTiles;
    }

    public int getWorldYMaxTiles() {
        if (this.yMaxTiles != -1) {
            return this.yMaxTiles;
        }
        this.yMaxTiles = this.getWorldYMin() * 8 + this.getWidthInTiles();
        return this.yMaxTiles;
    }

    public void Save() {
        if (GameServer.server) {
            return;
        }
        for (int x = 0; x < chunkGridWidth; ++x) {
            for (int y = 0; y < chunkGridWidth; ++y) {
                IsoChunk c = this.getChunk(x, y);
                if (c == null) continue;
                try {
                    c.Save(true);
                    continue;
                }
                catch (IOException e) {
                    ExceptionLogger.logException(e);
                }
            }
        }
    }

    public void renderBloodForChunks(int zza) {
        int n;
        if (!DebugOptions.instance.terrain.renderTiles.bloodDecals.getValue()) {
            return;
        }
        if ((float)zza > IsoCamera.getCameraCharacterZ()) {
            return;
        }
        int optionBloodDecals = Core.getInstance().getOptionBloodDecals();
        if (optionBloodDecals == 0) {
            return;
        }
        float worldAge = (float)GameTime.getInstance().getWorldAgeHours();
        int playerIndex = IsoCamera.frameState.playerIndex;
        for (n = 0; n < IsoFloorBloodSplat.FLOOR_BLOOD_TYPES.length; ++n) {
            splatByType.get(n).clear();
        }
        for (int x = 0; x < chunkGridWidth; ++x) {
            for (int y = 0; y < chunkGridWidth; ++y) {
                IsoFloorBloodSplat b;
                int n2;
                IsoChunk ch = this.getChunk(x, y);
                if (ch == null) continue;
                for (n2 = 0; n2 < ch.floorBloodSplatsFade.size(); ++n2) {
                    b = ch.floorBloodSplatsFade.get(n2);
                    if (b.index >= 1 && b.index <= 10 && IsoChunk.renderByIndex[optionBloodDecals - 1][b.index - 1] == 0 || PZMath.fastfloor(b.z) != zza || b.type < 0 || b.type >= IsoFloorBloodSplat.FLOOR_BLOOD_TYPES.length) continue;
                    b.chunk = ch;
                    splatByType.get(b.type).add(b);
                }
                if (ch.floorBloodSplats.isEmpty()) continue;
                for (n2 = 0; n2 < ch.floorBloodSplats.size(); ++n2) {
                    b = ch.floorBloodSplats.get(n2);
                    if (b.index >= 1 && b.index <= 10 && IsoChunk.renderByIndex[optionBloodDecals - 1][b.index - 1] == 0 || PZMath.fastfloor(b.z) != zza || b.type < 0 || b.type >= IsoFloorBloodSplat.FLOOR_BLOOD_TYPES.length) continue;
                    b.chunk = ch;
                    splatByType.get(b.type).add(b);
                }
            }
        }
        for (n = 0; n < splatByType.size(); ++n) {
            IsoSprite use;
            ArrayList<IsoFloorBloodSplat> splats = splatByType.get(n);
            if (splats.isEmpty()) continue;
            String type = IsoFloorBloodSplat.FLOOR_BLOOD_TYPES[n];
            if (!IsoFloorBloodSplat.spriteMap.containsKey(type)) {
                IsoSprite sp = IsoSprite.CreateSprite(IsoSpriteManager.instance);
                sp.LoadFramesPageSimple(type, type, type, type);
                IsoFloorBloodSplat.spriteMap.put(type, sp);
                use = sp;
            } else {
                use = IsoFloorBloodSplat.spriteMap.get(type);
            }
            for (int i = 0; i < splats.size(); ++i) {
                IsoGridSquare square;
                IsoFloorBloodSplat b = splats.get(i);
                IsoChunkMap.inf.r = 1.0f;
                IsoChunkMap.inf.g = 1.0f;
                IsoChunkMap.inf.b = 1.0f;
                IsoChunkMap.inf.a = 0.27f;
                float aa = (b.x + b.y / b.x) * (float)(b.type + 1);
                float bb = aa * b.x / b.y * (float)(b.type + 1) / (aa + b.y);
                float cc = bb * aa * bb * b.x / (b.y + 2.0f);
                aa *= 42367.543f;
                bb *= 6367.123f;
                cc *= 23367.133f;
                aa %= 1000.0f;
                bb %= 1000.0f;
                cc %= 1000.0f;
                aa /= 1000.0f;
                bb /= 1000.0f;
                cc /= 1000.0f;
                if (aa > 0.25f) {
                    aa = 0.25f;
                }
                IsoChunkMap.inf.r -= aa * 2.0f;
                IsoChunkMap.inf.g -= aa * 2.0f;
                IsoChunkMap.inf.b -= aa * 2.0f;
                IsoChunkMap.inf.r += bb / 3.0f;
                IsoChunkMap.inf.g -= cc / 3.0f;
                IsoChunkMap.inf.b -= cc / 3.0f;
                float deltaAge = worldAge - b.worldAge;
                if (deltaAge >= 0.0f && deltaAge < 72.0f) {
                    float f = 1.0f - deltaAge / 72.0f;
                    IsoChunkMap.inf.r *= 0.2f + f * 0.8f;
                    IsoChunkMap.inf.g *= 0.2f + f * 0.8f;
                    IsoChunkMap.inf.b *= 0.2f + f * 0.8f;
                    IsoChunkMap.inf.a *= 0.25f + f * 0.75f;
                } else {
                    IsoChunkMap.inf.r *= 0.2f;
                    IsoChunkMap.inf.g *= 0.2f;
                    IsoChunkMap.inf.b *= 0.2f;
                    IsoChunkMap.inf.a *= 0.25f;
                }
                if (b.fade > 0) {
                    IsoChunkMap.inf.a *= (float)b.fade / ((float)PerformanceSettings.getLockFPS() * 5.0f);
                    if (--b.fade == 0) {
                        b.chunk.floorBloodSplatsFade.remove(b);
                    }
                }
                if ((square = b.chunk.getGridSquare(PZMath.fastfloor(b.x), PZMath.fastfloor(b.y), PZMath.fastfloor(b.z))) != null) {
                    int l0 = square.getVertLight(0, playerIndex);
                    int l1 = square.getVertLight(1, playerIndex);
                    int l2 = square.getVertLight(2, playerIndex);
                    int l3 = square.getVertLight(3, playerIndex);
                    float r0 = Color.getRedChannelFromABGR(l0);
                    float g0 = Color.getGreenChannelFromABGR(l0);
                    float b0 = Color.getBlueChannelFromABGR(l0);
                    float r1 = Color.getRedChannelFromABGR(l1);
                    float g1 = Color.getGreenChannelFromABGR(l1);
                    float b1 = Color.getBlueChannelFromABGR(l1);
                    float r2 = Color.getRedChannelFromABGR(l2);
                    float g2 = Color.getGreenChannelFromABGR(l2);
                    float b2 = Color.getBlueChannelFromABGR(l2);
                    float r3 = Color.getRedChannelFromABGR(l3);
                    float g3 = Color.getGreenChannelFromABGR(l3);
                    float b3 = Color.getBlueChannelFromABGR(l3);
                    IsoChunkMap.inf.r *= (r0 + r1 + r2 + r3) / 4.0f;
                    IsoChunkMap.inf.g *= (g0 + g1 + g2 + g3) / 4.0f;
                    IsoChunkMap.inf.b *= (b0 + b1 + b2 + b3) / 4.0f;
                }
                use.renderBloodSplat((float)(b.chunk.wx * 8) + b.x, (float)(b.chunk.wy * 8) + b.y, b.z, inf);
            }
        }
    }

    public void copy(IsoChunkMap from) {
        IsoChunkMap to = this;
        to.worldX = from.worldX;
        to.worldY = from.worldY;
        to.xMinTiles = -1;
        to.yMinTiles = -1;
        to.xMaxTiles = -1;
        to.yMaxTiles = -1;
        for (int n = 0; n < chunkGridWidth * chunkGridWidth; ++n) {
            to.readBufferA = from.readBufferA;
            if (to.readBufferA) {
                if (from.chunksSwapA[n] == null) continue;
                from.chunksSwapA[n].refs.add(to);
                to.chunksSwapA[n] = from.chunksSwapA[n];
                continue;
            }
            if (from.chunksSwapB[n] == null) continue;
            from.chunksSwapB[n].refs.add(to);
            to.chunksSwapB[n] = from.chunksSwapB[n];
        }
    }

    public void Unload() {
        for (int y = 0; y < chunkGridWidth; ++y) {
            for (int x = 0; x < chunkGridWidth; ++x) {
                IsoChunk ch = this.getChunk(x, y);
                if (ch == null) continue;
                if (ch.refs.contains(this)) {
                    ch.refs.remove(this);
                    if (ch.refs.isEmpty()) {
                        SharedChunks.remove((ch.wx << 16) + ch.wy);
                        ch.removeFromWorld();
                        ChunkSaveWorker.instance.Add(ch);
                    }
                }
                this.chunksSwapA[y * IsoChunkMap.chunkGridWidth + x] = null;
                this.chunksSwapB[y * IsoChunkMap.chunkGridWidth + x] = null;
            }
        }
        WorldSimulation.instance.deactivateChunkMap(this.playerId);
        this.xMinTiles = -1;
        this.xMaxTiles = -1;
        this.yMinTiles = -1;
        this.yMaxTiles = -1;
        if (IsoWorld.instance != null && IsoWorld.instance.currentCell != null) {
            IsoWorld.instance.currentCell.clearCacheGridSquare(this.playerId);
        }
    }

    public static boolean isGridSquareOutOfRangeZ(int tileZ) {
        return tileZ < -32 || tileZ > 31;
    }

    static {
        worldXa = 11702;
        worldYa = 6896;
        SWorldX = new int[4];
        SWorldY = new int[4];
        chunkStore = new ConcurrentLinkedQueue();
        bSettingChunk = new ReentrantLock(true);
        chunkGridWidth = 13;
        chunkWidthInTiles = 8 * chunkGridWidth;
        inf = new ColorInfo();
        splatByType = new ArrayList();
        for (int i = 0; i < IsoFloorBloodSplat.FLOOR_BLOOD_TYPES.length; ++i) {
            splatByType.add(new ArrayList());
        }
        ppp_update = new PerformanceProfileProbe("IsoChunkMap.update");
    }
}

