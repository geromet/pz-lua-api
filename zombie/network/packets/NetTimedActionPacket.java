/*
 * Decompiled with CFR 0.152.
 */
package zombie.network.packets;

import se.krka.kahlua.integration.LuaReturn;
import se.krka.kahlua.vm.KahluaTable;
import zombie.Lua.LuaManager;
import zombie.UsedFromLua;
import zombie.characters.Capability;
import zombie.characters.IsoPlayer;
import zombie.core.ActionManager;
import zombie.core.NetTimedAction;
import zombie.core.Transaction;
import zombie.core.network.ByteBufferWriter;
import zombie.core.raknet.UdpConnection;
import zombie.debug.DebugLog;
import zombie.network.PacketSetting;
import zombie.network.PacketTypes;
import zombie.network.packets.INetworkPacket;

@PacketSetting(ordering=1, priority=1, reliability=3, requiredCapability=Capability.LoginOnServer, handlingType=3)
@UsedFromLua
public class NetTimedActionPacket
extends NetTimedAction
implements INetworkPacket {
    public static void createNewAndSend(String actionName, IsoPlayer owner, Object ... values2) {
        Object classObject = LuaManager.get(actionName);
        Object functionObject = LuaManager.getFunctionObject(actionName + ".new");
        Object[] arguments = new Object[values2.length + 1];
        arguments[0] = classObject;
        for (int i = 0; i < values2.length; ++i) {
            arguments[i + 1] = values2[i];
        }
        LuaReturn result = LuaManager.caller.protectedCall(LuaManager.thread, functionObject, arguments);
        if (!result.isSuccess() || result.getFirst() == null) {
            DebugLog.General.error("ERROR GETTING LUATABLE!!!");
            return;
        }
        ActionManager.getInstance().createNetTimedAction(owner, (KahluaTable)result.getFirst());
    }

    @Override
    public void setData(Object ... values2) {
        this.set((IsoPlayer)values2[0], (KahluaTable)values2[1]);
    }

    @Override
    public void processClient(UdpConnection connection) {
        ActionManager.getInstance().setStateFromPacket(this);
    }

    @Override
    public void processServer(PacketTypes.PacketType packetType, UdpConnection connection) {
        if (this.state == Transaction.TransactionState.Request) {
            if (this.isConsistent(connection) && this.action != null) {
                DebugLog.Action.trace("NetTimedAction accepted %s", this.getDescription());
                ActionManager.start(this);
                this.state = Transaction.TransactionState.Accept;
                ByteBufferWriter bbw = connection.startPacket();
                PacketTypes.PacketType.NetTimedAction.doPacket(bbw);
                this.write(bbw);
                PacketTypes.PacketType.NetTimedAction.send(connection);
            } else {
                DebugLog.Action.trace("NetTimedAction rejected %s", this.getDescription());
                this.state = Transaction.TransactionState.Reject;
                ByteBufferWriter bbw = connection.startPacket();
                PacketTypes.PacketType.NetTimedAction.doPacket(bbw);
                this.write(bbw);
                PacketTypes.PacketType.NetTimedAction.send(connection);
            }
        } else if (Transaction.TransactionState.Reject == this.state) {
            DebugLog.Action.trace("NetTimedAction reject %s", this.getDescription());
            ActionManager.stop(this);
        }
    }
}

