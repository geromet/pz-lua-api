/*
 * Decompiled with CFR 0.152.
 */
package zombie.ui;

import java.util.ArrayList;
import zombie.UsedFromLua;
import zombie.core.SpriteRenderer;
import zombie.core.fonts.AngelCodeFont;
import zombie.core.textures.Texture;
import zombie.input.JoypadManager;
import zombie.input.Mouse;
import zombie.ui.TextManager;
import zombie.ui.UIElement;
import zombie.ui.UIFont;
import zombie.ui.UITransition;
import zombie.util.StringUtils;

@UsedFromLua
public final class RadialMenu
extends UIElement {
    protected int outerRadius;
    protected int innerRadius;
    protected ArrayList<Slice> slices = new ArrayList();
    protected int highlight = -1;
    protected int joypad = -1;
    protected UITransition transition = new UITransition();
    protected UITransition select = new UITransition();
    protected UITransition deselect = new UITransition();
    protected int selectIndex = -1;
    protected int deselectIndex = -1;

    public RadialMenu(int x, int y, int innerRadius, int outerRadius) {
        this.setX(x);
        this.setY(y);
        this.setWidth(outerRadius * 2);
        this.setHeight(outerRadius * 2);
        this.innerRadius = innerRadius;
        this.outerRadius = outerRadius;
    }

    @Override
    public void update() {
        if (this.joypad != -1 && !JoypadManager.instance.isJoypadConnected(this.joypad)) {
            this.joypad = -1;
        }
    }

    @Override
    public void render() {
        Slice slice;
        if (!this.isVisible().booleanValue()) {
            return;
        }
        if (this.joypad != -1 && !JoypadManager.instance.isJoypadConnected(this.joypad)) {
            this.joypad = -1;
        }
        this.transition.setIgnoreUpdateTime(true);
        this.transition.setFadeIn(true);
        this.transition.update();
        if (this.slices.isEmpty()) {
            return;
        }
        float scale = this.transition.fraction();
        float innerRadius = (float)this.innerRadius * 0.85f + (float)this.innerRadius * scale * 0.15f;
        float outerRadius = (float)this.outerRadius * 0.85f + (float)this.outerRadius * scale * 0.15f;
        for (int i = 0; i < 48; ++i) {
            float degreesPerSlice = 7.5f;
            double theta2 = Math.toRadians((float)i * 7.5f);
            double theta3 = Math.toRadians((float)(i + 1) * 7.5f);
            double x0 = this.x + (double)(this.width / 2.0f);
            double y0 = this.y + (double)(this.height / 2.0f);
            double x1 = this.x + (double)(this.width / 2.0f);
            double y1 = this.y + (double)(this.height / 2.0f);
            double x2 = this.x + (double)(this.width / 2.0f) + (double)(outerRadius * (float)Math.cos(theta2));
            double y2 = this.y + (double)(this.height / 2.0f) + (double)(outerRadius * (float)Math.sin(theta2));
            double x3 = this.x + (double)(this.width / 2.0f) + (double)(outerRadius * (float)Math.cos(theta3));
            double y3 = this.y + (double)(this.height / 2.0f) + (double)(outerRadius * (float)Math.sin(theta3));
            if (i == 47) {
                y3 = y1;
            }
            float r = 0.1f;
            float g = 0.1f;
            float b = 0.1f;
            float a = 0.45f + 0.45f * scale;
            SpriteRenderer.instance.renderPoly((float)x0, (float)y0, (float)x2, (float)y2, (float)x3, (float)y3, (float)x1, (float)y1, 0.1f, 0.1f, 0.1f, a);
        }
        float degreesPerSlice = 360.0f / (float)Math.max(this.slices.size(), 2);
        float pad = this.slices.size() == 1 ? 0.0f : 1.5f;
        int highlight = this.highlight;
        if (highlight == -1) {
            highlight = this.joypad != -1 ? this.getSliceIndexFromJoypad(this.joypad) : this.getSliceIndexFromMouse(Mouse.getXA() - this.getAbsoluteX().intValue(), Mouse.getYA() - this.getAbsoluteY().intValue());
        }
        if ((slice = this.getSlice(highlight)) != null && slice.isEmpty()) {
            highlight = -1;
        }
        if (highlight != this.selectIndex) {
            this.select.reset();
            this.select.setIgnoreUpdateTime(true);
            if (this.selectIndex != -1) {
                this.deselectIndex = this.selectIndex;
                this.deselect.reset();
                this.deselect.setFadeIn(false);
                this.deselect.init(66.666664f, true);
            }
            this.selectIndex = highlight;
        }
        this.select.update();
        this.deselect.update();
        float startAngle = this.getStartAngle() - 180.0f;
        for (int i = 0; i < this.slices.size(); ++i) {
            int subSlice = Math.max(6, 48 / Math.max(this.slices.size(), 2));
            for (int j = 0; j < subSlice; ++j) {
                double theta0 = Math.toRadians(startAngle + (float)i * degreesPerSlice + (float)j * degreesPerSlice / (float)subSlice + (j == 0 ? pad : 0.0f));
                double theta1 = Math.toRadians(startAngle + (float)i * degreesPerSlice + (float)(j + 1) * degreesPerSlice / (float)subSlice - (j == subSlice - 1 ? pad : 0.0f));
                double theta2 = Math.toRadians(startAngle + (float)i * degreesPerSlice + (float)j * degreesPerSlice / (float)subSlice + (j == 0 ? pad / 2.0f : 0.0f));
                double theta3 = Math.toRadians((double)(startAngle + (float)i * degreesPerSlice + (float)(j + 1) * degreesPerSlice / (float)subSlice) - (j == subSlice - 1 ? (double)pad / 1.5 : 0.0));
                double x0 = this.x + (double)(this.width / 2.0f) + (double)(innerRadius * (float)Math.cos(theta0));
                double y0 = this.y + (double)(this.height / 2.0f) + (double)(innerRadius * (float)Math.sin(theta0));
                double x1 = this.x + (double)(this.width / 2.0f) + (double)(innerRadius * (float)Math.cos(theta1));
                double y1 = this.y + (double)(this.height / 2.0f) + (double)(innerRadius * (float)Math.sin(theta1));
                double x2 = this.x + (double)(this.width / 2.0f) + (double)(outerRadius * (float)Math.cos(theta2));
                double y2 = this.y + (double)(this.height / 2.0f) + (double)(outerRadius * (float)Math.sin(theta2));
                double x3 = this.x + (double)(this.width / 2.0f) + (double)(outerRadius * (float)Math.cos(theta3));
                double y3 = this.y + (double)(this.height / 2.0f) + (double)(outerRadius * (float)Math.sin(theta3));
                float r = 1.0f;
                float g = 1.0f;
                float b = 1.0f;
                float a = 0.025f;
                if (i == highlight) {
                    a = 0.25f + 0.25f * this.select.fraction();
                } else if (i == this.deselectIndex) {
                    a = 0.025f + 0.475f * this.deselect.fraction();
                }
                SpriteRenderer.instance.renderPoly((float)x0, (float)y0, (float)x2, (float)y2, (float)x3, (float)y3, (float)x1, (float)y1, 1.0f, 1.0f, 1.0f, a);
            }
            Texture texture = this.slices.get((int)i).texture;
            if (texture == null) continue;
            double theta = Math.toRadians(startAngle + (float)i * degreesPerSlice + degreesPerSlice / 2.0f);
            float cx = 0.0f + this.width / 2.0f + (innerRadius + (outerRadius - innerRadius) / 2.0f) * (float)Math.cos(theta);
            float cy = 0.0f + this.height / 2.0f + (innerRadius + (outerRadius - innerRadius) / 2.0f) * (float)Math.sin(theta);
            if (texture.getWidth() > 64) {
                this.DrawTextureScaledAspect(texture, (double)cx - 32.0 - (double)texture.offsetX, (double)cy - 32.0 - (double)texture.offsetY, 64.0, 64.0, 1.0, 1.0, 1.0, scale);
                continue;
            }
            this.DrawTexture(texture, cx - (float)(texture.getWidth() / 2) - texture.offsetX, cy - (float)(texture.getHeight() / 2) - texture.offsetY, scale);
        }
        if (slice != null && !StringUtils.isNullOrWhitespace(slice.text)) {
            this.formatTextInsideCircle(slice.text);
        }
    }

    private void formatTextInsideCircle(String text) {
        UIFont font = UIFont.Medium;
        AngelCodeFont fontObj = TextManager.instance.getFontFromEnum(font);
        int fontHgt = fontObj.getLineHeight();
        int nLines = 1;
        for (int i = 0; i < text.length(); ++i) {
            if (text.charAt(i) != '\n') continue;
            ++nLines;
        }
        if (nLines > 1) {
            int textHgt = nLines * fontHgt;
            int x = this.getAbsoluteX().intValue() + (int)this.width / 2;
            int y = this.getAbsoluteY().intValue() + (int)this.height / 2 - textHgt / 2;
            int offset = 0;
            for (int i = 0; i < text.length(); ++i) {
                if (text.charAt(i) != '\n') continue;
                this.drawTextWithBackground(fontObj, text, x, y, 1.0f, 1.0f, 1.0f, 1.0f, offset, i - 1);
                offset = i + 1;
                y += fontHgt;
            }
            if (offset < text.length()) {
                this.drawTextWithBackground(fontObj, text, x, y, 1.0f, 1.0f, 1.0f, 1.0f, offset, text.length() - 1);
            }
        } else {
            int x = this.getAbsoluteX().intValue() + (int)this.width / 2;
            int y = this.getAbsoluteY().intValue() + (int)this.height / 2 - fontHgt / 2;
            this.drawTextWithBackground(fontObj, text, x, y, 1.0f, 1.0f, 1.0f, 1.0f, 0, text.length() - 1);
        }
    }

    private void drawTextWithBackground(AngelCodeFont fontObj, String text, float x, float y, float r, float g, float b, float a, int startIndex, int endIndex) {
        float textWid = fontObj.getWidth(text, startIndex, endIndex);
        int fontHgt = fontObj.getLineHeight();
        int padX = 2;
        float scale = this.transition.fraction();
        float r1 = 0.1f;
        float g1 = 0.1f;
        float b1 = 0.1f;
        float a1 = 0.45f + 0.45f * scale;
        SpriteRenderer.instance.renderi(null, (int)(x - textWid / 2.0f) - 2, (int)y, (int)textWid + 4, fontHgt, 0.1f, 0.1f, 0.1f, a1, null);
        a = a / 2.0f + a / 2.0f * scale;
        fontObj.drawString(x - textWid / 2.0f, y, text, r, g, b, a, startIndex, endIndex);
    }

    public void clear() {
        this.slices.clear();
        this.transition.reset();
        this.transition.init(66.666664f, false);
        this.selectIndex = -1;
        this.deselectIndex = -1;
    }

    public void addSlice(String text, Texture texture) {
        Slice slice = new Slice();
        slice.text = text;
        slice.texture = texture;
        this.slices.add(slice);
    }

    private Slice getSlice(int sliceIndex) {
        if (sliceIndex < 0 || sliceIndex >= this.slices.size()) {
            return null;
        }
        return this.slices.get(sliceIndex);
    }

    public void setSliceText(int sliceIndex, String text) {
        Slice slice = this.getSlice(sliceIndex);
        if (slice != null) {
            slice.text = text;
        }
    }

    public void setSliceTexture(int sliceIndex, Texture texture) {
        Slice slice = this.getSlice(sliceIndex);
        if (slice != null) {
            slice.texture = texture;
        }
    }

    private float getStartAngle() {
        float degreesPerSlice = 360.0f / (float)Math.max(this.slices.size(), 2);
        return 90.0f - degreesPerSlice / 2.0f;
    }

    public int getSliceIndexFromMouse(int mx, int my) {
        float centerX = 0.0f + this.width / 2.0f;
        float centerY = 0.0f + this.height / 2.0f;
        double dist = Math.sqrt(Math.pow((float)mx - centerX, 2.0) + Math.pow((float)my - centerY, 2.0));
        if (dist > (double)this.outerRadius || dist < (double)this.innerRadius) {
            return -1;
        }
        double radians = Math.atan2((float)my - centerY, (float)mx - centerX) + Math.PI;
        double degrees = Math.toDegrees(radians);
        float degreesPerSlice = 360.0f / (float)Math.max(this.slices.size(), 2);
        if (degrees < (double)this.getStartAngle()) {
            return (int)((degrees + 360.0 - (double)this.getStartAngle()) / (double)degreesPerSlice);
        }
        return (int)((degrees - (double)this.getStartAngle()) / (double)degreesPerSlice);
    }

    public int getSliceIndexFromJoypad(int joypad) {
        if (JoypadManager.instance.isAimingAxisBeingApplied(joypad)) {
            float xAxis = JoypadManager.instance.getAimingAxisX(joypad);
            float yAxis = JoypadManager.instance.getAimingAxisY(joypad);
            double radians = Math.atan2(-yAxis, -xAxis);
            double degrees = Math.toDegrees(radians);
            float degreesPerSlice = 360.0f / (float)Math.max(this.slices.size(), 2);
            if (degrees < (double)this.getStartAngle()) {
                return (int)((degrees + 360.0 - (double)this.getStartAngle()) / (double)degreesPerSlice);
            }
            return (int)((degrees - (double)this.getStartAngle()) / (double)degreesPerSlice);
        }
        return -1;
    }

    public void setJoypad(int joypad) {
        this.joypad = joypad;
    }

    protected static class Slice {
        public String text;
        public Texture texture;

        protected Slice() {
        }

        boolean isEmpty() {
            return this.text == null && this.texture == null;
        }
    }
}

