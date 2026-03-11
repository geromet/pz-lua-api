/*
 * Decompiled with CFR 0.152.
 */
package zombie;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import zombie.UsedFromLua;
import zombie.ZomboidFileSystem;
import zombie.core.Core;
import zombie.core.Translator;
import zombie.core.logger.ExceptionLogger;
import zombie.debug.DebugLog;
import zombie.gameStates.ChooseGameInfo;
import zombie.iso.IsoWorld;
import zombie.modding.ActiveMods;
import zombie.util.list.PZArrayUtil;

@UsedFromLua
public final class MapGroups {
    private final ArrayList<MapGroup> groups = new ArrayList();
    private final ArrayList<MapDirectory> realDirectories = new ArrayList();

    private static ArrayList<String> getVanillaMapDirectories(boolean includeChallenges) {
        ArrayList<String> result = new ArrayList<String>();
        File mapsFile = ZomboidFileSystem.instance.getMediaFile("maps");
        String[] internalNames = mapsFile.list();
        if (internalNames != null) {
            for (int i = 0; i < internalNames.length; ++i) {
                String directoryName = internalNames[i];
                if (directoryName.equalsIgnoreCase("challengemaps")) {
                    if (!includeChallenges) continue;
                    try (DirectoryStream<Path> dstrm = Files.newDirectoryStream(Paths.get(mapsFile.getPath(), directoryName), path -> Files.isDirectory(path, new LinkOption[0]) && Files.exists(path.resolve("map.info"), new LinkOption[0]));){
                        for (Path path2 : dstrm) {
                            result.add(directoryName + "/" + path2.getFileName().toString());
                        }
                    }
                    catch (Exception exception) {}
                    continue;
                }
                result.add(directoryName);
            }
        }
        return result;
    }

    public static String addMissingVanillaDirectories(String mapName) {
        String[] ss;
        ArrayList<String> vanillaDirs = MapGroups.getVanillaMapDirectories(false);
        boolean usesVanilla = false;
        for (String name : ss = mapName.split(";")) {
            if ((name = name.trim()).isEmpty() || !vanillaDirs.contains(name)) continue;
            usesVanilla = true;
            break;
        }
        if (!usesVanilla) {
            return mapName;
        }
        ArrayList<String> names = new ArrayList<String>();
        for (String name : ss) {
            if ((name = name.trim()).isEmpty()) continue;
            names.add(name);
        }
        for (String vanillaDir : vanillaDirs) {
            if (names.contains(vanillaDir)) continue;
            names.add(vanillaDir);
        }
        Object result = "";
        for (String name : names) {
            if (!((String)result).isEmpty()) {
                result = (String)result + ";";
            }
            result = (String)result + name;
        }
        return result;
    }

    public void createGroups() {
        this.createGroups(ActiveMods.getById("currentGame"), true);
    }

    public void createGroups(ActiveMods activeMods, boolean includeVanilla) {
        this.createGroups(activeMods, includeVanilla, false);
    }

    public void createGroups(ActiveMods activeMods, boolean includeVanilla, boolean includeChallenges) {
        Object modDetails;
        this.groups.clear();
        this.realDirectories.clear();
        for (String modID : activeMods.getMods()) {
            Object directoryName;
            int i;
            Object internalNames;
            File fo;
            modDetails = ChooseGameInfo.getAvailableModDetails(modID);
            if (modDetails == null || !(fo = new File(((ChooseGameInfo.Mod)modDetails).getCommonDir() + "/media/maps/")).exists() || (internalNames = fo.list()) == null) continue;
            for (i = 0; i < ((String[])internalNames).length; ++i) {
                directoryName = internalNames[i];
                if (((String)directoryName).equalsIgnoreCase("challengemaps")) {
                    if (!includeChallenges) continue;
                    continue;
                }
                this.handleMapDirectory((String)directoryName, ((ChooseGameInfo.Mod)modDetails).getCommonDir() + "/media/maps/" + (String)directoryName);
            }
            fo = new File(((ChooseGameInfo.Mod)modDetails).getVersionDir() + "/media/maps/");
            if (!fo.exists() || (internalNames = fo.list()) == null) continue;
            for (i = 0; i < ((Object)internalNames).length; ++i) {
                directoryName = internalNames[i];
                if (((String)directoryName).equalsIgnoreCase("challengemaps")) {
                    if (!includeChallenges) continue;
                    continue;
                }
                this.handleMapDirectory((String)directoryName, ((ChooseGameInfo.Mod)modDetails).getVersionDir() + "/media/maps/" + (String)directoryName);
            }
        }
        if (includeVanilla) {
            Iterator<MapGroup> vanillaMaps = MapGroups.getVanillaMapDirectories(includeChallenges);
            String mapsPath = ZomboidFileSystem.instance.getMediaPath("maps");
            modDetails = ((ArrayList)((Object)vanillaMaps)).iterator();
            while (modDetails.hasNext()) {
                String directoryName = (String)modDetails.next();
                this.handleMapDirectory(directoryName, mapsPath + File.separator + directoryName);
            }
        }
        for (MapDirectory mapDir : this.realDirectories) {
            ArrayList<MapDirectory> directories = new ArrayList<MapDirectory>();
            this.getDirsRecursively(mapDir, directories);
            MapGroup group = this.findGroupWithAnyOfTheseDirectories(directories);
            if (group == null) {
                group = new MapGroup(this);
                this.groups.add(group);
            }
            for (MapDirectory mapDir2 : directories) {
                if (group.hasDirectory(mapDir2.name)) continue;
                group.addDirectory(mapDir2);
            }
        }
        for (MapGroup group : this.groups) {
            group.setPriority();
        }
        for (MapGroup group : this.groups) {
            group.setOrder(activeMods);
        }
        if (Core.debug) {
            int i = 1;
            for (MapGroup group : this.groups) {
                DebugLog.log("MapGroup " + i + "/" + this.groups.size());
                for (MapDirectory mapDir : group.directories) {
                    DebugLog.log("  " + mapDir.name);
                }
                ++i;
            }
            DebugLog.log("-----");
        }
    }

    private void getDirsRecursively(MapDirectory mapDir, ArrayList<MapDirectory> result) {
        if (result.contains(mapDir)) {
            return;
        }
        result.add(mapDir);
        block0: for (String lotDir : mapDir.lotDirs) {
            for (MapDirectory mapDir2 : this.realDirectories) {
                if (!mapDir2.name.equals(lotDir)) continue;
                this.getDirsRecursively(mapDir2, result);
                continue block0;
            }
        }
    }

    public int getNumberOfGroups() {
        return this.groups.size();
    }

    public ArrayList<String> getMapDirectoriesInGroup(int groupIndex) {
        if (groupIndex < 0 || groupIndex >= this.groups.size()) {
            throw new RuntimeException("invalid MapGroups index " + groupIndex);
        }
        ArrayList<String> result = new ArrayList<String>();
        for (MapDirectory mapDir : this.groups.get((int)groupIndex).directories) {
            result.add(mapDir.name);
        }
        return result;
    }

    public void setWorld(int groupIndex) {
        ArrayList<String> mapDirs = this.getMapDirectoriesInGroup(groupIndex);
        Object mapName = "";
        for (int i = 0; i < mapDirs.size(); ++i) {
            mapName = (String)mapName + mapDirs.get(i);
            if (i >= mapDirs.size() - 1) continue;
            mapName = (String)mapName + ";";
        }
        IsoWorld.instance.setMap((String)mapName);
    }

    private void handleMapDirectory(String directoryName, String path) {
        ArrayList<String> lotDirs = this.getLotDirectories(path);
        if (lotDirs == null) {
            return;
        }
        MapDirectory mapDir = new MapDirectory(this, directoryName, path, lotDirs);
        this.realDirectories.add(mapDir);
    }

    private ArrayList<String> getLotDirectories(String path) {
        File file = new File(path + "/map.info");
        if (!file.exists()) {
            return null;
        }
        ArrayList<String> lotDirs = new ArrayList<String>();
        try (FileReader reader = new FileReader(file.getAbsolutePath());
             BufferedReader br = new BufferedReader(reader);){
            String inputLine;
            while ((inputLine = br.readLine()) != null) {
                if (!(inputLine = inputLine.trim()).startsWith("lots=")) continue;
                lotDirs.add(inputLine.replace("lots=", "").trim());
            }
        }
        catch (Exception ex) {
            ExceptionLogger.logException(ex);
            return null;
        }
        return lotDirs;
    }

    private MapGroup findGroupWithAnyOfTheseDirectories(ArrayList<MapDirectory> directories) {
        for (MapGroup group : this.groups) {
            if (!group.hasAnyOfTheseDirectories(directories)) continue;
            return group;
        }
        return null;
    }

    public ArrayList<String> getAllMapsInOrder() {
        ArrayList<String> result = new ArrayList<String>();
        for (MapGroup group : this.groups) {
            for (MapDirectory mapDir : group.directories) {
                result.add(mapDir.name);
            }
        }
        return result;
    }

    public boolean checkMapConflicts() {
        boolean hasConflicts = false;
        for (MapGroup group : this.groups) {
            hasConflicts |= group.checkMapConflicts();
        }
        return hasConflicts;
    }

    public ArrayList<String> getMapConflicts(String mapName) {
        for (MapGroup group : this.groups) {
            MapDirectory mapDir = group.getDirectoryByName(mapName);
            if (mapDir == null) continue;
            return new ArrayList<String>(mapDir.conflicts);
        }
        return null;
    }

    private class MapDirectory {
        String name;
        String path;
        ArrayList<String> lotDirs;
        ArrayList<String> conflicts;

        public MapDirectory(MapGroups mapGroups, String name, String path) {
            Objects.requireNonNull(mapGroups);
            this.lotDirs = new ArrayList();
            this.conflicts = new ArrayList();
            this.name = name;
            this.path = path;
        }

        public MapDirectory(MapGroups mapGroups, String name, String path, ArrayList<String> lotDirs) {
            Objects.requireNonNull(mapGroups);
            this.lotDirs = new ArrayList();
            this.conflicts = new ArrayList();
            this.name = name;
            this.path = path;
            PZArrayUtil.addAll(this.lotDirs, lotDirs);
        }

        public void getLotHeaders(ArrayList<String> result) {
            File fo = new File(this.path);
            if (!fo.isDirectory()) {
                return;
            }
            String[] internalNames = fo.list();
            if (internalNames == null) {
                return;
            }
            for (int i = 0; i < internalNames.length; ++i) {
                if (!internalNames[i].endsWith(".lotheader")) continue;
                result.add(internalNames[i]);
            }
        }
    }

    private class MapGroup {
        private final LinkedList<MapDirectory> directories;
        final /* synthetic */ MapGroups this$0;

        private MapGroup(MapGroups mapGroups) {
            MapGroups mapGroups2 = mapGroups;
            Objects.requireNonNull(mapGroups2);
            this.this$0 = mapGroups2;
            this.directories = new LinkedList();
        }

        void addDirectory(String directoryName, String path) {
            assert (!this.hasDirectory(directoryName));
            MapDirectory mapDir = new MapDirectory(this.this$0, directoryName, path);
            this.directories.add(mapDir);
        }

        void addDirectory(String directoryName, String path, ArrayList<String> lotDirs) {
            assert (!this.hasDirectory(directoryName));
            MapDirectory mapDir = new MapDirectory(this.this$0, directoryName, path, lotDirs);
            this.directories.add(mapDir);
        }

        void addDirectory(MapDirectory mapDir) {
            assert (!this.hasDirectory(mapDir.name));
            this.directories.add(mapDir);
        }

        MapDirectory getDirectoryByName(String name) {
            for (MapDirectory mapDir : this.directories) {
                if (!mapDir.name.equals(name)) continue;
                return mapDir;
            }
            return null;
        }

        boolean hasDirectory(String name) {
            return this.getDirectoryByName(name) != null;
        }

        boolean hasAnyOfTheseDirectories(ArrayList<MapDirectory> anyOf) {
            for (MapDirectory mapDir : anyOf) {
                if (!this.directories.contains(mapDir)) continue;
                return true;
            }
            return false;
        }

        boolean isReferencedByOtherMaps(MapDirectory mapDir) {
            for (MapDirectory mapDir2 : this.directories) {
                if (mapDir == mapDir2 || !mapDir2.lotDirs.contains(mapDir.name)) continue;
                return true;
            }
            return false;
        }

        void getDirsRecursively(MapDirectory mapDir, ArrayList<String> result) {
            if (result.contains(mapDir.name)) {
                return;
            }
            result.add(mapDir.name);
            for (String lotDir : mapDir.lotDirs) {
                MapDirectory mapDir2 = this.getDirectoryByName(lotDir);
                if (mapDir2 == null) continue;
                this.getDirsRecursively(mapDir2, result);
            }
        }

        void setPriority() {
            ArrayList<MapDirectory> directories1 = new ArrayList<MapDirectory>(this.directories);
            for (MapDirectory mapDir : directories1) {
                if (this.isReferencedByOtherMaps(mapDir)) continue;
                ArrayList<String> priorityList = new ArrayList<String>();
                this.getDirsRecursively(mapDir, priorityList);
                this.setPriority(priorityList);
            }
        }

        void setPriority(List<String> priorityList) {
            ArrayList<MapDirectory> sorted2 = new ArrayList<MapDirectory>(priorityList.size());
            for (String name : priorityList) {
                if (!this.hasDirectory(name)) continue;
                sorted2.add(this.getDirectoryByName(name));
            }
            for (int i = 0; i < this.directories.size(); ++i) {
                MapDirectory mapDir1 = this.directories.get(i);
                if (!priorityList.contains(mapDir1.name)) continue;
                this.directories.set(i, (MapDirectory)sorted2.remove(0));
            }
        }

        void setOrder(ActiveMods activeMods) {
            if (!activeMods.getMapOrder().isEmpty()) {
                this.setPriority(activeMods.getMapOrder());
            }
        }

        boolean checkMapConflicts() {
            HashMap lotHeaderToDir = new HashMap();
            ArrayList<String> lotHeaders = new ArrayList<String>();
            for (MapDirectory mapDir : this.directories) {
                mapDir.conflicts.clear();
                lotHeaders.clear();
                mapDir.getLotHeaders(lotHeaders);
                for (String lotHeader : lotHeaders) {
                    if (!lotHeaderToDir.containsKey(lotHeader)) {
                        lotHeaderToDir.put(lotHeader, new ArrayList());
                    }
                    ((ArrayList)lotHeaderToDir.get(lotHeader)).add(mapDir.name);
                }
            }
            boolean hasConflicts = false;
            for (String lotHeader : lotHeaderToDir.keySet()) {
                ArrayList mapNames = (ArrayList)lotHeaderToDir.get(lotHeader);
                if (mapNames.size() <= 1) continue;
                for (int i = 0; i < mapNames.size(); ++i) {
                    MapDirectory mapDir = this.getDirectoryByName((String)mapNames.get(i));
                    for (int j = 0; j < mapNames.size(); ++j) {
                        if (i == j) continue;
                        String conflict = Translator.getText("UI_MapConflict", mapDir.name, mapNames.get(j), lotHeader);
                        mapDir.conflicts.add(conflict);
                        hasConflicts = true;
                    }
                }
            }
            return hasConflicts;
        }
    }
}

