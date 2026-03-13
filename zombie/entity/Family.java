/*
 * Decompiled with CFR 0.152.
 */
package zombie.entity;

import zombie.UsedFromLua;
import zombie.entity.ComponentType;
import zombie.entity.GameEntity;
import zombie.entity.util.BitSet;
import zombie.entity.util.ObjectMap;

@UsedFromLua
public class Family {
    private static final ObjectMap<String, Family> families = new ObjectMap();
    private static int familyIndex;
    private static final Builder builder;
    private static final BitSet zeroBits;
    private final BitSet all;
    private final BitSet one;
    private final BitSet exclude;
    private final int index;

    private Family(BitSet all, BitSet any, BitSet exclude) {
        this.all = all;
        this.one = any;
        this.exclude = exclude;
        this.index = familyIndex++;
    }

    public int getIndex() {
        return this.index;
    }

    public boolean matches(GameEntity entity) {
        BitSet entityComponentBits = entity.getComponentBits();
        if (entityComponentBits == null) {
            return false;
        }
        if (!entityComponentBits.containsAll(this.all)) {
            return false;
        }
        if (!this.one.isEmpty() && !this.one.intersects(entityComponentBits)) {
            return false;
        }
        return this.exclude.isEmpty() || !this.exclude.intersects(entityComponentBits);
    }

    public static final Builder all(ComponentType ... componentTypes) {
        return builder.reset().all(componentTypes);
    }

    public static final Builder one(ComponentType ... componentTypes) {
        return builder.reset().one(componentTypes);
    }

    public static final Builder exclude(ComponentType ... componentTypes) {
        return builder.reset().exclude(componentTypes);
    }

    public int hashCode() {
        return this.index;
    }

    public boolean equals(Object obj) {
        return this == obj;
    }

    private static String getFamilyHash(BitSet all, BitSet one, BitSet exclude) {
        StringBuilder stringBuilder = new StringBuilder();
        if (!all.isEmpty()) {
            stringBuilder.append("{all:").append(Family.getBitsString(all)).append("}");
        }
        if (!one.isEmpty()) {
            stringBuilder.append("{one:").append(Family.getBitsString(one)).append("}");
        }
        if (!exclude.isEmpty()) {
            stringBuilder.append("{exclude:").append(Family.getBitsString(exclude)).append("}");
        }
        return stringBuilder.toString();
    }

    private static String getBitsString(BitSet bits) {
        StringBuilder stringBuilder = new StringBuilder();
        int numBits = bits.length();
        for (int i = 0; i < numBits; ++i) {
            stringBuilder.append(bits.get(i) ? "1" : "0");
        }
        return stringBuilder.toString();
    }

    static {
        builder = new Builder();
        zeroBits = new BitSet();
    }

    public static class Builder {
        private BitSet all = zeroBits;
        private BitSet one = zeroBits;
        private BitSet exclude = zeroBits;

        Builder() {
        }

        public Builder reset() {
            this.all = zeroBits;
            this.one = zeroBits;
            this.exclude = zeroBits;
            return this;
        }

        public final Builder all(ComponentType ... componentTypes) {
            this.all = ComponentType.getBitsFor(componentTypes);
            return this;
        }

        public final Builder one(ComponentType ... componentTypes) {
            this.one = ComponentType.getBitsFor(componentTypes);
            return this;
        }

        public final Builder exclude(ComponentType ... componentTypes) {
            this.exclude = ComponentType.getBitsFor(componentTypes);
            return this;
        }

        public Family get() {
            String hash = Family.getFamilyHash(this.all, this.one, this.exclude);
            Family family = families.get(hash, null);
            if (family == null) {
                family = new Family(this.all, this.one, this.exclude);
                families.put(hash, family);
            }
            return family;
        }
    }
}

