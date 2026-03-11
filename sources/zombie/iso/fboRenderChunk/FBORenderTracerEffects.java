/*
 * Decompiled with CFR 0.152.
 */
package zombie.iso.fboRenderChunk;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.opengl.GL11;
import zombie.GameTime;
import zombie.UsedFromLua;
import zombie.ZomboidFileSystem;
import zombie.characters.IsoGameCharacter;
import zombie.config.ConfigFile;
import zombie.config.ConfigOption;
import zombie.config.DoubleConfigOption;
import zombie.core.Core;
import zombie.core.SpriteRenderer;
import zombie.core.math.PZMath;
import zombie.core.opengl.GLStateRenderThread;
import zombie.core.opengl.PZGLUtil;
import zombie.core.opengl.VBORenderer;
import zombie.core.skinnedmodel.model.ModelInstanceRenderData;
import zombie.core.skinnedmodel.model.VertexBufferObject;
import zombie.core.textures.TextureDraw;
import zombie.debug.DebugOptions;
import zombie.iso.IsoCamera;
import zombie.iso.IsoDepthHelper;
import zombie.iso.PlayerCamera;
import zombie.popman.ObjectPool;
import zombie.scripting.objects.ModelAttachment;
import zombie.vehicles.BaseVehicle;

@UsedFromLua
public final class FBORenderTracerEffects {
    private static FBORenderTracerEffects instance;
    private final ArrayList<Effect> effects = new ArrayList();
    private final ObjectPool<Effect> effectPool = new ObjectPool<Effect>(Effect::new);
    private final ObjectPool<Drawer> drawerPool = new ObjectPool<Drawer>(Drawer::new);
    public final HashMap<IsoGameCharacter, Matrix4f> playerWeaponTransform = new HashMap();
    private static final int VERSION = 1;
    private final ArrayList<ConfigOption> options = new ArrayList();
    final DoubleConfigOption1 startRadius = new DoubleConfigOption1(this, "StartRadius", 0.001f, 0.02f, 0.0025f);
    final DoubleConfigOption1 endRadius = new DoubleConfigOption1(this, "EndRadius", 0.001f, 0.02f, 0.01f);
    final DoubleConfigOption1 length = new DoubleConfigOption1(this, "Length", 0.1f, 10.0, 1.0);
    final DoubleConfigOption1 speed = new DoubleConfigOption1(this, "Speed", 0.001f, 0.5, 0.075f);
    final DoubleConfigOption1 red = new DoubleConfigOption1(this, "Red", 0.0, 1.0, 1.0);
    final DoubleConfigOption1 green = new DoubleConfigOption1(this, "Green", 0.0, 1.0, 1.0);
    final DoubleConfigOption1 blue = new DoubleConfigOption1(this, "Blue", 0.0, 1.0, 1.0);
    final DoubleConfigOption1 alpha = new DoubleConfigOption1(this, "Alpha", 0.0, 1.0, 1.0);

    public static FBORenderTracerEffects getInstance() {
        if (instance == null) {
            instance = new FBORenderTracerEffects();
        }
        return instance;
    }

    private FBORenderTracerEffects() {
        this.load();
    }

    public void releaseWeaponTransform(IsoGameCharacter chr) {
        Matrix4f mtx = this.playerWeaponTransform.remove(chr);
        if (mtx == null) {
            return;
        }
        BaseVehicle.releaseMatrix4f(mtx);
    }

    public void storeWeaponTransform(IsoGameCharacter chr, Matrix4f xfrm) {
        if (!DebugOptions.instance.fboRenderChunk.bulletTracers.getValue()) {
            return;
        }
        Matrix4f mtx = this.playerWeaponTransform.remove(chr);
        if (mtx != null) {
            BaseVehicle.releaseMatrix4f(mtx);
        }
        if (chr == null || chr.primaryHandModel == null || chr.primaryHandModel.modelScript == null) {
            return;
        }
        ModelAttachment attachment = chr.primaryHandModel.modelScript.getAttachmentById("muzzle");
        if (attachment == null) {
            return;
        }
        mtx = BaseVehicle.allocMatrix4f();
        mtx.set(xfrm);
        mtx.transpose();
        Matrix4f m = BaseVehicle.allocMatrix4f();
        ModelInstanceRenderData.makeAttachmentTransform(attachment, m);
        mtx.mul(m);
        BaseVehicle.releaseMatrix4f(m);
        this.playerWeaponTransform.put(chr, mtx);
    }

    public void addEffect(IsoGameCharacter chr, float range) {
        if (chr == null || !chr.getAnimationPlayer().isReady()) {
            return;
        }
        if (!this.playerWeaponTransform.containsKey(chr)) {
            return;
        }
        Effect e = this.effectPool.alloc();
        e.x0 = chr.getX();
        e.y0 = chr.getY();
        e.z0 = chr.getZ();
        e.angle = chr.getAnimationPlayer().getRenderedAngle();
        e.range = range;
        e.r1 = (float)this.red.getValue();
        e.g1 = (float)this.green.getValue();
        e.b1 = (float)this.blue.getValue();
        e.a1 = (float)this.alpha.getValue();
        e.thickness0 = (float)this.startRadius.getValue();
        e.thickness1 = (float)this.endRadius.getValue();
        e.length = (float)this.length.getValue();
        e.speed = (float)this.speed.getValue();
        e.t = 0.0f;
        e.weaponXfrm.set(this.playerWeaponTransform.get(chr));
        this.effects.add(e);
    }

    public void render() {
        Drawer drawer = this.drawerPool.alloc();
        for (int i = 0; i < this.effects.size(); ++i) {
            Effect effect = this.effects.get(i);
            Effect effectCopy = this.effectPool.alloc().set(effect);
            drawer.effects.add(effectCopy);
            if (!GameTime.isGamePaused()) {
                effect.t += GameTime.getInstance().getMultiplier() * effect.speed;
            }
            if (!(effect.t >= 1.0f)) continue;
            this.effects.remove(i--);
            this.effectPool.release(effect);
        }
        SpriteRenderer.instance.drawGeneric(drawer);
    }

    private void registerOption(ConfigOption option) {
        this.options.add(option);
    }

    public int getOptionCount() {
        return this.options.size();
    }

    public ConfigOption getOptionByIndex(int index) {
        return this.options.get(index);
    }

    public ConfigOption getOptionByName(String name) {
        for (int i = 0; i < this.options.size(); ++i) {
            ConfigOption setting = this.options.get(i);
            if (!setting.getName().equals(name)) continue;
            return setting;
        }
        return null;
    }

    public void save() {
        String fileName = ZomboidFileSystem.instance.getCacheDir() + File.separator + "bulletTracerEffect-options.ini";
        ConfigFile configFile = new ConfigFile();
        configFile.write(fileName, 1, this.options);
    }

    public void load() {
        ConfigFile configFile = new ConfigFile();
        String fileName = ZomboidFileSystem.instance.getCacheDir() + File.separator + "bulletTracerEffect-options.ini";
        if (configFile.read(fileName)) {
            for (int i = 0; i < configFile.getOptions().size(); ++i) {
                ConfigOption configOption = configFile.getOptions().get(i);
                ConfigOption myOption = this.getOptionByName(configOption.getName());
                if (myOption == null) continue;
                myOption.parse(configOption.getValueAsString());
            }
        }
    }

    public class DoubleConfigOption1
    extends DoubleConfigOption {
        public DoubleConfigOption1(FBORenderTracerEffects this$0, String name, double min, double max, double defaultValue) {
            Objects.requireNonNull(this$0);
            super(name, min, max, defaultValue);
            this$0.registerOption(this);
        }
    }

    private static final class Effect {
        float x0;
        float y0;
        float z0;
        float angle;
        float range;
        float r0;
        float g0;
        float b0;
        float a0;
        float r1;
        float g1;
        float b1;
        float a1;
        float thickness0;
        float thickness1;
        float length;
        float speed;
        float t = -1.0f;
        final Matrix4f projection = new Matrix4f();
        final Matrix4f view = new Matrix4f();
        final Matrix4f model = new Matrix4f();
        final Matrix4f weaponXfrm = new Matrix4f();

        private Effect() {
        }

        Effect set(Effect other) {
            this.x0 = other.x0;
            this.y0 = other.y0;
            this.z0 = other.z0;
            this.angle = other.angle;
            this.range = other.range;
            this.r1 = other.r1;
            this.g1 = other.g1;
            this.b1 = other.b1;
            this.a1 = other.a1;
            this.thickness0 = other.thickness0;
            this.thickness1 = other.thickness1;
            this.length = other.length;
            this.speed = other.speed;
            this.t = other.t;
            this.projection.set(other.projection);
            this.view.set(other.view);
            this.model.set(other.model);
            this.weaponXfrm.set(other.weaponXfrm);
            return this;
        }

        void update() {
        }

        void render() {
            VBORenderer vbor = VBORenderer.getInstance();
            vbor.addCylinder_Fill(this.thickness0, this.thickness1, this.length, 4, 1, this.r1, this.g1, this.b1, this.a1);
        }
    }

    private static final class Drawer
    extends TextureDraw.GenericDrawer {
        static final Matrix4f tempMatrix4f_1 = new Matrix4f();
        static final Vector3f tempVector3f_1 = new Vector3f();
        final ArrayList<Effect> effects = new ArrayList();

        private Drawer() {
        }

        @Override
        public void render() {
            GL11.glDepthFunc(515);
            GL11.glEnable(3042);
            GL11.glBlendFunc(770, 771);
            VBORenderer.getInstance().setDepthTestForAllRuns(Boolean.TRUE);
            float characterX = Core.getInstance().floatParamMap.get(0).floatValue();
            float characterY = Core.getInstance().floatParamMap.get(1).floatValue();
            for (int i = 0; i < this.effects.size(); ++i) {
                Effect effect = this.effects.get(i);
                Drawer.calculateProjectViewXfrm(effect.projection, effect.view, true);
                PZGLUtil.pushAndLoadMatrix(5889, effect.projection);
                Drawer.calculateModelXfrm(effect.x0, effect.y0, effect.z0, effect.angle, false, effect.model, true);
                Matrix4f m = tempMatrix4f_1.set(effect.view);
                m.mul(effect.model);
                m.mul(effect.weaponXfrm);
                m.translate(0.0f, 0.0f, effect.t * effect.range);
                PZGLUtil.pushAndLoadMatrix(5888, m);
                m.set(effect.model);
                m.mul(effect.weaponXfrm);
                m.setTranslation(0.0f, 0.0f, 0.0f);
                m.translate(0.0f, 0.0f, effect.t * effect.range);
                Vector3f v = m.transformPosition(tempVector3f_1.set(0.0f));
                float depthBufferValue = VertexBufferObject.getDepthValueAt(0.0f, 0.0f, 0.0f);
                IsoDepthHelper.Results results = IsoDepthHelper.getSquareDepthData(PZMath.fastfloor(characterX), PZMath.fastfloor(characterY), effect.x0 - v.x, effect.y0 - v.z, effect.z0 * 0.0f + v.y);
                float targetDepth = results.depthStart - (depthBufferValue + 1.0f) / 2.0f;
                VBORenderer.getInstance().setUserDepthForAllRuns(Float.valueOf(targetDepth));
                effect.render();
                VBORenderer.getInstance().flush();
                PZGLUtil.popMatrix(5888);
                PZGLUtil.popMatrix(5889);
            }
            VBORenderer.getInstance().setDepthTestForAllRuns(null);
            VBORenderer.getInstance().setUserDepthForAllRuns(null);
            GLStateRenderThread.restore();
        }

        @Override
        public void postRender() {
            FBORenderTracerEffects.getInstance().effectPool.releaseAll((List<Effect>)this.effects);
            this.effects.clear();
            FBORenderTracerEffects.getInstance().drawerPool.release(this);
        }

        static Matrix4f calculateProjectViewXfrm(Matrix4f projection, Matrix4f view, boolean renderThread) {
            int playerIndex = renderThread ? SpriteRenderer.instance.getRenderingPlayerIndex() : IsoCamera.frameState.playerIndex;
            PlayerCamera cam = renderThread ? SpriteRenderer.instance.getRenderingPlayerCamera(playerIndex) : IsoCamera.cameras[playerIndex];
            float offscreenWidth = renderThread ? (float)cam.offscreenWidth : (float)IsoCamera.getOffscreenWidth(playerIndex);
            float offscreenHeight = renderThread ? (float)cam.offscreenHeight : (float)IsoCamera.getOffscreenHeight(playerIndex);
            double screenWidth = offscreenWidth / 1920.0f;
            double screenHeight = offscreenHeight / 1920.0f;
            projection.setOrtho(-((float)screenWidth) / 2.0f, (float)screenWidth / 2.0f, -((float)screenHeight) / 2.0f, (float)screenHeight / 2.0f, -10.0f, 10.0f);
            view.scaling(Core.scale);
            view.scale((float)Core.tileScale / 2.0f);
            view.rotate(0.5235988f, 1.0f, 0.0f, 0.0f);
            view.rotate(2.3561945f, 0.0f, 1.0f, 0.0f);
            return view;
        }

        static void calculateModelXfrm(float ox, float oy, float oz, float useangle, boolean vehicle, Matrix4f model, boolean renderThread) {
            int playerIndex = renderThread ? SpriteRenderer.instance.getRenderingPlayerIndex() : IsoCamera.frameState.playerIndex;
            float cx = renderThread ? Core.getInstance().floatParamMap.get(0).floatValue() : IsoCamera.frameState.camCharacterX;
            float cy = renderThread ? Core.getInstance().floatParamMap.get(1).floatValue() : IsoCamera.frameState.camCharacterY;
            float cz = renderThread ? Core.getInstance().floatParamMap.get(2).floatValue() : IsoCamera.frameState.camCharacterZ;
            double x = cx;
            double y = cy;
            double z = cz;
            PlayerCamera cam = renderThread ? SpriteRenderer.instance.getRenderingPlayerCamera(playerIndex) : IsoCamera.cameras[playerIndex];
            float rcx = cam.rightClickX;
            float rcy = cam.rightClickY;
            float tox = cam.getTOffX();
            float toy = cam.getTOffY();
            float defx = cam.deferedX;
            float defy = cam.deferedY;
            x -= (double)cam.XToIso(-tox - rcx, -toy - rcy, 0.0f);
            y -= (double)cam.YToIso(-tox - rcx, -toy - rcy, 0.0f);
            double difX = (double)ox - (x += (double)defx);
            double difY = (double)oy - (y += (double)defy);
            model.identity();
            model.translate(-((float)difX), (float)((double)oz - z) * 2.44949f, -((float)difY));
            if (vehicle) {
                model.scale(-1.0f, 1.0f, 1.0f);
            } else {
                model.scale(-1.5f, 1.5f, 1.5f);
            }
            model.rotate(useangle + (float)Math.PI, 0.0f, 1.0f, 0.0f);
            if (!vehicle) {
                model.translate(0.0f, -0.48f, 0.0f);
            }
        }
    }
}

