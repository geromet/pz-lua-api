/*
 * Decompiled with CFR 0.152.
 */
package zombie.radio.StorySounds;

import zombie.UsedFromLua;
import zombie.characters.IsoPlayer;
import zombie.iso.Vector2;
import zombie.radio.StorySounds.SLSoundManager;

@UsedFromLua
public final class StorySound {
    private String name;
    private float baseVolume;

    public StorySound(String name, float baseVol) {
        this.name = name;
        this.baseVolume = baseVol;
    }

    public long playSound() {
        Vector2 pos = SLSoundManager.getInstance().getRandomBorderPosition();
        return SLSoundManager.emitter.playSound(this.name, this.baseVolume, pos.x, pos.y, 0.0f, 100.0f, SLSoundManager.getInstance().getRandomBorderRange());
    }

    public long playSound(float volumeOverride) {
        return SLSoundManager.emitter.playSound(this.name, volumeOverride, IsoPlayer.getInstance().getX(), IsoPlayer.getInstance().getY(), IsoPlayer.getInstance().getZ(), 10.0f, 50.0f);
    }

    public long playSound(float x, float y, float z, float minRange, float maxRange) {
        return this.playSound(this.baseVolume, x, y, z, minRange, maxRange);
    }

    public long playSound(float volumeMod, float x, float y, float z, float minRange, float maxRange) {
        return SLSoundManager.emitter.playSound(this.name, this.baseVolume * volumeMod, x, y, z, minRange, maxRange);
    }

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public float getBaseVolume() {
        return this.baseVolume;
    }

    public void setBaseVolume(float baseVolume) {
        this.baseVolume = baseVolume;
    }

    public StorySound getClone() {
        return new StorySound(this.name, this.baseVolume);
    }
}

