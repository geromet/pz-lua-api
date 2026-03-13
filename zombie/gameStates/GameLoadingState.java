/*
 * Decompiled with CFR 0.152.
 */
package zombie.gameStates;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import zombie.AmbientStreamManager;
import zombie.ChunkMapFilenames;
import zombie.GameTime;
import zombie.GameWindow;
import zombie.Lua.LuaEventManager;
import zombie.Lua.LuaManager;
import zombie.SandboxOptions;
import zombie.SoundManager;
import zombie.UsedFromLua;
import zombie.ZomboidFileSystem;
import zombie.characters.IsoPlayer;
import zombie.chat.ChatManager;
import zombie.chat.ChatUtility;
import zombie.core.Core;
import zombie.core.PerformanceSettings;
import zombie.core.SpriteRenderer;
import zombie.core.ThreadGroups;
import zombie.core.Translator;
import zombie.core.logger.ExceptionLogger;
import zombie.core.math.PZMath;
import zombie.core.physics.Bullet;
import zombie.core.raknet.UdpConnection;
import zombie.core.random.Rand;
import zombie.core.skinnedmodel.ModelManager;
import zombie.core.skinnedmodel.population.OutfitManager;
import zombie.core.skinnedmodel.runtime.RuntimeAnimationScript;
import zombie.core.textures.AnimatedTexture;
import zombie.core.textures.AnimatedTextures;
import zombie.core.textures.Texture;
import zombie.core.znet.ServerBrowser;
import zombie.core.znet.SteamUtils;
import zombie.debug.DebugLog;
import zombie.debug.DebugOptions;
import zombie.gameStates.GameState;
import zombie.gameStates.GameStateMachine;
import zombie.gameStates.IngameState;
import zombie.globalObjects.CGlobalObjects;
import zombie.globalObjects.SGlobalObjects;
import zombie.input.GameKeyboard;
import zombie.input.JoypadManager;
import zombie.input.Mouse;
import zombie.iso.IsoCamera;
import zombie.iso.IsoChunkMap;
import zombie.iso.IsoObjectPicker;
import zombie.iso.IsoPuddles;
import zombie.iso.IsoWater;
import zombie.iso.IsoWorld;
import zombie.iso.LosUtil;
import zombie.iso.WorldConverter;
import zombie.iso.WorldStreamer;
import zombie.iso.areas.SafeHouse;
import zombie.iso.fboRenderChunk.FBORenderChunkManager;
import zombie.iso.sprite.SkyBox;
import zombie.iso.weather.ClimateManager;
import zombie.modding.ActiveMods;
import zombie.modding.ActiveModsFile;
import zombie.network.GameClient;
import zombie.network.GameServer;
import zombie.network.NetworkAIParams;
import zombie.network.ServerOptions;
import zombie.randomizedWorld.randomizedBuilding.TableStories.RBTableStoryBase;
import zombie.savefile.SavefileNaming;
import zombie.scripting.ScriptManager;
import zombie.ui.ScreenFader;
import zombie.ui.TextManager;
import zombie.ui.TutorialManager;
import zombie.ui.UIFont;
import zombie.ui.UIManager;
import zombie.util.StringUtils;
import zombie.vehicles.BaseVehicle;
import zombie.world.WorldDictionary;
import zombie.worldMap.WorldMapImages;
import zombie.worldMap.WorldMapVisited;

@UsedFromLua
public final class GameLoadingState
extends GameState {
    public static final int QUICK_TIP_MAX_TIMER = 720;
    public static Thread loader;
    private static boolean newGame;
    private static long startTime;
    public static boolean worldVersionError;
    private static boolean unexpectedError;
    public static String gameLoadingString;
    public static boolean playerWrongIP;
    private static boolean showedUI;
    private static boolean showedClickToSkip;
    public static boolean mapDownloadFailed;
    private static boolean playerCreated;
    private static boolean done;
    public static boolean convertingWorld;
    public static int convertingFileCount;
    public static int convertingFileMax;
    private volatile boolean waitForAssetLoadingToFinish1;
    private volatile boolean waitForAssetLoadingToFinish2;
    private final Object assetLock1 = "Asset Lock 1";
    private final Object assetLock2 = "Asset Lock 2";
    private float time;
    private boolean forceDone;
    private String text;
    private float width;
    private static final ScreenFader screenFader;
    private AnimatedTexture animatedTexture;
    private long progressFadeStartMs;
    private int stage;
    private final float totalTime = 33.0f;
    private float loadingDotTick;
    private String loadingDot = "";
    private float clickToSkipAlpha = 1.0f;
    private boolean clickToSkipFadeIn;
    private float quickTipsTimer = 720.0f;
    private String quickTipsText;
    private List<String> quickTipsList;
    private List<String> quickTipsListJoke;
    private static final int BOTTOM_SCREEN = 40;

    @Override
    public void enter() {
        File file;
        this.loadQuickTipList();
        try {
            WorldMapImages.Reset();
            WorldMapVisited.Reset();
            LuaManager.releaseAllVideoTextures();
        }
        catch (Exception ex) {
            ExceptionLogger.logException(ex);
        }
        if (GameClient.client) {
            this.text = Translator.getText("UI_DirectConnectionPortWarning", ServerOptions.getInstance().udpPort.getValue());
            this.width = TextManager.instance.MeasureStringX(UIFont.NewMedium, this.text) + 8;
        }
        GameWindow.loadedAsClient = GameClient.client;
        GameWindow.okToSaveOnExit = false;
        showedUI = false;
        ChunkMapFilenames.instance.clear();
        DebugLog.DetailedInfo.trace("Savefile name is \"" + Core.gameSaveWorld + "\"");
        gameLoadingString = "";
        try {
            LuaManager.LoadDirBase("server");
            LuaManager.finishChecksum();
        }
        catch (Exception e) {
            ExceptionLogger.logException(e);
        }
        ScriptManager.instance.LoadedAfterLua();
        Core.getInstance().initFBOs();
        Core.getInstance().initShaders();
        SkyBox.getInstance();
        IsoPuddles.getInstance();
        IsoWater.getInstance();
        GameWindow.serverDisconnected = false;
        if (GameClient.client && !GameClient.instance.connected) {
            GameClient.instance.init();
            Core.getInstance().setGameMode("Multiplayer");
            while (GameClient.instance.id == -1) {
                try {
                    LuaEventManager.RunQueuedEvents();
                    Thread.sleep(10L);
                }
                catch (InterruptedException e) {
                    e.printStackTrace();
                }
                GameClient.instance.update();
            }
            Core.gameSaveWorld = "clienttest" + GameClient.instance.id;
            LuaManager.GlobalObject.deleteSave("clienttest" + GameClient.instance.id);
            LuaManager.GlobalObject.createWorld("clienttest" + GameClient.instance.id);
        }
        if (Core.gameSaveWorld.isEmpty()) {
            DebugLog.log("No savefile directory was specified.  It's a bug.");
            GameWindow.DoLoadingText("No savefile directory was specified.  The game will now close.  Sorry!");
            try {
                Thread.sleep(4000L);
            }
            catch (Exception e) {
                // empty catch block
            }
            System.exit(-1);
        }
        if (!(file = new File(ZomboidFileSystem.instance.getCurrentSaveDir())).exists() && !Core.getInstance().isNoSave()) {
            DebugLog.log("The savefile directory doesn't exist.  It's a bug.");
            GameWindow.DoLoadingText("The savefile directory doesn't exist.  The game will now close.  Sorry!");
            try {
                Thread.sleep(4000L);
            }
            catch (Exception exception) {
                // empty catch block
            }
            System.exit(-1);
        }
        if (!Core.getInstance().isNoSave()) {
            SavefileNaming.ensureSubdirectoriesExist(ZomboidFileSystem.instance.getCurrentSaveDir());
        }
        try {
            if (!(GameClient.client || GameServer.server || Core.tutorial || Core.isLastStand() || "Multiplayer".equals(Core.gameMode))) {
                FileWriter fw = new FileWriter(new File(ZomboidFileSystem.instance.getCacheDir() + File.separator + "latestSave.ini"));
                fw.write(IsoWorld.instance.getWorld() + "\r\n");
                fw.write(Core.getInstance().getGameMode() + "\r\n");
                fw.flush();
                fw.close();
            }
        }
        catch (IOException ex) {
            ExceptionLogger.logException(ex);
        }
        done = false;
        this.forceDone = false;
        IsoChunkMap.CalcChunkWidth();
        Core.setInitialSize();
        LosUtil.init(IsoChunkMap.chunkGridWidth * 8, IsoChunkMap.chunkGridWidth * 8);
        this.time = 0.0f;
        this.stage = 0;
        this.clickToSkipAlpha = 1.0f;
        this.clickToSkipFadeIn = false;
        startTime = System.currentTimeMillis();
        SoundManager.instance.Purge();
        SoundManager.instance.setMusicState("Loading");
        LuaEventManager.triggerEvent("OnPreMapLoad");
        newGame = true;
        worldVersionError = false;
        unexpectedError = false;
        mapDownloadFailed = false;
        playerCreated = false;
        convertingWorld = false;
        convertingFileCount = 0;
        convertingFileMax = -1;
        File inFile = ZomboidFileSystem.instance.getFileInCurrentSave("map_ver.bin");
        if (inFile.exists()) {
            newGame = false;
        }
        if (GameClient.client) {
            newGame = false;
        }
        if (!newGame) {
            this.stage = -1;
            screenFader.startFadeFromBlack();
            this.progressFadeStartMs = 0L;
        }
        WorldDictionary.setIsNewGame(newGame);
        GameKeyboard.noEventsWhileLoading = true;
        ServerBrowser.setSuppressLuaCallbacks(true);
        loader = new Thread(ThreadGroups.Workers, new Runnable(this){
            final /* synthetic */ GameLoadingState this$0;
            {
                GameLoadingState gameLoadingState = this$0;
                Objects.requireNonNull(gameLoadingState);
                this.this$0 = gameLoadingState;
            }

            @Override
            public void run() {
                LuaManager.thread.debugOwnerThread = Thread.currentThread();
                LuaManager.debugthread.debugOwnerThread = Thread.currentThread();
                try {
                    this.runInner();
                }
                catch (Throwable t) {
                    unexpectedError = true;
                    ExceptionLogger.logException(t);
                }
                finally {
                    LuaManager.thread.debugOwnerThread = GameWindow.gameThread;
                    LuaManager.debugthread.debugOwnerThread = GameWindow.gameThread;
                    UIManager.suspend = false;
                }
            }

            /*
             * WARNING - Removed try catching itself - possible behaviour change.
             */
            private void runInner() throws Exception {
                this.this$0.waitForAssetLoadingToFinish1 = true;
                Object object = this.this$0.assetLock1;
                synchronized (object) {
                    while (this.this$0.waitForAssetLoadingToFinish1) {
                        try {
                            this.this$0.assetLock1.wait();
                        }
                        catch (InterruptedException interruptedException) {}
                    }
                }
                boolean success = new File(ZomboidFileSystem.instance.getGameModeCacheDir() + File.separator).mkdir();
                BaseVehicle.LoadAllVehicleTextures();
                if (GameClient.client) {
                    GameClient.instance.GameLoadingRequestData();
                }
                TutorialManager.instance = new TutorialManager();
                GameTime.setInstance(new GameTime());
                ClimateManager.setInstance(new ClimateManager());
                String spawnRegion = IsoWorld.instance.getSpawnRegion();
                IsoWorld.instance = new IsoWorld();
                IsoWorld.instance.setSpawnRegion(spawnRegion);
                DebugOptions.testThreadCrash(0);
                IsoWorld.instance.init();
                if (GameWindow.serverDisconnected) {
                    done = true;
                    return;
                }
                if (playerWrongIP) {
                    return;
                }
                if (worldVersionError) {
                    return;
                }
                DebugLog.General.println("triggerEvent OnGameTimeLoaded");
                LuaEventManager.triggerEvent("OnGameTimeLoaded");
                DebugLog.General.println("GlobalObjects.initSystems() start");
                SGlobalObjects.initSystems();
                CGlobalObjects.initSystems();
                DebugLog.General.println("GlobalObjects.initSystems() end");
                IsoObjectPicker.Instance.Init();
                TutorialManager.instance.init();
                TutorialManager.instance.CreateQuests();
                File inFile = ZomboidFileSystem.instance.getFileInCurrentSave("map_t.bin");
                if (inFile.exists()) {
                    // empty if block
                }
                if (!GameServer.server) {
                    boolean newGame;
                    inFile = ZomboidFileSystem.instance.getFileInCurrentSave("map_ver.bin");
                    boolean bl = newGame = !inFile.exists();
                    if (newGame || IsoWorld.savedWorldVersion != 244) {
                        if (!newGame) {
                            gameLoadingString = "Saving converted world.";
                        }
                        try {
                            DebugLog.General.println("GameWindow.save() start");
                            GameWindow.save(true);
                            DebugLog.General.println("GameWindow.save() end");
                        }
                        catch (Throwable t) {
                            ExceptionLogger.logException(t);
                        }
                    }
                }
                ChatUtility.InitAllowedChatIcons();
                ChatManager.getInstance().init(true, IsoPlayer.getInstance());
                Bullet.startLoadingPhysicsMeshes();
                Texture.getSharedTexture("media/textures/NewShadow.png");
                Texture.getSharedTexture("media/wallcutaways.png", 3);
                DebugLog.General.println("bWaitForAssetLoadingToFinish2 start");
                this.this$0.waitForAssetLoadingToFinish2 = true;
                Object object2 = this.this$0.assetLock2;
                synchronized (object2) {
                    while (this.this$0.waitForAssetLoadingToFinish2) {
                        try {
                            this.this$0.assetLock2.wait();
                        }
                        catch (InterruptedException interruptedException) {}
                    }
                }
                DebugLog.General.println("bWaitForAssetLoadingToFinish2 end");
                if (PerformanceSettings.fboRenderChunk) {
                    DebugLog.General.println("FBORenderChunkManager.gameLoaded() start");
                    FBORenderChunkManager.instance.gameLoaded();
                    DebugLog.General.println("FBORenderChunkManager.gameLoaded() end");
                }
                DebugLog.General.println("Bullet.initPhysicsMeshes() start");
                Bullet.initPhysicsMeshes();
                DebugLog.General.println("Bullet.initPhysicsMeshes() end");
                RBTableStoryBase.initStories();
                playerCreated = true;
                gameLoadingString = "";
                GameLoadingState.SendDone();
            }
        });
        UIManager.suspend = true;
        loader.setName("GameLoadingThread");
        loader.setUncaughtExceptionHandler(GameWindow::uncaughtException);
        loader.start();
    }

    public static void SendDone() {
        DebugLog.log("game loading took " + (System.currentTimeMillis() - startTime + 999L) / 1000L + " seconds");
        if (!GameClient.client) {
            done = true;
            GameKeyboard.noEventsWhileLoading = false;
            return;
        }
        GameClient.instance.sendLoginQueueDone(System.currentTimeMillis() - startTime);
    }

    public static void Done() {
        done = true;
        GameKeyboard.noEventsWhileLoading = false;
    }

    @Override
    public GameState redirectState() {
        return new IngameState();
    }

    @Override
    public void exit() {
        SafeHouse safe;
        boolean useUIFBO = UIManager.useUiFbo;
        UIManager.useUiFbo = false;
        screenFader.startFadeToBlack();
        while (screenFader.isFading()) {
            screenFader.preRender();
            screenFader.postRender();
            if (!screenFader.isFading()) continue;
            try {
                Thread.sleep(33L);
            }
            catch (Exception exception) {}
        }
        UIManager.useUiFbo = useUIFBO;
        ServerBrowser.setSuppressLuaCallbacks(false);
        if (GameClient.client) {
            NetworkAIParams.Init();
        }
        UIManager.init();
        LuaEventManager.triggerEvent("OnCreatePlayer", 0, IsoPlayer.players[0]);
        loader = null;
        done = false;
        this.stage = 0;
        IsoCamera.SetCharacterToFollow(IsoPlayer.getInstance());
        if (GameClient.client && !ServerOptions.instance.safehouseAllowTrepass.getValue() && (safe = SafeHouse.isSafeHouse(IsoPlayer.getInstance().getCurrentSquare(), GameClient.username, true)) != null) {
            IsoPlayer.getInstance().setX((float)safe.getX() - 1.0f);
            IsoPlayer.getInstance().setY((float)safe.getY() - 1.0f);
        }
        SoundManager.instance.stopMusic("");
        AmbientStreamManager.instance.init();
        if (IsoPlayer.getInstance() != null && IsoPlayer.getInstance().isAsleep()) {
            UIManager.setFadeBeforeUI(IsoPlayer.getInstance().getPlayerNum(), true);
            UIManager.FadeOut(IsoPlayer.getInstance().getPlayerNum(), 2.0);
            UIManager.setFadeTime(IsoPlayer.getInstance().getPlayerNum(), 0.0);
            UIManager.getSpeedControls().SetCurrentGameSpeed(3);
        }
        if (!GameClient.client) {
            ActiveMods activeMods = ActiveMods.getById("currentGame");
            activeMods.checkMissingMods();
            activeMods.checkMissingMaps();
            ActiveMods.setLoadedMods(activeMods);
            String path = ZomboidFileSystem.instance.getFileNameInCurrentSave("mods.txt");
            ActiveModsFile activeModsFile = new ActiveModsFile();
            activeModsFile.write(path, activeMods);
        }
        DebugLog.log("Game Mode: " + Core.gameMode);
        DebugLog.log("Sandbox Options:");
        SandboxOptions options = LuaManager.GlobalObject.getSandboxOptions();
        for (int i = 0; i < options.getNumOptions(); ++i) {
            SandboxOptions.SandboxOption option = options.getOptionByIndex(i);
            DebugLog.log(option.getShortName() + " " + option.asConfigOption().getValueAsString());
        }
        GameWindow.okToSaveOnExit = true;
    }

    @Override
    public void render() {
        float del;
        float textend;
        float textfullend;
        float textfull;
        float textstart;
        float pos;
        float fontHeightSmall = TextManager.instance.getFontHeight(UIFont.NewSmall);
        float fontHeightMedium = TextManager.instance.getFontHeight(UIFont.NewMedium);
        this.loadingDotTick += GameTime.getInstance().getMultiplier();
        if (this.loadingDotTick > 20.0f) {
            this.loadingDot = ".";
        }
        if (this.loadingDotTick > 40.0f) {
            this.loadingDot = "..";
        }
        if (this.loadingDotTick > 60.0f) {
            this.loadingDot = "...";
        }
        if (this.loadingDotTick > 80.0f) {
            this.loadingDot = "";
            this.loadingDotTick = 0.0f;
        }
        this.time += GameTime.instance.getTimeDelta();
        float alpha1 = 0.0f;
        float alpha2 = 0.0f;
        float alpha3 = 0.0f;
        if (this.stage == 0) {
            pos = this.time;
            textstart = 0.0f;
            textfull = 1.0f;
            textfullend = 5.0f;
            textend = 7.0f;
            del = 0.0f;
            if (pos > 0.0f && pos < 1.0f) {
                del = (pos - 0.0f) / 1.0f;
            }
            if (pos >= 1.0f && pos <= 5.0f) {
                del = 1.0f;
            }
            if (pos > 5.0f && pos < 7.0f) {
                del = 1.0f - (pos - 5.0f) / 2.0f;
            }
            if (pos >= 7.0f) {
                ++this.stage;
            }
            alpha1 = del;
        }
        if (this.stage == 1) {
            pos = this.time;
            textstart = 7.0f;
            textfull = 8.0f;
            textfullend = 13.0f;
            textend = 15.0f;
            del = 0.0f;
            if (pos > 7.0f && pos < 8.0f) {
                del = (pos - 7.0f) / 1.0f;
            }
            if (pos >= 8.0f && pos <= 13.0f) {
                del = 1.0f;
            }
            if (pos > 13.0f && pos < 15.0f) {
                del = 1.0f - (pos - 13.0f) / 2.0f;
            }
            if (pos >= 15.0f) {
                ++this.stage;
            }
            alpha2 = del;
        }
        if (this.stage == 2) {
            pos = this.time;
            textstart = 15.0f;
            textfull = 16.0f;
            textfullend = 31.0f;
            textend = 33.0f;
            del = 0.0f;
            if (pos > 15.0f && pos < 16.0f) {
                del = (pos - 15.0f) / 1.0f;
            }
            if (pos >= 16.0f && pos <= 31.0f) {
                del = 1.0f;
            }
            if (pos > 31.0f && pos < 33.0f) {
                del = 1.0f - (pos - 31.0f) / 2.0f;
            }
            if (pos >= 33.0f) {
                ++this.stage;
            }
            alpha3 = del;
        }
        Core.getInstance().StartFrame();
        Core.getInstance().EndFrame();
        boolean useUIFBO = UIManager.useUiFbo;
        UIManager.useUiFbo = false;
        Core.getInstance().StartFrameUI();
        SpriteRenderer.instance.renderi(null, 0, 0, Core.getInstance().getScreenWidth(), Core.getInstance().getScreenHeight(), 0.0f, 0.0f, 0.0f, 1.0f, null);
        if (this.stage == -1) {
            this.renderProgressIndicator();
            screenFader.update();
            screenFader.render();
        }
        if (mapDownloadFailed) {
            int cx = Core.getInstance().getScreenWidth() / 2;
            int cy = Core.getInstance().getScreenHeight() / 2;
            int mediumHgt = TextManager.instance.getFontFromEnum(UIFont.Medium).getLineHeight();
            int top = cy - mediumHgt / 2;
            String reason = Translator.getText("UI_GameLoad_MapDownloadFailed");
            TextManager.instance.DrawStringCentre(UIFont.Medium, cx, top, reason, 0.8, 0.1, 0.1, 1.0);
            UIManager.render();
            Core.getInstance().EndFrameUI();
            return;
        }
        if (unexpectedError) {
            int mediumHgt = TextManager.instance.getFontFromEnum(UIFont.Medium).getLineHeight();
            int smallHgt = TextManager.instance.getFontFromEnum(UIFont.Small).getLineHeight();
            int pad1 = 8;
            int pad2 = 2;
            int dy = mediumHgt + 8 + smallHgt + 2 + smallHgt;
            int cx = Core.getInstance().getScreenWidth() / 2;
            int cy = Core.getInstance().getScreenHeight() / 2;
            int top = cy - dy / 2;
            TextManager.instance.DrawStringCentre(UIFont.Medium, cx, top, Translator.getText("UI_GameLoad_UnexpectedError1"), 0.8, 0.1, 0.1, 1.0);
            TextManager.instance.DrawStringCentre(UIFont.Small, cx, top + mediumHgt + 8, Translator.getText("UI_GameLoad_UnexpectedError2"), 1.0, 1.0, 1.0, 1.0);
            String consoleDotTxt = ZomboidFileSystem.instance.getCacheDir() + File.separator + "console.txt";
            TextManager.instance.DrawStringCentre(UIFont.Small, cx, top + mediumHgt + 8 + smallHgt + 2, consoleDotTxt, 1.0, 1.0, 1.0, 1.0);
            UIManager.render();
            Core.getInstance().EndFrameUI();
            return;
        }
        if (GameWindow.serverDisconnected) {
            int cx = Core.getInstance().getScreenWidth() / 2;
            int cy = Core.getInstance().getScreenHeight() / 2;
            int mediumHgt = TextManager.instance.getFontFromEnum(UIFont.Medium).getLineHeight();
            int pad = 2;
            int top = cy - (mediumHgt + 2 + mediumHgt) / 2;
            String reason = GameWindow.kickReason;
            if (reason == null) {
                reason = Translator.getText("UI_OnConnectFailed_ConnectionLost");
            }
            TextManager.instance.DrawStringCentre(UIFont.Medium, cx, top, reason, 0.8, 0.1, 0.1, 1.0);
            UIManager.render();
            Core.getInstance().EndFrameUI();
            return;
        }
        if (worldVersionError) {
            if (WorldConverter.convertingVersion == 0) {
                TextManager.instance.DrawStringCentre(UIFont.Small, Core.getInstance().getScreenWidth() / 2, Core.getInstance().getScreenHeight() - 100, Translator.getText("UI_CorruptedWorldVersion"), 0.8, 0.1, 0.1, 1.0);
            } else if (WorldConverter.convertingVersion < 1) {
                TextManager.instance.DrawStringCentre(UIFont.Small, Core.getInstance().getScreenWidth() / 2, Core.getInstance().getScreenHeight() - 100, Translator.getText("UI_ConvertWorldFailure"), 0.8, 0.1, 0.1, 1.0);
            }
        } else if (convertingWorld) {
            TextManager.instance.DrawStringCentre(UIFont.Small, Core.getInstance().getScreenWidth() / 2, Core.getInstance().getScreenHeight() - 100, Translator.getText("UI_ConvertWorld"), 0.5, 0.5, 0.5, 1.0);
            if (convertingFileMax != -1) {
                TextManager.instance.DrawStringCentre(UIFont.Small, Core.getInstance().getScreenWidth() / 2, Core.getInstance().getScreenHeight() - 100 + TextManager.instance.getFontFromEnum(UIFont.Small).getLineHeight() + 8, convertingFileCount + " / " + convertingFileMax, 0.5, 0.5, 0.5, 1.0);
            }
        }
        if (playerWrongIP) {
            int cx = Core.getInstance().getScreenWidth() / 2;
            int cy = Core.getInstance().getScreenHeight() / 2;
            int mediumHgt = TextManager.instance.getFontFromEnum(UIFont.Medium).getLineHeight();
            int pad = 2;
            int top = cy - (mediumHgt + 2 + mediumHgt) / 2;
            String str = gameLoadingString;
            if (gameLoadingString == null) {
                str = "";
            }
            TextManager.instance.DrawStringCentre(UIFont.Medium, cx, top, str, 0.8, 0.1, 0.1, 1.0);
            UIManager.render();
            Core.getInstance().EndFrameUI();
            return;
        }
        if (GameClient.client) {
            String str = gameLoadingString;
            if (gameLoadingString == null) {
                str = "";
            }
            TextManager.instance.DrawStringCentre(UIFont.Small, Core.getInstance().getScreenWidth() / 2, (float)(Core.getInstance().getScreenHeight() - 40) - fontHeightSmall - 5.0f, str, 0.5, 0.5, 0.5, 1.0);
            if (GameClient.connection.getConnectionType() == UdpConnection.ConnectionType.Steam) {
                SpriteRenderer.instance.render(null, ((float)Core.getInstance().getScreenWidth() - this.width) / 2.0f, (float)(Core.getInstance().getScreenHeight() - 40) - fontHeightSmall * 2.0f - 5.0f, this.width, 18.0f, 1.0f, 0.4f, 0.35f, 0.8f, null);
                TextManager.instance.DrawStringCentre(UIFont.Medium, Core.getInstance().getScreenWidth() / 2, (float)(Core.getInstance().getScreenHeight() - 40) - fontHeightSmall * 2.0f - 5.0f, this.text, 0.1, 0.1, 0.1, 1.0);
            }
        } else if (!playerCreated && newGame && !Core.isLastStand()) {
            TextManager.instance.DrawStringCentre(UIFont.NewSmall, Core.getInstance().getScreenWidth() / 2, (float)(Core.getInstance().getScreenHeight() - 40) - fontHeightSmall - 5.0f, Translator.getText("UI_Loading").replace(".", ""), 0.5, 0.5, 0.5, 1.0);
            TextManager.instance.DrawString(UIFont.NewSmall, Core.getInstance().getScreenWidth() / 2 + TextManager.instance.MeasureStringX(UIFont.Small, Translator.getText("UI_Loading").replace(".", "")) / 2 + 1, (float)(Core.getInstance().getScreenHeight() - 40) - fontHeightSmall - 5.0f, this.loadingDot, 0.5, 0.5, 0.5, 1.0);
        }
        this.doQuickTips();
        if (this.stage == 0) {
            int x = Core.getInstance().getScreenWidth() / 2;
            int y = Core.getInstance().getScreenHeight() / 2 - TextManager.instance.getFontFromEnum(UIFont.Intro).getLineHeight() / 2;
            TextManager.instance.DrawStringCentre(UIFont.Intro, x, y, Translator.getText("UI_Intro1"), 1.0, 1.0, 1.0, alpha1);
        }
        if (this.stage == 1) {
            int x = Core.getInstance().getScreenWidth() / 2;
            int y = Core.getInstance().getScreenHeight() / 2 - TextManager.instance.getFontFromEnum(UIFont.Intro).getLineHeight() / 2;
            TextManager.instance.DrawStringCentre(UIFont.Intro, x, y, Translator.getText("UI_Intro2"), 1.0, 1.0, 1.0, alpha2);
        }
        if (this.stage == 2) {
            int x = Core.getInstance().getScreenWidth() / 2;
            int y = Core.getInstance().getScreenHeight() / 2 - TextManager.instance.getFontFromEnum(UIFont.Intro).getLineHeight() / 2;
            TextManager.instance.DrawStringCentre(UIFont.Intro, x, y, Translator.getText("UI_Intro3"), 1.0, 1.0, 1.0, alpha3);
        }
        if (Core.getInstance().getDebug()) {
            showedClickToSkip = true;
        }
        if (done && playerCreated && (!newGame || this.time >= 33.0f || Core.isLastStand() || "Tutorial".equals(Core.gameMode))) {
            if (this.clickToSkipFadeIn) {
                this.clickToSkipAlpha += GameTime.getInstance().getThirtyFPSMultiplier() / 30.0f;
                if (this.clickToSkipAlpha > 1.0f) {
                    this.clickToSkipAlpha = 1.0f;
                    this.clickToSkipFadeIn = false;
                }
            } else {
                showedClickToSkip = true;
                this.clickToSkipAlpha -= GameTime.getInstance().getThirtyFPSMultiplier() / 30.0f;
                if (this.clickToSkipAlpha < 0.25f) {
                    this.clickToSkipFadeIn = true;
                }
            }
            int baseline = Core.getInstance().getScreenHeight();
            if (GameWindow.activatedJoyPad == null || JoypadManager.instance.joypadList.isEmpty()) {
                TextManager.instance.DrawStringCentre(UIFont.NewLarge, Core.getInstance().getScreenWidth() / 2, (float)(baseline - 40) - fontHeightSmall - 5.0f, Translator.getText("UI_ClickToSkip"), 1.0, 1.0, 1.0, this.clickToSkipAlpha);
            } else {
                String textureType = Core.getInstance().getOptionControllerButtonStyle() == 1 ? "XBOX" : "PS4";
                Texture tex = Texture.getSharedTexture("media/ui/controller/" + textureType + "_A.png");
                if (tex != null) {
                    int fontHgt = TextManager.instance.getFontFromEnum(UIFont.Small).getLineHeight();
                    SpriteRenderer.instance.renderi(tex, Core.getInstance().getScreenWidth() / 2 - TextManager.instance.MeasureStringX(UIFont.Small, Translator.getText("UI_PressAToStart")) / 2 - 8 - tex.getWidth(), baseline - 60 + fontHgt / 2 - tex.getHeight() / 2, tex.getWidth(), tex.getHeight(), 1.0f, 1.0f, 1.0f, this.clickToSkipAlpha, null);
                }
                TextManager.instance.DrawStringCentre(UIFont.Small, Core.getInstance().getScreenWidth() / 2, (float)(baseline - 40) - fontHeightSmall - 5.0f, Translator.getText("UI_PressAToStart"), 1.0, 1.0, 1.0, this.clickToSkipAlpha);
            }
        }
        ActiveMods.renderUI();
        Core.getInstance().EndFrameUI();
        UIManager.useUiFbo = useUIFBO;
    }

    private void doQuickTips() {
        if (newGame) {
            return;
        }
        if (this.quickTipsTimer > 720.0f) {
            this.quickTipsText = this.getNewQuickTip();
            this.quickTipsTimer = 0.0f;
        }
        this.quickTipsTimer += GameTime.getInstance().getMultiplier();
        if (!StringUtils.isNullOrEmpty(this.quickTipsText)) {
            TextManager.instance.DrawStringCentre(UIFont.NewMedium, (double)Core.getInstance().getScreenWidth() / 2.0, Core.getInstance().getScreenHeight() - 40, this.quickTipsText, 0.5, 0.5, 0.5, 1.0);
        }
    }

    private String getNewQuickTip() {
        if (this.quickTipsList.isEmpty() || this.quickTipsListJoke.isEmpty()) {
            return null;
        }
        String quickTip = this.quickTipsList.get(Rand.Next(this.quickTipsList.size()));
        if (Rand.NextBool(13)) {
            quickTip = this.quickTipsListJoke.get(Rand.Next(this.quickTipsListJoke.size()));
        }
        return quickTip;
    }

    private void loadQuickTipList() {
        this.quickTipsList = new ArrayList<String>();
        this.quickTipsListJoke = new ArrayList<String>();
        for (Map.Entry<String, String> entry : Translator.getUI().entrySet()) {
            String key = entry.getKey();
            if (key.startsWith("UI_quick_tip_joke")) {
                this.quickTipsListJoke.add(entry.getValue());
                continue;
            }
            if (!key.startsWith("UI_quick_tip")) continue;
            this.quickTipsList.add(entry.getValue());
        }
    }

    private void renderProgressIndicator() {
        if (unexpectedError) {
            return;
        }
        this.animatedTexture = convertingWorld ? AnimatedTextures.getTexture("media/ui/Progress/MaleDoor.png") : (SandboxOptions.instance.lore.speed.getValue() == 1 ? AnimatedTextures.getTexture("media/ui/Progress/MaleSprint06.png") : AnimatedTextures.getTexture("media/ui/Progress/MaleWalk2.png"));
        if (this.animatedTexture.isReady()) {
            int width = 196;
            float scale = 196.0f / (float)this.animatedTexture.getWidth();
            int height = (int)((float)this.animatedTexture.getHeight() * scale);
            float alpha = 0.66f;
            if (done && showedClickToSkip) {
                if (this.progressFadeStartMs == 0L) {
                    this.progressFadeStartMs = System.currentTimeMillis();
                }
                long fadeTime = 200L;
                long dt = PZMath.clamp(System.currentTimeMillis() - this.progressFadeStartMs, 0L, 200L);
                if ((alpha *= 1.0f - (float)dt / 200.0f) == 0.0f) {
                    return;
                }
            }
            int textY = Core.getInstance().getScreenHeight() - (convertingWorld ? 100 : 0);
            this.animatedTexture.render(Core.getInstance().getScreenWidth() / 2 - 98, PZMath.min(Core.getInstance().getScreenHeight(), textY) - height - 30 + (convertingWorld ? 32 : 0), 196, height, 1.0f, 1.0f, 1.0f, alpha);
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    @Override
    public GameStateMachine.StateAction update() {
        Object object;
        if (this.waitForAssetLoadingToFinish1 && !OutfitManager.instance.isLoadingClothingItems()) {
            if (Core.debug) {
                OutfitManager.instance.debugOutfits();
            }
            object = this.assetLock1;
            synchronized (object) {
                this.waitForAssetLoadingToFinish1 = false;
                this.assetLock1.notifyAll();
            }
        }
        if (this.waitForAssetLoadingToFinish2 && !ModelManager.instance.isLoadingAnimations() && !GameWindow.fileSystem.hasWork()) {
            object = this.assetLock2;
            synchronized (object) {
                this.waitForAssetLoadingToFinish2 = false;
                this.assetLock2.notifyAll();
                ArrayList<RuntimeAnimationScript> runtimeAnimationScripts = ScriptManager.instance.getAllRuntimeAnimationScripts();
                for (RuntimeAnimationScript runtimeAnimationScript : runtimeAnimationScripts) {
                    runtimeAnimationScript.exec();
                }
            }
        }
        if (unexpectedError || GameWindow.serverDisconnected || playerWrongIP) {
            if (!showedUI) {
                showedUI = true;
                IsoPlayer.setInstance(null);
                IsoPlayer.players[0] = null;
                UIManager.UI.clear();
                LuaManager.thread.debugOwnerThread = GameWindow.gameThread;
                LuaManager.debugthread.debugOwnerThread = GameWindow.gameThread;
                LuaEventManager.Reset();
                LuaManager.call("ISGameLoadingUI_OnGameLoadingUI", "");
                UIManager.suspend = false;
            }
            if (GameKeyboard.isKeyDownRaw(1)) {
                GameClient.instance.Shutdown();
                SteamUtils.shutdown();
                System.exit(1);
            }
            return GameStateMachine.StateAction.Remain;
        }
        if (!done) {
            return GameStateMachine.StateAction.Remain;
        }
        if (WorldStreamer.instance.isBusy()) {
            return GameStateMachine.StateAction.Remain;
        }
        if (ModelManager.instance.isLoadingAnimations()) {
            return GameStateMachine.StateAction.Remain;
        }
        if (!showedClickToSkip) {
            return GameStateMachine.StateAction.Remain;
        }
        if (Mouse.isButtonDown(0)) {
            this.forceDone = true;
        }
        if (GameWindow.activatedJoyPad != null && GameWindow.activatedJoyPad.isAPressed()) {
            this.forceDone = true;
        }
        if (this.forceDone) {
            SoundManager.instance.playUISound("UIClickToStart");
            this.forceDone = false;
            return GameStateMachine.StateAction.Continue;
        }
        return GameStateMachine.StateAction.Remain;
    }

    static {
        newGame = true;
        gameLoadingString = "";
        convertingFileCount = -1;
        convertingFileMax = -1;
        screenFader = new ScreenFader();
    }
}

