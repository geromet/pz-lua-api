/*
 * Decompiled with CFR 0.152.
 */
package zombie.randomizedWorld.randomizedBuilding;

import java.util.ArrayList;
import zombie.SandboxOptions;
import zombie.UsedFromLua;
import zombie.core.random.Rand;
import zombie.core.stash.StashSystem;
import zombie.inventory.InventoryItem;
import zombie.inventory.ItemContainer;
import zombie.inventory.ItemPickerJava;
import zombie.iso.BuildingDef;
import zombie.iso.IsoCell;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoObject;
import zombie.iso.IsoWorld;
import zombie.iso.SpawnPoints;
import zombie.iso.objects.IsoDoor;
import zombie.iso.objects.IsoWindow;
import zombie.network.GameServer;
import zombie.randomizedWorld.randomizedBuilding.RandomizedBuildingBase;

@UsedFromLua
public final class RBLooted
extends RandomizedBuildingBase {
    @Override
    public void randomizeBuilding(BuildingDef def) {
        IsoCell cell = IsoWorld.instance.currentCell;
        ArrayList<InventoryItem> removedItems = new ArrayList<InventoryItem>();
        for (int x = def.x - 1; x < def.x2 + 1; ++x) {
            for (int y = def.y - 1; y < def.y2 + 1; ++y) {
                for (int z = -32; z < 31; ++z) {
                    IsoGridSquare sq = cell.getGridSquare(x, y, z);
                    if (sq == null) continue;
                    for (int o = 0; o < sq.getObjects().size(); ++o) {
                        IsoDoor isoDoor;
                        IsoObject obj = sq.getObjects().get(o);
                        if (obj instanceof IsoDoor && (isoDoor = (IsoDoor)obj).isExterior() && Rand.Next(100) >= 85 && !isoDoor.isBarricaded()) {
                            isoDoor.destroy();
                        } else if (obj instanceof IsoDoor) {
                            IsoDoor isoDoor2 = (IsoDoor)obj;
                            isoDoor2.setLocked(false);
                        }
                        if (Rand.Next(100) >= 85 && obj instanceof IsoWindow) {
                            IsoWindow isoWindow = (IsoWindow)obj;
                            isoWindow.smashWindow(true, false);
                        }
                        for (int i = 0; i < obj.getContainerCount(); ++i) {
                            this.lootContainer(obj, obj.getContainerByIndex(i), removedItems);
                        }
                    }
                }
            }
        }
        def.setAllExplored(true);
        def.alarmed = false;
    }

    private void lootContainer(IsoObject obj, ItemContainer container, ArrayList<InventoryItem> removedItems) {
        if (container == null || container.getItems() == null) {
            return;
        }
        removedItems.clear();
        ArrayList<InventoryItem> items = container.getItems();
        for (int i = 0; i < items.size(); ++i) {
            if (Rand.Next(100) >= 80) continue;
            removedItems.add((InventoryItem)items.get(i));
        }
        if (removedItems.isEmpty()) {
            return;
        }
        if (GameServer.server) {
            GameServer.sendRemoveItemsFromContainer(container, removedItems);
        }
        for (InventoryItem item : removedItems) {
            container.DoRemoveItem(item);
        }
        ItemPickerJava.updateOverlaySprite(obj);
        container.setExplored(true);
    }

    @Override
    public boolean isValid(BuildingDef def, boolean force) {
        if (!super.isValid(def, force)) {
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
        if (SandboxOptions.instance.getCurrentLootedChance() < 1 && !force) {
            return false;
        }
        int max = SandboxOptions.instance.maximumLootedBuildingRooms.getValue();
        if (def.getRooms().size() > max) {
            this.debugLine = "Building is too large, maximum " + max + " rooms";
            return false;
        }
        return true;
    }

    public RBLooted() {
        this.name = "Looted";
        this.setChance(10);
    }
}

