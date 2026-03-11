/*
 * Decompiled with CFR 0.152.
 */
package zombie.worldMap.symbols;

import java.util.ArrayList;
import java.util.HashMap;
import zombie.UsedFromLua;
import zombie.core.textures.Texture;

@UsedFromLua
public final class MapSymbolDefinitions {
    private static MapSymbolDefinitions instance;
    private final ArrayList<MapSymbolDefinition> symbolList = new ArrayList();
    private final HashMap<String, MapSymbolDefinition> symbolById = new HashMap();

    public static MapSymbolDefinitions getInstance() {
        if (instance == null) {
            instance = new MapSymbolDefinitions();
        }
        return instance;
    }

    public void addTexture(String id, String path, int width, int height, String tab) {
        MapSymbolDefinition def = new MapSymbolDefinition();
        def.id = id;
        def.texturePath = path;
        def.width = width;
        def.height = height;
        if (tab != null) {
            def.tab = tab;
        }
        this.symbolList.add(def);
        this.symbolById.put(id, def);
    }

    public void addTexture(String id, String path) {
        Texture texture = Texture.getSharedTexture(path);
        if (texture == null) {
            this.addTexture(id, path, 18, 18, null);
            return;
        }
        this.addTexture(id, path, 20, 20, null);
    }

    public void addTexture(String id, String path, String tab) {
        Texture texture = Texture.getSharedTexture(path);
        if (texture == null) {
            this.addTexture(id, path, 18, 18, tab);
            return;
        }
        this.addTexture(id, path, 20, 20, tab);
    }

    public int getSymbolCount() {
        return this.symbolList.size();
    }

    public MapSymbolDefinition getSymbolByIndex(int index) {
        return this.symbolList.get(index);
    }

    public MapSymbolDefinition getSymbolById(String id) {
        return this.symbolById.get(id);
    }

    public static void Reset() {
        if (instance == null) {
            return;
        }
        MapSymbolDefinitions.getInstance().symbolList.clear();
        MapSymbolDefinitions.getInstance().symbolById.clear();
    }

    @UsedFromLua
    public static final class MapSymbolDefinition {
        private String id;
        private String texturePath;
        private int width;
        private int height;
        private String tab;

        public String getId() {
            return this.id;
        }

        public String getTexturePath() {
            return this.texturePath;
        }

        public int getWidth() {
            return this.width;
        }

        public int getHeight() {
            return this.height;
        }

        public String getTab() {
            return this.tab;
        }
    }
}

