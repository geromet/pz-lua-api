/*
 * Decompiled with CFR 0.152.
 */
package zombie.core.znet;

import zombie.UsedFromLua;
import zombie.core.znet.SteamUtils;
import zombie.core.znet.SteamWorkshop;
import zombie.core.znet.SteamWorkshopItem;
import zombie.debug.DebugLog;

@UsedFromLua
public class SteamUGCDetails {
    private final long id;
    private final String title;
    private final long timeCreated;
    private final long timeUpdated;
    private final int fileSize;
    private final long[] childIds;

    public SteamUGCDetails(long id, String title, long timeCreated, long timeUpdated, int fileSize, long[] childIds) {
        this.id = id;
        this.title = title;
        this.timeCreated = timeCreated;
        this.timeUpdated = timeUpdated;
        this.fileSize = fileSize;
        this.childIds = childIds;
    }

    public long getID() {
        return this.id;
    }

    public String getIDString() {
        return SteamUtils.convertSteamIDToString(this.id);
    }

    public String getTitle() {
        return this.title;
    }

    public long getTimeCreated() {
        return this.timeCreated;
    }

    public long getTimeUpdated() {
        return this.timeUpdated;
    }

    public int getFileSize() {
        return this.fileSize;
    }

    public long[] getChildren() {
        return this.childIds;
    }

    public int getNumChildren() {
        return this.childIds == null ? 0 : this.childIds.length;
    }

    public long getChildID(int index) {
        if (index < 0 || index >= this.getNumChildren()) {
            throw new IndexOutOfBoundsException("invalid child index");
        }
        return this.childIds[index];
    }

    public String getState() {
        long itemState = SteamWorkshop.instance.GetItemState(this.id);
        if (!SteamWorkshopItem.ItemState.Subscribed.and(itemState)) {
            return "NotSubscribed";
        }
        if (SteamWorkshopItem.ItemState.DownloadPending.and(itemState)) {
            DebugLog.log(SteamWorkshopItem.ItemState.toString(itemState) + " ID=" + this.id);
            return "Downloading";
        }
        if (SteamWorkshopItem.ItemState.NeedsUpdate.and(itemState)) {
            return "NeedsUpdate";
        }
        if (SteamWorkshopItem.ItemState.Installed.and(itemState)) {
            return "Installed";
        }
        return "Error";
    }
}

