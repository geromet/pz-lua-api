/*
 * Decompiled with CFR 0.152.
 */
package zombie.entity.util.assoc;

import java.util.EnumSet;
import java.util.Iterator;
import java.util.Objects;
import zombie.UsedFromLua;
import zombie.entity.util.assoc.AssocArray;

@UsedFromLua
public class AssocEnumArray<K extends Enum<K>, V>
extends AssocArray<K, V> {
    private final EnumSet<K> keys;

    public AssocEnumArray(Class<K> enumType) {
        this.keys = EnumSet.noneOf(enumType);
    }

    public AssocEnumArray(Class<K> enumType, int initialCapacity) {
        super(initialCapacity);
        this.keys = EnumSet.noneOf(enumType);
    }

    public boolean equalsKeys(AssocEnumArray<K, V> other) {
        if (other == this) {
            return true;
        }
        return this.keys.equals(other.keys);
    }

    public Iterator<K> keys() {
        return this.keys.iterator();
    }

    @Override
    public boolean containsKey(K o) {
        return this.keys.contains(o);
    }

    @Override
    public V put(K k, V v) {
        V res = super.put(k, v);
        this.keys.add(k);
        return res;
    }

    @Override
    public boolean add(K k, V v) {
        if (super.add(k, v)) {
            this.keys.add(k);
            return true;
        }
        return false;
    }

    @Override
    public void add(int frontIndex, K k, V v) {
        super.add(frontIndex, k, v);
        this.keys.add(k);
    }

    @Override
    public V removeIndex(int frontIndex) {
        Objects.checkIndex(frontIndex, this.size());
        Object[] es = this.elementData;
        int realKeyIdx = this.realKeyIndex(frontIndex);
        Object oldKey = es[realKeyIdx];
        Object oldValue = es[realKeyIdx + 1];
        this.fastRemove(es, realKeyIdx);
        this.keys.remove(oldKey);
        return (V)oldValue;
    }

    @Override
    public boolean equals(Object o) {
        return o.getClass() == AssocEnumArray.class && o == this;
    }

    @Override
    public V remove(K o) {
        Object val = super.remove(o);
        if (val != null) {
            this.keys.remove(o);
            return val;
        }
        return null;
    }

    @Override
    public void clear() {
        super.clear();
        this.keys.clear();
    }
}

