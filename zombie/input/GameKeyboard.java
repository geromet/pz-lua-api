/*
 * Decompiled with CFR 0.152.
 */
package zombie.input;

import org.lwjglx.input.KeyEventQueue;
import zombie.GameWindow;
import zombie.Lua.LuaEventManager;
import zombie.Lua.LuaManager;
import zombie.UsedFromLua;
import zombie.core.Core;
import zombie.core.opengl.RenderThread;
import zombie.input.KeyboardStateCache;
import zombie.input.Mouse;
import zombie.ui.UIManager;

@UsedFromLua
public final class GameKeyboard {
    private static boolean[] down;
    private static boolean[] lastDown;
    private static boolean[] eatKey;
    public static boolean noEventsWhileLoading;
    public static boolean doLuaKeyPressed;
    private static final KeyboardStateCache s_keyboardStateCache;

    public static void update() {
        if (!s_keyboardStateCache.getState().isCreated()) {
            s_keyboardStateCache.swap();
            return;
        }
        int c = s_keyboardStateCache.getState().getKeyCount();
        if (down == null) {
            down = new boolean[c];
            lastDown = new boolean[c];
            eatKey = new boolean[c];
        }
        boolean bDoingTextEntry = Core.currentTextEntryBox != null && Core.currentTextEntryBox.isDoingTextEntry();
        for (int n = 1; n < c; ++n) {
            GameKeyboard.lastDown[n] = down[n];
            GameKeyboard.down[n] = s_keyboardStateCache.getState().isKeyDown(n);
            if (!down[n] && lastDown[n]) {
                if (eatKey[n]) {
                    GameKeyboard.eatKey[n] = false;
                    continue;
                }
                if (noEventsWhileLoading || bDoingTextEntry || LuaManager.thread == UIManager.defaultthread && UIManager.onKeyRelease(n)) continue;
                if (Core.debug && !doLuaKeyPressed) {
                    System.out.println("KEY RELEASED " + n + " doLuaKeyPressed=false");
                }
                if (LuaManager.thread == UIManager.defaultthread && doLuaKeyPressed) {
                    LuaEventManager.triggerEvent("OnKeyPressed", n);
                }
                if (LuaManager.thread == UIManager.defaultthread) {
                    LuaEventManager.triggerEvent("OnCustomUIKey", n);
                    LuaEventManager.triggerEvent("OnCustomUIKeyReleased", n);
                }
            }
            if (down[n] && lastDown[n]) {
                if (noEventsWhileLoading || bDoingTextEntry || LuaManager.thread == UIManager.defaultthread && UIManager.onKeyRepeat(n)) continue;
                if (LuaManager.thread == UIManager.defaultthread && doLuaKeyPressed) {
                    LuaEventManager.triggerEvent("OnKeyKeepPressed", n);
                }
            }
            if (!down[n] || lastDown[n] || noEventsWhileLoading || bDoingTextEntry || eatKey[n] || LuaManager.thread == UIManager.defaultthread && UIManager.onKeyPress(n) || eatKey[n]) continue;
            if (LuaManager.thread == UIManager.defaultthread && doLuaKeyPressed) {
                LuaEventManager.triggerEvent("OnKeyStartPressed", n);
            }
            if (LuaManager.thread != UIManager.defaultthread) continue;
            LuaEventManager.triggerEvent("OnCustomUIKeyPressed", n);
        }
        s_keyboardStateCache.swap();
    }

    public static void poll() {
        s_keyboardStateCache.poll();
    }

    public static boolean isKeyDownRaw(int key) {
        if (down == null) {
            return false;
        }
        return down[key];
    }

    public static boolean wasKeyDownRaw(int key) {
        if (lastDown == null) {
            return false;
        }
        return lastDown[key];
    }

    public static boolean isKeyPressed(int key) {
        return GameKeyboard.isKeyDown(key) && !GameKeyboard.wasKeyDown(key);
    }

    public static boolean isKeyPressed(String keyName) {
        return GameKeyboard.isKeyPressed(Core.getInstance().getKey(keyName)) || GameKeyboard.isKeyPressed(Core.getInstance().getAltKey(keyName));
    }

    public static int whichKeyPressed(String keyName) {
        if (GameKeyboard.isKeyPressed(Core.getInstance().getKey(keyName))) {
            return Core.getInstance().getKey(keyName);
        }
        if (GameKeyboard.isKeyPressed(Core.getInstance().getAltKey(keyName))) {
            return Core.getInstance().getAltKey(keyName);
        }
        return 0;
    }

    public static boolean isKeyDown(int key) {
        Core.KeyBinding keyB = Core.getInstance().getKeyBinding(key);
        if (Core.getInstance().invalidBindingShiftCtrl(keyB)) {
            return false;
        }
        if (key >= 10000) {
            return Mouse.isButtonDownUICheck(key - 10000);
        }
        if (Core.currentTextEntryBox != null && Core.currentTextEntryBox.isDoingTextEntry()) {
            return false;
        }
        if (down == null) {
            return false;
        }
        return down[key];
    }

    public static boolean isKeyDown(String keyName) {
        Core.KeyBinding keyB = Core.getInstance().getKeyBinding(keyName);
        if (Core.getInstance().invalidBindingShiftCtrl(keyB)) {
            return false;
        }
        return GameKeyboard.isKeyDown(keyB.keyValue()) || GameKeyboard.isKeyDown(keyB.altKey());
    }

    public static int whichKeyDown(String keyName) {
        if (GameKeyboard.isKeyDown(Core.getInstance().getKey(keyName))) {
            return Core.getInstance().getKey(keyName);
        }
        if (GameKeyboard.isKeyDown(Core.getInstance().getAltKey(keyName))) {
            return Core.getInstance().getAltKey(keyName);
        }
        return 0;
    }

    public static int whichKeyDownIgnoreMouse(String keyName) {
        int key = Core.getInstance().getKey(keyName);
        if (key < 10000 && GameKeyboard.isKeyDown(key)) {
            return key;
        }
        key = Core.getInstance().getAltKey(keyName);
        if (key < 10000 && GameKeyboard.isKeyDown(key)) {
            return key;
        }
        return 0;
    }

    public static boolean wasKeyDown(int key) {
        if (key >= 10000) {
            return Mouse.wasButtonDown(key - 10000);
        }
        if (Core.currentTextEntryBox != null && Core.currentTextEntryBox.isDoingTextEntry()) {
            return false;
        }
        if (lastDown == null) {
            return false;
        }
        return lastDown[key];
    }

    public static boolean wasKeyDown(String keyName) {
        return GameKeyboard.wasKeyDown(Core.getInstance().getKey(keyName)) || GameKeyboard.wasKeyDown(Core.getInstance().getAltKey(keyName));
    }

    public static int whichKeyWasDown(String keyName) {
        if (GameKeyboard.wasKeyDown(Core.getInstance().getKey(keyName))) {
            return Core.getInstance().getKey(keyName);
        }
        if (GameKeyboard.wasKeyDown(Core.getInstance().getAltKey(keyName))) {
            return Core.getInstance().getAltKey(keyName);
        }
        return 0;
    }

    public static void eatKeyPress(int key) {
        if (key < 0 || key >= eatKey.length) {
            return;
        }
        GameKeyboard.eatKey[key] = true;
    }

    public static void setDoLuaKeyPressed(boolean doIt) {
        doLuaKeyPressed = doIt;
    }

    public static KeyEventQueue getEventQueue() {
        assert (Thread.currentThread() == GameWindow.gameThread);
        return s_keyboardStateCache.getState().getEventQueue();
    }

    public static KeyEventQueue getEventQueuePolling() {
        assert (Thread.currentThread() == RenderThread.renderThread);
        return s_keyboardStateCache.getStatePolling().getEventQueue();
    }

    static {
        doLuaKeyPressed = true;
        s_keyboardStateCache = new KeyboardStateCache();
    }
}

