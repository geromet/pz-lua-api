/*
 * Decompiled with CFR 0.152.
 */
package zombie.ui;

import java.util.HashMap;
import java.util.Map;
import zombie.GameTime;
import zombie.UsedFromLua;
import zombie.characters.CharacterStat;
import zombie.characters.IsoGameCharacter;
import zombie.characters.Moodles.Moodle;
import zombie.core.Color;
import zombie.core.Core;
import zombie.core.PerformanceSettings;
import zombie.core.SpriteRenderer;
import zombie.core.math.PZMath;
import zombie.core.textures.Texture;
import zombie.input.GameKeyboard;
import zombie.input.Mouse;
import zombie.scripting.objects.MoodleType;
import zombie.scripting.objects.Registries;
import zombie.ui.MoodleTextureSet;
import zombie.ui.TextManager;
import zombie.ui.UIElement;
import zombie.ui.UIManager;

@UsedFromLua
public final class MoodlesUI
extends UIElement {
    private final Map<MoodleType, MoodleUIData> moodleUiState = new HashMap<MoodleType, MoodleUIData>();
    private static final int MaxMouseOverSlot = 1000;
    private static final float DefaultMoodleDistY = 10.0f;
    private static final int DistFromRightEdge = 10;
    private static final float OFFSCREEN_Y = 10000.0f;
    private static final float OscillatorDecelerator = 0.96f;
    private static final float OscillatorRate = 0.8f;
    private static final float OscillatorScalar = 15.6f;
    private static final float OscillatorStartLevel = 1.0f;
    public float clientH;
    public float clientW;
    private static MoodlesUI instance;
    private float alpha = 1.0f;
    private final int[] textureSizes = new int[]{32, 48, 64, 80, 96, 128};
    private final MoodleTextureSet[] textureSets = new MoodleTextureSet[this.textureSizes.length];
    private MoodleTextureSet currentTextureSet;
    private float moodleDistY = 74.0f;
    private boolean mouseOver;
    private int mouseOverSlot;
    private int numUsedSlots;
    private int debugKeyDelay;
    private float oscillatorStep;
    private IsoGameCharacter isoGameCharacter;
    private boolean alphaIncrease = true;
    private final Color backgroundColour = new Color(Color.gray);

    public MoodlesUI() {
        this.x = Core.getInstance().getScreenWidth() - 10;
        this.y = 120.0;
        this.width = this.getTextureSizeForOption();
        this.height = Core.getInstance().getScreenHeight();
        for (int i = 0; i < this.textureSizes.length; ++i) {
            this.textureSets[i] = new MoodleTextureSet(this.textureSizes[i]);
        }
        int textureSetIndex = this.getTextureSetIndexForSize((int)this.width);
        this.currentTextureSet = this.textureSets[textureSetIndex];
        for (MoodleType type : Registries.MOODLE_TYPE.values()) {
            this.moodleUiState.put(type, new MoodleUIData());
        }
        this.clientW = this.width;
        this.clientH = this.height;
        instance = this;
    }

    private int getTextureSizeForOption() {
        int fontSizeIndex;
        int moodleSizeIndex = Core.getInstance().getOptionMoodleSize() - 1;
        if (moodleSizeIndex >= 0 && moodleSizeIndex < this.textureSizes.length) {
            return this.textureSizes[moodleSizeIndex];
        }
        if (moodleSizeIndex == 6 && (fontSizeIndex = Core.getInstance().getOptionFontSizeReal() - 1) >= 0 && fontSizeIndex < this.textureSizes.length) {
            return this.textureSizes[fontSizeIndex];
        }
        return 32;
    }

    private int getTextureSetIndexForSize(int size) {
        return switch (size) {
            case 32 -> 0;
            case 48 -> 1;
            case 64 -> 2;
            case 80 -> 3;
            case 96 -> 4;
            case 128 -> 5;
            default -> 0;
        };
    }

    private boolean isCurrentlyAnimating() {
        for (MoodleUIData moodleUIData : this.moodleUiState.values()) {
            if (moodleUIData.slotsPos == moodleUIData.slotsDesiredPos) continue;
            return true;
        }
        return false;
    }

    private boolean isPercentageBackground(MoodleType moodleType) {
        return false;
    }

    private float getBackgroundPercentage(MoodleType moodleType) {
        if (moodleType == MoodleType.ENDURANCE) {
            return this.isoGameCharacter.getStats().get(CharacterStat.ENDURANCE);
        }
        return 1.0f;
    }

    private void drawPercentageBackground(MoodleType moodleType, float wiggleOffset) {
        if (!this.isVisible().booleanValue()) {
            return;
        }
        MoodleTextureSet moodleTextureSet = this.currentTextureSet;
        Texture tex = moodleTextureSet.getBackground();
        if (tex == null) {
            return;
        }
        this.DrawTextureCol(moodleTextureSet.getBackground(), (int)wiggleOffset, this.moodleUiState.get((Object)moodleType).slotsPos, Color.gray);
        float percent = this.getBackgroundPercentage(moodleType);
        double padY = (double)(8 * this.getTextureSizeForOption()) / 128.0;
        double dx = (double)wiggleOffset + this.getAbsoluteX();
        double dy = (double)this.moodleUiState.get((Object)moodleType).slotsPos + this.getAbsoluteY();
        dx += (double)tex.offsetX;
        double wid = tex.getWidth();
        double hei = tex.getHeight();
        double y = (dy += (double)tex.offsetY) + this.yScroll;
        double clampY = Math.ceil(y + padY + (hei - padY * 2.0) * (double)(1.0f - percent));
        double clampH = hei - (clampY - y);
        SpriteRenderer.instance.renderClamped(tex, (int)(dx + this.xScroll), (int)(dy + this.yScroll), (int)wid, (int)hei, (int)(dx + this.xScroll), (int)clampY, (int)wid, (int)clampH, this.backgroundColour.r, this.backgroundColour.g, this.backgroundColour.b, this.backgroundColour.a, null);
        SpriteRenderer.instance.renderClamped(tex, (int)(dx + this.xScroll), (int)(dy + this.yScroll), (int)wid, (int)hei, (int)(dx + this.xScroll), (int)clampY, (int)wid, 2, this.backgroundColour.r * 0.5f, this.backgroundColour.g * 0.5f, this.backgroundColour.b * 0.5f, this.backgroundColour.a, null);
    }

    private void drawBackgroundPulse(MoodleType moodleType, float wiggleOffset) {
        boolean bIncreasing;
        if (!this.isVisible().booleanValue()) {
            return;
        }
        MoodleTextureSet moodleTextureSet = this.currentTextureSet;
        Texture tex = moodleTextureSet.getBackground();
        if (tex == null) {
            return;
        }
        MoodleUIData moodleUIData = this.moodleUiState.get(moodleType);
        float prevValue = moodleUIData.slotsPulse1;
        float percent = this.getBackgroundPercentage(moodleType);
        boolean bl = bIncreasing = percent >= prevValue;
        if (!GameTime.isGamePaused()) {
            float dt = (float)(UIManager.getMillisSinceLastRender() / 2500.0) * 100.0f;
            moodleUIData.slotsPulse2 = moodleUIData.slotsPulse2 + (bIncreasing ? dt : -dt);
        }
        if (bIncreasing && moodleUIData.slotsPulse2 > 100.0f) {
            moodleUIData.slotsPulse2 = 0.0f;
        } else if (!bIncreasing && moodleUIData.slotsPulse2 < 0.0f) {
            moodleUIData.slotsPulse2 = 100.0f;
        }
        percent = moodleUIData.slotsPulse2 / 100.0f;
        float r = PZMath.lerp(Color.gray.r, this.backgroundColour.r, percent);
        float g = PZMath.lerp(Color.gray.r, this.backgroundColour.g, percent);
        float b = PZMath.lerp(Color.gray.r, this.backgroundColour.b, percent);
        this.DrawTextureColor(moodleTextureSet.getBackground(), (int)wiggleOffset, (int)moodleUIData.slotsPos, r, g, b, 1.0);
        double padY = (double)(8 * this.getTextureSizeForOption()) / 128.0;
        double dx = (double)wiggleOffset + this.getAbsoluteX();
        double dy = (double)moodleUIData.slotsPos + this.getAbsoluteY();
        dx += (double)tex.offsetX;
        double wid = tex.getWidth();
        double hei = tex.getHeight();
        double y = (dy += (double)tex.offsetY) + this.yScroll;
        double clampY = Math.ceil(y + padY + (hei - padY * 2.0) * (double)(1.0f - percent));
        double clampH = hei - (clampY - y);
        r = PZMath.lerp(this.backgroundColour.r, 1.0f, percent);
        g = PZMath.lerp(this.backgroundColour.g, 1.0f, percent);
        b = PZMath.lerp(this.backgroundColour.b, 1.0f, percent);
        float a = 0.33f;
        SpriteRenderer.instance.renderClamped(tex, (int)(dx + this.xScroll), (int)(dy + this.yScroll), (int)wid, (int)hei, (int)(dx + this.xScroll), (int)clampY, (int)wid, (int)clampH, r, g, b, 0.33f, null);
        SpriteRenderer.instance.renderClamped(tex, (int)(dx + this.xScroll), (int)(dy + this.yScroll), (int)wid, (int)hei, (int)(dx + this.xScroll), (int)clampY, (int)wid, 2, r * 0.5f, g * 0.5f, b * 0.5f, 0.33f, null);
    }

    @Override
    public Boolean onMouseMove(double dx, double dy) {
        if (!this.isVisible().booleanValue()) {
            return false;
        }
        this.mouseOver = true;
        super.onMouseMove(dx, dy);
        this.mouseOverSlot = (int)(((double)Mouse.getYA() - this.getY()) / (double)this.moodleDistY);
        if (this.mouseOverSlot >= this.numUsedSlots) {
            this.mouseOverSlot = 1000;
        }
        return true;
    }

    @Override
    public void onMouseMoveOutside(double dx, double dy) {
        super.onMouseMoveOutside(dx, dy);
        this.mouseOverSlot = 1000;
        this.mouseOver = false;
    }

    @Override
    public void render() {
        int widthRequired = this.getTextureSizeForOption();
        if (widthRequired != this.currentTextureSet.getSize()) {
            int textureSetIndex = this.getTextureSetIndexForSize(widthRequired);
            this.currentTextureSet = this.textureSets[textureSetIndex];
            this.width = widthRequired;
        }
        if (this.isoGameCharacter == null) {
            return;
        }
        if (this.moodleDistY != 10.0f + this.width) {
            this.isoGameCharacter.getMoodles().setMoodlesStateChanged(true);
            this.update();
        }
        float fpsFraction = (float)(UIManager.getMillisSinceLastRender() / (double)33.3f);
        this.oscillatorStep += 0.8f * fpsFraction * 0.5f;
        float oscillator = (float)Math.sin(this.oscillatorStep);
        int renderedSlot = 0;
        MoodleTextureSet moodleTextureSet = this.currentTextureSet;
        for (Map.Entry<MoodleType, MoodleUIData> entry : this.moodleUiState.entrySet()) {
            MoodleType moodleType = entry.getKey();
            MoodleUIData moodleUIData = entry.getValue();
            if (moodleUIData.slotsPos == 10000.0f) continue;
            float wiggleOffset = oscillator * 15.6f * moodleUIData.oscillationLevel;
            this.backgroundColour.set(Color.gray);
            switch (moodleUIData.goodBadNeutral) {
                case 0: {
                    break;
                }
                case 1: {
                    Color.abgrToColor(Color.lerpABGR(Color.colorToABGR(new Color(Color.gray)), Color.colorToABGR(Core.getInstance().getGoodHighlitedColor().toColor()), (float)moodleUIData.level / 4.0f), this.backgroundColour);
                    break;
                }
                case 2: {
                    Color.abgrToColor(Color.lerpABGR(Color.colorToABGR(new Color(Color.gray)), Color.colorToABGR(Core.getInstance().getBadHighlitedColor().toColor()), (float)moodleUIData.level / 4.0f), this.backgroundColour);
                }
            }
            Texture moodleTex = moodleTextureSet.getTexture(moodleType);
            if (moodleType.toString().equals(Core.getInstance().getBlinkingMoodle())) {
                if (this.alphaIncrease) {
                    this.alpha += 0.1f * (30.0f / (float)PerformanceSettings.instance.getUIRenderFPS());
                    if (this.alpha > 1.0f) {
                        this.alpha = 1.0f;
                        this.alphaIncrease = false;
                    }
                } else {
                    this.alpha -= 0.1f * (30.0f / (float)PerformanceSettings.instance.getUIRenderFPS());
                    if (this.alpha < 0.0f) {
                        this.alpha = 0.0f;
                        this.alphaIncrease = true;
                    }
                }
            }
            if (Core.getInstance().getBlinkingMoodle() == null) {
                this.alpha = 1.0f;
            }
            int minFilter = 9985;
            int magFilter = 9729;
            Texture background = moodleTextureSet.getBackground();
            Texture border = moodleTextureSet.getBorder();
            background.getTextureId().setMinFilter(9985);
            background.getTextureId().setMagFilter(9729);
            border.getTextureId().setMinFilter(9985);
            border.getTextureId().setMagFilter(9729);
            if (this.isPercentageBackground(moodleType)) {
                this.drawBackgroundPulse(moodleType, wiggleOffset);
            } else {
                moodleUIData.slotsPulse2 = 0.0f;
                this.DrawTextureCol(background, (int)wiggleOffset, (int)moodleUIData.slotsPos, this.backgroundColour);
            }
            this.DrawTexture(border, (int)wiggleOffset, (int)moodleUIData.slotsPos, this.alpha);
            float scale = this.width;
            double offset = Math.ceil((this.width - scale) / 2.0f);
            moodleTex.getTextureId().setMinFilter(9985);
            moodleTex.getTextureId().setMagFilter(9729);
            this.DrawTexture(moodleTex, (int)((double)wiggleOffset + offset), (int)((double)moodleUIData.slotsPos + offset), this.alpha);
            if (this.mouseOver && renderedSlot == this.mouseOverSlot) {
                String s1 = this.isoGameCharacter.getMoodles().getMoodleDisplayString(moodleType);
                String s2 = this.isoGameCharacter.getMoodles().getMoodleDescriptionString(moodleType);
                int width1 = TextManager.instance.font.getWidth(s1);
                int width2 = TextManager.instance.font.getWidth(s2);
                int width = Math.max(width1, width2);
                int fontHgt = TextManager.instance.font.getLineHeight();
                int y = (int)moodleUIData.slotsPos + 1;
                int h = (2 + fontHgt) * 2;
                if (this.width > (float)h) {
                    y += (int)((this.width - (float)h) / 2.0f);
                }
                this.DrawTextureScaledColor(null, -10.0 - (double)width - 6.0, (double)y - 2.0, (double)width + 12.0, Double.valueOf(h), 0.0, 0.0, 0.0, 0.6);
                this.DrawTextRight(s1, -10.0, y, 1.0, 1.0, 1.0, 1.0);
                this.DrawTextRight(s2, -10.0, y + fontHgt, 0.8f, 0.8f, 0.8f, 1.0);
            }
            ++renderedSlot;
        }
        super.render();
    }

    public void wiggle(MoodleType moodleType) {
        this.moodleUiState.get((Object)moodleType).oscillationLevel = 1.0f;
    }

    @Override
    public void update() {
        this.moodleDistY = 10.0f + this.width;
        super.update();
        if (this.isoGameCharacter == null) {
            return;
        }
        if (!this.isCurrentlyAnimating()) {
            if (this.debugKeyDelay > 0) {
                --this.debugKeyDelay;
            } else if (GameKeyboard.isKeyDown(57)) {
                this.debugKeyDelay = 10;
            }
        }
        float fpsFraction = (float)PerformanceSettings.getLockFPS() / 30.0f;
        for (MoodleUIData moodleUIData : this.moodleUiState.values()) {
            moodleUIData.oscillationLevel -= moodleUIData.oscillationLevel * 0.04000002f / fpsFraction;
            if (!(moodleUIData.oscillationLevel < 0.01f)) continue;
            moodleUIData.oscillationLevel = 0.0f;
        }
        if (this.isoGameCharacter.getMoodles().UI_RefreshNeeded()) {
            int currentSlotPlace = 0;
            for (Map.Entry<MoodleType, MoodleUIData> entry : this.moodleUiState.entrySet()) {
                MoodleType moodleType = entry.getKey();
                MoodleUIData moodleUIData = entry.getValue();
                if (moodleType == MoodleType.FOOD_EATEN && this.isoGameCharacter.getMoodles().getMoodleLevel(moodleType) < Moodle.MoodleLevel.HighMoodleLevel.ordinal()) {
                    moodleUIData.slotsPos = 10000.0f;
                    moodleUIData.slotsDesiredPos = 10000.0f;
                    moodleUIData.oscillationLevel = 0.0f;
                    continue;
                }
                if (this.isoGameCharacter.getMoodles().getMoodleLevel(moodleType) > 0) {
                    boolean hasChanged = false;
                    if (moodleUIData.level != this.isoGameCharacter.getMoodles().getMoodleLevel(moodleType)) {
                        hasChanged = true;
                        moodleUIData.level = this.isoGameCharacter.getMoodles().getMoodleLevel(moodleType);
                        moodleUIData.oscillationLevel = 1.0f;
                    }
                    moodleUIData.slotsDesiredPos = this.moodleDistY * (float)currentSlotPlace;
                    if (hasChanged) {
                        if (moodleUIData.slotsPos == 10000.0f) {
                            moodleUIData.slotsPos = moodleUIData.slotsDesiredPos + 500.0f;
                            moodleUIData.oscillationLevel = 0.0f;
                        }
                        moodleUIData.goodBadNeutral = this.isoGameCharacter.getMoodles().getGoodBadNeutral(moodleType);
                    } else {
                        moodleUIData.oscillationLevel = 0.0f;
                    }
                    ++currentSlotPlace;
                    continue;
                }
                moodleUIData.slotsPos = 10000.0f;
                moodleUIData.slotsDesiredPos = 10000.0f;
                moodleUIData.oscillationLevel = 0.0f;
                moodleUIData.level = 0;
            }
            this.numUsedSlots = currentSlotPlace;
        }
        for (MoodleUIData moodleUIData : this.moodleUiState.values()) {
            if (Math.abs(moodleUIData.slotsPos - moodleUIData.slotsDesiredPos) > 0.8f) {
                moodleUIData.slotsPos += (moodleUIData.slotsDesiredPos - moodleUIData.slotsPos) * 0.15f;
                continue;
            }
            moodleUIData.slotsPos = moodleUIData.slotsDesiredPos;
        }
    }

    public void setCharacter(IsoGameCharacter chr) {
        if (chr == this.isoGameCharacter) {
            return;
        }
        this.isoGameCharacter = chr;
        if (this.isoGameCharacter != null && this.isoGameCharacter.getMoodles() != null) {
            this.isoGameCharacter.getMoodles().setMoodlesStateChanged(true);
        }
    }

    public static MoodlesUI getInstance() {
        return instance;
    }

    public static class MoodleUIData {
        public int goodBadNeutral;
        public int level;
        public float oscillationLevel;
        public float slotsDesiredPos = 10000.0f;
        public float slotsPos = 10000.0f;
        public float slotsPulse1;
        public float slotsPulse2;
    }
}

