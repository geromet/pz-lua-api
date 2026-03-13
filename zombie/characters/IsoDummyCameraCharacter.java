/*
 * Decompiled with CFR 0.152.
 */
package zombie.characters;

import zombie.UsedFromLua;
import zombie.characters.IsoGameCharacter;
import zombie.iso.IsoCamera;

@UsedFromLua
public final class IsoDummyCameraCharacter
extends IsoGameCharacter {
    public IsoDummyCameraCharacter(float x, float y, float z) {
        super(null, x, y, z);
        IsoCamera.setCameraCharacter(this);
    }

    @Override
    public void update() {
    }
}

