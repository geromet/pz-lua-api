/*
 * Decompiled with CFR 0.152.
 */
package zombie.gizmo;

import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.lwjgl.opengl.GL11;
import org.lwjglx.util.glu.PartialDisk;
import zombie.Lua.LuaManager;
import zombie.UsedFromLua;
import zombie.core.opengl.GLStateRenderThread;
import zombie.core.opengl.VBORenderer;
import zombie.core.skinnedmodel.model.Model;
import zombie.gizmo.Axis;
import zombie.gizmo.Gizmo;
import zombie.gizmo.Scene;
import zombie.gizmo.StateData;
import zombie.gizmo.TransformMode;
import zombie.ui.UIManager;
import zombie.vehicles.UI3DScene;

@UsedFromLua
public class TranslateGizmo
extends Gizmo {
    final Matrix4f startXfrm = new Matrix4f();
    final Matrix4f startInvXfrm = new Matrix4f();
    final Vector3f startPos = new Vector3f();
    final Vector3f currentPos = new Vector3f();
    Axis trackAxis = Axis.None;
    boolean doubleAxis;
    final PartialDisk disk = new PartialDisk();
    final Vector3f startTranslate = new Vector3f();

    TranslateGizmo(Scene scene) {
        super(scene);
        this.reverseZAxis = true;
    }

    @Override
    Axis hitTest(float uiX, float uiY) {
        if (!this.visible) {
            return Axis.None;
        }
        StateData stateData = this.scene.stateDataMain();
        this.scene.setModelViewProjection(stateData);
        this.scene.setGizmoTransforms(stateData);
        Matrix4f gizmoXfrm = this.allocMatrix4f();
        gizmoXfrm.set(stateData.gizmoParentTransform);
        gizmoXfrm.mul(stateData.gizmoOriginTransform);
        gizmoXfrm.mul(stateData.gizmoChildTransform);
        if (stateData.selectedAttachmentIsChildAttachment) {
            gizmoXfrm.mul(stateData.gizmoChildAttachmentTransformInv);
        }
        gizmoXfrm.mul(stateData.gizmoTransform);
        if (this.transformMode == TransformMode.Global) {
            gizmoXfrm.setRotationXYZ(0.0f, 0.0f, 0.0f);
        }
        uiY = (float)this.scene.screenHeight() - uiY;
        UI3DScene.Ray cameraRay = this.scene.getCameraRay(uiX, uiY, this.allocRay());
        UI3DScene.Ray axis = this.allocRay();
        gizmoXfrm.transformPosition(axis.origin.set(0.0f, 0.0f, 0.0f));
        float scale = this.getScale();
        float length = 0.5f * scale;
        float thickness = 0.05f * scale;
        float offset = 0.3f * scale;
        gizmoXfrm.transformDirection(axis.direction.set(1.0f, 0.0f, 0.0f)).normalize();
        float distX = UI3DScene.closest_distance_between_lines(axis, cameraRay);
        float xT = axis.t;
        float camXT = cameraRay.t;
        if (!this.gizmoAxisVisibleX || xT < offset || xT >= offset + length) {
            xT = Float.MAX_VALUE;
            distX = Float.MAX_VALUE;
        }
        float xdot = axis.direction.dot(cameraRay.direction);
        stateData.translateGizmoRenderData.hideX = !this.gizmoAxisVisibleX || Math.abs(xdot) > 0.9f;
        gizmoXfrm.transformDirection(axis.direction.set(0.0f, 1.0f, 0.0f)).normalize();
        float distY = UI3DScene.closest_distance_between_lines(axis, cameraRay);
        float yT = axis.t;
        float camYT = cameraRay.t;
        if (!this.gizmoAxisVisibleY || yT < offset || yT >= offset + length) {
            yT = Float.MAX_VALUE;
            distY = Float.MAX_VALUE;
        }
        float ydot = axis.direction.dot(cameraRay.direction);
        stateData.translateGizmoRenderData.hideY = !this.gizmoAxisVisibleY || Math.abs(ydot) > 0.9f;
        gizmoXfrm.transformDirection(axis.direction.set(0.0f, 0.0f, this.transformMode == TransformMode.Global && this.reverseZAxis ? -1.0f : 1.0f)).normalize();
        float distZ = UI3DScene.closest_distance_between_lines(axis, cameraRay);
        float zT = axis.t;
        float camZT = cameraRay.t;
        if (!this.gizmoAxisVisibleZ || zT < offset || zT >= offset + length) {
            zT = Float.MAX_VALUE;
            distZ = Float.MAX_VALUE;
        }
        float zdot = axis.direction.dot(cameraRay.direction);
        stateData.translateGizmoRenderData.hideZ = !this.gizmoAxisVisibleZ || Math.abs(zdot) > 0.9f;
        Axis doubleAxis = Axis.None;
        if (this.doubleAxis) {
            float localOffset = thickness * 1.5f;
            float inner = localOffset + offset;
            float outer = inner + length / 2.0f;
            Vector3f pointOnPlane3D = this.allocVector3f();
            Vector2f pointOnPlane2D = this.allocVector2f();
            if (this.getPointOnDualAxis(uiX, -(uiY - (float)this.scene.screenHeight()), Axis.XY, gizmoXfrm, pointOnPlane3D, pointOnPlane2D) && pointOnPlane2D.x >= 0.0f && pointOnPlane2D.y >= 0.0f && pointOnPlane2D.length() >= inner && pointOnPlane2D.length() < outer) {
                doubleAxis = Axis.XY;
            }
            if (this.getPointOnDualAxis(uiX, -(uiY - (float)this.scene.screenHeight()), Axis.XZ, gizmoXfrm, pointOnPlane3D, pointOnPlane2D) && pointOnPlane2D.x >= 0.0f && pointOnPlane2D.y >= 0.0f && pointOnPlane2D.length() >= inner && pointOnPlane2D.length() < outer) {
                doubleAxis = Axis.XZ;
            }
            if (this.getPointOnDualAxis(uiX, -(uiY - (float)this.scene.screenHeight()), Axis.YZ, gizmoXfrm, pointOnPlane3D, pointOnPlane2D) && pointOnPlane2D.x >= 0.0f && pointOnPlane2D.y >= 0.0f && pointOnPlane2D.length() >= inner && pointOnPlane2D.length() < outer) {
                doubleAxis = Axis.YZ;
            }
            this.releaseVector3f(pointOnPlane3D);
            this.releaseVector2f(pointOnPlane2D);
        }
        TranslateGizmo.releaseRay(axis);
        TranslateGizmo.releaseRay(cameraRay);
        this.releaseMatrix4f(gizmoXfrm);
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
        StateData stateData = this.scene.stateDataMain();
        this.scene.setModelViewProjection(stateData);
        this.scene.setGizmoTransforms(stateData);
        this.startXfrm.set(stateData.gizmoParentTransform);
        this.startXfrm.mul(stateData.gizmoOriginTransform);
        this.startXfrm.mul(stateData.gizmoChildTransform);
        if (!stateData.selectedAttachmentIsChildAttachment) {
            this.startXfrm.mul(stateData.gizmoTransform);
        }
        if (this.transformMode == TransformMode.Global) {
            this.startXfrm.setRotationXYZ(0.0f, 0.0f, 0.0f);
        }
        this.startInvXfrm.set(this.startXfrm);
        this.startInvXfrm.invert();
        this.trackAxis = axis;
        this.getPointOnAxis(uiX, uiY, axis, this.startXfrm, this.startPos);
        this.startTranslate.set(stateData.gizmoWorldPos);
    }

    @Override
    void updateTracking(float uiX, float uiY) {
        Vector3f pos = this.getPointOnAxis(uiX, uiY, this.trackAxis, this.startXfrm, this.allocVector3f());
        if (this.currentPos.equals(pos)) {
            this.releaseVector3f(pos);
            return;
        }
        this.currentPos.set(pos);
        this.releaseVector3f(pos);
        StateData stateData = this.scene.stateDataMain();
        this.scene.setModelViewProjection(stateData);
        this.scene.setGizmoTransforms(stateData);
        Vector3f delta = this.allocVector3f().set(this.currentPos).sub(this.startPos);
        if (this.scene.selectedAttachment == null && stateData.gizmoChild == null && !stateData.gizmoOriginIsGeometry) {
            delta.set(this.currentPos).sub(this.startPos);
        } else if (this.transformMode == TransformMode.Global) {
            vs = this.startInvXfrm.transformPosition(this.startPos, this.allocVector3f());
            vc = this.startInvXfrm.transformPosition(this.currentPos, this.allocVector3f());
            m = this.allocMatrix4f();
            m.set(stateData.gizmoParentTransform);
            m.mul(stateData.gizmoOriginTransform);
            if (!stateData.selectedAttachmentIsChildAttachment) {
                m.mul(stateData.gizmoChildTransform);
            }
            m.invert();
            m.transformPosition(vs);
            m.transformPosition(vc);
            this.releaseMatrix4f(m);
            delta.set(vc).sub(vs);
            this.releaseVector3f(vs);
            this.releaseVector3f(vc);
        } else {
            vs = this.startInvXfrm.transformPosition(this.startPos, this.allocVector3f());
            vc = this.startInvXfrm.transformPosition(this.currentPos, this.allocVector3f());
            m = this.allocMatrix4f();
            m.set(stateData.gizmoTransform);
            m.transformPosition(vs);
            m.transformPosition(vc);
            this.releaseMatrix4f(m);
            delta.set(vc).sub(vs);
            this.releaseVector3f(vs);
            this.releaseVector3f(vc);
        }
        if (this.getTable() != null) {
            float x = this.startTranslate.x + delta.x;
            float y = this.startTranslate.y - delta.z;
            float z = this.startTranslate.z + delta.y / 2.44949f;
            delta.set(x, y, z);
            LuaManager.caller.pcall(UIManager.getDefaultThread(), this.getTable().rawget("onTranslateGizmo"), this.getTable(), delta);
        }
        this.releaseVector3f(delta);
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
        StateData stateData = this.scene.stateDataRender();
        Matrix4f matrix4f = this.allocMatrix4f();
        matrix4f.set(stateData.gizmoParentTransform);
        matrix4f.mul(stateData.gizmoOriginTransform);
        matrix4f.mul(stateData.gizmoChildTransform);
        if (stateData.selectedAttachmentIsChildAttachment) {
            matrix4f.mul(stateData.gizmoChildAttachmentTransformInv);
        }
        matrix4f.mul(stateData.gizmoTransform);
        Vector3f scale = matrix4f.getScale(this.allocVector3f());
        matrix4f.scale(1.0f / scale.x, 1.0f / scale.y, 1.0f / scale.z);
        this.releaseVector3f(scale);
        if (this.transformMode == TransformMode.Global) {
            matrix4f.setRotationXYZ(0.0f, 0.0f, 0.0f);
        }
        stateData.modelView.mul(matrix4f, matrix4f);
        VBORenderer vbor = VBORenderer.getInstance();
        boolean flipZ = this.transformMode == TransformMode.Global && this.reverseZAxis;
        vbor.cmdPushAndLoadMatrix(5888, matrix4f);
        Model.debugDrawAxis(0.0f, 0.0f, 0.0f, false, false, flipZ, 0.5f, 1.0f);
        vbor.cmdPopMatrix(5888);
        float scale2 = this.getScale();
        float thickness = 0.05f * scale2;
        float length = 0.5f * scale2;
        float offset = 0.3f * scale2;
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
            vbor.addCylinder_Fill(thickness / 2.0f, thickness / 2.0f, length, 8, 1, r, 0.0f, 0.0f, 1.0f);
            vbor.cmdPopMatrix(5888);
            matrix4f.translate(0.0f, 0.0f, length);
            vbor.cmdPushAndMultMatrix(5888, matrix4f);
            vbor.addCylinder_Fill(thickness / 2.0f * 2.0f, 0.0f, 0.1f * scale2, 8, 1, r, 0.0f, 0.0f, 1.0f);
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
            vbor.addCylinder_Fill(thickness / 2.0f, thickness / 2.0f, length, 8, 1, 0.0f, g, 0.0f, 1.0f);
            vbor.cmdPopMatrix(5888);
            matrix4f.translate(0.0f, 0.0f, length);
            vbor.cmdPushAndMultMatrix(5888, matrix4f);
            vbor.addCylinder_Fill(thickness / 2.0f * 2.0f, 0.0f, 0.1f * scale2, 8, 1, 0.0f, g, 0.0f, 1.0f);
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
            matrix4f.translation(0.0f, 0.0f, -offset);
            matrix4f.rotateY((float)Math.PI);
            vbor.cmdPushAndMultMatrix(5888, matrix4f);
            vbor.addCylinder_Fill(thickness / 2.0f, thickness / 2.0f, length, 8, 1, 0.0f, 0.0f, b, 1.0f);
            vbor.cmdPopMatrix(5888);
            matrix4f.translate(0.0f, 0.0f, length);
            vbor.cmdPushAndMultMatrix(5888, matrix4f);
            vbor.addCylinder_Fill(thickness / 2.0f * 2.0f, 0.0f, 0.1f * scale2, 8, 1, 0.0f, 0.0f, b, 1.0f);
            vbor.cmdPopMatrix(5888);
        }
        if (this.doubleAxis) {
            float localOffset = thickness * 1.5f;
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
        vbor.cmdPopMatrix(5888);
        this.releaseMatrix4f(matrix4f);
        this.renderLineToOrigin();
        GLStateRenderThread.restore();
    }
}

