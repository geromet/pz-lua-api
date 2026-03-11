/*
 * Decompiled with CFR 0.152.
 */
package zombie.iso.objects;

import zombie.UsedFromLua;
import zombie.characters.IsoGameCharacter;
import zombie.characters.IsoPlayer;
import zombie.core.Core;
import zombie.core.opengl.Shader;
import zombie.core.random.Rand;
import zombie.core.skinnedmodel.model.ItemModelRenderer;
import zombie.core.skinnedmodel.model.WorldItemModelDrawer;
import zombie.core.textures.ColorInfo;
import zombie.core.textures.Texture;
import zombie.inventory.types.HandWeapon;
import zombie.iso.IsoCell;
import zombie.iso.IsoPhysicsObject;
import zombie.iso.objects.IsoTrap;
import zombie.network.GameClient;

@UsedFromLua
public class IsoMolotovCocktail
extends IsoPhysicsObject {
    private HandWeapon weapon;
    private IsoGameCharacter character;
    private int timer;
    private int explodeTimer;

    @Override
    public String getObjectName() {
        return "MolotovCocktail";
    }

    public IsoMolotovCocktail(IsoCell cell) {
        super(cell);
    }

    public IsoMolotovCocktail(IsoCell cell, float x, float y, float z, float xVelocity, float yVelocity, HandWeapon weapon, IsoGameCharacter character) {
        super(cell);
        this.weapon = weapon;
        this.character = character;
        this.explodeTimer = weapon.getTriggerExplosionTimer();
        this.velX = xVelocity;
        this.velY = yVelocity;
        float randX = (float)Rand.Next(4000) / 10000.0f;
        float randY = (float)Rand.Next(4000) / 10000.0f;
        this.velX += (randX -= 0.2f);
        this.velY += (randY -= 0.2f);
        this.setX(x);
        this.setY(y);
        this.setZ(z);
        this.setNextX(x);
        this.setNextY(y);
        this.offsetX = 0.0f;
        this.offsetY = 0.0f;
        this.terminalVelocity = -0.02f;
        Texture tex = this.sprite.LoadSingleTexture(weapon.getTex().getName());
        if (tex != null) {
            this.sprite.animate = false;
            int scale = Core.tileScale;
            this.sprite.def.scaleAspect(tex.getWidthOrig(), tex.getHeightOrig(), 16 * scale, 16 * scale);
        }
        this.speedMod = 0.6f;
    }

    public void collideCharacter() {
        if (this.explodeTimer == 0) {
            this.Explode();
        }
    }

    @Override
    public void collideGround() {
        if (this.explodeTimer == 0) {
            this.Explode();
        }
    }

    @Override
    public void collideWall() {
        if (this.explodeTimer == 0) {
            this.Explode();
        }
    }

    @Override
    public void update() {
        super.update();
        if (this.isDestroyed()) {
            return;
        }
        if (this.isCollidedThisFrame() && this.explodeTimer == 0) {
            this.Explode();
        }
        if (this.explodeTimer > 0) {
            ++this.timer;
            if (this.timer >= this.explodeTimer) {
                this.Explode();
            }
        }
    }

    @Override
    public void render(float x, float y, float z, ColorInfo info, boolean bDoAttached, boolean bWallLightingPass, Shader shader) {
        ItemModelRenderer.RenderStatus status;
        if (Core.getInstance().isOption3DGroundItem() && ItemModelRenderer.itemHasModel(this.weapon) && ((status = WorldItemModelDrawer.renderMain(this.weapon, this.getSquare(), this.getRenderSquare(), x, y, z, 0.0f)) == ItemModelRenderer.RenderStatus.Loading || status == ItemModelRenderer.RenderStatus.Ready)) {
            return;
        }
        super.render(x, y, z, info, bDoAttached, bWallLightingPass, shader);
        if (Core.debug) {
            // empty if block
        }
    }

    private void Explode() {
        if (this.isDestroyed() || this.getCurrentSquare() == null) {
            return;
        }
        this.setDestroyed(true);
        this.getCurrentSquare().getMovingObjects().remove(this);
        this.getCell().Remove(this);
        if (GameClient.client) {
            IsoPlayer isoPlayer;
            IsoGameCharacter isoGameCharacter = this.character;
            if (isoGameCharacter instanceof IsoPlayer && (isoPlayer = (IsoPlayer)isoGameCharacter).isLocalPlayer()) {
                this.square.syncIsoTrap(this.weapon);
            }
            return;
        }
        IsoTrap trap = new IsoTrap(this.character, this.weapon, this.getCurrentSquare().getCell(), this.getCurrentSquare());
        if (this.weapon.isInstantExplosion()) {
            if (this.weapon.canBeReused()) {
                trap.getSquare().AddTileObject(trap);
            }
            trap.triggerExplosion();
        } else {
            trap.getSquare().AddTileObject(trap);
        }
    }
}

