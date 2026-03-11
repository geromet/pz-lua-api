/*
 * Decompiled with CFR 0.152.
 */
package zombie.core.properties;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import zombie.UsedFromLua;
import zombie.core.math.PZMath;
import zombie.core.properties.TilePropertyKey;
import zombie.iso.IsoDirections;

@UsedFromLua
public final class TileProperties {
    private final Map<TilePropertyKey, String> properties = new HashMap<TilePropertyKey, String>();
    private final EnumSet<SurfaceFlag> surfaceFlags = EnumSet.noneOf(SurfaceFlag.class);
    private boolean surfaceValid;
    private byte surface;
    private byte itemHeight;
    private short stackReplaceTileOffset;
    private IsoDirections slopedSurfaceDirection;
    private byte slopedSurfaceHeightMin;
    private byte slopedSurfaceHeightMax;

    public boolean has(TilePropertyKey tilePropertyKey) {
        return this.properties.containsKey(tilePropertyKey);
    }

    public void set(TilePropertyKey tilePropertyKey, String value) {
        if (tilePropertyKey == null) {
            return;
        }
        this.properties.put(tilePropertyKey, value == null ? "" : value);
        this.invalidateSurfaceCache();
    }

    public void set(TilePropertyKey tilePropertyKey) {
        this.set(tilePropertyKey, "");
    }

    public void unset(TilePropertyKey tilePropertyKey) {
        if (tilePropertyKey == null) {
            return;
        }
        this.properties.remove(tilePropertyKey);
        this.invalidateSurfaceCache();
    }

    public String get(TilePropertyKey tilePropertyKey) {
        return this.properties.get(tilePropertyKey);
    }

    public boolean propertyEquals(TilePropertyKey tilePropertyKey, String value) {
        String v = this.properties.get(tilePropertyKey);
        return v != null && v.equals(value);
    }

    public void addProperties(TileProperties other) {
        if (other == null) {
            return;
        }
        this.properties.putAll(other.properties);
        this.invalidateSurfaceCache();
    }

    public List<TilePropertyKey> getTileProperties() {
        ArrayList<TilePropertyKey> list = new ArrayList<TilePropertyKey>(this.properties.keySet());
        list.sort(Comparator.comparing(TilePropertyKey::toString));
        return list;
    }

    public Map<TilePropertyKey, String> getAll() {
        return Collections.unmodifiableMap(this.properties);
    }

    public void clear() {
        this.properties.clear();
        this.surfaceFlags.clear();
        this.invalidateSurfaceCache();
    }

    private void invalidateSurfaceCache() {
        this.surfaceValid = false;
    }

    private void initSurface() {
        if (this.surfaceValid) {
            return;
        }
        this.surfaceFlags.clear();
        this.surface = 0;
        this.itemHeight = 0;
        this.stackReplaceTileOffset = 0;
        this.slopedSurfaceDirection = null;
        this.slopedSurfaceHeightMin = 0;
        this.slopedSurfaceHeightMax = 0;
        for (Map.Entry<TilePropertyKey, String> entry : this.properties.entrySet()) {
            TilePropertyKey tilePropertyKey = entry.getKey();
            String val = entry.getValue();
            if (tilePropertyKey == TilePropertyKey.SURFACE) {
                try {
                    this.surface = (byte)Integer.parseInt(val);
                }
                catch (NumberFormatException numberFormatException) {}
                continue;
            }
            if (tilePropertyKey == TilePropertyKey.IS_SURFACE_OFFSET) {
                this.surfaceFlags.add(SurfaceFlag.ISOFFSET);
                continue;
            }
            if (tilePropertyKey == TilePropertyKey.IS_TABLE) {
                this.surfaceFlags.add(SurfaceFlag.ISTABLE);
                continue;
            }
            if (tilePropertyKey == TilePropertyKey.IS_TABLE_TOP) {
                this.surfaceFlags.add(SurfaceFlag.ISTABLETOP);
                continue;
            }
            if (tilePropertyKey == TilePropertyKey.STACK_REPLACE_TILE_OFFSET) {
                try {
                    this.stackReplaceTileOffset = (short)Integer.parseInt(val);
                }
                catch (NumberFormatException numberFormatException) {}
                continue;
            }
            if (tilePropertyKey == TilePropertyKey.ITEM_HEIGHT) {
                try {
                    this.itemHeight = (byte)Integer.parseInt(val);
                }
                catch (NumberFormatException numberFormatException) {}
                continue;
            }
            if (tilePropertyKey == TilePropertyKey.SLOPED_SURFACE_DIRECTION) {
                this.slopedSurfaceDirection = IsoDirections.fromString(val);
                continue;
            }
            if (tilePropertyKey == TilePropertyKey.SLOPED_SURFACE_HEIGHT_MIN) {
                this.slopedSurfaceHeightMin = (byte)PZMath.clamp(PZMath.tryParseInt(val, 0), 0, 100);
                continue;
            }
            if (tilePropertyKey != TilePropertyKey.SLOPED_SURFACE_HEIGHT_MAX) continue;
            this.slopedSurfaceHeightMax = (byte)PZMath.clamp(PZMath.tryParseInt(val, 0), 0, 100);
        }
        this.surfaceValid = true;
    }

    public int getSurface() {
        this.initSurface();
        return this.surface;
    }

    public boolean isSurfaceOffset() {
        this.initSurface();
        return this.surfaceFlags.contains((Object)SurfaceFlag.ISOFFSET);
    }

    public boolean isTable() {
        this.initSurface();
        return this.surfaceFlags.contains((Object)SurfaceFlag.ISTABLE);
    }

    public boolean isTableTop() {
        this.initSurface();
        return this.surfaceFlags.contains((Object)SurfaceFlag.ISTABLETOP);
    }

    public int getStackReplaceTileOffset() {
        this.initSurface();
        return this.stackReplaceTileOffset;
    }

    public int getItemHeight() {
        this.initSurface();
        return this.itemHeight;
    }

    public IsoDirections getSlopedSurfaceDirection() {
        this.initSurface();
        return this.slopedSurfaceDirection;
    }

    public int getSlopedSurfaceHeightMin() {
        this.initSurface();
        return this.slopedSurfaceHeightMin;
    }

    public int getSlopedSurfaceHeightMax() {
        this.initSurface();
        return this.slopedSurfaceHeightMax;
    }

    public String toString() {
        return "TilePropertyContainer{properties=" + String.valueOf(this.properties) + ", surfaceFlags=" + String.valueOf(this.surfaceFlags) + "}";
    }

    public static enum SurfaceFlag {
        INVALID,
        ISOFFSET,
        ISTABLE,
        ISTABLETOP;

    }
}

