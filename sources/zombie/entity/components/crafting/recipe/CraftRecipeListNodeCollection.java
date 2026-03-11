/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  java.lang.MatchException
 */
package zombie.entity.components.crafting.recipe;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import zombie.UsedFromLua;
import zombie.entity.components.crafting.BaseCraftingLogic;
import zombie.entity.components.crafting.recipe.CraftRecipeListNode;
import zombie.scripting.entity.components.crafting.CraftRecipe;
import zombie.scripting.objects.CraftRecipeGroup;

@UsedFromLua
public class CraftRecipeListNodeCollection {
    private final List<CraftRecipeListNode> nodes = new ArrayList<CraftRecipeListNode>();
    private final Map<CraftRecipeGroup, CraftRecipeListNode> groupNodes = new HashMap<CraftRecipeGroup, CraftRecipeListNode>();

    public List<CraftRecipeListNode> getNodes() {
        return this.nodes;
    }

    public void add(CraftRecipe recipe) {
        if (recipe == null) {
            return;
        }
        CraftRecipeListNode groupParent = null;
        List<CraftRecipeListNode> parentNodeArray = this.nodes;
        if (recipe.getRecipeGroup() != null) {
            groupParent = this.groupNodes.get((Object)recipe.getRecipeGroup());
            if (groupParent == null) {
                CraftRecipeGroup recipeGroup = recipe.getRecipeGroup();
                groupParent = CraftRecipeListNode.createGroupNode(recipeGroup, recipeGroup.getTranslationName(), recipeGroup.getIconTexture(), CraftRecipeListNode.CraftRecipeListNodeExpandedState.PARTIAL);
                this.groupNodes.put(recipe.getRecipeGroup(), groupParent);
                this.nodes.add(groupParent);
            }
            parentNodeArray = groupParent.children;
        }
        CraftRecipeListNode recipeNode = CraftRecipeListNode.createRecipeNode(recipe, groupParent);
        parentNodeArray.add(recipeNode);
    }

    public void addAll(List<CraftRecipe> recipeList) {
        if (recipeList == null) {
            return;
        }
        for (CraftRecipe recipe : recipeList) {
            this.add(recipe);
        }
    }

    public void setInitialExpandedStates(BaseCraftingLogic logic, boolean isBuildCheat) {
        this.setInitialExpandedStates(logic, isBuildCheat, null, this.nodes);
    }

    private void setInitialExpandedStates(BaseCraftingLogic logic, boolean isBuildCheat, CraftRecipeListNode groupNode, List<CraftRecipeListNode> childNodes) {
        if (logic == null) {
            return;
        }
        for (CraftRecipeListNode node2 : childNodes) {
            BaseCraftingLogic.CachedRecipeInfo cachedRecipeInfo;
            if (node2.getType() == CraftRecipeListNode.CraftRecipeListNodeType.RECIPE && (cachedRecipeInfo = logic.getCachedRecipeInfo(node2.getRecipe())) != null) {
                boolean canBeCrafted = isBuildCheat || cachedRecipeInfo.isValid() && cachedRecipeInfo.isCanPerform();
                CraftRecipeListNode.CraftRecipeListNodeExpandedState craftRecipeListNodeExpandedState = node2.expandedState = canBeCrafted ? CraftRecipeListNode.CraftRecipeListNodeExpandedState.OPEN : CraftRecipeListNode.CraftRecipeListNodeExpandedState.CLOSED;
            }
            if (node2.getType() != CraftRecipeListNode.CraftRecipeListNodeType.GROUP) continue;
            this.setInitialExpandedStates(logic, isBuildCheat, node2, node2.getChildren());
        }
        if (groupNode != null) {
            if (childNodes.stream().allMatch(node -> node.expandedState == CraftRecipeListNode.CraftRecipeListNodeExpandedState.OPEN)) {
                groupNode.expandedState = CraftRecipeListNode.CraftRecipeListNodeExpandedState.OPEN;
            } else if (childNodes.stream().allMatch(node -> node.expandedState == CraftRecipeListNode.CraftRecipeListNodeExpandedState.CLOSED)) {
                groupNode.expandedState = CraftRecipeListNode.CraftRecipeListNodeExpandedState.CLOSED;
            }
        }
    }

    public void clear() {
        this.nodes.clear();
        this.groupNodes.clear();
    }

    public boolean contains(CraftRecipe recipe) {
        return this.collectionContains(recipe, this.nodes);
    }

    private boolean collectionContains(CraftRecipe recipe, List<CraftRecipeListNode> collection) {
        for (CraftRecipeListNode node : collection) {
            if (node.getType() == CraftRecipeListNode.CraftRecipeListNodeType.RECIPE && node.getRecipe().equals(recipe)) {
                return true;
            }
            if (node.getType() != CraftRecipeListNode.CraftRecipeListNodeType.GROUP || !this.collectionContains(recipe, node.children)) continue;
            return true;
        }
        return false;
    }

    public boolean isEmpty() {
        return this.nodes.isEmpty();
    }

    public void removeIf(Predicate<? super CraftRecipe> filter) {
        this.removeIf(filter, this.nodes);
    }

    private void removeIf(Predicate<? super CraftRecipe> filter, List<CraftRecipeListNode> collection) {
        for (int i = collection.size() - 1; i >= 0; --i) {
            CraftRecipeListNode node = collection.get(i);
            if (node.getType() == CraftRecipeListNode.CraftRecipeListNodeType.RECIPE && filter.test(node.getRecipe())) {
                collection.remove(i);
            }
            if (node.getType() != CraftRecipeListNode.CraftRecipeListNodeType.GROUP) continue;
            this.removeIf(filter, node.children);
        }
    }

    public void sort(Comparator<? super CraftRecipe> comparator) {
        RecipeNodeComparator nodeComparator = new RecipeNodeComparator(comparator);
        this.nodes.sort(nodeComparator);
    }

    public CraftRecipe getFirstRecipe() {
        return this.getFirstRecipe(this.nodes);
    }

    private CraftRecipe getFirstRecipe(List<CraftRecipeListNode> collection) {
        for (CraftRecipeListNode node : collection) {
            CraftRecipe recipe;
            if ((recipe = (switch (node.getType()) {
                default -> throw new MatchException(null, null);
                case CraftRecipeListNode.CraftRecipeListNodeType.RECIPE -> node.getRecipe();
                case CraftRecipeListNode.CraftRecipeListNodeType.GROUP -> this.getFirstRecipe(node.children);
            })) == null) continue;
            return recipe;
        }
        return null;
    }

    public List<CraftRecipe> getAllRecipes() {
        return this.getRecipesFromCollection(this.nodes);
    }

    private List<CraftRecipe> getRecipesFromCollection(List<CraftRecipeListNode> nodes) {
        ArrayList<CraftRecipe> recipes = new ArrayList<CraftRecipe>();
        for (CraftRecipeListNode node : nodes) {
            switch (node.getType()) {
                case GROUP: {
                    recipes.addAll(this.getRecipesFromCollection(node.getChildren()));
                    break;
                }
                case RECIPE: {
                    recipes.add(node.getRecipe());
                }
            }
        }
        return recipes;
    }

    private static class RecipeNodeComparator
    implements Comparator<CraftRecipeListNode> {
        private final Comparator<? super CraftRecipe> recipeComparator;

        public RecipeNodeComparator(Comparator<? super CraftRecipe> recipeComparator) {
            this.recipeComparator = recipeComparator;
        }

        @Override
        public int compare(CraftRecipeListNode v1, CraftRecipeListNode v2) {
            if (v1 == null && v2 == null) {
                return 0;
            }
            if (v1 == null) {
                return 1;
            }
            if (v2 == null) {
                return -1;
            }
            CraftRecipe bestV1 = null;
            CraftRecipe bestV2 = null;
            if (v1.getType() == CraftRecipeListNode.CraftRecipeListNodeType.RECIPE) {
                bestV1 = v1.recipe;
            } else if (!v1.children.isEmpty()) {
                v1.children.sort(this);
                bestV1 = ((CraftRecipeListNode)v1.children.getFirst()).recipe;
            }
            if (v2.getType() == CraftRecipeListNode.CraftRecipeListNodeType.RECIPE) {
                bestV2 = v2.recipe;
            } else if (!v2.children.isEmpty()) {
                v2.children.sort(this);
                bestV2 = ((CraftRecipeListNode)v2.children.getFirst()).recipe;
            }
            return this.recipeComparator.compare(bestV1, bestV2);
        }
    }
}

