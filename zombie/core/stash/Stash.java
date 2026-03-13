/*
 * Decompiled with CFR 0.152.
 */
package zombie.core.stash;

import java.util.ArrayList;
import se.krka.kahlua.j2se.KahluaTableImpl;
import se.krka.kahlua.vm.KahluaTable;
import se.krka.kahlua.vm.KahluaTableIterator;
import zombie.UsedFromLua;
import zombie.core.Translator;
import zombie.core.stash.StashAnnotation;
import zombie.core.stash.StashBuilding;
import zombie.core.stash.StashContainer;
import zombie.core.stash.StashSystem;
import zombie.debug.DebugLog;
import zombie.scripting.ScriptManager;
import zombie.worldMap.UIWorldMap;
import zombie.worldMap.symbols.WorldMapTextSymbol;
import zombie.worldMap.symbols.WorldMapTextureSymbol;

@UsedFromLua
public final class Stash {
    public String name;
    public String type;
    public String item;
    public String customName;
    public int buildingX;
    public int buildingY;
    public String spawnTable;
    public ArrayList<StashAnnotation> annotations;
    public boolean spawnOnlyOnZed;
    public int minDayToSpawn = -1;
    public int maxDayToSpawn = -1;
    public int minTrapToSpawn = -1;
    public int maxTrapToSpawn = -1;
    public int zombies;
    public ArrayList<StashContainer> containers;
    public int barricades;

    public Stash(String name) {
        this.name = name;
    }

    public void load(KahluaTableImpl stashDesc) {
        KahluaTableImpl luaAnnotations;
        KahluaTable luaContainers;
        String trapsToSpawn;
        this.type = stashDesc.rawgetStr("type");
        this.item = stashDesc.rawgetStr("item");
        StashBuilding stashBuilding = new StashBuilding(this.name, stashDesc.rawgetInt("buildingX"), stashDesc.rawgetInt("buildingY"));
        StashSystem.possibleStashes.add(stashBuilding);
        this.buildingX = stashBuilding.buildingX;
        this.buildingY = stashBuilding.buildingY;
        this.spawnTable = stashDesc.rawgetStr("spawnTable");
        this.customName = Translator.getText(stashDesc.rawgetStr("customName"));
        this.zombies = stashDesc.rawgetInt("zombies");
        this.barricades = stashDesc.rawgetInt("barricades");
        this.spawnOnlyOnZed = stashDesc.rawgetBool("spawnOnlyOnZed");
        String daysToSpawn = stashDesc.rawgetStr("daysToSpawn");
        if (daysToSpawn != null) {
            String[] days = daysToSpawn.split("-");
            if (days.length == 2) {
                this.minDayToSpawn = Integer.parseInt(days[0]);
                this.maxDayToSpawn = Integer.parseInt(days[1]);
            } else {
                this.minDayToSpawn = Integer.parseInt(days[0]);
            }
        }
        if ((trapsToSpawn = stashDesc.rawgetStr("traps")) != null) {
            String[] traps = trapsToSpawn.split("-");
            if (traps.length == 2) {
                this.minTrapToSpawn = Integer.parseInt(traps[0]);
                this.maxTrapToSpawn = Integer.parseInt(traps[1]);
            } else {
                this.maxTrapToSpawn = this.minTrapToSpawn = Integer.parseInt(traps[0]);
            }
        }
        if ((luaContainers = (KahluaTable)stashDesc.rawget("containers")) != null) {
            this.containers = new ArrayList();
            KahluaTableIterator it = luaContainers.iterator();
            while (it.advance()) {
                KahluaTableImpl contDesc = (KahluaTableImpl)it.getValue();
                StashContainer cont = new StashContainer(contDesc.rawgetStr("room"), contDesc.rawgetStr("containerSprite"), contDesc.rawgetStr("containerType"));
                cont.contX = contDesc.rawgetInt("contX");
                cont.contY = contDesc.rawgetInt("contY");
                cont.contZ = contDesc.rawgetInt("contZ");
                cont.containerItem = contDesc.rawgetStr("containerItem");
                if (cont.containerItem != null && ScriptManager.instance.getItem(cont.containerItem) == null) {
                    DebugLog.General.error("Stash containerItem \"%s\" doesn't exist.", cont.containerItem);
                }
                this.containers.add(cont);
            }
        }
        if ("Map".equals(this.type) && (luaAnnotations = (KahluaTableImpl)stashDesc.rawget("annotations")) != null) {
            this.annotations = new ArrayList();
            KahluaTableIterator it = luaAnnotations.iterator();
            while (it.advance()) {
                KahluaTable luaAnnotation = (KahluaTable)it.getValue();
                StashAnnotation annotation = new StashAnnotation();
                annotation.fromLua(luaAnnotation);
                this.annotations.add(annotation);
            }
        }
    }

    public String getName() {
        return this.name;
    }

    public String getItem() {
        return this.item;
    }

    public int getBuildingX() {
        return this.buildingX;
    }

    public int getBuildingY() {
        return this.buildingY;
    }

    public void applyAnnotations(UIWorldMap ui) {
        if (this.annotations == null) {
            return;
        }
        if (ui.getSymbolsDirect() == null) {
            return;
        }
        for (int i = 0; i < this.annotations.size(); ++i) {
            StashAnnotation annotation = this.annotations.get(i);
            if (annotation.symbol != null) {
                float anchorX = Float.isNaN(annotation.anchorX) ? 0.5f : annotation.anchorX;
                float anchorY = Float.isNaN(annotation.anchorY) ? 0.5f : annotation.anchorY;
                WorldMapTextureSymbol symbol = ui.getSymbolsDirect().addTexture(annotation.symbol, annotation.x, annotation.y, anchorX, anchorY, 0.666f, annotation.r, annotation.g, annotation.b, 1.0f);
                if (Float.isNaN(annotation.rotation)) continue;
                symbol.setRotation(annotation.rotation);
                continue;
            }
            if (annotation.text == null) continue;
            WorldMapTextSymbol symbol = Translator.getTextOrNull(annotation.text) == null ? ui.getSymbolsDirect().addTranslatedText(annotation.text, "note", annotation.x, annotation.y, annotation.r, annotation.g, annotation.b, 1.0f) : ui.getSymbolsDirect().addUntranslatedText(annotation.text, "note", annotation.x, annotation.y, annotation.r, annotation.g, annotation.b, 1.0f);
            if (!Float.isNaN(annotation.anchorX) && !Float.isNaN(annotation.anchorY)) {
                symbol.setAnchor(annotation.anchorX, annotation.anchorY);
            }
            if (Float.isNaN(annotation.rotation)) continue;
            symbol.setRotation(annotation.rotation);
        }
    }
}

