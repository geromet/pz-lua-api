/*
 * Decompiled with CFR 0.152.
 */
package zombie.entity;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.Objects;
import zombie.UsedFromLua;
import zombie.core.logger.ExceptionLogger;
import zombie.core.properties.PropertyContainer;
import zombie.debug.DebugLog;
import zombie.entity.Component;
import zombie.entity.ComponentType;
import zombie.entity.GameEntity;
import zombie.entity.GameEntityException;
import zombie.entity.GameEntityType;
import zombie.entity.components.script.EntityScriptInfo;
import zombie.entity.components.spriteconfig.SpriteConfig;
import zombie.inventory.InventoryItem;
import zombie.inventory.types.Moveable;
import zombie.iso.IsoObject;
import zombie.iso.SpriteDetails.IsoFlagType;
import zombie.scripting.ScriptManager;
import zombie.scripting.entity.ComponentScript;
import zombie.scripting.entity.GameEntityScript;
import zombie.scripting.entity.components.spriteconfig.SpriteConfigScript;
import zombie.scripting.objects.Item;
import zombie.vehicles.VehiclePart;

@UsedFromLua
public class GameEntityFactory {
    public static void TransferComponents(GameEntity source2, GameEntity target) {
        if (!source2.hasComponents()) {
            return;
        }
        try {
            Component component;
            int i;
            Moveable moveable;
            SpriteConfig spriteConfig;
            if (source2.hasComponent(ComponentType.SpriteConfig) && (spriteConfig = source2.getSpriteConfig()).isValidMultiSquare()) {
                DebugLog.General.warn("Cannot transfer components for multi-square objects.");
                return;
            }
            if (source2 instanceof Moveable && (moveable = (Moveable)source2).getSpriteGrid() != null && (moveable.getSpriteGrid().getWidth() > 1 || moveable.getSpriteGrid().getHeight() > 1)) {
                DebugLog.General.warn("Cannot transfer components for multi-square objects.");
                return;
            }
            ArrayList<Component> comps = new ArrayList<Component>();
            for (i = 0; i < source2.componentSize(); ++i) {
                component = source2.getComponentForIndex(i);
                comps.add(component);
            }
            for (i = 0; i < comps.size(); ++i) {
                component = (Component)comps.get(i);
                source2.removeComponent(component);
                if (target.hasComponent(component.getComponentType())) {
                    target.releaseComponent(component.getComponentType());
                }
                target.addComponent(component);
            }
            comps.clear();
            target.connectComponents();
        }
        catch (Exception e) {
            ExceptionLogger.logException(e);
        }
    }

    public static void TransferComponent(GameEntity source2, GameEntity target, ComponentType componentType) {
        try {
            Object component = source2.getComponent(componentType);
            source2.removeComponent((Component)component);
            if (target.hasComponent(componentType)) {
                target.releaseComponent(componentType);
            }
            target.addComponent((Component)component);
            target.connectComponents();
        }
        catch (Exception e) {
            ExceptionLogger.logException(e);
        }
    }

    public static void CreateIsoEntityFromCellLoading(IsoObject isoObject) {
        PropertyContainer props = isoObject.getProperties();
        if (!(props != null && (props.has(IsoFlagType.EntityScript) || props.has("IsMoveAble") && props.has("CustomItem")))) {
            return;
        }
        try {
            boolean isCustomItem;
            boolean isEntityScript = props.has(IsoFlagType.EntityScript);
            boolean bl = isCustomItem = props.has("IsMoveAble") && props.has("CustomItem");
            if (isEntityScript && isCustomItem) {
                DebugLog.General.warn("Entity has custom item '" + props.has("CustomItem") + "' set, and entity script '" + props.get("EntityScriptName") + "' defined");
            }
            if (isCustomItem) {
                GameEntityFactory.createIsoEntityFromCustomItem(isoObject, props.get("CustomItem"), true);
            } else {
                GameEntityFactory.createEntity(isoObject, true);
            }
        }
        catch (Exception e) {
            ExceptionLogger.logException(e);
        }
    }

    private static void createIsoEntityFromCustomItem(IsoObject isoObject, String customItem, boolean isFirstTimeCreated) throws Exception {
        Item scriptItem = ScriptManager.instance.FindItem(customItem);
        if (scriptItem != null) {
            GameEntityFactory.createEntity(isoObject, scriptItem, isFirstTimeCreated);
        } else {
            DebugLog.General.warn("Custom '" + customItem + "' item not found.");
        }
    }

    public static void CreateInventoryItemEntity(InventoryItem inventoryItem, Item itemScript, boolean isFirstTimeCreated) {
        if (itemScript == null || !itemScript.hasComponents()) {
            return;
        }
        try {
            GameEntityFactory.createEntity(inventoryItem, itemScript, isFirstTimeCreated);
        }
        catch (Exception e) {
            ExceptionLogger.logException(e);
        }
    }

    public static void CreateIsoObjectEntity(IsoObject isoObject, GameEntityScript script, boolean isFirstTimeCreated) {
        try {
            GameEntityFactory.createEntity(isoObject, script, isFirstTimeCreated);
        }
        catch (Exception e) {
            ExceptionLogger.logException(e);
        }
    }

    public static void CreateEntityDebugReload(GameEntity entity, GameEntityScript script, boolean isFirstTimeCreated) {
        try {
            GameEntityFactory.createEntity(entity, script, isFirstTimeCreated);
        }
        catch (Exception e) {
            ExceptionLogger.logException(e);
        }
    }

    private static void createEntity(GameEntity entity, boolean isFirstTimeCreated) throws GameEntityException {
        entity = Objects.requireNonNull(entity);
        GameEntityScript script = null;
        if (entity instanceof IsoObject) {
            IsoObject isoObject = (IsoObject)entity;
            PropertyContainer props = isoObject.getProperties();
            if (props != null && props.has(IsoFlagType.EntityScript)) {
                String scriptKey = props.get("EntityScriptName");
                if (scriptKey != null) {
                    script = ScriptManager.instance.getGameEntityScript(scriptKey);
                }
                if (script == null) {
                    throw new GameEntityException("EntityScript not found, script: " + (scriptKey != null ? scriptKey : "unknown"), entity);
                }
            }
        } else if (entity instanceof InventoryItem) {
            InventoryItem inventoryItem = (InventoryItem)entity;
            script = inventoryItem.getScriptItem();
        } else {
            if (entity instanceof VehiclePart) {
                throw new GameEntityException("Not implemented yet");
            }
            throw new GameEntityException("Unsupported entity type.");
        }
        if (script != null) {
            GameEntityFactory.createEntity(entity, script, isFirstTimeCreated);
        }
    }

    private static void createEntity(GameEntity entity, GameEntityScript script, boolean isFirstTimeCreated) throws GameEntityException {
        entity = Objects.requireNonNull(entity);
        script = Objects.requireNonNull(script);
        if (entity.hasComponents()) {
            throw new GameEntityException("Calling CreateEntity on entity that already has components.", entity);
        }
        GameEntityFactory.instanceComponents(entity, script);
        EntityScriptInfo scriptComponent = (EntityScriptInfo)ComponentType.Script.CreateComponent();
        scriptComponent.setOriginalScript(script);
        entity.addComponent(scriptComponent);
        entity.connectComponents();
        if (isFirstTimeCreated) {
            entity.onFirstCreation();
        }
    }

    private static void instanceComponents(GameEntity entity, GameEntityScript script) {
        boolean isIso;
        boolean bl = isIso = entity.getGameEntityType() == GameEntityType.IsoObject;
        if (isIso && script.containsComponent(ComponentType.SpriteConfig)) {
            SpriteConfigScript spriteScript = (SpriteConfigScript)script.getComponentScriptFor(ComponentType.SpriteConfig);
            SpriteConfig spriteConfig = (SpriteConfig)spriteScript.type.CreateComponentFromScript(spriteScript);
            entity.addComponent(spriteConfig);
            boolean isMaster = spriteConfig.isMultiSquareMaster();
            for (int i = 0; i < script.getComponentScripts().size(); ++i) {
                ComponentScript componentScript = script.getComponentScripts().get(i);
                if (componentScript.type == ComponentType.SpriteConfig || !isMaster && componentScript.isoMasterOnly()) continue;
                Component component = componentScript.type.CreateComponentFromScript(componentScript);
                entity.addComponent(component);
            }
        } else {
            for (int i = 0; i < script.getComponentScripts().size(); ++i) {
                ComponentScript componentScript = script.getComponentScripts().get(i);
                Component component = componentScript.type.CreateComponentFromScript(componentScript);
                entity.addComponent(component);
            }
        }
    }

    public static void RemoveComponentType(GameEntity entity, ComponentType componentType) {
        if (entity.hasComponent(componentType)) {
            entity.releaseComponent(componentType);
        }
        entity.connectComponents();
    }

    public static void RemoveComponentTypes(GameEntity entity, EnumSet<ComponentType> componentTypes) {
        for (ComponentType type : componentTypes) {
            if (!entity.hasComponent(type)) continue;
            entity.releaseComponent(type);
        }
        entity.connectComponents();
    }

    public static void RemoveComponent(GameEntity entity, Component component) {
        if (entity.containsComponent(component)) {
            entity.releaseComponent(component);
        }
        entity.connectComponents();
    }

    public static void RemoveComponents(GameEntity entity, Component ... components) {
        if (components == null || components.length == 0) {
            return;
        }
        for (Component component : components) {
            entity.releaseComponent(component);
        }
        entity.connectComponents();
    }

    public static void AddComponent(GameEntity entity, Component component) {
        GameEntityFactory.AddComponent(entity, true, component);
    }

    public static void AddComponents(GameEntity entity, Component ... components) {
        GameEntityFactory.AddComponents(entity, true, components);
    }

    public static void AddComponent(GameEntity entity, boolean replace, Component component) {
        if (entity.hasComponent(component.getComponentType())) {
            if (replace) {
                entity.releaseComponent(component.getComponentType());
            } else {
                return;
            }
        }
        entity.addComponent(component);
        entity.connectComponents();
    }

    public static void AddComponents(GameEntity entity, boolean replace, Component ... components) {
        if (components == null || components.length == 0) {
            return;
        }
        for (Component component : components) {
            if (entity.hasComponent(component.getComponentType())) {
                if (!replace) continue;
                entity.releaseComponent(component.getComponentType());
            }
            entity.addComponent(component);
        }
        entity.connectComponents();
    }
}

