/*
 * Decompiled with CFR 0.152.
 */
package zombie.pathfind.nativeCode;

import zombie.ai.astar.Mover;
import zombie.characters.IsoPlayer;
import zombie.characters.IsoZombie;
import zombie.characters.animals.IsoAnimal;
import zombie.pathfind.nativeCode.IPathfindTask;
import zombie.pathfind.nativeCode.PathFindRequest;
import zombie.pathfind.nativeCode.PathfindNativeThread;
import zombie.popman.ObjectPool;

class PathRequestTask
implements IPathfindTask {
    PathFindRequest request;
    int queueNumber;
    static final ObjectPool<PathRequestTask> pool = new ObjectPool<PathRequestTask>(PathRequestTask::new);

    PathRequestTask() {
    }

    /*
     * Enabled aggressive block sorting
     */
    PathRequestTask init(PathFindRequest request) {
        this.request = request;
        if (request.mover instanceof IsoPlayer && !(request.mover instanceof IsoAnimal)) {
            this.queueNumber = 1;
            return this;
        }
        Mover mover = request.mover;
        if (mover instanceof IsoZombie) {
            IsoZombie isoZombie = (IsoZombie)mover;
            if (isoZombie.target != null) {
                this.queueNumber = 2;
                return this;
            }
        }
        this.queueNumber = 3;
        return this;
    }

    @Override
    public void execute() {
        PathfindNativeThread.instance.addRequest(this.request, this.queueNumber);
    }

    static PathRequestTask alloc() {
        return pool.alloc();
    }

    @Override
    public void release() {
        pool.release(this);
    }
}

