/*
 * Decompiled with CFR 0.152.
 */
package zombie.gameStates;

import java.util.HashMap;
import zombie.GameWindow;
import zombie.SoundManager;
import zombie.UsedFromLua;
import zombie.core.Core;
import zombie.core.SpriteRenderer;
import zombie.debug.DebugLog;
import zombie.debug.LogSeverity;
import zombie.gameStates.GameLoadingState;
import zombie.gameStates.GameState;
import zombie.gameStates.GameStateMachine;
import zombie.gameStates.MainScreenState;
import zombie.input.GameKeyboard;
import zombie.modding.ActiveMods;
import zombie.network.ConnectionManager;
import zombie.network.GameClient;
import zombie.ui.LoadingQueueUI;
import zombie.ui.UIManager;

@UsedFromLua
public class LoadingQueueState
extends GameState {
    private static boolean cancel;
    private static boolean done;
    private static int placeInQueue;
    private boolean aButtonDown;
    private static final LoadingQueueUI ui;

    @Override
    public void enter() {
        cancel = false;
        done = false;
        placeInQueue = -1;
        this.aButtonDown = GameWindow.activatedJoyPad != null && GameWindow.activatedJoyPad.isAPressed();
        SoundManager.instance.setMusicState("Loading");
        if (GameClient.client) {
            try {
                GameClient.instance.sendLoginQueueRequest();
            }
            catch (Exception e) {
                DebugLog.General.printException(e, "", LogSeverity.Warning);
                cancel = true;
                done = true;
            }
        }
    }

    @Override
    public GameState redirectState() {
        if (cancel) {
            return new MainScreenState();
        }
        return new GameLoadingState();
    }

    @Override
    public void render() {
        Core.getInstance().StartFrame();
        Core.getInstance().EndFrame();
        boolean useUIFBO = UIManager.useUiFbo;
        UIManager.useUiFbo = false;
        Core.getInstance().StartFrameUI();
        SpriteRenderer.instance.renderi(null, 0, 0, Core.getInstance().getScreenWidth(), Core.getInstance().getScreenHeight(), 0.0f, 0.0f, 0.0f, 1.0f, null);
        if (placeInQueue >= 0) {
            MainScreenState.instance.renderBackground();
            UIManager.render();
            ActiveMods.renderUI();
            ui.render();
        }
        Core.getInstance().EndFrameUI();
        UIManager.useUiFbo = useUIFBO;
    }

    @Override
    public GameStateMachine.StateAction update() {
        boolean bAButtonDown;
        if (!GameClient.client) {
            return GameStateMachine.StateAction.Continue;
        }
        if (GameClient.client && GameClient.connection == null) {
            DebugLog.General.warn("Connection lost");
            cancel = true;
            done = true;
        }
        boolean bl = bAButtonDown = GameWindow.activatedJoyPad != null && GameWindow.activatedJoyPad.isAPressed();
        if (bAButtonDown) {
            if (this.aButtonDown) {
                bAButtonDown = false;
            }
        } else {
            this.aButtonDown = false;
        }
        if (bAButtonDown || GameKeyboard.isKeyDownRaw(1) || !GameClient.instance.connected) {
            cancel = true;
            SoundManager.instance.setMusicState("MainMenu");
            if (GameClient.connection != null) {
                GameClient.instance.connected = false;
                GameClient.client = false;
                GameClient.connection.forceDisconnect("loading-queue-canceled");
                GameClient.connection = null;
                ConnectionManager.getInstance().process();
            }
            return GameStateMachine.StateAction.Continue;
        }
        if (done) {
            return GameStateMachine.StateAction.Continue;
        }
        return GameStateMachine.StateAction.Remain;
    }

    public static void onConnectionImmediate() {
        done = true;
    }

    public static void onPlaceInQueue(int place, HashMap<String, Object> serverInformation) {
        placeInQueue = place;
        ui.setPlaceInQueue(place);
        ui.setServerInformation(serverInformation);
    }

    static {
        placeInQueue = -1;
        ui = new LoadingQueueUI();
    }
}

