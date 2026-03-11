/*
 * Decompiled with CFR 0.152.
 */
package zombie.iso.areas.isoregion;

import java.util.ArrayList;
import java.util.concurrent.ConcurrentLinkedQueue;
import zombie.UsedFromLua;
import zombie.core.Color;
import zombie.core.Core;
import zombie.debug.DebugType;
import zombie.iso.areas.isoregion.IsoRegionLogType;

@UsedFromLua
public class IsoRegionsLogger {
    private final ConcurrentLinkedQueue<IsoRegionLog> pool = new ConcurrentLinkedQueue();
    private final ConcurrentLinkedQueue<IsoRegionLog> loggerQueue = new ConcurrentLinkedQueue();
    private final boolean consolePrint;
    private final ArrayList<IsoRegionLog> logs = new ArrayList();
    private final int maxLogs = 100;
    private boolean isDirtyUi;

    public IsoRegionsLogger(boolean doConsolePrint) {
        this.consolePrint = doConsolePrint;
    }

    public ArrayList<IsoRegionLog> getLogs() {
        return this.logs;
    }

    public boolean isDirtyUI() {
        return this.isDirtyUi;
    }

    public void unsetDirtyUI() {
        this.isDirtyUi = false;
    }

    private IsoRegionLog getLog() {
        IsoRegionLog log = this.pool.poll();
        if (log == null) {
            log = new IsoRegionLog();
        }
        return log;
    }

    protected void log(String str) {
        this.log(str, null);
    }

    protected void log(String str, Color col) {
        if (Core.debug) {
            if (this.consolePrint) {
                DebugType.IsoRegion.println(str);
            }
            IsoRegionLog log = this.getLog();
            log.str = str;
            log.type = IsoRegionLogType.Normal;
            log.col = col;
            this.loggerQueue.offer(log);
        }
    }

    protected void warn(String str) {
        DebugType.IsoRegion.warn(str);
        if (Core.debug) {
            IsoRegionLog log = this.getLog();
            log.str = str;
            log.type = IsoRegionLogType.Warn;
            this.loggerQueue.offer(log);
        }
    }

    protected void update() {
        if (!Core.debug) {
            return;
        }
        IsoRegionLog log = this.loggerQueue.poll();
        while (log != null) {
            if (this.logs.size() >= 100) {
                IsoRegionLog removed = this.logs.remove(0);
                removed.col = null;
                this.pool.offer(removed);
            }
            this.logs.add(log);
            this.isDirtyUi = true;
            log = this.loggerQueue.poll();
        }
    }

    @UsedFromLua
    public static class IsoRegionLog {
        private String str;
        private IsoRegionLogType type;
        private Color col;

        public String getStr() {
            return this.str;
        }

        public IsoRegionLogType getType() {
            return this.type;
        }

        public Color getColor() {
            if (this.col != null) {
                return this.col;
            }
            if (this.type == IsoRegionLogType.Warn) {
                return Color.red;
            }
            return Color.white;
        }
    }
}

