/*
 * Decompiled with CFR 0.152.
 */
package zombie.core.physics;

import java.util.ArrayList;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import zombie.asset.Asset;
import zombie.asset.AssetManager;
import zombie.asset.AssetPath;
import zombie.asset.AssetType;
import zombie.core.skinnedmodel.model.jassimp.ProcessedAiScene;

public final class PhysicsShape
extends Asset {
    public PhysicsShapeAssetParams assetParams;
    public ArrayList<OneMesh> meshes = new ArrayList();
    public String postProcess;
    public boolean allMeshes;
    public int modificationCount;
    public String fullPath;
    public static final AssetType ASSET_TYPE = new AssetType("PhysicsShape");

    public PhysicsShape(AssetPath path, AssetManager manager, PhysicsShapeAssetParams params) {
        super(path, manager);
        this.assetParams = params;
        this.postProcess = this.assetParams == null ? null : this.assetParams.postProcess;
        this.allMeshes = this.assetParams == null ? false : this.assetParams.allMeshes;
    }

    protected void onLoadedX(ProcessedAiScene processedAiScene) {
        for (OneMesh oneMesh : this.meshes) {
            oneMesh.reset();
        }
        this.meshes.clear();
        processedAiScene.applyToPhysicsShape(this);
        ++this.modificationCount;
    }

    @Override
    public void onBeforeReady() {
        super.onBeforeReady();
        if (this.assetParams != null) {
            this.assetParams = null;
        }
    }

    @Override
    public void setAssetParams(AssetManager.AssetParams params) {
        this.assetParams = (PhysicsShapeAssetParams)params;
    }

    @Override
    public AssetType getType() {
        return ASSET_TYPE;
    }

    public static final class PhysicsShapeAssetParams
    extends AssetManager.AssetParams {
        public String postProcess;
        public boolean allMeshes;
    }

    public static final class OneMesh {
        public final Vector3f minXyz = new Vector3f(Float.MAX_VALUE);
        public final Vector3f maxXyz = new Vector3f(-3.4028235E38f);
        public Matrix4f transform;
        public float[] points;

        public void reset() {
            this.minXyz.set(Float.MAX_VALUE);
            this.maxXyz.set(-3.4028235E38f);
            this.transform = null;
            this.points = null;
        }
    }
}

