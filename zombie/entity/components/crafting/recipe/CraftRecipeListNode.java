/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  java.lang.MatchException
 */
package zombie.entity.components.crafting.recipe;

import java.util.ArrayList;
import java.util.List;
import zombie.UsedFromLua;
import zombie.core.textures.Texture;
import zombie.debug.DebugLog;
import zombie.scripting.entity.components.crafting.CraftRecipe;
import zombie.scripting.objects.CraftRecipeGroup;

@UsedFromLua
public class CraftRecipeListNode {
    protected CraftRecipeListNodeType type;
    protected CraftRecipeListNode parent;
    protected List<CraftRecipeListNode> children;
    protected CraftRecipe recipe;
    protected Texture iconTexture;
    protected String title;
    protected CraftRecipeGroup group;
    protected CraftRecipeListNodeExpandedState expandedState;

    public static CraftRecipeListNode createGroupNode(CraftRecipeGroup group, String title, Texture iconTexture, CraftRecipeListNodeExpandedState expandedState) {
        CraftRecipeListNode node = new CraftRecipeListNode();
        node.type = CraftRecipeListNodeType.GROUP;
        node.title = title;
        node.group = group;
        node.children = new ArrayList<CraftRecipeListNode>();
        node.iconTexture = iconTexture;
        node.expandedState = expandedState;
        return node;
    }

    public static CraftRecipeListNode createRecipeNode(CraftRecipe recipe, CraftRecipeListNode parent) {
        CraftRecipeListNode node = new CraftRecipeListNode();
        node.type = CraftRecipeListNodeType.RECIPE;
        node.parent = parent;
        node.recipe = recipe;
        node.expandedState = CraftRecipeListNodeExpandedState.PARTIAL;
        if (recipe != null) {
            node.group = recipe.getRecipeGroup();
            node.title = recipe.getTranslationName();
            node.iconTexture = recipe.getIconTexture();
        } else {
            DebugLog.CraftLogic.error("recipe == null when calling CraftRecipeListNode:allocRecipe - This should not happen!");
        }
        return node;
    }

    public CraftRecipeListNodeType getType() {
        return this.type;
    }

    public CraftRecipeListNode getParent() {
        return this.parent;
    }

    public CraftRecipe getRecipe() {
        return this.recipe;
    }

    public Texture getIconTexture() {
        return this.iconTexture;
    }

    public String getTitle() {
        return this.title;
    }

    public CraftRecipeGroup getGroup() {
        return this.group;
    }

    public CraftRecipeListNodeExpandedState getExpandedState() {
        return this.expandedState;
    }

    public void setExpandedState(CraftRecipeListNodeExpandedState state) {
        this.expandedState = state;
    }

    public void toggleExpandedState() {
        this.setExpandedState(switch (this.expandedState.ordinal()) {
            default -> throw new MatchException(null, null);
            case 0 -> CraftRecipeListNodeExpandedState.PARTIAL;
            case 1 -> CraftRecipeListNodeExpandedState.OPEN;
            case 2 -> CraftRecipeListNodeExpandedState.CLOSED;
        });
    }

    public List<CraftRecipeListNode> getChildren() {
        return this.children;
    }

    @UsedFromLua
    public static enum CraftRecipeListNodeType {
        GROUP,
        RECIPE;

    }

    @UsedFromLua
    public static enum CraftRecipeListNodeExpandedState {
        CLOSED,
        PARTIAL,
        OPEN;

    }
}

