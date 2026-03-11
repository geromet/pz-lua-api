/*
 * Decompiled with CFR 0.152.
 */
package zombie.debug;

import java.io.PrintStream;
import zombie.UsedFromLua;
import zombie.debug.DebugLog;
import zombie.debug.DebugLogStream;
import zombie.debug.LogSeverity;

@UsedFromLua
public enum DebugType {
    Packet,
    NetworkFileDebug,
    Network,
    General,
    DetailedInfo,
    Lua,
    Mod,
    Sound,
    Zombie,
    Combat,
    Objects,
    Fireplace,
    Radio,
    MapLoading,
    Clothing,
    Animation,
    AnimationDetailed,
    AnimationLayers,
    AnimationRecorder,
    Asset,
    Script,
    Shader,
    Sprite,
    Input,
    Recipe,
    ActionSystem,
    ActionSystemEvents,
    IsoRegion,
    FileIO,
    Multiplayer,
    Damage,
    Death,
    Discord,
    Statistic,
    Vehicle,
    Voice,
    Checksum,
    Animal,
    ItemPicker,
    CraftLogic,
    Action,
    Entity,
    Lightning,
    Grapple,
    ExitDebug,
    BodyDamage,
    Xml,
    Physics,
    Ballistics,
    Ragdoll,
    PZBullet,
    ModelManager,
    LoadAnimation,
    Zone,
    WorldGen,
    Foraging,
    Saving,
    Fluid,
    Energy,
    Translation,
    Moveable,
    Basement,
    FallDamage,
    ImGui,
    CharacterTrait,
    VehicleHit;

    public static final DebugType Default;
    private final DebugLogStream logStream = DebugLog.createLogStream(this);

    public boolean isEnabled() {
        return this.getLogStream().isEnabled();
    }

    public boolean isEnabled(LogSeverity logSeverity) {
        return this.getLogStream().isLogEnabled(logSeverity);
    }

    public DebugLogStream getLogStream() {
        return this.logStream;
    }

    public void print(boolean b) {
        this.getLogStream().print(b);
    }

    public void print(char c) {
        this.getLogStream().print(c);
    }

    public void print(int i) {
        this.getLogStream().print(i);
    }

    public void print(long l) {
        this.getLogStream().print(l);
    }

    public void print(float f) {
        this.getLogStream().print(f);
    }

    public void print(double d) {
        this.getLogStream().print(d);
    }

    public void print(String s) {
        this.getLogStream().print(s);
    }

    public void print(Object obj) {
        this.getLogStream().print(obj);
    }

    public PrintStream printf(String format, Object ... args2) {
        return this.getLogStream().printf(format, args2);
    }

    public void println() {
        this.getLogStream().println();
    }

    public void println(boolean x) {
        this.getLogStream().println(x);
    }

    public void println(char x) {
        this.getLogStream().println(x);
    }

    public void println(int x) {
        this.getLogStream().println(x);
    }

    public void println(long x) {
        this.getLogStream().println(x);
    }

    public void println(float x) {
        this.getLogStream().println(x);
    }

    public void println(double x) {
        this.getLogStream().println(x);
    }

    public void println(char[] x) {
        this.getLogStream().println(x);
    }

    public void println(String x) {
        this.getLogStream().println(x);
    }

    public void println(Object x) {
        this.getLogStream().println(x);
    }

    public void println(String format, Object ... params) {
        this.getLogStream().println(format, params);
    }

    public void trace(Object formatNoParams) {
        this.getLogStream().trace(1, formatNoParams);
    }

    public void trace(String format, Object ... params) {
        this.getLogStream().trace(1, format, params);
    }

    public void debugln(Object formatNoParams) {
        this.getLogStream().debugln(1, formatNoParams);
    }

    public void debugln(String format, Object ... params) {
        this.getLogStream().debugln(1, format, params);
    }

    public void debugOnceln(Object formatNoParams) {
        this.getLogStream().debugOnceln(1, formatNoParams);
    }

    public void debugOnceln(String format, Object ... params) {
        this.getLogStream().debugOnceln(1, format, params);
    }

    public void noise(Object formatNoParams) {
        this.getLogStream().noise(1, formatNoParams);
    }

    public void noise(String format, Object ... params) {
        this.getLogStream().noise(1, format, params);
    }

    public void warn(Object formatNoParams) {
        this.getLogStream().warn(1, formatNoParams);
    }

    public void warn(String format, Object ... params) {
        this.getLogStream().warn(1, format, params);
    }

    public void warnOnce(Object formatNoParams) {
        this.getLogStream().warnOnce(1, formatNoParams);
    }

    public void warnOnce(String format, Object ... params) {
        this.getLogStream().warnOnce(1, format, params);
    }

    public void error(Object formatNoParams) {
        this.getLogStream().error(1, formatNoParams);
    }

    public void error(String format, Object ... params) {
        this.getLogStream().error(1, format, params);
    }

    public void write(LogSeverity logSeverity, String logText) {
        this.routedWrite(1, logSeverity, logText);
    }

    public void routedWrite(int backTraceOffset, LogSeverity logSeverity, String logText) {
        switch (logSeverity) {
            case Trace: {
                this.getLogStream().trace(backTraceOffset + 1, (Object)logText);
                break;
            }
            case Noise: {
                this.getLogStream().noise(backTraceOffset + 1, (Object)logText);
                break;
            }
            case Debug: {
                this.getLogStream().debugln(backTraceOffset + 1, (Object)logText);
                break;
            }
            case General: {
                this.getLogStream().println(logText);
                break;
            }
            case Warning: {
                this.getLogStream().warn(backTraceOffset + 1, (Object)logText);
                break;
            }
            case Error: {
                this.getLogStream().error(backTraceOffset + 1, (Object)logText);
                break;
            }
        }
    }

    public void printException(Throwable ex, String message, LogSeverity logSeverity) {
        this.getLogStream().printException(ex, message, DebugLogStream.generateCallerPrefix(), logSeverity);
    }

    public void printException(Throwable ex, LogSeverity logSeverity, String messageFormat, Object ... params) {
        this.getLogStream().printException(ex, logSeverity, DebugLogStream.generateCallerPrefix(), messageFormat, new Object[]{logSeverity});
    }

    public void printStackTrace() {
        this.getLogStream().printStackTrace(LogSeverity.Error, 1, -1, null, new Object[0]);
    }

    public void printStackTrace(String message) {
        this.getLogStream().printStackTrace(LogSeverity.Error, 1, -1, message, new Object[0]);
    }

    public void printStackTrace(LogSeverity severity, int depth, String messageFormat, Object ... params) {
        this.getLogStream().printStackTrace(severity, 1, depth, messageFormat, params);
    }

    static {
        Default = General;
    }
}

