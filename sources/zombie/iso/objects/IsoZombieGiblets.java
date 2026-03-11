/*
 * Decompiled with CFR 0.152.
 */
package zombie.iso.objects;

import zombie.GameTime;
import zombie.IndieGL;
import zombie.UsedFromLua;
import zombie.core.Core;
import zombie.core.math.PZMath;
import zombie.core.opengl.Shader;
import zombie.core.random.Rand;
import zombie.core.textures.ColorInfo;
import zombie.iso.IsoCell;
import zombie.iso.IsoPhysicsObject;
import zombie.iso.IsoWorld;
import zombie.iso.sprite.IsoSpriteInstance;

@UsedFromLua
public class IsoZombieGiblets
extends IsoPhysicsObject {
    public float tintb = 1.0f;
    public float tintg = 1.0f;
    public float tintr = 1.0f;
    public float time;
    boolean invis;

    public IsoZombieGiblets(IsoCell cell) {
        super(cell);
    }

    @Override
    public boolean Serialize() {
        return false;
    }

    @Override
    public String getObjectName() {
        return "ZombieGiblets";
    }

    @Override
    public void update() {
        if (Rand.Next(Rand.AdjustForFramerate(12)) == 0 && this.getZ() > (float)PZMath.fastfloor(this.getZ()) && this.getCurrentSquare() != null && this.getCurrentSquare().getChunk() != null) {
            this.getCurrentSquare().getChunk().addBloodSplat(this.getX(), this.getY(), PZMath.fastfloor(this.getZ()), Rand.Next(8));
        }
        if (Core.lastStand && Rand.Next(Rand.AdjustForFramerate(15)) == 0 && this.getZ() > (float)PZMath.fastfloor(this.getZ()) && this.getCurrentSquare() != null && this.getCurrentSquare().getChunk() != null) {
            this.getCurrentSquare().getChunk().addBloodSplat(this.getX(), this.getY(), PZMath.fastfloor(this.getZ()), Rand.Next(8));
        }
        super.update();
        this.time += GameTime.instance.getMultipliedSecondsSinceLastUpdate();
        if (this.velX == 0.0f && this.velY == 0.0f && this.getZ() == (float)PZMath.fastfloor(this.getZ())) {
            this.setCollidable(false);
            IsoWorld.instance.currentCell.getRemoveList().add(this);
        }
    }

    @Override
    public void render(float x, float y, float z, ColorInfo info, boolean bDoAttached, boolean bWallLightingPass, Shader shader) {
        float alpha;
        if (this.invis) {
            return;
        }
        float or = info.r;
        float og = info.g;
        float ob = info.b;
        info.r = 0.5f;
        info.g = 0.5f;
        info.b = 0.5f;
        this.def.targetAlpha = alpha = 1.0f - PZMath.clamp(this.time, 0.0f, 1.0f);
        this.sprite.def.targetAlpha = alpha;
        this.setTargetAlpha(alpha);
        IndieGL.glBlendFunc(770, 771);
        super.render(x, y, z, info, bDoAttached, bWallLightingPass, shader);
        if (Core.debug) {
            // empty if block
        }
        info.r = or;
        info.g = og;
        info.b = ob;
    }

    public IsoZombieGiblets(GibletType type, IsoCell cell, float x, float y, float z, float xvel, float yvel) {
        super(cell);
        this.velX = xvel;
        this.velY = yvel;
        float randX = (float)Rand.Next(4000) / 10000.0f;
        float randY = (float)Rand.Next(4000) / 10000.0f;
        this.velX += (randX -= 0.2f);
        this.velY += (randY -= 0.2f);
        this.setX(x);
        this.setY(y);
        this.setZ(z);
        this.setNextX(x);
        this.setNextY(y);
        this.setAlpha(0.2f);
        this.def = IsoSpriteInstance.get(this.sprite);
        this.def.alpha = 0.2f;
        this.sprite.def.alpha = 0.4f;
        this.offsetX = 0.0f;
        this.offsetY = 0.0f;
        if (Rand.Next(3) != 0) {
            this.def.alpha = 0.0f;
            this.sprite.def.alpha = 0.0f;
            this.invis = true;
        }
        switch (type.ordinal()) {
            case 0: {
                this.sprite.setFromCache("Giblet", "00", 3);
                break;
            }
            case 1: {
                this.sprite.setFromCache("Giblet", "01", 3);
                break;
            }
            case 2: {
                this.sprite.setFromCache("Eyeball", "00", 1);
            }
        }
    }

    public static enum GibletType {
        A,
        B,
        Eye;

    }
}

