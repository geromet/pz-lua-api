/*
 * Decompiled with CFR 0.152.
 */
package zombie.gameStates;

import zombie.Lua.LuaEventManager;
import zombie.UsedFromLua;
import zombie.core.Core;
import zombie.gameStates.GameState;
import zombie.gameStates.GameStateMachine;
import zombie.ui.UIManager;

@UsedFromLua
public class TermsOfServiceState
extends GameState {
    private boolean exit;
    private boolean created;

    @Override
    public void enter() {
        LuaEventManager.triggerEvent("OnGameStateEnter", this);
        if (!this.created) {
            this.exit = true;
        }
    }

    @Override
    public void exit() {
        UIManager.clearArrays();
    }

    @Override
    public GameStateMachine.StateAction update() {
        if (this.exit) {
            return GameStateMachine.StateAction.Continue;
        }
        return GameStateMachine.StateAction.Remain;
    }

    @Override
    public void render() {
        Core.getInstance().StartFrame();
        Core.getInstance().EndFrame();
        if (Core.getInstance().StartFrameUI()) {
            UIManager.render();
        }
        Core.getInstance().EndFrameUI();
    }

    public Object fromLua0(String func) {
        switch (func) {
            case "created": {
                this.created = true;
                return null;
            }
            case "exit": {
                this.exit = true;
                return null;
            }
        }
        throw new IllegalArgumentException("unhandled \"" + func + "\"");
    }
}

