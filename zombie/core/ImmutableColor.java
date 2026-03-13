/*
 * Decompiled with CFR 0.152.
 */
package zombie.core;

import zombie.UsedFromLua;
import zombie.core.Color;
import zombie.core.math.PZMath;
import zombie.core.random.Rand;

@UsedFromLua
public final class ImmutableColor {
    public static final ImmutableColor transparent = new ImmutableColor(0.0f, 0.0f, 0.0f, 0.0f);
    public static final ImmutableColor white = new ImmutableColor(1.0f, 1.0f, 1.0f, 1.0f);
    public static final ImmutableColor yellow = new ImmutableColor(1.0f, 1.0f, 0.0f, 1.0f);
    public static final ImmutableColor red = new ImmutableColor(1.0f, 0.0f, 0.0f, 1.0f);
    public static final ImmutableColor purple = new ImmutableColor(196.0f, 0.0f, 171.0f);
    public static final ImmutableColor blue = new ImmutableColor(0.0f, 0.0f, 1.0f, 1.0f);
    public static final ImmutableColor green = new ImmutableColor(0.0f, 1.0f, 0.0f, 1.0f);
    public static final ImmutableColor black = new ImmutableColor(0.0f, 0.0f, 0.0f, 1.0f);
    public static final ImmutableColor gray = new ImmutableColor(0.5f, 0.5f, 0.5f, 1.0f);
    public static final ImmutableColor cyan = new ImmutableColor(0.0f, 1.0f, 1.0f, 1.0f);
    public static final ImmutableColor darkGray = new ImmutableColor(0.3f, 0.3f, 0.3f, 1.0f);
    public static final ImmutableColor lightGray = new ImmutableColor(0.7f, 0.7f, 0.7f, 1.0f);
    public static final ImmutableColor pink = new ImmutableColor(255, 175, 175, 255);
    public static final ImmutableColor orange = new ImmutableColor(255, 200, 0, 255);
    public static final ImmutableColor magenta = new ImmutableColor(255, 0, 255, 255);
    public static final ImmutableColor darkGreen = new ImmutableColor(22, 113, 20, 255);
    public static final ImmutableColor lightGreen = new ImmutableColor(55, 148, 53, 255);
    public final float a;
    public final float b;
    public final float g;
    public final float r;

    public ImmutableColor(ImmutableColor color) {
        if (color == null) {
            this.r = 0.0f;
            this.g = 0.0f;
            this.b = 0.0f;
            this.a = 1.0f;
            return;
        }
        this.r = color.r;
        this.g = color.g;
        this.b = color.b;
        this.a = color.a;
    }

    public ImmutableColor(Color color) {
        if (color == null) {
            this.r = 0.0f;
            this.g = 0.0f;
            this.b = 0.0f;
            this.a = 1.0f;
            return;
        }
        this.r = color.r;
        this.g = color.g;
        this.b = color.b;
        this.a = color.a;
    }

    public Color toMutableColor() {
        return new Color(this.r, this.g, this.b, this.a);
    }

    public ImmutableColor(float r, float g, float b) {
        this.r = PZMath.clamp(r, 0.0f, 1.0f);
        this.g = PZMath.clamp(g, 0.0f, 1.0f);
        this.b = PZMath.clamp(b, 0.0f, 1.0f);
        this.a = 1.0f;
    }

    public ImmutableColor(float r, float g, float b, float a) {
        this.r = Math.min(r, 1.0f);
        this.g = Math.min(g, 1.0f);
        this.b = Math.min(b, 1.0f);
        this.a = Math.min(a, 1.0f);
    }

    public ImmutableColor(Color colorA, Color colorB, float delta) {
        float r = (colorB.r - colorA.r) * delta;
        float g = (colorB.g - colorA.g) * delta;
        float b = (colorB.b - colorA.b) * delta;
        float a = (colorB.a - colorA.a) * delta;
        this.r = colorA.r + r;
        this.g = colorA.g + g;
        this.b = colorA.b + b;
        this.a = colorA.a + a;
    }

    public ImmutableColor(int r, int g, int b) {
        this.r = (float)r / 255.0f;
        this.g = (float)g / 255.0f;
        this.b = (float)b / 255.0f;
        this.a = 1.0f;
    }

    public ImmutableColor(int r, int g, int b, int a) {
        this.r = (float)r / 255.0f;
        this.g = (float)g / 255.0f;
        this.b = (float)b / 255.0f;
        this.a = (float)a / 255.0f;
    }

    public ImmutableColor(int value) {
        int b = (value & 0xFF0000) >> 16;
        int g = (value & 0xFF00) >> 8;
        int r = value & 0xFF;
        int a = (value & 0xFF000000) >> 24;
        if (a < 0) {
            a += 256;
        }
        if (a == 0) {
            a = 255;
        }
        this.r = (float)r / 255.0f;
        this.g = (float)g / 255.0f;
        this.b = (float)b / 255.0f;
        this.a = (float)a / 255.0f;
    }

    public static ImmutableColor random() {
        float colorHue = Rand.Next(0.0f, 1.0f);
        float colorSaturation = Rand.Next(0.0f, 0.6f);
        float colorBrightness = Rand.Next(0.0f, 0.9f);
        Color newC = Color.HSBtoRGB(colorHue, colorSaturation, colorBrightness);
        return new ImmutableColor(newC);
    }

    public static ImmutableColor decode(String nm) {
        return new ImmutableColor(Integer.decode(nm));
    }

    public ImmutableColor add(ImmutableColor c) {
        return new ImmutableColor(this.r + c.r, this.g + c.g, this.b + c.b, this.a + c.a);
    }

    public ImmutableColor brighter() {
        return this.brighter(0.2f);
    }

    public ImmutableColor brighter(float scale) {
        return new ImmutableColor(this.r + scale, this.g + scale, this.b + scale);
    }

    public ImmutableColor darker() {
        return this.darker(0.5f);
    }

    public ImmutableColor darker(float scale) {
        return new ImmutableColor(this.r - scale, this.g - scale, this.b - scale);
    }

    public boolean equals(Object other) {
        if (other instanceof ImmutableColor) {
            ImmutableColor o = (ImmutableColor)other;
            return o.r == this.r && o.g == this.g && o.b == this.b && o.a == this.a;
        }
        return false;
    }

    public int getAlphaInt() {
        return (int)(this.a * 255.0f);
    }

    public float getAlphaFloat() {
        return this.a;
    }

    public float getRedFloat() {
        return this.r;
    }

    public float getGreenFloat() {
        return this.g;
    }

    public float getBlueFloat() {
        return this.b;
    }

    public byte getAlphaByte() {
        return (byte)((int)(this.a * 255.0f) & 0xFF);
    }

    public int getBlueInt() {
        return (int)(this.b * 255.0f);
    }

    public byte getBlueByte() {
        return (byte)((int)(this.b * 255.0f) & 0xFF);
    }

    public int getGreenInt() {
        return (int)(this.g * 255.0f);
    }

    public byte getGreenByte() {
        return (byte)((int)(this.g * 255.0f) & 0xFF);
    }

    public int getRedInt() {
        return (int)(this.r * 255.0f);
    }

    public byte getRedByte() {
        return (byte)((int)(this.r * 255.0f) & 0xFF);
    }

    public int hashCode() {
        return (int)(this.r + this.g + this.b + this.a) * 255;
    }

    public ImmutableColor multiply(Color c) {
        return new ImmutableColor(this.r * c.r, this.g * c.g, this.b * c.b, this.a * c.a);
    }

    public ImmutableColor scale(float value) {
        return new ImmutableColor(this.r * value, this.g * value, this.b * value, this.a * value);
    }

    public String toString() {
        return "ImmutableColor (" + this.r + "," + this.g + "," + this.b + "," + this.a + ")";
    }

    public ImmutableColor interp(ImmutableColor to, float delta) {
        float r = to.r - this.r;
        float g = to.g - this.g;
        float b = to.b - this.b;
        float a = to.a - this.a;
        return new ImmutableColor(this.r + (r *= delta), this.g + (g *= delta), this.b + (b *= delta), this.a + (a *= delta));
    }

    public static Integer[] HSBtoRGB(float hue, float saturation, float brightness) {
        int r = 0;
        int g = 0;
        int b = 0;
        if (saturation == 0.0f) {
            g = b = (int)(brightness * 255.0f + 0.5f);
            r = b;
        } else {
            float h = (hue - (float)Math.floor(hue)) * 6.0f;
            float f = h - (float)Math.floor(h);
            float p = brightness * (1.0f - saturation);
            float q = brightness * (1.0f - saturation * f);
            float t = brightness * (1.0f - saturation * (1.0f - f));
            switch ((int)h) {
                case 0: {
                    r = (int)(brightness * 255.0f + 0.5f);
                    g = (int)(t * 255.0f + 0.5f);
                    b = (int)(p * 255.0f + 0.5f);
                    break;
                }
                case 1: {
                    r = (int)(q * 255.0f + 0.5f);
                    g = (int)(brightness * 255.0f + 0.5f);
                    b = (int)(p * 255.0f + 0.5f);
                    break;
                }
                case 2: {
                    r = (int)(p * 255.0f + 0.5f);
                    g = (int)(brightness * 255.0f + 0.5f);
                    b = (int)(t * 255.0f + 0.5f);
                    break;
                }
                case 3: {
                    r = (int)(p * 255.0f + 0.5f);
                    g = (int)(q * 255.0f + 0.5f);
                    b = (int)(brightness * 255.0f + 0.5f);
                    break;
                }
                case 4: {
                    r = (int)(t * 255.0f + 0.5f);
                    g = (int)(p * 255.0f + 0.5f);
                    b = (int)(brightness * 255.0f + 0.5f);
                    break;
                }
                case 5: {
                    r = (int)(brightness * 255.0f + 0.5f);
                    g = (int)(p * 255.0f + 0.5f);
                    b = (int)(q * 255.0f + 0.5f);
                }
            }
        }
        Integer[] result = new Integer[]{r, g, b};
        return result;
    }
}

