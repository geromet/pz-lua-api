/*
 * Decompiled with CFR 0.152.
 */
package zombie.gameStates;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import zombie.Lua.LuaManager;
import zombie.UsedFromLua;
import zombie.ZomboidFileSystem;
import zombie.core.Core;
import zombie.core.GameVersion;
import zombie.core.Translator;
import zombie.core.logger.ExceptionLogger;
import zombie.core.textures.Texture;
import zombie.core.znet.SteamUtils;
import zombie.debug.DebugLog;
import zombie.util.StringUtils;

public final class ChooseGameInfo {
    private static final HashMap<String, Map> Maps = new HashMap();
    private static final HashMap<String, Mod> Mods = new HashMap();
    private static final HashSet<String> MissingMods = new HashSet();
    private static final ArrayList<String> tempStrings = new ArrayList();
    private static final GameVersion minRequiredVersion = new GameVersion(42, 0, "");

    private ChooseGameInfo() {
    }

    public static void Reset() {
        Maps.clear();
        Mods.clear();
        MissingMods.clear();
    }

    public static GameVersion getMinRequiredVersion() {
        return minRequiredVersion;
    }

    public static Map getMapDetails(String dir) {
        if (Maps.containsKey(dir)) {
            return Maps.get(dir);
        }
        File file = new File(ZomboidFileSystem.instance.getString("media/maps/" + dir + "/map.info"));
        if (!file.exists()) {
            return null;
        }
        Map map = new Map();
        map.dir = new File(file.getParent()).getAbsolutePath();
        map.title = dir;
        map.lotsDir = new ArrayList();
        try {
            FileReader reader = new FileReader(file.getAbsolutePath());
            BufferedReader br = new BufferedReader(reader);
            try {
                String inputLine;
                while ((inputLine = br.readLine()) != null) {
                    if ((inputLine = inputLine.trim()).startsWith("title=")) {
                        map.title = inputLine.replace("title=", "");
                        continue;
                    }
                    if (inputLine.startsWith("lots=")) {
                        map.lotsDir.add(inputLine.replace("lots=", "").trim());
                        continue;
                    }
                    if (inputLine.startsWith("description=")) {
                        if (map.desc == null) {
                            map.desc = "";
                        }
                        map.desc = map.desc + inputLine.replace("description=", "");
                        continue;
                    }
                    if (inputLine.startsWith("fixed2x=")) {
                        map.fixed2x = Boolean.parseBoolean(inputLine.replace("fixed2x=", "").trim());
                        continue;
                    }
                    if (inputLine.startsWith("zoomX=")) {
                        map.zoomX = Float.parseFloat(inputLine.replace("zoomX=", "").trim());
                        continue;
                    }
                    if (inputLine.startsWith("zoomY=")) {
                        map.zoomY = Float.parseFloat(inputLine.replace("zoomY=", "").trim());
                        continue;
                    }
                    if (inputLine.startsWith("zoomS=")) {
                        map.zoomS = Float.parseFloat(inputLine.replace("zoomS=", "").trim());
                        continue;
                    }
                    if (!inputLine.startsWith("demoVideo=")) continue;
                    map.demoVideo = inputLine.replace("demoVideo=", "");
                }
            }
            catch (IOException ex) {
                Logger.getLogger(ChooseGameInfo.class.getName()).log(Level.SEVERE, null, ex);
            }
            br.close();
            map.thumb = Texture.getSharedTexture(map.dir + "/thumb.png");
            String pyramidPath = ZomboidFileSystem.instance.getString(map.dir + "/spawnSelectImagePyramid.zip");
            if (new File(pyramidPath).exists()) {
                map.spawnSelectImagePyramid = pyramidPath;
            } else {
                map.worldmap = Texture.getSharedTexture(map.dir + "/worldmap.png");
            }
            Translator.readMapTranslation(map, dir);
        }
        catch (Exception ex) {
            ExceptionLogger.logException(ex);
            return null;
        }
        Maps.put(dir, map);
        return map;
    }

    public static Mod getModDetails(String modId) {
        Mod mod;
        if (MissingMods.contains(modId)) {
            return null;
        }
        if (Mods.containsKey(modId)) {
            return Mods.get(modId);
        }
        String modDir = ZomboidFileSystem.instance.getModDir(modId);
        if (modDir == null) {
            ArrayList<String> modsFolders = tempStrings;
            ZomboidFileSystem.instance.getAllModFolders(modsFolders);
            for (int i = 0; i < modsFolders.size(); ++i) {
                Mod mod2 = ChooseGameInfo.readModInfo(modsFolders.get(i));
                if (mod2 == null) continue;
                ZomboidFileSystem.instance.setModIdToDir(mod2.getId(), modsFolders.get(i));
                if (!mod2.getId().equals(modId)) continue;
                return mod2;
            }
        }
        if ((mod = ChooseGameInfo.readModInfo(modDir)) == null) {
            MissingMods.add(modId);
        }
        return mod;
    }

    public static Mod getAvailableModDetails(String modId) {
        Mod mod = ChooseGameInfo.getModDetails(modId);
        if (mod != null && mod.isAvailable()) {
            return mod;
        }
        return null;
    }

    public static Mod readModInfo(String modDir) {
        Mod mod = ChooseGameInfo.readModInfoAux(modDir);
        if (mod != null) {
            Mod exists = Mods.get(mod.getId());
            if (exists == null) {
                Mods.put(mod.getId(), mod);
            } else if (exists != mod) {
                ZomboidFileSystem.instance.getAllModFolders(tempStrings);
                int index1 = tempStrings.indexOf(mod.getDir());
                int index2 = tempStrings.indexOf(exists.getDir());
                if (index1 < index2) {
                    Mods.put(mod.getId(), mod);
                }
            }
        }
        return mod;
    }

    /*
     * Exception decompiling
     */
    private static Mod readModInfoAux(String modDir) {
        /*
         * This method has failed to decompile.  When submitting a bug report, please provide this stack trace, and (if you hold appropriate legal rights) the relevant class file.
         * 
         * org.benf.cfr.reader.util.ConfusedCFRException: Tried to end blocks [2[TRYBLOCK]], but top level block is 43[WHILELOOP]
         *     at org.benf.cfr.reader.bytecode.analysis.opgraph.Op04StructuredStatement.processEndingBlocks(Op04StructuredStatement.java:435)
         *     at org.benf.cfr.reader.bytecode.analysis.opgraph.Op04StructuredStatement.buildNestedBlocks(Op04StructuredStatement.java:484)
         *     at org.benf.cfr.reader.bytecode.analysis.opgraph.Op03SimpleStatement.createInitialStructuredBlock(Op03SimpleStatement.java:736)
         *     at org.benf.cfr.reader.bytecode.CodeAnalyser.getAnalysisInner(CodeAnalyser.java:850)
         *     at org.benf.cfr.reader.bytecode.CodeAnalyser.getAnalysisOrWrapFail(CodeAnalyser.java:278)
         *     at org.benf.cfr.reader.bytecode.CodeAnalyser.getAnalysis(CodeAnalyser.java:201)
         *     at org.benf.cfr.reader.entities.attributes.AttributeCode.analyse(AttributeCode.java:94)
         *     at org.benf.cfr.reader.entities.Method.analyse(Method.java:531)
         *     at org.benf.cfr.reader.entities.ClassFile.analyseMid(ClassFile.java:1055)
         *     at org.benf.cfr.reader.entities.ClassFile.analyseTop(ClassFile.java:942)
         *     at org.benf.cfr.reader.Driver.doJarVersionTypes(Driver.java:257)
         *     at org.benf.cfr.reader.Driver.doJar(Driver.java:139)
         *     at org.benf.cfr.reader.CfrDriverImpl.analyse(CfrDriverImpl.java:76)
         *     at org.benf.cfr.reader.Main.main(Main.java:54)
         */
        throw new IllegalStateException("Decompilation failed");
    }

    public static final class Map {
        private String dir;
        private Texture thumb;
        private Texture worldmap;
        private String spawnSelectImagePyramid;
        private String title;
        private ArrayList<String> lotsDir;
        private float zoomX;
        private float zoomY;
        private float zoomS;
        private String demoVideo;
        private String desc;
        private boolean fixed2x;

        public String getDirectory() {
            return this.dir;
        }

        public void setDirectory(String dir) {
            this.dir = dir;
        }

        public Texture getThumbnail() {
            return this.thumb;
        }

        public void setThumbnail(Texture thumb) {
            this.thumb = thumb;
        }

        public Texture getWorldmap() {
            return this.worldmap;
        }

        public void setWorldmap(Texture worldmap) {
            this.worldmap = worldmap;
        }

        public String getSpawnSelectImagePyramid() {
            return this.spawnSelectImagePyramid;
        }

        public String getTitle() {
            return this.title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public ArrayList<String> getLotDirectories() {
            return this.lotsDir;
        }

        public float getZoomX() {
            return this.zoomX;
        }

        public void setZoomX(float newX) {
            this.zoomX = newX;
        }

        public float getZoomY() {
            return this.zoomY;
        }

        public void setZoomY(float newY) {
            this.zoomY = newY;
        }

        public float getZoomS() {
            return this.zoomS;
        }

        public void setZoomS(float newS) {
            this.zoomS = newS;
        }

        public String getDemoVideo() {
            return this.demoVideo;
        }

        public void setDemoVideo(String video) {
            this.demoVideo = video;
        }

        public String getDescription() {
            return this.desc;
        }

        public void setDescription(String desc) {
            this.desc = desc;
        }

        public boolean isFixed2x() {
            return this.fixed2x;
        }

        public void setFixed2x(boolean fixed) {
            this.fixed2x = fixed;
        }
    }

    @UsedFromLua
    public static final class Mod {
        public String dir;
        public String commonDir;
        public String versionDir;
        public final ZomboidFileSystem.PZModFolder baseFile = new ZomboidFileSystem.PZModFolder();
        public final ZomboidFileSystem.PZModFolder mediaFile = new ZomboidFileSystem.PZModFolder();
        public final ZomboidFileSystem.PZModFolder actionGroupsFile = new ZomboidFileSystem.PZModFolder();
        public final ZomboidFileSystem.PZModFolder animSetsFile = new ZomboidFileSystem.PZModFolder();
        public final ZomboidFileSystem.PZModFolder animsXFile = new ZomboidFileSystem.PZModFolder();
        private final ArrayList<String> posters = new ArrayList();
        public Texture tex;
        private ArrayList<String> require;
        private ArrayList<String> incompatible;
        private ArrayList<String> loadAfter;
        private ArrayList<String> loadBefore;
        private String name = "Unnamed Mod";
        private String desc = "";
        private String id = "undefined_id";
        private String url;
        private String author;
        private String modVersion;
        private String icon;
        private String category;
        private String workshopId = "";
        private String source = "Mods";
        private boolean availableDone;
        private boolean available = true;
        private GameVersion versionMin;
        private GameVersion versionMax;
        private final ArrayList<PackFile> packs = new ArrayList();
        private final ArrayList<TileDef> tileDefs = new ArrayList();
        private boolean read;
        private boolean valid;

        public Mod(String dir) {
            this.dir = dir;
            Path dirPath = Paths.get(dir, new String[0]);
            Path commonPath = dirPath.resolve("common");
            Path versionPath = dirPath.resolve(ZomboidFileSystem.instance.getModVersionDirName(dirPath));
            this.commonDir = commonPath.toAbsolutePath().toString();
            this.versionDir = versionPath.toAbsolutePath().toString();
            this.baseFile.setWithCatch(commonPath.toFile(), versionPath.toFile());
            this.mediaFile.setWithCatch(new File(this.baseFile.common.canonicalFile, "media"), new File(this.baseFile.version.canonicalFile, "media"));
            this.actionGroupsFile.setWithCatch(new File(this.mediaFile.common.canonicalFile, "actiongroups"), new File(this.mediaFile.version.canonicalFile, "actiongroups"));
            this.animSetsFile.setWithCatch(new File(this.mediaFile.common.canonicalFile, "AnimSets"), new File(this.mediaFile.version.canonicalFile, "AnimSets"));
            this.animsXFile.setWithCatch(new File(this.mediaFile.common.canonicalFile, "anims_X"), new File(this.mediaFile.version.canonicalFile, "anims_X"));
            Path root = Paths.get(dir, "..", "..").normalize().getFileName();
            if (root != null) {
                String dirName = root.toString();
                if (dirName.equalsIgnoreCase("Contents")) {
                    this.source = "Workshop";
                } else if (SteamUtils.isValidSteamID(dirName)) {
                    this.source = "Steam";
                    this.workshopId = dirName;
                }
            }
        }

        public Texture getTexture() {
            if (this.tex == null) {
                String texName;
                String string = texName = this.posters.isEmpty() ? null : this.posters.get(0);
                if (!StringUtils.isNullOrWhitespace(texName)) {
                    this.tex = Texture.getSharedTexture(texName);
                }
                if (this.tex == null || this.tex.isFailure()) {
                    if (Core.debug && this.tex == null) {
                        DebugLog.Mod.println("failed to load poster " + (texName == null ? this.id : texName));
                    }
                    this.tex = Texture.getWhite();
                }
            }
            return this.tex;
        }

        public void setTexture(Texture tex) {
            this.tex = tex;
        }

        public int getPosterCount() {
            return this.posters.size();
        }

        public String getPoster(int index) {
            if (index >= 0 && index < this.posters.size()) {
                return this.posters.get(index);
            }
            return null;
        }

        public String getName() {
            return this.name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getDir() {
            return this.dir;
        }

        public String getCommonDir() {
            return this.commonDir;
        }

        public String getVersionDir() {
            return this.versionDir;
        }

        public String getDescription() {
            return this.desc;
        }

        public void setDescription(String desc) {
            this.desc = desc;
        }

        public ArrayList<String> getRequire() {
            return this.require;
        }

        public void setRequire(ArrayList<String> require) {
            this.require = require;
        }

        public ArrayList<String> getIncompatible() {
            return this.incompatible;
        }

        public void setIncompatible(ArrayList<String> incompatible) {
            this.incompatible = incompatible;
        }

        public ArrayList<String> getLoadAfter() {
            return this.loadAfter;
        }

        public void setLoadAfter(ArrayList<String> loadAfter) {
            this.loadAfter = loadAfter;
        }

        public ArrayList<String> getLoadBefore() {
            return this.loadBefore;
        }

        public void setLoadBefore(ArrayList<String> loadBefore) {
            this.loadBefore = loadBefore;
        }

        public String getId() {
            return this.id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public boolean isAvailable() {
            if (this.availableDone) {
                return this.available;
            }
            this.availableDone = true;
            if (!this.isAvailableSelf()) {
                this.available = false;
                return false;
            }
            tempStrings.clear();
            tempStrings.add(this.getId());
            if (!this.isAvailableRequired(tempStrings)) {
                this.available = false;
                return false;
            }
            this.available = true;
            return true;
        }

        public boolean isAvailableSelf() {
            GameVersion gameVersion = Core.getInstance().getGameVersion();
            if (this.versionMin != null && this.versionMin.isGreaterThan(gameVersion)) {
                return false;
            }
            return this.versionMax == null || !this.versionMax.isLessThan(gameVersion);
        }

        private boolean isAvailableRequired(ArrayList<String> seen) {
            if (this.require == null || this.require.isEmpty()) {
                return true;
            }
            for (int i = 0; i < this.require.size(); ++i) {
                String modID = this.require.get(i).trim();
                if (seen.contains(modID)) continue;
                seen.add(modID);
                Mod mod = ChooseGameInfo.getModDetails(modID);
                if (mod == null) {
                    return false;
                }
                if (!mod.isAvailableSelf()) {
                    return false;
                }
                if (mod.isAvailableRequired(seen)) continue;
                return false;
            }
            return true;
        }

        @Deprecated
        public void setAvailable(boolean available) {
        }

        public String getUrl() {
            if (this.url == null) {
                return "";
            }
            return this.url;
        }

        public void setUrl(String url) {
            if (LuaManager.isIndieStoneUrl(url)) {
                this.url = url;
            }
        }

        public String getAuthor() {
            if (this.author == null) {
                return "";
            }
            return this.author;
        }

        public void setAuthor(String author) {
            this.author = author;
        }

        public String getModVersion() {
            if (this.modVersion == null) {
                return "";
            }
            return this.modVersion;
        }

        public void setModVersion(String version) {
            this.modVersion = version;
        }

        public String getIcon() {
            if (this.icon == null) {
                return "";
            }
            return this.icon;
        }

        public void setIcon(String name) {
            this.icon = name;
        }

        public String getCategory() {
            if (this.category == null) {
                return "";
            }
            return this.category;
        }

        public void setCategory(String name) {
            this.category = name;
        }

        public GameVersion getVersionMin() {
            return this.versionMin;
        }

        public GameVersion getVersionMax() {
            return this.versionMax;
        }

        public void addPack(String name, int flags) {
            this.packs.add(new PackFile(name, flags));
        }

        public void addTileDef(String name, int fileNumber) {
            this.tileDefs.add(new TileDef(name, fileNumber));
        }

        public ArrayList<PackFile> getPacks() {
            return this.packs;
        }

        public ArrayList<TileDef> getTileDefs() {
            return this.tileDefs;
        }

        public String getSource() {
            return this.source;
        }

        public String getWorkshopID() {
            return this.workshopId;
        }
    }

    public static final class TileDef {
        public String name;
        public int fileNumber;

        public TileDef(String name, int fileNumber) {
            this.name = name;
            this.fileNumber = fileNumber;
        }
    }

    public static final class PackFile {
        public final String name;
        public final int flags;

        public PackFile(String name, int flags) {
            this.name = name;
            this.flags = flags;
        }
    }

    public static final class SpawnOrigin {
        public int x;
        public int y;
        public int w;
        public int h;

        public SpawnOrigin(int x, int y, int w, int h) {
            this.x = x;
            this.y = y;
            this.w = w;
            this.h = h;
        }
    }
}

