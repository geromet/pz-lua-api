/*
 * Decompiled with CFR 0.152.
 */
package zombie.erosion;

import java.io.File;
import java.nio.file.CopyOption;
import java.nio.file.Files;
import zombie.GameTime;
import zombie.Lua.LuaEventManager;
import zombie.SandboxOptions;
import zombie.UsedFromLua;
import zombie.ZomboidFileSystem;
import zombie.characters.IsoPlayer;
import zombie.core.Core;
import zombie.core.network.ByteBufferReader;
import zombie.core.network.ByteBufferWriter;
import zombie.core.random.RandLocation;
import zombie.core.utils.Bits;
import zombie.debug.DebugLog;
import zombie.erosion.ErosionConfig;
import zombie.erosion.ErosionData;
import zombie.erosion.ErosionWorld;
import zombie.erosion.season.ErosionIceQueen;
import zombie.erosion.season.ErosionSeason;
import zombie.erosion.utils.Noise2D;
import zombie.iso.IsoChunk;
import zombie.iso.IsoChunkMap;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoWorld;
import zombie.iso.sprite.IsoSpriteManager;
import zombie.network.GameClient;
import zombie.network.GameServer;
import zombie.network.ServerMap;

@UsedFromLua
public final class ErosionMain {
    private static ErosionMain instance;
    private ErosionConfig cfg;
    private boolean debug;
    private final IsoSpriteManager sprMngr;
    private ErosionIceQueen iceQueen;
    private boolean isSnow;
    private String gameSaveWorld;
    private String cfgPath;
    private IsoChunk chunk;
    private ErosionData.Chunk chunkModData;
    private Noise2D noiseMain;
    private Noise2D noiseMoisture;
    private Noise2D noiseMinerals;
    private Noise2D noiseKudzu;
    private ErosionWorld world;
    private ErosionSeason season;
    private int tickUnit = 144;
    private int ticks;
    private int eTicks;
    private int day;
    private int month;
    private int year;
    private int epoch;
    private static final int[][] soilTable;
    private int snowFrac;
    private int snowFracYesterday;
    private int[] snowFracOnDay;

    public static ErosionMain getInstance() {
        return instance;
    }

    public ErosionMain(IsoSpriteManager isoSpriteManager, boolean debug) {
        instance = this;
        this.sprMngr = isoSpriteManager;
        this.debug = debug;
        this.start();
    }

    public ErosionConfig getConfig() {
        return this.cfg;
    }

    public ErosionSeason getSeasons() {
        return this.season;
    }

    public int getEtick() {
        return this.eTicks;
    }

    public IsoSpriteManager getSpriteManager() {
        return this.sprMngr;
    }

    public void mainTimer() {
        GameTime gameTime;
        if (GameClient.client) {
            if (Core.debug) {
                this.cfg.writeFile(this.cfgPath);
            }
            return;
        }
        int erosionDays = SandboxOptions.instance.erosionDays.getValue();
        if (this.debug) {
            ++this.eTicks;
        } else if (erosionDays < 0) {
            this.eTicks = 0;
        } else if (erosionDays > 0) {
            ++this.ticks;
            this.eTicks = (int)((float)this.ticks / 144.0f / (float)erosionDays * 100.0f);
        } else {
            ++this.ticks;
            if (this.ticks >= this.tickUnit) {
                this.ticks = 0;
                ++this.eTicks;
            }
        }
        if (this.eTicks < 0) {
            this.eTicks = Integer.MAX_VALUE;
        }
        if ((gameTime = GameTime.getInstance()).getDay() != this.day || gameTime.getMonth() != this.month || gameTime.getYear() != this.year) {
            this.month = gameTime.getMonth();
            this.year = gameTime.getYear();
            this.day = gameTime.getDay();
            ++this.epoch;
            this.season.setDay(this.day, this.month, this.year);
            this.snowCheck();
        }
        if (GameServer.server) {
            for (int i = 0; i < ServerMap.instance.loadedCells.size(); ++i) {
                ServerMap.ServerCell cell = ServerMap.instance.loadedCells.get(i);
                if (!cell.isLoaded) continue;
                for (int y = 0; y < 8; ++y) {
                    for (int x = 0; x < 8; ++x) {
                        IsoChunk chunk = cell.chunks[x][y];
                        if (chunk == null) continue;
                        ErosionData.Chunk chunkData = chunk.getErosionData();
                        if (chunkData.eTickStamp == this.eTicks && chunkData.epoch == this.epoch) continue;
                        for (int yy = 0; yy < 8; ++yy) {
                            for (int xx = 0; xx < 8; ++xx) {
                                IsoGridSquare sq = chunk.getGridSquare(xx, yy, 0);
                                if (sq == null) continue;
                                this.loadGridsquare(sq);
                            }
                        }
                        chunkData.eTickStamp = this.eTicks;
                        chunkData.epoch = this.epoch;
                    }
                }
            }
        }
        this.cfg.time.ticks = this.ticks;
        this.cfg.time.eticks = this.eTicks;
        this.cfg.time.epoch = this.epoch;
        this.cfg.writeFile(this.cfgPath);
    }

    public void snowCheck() {
    }

    public int getSnowFraction() {
        return this.snowFrac;
    }

    public int getSnowFractionYesterday() {
        return this.snowFracYesterday;
    }

    public boolean isSnow() {
        return this.isSnow;
    }

    public void sendState(ByteBufferWriter bb) {
        if (!GameServer.server) {
            return;
        }
        bb.putInt(this.eTicks);
        bb.putInt(this.ticks);
        bb.putInt(this.epoch);
        bb.putByte(this.getSnowFraction());
        bb.putByte(this.getSnowFractionYesterday());
        bb.putFloat(GameTime.getInstance().getTimeOfDay());
    }

    public void receiveState(ByteBufferReader bb) {
        if (!GameClient.client) {
            return;
        }
        int oldTicks = this.eTicks;
        int oldEpoch = this.epoch;
        this.eTicks = bb.getInt();
        this.ticks = bb.getInt();
        this.epoch = bb.getInt();
        this.cfg.time.ticks = this.ticks;
        this.cfg.time.eticks = this.eTicks;
        this.cfg.time.epoch = this.epoch;
        byte snowFrac = bb.getByte();
        byte snowFracYesterday = bb.getByte();
        float snowTimeOfDay = bb.getFloat();
        GameTime gameTime = GameTime.getInstance();
        if (gameTime.getDay() != this.day || gameTime.getMonth() != this.month || gameTime.getYear() != this.year) {
            this.month = gameTime.getMonth();
            this.year = gameTime.getYear();
            this.day = gameTime.getDay();
            this.season.setDay(this.day, this.month, this.year);
        }
        if (oldTicks != this.eTicks || oldEpoch != this.epoch) {
            this.updateMapNow();
        }
    }

    private void loadGridsquare(IsoGridSquare square) {
        if (square != null && square.chunk != null && square.getZ() == 0) {
            this.getChunk(square);
            ErosionData.Square erosionModData = square.getErosionData();
            if (!erosionModData.init) {
                this.initGridSquare(square, erosionModData);
                this.world.validateSpawn(square, erosionModData, this.chunkModData);
            }
            if (erosionModData.doNothing) {
                return;
            }
            if (this.chunkModData.eTickStamp >= this.eTicks && this.chunkModData.epoch == this.epoch) {
                return;
            }
            this.world.update(square, erosionModData, this.chunkModData, this.eTicks);
        }
    }

    private void initGridSquare(IsoGridSquare square, ErosionData.Square erosionModData) {
        int sqx = square.getX();
        int sqy = square.getY();
        float noise = this.noiseMain.layeredNoise((float)sqx / 10.0f, (float)sqy / 10.0f);
        erosionModData.noiseMainByte = Bits.packFloatUnitToByte(noise);
        erosionModData.noiseMain = noise;
        erosionModData.noiseMainInt = (int)Math.floor(erosionModData.noiseMain * 100.0f);
        erosionModData.noiseKudzu = this.noiseKudzu.layeredNoise((float)sqx / 10.0f, (float)sqy / 10.0f);
        erosionModData.soil = this.chunkModData.soil;
        erosionModData.rand = new RandLocation(sqx, sqy);
        float magic = (float)erosionModData.rand(sqx, sqy, 100) / 100.0f;
        erosionModData.magicNumByte = Bits.packFloatUnitToByte(magic);
        erosionModData.magicNum = magic;
        erosionModData.regions.clear();
        erosionModData.init = true;
    }

    private void getChunk(IsoGridSquare square) {
        this.chunk = square.getChunk();
        this.chunkModData = this.chunk.getErosionData();
        if (this.chunkModData.init) {
            return;
        }
        this.initChunk(this.chunk, this.chunkModData);
    }

    private void initChunk(IsoChunk chunk, ErosionData.Chunk chunkModData) {
        chunkModData.set(chunk);
        float nx = (float)chunkModData.x / 5.0f;
        float ny = (float)chunkModData.y / 5.0f;
        float moisture = this.noiseMoisture.layeredNoise(nx, ny);
        float minerals = this.noiseMinerals.layeredNoise(nx, ny);
        int moi = moisture < 1.0f ? (int)Math.floor(moisture * 10.0f) : 9;
        int min = minerals < 1.0f ? (int)Math.floor(minerals * 10.0f) : 9;
        chunkModData.init = true;
        chunkModData.eTickStamp = -1;
        chunkModData.epoch = -1;
        chunkModData.moisture = moisture;
        chunkModData.minerals = minerals;
        chunkModData.soil = soilTable[moi][min] - 1;
    }

    private boolean initConfig() {
        File cfgFileSrc;
        String cfgName = "erosion.ini";
        if (GameClient.client) {
            this.cfg = GameClient.instance.erosionConfig;
            assert (this.cfg != null);
            GameClient.instance.erosionConfig = null;
            this.cfgPath = ZomboidFileSystem.instance.getFileNameInCurrentSave("erosion.ini");
            return true;
        }
        this.cfg = new ErosionConfig();
        this.cfgPath = ZomboidFileSystem.instance.getFileNameInCurrentSave("erosion.ini");
        File cfgFile = new File(this.cfgPath);
        if (cfgFile.exists()) {
            DebugLog.DetailedInfo.trace("erosion: reading " + cfgFile.getAbsolutePath());
            if (this.cfg.readFile(cfgFile.getAbsolutePath())) {
                return true;
            }
            this.cfg = new ErosionConfig();
        }
        if (!(cfgFile = new File(ZomboidFileSystem.instance.getCacheDir() + File.separator + "erosion.ini")).exists() && !Core.getInstance().isNoSave() && (cfgFileSrc = ZomboidFileSystem.instance.getMediaFile("data" + File.separator + "erosion.ini")).exists()) {
            try {
                DebugLog.DetailedInfo.trace("erosion: copying " + cfgFileSrc.getAbsolutePath() + " to " + cfgFile.getAbsolutePath());
                Files.copy(cfgFileSrc.toPath(), cfgFile.toPath(), new CopyOption[0]);
            }
            catch (Exception ex) {
                ex.printStackTrace();
            }
        }
        if (cfgFile.exists()) {
            DebugLog.DetailedInfo.trace("erosion: reading " + cfgFile.getAbsolutePath());
            if (!this.cfg.readFile(cfgFile.getAbsolutePath())) {
                this.cfg = new ErosionConfig();
            }
        }
        int erosionSpeed = SandboxOptions.instance.getErosionSpeed();
        switch (erosionSpeed) {
            case 1: {
                this.cfg.time.tickunit /= 5;
                break;
            }
            case 2: {
                this.cfg.time.tickunit /= 2;
                break;
            }
            case 3: {
                break;
            }
            case 4: {
                this.cfg.time.tickunit *= 2;
                break;
            }
            case 5: {
                this.cfg.time.tickunit *= 5;
            }
        }
        float daysTill100Percent = (float)(this.cfg.time.tickunit * 100) / 144.0f;
        float daysSinceApo = (SandboxOptions.instance.timeSinceApo.getValue() - 1) * 30;
        this.cfg.time.eticks = (int)Math.floor(Math.min(1.0f, daysSinceApo / daysTill100Percent) * 100.0f);
        int erosionDays = SandboxOptions.instance.erosionDays.getValue();
        if (erosionDays > 0) {
            this.cfg.time.tickunit = 144;
            this.cfg.time.eticks = (int)Math.floor(Math.min(1.0f, daysSinceApo / (float)erosionDays) * 100.0f);
        }
        return true;
    }

    public void start() {
        if (!this.initConfig()) {
            return;
        }
        this.gameSaveWorld = Core.gameSaveWorld;
        this.tickUnit = this.cfg.time.tickunit;
        this.ticks = this.cfg.time.ticks;
        this.eTicks = this.cfg.time.eticks;
        this.month = GameTime.getInstance().getMonth();
        this.year = GameTime.getInstance().getYear();
        this.day = GameTime.getInstance().getDay();
        this.debug = !GameServer.server && this.cfg.debug.enabled;
        this.cfg.consolePrint();
        this.noiseMain = new Noise2D();
        this.noiseMain.addLayer(this.cfg.seeds.seedMain0, 0.5f, 3.0f);
        this.noiseMain.addLayer(this.cfg.seeds.seedMain1, 2.0f, 5.0f);
        this.noiseMain.addLayer(this.cfg.seeds.seedMain2, 5.0f, 8.0f);
        this.noiseMoisture = new Noise2D();
        this.noiseMoisture.addLayer(this.cfg.seeds.seedMoisture0, 2.0f, 3.0f);
        this.noiseMoisture.addLayer(this.cfg.seeds.seedMoisture1, 1.6f, 5.0f);
        this.noiseMoisture.addLayer(this.cfg.seeds.seedMoisture2, 0.6f, 8.0f);
        this.noiseMinerals = new Noise2D();
        this.noiseMinerals.addLayer(this.cfg.seeds.seedMinerals0, 2.0f, 3.0f);
        this.noiseMinerals.addLayer(this.cfg.seeds.seedMinerals1, 1.6f, 5.0f);
        this.noiseMinerals.addLayer(this.cfg.seeds.seedMinerals2, 0.6f, 8.0f);
        this.noiseKudzu = new Noise2D();
        this.noiseKudzu.addLayer(this.cfg.seeds.seedKudzu0, 6.0f, 3.0f);
        this.noiseKudzu.addLayer(this.cfg.seeds.seedKudzu1, 3.0f, 5.0f);
        this.noiseKudzu.addLayer(this.cfg.seeds.seedKudzu2, 0.5f, 8.0f);
        this.season = new ErosionSeason();
        ErosionConfig.Season sc = this.cfg.season;
        int tempMin = sc.tempMin;
        int tempMax = sc.tempMax;
        if (SandboxOptions.instance.getTemperatureModifier() == 1) {
            tempMin -= 10;
            tempMax -= 10;
        } else if (SandboxOptions.instance.getTemperatureModifier() == 2) {
            tempMin -= 5;
            tempMax -= 5;
        } else if (SandboxOptions.instance.getTemperatureModifier() == 4) {
            tempMin = (int)((double)tempMin + 7.5);
            tempMax += 4;
        } else if (SandboxOptions.instance.getTemperatureModifier() == 5) {
            tempMin += 15;
            tempMax += 8;
        }
        this.season.init(sc.lat, tempMax, tempMin, sc.tempDiff, sc.seasonLag, sc.noon, sc.seedA, sc.seedB, sc.seedC);
        this.season.setRain(sc.jan, sc.feb, sc.mar, sc.apr, sc.may, sc.jun, sc.jul, sc.aug, sc.sep, sc.oct, sc.nov, sc.dec);
        this.season.setDay(this.day, this.month, this.year);
        LuaEventManager.triggerEvent("OnInitSeasons", this.season);
        this.iceQueen = new ErosionIceQueen(this.sprMngr);
        this.world = new ErosionWorld();
        if (!this.world.init()) {
            return;
        }
        this.snowCheck();
    }

    private void loadChunk(IsoChunk chunk) {
        ErosionData.Chunk chunkModData = chunk.getErosionData();
        if (!chunkModData.init) {
            this.initChunk(chunk, chunkModData);
        }
        chunkModData.eTickStamp = this.eTicks;
        chunkModData.epoch = this.epoch;
    }

    public void DebugUpdateMapNow() {
        this.updateMapNow();
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    private void updateMapNow() {
        for (int playerIndex = 0; playerIndex < IsoPlayer.numPlayers; ++playerIndex) {
            IsoChunkMap cm = IsoWorld.instance.currentCell.getChunkMap(playerIndex);
            if (cm.ignore) continue;
            IsoChunkMap.bSettingChunk.lock();
            try {
                for (int y = 0; y < IsoChunkMap.chunkGridWidth; ++y) {
                    for (int x = 0; x < IsoChunkMap.chunkGridWidth; ++x) {
                        IsoChunk chunk = cm.getChunk(x, y);
                        if (chunk == null) continue;
                        ErosionData.Chunk chunkData = chunk.getErosionData();
                        if (chunkData.eTickStamp == this.eTicks && chunkData.epoch == this.epoch) continue;
                        for (int yy = 0; yy < 8; ++yy) {
                            for (int xx = 0; xx < 8; ++xx) {
                                IsoGridSquare sq = chunk.getGridSquare(xx, yy, 0);
                                if (sq == null) continue;
                                this.loadGridsquare(sq);
                            }
                        }
                        chunkData.eTickStamp = this.eTicks;
                        chunkData.epoch = this.epoch;
                    }
                }
                continue;
            }
            finally {
                IsoChunkMap.bSettingChunk.unlock();
            }
        }
    }

    public static void LoadGridsquare(IsoGridSquare square) {
        instance.loadGridsquare(square);
    }

    public static void ChunkLoaded(IsoChunk isoChunk) {
        instance.loadChunk(isoChunk);
    }

    public static void EveryTenMinutes() {
        instance.mainTimer();
    }

    public static void Reset() {
        instance = null;
    }

    static {
        soilTable = new int[][]{{1, 1, 1, 1, 1, 4, 4, 4, 4, 4}, {1, 1, 1, 1, 2, 5, 4, 4, 4, 4}, {1, 1, 1, 2, 2, 5, 5, 4, 4, 4}, {1, 1, 2, 2, 3, 6, 5, 5, 4, 4}, {1, 2, 2, 3, 3, 6, 6, 5, 5, 4}, {7, 8, 8, 9, 9, 12, 12, 11, 11, 10}, {7, 7, 8, 8, 9, 12, 11, 11, 10, 10}, {7, 7, 7, 8, 8, 11, 11, 10, 10, 10}, {7, 7, 7, 7, 8, 11, 10, 10, 10, 10}, {7, 7, 7, 7, 7, 10, 10, 10, 10, 10}};
    }
}

