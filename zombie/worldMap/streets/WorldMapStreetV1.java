/*
 * Decompiled with CFR 0.152.
 */
package zombie.worldMap.streets;

import zombie.UsedFromLua;
import zombie.worldMap.streets.WorldMapStreet;
import zombie.worldMap.streets.WorldMapStreetsV1;

@UsedFromLua
public class WorldMapStreetV1 {
    WorldMapStreetsV1 owner;
    WorldMapStreet street;

    protected WorldMapStreetV1 init(WorldMapStreetsV1 owner, WorldMapStreet street) {
        this.owner = owner;
        this.street = street;
        return this;
    }

    public String getTranslatedText() {
        return this.street.getTranslatedText();
    }
}

