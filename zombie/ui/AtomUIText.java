/*
 * Decompiled with CFR 0.152.
 */
package zombie.ui;

import java.util.ArrayList;
import se.krka.kahlua.vm.KahluaTable;
import zombie.IndieGL;
import zombie.UsedFromLua;
import zombie.core.SpriteRenderer;
import zombie.core.fonts.AngelCodeFont;
import zombie.debug.DebugOptions;
import zombie.ui.AtomUI;
import zombie.ui.TextManager;
import zombie.ui.UIFont;
import zombie.ui.UIManager;

@UsedFromLua
public class AtomUIText
extends AtomUI {
    AngelCodeFont fontToUse;
    String text;
    double textTracking;
    double textLeading;
    int autoWidth = -1;
    float outlineThick;
    float outlineColorR;
    float outlineColorG;
    float outlineColorB;
    float outlineColorA;
    boolean shadow;
    float shadowValue;
    private int charNum;
    private int textWidth;
    private int textHeight;
    private int realTextHeight;
    private static char[] data = new char[256];
    ArrayList<CharData> textData = new ArrayList();

    public AtomUIText(KahluaTable table) {
        super(table);
    }

    @Override
    public void render() {
        if (!this.visible) {
            return;
        }
        this.drawText();
        super.render();
    }

    @Override
    public void init() {
        super.init();
        this.updateInternalValues();
    }

    void drawText() {
        DebugOptions.instance.isoSprite.forceNearestMagFilter.setValue(false);
        TextManager.sdfShader.updateThreshold(this.getSdfThreshold());
        TextManager.sdfShader.updateShadow(this.shadowValue);
        TextManager.sdfShader.updateOutline(this.outlineThick, this.outlineColorR, this.outlineColorG, this.outlineColorB, this.outlineColorA);
        IndieGL.StartShader(TextManager.sdfShader);
        double dx = this.pivotX * (double)this.textWidth;
        double dy = this.pivotY * (double)this.textHeight;
        for (CharData ch : this.textData) {
            double x0 = ch.x + (double)ch.def.xoffset - dx;
            double y0 = ch.y + (double)ch.def.yoffset - dy;
            double x1 = x0 + (double)ch.def.width;
            double y1 = y0 + (double)ch.def.height;
            double[] leftTop = this.getAbsolutePosition(x0, y0);
            double[] rightTop = this.getAbsolutePosition(x1, y0);
            double[] rightDown = this.getAbsolutePosition(x1, y1);
            double[] leftDown = this.getAbsolutePosition(x0, y1);
            SpriteRenderer.instance.render(ch.def.image, leftTop[0], leftTop[1], rightTop[0], rightTop[1], rightDown[0], rightDown[1], leftDown[0], leftDown[1], this.colorR, this.colorG, this.colorB, this.colorA, null);
        }
        IndieGL.EndShader();
    }

    float getSdfThreshold() {
        double[] p0 = this.getAbsolutePosition(-5.0, 0.0);
        double[] p1 = this.getAbsolutePosition(5.0, 0.0);
        double distance = Math.hypot(p0[0] - p1[0], p0[1] - p1[1]);
        return (float)(0.125 / (distance / 10.0));
    }

    @Override
    void loadFromTable() {
        super.loadFromTable();
        this.fontToUse = TextManager.instance.getFontFromEnum(this.tryGetFont("font", UIFont.SdfRegular));
        this.text = this.tryGetString("text", "");
        this.textTracking = this.tryGetDouble("textTracking", 0.0);
        this.textLeading = this.tryGetDouble("textLeading", 0.0);
        this.autoWidth = (int)this.tryGetDouble("autoWidth", -1.0);
        this.outlineThick = (float)this.tryGetDouble("outlineThick", 0.0);
        this.outlineColorR = (float)this.tryGetDouble("outlineColorR", 0.0);
        this.outlineColorG = (float)this.tryGetDouble("outlineColorG", 0.0);
        this.outlineColorB = (float)this.tryGetDouble("outlineColorB", 0.0);
        this.outlineColorA = (float)this.tryGetDouble("outlineColorA", 0.0);
        this.shadow = this.tryGetBoolean("shadow", false);
        this.shadowValue = this.shadow ? 1.0f : 0.0f;
    }

    void updateCharData(AngelCodeFont.CharDef def, double x, double y) {
        if (this.charNum >= this.textData.size()) {
            this.textData.add(new CharData());
        }
        CharData chData = this.textData.get(this.charNum);
        chData.def = def;
        chData.x = x;
        chData.y = y;
        ++this.charNum;
    }

    @Override
    void updateInternalValues() {
        int id;
        int i;
        super.updateInternalValues();
        this.textWidth = 0;
        this.textHeight = this.fontToUse.getHeight(this.text);
        this.textData.clear();
        this.charNum = 0;
        int numChars = this.text.length();
        if (data.length < numChars) {
            data = new char[(numChars + 128 - 1) / 128 * 128];
        }
        this.text.getChars(0, numChars, data, 0);
        ArrayList<Double> addTracking = new ArrayList<Double>();
        int lastSpace = -1;
        int startIndex = 0;
        double diff = 0.0;
        float x = 0.0f;
        float y = 0.0f;
        float diffX = 0.0f;
        AngelCodeFont.CharDef lastCharDef = null;
        int spaceNum = 0;
        if (this.autoWidth != -1) {
            for (i = 0; i < numChars; ++i) {
                AngelCodeFont.CharDef charDef;
                id = data[i];
                if (id == 10) {
                    x = 0.0f;
                    for (int j = startIndex; j <= i; ++j) {
                        addTracking.add(0.0);
                    }
                    startIndex = i + 1;
                    lastSpace = -1;
                    spaceNum = 0;
                    y += 1.0f;
                    continue;
                }
                if (id == 32) {
                    diff = (float)this.autoWidth - x;
                    if (y == 0.0f && lastCharDef != null) {
                        diff -= (double)lastCharDef.xadvance / 2.0;
                    }
                    lastSpace = i;
                    diffX = x;
                    ++spaceNum;
                }
                if (x >= (float)this.autoWidth && lastSpace != -1) {
                    AtomUIText.data[lastSpace] = 10;
                    boolean isStartLineSpace = true;
                    for (int j = startIndex; j <= lastSpace; ++j) {
                        if (data[j] == ' ') {
                            if (!isStartLineSpace) {
                                addTracking.add(diff / (double)(spaceNum - 1));
                                continue;
                            }
                            --spaceNum;
                            addTracking.add(0.0);
                            continue;
                        }
                        isStartLineSpace = false;
                        addTracking.add(0.0);
                    }
                    startIndex = lastSpace + 1;
                    lastSpace = -1;
                    spaceNum = 0;
                    y += 1.0f;
                    x -= diffX;
                }
                if (id >= this.fontToUse.chars.length) {
                    id = 63;
                }
                if ((charDef = this.fontToUse.chars[id]) == null) continue;
                if (lastCharDef != null) {
                    x += (float)lastCharDef.getKerning(id);
                }
                lastCharDef = charDef;
                x = (float)((double)x + ((double)charDef.xadvance + this.textTracking));
            }
        }
        x = 0.0f;
        y = 0.0f;
        lastCharDef = null;
        for (i = 0; i < numChars; ++i) {
            AngelCodeFont.CharDef charDef;
            id = data[i];
            if (id == 10) {
                this.textWidth = (int)Math.max(x, (float)this.textWidth);
                x = 0.0f;
                y = (float)((double)y + ((double)this.fontToUse.getLineHeight() + this.textLeading));
                continue;
            }
            if (id >= this.fontToUse.chars.length) {
                id = 63;
            }
            if ((charDef = this.fontToUse.chars[id]) == null) continue;
            if (lastCharDef != null) {
                x += (float)lastCharDef.getKerning(id);
            }
            lastCharDef = charDef;
            this.updateCharData(charDef, x, y);
            x = (float)((double)x + ((double)charDef.xadvance + this.textTracking));
            if (i >= addTracking.size()) continue;
            x = (float)((double)x + (Double)addTracking.get(i));
        }
        this.textWidth = (int)Math.max(x, (float)this.textWidth);
        this.realTextHeight = (int)Math.max(y, (float)this.textHeight);
    }

    public void setFont(UIFont font) {
        this.fontToUse = TextManager.instance.getFontFromEnum(font);
        this.updateInternalValues();
    }

    public void setText(String text) {
        this.text = text;
        this.updateInternalValues();
    }

    public void setAutoWidth(Double width) {
        this.autoWidth = width.intValue();
        this.updateInternalValues();
    }

    public Double getTextHeight() {
        return this.realTextHeight;
    }

    public Double getTextWidth() {
        return this.textWidth;
    }

    UIFont tryGetFont(String key, UIFont defaultValue) {
        UIFont uiFont;
        Object value = UIManager.tableget(this.table, key);
        return value instanceof UIFont ? (uiFont = (UIFont)((Object)value)) : defaultValue;
    }

    static class CharData {
        public AngelCodeFont.CharDef def;
        public double x;
        public double y;

        CharData() {
        }
    }
}

