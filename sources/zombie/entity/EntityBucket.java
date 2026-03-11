/*
 * Decompiled with CFR 0.152.
 */
package zombie.entity;

import java.util.Comparator;
import zombie.UsedFromLua;
import zombie.core.Core;
import zombie.debug.DebugLog;
import zombie.entity.Family;
import zombie.entity.GameEntity;
import zombie.entity.GameEntityManager;
import zombie.entity.IBucketListener;
import zombie.entity.util.Array;
import zombie.entity.util.BitSet;
import zombie.entity.util.ImmutableArray;
import zombie.entity.util.ObjectSet;
import zombie.inventory.InventoryItem;
import zombie.iso.IsoObject;
import zombie.vehicles.VehiclePart;

@UsedFromLua
public abstract class EntityBucket {
    private final Array<GameEntity> entities;
    private final ImmutableArray<GameEntity> immutableEntities;
    private final Array<BucketListenerData> listeners = new Array(true, 16);
    private final ObjectSet<IBucketListener> listenerSet = new ObjectSet();
    private final BucketListenerComparator listenerComparator = new BucketListenerComparator();
    private final int index;
    private boolean verbose;

    private EntityBucket(int index) {
        this.entities = new Array(false, 16);
        this.immutableEntities = new ImmutableArray<GameEntity>(this.entities);
        this.index = index;
    }

    public final int getIndex() {
        return this.index;
    }

    public final ImmutableArray<GameEntity> getEntities() {
        return this.immutableEntities;
    }

    public final void setVerbose(boolean b) {
        this.verbose = b;
    }

    protected abstract boolean acceptsEntity(GameEntity var1);

    final void updateMembership(GameEntity entity) {
        block11: {
            boolean acceptsEntity;
            boolean containsEntity;
            BitSet bits;
            block10: {
                bits = entity.getBucketBits();
                containsEntity = bits.get(this.index);
                acceptsEntity = this.acceptsEntity(entity);
                if (Core.debug && this.verbose) {
                    DebugLog.Entity.println("testing entity = " + entity.getEntityNetID() + ", type=" + String.valueOf(entity.getGameEntityType()) + ", contains=" + containsEntity + ", accepts=" + acceptsEntity + ", removing=" + entity.removingFromEngine);
                }
                if (entity.removingFromEngine || containsEntity || !acceptsEntity) break block10;
                if (Core.debug && this.verbose) {
                    DebugLog.Entity.println("adding entity = " + entity.getEntityNetID() + ", type=" + String.valueOf(entity.getGameEntityType()));
                }
                if (Core.debug && GameEntityManager.debugMode && this.entities.contains(entity, true)) {
                    throw new RuntimeException("Entity already exists in bucket.");
                }
                this.entities.add(entity);
                bits.set(this.index);
                if (Core.debug && this.verbose) {
                    DebugLog.Entity.println("bits = " + bits.get(this.index));
                }
                if (this.listeners.size <= 0) break block11;
                for (int i = 0; i < this.listeners.size; ++i) {
                    this.listeners.get((int)i).listener.onBucketEntityAdded(this, entity);
                }
                break block11;
            }
            if (containsEntity && (entity.removingFromEngine || !acceptsEntity)) {
                if (Core.debug && this.verbose) {
                    DebugLog.Entity.println("removing entity = " + entity.getEntityNetID() + ", type=" + String.valueOf(entity.getGameEntityType()));
                }
                if (Core.debug && GameEntityManager.debugMode && !this.entities.contains(entity, true)) {
                    throw new RuntimeException("Entity should exist in bucket but does not.");
                }
                this.entities.removeValue(entity, true);
                bits.clear(this.index);
                if (this.listeners.size > 0) {
                    for (int i = 0; i < this.listeners.size; ++i) {
                        this.listeners.get((int)i).listener.onBucketEntityRemoved(this, entity);
                    }
                }
            }
        }
    }

    public final void addListener(int priority, IBucketListener listener) {
        if (this.listenerSet.contains(listener)) {
            return;
        }
        BucketListenerData data = new BucketListenerData();
        data.listener = listener;
        data.priority = priority;
        this.listeners.add(data);
        this.listeners.sort(this.listenerComparator);
    }

    public final void removeListener(IBucketListener listener) {
        if (this.listenerSet.remove(listener)) {
            for (int i = 0; i < this.listeners.size; ++i) {
                if (this.listeners.get((int)i).listener != listener) continue;
                this.listeners.removeIndex(i);
                break;
            }
        }
    }

    private static class BucketListenerComparator
    implements Comparator<BucketListenerData> {
        private BucketListenerComparator() {
        }

        @Override
        public int compare(BucketListenerData a, BucketListenerData b) {
            return Integer.compare(a.priority, b.priority);
        }
    }

    private static class BucketListenerData {
        public IBucketListener listener;
        public int priority;

        private BucketListenerData() {
        }
    }

    protected static class CustomBucket
    extends EntityBucket {
        private final EntityValidator validator;

        protected CustomBucket(int index, EntityValidator validator) {
            super(index);
            this.validator = validator;
        }

        @Override
        protected final boolean acceptsEntity(GameEntity entity) {
            return this.validator.acceptsEntity(entity);
        }
    }

    public static interface EntityValidator {
        public boolean acceptsEntity(GameEntity var1);
    }

    protected static class FamilyBucket
    extends EntityBucket {
        private final Family family;

        protected FamilyBucket(int index, Family family) {
            super(index);
            this.family = family;
        }

        @Override
        protected final boolean acceptsEntity(GameEntity entity) {
            return this.family.matches(entity);
        }
    }

    protected static class VehiclePartBucket
    extends EntityBucket {
        protected VehiclePartBucket(int index) {
            super(index);
        }

        @Override
        protected final boolean acceptsEntity(GameEntity entity) {
            return entity instanceof VehiclePart;
        }
    }

    protected static class InventoryItemBucket
    extends EntityBucket {
        protected InventoryItemBucket(int index) {
            super(index);
        }

        @Override
        protected final boolean acceptsEntity(GameEntity entity) {
            return entity instanceof InventoryItem;
        }
    }

    protected static class IsoObjectBucket
    extends EntityBucket {
        protected IsoObjectBucket(int index) {
            super(index);
        }

        @Override
        protected final boolean acceptsEntity(GameEntity entity) {
            return entity instanceof IsoObject;
        }
    }

    protected static class RendererBucket
    extends EntityBucket {
        protected RendererBucket(int index) {
            super(index);
        }

        @Override
        protected final boolean acceptsEntity(GameEntity entity) {
            return entity.hasRenderers();
        }
    }
}

