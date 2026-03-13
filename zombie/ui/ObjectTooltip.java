/*
 * Decompiled with CFR 0.152.
 */
package zombie.ui;

import java.util.ArrayList;
import java.util.Stack;
import zombie.UsedFromLua;
import zombie.characters.IsoGameCharacter;
import zombie.core.Core;
import zombie.core.textures.Texture;
import zombie.inventory.InventoryItem;
import zombie.iso.IsoObject;
import zombie.ui.TextManager;
import zombie.ui.UIElement;
import zombie.ui.UIFont;

@UsedFromLua
public final class ObjectTooltip
extends UIElement {
    public static float alphaStep = 0.1f;
    public boolean isItem;
    public InventoryItem item;
    public IsoObject object;
    float alpha;
    int showDelay;
    float targetAlpha;
    Texture texture = Texture.getSharedTexture("black");
    public int padLeft = 5;
    public int padTop = 5;
    public int padRight = 5;
    public int padBottom = 5;
    private IsoGameCharacter character;
    private boolean measureOnly;
    private float weightOfStack;
    private static int lineSpacing = 14;
    private static int staticPadLeft = -1;
    private static int staticPadRight;
    private static int staticPadTop;
    private static int staticPadBottom;
    private static String fontSize;
    private static UIFont font;
    private static final Stack<Layout> freeLayouts;

    public ObjectTooltip() {
        this.width = 130.0f;
        this.height = 130.0f;
        this.defaultDraw = false;
        lineSpacing = TextManager.instance.getFontFromEnum(font).getLineHeight();
        ObjectTooltip.checkFont();
    }

    public static void checkFont() {
        int charWidth;
        if (!fontSize.equals(Core.getInstance().getOptionTooltipFont())) {
            fontSize = Core.getInstance().getOptionTooltipFont();
            font = "Large".equals(fontSize) ? UIFont.Large : ("Medium".equals(fontSize) ? UIFont.Medium : UIFont.Small);
            lineSpacing = TextManager.instance.getFontFromEnum(font).getLineHeight();
        }
        staticPadLeft = staticPadRight = (charWidth = TextManager.instance.MeasureStringX(font, "0"));
        staticPadTop = staticPadBottom = charWidth / 2;
    }

    public UIFont getFont() {
        return font;
    }

    public int getLineSpacing() {
        return lineSpacing;
    }

    @Override
    public void DrawText(UIFont font, String text, double x, double y, double r, double g, double b, double alpha) {
        if (this.measureOnly) {
            return;
        }
        super.DrawText(font, text, x, y, r, g, b, alpha);
    }

    @Override
    public void DrawTextCentre(UIFont font, String text, double x, double y, double r, double g, double b, double alpha) {
        if (this.measureOnly) {
            return;
        }
        super.DrawTextCentre(font, text, x, y, r, g, b, alpha);
    }

    @Override
    public void DrawTextRight(UIFont font, String text, double x, double y, double r, double g, double b, double alpha) {
        if (this.measureOnly) {
            return;
        }
        super.DrawTextRight(font, text, x, y, r, g, b, alpha);
    }

    public void DrawValueRight(int value, int x, int y, boolean highGood) {
        Object str = Integer.toString(value);
        float r = 0.3f;
        float g = 1.0f;
        float b = 0.2f;
        float a = 1.0f;
        if (value > 0) {
            str = "+" + (String)str;
        }
        if (value < 0 && highGood || value > 0 && !highGood) {
            r = 0.8f;
            g = 0.3f;
            b = 0.2f;
        }
        this.DrawTextRight(font, (String)str, x, y, r, g, b, 1.0);
    }

    public void DrawValueRightNoPlus(int value, int x, int y) {
        String str = Integer.toString(value);
        float r = 1.0f;
        float g = 1.0f;
        float b = 1.0f;
        float a = 1.0f;
        this.DrawTextRight(font, str, x, y, 1.0, 1.0, 1.0, 1.0);
    }

    public void DrawValueRightNoPlus(float value, int x, int y) {
        float val = (float)((int)(((double)value + 0.01) * 10.0)) / 10.0f;
        String str = Float.toString(val);
        float r = 1.0f;
        float g = 1.0f;
        float b = 1.0f;
        float a = 1.0f;
        this.DrawTextRight(font, str, x, y, 1.0, 1.0, 1.0, 1.0);
    }

    @Override
    public void DrawTextureScaled(Texture tex, double x, double y, double width, double height, double alpha) {
        if (this.measureOnly) {
            return;
        }
        super.DrawTextureScaled(tex, x, y, width, height, alpha);
    }

    @Override
    public void DrawTextureScaledAspect(Texture tex, double x, double y, double width, double height, double r, double g, double b, double alpha) {
        if (this.measureOnly) {
            return;
        }
        super.DrawTextureScaledAspect(tex, x, y, width, height, r, g, b, alpha);
    }

    public void DrawProgressBar(int x, int y, int w, int h, float f, double r, double g, double b, double a) {
        if (this.measureOnly) {
            return;
        }
        if (f < 0.0f) {
            f = 0.0f;
        }
        if (f > 1.0f) {
            f = 1.0f;
        }
        int done = (int)Math.floor((float)w * f);
        this.DrawTextureScaledColor(null, (double)x - 1.0, (double)y - 1.0, (double)w + 2.0, Double.valueOf(h), 0.25, 0.25, 0.25, 1.0);
        if (f != 0.0f && f != 1.0f) {
            this.DrawTextureScaledColor(null, (double)x + (double)done, Double.valueOf(y), (double)w - (double)done, (double)h - 2.0, 0.5, 0.5, 0.5, 1.0);
        }
        this.DrawTextureScaledColor(null, Double.valueOf(x), Double.valueOf(y), Double.valueOf(done), (double)h - 2.0, r, g, b, a);
    }

    @Override
    public Boolean onMouseMove(double dx, double dy) {
        this.setX(this.getX() + dx);
        this.setY(this.getY() + dy);
        return Boolean.FALSE;
    }

    @Override
    public void onMouseMoveOutside(double dx, double dy) {
        this.setX(this.getX() + dx);
        this.setY(this.getY() + dy);
    }

    @Override
    public void render() {
        if (!this.isVisible().booleanValue()) {
            return;
        }
        if (this.alpha <= 0.0f) {
            return;
        }
        if (!this.isItem && this.object != null && this.object.haveSpecialTooltip()) {
            this.object.DoSpecialTooltip(this, this.object.square);
        }
        super.render();
    }

    public void show(IsoObject obj, double x, double y) {
        this.isItem = false;
        this.object = obj;
        this.setX(x);
        this.setY(y);
        this.targetAlpha = 0.5f;
        this.showDelay = 15;
        this.alpha = 0.0f;
    }

    public void hide() {
        this.object = null;
        this.showDelay = 0;
        this.setVisible(false);
    }

    @Override
    public void update() {
        if (this.alpha <= 0.0f && this.targetAlpha == 0.0f) {
            return;
        }
        if (this.showDelay > 0) {
            if (--this.showDelay == 0) {
                this.setVisible(true);
            }
            return;
        }
        if (this.alpha < this.targetAlpha) {
            this.alpha += alphaStep;
            if (this.alpha > 0.5f) {
                this.alpha = 0.5f;
            }
        } else if (this.alpha > this.targetAlpha) {
            this.alpha -= alphaStep;
            if (this.alpha < this.targetAlpha) {
                this.alpha = this.targetAlpha;
            }
        }
    }

    void show(InventoryItem info, int i, int i0) {
        this.object = null;
        this.item = info;
        this.isItem = true;
        this.setX(this.getX());
        this.setY(this.getY());
        this.targetAlpha = 0.5f;
        this.showDelay = 15;
        this.alpha = 0.0f;
        this.setVisible(true);
    }

    public void adjustWidth(int textX, String text) {
        int textWidth = TextManager.instance.MeasureStringX(font, text);
        if ((float)(textX + textWidth + this.padRight) > this.width) {
            this.setWidth(textX + textWidth + this.padRight);
        }
    }

    public Layout beginLayout() {
        this.padLeft = staticPadLeft;
        this.padRight = staticPadRight;
        this.padTop = staticPadTop;
        this.padBottom = staticPadBottom;
        if (freeLayouts.isEmpty()) {
            return new Layout();
        }
        return freeLayouts.pop();
    }

    public void endLayout(Layout layout) {
        while (layout != null) {
            Layout next = layout.next;
            layout.free();
            freeLayouts.push(layout);
            layout = next;
        }
    }

    public Texture getTexture() {
        return this.texture;
    }

    public void setCharacter(IsoGameCharacter chr) {
        this.character = chr;
    }

    public IsoGameCharacter getCharacter() {
        return this.character;
    }

    public void setMeasureOnly(boolean b) {
        this.measureOnly = b;
    }

    public boolean isMeasureOnly() {
        return this.measureOnly;
    }

    public float getWeightOfStack() {
        return this.weightOfStack;
    }

    public void setWeightOfStack(float weight) {
        this.weightOfStack = weight;
    }

    static {
        fontSize = "Small";
        font = UIFont.Small;
        freeLayouts = new Stack();
    }

    @UsedFromLua
    public static class Layout {
        public ArrayList<LayoutItem> items = new ArrayList();
        public int minLabelWidth;
        public int minValueWidth;
        public Layout next;
        public int nextPadY;
        public int offsetY;
        private static final Stack<LayoutItem> freeItems = new Stack();

        public LayoutItem addItem() {
            LayoutItem item;
            if (freeItems.isEmpty()) {
                item = new LayoutItem();
            } else {
                item = freeItems.pop();
                item.reset();
            }
            this.items.add(item);
            return item;
        }

        public void setMinLabelWidth(int minWidth) {
            this.minLabelWidth = minWidth;
        }

        public void setMinValueWidth(int minWidth) {
            this.minValueWidth = minWidth;
        }

        public int render(int left, int top, ObjectTooltip ui) {
            LayoutItem item;
            int i;
            int widthLabel = this.minLabelWidth;
            int widthValue = this.minValueWidth;
            int widthValueRight = this.minValueWidth;
            int widthProgress = 0;
            int widthTotal = 0;
            int padX = Math.max(TextManager.instance.MeasureStringX(font, "W"), 8);
            int mid = 0;
            for (i = 0; i < this.items.size(); ++i) {
                item = this.items.get(i);
                item.calcSizes();
                if (item.hasValue) {
                    widthLabel = Math.max(widthLabel, item.labelWidth);
                    widthValue = Math.max(widthValue, item.valueWidth);
                    widthValueRight = Math.max(widthValueRight, item.valueWidthRight);
                    widthProgress = Math.max(widthProgress, item.progressWidth);
                    mid = Math.max(mid, Math.max(item.labelWidth, this.minLabelWidth) + padX);
                    widthTotal = Math.max(widthTotal, widthLabel + padX + Math.max(Math.max(widthValue, widthValueRight), widthProgress));
                    continue;
                }
                if (item.couldHaveValue) {
                    widthLabel = Math.max(widthLabel, item.labelWidth);
                }
                widthTotal = Math.max(widthTotal, item.labelWidth);
            }
            if ((float)(left + widthTotal + ui.padRight) > ui.width) {
                ui.setWidth(left + widthTotal + ui.padRight);
            }
            for (i = 0; i < this.items.size(); ++i) {
                item = this.items.get(i);
                item.render(left, top, mid, widthValueRight, ui);
                top += item.height;
            }
            if (this.next != null) {
                return this.next.render(left, top + this.next.nextPadY, ui);
            }
            return top;
        }

        public void free() {
            freeItems.addAll(this.items);
            this.items.clear();
            this.minLabelWidth = 0;
            this.minValueWidth = 0;
            this.next = null;
            this.nextPadY = 0;
            this.offsetY = 0;
        }
    }

    @UsedFromLua
    public static class LayoutItem {
        public String label;
        public float r0;
        public float g0;
        public float b0;
        public float a0;
        public boolean hasValue;
        public boolean couldHaveValue;
        public String value;
        public boolean rightJustify;
        public float r1;
        public float g1;
        public float b1;
        public float a1;
        public float progressFraction = -1.0f;
        public int labelWidth;
        public int valueWidth;
        public int valueWidthRight;
        public int progressWidth;
        public int height;

        public void reset() {
            this.label = null;
            this.value = null;
            this.hasValue = false;
            this.couldHaveValue = false;
            this.rightJustify = false;
            this.progressFraction = -1.0f;
        }

        public void setLabel(String label, float r, float g, float b, float a) {
            this.label = label;
            this.r0 = r;
            this.b0 = b;
            this.g0 = g;
            this.a0 = a;
        }

        public void setValue(String label, float r, float g, float b, float a) {
            this.value = label;
            this.r1 = r;
            this.b1 = b;
            this.g1 = g;
            this.a1 = a;
            this.hasValue = true;
            this.rightJustify = true;
        }

        public void setValueRight(int value, boolean highGood) {
            this.value = Integer.toString(value);
            if (value > 0) {
                this.value = "+" + this.value;
            }
            if (value < 0 && highGood || value > 0 && !highGood) {
                this.r1 = Core.getInstance().getBadHighlitedColor().getR();
                this.g1 = Core.getInstance().getBadHighlitedColor().getG();
                this.b1 = Core.getInstance().getBadHighlitedColor().getB();
            } else {
                this.r1 = Core.getInstance().getGoodHighlitedColor().getR();
                this.g1 = Core.getInstance().getGoodHighlitedColor().getG();
                this.b1 = Core.getInstance().getGoodHighlitedColor().getB();
            }
            this.a1 = 1.0f;
            this.hasValue = true;
            this.rightJustify = true;
        }

        public void setValueRightNoPlus(float value) {
            value = (float)((int)((value + 0.005f) * 100.0f)) / 100.0f;
            this.value = Float.toString(value);
            this.r1 = 1.0f;
            this.g1 = 1.0f;
            this.b1 = 1.0f;
            this.a1 = 1.0f;
            this.hasValue = true;
            this.rightJustify = true;
        }

        public void setValueRightNoPlus(int value) {
            this.value = Integer.toString(value);
            this.r1 = 1.0f;
            this.g1 = 1.0f;
            this.b1 = 1.0f;
            this.a1 = 1.0f;
            this.hasValue = true;
            this.rightJustify = true;
        }

        public void setProgress(float fraction, float r, float g, float b, float a) {
            this.progressFraction = fraction;
            this.r1 = r;
            this.b1 = b;
            this.g1 = g;
            this.a1 = a;
            this.hasValue = true;
        }

        public void calcSizes() {
            int i;
            int lines1;
            this.progressWidth = 0;
            this.valueWidthRight = 0;
            this.valueWidth = 0;
            this.labelWidth = 0;
            if (this.label != null) {
                this.labelWidth = TextManager.instance.MeasureStringX(font, this.label);
            }
            if (this.hasValue) {
                if (this.value != null) {
                    int textWidth = TextManager.instance.MeasureStringX(font, this.value);
                    this.valueWidth = this.rightJustify ? 0 : textWidth;
                    this.valueWidthRight = this.rightJustify ? textWidth : 0;
                } else if (this.progressFraction != -1.0f) {
                    this.progressWidth = 80;
                }
            }
            int lines = 1;
            if (this.label != null) {
                lines1 = 1;
                for (i = 0; i < this.label.length(); ++i) {
                    if (this.label.charAt(i) != '\n') continue;
                    ++lines1;
                }
                lines = Math.max(lines, lines1);
            }
            if (this.hasValue && this.value != null) {
                lines1 = 1;
                for (i = 0; i < this.value.length(); ++i) {
                    if (this.value.charAt(i) != '\n') continue;
                    ++lines1;
                }
                lines = Math.max(lines, lines1);
            }
            this.height = lines * lineSpacing;
        }

        public void render(int x, int y, int mid, int right, ObjectTooltip ui) {
            if (this.label != null) {
                ui.DrawText(font, this.label, x, y, this.r0, this.g0, this.b0, this.a0);
            }
            if (this.value != null) {
                if (this.rightJustify) {
                    ui.DrawTextRight(font, this.value, x + mid + right, y, this.r1, this.g1, this.b1, this.a1);
                } else {
                    ui.DrawText(font, this.value, x + mid, y, this.r1, this.g1, this.b1, this.a1);
                }
            }
            if (this.progressFraction != -1.0f) {
                int h = 5;
                if ("Medium".equals(fontSize)) {
                    h = 6;
                }
                if ("Large".equals(fontSize)) {
                    h = 7;
                }
                ui.DrawProgressBar(x + mid, y + lineSpacing / 2 - 1, right, h, this.progressFraction, this.r1, this.g1, this.b1, this.a1);
            }
        }
    }
}

