/*
 * Decompiled with CFR 0.152.
 */
package zombie.iso.areas.isoregion.regions;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import zombie.UsedFromLua;
import zombie.core.Color;
import zombie.core.Core;
import zombie.iso.areas.isoregion.IsoRegions;
import zombie.iso.areas.isoregion.data.DataChunk;
import zombie.iso.areas.isoregion.regions.IChunkRegion;
import zombie.iso.areas.isoregion.regions.IsoRegionManager;
import zombie.iso.areas.isoregion.regions.IsoWorldRegion;

@UsedFromLua
public final class IsoChunkRegion
implements IChunkRegion {
    private final IsoRegionManager manager;
    private boolean isInPool;
    private Color color;
    private int id;
    private DataChunk dataChunk;
    private byte zLayer;
    private byte squareSize;
    private byte roofCnt;
    private byte chunkBorderSquaresCnt;
    private final boolean[] enclosed = new boolean[4];
    private boolean enclosedCache = true;
    private final List<IsoChunkRegion> connectedNeighbors = new LinkedList<IsoChunkRegion>();
    private final List<IsoChunkRegion> allNeighbors = new LinkedList<IsoChunkRegion>();
    private boolean isDirtyEnclosed;
    private IsoWorldRegion isoWorldRegion;

    public int getID() {
        return this.id;
    }

    public int getSquareSize() {
        return this.squareSize;
    }

    public Color getColor() {
        return this.color;
    }

    public int getzLayer() {
        return this.zLayer;
    }

    public IsoWorldRegion getIsoWorldRegion() {
        return this.isoWorldRegion;
    }

    public void setIsoWorldRegion(IsoWorldRegion mr) {
        this.isoWorldRegion = mr;
    }

    protected boolean isInPool() {
        return this.isInPool;
    }

    protected IsoChunkRegion(IsoRegionManager manager) {
        this.manager = manager;
    }

    public DataChunk getDataChunk() {
        return this.dataChunk;
    }

    protected void init(int id, DataChunk dataChunk, int zLayer) {
        this.isInPool = false;
        this.id = id;
        this.dataChunk = dataChunk;
        this.zLayer = (byte)zLayer;
        this.resetChunkBorderSquaresCnt();
        if (this.color == null) {
            this.color = this.manager.getColor();
        }
        this.squareSize = 0;
        this.roofCnt = 0;
        this.resetEnclosed();
    }

    protected IsoChunkRegion reset() {
        this.isInPool = true;
        this.unlinkNeighbors();
        IsoWorldRegion mr = this.unlinkFromIsoWorldRegion();
        if (mr != null && mr.size() <= 0) {
            if (Core.debug) {
                throw new RuntimeException("ChunkRegion.reset IsoChunkRegion has IsoWorldRegion with 0 members.");
            }
            this.manager.releaseIsoWorldRegion(mr);
            IsoRegions.warn("ChunkRegion.reset IsoChunkRegion has IsoWorldRegion with 0 members.");
        }
        this.resetChunkBorderSquaresCnt();
        this.id = -1;
        this.squareSize = 0;
        this.roofCnt = 0;
        this.resetEnclosed();
        return this;
    }

    public IsoWorldRegion unlinkFromIsoWorldRegion() {
        if (this.isoWorldRegion != null) {
            IsoWorldRegion mr = this.isoWorldRegion;
            this.isoWorldRegion.removeIsoChunkRegion(this);
            this.isoWorldRegion = null;
            return mr;
        }
        return null;
    }

    public int getRoofCnt() {
        return this.roofCnt;
    }

    public void addRoof() {
        this.roofCnt = (byte)(this.roofCnt + 1);
        if (this.roofCnt > this.squareSize) {
            IsoRegions.warn("ChunkRegion.addRoof roofCount exceed squareSize.");
            this.roofCnt = this.squareSize;
            return;
        }
        if (this.isoWorldRegion != null) {
            this.isoWorldRegion.addRoof();
        }
    }

    public void resetRoofCnt() {
        if (this.isoWorldRegion != null) {
            this.isoWorldRegion.removeRoofs(this.roofCnt);
        }
        this.roofCnt = 0;
    }

    public void addSquareCount() {
        this.squareSize = (byte)(this.squareSize + 1);
    }

    public int getChunkBorderSquaresCnt() {
        return this.chunkBorderSquaresCnt;
    }

    public void addChunkBorderSquaresCnt() {
        this.chunkBorderSquaresCnt = (byte)(this.chunkBorderSquaresCnt + 1);
    }

    protected void removeChunkBorderSquaresCnt() {
        this.chunkBorderSquaresCnt = (byte)(this.chunkBorderSquaresCnt - 1);
        if (this.chunkBorderSquaresCnt < 0) {
            this.chunkBorderSquaresCnt = 0;
        }
    }

    protected void resetChunkBorderSquaresCnt() {
        this.chunkBorderSquaresCnt = 0;
    }

    private void resetEnclosed() {
        for (int dir = 0; dir < 4; dir = (int)((byte)(dir + 1))) {
            this.enclosed[dir] = true;
        }
        this.isDirtyEnclosed = false;
        this.enclosedCache = true;
    }

    public void setEnclosed(byte dir, boolean b) {
        this.isDirtyEnclosed = true;
        this.enclosed[dir] = b;
    }

    protected void setDirtyEnclosed() {
        this.isDirtyEnclosed = true;
        if (this.isoWorldRegion != null) {
            this.isoWorldRegion.setDirtyEnclosed();
        }
    }

    public boolean getIsEnclosed() {
        if (!this.isDirtyEnclosed) {
            return this.enclosedCache;
        }
        this.isDirtyEnclosed = false;
        this.enclosedCache = true;
        for (int dir = 0; dir < 4; dir = (int)((byte)(dir + 1))) {
            if (this.enclosed[dir]) continue;
            this.enclosedCache = false;
        }
        if (this.isoWorldRegion != null) {
            this.isoWorldRegion.setDirtyEnclosed();
        }
        return this.enclosedCache;
    }

    public List<IsoChunkRegion> getConnectedNeighbors() {
        return this.connectedNeighbors;
    }

    public void addConnectedNeighbor(IsoChunkRegion neighbor) {
        if (neighbor == null) {
            return;
        }
        if (!this.connectedNeighbors.contains(neighbor)) {
            this.connectedNeighbors.add(neighbor);
        }
    }

    protected void removeConnectedNeighbor(IsoChunkRegion neighbor) {
        this.connectedNeighbors.remove(neighbor);
    }

    public int getNeighborCount() {
        return this.allNeighbors.size();
    }

    protected List<IsoChunkRegion> getAllNeighbors() {
        return this.allNeighbors;
    }

    public void addNeighbor(IsoChunkRegion neighbor) {
        if (neighbor == null) {
            return;
        }
        if (!this.allNeighbors.contains(neighbor)) {
            this.allNeighbors.add(neighbor);
        }
    }

    protected void removeNeighbor(IsoChunkRegion neighbor) {
        this.allNeighbors.remove(neighbor);
    }

    protected void unlinkNeighbors() {
        IsoChunkRegion neighbor;
        int i;
        for (i = 0; i < this.connectedNeighbors.size(); ++i) {
            neighbor = this.connectedNeighbors.get(i);
            neighbor.removeConnectedNeighbor(this);
        }
        this.connectedNeighbors.clear();
        for (i = 0; i < this.allNeighbors.size(); ++i) {
            neighbor = this.allNeighbors.get(i);
            neighbor.removeNeighbor(this);
        }
        this.allNeighbors.clear();
    }

    public ArrayList<IsoChunkRegion> getDebugConnectedNeighborCopy() {
        ArrayList<IsoChunkRegion> copy = new ArrayList<IsoChunkRegion>();
        if (this.connectedNeighbors.isEmpty()) {
            return copy;
        }
        copy.addAll(this.connectedNeighbors);
        return copy;
    }

    public boolean containsConnectedNeighbor(IsoChunkRegion n) {
        return this.connectedNeighbors.contains(n);
    }

    public boolean containsConnectedNeighborID(int id) {
        if (this.connectedNeighbors.isEmpty()) {
            return false;
        }
        for (int i = 0; i < this.connectedNeighbors.size(); ++i) {
            IsoChunkRegion neighbor = this.connectedNeighbors.get(i);
            if (neighbor.getID() != id) continue;
            return true;
        }
        return false;
    }

    public IsoChunkRegion getConnectedNeighborWithLargestIsoWorldRegion() {
        if (this.connectedNeighbors.isEmpty()) {
            return null;
        }
        IsoWorldRegion largest = null;
        IsoChunkRegion target = null;
        for (int i = 0; i < this.connectedNeighbors.size(); ++i) {
            IsoChunkRegion neighbor = this.connectedNeighbors.get(i);
            IsoWorldRegion mr = neighbor.getIsoWorldRegion();
            if (mr == null || largest != null && mr.getSquareSize() <= largest.getSquareSize()) continue;
            largest = mr;
            target = neighbor;
        }
        return target;
    }

    protected IsoChunkRegion getFirstNeighborWithIsoWorldRegion() {
        if (this.connectedNeighbors.isEmpty()) {
            return null;
        }
        for (int i = 0; i < this.connectedNeighbors.size(); ++i) {
            IsoChunkRegion neighbor = this.connectedNeighbors.get(i);
            IsoWorldRegion mr = neighbor.getIsoWorldRegion();
            if (mr == null) continue;
            return neighbor;
        }
        return null;
    }
}

