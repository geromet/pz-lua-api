/*
 * Decompiled with CFR 0.152.
 */
package zombie.worldMap;

import java.io.File;
import java.util.ArrayList;
import zombie.ZomboidFileSystem;
import zombie.asset.Asset;
import zombie.asset.AssetStateObserver;
import zombie.core.math.PZMath;
import zombie.inventory.types.MapItem;
import zombie.iso.IsoMetaGrid;
import zombie.iso.IsoWorld;
import zombie.iso.MapFiles;
import zombie.util.StringUtils;
import zombie.worldMap.WorldMapCell;
import zombie.worldMap.WorldMapData;
import zombie.worldMap.WorldMapImages;
import zombie.worldMap.WorldMapSettings;
import zombie.worldMap.WorldMapVisited;
import zombie.worldMap.streets.IWorldMapStreetListener;
import zombie.worldMap.streets.WorldMapStreet;
import zombie.worldMap.streets.WorldMapStreets;
import zombie.worldMap.symbols.MapSymbolDefinitions;

public final class WorldMap
implements AssetStateObserver,
IWorldMapStreetListener {
    public final ArrayList<WorldMapData> data = new ArrayList();
    public final ArrayList<WorldMapImages> images = new ArrayList();
    public final ArrayList<WorldMapStreets> streetData = new ArrayList();
    public final WorldMapStreets combinedStreets = new WorldMapStreets("combined", "combined");
    public int minDataX;
    public int minDataY;
    public int maxDataX;
    public int maxDataY;
    public int minX;
    public int minY;
    public int maxX;
    public int maxY;
    private boolean boundsFromData;
    public final ArrayList<WorldMapData> lastDataInDirectory = new ArrayList();

    public void setBoundsInCells(int minX, int minY, int maxX, int maxY) {
        this.setBoundsInSquares(minX * 256, minY * 256, maxX * 256 + 256 - 1, maxY * 256 + 256 - 1);
    }

    public void setBoundsInSquares(int minX, int minY, int maxX, int maxY) {
        this.minX = minX;
        this.minY = minY;
        this.maxX = maxX;
        this.maxY = maxY;
    }

    public void setBoundsFromData() {
        this.boundsFromData = true;
        this.setBoundsInCells(this.minDataX, this.minDataY, this.maxDataX, this.maxDataY);
    }

    public void setBoundsFromWorld() {
        IsoMetaGrid metaGrid = IsoWorld.instance.getMetaGrid();
        this.setBoundsInCells(metaGrid.getMinX(), metaGrid.getMinY(), metaGrid.getMaxX(), metaGrid.getMaxY());
    }

    public void addData(String fileName) {
        if (StringUtils.isNullOrWhitespace(fileName)) {
            return;
        }
        String absFileName = ZomboidFileSystem.instance.getString(fileName);
        WorldMapData data = WorldMapData.getOrCreateData(absFileName);
        if (data != null && !this.data.contains(data)) {
            data.relativeFileName = fileName;
            this.data.add(data);
            data.getObserverCb().add(this);
            if (data.isReady()) {
                this.updateDataBounds();
            }
        }
    }

    public int getDataCount() {
        return this.data.size();
    }

    public WorldMapData getDataByIndex(int index) {
        return this.data.get(index);
    }

    public boolean isDataLoaded() {
        for (int i = 0; i < this.getDataCount(); ++i) {
            WorldMapData data = this.getDataByIndex(i);
            if (!data.isEmpty()) continue;
            return false;
        }
        return true;
    }

    public void clearData() {
        for (WorldMapData data : this.data) {
            data.getObserverCb().remove(this);
        }
        this.data.clear();
        this.lastDataInDirectory.clear();
        this.updateDataBounds();
    }

    public void endDirectoryData() {
        WorldMapData data;
        if (this.hasData() && !this.lastDataInDirectory.contains(data = this.getDataByIndex(this.getDataCount() - 1))) {
            this.lastDataInDirectory.add(data);
        }
    }

    public boolean isLastDataInDirectory(WorldMapData data) {
        return this.lastDataInDirectory.contains(data);
    }

    private void updateDataBounds() {
        this.minDataX = Integer.MAX_VALUE;
        this.minDataY = Integer.MAX_VALUE;
        this.maxDataX = Integer.MIN_VALUE;
        this.maxDataY = Integer.MIN_VALUE;
        for (int i = 0; i < this.data.size(); ++i) {
            WorldMapData data = this.data.get(i);
            if (!data.isReady()) continue;
            this.minDataX = Math.min(this.minDataX, data.minX);
            this.minDataY = Math.min(this.minDataY, data.minY);
            this.maxDataX = Math.max(this.maxDataX, data.maxX);
            this.maxDataY = Math.max(this.maxDataY, data.maxY);
        }
        if (this.minDataX > this.maxDataX) {
            this.maxDataY = 0;
            this.minDataY = 0;
            this.maxDataX = 0;
            this.minDataX = 0;
        }
    }

    public boolean hasData() {
        return !this.data.isEmpty();
    }

    public void addImages(String directory) {
        String zipFile;
        if (StringUtils.isNullOrWhitespace(directory)) {
            return;
        }
        WorldMapImages images = WorldMapImages.getOrCreate(directory);
        if (images != null && !this.images.contains(images)) {
            this.images.add(images);
        }
        if ((images = WorldMapImages.getOrCreateWithFileName(zipFile = ZomboidFileSystem.instance.getString(directory + "/forest.pyramid.zip"))) != null && !this.images.contains(images)) {
            this.images.add(images);
        }
    }

    public void addImagePyramid(String absolutePath) {
        if (StringUtils.isNullOrWhitespace(absolutePath)) {
            return;
        }
        WorldMapImages images = WorldMapImages.getOrCreateWithFileName(absolutePath);
        if (images != null && !this.images.contains(images)) {
            this.images.add(images);
        }
    }

    public boolean hasImages() {
        return !this.images.isEmpty();
    }

    public int getImagesCount() {
        return this.images.size();
    }

    public WorldMapImages getImagesByIndex(int index) {
        return this.images.get(index);
    }

    public void clearImages() {
        this.images.clear();
    }

    public WorldMapImages getWorldMapImagesByFileName(String absolutePath) {
        if (StringUtils.isNullOrWhitespace(absolutePath)) {
            return null;
        }
        for (int i = 0; i < this.getImagesCount(); ++i) {
            WorldMapImages worldMapImages = this.getImagesByIndex(i);
            if (!absolutePath.equalsIgnoreCase(worldMapImages.getAbsolutePath())) continue;
            return worldMapImages;
        }
        return null;
    }

    public void addStreetData(String relativeFileName) {
        Object absFileName;
        if (StringUtils.isNullOrWhitespace(relativeFileName)) {
            return;
        }
        if (ZomboidFileSystem.instance.isKnownFile(relativeFileName)) {
            absFileName = ZomboidFileSystem.instance.getString(relativeFileName);
        } else {
            int index = PZMath.max(relativeFileName.lastIndexOf(47), relativeFileName.lastIndexOf(File.separator));
            String relativeDir = relativeFileName.substring(0, index + 1);
            if (!ZomboidFileSystem.instance.isKnownFile(relativeDir)) {
                return;
            }
            Object absoluteDir = ZomboidFileSystem.instance.getString(relativeDir);
            if (!((String)absoluteDir).endsWith(File.separator) && !((String)absoluteDir).endsWith("/")) {
                absoluteDir = (String)absoluteDir + File.separator;
            }
            absFileName = (String)absoluteDir + relativeFileName.substring(index + 1);
        }
        WorldMapStreets data = WorldMapStreets.getOrCreateData(relativeFileName, (String)absFileName);
        if (data != null && !this.streetData.contains(data)) {
            this.streetData.add(data);
            this.combinedStreets.combine(data);
            data.addListener(this);
        }
    }

    public int getStreetDataCount() {
        return this.streetData.size();
    }

    public WorldMapStreets getStreetDataByIndex(int index) {
        return this.streetData.get(index);
    }

    public WorldMapStreets getStreetDataByRelativeFileName(String relativeFileName) {
        for (int i = 0; i < this.getStreetDataCount(); ++i) {
            WorldMapStreets streets = this.getStreetDataByIndex(i);
            if (!streets.getRelativeFileName().equalsIgnoreCase(relativeFileName)) continue;
            return streets;
        }
        return null;
    }

    public void clearStreetData() {
        for (int i = 0; i < this.streetData.size(); ++i) {
            this.streetData.get(i).removeListener(this);
        }
        this.streetData.clear();
        this.combinedStreets.clear();
    }

    public int getMinXInCells() {
        return this.minX / 256;
    }

    public int getMinYInCells() {
        return this.minY / 256;
    }

    public int getMaxXInCells() {
        return this.maxX / 256;
    }

    public int getMaxYInCells() {
        return this.maxY / 256;
    }

    public int getWidthInCells() {
        return this.getMaxXInCells() - this.getMinXInCells() + 1;
    }

    public int getHeightInCells() {
        return this.getMaxYInCells() - this.getMinYInCells() + 1;
    }

    public int getMinXInSquares() {
        return this.minX;
    }

    public int getMinYInSquares() {
        return this.minY;
    }

    public int getMaxXInSquares() {
        return this.maxX;
    }

    public int getMaxYInSquares() {
        return this.maxY;
    }

    public int getWidthInSquares() {
        return this.maxX - this.minX + 1;
    }

    public int getHeightInSquares() {
        return this.maxY - this.minY + 1;
    }

    public WorldMapCell getCell(int x, int y) {
        for (int i = 0; i < this.data.size(); ++i) {
            WorldMapCell cell;
            WorldMapData data = this.data.get(i);
            if (!data.isReady() || (cell = data.getCell(x, y)) == null) continue;
            return cell;
        }
        return null;
    }

    public int getDataWidthInCells() {
        return this.maxDataX - this.minDataX + 1;
    }

    public int getDataHeightInCells() {
        return this.maxDataY - this.minDataY + 1;
    }

    public int getDataWidthInSquares() {
        return this.getDataWidthInCells() * 256;
    }

    public int getDataHeightInSquares() {
        return this.getDataHeightInCells() * 256;
    }

    public static void Reset() {
        WorldMapSettings.Reset();
        WorldMapVisited.Reset();
        WorldMapData.Reset();
        WorldMapImages.Reset();
        MapSymbolDefinitions.Reset();
        MapItem.Reset();
        MapFiles.Reset();
    }

    @Override
    public void onStateChanged(Asset.State oldState, Asset.State newState, Asset asset) {
        this.updateDataBounds();
        if (this.boundsFromData) {
            this.setBoundsInCells(this.minDataX, this.minDataY, this.maxDataX, this.maxDataY);
        }
    }

    @Override
    public void onAdd(WorldMapStreet street) {
        this.combinedStreets.setDirty(true);
    }

    @Override
    public void onBeforeRemove(WorldMapStreet street) {
        this.combinedStreets.setDirty(true);
    }

    @Override
    public void onAfterRemove(WorldMapStreet street) {
        this.combinedStreets.setDirty(true);
    }

    @Override
    public void onBeforeModifyStreet(WorldMapStreet street) {
        this.combinedStreets.setDirty(true);
    }

    @Override
    public void onAfterModifyStreet(WorldMapStreet street) {
        this.combinedStreets.setDirty(true);
    }
}

