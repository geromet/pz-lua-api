/*
 * Decompiled with CFR 0.152.
 */
package zombie.network.packets;

import zombie.UsedFromLua;
import zombie.characters.Capability;
import zombie.characters.CharacterStat;
import zombie.characters.IsoPlayer;
import zombie.core.network.ByteBufferReader;
import zombie.core.network.ByteBufferWriter;
import zombie.debug.DebugLog;
import zombie.network.IConnection;
import zombie.network.JSONField;
import zombie.network.PacketSetting;
import zombie.network.fields.character.PlayerID;
import zombie.network.packets.INetworkPacket;

@PacketSetting(ordering=0, priority=1, reliability=2, requiredCapability=Capability.CanModifyBodyStats, handlingType=3)
@UsedFromLua
public class SyncPlayerStatsPacket
implements INetworkPacket {
    @JSONField
    private final PlayerID playerId = new PlayerID();
    @JSONField
    private int syncParams;

    public static int getBitMaskForStat(CharacterStat stat) {
        for (int i = 0; i < CharacterStat.ORDERED_STATS.length; ++i) {
            if (CharacterStat.ORDERED_STATS[i] != stat) continue;
            return 1 << i;
        }
        return 0;
    }

    @Override
    public void setData(Object ... values2) {
        if (values2[0] instanceof IsoPlayer) {
            this.playerId.set((IsoPlayer)values2[0]);
            this.syncParams = (Integer)values2[1];
        } else {
            DebugLog.Multiplayer.warn(this.getClass().getSimpleName() + ".set get invalid arguments");
            DebugLog.Multiplayer.printStackTrace();
        }
    }

    @Override
    public void parse(ByteBufferReader b, IConnection connection) {
        this.playerId.parse(b, connection);
        this.syncParams = b.getInt();
        if (this.syncParams == -1) {
            this.playerId.getPlayer().getNutrition().load(b.bb);
        } else {
            for (byte i = 0; i < CharacterStat.ORDERED_STATS.length; i = (byte)((byte)(i + 1))) {
                if ((this.syncParams >> i & 1) == 0) continue;
                this.playerId.getPlayer().getStats().parse(b.bb, i);
            }
        }
    }

    @Override
    public void write(ByteBufferWriter b) {
        this.playerId.write(b);
        b.putInt(this.syncParams);
        if (this.syncParams == -1) {
            this.playerId.getPlayer().getNutrition().save(b.bb);
        } else {
            for (byte i = 0; i < CharacterStat.ORDERED_STATS.length; i = (byte)((byte)(i + 1))) {
                if ((this.syncParams >> i & 1) == 0) continue;
                this.playerId.getPlayer().getStats().write(b.bb, i);
            }
        }
    }
}

