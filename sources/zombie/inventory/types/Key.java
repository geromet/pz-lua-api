/*
 * Decompiled with CFR 0.152.
 */
package zombie.inventory.types;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import zombie.UsedFromLua;
import zombie.characters.IsoPlayer;
import zombie.core.Core;
import zombie.core.math.PZMath;
import zombie.core.random.Rand;
import zombie.inventory.InventoryItem;
import zombie.inventory.ItemContainer;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoObject;
import zombie.iso.IsoWorld;
import zombie.iso.objects.IsoDoor;
import zombie.iso.objects.IsoThumpable;
import zombie.scripting.objects.ItemType;

@UsedFromLua
public final class Key
extends InventoryItem {
    private int keyId = -1;
    private boolean padlock;
    private int numberOfKey;
    private boolean digitalPadlock;
    public static final HighlightDoor[] highlightDoor = new HighlightDoor[4];

    public Key(String module, String name, String type, String tex) {
        super(module, name, type, tex);
        this.itemType = ItemType.KEY;
    }

    public void takeKeyId() {
        ItemContainer outContainer = this.getOutermostContainer();
        if (outContainer != null && outContainer.getSourceGrid() != null && outContainer.getSourceGrid().getBuilding() != null && outContainer.getSourceGrid().getBuilding().def != null) {
            this.setKeyId(outContainer.getSourceGrid().getBuilding().def.getKeyId());
        } else {
            this.setKeyId(Rand.Next(10000000));
        }
    }

    public static void setHighlightDoors(int playerNum, InventoryItem item) {
        Key key;
        ArrayList<IsoObject> doors = Key.highlightDoor[playerNum].doors;
        if (item instanceof Key && !(key = (Key)item).isPadlock() && !key.isDigitalPadlock()) {
            Key.highlightDoor[playerNum].key = key;
            for (IsoObject door : doors) {
                door.setHighlighted(playerNum, false);
            }
            doors.clear();
            IsoPlayer player = IsoPlayer.players[playerNum];
            int x0 = PZMath.fastfloor(player.getX());
            int y0 = PZMath.fastfloor(player.getY());
            int z0 = PZMath.fastfloor(player.getZ());
            for (int x = x0 - 20; x < x0 + 20; ++x) {
                for (int y = y0 - 20; y < y0 + 20; ++y) {
                    IsoThumpable isoThumpable;
                    IsoDoor isoDoor;
                    IsoGridSquare sq = IsoWorld.instance.getCell().getGridSquare(x, y, z0);
                    if (sq == null) continue;
                    IsoObject door = sq.getDoor(true);
                    if (door instanceof IsoDoor) {
                        isoDoor = (IsoDoor)door;
                        door.setHighlightColor(playerNum, Core.getInstance().getObjectHighlitedColor());
                        isoDoor.checkKeyHighlight(playerNum);
                        doors.add(door);
                    }
                    if (door instanceof IsoThumpable && (isoThumpable = (IsoThumpable)door).isDoor()) {
                        door.setHighlightColor(playerNum, Core.getInstance().getObjectHighlitedColor());
                        isoThumpable.checkKeyHighlight(playerNum);
                        doors.add(door);
                    }
                    if ((door = sq.getDoor(false)) instanceof IsoDoor) {
                        isoDoor = (IsoDoor)door;
                        door.setHighlightColor(playerNum, Core.getInstance().getObjectHighlitedColor());
                        isoDoor.checkKeyHighlight(playerNum);
                        doors.add(door);
                    }
                    if (!(door instanceof IsoThumpable) || !(isoThumpable = (IsoThumpable)door).isDoor()) continue;
                    door.setHighlightColor(playerNum, Core.getInstance().getObjectHighlitedColor());
                    isoThumpable.checkKeyHighlight(playerNum);
                    doors.add(door);
                }
            }
        } else {
            if (!doors.isEmpty()) {
                for (IsoObject door : doors) {
                    door.setHighlighted(playerNum, false, false);
                }
                doors.clear();
            }
            Key.highlightDoor[playerNum].key = null;
        }
    }

    @Override
    public int getKeyId() {
        return this.keyId;
    }

    @Override
    public void setKeyId(int keyId) {
        if (keyId == -1) {
            keyId = Rand.Next(10000000);
        }
        this.keyId = keyId;
    }

    @Override
    public String getCategory() {
        if (this.mainCategory != null) {
            return this.mainCategory;
        }
        return "Key";
    }

    @Override
    public void save(ByteBuffer output, boolean net) throws IOException {
        super.save(output, net);
        output.putInt(this.getKeyId());
        output.put((byte)this.numberOfKey);
    }

    @Override
    public void load(ByteBuffer input, int worldVersion) throws IOException {
        super.load(input, worldVersion);
        this.setKeyId(input.getInt());
        this.numberOfKey = input.get();
    }

    public boolean isPadlock() {
        return this.padlock;
    }

    public void setPadlock(boolean padlock) {
        this.padlock = padlock;
    }

    public int getNumberOfKey() {
        return this.numberOfKey;
    }

    public void setNumberOfKey(int numberOfKey) {
        this.numberOfKey = numberOfKey;
    }

    public boolean isDigitalPadlock() {
        return this.digitalPadlock;
    }

    public void setDigitalPadlock(boolean digitalPadlock) {
        this.digitalPadlock = digitalPadlock;
    }

    static {
        for (int i = 0; i < 4; ++i) {
            Key.highlightDoor[i] = new HighlightDoor();
        }
    }

    public static final class HighlightDoor {
        public Key key;
        public final ArrayList<IsoObject> doors = new ArrayList();
    }
}

