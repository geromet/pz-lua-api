/*
 * Decompiled with CFR 0.152.
 */
package zombie.network.fields;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Objects;
import zombie.UsedFromLua;
import zombie.characters.IsoPlayer;
import zombie.core.network.ByteBufferReader;
import zombie.core.network.ByteBufferWriter;
import zombie.debug.DebugLog;
import zombie.inventory.InventoryItem;
import zombie.inventory.ItemContainer;
import zombie.inventory.types.InventoryContainer;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoObject;
import zombie.iso.IsoWorld;
import zombie.iso.objects.IsoDeadBody;
import zombie.iso.objects.IsoWorldInventoryObject;
import zombie.network.GameClient;
import zombie.network.GameServer;
import zombie.network.IConnection;
import zombie.network.JSONField;
import zombie.network.ServerMap;
import zombie.network.fields.INetworkPacketField;
import zombie.network.fields.character.PlayerID;
import zombie.vehicles.BaseVehicle;
import zombie.vehicles.VehicleManager;
import zombie.vehicles.VehiclePart;

@UsedFromLua
public class ContainerID
implements INetworkPacketField {
    @JSONField
    public final PlayerID playerId = new PlayerID();
    @JSONField
    public ContainerType containerType = ContainerType.Undefined;
    @JSONField
    public int x;
    @JSONField
    public int y;
    @JSONField
    public byte z;
    @JSONField
    short index = (short)-1;
    @JSONField
    short containerIndex = (short)-1;
    @JSONField
    short vid = (short)-1;
    @JSONField
    public int worldItemId = -1;
    @JSONField
    public int[] floorXY = null;
    ItemContainer container;
    IsoObject object;

    public ContainerType getContainerType() {
        return this.containerType;
    }

    public void set(ItemContainer container) {
        if (container == null) {
            this.containerType = ContainerType.Undefined;
            return;
        }
        IsoObject o = container.getParent();
        if (!(o instanceof IsoPlayer)) {
            if (container.getContainingItem() != null && container.getContainingItem().getWorldItem() != null) {
                o = container.getContainingItem().getWorldItem();
            }
            if (container.containingItem != null && container.containingItem.getOutermostContainer() != null) {
                ItemContainer outermost = container.containingItem.getOutermostContainer();
                o = outermost.getParent();
                if (o instanceof IsoPlayer) {
                    IsoPlayer player = (IsoPlayer)o;
                    this.setInventoryContainer(container, player);
                } else if (o instanceof BaseVehicle) {
                    this.setObjectInVehicle(container, o, o.square, outermost);
                } else {
                    this.setObject(container, o, o.square);
                }
                return;
            }
        }
        if (o == null) {
            if (container.getType().equals("floor")) {
                for (int n = 0; n < container.getItems().size(); ++n) {
                    InventoryItem inventoryItem = container.getItems().get(n);
                    if (inventoryItem.getWorldItem() == null || inventoryItem.getWorldItem().getSquare() == null) continue;
                    this.setFloor(container, inventoryItem.getWorldItem().getSquare());
                    break;
                }
                return;
            }
            throw new RuntimeException();
        }
        this.set(container, o);
    }

    public void copy(ContainerID other) {
        this.containerType = other.containerType;
        this.x = other.x;
        this.y = other.y;
        this.z = other.z;
        this.floorXY = other.floorXY == null ? null : Arrays.copyOf(other.floorXY, other.floorXY.length);
        this.index = other.index;
        this.containerIndex = other.containerIndex;
        this.vid = other.vid;
        this.worldItemId = other.worldItemId;
        this.playerId.copy(other.playerId);
        this.container = other.container;
        this.object = other.object;
    }

    public void setFloor(ItemContainer container, IsoGridSquare sq) {
        this.x = sq.getX();
        this.y = sq.getY();
        this.z = (byte)sq.getZ();
        this.calculateFloorXY(container);
        this.containerType = ContainerType.Floor;
        this.container = container;
    }

    public void setObject(ItemContainer container, IsoObject o, IsoGridSquare sq) {
        this.x = sq.getX();
        this.y = sq.getY();
        this.z = (byte)sq.getZ();
        this.container = container;
        if (o != null) {
            this.containerType = ContainerType.ObjectContainer;
            this.index = (short)o.square.getObjects().indexOf(o);
            this.containerIndex = (short)o.getContainerIndex(container);
        } else {
            this.containerType = ContainerType.IsoObject;
            this.index = (short)-1;
            this.containerIndex = (short)-1;
        }
    }

    public void setObjectInVehicle(ItemContainer container, IsoObject o, IsoGridSquare sq, ItemContainer part) {
        this.x = sq.getX();
        this.y = sq.getY();
        this.z = (byte)sq.getZ();
        this.container = container;
        this.containerType = ContainerType.ObjectInVehicle;
        this.index = (short)part.vehiclePart.getIndex();
        this.containerIndex = (short)part.getItems().indexOf(container.getContainingItem());
        this.vid = ((BaseVehicle)o).vehicleId;
    }

    public void setInventoryContainer(ItemContainer container, IsoPlayer player) {
        this.x = player.square.getX();
        this.y = player.square.getY();
        this.z = (byte)player.square.getZ();
        this.containerType = ContainerType.InventoryContainer;
        this.playerId.set(player);
        this.worldItemId = container.containingItem.id;
        this.container = container;
    }

    public void set(ItemContainer container, IsoObject o) {
        if (o.square != null) {
            this.x = o.square.getX();
            this.y = o.square.getY();
            this.z = (byte)o.square.getZ();
        }
        if (o instanceof IsoDeadBody) {
            this.containerType = ContainerType.DeadBody;
            this.index = (short)o.getStaticMovingObjectIndex();
        } else if (o instanceof IsoWorldInventoryObject) {
            IsoWorldInventoryObject isoWorldInventoryObject = (IsoWorldInventoryObject)o;
            this.containerType = ContainerType.WorldObject;
            this.worldItemId = isoWorldInventoryObject.getItem().id;
        } else if (o instanceof BaseVehicle) {
            BaseVehicle baseVehicle = (BaseVehicle)o;
            this.containerType = ContainerType.Vehicle;
            this.vid = baseVehicle.vehicleId;
            this.index = (short)container.vehiclePart.getIndex();
        } else if (o instanceof IsoPlayer) {
            IsoPlayer isoPlayer = (IsoPlayer)o;
            this.containerType = ContainerType.PlayerInventory;
            this.playerId.set(isoPlayer);
        } else if (o.getContainerIndex(container) != -1) {
            this.containerType = ContainerType.ObjectContainer;
            this.index = (short)o.square.getObjects().indexOf(o);
            this.containerIndex = (short)o.getContainerIndex(container);
        } else {
            this.containerType = ContainerType.IsoObject;
            this.index = (short)o.square.getObjects().indexOf(o);
            this.containerIndex = (short)-1;
        }
        this.container = container;
    }

    public boolean isContainerTheSame(int itemId, ItemContainer source2) {
        if ("floor".equals(source2.getType())) {
            if (ContainerType.WorldObject == this.containerType) {
                for (InventoryItem item : source2.getItems()) {
                    if (item.id != this.worldItemId) continue;
                    return true;
                }
            }
            return false;
        }
        return this.container == source2;
    }

    public ItemContainer getContainer() {
        return this.container;
    }

    public IsoObject getObject() {
        return this.object;
    }

    public VehiclePart getPart() {
        if (this.containerType != ContainerType.Vehicle) {
            return null;
        }
        BaseVehicle vehicle = VehicleManager.instance.getVehicleByID(this.vid);
        if (vehicle == null) {
            return null;
        }
        return vehicle.getPartByIndex(this.index);
    }

    public BaseVehicle getVehicle() {
        if (this.containerType != ContainerType.Vehicle) {
            return null;
        }
        return VehicleManager.instance.getVehicleByID(this.vid);
    }

    @Override
    public void parse(ByteBufferReader b, IConnection connection) {
        this.container = null;
        this.containerType = b.getEnum(ContainerType.class);
        if (this.containerType == ContainerType.Undefined) {
            return;
        }
        if (this.containerType == ContainerType.PlayerInventory) {
            this.playerId.parse(b, connection);
        } else if (this.containerType == ContainerType.InventoryContainer) {
            this.playerId.parse(b, connection);
            this.worldItemId = b.getInt();
        } else {
            int squareCount;
            this.x = b.getInt();
            this.y = b.getInt();
            this.z = b.getByte();
            if (this.containerType == ContainerType.DeadBody) {
                this.index = b.getShort();
            } else if (this.containerType == ContainerType.WorldObject) {
                this.worldItemId = b.getInt();
            } else if (this.containerType == ContainerType.IsoObject) {
                this.index = b.getShort();
                this.containerIndex = (short)-1;
            } else if (this.containerType == ContainerType.ObjectContainer) {
                this.index = b.getShort();
                this.containerIndex = b.getShort();
            } else if (this.containerType == ContainerType.Vehicle) {
                this.vid = b.getShort();
                this.index = b.getShort();
            } else if (this.containerType == ContainerType.ObjectInVehicle) {
                this.vid = b.getShort();
                this.index = b.getShort();
                this.containerIndex = b.getShort();
            } else if (this.containerType == ContainerType.Floor && (squareCount = b.getByte()) > 0) {
                this.floorXY = new int[squareCount * 2];
                for (int i = 0; i < squareCount; ++i) {
                    this.floorXY[i * 2] = b.getInt();
                    this.floorXY[i * 2 + 1] = b.getInt();
                }
            }
        }
        this.findObject();
    }

    @Override
    public void write(ByteBufferWriter b) {
        b.putEnum(this.containerType);
        if (this.containerType == ContainerType.Undefined) {
            return;
        }
        if (this.containerType == ContainerType.PlayerInventory) {
            this.playerId.write(b);
            return;
        }
        if (this.containerType == ContainerType.InventoryContainer) {
            this.playerId.write(b);
            b.putInt(this.worldItemId);
            return;
        }
        b.putInt(this.x);
        b.putInt(this.y);
        b.putByte(this.z);
        if (this.containerType == ContainerType.Floor) {
            if (this.floorXY == null || this.floorXY.length == 2) {
                b.putByte((byte)0);
            } else {
                b.putByte((byte)(this.floorXY.length / 2));
                for (int i = 0; i < this.floorXY.length; ++i) {
                    b.putInt(this.floorXY[i]);
                }
            }
        }
        if (this.containerType == ContainerType.DeadBody) {
            b.putShort(this.index);
        } else if (this.containerType == ContainerType.WorldObject) {
            b.putInt(this.worldItemId);
        } else if (this.containerType == ContainerType.Vehicle) {
            b.putShort(this.vid);
            b.putShort(this.index);
        } else if (this.containerType == ContainerType.IsoObject) {
            b.putShort(this.index);
        } else if (this.containerType == ContainerType.ObjectContainer) {
            b.putShort(this.index);
            b.putShort(this.containerIndex);
        } else if (this.containerType == ContainerType.ObjectInVehicle) {
            b.putShort(this.vid);
            b.putShort(this.index);
            b.putShort(this.containerIndex);
        }
    }

    public void write(ByteBuffer bb) {
        bb.put((byte)this.containerType.ordinal());
        if (this.containerType == ContainerType.Undefined) {
            return;
        }
        if (this.containerType == ContainerType.PlayerInventory) {
            this.playerId.write(bb);
            return;
        }
        if (this.containerType == ContainerType.InventoryContainer) {
            this.playerId.write(bb);
            bb.putInt(this.worldItemId);
            return;
        }
        bb.putInt(this.x);
        bb.putInt(this.y);
        bb.put(this.z);
        if (this.containerType == ContainerType.Floor) {
            if (this.floorXY == null || this.floorXY.length == 2) {
                bb.put((byte)0);
            } else {
                bb.put((byte)(this.floorXY.length / 2));
                for (int i = 0; i < this.floorXY.length; ++i) {
                    bb.putInt(this.floorXY[i]);
                }
            }
        }
        if (this.containerType == ContainerType.DeadBody) {
            bb.putShort(this.index);
        } else if (this.containerType == ContainerType.WorldObject) {
            bb.putInt(this.worldItemId);
        } else if (this.containerType == ContainerType.Vehicle) {
            bb.putShort(this.vid);
            bb.putShort(this.index);
        } else if (this.containerType == ContainerType.IsoObject) {
            bb.putShort(this.index);
        } else if (this.containerType == ContainerType.ObjectContainer) {
            bb.putShort(this.index);
            bb.putShort(this.containerIndex);
        } else if (this.containerType == ContainerType.ObjectInVehicle) {
            bb.putShort(this.vid);
            bb.putShort(this.index);
            bb.putShort(this.containerIndex);
        }
    }

    public void findObject() {
        if (this.containerType == ContainerType.PlayerInventory) {
            this.container = this.playerId.getPlayer().getInventory();
            return;
        }
        if (this.containerType == ContainerType.InventoryContainer) {
            ItemContainer playerContainer = this.playerId.getPlayer().getInventory();
            InventoryContainer containingItem = (InventoryContainer)playerContainer.getItemWithIDRecursiv(this.worldItemId);
            this.container = containingItem.getItemContainer();
            return;
        }
        if (IsoWorld.instance.currentCell == null) {
            return;
        }
        IsoGridSquare sq = IsoWorld.instance.currentCell.getGridSquare(this.x, this.y, this.z);
        if (GameServer.server && sq == null) {
            sq = ServerMap.instance.getGridSquare(this.x, this.y, this.z);
        }
        if (sq != null) {
            this.container = null;
            if (this.containerType == ContainerType.Floor) {
                this.object = new IsoWorldInventoryObject(null, sq, 0.0f, 0.0f, 0.0f);
                this.container = new ItemContainer("floor", sq, null);
                if (this.floorXY == null) {
                    this.collectFloorItems(sq);
                } else {
                    for (int i = 0; i < this.floorXY.length; i += 2) {
                        int floorX = this.floorXY[i];
                        int floorY = this.floorXY[i + 1];
                        IsoGridSquare floorSq = IsoWorld.instance.currentCell.getGridSquare(floorX, floorY, this.z);
                        this.collectFloorItems(floorSq);
                    }
                }
            } else if (this.containerType == ContainerType.DeadBody) {
                if (this.index < 0 || this.index >= sq.getStaticMovingObjects().size()) {
                    DebugLog.log("ERROR: ContainerID: invalid corpse index");
                    return;
                }
                this.object = sq.getStaticMovingObjects().get(this.index);
                if (this.object != null && this.object.getContainer() != null) {
                    this.container = this.object.getContainer();
                }
            } else if (this.containerType == ContainerType.WorldObject) {
                for (int i = 0; i < sq.getWorldObjects().size(); ++i) {
                    IsoWorldInventoryObject wo = sq.getWorldObjects().get(i);
                    if (wo == null || wo.getItem() == null || wo.getItem().id != this.worldItemId) continue;
                    this.object = wo;
                    InventoryItem item = wo.getItem();
                    if (item instanceof InventoryContainer) {
                        InventoryContainer inventoryContainer = (InventoryContainer)item;
                        this.container = inventoryContainer.getItemContainer();
                        break;
                    }
                    this.container = new ItemContainer("floor", sq, null);
                    this.container.getItems().add(item);
                    break;
                }
                if (this.container == null) {
                    DebugLog.log("ERROR: sendItemsToContainer: can't find world item with id=" + this.worldItemId);
                }
            } else if (this.containerType == ContainerType.IsoObject) {
                if (this.index < 0 || this.index >= sq.getObjects().size()) {
                    this.object = new IsoObject();
                    this.object.setSquare(sq);
                    this.container = new ItemContainer("object", sq, this.object);
                } else {
                    this.object = sq.getObjects().get(this.index);
                    this.container = this.object != null ? this.object.getContainerByIndex(this.containerIndex) : null;
                }
            } else if (this.containerType == ContainerType.ObjectContainer) {
                if (this.index < 0 || this.index >= sq.getObjects().size()) {
                    this.object = new IsoObject();
                    this.object.setSquare(sq);
                } else {
                    this.object = sq.getObjects().get(this.index);
                    this.container = this.object != null ? this.object.getContainerByIndex(this.containerIndex) : null;
                }
            } else if (this.containerType == ContainerType.Vehicle) {
                BaseVehicle vehicle = VehicleManager.instance.getVehicleByID(this.vid);
                if (vehicle == null) {
                    DebugLog.log("ERROR: sendItemsToContainer: invalid vehicle id");
                } else {
                    VehiclePart part = vehicle.getPartByIndex(this.index);
                    if (part == null) {
                        DebugLog.log("ERROR: sendItemsToContainer: invalid part index");
                    } else {
                        this.object = vehicle;
                        this.container = part.getItemContainer();
                        if (this.container == null) {
                            DebugLog.log("ERROR: sendItemsToContainer: part " + part.getId() + " has no container");
                        }
                    }
                }
            } else if (this.containerType == ContainerType.ObjectInVehicle) {
                BaseVehicle vehicle = VehicleManager.instance.getVehicleByID(this.vid);
                if (vehicle == null) {
                    DebugLog.log("ERROR: sendItemsToContainer: invalid vehicle id");
                } else {
                    VehiclePart part = vehicle.getPartByIndex(this.index);
                    if (part == null) {
                        DebugLog.log("ERROR: sendItemsToContainer: invalid part index");
                    } else {
                        InventoryItem item = part.getItemContainer().getItems().get(this.containerIndex);
                        if (item == null) {
                            DebugLog.log("ERROR: sendItemsToContainer: invalid item index");
                            return;
                        }
                        this.container = ((InventoryContainer)item).getItemContainer();
                        if (this.container == null) {
                            DebugLog.log("ERROR: sendItemsToContainer: item " + item.getID() + " has no container");
                        }
                    }
                }
            } else {
                DebugLog.log("ERROR: sendItemsToContainer: unknown container type");
            }
        } else if (GameClient.client) {
            GameClient.instance.delayPacket(this.x, this.y, this.z);
        }
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || this.getClass() != o.getClass()) {
            return false;
        }
        ContainerID that = (ContainerID)o;
        if (this.containerType == ContainerType.InventoryContainer || this.containerType == ContainerType.PlayerInventory) {
            return this.containerType == that.containerType && this.playerId.getID() == that.playerId.getID() && this.worldItemId == that.worldItemId;
        }
        return this.containerType == that.containerType && this.x == that.x && this.y == that.y && this.z == that.z && Arrays.equals(this.floorXY, that.floorXY) && this.index == that.index && this.containerIndex == that.containerIndex && this.vid == that.vid && this.worldItemId == that.worldItemId;
    }

    public int hashCode() {
        return Objects.hash(new Object[]{this.containerType, this.x, this.y, this.z, Arrays.hashCode(this.floorXY), this.index, this.containerIndex, this.vid, this.worldItemId});
    }

    public String toString() {
        return String.valueOf((Object)this.containerType) + this.hashCode();
    }

    private boolean contains(int[] floorXY, int x, int y) {
        for (int i = 0; i < floorXY.length; i += 2) {
            if (floorXY[i] != x || floorXY[i + 1] != y) continue;
            return true;
        }
        return false;
    }

    private int[] addFloorXY(int x, int y, int[] floorXY) {
        if (floorXY == null) {
            return new int[]{x, y};
        }
        if (this.contains(floorXY, x, y)) {
            return floorXY;
        }
        int[] floorXYcopy = Arrays.copyOf(floorXY, floorXY.length + 2);
        floorXYcopy[floorXY.length] = x;
        floorXYcopy[floorXY.length + 1] = y;
        return floorXYcopy;
    }

    private void calculateFloorXY(ItemContainer container) {
        this.floorXY = null;
        for (int n = 0; n < container.getItems().size(); ++n) {
            InventoryItem inventoryItem = container.getItems().get(n);
            IsoWorldInventoryObject worldItem = inventoryItem.getWorldItem();
            if (worldItem == null || worldItem.getSquare() == null) continue;
            this.floorXY = this.addFloorXY(worldItem.getSquare().getX(), worldItem.getSquare().getY(), this.floorXY);
        }
    }

    private void collectFloorItems(IsoGridSquare sq) {
        if (sq == null) {
            return;
        }
        for (int i = 0; i < sq.getWorldObjects().size(); ++i) {
            IsoWorldInventoryObject wo = sq.getWorldObjects().get(i);
            if (wo == null || wo.getItem() == null) continue;
            this.container.getItems().add(wo.getItem());
        }
    }

    @UsedFromLua
    public static enum ContainerType {
        Undefined,
        DeadBody,
        WorldObject,
        IsoObject,
        ObjectContainer,
        ObjectInVehicle,
        Vehicle,
        PlayerInventory,
        InventoryContainer,
        Floor;

    }
}

