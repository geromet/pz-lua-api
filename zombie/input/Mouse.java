/*
 * Decompiled with CFR 0.152.
 */
package zombie.input;

import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.IntBuffer;
import javax.imageio.ImageIO;
import org.lwjgl.BufferUtils;
import org.lwjglx.LWJGLException;
import org.lwjglx.input.Cursor;
import zombie.GameTime;
import zombie.UsedFromLua;
import zombie.ZomboidFileSystem;
import zombie.core.Core;
import zombie.core.SpriteRenderer;
import zombie.core.textures.Texture;
import zombie.input.AimingReticle;
import zombie.input.MouseState;
import zombie.input.MouseStateCache;

@UsedFromLua
public final class Mouse {
    protected static int x;
    protected static int y;
    private static float timeRightPressed;
    private static final float TIME_RIGHT_PRESSED_SECONDS = 0.15f;
    public static final int BTN_OFFSET = 10000;
    public static final int BTN_0 = 10000;
    public static final int BTN_1 = 10001;
    public static final int BTN_2 = 10002;
    public static final int BTN_3 = 10003;
    public static final int BTN_4 = 10004;
    public static final int BTN_5 = 10005;
    public static final int BTN_6 = 10006;
    public static final int BTN_7 = 10007;
    public static final int LMB = 10000;
    public static final int RMB = 10001;
    public static final int MMB = 10002;
    public static boolean[] buttonDownStates;
    public static boolean[] buttonPrevStates;
    public static long lastActivity;
    public static int wheelDelta;
    private static final MouseStateCache s_mouseStateCache;
    public static boolean[] uiCaptured;
    static Cursor blankCursor;
    static Cursor defaultCursor;
    private static boolean isCursorVisible;
    private static Texture mouseCursorTexture;

    public static int getWheelState() {
        return wheelDelta;
    }

    public static int getButtonCount() {
        return s_mouseStateCache.getState().getButtonCount();
    }

    public static synchronized int getXA() {
        return x;
    }

    public static synchronized int getYA() {
        return y;
    }

    public static synchronized int getX() {
        return (int)((float)x * Core.getInstance().getZoom(0));
    }

    public static synchronized int getY() {
        return (int)((float)y * Core.getInstance().getZoom(0));
    }

    public static boolean isButtonDown(int number) {
        if (buttonDownStates != null) {
            return buttonDownStates[number];
        }
        return false;
    }

    public static boolean wasButtonDown(int number) {
        if (buttonPrevStates != null) {
            return buttonPrevStates[number];
        }
        return false;
    }

    public static boolean isButtonPressed(int number) {
        if (buttonDownStates != null && buttonPrevStates != null) {
            return !buttonPrevStates[number] && buttonDownStates[number];
        }
        return false;
    }

    public static boolean isButtonReleased(int number) {
        if (buttonDownStates != null && buttonPrevStates != null) {
            return buttonPrevStates[number] && !buttonDownStates[number];
        }
        return false;
    }

    public static void UIBlockButtonDown(int number) {
        Mouse.uiCaptured[number] = true;
    }

    public static boolean isButtonDownUICheck(int number) {
        if (buttonDownStates == null) {
            return false;
        }
        boolean b = buttonDownStates[number];
        if (!b) {
            Mouse.uiCaptured[number] = false;
        } else if (uiCaptured[number]) {
            return false;
        }
        if (number == 1) {
            return Mouse.isRightDelay();
        }
        return b;
    }

    public static boolean isRightDelay() {
        if (uiCaptured[1] || buttonDownStates == null || !buttonDownStates[1]) {
            return false;
        }
        return timeRightPressed >= 0.15f;
    }

    public static boolean isLeftDown() {
        return Mouse.isButtonDown(0);
    }

    public static boolean isLeftPressed() {
        return Mouse.isButtonPressed(0);
    }

    public static boolean isLeftReleased() {
        return Mouse.isButtonReleased(0);
    }

    public static boolean isLeftUp() {
        return !Mouse.isButtonDown(0);
    }

    public static boolean isMiddleDown() {
        return Mouse.isButtonDown(2);
    }

    public static boolean isMiddlePressed() {
        return Mouse.isButtonPressed(2);
    }

    public static boolean isMiddleReleased() {
        return Mouse.isButtonReleased(2);
    }

    public static boolean isMiddleUp() {
        return !Mouse.isButtonDown(2);
    }

    public static boolean isRightDown() {
        return Mouse.isButtonDown(1);
    }

    public static boolean isRightPressed() {
        return Mouse.isButtonPressed(1);
    }

    public static boolean isRightReleased() {
        return Mouse.isButtonReleased(1);
    }

    public static boolean isRightUp() {
        return !Mouse.isButtonDown(1);
    }

    public static synchronized void update() {
        int i;
        boolean bActivity;
        MouseState state = s_mouseStateCache.getState();
        if (!state.isCreated()) {
            s_mouseStateCache.swap();
            try {
                org.lwjglx.input.Mouse.create();
            }
            catch (LWJGLException e) {
                e.printStackTrace();
            }
            return;
        }
        int lastX = x;
        int lastY = y;
        x = state.getX();
        y = Core.getInstance().getScreenHeight() - state.getY() - 1;
        wheelDelta = state.getDWheel();
        state.resetDWheel();
        boolean bl = bActivity = lastX != x || lastY != y || wheelDelta != 0;
        if (buttonDownStates == null) {
            buttonDownStates = new boolean[state.getButtonCount()];
        }
        if (buttonPrevStates == null) {
            buttonPrevStates = new boolean[state.getButtonCount()];
        }
        for (i = 0; i < buttonDownStates.length; ++i) {
            Mouse.buttonPrevStates[i] = buttonDownStates[i];
        }
        for (i = 0; i < buttonDownStates.length; ++i) {
            if (buttonDownStates[i] != state.isButtonDown(i)) {
                bActivity = true;
            }
            Mouse.buttonDownStates[i] = state.isButtonDown(i);
        }
        timeRightPressed = buttonDownStates[1] ? (timeRightPressed += GameTime.getInstance().getRealworldSecondsSinceLastUpdate()) : 0.0f;
        if (bActivity) {
            lastActivity = System.currentTimeMillis();
        }
        s_mouseStateCache.swap();
        AimingReticle.update();
    }

    public static void poll() {
        s_mouseStateCache.poll();
    }

    public static synchronized void setXY(int x, int y) {
        s_mouseStateCache.getState().setCursorPosition(x, Core.getInstance().getOffscreenHeight(0) - 1 - y);
    }

    public static Cursor loadCursor(String filename) throws LWJGLException {
        File file = ZomboidFileSystem.instance.getMediaFile("ui/" + filename);
        try {
            BufferedImage img = ImageIO.read(file);
            int w = img.getWidth();
            int h = img.getHeight();
            int[] rgbData = new int[w * h];
            for (int i = 0; i < rgbData.length; ++i) {
                int x = i % w;
                int y = h - 1 - i / w;
                rgbData[i] = img.getRGB(x, y);
            }
            IntBuffer buffer = BufferUtils.createIntBuffer(w * h);
            buffer.put(rgbData);
            buffer.rewind();
            boolean xHotspot = true;
            boolean yHotspot = true;
            return new Cursor(w, h, 1, 1, 1, buffer, null);
        }
        catch (Exception ex) {
            return null;
        }
    }

    public static void initCustomCursor() {
        if (blankCursor == null) {
            try {
                blankCursor = Mouse.loadCursor("cursor_blank.png");
                defaultCursor = Mouse.loadCursor("cursor_white.png");
            }
            catch (LWJGLException e) {
                e.printStackTrace();
            }
        }
        if (defaultCursor == null) {
            return;
        }
        try {
            org.lwjglx.input.Mouse.setNativeCursor(defaultCursor);
        }
        catch (LWJGLException e) {
            e.printStackTrace();
        }
    }

    public static void setCursorVisible(boolean bVisible) {
        isCursorVisible = bVisible;
    }

    public static boolean isCursorVisible() {
        return isCursorVisible;
    }

    public static void renderCursorTexture() {
        if (!Mouse.isCursorVisible()) {
            return;
        }
        if (mouseCursorTexture == null) {
            mouseCursorTexture = Texture.getSharedTexture("media/ui/cursor_white.png");
        }
        if (mouseCursorTexture == null || !mouseCursorTexture.isReady()) {
            return;
        }
        int mouseX = Mouse.getXA();
        int mouseY = Mouse.getYA();
        boolean hotSpotX = true;
        boolean hotSpotY = true;
        SpriteRenderer.instance.render(mouseCursorTexture, mouseX - 1, mouseY - 1, mouseCursorTexture.getWidth(), mouseCursorTexture.getHeight(), 1.0f, 1.0f, 1.0f, 1.0f, null);
    }

    static {
        s_mouseStateCache = new MouseStateCache();
        uiCaptured = new boolean[10];
        isCursorVisible = true;
    }
}

