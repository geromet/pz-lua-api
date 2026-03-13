/*
 * Decompiled with CFR 0.152.
 */
package zombie.debug;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.file.Path;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;
import pl.mjaron.tinyloki.ILogStream;
import pl.mjaron.tinyloki.Labels;
import pl.mjaron.tinyloki.StreamSet;
import pl.mjaron.tinyloki.TinyLoki;
import zombie.DebugFileWatcher;
import zombie.GameTime;
import zombie.PredicatedFileWatcher;
import zombie.UsedFromLua;
import zombie.ZomboidFileSystem;
import zombie.config.ConfigFile;
import zombie.config.ConfigOption;
import zombie.config.StringConfigOption;
import zombie.core.Core;
import zombie.core.logger.LoggerManager;
import zombie.core.logger.ZLogger;
import zombie.debug.DebugLogStream;
import zombie.debug.DebugOptions;
import zombie.debug.DebugType;
import zombie.debug.GeneralErrorDebugLogFormatter;
import zombie.debug.GenericDebugLogFormatter;
import zombie.debug.LogSeverity;
import zombie.iso.IsoWorld;
import zombie.network.GameClient;
import zombie.network.GameServer;
import zombie.network.statistics.StatisticManager;
import zombie.scripting.ScriptManager;
import zombie.scripting.ScriptType;
import zombie.ui.UIDebugConsole;
import zombie.util.StringUtils;

@UsedFromLua
public final class DebugLog {
    private static boolean initialized;
    public static boolean printServerTime;
    private static final OutputStreamWrapper s_stdout;
    private static final OutputStreamWrapper s_stderr;
    private static final PrintStream s_originalOut;
    private static final PrintStream s_originalErr;
    private static final PrintStream GeneralErr;
    private static ZLogger logFileLogger;
    private static PredicatedFileWatcher debugCfgFileWatcher;
    private static String debugCfgFileWatcherPath;
    private static boolean lokiInit;
    private static TinyLoki loki;
    private static StreamSet logSet;
    private static ILogStream errorStream;
    public static final DebugType Entity;
    public static final DebugType General;
    public static final DebugType DetailedInfo;
    public static final DebugType Lua;
    public static final DebugType MapLoading;
    public static final DebugType Mod;
    public static final DebugType Multiplayer;
    public static final DebugType Network;
    public static final DebugType NetworkFileDebug;
    public static final DebugType Objects;
    public static final DebugType Radio;
    public static final DebugType Recipe;
    public static final DebugType Script;
    public static final DebugType Shader;
    public static final DebugType Sound;
    public static final DebugType Vehicle;
    public static final DebugType Voice;
    public static final DebugType Zombie;
    public static final DebugType Animal;
    public static final DebugType CraftLogic;
    public static final DebugType Action;
    public static final DebugType Grapple;
    private static boolean logTraceFileLocationEnabled;
    private static PrintStream recordingOut;
    public static final int VERSION1 = 1;
    public static final int VERSION2 = 2;
    public static final int VERSION = 4;

    public static void setDefaultLogSeverity() {
        LogSeverity logSeverity = DebugLog.getDefaultLogSeverity();
        for (DebugType debugType : DebugType.values()) {
            if (logSeverity.ordinal() >= debugType.getLogStream().getLogSeverity().ordinal()) continue;
            DebugLog.enableLog(debugType, logSeverity);
        }
    }

    private static LogSeverity getDefaultLogSeverity() {
        if (Core.debug) {
            return LogSeverity.General;
        }
        if (GameServer.server) {
            return LogSeverity.Warning;
        }
        return LogSeverity.Off;
    }

    public static void printLogLevels() {
        if (!GameServer.server) {
            DetailedInfo.trace("You can setup the log levels in the " + DebugLog.getConfigFileName() + " file");
        }
        DebugType.General.println("Logs configuration:");
        LogSeverity defaultLogSeverity = DebugLog.getDefaultLogSeverity();
        for (DebugType type : DebugType.values()) {
            DebugLogStream logStream = type.getLogStream();
            if (logStream.getLogSeverity() == defaultLogSeverity) continue;
            DebugType.General.println("%12s: %s", type.name(), logStream.getLogSeverity().name());
        }
        DebugType.General.println("%12s: %s", new Object[]{"Default", defaultLogSeverity});
    }

    public static void enableLog(DebugType type, LogSeverity severity) {
        DebugLog.setLogSeverity(type, severity);
    }

    public static LogSeverity getLogLevel(DebugType type) {
        return DebugLog.getLogSeverity(type);
    }

    public static LogSeverity getLogSeverity(DebugType type) {
        return type.getLogStream().getLogSeverity();
    }

    public static void setLogSeverity(DebugType type, LogSeverity logSeverity) {
        type.getLogStream().setLogSeverity(logSeverity);
    }

    public static boolean isEnabled(DebugType type) {
        return type.isEnabled();
    }

    public static boolean isLogEnabled(DebugType type, LogSeverity logSeverity) {
        return type.getLogStream().isLogEnabled(logSeverity);
    }

    public static String formatString(DebugType type, LogSeverity logSeverity, Object affix, boolean allowRepeat, String formatNoParams) {
        if (DebugLog.isLogEnabled(type, logSeverity)) {
            return DebugLog.formatStringVarArgs(type, logSeverity, affix, allowRepeat, "%s", formatNoParams);
        }
        return null;
    }

    public static String formatString(DebugType type, LogSeverity logSeverity, Object affix, boolean allowRepeat, String format, Object ... params) {
        if (DebugLog.isLogEnabled(type, logSeverity)) {
            return DebugLog.formatStringVarArgs(type, logSeverity, affix, allowRepeat, format, params);
        }
        return null;
    }

    public static String formatStringVarArgs(DebugType type, LogSeverity logSeverity, Object affix, boolean allowRepeat, String format, Object ... params) {
        if (!DebugLog.isLogEnabled(type, logSeverity)) {
            return null;
        }
        String ms = DebugLog.generateCurrentTimeMillisStr();
        int frameNo = IsoWorld.instance.getFrameNo();
        String typeStr = StringUtils.leftJustify(type.toString(), 12);
        String formattedOutputStr = String.format(format, params);
        String affixedOutputStr = String.valueOf(affix) + formattedOutputStr;
        if (!RepeatWatcher.check(type, logSeverity, affixedOutputStr, allowRepeat)) {
            return null;
        }
        return logSeverity.logPrefix + typeStr + " f:" + frameNo + ", t:" + ms + "> " + affixedOutputStr;
    }

    private static String generateCurrentTimeMillisStr() {
        Object ms = String.valueOf(System.currentTimeMillis());
        if (GameServer.server || GameClient.client || printServerTime) {
            ms = (String)ms + ", st:" + NumberFormat.getNumberInstance().format(TimeUnit.NANOSECONDS.toMillis(GameTime.getServerTime()));
        }
        return ms;
    }

    public static void echoToLogFiles(LogSeverity logSeverity, String outString) {
        DebugLog.echoToLogFile(outString);
        DebugLog.echoToLoki(logSeverity, outString);
        DebugLog.echoToRecording(outString);
    }

    public static void echoExceptionLineToLogFiles(LogSeverity logSeverity, String messageType, String outString) {
        DebugLog.echoToLogFile(outString);
        DebugLog.echoExceptionLineToLoki(logSeverity, messageType, outString);
        DebugLog.echoToRecording(outString);
    }

    private static void echoToLoki(LogSeverity logSeverity, String formattedString) {
        if (logSet == null) {
            return;
        }
        switch (logSeverity) {
            case Trace: 
            case Noise: {
                logSet.verbose(formattedString);
                break;
            }
            case Debug: {
                logSet.debug(formattedString);
                break;
            }
            case General: {
                logSet.info(formattedString);
                break;
            }
            case Warning: {
                logSet.warning(formattedString);
                break;
            }
            case Error: {
                if (errorStream == null) {
                    errorStream = loki.stream().l("level", "error").open();
                }
                errorStream.log(formattedString);
                break;
            }
            default: {
                logSet.unknown(formattedString);
            }
        }
    }

    private static void echoExceptionLineToLoki(LogSeverity logSeverity, String messageType, String message) {
        if (logSet == null) {
            return;
        }
        switch (logSeverity) {
            case Trace: 
            case Noise: {
                logSet.verbose(message, Labels.of("type", messageType));
                break;
            }
            case Debug: {
                logSet.debug(message, Labels.of("type", messageType));
                break;
            }
            case General: {
                logSet.info(message, Labels.of("type", messageType));
                break;
            }
            case Warning: {
                logSet.warning(message, Labels.of("type", messageType));
                break;
            }
            case Error: {
                logSet.fatal().log(message, Labels.of("type", messageType));
                break;
            }
            default: {
                logSet.unknown(message, Labels.of("type", messageType));
            }
        }
    }

    private static void echoToLogFile(String formattedLine) {
        if (logFileLogger == null) {
            if (initialized) {
                return;
            }
            logFileLogger = new ZLogger(GameServer.server ? "DebugLog-server" : "DebugLog", false);
        }
        try {
            logFileLogger.writeUnsafe(formattedLine, null, false);
        }
        catch (Exception e) {
            s_originalErr.println("Exception thrown writing to log file.");
            s_originalErr.println(e);
            e.printStackTrace(s_originalErr);
        }
    }

    private static void echoToRecording(String formattedString) {
        if (recordingOut == null) {
            return;
        }
        int frameNo = IsoWorld.instance.getFrameNo();
        recordingOut.print(frameNo);
        recordingOut.print(",");
        recordingOut.print('\"');
        recordingOut.print(formattedString);
        recordingOut.println('\"');
    }

    public static void log(DebugType type, String str) {
        type.println(str);
    }

    public static void setLogEnabled(DebugType type, boolean bEnabled) {
        DebugLogStream logStream = type.getLogStream();
        if (logStream.isEnabled() != bEnabled) {
            logStream.setLogSeverity(bEnabled ? DebugLog.getDefaultLogSeverity() : LogSeverity.Off);
        }
    }

    public static void log(String str) {
        DebugLog.log(DebugType.General, str);
    }

    public static ArrayList<DebugType> getDebugTypes() {
        ArrayList<DebugType> debugTypes = new ArrayList<DebugType>(Arrays.asList(DebugType.values()));
        debugTypes.sort((a, b) -> String.CASE_INSENSITIVE_ORDER.compare(a.name(), b.name()));
        return debugTypes;
    }

    public static void save() {
        LogSeverity[] logSeverityValues = LogSeverity.values();
        String[] logSeverityNames = new String[logSeverityValues.length];
        for (int i = 0; i < logSeverityValues.length; ++i) {
            logSeverityNames[i] = logSeverityValues[i].name();
        }
        ArrayList<StringConfigOption> options = new ArrayList<StringConfigOption>();
        for (DebugType debugType : DebugType.values()) {
            StringConfigOption option = new StringConfigOption(debugType.name(), LogSeverity.Off.name(), logSeverityNames);
            option.setValue(DebugLog.getLogSeverity(debugType).name());
            options.add(option);
        }
        String fileName = ZomboidFileSystem.instance.getCacheDir() + File.separator + "debuglog.ini";
        ConfigFile configFile = new ConfigFile();
        configFile.write(fileName, 4, options);
    }

    private static String getConfigFileName() {
        return ZomboidFileSystem.instance.getCacheDir() + File.separator + "debuglog.ini";
    }

    public static void load() {
        String fileName = DebugLog.getConfigFileName();
        ConfigFile configFile = new ConfigFile();
        File file = new File(fileName);
        if (!file.exists()) {
            DebugLog.setDefaultLogSeverity();
            DebugLog.save();
        }
        if (configFile.read(fileName)) {
            if (configFile.getVersion() != 4) {
                DebugLog.setDefaultLogSeverity();
                DebugLog.save();
            } else {
                for (int i = 0; i < configFile.getOptions().size(); ++i) {
                    ConfigOption configOption = configFile.getOptions().get(i);
                    try {
                        DebugType debugType = DebugType.valueOf(configOption.getName());
                        if (configFile.getVersion() == 1) {
                            DebugLog.setLogEnabled(debugType, StringUtils.tryParseBoolean(configOption.getValueAsString()));
                            continue;
                        }
                        LogSeverity logSeverity = LogSeverity.valueOf(configOption.getValueAsString());
                        DebugLog.setLogSeverity(debugType, logSeverity);
                        continue;
                    }
                    catch (Exception exception) {
                        // empty catch block
                    }
                }
            }
        }
    }

    public static boolean isLogTraceFileLocationEnabled() {
        return logTraceFileLocationEnabled;
    }

    public static PrintStream getRecordingOut() {
        return recordingOut;
    }

    public static void setRecordingOut(PrintStream recordingOut) {
        DebugLog.recordingOut = recordingOut;
    }

    public static DebugLogStream createLogStream(DebugType debugType) {
        if (debugType.getLogStream() != null) {
            return debugType.getLogStream();
        }
        return new DebugLogStream(s_originalOut, s_originalOut, s_originalErr, new GenericDebugLogFormatter(debugType));
    }

    public static void setStdOut(OutputStream out) {
        s_stdout.setStream(out);
    }

    public static void setStdErr(OutputStream out) {
        s_stderr.setStream(out);
    }

    public static void init() {
        if (initialized) {
            return;
        }
        initialized = true;
        DebugLog.setStdOut(System.out);
        DebugLog.setStdErr(System.err);
        System.setOut(General.getLogStream());
        System.setErr(GeneralErr);
        if (!GameServer.server) {
            DebugLog.load();
        }
        logFileLogger = LoggerManager.getLogger(GameServer.server ? "DebugLog-server" : "DebugLog");
        if (!lokiInit) {
            lokiInit = true;
            String lokiUrl = System.getProperty("lokiUrl");
            if (lokiUrl != null) {
                System.out.println("Loki logging enabled.");
                String lokiUser = System.getProperty("lokiUser");
                String lokiPass = System.getProperty("lokiPass");
                loki = TinyLoki.withUrl(lokiUrl).withThreadExecutor(2000).withBasicAuth(lokiUser, lokiPass).withLabels(Labels.of("instance", GameServer.server ? StatisticManager.getInstanceName() : GameClient.username).l("service_name", GameServer.server ? "pz.server" : "pz.client")).open();
                logSet = loki.streamSet().open();
            } else {
                loki = null;
                logSet = null;
            }
        }
        DebugType.General.getLogStream().setLogSeverity(LogSeverity.General);
        DebugType.Lua.getLogStream().setLogSeverity(LogSeverity.General);
        DebugType.Mod.getLogStream().setLogSeverity(LogSeverity.General);
        DebugType.Multiplayer.getLogStream().setLogSeverity(LogSeverity.General);
        DebugType.Network.getLogStream().setLogSeverity(LogSeverity.Error);
    }

    public static void loadDebugConfig(String filepath) {
        if (GameServer.server) {
            return;
        }
        try {
            File file;
            if (!(filepath != null || (file = new File((String)(filepath = ZomboidFileSystem.instance.getCacheDir() + File.separator + "debuglog.cfg"))).exists() && file.isFile())) {
                return;
            }
            DebugLog.log("Attempting to read debug config...");
            file = new File((String)filepath);
            if (!file.exists() || !file.isFile()) {
                DebugLog.log("Attempting relative path...");
                File p = new File("");
                Path path = Path.of(p.toURI()).getParent();
                file = new File(String.valueOf(path) + File.separator + (String)filepath);
            }
            DetailedInfo.trace("file = " + file.getAbsolutePath());
            if (!file.exists() || !file.isFile()) {
                DebugLog.log("Could not find debug config.");
                return;
            }
            String selectedConfig = null;
            HashMap configs = new HashMap();
            HashMap<String, String> aliases = new HashMap<String, String>();
            ArrayList<String> commands = null;
            boolean opened = false;
            try (BufferedReader br = new BufferedReader(new FileReader(file));){
                String l;
                String line = null;
                while ((l = br.readLine()) != null) {
                    String lastLine = line;
                    line = l.trim();
                    if (line.startsWith("//") || line.startsWith("#") || StringUtils.isNullOrWhitespace(line)) continue;
                    if (line.startsWith("=")) {
                        selectedConfig = line.substring(1).trim();
                        continue;
                    }
                    if (line.startsWith("$")) {
                        try {
                            String s = line.substring(1).trim();
                            int i = s.indexOf(61);
                            String alias = s.substring(0, i).trim();
                            String command = s.substring(i + 1).trim();
                            aliases.put(alias, command);
                        }
                        catch (Exception e) {
                            e.printStackTrace();
                        }
                        continue;
                    }
                    if (!opened && line.startsWith("{") && lastLine != null) {
                        opened = true;
                        commands = new ArrayList<String>();
                        configs.put(lastLine, commands);
                        continue;
                    }
                    if (!opened) continue;
                    if (line.startsWith("}")) {
                        opened = false;
                        continue;
                    }
                    commands.add(line);
                }
            }
            if (selectedConfig != null) {
                String[] ss;
                if (selectedConfig.startsWith("$")) {
                    DebugLog.log("Selected debug alias = '" + selectedConfig + "'");
                    selectedConfig = (String)aliases.get(selectedConfig.substring(1).trim());
                } else {
                    DebugLog.log("Selected debug profile = '" + selectedConfig + "'");
                }
                for (String elem : ss = selectedConfig.split("\\+")) {
                    String profile = elem.trim();
                    if (configs.containsKey(profile)) {
                        DebugLog.log("Debug.cfg loading profile '" + profile + "'");
                        for (String s : (ArrayList)configs.get(profile)) {
                            if (s.startsWith("+")) {
                                DebugLog.readConfigCommand(s.substring(1), true);
                                continue;
                            }
                            if (s.startsWith("-")) {
                                DebugLog.readConfigCommand(s.substring(1), false);
                                continue;
                            }
                            DebugLog.log("unknown command: '" + s + "'");
                        }
                        continue;
                    }
                    DebugLog.log("Debug.cfg profile note found: '" + profile + "'");
                }
            }
            DebugLog.startWatchingDebugCfgFile(file);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void startWatchingDebugCfgFile(File file) {
        if (debugCfgFileWatcher != null && debugCfgFileWatcherPath.equalsIgnoreCase(file.getPath())) {
            return;
        }
        if (debugCfgFileWatcher != null) {
            DebugLog.stopWatchingDebugCfgFile();
        }
        String cfgFileDir = file.getParent();
        DebugFileWatcher.instance.addDirectory(cfgFileDir);
        debugCfgFileWatcherPath = file.getPath();
        debugCfgFileWatcher = new PredicatedFileWatcher(debugCfgFileWatcherPath, DebugLog::isDebugCfgPath, DebugLog::onDebugCfgFileChanged);
        DebugFileWatcher.instance.add(debugCfgFileWatcher);
    }

    private static void stopWatchingDebugCfgFile() {
        DebugFileWatcher.instance.remove(debugCfgFileWatcher);
        debugCfgFileWatcher = null;
        debugCfgFileWatcherPath = null;
    }

    private static void onDebugCfgFileChanged(String path) {
        DebugLog.loadDebugConfig(debugCfgFileWatcherPath);
        DebugLog.printLogLevels();
    }

    private static boolean isDebugCfgPath(String path) {
        return StringUtils.equalsIgnoreCase(debugCfgFileWatcherPath, path);
    }

    private static void readConfigCommand(String s, boolean enable) {
        try {
            DebugType type;
            String logTypeStr = s;
            String logSeverityStr = null;
            if (StringUtils.containsWhitespace(s)) {
                String[] split = s.split("\\s+");
                logTypeStr = split[0].trim();
                logSeverityStr = split[1].trim();
            }
            LogSeverity logSeverity = LogSeverity.Debug;
            if (!StringUtils.isNullOrWhitespace(logSeverityStr)) {
                logSeverity = LogSeverity.valueOf(logSeverityStr);
            }
            if (logTypeStr.equalsIgnoreCase("LogTraceFileLocation")) {
                logTraceFileLocationEnabled = enable;
                return;
            }
            if (logTypeStr.equalsIgnoreCase("all")) {
                for (DebugType type2 : DebugType.values()) {
                    if (type2 == DebugType.General && !enable) continue;
                    DebugLog.setLogSeverity(type2, logSeverity);
                    DebugLog.setLogEnabled(type2, enable);
                }
                return;
            }
            if (logTypeStr.contains(".")) {
                String[] split = logTypeStr.split("\\.");
                type = DebugType.valueOf(split[0]);
                ScriptType scriptType = ScriptType.valueOf(split[1]);
                ScriptManager.EnableDebug(scriptType, enable);
            } else {
                type = DebugType.valueOf(logTypeStr);
            }
            DebugLog.setLogSeverity(type, logSeverity);
            DebugLog.setLogEnabled(type, enable);
        }
        catch (Exception e) {
            General.printException(e, "Exception thrown in readConfigCommand", LogSeverity.Error);
        }
    }

    public static void nativeLog(String logType, String logSeverity, String logTxt) {
        DebugType type = StringUtils.tryParseEnum(DebugType.class, logType, DebugType.General);
        LogSeverity severity = StringUtils.tryParseEnum(LogSeverity.class, logSeverity, LogSeverity.General);
        type.routedWrite(1, severity, logTxt);
    }

    static {
        s_stdout = new OutputStreamWrapper(System.out);
        s_stderr = new OutputStreamWrapper(System.err);
        s_originalOut = new PrintStream(s_stdout, true);
        s_originalErr = new PrintStream(s_stderr, true);
        GeneralErr = new DebugLogStream(s_originalErr, s_originalErr, s_originalErr, new GeneralErrorDebugLogFormatter(), LogSeverity.All);
        Entity = DebugType.Entity;
        General = DebugType.General;
        DetailedInfo = DebugType.DetailedInfo;
        Lua = DebugType.Lua;
        MapLoading = DebugType.MapLoading;
        Mod = DebugType.Mod;
        Multiplayer = DebugType.Multiplayer;
        Network = DebugType.Network;
        NetworkFileDebug = DebugType.NetworkFileDebug;
        Objects = DebugType.Objects;
        Radio = DebugType.Radio;
        Recipe = DebugType.Recipe;
        Script = DebugType.Script;
        Shader = DebugType.Shader;
        Sound = DebugType.Sound;
        Vehicle = DebugType.Vehicle;
        Voice = DebugType.Voice;
        Zombie = DebugType.Zombie;
        Animal = DebugType.Animal;
        CraftLogic = DebugType.CraftLogic;
        Action = DebugType.Action;
        Grapple = DebugType.Grapple;
    }

    private static final class RepeatWatcher {
        private static final Object Lock = "RepeatWatcher_Lock";
        private static String lastLine;
        private static DebugType lastDebugType;
        private static LogSeverity lastLogSeverity;

        private RepeatWatcher() {
        }

        /*
         * WARNING - Removed try catching itself - possible behaviour change.
         */
        public static boolean check(DebugType type, LogSeverity logSeverity, String newLine, boolean allowRepeat) {
            Object object = Lock;
            synchronized (object) {
                if (allowRepeat) {
                    lastLine = null;
                    lastDebugType = null;
                    lastLogSeverity = null;
                    return true;
                }
                if (lastLine == null) {
                    lastLine = newLine;
                    lastDebugType = type;
                    lastLogSeverity = logSeverity;
                    return true;
                }
                if (lastDebugType == type && lastLogSeverity == logSeverity && lastLine.equals(newLine)) {
                    return false;
                }
                lastLine = newLine;
                lastDebugType = type;
                lastLogSeverity = logSeverity;
                return true;
            }
        }
    }

    private static final class OutputStreamWrapper
    extends FilterOutputStream {
        public OutputStreamWrapper(OutputStream out) {
            super(out);
        }

        @Override
        public void write(byte[] b, int off, int len) throws IOException {
            this.out.write(b, off, len);
            if (Core.debug && UIDebugConsole.instance != null && DebugOptions.instance.uiDebugConsoleDebugLog.getValue()) {
                UIDebugConsole.instance.addOutput(b, off, len);
            }
        }

        public void setStream(OutputStream out) {
            this.out = out;
        }
    }
}

