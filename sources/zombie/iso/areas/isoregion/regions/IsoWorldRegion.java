/*
 * Decompiled with CFR 0.152.
 */
package zombie.iso.areas.isoregion.regions;

import java.util.ArrayList;
import zombie.UsedFromLua;
import zombie.characters.IsoGameCharacter;
import zombie.core.Color;
import zombie.core.Core;
import zombie.iso.BuildingDef;
import zombie.iso.areas.isoregion.IsoRegions;
import zombie.iso.areas.isoregion.data.DataChunk;
import zombie.iso.areas.isoregion.regions.IWorldRegion;
import zombie.iso.areas.isoregion.regions.IsoChunkRegion;
import zombie.iso.areas.isoregion.regions.IsoRegionManager;

@UsedFromLua
public final class IsoWorldRegion
implements IWorldRegion {
    private final IsoRegionManager manager;
    private boolean isInPool;
    private int id;
    private Color color;
    private boolean enclosed = true;
    private ArrayList<IsoChunkRegion> isoChunkRegions = new ArrayList();
    private int squareSize;
    private int roofCnt;
    private boolean isDirtyEnclosed;
    private boolean isDirtyRoofed;
    private final ArrayList<IsoWorldRegion> neighbors = new ArrayList();
    private BuildingDef buildingDef;
    static final IsoGameCharacter.Location tempLocation = new IsoGameCharacter.Location();

    public int getID() {
        return this.id;
    }

    public Color getColor() {
        return this.color;
    }

    public int size() {
        return this.isoChunkRegions.size();
    }

    @Override
    public int getSquareSize() {
        return this.squareSize;
    }

    protected boolean isInPool() {
        return this.isInPool;
    }

    protected IsoWorldRegion(IsoRegionManager manager) {
        this.manager = manager;
    }

    protected void init(int id) {
        this.isInPool = false;
        this.id = id;
        if (this.color == null) {
            this.color = this.manager.getColor();
        }
        this.squareSize = 0;
        this.roofCnt = 0;
        this.enclosed = true;
        this.isDirtyEnclosed = false;
        this.isDirtyRoofed = false;
    }

    protected IsoWorldRegion reset() {
        this.isInPool = true;
        this.id = -1;
        this.squareSize = 0;
        this.roofCnt = 0;
        this.enclosed = true;
        this.isDirtyRoofed = false;
        this.isDirtyEnclosed = false;
        this.unlinkNeighbors();
        if (!this.isoChunkRegions.isEmpty()) {
            if (Core.debug) {
                throw new RuntimeException("MasterRegion.reset Resetting master region which still has chunk regions");
            }
            IsoRegions.warn("MasterRegion.reset Resetting master region which still has chunk regions");
            for (int i = 0; i < this.isoChunkRegions.size(); ++i) {
                IsoChunkRegion r = this.isoChunkRegions.get(i);
                r.setIsoWorldRegion(null);
            }
            this.isoChunkRegions.clear();
        }
        this.buildingDef = null;
        return this;
    }

    public void unlinkNeighbors() {
        for (int i = 0; i < this.neighbors.size(); ++i) {
            IsoWorldRegion n = this.neighbors.get(i);
            n.removeNeighbor(this);
        }
        this.neighbors.clear();
    }

    public void linkNeighbors() {
        for (int i = 0; i < this.isoChunkRegions.size(); ++i) {
            IsoChunkRegion c = this.isoChunkRegions.get(i);
            for (int j = 0; j < c.getAllNeighbors().size(); ++j) {
                IsoChunkRegion n = c.getAllNeighbors().get(j);
                if (n.getIsoWorldRegion() == null || n.getIsoWorldRegion() == this) continue;
                this.addNeighbor(n.getIsoWorldRegion());
                n.getIsoWorldRegion().addNeighbor(this);
            }
        }
    }

    private void addNeighbor(IsoWorldRegion mr) {
        if (!this.neighbors.contains(mr)) {
            this.neighbors.add(mr);
        }
    }

    private void removeNeighbor(IsoWorldRegion mr) {
        this.neighbors.remove(mr);
    }

    @Override
    public ArrayList<IsoWorldRegion> getNeighbors() {
        return this.neighbors;
    }

    @Override
    public ArrayList<IsoWorldRegion> getDebugConnectedNeighborCopy() {
        ArrayList<IsoWorldRegion> copy = new ArrayList<IsoWorldRegion>();
        if (this.neighbors.isEmpty()) {
            return copy;
        }
        copy.addAll(this.neighbors);
        return copy;
    }

    @Override
    public boolean isFogMask() {
        return this.isEnclosed() && this.isFullyRoofed();
    }

    @Override
    public boolean isPlayerRoom() {
        return this.isFogMask();
    }

    @Override
    public boolean isFullyRoofed() {
        return this.roofCnt == this.squareSize;
    }

    public float getRoofedPercentage() {
        if (this.squareSize == 0) {
            return 0.0f;
        }
        return (float)this.roofCnt / (float)this.squareSize;
    }

    @Override
    public int getRoofCnt() {
        return this.roofCnt;
    }

    protected void addRoof() {
        ++this.roofCnt;
        if (this.roofCnt > this.squareSize) {
            IsoRegions.warn("WorldRegion.addRoof roofCount exceed squareSize.");
            this.roofCnt = this.squareSize;
        }
    }

    protected void removeRoofs(int roofs) {
        if (roofs <= 0) {
            return;
        }
        this.roofCnt -= roofs;
        if (this.roofCnt < 0) {
            IsoRegions.warn("MasterRegion.removeRoofs Roofcount managed to get below zero.");
            this.roofCnt = 0;
        }
    }

    public void addIsoChunkRegion(IsoChunkRegion region) {
        if (!this.isoChunkRegions.contains(region)) {
            this.squareSize += region.getSquareSize();
            this.roofCnt += region.getRoofCnt();
            this.isDirtyEnclosed = true;
            this.isoChunkRegions.add(region);
            region.setIsoWorldRegion(this);
        }
    }

    protected void removeIsoChunkRegion(IsoChunkRegion region) {
        if (this.isoChunkRegions.remove(region)) {
            this.squareSize -= region.getSquareSize();
            this.roofCnt -= region.getRoofCnt();
            this.isDirtyEnclosed = true;
            region.setIsoWorldRegion(null);
        }
    }

    public boolean containsIsoChunkRegion(IsoChunkRegion region) {
        return this.isoChunkRegions.contains(region);
    }

    public ArrayList<IsoChunkRegion> swapIsoChunkRegions(ArrayList<IsoChunkRegion> newlist) {
        ArrayList<IsoChunkRegion> oldlist = this.isoChunkRegions;
        this.isoChunkRegions = newlist;
        return oldlist;
    }

    protected void resetSquareSize() {
        this.squareSize = 0;
    }

    protected void setDirtyEnclosed() {
        this.isDirtyEnclosed = true;
    }

    public boolean isEnclosed() {
        if (this.isDirtyEnclosed) {
            this.recalcEnclosed();
        }
        return this.enclosed;
    }

    private void recalcEnclosed() {
        this.isDirtyEnclosed = false;
        this.enclosed = true;
        for (int i = 0; i < this.isoChunkRegions.size(); ++i) {
            IsoChunkRegion region = this.isoChunkRegions.get(i);
            if (region.getIsEnclosed()) continue;
            this.enclosed = false;
        }
    }

    public void merge(IsoWorldRegion other) {
        int i;
        if (!other.isoChunkRegions.isEmpty()) {
            for (i = other.isoChunkRegions.size() - 1; i >= 0; --i) {
                IsoChunkRegion c = other.isoChunkRegions.get(i);
                other.removeIsoChunkRegion(c);
                this.addIsoChunkRegion(c);
            }
            this.isDirtyEnclosed = true;
            other.isoChunkRegions.clear();
        }
        if (!other.neighbors.isEmpty()) {
            for (i = 0; i < other.neighbors.size(); ++i) {
                IsoWorldRegion otherNeighbor = other.neighbors.get(i);
                otherNeighbor.removeNeighbor(other);
                this.addNeighbor(otherNeighbor);
            }
            other.neighbors.clear();
        }
        this.manager.releaseIsoWorldRegion(other);
    }

    @Override
    public ArrayList<IsoChunkRegion> getDebugIsoChunkRegionCopy() {
        return new ArrayList<IsoChunkRegion>(this.isoChunkRegions);
    }

    public int getCellX() {
        if (this.isoChunkRegions.isEmpty()) {
            throw new RuntimeException("isoChunkRegions is empty");
        }
        IsoChunkRegion chunkRegion = this.isoChunkRegions.get(0);
        return chunkRegion.getDataChunk().getCellX();
    }

    public int getCellY() {
        if (this.isoChunkRegions.isEmpty()) {
            throw new RuntimeException("isoChunkRegions is empty");
        }
        IsoChunkRegion chunkRegion = this.isoChunkRegions.get(0);
        return chunkRegion.getDataChunk().getCellY();
    }

    public void setBuildingDef(BuildingDef buildingDef) {
        this.buildingDef = buildingDef;
    }

    public BuildingDef getBuildingDef() {
        return this.buildingDef;
    }

    public void clearBuildingDef(ArrayList<IsoGameCharacter.Location> changedCells) {
        if (this.getBuildingDef() == null) {
            return;
        }
        this.setBuildingDef(null);
        for (int i = 0; i < this.isoChunkRegions.size(); ++i) {
            IsoChunkRegion chunkRegion = this.isoChunkRegions.get(i);
            DataChunk dataChunk = chunkRegion.getDataChunk();
            tempLocation.set(dataChunk.getCellX(), dataChunk.getCellY(), 0);
            if (changedCells.contains(tempLocation)) continue;
            IsoGameCharacter.Location location = new IsoGameCharacter.Location(dataChunk.getCellX(), dataChunk.getCellY(), 0);
            changedCells.add(location);
        }
    }
}

