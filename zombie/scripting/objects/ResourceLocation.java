/*
 * Decompiled with CFR 0.152.
 */
package zombie.scripting.objects;

import java.util.Locale;
import java.util.Objects;
import zombie.UsedFromLua;

@UsedFromLua
public final class ResourceLocation {
    public static final String DEFAULT_NAMESPACE = "base";
    private final String namespace;
    private final String path;
    private final String id;

    public ResourceLocation(String namespace, String path) {
        if (namespace == null || namespace.isEmpty()) {
            throw new IllegalArgumentException("Namespace cannot be null or empty");
        }
        if (path == null || path.isEmpty()) {
            throw new IllegalArgumentException("Path cannot be null or empty");
        }
        this.namespace = namespace.toLowerCase(Locale.ENGLISH);
        this.path = path.toLowerCase(Locale.ENGLISH);
        this.id = this.namespace + ":" + this.path;
    }

    public static ResourceLocation of(String id) {
        if (id == null || id.isEmpty()) {
            throw new IllegalArgumentException("Identifier cannot be null or empty");
        }
        String[] parts = id.split(":", 2);
        if (parts.length == 1) {
            return new ResourceLocation(DEFAULT_NAMESPACE, parts[0]);
        }
        return new ResourceLocation(parts[0], parts[1]);
    }

    public String getNamespace() {
        return this.namespace;
    }

    public String getPath() {
        return this.path;
    }

    public String toString() {
        return this.id;
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ResourceLocation)) {
            return false;
        }
        ResourceLocation resourceLocation = (ResourceLocation)o;
        return this.namespace.equals(resourceLocation.namespace) && this.path.equals(resourceLocation.path);
    }

    public int hashCode() {
        return Objects.hash(this.namespace, this.path);
    }
}

