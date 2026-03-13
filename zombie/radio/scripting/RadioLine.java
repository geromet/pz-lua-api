/*
 * Decompiled with CFR 0.152.
 */
package zombie.radio.scripting;

import zombie.UsedFromLua;

@UsedFromLua
public final class RadioLine {
    private final float r;
    private final float g;
    private final float b;
    private String text = "<!text missing!>";
    private String effects = "";
    private float airTime = -1.0f;

    public RadioLine(String txt, float red, float green, float blue) {
        this(txt, red, green, blue, null);
    }

    public RadioLine(String txt, float red, float green, float blue, String fx) {
        this.text = txt != null ? txt : this.text;
        this.r = red;
        this.g = green;
        this.b = blue;
        this.effects = fx != null ? fx : this.effects;
    }

    public float getR() {
        return this.r;
    }

    public float getG() {
        return this.g;
    }

    public float getB() {
        return this.b;
    }

    public String getText() {
        return this.text;
    }

    public String getEffectsString() {
        return this.effects;
    }

    public boolean isCustomAirTime() {
        return this.airTime > 0.0f;
    }

    public float getAirTime() {
        return this.airTime;
    }

    public void setAirTime(float airTime) {
        this.airTime = airTime;
    }

    public void setText(String text) {
        this.text = text;
    }
}

