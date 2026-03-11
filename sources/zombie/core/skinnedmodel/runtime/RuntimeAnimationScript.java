/*
 * Decompiled with CFR 0.152.
 */
package zombie.core.skinnedmodel.runtime;

import java.util.ArrayList;
import zombie.UsedFromLua;
import zombie.core.skinnedmodel.ModelManager;
import zombie.core.skinnedmodel.animation.AnimationClip;
import zombie.core.skinnedmodel.animation.Keyframe;
import zombie.core.skinnedmodel.runtime.CopyFrame;
import zombie.core.skinnedmodel.runtime.CopyFrames;
import zombie.core.skinnedmodel.runtime.IRuntimeAnimationCommand;
import zombie.scripting.ScriptParser;
import zombie.scripting.ScriptType;
import zombie.scripting.objects.BaseScriptObject;

@UsedFromLua
public final class RuntimeAnimationScript
extends BaseScriptObject {
    protected String name = this.toString();
    protected final ArrayList<IRuntimeAnimationCommand> commands = new ArrayList();

    public RuntimeAnimationScript() {
        super(ScriptType.RuntimeAnimation);
    }

    @Override
    public void Load(String name, String totalFile) throws Exception {
        this.name = name;
        ScriptParser.Block block = ScriptParser.parse(totalFile);
        block = block.children.get(0);
        this.LoadCommonBlock(block);
        for (ScriptParser.Value value : block.values) {
            String k = value.getKey().trim();
            String v = value.getValue().trim();
            if (!"xxx".equals(k)) continue;
        }
        for (ScriptParser.Block child : block.children) {
            IRuntimeAnimationCommand cmd;
            if ("CopyFrame".equals(child.type)) {
                cmd = new CopyFrame();
                ((CopyFrame)cmd).parse(child);
                this.commands.add(cmd);
                continue;
            }
            if (!"CopyFrames".equals(child.type)) continue;
            cmd = new CopyFrames();
            ((CopyFrames)cmd).parse(child);
            this.commands.add(cmd);
        }
    }

    public void exec() {
        ArrayList<Keyframe> keyframes = new ArrayList<Keyframe>();
        for (IRuntimeAnimationCommand cmd : this.commands) {
            cmd.exec(keyframes);
        }
        float duration = 0.0f;
        for (int i = 0; i < keyframes.size(); ++i) {
            duration = Math.max(duration, ((Keyframe)keyframes.get((int)i)).time);
        }
        AnimationClip clip = new AnimationClip(duration, keyframes, this.name, true);
        keyframes.clear();
        ModelManager.instance.addAnimationClip(clip.name, clip);
        keyframes.clear();
    }

    @Override
    public void reset() {
        this.name = this.toString();
        this.commands.clear();
    }
}

