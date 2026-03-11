/*
 * Decompiled with CFR 0.152.
 */
package zombie.iso;

import zombie.UsedFromLua;
import zombie.characters.IsoGameCharacter;
import zombie.core.math.PZMath;
import zombie.iso.IsoCell;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoGridSquareCollisionData;
import zombie.iso.IsoWorld;
import zombie.iso.Vector3;

@UsedFromLua
public final class LosUtil {
    public static int sizeX = 200;
    public static int sizeY = 200;
    public static int sizeZ = 16;
    public static PerPlayerData[] cachedresults = new PerPlayerData[4];
    public static boolean[] cachecleared = new boolean[4];

    public static void init(int width, int height) {
        sizeX = Math.min(width, 200);
        sizeY = Math.min(height, 200);
    }

    public static TestResults lineClear(IsoCell cell, int x0, int y0, int z0, int x1, int y1, int z1, boolean bIgnoreDoors) {
        return LosUtil.lineClear(cell, x0, y0, z0, x1, y1, z1, bIgnoreDoors, 10000);
    }

    public static TestResults lineClear(IsoCell cell, int x0, int y0, int z0, int x1, int y1, int z1, boolean bIgnoreDoors, int rangeTillWindows) {
        IsoGridSquare sq;
        if (z1 == z0 - 1 && (sq = cell.getGridSquare(x1, y1, z1)) != null && sq.HasElevatedFloor()) {
            z1 = z0;
        }
        TestResults test = TestResults.Clear;
        int dy = y1 - y0;
        int dx = x1 - x0;
        int dz = z1 - z0;
        float t = 0.5f;
        float t2 = 0.5f;
        int lx = x0;
        int ly = y0;
        int lz = z0;
        IsoGridSquare b = cell.getGridSquare(lx, ly, lz);
        int dist = 0;
        boolean windowChange = false;
        if (Math.abs(dx) > Math.abs(dy) && Math.abs(dx) > Math.abs(dz)) {
            float m = (float)dy / (float)dx;
            float m2 = (float)dz / (float)dx;
            t += (float)y0;
            t2 += (float)z0;
            dx = dx < 0 ? -1 : 1;
            m *= (float)dx;
            m2 *= (float)dx;
            while (x0 != x1) {
                IsoGridSquare a = cell.getGridSquare(x0 += dx, PZMath.fastfloor(t += m), PZMath.fastfloor(t2 += m2));
                if (a != null && b != null) {
                    TestResults newTest = a.testVisionAdjacent(b.getX() - a.getX(), b.getY() - a.getY(), b.getZ() - a.getZ(), true, bIgnoreDoors);
                    if (newTest == TestResults.ClearThroughWindow) {
                        windowChange = true;
                    }
                    if (newTest == TestResults.Blocked || test == TestResults.Clear || newTest == TestResults.ClearThroughWindow && test == TestResults.ClearThroughOpenDoor) {
                        test = newTest;
                    } else if (newTest == TestResults.ClearThroughClosedDoor && test == TestResults.ClearThroughOpenDoor) {
                        test = newTest;
                    }
                    if (test == TestResults.Blocked) {
                        return TestResults.Blocked;
                    }
                    if (windowChange) {
                        if (dist > rangeTillWindows) {
                            return TestResults.Blocked;
                        }
                        dist = 0;
                    }
                }
                b = a;
                ++dist;
                windowChange = false;
            }
        } else if (Math.abs(dy) >= Math.abs(dx) && Math.abs(dy) > Math.abs(dz)) {
            float m = (float)dx / (float)dy;
            float m2 = (float)dz / (float)dy;
            t += (float)x0;
            t2 += (float)z0;
            dy = dy < 0 ? -1 : 1;
            m *= (float)dy;
            m2 *= (float)dy;
            while (y0 != y1) {
                IsoGridSquare a = cell.getGridSquare(PZMath.fastfloor(t += m), y0 += dy, PZMath.fastfloor(t2 += m2));
                if (a != null && b != null) {
                    TestResults newTest = a.testVisionAdjacent(b.getX() - a.getX(), b.getY() - a.getY(), b.getZ() - a.getZ(), true, bIgnoreDoors);
                    if (newTest == TestResults.ClearThroughWindow) {
                        windowChange = true;
                    }
                    if (newTest == TestResults.Blocked || test == TestResults.Clear || newTest == TestResults.ClearThroughWindow && test == TestResults.ClearThroughOpenDoor) {
                        test = newTest;
                    } else if (newTest == TestResults.ClearThroughClosedDoor && test == TestResults.ClearThroughOpenDoor) {
                        test = newTest;
                    }
                    if (test == TestResults.Blocked) {
                        return TestResults.Blocked;
                    }
                    if (windowChange) {
                        if (dist > rangeTillWindows) {
                            return TestResults.Blocked;
                        }
                        dist = 0;
                    }
                }
                b = a;
                ++dist;
                windowChange = false;
            }
        } else {
            float m = (float)dx / (float)dz;
            float m2 = (float)dy / (float)dz;
            t += (float)x0;
            t2 += (float)y0;
            dz = dz < 0 ? -1 : 1;
            m *= (float)dz;
            m2 *= (float)dz;
            while (z0 != z1) {
                IsoGridSquare a = cell.getGridSquare(PZMath.fastfloor(t += m), PZMath.fastfloor(t2 += m2), z0 += dz);
                if (a != null && b != null) {
                    TestResults newTest = a.testVisionAdjacent(b.getX() - a.getX(), b.getY() - a.getY(), b.getZ() - a.getZ(), true, bIgnoreDoors);
                    if (newTest == TestResults.ClearThroughWindow) {
                        windowChange = true;
                    }
                    if (newTest == TestResults.Blocked || test == TestResults.Clear || newTest == TestResults.ClearThroughWindow && test == TestResults.ClearThroughOpenDoor) {
                        test = newTest;
                    } else if (newTest == TestResults.ClearThroughClosedDoor && test == TestResults.ClearThroughOpenDoor) {
                        test = newTest;
                    }
                    if (test == TestResults.Blocked) {
                        return TestResults.Blocked;
                    }
                    if (windowChange) {
                        if (dist > rangeTillWindows) {
                            return TestResults.Blocked;
                        }
                        dist = 0;
                    }
                }
                b = a;
                ++dist;
                windowChange = false;
            }
        }
        return test;
    }

    public static boolean lineClearCollide(int x1, int y1, int z1, int x0, int y0, int z0, boolean bIgnoreDoors) {
        IsoCell cell = IsoWorld.instance.currentCell;
        int dy = y1 - y0;
        int dx = x1 - x0;
        int dz = z1 - z0;
        float t = 0.5f;
        float t2 = 0.5f;
        int lx = x0;
        int ly = y0;
        int lz = z0;
        IsoGridSquare b = cell.getGridSquare(lx, ly, lz);
        if (Math.abs(dx) > Math.abs(dy) && Math.abs(dx) > Math.abs(dz)) {
            float m = (float)dy / (float)dx;
            float m2 = (float)dz / (float)dx;
            t += (float)y0;
            t2 += (float)z0;
            dx = dx < 0 ? -1 : 1;
            m *= (float)dx;
            m2 *= (float)dx;
            while (x0 != x1) {
                IsoGridSquare a = cell.getGridSquare(x0 += dx, PZMath.fastfloor(t += m), PZMath.fastfloor(t2 += m2));
                if (a != null && b != null) {
                    boolean bBlocked = a.CalculateCollide(b, false, false, true, true);
                    if (!bIgnoreDoors && a.isDoorBlockedTo(b)) {
                        bBlocked = true;
                    }
                    if (bBlocked) {
                        return true;
                    }
                }
                b = a;
            }
        } else if (Math.abs(dy) >= Math.abs(dx) && Math.abs(dy) > Math.abs(dz)) {
            float m = (float)dx / (float)dy;
            float m2 = (float)dz / (float)dy;
            t += (float)x0;
            t2 += (float)z0;
            dy = dy < 0 ? -1 : 1;
            m *= (float)dy;
            m2 *= (float)dy;
            while (y0 != y1) {
                IsoGridSquare a = cell.getGridSquare(PZMath.fastfloor(t += m), y0 += dy, PZMath.fastfloor(t2 += m2));
                if (a != null && b != null) {
                    boolean bBlocked = a.CalculateCollide(b, false, false, true, true);
                    if (!bIgnoreDoors && a.isDoorBlockedTo(b)) {
                        bBlocked = true;
                    }
                    if (bBlocked) {
                        return true;
                    }
                }
                b = a;
            }
        } else {
            float m = (float)dx / (float)dz;
            float m2 = (float)dy / (float)dz;
            t += (float)x0;
            t2 += (float)y0;
            dz = dz < 0 ? -1 : 1;
            m *= (float)dz;
            m2 *= (float)dz;
            while (z0 != z1) {
                boolean bBlocked;
                IsoGridSquare a = cell.getGridSquare(PZMath.fastfloor(t += m), PZMath.fastfloor(t2 += m2), z0 += dz);
                if (a != null && b != null && (bBlocked = a.CalculateCollide(b, false, false, true, true))) {
                    return true;
                }
                b = a;
            }
        }
        return false;
    }

    public static int lineClearCollideCount(IsoGameCharacter chr, IsoCell cell, int x1, int y1, int z1, int x0, int y0, int z0) {
        int l = 0;
        int dy = y1 - y0;
        int dx = x1 - x0;
        int dz = z1 - z0;
        float t = 0.5f;
        float t2 = 0.5f;
        int lx = x0;
        int ly = y0;
        int lz = z0;
        IsoGridSquare b = cell.getGridSquare(lx, ly, lz);
        if (Math.abs(dx) > Math.abs(dy) && Math.abs(dx) > Math.abs(dz)) {
            float m = (float)dy / (float)dx;
            float m2 = (float)dz / (float)dx;
            t += (float)y0;
            t2 += (float)z0;
            dx = dx < 0 ? -1 : 1;
            m *= (float)dx;
            m2 *= (float)dx;
            while (x0 != x1) {
                boolean bTest;
                IsoGridSquare a = cell.getGridSquare(x0 += dx, PZMath.fastfloor(t += m), PZMath.fastfloor(t2 += m2));
                if (a != null && b != null && (bTest = b.testCollideAdjacent(chr, a.getX() - b.getX(), a.getY() - b.getY(), a.getZ() - b.getZ()))) {
                    return l;
                }
                ++l;
                b = a;
            }
        } else if (Math.abs(dy) >= Math.abs(dx) && Math.abs(dy) > Math.abs(dz)) {
            float m = (float)dx / (float)dy;
            float m2 = (float)dz / (float)dy;
            t += (float)x0;
            t2 += (float)z0;
            dy = dy < 0 ? -1 : 1;
            m *= (float)dy;
            m2 *= (float)dy;
            while (y0 != y1) {
                boolean bTest;
                IsoGridSquare a = cell.getGridSquare(PZMath.fastfloor(t += m), y0 += dy, PZMath.fastfloor(t2 += m2));
                if (a != null && b != null && (bTest = b.testCollideAdjacent(chr, a.getX() - b.getX(), a.getY() - b.getY(), a.getZ() - b.getZ()))) {
                    return l;
                }
                ++l;
                b = a;
            }
        } else {
            float m = (float)dx / (float)dz;
            float m2 = (float)dy / (float)dz;
            t += (float)x0;
            t2 += (float)y0;
            dz = dz < 0 ? -1 : 1;
            m *= (float)dz;
            m2 *= (float)dz;
            while (z0 != z1) {
                boolean bTest;
                IsoGridSquare a = cell.getGridSquare(PZMath.fastfloor(t += m), PZMath.fastfloor(t2 += m2), z0 += dz);
                if (a != null && b != null && (bTest = b.testCollideAdjacent(chr, a.getX() - b.getX(), a.getY() - b.getY(), a.getZ() - b.getZ()))) {
                    return l;
                }
                ++l;
                b = a;
            }
        }
        return l;
    }

    public static TestResults lineClearCached(IsoCell cell, int x1, int y1, int z1, int x0, int y0, int z0, boolean bIgnoreDoors, int playerIndex) {
        IsoGridSquare sq;
        if (z1 == z0 - 1 && (sq = cell.getGridSquare(x1, y1, z1)) != null && sq.HasElevatedFloor()) {
            z1 = z0;
        }
        int sx = x0;
        int sy = y0;
        int sz = z0;
        int dy = y1 - y0;
        int dx = x1 - x0;
        int dz = z1 - z0;
        int cx = dx;
        int cy = dy;
        int cz = dz;
        if ((cx += sizeX / 2) < 0 || (cy += sizeY / 2) < 0 || (cz += sizeZ / 2) < 0 || cx >= sizeX || cy >= sizeY || cz >= sizeZ) {
            return TestResults.Blocked;
        }
        TestResults res = TestResults.Clear;
        int resultToPropagate = 1;
        PerPlayerData ppd = cachedresults[playerIndex];
        ppd.checkSize();
        byte[][][] cachedresults = ppd.cachedresults;
        if (cachedresults[cx][cy][cz] != 0) {
            if (cachedresults[cx][cy][cz] == 1) {
                res = TestResults.Clear;
            }
            if (cachedresults[cx][cy][cz] == 2) {
                res = TestResults.ClearThroughOpenDoor;
            }
            if (cachedresults[cx][cy][cz] == 3) {
                res = TestResults.ClearThroughWindow;
            }
            if (cachedresults[cx][cy][cz] == 4) {
                res = TestResults.Blocked;
            }
            if (cachedresults[cx][cy][cz] == 5) {
                res = TestResults.ClearThroughClosedDoor;
            }
            return res;
        }
        float t = 0.5f;
        float t2 = 0.5f;
        int lx = x0;
        int ly = y0;
        int lz = z0;
        IsoGridSquare b = cell.getGridSquare(lx, ly, lz);
        if (Math.abs(dx) > Math.abs(dy) && Math.abs(dx) > Math.abs(dz)) {
            float m = (float)dy / (float)dx;
            float m2 = (float)dz / (float)dx;
            t += (float)y0;
            t2 += (float)z0;
            dx = dx < 0 ? -1 : 1;
            m *= (float)dx;
            m2 *= (float)dx;
            while (x0 != x1) {
                IsoGridSquare a = cell.getGridSquare(x0 += dx, PZMath.fastfloor(t += m), PZMath.fastfloor(t2 += m2));
                if (a != null && b != null) {
                    if (resultToPropagate != 4 && a.testVisionAdjacent(b.getX() - a.getX(), b.getY() - a.getY(), b.getZ() - a.getZ(), true, bIgnoreDoors) == TestResults.Blocked) {
                        resultToPropagate = 4;
                    }
                    cx2 = x0 - sx;
                    cy2 = PZMath.fastfloor(t) - sy;
                    cz2 = PZMath.fastfloor(t2) - sz;
                    if (cachedresults[cx2 += sizeX / 2][cy2 += sizeY / 2][cz2 += sizeZ / 2] == 0) {
                        cachedresults[cx2][cy2][cz2] = (byte)resultToPropagate;
                    }
                } else {
                    cx2 = x0 - sx;
                    cy2 = PZMath.fastfloor(t) - sy;
                    cz2 = PZMath.fastfloor(t2) - sz;
                    if (cachedresults[cx2 += sizeX / 2][cy2 += sizeY / 2][cz2 += sizeZ / 2] == 0) {
                        cachedresults[cx2][cy2][cz2] = (byte)resultToPropagate;
                    }
                }
                b = a;
            }
        } else {
            t += (float)x0;
            if (Math.abs(dy) >= Math.abs(dx) && Math.abs(dy) > Math.abs(dz)) {
                float m = (float)dx / (float)dy;
                float m2 = (float)dz / (float)dy;
                t2 += (float)z0;
                dy = dy < 0 ? -1 : 1;
                m *= (float)dy;
                m2 *= (float)dy;
                while (y0 != y1) {
                    IsoGridSquare a = cell.getGridSquare(PZMath.fastfloor(t += m), y0 += dy, PZMath.fastfloor(t2 += m2));
                    if (a != null && b != null) {
                        if (resultToPropagate != 4 && a.testVisionAdjacent(b.getX() - a.getX(), b.getY() - a.getY(), b.getZ() - a.getZ(), true, bIgnoreDoors) == TestResults.Blocked) {
                            resultToPropagate = 4;
                        }
                        cx2 = PZMath.fastfloor(t) - sx;
                        cy2 = PZMath.fastfloor(y0) - sy;
                        cz2 = PZMath.fastfloor(t2) - sz;
                        if (cachedresults[cx2 += sizeX / 2][cy2 += sizeY / 2][cz2 += sizeZ / 2] == 0) {
                            cachedresults[cx2][cy2][cz2] = (byte)resultToPropagate;
                        }
                    } else {
                        cx2 = PZMath.fastfloor(t) - sx;
                        cy2 = PZMath.fastfloor(y0) - sy;
                        cz2 = PZMath.fastfloor(t2) - sz;
                        if (0 == cachedresults[cx2 += sizeX / 2][cy2 += sizeY / 2][cz2 += sizeZ / 2]) {
                            cachedresults[cx2][cy2][cz2] = (byte)resultToPropagate;
                        }
                    }
                    b = a;
                }
            } else {
                float m = (float)dx / (float)dz;
                float m2 = (float)dy / (float)dz;
                t2 += (float)y0;
                dz = dz < 0 ? -1 : 1;
                m *= (float)dz;
                m2 *= (float)dz;
                while (z0 != z1) {
                    IsoGridSquare a = cell.getGridSquare(PZMath.fastfloor(t += m), PZMath.fastfloor(t2 += m2), z0 += dz);
                    if (a != null && b != null) {
                        if (resultToPropagate != 4 && a.testVisionAdjacent(b.getX() - a.getX(), b.getY() - a.getY(), b.getZ() - a.getZ(), true, bIgnoreDoors) == TestResults.Blocked) {
                            resultToPropagate = 4;
                        }
                        cx2 = PZMath.fastfloor(t) - sx;
                        cy2 = PZMath.fastfloor(t2) - sy;
                        cz2 = PZMath.fastfloor(z0) - sz;
                        if (cachedresults[cx2 += sizeX / 2][cy2 += sizeY / 2][cz2 += sizeZ / 2] == 0) {
                            cachedresults[cx2][cy2][cz2] = (byte)resultToPropagate;
                        }
                    } else {
                        cx2 = PZMath.fastfloor(t) - sx;
                        cy2 = PZMath.fastfloor(t2) - sy;
                        cz2 = PZMath.fastfloor(z0) - sz;
                        if (cachedresults[cx2 += sizeX / 2][cy2 += sizeY / 2][cz2 += sizeZ / 2] == 0) {
                            cachedresults[cx2][cy2][cz2] = (byte)resultToPropagate;
                        }
                    }
                    b = a;
                }
            }
        }
        if (resultToPropagate == 1) {
            cachedresults[cx][cy][cz] = (byte)resultToPropagate;
            return TestResults.Clear;
        }
        if (resultToPropagate == 2) {
            cachedresults[cx][cy][cz] = (byte)resultToPropagate;
            return TestResults.ClearThroughOpenDoor;
        }
        if (resultToPropagate == 3) {
            cachedresults[cx][cy][cz] = (byte)resultToPropagate;
            return TestResults.ClearThroughWindow;
        }
        if (resultToPropagate == 4) {
            cachedresults[cx][cy][cz] = (byte)resultToPropagate;
            return TestResults.Blocked;
        }
        if (resultToPropagate == 5) {
            cachedresults[cx][cy][cz] = (byte)resultToPropagate;
            return TestResults.ClearThroughClosedDoor;
        }
        return TestResults.Blocked;
    }

    public static IsoGridSquareCollisionData getFirstBlockingIsoGridSquare(IsoCell cell, int x0, int y0, int z0, int x1, int y1, int z1, boolean bIgnoreDoors) {
        IsoGridSquare sq;
        Vector3 midPoint = new Vector3();
        int rangeTillWindows = 10000;
        IsoGridSquareCollisionData isoGridSquareCollisionData = new IsoGridSquareCollisionData();
        if (z1 == z0 - 1 && (sq = cell.getGridSquare(x1, y1, z1)) != null && sq.HasElevatedFloor()) {
            z1 = z0;
        }
        TestResults test = TestResults.Clear;
        int dy = y1 - y0;
        int dx = x1 - x0;
        int dz = z1 - z0;
        float t = 0.5f;
        float t2 = 0.5f;
        int lx = x0;
        int ly = y0;
        int lz = z0;
        IsoGridSquare b = cell.getGridSquare(lx, ly, lz);
        int dist = 0;
        boolean windowChange = false;
        if (Math.abs(dx) > Math.abs(dy) && Math.abs(dx) > Math.abs(dz)) {
            float m = (float)dy / (float)dx;
            float m2 = (float)dz / (float)dx;
            t += (float)y0;
            t2 += (float)z0;
            dx = dx < 0 ? -1 : 1;
            m *= (float)dx;
            m2 *= (float)dx;
            while (x0 != x1) {
                IsoGridSquare a = cell.getGridSquare(x0 += dx, PZMath.fastfloor(t += m), PZMath.fastfloor(t2 += m2));
                if (a != null && b != null) {
                    int x = b.getX() - a.getX();
                    int y = b.getY() - a.getY();
                    int z = b.getZ() - a.getZ();
                    midPoint.x = (float)(a.getX() + b.getX()) * 0.5f;
                    midPoint.y = (float)(a.getY() + b.getY()) * 0.5f;
                    midPoint.z = (float)(a.getZ() + b.getZ()) * 0.5f;
                    a.getFirstBlocking(isoGridSquareCollisionData, x, y, z, true, bIgnoreDoors);
                    TestResults newTest = isoGridSquareCollisionData.testResults;
                    if (newTest == TestResults.ClearThroughWindow) {
                        windowChange = true;
                    }
                    if (newTest == TestResults.Blocked || test == TestResults.Clear || newTest == TestResults.ClearThroughWindow && test == TestResults.ClearThroughOpenDoor) {
                        test = newTest;
                    } else if (newTest == TestResults.ClearThroughClosedDoor && test == TestResults.ClearThroughOpenDoor) {
                        test = newTest;
                    }
                    if (test == TestResults.Blocked) {
                        IsoGridSquare blockingIsoGridSquare;
                        if (x < 0) {
                            midPoint.x -= (float)x;
                            midPoint.y -= (float)y;
                            midPoint.z -= (float)z;
                            blockingIsoGridSquare = IsoCell.getInstance().getGridSquare(a.getX(), a.getY(), a.getZ());
                        } else {
                            blockingIsoGridSquare = IsoCell.getInstance().getGridSquare(b.getX(), b.getY(), b.getZ());
                        }
                        isoGridSquareCollisionData.isoGridSquare = blockingIsoGridSquare;
                        isoGridSquareCollisionData.hitPosition.set(midPoint.x, midPoint.y, midPoint.z);
                        isoGridSquareCollisionData.testResults = TestResults.Blocked;
                        return isoGridSquareCollisionData;
                    }
                    if (windowChange) {
                        if (dist > 10000) {
                            isoGridSquareCollisionData.isoGridSquare = b;
                            isoGridSquareCollisionData.hitPosition.set(midPoint.x, midPoint.y, midPoint.z);
                            isoGridSquareCollisionData.testResults = TestResults.Blocked;
                            return isoGridSquareCollisionData;
                        }
                        dist = 0;
                    }
                }
                b = a;
                ++dist;
                windowChange = false;
            }
        } else if (Math.abs(dy) >= Math.abs(dx) && Math.abs(dy) > Math.abs(dz)) {
            float m = (float)dx / (float)dy;
            float m2 = (float)dz / (float)dy;
            t += (float)x0;
            t2 += (float)z0;
            dy = dy < 0 ? -1 : 1;
            m *= (float)dy;
            m2 *= (float)dy;
            while (y0 != y1) {
                IsoGridSquare a = cell.getGridSquare(PZMath.fastfloor(t += m), y0 += dy, PZMath.fastfloor(t2 += m2));
                if (a != null && b != null) {
                    int x = b.getX() - a.getX();
                    int y = b.getY() - a.getY();
                    int z = b.getZ() - a.getZ();
                    midPoint.x = (float)(b.getX() + a.getX()) * 0.5f;
                    midPoint.y = (float)(b.getY() + a.getY()) * 0.5f;
                    midPoint.z = (float)(b.getZ() + a.getZ()) * 0.5f;
                    a.getFirstBlocking(isoGridSquareCollisionData, x, y, z, true, bIgnoreDoors);
                    TestResults newTest = isoGridSquareCollisionData.testResults;
                    if (newTest == TestResults.ClearThroughWindow) {
                        windowChange = true;
                    }
                    if (newTest == TestResults.Blocked || test == TestResults.Clear || newTest == TestResults.ClearThroughWindow && test == TestResults.ClearThroughOpenDoor) {
                        test = newTest;
                    } else if (newTest == TestResults.ClearThroughClosedDoor && test == TestResults.ClearThroughOpenDoor) {
                        test = newTest;
                    }
                    if (test == TestResults.Blocked) {
                        IsoGridSquare blockingIsoGridSquare;
                        if (y < 0) {
                            midPoint.x -= (float)x;
                            midPoint.y -= (float)y;
                            midPoint.z -= (float)z;
                            blockingIsoGridSquare = IsoCell.getInstance().getGridSquare(a.getX(), a.getY(), a.getZ());
                        } else {
                            blockingIsoGridSquare = IsoCell.getInstance().getGridSquare(b.getX(), b.getY(), b.getZ());
                        }
                        isoGridSquareCollisionData.isoGridSquare = blockingIsoGridSquare;
                        isoGridSquareCollisionData.hitPosition.set(midPoint.x, midPoint.y, midPoint.z);
                        isoGridSquareCollisionData.testResults = TestResults.Blocked;
                        return isoGridSquareCollisionData;
                    }
                    if (windowChange) {
                        if (dist > 10000) {
                            isoGridSquareCollisionData.isoGridSquare = b;
                            isoGridSquareCollisionData.hitPosition.set(midPoint.x, midPoint.y, midPoint.z);
                            isoGridSquareCollisionData.testResults = TestResults.Blocked;
                            return isoGridSquareCollisionData;
                        }
                        dist = 0;
                    }
                }
                b = a;
                ++dist;
                windowChange = false;
            }
        } else {
            float m = (float)dx / (float)dz;
            float m2 = (float)dy / (float)dz;
            t += (float)x0;
            t2 += (float)y0;
            dz = dz < 0 ? -1 : 1;
            m *= (float)dz;
            m2 *= (float)dz;
            while (z0 != z1) {
                IsoGridSquare a = cell.getGridSquare(PZMath.fastfloor(t += m), PZMath.fastfloor(t2 += m2), z0 += dz);
                if (a != null && b != null) {
                    int x = b.getX() - a.getX();
                    int y = b.getY() - a.getY();
                    int z = b.getZ() - a.getZ();
                    midPoint.x = (float)(b.getX() + a.getX()) * 0.5f;
                    midPoint.y = (float)(b.getY() + a.getY()) * 0.5f;
                    midPoint.z = (float)(b.getZ() + a.getZ()) * 0.5f;
                    a.getFirstBlocking(isoGridSquareCollisionData, x, y, z, true, bIgnoreDoors);
                    TestResults newTest = isoGridSquareCollisionData.testResults;
                    if (newTest == TestResults.ClearThroughWindow) {
                        windowChange = true;
                    }
                    if (newTest == TestResults.Blocked || test == TestResults.Clear || newTest == TestResults.ClearThroughWindow && test == TestResults.ClearThroughOpenDoor) {
                        test = newTest;
                    } else if (newTest == TestResults.ClearThroughClosedDoor && test == TestResults.ClearThroughOpenDoor) {
                        test = newTest;
                    }
                    if (test == TestResults.Blocked) {
                        isoGridSquareCollisionData.isoGridSquare = b;
                        isoGridSquareCollisionData.hitPosition.set(midPoint.x, midPoint.y, midPoint.z);
                        isoGridSquareCollisionData.testResults = TestResults.Blocked;
                        return isoGridSquareCollisionData;
                    }
                    if (windowChange) {
                        if (dist > 10000) {
                            isoGridSquareCollisionData.isoGridSquare = b;
                            isoGridSquareCollisionData.hitPosition.set(midPoint.x, midPoint.y, midPoint.z);
                            isoGridSquareCollisionData.testResults = TestResults.Blocked;
                            return isoGridSquareCollisionData;
                        }
                        dist = 0;
                    }
                }
                b = a;
                ++dist;
                windowChange = false;
            }
        }
        isoGridSquareCollisionData.isoGridSquare = null;
        isoGridSquareCollisionData.hitPosition.set(0.0f, 0.0f, 0.0f);
        isoGridSquareCollisionData.testResults = test;
        return isoGridSquareCollisionData;
    }

    static {
        for (int n = 0; n < 4; ++n) {
            LosUtil.cachecleared[n] = true;
            LosUtil.cachedresults[n] = new PerPlayerData();
        }
    }

    public static enum TestResults {
        Clear,
        ClearThroughOpenDoor,
        ClearThroughWindow,
        Blocked,
        ClearThroughClosedDoor;

    }

    public static final class PerPlayerData {
        public byte[][][] cachedresults;

        public void checkSize() {
            if (this.cachedresults == null || this.cachedresults.length != sizeX || this.cachedresults[0].length != sizeY || this.cachedresults[0][0].length != sizeZ) {
                this.cachedresults = new byte[sizeX][sizeY][sizeZ];
            }
        }
    }
}

