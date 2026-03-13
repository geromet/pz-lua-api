/*
 * Decompiled with CFR 0.152.
 */
package zombie.iso;

import zombie.GameTime;
import zombie.UsedFromLua;
import zombie.characters.IsoPlayer;
import zombie.core.math.PZMath;

@UsedFromLua
public class SearchMode {
    private static SearchMode instance;
    private float fadeTime = 1.0f;
    private final PlayerSearchMode[] plrModes = new PlayerSearchMode[4];

    public static SearchMode getInstance() {
        if (instance == null) {
            instance = new SearchMode();
        }
        return instance;
    }

    private SearchMode() {
        for (int i = 0; i < this.plrModes.length; ++i) {
            this.plrModes[i] = new PlayerSearchMode(i, this);
            this.plrModes[i].blur.setTargets(1.0f, 1.0f);
            this.plrModes[i].desat.setTargets(0.85f, 0.85f);
            this.plrModes[i].radius.setTargets(4.0f, 4.0f);
            this.plrModes[i].darkness.setTargets(0.0f, 0.0f);
            this.plrModes[i].gradientWidth.setTargets(4.0f, 4.0f);
        }
    }

    public PlayerSearchMode getSearchModeForPlayer(int index) {
        return this.plrModes[index];
    }

    public float getFadeTime() {
        return this.fadeTime;
    }

    public void setFadeTime(float fadeTime) {
        this.fadeTime = fadeTime;
    }

    public boolean isOverride(int plrIdx) {
        return this.plrModes[plrIdx].override;
    }

    public void setOverride(int plrIdx, boolean enabled) {
        this.plrModes[plrIdx].override = enabled;
    }

    public boolean isOverrideSearchManager(int plrIdx) {
        return this.plrModes[plrIdx].overrideSearchManager;
    }

    public void setOverrideSearchManager(int plrIdx, boolean enabled) {
        this.plrModes[plrIdx].overrideSearchManager = enabled;
    }

    public SearchModeFloat getRadius(int plrIdx) {
        return this.plrModes[plrIdx].radius;
    }

    public SearchModeFloat getGradientWidth(int plrIdx) {
        return this.plrModes[plrIdx].gradientWidth;
    }

    public SearchModeFloat getBlur(int plrIdx) {
        return this.plrModes[plrIdx].blur;
    }

    public SearchModeFloat getDesat(int plrIdx) {
        return this.plrModes[plrIdx].desat;
    }

    public SearchModeFloat getDarkness(int plrIdx) {
        return this.plrModes[plrIdx].darkness;
    }

    public boolean isEnabled(int plrIdx) {
        return this.plrModes[plrIdx].enabled;
    }

    public void setEnabled(int plrIdx, boolean b) {
        PlayerSearchMode mode = this.plrModes[plrIdx];
        if (b && !mode.enabled) {
            mode.enabled = true;
            this.FadeIn(plrIdx);
        } else if (!b && mode.enabled) {
            mode.enabled = false;
            this.FadeOut(plrIdx);
        }
    }

    private void FadeIn(int plrIdx) {
        PlayerSearchMode playerSearchMode = this.plrModes[plrIdx];
        playerSearchMode.timer = Math.max(playerSearchMode.timer, 0.0f);
        playerSearchMode.doFadeIn = true;
        playerSearchMode.doFadeOut = false;
    }

    private void FadeOut(int plrIdx) {
        PlayerSearchMode playerSearchMode = this.plrModes[plrIdx];
        playerSearchMode.timer = Math.min(playerSearchMode.timer, this.fadeTime);
        playerSearchMode.doFadeIn = false;
        playerSearchMode.doFadeOut = true;
    }

    public void update() {
        for (int i = 0; i < this.plrModes.length; ++i) {
            PlayerSearchMode plrSm = this.plrModes[i];
            plrSm.update();
        }
    }

    public static void reset() {
        instance = null;
    }

    @UsedFromLua
    public static class PlayerSearchMode {
        private final int plrIndex;
        private final SearchMode parent;
        private boolean override;
        private boolean overrideSearchManager;
        private boolean enabled;
        private final SearchModeFloat radius = new SearchModeFloat(0.0f, 50.0f, 1.0f);
        private final SearchModeFloat gradientWidth = new SearchModeFloat(0.0f, 20.0f, 1.0f);
        private final SearchModeFloat blur = new SearchModeFloat(0.0f, 1.0f, 0.01f);
        private final SearchModeFloat desat = new SearchModeFloat(0.0f, 1.0f, 0.01f);
        private final SearchModeFloat darkness = new SearchModeFloat(0.0f, 1.0f, 0.01f);
        private float timer;
        private boolean doFadeOut;
        private boolean doFadeIn;

        public PlayerSearchMode(int index, SearchMode sm) {
            this.plrIndex = index;
            this.parent = sm;
        }

        public boolean isShaderEnabled() {
            return this.enabled || this.doFadeIn || this.doFadeOut;
        }

        private boolean isPlayerExterior() {
            IsoPlayer player = IsoPlayer.players[this.plrIndex];
            return player != null && player.getCurrentSquare() != null && !player.getCurrentSquare().isInARoom();
        }

        public float getShaderBlur() {
            return this.isPlayerExterior() ? this.blur.getExterior() : this.blur.getInterior();
        }

        public float getShaderDesat() {
            return this.isPlayerExterior() ? this.desat.getExterior() : this.desat.getInterior();
        }

        public float getShaderRadius() {
            return this.isPlayerExterior() ? this.radius.getExterior() : this.radius.getInterior();
        }

        public float getShaderGradientWidth() {
            return this.isPlayerExterior() ? this.gradientWidth.getExterior() : this.gradientWidth.getInterior();
        }

        public float getShaderDarkness() {
            return this.isPlayerExterior() ? this.darkness.getExterior() : this.darkness.getInterior();
        }

        public SearchModeFloat getBlur() {
            return this.blur;
        }

        public SearchModeFloat getDesat() {
            return this.desat;
        }

        public SearchModeFloat getRadius() {
            return this.radius;
        }

        public SearchModeFloat getGradientWidth() {
            return this.gradientWidth;
        }

        public SearchModeFloat getDarkness() {
            return this.darkness;
        }

        private void update() {
            if (this.override) {
                return;
            }
            if (this.doFadeIn) {
                this.timer += GameTime.getInstance().getTimeDelta();
                this.timer = PZMath.clamp(this.timer, 0.0f, this.parent.fadeTime);
                float delta = PZMath.clamp(this.timer / this.parent.fadeTime, 0.0f, 1.0f);
                this.blur.update(delta);
                this.desat.update(delta);
                this.radius.update(delta);
                this.darkness.update(delta);
                this.gradientWidth.equalise();
                if (this.timer >= this.parent.fadeTime) {
                    this.doFadeIn = false;
                }
                return;
            }
            if (this.doFadeOut) {
                this.timer -= GameTime.getInstance().getTimeDelta();
                this.timer = PZMath.clamp(this.timer, 0.0f, this.parent.fadeTime);
                float delta = PZMath.clamp(this.timer / this.parent.fadeTime, 0.0f, 1.0f);
                this.blur.update(delta);
                this.desat.update(delta);
                this.radius.update(delta);
                this.darkness.update(delta);
                this.gradientWidth.equalise();
                if (this.timer <= 0.0f) {
                    this.doFadeOut = false;
                }
                return;
            }
            if (this.enabled) {
                this.blur.equalise();
                this.desat.equalise();
                this.radius.equalise();
                this.darkness.equalise();
                this.gradientWidth.equalise();
            } else {
                this.blur.reset();
                this.desat.reset();
                this.radius.reset();
                this.darkness.reset();
                this.gradientWidth.equalise();
            }
        }
    }

    @UsedFromLua
    public static class SearchModeFloat {
        private final float min;
        private final float max;
        private final float stepsize;
        private float exterior;
        private float targetExterior;
        private float interior;
        private float targetInterior;

        private SearchModeFloat(float min, float max, float stepsize) {
            this.min = min;
            this.max = max;
            this.stepsize = stepsize;
        }

        public void set(float exterior, float targetExterior, float interior, float targetInterior) {
            this.setExterior(exterior);
            this.setTargetExterior(targetExterior);
            this.setInterior(interior);
            this.setTargetInterior(targetInterior);
        }

        public void setAll(float value) {
            this.setExterior(value);
            this.setTargetExterior(value);
            this.setInterior(value);
            this.setTargetInterior(value);
        }

        public void setTargets(float targetExterior, float targetInterior) {
            this.setTargetExterior(targetExterior);
            this.setTargetInterior(targetInterior);
        }

        public float getExterior() {
            return this.exterior;
        }

        public void setExterior(float exterior) {
            this.exterior = exterior;
        }

        public float getTargetExterior() {
            return this.targetExterior;
        }

        public void setTargetExterior(float targetExterior) {
            this.targetExterior = targetExterior;
        }

        public float getInterior() {
            return this.interior;
        }

        public void setInterior(float interior) {
            this.interior = interior;
        }

        public float getTargetInterior() {
            return this.targetInterior;
        }

        public void setTargetInterior(float targetInterior) {
            this.targetInterior = targetInterior;
        }

        public void update(float delta) {
            this.exterior = delta * this.targetExterior;
            this.interior = delta * this.targetInterior;
        }

        public void equalise() {
            this.exterior = !PZMath.equal(this.exterior, this.targetExterior, 0.001f) ? PZMath.lerp(this.exterior, this.targetExterior, 0.01f) : this.targetExterior;
            this.interior = !PZMath.equal(this.interior, this.targetInterior, 0.001f) ? PZMath.lerp(this.interior, this.targetInterior, 0.01f) : this.targetInterior;
        }

        public void reset() {
            this.exterior = 0.0f;
            this.interior = 0.0f;
        }

        public void resetAll() {
            this.exterior = 0.0f;
            this.interior = 0.0f;
            this.targetInterior = 0.0f;
            this.targetExterior = 0.0f;
        }

        public float getMin() {
            return this.min;
        }

        public float getMax() {
            return this.max;
        }

        public float getStepsize() {
            return this.stepsize;
        }
    }
}

