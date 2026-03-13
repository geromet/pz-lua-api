/*
 * Decompiled with CFR 0.152.
 */
package zombie.network.packets;

import zombie.UsedFromLua;
import zombie.characters.BodyDamage.BodyPart;
import zombie.characters.Capability;
import zombie.characters.IsoPlayer;
import zombie.core.network.ByteBufferReader;
import zombie.core.network.ByteBufferWriter;
import zombie.network.IConnection;
import zombie.network.JSONField;
import zombie.network.PacketSetting;
import zombie.network.fields.character.PlayerID;
import zombie.network.packets.INetworkPacket;

@PacketSetting(ordering=0, priority=1, reliability=2, requiredCapability=Capability.LoginOnServer, handlingType=2)
@UsedFromLua
public class BodyPartSyncPacket
implements INetworkPacket {
    public static final long BD_Health = 1L;
    public static final long BD_bandaged = 2L;
    public static final long BD_bitten = 4L;
    public static final long BD_bleeding = 8L;
    public static final long BD_IsBleedingStemmed = 16L;
    public static final long BD_IsCauterized = 32L;
    public static final long BD_scratched = 64L;
    public static final long BD_stitched = 128L;
    public static final long BD_deepWounded = 256L;
    public static final long BD_IsInfected = 512L;
    public static final long BD_IsFakeInfected = 1024L;
    public static final long BD_bandageLife = 2048L;
    public static final long BD_scratchTime = 4096L;
    public static final long BD_biteTime = 8192L;
    public static final long BD_alcoholicBandage = 16384L;
    public static final long BD_woundInfectionLevel = 32768L;
    public static final long BD_infectedWound = 65536L;
    public static final long BD_bleedingTime = 131072L;
    public static final long BD_deepWoundTime = 262144L;
    public static final long BD_haveGlass = 524288L;
    public static final long BD_stitchTime = 0x100000L;
    public static final long BD_alcoholLevel = 0x200000L;
    public static final long BD_additionalPain = 0x400000L;
    public static final long BD_bandageType = 0x800000L;
    public static final long BD_getBandageXp = 0x1000000L;
    public static final long BD_getStitchXp = 0x2000000L;
    public static final long BD_getSplintXp = 0x4000000L;
    public static final long BD_fractureTime = 0x8000000L;
    public static final long BD_splint = 0x10000000L;
    public static final long BD_splintFactor = 0x20000000L;
    public static final long BD_haveBullet = 0x40000000L;
    public static final long BD_burnTime = 0x80000000L;
    public static final long BD_needBurnWash = 0x100000000L;
    public static final long BD_lastTimeBurnWash = 0x200000000L;
    public static final long BD_splintItem = 0x400000000L;
    public static final long BD_plantainFactor = 0x800000000L;
    public static final long BD_comfreyFactor = 0x1000000000L;
    public static final long BD_garlicFactor = 0x2000000000L;
    public static final long BD_cut = 0x4000000000L;
    public static final long BD_cutTime = 0x8000000000L;
    public static final long BD_stiffness = 0x10000000000L;
    public static final long BD_BodyDamage = 0x20000000000L;
    @JSONField
    PlayerID playerId = new PlayerID();
    @JSONField
    BodyPart bodyPart;
    @JSONField
    long syncParams;

    @Override
    public void setData(Object ... values2) {
        this.bodyPart = (BodyPart)values2[0];
        this.playerId.set((IsoPlayer)this.bodyPart.getParentChar());
        this.syncParams = (Long)values2[1];
    }

    @Override
    public void parse(ByteBufferReader b, IConnection connection) {
        this.playerId.parse(b, connection);
        byte bodyPartIdx = b.getByte();
        this.bodyPart = this.playerId.getPlayer().getBodyDamage().getBodyParts().get(bodyPartIdx);
        this.syncParams = b.getLong();
        for (int i = 0; i < 42; ++i) {
            if ((this.syncParams >> i & 1L) <= 0L) continue;
            this.bodyPart.sync(b, (byte)(i + 1));
        }
    }

    @Override
    public void write(ByteBufferWriter b) {
        this.playerId.write(b);
        b.putByte(this.bodyPart.getIndex());
        b.putLong(this.syncParams);
        for (int i = 0; i < 42; ++i) {
            if ((this.syncParams >> i & 1L) <= 0L) continue;
            this.bodyPart.syncWrite(b, i + 1);
        }
    }
}

