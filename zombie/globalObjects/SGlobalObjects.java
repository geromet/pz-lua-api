/*
 * Decompiled with CFR 0.152.
 */
package zombie.globalObjects;

import java.io.IOException;
import java.util.ArrayList;
import se.krka.kahlua.vm.KahluaTable;
import zombie.Lua.LuaEventManager;
import zombie.Lua.LuaManager;
import zombie.UsedFromLua;
import zombie.characters.IsoPlayer;
import zombie.core.BoxedStaticValues;
import zombie.core.Core;
import zombie.core.logger.ExceptionLogger;
import zombie.core.network.ByteBufferWriter;
import zombie.debug.DebugLog;
import zombie.globalObjects.CGlobalObjects;
import zombie.globalObjects.GlobalObject;
import zombie.globalObjects.GlobalObjectLookup;
import zombie.globalObjects.SGlobalObjectSystem;
import zombie.iso.IsoObject;
import zombie.iso.SliceY;
import zombie.network.GameClient;
import zombie.network.GameServer;
import zombie.network.TableNetworkUtils;

@UsedFromLua
public final class SGlobalObjects {
    protected static final ArrayList<SGlobalObjectSystem> systems = new ArrayList();

    public static void noise(String message) {
        if (Core.debug) {
            DebugLog.log("SGlobalObjects: " + message);
        }
    }

    public static SGlobalObjectSystem registerSystem(String name) {
        SGlobalObjectSystem system = SGlobalObjects.getSystemByName(name);
        if (system == null) {
            system = SGlobalObjects.newSystem(name);
            system.load();
        }
        return system;
    }

    public static SGlobalObjectSystem newSystem(String name) throws IllegalStateException {
        if (SGlobalObjects.getSystemByName(name) != null) {
            throw new IllegalStateException("system with that name already exists");
        }
        SGlobalObjects.noise("newSystem " + name);
        SGlobalObjectSystem system = new SGlobalObjectSystem(name);
        systems.add(system);
        return system;
    }

    public static int getSystemCount() {
        return systems.size();
    }

    public static SGlobalObjectSystem getSystemByIndex(int index) {
        if (index < 0 || index >= systems.size()) {
            return null;
        }
        return systems.get(index);
    }

    public static SGlobalObjectSystem getSystemByName(String name) {
        for (int i = 0; i < systems.size(); ++i) {
            SGlobalObjectSystem system = systems.get(i);
            if (!system.name.equals(name)) continue;
            return system;
        }
        return null;
    }

    public static void update() {
        for (int i = 0; i < systems.size(); ++i) {
            SGlobalObjectSystem system = systems.get(i);
            system.update();
        }
    }

    public static void chunkLoaded(int wx, int wy) {
        for (int i = 0; i < systems.size(); ++i) {
            SGlobalObjectSystem system = systems.get(i);
            system.chunkLoaded(wx, wy);
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public static void initSystems() {
        if (GameClient.client) {
            return;
        }
        LuaEventManager.triggerEvent("OnSGlobalObjectSystemInit");
        if (GameServer.server) {
            return;
        }
        try {
            Object object = SliceY.SliceBufferLock;
            synchronized (object) {
                SliceY.SliceBuffer.clear();
                SGlobalObjects.saveInitialStateForClient(SliceY.sliceBufferWriter);
                SliceY.SliceBuffer.flip();
                CGlobalObjects.loadInitialState(SliceY.sliceBufferReader);
            }
        }
        catch (Throwable t) {
            ExceptionLogger.logException(t);
        }
    }

    public static void saveInitialStateForClient(ByteBufferWriter bb) throws IOException {
        bb.putByte(systems.size());
        for (int i = 0; i < systems.size(); ++i) {
            SGlobalObjectSystem system = systems.get(i);
            bb.putUTF(system.name);
            KahluaTable tbl = system.getInitialStateForClient();
            if (tbl == null) {
                tbl = LuaManager.platform.newTable();
            }
            KahluaTable objectsTable = LuaManager.platform.newTable();
            tbl.rawset("_objects", (Object)objectsTable);
            for (int j = 0; j < system.getObjectCount(); ++j) {
                GlobalObject globalObject = system.getObjectByIndex(j);
                KahluaTable objTable = LuaManager.platform.newTable();
                objTable.rawset("x", (Object)BoxedStaticValues.toDouble(globalObject.getX()));
                objTable.rawset("y", (Object)BoxedStaticValues.toDouble(globalObject.getY()));
                objTable.rawset("z", (Object)BoxedStaticValues.toDouble(globalObject.getZ()));
                for (String key : system.objectSyncKeys) {
                    objTable.rawset(key, globalObject.getModData().rawget(key));
                }
                objectsTable.rawset(j + 1, (Object)objTable);
            }
            if (!bb.putBoolean(!tbl.isEmpty())) continue;
            TableNetworkUtils.save(tbl, bb);
        }
    }

    public static boolean receiveClientCommand(String systemName, String command, IsoPlayer playerObj, KahluaTable args2) {
        SGlobalObjects.noise("receiveClientCommand " + systemName + " " + command + " OnlineID=" + playerObj.getOnlineID());
        SGlobalObjectSystem system = SGlobalObjects.getSystemByName(systemName);
        if (system == null) {
            throw new IllegalStateException("system '" + systemName + "' not found");
        }
        system.receiveClientCommand(command, playerObj, args2);
        return true;
    }

    public static void load() {
    }

    public static void save() {
        for (int i = 0; i < systems.size(); ++i) {
            SGlobalObjectSystem system = systems.get(i);
            system.save();
        }
    }

    public static void OnIsoObjectChangedItself(String systemName, IsoObject isoObject) {
        if (GameClient.client) {
            return;
        }
        SGlobalObjectSystem system = SGlobalObjects.getSystemByName(systemName);
        if (system == null) {
            return;
        }
        system.OnIsoObjectChangedItself(isoObject);
    }

    public static void OnModDataChangeItself(String systemName, IsoObject isoObject) {
        if (GameClient.client || isoObject == null) {
            return;
        }
        SGlobalObjectSystem system = SGlobalObjects.getSystemByName(systemName);
        if (system == null) {
            return;
        }
        system.OnModDataChangeItself(isoObject);
    }

    public static void Reset() {
        for (int i = 0; i < systems.size(); ++i) {
            SGlobalObjectSystem system = systems.get(i);
            system.Reset();
        }
        systems.clear();
        GlobalObjectLookup.Reset();
    }
}

