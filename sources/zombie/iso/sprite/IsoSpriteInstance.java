/*
 * Decompiled with CFR 0.152.
 */
package zombie.iso.sprite;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import zombie.UsedFromLua;
import zombie.core.PerformanceSettings;
import zombie.core.textures.ColorInfo;
import zombie.core.textures.TextureDraw;
import zombie.debug.DebugOptions;
import zombie.iso.IsoCamera;
import zombie.iso.IsoDirections;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoObject;
import zombie.iso.sprite.IsoSprite;
import zombie.popman.ObjectPool;

@UsedFromLua
public final class IsoSpriteInstance {
    public static final ObjectPool<IsoSpriteInstance> pool = new ObjectPool<IsoSpriteInstance>(IsoSpriteInstance::new);
    private static final AtomicBoolean lock = new AtomicBoolean(false);
    public IsoSprite parentSprite;
    public float tintb = 1.0f;
    public float tintg = 1.0f;
    public float tintr = 1.0f;
    public float frame;
    public float alpha = 1.0f;
    public float targetAlpha = 1.0f;
    public boolean copyTargetAlpha = true;
    public boolean multiplyObjectAlpha;
    public boolean flip;
    public float offZ;
    public float offX;
    public float offY;
    public float animFrameIncrease = 1.0f;
    static float multiplier = 1.0f;
    public boolean looped = true;
    public boolean finished;
    public boolean nextFrame;
    public float scaleX = 1.0f;
    public float scaleY = 1.0f;

    public static IsoSpriteInstance get(IsoSprite spr) {
        while (true) {
            if (lock.compareAndSet(false, true)) break;
            Thread.onSpinWait();
        }
        IsoSpriteInstance spri = pool.alloc();
        lock.set(false);
        spri.parentSprite = spr;
        spri.reset();
        return spri;
    }

    private void reset() {
        this.tintb = 1.0f;
        this.tintg = 1.0f;
        this.tintr = 1.0f;
        this.frame = 0.0f;
        this.alpha = 1.0f;
        this.targetAlpha = 1.0f;
        this.copyTargetAlpha = true;
        this.multiplyObjectAlpha = false;
        this.flip = false;
        this.offZ = 0.0f;
        this.offX = 0.0f;
        this.offY = 0.0f;
        this.animFrameIncrease = 1.0f;
        multiplier = 1.0f;
        this.looped = true;
        this.finished = false;
        this.nextFrame = false;
        this.scaleX = 1.0f;
        this.scaleY = 1.0f;
    }

    public IsoSpriteInstance() {
    }

    public void setFrameSpeedPerFrame(float perSecond) {
        this.animFrameIncrease = perSecond * multiplier;
    }

    public int getID() {
        return this.parentSprite.id;
    }

    public String getName() {
        return this.parentSprite.getName();
    }

    public IsoSprite getParentSprite() {
        return this.parentSprite;
    }

    public IsoSpriteInstance(IsoSprite spr) {
        this.parentSprite = spr;
    }

    public float getTintR() {
        return this.tintr;
    }

    public float getTintG() {
        return this.tintg;
    }

    public float getTintB() {
        return this.tintb;
    }

    public float getAlpha() {
        return this.alpha;
    }

    public float getTargetAlpha() {
        return this.targetAlpha;
    }

    public boolean isCopyTargetAlpha() {
        return this.copyTargetAlpha;
    }

    public boolean isMultiplyObjectAlpha() {
        return this.multiplyObjectAlpha;
    }

    public void render(IsoObject obj, float x, float y, float z, IsoDirections dir, float offsetX, float offsetY, ColorInfo info2) {
        this.parentSprite.render(this, obj, x, y, z, dir, offsetX, offsetY, info2, true);
    }

    public void render(IsoObject obj, float x, float y, float z, IsoDirections dir, float offsetX, float offsetY, ColorInfo info2, boolean bDoRenderPrep) {
        this.parentSprite.render(this, obj, x, y, z, dir, offsetX, offsetY, info2, bDoRenderPrep);
    }

    public void render(IsoObject obj, float x, float y, float z, IsoDirections dir, float offsetX, float offsetY, ColorInfo info2, boolean bDoRenderPrep, Consumer<TextureDraw> texdModifier) {
        this.parentSprite.render(this, obj, x, y, z, dir, offsetX, offsetY, info2, bDoRenderPrep, texdModifier);
    }

    public void SetAlpha(float f) {
        this.alpha = f;
        this.copyTargetAlpha = false;
    }

    public void SetTargetAlpha(float targetAlpha) {
        this.targetAlpha = targetAlpha;
        this.copyTargetAlpha = false;
    }

    public void update() {
    }

    protected void renderprep(IsoObject obj) {
        if (DebugOptions.instance.fboRenderChunk.forceAlphaAndTargetOne.getValue() && obj != null) {
            obj.setAlphaAndTarget(1.0f);
        }
        if (PerformanceSettings.fboRenderChunk && DebugOptions.instance.fboRenderChunk.forceAlphaToTarget.getValue()) {
            if (obj != null && this.copyTargetAlpha) {
                this.targetAlpha = obj.getTargetAlpha(IsoCamera.frameState.playerIndex);
            }
            this.alpha = this.targetAlpha;
            return;
        }
        if (obj != null && this.copyTargetAlpha) {
            this.targetAlpha = obj.getTargetAlpha(IsoCamera.frameState.playerIndex);
            this.alpha = obj.getAlpha(IsoCamera.frameState.playerIndex);
            return;
        }
        if (this.multiplyObjectAlpha) {
            return;
        }
        if (this.alpha < this.targetAlpha) {
            this.alpha += IsoSprite.alphaStep;
            if (this.alpha > this.targetAlpha) {
                this.alpha = this.targetAlpha;
            }
        } else if (this.alpha > this.targetAlpha) {
            this.alpha -= IsoSprite.alphaStep;
            if (this.alpha < this.targetAlpha) {
                this.alpha = this.targetAlpha;
            }
        }
        if (this.alpha < 0.0f) {
            this.alpha = 0.0f;
        }
        if (this.alpha > 1.0f) {
            this.alpha = 1.0f;
        }
    }

    public float getFrame() {
        return this.frame;
    }

    public boolean isFinished() {
        return this.finished;
    }

    public void Dispose() {
    }

    public void RenderGhostTileColor(int x, int y, int z, float r, float g, float b, float a) {
        if (this.parentSprite == null) {
            return;
        }
        IsoSpriteInstance spriteInstance = IsoSpriteInstance.get(this.parentSprite);
        spriteInstance.frame = this.frame;
        spriteInstance.tintr = r;
        spriteInstance.tintg = g;
        spriteInstance.tintb = b;
        spriteInstance.alpha = spriteInstance.targetAlpha = a;
        IsoGridSquare.getDefColorInfo().a = 1.0f;
        IsoGridSquare.getDefColorInfo().b = 1.0f;
        IsoGridSquare.getDefColorInfo().g = 1.0f;
        IsoGridSquare.getDefColorInfo().r = 1.0f;
        this.parentSprite.render(spriteInstance, null, (float)x, (float)y, z, IsoDirections.N, 0.0f, -144.0f, IsoGridSquare.getDefColorInfo(), true);
    }

    public void setScale(float scaleX, float scaleY) {
        this.scaleX = scaleX;
        this.scaleY = scaleY;
    }

    public float getScaleX() {
        return this.scaleX;
    }

    public float getScaleY() {
        return this.scaleY;
    }

    public void scaleAspect(float texW, float texH, float width, float height) {
        if (texW > 0.0f && texH > 0.0f && width > 0.0f && height > 0.0f) {
            boolean useHeight;
            float rw = height * texW / texH;
            float rh = width * texH / texW;
            boolean bl = useHeight = rw <= width;
            if (useHeight) {
                width = rw;
            } else {
                height = rh;
            }
            this.scaleX = width / texW;
            this.scaleY = height / texH;
        }
    }

    public static void add(IsoSpriteInstance isoSpriteInstance) {
        if (isoSpriteInstance == null) {
            return;
        }
        isoSpriteInstance.reset();
        while (true) {
            if (lock.compareAndSet(false, true)) break;
            Thread.onSpinWait();
        }
        pool.release(isoSpriteInstance);
        lock.set(false);
    }
}

