/*
 * Decompiled with CFR 0.152.
 */
package zombie.scripting.objects;

import java.util.ArrayList;
import zombie.UsedFromLua;
import zombie.core.skinnedmodel.model.ModelMesh;
import zombie.scripting.ScriptParser;
import zombie.scripting.ScriptType;
import zombie.scripting.objects.BaseScriptObject;
import zombie.util.StringUtils;

@UsedFromLua
public final class AnimationsMesh
extends BaseScriptObject {
    public String meshFile;
    public boolean keepMeshAnimations;
    public String postProcess;
    public final ArrayList<String> animationDirectories = new ArrayList();
    public final ArrayList<String> animationPrefixes = new ArrayList();
    public ModelMesh modelMesh;

    public AnimationsMesh() {
        super(ScriptType.AnimationMesh);
    }

    @Override
    public void Load(String name, String totalFile) throws Exception {
        ScriptParser.Block block = ScriptParser.parse(totalFile);
        block = block.children.get(0);
        this.LoadCommonBlock(block);
        for (ScriptParser.Value value : block.values) {
            String k = value.getKey().trim();
            String v = value.getValue().trim();
            if ("meshFile".equalsIgnoreCase(k)) {
                this.meshFile = v;
                continue;
            }
            if ("animationDirectory".equalsIgnoreCase(k)) {
                this.animationDirectories.add(v);
                continue;
            }
            if ("animationPrefix".equalsIgnoreCase(k)) {
                this.animationPrefixes.add(v);
                continue;
            }
            if ("keepMeshAnimations".equalsIgnoreCase(k)) {
                this.keepMeshAnimations = StringUtils.tryParseBoolean(v);
                continue;
            }
            if (!"postProcess".equalsIgnoreCase(k)) continue;
            this.postProcess = v;
        }
    }

    @Override
    public void reset() {
        this.meshFile = null;
        this.animationDirectories.clear();
        this.animationPrefixes.clear();
        this.modelMesh = null;
    }
}

