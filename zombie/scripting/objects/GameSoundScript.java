/*
 * Decompiled with CFR 0.152.
 */
package zombie.scripting.objects;

import zombie.UsedFromLua;
import zombie.audio.GameSound;
import zombie.audio.GameSoundClip;
import zombie.core.math.PZMath;
import zombie.scripting.ScriptParser;
import zombie.scripting.ScriptType;
import zombie.scripting.objects.BaseScriptObject;

@UsedFromLua
public final class GameSoundScript
extends BaseScriptObject {
    public final GameSound gameSound = new GameSound();

    public GameSoundScript() {
        super(ScriptType.Sound);
    }

    @Override
    public void Load(String name, String totalFile) throws Exception {
        this.gameSound.name = name;
        ScriptParser.Block block = ScriptParser.parse(totalFile);
        block = block.children.get(0);
        this.LoadCommonBlock(block);
        for (ScriptParser.Value value : block.values) {
            String[] ss = value.string.split("=");
            String k = ss[0].trim();
            String v = ss[1].trim();
            if ("category".equals(k)) {
                this.gameSound.category = v;
                continue;
            }
            if ("is3D".equals(k)) {
                this.gameSound.is3d = Boolean.parseBoolean(v);
                continue;
            }
            if ("loop".equals(k)) {
                this.gameSound.loop = Boolean.parseBoolean(v);
                continue;
            }
            if ("master".equals(k)) {
                this.gameSound.master = GameSound.MasterVolume.valueOf(v);
                continue;
            }
            if (!"maxInstancesPerEmitter".equals(k)) continue;
            this.gameSound.maxInstancesPerEmitter = PZMath.tryParseInt(v, -1);
        }
        for (ScriptParser.Block child : block.children) {
            if (!"clip".equals(child.type)) continue;
            GameSoundClip clip = this.LoadClip(child);
            this.gameSound.clips.add(clip);
        }
    }

    private GameSoundClip LoadClip(ScriptParser.Block block) {
        GameSoundClip clip = new GameSoundClip(this.gameSound);
        for (ScriptParser.Value value : block.values) {
            String[] ss = value.string.split("=");
            String k = ss[0].trim();
            String v = ss[1].trim();
            if ("distanceMax".equals(k)) {
                clip.distanceMax = Integer.parseInt(v);
                clip.initFlags = (short)(clip.initFlags | 2);
                continue;
            }
            if ("distanceMin".equals(k)) {
                clip.distanceMin = Integer.parseInt(v);
                clip.initFlags = (short)(clip.initFlags | 1);
                continue;
            }
            if ("event".equals(k)) {
                clip.event = v;
                continue;
            }
            if ("file".equals(k)) {
                clip.file = v;
                continue;
            }
            if ("pitch".equals(k)) {
                clip.pitch = Float.parseFloat(v);
                continue;
            }
            if ("volume".equals(k)) {
                clip.volume = Float.parseFloat(v);
                continue;
            }
            if ("reverbFactor".equals(k)) {
                clip.reverbFactor = Float.parseFloat(v);
                continue;
            }
            if ("reverbMaxRange".equals(k)) {
                clip.reverbMaxRange = Float.parseFloat(v);
                continue;
            }
            if (!"stopImmediate".equals(k)) continue;
            clip.initFlags = (short)(clip.initFlags | 4);
        }
        return clip;
    }

    @Override
    public void reset() {
        this.gameSound.reset();
    }
}

