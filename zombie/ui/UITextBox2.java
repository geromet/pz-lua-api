/*
 * Decompiled with CFR 0.152.
 */
package zombie.ui;

import gnu.trove.list.array.TIntArrayList;
import java.util.Objects;
import java.util.Stack;
import org.lwjglx.input.Keyboard;
import zombie.GameTime;
import zombie.Lua.LuaManager;
import zombie.UsedFromLua;
import zombie.characters.Capability;
import zombie.characters.IsoPlayer;
import zombie.core.Clipboard;
import zombie.core.Color;
import zombie.core.Core;
import zombie.core.fonts.AngelCodeFont;
import zombie.core.math.PZMath;
import zombie.core.textures.ColorInfo;
import zombie.core.textures.Texture;
import zombie.input.GameKeyboard;
import zombie.input.Mouse;
import zombie.network.GameClient;
import zombie.ui.TextManager;
import zombie.ui.UIDebugConsole;
import zombie.ui.UIElement;
import zombie.ui.UIFont;
import zombie.ui.UIManager;
import zombie.ui.UINineGrid;
import zombie.ui.UITextEntryInterface;
import zombie.ui.UITransition;
import zombie.util.StringUtils;

@UsedFromLua
public class UITextBox2
extends UIElement
implements UITextEntryInterface {
    private static boolean consoleHasFocus;
    public Stack<String> lines = new Stack();
    public UINineGrid frame;
    public String text = "";
    private final boolean centered = false;
    private boolean centerVertically;
    private final Color standardFrameColour = new Color(50, 50, 50, 212);
    private final Color textEntryFrameColour = new Color(50, 50, 127, 212);
    private final Color textEntryCursorColour = new Color(170, 170, 220, 240);
    private final Color textEntryCursorColour2 = new Color(100, 100, 220, 160);
    private final Color neutralColour = new Color(0, 0, 255, 33);
    private final Color neutralColour2 = new Color(127, 0, 255, 33);
    private final Color badColour = new Color(255, 0, 0, 33);
    private final Color goodColour = new Color(0, 255, 33);
    public boolean doingTextEntry;
    private int textEntryCursorPos;
    public int textEntryMaxLength = 2000;
    public boolean isEditable;
    private boolean isSelectable;
    private int cursorLine;
    public boolean multipleLine;
    private final TIntArrayList textOffsetOfLineStart = new TIntArrayList();
    private int toSelectionIndex;
    public String internalText = "";
    private final String maskChr = "*";
    private boolean mask;
    private boolean ignoreFirst;
    public UIFont font;
    private final int[] highlightLines = new int[1000];
    private boolean hasFrame;
    public int numVisibleLines;
    public int topLineIndex;
    private final int blinkFramesOn = 12;
    private final int blinkFramesOff = 8;
    private float blinkFrame = 12.0f;
    private boolean blinkState = true;
    private final ColorInfo textColor = new ColorInfo();
    private final int edgeSize = 5;
    private boolean selectingRange;
    private int maxTextLength = -1;
    private boolean forceUpperCase;
    private int xOffset;
    private int maxLines = 1;
    private boolean onlyNumbers;
    private boolean onlyText;
    private final Texture clearButtonTexture;
    private int clearButtonSize;
    private boolean clearButton;
    private UITransition clearButtonTransition;
    private boolean wrapLines = true;
    private String placeholderText;
    private final ColorInfo placeholderTextColor = new ColorInfo(0.5f, 0.5f, 0.5f, 1.0f);
    public boolean alwaysPaginate = true;
    public boolean textChanged;
    private int paginateWidth = -1;
    private UIFont paginateFont;

    public UITextBox2(UIFont font, int x, int y, int width, int height, String text, boolean hasFrame) {
        this.font = font;
        this.x = x;
        this.y = y;
        this.SetText(text);
        this.width = width;
        this.height = height;
        this.numVisibleLines = 10;
        this.topLineIndex = 0;
        Core.currentTextEntryBox = this;
        for (int i = 0; i < 1000; ++i) {
            this.highlightLines[i] = 0;
        }
        this.hasFrame = hasFrame;
        if (hasFrame) {
            this.frame = new UINineGrid(0, 0, width, height, 5, 5, 5, 5, "media/ui/Box_TopLeft.png", "media/ui/Box_Top.png", "media/ui/Box_TopRight.png", "media/ui/Box_Left.png", "media/ui/Box_Center.png", "media/ui/Box_Right.png", "media/ui/Box_BottomLeft.png", "media/ui/Box_Bottom.png", "media/ui/Box_BottomRight.png");
            this.AddChild(this.frame);
        }
        this.Paginate();
        this.doingTextEntry = false;
        this.textEntryMaxLength = 2000;
        this.toSelectionIndex = this.textEntryCursorPos = 0;
        this.isEditable = false;
        Keyboard.enableRepeatEvents(true);
        this.clearButtonTexture = Texture.getSharedTexture("media/ui/inventoryPanes/Button_Close.png");
        this.clearButtonSize = TextManager.instance.getFontHeight(this.font) - 4;
    }

    public void setFont(UIFont font) {
        Objects.requireNonNull(font);
        this.font = font;
        this.clearButtonSize = TextManager.instance.getFontHeight(this.font) - 4;
    }

    public void ClearHighlights() {
        for (int i = 0; i < 1000; ++i) {
            this.highlightLines[i] = 0;
        }
    }

    public void setMasked(boolean b) {
        if (this.mask == b) {
            return;
        }
        this.mask = b;
        this.text = this.mask ? "*".repeat(this.internalText.length()) : this.internalText;
    }

    public boolean isMasked() {
        return this.mask;
    }

    @Override
    public void onresize() {
        this.Paginate();
    }

    @Override
    public void render() {
        if (!this.isVisible().booleanValue()) {
            return;
        }
        if (this.parent != null && this.parent.maxDrawHeight != -1 && (double)this.parent.maxDrawHeight <= this.y) {
            return;
        }
        if (this.mask) {
            if (this.internalText.length() != this.text.length()) {
                this.text = "*".repeat(this.internalText.length());
            }
        } else {
            this.text = this.internalText;
        }
        super.render();
        this.Paginate();
        int lineHeight = TextManager.instance.getFontFromEnum(this.font).getLineHeight();
        int xInset = this.getInset();
        int yInset = this.getInset();
        if (this.centerVertically) {
            yInset = (int)((this.getHeight() - (double)TextManager.instance.MeasureStringY(this.font, this.internalText)) / 2.0);
        }
        this.keepCursorVisible();
        int right = (int)this.width - xInset;
        if (this.clearButton && this.clearButtonTexture != null && !this.lines.isEmpty()) {
            right -= 2 + this.clearButtonSize + 2;
            float alpha = 0.5f;
            if (!this.selectingRange && this.isMouseOver().booleanValue() && (double)Mouse.getXA() >= this.getAbsoluteX() + (double)right) {
                alpha = 1.0f;
            }
            this.clearButtonTransition.setFadeIn(alpha == 1.0f);
            this.clearButtonTransition.update();
            this.DrawTextureScaledAspect(this.clearButtonTexture, this.width - (float)xInset - 2.0f - (float)this.clearButtonSize, yInset + (lineHeight - this.clearButtonSize) / 2, this.clearButtonSize, this.clearButtonSize, 1.0, 1.0, 1.0, alpha * this.clearButtonTransition.fraction() + 0.35f * (1.0f - this.clearButtonTransition.fraction()));
        }
        Double sx1 = this.clampToParentX(this.getAbsoluteX().intValue() + xInset);
        Double sx2 = this.clampToParentX(this.getAbsoluteX().intValue() + right);
        Double sy1 = this.clampToParentY(this.getAbsoluteY().intValue() + yInset);
        Double sy2 = this.clampToParentY(this.getAbsoluteY().intValue() + (int)this.height - yInset);
        this.setStencilRect(sx1.intValue() - this.getAbsoluteX().intValue(), sy1.intValue() - this.getAbsoluteY().intValue(), sx2.intValue() - sx1.intValue(), sy2.intValue() - sy1.intValue());
        if (!this.lines.isEmpty()) {
            int y = yInset;
            for (int lineIndex = this.topLineIndex; lineIndex < this.topLineIndex + this.numVisibleLines && lineIndex < this.lines.size(); ++lineIndex) {
                if (this.lines.get(lineIndex) == null) continue;
                if (lineIndex >= 0 && lineIndex < this.highlightLines.length) {
                    if (this.highlightLines[lineIndex] == 1) {
                        this.DrawTextureScaledCol(null, xInset - 1, y, this.getWidth().intValue() - xInset * 2 + 2, lineHeight, this.neutralColour);
                    } else if (this.highlightLines[lineIndex] == 2) {
                        this.DrawTextureScaledCol(null, xInset - 1, y, this.getWidth().intValue() - xInset * 2 + 2, lineHeight, this.neutralColour2);
                    } else if (this.highlightLines[lineIndex] == 3) {
                        this.DrawTextureScaledCol(null, xInset - 1, y, this.getWidth().intValue() - xInset * 2 + 2, lineHeight, this.badColour);
                    } else if (this.highlightLines[lineIndex] == 4) {
                        this.DrawTextureScaledCol(null, xInset - 1, y, this.getWidth().intValue() - xInset * 2 + 2, lineHeight, this.goodColour);
                    }
                }
                String text = (String)this.lines.get(lineIndex);
                TextManager.instance.DrawString(this.font, -this.xOffset + this.getAbsoluteX().intValue() + xInset, this.getAbsoluteY().intValue() + y, text, this.textColor.r, this.textColor.g, this.textColor.b, 1.0);
                y += lineHeight;
            }
        }
        consoleHasFocus = this.doingTextEntry;
        if (this.textEntryCursorPos > this.text.length()) {
            this.textEntryCursorPos = this.text.length();
        }
        if (this.toSelectionIndex > this.text.length()) {
            this.toSelectionIndex = this.text.length();
        }
        this.cursorLine = this.toDisplayLine(this.textEntryCursorPos);
        if (this.doingTextEntry) {
            AngelCodeFont fontObj = TextManager.instance.getFontFromEnum(this.font);
            if (this.blinkState) {
                int textOffset = 0;
                if (!this.lines.isEmpty()) {
                    int n = this.textEntryCursorPos - this.textOffsetOfLineStart.get(this.cursorLine);
                    n = Math.min(n, ((String)this.lines.get(this.cursorLine)).length());
                    textOffset = fontObj.getWidth((String)this.lines.get(this.cursorLine), 0, n - 1, true);
                    if (textOffset > 0) {
                        --textOffset;
                    }
                }
                this.DrawTextureScaledCol(Texture.getWhite(), -this.xOffset + xInset + textOffset, yInset + this.cursorLine * lineHeight, 1.0, lineHeight, this.textEntryCursorColour);
            }
            if (!this.lines.isEmpty() && this.toSelectionIndex != this.textEntryCursorPos) {
                int startOffset = Math.min(this.textEntryCursorPos, this.toSelectionIndex);
                int endOffset = Math.max(this.textEntryCursorPos, this.toSelectionIndex);
                int line1 = this.toDisplayLine(startOffset);
                int line2 = this.toDisplayLine(endOffset);
                for (int i = line1; i <= line2; ++i) {
                    int l = this.textOffsetOfLineStart.get(i);
                    int h = l + ((String)this.lines.get(i)).length();
                    l = Math.max(l, startOffset);
                    h = Math.min(h, endOffset);
                    String text = (String)this.lines.get(i);
                    int lx = fontObj.getWidth(text, 0, l - this.textOffsetOfLineStart.get(i) - 1, true);
                    int hx = fontObj.getWidth(text, 0, h - this.textOffsetOfLineStart.get(i) - 1, true);
                    this.DrawTextureScaledCol(null, -this.xOffset + xInset + lx, yInset + i * lineHeight, hx - lx, lineHeight, this.textEntryCursorColour2);
                }
            }
        } else if (this.internalText.isEmpty() && this.getPlaceholderText() != null) {
            TextManager.instance.DrawString(this.font, -this.xOffset + this.getAbsoluteX().intValue() + xInset, this.getAbsoluteY().intValue() + yInset, this.getPlaceholderText(), this.placeholderTextColor.r, this.placeholderTextColor.g, this.placeholderTextColor.b, 1.0);
        }
        this.clearStencilRect();
        if (stencilLevel > 0) {
            this.repaintStencilRect(sx1.intValue() - this.getAbsoluteX().intValue(), sy1.intValue() - this.getAbsoluteY().intValue(), sx2.intValue() - sx1.intValue(), sy2.intValue() - sy1.intValue());
        }
    }

    public float getFrameAlpha() {
        return this.frame.getAlpha();
    }

    public void setFrameAlpha(float alpha) {
        this.frame.setAlpha(alpha);
    }

    public void setTextColor(ColorInfo newColor) {
        this.textColor.set(newColor);
    }

    public void setTextRGBA(float r, float g, float b, float a) {
        this.textColor.set(r, g, b, a);
    }

    private void keepCursorVisible() {
        if (this.lines.isEmpty() || !this.doingTextEntry || this.multipleLine) {
            this.xOffset = 0;
            return;
        }
        if (this.textEntryCursorPos > this.text.length()) {
            this.textEntryCursorPos = this.text.length();
        }
        String text = (String)this.lines.get(0);
        int textWid = TextManager.instance.MeasureStringX(this.font, text);
        int inset = this.getInset();
        int displayWid = this.getWidth().intValue() - inset * 2;
        if (this.clearButton && this.clearButtonTexture != null) {
            displayWid -= 2 + this.clearButtonSize + 2;
        }
        if (textWid <= displayWid) {
            this.xOffset = 0;
        } else if (-this.xOffset + textWid < displayWid) {
            this.xOffset = textWid - displayWid;
        }
        int textWidBeforeCursor = TextManager.instance.MeasureStringX(this.font, text.substring(0, this.textEntryCursorPos));
        int cursorX = -this.xOffset + inset + textWidBeforeCursor - 1;
        if (cursorX < inset) {
            this.xOffset = textWidBeforeCursor;
        } else if (cursorX >= inset + displayWid) {
            this.xOffset = 0;
            int cursorPos = this.getCursorPosFromX(textWidBeforeCursor - displayWid);
            this.xOffset = TextManager.instance.MeasureStringX(this.font, text.substring(0, cursorPos));
            cursorX = -this.xOffset + inset + textWidBeforeCursor - 1;
            if (cursorX >= inset + displayWid) {
                this.xOffset = TextManager.instance.MeasureStringX(this.font, text.substring(0, cursorPos + 1));
            }
            if (-this.xOffset + textWid < displayWid) {
                this.xOffset = textWid - displayWid;
            }
        }
    }

    public String getText() {
        return this.text;
    }

    public String getInternalText() {
        return this.internalText;
    }

    @Override
    public void update() {
        if (this.maxTextLength > -1 && this.internalText.length() > this.maxTextLength) {
            this.internalText = this.internalText.substring(0, this.maxTextLength);
        }
        if (this.forceUpperCase) {
            this.internalText = this.internalText.toUpperCase();
        }
        if (this.mask) {
            if (this.internalText.length() != this.text.length()) {
                Object text = "";
                for (int n = 0; n < this.internalText.length(); ++n) {
                    text = (String)text + "*";
                }
                if (this.doingTextEntry && this.text != text) {
                    this.resetBlink();
                }
                this.text = text;
            }
        } else {
            if (this.doingTextEntry && this.text != this.internalText) {
                this.resetBlink();
            }
            this.text = this.internalText;
        }
        this.Paginate();
        int inset = this.getInset();
        int lineHeight = TextManager.instance.getFontFromEnum(this.font).getLineHeight();
        if ((double)(lineHeight + inset * 2) > this.getHeight()) {
            this.setHeight(lineHeight + inset * 2);
        }
        if (this.frame != null) {
            this.frame.setHeight(this.getHeight());
        }
        this.numVisibleLines = (int)(this.getHeight() - (double)(inset * 2)) / lineHeight;
        if (this.blinkFrame > 0.0f) {
            this.blinkFrame = UIManager.defaultthread == LuaManager.debugthread ? (this.blinkFrame -= 1.0f) : (this.blinkFrame -= GameTime.getInstance().getRealworldSecondsSinceLastUpdate() * 30.0f);
        } else {
            this.blinkState = !this.blinkState;
            this.blinkFrame = this.blinkState ? 12.0f : 8.0f;
        }
        if (this.numVisibleLines * lineHeight + inset * 2 < this.getHeight().intValue()) {
            if (this.numVisibleLines < this.lines.size()) {
                this.setScrollHeight((this.lines.size() + 1) * lineHeight);
            }
            ++this.numVisibleLines;
        } else {
            this.setScrollHeight(this.lines.size() * lineHeight);
        }
        if (UIDebugConsole.instance == null || this != UIDebugConsole.instance.outputLog) {
            this.topLineIndex = (int)(-this.getYScroll().doubleValue() + (double)inset) / lineHeight;
        }
        this.setYScroll(-this.topLineIndex * lineHeight);
    }

    private void Paginate() {
        boolean bPaginate = this.alwaysPaginate;
        if (!this.alwaysPaginate) {
            if (this.paginateFont != this.font) {
                this.paginateFont = this.font;
                bPaginate = true;
            }
            if (this.paginateWidth != this.getWidth().intValue()) {
                this.paginateWidth = this.getWidth().intValue();
                bPaginate = true;
            }
            if (this.textChanged) {
                this.textChanged = false;
                bPaginate = true;
            }
            if (!bPaginate) {
                return;
            }
        }
        this.lines.clear();
        this.textOffsetOfLineStart.resetQuick();
        if (this.text.isEmpty()) {
            return;
        }
        if (!this.multipleLine) {
            this.lines.add(this.text);
            this.textOffsetOfLineStart.add(0);
            return;
        }
        String[] textarr = this.text.split("\n", -1);
        int textOffset = 0;
        block0: for (String text : textarr) {
            int n = 0;
            if (text.isEmpty()) {
                this.lines.add(this.multipleLine ? "" : " ");
                this.textOffsetOfLineStart.add(textOffset);
                ++textOffset;
                continue;
            }
            do {
                int m;
                int z;
                if ((z = (m = text.indexOf(" ", n + 1))) == -1) {
                    z = text.length();
                }
                int wid = TextManager.instance.MeasureStringX(this.font, text.substring(0, z));
                int scrollBarWid = 17;
                if (this.wrapLines && (double)wid >= this.getWidth() - (double)(this.getInset() * 2) - 17.0 && n > 0) {
                    String sub = text.substring(0, n);
                    text = text.substring(n + 1);
                    this.lines.add(sub);
                    this.textOffsetOfLineStart.add(textOffset);
                    textOffset += sub.length() + 1;
                    m = 0;
                } else if (m == -1) {
                    this.lines.add(text);
                    this.textOffsetOfLineStart.add(textOffset);
                    textOffset += text.length() + 1;
                    continue block0;
                }
                n = m;
            } while (!text.isEmpty());
        }
    }

    public int getInset() {
        int inset = 2;
        if (this.hasFrame) {
            inset = 5;
        }
        return inset;
    }

    public void setEditable(boolean b) {
        this.isEditable = b;
    }

    @Override
    public boolean isEditable() {
        return this.isEditable;
    }

    public void setSelectable(boolean b) {
        this.isSelectable = b;
    }

    public boolean isSelectable() {
        return this.isSelectable;
    }

    @Override
    public Boolean onMouseUp(double x, double y) {
        if (!this.isVisible().booleanValue()) {
            return false;
        }
        super.onMouseUp(x, y);
        this.selectingRange = false;
        return Boolean.TRUE;
    }

    @Override
    public void onMouseUpOutside(double x, double y) {
        if (!this.isVisible().booleanValue()) {
            return;
        }
        super.onMouseUpOutside(x, y);
        this.selectingRange = false;
    }

    @Override
    public Boolean onMouseMove(double dx, double dy) {
        int mx = Mouse.getXA();
        int my = Mouse.getYA();
        if (!this.isVisible().booleanValue()) {
            return Boolean.FALSE;
        }
        boolean consume = this.isConsumeMouseEvents();
        this.setConsumeMouseEvents(false);
        Boolean handled = super.onMouseMove(dx, dy);
        this.setConsumeMouseEvents(consume);
        if (handled.booleanValue()) {
            return Boolean.TRUE;
        }
        if ((this.isEditable || this.isSelectable) && this.selectingRange) {
            if (this.multipleLine) {
                int inset = this.getInset();
                int lineHeight = TextManager.instance.getFontFromEnum(this.font).getLineHeight();
                this.cursorLine = (my - this.getAbsoluteY().intValue() - inset - this.getYScroll().intValue()) / lineHeight;
                if (this.cursorLine > this.lines.size() - 1) {
                    this.cursorLine = this.lines.size() - 1;
                }
            }
            this.textEntryCursorPos = this.getCursorPosFromX((int)((double)mx - this.getAbsoluteX()));
            return Boolean.TRUE;
        }
        return Boolean.FALSE;
    }

    @Override
    public void onMouseMoveOutside(double dx, double dy) {
        int mx = Mouse.getXA();
        int my = Mouse.getYA();
        if (!Mouse.isButtonDown(0)) {
            this.selectingRange = false;
        }
        if (!this.isVisible().booleanValue()) {
            return;
        }
        super.onMouseMoveOutside(dx, dy);
        if ((this.isEditable || this.isSelectable) && this.selectingRange) {
            if (this.multipleLine) {
                int inset = this.getInset();
                int lineHeight = TextManager.instance.getFontFromEnum(this.font).getLineHeight();
                this.cursorLine = (my - this.getAbsoluteY().intValue() - inset - this.getYScroll().intValue()) / lineHeight;
                if (this.cursorLine < 0) {
                    this.cursorLine = 0;
                }
                if (this.cursorLine > this.lines.size() - 1) {
                    this.cursorLine = this.lines.size() - 1;
                }
            }
            this.textEntryCursorPos = this.getCursorPosFromX((int)((double)mx - this.getAbsoluteX()));
        }
    }

    public void focus() {
        this.doingTextEntry = true;
        Core.currentTextEntryBox = this;
    }

    public void unfocus() {
        this.doingTextEntry = false;
        if (Core.currentTextEntryBox == this) {
            Core.currentTextEntryBox = null;
        }
        this.onLostFocus();
    }

    public void ignoreFirstInput() {
        this.ignoreFirst = true;
    }

    @Override
    public Boolean onMouseDown(double x, double y) {
        if (!this.isVisible().booleanValue()) {
            return Boolean.FALSE;
        }
        if (!this.getControls().isEmpty()) {
            for (int i = 0; i < this.getControls().size(); ++i) {
                UIElement ui = this.getControls().get(i);
                if (ui == this.frame || !ui.isMouseOver().booleanValue()) continue;
                return ui.onMouseDown(x - (double)ui.getXScrolled(this).intValue(), y - (double)ui.getYScrolled(this).intValue()) != false ? Boolean.TRUE : Boolean.FALSE;
            }
        }
        if (this.clearButton && this.clearButtonTexture != null && !this.lines.isEmpty()) {
            int right = this.getWidth().intValue() - this.getInset();
            if (x >= (double)(right -= 2 + this.clearButtonSize + 2)) {
                this.clearInput();
                return Boolean.TRUE;
            }
        }
        if (this.multipleLine) {
            int inset = this.getInset();
            int lineHeight = TextManager.instance.getFontFromEnum(this.font).getLineHeight();
            this.cursorLine = ((int)y - inset - this.getYScroll().intValue()) / lineHeight;
            if (this.cursorLine > this.lines.size() - 1) {
                this.cursorLine = this.lines.size() - 1;
            }
        }
        if (this.isEditable || this.isSelectable) {
            if (Core.currentTextEntryBox != this) {
                if (Core.currentTextEntryBox != null) {
                    Core.currentTextEntryBox.setDoingTextEntry(false);
                    if (Core.currentTextEntryBox.getFrame() != null) {
                        Core.currentTextEntryBox.getFrame().color = this.standardFrameColour;
                    }
                }
                Core.currentTextEntryBox = this;
                Core.currentTextEntryBox.setSelectingRange(true);
            }
            if (!this.doingTextEntry) {
                this.focus();
                this.toSelectionIndex = this.textEntryCursorPos = this.getCursorPosFromX((int)x);
                if (this.frame != null) {
                    this.frame.color = this.textEntryFrameColour;
                }
            } else {
                this.toSelectionIndex = this.textEntryCursorPos = this.getCursorPosFromX((int)x);
            }
        } else {
            if (this.frame != null) {
                this.frame.color = this.standardFrameColour;
            }
            this.doingTextEntry = false;
            return Boolean.FALSE;
        }
        return Boolean.TRUE;
    }

    private int getCursorPosFromX(int x) {
        if (this.lines.isEmpty()) {
            return 0;
        }
        String text = (String)this.lines.get(this.cursorLine);
        if (text.isEmpty()) {
            return this.textOffsetOfLineStart.get(this.cursorLine);
        }
        if (x + this.xOffset < 0) {
            return this.textOffsetOfLineStart.get(this.cursorLine);
        }
        for (int n = 0; n <= text.length(); ++n) {
            int wid;
            String str = "";
            if (n > 0) {
                str = text.substring(0, n);
            }
            if ((wid = TextManager.instance.MeasureStringX(this.font, str)) <= x + this.xOffset || n < 0) continue;
            return this.textOffsetOfLineStart.get(this.cursorLine) + n - 1;
        }
        return this.textOffsetOfLineStart.get(this.cursorLine) + text.length();
    }

    public void updateText() {
        if (this.mask) {
            Object text = "";
            for (int n = 0; n < this.internalText.length(); ++n) {
                text = (String)text + "*";
            }
            this.text = text;
        } else {
            this.text = this.internalText;
        }
    }

    public void SetText(String text) {
        int n;
        this.internalText = text;
        if (this.mask) {
            text = "";
            for (n = 0; n < this.internalText.length(); ++n) {
                text = (String)text + "*";
            }
            this.text = text;
        } else {
            this.text = text;
        }
        this.toSelectionIndex = this.textEntryCursorPos = ((String)text).length();
        this.update();
        this.toSelectionIndex = 0;
        this.textEntryCursorPos = 0;
        if (!this.lines.isEmpty()) {
            n = this.lines.size() - 1;
            this.textEntryCursorPos = this.toSelectionIndex = this.textOffsetOfLineStart.get(n) + ((String)this.lines.get(n)).length();
        }
    }

    public void setPlaceholderText(String text) {
        this.placeholderText = StringUtils.discardNullOrWhitespace(text);
    }

    public String getPlaceholderText() {
        return this.placeholderText;
    }

    public void setPlaceholderTextColor(ColorInfo color) {
        this.placeholderTextColor.set(color);
    }

    public void setPlaceholderTextRGBA(float r, float g, float b, float a) {
        this.placeholderTextColor.set(r, g, b, a);
    }

    public void clearInput() {
        this.text = "";
        this.internalText = "";
        this.textEntryCursorPos = 0;
        this.toSelectionIndex = 0;
        this.update();
        this.onTextChange();
    }

    public void onPressUp() {
        if (this.getTable() != null && UIManager.tableget(this.table, "onPressUp") != null) {
            Object[] objectArray = LuaManager.caller.pcall(LuaManager.thread, UIManager.tableget(this.table, "onPressUp"), (Object)this.getTable());
        }
    }

    public void onPressDown() {
        if (this.getTable() != null && UIManager.tableget(this.table, "onPressDown") != null) {
            Object[] objectArray = LuaManager.caller.pcall(LuaManager.thread, UIManager.tableget(this.table, "onPressDown"), (Object)this.getTable());
        }
    }

    public void onCommandEntered() {
        if (this.getTable() != null && UIManager.tableget(this.table, "onCommandEntered") != null) {
            Object[] objectArray = LuaManager.caller.pcall(LuaManager.thread, UIManager.tableget(this.table, "onCommandEntered"), (Object)this.getTable());
        }
    }

    public void onTextChange() {
        if (this.getTable() != null && UIManager.tableget(this.table, "onTextChange") != null) {
            Object[] objectArray = LuaManager.caller.pcall(LuaManager.thread, UIManager.tableget(this.table, "onTextChange"), (Object)this.getTable());
        }
    }

    @Override
    public void onOtherKey(int key) {
        if (this.getTable() != null && UIManager.tableget(this.table, "onOtherKey") != null) {
            Object[] objectArray = LuaManager.caller.pcall(LuaManager.thread, UIManager.tableget(this.table, "onOtherKey"), this.getTable(), key);
        }
    }

    public void onLostFocus() {
        if (this.getTable() != null && UIManager.tableget(this.table, "onLostFocus") != null) {
            Object[] objectArray = LuaManager.caller.pcall(LuaManager.thread, UIManager.tableget(this.table, "onLostFocus"), (Object)this.getTable());
        }
    }

    public int getMaxTextLength() {
        return this.maxTextLength;
    }

    public void setMaxTextLength(int maxTextLength) {
        this.maxTextLength = maxTextLength;
    }

    public boolean getForceUpperCase() {
        return this.forceUpperCase;
    }

    public void setForceUpperCase(boolean forceUpperCase) {
        this.forceUpperCase = forceUpperCase;
    }

    public boolean getHasFrame() {
        return this.hasFrame;
    }

    public void setHasFrame(boolean hasFrame) {
        if (this.hasFrame == hasFrame) {
            return;
        }
        this.hasFrame = hasFrame;
        if (this.hasFrame) {
            this.frame = new UINineGrid(0, 0, (int)this.width, (int)this.height, 5, 5, 5, 5, "media/ui/Box_TopLeft.png", "media/ui/Box_Top.png", "media/ui/Box_TopRight.png", "media/ui/Box_Left.png", "media/ui/Box_Center.png", "media/ui/Box_Right.png", "media/ui/Box_BottomLeft.png", "media/ui/Box_Bottom.png", "media/ui/Box_BottomRight.png");
            this.frame.setAnchorRight(true);
            this.AddChild(this.frame);
        } else {
            this.RemoveChild(this.frame);
            this.frame = null;
        }
    }

    public void setClearButton(boolean hasButton) {
        this.clearButton = hasButton;
        if (this.clearButton && this.clearButtonTransition == null) {
            this.clearButtonTransition = new UITransition();
        }
    }

    public int toDisplayLine(int textOffset) {
        for (int i = 0; i < this.lines.size(); ++i) {
            if (textOffset < this.textOffsetOfLineStart.get(i) || textOffset > this.textOffsetOfLineStart.get(i) + ((String)this.lines.get(i)).length()) continue;
            return i;
        }
        return 0;
    }

    public void setMultipleLine(boolean multiple) {
        this.multipleLine = multiple;
    }

    public boolean isMultipleLine() {
        return this.multipleLine;
    }

    public int getCursorLine() {
        return this.cursorLine;
    }

    public void setCursorLine(int line) {
        this.cursorLine = line;
    }

    public int getCursorPos() {
        return this.textEntryCursorPos;
    }

    public void setCursorPos(int charIndex) {
        if (this.multipleLine) {
            if (this.cursorLine >= 0 && this.cursorLine < this.lines.size()) {
                this.textEntryCursorPos = PZMath.clamp(charIndex, 0, ((String)this.lines.get(this.cursorLine)).length());
            }
        } else {
            this.textEntryCursorPos = PZMath.clamp(charIndex, 0, this.internalText.length());
        }
        this.toSelectionIndex = this.textEntryCursorPos;
    }

    public int getMaxLines() {
        return this.maxLines;
    }

    public void setMaxLines(int maxLines) {
        this.maxLines = maxLines;
    }

    public boolean isFocused() {
        return this.doingTextEntry;
    }

    @Override
    public boolean isOnlyNumbers() {
        return this.onlyNumbers;
    }

    public void setOnlyNumbers(boolean onlyNumbers) {
        this.onlyNumbers = onlyNumbers;
    }

    @Override
    public boolean isOnlyText() {
        return this.onlyText;
    }

    public void setOnlyText(boolean onlyText) {
        this.onlyText = onlyText;
    }

    public void resetBlink() {
        this.blinkState = true;
        this.blinkFrame = 12.0f;
    }

    @Override
    public void selectAll() {
        this.textEntryCursorPos = this.internalText.length();
        this.toSelectionIndex = 0;
    }

    @Override
    public boolean isDoingTextEntry() {
        return this.doingTextEntry;
    }

    @Override
    public void setDoingTextEntry(boolean value) {
        this.doingTextEntry = value;
        if (!this.doingTextEntry) {
            this.onLostFocus();
        }
    }

    @Override
    public UINineGrid getFrame() {
        return this.frame;
    }

    @Override
    public boolean isIgnoreFirst() {
        return this.ignoreFirst;
    }

    @Override
    public void setIgnoreFirst(boolean value) {
        this.ignoreFirst = value;
    }

    @Override
    public void setSelectingRange(boolean value) {
        this.selectingRange = value;
    }

    @Override
    public Color getStandardFrameColour() {
        return this.standardFrameColour;
    }

    @Override
    public void onKeyEnter() {
        boolean commandEntered = false;
        if (UIManager.getDebugConsole() != null && this == UIManager.getDebugConsole().commandLine) {
            commandEntered = true;
        }
        if (this.multipleLine) {
            if (this.lines.size() < this.getMaxLines()) {
                if (this.textEntryCursorPos != this.toSelectionIndex) {
                    int l = Math.min(this.textEntryCursorPos, this.toSelectionIndex);
                    int h = Math.max(this.textEntryCursorPos, this.toSelectionIndex);
                    this.internalText = !this.internalText.isEmpty() ? this.internalText.substring(0, l) + "\n" + this.internalText.substring(h) : "\n";
                    this.textEntryCursorPos = l + 1;
                } else {
                    int textOffset = this.textEntryCursorPos;
                    String text = this.internalText.substring(0, textOffset) + "\n" + this.internalText.substring(textOffset);
                    this.SetText(text);
                    this.textEntryCursorPos = textOffset + 1;
                }
                this.toSelectionIndex = this.textEntryCursorPos;
                this.cursorLine = this.toDisplayLine(this.textEntryCursorPos);
            }
        } else {
            this.onCommandEntered();
        }
        if (commandEntered && (!GameClient.client || IsoPlayer.getInstance().getRole().hasCapability(Capability.UIManagerProcessCommands) || GameClient.connection != null && GameClient.connection.isCoopHost)) {
            UIManager.getDebugConsole().ProcessCommand();
        }
    }

    @Override
    public void onKeyHome() {
        boolean isShiftKeyDown = GameKeyboard.isKeyDownRaw(42) || GameKeyboard.isKeyDownRaw(54);
        this.textEntryCursorPos = 0;
        if (!this.lines.isEmpty()) {
            this.textEntryCursorPos = this.textOffsetOfLineStart.get(this.cursorLine);
        }
        if (!isShiftKeyDown) {
            this.toSelectionIndex = this.textEntryCursorPos;
        }
        this.resetBlink();
    }

    @Override
    public void onKeyEnd() {
        boolean isShiftKeyDown = GameKeyboard.isKeyDownRaw(42) || GameKeyboard.isKeyDownRaw(54);
        this.textEntryCursorPos = this.internalText.length();
        if (!this.lines.isEmpty()) {
            this.textEntryCursorPos = this.textOffsetOfLineStart.get(this.cursorLine) + ((String)this.lines.get(this.cursorLine)).length();
        }
        if (!isShiftKeyDown) {
            this.toSelectionIndex = this.textEntryCursorPos;
        }
        this.resetBlink();
    }

    @Override
    public void onKeyUp() {
        boolean isShiftKeyDown;
        boolean bl = isShiftKeyDown = GameKeyboard.isKeyDownRaw(42) || GameKeyboard.isKeyDownRaw(54);
        if (this.cursorLine > 0) {
            int column = this.textEntryCursorPos - this.textOffsetOfLineStart.get(this.cursorLine);
            --this.cursorLine;
            if (column > ((String)this.lines.get(this.cursorLine)).length()) {
                column = ((String)this.lines.get(this.cursorLine)).length();
            }
            this.textEntryCursorPos = this.textOffsetOfLineStart.get(this.cursorLine) + column;
            if (!isShiftKeyDown) {
                this.toSelectionIndex = this.textEntryCursorPos;
            }
        }
        this.onPressUp();
    }

    @Override
    public void onKeyDown() {
        boolean isShiftKeyDown;
        boolean bl = isShiftKeyDown = GameKeyboard.isKeyDownRaw(42) || GameKeyboard.isKeyDownRaw(54);
        if (this.lines.size() - 1 > this.cursorLine && this.cursorLine + 1 < this.getMaxLines()) {
            int column = this.textEntryCursorPos - this.textOffsetOfLineStart.get(this.cursorLine);
            ++this.cursorLine;
            if (column > ((String)this.lines.get(this.cursorLine)).length()) {
                column = ((String)this.lines.get(this.cursorLine)).length();
            }
            this.textEntryCursorPos = this.textOffsetOfLineStart.get(this.cursorLine) + column;
            if (!isShiftKeyDown) {
                this.toSelectionIndex = this.textEntryCursorPos;
            }
        }
        this.onPressDown();
    }

    @Override
    public void onKeyLeft() {
        boolean isShiftKeyDown = GameKeyboard.isKeyDownRaw(42) || GameKeyboard.isKeyDownRaw(54);
        --this.textEntryCursorPos;
        if (this.textEntryCursorPos < 0) {
            this.textEntryCursorPos = 0;
        }
        if (!isShiftKeyDown) {
            this.toSelectionIndex = this.textEntryCursorPos;
        }
        this.resetBlink();
    }

    @Override
    public void onKeyRight() {
        boolean isShiftKeyDown = GameKeyboard.isKeyDownRaw(42) || GameKeyboard.isKeyDownRaw(54);
        ++this.textEntryCursorPos;
        if (this.textEntryCursorPos > this.internalText.length()) {
            this.textEntryCursorPos = this.internalText.length();
        }
        if (!isShiftKeyDown) {
            this.toSelectionIndex = this.textEntryCursorPos;
        }
        this.resetBlink();
    }

    void onTextDelete() {
        int l = Math.min(this.textEntryCursorPos, this.toSelectionIndex);
        int h = Math.max(this.textEntryCursorPos, this.toSelectionIndex);
        this.internalText = this.internalText.substring(0, l) + this.internalText.substring(h);
        this.cursorLine = this.toDisplayLine(l);
        this.toSelectionIndex = l;
        this.textEntryCursorPos = l;
        this.onTextChange();
    }

    @Override
    public void onKeyBack() {
        if (this.textEntryCursorPos != this.toSelectionIndex) {
            this.onTextDelete();
            return;
        }
        if (this.internalText.isEmpty() || this.textEntryCursorPos <= 0) {
            return;
        }
        if (this.textEntryCursorPos > this.internalText.length()) {
            this.internalText = this.internalText.substring(0, this.internalText.length() - 1);
        } else {
            int textOffset = this.textEntryCursorPos;
            this.internalText = this.internalText.substring(0, textOffset - 1) + this.internalText.substring(textOffset);
        }
        --this.textEntryCursorPos;
        this.toSelectionIndex = this.textEntryCursorPos;
        this.onTextChange();
    }

    @Override
    public void onKeyDelete() {
        if (this.textEntryCursorPos != this.toSelectionIndex) {
            this.onTextDelete();
            return;
        }
        if (this.internalText.isEmpty() || this.textEntryCursorPos >= this.internalText.length()) {
            return;
        }
        this.internalText = this.textEntryCursorPos > 0 ? this.internalText.substring(0, this.textEntryCursorPos) + this.internalText.substring(this.textEntryCursorPos + 1) : this.internalText.substring(1);
        this.onTextChange();
    }

    @Override
    public void pasteFromClipboard() {
        String clip = Clipboard.getClipboard();
        if (clip == null) {
            return;
        }
        if (this.textEntryCursorPos != this.toSelectionIndex) {
            int l = Math.min(this.textEntryCursorPos, this.toSelectionIndex);
            int h = Math.max(this.textEntryCursorPos, this.toSelectionIndex);
            this.internalText = this.internalText.substring(0, l) + clip + this.internalText.substring(h);
            this.toSelectionIndex = l + clip.length();
            this.textEntryCursorPos = l + clip.length();
        } else {
            this.internalText = this.textEntryCursorPos < this.internalText.length() ? this.internalText.substring(0, this.textEntryCursorPos) + clip + this.internalText.substring(this.textEntryCursorPos) : this.internalText + clip;
            this.textEntryCursorPos += clip.length();
            this.toSelectionIndex += clip.length();
        }
        this.onTextChange();
    }

    @Override
    public void cutToClipboard() {
        if (this.textEntryCursorPos == this.toSelectionIndex) {
            return;
        }
        this.updateText();
        int l = Math.min(this.textEntryCursorPos, this.toSelectionIndex);
        int h = Math.max(this.textEntryCursorPos, this.toSelectionIndex);
        String newClip = this.text.substring(l, h);
        if (!newClip.isEmpty()) {
            Clipboard.setClipboard(newClip);
        }
        this.internalText = this.internalText.substring(0, l) + this.internalText.substring(h);
        this.toSelectionIndex = l;
        this.textEntryCursorPos = l;
    }

    @Override
    public void copyToClipboard() {
        if (this.textEntryCursorPos == this.toSelectionIndex) {
            return;
        }
        this.updateText();
        int l = Math.min(this.textEntryCursorPos, this.toSelectionIndex);
        int h = Math.max(this.textEntryCursorPos, this.toSelectionIndex);
        String newClip = this.text.substring(l, h);
        if (!newClip.isEmpty()) {
            Clipboard.setClipboard(newClip);
        }
    }

    @Override
    public boolean isTextLimit() {
        return this.internalText.length() >= this.textEntryMaxLength;
    }

    @Override
    public void putCharacter(char eventChar) {
        if (this.textEntryCursorPos == this.toSelectionIndex) {
            int textOffset = this.textEntryCursorPos;
            this.internalText = textOffset < this.internalText.length() ? this.internalText.substring(0, textOffset) + eventChar + this.internalText.substring(textOffset) : this.internalText + eventChar;
            ++this.textEntryCursorPos;
            ++this.toSelectionIndex;
            this.onTextChange();
        } else {
            int l = Math.min(this.textEntryCursorPos, this.toSelectionIndex);
            int h = Math.max(this.textEntryCursorPos, this.toSelectionIndex);
            this.internalText = !this.internalText.isEmpty() ? this.internalText.substring(0, l) + eventChar + this.internalText.substring(h) : "" + eventChar;
            this.toSelectionIndex = l + 1;
            this.textEntryCursorPos = l + 1;
            this.onTextChange();
        }
    }

    public void setWrapLines(boolean b) {
        this.wrapLines = b;
    }

    public void setCentreVertically(boolean b) {
        this.centerVertically = b;
    }
}

