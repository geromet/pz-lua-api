/*
 * Decompiled with CFR 0.152.
 */
package zombie.iso.weather.fx;

import java.util.ArrayList;
import org.lwjgl.util.vector.Matrix4f;
import zombie.GameTime;
import zombie.IndieGL;
import zombie.SandboxOptions;
import zombie.UsedFromLua;
import zombie.characters.IsoPlayer;
import zombie.core.Color;
import zombie.core.Core;
import zombie.core.PerformanceSettings;
import zombie.core.SpriteRenderer;
import zombie.core.math.PZMath;
import zombie.core.opengl.RenderThread;
import zombie.core.random.Rand;
import zombie.core.skinnedmodel.shader.Shader;
import zombie.core.skinnedmodel.shader.ShaderManager;
import zombie.core.textures.Texture;
import zombie.core.textures.TextureDraw;
import zombie.debug.DebugLog;
import zombie.iso.IsoCamera;
import zombie.iso.IsoWorld;
import zombie.iso.SpriteDetails.IsoFlagType;
import zombie.iso.weather.ClimateManager;
import zombie.iso.weather.fx.CloudParticle;
import zombie.iso.weather.fx.FogParticle;
import zombie.iso.weather.fx.ParticleRectangle;
import zombie.iso.weather.fx.RainParticle;
import zombie.iso.weather.fx.SnowParticle;
import zombie.iso.weather.fx.SteppedUpdateFloat;
import zombie.iso.weather.fx.WeatherFxMask;
import zombie.iso.weather.fx.WeatherParticle;
import zombie.iso.weather.fx.WeatherParticleDrawer;
import zombie.network.GameServer;

@UsedFromLua
public class IsoWeatherFX {
    private static final boolean VERBOSE = false;
    protected static boolean debugBounds;
    private static float delta;
    private ParticleRectangle cloudParticles;
    private ParticleRectangle fogParticles;
    private ParticleRectangle snowParticles;
    private ParticleRectangle rainParticles;
    public static int cloudId;
    public static int fogId;
    public static int snowId;
    public static int rainId;
    public static float zoomMod;
    protected boolean playerIndoors;
    protected SteppedUpdateFloat windPrecipIntensity = new SteppedUpdateFloat(0.0f, 0.025f, 0.0f, 1.0f);
    protected SteppedUpdateFloat windIntensity = new SteppedUpdateFloat(0.0f, 0.005f, 0.0f, 1.0f);
    protected SteppedUpdateFloat windAngleIntensity = new SteppedUpdateFloat(0.0f, 0.005f, -1.0f, 1.0f);
    protected SteppedUpdateFloat precipitationIntensity = new SteppedUpdateFloat(0.0f, 0.005f, 0.0f, 1.0f);
    protected SteppedUpdateFloat precipitationIntensitySnow = new SteppedUpdateFloat(0.0f, 0.005f, 0.0f, 1.0f);
    protected SteppedUpdateFloat precipitationIntensityRain = new SteppedUpdateFloat(0.0f, 0.005f, 0.0f, 1.0f);
    protected SteppedUpdateFloat cloudIntensity = new SteppedUpdateFloat(0.0f, 0.005f, 0.0f, 1.0f);
    protected SteppedUpdateFloat fogIntensity = new SteppedUpdateFloat(0.0f, 0.005f, 0.0f, 1.0f);
    protected SteppedUpdateFloat windAngleMod = new SteppedUpdateFloat(0.0f, 0.005f, 0.0f, 1.0f);
    protected boolean precipitationIsSnow = true;
    private float fogOverlayAlpha;
    private final float windSpeedMax = 6.0f;
    protected float windSpeed;
    protected float windSpeedFog;
    protected float windAngle = 90.0f;
    protected float windAngleClouds = 90.0f;
    private Texture texFogCircle;
    private Texture texFogWhite;
    private final Color fogColor = new Color(1.0f, 1.0f, 1.0f, 1.0f);
    protected SteppedUpdateFloat indoorsAlphaMod = new SteppedUpdateFloat(1.0f, 0.05f, 0.0f, 1.0f);
    private final ArrayList<ParticleRectangle> particleRectangles = new ArrayList(0);
    private final WeatherParticleDrawer[][][] drawers = new WeatherParticleDrawer[4][3][4];
    protected static IsoWeatherFX instance;
    private float windUpdCounter;
    static Shader shader;
    static final Drawer[][] s_drawer;

    public IsoWeatherFX() {
        instance = this;
    }

    public void init() {
        if (GameServer.server) {
            return;
        }
        int id = 0;
        Texture[] cloudTextures = new Texture[6];
        for (int i = 0; i < cloudTextures.length; ++i) {
            cloudTextures[i] = Texture.getSharedTexture("media/textures/weather/clouds_" + i + ".png");
            if (cloudTextures[i] != null) continue;
            DebugLog.log("Missing texture: media/textures/weather/clouds_" + i + ".png");
        }
        this.cloudParticles = new ParticleRectangle(cloudId, 8192, 4096);
        WeatherParticle[] cloudparticles = new WeatherParticle[16];
        for (int i = 0; i < cloudparticles.length; ++i) {
            Texture tex = cloudTextures[Rand.Next(cloudTextures.length)];
            CloudParticle p = new CloudParticle(tex, tex.getWidth() * 8, tex.getHeight() * 8);
            p.position.set(Rand.Next(0, this.cloudParticles.getWidth()), Rand.Next(0, this.cloudParticles.getHeight()));
            p.speed = Rand.Next(0.01f, 0.1f);
            p.angleOffset = 180.0f - Rand.Next(0.0f, 360.0f);
            p.alpha = Rand.Next(0.25f, 0.75f);
            cloudparticles[i] = p;
        }
        this.cloudParticles.SetParticles(cloudparticles);
        this.cloudParticles.SetParticlesStrength(1.0f);
        this.particleRectangles.add(id, this.cloudParticles);
        cloudId = id++;
        if (this.texFogCircle == null) {
            this.texFogCircle = Texture.getSharedTexture("media/textures/weather/fogcircle_tex.png", 35);
        }
        if (this.texFogWhite == null) {
            this.texFogWhite = Texture.getSharedTexture("media/textures/weather/fogwhite_tex.png", 35);
        }
        Texture[] fogTextures = new Texture[6];
        for (int i = 0; i < fogTextures.length; ++i) {
            fogTextures[i] = Texture.getSharedTexture("media/textures/weather/fog_" + i + ".png");
            if (fogTextures[i] != null) continue;
            DebugLog.log("Missing texture: media/textures/weather/fog_" + i + ".png");
        }
        this.fogParticles = new ParticleRectangle(fogId, 2048, 1024);
        WeatherParticle[] particles = new WeatherParticle[16];
        for (int i = 0; i < particles.length; ++i) {
            Texture tex = fogTextures[Rand.Next(fogTextures.length)];
            FogParticle p = new FogParticle(tex, tex.getWidth() * 2, tex.getHeight() * 2);
            p.position.set(Rand.Next(0, this.fogParticles.getWidth()), Rand.Next(0, this.fogParticles.getHeight()));
            p.speed = Rand.Next(0.01f, 0.1f);
            p.angleOffset = 180.0f - Rand.Next(0.0f, 360.0f);
            p.alpha = Rand.Next(0.05f, 0.25f);
            particles[i] = p;
        }
        this.fogParticles.SetParticles(particles);
        this.fogParticles.SetParticlesStrength(1.0f);
        this.particleRectangles.add(id, this.fogParticles);
        fogId = id++;
        Texture[] snowTextures = new Texture[3];
        for (int i = 0; i < snowTextures.length; ++i) {
            snowTextures[i] = Texture.getSharedTexture("media/textures/weather/snow_" + (i + 1) + ".png");
            if (snowTextures[i] != null) continue;
            DebugLog.log("Missing texture: media/textures/weather/snow_" + (i + 1) + ".png");
        }
        this.snowParticles = new ParticleRectangle(snowId, 512, 512);
        WeatherParticle[] particlesSnow = new WeatherParticle[1024];
        for (int i = 0; i < particlesSnow.length; ++i) {
            SnowParticle p = new SnowParticle(snowTextures[Rand.Next(snowTextures.length)]);
            p.position.set(Rand.Next(0, this.snowParticles.getWidth()), Rand.Next(0, this.snowParticles.getHeight()));
            p.speed = Rand.Next(1.0f, 2.0f);
            p.angleOffset = 15.0f - Rand.Next(0.0f, 30.0f);
            p.alpha = Rand.Next(0.25f, 0.6f);
            particlesSnow[i] = p;
        }
        this.snowParticles.SetParticles(particlesSnow);
        this.particleRectangles.add(id, this.snowParticles);
        snowId = id++;
        this.rainParticles = new ParticleRectangle(rainId, 512, 512);
        WeatherParticle[] particlesRain = new WeatherParticle[1024];
        for (int i = 0; i < particlesRain.length; ++i) {
            RainParticle p = new RainParticle(this.texFogWhite, Rand.Next(5, 12));
            p.position.set(Rand.Next(0, this.rainParticles.getWidth()), Rand.Next(0, this.rainParticles.getHeight()));
            p.speed = Rand.Next(7, 12);
            p.angleOffset = 3.0f - Rand.Next(0.0f, 6.0f);
            p.alpha = Rand.Next(0.5f, 0.8f);
            p.color = new Color(Rand.Next(0.75f, 0.8f), Rand.Next(0.85f, 0.9f), Rand.Next(0.95f, 1.0f), 1.0f);
            particlesRain[i] = p;
        }
        this.rainParticles.SetParticles(particlesRain);
        this.particleRectangles.add(id, this.rainParticles);
        rainId = id++;
    }

    public void update() {
        float f;
        if (GameServer.server) {
            return;
        }
        this.playerIndoors = IsoCamera.frameState.camCharacterSquare != null && !IsoCamera.frameState.camCharacterSquare.has(IsoFlagType.exterior);
        GameTime gt = GameTime.getInstance();
        delta = gt.getMultiplier();
        if (!WeatherFxMask.playerHasMaskToDraw(IsoCamera.frameState.playerIndex)) {
            if (this.playerIndoors && this.indoorsAlphaMod.value() > 0.0f) {
                this.indoorsAlphaMod.setTarget(this.indoorsAlphaMod.value() - 0.05f * delta);
            } else if (!this.playerIndoors && this.indoorsAlphaMod.value() < 1.0f) {
                this.indoorsAlphaMod.setTarget(this.indoorsAlphaMod.value() + 0.05f * delta);
            }
        } else {
            this.indoorsAlphaMod.setTarget(1.0f);
        }
        this.indoorsAlphaMod.update(delta);
        this.cloudIntensity.update(delta);
        this.windIntensity.update(delta);
        this.windPrecipIntensity.update(delta);
        this.windAngleIntensity.update(delta);
        this.precipitationIntensity.update(delta);
        this.fogIntensity.update(delta);
        if (this.precipitationIsSnow) {
            this.precipitationIntensitySnow.setTarget(this.precipitationIntensity.getTarget());
        } else {
            this.precipitationIntensitySnow.setTarget(0.0f);
        }
        if (!this.precipitationIsSnow) {
            this.precipitationIntensityRain.setTarget(this.precipitationIntensity.getTarget());
        } else {
            this.precipitationIntensityRain.setTarget(0.0f);
        }
        if (this.precipitationIsSnow) {
            this.windAngleMod.setTarget(0.3f);
        } else {
            this.windAngleMod.setTarget(0.6f);
        }
        this.precipitationIntensitySnow.update(delta);
        this.precipitationIntensityRain.update(delta);
        this.windAngleMod.update(delta);
        float f2 = this.fogIntensity.value() * this.indoorsAlphaMod.value();
        this.fogOverlayAlpha = 0.8f * f2;
        this.windUpdCounter += 1.0f;
        if (f > 15.0f) {
            this.windUpdCounter = 0.0f;
            if (this.windAngleIntensity.value() > 0.0f) {
                this.windAngle = IsoWeatherFX.lerp(this.windPrecipIntensity.value(), 90.0f, 0.0f + 54.0f * this.windAngleMod.value());
                this.windAngleClouds = this.windAngleIntensity.value() < 0.5f ? IsoWeatherFX.lerp(this.windAngleIntensity.value() * 2.0f, 90.0f, 0.0f) : IsoWeatherFX.lerp((this.windAngleIntensity.value() - 0.5f) * 2.0f, 360.0f, 270.0f);
            } else if (this.windAngleIntensity.value() < 0.0f) {
                this.windAngle = IsoWeatherFX.lerp(Math.abs(this.windPrecipIntensity.value()), 90.0f, 180.0f - 54.0f * this.windAngleMod.value());
                this.windAngleClouds = IsoWeatherFX.lerp(Math.abs(this.windAngleIntensity.value()), 90.0f, 270.0f);
            } else {
                this.windAngle = 90.0f;
            }
            this.windSpeed = 6.0f * this.windPrecipIntensity.value();
            this.windSpeedFog = 6.0f * this.windIntensity.value() * (4.0f + 16.0f * Math.abs(this.windAngleIntensity.value()));
            if (this.windSpeed < 1.0f) {
                this.windSpeed = 1.0f;
            }
            if (this.windSpeedFog < 1.0f) {
                this.windSpeedFog = 1.0f;
            }
        }
        float zoom = Core.getInstance().getZoom(IsoPlayer.getInstance().getPlayerNum());
        float mod = 1.0f - (zoom - 0.5f) * 0.5f * 0.75f;
        zoomMod = 0.0f;
        if (Core.getInstance().isZoomEnabled() && zoom > 1.0f) {
            zoomMod = ClimateManager.clamp(0.0f, 1.0f, (zoom - 1.0f) * 0.6666667f);
        }
        if (this.cloudIntensity.value() <= 0.0f) {
            this.cloudParticles.SetParticlesStrength(0.0f);
        } else {
            this.cloudParticles.SetParticlesStrength(1.0f);
        }
        if (this.fogIntensity.value() <= 0.0f) {
            this.fogParticles.SetParticlesStrength(0.0f);
        } else {
            this.fogParticles.SetParticlesStrength(1.0f);
        }
        this.snowParticles.SetParticlesStrength(this.precipitationIntensitySnow.value() * mod);
        this.rainParticles.SetParticlesStrength(this.precipitationIntensityRain.value() * mod);
        for (int i = 0; i < this.particleRectangles.size(); ++i) {
            if (!this.particleRectangles.get(i).requiresUpdate()) continue;
            this.particleRectangles.get(i).update(delta);
        }
    }

    public void setDebugBounds(boolean b) {
        debugBounds = b;
    }

    public boolean isDebugBounds() {
        return debugBounds;
    }

    public void setWindAngleIntensity(float intensity) {
        this.windAngleIntensity.setTarget(intensity);
    }

    public float getWindAngleIntensity() {
        return this.windAngleIntensity.value();
    }

    public float getRenderWindAngleRain() {
        return this.windAngle;
    }

    public void setWindPrecipIntensity(float intensity) {
        this.windPrecipIntensity.setTarget(intensity);
    }

    public float getWindPrecipIntensity() {
        return this.windPrecipIntensity.value();
    }

    public void setWindIntensity(float intensity) {
        this.windIntensity.setTarget(intensity);
    }

    public float getWindIntensity() {
        return this.windIntensity.value();
    }

    public void setFogIntensity(float intensity) {
        if (SandboxOptions.instance.maxFogIntensity.getValue() == 2) {
            intensity = Math.min(intensity, 0.75f);
        } else if (SandboxOptions.instance.maxFogIntensity.getValue() == 3) {
            intensity = Math.min(intensity, 0.5f);
        } else if (SandboxOptions.instance.maxFogIntensity.getValue() == 4) {
            intensity = Math.min(intensity, 0.0f);
        }
        this.fogIntensity.setTarget(intensity);
    }

    public float getFogIntensity() {
        return this.fogIntensity.value();
    }

    public void setCloudIntensity(float intensity) {
        this.cloudIntensity.setTarget(intensity);
    }

    public float getCloudIntensity() {
        return this.cloudIntensity.value();
    }

    public void setPrecipitationIntensity(float intensity) {
        if (SandboxOptions.instance.maxRainFxIntensity.getValue() == 2) {
            intensity *= 0.75f;
        } else if (SandboxOptions.instance.maxRainFxIntensity.getValue() == 3) {
            intensity *= 0.5f;
        }
        if (intensity > 0.0f) {
            intensity = 0.05f + 0.95f * intensity;
        }
        this.precipitationIntensity.setTarget(intensity);
    }

    public float getPrecipitationIntensity() {
        return this.precipitationIntensity.value();
    }

    public void setPrecipitationIsSnow(boolean b) {
        this.precipitationIsSnow = b;
    }

    public boolean getPrecipitationIsSnow() {
        return this.precipitationIsSnow;
    }

    public boolean hasCloudsToRender() {
        return this.cloudIntensity.value() > 0.0f || this.particleRectangles.get(cloudId).requiresUpdate();
    }

    public boolean hasPrecipitationToRender() {
        return this.precipitationIntensity.value() > 0.0f || this.particleRectangles.get(snowId).requiresUpdate() || this.particleRectangles.get(rainId).requiresUpdate();
    }

    public boolean hasFogToRender() {
        return this.fogIntensity.value() > 0.0f || this.particleRectangles.get(fogId).requiresUpdate();
    }

    public void render() {
        if (GameServer.server) {
            return;
        }
        for (int i = 0; i < this.particleRectangles.size(); ++i) {
            if (i == fogId) {
                if (PerformanceSettings.fogQuality != 2) continue;
                this.renderFogCircle();
            }
            if ((i == rainId || i == snowId) && Core.getInstance().getOptionRenderPrecipitation() > 2 || !this.particleRectangles.get(i).requiresUpdate()) continue;
            this.particleRectangles.get(i).render();
            IsoWorld.instance.getCell().getWeatherFX().getDrawer(i).endFrame();
        }
    }

    public void renderLayered(boolean doClouds, boolean doFog, boolean doPrecip) {
        if (doClouds) {
            this.renderClouds();
        } else if (doFog) {
            this.renderFog();
        } else if (doPrecip) {
            this.renderPrecipitation();
        }
    }

    public void renderClouds() {
        if (GameServer.server) {
            return;
        }
        if (this.particleRectangles.get(cloudId).requiresUpdate()) {
            this.particleRectangles.get(cloudId).render();
            IsoWorld.instance.getCell().getWeatherFX().getDrawer(cloudId).endFrame();
        }
    }

    public void renderFog() {
        if (GameServer.server) {
            return;
        }
        this.renderFogCircle();
        if (this.particleRectangles.get(fogId).requiresUpdate()) {
            this.particleRectangles.get(fogId).render();
            IsoWorld.instance.getCell().getWeatherFX().getDrawer(fogId).endFrame();
        }
    }

    public void renderPrecipitation() {
        if (GameServer.server) {
            return;
        }
        if (this.particleRectangles.get(snowId).requiresUpdate()) {
            this.particleRectangles.get(snowId).render();
            IsoWorld.instance.getCell().getWeatherFX().getDrawer(snowId).endFrame();
        }
        if (this.particleRectangles.get(rainId).requiresUpdate()) {
            this.particleRectangles.get(rainId).render();
            IsoWorld.instance.getCell().getWeatherFX().getDrawer(rainId).endFrame();
        }
    }

    private void renderFogCircle() {
        if (this.fogOverlayAlpha <= 0.0f) {
            return;
        }
        int player = IsoCamera.frameState.playerIndex;
        float zoom = Core.getInstance().getCurrentPlayerZoom();
        int screenW = IsoCamera.getScreenWidth(player);
        int screenH = IsoCamera.getScreenHeight(player);
        int circleWidth = 2048 - (int)(512.0f * this.fogIntensity.value());
        int circleHeight = 1024 - (int)(256.0f * this.fogIntensity.value());
        circleWidth = (int)((float)circleWidth / zoom);
        circleHeight = (int)((float)circleHeight / zoom);
        int circleX = screenW / 2 - circleWidth / 2;
        int circleY = screenH / 2 - circleHeight / 2;
        circleX = (int)((float)circleX - IsoCamera.getRightClickOffX() / zoom);
        circleY = (int)((float)circleY - IsoCamera.getRightClickOffY() / zoom);
        int circleEndX = circleX + circleWidth;
        int circleEndY = circleY + circleHeight;
        SpriteRenderer.instance.glBind(this.texFogWhite.getID());
        IndieGL.glTexParameteri(3553, 10241, 9728);
        IndieGL.glTexParameteri(3553, 10240, 9728);
        if (shader == null) {
            RenderThread.invokeOnRenderContext(() -> {
                shader = ShaderManager.instance.getOrCreateShader("fogCircle", false, false);
            });
        }
        if (shader.getShaderProgram().isCompiled()) {
            IndieGL.StartShader(shader.getID(), player);
            int stateIndex = SpriteRenderer.instance.getMainStateIndex();
            if (s_drawer[player][stateIndex] == null) {
                IsoWeatherFX.s_drawer[player][stateIndex] = new Drawer();
            }
            s_drawer[player][stateIndex].init(screenW, screenH);
        }
        IndieGL.disableDepthTest();
        SpriteRenderer.instance.renderi(this.texFogCircle, circleX, circleY, circleWidth, circleHeight, this.fogColor.r, this.fogColor.g, this.fogColor.b, this.fogOverlayAlpha, null);
        SpriteRenderer.instance.renderi(this.texFogWhite, 0, 0, circleX, screenH, this.fogColor.r, this.fogColor.g, this.fogColor.b, this.fogOverlayAlpha, null);
        SpriteRenderer.instance.renderi(this.texFogWhite, circleX, 0, circleWidth, circleY, this.fogColor.r, this.fogColor.g, this.fogColor.b, this.fogOverlayAlpha, null);
        SpriteRenderer.instance.renderi(this.texFogWhite, circleEndX, 0, screenW - circleEndX, screenH, this.fogColor.r, this.fogColor.g, this.fogColor.b, this.fogOverlayAlpha, null);
        SpriteRenderer.instance.renderi(this.texFogWhite, circleX, circleEndY, circleWidth, screenH - circleEndY, this.fogColor.r, this.fogColor.g, this.fogColor.b, this.fogOverlayAlpha, null);
        if (shader.getShaderProgram().isCompiled()) {
            IndieGL.EndShader();
        }
        if (Core.getInstance().getOffscreenBuffer() != null) {
            if (Core.getInstance().isZoomEnabled() && Core.getInstance().getZoom(player) > 0.5f) {
                IndieGL.glTexParameteri(3553, 10241, 9729);
            } else {
                IndieGL.glTexParameteri(3553, 10241, 9728);
            }
            if (Core.getInstance().getZoom(player) == 0.5f) {
                IndieGL.glTexParameteri(3553, 10240, 9728);
            } else {
                IndieGL.glTexParameteri(3553, 10240, 9729);
            }
        }
    }

    public static float clamp(float min, float max, float val) {
        val = Math.min(max, val);
        val = Math.max(min, val);
        return val;
    }

    public static float lerp(float t, float a, float b) {
        return a + t * (b - a);
    }

    public static float clerp(float t, float a, float b) {
        float t2 = (float)(1.0 - Math.cos((double)t * Math.PI)) / 2.0f;
        return a * (1.0f - t2) + b * t2;
    }

    public WeatherParticleDrawer getDrawer(int id) {
        int playerIndex = IsoCamera.frameState.playerIndex;
        int stateIndex = SpriteRenderer.instance.getMainStateIndex();
        if (this.drawers[playerIndex][stateIndex][id] == null) {
            this.drawers[playerIndex][stateIndex][id] = new WeatherParticleDrawer();
        }
        return this.drawers[playerIndex][stateIndex][id];
    }

    public void Reset() {
        this.cloudParticles.Reset();
        this.fogParticles.Reset();
        this.snowParticles.Reset();
        this.rainParticles.Reset();
        this.cloudParticles = null;
        this.fogParticles = null;
        this.snowParticles = null;
        this.rainParticles = null;
        for (int p = 0; p < 4; ++p) {
            for (int s = 0; s < 3; ++s) {
                for (int id = 0; id < this.drawers[p][s].length; ++id) {
                    if (this.drawers[p][s][id] == null) continue;
                    this.drawers[p][s][id].Reset();
                    this.drawers[p][s][id] = null;
                }
            }
        }
    }

    static {
        fogId = 1;
        snowId = 2;
        rainId = 3;
        zoomMod = 1.0f;
        s_drawer = new Drawer[4][3];
    }

    private static final class Drawer
    extends TextureDraw.GenericDrawer {
        static final org.joml.Matrix4f s_matrix4f = new org.joml.Matrix4f();
        final Matrix4f mvp = new Matrix4f();
        int width;
        int height;
        boolean set;

        private Drawer() {
        }

        void init(int w, int h) {
            if (w == this.width && h == this.height && this.set) {
                return;
            }
            this.width = w;
            this.height = h;
            this.set = false;
            s_matrix4f.setOrtho(0.0f, this.width, this.height, 0.0f, -1.0f, 1.0f);
            PZMath.convertMatrix(s_matrix4f, this.mvp);
            this.mvp.transpose();
            SpriteRenderer.instance.drawGeneric(this);
        }

        @Override
        public void render() {
            shader.getShaderProgram().setValue("u_mvp", this.mvp);
            this.set = true;
        }
    }
}

