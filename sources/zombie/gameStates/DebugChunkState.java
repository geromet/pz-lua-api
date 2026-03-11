/*
 * Decompiled with CFR 0.152.
 */
package zombie.gameStates;

import java.io.File;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Objects;
import java.util.Stack;
import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.lwjgl.opengl.GL11;
import se.krka.kahlua.vm.KahluaTable;
import se.krka.kahlua.vm.KahluaTableIterator;
import zombie.AmbientStreamManager;
import zombie.FliesSound;
import zombie.IndieGL;
import zombie.Lua.Event;
import zombie.Lua.LuaEventManager;
import zombie.Lua.LuaManager;
import zombie.UsedFromLua;
import zombie.VirtualZombieManager;
import zombie.ZomboidFileSystem;
import zombie.ai.astar.Mover;
import zombie.characters.CharacterStat;
import zombie.characters.IsoGameCharacter;
import zombie.characters.IsoPlayer;
import zombie.chat.ChatElement;
import zombie.config.BooleanConfigOption;
import zombie.config.ConfigFile;
import zombie.config.ConfigOption;
import zombie.config.DoubleConfigOption;
import zombie.config.IntegerConfigOption;
import zombie.config.StringConfigOption;
import zombie.core.BoxedStaticValues;
import zombie.core.Core;
import zombie.core.PerformanceSettings;
import zombie.core.SpriteRenderer;
import zombie.core.math.PZMath;
import zombie.core.opengl.GLStateRenderThread;
import zombie.core.opengl.VBORenderer;
import zombie.core.properties.PropertyContainer;
import zombie.core.skinnedmodel.model.VertexBufferObject;
import zombie.core.skinnedmodel.shader.Shader;
import zombie.core.skinnedmodel.shader.ShaderManager;
import zombie.core.textures.Texture;
import zombie.core.textures.TextureDraw;
import zombie.core.utils.BooleanGrid;
import zombie.debug.DebugLog;
import zombie.debug.DebugOptions;
import zombie.debug.LineDrawer;
import zombie.erosion.ErosionData;
import zombie.gameStates.GameState;
import zombie.gameStates.GameStateMachine;
import zombie.gameStates.IngameState;
import zombie.input.GameKeyboard;
import zombie.input.Mouse;
import zombie.inventory.InventoryItem;
import zombie.iso.BuildingDef;
import zombie.iso.IsoCamera;
import zombie.iso.IsoCell;
import zombie.iso.IsoChunk;
import zombie.iso.IsoChunkMap;
import zombie.iso.IsoDepthHelper;
import zombie.iso.IsoDirections;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoLightSource;
import zombie.iso.IsoObject;
import zombie.iso.IsoObjectPicker;
import zombie.iso.IsoRoomLight;
import zombie.iso.IsoUtils;
import zombie.iso.IsoWorld;
import zombie.iso.LosUtil;
import zombie.iso.NearestWalls;
import zombie.iso.ParticlesFire;
import zombie.iso.PlayerCamera;
import zombie.iso.RoomDef;
import zombie.iso.SpriteDetails.IsoFlagType;
import zombie.iso.SpriteDetails.IsoObjectType;
import zombie.iso.areas.IsoBuilding;
import zombie.iso.areas.isoregion.IsoRegions;
import zombie.iso.areas.isoregion.regions.IWorldRegion;
import zombie.iso.areas.isoregion.regions.IsoWorldRegion;
import zombie.iso.fboRenderChunk.FBORenderChunk;
import zombie.iso.fboRenderChunk.FBORenderChunkManager;
import zombie.iso.fboRenderChunk.FBORenderLevels;
import zombie.iso.fboRenderChunk.FBORenderOcclusion;
import zombie.iso.sprite.IsoSprite;
import zombie.iso.zones.VehicleZone;
import zombie.iso.zones.Zone;
import zombie.network.GameClient;
import zombie.pathfind.LiangBarsky;
import zombie.randomizedWorld.randomizedVehicleStory.RandomizedVehicleStoryBase;
import zombie.randomizedWorld.randomizedVehicleStory.VehicleStorySpawner;
import zombie.ui.TextDrawObject;
import zombie.ui.TextManager;
import zombie.ui.UIElementInterface;
import zombie.ui.UIFont;
import zombie.ui.UIManager;
import zombie.vehicles.BaseVehicle;
import zombie.vehicles.ClipperOffset;
import zombie.vehicles.EditVehicleState;

@UsedFromLua
public final class DebugChunkState
extends GameState {
    private static final IsoDirections[] DIRECTIONS = IsoDirections.values();
    public static DebugChunkState instance;
    private EditVehicleState.LuaEnvironment luaEnv;
    private boolean exit;
    private final ArrayList<UIElementInterface> gameUi = new ArrayList();
    private final ArrayList<UIElementInterface> selfUi = new ArrayList();
    private boolean suspendUi;
    private final ArrayList<Event> eventList = new ArrayList();
    private final HashMap<String, Event> eventMap = new HashMap();
    private KahluaTable table;
    private int playerIndex;
    public int z;
    private int gridX = -1;
    private int gridY = -1;
    public float gridXf;
    public float gridYf;
    private static final UIFont FONT;
    private String vehicleStoryName = "Basic Car Crash";
    private static boolean keyQpressed;
    private final GeometryDrawer[] geometryDrawers = new GeometryDrawer[3];
    private InventoryItem inventoryItem1;
    private static final ClipperOffset m_clipperOffset;
    private static ByteBuffer clipperBuffer;
    private static final int VERSION = 1;
    private final ArrayList<ConfigOption> options = new ArrayList();
    private final ArrayList<ConfigOption> optionsHidden = new ArrayList();
    private final BooleanDebugOption buildingRect = new BooleanDebugOption(this, "BuildingRect", true);
    private final BooleanDebugOption chunkGrid = new BooleanDebugOption(this, "ChunkGrid", true);
    private final BooleanDebugOption chunkColorTexture = new BooleanDebugOption(this, "ChunkColorTexture", true);
    private final BooleanDebugOption chunkDepthTexture = new BooleanDebugOption(this, "ChunkDepthTexture", false);
    private final BooleanDebugOption closestRoomSquare = new BooleanDebugOption(this, "ClosestRoomSquare", true);
    private final BooleanDebugOption depthValues = new BooleanDebugOption(this, "DepthValues", false);
    private final BooleanDebugOption emptySquares = new BooleanDebugOption(this, "EmptySquares", true);
    private final BooleanDebugOption flyBuzzEmitters = new BooleanDebugOption(this, "FlyBuzzEmitters", true);
    private final BooleanDebugOption lightSquares = new BooleanDebugOption(this, "LightSquares", true);
    private final BooleanDebugOption lineClearCollide = new BooleanDebugOption(this, "LineClearCollide", true);
    private final BooleanDebugOption nearestWalls = new BooleanDebugOption(this, "NearestWalls", false);
    private final BooleanDebugOption nearestExteriorWalls = new BooleanDebugOption(this, "NearestExteriorWalls", false);
    private final BooleanDebugOption objectAtCursor = new BooleanDebugOption(this, "ObjectAtCursor", false);
    private final StringDebugOption objectAtCursorId = new StringDebugOption(this, "ObjectAtCursor.id", "cylinder");
    private final IntegerDebugOption objectAtCursorLevels = new IntegerDebugOption(this, "ObjectAtCursor.levels", 1, 10, 1);
    private final DoubleDebugOption objectAtCursorScale = new DoubleDebugOption(this, "ObjectAtCursor.scale", 1.0, 10.0, 1.0);
    private final DoubleDebugOption objectAtCursorWidth = new DoubleDebugOption(this, "ObjectAtCursor.width", 0.5, 20.0, 1.0);
    private final BooleanDebugOption objectPicker = new BooleanDebugOption(this, "ObjectPicker", true);
    private final BooleanDebugOption occludedSquares = new BooleanDebugOption(this, "OccludedSquares", false);
    private final BooleanDebugOption roofHideBuilding = new BooleanDebugOption(this, "RoofHideBuilding", false);
    private final BooleanDebugOption roomLightRects = new BooleanDebugOption(this, "RoomLightRects", true);
    private final BooleanDebugOption vehicleStory = new BooleanDebugOption(this, "VehicleStory", true);
    private final BooleanDebugOption randomSquareInZone = new BooleanDebugOption(this, "RandomSquareInZone", true);
    private final BooleanDebugOption zoneRect = new BooleanDebugOption(this, "ZoneRect", true);

    public DebugChunkState() {
        instance = this;
    }

    @Override
    public void enter() {
        instance = this;
        this.load();
        if (this.luaEnv == null) {
            this.luaEnv = new EditVehicleState.LuaEnvironment(LuaManager.platform, LuaManager.converterManager, LuaManager.env);
        }
        this.saveGameUI();
        if (this.selfUi.isEmpty()) {
            IsoPlayer player = IsoPlayer.players[this.playerIndex];
            this.z = player == null ? 0 : PZMath.fastfloor(player.getZ());
            this.luaEnv.caller.pcall(this.luaEnv.thread, this.luaEnv.env.rawget("DebugChunkState_InitUI"), (Object)this);
            if (this.table != null && this.table.getMetatable() != null) {
                this.table.getMetatable().rawset("_LUA_RELOADED_CHECK", (Object)Boolean.FALSE);
            }
        } else {
            UIManager.UI.addAll(this.selfUi);
            this.luaEnv.caller.pcall(this.luaEnv.thread, this.table.rawget("showUI"), (Object)this.table);
        }
        this.exit = false;
    }

    @Override
    public void yield() {
        this.restoreGameUI();
    }

    @Override
    public void reenter() {
        this.saveGameUI();
    }

    @Override
    public void exit() {
        this.save();
        this.restoreGameUI();
        for (int i = 0; i < IsoCamera.cameras.length; ++i) {
            IsoCamera.cameras[i].deferedY = 0.0f;
            IsoCamera.cameras[i].deferedX = 0.0f;
        }
    }

    @Override
    public void render() {
        int pn;
        IsoPlayer.setInstance(IsoPlayer.players[this.playerIndex]);
        IsoCamera.setCameraCharacter(IsoPlayer.players[this.playerIndex]);
        boolean clear = true;
        for (pn = 0; pn < IsoPlayer.numPlayers; ++pn) {
            if (pn == this.playerIndex || IsoPlayer.players[pn] == null) continue;
            Core.getInstance().StartFrame(pn, clear);
            Core.getInstance().EndFrame(pn);
            clear = false;
        }
        Core.getInstance().StartFrame(this.playerIndex, clear);
        this.renderScene();
        Core.getInstance().EndFrame(this.playerIndex);
        Core.getInstance().RenderOffScreenBuffer();
        for (pn = 0; pn < IsoPlayer.numPlayers; ++pn) {
            TextDrawObject.NoRender(pn);
            ChatElement.NoRender(pn);
        }
        if (Core.getInstance().StartFrameUI()) {
            this.renderUI();
        }
        Core.getInstance().EndFrameUI();
    }

    @Override
    public GameStateMachine.StateAction update() {
        if (this.exit || GameKeyboard.isKeyPressed(60)) {
            return GameStateMachine.StateAction.Continue;
        }
        return this.updateScene();
    }

    public static DebugChunkState checkInstance() {
        instance = null;
        if (instance != null) {
            if (DebugChunkState.instance.table == null || DebugChunkState.instance.table.getMetatable() == null) {
                instance = null;
            } else if (DebugChunkState.instance.table.getMetatable().rawget("_LUA_RELOADED_CHECK") == null) {
                instance = null;
            }
        }
        if (instance == null) {
            return new DebugChunkState();
        }
        return instance;
    }

    public void renderScene() {
        Object object;
        IsoGridSquare sq;
        int i;
        IsoCamera.frameState.set(this.playerIndex);
        IsoCamera.frameState.paused = true;
        IsoGameCharacter isoGameCharacter = IsoCamera.getCameraCharacter();
        SpriteRenderer.instance.doCoreIntParam(0, isoGameCharacter.getX());
        SpriteRenderer.instance.doCoreIntParam(1, isoGameCharacter.getY());
        SpriteRenderer.instance.doCoreIntParam(2, IsoCamera.frameState.camCharacterZ);
        IsoSprite.globalOffsetX = -1.0f;
        IsoWorld.instance.currentCell.render();
        this.drawTestModels();
        IndieGL.StartShader(0, this.playerIndex);
        IndieGL.disableDepthTest();
        if (this.chunkGrid.getValue()) {
            this.drawGrid();
        }
        this.drawCursor();
        if (this.lightSquares.getValue()) {
            Stack<IsoLightSource> lampposts = IsoWorld.instance.getCell().getLamppostPositions();
            for (i = 0; i < lampposts.size(); ++i) {
                IsoLightSource l = (IsoLightSource)lampposts.get(i);
                if (l.z != this.z) continue;
                this.paintSquare(l.x, l.y, l.z, 1.0f, 1.0f, 0.0f, 0.5f);
            }
        }
        if (this.zoneRect.getValue()) {
            this.drawZones();
        }
        if (this.buildingRect.getValue() && (sq = IsoWorld.instance.getCell().getGridSquare(this.gridX, this.gridY, this.z)) != null && sq.getBuilding() != null) {
            BuildingDef def = sq.getBuilding().getDef();
            this.DrawIsoLine(def.getX(), def.getY(), def.getX2(), def.getY(), 1.0f, 1.0f, 1.0f, 1.0f, 2);
            this.DrawIsoLine(def.getX2(), def.getY(), def.getX2(), def.getY2(), 1.0f, 1.0f, 1.0f, 1.0f, 2);
            this.DrawIsoLine(def.getX2(), def.getY2(), def.getX(), def.getY2(), 1.0f, 1.0f, 1.0f, 1.0f, 2);
            this.DrawIsoLine(def.getX(), def.getY2(), def.getX(), def.getY(), 1.0f, 1.0f, 1.0f, 1.0f, 2);
        }
        if (this.buildingRect.getValue() && this.z >= 0) {
            IsoWorldRegion worldRegion;
            BuildingDef buildingDef;
            IWorldRegion iWorldRegion;
            sq = IsoWorld.instance.getCell().getGridSquare(this.gridX, this.gridY, this.z);
            RoomDef roomDef2 = IsoWorld.instance.getMetaGrid().getRoomAt(this.gridX, this.gridY, this.z);
            IWorldRegion wr = IsoRegions.getIsoWorldRegion(this.gridX, this.gridY, this.z);
            if (sq != null && (iWorldRegion = sq.getIsoWorldRegion()) instanceof IsoWorldRegion && (buildingDef = (worldRegion = (IsoWorldRegion)iWorldRegion).getBuildingDef()) != null) {
                for (RoomDef roomDef : buildingDef.getRooms()) {
                    if (roomDef.level != this.z) continue;
                    float r = 1.0f;
                    float g = 1.0f;
                    float b = 1.0f;
                    if (roomDef.contains(this.gridX, this.gridY)) {
                        g = 0.0f;
                        r = 0.0f;
                        RoomDef roomDef1 = IsoWorld.instance.getMetaGrid().getRoomAt(this.gridX, this.gridY, this.z);
                        boolean bl = true;
                    }
                    for (RoomDef.RoomRect rect : roomDef.getRects()) {
                        this.DrawIsoRect(rect.getX(), rect.getY(), rect.getW(), rect.getH(), r, g, 1.0f, 1.0f, 1);
                    }
                }
            }
        }
        if (this.roomLightRects.getValue()) {
            ArrayList<IsoRoomLight> roomLights = IsoWorld.instance.currentCell.roomLights;
            for (i = 0; i < roomLights.size(); ++i) {
                IsoRoomLight roomLight = roomLights.get(i);
                if (roomLight.z != this.z) continue;
                this.DrawIsoRect(roomLight.x, roomLight.y, roomLight.width, roomLight.height, 0.0f, 1.0f, 1.0f, 1.0f, 1);
            }
        }
        if (this.flyBuzzEmitters.getValue()) {
            FliesSound.instance.render(this.playerIndex);
        }
        if (this.closestRoomSquare.getValue()) {
            BuildingDef buildingDef;
            float px = IsoPlayer.players[this.playerIndex].getX();
            float py = IsoPlayer.players[this.playerIndex].getY();
            if (AmbientStreamManager.getInstance() instanceof AmbientStreamManager && (buildingDef = AmbientStreamManager.getNearestBuilding(px, py)) != null) {
                Vector2f closestXY = BaseVehicle.allocVector2f();
                buildingDef.getClosestPoint(px, py, closestXY);
                this.DrawIsoLine(px, py, closestXY.x, closestXY.y, 1.0f, 1.0f, 1.0f, 1.0f, 1);
                BaseVehicle.releaseVector2f(closestXY);
            }
        }
        if (this.table != null && this.table.rawget("selectedSquare") != null && (object = this.table.rawget("selectedSquare")) instanceof IsoGridSquare) {
            IsoGridSquare square = (IsoGridSquare)object;
            this.DrawIsoRect(square.x, square.y, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 2);
        }
        LineDrawer.render();
        LineDrawer.clear();
    }

    private void renderUI() {
        int playerIndex = this.playerIndex;
        Stack<IsoLightSource> lampposts = IsoWorld.instance.getCell().getLamppostPositions();
        int activeLights = 0;
        for (int i = 0; i < lampposts.size(); ++i) {
            IsoLightSource l = (IsoLightSource)lampposts.get(i);
            if (!l.active) continue;
            ++activeLights;
        }
        if (PerformanceSettings.fboRenderChunk) {
            int x = 220 + Core.getInstance().getOptionFontSizeReal() * 50;
            int y = 80;
            if (this.chunkColorTexture.getValue()) {
                x = this.renderChunkTexture(x, 80, true);
            }
            if (this.chunkDepthTexture.getValue()) {
                this.renderChunkTexture(x, 80, false);
            }
        }
        UIManager.render();
    }

    private int renderChunkTexture(int x, int y, boolean colorTexture) {
        IsoChunk chunk1 = IsoWorld.instance.currentCell.getChunkForGridSquare(this.gridX, this.gridY, 0);
        if (chunk1 == null) {
            return x;
        }
        FBORenderLevels renderLevels = chunk1.getRenderLevels(this.playerIndex);
        FBORenderChunk rc = renderLevels.getFBOForLevel(this.z, Core.getInstance().getZoom(this.playerIndex));
        if (rc == null || !rc.isInit) {
            return x;
        }
        Texture tex = colorTexture ? rc.tex : rc.depth;
        float scale = 0.5f;
        boolean bRCM = DebugOptions.instance.fboRenderChunk.combinedFbo.getValue();
        if (bRCM) {
            tex = colorTexture ? FBORenderChunkManager.instance.combinedTexture : FBORenderChunkManager.instance.combinedDepthTexture;
            scale = 0.125f;
        }
        if (tex == null) {
            return x;
        }
        SpriteRenderer.instance.render(null, x, y, (float)tex.getWidth() * scale, (float)tex.getHeight() * scale, 0.2f, 0.2f, 0.2f, 1.0f, null);
        SpriteRenderer.instance.render(tex, x, y, (float)tex.getWidth() * scale, (float)tex.getHeight() * scale, 1.0f, 1.0f, 1.0f, 1.0f, null);
        TextManager.instance.DrawString(x + 4, y + 4, "Level %d-%d / %d-%d".formatted(rc.getMinLevel(), rc.getTopLevel(), chunk1.minLevel, chunk1.maxLevel));
        return x + (int)Math.ceil((float)tex.getWidth() * scale);
    }

    public void setTable(KahluaTable table) {
        this.table = table;
    }

    public GameStateMachine.StateAction updateScene() {
        IsoPlayer.setInstance(IsoPlayer.players[this.playerIndex]);
        IsoCamera.setCameraCharacter(IsoPlayer.players[this.playerIndex]);
        UIManager.setPicked(IsoObjectPicker.Instance.ContextPick(Mouse.getXA(), Mouse.getYA()));
        IsoObject mouseOverObject = UIManager.getPicked() == null ? null : UIManager.getPicked().tile;
        UIManager.setLastPicked(mouseOverObject);
        if (GameKeyboard.isKeyDown(19)) {
            if (!keyQpressed) {
                DebugOptions.instance.terrain.renderTiles.newRender.setValue(true);
                keyQpressed = true;
                DebugLog.General.debugln("IsoCell.newRender = %s", DebugOptions.instance.terrain.renderTiles.newRender.getValue());
            }
        } else {
            keyQpressed = false;
        }
        if (GameKeyboard.isKeyDown(20)) {
            if (!keyQpressed) {
                DebugOptions.instance.terrain.renderTiles.newRender.setValue(false);
                keyQpressed = true;
                DebugLog.General.debugln("IsoCell.newRender = %s", DebugOptions.instance.terrain.renderTiles.newRender.getValue());
            }
        } else {
            keyQpressed = false;
        }
        if (GameKeyboard.isKeyDown(31)) {
            if (!keyQpressed) {
                ParticlesFire.getInstance().reloadShader();
                keyQpressed = true;
                DebugLog.General.debugln("ParticlesFire.reloadShader");
            }
        } else {
            keyQpressed = false;
        }
        IsoCamera.update();
        this.updateCursor();
        return GameStateMachine.StateAction.Remain;
    }

    private void saveGameUI() {
        this.gameUi.clear();
        this.gameUi.addAll(UIManager.UI);
        UIManager.UI.clear();
        this.suspendUi = UIManager.suspend;
        UIManager.suspend = false;
        UIManager.setShowPausedMessage(false);
        UIManager.defaultthread = this.luaEnv.thread;
        LuaEventManager.getEvents(this.eventList, this.eventMap);
        LuaEventManager.setEvents(new ArrayList<Event>(), new HashMap<String, Event>());
    }

    private void restoreGameUI() {
        this.selfUi.clear();
        this.selfUi.addAll(UIManager.UI);
        UIManager.UI.clear();
        UIManager.UI.addAll(this.gameUi);
        UIManager.suspend = this.suspendUi;
        UIManager.setShowPausedMessage(true);
        UIManager.defaultthread = LuaManager.thread;
        LuaEventManager.setEvents(this.eventList, this.eventMap);
        this.eventList.clear();
        this.eventMap.clear();
    }

    public Object fromLua0(String func) {
        switch (func) {
            case "exit": {
                this.exit = true;
                return null;
            }
            case "getCameraDragX": {
                return BoxedStaticValues.toDouble(-IsoCamera.cameras[this.playerIndex].deferedX);
            }
            case "getCameraDragY": {
                return BoxedStaticValues.toDouble(-IsoCamera.cameras[this.playerIndex].deferedY);
            }
            case "getPlayerIndex": {
                return BoxedStaticValues.toDouble(this.playerIndex);
            }
            case "getVehicleStory": {
                return this.vehicleStoryName;
            }
            case "getZ": {
                return BoxedStaticValues.toDouble(this.z);
            }
        }
        throw new IllegalArgumentException(String.format("unhandled \"%s\"", func));
    }

    public Object fromLua1(String func, Object arg0) {
        switch (func) {
            case "getCameraDragX": {
                return BoxedStaticValues.toDouble(-IsoCamera.cameras[this.playerIndex].deferedX);
            }
            case "getCameraDragY": {
                return BoxedStaticValues.toDouble(-IsoCamera.cameras[this.playerIndex].deferedY);
            }
            case "getObjectAtCursor": {
                return switch ((String)arg0) {
                    case "id" -> this.objectAtCursorId.getValue();
                    case "levels" -> this.objectAtCursorLevels.getValue();
                    case "scale" -> this.objectAtCursorScale.getValue();
                    case "width" -> this.objectAtCursorWidth.getValue();
                    default -> throw new IllegalArgumentException(String.format("unhandled \"%s\" \"%s\"", func, arg0));
                };
            }
            case "setPlayerIndex": {
                this.playerIndex = PZMath.clamp(((Double)arg0).intValue(), 0, 3);
                return null;
            }
            case "setVehicleStory": {
                this.vehicleStoryName = (String)arg0;
                return null;
            }
            case "setZ": {
                this.z = PZMath.clamp(((Double)arg0).intValue(), -31, 31);
                return null;
            }
        }
        throw new IllegalArgumentException(String.format("unhandled \"%s\" \"%s\"", func, arg0));
    }

    public Object fromLua2(String func, Object arg0, Object arg1) {
        switch (func) {
            case "dragCamera": {
                float dx = ((Double)arg0).floatValue();
                float dy = ((Double)arg1).floatValue();
                IsoCamera.cameras[this.playerIndex].deferedX = -dx;
                IsoCamera.cameras[this.playerIndex].deferedY = -dy;
                return null;
            }
            case "setObjectAtCursor": {
                switch ((String)arg0) {
                    case "id": {
                        this.objectAtCursorId.setValue((String)arg1);
                        break;
                    }
                    case "levels": {
                        this.objectAtCursorLevels.setValue(((Double)arg1).intValue());
                        break;
                    }
                    case "scale": {
                        this.objectAtCursorScale.setValue((Double)arg1);
                        break;
                    }
                    case "width": {
                        this.objectAtCursorWidth.setValue((Double)arg1);
                    }
                }
                return null;
            }
        }
        throw new IllegalArgumentException(String.format("unhandled \"%s\" \"%s\" \\\"%s\\\"", func, arg0, arg1));
    }

    private void updateCursor() {
        int playerIndex = this.playerIndex;
        int scale = Core.tileScale;
        float x = Mouse.getXA();
        float y = Mouse.getYA();
        x -= (float)IsoCamera.getScreenLeft(playerIndex);
        y -= (float)IsoCamera.getScreenTop(playerIndex);
        int z = PZMath.fastfloor(this.z);
        this.gridXf = IsoUtils.XToIso(x *= Core.getInstance().getZoom(playerIndex), y *= Core.getInstance().getZoom(playerIndex), z);
        this.gridYf = IsoUtils.YToIso(x, y, z);
        this.gridX = PZMath.fastfloor(this.gridXf);
        this.gridY = PZMath.fastfloor(this.gridYf);
    }

    private void DrawIsoLine(float x, float y, float x2, float y2, float r, float g, float b, float a, int thickness) {
        float z = this.z;
        float sx = IsoUtils.XToScreenExact(x, y, z, 0);
        float sy = IsoUtils.YToScreenExact(x, y, z, 0);
        float sx2 = IsoUtils.XToScreenExact(x2, y2, z, 0);
        float sy2 = IsoUtils.YToScreenExact(x2, y2, z, 0);
        int playerIndex = IsoCamera.frameState.playerIndex;
        PlayerCamera camera = IsoCamera.cameras[playerIndex];
        float zoom = camera.zoom;
        LineDrawer.drawLine(sx += camera.fixJigglyModelsX * zoom, sy += camera.fixJigglyModelsY * zoom, sx2 += camera.fixJigglyModelsX * zoom, sy2 += camera.fixJigglyModelsY * zoom, r, g, b, a, thickness);
    }

    private void DrawIsoRect(float x, float y, float width, float height, float r, float g, float b, float a, int thickness) {
        this.DrawIsoLine(x, y, x + width, y, r, g, b, a, thickness);
        this.DrawIsoLine(x + width, y, x + width, y + height, r, g, b, a, thickness);
        this.DrawIsoLine(x + width, y + height, x, y + height, r, g, b, a, thickness);
        this.DrawIsoLine(x, y + height, x, y, r, g, b, a, thickness);
    }

    private void drawGrid() {
        int x;
        int y;
        int playerIndex = this.playerIndex;
        float topLeftX = IsoUtils.XToIso(-128.0f, -256.0f, this.z);
        float topRightY = IsoUtils.YToIso(Core.getInstance().getOffscreenWidth(playerIndex) + 128, -256.0f, this.z);
        float bottomRightX = IsoUtils.XToIso(Core.getInstance().getOffscreenWidth(playerIndex) + 128, Core.getInstance().getOffscreenHeight(playerIndex) + 256, this.z);
        float bottomLeftY = IsoUtils.YToIso(-128.0f, Core.getInstance().getOffscreenHeight(playerIndex) + 256, this.z);
        int minY = (int)topRightY;
        int maxY = (int)bottomLeftY;
        int minX = (int)topLeftX;
        int maxX = (int)bottomRightX;
        minX -= 2;
        for (y = minY -= 2; y <= maxY; ++y) {
            if (PZMath.coordmodulo(y, 8) != 0) continue;
            this.DrawIsoLine(minX, y, maxX, y, 1.0f, 1.0f, 1.0f, 0.5f, 1);
        }
        for (x = minX; x <= maxX; ++x) {
            if (PZMath.coordmodulo(x, 8) != 0) continue;
            this.DrawIsoLine(x, minY, x, maxY, 1.0f, 1.0f, 1.0f, 0.5f, 1);
        }
        for (y = minY; y <= maxY; ++y) {
            if (y % 256 != 0) continue;
            this.DrawIsoLine(minX, y, maxX, y, 0.0f, 1.0f, 0.0f, 0.5f, 1);
        }
        for (x = minX; x <= maxX; ++x) {
            if (x % 256 != 0) continue;
            this.DrawIsoLine(x, minY, x, maxY, 0.0f, 1.0f, 0.0f, 0.5f, 1);
        }
        if (GameClient.client) {
            for (y = minY; y <= maxY; ++y) {
                if (y % 64 != 0) continue;
                this.DrawIsoLine(minX, y, maxX, y, 1.0f, 0.0f, 0.0f, 0.5f, 1);
            }
            for (x = minX; x <= maxX; ++x) {
                if (x % 64 != 0) continue;
                this.DrawIsoLine(x, minY, x, maxY, 1.0f, 0.0f, 0.0f, 0.5f, 1);
            }
        }
    }

    public void drawObjectAtCursor() {
        if (this.objectAtCursor.getValue()) {
            int stateIndex = SpriteRenderer.instance.getMainStateIndex();
            if (this.geometryDrawers[stateIndex] == null) {
                this.geometryDrawers[stateIndex] = new GeometryDrawer();
            }
            GeometryDrawer drawer = this.geometryDrawers[stateIndex];
            drawer.renderMain();
            SpriteRenderer.instance.drawGeneric(drawer);
        }
    }

    private void drawTestModels() {
        IsoGridSquare square = IsoWorld.instance.currentCell.getGridSquare(this.gridX, this.gridY, this.z);
        if (square == null) {
            return;
        }
    }

    private void drawCursor() {
        int playerIndex = this.playerIndex;
        int scale = Core.tileScale;
        float z = this.z;
        int sx = (int)IsoUtils.XToScreenExact(this.gridX, this.gridY + 1, z, 0);
        int sy = (int)IsoUtils.YToScreenExact(this.gridX, this.gridY + 1, z, 0);
        SpriteRenderer.instance.renderPoly(sx, sy, sx + 32 * scale, sy - 16 * scale, sx + 64 * scale, sy, sx + 32 * scale, sy + 16 * scale, 0.0f, 0.0f, 1.0f, 0.5f);
        if (PerformanceSettings.fboRenderChunk && this.depthValues.getValue()) {
            float playerX = IsoPlayer.players[playerIndex].getX();
            float playerY = IsoPlayer.players[playerIndex].getY();
            IsoDepthHelper.Results results = IsoDepthHelper.getSquareDepthData(PZMath.fastfloor(playerX), PZMath.fastfloor(playerY), this.gridXf, this.gridYf, this.z);
            float characterDepth = results.depthStart - 0.001f;
            TextManager.instance.DrawString(FONT, sx, sy, Float.toString(characterDepth), 1.0, 1.0, 1.0, 1.0);
            float squareDepth = IsoSprite.calculateDepth(this.gridXf, this.gridYf, this.z);
            results = IsoDepthHelper.getChunkDepthData(PZMath.fastfloor(playerX / 8.0f), PZMath.fastfloor(playerY / 8.0f), PZMath.fastfloor(this.gridXf / 8.0f), PZMath.fastfloor(this.gridYf / 8.0f), this.z);
            float depth = results.depthStart;
            TextManager.instance.DrawString(FONT, sx, sy + TextManager.instance.getFontHeight(FONT), String.format("%.4f (%.4f + %.4f)", Float.valueOf(depth + squareDepth), Float.valueOf(depth), Float.valueOf(squareDepth)), 1.0, 1.0, 1.0, 1.0);
        }
        IsoChunkMap cm = IsoWorld.instance.getCell().chunkMap[playerIndex];
        for (int y = cm.getWorldYMinTiles(); y < cm.getWorldYMaxTiles(); ++y) {
            for (int x = cm.getWorldXMinTiles(); x < cm.getWorldXMaxTiles(); ++x) {
                IsoGridSquare sq = IsoWorld.instance.getCell().getGridSquare((double)x, (double)y, z);
                if (sq == null) continue;
                if (sq != cm.getGridSquare(x, y, PZMath.fastfloor(z))) {
                    sx = (int)IsoUtils.XToScreenExact(x, y + 1, z, 0);
                    sy = (int)IsoUtils.YToScreenExact(x, y + 1, z, 0);
                    SpriteRenderer.instance.renderPoly(sx, sy, sx + 32, sy - 16, sx + 64, sy, sx + 32, sy + 16, 1.0f, 0.0f, 0.0f, 0.8f);
                }
                if (sq == null || sq.getX() != x || sq.getY() != y || (float)sq.getZ() != z || sq.e != null && sq.e.w != null && sq.e.w != sq || sq.w != null && sq.w.e != null && sq.w.e != sq || sq.n != null && sq.n.s != null && sq.n.s != sq || sq.s != null && sq.s.n != null && sq.s.n != sq || sq.nw != null && sq.nw.se != null && sq.nw.se != sq || sq.se != null && sq.se.nw != null && sq.se.nw != sq) {
                    sx = (int)IsoUtils.XToScreenExact(x, y + 1, z, 0);
                    sy = (int)IsoUtils.YToScreenExact(x, y + 1, z, 0);
                    SpriteRenderer.instance.renderPoly(sx, sy, sx + 32, sy - 16, sx + 64, sy, sx + 32, sy + 16, 1.0f, 0.0f, 0.0f, 0.5f);
                }
                if (sq != null) {
                    IsoGridSquare se;
                    IsoGridSquare w = sq.testPathFindAdjacent(null, -1, 0, 0) ? null : sq.getAdjacentSquare(IsoDirections.W);
                    IsoGridSquare n = sq.testPathFindAdjacent(null, 0, -1, 0) ? null : sq.getAdjacentSquare(IsoDirections.N);
                    IsoGridSquare e = sq.testPathFindAdjacent(null, 1, 0, 0) ? null : sq.getAdjacentSquare(IsoDirections.E);
                    IsoGridSquare s = sq.testPathFindAdjacent(null, 0, 1, 0) ? null : sq.getAdjacentSquare(IsoDirections.S);
                    IsoGridSquare nw = sq.testPathFindAdjacent(null, -1, -1, 0) ? null : sq.getAdjacentSquare(IsoDirections.NW);
                    IsoGridSquare ne = sq.testPathFindAdjacent(null, 1, -1, 0) ? null : sq.getAdjacentSquare(IsoDirections.NE);
                    IsoGridSquare sw = sq.testPathFindAdjacent(null, -1, 1, 0) ? null : sq.getAdjacentSquare(IsoDirections.SW);
                    IsoGridSquare isoGridSquare = se = sq.testPathFindAdjacent(null, 1, 1, 0) ? null : sq.getAdjacentSquare(IsoDirections.SE);
                    if (w != sq.w || n != sq.n || e != sq.e || s != sq.s || nw != sq.nw || ne != sq.ne || sw != sq.sw || se != sq.se) {
                        this.paintSquare(x, y, PZMath.fastfloor(z), 1.0f, 0.0f, 0.0f, 0.5f);
                    }
                }
                if (sq != null && (sq.getAdjacentSquare(IsoDirections.NW) != null && sq.getAdjacentSquare(IsoDirections.NW).getAdjacentSquare(IsoDirections.SE) != sq || sq.getAdjacentSquare(IsoDirections.NE) != null && sq.getAdjacentSquare(IsoDirections.NE).getAdjacentSquare(IsoDirections.SW) != sq || sq.getAdjacentSquare(IsoDirections.SW) != null && sq.getAdjacentSquare(IsoDirections.SW).getAdjacentSquare(IsoDirections.NE) != sq || sq.getAdjacentSquare(IsoDirections.SE) != null && sq.getAdjacentSquare(IsoDirections.SE).getAdjacentSquare(IsoDirections.NW) != sq || sq.getAdjacentSquare(IsoDirections.N) != null && sq.getAdjacentSquare(IsoDirections.N).getAdjacentSquare(IsoDirections.S) != sq || sq.getAdjacentSquare(IsoDirections.S) != null && sq.getAdjacentSquare(IsoDirections.S).getAdjacentSquare(IsoDirections.N) != sq || sq.getAdjacentSquare(IsoDirections.W) != null && sq.getAdjacentSquare(IsoDirections.W).getAdjacentSquare(IsoDirections.E) != sq || sq.getAdjacentSquare(IsoDirections.E) != null && sq.getAdjacentSquare(IsoDirections.E).getAdjacentSquare(IsoDirections.W) != sq)) {
                    sx = (int)IsoUtils.XToScreenExact(x, y + 1, z, 0);
                    sy = (int)IsoUtils.YToScreenExact(x, y + 1, z, 0);
                    SpriteRenderer.instance.renderPoly(sx, sy, sx + 32, sy - 16, sx + 64, sy, sx + 32, sy + 16, 1.0f, 0.0f, 0.0f, 0.5f);
                }
                if (this.emptySquares.getValue() && sq.getObjects().isEmpty()) {
                    this.paintSquare(x, y, PZMath.fastfloor(z), 1.0f, 1.0f, 0.0f, 0.5f);
                }
                if (sq.getRoom() != null && sq.isFree(false) && !VirtualZombieManager.instance.canSpawnAt(x, y, PZMath.fastfloor(z))) {
                    this.paintSquare(x, y, PZMath.fastfloor(z), 1.0f, 1.0f, 1.0f, 1.0f);
                }
                if (this.roofHideBuilding.getValue() && sq.roofHideBuilding != null) {
                    this.paintSquare(x, y, PZMath.fastfloor(z), 0.0f, 0.0f, 1.0f, 0.25f);
                }
                if (!this.occludedSquares.getValue() || !FBORenderOcclusion.getInstance().isOccluded(sq.x, sq.y, sq.z)) continue;
                this.paintSquare(x, y, sq.z, 1.0f, 1.0f, 1.0f, 0.5f);
            }
        }
        IsoGameCharacter isoGameCharacter = IsoCamera.getCameraCharacter();
        if (!PerformanceSettings.fboRenderChunk && isoGameCharacter.getCurrentSquare() != null && Math.abs(this.gridX - PZMath.fastfloor(isoGameCharacter.getX())) <= 1 && Math.abs(this.gridY - PZMath.fastfloor(isoGameCharacter.getY())) <= 1) {
            IsoGridSquare next = IsoWorld.instance.currentCell.getGridSquare(this.gridX, this.gridY, this.z);
            IsoObject thump = isoGameCharacter.getCurrentSquare().testCollideSpecialObjects(next);
            if (thump != null) {
                thump.getSprite().RenderGhostTileRed(PZMath.fastfloor(thump.getX()), PZMath.fastfloor(thump.getY()), PZMath.fastfloor(thump.getZ()));
            }
        }
        if (this.lineClearCollide.getValue()) {
            this.lineClearCached(IsoWorld.instance.currentCell, this.gridX, this.gridY, PZMath.fastfloor(z), PZMath.fastfloor(isoGameCharacter.getX()), PZMath.fastfloor(isoGameCharacter.getY()), this.z, false);
        }
        if (this.nearestWalls.getValue()) {
            NearestWalls.render(this.gridX, this.gridY, this.z, false);
        }
        if (this.nearestExteriorWalls.getValue()) {
            NearestWalls.render(this.gridX, this.gridY, this.z, true);
        }
        if (this.vehicleStory.getValue()) {
            this.drawVehicleStory();
        }
    }

    private void drawZones() {
        IsoGridSquare square;
        ArrayList<Zone> zones = IsoWorld.instance.metaGrid.getZonesAt(this.gridX, this.gridY, this.z, new ArrayList<Zone>());
        Zone topZone = null;
        for (int i = 0; i < zones.size(); ++i) {
            Zone zone = zones.get(i);
            if (zone.isPreferredZoneForSquare) {
                topZone = zone;
            }
            if (zone.isPolyline()) continue;
            if (!zone.points.isEmpty()) {
                for (int j = 0; j < zone.points.size(); j += 2) {
                    int x1 = zone.points.get(j);
                    int y1 = zone.points.get(j + 1);
                    int x2 = zone.points.get((j + 2) % zone.points.size());
                    int y2 = zone.points.get((j + 3) % zone.points.size());
                    this.DrawIsoLine(x1, y1, x2, y2, 1.0f, 1.0f, 0.0f, 1.0f, 1);
                }
                continue;
            }
            this.DrawIsoLine(zone.x, zone.y, zone.x + zone.w, zone.y, 1.0f, 1.0f, 0.0f, 1.0f, 1);
            this.DrawIsoLine(zone.x, zone.y + zone.h, zone.x + zone.w, zone.y + zone.h, 1.0f, 1.0f, 0.0f, 1.0f, 1);
            this.DrawIsoLine(zone.x, zone.y, zone.x, zone.y + zone.h, 1.0f, 1.0f, 0.0f, 1.0f, 1);
            this.DrawIsoLine(zone.x + zone.w, zone.y, zone.x + zone.w, zone.y + zone.h, 1.0f, 1.0f, 0.0f, 1.0f, 1);
        }
        zones = IsoWorld.instance.metaGrid.getZonesIntersecting(this.gridX - 1, this.gridY - 1, this.z, 3, 3, new ArrayList<Zone>());
        LiangBarsky lb = new LiangBarsky();
        double[] t1t2 = new double[2];
        IsoChunk chunk = IsoWorld.instance.currentCell.getChunkForGridSquare(this.gridX, this.gridY, this.z);
        for (int i = 0; i < zones.size(); ++i) {
            Zone zone = zones.get(i);
            if (zone == null || !zone.isPolyline() || zone.points.isEmpty()) continue;
            for (int j = 0; j < zone.points.size() - 2; j += 2) {
                int x1 = zone.points.get(j);
                int y1 = zone.points.get(j + 1);
                int x2 = zone.points.get(j + 2);
                int y2 = zone.points.get(j + 3);
                this.DrawIsoLine(x1, y1, x2, y2, 1.0f, 1.0f, 0.0f, 1.0f, 1);
                float dx = x2 - x1;
                float dy = y2 - y1;
                if (chunk == null || !lb.lineRectIntersect(x1, y1, dx, dy, chunk.wx * 10, chunk.wy * 10, chunk.wx * 10 + 10, chunk.wy * 10 + 10, t1t2)) continue;
                this.DrawIsoLine((float)x1 + (float)t1t2[0] * dx, (float)y1 + (float)t1t2[0] * dy, (float)x1 + (float)t1t2[1] * dx, (float)y1 + (float)t1t2[1] * dy, 0.0f, 1.0f, 0.0f, 1.0f, 1);
            }
            if (zone.polylineOutlinePoints == null) continue;
            float[] points = zone.polylineOutlinePoints;
            for (int j = 0; j < points.length; j += 2) {
                float x1 = points[j];
                float y1 = points[j + 1];
                float x2 = points[(j + 2) % points.length];
                float y2 = points[(j + 3) % points.length];
                this.DrawIsoLine(x1, y1, x2, y2, 1.0f, 1.0f, 0.0f, 1.0f, 1);
            }
        }
        VehicleZone vehicleZone = IsoWorld.instance.metaGrid.getVehicleZoneAt(this.gridX, this.gridY, this.z);
        if (vehicleZone != null) {
            float r = 0.5f;
            float g = 1.0f;
            float b = 0.5f;
            float a = 1.0f;
            if (vehicleZone.isPolygon()) {
                for (int j = 0; j < vehicleZone.points.size(); j += 2) {
                    int x1 = vehicleZone.points.get(j);
                    int y1 = vehicleZone.points.get(j + 1);
                    int x2 = vehicleZone.points.get((j + 2) % vehicleZone.points.size());
                    int y2 = vehicleZone.points.get((j + 3) % vehicleZone.points.size());
                    this.DrawIsoLine(x1, y1, x2, y2, 1.0f, 1.0f, 0.0f, 1.0f, 1);
                }
            } else if (vehicleZone.isPolyline()) {
                for (int j = 0; j < vehicleZone.points.size() - 2; j += 2) {
                    int x1 = vehicleZone.points.get(j);
                    int y1 = vehicleZone.points.get(j + 1);
                    int x2 = vehicleZone.points.get(j + 2);
                    int y2 = vehicleZone.points.get(j + 3);
                    this.DrawIsoLine(x1, y1, x2, y2, 1.0f, 1.0f, 0.0f, 1.0f, 1);
                }
                if (vehicleZone.polylineOutlinePoints != null) {
                    float[] points = vehicleZone.polylineOutlinePoints;
                    for (int j = 0; j < points.length; j += 2) {
                        float x1 = points[j];
                        float y1 = points[j + 1];
                        float x2 = points[(j + 2) % points.length];
                        float y2 = points[(j + 3) % points.length];
                        this.DrawIsoLine(x1, y1, x2, y2, 1.0f, 1.0f, 0.0f, 1.0f, 1);
                    }
                }
            } else {
                this.DrawIsoLine(vehicleZone.x, vehicleZone.y, vehicleZone.x + vehicleZone.w, vehicleZone.y, 0.5f, 1.0f, 0.5f, 1.0f, 1);
                this.DrawIsoLine(vehicleZone.x, vehicleZone.y + vehicleZone.h, vehicleZone.x + vehicleZone.w, vehicleZone.y + vehicleZone.h, 0.5f, 1.0f, 0.5f, 1.0f, 1);
                this.DrawIsoLine(vehicleZone.x, vehicleZone.y, vehicleZone.x, vehicleZone.y + vehicleZone.h, 0.5f, 1.0f, 0.5f, 1.0f, 1);
                this.DrawIsoLine(vehicleZone.x + vehicleZone.w, vehicleZone.y, vehicleZone.x + vehicleZone.w, vehicleZone.y + vehicleZone.h, 0.5f, 1.0f, 0.5f, 1.0f, 1);
            }
        }
        if (this.randomSquareInZone.getValue() && topZone != null && (square = topZone.getRandomSquareInZone()) != null) {
            this.paintSquare(square.x, square.y, square.z, 0.0f, 1.0f, 0.0f, 0.5f);
        }
    }

    private void drawVehicleStory() {
        ArrayList<Zone> zones = IsoWorld.instance.metaGrid.getZonesIntersecting(this.gridX - 1, this.gridY - 1, this.z, 3, 3, new ArrayList<Zone>());
        if (zones.isEmpty()) {
            return;
        }
        IsoChunk chunk = IsoWorld.instance.currentCell.getChunkForGridSquare(this.gridX, this.gridY, this.z);
        if (chunk == null) {
            return;
        }
        for (int i = 0; i < zones.size(); ++i) {
            Zone zone = zones.get(i);
            if (!"Nav".equals(zone.type)) continue;
            VehicleStorySpawner spawner = VehicleStorySpawner.getInstance();
            RandomizedVehicleStoryBase story = IsoWorld.instance.getRandomizedVehicleStoryByName(this.vehicleStoryName);
            if (story == null || !story.isValid(zone, chunk, true) || !story.initVehicleStorySpawner(zone, chunk, true)) continue;
            int spawnW = story.getMinZoneWidth();
            int spawnH = story.getMinZoneHeight();
            float[] xyd = new float[3];
            if (!story.getSpawnPoint(zone, chunk, xyd)) continue;
            float spawnX = xyd[0];
            float spawnY = xyd[1];
            float angle = xyd[2] + 1.5707964f;
            spawner.spawn(spawnX, spawnY, 0.0f, angle, (spawner2, element) -> {});
            spawner.render(spawnX, spawnY, 0.0f, spawnW, spawnH, xyd[2]);
        }
    }

    private void DrawBehindStuff() {
        this.IsBehindStuff(IsoCamera.getCameraCharacter().getCurrentSquare());
    }

    private boolean IsBehindStuff(IsoGridSquare sq) {
        for (int z = 1; z < 8 && sq.getZ() + z < 8; ++z) {
            for (int y = -5; y <= 6; ++y) {
                for (int x = -5; x <= 6; ++x) {
                    if (x < y - 5 || x > y + 5) continue;
                    this.paintSquare(sq.getX() + x + z * 3, sq.getY() + y + z * 3, sq.getZ() + z, 1.0f, 1.0f, 0.0f, 0.25f);
                }
            }
        }
        return true;
    }

    private boolean IsBehindStuffRecY(int x, int y, int z) {
        IsoGridSquare sq = IsoWorld.instance.currentCell.getGridSquare(x, y, z);
        if (z >= 15) {
            return false;
        }
        this.paintSquare(x, y, z, 1.0f, 1.0f, 0.0f, 0.25f);
        return this.IsBehindStuffRecY(x, y + 1, z + 1);
    }

    private boolean IsBehindStuffRecXY(int x, int y, int z, int n) {
        IsoGridSquare sq = IsoWorld.instance.currentCell.getGridSquare(x, y, z);
        if (z >= 15) {
            return false;
        }
        this.paintSquare(x, y, z, 1.0f, 1.0f, 0.0f, 0.25f);
        return this.IsBehindStuffRecXY(x + n, y + n, z + 1, n);
    }

    private boolean IsBehindStuffRecX(int x, int y, int z) {
        IsoGridSquare sq = IsoWorld.instance.currentCell.getGridSquare(x, y, z);
        if (z >= 15) {
            return false;
        }
        this.paintSquare(x, y, z, 1.0f, 1.0f, 0.0f, 0.25f);
        return this.IsBehindStuffRecX(x + 1, y, z + 1);
    }

    private void paintSquare(int x, int y, int z, float r, float g, float b, float a) {
        int scale = Core.tileScale;
        int sx = (int)IsoUtils.XToScreenExact(x, y + 1, z, 0);
        int sy = (int)IsoUtils.YToScreenExact(x, y + 1, z, 0);
        SpriteRenderer.instance.renderPoly(sx, sy, sx + 32 * scale, sy - 16 * scale, sx + 64 * scale, sy, sx + 32 * scale, sy + 16 * scale, r, g, b, a);
    }

    private void drawModData() {
        ErosionData.Square sqErosionData;
        int z = this.z;
        IsoGridSquare sq = IsoWorld.instance.getCell().getGridSquare(this.gridX, this.gridY, z);
        int x = Core.getInstance().getScreenWidth() - 250;
        int y = 10;
        int lineHgt = TextManager.instance.getFontFromEnum(FONT).getLineHeight();
        if (sq != null && sq.getModData() != null) {
            KahluaTable table = sq.getModData();
            this.DrawString(x, y += lineHgt, "MOD DATA x,y,z=" + sq.getX() + "," + sq.getY() + "," + sq.getZ());
            KahluaTableIterator it = table.iterator();
            while (it.advance()) {
                this.DrawString(x, y += lineHgt, it.getKey().toString() + " = " + it.getValue().toString());
                if (!(it.getValue() instanceof KahluaTable)) continue;
                KahluaTableIterator it2 = ((KahluaTable)it.getValue()).iterator();
                while (it2.advance()) {
                    this.DrawString(x + 8, y += lineHgt, it2.getKey().toString() + " = " + it2.getValue().toString());
                }
            }
            y += lineHgt;
        }
        if (sq != null) {
            PropertyContainer pc = sq.getProperties();
            ArrayList<String> keys2 = pc.getPropertyNames();
            if (!keys2.isEmpty()) {
                this.DrawString(x, y += lineHgt, "PROPERTIES x,y,z=" + sq.getX() + "," + sq.getY() + "," + sq.getZ());
                Collections.sort(keys2);
                for (String key : keys2) {
                    this.DrawString(x, y += lineHgt, key + " = \"" + pc.get(key) + "\"");
                }
            }
            for (IsoFlagType f : IsoFlagType.values()) {
                if (!pc.has(f)) continue;
                this.DrawString(x, y += lineHgt, f.toString());
            }
        }
        if (sq != null && (sqErosionData = sq.getErosionData()) != null) {
            y += lineHgt;
            this.DrawString(x, y += lineHgt, "EROSION x,y,z=" + sq.getX() + "," + sq.getY() + "," + sq.getZ());
            this.DrawString(x, y += lineHgt, "init=" + sqErosionData.init);
            this.DrawString(x, y += lineHgt, "doNothing=" + sqErosionData.doNothing);
            this.DrawString(x, y += lineHgt, "chunk.init=" + sq.chunk.getErosionData().init);
        }
    }

    private void drawPlayerInfo() {
        int x = Core.getInstance().getScreenWidth() - 250;
        int y = Core.getInstance().getScreenHeight() / 2;
        int lineHgt = TextManager.instance.getFontFromEnum(FONT).getLineHeight();
        IsoGameCharacter isoGameCharacter = IsoCamera.getCameraCharacter();
        this.DrawString(x, y += lineHgt, CharacterStat.BOREDOM.getId() + " = " + isoGameCharacter.getStats().get(CharacterStat.BOREDOM));
        this.DrawString(x, y += lineHgt, CharacterStat.ENDURANCE.getId() + " = " + isoGameCharacter.getStats().get(CharacterStat.ENDURANCE));
        this.DrawString(x, y += lineHgt, CharacterStat.FATIGUE.getId() + " = " + isoGameCharacter.getStats().get(CharacterStat.FATIGUE));
        this.DrawString(x, y += lineHgt, CharacterStat.HUNGER.getId() + " = " + isoGameCharacter.getStats().get(CharacterStat.HUNGER));
        this.DrawString(x, y += lineHgt, CharacterStat.PAIN.getId() + " = " + isoGameCharacter.getStats().get(CharacterStat.PAIN));
        this.DrawString(x, y += lineHgt, CharacterStat.PANIC.getId() + " = " + isoGameCharacter.getStats().get(CharacterStat.PANIC));
        this.DrawString(x, y += lineHgt, CharacterStat.STRESS.getId() + " = " + isoGameCharacter.getStats().get(CharacterStat.STRESS));
        this.DrawString(x, y += lineHgt, "clothingTemp = " + ((IsoPlayer)isoGameCharacter).getPlayerClothingTemperature());
        this.DrawString(x, y += lineHgt, CharacterStat.TEMPERATURE.getId() + " = " + isoGameCharacter.getStats().get(CharacterStat.TEMPERATURE));
        this.DrawString(x, y += lineHgt, CharacterStat.THIRST.getId() + " = " + isoGameCharacter.getStats().get(CharacterStat.THIRST));
        this.DrawString(x, y += lineHgt, CharacterStat.FOOD_SICKNESS.getId() + " = " + isoGameCharacter.getStats().get(CharacterStat.FOOD_SICKNESS));
        this.DrawString(x, y += lineHgt, CharacterStat.POISON.getId() + " = " + isoGameCharacter.getStats().get(CharacterStat.POISON));
        this.DrawString(x, y += lineHgt, CharacterStat.UNHAPPINESS.getId() + " = " + isoGameCharacter.getStats().get(CharacterStat.UNHAPPINESS));
        this.DrawString(x, y += lineHgt, "infected = " + isoGameCharacter.getBodyDamage().isInfected());
        this.DrawString(x, y += lineHgt, CharacterStat.ZOMBIE_INFECTION.getId() + " = " + isoGameCharacter.getStats().get(CharacterStat.ZOMBIE_INFECTION));
        this.DrawString(x, y += lineHgt, CharacterStat.ZOMBIE_FEVER.getId() + " = " + isoGameCharacter.getStats().get(CharacterStat.ZOMBIE_FEVER));
        y += lineHgt;
        this.DrawString(x, y += lineHgt, "WORLD");
        this.DrawString(x, y += lineHgt, "globalTemperature = " + IsoWorld.instance.getGlobalTemperature());
    }

    public LosUtil.TestResults lineClearCached(IsoCell cell, int x1, int y1, int z1, int x0, int y0, int z0, boolean bIgnoreDoors) {
        int sx = x0;
        int sy = y0;
        int sz = z0;
        int dy = y1 - y0;
        int dx = x1 - x0;
        int dz = z1 - z0;
        int cx = dx;
        int cy = dy;
        int cz = dz;
        if ((cx += 100) < 0 || (cy += 100) < 0 || (cz += 16) < 0 || cx >= 200 || cy >= 200) {
            return LosUtil.TestResults.Blocked;
        }
        LosUtil.TestResults res = LosUtil.TestResults.Clear;
        int resultToPropagate = 1;
        float t = 0.5f;
        float t2 = 0.5f;
        int lx = x0;
        int ly = y0;
        int lz = z0;
        IsoGridSquare b = cell.getGridSquare(lx, ly, lz);
        if (Math.abs(dx) > Math.abs(dy) && Math.abs(dx) > Math.abs(dz)) {
            float m = (float)dy / (float)dx;
            float m2 = (float)dz / (float)dx;
            t += (float)y0;
            t2 += (float)z0;
            dx = dx < 0 ? -1 : 1;
            m *= (float)dx;
            m2 *= (float)dx;
            while (x0 != x1) {
                IsoGridSquare a = cell.getGridSquare(x0 += dx, PZMath.fastfloor(t += m), PZMath.fastfloor(t2 += m2));
                this.paintSquare(x0, PZMath.fastfloor(t), PZMath.fastfloor(t2), 1.0f, 1.0f, 1.0f, 0.5f);
                if (a != null && b != null && a.testVisionAdjacent(b.getX() - a.getX(), b.getY() - a.getY(), b.getZ() - a.getZ(), true, bIgnoreDoors) == LosUtil.TestResults.Blocked) {
                    this.paintSquare(x0, PZMath.fastfloor(t), PZMath.fastfloor(t2), 1.0f, 0.0f, 0.0f, 0.5f);
                    this.paintSquare(b.getX(), b.getY(), b.getZ(), 1.0f, 0.0f, 0.0f, 0.5f);
                    resultToPropagate = 4;
                }
                b = a;
            }
        } else if (Math.abs(dy) >= Math.abs(dx) && Math.abs(dy) > Math.abs(dz)) {
            float m = (float)dx / (float)dy;
            float m2 = (float)dz / (float)dy;
            t += (float)x0;
            t2 += (float)z0;
            dy = dy < 0 ? -1 : 1;
            m *= (float)dy;
            m2 *= (float)dy;
            while (y0 != y1) {
                IsoGridSquare a = cell.getGridSquare(PZMath.fastfloor(t += m), y0 += dy, PZMath.fastfloor(t2 += m2));
                this.paintSquare(PZMath.fastfloor(t), y0, PZMath.fastfloor(t2), 1.0f, 1.0f, 1.0f, 0.5f);
                if (a != null && b != null && a.testVisionAdjacent(b.getX() - a.getX(), b.getY() - a.getY(), b.getZ() - a.getZ(), true, bIgnoreDoors) == LosUtil.TestResults.Blocked) {
                    this.paintSquare(PZMath.fastfloor(t), y0, PZMath.fastfloor(t2), 1.0f, 0.0f, 0.0f, 0.5f);
                    this.paintSquare(b.getX(), b.getY(), b.getZ(), 1.0f, 0.0f, 0.0f, 0.5f);
                    resultToPropagate = 4;
                }
                b = a;
            }
        } else {
            float m = (float)dx / (float)dz;
            float m2 = (float)dy / (float)dz;
            t += (float)x0;
            t2 += (float)y0;
            dz = dz < 0 ? -1 : 1;
            m *= (float)dz;
            m2 *= (float)dz;
            while (z0 != z1) {
                IsoGridSquare a = cell.getGridSquare(PZMath.fastfloor(t += m), PZMath.fastfloor(t2 += m2), z0 += dz);
                this.paintSquare(PZMath.fastfloor(t), PZMath.fastfloor(t2), z0, 1.0f, 1.0f, 1.0f, 0.5f);
                if (a != null && b != null && a.testVisionAdjacent(b.getX() - a.getX(), b.getY() - a.getY(), b.getZ() - a.getZ(), true, bIgnoreDoors) == LosUtil.TestResults.Blocked) {
                    resultToPropagate = 4;
                }
                b = a;
            }
        }
        if (resultToPropagate == 1) {
            return LosUtil.TestResults.Clear;
        }
        if (resultToPropagate == 2) {
            return LosUtil.TestResults.ClearThroughOpenDoor;
        }
        if (resultToPropagate == 3) {
            return LosUtil.TestResults.ClearThroughWindow;
        }
        if (resultToPropagate == 4) {
            return LosUtil.TestResults.Blocked;
        }
        return LosUtil.TestResults.Blocked;
    }

    private void DrawString(int x, int y, String text) {
        int width = TextManager.instance.MeasureStringX(FONT, text);
        int height = TextManager.instance.getFontFromEnum(FONT).getLineHeight();
        SpriteRenderer.instance.renderi(null, x - 1, y, width + 2, height, 0.0f, 0.0f, 0.0f, 0.8f, null);
        TextManager.instance.DrawString(FONT, x, y, text, 1.0, 1.0, 1.0, 1.0);
    }

    public float getObjectAtCursorScale() {
        if (!this.objectAtCursorId.getValue().equalsIgnoreCase("player")) {
            return 1.0f;
        }
        return (float)this.objectAtCursorScale.getValue();
    }

    private void registerOption(ConfigOption option) {
        switch (option.getName()) {
            case "ObjectAtCursor.id": 
            case "ObjectAtCursor.levels": 
            case "ObjectAtCursor.scale": 
            case "ObjectAtCursor.width": {
                this.optionsHidden.add(option);
                break;
            }
            default: {
                this.options.add(option);
            }
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
        String fileName = ZomboidFileSystem.instance.getCacheDir() + File.separator + "debugChunkState-options.ini";
        ConfigFile configFile = new ConfigFile();
        ArrayList<ConfigOption> options1 = new ArrayList<ConfigOption>(this.options);
        options1.addAll(this.optionsHidden);
        configFile.write(fileName, 1, options1);
        options1.clear();
    }

    public void load() {
        ConfigFile configFile = new ConfigFile();
        String fileName = ZomboidFileSystem.instance.getCacheDir() + File.separator + "debugChunkState-options.ini";
        if (configFile.read(fileName)) {
            for (int i = 0; i < configFile.getOptions().size(); ++i) {
                ConfigOption configOption = configFile.getOptions().get(i);
                ConfigOption myOption = this.getOptionByName(configOption.getName());
                if (myOption == null) {
                    for (int j = 0; j < this.optionsHidden.size(); ++j) {
                        ConfigOption myOption1 = this.optionsHidden.get(j);
                        if (!myOption1.getName().equals(configOption.getName())) continue;
                        myOption = myOption1;
                        break;
                    }
                }
                if (myOption == null) continue;
                myOption.parse(configOption.getValueAsString());
            }
        }
    }

    static {
        FONT = UIFont.DebugConsole;
        m_clipperOffset = null;
    }

    private static final class GeometryDrawer
    extends TextureDraw.GenericDrawer {
        String object;
        int levels;
        float scale;
        float width;
        float playerX;
        float playerY;
        float x;
        float y;
        float z;

        private GeometryDrawer() {
        }

        public void renderMain() {
            this.object = DebugChunkState.instance.objectAtCursorId.getValue();
            this.levels = DebugChunkState.instance.objectAtCursorLevels.getValue();
            this.scale = (float)DebugChunkState.instance.objectAtCursorScale.getValue();
            this.width = (float)DebugChunkState.instance.objectAtCursorWidth.getValue();
            this.playerX = IsoPlayer.players[DebugChunkState.instance.playerIndex].getX();
            this.playerY = IsoPlayer.players[DebugChunkState.instance.playerIndex].getY();
            this.x = DebugChunkState.instance.gridXf;
            this.y = DebugChunkState.instance.gridYf;
            this.z = DebugChunkState.instance.z;
        }

        @Override
        public void render() {
            float a;
            float b;
            float g;
            float r;
            Shader shader = ShaderManager.instance.getOrCreateShader("debug_chunk_state_geometry", false, false);
            if (shader.getShaderProgram() == null || !shader.getShaderProgram().isCompiled()) {
                return;
            }
            boolean bGeometryOriginAtCenter = "box".equalsIgnoreCase(this.object);
            Core.getInstance().DoPushIsoStuff(this.x, this.y, this.z + (bGeometryOriginAtCenter ? (float)this.levels / 2.0f : 0.0f), 0.0f, false);
            VBORenderer vbor = VBORenderer.getInstance();
            vbor.setDepthTestForAllRuns(Boolean.TRUE);
            float height = 2.44949f * (float)this.levels;
            Matrix4f modelView = Core.getInstance().modelViewMatrixStack.alloc();
            modelView.identity();
            if ("box".equalsIgnoreCase(this.object)) {
                // empty if block
            }
            if ("cylinder".equalsIgnoreCase(this.object)) {
                modelView.rotateXYZ(4.712389f, 0.0f, (float)(IngameState.instance.numberTicks % 360L) * ((float)Math.PI / 180));
            }
            if ("plane".equalsIgnoreCase(this.object)) {
                modelView.rotateXYZ(1.5707964f, 0.0f, 0.0f);
            }
            modelView.scale(0.6666667f);
            Core.getInstance().modelViewMatrixStack.peek().mul(modelView, modelView);
            Core.getInstance().modelViewMatrixStack.push(modelView);
            float depthBufferValue = VertexBufferObject.getDepthValueAt(0.0f, 0.0f, 0.0f);
            IsoDepthHelper.Results results = IsoDepthHelper.getSquareDepthData(PZMath.fastfloor(this.playerX), PZMath.fastfloor(this.playerY), this.x, this.y, this.z + (bGeometryOriginAtCenter ? (float)this.levels / 2.0f : 0.0f));
            float targetDepth = results.depthStart - (depthBufferValue + 1.0f) / 2.0f;
            vbor.setUserDepthForAllRuns(Float.valueOf(targetDepth));
            GL11.glEnable(3042);
            GL11.glBlendFunc(770, 771);
            GL11.glDepthFunc(515);
            GL11.glDisable(2884);
            if ("box".equalsIgnoreCase(this.object)) {
                float r2 = 1.0f;
                float g2 = 1.0f;
                float b2 = 1.0f;
                float a2 = 1.0f;
                vbor.addBox(this.width, height, this.width, 1.0f, 1.0f, 1.0f, 1.0f, shader.getShaderProgram());
            }
            if ("cylinder".equalsIgnoreCase(this.object)) {
                float thickness = this.width;
                r = 1.0f;
                g = 1.0f;
                b = 1.0f;
                a = 1.0f;
                vbor.addCylinder_Fill(thickness / 2.0f, thickness / 2.0f, height, 48, 1, 1.0f, 1.0f, 1.0f, 1.0f, shader.getShaderProgram());
            }
            if ("plane".equalsIgnoreCase(this.object)) {
                float z = 0.0f;
                r = 1.0f;
                g = 1.0f;
                b = 1.0f;
                a = 1.0f;
                vbor.startRun(vbor.formatPositionColor);
                vbor.setMode(7);
                vbor.addQuad(-this.width / 2.0f, -this.width / 2.0f, this.width / 2.0f, this.width / 2.0f, 0.0f, 1.0f, 1.0f, 1.0f, 1.0f);
                vbor.endRun();
            }
            vbor.flush();
            vbor.setDepthTestForAllRuns(null);
            vbor.setUserDepthForAllRuns(null);
            Core.getInstance().modelViewMatrixStack.pop();
            Core.getInstance().DoPopIsoStuff();
            GLStateRenderThread.restore();
        }
    }

    @UsedFromLua
    public class BooleanDebugOption
    extends BooleanConfigOption {
        public BooleanDebugOption(DebugChunkState this$0, String name, boolean defaultValue) {
            Objects.requireNonNull(this$0);
            super(name, defaultValue);
            this$0.registerOption(this);
        }
    }

    public class StringDebugOption
    extends StringConfigOption {
        public StringDebugOption(DebugChunkState this$0, String name, String defaultValue) {
            Objects.requireNonNull(this$0);
            super(name, defaultValue, -1);
            this$0.registerOption(this);
        }
    }

    public class IntegerDebugOption
    extends IntegerConfigOption {
        public IntegerDebugOption(DebugChunkState this$0, String name, int min, int max, int defaultValue) {
            Objects.requireNonNull(this$0);
            super(name, min, max, defaultValue);
            this$0.registerOption(this);
        }
    }

    public class DoubleDebugOption
    extends DoubleConfigOption {
        public DoubleDebugOption(DebugChunkState this$0, String name, double min, double max, double defaultValue) {
            Objects.requireNonNull(this$0);
            super(name, min, max, defaultValue);
            this$0.registerOption(this);
        }
    }

    private class FloodFill {
        private IsoGridSquare start;
        private static final int FLOOD_SIZE = 11;
        private final BooleanGrid visited;
        private final Stack<IsoGridSquare> stack;
        private IsoBuilding building;
        private Mover mover;

        private FloodFill(DebugChunkState debugChunkState) {
            Objects.requireNonNull(debugChunkState);
            this.visited = new BooleanGrid(11, 11);
            this.stack = new Stack();
        }

        private void calculate(Mover mv, IsoGridSquare sq) {
            this.start = sq;
            this.mover = mv;
            if (this.start.getRoom() != null) {
                this.building = this.start.getRoom().getBuilding();
            }
            if (!this.push(this.start.getX(), this.start.getY())) {
                return;
            }
            while ((sq = this.pop()) != null) {
                int x = sq.getX();
                int y1 = sq.getY();
                while (this.shouldVisit(x, y1, x, y1 - 1)) {
                    --y1;
                }
                boolean spanRight = false;
                boolean spanLeft = false;
                do {
                    this.visited.setValue(this.gridX(x), this.gridY(y1), true);
                    if (!spanLeft && this.shouldVisit(x, y1, x - 1, y1)) {
                        if (!this.push(x - 1, y1)) {
                            return;
                        }
                        spanLeft = true;
                    } else if (spanLeft && !this.shouldVisit(x, y1, x - 1, y1)) {
                        spanLeft = false;
                    } else if (spanLeft && !this.shouldVisit(x - 1, y1, x - 1, y1 - 1) && !this.push(x - 1, y1)) {
                        return;
                    }
                    if (!spanRight && this.shouldVisit(x, y1, x + 1, y1)) {
                        if (!this.push(x + 1, y1)) {
                            return;
                        }
                        spanRight = true;
                        continue;
                    }
                    if (spanRight && !this.shouldVisit(x, y1, x + 1, y1)) {
                        spanRight = false;
                        continue;
                    }
                    if (!spanRight || this.shouldVisit(x + 1, y1, x + 1, y1 - 1) || this.push(x + 1, y1)) continue;
                    return;
                } while (this.shouldVisit(x, ++y1 - 1, x, y1));
            }
        }

        private boolean shouldVisit(int x1, int y1, int x2, int y2) {
            if (this.gridX(x2) >= 11 || this.gridX(x2) < 0) {
                return false;
            }
            if (this.gridY(y2) >= 11 || this.gridY(y2) < 0) {
                return false;
            }
            if (this.visited.getValue(this.gridX(x2), this.gridY(y2))) {
                return false;
            }
            IsoGridSquare sq = IsoWorld.instance.currentCell.getGridSquare(x2, y2, this.start.getZ());
            if (sq == null) {
                return false;
            }
            if (sq.has(IsoObjectType.stairsBN) || sq.has(IsoObjectType.stairsMN) || sq.has(IsoObjectType.stairsTN)) {
                return false;
            }
            if (sq.has(IsoObjectType.stairsBW) || sq.has(IsoObjectType.stairsMW) || sq.has(IsoObjectType.stairsTW)) {
                return false;
            }
            if (sq.getRoom() != null && this.building == null) {
                return false;
            }
            if (sq.getRoom() == null && this.building != null) {
                return false;
            }
            return !IsoWorld.instance.currentCell.blocked(this.mover, x2, y2, this.start.getZ(), x1, y1, this.start.getZ());
        }

        private boolean push(int x, int y) {
            IsoGridSquare sq = IsoWorld.instance.currentCell.getGridSquare(x, y, this.start.getZ());
            this.stack.push(sq);
            return true;
        }

        private IsoGridSquare pop() {
            return this.stack.isEmpty() ? null : this.stack.pop();
        }

        private int gridX(int x) {
            return x - (this.start.getX() - 5);
        }

        private int gridY(int y) {
            return y - (this.start.getY() - 5);
        }

        private int gridX(IsoGridSquare sq) {
            return sq.getX() - (this.start.getX() - 5);
        }

        private int gridY(IsoGridSquare sq) {
            return sq.getY() - (this.start.getY() - 5);
        }

        private void draw() {
            int minX = this.start.getX() - 5;
            int minY = this.start.getY() - 5;
            for (int y = 0; y < 11; ++y) {
                for (int x = 0; x < 11; ++x) {
                    if (!this.visited.getValue(x, y)) continue;
                    int sx = (int)IsoUtils.XToScreenExact(minX + x, minY + y + 1, this.start.getZ(), 0);
                    int sy = (int)IsoUtils.YToScreenExact(minX + x, minY + y + 1, this.start.getZ(), 0);
                    SpriteRenderer.instance.renderPoly(sx, sy, sx + 32, sy - 16, sx + 64, sy, sx + 32, sy + 16, 1.0f, 1.0f, 0.0f, 0.5f);
                }
            }
        }
    }
}

