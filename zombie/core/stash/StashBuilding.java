/*
 * Decompiled with CFR 0.152.
 */
package zombie.core.stash;

import zombie.UsedFromLua;

@UsedFromLua
public final class StashBuilding {
    public int buildingX;
    public int buildingY;
    public String stashName;

    public StashBuilding(String stashName, int buildingX, int buildingY) {
        this.stashName = stashName;
        this.buildingX = buildingX;
        this.buildingY = buildingY;
    }

    public String getName() {
        return this.stashName;
    }
}

