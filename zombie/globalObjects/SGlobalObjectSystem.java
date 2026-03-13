/*
 * Decompiled with CFR 0.152.
 */
package zombie.globalObjects;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.HashSet;
import se.krka.kahlua.vm.KahluaTable;
import se.krka.kahlua.vm.KahluaTableIterator;
import zombie.Lua.LuaManager;
import zombie.UsedFromLua;
import zombie.ZomboidFileSystem;
import zombie.characters.IsoPlayer;
import zombie.core.BoxedStaticValues;
import zombie.core.Core;
import zombie.core.logger.ExceptionLogger;
import zombie.globalObjects.GlobalObject;
import zombie.globalObjects.GlobalObjectSystem;
import zombie.globalObjects.SGlobalObject;
import zombie.globalObjects.SGlobalObjectNetwork;
import zombie.iso.IsoObject;
import zombie.iso.SliceY;
import zombie.network.GameClient;
import zombie.util.Type;

@UsedFromLua
public final class SGlobalObjectSystem
extends GlobalObjectSystem {
    private static KahluaTable tempTable;
    protected int loadedWorldVersion = -1;
    protected final HashSet<String> modDataKeys = new HashSet();
    protected final HashSet<String> objectModDataKeys = new HashSet();
    protected final HashSet<String> objectSyncKeys = new HashSet();

    public SGlobalObjectSystem(String name) {
        super(name);
    }

    @Override
    protected GlobalObject makeObject(int x, int y, int z) {
        return new SGlobalObject(this, x, y, z);
    }

    public void setModDataKeys(KahluaTable keys2) {
        this.modDataKeys.clear();
        if (keys2 == null) {
            return;
        }
        KahluaTableIterator iterator2 = keys2.iterator();
        while (iterator2.advance()) {
            Object value = iterator2.getValue();
            if (!(value instanceof String)) {
                throw new IllegalArgumentException("expected string but got \"" + String.valueOf(value) + "\"");
            }
            String s = (String)value;
            this.modDataKeys.add(s);
        }
    }

    public void setObjectModDataKeys(KahluaTable keys2) {
        this.objectModDataKeys.clear();
        if (keys2 == null) {
            return;
        }
        KahluaTableIterator iterator2 = keys2.iterator();
        while (iterator2.advance()) {
            Object value = iterator2.getValue();
            if (!(value instanceof String)) {
                throw new IllegalArgumentException("expected string but got \"" + String.valueOf(value) + "\"");
            }
            String s = (String)value;
            this.objectModDataKeys.add(s);
        }
    }

    public void setObjectSyncKeys(KahluaTable keys2) {
        this.objectSyncKeys.clear();
        if (keys2 == null) {
            return;
        }
        KahluaTableIterator iterator2 = keys2.iterator();
        while (iterator2.advance()) {
            Object value = iterator2.getValue();
            if (!(value instanceof String)) {
                throw new IllegalArgumentException("expected string but got \"" + String.valueOf(value) + "\"");
            }
            String s = (String)value;
            this.objectSyncKeys.add(s);
        }
    }

    public void update() {
    }

    public void chunkLoaded(int wx, int wy) {
        if (!this.hasObjectsInChunk(wx, wy)) {
            return;
        }
        Object function = this.modData.rawget("OnChunkLoaded");
        if (function == null) {
            throw new IllegalStateException("OnChunkLoaded method undefined for system '" + this.name + "'");
        }
        Double dwx = BoxedStaticValues.toDouble(wx);
        Double dwy = BoxedStaticValues.toDouble(wy);
        LuaManager.caller.pcall(LuaManager.thread, function, this.modData, dwx, dwy);
    }

    public void sendCommand(String command, KahluaTable args2) {
        SGlobalObjectNetwork.sendServerCommand(this.name, command, args2);
    }

    public void receiveClientCommand(String command, IsoPlayer playerObj, KahluaTable args2) {
        Object function = this.modData.rawget("OnClientCommand");
        if (function == null) {
            throw new IllegalStateException("OnClientCommand method undefined for system '" + this.name + "'");
        }
        LuaManager.caller.pcall(LuaManager.thread, function, this.modData, command, playerObj, args2);
    }

    public void addGlobalObjectOnClient(SGlobalObject globalObject) throws IOException {
        if (globalObject == null) {
            throw new IllegalArgumentException("globalObject is null");
        }
        if (globalObject.system != this) {
            throw new IllegalArgumentException("object not in this system");
        }
        SGlobalObjectNetwork.addGlobalObjectOnClient(globalObject);
    }

    public void removeGlobalObjectOnClient(SGlobalObject globalObject) throws IOException {
        if (globalObject == null) {
            throw new IllegalArgumentException("globalObject is null");
        }
        if (globalObject.system != this) {
            throw new IllegalArgumentException("object not in this system");
        }
        SGlobalObjectNetwork.removeGlobalObjectOnClient(globalObject);
    }

    public void updateGlobalObjectOnClient(SGlobalObject globalObject) throws IOException {
        if (globalObject == null) {
            throw new IllegalArgumentException("globalObject is null");
        }
        if (globalObject.system != this) {
            throw new IllegalArgumentException("object not in this system");
        }
        SGlobalObjectNetwork.updateGlobalObjectOnClient(globalObject);
    }

    private String getFileName() {
        return ZomboidFileSystem.instance.getFileNameInCurrentSave("gos_" + this.name + ".bin");
    }

    public KahluaTable getInitialStateForClient() {
        Object function = this.modData.rawget("getInitialStateForClient");
        if (function == null) {
            throw new IllegalStateException("getInitialStateForClient method undefined for system '" + this.name + "'");
        }
        Object[] result = LuaManager.caller.pcall(LuaManager.thread, function, (Object)this.modData);
        if (result != null && result[0].equals(Boolean.TRUE) && result[1] instanceof KahluaTable) {
            return (KahluaTable)result[1];
        }
        return null;
    }

    public void OnIsoObjectChangedItself(IsoObject isoObject) {
        GlobalObject globalObject = this.getObjectAt(isoObject.getSquare().x, isoObject.getSquare().y, isoObject.getSquare().z);
        if (globalObject == null) {
            return;
        }
        Object function = this.modData.rawget("OnIsoObjectChangedItself");
        if (function == null) {
            throw new IllegalStateException("OnIsoObjectChangedItself method undefined for system '" + this.name + "'");
        }
        LuaManager.caller.pcall(LuaManager.thread, function, this.modData, isoObject);
    }

    public void OnModDataChangeItself(IsoObject isoObject) {
        GlobalObject globalObject = this.getObjectAt(isoObject.getSquare().x, isoObject.getSquare().y, isoObject.getSquare().z);
        if (globalObject == null) {
            return;
        }
        Object function = this.modData.rawget("OnModDataChangeItself");
        if (function == null) {
            throw new IllegalStateException("OnModDataChangeItself method undefined for system '" + this.name + "'");
        }
        LuaManager.caller.pcall(LuaManager.thread, function, this.modData, isoObject);
    }

    public int loadedWorldVersion() {
        return this.loadedWorldVersion;
    }

    public void load(ByteBuffer bb, int worldVersion) throws IOException {
        if (bb.get() != 0) {
            this.modData.load(bb, worldVersion);
        }
        int count = bb.getInt();
        for (int i = 0; i < count; ++i) {
            int x = bb.getInt();
            int y = bb.getInt();
            byte z = bb.get();
            SGlobalObject object = Type.tryCastTo(this.newObject(x, y, z), SGlobalObject.class);
            object.load(bb, worldVersion);
        }
        this.loadedWorldVersion = worldVersion;
    }

    public void save(ByteBuffer bb) throws IOException {
        if (tempTable == null) {
            tempTable = LuaManager.platform.newTable();
        }
        tempTable.wipe();
        KahluaTableIterator iterator2 = this.modData.iterator();
        while (iterator2.advance()) {
            Object key = iterator2.getKey();
            if (!this.modDataKeys.contains(key)) continue;
            tempTable.rawset(key, this.modData.rawget(key));
        }
        if (tempTable.isEmpty()) {
            bb.put((byte)0);
        } else {
            bb.put((byte)1);
            tempTable.save(bb);
        }
        bb.putInt(this.objects.size());
        for (int i = 0; i < this.objects.size(); ++i) {
            SGlobalObject object = Type.tryCastTo((GlobalObject)this.objects.get(i), SGlobalObject.class);
            object.save(bb);
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public void load() {
        File file = new File(this.getFileName());
        try (FileInputStream fis2 = new FileInputStream(file);
             BufferedInputStream bis = new BufferedInputStream(fis2);){
            Object object = SliceY.SliceBufferLock;
            synchronized (object) {
                int worldVersion;
                ByteBuffer bb = SliceY.SliceBuffer;
                bb.clear();
                int numBytes = bis.read(bb.array());
                bb.limit(numBytes);
                byte b1 = bb.get();
                byte b2 = bb.get();
                byte b3 = bb.get();
                byte b4 = bb.get();
                if (b1 == 71 && b2 == 76 && b3 == 79 && b4 == 83) {
                    worldVersion = bb.getInt();
                    if (worldVersion > 244) {
                        throw new IOException("file is from a newer version " + worldVersion + " of the game: " + file.getAbsolutePath());
                    }
                } else {
                    throw new IOException("doesn't appear to be a GlobalObjectSystem file:" + file.getAbsolutePath());
                }
                this.load(bb, worldVersion);
            }
        }
        catch (FileNotFoundException fis2) {
        }
        catch (Throwable t) {
            ExceptionLogger.logException(t);
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public void save() {
        if (Core.getInstance().isNoSave()) {
            return;
        }
        if (GameClient.client) {
            return;
        }
        File file = new File(this.getFileName());
        try (FileOutputStream fos = new FileOutputStream(file);
             BufferedOutputStream bos = new BufferedOutputStream(fos);){
            Object object = SliceY.SliceBufferLock;
            synchronized (object) {
                ByteBuffer bb = SliceY.SliceBuffer;
                bb.clear();
                bb.put((byte)71);
                bb.put((byte)76);
                bb.put((byte)79);
                bb.put((byte)83);
                bb.putInt(244);
                this.save(bb);
                bos.write(bb.array(), 0, bb.position());
            }
        }
        catch (Throwable t) {
            ExceptionLogger.logException(t);
        }
    }

    @Override
    public void Reset() {
        super.Reset();
        this.modDataKeys.clear();
        this.objectModDataKeys.clear();
        this.objectSyncKeys.clear();
    }
}

