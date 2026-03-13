/*
 * Decompiled with CFR 0.152.
 */
package zombie.iso.sprite;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.lang.invoke.CallSite;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Stack;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.joml.Vector3f;
import zombie.GameProfiler;
import zombie.GameTime;
import zombie.GameWindow;
import zombie.IndieGL;
import zombie.UsedFromLua;
import zombie.characters.IsoGameCharacter;
import zombie.characters.IsoPlayer;
import zombie.core.Core;
import zombie.core.PerformanceSettings;
import zombie.core.SceneShaderStore;
import zombie.core.SpriteRenderer;
import zombie.core.logger.ExceptionLogger;
import zombie.core.math.PZMath;
import zombie.core.opengl.CharacterModelCamera;
import zombie.core.opengl.Shader;
import zombie.core.opengl.ShaderUniformSetter;
import zombie.core.properties.IsoPropertyType;
import zombie.core.properties.PropertyContainer;
import zombie.core.properties.RoofProperties;
import zombie.core.skinnedmodel.ModelCameraRenderData;
import zombie.core.skinnedmodel.ModelManager;
import zombie.core.skinnedmodel.model.IsoObjectModelDrawer;
import zombie.core.textures.ColorInfo;
import zombie.core.textures.Mask;
import zombie.core.textures.Texture;
import zombie.core.textures.TextureDraw;
import zombie.debug.DebugOptions;
import zombie.debug.LineDrawer;
import zombie.iso.IsoCamera;
import zombie.iso.IsoDepthHelper;
import zombie.iso.IsoDirections;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoMovingObject;
import zombie.iso.IsoObject;
import zombie.iso.IsoObjectPicker;
import zombie.iso.IsoUtils;
import zombie.iso.IsoWater;
import zombie.iso.PlayerCamera;
import zombie.iso.SpriteDetails.IsoFlagType;
import zombie.iso.SpriteDetails.IsoObjectType;
import zombie.iso.SpriteModel;
import zombie.iso.Vector3;
import zombie.iso.WorldConverter;
import zombie.iso.fboRenderChunk.FBORenderCell;
import zombie.iso.fboRenderChunk.FBORenderChunkManager;
import zombie.iso.fboRenderChunk.FBORenderObjectHighlight;
import zombie.iso.fboRenderChunk.FBORenderObjectOutline;
import zombie.iso.fboRenderChunk.ObjectRenderInfo;
import zombie.iso.objects.IsoBarbecue;
import zombie.iso.objects.IsoCarBatteryCharger;
import zombie.iso.objects.IsoFire;
import zombie.iso.objects.IsoFireplace;
import zombie.iso.objects.IsoMolotovCocktail;
import zombie.iso.objects.IsoTrap;
import zombie.iso.objects.IsoTree;
import zombie.iso.objects.IsoWindow;
import zombie.iso.objects.IsoWorldInventoryObject;
import zombie.iso.objects.IsoZombieGiblets;
import zombie.iso.sprite.IsoAnim;
import zombie.iso.sprite.IsoDirectionFrame;
import zombie.iso.sprite.IsoSpriteGrid;
import zombie.iso.sprite.IsoSpriteInstance;
import zombie.iso.sprite.IsoSpriteManager;
import zombie.iso.sprite.shapers.WallShaper;
import zombie.iso.sprite.shapers.WallShaperSliceN;
import zombie.iso.sprite.shapers.WallShaperSliceW;
import zombie.iso.sprite.shapers.WallShaperWhole;
import zombie.iso.weather.fx.WeatherFxMask;
import zombie.tileDepth.CutawayAttachedModifier;
import zombie.tileDepth.TileDepthModifier;
import zombie.tileDepth.TileDepthShader;
import zombie.tileDepth.TileDepthTexture;
import zombie.tileDepth.TileDepthTextureManager;
import zombie.tileDepth.TileSeamManager;
import zombie.tileDepth.TileSeamModifier;
import zombie.tileDepth.TileSeamShader;
import zombie.util.StringUtils;
import zombie.vehicles.BaseVehicle;
import zombie.vehicles.VehicleModelCamera;

@UsedFromLua
public final class IsoSprite {
    public static int maxCount;
    public static float alphaStep;
    public static float globalOffsetX;
    public static float globalOffsetY;
    private static final ColorInfo info;
    private static final HashMap<String, Object[]> AnimNameSet;
    public int firerequirement;
    public String burntTile;
    public boolean forceAmbient;
    public boolean solidfloor;
    public boolean canBeRemoved;
    public boolean attachedFloor;
    public boolean cutW;
    public boolean cutN;
    public boolean solid;
    public boolean solidTrans;
    public boolean invisible;
    public boolean alwaysDraw;
    public boolean forceRender;
    public boolean moveWithWind;
    public boolean isBush;
    public static final byte RL_DEFAULT = 0;
    public static final byte RL_FLOOR = 1;
    public byte renderLayer = 0;
    public int windType = 1;
    public Texture texture;
    public boolean animate = true;
    public IsoAnim currentAnim;
    public boolean deleteWhenFinished;
    public boolean loop = true;
    public short soffX;
    public short soffY;
    public final PropertyContainer properties = new PropertyContainer();
    public final ColorInfo tintMod = new ColorInfo(1.0f, 1.0f, 1.0f, 1.0f);
    public HashMap<String, IsoAnim> animMap;
    public ArrayList<IsoAnim> animStack;
    public String name;
    public String tilesetName;
    public int tileSheetIndex;
    public static final int DEFAULT_SPRITE_ID = 20000000;
    public int id = 20000000;
    public IsoSpriteInstance def;
    public ModelManager.ModelSlot modelSlot;
    IsoSpriteManager parentManager;
    private IsoObjectType tileType = IsoObjectType.MAX;
    private String parentObjectName;
    private IsoSpriteGrid spriteGrid;
    public boolean treatAsWallOrder;
    public SpriteModel spriteModel;
    public TileDepthTexture depthTexture;
    public int depthFlags;
    private boolean hideForWaterRender;
    private Vector3f curtainOffset;
    private IsoSprite snowSprite;
    public static final int SDF_USE_OBJECT_DEPTH_TEXTURE = 1;
    public static final int SDF_TRANSLUCENT = 2;
    public static final int SDF_OPAQUE_PIXELS_ONLY = 4;
    public static TileSeamManager.Tiles seamFix2;
    public static boolean seamEast;
    public static final boolean SEAM_SOUTH = true;
    private static final AndThen AND_THEN;
    private boolean initRoofProperties;
    private RoofProperties roofProperties;

    public void setHideForWaterRender() {
        this.hideForWaterRender = true;
    }

    public IsoSprite() {
        this.parentManager = IsoSpriteManager.instance;
        this.def = IsoSpriteInstance.get(this);
    }

    public IsoSprite(IsoSpriteManager manager) {
        this.parentManager = manager;
        this.def = IsoSpriteInstance.get(this);
    }

    public static IsoSprite CreateSprite(IsoSpriteManager manager) {
        return new IsoSprite(manager);
    }

    public static IsoSprite CreateSpriteUsingCache(String objectName, String animName, int numFrames) {
        IsoSprite sprite = IsoSprite.CreateSprite(IsoSpriteManager.instance);
        return sprite.setFromCache(objectName, animName, numFrames);
    }

    public static IsoSprite getSprite(IsoSpriteManager manager, int id) {
        if (WorldConverter.instance.tilesetConversions != null && !WorldConverter.instance.tilesetConversions.isEmpty() && WorldConverter.instance.tilesetConversions.containsKey(id)) {
            id = WorldConverter.instance.tilesetConversions.get(id);
        }
        if (manager.intMap.containsKey(id)) {
            return manager.intMap.get(id);
        }
        return null;
    }

    public static void setSpriteID(IsoSpriteManager manager, int id, IsoSprite spr) {
        if (manager.intMap.containsKey(spr.id)) {
            manager.intMap.remove(spr.id);
            spr.id = id;
            manager.intMap.put(id, spr);
        }
    }

    public static IsoSprite getSprite(IsoSpriteManager manager, IsoSprite spr, int offset) {
        if (spr.name.contains("_")) {
            String[] split = spr.name.split("_");
            int id = Integer.parseInt(split[split.length - 1].trim());
            return manager.namedMap.get(spr.name.substring(0, spr.name.lastIndexOf("_")) + "_" + (id += offset));
        }
        return null;
    }

    public static IsoSprite getSprite(IsoSpriteManager manager, String name, int offset) {
        IsoSprite spr = manager.namedMap.get(name);
        if (spr == null) {
            return null;
        }
        if (offset == 0) {
            return spr;
        }
        if (spr.tilesetName == null) {
            if (spr.name.contains("_")) {
                String start = spr.name.substring(0, spr.name.lastIndexOf(95));
                String end = spr.name.substring(spr.name.lastIndexOf(95) + 1);
                int id = Integer.parseInt(end.trim());
                return manager.getSprite(start + "_" + (id += offset));
            }
            return null;
        }
        return manager.getSprite(spr.tilesetName + "_" + (spr.tileSheetIndex + offset));
    }

    public static void DisposeAll() {
        AnimNameSet.clear();
    }

    public static boolean HasCache(String string) {
        return AnimNameSet.containsKey(string);
    }

    public IsoSpriteInstance newInstance() {
        return IsoSpriteInstance.get(this);
    }

    public PropertyContainer getProperties() {
        return this.properties;
    }

    public boolean hasProperty(IsoPropertyType propertyType) {
        return this.getProperties().has(propertyType);
    }

    public boolean hasProperty(String propertyName) {
        return this.getProperties().has(propertyName);
    }

    public boolean hasProperty(IsoFlagType flag) {
        return this.getProperties().has(flag);
    }

    public String getParentObjectName() {
        return this.parentObjectName;
    }

    public void setParentObjectName(String val) {
        this.parentObjectName = val;
    }

    public void save(DataOutputStream output) throws IOException {
        GameWindow.WriteString(output, this.name);
    }

    public void load(DataInputStream input) throws IOException {
        this.name = GameWindow.ReadString(input);
        this.LoadFramesNoDirPageSimple(this.name);
    }

    public void Dispose() {
        this.disposeAnimation();
        this.texture = null;
    }

    private void allocateAnimationIfNeeded() {
        if (this.currentAnim == null) {
            this.currentAnim = new IsoAnim();
        }
        if (this.animMap == null) {
            this.animMap = new HashMap(2);
        }
        if (this.animStack == null) {
            this.animStack = new ArrayList(1);
        }
    }

    public void disposeAnimation() {
        if (this.animMap != null) {
            for (IsoAnim anim : this.animMap.values()) {
                anim.Dispose();
            }
            this.animMap = null;
        }
        if (this.animStack != null) {
            this.animStack.clear();
            this.animStack = null;
        }
        this.currentAnim = null;
    }

    public boolean isMaskClicked(IsoDirections dir, int x, int y) {
        try {
            Texture image = this.getTextureForCurrentFrame(dir);
            if (image == null) {
                return false;
            }
            Mask mask = image.getMask();
            if (mask == null) {
                return false;
            }
            x = (int)((float)x - image.offsetX);
            y = (int)((float)y - image.offsetY);
            return mask.get(x, y);
        }
        catch (Exception ex) {
            ExceptionLogger.logException(ex);
            return true;
        }
    }

    public boolean isMaskClicked(IsoDirections dir, int x, int y, boolean flip) {
        this.initSpriteInstance();
        try {
            Texture image = this.getTextureForCurrentFrame(dir);
            if (image == null) {
                return false;
            }
            Mask mask = image.getMask();
            if (mask == null) {
                return false;
            }
            if (flip) {
                x = (int)((float)x - ((float)(image.getWidthOrig() - image.getWidth()) - image.offsetX));
                y = (int)((float)y - image.offsetY);
                x = image.getWidth() - x;
            } else {
                x = (int)((float)x - image.offsetX);
                y = (int)((float)y - image.offsetY);
            }
            if (x < 0 || y < 0 || x > image.getWidth() || y > image.getHeight()) {
                return false;
            }
            return mask.get(x, y);
        }
        catch (Exception ex) {
            ExceptionLogger.logException(ex);
            return true;
        }
    }

    public float getMaskClickedY(IsoDirections dir, int x, int y, boolean flip) {
        try {
            Texture image = this.getTextureForCurrentFrame(dir);
            if (image == null) {
                return 10000.0f;
            }
            Mask mask = image.getMask();
            if (mask == null) {
                return 10000.0f;
            }
            y = (int)((float)y - image.offsetY);
            return y;
        }
        catch (Exception ex) {
            Logger.getLogger(GameWindow.class.getName()).log(Level.SEVERE, null, ex);
            return 10000.0f;
        }
    }

    public Texture LoadSingleTexture(String textureName) {
        this.disposeAnimation();
        this.texture = Texture.getSharedTexture(textureName);
        return this.texture;
    }

    public Texture LoadFrameExplicit(String objectName) {
        this.currentAnim = new IsoAnim();
        this.allocateAnimationIfNeeded();
        this.animMap.put("default", this.currentAnim);
        this.currentAnim.id = this.animStack.size();
        this.animStack.add(this.currentAnim);
        return this.currentAnim.LoadFrameExplicit(objectName);
    }

    public void LoadFrames(String objectName, String animName, int nFrames) {
        if (this.animMap != null && this.animMap.containsKey(animName)) {
            return;
        }
        this.currentAnim = new IsoAnim();
        this.allocateAnimationIfNeeded();
        this.animMap.put(animName, this.currentAnim);
        this.currentAnim.id = this.animStack.size();
        this.animStack.add(this.currentAnim);
        this.currentAnim.LoadFrames(objectName, animName, nFrames);
    }

    public void LoadFramesReverseAltName(String objectName, String animName, String altName, int nFrames) {
        if (this.animMap != null && this.animMap.containsKey(altName)) {
            return;
        }
        this.currentAnim = new IsoAnim();
        this.allocateAnimationIfNeeded();
        this.animMap.put(altName, this.currentAnim);
        this.currentAnim.id = this.animStack.size();
        this.animStack.add(this.currentAnim);
        this.currentAnim.LoadFramesReverseAltName(objectName, animName, altName, nFrames);
    }

    public void LoadFramesNoDirPage(String objectName, String animName, int nFrames) {
        this.currentAnim = new IsoAnim();
        this.allocateAnimationIfNeeded();
        this.animMap.put(animName, this.currentAnim);
        this.currentAnim.id = this.animStack.size();
        this.animStack.add(this.currentAnim);
        this.currentAnim.LoadFramesNoDirPage(objectName, animName, nFrames);
    }

    public void LoadFramesNoDirPageDirect(String objectName, String animName, int nFrames) {
        this.currentAnim = new IsoAnim();
        this.allocateAnimationIfNeeded();
        this.animMap.put(animName, this.currentAnim);
        this.currentAnim.id = this.animStack.size();
        this.animStack.add(this.currentAnim);
        this.currentAnim.LoadFramesNoDirPageDirect(objectName, animName, nFrames);
    }

    public void LoadFramesNoDirPageSimple(String objectName) {
        if (this.animMap != null && this.animMap.containsKey("default")) {
            IsoAnim anim = this.animMap.get("default");
            this.animStack.remove(anim);
            this.animMap.remove("default");
        }
        this.currentAnim = new IsoAnim();
        this.allocateAnimationIfNeeded();
        this.animMap.put("default", this.currentAnim);
        this.currentAnim.id = this.animStack.size();
        this.animStack.add(this.currentAnim);
        this.currentAnim.LoadFramesNoDirPage(objectName);
    }

    public void ReplaceCurrentAnimFrames(String objectName) {
        if (this.currentAnim == null) {
            this.texture = Texture.getSharedTexture(objectName);
            return;
        }
        this.currentAnim.frames.clear();
        this.currentAnim.LoadFramesNoDirPage(objectName);
    }

    public void LoadFramesPageSimple(String nObjectName, String sObjectName, String eObjectName, String wObjectName) {
        this.currentAnim = new IsoAnim();
        this.allocateAnimationIfNeeded();
        this.animMap.put("default", this.currentAnim);
        this.currentAnim.id = this.animStack.size();
        this.animStack.add(this.currentAnim);
        this.currentAnim.LoadFramesPageSimple(nObjectName, sObjectName, eObjectName, wObjectName);
    }

    public void PlayAnim(IsoAnim anim) {
        if (this.currentAnim == null || this.currentAnim != anim) {
            this.currentAnim = anim;
        }
    }

    public void PlayAnim(String name) {
        if ((this.currentAnim == null || !this.currentAnim.name.equals(name)) && this.animMap != null && this.animMap.containsKey(name)) {
            this.currentAnim = this.animMap.get(name);
        }
    }

    public void PlayAnimUnlooped(String name) {
        if (this.animMap == null) {
            return;
        }
        if (this.animMap.containsKey(name)) {
            if (this.currentAnim == null || !this.currentAnim.name.equals(name)) {
                this.currentAnim = this.animMap.get(name);
            }
            this.currentAnim.looped = false;
        }
    }

    public void ChangeTintMod(ColorInfo newTintMod) {
        this.tintMod.r = newTintMod.r;
        this.tintMod.g = newTintMod.g;
        this.tintMod.b = newTintMod.b;
        this.tintMod.a = newTintMod.a;
    }

    public void RenderGhostTile(int x, int y, int z) {
        this.RenderGhostTileColor(x, y, z, 1.0f, 1.0f, 1.0f, 0.6f);
    }

    public void RenderGhostTileRed(int x, int y, int z) {
        this.RenderGhostTileColor(x, y, z, 0.65f, 0.2f, 0.2f, 0.6f);
    }

    public void RenderGhostTileColor(int x, int y, int z, float r, float g, float b, float a) {
        this.RenderGhostTileColor(x, y, z, 0.0f, 0.0f, r, g, b, a);
    }

    public void RenderGhostTileColor(int x, int y, int z, float offsetX, float offsetY, float r, float g, float b, float a) {
        float offset;
        ColorInfo col;
        IsoObjectModelDrawer.RenderStatus renderStatus;
        if (this.spriteModel != null && ((renderStatus = IsoObjectModelDrawer.renderMain(this.spriteModel, (float)x + 0.5f, (float)y + 0.5f, z, col = new ColorInfo(r, g, b, 1.0f), offsetY / (float)Core.tileScale + (offset = this.getProperties().isSurfaceOffset() ? (float)this.getProperties().getSurface() : 0.0f), null, null, false, false)) == IsoObjectModelDrawer.RenderStatus.Loading || renderStatus == IsoObjectModelDrawer.RenderStatus.Ready)) {
            return;
        }
        IsoSpriteInstance spriteInstance = IsoSpriteInstance.get(this);
        spriteInstance.tintr = r;
        spriteInstance.tintg = g;
        spriteInstance.tintb = b;
        spriteInstance.alpha = spriteInstance.targetAlpha = a;
        IsoGridSquare.getDefColorInfo().set(1.0f, 1.0f, 1.0f, 1.0f);
        int scale = Core.tileScale;
        if (PerformanceSettings.fboRenderChunk) {
            FBORenderObjectHighlight.getInstance().setRenderingGhostTile(true);
            IndieGL.StartShader(0);
        }
        IndieGL.glDefaultBlendFunc();
        Texture texture = this.getTextureForCurrentFrame(IsoDirections.N);
        if (texture != null && texture.getName() != null && texture.getName().contains("JUMBO")) {
            this.render(spriteInstance, null, (float)x, (float)y, z, IsoDirections.N, (float)(96 * scale) + offsetX, (float)(224 * scale) + offsetY, IsoGridSquare.getDefColorInfo(), true);
        } else {
            this.render(spriteInstance, null, (float)x, (float)y, z, IsoDirections.N, (float)(32 * scale) + offsetX, (float)(96 * scale) + offsetY, IsoGridSquare.getDefColorInfo(), true);
        }
        if (PerformanceSettings.fboRenderChunk) {
            FBORenderObjectHighlight.getInstance().setRenderingGhostTile(false);
        }
        IsoSpriteInstance.add(spriteInstance);
    }

    public boolean hasActiveModel() {
        if (!ModelManager.instance.debugEnableModels) {
            return false;
        }
        if (!ModelManager.instance.isCreated()) {
            return false;
        }
        return this.modelSlot != null && this.modelSlot.active;
    }

    public void renderVehicle(IsoSpriteInstance inst, IsoObject obj, float x, float y, float z, float offsetX, float offsetY, ColorInfo info2, boolean bDoRenderPrep) {
        if (inst == null) {
            return;
        }
        if (this.hasActiveModel()) {
            SpriteRenderer.instance.drawGeneric(ModelCameraRenderData.s_pool.alloc().init(VehicleModelCamera.instance, this.modelSlot));
            SpriteRenderer.instance.drawModel(this.modelSlot);
            if (!BaseVehicle.renderToTexture) {
                return;
            }
        }
        IsoSprite.info.r = info2.r;
        IsoSprite.info.g = info2.g;
        IsoSprite.info.b = info2.b;
        IsoSprite.info.a = info2.a;
        try {
            if (bDoRenderPrep) {
                inst.renderprep(obj);
            }
            float sx = 0.0f;
            float sy = 0.0f;
            if (globalOffsetX == -1.0f) {
                globalOffsetX = -IsoCamera.frameState.offX;
                globalOffsetY = -IsoCamera.frameState.offY;
            }
            if (obj == null || obj.sx == 0.0f || obj instanceof IsoMovingObject) {
                sx = IsoUtils.XToScreen(x + inst.offX, y + inst.offY, z + inst.offZ, 0);
                sy = IsoUtils.YToScreen(x + inst.offX, y + inst.offY, z + inst.offZ, 0);
                sx -= offsetX;
                sy -= offsetY;
                if (obj != null) {
                    obj.sx = sx;
                    obj.sy = sy;
                }
            }
            if (obj != null) {
                sx = obj.sx + globalOffsetX;
                sy = obj.sy + globalOffsetY;
                sx += (float)this.soffX;
                sy += (float)this.soffY;
            } else {
                sx += globalOffsetX;
                sy += globalOffsetY;
                sx += (float)this.soffX;
                sy += (float)this.soffY;
            }
            if (bDoRenderPrep) {
                if (inst.tintr != 1.0f || inst.tintg != 1.0f || inst.tintb != 1.0f) {
                    IsoSprite.info.r *= inst.tintr;
                    IsoSprite.info.g *= inst.tintg;
                    IsoSprite.info.b *= inst.tintb;
                }
                IsoSprite.info.a = inst.alpha;
            }
            if (!(this.hasActiveModel() || this.tintMod.r == 1.0f && this.tintMod.g == 1.0f && this.tintMod.b == 1.0f)) {
                IsoSprite.info.r *= this.tintMod.r;
                IsoSprite.info.g *= this.tintMod.g;
                IsoSprite.info.b *= this.tintMod.b;
            }
            if (this.hasActiveModel()) {
                float scaleX = inst.getScaleX() * (float)Core.tileScale;
                float scaleY = -inst.getScaleY() * (float)Core.tileScale;
                float resized = 0.666f;
                int texW = ModelManager.instance.bitmap.getTexture().getWidth();
                int texH = ModelManager.instance.bitmap.getTexture().getHeight();
                sx -= (float)texW * (scaleX /= 2.664f) / 2.0f;
                sy -= (float)texH * (scaleY /= 2.664f) / 2.0f;
                float dy = ((BaseVehicle)obj).jniTransform.origin.y / 2.44949f;
                sy += 96.0f * dy / scaleY / 0.666f;
                sy += 27.84f / scaleY / 0.666f;
                if (SceneShaderStore.weatherShader != null && Core.getInstance().getOffscreenBuffer() != null) {
                    SpriteRenderer.instance.render((Texture)ModelManager.instance.bitmap.getTexture(), sx, sy, (float)texW * scaleX, (float)texH * scaleY, 1.0f, 1.0f, 1.0f, IsoSprite.info.a, null);
                } else {
                    SpriteRenderer.instance.render((Texture)ModelManager.instance.bitmap.getTexture(), sx, sy, (float)texW * scaleX, (float)texH * scaleY, IsoSprite.info.r, IsoSprite.info.g, IsoSprite.info.b, IsoSprite.info.a, null);
                }
                if (Core.debug && DebugOptions.instance.model.render.bounds.getValue()) {
                    LineDrawer.drawRect(sx, sy, (float)texW * scaleX, (float)texH * scaleY, 1.0f, 1.0f, 1.0f, 1.0f, 1);
                }
            }
            IsoSprite.info.r = 1.0f;
            IsoSprite.info.g = 1.0f;
            IsoSprite.info.b = 1.0f;
        }
        catch (Exception ex) {
            Logger.getLogger(GameWindow.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private IsoSpriteInstance getSpriteInstance() {
        this.initSpriteInstance();
        return this.def;
    }

    private void initSpriteInstance() {
        if (this.def == null) {
            this.def = IsoSpriteInstance.get(this);
        }
    }

    public final void render(IsoObject obj, float x, float y, float z, IsoDirections dir, float offsetX, float offsetY, ColorInfo info2, boolean bDoRenderPrep) {
        this.render(obj, x, y, z, dir, offsetX, offsetY, info2, bDoRenderPrep, null);
    }

    public final void render(IsoObject obj, float x, float y, float z, IsoDirections dir, float offsetX, float offsetY, ColorInfo info2, boolean bDoRenderPrep, Consumer<TextureDraw> texdModifier) {
        this.render(this.getSpriteInstance(), obj, x, y, z, dir, offsetX, offsetY, info2, bDoRenderPrep, texdModifier);
    }

    public final void renderDepth(IsoObject obj, IsoDirections isoDirections, boolean cutawayNW, boolean cutawayNE, boolean cutawaySW, int cutawaySEX, float x, float y, float z, float offsetX, float offsetY, ColorInfo info2, boolean bDoRenderPrep, Consumer<TextureDraw> texdModifier) {
        this.renderDepth(this.getSpriteInstance(), obj, isoDirections, cutawayNW, cutawayNE, cutawaySW, cutawaySEX, x, y, z, offsetX, offsetY, info2, bDoRenderPrep, texdModifier);
    }

    public final void render(IsoSpriteInstance inst, IsoObject obj, float x, float y, float z, IsoDirections dir, float offsetX, float offsetY, ColorInfo info2, boolean bDoRenderPrep) {
        this.render(inst, obj, x, y, z, dir, offsetX, offsetY, info2, bDoRenderPrep, null);
    }

    public void renderWallSliceW(IsoObject obj, float x, float y, float z, IsoDirections dir, float offsetX, float offsetY, ColorInfo info2, boolean bDoRenderPrep, Consumer<TextureDraw> texdModifier) {
        this.render(this.getSpriteInstance(), obj, x, y, z, dir, offsetX + (float)(32 * Core.tileScale), offsetY - (float)(16 * Core.tileScale), info2, bDoRenderPrep, WallShaperSliceW.instance);
    }

    public void renderWallSliceN(IsoObject obj, float x, float y, float z, IsoDirections dir, float offsetX, float offsetY, ColorInfo info2, boolean bDoRenderPrep, Consumer<TextureDraw> texdModifier) {
        this.render(this.getSpriteInstance(), obj, x, y, z, dir, offsetX - (float)(32 * Core.tileScale), offsetY - (float)(16 * Core.tileScale), info2, bDoRenderPrep, WallShaperSliceN.instance);
    }

    public void render(IsoSpriteInstance inst, IsoObject obj, float x, float y, float z, IsoDirections dir, float offsetX, float offsetY, ColorInfo info2, boolean bDoRenderPrep, Consumer<TextureDraw> texdModifier) {
        if (this.hasActiveModel()) {
            this.renderActiveModel();
        } else {
            this.renderCurrentAnim(inst, obj, x, y, z, dir, offsetX, offsetY, info2, bDoRenderPrep, texdModifier);
        }
    }

    public void renderDepth(IsoSpriteInstance inst, IsoObject obj, IsoDirections isoDirections, boolean cutawayNW, boolean cutawayNE, boolean cutawaySW, int cutawaySEX, float x, float y, float z, float offsetX, float offsetY, ColorInfo info2, boolean bDoRenderPrep, Consumer<TextureDraw> texdModifier) {
        this.renderCurrentAnimDepth(inst, obj, isoDirections, cutawayNW, cutawayNE, cutawaySW, cutawaySEX, x, y, z, offsetX, offsetY, info2, bDoRenderPrep, texdModifier);
    }

    public void renderCurrentAnim(IsoSpriteInstance inst, IsoObject obj, float x, float y, float z, IsoDirections dir, float offsetX, float offsetY, ColorInfo col, boolean bDoRenderPrep, Consumer<TextureDraw> texdModifier) {
        if (!DebugOptions.instance.isoSprite.renderSprites.getValue()) {
            return;
        }
        if (this.hasNoTextures()) {
            return;
        }
        float frame = this.getCurrentSpriteFrame(inst);
        info.set(col);
        if (WeatherFxMask.isRenderingMask() || FBORenderObjectHighlight.getInstance().isRenderingGhostTile() || FBORenderObjectOutline.getInstance().isRendering()) {
            IndieGL.disableDepthTest();
        } else if (PerformanceSettings.fboRenderChunk) {
            this.renderCurrentAnim_FBORender(inst, obj, x, y, z, dir, offsetX, offsetY, col, bDoRenderPrep, texdModifier);
            return;
        }
        Vector3 colorInfoBackup = l_renderCurrentAnim.colorInfoBackup.set(IsoSprite.info.r, IsoSprite.info.g, IsoSprite.info.b);
        Vector3 spritePos = l_renderCurrentAnim.spritePos.set(0.0f, 0.0f, 0.0f);
        this.prepareToRenderSprite(inst, obj, x, y, z, dir, offsetX, offsetY, bDoRenderPrep, (int)frame, spritePos);
        this.performRenderFrame(inst, obj, dir, (int)frame, spritePos.x, spritePos.y, 0.0f, texdModifier);
        IsoSprite.info.r = colorInfoBackup.x;
        IsoSprite.info.g = colorInfoBackup.y;
        IsoSprite.info.b = colorInfoBackup.z;
    }

    private void renderCurrentAnim_FBORender(IsoSpriteInstance inst, IsoObject obj, float x, float y, float z, IsoDirections dir, float offsetX, float offsetY, ColorInfo col, boolean bDoRenderPrep, Consumer<TextureDraw> texdModifier) {
        Vector3 colorInfoBackup;
        block28: {
            Vector3 spritePos;
            float frame;
            block30: {
                block33: {
                    block32: {
                        block31: {
                            Consumer<TextureDraw> mod;
                            block29: {
                                block27: {
                                    frame = this.getCurrentSpriteFrame(inst);
                                    colorInfoBackup = l_renderCurrentAnim.colorInfoBackup.set(IsoSprite.info.r, IsoSprite.info.g, IsoSprite.info.b);
                                    if (this.getProperties().has(IsoFlagType.unlit)) {
                                        IsoSprite.info.r = 1.0f;
                                        IsoSprite.info.g = 1.0f;
                                        IsoSprite.info.b = 1.0f;
                                    }
                                    spritePos = l_renderCurrentAnim.spritePos.set(0.0f, 0.0f, 0.0f);
                                    this.prepareToRenderSprite(inst, obj, x, y, z, dir, offsetX, offsetY, bDoRenderPrep, (int)frame, spritePos);
                                    if (DebugOptions.instance.fboRenderChunk.depthTestAll.getValue()) {
                                        IndieGL.enableDepthTest();
                                        IndieGL.glDepthFunc(515);
                                    } else {
                                        IndieGL.enableDepthTest();
                                        IndieGL.glDepthFunc(519);
                                    }
                                    if (texdModifier != CutawayAttachedModifier.instance) break block27;
                                    IndieGL.enableDepthTest();
                                    IndieGL.glDepthFunc(519);
                                    CutawayAttachedModifier.instance.setSprite(this);
                                    this.performRenderFrame(inst, obj, dir, (int)frame, spritePos.x, spritePos.y, spritePos.z, texdModifier);
                                    break block28;
                                }
                                if (dir != IsoDirections.NW) break block29;
                                if (texdModifier instanceof WallShaper && this.setupTileDepthWall(obj, dir, x, y, z, true)) {
                                    Consumer<TextureDraw> mod0 = seamFix2 == null ? TileDepthModifier.instance : TileSeamModifier.instance;
                                    Consumer<TextureDraw> mod2 = seamFix2 == null ? AND_THEN.set(mod0, texdModifier) : mod0;
                                    this.performRenderFrame(inst, obj, dir, (int)frame, spritePos.x, spritePos.y, spritePos.z, mod2);
                                }
                                break block28;
                            }
                            if (texdModifier == WallShaperWhole.instance || texdModifier == WallShaperSliceW.instance || texdModifier == WallShaperSliceN.instance) break block30;
                            float z2 = z;
                            if (obj != null && obj.getRenderYOffset() != 0.0f) {
                                z2 += obj.getRenderYOffset() / 96.0f;
                            }
                            if (!this.setupTileDepth(obj, x, y, z, z2, true)) break block31;
                            TileDepthModifier.instance.setSpriteScale(this.def.scaleX, this.def.scaleY);
                            Consumer<TextureDraw> mod0 = seamFix2 == null ? TileDepthModifier.instance : TileSeamModifier.instance;
                            Consumer<TextureDraw> consumer = mod = texdModifier == null || seamFix2 != null ? mod0 : AND_THEN.set(mod0, texdModifier);
                            if (FBORenderCell.instance.renderTranslucentOnly) {
                                IndieGL.glDepthMask(false);
                                IndieGL.enableDepthTest();
                            }
                            this.performRenderFrame(inst, obj, dir, (int)frame, spritePos.x, spritePos.y, spritePos.z, mod);
                            break block28;
                        }
                        if (!(obj instanceof IsoTree)) break block32;
                        IsoTree tree = (IsoTree)obj;
                        if (tree.useTreeShader) break block33;
                    }
                    IndieGL.StartShader(0);
                }
                if (FBORenderCell.instance.renderTranslucentOnly) {
                    int chunksPerWidth = 8;
                    IsoDepthHelper.Results result = IsoDepthHelper.getChunkDepthData(PZMath.fastfloor(IsoCamera.frameState.camCharacterX / 8.0f), PZMath.fastfloor(IsoCamera.frameState.camCharacterY / 8.0f), PZMath.fastfloor(x / 8.0f), PZMath.fastfloor(y / 8.0f), PZMath.fastfloor(z));
                    TextureDraw.nextChunkDepth = result.depthStart;
                    if (this.soffY != 0 && !(obj instanceof IsoFire) && !(obj instanceof IsoGameCharacter)) {
                        TextureDraw.nextChunkDepth += (float)this.soffY / 96.0f * 0.0028867084f;
                    }
                    TextureDraw.nextChunkDepth += 0.5f;
                    TextureDraw.nextChunkDepth = TextureDraw.nextChunkDepth * 2.0f - 1.0f;
                    IndieGL.glDepthMask(false);
                    IndieGL.enableDepthTest();
                    if (obj instanceof IsoTree) {
                        IsoTree isoTree = (IsoTree)obj;
                        if (isoTree.useTreeShader) {
                            IsoTree.TreeShader.instance.setDepth(TextureDraw.nextChunkDepth, spritePos.z * 2.0f - 1.0f);
                        }
                    }
                    if (obj instanceof IsoTree) {
                        IndieGL.glDepthMask(true);
                    }
                } else if (!FBORenderChunkManager.instance.isCaching()) {
                    int chunksPerWidth = 8;
                    spritePos.z += IsoDepthHelper.getChunkDepthData((int)PZMath.fastfloor((float)(IsoCamera.frameState.camCharacterX / 8.0f)), (int)PZMath.fastfloor((float)(IsoCamera.frameState.camCharacterY / 8.0f)), (int)PZMath.fastfloor((float)(x / 8.0f)), (int)PZMath.fastfloor((float)(y / 8.0f)), (int)PZMath.fastfloor((float)z)).depthStart;
                }
                this.performRenderFrame(inst, obj, dir, (int)frame, spritePos.x, spritePos.y, spritePos.z * 2.0f - 1.0f, texdModifier);
                break block28;
            }
            if (texdModifier == WallShaperWhole.instance || texdModifier == WallShaperSliceW.instance || texdModifier == WallShaperSliceN.instance) {
                if (dir != null) {
                    Consumer<TextureDraw> mod0;
                    Consumer<TextureDraw> consumer = mod0 = seamFix2 == null ? TileDepthModifier.instance : TileSeamModifier.instance;
                    if (texdModifier == WallShaperSliceW.instance || texdModifier == WallShaperSliceN.instance) {
                        boolean dy;
                        int dox = 0;
                        int doy = 0;
                        boolean dx = texdModifier == WallShaperSliceN.instance;
                        boolean bl = dy = texdModifier == WallShaperSliceW.instance;
                        if (this.setupTileDepthWall2(dir, obj.square.x + dox, obj.square.y + doy, x + (float)dx, y + (float)dy, z, true)) {
                            AndThen mod = AND_THEN.set(mod0, texdModifier);
                            this.performRenderFrame(inst, obj, dir, (int)frame, spritePos.x, spritePos.y, spritePos.z, mod);
                        }
                    } else if (this.getParentSpriteDepthTextureToUse(obj) != null) {
                        TileDepthModifier.instance.setupTileDepthTexture(this, this.getParentSpriteDepthTextureToUse(obj));
                        this.startTileDepthShader(obj, x, y, z, z, true);
                        Consumer<TextureDraw> mod = seamFix2 == null ? AND_THEN.set(mod0, texdModifier) : mod0;
                        this.performRenderFrame(inst, obj, dir, (int)frame, spritePos.x, spritePos.y, spritePos.z, mod);
                    } else if (this.setupTileDepthWall(obj, dir, x, y, z, true)) {
                        Consumer<TextureDraw> mod = seamFix2 == null ? AND_THEN.set(mod0, texdModifier) : mod0;
                        this.performRenderFrame(inst, obj, dir, (int)frame, spritePos.x, spritePos.y, spritePos.z, mod);
                    }
                } else {
                    this.performRenderFrame(inst, obj, dir, (int)frame, spritePos.x, spritePos.y, spritePos.z, texdModifier);
                }
            }
        }
        IsoSprite.info.r = colorInfoBackup.x;
        IsoSprite.info.g = colorInfoBackup.y;
        IsoSprite.info.b = colorInfoBackup.z;
        if (obj != null && FBORenderChunkManager.instance.isCaching()) {
            obj.sx = 0.0f;
        }
    }

    public void renderCurrentAnimDepth(IsoSpriteInstance inst, IsoObject obj, IsoDirections dir, boolean cutawayNW, boolean cutawayNE, boolean cutawaySW, int cutawaySEX, float x, float y, float z, float offsetX, float offsetY, ColorInfo col, boolean bDoRenderPrep, Consumer<TextureDraw> texdModifier) {
        if (!DebugOptions.instance.isoSprite.renderSprites.getValue()) {
            return;
        }
        Texture texture1 = this.getTextureForCurrentFrame(dir);
        if (texture1 == null) {
            return;
        }
        float frame = this.getCurrentSpriteFrame(inst);
        info.set(col);
        if (this.getProperties().has(IsoFlagType.unlit)) {
            IsoSprite.info.r = 1.0f;
            IsoSprite.info.g = 1.0f;
            IsoSprite.info.b = 1.0f;
        }
        Vector3 colorInfoBackup = l_renderCurrentAnim.colorInfoBackup.set(IsoSprite.info.r, IsoSprite.info.g, IsoSprite.info.b);
        Vector3 spritePos = l_renderCurrentAnim.spritePos.set(0.0f, 0.0f, 0.0f);
        this.prepareToRenderSprite(inst, obj, x, y, z, dir, offsetX, offsetY, bDoRenderPrep, (int)frame, spritePos);
        if (this.setupTileDepthWall(obj, dir, x, y, z, false)) {
            this.performRenderFrame(inst, obj, dir, (int)frame, spritePos.x, spritePos.y, spritePos.z, TileDepthModifier.instance);
        }
        IsoSprite.info.r = colorInfoBackup.x;
        IsoSprite.info.g = colorInfoBackup.y;
        IsoSprite.info.b = colorInfoBackup.z;
    }

    private boolean setupTileDepth(IsoObject obj, float x, float y, float z, float z2, boolean drawPixels) {
        IsoFireplace fireplace;
        if (obj instanceof IsoTree) {
            return false;
        }
        if (obj instanceof IsoBarbecue && obj.sprite != null && this != obj.sprite) {
            return false;
        }
        if (obj instanceof IsoFire && this != obj.sprite) {
            TileDepthTexture billboardDepthTexture = TileDepthTextureManager.getInstance().getBillboardDepthTexture();
            if (billboardDepthTexture != null && !billboardDepthTexture.isEmpty()) {
                TileDepthModifier.instance.setupTileDepthTexture(this, billboardDepthTexture);
                this.startTileDepthShader(obj, x - 0.5f, y - 0.5f, z, z2, drawPixels);
                return true;
            }
            return false;
        }
        if (obj instanceof IsoCarBatteryCharger) {
            return false;
        }
        if (obj instanceof IsoMolotovCocktail) {
            return false;
        }
        if (obj instanceof IsoTrap && this.texture != null && this.texture.getName() != null && this.texture.getName().startsWith("Item_")) {
            return false;
        }
        if (obj instanceof IsoWorldInventoryObject) {
            return false;
        }
        if (obj instanceof IsoZombieGiblets) {
            return false;
        }
        if (obj instanceof IsoGameCharacter) {
            return false;
        }
        if (this.depthTexture != null && !this.depthTexture.isEmpty()) {
            if (seamFix2 == null) {
                TileDepthModifier.instance.setupTileDepthTexture(this, this.depthTexture);
            } else if (seamFix2 == TileSeamManager.Tiles.FloorSouthOneThird || seamFix2 == TileSeamManager.Tiles.FloorEastOneThird || seamFix2 == TileSeamManager.Tiles.FloorSouthTwoThirds || seamFix2 == TileSeamManager.Tiles.FloorEastTwoThirds) {
                TileSeamModifier.instance.setupFloorDepth(this, seamFix2, this.depthTexture);
            } else if (seamFix2 == TileSeamManager.Tiles.FloorSouth || seamFix2 == TileSeamManager.Tiles.FloorEast) {
                TileSeamModifier.instance.setupFloorDepth(this, seamFix2, this.depthTexture);
            }
            this.startTileDepthShader(obj, x, y, z, z2, drawPixels);
            return true;
        }
        if (this.getProperties().has(IsoFlagType.solidfloor) || this.getProperties().has(IsoFlagType.FloorOverlay) || this.renderLayer == 1) {
            if (seamFix2 == null) {
                TileDepthModifier.instance.setupFloorDepth(this);
            } else {
                TileSeamModifier.instance.setupFloorDepth(this, seamFix2);
            }
            this.startTileDepthShader(obj, x, y, z, z2, drawPixels);
            return true;
        }
        TileDepthTexture objectDepthTexture = this.getParentSpriteDepthTextureToUse(obj);
        if (objectDepthTexture != null) {
            TileDepthModifier.instance.setupTileDepthTexture(this, objectDepthTexture);
            this.startTileDepthShader(obj, x, y, z, z2, drawPixels);
            return true;
        }
        if (this.getProperties().has(IsoFlagType.windowN) || obj instanceof IsoWindow && obj.getSprite().getProperties().has(IsoFlagType.windowN)) {
            TileDepthModifier.instance.setupWallDepth(this, IsoDirections.N);
            this.startTileDepthShader(obj, x, y, z, z2, drawPixels);
            return true;
        }
        if (this.getProperties().has(IsoFlagType.windowW) || obj instanceof IsoWindow && obj.getSprite().getProperties().has(IsoFlagType.windowW)) {
            TileDepthModifier.instance.setupWallDepth(this, IsoDirections.W);
            this.startTileDepthShader(obj, x, y, z, z2, drawPixels);
            return true;
        }
        if (this.getProperties().has(IsoFlagType.WallOverlay) && this.getProperties().has(IsoFlagType.attachedW)) {
            TileDepthModifier.instance.setupWallDepth(this, IsoDirections.W);
            this.startTileDepthShader(obj, x, y, z, z2, drawPixels);
            return true;
        }
        if (this.getProperties().has(IsoFlagType.WallOverlay) && this.getProperties().has(IsoFlagType.attachedN)) {
            TileDepthModifier.instance.setupWallDepth(this, IsoDirections.N);
            this.startTileDepthShader(obj, x, y, z, z2, drawPixels);
            return true;
        }
        if (obj instanceof IsoFireplace && (fireplace = (IsoFireplace)obj).isFireSpriteUsingOurDepthTexture() && obj.sprite != null && this != obj.sprite && obj.sprite.depthTexture != null) {
            TileDepthModifier.instance.setupTileDepthTexture(this, obj.sprite.depthTexture);
            this.startTileDepthShader(obj, x, y, z, z2, drawPixels);
            return true;
        }
        TileDepthTexture defaultDepthTexture = TileDepthTextureManager.getInstance().getDefaultDepthTexture();
        if (defaultDepthTexture != null && !defaultDepthTexture.isEmpty()) {
            TileDepthModifier.instance.setupTileDepthTexture(this, defaultDepthTexture);
            this.startTileDepthShader(obj, x, y, z, z2, drawPixels);
            return true;
        }
        return false;
    }

    private boolean setupTileDepthWall(IsoObject obj, IsoDirections isoDirections, float x, float y, float z, boolean drawPixels) {
        if (this.depthTexture != null && !this.depthTexture.isEmpty()) {
            TileDepthModifier.instance.setupTileDepthTexture(this, this.depthTexture);
            this.startTileDepthShader(obj, x, y, z, z, drawPixels);
            if (seamFix2 != null) {
                TileSeamModifier.instance.setupWallDepth(this, isoDirections);
            }
            return true;
        }
        if (seamFix2 == null) {
            TileDepthTexture objectDepthTexture = this.getParentSpriteDepthTextureToUse(obj);
            if (objectDepthTexture != null) {
                TileDepthModifier.instance.setupTileDepthTexture(this, objectDepthTexture);
                this.startTileDepthShader(obj, x, y, z, z, drawPixels);
                return true;
            }
            IsoSprite sprite = this;
            if (obj != null && obj.getSprite() != null && obj.getSprite() != this && (this.depthFlags & 1) != 0) {
                sprite = obj.getSprite();
            }
            TileDepthModifier.instance.setupWallDepth(sprite, isoDirections);
        } else {
            TileSeamModifier.instance.setupWallDepth(this, isoDirections);
        }
        this.startTileDepthShader(obj, x, y, z, z, drawPixels);
        return true;
    }

    private void startTileDepthShader(IsoObject obj, float x, float y, float z, float z2, boolean drawPixels) {
        ShaderUniformSetter uniforms;
        if (FBORenderCell.instance.renderTranslucentOnly) {
            int playerIndex = IsoCamera.frameState.playerIndex;
            PlayerCamera camera = IsoCamera.cameras[playerIndex];
            x += camera.fixJigglyModelsSquareX;
            y += camera.fixJigglyModelsSquareY;
        }
        int chunksPerWidth = 8;
        float farDepthZ = IsoDepthHelper.getSquareDepthData((int)PZMath.fastfloor((float)IsoCamera.frameState.camCharacterX), (int)PZMath.fastfloor((float)IsoCamera.frameState.camCharacterY), (float)x, (float)y, (float)z).depthStart;
        float zPlusOne = z + 1.0f;
        float frontDepthZ = IsoDepthHelper.getSquareDepthData((int)PZMath.fastfloor((float)IsoCamera.frameState.camCharacterX), (int)PZMath.fastfloor((float)IsoCamera.frameState.camCharacterY), (float)(x + 1.0f), (float)(y + 1.0f), (float)zPlusOne).depthStart;
        if (z != z2) {
            farDepthZ -= (z2 - z) * 0.0028867084f;
            frontDepthZ -= (z2 - z) * 0.0028867084f;
        }
        if (FBORenderCell.instance.renderTranslucentOnly) {
            IndieGL.glDepthMask(false);
            IndieGL.enableDepthTest();
            IndieGL.glDepthFunc(515);
            if (FBORenderObjectHighlight.getInstance().isRendering()) {
                frontDepthZ -= 5.0E-5f;
                farDepthZ -= 5.0E-5f;
            } else if (FBORenderCell.instance.renderAnimatedAttachments && obj != null && obj.getOnOverlay() != null && this == obj.getOnOverlay().getParentSprite()) {
                frontDepthZ -= 5.0E-5f;
                farDepthZ -= 5.0E-5f;
            } else if (obj instanceof IsoFireplace && this != obj.getSprite()) {
                frontDepthZ -= 5.0E-5f;
                farDepthZ -= 5.0E-5f;
            }
        } else {
            int chunkX = PZMath.fastfloor(x / 8.0f);
            int chunkY = PZMath.fastfloor(y / 8.0f);
            int level = PZMath.fastfloor(z);
            if (obj != null && obj.renderSquareOverride != null) {
                chunkX = obj.renderSquareOverride.chunk.wx;
                chunkY = obj.renderSquareOverride.chunk.wy;
                level = obj.renderSquareOverride.z;
            } else if (obj != null && obj.renderSquareOverride2 != null) {
                chunkX = PZMath.fastfloor((float)obj.square.x / 8.0f);
                chunkY = PZMath.fastfloor((float)obj.square.y / 8.0f);
            }
            IsoDepthHelper.Results resultFar = IsoDepthHelper.getChunkDepthData(PZMath.fastfloor(IsoCamera.frameState.camCharacterX / 8.0f), PZMath.fastfloor(IsoCamera.frameState.camCharacterY / 8.0f), chunkX, chunkY, level);
            float chunkDepth = resultFar.depthStart;
            if (obj != null && obj.renderDepthAdjust != 0.0f) {
                chunkDepth += obj.renderDepthAdjust;
            }
            farDepthZ -= chunkDepth;
            frontDepthZ -= chunkDepth;
        }
        float zDepthBlendZ = frontDepthZ;
        float zDepthBlendToZ = farDepthZ;
        if (seamFix2 != null) {
            TileSeamShader shader = SceneShaderStore.tileSeamShader;
            uniforms = ShaderUniformSetter.uniform1f(shader, "zDepth", frontDepthZ);
            uniforms.setNext(ShaderUniformSetter.uniform1i(shader, "drawPixels", drawPixels ? 1 : 0)).setNext(ShaderUniformSetter.uniform1f(shader, "zDepthBlendZ", zDepthBlendZ)).setNext(ShaderUniformSetter.uniform1f(shader, "zDepthBlendToZ", zDepthBlendToZ));
            IndieGL.StartShader(SceneShaderStore.tileSeamShader.getID(), uniforms);
            return;
        }
        TileDepthShader shader = SceneShaderStore.tileDepthShader;
        if ((this.depthFlags & 4) != 0) {
            shader = SceneShaderStore.opaqueDepthShader;
        }
        uniforms = ShaderUniformSetter.uniform1f(shader, "zDepth", frontDepthZ);
        uniforms.setNext(ShaderUniformSetter.uniform1i(shader, "drawPixels", drawPixels ? 1 : 0)).setNext(ShaderUniformSetter.uniform1f(shader, "zDepthBlendZ", zDepthBlendZ)).setNext(ShaderUniformSetter.uniform1f(shader, "zDepthBlendToZ", zDepthBlendToZ));
        IndieGL.StartShader(shader.getID(), uniforms);
    }

    private boolean setupTileDepthWall2(IsoDirections isoDirections, int objX, int objY, float x, float y, float z, boolean drawPixels) {
        if (this.depthTexture != null && !this.depthTexture.isEmpty()) {
            TileDepthModifier.instance.setupTileDepthTexture(this, this.depthTexture);
            this.startTileDepthShader2(objX, objY, x, y, z, z, drawPixels);
            return true;
        }
        TileDepthModifier.instance.setupWallDepth(this, isoDirections);
        this.startTileDepthShader2(objX, objY, x, y, z, z, drawPixels);
        return true;
    }

    private void startTileDepthShader2(int objX, int objY, float x, float y, float z, float z2, boolean drawPixels) {
        int chunksPerWidth = 8;
        float farDepthZ = IsoDepthHelper.getSquareDepthData((int)PZMath.fastfloor((float)IsoCamera.frameState.camCharacterX), (int)PZMath.fastfloor((float)IsoCamera.frameState.camCharacterY), (float)x, (float)y, (float)z).depthStart;
        float zPlusOne = z + 1.0f;
        float frontDepthZ = IsoDepthHelper.getSquareDepthData((int)PZMath.fastfloor((float)IsoCamera.frameState.camCharacterX), (int)PZMath.fastfloor((float)IsoCamera.frameState.camCharacterY), (float)(x + 1.0f), (float)(y + 1.0f), (float)zPlusOne).depthStart;
        if (z != z2) {
            farDepthZ -= (z2 - z) * 0.0028867084f;
            frontDepthZ -= (z2 - z) * 0.0028867084f;
        }
        if (FBORenderCell.instance.renderTranslucentOnly) {
            IndieGL.glDepthMask(false);
            IndieGL.enableDepthTest();
            IndieGL.glDepthFunc(515);
        } else {
            IsoDepthHelper.Results resultFar = IsoDepthHelper.getChunkDepthData(PZMath.fastfloor(IsoCamera.frameState.camCharacterX / 8.0f), PZMath.fastfloor(IsoCamera.frameState.camCharacterY / 8.0f), PZMath.fastfloor(objX / 8), PZMath.fastfloor(objY / 8), PZMath.fastfloor(z));
            float chunkDepth = resultFar.depthStart;
            farDepthZ -= (chunkDepth -= 1.0E-4f);
            frontDepthZ -= chunkDepth;
        }
        float zDepthBlendZ = frontDepthZ;
        float zDepthBlendToZ = farDepthZ;
        IndieGL.StartShader(SceneShaderStore.tileDepthShader.getID());
        IndieGL.shaderSetValue((Shader)SceneShaderStore.tileDepthShader, "zDepth", frontDepthZ);
        IndieGL.shaderSetValue((Shader)SceneShaderStore.tileDepthShader, "drawPixels", drawPixels ? 1 : 0);
        IndieGL.shaderSetValue((Shader)SceneShaderStore.tileDepthShader, "zDepthBlendZ", zDepthBlendZ);
        IndieGL.shaderSetValue((Shader)SceneShaderStore.tileDepthShader, "zDepthBlendToZ", zDepthBlendToZ);
    }

    public static void renderTextureWithDepth(Texture texture, float width, float height, float r, float g, float b, float a, float x, float y, float z) {
        int playerIndex = IsoCamera.frameState.playerIndex;
        PlayerCamera camera = IsoCamera.cameras[playerIndex];
        x += camera.fixJigglyModelsSquareX;
        y += camera.fixJigglyModelsSquareY;
        if (FBORenderCell.instance.renderTranslucentOnly) {
            int chunksPerWidth = 8;
            IsoDepthHelper.Results result = IsoDepthHelper.getChunkDepthData(PZMath.fastfloor(IsoCamera.frameState.camCharacterX / 8.0f), PZMath.fastfloor(IsoCamera.frameState.camCharacterY / 8.0f), PZMath.fastfloor(x / 8.0f), PZMath.fastfloor(y / 8.0f), PZMath.fastfloor(z));
            TextureDraw.nextChunkDepth = result.depthStart;
            TextureDraw.nextChunkDepth += 0.5f;
            TextureDraw.nextChunkDepth = TextureDraw.nextChunkDepth * 2.0f - 1.0f;
        }
        TextureDraw.nextZ = IsoDepthHelper.calculateDepth(x, y, z) * 2.0f - 1.0f;
        IndieGL.StartShader(0);
        float screenX = IsoUtils.XToScreen(x, y, z, 0) - camera.getOffX() - width / 2.0f;
        float screenY = IsoUtils.YToScreen(x, y, z, 0) - camera.getOffY() - height;
        SpriteRenderer.instance.render(texture, screenX, screenY, width, height, r, g, b, a, null);
        IndieGL.EndShader();
    }

    private TileDepthTexture getParentSpriteDepthTextureToUse(IsoObject obj) {
        if (obj == null) {
            return null;
        }
        IsoSprite sprite = obj.getSprite();
        if (this == sprite) {
            return null;
        }
        boolean bUseObjectDepthTexture = false;
        if ((this.depthFlags & 1) != 0) {
            bUseObjectDepthTexture = true;
        } else if (this.getProperties().has(IsoFlagType.WallOverlay) && this.getProperties().has(IsoFlagType.attachedN)) {
            bUseObjectDepthTexture = true;
        } else if (this.getProperties().has(IsoFlagType.WallOverlay) && this.getProperties().has(IsoFlagType.attachedW)) {
            bUseObjectDepthTexture = true;
        }
        if (bUseObjectDepthTexture && sprite != null && sprite.depthTexture != null && !sprite.depthTexture.isEmpty()) {
            return sprite.depthTexture;
        }
        return null;
    }

    public boolean hasAnimation() {
        return this.currentAnim != null;
    }

    public int getFrameCount() {
        return this.currentAnim == null ? 1 : this.currentAnim.frames.size();
    }

    public boolean hasNoTextures() {
        if (this.currentAnim == null) {
            return this.texture == null;
        }
        return this.currentAnim.hasNoTextures();
    }

    private float getCurrentSpriteFrame(IsoSpriteInstance inst) {
        float frame;
        if (!this.hasAnimation()) {
            inst.frame = 0.0f;
            return 0.0f;
        }
        if (this.currentAnim.framesArray == null) {
            this.currentAnim.framesArray = this.currentAnim.frames.toArray(new IsoDirectionFrame[0]);
        }
        if (this.currentAnim.framesArray.length != this.currentAnim.frames.size()) {
            this.currentAnim.framesArray = this.currentAnim.frames.toArray(this.currentAnim.framesArray);
        }
        if (inst.frame >= (float)this.currentAnim.frames.size()) {
            frame = this.currentAnim.framesArray.length - 1;
        } else if (inst.frame < 0.0f) {
            inst.frame = 0.0f;
            frame = 0.0f;
        } else {
            frame = inst.frame;
        }
        return frame;
    }

    private void prepareToRenderSprite(IsoSpriteInstance inst, IsoObject obj, float x, float y, float z, IsoDirections dir, float offsetX, float offsetY, boolean bDoRenderPrep, int frame, Vector3 spritePos) {
        float ox = x;
        float oy = y;
        if (bDoRenderPrep) {
            inst.renderprep(obj);
        }
        float sx = 0.0f;
        float sy = 0.0f;
        if (globalOffsetX == -1.0f) {
            globalOffsetX = -IsoCamera.frameState.offX;
            globalOffsetY = -IsoCamera.frameState.offY;
        }
        float goX = globalOffsetX;
        float goY = globalOffsetY;
        if (FBORenderChunkManager.instance.isCaching()) {
            goX = FBORenderChunkManager.instance.getXOffset();
            goY = FBORenderChunkManager.instance.getYOffset();
            x = PZMath.coordmodulof(x, 8);
            y = PZMath.coordmodulof(y, 8);
            if (obj != null && obj.getSquare() != obj.getRenderSquare()) {
                x = ox - (float)(obj.getRenderSquare().chunk.wx * 8);
                y = oy - (float)(obj.getRenderSquare().chunk.wy * 8);
            } else if (obj != null && obj.renderSquareOverride2 != null) {
                x = ox - (float)(obj.getSquare().chunk.wx * 8);
                y = oy - (float)(obj.getSquare().chunk.wy * 8);
            }
            if (obj != null) {
                obj.sx = 0.0f;
            }
        }
        if (obj == null || obj.sx == 0.0f || obj instanceof IsoMovingObject) {
            sx = IsoUtils.XToScreen(x + inst.offX, y + inst.offY, z + inst.offZ, 0);
            sy = IsoUtils.YToScreen(x + inst.offX, y + inst.offY, z + inst.offZ, 0);
            sx -= offsetX;
            sy -= offsetY;
            if (obj != null) {
                obj.sx = sx;
                obj.sy = sy;
            }
            sx += goX;
            sy += goY;
            sx += (float)this.soffX;
            sy += (float)this.soffY;
        } else if (obj != null) {
            sx = obj.sx + goX;
            sy = obj.sy + goY;
            sx += (float)this.soffX;
            sy += (float)this.soffY;
        } else {
            sx += goX;
            sy += goY;
            sx += (float)this.soffX;
            sy += (float)this.soffY;
        }
        if (PerformanceSettings.fboRenderChunk && !FBORenderChunkManager.instance.isCaching()) {
            int playerIndex = IsoCamera.frameState.playerIndex;
            float zoom = IsoCamera.frameState.zoom;
            sx += IsoCamera.cameras[playerIndex].fixJigglyModelsX * zoom;
            sy += IsoCamera.cameras[playerIndex].fixJigglyModelsY * zoom;
        }
        if (obj instanceof IsoMovingObject) {
            Texture texture1 = this.getTextureForFrame(frame, dir, obj.isUseSnowSprite());
            sx -= (float)texture1.getWidthOrig() / 2.0f * inst.getScaleX();
            sy -= (float)texture1.getHeightOrig() * inst.getScaleY();
        }
        if (bDoRenderPrep) {
            if (inst.tintr != 1.0f || inst.tintg != 1.0f || inst.tintb != 1.0f) {
                IsoSprite.info.r *= inst.tintr;
                IsoSprite.info.g *= inst.tintg;
                IsoSprite.info.b *= inst.tintb;
            }
            IsoSprite.info.a = inst.alpha;
            if (inst.multiplyObjectAlpha && obj != null) {
                IsoSprite.info.a *= obj.getAlpha(IsoCamera.frameState.playerIndex);
            }
        }
        if (this.tintMod.r != 1.0f || this.tintMod.g != 1.0f || this.tintMod.b != 1.0f) {
            IsoSprite.info.r *= this.tintMod.r;
            IsoSprite.info.g *= this.tintMod.g;
            IsoSprite.info.b *= this.tintMod.b;
        }
        if (PerformanceSettings.fboRenderChunk && !WeatherFxMask.isRenderingMask() && !FBORenderObjectHighlight.getInstance().isRenderingGhostTile()) {
            if (obj != null && obj.square == IsoPlayer.getInstance().getCurrentSquare()) {
                boolean texture1 = false;
            }
            float sz = IsoSprite.calculateDepth(x, y, z);
            if (obj instanceof IsoTree) {
                sz = IsoSprite.calculateDepth(x + 0.99f, y + 0.99f, z);
                if (!FBORenderCell.instance.renderTranslucentOnly && obj.getSquare() != obj.getRenderSquare()) {
                    int chunksPerWidth = 8;
                    sz = IsoDepthHelper.getSquareDepthData((int)PZMath.fastfloor((float)IsoCamera.frameState.camCharacterX), (int)PZMath.fastfloor((float)IsoCamera.frameState.camCharacterY), (float)(ox + 0.99f), (float)(oy + 0.99f), (float)z).depthStart;
                    sz -= IsoDepthHelper.getChunkDepthData((int)PZMath.fastfloor((float)(IsoCamera.frameState.camCharacterX / 8.0f)), (int)PZMath.fastfloor((float)(IsoCamera.frameState.camCharacterY / 8.0f)), (int)PZMath.fastfloor((float)((float)obj.getRenderSquare().x / 8.0f)), (int)PZMath.fastfloor((float)((float)obj.getRenderSquare().y / 8.0f)), (int)PZMath.fastfloor((float)z)).depthStart;
                }
            }
            if (obj != null && obj.square != null && obj.square.getWater() != null && obj.square.getWater().isValid()) {
                sz = IsoSprite.calculateDepth(x + 0.99f, y + 0.99f, z);
            }
            if (obj instanceof IsoCarBatteryCharger || obj instanceof IsoTrap || obj instanceof IsoWorldInventoryObject) {
                sz = IsoSprite.calculateDepth(x + 0.25f, y + 0.25f, z);
            }
            spritePos.set(sx, sy, sz);
            return;
        }
        spritePos.set(sx, sy, 0.0f);
    }

    public static float calculateDepth(float x, float y, float z) {
        return IsoDepthHelper.calculateDepth(x, y, z);
    }

    private void performRenderFrame(IsoSpriteInstance inst, IsoObject obj, IsoDirections dir, int frame, float tx, float ty, float tdepth, Consumer<TextureDraw> texdModifier) {
        Texture tex = this.getTextureForFrame(frame, dir, obj != null && obj.isUseSnowSprite());
        if (tex == null) {
            return;
        }
        if (Core.tileScale == 2 && tex.getWidthOrig() == 64 && tex.getHeightOrig() == 128) {
            inst.setScale(2.0f, 2.0f);
        }
        if (Core.tileScale == 2 && inst.scaleX == 2.0f && inst.scaleY == 2.0f && tex.getWidthOrig() == 128 && tex.getHeightOrig() == 256) {
            inst.setScale(1.0f, 1.0f);
        }
        if (inst.scaleX <= 0.0f || inst.scaleY <= 0.0f) {
            return;
        }
        float width = tex.getWidth();
        float height = tex.getHeight();
        float scaleX = inst.scaleX;
        float scaleY = inst.scaleY;
        float tx1 = tx;
        float ty1 = ty;
        if (scaleX != 1.0f) {
            tx += tex.getOffsetX() * (scaleX - 1.0f);
            width *= scaleX;
        }
        if (scaleY != 1.0f) {
            ty += tex.getOffsetY() * (scaleY - 1.0f);
            height *= scaleY;
        }
        TextureDraw.nextZ = tdepth;
        if (!this.hideForWaterRender || !IsoWater.getInstance().getShaderEnable()) {
            if (this.hasAnimation()) {
                IsoDirectionFrame fr = this.getAnimFrame(frame);
                if (obj != null && obj.getObjectRenderEffectsToApply() != null) {
                    fr.render(obj.getObjectRenderEffectsToApply(), tx, ty, width, height, dir, info, inst.flip, texdModifier);
                } else {
                    fr.render(tx, ty, width, height, dir, info, inst.flip, texdModifier);
                }
            } else if (obj != null && obj.getObjectRenderEffectsToApply() != null) {
                if (inst.flip) {
                    tex.flip = !tex.flip;
                }
                tex.render(obj.getObjectRenderEffectsToApply(), tx, ty, width, height, IsoSprite.info.r, IsoSprite.info.g, IsoSprite.info.b, IsoSprite.info.a, texdModifier);
                tex.flip = false;
            } else {
                if (inst.flip) {
                    tex.flip = !tex.flip;
                }
                tex.render(tx, ty, width, height, IsoSprite.info.r, IsoSprite.info.g, IsoSprite.info.b, IsoSprite.info.a, texdModifier);
                tex.flip = false;
            }
        }
        if (DebugOptions.instance.isoSprite.movingObjectEdges.getValue() && obj instanceof IsoMovingObject) {
            this.renderSpriteOutline(tx1, ty1, tex, scaleX, scaleY);
        }
        if (DebugOptions.instance.isoSprite.dropShadowEdges.getValue() && StringUtils.equals(tex.getName(), "dropshadow")) {
            this.renderSpriteOutline(tx1, ty1, tex, scaleX, scaleY);
        }
        if (PerformanceSettings.fboRenderChunk) {
            if (obj == null) {
                return;
            }
            if (FBORenderCell.instance.renderAnimatedAttachments) {
                return;
            }
            if (WeatherFxMask.isRenderingMask()) {
                return;
            }
            if (IsoSprite.info.a == 0.0f) {
                return;
            }
            int playerIndex = IsoCamera.frameState.playerIndex;
            ObjectRenderInfo renderInfo = obj.getRenderInfo(playerIndex);
            if (FBORenderObjectHighlight.getInstance().isRendering() || FBORenderObjectOutline.getInstance().isRendering()) {
                boolean bl = true;
            } else if (FBORenderCell.instance.renderTranslucentOnly) {
                renderInfo.renderX = obj.sx - IsoCamera.frameState.offX;
                renderInfo.renderY = obj.sy - IsoCamera.frameState.offY;
            } else if (FBORenderChunkManager.instance.renderChunk != null && FBORenderChunkManager.instance.renderChunk.highRes) {
                renderInfo.renderX = obj.sx + FBORenderChunkManager.instance.getXOffset() - (float)FBORenderChunkManager.instance.renderChunk.w / 4.0f;
                renderInfo.renderY = obj.sy + FBORenderChunkManager.instance.getYOffset();
            } else {
                renderInfo.renderX = obj.sx + FBORenderChunkManager.instance.getXOffset();
                renderInfo.renderY = obj.sy + FBORenderChunkManager.instance.getYOffset();
            }
            renderInfo.renderWidth = (float)tex.getWidthOrig() * scaleX;
            renderInfo.renderHeight = (float)tex.getHeightOrig() * scaleY;
            renderInfo.renderScaleX = scaleX;
            renderInfo.renderScaleY = scaleY;
            renderInfo.renderAlpha = IsoSprite.info.a;
            return;
        }
        if (IsoObjectPicker.Instance.wasDirty && IsoCamera.frameState.playerIndex == 0 && obj != null) {
            boolean flip;
            boolean bl = flip = dir == IsoDirections.W || dir == IsoDirections.SW || dir == IsoDirections.S;
            if (inst.flip) {
                flip = !flip;
            }
            tx = obj.sx + globalOffsetX;
            ty = obj.sy + globalOffsetY;
            if (obj instanceof IsoMovingObject) {
                tx -= (float)tex.getWidthOrig() / 2.0f * scaleX;
                ty -= (float)tex.getHeightOrig() * scaleY;
            }
            IsoObjectPicker.Instance.Add((int)tx, (int)ty, (int)((float)tex.getWidthOrig() * scaleX), (int)((float)tex.getHeightOrig() * scaleY), obj.square, obj, flip, scaleX, scaleY);
        }
    }

    private void renderSpriteOutline(float tx, float ty, Texture tex, float scaleX, float scaleY) {
        if (PerformanceSettings.fboRenderChunk) {
            IndieGL.glBlendFunc(770, 771);
            IndieGL.StartShader(0);
            IndieGL.disableDepthTest();
        }
        LineDrawer.drawRect(tx, ty, (float)tex.getWidthOrig() * scaleX, (float)tex.getHeightOrig() * scaleY, 1.0f, 1.0f, 1.0f, 1.0f, 1);
        LineDrawer.drawRect(tx + tex.getOffsetX() * scaleX, ty + tex.getOffsetY() * scaleY, (float)tex.getWidth() * scaleX, (float)tex.getHeight() * scaleY, 1.0f, 1.0f, 1.0f, 1.0f, 1);
    }

    public void renderActiveModel() {
        if (this.modelSlot.model.object == null || !DebugOptions.instance.isoSprite.renderModels.getValue()) {
            return;
        }
        try (GameProfiler.ProfileArea ignored = GameProfiler.getInstance().profile("Render Active Model");){
            if (!PerformanceSettings.fboRenderChunk || !FBORenderCell.instance.renderDebugChunkState) {
                this.modelSlot.model.updateLights();
            }
            ModelCameraRenderData cameraRenderData = ModelCameraRenderData.s_pool.alloc();
            cameraRenderData.init(CharacterModelCamera.instance, this.modelSlot);
            SpriteRenderer.instance.drawGeneric(cameraRenderData);
            SpriteRenderer.instance.drawModel(this.modelSlot);
        }
    }

    public void renderBloodSplat(float x, float y, float z, ColorInfo info2) {
        Texture texture = this.getTextureForCurrentFrame(IsoDirections.N);
        if (texture == null) {
            return;
        }
        try {
            if (globalOffsetX == -1.0f) {
                globalOffsetX = -IsoCamera.frameState.offX;
                globalOffsetY = -IsoCamera.frameState.offY;
            }
            float goX = globalOffsetX;
            float goY = globalOffsetY;
            if (FBORenderChunkManager.instance.isCaching()) {
                goX = FBORenderChunkManager.instance.getXOffset();
                goY = FBORenderChunkManager.instance.getYOffset();
                x -= (float)(FBORenderChunkManager.instance.renderChunk.chunk.wx * 8);
                y -= (float)(FBORenderChunkManager.instance.renderChunk.chunk.wy * 8);
            }
            float sx = IsoUtils.XToScreen(x, y, z, 0);
            float sy = IsoUtils.YToScreen(x, y, z, 0);
            sx = (int)sx;
            sy = (int)sy;
            sx -= (float)texture.getWidth() / 2.0f * (float)Core.tileScale;
            sy -= (float)texture.getHeight() / 2.0f * (float)Core.tileScale;
            sx += goX;
            sy += goY;
            if (!PerformanceSettings.fboRenderChunk) {
                if (sx >= (float)IsoCamera.frameState.offscreenWidth || sx + 64.0f <= 0.0f) {
                    return;
                }
                if (sy >= (float)IsoCamera.frameState.offscreenHeight || sy + 64.0f <= 0.0f) {
                    return;
                }
            }
            IsoSprite.info.r = info2.r;
            IsoSprite.info.g = info2.g;
            IsoSprite.info.b = info2.b;
            IsoSprite.info.a = info2.a;
            SpriteRenderer.instance.StartShader(SceneShaderStore.defaultShaderId, IsoCamera.frameState.playerIndex);
            IndieGL.disableDepthTest();
            IndieGL.glBlendFuncSeparate(770, 771, 773, 1);
            texture.render(sx, sy, texture.getWidth(), texture.getHeight(), IsoSprite.info.r, IsoSprite.info.g, IsoSprite.info.b, IsoSprite.info.a, TileDepthModifier.instance);
        }
        catch (Exception ex) {
            Logger.getLogger(GameWindow.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public void renderObjectPicker(IsoSpriteInstance def, IsoObject obj, IsoDirections dir) {
        if (def == null) {
            return;
        }
        if (IsoPlayer.getInstance() != IsoPlayer.players[0]) {
            return;
        }
        Texture tex = this.getTextureForFrame((int)def.frame, dir, obj != null && obj.isUseSnowSprite());
        if (tex == null) {
            return;
        }
        float sx = obj.sx + globalOffsetX;
        float sy = obj.sy + globalOffsetY;
        if (obj instanceof IsoMovingObject) {
            sx -= (float)tex.getWidthOrig() / 2.0f * def.getScaleX();
            sy -= (float)tex.getHeightOrig() * def.getScaleY();
        }
        if (IsoObjectPicker.Instance.wasDirty && IsoCamera.frameState.playerIndex == 0) {
            boolean flip;
            boolean bl = flip = dir == IsoDirections.W || dir == IsoDirections.SW || dir == IsoDirections.S;
            if (def.flip) {
                flip = !flip;
            }
            IsoObjectPicker.Instance.Add((int)sx, (int)sy, (int)((float)tex.getWidthOrig() * def.getScaleX()), (int)((float)tex.getHeightOrig() * def.getScaleY()), obj.square, obj, flip, def.getScaleX(), def.getScaleY());
        }
    }

    public IsoDirectionFrame getAnimFrame(int frame) {
        if (this.currentAnim == null || this.currentAnim.frames.isEmpty()) {
            return null;
        }
        if (this.currentAnim.framesArray == null) {
            this.currentAnim.framesArray = this.currentAnim.frames.toArray(new IsoDirectionFrame[0]);
        }
        if (this.currentAnim.framesArray.length != this.currentAnim.frames.size()) {
            this.currentAnim.framesArray = this.currentAnim.frames.toArray(this.currentAnim.framesArray);
        }
        if (frame >= this.currentAnim.framesArray.length) {
            frame = this.currentAnim.framesArray.length - 1;
        }
        if (frame < 0) {
            frame = 0;
        }
        return this.currentAnim.framesArray[frame];
    }

    public Texture getTextureForFrame(int frame, IsoDirections dir, boolean useSnowSprite) {
        if (useSnowSprite && this.snowSprite != null) {
            return this.snowSprite.getTextureForFrame(frame, dir, false);
        }
        if (this.currentAnim == null || this.currentAnim.frames.isEmpty()) {
            return this.texture;
        }
        return this.getAnimFrame(frame).getTexture(dir);
    }

    public Texture getTextureForFrame(int frame, IsoDirections dir) {
        return this.getTextureForFrame(frame, dir, false);
    }

    public Texture getTextureForCurrentFrame(IsoDirections dir, boolean useSnowSprite) {
        this.initSpriteInstance();
        return this.getTextureForFrame((int)this.def.frame, dir, useSnowSprite);
    }

    public Texture getTextureForCurrentFrame(IsoDirections dir) {
        return this.getTextureForCurrentFrame(dir, false);
    }

    public Texture getTextureForCurrentFrame(IsoDirections dir, IsoObject obj) {
        return this.getTextureForCurrentFrame(dir, obj != null && obj.isUseSnowSprite());
    }

    public void update() {
        this.update(this.def);
    }

    public void update(IsoSpriteInstance def) {
        if (def == null) {
            def = IsoSpriteInstance.get(this);
        }
        if (this.currentAnim == null) {
            def.frame = 0.0f;
            return;
        }
        if (this.animate && !def.finished) {
            float lastFrame = def.frame;
            if (!GameTime.isGamePaused()) {
                def.frame += def.animFrameIncrease * (GameTime.instance.getMultipliedSecondsSinceLastUpdate() * 60.0f);
            }
            if ((int)def.frame >= this.currentAnim.frames.size() && this.loop && def.looped) {
                def.frame = 0.0f;
            }
            if ((int)lastFrame != (int)def.frame) {
                def.nextFrame = true;
            }
            if (!((int)def.frame < this.currentAnim.frames.size() || this.loop && def.looped)) {
                def.finished = true;
                def.frame = this.currentAnim.finishUnloopedOnFrame;
                if (this.deleteWhenFinished) {
                    this.Dispose();
                    this.animate = false;
                }
            }
        }
    }

    public void CacheAnims(String key) {
        this.name = key;
        Stack<CallSite> animList = new Stack<CallSite>();
        for (int n = 0; n < this.animStack.size(); ++n) {
            IsoAnim anim = this.animStack.get(n);
            String total = key + anim.name;
            animList.add((CallSite)((Object)total));
            if (IsoAnim.GlobalAnimMap.containsKey(total)) continue;
            IsoAnim.GlobalAnimMap.put(total, anim);
        }
        AnimNameSet.put(key, animList.toArray());
    }

    public void LoadCache(String string) {
        this.allocateAnimationIfNeeded();
        Object[] arr = AnimNameSet.get(string);
        this.name = string;
        for (int n = 0; n < arr.length; ++n) {
            String s = (String)arr[n];
            IsoAnim a = IsoAnim.GlobalAnimMap.get(s);
            this.animMap.put(a.name, a);
            this.animStack.add(a);
            this.currentAnim = a;
        }
    }

    public IsoSprite setFromCache(String objectName, String animName, int numFrames) {
        String cacheKey = objectName + animName;
        if (IsoSprite.HasCache(cacheKey)) {
            this.LoadCache(cacheKey);
        } else {
            this.LoadFramesNoDirPage(objectName, animName, numFrames);
            this.CacheAnims(cacheKey);
        }
        return this;
    }

    public IsoObjectType getType() {
        return this.tileType;
    }

    public void setType(IsoObjectType type) {
        this.tileType = type;
    }

    public IsoObjectType getTileType() {
        return this.tileType;
    }

    public void setTileType(IsoObjectType type) {
        this.tileType = type;
    }

    public void AddProperties(IsoSprite sprite) {
        this.getProperties().AddProperties(sprite.getProperties());
    }

    public int getItemHeight() {
        return this.properties.getItemHeight();
    }

    public int getSurface() {
        return this.properties.getSurface();
    }

    public int getStackReplaceTileOffset() {
        return this.properties.getStackReplaceTileOffset();
    }

    public boolean isTable() {
        return this.properties.isTable();
    }

    public boolean isTableTop() {
        return this.properties.isTableTop();
    }

    public boolean isSurfaceOffset() {
        return this.properties.isSurfaceOffset();
    }

    public IsoDirections getSlopedSurfaceDirection() {
        return this.properties.getSlopedSurfaceDirection();
    }

    public int getID() {
        return this.id;
    }

    public String getName() {
        return this.name;
    }

    public void setName(String string) {
        this.name = string;
    }

    public ColorInfo getTintMod() {
        return this.tintMod;
    }

    public void setTintMod(ColorInfo info) {
        this.tintMod.set(info);
    }

    public void setAnimate(boolean animate) {
        this.animate = animate;
    }

    public IsoSpriteGrid getSpriteGrid() {
        return this.spriteGrid;
    }

    public void setSpriteGrid(IsoSpriteGrid sGrid) {
        this.spriteGrid = sGrid;
    }

    public boolean isMoveWithWind() {
        return this.moveWithWind;
    }

    public boolean is(IsoFlagType flag) {
        return this.getProperties().has(flag);
    }

    public boolean isWallSE() {
        return this.is(IsoFlagType.WallSE);
    }

    public int getSheetGridIdFromName() {
        if (this.name != null) {
            return IsoSprite.getSheetGridIdFromName(this.name);
        }
        return -1;
    }

    public static int getSheetGridIdFromName(String name) {
        int index;
        if (name != null && (index = name.lastIndexOf(95)) > 0 && index + 1 < name.length()) {
            return Integer.parseInt(name.substring(index + 1));
        }
        return -1;
    }

    public IsoDirections getFacing() {
        String facing;
        if (this.getProperties().has(IsoPropertyType.FACING) && (facing = this.getProperties().get(IsoPropertyType.FACING)) != null) {
            switch (facing) {
                case "N": {
                    return IsoDirections.N;
                }
                case "S": {
                    return IsoDirections.S;
                }
                case "W": {
                    return IsoDirections.W;
                }
                case "E": {
                    return IsoDirections.E;
                }
            }
        }
        return null;
    }

    private void initRoofProperties() {
        if (this.initRoofProperties) {
            return;
        }
        this.initRoofProperties = true;
        this.roofProperties = RoofProperties.initSprite(this);
    }

    public RoofProperties getRoofProperties() {
        this.initRoofProperties();
        return this.roofProperties;
    }

    public void clearCurtainOffset() {
        this.curtainOffset = null;
    }

    public void setCurtainOffset(float x, float y, float z) {
        if (this.curtainOffset == null) {
            this.curtainOffset = new Vector3f();
        }
        this.curtainOffset.set(x, y, z);
    }

    public Vector3f getCurtainOffset() {
        return this.curtainOffset;
    }

    public boolean shouldHaveCollision() {
        if (this.getProperties() != null) {
            return this.getProperties().has(IsoFlagType.solid) || this.getProperties().has(IsoFlagType.solidtrans) || this.getProperties().has(IsoFlagType.WallN) || this.getProperties().has(IsoFlagType.WallNW) || this.getProperties().has(IsoFlagType.WallW) || this.getProperties().has(IsoFlagType.collideN) || this.getProperties().has(IsoFlagType.collideW);
        }
        return false;
    }

    public void setSnowSprite(IsoSprite sprite) {
        this.snowSprite = sprite;
    }

    public IsoSprite getSnowSprite() {
        return this.snowSprite;
    }

    static {
        alphaStep = 0.05f;
        globalOffsetX = -1.0f;
        globalOffsetY = -1.0f;
        info = new ColorInfo();
        AnimNameSet = new HashMap();
        seamEast = true;
        AND_THEN = new AndThen();
    }

    private static class l_renderCurrentAnim {
        static final Vector3 colorInfoBackup = new Vector3();
        static final Vector3 spritePos = new Vector3();

        private l_renderCurrentAnim() {
        }
    }

    private static final class AndThen
    implements Consumer<TextureDraw> {
        private Consumer<? super TextureDraw> a;
        private Consumer<? super TextureDraw> b;

        private AndThen() {
        }

        private AndThen set(Consumer<TextureDraw> a, Consumer<TextureDraw> b) {
            this.a = a;
            this.b = b;
            return this;
        }

        @Override
        public void accept(TextureDraw o) {
            this.a.accept(o);
            this.b.accept(o);
        }
    }
}

