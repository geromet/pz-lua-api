/*
 * Decompiled with CFR 0.152.
 */
package zombie.iso.weather;

import java.util.ArrayList;
import zombie.GameTime;
import zombie.UsedFromLua;
import zombie.characters.IsoPlayer;
import zombie.core.Core;
import zombie.core.SceneShaderStore;
import zombie.core.math.PZMath;
import zombie.core.opengl.RenderSettings;
import zombie.core.random.Rand;
import zombie.debug.LineDrawer;
import zombie.iso.IsoUtils;
import zombie.iso.Vector2;
import zombie.iso.weather.ClimateColorInfo;
import zombie.iso.weather.ClimateManager;
import zombie.iso.weather.fx.SteppedUpdateFloat;

@UsedFromLua
public class WorldFlares {
    public static final boolean ENABLED = true;
    public static boolean debugDraw;
    public static int nextId;
    private static final ArrayList<Flare> flares;

    public static void Clear() {
        flares.clear();
    }

    public static int getFlareCount() {
        return flares.size();
    }

    public static Flare getFlare(int index) {
        return flares.get(index);
    }

    public static Flare getFlareID(int id) {
        for (int i = 0; i < flares.size(); ++i) {
            if (WorldFlares.flares.get((int)i).id != id) continue;
            return flares.get(i);
        }
        return null;
    }

    public static void launchFlare(float lifetime, int x, int y, int range, float windSpeed, float r, float g, float b, float ri, float gi, float bi) {
        if (flares.size() > 100) {
            flares.remove(0);
        }
        Flare flare = new Flare();
        flare.id = nextId++;
        flare.x = x;
        flare.y = y;
        flare.range = range;
        flare.windSpeed = windSpeed;
        flare.color.setExterior(r, g, b, 1.0f);
        flare.color.setInterior(ri, gi, bi, 1.0f);
        flare.hasLaunched = true;
        flare.maxLifeTime = lifetime;
        flares.add(flare);
    }

    public static void update() {
        for (int i = flares.size() - 1; i >= 0; --i) {
            flares.get(i).update();
            if (WorldFlares.flares.get((int)i).hasLaunched) continue;
            flares.remove(i);
        }
    }

    public static void applyFlaresForPlayer(RenderSettings.PlayerRenderSettings renderSettings, int plrIndex, IsoPlayer player) {
        for (int i = flares.size() - 1; i >= 0; --i) {
            if (!WorldFlares.flares.get((int)i).hasLaunched) continue;
            flares.get(i).applyFlare(renderSettings, plrIndex, player);
        }
    }

    public static void setDebugDraw(boolean b) {
        debugDraw = b;
    }

    public static boolean getDebugDraw() {
        return debugDraw;
    }

    public static void debugRender() {
        if (!debugDraw) {
            return;
        }
        double step = 0.15707963267948966;
        float z = 0.0f;
        for (int i = flares.size() - 1; i >= 0; --i) {
            Flare flare = flares.get(i);
            float range = 0.5f;
            for (double theta = 0.0; theta < Math.PI * 2; theta += 0.15707963267948966) {
                WorldFlares.DrawIsoLine(flare.x + (float)flare.range * (float)Math.cos(theta), flare.y + (float)flare.range * (float)Math.sin(theta), flare.x + (float)flare.range * (float)Math.cos(theta + 0.15707963267948966), flare.y + (float)flare.range * (float)Math.sin(theta + 0.15707963267948966), 0.0f, 1.0f, 1.0f, 1.0f, 0.25f, 1);
                WorldFlares.DrawIsoLine(flare.x + 0.5f * (float)Math.cos(theta), flare.y + 0.5f * (float)Math.sin(theta), flare.x + 0.5f * (float)Math.cos(theta + 0.15707963267948966), flare.y + 0.5f * (float)Math.sin(theta + 0.15707963267948966), 0.0f, 1.0f, 1.0f, 1.0f, 0.25f, 1);
            }
        }
    }

    private static void DrawIsoLine(float x, float y, float x2, float y2, float z, float r, float g, float b, float a, int thickness) {
        float sx = IsoUtils.XToScreenExact(x, y, z, 0);
        float sy = IsoUtils.YToScreenExact(x, y, z, 0);
        float sx2 = IsoUtils.XToScreenExact(x2, y2, z, 0);
        float sy2 = IsoUtils.YToScreenExact(x2, y2, z, 0);
        LineDrawer.drawLine(sx, sy, sx2, sy2, r, g, b, a, thickness);
    }

    static {
        flares = new ArrayList();
    }

    @UsedFromLua
    public static class Flare {
        private int id;
        private float x;
        private float y;
        private int range;
        private float windSpeed;
        private final ClimateColorInfo color = new ClimateColorInfo(1.0f, 0.0f, 0.0f, 1.0f, 1.0f, 0.0f, 0.0f, 1.0f);
        private boolean hasLaunched;
        private final SteppedUpdateFloat intensity = new SteppedUpdateFloat(0.0f, 0.01f, 0.0f, 1.0f);
        private float maxLifeTime;
        private float lifeTime;
        private int nextRandomTargetIntens = 10;
        private float perc;
        private final PlayerFlareLightInfo[] infos = new PlayerFlareLightInfo[4];

        public Flare() {
            for (int i = 0; i < this.infos.length; ++i) {
                this.infos[i] = new PlayerFlareLightInfo();
            }
        }

        public int getId() {
            return this.id;
        }

        public float getX() {
            return this.x;
        }

        public float getY() {
            return this.y;
        }

        public int getRange() {
            return this.range;
        }

        public float getWindSpeed() {
            return this.windSpeed;
        }

        public ClimateColorInfo getColor() {
            return this.color;
        }

        public boolean isHasLaunched() {
            return this.hasLaunched;
        }

        public float getIntensity() {
            return this.intensity.value();
        }

        public float getMaxLifeTime() {
            return this.maxLifeTime;
        }

        public float getLifeTime() {
            return this.lifeTime;
        }

        public float getPercent() {
            return this.perc;
        }

        public float getIntensityPlayer(int index) {
            return this.infos[index].intensity;
        }

        public float getLerpPlayer(int index) {
            return this.infos[index].lerp;
        }

        public float getDistModPlayer(int index) {
            return this.infos[index].distMod;
        }

        public ClimateColorInfo getColorPlayer(int index) {
            return this.infos[index].flareCol;
        }

        public ClimateColorInfo getOutColorPlayer(int index) {
            return this.infos[index].outColor;
        }

        private int GetDistance(int dx, int dy, int sx, int sy) {
            return (int)Math.sqrt(Math.pow(dx - sx, 2.0) + Math.pow(dy - sy, 2.0));
        }

        private void update() {
            if (this.hasLaunched) {
                if (this.lifeTime > this.maxLifeTime) {
                    this.hasLaunched = false;
                    return;
                }
                this.perc = this.lifeTime / this.maxLifeTime;
                this.nextRandomTargetIntens = (int)((float)this.nextRandomTargetIntens - GameTime.instance.getMultiplier());
                if (this.nextRandomTargetIntens <= 0) {
                    this.intensity.setTarget(Rand.Next(0.8f, 1.0f));
                    this.nextRandomTargetIntens = Rand.Next(5, 30);
                }
                this.intensity.update(GameTime.instance.getMultiplier());
                if (this.windSpeed > 0.0f) {
                    Vector2 add = new Vector2(this.windSpeed / 60.0f * ClimateManager.getInstance().getWindIntensity() * (float)Math.sin(ClimateManager.getInstance().getWindAngleRadians()), this.windSpeed / 60.0f * ClimateManager.getInstance().getWindIntensity() * (float)Math.cos(ClimateManager.getInstance().getWindAngleRadians()));
                    this.x += add.x * GameTime.instance.getMultiplier();
                    this.y += add.y * GameTime.instance.getMultiplier();
                }
                for (int i = 0; i < 4; ++i) {
                    PlayerFlareLightInfo info = this.infos[i];
                    IsoPlayer player = IsoPlayer.players[i];
                    if (player == null) {
                        info.intensity = 0.0f;
                        continue;
                    }
                    int dist = this.GetDistance((int)this.x, (int)this.y, (int)player.getX(), (int)player.getY());
                    if (dist > this.range) {
                        info.intensity = 0.0f;
                        info.lerp = 1.0f;
                    } else {
                        info.distMod = 1.0f - (float)dist / (float)this.range;
                        info.lerp = this.perc < 0.75f ? 0.0f : (this.perc - 0.75f) / 0.25f;
                        info.intensity = this.intensity.value();
                    }
                    float lerp = (1.0f - info.lerp) * info.distMod * info.intensity;
                    ClimateManager.getInstance().dayLightStrength.finalValue += (1.0f - ClimateManager.getInstance().dayLightStrength.finalValue) * lerp;
                    if (player == null) continue;
                    player.dirtyRecalcGridStackTime = 1.0f;
                }
                this.lifeTime += GameTime.instance.getMultiplier();
            }
        }

        private void applyFlare(RenderSettings.PlayerRenderSettings renderSettings, int plrIndex, IsoPlayer player) {
            PlayerFlareLightInfo linfo = this.infos[plrIndex];
            if (linfo.distMod > 0.0f) {
                float darkness = 1.0f - renderSettings.cmDayLightStrength;
                darkness = renderSettings.cmNightStrength > darkness ? renderSettings.cmNightStrength : darkness;
                darkness = PZMath.clamp(darkness * 2.0f, 0.0f, 1.0f);
                float lerp = 1.0f - linfo.lerp;
                ClimateColorInfo gl = renderSettings.cmGlobalLight;
                linfo.outColor.setTo(gl);
                linfo.outColor.getExterior().g *= 1.0f - darkness * (lerp *= linfo.distMod) * linfo.intensity * 0.5f;
                linfo.outColor.getInterior().g *= 1.0f - darkness * lerp * linfo.intensity * 0.5f;
                linfo.outColor.getExterior().b *= 1.0f - darkness * lerp * linfo.intensity * 0.8f;
                linfo.outColor.getInterior().b *= 1.0f - darkness * lerp * linfo.intensity * 0.8f;
                linfo.flareCol.setTo(this.color);
                linfo.flareCol.scale(darkness);
                linfo.flareCol.getExterior().a = 1.0f;
                linfo.flareCol.getInterior().a = 1.0f;
                linfo.outColor.getExterior().r = linfo.outColor.getExterior().r > linfo.flareCol.getExterior().r ? linfo.outColor.getExterior().r : linfo.flareCol.getExterior().r;
                linfo.outColor.getExterior().g = linfo.outColor.getExterior().g > linfo.flareCol.getExterior().g ? linfo.outColor.getExterior().g : linfo.flareCol.getExterior().g;
                linfo.outColor.getExterior().b = linfo.outColor.getExterior().b > linfo.flareCol.getExterior().b ? linfo.outColor.getExterior().b : linfo.flareCol.getExterior().b;
                linfo.outColor.getExterior().a = linfo.outColor.getExterior().a > linfo.flareCol.getExterior().a ? linfo.outColor.getExterior().a : linfo.flareCol.getExterior().a;
                linfo.outColor.getInterior().r = linfo.outColor.getInterior().r > linfo.flareCol.getInterior().r ? linfo.outColor.getInterior().r : linfo.flareCol.getInterior().r;
                linfo.outColor.getInterior().g = linfo.outColor.getInterior().g > linfo.flareCol.getInterior().g ? linfo.outColor.getInterior().g : linfo.flareCol.getInterior().g;
                linfo.outColor.getInterior().b = linfo.outColor.getInterior().b > linfo.flareCol.getInterior().b ? linfo.outColor.getInterior().b : linfo.flareCol.getInterior().b;
                linfo.outColor.getInterior().a = linfo.outColor.getInterior().a > linfo.flareCol.getInterior().a ? linfo.outColor.getInterior().a : linfo.flareCol.getInterior().a;
                float useLerp = 1.0f - lerp * linfo.intensity;
                linfo.outColor.interp(gl, useLerp, gl);
                float ambient = ClimateManager.lerp(useLerp, 0.35f, renderSettings.cmAmbient);
                renderSettings.cmAmbient = renderSettings.cmAmbient > ambient ? renderSettings.cmAmbient : ambient;
                float daylight = ClimateManager.lerp(useLerp, 0.6f * linfo.intensity, renderSettings.cmDayLightStrength);
                float f = renderSettings.cmDayLightStrength = renderSettings.cmDayLightStrength > daylight ? renderSettings.cmDayLightStrength : daylight;
                if (SceneShaderStore.weatherShader != null && Core.getInstance().getOffscreenBuffer() != null) {
                    float desaturation = ClimateManager.lerp(useLerp, 1.0f * darkness, renderSettings.cmDesaturation);
                    renderSettings.cmDesaturation = renderSettings.cmDesaturation > desaturation ? renderSettings.cmDesaturation : desaturation;
                }
            }
        }
    }

    private static class PlayerFlareLightInfo {
        private float intensity;
        private float lerp;
        private float distMod;
        private final ClimateColorInfo flareCol = new ClimateColorInfo(1.0f, 1.0f, 1.0f, 1.0f);
        private final ClimateColorInfo outColor = new ClimateColorInfo(1.0f, 1.0f, 1.0f, 1.0f);

        private PlayerFlareLightInfo() {
        }
    }
}

