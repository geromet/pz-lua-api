/*
 * Decompiled with CFR 0.152.
 */
package zombie.randomizedWorld.randomizedDeadSurvivor;

import zombie.UsedFromLua;
import zombie.characters.IsoPlayer;
import zombie.core.random.Rand;
import zombie.core.stash.StashSystem;
import zombie.iso.BuildingDef;
import zombie.iso.RoomDef;
import zombie.iso.SpawnPoints;
import zombie.network.GameClient;
import zombie.network.GameServer;
import zombie.randomizedWorld.randomizedDeadSurvivor.RandomizedDeadSurvivorBase;

@UsedFromLua
public final class RDSPokerNight
extends RandomizedDeadSurvivorBase {
    private final String money;
    private final String card;

    public RDSPokerNight() {
        this.name = "Poker Night";
        this.setChance(4);
        this.setMaximumDays(60);
        this.money = "Base.Money";
        this.card = "Base.CardDeck";
    }

    @Override
    public boolean isValid(BuildingDef def, boolean force) {
        this.debugLine = "";
        if (GameClient.client) {
            return false;
        }
        if (def.isAllExplored() && !force) {
            return false;
        }
        if (SpawnPoints.instance.isSpawnBuilding(def)) {
            this.debugLine = "Spawn houses are invalid";
            return false;
        }
        if (StashSystem.isStashBuilding(def)) {
            this.debugLine = "Stash buildings are invalid";
            return false;
        }
        if (!force) {
            for (int i = 0; i < GameServer.Players.size(); ++i) {
                IsoPlayer player = GameServer.Players.get(i);
                if (player.getSquare() == null || player.getSquare().getBuilding() == null || player.getSquare().getBuilding().def != def) continue;
                return false;
            }
        }
        if (this.getRoom(def, "kitchen") != null) {
            return true;
        }
        this.debugLine = "No kitchen";
        return false;
    }

    @Override
    public void randomizeDeadSurvivor(BuildingDef def) {
        RoomDef room = this.getRoom(def, "kitchen");
        this.addZombies(def, Rand.Next(3, 5), null, 10, room);
        this.addZombies(def, 1, "PokerDealer", 0, room);
        this.addRandomItemsOnGround(room, this.getPokerNightClutter(), Rand.Next(3, 7));
        this.addRandomItemsOnGround(room, this.money, Rand.Next(8, 13));
        this.addRandomItemsOnGround(room, this.card, 1);
        def.alarmed = false;
    }
}

