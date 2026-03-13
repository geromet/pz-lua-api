/*
 * Decompiled with CFR 0.152.
 */
package zombie.vehicles;

import gnu.trove.list.array.TFloatArrayList;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import org.joml.Quaternionf;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL20;
import org.lwjgl.util.vector.Matrix4f;
import org.lwjglx.BufferUtils;
import org.lwjglx.util.glu.Cylinder;
import org.lwjglx.util.glu.PartialDisk;
import se.krka.kahlua.vm.KahluaTable;
import se.krka.kahlua.vm.KahluaUtil;
import zombie.GameTime;
import zombie.IndieGL;
import zombie.Lua.LuaManager;
import zombie.UsedFromLua;
import zombie.characters.action.ActionContext;
import zombie.characters.action.ActionGroup;
import zombie.characters.animals.AnimalDefinitions;
import zombie.characters.animals.datas.AnimalBreed;
import zombie.core.BoxedStaticValues;
import zombie.core.Color;
import zombie.core.Core;
import zombie.core.DefaultShader;
import zombie.core.SceneShaderStore;
import zombie.core.ShaderHelper;
import zombie.core.SpriteRenderer;
import zombie.core.math.PZMath;
import zombie.core.opengl.GLStateRenderThread;
import zombie.core.opengl.PZGLUtil;
import zombie.core.opengl.Shader;
import zombie.core.opengl.VBORenderer;
import zombie.core.physics.Bullet;
import zombie.core.physics.PhysicsShape;
import zombie.core.physics.PhysicsShapeAssetManager;
import zombie.core.skinnedmodel.ModelCamera;
import zombie.core.skinnedmodel.ModelManager;
import zombie.core.skinnedmodel.advancedanimation.AnimNode;
import zombie.core.skinnedmodel.advancedanimation.AnimState;
import zombie.core.skinnedmodel.advancedanimation.AnimatedModel;
import zombie.core.skinnedmodel.advancedanimation.AnimationSet;
import zombie.core.skinnedmodel.animation.AnimationClip;
import zombie.core.skinnedmodel.animation.AnimationMultiTrack;
import zombie.core.skinnedmodel.animation.AnimationPlayer;
import zombie.core.skinnedmodel.animation.AnimationTrack;
import zombie.core.skinnedmodel.animation.Keyframe;
import zombie.core.skinnedmodel.model.IsoObjectAnimations;
import zombie.core.skinnedmodel.model.Model;
import zombie.core.skinnedmodel.model.ModelInstanceRenderData;
import zombie.core.skinnedmodel.model.SkinningBone;
import zombie.core.skinnedmodel.model.SkinningData;
import zombie.core.skinnedmodel.shader.ShaderManager;
import zombie.core.skinnedmodel.visual.AnimalVisual;
import zombie.core.skinnedmodel.visual.HumanVisual;
import zombie.core.skinnedmodel.visual.IAnimalVisual;
import zombie.core.skinnedmodel.visual.ItemVisuals;
import zombie.core.textures.Mask;
import zombie.core.textures.Texture;
import zombie.core.textures.TextureDraw;
import zombie.debug.DebugLog;
import zombie.debug.DebugOptions;
import zombie.debug.LineDrawer;
import zombie.input.GameKeyboard;
import zombie.input.Mouse;
import zombie.iso.IsoDirections;
import zombie.iso.IsoUtils;
import zombie.iso.SpriteModel;
import zombie.iso.Vector2;
import zombie.iso.Vector2ObjectPool;
import zombie.iso.sprite.IsoSprite;
import zombie.iso.sprite.IsoSpriteGrid;
import zombie.iso.sprite.IsoSpriteManager;
import zombie.popman.ObjectPool;
import zombie.scripting.ScriptManager;
import zombie.scripting.objects.ModelAttachment;
import zombie.scripting.objects.ModelScript;
import zombie.scripting.objects.PhysicsShapeScript;
import zombie.scripting.objects.VehicleScript;
import zombie.seating.SeatingManager;
import zombie.tileDepth.CylinderUtils;
import zombie.tileDepth.TileDepthTexture;
import zombie.tileDepth.TileDepthTextureManager;
import zombie.tileDepth.TileGeometryFile;
import zombie.tileDepth.TileGeometryManager;
import zombie.tileDepth.TileGeometryUtils;
import zombie.ui.TextManager;
import zombie.ui.UIElement;
import zombie.ui.UIFont;
import zombie.ui.UIManager;
import zombie.util.Pool;
import zombie.util.StringUtils;
import zombie.util.Type;
import zombie.util.list.PZArrayUtil;
import zombie.vehicles.BaseVehicle;
import zombie.vehicles.Clipper;
import zombie.worldMap.Rasterize;

@UsedFromLua
public final class UI3DScene
extends UIElement {
    public static final float Z_SCALE = 0.8164967f;
    private final ArrayList<SceneObject> objects = new ArrayList();
    private View view = View.Right;
    private TransformMode transformMode = TransformMode.Local;
    private int viewX;
    private int viewY;
    private final Vector3f viewRotation = new Vector3f();
    private int zoom = 3;
    private int zoomMax = 10;
    private int gridDivisions = 1;
    private GridPlane gridPlane = GridPlane.YZ;
    private final org.joml.Matrix4f projection = new org.joml.Matrix4f();
    private final org.joml.Matrix4f modelView = new org.joml.Matrix4f();
    private static final long VIEW_CHANGE_TIME = 350L;
    private long viewChangeTime;
    private final Quaternionf modelViewChange = new Quaternionf();
    private boolean drawAttachments;
    private boolean drawGrid = true;
    private boolean drawGridAxes;
    private boolean drawGeometry = true;
    private boolean drawGridPlane;
    private final CharacterSceneModelCamera characterSceneModelCamera = new CharacterSceneModelCamera(this);
    private final VehicleSceneModelCamera vehicleSceneModelCamera = new VehicleSceneModelCamera(this);
    private static final ObjectPool<SetModelCamera> s_SetModelCameraPool = new ObjectPool<SetModelCamera>(SetModelCamera::new);
    private final StateData[] stateData = new StateData[3];
    private Gizmo gizmo;
    private final RotateGizmo rotateGizmo = new RotateGizmo(this);
    private final ScaleGizmo scaleGizmo = new ScaleGizmo(this);
    private final TranslateGizmo translateGizmo = new TranslateGizmo(this);
    private final Vector3f gizmoPos = new Vector3f();
    private final Vector3f gizmoRotate = new Vector3f();
    private SceneObject gizmoParent;
    private SceneObject gizmoOrigin;
    private SceneObject gizmoChild;
    private final OriginAttachment originAttachment = new OriginAttachment(this);
    private final OriginBone originBone = new OriginBone(this);
    private final OriginGeometry originGeometry = new OriginGeometry(this);
    private final OriginGizmo originGizmo = new OriginGizmo(this);
    private final OriginVehiclePart originVehiclePart = new OriginVehiclePart(this);
    private float gizmoScale = 1.0f;
    private boolean gizmoAxisVisibleX = true;
    private boolean gizmoAxisVisibleY = true;
    private boolean gizmoAxisVisibleZ = true;
    private String selectedAttachment;
    private final ArrayList<PositionRotation> axes = new ArrayList();
    private final OriginBone highlightBone = new OriginBone(this);
    private final OriginVehiclePart highlightPartBone = new OriginVehiclePart(this);
    private final PolygonEditor polygonEditor = new PolygonEditor(this);
    private static Clipper clipper;
    private static final ObjectPool<PositionRotation> s_posRotPool;
    private final ArrayList<AABB> aabb = new ArrayList();
    private static final ObjectPool<AABB> s_aabbPool;
    private final ArrayList<Box3D> box3d = new ArrayList();
    private static final ObjectPool<Box3D> s_box3DPool;
    private final ArrayList<PhysicsMesh> physicsMesh = new ArrayList();
    private static final ObjectPool<PhysicsMesh> s_physicsMeshPool;
    final Vector3f tempVector3f = new Vector3f();
    final int[] viewport = new int[]{0, 0, 0, 0};
    private static final float GRID_DARK = 0.1f;
    private static final float GRID_LIGHT = 0.2f;
    private float gridAlpha = 1.0f;
    private static final int HALF_GRID = 5;
    private static VBORenderer vboRenderer;
    private static final ThreadLocal<ObjectPool<Ray>> TL_Ray_pool;
    private static final ThreadLocal<ObjectPool<Plane>> TL_Plane_pool;
    static final float SMALL_NUM = 1.0E-8f;

    public UI3DScene(KahluaTable table) {
        super(table);
        for (int i = 0; i < this.stateData.length; ++i) {
            this.stateData[i] = new StateData();
            this.stateData[i].gridPlaneDrawer = new GridPlaneDrawer(this, this);
            this.stateData[i].overlaysDrawer = new OverlaysDrawer(this);
        }
    }

    SceneObject getSceneObjectById(String id, boolean required) {
        for (int i = 0; i < this.objects.size(); ++i) {
            SceneObject sceneObject = this.objects.get(i);
            if (!sceneObject.id.equalsIgnoreCase(id)) continue;
            return sceneObject;
        }
        if (required) {
            throw new NullPointerException("scene object \"" + id + "\" not found");
        }
        return null;
    }

    <C> C getSceneObjectById(String id, Class<C> clazz, boolean required) {
        for (int i = 0; i < this.objects.size(); ++i) {
            SceneObject sceneObject = this.objects.get(i);
            if (!sceneObject.id.equalsIgnoreCase(id)) continue;
            if (clazz.isInstance(sceneObject)) {
                return clazz.cast(sceneObject);
            }
            if (!required) continue;
            throw new ClassCastException("scene object \"" + id + "\" is " + sceneObject.getClass().getSimpleName() + " expected " + clazz.getSimpleName());
        }
        if (required) {
            throw new NullPointerException("scene object \"" + id + "\" not found");
        }
        return null;
    }

    @Override
    public void render() {
        org.joml.Matrix4f boneMatrix;
        if (!this.isVisible().booleanValue()) {
            return;
        }
        vboRenderer = VBORenderer.getInstance();
        super.render();
        IndieGL.glDepthMask(true);
        SpriteRenderer.instance.glClearDepth(1.0f);
        IndieGL.glClear(256);
        StateData stateData = this.stateDataMain();
        this.setModelViewProjection(stateData);
        if (this.drawGridPlane) {
            SpriteRenderer.instance.drawGeneric(stateData.gridPlaneDrawer);
        }
        PZArrayUtil.forEach(stateData.objectData, SceneObjectRenderData::release);
        stateData.objectData.clear();
        for (int i = 0; i < this.objects.size(); ++i) {
            SceneObjectRenderData renderData;
            SceneObject sceneObject = this.objects.get(i);
            if (!sceneObject.visible) continue;
            if (sceneObject.autoRotate) {
                sceneObject.autoRotateAngle = (float)((double)sceneObject.autoRotateAngle + UIManager.getMillisSinceLastRender() / 30.0);
                if (sceneObject.autoRotateAngle > 360.0f) {
                    sceneObject.autoRotateAngle = 0.0f;
                }
            }
            if ((renderData = sceneObject.renderMain()) == null) continue;
            stateData.objectData.add(renderData);
        }
        float mouseX = Mouse.getXA() - this.getAbsoluteX().intValue();
        float mouseY = Mouse.getYA() - this.getAbsoluteY().intValue();
        this.setGizmoTransforms(stateData);
        if (this.gizmo != null) {
            stateData.gizmoAxis = this.gizmo.hitTest(mouseX, mouseY);
        }
        stateData.overlaysDrawer.init();
        SpriteRenderer.instance.drawGeneric(stateData.overlaysDrawer);
        if (this.drawGrid) {
            Vector3f scenePos = this.uiToScene(mouseX, mouseY, 0.0f, this.tempVector3f);
            scenePos.x = (float)Math.round(scenePos.x * this.gridMult()) / this.gridMult();
            scenePos.y = (float)Math.round(scenePos.y * this.gridMult()) / this.gridMult();
            scenePos.z = (float)Math.round(scenePos.z * this.gridMult()) / this.gridMult();
            int xWidth = TextManager.instance.MeasureStringX(UIFont.Small, String.format("X: %.3f", Float.valueOf(scenePos.x)));
            int yWidth = TextManager.instance.MeasureStringX(UIFont.Small, String.format("Y: %.3f", Float.valueOf(scenePos.y)));
            int zWidth = TextManager.instance.MeasureStringX(UIFont.Small, String.format("Z: %.3f", Float.valueOf(scenePos.z)));
            this.DrawText(UIFont.Small, String.format("X: %.3f", Float.valueOf(scenePos.x)), this.width - 20.0f - (float)zWidth - 20.0f - (float)yWidth - 20.0f - (float)xWidth, 10.0, 1.0, 0.0, 0.0, 1.0);
            this.DrawText(UIFont.Small, String.format("Y: %.3f", Float.valueOf(scenePos.y)), this.width - 20.0f - (float)zWidth - 20.0f - (float)yWidth, 10.0, 0.0, 1.0, 0.0, 1.0);
            this.DrawText(UIFont.Small, String.format("Z: %.3f", Float.valueOf(scenePos.z)), this.width - 20.0f - (float)zWidth, 10.0, 0.0, 0.5, 1.0, 1.0);
        }
        if (this.gizmo == this.rotateGizmo && this.rotateGizmo.trackAxis != Axis.None) {
            Vector3f xln = this.rotateGizmo.startXfrm.getTranslation(UI3DScene.allocVector3f());
            float x = this.sceneToUIX(xln.x, xln.y, xln.z);
            float y = this.sceneToUIY(xln.x, xln.y, xln.z);
            LineDrawer.drawLine(x, y, mouseX, mouseY, 0.5f, 0.5f, 0.5f, 1.0f, 1);
            UI3DScene.releaseVector3f(xln);
        }
        if (this.highlightBone.boneName != null) {
            boneMatrix = this.highlightBone.getGlobalTransform(UI3DScene.allocMatrix4f());
            if (this.highlightBone.character != null) {
                this.highlightBone.character.getGlobalTransform(UI3DScene.allocMatrix4f()).mul(boneMatrix, boneMatrix);
            } else if (this.highlightBone.sceneModel != null) {
                this.highlightBone.sceneModel.getGlobalTransform(UI3DScene.allocMatrix4f()).mul(boneMatrix, boneMatrix);
            }
            Vector3f scenePos = boneMatrix.getTranslation(UI3DScene.allocVector3f());
            float x = this.sceneToUIX(scenePos.x, scenePos.y, scenePos.z);
            float y = this.sceneToUIY(scenePos.x, scenePos.y, scenePos.z);
            LineDrawer.drawCircle(x, y, 10.0f, 16, 1.0f, 1.0f, 1.0f);
            UI3DScene.releaseVector3f(scenePos);
            UI3DScene.releaseMatrix4f(boneMatrix);
        }
        if (this.highlightPartBone.vehicle != null) {
            boneMatrix = this.highlightPartBone.getGlobalBoneTransform(UI3DScene.allocMatrix4f());
            Vector3f scenePos = boneMatrix.getTranslation(UI3DScene.allocVector3f());
            float x = this.sceneToUIX(scenePos.x, scenePos.y, scenePos.z);
            float y = this.sceneToUIY(scenePos.x, scenePos.y, scenePos.z);
            LineDrawer.drawCircle(x, y, 10.0f, 16, 1.0f, 1.0f, 1.0f);
            UI3DScene.releaseVector3f(scenePos);
            UI3DScene.releaseMatrix4f(boneMatrix);
        }
        for (int i = 0; i < this.objects.size(); ++i) {
            ScenePolygon scenePolygon = Type.tryCastTo(this.objects.get(i), ScenePolygon.class);
            if (scenePolygon == null || !scenePolygon.editing) continue;
            scenePolygon.renderPoints();
        }
    }

    private void setModelViewProjection(StateData stateData) {
        this.calcMatrices(this.projection, this.modelView);
        stateData.projection.set(this.projection);
        long ms = System.currentTimeMillis();
        if (this.viewChangeTime + 350L > ms) {
            float f = (float)(this.viewChangeTime + 350L - ms) / 350.0f;
            Quaternionf q = UI3DScene.allocQuaternionf().setFromUnnormalized(this.modelView);
            stateData.modelView.set(this.modelViewChange.slerp(q, 1.0f - f));
            UI3DScene.releaseQuaternionf(q);
        } else {
            stateData.modelView.set(this.modelView);
        }
        stateData.zoom = this.zoom;
    }

    private void setGizmoTransforms(StateData stateData) {
        stateData.gizmo = this.gizmo;
        if (this.gizmo != null) {
            stateData.gizmoTranslate.set(this.gizmoPos);
            stateData.gizmoRotate.set(this.gizmoRotate);
            stateData.gizmoTransform.translation(this.gizmoPos);
            stateData.gizmoTransform.rotateXYZ(this.gizmoRotate.x * ((float)Math.PI / 180), this.gizmoRotate.y * ((float)Math.PI / 180), this.gizmoRotate.z * ((float)Math.PI / 180));
        }
        stateData.gizmoChildTransform.identity();
        stateData.gizmoChildAttachmentTransform.identity();
        boolean bl = stateData.selectedAttachmentIsChildAttachment = this.gizmoChild != null && this.gizmoChild.attachment != null && this.gizmoChild.attachment.equals(this.selectedAttachment);
        if (this.gizmoChild != null) {
            this.gizmoChild.getLocalTransform(stateData.gizmoChildTransform);
            this.gizmoChild.getAttachmentTransform(this.gizmoChild.attachment, stateData.gizmoChildAttachmentTransform);
            stateData.gizmoChildAttachmentTransformInv.set(stateData.gizmoChildAttachmentTransform).invert();
        }
        stateData.gizmoOriginTransform.identity();
        stateData.hasGizmoOrigin = this.gizmoOrigin != null;
        boolean bl2 = stateData.gizmoOriginIsGeometry = this.gizmoOrigin == this.originGeometry;
        if (this.gizmoOrigin != null && this.gizmoOrigin != this.gizmoParent) {
            this.gizmoOrigin.getGlobalTransform(stateData.gizmoOriginTransform);
        }
        stateData.gizmoParentTransform.identity();
        if (this.gizmoParent != null) {
            this.gizmoParent.getGlobalTransform(stateData.gizmoParentTransform);
        }
    }

    private float gridMult() {
        return 100 * this.gridDivisions;
    }

    private float zoomMult() {
        return (float)Math.exp((float)this.zoom * 0.2f) * 160.0f / Math.max(1.82f, 1.0f);
    }

    private static org.joml.Matrix4f allocMatrix4f() {
        return (org.joml.Matrix4f)BaseVehicle.TL_matrix4f_pool.get().alloc();
    }

    private static void releaseMatrix4f(org.joml.Matrix4f matrix) {
        BaseVehicle.TL_matrix4f_pool.get().release(matrix);
    }

    private static Quaternionf allocQuaternionf() {
        return (Quaternionf)BaseVehicle.TL_quaternionf_pool.get().alloc();
    }

    private static void releaseQuaternionf(Quaternionf q) {
        BaseVehicle.TL_quaternionf_pool.get().release(q);
    }

    public static Ray allocRay() {
        return TL_Ray_pool.get().alloc();
    }

    public static void releaseRay(Ray ray) {
        TL_Ray_pool.get().release(ray);
    }

    public static Plane allocPlane() {
        return TL_Plane_pool.get().alloc();
    }

    public static void releasePlane(Plane plane) {
        TL_Plane_pool.get().release(plane);
    }

    private static Vector2 allocVector2() {
        return (Vector2)Vector2ObjectPool.get().alloc();
    }

    private static void releaseVector2(Vector2 vector2) {
        Vector2ObjectPool.get().release(vector2);
    }

    private static Vector2f allocVector2f() {
        return BaseVehicle.allocVector2f();
    }

    private static void releaseVector2f(Vector2f vector2f) {
        BaseVehicle.releaseVector2f(vector2f);
    }

    private static Vector3f allocVector3f() {
        return (Vector3f)BaseVehicle.TL_vector3f_pool.get().alloc();
    }

    private static void releaseVector3f(Vector3f vector3f) {
        BaseVehicle.TL_vector3f_pool.get().release(vector3f);
    }

    public Object fromLua0(String func) {
        switch (func) {
            case "clearAABBs": {
                s_aabbPool.release((List<AABB>)this.aabb);
                this.aabb.clear();
                return null;
            }
            case "clearAxes": {
                s_posRotPool.release((List<PositionRotation>)this.axes);
                this.axes.clear();
                return null;
            }
            case "clearBox3Ds": {
                s_box3DPool.release((List<Box3D>)this.box3d);
                this.box3d.clear();
                return null;
            }
            case "clearGizmoRotate": {
                this.gizmoRotate.set(0.0f);
                return null;
            }
            case "clearHighlightBone": {
                this.highlightBone.boneName = null;
                this.highlightPartBone.vehicle = null;
                return null;
            }
            case "clearPhysicsMeshes": {
                s_physicsMeshPool.releaseAll((List<PhysicsMesh>)this.physicsMesh);
                this.physicsMesh.clear();
                return null;
            }
            case "getDrawGeometry": {
                return this.drawGeometry ? Boolean.TRUE : Boolean.FALSE;
            }
            case "getGeometryNames": {
                ArrayList<String> names = new ArrayList<String>();
                for (SceneObject sceneObject : this.objects) {
                    if (!(sceneObject instanceof SceneGeometry)) continue;
                    SceneGeometry geometry = (SceneGeometry)sceneObject;
                    names.add(geometry.id);
                }
                return names;
            }
            case "getGizmoPos": {
                return this.gizmoPos;
            }
            case "getGridMult": {
                return BoxedStaticValues.toDouble(this.gridMult());
            }
            case "getObjectNames": {
                ArrayList<String> names = new ArrayList<String>();
                for (SceneObject sceneObject : this.objects) {
                    names.add(sceneObject.id);
                }
                return names;
            }
            case "getView": {
                return this.view.name();
            }
            case "getViewRotation": {
                return this.viewRotation;
            }
            case "getModelCount": {
                int count = 0;
                for (int i = 0; i < this.objects.size(); ++i) {
                    if (!(this.objects.get(i) instanceof SceneModel)) continue;
                    ++count;
                }
                return BoxedStaticValues.toDouble(count);
            }
            case "rotateAllGeometry": {
                org.joml.Matrix4f m = UI3DScene.allocMatrix4f().rotationXYZ(0.0f, 4.712389f, 0.0f);
                org.joml.Matrix4f m2 = UI3DScene.allocMatrix4f();
                Quaternionf q = UI3DScene.allocQuaternionf();
                for (SceneObject sceneObject : this.objects) {
                    if (!(sceneObject instanceof SceneGeometry)) continue;
                    SceneGeometry sceneGeometry = (SceneGeometry)sceneObject;
                    sceneGeometry.getLocalTransform(m2);
                    m.mul(m2, m2);
                    m2.getTranslation(sceneGeometry.translate);
                    q.setFromUnnormalized(m2);
                    q.getEulerAnglesXYZ(sceneGeometry.rotate);
                    sceneGeometry.rotate.mul(57.295776f);
                }
                UI3DScene.releaseMatrix4f(m);
                UI3DScene.releaseMatrix4f(m2);
                UI3DScene.releaseQuaternionf(q);
                return null;
            }
            case "stopGizmoTracking": {
                if (this.gizmo != null) {
                    this.gizmo.stopTracking();
                }
                return null;
            }
        }
        throw new IllegalArgumentException("unhandled \"" + func + "\"");
    }

    public Object fromLua1(String func, Object arg0) {
        switch (func) {
            case "addCylinderAABB": {
                SceneCylinder object = this.getSceneObjectById((String)arg0, SceneCylinder.class, true);
                this.aabb.add(object.getAABB(s_aabbPool.alloc()));
                return null;
            }
            case "createCharacter": {
                SceneObject sceneObject = this.getSceneObjectById((String)arg0, false);
                if (sceneObject != null) {
                    throw new IllegalStateException("scene object \"" + String.valueOf(arg0) + "\" exists");
                }
                ScenePlayer character = new ScenePlayer(this, (String)arg0);
                ((SceneCharacter)character).initAnimatedModel();
                this.objects.add(character);
                return character;
            }
            case "createBox": {
                SceneObject sceneObject = this.getSceneObjectById((String)arg0, false);
                if (sceneObject != null) {
                    throw new IllegalStateException("scene object \"" + String.valueOf(arg0) + "\" exists");
                }
                SceneBox box = new SceneBox(this, (String)arg0);
                this.objects.add(box);
                return box;
            }
            case "createCylinder": {
                SceneObject sceneObject = this.getSceneObjectById((String)arg0, false);
                if (sceneObject != null) {
                    throw new IllegalStateException("scene object \"" + String.valueOf(arg0) + "\" exists");
                }
                SceneCylinder cylinder = new SceneCylinder(this, (String)arg0);
                cylinder.height = 1.0f;
                cylinder.radius = 0.5f;
                this.objects.add(cylinder);
                return cylinder;
            }
            case "createPolygon": {
                SceneObject sceneObject = this.getSceneObjectById((String)arg0, false);
                if (sceneObject != null) {
                    throw new IllegalStateException("scene object \"" + String.valueOf(arg0) + "\" exists");
                }
                ScenePolygon scenePolygon = new ScenePolygon(this, (String)arg0);
                for (int i = 0; i < this.objects.size(); ++i) {
                    if (this.objects.get(i) instanceof SceneGeometry) continue;
                    this.objects.add(i, scenePolygon);
                    return scenePolygon;
                }
                this.objects.add(scenePolygon);
                return scenePolygon;
            }
            case "createVehicle": {
                SceneObject sceneObject = this.getSceneObjectById((String)arg0, false);
                if (sceneObject != null) {
                    throw new IllegalStateException("scene object \"" + String.valueOf(arg0) + "\" exists");
                }
                SceneVehicle vehicle = new SceneVehicle(this, (String)arg0);
                this.objects.add(vehicle);
                return null;
            }
            case "getBoxMaxExtents": {
                SceneBox sceneObject = this.getSceneObjectById((String)arg0, SceneBox.class, true);
                return sceneObject.max;
            }
            case "getBoxMinExtents": {
                SceneBox sceneObject = this.getSceneObjectById((String)arg0, SceneBox.class, true);
                return sceneObject.min;
            }
            case "getCharacterAnimate": {
                SceneCharacter character = this.getSceneObjectById((String)arg0, SceneCharacter.class, true);
                return character.animatedModel.isAnimate();
            }
            case "getCharacterAnimationDuration": {
                SceneCharacter sceneCharacter = this.getSceneObjectById((String)arg0, SceneCharacter.class, true);
                AnimationPlayer animPlayer = sceneCharacter.animatedModel.getAnimationPlayer();
                if (animPlayer == null) {
                    return null;
                }
                AnimationMultiTrack multiTrack = animPlayer.getMultiTrack();
                if (multiTrack == null || multiTrack.getTracks().isEmpty()) {
                    return null;
                }
                return KahluaUtil.toDouble(multiTrack.getTracks().get(0).getDuration());
            }
            case "getCharacterAnimationTime": {
                SceneCharacter sceneCharacter = this.getSceneObjectById((String)arg0, SceneCharacter.class, true);
                AnimationPlayer animPlayer = sceneCharacter.animatedModel.getAnimationPlayer();
                if (animPlayer == null) {
                    return null;
                }
                AnimationMultiTrack multiTrack = animPlayer.getMultiTrack();
                if (multiTrack == null || multiTrack.getTracks().isEmpty()) {
                    return null;
                }
                return KahluaUtil.toDouble(multiTrack.getTracks().get(0).getCurrentTimeValue());
            }
            case "getCharacterShowBones": {
                SceneCharacter character = this.getSceneObjectById((String)arg0, SceneCharacter.class, true);
                return character.showBones;
            }
            case "getCylinderHeight": {
                SceneCylinder object = this.getSceneObjectById((String)arg0, SceneCylinder.class, true);
                return (double)object.height;
            }
            case "getCylinderRadius": {
                SceneCylinder object = this.getSceneObjectById((String)arg0, SceneCylinder.class, true);
                return (double)object.radius;
            }
            case "getGeometryType": {
                SceneGeometry sceneGeometry = this.getSceneObjectById((String)arg0, SceneGeometry.class, false);
                return sceneGeometry == null ? null : sceneGeometry.getTypeName();
            }
            case "getPolygonExtents": {
                ScenePolygon geometry = this.getSceneObjectById((String)arg0, ScenePolygon.class, true);
                return geometry.extents;
            }
            case "getPolygonPlane": {
                ScenePolygon geometry = this.getSceneObjectById((String)arg0, ScenePolygon.class, true);
                return geometry.plane.name();
            }
            case "getModelIgnoreVehicleScale": {
                SceneModel sceneModel = this.getSceneObjectById((String)arg0, SceneModel.class, true);
                return sceneModel.ignoreVehicleScale ? Boolean.TRUE : Boolean.FALSE;
            }
            case "getModelScript": {
                int count = 0;
                for (int i = 0; i < this.objects.size(); ++i) {
                    SceneObject sceneObject = this.objects.get(i);
                    if (!(sceneObject instanceof SceneModel)) continue;
                    SceneModel sceneModel = (SceneModel)sceneObject;
                    if (count++ != ((Double)arg0).intValue()) continue;
                    return sceneModel.modelScript;
                }
                return null;
            }
            case "getModelSpriteModel": {
                SceneModel sceneModel = this.getSceneObjectById((String)arg0, SceneModel.class, true);
                return sceneModel.spriteModel;
            }
            case "getObjectAutoRotate": {
                SceneObject sceneObject = this.getSceneObjectById((String)arg0, true);
                return sceneObject.autoRotate ? Boolean.TRUE : Boolean.FALSE;
            }
            case "getObjectExists": {
                return this.getSceneObjectById((String)arg0, false) != null;
            }
            case "getObjectParent": {
                SceneObject sceneObject = this.getSceneObjectById((String)arg0, true);
                return sceneObject.parent == null ? null : sceneObject.parent.id;
            }
            case "getObjectParentAttachment": {
                SceneObject sceneObject = this.getSceneObjectById((String)arg0, true);
                return sceneObject.parentAttachment;
            }
            case "getObjectParentVehicle": {
                SceneObject sceneObject = this.getSceneObjectById((String)arg0, true);
                return sceneObject.parentVehiclePart == null ? null : sceneObject.parentVehiclePart.vehicle.id;
            }
            case "getObjectParentVehiclePart": {
                SceneObject sceneObject = this.getSceneObjectById((String)arg0, true);
                return sceneObject.parentVehiclePart == null ? null : sceneObject.parentVehiclePart.partId;
            }
            case "getObjectParentVehiclePartModel": {
                SceneObject sceneObject = this.getSceneObjectById((String)arg0, true);
                return sceneObject.parentVehiclePart == null ? null : sceneObject.parentVehiclePart.partModelId;
            }
            case "getObjectParentVehiclePartModelAttachment": {
                SceneObject sceneObject = this.getSceneObjectById((String)arg0, true);
                return sceneObject.parentVehiclePart == null ? null : sceneObject.parentVehiclePart.attachmentName;
            }
            case "getObjectRotation": {
                SceneObject sceneObject = this.getSceneObjectById((String)arg0, true);
                return sceneObject.rotate;
            }
            case "getObjectScale": {
                SceneObject sceneObject = this.getSceneObjectById((String)arg0, true);
                return sceneObject.scale;
            }
            case "getObjectTranslation": {
                SceneObject sceneObject = this.getSceneObjectById((String)arg0, true);
                return sceneObject.translate;
            }
            case "getVehicleScript": {
                SceneVehicle vehicle = this.getSceneObjectById((String)arg0, SceneVehicle.class, true);
                return vehicle.script;
            }
            case "isCharacterFemale": {
                SceneCharacter sceneCharacter = this.getSceneObjectById((String)arg0, SceneCharacter.class, true);
                return sceneCharacter.animatedModel.isFemale();
            }
            case "isObjectVisible": {
                SceneObject sceneObject = this.getSceneObjectById((String)arg0, true);
                return sceneObject.visible ? Boolean.TRUE : Boolean.FALSE;
            }
            case "moveCylinderToGround": {
                SceneCylinder object = this.getSceneObjectById((String)arg0, SceneCylinder.class, true);
                AABB aabb = object.getAABB(s_aabbPool.alloc());
                Boolean result = Boolean.FALSE;
                if (object.translate.y != aabb.h / 2.0f) {
                    object.translate.y = aabb.h / 2.0f;
                    result = Boolean.TRUE;
                }
                s_aabbPool.release(aabb);
                return result;
            }
            case "moveCylinderToOrigin": {
                SceneCylinder object = this.getSceneObjectById((String)arg0, SceneCylinder.class, true);
                AABB aabb = object.getAABB(s_aabbPool.alloc());
                object.translate.set(0.0f, aabb.h / 2.0f, 0.0f);
                s_aabbPool.release(aabb);
                return null;
            }
            case "recalculateBoxCenter": {
                SceneBox box = this.getSceneObjectById((String)arg0, SceneBox.class, true);
                Vector3f min = UI3DScene.allocVector3f().set(box.min);
                Vector3f max = UI3DScene.allocVector3f().set(box.max);
                Vector3f center = UI3DScene.allocVector3f().set(max).add(min).mul(0.5f).setComponent(1, min.y);
                org.joml.Matrix4f m = box.getLocalTransform(UI3DScene.allocMatrix4f());
                min.sub(center);
                max.sub(center);
                m.transformPosition(center);
                box.translate.set(center);
                box.getLocalTransform(m);
                m.invert();
                box.min.set(min);
                box.max.set(max);
                UI3DScene.releaseMatrix4f(m);
                UI3DScene.releaseVector3f(center);
                UI3DScene.releaseVector3f(max);
                UI3DScene.releaseVector3f(min);
                return null;
            }
            case "removeModel": {
                SceneModel sceneModel = this.getSceneObjectById((String)arg0, SceneModel.class, true);
                this.objects.remove(sceneModel);
                for (SceneObject sceneObject : this.objects) {
                    if (sceneObject.parent != sceneModel) continue;
                    sceneObject.attachment = null;
                    sceneObject.parent = null;
                    sceneObject.parentAttachment = null;
                }
                return null;
            }
            case "removeObject": {
                SceneObject object = this.getSceneObjectById((String)arg0, true);
                this.objects.remove(object);
                return null;
            }
            case "setDrawAttachments": {
                this.drawAttachments = (Boolean)arg0;
                return null;
            }
            case "setDrawGrid": {
                this.drawGrid = (Boolean)arg0;
                return null;
            }
            case "setDrawGridAxes": {
                this.drawGridAxes = (Boolean)arg0;
                return null;
            }
            case "setDrawGeometry": {
                this.drawGeometry = (Boolean)arg0;
                return null;
            }
            case "setDrawGridPlane": {
                this.drawGridPlane = (Boolean)arg0;
                return null;
            }
            case "setGizmoOrigin": {
                String origin;
                switch (origin = (String)arg0) {
                    case "none": {
                        this.gizmoParent = null;
                        this.gizmoOrigin = null;
                        this.gizmoChild = null;
                    }
                }
                return null;
            }
            case "setGizmoPos": {
                Vector3f newPos = (Vector3f)arg0;
                if (!this.gizmoPos.equals(newPos)) {
                    this.gizmoPos.set(newPos);
                }
                return null;
            }
            case "setGizmoRotate": {
                Vector3f newRot = (Vector3f)arg0;
                if (!this.gizmoRotate.equals(newRot)) {
                    this.gizmoRotate.set(newRot);
                }
                return null;
            }
            case "setGizmoScale": {
                this.gizmoScale = Math.max(((Double)arg0).floatValue(), 0.01f);
                return null;
            }
            case "setGizmoVisible": {
                String rst = (String)arg0;
                this.rotateGizmo.visible = "rotate".equalsIgnoreCase(rst);
                this.scaleGizmo.visible = "scale".equalsIgnoreCase(rst);
                this.translateGizmo.visible = "translate".equalsIgnoreCase(rst);
                switch (rst) {
                    case "rotate": {
                        this.gizmo = this.rotateGizmo;
                        break;
                    }
                    case "scale": {
                        this.gizmo = this.scaleGizmo;
                        break;
                    }
                    case "translate": {
                        this.gizmo = this.translateGizmo;
                        break;
                    }
                    default: {
                        this.gizmo = null;
                    }
                }
                return null;
            }
            case "setGridMult": {
                this.gridDivisions = PZMath.clamp(((Double)arg0).intValue(), 1, 100);
                return null;
            }
            case "setGridPlane": {
                this.gridPlane = GridPlane.valueOf((String)arg0);
                return null;
            }
            case "setMaxZoom": {
                this.zoomMax = PZMath.clamp(((Double)arg0).intValue(), 1, 20);
                return null;
            }
            case "setRotateGizmoSnap": {
                this.rotateGizmo.snap = (Boolean)arg0;
                return null;
            }
            case "setScaleGizmoSnap": {
                this.scaleGizmo.snap = (Boolean)arg0;
                return null;
            }
            case "setSelectedAttachment": {
                this.selectedAttachment = (String)arg0;
                return null;
            }
            case "setTransformMode": {
                this.transformMode = TransformMode.valueOf((String)arg0);
                return null;
            }
            case "setZoom": {
                this.zoom = PZMath.clamp(((Double)arg0).intValue(), 1, this.zoomMax);
                this.calcMatrices(this.projection, this.modelView);
                return null;
            }
            case "setView": {
                View old = this.view;
                this.view = View.valueOf((String)arg0);
                if (old != this.view) {
                    long ms = System.currentTimeMillis();
                    if (this.viewChangeTime + 350L < ms) {
                        this.modelViewChange.setFromUnnormalized(this.modelView);
                    }
                    this.viewChangeTime = ms;
                }
                this.calcMatrices(this.projection, this.modelView);
                return null;
            }
            case "zoom": {
                int delta = -((Double)arg0).intValue();
                float mouseX = Mouse.getXA() - this.getAbsoluteX().intValue();
                float mouseY = Mouse.getYA() - this.getAbsoluteY().intValue();
                float ox = this.uiToSceneX(mouseX, mouseY);
                float oy = this.uiToSceneY(mouseX, mouseY);
                this.zoom = PZMath.clamp(this.zoom + delta, 1, this.zoomMax);
                this.calcMatrices(this.projection, this.modelView);
                float nx = this.uiToSceneX(mouseX, mouseY);
                float ny = this.uiToSceneY(mouseX, mouseY);
                this.viewX = (int)((float)this.viewX - (nx - ox) * this.zoomMult());
                this.viewY = (int)((float)this.viewY + (ny - oy) * this.zoomMult());
                this.calcMatrices(this.projection, this.modelView);
                return null;
            }
        }
        throw new IllegalArgumentException(String.format("unhandled \"%s\" \"%s\"", func, arg0));
    }

    public Object fromLua2(String func, Object arg0, Object arg1) {
        switch (func) {
            case "addAttachment": {
                SceneModel sceneModel = this.getSceneObjectById((String)arg0, SceneModel.class, true);
                if (sceneModel.modelScript.getAttachmentById((String)((Object)arg1)) != null) {
                    throw new IllegalArgumentException("model script \"" + String.valueOf(arg0) + "\" already has attachment named \"" + String.valueOf(arg1) + "\"");
                }
                ModelAttachment attach = new ModelAttachment((String)((Object)arg1));
                sceneModel.modelScript.addAttachment(attach);
                return attach;
            }
            case "addBoneAxis": {
                PositionRotation axis = s_posRotPool.alloc();
                org.joml.Matrix4f mat = UI3DScene.allocMatrix4f().identity();
                mat.getTranslation(axis.pos);
                UI3DScene.releaseMatrix4f(mat);
                Quaternionf q = mat.getUnnormalizedRotation(UI3DScene.allocQuaternionf());
                q.getEulerAnglesXYZ(axis.rot);
                UI3DScene.releaseQuaternionf(q);
                this.axes.add(axis);
                return null;
            }
            case "addPolygonPoint": {
                ScenePolygon scenePolygon = this.getSceneObjectById((String)arg0, ScenePolygon.class, true);
                scenePolygon.points.add(new Vector2f((Vector2f)((Object)arg1)));
                scenePolygon.triangulate();
                return null;
            }
            case "applyDeltaRotation": {
                Vector3f eulerXYZ = (Vector3f)arg0;
                Vector3f deltaXYZ = (Vector3f)((Object)arg1);
                Quaternionf q0 = UI3DScene.allocQuaternionf().rotationXYZ(eulerXYZ.x * ((float)Math.PI / 180), eulerXYZ.y * ((float)Math.PI / 180), eulerXYZ.z * ((float)Math.PI / 180));
                Quaternionf q1 = UI3DScene.allocQuaternionf().rotationXYZ(deltaXYZ.x * ((float)Math.PI / 180), deltaXYZ.y * ((float)Math.PI / 180), deltaXYZ.z * ((float)Math.PI / 180));
                q0.mul(q1);
                q0.getEulerAnglesXYZ(eulerXYZ);
                UI3DScene.releaseQuaternionf(q0);
                UI3DScene.releaseQuaternionf(q1);
                eulerXYZ.mul(57.295776f);
                if (this.rotateGizmo.snap) {
                    eulerXYZ.x = (float)Math.floor(eulerXYZ.x + 0.5f);
                    eulerXYZ.y = (float)Math.floor(eulerXYZ.y + 0.5f);
                    eulerXYZ.z = (float)Math.floor(eulerXYZ.z + 0.5f);
                }
                return eulerXYZ;
            }
            case "cloneObject": {
                SceneObject sceneObject1 = this.getSceneObjectById((String)arg0, true);
                SceneObject sceneObject2 = this.getSceneObjectById((String)((Object)arg1), false);
                if (sceneObject2 != null) {
                    throw new IllegalStateException("scene object \"" + String.valueOf(arg1) + "\" exists");
                }
                sceneObject2 = sceneObject1.clone((String)((Object)arg1));
                this.objects.add(sceneObject2);
                return sceneObject2;
            }
            case "configDepthTexture": {
                SceneDepthTexture sceneDepthTexture = this.getSceneObjectById((String)arg0, SceneDepthTexture.class, true);
                sceneDepthTexture.texture = (Texture)((Object)arg1);
                return sceneDepthTexture;
            }
            case "copyGeometryFromSpriteGrid": {
                String modID = (String)arg0;
                String tileName = (String)((Object)arg1);
                IsoSprite sprite = IsoSpriteManager.instance.getSprite(tileName);
                IsoSpriteGrid spriteGrid = sprite.getSpriteGrid();
                int spriteGridIndex = spriteGrid.getSpriteIndex(sprite);
                for (int i = 0; i < spriteGrid.getSpriteCount(); ++i) {
                    IsoSprite sprite2;
                    if (i == spriteGridIndex || (sprite2 = spriteGrid.getSpriteFromIndex(i)) == null) continue;
                    String tilesetName = sprite2.tilesetName;
                    int col = sprite2.tileSheetIndex % 8;
                    int row = sprite2.tileSheetIndex / 8;
                    ArrayList<TileGeometryFile.Geometry> geometries = TileGeometryManager.getInstance().getGeometry(modID, tilesetName, col, row);
                    if (geometries == null || geometries.isEmpty()) continue;
                    tilesetName = sprite.tilesetName;
                    col = sprite.tileSheetIndex % 8;
                    row = sprite.tileSheetIndex / 8;
                    TileGeometryManager.getInstance().copyGeometry(modID, tilesetName, col, row, geometries);
                    float gx1 = spriteGrid.getSpriteGridPosX(sprite);
                    float gy1 = spriteGrid.getSpriteGridPosY(sprite);
                    float gz1 = (float)spriteGrid.getSpriteGridPosZ(sprite) * 2.4495f;
                    float gx2 = spriteGrid.getSpriteGridPosX(sprite2);
                    float gy2 = spriteGrid.getSpriteGridPosY(sprite2);
                    float gz2 = (float)spriteGrid.getSpriteGridPosZ(sprite2) * 2.4495f;
                    geometries = TileGeometryManager.getInstance().getGeometry(modID, tilesetName, col, row);
                    for (int j = 0; j < geometries.size(); ++j) {
                        TileGeometryFile.Geometry geometry = geometries.get(j);
                        geometry.offset(gx2 - gx1, gz2 - gz1, gy2 - gy1);
                    }
                    break;
                }
                return null;
            }
            case "createModel": {
                SceneObject sceneObject = this.getSceneObjectById((String)arg0, false);
                if (sceneObject != null) {
                    throw new IllegalStateException("scene object \"" + String.valueOf(arg0) + "\" exists");
                }
                ModelScript modelScript = ScriptManager.instance.getModelScript((String)((Object)arg1));
                if (modelScript == null) {
                    throw new NullPointerException("model script \"" + String.valueOf(arg1) + "\" not found");
                }
                Model model = ModelManager.instance.getLoadedModel((String)((Object)arg1));
                if (model == null) {
                    throw new NullPointerException("model \"" + String.valueOf(arg1) + "\" not found");
                }
                SceneModel sceneModel = new SceneModel(this, (String)arg0, modelScript, model);
                this.objects.add(sceneModel);
                return null;
            }
            case "dragGizmo": {
                float uiX = ((Double)arg0).floatValue();
                float uiY = ((Double)((Object)arg1)).floatValue();
                if (this.gizmo == null) {
                    throw new NullPointerException("gizmo is null");
                }
                this.gizmo.updateTracking(uiX, uiY);
                return null;
            }
            case "dragView": {
                int x = ((Double)arg0).intValue();
                int y = ((Double)((Object)arg1)).intValue();
                this.viewX -= x;
                this.viewY -= y;
                this.calcMatrices(this.projection, this.modelView);
                return null;
            }
            case "getCharacterAnimationKeyframeTimes": {
                SceneCharacter sceneCharacter = this.getSceneObjectById((String)arg0, SceneCharacter.class, true);
                AnimationPlayer animPlayer = sceneCharacter.animatedModel.getAnimationPlayer();
                if (animPlayer == null) {
                    return null;
                }
                AnimationMultiTrack multiTrack = animPlayer.getMultiTrack();
                if (multiTrack == null || multiTrack.getTracks().isEmpty()) {
                    return null;
                }
                AnimationTrack track = multiTrack.getTracks().get(0);
                AnimationClip clip = track.getClip();
                if (clip == null) {
                    return null;
                }
                if (arg1 == null) {
                    arg1 = new ArrayList();
                }
                ArrayList times = arg1;
                times.clear();
                Keyframe[] keyframes = clip.getKeyframes();
                for (int i = 0; i < keyframes.length; ++i) {
                    Keyframe keyframe = keyframes[i];
                    Double time = KahluaUtil.toDouble(keyframe.time);
                    if (times.contains(time)) continue;
                    times.add(time);
                }
                return times;
            }
            case "moveCylinderToOrigin": {
                SceneCylinder object = this.getSceneObjectById((String)arg0, SceneCylinder.class, true);
                String axis = (String)((Object)arg1);
                AABB aabb = object.getAABB(s_aabbPool.alloc());
                if (axis == null || "None".equalsIgnoreCase(axis)) {
                    object.translate.set(0.0f, aabb.h / 2.0f, 0.0f);
                } else if ("X".equalsIgnoreCase(axis)) {
                    object.translate.setComponent(0, 0.0f);
                } else if ("Y".equalsIgnoreCase(axis)) {
                    object.translate.setComponent(1, aabb.h / 2.0f);
                } else if ("Z".equalsIgnoreCase(axis)) {
                    object.translate.setComponent(2, 0.0f);
                }
                s_aabbPool.release(aabb);
                return null;
            }
            case "removeAttachment": {
                SceneModel sceneModel = this.getSceneObjectById((String)arg0, SceneModel.class, true);
                ModelAttachment attach = sceneModel.modelScript.getAttachmentById((String)((Object)arg1));
                if (attach == null) {
                    throw new IllegalArgumentException("model script \"" + String.valueOf(arg0) + "\" attachment \"" + String.valueOf(arg1) + "\" not found");
                }
                sceneModel.modelScript.removeAttachment(attach);
                return null;
            }
            case "removePolygonPoint": {
                ScenePolygon scenePolygon = this.getSceneObjectById((String)arg0, ScenePolygon.class, true);
                int pointIndex = ((Double)((Object)arg1)).intValue();
                if (scenePolygon.points.size() <= 3) {
                    return null;
                }
                scenePolygon.points.remove(pointIndex);
                scenePolygon.triangulate();
                return null;
            }
            case "setCharacterAlpha": {
                SceneCharacter character = this.getSceneObjectById((String)arg0, SceneCharacter.class, true);
                character.animatedModel.setAlpha(((Double)((Object)arg1)).floatValue());
                return null;
            }
            case "setCharacterAnimate": {
                SceneCharacter character = this.getSceneObjectById((String)arg0, SceneCharacter.class, true);
                character.animatedModel.setAnimate((Boolean)((Object)arg1));
                return null;
            }
            case "setCharacterAnimationClip": {
                SceneCharacter character = this.getSceneObjectById((String)arg0, SceneCharacter.class, true);
                AnimationSet animSet = AnimationSet.GetAnimationSet(character.animatedModel.GetAnimSetName(), false);
                if (animSet == null) {
                    return null;
                }
                AnimState state2 = animSet.GetState(character.animatedModel.getState());
                if (state2 == null || state2.nodes.isEmpty()) {
                    return null;
                }
                AnimNode node = state2.nodes.get(0);
                node.animName = (String)((Object)arg1);
                character.animatedModel.getAdvancedAnimator().OnAnimDataChanged(false);
                character.animatedModel.getAdvancedAnimator().setState(state2.name);
                return null;
            }
            case "setCharacterAnimationSpeed": {
                SceneCharacter character = this.getSceneObjectById((String)arg0, SceneCharacter.class, true);
                AnimationMultiTrack multiTrack = character.animatedModel.getAnimationPlayer().getMultiTrack();
                if (multiTrack.getTracks().isEmpty()) {
                    return null;
                }
                multiTrack.getTracks().get(0).setSpeedDelta(PZMath.clamp(((Double)((Object)arg1)).floatValue(), 0.0f, 10.0f));
                return null;
            }
            case "setCharacterAnimationTime": {
                SceneCharacter character = this.getSceneObjectById((String)arg0, SceneCharacter.class, true);
                character.animatedModel.setTrackTime(((Double)((Object)arg1)).floatValue());
                AnimationPlayer animPlayer = character.animatedModel.getAnimationPlayer();
                if (animPlayer == null) {
                    return null;
                }
                AnimationMultiTrack multiTrack = animPlayer.getMultiTrack();
                if (multiTrack == null || multiTrack.getTracks().isEmpty()) {
                    return null;
                }
                multiTrack.getTracks().get(0).setCurrentTimeValue(((Double)((Object)arg1)).floatValue());
                return null;
            }
            case "setCharacterAnimSet": {
                SceneCharacter character = this.getSceneObjectById((String)arg0, SceneCharacter.class, true);
                String animSet = (String)((Object)arg1);
                if (!animSet.equals(character.animatedModel.GetAnimSetName())) {
                    character.animatedModel.setAnimSetName(animSet);
                    character.animatedModel.getAdvancedAnimator().OnAnimDataChanged(false);
                    ActionGroup actionGroup = ActionGroup.getActionGroup(character.animatedModel.GetAnimSetName());
                    ActionContext actionContext = character.animatedModel.getActionContext();
                    if (actionGroup != actionContext.getGroup()) {
                        actionContext.setGroup(actionGroup);
                    }
                    character.animatedModel.getAdvancedAnimator().setState(actionContext.getCurrentStateName(), PZArrayUtil.listConvert(actionContext.getChildStates(), state -> state.getName()));
                }
                return null;
            }
            case "setCharacterClearDepthBuffer": {
                SceneCharacter character = this.getSceneObjectById((String)arg0, SceneCharacter.class, true);
                character.clearDepthBuffer = (Boolean)((Object)arg1);
                return null;
            }
            case "setCharacterFemale": {
                SceneCharacter character = this.getSceneObjectById((String)arg0, SceneCharacter.class, true);
                boolean bFemale = (Boolean)((Object)arg1);
                if (bFemale != character.animatedModel.isFemale()) {
                    character.animatedModel.setOutfitName("Naked", bFemale, false);
                }
                return null;
            }
            case "setCharacterShowBones": {
                SceneCharacter character = this.getSceneObjectById((String)arg0, SceneCharacter.class, true);
                character.showBones = (Boolean)((Object)arg1);
                return null;
            }
            case "setCharacterShowBip01": {
                SceneCharacter character = this.getSceneObjectById((String)arg0, SceneCharacter.class, true);
                character.showBip01 = (Boolean)((Object)arg1);
                return null;
            }
            case "setCharacterUseDeferredMovement": {
                SceneCharacter character = this.getSceneObjectById((String)arg0, SceneCharacter.class, true);
                character.useDeferredMovement = (Boolean)((Object)arg1);
                return null;
            }
            case "setCylinderHeight": {
                SceneCylinder object = this.getSceneObjectById((String)arg0, SceneCylinder.class, true);
                object.height = PZMath.clamp(((Double)((Object)arg1)).floatValue(), 0.01f, 2.44949f);
                return null;
            }
            case "setCylinderRadius": {
                SceneCylinder object = this.getSceneObjectById((String)arg0, SceneCylinder.class, true);
                object.radius = PZMath.clamp(((Double)((Object)arg1)).floatValue(), 0.01f, 10.0f);
                return null;
            }
            case "setGeometryExtents": {
                ScenePolygon scenePolygon = this.getSceneObjectById((String)arg0, ScenePolygon.class, true);
                scenePolygon.extents.set((Vector3f)((Object)arg1));
                return null;
            }
            case "setGeometrySelected": {
                SceneGeometry sceneGeometry = this.getSceneObjectById((String)arg0, SceneGeometry.class, true);
                sceneGeometry.selected = (Boolean)((Object)arg1);
                return null;
            }
            case "setPolygonEditing": {
                ScenePolygon scenePolygon = this.getSceneObjectById((String)arg0, ScenePolygon.class, true);
                scenePolygon.editing = (Boolean)((Object)arg1);
                return null;
            }
            case "setPolygonHighlightPoint": {
                ScenePolygon scenePolygon = this.getSceneObjectById((String)arg0, ScenePolygon.class, true);
                scenePolygon.highlightPointIndex = ((Double)((Object)arg1)).intValue();
                return null;
            }
            case "setPolygonPlane": {
                ScenePolygon scenePolygon = this.getSceneObjectById((String)arg0, ScenePolygon.class, true);
                scenePolygon.plane = GridPlane.valueOf((String)((Object)arg1));
                switch (scenePolygon.plane.ordinal()) {
                    case 0: {
                        scenePolygon.rotate.set(0.0f, 0.0f, 0.0f);
                        break;
                    }
                    case 1: {
                        scenePolygon.rotate.set(270.0f, 0.0f, 0.0f);
                        break;
                    }
                    case 2: {
                        scenePolygon.rotate.set(0.0f, 90.0f, 0.0f);
                    }
                }
                if (scenePolygon.points.isEmpty()) {
                    scenePolygon.points.add(new Vector2f(-0.5f, -0.5f));
                    scenePolygon.points.add(new Vector2f(0.5f, -0.5f));
                    scenePolygon.points.add(new Vector2f(0.5f, 0.5f));
                    scenePolygon.points.add(new Vector2f(-0.5f, 0.5f));
                }
                scenePolygon.triangulate();
                return null;
            }
            case "setGizmoAxisVisible": {
                Axis axis = Axis.valueOf((String)arg0);
                Boolean visible = (Boolean)((Object)arg1);
                switch (axis.ordinal()) {
                    case 1: {
                        this.gizmoAxisVisibleX = visible;
                        break;
                    }
                    case 2: {
                        this.gizmoAxisVisibleY = visible;
                        break;
                    }
                    case 3: {
                        this.gizmoAxisVisibleZ = visible;
                    }
                }
                return null;
            }
            case "setGizmoOrigin": {
                String origin;
                switch (origin = (String)arg0) {
                    case "centerOfMass": {
                        this.gizmoOrigin = this.gizmoParent = (SceneObject)this.getSceneObjectById((String)((Object)arg1), SceneVehicle.class, true);
                        this.gizmoChild = null;
                        break;
                    }
                    case "chassis": {
                        SceneVehicle vehicle = this.getSceneObjectById((String)((Object)arg1), SceneVehicle.class, true);
                        this.gizmoParent = vehicle;
                        this.originGizmo.translate.set(vehicle.script.getCenterOfMassOffset());
                        this.originGizmo.rotate.zero();
                        this.gizmoOrigin = this.originGizmo;
                        this.gizmoChild = null;
                        break;
                    }
                    case "character": {
                        this.gizmoOrigin = this.gizmoParent = (SceneObject)this.getSceneObjectById((String)((Object)arg1), SceneCharacter.class, true);
                        this.gizmoChild = null;
                        break;
                    }
                    case "model": {
                        this.gizmoOrigin = this.gizmoParent = (SceneObject)this.getSceneObjectById((String)((Object)arg1), SceneModel.class, true);
                        this.gizmoChild = null;
                        break;
                    }
                    case "object": {
                        this.gizmoOrigin = this.gizmoParent = this.getSceneObjectById((String)((Object)arg1), true);
                        this.gizmoChild = null;
                        break;
                    }
                    case "vehicleModel": {
                        SceneVehicle vehicle = this.getSceneObjectById((String)((Object)arg1), SceneVehicle.class, true);
                        this.gizmoParent = vehicle;
                        this.originGizmo.translate.set(vehicle.script.getModel().getOffset());
                        this.originGizmo.rotate.zero();
                        this.gizmoOrigin = this.originGizmo;
                        this.gizmoChild = null;
                        break;
                    }
                }
                return null;
            }
            case "setCharacterState": {
                SceneCharacter character = this.getSceneObjectById((String)arg0, SceneCharacter.class, true);
                character.animatedModel.setState((String)((Object)arg1));
                return null;
            }
            case "setHighlightBone": {
                String boneName;
                SceneObject sceneObject = this.getSceneObjectById((String)arg0, true);
                if (sceneObject instanceof SceneCharacter) {
                    SceneCharacter sceneCharacter = (SceneCharacter)sceneObject;
                    boneName = (String)((Object)arg1);
                    this.highlightBone.character = sceneCharacter;
                    this.highlightBone.sceneModel = null;
                    this.highlightBone.boneName = boneName;
                    this.highlightPartBone.vehicle = null;
                }
                if (sceneObject instanceof SceneModel) {
                    SceneModel sceneModel = (SceneModel)sceneObject;
                    boneName = (String)((Object)arg1);
                    this.highlightBone.character = null;
                    this.highlightBone.sceneModel = sceneModel;
                    this.highlightBone.boneName = boneName;
                    this.highlightPartBone.vehicle = null;
                }
                return null;
            }
            case "setModelIgnoreVehicleScale": {
                SceneModel sceneModel = this.getSceneObjectById((String)arg0, SceneModel.class, true);
                sceneModel.ignoreVehicleScale = (Boolean)((Object)arg1);
                return null;
            }
            case "setModelScript": {
                SceneModel sceneModel = this.getSceneObjectById((String)arg0, SceneModel.class, true);
                ModelScript modelScript = ScriptManager.instance.getModelScript((String)((Object)arg1));
                if (modelScript == null) {
                    throw new NullPointerException("model script \"" + String.valueOf(arg1) + "\" not found");
                }
                Model model = ModelManager.instance.getLoadedModel((String)((Object)arg1));
                if (model == null) {
                    throw new NullPointerException("model \"" + String.valueOf(arg1) + "\" not found");
                }
                sceneModel.modelScript = modelScript;
                sceneModel.model = model;
                return null;
            }
            case "setModelSpriteModel": {
                SceneModel sceneModel = this.getSceneObjectById((String)arg0, SceneModel.class, true);
                SpriteModel spriteModel = (SpriteModel)((Object)arg1);
                sceneModel.setSpriteModel(spriteModel);
                return null;
            }
            case "setModelSpriteModelEditor": {
                SceneModel sceneModel = this.getSceneObjectById((String)arg0, SceneModel.class, true);
                sceneModel.spriteModelEditor = (Boolean)((Object)arg1);
                return null;
            }
            case "setModelUseWorldAttachment": {
                SceneModel sceneModel = this.getSceneObjectById((String)arg0, SceneModel.class, true);
                sceneModel.useWorldAttachment = (Boolean)((Object)arg1);
                return null;
            }
            case "setModelWeaponRotationHack": {
                SceneModel sceneModel = this.getSceneObjectById((String)arg0, SceneModel.class, true);
                sceneModel.weaponRotationHack = (Boolean)((Object)arg1);
                return null;
            }
            case "setObjectAutoRotate": {
                SceneObject sceneObject = this.getSceneObjectById((String)arg0, true);
                sceneObject.autoRotate = (Boolean)((Object)arg1);
                if (!sceneObject.autoRotate) {
                    sceneObject.autoRotateAngle = 0.0f;
                }
                return null;
            }
            case "setObjectVisible": {
                SceneObject sceneObject = this.getSceneObjectById((String)arg0, true);
                sceneObject.visible = (Boolean)((Object)arg1);
                return null;
            }
            case "setVehicleScript": {
                SceneVehicle vehicle = this.getSceneObjectById((String)arg0, SceneVehicle.class, true);
                vehicle.setScriptName((String)((Object)arg1));
                return null;
            }
            case "subtractSpriteGridPixels": {
                String modID = (String)arg0;
                String tileName = (String)((Object)arg1);
                IsoSprite sprite = IsoSpriteManager.instance.getSprite(tileName);
                IsoSpriteGrid spriteGrid = sprite.getSpriteGrid();
                int spriteGridIndex = spriteGrid.getSpriteIndex(sprite);
                int gridPosX = spriteGrid.getSpriteGridPosX(sprite);
                int gridPosY = spriteGrid.getSpriteGridPosY(sprite);
                TileDepthTexture depthTexture = TileDepthTextureManager.getInstance().getTexture(modID, sprite.tilesetName, sprite.tileSheetIndex);
                for (int i = 0; i < spriteGrid.getSpriteCount(); ++i) {
                    Texture texture;
                    IsoSprite sprite2;
                    if (i == spriteGridIndex || (sprite2 = spriteGrid.getSpriteFromIndex(i)) == null || (texture = sprite2.getTextureForCurrentFrame(IsoDirections.N)) == null || texture.getMask() == null) continue;
                    int gridPosX2 = spriteGrid.getSpriteGridPosX(sprite2);
                    int gridPosY2 = spriteGrid.getSpriteGridPosY(sprite2);
                    int dx = gridPosX - gridPosX2;
                    int dy = gridPosY - gridPosY2;
                    for (int y = 0; y < 256; ++y) {
                        int y1 = y + dx * 32 + dy * 32;
                        for (int x = 0; x < 128; ++x) {
                            int x1 = x + dx * 64 - dy * 64;
                            if (!texture.isMaskSet(x1, y1)) continue;
                            depthTexture.setPixel(x, y, -1.0f);
                        }
                    }
                }
                depthTexture.updateGPUTexture();
                return null;
            }
            case "testGizmoAxis": {
                int x = ((Double)arg0).intValue();
                int y = ((Double)((Object)arg1)).intValue();
                if (this.gizmo == null) {
                    return "None";
                }
                StateData stateData = this.stateDataMain();
                this.setModelViewProjection(stateData);
                this.setGizmoTransforms(stateData);
                return this.gizmo.hitTest(x, y).toString();
            }
        }
        throw new IllegalArgumentException(String.format("unhandled \"%s\" \"%s\" \"%s\"", func, arg0, arg1));
    }

    public Object fromLua3(String func, Object arg0, Object arg1, Object arg2) {
        switch (func) {
            case "addAxis": {
                float x2 = ((Double)arg0).floatValue();
                float y2 = ((Double)arg1).floatValue();
                float z = ((Double)arg2).floatValue();
                this.axes.add(s_posRotPool.alloc().set(x2, y2, z));
                return null;
            }
            case "changeCylinderHeight": {
                SceneCylinder object = this.getSceneObjectById((String)arg0, SceneCylinder.class, true);
                String what = (String)arg1;
                float newHeight = PZMath.clamp(((Double)arg2).floatValue(), 0.01f, 2.44949f);
                org.joml.Matrix4f xfrm = object.getLocalTransform(UI3DScene.allocMatrix4f());
                Vector3f zAxis = xfrm.transformDirection(UI3DScene.allocVector3f().set(0.0f, 0.0f, 1.0f));
                if ("zMax".equalsIgnoreCase(what)) {
                    object.translate.add(zAxis.mul(newHeight - object.height).div(2.0f));
                } else if ("zMin".equalsIgnoreCase(what)) {
                    object.translate.add(zAxis.mul(-(newHeight - object.height)).div(2.0f));
                }
                object.height = newHeight;
                UI3DScene.releaseMatrix4f(xfrm);
                UI3DScene.releaseVector3f(zAxis);
                return null;
            }
            case "copyGeometryFrom": {
                String modID = (String)arg0;
                String tileNameDst = (String)arg1;
                String tileNameSrc = (String)arg2;
                IsoSprite spriteSrc = IsoSpriteManager.instance.getSprite(tileNameSrc);
                IsoSprite spriteDst = IsoSpriteManager.instance.getSprite(tileNameDst);
                String tilesetName = spriteSrc.tilesetName;
                int col = spriteSrc.tileSheetIndex % 8;
                int row = spriteSrc.tileSheetIndex / 8;
                ArrayList<TileGeometryFile.Geometry> geometries = TileGeometryManager.getInstance().getGeometry(modID, tilesetName, col, row);
                if (geometries == null || geometries.isEmpty()) {
                    return null;
                }
                tilesetName = spriteDst.tilesetName;
                col = spriteDst.tileSheetIndex % 8;
                row = spriteDst.tileSheetIndex / 8;
                TileGeometryManager.getInstance().copyGeometry(modID, tilesetName, col, row, geometries);
                return null;
            }
            case "createAnimal": {
                SceneObject sceneObject = this.getSceneObjectById((String)arg0, false);
                if (sceneObject != null) {
                    throw new IllegalStateException("scene object \"" + String.valueOf(arg0) + "\" exists");
                }
                SceneAnimal character = new SceneAnimal(this, (String)arg0, (AnimalDefinitions)arg1, (AnimalBreed)arg2);
                ((SceneCharacter)character).initAnimatedModel();
                this.objects.add(character);
                return character;
            }
            case "getGeometryDepthAt": {
                SceneGeometry sceneGeometry = this.getSceneObjectById((String)arg0, SceneGeometry.class, true);
                float tileX = ((Double)arg1).floatValue();
                float tileY = ((Double)arg2).floatValue();
                return (double)sceneGeometry.getNormalizedDepthAt(tileX, tileY);
            }
            case "getPolygonPoint": {
                ScenePolygon scenePolygon = this.getSceneObjectById((String)arg0, ScenePolygon.class, true);
                int pointIndex = ((Double)arg1).intValue();
                ((Vector2f)arg2).set(scenePolygon.points.get(pointIndex));
                return arg2;
            }
            case "pickCharacterBone": {
                SceneCharacter sceneCharacter = this.getSceneObjectById((String)arg0, SceneCharacter.class, true);
                float uiX = ((Double)arg1).floatValue();
                float uiY = ((Double)arg2).floatValue();
                return sceneCharacter.pickBone(uiX, uiY);
            }
            case "pickModelBone": {
                SceneModel sceneModel = this.getSceneObjectById((String)arg0, SceneModel.class, true);
                if (sceneModel.model.isStatic) {
                    return "";
                }
                float uiX = ((Double)arg1).floatValue();
                float uiY = ((Double)arg2).floatValue();
                return sceneModel.pickBone(uiX, uiY);
            }
            case "placeAttachmentAtOrigin": {
                SceneModel sceneModel = this.getSceneObjectById((String)arg0, SceneModel.class, true);
                ModelAttachment attach = sceneModel.modelScript.getAttachmentById((String)arg1);
                Boolean bWeaponRotationHack = (Boolean)arg2;
                if (attach == null) {
                    throw new IllegalArgumentException("model script \"" + String.valueOf(arg0) + "\" attachment \"" + String.valueOf(arg1) + "\" not found");
                }
                org.joml.Matrix4f transform = UI3DScene.allocMatrix4f();
                transform.identity();
                if (bWeaponRotationHack.booleanValue()) {
                    transform.rotateXYZ(0.0f, (float)Math.PI, 1.5707964f);
                }
                org.joml.Matrix4f attachmentXfrm = ModelInstanceRenderData.makeAttachmentTransform(attach, UI3DScene.allocMatrix4f());
                attachmentXfrm.invert();
                transform.mul(attachmentXfrm);
                transform.getTranslation(sceneModel.translate);
                Quaternionf rotation = transform.getUnnormalizedRotation(UI3DScene.allocQuaternionf());
                rotation.getEulerAnglesXYZ(sceneModel.rotate);
                sceneModel.rotate.mul(57.295776f);
                UI3DScene.releaseQuaternionf(rotation);
                UI3DScene.releaseMatrix4f(attachmentXfrm);
                UI3DScene.releaseMatrix4f(transform);
                return null;
            }
            case "polygonToUI": {
                ScenePolygon scenePolygon = this.getSceneObjectById((String)arg0, ScenePolygon.class, true);
                this.polygonEditor.setPlane(scenePolygon.translate, scenePolygon.rotate, scenePolygon.plane);
                this.polygonEditor.planeToUI((Vector2f)arg1, (Vector2f)arg2);
                return arg2;
            }
            case "rasterizePolygon": {
                ScenePolygon scenePolygon = this.getSceneObjectById((String)arg0, ScenePolygon.class, true);
                scenePolygon.rasterize((x, y) -> LuaManager.caller.protectedCallVoid(LuaManager.thread, arg1, arg2, BoxedStaticValues.toDouble(x), BoxedStaticValues.toDouble(y)));
                return null;
            }
            case "setAnimalDefinition": {
                SceneAnimal sceneAnimal = this.getSceneObjectById((String)arg0, SceneAnimal.class, true);
                sceneAnimal.setAnimalDefinition((AnimalDefinitions)arg1, (AnimalBreed)arg2);
                return null;
            }
            case "setAttachmentToOrigin": {
                SceneModel sceneModel = this.getSceneObjectById((String)arg0, SceneModel.class, true);
                ModelAttachment attach = sceneModel.modelScript.getAttachmentById((String)arg1);
                Boolean bWeaponRotationHack = (Boolean)arg2;
                if (attach == null) {
                    throw new IllegalArgumentException("model script \"" + String.valueOf(arg0) + "\" attachment \"" + String.valueOf(arg1) + "\" not found");
                }
                org.joml.Matrix4f transform = sceneModel.getGlobalTransform(UI3DScene.allocMatrix4f());
                if (bWeaponRotationHack.booleanValue()) {
                    org.joml.Matrix4f transform2 = UI3DScene.allocMatrix4f().rotationXYZ(0.0f, (float)Math.PI, 1.5707964f);
                    transform2.invert();
                    transform2.mul(transform, transform);
                    UI3DScene.releaseMatrix4f(transform2);
                }
                transform.invert();
                transform.getTranslation(attach.getOffset());
                Quaternionf rotation = transform.getUnnormalizedRotation(UI3DScene.allocQuaternionf());
                rotation.getEulerAnglesXYZ(attach.getRotate());
                attach.getRotate().mul(57.295776f);
                UI3DScene.releaseQuaternionf(rotation);
                UI3DScene.releaseMatrix4f(transform);
                return null;
            }
            case "setPolygonPoint": {
                ScenePolygon scenePolygon = this.getSceneObjectById((String)arg0, ScenePolygon.class, true);
                int pointIndex = ((Double)arg1).intValue();
                scenePolygon.points.get(pointIndex).set((Vector2f)arg2);
                scenePolygon.triangulate();
                return null;
            }
            case "setGizmoOrigin": {
                String origin;
                switch (origin = (String)arg0) {
                    case "bone": {
                        SceneObject sceneObject = this.getSceneObjectById((String)arg1, true);
                        if (sceneObject instanceof SceneCharacter) {
                            SceneCharacter sceneCharacter = (SceneCharacter)sceneObject;
                            this.gizmoParent = sceneCharacter;
                            this.originBone.character = sceneCharacter;
                            this.originBone.sceneModel = null;
                            this.originBone.boneName = (String)arg2;
                            this.gizmoOrigin = this.originBone;
                            this.gizmoChild = null;
                        }
                        if (!(sceneObject instanceof SceneModel)) break;
                        SceneModel sceneModel = (SceneModel)sceneObject;
                        this.gizmoParent = sceneModel;
                        this.originBone.character = null;
                        this.originBone.sceneModel = sceneModel;
                        this.originBone.boneName = (String)arg2;
                        this.gizmoOrigin = this.originBone;
                        this.gizmoChild = null;
                        break;
                    }
                    case "geometry": {
                        SceneGeometry sceneGeometry = this.getSceneObjectById((String)arg1, SceneGeometry.class, true);
                        this.gizmoParent = sceneGeometry;
                        this.originGeometry.sceneGeometry = sceneGeometry;
                        this.originGeometry.originHint = (String)arg2;
                        this.gizmoOrigin = this.originGeometry;
                        this.gizmoChild = null;
                        break;
                    }
                }
                return null;
            }
            case "setGizmoXYZ": {
                float x3 = ((Double)arg0).floatValue();
                float y3 = ((Double)arg1).floatValue();
                float z = ((Double)arg2).floatValue();
                this.gizmoPos.set(x3, y3, z);
                return null;
            }
            case "setShowVehiclePartBones": {
                SceneVehicle sceneVehicle = this.getSceneObjectById((String)arg0, SceneVehicle.class, true);
                String partId = (String)arg1;
                String partModelId = (String)arg2;
                sceneVehicle.showBonesPartId = partId;
                sceneVehicle.showBonesModelId = partModelId;
                return null;
            }
            case "startGizmoTracking": {
                float uiX = ((Double)arg0).floatValue();
                float uiY = ((Double)arg1).floatValue();
                Axis axis = Axis.valueOf((String)arg2);
                if (this.gizmo != null) {
                    this.gizmo.startTracking(uiX, uiY, axis);
                }
                return null;
            }
            case "setViewRotation": {
                float x4 = ((Double)arg0).floatValue();
                float y4 = ((Double)arg1).floatValue();
                float z = ((Double)arg2).floatValue();
                this.viewRotation.set(x4 %= 360.0f, y4 %= 360.0f, z %= 360.0f);
                return null;
            }
        }
        throw new IllegalArgumentException(String.format("unhandled \"%s\" \"%s\" \"%s\" \"%s\"", func, arg0, arg1, arg2));
    }

    public Object fromLua4(String func, Object arg0, Object arg1, Object arg2, Object arg3) {
        switch (func) {
            case "clearGeometry": {
                String modID = (String)arg0;
                String tilesetName = (String)arg1;
                int col = ((Double)arg2).intValue();
                int row = ((Double)arg3).intValue();
                TileGeometryManager.getInstance().setGeometry(modID, tilesetName, col, row, new ArrayList<TileGeometryFile.Geometry>());
                return null;
            }
            case "loadFromGeometryFile": {
                String modID = (String)arg0;
                String tilesetName = (String)arg1;
                int col = ((Double)arg2).intValue();
                int row = ((Double)arg3).intValue();
                for (int i = this.objects.size() - 1; i >= 0; --i) {
                    SceneObject object = this.objects.get(i);
                    if (Type.tryCastTo(object, SceneGeometry.class) == null) continue;
                    this.objects.remove(i);
                }
                ArrayList<TileGeometryFile.Geometry> geometries = TileGeometryManager.getInstance().getGeometry(modID, tilesetName, col, row);
                if (geometries == null) {
                    return null;
                }
                int n = 1;
                for (TileGeometryFile.Geometry tileGeometry : geometries) {
                    TileGeometryFile.Box tileBox = tileGeometry.asBox();
                    if (tileBox != null) {
                        SceneBox sceneBox = new SceneBox(this, "box" + n);
                        sceneBox.translate.set(tileBox.translate);
                        sceneBox.rotate.set(tileBox.rotate);
                        sceneBox.min.set(tileBox.min);
                        sceneBox.max.set(tileBox.max);
                        this.objects.add(n - 1, sceneBox);
                        ++n;
                        continue;
                    }
                    TileGeometryFile.Cylinder tileCylinder = tileGeometry.asCylinder();
                    if (tileCylinder != null) {
                        SceneCylinder sceneCylinder = new SceneCylinder(this, "cylinder" + n);
                        sceneCylinder.translate.set(tileCylinder.translate);
                        sceneCylinder.rotate.set(tileCylinder.rotate);
                        sceneCylinder.radius = Math.max(tileCylinder.radius1, tileCylinder.radius2);
                        sceneCylinder.height = tileCylinder.height;
                        this.objects.add(n - 1, sceneCylinder);
                        ++n;
                        continue;
                    }
                    TileGeometryFile.Polygon tilePolygon = tileGeometry.asPolygon();
                    if (tilePolygon == null) continue;
                    ScenePolygon scenePolygon = new ScenePolygon(this, "polygon" + n);
                    scenePolygon.translate.set(tilePolygon.translate);
                    scenePolygon.rotate.set(tilePolygon.rotate);
                    scenePolygon.plane = GridPlane.valueOf(tilePolygon.plane.name());
                    for (int i = 0; i < tilePolygon.points.size(); i += 2) {
                        scenePolygon.points.add(new Vector2f(tilePolygon.points.get(i), tilePolygon.points.get(i + 1)));
                    }
                    scenePolygon.triangulate();
                    this.objects.add(n - 1, scenePolygon);
                    ++n;
                }
                return null;
            }
            case "pickPolygonEdge": {
                ScenePolygon scenePolygon = this.getSceneObjectById((String)arg0, ScenePolygon.class, true);
                int index = scenePolygon.pickEdge(((Double)arg1).floatValue(), ((Double)arg2).floatValue(), ((Double)arg3).floatValue());
                return BoxedStaticValues.toDouble(index);
            }
            case "pickPolygonPoint": {
                ScenePolygon scenePolygon = this.getSceneObjectById((String)arg0, ScenePolygon.class, true);
                int index = scenePolygon.pickPoint(((Double)arg1).floatValue(), ((Double)arg2).floatValue(), ((Double)arg3).floatValue());
                return BoxedStaticValues.toDouble(index);
            }
            case "setGizmoOrigin": {
                String origin;
                switch (origin = (String)arg0) {
                    case "attachment": {
                        SceneObject sceneObject = this.getSceneObjectById((String)arg1, true);
                        this.originAttachment.object = this.gizmoParent = this.getSceneObjectById((String)arg2, true);
                        this.originAttachment.attachmentName = (String)arg3;
                        this.gizmoOrigin = this.originAttachment;
                        this.gizmoChild = sceneObject;
                        break;
                    }
                }
                return null;
            }
            case "setHighlightPartBone": {
                this.highlightPartBone.vehicle = this.getSceneObjectById((String)arg0, SceneVehicle.class, true);
                this.highlightPartBone.partId = (String)arg1;
                this.highlightPartBone.partModelId = (String)arg2;
                this.highlightPartBone.attachmentName = (String)arg3;
                this.highlightBone.character = null;
                this.highlightBone.sceneModel = null;
                this.highlightBone.boneName = null;
                return null;
            }
            case "setObjectParent": {
                SceneObject sceneObject1 = this.getSceneObjectById((String)arg0, true);
                sceneObject1.translate.zero();
                sceneObject1.rotate.zero();
                sceneObject1.attachment = (String)arg1;
                sceneObject1.parent = this.getSceneObjectById((String)arg2, false);
                sceneObject1.parentAttachment = (String)arg3;
                if (sceneObject1.parent != null && sceneObject1.parent.parent == sceneObject1) {
                    sceneObject1.parent.parent = null;
                }
                sceneObject1.parentVehiclePart = null;
                return null;
            }
            case "setObjectPosition": {
                SceneObject sceneObject = this.getSceneObjectById((String)arg0, true);
                sceneObject.translate.set(((Double)arg1).floatValue(), ((Double)arg2).floatValue(), ((Double)arg3).floatValue());
                return null;
            }
            case "setPassengerPosition": {
                SceneCharacter character = this.getSceneObjectById((String)arg0, SceneCharacter.class, true);
                SceneVehicle vehicle = this.getSceneObjectById((String)arg1, SceneVehicle.class, true);
                VehicleScript.Passenger pngr = vehicle.script.getPassengerById((String)arg2);
                if (pngr == null) {
                    return null;
                }
                VehicleScript.Position pos = pngr.getPositionById((String)arg3);
                if (pos != null) {
                    this.tempVector3f.set(vehicle.script.getModel().getOffset());
                    this.tempVector3f.add(pos.getOffset());
                    character.translate.set(this.tempVector3f);
                    character.rotate.set(pos.rotate);
                    character.parent = vehicle;
                    if (character.animatedModel != null) {
                        String animSet;
                        String string = animSet = "inside".equalsIgnoreCase(pos.getId()) ? "player-vehicle" : "player-editor";
                        if (!animSet.equals(character.animatedModel.GetAnimSetName())) {
                            character.animatedModel.setAnimSetName(animSet);
                            character.animatedModel.getAdvancedAnimator().OnAnimDataChanged(false);
                            ActionGroup actionGroup = ActionGroup.getActionGroup(character.animatedModel.GetAnimSetName());
                            ActionContext actionContext = character.animatedModel.getActionContext();
                            if (actionGroup != actionContext.getGroup()) {
                                actionContext.setGroup(actionGroup);
                            }
                            character.animatedModel.getAdvancedAnimator().setState(actionContext.getCurrentStateName(), PZArrayUtil.listConvert(actionContext.getChildStates(), state -> state.getName()));
                        }
                    }
                }
                return null;
            }
            case "uiToPolygonPoint": {
                ScenePolygon scenePolygon = this.getSceneObjectById((String)arg0, ScenePolygon.class, true);
                this.polygonEditor.setPlane(scenePolygon.translate, scenePolygon.rotate, scenePolygon.plane);
                this.polygonEditor.uiToPlane2D(((Double)arg1).floatValue(), ((Double)arg2).floatValue(), (Vector2f)arg3);
                return arg3;
            }
            case "uiToGrid": {
                float uiX = ((Double)arg0).floatValue();
                float uiY = ((Double)arg1).floatValue();
                return this.uiToGrid(uiX, uiY, GridPlane.valueOf((String)arg2), (Vector3f)arg3) ? Boolean.TRUE : Boolean.FALSE;
            }
            case "updateGeometryFile": {
                String modID = (String)arg0;
                String tilesetName = (String)arg1;
                int col = ((Double)arg2).intValue();
                int row = ((Double)arg3).intValue();
                ArrayList<TileGeometryFile.Geometry> geometries = new ArrayList<TileGeometryFile.Geometry>();
                for (SceneObject object : this.objects) {
                    SceneGeometry sceneGeometry;
                    TileGeometryFile.Geometry tileGeometry;
                    if (!(object instanceof SceneGeometry) || (tileGeometry = (sceneGeometry = (SceneGeometry)object).toGeometryFileObject()) == null) continue;
                    geometries.add(tileGeometry);
                }
                TileGeometryManager.getInstance().setGeometry(modID, tilesetName, col, row, geometries);
                geometries.clear();
                return null;
            }
        }
        throw new IllegalArgumentException(String.format("unhandled \"%s\" \"%s\" \"%s\" \"%s\" \"%s\"", func, arg0, arg1, arg2, arg3));
    }

    public Object fromLua5(String func, Object arg0, Object arg1, Object arg2, Object arg3, Object arg4) {
        switch (func) {
            case "addPolygonPointOnEdge": {
                ScenePolygon scenePolygon = this.getSceneObjectById((String)arg0, ScenePolygon.class, true);
                float mouseX = ((Double)arg1).floatValue();
                float mouseY = ((Double)arg2).floatValue();
                float pointX = ((Double)arg3).floatValue();
                float pointY = ((Double)arg4).floatValue();
                return BoxedStaticValues.toDouble(scenePolygon.addPointOnEdge(mouseX, mouseY, pointX, pointY));
            }
            case "pickVehiclePartBone": {
                SceneVehicle sceneVehicle = this.getSceneObjectById((String)arg0, SceneVehicle.class, true);
                String partId = (String)arg1;
                String partModelId = (String)arg2;
                float uiX = ((Double)arg3).floatValue();
                float uiY = ((Double)arg4).floatValue();
                VehicleScript.Part part = sceneVehicle.script.getPartById(partId);
                VehicleScript.Model model = part.getModelById(partModelId);
                return sceneVehicle.pickBone(part, model, uiX, uiY);
            }
            case "setObjectParentToVehiclePart": {
                SceneObject sceneObject1 = this.getSceneObjectById((String)arg1, true);
                sceneObject1.translate.zero();
                sceneObject1.rotate.zero();
                sceneObject1.parent = null;
                sceneObject1.attachment = null;
                sceneObject1.parentAttachment = null;
                sceneObject1.parentVehiclePart = new ParentVehiclePart();
                sceneObject1.parentVehiclePart.vehicle = this.getSceneObjectById((String)arg0, SceneVehicle.class, true);
                sceneObject1.parentVehiclePart.partId = (String)arg2;
                sceneObject1.parentVehiclePart.partModelId = (String)arg3;
                sceneObject1.parentVehiclePart.attachmentName = (String)arg4;
                return null;
            }
        }
        throw new IllegalArgumentException(String.format("unhandled \"%s\" \"%s\" \"%s\" \"%s\" \"%s\" \"%s\"", func, arg0, arg1, arg2, arg3, arg4));
    }

    public Object fromLua6(String func, Object arg0, Object arg1, Object arg2, Object arg3, Object arg4, Object arg5) {
        switch (func) {
            case "addAABB": {
                float x = ((Double)arg0).floatValue();
                float y = ((Double)arg1).floatValue();
                float z = ((Double)arg2).floatValue();
                float w = ((Double)arg3).floatValue();
                float h = ((Double)arg4).floatValue();
                float l = ((Double)arg5).floatValue();
                this.aabb.add(s_aabbPool.alloc().set(x, y, z, w, h, l, 1.0f, 1.0f, 1.0f, 1.0f, false));
                return null;
            }
            case "addAxis": {
                float x = ((Double)arg0).floatValue();
                float y = ((Double)arg1).floatValue();
                float z = ((Double)arg2).floatValue();
                float rx = ((Double)arg3).floatValue();
                float ry = ((Double)arg4).floatValue();
                float rz = ((Double)arg5).floatValue();
                this.axes.add(s_posRotPool.alloc().set(x, y, z, rx, ry, rz));
                return null;
            }
            case "addAxisRelativeToOrigin": {
                float x = ((Double)arg0).floatValue();
                float y = ((Double)arg1).floatValue();
                float z = ((Double)arg2).floatValue();
                float rx = ((Double)arg3).floatValue();
                float ry = ((Double)arg4).floatValue();
                float rz = ((Double)arg5).floatValue();
                this.axes.add(s_posRotPool.alloc().set(x, y, z, rx, ry, rz));
                this.axes.get((int)(this.axes.size() - 1)).relativeToOrigin = true;
                return null;
            }
            case "addBox3D": {
                Vector3f offset = (Vector3f)arg0;
                Vector3f extents = (Vector3f)arg1;
                Vector3f rotate = (Vector3f)arg2;
                float r = ((Double)arg3).floatValue();
                float g = ((Double)arg4).floatValue();
                float b = ((Double)arg5).floatValue();
                this.box3d.add(s_box3DPool.alloc().set(offset.x, offset.y, offset.z, -extents.x / 2.0f, -extents.y / 2.0f, -extents.z / 2.0f, extents.x / 2.0f, extents.y / 2.0f, extents.z / 2.0f, rotate.x, rotate.y, rotate.z, r, g, b, 1.0f, false));
                return null;
            }
            case "getAdjacentSeatingPosition": {
                String modID = (String)arg0;
                SceneCharacter sceneCharacter = this.getSceneObjectById((String)arg1, SceneCharacter.class, true);
                String spriteName = (String)arg2;
                String sitDirection = (String)arg3;
                String side = (String)arg4;
                IsoSprite sprite = IsoSpriteManager.instance.namedMap.get(spriteName);
                Vector2f localPos = (Vector2f)arg5;
                boolean valid = SeatingManager.getInstance().getAdjacentPosition(modID, sprite, sitDirection, side, sceneCharacter.animatedModel.getAnimationPlayer().getModel(), "player", "sitonfurniture", "SitOnFurniture" + side, localPos);
                if (valid) {
                    Vector3f xln = SeatingManager.getInstance().getTranslation(modID, sprite, sitDirection, new Vector3f());
                    localPos.sub(xln.x(), xln.y());
                    localPos.add(sceneCharacter.translate.x, sceneCharacter.translate.z);
                }
                return valid ? Boolean.TRUE : Boolean.FALSE;
            }
            case "shiftGeometryByPixels": {
                String modID = (String)arg0;
                String tilesetName = (String)arg1;
                int col = ((Double)arg2).intValue();
                int row = ((Double)arg3).intValue();
                int shiftX = ((Double)arg4).intValue();
                int shiftY = ((Double)arg5).intValue();
                ArrayList<TileGeometryFile.Geometry> geometries = TileGeometryManager.getInstance().getGeometry(modID, tilesetName, col, row);
                if (geometries == null) {
                    return null;
                }
                int tileWidth = 128;
                float onePixel = 0.015625f;
                for (TileGeometryFile.Geometry tileGeometry : geometries) {
                    TileGeometryFile.Box tileBox = tileGeometry.asBox();
                    if (tileBox != null) {
                        tileBox.translate.add((float)shiftX * 0.0078125f, 0.0f, (float)(-shiftX) * 0.0078125f);
                        continue;
                    }
                    TileGeometryFile.Cylinder tileCylinder = tileGeometry.asCylinder();
                    if (tileCylinder != null) {
                        tileCylinder.translate.add((float)shiftX * 0.0078125f, 0.0f, (float)(-shiftX) * 0.0078125f);
                        continue;
                    }
                    TileGeometryFile.Polygon tilePolygon = tileGeometry.asPolygon();
                    if (tilePolygon == null) continue;
                    tilePolygon.translate.add((float)shiftX * 0.0078125f, 0.0f, (float)(-shiftX) * 0.0078125f);
                }
                return null;
            }
            case "renderSpriteGridTextureMask": {
                SpriteGridTextureMaskDrawer drawer = new SpriteGridTextureMaskDrawer();
                drawer.scene = this;
                drawer.sx = ((Double)arg0).floatValue();
                drawer.sy = ((Double)arg1).floatValue();
                drawer.sx2 = ((Double)arg2).floatValue();
                drawer.sy2 = ((Double)arg3).floatValue();
                drawer.pixelSize = ((Double)arg4).floatValue();
                drawer.sprite = (IsoSprite)arg5;
                drawer.r = 1.0f;
                drawer.g = 0.0f;
                drawer.b = 0.0f;
                drawer.a = 1.0f;
                SpriteRenderer.instance.drawGeneric(drawer);
                return null;
            }
            case "renderTextureMask": {
                TextureMaskDrawer drawer = new TextureMaskDrawer();
                drawer.scene = this;
                drawer.sx = ((Double)arg0).floatValue();
                drawer.sy = ((Double)arg1).floatValue();
                drawer.sx2 = ((Double)arg2).floatValue();
                drawer.sy2 = ((Double)arg3).floatValue();
                drawer.pixelSize = ((Double)arg4).floatValue();
                drawer.texture = (Texture)arg5;
                drawer.r = 1.0f;
                drawer.g = 0.0f;
                drawer.b = 0.0f;
                drawer.a = 1.0f;
                SpriteRenderer.instance.drawGeneric(drawer);
                return null;
            }
            case "setGizmoOrigin": {
                String origin;
                switch (origin = (String)arg0) {
                    case "vehiclePart": {
                        SceneVehicle sceneVehicle = this.getSceneObjectById((String)arg1, SceneVehicle.class, true);
                        this.gizmoParent = sceneVehicle;
                        this.originVehiclePart.vehicle = sceneVehicle;
                        this.originVehiclePart.partId = (String)arg2;
                        this.originVehiclePart.partModelId = (String)arg3;
                        this.originVehiclePart.attachmentName = (String)arg4;
                        this.originVehiclePart.boneOnly = (Boolean)arg5;
                        this.gizmoOrigin = this.originVehiclePart;
                        this.gizmoChild = null;
                        break;
                    }
                }
                return null;
            }
        }
        throw new IllegalArgumentException(String.format("unhandled \"%s\" \"%s\" \"%s\" \"%s\" \"%s\" \"%s\" \"%s\"", func, arg0, arg1, arg2, arg3, arg4, arg5));
    }

    public Object fromLua7(String func, Object arg0, Object arg1, Object arg2, Object arg3, Object arg4, Object arg5, Object arg6) {
        switch (func) {
            case "addBox3D": {
                Vector3f translate = (Vector3f)arg0;
                Vector3f rotate = (Vector3f)arg1;
                Vector3f min = (Vector3f)arg2;
                Vector3f max = (Vector3f)arg3;
                float r = ((Double)arg4).floatValue();
                float g = ((Double)arg5).floatValue();
                float b = ((Double)arg6).floatValue();
                this.box3d.add(s_box3DPool.alloc().set(translate.x, translate.y, translate.z, min.x, min.y, min.z, max.x, max.y, max.z, rotate.x, rotate.y, rotate.z, r, g, b, 1.0f, false));
                return null;
            }
            case "addPhysicsMesh": {
                Vector3f offset = (Vector3f)arg0;
                Vector3f rotate = (Vector3f)arg1;
                float scale = ((Double)arg2).floatValue();
                String physicsShapeScriptName = (String)arg3;
                float r = ((Double)arg4).floatValue();
                float g = ((Double)arg5).floatValue();
                float b = ((Double)arg6).floatValue();
                this.physicsMesh.add(s_physicsMeshPool.alloc().set(offset, rotate, scale, physicsShapeScriptName, r, g, b));
                return null;
            }
            case "createDepthTexture": {
                SceneObject sceneObject = this.getSceneObjectById((String)arg0, false);
                if (sceneObject != null) {
                    throw new IllegalStateException("scene object \"" + String.valueOf(arg0) + "\" exists");
                }
                SceneDepthTexture sceneDepthTexture = new SceneDepthTexture(this, (String)arg0);
                sceneDepthTexture.texture = (Texture)arg1;
                for (int i = 0; i < this.objects.size(); ++i) {
                    if (this.objects.get(i) instanceof SceneDepthTexture) continue;
                    this.objects.add(i, sceneDepthTexture);
                    return sceneDepthTexture;
                }
                this.objects.add(sceneDepthTexture);
                return sceneDepthTexture;
            }
        }
        throw new IllegalArgumentException(String.format("unhandled \"%s\" \"%s\" \"%s\" \"%s\" \"%s\" \"%s\" \"%s\" \"%s\"", func, arg0, arg1, arg2, arg3, arg4, arg5, arg6));
    }

    public Object fromLua9(String func, Object arg0, Object arg1, Object arg2, Object arg3, Object arg4, Object arg5, Object arg6, Object arg7, Object arg8) {
        switch (func) {
            case "addAABB": {
                float x = ((Double)arg0).floatValue();
                float y = ((Double)arg1).floatValue();
                float z = ((Double)arg2).floatValue();
                float w = ((Double)arg3).floatValue();
                float h = ((Double)arg4).floatValue();
                float l = ((Double)arg5).floatValue();
                float r = ((Double)arg6).floatValue();
                float g = ((Double)arg7).floatValue();
                float b = ((Double)arg8).floatValue();
                this.aabb.add(s_aabbPool.alloc().set(x, y, z, w, h, l, r, g, b, 1.0f, false));
                return null;
            }
            case "addBox3D": {
                Vector3f translate = (Vector3f)arg0;
                Vector3f rotate = (Vector3f)arg1;
                Vector3f min = (Vector3f)arg2;
                Vector3f max = (Vector3f)arg3;
                float r = ((Double)arg4).floatValue();
                float g = ((Double)arg5).floatValue();
                float b = ((Double)arg6).floatValue();
                float a = ((Double)arg7).floatValue();
                boolean bQuads = (Boolean)arg8;
                this.box3d.add(s_box3DPool.alloc().set(translate.x, translate.y, translate.z, min.x, min.y, min.z, max.x, max.y, max.z, rotate.x, rotate.y, rotate.z, r, g, b, a, bQuads));
                return null;
            }
        }
        throw new IllegalArgumentException(String.format("unhandled \"%s\" \"%s\" \"%s\" \"%s\" \"%s\" \"%s\" \"%s\" \"%s\" \"%s\" \"%s\"", func, arg0, arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8));
    }

    private int screenWidth() {
        return (int)this.width;
    }

    private int screenHeight() {
        return (int)this.height;
    }

    public float uiToSceneX(float uiX, float uiY) {
        float sceneX = uiX - (float)this.screenWidth() / 2.0f;
        sceneX += (float)this.viewX;
        return sceneX /= this.zoomMult();
    }

    public float uiToSceneY(float uiX, float uiY) {
        float sceneY = uiY - (float)this.screenHeight() / 2.0f;
        sceneY *= -1.0f;
        sceneY -= (float)this.viewY;
        return sceneY /= this.zoomMult();
    }

    public Vector3f uiToScene(float uiX, float uiY, float uiZ, Vector3f out) {
        this.uiToScene(null, uiX, uiY, uiZ, out);
        switch (this.view.ordinal()) {
            case 0: 
            case 1: {
                out.x = 0.0f;
                break;
            }
            case 2: 
            case 3: {
                out.y = 0.0f;
                break;
            }
            case 4: 
            case 5: {
                out.z = 0.0f;
            }
        }
        if (this.view == View.UserDefined) {
            Vector3f orientation = UI3DScene.allocVector3f();
            switch (this.gridPlane.ordinal()) {
                case 0: {
                    orientation.set(0.0f, 0.0f, 1.0f);
                    break;
                }
                case 1: {
                    orientation.set(0.0f, 1.0f, 0.0f);
                    break;
                }
                case 2: {
                    orientation.set(1.0f, 0.0f, 0.0f);
                }
            }
            Vector3f center = UI3DScene.allocVector3f().set(0.0f);
            Plane plane = UI3DScene.allocPlane().set(orientation, center);
            UI3DScene.releaseVector3f(orientation);
            UI3DScene.releaseVector3f(center);
            Ray cameraRay = this.getCameraRay(uiX, (float)this.screenHeight() - uiY, UI3DScene.allocRay());
            if (UI3DScene.intersect_ray_plane(plane, cameraRay, out) != 1) {
                out.set(0.0f);
            }
            UI3DScene.releasePlane(plane);
            UI3DScene.releaseRay(cameraRay);
        }
        return out;
    }

    public Vector3f uiToScene(org.joml.Matrix4f modelTransform, float uiX, float uiY, float uiZ, Vector3f out) {
        uiY = (float)this.screenHeight() - uiY;
        org.joml.Matrix4f matrix4f = UI3DScene.allocMatrix4f();
        matrix4f.set(this.projection);
        matrix4f.mul(this.modelView);
        if (modelTransform != null) {
            matrix4f.mul(modelTransform);
        }
        matrix4f.invert();
        this.viewport[2] = this.screenWidth();
        this.viewport[3] = this.screenHeight();
        matrix4f.unprojectInv(uiX, uiY, uiZ, this.viewport, out);
        UI3DScene.releaseMatrix4f(matrix4f);
        return out;
    }

    public float sceneToUIX(float sceneX, float sceneY, float sceneZ) {
        org.joml.Matrix4f matrix4f = UI3DScene.allocMatrix4f();
        matrix4f.set(this.projection);
        matrix4f.mul(this.modelView);
        this.viewport[2] = this.screenWidth();
        this.viewport[3] = this.screenHeight();
        matrix4f.project(sceneX, sceneY, sceneZ, this.viewport, this.tempVector3f);
        UI3DScene.releaseMatrix4f(matrix4f);
        return this.tempVector3f.x();
    }

    public float sceneToUIY(float sceneX, float sceneY, float sceneZ) {
        org.joml.Matrix4f matrix4f = UI3DScene.allocMatrix4f();
        matrix4f.set(this.projection);
        matrix4f.mul(this.modelView);
        this.viewport[2] = this.screenWidth();
        this.viewport[3] = this.screenHeight();
        matrix4f.project(sceneX, sceneY, sceneZ, this.viewport, this.tempVector3f);
        UI3DScene.releaseMatrix4f(matrix4f);
        return (float)this.screenHeight() - this.tempVector3f.y();
    }

    public float sceneToUIX(Vector3f scenePos) {
        return this.sceneToUIX(scenePos.x, scenePos.y, scenePos.z);
    }

    public float sceneToUIY(Vector3f scenePos) {
        return this.sceneToUIY(scenePos.x, scenePos.y, scenePos.z);
    }

    public boolean uiToGrid(float uiX, float uiY, GridPlane gridPlane, Vector3f outScenePos) {
        boolean hitPlane;
        Plane plane = UI3DScene.allocPlane();
        plane.point.set(0.0f);
        switch (gridPlane.ordinal()) {
            case 0: {
                plane.normal.set(0.0f, 0.0f, 1.0f);
                break;
            }
            case 1: {
                plane.normal.set(0.0f, 1.0f, 0.0f);
                break;
            }
            case 2: {
                plane.normal.set(1.0f, 0.0f, 0.0f);
            }
        }
        Ray cameraRay = this.getCameraRay(uiX, (float)this.screenHeight() - uiY, UI3DScene.allocRay());
        boolean bl = hitPlane = UI3DScene.intersect_ray_plane(plane, cameraRay, outScenePos) == 1;
        if (!hitPlane) {
            outScenePos.set(0.0f);
        }
        UI3DScene.releasePlane(plane);
        UI3DScene.releaseRay(cameraRay);
        return hitPlane;
    }

    private void renderGridXY(int div) {
        int y;
        int i;
        int x;
        for (x = -5; x < 5; ++x) {
            for (i = 1; i < div; ++i) {
                vboRenderer.addLine((float)x + (float)i / (float)div, -5.0f, 0.0f, (float)x + (float)i / (float)div, 5.0f, 0.0f, 0.2f, 0.2f, 0.2f, this.gridAlpha);
            }
        }
        for (y = -5; y < 5; ++y) {
            for (i = 1; i < div; ++i) {
                vboRenderer.addLine(-5.0f, (float)y + (float)i / (float)div, 0.0f, 5.0f, (float)y + (float)i / (float)div, 0.0f, 0.2f, 0.2f, 0.2f, this.gridAlpha);
            }
        }
        for (x = -5; x <= 5; ++x) {
            vboRenderer.addLine(x, -5.0f, 0.0f, x, 5.0f, 0.0f, 0.1f, 0.1f, 0.1f, this.gridAlpha);
        }
        for (y = -5; y <= 5; ++y) {
            vboRenderer.addLine(-5.0f, y, 0.0f, 5.0f, y, 0.0f, 0.1f, 0.1f, 0.1f, this.gridAlpha);
        }
        if (this.drawGridAxes) {
            boolean z = false;
            vboRenderer.addLine(-5.0f, 0.0f, 0.0f, 5.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f, this.gridAlpha);
            z = false;
            vboRenderer.addLine(0.0f, -5.0f, 0.0f, 0.0f, 5.0f, 0.0f, 0.0f, 1.0f, 0.0f, this.gridAlpha);
        }
    }

    private void renderGridXZ(int div) {
        int z;
        int i;
        int x;
        for (x = -5; x < 5; ++x) {
            for (i = 1; i < div; ++i) {
                vboRenderer.addLine((float)x + (float)i / (float)div, 0.0f, -5.0f, (float)x + (float)i / (float)div, 0.0f, 5.0f, 0.2f, 0.2f, 0.2f, this.gridAlpha);
            }
        }
        for (z = -5; z < 5; ++z) {
            for (i = 1; i < div; ++i) {
                vboRenderer.addLine(-5.0f, 0.0f, (float)z + (float)i / (float)div, 5.0f, 0.0f, (float)z + (float)i / (float)div, 0.2f, 0.2f, 0.2f, this.gridAlpha);
            }
        }
        for (x = -5; x <= 5; ++x) {
            vboRenderer.addLine(x, 0.0f, -5.0f, x, 0.0f, 5.0f, 0.1f, 0.1f, 0.1f, this.gridAlpha);
        }
        for (z = -5; z <= 5; ++z) {
            vboRenderer.addLine(-5.0f, 0.0f, z, 5.0f, 0.0f, z, 0.1f, 0.1f, 0.1f, this.gridAlpha);
        }
        if (this.drawGridAxes) {
            z = 0;
            vboRenderer.addLine(-5.0f, 0.0f, 0.0f, 5.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f, this.gridAlpha);
            x = 0;
            vboRenderer.addLine(0.0f, 0.0f, -5.0f, 0.0f, 0.0f, 5.0f, 0.0f, 0.0f, 1.0f, this.gridAlpha);
        }
    }

    private void renderGridYZ(int div) {
        int z;
        int i;
        int y;
        for (y = -5; y < 5; ++y) {
            for (i = 1; i < div; ++i) {
                vboRenderer.addLine(0.0f, (float)y + (float)i / (float)div, -5.0f, 0.0f, (float)y + (float)i / (float)div, 5.0f, 0.2f, 0.2f, 0.2f, this.gridAlpha);
            }
        }
        for (z = -5; z < 5; ++z) {
            for (i = 1; i < div; ++i) {
                vboRenderer.addLine(0.0f, -5.0f, (float)z + (float)i / (float)div, 0.0f, 5.0f, (float)z + (float)i / (float)div, 0.2f, 0.2f, 0.2f, this.gridAlpha);
            }
        }
        for (y = -5; y <= 5; ++y) {
            vboRenderer.addLine(0.0f, y, -5.0f, 0.0f, y, 5.0f, 0.1f, 0.1f, 0.1f, this.gridAlpha);
        }
        for (z = -5; z <= 5; ++z) {
            vboRenderer.addLine(0.0f, -5.0f, z, 0.0f, 5.0f, z, 0.1f, 0.1f, 0.1f, this.gridAlpha);
        }
        if (this.drawGridAxes) {
            z = 0;
            vboRenderer.addLine(0.0f, -5.0f, 0.0f, 0.0f, 5.0f, 0.0f, 0.0f, 1.0f, 0.0f, this.gridAlpha);
            y = 0;
            vboRenderer.addLine(0.0f, 0.0f, -5.0f, 0.0f, 0.0f, 5.0f, 0.0f, 0.0f, 1.0f, this.gridAlpha);
        }
    }

    private void renderGrid() {
        vboRenderer.startRun(UI3DScene.vboRenderer.formatPositionColor);
        vboRenderer.setMode(1);
        vboRenderer.setLineWidth(1.0f);
        this.gridAlpha = 1.0f;
        long ms = System.currentTimeMillis();
        if (this.viewChangeTime + 350L > ms) {
            float f = (float)(this.viewChangeTime + 350L - ms) / 350.0f;
            this.gridAlpha = 1.0f - f;
            this.gridAlpha *= this.gridAlpha;
        }
        switch (this.view.ordinal()) {
            case 0: 
            case 1: {
                this.renderGridYZ(10);
                vboRenderer.endRun();
                return;
            }
            case 4: 
            case 5: {
                this.renderGridXY(10);
                vboRenderer.endRun();
                return;
            }
            case 2: 
            case 3: {
                this.renderGridXZ(10);
                vboRenderer.endRun();
                return;
            }
        }
        switch (this.gridPlane.ordinal()) {
            case 0: {
                this.renderGridXY(10);
                vboRenderer.endRun();
                break;
            }
            case 1: {
                this.renderGridXZ(10);
                vboRenderer.endRun();
                break;
            }
            case 2: {
                this.renderGridYZ(10);
                vboRenderer.endRun();
            }
        }
    }

    void renderAxis(PositionRotation axis) {
        this.renderAxis(axis.pos, axis.rot, axis.relativeToOrigin);
    }

    void renderAxis(Vector3f pos, Vector3f rot, boolean bRelativeToOrigin) {
        StateData stateData = this.stateDataRender();
        vboRenderer.flush();
        org.joml.Matrix4f matrix4f = UI3DScene.allocMatrix4f().identity();
        if (!bRelativeToOrigin) {
            matrix4f.mul(stateData.gizmoParentTransform);
            matrix4f.mul(stateData.gizmoOriginTransform);
            matrix4f.mul(stateData.gizmoChildTransform);
            if (stateData.selectedAttachmentIsChildAttachment) {
                matrix4f.mul(stateData.gizmoChildAttachmentTransformInv);
            }
        }
        matrix4f.translate(pos);
        matrix4f.rotateXYZ(rot.x * ((float)Math.PI / 180), rot.y * ((float)Math.PI / 180), rot.z * ((float)Math.PI / 180));
        stateData.modelView.mul(matrix4f, matrix4f);
        PZGLUtil.pushAndLoadMatrix(5888, matrix4f);
        UI3DScene.releaseMatrix4f(matrix4f);
        float length = 0.1f;
        Model.debugDrawAxis(0.0f, 0.0f, 0.0f, 0.1f, 3.0f);
        PZGLUtil.popMatrix(5888);
    }

    private void renderAABB(float x, float y, float z, float xMin, float yMin, float zMin, float xMax, float yMax, float zMax, float r, float g, float b, float a, boolean bQuads) {
        vboRenderer.addAABB(x, y, z, xMin, yMin, zMin, xMax, yMax, zMax, r, g, b, a, bQuads);
    }

    private void renderAABB(float x, float y, float z, Vector3f min, Vector3f max, float r, float g, float b) {
        vboRenderer.addAABB(x, y, z, min, max, r, g, b);
    }

    private void renderBox3D(float x, float y, float z, float xMin, float yMin, float zMin, float xMax, float yMax, float zMax, float rx, float ry, float rz, float r, float g, float b, float a, boolean bQuads) {
        StateData stateData = this.stateDataRender();
        vboRenderer.flush();
        org.joml.Matrix4f matrix4f = UI3DScene.allocMatrix4f();
        matrix4f.identity();
        matrix4f.translate(x, y, z);
        matrix4f.rotateXYZ(rx * ((float)Math.PI / 180), ry * ((float)Math.PI / 180), rz * ((float)Math.PI / 180));
        stateData.modelView.mul(matrix4f, matrix4f);
        PZGLUtil.pushAndLoadMatrix(5888, matrix4f);
        UI3DScene.releaseMatrix4f(matrix4f);
        this.renderAABB(x * 0.0f, y * 0.0f, z * 0.0f, xMin, yMin, zMin, xMax, yMax, zMax, r, g, b, a, bQuads);
        vboRenderer.flush();
        PZGLUtil.popMatrix(5888);
    }

    private void renderPhysicsMesh(float x, float y, float z, float rx, float ry, float rz, float r, float g, float b, float[] points) {
        StateData stateData = this.stateDataRender();
        vboRenderer.flush();
        org.joml.Matrix4f matrix4f = UI3DScene.allocMatrix4f();
        matrix4f.identity();
        matrix4f.translate(x, y, z);
        matrix4f.rotateXYZ(rx * ((float)Math.PI / 180), ry * ((float)Math.PI / 180), rz * ((float)Math.PI / 180));
        stateData.modelView.mul(matrix4f, matrix4f);
        PZGLUtil.pushAndLoadMatrix(5888, matrix4f);
        UI3DScene.releaseMatrix4f(matrix4f);
        vboRenderer.startRun(UI3DScene.vboRenderer.formatPositionColor);
        vboRenderer.setMode(1);
        for (int i = 0; i < points.length / 3 - 1; ++i) {
            int i1 = i * 3;
            int i2 = (i + 1) * 3;
            vboRenderer.addLine(points[i1], points[i1 + 1], points[i1 + 2], points[i2], points[i2 + 1], points[i2 + 2], r, g, b, 1.0f);
        }
        vboRenderer.endRun();
        vboRenderer.flush();
        PZGLUtil.popMatrix(5888);
    }

    private void calcMatrices(org.joml.Matrix4f projection, org.joml.Matrix4f modelView) {
        float w = this.screenWidth();
        float scale = 1366.0f / w;
        float h = (float)this.screenHeight() * scale;
        w = 1366.0f;
        projection.setOrtho(-(w /= this.zoomMult()) / 2.0f, w / 2.0f, -(h /= this.zoomMult()) / 2.0f, h / 2.0f, -10.0f, 10.0f);
        float viewX = (float)this.viewX / this.zoomMult() * scale;
        float viewY = (float)this.viewY / this.zoomMult() * scale;
        projection.translate(-viewX, viewY, 0.0f);
        modelView.identity();
        float rotateX = 0.0f;
        float rotateY = 0.0f;
        float rotateZ = 0.0f;
        switch (this.view.ordinal()) {
            case 0: {
                rotateY = 270.0f;
                break;
            }
            case 1: {
                rotateY = 90.0f;
                break;
            }
            case 4: {
                break;
            }
            case 5: {
                rotateY = 180.0f;
                break;
            }
            case 2: {
                rotateY = 90.0f;
                rotateZ = 90.0f;
                break;
            }
            case 3: {
                rotateY = 90.0f;
                rotateZ = 270.0f;
                break;
            }
            case 6: {
                rotateX = this.viewRotation.x;
                rotateY = this.viewRotation.y;
                rotateZ = this.viewRotation.z;
            }
        }
        modelView.rotateXYZ(rotateX * ((float)Math.PI / 180), rotateY * ((float)Math.PI / 180), rotateZ * ((float)Math.PI / 180));
    }

    Ray getCameraRay(float uiX, float uiY, Ray cameraRay) {
        return this.getCameraRay(uiX, uiY, this.projection, this.modelView, cameraRay);
    }

    Ray getCameraRay(float uiX, float uiY, org.joml.Matrix4f projection, org.joml.Matrix4f modelView, Ray cameraRay) {
        return this.getCameraRay(uiX, uiY, projection, modelView, this.screenWidth(), this.screenHeight(), cameraRay);
    }

    Ray getCameraRay(float uiX, float uiY, org.joml.Matrix4f projection, org.joml.Matrix4f modelView, int viewWidth, int viewHeight, Ray cameraRay) {
        org.joml.Matrix4f matrix4f = UI3DScene.allocMatrix4f();
        matrix4f.set(projection);
        matrix4f.mul(modelView);
        matrix4f.invert();
        this.viewport[2] = viewWidth;
        this.viewport[3] = viewHeight;
        Vector3f rayStart = matrix4f.unprojectInv(uiX, uiY, 0.0f, this.viewport, UI3DScene.allocVector3f());
        Vector3f rayEnd = matrix4f.unprojectInv(uiX, uiY, 1.0f, this.viewport, UI3DScene.allocVector3f());
        cameraRay.origin.set(rayStart);
        cameraRay.direction.set(rayEnd.sub(rayStart).normalize());
        UI3DScene.releaseVector3f(rayEnd);
        UI3DScene.releaseVector3f(rayStart);
        UI3DScene.releaseMatrix4f(matrix4f);
        return cameraRay;
    }

    public static float closest_distance_between_lines(Ray l1, Ray l2) {
        float tc;
        float sc;
        Vector3f u = UI3DScene.allocVector3f().set(l1.direction);
        Vector3f v = UI3DScene.allocVector3f().set(l2.direction);
        Vector3f w = UI3DScene.allocVector3f().set(l1.origin).sub(l2.origin);
        float a = u.dot(u);
        float b = u.dot(v);
        float c = v.dot(v);
        float d = u.dot(w);
        float e = v.dot(w);
        float det = a * c - b * b;
        if (det < 1.0E-8f) {
            sc = 0.0f;
            tc = b > c ? d / b : e / c;
        } else {
            sc = (b * e - c * d) / det;
            tc = (a * e - b * d) / det;
        }
        Vector3f dP = w.add(u.mul(sc)).sub(v.mul(tc));
        l1.t = sc;
        l2.t = tc;
        UI3DScene.releaseVector3f(u);
        UI3DScene.releaseVector3f(v);
        UI3DScene.releaseVector3f(w);
        return dP.length();
    }

    static Vector3f project(Vector3f a, Vector3f b, Vector3f out) {
        return out.set(b).mul(a.dot(b) / b.dot(b));
    }

    static Vector3f reject(Vector3f a, Vector3f b, Vector3f out) {
        Vector3f p = UI3DScene.project(a, b, UI3DScene.allocVector3f());
        out.set(a).sub(p);
        UI3DScene.releaseVector3f(p);
        return out;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public static int intersect_ray_plane(Plane pn, Ray s, Vector3f out) {
        Vector3f u = UI3DScene.allocVector3f().set(s.direction).mul(100.0f);
        Vector3f w = UI3DScene.allocVector3f().set(s.origin).sub(pn.point);
        try {
            float d = pn.normal.dot(u);
            float n = -pn.normal.dot(w);
            if (Math.abs(d) < 1.0E-8f) {
                if (n == 0.0f) {
                    int n2 = 2;
                    return n2;
                }
                int n3 = 0;
                return n3;
            }
            float sI = n / d;
            if (sI < 0.0f || sI > 1.0f) {
                int n4 = 0;
                return n4;
            }
            out.set(s.origin).add(u.mul(sI));
            int n5 = 1;
            return n5;
        }
        finally {
            UI3DScene.releaseVector3f(u);
            UI3DScene.releaseVector3f(w);
        }
    }

    public static float distance_between_point_ray(Vector3f p, Ray l) {
        Vector3f v = UI3DScene.allocVector3f().set(l.direction).mul(100.0f);
        Vector3f w = UI3DScene.allocVector3f().set(p).sub(l.origin);
        float c1 = w.dot(v);
        float c2 = v.dot(v);
        float b = c1 / c2;
        Vector3f pb = v.mul(b).add(l.origin);
        float dist = pb.sub(p).length();
        UI3DScene.releaseVector3f(w);
        UI3DScene.releaseVector3f(v);
        return dist;
    }

    public static float closest_distance_line_circle(Ray ray, Circle c, Vector3f point) {
        float dist;
        Vector3f onPlane;
        Plane plane = UI3DScene.allocPlane().set(c.orientation, c.center);
        if (UI3DScene.intersect_ray_plane(plane, ray, onPlane = UI3DScene.allocVector3f()) == 1) {
            point.set(onPlane).sub(c.center).normalize().mul(c.radius).add(c.center);
            dist = onPlane.sub(point).length();
        } else {
            Vector3f a = UI3DScene.allocVector3f().set(ray.origin).sub(c.center);
            Vector3f rt = UI3DScene.reject(a, c.orientation, UI3DScene.allocVector3f());
            point.set(rt.normalize().mul(c.radius).add(c.center));
            dist = UI3DScene.distance_between_point_ray(point, ray);
            UI3DScene.releaseVector3f(rt);
            UI3DScene.releaseVector3f(a);
        }
        UI3DScene.releaseVector3f(onPlane);
        UI3DScene.releasePlane(plane);
        return dist;
    }

    private StateData stateDataMain() {
        return this.stateData[SpriteRenderer.instance.getMainStateIndex()];
    }

    private StateData stateDataRender() {
        return this.stateData[SpriteRenderer.instance.getRenderStateIndex()];
    }

    static {
        s_posRotPool = new ObjectPool<PositionRotation>(PositionRotation::new);
        s_aabbPool = new ObjectPool<AABB>(AABB::new);
        s_box3DPool = new ObjectPool<Box3D>(Box3D::new);
        s_physicsMeshPool = new ObjectPool<PhysicsMesh>(PhysicsMesh::new);
        TL_Ray_pool = ThreadLocal.withInitial(RayObjectPool::new);
        TL_Plane_pool = ThreadLocal.withInitial(PlaneObjectPool::new);
    }

    private static enum View {
        Left,
        Right,
        Top,
        Bottom,
        Front,
        Back,
        UserDefined;

    }

    private static enum TransformMode {
        Global,
        Local;

    }

    public static enum GridPlane {
        XY,
        XZ,
        YZ;

    }

    private final class CharacterSceneModelCamera
    extends SceneModelCamera {
        final /* synthetic */ UI3DScene this$0;

        private CharacterSceneModelCamera(UI3DScene uI3DScene) {
            UI3DScene uI3DScene2 = uI3DScene;
            Objects.requireNonNull(uI3DScene2);
            this.this$0 = uI3DScene2;
            super(uI3DScene);
        }

        @Override
        public void Begin() {
            StateData stateData = this.this$0.stateDataRender();
            GL11.glViewport(this.this$0.getAbsoluteX().intValue(), Core.getInstance().getScreenHeight() - this.this$0.getAbsoluteY().intValue() - this.this$0.getHeight().intValue(), this.this$0.getWidth().intValue(), this.this$0.getHeight().intValue());
            PZGLUtil.pushAndLoadMatrix(5889, stateData.projection);
            org.joml.Matrix4f matrix4f = UI3DScene.allocMatrix4f();
            matrix4f.set(stateData.modelView);
            matrix4f.mul(this.renderData.transform);
            PZGLUtil.pushAndLoadMatrix(5888, matrix4f);
            UI3DScene.releaseMatrix4f(matrix4f);
        }

        @Override
        public void End() {
            PZGLUtil.popMatrix(5889);
            PZGLUtil.popMatrix(5888);
        }
    }

    private final class VehicleSceneModelCamera
    extends SceneModelCamera {
        final /* synthetic */ UI3DScene this$0;

        private VehicleSceneModelCamera(UI3DScene uI3DScene) {
            UI3DScene uI3DScene2 = uI3DScene;
            Objects.requireNonNull(uI3DScene2);
            this.this$0 = uI3DScene2;
            super(uI3DScene);
        }

        @Override
        public void Begin() {
            StateData stateData = this.this$0.stateDataRender();
            GL11.glViewport(this.this$0.getAbsoluteX().intValue(), Core.getInstance().getScreenHeight() - this.this$0.getAbsoluteY().intValue() - this.this$0.getHeight().intValue(), this.this$0.getWidth().intValue(), this.this$0.getHeight().intValue());
            PZGLUtil.pushAndLoadMatrix(5889, stateData.projection);
            org.joml.Matrix4f matrix4f = UI3DScene.allocMatrix4f();
            matrix4f.set(stateData.modelView);
            matrix4f.mul(this.renderData.transform);
            PZGLUtil.pushAndLoadMatrix(5888, matrix4f);
            UI3DScene.releaseMatrix4f(matrix4f);
            GL11.glDepthRange(0.0, 1.0);
            GL11.glDepthMask(true);
        }

        @Override
        public void End() {
            PZGLUtil.popMatrix(5889);
            PZGLUtil.popMatrix(5888);
        }
    }

    private static final class StateData {
        final org.joml.Matrix4f projection = new org.joml.Matrix4f();
        final org.joml.Matrix4f modelView = new org.joml.Matrix4f();
        int zoom;
        GridPlaneDrawer gridPlaneDrawer;
        OverlaysDrawer overlaysDrawer;
        final ArrayList<SceneObjectRenderData> objectData = new ArrayList();
        Gizmo gizmo;
        final Vector3f gizmoTranslate = new Vector3f();
        final Vector3f gizmoRotate = new Vector3f();
        final org.joml.Matrix4f gizmoParentTransform = new org.joml.Matrix4f();
        final org.joml.Matrix4f gizmoOriginTransform = new org.joml.Matrix4f();
        final org.joml.Matrix4f gizmoChildTransform = new org.joml.Matrix4f();
        final org.joml.Matrix4f gizmoChildAttachmentTransform = new org.joml.Matrix4f();
        final org.joml.Matrix4f gizmoChildAttachmentTransformInv = new org.joml.Matrix4f();
        final org.joml.Matrix4f gizmoTransform = new org.joml.Matrix4f();
        boolean hasGizmoOrigin;
        boolean gizmoOriginIsGeometry;
        boolean selectedAttachmentIsChildAttachment;
        Axis gizmoAxis = Axis.None;
        final TranslateGizmoRenderData translateGizmoRenderData = new TranslateGizmoRenderData();
        final ArrayList<PositionRotation> axes = new ArrayList();
        final ArrayList<AABB> aabb = new ArrayList();
        final ArrayList<Box3D> box3d = new ArrayList();
        final ArrayList<PhysicsMesh> physicsMesh = new ArrayList();

        private StateData() {
        }

        private float zoomMult() {
            return (float)Math.exp((float)this.zoom * 0.2f) * 160.0f / Math.max(1.82f, 1.0f);
        }
    }

    private final class RotateGizmo
    extends Gizmo {
        Axis trackAxis;
        boolean snap;
        final Circle trackCircle;
        final org.joml.Matrix4f startXfrm;
        final org.joml.Matrix4f startInvXfrm;
        final Vector3f startPointOnCircle;
        final Vector3f currentPointOnCircle;
        final ArrayList<Vector3f> circlePointsMain;
        final ArrayList<Vector3f> circlePointsRender;
        final /* synthetic */ UI3DScene this$0;

        private RotateGizmo(UI3DScene uI3DScene) {
            UI3DScene uI3DScene2 = uI3DScene;
            Objects.requireNonNull(uI3DScene2);
            this.this$0 = uI3DScene2;
            super(uI3DScene);
            this.trackAxis = Axis.None;
            this.snap = true;
            this.trackCircle = new Circle();
            this.startXfrm = new org.joml.Matrix4f();
            this.startInvXfrm = new org.joml.Matrix4f();
            this.startPointOnCircle = new Vector3f();
            this.currentPointOnCircle = new Vector3f();
            this.circlePointsMain = new ArrayList();
            this.circlePointsRender = new ArrayList();
        }

        @Override
        Axis hitTest(float uiX, float uiY) {
            if (!this.visible) {
                return Axis.None;
            }
            StateData stateData = this.this$0.stateDataMain();
            this.this$0.setModelViewProjection(stateData);
            this.this$0.setGizmoTransforms(stateData);
            uiY = (float)this.this$0.screenHeight() - uiY;
            Ray cameraRay = this.this$0.getCameraRay(uiX, uiY, UI3DScene.allocRay());
            org.joml.Matrix4f gizmoXfrm = UI3DScene.allocMatrix4f();
            gizmoXfrm.set(stateData.gizmoParentTransform);
            gizmoXfrm.mul(stateData.gizmoOriginTransform);
            gizmoXfrm.mul(stateData.gizmoChildTransform);
            if (stateData.selectedAttachmentIsChildAttachment) {
                gizmoXfrm.mul(stateData.gizmoChildAttachmentTransformInv);
            }
            gizmoXfrm.mul(stateData.gizmoTransform);
            Vector3f scale = gizmoXfrm.getScale(UI3DScene.allocVector3f());
            gizmoXfrm.scale(1.0f / scale.x, 1.0f / scale.y, 1.0f / scale.z);
            UI3DScene.releaseVector3f(scale);
            if (this.this$0.transformMode == TransformMode.Global) {
                gizmoXfrm.setRotationXYZ(0.0f, 0.0f, 0.0f);
            }
            float scale2 = this.this$0.gizmoScale / stateData.zoomMult() * 1000.0f;
            float radius = 0.5f * scale2;
            Vector3f center = gizmoXfrm.transformProject(UI3DScene.allocVector3f().set(0.0f, 0.0f, 0.0f));
            Vector3f xAxis = gizmoXfrm.transformDirection(UI3DScene.allocVector3f().set(1.0f, 0.0f, 0.0f)).normalize();
            Vector3f yAxis = gizmoXfrm.transformDirection(UI3DScene.allocVector3f().set(0.0f, 1.0f, 0.0f)).normalize();
            Vector3f zAxis = gizmoXfrm.transformDirection(UI3DScene.allocVector3f().set(0.0f, 0.0f, 1.0f)).normalize();
            Vector2 point = UI3DScene.allocVector2();
            this.getCircleSegments(center, radius, yAxis, zAxis, this.circlePointsMain);
            float distX = this.hitTestCircle(cameraRay, this.circlePointsMain, point);
            BaseVehicle.TL_vector3f_pool.get().release(this.circlePointsMain);
            this.circlePointsMain.clear();
            this.getCircleSegments(center, radius, xAxis, zAxis, this.circlePointsMain);
            float distY = this.hitTestCircle(cameraRay, this.circlePointsMain, point);
            BaseVehicle.TL_vector3f_pool.get().release(this.circlePointsMain);
            this.circlePointsMain.clear();
            this.getCircleSegments(center, radius, xAxis, yAxis, this.circlePointsMain);
            float distZ = this.hitTestCircle(cameraRay, this.circlePointsMain, point);
            BaseVehicle.TL_vector3f_pool.get().release(this.circlePointsMain);
            this.circlePointsMain.clear();
            UI3DScene.releaseVector2(point);
            UI3DScene.releaseVector3f(xAxis);
            UI3DScene.releaseVector3f(yAxis);
            UI3DScene.releaseVector3f(zAxis);
            UI3DScene.releaseVector3f(center);
            UI3DScene.releaseRay(cameraRay);
            UI3DScene.releaseMatrix4f(gizmoXfrm);
            float dist = 8.0f;
            if (distX < distY && distX < distZ) {
                return distX <= 8.0f ? Axis.X : Axis.None;
            }
            if (distY < distX && distY < distZ) {
                return distY <= 8.0f ? Axis.Y : Axis.None;
            }
            if (distZ < distX && distZ < distY) {
                return distZ <= 8.0f ? Axis.Z : Axis.None;
            }
            return Axis.None;
        }

        @Override
        void startTracking(float uiX, float uiY, Axis axis) {
            StateData stateData = this.this$0.stateDataMain();
            this.this$0.setModelViewProjection(stateData);
            this.this$0.setGizmoTransforms(stateData);
            this.startXfrm.set(stateData.gizmoParentTransform);
            this.startXfrm.mul(stateData.gizmoOriginTransform);
            this.startXfrm.mul(stateData.gizmoChildTransform);
            if (!stateData.selectedAttachmentIsChildAttachment) {
                this.startXfrm.mul(stateData.gizmoTransform);
            }
            if (this.this$0.transformMode == TransformMode.Global) {
                this.startXfrm.setRotationXYZ(0.0f, 0.0f, 0.0f);
            }
            this.startInvXfrm.set(stateData.gizmoParentTransform);
            this.startInvXfrm.mul(stateData.gizmoOriginTransform);
            this.startInvXfrm.mul(stateData.gizmoChildTransform);
            if (!stateData.selectedAttachmentIsChildAttachment) {
                this.startInvXfrm.mul(stateData.gizmoTransform);
            }
            this.startInvXfrm.invert();
            this.trackAxis = axis;
            this.getPointOnAxis(uiX, uiY, axis, this.trackCircle, this.startXfrm, this.startPointOnCircle);
        }

        @Override
        void updateTracking(float uiX, float uiY) {
            Vector3f pos = this.getPointOnAxis(uiX, uiY, this.trackAxis, this.trackCircle, this.startXfrm, UI3DScene.allocVector3f());
            if (this.currentPointOnCircle.equals(pos)) {
                UI3DScene.releaseVector3f(pos);
                return;
            }
            this.currentPointOnCircle.set(pos);
            UI3DScene.releaseVector3f(pos);
            float angle = this.calculateRotation(this.startPointOnCircle, this.currentPointOnCircle, this.trackCircle);
            if (GameKeyboard.isKeyDown(29)) {
                angle = angle > 0.0f ? (float)((int)(angle / 5.0f) * 5) : (float)(Math.round(-angle / 5.0f) * -5);
            }
            switch (this.trackAxis.ordinal()) {
                case 1: {
                    this.trackCircle.orientation.set(1.0f, 0.0f, 0.0f);
                    break;
                }
                case 2: {
                    this.trackCircle.orientation.set(0.0f, 1.0f, 0.0f);
                    break;
                }
                case 3: {
                    this.trackCircle.orientation.set(0.0f, 0.0f, 1.0f);
                }
            }
            Vector3f orientation = UI3DScene.allocVector3f().set(this.trackCircle.orientation);
            Ray cameraRay = this.this$0.getCameraRay(uiX, uiY, UI3DScene.allocRay());
            Vector3f orientation2 = this.startXfrm.transformDirection(UI3DScene.allocVector3f().set(orientation)).normalize();
            float dot = cameraRay.direction.dot(orientation2);
            UI3DScene.releaseVector3f(orientation2);
            UI3DScene.releaseRay(cameraRay);
            if (this.this$0.gizmoParent instanceof SceneCharacter) {
                if (dot > 0.0f) {
                    angle *= -1.0f;
                }
            } else if (this.this$0.gizmoOrigin instanceof OriginVehiclePart) {
                if (dot > 0.0f) {
                    angle *= -1.0f;
                }
            } else if (dot < 0.0f) {
                angle *= -1.0f;
            }
            if (this.this$0.transformMode == TransformMode.Global) {
                this.startInvXfrm.transformDirection(orientation);
            }
            Quaternionf rotationQuat = UI3DScene.allocQuaternionf().fromAxisAngleDeg(orientation, angle);
            UI3DScene.releaseVector3f(orientation);
            Vector3f delta = rotationQuat.getEulerAnglesXYZ(new Vector3f());
            UI3DScene.releaseQuaternionf(rotationQuat);
            delta.mul(57.295776f);
            if (this.snap) {
                // empty if block
            }
            LuaManager.caller.pcall(UIManager.getDefaultThread(), this.this$0.getTable().rawget("onGizmoChanged"), this.this$0.table, delta);
        }

        @Override
        void stopTracking() {
            this.trackAxis = Axis.None;
        }

        @Override
        void render() {
            float b;
            float g;
            float r;
            Axis axis;
            if (!this.visible) {
                return;
            }
            StateData stateData = this.this$0.stateDataRender();
            org.joml.Matrix4f matrix4f = UI3DScene.allocMatrix4f();
            matrix4f.set(stateData.gizmoParentTransform);
            matrix4f.mul(stateData.gizmoOriginTransform);
            matrix4f.mul(stateData.gizmoChildTransform);
            if (stateData.selectedAttachmentIsChildAttachment) {
                matrix4f.mul(stateData.gizmoChildAttachmentTransformInv);
            }
            matrix4f.mul(stateData.gizmoTransform);
            Vector3f scale = matrix4f.getScale(UI3DScene.allocVector3f());
            matrix4f.scale(1.0f / scale.x, 1.0f / scale.y, 1.0f / scale.z);
            UI3DScene.releaseVector3f(scale);
            if (this.this$0.transformMode == TransformMode.Global) {
                matrix4f.setRotationXYZ(0.0f, 0.0f, 0.0f);
            }
            float mouseX = Mouse.getXA() - this.this$0.getAbsoluteX().intValue();
            float mouseY = Mouse.getYA() - this.this$0.getAbsoluteY().intValue();
            Ray cameraRay = this.this$0.getCameraRay(mouseX, (float)this.this$0.screenHeight() - mouseY, stateData.projection, stateData.modelView, UI3DScene.allocRay());
            float scale2 = this.this$0.gizmoScale / stateData.zoomMult() * 1000.0f;
            float radius = 0.5f * scale2;
            GL11.glClear(256);
            GL11.glEnable(2929);
            GL11.glDepthFunc(513);
            org.joml.Matrix4f axisMatrix4f = UI3DScene.allocMatrix4f();
            Axis axis2 = axis = this.trackAxis == Axis.None ? stateData.gizmoAxis : this.trackAxis;
            if (this.trackAxis == Axis.None || this.trackAxis == Axis.X) {
                r = axis == Axis.X ? 1.0f : 0.5f;
                g = 0.0f;
                b = 0.0f;
                axisMatrix4f.set(matrix4f);
                axisMatrix4f.rotateY(1.5707964f);
                this.renderAxis(axisMatrix4f, 0.01f * scale2, radius / 2.0f, r, 0.0f, 0.0f, cameraRay);
            }
            if (this.trackAxis == Axis.None || this.trackAxis == Axis.Y) {
                r = 0.0f;
                g = axis == Axis.Y ? 1.0f : 0.5f;
                b = 0.0f;
                axisMatrix4f.set(matrix4f);
                axisMatrix4f.rotateX(1.5707964f);
                this.renderAxis(axisMatrix4f, 0.01f * scale2, radius / 2.0f, 0.0f, g, 0.0f, cameraRay);
            }
            if (this.trackAxis == Axis.None || this.trackAxis == Axis.Z) {
                r = 0.0f;
                g = 0.0f;
                b = axis == Axis.Z ? 1.0f : 0.5f;
                axisMatrix4f.set(matrix4f);
                this.renderAxis(axisMatrix4f, 0.01f * scale2, radius / 2.0f, 0.0f, 0.0f, b, cameraRay);
            }
            UI3DScene.releaseMatrix4f(axisMatrix4f);
            UI3DScene.releaseRay(cameraRay);
            UI3DScene.releaseMatrix4f(matrix4f);
            GL11.glColor3f(1.0f, 1.0f, 1.0f);
            GL11.glDepthFunc(519);
            this.renderLineToOrigin();
            GLStateRenderThread.restore();
        }

        void getCircleSegments(Vector3f center, float radius, Vector3f orthoNormal1, Vector3f orthoNormal2, ArrayList<Vector3f> out) {
            Vector3f p1 = UI3DScene.allocVector3f();
            Vector3f p2 = UI3DScene.allocVector3f();
            int segments = 32;
            double t = 0.0;
            double cos = Math.cos(t);
            double sin = Math.sin(t);
            orthoNormal1.mul((float)cos, p1);
            orthoNormal2.mul((float)sin, p2);
            p1.add(p2).mul(radius);
            out.add(UI3DScene.allocVector3f().set(center).add(p1));
            for (int i = 1; i <= 32; ++i) {
                t = (double)i * 360.0 / 32.0 * 0.01745329238474369;
                cos = Math.cos(t);
                sin = Math.sin(t);
                orthoNormal1.mul((float)cos, p1);
                orthoNormal2.mul((float)sin, p2);
                p1.add(p2).mul(radius);
                out.add(UI3DScene.allocVector3f().set(center).add(p1));
            }
            UI3DScene.releaseVector3f(p1);
            UI3DScene.releaseVector3f(p2);
        }

        private float hitTestCircle(Ray cameraRay, ArrayList<Vector3f> circlePoints, Vector2 closestPoint) {
            Ray ray = UI3DScene.allocRay();
            Vector3f v = UI3DScene.allocVector3f();
            float camX = this.this$0.sceneToUIX(cameraRay.origin.x, cameraRay.origin.y, cameraRay.origin.z);
            float camY = this.this$0.sceneToUIY(cameraRay.origin.x, cameraRay.origin.y, cameraRay.origin.z);
            float closestDist = Float.MAX_VALUE;
            Vector3f p1 = circlePoints.get(0);
            for (int i = 1; i < circlePoints.size(); ++i) {
                Vector3f p2 = circlePoints.get(i);
                float x1 = this.this$0.sceneToUIX(p1.x, p1.y, p1.z);
                float y1 = this.this$0.sceneToUIY(p1.x, p1.y, p1.z);
                float x2 = this.this$0.sceneToUIX(p2.x, p2.y, p2.z);
                float y2 = this.this$0.sceneToUIY(p2.x, p2.y, p2.z);
                double lenSq = Math.pow(x2 - x1, 2.0) + Math.pow(y2 - y1, 2.0);
                if (lenSq < 0.001) {
                    p1 = p2;
                    continue;
                }
                double u = (double)((camX - x1) * (x2 - x1) + (camY - y1) * (y2 - y1)) / lenSq;
                double xu = (double)x1 + u * (double)(x2 - x1);
                double yu = (double)y1 + u * (double)(y2 - y1);
                if (u <= 0.0) {
                    xu = x1;
                    yu = y1;
                } else if (u >= 1.0) {
                    xu = x2;
                    yu = y2;
                }
                float dist = IsoUtils.DistanceTo2D(camX, camY, (float)xu, (float)yu);
                if (dist < closestDist) {
                    closestDist = dist;
                    closestPoint.set((float)xu, (float)yu);
                }
                p1 = p2;
            }
            UI3DScene.releaseVector3f(v);
            UI3DScene.releaseRay(ray);
            return closestDist;
        }

        void renderAxis(org.joml.Matrix4f axisMatrix4f, float r, float c, float r1, float g1, float b1, Ray cameraRay) {
            Ray cameraRay2 = UI3DScene.allocRay().set(cameraRay);
            axisMatrix4f.invert();
            axisMatrix4f.transformPosition(cameraRay2.origin);
            axisMatrix4f.transformDirection(cameraRay2.direction);
            axisMatrix4f.invert();
            VBORenderer vbor = VBORenderer.getInstance();
            vbor.cmdPushAndMultMatrix(5888, axisMatrix4f);
            vbor.addTorus(r, c, 8, 32, r1, g1, b1, cameraRay2);
            vbor.cmdPopMatrix(5888);
            vbor.flush();
            UI3DScene.releaseRay(cameraRay2);
        }

        void renderAxis(Vector3f center, float radius, Vector3f orthoNormal1, Vector3f orthoNormal2, float r, float g, float b, Ray cameraRay) {
            vboRenderer.flush();
            vboRenderer.setLineWidth(6.0f);
            this.getCircleSegments(center, radius, orthoNormal1, orthoNormal2, this.circlePointsRender);
            Vector3f spoke = UI3DScene.allocVector3f();
            Vector3f p0 = this.circlePointsRender.get(0);
            for (int i = 1; i < this.circlePointsRender.size(); ++i) {
                Vector3f p1 = this.circlePointsRender.get(i);
                spoke.set(p1.x - center.x, p1.y - center.y, p1.z - center.z).normalize();
                float dot = spoke.dot(cameraRay.direction);
                if (dot < 0.1f) {
                    vboRenderer.addLine(p0.x, p0.y, p0.z, p1.x, p1.y, p1.z, r, g, b, 1.0f);
                } else {
                    vboRenderer.addLine(p0.x, p0.y, p0.z, p1.x, p1.y, p1.z, r / 2.0f, g / 2.0f, b / 2.0f, 0.25f);
                }
                p0 = p1;
            }
            BaseVehicle.TL_vector3f_pool.get().release(this.circlePointsRender);
            this.circlePointsRender.clear();
            UI3DScene.releaseVector3f(spoke);
            vboRenderer.flush();
        }

        Vector3f getPointOnAxis(float uiX, float uiY, Axis axis, Circle circle, org.joml.Matrix4f gizmoXfrm, Vector3f out) {
            float scale = 1.0f;
            circle.radius = 0.5f;
            gizmoXfrm.getTranslation(circle.center);
            float cx = this.this$0.sceneToUIX(circle.center.x, circle.center.y, circle.center.z);
            float cy = this.this$0.sceneToUIY(circle.center.x, circle.center.y, circle.center.z);
            circle.center.set(cx, cy, 0.0f);
            circle.orientation.set(0.0f, 0.0f, 1.0f);
            Ray cameraRay = UI3DScene.allocRay();
            cameraRay.origin.set(uiX, uiY, 0.0f);
            cameraRay.direction.set(0.0f, 0.0f, -1.0f);
            UI3DScene.closest_distance_line_circle(cameraRay, circle, out);
            UI3DScene.releaseRay(cameraRay);
            return out;
        }

        float calculateRotation(Vector3f pp, Vector3f pc, Circle circle) {
            if (pp.equals(pc)) {
                return 0.0f;
            }
            Vector3f vp = UI3DScene.allocVector3f().set(pp).sub(circle.center).normalize();
            Vector3f vc = UI3DScene.allocVector3f().set(pc).sub(circle.center).normalize();
            float angle = (float)Math.acos(vc.dot(vp));
            Vector3f cross = vp.cross(vc, UI3DScene.allocVector3f());
            int sign = (int)Math.signum(cross.dot(circle.orientation));
            UI3DScene.releaseVector3f(vp);
            UI3DScene.releaseVector3f(vc);
            UI3DScene.releaseVector3f(cross);
            return (float)sign * angle * 57.295776f;
        }
    }

    private final class ScaleGizmo
    extends Gizmo {
        final org.joml.Matrix4f startXfrm;
        final org.joml.Matrix4f startInvXfrm;
        final Vector3f startPos;
        final Vector3f currentPos;
        Axis trackAxis;
        boolean snap;
        boolean hideX;
        boolean hideY;
        boolean hideZ;
        final Cylinder cylinder;
        final /* synthetic */ UI3DScene this$0;

        private ScaleGizmo(UI3DScene uI3DScene) {
            UI3DScene uI3DScene2 = uI3DScene;
            Objects.requireNonNull(uI3DScene2);
            this.this$0 = uI3DScene2;
            super(uI3DScene);
            this.startXfrm = new org.joml.Matrix4f();
            this.startInvXfrm = new org.joml.Matrix4f();
            this.startPos = new Vector3f();
            this.currentPos = new Vector3f();
            this.trackAxis = Axis.None;
            this.snap = true;
            this.cylinder = new Cylinder();
        }

        @Override
        Axis hitTest(float uiX, float uiY) {
            float zdot;
            float ydot;
            float xdot;
            if (!this.visible) {
                return Axis.None;
            }
            StateData stateData = this.this$0.stateDataMain();
            org.joml.Matrix4f gizmoXfrm = UI3DScene.allocMatrix4f();
            gizmoXfrm.set(stateData.gizmoParentTransform);
            gizmoXfrm.mul(stateData.gizmoOriginTransform);
            gizmoXfrm.mul(stateData.gizmoChildTransform);
            if (stateData.selectedAttachmentIsChildAttachment) {
                gizmoXfrm.mul(stateData.gizmoChildAttachmentTransformInv);
            }
            gizmoXfrm.mul(stateData.gizmoTransform);
            if (this.this$0.transformMode == TransformMode.Global) {
                gizmoXfrm.setRotationXYZ(0.0f, 0.0f, 0.0f);
            }
            uiY = (float)this.this$0.screenHeight() - uiY;
            Ray cameraRay = this.this$0.getCameraRay(uiX, uiY, UI3DScene.allocRay());
            Ray axis = UI3DScene.allocRay();
            gizmoXfrm.transformProject(axis.origin.set(0.0f, 0.0f, 0.0f));
            float scale = this.this$0.gizmoScale / stateData.zoomMult() * 1000.0f;
            float length = 0.5f * scale;
            float thickness = 0.05f * scale;
            float offset = 0.1f * scale;
            gizmoXfrm.transformDirection(axis.direction.set(1.0f, 0.0f, 0.0f)).normalize();
            float distX = UI3DScene.closest_distance_between_lines(axis, cameraRay);
            float xT = axis.t;
            float camXT = cameraRay.t;
            if (xT < offset || xT >= offset + length) {
                xT = Float.MAX_VALUE;
                distX = Float.MAX_VALUE;
            }
            this.hideX = Math.abs(xdot = axis.direction.dot(cameraRay.direction)) > 0.9f;
            gizmoXfrm.transformDirection(axis.direction.set(0.0f, 1.0f, 0.0f)).normalize();
            float distY = UI3DScene.closest_distance_between_lines(axis, cameraRay);
            float yT = axis.t;
            float camYT = cameraRay.t;
            if (yT < offset || yT >= offset + length) {
                yT = Float.MAX_VALUE;
                distY = Float.MAX_VALUE;
            }
            this.hideY = Math.abs(ydot = axis.direction.dot(cameraRay.direction)) > 0.9f;
            gizmoXfrm.transformDirection(axis.direction.set(0.0f, 0.0f, 1.0f)).normalize();
            float distZ = UI3DScene.closest_distance_between_lines(axis, cameraRay);
            float zT = axis.t;
            float camZT = cameraRay.t;
            if (zT < offset || zT >= offset + length) {
                zT = Float.MAX_VALUE;
                distZ = Float.MAX_VALUE;
            }
            this.hideZ = Math.abs(zdot = axis.direction.dot(cameraRay.direction)) > 0.9f;
            UI3DScene.releaseRay(axis);
            UI3DScene.releaseRay(cameraRay);
            UI3DScene.releaseMatrix4f(gizmoXfrm);
            if (xT >= offset && xT < offset + length && distX < distY && distX < distZ) {
                return distX <= thickness / 2.0f ? Axis.X : Axis.None;
            }
            if (yT >= offset && yT < offset + length && distY < distX && distY < distZ) {
                return distY <= thickness / 2.0f ? Axis.Y : Axis.None;
            }
            if (zT >= offset && zT < offset + length && distZ < distX && distZ < distY) {
                return distZ <= thickness / 2.0f ? Axis.Z : Axis.None;
            }
            return Axis.None;
        }

        @Override
        void startTracking(float uiX, float uiY, Axis axis) {
            StateData stateData = this.this$0.stateDataMain();
            this.this$0.setModelViewProjection(stateData);
            this.this$0.setGizmoTransforms(stateData);
            this.startXfrm.set(stateData.gizmoParentTransform);
            this.startXfrm.mul(stateData.gizmoOriginTransform);
            this.startXfrm.mul(stateData.gizmoChildTransform);
            if (!stateData.selectedAttachmentIsChildAttachment) {
                this.startXfrm.mul(stateData.gizmoTransform);
            }
            if (this.this$0.transformMode == TransformMode.Global) {
                this.startXfrm.setRotationXYZ(0.0f, 0.0f, 0.0f);
            }
            this.startInvXfrm.set(this.startXfrm);
            this.startInvXfrm.invert();
            this.trackAxis = axis;
            this.getPointOnAxis(uiX, uiY, axis, this.startXfrm, this.startPos);
        }

        @Override
        void updateTracking(float uiX, float uiY) {
            Vector3f pos = this.getPointOnAxis(uiX, uiY, this.trackAxis, this.startXfrm, UI3DScene.allocVector3f());
            if (this.currentPos.equals(pos)) {
                UI3DScene.releaseVector3f(pos);
                return;
            }
            this.currentPos.set(pos);
            UI3DScene.releaseVector3f(pos);
            StateData stateData = this.this$0.stateDataMain();
            this.this$0.setModelViewProjection(stateData);
            this.this$0.setGizmoTransforms(stateData);
            Vector3f delta = new Vector3f(this.currentPos).sub(this.startPos);
            if (this.this$0.transformMode == TransformMode.Global) {
                Vector3f vs = this.startInvXfrm.transformPosition(this.startPos, UI3DScene.allocVector3f());
                Vector3f vc = this.startInvXfrm.transformPosition(this.currentPos, UI3DScene.allocVector3f());
                org.joml.Matrix4f m = UI3DScene.allocMatrix4f();
                m.set(stateData.gizmoParentTransform);
                m.mul(stateData.gizmoOriginTransform);
                if (!stateData.selectedAttachmentIsChildAttachment) {
                    m.mul(stateData.gizmoChildTransform);
                }
                m.invert();
                m.transformPosition(vs);
                m.transformPosition(vc);
                UI3DScene.releaseMatrix4f(m);
                delta.set(vc).sub(vs);
                UI3DScene.releaseVector3f(vs);
                UI3DScene.releaseVector3f(vc);
            } else {
                Vector3f vs = this.startInvXfrm.transformPosition(this.startPos, UI3DScene.allocVector3f());
                Vector3f vc = this.startInvXfrm.transformPosition(this.currentPos, UI3DScene.allocVector3f());
                delta.set(vc).sub(vs);
                UI3DScene.releaseVector3f(vs);
                UI3DScene.releaseVector3f(vc);
            }
            if (this.snap) {
                delta.x = (float)PZMath.fastfloor(delta.x * this.this$0.gridMult()) / this.this$0.gridMult();
                delta.y = (float)PZMath.fastfloor(delta.y * this.this$0.gridMult()) / this.this$0.gridMult();
                delta.z = (float)PZMath.fastfloor(delta.z * this.this$0.gridMult()) / this.this$0.gridMult();
            }
            LuaManager.caller.pcall(UIManager.getDefaultThread(), this.this$0.getTable().rawget("onGizmoChanged"), this.this$0.table, delta);
        }

        @Override
        void stopTracking() {
            this.trackAxis = Axis.None;
        }

        @Override
        void render() {
            float a;
            float b;
            float g;
            float r;
            if (!this.visible) {
                return;
            }
            StateData stateData = this.this$0.stateDataRender();
            float scale = this.this$0.gizmoScale / stateData.zoomMult() * 1000.0f;
            float length = 0.5f * scale;
            float thickness = 0.05f * scale;
            float offset = 0.1f * scale;
            org.joml.Matrix4f matrix4f = UI3DScene.allocMatrix4f();
            matrix4f.set(stateData.gizmoParentTransform);
            matrix4f.mul(stateData.gizmoOriginTransform);
            matrix4f.mul(stateData.gizmoChildTransform);
            if (stateData.selectedAttachmentIsChildAttachment) {
                matrix4f.mul(stateData.gizmoChildAttachmentTransformInv);
            }
            matrix4f.mul(stateData.gizmoTransform);
            Vector3f localScale = matrix4f.getScale(UI3DScene.allocVector3f());
            matrix4f.scale(1.0f / localScale.x, 1.0f / localScale.y, 1.0f / localScale.z);
            UI3DScene.releaseVector3f(localScale);
            if (this.this$0.transformMode == TransformMode.Global) {
                matrix4f.setRotationXYZ(0.0f, 0.0f, 0.0f);
            }
            stateData.modelView.mul(matrix4f, matrix4f);
            VBORenderer vbor = VBORenderer.getInstance();
            vbor.cmdPushAndLoadMatrix(5888, matrix4f);
            if (!this.hideX) {
                r = stateData.gizmoAxis == Axis.X || this.trackAxis == Axis.X ? 1.0f : 0.5f;
                g = 0.0f;
                b = 0.0f;
                a = 1.0f;
                matrix4f.rotation(1.5707964f, 0.0f, 1.0f, 0.0f);
                matrix4f.translate(0.0f, 0.0f, offset);
                vbor.cmdPushAndMultMatrix(5888, matrix4f);
                vbor.addCylinder_Fill(thickness / 2.0f, thickness / 2.0f, length, 8, 1, r, 0.0f, 0.0f, 1.0f);
                vbor.cmdPopMatrix(5888);
                matrix4f.translate(0.0f, 0.0f, length);
                vbor.cmdPushAndMultMatrix(5888, matrix4f);
                vbor.addCylinder_Fill(thickness, thickness, 0.1f * scale, 8, 1, r, 0.0f, 0.0f, 1.0f);
                vbor.cmdPopMatrix(5888);
            }
            if (!this.hideY) {
                r = 0.0f;
                g = stateData.gizmoAxis == Axis.Y || this.trackAxis == Axis.Y ? 1.0f : 0.5f;
                b = 0.0f;
                a = 1.0f;
                matrix4f.rotation(-1.5707964f, 1.0f, 0.0f, 0.0f);
                matrix4f.translate(0.0f, 0.0f, offset);
                vbor.cmdPushAndMultMatrix(5888, matrix4f);
                vbor.addCylinder_Fill(thickness / 2.0f, thickness / 2.0f, length, 8, 1, 0.0f, g, 0.0f, 1.0f);
                vbor.cmdPopMatrix(5888);
                matrix4f.translate(0.0f, 0.0f, length);
                vbor.cmdPushAndMultMatrix(5888, matrix4f);
                vbor.addCylinder_Fill(thickness, thickness, 0.1f * scale, 8, 1, 0.0f, g, 0.0f, 1.0f);
                vbor.cmdPopMatrix(5888);
            }
            if (!this.hideZ) {
                r = 0.0f;
                g = 0.0f;
                b = stateData.gizmoAxis == Axis.Z || this.trackAxis == Axis.Z ? 1.0f : 0.5f;
                a = 1.0f;
                matrix4f.translation(0.0f, 0.0f, offset);
                vbor.cmdPushAndMultMatrix(5888, matrix4f);
                vbor.addCylinder_Fill(thickness / 2.0f, thickness / 2.0f, length, 8, 1, 0.0f, 0.0f, b, 1.0f);
                vbor.cmdPopMatrix(5888);
                matrix4f.translate(0.0f, 0.0f, length);
                vbor.cmdPushAndMultMatrix(5888, matrix4f);
                vbor.addCylinder_Fill(thickness, thickness, 0.1f * scale, 8, 1, 0.0f, 0.0f, b, 1.0f);
                vbor.cmdPopMatrix(5888);
            }
            GL11.glColor3f(1.0f, 1.0f, 1.0f);
            UI3DScene.releaseMatrix4f(matrix4f);
            vbor.cmdPopMatrix(5888);
            this.renderLineToOrigin();
            GLStateRenderThread.restore();
        }
    }

    private final class TranslateGizmo
    extends Gizmo {
        final org.joml.Matrix4f startXfrm;
        final org.joml.Matrix4f startInvXfrm;
        final Vector3f startPos;
        final Vector3f currentPos;
        Axis trackAxis;
        boolean doubleAxis;
        final PartialDisk disk;
        final /* synthetic */ UI3DScene this$0;

        private TranslateGizmo(UI3DScene uI3DScene) {
            UI3DScene uI3DScene2 = uI3DScene;
            Objects.requireNonNull(uI3DScene2);
            this.this$0 = uI3DScene2;
            super(uI3DScene);
            this.startXfrm = new org.joml.Matrix4f();
            this.startInvXfrm = new org.joml.Matrix4f();
            this.startPos = new Vector3f();
            this.currentPos = new Vector3f();
            this.trackAxis = Axis.None;
            this.disk = new PartialDisk();
        }

        @Override
        Axis hitTest(float uiX, float uiY) {
            if (!this.visible) {
                return Axis.None;
            }
            StateData stateData = this.this$0.stateDataMain();
            this.this$0.setModelViewProjection(stateData);
            this.this$0.setGizmoTransforms(stateData);
            org.joml.Matrix4f gizmoXfrm = UI3DScene.allocMatrix4f();
            gizmoXfrm.set(stateData.gizmoParentTransform);
            gizmoXfrm.mul(stateData.gizmoOriginTransform);
            gizmoXfrm.mul(stateData.gizmoChildTransform);
            if (stateData.selectedAttachmentIsChildAttachment) {
                gizmoXfrm.mul(stateData.gizmoChildAttachmentTransformInv);
            }
            gizmoXfrm.mul(stateData.gizmoTransform);
            if (this.this$0.transformMode == TransformMode.Global) {
                gizmoXfrm.setRotationXYZ(0.0f, 0.0f, 0.0f);
            }
            uiY = (float)this.this$0.screenHeight() - uiY;
            Ray cameraRay = this.this$0.getCameraRay(uiX, uiY, UI3DScene.allocRay());
            Ray axis = UI3DScene.allocRay();
            gizmoXfrm.transformPosition(axis.origin.set(0.0f, 0.0f, 0.0f));
            float scale = this.this$0.gizmoScale / stateData.zoomMult() * 1000.0f;
            float length = 0.5f * scale;
            float thickness = 0.05f * scale;
            float offset = 0.1f * scale;
            gizmoXfrm.transformDirection(axis.direction.set(1.0f, 0.0f, 0.0f)).normalize();
            float distX = UI3DScene.closest_distance_between_lines(axis, cameraRay);
            float xT = axis.t;
            float camXT = cameraRay.t;
            if (!this.this$0.gizmoAxisVisibleX || xT < offset || xT >= offset + length) {
                xT = Float.MAX_VALUE;
                distX = Float.MAX_VALUE;
            }
            float xdot = axis.direction.dot(cameraRay.direction);
            stateData.translateGizmoRenderData.hideX = !this.this$0.gizmoAxisVisibleX || Math.abs(xdot) > 0.9f;
            gizmoXfrm.transformDirection(axis.direction.set(0.0f, 1.0f, 0.0f)).normalize();
            float distY = UI3DScene.closest_distance_between_lines(axis, cameraRay);
            float yT = axis.t;
            float camYT = cameraRay.t;
            if (!this.this$0.gizmoAxisVisibleY || yT < offset || yT >= offset + length) {
                yT = Float.MAX_VALUE;
                distY = Float.MAX_VALUE;
            }
            float ydot = axis.direction.dot(cameraRay.direction);
            stateData.translateGizmoRenderData.hideY = !this.this$0.gizmoAxisVisibleY || Math.abs(ydot) > 0.9f;
            gizmoXfrm.transformDirection(axis.direction.set(0.0f, 0.0f, 1.0f)).normalize();
            float distZ = UI3DScene.closest_distance_between_lines(axis, cameraRay);
            float zT = axis.t;
            float camZT = cameraRay.t;
            if (!this.this$0.gizmoAxisVisibleZ || zT < offset || zT >= offset + length) {
                zT = Float.MAX_VALUE;
                distZ = Float.MAX_VALUE;
            }
            float zdot = axis.direction.dot(cameraRay.direction);
            stateData.translateGizmoRenderData.hideZ = !this.this$0.gizmoAxisVisibleZ || Math.abs(zdot) > 0.9f;
            Axis doubleAxis = Axis.None;
            if (this.doubleAxis) {
                float localOffset = thickness * 1.5f;
                float inner = localOffset + offset;
                float outer = inner + length / 2.0f;
                Vector3f pointOnPlane3D = UI3DScene.allocVector3f();
                Vector2f pointOnPlane2D = UI3DScene.allocVector2f();
                if (this.this$0.gizmoOrigin instanceof SceneCharacter) {
                    gizmoXfrm.scale(0.6666667f);
                }
                if (this.getPointOnDualAxis(uiX, -(uiY - (float)this.this$0.screenHeight()), Axis.XY, gizmoXfrm, pointOnPlane3D, pointOnPlane2D) && pointOnPlane2D.x >= 0.0f && pointOnPlane2D.y >= 0.0f && pointOnPlane2D.length() >= inner && pointOnPlane2D.length() < outer) {
                    doubleAxis = Axis.XY;
                }
                if (this.getPointOnDualAxis(uiX, -(uiY - (float)this.this$0.screenHeight()), Axis.XZ, gizmoXfrm, pointOnPlane3D, pointOnPlane2D) && pointOnPlane2D.x >= 0.0f && pointOnPlane2D.y >= 0.0f && pointOnPlane2D.length() >= inner && pointOnPlane2D.length() < outer) {
                    doubleAxis = Axis.XZ;
                }
                if (this.getPointOnDualAxis(uiX, -(uiY - (float)this.this$0.screenHeight()), Axis.YZ, gizmoXfrm, pointOnPlane3D, pointOnPlane2D) && pointOnPlane2D.x >= 0.0f && pointOnPlane2D.y >= 0.0f && pointOnPlane2D.length() >= inner && pointOnPlane2D.length() < outer) {
                    doubleAxis = Axis.YZ;
                }
                UI3DScene.releaseVector3f(pointOnPlane3D);
                UI3DScene.releaseVector2f(pointOnPlane2D);
            }
            UI3DScene.releaseRay(axis);
            UI3DScene.releaseRay(cameraRay);
            UI3DScene.releaseMatrix4f(gizmoXfrm);
            if (doubleAxis != Axis.None) {
                return doubleAxis;
            }
            if (xT >= offset && xT < offset + length && distX < distY && distX < distZ) {
                return distX <= thickness / 2.0f ? Axis.X : Axis.None;
            }
            if (yT >= offset && yT < offset + length && distY < distX && distY < distZ) {
                return distY <= thickness / 2.0f ? Axis.Y : Axis.None;
            }
            if (zT >= offset && zT < offset + length && distZ < distX && distZ < distY) {
                return distZ <= thickness / 2.0f ? Axis.Z : Axis.None;
            }
            return Axis.None;
        }

        @Override
        void startTracking(float uiX, float uiY, Axis axis) {
            StateData stateData = this.this$0.stateDataMain();
            this.this$0.setModelViewProjection(stateData);
            this.this$0.setGizmoTransforms(stateData);
            this.startXfrm.set(stateData.gizmoParentTransform);
            this.startXfrm.mul(stateData.gizmoOriginTransform);
            this.startXfrm.mul(stateData.gizmoChildTransform);
            if (!stateData.selectedAttachmentIsChildAttachment) {
                this.startXfrm.mul(stateData.gizmoTransform);
            }
            if (this.this$0.transformMode == TransformMode.Global) {
                this.startXfrm.setRotationXYZ(0.0f, 0.0f, 0.0f);
            }
            this.startInvXfrm.set(this.startXfrm);
            this.startInvXfrm.invert();
            this.trackAxis = axis;
            this.getPointOnAxis(uiX, uiY, axis, this.startXfrm, this.startPos);
        }

        @Override
        void updateTracking(float uiX, float uiY) {
            Vector3f pos = this.getPointOnAxis(uiX, uiY, this.trackAxis, this.startXfrm, UI3DScene.allocVector3f());
            if (this.currentPos.equals(pos)) {
                UI3DScene.releaseVector3f(pos);
                return;
            }
            this.currentPos.set(pos);
            UI3DScene.releaseVector3f(pos);
            StateData stateData = this.this$0.stateDataMain();
            this.this$0.setModelViewProjection(stateData);
            this.this$0.setGizmoTransforms(stateData);
            Vector3f delta = new Vector3f(this.currentPos).sub(this.startPos);
            if (this.this$0.selectedAttachment == null && this.this$0.gizmoChild == null && !stateData.gizmoOriginIsGeometry) {
                delta.set(this.currentPos).sub(this.startPos);
            } else if (this.this$0.transformMode == TransformMode.Global) {
                Vector3f vs = this.startInvXfrm.transformPosition(this.startPos, UI3DScene.allocVector3f());
                Vector3f vc = this.startInvXfrm.transformPosition(this.currentPos, UI3DScene.allocVector3f());
                org.joml.Matrix4f m = UI3DScene.allocMatrix4f();
                m.set(stateData.gizmoParentTransform);
                m.mul(stateData.gizmoOriginTransform);
                if (!stateData.selectedAttachmentIsChildAttachment) {
                    m.mul(stateData.gizmoChildTransform);
                }
                m.invert();
                m.transformPosition(vs);
                m.transformPosition(vc);
                UI3DScene.releaseMatrix4f(m);
                delta.set(vc).sub(vs);
                UI3DScene.releaseVector3f(vs);
                UI3DScene.releaseVector3f(vc);
            } else {
                Vector3f vs = this.startInvXfrm.transformPosition(this.startPos, UI3DScene.allocVector3f());
                Vector3f vc = this.startInvXfrm.transformPosition(this.currentPos, UI3DScene.allocVector3f());
                org.joml.Matrix4f m = UI3DScene.allocMatrix4f();
                m.set(stateData.gizmoTransform);
                m.transformPosition(vs);
                m.transformPosition(vc);
                UI3DScene.releaseMatrix4f(m);
                delta.set(vc).sub(vs);
                UI3DScene.releaseVector3f(vs);
                UI3DScene.releaseVector3f(vc);
            }
            LuaManager.caller.pcall(UIManager.getDefaultThread(), this.this$0.getTable().rawget("onGizmoChanged"), this.this$0.table, delta);
        }

        @Override
        void stopTracking() {
            this.trackAxis = Axis.None;
        }

        @Override
        void render() {
            float a;
            float b;
            float g;
            float r;
            boolean highlight;
            if (!this.visible) {
                return;
            }
            StateData stateData = this.this$0.stateDataRender();
            org.joml.Matrix4f matrix4f = UI3DScene.allocMatrix4f();
            matrix4f.set(stateData.gizmoParentTransform);
            matrix4f.mul(stateData.gizmoOriginTransform);
            matrix4f.mul(stateData.gizmoChildTransform);
            if (stateData.selectedAttachmentIsChildAttachment) {
                matrix4f.mul(stateData.gizmoChildAttachmentTransformInv);
            }
            matrix4f.mul(stateData.gizmoTransform);
            Vector3f scale = matrix4f.getScale(UI3DScene.allocVector3f());
            matrix4f.scale(1.0f / scale.x, 1.0f / scale.y, 1.0f / scale.z);
            UI3DScene.releaseVector3f(scale);
            if (this.this$0.transformMode == TransformMode.Global) {
                matrix4f.setRotationXYZ(0.0f, 0.0f, 0.0f);
            }
            stateData.modelView.mul(matrix4f, matrix4f);
            float scale2 = this.this$0.gizmoScale / stateData.zoomMult() * 1000.0f;
            float thickenss = 0.05f * scale2;
            float length = 0.5f * scale2;
            float offset = 0.1f * scale2;
            VBORenderer vbor = VBORenderer.getInstance();
            vbor.cmdPushAndLoadMatrix(5888, matrix4f);
            if (!stateData.translateGizmoRenderData.hideX) {
                highlight = stateData.gizmoAxis == Axis.X || this.trackAxis == Axis.X;
                highlight |= stateData.gizmoAxis == Axis.XY || this.trackAxis == Axis.XY;
                r = (highlight |= stateData.gizmoAxis == Axis.XZ || this.trackAxis == Axis.XZ) ? 1.0f : 0.5f;
                g = 0.0f;
                b = 0.0f;
                a = 1.0f;
                matrix4f.rotation(1.5707964f, 0.0f, 1.0f, 0.0f);
                matrix4f.translate(0.0f, 0.0f, offset);
                vbor.cmdPushAndMultMatrix(5888, matrix4f);
                vbor.addCylinder_Fill(thickenss / 2.0f, thickenss / 2.0f, length, 8, 1, r, 0.0f, 0.0f, 1.0f);
                vbor.cmdPopMatrix(5888);
                matrix4f.translate(0.0f, 0.0f, length);
                vbor.cmdPushAndMultMatrix(5888, matrix4f);
                vbor.addCylinder_Fill(thickenss / 2.0f * 2.0f, 0.0f, 0.1f * scale2, 8, 1, r, 0.0f, 0.0f, 1.0f);
                vbor.cmdPopMatrix(5888);
            }
            if (!stateData.translateGizmoRenderData.hideY) {
                highlight = stateData.gizmoAxis == Axis.Y || this.trackAxis == Axis.Y;
                highlight |= stateData.gizmoAxis == Axis.XY || this.trackAxis == Axis.XY;
                boolean bl = stateData.gizmoAxis == Axis.YZ || this.trackAxis == Axis.YZ;
                r = 0.0f;
                g = (highlight |= bl) ? 1.0f : 0.5f;
                b = 0.0f;
                a = 1.0f;
                matrix4f.rotation(-1.5707964f, 1.0f, 0.0f, 0.0f);
                matrix4f.translate(0.0f, 0.0f, offset);
                vbor.cmdPushAndMultMatrix(5888, matrix4f);
                vbor.addCylinder_Fill(thickenss / 2.0f, thickenss / 2.0f, length, 8, 1, 0.0f, g, 0.0f, 1.0f);
                vbor.cmdPopMatrix(5888);
                matrix4f.translate(0.0f, 0.0f, length);
                vbor.cmdPushAndMultMatrix(5888, matrix4f);
                vbor.addCylinder_Fill(thickenss / 2.0f * 2.0f, 0.0f, 0.1f * scale2, 8, 1, 0.0f, g, 0.0f, 1.0f);
                vbor.cmdPopMatrix(5888);
            }
            if (!stateData.translateGizmoRenderData.hideZ) {
                highlight = stateData.gizmoAxis == Axis.Z || this.trackAxis == Axis.Z;
                highlight |= stateData.gizmoAxis == Axis.XZ || this.trackAxis == Axis.XZ;
                boolean bl = stateData.gizmoAxis == Axis.YZ || this.trackAxis == Axis.YZ;
                r = 0.0f;
                g = 0.0f;
                b = (highlight |= bl) ? 1.0f : 0.5f;
                a = 1.0f;
                matrix4f.translation(0.0f, 0.0f, offset);
                vbor.cmdPushAndMultMatrix(5888, matrix4f);
                vbor.addCylinder_Fill(thickenss / 2.0f, thickenss / 2.0f, length, 8, 1, 0.0f, 0.0f, b, 1.0f);
                vbor.cmdPopMatrix(5888);
                matrix4f.translate(0.0f, 0.0f, length);
                vbor.cmdPushAndMultMatrix(5888, matrix4f);
                vbor.addCylinder_Fill(thickenss / 2.0f * 2.0f, 0.0f, 0.1f * scale2, 8, 1, 0.0f, 0.0f, b, 1.0f);
                vbor.cmdPopMatrix(5888);
            }
            if (this.doubleAxis) {
                float localOffset = thickenss * 1.5f;
                if (!stateData.translateGizmoRenderData.hideX && !stateData.translateGizmoRenderData.hideY) {
                    boolean highlight2 = stateData.gizmoAxis == Axis.XY || this.trackAxis == Axis.XY;
                    GL11.glColor4f(1.0f, 1.0f, 0.0f, highlight2 ? 1.0f : 0.5f);
                    GL11.glTranslatef(localOffset, localOffset, 0.0f);
                    this.disk.draw(offset, offset + length / 2.0f, 5, 1, 0.0f, 90.0f);
                    GL11.glTranslatef(-localOffset, -localOffset, 0.0f);
                }
                if (!stateData.translateGizmoRenderData.hideX && !stateData.translateGizmoRenderData.hideZ) {
                    boolean highlight3 = stateData.gizmoAxis == Axis.XZ || this.trackAxis == Axis.XZ;
                    GL11.glColor4f(1.0f, 0.0f, 1.0f, highlight3 ? 1.0f : 0.5f);
                    GL11.glTranslatef(localOffset, 0.0f, localOffset);
                    GL11.glRotated(-90.0, 1.0, 0.0, 0.0);
                    this.disk.draw(offset, offset + length / 2.0f, 5, 1, 90.0f, 90.0f);
                    GL11.glRotated(90.0, 1.0, 0.0, 0.0);
                    GL11.glTranslatef(-localOffset, 0.0f, -localOffset);
                }
                if (!stateData.translateGizmoRenderData.hideY && !stateData.translateGizmoRenderData.hideZ) {
                    boolean highlight4 = stateData.gizmoAxis == Axis.YZ || this.trackAxis == Axis.YZ;
                    GL11.glColor4f(0.0f, 1.0f, 1.0f, highlight4 ? 1.0f : 0.5f);
                    GL11.glTranslatef(0.0f, localOffset, localOffset);
                    GL11.glRotated(-90.0, 0.0, 1.0, 0.0);
                    this.disk.draw(offset, offset + length / 2.0f, 5, 1, 0.0f, 90.0f);
                    GL11.glRotated(90.0, 0.0, 1.0, 0.0);
                    GL11.glTranslatef(0.0f, -localOffset, -localOffset);
                }
            }
            UI3DScene.releaseMatrix4f(matrix4f);
            GL11.glColor3f(1.0f, 1.0f, 1.0f);
            vbor.cmdPopMatrix(5888);
            this.renderLineToOrigin();
            GLStateRenderThread.restore();
        }
    }

    private static final class OriginAttachment
    extends SceneObject {
        SceneObject object;
        String attachmentName;

        OriginAttachment(UI3DScene scene) {
            super(scene, "OriginAttachment");
        }

        @Override
        SceneObjectRenderData renderMain() {
            return null;
        }

        @Override
        org.joml.Matrix4f getGlobalTransform(org.joml.Matrix4f transform) {
            return this.object.getAttachmentTransform(this.attachmentName, transform);
        }
    }

    private static final class OriginBone
    extends SceneObject {
        SceneCharacter character;
        SceneModel sceneModel;
        String boneName;

        OriginBone(UI3DScene scene) {
            super(scene, "OriginBone");
        }

        @Override
        SceneObjectRenderData renderMain() {
            return null;
        }

        @Override
        org.joml.Matrix4f getGlobalTransform(org.joml.Matrix4f transform) {
            if (this.sceneModel != null) {
                return this.sceneModel.getBoneMatrix(this.boneName, transform);
            }
            return this.character.getBoneMatrix(this.boneName, transform);
        }
    }

    private static final class OriginGeometry
    extends SceneObject {
        SceneGeometry sceneGeometry;
        String originHint;

        OriginGeometry(UI3DScene scene) {
            super(scene, "OriginGeometry");
        }

        @Override
        SceneObjectRenderData renderMain() {
            return null;
        }

        @Override
        org.joml.Matrix4f getGlobalTransform(org.joml.Matrix4f transform) {
            return this.sceneGeometry.getOriginTransform(this.originHint, transform);
        }
    }

    private static final class OriginGizmo
    extends SceneObject {
        OriginGizmo(UI3DScene scene) {
            super(scene, "OriginGizmo");
        }

        @Override
        SceneObjectRenderData renderMain() {
            return null;
        }
    }

    private static final class OriginVehiclePart
    extends SceneObject {
        SceneVehicle vehicle;
        String partId;
        String partModelId;
        String attachmentName;
        boolean boneOnly = true;

        OriginVehiclePart(UI3DScene scene) {
            super(scene, "OriginVehiclePart");
        }

        @Override
        SceneObjectRenderData renderMain() {
            return null;
        }

        @Override
        org.joml.Matrix4f getGlobalTransform(org.joml.Matrix4f transform) {
            this.vehicle.getTransformForPart(this.partId, this.partModelId, this.attachmentName, this.boneOnly, transform);
            VehicleRenderData vrd = VehicleRenderData.s_pool.alloc();
            vrd.initVehicle(this.vehicle);
            VehicleModelRenderData parentVMRD = vrd.partToRenderData.get(this.partId);
            if (parentVMRD != null) {
                parentVMRD.xfrm.mul(transform, transform);
            }
            vrd.release();
            return transform;
        }

        org.joml.Matrix4f getGlobalBoneTransform(org.joml.Matrix4f transform) {
            transform.identity();
            SceneVehicleModelInfo modelInfo = this.vehicle.getModelInfoForPart(this.partId);
            if (modelInfo == null) {
                return transform;
            }
            AnimationPlayer animationPlayer = modelInfo.getAnimationPlayer();
            if (animationPlayer == null) {
                return transform;
            }
            SkinningBone bone = animationPlayer.getSkinningData().getBone(this.attachmentName);
            if (bone == null) {
                return transform;
            }
            transform = PZMath.convertMatrix(animationPlayer.getModelTransformAt(bone.index), transform);
            transform.transpose();
            VehicleRenderData vrd = VehicleRenderData.s_pool.alloc();
            vrd.initVehicle(this.vehicle);
            VehicleModelRenderData parentVMRD = vrd.partToRenderData.get(this.partId);
            if (parentVMRD != null) {
                parentVMRD.xfrm.mul(transform, transform);
            }
            vrd.release();
            return transform;
        }
    }

    public static final class PolygonEditor {
        final UI3DScene scene;
        final Plane plane = new Plane();
        final Vector3f rotate = new Vector3f();
        GridPlane gridPlane = GridPlane.XY;

        PolygonEditor(UI3DScene scene) {
            this.scene = scene;
        }

        void setPlane(Vector3f translate, Vector3f rotate, GridPlane gridPlane) {
            this.plane.point.set(translate);
            this.plane.normal.set(0.0f, 0.0f, 1.0f);
            org.joml.Matrix4f m = UI3DScene.allocMatrix4f().rotationXYZ(rotate.x * ((float)Math.PI / 180), rotate.y * ((float)Math.PI / 180), rotate.z * ((float)Math.PI / 180));
            m.transformDirection(this.plane.normal);
            UI3DScene.releaseMatrix4f(m);
            this.rotate.set(rotate);
            this.gridPlane = gridPlane;
        }

        boolean uiToPlane3D(float uiX, float uiY, Vector3f result) {
            boolean hit = false;
            Ray cameraRay = this.scene.getCameraRay(uiX, (float)this.scene.screenHeight() - uiY, UI3DScene.allocRay());
            if (UI3DScene.intersect_ray_plane(this.plane, cameraRay, result) == 1) {
                hit = true;
            }
            UI3DScene.releaseRay(cameraRay);
            return hit;
        }

        boolean uiToPlane2D(float uiX, float uiY, Vector2f result) {
            Vector3f pointOnPlane = UI3DScene.allocVector3f();
            boolean hit = this.uiToPlane3D(uiX, uiY, pointOnPlane);
            if (hit) {
                org.joml.Matrix4f m = UI3DScene.allocMatrix4f();
                m.translation(this.plane.point);
                m.rotateXYZ(this.rotate.x * ((float)Math.PI / 180), this.rotate.y * ((float)Math.PI / 180), this.rotate.z * ((float)Math.PI / 180));
                m.invert();
                m.transformPosition(pointOnPlane);
                result.set(pointOnPlane.x, pointOnPlane.y);
                UI3DScene.releaseMatrix4f(m);
            }
            UI3DScene.releaseVector3f(pointOnPlane);
            return hit;
        }

        Vector3f planeTo3D(Vector2f pointOnPlane, Vector3f result) {
            org.joml.Matrix4f m = UI3DScene.allocMatrix4f();
            m.translation(this.plane.point);
            m.rotateXYZ(this.rotate.x * ((float)Math.PI / 180), this.rotate.y * ((float)Math.PI / 180), this.rotate.z * ((float)Math.PI / 180));
            m.transformPosition(pointOnPlane.x, pointOnPlane.y, 0.0f, result);
            UI3DScene.releaseMatrix4f(m);
            return result;
        }

        Vector2f planeToUI(Vector2f pointOnPlane, Vector2f result) {
            Vector3f scenePos = this.planeTo3D(pointOnPlane, UI3DScene.allocVector3f());
            result.set(this.scene.sceneToUIX(scenePos), this.scene.sceneToUIY(scenePos));
            UI3DScene.releaseVector3f(scenePos);
            return result;
        }
    }

    private final class GridPlaneDrawer
    extends TextureDraw.GenericDrawer {
        final UI3DScene scene;
        final /* synthetic */ UI3DScene this$0;

        GridPlaneDrawer(UI3DScene uI3DScene, UI3DScene scene) {
            UI3DScene uI3DScene2 = uI3DScene;
            Objects.requireNonNull(uI3DScene2);
            this.this$0 = uI3DScene2;
            this.scene = scene;
        }

        @Override
        public void render() {
            StateData stateData = this.this$0.stateDataRender();
            PZGLUtil.pushAndLoadMatrix(5889, stateData.projection);
            PZGLUtil.pushAndLoadMatrix(5888, stateData.modelView);
            GL11.glPushAttrib(2048);
            GL11.glViewport(this.this$0.getAbsoluteX().intValue(), Core.getInstance().getScreenHeight() - this.this$0.getAbsoluteY().intValue() - this.this$0.getHeight().intValue(), this.this$0.getWidth().intValue(), this.this$0.getHeight().intValue());
            float halfGrid = 5.0f;
            vboRenderer.startRun(UI3DScene.vboRenderer.formatPositionColor);
            vboRenderer.setMode(4);
            vboRenderer.setDepthTest(true);
            if (this.scene.gridPlane == GridPlane.XZ) {
                vboRenderer.addTriangle(-5.0f, 0.0f, -5.0f, 5.0f, 0.0f, -5.0f, -5.0f, 0.0f, 5.0f, 0.5f, 0.5f, 0.5f, 1.0f);
                vboRenderer.addTriangle(5.0f, 0.0f, 5.0f, -5.0f, 0.0f, 5.0f, 5.0f, 0.0f, -5.0f, 0.5f, 0.5f, 0.5f, 1.0f);
            }
            vboRenderer.endRun();
            vboRenderer.flush();
            GL11.glPopAttrib();
            ShaderHelper.glUseProgramObjectARB(0);
            PZGLUtil.popMatrix(5889);
            PZGLUtil.popMatrix(5888);
        }
    }

    private final class OverlaysDrawer
    extends TextureDraw.GenericDrawer {
        final /* synthetic */ UI3DScene this$0;

        private OverlaysDrawer(UI3DScene uI3DScene) {
            UI3DScene uI3DScene2 = uI3DScene;
            Objects.requireNonNull(uI3DScene2);
            this.this$0 = uI3DScene2;
        }

        void init() {
            int i;
            StateData stateData = this.this$0.stateDataMain();
            s_aabbPool.release((List<AABB>)stateData.aabb);
            stateData.aabb.clear();
            for (i = 0; i < this.this$0.aabb.size(); ++i) {
                AABB aabb = this.this$0.aabb.get(i);
                stateData.aabb.add(s_aabbPool.alloc().set(aabb));
            }
            s_box3DPool.release((List<Box3D>)stateData.box3d);
            stateData.box3d.clear();
            for (i = 0; i < this.this$0.box3d.size(); ++i) {
                Box3D box3D = this.this$0.box3d.get(i);
                stateData.box3d.add(s_box3DPool.alloc().set(box3D));
            }
            s_physicsMeshPool.releaseAll((List<PhysicsMesh>)stateData.physicsMesh);
            stateData.physicsMesh.clear();
            for (i = 0; i < this.this$0.physicsMesh.size(); ++i) {
                PhysicsShape asset;
                PhysicsShapeScript physicsShapeScript;
                PhysicsMesh physicsMesh = this.this$0.physicsMesh.get(i);
                if (physicsMesh.physicsShapeScript == null || (physicsShapeScript = ScriptManager.instance.getPhysicsShape(physicsMesh.physicsShapeScript)) == null || (asset = (PhysicsShape)PhysicsShapeAssetManager.instance.getAssetTable().get(physicsShapeScript.meshName)) == null || !asset.isReady()) continue;
                for (int j = 0; j < asset.meshes.size(); ++j) {
                    PhysicsShape.OneMesh oneMesh = asset.meshes.get(j);
                    PhysicsMesh physicsMesh1 = s_physicsMeshPool.alloc().set(physicsMesh);
                    org.joml.Matrix4f xfrm = Bullet.translationRotateScale(physicsShapeScript.translate, physicsShapeScript.rotate, physicsShapeScript.scale * physicsMesh.scale, UI3DScene.allocMatrix4f());
                    oneMesh.transform.transpose();
                    xfrm.mul(oneMesh.transform);
                    oneMesh.transform.transpose();
                    physicsMesh1.points = Bullet.transformPhysicsMeshPoints(xfrm, oneMesh.points, false);
                    UI3DScene.releaseMatrix4f(xfrm);
                    stateData.physicsMesh.add(physicsMesh1);
                }
            }
            s_posRotPool.release((List<PositionRotation>)stateData.axes);
            stateData.axes.clear();
            for (i = 0; i < this.this$0.axes.size(); ++i) {
                PositionRotation axis = this.this$0.axes.get(i);
                stateData.axes.add(s_posRotPool.alloc().set(axis));
            }
        }

        @Override
        public void render() {
            int i;
            StateData stateData = this.this$0.stateDataRender();
            DefaultShader.isActive = false;
            ShaderHelper.forgetCurrentlyBound();
            GL20.glUseProgram(0);
            PZGLUtil.pushAndLoadMatrix(5889, stateData.projection);
            PZGLUtil.pushAndLoadMatrix(5888, stateData.modelView);
            GL11.glPushAttrib(2048);
            GL11.glViewport(this.this$0.getAbsoluteX().intValue(), Core.getInstance().getScreenHeight() - this.this$0.getAbsoluteY().intValue() - this.this$0.getHeight().intValue(), this.this$0.getWidth().intValue(), this.this$0.getHeight().intValue());
            vboRenderer.setOffset(0.0f, 0.0f, 0.0f);
            if (this.this$0.drawGrid) {
                this.this$0.renderGrid();
            }
            for (i = 0; i < stateData.aabb.size(); ++i) {
                AABB aabb = stateData.aabb.get(i);
                this.this$0.renderAABB(aabb.x, aabb.y, aabb.z, -aabb.w / 2.0f, -aabb.h / 2.0f, -aabb.l / 2.0f, aabb.w / 2.0f, aabb.h / 2.0f, aabb.l / 2.0f, aabb.r, aabb.g, aabb.b, 1.0f, false);
            }
            for (i = 0; i < stateData.box3d.size(); ++i) {
                Box3D b = stateData.box3d.get(i);
                this.this$0.renderBox3D(b.x, b.y, b.z, b.xMin, b.yMin, b.zMin, b.xMax, b.yMax, b.zMax, b.rx, b.ry, b.rz, b.r, b.g, b.b, b.a, b.quads);
            }
            for (i = 0; i < stateData.physicsMesh.size(); ++i) {
                PhysicsMesh m = stateData.physicsMesh.get(i);
                this.this$0.renderPhysicsMesh(m.x, m.y, m.z, m.rx, m.ry, m.rz, m.r, m.g, m.b, m.points);
            }
            for (i = 0; i < stateData.axes.size(); ++i) {
                this.this$0.renderAxis(stateData.axes.get(i));
            }
            vboRenderer.flush();
            if (stateData.gizmo != null) {
                GL11.glDisable(3553);
                stateData.gizmo.render();
                GL11.glEnable(3553);
            }
            vboRenderer.flush();
            GL11.glPopAttrib();
            PZGLUtil.popMatrix(5889);
            PZGLUtil.popMatrix(5888);
            ShaderHelper.glUseProgramObjectARB(0);
            GLStateRenderThread.restore();
        }
    }

    private static abstract class SceneObject {
        final UI3DScene scene;
        final String id;
        boolean visible = true;
        final Vector3f translate = new Vector3f();
        final Vector3f rotate = new Vector3f();
        final Vector3f scale = new Vector3f(1.0f);
        SceneObject parent;
        String attachment;
        String parentAttachment;
        boolean autoRotate;
        float autoRotateAngle;
        ParentVehiclePart parentVehiclePart;

        SceneObject(UI3DScene scene, String id) {
            this.scene = scene;
            this.id = id;
        }

        void initClone(SceneObject clone) {
            clone.visible = this.visible;
            clone.translate.set(this.translate);
            clone.rotate.set(this.rotate);
            clone.scale.set(this.scale);
            clone.parent = this.parent;
            clone.attachment = this.attachment;
            clone.parentAttachment = this.parentAttachment;
            clone.parentVehiclePart = this.parentVehiclePart;
            clone.autoRotate = this.autoRotate;
            clone.autoRotateAngle = this.autoRotateAngle;
        }

        SceneObject clone(String id) {
            throw new RuntimeException("not implemented");
        }

        abstract SceneObjectRenderData renderMain();

        org.joml.Matrix4f getLocalTransform(org.joml.Matrix4f transform) {
            boolean bInvertX;
            transform.identity();
            SceneModel sceneModel = Type.tryCastTo(this, SceneModel.class);
            boolean bl = bInvertX = sceneModel != null && sceneModel.spriteModelEditor;
            if (sceneModel != null && bInvertX) {
                transform.translate(this.translate.x, this.translate.y, this.translate.z);
            } else if (sceneModel == null || !sceneModel.useWorldAttachment) {
                transform.translate(this.translate);
            } else {
                transform.translate(-this.translate.x, this.translate.y, this.translate.z);
            }
            float rotateY = this.rotate.y;
            transform.rotateXYZ(this.rotate.x * ((float)Math.PI / 180), rotateY * ((float)Math.PI / 180), this.rotate.z * ((float)Math.PI / 180));
            transform.scale(this.scale.x, this.scale.y, this.scale.z);
            if (sceneModel != null && bInvertX) {
                transform.scale(-1.5f, 1.5f, 1.5f);
            } else if (sceneModel != null && sceneModel.useWorldAttachment) {
                transform.scale(-1.5f, 1.5f, 1.5f);
            }
            if (this.attachment != null) {
                org.joml.Matrix4f attachmentXfrm = this.getAttachmentTransform(this.attachment, UI3DScene.allocMatrix4f());
                if (ModelInstanceRenderData.invertAttachmentSelfTransform) {
                    attachmentXfrm.invert();
                }
                transform.mul(attachmentXfrm);
                UI3DScene.releaseMatrix4f(attachmentXfrm);
            }
            if (this.autoRotate) {
                transform.rotateY(this.autoRotateAngle * ((float)Math.PI / 180));
            }
            return transform;
        }

        org.joml.Matrix4f getGlobalTransform(org.joml.Matrix4f transform) {
            org.joml.Matrix4f parentXfrm;
            this.getLocalTransform(transform);
            if (this.parent != null) {
                if (this.parentAttachment != null) {
                    org.joml.Matrix4f attachmentXfrm = this.parent.getAttachmentTransform(this.parentAttachment, UI3DScene.allocMatrix4f());
                    attachmentXfrm.mul(transform, transform);
                    UI3DScene.releaseMatrix4f(attachmentXfrm);
                }
                parentXfrm = this.parent.getGlobalTransform(UI3DScene.allocMatrix4f());
                parentXfrm.mul(transform, transform);
                UI3DScene.releaseMatrix4f(parentXfrm);
            }
            if (this.parentVehiclePart != null) {
                parentXfrm = this.parentVehiclePart.getGlobalTransform(UI3DScene.allocMatrix4f());
                org.joml.Matrix4f attachmentXfrm = this.getAttachmentTransform(this.parentVehiclePart.attachmentName, UI3DScene.allocMatrix4f());
                transform.mul(attachmentXfrm);
                UI3DScene.releaseMatrix4f(attachmentXfrm);
                parentXfrm.mul(transform, transform);
                UI3DScene.releaseMatrix4f(parentXfrm);
            }
            return transform;
        }

        org.joml.Matrix4f getAttachmentTransform(String attachmentName, org.joml.Matrix4f transform) {
            transform.identity();
            return transform;
        }
    }

    private static class SceneObjectRenderData {
        SceneObject object;
        final org.joml.Matrix4f transform = new org.joml.Matrix4f();
        private static final ObjectPool<SceneObjectRenderData> s_pool = new ObjectPool<SceneObjectRenderData>(SceneObjectRenderData::new);

        private SceneObjectRenderData() {
        }

        SceneObjectRenderData init(SceneObject sceneObject) {
            this.object = sceneObject;
            sceneObject.getGlobalTransform(this.transform);
            return this;
        }

        void release() {
            s_pool.release(this);
        }
    }

    private abstract class Gizmo {
        static final float LENGTH = 0.5f;
        static final float THICKNESS = 0.05f;
        boolean visible;
        final /* synthetic */ UI3DScene this$0;

        private Gizmo(UI3DScene uI3DScene) {
            UI3DScene uI3DScene2 = uI3DScene;
            Objects.requireNonNull(uI3DScene2);
            this.this$0 = uI3DScene2;
        }

        abstract Axis hitTest(float var1, float var2);

        abstract void startTracking(float var1, float var2, Axis var3);

        abstract void updateTracking(float var1, float var2);

        abstract void stopTracking();

        abstract void render();

        Vector3f getPointOnAxis(float uiX, float uiY, Axis axis1, org.joml.Matrix4f gizmoXfrm, Vector3f out) {
            StateData stateData = this.this$0.stateDataMain();
            if (axis1 == Axis.XY || axis1 == Axis.XZ || axis1 == Axis.YZ) {
                Vector3f planePoint = gizmoXfrm.transformPosition(UI3DScene.allocVector3f().set(0.0f, 0.0f, 0.0f));
                Vector3f planeRotate = UI3DScene.allocVector3f();
                GridPlane gridPlane = GridPlane.XY;
                switch (axis1.ordinal()) {
                    case 4: {
                        planeRotate.set(0.0f, 0.0f, 0.0f);
                        gridPlane = GridPlane.XY;
                        break;
                    }
                    case 5: {
                        planeRotate.set(90.0f, 0.0f, 0.0f);
                        gridPlane = GridPlane.XZ;
                        break;
                    }
                    case 6: {
                        planeRotate.set(0.0f, 90.0f, 0.0f);
                        gridPlane = GridPlane.YZ;
                    }
                }
                this.this$0.polygonEditor.setPlane(planePoint, planeRotate, gridPlane);
                this.this$0.polygonEditor.uiToPlane3D(uiX, uiY, out.set(0.0f));
                UI3DScene.releaseVector3f(planePoint);
                UI3DScene.releaseVector3f(planeRotate);
                return out;
            }
            uiY = (float)this.this$0.screenHeight() - uiY;
            Ray cameraRay = this.this$0.getCameraRay(uiX, uiY, UI3DScene.allocRay());
            Ray axis = UI3DScene.allocRay();
            gizmoXfrm.transformPosition(axis.origin.set(0.0f, 0.0f, 0.0f));
            switch (axis1.ordinal()) {
                case 1: {
                    axis.direction.set(1.0f, 0.0f, 0.0f);
                    break;
                }
                case 2: {
                    axis.direction.set(0.0f, 1.0f, 0.0f);
                    break;
                }
                case 3: {
                    axis.direction.set(0.0f, 0.0f, 1.0f);
                }
            }
            gizmoXfrm.transformDirection(axis.direction).normalize();
            UI3DScene.closest_distance_between_lines(axis, cameraRay);
            UI3DScene.releaseRay(cameraRay);
            out.set(axis.direction).mul(axis.t).add(axis.origin);
            UI3DScene.releaseRay(axis);
            return out;
        }

        boolean hitTestRect(float uiX, float uiY, float x0, float y0, float z0, float x1, float y1, float z1) {
            float xx0 = this.this$0.sceneToUIX(x0, y0, z0);
            float yy0 = this.this$0.sceneToUIY(x0, y0, z0);
            float xx1 = this.this$0.sceneToUIX(x1, y1, z1);
            float yy1 = this.this$0.sceneToUIY(x1, y1, z1);
            float dx = 0.025f * this.this$0.zoomMult();
            float dy = 0.025f * this.this$0.zoomMult();
            float xmin = Math.min(xx0 - dx, xx1 - dx);
            float xmax = Math.max(xx0 + dx, xx1 + dx);
            float ymin = Math.min(yy0 - dy, yy1 - dy);
            float ymax = Math.max(yy0 + dy, yy1 + dy);
            return uiX >= xmin && uiY >= ymin && uiX < xmax && uiY < ymax;
        }

        boolean getPointOnDualAxis(float uiX, float uiY, Axis axis, org.joml.Matrix4f gizmoXfrm, Vector3f pointOnPlane3D, Vector2f pointOnPlane2D) {
            Plane plane = UI3DScene.allocPlane();
            gizmoXfrm.transformPosition(plane.point.set(0.0f, 0.0f, 0.0f));
            switch (axis.ordinal()) {
                case 4: {
                    plane.normal.set(0.0f, 0.0f, 1.0f);
                    break;
                }
                case 5: {
                    plane.normal.set(0.0f, 1.0f, 0.0f);
                    break;
                }
                case 6: {
                    plane.normal.set(1.0f, 0.0f, 0.0f);
                }
            }
            gizmoXfrm.transformDirection(plane.normal);
            Ray cameraRay = this.this$0.getCameraRay(uiX, (float)this.this$0.screenHeight() - uiY, UI3DScene.allocRay());
            boolean hit = UI3DScene.intersect_ray_plane(plane, cameraRay, pointOnPlane3D) == 1;
            UI3DScene.releaseRay(cameraRay);
            UI3DScene.releasePlane(plane);
            if (hit) {
                org.joml.Matrix4f m = UI3DScene.allocMatrix4f().set(gizmoXfrm);
                m.invert();
                Vector3f localToPlaneOrigin = m.transformPosition(pointOnPlane3D, UI3DScene.allocVector3f());
                UI3DScene.releaseMatrix4f(m);
                switch (axis.ordinal()) {
                    case 4: {
                        pointOnPlane2D.set(localToPlaneOrigin.x, localToPlaneOrigin.y);
                        break;
                    }
                    case 5: {
                        pointOnPlane2D.set(localToPlaneOrigin.x, localToPlaneOrigin.z);
                        break;
                    }
                    case 6: {
                        pointOnPlane2D.set(localToPlaneOrigin.y, localToPlaneOrigin.z);
                    }
                }
                UI3DScene.releaseVector3f(localToPlaneOrigin);
                return true;
            }
            return false;
        }

        void renderLineToOrigin() {
            StateData stateData = this.this$0.stateDataRender();
            if (!stateData.hasGizmoOrigin) {
                return;
            }
            this.this$0.renderAxis(stateData.gizmoTranslate, stateData.gizmoRotate, false);
            Vector3f gizmoPos = stateData.gizmoTranslate;
            vboRenderer.flush();
            org.joml.Matrix4f matrix4f = UI3DScene.allocMatrix4f();
            matrix4f.set(stateData.modelView);
            matrix4f.mul(stateData.gizmoParentTransform);
            matrix4f.mul(stateData.gizmoOriginTransform);
            matrix4f.mul(stateData.gizmoChildTransform);
            if (stateData.selectedAttachmentIsChildAttachment) {
                matrix4f.mul(stateData.gizmoChildAttachmentTransformInv);
            }
            vboRenderer.cmdPushAndLoadMatrix(5888, matrix4f);
            UI3DScene.releaseMatrix4f(matrix4f);
            vboRenderer.startRun(UI3DScene.vboRenderer.formatPositionColor);
            vboRenderer.setMode(1);
            vboRenderer.setLineWidth(2.0f);
            vboRenderer.addLine(gizmoPos.x, gizmoPos.y, gizmoPos.z, 0.0f, 0.0f, 0.0f, 1.0f, 1.0f, 1.0f, 1.0f);
            vboRenderer.endRun();
            vboRenderer.cmdPopMatrix(5888);
            vboRenderer.flush();
        }
    }

    static enum Axis {
        None,
        X,
        Y,
        Z,
        XY,
        XZ,
        YZ;

    }

    private static abstract class SceneCharacter
    extends SceneObject {
        final AnimatedModel animatedModel = new AnimatedModel();
        boolean showBones;
        boolean showBip01;
        boolean clearDepthBuffer = true;
        boolean useDeferredMovement;

        SceneCharacter(UI3DScene scene, String id) {
            super(scene, id);
        }

        abstract void initAnimatedModel();

        @Override
        SceneObjectRenderData renderMain() {
            this.animatedModel.update();
            CharacterRenderData renderData = CharacterRenderData.s_pool.alloc();
            renderData.initCharacter(this);
            SpriteRenderer.instance.drawGeneric(renderData.drawer);
            return renderData;
        }

        @Override
        org.joml.Matrix4f getLocalTransform(org.joml.Matrix4f transform) {
            transform.identity();
            transform.translate(this.translate.x, this.translate.y, this.translate.z);
            float rotateY = this.rotate.y;
            transform.rotateXYZ(-this.rotate.x * ((float)Math.PI / 180), -rotateY * ((float)Math.PI / 180), this.rotate.z * ((float)Math.PI / 180));
            transform.scale(1.5f * this.scale.x, 1.5f * this.scale.y, 1.5f * this.scale.z);
            org.joml.Matrix4f m = UI3DScene.allocMatrix4f();
            m.identity();
            m.rotateY((float)Math.PI);
            m.scale(-1.0f, 1.0f, 1.0f);
            transform.mul(m);
            UI3DScene.releaseMatrix4f(m);
            if (this.autoRotate) {
                transform.rotateY(this.autoRotateAngle * ((float)Math.PI / 180));
            }
            if (this.animatedModel.getAnimationPlayer().getMultiTrack().getTracks().isEmpty()) {
                return transform;
            }
            if (this.useDeferredMovement) {
                AnimationMultiTrack multiTrack = this.animatedModel.getAnimationPlayer().getMultiTrack();
                float deferredRotation = multiTrack.getTracks().get(0).getCurrentDeferredRotation();
                org.lwjgl.util.vector.Vector3f translate = new org.lwjgl.util.vector.Vector3f();
                multiTrack.getTracks().get(0).getCurrentDeferredPosition(translate);
                transform.translate(translate.x, translate.y, translate.z);
            }
            return transform;
        }

        @Override
        org.joml.Matrix4f getAttachmentTransform(String attachmentName, org.joml.Matrix4f transform) {
            transform.identity();
            boolean bFemale = this.animatedModel.isFemale();
            ModelScript modelScript = ScriptManager.instance.getModelScript(bFemale ? "FemaleBody" : "MaleBody");
            if (modelScript == null) {
                return transform;
            }
            ModelAttachment attachment = modelScript.getAttachmentById(attachmentName);
            if (attachment == null) {
                return transform;
            }
            ModelInstanceRenderData.makeAttachmentTransform(attachment, transform);
            if (attachment.getBone() != null) {
                org.joml.Matrix4f boneXfrm = this.getBoneMatrix(attachment.getBone(), UI3DScene.allocMatrix4f());
                boneXfrm.mul(transform, transform);
                UI3DScene.releaseMatrix4f(boneXfrm);
            }
            return transform;
        }

        int hitTestBone(int boneIndex, Ray boneRay, Ray cameraRay, org.joml.Matrix4f characterMatrix, Vector2f out) {
            AnimationPlayer animPlayer = this.animatedModel.getAnimationPlayer();
            SkinningData skinningData = animPlayer.getSkinningData();
            int parentIndex = skinningData.skeletonHierarchy.get(boneIndex);
            if (parentIndex == -1) {
                return -1;
            }
            Matrix4f boneMatrix = animPlayer.getModelTransformAt(parentIndex);
            boneRay.origin.set(boneMatrix.m03, boneMatrix.m13, boneMatrix.m23);
            characterMatrix.transformPosition(boneRay.origin);
            boneMatrix = animPlayer.getModelTransformAt(boneIndex);
            Vector3f nextBone = UI3DScene.allocVector3f();
            nextBone.set(boneMatrix.m03, boneMatrix.m13, boneMatrix.m23);
            characterMatrix.transformPosition(nextBone);
            boneRay.direction.set(nextBone).sub(boneRay.origin);
            float boneLength = boneRay.direction.length();
            boneRay.direction.normalize();
            UI3DScene.closest_distance_between_lines(cameraRay, boneRay);
            float camX = this.scene.sceneToUIX(cameraRay.origin.x + cameraRay.direction.x * cameraRay.t, cameraRay.origin.y + cameraRay.direction.y * cameraRay.t, cameraRay.origin.z + cameraRay.direction.z * cameraRay.t);
            float camY = this.scene.sceneToUIY(cameraRay.origin.x + cameraRay.direction.x * cameraRay.t, cameraRay.origin.y + cameraRay.direction.y * cameraRay.t, cameraRay.origin.z + cameraRay.direction.z * cameraRay.t);
            float boneX = this.scene.sceneToUIX(boneRay.origin.x + boneRay.direction.x * boneRay.t, boneRay.origin.y + boneRay.direction.y * boneRay.t, boneRay.origin.z + boneRay.direction.z * boneRay.t);
            float boneY = this.scene.sceneToUIY(boneRay.origin.x + boneRay.direction.x * boneRay.t, boneRay.origin.y + boneRay.direction.y * boneRay.t, boneRay.origin.z + boneRay.direction.z * boneRay.t);
            int hitBone = -1;
            float pickDist = 10.0f;
            float dist = (float)Math.sqrt(Math.pow(boneX - camX, 2.0) + Math.pow(boneY - camY, 2.0));
            if (dist < 10.0f) {
                if (boneRay.t >= 0.0f && boneRay.t <= 0.01f || boneRay.t >= 0.09f && boneRay.t <= 1.0f) {
                    out.set(dist / 10.0f, 0.0f);
                } else {
                    out.set(dist, 0.0f);
                }
                if (boneRay.t >= 0.0f && boneRay.t < boneLength * 0.5f) {
                    hitBone = parentIndex;
                } else if (boneRay.t >= boneLength * 0.5f && boneRay.t < boneLength) {
                    hitBone = boneIndex;
                }
            }
            UI3DScene.releaseVector3f(nextBone);
            return hitBone;
        }

        String pickBone(float uiX, float uiY) {
            if (this.animatedModel.getAnimationPlayer().getModelTransformsCount() == 0) {
                return "";
            }
            uiY = (float)this.scene.screenHeight() - uiY;
            Ray cameraRay = this.scene.getCameraRay(uiX, uiY, UI3DScene.allocRay());
            org.joml.Matrix4f characterXfrm = UI3DScene.allocMatrix4f();
            this.getLocalTransform(characterXfrm);
            Ray boneRay = UI3DScene.allocRay();
            int hitBoneIndex = -1;
            Vector2f dist = UI3DScene.allocVector2f();
            float closestDist = Float.MAX_VALUE;
            for (int i = 0; i < this.animatedModel.getAnimationPlayer().getModelTransformsCount(); ++i) {
                int testBoneIndex = this.hitTestBone(i, boneRay, cameraRay, characterXfrm, dist);
                if (testBoneIndex == -1 || !(dist.x < closestDist)) continue;
                closestDist = dist.x;
                hitBoneIndex = testBoneIndex;
            }
            UI3DScene.releaseVector2f(dist);
            UI3DScene.releaseRay(boneRay);
            UI3DScene.releaseRay(cameraRay);
            UI3DScene.releaseMatrix4f(characterXfrm);
            return hitBoneIndex == -1 ? "" : this.animatedModel.getAnimationPlayer().getSkinningData().getBoneAt((int)hitBoneIndex).name;
        }

        org.joml.Matrix4f getBoneMatrix(String boneName, org.joml.Matrix4f mat) {
            mat.identity();
            if (this.animatedModel.getAnimationPlayer().getModelTransformsCount() == 0) {
                return mat;
            }
            SkinningBone bone = this.animatedModel.getAnimationPlayer().getSkinningData().getBone(boneName);
            if (bone == null) {
                return mat;
            }
            mat = PZMath.convertMatrix(this.animatedModel.getAnimationPlayer().getModelTransformAt(bone.index), mat);
            mat.transpose();
            return mat;
        }

        PositionRotation getBoneAxis(String boneName, PositionRotation axis) {
            org.joml.Matrix4f mat = UI3DScene.allocMatrix4f().identity();
            mat.getTranslation(axis.pos);
            UI3DScene.releaseMatrix4f(mat);
            Quaternionf q = mat.getUnnormalizedRotation(UI3DScene.allocQuaternionf());
            q.getEulerAnglesXYZ(axis.rot);
            UI3DScene.releaseQuaternionf(q);
            return axis;
        }
    }

    private static final class SceneModel
    extends SceneObject {
        SpriteModel spriteModel;
        ModelScript modelScript;
        Model model;
        Texture texture;
        boolean useWorldAttachment;
        boolean weaponRotationHack;
        boolean ignoreVehicleScale;
        boolean spriteModelEditor;
        static AnimationPlayer animationPlayer;

        SceneModel(UI3DScene scene, String id, ModelScript modelScript, Model model) {
            super(scene, id);
            Objects.requireNonNull(modelScript);
            Objects.requireNonNull(model);
            this.modelScript = modelScript;
            this.model = model;
            this.setSpriteModel(null);
        }

        void setSpriteModel(SpriteModel spriteModel) {
            this.spriteModel = spriteModel;
            this.texture = null;
            if (spriteModel != null && spriteModel.getTextureName() != null) {
                this.texture = spriteModel.getTextureName().contains("media/") ? Texture.getSharedTexture(spriteModel.getTextureName()) : Texture.getSharedTexture("media/textures/" + spriteModel.getTextureName() + ".png");
            }
        }

        @Override
        SceneObjectRenderData renderMain() {
            if (!this.model.isReady()) {
                return null;
            }
            ModelRenderData renderData = ModelRenderData.s_pool.alloc();
            renderData.initModel(this);
            SpriteRenderer.instance.drawGeneric(renderData.drawer);
            return renderData;
        }

        @Override
        org.joml.Matrix4f getLocalTransform(org.joml.Matrix4f transform) {
            super.getLocalTransform(transform);
            return transform;
        }

        @Override
        org.joml.Matrix4f getAttachmentTransform(String attachmentName, org.joml.Matrix4f transform) {
            transform.identity();
            ModelAttachment attachment = this.modelScript.getAttachmentById(attachmentName);
            if (attachment == null) {
                return transform;
            }
            ModelInstanceRenderData.makeAttachmentTransform(attachment, transform);
            return transform;
        }

        static AnimationPlayer initAnimationPlayer(Model model) {
            if (animationPlayer != null) {
                while (animationPlayer.getMultiTrack().getTrackCount() > 0) {
                    animationPlayer.getMultiTrack().removeTrackAt(0);
                }
            }
            if (animationPlayer != null && animationPlayer.getModel() != model) {
                animationPlayer.release();
                animationPlayer = null;
            }
            if (animationPlayer == null) {
                animationPlayer = AnimationPlayer.alloc(model);
            }
            if (!animationPlayer.isReady()) {
                return null;
            }
            AnimationTrack track = animationPlayer.play("Open", false);
            if (track == null) {
                return null;
            }
            float duration = track.getDuration();
            track.setCurrentTimeValue(0.0f);
            track.setBlendWeight(1.0f);
            track.setSpeedDelta(1.0f);
            track.isPlaying = false;
            track.reverse = false;
            animationPlayer.Update(100.0f);
            return animationPlayer;
        }

        int hitTestBone(int boneIndex, Ray boneRay, Ray cameraRay, org.joml.Matrix4f characterMatrix, Vector2f out) {
            AnimationPlayer animPlayer = animationPlayer;
            SkinningData skinningData = animPlayer.getSkinningData();
            int parentIndex = skinningData.skeletonHierarchy.get(boneIndex);
            if (parentIndex == -1) {
                return -1;
            }
            Matrix4f boneMatrix = animPlayer.getModelTransformAt(parentIndex);
            boneRay.origin.set(boneMatrix.m03, boneMatrix.m13, boneMatrix.m23);
            characterMatrix.transformPosition(boneRay.origin);
            boneMatrix = animPlayer.getModelTransformAt(boneIndex);
            Vector3f nextBone = UI3DScene.allocVector3f();
            nextBone.set(boneMatrix.m03, boneMatrix.m13, boneMatrix.m23);
            characterMatrix.transformPosition(nextBone);
            boneRay.direction.set(nextBone).sub(boneRay.origin);
            float boneLength = boneRay.direction.length();
            if (boneLength < 0.001f) {
                if (PZArrayUtil.contains(skinningData.skeletonHierarchy, i -> i == boneIndex)) {
                    return -1;
                }
                boneRay.direction.set(0.0f, 1.0f, 0.0f);
                boneLength = 1.0f;
            }
            boneRay.direction.normalize();
            UI3DScene.closest_distance_between_lines(cameraRay, boneRay);
            float camX = this.scene.sceneToUIX(cameraRay.origin.x + cameraRay.direction.x * cameraRay.t, cameraRay.origin.y + cameraRay.direction.y * cameraRay.t, cameraRay.origin.z + cameraRay.direction.z * cameraRay.t);
            float camY = this.scene.sceneToUIY(cameraRay.origin.x + cameraRay.direction.x * cameraRay.t, cameraRay.origin.y + cameraRay.direction.y * cameraRay.t, cameraRay.origin.z + cameraRay.direction.z * cameraRay.t);
            float boneX = this.scene.sceneToUIX(boneRay.origin.x + boneRay.direction.x * boneRay.t, boneRay.origin.y + boneRay.direction.y * boneRay.t, boneRay.origin.z + boneRay.direction.z * boneRay.t);
            float boneY = this.scene.sceneToUIY(boneRay.origin.x + boneRay.direction.x * boneRay.t, boneRay.origin.y + boneRay.direction.y * boneRay.t, boneRay.origin.z + boneRay.direction.z * boneRay.t);
            int hitBone = -1;
            float pickDist = 10.0f;
            float dist = (float)Math.sqrt(Math.pow(boneX - camX, 2.0) + Math.pow(boneY - camY, 2.0));
            if (dist < 10.0f) {
                if (boneRay.t >= 0.0f && boneRay.t <= 0.01f || boneRay.t >= 0.09f && boneRay.t <= 1.0f) {
                    out.set(dist / 10.0f, 0.0f);
                } else {
                    out.set(dist, 0.0f);
                }
                if (boneRay.t >= 0.0f && boneRay.t < boneLength * 0.5f) {
                    hitBone = parentIndex;
                } else if (boneRay.t >= boneLength * 0.5f && boneRay.t < boneLength) {
                    hitBone = boneIndex;
                }
            }
            UI3DScene.releaseVector3f(nextBone);
            return hitBone;
        }

        String pickBone(float uiX, float uiY) {
            AnimationPlayer animationPlayer = SceneModel.initAnimationPlayer(this.model);
            if (animationPlayer == null) {
                return "";
            }
            if (animationPlayer.getModelTransformsCount() == 0) {
                return "";
            }
            uiY = (float)this.scene.screenHeight() - uiY;
            Ray cameraRay = this.scene.getCameraRay(uiX, uiY, UI3DScene.allocRay());
            org.joml.Matrix4f characterXfrm = UI3DScene.allocMatrix4f();
            this.getLocalTransform(characterXfrm);
            Ray boneRay = UI3DScene.allocRay();
            int hitBoneIndex = -1;
            Vector2f dist = UI3DScene.allocVector2f();
            float closestDist = Float.MAX_VALUE;
            for (int i = 0; i < animationPlayer.getModelTransformsCount(); ++i) {
                int testBoneIndex = this.hitTestBone(i, boneRay, cameraRay, characterXfrm, dist);
                if (testBoneIndex == -1 || !(dist.x < closestDist)) continue;
                closestDist = dist.x;
                hitBoneIndex = testBoneIndex;
            }
            UI3DScene.releaseVector2f(dist);
            UI3DScene.releaseRay(boneRay);
            UI3DScene.releaseRay(cameraRay);
            UI3DScene.releaseMatrix4f(characterXfrm);
            return hitBoneIndex == -1 ? "" : animationPlayer.getSkinningData().getBoneAt((int)hitBoneIndex).name;
        }

        org.joml.Matrix4f getBoneMatrix(String boneName, org.joml.Matrix4f mat) {
            mat.identity();
            AnimationPlayer animationPlayer = SceneModel.initAnimationPlayer(this.model);
            if (animationPlayer == null) {
                return mat;
            }
            if (animationPlayer.getModelTransformsCount() == 0) {
                return mat;
            }
            SkinningBone bone = animationPlayer.getSkinningData().getBone(boneName);
            if (bone == null) {
                return mat;
            }
            mat = PZMath.convertMatrix(animationPlayer.getModelTransformAt(bone.index), mat);
            mat.transpose();
            return mat;
        }
    }

    private static final class SceneVehicle
    extends SceneObject {
        String scriptName = "Base.ModernCar";
        VehicleScript script;
        final ArrayList<SceneVehicleModelInfo> modelInfo = new ArrayList();
        String showBonesPartId;
        String showBonesModelId;
        boolean init;

        SceneVehicle(UI3DScene scene, String id) {
            super(scene, id);
            this.setScriptName("Base.ModernCar");
        }

        @Override
        SceneObjectRenderData renderMain() {
            if (this.script == null) {
                return null;
            }
            if (!this.init) {
                this.init = true;
                String modelName = this.script.getModel().file;
                Model model = ModelManager.instance.getLoadedModel(modelName);
                if (model == null) {
                    return null;
                }
                SceneVehicleModelInfo modelInfo = SceneVehicleModelInfo.s_pool.alloc();
                modelInfo.sceneVehicle = this;
                modelInfo.part = null;
                modelInfo.scriptModel = this.script.getModel();
                modelInfo.modelScript = ScriptManager.instance.getModelScript(modelInfo.scriptModel.file);
                modelInfo.wheelIndex = -1;
                modelInfo.model = model;
                modelInfo.tex = model.tex;
                if (this.script.getSkinCount() > 0) {
                    modelInfo.tex = Texture.getSharedTexture("media/textures/" + this.script.getSkin((int)0).texture + ".png");
                }
                modelInfo.releaseAnimationPlayer();
                modelInfo.animPlayer = null;
                modelInfo.track = null;
                this.modelInfo.add(modelInfo);
                for (int i = 0; i < this.script.getPartCount(); ++i) {
                    VehicleScript.Part scriptPart = this.script.getPart(i);
                    if (scriptPart.wheel != null) continue;
                    for (int j = 0; j < scriptPart.getModelCount(); ++j) {
                        VehicleScript.Model scriptModel = scriptPart.getModel(j);
                        modelName = scriptModel.file;
                        if (modelName == null || (model = ModelManager.instance.getLoadedModel(modelName)) == null) continue;
                        modelInfo = SceneVehicleModelInfo.s_pool.alloc();
                        modelInfo.sceneVehicle = this;
                        modelInfo.part = scriptPart;
                        modelInfo.scriptModel = scriptModel;
                        modelInfo.modelScript = ScriptManager.instance.getModelScript(scriptModel.file);
                        modelInfo.wheelIndex = -1;
                        modelInfo.model = model;
                        modelInfo.tex = model.tex;
                        modelInfo.releaseAnimationPlayer();
                        modelInfo.animPlayer = null;
                        modelInfo.track = null;
                        this.modelInfo.add(modelInfo);
                    }
                }
            }
            if (this.modelInfo.isEmpty()) {
                return null;
            }
            VehicleRenderData renderData = VehicleRenderData.s_pool.alloc();
            renderData.initVehicle(this);
            SetModelCamera setModelCamera = s_SetModelCameraPool.alloc();
            SpriteRenderer.instance.drawGeneric(setModelCamera.init(this.scene.vehicleSceneModelCamera, renderData));
            SpriteRenderer.instance.drawGeneric(renderData.drawer);
            return renderData;
        }

        @Override
        org.joml.Matrix4f getAttachmentTransform(String attachmentName, org.joml.Matrix4f transform) {
            transform.identity();
            ModelAttachment attachment = this.script.getAttachmentById(attachmentName);
            if (attachment == null) {
                return transform;
            }
            ModelInstanceRenderData.makeAttachmentTransform(attachment, transform);
            if (attachment.getBone() != null) {
                org.joml.Matrix4f boneXfrm = this.getBoneMatrix(attachment.getBone(), UI3DScene.allocMatrix4f());
                boneXfrm.mul(transform, transform);
                UI3DScene.releaseMatrix4f(boneXfrm);
            }
            return transform;
        }

        org.joml.Matrix4f getBoneMatrix(String boneName, org.joml.Matrix4f mat) {
            mat.identity();
            if (this.modelInfo.isEmpty()) {
                return mat;
            }
            SceneVehicleModelInfo modelInfo = this.modelInfo.get(0);
            if (modelInfo == null) {
                return mat;
            }
            AnimationPlayer animationPlayer = modelInfo.getAnimationPlayer();
            if (animationPlayer == null) {
                return mat;
            }
            if (animationPlayer.getModelTransformsCount() == 0) {
                return mat;
            }
            SkinningBone bone = animationPlayer.getSkinningData().getBone(boneName);
            if (bone == null) {
                return mat;
            }
            mat = PZMath.convertMatrix(animationPlayer.getModelTransformAt(bone.index), mat);
            mat.transpose();
            return mat;
        }

        org.joml.Matrix4f getTransformForPart(String partId, String partModelId, String attachmentName, boolean bBoneOnly, org.joml.Matrix4f transform) {
            transform.identity();
            VehicleScript.Part part = this.script.getPartById(partId);
            if (part == null) {
                return transform;
            }
            VehicleScript.Model partModel = part.getModelById(partModelId);
            if (partModel == null) {
                return transform;
            }
            if (partModel.getFile() == null) {
                return transform;
            }
            ModelScript modelScript = ScriptManager.instance.getModelScript(partModel.getFile());
            if (modelScript == null) {
                return transform;
            }
            ModelAttachment attachment = modelScript.getAttachmentById(attachmentName);
            if (attachment == null) {
                return transform;
            }
            transform.scale(1.0f / modelScript.scale);
            if (attachment.getBone() != null) {
                org.joml.Matrix4f boneXfrm = this.getBoneMatrix(part, attachment, UI3DScene.allocMatrix4f());
                boneXfrm.mul(transform, transform);
                UI3DScene.releaseMatrix4f(boneXfrm);
            }
            if (bBoneOnly) {
                return transform;
            }
            org.joml.Matrix4f attachmentXfrm = ModelInstanceRenderData.makeAttachmentTransform(attachment, UI3DScene.allocMatrix4f());
            transform.mul(attachmentXfrm);
            UI3DScene.releaseMatrix4f(attachmentXfrm);
            return transform;
        }

        org.joml.Matrix4f getBoneMatrix(VehicleScript.Part part, ModelAttachment attachment, org.joml.Matrix4f mat) {
            mat.identity();
            SceneVehicleModelInfo modelInfo = this.getModelInfoForPart(part.getId());
            if (modelInfo == null) {
                return mat;
            }
            AnimationPlayer animationPlayer = modelInfo.getAnimationPlayer();
            if (animationPlayer == null) {
                return mat;
            }
            if (animationPlayer.getModelTransformsCount() == 0) {
                return mat;
            }
            SkinningBone bone = animationPlayer.getSkinningData().getBone(attachment.getBone());
            if (bone == null) {
                return mat;
            }
            mat = PZMath.convertMatrix(animationPlayer.getModelTransformAt(bone.index), mat);
            mat.transpose();
            return mat;
        }

        int hitTestBone(int boneIndex, Ray boneRay, Ray cameraRay, AnimationPlayer animPlayer, org.joml.Matrix4f modelMatrix, Vector2f out) {
            SkinningData skinningData = animPlayer.getSkinningData();
            int parentIndex = skinningData.skeletonHierarchy.get(boneIndex);
            if (parentIndex == -1) {
                return -1;
            }
            Matrix4f boneMatrix = animPlayer.getModelTransformAt(parentIndex);
            boneRay.origin.set(boneMatrix.m03, boneMatrix.m13, boneMatrix.m23);
            modelMatrix.transformPosition(boneRay.origin);
            boneMatrix = animPlayer.getModelTransformAt(boneIndex);
            Vector3f nextBone = UI3DScene.allocVector3f();
            nextBone.set(boneMatrix.m03, boneMatrix.m13, boneMatrix.m23);
            modelMatrix.transformPosition(nextBone);
            boneRay.direction.set(nextBone).sub(boneRay.origin);
            float boneLength = boneRay.direction.length();
            boneRay.direction.normalize();
            UI3DScene.closest_distance_between_lines(cameraRay, boneRay);
            float camX = this.scene.sceneToUIX(cameraRay.origin.x + cameraRay.direction.x * cameraRay.t, cameraRay.origin.y + cameraRay.direction.y * cameraRay.t, cameraRay.origin.z + cameraRay.direction.z * cameraRay.t);
            float camY = this.scene.sceneToUIY(cameraRay.origin.x + cameraRay.direction.x * cameraRay.t, cameraRay.origin.y + cameraRay.direction.y * cameraRay.t, cameraRay.origin.z + cameraRay.direction.z * cameraRay.t);
            float boneX = this.scene.sceneToUIX(boneRay.origin.x + boneRay.direction.x * boneRay.t, boneRay.origin.y + boneRay.direction.y * boneRay.t, boneRay.origin.z + boneRay.direction.z * boneRay.t);
            float boneY = this.scene.sceneToUIY(boneRay.origin.x + boneRay.direction.x * boneRay.t, boneRay.origin.y + boneRay.direction.y * boneRay.t, boneRay.origin.z + boneRay.direction.z * boneRay.t);
            int hitBone = -1;
            float pickDist = 10.0f;
            float dist = (float)Math.sqrt(Math.pow(boneX - camX, 2.0) + Math.pow(boneY - camY, 2.0));
            if (dist < 10.0f) {
                if (boneRay.t >= 0.0f && boneRay.t <= 0.01f || boneRay.t >= 0.09f && boneRay.t <= 1.0f) {
                    out.set(dist / 10.0f, 0.0f);
                } else {
                    out.set(dist, 0.0f);
                }
                if (boneRay.t >= 0.0f && boneRay.t < boneLength * 0.5f) {
                    hitBone = parentIndex;
                } else if (boneRay.t >= boneLength * 0.5f && boneRay.t < boneLength) {
                    hitBone = boneIndex;
                }
            }
            UI3DScene.releaseVector3f(nextBone);
            return hitBone;
        }

        String pickBone(VehicleScript.Part part, VehicleScript.Model model, float uiX, float uiY) {
            SceneVehicleModelInfo modelInfo = this.getModelInfoForPart(part.getId(), model.getId());
            if (modelInfo == null) {
                return "";
            }
            AnimationPlayer animationPlayer = modelInfo.getAnimationPlayer();
            if (animationPlayer == null || animationPlayer.getModelTransformsCount() == 0) {
                return "";
            }
            uiY = (float)this.scene.screenHeight() - uiY;
            Ray cameraRay = this.scene.getCameraRay(uiX, uiY, UI3DScene.allocRay());
            org.joml.Matrix4f modelXfrm = UI3DScene.allocMatrix4f();
            this.getLocalTransform(modelXfrm);
            VehicleRenderData vrd = VehicleRenderData.s_pool.alloc();
            vrd.initVehicle(this);
            VehicleModelRenderData parentVMRD = vrd.partToRenderData.get(part.getId());
            if (parentVMRD != null) {
                modelXfrm.set(parentVMRD.xfrm);
            }
            vrd.release();
            Ray boneRay = UI3DScene.allocRay();
            int hitBoneIndex = -1;
            Vector2f dist = UI3DScene.allocVector2f();
            float closestDist = Float.MAX_VALUE;
            for (int i = 0; i < animationPlayer.getModelTransformsCount(); ++i) {
                int testBoneIndex = this.hitTestBone(i, boneRay, cameraRay, animationPlayer, modelXfrm, dist);
                if (testBoneIndex == -1 || !(dist.x < closestDist)) continue;
                closestDist = dist.x;
                hitBoneIndex = testBoneIndex;
            }
            UI3DScene.releaseVector2f(dist);
            UI3DScene.releaseRay(boneRay);
            UI3DScene.releaseRay(cameraRay);
            UI3DScene.releaseMatrix4f(modelXfrm);
            return hitBoneIndex == -1 ? "" : animationPlayer.getSkinningData().getBoneAt((int)hitBoneIndex).name;
        }

        void setScriptName(String scriptName) {
            this.scriptName = scriptName;
            this.script = ScriptManager.instance.getVehicle(scriptName);
            SceneVehicleModelInfo.s_pool.releaseAll((List<SceneVehicleModelInfo>)this.modelInfo);
            this.modelInfo.clear();
            this.init = false;
        }

        SceneVehicleModelInfo getModelInfoForPart(String partId) {
            for (int i = 0; i < this.modelInfo.size(); ++i) {
                SceneVehicleModelInfo modelInfo = this.modelInfo.get(i);
                if (modelInfo.part == null || !modelInfo.part.getId().equalsIgnoreCase(partId)) continue;
                return modelInfo;
            }
            return null;
        }

        SceneVehicleModelInfo getModelInfoForPart(String partId, String partModelId) {
            for (int i = 0; i < this.modelInfo.size(); ++i) {
                SceneVehicleModelInfo modelInfo = this.modelInfo.get(i);
                if (modelInfo.part == null || !modelInfo.part.getId().equalsIgnoreCase(partId) || !modelInfo.scriptModel.getId().equalsIgnoreCase(partModelId)) continue;
                return modelInfo;
            }
            return null;
        }
    }

    private static final class ScenePolygon
    extends SceneGeometry {
        GridPlane plane = GridPlane.XZ;
        final Vector3f extents = new Vector3f(1.0f);
        final ArrayList<Vector2f> points = new ArrayList();
        boolean editing = true;
        int highlightPointIndex = -1;
        final TFloatArrayList triangles = new TFloatArrayList();
        static final Rasterize s_rasterize = new Rasterize();

        ScenePolygon(UI3DScene scene, String id) {
            super(scene, id);
        }

        @Override
        public String getTypeName() {
            return "polygon";
        }

        @Override
        void initClone(SceneObject clone) {
            ScenePolygon polygon = (ScenePolygon)clone;
            super.initClone(clone);
            polygon.plane = this.plane;
            polygon.extents.set(this.extents);
            for (Vector2f point : this.points) {
                polygon.points.add(new Vector2f(point));
            }
        }

        @Override
        SceneObject clone(String id) {
            ScenePolygon clone = new ScenePolygon(this.scene, id);
            this.initClone(clone);
            return clone;
        }

        @Override
        SceneObjectRenderData renderMain() {
            if (!this.scene.drawGeometry) {
                return null;
            }
            ScenePolygonRenderData renderData = ScenePolygonRenderData.s_pool.alloc();
            renderData.initPolygon(this);
            SpriteRenderer.instance.drawGeneric(renderData.drawer);
            return renderData;
        }

        @Override
        org.joml.Matrix4f getLocalTransform(org.joml.Matrix4f transform) {
            super.getLocalTransform(transform);
            return transform;
        }

        @Override
        public boolean isPolygon() {
            return true;
        }

        @Override
        org.joml.Matrix4f getOriginTransform(String hint, org.joml.Matrix4f xfrm) {
            return this.getGlobalTransform(xfrm);
        }

        @Override
        float getNormalizedDepthAt(float tileX, float tileY) {
            Vector3f normal = UI3DScene.allocVector3f().set(0.0f, 0.0f, 1.0f);
            org.joml.Matrix4f m = UI3DScene.allocMatrix4f().rotationXYZ(this.rotate.x * ((float)Math.PI / 180), this.rotate.y * ((float)Math.PI / 180), this.rotate.z * ((float)Math.PI / 180));
            m.transformDirection(normal);
            UI3DScene.releaseMatrix4f(m);
            float depth = TileGeometryUtils.getNormalizedDepthOnPlaneAt(tileX, tileY, this.translate, normal);
            UI3DScene.releaseVector3f(normal);
            return depth;
        }

        @Override
        TileGeometryFile.Geometry toGeometryFileObject() {
            TileGeometryFile.Polygon tilePolygon = new TileGeometryFile.Polygon();
            tilePolygon.plane = TileGeometryFile.Plane.valueOf(this.plane.name());
            tilePolygon.translate.set(this.translate);
            tilePolygon.rotate.set(this.rotate);
            for (Vector2f point : this.points) {
                tilePolygon.points.add(point.x);
                tilePolygon.points.add(point.y);
            }
            return tilePolygon;
        }

        int addPointOnEdge(float uiX, float uiY, float pointX, float pointY) {
            if (this.pickPoint(uiX, uiY, 5.0f) != -1) {
                return -1;
            }
            int edgeIndex = this.pickEdge(uiX, uiY, 10.0f);
            if (edgeIndex == -1) {
                return -1;
            }
            Vector2f newPoint = new Vector2f();
            if (this.scene.polygonEditor.uiToPlane2D(pointX, pointY, newPoint)) {
                this.points.add(edgeIndex + 1, newPoint);
                this.triangulate();
                return edgeIndex;
            }
            return -1;
        }

        int pickEdge(float uiX, float uiY, float maxDist) {
            float closestDist = Float.MAX_VALUE;
            int segment = -1;
            this.scene.polygonEditor.setPlane(this.translate, this.rotate, this.plane);
            Vector2f p = UI3DScene.allocVector2f().set(uiX, uiY);
            Vector2f p1 = UI3DScene.allocVector2f();
            Vector2f p2 = UI3DScene.allocVector2f();
            for (int i = 0; i < this.points.size(); ++i) {
                this.scene.polygonEditor.planeToUI(this.points.get(i), p1);
                this.scene.polygonEditor.planeToUI(this.points.get((i + 1) % this.points.size()), p2);
                float dist = this.distanceOfPointToLineSegment(p1, p2, p);
                if (!(dist < closestDist) || !(dist < maxDist)) continue;
                closestDist = dist;
                segment = i;
            }
            UI3DScene.releaseVector2f(p);
            UI3DScene.releaseVector2f(p1);
            UI3DScene.releaseVector2f(p2);
            return segment;
        }

        float distanceOfPointToLineSegment(Vector2f p1, Vector2f p2, Vector2f p) {
            Vector2f pa;
            Vector2f n = UI3DScene.allocVector2f().set(p2).sub(p1);
            float c = n.dot(pa = UI3DScene.allocVector2f().set(p1).sub(p));
            if (c > 0.0f) {
                float result = pa.dot(pa);
                UI3DScene.releaseVector2f(n);
                UI3DScene.releaseVector2f(pa);
                return result;
            }
            Vector2f bp = UI3DScene.allocVector2f().set(p).sub(p2);
            if (n.dot(bp) > 0.0f) {
                float result = bp.dot(bp);
                UI3DScene.releaseVector2f(bp);
                UI3DScene.releaseVector2f(n);
                UI3DScene.releaseVector2f(pa);
                return result;
            }
            UI3DScene.releaseVector2f(bp);
            Vector2f e = UI3DScene.allocVector2f().set(n).mul(c / n.dot(n));
            pa.sub(e, e);
            float result = e.dot(e);
            UI3DScene.releaseVector2f(e);
            UI3DScene.releaseVector2f(n);
            UI3DScene.releaseVector2f(pa);
            return result;
        }

        boolean isClockwise() {
            float sum = 0.0f;
            for (int i = 0; i < this.points.size(); ++i) {
                float p1x = this.points.get((int)i).x;
                float p1y = this.points.get((int)i).y;
                float p2x = this.points.get((int)((i + 1) % this.points.size())).x;
                float p2y = this.points.get((int)((i + 1) % this.points.size())).y;
                sum += (p2x - p1x) * (p2y + p1y);
            }
            return (double)sum > 0.0;
        }

        void triangulate() {
            this.triangles.clear();
            if (this.points.size() < 3) {
                return;
            }
            if (clipper == null) {
                clipper = new Clipper();
            }
            clipper.clear();
            ByteBuffer bb = ByteBuffer.allocateDirect(8 * this.points.size() * 3);
            if (this.isClockwise()) {
                for (i = this.points.size() - 1; i >= 0; --i) {
                    point = this.points.get(i);
                    bb.putFloat(point.x);
                    bb.putFloat(point.y);
                }
            } else {
                for (i = 0; i < this.points.size(); ++i) {
                    point = this.points.get(i);
                    bb.putFloat(point.x);
                    bb.putFloat(point.y);
                }
            }
            clipper.addPath(this.points.size(), bb, false);
            int numPolygons = clipper.generatePolygons();
            if (numPolygons < 1) {
                return;
            }
            bb.clear();
            int numPoints = clipper.triangulate(0, bb);
            this.triangles.clear();
            for (int i = 0; i < numPoints; ++i) {
                this.triangles.add(bb.getFloat());
                this.triangles.add(bb.getFloat());
            }
        }

        int pickPoint(float uiX, float uiY, float maxDist) {
            float closestDist = Float.MAX_VALUE;
            int closestIndex = -1;
            Vector2f uiPos = UI3DScene.allocVector2f();
            this.scene.polygonEditor.setPlane(this.translate, this.rotate, this.plane);
            for (int i = 0; i < this.points.size(); ++i) {
                Vector2f point = this.points.get(i);
                this.scene.polygonEditor.planeToUI(point, uiPos);
                float dist = IsoUtils.DistanceTo2D(uiX, uiY, uiPos.x, uiPos.y);
                if (!(dist < maxDist) || !(dist < closestDist)) continue;
                closestDist = dist;
                closestIndex = i;
            }
            UI3DScene.releaseVector2f(uiPos);
            return closestIndex;
        }

        void renderPoints() {
            this.scene.polygonEditor.setPlane(this.translate, this.rotate, this.plane);
            Vector2f uiPos = UI3DScene.allocVector2f();
            for (int i = 0; i < this.points.size(); ++i) {
                Vector2f point = this.points.get(i);
                this.scene.polygonEditor.planeToUI(point, uiPos);
                if (i == this.highlightPointIndex) {
                    this.scene.DrawTextureScaledCol(null, (double)uiPos.x - 5.0, (double)uiPos.y - 5.0, 10.0, 10.0, 0.0, 1.0, 0.0, 1.0);
                    continue;
                }
                this.scene.DrawTextureScaledCol(null, (double)uiPos.x - 5.0, (double)uiPos.y - 5.0, 10.0, 10.0, 1.0, 1.0, 1.0, 1.0);
            }
            UI3DScene.releaseVector2f(uiPos);
        }

        Vector2f uiToTile(Vector2f tileXY, float pixelSize, Vector2f uiPos, Vector2f tilePos) {
            float x = (uiPos.x - tileXY.x) / pixelSize;
            float y = (uiPos.y - tileXY.y) / pixelSize;
            return tilePos.set(x, y);
        }

        void rasterize(Rasterize.ICallback consumer) {
            Vector2f tileXY = SceneDepthTexture.calculateTextureTopLeft(this.scene, 0.0f, 0.0f, 0.0f, UI3DScene.allocVector2f());
            float pixelSize = SceneDepthTexture.calculatePixelSize(this.scene);
            this.scene.polygonEditor.setPlane(this.translate, this.rotate, this.plane);
            Vector2f point = UI3DScene.allocVector2f();
            Vector2f uiPos1 = UI3DScene.allocVector2f();
            Vector2f uiPos2 = UI3DScene.allocVector2f();
            Vector2f uiPos3 = UI3DScene.allocVector2f();
            for (int i = 0; i < this.triangles.size(); i += 6) {
                float x0 = this.triangles.get(i);
                float y0 = this.triangles.get(i + 1);
                float x1 = this.triangles.get(i + 2);
                float y1 = this.triangles.get(i + 3);
                float x2 = this.triangles.get(i + 4);
                float y2 = this.triangles.get(i + 5);
                this.scene.polygonEditor.planeToUI(point.set(x0, y0), uiPos1);
                this.scene.polygonEditor.planeToUI(point.set(x1, y1), uiPos2);
                this.scene.polygonEditor.planeToUI(point.set(x2, y2), uiPos3);
                this.uiToTile(tileXY, pixelSize, uiPos1, uiPos1);
                this.uiToTile(tileXY, pixelSize, uiPos2, uiPos2);
                this.uiToTile(tileXY, pixelSize, uiPos3, uiPos3);
                s_rasterize.scanTriangle(uiPos1.x, uiPos1.y, uiPos2.x, uiPos2.y, uiPos3.x, uiPos3.y, -1000, 1000, consumer);
            }
            UI3DScene.releaseVector2f(point);
            UI3DScene.releaseVector2f(uiPos1);
            UI3DScene.releaseVector2f(uiPos2);
            UI3DScene.releaseVector2f(uiPos3);
            UI3DScene.releaseVector2f(tileXY);
        }
    }

    public static final class Ray {
        public final Vector3f origin = new Vector3f();
        public final Vector3f direction = new Vector3f();
        public float t;

        public Ray set(Ray rhs) {
            this.origin.set(rhs.origin);
            this.direction.set(rhs.direction);
            this.t = rhs.t;
            return this;
        }
    }

    public static final class Plane {
        public final Vector3f point = new Vector3f();
        public final Vector3f normal = new Vector3f();

        public Plane() {
        }

        public Plane(Vector3f normal, Vector3f point) {
            this.point.set(point);
            this.normal.set(normal);
        }

        public Plane set(Vector3f normal, Vector3f point) {
            this.point.set(point);
            this.normal.set(normal);
            return this;
        }
    }

    private static abstract class SceneGeometry
    extends SceneObject {
        boolean selected;

        SceneGeometry(UI3DScene scene, String id) {
            super(scene, id);
        }

        public abstract String getTypeName();

        public boolean isBox() {
            return false;
        }

        public SceneBox asBox() {
            return Type.tryCastTo(this, SceneBox.class);
        }

        public boolean isCylinder() {
            return false;
        }

        public SceneCylinder asCylinder() {
            return Type.tryCastTo(this, SceneCylinder.class);
        }

        public boolean isPolygon() {
            return false;
        }

        public ScenePolygon asPolygon() {
            return Type.tryCastTo(this, ScenePolygon.class);
        }

        abstract org.joml.Matrix4f getOriginTransform(String var1, org.joml.Matrix4f var2);

        abstract float getNormalizedDepthAt(float var1, float var2);

        abstract TileGeometryFile.Geometry toGeometryFileObject();
    }

    private static final class SceneCylinder
    extends SceneGeometry {
        float radius;
        float height;

        SceneCylinder(UI3DScene scene, String id) {
            super(scene, id);
        }

        @Override
        public String getTypeName() {
            return "cylinder";
        }

        @Override
        void initClone(SceneObject clone) {
            SceneCylinder cylinder = (SceneCylinder)clone;
            super.initClone(clone);
            cylinder.radius = this.radius;
            cylinder.height = this.height;
        }

        @Override
        SceneObject clone(String id) {
            SceneCylinder clone = new SceneCylinder(this.scene, id);
            this.initClone(clone);
            return clone;
        }

        @Override
        SceneObjectRenderData renderMain() {
            if (!this.scene.drawGeometry) {
                return null;
            }
            SceneCylinderDrawer drawer = SceneCylinderDrawer.s_pool.alloc();
            drawer.sceneObject = this;
            drawer.radiusBase = this.radius;
            drawer.radiusTop = this.radius;
            drawer.length = this.height;
            SpriteRenderer.instance.drawGeneric(drawer);
            return null;
        }

        @Override
        public boolean isCylinder() {
            return true;
        }

        @Override
        org.joml.Matrix4f getOriginTransform(String hint, org.joml.Matrix4f xfrm) {
            xfrm.identity();
            switch (hint) {
                case "xMin": {
                    xfrm.translation(-this.radius, 0.0f, 0.0f);
                    break;
                }
                case "xMax": {
                    xfrm.translation(this.radius, 0.0f, 0.0f);
                    break;
                }
                case "yMin": {
                    xfrm.translation(0.0f, -this.radius, 0.0f);
                    break;
                }
                case "yMax": {
                    xfrm.translation(0.0f, this.radius, 0.0f);
                    break;
                }
                case "zMin": {
                    xfrm.translation(0.0f, 0.0f, -this.height / 2.0f);
                    break;
                }
                case "zMax": {
                    xfrm.translation(0.0f, 0.0f, this.height / 2.0f);
                }
            }
            return xfrm;
        }

        @Override
        float getNormalizedDepthAt(float tileX, float tileY) {
            return TileGeometryUtils.getNormalizedDepthOnCylinderAt(tileX, tileY, this.translate, this.rotate, this.radius, this.height);
        }

        @Override
        TileGeometryFile.Geometry toGeometryFileObject() {
            TileGeometryFile.Cylinder cylinder = new TileGeometryFile.Cylinder();
            cylinder.translate.set(this.translate);
            cylinder.rotate.set(this.rotate);
            cylinder.radius1 = this.radius;
            cylinder.radius2 = this.radius;
            cylinder.height = this.height;
            return cylinder;
        }

        boolean intersect(Ray rayIn, CylinderUtils.IntersectionRecord outRecord) {
            return CylinderUtils.intersect(this.radius, this.height, rayIn, outRecord);
        }

        AABB getAABB(AABB aabb) {
            org.joml.Matrix4f m = UI3DScene.allocMatrix4f().rotationXYZ(this.rotate.x * ((float)Math.PI / 180), this.rotate.y * ((float)Math.PI / 180), this.rotate.z * ((float)Math.PI / 180));
            Vector3f zAxis = m.transformDirection(UI3DScene.allocVector3f().set(0.0f, 0.0f, 1.0f)).normalize();
            UI3DScene.releaseMatrix4f(m);
            Vector3f pa = UI3DScene.allocVector3f().set(zAxis).mul(-this.height / 2.0f).add(this.translate);
            Vector3f pb = UI3DScene.allocVector3f().set(zAxis).mul(this.height / 2.0f).add(this.translate);
            UI3DScene.releaseVector3f(zAxis);
            Vector3f a = UI3DScene.allocVector3f().set(pb).sub(pa);
            Vector3f axa = UI3DScene.allocVector3f().set(a).mul(a);
            axa.div(a.dot(a));
            UI3DScene.releaseVector3f(a);
            Vector3f e = UI3DScene.allocVector3f().set((double)this.radius * Math.sqrt(1.0 - (double)axa.x), (double)this.radius * Math.sqrt(1.0 - (double)axa.y), (double)this.radius * Math.sqrt(1.0 - (double)axa.z));
            UI3DScene.releaseVector3f(axa);
            Vector3f paMe = UI3DScene.allocVector3f().set(pa).sub(e);
            Vector3f pbMe = UI3DScene.allocVector3f().set(pb).sub(e);
            Vector3f paPe = UI3DScene.allocVector3f().set(pa).add(e);
            Vector3f pbPe = UI3DScene.allocVector3f().set(pb).add(e);
            UI3DScene.releaseVector3f(pa);
            UI3DScene.releaseVector3f(pb);
            UI3DScene.releaseVector3f(e);
            Vector3f min = UI3DScene.allocVector3f().set(paMe).min(pbMe);
            Vector3f max = UI3DScene.allocVector3f().set(paPe).max(pbPe);
            UI3DScene.releaseVector3f(paMe);
            UI3DScene.releaseVector3f(pbMe);
            UI3DScene.releaseVector3f(paPe);
            UI3DScene.releaseVector3f(pbPe);
            aabb.set(this.translate.x, this.translate.y, this.translate.z, max.x - min.x, max.y - min.y, max.z - min.z, 1.0f, 1.0f, 1.0f, 1.0f, false);
            UI3DScene.releaseVector3f(min);
            UI3DScene.releaseVector3f(max);
            return aabb;
        }
    }

    private static final class AABB {
        float x;
        float y;
        float z;
        float w;
        float h;
        float l;
        float r;
        float g;
        float b;
        float a;
        boolean quads;

        private AABB() {
        }

        AABB set(AABB rhs) {
            return this.set(rhs.x, rhs.y, rhs.z, rhs.w, rhs.h, rhs.l, rhs.r, rhs.g, rhs.b, rhs.a, rhs.quads);
        }

        AABB set(float x, float y, float z, float w, float h, float l, float r, float g, float b, float a, boolean bQuads) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.w = w;
            this.h = h;
            this.l = l;
            this.r = r;
            this.g = g;
            this.b = b;
            this.a = a;
            this.quads = bQuads;
            return this;
        }
    }

    private static final class ScenePlayer
    extends SceneCharacter {
        ScenePlayer(UI3DScene scene, String id) {
            super(scene, id);
        }

        @Override
        void initAnimatedModel() {
            this.animatedModel.setAnimSetName("player-vehicle");
            this.animatedModel.setState("idle");
            this.animatedModel.setOutfitName("Naked", false, false);
            this.animatedModel.setVisual(new HumanVisual(this.animatedModel));
            this.animatedModel.getHumanVisual().setHairModel("Bald");
            this.animatedModel.getHumanVisual().setBeardModel("");
            this.animatedModel.getHumanVisual().setSkinTextureIndex(0);
            this.animatedModel.setAlpha(0.5f);
            this.animatedModel.setAnimate(false);
        }
    }

    private static final class SceneBox
    extends SceneGeometry {
        final Vector3f min = new Vector3f(-0.5f, 0.0f, -0.5f);
        final Vector3f max = new Vector3f(0.5f, 2.44949f, 0.5f);

        SceneBox(UI3DScene scene, String id) {
            super(scene, id);
        }

        @Override
        public String getTypeName() {
            return "box";
        }

        @Override
        void initClone(SceneObject clone) {
            SceneBox box = (SceneBox)clone;
            super.initClone(clone);
            box.min.set(this.min);
            box.max.set(this.max);
        }

        @Override
        SceneObject clone(String id) {
            SceneBox clone = new SceneBox(this.scene, id);
            this.initClone(clone);
            return clone;
        }

        @Override
        SceneObjectRenderData renderMain() {
            if (!this.scene.drawGeometry) {
                return null;
            }
            Box3D box3D = s_box3DPool.alloc();
            box3D.x = this.translate.x;
            box3D.y = this.translate.y;
            box3D.z = this.translate.z;
            box3D.rx = this.rotate.x;
            box3D.ry = this.rotate.y;
            box3D.rz = this.rotate.z;
            box3D.xMin = this.min.x;
            box3D.yMin = this.min.y;
            box3D.zMin = this.min.z;
            box3D.xMax = this.max.x;
            box3D.yMax = this.max.y;
            box3D.zMax = this.max.z;
            box3D.a = 1.0f;
            box3D.b = 1.0f;
            box3D.g = 1.0f;
            box3D.r = 1.0f;
            box3D.quads = false;
            if (this.selected) {
                box3D.b = 0.0f;
            }
            this.scene.box3d.add(box3D);
            return null;
        }

        @Override
        public boolean isBox() {
            return true;
        }

        @Override
        org.joml.Matrix4f getOriginTransform(String hint, org.joml.Matrix4f xfrm) {
            xfrm.identity();
            switch (hint) {
                case "xMin": {
                    xfrm.translation(this.min.x(), 0.0f, 0.0f);
                    break;
                }
                case "xMax": {
                    xfrm.translation(this.max.x(), 0.0f, 0.0f);
                    break;
                }
                case "yMin": {
                    xfrm.translation(0.0f, this.min.y(), 0.0f);
                    break;
                }
                case "yMax": {
                    xfrm.translation(0.0f, this.max.y(), 0.0f);
                    break;
                }
                case "zMin": {
                    xfrm.translation(0.0f, 0.0f, this.min.z());
                    break;
                }
                case "zMax": {
                    xfrm.translation(0.0f, 0.0f, this.max.z());
                }
            }
            return xfrm;
        }

        @Override
        float getNormalizedDepthAt(float tileX, float tileY) {
            return TileGeometryUtils.getNormalizedDepthOnBoxAt(tileX, tileY, this.translate, this.rotate, this.min, this.max);
        }

        @Override
        TileGeometryFile.Geometry toGeometryFileObject() {
            TileGeometryFile.Box tileBox = new TileGeometryFile.Box();
            tileBox.translate.set(this.translate);
            tileBox.rotate.set(this.rotate);
            tileBox.min.set(this.min);
            tileBox.max.set(this.max);
            return tileBox;
        }
    }

    private static final class ParentVehiclePart {
        SceneVehicle vehicle;
        String partId;
        String partModelId;
        String attachmentName;

        private ParentVehiclePart() {
        }

        org.joml.Matrix4f getGlobalTransform(org.joml.Matrix4f transform) {
            this.vehicle.getTransformForPart(this.partId, this.partModelId, this.attachmentName, false, transform);
            VehicleRenderData vrd = VehicleRenderData.s_pool.alloc();
            vrd.initVehicle(this.vehicle);
            VehicleModelRenderData parentVMRD = vrd.partToRenderData.get(this.partId);
            if (parentVMRD != null) {
                parentVMRD.xfrm.mul(transform, transform);
            }
            vrd.release();
            return transform;
        }

        VehicleScript.Part getScriptPart() {
            if (this.vehicle == null || this.vehicle.script == null || this.partId == null) {
                return null;
            }
            return this.vehicle.script.getPartById(this.partId);
        }

        VehicleScript.Model getScriptModel() {
            VehicleScript.Part scriptPart = this.getScriptPart();
            if (scriptPart == null || this.partModelId == null) {
                return null;
            }
            return scriptPart.getModelById(this.partModelId);
        }
    }

    private static final class PositionRotation {
        final Vector3f pos = new Vector3f();
        final Vector3f rot = new Vector3f();
        boolean relativeToOrigin;

        private PositionRotation() {
        }

        PositionRotation set(PositionRotation rhs) {
            this.pos.set(rhs.pos);
            this.rot.set(rhs.rot);
            this.relativeToOrigin = rhs.relativeToOrigin;
            return this;
        }

        PositionRotation set(float x, float y, float z) {
            this.pos.set(x, y, z);
            this.rot.set(0.0f, 0.0f, 0.0f);
            this.relativeToOrigin = false;
            return this;
        }

        PositionRotation set(float x, float y, float z, float rx, float ry, float rz) {
            this.pos.set(x, y, z);
            this.rot.set(rx, ry, rz);
            this.relativeToOrigin = false;
            return this;
        }
    }

    private static final class SceneDepthTexture
    extends SceneObject {
        Texture texture;

        SceneDepthTexture(UI3DScene scene, String id) {
            super(scene, id);
        }

        @Override
        SceneObjectRenderData renderMain() {
            IndieGL.enableDepthTest();
            IndieGL.glDepthMask(true);
            IndieGL.glDepthFunc(519);
            if (!this.scene.drawGeometry) {
                IndieGL.glColorMask(false, false, false, false);
            }
            this.renderTexture(this.translate.x, this.translate.y, this.translate.z);
            if (!this.scene.drawGeometry) {
                IndieGL.glColorMask(true, true, true, true);
            }
            IndieGL.disableDepthTest();
            IndieGL.glDepthMask(false);
            return null;
        }

        static float calculatePixelSize(UI3DScene scene) {
            float sx = scene.sceneToUIX(0.0f, 0.0f, 0.0f);
            float sy = scene.sceneToUIY(0.0f, 0.0f, 0.0f);
            float sx2 = scene.sceneToUIX(1.0f, 0.0f, 0.0f);
            float sy2 = scene.sceneToUIY(1.0f, 0.0f, 0.0f);
            return (float)(Math.sqrt((sx2 - sx) * (sx2 - sx) + (sy2 - sy) * (sy2 - sy)) / Math.sqrt(5120.0));
        }

        static Vector2f calculateTextureTopLeft(UI3DScene scene, float sceneX, float sceneY, float sceneZ, Vector2f topLeft) {
            float sx = scene.sceneToUIX(sceneX, sceneY, sceneZ);
            float sy = scene.sceneToUIY(sceneX, sceneY, sceneZ);
            float pixelSize = SceneDepthTexture.calculatePixelSize(scene);
            float tileX = sx - 64.0f * pixelSize;
            float tileY = sy - 224.0f * pixelSize;
            boolean textureOffsetX = true;
            return topLeft.set(tileX += 1.0f * pixelSize, tileY);
        }

        void renderTexture(float x, float y, float z) {
            Texture tex;
            org.joml.Matrix4f mvp = UI3DScene.allocMatrix4f();
            mvp.set(this.scene.projection);
            mvp.mul(this.scene.modelView);
            Vector3f v = UI3DScene.allocVector3f();
            float frontDepthZ = mvp.transformPosition((Vector3f)v.set((float)(x + 1.5f), (float)(y + 0.0f), (float)(z + 1.5f))).z;
            float farDepthZ = mvp.transformPosition((Vector3f)v.set((float)(x - 0.5f), (float)(y + 0.0f), (float)(z - 0.5f))).z;
            frontDepthZ = (frontDepthZ + 1.0f) / 2.0f;
            farDepthZ = (farDepthZ + 1.0f) / 2.0f;
            UI3DScene.releaseMatrix4f(mvp);
            UI3DScene.releaseVector3f(v);
            this.texture.getTextureId().setMagFilter(9728);
            IndieGL.StartShader(SceneShaderStore.tileDepthShader.getID());
            IndieGL.shaderSetValue((Shader)SceneShaderStore.tileDepthShader, "zDepth", frontDepthZ);
            boolean drawPixels = false;
            IndieGL.shaderSetValue((Shader)SceneShaderStore.tileDepthShader, "drawPixels", 0);
            IndieGL.shaderSetValue((Shader)SceneShaderStore.tileDepthShader, "zDepthBlendZ", frontDepthZ);
            IndieGL.shaderSetValue((Shader)SceneShaderStore.tileDepthShader, "zDepthBlendToZ", farDepthZ);
            float pixelSize = SceneDepthTexture.calculatePixelSize(this.scene);
            Vector2f topLeft = SceneDepthTexture.calculateTextureTopLeft(this.scene, x, y, z, UI3DScene.allocVector2f());
            SpriteRenderer.instance.render(this.texture, topLeft.x + this.texture.getOffsetX() * pixelSize, topLeft.y + this.texture.getOffsetY() * pixelSize, (float)this.texture.getWidth() * pixelSize, (float)this.texture.getHeight() * pixelSize, 1.0f, 1.0f, 1.0f, 0.5f, null);
            UI3DScene.releaseVector2f(topLeft);
            int numSprites = SpriteRenderer.instance.states.getPopulatingActiveState().numSprites;
            TextureDraw textureDraw = SpriteRenderer.instance.states.getPopulatingActiveState().sprite[numSprites - 1];
            textureDraw.tex1 = tex = this.texture;
            textureDraw.tex1U0 = tex.getXStart();
            textureDraw.tex1U1 = tex.getXEnd();
            textureDraw.tex1U2 = tex.getXEnd();
            textureDraw.tex1U3 = tex.getXStart();
            textureDraw.tex1V0 = tex.getYStart();
            textureDraw.tex1V1 = tex.getYStart();
            textureDraw.tex1V2 = tex.getYEnd();
            textureDraw.tex1V3 = tex.getYEnd();
            IndieGL.EndShader();
        }
    }

    private static final class SceneAnimal
    extends SceneCharacter
    implements IAnimalVisual {
        AnimalVisual visual;
        final ItemVisuals itemVisuals = new ItemVisuals();
        AnimalDefinitions definition;
        AnimalBreed breed;

        SceneAnimal(UI3DScene scene, String id, AnimalDefinitions definition, AnimalBreed breed) {
            super(scene, id);
            this.definition = definition;
            this.breed = breed;
        }

        void setAnimalDefinition(AnimalDefinitions definition, AnimalBreed breed) {
            this.definition = definition;
            this.breed = breed;
            if (this.isFemale()) {
                this.visual.setSkinTextureName(PZArrayUtil.pickRandom(this.breed.texture));
            } else {
                this.visual.setSkinTextureName(this.breed.textureMale);
            }
            if (!this.animatedModel.GetAnimSetName().endsWith("-editor")) {
                this.animatedModel.setAnimSetName(this.definition.animset);
            }
            this.animatedModel.setModelData(this.visual, this.itemVisuals);
        }

        @Override
        void initAnimatedModel() {
            this.visual = new AnimalVisual(this);
            if (this.isFemale()) {
                this.visual.setSkinTextureName(PZArrayUtil.pickRandom(this.breed.texture));
            } else {
                this.visual.setSkinTextureName(this.breed.textureMale);
            }
            this.animatedModel.setAnimSetName(this.definition.animset);
            this.animatedModel.setState("idle");
            this.animatedModel.setModelData(this.visual, this.itemVisuals);
            this.animatedModel.setAlpha(0.5f);
            this.animatedModel.setAnimate(false);
        }

        @Override
        public AnimalVisual getAnimalVisual() {
            return this.visual;
        }

        @Override
        public String getAnimalType() {
            return this.definition.getAnimalType();
        }

        @Override
        public float getAnimalSize() {
            return 1.0f;
        }

        @Override
        public HumanVisual getHumanVisual() {
            return null;
        }

        @Override
        public void getItemVisuals(ItemVisuals itemVisuals) {
            itemVisuals.clear();
            itemVisuals.addAll(this.itemVisuals);
        }

        @Override
        public boolean isFemale() {
            return this.definition.female;
        }

        @Override
        public boolean isZombie() {
            return false;
        }

        @Override
        public boolean isSkeleton() {
            return false;
        }
    }

    private static final class Box3D {
        float x;
        float y;
        float z;
        float xMin;
        float yMin;
        float zMin;
        float xMax;
        float yMax;
        float zMax;
        float rx;
        float ry;
        float rz;
        float r;
        float g;
        float b;
        float a;
        boolean quads;

        private Box3D() {
        }

        Box3D set(Box3D rhs) {
            return this.set(rhs.x, rhs.y, rhs.z, rhs.xMin, rhs.yMin, rhs.zMin, rhs.xMax, rhs.yMax, rhs.zMax, rhs.rx, rhs.ry, rhs.rz, rhs.r, rhs.g, rhs.b, rhs.a, rhs.quads);
        }

        Box3D set(float x, float y, float z, float xMin, float yMin, float zMin, float xMax, float yMax, float zMax, float rx, float ry, float rz, float r, float g, float b, float a, boolean bQuads) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.xMin = xMin;
            this.yMin = yMin;
            this.zMin = zMin;
            this.xMax = xMax;
            this.yMax = yMax;
            this.zMax = zMax;
            this.rx = rx;
            this.ry = ry;
            this.rz = rz;
            this.r = r;
            this.g = g;
            this.b = b;
            this.a = a;
            this.quads = bQuads;
            return this;
        }
    }

    private static final class SpriteGridTextureMaskDrawer
    extends TextureDraw.GenericDrawer {
        UI3DScene scene;
        IsoSprite sprite;
        float sx;
        float sy;
        float sx2;
        float sy2;
        float pixelSize;
        float r;
        float g;
        float b;
        float a;

        private SpriteGridTextureMaskDrawer() {
        }

        @Override
        public void render() {
            IsoSpriteGrid spriteGrid = this.sprite.getSpriteGrid();
            int spriteGridIndex = spriteGrid.getSpriteIndex(this.sprite);
            int gridPosX = spriteGrid.getSpriteGridPosX(this.sprite);
            int gridPosY = spriteGrid.getSpriteGridPosY(this.sprite);
            int gridPosZ = spriteGrid.getSpriteGridPosZ(this.sprite);
            for (int i = 0; i < spriteGrid.getSpriteCount(); ++i) {
                Texture texture;
                IsoSprite sprite2;
                if (i == spriteGridIndex || (sprite2 = spriteGrid.getSpriteFromIndex(i)) == null || (texture = sprite2.getTextureForCurrentFrame(IsoDirections.N)) == null || texture.getMask() == null) continue;
                int gridPosX2 = spriteGrid.getSpriteGridPosX(sprite2);
                int gridPosY2 = spriteGrid.getSpriteGridPosY(sprite2);
                int gridPosZ2 = spriteGrid.getSpriteGridPosZ(sprite2);
                int dx = gridPosX2 - gridPosX;
                int dy = gridPosY2 - gridPosY;
                int dz = gridPosZ2 - gridPosZ;
                this.render(texture, this.scene.sceneToUIX((float)dx + 0.5f, (float)(dz * 3) * 0.8164967f, (float)dy + 0.5f) - 64.0f * this.pixelSize, this.scene.sceneToUIY((float)dx + 0.5f, (float)(dz * 3) * 0.8164967f, (float)dy + 0.5f) - 256.0f * this.pixelSize);
            }
        }

        void render(Texture texture, float sx, float sy) {
            Mask mask = texture.getMask();
            if (mask == null) {
                return;
            }
            VBORenderer vbor = VBORenderer.getInstance();
            vbor.startRun(VBORenderer.getInstance().formatPositionColor);
            float z = 0.0f;
            for (int y = 0; y < 256; ++y) {
                for (int x = 0; x < 128; ++x) {
                    if (!texture.isMaskSet(x, y)) continue;
                    float x1 = sx + (float)x * this.pixelSize;
                    float y1 = sy + (float)y * this.pixelSize;
                    float x2 = sx + (float)(x + 1) * this.pixelSize;
                    float y2 = sy + (float)(y + 1) * this.pixelSize;
                    vbor.addLine(x1, y1, 0.0f, x2, y1, 0.0f, this.r, this.g, this.b, this.a);
                    vbor.addLine(x2, y1, 0.0f, x2, y2, 0.0f, this.r, this.g, this.b, this.a);
                    vbor.addLine(x2, y2, 0.0f, x1, y2, 0.0f, this.r, this.g, this.b, this.a);
                    vbor.addLine(x1, y2, 0.0f, x1, y1, 0.0f, this.r, this.g, this.b, this.a);
                }
            }
            vbor.endRun();
            vbor.flush();
        }
    }

    private static final class TextureMaskDrawer
    extends TextureDraw.GenericDrawer {
        UI3DScene scene;
        Texture texture;
        float sx;
        float sy;
        float sx2;
        float sy2;
        float pixelSize;
        float r;
        float g;
        float b;
        float a;

        private TextureMaskDrawer() {
        }

        @Override
        public void render() {
            Mask mask = this.texture.getMask();
            if (mask == null) {
                return;
            }
            VBORenderer vbor = VBORenderer.getInstance();
            vbor.startRun(VBORenderer.getInstance().formatPositionColor);
            float z = 0.0f;
            for (int y = 0; y < 256; ++y) {
                for (int x = 0; x < 128; ++x) {
                    if (!this.texture.isMaskSet(x, y)) continue;
                    float x1 = this.sx + (float)x * this.pixelSize;
                    float y1 = this.sy + (float)y * this.pixelSize;
                    float x2 = this.sx + (float)(x + 1) * this.pixelSize;
                    float y2 = this.sy + (float)(y + 1) * this.pixelSize;
                    vbor.addLine(x1, y1, 0.0f, x2, y1, 0.0f, this.r, this.g, this.b, this.a);
                    vbor.addLine(x2, y1, 0.0f, x2, y2, 0.0f, this.r, this.g, this.b, this.a);
                    vbor.addLine(x2, y2, 0.0f, x1, y2, 0.0f, this.r, this.g, this.b, this.a);
                    vbor.addLine(x1, y2, 0.0f, x1, y1, 0.0f, this.r, this.g, this.b, this.a);
                }
            }
            vbor.endRun();
            vbor.flush();
        }
    }

    public static final class PhysicsMesh {
        float x;
        float y;
        float z;
        float rx;
        float ry;
        float rz;
        float r;
        float g;
        float b;
        String physicsShapeScript;
        float scale;
        float[] points;

        PhysicsMesh set(Vector3f translate, Vector3f rotate, float scale, String physicsShapeScript, float r, float g, float b) {
            this.x = translate.x;
            this.y = translate.y;
            this.z = translate.z;
            this.rx = rotate.x;
            this.ry = rotate.y;
            this.rz = rotate.z;
            this.scale = scale;
            this.physicsShapeScript = physicsShapeScript;
            this.r = r;
            this.g = g;
            this.b = b;
            this.points = null;
            return this;
        }

        PhysicsMesh set(PhysicsMesh rhs) {
            this.x = rhs.x;
            this.y = rhs.y;
            this.z = rhs.z;
            this.rx = rhs.rx;
            this.ry = rhs.ry;
            this.rz = rhs.rz;
            this.scale = rhs.scale;
            this.physicsShapeScript = rhs.physicsShapeScript;
            this.r = rhs.r;
            this.g = rhs.g;
            this.b = rhs.b;
            this.points = rhs.points;
            return this;
        }
    }

    public static final class Circle {
        public final Vector3f center = new Vector3f();
        public final Vector3f orientation = new Vector3f();
        public float radius = 1.0f;
    }

    private static final class VehicleDrawer
    extends TextureDraw.GenericDrawer {
        SceneVehicle vehicle;
        VehicleRenderData renderData;
        boolean rendered;
        final float[] fzeroes = new float[16];
        final Vector3f paintColor = new Vector3f(0.0f, 0.5f, 0.5f);
        static final org.joml.Matrix4f IDENTITY = new org.joml.Matrix4f();

        private VehicleDrawer() {
        }

        public void init(SceneVehicle sceneVehicle, VehicleRenderData renderData) {
            this.vehicle = sceneVehicle;
            this.renderData = renderData;
            this.rendered = false;
        }

        @Override
        public void render() {
            int i;
            for (i = 0; i < this.renderData.models.size(); ++i) {
                GL11.glPushAttrib(1048575);
                GL11.glPushClientAttrib(-1);
                this.render(i);
                GL11.glPopAttrib();
                GL11.glPopClientAttrib();
                Texture.lastTextureID = -1;
                SpriteRenderer.ringBuffer.restoreBoundTextures = true;
                SpriteRenderer.ringBuffer.restoreVbos = true;
            }
            GL11.glPushAttrib(1048575);
            GL11.glPushClientAttrib(-1);
            for (i = 0; i < this.renderData.models.size(); ++i) {
                VehicleModelRenderData modelRenderData = this.renderData.models.get(i);
                this.renderData.transform.set(modelRenderData.xfrm);
                ModelCamera.instance.Begin();
                this.renderSkeleton(modelRenderData);
                ModelCamera.instance.End();
            }
            GL11.glPopAttrib();
            GL11.glPopClientAttrib();
        }

        private void render(int modelIndex) {
            VehicleModelRenderData modelRenderData = this.renderData.models.get(modelIndex);
            this.renderData.transform.set(modelRenderData.xfrm);
            ModelCamera.instance.Begin();
            Model model = modelRenderData.model;
            boolean bStatic = model.isStatic;
            if (Core.debug && DebugOptions.instance.model.render.wireframe.getValue()) {
                GL11.glPolygonMode(1032, 6913);
                GL11.glEnable(2848);
                GL11.glLineWidth(0.75f);
                zombie.core.skinnedmodel.shader.Shader effect = ShaderManager.instance.getOrCreateShader("vehicle_wireframe", bStatic, false);
                if (effect != null) {
                    effect.Start();
                    if (model.isStatic) {
                        effect.setTransformMatrix(IDENTITY.identity(), false);
                    } else {
                        effect.setMatrixPalette(modelRenderData.matrixPalette, true);
                    }
                    model.mesh.Draw(effect);
                    effect.End();
                }
                GL11.glDisable(2848);
                ModelCamera.instance.End();
                return;
            }
            zombie.core.skinnedmodel.shader.Shader effect = model.effect;
            if (effect != null && effect.isVehicleShader()) {
                GL11.glDepthFunc(513);
                GL11.glDepthMask(true);
                GL11.glDepthRange(0.0, 1.0);
                GL11.glEnable(2929);
                GL11.glColor3f(1.0f, 1.0f, 1.0f);
                effect.Start();
                Texture tex = modelRenderData.tex;
                if (tex == null && modelIndex > 0) {
                    tex = this.renderData.models.get((int)0).tex;
                }
                if (tex != null) {
                    effect.setTexture(tex, "Texture0", 0);
                    GL11.glTexEnvi(8960, 8704, 7681);
                    if (this.vehicle.script.getSkinCount() > 0 && this.vehicle.script.getSkin((int)0).textureMask != null) {
                        Texture textureMask = Texture.getSharedTexture("media/textures/" + this.vehicle.script.getSkin((int)0).textureMask + ".png");
                        effect.setTexture(textureMask, "TextureMask", 2);
                        GL11.glTexEnvi(8960, 8704, 7681);
                    }
                }
                effect.setDepthBias(0.0f);
                effect.setAmbient(1.0f);
                effect.setLightingAmount(1.0f);
                effect.setHueShift(0.0f);
                effect.setTint(1.0f, 1.0f, 1.0f);
                effect.setAlpha(1.0f);
                for (int i = 0; i < 5; ++i) {
                    effect.setLight(i, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, Float.NaN, 0.0f, 0.0f, 0.0f, null);
                }
                effect.setTextureUninstall1(this.fzeroes);
                effect.setTextureUninstall2(this.fzeroes);
                effect.setTextureLightsEnables2(this.fzeroes);
                effect.setTextureDamage1Enables1(this.fzeroes);
                effect.setTextureDamage1Enables2(this.fzeroes);
                effect.setTextureDamage2Enables1(this.fzeroes);
                effect.setTextureDamage2Enables2(this.fzeroes);
                effect.setMatrixBlood1(this.fzeroes, this.fzeroes);
                effect.setMatrixBlood2(this.fzeroes, this.fzeroes);
                effect.setTextureRustA(0.0f);
                effect.setTexturePainColor(this.paintColor, 1.0f);
                if (model.isStatic) {
                    effect.setTransformMatrix(IDENTITY.identity(), false);
                } else {
                    effect.setMatrixPalette(modelRenderData.matrixPalette, true);
                }
                effect.setTargetDepth(0.5f);
                model.mesh.Draw(effect);
                effect.End();
            } else if (effect != null && model.mesh != null && model.mesh.isReady()) {
                GL11.glDepthFunc(513);
                GL11.glDepthMask(true);
                GL11.glDepthRange(0.0, 1.0);
                GL11.glEnable(2929);
                GL11.glColor3f(1.0f, 1.0f, 1.0f);
                effect.Start();
                if (model.tex != null) {
                    effect.setTexture(model.tex, "Texture", 0);
                }
                effect.setDepthBias(0.0f);
                effect.setAmbient(1.0f);
                effect.setLightingAmount(1.0f);
                effect.setHueShift(0.0f);
                effect.setTint(1.0f, 1.0f, 1.0f);
                effect.setAlpha(1.0f);
                for (int i = 0; i < 5; ++i) {
                    effect.setLight(i, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, Float.NaN, 0.0f, 0.0f, 0.0f, null);
                }
                effect.setTransformMatrix(IDENTITY.identity(), false);
                effect.setTargetDepth(0.5f);
                model.mesh.Draw(effect);
                effect.End();
            }
            ModelCamera.instance.End();
            this.rendered = true;
        }

        private void renderSkeleton(VehicleModelRenderData renderData) {
            TFloatArrayList boneCoords = renderData.boneCoords;
            if (boneCoords.isEmpty()) {
                return;
            }
            VBORenderer vbor = VBORenderer.getInstance();
            vbor.startRun(vbor.formatPositionColor);
            vbor.setDepthTest(false);
            vbor.setLineWidth(1.0f);
            vbor.setMode(1);
            for (int i = 0; i < boneCoords.size(); i += 6) {
                Color c = Model.debugDrawColours[i % Model.debugDrawColours.length];
                vbor.addElement();
                vbor.setColor(c.r, c.g, c.b, 1.0f);
                float x = boneCoords.get(i);
                float y = boneCoords.get(i + 1);
                float z = boneCoords.get(i + 2);
                vbor.setVertex(x, y, z);
                vbor.addElement();
                vbor.setColor(c.r, c.g, c.b, 1.0f);
                x = boneCoords.get(i + 3);
                y = boneCoords.get(i + 4);
                z = boneCoords.get(i + 5);
                vbor.setVertex(x, y, z);
            }
            vbor.endRun();
            vbor.flush();
            GL11.glColor3f(1.0f, 1.0f, 1.0f);
            GL11.glEnable(2929);
        }

        @Override
        public void postRender() {
        }
    }

    private static final class ModelDrawer
    extends TextureDraw.GenericDrawer {
        SceneModel model;
        ModelRenderData renderData;
        boolean rendered;
        FloatBuffer matrixPalette;
        TFloatArrayList boneCoords;
        Texture texture;

        private ModelDrawer() {
        }

        public void init(SceneModel model, ModelRenderData renderData) {
            SpriteModel spriteModel;
            this.model = model;
            this.renderData = renderData;
            this.rendered = false;
            this.matrixPalette = null;
            this.boneCoords = null;
            this.texture = model.texture;
            if (!model.modelScript.isStatic && model.spriteModel != null && model.spriteModel.getAnimationName() != null) {
                this.matrixPalette = IsoObjectAnimations.getInstance().getMatrixPaletteForFrame(model.model, model.spriteModel.getAnimationName(), model.spriteModel.getAnimationTime());
                if (this.matrixPalette != null) {
                    this.matrixPalette.position(0);
                }
                this.boneCoords = IsoObjectAnimations.getInstance().getBonesForFrame(model.model, model.spriteModel.getAnimationName(), model.spriteModel.getAnimationTime());
            }
            if (!model.modelScript.isStatic && model.spriteModel == null && (spriteModel = ScriptManager.instance.getSpriteModel(model.modelScript.name)) != null && spriteModel.getAnimationName() != null) {
                this.matrixPalette = IsoObjectAnimations.getInstance().getMatrixPaletteForFrame(model.model, spriteModel.getAnimationName(), spriteModel.getAnimationTime());
                if (this.matrixPalette != null) {
                    this.matrixPalette.position(0);
                }
                this.boneCoords = IsoObjectAnimations.getInstance().getBonesForFrame(model.model, spriteModel.getAnimationName(), spriteModel.getAnimationTime());
            }
        }

        @Override
        public void render() {
            StateData stateData = this.model.scene.stateDataRender();
            PZGLUtil.pushAndLoadMatrix(5889, stateData.projection);
            PZGLUtil.pushAndLoadMatrix(5888, stateData.modelView);
            Model model = this.model.model;
            zombie.core.skinnedmodel.shader.Shader effect = model.effect;
            if (effect != null && model.mesh != null && model.mesh.isReady()) {
                GL11.glPushAttrib(1048575);
                GL11.glPushClientAttrib(-1);
                UI3DScene scene = this.renderData.object.scene;
                GL11.glViewport(scene.getAbsoluteX().intValue(), Core.getInstance().getScreenHeight() - scene.getAbsoluteY().intValue() - scene.getHeight().intValue(), scene.getWidth().intValue(), scene.getHeight().intValue());
                GL11.glDepthFunc(513);
                GL11.glDepthMask(true);
                GL11.glDepthRange(0.0, 1.0);
                GL11.glEnable(2929);
                ModelScript modelScript = this.model.modelScript;
                if (modelScript != null && modelScript.cullFace != -1) {
                    if (modelScript.cullFace == 0) {
                        GL11.glDisable(2884);
                    } else {
                        GL11.glEnable(2884);
                        GL11.glCullFace(modelScript.cullFace);
                    }
                } else {
                    GL11.glDisable(2884);
                }
                GL11.glColor3f(1.0f, 1.0f, 1.0f);
                effect.Start();
                Texture tex = model.tex;
                if (this.texture != null) {
                    tex = this.texture;
                }
                if (tex != null) {
                    effect.setTexture(tex, "Texture", 0);
                    if (effect.getShaderProgram().getName().equalsIgnoreCase("door")) {
                        int widthHW = tex.getWidthHW();
                        int heightHW = tex.getHeightHW();
                        float x1 = tex.xStart * (float)widthHW - tex.offsetX;
                        float y1 = tex.yStart * (float)heightHW - tex.offsetY;
                        float x2 = x1 + (float)tex.getWidthOrig();
                        float y2 = y1 + (float)tex.getHeightOrig();
                        Vector2 tempVector2 = BaseVehicle.allocVector2();
                        effect.getShaderProgram().setValue("UVOffset", tempVector2.set(x1 / (float)widthHW, y1 / (float)heightHW));
                        effect.getShaderProgram().setValue("UVScale", tempVector2.set((x2 - x1) / (float)widthHW, (y2 - y1) / (float)heightHW));
                        BaseVehicle.releaseVector2(tempVector2);
                        GL11.glEnable(2884);
                        GL11.glCullFace(1028);
                    }
                }
                effect.setDepthBias(0.0f);
                effect.setAmbient(1.0f);
                effect.setLightingAmount(1.0f);
                effect.setHueShift(0.0f);
                effect.setTint(1.0f, 1.0f, 1.0f);
                effect.setAlpha(1.0f);
                for (int i = 0; i < 5; ++i) {
                    effect.setLight(i, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, Float.NaN, 0.0f, 0.0f, 0.0f, null);
                }
                if (model.isStatic) {
                    effect.setTransformMatrix(this.renderData.transform, false);
                } else if ("door".equalsIgnoreCase(effect.getShaderProgram().getName())) {
                    if (this.matrixPalette != null) {
                        effect.setMatrixPalette(this.matrixPalette);
                    }
                    PZGLUtil.pushAndMultMatrix(5888, this.renderData.transform);
                    if (this.model.modelScript.meshName.contains("door1")) {
                        // empty if block
                    }
                }
                effect.setTargetDepth(0.5f);
                model.mesh.Draw(effect);
                effect.End();
                if (DebugOptions.instance.model.render.bones.getValue()) {
                    this.renderSkeleton();
                }
                if (!model.isStatic && "door".equalsIgnoreCase(effect.getShaderProgram().getName())) {
                    PZGLUtil.popMatrix(5888);
                }
                if (Core.debug) {
                    // empty if block
                }
                if (DebugOptions.instance.model.render.axis.getValue()) {
                    // empty if block
                }
                if (scene.drawAttachments) {
                    org.joml.Matrix4f modelXfrm = UI3DScene.allocMatrix4f();
                    modelXfrm.set(this.renderData.transform);
                    modelXfrm.mul(model.mesh.transform);
                    modelXfrm.scale(this.model.modelScript.scale);
                    org.joml.Matrix4f attachmentXfrm = UI3DScene.allocMatrix4f();
                    for (int i = 0; i < this.model.modelScript.getAttachmentCount(); ++i) {
                        ModelAttachment attachment = this.model.modelScript.getAttachment(i);
                        ModelInstanceRenderData.makeAttachmentTransform(attachment, attachmentXfrm);
                        modelXfrm.mul(attachmentXfrm, attachmentXfrm);
                        PZGLUtil.pushAndMultMatrix(5888, attachmentXfrm);
                        Model.debugDrawAxis(0.0f, 0.0f, 0.0f, 0.1f, 1.0f);
                        PZGLUtil.popMatrix(5888);
                    }
                    UI3DScene.releaseMatrix4f(modelXfrm);
                    UI3DScene.releaseMatrix4f(attachmentXfrm);
                }
                GL11.glPopAttrib();
                GL11.glPopClientAttrib();
                Texture.lastTextureID = -1;
                SpriteRenderer.ringBuffer.restoreBoundTextures = true;
                SpriteRenderer.ringBuffer.restoreVbos = true;
                GL20.glUseProgram(0);
                ShaderHelper.forgetCurrentlyBound();
            }
            PZGLUtil.popMatrix(5889);
            PZGLUtil.popMatrix(5888);
            this.rendered = true;
        }

        @Override
        public void postRender() {
        }

        private void renderSkeleton() {
            if (this.boneCoords == null) {
                return;
            }
            if (this.boneCoords.isEmpty()) {
                return;
            }
            VBORenderer vbor = VBORenderer.getInstance();
            vbor.flush();
            vbor.startRun(vbor.formatPositionColor);
            vbor.setDepthTest(false);
            vbor.setLineWidth(1.0f);
            vbor.setMode(1);
            for (int i = 0; i < this.boneCoords.size(); i += 6) {
                Color c = Model.debugDrawColours[i % Model.debugDrawColours.length];
                float x1 = this.boneCoords.get(i) / this.model.modelScript.scale;
                float y1 = this.boneCoords.get(i + 1) / this.model.modelScript.scale;
                float z1 = this.boneCoords.get(i + 2) / this.model.modelScript.scale;
                float x2 = this.boneCoords.get(i + 3) / this.model.modelScript.scale;
                float y2 = this.boneCoords.get(i + 4) / this.model.modelScript.scale;
                float z2 = this.boneCoords.get(i + 5) / this.model.modelScript.scale;
                vbor.addLine(x1, y1, z1, x2, y2, z2, c.r, c.g, c.b, 1.0f);
            }
            vbor.endRun();
            vbor.flush();
            GL11.glColor3f(1.0f, 1.0f, 1.0f);
            GL11.glEnable(2929);
        }
    }

    private static final class CharacterDrawer
    extends TextureDraw.GenericDrawer {
        SceneCharacter character;
        CharacterRenderData renderData;
        boolean rendered;

        private CharacterDrawer() {
        }

        public void init(SceneCharacter character, CharacterRenderData renderData) {
            this.character = character;
            this.renderData = renderData;
            this.rendered = false;
            this.character.animatedModel.renderMain();
        }

        @Override
        public void render() {
            if (this.character.clearDepthBuffer) {
                GL11.glClear(256);
            }
            boolean showBones = DebugOptions.instance.model.render.bones.getValue();
            DebugOptions.instance.model.render.bones.setValue(this.character.showBones);
            this.character.scene.characterSceneModelCamera.renderData = this.renderData;
            this.character.animatedModel.setShowBip01(this.character.showBip01);
            this.character.animatedModel.DoRender(this.character.scene.characterSceneModelCamera);
            DebugOptions.instance.model.render.bones.setValue(showBones);
            this.rendered = true;
            GL11.glDepthMask(true);
        }

        @Override
        public void postRender() {
            this.character.animatedModel.postRender(this.rendered);
        }
    }

    private static final class TranslateGizmoRenderData {
        boolean hideX;
        boolean hideY;
        boolean hideZ;

        private TranslateGizmoRenderData() {
        }
    }

    public static final class PlaneObjectPool
    extends ObjectPool<Plane> {
        int allocated;

        public PlaneObjectPool() {
            super(Plane::new);
        }

        @Override
        protected Plane makeObject() {
            ++this.allocated;
            return (Plane)super.makeObject();
        }
    }

    public static final class RayObjectPool
    extends ObjectPool<Ray> {
        int allocated;

        public RayObjectPool() {
            super(Ray::new);
        }

        @Override
        protected Ray makeObject() {
            ++this.allocated;
            return (Ray)super.makeObject();
        }
    }

    private static final class SetModelCamera
    extends TextureDraw.GenericDrawer {
        SceneModelCamera camera;
        SceneObjectRenderData renderData;

        private SetModelCamera() {
        }

        SetModelCamera init(SceneModelCamera camera, SceneObjectRenderData renderData) {
            this.camera = camera;
            this.renderData = renderData;
            return this;
        }

        @Override
        public void render() {
            this.camera.renderData = this.renderData;
            ModelCamera.instance = this.camera;
        }

        @Override
        public void postRender() {
            s_SetModelCameraPool.release(this);
        }
    }

    private abstract class SceneModelCamera
    extends ModelCamera {
        SceneObjectRenderData renderData;

        private SceneModelCamera(UI3DScene uI3DScene) {
            Objects.requireNonNull(uI3DScene);
        }
    }

    private static class VehicleModelRenderData {
        public Model model;
        public Texture tex;
        public final org.joml.Matrix4f xfrm = new org.joml.Matrix4f();
        public FloatBuffer matrixPalette;
        private final TFloatArrayList boneCoords = new TFloatArrayList();
        private final ArrayList<Matrix4f> boneMatrices = new ArrayList();
        private static final ObjectPool<VehicleModelRenderData> s_pool = new ObjectPool<VehicleModelRenderData>(VehicleModelRenderData::new);

        private VehicleModelRenderData() {
        }

        void initSkeleton(SceneVehicleModelInfo modelInfo) {
            this.boneCoords.clear();
            this.initSkeleton(modelInfo.getAnimationPlayer());
        }

        private void initSkeleton(AnimationPlayer animPlayer) {
            if (animPlayer == null || !animPlayer.hasSkinningData() || animPlayer.isBoneTransformsNeedFirstFrame()) {
                return;
            }
            Integer translationBoneIndex = animPlayer.getSkinningData().boneIndices.get("Translation_Data");
            for (int i = 0; i < animPlayer.getModelTransformsCount(); ++i) {
                int parentIdx;
                if (translationBoneIndex != null && i == translationBoneIndex || (parentIdx = animPlayer.getSkinningData().skeletonHierarchy.get(i).intValue()) < 0) continue;
                this.initSkeleton(animPlayer, i);
                this.initSkeleton(animPlayer, parentIdx);
            }
        }

        private void initSkeleton(AnimationPlayer animPlayer, int boneIndex) {
            Matrix4f boneTransform = animPlayer.getModelTransformAt(boneIndex);
            float x = boneTransform.m03;
            float y = boneTransform.m13;
            float z = boneTransform.m23;
            this.boneCoords.add(x);
            this.boneCoords.add(y);
            this.boneCoords.add(z);
        }

        void release() {
            s_pool.release(this);
        }
    }

    private static class VehicleRenderData
    extends SceneObjectRenderData {
        final ArrayList<VehicleModelRenderData> models = new ArrayList();
        final HashMap<String, VehicleModelRenderData> partToRenderData = new HashMap();
        final VehicleDrawer drawer = new VehicleDrawer();
        private static final ObjectPool<VehicleRenderData> s_pool = new ObjectPool<VehicleRenderData>(VehicleRenderData::new);

        private VehicleRenderData() {
        }

        SceneObjectRenderData initVehicle(SceneVehicle sceneVehicle) {
            this.init(sceneVehicle);
            VehicleModelRenderData.s_pool.release((List<VehicleModelRenderData>)this.models);
            this.models.clear();
            VehicleScript script = sceneVehicle.script;
            if (script.getModel() == null) {
                return null;
            }
            this.initVehicleModel(sceneVehicle);
            float scale = script.getModelScale();
            Vector3f modelOffset = script.getModel().getOffset();
            boolean rightToLeftHand = true;
            org.joml.Matrix4f vehicleTransform = UI3DScene.allocMatrix4f();
            vehicleTransform.translationRotateScale(modelOffset.x * 1.0f, modelOffset.y, modelOffset.z, 0.0f, 0.0f, 0.0f, 1.0f, scale);
            this.transform.mul(vehicleTransform, vehicleTransform);
            for (int i = 0; i < script.getPartCount(); ++i) {
                VehicleScript.Part scriptPart = script.getPart(i);
                if (scriptPart.wheel == null) {
                    this.initPartModels(sceneVehicle, scriptPart, vehicleTransform);
                    continue;
                }
                this.initWheelModel(sceneVehicle, scriptPart, vehicleTransform);
            }
            UI3DScene.releaseMatrix4f(vehicleTransform);
            this.drawer.init(sceneVehicle, this);
            return this;
        }

        private void initVehicleModel(SceneVehicle sceneVehicle) {
            SceneVehicleModelInfo modelInfo = sceneVehicle.modelInfo.get(0);
            VehicleModelRenderData modelRenderData = VehicleModelRenderData.s_pool.alloc();
            modelRenderData.model = modelInfo.model;
            modelRenderData.tex = modelInfo.tex;
            modelRenderData.boneCoords.clear();
            this.models.add(modelRenderData);
            VehicleScript script = sceneVehicle.script;
            float scale = script.getModelScale();
            float scale2 = 1.0f;
            ModelScript modelScript = modelInfo.modelScript;
            if (modelScript != null && modelScript.scale != 1.0f) {
                scale2 = modelScript.scale;
            }
            float scaleInvertX = 1.0f;
            if (modelScript != null) {
                scaleInvertX = modelScript.invertX ? -1.0f : 1.0f;
            }
            scaleInvertX *= -1.0f;
            Quaternionf modelRotQ = UI3DScene.allocQuaternionf();
            boolean rightToLeftHand = true;
            org.joml.Matrix4f renderTransform = modelRenderData.xfrm;
            Vector3f modelRotate = script.getModel().getRotate();
            modelRotQ.rotationXYZ(modelRotate.x * ((float)Math.PI / 180), modelRotate.y * ((float)Math.PI / 180), modelRotate.z * ((float)Math.PI / 180));
            Vector3f modelOffset = script.getModel().getOffset();
            renderTransform.translationRotateScale(modelOffset.x * 1.0f, modelOffset.y, modelOffset.z, modelRotQ.x, modelRotQ.y, modelRotQ.z, modelRotQ.w, scale * scale2 * scaleInvertX, scale * scale2, scale * scale2);
            ModelInstanceRenderData.postMultiplyMeshTransform(renderTransform, modelRenderData.model.mesh);
            this.transform.mul(renderTransform, renderTransform);
            UI3DScene.releaseQuaternionf(modelRotQ);
        }

        private void initWheelModel(SceneVehicle sceneVehicle, VehicleScript.Part scriptPart, org.joml.Matrix4f vehicleTransform) {
            VehicleScript script = sceneVehicle.script;
            float scale = script.getModelScale();
            VehicleScript.Wheel scriptWheel = script.getWheelById(scriptPart.wheel);
            if (scriptWheel == null || scriptPart.models.isEmpty()) {
                return;
            }
            VehicleScript.Model scriptModel = scriptPart.models.get(0);
            Vector3f modelOffset = scriptModel.getOffset();
            Vector3f modelRotate = scriptModel.getRotate();
            Model model = ModelManager.instance.getLoadedModel(scriptModel.file);
            if (model == null) {
                return;
            }
            VehicleModelRenderData modelRenderData = VehicleModelRenderData.s_pool.alloc();
            modelRenderData.model = model;
            modelRenderData.tex = model.tex;
            modelRenderData.boneCoords.clear();
            this.models.add(modelRenderData);
            float scale1 = scriptModel.scale;
            float scale2 = 1.0f;
            float scaleInvertX = 1.0f;
            ModelScript modelScript = ScriptManager.instance.getModelScript(scriptModel.file);
            if (modelScript != null) {
                scale2 = modelScript.scale;
                scaleInvertX = modelScript.invertX ? -1.0f : 1.0f;
            }
            Quaternionf modelRotQ = UI3DScene.allocQuaternionf();
            modelRotQ.rotationXYZ(modelRotate.x * ((float)Math.PI / 180), modelRotate.y * ((float)Math.PI / 180), modelRotate.z * ((float)Math.PI / 180));
            boolean rightToLeftHand = true;
            org.joml.Matrix4f renderTransform = modelRenderData.xfrm;
            renderTransform.translation(scriptWheel.offset.x / scale * 1.0f, scriptWheel.offset.y / scale, scriptWheel.offset.z / scale);
            org.joml.Matrix4f matrix4f = UI3DScene.allocMatrix4f();
            matrix4f.translationRotateScale(modelOffset.x * 1.0f, modelOffset.y, modelOffset.z, modelRotQ.x, modelRotQ.y, modelRotQ.z, modelRotQ.w, scale1 * scale2 * scaleInvertX, scale1 * scale2, scale1 * scale2);
            renderTransform.mul(matrix4f);
            UI3DScene.releaseMatrix4f(matrix4f);
            vehicleTransform.mul(renderTransform, renderTransform);
            ModelInstanceRenderData.postMultiplyMeshTransform(renderTransform, model.mesh);
            UI3DScene.releaseQuaternionf(modelRotQ);
        }

        private void initPartModels(SceneVehicle sceneVehicle, VehicleScript.Part scriptPart, org.joml.Matrix4f vehicleTransform) {
            for (int i = 0; i < scriptPart.getModelCount(); ++i) {
                VehicleScript.Model scriptModel = scriptPart.getModel(i);
                if (scriptPart.parent != null && scriptModel.attachmentNameParent != null) {
                    VehicleModelRenderData renderData = this.partToRenderData.get(scriptPart.parent);
                    if (renderData == null) continue;
                    this.initChildPartModel(sceneVehicle, renderData, scriptPart, scriptModel);
                    continue;
                }
                this.initPartModel(sceneVehicle, scriptPart, scriptModel, vehicleTransform);
            }
        }

        private void initPartModel(SceneVehicle sceneVehicle, VehicleScript.Part scriptPart, VehicleScript.Model scriptModel, org.joml.Matrix4f vehicleTransform) {
            AnimationPlayer animationPlayer;
            SceneVehicleModelInfo modelInfo = sceneVehicle.getModelInfoForPart(scriptPart.getId());
            if (modelInfo == null) {
                return;
            }
            Vector3f modelOffset = scriptModel.getOffset();
            Vector3f modelRotate = scriptModel.getRotate();
            Model model = modelInfo.model;
            if (model == null) {
                return;
            }
            VehicleModelRenderData modelRenderData = VehicleModelRenderData.s_pool.alloc();
            modelRenderData.model = model;
            modelRenderData.tex = model.tex == null ? sceneVehicle.modelInfo.get((int)0).tex : model.tex;
            modelRenderData.boneCoords.clear();
            AnimationPlayer animationPlayer2 = animationPlayer = model.isStatic ? null : modelInfo.getAnimationPlayer();
            if (animationPlayer != null) {
                modelInfo.updateAnimationPlayer();
                SkinningData skinningData = (SkinningData)model.tag;
                if (Core.debug && skinningData == null) {
                    DebugLog.General.warn("skinningData is null, matrixPalette may be invalid");
                }
                Matrix4f[] skinTransforms = animationPlayer.getSkinTransforms(skinningData);
                int matrixFloats = 16;
                if (modelRenderData.matrixPalette == null || modelRenderData.matrixPalette.capacity() < skinTransforms.length * 16) {
                    modelRenderData.matrixPalette = BufferUtils.createFloatBuffer(skinTransforms.length * 16);
                }
                modelRenderData.matrixPalette.clear();
                for (int i = 0; i < skinTransforms.length; ++i) {
                    skinTransforms[i].store(modelRenderData.matrixPalette);
                }
                modelRenderData.matrixPalette.flip();
                if (scriptPart.getId().equalsIgnoreCase(sceneVehicle.showBonesPartId) && scriptModel.getId().equalsIgnoreCase(sceneVehicle.showBonesModelId)) {
                    modelRenderData.initSkeleton(modelInfo);
                }
                this.partToRenderData.put(scriptPart.getId(), modelRenderData);
            }
            this.models.add(modelRenderData);
            float scale1 = scriptModel.scale;
            float scale2 = 1.0f;
            float scaleInvertX = 1.0f;
            ModelScript modelScript = ScriptManager.instance.getModelScript(scriptModel.file);
            if (modelScript != null) {
                scale2 = modelScript.scale;
                scaleInvertX = modelScript.invertX ? -1.0f : 1.0f;
            }
            scaleInvertX *= -1.0f;
            Quaternionf modelRotQ = UI3DScene.allocQuaternionf();
            modelRotQ.rotationXYZ(modelRotate.x * ((float)Math.PI / 180), modelRotate.y * ((float)Math.PI / 180), modelRotate.z * ((float)Math.PI / 180));
            boolean rightToLeftHand = true;
            org.joml.Matrix4f renderTransform = modelRenderData.xfrm;
            renderTransform.translationRotateScale(modelOffset.x * 1.0f, modelOffset.y, modelOffset.z, modelRotQ.x, modelRotQ.y, modelRotQ.z, modelRotQ.w, scale1 * scale2 * scaleInvertX, scale1 * scale2, scale1 * scale2);
            vehicleTransform.mul(renderTransform, renderTransform);
            ModelInstanceRenderData.postMultiplyMeshTransform(renderTransform, model.mesh);
            UI3DScene.releaseQuaternionf(modelRotQ);
        }

        void initChildPartModel(SceneVehicle sceneVehicle, VehicleModelRenderData parentRenderData, VehicleScript.Part scriptPart, VehicleScript.Model scriptModel) {
            SceneVehicleModelInfo modelInfo = sceneVehicle.getModelInfoForPart(scriptPart.getId());
            if (modelInfo == null) {
                return;
            }
            Model model = modelInfo.model;
            if (model == null) {
                return;
            }
            VehicleModelRenderData modelRenderData = VehicleModelRenderData.s_pool.alloc();
            modelRenderData.model = model;
            modelRenderData.tex = model.tex == null ? sceneVehicle.modelInfo.get((int)0).tex : model.tex;
            modelRenderData.boneCoords.clear();
            SceneVehicleModelInfo parentModelInfo = sceneVehicle.getModelInfoForPart(scriptPart.parent);
            org.joml.Matrix4f attachmentXfrm = UI3DScene.allocMatrix4f();
            this.initTransform(sceneVehicle, parentModelInfo.getAnimationPlayer(), parentModelInfo.modelScript, modelInfo.modelScript, scriptModel.attachmentNameParent, scriptModel.attachmentNameSelf, attachmentXfrm);
            parentRenderData.xfrm.mul(attachmentXfrm, modelRenderData.xfrm);
            float scale1 = scriptModel.scale;
            float scale2 = modelInfo.modelScript.scale;
            boolean bIgnoreVehicleScale = scriptModel.ignoreVehicleScale;
            float scale3 = bIgnoreVehicleScale ? 1.5f / sceneVehicle.script.getModelScale() : 1.0f;
            modelRenderData.xfrm.scale(scale1 * scale2 * scale3);
            ModelInstanceRenderData.postMultiplyMeshTransform(modelRenderData.xfrm, model.mesh);
            UI3DScene.releaseMatrix4f(attachmentXfrm);
            this.models.add(modelRenderData);
        }

        void initTransform(SceneVehicle sceneVehicle, AnimationPlayer animationPlayer, ModelScript parentModelScript, ModelScript modelScript, String attachmentNameParent, String attachmentNameSelf, org.joml.Matrix4f transform) {
            ModelAttachment selfAttachment;
            transform.identity();
            org.joml.Matrix4f attachmentXfrm = UI3DScene.allocMatrix4f();
            ModelAttachment parentAttachment = parentModelScript.getAttachmentById(attachmentNameParent);
            if (parentAttachment == null) {
                parentAttachment = sceneVehicle.script.getAttachmentById(attachmentNameParent);
            }
            if (parentAttachment != null) {
                ModelInstanceRenderData.makeBoneTransform(animationPlayer, parentAttachment.getBone(), transform);
                transform.scale(1.0f / parentModelScript.scale);
                ModelInstanceRenderData.makeAttachmentTransform(parentAttachment, attachmentXfrm);
                transform.mul(attachmentXfrm);
            }
            if ((selfAttachment = modelScript.getAttachmentById(attachmentNameSelf)) != null) {
                ModelInstanceRenderData.makeAttachmentTransform(selfAttachment, attachmentXfrm);
                if (ModelInstanceRenderData.invertAttachmentSelfTransform) {
                    attachmentXfrm.invert();
                }
                transform.mul(attachmentXfrm);
            }
            UI3DScene.releaseMatrix4f(attachmentXfrm);
        }

        @Override
        void release() {
            s_pool.release(this);
        }
    }

    private static class ModelRenderData
    extends SceneObjectRenderData {
        final ModelDrawer drawer = new ModelDrawer();
        private static final ObjectPool<ModelRenderData> s_pool = new ObjectPool<ModelRenderData>(ModelRenderData::new);

        private ModelRenderData() {
        }

        SceneObjectRenderData initModel(SceneModel sceneObject) {
            this.init(sceneObject);
            if (sceneObject.useWorldAttachment) {
                ModelAttachment attachment;
                if (sceneObject.weaponRotationHack) {
                    this.transform.rotateXYZ(0.0f, (float)Math.PI, 1.5707964f);
                }
                if (sceneObject.modelScript != null && (attachment = sceneObject.modelScript.getAttachmentById("world")) != null) {
                    org.joml.Matrix4f m = ModelInstanceRenderData.makeAttachmentTransform(attachment, UI3DScene.allocMatrix4f());
                    m.invert();
                    this.transform.mul(m);
                    UI3DScene.releaseMatrix4f(m);
                }
            }
            if (sceneObject.ignoreVehicleScale && sceneObject.parentVehiclePart != null && sceneObject.parentVehiclePart.vehicle.script != null) {
                this.transform.scale(1.5f / sceneObject.parentVehiclePart.vehicle.script.getModelScale());
            }
            ModelInstanceRenderData.postMultiplyMeshTransform(this.transform, sceneObject.model.mesh);
            if (sceneObject.modelScript != null && sceneObject.modelScript.scale != 1.0f) {
                this.transform.scale(sceneObject.modelScript.scale);
            }
            this.drawer.init(sceneObject, this);
            return this;
        }

        @Override
        void release() {
            s_pool.release(this);
        }
    }

    private static class CharacterRenderData
    extends SceneObjectRenderData {
        final CharacterDrawer drawer = new CharacterDrawer();
        private static final ObjectPool<CharacterRenderData> s_pool = new ObjectPool<CharacterRenderData>(CharacterRenderData::new);

        private CharacterRenderData() {
        }

        SceneObjectRenderData initCharacter(SceneCharacter sceneObject) {
            this.drawer.init(sceneObject, this);
            this.init(sceneObject);
            return this;
        }

        @Override
        void release() {
            s_pool.release(this);
        }
    }

    private static final class ScenePolygonDrawer
    extends TextureDraw.GenericDrawer {
        ScenePolygonRenderData renderData;

        private ScenePolygonDrawer() {
        }

        public void init(ScenePolygonRenderData renderData) {
            this.renderData = renderData;
        }

        @Override
        public void render() {
            int i;
            UI3DScene scene = this.renderData.polygon.scene;
            StateData stateData = scene.stateDataRender();
            GL11.glViewport(scene.getAbsoluteX().intValue(), Core.getInstance().getScreenHeight() - scene.getAbsoluteY().intValue() - scene.getHeight().intValue(), scene.getWidth().intValue(), scene.getHeight().intValue());
            PZGLUtil.pushAndLoadMatrix(5889, stateData.projection);
            org.joml.Matrix4f matrix4f = UI3DScene.allocMatrix4f();
            matrix4f.set(stateData.modelView);
            matrix4f.mul(this.renderData.transform);
            PZGLUtil.pushAndLoadMatrix(5888, matrix4f);
            UI3DScene.releaseMatrix4f(matrix4f);
            GL11.glDepthMask(false);
            GL11.glDepthFunc(513);
            ScenePolygon scenePolygon = this.renderData.polygon;
            Vector3f extents = scenePolygon.extents;
            GL11.glPolygonMode(1032, 6914);
            vboRenderer.startRun(UI3DScene.vboRenderer.formatPositionColor);
            vboRenderer.setLineWidth(2.0f);
            vboRenderer.setMode(4);
            vboRenderer.setDepthTest(true);
            GL20.glUseProgram(0);
            ShaderHelper.forgetCurrentlyBound();
            boolean drawGeometry = scene.drawGeometry;
            GL11.glColorMask(drawGeometry, drawGeometry, drawGeometry, drawGeometry);
            float alpha = 0.25f;
            ArrayList<Vector3f> tris = this.renderData.triangles;
            if (!tris.isEmpty()) {
                float r = 0.0f;
                float g = 1.0f;
                float b = 0.0f;
                for (i = 0; i < tris.size(); i += 3) {
                    Vector3f p0 = tris.get(i);
                    Vector3f p1 = tris.get(i + 1);
                    Vector3f p2 = tris.get(i + 2);
                    vboRenderer.addTriangle(p0.x, p0.y, p0.z, p1.x, p1.y, p1.z, p2.x, p2.y, p2.z, r, g, b, 0.25f);
                }
            }
            vboRenderer.endRun();
            vboRenderer.startRun(UI3DScene.vboRenderer.formatPositionColor);
            vboRenderer.setMode(1);
            vboRenderer.setDepthTest(false);
            GL11.glDepthFunc(519);
            GL11.glColorMask(true, true, true, true);
            GL11.glPolygonMode(1032, 6914);
            for (i = 0; i < this.renderData.points.size(); ++i) {
                Vector3f scenePos1 = this.renderData.points.get(i);
                Vector3f scenePos2 = this.renderData.points.get((i + 1) % this.renderData.points.size());
                vboRenderer.addLine(scenePos1.x, scenePos1.y, scenePos1.z, scenePos2.x, scenePos2.y, scenePos2.z, 1.0f, 1.0f, 1.0f, scenePolygon.editing ? 1.0f : 0.5f);
            }
            vboRenderer.endRun();
            vboRenderer.flush();
            PZGLUtil.popMatrix(5889);
            PZGLUtil.popMatrix(5888);
            ShaderHelper.glUseProgramObjectARB(0);
            GLStateRenderThread.restore();
        }

        @Override
        public void postRender() {
        }
    }

    private static class ScenePolygonRenderData
    extends SceneObjectRenderData {
        final ScenePolygonDrawer drawer = new ScenePolygonDrawer();
        ScenePolygon polygon;
        final ArrayList<Vector3f> points = new ArrayList();
        final ArrayList<Vector3f> triangles = new ArrayList();
        private static final ObjectPool<ScenePolygonRenderData> s_pool = new ObjectPool<ScenePolygonRenderData>(ScenePolygonRenderData::new);

        private ScenePolygonRenderData() {
        }

        SceneObjectRenderData initPolygon(ScenePolygon scenePolygon) {
            int i;
            this.init(scenePolygon);
            PolygonEditor polygonEditor = scenePolygon.scene.polygonEditor;
            polygonEditor.setPlane(scenePolygon.translate, scenePolygon.rotate, scenePolygon.plane);
            this.points.clear();
            for (i = 0; i < scenePolygon.points.size(); ++i) {
                Vector2f pointOnPlane = scenePolygon.points.get(i);
                this.points.add(UI3DScene.allocVector3f().set(pointOnPlane.x, pointOnPlane.y, 0.0f));
            }
            this.triangles.clear();
            for (i = 0; i < scenePolygon.triangles.size(); i += 2) {
                float x = scenePolygon.triangles.get(i);
                float y = scenePolygon.triangles.get(i + 1);
                this.triangles.add(UI3DScene.allocVector3f().set(x, y, 0.0f));
            }
            this.drawer.init(this);
            this.polygon = scenePolygon;
            return this;
        }

        @Override
        void release() {
            BaseVehicle.TL_vector3f_pool.get().releaseAll(this.points);
            this.points.clear();
            BaseVehicle.TL_vector3f_pool.get().releaseAll(this.triangles);
            this.triangles.clear();
            s_pool.release(this);
        }
    }

    private static final class SceneCylinderDrawer
    extends TextureDraw.GenericDrawer {
        SceneCylinder sceneObject;
        float radiusBase;
        float radiusTop;
        float length;
        int slices = 32;
        int stacks = 2;
        private static final ObjectPool<SceneCylinderDrawer> s_pool = new ObjectPool<SceneCylinderDrawer>(SceneCylinderDrawer::new);

        private SceneCylinderDrawer() {
        }

        @Override
        public void render() {
            UI3DScene scene = this.sceneObject.scene;
            StateData stateData = scene.stateDataRender();
            PZGLUtil.pushAndLoadMatrix(5889, stateData.projection);
            PZGLUtil.pushAndLoadMatrix(5888, stateData.modelView);
            boolean wireframe = false;
            GL11.glPolygonMode(1032, 6914);
            GL20.glUseProgram(0);
            ShaderHelper.forgetCurrentlyBound();
            GL11.glDisable(2929);
            for (int i = 7; i >= 0; --i) {
                GL13.glActiveTexture(33984 + i);
                GL11.glDisable(3553);
            }
            GL11.glColor4f(1.0f, 1.0f, 1.0f, 1.0f);
            boolean bLighting = false;
            VBORenderer vbor = VBORenderer.getInstance();
            org.joml.Matrix4f cylinderMatrix = UI3DScene.allocMatrix4f();
            this.sceneObject.getGlobalTransform(cylinderMatrix);
            PZGLUtil.pushAndMultMatrix(5888, cylinderMatrix);
            GL11.glEnable(2929);
            GL11.glDepthFunc(513);
            Core.getInstance().modelViewMatrixStack.peek().translate(0.0f, 0.0f, -this.length / 2.0f);
            float r = 1.0f;
            float g = 1.0f;
            float b = this.sceneObject.selected ? 0.0f : 1.0f;
            vbor.addCylinder_Line(this.radiusBase, this.radiusTop, this.length, this.slices, this.stacks, 1.0f, 1.0f, b, 0.75f);
            vbor.flush();
            Core.getInstance().modelViewMatrixStack.peek().translate(0.0f, 0.0f, this.length / 2.0f);
            GL11.glDisable(2929);
            GL11.glDepthFunc(519);
            GL11.glPolygonMode(1032, 6914);
            float mouseX = Mouse.getXA() - scene.getAbsoluteX().intValue();
            float mouseY = Mouse.getYA() - scene.getAbsoluteY().intValue();
            Ray cameraRay = scene.getCameraRay(mouseX, (float)scene.screenHeight() - mouseY, stateData.projection, stateData.modelView, UI3DScene.allocRay());
            cylinderMatrix.invert();
            cylinderMatrix.transformPosition(cameraRay.origin);
            cylinderMatrix.transformDirection(cameraRay.direction);
            UI3DScene.releaseMatrix4f(cylinderMatrix);
            CylinderUtils.IntersectionRecord intersectionRecord = new CylinderUtils.IntersectionRecord();
            if (this.sceneObject.intersect(cameraRay, intersectionRecord)) {
                vbor.startRun(vbor.formatPositionColor);
                vbor.setMode(1);
                vbor.addLine(intersectionRecord.location.x, intersectionRecord.location.y, intersectionRecord.location.z, intersectionRecord.location.x + intersectionRecord.normal.x, intersectionRecord.location.y + intersectionRecord.normal.y, intersectionRecord.location.z + intersectionRecord.normal.z, 1.0f, 1.0f, 1.0f, 1.0f);
                vboRenderer.endRun();
                vbor.flush();
                Model.debugDrawAxis(intersectionRecord.location.x, intersectionRecord.location.y, intersectionRecord.location.z, 0.1f, 1.0f);
            }
            UI3DScene.releaseRay(cameraRay);
            PZGLUtil.popMatrix(5888);
            PZGLUtil.popMatrix(5889);
            PZGLUtil.popMatrix(5888);
            ShaderHelper.glUseProgramObjectARB(0);
            GLStateRenderThread.restore();
        }

        @Override
        public void postRender() {
            s_pool.release(this);
        }
    }

    private static final class SceneVehicleModelInfo {
        SceneVehicle sceneVehicle;
        VehicleScript.Part part;
        VehicleScript.Model scriptModel;
        ModelScript modelScript;
        int wheelIndex;
        Model model;
        Texture tex;
        AnimationPlayer animPlayer;
        AnimationTrack track;
        private static final ObjectPool<SceneVehicleModelInfo> s_pool = new ObjectPool<SceneVehicleModelInfo>(SceneVehicleModelInfo::new);

        private SceneVehicleModelInfo() {
        }

        public AnimationPlayer getAnimationPlayer() {
            SceneVehicleModelInfo modelInfoParent;
            if (this.part != null && this.part.parent != null && (modelInfoParent = this.sceneVehicle.getModelInfoForPart(this.part.parent)) != null) {
                return modelInfoParent.getAnimationPlayer();
            }
            String modelName = this.scriptModel.file;
            Model model = ModelManager.instance.getLoadedModel(modelName);
            if (model == null || model.isStatic) {
                return null;
            }
            if (this.animPlayer != null && this.animPlayer.getModel() != model) {
                this.animPlayer = Pool.tryRelease(this.animPlayer);
            }
            if (this.animPlayer == null) {
                this.animPlayer = AnimationPlayer.alloc(model);
            }
            return this.animPlayer;
        }

        public void releaseAnimationPlayer() {
            this.animPlayer = Pool.tryRelease(this.animPlayer);
        }

        public void playPartAnim(String animId) {
            AnimationTrack track;
            VehicleScript.Anim anim = this.part.getAnimById(animId);
            if (anim == null || StringUtils.isNullOrWhitespace(anim.anim)) {
                return;
            }
            AnimationPlayer animPlayer = this.getAnimationPlayer();
            if (animPlayer == null || !animPlayer.isReady()) {
                return;
            }
            if (animPlayer.getMultiTrack().getIndexOfTrack(this.track) != -1) {
                animPlayer.getMultiTrack().removeTrack(this.track);
            }
            this.track = null;
            SkinningData skinningData = animPlayer.getSkinningData();
            if (skinningData != null && !skinningData.animationClips.containsKey(anim.anim)) {
                return;
            }
            this.track = track = animPlayer.play(anim.anim, anim.loop);
            if (track != null) {
                track.setBlendWeight(1.0f);
                track.setSpeedDelta(anim.rate);
                track.isPlaying = anim.animate;
                track.reverse = anim.reverse;
                if (!this.modelScript.boneWeights.isEmpty()) {
                    track.setBoneWeights(this.modelScript.boneWeights);
                    track.initBoneWeights(skinningData);
                }
                if (this.part.window != null) {
                    float openDelta = 0.0f;
                    track.setCurrentTimeValue(track.getDuration() * 0.0f);
                }
            }
        }

        protected void updateAnimationPlayer() {
            AnimationTrack track;
            AnimationPlayer animPlayer = this.getAnimationPlayer();
            if (animPlayer == null || !animPlayer.isReady()) {
                return;
            }
            AnimationMultiTrack multiTrack = animPlayer.getMultiTrack();
            float del = 0.016666668f;
            del *= 0.8f;
            animPlayer.Update(del *= GameTime.instance.getUnmoddedMultiplier());
            for (int i = 0; i < multiTrack.getTrackCount(); ++i) {
                track = multiTrack.getTracks().get(i);
                if (!track.isPlaying || !track.isFinished()) continue;
                multiTrack.removeTrackAt(i);
                --i;
            }
            if (this.part == null) {
                return;
            }
            SceneVehicleModelInfo modelInfo = this;
            if (modelInfo.track != null && multiTrack.getIndexOfTrack(modelInfo.track) == -1) {
                modelInfo.track = null;
            }
            if (modelInfo.track != null) {
                if (this.part.window != null) {
                    track = modelInfo.track;
                    float openDelta = 0.0f;
                    track.setCurrentTimeValue(track.getDuration() * 0.0f);
                }
                return;
            }
            if (this.part.door != null) {
                boolean bOpen = false;
                this.playPartAnim("Closed");
            }
            if (this.part.window != null) {
                this.playPartAnim("ClosedToOpen");
            }
        }

        void release() {
            s_pool.release(this);
        }
    }
}

