/*
 * Decompiled with CFR 0.152.
 */
package zombie.basements;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import zombie.UsedFromLua;
import zombie.ZomboidFileSystem;
import zombie.basements.BasementDefinition;
import zombie.basements.BasementOverlap;
import zombie.basements.BasementPlacement;
import zombie.basements.BasementSpawnLocation;
import zombie.basements.BasementsPerMap;
import zombie.basements.BasementsV1;
import zombie.buildingRooms.BuildingRoomsEditor;
import zombie.core.Core;
import zombie.core.logger.ExceptionLogger;
import zombie.core.math.PZMath;
import zombie.core.random.RandInterface;
import zombie.core.random.RandSeeded;
import zombie.debug.DebugLog;
import zombie.iso.BuildingDef;
import zombie.iso.BuildingID;
import zombie.iso.IsoChunk;
import zombie.iso.IsoLot;
import zombie.iso.IsoMetaCell;
import zombie.iso.IsoMetaGrid;
import zombie.iso.IsoWorld;
import zombie.iso.LotHeader;
import zombie.iso.NewMapBinaryFile;
import zombie.iso.RoomDef;
import zombie.iso.RoomID;
import zombie.iso.areas.IsoBuilding;
import zombie.iso.areas.IsoRoom;
import zombie.iso.worldgen.WorldGenParams;
import zombie.util.StringUtils;
import zombie.util.list.PZArrayUtil;

@UsedFromLua
public final class Basements {
    private static Basements instance;
    final ArrayList<BasementDefinition> basementDefinitions = new ArrayList();
    final HashMap<String, BasementDefinition> basementDefinitionByName = new HashMap();
    final ArrayList<BasementDefinition> basementAccessDefinitions = new ArrayList();
    final HashMap<String, BasementDefinition> basementAccessDefinitionByName = new HashMap();
    final ArrayList<BasementSpawnLocation> basementSpawnLocations = new ArrayList();
    final ArrayList<BasementPlacement> basementPlacements = new ArrayList();
    private final HashMap<String, BasementsPerMap> basementsPerMap = new HashMap();
    private final BasementsV1 apiV1 = new BasementsV1();
    public static final int SAVEFILE_VERSION = 1;
    private static final byte[] FILE_MAGIC;
    private final ArrayList<BuildingDef> buildingDefs = new ArrayList();
    private final ArrayList<BuildingDef> tempBuildingDefs = new ArrayList();
    private final ArrayList<RoomDef> tempRooms = new ArrayList();
    private final HashMap<BuildingDef, MergedRooms> mergedRooms = new HashMap();

    public static Basements getInstance() {
        if (instance == null) {
            instance = new Basements();
        }
        return instance;
    }

    public static BasementsV1 getAPIv1() {
        return Basements.getInstance().apiV1;
    }

    public BasementsPerMap getPerMap(String mapID) {
        if (StringUtils.isNullOrWhitespace(mapID)) {
            throw new IllegalArgumentException("invalid mapID \"%s\"".formatted(mapID));
        }
        mapID = mapID.trim();
        return this.basementsPerMap.get(mapID);
    }

    public BasementsPerMap getOrCreatePerMap(String mapID) {
        BasementsPerMap basementsPerMap1 = this.getPerMap(mapID);
        if (basementsPerMap1 == null) {
            mapID = mapID.trim();
            basementsPerMap1 = new BasementsPerMap(mapID);
            this.basementsPerMap.put(mapID, basementsPerMap1);
        }
        return basementsPerMap1;
    }

    public void beforeOnLoadMapZones() {
        this.basementSpawnLocations.clear();
        for (BasementsPerMap basementsPerMap1 : this.basementsPerMap.values()) {
            basementsPerMap1.basementDefinitions.clear();
            basementsPerMap1.basementDefinitionByName.clear();
            basementsPerMap1.basementAccessDefinitions.clear();
            basementsPerMap1.basementAccessDefinitionByName.clear();
            basementsPerMap1.basementSpawnLocations.clear();
        }
        this.basementsPerMap.clear();
    }

    public void beforeLoadMetaGrid() {
        this.basementDefinitions.clear();
        this.basementDefinitionByName.clear();
        this.basementAccessDefinitions.clear();
        this.basementAccessDefinitionByName.clear();
        this.basementPlacements.clear();
        this.buildingDefs.clear();
        this.parseBasementDefinitions();
        this.parseBasementAccessDefinitions();
        this.parseBasementSpawnLocations();
        if (this.loadSavefile()) {
            BuildingRoomsEditor.getInstance().checkBuildingAndRoomIDs();
            this.addBasementBuildingDefsToLotHeaders();
            BuildingRoomsEditor.getInstance().checkBuildingAndRoomIDs();
            this.addBasementBuildingDefsToMetaGrid();
            BuildingRoomsEditor.getInstance().checkBuildingAndRoomIDs();
            return;
        }
        this.loadBasementDefinitionHeaders();
        this.loadBasementAccessDefinitionHeaders();
        this.calculateBasementPlacements();
        this.createBasementBuildingDefs();
        this.writeSavefile();
        this.addBasementBuildingDefsToLotHeaders();
        this.addBasementBuildingDefsToMetaGrid();
    }

    public void afterLoadMetaGrid() {
        this.basementSpawnLocations.clear();
        for (BasementDefinition basementDefinition : this.basementDefinitions) {
            if (basementDefinition.header == null) continue;
            basementDefinition.header.Dispose();
            basementDefinition.header = null;
        }
        this.basementDefinitions.clear();
        this.basementDefinitionByName.clear();
        for (BasementDefinition basementDefinition : this.basementAccessDefinitions) {
            if (basementDefinition.header == null) continue;
            basementDefinition.header.Dispose();
            basementDefinition.header = null;
        }
        this.basementAccessDefinitions.clear();
        this.basementAccessDefinitionByName.clear();
    }

    /*
     * Enabled aggressive exception aggregation
     */
    boolean loadSavefile() {
        File file = ZomboidFileSystem.instance.getFileInCurrentSave("map_basements.bin");
        try (FileInputStream fis2 = new FileInputStream(file);){
            boolean bl;
            try (DataInputStream dis = new DataInputStream(fis2);){
                this.loadSavefile(file, dis);
                bl = true;
            }
            return bl;
        }
        catch (FileNotFoundException fis2) {
        }
        catch (Exception ex) {
            ExceptionLogger.logException(ex);
        }
        return false;
    }

    void loadSavefile(File file, DataInputStream in) throws IOException {
        byte[] magic = new byte[4];
        int byteCount = in.read(magic);
        if (byteCount < 4 || !Arrays.equals(magic, FILE_MAGIC)) {
            throw new IOException(file.getAbsolutePath() + " does not appear to be map_basements.bin");
        }
        in.readInt();
        in.readInt();
        int placementCount = in.readInt();
        for (int i = 0; i < placementCount; ++i) {
            BasementPlacement basementPlacement = new BasementPlacement();
            basementPlacement.x = in.readInt();
            basementPlacement.y = in.readInt();
            basementPlacement.z = in.readInt();
            basementPlacement.w = in.readShort();
            basementPlacement.h = in.readShort();
            basementPlacement.name = in.readUTF();
            this.basementPlacements.add(basementPlacement);
        }
        ArrayList<RoomDef> tempRooms = new ArrayList<RoomDef>();
        int buildingCount = in.readInt();
        for (int buildingIndex = 0; buildingIndex < buildingCount; ++buildingIndex) {
            BuildingDef buildingDef = new BuildingDef();
            buildingDef.id = buildingIndex;
            int roomCount = in.readShort();
            for (int roomIndex = 0; roomIndex < roomCount; ++roomIndex) {
                String name = in.readUTF();
                RoomDef roomDef = new RoomDef(roomIndex, name);
                roomDef.building = buildingDef;
                roomDef.level = in.readByte();
                int rectCount = in.readShort();
                for (int rectIndex = 0; rectIndex < rectCount; ++rectIndex) {
                    short x = in.readShort();
                    short y = in.readShort();
                    short w = in.readShort();
                    short h = in.readShort();
                    RoomDef.RoomRect roomRect = new RoomDef.RoomRect(x, y, w, h);
                    roomDef.rects.add(roomRect);
                }
                roomDef.CalculateBounds();
                buildingDef.rooms.add(roomDef);
            }
            buildingDef.CalculateBounds(tempRooms);
            this.buildingDefs.add(buildingDef);
        }
    }

    void writeSavefile() {
        if (Core.getInstance().isNoSave()) {
            return;
        }
        File file = ZomboidFileSystem.instance.getFileInCurrentSave("map_basements.bin");
        try (FileOutputStream fos = new FileOutputStream(file);
             DataOutputStream out = new DataOutputStream(fos);){
            this.writeSavefile(out);
        }
        catch (Exception ex) {
            ExceptionLogger.logException(ex);
        }
    }

    void writeSavefile(DataOutputStream out) throws IOException {
        out.write(FILE_MAGIC);
        out.writeInt(244);
        out.writeInt(1);
        out.writeInt(this.basementPlacements.size());
        for (int i = 0; i < this.basementPlacements.size(); ++i) {
            BasementPlacement basementPlacement = this.basementPlacements.get(i);
            out.writeInt(basementPlacement.x);
            out.writeInt(basementPlacement.y);
            out.writeInt(basementPlacement.z);
            out.writeShort(basementPlacement.w);
            out.writeShort(basementPlacement.h);
            out.writeUTF(basementPlacement.name);
        }
        out.writeInt(this.buildingDefs.size());
        for (int buildingIndex = 0; buildingIndex < this.buildingDefs.size(); ++buildingIndex) {
            BuildingDef buildingDef = this.buildingDefs.get(buildingIndex);
            out.writeShort(buildingDef.rooms.size());
            for (int roomIndex = 0; roomIndex < buildingDef.rooms.size(); ++roomIndex) {
                RoomDef roomDef = buildingDef.rooms.get(roomIndex);
                out.writeUTF(roomDef.name);
                out.writeByte(roomDef.level);
                out.writeShort(roomDef.rects.size());
                for (int rectIndex = 0; rectIndex < roomDef.rects.size(); ++rectIndex) {
                    RoomDef.RoomRect roomRect = roomDef.rects.get(rectIndex);
                    out.writeShort(roomRect.x);
                    out.writeShort(roomRect.y);
                    out.writeShort(roomRect.w);
                    out.writeShort(roomRect.h);
                }
            }
        }
    }

    public void parseBasementDefinitions() {
        ArrayList<String> lotDirectories = IsoWorld.instance.getMetaGrid().getLotDirectories();
        for (String lotDirectory : lotDirectories) {
            BasementsPerMap basementsPerMap1 = this.getPerMap(lotDirectory);
            if (basementsPerMap1 == null) continue;
            this.basementDefinitions.addAll(basementsPerMap1.basementDefinitions);
            this.basementDefinitionByName.putAll(basementsPerMap1.basementDefinitionByName);
        }
    }

    public void parseBasementAccessDefinitions() {
        ArrayList<String> lotDirectories = IsoWorld.instance.getMetaGrid().getLotDirectories();
        for (String lotDirectory : lotDirectories) {
            BasementsPerMap basementsPerMap1 = this.getPerMap(lotDirectory);
            if (basementsPerMap1 == null) continue;
            this.basementAccessDefinitions.addAll(basementsPerMap1.basementAccessDefinitions);
            this.basementAccessDefinitionByName.putAll(basementsPerMap1.basementAccessDefinitionByName);
        }
    }

    void parseBasementSpawnLocations() {
        ArrayList<String> lotDirectories = IsoWorld.instance.getMetaGrid().getLotDirectories();
        for (String lotDirectory : lotDirectories) {
            BasementsPerMap basementsPerMap1 = this.getPerMap(lotDirectory);
            if (basementsPerMap1 == null) continue;
            this.basementSpawnLocations.addAll(basementsPerMap1.basementSpawnLocations);
        }
    }

    void calculateBasementPlacements() {
        RandSeeded rnd = new RandSeeded(WorldGenParams.INSTANCE.getSeed());
        BasementOverlap overlap = new BasementOverlap();
        for (BasementsPerMap basementsPerMap1 : this.basementsPerMap.values()) {
            this.calculateBasementPlacements(overlap, basementsPerMap1, rnd);
        }
        overlap.Dispose();
    }

    void calculateBasementPlacements(BasementOverlap overlap, BasementsPerMap basementsPerMap1, RandInterface rnd) {
        ArrayList<BasementDefinition> choices = new ArrayList<BasementDefinition>();
        for (int i = 0; i < basementsPerMap1.basementSpawnLocations.size(); ++i) {
            BasementDefinition basementAccessDefinition;
            BasementSpawnLocation basementSpawnLocation = basementsPerMap1.basementSpawnLocations.get(i);
            choices.clear();
            if (basementSpawnLocation.specificBasement == null || basementSpawnLocation.specificBasement.isEmpty()) {
                for (j = 0; j < basementsPerMap1.basementDefinitions.size(); ++j) {
                    BasementDefinition basementDefinition = basementsPerMap1.basementDefinitions.get(j);
                    if (!this.canPlaceAt(basementsPerMap1.mapId, basementDefinition, basementSpawnLocation, overlap)) continue;
                    choices.add(basementDefinition);
                }
            } else {
                for (j = 0; j < basementSpawnLocation.specificBasement.size(); ++j) {
                    String basementName = basementSpawnLocation.specificBasement.get(j);
                    BasementDefinition basementDefinition = basementsPerMap1.basementDefinitionByName.get(basementName);
                    if (basementDefinition == null || basementDefinition.header == null || !this.canPlaceAt(basementsPerMap1.mapId, basementDefinition, basementSpawnLocation, overlap)) continue;
                    choices.add(basementDefinition);
                }
            }
            if (choices.isEmpty()) continue;
            BasementDefinition basementDefinition = (BasementDefinition)PZArrayUtil.pickRandom(choices, rnd);
            BasementPlacement basementPlacement = new BasementPlacement();
            if (basementSpawnLocation.w > 1 && basementSpawnLocation.h > 1) {
                int topStairX = basementSpawnLocation.x + basementSpawnLocation.stairX;
                int topStairY = basementSpawnLocation.y + basementSpawnLocation.stairY;
                basementPlacement.x = topStairX - basementDefinition.stairx;
                basementPlacement.y = topStairY - basementDefinition.stairy;
            } else {
                basementPlacement.x = basementSpawnLocation.x - basementDefinition.stairx;
                basementPlacement.y = basementSpawnLocation.y - basementDefinition.stairy;
            }
            basementPlacement.z = basementSpawnLocation.z - basementDefinition.header.levels;
            basementPlacement.w = basementDefinition.width;
            basementPlacement.h = basementDefinition.height;
            basementPlacement.name = basementDefinition.name;
            this.basementPlacements.add(basementPlacement);
            overlap.addBasement(basementDefinition.header.buildingDefList.get(0), basementPlacement.x, basementPlacement.y, basementPlacement.z);
            if (basementSpawnLocation.access == null || (basementAccessDefinition = this.basementAccessDefinitionByName.get(basementSpawnLocation.access)) == null || basementAccessDefinition.header == null) continue;
            BasementPlacement basementPlacement2 = new BasementPlacement();
            basementPlacement2.x = basementPlacement.x + basementDefinition.stairx - basementAccessDefinition.stairx;
            basementPlacement2.y = basementPlacement.y + basementDefinition.stairy - basementAccessDefinition.stairy;
            basementPlacement2.z = basementSpawnLocation.z;
            basementPlacement2.w = basementAccessDefinition.width;
            basementPlacement2.h = basementAccessDefinition.height;
            basementPlacement2.name = basementAccessDefinition.name;
            this.basementPlacements.add(basementPlacement2);
        }
    }

    boolean canPlaceAt(String mapID, BasementDefinition basementDefinition, BasementSpawnLocation basementSpawnLocation, BasementOverlap overlap) {
        if (basementDefinition.north != basementSpawnLocation.north) {
            return false;
        }
        int topStairX = basementSpawnLocation.x + basementSpawnLocation.stairX;
        int topStairY = basementSpawnLocation.y + basementSpawnLocation.stairY;
        int basementX = topStairX - basementDefinition.stairx;
        int basementY = topStairY - basementDefinition.stairy;
        int basementZ = basementSpawnLocation.z - basementDefinition.header.levels;
        if (basementSpawnLocation.w > 1 && basementSpawnLocation.h > 1) {
            boolean bBasementInsideSpawnBounds;
            boolean bl = bBasementInsideSpawnBounds = basementX >= basementSpawnLocation.x && basementX + basementDefinition.width <= basementSpawnLocation.x + basementSpawnLocation.w && basementY >= basementSpawnLocation.y && basementY + basementDefinition.height <= basementSpawnLocation.y + basementSpawnLocation.h;
            if (!bBasementInsideSpawnBounds) {
                return false;
            }
        }
        if (!this.isCellFromThisMap(mapID, basementSpawnLocation.x, basementSpawnLocation.y)) {
            return false;
        }
        return !overlap.checkOverlap(basementDefinition.header.buildingDefList.get(0), basementX, basementY, basementZ);
    }

    boolean isCellFromThisMap(String mapID, int spawnX, int spawnY) {
        int cellX = spawnX / 256;
        int cellY = spawnY / 256;
        LotHeader lotHeader = IsoLot.getHeader(cellX, cellY);
        return lotHeader != null && lotHeader.mapFiles.mapDirectoryName.equalsIgnoreCase(mapID);
    }

    public boolean chunkHasBasement(IsoChunk chunk) {
        for (int i = 0; i < this.basementPlacements.size(); ++i) {
            BasementPlacement basementPlacement = this.basementPlacements.get(i);
            int x1 = basementPlacement.x;
            int y1 = basementPlacement.y;
            int w1 = basementPlacement.w + 1;
            int h1 = basementPlacement.h + 1;
            if (!this.chunkOverlaps(chunk, x1, y1, w1, h1)) continue;
            return true;
        }
        return false;
    }

    public void onNewChunkLoaded(IsoChunk chunk) {
        for (int i = 0; i < this.basementPlacements.size(); ++i) {
            BasementPlacement basementPlacement = this.basementPlacements.get(i);
            int x1 = basementPlacement.x;
            int y1 = basementPlacement.y;
            int w1 = basementPlacement.w + 1;
            int h1 = basementPlacement.h + 1;
            if (!this.chunkOverlaps(chunk, x1, y1, w1, h1)) continue;
            try {
                NewMapBinaryFile.SpawnBasementInChunk(chunk, basementPlacement.name, basementPlacement.x, basementPlacement.y, basementPlacement.z);
                continue;
            }
            catch (IOException e) {
                ExceptionLogger.logException(e);
            }
        }
    }

    boolean chunkOverlaps(IsoChunk chunk, int x, int y, int w, int h) {
        int chunksPerWidth = 8;
        int cx = chunk.wx * 8;
        int cy = chunk.wy * 8;
        return x + w > cx && x < cx + 8 && y + h > cy && y < cy + 8;
    }

    void loadBasementDefinitionHeaders() {
        NewMapBinaryFile file = new NewMapBinaryFile(true);
        for (int i = 0; i < this.basementDefinitions.size(); ++i) {
            BasementDefinition basementDefinition = this.basementDefinitions.get(i);
            String fileName = "media/binmap/" + basementDefinition.name + ".pzby";
            try {
                NewMapBinaryFile.Header header = file.loadHeader(fileName);
                if (header.buildingDefList.size() != 1) {
                    this.basementDefinitions.remove(i--);
                    this.basementDefinitionByName.remove(basementDefinition.name);
                    continue;
                }
                basementDefinition.header = header;
                BuildingDef buildingDef = header.buildingDefList.get(0);
                basementDefinition.width = buildingDef.getW();
                basementDefinition.height = buildingDef.getH();
                continue;
            }
            catch (IOException e) {
                this.basementDefinitions.remove(i--);
                this.basementDefinitionByName.remove(basementDefinition.name);
            }
        }
    }

    void loadBasementAccessDefinitionHeaders() {
        NewMapBinaryFile file = new NewMapBinaryFile(true);
        for (int i = 0; i < this.basementAccessDefinitions.size(); ++i) {
            BasementDefinition basementDefinition = this.basementAccessDefinitions.get(i);
            String fileName = "media/basement_access/" + basementDefinition.name + ".pzby";
            try {
                basementDefinition.header = file.loadHeader(fileName);
                continue;
            }
            catch (IOException e) {
                this.basementAccessDefinitions.remove(i--);
                this.basementAccessDefinitionByName.remove(basementDefinition.name);
            }
        }
    }

    void createBasementBuildingDefs() {
        ArrayList<RoomDef> tempRooms = new ArrayList<RoomDef>();
        for (int i = 0; i < this.basementPlacements.size(); ++i) {
            int cellY;
            int cellX;
            IsoMetaCell metaCell;
            BasementPlacement basementPlacement = this.basementPlacements.get(i);
            BasementDefinition basementDefinition = this.basementDefinitionByName.get(basementPlacement.name);
            if (basementDefinition == null) {
                basementDefinition = this.basementAccessDefinitionByName.get(basementPlacement.name);
            }
            if (basementDefinition == null) continue;
            NewMapBinaryFile.Header header = basementDefinition.header;
            if (header.buildingDefList.isEmpty() || (metaCell = IsoWorld.instance.metaGrid.getCellData(cellX = basementPlacement.x / 256, cellY = basementPlacement.y / 256)) == null) continue;
            BuildingDef buildingDef = new BuildingDef();
            buildingDef.id = this.buildingDefs.size();
            int basementBottomZ = basementPlacement.z;
            for (int roomIndex = 0; roomIndex < header.roomDefList.size(); ++roomIndex) {
                RoomDef roomDef = header.roomDefList.get(roomIndex);
                long roomID = buildingDef.rooms.size();
                RoomDef newRoomDef = new RoomDef(roomID, roomDef.name);
                newRoomDef.level = basementBottomZ + roomDef.level;
                for (RoomDef.RoomRect rect : roomDef.rects) {
                    newRoomDef.rects.add(new RoomDef.RoomRect(basementPlacement.x + rect.x, basementPlacement.y + rect.y, rect.getW(), rect.getH()));
                }
                newRoomDef.CalculateBounds();
                newRoomDef.building = buildingDef;
                if (roomDef.isEmptyOutside()) {
                    buildingDef.emptyoutside.add(newRoomDef);
                    continue;
                }
                buildingDef.rooms.add(newRoomDef);
            }
            buildingDef.CalculateBounds(tempRooms);
            this.buildingDefs.add(buildingDef);
        }
    }

    private BuildingDef getBuildingToMergeWith(BuildingDef buildingDef) {
        BuildingDef mergeDef = null;
        if (buildingDef.getMinLevel() < 0) {
            return mergeDef;
        }
        this.tempBuildingDefs.clear();
        IsoWorld.instance.metaGrid.getBuildingsIntersecting(buildingDef.getX() - 1, buildingDef.getY() - 1, buildingDef.getW() + 2, buildingDef.getH() + 2, this.tempBuildingDefs);
        for (int i = 0; i < this.tempBuildingDefs.size(); ++i) {
            BuildingDef candidateDef = this.tempBuildingDefs.get(i);
            if (candidateDef.getMinLevel() < 0 || !buildingDef.isAdjacent(candidateDef)) continue;
            if (mergeDef != null) {
                return null;
            }
            mergeDef = candidateDef;
        }
        return mergeDef;
    }

    private void removeBuildingFromMetaCell(BuildingDef buildingDef, IsoMetaCell metaCell) {
        RoomDef roomDef;
        int i;
        metaCell.buildings.remove(buildingDef);
        metaCell.buildingByMetaId.remove(buildingDef.metaId);
        assert (!metaCell.isoBuildings.containsKey(buildingDef.id));
        for (i = 0; i < buildingDef.rooms.size(); ++i) {
            roomDef = buildingDef.rooms.get(i);
            metaCell.rooms.remove(roomDef.id);
            metaCell.roomList.remove(roomDef);
            metaCell.roomByMetaId.remove(roomDef.metaId);
            assert (!metaCell.isoRooms.containsKey(roomDef.id));
        }
        for (i = 0; i < buildingDef.emptyoutside.size(); ++i) {
            roomDef = buildingDef.emptyoutside.get(i);
            metaCell.rooms.remove(roomDef.id);
            metaCell.roomList.remove(roomDef);
            metaCell.roomByMetaId.remove(roomDef.metaId);
            assert (!metaCell.isoRooms.containsKey(roomDef.id));
        }
    }

    void recalculateBuildingAndRoomIDs(IsoMetaCell metaCell) {
        int i;
        int cellX = metaCell.getX();
        int cellY = metaCell.getY();
        metaCell.rooms.clear();
        for (i = 0; i < metaCell.roomList.size(); ++i) {
            RoomDef roomDef = metaCell.roomList.get(i);
            roomDef.id = RoomID.makeID(cellX, cellY, i);
            metaCell.rooms.put(roomDef.id, roomDef);
        }
        for (i = 0; i < metaCell.buildings.size(); ++i) {
            BuildingDef buildingDef = metaCell.buildings.get(i);
            buildingDef.id = BuildingID.makeID(cellX, cellY, i);
        }
        ArrayList<IsoBuilding> isoBuildings = new ArrayList<IsoBuilding>(metaCell.isoBuildings.values());
        metaCell.isoBuildings.clear();
        for (IsoBuilding isoBuilding : isoBuildings) {
            metaCell.isoBuildings.put(isoBuilding.def.id, isoBuilding);
        }
        ArrayList<IsoRoom> isoRooms = new ArrayList<IsoRoom>(metaCell.isoRooms.values());
        metaCell.isoRooms.clear();
        for (IsoRoom isoRoom : isoRooms) {
            metaCell.isoRooms.put(isoRoom.def.id, isoRoom);
        }
    }

    private void mergeRoomsOntoMetaCell(ArrayList<RoomDef> rooms, IsoMetaCell metaCell) {
        for (int roomIndex = 0; roomIndex < rooms.size(); ++roomIndex) {
            RoomDef roomDef = rooms.get(roomIndex);
            roomDef.id = RoomID.makeID(metaCell.getX(), metaCell.getY(), metaCell.roomList.size());
            roomDef.metaId = roomDef.calculateMetaID(metaCell.getX(), metaCell.getY());
            if (metaCell.rooms.containsKey(roomDef.id)) {
                DebugLog.General.error("duplicate RoomDef.ID for room at %d,%d,%d", roomDef.x, roomDef.y, roomDef.level);
            }
            metaCell.rooms.put(roomDef.id, roomDef);
            metaCell.roomList.add(roomDef);
            if (metaCell.roomByMetaId.contains(roomDef.metaId)) {
                DebugLog.General.error("duplicate RoomDef.metaID for room at %d,%d,%d", roomDef.x, roomDef.y, roomDef.level);
            }
            metaCell.roomByMetaId.put(roomDef.metaId, roomDef);
        }
    }

    private void mergeBuildings(BuildingDef buildingDef, BuildingDef mergeDef) {
        int cellX1 = buildingDef.x / 256;
        int cellY1 = buildingDef.y / 256;
        int cellX2 = mergeDef.x / 256;
        int cellY2 = mergeDef.y / 256;
        int cellX = PZMath.min(cellX1, cellX2);
        int cellY = PZMath.min(cellY1, cellY2);
        IsoMetaCell metaCell = IsoWorld.instance.getMetaGrid().getCellData(cellX, cellY);
        if (metaCell == null) {
            return;
        }
        if (cellX != cellX2 || cellY != cellY2) {
            IsoMetaCell metaCell1 = IsoWorld.instance.getMetaGrid().getCellData(cellX2, cellY2);
            if (metaCell1 == null) {
                return;
            }
            this.removeBuildingFromMetaCell(mergeDef, metaCell1);
            this.recalculateBuildingAndRoomIDs(metaCell1);
            mergeDef.id = BuildingID.makeID(metaCell.getX(), metaCell.getY(), metaCell.buildings.size());
            metaCell.buildings.add(mergeDef);
            metaCell.roomList.addAll(mergeDef.rooms);
            metaCell.roomList.addAll(mergeDef.emptyoutside);
            this.recalculateBuildingAndRoomIDs(metaCell);
        }
        this.mergeRoomsOntoMetaCell(buildingDef.rooms, metaCell);
        this.mergeRoomsOntoMetaCell(buildingDef.emptyoutside, metaCell);
        MergedRooms mergedRooms1 = this.mergedRooms.get(mergeDef);
        if (mergedRooms1 == null) {
            mergedRooms1 = new MergedRooms();
            mergedRooms1.buildingDef = mergeDef;
            this.mergedRooms.put(mergeDef, mergedRooms1);
        }
        mergedRooms1.rooms.addAll(buildingDef.rooms);
        mergedRooms1.emptyoutside.addAll(buildingDef.emptyoutside);
        metaCell.buildingByMetaId.remove(mergeDef.metaId);
        mergeDef.addRoomsOf(buildingDef, this.tempRooms);
        mergeDef.metaId = mergeDef.calculateMetaID(cellX, cellY);
        metaCell.buildingByMetaId.put(mergeDef.metaId, mergeDef);
    }

    void addBasementBuildingDefsToLotHeaders() {
        this.mergedRooms.clear();
        for (int i = 0; i < this.buildingDefs.size(); ++i) {
            BuildingDef buildingDef = this.buildingDefs.get(i);
            int cellX = buildingDef.x / 256;
            int cellY = buildingDef.y / 256;
            IsoMetaCell metaCell = IsoWorld.instance.getMetaGrid().getCellData(cellX, cellY);
            if (metaCell == null) continue;
            BuildingDef mergeDef = this.getBuildingToMergeWith(buildingDef);
            if (mergeDef != null) {
                this.mergeBuildings(buildingDef, mergeDef);
                this.buildingDefs.remove(i--);
                continue;
            }
            int buildingIndex = metaCell.buildings.size();
            buildingDef.id = BuildingID.makeID(cellX, cellY, buildingIndex);
            buildingDef.metaId = buildingDef.calculateMetaID(cellX, cellY);
            this.mergeRoomsOntoMetaCell(buildingDef.rooms, metaCell);
            metaCell.buildings.add(buildingDef);
            if (metaCell.buildingByMetaId.contains(buildingDef.metaId)) {
                DebugLog.General.error("duplicate BuildingDef.metaID for building at %d,%d", buildingDef.x, buildingDef.y);
            }
            metaCell.buildingByMetaId.put(buildingDef.metaId, buildingDef);
        }
    }

    void addBasementBuildingDefsToMetaGrid() {
        IsoMetaGrid metaGrid = IsoWorld.instance.getMetaGrid();
        for (int i = 0; i < this.buildingDefs.size(); ++i) {
            BuildingDef buildingDef = this.buildingDefs.get(i);
            int cellX = buildingDef.x / 256;
            int cellY = buildingDef.y / 256;
            IsoMetaCell metaCell = metaGrid.getCellData(cellX, cellY);
            if (metaCell == null) continue;
            metaCell.addRooms(buildingDef.rooms, cellX * 256, cellY * 256);
            metaCell.addRooms(buildingDef.emptyoutside, cellX * 256, cellY * 256);
            metaGrid.addRoomsToAdjacentCells(buildingDef);
            if (metaGrid.buildings.contains(buildingDef)) continue;
            metaGrid.buildings.add(buildingDef);
        }
        for (MergedRooms mergedRooms1 : this.mergedRooms.values()) {
            BuildingDef buildingDef = mergedRooms1.buildingDef;
            int cellX = buildingDef.x / 256;
            int cellY = buildingDef.y / 256;
            IsoMetaCell metaCell = metaGrid.getCellData(cellX, cellY);
            if (metaCell == null) continue;
            metaCell.addRooms(mergedRooms1.rooms, cellX * 256, cellY * 256);
            metaCell.addRooms(mergedRooms1.emptyoutside, cellX * 256, cellY * 256);
            metaGrid.addRoomsToAdjacentCells(buildingDef, mergedRooms1.rooms);
            metaGrid.addRoomsToAdjacentCells(buildingDef, mergedRooms1.emptyoutside);
        }
        this.mergedRooms.clear();
    }

    static {
        FILE_MAGIC = new byte[]{66, 83, 77, 84};
    }

    private static final class MergedRooms {
        BuildingDef buildingDef;
        final ArrayList<RoomDef> rooms = new ArrayList();
        final ArrayList<RoomDef> emptyoutside = new ArrayList();

        private MergedRooms() {
        }
    }
}

