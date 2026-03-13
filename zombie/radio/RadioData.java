/*
 * Decompiled with CFR 0.152.
 */
package zombie.radio;

import java.io.File;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import zombie.UsedFromLua;
import zombie.ZomboidFileSystem;
import zombie.core.Core;
import zombie.core.Language;
import zombie.core.Translator;
import zombie.core.logger.ExceptionLogger;
import zombie.core.random.Rand;
import zombie.debug.DebugLog;
import zombie.debug.DebugType;
import zombie.gameStates.ChooseGameInfo;
import zombie.radio.ChannelCategory;
import zombie.radio.RadioTranslationData;
import zombie.radio.scripting.RadioBroadCast;
import zombie.radio.scripting.RadioChannel;
import zombie.radio.scripting.RadioLine;
import zombie.radio.scripting.RadioScript;

@UsedFromLua
public final class RadioData {
    private static final boolean PRINTDEBUG = false;
    private boolean isVanilla;
    private String guid;
    private int version;
    private final String xmlFilePath;
    private final ArrayList<RadioChannel> radioChannels = new ArrayList();
    private final ArrayList<RadioTranslationData> translationDataList = new ArrayList();
    private RadioTranslationData currentTranslation;
    private Node rootNode;
    private final Map<String, RadioScript> advertQue = new HashMap<String, RadioScript>();
    private static final String fieldStart = "\\$\\{t:";
    private static final String fieldEnd = "\\}";
    private static final String regex = "\\$\\{t:([^}]+)\\}";
    private static final Pattern pattern = Pattern.compile("\\$\\{t:([^}]+)\\}");

    public RadioData(String xmlFile) {
        this.xmlFilePath = xmlFile;
    }

    public ArrayList<RadioChannel> getRadioChannels() {
        return this.radioChannels;
    }

    public boolean isVanilla() {
        return this.isVanilla;
    }

    public static ArrayList<String> getTranslatorNames(Language language) {
        ArrayList<String> names = new ArrayList<String>();
        if (language != Translator.getDefaultLanguage()) {
            ArrayList<RadioData> radioDataList = RadioData.fetchRadioData(false);
            for (RadioData radioData : radioDataList) {
                for (RadioTranslationData translationData : radioData.translationDataList) {
                    if (translationData.getLanguageEnum() != language) continue;
                    for (String name : translationData.getTranslators()) {
                        if (names.contains(name)) continue;
                        names.add(name);
                    }
                }
            }
        }
        return names;
    }

    private static ArrayList<RadioData> fetchRadioData(boolean loadMods) {
        return RadioData.fetchRadioData(loadMods, DebugLog.isEnabled(DebugType.Radio));
    }

    private static ArrayList<RadioData> fetchRadioData(boolean loadMods, boolean printStuff) {
        ArrayList<RadioData> radioDataList = new ArrayList<RadioData>();
        try {
            String modDir;
            ChooseGameInfo.Mod mod;
            ArrayList<String> modRoots = ZomboidFileSystem.instance.getModIDs();
            if (printStuff) {
                System.out.println(":: Searching for radio data files:");
            }
            ArrayList<String> files = new ArrayList<String>();
            RadioData.searchForFiles(ZomboidFileSystem.instance.getMediaFile("radio"), "xml", files);
            ArrayList<String> vanilla = new ArrayList<String>(files);
            HashMap<String, String> modFilesMap = new HashMap<String, String>();
            ArrayList<String> modFiles = new ArrayList<String>();
            if (loadMods) {
                for (int n = 0; n < modRoots.size(); ++n) {
                    String rel;
                    mod = ChooseGameInfo.getAvailableModDetails(modRoots.get(n));
                    if (mod == null) continue;
                    modFilesMap.clear();
                    modFiles.clear();
                    modDir = mod.getCommonDir();
                    File modPathbase = new File(modDir.toLowerCase(Locale.ENGLISH));
                    URI modPathbaseURI = modPathbase.toURI();
                    if (modDir != null) {
                        RadioData.searchForFiles(new File(modDir + File.separator + "media" + File.separator + "radio"), "xml", modFiles);
                    }
                    for (String fileStr : modFiles) {
                        rel = ZomboidFileSystem.instance.getRelativeFile(modPathbaseURI, fileStr);
                        rel = rel.toLowerCase(Locale.ENGLISH);
                        modFilesMap.putIfAbsent(rel, fileStr);
                    }
                    modFiles.clear();
                    modDir = mod.getVersionDir();
                    modPathbase = new File(modDir.toLowerCase(Locale.ENGLISH));
                    modPathbaseURI = modPathbase.toURI();
                    if (modDir != null) {
                        RadioData.searchForFiles(new File(modDir + File.separator + "media" + File.separator + "radio"), "xml", modFiles);
                    }
                    for (String fileStr : modFiles) {
                        rel = ZomboidFileSystem.instance.getRelativeFile(modPathbaseURI, fileStr);
                        rel = rel.toLowerCase(Locale.ENGLISH);
                        modFilesMap.putIfAbsent(rel, fileStr);
                    }
                    files.addAll(modFilesMap.values());
                }
            }
            for (String file : files) {
                RadioData radioData = RadioData.ReadFile(file);
                if (radioData != null) {
                    if (printStuff) {
                        System.out.println(" Found file: " + file);
                    }
                    for (String vanfile : vanilla) {
                        if (!vanfile.equals(file)) continue;
                        radioData.isVanilla = true;
                    }
                    radioDataList.add(radioData);
                    continue;
                }
                System.out.println("[Failure] Cannot parse file: " + file);
            }
            if (printStuff) {
                System.out.println(":: Searching for translation files:");
            }
            files.clear();
            RadioData.searchForFiles(ZomboidFileSystem.instance.getMediaFile("radio"), "txt", files);
            if (loadMods) {
                for (int n = 0; n < modRoots.size(); ++n) {
                    mod = ChooseGameInfo.getAvailableModDetails(modRoots.get(n));
                    modDir = mod.getCommonDir();
                    if (modDir != null) {
                        RadioData.searchForFiles(new File(modDir + File.separator + "media" + File.separator + "radio"), "txt", files);
                    }
                    if ((modDir = mod.getVersionDir()) == null) continue;
                    RadioData.searchForFiles(new File(modDir + File.separator + "media" + File.separator + "radio"), "txt", files);
                }
            }
            for (String file : files) {
                RadioTranslationData translationData = RadioTranslationData.ReadFile(file);
                if (translationData != null) {
                    if (printStuff) {
                        System.out.println(" Found file: " + file);
                    }
                    for (RadioData radioData : radioDataList) {
                        if (!radioData.guid.equals(translationData.getGuid())) continue;
                        if (printStuff) {
                            System.out.println(" Adding translation: " + radioData.guid);
                        }
                        radioData.translationDataList.add(translationData);
                    }
                    continue;
                }
                if (!printStuff) continue;
                System.out.println("[Failure] " + file);
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        return radioDataList;
    }

    public static ArrayList<RadioData> fetchAllRadioData() {
        boolean bDebugEnabled = DebugLog.isEnabled(DebugType.Radio);
        ArrayList<RadioData> radioDataList = RadioData.fetchRadioData(true);
        for (int i = radioDataList.size() - 1; i >= 0; --i) {
            RadioData radioData = radioDataList.get(i);
            if (radioData.loadRadioScripts()) {
                if (bDebugEnabled) {
                    DebugLog.Radio.println(" Adding" + (radioData.isVanilla ? " (vanilla)" : "") + " file: " + radioData.xmlFilePath);
                    DebugLog.Radio.println(" - GUID: " + radioData.guid);
                }
                radioData.currentTranslation = null;
                radioData.translationDataList.clear();
                continue;
            }
            DebugLog.Radio.println("[Failure] Failed to load radio scripts for GUID: " + radioData.guid);
            DebugLog.Radio.println("          File: " + radioData.xmlFilePath);
            radioDataList.remove(i);
        }
        return radioDataList;
    }

    private static void searchForFiles(File path, String extension, ArrayList<String> files) {
        if (path.isDirectory()) {
            String[] internalNames = path.list();
            for (int i = 0; i < internalNames.length; ++i) {
                RadioData.searchForFiles(new File(path.getAbsolutePath() + File.separator + internalNames[i]), extension, files);
            }
        } else if (path.getAbsolutePath().toLowerCase().contains(extension)) {
            files.add(path.getAbsolutePath());
        }
    }

    private static RadioData ReadFile(String filePath) {
        RadioData radioData = new RadioData(filePath);
        boolean validRadioFile = false;
        try {
            if (DebugLog.isEnabled(DebugType.Radio)) {
                DebugLog.Radio.println("Reading xml: " + filePath);
            }
            File xmlFile = new File(filePath);
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(xmlFile);
            doc.getDocumentElement().normalize();
            NodeList radioDataNodes = doc.getElementsByTagName("RadioData");
            if (DebugLog.isEnabled(DebugType.Radio)) {
                DebugLog.Radio.println("RadioData nodes len: " + radioDataNodes.getLength());
            }
            if (radioDataNodes.getLength() > 0) {
                radioData.rootNode = radioDataNodes.item(0);
                validRadioFile = radioData.loadRootInfo();
                if (DebugLog.isEnabled(DebugType.Radio)) {
                    DebugLog.Radio.println("valid file: " + validRadioFile);
                }
            }
        }
        catch (Exception e) {
            ExceptionLogger.logException(e);
        }
        if (validRadioFile) {
            return radioData;
        }
        return null;
    }

    private void print(String line) {
    }

    private ArrayList<Node> getChildNodes(Node parent) {
        ArrayList<Node> children = new ArrayList<Node>();
        if (parent.hasChildNodes()) {
            Node child = parent.getFirstChild();
            while (child != null) {
                if (!(child instanceof Element)) {
                    child = child.getNextSibling();
                    continue;
                }
                children.add(child);
                child = child.getNextSibling();
            }
        }
        return children;
    }

    private String toLowerLocaleSafe(String str) {
        return str.toLowerCase(Locale.ENGLISH);
    }

    private boolean nodeNameIs(Node node, String name) {
        return node.getNodeName().equals(name);
    }

    private String getAttrib(Node node, String name, boolean trim) {
        return this.getAttrib(node, name, trim, false);
    }

    private String getAttrib(Node node, String name) {
        return this.getAttrib(node, name, true, false).trim();
    }

    private String getAttrib(Node node, String name, boolean trim, boolean tolower) {
        String s = node.getAttributes().getNamedItem(name).getTextContent();
        if (trim) {
            s = s.trim();
        }
        if (tolower) {
            s = this.toLowerLocaleSafe(s);
        }
        return s;
    }

    private boolean loadRootInfo() {
        boolean bDebugEnabled = DebugLog.isEnabled(DebugType.Radio);
        if (bDebugEnabled) {
            DebugLog.Radio.println("Reading RootInfo...");
        }
        for (Node n : this.getChildNodes(this.rootNode)) {
            if (!this.nodeNameIs(n, "RootInfo")) continue;
            if (bDebugEnabled) {
                DebugLog.Radio.println("RootInfo found");
            }
            for (Node n2 : this.getChildNodes(n)) {
                String name = n2.getNodeName();
                String val = n2.getTextContent();
                if (name == null || val == null) continue;
                name = name.trim();
                if (bDebugEnabled) {
                    DebugLog.Radio.println("Found element: " + name);
                }
                if (name.equals("Version")) {
                    if (bDebugEnabled) {
                        DebugLog.Radio.println("Version = " + this.version);
                    }
                    this.version = Integer.parseInt(val);
                    continue;
                }
                if (!name.equals("FileGUID")) continue;
                if (bDebugEnabled) {
                    DebugLog.Radio.println("GUID = " + val);
                }
                this.guid = val;
            }
        }
        return this.guid != null && this.version >= 0;
    }

    private boolean loadRadioScripts() {
        boolean success = false;
        this.currentTranslation = null;
        this.advertQue.clear();
        if (Core.getInstance().getContentTranslationsEnabled() && Translator.getLanguage() != Translator.getDefaultLanguage()) {
            System.out.println("Attempting to load translation: " + Translator.getLanguage().toString());
            for (RadioTranslationData translationData : this.translationDataList) {
                if (translationData.getLanguageEnum() != Translator.getLanguage()) continue;
                System.out.println("Translation found!");
                if (translationData.loadTranslations()) {
                    this.currentTranslation = translationData;
                    System.out.println("Count = " + this.currentTranslation.getTranslationCount());
                    continue;
                }
                System.out.println("Error loading translations for " + this.guid);
            }
        } else if (!Core.getInstance().getContentTranslationsEnabled()) {
            System.out.println("NOTE: Community Content Translations are disabled.");
        }
        for (Node n : this.getChildNodes(this.rootNode)) {
            if (!this.nodeNameIs(n, "Adverts")) continue;
            this.loadAdverts(n);
        }
        for (Node n : this.getChildNodes(this.rootNode)) {
            if (!this.nodeNameIs(n, "Channels")) continue;
            this.loadChannels(n);
            success = true;
        }
        this.rootNode = null;
        return success;
    }

    private void loadAdverts(Node parent) {
        this.print(">>> Loading adverts...");
        ArrayList<RadioScript> scripts = new ArrayList<RadioScript>();
        scripts = this.loadScripts(parent, scripts, true);
        for (RadioScript script : scripts) {
            if (this.advertQue.containsKey(script.GetName())) continue;
            this.advertQue.put(script.GetGUID(), script);
        }
    }

    private void loadChannels(Node parent) {
        this.print(">>> Loading channels...");
        ArrayList<RadioScript> scripts = new ArrayList<RadioScript>();
        for (Node n : this.getChildNodes(parent)) {
            if (!this.nodeNameIs(n, "ChannelEntry")) continue;
            String id = this.getAttrib(n, "ID");
            String name = this.getAttrib(n, "name");
            String cat = this.getAttrib(n, "cat");
            String freq = this.getAttrib(n, "freq");
            String startscript = this.getAttrib(n, "startscript");
            this.print(" -> Found channel: " + name + ", on freq: " + freq + " , category: " + cat + ", startscript: " + startscript + ", ID: " + id);
            RadioChannel chan = new RadioChannel(name, Integer.parseInt(freq), ChannelCategory.valueOf(cat), id);
            scripts.clear();
            scripts = this.loadScripts(n, scripts, false);
            for (RadioScript script : scripts) {
                chan.AddRadioScript(script);
            }
            chan.setActiveScript(startscript, 0);
            this.radioChannels.add(chan);
            chan.setRadioData(this);
        }
    }

    private ArrayList<RadioScript> loadScripts(Node parent, ArrayList<RadioScript> scripts, boolean advertScript) {
        this.print(" --> Loading scripts...");
        for (Node n : this.getChildNodes(parent)) {
            if (!this.nodeNameIs(n, "ScriptEntry")) continue;
            String id = this.getAttrib(n, "ID");
            String name = this.getAttrib(n, "name");
            String loopmin = this.getAttrib(n, "loopmin");
            String loopmax = this.getAttrib(n, "loopmax");
            this.print(" ---> Found script: " + name);
            RadioScript script = new RadioScript(name, Integer.parseInt(loopmin), Integer.parseInt(loopmax), id);
            for (Node nn : this.getChildNodes(n)) {
                if (this.nodeNameIs(nn, "BroadcastEntry")) {
                    this.loadBroadcast(nn, script);
                    continue;
                }
                if (advertScript || !this.nodeNameIs(nn, "ExitOptions")) continue;
                this.loadExitOptions(nn, script);
            }
            scripts.add(script);
        }
        return scripts;
    }

    private RadioBroadCast loadBroadcast(Node broadcast, RadioScript script) {
        RadioScript adverts;
        String id = this.getAttrib(broadcast, "ID");
        String startstamp = this.getAttrib(broadcast, "timestamp");
        String endstamp = this.getAttrib(broadcast, "endstamp");
        this.print(" ----> BroadCast, Timestamp: " + startstamp + ", endstamp: " + endstamp);
        int startStampInt = Integer.parseInt(startstamp);
        int endStampInt = Integer.parseInt(endstamp);
        String segment = this.getAttrib(broadcast, "isSegment");
        boolean isSegment = this.toLowerLocaleSafe(segment).equals("true");
        String advertCat = this.getAttrib(broadcast, "advertCat");
        RadioBroadCast broadCast = new RadioBroadCast(id, startStampInt, endStampInt);
        if (!isSegment && !this.toLowerLocaleSafe(advertCat).equals("none") && this.advertQue.containsKey(advertCat) && Rand.Next(101) < 75 && !(adverts = this.advertQue.get(advertCat)).getBroadcastList().isEmpty()) {
            if (Rand.Next(101) < 50) {
                broadCast.setPreSegment(adverts.getBroadcastList().get(Rand.Next(adverts.getBroadcastList().size())));
            } else {
                broadCast.setPostSegment(adverts.getBroadcastList().get(Rand.Next(adverts.getBroadcastList().size())));
            }
        }
        for (Node l : this.getChildNodes(broadcast)) {
            if (!this.nodeNameIs(l, "LineEntry")) continue;
            String lineId = this.getAttrib(l, "ID");
            String r = this.getAttrib(l, "r");
            String g = this.getAttrib(l, "g");
            String b = this.getAttrib(l, "b");
            String codes = null;
            if (l.getAttributes().getNamedItem("codes") != null) {
                codes = this.getAttrib(l, "codes");
            }
            this.print(" -----> New Line, Color: " + r + ", " + g + ", " + b);
            String text = Translator.getText("RD_" + lineId);
            RadioLine radioLine = new RadioLine(text, Float.parseFloat(r) / 255.0f, Float.parseFloat(g) / 255.0f, Float.parseFloat(b) / 255.0f, codes);
            broadCast.AddRadioLine(radioLine);
            if (!(text = text.trim()).toLowerCase().startsWith("${t:")) continue;
            text = this.checkForCustomAirTimer(text, radioLine);
            radioLine.setText(text);
        }
        if (script != null) {
            script.AddBroadcast(broadCast, isSegment);
        }
        return broadCast;
    }

    private String checkForTranslation(String id, String text) {
        if (this.currentTranslation != null) {
            String trans = this.currentTranslation.getTranslation(id);
            if (trans != null) {
                return trans;
            }
            DebugLog.log(DebugType.Radio, "no translation for: " + id);
        }
        return text;
    }

    private void loadExitOptions(Node exitOptions, RadioScript script) {
        for (Node exitOption : this.getChildNodes(exitOptions)) {
            if (!this.nodeNameIs(exitOption, "ExitOption")) continue;
            String scriptName = this.getAttrib(exitOption, "script");
            String chance = this.getAttrib(exitOption, "chance");
            String delay = this.getAttrib(exitOption, "delay");
            int chanceInt = Integer.parseInt(chance);
            int delayInt = Integer.parseInt(delay);
            script.AddExitOption(scriptName, chanceInt, delayInt);
        }
    }

    private String checkForCustomAirTimer(String line, RadioLine radioLine) {
        Matcher m = pattern.matcher(line);
        String result = line;
        float val = -1.0f;
        if (m.find()) {
            try {
                val = Float.parseFloat(m.group(1).toLowerCase().trim());
                radioLine.setAirTime(val);
            }
            catch (Exception e) {
                e.printStackTrace();
            }
            result = result.replaceFirst(regex, "");
        }
        if (val >= 0.0f) {
            return "[cdt=" + val + "]" + result.trim();
        }
        return result.trim();
    }
}

