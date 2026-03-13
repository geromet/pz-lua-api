/*
 * Decompiled with CFR 0.152.
 */
package zombie.globalObjects;

import java.io.IOException;
import java.nio.ByteBuffer;
import se.krka.kahlua.vm.KahluaTable;
import se.krka.kahlua.vm.KahluaTableIterator;
import zombie.Lua.LuaManager;
import zombie.UsedFromLua;
import zombie.globalObjects.GlobalObject;
import zombie.globalObjects.SGlobalObjectSystem;

@UsedFromLua
public final class SGlobalObject
extends GlobalObject {
    private static KahluaTable tempTable;

    SGlobalObject(SGlobalObjectSystem system, int x, int y, int z) {
        super(system, x, y, z);
    }

    public void load(ByteBuffer bb, int worldVersion) throws IOException {
        if (bb.get() != 0) {
            this.modData.load(bb, worldVersion);
        }
    }

    public void save(ByteBuffer bb) throws IOException {
        bb.putInt(this.x);
        bb.putInt(this.y);
        bb.put((byte)this.z);
        if (tempTable == null) {
            tempTable = LuaManager.platform.newTable();
        }
        tempTable.wipe();
        KahluaTableIterator iterator2 = this.modData.iterator();
        while (iterator2.advance()) {
            Object key = iterator2.getKey();
            if (!((SGlobalObjectSystem)this.system).objectModDataKeys.contains(key)) continue;
            tempTable.rawset(key, this.modData.rawget(key));
        }
        if (tempTable.isEmpty()) {
            bb.put((byte)0);
        } else {
            bb.put((byte)1);
            tempTable.save(bb);
            tempTable.wipe();
        }
    }
}

