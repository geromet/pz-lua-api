/*
 * Decompiled with CFR 0.152.
 */
package zombie.entity.components.crafting;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedDeque;
import zombie.UsedFromLua;
import zombie.characters.IsoPlayer;
import zombie.core.Core;
import zombie.debug.DebugLog;
import zombie.entity.GameEntity;
import zombie.entity.MetaEntity;
import zombie.entity.components.crafting.CraftRecipeMonitor;
import zombie.entity.components.crafting.recipe.CraftRecipeData;
import zombie.entity.components.fluids.Fluid;
import zombie.entity.components.resources.Resource;
import zombie.entity.components.resources.ResourceEnergy;
import zombie.entity.components.resources.ResourceFluid;
import zombie.entity.components.resources.ResourceIO;
import zombie.entity.components.resources.ResourceType;
import zombie.entity.energy.Energy;
import zombie.inventory.InventoryItem;
import zombie.iso.IsoGridSquare;
import zombie.iso.weather.ClimateManager;
import zombie.scripting.entity.components.crafting.CraftRecipe;
import zombie.scripting.objects.Item;

@UsedFromLua
public class CraftUtil {
    private static final ConcurrentLinkedDeque<ArrayList<Resource>> resource_list_pool = new ConcurrentLinkedDeque();

    public static ArrayList<Resource> AllocResourceList() {
        ArrayList<Resource> list = resource_list_pool.poll();
        if (list == null) {
            list = new ArrayList();
        }
        return list;
    }

    public static void ReleaseResourceList(ArrayList<Resource> list) {
        if (list == null) {
            return;
        }
        list.clear();
        assert (!Core.debug || !resource_list_pool.contains(list)) : "Object already in pool.";
        resource_list_pool.offer(list);
    }

    public static boolean canItemsStack(InventoryItem item, InventoryItem other) {
        return CraftUtil.canItemsStack(item, other, false);
    }

    public static boolean canItemsStack(InventoryItem item, InventoryItem other, boolean nullReturn) {
        if (item == null || other == null) {
            return nullReturn;
        }
        if (item == other) {
            return false;
        }
        return item.getRegistry_id() == other.getRegistry_id();
    }

    public static boolean canItemsStack(Item item, Item other, boolean nullReturn) {
        if (item == null || other == null) {
            return nullReturn;
        }
        if (item == other) {
            return true;
        }
        return item.getRegistry_id() == other.getRegistry_id();
    }

    public static Resource findResourceOrEmpty(ResourceIO resourceIO, List<Resource> outputResources, InventoryItem item, int count, Resource ignoreResource, HashSet<Resource> ignoreSet) {
        return CraftUtil.findResourceOrEmpty(resourceIO, outputResources, item.getScriptItem(), count, ignoreResource, ignoreSet);
    }

    public static Resource findResourceOrEmpty(ResourceIO resourceIO, List<Resource> resources, Item item, int count, Resource ignoreResource, HashSet<Resource> ignoreSet) {
        Resource bestFit = null;
        if (resources != null && !resources.isEmpty()) {
            for (int i = 0; i < resources.size(); ++i) {
                Resource resource = resources.get(i);
                if (ignoreResource != null && ignoreResource == resource || ignoreSet != null && ignoreSet.contains(resource)) continue;
                if (resourceIO != ResourceIO.Any && resource.getIO() != ResourceIO.Output) {
                    if (!Core.debug) continue;
                    DebugLog.General.warn("resource passed does not match selected IO type.");
                    continue;
                }
                if (resource.getType() != ResourceType.Item || resource.isFull() || count > 0 && resource.getFreeItemCapacity() < count) continue;
                if (resource.isEmpty() && bestFit == null) {
                    bestFit = resource;
                }
                if (!resource.canStackItem(item) || bestFit != null && !bestFit.isEmpty() && bestFit.getFreeItemCapacity() < resource.getFreeItemCapacity()) continue;
                bestFit = resource;
            }
        }
        return bestFit;
    }

    public static boolean canResourceFitItem(Resource resource, InventoryItem item) {
        return CraftUtil.canResourceFitItem(resource, item, 1, null, null);
    }

    public static boolean canResourceFitItem(Resource resource, InventoryItem item, int count) {
        return CraftUtil.canResourceFitItem(resource, item, count, null, null);
    }

    public static boolean canResourceFitItem(Resource resource, InventoryItem item, int count, Resource ignoreResource, HashSet<Resource> ignoreSet) {
        if (resource == null || resource.getType() != ResourceType.Item || resource.isFull()) {
            return false;
        }
        if (ignoreResource != null && ignoreResource == resource) {
            return false;
        }
        if (ignoreSet != null && ignoreSet.contains(resource)) {
            return false;
        }
        if (count > 0 && resource.getFreeItemCapacity() < count) {
            return false;
        }
        return resource.canStackItem(item);
    }

    public static boolean canResourceFitItem(Resource resource, Item item) {
        return CraftUtil.canResourceFitItem(resource, item, 1, null, null);
    }

    public static boolean canResourceFitItem(Resource resource, Item item, int count) {
        return CraftUtil.canResourceFitItem(resource, item, count, null, null);
    }

    public static boolean canResourceFitItem(Resource resource, Item item, int count, Resource ignoreResource, HashSet<Resource> ignoreSet) {
        if (resource == null || resource.getType() != ResourceType.Item || resource.isFull()) {
            return false;
        }
        if (ignoreResource != null && ignoreResource == resource) {
            return false;
        }
        if (ignoreSet != null && ignoreSet.contains(resource)) {
            return false;
        }
        if (count > 0 && resource.getFreeItemCapacity() < count) {
            return false;
        }
        return resource.canStackItem(item);
    }

    public static Resource findResourceOrEmpty(ResourceIO resourceIO, List<Resource> resources, Fluid fluid, float amount, Resource ignoreResource, HashSet<Resource> ignoreSet) {
        float bestRatio = 0.0f;
        Resource secondary = null;
        Resource bestFit = null;
        if (resources != null && !resources.isEmpty()) {
            for (int i = 0; i < resources.size(); ++i) {
                float ratio;
                ResourceFluid resourceFluid;
                Resource resource = resources.get(i);
                if (ignoreResource != null && ignoreResource == resource || ignoreSet != null && ignoreSet.contains(resource)) continue;
                if (resourceIO != ResourceIO.Any && resource.getIO() != ResourceIO.Output) {
                    if (!Core.debug) continue;
                    DebugLog.General.warn("resource passed does not match selected IO type.");
                    continue;
                }
                if (resource.getType() != ResourceType.Fluid || resource.isFull() || amount > 0.0f && resource.getFreeFluidCapacity() < amount) continue;
                if (resource.isEmpty() && secondary == null) {
                    secondary = resource;
                }
                if (!(resourceFluid = (ResourceFluid)resource).getFluidContainer().canAddFluid(fluid)) continue;
                if (resourceFluid.getFluidContainer().isPureFluid(fluid)) {
                    if (bestFit != null && bestFit.getFreeFluidCapacity() < resource.getFreeFluidCapacity()) continue;
                    bestFit = resource;
                }
                if (bestFit != null || !resourceFluid.getFluidContainer().contains(fluid) || (ratio = resourceFluid.getFluidContainer().getRatioForFluid(fluid)) < bestRatio) continue;
                bestRatio = ratio;
                secondary = resource;
            }
        }
        return bestFit != null ? bestFit : secondary;
    }

    public static Resource findResourceOrEmpty(ResourceIO resourceIO, List<Resource> resources, Energy energy, float amount, Resource ignoreResource, HashSet<Resource> ignoreSet) {
        Resource bestFit = null;
        if (resources != null && !resources.isEmpty()) {
            for (int i = 0; i < resources.size(); ++i) {
                ResourceEnergy resourceEnergy;
                Resource resource = resources.get(i);
                if (ignoreResource != null && ignoreResource == resource || ignoreSet != null && ignoreSet.contains(resource)) continue;
                if (resourceIO != ResourceIO.Any && resource.getIO() != ResourceIO.Output) {
                    if (!Core.debug) continue;
                    DebugLog.General.warn("resource passed does not match selected IO type.");
                    continue;
                }
                if (resource.getType() != ResourceType.Energy || resource.isFull() || amount > 0.0f && resource.getFreeEnergyCapacity() < amount || (resourceEnergy = (ResourceEnergy)resource).getEnergy() != energy) continue;
                if (resource.isEmpty() && bestFit == null) {
                    bestFit = resource;
                }
                if (bestFit != null && !bestFit.isEmpty() && bestFit.getFreeEnergyCapacity() < resource.getFreeEnergyCapacity()) continue;
                bestFit = resource;
            }
        }
        return bestFit;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public static CraftRecipeMonitor debugCanStart(IsoPlayer player, CraftRecipeData craftTestData, List<CraftRecipe> recipes, List<Resource> inputs, List<Resource> outputs, CraftRecipeMonitor monitor) {
        try {
            craftTestData.setMonitor(monitor);
            CraftUtil.canStart(craftTestData, recipes, inputs, outputs, monitor);
            craftTestData.setMonitor(null);
            CraftRecipeMonitor craftRecipeMonitor = monitor.seal();
            return craftRecipeMonitor;
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        finally {
            craftTestData.setMonitor(null);
        }
        return null;
    }

    public static boolean canStart(CraftRecipeData craftTestData, List<CraftRecipe> recipes, List<Resource> inputs, List<Resource> outputs) {
        return CraftUtil.canStart(craftTestData, recipes, inputs, outputs, null);
    }

    public static boolean canStart(CraftRecipeData craftTestData, List<CraftRecipe> recipes, List<Resource> inputs, List<Resource> outputs, CraftRecipeMonitor monitor) {
        if (monitor != null) {
            monitor.log("starting craftProcessor 'can start test'...");
        }
        CraftRecipe recipe = CraftUtil.getPossibleRecipe(craftTestData, recipes, inputs, outputs, monitor);
        if (monitor != null) {
            if (recipe != null) {
                monitor.success("selected recipe: " + recipe.getScriptObjectFullType());
                monitor.setRecipe(recipe);
                monitor.logRecipe(recipe, false);
            } else {
                monitor.warn("no recipe can be performed for this craftProcessor");
            }
        }
        return recipe != null;
    }

    public static boolean canPerformRecipe(CraftRecipe recipe, CraftRecipeData craftTestData, List<Resource> inputs, List<Resource> outputs) {
        return CraftUtil.canPerformRecipe(recipe, craftTestData, inputs, outputs, null);
    }

    public static boolean canPerformRecipe(CraftRecipe recipe, CraftRecipeData craftTestData, List<Resource> inputs, List<Resource> outputs, CraftRecipeMonitor monitor) {
        if (recipe == null || craftTestData == null || inputs == null) {
            return false;
        }
        craftTestData.setRecipe(recipe);
        if (craftTestData.canConsumeInputs(inputs) && (outputs == null || craftTestData.canCreateOutputs(outputs))) {
            if (monitor != null) {
                monitor.log("Input and Output passed, Calling LuaTest...");
            }
            boolean success = craftTestData.luaCallOnTest();
            if (monitor != null) {
                if (success) {
                    monitor.success("LuaTest: OK");
                } else {
                    monitor.warn("LuaTest: FAILED");
                }
            }
            return success;
        }
        return false;
    }

    public static CraftRecipe getPossibleRecipe(CraftRecipeData craftTestData, List<CraftRecipe> recipes, List<Resource> inputs, List<Resource> outputs) {
        return CraftUtil.getPossibleRecipe(craftTestData, recipes, inputs, outputs, null);
    }

    public static CraftRecipe getPossibleRecipe(CraftRecipeData craftTestData, List<CraftRecipe> recipes, List<Resource> inputs, List<Resource> outputs, CraftRecipeMonitor monitor) {
        if (craftTestData == null || recipes == null || inputs == null) {
            return null;
        }
        if (monitor != null) {
            monitor.log("Get possible recipe...");
            monitor.open();
        }
        for (int i = 0; i < recipes.size(); ++i) {
            CraftRecipe recipe = recipes.get(i);
            if (monitor != null) {
                monitor.log("[" + i + "] test recipe = " + recipe.getScriptObjectFullType());
            }
            if (!CraftUtil.canPerformRecipe(recipe, craftTestData, inputs, outputs, monitor)) continue;
            if (monitor != null) {
                monitor.close();
            }
            return recipe;
        }
        if (monitor != null) {
            monitor.close();
        }
        return null;
    }

    public static float getEntityTemperature(GameEntity entity) {
        float ambientTemp = ClimateManager.getInstance().getTemperature();
        if (entity instanceof MetaEntity) {
            return ambientTemp;
        }
        IsoGridSquare square = entity.getSquare();
        if (square == null) {
            return ambientTemp;
        }
        return ClimateManager.getInstance().getAirTemperatureForSquare(square);
    }
}

