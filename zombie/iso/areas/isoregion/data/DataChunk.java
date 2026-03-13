/*
 * Decompiled with CFR 0.152.
 */
package zombie.iso.areas.isoregion.data;

import java.nio.ByteBuffer;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import zombie.UsedFromLua;
import zombie.characters.IsoGameCharacter;
import zombie.core.math.PZMath;
import zombie.debug.DebugLog;
import zombie.iso.areas.isoregion.IsoRegions;
import zombie.iso.areas.isoregion.data.DataCell;
import zombie.iso.areas.isoregion.data.DataRoot;
import zombie.iso.areas.isoregion.data.DataSquarePos;
import zombie.iso.areas.isoregion.regions.IsoChunkRegion;
import zombie.iso.areas.isoregion.regions.IsoWorldRegion;

@UsedFromLua
public final class DataChunk {
    private final DataCell cell;
    private final int hashId;
    private final int chunkX;
    private final int chunkY;
    private int highestZ;
    private long lastUpdateStamp;
    private final boolean[] activeZLayers = new boolean[32];
    private final boolean[] dirtyZLayers = new boolean[32];
    private byte[] squareFlags;
    private byte[] regionIds;
    private final ArrayList<ArrayList<IsoChunkRegion>> chunkRegions = new ArrayList(32);
    private static byte selectedFlags;
    static final HashSet<IsoWorldRegion> tempWorldRegions;
    private static final ArrayDeque<DataSquarePos> tmpSquares;
    private static final HashSet<Integer> tmpLinkedChunks;
    private static final boolean[] exploredPositions;
    private static IsoChunkRegion lastCurRegion;
    private static IsoChunkRegion lastOtherRegionFullConnect;
    private static ArrayList<IsoChunkRegion> oldList;
    private static final ArrayDeque<IsoChunkRegion> chunkQueue;

    DataChunk(int chunkX, int chunkY, DataCell cell, int chunkID) {
        this.cell = cell;
        this.hashId = chunkID < 0 ? IsoRegions.hash(chunkX, chunkY) : chunkID;
        this.chunkX = chunkX;
        this.chunkY = chunkY;
        for (int i = 0; i < 32; ++i) {
            this.chunkRegions.add(new ArrayList());
        }
    }

    int getHashId() {
        return this.hashId;
    }

    public int getChunkX() {
        return this.chunkX;
    }

    public int getChunkY() {
        return this.chunkY;
    }

    public int getCellX() {
        return PZMath.fastfloor((float)this.getChunkX() / 32.0f);
    }

    public int getCellY() {
        return PZMath.fastfloor((float)this.getChunkY() / 32.0f);
    }

    ArrayList<IsoChunkRegion> getChunkRegions(int z) {
        return this.chunkRegions.get(z);
    }

    public long getLastUpdateStamp() {
        return this.lastUpdateStamp;
    }

    public void setLastUpdateStamp(long lastUpdateStamp) {
        this.lastUpdateStamp = lastUpdateStamp;
    }

    private boolean isDirty(int z) {
        if (this.activeZLayers[z]) {
            return this.dirtyZLayers[z];
        }
        return false;
    }

    private void setDirty(int z) {
        if (this.activeZLayers[z]) {
            this.dirtyZLayers[z] = true;
            this.cell.dataRoot.EnqueueDirtyDataChunk(this);
        }
    }

    public void setDirtyAllActive() {
        boolean queued = false;
        for (int z = 0; z < 32; ++z) {
            if (!this.activeZLayers[z]) continue;
            this.dirtyZLayers[z] = true;
            if (queued) continue;
            this.cell.dataRoot.EnqueueDirtyDataChunk(this);
            queued = true;
        }
    }

    void unsetDirtyAll() {
        Arrays.fill(this.dirtyZLayers, false);
    }

    private boolean validCoords(int x, int y, int z) {
        return x >= 0 && x < 8 && y >= 0 && y < 8 && z >= 0 && z < this.highestZ + 1;
    }

    private int getCoord1D(int x, int y, int z) {
        return z * 8 * 8 + y * 8 + x;
    }

    public byte getSquare(int x, int y, int z) {
        return this.getSquare(x, y, z, false);
    }

    public byte getSquare(int x, int y, int z, boolean ignoreCoordCheck) {
        if (this.squareFlags != null && (ignoreCoordCheck || this.validCoords(x, y, z))) {
            if (this.activeZLayers[z]) {
                return this.squareFlags[this.getCoord1D(x, y, z)];
            }
            return -1;
        }
        return -1;
    }

    private byte setOrAddSquare(int x, int y, int z, byte flags) {
        return this.setOrAddSquare(x, y, z, flags, false);
    }

    byte setOrAddSquare(int x, int y, int z, byte flags, boolean ignoreCoordCheck) {
        if (ignoreCoordCheck || this.validCoords(x, y, z)) {
            this.ensureSquares(z);
            int id = this.getCoord1D(x, y, z);
            if (this.squareFlags[id] != flags) {
                this.setDirty(z);
            }
            this.squareFlags[id] = flags;
            return flags;
        }
        return -1;
    }

    private void ensureSquares(int zlayer) {
        if (zlayer < 0 || zlayer >= 32) {
            return;
        }
        if (!this.activeZLayers[zlayer]) {
            this.ensureSquareArray(zlayer);
            this.activeZLayers[zlayer] = true;
            if (zlayer > this.highestZ) {
                this.highestZ = zlayer;
            }
            for (int y = 0; y < 8; ++y) {
                for (int x = 0; x < 8; ++x) {
                    int index = this.getCoord1D(x, y, zlayer);
                    this.squareFlags[index] = zlayer == 0 ? 16 : 0;
                }
            }
        }
    }

    private void ensureSquareArray(int zlayer) {
        int squareArraySize = (zlayer + 1) * 8 * 8;
        if (this.squareFlags == null || this.squareFlags.length < squareArraySize) {
            byte[] oldArray = this.squareFlags;
            byte[] oldRegions = this.regionIds;
            this.squareFlags = new byte[squareArraySize];
            this.regionIds = new byte[squareArraySize];
            if (oldArray != null) {
                for (int i = 0; i < oldArray.length; ++i) {
                    this.squareFlags[i] = oldArray[i];
                    this.regionIds[i] = oldRegions[i];
                }
            }
        }
    }

    public void save(ByteBuffer bb) {
        try {
            int startPos = bb.position();
            bb.putInt(0);
            bb.putInt(this.highestZ);
            int maxBytes = (this.highestZ + 1) * 8 * 8;
            bb.putInt(maxBytes);
            for (int i = 0; i < maxBytes; ++i) {
                bb.put(this.squareFlags[i]);
            }
            int endPos = bb.position();
            bb.position(startPos);
            bb.putInt(endPos - startPos);
            bb.position(endPos);
        }
        catch (Exception e) {
            DebugLog.log(e.getMessage());
            e.printStackTrace();
        }
    }

    public void load(ByteBuffer bb, int worldVersion, boolean readLength) {
        try {
            if (readLength) {
                bb.getInt();
            }
            for (int z = this.highestZ = bb.getInt(); z >= 0; --z) {
                this.ensureSquares(z);
            }
            int maxBytes = bb.getInt();
            for (int i = 0; i < maxBytes; ++i) {
                this.squareFlags[i] = bb.get();
            }
        }
        catch (Exception e) {
            DebugLog.log(e.getMessage());
            e.printStackTrace();
        }
    }

    public void setSelectedFlags(int x, int y, int z) {
        selectedFlags = z >= 0 && z <= this.highestZ ? this.squareFlags[this.getCoord1D(x, y, z)] : (byte)-1;
    }

    public boolean selectedHasFlags(byte flags) {
        return (selectedFlags & flags) == flags;
    }

    private boolean squareHasFlags(int x, int y, int z, byte flags) {
        return this.squareHasFlags(this.getCoord1D(x, y, z), flags);
    }

    private boolean squareHasFlags(int coord1D, byte flags) {
        byte f = this.squareFlags[coord1D];
        return (f & flags) == flags;
    }

    public byte squareGetFlags(int x, int y, int z) {
        return this.squareGetFlags(this.getCoord1D(x, y, z));
    }

    private byte squareGetFlags(int coord1D) {
        return this.squareFlags[coord1D];
    }

    private void squareAddFlags(int x, int y, int z, byte flags) {
        this.squareAddFlags(this.getCoord1D(x, y, z), flags);
    }

    private void squareAddFlags(int coord1D, byte flags) {
        int n = coord1D;
        this.squareFlags[n] = (byte)(this.squareFlags[n] | flags);
    }

    private void squareRemoveFlags(int x, int y, int z, byte flags) {
        this.squareRemoveFlags(this.getCoord1D(x, y, z), flags);
    }

    private void squareRemoveFlags(int coord1D, byte flags) {
        int n = coord1D;
        this.squareFlags[n] = (byte)(this.squareFlags[n] ^ flags);
    }

    private boolean squareCanConnect(int x, int y, int z, byte dir) {
        return this.squareCanConnect(this.getCoord1D(x, y, z), z, dir);
    }

    private boolean squareCanConnect(int coord1D, int z, byte dir) {
        if (z >= 0 && z < this.highestZ + 1) {
            if (dir == 0) {
                return !this.squareHasFlags(coord1D, (byte)1);
            }
            if (dir == 1) {
                return !this.squareHasFlags(coord1D, (byte)2);
            }
            if (dir == 2) {
                return true;
            }
            if (dir == 3) {
                return true;
            }
            if (dir == 4) {
                return !this.squareHasFlags(coord1D, (byte)64);
            }
            if (dir == 5) {
                return !this.squareHasFlags(coord1D, (byte)16);
            }
        }
        return false;
    }

    public IsoChunkRegion getIsoChunkRegion(int x, int y, int z) {
        return this.getIsoChunkRegion(this.getCoord1D(x, y, z), z);
    }

    private IsoChunkRegion getIsoChunkRegion(int coord1D, int z) {
        byte id;
        if (z >= 0 && z < this.highestZ + 1 && (id = this.regionIds[coord1D]) >= 0 && id < this.chunkRegions.get(z).size()) {
            return this.chunkRegions.get(z).get(id);
        }
        return null;
    }

    public void setRegion(int x, int y, int z, byte regionIndex) {
        this.regionIds[this.getCoord1D((int)x, (int)y, (int)z)] = regionIndex;
    }

    void clearBuildingDefs(ArrayList<IsoGameCharacter.Location> changedCells) {
        tempWorldRegions.clear();
        for (int z = 0; z <= this.highestZ; ++z) {
            if (!this.dirtyZLayers[z] || !this.activeZLayers[z]) continue;
            ArrayList<IsoChunkRegion> zRegions = this.chunkRegions.get(z);
            for (int i = zRegions.size() - 1; i >= 0; --i) {
                IsoChunkRegion isoChunkRegion = zRegions.get(i);
                IsoWorldRegion isoWorldRegion = isoChunkRegion.getIsoWorldRegion();
                this.clearBuildingDefs(isoWorldRegion, changedCells, tempWorldRegions);
            }
        }
    }

    void clearBuildingDefs(IsoWorldRegion isoWorldRegion, ArrayList<IsoGameCharacter.Location> changedCells, HashSet<IsoWorldRegion> done) {
        if (done.contains(isoWorldRegion)) {
            return;
        }
        done.add(isoWorldRegion);
        isoWorldRegion.clearBuildingDef(changedCells);
        for (IsoWorldRegion isoWorldRegion1 : isoWorldRegion.getNeighbors()) {
            this.clearBuildingDefs(isoWorldRegion1, changedCells, done);
        }
    }

    void recalculate() {
        for (int z = 0; z <= this.highestZ; ++z) {
            if (!this.dirtyZLayers[z] || !this.activeZLayers[z]) continue;
            this.recalculate(z);
        }
    }

    private void recalculate(int z) {
        ArrayList<IsoChunkRegion> zRegions = this.chunkRegions.get(z);
        for (int i = zRegions.size() - 1; i >= 0; --i) {
            IsoChunkRegion isoChunkRegion = zRegions.get(i);
            IsoWorldRegion mr = isoChunkRegion.unlinkFromIsoWorldRegion();
            if (mr != null && mr.size() <= 0) {
                this.cell.dataRoot.regionManager.releaseIsoWorldRegion(mr);
            }
            this.cell.dataRoot.regionManager.releaseIsoChunkRegion(isoChunkRegion);
            zRegions.remove(i);
        }
        zRegions.clear();
        int zBlockSize = 64;
        Arrays.fill(this.regionIds, z * 64, z * 64 + 64, (byte)-1);
        for (int y = 0; y < 8; ++y) {
            for (int x = 0; x < 8; ++x) {
                if (this.regionIds[this.getCoord1D(x, y, z)] != -1) continue;
                IsoChunkRegion isoChunkRegion = this.floodFill(x, y, z);
            }
        }
    }

    private IsoChunkRegion floodFill(int x, int y, int z) {
        DataSquarePos dsPos;
        IsoChunkRegion region = this.cell.dataRoot.regionManager.allocIsoChunkRegion(this, z);
        byte regionIndex = (byte)this.chunkRegions.get(z).size();
        this.chunkRegions.get(z).add(region);
        this.clearExploredPositions();
        tmpSquares.clear();
        tmpLinkedChunks.clear();
        tmpSquares.add(DataSquarePos.alloc(x, y, z));
        while ((dsPos = tmpSquares.poll()) != null) {
            int coord1D = this.getCoord1D(dsPos.x, dsPos.y, dsPos.z);
            this.setExploredPosition(coord1D, dsPos.z);
            if (this.regionIds[coord1D] != -1) continue;
            this.regionIds[coord1D] = regionIndex;
            region.addSquareCount();
            for (byte dir = 0; dir < 4; dir = (byte)(dir + 1)) {
                DataSquarePos neighbor = this.getNeighbor(dsPos, dir);
                if (neighbor != null) {
                    int neighborCoord1D = this.getCoord1D(neighbor.x, neighbor.y, neighbor.z);
                    if (this.isExploredPosition(neighborCoord1D, neighbor.z)) {
                        DataSquarePos.release(neighbor);
                        continue;
                    }
                    if (this.squareCanConnect(coord1D, dsPos.z, dir) && this.squareCanConnect(neighborCoord1D, neighbor.z, IsoRegions.GetOppositeDir(dir))) {
                        if (this.regionIds[neighborCoord1D] == -1) {
                            tmpSquares.add(neighbor);
                            this.setExploredPosition(neighborCoord1D, neighbor.z);
                            continue;
                        }
                    } else {
                        IsoChunkRegion nc = this.getIsoChunkRegion(neighborCoord1D, neighbor.z);
                        if (nc != null && nc != region) {
                            if (!tmpLinkedChunks.contains(nc.getID())) {
                                region.addNeighbor(nc);
                                nc.addNeighbor(region);
                                tmpLinkedChunks.add(nc.getID());
                            }
                            this.setExploredPosition(neighborCoord1D, neighbor.z);
                            DataSquarePos.release(neighbor);
                            continue;
                        }
                    }
                    DataSquarePos.release(neighbor);
                    continue;
                }
                if (!this.squareCanConnect(coord1D, dsPos.z, dir)) continue;
                region.addChunkBorderSquaresCnt();
            }
        }
        return region;
    }

    private boolean isExploredPosition(int coord1D, int z) {
        int index = coord1D - z * 8 * 8;
        return exploredPositions[index];
    }

    private void setExploredPosition(int coord1D, int z) {
        int index = coord1D - z * 8 * 8;
        DataChunk.exploredPositions[index] = true;
    }

    private void clearExploredPositions() {
        Arrays.fill(exploredPositions, false);
    }

    private DataSquarePos getNeighbor(DataSquarePos pos, byte dir) {
        int tmpx = pos.x;
        int tmpy = pos.y;
        if (dir == 1) {
            tmpx = pos.x - 1;
        } else if (dir == 3) {
            tmpx = pos.x + 1;
        }
        if (dir == 0) {
            tmpy = pos.y - 1;
        } else if (dir == 2) {
            tmpy = pos.y + 1;
        }
        if (tmpx < 0 || tmpx >= 8 || tmpy < 0 || tmpy >= 8) {
            return null;
        }
        return DataSquarePos.alloc(tmpx, tmpy, pos.z);
    }

    void link(DataChunk n, DataChunk w, DataChunk s, DataChunk e) {
        for (int z = 0; z <= this.highestZ; ++z) {
            if (!this.dirtyZLayers[z] || !this.activeZLayers[z]) continue;
            this.linkRegionsOnSide(z, n, (byte)0);
            this.linkRegionsOnSide(z, w, (byte)1);
            this.linkRegionsOnSide(z, s, (byte)2);
            this.linkRegionsOnSide(z, e, (byte)3);
        }
    }

    private void linkRegionsOnSide(int z, DataChunk opposite, byte dir) {
        int yMax;
        int xMax;
        if (dir == 0 || dir == 2) {
            xStart = 0;
            xMax = 8;
            yStart = dir == 0 ? 0 : 7;
            yMax = yStart + 1;
        } else {
            xStart = dir == 1 ? 0 : 7;
            xMax = xStart + 1;
            yStart = 0;
            yMax = 8;
        }
        if (opposite != null && opposite.isDirty(z)) {
            opposite.resetEnclosedSide(z, IsoRegions.GetOppositeDir(dir));
        }
        lastCurRegion = null;
        lastOtherRegionFullConnect = null;
        for (int y = yStart; y < yMax; ++y) {
            for (int x = xStart; x < xMax; ++x) {
                IsoChunkRegion otherRegion;
                int yOther;
                int xOther;
                if (dir == 0 || dir == 2) {
                    xOther = x;
                    yOther = dir == 0 ? 7 : 0;
                } else {
                    xOther = dir == 1 ? 7 : 0;
                    yOther = y;
                }
                int coord1d = this.getCoord1D(x, y, z);
                int otherCoord1d = this.getCoord1D(xOther, yOther, z);
                IsoChunkRegion curRegion = this.getIsoChunkRegion(coord1d, z);
                IsoChunkRegion isoChunkRegion = otherRegion = opposite != null ? opposite.getIsoChunkRegion(otherCoord1d, z) : null;
                if (curRegion == null) {
                    IsoRegions.warn("ds.getRegion()==null, shouldnt happen at this point.");
                    continue;
                }
                if (lastCurRegion != null && lastCurRegion != curRegion) {
                    lastOtherRegionFullConnect = null;
                }
                if (lastCurRegion != null && lastCurRegion == curRegion && otherRegion != null && lastOtherRegionFullConnect == otherRegion) continue;
                if (opposite == null || otherRegion == null) {
                    if (this.squareCanConnect(coord1d, z, dir)) {
                        curRegion.setEnclosed(dir, false);
                    }
                } else if (this.squareCanConnect(coord1d, z, dir) && opposite.squareCanConnect(otherCoord1d, z, IsoRegions.GetOppositeDir(dir))) {
                    curRegion.addConnectedNeighbor(otherRegion);
                    otherRegion.addConnectedNeighbor(curRegion);
                    curRegion.addNeighbor(otherRegion);
                    otherRegion.addNeighbor(curRegion);
                    if (!otherRegion.getIsEnclosed()) {
                        otherRegion.setEnclosed(IsoRegions.GetOppositeDir(dir), true);
                    }
                    lastOtherRegionFullConnect = otherRegion;
                } else {
                    curRegion.addNeighbor(otherRegion);
                    otherRegion.addNeighbor(curRegion);
                    if (!otherRegion.getIsEnclosed()) {
                        otherRegion.setEnclosed(IsoRegions.GetOppositeDir(dir), true);
                    }
                    lastOtherRegionFullConnect = null;
                }
                lastCurRegion = curRegion;
            }
        }
    }

    private void resetEnclosedSide(int z, byte dir) {
        ArrayList<IsoChunkRegion> regions = this.chunkRegions.get(z);
        for (int i = 0; i < regions.size(); ++i) {
            IsoChunkRegion r = regions.get(i);
            if (r.getzLayer() != z) continue;
            r.setEnclosed(dir, true);
        }
    }

    void interConnect() {
        for (int z = 0; z <= this.highestZ; ++z) {
            if (!this.dirtyZLayers[z] || !this.activeZLayers[z]) continue;
            ArrayList<IsoChunkRegion> regionList = this.chunkRegions.get(z);
            for (int i = 0; i < regionList.size(); ++i) {
                IsoChunkRegion region = regionList.get(i);
                if (region.getzLayer() != z || region.getIsoWorldRegion() != null) continue;
                if (region.getConnectedNeighbors().isEmpty()) {
                    IsoWorldRegion ms = this.cell.dataRoot.regionManager.allocIsoWorldRegion();
                    this.cell.dataRoot.EnqueueDirtyIsoWorldRegion(ms);
                    ms.addIsoChunkRegion(region);
                    continue;
                }
                IsoChunkRegion neighbor = region.getConnectedNeighborWithLargestIsoWorldRegion();
                if (neighbor != null) {
                    IsoChunkRegion r;
                    IsoWorldRegion largestWorldRegion = neighbor.getIsoWorldRegion();
                    oldList.clear();
                    oldList = largestWorldRegion.swapIsoChunkRegions(oldList);
                    for (int k = 0; k < oldList.size(); ++k) {
                        r = oldList.get(k);
                        r.setIsoWorldRegion(null);
                    }
                    this.cell.dataRoot.regionManager.releaseIsoWorldRegion(largestWorldRegion);
                    IsoWorldRegion target = this.cell.dataRoot.regionManager.allocIsoWorldRegion();
                    this.cell.dataRoot.EnqueueDirtyIsoWorldRegion(target);
                    this.floodFillExpandWorldRegion(region, target);
                    for (int k = 0; k < oldList.size(); ++k) {
                        r = oldList.get(k);
                        if (r.getIsoWorldRegion() != null) continue;
                        IsoWorldRegion mr = this.cell.dataRoot.regionManager.allocIsoWorldRegion();
                        this.cell.dataRoot.EnqueueDirtyIsoWorldRegion(mr);
                        this.floodFillExpandWorldRegion(r, mr);
                    }
                    ++DataRoot.floodFills;
                    continue;
                }
                IsoWorldRegion ms = this.cell.dataRoot.regionManager.allocIsoWorldRegion();
                this.cell.dataRoot.EnqueueDirtyIsoWorldRegion(ms);
                this.floodFillExpandWorldRegion(region, ms);
                ++DataRoot.floodFills;
            }
        }
    }

    private void floodFillExpandWorldRegion(IsoChunkRegion start, IsoWorldRegion worldRegion) {
        IsoChunkRegion current;
        chunkQueue.add(start);
        while ((current = chunkQueue.poll()) != null) {
            worldRegion.addIsoChunkRegion(current);
            if (current.getConnectedNeighbors().isEmpty()) continue;
            for (int i = 0; i < current.getConnectedNeighbors().size(); ++i) {
                IsoChunkRegion neighbor = current.getConnectedNeighbors().get(i);
                if (chunkQueue.contains(neighbor)) continue;
                if (neighbor.getIsoWorldRegion() == null) {
                    chunkQueue.add(neighbor);
                    continue;
                }
                if (neighbor.getIsoWorldRegion() == worldRegion) continue;
                worldRegion.merge(neighbor.getIsoWorldRegion());
            }
        }
    }

    void recalcRoofs() {
        if (this.highestZ < 1) {
            return;
        }
        for (int i = 0; i < this.chunkRegions.size(); ++i) {
            for (int j = 0; j < this.chunkRegions.get(i).size(); ++j) {
                IsoChunkRegion c = this.chunkRegions.get(i).get(j);
                c.resetRoofCnt();
            }
        }
        int z = this.highestZ;
        for (int y = 0; y < 8; ++y) {
            for (int x = 0; x < 8; ++x) {
                byte flags = this.getSquare(x, y, z);
                boolean hasroof = false;
                if (flags > 0) {
                    hasroof = this.squareHasFlags(x, y, z, (byte)16);
                }
                if (z < 1) continue;
                for (int zz = z - 1; zz >= 0; --zz) {
                    boolean bEnclosedRegion;
                    flags = this.getSquare(x, y, zz);
                    IsoChunkRegion region = this.getIsoChunkRegion(x, y, zz);
                    IsoWorldRegion worldRegion = region == null ? null : region.getIsoWorldRegion();
                    boolean bl = bEnclosedRegion = worldRegion != null && worldRegion.isEnclosed();
                    if (flags > 0 || bEnclosedRegion) {
                        boolean bl2 = hasroof = hasroof || this.squareHasFlags(x, y, zz, (byte)32);
                        if (hasroof) {
                            if (region != null) {
                                region.addRoof();
                                if (region.getIsoWorldRegion() != null && !region.getIsoWorldRegion().isEnclosed()) {
                                    hasroof = false;
                                }
                            } else {
                                hasroof = false;
                            }
                        }
                        if (hasroof) continue;
                        hasroof = this.squareHasFlags(x, y, zz, (byte)16);
                        continue;
                    }
                    hasroof = false;
                }
            }
        }
    }

    static {
        tempWorldRegions = new HashSet();
        tmpSquares = new ArrayDeque();
        tmpLinkedChunks = new HashSet();
        exploredPositions = new boolean[64];
        oldList = new ArrayList();
        chunkQueue = new ArrayDeque();
    }
}

