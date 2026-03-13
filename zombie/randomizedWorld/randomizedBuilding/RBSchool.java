/*
 * Decompiled with CFR 0.152.
 */
package zombie.randomizedWorld.randomizedBuilding;

import zombie.UsedFromLua;
import zombie.core.random.Rand;
import zombie.iso.BuildingDef;
import zombie.iso.IsoCell;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoObject;
import zombie.iso.IsoWorld;
import zombie.randomizedWorld.randomizedBuilding.RandomizedBuildingBase;

@UsedFromLua
public final class RBSchool
extends RandomizedBuildingBase {
    @Override
    public void randomizeBuilding(BuildingDef def) {
        IsoCell cell = IsoWorld.instance.currentCell;
        for (int x = def.x - 1; x < def.x2 + 1; ++x) {
            for (int y = def.y - 1; y < def.y2 + 1; ++y) {
                for (int z = -32; z < 31; ++z) {
                    IsoGridSquare sq = cell.getGridSquare(x, y, z);
                    if (sq == null || !this.roomValid(sq)) continue;
                    block30: for (int i = 0; i < sq.getObjects().size(); ++i) {
                        IsoObject obj = sq.getObjects().get(i);
                        if (!Rand.NextBool(3) || !this.isTableFor3DItems(obj, sq) || !obj.hasAdjacentCanStandSquare()) continue;
                        int penType = Rand.Next(0, 8);
                        switch (penType) {
                            case 0: {
                                RBSchool.trySpawnStoryItem("Pen", sq, Rand.Next(0.4f, 0.8f), Rand.Next(0.4f, 0.8f), obj.getSurfaceOffsetNoTable() / 96.0f);
                                break;
                            }
                            case 1: {
                                RBSchool.trySpawnStoryItem("Pencil", sq, Rand.Next(0.4f, 0.8f), Rand.Next(0.4f, 0.8f), obj.getSurfaceOffsetNoTable() / 96.0f);
                                break;
                            }
                            case 2: {
                                RBSchool.trySpawnStoryItem("Crayons", sq, Rand.Next(0.4f, 0.8f), Rand.Next(0.4f, 0.8f), obj.getSurfaceOffsetNoTable() / 96.0f);
                                break;
                            }
                            case 3: {
                                RBSchool.trySpawnStoryItem("RedPen", sq, Rand.Next(0.4f, 0.8f), Rand.Next(0.4f, 0.8f), obj.getSurfaceOffsetNoTable() / 96.0f);
                                break;
                            }
                            case 4: {
                                RBSchool.trySpawnStoryItem("BluePen", sq, Rand.Next(0.4f, 0.8f), Rand.Next(0.4f, 0.8f), obj.getSurfaceOffsetNoTable() / 96.0f);
                                break;
                            }
                            case 5: {
                                RBSchool.trySpawnStoryItem("Eraser", sq, Rand.Next(0.4f, 0.8f), Rand.Next(0.4f, 0.8f), obj.getSurfaceOffsetNoTable() / 96.0f);
                                break;
                            }
                            case 6: {
                                RBSchool.trySpawnStoryItem("CorrectionFluid", sq, Rand.Next(0.4f, 0.8f), Rand.Next(0.4f, 0.8f), obj.getSurfaceOffsetNoTable() / 96.0f);
                            }
                        }
                        int bookType = Rand.Next(0, 6);
                        switch (bookType) {
                            case 0: {
                                RBSchool.trySpawnStoryItem("DoodleKids", sq, Rand.Next(0.4f, 0.8f), Rand.Next(0.4f, 0.8f), obj.getSurfaceOffsetNoTable() / 96.0f);
                                continue block30;
                            }
                            case 1: {
                                RBSchool.trySpawnStoryItem("Book_SchoolTextbook", sq, Rand.Next(0.4f, 0.8f), Rand.Next(0.4f, 0.8f), obj.getSurfaceOffsetNoTable() / 96.0f);
                                continue block30;
                            }
                            case 2: {
                                RBSchool.trySpawnStoryItem("Notebook", sq, Rand.Next(0.4f, 0.8f), Rand.Next(0.4f, 0.8f), obj.getSurfaceOffsetNoTable() / 96.0f);
                                continue block30;
                            }
                            case 3: {
                                RBSchool.trySpawnStoryItem("SheetPaper2", sq, Rand.Next(0.4f, 0.8f), Rand.Next(0.4f, 0.8f), obj.getSurfaceOffsetNoTable() / 96.0f);
                            }
                        }
                    }
                    if (sq.getRoom() == null || !"classroom".equals(sq.getRoom().getName()) || !sq.hasAdjacentCanStandSquare()) continue;
                    if (Rand.NextBool(50)) {
                        int bookType = Rand.Next(0, 10);
                        switch (bookType) {
                            case 0: {
                                RBSchool.trySpawnStoryItem("DoodleKids", sq, Rand.Next(0.4f, 0.8f), Rand.Next(0.4f, 0.8f), 0.0f);
                                break;
                            }
                            case 1: {
                                RBSchool.trySpawnStoryItem("Book_SchoolTextbook", sq, Rand.Next(0.4f, 0.8f), Rand.Next(0.4f, 0.8f), 0.0f);
                                break;
                            }
                            case 2: {
                                RBSchool.trySpawnStoryItem("Notebook", sq, Rand.Next(0.4f, 0.8f), Rand.Next(0.4f, 0.8f), 0.0f);
                                break;
                            }
                            case 3: {
                                RBSchool.trySpawnStoryItem("SheetPaper2", sq, Rand.Next(0.4f, 0.8f), Rand.Next(0.4f, 0.8f), 0.0f);
                                break;
                            }
                            case 4: {
                                RBSchool.trySpawnStoryItem("Pen", sq, Rand.Next(0.4f, 0.8f), Rand.Next(0.4f, 0.8f), 0.0f);
                                break;
                            }
                            case 5: {
                                RBSchool.trySpawnStoryItem("Pencil", sq, Rand.Next(0.4f, 0.8f), Rand.Next(0.4f, 0.8f), 0.0f);
                                break;
                            }
                            case 6: {
                                RBSchool.trySpawnStoryItem("Crayons", sq, Rand.Next(0.4f, 0.8f), Rand.Next(0.4f, 0.8f), 0.0f);
                                break;
                            }
                            case 7: {
                                RBSchool.trySpawnStoryItem("RedPen", sq, Rand.Next(0.4f, 0.8f), Rand.Next(0.4f, 0.8f), 0.0f);
                                break;
                            }
                            case 8: {
                                RBSchool.trySpawnStoryItem("BluePen", sq, Rand.Next(0.4f, 0.8f), Rand.Next(0.4f, 0.8f), 0.0f);
                                break;
                            }
                            case 9: {
                                RBSchool.trySpawnStoryItem("Eraser", sq, Rand.Next(0.4f, 0.8f), Rand.Next(0.4f, 0.8f), 0.0f);
                            }
                        }
                    }
                    if (!Rand.NextBool(120)) continue;
                    RBSchool.trySpawnStoryItem("Bag_Schoolbag_Kids", sq, Rand.Next(0.4f, 0.8f), Rand.Next(0.4f, 0.8f), 0.0f);
                }
            }
        }
    }

    public boolean roomValid(IsoGridSquare sq) {
        return sq.getRoom() != null && "classroom".equals(sq.getRoom().getName());
    }

    @Override
    public boolean isValid(BuildingDef def, boolean force) {
        return def.getRoom("classroom") != null || force;
    }

    public RBSchool() {
        this.name = "School";
        this.setAlwaysDo(true);
    }
}

