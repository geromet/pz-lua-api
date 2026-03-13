/*
 * Decompiled with CFR 0.152.
 */
package zombie.core.znet;

import zombie.UsedFromLua;
import zombie.core.textures.Texture;
import zombie.core.znet.SteamFriends;
import zombie.core.znet.SteamUtils;

@UsedFromLua
public class SteamFriend {
    private String name = "";
    private long steamId;
    private String steamIdString;

    public SteamFriend() {
    }

    public SteamFriend(String name, long steamId) {
        this.steamId = steamId;
        this.steamIdString = SteamUtils.convertSteamIDToString(steamId);
        this.name = name;
    }

    public String getName() {
        return this.name;
    }

    public String getSteamID() {
        return this.steamIdString;
    }

    public Texture getAvatar() {
        return Texture.getSteamAvatar(this.steamId);
    }

    public String getState() {
        int state = SteamFriends.GetFriendPersonaState(this.steamId);
        switch (state) {
            case 0: {
                return "Offline";
            }
            case 1: {
                return "Online";
            }
            case 2: {
                return "Busy";
            }
            case 3: {
                return "Away";
            }
            case 4: {
                return "Snooze";
            }
            case 5: {
                return "LookingToTrade";
            }
            case 6: {
                return "LookingToPlay";
            }
        }
        return "Unknown";
    }
}

