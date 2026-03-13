/*
 * Decompiled with CFR 0.152.
 */
package zombie.gizmo;

import java.util.ArrayList;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.lwjgl.opengl.GL11;
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
import zombie.input.GameKeyboard;
import zombie.iso.IsoUtils;
import zombie.ui.UIManager;
import zombie.vehicles.BaseVehicle;
import zombie.vehicles.UI3DScene;

@UsedFromLua
public class RotateGizmo
extends Gizmo {
    Axis trackAxis = Axis.None;
    boolean snap = true;
    final UI3DScene.Circle trackCircle = new UI3DScene.Circle();
    final Matrix4f startXfrm = new Matrix4f();
    final Matrix4f startInvXfrm = new Matrix4f();
    final Vector3f startPointOnCircle = new Vector3f();
    final Vector3f startRotate = new Vector3f();
    final Vector3f currentPointOnCircle = new Vector3f();
    final ArrayList<Vector3f> circlePointsMain = new ArrayList();
    final ArrayList<Vector3f> circlePointsRender = new ArrayList();

    RotateGizmo(Scene scene) {
        super(scene);
        this.transformMode = TransformMode.Local;
    }

    @Override
    Axis hitTest(float uiX, float uiY) {
        if (!this.visible) {
            return Axis.None;
        }
        StateData stateData = this.scene.stateDataMain();
        this.scene.setModelViewProjection(stateData);
        this.scene.setGizmoTransforms(stateData);
        uiY = (float)this.scene.screenHeight() - uiY;
        UI3DScene.Ray cameraRay = this.scene.getCameraRay(uiX, uiY, this.allocRay());
        Matrix4f gizmoXfrm = this.allocMatrix4f();
        gizmoXfrm.set(stateData.gizmoParentTransform);
        gizmoXfrm.mul(stateData.gizmoOriginTransform);
        gizmoXfrm.mul(stateData.gizmoChildTransform);
        if (stateData.selectedAttachmentIsChildAttachment) {
            gizmoXfrm.mul(stateData.gizmoChildAttachmentTransformInv);
        }
        gizmoXfrm.mul(stateData.gizmoTransform);
        Vector3f scale = gizmoXfrm.getScale(this.allocVector3f());
        gizmoXfrm.scale(1.0f / scale.x, 1.0f / scale.y, 1.0f / scale.z);
        this.releaseVector3f(scale);
        if (this.transformMode == TransformMode.Global) {
            gizmoXfrm.setRotationXYZ(0.0f, 0.0f, 0.0f);
        }
        float scale2 = this.getScale();
        float radius = 0.5f * scale2;
        Vector3f center = gizmoXfrm.transformProject(this.allocVector3f().set(0.0f, 0.0f, 0.0f));
        Vector3f xAxis = gizmoXfrm.transformDirection(this.allocVector3f().set(1.0f, 0.0f, 0.0f)).normalize();
        Vector3f yAxis = gizmoXfrm.transformDirection(this.allocVector3f().set(0.0f, 1.0f, 0.0f)).normalize();
        Vector3f zAxis = gizmoXfrm.transformDirection(this.allocVector3f().set(0.0f, 0.0f, 1.0f)).normalize();
        Vector2f point = this.allocVector2f();
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
        this.releaseVector2f(point);
        this.releaseVector3f(xAxis);
        this.releaseVector3f(yAxis);
        this.releaseVector3f(zAxis);
        this.releaseVector3f(center);
        RotateGizmo.releaseRay(cameraRay);
        this.releaseMatrix4f(gizmoXfrm);
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
        StateData stateData = this.scene.stateDataMain();
        this.scene.setModelViewProjection(stateData);
        this.scene.setGizmoTransforms(stateData);
        this.startRotate.set(stateData.gizmoRotate);
        this.startXfrm.set(stateData.gizmoParentTransform);
        this.startXfrm.mul(stateData.gizmoOriginTransform);
        this.startXfrm.mul(stateData.gizmoChildTransform);
        if (!stateData.selectedAttachmentIsChildAttachment) {
            this.startXfrm.mul(stateData.gizmoTransform);
        }
        if (this.transformMode == TransformMode.Global) {
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
        Vector3f pos = this.getPointOnAxis(uiX, uiY, this.trackAxis, this.trackCircle, this.startXfrm, this.allocVector3f());
        if (this.currentPointOnCircle.equals(pos)) {
            this.releaseVector3f(pos);
            return;
        }
        this.currentPointOnCircle.set(pos);
        this.releaseVector3f(pos);
        float angle = this.calculateRotation(this.startPointOnCircle, this.currentPointOnCircle, this.trackCircle);
        if (GameKeyboard.isKeyDown(29)) {
            angle = angle > 0.0f ? (float)((int)(angle / 5.0f) * 5) : (float)(Math.round(-angle / 5.0f) * -5);
        }
        switch (this.trackAxis) {
            case X: {
                this.trackCircle.orientation.set(1.0f, 0.0f, 0.0f);
                break;
            }
            case Y: {
                this.trackCircle.orientation.set(0.0f, 1.0f, 0.0f);
                break;
            }
            case Z: {
                this.trackCircle.orientation.set(0.0f, 0.0f, 1.0f);
            }
        }
        Vector3f orientation = this.allocVector3f().set(this.trackCircle.orientation);
        UI3DScene.Ray cameraRay = this.scene.getCameraRay(uiX, uiY, this.allocRay());
        Vector3f orientation2 = this.startXfrm.transformDirection(this.allocVector3f().set(orientation)).normalize();
        float dot = cameraRay.direction.dot(orientation2);
        this.releaseVector3f(orientation2);
        RotateGizmo.releaseRay(cameraRay);
        if (dot > 0.0f) {
            angle *= -1.0f;
        }
        if (this.transformMode == TransformMode.Global) {
            this.startInvXfrm.transformDirection(orientation);
        }
        Quaternionf rotationQuat = this.allocQuaternionf().fromAxisAngleDeg(orientation, angle);
        this.releaseVector3f(orientation);
        Vector3f delta = rotationQuat.getEulerAnglesXYZ(this.allocVector3f());
        this.releaseQuaternionf(rotationQuat);
        delta.mul(57.295776f);
        if (this.snap) {
            // empty if block
        }
        if (this.getTable() != null) {
            Quaternionf q0 = this.allocQuaternionf().rotationXYZ(this.startRotate.x * ((float)Math.PI / 180), this.startRotate.y * ((float)Math.PI / 180), this.startRotate.z * ((float)Math.PI / 180));
            Quaternionf q1 = this.allocQuaternionf().rotationXYZ(delta.x * ((float)Math.PI / 180), delta.y * ((float)Math.PI / 180), delta.z * ((float)Math.PI / 180));
            q0.mul(q1);
            Vector3f rot = q0.getEulerAnglesXYZ(this.allocVector3f());
            this.releaseQuaternionf(q0);
            this.releaseQuaternionf(q1);
            rot.mul(57.295776f);
            rot.set(rot.x, rot.z, rot.y);
            LuaManager.caller.pcall(UIManager.getDefaultThread(), this.getTable().rawget("onRotateGizmo"), this.getTable(), rot);
            this.releaseVector3f(rot);
        }
        this.releaseVector3f(delta);
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
        float scale2 = this.getScale();
        float radius = 0.5f * scale2;
        GL11.glEnable(2929);
        GL11.glDepthFunc(513);
        vboRenderer = VBORenderer.getInstance();
        VBORenderer.getInstance().setDepthTestForAllRuns(Boolean.FALSE);
        Matrix4f axisMatrix4f = this.allocMatrix4f();
        Axis axis2 = axis = this.trackAxis == Axis.None ? stateData.gizmoAxis : this.trackAxis;
        if (this.trackAxis == Axis.None || this.trackAxis == Axis.X) {
            r = axis == Axis.X ? 1.0f : 0.5f;
            g = 0.0f;
            b = 0.0f;
            axisMatrix4f.set(matrix4f);
            axisMatrix4f.rotateY(1.5707964f);
            this.renderAxis(axisMatrix4f, 0.01f * scale2, radius / 2.0f, r, 0.0f, 0.0f, stateData.cameraRay);
        }
        if (this.trackAxis == Axis.None || this.trackAxis == Axis.Y) {
            r = 0.0f;
            g = axis == Axis.Y ? 1.0f : 0.5f;
            b = 0.0f;
            axisMatrix4f.set(matrix4f);
            axisMatrix4f.rotateX(1.5707964f);
            this.renderAxis(axisMatrix4f, 0.01f * scale2, radius / 2.0f, 0.0f, g, 0.0f, stateData.cameraRay);
        }
        if (this.trackAxis == Axis.None || this.trackAxis == Axis.Z) {
            r = 0.0f;
            g = 0.0f;
            b = axis == Axis.Z ? 1.0f : 0.5f;
            axisMatrix4f.set(matrix4f);
            this.renderAxis(axisMatrix4f, 0.01f * scale2, radius / 2.0f, 0.0f, 0.0f, b, stateData.cameraRay);
        }
        vboRenderer.setDepthTestForAllRuns(null);
        vboRenderer.setUserDepthForAllRuns(null);
        vboRenderer.cmdPushAndMultMatrix(5888, matrix4f);
        Model.debugDrawAxis(0.0f, 0.0f, 0.0f, 0.5f, 1.0f);
        vboRenderer.cmdPopMatrix(5888);
        this.releaseMatrix4f(axisMatrix4f);
        this.releaseMatrix4f(matrix4f);
        GL11.glDepthFunc(519);
        this.renderLineToOrigin();
        GLStateRenderThread.restore();
    }

    void getCircleSegments(Vector3f center, float radius, Vector3f orthoNormal1, Vector3f orthoNormal2, ArrayList<Vector3f> out) {
        Vector3f p1 = this.allocVector3f();
        Vector3f p2 = this.allocVector3f();
        int segments = 32;
        double t = 0.0;
        double cos = Math.cos(t);
        double sin = Math.sin(t);
        orthoNormal1.mul((float)cos, p1);
        orthoNormal2.mul((float)sin, p2);
        p1.add(p2).mul(radius);
        out.add(this.allocVector3f().set(center).add(p1));
        for (int i = 1; i <= 32; ++i) {
            t = (double)i * 360.0 / 32.0 * 0.01745329238474369;
            cos = Math.cos(t);
            sin = Math.sin(t);
            orthoNormal1.mul((float)cos, p1);
            orthoNormal2.mul((float)sin, p2);
            p1.add(p2).mul(radius);
            out.add(this.allocVector3f().set(center).add(p1));
        }
        this.releaseVector3f(p1);
        this.releaseVector3f(p2);
    }

    private float hitTestCircle(UI3DScene.Ray cameraRay, ArrayList<Vector3f> circlePoints, Vector2f closestPoint) {
        UI3DScene.Ray ray = this.allocRay();
        Vector3f v = this.allocVector3f();
        float camX = this.scene.sceneToUIX(cameraRay.origin.x, cameraRay.origin.y, cameraRay.origin.z);
        float camY = this.scene.sceneToUIY(cameraRay.origin.x, cameraRay.origin.y, cameraRay.origin.z);
        float closestDist = Float.MAX_VALUE;
        Vector3f p1 = circlePoints.get(0);
        for (int i = 1; i < circlePoints.size(); ++i) {
            Vector3f p2 = circlePoints.get(i);
            float x1 = this.scene.sceneToUIX(p1.x, p1.y, p1.z);
            float y1 = this.scene.sceneToUIY(p1.x, p1.y, p1.z);
            float x2 = this.scene.sceneToUIX(p2.x, p2.y, p2.z);
            float y2 = this.scene.sceneToUIY(p2.x, p2.y, p2.z);
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
        this.releaseVector3f(v);
        RotateGizmo.releaseRay(ray);
        return closestDist;
    }

    void renderAxis(Matrix4f axisMatrix4f, float r, float c, float r1, float g1, float b1, UI3DScene.Ray cameraRay) {
        UI3DScene.Ray cameraRay2 = this.allocRay().set(cameraRay);
        axisMatrix4f.invert();
        axisMatrix4f.transformPosition(cameraRay2.origin);
        axisMatrix4f.transformDirection(cameraRay2.direction);
        axisMatrix4f.invert();
        VBORenderer vbor = VBORenderer.getInstance();
        vbor.cmdPushAndMultMatrix(5888, axisMatrix4f);
        vbor.addTorus(r, c, 8, 32, r1, g1, b1, cameraRay2);
        vbor.cmdPopMatrix(5888);
        vbor.flush();
        RotateGizmo.releaseRay(cameraRay2);
    }

    void renderAxis(Vector3f center, float radius, Vector3f orthoNormal1, Vector3f orthoNormal2, float r, float g, float b, UI3DScene.Ray cameraRay) {
        vboRenderer.flush();
        vboRenderer.setLineWidth(6.0f);
        this.getCircleSegments(center, radius, orthoNormal1, orthoNormal2, this.circlePointsRender);
        Vector3f spoke = this.allocVector3f();
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
        this.releaseVector3f(spoke);
        vboRenderer.flush();
    }

    Vector3f getPointOnAxis(float uiX, float uiY, Axis axis, UI3DScene.Circle circle, Matrix4f gizmoXfrm, Vector3f out) {
        float scale = 1.0f;
        circle.radius = 0.5f;
        gizmoXfrm.getTranslation(circle.center);
        float cx = this.scene.sceneToUIX(circle.center.x, circle.center.y, circle.center.z);
        float cy = this.scene.sceneToUIY(circle.center.x, circle.center.y, circle.center.z);
        circle.center.set(cx, cy, 0.0f);
        circle.orientation.set(0.0f, 0.0f, 1.0f);
        UI3DScene.Ray cameraRay = this.allocRay();
        cameraRay.origin.set(uiX, uiY, 0.0f);
        cameraRay.direction.set(0.0f, 0.0f, -1.0f);
        UI3DScene.closest_distance_line_circle(cameraRay, circle, out);
        RotateGizmo.releaseRay(cameraRay);
        return out;
    }

    float calculateRotation(Vector3f pp, Vector3f pc, UI3DScene.Circle circle) {
        if (pp.equals(pc)) {
            return 0.0f;
        }
        Vector3f vp = this.allocVector3f().set(pp).sub(circle.center).normalize();
        Vector3f vc = this.allocVector3f().set(pc).sub(circle.center).normalize();
        float angle = (float)Math.acos(vc.dot(vp));
        Vector3f cross = vp.cross(vc, this.allocVector3f());
        int sign = (int)Math.signum(cross.dot(circle.orientation));
        this.releaseVector3f(vp);
        this.releaseVector3f(vc);
        this.releaseVector3f(cross);
        return (float)sign * angle * 57.295776f;
    }
}

