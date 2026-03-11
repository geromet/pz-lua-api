/*
 * Decompiled with CFR 0.152.
 */
package zombie.entity.components.spriteconfig;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import zombie.UsedFromLua;
import zombie.core.Core;
import zombie.core.properties.PropertyContainer;
import zombie.core.textures.Texture;
import zombie.debug.DebugLog;
import zombie.debug.objects.DebugClassFields;
import zombie.entity.ComponentType;
import zombie.iso.IsoObject;
import zombie.iso.SpriteDetails.IsoFlagType;
import zombie.iso.sprite.IsoSprite;
import zombie.iso.sprite.IsoSpriteManager;
import zombie.scripting.ScriptManager;
import zombie.scripting.entity.GameEntityScript;
import zombie.scripting.entity.components.crafting.CraftRecipeComponentScript;
import zombie.scripting.entity.components.spriteconfig.SpriteConfigScript;
import zombie.world.ScriptsDictionary;

@UsedFromLua
public class SpriteConfigManager {
    public static final String FACE_SINGLE = "single";
    public static final String FACE_N = "n";
    public static final String FACE_W = "w";
    public static final String FACE_S = "s";
    public static final String FACE_E = "e";
    public static final String FACE_N_OPEN = "n_open";
    public static final String FACE_W_OPEN = "w_open";
    public static final int FACE_ID_SINGLE = 0;
    public static final int FACE_ID_N = 0;
    public static final int FACE_ID_W = 1;
    public static final int FACE_ID_S = 2;
    public static final int FACE_ID_E = 3;
    public static final int FACE_ID_CARDINAL_MAX = 4;
    public static final int FACE_ID_N_OPEN = 4;
    public static final int FACE_ID_W_OPEN = 5;
    public static final int FACE_ID_MAX = 6;
    private static boolean hasLoadErrors;
    private static final HashMap<String, ObjectInfo> objectInfos;
    private static final ArrayList<ObjectInfo> objectInfosList;
    private static final HashSet<IsoSprite> registeredScriptedSprites;
    private static final HashSet<String> tempFaceSprites;

    public static int GetFaceIdForString(String face) {
        if (face.equalsIgnoreCase(FACE_N)) {
            return 0;
        }
        if (face.equalsIgnoreCase(FACE_N_OPEN)) {
            return 4;
        }
        if (face.equalsIgnoreCase(FACE_E)) {
            return 3;
        }
        if (face.equalsIgnoreCase(FACE_S)) {
            return 2;
        }
        if (face.equalsIgnoreCase(FACE_W)) {
            return 1;
        }
        if (face.equalsIgnoreCase(FACE_W_OPEN)) {
            return 5;
        }
        if (face.equalsIgnoreCase(FACE_SINGLE)) {
            return 0;
        }
        return -1;
    }

    public static boolean HasLoadErrors() {
        return hasLoadErrors;
    }

    public static ObjectInfo GetObjectInfo(String name) {
        return objectInfos.get(name);
    }

    public static ObjectInfo getObjectInfoFromSprite(String spriteName) {
        for (ObjectInfo info : objectInfos.values()) {
            if (info.getFaceForSprite(spriteName) == null) continue;
            return info;
        }
        return null;
    }

    public static ArrayList<ObjectInfo> GetObjectInfoList() {
        return objectInfosList;
    }

    public static void Reset() {
        objectInfos.clear();
        objectInfosList.clear();
        registeredScriptedSprites.clear();
        hasLoadErrors = false;
    }

    public static void InitScriptsPostTileDef() {
        SpriteConfigManager.Reset();
        ArrayList<GameEntityScript> entityScripts = ScriptManager.instance.getAllGameEntities();
        for (GameEntityScript entityScript : entityScripts) {
            SpriteConfigManager.parseEntityScript(entityScript);
        }
        registeredScriptedSprites.clear();
    }

    private static void parseEntityScript(GameEntityScript entityScript) {
        try {
            SpriteConfigScript spriteConfig = (SpriteConfigScript)entityScript.getComponentScriptFor(ComponentType.SpriteConfig);
            if (spriteConfig != null) {
                IsoSprite spr;
                if (!spriteConfig.isValid()) {
                    throw new Exception("Invalid SpriteConfig script for entity '" + entityScript.getName() + "'");
                }
                if (!SpriteConfigManager.parseSpriteConfigScript(spriteConfig)) {
                    throw new Exception("Cannot parse SpriteConfig script for entity '" + entityScript.getName() + "'");
                }
                ArrayList<String> spriteNames = spriteConfig.getAllTileNames();
                for (String s : spriteNames) {
                    spr = IsoSpriteManager.instance.getSprite(s);
                    if (spr == null) {
                        throw new Exception("Sprite '" + s + "' does not exist. entity script: " + entityScript.getName());
                    }
                    if (registeredScriptedSprites.contains(spr)) {
                        throw new Exception("Sprite '" + s + "' is duplicate. entity script: " + entityScript.getName());
                    }
                    registeredScriptedSprites.add(spr);
                }
                for (String s : spriteNames) {
                    spr = IsoSpriteManager.instance.getSprite(s);
                    if (spr == null) continue;
                    PropertyContainer props = spr.getProperties();
                    props.set("EntityScriptName", entityScript.getName());
                    props.set(IsoFlagType.EntityScript);
                    props.CreateKeySet();
                    if (entityScript.getName() != null) continue;
                    DebugLog.log("type = " + entityScript.getScriptObjectFullType());
                    DebugLog.log("name = " + entityScript.getScriptObjectName());
                    throw new Exception("KABLAMO!");
                }
            }
        }
        catch (Exception e) {
            hasLoadErrors = true;
            e.printStackTrace();
        }
    }

    private static boolean parseSpriteConfigScript(SpriteConfigScript configScript) {
        try {
            if (!configScript.isValid()) {
                return false;
            }
            String entryName = configScript.getName();
            if (objectInfos.containsKey(entryName)) {
                throw new Exception("double entry, script: " + entryName);
            }
            ObjectInfo objectInfo = new ObjectInfo(configScript);
            long version = ScriptsDictionary.spriteConfigs.getVersionHash(configScript);
            objectInfo.setVersion(version);
            for (int faceID = 0; faceID < 6; ++faceID) {
                SpriteConfigScript.FaceScript faceScript;
                if (configScript.isSingleFace() && faceID != 0 || (faceScript = configScript.getFace(faceID)) == null) continue;
                FaceInfo face = objectInfo.CreateFace(faceScript.getFaceName(), faceScript.getTotalWidth(), faceScript.getTotalHeight(), faceScript.getZLayers());
                for (int z = 0; z < faceScript.getZLayers(); ++z) {
                    SpriteConfigScript.ZLayer layer = faceScript.getLayer(z);
                    for (int y = 0; y < layer.getHeight(); ++y) {
                        SpriteConfigScript.XRow row = layer.getRow(y);
                        for (int x = 0; x < row.getWidth(); ++x) {
                            SpriteConfigScript.TileScript tile = row.getTile(x);
                            if (tile.isEmptySpace()) {
                                face.CreateEmpty(tile.isBlocksSquare(), x, y, z);
                                continue;
                            }
                            String tileName = tile.getTileName();
                            IsoSprite spr = IsoSpriteManager.instance.getSprite(tileName);
                            if (spr == null) {
                                throw new Exception("Sprite is null '" + tileName + ", in script: " + entryName);
                            }
                            face.CreateTile(tile.getTileName(), x, y, z, false);
                        }
                    }
                }
                face.ensureTileInfos();
                tempFaceSprites.clear();
                for (int z = 0; z < face.tileInfos.length; ++z) {
                    for (int x = 0; x < face.tileInfos[z].length; ++x) {
                        for (int y = 0; y < face.tileInfos[z][x].length; ++y) {
                            TileInfo tileInfo = face.tileInfos[z][x][y];
                            tileInfo.masterOffsetX = face.getMasterX() - tileInfo.getX();
                            tileInfo.masterOffsetY = face.getMasterY() - tileInfo.getY();
                            tileInfo.masterOffsetZ = face.getMasterZ() - tileInfo.getZ();
                            if (!tileInfo.empty && tileInfo.tileSprite == null) {
                                throw new Exception("tile not empty, but sprite is null? face: " + face.getFaceName() + ", group: " + objectInfo.groupName);
                            }
                            if (tileInfo.tileSprite != null && tempFaceSprites.contains(tileInfo.tileSprite)) {
                                throw new Exception("double entry for sprite in face map: " + tileInfo.tileSprite + ", group: " + objectInfo.groupName);
                            }
                            if (tileInfo.tileSprite == null) continue;
                            tempFaceSprites.add(tileInfo.tileSprite);
                        }
                    }
                }
            }
            objectInfos.put(entryName, objectInfo);
            objectInfosList.add(objectInfo);
            return true;
        }
        catch (Exception e) {
            hasLoadErrors = true;
            e.printStackTrace();
            return false;
        }
    }

    static {
        objectInfos = new HashMap();
        objectInfosList = new ArrayList();
        registeredScriptedSprites = new HashSet();
        tempFaceSprites = new HashSet();
    }

    @DebugClassFields
    @UsedFromLua
    public static class ObjectInfo {
        private final SpriteConfigScript script;
        private final String groupName;
        private final FaceInfo[] faces = new FaceInfo[6];
        private boolean isSingleFace;
        private long version;
        private String mainSpriteCache;
        private final Texture iconTexture;

        private ObjectInfo(SpriteConfigScript script) {
            this.script = script;
            this.groupName = script.getName();
            if (script != null && !script.getAllTileNames().isEmpty()) {
                String spriteName = script.getAllTileNames().get(0);
                this.iconTexture = Texture.trygetTexture(spriteName);
            } else {
                this.iconTexture = null;
            }
        }

        public SpriteConfigScript getScript() {
            return this.script;
        }

        public CraftRecipeComponentScript getRecipe() {
            CraftRecipeComponentScript recipeScript;
            GameEntityScript gameEntityScript;
            if (this.script != null && (gameEntityScript = (GameEntityScript)this.script.getParent()) != null && (recipeScript = (CraftRecipeComponentScript)gameEntityScript.getComponentScriptFor(ComponentType.CraftRecipe)) != null) {
                return recipeScript;
            }
            return null;
        }

        public String getName() {
            return this.groupName;
        }

        public long getVersion() {
            return this.version;
        }

        private void setVersion(long version) {
            this.version = version;
        }

        private FaceInfo CreateFace(String face, int width, int height, int zLayers) {
            if ((face = face.toLowerCase()).equalsIgnoreCase(SpriteConfigManager.FACE_SINGLE)) {
                this.isSingleFace = true;
            }
            FaceInfo faceInfo = new FaceInfo(face, width, height, zLayers);
            int faceID = SpriteConfigManager.GetFaceIdForString(face);
            if (faceID != -1) {
                if (this.faces[faceID] == null) {
                    this.faces[faceID] = faceInfo;
                } else {
                    DebugLog.log("MultiTileObject -> CreateFace face already created faceID: " + face);
                    if (Core.debug) {
                        throw new RuntimeException("MultiTileObject -> CreateFace face already created faceID: " + face);
                    }
                }
            } else {
                DebugLog.log("MultiTileObject -> CreateFace cannot get correct faceID: " + face);
                if (Core.debug) {
                    throw new RuntimeException("MultiTileObject -> CreateFace cannot get correct faceID: " + face);
                }
                return null;
            }
            return faceInfo;
        }

        public String getMainSpriteNameUI() {
            if (this.mainSpriteCache == null) {
                for (int i = 0; i < 6; ++i) {
                    String s;
                    if (this.faces[i] == null || (s = this.faces[i].getMainSpriteName()) == null) continue;
                    this.mainSpriteCache = s;
                    break;
                }
            }
            return this.mainSpriteCache;
        }

        public boolean isSingleFace() {
            return this.isSingleFace;
        }

        public boolean canRotate() {
            if (this.isSingleFace) {
                return false;
            }
            int count = 0;
            for (int i = 0; i < 4; ++i) {
                if (this.faces[i] == null) continue;
                ++count;
            }
            return count > 1;
        }

        public FaceInfo getFace(String face) {
            return this.getFace(SpriteConfigManager.GetFaceIdForString(face));
        }

        public FaceInfo getFace(int faceID) {
            if (faceID < 0 || faceID > 6) {
                DebugLog.log("MultiTileObject -> getFace faceID out of bounds: " + faceID);
                if (Core.debug) {
                    throw new RuntimeException("MultiTileObject -> getFace faceID out of bounds: " + faceID);
                }
                return null;
            }
            return this.faces[faceID];
        }

        public FaceInfo getFaceForSprite(String spriteName) {
            for (int i = 0; i < this.faces.length; ++i) {
                if (this.faces[i] == null || !this.faces[i].sprites.containsKey(spriteName)) continue;
                return this.faces[i];
            }
            return null;
        }

        public boolean isProp() {
            return false;
        }

        public Texture getIconTexture() {
            return this.iconTexture;
        }

        public int getTime() {
            return 0;
        }

        public boolean needToBeLearn() {
            return false;
        }

        public List<String> getTags() {
            return new ArrayList<String>();
        }

        public int getRequiredSkillCount() {
            return 0;
        }
    }

    @DebugClassFields
    @UsedFromLua
    public static class FaceInfo {
        private final HashMap<String, TileInfo> sprites;
        private final String face;
        private final int width;
        private final int height;
        private final int zLayers;
        private final TileInfo[][][] tileInfos;
        private int masterPosX;
        private int masterPosY;
        private int masterPosZ;
        private boolean isMasterSet;
        private final boolean isMultiSquare;

        private FaceInfo(String face, int width, int height, int zLayers) {
            this.face = face;
            this.width = width;
            this.height = height;
            this.zLayers = zLayers;
            this.tileInfos = new TileInfo[zLayers][width][height];
            this.sprites = new HashMap(width * height * zLayers);
            this.isMultiSquare = width > 1 || height > 1 || zLayers > 1;
        }

        public String getFaceName() {
            return this.face;
        }

        public int getWidth() {
            return this.width;
        }

        public int getHeight() {
            return this.height;
        }

        public int getzLayers() {
            return this.zLayers;
        }

        public int getMasterX() {
            return this.masterPosX;
        }

        public int getMasterY() {
            return this.masterPosY;
        }

        public int getMasterZ() {
            return this.masterPosZ;
        }

        public boolean isMasterSet() {
            return this.isMasterSet;
        }

        public boolean isMultiSquare() {
            return this.isMultiSquare;
        }

        public TileInfo getMasterTileInfo() {
            return this.tileInfos[this.masterPosZ][this.masterPosX][this.masterPosY];
        }

        public boolean verifyObject(int x, int y, int z, IsoObject object) {
            TileInfo info = this.tileInfos[z][x][y];
            if (info != null) {
                return info.verifyObject(object);
            }
            return false;
        }

        private TileInfo CreateTile(String sprite, int gridPosX, int gridPosY, int gridPosZ, boolean isMaster) {
            TileInfo tileInfo;
            if (this.tileInfos[gridPosZ][gridPosX][gridPosY] != null) {
                DebugLog.log("FaceInfo -> CreateTile, tile already created: " + sprite);
                if (Core.debug) {
                    throw new RuntimeException("FaceInfo -> CreateTile, tile already created: " + sprite);
                }
                return null;
            }
            if (!this.isMasterSet) {
                tileInfo = new TileInfo(sprite, gridPosX, gridPosY, gridPosZ, true, false, true);
                this.masterPosX = gridPosX;
                this.masterPosY = gridPosY;
                this.masterPosZ = gridPosZ;
                this.isMasterSet = true;
            } else {
                tileInfo = new TileInfo(sprite, gridPosX, gridPosY, gridPosZ, false, false, true);
            }
            this.tileInfos[gridPosZ][gridPosX][gridPosY] = tileInfo;
            this.sprites.put(sprite, tileInfo);
            return tileInfo;
        }

        private TileInfo CreateEmpty(boolean blocks, int gridPosX, int gridPosY, int gridPosZ) {
            TileInfo tileInfo;
            this.tileInfos[gridPosZ][gridPosX][gridPosY] = tileInfo = new TileInfo(null, gridPosX, gridPosY, gridPosZ, false, true, blocks);
            return tileInfo;
        }

        public TileInfo getTileInfo(int x, int y, int z) {
            return this.tileInfos[z][x][y];
        }

        public TileInfo getTileInfoForSprite(String tile) {
            return this.sprites.get(tile);
        }

        protected void ensureTileInfos() {
            for (int z = 0; z < this.zLayers; ++z) {
                for (int x = 0; x < this.width; ++x) {
                    for (int y = 0; y < this.height; ++y) {
                        if (this.tileInfos[z][x][y] != null) continue;
                        this.CreateEmpty(false, x, y, z);
                    }
                }
            }
        }

        protected String getMainSpriteName() {
            for (int z = 0; z < this.zLayers; ++z) {
                for (int x = this.width - 1; x >= 0; --x) {
                    for (int y = this.height - 1; y >= 0; --y) {
                        if (this.tileInfos[z][x][y] != null || this.tileInfos[z][x][y].tileSprite == null) continue;
                        return this.tileInfos[z][x][y].tileSprite;
                    }
                }
            }
            return null;
        }
    }

    @DebugClassFields
    @UsedFromLua
    public static class TileInfo {
        private final String tileSprite;
        private final int x;
        private final int y;
        private final int z;
        private int masterOffsetX;
        private int masterOffsetY;
        private int masterOffsetZ;
        private final boolean master;
        private final boolean empty;
        private final boolean blocking;

        private TileInfo(String tileSprite, int x, int y, int z, boolean master, boolean empty, boolean blocking) {
            this.tileSprite = tileSprite;
            this.x = x;
            this.y = y;
            this.z = z;
            this.master = master;
            this.empty = empty;
            this.blocking = blocking;
        }

        public String getSpriteName() {
            return this.tileSprite;
        }

        public int getX() {
            return this.x;
        }

        public int getY() {
            return this.y;
        }

        public int getZ() {
            return this.z;
        }

        public boolean isMaster() {
            return this.master;
        }

        public boolean isEmpty() {
            return this.empty;
        }

        public boolean isBlocking() {
            return this.blocking;
        }

        public int getMasterOffsetX() {
            return this.masterOffsetX;
        }

        public int getMasterOffsetY() {
            return this.masterOffsetY;
        }

        public int getMasterOffsetZ() {
            return this.masterOffsetZ;
        }

        public boolean verifyObject(IsoObject object) {
            IsoSprite sprite;
            return !this.isEmpty() && (sprite = object.getSprite()) != null && sprite.getName() != null && sprite.getName().equalsIgnoreCase(this.getSpriteName());
        }
    }
}

