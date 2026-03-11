/*
 * Decompiled with CFR 0.152.
 */
package zombie.scripting.ui;

import zombie.UsedFromLua;
import zombie.scripting.ui.XuiScript;

@UsedFromLua
public enum VectorPosAlign {
    None(0.0f, 0.0f),
    TopLeft(0.0f, 0.0f),
    TopMiddle(0.5f, 0.5f),
    TopRight(1.0f, 1.0f),
    CenterLeft(0.0f, 0.5f),
    CenterMiddle(0.5f, 0.5f),
    CenterRight(1.0f, 0.5f),
    BottomLeft(0.0f, 1.0f),
    BottomMiddle(0.5f, 1.0f),
    BottomRight(1.0f, 1.0f);

    private final float xmod;
    private final float ymod;

    private VectorPosAlign(float xmod, float ymod) {
        this.xmod = xmod;
        this.ymod = ymod;
    }

    public float getXmod() {
        return this.xmod;
    }

    public float getYmod() {
        return this.ymod;
    }

    public float getX(XuiScript.XuiVector v) {
        if (v.isxPercent()) {
            return v.getX();
        }
        return v.getX() - v.getW() * this.xmod;
    }

    public float getY(XuiScript.XuiVector v) {
        if (v.isyPercent()) {
            return v.getY();
        }
        return v.getY() - v.getH() * this.ymod;
    }
}

