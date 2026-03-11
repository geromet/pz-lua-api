/*
 * Decompiled with CFR 0.152.
 */
package zombie.spriteModel;

import java.io.File;
import java.util.ArrayList;
import zombie.UsedFromLua;
import zombie.ZomboidFileSystem;
import zombie.gameStates.ChooseGameInfo;
import zombie.iso.SpriteModel;
import zombie.iso.SpriteModels;
import zombie.iso.SpriteModelsFile;
import zombie.iso.sprite.IsoSprite;
import zombie.iso.sprite.IsoSpriteManager;
import zombie.util.StringUtils;

@UsedFromLua
public final class SpriteModelManager {
    private static SpriteModelManager instance;
    private boolean loadedTileDefinitions;
    private final ArrayList<ModData> modData = new ArrayList();

    public static SpriteModelManager getInstance() {
        if (instance == null) {
            instance = new SpriteModelManager();
        }
        return instance;
    }

    public void init() {
        this.initGameData();
        ArrayList<String> modIDs = ZomboidFileSystem.instance.getModIDs();
        for (String modID : modIDs) {
            File file;
            ChooseGameInfo.Mod mod = ChooseGameInfo.getAvailableModDetails(modID);
            if (mod == null || !(file = new File(mod.mediaFile.common.absoluteFile, "spriteModels.txt")).exists()) continue;
            this.initModData(mod);
        }
    }

    public void initGameData() {
        ModData data = new ModData("game", ZomboidFileSystem.instance.getMediaRootPath());
        data.spriteModels.init();
        this.modData.add(data);
    }

    public void initModData(ChooseGameInfo.Mod mod) {
        ModData data = new ModData(mod.getId(), mod.mediaFile.common.absoluteFile.getAbsolutePath());
        data.spriteModels.init();
        this.modData.add(data);
    }

    public ArrayList<String> getModIDs() {
        ArrayList<String> modIDs = new ArrayList<String>();
        for (int i = 0; i < this.modData.size(); ++i) {
            ModData data = this.modData.get(i);
            modIDs.add(data.modId);
        }
        return modIDs;
    }

    ModData getModData(String modID) {
        for (int i = 0; i < this.modData.size(); ++i) {
            ModData data = this.modData.get(i);
            if (!StringUtils.equals(modID, data.modId)) continue;
            return data;
        }
        return null;
    }

    public void setTileProperties(String modID, String tilesetName, int col, int row, SpriteModel spriteModel) {
        this.getModData((String)modID).spriteModels.setTileProperties(tilesetName, col, row, spriteModel);
    }

    public SpriteModel getTileProperties(String modID, String tilesetName, int col, int row) {
        return this.getModData((String)modID).spriteModels.getTileProperties(tilesetName, col, row);
    }

    public void clearTileProperties(String modID, String tilesetName, int col, int row) {
        this.getModData((String)modID).spriteModels.clearTileProperties(tilesetName, col, row);
    }

    public SpriteModelsFile.Tileset findTileset(String modID, String tilesetName) {
        return this.getModData((String)modID).spriteModels.findTileset(tilesetName);
    }

    public void toScriptManager(String modID) {
        this.getModData((String)modID).spriteModels.toScriptManager();
    }

    public void toScriptManager() {
        for (int i = 0; i < this.modData.size(); ++i) {
            this.modData.get((int)i).spriteModels.toScriptManager();
        }
    }

    public void loadedTileDefinitions() {
        this.loadedTileDefinitions = true;
        this.initSprites();
    }

    public void initSprites() {
        for (IsoSprite sprite : IsoSpriteManager.instance.namedMap.values()) {
            sprite.spriteModel = null;
        }
        for (int i = 0; i < this.modData.size(); ++i) {
            ModData data = this.modData.get(i);
            data.spriteModels.initSprites();
        }
    }

    public void write(String modID) {
        this.getModData((String)modID).spriteModels.write();
    }

    public void Reset() {
        for (ModData data : this.modData) {
            data.spriteModels.Reset();
        }
        this.modData.clear();
        this.loadedTileDefinitions = false;
    }

    static final class ModData {
        final String modId;
        final String mediaAbsPath;
        final SpriteModels spriteModels;

        ModData(String modId, String mediaAbsPath) {
            this.modId = modId;
            this.mediaAbsPath = mediaAbsPath;
            this.spriteModels = new SpriteModels(mediaAbsPath);
        }
    }
}

