/*
 * Decompiled with CFR 0.152.
 */
package zombie.scripting.objects;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import zombie.UsedFromLua;
import zombie.scripting.objects.ResourceLocation;

@UsedFromLua
public class Registry<T>
implements Iterable<T> {
    private final Map<ResourceLocation, T> byLocation = new HashMap<ResourceLocation, T>();
    private final Map<T, ResourceLocation> byObject = new IdentityHashMap<T, ResourceLocation>();
    private final List<T> values = new ArrayList<T>();
    private final String name;

    public Registry(String name) {
        this.name = name;
    }

    public ResourceLocation getLocation(T t) {
        return this.byObject.get(t);
    }

    public T register(ResourceLocation id, T t) {
        ResourceLocation oldId = this.byObject.put(t, id);
        T oldT = this.byLocation.put(id, t);
        this.values.add(t);
        if (oldT != null || oldId != null) {
            throw new IllegalArgumentException("Tried to register duplicate object: %s in: %s".formatted(id, this.name));
        }
        return t;
    }

    public T get(ResourceLocation id) {
        return this.byLocation.get(id);
    }

    public boolean contains(ResourceLocation id) {
        return this.byLocation.containsKey(id);
    }

    public List<T> values() {
        return Collections.unmodifiableList(this.values);
    }

    public Set<ResourceLocation> keys() {
        return Collections.unmodifiableSet(this.byLocation.keySet());
    }

    protected void reset() {
        Set thingsToDelete = this.byObject.entrySet().stream().filter(e -> !((ResourceLocation)e.getValue()).getNamespace().equals("base")).map(Map.Entry::getKey).collect(Collectors.toSet());
        this.byObject.keySet().removeAll(thingsToDelete);
        this.byLocation.entrySet().removeIf(e -> thingsToDelete.contains(e.getValue()));
        this.values.removeAll(thingsToDelete);
    }

    @Override
    public Iterator<T> iterator() {
        return this.byLocation.values().iterator();
    }

    public String toString() {
        return "Registry{" + this.name + ", size=" + this.byLocation.size() + "}";
    }
}

