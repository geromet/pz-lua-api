/*
 * Decompiled with CFR 0.152.
 */
package zombie.modding;

import java.util.ArrayList;
import java.util.Objects;
import zombie.GameWindow;
import zombie.MapGroups;
import zombie.UsedFromLua;
import zombie.core.Core;
import zombie.core.SpriteRenderer;
import zombie.debug.DebugOptions;
import zombie.gameStates.ChooseGameInfo;
import zombie.ui.TextManager;
import zombie.ui.UIFont;
import zombie.util.StringUtils;
import zombie.util.list.PZArrayUtil;

@UsedFromLua
public final class ActiveMods {
    private static final ArrayList<ActiveMods> s_activeMods = new ArrayList();
    private static final ActiveMods s_loaded = new ActiveMods("loaded");
    private final String id;
    private final ArrayList<String> mods = new ArrayList();
    private final ArrayList<String> mapOrder = new ArrayList();

    private static int count() {
        return s_activeMods.size();
    }

    public static ActiveMods getByIndex(int index) {
        return s_activeMods.get(index);
    }

    public static ActiveMods getById(String id) {
        int index = ActiveMods.indexOf(id);
        if (index == -1) {
            return ActiveMods.create(id);
        }
        return s_activeMods.get(index);
    }

    public static int indexOf(String id) {
        id = id.trim();
        ActiveMods.requireValidId(id);
        for (int i = 0; i < s_activeMods.size(); ++i) {
            ActiveMods activeMods = s_activeMods.get(i);
            if (!activeMods.id.equalsIgnoreCase(id)) continue;
            return i;
        }
        return -1;
    }

    private static ActiveMods create(String id) {
        ActiveMods.requireValidId(id);
        if (ActiveMods.indexOf(id) != -1) {
            throw new IllegalStateException("id \"" + id + "\" exists");
        }
        ActiveMods activeMods = new ActiveMods(id);
        s_activeMods.add(activeMods);
        return activeMods;
    }

    private static void requireValidId(String id) {
        if (StringUtils.isNullOrWhitespace(id)) {
            throw new IllegalArgumentException("id is null or whitespace");
        }
    }

    public static void setLoadedMods(ActiveMods activeMods) {
        if (activeMods == null) {
            return;
        }
        s_loaded.copyFrom(activeMods);
    }

    public static boolean requiresResetLua(ActiveMods activeMods) {
        Objects.requireNonNull(activeMods);
        return !ActiveMods.s_loaded.mods.equals(activeMods.mods);
    }

    public static void renderUI() {
        if (!DebugOptions.instance.modRenderLoaded.getValue()) {
            return;
        }
        if (GameWindow.drawReloadingLua) {
            return;
        }
        UIFont font = UIFont.DebugConsole;
        int fontHgt = TextManager.instance.getFontHeight(font);
        String label = "Active Mods:";
        int width = TextManager.instance.MeasureStringX(font, "Active Mods:");
        for (int i = 0; i < ActiveMods.s_loaded.mods.size(); ++i) {
            String modID = ActiveMods.s_loaded.mods.get(i);
            int width1 = TextManager.instance.MeasureStringX(font, modID);
            width = Math.max(width, width1);
        }
        int pad = 10;
        int x = Core.width - 20 - (width += 20);
        int y = 20;
        int height = (1 + ActiveMods.s_loaded.mods.size()) * fontHgt + 20;
        SpriteRenderer.instance.renderi(null, x, y, width, height, 0.0f, 0.5f, 0.75f, 1.0f, null);
        TextManager.instance.DrawString(font, x + 10, y += 10, "Active Mods:", 1.0, 1.0, 1.0, 1.0);
        for (int i = 0; i < ActiveMods.s_loaded.mods.size(); ++i) {
            String modID = ActiveMods.s_loaded.mods.get(i);
            TextManager.instance.DrawString(font, x + 10, y += fontHgt, modID, 1.0, 1.0, 1.0, 1.0);
        }
    }

    public static void Reset() {
        s_loaded.clear();
    }

    public ActiveMods(String id) {
        ActiveMods.requireValidId(id);
        this.id = id;
    }

    public void clear() {
        this.mods.clear();
        this.mapOrder.clear();
    }

    public ArrayList<String> getMods() {
        return this.mods;
    }

    public ArrayList<String> getMapOrder() {
        return this.mapOrder;
    }

    public void copyFrom(ActiveMods other) {
        this.mods.clear();
        this.mapOrder.clear();
        PZArrayUtil.addAll(this.mods, other.mods);
        PZArrayUtil.addAll(this.mapOrder, other.mapOrder);
    }

    public void setModActive(String modID, boolean active) {
        if (StringUtils.isNullOrWhitespace(modID = modID.trim())) {
            return;
        }
        if (active) {
            if (!this.mods.contains(modID)) {
                this.mods.add(modID);
            }
        } else {
            this.mods.remove(modID);
        }
    }

    public boolean isModActive(String modID) {
        if (StringUtils.isNullOrWhitespace(modID = modID.trim())) {
            return false;
        }
        return this.mods.contains(modID);
    }

    public void removeMod(String modID) {
        modID = modID.trim();
        this.mods.remove(modID);
    }

    public void removeMapOrder(String folder) {
        this.mapOrder.remove(folder);
    }

    public void checkMissingMods() {
        if (this.mods.isEmpty()) {
            return;
        }
        for (int i = this.mods.size() - 1; i >= 0; --i) {
            String modID = this.mods.get(i);
            if (ChooseGameInfo.getAvailableModDetails(modID) != null) continue;
            this.mods.remove(i);
        }
    }

    public void checkMissingMaps() {
        if (this.mapOrder.isEmpty()) {
            return;
        }
        MapGroups mapGroups = new MapGroups();
        mapGroups.createGroups(this, false);
        if (mapGroups.checkMapConflicts()) {
            ArrayList<String> allMaps = mapGroups.getAllMapsInOrder();
            for (int i = this.mapOrder.size() - 1; i >= 0; --i) {
                String mapName = this.mapOrder.get(i);
                if (allMaps.contains(mapName)) continue;
                this.mapOrder.remove(i);
            }
        } else {
            this.mapOrder.clear();
        }
    }
}

