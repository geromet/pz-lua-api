/*
 * Decompiled with CFR 0.152.
 */
package zombie.ui;

import zombie.GameTime;
import zombie.SoundManager;
import zombie.UsedFromLua;
import zombie.core.Core;
import zombie.network.GameClient;
import zombie.network.GameServer;
import zombie.ui.HUDButton;
import zombie.ui.UIElement;

@UsedFromLua
public final class SpeedControls
extends UIElement {
    public static SpeedControls instance;
    public static final int PauseSpeed = 0;
    public static final int PlaySpeed = 1;
    public static final int FastForwardSpeed = 2;
    public static final int FasterForwardSpeed = 3;
    public static final int WaitSpeed = 4;
    private static final int ResumeSpeed = 5;
    private final float playMultiplier = 1.0f;
    private final float fastForwardMultiplier = 5.0f;
    private final float fasterForwardMultipler = 20.0f;
    private final float waitMultiplier = 40.0f;
    private int currentSpeed = 1;
    private int speedBeforePause = 1;
    private float multiBeforePause = 1.0f;
    private boolean doFrameStep;
    private boolean frameSkipped;
    private boolean mouseOver;
    private static HUDButton play;
    private static HUDButton pause;
    private static HUDButton fastForward;
    private static HUDButton fasterForward;
    private static HUDButton wait;
    private static HUDButton stepForward;

    public SpeedControls() {
        this.x = 0.0;
        this.y = 0.0;
        int gap = 2;
        stepForward = new SCButton("StepForward", 1.0f, 0.0f, "media/ui/speedControls/StepForward_Off.png", "media/ui/speedControls/StepForward_Off.png", (UIElement)this);
        pause = new SCButton("Pause", (float)(SpeedControls.stepForward.x + (double)SpeedControls.stepForward.width + 2.0), 0.0f, "media/ui/speedControls/Pause_Off.png", "media/ui/speedControls/Pause_On.png", (UIElement)this);
        play = new SCButton("Play", (float)(SpeedControls.pause.x + (double)SpeedControls.pause.width + 2.0), 0.0f, "media/ui/speedControls/Play_Off.png", "media/ui/speedControls/Play_On.png", (UIElement)this);
        fastForward = new SCButton("Fast Forward x 1", (float)(SpeedControls.play.x + (double)SpeedControls.play.width + 2.0), 0.0f, "media/ui/speedControls/FFwd1_Off.png", "media/ui/speedControls/FFwd1_On.png", (UIElement)this);
        fasterForward = new SCButton("Fast Forward x 2", (float)(SpeedControls.fastForward.x + (double)SpeedControls.fastForward.width + 2.0), 0.0f, "media/ui/speedControls/FFwd2_Off.png", "media/ui/speedControls/FFwd2_On.png", (UIElement)this);
        wait = new SCButton("Wait", (float)(SpeedControls.fasterForward.x + (double)SpeedControls.fasterForward.width + 2.0), 0.0f, "media/ui/speedControls/Wait_Off.png", "media/ui/speedControls/Wait_On.png", (UIElement)this);
        this.width = (float)((int)SpeedControls.wait.x) + SpeedControls.wait.width;
        this.height = SpeedControls.wait.height;
        if (Core.debug) {
            this.AddChild(stepForward);
        }
        this.AddChild(pause);
        this.AddChild(play);
        this.AddChild(fastForward);
        this.AddChild(fasterForward);
        this.AddChild(wait);
    }

    @Override
    public void ButtonClicked(String name) {
        GameTime.instance.setMultiplier(1.0f);
        if (name.equals(SpeedControls.stepForward.name)) {
            if (this.currentSpeed > 0) {
                this.SetCurrentGameSpeed(0);
            } else {
                this.SetCurrentGameSpeed(1);
                this.doFrameStep = true;
            }
        }
        if (name.equals(SpeedControls.pause.name)) {
            if (this.currentSpeed > 0) {
                this.SetCurrentGameSpeed(0);
            } else {
                this.SetCurrentGameSpeed(5);
            }
        }
        if (name.equals(SpeedControls.play.name)) {
            this.SetCurrentGameSpeed(1);
            GameTime.instance.setMultiplier(1.0f);
        }
        if (name.equals(SpeedControls.fastForward.name)) {
            this.SetCurrentGameSpeed(2);
            GameTime.instance.setMultiplier(5.0f);
        }
        if (name.equals(SpeedControls.fasterForward.name)) {
            this.SetCurrentGameSpeed(3);
            GameTime.instance.setMultiplier(20.0f);
        }
        if (name.equals(SpeedControls.wait.name)) {
            this.SetCurrentGameSpeed(4);
            GameTime.instance.setMultiplier(40.0f);
        }
    }

    public int getCurrentGameSpeed() {
        if (GameClient.client || GameServer.server) {
            return 1;
        }
        return this.currentSpeed;
    }

    public void SetCorrectIconStates() {
        if (this.currentSpeed == 0) {
            super.ButtonClicked(SpeedControls.pause.name);
        }
        if (this.currentSpeed == 1) {
            super.ButtonClicked(SpeedControls.play.name);
        }
        if (GameTime.instance.getTrueMultiplier() == 5.0f) {
            super.ButtonClicked(SpeedControls.fastForward.name);
        }
        if (GameTime.instance.getTrueMultiplier() == 20.0f) {
            super.ButtonClicked(SpeedControls.fasterForward.name);
        }
        if (GameTime.instance.getTrueMultiplier() == 40.0f) {
            super.ButtonClicked(SpeedControls.wait.name);
        }
    }

    public void Pause() {
        this.SetCurrentGameSpeed(0);
    }

    public void SetCurrentGameSpeed(int newSpeed) {
        if (this.currentSpeed > 0 && newSpeed == 0) {
            SoundManager.instance.pauseSoundAndMusic(true);
            SoundManager.instance.setMusicState("PauseMenu");
        } else if (this.currentSpeed == 0 && newSpeed > 0) {
            SoundManager.instance.setMusicState("InGame");
            SoundManager.instance.resumeSoundAndMusic();
        }
        GameTime.instance.setMultiplier(1.0f);
        if (newSpeed == 0) {
            this.speedBeforePause = this.currentSpeed;
            this.multiBeforePause = GameTime.instance.getMultiplier();
        }
        if (newSpeed == 5) {
            newSpeed = this.speedBeforePause;
            GameTime.instance.setMultiplier(this.multiBeforePause);
        }
        this.currentSpeed = newSpeed;
        this.SetCorrectIconStates();
    }

    @Override
    public Boolean onMouseMove(double dx, double dy) {
        if (!this.isVisible().booleanValue()) {
            return false;
        }
        this.mouseOver = true;
        super.onMouseMove(dx, dy);
        this.SetCorrectIconStates();
        return Boolean.TRUE;
    }

    @Override
    public void onMouseMoveOutside(double dx, double dy) {
        super.onMouseMoveOutside(dx, dy);
        this.mouseOver = false;
        this.SetCorrectIconStates();
    }

    @Override
    public void render() {
        super.render();
        if ("Tutorial".equals(Core.gameMode)) {
            stepForward.setVisible(false);
            pause.setVisible(false);
            play.setVisible(false);
            fastForward.setVisible(false);
            fasterForward.setVisible(false);
            wait.setVisible(false);
        }
        this.SetCorrectIconStates();
    }

    @Override
    public void update() {
        super.update();
        this.SetCorrectIconStates();
        if (this.frameSkipped) {
            this.SetCurrentGameSpeed(0);
            this.frameSkipped = false;
        }
        if (this.doFrameStep) {
            this.frameSkipped = true;
            this.doFrameStep = false;
        }
    }

    public void stepForward() {
        if (GameClient.client) {
            GameClient.SendCommandToServer("/setTimeSpeed 0");
        } else if (this.currentSpeed > 0) {
            this.SetCurrentGameSpeed(0);
        } else {
            this.SetCurrentGameSpeed(1);
            this.doFrameStep = true;
        }
    }

    public boolean isPaused() {
        return this.currentSpeed == 0;
    }

    public static final class SCButton
    extends HUDButton {
        private static final int BORDER = 3;

        public SCButton(String name, float x, float y, String texture, String highlight, UIElement display) {
            super(name, (double)x, (double)y, texture, highlight, display);
            this.width += 6.0f;
            this.height += 6.0f;
        }

        @Override
        public void render() {
            int dy = 3;
            if (this.clicked) {
                ++dy;
            }
            this.DrawTextureScaledCol(null, 0.0, this.clicked ? 1.0 : 0.0, this.width, this.height, 0.0, 0.0, 0.0, 0.75);
            if (this.mouseOver || this.name.equals(this.display.getClickedValue())) {
                this.DrawTextureScaled(this.highlight, 3.0, dy, this.highlight.getWidth(), this.highlight.getHeight(), this.clickedalpha);
            } else {
                this.DrawTextureScaled(this.texture, 3.0, dy, this.texture.getWidth(), this.texture.getHeight(), this.notclickedAlpha);
            }
            if (this.overicon != null) {
                this.DrawTextureScaled(this.overicon, 3.0, dy, this.overicon.getWidth(), this.overicon.getHeight(), 1.0);
            }
        }
    }
}

