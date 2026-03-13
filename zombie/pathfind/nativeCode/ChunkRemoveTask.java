/*
 * Decompiled with CFR 0.152.
 */
package zombie.pathfind.nativeCode;

import zombie.iso.IsoChunk;
import zombie.pathfind.nativeCode.IPathfindTask;
import zombie.pathfind.nativeCode.PathfindNative;
import zombie.popman.ObjectPool;

class ChunkRemoveTask
implements IPathfindTask {
    int wx;
    int wy;
    static final ObjectPool<ChunkRemoveTask> pool = new ObjectPool<ChunkRemoveTask>(ChunkRemoveTask::new);

    ChunkRemoveTask() {
    }

    ChunkRemoveTask init(IsoChunk chunk) {
        this.wx = chunk.wx;
        this.wy = chunk.wy;
        return this;
    }

    @Override
    public void execute() {
        PathfindNative.removeChunk(this.wx, this.wy);
    }

    static ChunkRemoveTask alloc() {
        return pool.alloc();
    }

    @Override
    public void release() {
        pool.release(this);
    }
}

