/*
 * Decompiled with CFR 0.152.
 */
package zombie.worldMap.symbols;

import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Objects;
import se.krka.kahlua.j2se.KahluaTableImpl;
import se.krka.kahlua.vm.JavaFunction;
import se.krka.kahlua.vm.KahluaTable;
import se.krka.kahlua.vm.LuaClosure;
import zombie.Lua.LuaManager;
import zombie.UsedFromLua;
import zombie.ZomboidFileSystem;
import zombie.core.BoxedStaticValues;
import zombie.core.math.PZMath;
import zombie.inventory.types.MapItem;
import zombie.network.GameClient;
import zombie.ui.TextManager;
import zombie.ui.UIFont;
import zombie.util.Pool;
import zombie.util.PooledObject;
import zombie.util.StringUtils;
import zombie.util.Type;
import zombie.worldMap.UIWorldMap;
import zombie.worldMap.network.WorldMapClient;
import zombie.worldMap.network.WorldMapSymbolNetworkInfo;
import zombie.worldMap.styles.WorldMapStyle;
import zombie.worldMap.styles.WorldMapTextStyleLayer;
import zombie.worldMap.symbols.IWorldMapSymbolListener;
import zombie.worldMap.symbols.SymbolLayout;
import zombie.worldMap.symbols.TextLayout;
import zombie.worldMap.symbols.WorldMapBaseSymbol;
import zombie.worldMap.symbols.WorldMapSymbols;
import zombie.worldMap.symbols.WorldMapSymbolsAPI;
import zombie.worldMap.symbols.WorldMapTextSymbol;
import zombie.worldMap.symbols.WorldMapTextureSymbol;

@UsedFromLua
public final class WorldMapSymbolsV2
extends WorldMapSymbolsAPI {
    private static final Pool<WorldMapTextSymbolV2> s_textPool = new Pool<WorldMapTextSymbolV2>(WorldMapTextSymbolV2::new);
    private static final Pool<WorldMapTextureSymbolV2> s_texturePool = new Pool<WorldMapTextureSymbolV2>(WorldMapTextureSymbolV2::new);
    private final UIWorldMap ui;
    private final WorldMapSymbols uiSymbols;
    private final ArrayList<WorldMapBaseSymbolV2> symbols = new ArrayList();
    private final Listener listener = new Listener(this);
    static final ThreadLocal<TextLayout> TL_textLayout = ThreadLocal.withInitial(TextLayout::new);

    public WorldMapSymbolsV2(UIWorldMap ui, WorldMapSymbols symbols) {
        Objects.requireNonNull(ui);
        this.ui = ui;
        this.uiSymbols = symbols;
        this.uiSymbols.addListener(this.listener);
        this.reinit();
    }

    public WorldMapTextSymbolV2 addTranslatedText(String text, UIFont font, float x, float y) {
        return this.addTranslatedText(text, WorldMapSymbols.getDefaultTextLayerID(), x, y);
    }

    public WorldMapTextSymbolV2 addUntranslatedText(String text, UIFont font, float x, float y) {
        return this.addUntranslatedText(text, WorldMapSymbols.getDefaultTextLayerID(), x, y);
    }

    public WorldMapTextSymbolV2 addTranslatedText(String text, String layerID, float x, float y) {
        WorldMapTextSymbol symbol = this.uiSymbols.addTranslatedText(text, layerID, x, y, 1.0f, 1.0f, 1.0f, 1.0f);
        return (WorldMapTextSymbolV2)this.symbols.get(this.uiSymbols.indexOf(symbol));
    }

    public WorldMapTextSymbolV2 addUntranslatedText(String text, String layerID, float x, float y) {
        WorldMapTextSymbol symbol = this.uiSymbols.addUntranslatedText(text, layerID, x, y, 1.0f, 1.0f, 1.0f, 1.0f);
        return (WorldMapTextSymbolV2)this.symbols.get(this.uiSymbols.indexOf(symbol));
    }

    public WorldMapTextureSymbolV2 addTexture(String symbolID, float x, float y) {
        WorldMapTextureSymbol symbol = this.uiSymbols.addTexture(symbolID, x, y, 1.0f, 1.0f, 1.0f, 1.0f);
        return (WorldMapTextureSymbolV2)this.symbols.get(this.uiSymbols.indexOf(symbol));
    }

    public int hitTest(float uiX, float uiY) {
        return this.uiSymbols.hitTest(this.ui, uiX, uiY);
    }

    public int getSymbolCount() {
        return this.symbols.size();
    }

    public WorldMapBaseSymbolV2 getSymbolByIndex(int index) {
        return this.symbols.get(index);
    }

    public void removeSymbolByIndex(int index) {
        this.uiSymbols.removeSymbolByIndex(index);
    }

    public void removeSymbol(WorldMapBaseSymbolV2 symbol) {
        int index = this.uiSymbols.indexOf(symbol.symbol);
        if (index == -1) {
            throw new IllegalArgumentException("invalid symbol");
        }
        this.removeSymbolByIndex(index);
    }

    public String getDefaultLayerID() {
        return WorldMapSymbols.getDefaultTextLayerID();
    }

    public float getDisplayScale(String layerID, float scale, boolean bApplyZoom) {
        WorldMapTextStyleLayer textLayer = this.ui.getAPI().getStyle().getTextStyleLayerOrDefault(layerID);
        UIFont font = textLayer.getFont();
        scale *= textLayer.calculateScale(this.ui);
        if (bApplyZoom) {
            float worldScale = this.ui.getAPI().getWorldScale();
            scale = this.ui.getAPI().getBoolean("MiniMapSymbols") ? PZMath.min(worldScale, 1.0f) : (scale *= worldScale);
        }
        return scale;
    }

    public int getTextLayoutWidth(String text, String layerID) {
        WorldMapStyle style = this.ui.getAPI().getStyle();
        WorldMapTextStyleLayer textLayer = style.getTextStyleLayerOrDefault(layerID);
        TextLayout textLayout = TL_textLayout.get();
        textLayout.set(text, textLayer);
        return textLayout.maxLineLength;
    }

    public int getTextLayoutHeight(String text, String layerID) {
        WorldMapStyle style = this.ui.getAPI().getStyle();
        WorldMapTextStyleLayer textLayer = style.getTextStyleLayerOrDefault(layerID);
        TextLayout textLayout = TL_textLayout.get();
        textLayout.set(text, textLayer);
        return textLayout.numLines * TextManager.instance.getFontHeight(textLayer.getFont());
    }

    public boolean isUserEditing() {
        return this.uiSymbols.isUserEditing();
    }

    public void setUserEditing(boolean b) {
        this.uiSymbols.setUserEditing(b);
    }

    public String getDefaultTextLayerID() {
        return "text-note";
    }

    public void clear() {
        this.uiSymbols.clear();
    }

    public void reinitDefaultAnnotations() {
        MapItem singleton = MapItem.getSingleton();
        if (singleton != null) {
            singleton.clearDefaultAnnotations();
        }
    }

    public void initDefaultAnnotations() {
        MapItem singleton = MapItem.getSingleton();
        if (singleton != null && this.ui.getSymbolsDirect() == singleton.getSymbols() && !singleton.checkDefaultAnnotationsLoaded()) {
            return;
        }
        ArrayList<String> lotDirs = LuaManager.GlobalObject.getLotDirectories();
        if (lotDirs == null || lotDirs.isEmpty()) {
            return;
        }
        for (int i = 0; i < lotDirs.size(); ++i) {
            Object functionObj;
            String dirName = lotDirs.get(i);
            String absFilePath = ZomboidFileSystem.instance.getString("media/maps/" + dirName + "/worldmap-annotations.lua");
            Path path = FileSystems.getDefault().getPath(absFilePath, new String[0]);
            if (!Files.exists(path, new LinkOption[0]) || !((functionObj = LuaManager.GlobalObject.reloadLuaFile(absFilePath)) instanceof JavaFunction) && !(functionObj instanceof LuaClosure)) continue;
            LuaManager.caller.pcallvoid(LuaManager.thread, functionObj, this.ui.getTable());
        }
    }

    public void clearUserAnnotations() {
        this.uiSymbols.clearUserAnnotations();
    }

    private void checkLayout() {
        this.ui.checkSymbolsLayout();
    }

    private void reinit() {
        int i;
        for (i = 0; i < this.symbols.size(); ++i) {
            this.symbols.get(i).release();
        }
        this.symbols.clear();
        for (i = 0; i < this.uiSymbols.getSymbolCount(); ++i) {
            WorldMapBaseSymbolV2 symbolV2;
            WorldMapBaseSymbol symbol = this.uiSymbols.getSymbolByIndex(i);
            if (symbol instanceof WorldMapTextSymbol) {
                WorldMapTextSymbol textSymbol = (WorldMapTextSymbol)symbol;
                symbolV2 = s_textPool.alloc().init(this, textSymbol);
                this.symbols.add(symbolV2);
            }
            if (!(symbol instanceof WorldMapTextureSymbol)) continue;
            WorldMapTextureSymbol textureSymbol = (WorldMapTextureSymbol)symbol;
            symbolV2 = s_texturePool.alloc().init(this, textureSymbol);
            this.symbols.add(symbolV2);
        }
    }

    public void sendShareSymbol(WorldMapBaseSymbolV2 symbolV2, WorldMapSymbolNetworkInfo networkInfo) {
        WorldMapClient.getInstance().sendShareSymbol(symbolV2.symbol, networkInfo);
    }

    public void sendRemoveSymbol(WorldMapBaseSymbolV2 symbolV2) {
        WorldMapClient.getInstance().sendRemoveSymbol(symbolV2.symbol);
    }

    public void sendModifySymbol(WorldMapBaseSymbolV2 symbolV2) {
        WorldMapClient.getInstance().sendModifySymbol(symbolV2.symbol);
    }

    public void sendSetPrivateSymbol(WorldMapBaseSymbolV2 symbolV2) {
        WorldMapClient.getInstance().sendSetPrivateSymbol(symbolV2.symbol);
    }

    public static void setExposed(LuaManager.Exposer exposer) {
        exposer.setExposed(WorldMapSymbolsV2.class);
        exposer.setExposed(WorldMapTextSymbolV2.class);
        exposer.setExposed(WorldMapTextureSymbolV2.class);
    }

    private static final class Listener
    implements IWorldMapSymbolListener {
        final WorldMapSymbolsV2 api;

        Listener(WorldMapSymbolsV2 api) {
            this.api = api;
        }

        @Override
        public void onAdd(WorldMapBaseSymbol symbol) {
            int index = this.indexOf(symbol);
            if (symbol instanceof WorldMapTextSymbol) {
                WorldMapTextSymbol textSymbol = (WorldMapTextSymbol)symbol;
                WorldMapTextSymbolV2 symbolV2 = s_textPool.alloc().init(this.api, textSymbol);
                this.api.symbols.add(index, symbolV2);
                return;
            }
            if (symbol instanceof WorldMapTextureSymbol) {
                WorldMapTextureSymbol textureSymbol = (WorldMapTextureSymbol)symbol;
                WorldMapTextureSymbolV2 symbolV2 = s_texturePool.alloc().init(this.api, textureSymbol);
                this.api.symbols.add(index, symbolV2);
                return;
            }
            throw new RuntimeException("unhandled symbol class " + symbol.getClass().getSimpleName());
        }

        @Override
        public void onBeforeRemove(WorldMapBaseSymbol symbol) {
            int index = this.indexOf(symbol);
            WorldMapBaseSymbolV2 symbolV2 = this.api.symbols.remove(index);
            symbolV2.release();
        }

        @Override
        public void onAfterRemove(WorldMapBaseSymbol symbol) {
        }

        @Override
        public void onBeforeClear() {
        }

        @Override
        public void onAfterClear() {
            this.api.reinit();
        }

        int indexOf(WorldMapBaseSymbol symbol) {
            return this.api.uiSymbols.indexOf(symbol);
        }
    }

    @UsedFromLua
    public static class WorldMapTextSymbolV2
    extends WorldMapBaseSymbolV2 {
        WorldMapTextSymbol textSymbol;

        WorldMapTextSymbolV2 init(WorldMapSymbolsV2 owner, WorldMapTextSymbol symbol) {
            super.init(owner, symbol);
            this.textSymbol = symbol;
            return this;
        }

        public void setTranslatedText(String text) {
            if (StringUtils.isNullOrWhitespace(text)) {
                return;
            }
            this.textSymbol.setTranslatedText(text);
            this.owner.uiSymbols.invalidateLayout();
        }

        public void setUntranslatedText(String text) {
            if (StringUtils.isNullOrWhitespace(text)) {
                return;
            }
            this.textSymbol.setUntranslatedText(text);
            this.owner.uiSymbols.invalidateLayout();
        }

        public String getTranslatedText() {
            return this.textSymbol.getTranslatedText();
        }

        public String getUntranslatedText() {
            return this.textSymbol.getUntranslatedText();
        }

        public String getLayerID() {
            return this.textSymbol.getLayerID();
        }

        public void setLayerID(String layerID) {
            this.textSymbol.setLayerID(layerID);
            this.owner.uiSymbols.invalidateLayout();
        }

        public UIFont getFont() {
            return this.textSymbol.getFont(this.owner.ui);
        }

        @Override
        public boolean isText() {
            return true;
        }
    }

    @UsedFromLua
    public static class WorldMapTextureSymbolV2
    extends WorldMapBaseSymbolV2 {
        WorldMapTextureSymbol textureSymbol;

        WorldMapTextureSymbolV2 init(WorldMapSymbolsV2 owner, WorldMapTextureSymbol symbol) {
            super.init(owner, symbol);
            this.textureSymbol = symbol;
            return this;
        }

        public String getSymbolID() {
            return this.textureSymbol.getSymbolID();
        }

        @Override
        public boolean isTexture() {
            return true;
        }
    }

    protected static class WorldMapBaseSymbolV2
    extends PooledObject {
        WorldMapSymbolsV2 owner;
        WorldMapBaseSymbol symbol;

        protected WorldMapBaseSymbolV2() {
        }

        WorldMapBaseSymbolV2 init(WorldMapSymbolsV2 owner, WorldMapBaseSymbol symbol) {
            this.owner = owner;
            this.symbol = symbol;
            return this;
        }

        public float getWorldX() {
            return this.symbol.x;
        }

        public float getWorldY() {
            return this.symbol.y;
        }

        public boolean isUserDefined() {
            return this.symbol.isUserDefined();
        }

        public void setUserDefined(boolean b) {
            this.symbol.setUserDefined(b);
        }

        public float getScale() {
            return this.symbol.scale;
        }

        public float getDisplayX() {
            this.owner.checkLayout();
            SymbolLayout layout = this.owner.ui.getSymbolsLayoutData().getLayout(this.symbol);
            return layout.x + this.owner.ui.getAPIv1().worldOriginX();
        }

        public float getDisplayY() {
            this.owner.checkLayout();
            SymbolLayout layout = this.owner.ui.getSymbolsLayoutData().getLayout(this.symbol);
            return layout.y + this.owner.ui.getAPIv1().worldOriginY();
        }

        public float getDisplayWidth() {
            this.owner.checkLayout();
            return this.symbol.widthScaled(this.owner.ui);
        }

        public float getDisplayHeight() {
            this.owner.checkLayout();
            return this.symbol.heightScaled(this.owner.ui);
        }

        public float getDisplayScale() {
            this.owner.checkLayout();
            return this.symbol.getDisplayScale(this.owner.ui);
        }

        public void setAnchor(float x, float y) {
            this.symbol.setAnchor(x, y);
        }

        public float getAnchorX() {
            return this.symbol.getAnchorX();
        }

        public float getAnchorY() {
            return this.symbol.getAnchorY();
        }

        public float getRotation() {
            return this.symbol.getRotation();
        }

        public void setRotation(float degrees) {
            this.symbol.setRotation(degrees);
        }

        public boolean isMatchPerspective() {
            return this.symbol.isMatchPerspective();
        }

        public void setMatchPerspective(boolean b) {
            this.symbol.setMatchPerspective(b);
        }

        public boolean isApplyZoom() {
            return this.symbol.isApplyZoom();
        }

        public void setApplyZoom(boolean b) {
            this.symbol.setApplyZoom(b);
        }

        public float getMinZoom() {
            return this.symbol.getMinZoom();
        }

        public void setMinZoom(float zoomF) {
            this.symbol.setMinZoom(zoomF);
        }

        public float getMaxZoom() {
            return this.symbol.getMaxZoom();
        }

        public void setMaxZoom(float zoomF) {
            this.symbol.setMaxZoom(zoomF);
        }

        public void setPosition(float x, float y) {
            this.symbol.setPosition(x, y);
            this.owner.uiSymbols.invalidateLayout();
        }

        public void setCollide(boolean collide) {
            this.symbol.setCollide(collide);
        }

        public void setVisible(boolean visible) {
            this.symbol.setVisible(visible);
        }

        public boolean isVisible() {
            return this.symbol.isVisible(this.owner.ui);
        }

        public void setRGBA(float r, float g, float b, float a) {
            this.symbol.setRGBA(r, g, b, a);
        }

        public float getRed() {
            return this.symbol.r;
        }

        public float getGreen() {
            return this.symbol.g;
        }

        public float getBlue() {
            return this.symbol.b;
        }

        public float getAlpha() {
            return this.symbol.a;
        }

        public boolean hasCustomColor() {
            return this.symbol.hasCustomColor();
        }

        public void setScale(float scale) {
            this.symbol.setScale(scale);
            this.owner.uiSymbols.invalidateLayout();
        }

        public boolean isText() {
            return false;
        }

        public boolean isTexture() {
            return false;
        }

        public void setSharing(KahluaTable table) {
            KahluaTableImpl playerTable;
            if (table == null || table.isEmpty()) {
                if (this.symbol.isPrivate()) {
                    return;
                }
                this.owner.sendSetPrivateSymbol(this);
                return;
            }
            KahluaTableImpl tableImpl = (KahluaTableImpl)table;
            WorldMapSymbolNetworkInfo networkInfo = new WorldMapSymbolNetworkInfo();
            networkInfo.setAuthor(GameClient.username);
            boolean bEveryone = tableImpl.rawgetBool("everyone");
            boolean bFaction = tableImpl.rawgetBool("faction");
            boolean bSafehouse = tableImpl.rawgetBool("safehouse");
            networkInfo.setVisibleToEveryone(bEveryone);
            networkInfo.setVisibleToFaction(bFaction && !bEveryone);
            networkInfo.setVisibleToSafehouse(bSafehouse && !bEveryone);
            if (!bEveryone && (playerTable = Type.tryCastTo(tableImpl.rawget("players"), KahluaTableImpl.class)) != null && !playerTable.isEmpty()) {
                int count = playerTable.len();
                for (int i = 1; i <= count; ++i) {
                    String username = playerTable.rawgetStr(BoxedStaticValues.toDouble(i));
                    networkInfo.addPlayer(username);
                }
            }
            this.owner.sendShareSymbol(this, networkInfo);
        }

        public boolean isShared() {
            return this.symbol.getNetworkInfo() != null;
        }

        public boolean isPrivate() {
            return this.symbol.getNetworkInfo() == null;
        }

        public String getAuthor() {
            return this.isShared() ? this.symbol.getNetworkInfo().getAuthor() : GameClient.username;
        }

        public boolean isVisibleToEveryone() {
            return this.isShared() && this.symbol.getNetworkInfo().isVisibleToEveryone();
        }

        public boolean isVisibleToFaction() {
            return this.isShared() && this.symbol.getNetworkInfo().isVisibleToFaction();
        }

        public boolean isVisibleToSafehouse() {
            return this.isShared() && this.symbol.getNetworkInfo().isVisibleToSafehouse();
        }

        public int getVisibleToPlayerCount() {
            return this.isShared() ? this.symbol.getNetworkInfo().getPlayerCount() : 0;
        }

        public String getVisibleToPlayerByIndex(int index) {
            return this.isShared() ? this.symbol.getNetworkInfo().getPlayerByIndex(index) : null;
        }

        public boolean canClientModify() {
            return this.isShared() ? StringUtils.equals(GameClient.username, this.getAuthor()) : true;
        }

        public void renderOutline(float r, float g, float b, float a, float thickness) {
            this.symbol.renderOutline(this.owner.ui, r, g, b, a, thickness);
        }

        public WorldMapBaseSymbolV2 createCopy() {
            WorldMapBaseSymbol copy = this.symbol.createCopy();
            this.owner.uiSymbols.addSymbol(copy);
            return this.owner.getSymbolByIndex(this.owner.uiSymbols.indexOf(copy));
        }
    }
}

