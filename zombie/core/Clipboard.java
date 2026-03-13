/*
 * Decompiled with CFR 0.152.
 */
package zombie.core;

import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWErrorCallback;
import zombie.UsedFromLua;

@UsedFromLua
public final class Clipboard {
    private static Thread mainThread;
    private static String previousKnownValue;
    private static String delaySetMainThread;

    public static void initMainThread() {
        mainThread = Thread.currentThread();
        previousKnownValue = Clipboard.getClipboard();
    }

    public static void rememberCurrentValue() {
        if (Thread.currentThread() == mainThread) {
            GLFWErrorCallback errorCallback = GLFW.glfwSetErrorCallback(null);
            try {
                previousKnownValue = new String(GLFW.glfwGetClipboardString(0L));
            }
            catch (Throwable t) {
                previousKnownValue = "";
            }
            finally {
                GLFW.glfwSetErrorCallback(errorCallback);
            }
        }
    }

    public static synchronized String getClipboard() {
        if (Thread.currentThread() == mainThread) {
            GLFWErrorCallback errorCallback = GLFW.glfwSetErrorCallback(null);
            try {
                String string = previousKnownValue = new String(GLFW.glfwGetClipboardString(0L));
                return string;
            }
            catch (Throwable t) {
                previousKnownValue = "";
                String string = "";
                return string;
            }
            finally {
                GLFW.glfwSetErrorCallback(errorCallback);
            }
        }
        return previousKnownValue;
    }

    public static synchronized void setClipboard(String str) {
        previousKnownValue = str;
        if (Thread.currentThread() == mainThread) {
            GLFW.glfwSetClipboardString(0L, str);
        } else {
            delaySetMainThread = str;
        }
    }

    public static synchronized void updateMainThread() {
        if (delaySetMainThread != null) {
            Clipboard.setClipboard(delaySetMainThread);
            delaySetMainThread = null;
        }
    }
}

