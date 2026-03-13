/*
 * Decompiled with CFR 0.152.
 */
package zombie.iso.weather;

import fmod.fmod.FMODManager;
import fmod.javafmod;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Objects;
import zombie.GameSounds;
import zombie.GameTime;
import zombie.Lua.LuaEventManager;
import zombie.UsedFromLua;
import zombie.audio.GameSound;
import zombie.audio.GameSoundClip;
import zombie.characters.IsoPlayer;
import zombie.core.Core;
import zombie.core.SceneShaderStore;
import zombie.core.math.PZMath;
import zombie.core.network.ByteBufferReader;
import zombie.core.network.ByteBufferWriter;
import zombie.core.opengl.RenderSettings;
import zombie.core.random.Rand;
import zombie.debug.DebugLog;
import zombie.iso.weather.ClimateColorInfo;
import zombie.iso.weather.ClimateManager;
import zombie.network.GameClient;
import zombie.network.GameServer;
import zombie.ui.SpeedControls;
import zombie.ui.UIManager;

@UsedFromLua
public class ThunderStorm {
    public static int mapMinX = -3000;
    public static int mapMinY = -3000;
    public static int mapMaxX = 25000;
    public static int mapMaxY = 20000;
    private boolean hasActiveThunderClouds;
    private final float cloudMaxRadius = 20000.0f;
    private final ThunderEvent[] events = new ThunderEvent[30];
    private final ThunderCloud[] clouds = new ThunderCloud[3];
    private final ClimateManager climateManager;
    private ArrayList<ThunderCloud> cloudCache;
    private final boolean donoise = false;
    private int strikeRadius = 4000;
    private final PlayerLightningInfo[] lightningInfos = new PlayerLightningInfo[4];
    private final ThunderEvent networkThunderEvent = new ThunderEvent();
    private ThunderCloud dummyCloud;

    public ArrayList<ThunderCloud> getClouds() {
        if (this.cloudCache == null) {
            this.cloudCache = new ArrayList(this.clouds.length);
            for (int i = 0; i < this.clouds.length; ++i) {
                this.cloudCache.add(this.clouds[i]);
            }
        }
        return this.cloudCache;
    }

    public ThunderStorm(ClimateManager climmgr) {
        int i;
        this.climateManager = climmgr;
        for (i = 0; i < this.events.length; ++i) {
            this.events[i] = new ThunderEvent();
        }
        for (i = 0; i < this.clouds.length; ++i) {
            this.clouds[i] = new ThunderCloud();
        }
        for (i = 0; i < 4; ++i) {
            this.lightningInfos[i] = new PlayerLightningInfo(this);
        }
    }

    private ThunderEvent getFreeEvent() {
        for (int i = 0; i < this.events.length; ++i) {
            if (this.events[i].isRunning) continue;
            return this.events[i];
        }
        return null;
    }

    private ThunderCloud getFreeCloud() {
        for (int i = 0; i < this.clouds.length; ++i) {
            if (this.clouds[i].isRunning) continue;
            return this.clouds[i];
        }
        return null;
    }

    private ThunderCloud getCloud(int id) {
        if (id >= 0 && id < this.clouds.length) {
            return this.clouds[id];
        }
        return null;
    }

    public boolean HasActiveThunderClouds() {
        return this.hasActiveThunderClouds;
    }

    public void noise(String s) {
    }

    public void stopAllClouds() {
        for (int i = 0; i < this.clouds.length; ++i) {
            this.stopCloud(i);
        }
    }

    public void stopCloud(int id) {
        ThunderCloud thunderCloud = this.getCloud(id);
        if (thunderCloud != null) {
            thunderCloud.isRunning = false;
        }
    }

    private static float addToAngle(float angle, float addition) {
        if ((angle += addition) > 360.0f) {
            angle -= 360.0f;
        } else if (angle < 0.0f) {
            angle += 360.0f;
        }
        return angle;
    }

    public static int getMapDiagonal() {
        int width = mapMaxX - mapMinX;
        int height = mapMaxY - mapMinY;
        int diag = (int)Math.sqrt(Math.pow(width, 2.0) + Math.pow(height, 2.0));
        return diag /= 2;
    }

    public void startThunderCloud(float str, float angle, float radius, float eventFreq, float thunderRatio, double duration, boolean targetRandomPlayer) {
        this.startThunderCloud(str, angle, radius, eventFreq, thunderRatio, duration, targetRandomPlayer);
    }

    public ThunderCloud startThunderCloud(float str, float angle, float radius, float eventFreq, float thunderRatio, double duration, boolean targetRandomPlayer, float percentageOffset) {
        if (GameClient.client) {
            return null;
        }
        ThunderCloud thunderCloud = this.getFreeCloud();
        if (thunderCloud != null) {
            angle = ThunderStorm.addToAngle(angle, Rand.Next(-10.0f, 10.0f));
            thunderCloud.startTime = GameTime.instance.getWorldAgeHours();
            thunderCloud.endTime = thunderCloud.startTime + duration;
            thunderCloud.duration = duration;
            thunderCloud.strength = ClimateManager.clamp01(str);
            thunderCloud.angle = angle;
            thunderCloud.radius = radius;
            if (thunderCloud.radius > 20000.0f) {
                thunderCloud.radius = 20000.0f;
            }
            thunderCloud.eventFrequency = eventFreq;
            thunderCloud.thunderRatio = ClimateManager.clamp01(thunderRatio);
            thunderCloud.percentageOffset = PZMath.clamp_01(percentageOffset);
            float angleOpposing = ThunderStorm.addToAngle(angle, 180.0f);
            int width = mapMaxX - mapMinX;
            int height = mapMaxY - mapMinY;
            int centerX = Rand.Next(mapMinX + width / 5, mapMaxX - width / 5);
            int centerY = Rand.Next(mapMinY + height / 5, mapMaxY - height / 5);
            if (targetRandomPlayer) {
                if (!GameServer.server) {
                    IsoPlayer player = IsoPlayer.getInstance();
                    if (player != null) {
                        centerX = (int)player.getX();
                        centerY = (int)player.getY();
                    }
                } else if (!GameServer.Players.isEmpty()) {
                    ArrayList<IsoPlayer> players = GameServer.getPlayers();
                    for (int i = players.size() - 1; i >= 0; --i) {
                        if (players.get(i).getCurrentSquare() != null) continue;
                        players.remove(i);
                    }
                    if (!players.isEmpty()) {
                        IsoPlayer randomPlayer = players.get(Rand.Next(players.size()));
                        centerX = randomPlayer.getCurrentSquare().getX();
                        centerY = randomPlayer.getCurrentSquare().getY();
                    }
                } else {
                    DebugLog.log("Thundercloud couldnt target player...");
                    return null;
                }
            }
            thunderCloud.setCenter(centerX, centerY, angle);
            thunderCloud.isRunning = true;
            thunderCloud.suspendTimer.init(3);
            return thunderCloud;
        }
        return null;
    }

    public void update(double currentTime) {
        int i;
        if (!GameClient.client || GameServer.server) {
            this.hasActiveThunderClouds = false;
            for (i = 0; i < this.clouds.length; ++i) {
                ThunderCloud cloud = this.clouds[i];
                if (!cloud.isRunning) continue;
                if (currentTime < cloud.endTime) {
                    float t = (float)((currentTime - cloud.startTime) / cloud.duration);
                    if (cloud.percentageOffset > 0.0f) {
                        t = cloud.percentageOffset + (1.0f - cloud.percentageOffset) * t;
                    }
                    cloud.currentX = (int)ClimateManager.lerp(t, cloud.startX, cloud.endX);
                    cloud.currentY = (int)ClimateManager.lerp(t, cloud.startY, cloud.endY);
                    cloud.suspendTimer.update();
                    this.hasActiveThunderClouds = true;
                    if (!cloud.suspendTimer.finished()) continue;
                    float suspendNext = Rand.Next(3.5f - 3.0f * cloud.strength, 24.0f - 20.0f * cloud.strength);
                    cloud.suspendTimer.init((int)(suspendNext * 60.0f));
                    float r = Rand.Next(0.0f, 1.0f);
                    this.strikeRadius = r < 0.6f ? (int)(cloud.radius / 2.0f) / 3 : (r < 0.9f ? (int)(cloud.radius / 2.0f) / 4 * 3 : (int)(cloud.radius / 2.0f));
                    if (Rand.Next(0.0f, 1.0f) < cloud.thunderRatio) {
                        this.noise("trigger thunder event");
                        this.triggerThunderEvent(Rand.Next(cloud.currentX - this.strikeRadius, cloud.currentX + this.strikeRadius), Rand.Next(cloud.currentY - this.strikeRadius, cloud.currentY + this.strikeRadius), true, !Core.getInstance().getOptionLightSensitivity(), Rand.Next(0.0f, 1.0f) > 0.4f);
                        continue;
                    }
                    this.triggerThunderEvent(Rand.Next(cloud.currentX - this.strikeRadius, cloud.currentX + this.strikeRadius), Rand.Next(cloud.currentY - this.strikeRadius, cloud.currentY + this.strikeRadius), false, false, true);
                    this.noise("trigger rumble event");
                    continue;
                }
                cloud.isRunning = false;
            }
        }
        if (GameClient.client || !GameServer.server) {
            for (i = 0; i < 4; ++i) {
                PlayerLightningInfo linfo = this.lightningInfos[i];
                if (linfo.lightningState != LightningState.ApplyLightning) continue;
                linfo.timer.update();
                if (!linfo.timer.finished()) {
                    linfo.lightningMod = ClimateManager.clamp01(linfo.timer.ratio());
                    this.climateManager.dayLightStrength.finalValue += (1.0f - this.climateManager.dayLightStrength.finalValue) * (1.0f - linfo.lightningMod);
                    IsoPlayer player = IsoPlayer.players[i];
                    if (player == null) continue;
                    player.dirtyRecalcGridStackTime = 1.0f;
                    continue;
                }
                this.noise("apply lightning done.");
                linfo.timer.init(2);
                linfo.lightningStrength = 0.0f;
                linfo.lightningState = LightningState.Idle;
            }
            boolean bFastForward = SpeedControls.instance.getCurrentGameSpeed() > 1;
            boolean bStrike = false;
            boolean bRumble = false;
            for (int i2 = 0; i2 < this.events.length; ++i2) {
                ThunderEvent event = this.events[i2];
                if (!event.isRunning) continue;
                event.soundDelay.update();
                if (event.soundDelay.finished()) {
                    long inst;
                    GameSoundClip clip;
                    GameSound gameSound;
                    event.isRunning = false;
                    boolean playSound = true;
                    if (UIManager.getSpeedControls() != null && UIManager.getSpeedControls().getCurrentGameSpeed() > 1) {
                        playSound = false;
                    }
                    if (!playSound || Core.soundDisabled || FMODManager.instance.getNumListeners() <= 0) continue;
                    if (!(!event.doStrike || bFastForward && bStrike)) {
                        this.noise("thunder sound");
                        gameSound = GameSounds.getSound("Thunder");
                        GameSoundClip gameSoundClip = clip = gameSound == null ? null : gameSound.getRandomClip();
                        if (clip != null && clip.eventDescription != null) {
                            long thunderEvent = clip.eventDescription.address;
                            inst = javafmod.FMOD_Studio_System_CreateEventInstance(thunderEvent);
                            javafmod.FMOD_Studio_EventInstance3D(inst, event.eventX, event.eventY, 100.0f);
                            javafmod.FMOD_Studio_EventInstance_SetVolume(inst, clip.getEffectiveVolume());
                            javafmod.FMOD_Studio_StartEvent(inst);
                            javafmod.FMOD_Studio_ReleaseEventInstance(inst);
                        }
                    }
                    if (!event.doRumble || bFastForward && bRumble) continue;
                    this.noise("rumble sound");
                    gameSound = GameSounds.getSound("RumbleThunder");
                    GameSoundClip gameSoundClip = clip = gameSound == null ? null : gameSound.getRandomClip();
                    if (clip == null || clip.eventDescription == null) continue;
                    long rumbleEvent = clip.eventDescription.address;
                    inst = javafmod.FMOD_Studio_System_CreateEventInstance(rumbleEvent);
                    javafmod.FMOD_Studio_EventInstance3D(inst, event.eventX, event.eventY, 200.0f);
                    javafmod.FMOD_Studio_EventInstance_SetVolume(inst, clip.getEffectiveVolume());
                    javafmod.FMOD_Studio_StartEvent(inst);
                    javafmod.FMOD_Studio_ReleaseEventInstance(inst);
                    continue;
                }
                bStrike = bStrike || event.doStrike;
                bRumble = bRumble || event.doRumble;
            }
        }
    }

    public void applyLightningForPlayer(RenderSettings.PlayerRenderSettings renderSettings, int plrIndex, IsoPlayer player) {
        PlayerLightningInfo linfo = this.lightningInfos[plrIndex];
        if (linfo.lightningState == LightningState.ApplyLightning) {
            ClimateColorInfo gl = renderSettings.cmGlobalLight;
            linfo.lightningColor.getExterior().r = gl.getExterior().r + linfo.lightningStrength * (1.0f - gl.getExterior().r);
            linfo.lightningColor.getExterior().g = gl.getExterior().g + linfo.lightningStrength * (1.0f - gl.getExterior().g);
            linfo.lightningColor.getExterior().b = gl.getExterior().b + linfo.lightningStrength * (1.0f - gl.getExterior().b);
            linfo.lightningColor.getInterior().r = gl.getInterior().r + linfo.lightningStrength * (1.0f - gl.getInterior().r);
            linfo.lightningColor.getInterior().g = gl.getInterior().g + linfo.lightningStrength * (1.0f - gl.getInterior().g);
            linfo.lightningColor.getInterior().b = gl.getInterior().b + linfo.lightningStrength * (1.0f - gl.getInterior().b);
            linfo.lightningColor.interp(renderSettings.cmGlobalLight, linfo.lightningMod, linfo.outColor);
            renderSettings.cmGlobalLight.getExterior().r = linfo.outColor.getExterior().r;
            renderSettings.cmGlobalLight.getExterior().g = linfo.outColor.getExterior().g;
            renderSettings.cmGlobalLight.getExterior().b = linfo.outColor.getExterior().b;
            renderSettings.cmGlobalLight.getInterior().r = linfo.outColor.getInterior().r;
            renderSettings.cmGlobalLight.getInterior().g = linfo.outColor.getInterior().g;
            renderSettings.cmGlobalLight.getInterior().b = linfo.outColor.getInterior().b;
            renderSettings.cmAmbient = ClimateManager.lerp(linfo.lightningMod, 1.0f, renderSettings.cmAmbient);
            renderSettings.cmDayLightStrength = ClimateManager.lerp(linfo.lightningMod, 1.0f, renderSettings.cmDayLightStrength);
            renderSettings.cmDesaturation = ClimateManager.lerp(linfo.lightningMod, 0.0f, renderSettings.cmDesaturation);
            renderSettings.cmGlobalLightIntensity = SceneShaderStore.weatherShader != null && Core.getInstance().getOffscreenBuffer() != null ? ClimateManager.lerp(linfo.lightningMod, 1.0f, renderSettings.cmGlobalLightIntensity) : ClimateManager.lerp(linfo.lightningMod, 0.0f, renderSettings.cmGlobalLightIntensity);
        }
    }

    public boolean isModifyingNight() {
        return false;
    }

    public void triggerThunderEvent(int x, int y, boolean doStrike, boolean doLightning, boolean doRumble) {
        if (GameServer.server) {
            this.networkThunderEvent.eventX = x;
            this.networkThunderEvent.eventY = y;
            this.networkThunderEvent.doStrike = doStrike;
            this.networkThunderEvent.doLightning = doLightning;
            this.networkThunderEvent.doRumble = doRumble;
            this.climateManager.transmitClimatePacket(ClimateManager.ClimateNetAuth.ServerOnly, (byte)2, null);
        } else if (!GameClient.client) {
            this.enqueueThunderEvent(x, y, doStrike, doLightning, doRumble);
        }
    }

    public void writeNetThunderEvent(ByteBufferWriter output) throws IOException {
        output.putInt(this.networkThunderEvent.eventX);
        output.putInt(this.networkThunderEvent.eventY);
        output.putBoolean(this.networkThunderEvent.doStrike);
        output.putBoolean(this.networkThunderEvent.doLightning);
        output.putBoolean(this.networkThunderEvent.doRumble);
    }

    public void readNetThunderEvent(ByteBufferReader input) throws IOException {
        int x = input.getInt();
        int y = input.getInt();
        boolean doStrike = input.getBoolean();
        boolean doLightning = input.getBoolean();
        boolean doRumble = input.getBoolean();
        this.enqueueThunderEvent(x, y, doStrike, doLightning, doRumble);
    }

    public void enqueueThunderEvent(int x, int y, boolean doStrike, boolean doLightning, boolean doRumble) {
        LuaEventManager.triggerEvent("OnThunderEvent", x, y, doStrike, doLightning, doRumble);
        if (doStrike || doRumble) {
            ThunderEvent event;
            int dist = 9999999;
            for (int i = 0; i < IsoPlayer.numPlayers; ++i) {
                IsoPlayer player = IsoPlayer.players[i];
                if (player == null) continue;
                int pdist = this.GetDistance((int)player.getX(), (int)player.getY(), x, y);
                if (pdist < dist) {
                    dist = pdist;
                }
                if (!doLightning) continue;
                this.lightningInfos[i].distance = pdist;
                this.lightningInfos[i].x = x;
                this.lightningInfos[i].y = y;
            }
            this.noise("dist to player = " + dist);
            if (dist < 10000 && (event = this.getFreeEvent()) != null) {
                event.doRumble = doRumble;
                event.doStrike = doStrike;
                event.eventX = x;
                event.eventY = y;
                event.isRunning = true;
                event.soundDelay.init((int)((float)dist / 300.0f * 60.0f));
                if (doLightning) {
                    for (int i = 0; i < IsoPlayer.numPlayers; ++i) {
                        IsoPlayer player = IsoPlayer.players[i];
                        if (player == null || !((float)this.lightningInfos[i].distance < 7500.0f)) continue;
                        float ls = 1.0f - (float)this.lightningInfos[i].distance / 7500.0f;
                        this.lightningInfos[i].lightningState = LightningState.ApplyLightning;
                        if (!(ls > this.lightningInfos[i].lightningStrength)) continue;
                        this.lightningInfos[i].lightningStrength = ls;
                        this.lightningInfos[i].timer.init(20 + (int)(80.0f * this.lightningInfos[i].lightningStrength));
                    }
                }
            }
        }
    }

    private int GetDistance(int dx, int dy, int sx, int sy) {
        return (int)Math.sqrt(Math.pow(dx - sx, 2.0) + Math.pow(dy - sy, 2.0));
    }

    public void save(DataOutputStream output) throws IOException {
        if (!GameClient.client || GameServer.server) {
            output.writeByte(this.clouds.length);
            for (int i = 0; i < this.clouds.length; ++i) {
                ThunderCloud cloud = this.clouds[i];
                output.writeBoolean(cloud.isRunning);
                if (!cloud.isRunning) continue;
                output.writeInt(cloud.startX);
                output.writeInt(cloud.startY);
                output.writeInt(cloud.endX);
                output.writeInt(cloud.endY);
                output.writeFloat(cloud.radius);
                output.writeFloat(cloud.angle);
                output.writeFloat(cloud.strength);
                output.writeFloat(cloud.thunderRatio);
                output.writeDouble(cloud.startTime);
                output.writeDouble(cloud.endTime);
                output.writeDouble(cloud.duration);
                output.writeFloat(cloud.percentageOffset);
            }
        } else {
            output.writeByte(0);
        }
    }

    public void load(DataInputStream input) throws IOException {
        int len = input.readByte();
        if (len == 0) {
            return;
        }
        if (len > this.clouds.length && this.dummyCloud == null) {
            this.dummyCloud = new ThunderCloud();
        }
        for (int i = 0; i < len; ++i) {
            boolean isrunnin = input.readBoolean();
            ThunderCloud cloud = i >= this.clouds.length ? this.dummyCloud : this.clouds[i];
            cloud.isRunning = isrunnin;
            if (!isrunnin) continue;
            cloud.startX = input.readInt();
            cloud.startY = input.readInt();
            cloud.endX = input.readInt();
            cloud.endY = input.readInt();
            cloud.radius = input.readFloat();
            cloud.angle = input.readFloat();
            cloud.strength = input.readFloat();
            cloud.thunderRatio = input.readFloat();
            cloud.startTime = input.readDouble();
            cloud.endTime = input.readDouble();
            cloud.duration = input.readDouble();
            cloud.percentageOffset = input.readFloat();
        }
    }

    @UsedFromLua
    public static class ThunderCloud {
        private int currentX;
        private int currentY;
        private int startX;
        private int startY;
        private int endX;
        private int endY;
        private double startTime;
        private double endTime;
        private double duration;
        private float strength;
        private float angle;
        private float radius;
        private float eventFrequency;
        private float thunderRatio;
        private float percentageOffset;
        private boolean isRunning;
        private final GameTime.AnimTimer suspendTimer = new GameTime.AnimTimer();

        public int getCurrentX() {
            return this.currentX;
        }

        public int getCurrentY() {
            return this.currentY;
        }

        public float getRadius() {
            return this.radius;
        }

        public boolean isRunning() {
            return this.isRunning;
        }

        public float getStrength() {
            return this.strength;
        }

        public double lifeTime() {
            return (this.startTime - this.endTime) / this.duration;
        }

        public void setCenter(int centerX, int centerY, float angle) {
            int diag = ThunderStorm.getMapDiagonal();
            float angleOpposing = ThunderStorm.addToAngle(angle, 180.0f);
            int randDist = diag + Rand.Next(1500, 7500);
            int sx = (int)((double)centerX + (double)randDist * Math.cos(Math.toRadians(angleOpposing)));
            int sy = (int)((double)centerY + (double)randDist * Math.sin(Math.toRadians(angleOpposing)));
            randDist = diag + Rand.Next(1500, 7500);
            int ex = (int)((double)centerX + (double)randDist * Math.cos(Math.toRadians(angle)));
            int ey = (int)((double)centerY + (double)randDist * Math.sin(Math.toRadians(angle)));
            this.startX = sx;
            this.startY = sy;
            this.endX = ex;
            this.endY = ey;
            this.currentX = sx;
            this.currentY = sy;
        }
    }

    private static class ThunderEvent {
        private int eventX;
        private int eventY;
        private boolean doLightning;
        private boolean doRumble;
        private boolean doStrike;
        private final GameTime.AnimTimer soundDelay = new GameTime.AnimTimer();
        private boolean isRunning;

        private ThunderEvent() {
        }
    }

    private class PlayerLightningInfo {
        public LightningState lightningState;
        public GameTime.AnimTimer timer;
        public float lightningStrength;
        public float lightningMod;
        public ClimateColorInfo lightningColor;
        public ClimateColorInfo outColor;
        public int x;
        public int y;
        public int distance;

        private PlayerLightningInfo(ThunderStorm thunderStorm) {
            Objects.requireNonNull(thunderStorm);
            this.lightningState = LightningState.Idle;
            this.timer = new GameTime.AnimTimer();
            this.lightningStrength = 1.0f;
            this.lightningColor = new ClimateColorInfo(1.0f, 1.0f, 1.0f, 1.0f);
            this.outColor = new ClimateColorInfo(1.0f, 1.0f, 1.0f, 1.0f);
        }
    }

    private static enum LightningState {
        Idle,
        ApplyLightning;

    }
}

