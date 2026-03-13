/*
 * Decompiled with CFR 0.152.
 */
package zombie.network.fields;

import zombie.characters.IsoGameCharacter;
import zombie.core.math.PZMath;
import zombie.core.network.ByteBufferReader;
import zombie.core.network.ByteBufferWriter;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoWorld;
import zombie.network.GameServer;
import zombie.network.IConnection;
import zombie.network.ServerMap;
import zombie.network.fields.INetworkPacketField;
import zombie.network.fields.IPositional;
import zombie.network.fields.Position;

public class Square
extends Position
implements IPositional,
INetworkPacketField {
    protected IsoGridSquare square;

    public void set(IsoGameCharacter character) {
        this.set(character.getAttackTargetSquare());
    }

    public void set(IsoGridSquare square) {
        if (square != null) {
            this.set(square.getX(), square.getY(), square.getZ());
        } else {
            this.set(0.0f, 0.0f, 0.0f);
        }
        this.square = square;
    }

    @Override
    public void parse(ByteBufferReader b, IConnection connection) {
        super.parse(b, connection);
        this.square = GameServer.server ? ServerMap.instance.getGridSquare(PZMath.fastfloor(this.getX()), PZMath.fastfloor(this.getY()), PZMath.fastfloor(this.getZ())) : IsoWorld.instance.currentCell.getGridSquare(PZMath.fastfloor(this.getX()), PZMath.fastfloor(this.getY()), PZMath.fastfloor(this.getZ()));
    }

    @Override
    public void write(ByteBufferWriter b) {
        super.write(b);
    }

    public void process(IsoGameCharacter character) {
        character.setAttackTargetSquare(character.getCell().getGridSquare(this.getX(), this.getY(), this.getZ()));
    }

    public IsoGridSquare getSquare() {
        return this.square;
    }

    @Override
    public boolean isConsistent(IConnection connection) {
        return this.square != null;
    }
}

