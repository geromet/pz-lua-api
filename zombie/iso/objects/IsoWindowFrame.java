/*
 * Decompiled with CFR 0.152.
 */
package zombie.iso.objects;

import java.io.IOException;
import java.nio.ByteBuffer;
import zombie.UsedFromLua;
import zombie.characters.IsoGameCharacter;
import zombie.characters.IsoPlayer;
import zombie.inventory.InventoryItem;
import zombie.iso.IsoCell;
import zombie.iso.IsoDirections;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoObject;
import zombie.iso.SpriteDetails.IsoFlagType;
import zombie.iso.SpriteDetails.IsoObjectType;
import zombie.iso.objects.IsoBarricade;
import zombie.iso.objects.IsoCurtain;
import zombie.iso.objects.IsoThumpable;
import zombie.iso.objects.IsoWindow;
import zombie.iso.objects.interfaces.BarricadeAble;
import zombie.iso.objects.interfaces.Thumpable;
import zombie.iso.sprite.IsoSprite;
import zombie.network.GameClient;
import zombie.network.GameServer;

@UsedFromLua
public class IsoWindowFrame
extends IsoObject
implements BarricadeAble {
    private boolean north;

    public IsoWindowFrame(IsoCell cell) {
        super(cell);
    }

    public IsoWindowFrame(IsoCell cell, IsoGridSquare gridSquare, IsoSprite gid, boolean north) {
        super(cell, gridSquare, gid);
        this.north = north;
    }

    @Override
    public String getObjectName() {
        return "IsoWindowFrame";
    }

    @Override
    public boolean haveSheetRope() {
        return IsoWindow.isTopOfSheetRopeHere(this.getSquare(), this.getNorth());
    }

    @Override
    public int countAddSheetRope() {
        return IsoWindowFrame.countAddSheetRope(this);
    }

    @Override
    public boolean canAddSheetRope() {
        if (!this.canClimbThrough(null)) {
            return false;
        }
        return IsoWindowFrame.canAddSheetRope(this);
    }

    @Override
    public boolean addSheetRope(IsoPlayer player, String itemType) {
        return IsoWindowFrame.addSheetRope(this, player, itemType);
    }

    @Override
    public boolean removeSheetRope(IsoPlayer player) {
        return IsoWindowFrame.removeSheetRope(this, player);
    }

    @Override
    public Thumpable getThumpableFor(IsoGameCharacter chr) {
        IsoWindow window = this.getWindow();
        if (window != null) {
            return window.getThumpableFor(chr);
        }
        IsoBarricade barricade = this.getBarricadeForCharacter(chr);
        if (barricade != null) {
            return barricade;
        }
        barricade = this.getBarricadeOppositeCharacter(chr);
        if (barricade != null) {
            return barricade;
        }
        return null;
    }

    @Override
    public boolean isBarricaded() {
        IsoBarricade barricade = this.getBarricadeOnSameSquare();
        if (barricade == null) {
            barricade = this.getBarricadeOnOppositeSquare();
        }
        return barricade != null;
    }

    @Override
    public boolean isBarricadeAllowed() {
        return this.getWindow() == null;
    }

    @Override
    public IsoBarricade getBarricadeOnSameSquare() {
        if (this.hasWindow()) {
            return null;
        }
        return IsoBarricade.GetBarricadeOnSquare(this.square, this.getNorth() ? IsoDirections.N : IsoDirections.W);
    }

    @Override
    public IsoBarricade getBarricadeOnOppositeSquare() {
        if (this.hasWindow()) {
            return null;
        }
        return IsoBarricade.GetBarricadeOnSquare(this.getOppositeSquare(), this.getNorth() ? IsoDirections.S : IsoDirections.E);
    }

    @Override
    public IsoBarricade getBarricadeForCharacter(IsoGameCharacter chr) {
        if (this.hasWindow()) {
            return null;
        }
        return IsoBarricade.GetBarricadeForCharacter(this, chr);
    }

    @Override
    public IsoBarricade getBarricadeOppositeCharacter(IsoGameCharacter chr) {
        if (this.hasWindow()) {
            return null;
        }
        return IsoBarricade.GetBarricadeOppositeCharacter(this, chr);
    }

    @Override
    public IsoGridSquare getOppositeSquare() {
        if (this.getSquare() == null) {
            return null;
        }
        return this.getSquare().getAdjacentSquare(this.getNorth() ? IsoDirections.N : IsoDirections.W);
    }

    @Override
    public boolean getNorth() {
        return this.north;
    }

    @Override
    public void save(ByteBuffer output, boolean isDebugSave) throws IOException {
        super.save(output, isDebugSave);
        output.put(this.north ? (byte)1 : 0);
    }

    @Override
    public void load(ByteBuffer input, int worldVersion, boolean isDebugSave) throws IOException {
        super.load(input, worldVersion, isDebugSave);
        this.north = input.get() != 0;
    }

    public IsoWindow getWindow() {
        if (this.getSquare() == null) {
            return null;
        }
        return this.getSquare().getWindow(this.getNorth());
    }

    public boolean hasWindow() {
        return this.getWindow() != null;
    }

    public boolean canClimbThrough(IsoGameCharacter chr) {
        return IsoWindowFrame.canClimbThrough(this, chr);
    }

    public IsoCurtain getCurtain() {
        return IsoWindowFrame.getCurtain(this);
    }

    public IsoCurtain HasCurtains() {
        return this.getCurtain();
    }

    public IsoGridSquare getAddSheetSquare(IsoGameCharacter chr) {
        return IsoWindowFrame.getAddSheetSquare(this, chr);
    }

    public void addSheet(IsoGameCharacter chr) {
        IsoWindowFrame.addSheet(this, chr);
    }

    private static Direction getDirection(IsoObject o) {
        if (o instanceof IsoWindowFrame) {
            IsoWindowFrame isoWindowFrame = (IsoWindowFrame)o;
            return isoWindowFrame.getNorth() ? Direction.NORTH : Direction.WEST;
        }
        if (o instanceof IsoWindow || o instanceof IsoThumpable) {
            return Direction.INVALID;
        }
        if (o == null || o.getProperties() == null || o.getObjectIndex() == -1) {
            return Direction.INVALID;
        }
        if (o.getProperties().has(IsoFlagType.WindowN)) {
            return Direction.NORTH;
        }
        if (o.getProperties().has(IsoFlagType.WindowW)) {
            return Direction.WEST;
        }
        return Direction.INVALID;
    }

    public static boolean isWindowFrame(IsoObject o) {
        return IsoWindowFrame.getDirection(o).isValid();
    }

    public static boolean isWindowFrame(IsoObject o, boolean north) {
        Direction dir = IsoWindowFrame.getDirection(o);
        return north && dir == Direction.NORTH || !north && dir == Direction.WEST;
    }

    public static int countAddSheetRope(IsoObject o) {
        Direction dir = IsoWindowFrame.getDirection(o);
        return dir.isValid() ? IsoWindow.countAddSheetRope(o.getSquare(), dir == Direction.NORTH) : 0;
    }

    public static boolean canAddSheetRope(IsoObject o) {
        Direction dir = IsoWindowFrame.getDirection(o);
        return dir.isValid() && IsoWindow.canAddSheetRope(o.getSquare(), dir == Direction.NORTH);
    }

    public static boolean haveSheetRope(IsoObject o) {
        Direction dir = IsoWindowFrame.getDirection(o);
        return dir.isValid() && IsoWindow.isTopOfSheetRopeHere(o.getSquare(), dir == Direction.NORTH);
    }

    public static boolean addSheetRope(IsoObject o, IsoPlayer player, String itemType) {
        if (!IsoWindowFrame.canAddSheetRope(o)) {
            return false;
        }
        return IsoWindow.addSheetRope(player, o.getSquare(), IsoWindowFrame.getDirection(o) == Direction.NORTH, itemType);
    }

    public static boolean removeSheetRope(IsoObject o, IsoPlayer player) {
        if (!IsoWindowFrame.haveSheetRope(o)) {
            return false;
        }
        return IsoWindow.removeSheetRope(player, o.getSquare(), IsoWindowFrame.getDirection(o) == Direction.NORTH);
    }

    public static IsoGridSquare getOppositeSquare(IsoObject o) {
        Direction dir = IsoWindowFrame.getDirection(o);
        if (!dir.isValid()) {
            return null;
        }
        boolean north = dir == Direction.NORTH;
        return o.getSquare().getAdjacentSquare(north ? IsoDirections.N : IsoDirections.W);
    }

    public static IsoGridSquare getIndoorSquare(IsoObject o) {
        Direction dir = IsoWindowFrame.getDirection(o);
        if (!dir.isValid()) {
            return null;
        }
        IsoGridSquare sqThis = o.getSquare();
        if (sqThis.getRoom() != null) {
            return sqThis;
        }
        IsoGridSquare sq = IsoWindowFrame.getOppositeSquare(o);
        if (sq != null && sq.getRoom() != null) {
            return sq;
        }
        return null;
    }

    public static IsoCurtain getCurtain(IsoObject o) {
        Direction dir = IsoWindowFrame.getDirection(o);
        if (!dir.isValid()) {
            return null;
        }
        boolean north = dir == Direction.NORTH;
        IsoCurtain curtain = o.getSquare().getCurtain(north ? IsoObjectType.curtainN : IsoObjectType.curtainW);
        if (curtain != null) {
            return curtain;
        }
        IsoGridSquare square = IsoWindowFrame.getOppositeSquare(o);
        return square == null ? null : square.getCurtain(north ? IsoObjectType.curtainS : IsoObjectType.curtainE);
    }

    public static IsoGridSquare getAddSheetSquare(IsoObject o, IsoGameCharacter chr) {
        boolean north;
        Direction dir = IsoWindowFrame.getDirection(o);
        if (!dir.isValid()) {
            return null;
        }
        boolean bl = north = dir == Direction.NORTH;
        if (chr == null || chr.getCurrentSquare() == null) {
            return null;
        }
        IsoGridSquare sqChr = chr.getCurrentSquare();
        IsoGridSquare sqThis = o.getSquare();
        if (north) {
            if (sqChr.getY() < sqThis.getY()) {
                return sqThis.getAdjacentSquare(IsoDirections.N);
            }
        } else if (sqChr.getX() < sqThis.getX()) {
            return sqThis.getAdjacentSquare(IsoDirections.W);
        }
        return sqThis;
    }

    public static void addSheet(IsoObject o, IsoGameCharacter chr) {
        IsoObjectType curtainType;
        Direction dir = IsoWindowFrame.getDirection(o);
        if (!dir.isValid()) {
            return;
        }
        boolean north = dir == Direction.NORTH;
        IsoGridSquare sq = IsoWindowFrame.getIndoorSquare(o);
        if (sq == null) {
            sq = o.getSquare();
        }
        if (chr != null) {
            sq = IsoWindowFrame.getAddSheetSquare(o, chr);
        }
        if (sq == null) {
            return;
        }
        if (sq == o.getSquare()) {
            curtainType = north ? IsoObjectType.curtainN : IsoObjectType.curtainW;
        } else {
            IsoObjectType isoObjectType = curtainType = north ? IsoObjectType.curtainS : IsoObjectType.curtainE;
        }
        if (sq.getCurtain(curtainType) != null) {
            return;
        }
        int gid = 16;
        if (curtainType == IsoObjectType.curtainE) {
            ++gid;
        }
        if (curtainType == IsoObjectType.curtainS) {
            gid += 3;
        }
        if (curtainType == IsoObjectType.curtainN) {
            gid += 2;
        }
        IsoCurtain curtain = new IsoCurtain(o.getCell(), sq, "fixtures_windows_curtains_01_" + (gid += 4), north);
        sq.AddSpecialTileObject(curtain);
        if (!GameClient.client) {
            InventoryItem item = chr.getInventory().FindAndReturn("Sheet");
            chr.getInventory().Remove(item);
            if (GameServer.server) {
                GameServer.sendRemoveItemFromContainer(chr.getInventory(), item);
            }
        }
        if (GameServer.server) {
            curtain.transmitCompleteItemToClients();
        }
    }

    public static boolean canClimbThrough(IsoObject o, IsoGameCharacter chr) {
        IsoWindowFrame isoWindowFrame;
        Direction dir = IsoWindowFrame.getDirection(o);
        if (!dir.isValid()) {
            return false;
        }
        if (o.getSquare() == null) {
            return false;
        }
        IsoWindow window = o.getSquare().getWindow(dir == Direction.NORTH);
        if (window != null && window.isBarricaded()) {
            return false;
        }
        if (o instanceof IsoWindowFrame && (isoWindowFrame = (IsoWindowFrame)o).isBarricaded()) {
            return false;
        }
        if (chr != null) {
            IsoGridSquare oppositeSq = dir == Direction.NORTH ? o.getSquare().getAdjacentSquare(IsoDirections.N) : o.getSquare().getAdjacentSquare(IsoDirections.W);
            if (!IsoWindow.canClimbThroughHelper(chr, o.getSquare(), oppositeSq, dir == Direction.NORTH)) {
                return false;
            }
        }
        return true;
    }

    private static enum Direction {
        INVALID,
        NORTH,
        WEST;


        public boolean isValid() {
            return this != INVALID;
        }
    }
}

