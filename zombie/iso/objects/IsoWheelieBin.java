/*
 * Decompiled with CFR 0.152.
 */
package zombie.iso.objects;

import zombie.UsedFromLua;
import zombie.characters.IsoPlayer;
import zombie.inventory.ItemContainer;
import zombie.iso.IsoCell;
import zombie.iso.IsoDirections;
import zombie.iso.IsoPushableObject;

@UsedFromLua
public class IsoWheelieBin
extends IsoPushableObject {
    float velx;
    float vely;

    @Override
    public String getObjectName() {
        return "WheelieBin";
    }

    public IsoWheelieBin(IsoCell cell) {
        super(cell);
        this.container = new ItemContainer("wheeliebin", this.square, this);
        this.collidable = true;
        this.solid = true;
        this.shootable = false;
        this.width = 0.3f;
        this.dir = IsoDirections.E;
        this.setAlphaAndTarget(0.0f);
        this.offsetX = -26.0f;
        this.offsetY = -248.0f;
        this.outlineOnMouseover = true;
        this.sprite.LoadFramesPageSimple("TileObjectsExt_7", "TileObjectsExt_5", "TileObjectsExt_6", "TileObjectsExt_8");
    }

    public IsoWheelieBin(IsoCell cell, int x, int y, int z) {
        super(cell, x, y, z);
        this.setX((float)x + 0.5f);
        this.setY((float)y + 0.5f);
        this.setZ(z);
        this.setNextX(this.getX());
        this.setNextY(this.getY());
        this.offsetX = -26.0f;
        this.offsetY = -248.0f;
        this.weight = 6.0f;
        this.sprite.LoadFramesPageSimple("TileObjectsExt_7", "TileObjectsExt_5", "TileObjectsExt_6", "TileObjectsExt_8");
        this.square = this.getCell().getGridSquare(x, y, z);
        this.current = this.getCell().getGridSquare(x, y, z);
        this.container = new ItemContainer("wheeliebin", this.square, this);
        this.collidable = true;
        this.solid = true;
        this.shootable = false;
        this.width = 0.3f;
        this.dir = IsoDirections.E;
        this.setAlphaAndTarget(0.0f);
        this.outlineOnMouseover = true;
    }

    @Override
    public void update() {
        this.velx = this.getX() - this.getLastX();
        this.vely = this.getY() - this.getLastY();
        float capacityDelta = 1.0f - this.container.getContentsWeight() / 500.0f;
        if (capacityDelta < 0.0f) {
            capacityDelta = 0.0f;
        }
        if (capacityDelta < 0.7f) {
            capacityDelta *= capacityDelta;
        }
        if (IsoPlayer.getInstance() != null && IsoPlayer.getInstance().getDragObject() != this) {
            if (this.velx != 0.0f && this.vely == 0.0f && (this.dir == IsoDirections.E || this.dir == IsoDirections.W)) {
                this.setNextX(this.getNextX() + this.velx * 0.65f * capacityDelta);
            }
            if (this.vely != 0.0f && this.velx == 0.0f && (this.dir == IsoDirections.N || this.dir == IsoDirections.S)) {
                this.setNextY(this.getNextY() + this.vely * 0.65f * capacityDelta);
            }
        }
        super.update();
    }

    @Override
    public float getWeight(float x, float y) {
        float capacityDelta = this.container.getContentsWeight() / 500.0f;
        if (capacityDelta < 0.0f) {
            capacityDelta = 0.0f;
        }
        if (capacityDelta > 1.0f) {
            return this.getWeight() * 8.0f;
        }
        float weight = this.getWeight() * capacityDelta + 1.5f;
        if (this.dir == IsoDirections.W || this.dir == IsoDirections.E && y == 0.0f) {
            return weight / 2.0f;
        }
        if (this.dir == IsoDirections.N || this.dir == IsoDirections.S && x == 0.0f) {
            return weight / 2.0f;
        }
        return weight * 3.0f;
    }
}

