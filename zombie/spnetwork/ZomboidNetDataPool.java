/*
 * Decompiled with CFR 0.152.
 */
package zombie.spnetwork;

import java.util.ArrayDeque;
import zombie.spnetwork.ZomboidNetData;

public final class ZomboidNetDataPool {
    public static ZomboidNetDataPool instance = new ZomboidNetDataPool();
    private final ArrayDeque<ZomboidNetData> pool = new ArrayDeque();

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public ZomboidNetData get() {
        ArrayDeque<ZomboidNetData> arrayDeque = this.pool;
        synchronized (arrayDeque) {
            if (this.pool.isEmpty()) {
                return new ZomboidNetData();
            }
            return this.pool.pop();
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public void discard(ZomboidNetData data) {
        data.reset();
        if (data.buffer.capacity() == 2048) {
            ArrayDeque<ZomboidNetData> arrayDeque = this.pool;
            synchronized (arrayDeque) {
                this.pool.add(data);
            }
        }
    }

    public ZomboidNetData getLong(int len) {
        return new ZomboidNetData(len);
    }
}

