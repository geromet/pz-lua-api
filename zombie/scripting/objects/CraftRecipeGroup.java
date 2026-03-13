/*
 * Decompiled with CFR 0.152.
 */
package zombie.scripting.objects;

import zombie.UsedFromLua;
import zombie.core.Translator;
import zombie.core.textures.Texture;

@UsedFromLua
public enum CraftRecipeGroup {
    OPEN_BOX("OpenBox", "media/textures/Build_CrateWood.png");

    private final String id;
    private final String iconPath;
    private Texture iconTexture;

    private CraftRecipeGroup(String id, String iconPath) {
        this.id = id;
        this.iconPath = iconPath;
    }

    public static CraftRecipeGroup fromString(String id) {
        if (id != null) {
            for (CraftRecipeGroup entry : CraftRecipeGroup.values()) {
                if (!id.equals(entry.id)) continue;
                return entry;
            }
        }
        return null;
    }

    public String toString() {
        return this.id;
    }

    public String getTranslationName() {
        return Translator.getRecipeGroupName(String.format("RecipeGroup_%s", this.id));
    }

    public Texture getIconTexture() {
        if (this.iconTexture == null && this.iconPath != null) {
            this.iconTexture = Texture.getTexture(this.iconPath);
        }
        return this.iconTexture;
    }
}

