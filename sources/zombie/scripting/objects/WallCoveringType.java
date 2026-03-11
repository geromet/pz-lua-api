/*
 * Decompiled with CFR 0.152.
 */
package zombie.scripting.objects;

import zombie.UsedFromLua;

@UsedFromLua
public enum WallCoveringType {
    PAINT_THUMP("paintThump"),
    PAINT_SIGN("paintSign"),
    PLASTER("plaster"),
    WALLPAPER("wallpaper");

    private final String typeString;

    private WallCoveringType(String typeString) {
        this.typeString = typeString;
    }

    public String toString() {
        return this.typeString;
    }

    public static WallCoveringType typeOf(String typeString) {
        for (WallCoveringType type : WallCoveringType.values()) {
            if (!type.toString().equalsIgnoreCase(typeString)) continue;
            return type;
        }
        throw new IllegalArgumentException("No enum constant WallCoveringType with value " + typeString);
    }
}

