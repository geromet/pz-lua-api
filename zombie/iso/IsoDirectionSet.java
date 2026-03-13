/*
 * Decompiled with CFR 0.152.
 */
package zombie.iso;

import zombie.UsedFromLua;
import zombie.iso.IsoDirections;

@UsedFromLua
public class IsoDirectionSet {
    public int set;

    public static IsoDirections rotate(IsoDirections dir, int amount) {
        return IsoDirections.fromIndex(dir.ordinal() + amount);
    }

    public IsoDirections getNext() {
        for (int i = 0; i < 8; ++i) {
            int bit = 1 << i;
            if ((this.set & bit) == 0) continue;
            this.set ^= bit;
            return IsoDirections.fromIndex(i);
        }
        return null;
    }
}

