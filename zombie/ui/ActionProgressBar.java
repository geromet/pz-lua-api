/*
 * Decompiled with CFR 0.152.
 */
package zombie.ui;

import zombie.GameTime;
import zombie.IndieGL;
import zombie.UsedFromLua;
import zombie.characters.IsoPlayer;
import zombie.core.Color;
import zombie.core.Core;
import zombie.core.textures.Texture;
import zombie.iso.IsoCamera;
import zombie.iso.IsoUtils;
import zombie.ui.UIElement;
import zombie.ui.UIManager;

@UsedFromLua
public final class ActionProgressBar
extends UIElement {
    Texture background = Texture.getSharedTexture("BuildBar_Bkg");
    Texture foreground = Texture.getSharedTexture("BuildBar_Bar");
    float deltaValue = 1.0f;
    float animationProgress;
    public int delayHide;
    private final int offsetX;
    private final int offsetY;

    public ActionProgressBar(int x, int y) {
        this.offsetX = x;
        this.offsetY = y;
        this.width = this.background.getWidth();
        this.height = this.background.getHeight();
        this.followGameWorld = true;
    }

    @Override
    public void render() {
        if (!this.isVisible().booleanValue() || !UIManager.visibleAllUi) {
            return;
        }
        float scale = Core.getInstance().getOptionActionProgressBarSize();
        IndieGL.glBlendFuncSeparate(770, 771, 1, 771);
        this.DrawUVSliceTexture(this.background, 0.0, 0.0, (float)this.background.getWidth() * scale, (float)this.background.getHeight() * scale, Color.white, 0.0, 0.0, 1.0, 1.0);
        float fgY = this.foreground.offsetY * scale - this.foreground.offsetY;
        float fgH = this.foreground.getHeight() + 1;
        float fgW = this.foreground.getWidth() + 1;
        if (this.deltaValue == Float.POSITIVE_INFINITY) {
            if (this.animationProgress < 0.5f) {
                this.DrawUVSliceTexture(this.foreground, 3.0f * scale, fgY, fgW * scale, fgH * scale, Color.white, 0.0, 0.0, this.animationProgress * 2.0f, 1.0);
            } else {
                this.DrawUVSliceTexture(this.foreground, 3.0f * scale, fgY, fgW * scale, fgH * scale, Color.white, (this.animationProgress - 0.5f) * 2.0f, 0.0, 1.0, 1.0);
            }
        } else {
            this.DrawUVSliceTexture(this.foreground, 3.0f * scale, fgY, fgW * scale, fgH * scale, Color.white, 0.0, 0.0, this.deltaValue, 1.0);
        }
    }

    public void setValue(float delta) {
        this.deltaValue = delta;
    }

    public float getValue() {
        return this.deltaValue;
    }

    public void update(int nPlayer) {
        if (this.deltaValue == Float.POSITIVE_INFINITY) {
            this.animationProgress += 0.02f * (GameTime.getInstance().getRealworldSecondsSinceLastUpdate() * 60.0f);
            if (this.animationProgress > 1.0f) {
                this.animationProgress = 0.0f;
            }
            this.setVisible(true);
            this.updateScreenPos(nPlayer);
            this.delayHide = 2;
            return;
        }
        if (this.getValue() > 0.0f && this.getValue() < 1.0f) {
            this.setVisible(true);
            this.delayHide = 2;
        } else if (this.isVisible().booleanValue() && this.delayHide > 0 && --this.delayHide == 0) {
            this.setVisible(false);
        }
        if (!UIManager.visibleAllUi) {
            this.setVisible(false);
        }
        if (this.isVisible().booleanValue()) {
            this.updateScreenPos(nPlayer);
        }
    }

    private void updateScreenPos(int nPlayer) {
        IsoPlayer player = IsoPlayer.players[nPlayer];
        if (player == null) {
            return;
        }
        float scale = Core.getInstance().getOptionActionProgressBarSize();
        this.width = (float)this.background.getWidth() * scale;
        this.height = (float)this.background.getHeight() * scale;
        float sx = IsoUtils.XToScreen(player.getX(), player.getY(), player.getZ(), 0);
        float sy = IsoUtils.YToScreen(player.getX(), player.getY(), player.getZ(), 0);
        sx = sx - IsoCamera.getOffX() - player.offsetX;
        sy = sy - IsoCamera.getOffY() - player.offsetY;
        sy -= (float)(128 / (2 / Core.tileScale));
        sx /= Core.getInstance().getZoom(nPlayer);
        sy /= Core.getInstance().getZoom(nPlayer);
        sx -= this.width / 2.0f;
        sy -= this.height;
        if (player.getUserNameHeight() > 0) {
            sy -= (float)(player.getUserNameHeight() + 2);
        }
        this.setX(sx + (float)this.offsetX);
        this.setY(sy + (float)this.offsetY);
    }
}

