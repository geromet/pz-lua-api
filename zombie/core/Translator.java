/*
 * Decompiled with CFR 0.152.
 */
package zombie.core;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import org.json.JSONObject;
import zombie.UsedFromLua;
import zombie.ZomboidFileSystem;
import zombie.characters.skills.PerkFactory;
import zombie.core.Core;
import zombie.core.Language;
import zombie.core.Languages;
import zombie.debug.DebugOptions;
import zombie.debug.DebugType;
import zombie.gameStates.ChooseGameInfo;
import zombie.scripting.ScriptManager;
import zombie.scripting.objects.Item;
import zombie.util.StringUtils;

@UsedFromLua
public final class Translator {
    private static List<Language> availableLanguage;
    public static boolean debug;
    private static FileWriter debugFile;
    private static boolean debugErrors;
    private static final Set<String> debugItemEvolvedRecipeName;
    private static final Set<String> debugItem;
    private static final Set<String> debugMultiStageBuild;
    private static final Set<String> debugRecipe;
    private static final Set<String> debugRecipeGroups;
    private static final Map<String, String> moodles;
    private static final Map<String, String> ui;
    private static final Map<String, String> survivalGuide;
    private static final Map<String, String> contextMenu;
    private static final Map<String, String> farming;
    private static final Map<String, String> recipe;
    private static final Map<String, String> recipeGroups;
    private static final Map<String, String> igui;
    private static final Map<String, String> sandbox;
    private static final Map<String, String> tooltip;
    private static final Map<String, String> challenge;
    private static final Set<String> missing;
    private static ArrayList<String> azertyLanguages;
    private static final Map<String, String> stash;
    private static final Map<String, String> moveables;
    private static final Map<String, String> makeup;
    private static final Map<String, String> gameSound;
    private static final Map<String, String> dynamicRadio;
    private static final Map<String, String> items;
    private static final Map<String, String> itemName;
    private static final Map<String, String> itemEvolvedRecipeName;
    private static final Map<String, String> recordedMedia;
    private static final Map<String, String> recordedMedia_EN;
    private static final Map<String, String> survivorNames;
    private static final Map<String, String> attributes;
    private static final Map<String, String> fluids;
    private static final Map<String, String> entity;
    private static final Map<String, String> mapLabel;
    private static final Map<String, String> printMedia;
    private static final Map<String, String> printText;
    private static final Map<String, String> radioData;
    private static final Map<String, String> bodyParts;
    private static final Map<String, String> brReplacements;
    private static final Map<String, String> tempMap;
    public static final Map<String, Map<String, String>> BY_NAME;
    public static Language language;

    public static void loadFiles() {
        language = null;
        availableLanguage = null;
        File file = new File(ZomboidFileSystem.instance.getCacheDir() + File.separator + "translationProblems.txt");
        if (debug) {
            try {
                if (debugFile != null) {
                    debugFile.close();
                }
                debugFile = new FileWriter(file);
            }
            catch (IOException e) {
                e.printStackTrace();
            }
        }
        moodles.clear();
        ui.clear();
        survivalGuide.clear();
        items.clear();
        itemName.clear();
        contextMenu.clear();
        farming.clear();
        recipe.clear();
        recipeGroups.clear();
        igui.clear();
        sandbox.clear();
        tooltip.clear();
        challenge.clear();
        missing.clear();
        stash.clear();
        moveables.clear();
        makeup.clear();
        gameSound.clear();
        dynamicRadio.clear();
        itemEvolvedRecipeName.clear();
        recordedMedia.clear();
        survivorNames.clear();
        attributes.clear();
        fluids.clear();
        printMedia.clear();
        printText.clear();
        radioData.clear();
        bodyParts.clear();
        mapLabel.clear();
        DebugType.Translation.println("translator: language is " + String.valueOf(Translator.getLanguage()));
        debugErrors = false;
        BY_NAME.forEach((name, map) -> Translator.forLanguageStack(l -> {
            Translator.tryFillMapFromFile(ZomboidFileSystem.instance.base.canonicalFile.getPath(), name, map, l);
            Translator.tryFillMapFromMods(name, map, l);
        }));
        Translator.tryFillMapFromFile(ZomboidFileSystem.instance.base.canonicalFile.getPath(), "Recorded_Media", recordedMedia_EN, Translator.getDefaultLanguage());
        Translator.tryFillMapFromMods("Recorded_Media", recordedMedia_EN, Translator.getDefaultLanguage());
        if (debug) {
            if (debugErrors) {
                DebugType.Translation.trace("translator: errors detected, please see " + file.getAbsolutePath());
            }
            debugItemEvolvedRecipeName.clear();
            debugItem.clear();
            debugMultiStageBuild.clear();
            debugRecipe.clear();
            debugRecipeGroups.clear();
        }
        PerkFactory.initTranslations();
    }

    public static void forLanguageStack(Consumer<Language> consumer) {
        LinkedHashSet<Language> bases = new LinkedHashSet<Language>();
        Language language = Translator.getLanguage();
        while (language != null && bases.add(language)) {
            language = Languages.instance.getByName(language.base());
        }
        bases.add(Translator.getDefaultLanguage());
        Language[] languages = bases.toArray(new Language[0]);
        for (int i = languages.length - 1; i >= 0; --i) {
            consumer.accept(languages[i]);
        }
    }

    private static void tryFillMapFromFile(String rootDir, String fileName, Map<String, String> map, Language language) {
        File file = new File("%s/media/lua/shared/Translate/%s/%s.json".formatted(rootDir, language, fileName));
        if (file.exists()) {
            try {
                String content = Files.readString(file.toPath());
                new JSONObject(content).toMap().forEach((k, v) -> map.put((String)k, ((String)v).replaceAll("%", "%%").replaceAll("%%(\\d+)", "%$1\\$s")));
            }
            catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private static void tryFillMapFromMods(String fileName, Map<String, String> map, Language language) {
        ArrayList<String> modIDs = ZomboidFileSystem.instance.getModIDs();
        for (int n = 0; n < modIDs.size(); ++n) {
            ChooseGameInfo.Mod mod = ChooseGameInfo.getAvailableModDetails(modIDs.get(n));
            if (mod == null) continue;
            String modDir = mod.getCommonDir();
            if (modDir != null) {
                Translator.tryFillMapFromFile(modDir, fileName, map, language);
            }
            if ((modDir = mod.getVersionDir()) == null) continue;
            Translator.tryFillMapFromFile(modDir, fileName, map, language);
        }
    }

    public static void readMapTranslation(ChooseGameInfo.Map map, String dir) {
        tempMap.clear();
        Translator.forLanguageStack(lang -> {
            Translator.tryFillMapFromFile(ZomboidFileSystem.instance.base.canonicalFile.getPath(), dir, tempMap, lang);
            Translator.tryFillMapFromMods(dir, tempMap, lang);
        });
        map.setTitle(tempMap.getOrDefault("title", map.getTitle()));
        map.setDescription(tempMap.getOrDefault("description", map.getDescription()));
    }

    public static void readModTranslation(ChooseGameInfo.Mod mod) {
        tempMap.clear();
        Translator.forLanguageStack(lang -> {
            String modDir = mod.getCommonDir();
            if (modDir != null) {
                Translator.tryFillMapFromFile(modDir, "Mod", tempMap, lang);
            }
            if ((modDir = mod.getVersionDir()) != null) {
                Translator.tryFillMapFromFile(modDir, "Mod", tempMap, lang);
            }
        });
        mod.setName(tempMap.getOrDefault("name", mod.getName()));
        mod.setDescription(tempMap.getOrDefault("description", mod.getDescription()));
    }

    public static String getText(String desc) {
        return Translator.getTextInternal(desc, false);
    }

    public static String getTextOrNull(String desc) {
        return Translator.getTextInternal(desc, true);
    }

    private static String getTextInternal(String desc, boolean nullOK) {
        String dbg;
        if (ui == null) {
            Translator.loadFiles();
        }
        String result = null;
        if (desc.startsWith("UI_")) {
            result = ui.get(desc);
        } else if (desc.startsWith("Moodles_")) {
            result = moodles.get(desc);
        } else if (desc.startsWith("SurvivalGuide_")) {
            result = survivalGuide.get(desc);
        } else if (desc.startsWith("Farming_")) {
            result = farming.get(desc);
        } else if (desc.startsWith("IGUI_")) {
            result = igui.get(desc);
        } else if (desc.startsWith("ContextMenu_")) {
            result = contextMenu.get(desc);
        } else if (desc.startsWith("GameSound_")) {
            result = gameSound.get(desc);
        } else if (desc.startsWith("Sandbox_")) {
            result = sandbox.get(desc);
        } else if (desc.startsWith("Tooltip_")) {
            result = tooltip.get(desc);
        } else if (desc.startsWith("Challenge_")) {
            result = challenge.get(desc);
        } else if (desc.startsWith("MakeUp")) {
            result = makeup.get(desc);
        } else if (desc.startsWith("Stash_")) {
            result = stash.get(desc);
        } else if (desc.startsWith("RM_")) {
            result = recordedMedia.get(desc);
        } else if (desc.startsWith("SurvivorName_")) {
            result = survivorNames.get(desc);
        } else if (desc.startsWith("SurvivorSurname_")) {
            result = survivorNames.get(desc);
        } else if (desc.startsWith("Attributes_")) {
            result = attributes.get(desc);
        } else if (desc.startsWith("Fluid_")) {
            result = fluids.get(desc);
        } else if (desc.startsWith("Print_Media_")) {
            result = printMedia.get(desc);
        } else if (desc.startsWith("Print_Text_")) {
            result = printText.get(desc);
        } else if (desc.startsWith("EC_")) {
            result = entity.get(desc);
        } else if (desc.startsWith("RD_")) {
            result = radioData.get(desc);
        } else if (desc.startsWith("BODYPART_")) {
            result = bodyParts.get(desc);
        } else if (desc.startsWith("MapLabel_")) {
            result = mapLabel.get(desc);
        }
        String string = dbg = Core.debug && DebugOptions.instance.translationPrefix.getValue() ? "*" : null;
        if (result == null) {
            if (nullOK) {
                return null;
            }
            if (!missing.contains(desc)) {
                if (Core.debug) {
                    DebugType.Translation.error("ERROR: Missing translation \"" + desc + "\"");
                }
                if (debug) {
                    Translator.debugwrite("ERROR: Missing translation \"" + desc + "\"\r\n");
                }
                missing.add(desc);
            }
            result = desc;
            String string2 = dbg = Core.debug && DebugOptions.instance.translationPrefix.getValue() ? "!" : null;
        }
        if (result.contains("<br>") || result.contains("<BR>")) {
            return brReplacements.computeIfAbsent(result, s -> s.replaceAll("<br>", "\n").replaceAll("<BR>", "\n"));
        }
        return dbg == null ? result : dbg + result;
    }

    public static String getText(String desc, Object ... args2) {
        return Translator.getText(desc).formatted(Translator.fixupArgs(args2));
    }

    public static String getTextOrNull(String desc, Object ... args2) {
        String text = Translator.getTextOrNull(desc);
        return text == null ? null : text.formatted(Translator.fixupArgs(args2));
    }

    private static Object[] fixupArgs(Object[] args2) {
        for (int i = 0; i < args2.length; ++i) {
            if (args2[i] == null) {
                args2[i] = "";
                continue;
            }
            Object object = args2[i];
            if (!(object instanceof Double)) continue;
            Double boxedDouble = (Double)object;
            double d = boxedDouble;
            args2[i] = d == (double)((long)d) ? Long.toString((long)d) : args2[i].toString();
        }
        return args2;
    }

    public static void setLanguage(Language newlanguage) {
        if (newlanguage == null) {
            newlanguage = Translator.getDefaultLanguage();
        }
        language = newlanguage;
    }

    public static void setLanguage(int languageId) {
        Translator.setLanguage(Languages.instance.getLanguages().get(languageId));
    }

    public static Language getLanguage() {
        String languageName;
        if (language == null && !StringUtils.isNullOrWhitespace(languageName = Core.getInstance().getOptionLanguageName())) {
            language = Languages.instance.getByName(languageName);
        }
        if (language == null) {
            language = Languages.instance.getByName(System.getProperty("user.language").toUpperCase());
        }
        if (language == null) {
            language = Translator.getDefaultLanguage();
        }
        return language;
    }

    public static List<Language> getAvailableLanguage() {
        if (availableLanguage == null) {
            availableLanguage = Languages.instance.getLanguages();
        }
        return availableLanguage;
    }

    public static String getDisplayItemName(String trim) {
        String result = items.get(trim.replaceAll(" ", "_").replaceAll("-", "_"));
        if (result == null) {
            return trim;
        }
        return result;
    }

    public static String getItemNameFromFullType(String fullType) {
        if (!fullType.contains(".")) {
            throw new IllegalArgumentException("fullType must contain \".\" i.e. module.type");
        }
        String name = itemName.get(fullType);
        if (name == null) {
            Item scriptItem;
            if (debug && Translator.getLanguage() != Translator.getDefaultLanguage() && !debugItem.contains(fullType)) {
                debugItem.add(fullType);
            }
            name = (scriptItem = ScriptManager.instance.getItem(fullType)) == null ? fullType : scriptItem.getDisplayName();
            itemName.put(fullType, name);
        }
        return name;
    }

    public static void setDefaultItemEvolvedRecipeName(String fullType, String english) {
        if (Translator.getLanguage() != Translator.getDefaultLanguage()) {
            return;
        }
        if (!fullType.contains(".")) {
            throw new IllegalArgumentException("fullType must contain \".\" i.e. module.type");
        }
        if (itemEvolvedRecipeName.containsKey(fullType)) {
            return;
        }
        itemEvolvedRecipeName.put(fullType, english);
    }

    public static String getItemEvolvedRecipeName(String fullType) {
        if (!fullType.contains(".")) {
            throw new IllegalArgumentException("fullType must contain \".\" i.e. module.type");
        }
        String name = itemEvolvedRecipeName.get(fullType);
        if (name == null) {
            Item scriptItem;
            if (debug && Translator.getLanguage() != Translator.getDefaultLanguage() && !debugItemEvolvedRecipeName.contains(fullType)) {
                debugItemEvolvedRecipeName.add(fullType);
            }
            name = (scriptItem = ScriptManager.instance.getItem(fullType)) == null ? fullType : scriptItem.getDisplayName();
            itemEvolvedRecipeName.put(fullType, name);
        }
        return name;
    }

    public static String getMoveableDisplayName(String name) {
        String replaced = name.replaceAll(" ", "_").replaceAll("-", "_").replaceAll("'", "").replaceAll("\\.", "");
        String result = moveables.get(replaced);
        if (result == null) {
            if (Core.debug && DebugOptions.instance.translationPrefix.getValue()) {
                return "!" + name;
            }
            return name;
        }
        if (Core.debug && DebugOptions.instance.translationPrefix.getValue()) {
            return "*" + result;
        }
        return result;
    }

    public static String getMoveableDisplayNameOrNull(String name) {
        String replaced = name.replaceAll(" ", "_").replaceAll("-", "_").replaceAll("'", "").replaceAll("\\.", "");
        String result = moveables.get(replaced);
        if (result == null) {
            return null;
        }
        if (Core.debug && DebugOptions.instance.translationPrefix.getValue()) {
            return "*" + result;
        }
        return result;
    }

    public static String getRecipeName(String name) {
        String result = recipe.get(name);
        if (result == null || result.isEmpty()) {
            if (debug && Translator.getLanguage() != Translator.getDefaultLanguage() && !debugRecipe.contains(name)) {
                debugRecipe.add(name);
            }
            return name;
        }
        return result;
    }

    public static String getRecipeGroupName(String name) {
        String result = recipeGroups.get(name);
        if (result == null || result.isEmpty()) {
            if (debug && Translator.getLanguage() != Translator.getDefaultLanguage()) {
                debugRecipeGroups.add(name);
            }
            return name;
        }
        return result;
    }

    public static Language getDefaultLanguage() {
        return Languages.instance.getDefaultLanguage();
    }

    public static void debugItemEvolvedRecipeNames() {
        if (!debug || debugItemEvolvedRecipeName.isEmpty()) {
            return;
        }
        Translator.debugwrite("EvolvedRecipeName_" + String.valueOf(Translator.getLanguage()) + ".txt\r\n");
        ArrayList<String> sorted2 = new ArrayList<String>(debugItemEvolvedRecipeName);
        Collections.sort(sorted2);
        for (String name : sorted2) {
            Translator.debugwrite("\tEvolvedRecipeName_" + name + " = \"" + itemEvolvedRecipeName.get(name) + "\",\r\n");
        }
        debugItemEvolvedRecipeName.clear();
    }

    public static void debugItemNames() {
        if (!debug || debugItem.isEmpty()) {
            return;
        }
        Translator.debugwrite("ItemName_" + String.valueOf(Translator.getLanguage()) + ".txt\r\n");
        ArrayList<String> sorted2 = new ArrayList<String>(debugItem);
        Collections.sort(sorted2);
        for (String name : sorted2) {
            Translator.debugwrite("\tItemName_" + name + " = \"" + itemName.get(name) + "\",\r\n");
        }
        debugItem.clear();
    }

    public static void debugMultiStageBuildNames() {
        if (!debug || debugMultiStageBuild.isEmpty()) {
            return;
        }
        Translator.debugwrite("MultiStageBuild_" + String.valueOf(Translator.getLanguage()) + ".txt\r\n");
        ArrayList<String> sorted2 = new ArrayList<String>(debugMultiStageBuild);
        Collections.sort(sorted2);
        for (String name : sorted2) {
            Translator.debugwrite("\tMultiStageBuild_" + name + " = \"\",\r\n");
        }
        debugMultiStageBuild.clear();
    }

    public static void debugRecipeNames() {
        if (!debug || debugRecipe.isEmpty()) {
            return;
        }
        Translator.debugwrite("Recipes_" + String.valueOf(Translator.getLanguage()) + ".txt\r\n");
        ArrayList<String> sorted2 = new ArrayList<String>(debugRecipe);
        Collections.sort(sorted2);
        for (String name : sorted2) {
            Translator.debugwrite("\tRecipe_" + name.replace(" ", "_") + " = \"\",\r\n");
        }
        debugRecipe.clear();
    }

    public static void debugRecipeGroupNames() {
        if (!debug || debugRecipeGroups.isEmpty()) {
            return;
        }
        Translator.debugwrite("RecipeGroups_" + String.valueOf(Translator.getLanguage()) + ".txt\r\n");
        ArrayList<String> sorted2 = new ArrayList<String>(debugRecipeGroups);
        Collections.sort(sorted2);
        for (String name : sorted2) {
            Translator.debugwrite("\tRecipeGroup_" + name.replace(" ", "_") + " = \"\",\r\n");
        }
        debugRecipeGroups.clear();
    }

    private static void debugwrite(String s) {
        if (debugFile != null) {
            try {
                debugFile.write(s);
                debugFile.flush();
            }
            catch (IOException iOException) {
                // empty catch block
            }
        }
    }

    public static ArrayList<String> getAzertyMap() {
        if (azertyLanguages == null) {
            azertyLanguages = new ArrayList();
            azertyLanguages.add("FR");
        }
        return azertyLanguages;
    }

    public static String getRadioText(String s) {
        String result = dynamicRadio.get(s);
        if (result == null) {
            return s;
        }
        return result;
    }

    public static String getTextMediaEN(String desc) {
        String dbg;
        if (ui == null) {
            Translator.loadFiles();
        }
        String result = null;
        if (desc.startsWith("RM_")) {
            result = recordedMedia_EN.get(desc);
        }
        String string = dbg = Core.debug && DebugOptions.instance.translationPrefix.getValue() ? "*" : null;
        if (result == null) {
            if (!missing.contains(desc) && Core.debug) {
                if (Core.debug) {
                    DebugType.Translation.error("ERROR: Missing translation \"" + desc + "\"");
                }
                if (debug) {
                    Translator.debugwrite("ERROR: Missing translation \"" + desc + "\"\r\n");
                }
                missing.add(desc);
            }
            result = desc;
            String string2 = dbg = Core.debug && DebugOptions.instance.translationPrefix.getValue() ? "!" : null;
        }
        if (result.contains("<br>")) {
            return result.replaceAll("<br>", "\n");
        }
        return dbg == null ? result : dbg + result;
    }

    public static String getAttributeText(String s) {
        return Translator.getAttributeText(s, false);
    }

    public static String getAttributeTextOrNull(String s) {
        return Translator.getAttributeText(s, true);
    }

    private static String getAttributeText(String s, boolean nullOnFail) {
        String result = attributes.get(s);
        if (result == null) {
            if (!missing.contains(s)) {
                DebugType.Translation.error("ERROR: Missing translation \"" + s + "\"");
                if (debug) {
                    Translator.debugwrite("ERROR: Missing translation \"" + s + "\"\r\n");
                }
                missing.add(s);
            }
            return nullOnFail ? null : s;
        }
        return result;
    }

    public static String getFluidText(String s) {
        String result = fluids.get(s);
        if (result == null) {
            return s;
        }
        return result;
    }

    public static String getEntityText(String s) {
        String result = entity.get(s);
        if (result == null) {
            return s;
        }
        return result;
    }

    public static String getMapLabelText(String s) {
        String result = mapLabel.get(s);
        if (result == null) {
            return s;
        }
        return result;
    }

    public static Map<String, String> getUI() {
        return ui;
    }

    static {
        debugItemEvolvedRecipeName = new HashSet<String>();
        debugItem = new HashSet<String>();
        debugMultiStageBuild = new HashSet<String>();
        debugRecipe = new HashSet<String>();
        debugRecipeGroups = new HashSet<String>();
        moodles = new HashMap<String, String>();
        ui = new HashMap<String, String>();
        survivalGuide = new HashMap<String, String>();
        contextMenu = new HashMap<String, String>();
        farming = new HashMap<String, String>();
        recipe = new LinkedHashMap<String, String>();
        recipeGroups = new HashMap<String, String>();
        igui = new HashMap<String, String>();
        sandbox = new HashMap<String, String>();
        tooltip = new HashMap<String, String>();
        challenge = new HashMap<String, String>();
        missing = new HashSet<String>();
        stash = new HashMap<String, String>();
        moveables = new HashMap<String, String>();
        makeup = new HashMap<String, String>();
        gameSound = new HashMap<String, String>();
        dynamicRadio = new HashMap<String, String>();
        items = new HashMap<String, String>();
        itemName = new HashMap<String, String>();
        itemEvolvedRecipeName = new HashMap<String, String>();
        recordedMedia = new HashMap<String, String>();
        recordedMedia_EN = new HashMap<String, String>();
        survivorNames = new HashMap<String, String>();
        attributes = new HashMap<String, String>();
        fluids = new HashMap<String, String>();
        entity = new HashMap<String, String>();
        mapLabel = new HashMap<String, String>();
        printMedia = new HashMap<String, String>();
        printText = new HashMap<String, String>();
        radioData = new HashMap<String, String>();
        bodyParts = new HashMap<String, String>();
        brReplacements = new HashMap<String, String>();
        tempMap = new HashMap<String, String>();
        BY_NAME = new LinkedHashMap<String, Map<String, String>>(){
            {
                this.put("Tooltip", tooltip);
                this.put("IG_UI", igui);
                this.put("Recipes", recipe);
                this.put("RecipeGroups", recipeGroups);
                this.put("Farming", farming);
                this.put("ContextMenu", contextMenu);
                this.put("SurvivalGuide", survivalGuide);
                this.put("UI", ui);
                this.put("Items", items);
                this.put("ItemName", itemName);
                this.put("Moodles", moodles);
                this.put("Sandbox", sandbox);
                this.put("Challenge", challenge);
                this.put("Stash", stash);
                this.put("Moveables", moveables);
                this.put("MakeUp", makeup);
                this.put("GameSound", gameSound);
                this.put("DynamicRadio", dynamicRadio);
                this.put("EvolvedRecipeName", itemEvolvedRecipeName);
                this.put("Recorded_Media", recordedMedia);
                this.put("SurvivorNames", survivorNames);
                this.put("Attributes", attributes);
                this.put("Fluids", fluids);
                this.put("Print_Media", printMedia);
                this.put("Print_Text", printText);
                this.put("Entity", entity);
                this.put("RadioData", radioData);
                this.put("BodyParts", bodyParts);
                this.put("MapLabel", mapLabel);
            }
        };
    }
}

