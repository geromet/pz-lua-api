/*
 * Decompiled with CFR 0.152.
 */
package zombie.core.znet;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import se.krka.kahlua.vm.KahluaTable;
import zombie.UsedFromLua;
import zombie.ZomboidFileSystem;
import zombie.core.Core;
import zombie.core.logger.ExceptionLogger;
import zombie.core.textures.PNGDecoder;
import zombie.core.znet.SteamUtils;
import zombie.core.znet.SteamWorkshop;
import zombie.gameStates.ChooseGameInfo;
import zombie.util.StringUtils;

@UsedFromLua
public class SteamWorkshopItem {
    private final String workshopFolder;
    private String publishedFileId;
    private String title = "";
    private String description = "";
    private String visibility = "public";
    private final ArrayList<String> tags = new ArrayList();
    private String changeNote = "";
    private boolean hasMod;
    private boolean hasMap;
    private final ArrayList<String> modIds = new ArrayList();
    private final ArrayList<String> mapFolders = new ArrayList();
    private static final int VERSION1 = 1;
    private static final int LATEST_VERSION = 1;

    public SteamWorkshopItem(String workshopFolder) {
        this.workshopFolder = workshopFolder;
    }

    public String getContentFolder() {
        return this.workshopFolder + File.separator + "Contents";
    }

    public String getFolderName() {
        return new File(this.workshopFolder).getName();
    }

    public void setID(String id) {
        if (id != null && !SteamUtils.isValidSteamID(id)) {
            id = null;
        }
        this.publishedFileId = id;
    }

    public String getID() {
        return this.publishedFileId;
    }

    public void setTitle(String title) {
        if (title == null) {
            title = "";
        }
        this.title = title;
    }

    public String getTitle() {
        return this.title;
    }

    public void setDescription(String description) {
        if (description == null) {
            description = "";
        }
        this.description = description;
    }

    public String getDescription() {
        return this.description;
    }

    public void setVisibility(String visibility) {
        this.visibility = visibility;
    }

    public String getVisibility() {
        return this.visibility;
    }

    public void setVisibilityInteger(int v) {
        switch (v) {
            case 0: {
                this.visibility = "public";
                break;
            }
            case 1: {
                this.visibility = "friendsOnly";
                break;
            }
            case 2: {
                this.visibility = "private";
                break;
            }
            case 3: {
                this.visibility = "unlisted";
                break;
            }
            default: {
                this.visibility = "public";
            }
        }
    }

    public int getVisibilityInteger() {
        if ("public".equals(this.visibility)) {
            return 0;
        }
        if ("friendsOnly".equals(this.visibility)) {
            return 1;
        }
        if ("private".equals(this.visibility)) {
            return 2;
        }
        if ("unlisted".equals(this.visibility)) {
            return 3;
        }
        return 0;
    }

    public void setTags(ArrayList<String> tags) {
        this.tags.clear();
        this.tags.addAll(tags);
    }

    public static ArrayList<String> getAllowedTags() {
        ArrayList<String> tags = new ArrayList<String>();
        File file = ZomboidFileSystem.instance.getMediaFile("WorkshopTags.txt");
        try (FileReader fileReader = new FileReader(file);
             BufferedReader bufferedReader = new BufferedReader(fileReader);){
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                if ((line = line.trim()).isEmpty()) continue;
                tags.add(line);
            }
        }
        catch (Exception ex) {
            ExceptionLogger.logException(ex);
        }
        return tags;
    }

    public ArrayList<String> getTags() {
        return this.tags;
    }

    public String getSubmitDescription() {
        int i;
        Object s = this.getDescription();
        if (!((String)s).isEmpty()) {
            s = (String)s + "\n\n";
        }
        s = (String)s + "Workshop ID: " + this.getID();
        for (i = 0; i < this.modIds.size(); ++i) {
            s = (String)s + "\nMod ID: " + this.modIds.get(i);
        }
        for (i = 0; i < this.mapFolders.size(); ++i) {
            s = (String)s + "\nMap Folder: " + this.mapFolders.get(i);
        }
        return s;
    }

    public String[] getSubmitTags() {
        return this.tags.toArray(new String[0]);
    }

    public String getPreviewImage() {
        return this.workshopFolder + File.separator + "preview.png";
    }

    public void setChangeNote(String changeNote) {
        if (changeNote == null) {
            changeNote = "";
        }
        this.changeNote = changeNote;
    }

    public String getChangeNote() {
        return this.changeNote;
    }

    public boolean create() {
        return SteamWorkshop.instance.CreateWorkshopItem(this);
    }

    public boolean submitUpdate() {
        return SteamWorkshop.instance.SubmitWorkshopItem(this);
    }

    public boolean getUpdateProgress(KahluaTable table) {
        if (table == null) {
            throw new NullPointerException("table is null");
        }
        long[] progress = new long[2];
        if (SteamWorkshop.instance.GetItemUpdateProgress(progress)) {
            System.out.println(progress[0] + "/" + progress[1]);
            table.rawset("processed", (Object)progress[0]);
            table.rawset("total", (Object)Math.max(progress[1], 1L));
            return true;
        }
        return false;
    }

    public int getUpdateProgressTotal() {
        return 1;
    }

    /*
     * Enabled aggressive block sorting
     * Enabled unnecessary exception pruning
     * Enabled aggressive exception aggregation
     */
    private String validateFileTypes(Path dir) {
        try (DirectoryStream<Path> dstrm = Files.newDirectoryStream(dir);){
            block13: {
                Iterator<Path> iterator2 = dstrm.iterator();
                while (iterator2.hasNext()) {
                    Path path = iterator2.next();
                    if (Files.isDirectory(path, new LinkOption[0])) {
                        String err = this.validateFileTypes(path);
                        if (err == null) continue;
                        String string = err;
                        return string;
                    }
                    String fileName = path.getFileName().toString();
                    if (!(!StringUtils.endsWithIgnoreCase(fileName, "pyramid.zip") && (fileName.endsWith(".exe") || fileName.endsWith(".dll") || fileName.endsWith(".bat") || fileName.endsWith(".app") || fileName.endsWith(".dylib") || fileName.endsWith(".sh") || fileName.endsWith(".so") || fileName.endsWith(".zip")))) {
                        continue;
                    }
                    break block13;
                }
                return null;
            }
            String string = "FileTypeNotAllowed";
            return string;
        }
        catch (Exception ex) {
            ExceptionLogger.logException(ex);
            return "IOError";
        }
    }

    private String validateModDotInfo(Path path) {
        String modID = null;
        try (FileReader fr = new FileReader(path.toFile());
             BufferedReader br = new BufferedReader(fr);){
            String line;
            while ((line = br.readLine()) != null) {
                if (!line.startsWith("id=")) continue;
                modID = line.replace("id=", "").trim();
                break;
            }
        }
        catch (FileNotFoundException e) {
            return "MissingModDotInfo";
        }
        catch (IOException e) {
            ExceptionLogger.logException(e);
            return "IOError";
        }
        if (modID == null || modID.isEmpty()) {
            return "InvalidModDotInfo";
        }
        if (!this.modIds.contains(modID)) {
            this.modIds.add(modID);
        }
        return null;
    }

    private String validateMapDotInfo(Path path) {
        return null;
    }

    /*
     * Enabled aggressive block sorting
     * Enabled unnecessary exception pruning
     * Enabled aggressive exception aggregation
     */
    private String validateMapFolder(Path dir) {
        boolean foundMapDotInfo = false;
        try (DirectoryStream<Path> dstrm = Files.newDirectoryStream(dir);){
            for (Path path : dstrm) {
                if (Files.isDirectory(path, new LinkOption[0]) || !"map.info".equals(path.getFileName().toString())) continue;
                String err = this.validateMapDotInfo(path);
                if (err != null) {
                    String string = err;
                    return string;
                }
                foundMapDotInfo = true;
            }
        }
        catch (Exception ex) {
            ExceptionLogger.logException(ex);
            return "IOError";
        }
        if (!foundMapDotInfo) {
            return "MissingMapDotInfo";
        }
        String mapDirName = dir.getFileName().toString();
        if (this.mapFolders.contains(mapDirName)) return null;
        this.mapFolders.add(mapDirName);
        return null;
    }

    /*
     * Enabled aggressive block sorting
     * Enabled unnecessary exception pruning
     * Enabled aggressive exception aggregation
     */
    private String validateMapsFolder(Path dir) {
        boolean atLeastOneMap = false;
        try (DirectoryStream<Path> dstrm = Files.newDirectoryStream(dir);){
            for (Path path : dstrm) {
                if (!Files.isDirectory(path, new LinkOption[0])) continue;
                String err = this.validateMapFolder(path);
                if (err != null) {
                    String string = err;
                    return string;
                }
                atLeastOneMap = true;
            }
        }
        catch (Exception ex) {
            ExceptionLogger.logException(ex);
            return "IOError";
        }
        if (!atLeastOneMap) {
            return null;
        }
        this.hasMap = true;
        return null;
    }

    /*
     * Enabled aggressive block sorting
     * Enabled unnecessary exception pruning
     * Enabled aggressive exception aggregation
     */
    private String validateMediaFolder(Path dir) {
        try (DirectoryStream<Path> dstrm = Files.newDirectoryStream(dir);){
            String err;
            Path path;
            Iterator<Path> iterator2 = dstrm.iterator();
            do {
                if (!iterator2.hasNext()) return null;
            } while (!Files.isDirectory(path = iterator2.next(), new LinkOption[0]) || !"maps".equals(path.getFileName().toString()) || (err = this.validateMapsFolder(path)) == null);
            String string = err;
            return string;
        }
        catch (Exception ex) {
            ExceptionLogger.logException(ex);
            return "IOError";
        }
    }

    /*
     * Enabled aggressive block sorting
     * Enabled unnecessary exception pruning
     * Enabled aggressive exception aggregation
     */
    private String validateModFolder(Path dir) {
        boolean foundModDotInfo = false;
        try (DirectoryStream<Path> dstrm = Files.newDirectoryStream(dir);){
            int minRequiredVersion = ChooseGameInfo.getMinRequiredVersion().getInt();
            int gameVersion = Core.getInstance().getGameVersion().getInt();
            for (Path path : dstrm) {
                String string;
                String err;
                if (!Files.isDirectory(path, new LinkOption[0])) continue;
                if ("common".equals(path.getFileName().toString())) {
                    String err2;
                    Path path2 = path.resolve("media");
                    if (Files.exists(path2, new LinkOption[0]) && (err2 = this.validateMediaFolder(path2)) != null) {
                        String string2 = err2;
                        return string2;
                    }
                    Path path3 = path.resolve("mod.info");
                    if (!Files.exists(path3, new LinkOption[0])) continue;
                    err = this.validateModDotInfo(path3);
                    if (err != null) {
                        string = err;
                        return string;
                    }
                    foundModDotInfo = true;
                    continue;
                }
                int ver = ZomboidFileSystem.instance.getGameVersionIntFromName(path.getFileName().toString());
                if (ver < minRequiredVersion || ver > gameVersion) continue;
                Path path2 = path.resolve("media");
                if (Files.exists(path2, new LinkOption[0]) && (err = this.validateMediaFolder(path2)) != null) {
                    string = err;
                    return string;
                }
                Path path3 = path.resolve("mod.info");
                if (!Files.exists(path3, new LinkOption[0])) continue;
                String err3 = this.validateModDotInfo(path3);
                if (err3 != null) {
                    String string3 = err3;
                    return string3;
                }
                foundModDotInfo = true;
            }
        }
        catch (Exception ex) {
            ExceptionLogger.logException(ex);
            return "IOError";
        }
        if (foundModDotInfo) return null;
        return "MissingModDotInfo";
    }

    /*
     * Enabled aggressive block sorting
     * Enabled unnecessary exception pruning
     * Enabled aggressive exception aggregation
     */
    private String validateModsFolder(Path dir) {
        boolean atLeastOneMod = false;
        try (DirectoryStream<Path> dstrm = Files.newDirectoryStream(dir);){
            for (Path path : dstrm) {
                if (Files.isDirectory(path, new LinkOption[0])) {
                    String err = this.validateModFolder(path);
                    if (err != null) {
                        String string = err;
                        return string;
                    }
                    atLeastOneMod = true;
                    continue;
                }
                String string = "FileNotAllowedInMods";
                return string;
            }
        }
        catch (Exception ex) {
            ExceptionLogger.logException(ex);
            return "IOError";
        }
        if (!atLeastOneMod) {
            return "EmptyModsFolder";
        }
        this.hasMod = true;
        return null;
    }

    private String validateBuildingsFolder(Path dir) {
        return null;
    }

    private String validateCreativeFolder(Path dir) {
        return null;
    }

    /*
     * Enabled aggressive block sorting
     * Enabled unnecessary exception pruning
     * Enabled aggressive exception aggregation
     */
    public String validatePreviewImage(Path path) throws IOException {
        if (!Files.exists(path, new LinkOption[0])) return "PreviewNotFound";
        if (!Files.isReadable(path)) return "PreviewNotFound";
        if (Files.isDirectory(path, new LinkOption[0])) {
            return "PreviewNotFound";
        }
        if (Files.size(path) > 1024000L) {
            return "PreviewFileSize";
        }
        try (FileInputStream fis = new FileInputStream(path.toFile());
             BufferedInputStream bis = new BufferedInputStream(fis);){
            PNGDecoder png = new PNGDecoder(bis, false);
            if (png.getWidth() == png.getHeight()) {
                if (png.getWidth() == 256) return null;
                if (png.getWidth() == 512) return null;
            }
            String string = "PreviewDimensions";
            return string;
        }
        catch (IOException ex) {
            ExceptionLogger.logException(ex);
            return "PreviewFormat";
        }
    }

    /*
     * Enabled aggressive block sorting
     * Enabled unnecessary exception pruning
     * Enabled aggressive exception aggregation
     */
    public String validateContents() {
        this.hasMod = false;
        this.hasMap = false;
        this.modIds.clear();
        this.mapFolders.clear();
        try {
            Path dir = FileSystems.getDefault().getPath(this.getContentFolder(), new String[0]);
            if (!Files.isDirectory(dir, new LinkOption[0])) {
                return "MissingContents";
            }
            Path path0 = FileSystems.getDefault().getPath(this.getPreviewImage(), new String[0]);
            String err = this.validatePreviewImage(path0);
            if (err != null) {
                return err;
            }
            boolean atLeastOneFolder = false;
            try (DirectoryStream<Path> dstrm = Files.newDirectoryStream(dir);){
                for (Path path : dstrm) {
                    if (Files.isDirectory(path, new LinkOption[0])) {
                        if ("buildings".equals(path.getFileName().toString())) {
                            err = this.validateBuildingsFolder(path);
                            if (err != null) {
                                String string = err;
                                return string;
                            }
                        } else if ("creative".equals(path.getFileName().toString())) {
                            err = this.validateCreativeFolder(path);
                            if (err != null) {
                                String string = err;
                                return string;
                            }
                        } else {
                            if (!"mods".equals(path.getFileName().toString())) {
                                String string = "FolderNotAllowedInContents";
                                return string;
                            }
                            err = this.validateModsFolder(path);
                            if (err != null) {
                                String string = err;
                                return string;
                            }
                        }
                        atLeastOneFolder = true;
                        continue;
                    }
                    String string = "FileNotAllowedInContents";
                    return string;
                }
            }
            catch (Exception ex) {
                ExceptionLogger.logException(ex);
                return "IOError";
            }
            if (atLeastOneFolder) return this.validateFileTypes(dir);
            return "EmptyContentsFolder";
        }
        catch (IOException ex) {
            ExceptionLogger.logException(ex);
            return "IOError";
        }
    }

    public String getExtendedErrorInfo(String error) {
        if ("FolderNotAllowedInContents".equals(error)) {
            return "buildings/ creative/ mods/";
        }
        if ("FileTypeNotAllowed".equals(error)) {
            return "*.exe *.dll *.bat *.app *.dylib *.sh *.so *.zip";
        }
        return "";
    }

    /*
     * Enabled aggressive exception aggregation
     */
    public boolean readWorkshopTxt() {
        String fileName = this.workshopFolder + File.separator + "workshop.txt";
        if (!new File(fileName).exists()) {
            return true;
        }
        try (FileReader fr = new FileReader(fileName);){
            boolean bl;
            try (BufferedReader br = new BufferedReader(fr);){
                String line;
                while ((line = br.readLine()) != null) {
                    if ((line = line.trim()).isEmpty() || line.startsWith("#") || line.startsWith("//")) continue;
                    if (line.startsWith("id=")) {
                        String id = line.replace("id=", "");
                        this.setID(id);
                        continue;
                    }
                    if (line.startsWith("description=")) {
                        if (!this.description.isEmpty()) {
                            this.description = this.description + "\n";
                        }
                        this.description = this.description + line.replace("description=", "");
                        continue;
                    }
                    if (line.startsWith("tags=")) {
                        this.tags.addAll(Arrays.asList(line.replace("tags=", "").split(";")));
                        continue;
                    }
                    if (line.startsWith("title=")) {
                        this.title = line.replace("title=", "");
                        continue;
                    }
                    if (line.startsWith("version=") || !line.startsWith("visibility=")) continue;
                    this.visibility = line.replace("visibility=", "");
                }
                bl = true;
            }
            return bl;
        }
        catch (IOException ex) {
            ExceptionLogger.logException(ex);
            return false;
        }
    }

    public boolean writeWorkshopTxt() {
        String fileName = this.workshopFolder + File.separator + "workshop.txt";
        File file = new File(fileName);
        try {
            FileWriter fw = new FileWriter(file);
            BufferedWriter bw = new BufferedWriter(fw);
            bw.write("version=1");
            bw.newLine();
            bw.write("id=" + (this.publishedFileId == null ? "" : this.publishedFileId));
            bw.newLine();
            bw.write("title=" + this.title);
            bw.newLine();
            for (String s : this.description.split("\n")) {
                bw.write("description=" + s);
                bw.newLine();
            }
            Object tagStr = "";
            for (int i = 0; i < this.tags.size(); ++i) {
                tagStr = (String)tagStr + this.tags.get(i);
                if (i >= this.tags.size() - 1) continue;
                tagStr = (String)tagStr + ";";
            }
            bw.write("tags=" + (String)tagStr);
            bw.newLine();
            bw.write("visibility=" + this.visibility);
            bw.newLine();
            bw.close();
        }
        catch (IOException ex) {
            ExceptionLogger.logException(ex);
            return false;
        }
        return true;
    }

    public static enum ItemState {
        None(0),
        Subscribed(1),
        LegacyItem(2),
        Installed(4),
        NeedsUpdate(8),
        Downloading(16),
        DownloadPending(32);

        private final int value;

        private ItemState(int value) {
            this.value = value;
        }

        public int getValue() {
            return this.value;
        }

        public boolean and(ItemState other) {
            return (this.value & other.value) != 0;
        }

        public boolean and(long bits) {
            return ((long)this.value & bits) != 0L;
        }

        public boolean not(long bits) {
            return ((long)this.value & bits) == 0L;
        }

        public static String toString(long bits) {
            if (bits == (long)None.getValue()) {
                return "None";
            }
            StringBuilder s = new StringBuilder();
            for (ItemState e : ItemState.values()) {
                if (e == None || !e.and(bits)) continue;
                if (!s.isEmpty()) {
                    s.append("|");
                }
                s.append(e.name());
            }
            return s.toString();
        }
    }
}

