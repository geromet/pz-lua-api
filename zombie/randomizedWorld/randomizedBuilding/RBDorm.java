/*
 * Decompiled with CFR 0.152.
 */
package zombie.randomizedWorld.randomizedBuilding;

import java.util.Objects;
import zombie.UsedFromLua;
import zombie.core.properties.IsoPropertyType;
import zombie.core.random.Rand;
import zombie.inventory.ItemPickerJava;
import zombie.iso.BuildingDef;
import zombie.iso.IsoCell;
import zombie.iso.IsoDirections;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoObject;
import zombie.iso.IsoWorld;
import zombie.iso.sprite.IsoSprite;
import zombie.randomizedWorld.randomizedBuilding.RBBasic;
import zombie.randomizedWorld.randomizedBuilding.RandomizedBuildingBase;

@UsedFromLua
public final class RBDorm
extends RandomizedBuildingBase {
    @Override
    public void randomizeBuilding(BuildingDef def) {
        IsoCell cell = IsoWorld.instance.currentCell;
        for (int x = def.x - 1; x < def.x2 + 1; ++x) {
            for (int y = def.y - 1; y < def.y2 + 1; ++y) {
                for (int z = -32; z < 31; ++z) {
                    IsoGridSquare sq = cell.getGridSquare(x, y, z);
                    if (sq == null || !this.roomValid(sq) || sq.getObjects().size() > 3) continue;
                    for (int i = 0; i < sq.getObjects().size(); ++i) {
                        IsoObject obj = sq.getObjects().get(i);
                        if (!obj.isTableSurface() || sq.getObjects().size() > 3 || !Rand.NextBool(3)) continue;
                        IsoDirections facing = this.getFacing(obj.getSprite());
                        if (facing != null) {
                            if (facing == IsoDirections.E) {
                                RBDorm.trySpawnStoryItem(RBBasic.getDormClutterItem(), sq, 0.42f, Rand.Next(0.34f, 0.74f), obj.getSurfaceOffsetNoTable() / 96.0f);
                            }
                            if (facing == IsoDirections.W) {
                                RBDorm.trySpawnStoryItem(RBBasic.getDormClutterItem(), sq, 0.64f, Rand.Next(0.34f, 0.74f), obj.getSurfaceOffsetNoTable() / 96.0f);
                            }
                            if (facing == IsoDirections.N) {
                                RBDorm.trySpawnStoryItem(RBBasic.getDormClutterItem(), sq, Rand.Next(0.44f, 0.64f), 0.67f, obj.getSurfaceOffsetNoTable() / 96.0f);
                            }
                            if (facing == IsoDirections.S) {
                                RBDorm.trySpawnStoryItem(RBBasic.getDormClutterItem(), sq, Rand.Next(0.44f, 0.64f), 0.42f, obj.getSurfaceOffsetNoTable() / 96.0f);
                            }
                        } else {
                            RBDorm.trySpawnStoryItem(RBBasic.getDormClutterItem(), sq, Rand.Next(0.4f, 0.8f), Rand.Next(0.4f, 0.8f), obj.getSurfaceOffsetNoTable() / 96.0f);
                        }
                        if (!Rand.NextBool(3)) continue;
                        if (facing != null) {
                            if (facing == IsoDirections.E) {
                                RBDorm.trySpawnStoryItem(RBBasic.getDormClutterItem(), sq, 0.42f, Rand.Next(0.34f, 0.74f), obj.getSurfaceOffsetNoTable() / 96.0f);
                            }
                            if (facing == IsoDirections.W) {
                                RBDorm.trySpawnStoryItem(RBBasic.getDormClutterItem(), sq, 0.64f, Rand.Next(0.34f, 0.74f), obj.getSurfaceOffsetNoTable() / 96.0f);
                            }
                            if (facing == IsoDirections.N) {
                                RBDorm.trySpawnStoryItem(RBBasic.getDormClutterItem(), sq, Rand.Next(0.44f, 0.64f), 0.67f, obj.getSurfaceOffsetNoTable() / 96.0f);
                            }
                            if (facing != IsoDirections.S) continue;
                            RBDorm.trySpawnStoryItem(RBBasic.getDormClutterItem(), sq, Rand.Next(0.44f, 0.64f), 0.42f, obj.getSurfaceOffsetNoTable() / 96.0f);
                            continue;
                        }
                        RBDorm.trySpawnStoryItem(RBBasic.getDormClutterItem(), sq, Rand.Next(0.4f, 0.8f), Rand.Next(0.4f, 0.8f), obj.getSurfaceOffsetNoTable() / 96.0f);
                    }
                }
            }
        }
    }

    public boolean roomValid(IsoGridSquare sq) {
        return sq.getRoom() != null && "livingroom".equals(sq.getRoom().getName());
    }

    @Override
    public boolean isValid(BuildingDef def, boolean force) {
        IsoCell cell = IsoWorld.instance.currentCell;
        IsoGridSquare sq = cell.getGridSquare(def.x, def.y, 0);
        if (sq == null) {
            return false;
        }
        return def.getRoom("livingroom") != null && ItemPickerJava.getSquareZombiesType(sq) != null && Objects.equals(ItemPickerJava.getSquareZombiesType(sq), "University");
    }

    public RBDorm() {
        this.name = "Dorm";
        this.setAlwaysDo(true);
    }

    private IsoDirections getFacing(IsoSprite sprite) {
        if (sprite != null && sprite.getProperties().has(IsoPropertyType.FACING)) {
            String facing;
            switch (facing = sprite.getProperties().get(IsoPropertyType.FACING)) {
                case "N": {
                    return IsoDirections.N;
                }
                case "S": {
                    return IsoDirections.S;
                }
                case "W": {
                    return IsoDirections.W;
                }
                case "E": {
                    return IsoDirections.E;
                }
            }
        }
        return null;
    }
}

