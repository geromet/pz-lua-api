/*
 * Decompiled with CFR 0.152.
 */
package zombie.ui;

import se.krka.kahlua.vm.KahluaTable;
import zombie.UsedFromLua;
import zombie.core.SpriteRenderer;
import zombie.core.math.PZMath;
import zombie.core.textures.ColorInfo;
import zombie.core.textures.Texture;
import zombie.debug.DebugLog;
import zombie.iso.Vector2;
import zombie.ui.UIElement;

@UsedFromLua
public final class RadialProgressBar
extends UIElement {
    private static final boolean DEBUG = false;
    Texture radialTexture;
    ColorInfo radialColor = new ColorInfo(1.0f, 1.0f, 1.0f, 1.0f);
    float deltaValue = 1.0f;
    private static final RadSegment[] segments = new RadSegment[8];
    private static final float TWO_PI = 6.283185f;
    private static final float PI_OVER_TWO = 1.570796f;

    public RadialProgressBar(KahluaTable table, Texture tex) {
        super(table);
        this.radialTexture = tex;
    }

    @Override
    public void update() {
        super.update();
    }

    @Override
    public void render() {
        float textureScalar;
        float scalar;
        if (!this.enabled) {
            return;
        }
        if (!this.isVisible().booleanValue()) {
            return;
        }
        if (this.parent != null && this.parent.maxDrawHeight != -1 && (double)this.parent.maxDrawHeight <= this.y) {
            return;
        }
        if (this.radialTexture == null) {
            return;
        }
        float dx = (float)(this.xScroll + this.getAbsoluteX() + (double)this.radialTexture.offsetX);
        float dy = (float)(this.yScroll + this.getAbsoluteY() + (double)this.radialTexture.offsetY);
        float uvX = this.radialTexture.xStart;
        float uvY = this.radialTexture.yStart;
        float uvW = this.radialTexture.xEnd - this.radialTexture.xStart;
        float uvH = this.radialTexture.yEnd - this.radialTexture.yStart;
        float centerX = dx + 0.5f * this.width;
        float centerY = dy + 0.5f * this.height;
        float percent = this.deltaValue;
        float angle = percent * 6.283185f - 1.570796f;
        Vector2 angleVector = new Vector2((float)Math.cos(angle), (float)Math.sin(angle));
        if (Math.abs(this.width / 2.0f / angleVector.x) < Math.abs(this.height / 2.0f / angleVector.y)) {
            scalar = Math.abs(this.width / 2.0f / angleVector.x);
            textureScalar = Math.abs(0.5f / angleVector.x);
        } else {
            scalar = Math.abs(this.height / 2.0f / angleVector.y);
            textureScalar = Math.abs(0.5f / angleVector.y);
        }
        float tx = centerX + angleVector.x * scalar;
        float ty = centerY + angleVector.y * scalar;
        float uvx = 0.5f + angleVector.x * textureScalar;
        float uvy = 0.5f + angleVector.y * textureScalar;
        int index = (int)(percent * 8.0f);
        if (percent <= 0.0f) {
            index = -1;
        }
        for (int i = 0; i < segments.length; ++i) {
            RadSegment s = segments[i];
            if (s == null || i > index) continue;
            if (i != index) {
                SpriteRenderer.instance.renderPoly(this.radialTexture, dx + s.vertex[0].x * (float)this.radialTexture.getWidth(), dy + s.vertex[0].y * (float)this.radialTexture.getHeight(), dx + s.vertex[1].x * (float)this.radialTexture.getWidth(), dy + s.vertex[1].y * (float)this.radialTexture.getHeight(), dx + s.vertex[2].x * (float)this.radialTexture.getWidth(), dy + s.vertex[2].y * (float)this.radialTexture.getHeight(), dx + s.vertex[2].x * (float)this.radialTexture.getWidth(), dy + s.vertex[2].y * (float)this.radialTexture.getHeight(), this.radialColor.getR(), this.radialColor.getG(), this.radialColor.getB(), this.radialColor.getA(), uvX + s.uv[0].x * uvW, uvY + s.uv[0].y * uvH, uvX + s.uv[1].x * uvW, uvY + s.uv[1].y * uvH, uvX + s.uv[2].x * uvW, uvY + s.uv[2].y * uvH, uvX + s.uv[2].x * uvW, uvY + s.uv[2].y * uvH);
                continue;
            }
            SpriteRenderer.instance.renderPoly(this.radialTexture, dx + s.vertex[0].x * (float)this.radialTexture.getWidth(), dy + s.vertex[0].y * (float)this.radialTexture.getHeight(), tx, ty, dx + s.vertex[2].x * (float)this.radialTexture.getWidth(), dy + s.vertex[2].y * (float)this.radialTexture.getHeight(), dx + s.vertex[2].x * (float)this.radialTexture.getWidth(), dy + s.vertex[2].y * (float)this.radialTexture.getHeight(), this.radialColor.getR(), this.radialColor.getG(), this.radialColor.getB(), this.radialColor.getA(), uvX + s.uv[0].x * uvW, uvY + s.uv[0].y * uvH, uvX + uvx * uvW, uvY + uvy * uvH, uvX + s.uv[2].x * uvW, uvY + s.uv[2].y * uvH, uvX + s.uv[2].x * uvW, uvY + s.uv[2].y * uvH);
        }
    }

    public void setValue(float delta) {
        this.deltaValue = PZMath.clamp(delta, 0.0f, 1.0f);
    }

    public float getValue() {
        return this.deltaValue;
    }

    public void setTexture(Texture texture) {
        this.radialTexture = texture;
    }

    public Texture getTexture() {
        return this.radialTexture;
    }

    public void setColor(ColorInfo color) {
        this.radialColor = color;
    }

    private void printTexture(Texture t) {
        DebugLog.log("xStart = " + t.xStart);
        DebugLog.log("yStart = " + t.yStart);
        DebugLog.log("offX = " + t.offsetX);
        DebugLog.log("offY = " + t.offsetY);
        DebugLog.log("xEnd = " + t.xEnd);
        DebugLog.log("yEnd = " + t.yEnd);
        DebugLog.log("Width = " + t.getWidth());
        DebugLog.log("Height = " + t.getHeight());
        DebugLog.log("RealWidth = " + t.getRealWidth());
        DebugLog.log("RealHeight = " + t.getRealHeight());
        DebugLog.log("OrigWidth = " + t.getWidthOrig());
        DebugLog.log("OrigHeight = " + t.getHeightOrig());
    }

    static {
        RadialProgressBar.segments[0] = new RadSegment();
        segments[0].set(0.5f, 0.0f, 1.0f, 0.0f, 0.5f, 0.5f);
        RadialProgressBar.segments[1] = new RadSegment();
        segments[1].set(1.0f, 0.0f, 1.0f, 0.5f, 0.5f, 0.5f);
        RadialProgressBar.segments[2] = new RadSegment();
        segments[2].set(1.0f, 0.5f, 1.0f, 1.0f, 0.5f, 0.5f);
        RadialProgressBar.segments[3] = new RadSegment();
        segments[3].set(1.0f, 1.0f, 0.5f, 1.0f, 0.5f, 0.5f);
        RadialProgressBar.segments[4] = new RadSegment();
        segments[4].set(0.5f, 1.0f, 0.0f, 1.0f, 0.5f, 0.5f);
        RadialProgressBar.segments[5] = new RadSegment();
        segments[5].set(0.0f, 1.0f, 0.0f, 0.5f, 0.5f, 0.5f);
        RadialProgressBar.segments[6] = new RadSegment();
        segments[6].set(0.0f, 0.5f, 0.0f, 0.0f, 0.5f, 0.5f);
        RadialProgressBar.segments[7] = new RadSegment();
        segments[7].set(0.0f, 0.0f, 0.5f, 0.0f, 0.5f, 0.5f);
    }

    private static class RadSegment {
        Vector2[] vertex = new Vector2[3];
        Vector2[] uv = new Vector2[3];

        private RadSegment() {
        }

        private RadSegment set(int index, float vx, float vy, float uv1, float uv2) {
            this.vertex[index] = new Vector2(vx, vy);
            this.uv[index] = new Vector2(uv1, uv2);
            return this;
        }

        private void set(float x0, float y0, float x1, float y1, float x2, float y2) {
            this.vertex[0] = new Vector2(x0, y0);
            this.vertex[1] = new Vector2(x1, y1);
            this.vertex[2] = new Vector2(x2, y2);
            this.uv[0] = new Vector2(x0, y0);
            this.uv[1] = new Vector2(x1, y1);
            this.uv[2] = new Vector2(x2, y2);
        }
    }
}

