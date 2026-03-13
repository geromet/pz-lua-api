/*
 * Decompiled with CFR 0.152.
 */
package zombie.core.raknet;

import gnu.trove.list.array.TShortArrayList;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import zombie.SystemDisabler;
import zombie.characters.IsoPlayer;
import zombie.characters.Role;
import zombie.core.math.PZMath;
import zombie.core.network.ByteBufferWriter;
import zombie.core.raknet.RakNetPeerInterface;
import zombie.core.raknet.UdpEngine;
import zombie.core.utils.UpdateTimer;
import zombie.core.znet.ZNetStatistics;
import zombie.iso.IsoUtils;
import zombie.iso.Vector3;
import zombie.network.ClientServerMap;
import zombie.network.ConnectionManager;
import zombie.network.GameClient;
import zombie.network.GameServer;
import zombie.network.IConnection;
import zombie.network.PacketsCache;
import zombie.network.PlayerDownloadServer;
import zombie.network.anticheats.PacketValidator;
import zombie.network.server.EventManager;
import zombie.network.statistics.data.NetworkStatistic;
import zombie.util.StringUtils;

public class UdpConnection
extends PacketsCache
implements IConnection {
    final Lock bufferLock = new ReentrantLock();
    private final ByteBuffer bb = ByteBuffer.allocate(1000000);
    private final ByteBufferWriter bbw = new ByteBufferWriter(this.bb);
    final Lock bufferLockPing = new ReentrantLock();
    private final ByteBuffer bbPing = ByteBuffer.allocate(50);
    private final ByteBufferWriter bbwPing = new ByteBufferWriter(this.bbPing);
    private final long connectedGuid;
    UdpEngine engine;
    private int index;
    private boolean allChatMuted;
    private String userName;
    public String[] usernames = new String[4];
    private byte relevantRange;
    private Role role;
    public static HashMap<String, Long> lastConnections = new HashMap();
    private long lastConnection;
    public long lastUnauthorizedPacket;
    private String ip;
    private boolean wasInLoadingQueue;
    public String password;
    private boolean ping;
    public Vector3[] releventPos = new Vector3[4];
    public short[] playerIds = new short[4];
    public IsoPlayer[] players = new IsoPlayer[4];
    public Vector3[] connectArea = new Vector3[4];
    private int chunkGridWidth;
    private final ClientServerMap[] loadedCells = new ClientServerMap[4];
    private PlayerDownloadServer playerDownloadServer;
    private int zombieListHash;
    private boolean isNeighborPlayer;
    public ChecksumState checksumState = ChecksumState.Init;
    public long checksumTime;
    public boolean awaitingCoopApprove;
    private long steamId;
    private long ownerId;
    private String idStr;
    public boolean isCoopHost;
    private int maxPlayers;
    public final TShortArrayList chunkObjectState = new TShortArrayList();
    public ZNetStatistics netStatistics;
    public final Deque<Integer> pingHistory = new ArrayDeque<Integer>();
    private final PacketValidator validator = new PacketValidator(this);
    private static final long CONNECTION_ATTEMPT_TIMEOUT = 5000L;
    private static final long CONNECTION_GOOGLE_AUTH_TIMEOUT = 60000L;
    public static final long CONNECTION_GRACE_INTERVAL = 60000L;
    public static final long CONNECTION_READY_INTERVAL = 5000L;
    public long connectionTimestamp;
    public boolean googleAuth;
    private boolean ready;
    public UpdateTimer timerSendZombie = new UpdateTimer();
    public HashMap<Short, UpdateTimer> timerUpdateAnimal = new HashMap();
    private boolean fullyConnected;

    public UdpConnection(UdpEngine engine, long connectedGuid, int index) {
        this.engine = engine;
        this.connectedGuid = connectedGuid;
        this.index = index;
        this.releventPos[0] = new Vector3();
        for (int i = 0; i < 4; ++i) {
            this.playerIds[i] = -1;
        }
        this.setConnectionTimestamp(5000L);
        this.wasInLoadingQueue = false;
    }

    public int getMaxPlayers() {
        return this.maxPlayers;
    }

    public void setMaxPlayers(int maxPlayers) {
        this.maxPlayers = maxPlayers;
    }

    public RakNetPeerInterface getPeer() {
        return this.engine.peer;
    }

    @Override
    public long getConnectedGUID() {
        return this.connectedGuid;
    }

    @Override
    public String getIDStr() {
        return this.idStr;
    }

    public void setIDStr(String idStr) {
        this.idStr = idStr;
    }

    public long getOwnerId() {
        return this.ownerId;
    }

    public void setOwnerId(long ownerId) {
        this.ownerId = ownerId;
    }

    public void setPinged(boolean ping) {
        this.ping = ping;
    }

    @Override
    public boolean wasInLoadingQueue() {
        return this.wasInLoadingQueue;
    }

    public void setWasInLoadingQueue(boolean wasInLoadingQueue) {
        this.wasInLoadingQueue = wasInLoadingQueue;
    }

    @Override
    public PlayerDownloadServer getPlayerDownloadServer() {
        return this.playerDownloadServer;
    }

    public void setPlayerDownloadServer(PlayerDownloadServer playerDownloadServer) {
        this.playerDownloadServer = playerDownloadServer;
    }

    public int getZombieListHash() {
        return this.zombieListHash;
    }

    public void setZombieListHash(int zombieListHash) {
        this.zombieListHash = zombieListHash;
    }

    public boolean isNeighborPlayer() {
        return this.isNeighborPlayer;
    }

    public String getServerIP() {
        return this.engine.getServerIP();
    }

    @Override
    public IsoPlayer getPlayerAt(int playerIndex) {
        return this.players[playerIndex];
    }

    @Override
    public void setPlayerAt(int playerIndex, IsoPlayer player) {
        this.players[playerIndex] = player;
    }

    @Override
    public ByteBufferWriter startPacket() {
        this.bufferLock.lock();
        this.bbw.clear();
        return this.bbw;
    }

    public ByteBufferWriter startPingPacket() {
        this.bufferLockPing.lock();
        this.bbwPing.clear();
        return this.bbwPing;
    }

    @Override
    public boolean isRelevantTo(float x, float y) {
        for (int n = 0; n < 4; ++n) {
            if (this.connectArea[n] != null) {
                int chunkMapWidth = (int)this.connectArea[n].z;
                int minX = PZMath.fastfloor(this.connectArea[n].x - (float)(chunkMapWidth / 2)) * 8;
                int minY = PZMath.fastfloor(this.connectArea[n].y - (float)(chunkMapWidth / 2)) * 8;
                int maxX = minX + chunkMapWidth * 8;
                int maxY = minY + chunkMapWidth * 8;
                if (x >= (float)minX && x < (float)maxX && y >= (float)minY && y < (float)maxY) {
                    return true;
                }
            }
            if (this.releventPos[n] == null || !(Math.abs(this.releventPos[n].x - x) <= (float)(this.relevantRange * 8)) || !(Math.abs(this.releventPos[n].y - y) <= (float)(this.relevantRange * 8))) continue;
            return true;
        }
        return false;
    }

    public float getRelevantAndDistance(float x, float y, float z) {
        for (int n = 0; n < 4; ++n) {
            if (this.releventPos[n] == null || !(Math.abs(this.releventPos[n].x - x) <= (float)(this.relevantRange * 8)) || !(Math.abs(this.releventPos[n].y - y) <= (float)(this.relevantRange * 8))) continue;
            return IsoUtils.DistanceTo(this.releventPos[n].x, this.releventPos[n].y, x, y);
        }
        return Float.POSITIVE_INFINITY;
    }

    public boolean RelevantToPlayerIndex(int n, float x, float y) {
        if (this.connectArea[n] != null) {
            int chunkMapWidth = (int)this.connectArea[n].z;
            int minX = PZMath.fastfloor(this.connectArea[n].x - (float)(chunkMapWidth / 2)) * 8;
            int minY = PZMath.fastfloor(this.connectArea[n].y - (float)(chunkMapWidth / 2)) * 8;
            int maxX = minX + chunkMapWidth * 8;
            int maxY = minY + chunkMapWidth * 8;
            if (x >= (float)minX && x < (float)maxX && y >= (float)minY && y < (float)maxY) {
                return true;
            }
        }
        return this.releventPos[n] != null && Math.abs(this.releventPos[n].x - x) <= (float)(this.relevantRange * 8) && Math.abs(this.releventPos[n].y - y) <= (float)(this.relevantRange * 8);
    }

    public boolean RelevantTo(float x, float y, float radius) {
        for (int n = 0; n < 4; ++n) {
            if (this.connectArea[n] != null) {
                int chunkMapWidth = (int)this.connectArea[n].z;
                int minX = PZMath.fastfloor(this.connectArea[n].x - (float)(chunkMapWidth / 2)) * 8;
                int minY = PZMath.fastfloor(this.connectArea[n].y - (float)(chunkMapWidth / 2)) * 8;
                int maxX = minX + chunkMapWidth * 8;
                int maxY = minY + chunkMapWidth * 8;
                if (x >= (float)minX && x < (float)maxX && y >= (float)minY && y < (float)maxY) {
                    return true;
                }
            }
            if (this.releventPos[n] == null || !(Math.abs(this.releventPos[n].x - x) <= radius) || !(Math.abs(this.releventPos[n].y - y) <= radius)) continue;
            return true;
        }
        return false;
    }

    @Override
    public void cancelPacket() {
        this.bufferLock.unlock();
    }

    @Override
    public long getLastConnection() {
        return this.lastConnection;
    }

    public void setLastConnection(long lastConnection) {
        this.lastConnection = lastConnection;
    }

    public int getBufferPosition() {
        return this.bb.position();
    }

    @Override
    public void endPacket(int priority, int reliability, byte ordering) {
        this.endPacket(this.bb, priority, reliability, ordering, this.bufferLock);
    }

    private void endPacket(ByteBuffer bb, int priority, int reliability, byte ordering, Lock lock) {
        int currentPosition = bb.position();
        bb.position(1);
        NetworkStatistic.getInstance().addOutcomePacket(bb.getShort(), currentPosition, this);
        bb.position(currentPosition);
        this.flipSendUnlock(bb, priority, reliability, ordering, lock);
    }

    private void flipSendUnlock(ByteBuffer bb, int priority, int reliability, byte ordering, Lock lock) {
        bb.flip();
        this.engine.peer.Send(bb, priority, reliability, ordering, this.connectedGuid, false);
        lock.unlock();
    }

    @Override
    public String getUserName() {
        return this.userName;
    }

    @Override
    public String getUserName(int playerIndex) {
        return this.usernames[playerIndex];
    }

    @Override
    public void setRelevantRange(byte b) {
        this.relevantRange = b;
    }

    public byte getRelevantRange() {
        return this.relevantRange;
    }

    public void endPacket() {
        this.endPacket(1, 3, (byte)0);
    }

    public void endPacketImmediate() {
        this.endPacket(0, 3, (byte)0);
    }

    public void endPacketUnordered() {
        this.endPacket(2, 2, (byte)0);
    }

    public void endPacketUnreliable() {
        this.flipSendUnlock(this.bb, 2, 1, (byte)0, this.bufferLock);
    }

    public void endPacketSuperHighUnreliable() {
        this.endPacket(0, 1, (byte)0);
    }

    public void endPingPacket() {
        this.endPacket(this.bbPing, 0, 1, (byte)0, this.bufferLockPing);
    }

    public InetSocketAddress getInetSocketAddress() {
        String ip = this.engine.peer.getIPFromGUID(this.connectedGuid);
        if ("UNASSIGNED_SYSTEM_ADDRESS".equals(ip)) {
            return null;
        }
        ip = ip.replace("|", "\u00a3");
        String[] spl = ip.split("\u00a3");
        return new InetSocketAddress(spl[0], Integer.parseInt(spl[1]));
    }

    @Override
    public void forceDisconnect(String description) {
        if (!GameServer.server) {
            GameClient.instance.disconnect(!"receive-CustomizationData".equals(description) && !"lua-connect".equals(description));
        }
        this.engine.forceDisconnect(this.getConnectedGUID(), description);
        ConnectionManager.log("force-disconnect", description, this);
    }

    @Override
    public void setFullyConnected() {
        this.validator.reset();
        this.fullyConnected = true;
        this.setConnectionTimestamp(5000L);
        ConnectionManager.log("fully-connected", "", this);
        EventManager.instance().report("[" + this.userName + "] connected to server");
    }

    @Override
    public long getSteamId() {
        return this.steamId;
    }

    public void setSteamId(long steamId) {
        this.steamId = steamId;
    }

    @Override
    public String getIP() {
        return this.ip;
    }

    public void setIP(String ip) {
        this.ip = ip;
    }

    @Override
    public short getPlayerId(int playerIndex) {
        return this.playerIds[playerIndex];
    }

    @Override
    public void setChunkGridWidth(int range) {
        this.chunkGridWidth = range;
    }

    @Override
    public void setLoadedCells(int playerIndex, ClientServerMap clientServerMap) {
        this.loadedCells[playerIndex] = clientServerMap;
    }

    @Override
    public ClientServerMap getLoadedCell(int playerIndex) {
        return this.loadedCells[playerIndex];
    }

    @Override
    public void setConnectArea(int playerIndex, Vector3 connectArea) {
        this.connectArea[playerIndex] = connectArea;
    }

    @Override
    public Vector3 getRelevantPos(int playerIndex) {
        return this.releventPos[playerIndex];
    }

    @Override
    public void setConnectionTimestamp(long interval) {
        this.connectionTimestamp = System.currentTimeMillis() + interval;
        this.setReady(false);
    }

    @Override
    public PacketValidator getValidator() {
        return this.validator;
    }

    @Override
    public int getChunkGridWidth() {
        return this.chunkGridWidth;
    }

    public int getIndex() {
        return this.index;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    public boolean isAllChatMuted() {
        return this.allChatMuted;
    }

    public void setAllChatMuted(boolean allChatMuted) {
        this.allChatMuted = allChatMuted;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    @Override
    public void setUserName(int playerIndex, String userName) {
        this.usernames[playerIndex] = userName;
    }

    @Override
    public void setRelevantPos(int playerIndex, Vector3 vector3) {
        this.releventPos[playerIndex] = vector3;
    }

    @Override
    public void setPlayerId(int playerIndex, short playerID) {
        this.playerIds[playerIndex] = playerID;
    }

    public void checkReady() {
        if (!this.ready) {
            boolean ready;
            boolean bl = ready = System.currentTimeMillis() > this.connectionTimestamp;
            if (this.ready != ready) {
                this.setReady(ready);
            }
            if (!ready) {
                IsoPlayer.getInstance().setAlpha(0, 0.6f);
            }
        }
    }

    public boolean isReady() {
        return this.ready;
    }

    public void setReady(boolean ready) {
        this.ready = ready;
    }

    public boolean isGoogleAuthTimeout() {
        return System.currentTimeMillis() > this.connectionTimestamp + 60000L;
    }

    public boolean isConnectionAttemptTimeout() {
        return System.currentTimeMillis() > this.connectionTimestamp + 5000L;
    }

    public boolean isConnectionGraceIntervalTimeout() {
        return System.currentTimeMillis() > this.connectionTimestamp + 60000L;
    }

    public boolean isFullyConnected() {
        return this.fullyConnected;
    }

    public void calcCountPlayersInRelevantPosition() {
        if (!this.isFullyConnected()) {
            return;
        }
        boolean isNeighborPlayer = false;
        for (int j = 0; j < GameServer.udpEngine.connections.size(); ++j) {
            UdpConnection c2 = GameServer.udpEngine.connections.get(j);
            if (!c2.isFullyConnected() || c2 == this) continue;
            for (int k = 0; k < c2.players.length; ++k) {
                IsoPlayer p = c2.players[k];
                if (p == null || !this.RelevantTo(p.getX(), p.getY(), 120.0f)) continue;
                isNeighborPlayer = true;
            }
            if (isNeighborPlayer) break;
        }
        this.isNeighborPlayer = isNeighborPlayer;
    }

    public ZNetStatistics getStatistics() {
        try {
            this.netStatistics = this.engine.peer.GetNetStatistics(this.connectedGuid);
        }
        catch (Exception e) {
            return null;
        }
        return this.netStatistics;
    }

    public int getAveragePing() {
        return this.engine.peer.GetAveragePing(this.connectedGuid);
    }

    public int getLastPing() {
        return this.engine.peer.GetLastPing(this.connectedGuid);
    }

    public int getLowestPing() {
        return this.engine.peer.GetLowestPing(this.connectedGuid);
    }

    @Override
    public int getMTUSize() {
        return this.engine.peer.GetMTUSize(this.connectedGuid);
    }

    public ConnectionType getConnectionType() {
        return ConnectionType.values()[this.engine.peer.GetConnectionType(this.connectedGuid)];
    }

    public String toString() {
        if (GameClient.client) {
            if (SystemDisabler.printDetailedInfo()) {
                return String.format("guid=%s ip=%s steam-id=%s role=\"%s\" username=\"%s\" connection-type=\"%s\"", this.connectedGuid, this.ip == null ? GameClient.ip : this.ip, this.steamId == 0L ? GameClient.steamID : this.steamId, this.role == null ? "" : this.role.getName(), this.userName == null ? GameClient.username : this.userName, this.getConnectionType().name());
            }
            return String.format("guid=%s", this.connectedGuid);
        }
        if (SystemDisabler.printDetailedInfo()) {
            return String.format("guid=%s ip=%s steam-id=%s role=%s username=\"%s\" connection-type=\"%s\"", this.connectedGuid, this.ip, this.steamId, this.role == null ? "" : this.role.getName(), this.userName, this.getConnectionType().name());
        }
        return String.format("guid=%s", this.connectedGuid);
    }

    @Override
    public boolean hasPlayer(String userName) {
        if (StringUtils.isNullOrEmpty(userName)) {
            return false;
        }
        for (int i = 0; i < this.players.length; ++i) {
            if (this.players[i] == null || !userName.equals(this.players[i].getUsername())) continue;
            return true;
        }
        return false;
    }

    @Override
    public Role getRole() {
        return this.role;
    }

    @Override
    public void setRole(Role role) {
        this.role = role;
    }

    public boolean havePlayer(IsoPlayer p) {
        if (p == null) {
            return false;
        }
        for (int i = 0; i < this.players.length; ++i) {
            if (this.players[i] != p) continue;
            return true;
        }
        return false;
    }

    public byte getPlayerIndex(IsoPlayer p) {
        for (byte i = 0; i < this.playerIds.length; i = (byte)(i + 1)) {
            if (this.playerIds[i] != p.onlineId) continue;
            return i;
        }
        return -1;
    }

    public static enum ChecksumState {
        Init,
        Different,
        Done;

    }

    public static enum ConnectionType {
        Disconnected,
        UDPRakNet,
        Steam;

    }
}

