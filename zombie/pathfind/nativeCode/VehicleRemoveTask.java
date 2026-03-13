/*
 * Decompiled with CFR 0.152.
 */
package zombie.pathfind.nativeCode;

import zombie.pathfind.nativeCode.IPathfindTask;
import zombie.pathfind.nativeCode.PathfindNative;
import zombie.popman.ObjectPool;
import zombie.vehicles.BaseVehicle;

class VehicleRemoveTask
implements IPathfindTask {
    short vehicleId;
    static final ObjectPool<VehicleRemoveTask> pool = new ObjectPool<VehicleRemoveTask>(VehicleRemoveTask::new);

    VehicleRemoveTask() {
    }

    public VehicleRemoveTask init(BaseVehicle vehicle) {
        this.vehicleId = vehicle.vehicleId;
        assert (this.vehicleId != -1);
        return this;
    }

    @Override
    public void execute() {
        PathfindNative.removeVehicle(this.vehicleId);
    }

    static VehicleRemoveTask alloc() {
        return pool.alloc();
    }

    @Override
    public void release() {
        pool.release(this);
    }
}

