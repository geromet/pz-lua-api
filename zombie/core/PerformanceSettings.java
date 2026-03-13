/*
 * Decompiled with CFR 0.152.
 */
package zombie.core;

import zombie.UsedFromLua;
import zombie.core.Core;
import zombie.core.math.PZMath;
import zombie.iso.IsoPuddles;
import zombie.iso.IsoWater;
import zombie.ui.UIManager;

@UsedFromLua
public final class PerformanceSettings {
    public static int manualFrameSkips;
    private static int lockFps;
    private static boolean uncappedFps;
    public static int waterQuality;
    public static int puddlesQuality;
    public static boolean newRoofHiding;
    public static boolean lightingThread;
    public static int lightingFps;
    public static boolean auto3DZombies;
    public static final PerformanceSettings instance;
    public static boolean interpolateAnims;
    public static int animationSkip;
    public static boolean modelLighting;
    public static int zombieAnimationSpeedFalloffCount;
    public static int zombieBonusFullspeedFalloff;
    public static int baseStaticAnimFramerate;
    public static boolean useFbos;
    public static int numberZombiesBlended;
    public static boolean fboRenderChunk;
    public static int fogQuality;
    public static int viewConeOpacity;

    public static int getLockFPS() {
        return lockFps;
    }

    public static void setLockFPS(int lockFPS) {
        lockFps = lockFPS;
    }

    public boolean isFramerateUncapped() {
        return uncappedFps;
    }

    public void setFramerateUncapped(boolean uncappedFPS) {
        uncappedFps = uncappedFPS;
    }

    public int getFramerate() {
        return PerformanceSettings.getLockFPS();
    }

    public void setFramerate(int framerate) {
        PerformanceSettings.setLockFPS(framerate);
    }

    public void setLightingQuality(int lighting) {
    }

    public int getLightingQuality() {
        return 0;
    }

    public void setWaterQuality(int water) {
        waterQuality = water;
        IsoWater.getInstance().applyWaterQuality();
    }

    public int getWaterQuality() {
        return waterQuality;
    }

    public void setPuddlesQuality(int puddles) {
        puddlesQuality = puddles;
        if (puddles > 2 || puddles < 0) {
            puddlesQuality = 0;
        }
        IsoPuddles.getInstance().applyPuddlesQuality();
    }

    public int getPuddlesQuality() {
        return puddlesQuality;
    }

    public void setNewRoofHiding(boolean enabled) {
        newRoofHiding = enabled;
    }

    public boolean getNewRoofHiding() {
        return newRoofHiding;
    }

    public void setLightingFPS(int fps) {
        lightingFps = fps = Math.max(1, Math.min(120, fps));
        System.out.println("LightingFPS set to " + lightingFps);
    }

    public int getLightingFPS() {
        return lightingFps;
    }

    public int getUIRenderFPS() {
        return UIManager.useUiFbo ? Core.getInstance().getOptionUIRenderFPS() : lockFps;
    }

    public int getFogQuality() {
        return fogQuality;
    }

    public void setFogQuality(int fogQuality) {
        PerformanceSettings.fogQuality = PZMath.clamp(fogQuality, 0, 2);
    }

    public int getViewConeOpacity() {
        return viewConeOpacity;
    }

    public void setViewConeOpacity(int viewConeOpacity) {
        PerformanceSettings.viewConeOpacity = PZMath.clamp(viewConeOpacity, 0, 5);
    }

    static {
        lockFps = 60;
        newRoofHiding = true;
        lightingThread = true;
        lightingFps = 15;
        instance = new PerformanceSettings();
        interpolateAnims = true;
        animationSkip = 1;
        modelLighting = true;
        zombieAnimationSpeedFalloffCount = 6;
        zombieBonusFullspeedFalloff = 3;
        baseStaticAnimFramerate = 60;
        numberZombiesBlended = 20;
        fboRenderChunk = true;
        viewConeOpacity = 3;
    }
}

