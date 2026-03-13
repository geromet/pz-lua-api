/*
 * Decompiled with CFR 0.152.
 */
package zombie.randomizedWorld.randomizedZoneStory;

import java.util.ArrayList;
import zombie.UsedFromLua;
import zombie.characters.IsoZombie;
import zombie.characters.SurvivorDesc;
import zombie.core.Translator;
import zombie.inventory.InventoryItem;
import zombie.inventory.InventoryItemFactory;
import zombie.iso.IsoGridSquare;
import zombie.iso.zones.Zone;
import zombie.randomizedWorld.randomizedZoneStory.RandomizedZoneStoryBase;

@UsedFromLua
public class RZSSirTwiggy
extends RandomizedZoneStoryBase {
    public RZSSirTwiggy() {
        this.name = "SirTwiggy";
        this.chance = 100;
        this.zoneType.add(RandomizedZoneStoryBase.ZoneType.SirTwiggy.toString());
        this.alwaysDo = true;
    }

    @Override
    public void randomizeZoneStory(Zone zone) {
        IsoGridSquare sq = this.getRandomExtraFreeSquare(this, zone);
        ArrayList<IsoZombie> zeds = this.addZombiesOnSquare(1, "Sir_Twiggy", 0, sq);
        if (zeds == null || zeds.isEmpty()) {
            return;
        }
        IsoZombie twiggy = zeds.get(0);
        twiggy.getHumanVisual().setSkinTextureIndex(0);
        SurvivorDesc desc = twiggy.getDescriptor();
        if (desc == null) {
            return;
        }
        desc.setForename("Ted");
        desc.setSurname("Wigginton");
        Object license = InventoryItemFactory.CreateItem("Base.OfficialDocument");
        ((InventoryItem)license).setName(Translator.getText("IGUI_Document_Twiggys"));
        twiggy.addItemToSpawnAtDeath((InventoryItem)license);
    }
}

