/*
 * Decompiled with CFR 0.152.
 */
package zombie.core;

import java.util.LinkedHashMap;
import se.krka.kahlua.integration.LuaReturn;
import se.krka.kahlua.vm.KahluaTable;
import se.krka.kahlua.vm.KahluaTableIterator;
import se.krka.kahlua.vm.LuaClosure;
import se.krka.kahlua.vm.Prototype;
import zombie.GameTime;
import zombie.Lua.LuaManager;
import zombie.UsedFromLua;
import zombie.characters.IsoPlayer;
import zombie.core.Action;
import zombie.core.Transaction;
import zombie.core.network.ByteBufferReader;
import zombie.core.network.ByteBufferWriter;
import zombie.debug.DebugLog;
import zombie.debug.LogSeverity;
import zombie.network.GameServer;
import zombie.network.IConnection;
import zombie.network.JSONField;
import zombie.network.PZNetKahluaNull;
import zombie.network.PZNetKahluaTableImpl;

@UsedFromLua
public class NetTimedAction
extends Action {
    @JSONField
    public String type = "";
    @JSONField
    public String name = "";
    public KahluaTable action;
    @JSONField
    PZNetKahluaTableImpl actionArgs;
    @JSONField
    boolean isUsingTimeout = true;

    public void set(IsoPlayer player, KahluaTable action) {
        this.action = action;
        this.type = this.action.getMetatable().getString("Type");
        this.name = this.action.getString("name");
        this.actionArgs = new PZNetKahluaTableImpl(new LinkedHashMap<Object, Object>());
        Prototype itemNew = ((LuaClosure)this.action.getMetatable().rawget((Object)"new")).prototype;
        for (int i = 1; i < itemNew.numParams; ++i) {
            String paramName = itemNew.locvars[i];
            Object o = this.action.rawget(paramName);
            if (o == null) {
                o = PZNetKahluaNull.instance;
            }
            this.actionArgs.rawset(paramName, o);
        }
        this.isUsingTimeout = LuaManager.caller.protectedCallBoolean(LuaManager.thread, action.rawget("isUsingTimeout"), action);
        this.set(player);
    }

    @Override
    float getDuration() {
        if (this.action == null) {
            return 0.0f;
        }
        LuaReturn result = LuaManager.caller.protectedCall(LuaManager.thread, this.action.rawget("getDuration"), this.action);
        if (result.isSuccess()) {
            float duration = this.getAdjustedDuration(((Double)result.getFirst()).floatValue());
            if (duration == -1.0f) {
                return -1.0f;
            }
            return duration * 20.0f;
        }
        return 0.0f;
    }

    private float getAdjustedDuration(float initialValue) {
        if (!GameServer.server) {
            return initialValue;
        }
        LuaReturn adjustResult = LuaManager.caller.protectedCall(LuaManager.thread, this.action.rawget("adjustMaxTime"), this.action, Float.valueOf(initialValue));
        if (adjustResult.isSuccess()) {
            return ((Double)adjustResult.getFirst()).floatValue();
        }
        return initialValue;
    }

    @Override
    void start() {
        LuaReturn result;
        this.setTimeData();
        Object functionObject = this.action.rawget("serverStart");
        if (functionObject != null && !(result = LuaManager.caller.protectedCall(LuaManager.thread, functionObject, this.action)).isSuccess()) {
            DebugLog.Action.warn("Get function object \"serverStart\" failed");
        }
    }

    @Override
    void stop() {
        LuaReturn result;
        Object functionObject = this.action.rawget("serverStop");
        if (functionObject != null && !(result = LuaManager.caller.protectedCall(LuaManager.thread, functionObject, this.action)).isSuccess()) {
            DebugLog.Action.warn("Get function object \"serverStop\" failed");
        }
    }

    @Override
    boolean isValid() {
        return false;
    }

    @Override
    boolean isUsingTimeout() {
        return this.isUsingTimeout;
    }

    @Override
    void update() {
    }

    @Override
    boolean perform() {
        try {
            return LuaManager.caller.protectedCallBoolean(LuaManager.thread, this.action.rawget("complete"), this.action);
        }
        catch (Exception e) {
            DebugLog.Action.printException(e, "Perform failed", LogSeverity.Error);
            return false;
        }
    }

    @Override
    public void parse(ByteBufferReader b, IConnection connection) {
        super.parse(b, connection);
        if (this.state == Transaction.TransactionState.Request) {
            this.type = b.getUTF().trim();
            this.name = b.getUTF().trim();
            this.actionArgs = new PZNetKahluaTableImpl(new LinkedHashMap<Object, Object>());
            this.actionArgs.load(b, connection);
            Object classObject = LuaManager.get(this.type);
            Object functionObject = LuaManager.getFunctionObject(this.type + ".new");
            byte numParams = (byte)(this.actionArgs.size() + 1);
            Object[] arguments = new Object[numParams];
            int i = 1;
            arguments[0] = classObject;
            KahluaTableIterator it = this.actionArgs.iterator();
            while (it.advance()) {
                arguments[i++] = it.getValue();
            }
            LuaReturn result = LuaManager.caller.protectedCall(LuaManager.thread, functionObject, arguments);
            if (!result.isSuccess() || result.getFirst() == null) {
                this.action = null;
                return;
            }
            this.action = (KahluaTable)result.getFirst();
            this.action.rawset("name", (Object)this.name);
            this.action.rawset("netAction", (Object)this);
        }
    }

    public void forceComplete() {
        this.endTime = GameTime.getServerTimeMills();
    }

    @Override
    public void write(ByteBufferWriter b) {
        super.write(b);
        if (this.state == Transaction.TransactionState.Request) {
            b.putUTF(this.type);
            b.putUTF(this.name);
            this.actionArgs.save(b.bb);
        }
    }

    public void animEvent(String event, String parameter) {
        Object functionObject = this.action.rawget("animEvent");
        if (functionObject != null) {
            LuaManager.caller.protectedCallVoid(LuaManager.thread, functionObject, this.action, event, parameter);
        }
    }
}

