/*
 * Decompiled with CFR 0.152.
 */
package zombie.iso.zones;

import se.krka.kahlua.vm.KahluaTable;
import zombie.UsedFromLua;
import zombie.iso.IsoDirections;
import zombie.iso.zones.Zone;

@UsedFromLua
public final class VehicleZone
extends Zone {
    public static final short VZF_FaceDirection = 1;
    public IsoDirections dir;
    public short flags;

    public VehicleZone(String name, String type, int x, int y, int z, int w, int h, KahluaTable properties) {
        super(name, type, x, y, z, w, h);
        if (properties != null) {
            Object o = properties.rawget("Direction");
            if (o instanceof String) {
                String s = (String)o;
                this.dir = IsoDirections.valueOf(s);
            }
            if ((o = properties.rawget("FaceDirection")) == Boolean.TRUE) {
                this.flags = (short)(this.flags | 1);
            }
        }
    }

    public boolean isFaceDirection() {
        return (this.flags & 1) != 0;
    }
}

