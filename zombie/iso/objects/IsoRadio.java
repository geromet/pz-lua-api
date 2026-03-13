/*
 * Decompiled with CFR 0.152.
 */
package zombie.iso.objects;

import zombie.UsedFromLua;
import zombie.iso.IsoCell;
import zombie.iso.IsoGridSquare;
import zombie.iso.objects.IsoWaveSignal;
import zombie.iso.sprite.IsoSprite;

@UsedFromLua
public class IsoRadio
extends IsoWaveSignal {
    public IsoRadio(IsoCell cell) {
        super(cell);
    }

    public IsoRadio(IsoCell cell, IsoGridSquare sq, IsoSprite spr) {
        super(cell, sq, spr);
    }

    @Override
    public String getObjectName() {
        return "Radio";
    }
}

