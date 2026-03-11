/*
 * Decompiled with CFR 0.152.
 */
package zombie.network;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import zombie.GameTime;
import zombie.UsedFromLua;
import zombie.characters.Capability;
import zombie.characters.Faction;
import zombie.characters.IsoPlayer;
import zombie.debug.DebugLog;
import zombie.iso.areas.SafeHouse;
import zombie.network.GameClient;
import zombie.network.GameServer;
import zombie.network.PacketTypes;
import zombie.network.ServerOptions;
import zombie.network.packets.INetworkPacket;
import zombie.util.StringUtils;

@UsedFromLua
public final class WarManager {
    private static final ArrayList<War> wars = new ArrayList();
    private static final ArrayList<War> temp = new ArrayList();

    private WarManager() {
    }

    public static ArrayList<War> getWarRelevent(IsoPlayer player) {
        temp.clear();
        if (player.role != null && player.role.hasCapability(Capability.CanGoInsideSafehouses)) {
            temp.addAll(wars);
        } else {
            for (War war : wars) {
                if (!war.isRelevant(player.getUsername())) continue;
                temp.add(war);
            }
        }
        return temp;
    }

    public static War getWarNearest(IsoPlayer player) {
        War nearest = null;
        for (War war : WarManager.getWarRelevent(player)) {
            if (nearest != null && war.timestamp >= nearest.timestamp) continue;
            nearest = war;
        }
        return nearest;
    }

    public static War getWar(int onlineID, String attacker) {
        for (War war : wars) {
            if (war.onlineId != onlineID || !war.attacker.equals(attacker)) continue;
            return war;
        }
        return null;
    }

    public static boolean isWarClaimed(int onlineID) {
        for (War war : wars) {
            if (war.onlineId != onlineID) continue;
            return false;
        }
        return true;
    }

    public static boolean isWarClaimed(String username) {
        for (War war : wars) {
            if (!war.attacker.equals(username)) continue;
            return true;
        }
        return false;
    }

    public static boolean isWarStarted(int onlineID, String username) {
        for (War war : wars) {
            if (war.onlineId != onlineID || State.Started != war.state) continue;
            return war.isRelevant(username);
        }
        return false;
    }

    public static void removeWar(int onlineID, String attacker) {
        wars.removeIf(war -> war.onlineId == onlineID && (StringUtils.isNullOrEmpty(attacker) || war.attacker.equals(attacker)));
    }

    public static void clear() {
        wars.clear();
    }

    public static void sendWarToPlayer(IsoPlayer player) {
        for (War war : wars) {
            INetworkPacket.send(player, PacketTypes.PacketType.WarSync, war);
        }
    }

    public static void updateWar(int onlineId, String attacker, State state, long timestamp) {
        for (War war : wars) {
            if (war.onlineId != onlineId || !war.attacker.equals(attacker)) continue;
            war.setState(state);
            if (GameClient.client || GameServer.server && State.Ended == state) {
                war.setTimestamp(timestamp);
            }
            return;
        }
        War war = new War(onlineId, attacker, state, timestamp);
        wars.add(war);
    }

    public static void update() {
        if (ServerOptions.getInstance().war.getValue()) {
            long timestamp = GameTime.getServerTimeMills();
            Iterator<War> iterator2 = wars.iterator();
            while (iterator2.hasNext()) {
                War war = iterator2.next();
                if (timestamp < war.timestamp || war.state == null) continue;
                switch (war.state.ordinal()) {
                    case 3: 
                    case 4: {
                        SafeHouse.hitPoint(war.onlineId);
                        break;
                    }
                    case 5: 
                    case 6: {
                        war.setTimestamp(timestamp + WarManager.getWarDuration());
                        break;
                    }
                    case 0: {
                        iterator2.remove();
                    }
                }
                if (war.state.next == null) continue;
                war.setState(war.state.next);
            }
        } else {
            Iterator<War> iterator3 = wars.iterator();
            while (iterator3.hasNext()) {
                War war = iterator3.next();
                war.setState(State.Ended);
                iterator3.remove();
            }
        }
    }

    public static long getWarDuration() {
        return (long)ServerOptions.instance.warDuration.getValue() * 1000L;
    }

    public static long getStartDelay() {
        return (long)ServerOptions.instance.warStartDelay.getValue() * 1000L;
    }

    @UsedFromLua
    public static class War {
        private static final HashMap<State, State> transitions = new HashMap<State, State>(){
            {
                this.put(State.Accepted, State.Claimed);
                this.put(State.Canceled, State.Claimed);
                this.put(State.Refused, State.Claimed);
                this.put(State.Started, State.Accepted);
                this.put(State.Blocked, State.Canceled);
            }
        };
        private final int onlineId;
        private final String attacker;
        private State state = State.Ended;
        private long timestamp;

        public War(int onlineId, String attacker, State state, long timestamp) {
            this.onlineId = onlineId;
            this.attacker = attacker;
            this.setTimestamp(timestamp);
            this.setState(state);
        }

        public int getOnlineID() {
            return this.onlineId;
        }

        public String getAttacker() {
            return this.attacker;
        }

        public String getDefender() {
            SafeHouse safehouse = SafeHouse.getSafeHouse(this.onlineId);
            return safehouse != null ? safehouse.getOwner() : "";
        }

        public State getState() {
            return this.state;
        }

        public boolean isValidState(State state) {
            return transitions.get((Object)state) == this.state;
        }

        public void setState(State state) {
            DebugLog.Multiplayer.debugln("War id=%d state=%s->%s", new Object[]{this.onlineId, this.state, state});
            this.state = state;
            INetworkPacket.sendToAll(PacketTypes.PacketType.WarSync, this);
        }

        public long getTimestamp() {
            return this.timestamp;
        }

        public void setTimestamp(long timestamp) {
            DebugLog.Multiplayer.debugln("War id=%d time=%d", this.onlineId, timestamp - GameTime.getServerTimeMills());
            this.timestamp = timestamp;
        }

        public String getTime() {
            long time;
            String result = "00:00:00";
            long serverTime = GameTime.getServerTimeMills();
            if (serverTime > 0L && (time = this.timestamp - serverTime) > 0L) {
                long seconds = time / 1000L % 60L;
                long minutes = time / 1000L / 60L % 60L;
                long hours = time / 3600000L;
                result = String.format("%02d:%02d:%02d", hours, minutes, seconds);
            }
            return result;
        }

        private boolean isRelevant(String username) {
            if (username != null) {
                if (this.attacker.equals(username)) {
                    return true;
                }
                Faction attackerFaction = Faction.getPlayerFaction(this.attacker);
                if (attackerFaction != null && (attackerFaction.isOwner(username) || attackerFaction.isMember(username))) {
                    return true;
                }
                SafeHouse safehouse = SafeHouse.getSafeHouse(this.onlineId);
                if (safehouse != null) {
                    String defender = safehouse.getOwner();
                    if (defender.equals(username)) {
                        return true;
                    }
                    Faction defenderFaction = Faction.getPlayerFaction(defender);
                    if (defenderFaction != null && (defenderFaction.isOwner(username) || defenderFaction.isMember(username))) {
                        return true;
                    }
                    return safehouse.playerAllowed(username);
                }
            }
            return false;
        }
    }

    @UsedFromLua
    public static enum State {
        Ended(null),
        Started(Ended),
        Blocked(Ended),
        Refused(Ended),
        Claimed(Ended),
        Accepted(Started),
        Canceled(Blocked);

        private final State next;

        private State(State next) {
            this.next = next;
        }

        public static State valueOf(int ordinal) {
            for (State state : State.values()) {
                if (state.ordinal() != ordinal) continue;
                return state;
            }
            return Ended;
        }
    }
}

