/*
 * Decompiled with CFR 0.152.
 */
package zombie.tileDepth;

import java.io.File;
import java.util.ArrayList;
import zombie.UsedFromLua;
import zombie.ZomboidFileSystem;
import zombie.gameStates.ChooseGameInfo;
import zombie.iso.sprite.IsoSprite;
import zombie.iso.sprite.IsoSpriteManager;
import zombie.tileDepth.TileDepthTexture;
import zombie.tileDepth.TileDepthTextureAssignments;
import zombie.tileDepth.TileDepthTextureManager;
import zombie.util.StringUtils;

@UsedFromLua
public final class TileDepthTextureAssignmentManager {
    private static TileDepthTextureAssignmentManager instance;
    private final ArrayList<ModData> modData = new ArrayList();

    public static TileDepthTextureAssignmentManager getInstance() {
        if (instance == null) {
            instance = new TileDepthTextureAssignmentManager();
        }
        return instance;
    }

    private TileDepthTextureAssignmentManager() {
    }

    public void init() {
        ArrayList<String> modIDs = ZomboidFileSystem.instance.getModIDs();
        for (String modID : modIDs) {
            File file;
            ChooseGameInfo.Mod mod = ChooseGameInfo.getAvailableModDetails(modID);
            if (mod == null || !(file = new File(mod.mediaFile.common.absoluteFile, "tileDepthTextureAssignments.txt")).exists()) continue;
            this.initModData(mod);
        }
        this.initGameData();
    }

    public void initGameData() {
        ModData data = new ModData("game", ZomboidFileSystem.instance.getMediaRootPath());
        data.assignments.load();
        this.modData.add(data);
    }

    public void initModData(ChooseGameInfo.Mod mod) {
        ModData data = new ModData(mod.getId(), mod.mediaFile.common.absoluteFile.getAbsolutePath());
        data.assignments.load();
        this.modData.add(data);
    }

    public void save(String modID) {
        this.getModData((String)modID).assignments.save();
    }

    ModData getModData(String modID) {
        for (int i = 0; i < this.modData.size(); ++i) {
            ModData data = this.modData.get(i);
            if (!StringUtils.equals(modID, data.modId)) continue;
            return data;
        }
        return null;
    }

    public void initSprites() {
        for (IsoSprite sprite : IsoSpriteManager.instance.namedMap.values()) {
            TileDepthTexture depthTexture;
            String otherTile;
            if (sprite.depthTexture != null || sprite.tilesetName == null || (otherTile = this.getAssignedTileName(sprite.name)) == null || (depthTexture = TileDepthTextureManager.getInstance().getTextureFromTileName(otherTile)) == null || depthTexture.isEmpty()) continue;
            sprite.depthTexture = depthTexture;
        }
    }

    public void assignTileName(String modID, String assignTo, String otherTile) {
        this.getModData((String)modID).assignments.assignTileName(assignTo, otherTile);
    }

    public String getAssignedTileName(String modID, String tileName) {
        return this.getModData((String)modID).assignments.getAssignedTileName(tileName);
    }

    public void clearAssignedTileName(String modID, String assignTo) {
        this.getModData((String)modID).assignments.clearAssignedTileName(assignTo);
    }

    public void assignDepthTextureToSprite(String modID, String tileName) {
        this.getModData((String)modID).assignments.assignDepthTextureToSprite(tileName);
    }

    String getAssignedTileName(String tileName) {
        for (int i = 0; i < this.modData.size(); ++i) {
            ModData data = this.modData.get(i);
            String otherTile = data.assignments.getAssignedTileName(tileName);
            if (otherTile == null) continue;
            return otherTile;
        }
        return null;
    }

    static final class ModData {
        final String modId;
        final String mediaAbsPath;
        final TileDepthTextureAssignments assignments;

        ModData(String modID, String mediaAbsPath) {
            this.modId = modID;
            this.mediaAbsPath = mediaAbsPath;
            this.assignments = new TileDepthTextureAssignments(mediaAbsPath);
        }
    }
}

