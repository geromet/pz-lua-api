/*
 * Decompiled with CFR 0.152.
 */
package zombie.debug;

import java.util.ArrayList;
import java.util.Arrays;
import zombie.UsedFromLua;

@UsedFromLua
public enum LogSeverity {
    Trace("TRACE: "),
    Noise("NOISE: "),
    Debug("DEBUG: "),
    General("LOG  : "),
    Warning("WARN : "),
    Error("ERROR: "),
    Off("!OFF!");

    public static final LogSeverity All;
    public final String logPrefix;

    private LogSeverity(String logPrefix) {
        this.logPrefix = logPrefix;
    }

    public static ArrayList<LogSeverity> getValueList() {
        return new ArrayList<LogSeverity>(Arrays.asList(LogSeverity.values()));
    }

    static {
        All = Trace;
    }
}

