/*
 * Decompiled with CFR 0.152.
 */
package zombie.inventory.recipemanager;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import zombie.UsedFromLua;
import zombie.ZomboidFileSystem;
import zombie.characters.IsoGameCharacter;
import zombie.core.Color;
import zombie.core.Colors;
import zombie.core.Core;
import zombie.debug.DebugLog;
import zombie.inventory.InventoryItem;
import zombie.inventory.ItemContainer;
import zombie.scripting.objects.Recipe;
import zombie.util.StringUtils;

@UsedFromLua
public class RecipeMonitor {
    private static boolean enabled;
    private static boolean suspended;
    private static int monitorID;
    private static int tabs;
    private static String tabStr;
    private static final String tabSize = "  ";
    private static final Color defColor;
    public static final Color colGray;
    public static final Color colNeg;
    public static final Color colPos;
    public static final Color colHeader;
    private static final ArrayList<String> lines;
    private static final ArrayList<Color> colors;
    private static String recipeName;
    private static Recipe lastRecipe;
    private static final ArrayList<String> recipeLines;

    public static void Enable(boolean b) {
        enabled = b;
    }

    public static boolean IsEnabled() {
        return enabled;
    }

    public static int getMonitorID() {
        return monitorID;
    }

    public static void StartMonitor() {
        if (enabled) {
            ++monitorID;
            suspended = false;
            lines.clear();
            colors.clear();
            recipeLines.clear();
            recipeName = "none";
            lastRecipe = null;
            RecipeMonitor.ResetTabs();
            RecipeMonitor.Log("MonitorID = " + monitorID);
        }
    }

    public static Color getColGray() {
        return colGray;
    }

    public static Color getColBlack() {
        return Color.black;
    }

    public static void setRecipe(Recipe recipe) {
        recipeName = recipe.getOriginalname();
        lastRecipe = recipe;
    }

    public static String getRecipeName() {
        return recipeName;
    }

    public static Recipe getRecipe() {
        return lastRecipe;
    }

    @Deprecated
    public static ArrayList<String> getRecipeLines() {
        return recipeLines;
    }

    public static boolean canLog() {
        return Core.debug && enabled && !suspended;
    }

    public static void suspend() {
        suspended = true;
    }

    public static void resume() {
        suspended = false;
    }

    public static void Log(String s) {
        if (!RecipeMonitor.canLog()) {
            return;
        }
        RecipeMonitor.Log(s, defColor);
    }

    public static void Log(String s, Color c) {
        if (RecipeMonitor.canLog()) {
            lines.add(tabStr + s);
            colors.add(c);
        }
    }

    public static void LogBlanc() {
        if (!RecipeMonitor.canLog()) {
            return;
        }
        RecipeMonitor.Log("");
    }

    public static <T> void LogList(String tag, ArrayList<T> sourceTypes) {
        if (!RecipeMonitor.canLog()) {
            return;
        }
        RecipeMonitor.Log(tag + " {");
        RecipeMonitor.IncTab();
        if (sourceTypes != null) {
            for (T sourceType : sourceTypes) {
                RecipeMonitor.Log(sourceType.toString());
            }
        }
        RecipeMonitor.DecTab();
        RecipeMonitor.Log("}");
    }

    public static void LogInit(Recipe recipe, IsoGameCharacter character, ArrayList<ItemContainer> containers, InventoryItem selectedItem, ArrayList<InventoryItem> ignoreItems, boolean allItems) {
        if (!RecipeMonitor.canLog()) {
            return;
        }
        RecipeMonitor.Log("[Recipe]", colHeader);
        RecipeMonitor.Log("Starting recipe: " + recipe.getOriginalname());
        RecipeMonitor.Log("All items = " + allItems);
        RecipeMonitor.Log("character = " + character.getFullName());
        RecipeMonitor.Log("selected item = " + String.valueOf(selectedItem));
        RecipeMonitor.LogContainers("containers", containers);
        RecipeMonitor.LogBlanc();
    }

    public static String getContainerString(ItemContainer container) {
        if (container == null) {
            return "ItemContainer:[null]";
        }
        if (container.getParent() != null) {
            if (container.getParent() instanceof IsoGameCharacter) {
                return "ItemContainer:[type:" + container.type + ", parent:PlayerInventory]";
            }
            if (container.getParent().getSprite() != null) {
                return "ItemContainer:[type:" + container.type + ", parent:PlayerInventory, sprite:" + container.getParent().getSprite().name + "]";
            }
        }
        return container.toString();
    }

    private static void LogContainers(String tag, ArrayList<ItemContainer> containers) {
        RecipeMonitor.LogContainers(tag, containers, false);
    }

    private static void LogContainers(String tag, ArrayList<ItemContainer> containers, boolean full) {
        if (!RecipeMonitor.canLog()) {
            return;
        }
        RecipeMonitor.Log(tag + " {");
        RecipeMonitor.IncTab();
        if (containers != null) {
            for (ItemContainer container : containers) {
                if (full) {
                    RecipeMonitor.Log(RecipeMonitor.getContainerString(container));
                    RecipeMonitor.IncTab();
                    for (InventoryItem item : container.getItems()) {
                        RecipeMonitor.Log("item > " + String.valueOf(item));
                    }
                    RecipeMonitor.DecTab();
                    continue;
                }
                RecipeMonitor.Log(RecipeMonitor.getContainerString(container));
            }
        } else {
            RecipeMonitor.Log("null");
        }
        RecipeMonitor.DecTab();
        RecipeMonitor.Log("}");
    }

    public static void LogSources(List<Recipe.Source> sources) {
        if (!RecipeMonitor.canLog()) {
            return;
        }
        RecipeMonitor.Log("[Sources]", colHeader);
        if (sources == null) {
            RecipeMonitor.Log("Sources null.", colNeg);
            return;
        }
        for (int i = 0; i < sources.size(); ++i) {
            RecipeMonitor.LogSource("[" + i + "] Source: ", sources.get(i));
        }
    }

    private static void LogSource(String tag, Recipe.Source source2) {
        if (!RecipeMonitor.canLog()) {
            return;
        }
        RecipeMonitor.Log(tag + " {");
        RecipeMonitor.IncTab();
        if (source2 != null) {
            RecipeMonitor.Log((source2.keep ? "(keep)" : "") + (source2.destroy ? "(destroy)" : "") + "(count=" + source2.count + ")(use=" + source2.use + "):");
            RecipeMonitor.IncTab();
            RecipeMonitor.Log("items=" + String.valueOf(source2.getItems()));
            RecipeMonitor.Log("orig=" + String.valueOf(source2.getOriginalItems()));
            RecipeMonitor.DecTab();
        }
        RecipeMonitor.DecTab();
        RecipeMonitor.Log("}");
    }

    public static void LogItem(String tag, InventoryItem item) {
        if (!RecipeMonitor.canLog()) {
            return;
        }
        RecipeMonitor.Log(tag + " = " + String.valueOf(item));
    }

    public static String getResultString(Recipe.Result result) {
        return "result = [" + result.getFullType() + ", count=" + result.getCount() + ", drain=" + result.getDrainableCount() + "]";
    }

    private static void setTabStr() {
        tabStr = tabs > 0 ? tabSize.repeat(tabs) : "";
    }

    public static void ResetTabs() {
        tabs = 0;
        RecipeMonitor.setTabStr();
    }

    public static void SetTab(int i) {
        if (!RecipeMonitor.canLog()) {
            return;
        }
        tabs = i;
        RecipeMonitor.setTabStr();
    }

    public static void IncTab() {
        if (!RecipeMonitor.canLog()) {
            return;
        }
        ++tabs;
        RecipeMonitor.setTabStr();
    }

    public static void DecTab() {
        if (!RecipeMonitor.canLog()) {
            return;
        }
        if (--tabs < 0) {
            tabs = 0;
        }
        RecipeMonitor.setTabStr();
    }

    public static ArrayList<String> GetLines() {
        return lines;
    }

    public static ArrayList<Color> GetColors() {
        return colors;
    }

    public static Color GetColorForLine(int i) {
        if (i >= 0 && i < colors.size()) {
            return colors.get(i);
        }
        return defColor;
    }

    public static String GetSaveDir() {
        return ZomboidFileSystem.instance.getCacheDir() + File.separator + "RecipeLogs" + File.separator;
    }

    public static void SaveToFile() {
        if (!lines.isEmpty()) {
            try {
                String stamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
                String saveFile = "log_" + stamp;
                String name = recipeName;
                if (name != null) {
                    name = name.toLowerCase();
                    name = name.replaceAll("\\s", "_");
                    name = name.replace("\\.", "");
                }
                if (StringUtils.isNullOrWhitespace(name)) {
                    name = "unkown";
                }
                saveFile = saveFile + "_" + name;
                String path = RecipeMonitor.GetSaveDir();
                File pathFile = new File(path);
                if (!pathFile.exists() && !pathFile.mkdirs()) {
                    DebugLog.log("Failed to create path = " + path);
                    return;
                }
                String fileName = path + saveFile + ".txt";
                DebugLog.log("Attempting to save recipe log to: " + fileName);
                File f = new File(fileName);
                try (BufferedWriter w = new BufferedWriter(new FileWriter(f, false));){
                    RecipeMonitor.w_write(w, "Recipe name = " + recipeName);
                    RecipeMonitor.w_write(w, "# Recipe at time of recording:");
                    RecipeMonitor.w_blanc(w);
                    for (String s : recipeLines) {
                        RecipeMonitor.w_write(w, s);
                    }
                    RecipeMonitor.w_blanc(w);
                    RecipeMonitor.w_write(w, "# Recipe monitor log:");
                    RecipeMonitor.w_blanc(w);
                    for (String s : lines) {
                        RecipeMonitor.w_write(w, s);
                    }
                }
                catch (Exception e) {
                    e.printStackTrace();
                }
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private static void w_blanc(BufferedWriter w) throws IOException {
        RecipeMonitor.w_write(w, null);
    }

    private static void w_write(BufferedWriter w, String line) throws IOException {
        if (line != null) {
            w.write(line);
        }
        w.newLine();
    }

    static {
        monitorID = -1;
        tabStr = "";
        defColor = Color.black;
        colGray = new Color(0.5f, 0.5f, 0.5f);
        colNeg = Colors.Maroon;
        colPos = Colors.DarkGreen;
        colHeader = Colors.SaddleBrown;
        lines = new ArrayList();
        colors = new ArrayList();
        recipeName = "none";
        recipeLines = new ArrayList();
    }
}

