/*
 * Decompiled with CFR 0.152.
 */
package zombie.core.logger;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.text.SimpleDateFormat;
import zombie.UsedFromLua;
import zombie.ZomboidFileSystem;
import zombie.core.logger.LoggerManager;
import zombie.debug.DebugLog;
import zombie.util.StringUtils;

@UsedFromLua
public final class ZLogger {
    private final String name;
    private final OutputStreams outputStreams = new OutputStreams();
    private File file;
    private static final SimpleDateFormat s_logSdf = new SimpleDateFormat("dd-MM-yy HH:mm:ss.SSS");
    private static final long s_maxSizeKo = 10000L;

    public ZLogger(String name, boolean useConsole) {
        this.name = name;
        try {
            this.file = new File(LoggerManager.getLogsDir() + File.separator + ZLogger.getLoggerName(name) + ".txt");
            this.outputStreams.file = new PrintStream(this.file);
        }
        catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        if (useConsole) {
            this.outputStreams.console = System.out;
        }
    }

    private static String getLoggerName(String fileName) {
        return ZomboidFileSystem.getStartupTimeStamp() + "_" + fileName;
    }

    public void write(String logs) {
        this.write(logs, null);
    }

    public void write(String logs, String level) {
        this.write(logs, level, false);
    }

    public void write(String logs, String level, boolean append) {
        try {
            this.writeUnsafe(logs, level, append);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    public synchronized void writeUnsafe(String logs, String prefix, boolean append) throws Exception {
        StringBuilder strBuilder = new StringBuilder();
        strBuilder.setLength(0);
        if (!append) {
            strBuilder.append("[").append(ZomboidFileSystem.getTimeStampStringNow(s_logSdf)).append("]");
        }
        if (!StringUtils.isNullOrEmpty(prefix)) {
            strBuilder.append("[").append(prefix).append("]");
        }
        int count = logs.length();
        if (logs.lastIndexOf(10) == logs.length() - 1) {
            --count;
        }
        if (!append) {
            strBuilder.append(" ").append(logs, 0, count).append(".");
        } else {
            strBuilder.append(logs, 0, count);
        }
        this.outputStreams.println(strBuilder.toString());
        this.checkSizeUnsafe();
    }

    public synchronized void write(Exception ex) {
        ex.printStackTrace(this.outputStreams.file);
        this.checkSize();
    }

    private synchronized void checkSize() {
        try {
            this.checkSizeUnsafe();
        }
        catch (Exception e) {
            DebugLog.General.error("Exception thrown checking log file size.");
            DebugLog.General.error(e);
            e.printStackTrace();
        }
    }

    private synchronized void checkSizeUnsafe() throws Exception {
        long currentKo = this.file.length() / 1024L;
        if (currentKo > 10000L) {
            this.outputStreams.file.close();
            this.file = new File(LoggerManager.getLogsDir() + File.separator + ZLogger.getLoggerName(this.name) + ".txt");
            this.outputStreams.file = new PrintStream(this.file);
        }
    }

    private static class OutputStreams {
        public PrintStream file;
        public PrintStream console;

        private OutputStreams() {
        }

        public void println(String str) {
            if (this.file != null) {
                this.file.println(str);
                this.file.flush();
            }
            if (this.console != null) {
                this.console.println(str);
            }
        }
    }
}

