/*
 * Decompiled with CFR 0.152.
 */
package zombie.globalObjects;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import se.krka.kahlua.vm.KahluaTable;
import se.krka.kahlua.vm.KahluaTableIterator;
import zombie.Lua.LuaEventManager;
import zombie.Lua.LuaManager;
import zombie.UsedFromLua;
import zombie.core.Core;
import zombie.core.network.ByteBufferReader;
import zombie.debug.DebugLog;
import zombie.globalObjects.CGlobalObject;
import zombie.globalObjects.CGlobalObjectNetwork;
import zombie.globalObjects.CGlobalObjectSystem;
import zombie.network.TableNetworkUtils;
import zombie.util.Type;

@UsedFromLua
public final class CGlobalObjects {
    protected static final ArrayList<CGlobalObjectSystem> systems = new ArrayList();
    protected static final HashMap<String, KahluaTable> initialState = new HashMap();

    public static void noise(String message) {
        if (Core.debug) {
            DebugLog.log("CGlobalObjects: " + message);
        }
    }

    public static CGlobalObjectSystem registerSystem(String name) {
        CGlobalObjectSystem system = CGlobalObjects.getSystemByName(name);
        if (system == null) {
            system = CGlobalObjects.newSystem(name);
            KahluaTable tbl = initialState.get(name);
            if (tbl != null) {
                KahluaTableIterator iterator2 = tbl.iterator();
                while (iterator2.advance()) {
                    Object key = iterator2.getKey();
                    Object value = iterator2.getValue();
                    if ("_objects".equals(key)) {
                        KahluaTable objectsTable = Type.tryCastTo(value, KahluaTable.class);
                        int n = objectsTable.len();
                        for (int i = 1; i <= n; ++i) {
                            KahluaTable objTable = Type.tryCastTo(objectsTable.rawget(i), KahluaTable.class);
                            int x = ((Double)objTable.rawget("x")).intValue();
                            int y = ((Double)objTable.rawget("y")).intValue();
                            int z = ((Double)objTable.rawget("z")).intValue();
                            objTable.rawset("x", null);
                            objTable.rawset("y", null);
                            objTable.rawset("z", null);
                            CGlobalObject object = Type.tryCastTo(system.newObject(x, y, z), CGlobalObject.class);
                            KahluaTableIterator it = objTable.iterator();
                            while (it.advance()) {
                                object.getModData().rawset(it.getKey(), it.getValue());
                            }
                        }
                        objectsTable.wipe();
                        continue;
                    }
                    system.modData.rawset(key, value);
                }
            }
        }
        return system;
    }

    public static CGlobalObjectSystem newSystem(String name) throws IllegalStateException {
        if (CGlobalObjects.getSystemByName(name) != null) {
            throw new IllegalStateException("system with that name already exists");
        }
        CGlobalObjects.noise("newSystem " + name);
        CGlobalObjectSystem system = new CGlobalObjectSystem(name);
        systems.add(system);
        return system;
    }

    public static int getSystemCount() {
        return systems.size();
    }

    public static CGlobalObjectSystem getSystemByIndex(int index) {
        if (index < 0 || index >= systems.size()) {
            return null;
        }
        return systems.get(index);
    }

    public static CGlobalObjectSystem getSystemByName(String name) {
        for (int i = 0; i < systems.size(); ++i) {
            CGlobalObjectSystem system = systems.get(i);
            if (!system.name.equals(name)) continue;
            return system;
        }
        return null;
    }

    public static void initSystems() {
        LuaEventManager.triggerEvent("OnCGlobalObjectSystemInit");
    }

    public static void loadInitialState(ByteBufferReader bb) throws IOException {
        int count = bb.getByte();
        for (int i = 0; i < count; ++i) {
            String systemName = bb.getUTF();
            if (!bb.getBoolean()) continue;
            KahluaTable tbl = LuaManager.platform.newTable();
            initialState.put(systemName, tbl);
            TableNetworkUtils.load(tbl, bb);
        }
    }

    public static boolean receiveServerCommand(String systemName, String command, KahluaTable args2) {
        CGlobalObjectSystem system = CGlobalObjects.getSystemByName(systemName);
        if (system == null) {
            throw new IllegalStateException("system '" + systemName + "' not found");
        }
        system.receiveServerCommand(command, args2);
        return true;
    }

    public static void Reset() {
        for (int i = 0; i < systems.size(); ++i) {
            CGlobalObjectSystem system = systems.get(i);
            system.Reset();
        }
        systems.clear();
        initialState.clear();
        CGlobalObjectNetwork.Reset();
    }
}

