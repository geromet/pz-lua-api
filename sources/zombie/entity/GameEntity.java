/*
 * Decompiled with CFR 0.152.
 */
package zombie.entity;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import zombie.GameWindow;
import zombie.UsedFromLua;
import zombie.characters.IsoPlayer;
import zombie.core.Core;
import zombie.core.Translator;
import zombie.core.logger.ExceptionLogger;
import zombie.core.math.PZMath;
import zombie.core.network.ByteBufferReader;
import zombie.core.raknet.UdpConnection;
import zombie.core.utils.ByteBlock;
import zombie.debug.DebugLog;
import zombie.debug.objects.DebugClassFields;
import zombie.debug.objects.DebugMethod;
import zombie.debug.objects.DebugNonRecursive;
import zombie.entity.Component;
import zombie.entity.ComponentContainer;
import zombie.entity.ComponentOperationHandler;
import zombie.entity.ComponentType;
import zombie.entity.GameEntityManager;
import zombie.entity.GameEntityNetwork;
import zombie.entity.GameEntityType;
import zombie.entity.components.attributes.AttributeContainer;
import zombie.entity.components.combat.Durability;
import zombie.entity.components.fluids.FluidContainer;
import zombie.entity.components.script.EntityScriptInfo;
import zombie.entity.components.spriteconfig.SpriteConfig;
import zombie.entity.components.ui.UiConfig;
import zombie.entity.events.ComponentEvent;
import zombie.entity.events.ComponentEventType;
import zombie.entity.events.EntityEvent;
import zombie.entity.events.EntityEventType;
import zombie.entity.network.EntityPacketData;
import zombie.entity.network.EntityPacketType;
import zombie.entity.util.BitSet;
import zombie.inventory.InventoryItem;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoObject;
import zombie.iso.objects.IsoFeedingTrough;
import zombie.network.GameClient;
import zombie.network.GameServer;
import zombie.network.IConnection;
import zombie.scripting.entity.GameEntityScript;
import zombie.scripting.objects.Item;
import zombie.vehicles.VehiclePart;

@DebugClassFields
@UsedFromLua
public abstract class GameEntity {
    public static final String DEFAULT_ENTITY_DISPLAY_NAME = "EC_Entity_DisplayName_Default";
    private ComponentContainer components;
    private boolean addedToWorldOrEquipped;
    boolean addedToEntityManager;
    private static final BitSet dummyBits = new BitSet();
    boolean addedToEngine;
    boolean removingFromEngine;
    boolean scheduledDelayedAddToEngine;
    boolean scheduledForEngineRemoval;
    boolean scheduledForBucketUpdate;
    @DebugNonRecursive
    private IsoPlayer usingPlayer;
    private static final boolean IOverbose = false;

    public static String getDefaultEntityDisplayName() {
        return DEFAULT_ENTITY_DISPLAY_NAME;
    }

    @DebugMethod
    @DebugNonRecursive
    public abstract GameEntityType getGameEntityType();

    @DebugMethod
    @DebugNonRecursive
    public abstract IsoGridSquare getSquare();

    @DebugMethod
    public abstract long getEntityNetID();

    @DebugMethod
    public abstract float getX();

    @DebugMethod
    public abstract float getY();

    @DebugMethod
    public abstract float getZ();

    @DebugMethod
    public final int getXi() {
        return PZMath.fastfloor(this.getX());
    }

    @DebugMethod
    public final int getYi() {
        return PZMath.fastfloor(this.getY());
    }

    @DebugMethod
    public final int getZi() {
        return PZMath.fastfloor(this.getZ());
    }

    @DebugMethod
    public abstract boolean isEntityValid();

    public boolean isValidEngineEntity() {
        return this.addedToEngine && !this.scheduledForEngineRemoval && !this.removingFromEngine;
    }

    @DebugMethod
    public boolean isMeta() {
        return false;
    }

    public boolean isOutside() {
        if (this.getSquare() == null) {
            return false;
        }
        return this.getSquare().isOutside();
    }

    @DebugMethod
    public String getEntityDisplayName() {
        if (this.hasComponent(ComponentType.UiConfig)) {
            return ((UiConfig)this.getComponent(ComponentType.UiConfig)).getEntityDisplayName();
        }
        return Translator.getEntityText(DEFAULT_ENTITY_DISPLAY_NAME);
    }

    @DebugMethod
    public String getEntityFullTypeDebug() {
        EntityScriptInfo script;
        if (this.hasComponent(ComponentType.Script) && (script = (EntityScriptInfo)this.getComponent(ComponentType.Script)) != null && script.getOriginalScript() != null) {
            return script.getOriginalScript();
        }
        return "<anonymous>";
    }

    public GameEntityScript getEntityScript() {
        EntityScriptInfo script;
        if (this.hasComponent(ComponentType.Script) && (script = (EntityScriptInfo)this.getComponent(ComponentType.Script)) != null && script.getScript() != null) {
            return script.getScript();
        }
        return null;
    }

    public final String getExceptionCompatibleString() {
        GameEntity gameEntity = this;
        if (gameEntity instanceof InventoryItem) {
            InventoryItem item = (InventoryItem)gameEntity;
            if (item.getScriptItem() != null) {
                Item i = item.getScriptItem();
                return "ITEM_ENTITY[id=" + i.getRegistry_id() + ", name=" + i.getName() + ", type=" + i.getScriptObjectFullType() + ", vanilla=" + item.isVanilla() + "]";
            }
        } else {
            if (this instanceof IsoObject) {
                return "ISO_ENTITY[anonymous]";
            }
            if (this instanceof VehiclePart) {
                return "V_PART_ENTITY[anonymous]";
            }
        }
        return "ENTITY[anonymous]";
    }

    public final AttributeContainer attrib() {
        return (AttributeContainer)this.getComponent(ComponentType.Attributes);
    }

    public final AttributeContainer getAttributes() {
        return (AttributeContainer)this.getComponent(ComponentType.Attributes);
    }

    public final FluidContainer getFluidContainer() {
        return (FluidContainer)this.getComponent(ComponentType.FluidContainer);
    }

    public final Durability getDurabilityComponent() {
        return (Durability)this.getComponent(ComponentType.Durability);
    }

    public final SpriteConfig getSpriteConfig() {
        return (SpriteConfig)this.getComponent(ComponentType.SpriteConfig);
    }

    public final boolean isAddedToEngine() {
        return this.addedToEngine;
    }

    public final boolean isRemovingFromEngine() {
        return this.removingFromEngine;
    }

    public final boolean isScheduledForEngineRemoval() {
        return this.scheduledForEngineRemoval;
    }

    public final boolean isScheduledForBucketUpdate() {
        return this.scheduledForBucketUpdate;
    }

    BitSet getBucketBits() {
        if (this.components != null) {
            return this.components.getBucketBits();
        }
        DebugLog.General.error("Getting bucketBits while no Component data");
        if (Core.debug) {
            throw new RuntimeException();
        }
        dummyBits.clear();
        return dummyBits;
    }

    BitSet getComponentBits() {
        return this.components != null ? this.components.getComponentBits() : null;
    }

    public boolean hasRenderers() {
        return this.components != null && this.components.hasRenderers();
    }

    ComponentOperationHandler getComponentOperationHandler() {
        return this.components != null ? this.components.getComponentOperationHandler() : null;
    }

    void setComponentOperationHandler(ComponentOperationHandler handler) {
        if (this.components != null) {
            this.components.setComponentOperationHandler(handler);
        }
    }

    private void ensureComponents() {
        if (this.components == null) {
            this.components = ComponentContainer.Alloc(this);
        }
    }

    public final boolean hasComponents() {
        return this.components != null && !this.components.isEmpty();
    }

    final boolean addComponent(Component component) {
        if (component == null) {
            DebugLog.General.error("Trying to add 'null' component.");
            return false;
        }
        if (this.components != null && this.components.size() >= 127) {
            DebugLog.General.error("Maximum component size '127' for entity reached.");
            return false;
        }
        if (!this.hasComponent(component.getComponentType())) {
            this.ensureComponents();
            if (component.getOwner() != null) {
                DebugLog.General.warn("Component added has not been removed from previous Entity.");
                component.getOwner().removeComponent(component);
            }
            this.components.add(component);
            component.setOwner(this);
            component.onAddedToOwner();
            if (this.addedToWorldOrEquipped && !this.addedToEntityManager) {
                GameEntityManager.RegisterEntity(this);
            }
            return true;
        }
        DebugLog.General.error("Trying to add component but component type already exists: " + String.valueOf((Object)component.getComponentType()));
        return false;
    }

    final boolean releaseComponent(ComponentType componentType) {
        Component component = this.removeComponent(componentType);
        if (component != null) {
            ComponentType.ReleaseComponent(component);
            return true;
        }
        return false;
    }

    final boolean releaseComponent(Component component) {
        Component remove = this.removeComponent(component);
        if (remove != null) {
            ComponentType.ReleaseComponent(remove);
            return true;
        }
        return false;
    }

    final Component removeComponent(ComponentType componentType) {
        if (!this.hasComponents()) {
            DebugLog.General.error("Trying to remove component but has no components: " + String.valueOf((Object)componentType));
            return null;
        }
        Component removed = this.components.remove(componentType);
        if (removed != null) {
            removed.onRemovedFromOwner();
            removed.setOwner(null);
            return removed;
        }
        return null;
    }

    final Component removeComponent(Component component) {
        if (component == null) {
            DebugLog.General.error("Trying to remove 'null' component.");
            return null;
        }
        if (!this.hasComponents()) {
            DebugLog.General.error("Trying to remove component but has no components: " + String.valueOf((Object)component.getComponentType()));
            return null;
        }
        if (this.components.removeComponent(component)) {
            component.onRemovedFromOwner();
            component.setOwner(null);
            return component;
        }
        return null;
    }

    public final boolean hasComponent(ComponentType type) {
        if (type == null || !this.hasComponents()) {
            return false;
        }
        return this.components.contains(type);
    }

    public final boolean hasComponentAny(ComponentType ... types) {
        if (types == null || !this.hasComponents()) {
            return false;
        }
        for (ComponentType type : types) {
            if (!this.components.contains(type)) continue;
            return true;
        }
        return false;
    }

    public final int componentSize() {
        return this.hasComponents() ? this.components.size() : 0;
    }

    public final Component getComponentForIndex(int index) {
        if (!this.hasComponents() || index < 0 || index >= this.components.size()) {
            return null;
        }
        return this.components.getForIndex(index);
    }

    public final <T extends Component> T getComponent(ComponentType type) {
        if (type == null || !this.hasComponents()) {
            return null;
        }
        return (T)this.components.get(type);
    }

    public final <T extends Component> T getComponentAny(ComponentType ... types) {
        if (types == null || types.length == 0 || !this.hasComponents()) {
            return null;
        }
        for (ComponentType type : types) {
            if (!this.components.contains(type)) continue;
            return (T)this.components.get(type);
        }
        return null;
    }

    public final Component getComponentFromID(short id) {
        return this.getComponent(ComponentType.FromId(id));
    }

    public final boolean containsComponent(Component component) {
        if (this.hasComponents()) {
            return this.components.contains(component);
        }
        return false;
    }

    protected final void sendComponentEvent(Component sender, ComponentEventType eventType) {
        ComponentEvent event = ComponentEvent.Alloc(eventType, sender);
        this.sendComponentEvent(sender, event);
    }

    protected final void sendComponentEvent(Component sender, ComponentEvent event) {
        for (int i = 0; i < this.components.size(); ++i) {
            if (sender != null && this.components.getForIndex(i) == sender) continue;
            this.components.getForIndex(i).onComponentEvent(event);
        }
        event.release();
    }

    protected final void sendEntityEvent(EntityEventType eventType) {
        EntityEvent event = EntityEvent.Alloc(eventType, this);
        this.sendEntityEvent(event);
    }

    protected final void sendEntityEvent(EntityEvent event) {
        if (this.components != null) {
            for (int i = 0; i < this.components.size(); ++i) {
                this.components.getForIndex(i).onEntityEvent(event);
            }
        }
        event.release();
    }

    protected final void connectComponents() {
        if (!this.hasComponents()) {
            return;
        }
        for (int i = 0; i < this.components.size(); ++i) {
            this.components.getForIndex(i).onConnectComponents();
        }
    }

    protected final void onFirstCreation() {
        if (!this.hasComponents()) {
            return;
        }
        for (int i = 0; i < this.components.size(); ++i) {
            this.components.getForIndex(i).onFirstCreation();
        }
    }

    public void reset() {
        if (this.addedToEngine || this.addedToEntityManager) {
            if (Core.debug) {
                throw new IllegalStateException("Entity should be removed from Engine and Manager at this point.");
            }
            if (!GameEntityManager.isEngineProcessing()) {
                GameEntityManager.UnregisterEntity(this);
            } else {
                throw new IllegalStateException("Fatal entity state.");
            }
        }
        this.usingPlayer = null;
        this.addedToEngine = false;
        this.removingFromEngine = false;
        this.scheduledDelayedAddToEngine = false;
        this.scheduledForEngineRemoval = false;
        this.scheduledForBucketUpdate = false;
        this.addedToEntityManager = false;
        this.addedToWorldOrEquipped = false;
        if (this.components != null) {
            this.components.release();
            this.components = null;
        }
    }

    public void onEquip() {
        this.onEquip(true);
    }

    public void onEquip(boolean register) {
        if (this.getGameEntityType() == GameEntityType.InventoryItem) {
            if (register) {
                GameEntityManager.RegisterEntity(this);
            }
            this.addedToWorldOrEquipped = true;
        }
    }

    public void onUnEquip() {
        if (this.getGameEntityType() == GameEntityType.InventoryItem) {
            GameEntityManager.UnregisterEntity(this);
            this.addedToWorldOrEquipped = false;
        }
    }

    public void addToWorld() {
        if (this.getGameEntityType() == GameEntityType.InventoryItem) {
            return;
        }
        GameEntityManager.RegisterEntity(this);
        this.addedToWorldOrEquipped = true;
        this.sendEntityEvent(EntityEventType.AddedToWorld);
    }

    public void removeFromWorld() {
        this.removeFromWorld(false);
    }

    public final void removeFromWorld(boolean offloadEntityToMeta) {
        if (this.getGameEntityType() == GameEntityType.InventoryItem) {
            return;
        }
        GameEntityManager.UnregisterEntity(this, offloadEntityToMeta);
        this.addedToWorldOrEquipped = false;
    }

    public void renderlast() {
    }

    public void renderlastComponents() {
        if (!this.hasComponents()) {
            return;
        }
        this.components.render();
    }

    public final boolean requiresEntitySave() {
        return this.hasComponents();
    }

    public final void saveEntity(ByteBuffer output) throws IOException {
        output.put(this.hasComponents() ? (byte)this.components.size() : (byte)0);
        if (this.hasComponents()) {
            for (int i = 0; i < this.components.size(); ++i) {
                Component component = this.components.getForIndex(i);
                ByteBlock block = ByteBlock.Start(output, ByteBlock.Mode.Save);
                output.putShort(component.getComponentType().GetID());
                component.save(output);
                ByteBlock.End(output, block);
            }
        }
    }

    public final void loadEntity(ByteBuffer input, int worldVersion) throws IOException {
        this.loadEntity(input, worldVersion, null);
    }

    public final void loadEntity(ByteBuffer input, int worldVersion, List<Component> loaded) throws IOException {
        int componentCount;
        if (loaded != null) {
            loaded.clear();
        }
        if ((componentCount = input.get()) > 0) {
            for (int i = 0; i < componentCount; ++i) {
                ByteBlock block = ByteBlock.Start(input, ByteBlock.Mode.Load);
                block.safelyForceSkipOnEnd(true);
                try {
                    short componentID = input.getShort();
                    ComponentType componentType = ComponentType.FromId(componentID);
                    Object component = this.getComponent(componentType);
                    boolean componentExists = true;
                    if (component == null) {
                        component = componentType.CreateComponent();
                        componentExists = false;
                    }
                    if (component != null) {
                        ((Component)component).load(input, worldVersion);
                        if (!componentExists) {
                            this.addComponent((Component)component);
                        }
                        if (loaded != null) {
                            loaded.add((Component)component);
                        }
                    } else if (Core.debug) {
                        throw new IOException("Component with id '" + componentID + "' not found.");
                    }
                }
                catch (Exception e) {
                    ExceptionLogger.logException(e);
                }
                ByteBlock.End(input, block);
            }
            this.connectComponents();
        }
    }

    public boolean isUsingPlayer(IsoPlayer target) {
        if (this.getUsingPlayer() == null) {
            return false;
        }
        if (target == null) {
            return false;
        }
        return this.getUsingPlayer() == target;
    }

    public IsoPlayer getUsingPlayer() {
        return this.usingPlayer;
    }

    public void setUsingPlayer(IsoPlayer player) {
        if (this.usingPlayer != player) {
            this.usingPlayer = player;
            if ((GameClient.client || GameServer.server) && this.getGameEntityType() == GameEntityType.IsoObject) {
                this.sendUpdateUsingPlayer();
            }
        }
    }

    protected final void sendServerEntityPacketTo(IsoPlayer player, EntityPacketData data) {
        GameEntityNetwork.sendPacketDataTo(player, data, this, null);
    }

    protected final void sendClientEntityPacket(EntityPacketData data) {
        GameEntityNetwork.sendPacketData(data, this, null, null, true);
    }

    protected final void sendServerEntityPacket(EntityPacketData data, UdpConnection ignoreConnection) {
        GameEntityNetwork.sendPacketData(data, this, null, ignoreConnection, true);
    }

    protected final boolean onReceiveEntityPacket(ByteBufferReader input, EntityPacketType type, IConnection senderConnection) throws IOException {
        switch (type) {
            case UpdateUsingPlayer: {
                this.receiveUpdateUsingPlayer(input, senderConnection);
                return true;
            }
            case SyncGameEntity: {
                this.receiveSyncEntity(input, senderConnection);
                return true;
            }
            case RequestSyncGameEntity: {
                this.receiveRequestSyncGameEntity(input, senderConnection);
                return true;
            }
        }
        throw new IOException("Packet type not found: " + String.valueOf((Object)type));
    }

    protected final void sendUpdateUsingPlayer() {
        EntityPacketData data = GameEntityNetwork.createPacketData(EntityPacketType.UpdateUsingPlayer);
        data.bb.put(this.usingPlayer != null ? (byte)1 : 0);
        if (this.usingPlayer != null) {
            GameWindow.WriteString(data.bb, this.usingPlayer.getUsername());
            data.bb.putShort(this.usingPlayer.getOnlineID());
        }
        if (GameClient.client) {
            this.sendClientEntityPacket(data);
        } else {
            this.sendServerEntityPacket(data, null);
        }
    }

    protected final void receiveUpdateUsingPlayer(ByteBufferReader input, IConnection senderConnection) {
        if (input.getBoolean()) {
            String username = input.getUTF();
            short onlineID = input.getShort();
            if (GameClient.client) {
                this.usingPlayer = GameClient.instance.getPlayerByOnlineID(onlineID);
            } else if (GameServer.server) {
                IsoPlayer player = GameServer.getPlayerByUserName(username);
                if (this.usingPlayer == null) {
                    this.usingPlayer = player;
                }
            }
        } else {
            this.usingPlayer = null;
        }
        if (GameServer.server) {
            this.sendUpdateUsingPlayer();
        }
    }

    public final void sendSyncEntity(UdpConnection ignoreConnection) {
        if (!GameServer.server) {
            return;
        }
        EntityPacketData data = GameEntityNetwork.createPacketData(EntityPacketType.SyncGameEntity);
        try {
            data.bb.put(this.usingPlayer != null ? (byte)1 : 0);
            if (this.usingPlayer != null) {
                data.bb.putShort(this.usingPlayer.getOnlineID());
            }
            data.bb.put(this.hasComponents() ? (byte)this.components.size() : (byte)0);
            if (this.hasComponents()) {
                for (int i = 0; i < this.components.size(); ++i) {
                    Component component = this.components.getForIndex(i);
                    data.bb.putShort(component.getComponentType().GetID());
                    component.saveSyncData(data.bb);
                }
            }
        }
        catch (Exception e) {
            e.printStackTrace();
            EntityPacketData.release(data);
            return;
        }
        if (GameClient.client) {
            this.sendClientEntityPacket(data);
        } else {
            this.sendServerEntityPacket(data, ignoreConnection);
        }
    }

    public final void sendRequestSyncGameEntity() {
        if (!GameClient.client) {
            DebugLog.General.warn("Can only call on client.");
            return;
        }
        if (this.hasComponent(ComponentType.MetaTag)) {
            this.sendClientEntityPacket(GameEntityNetwork.createPacketData(EntityPacketType.RequestSyncGameEntity));
        }
    }

    protected final void receiveSyncEntity(ByteBufferReader input, IConnection senderConnection) {
        if (!GameClient.client) {
            DebugLog.General.warn("Can only call on client.");
            return;
        }
        try {
            if (input.getBoolean()) {
                short onlineID = input.getShort();
                this.usingPlayer = GameClient.instance.getPlayerByOnlineID(onlineID);
            } else {
                this.usingPlayer = null;
            }
            ArrayList<ComponentType> syncedTypes = new ArrayList<ComponentType>();
            int size = input.getByte();
            if (size > 0) {
                for (int i = 0; i < size; ++i) {
                    short componentID = input.getShort();
                    ComponentType type = ComponentType.FromId(componentID);
                    syncedTypes.add(type);
                    Object component = this.getComponent(type);
                    if (component == null) {
                        component = type.CreateComponent();
                        this.addComponent((Component)component);
                    }
                    ((Component)component).loadSyncData(input.bb);
                    ((Component)component).onAddedToOwner();
                }
            }
            int i = 0;
            while (i < this.components.size()) {
                Component component = this.components.getForIndex(i);
                ComponentType type = component.getComponentType();
                if (!syncedTypes.contains((Object)type)) {
                    this.removeComponent(component);
                    continue;
                }
                ++i;
            }
            GameEntity gameEntity = this;
            if (gameEntity instanceof IsoFeedingTrough) {
                IsoFeedingTrough feedingTrough = (IsoFeedingTrough)gameEntity;
                feedingTrough.checkContainer();
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    protected final void receiveRequestSyncGameEntity(ByteBufferReader input, IConnection senderConnection) {
        if (!GameServer.server) {
            return;
        }
        EntityPacketData data = GameEntityNetwork.createPacketData(EntityPacketType.SyncGameEntity);
        try {
            data.bb.put(this.usingPlayer != null ? (byte)1 : 0);
            if (this.usingPlayer != null) {
                data.bb.putShort(this.usingPlayer.getOnlineID());
            }
            data.bb.put(this.hasComponents() ? (byte)this.components.size() : (byte)0);
            if (this.hasComponents()) {
                for (int i = 0; i < this.components.size(); ++i) {
                    Component component = this.components.getForIndex(i);
                    data.bb.putShort(component.getComponentType().GetID());
                    component.saveSyncData(data.bb);
                }
            }
        }
        catch (Exception e) {
            e.printStackTrace();
            EntityPacketData.release(data);
            return;
        }
        GameEntityNetwork.sendPacketData(data, this, null, senderConnection, false);
    }

    public void onFluidContainerUpdate() {
    }
}

