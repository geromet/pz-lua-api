/*
 * Decompiled with CFR 0.152.
 */
package zombie.iso;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.joml.Vector4f;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.lwjgl.opengl.GL14;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL21;
import org.lwjgl.system.MemoryUtil;
import org.lwjglx.BufferUtils;
import zombie.GameTime;
import zombie.UsedFromLua;
import zombie.core.Core;
import zombie.core.PerformanceSettings;
import zombie.core.ShaderHelper;
import zombie.core.SpriteRenderer;
import zombie.core.Translator;
import zombie.core.math.PZMath;
import zombie.core.opengl.GLStateRenderThread;
import zombie.core.opengl.RenderThread;
import zombie.core.opengl.Shader;
import zombie.core.opengl.SharedVertexBufferObjects;
import zombie.core.skinnedmodel.model.VertexBufferObject;
import zombie.core.textures.Texture;
import zombie.core.textures.TextureDraw;
import zombie.debug.DebugLog;
import zombie.debug.DebugOptions;
import zombie.debug.LogSeverity;
import zombie.interfaces.ITexture;
import zombie.iso.IsoCamera;
import zombie.iso.IsoDepthHelper;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoPuddlesGeometry;
import zombie.iso.IsoUtils;
import zombie.iso.PlayerCamera;
import zombie.iso.PuddlesShader;
import zombie.iso.fboRenderChunk.FBORenderChunk;
import zombie.iso.fboRenderChunk.FBORenderChunkManager;
import zombie.iso.fboRenderChunk.FBORenderLevels;
import zombie.iso.weather.ClimateManager;
import zombie.network.GameServer;
import zombie.popman.ObjectPool;
import zombie.tileDepth.TileSeamManager;

@UsedFromLua
public final class IsoPuddles {
    public Shader effect;
    private float puddlesWindAngle;
    private float puddlesWindIntensity;
    private float puddlesTime;
    private final Vector2f puddlesParamWindInt;
    public static boolean leakingPuddlesInTheRoom;
    private Texture texHm;
    private ByteBuffer bufferHm;
    private int apiId;
    private static IsoPuddles instance;
    private static boolean isShaderEnable;
    static final int BYTES_PER_FLOAT = 4;
    static final int FLOATS_PER_VERTEX = 8;
    static final int BYTES_PER_VERTEX = 32;
    static final int VERTICES_PER_SQUARE = 4;
    public static final SharedVertexBufferObjects VBOs;
    private final RenderData[][] renderData = new RenderData[3][4];
    private final Vector4f shaderOffset = new Vector4f();
    private final Vector4f shaderOffsetMain = new Vector4f();
    private final FloatBuffer floatBuffer = BufferUtils.createFloatBuffer(16);
    public static final int BOOL_MAX = 0;
    public static final int FLOAT_RAIN = 0;
    public static final int FLOAT_WETGROUND = 1;
    public static final int FLOAT_MUDDYPUDDLES = 2;
    public static final int FLOAT_PUDDLESSIZE = 3;
    public static final int FLOAT_RAININTENSITY = 4;
    public static final int FLOAT_MAX = 5;
    private PuddlesFloat rain;
    private PuddlesFloat wetGround;
    private PuddlesFloat muddyPuddles;
    private PuddlesFloat puddlesSize;
    private PuddlesFloat rainIntensity;
    private final PuddlesFloat[] climateFloats = new PuddlesFloat[5];
    private final ObjectPool<RenderToChunkTexture> renderToChunkTexturePool = new ObjectPool<RenderToChunkTexture>(RenderToChunkTexture::new);

    public static synchronized IsoPuddles getInstance() {
        if (instance == null) {
            instance = new IsoPuddles();
        }
        return instance;
    }

    public boolean getShaderEnable() {
        return isShaderEnable;
    }

    public IsoPuddles() {
        if (GameServer.server && !GameServer.guiCommandline) {
            Core.getInstance().setPerfPuddles(3);
            this.applyPuddlesQuality();
            this.puddlesParamWindInt = new Vector2f(0.0f);
            this.setup();
            return;
        }
        this.texHm = Texture.getSharedTexture("media/textures/puddles_hm.png");
        RenderThread.invokeOnRenderContext(() -> {
            if (GL.getCapabilities().OpenGL30) {
                this.apiId = 1;
            }
            if (GL.getCapabilities().GL_ARB_framebuffer_object) {
                this.apiId = 2;
            }
            if (GL.getCapabilities().GL_EXT_framebuffer_object) {
                this.apiId = 3;
            }
        });
        this.applyPuddlesQuality();
        this.puddlesParamWindInt = new Vector2f(0.0f);
        for (int i = 0; i < this.renderData.length; ++i) {
            for (int pn = 0; pn < 4; ++pn) {
                this.renderData[i][pn] = new RenderData();
            }
        }
        this.setup();
    }

    public void applyPuddlesQuality() {
        boolean bl = leakingPuddlesInTheRoom = Core.getInstance().getPerfPuddles() == 0;
        if (Core.getInstance().getPerfPuddles() == 3) {
            isShaderEnable = false;
        } else {
            isShaderEnable = true;
            if (PerformanceSettings.puddlesQuality == 2) {
                RenderThread.invokeOnRenderContext(() -> {
                    this.effect = new PuddlesShader("puddles_lq");
                    this.effect.Start();
                    this.effect.End();
                });
            }
            if (PerformanceSettings.puddlesQuality == 1) {
                RenderThread.invokeOnRenderContext(() -> {
                    this.effect = new PuddlesShader("puddles_mq");
                    this.effect.Start();
                    this.effect.End();
                });
            }
            if (PerformanceSettings.puddlesQuality == 0) {
                RenderThread.invokeOnRenderContext(() -> {
                    this.effect = new PuddlesShader("puddles_hq");
                    this.effect.Start();
                    this.effect.End();
                });
            }
        }
    }

    public Vector4f getShaderOffset() {
        int playerIndex = SpriteRenderer.instance.getRenderingPlayerIndex();
        PlayerCamera camera = SpriteRenderer.instance.getRenderingPlayerCamera(playerIndex);
        float jx = -camera.fixJigglyModelsX * camera.zoom;
        float jy = -camera.fixJigglyModelsY * camera.zoom;
        return this.shaderOffset.set(camera.getOffX() + jx, camera.getOffY() + jy, camera.offscreenWidth, camera.offscreenHeight);
    }

    public Vector4f getShaderOffsetMain() {
        int playerIndex = IsoCamera.frameState.playerIndex;
        PlayerCamera camera = IsoCamera.cameras[playerIndex];
        float jx = -camera.fixJigglyModelsX * camera.zoom;
        float jy = -camera.fixJigglyModelsY * camera.zoom;
        return this.shaderOffsetMain.set(camera.getOffX() + jx, camera.getOffY() + jy, IsoCamera.getOffscreenWidth(playerIndex), IsoCamera.getOffscreenHeight(playerIndex));
    }

    public boolean shouldRenderPuddles() {
        if (!DebugOptions.instance.weather.waterPuddles.getValue()) {
            return false;
        }
        if (!this.getShaderEnable()) {
            return false;
        }
        if (!Core.getInstance().getUseShaders()) {
            return false;
        }
        if (Core.getInstance().getPerfPuddles() == 3) {
            return false;
        }
        return (double)this.wetGround.getFinalValue() != 0.0 || (double)this.puddlesSize.getFinalValue() != 0.0;
    }

    public void render(ArrayList<IsoGridSquare> grid, int z) {
        if (!DebugOptions.instance.weather.waterPuddles.getValue()) {
            return;
        }
        int stateIndex = SpriteRenderer.instance.getMainStateIndex();
        int playerIndex = IsoCamera.frameState.playerIndex;
        RenderData threadData = this.renderData[stateIndex][playerIndex];
        if (grid.isEmpty()) {
            return;
        }
        if (!this.getShaderEnable()) {
            return;
        }
        if (!Core.getInstance().getUseShaders()) {
            return;
        }
        if (Core.getInstance().getPerfPuddles() == 3) {
            return;
        }
        if (z > 0 && Core.getInstance().getPerfPuddles() > 0) {
            return;
        }
        if ((double)this.wetGround.getFinalValue() == 0.0 && (double)this.puddlesSize.getFinalValue() == 0.0) {
            return;
        }
        int firstSquare = threadData.numSquares;
        for (int i = 0; i < grid.size(); ++i) {
            IsoPuddlesGeometry puddlesGeometry = grid.get(i).getPuddles();
            if (puddlesGeometry == null || !puddlesGeometry.shouldRender()) continue;
            puddlesGeometry.updateLighting(playerIndex);
            threadData.addSquare(z, puddlesGeometry, null);
        }
        int numSquares = threadData.numSquares - firstSquare;
        if (numSquares <= 0) {
            return;
        }
        SpriteRenderer.instance.drawPuddles(playerIndex, z, firstSquare, numSquares);
    }

    public void puddlesProjection(Matrix4f projection) {
        int playerIndex = SpriteRenderer.instance.getRenderingPlayerIndex();
        PlayerCamera camera = SpriteRenderer.instance.getRenderingPlayerCamera(playerIndex);
        projection.setOrtho(camera.getOffX(), camera.getOffX() + (float)camera.offscreenWidth, camera.getOffY() + (float)camera.offscreenHeight, camera.getOffY(), -1.0f, 1.0f);
    }

    public void puddlesGeometry(int firstSquare, int numSquares) {
        while (numSquares > 0) {
            int some = this.renderSome(firstSquare, numSquares, false);
            firstSquare += some;
            numSquares -= some;
        }
        SpriteRenderer.ringBuffer.restoreVbos = true;
    }

    private int renderSome(int firstSquare, int numSquares, boolean bRenderToChunkTexture) {
        VBOs.next();
        FloatBuffer vertices = IsoPuddles.VBOs.vertices;
        ShortBuffer indices = IsoPuddles.VBOs.indices;
        boolean aVertex = false;
        boolean aColor = true;
        int aDirNE = 2;
        int aDirNW = 3;
        int aDirAll = 4;
        int aDirNone = 5;
        int aFragDepth = 6;
        GL20.glEnableVertexAttribArray(4);
        GL20.glEnableVertexAttribArray(5);
        GL20.glEnableVertexAttribArray(6);
        GL20.glVertexAttribPointer(2, 1, 5126, true, 32, 0L);
        GL20.glVertexAttribPointer(3, 1, 5126, true, 32, 4L);
        GL20.glVertexAttribPointer(4, 1, 5126, true, 32, 8L);
        GL20.glVertexAttribPointer(5, 1, 5126, true, 32, 12L);
        GL20.glVertexAttribPointer(0, 2, 5126, false, 32, 16L);
        GL20.glVertexAttribPointer(1, 4, 5121, true, 32, 24L);
        GL20.glVertexAttribPointer(6, 1, 5126, true, 32, 28L);
        int stateIndex = SpriteRenderer.instance.getRenderStateIndex();
        int playerIndex = SpriteRenderer.instance.getRenderingPlayerIndex();
        RenderData threadData = this.renderData[stateIndex][playerIndex];
        int numVertices = Math.min(numSquares * 4, IsoPuddles.VBOs.bufferSizeVertices);
        vertices.put(threadData.data, firstSquare * 4 * 8, numVertices * 8);
        int vertexCursor = 0;
        int indexCursor = 0;
        for (int i = 0; i < numVertices / 4; ++i) {
            indices.put((short)vertexCursor);
            indices.put((short)(vertexCursor + 1));
            indices.put((short)(vertexCursor + 2));
            indices.put((short)vertexCursor);
            indices.put((short)(vertexCursor + 2));
            indices.put((short)(vertexCursor + 3));
            vertexCursor += 4;
            indexCursor += 6;
        }
        VBOs.unmap();
        if (bRenderToChunkTexture) {
            GL11.glDepthMask(false);
        } else {
            GL11.glDepthMask(false);
            GL11.glBlendFunc(770, 771);
        }
        GL11.glEnable(2929);
        GL11.glDepthFunc(515);
        boolean start = false;
        int length = vertexCursor;
        boolean startIndex = false;
        int endIndex = indexCursor;
        GL12.glDrawRangeElements(4, 0, 0 + length, endIndex - 0, 5123, 0L);
        GL20.glDisableVertexAttribArray(4);
        GL20.glDisableVertexAttribArray(5);
        GL20.glDisableVertexAttribArray(6);
        return numVertices / 4;
    }

    public void update(ClimateManager cm) {
        this.puddlesWindAngle = cm.getCorrectedWindAngleIntensity();
        this.puddlesWindIntensity = cm.getWindIntensity();
        this.rain.setFinalValue(cm.getRainIntensity());
        float timeMultiplier = GameTime.getInstance().getThirtyFPSMultiplier();
        float dryingGround = 2.0E-5f * timeMultiplier * cm.getTemperature();
        float dryingPuddles = 2.0E-5f * timeMultiplier;
        float particleDeposition = 2.0E-4f * timeMultiplier;
        float rain = this.rain.getFinalValue();
        rain = rain * rain * 0.05f * timeMultiplier;
        this.rainIntensity.setFinalValue(this.rain.getFinalValue() * 2.0f);
        this.wetGround.addFinalValue(rain);
        this.muddyPuddles.addFinalValue(rain * 2.0f);
        this.puddlesSize.addFinalValueForMax(rain * 0.01f, 0.7f);
        if ((double)rain == 0.0) {
            this.wetGround.addFinalValue(-dryingGround);
            this.muddyPuddles.addFinalValue(-particleDeposition);
        }
        if ((double)this.wetGround.getFinalValue() == 0.0) {
            this.puddlesSize.addFinalValue(-dryingPuddles);
        }
        this.puddlesTime += 0.0166f * GameTime.getInstance().getMultiplier();
        this.puddlesParamWindInt.add((float)Math.sin(this.puddlesWindAngle * 6.0f) * this.puddlesWindIntensity * 0.05f, (float)Math.cos(this.puddlesWindAngle * 6.0f) * this.puddlesWindIntensity * 0.05f);
    }

    public float getShaderTime() {
        return this.puddlesTime;
    }

    public float getPuddlesSize() {
        return this.puddlesSize.getFinalValue();
    }

    public ITexture getHMTexture() {
        return this.texHm;
    }

    public ByteBuffer getHMTextureBuffer() {
        return this.bufferHm;
    }

    public void updateHMTextureBuffer() {
        if (PerformanceSettings.puddlesQuality == 2) {
            if (this.bufferHm == null) {
                try {
                    int bufferSize = this.texHm.getWidthHW() * this.texHm.getHeightHW() * 4;
                    this.bufferHm = MemoryUtil.memAlloc(bufferSize);
                    GL21.glGetTexImage(3553, 0, 6408, 5121, this.bufferHm);
                }
                catch (Exception e) {
                    DebugLog.General.printException(e, "IsoPuddles - Unable to create HMTextureBuffer. Low quality puddles will be non-interactable.", LogSeverity.Error);
                    this.freeHMTextureBuffer();
                }
            }
        } else {
            this.freeHMTextureBuffer();
        }
    }

    public void freeHMTextureBuffer() {
        if (this.bufferHm != null) {
            MemoryUtil.memFree(this.bufferHm);
            this.bufferHm = null;
        }
    }

    public FloatBuffer getPuddlesParams(int z) {
        this.floatBuffer.clear();
        this.floatBuffer.put(this.puddlesParamWindInt.x);
        this.floatBuffer.put(this.muddyPuddles.getFinalValue());
        this.floatBuffer.put(0.0f);
        this.floatBuffer.put(0.0f);
        this.floatBuffer.put(this.puddlesParamWindInt.y);
        this.floatBuffer.put(this.wetGround.getFinalValue());
        this.floatBuffer.put(0.0f);
        this.floatBuffer.put(0.0f);
        this.floatBuffer.put(this.puddlesWindIntensity * 1.0f);
        this.floatBuffer.put(this.puddlesSize.getFinalValue());
        this.floatBuffer.put(0.0f);
        this.floatBuffer.put(0.0f);
        this.floatBuffer.put(z);
        this.floatBuffer.put(this.rainIntensity.getFinalValue());
        this.floatBuffer.put(0.0f);
        this.floatBuffer.put(0.0f);
        this.floatBuffer.flip();
        return this.floatBuffer;
    }

    public float getRainIntensity() {
        return this.rainIntensity.getFinalValue();
    }

    public int getFloatMax() {
        return 5;
    }

    public int getBoolMax() {
        return 0;
    }

    public PuddlesFloat getPuddlesFloat(int id) {
        if (id >= 0 && id < 5) {
            return this.climateFloats[id];
        }
        DebugLog.log("ERROR: Climate: cannot get float override id.");
        return null;
    }

    private PuddlesFloat initClimateFloat(int id, String name) {
        if (id >= 0 && id < 5) {
            return this.climateFloats[id].init(id, name);
        }
        DebugLog.log("ERROR: Climate: cannot get float override id.");
        return null;
    }

    private void setup() {
        for (int i = 0; i < this.climateFloats.length; ++i) {
            this.climateFloats[i] = new PuddlesFloat();
        }
        this.rain = this.initClimateFloat(0, Translator.getText("IGUI_PuddlesControl_Rain"));
        this.wetGround = this.initClimateFloat(1, Translator.getText("IGUI_PuddlesControl_WetGround"));
        this.muddyPuddles = this.initClimateFloat(2, Translator.getText("IGUI_PuddlesControl_MudPuddle"));
        this.puddlesSize = this.initClimateFloat(3, Translator.getText("IGUI_PuddlesControl_PuddleSize"));
        this.rainIntensity = this.initClimateFloat(4, Translator.getText("IGUI_PuddlesControl_RainIntensity"));
    }

    public void clearThreadData() {
        int stateIndex = SpriteRenderer.instance.getMainStateIndex();
        int playerIndex = IsoCamera.frameState.playerIndex;
        RenderData threadData = this.renderData[stateIndex][playerIndex];
        threadData.clear();
    }

    public void renderToChunkTexture(ArrayList<IsoGridSquare> squares, int z) {
        if (squares.isEmpty()) {
            return;
        }
        if (z > 0 && Core.getInstance().getPerfPuddles() > 0) {
            return;
        }
        int stateIndex = SpriteRenderer.instance.getMainStateIndex();
        int playerIndex = IsoCamera.frameState.playerIndex;
        RenderData threadData = this.renderData[stateIndex][playerIndex];
        int firstSquare = threadData.numSquares;
        for (int i = 0; i < squares.size(); ++i) {
            IsoGridSquare square = squares.get(i);
            IsoPuddlesGeometry puddlesGeometry = square.getPuddles();
            if (puddlesGeometry == null || !puddlesGeometry.shouldRender()) continue;
            puddlesGeometry.updateLighting(playerIndex);
            threadData.addSquare(z, puddlesGeometry, null);
            if (!DebugOptions.instance.fboRenderChunk.seamFix2.getValue()) continue;
            int chunksPerWidth = 8;
            if (PZMath.coordmodulo(square.x, 8) == 7) {
                threadData.addSquare(z, puddlesGeometry, TileSeamManager.Tiles.FloorEast);
            }
            if (PZMath.coordmodulo(square.y, 8) != 7) continue;
            threadData.addSquare(z, puddlesGeometry, TileSeamManager.Tiles.FloorSouth);
        }
        if (threadData.numSquares == firstSquare) {
            return;
        }
        RenderToChunkTexture rtct = this.renderToChunkTexturePool.alloc();
        FBORenderChunk renderChunk = FBORenderChunkManager.instance.renderChunk;
        rtct.renderChunkX = IsoUtils.XToScreen(renderChunk.chunk.wx * 8, renderChunk.chunk.wy * 8, z, 0);
        rtct.renderChunkY = IsoUtils.YToScreen(renderChunk.chunk.wx * 8, renderChunk.chunk.wy * 8, z, 0);
        rtct.renderChunkWidth = renderChunk.w;
        rtct.renderChunkHeight = renderChunk.h;
        int chunkFloorYSpan = FBORenderChunk.FLOOR_HEIGHT * 8;
        int numLevels = renderChunk.getTopLevel() - renderChunk.getMinLevel() + 1;
        rtct.renderChunkBottom = chunkFloorYSpan + numLevels * FBORenderChunk.PIXELS_PER_LEVEL;
        rtct.renderChunkBottom += FBORenderLevels.extraHeightForJumboTrees(renderChunk.getMinLevel(), renderChunk.getTopLevel());
        rtct.renderChunkMinZ = renderChunk.getMinLevel();
        rtct.highRes = renderChunk.highRes;
        rtct.playerIndex = playerIndex;
        rtct.z = z;
        rtct.firstSquare = firstSquare;
        rtct.numSquares = threadData.numSquares - firstSquare;
        SpriteRenderer.instance.drawGeneric(rtct);
    }

    public float getWetGroundFinalValue() {
        return this.wetGround.getFinalValue();
    }

    public float getPuddlesSizeFinalValue() {
        return this.puddlesSize.getFinalValue();
    }

    static {
        VBOs = new SharedVertexBufferObjects(32);
    }

    private static final class RenderData {
        final int[] squaresPerLevel = new int[64];
        int numSquares;
        int capacity = 512;
        float[] data;

        RenderData() {
        }

        void clear() {
            this.numSquares = 0;
            Arrays.fill(this.squaresPerLevel, 0);
        }

        void addSquare(int z, IsoPuddlesGeometry pg, TileSeamManager.Tiles seamFix2) {
            int verticesPerSquare = 4;
            if (this.data == null) {
                this.data = new float[this.capacity * 4 * 8];
            }
            if (this.numSquares + 1 > this.capacity) {
                this.capacity += 128;
                this.data = Arrays.copyOf(this.data, this.capacity * 4 * 8);
            }
            int playerIndex = IsoCamera.frameState.playerIndex;
            PlayerCamera camera = IsoCamera.cameras[playerIndex];
            float jx = camera.fixJigglyModelsX * camera.zoom;
            float jy = camera.fixJigglyModelsY * camera.zoom;
            int n = this.numSquares * 4 * 8;
            for (int i = 0; i < 4; ++i) {
                this.data[n++] = pg.pdne[i];
                this.data[n++] = pg.pdnw[i];
                this.data[n++] = pg.pda[i];
                this.data[n++] = pg.pnon[i];
                this.data[n++] = pg.x[i] + jx;
                this.data[n++] = pg.y[i] + jy;
                this.data[n++] = Float.intBitsToFloat(pg.color[i]);
                if (DebugOptions.instance.fboRenderChunk.nolighting.getValue()) {
                    this.data[n - 1] = Float.intBitsToFloat(-1);
                }
                float dx = 0.0f;
                float dy = 0.0f;
                if (i == 2 || i == 3) {
                    dx = 1.0f;
                }
                if (i == 1 || i == 2) {
                    dy = 1.0f;
                }
                this.data[n++] = IsoDepthHelper.getSquareDepthData((int)PZMath.fastfloor((float)IsoCamera.frameState.camCharacterX), (int)PZMath.fastfloor((float)IsoCamera.frameState.camCharacterY), (float)((float)pg.square.x + (dx += camera.fixJigglyModelsSquareX)), (float)((float)pg.square.y + (dy += camera.fixJigglyModelsSquareY)), (float)((float)pg.square.z)).depthStart - 1.0E-4f;
                if (!FBORenderChunkManager.instance.isCaching()) continue;
                int n2 = n - 1;
                this.data[n2] = this.data[n2] - IsoDepthHelper.getChunkDepthData((int)PZMath.fastfloor((float)(IsoCamera.frameState.camCharacterX / 8.0f)), (int)PZMath.fastfloor((float)(IsoCamera.frameState.camCharacterY / 8.0f)), (int)pg.square.chunk.wx, (int)pg.square.chunk.wy, (int)z).depthStart;
                if (PZMath.coordmodulo(pg.square.x, 8) == 0 && dx == 0.0f) {
                    int n3 = n - 4;
                    this.data[n3] = this.data[n3] - 2.0f;
                    int n4 = n - 3;
                    this.data[n4] = this.data[n4] - 1.0f;
                }
                if (seamFix2 == TileSeamManager.Tiles.FloorSouth) {
                    int n5 = n - 1;
                    this.data[n5] = this.data[n5] - 0.0028867084f;
                    int n6 = n - 4;
                    this.data[n6] = this.data[n6] - (dy == 1.0f ? 6.0f : 64.0f);
                    int n7 = n - 3;
                    this.data[n7] = this.data[n7] + (dy == 1.0f ? 3.0f : 32.0f);
                }
                if (seamFix2 != TileSeamManager.Tiles.FloorEast) continue;
                int n8 = n - 1;
                this.data[n8] = this.data[n8] - 0.0028867084f;
                int n9 = n - 4;
                this.data[n9] = this.data[n9] + (dx == 1.0f ? 6.0f : 64.0f);
                int n10 = n - 3;
                this.data[n10] = this.data[n10] + (dx == 1.0f ? 3.0f : 32.0f);
            }
            ++this.numSquares;
            int n11 = z + 32;
            this.squaresPerLevel[n11] = this.squaresPerLevel[n11] + 1;
        }
    }

    @UsedFromLua
    public static class PuddlesFloat {
        protected float finalValue;
        private boolean isAdminOverride;
        private float adminValue;
        private final float min = 0.0f;
        private final float max = 1.0f;
        private final float delta = 0.01f;
        private int id;
        private String name;

        public PuddlesFloat init(int id, String name) {
            this.id = id;
            this.name = name;
            return this;
        }

        public int getID() {
            return this.id;
        }

        public String getName() {
            return this.name;
        }

        public float getMin() {
            return 0.0f;
        }

        public float getMax() {
            return 1.0f;
        }

        public void setEnableAdmin(boolean b) {
            this.isAdminOverride = b;
        }

        public boolean isEnableAdmin() {
            return this.isAdminOverride;
        }

        public void setAdminValue(float f) {
            this.adminValue = Math.max(0.0f, Math.min(1.0f, f));
        }

        public float getAdminValue() {
            return this.adminValue;
        }

        public void setFinalValue(float f) {
            this.finalValue = Math.max(0.0f, Math.min(1.0f, f));
        }

        public void addFinalValue(float f) {
            this.finalValue = Math.max(0.0f, Math.min(1.0f, this.finalValue + f));
        }

        public void addFinalValueForMax(float f, float maximum) {
            this.finalValue = Math.max(0.0f, Math.min(maximum, this.finalValue + f));
        }

        public float getFinalValue() {
            if (this.isAdminOverride) {
                return this.adminValue;
            }
            return this.finalValue;
        }

        public void interpolateFinalValue(float f) {
            this.finalValue = Math.abs(this.finalValue - f) < 0.01f ? f : (f > this.finalValue ? (this.finalValue += 0.01f) : (this.finalValue -= 0.01f));
        }

        private void calculate() {
            if (this.isAdminOverride) {
                this.finalValue = this.adminValue;
                return;
            }
        }
    }

    private static final class RenderToChunkTexture
    extends TextureDraw.GenericDrawer {
        float renderChunkX;
        float renderChunkY;
        int renderChunkWidth;
        int renderChunkHeight;
        int renderChunkBottom;
        int renderChunkMinZ;
        boolean highRes;
        int playerIndex;
        int z;
        int firstSquare;
        int numSquares;

        private RenderToChunkTexture() {
        }

        @Override
        public void render() {
            GL11.glPushClientAttrib(-1);
            GL11.glPushAttrib(1048575);
            Matrix4f projection = Core.getInstance().projectionMatrixStack.alloc();
            int chunkFloorYSpan = FBORenderChunk.FLOOR_HEIGHT * 8;
            if (this.highRes) {
                projection.setOrtho((float)(-this.renderChunkWidth) / 4.0f, (float)this.renderChunkWidth / 4.0f, (float)(-chunkFloorYSpan) / 2.0f + 256.0f - (float)(this.z * FBORenderChunk.PIXELS_PER_LEVEL), (float)chunkFloorYSpan / 2.0f + 256.0f - (float)(this.z * FBORenderChunk.PIXELS_PER_LEVEL), -1.0f, 1.0f);
            } else {
                projection.setOrtho((float)(-this.renderChunkWidth) / 2.0f, (float)this.renderChunkWidth / 2.0f, (float)(-chunkFloorYSpan) / 2.0f + 256.0f - (float)(this.z * FBORenderChunk.PIXELS_PER_LEVEL), (float)chunkFloorYSpan / 2.0f + 256.0f - (float)(this.z * FBORenderChunk.PIXELS_PER_LEVEL), -1.0f, 1.0f);
            }
            Core.getInstance().projectionMatrixStack.push(projection);
            Matrix4f modelView = Core.getInstance().modelViewMatrixStack.alloc();
            modelView.identity();
            Core.getInstance().modelViewMatrixStack.push(modelView);
            if (this.highRes) {
                GL11.glViewport(0, (this.renderChunkBottom - chunkFloorYSpan - (this.z - this.renderChunkMinZ) * FBORenderChunk.PIXELS_PER_LEVEL) * 2, this.renderChunkWidth, chunkFloorYSpan * 2);
            } else {
                GL11.glViewport(0, this.renderChunkBottom - chunkFloorYSpan - (this.z - this.renderChunkMinZ) * FBORenderChunk.PIXELS_PER_LEVEL, this.renderChunkWidth, chunkFloorYSpan);
            }
            int shaderID = IsoPuddles.getInstance().effect.getID();
            ShaderHelper.glUseProgramObjectARB(shaderID);
            Shader shader = Shader.ShaderMap.get(shaderID);
            if (shader instanceof PuddlesShader) {
                PuddlesShader puddlesShader = (PuddlesShader)shader;
                puddlesShader.updatePuddlesParams(this.playerIndex, this.z);
                int waterOffset = GL20.glGetUniformLocation(shaderID, "WOffset");
                float offsetX = this.renderChunkX;
                float offsetY = -this.renderChunkY;
                if (this.highRes) {
                    GL20.glUniform4f(waterOffset, offsetX - 90000.0f, offsetY - 640000.0f, this.renderChunkWidth / 2, chunkFloorYSpan);
                    waterViewport = GL20.glGetUniformLocation(shaderID, "WViewport");
                    GL20.glUniform4f(waterViewport, 0.0f, (this.renderChunkBottom - chunkFloorYSpan - (this.z - this.renderChunkMinZ) * FBORenderChunk.PIXELS_PER_LEVEL) * 2, this.renderChunkWidth, chunkFloorYSpan * 2);
                } else {
                    GL20.glUniform4f(waterOffset, offsetX - 90000.0f, offsetY - 640000.0f, this.renderChunkWidth, chunkFloorYSpan);
                    waterViewport = GL20.glGetUniformLocation(shaderID, "WViewport");
                    GL20.glUniform4f(waterViewport, 0.0f, this.renderChunkBottom - chunkFloorYSpan - (this.z - this.renderChunkMinZ) * FBORenderChunk.PIXELS_PER_LEVEL, this.renderChunkWidth, chunkFloorYSpan);
                }
            }
            VertexBufferObject.setModelViewProjection(shader.getProgram());
            GL14.glBlendFuncSeparate(770, 771, 1, 1);
            while (this.numSquares > 0) {
                int some = instance.renderSome(this.firstSquare, this.numSquares, true);
                this.firstSquare += some;
                this.numSquares -= some;
            }
            SpriteRenderer.ringBuffer.restoreVbos = true;
            Core.getInstance().projectionMatrixStack.pop();
            Core.getInstance().modelViewMatrixStack.pop();
            ShaderHelper.glUseProgramObjectARB(0);
            Texture.lastTextureID = -1;
            GL11.glPopAttrib();
            GL11.glPopClientAttrib();
            ShaderHelper.glUseProgramObjectARB(0);
            Texture.lastTextureID = -1;
            GLStateRenderThread.restore();
        }

        @Override
        public void postRender() {
            IsoPuddles.instance.renderToChunkTexturePool.release(this);
        }
    }
}

