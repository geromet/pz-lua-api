/*
 * Decompiled with CFR 0.152.
 */
package zombie.ui;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import zombie.GameTime;
import zombie.GameWindow;
import zombie.UsedFromLua;
import zombie.characters.CharacterStat;
import zombie.characters.IsoPlayer;
import zombie.core.Core;
import zombie.gameStates.IngameState;
import zombie.ui.DialogButton;
import zombie.ui.NewWindow;
import zombie.ui.TextBox;
import zombie.ui.TutorialManager;
import zombie.ui.UIEventHandler;
import zombie.ui.UIFont;
import zombie.ui.UIManager;

@UsedFromLua
public final class ModalDialog
extends NewWindow {
    public boolean yes;
    public String name;
    UIEventHandler handler;
    public boolean clicked;

    public ModalDialog(String name, String help, boolean bYesNo) {
        super(Core.getInstance().getOffscreenWidth(0) / 2, Core.getInstance().getOffscreenHeight(0) / 2, 470, 10, false);
        this.name = name;
        this.resizeToFitY = false;
        this.ignoreLossControl = true;
        TextBox text = new TextBox(UIFont.Medium, 0, 0, 450, help);
        text.centered = true;
        text.resizeParent = true;
        text.update();
        this.Nest(text, 20, 10, 20, 10);
        this.update();
        this.height *= 1.3f;
        if (bYesNo) {
            this.AddChild(new DialogButton(this, (float)(this.getWidth().intValue() / 2 - 40), (float)(this.getHeight().intValue() - 18), "Yes", "Yes"));
            this.AddChild(new DialogButton(this, (float)(this.getWidth().intValue() / 2 + 40), (float)(this.getHeight().intValue() - 18), "No", "No"));
        } else {
            this.AddChild(new DialogButton(this, (float)(this.getWidth().intValue() / 2), (float)(this.getHeight().intValue() - 18), "Ok", "Ok"));
        }
        this.x -= (double)(this.width / 2.0f);
        this.y -= (double)(this.height / 2.0f);
    }

    @Override
    public void ButtonClicked(String name) {
        if (this.handler != null) {
            this.handler.ModalClick(this.name, name);
            this.setVisible(false);
            return;
        }
        if (name.equals("Ok")) {
            UIManager.getSpeedControls().SetCurrentGameSpeed(4);
            this.Clicked(name);
            this.clicked = true;
            this.yes = true;
            this.setVisible(false);
            IngameState.instance.paused = false;
        }
        if (name.equals("Yes")) {
            UIManager.getSpeedControls().SetCurrentGameSpeed(4);
            this.Clicked(name);
            this.clicked = true;
            this.yes = true;
            this.setVisible(false);
            IngameState.instance.paused = false;
        }
        if (name.equals("No")) {
            UIManager.getSpeedControls().SetCurrentGameSpeed(4);
            this.Clicked(name);
            this.clicked = true;
            this.yes = false;
            this.setVisible(false);
            IngameState.instance.paused = false;
        }
    }

    public void Clicked(String name) {
        if (this.name.equals("Sleep") && name.equals("Yes")) {
            float sleepHours = 12.0f * IsoPlayer.getInstance().getStats().get(CharacterStat.FATIGUE);
            if (sleepHours < 7.0f) {
                sleepHours = 7.0f;
            }
            if ((sleepHours += GameTime.getInstance().getTimeOfDay()) >= 24.0f) {
                sleepHours -= 24.0f;
            }
            IsoPlayer.getInstance().setForceWakeUpTime((int)sleepHours);
            IsoPlayer.getInstance().setAsleepTime(0.0f);
            TutorialManager.instance.stealControl = true;
            IsoPlayer.getInstance().setAsleep(true);
            UIManager.setbFadeBeforeUI(true);
            UIManager.FadeOut(4.0);
            UIManager.getSpeedControls().SetCurrentGameSpeed(3);
            try {
                GameWindow.save(true);
            }
            catch (IOException ex) {
                Logger.getLogger(ModalDialog.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        UIManager.modal.setVisible(false);
    }
}

