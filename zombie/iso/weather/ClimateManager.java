/*
 * Decompiled with CFR 0.152.
 */
package zombie.iso.weather;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.GregorianCalendar;
import se.krka.kahlua.vm.KahluaTable;
import zombie.GameTime;
import zombie.Lua.LuaEventManager;
import zombie.Lua.LuaManager;
import zombie.SandboxOptions;
import zombie.UsedFromLua;
import zombie.characters.IsoGameCharacter;
import zombie.characters.IsoPlayer;
import zombie.core.Color;
import zombie.core.Core;
import zombie.core.PerformanceSettings;
import zombie.core.SceneShaderStore;
import zombie.core.math.PZMath;
import zombie.core.network.ByteBufferReader;
import zombie.core.network.ByteBufferWriter;
import zombie.core.raknet.UdpConnection;
import zombie.core.random.Rand;
import zombie.debug.DebugLog;
import zombie.debug.DebugType;
import zombie.debug.LogSeverity;
import zombie.erosion.ErosionMain;
import zombie.erosion.season.ErosionIceQueen;
import zombie.erosion.season.ErosionSeason;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoMetaGrid;
import zombie.iso.IsoPuddles;
import zombie.iso.IsoWater;
import zombie.iso.IsoWorld;
import zombie.iso.sprite.SkyBox;
import zombie.iso.weather.ClimateColorInfo;
import zombie.iso.weather.ClimateForecaster;
import zombie.iso.weather.ClimateHistory;
import zombie.iso.weather.ClimateMoon;
import zombie.iso.weather.ClimateValues;
import zombie.iso.weather.SimplexNoise;
import zombie.iso.weather.Temperature;
import zombie.iso.weather.ThunderStorm;
import zombie.iso.weather.WeatherPeriod;
import zombie.iso.weather.WorldFlares;
import zombie.iso.weather.dbg.ClimMngrDebug;
import zombie.iso.weather.fx.IsoWeatherFX;
import zombie.iso.weather.fx.SteppedUpdateFloat;
import zombie.network.GameClient;
import zombie.network.GameServer;
import zombie.network.IConnection;
import zombie.network.PacketTypes;
import zombie.vehicles.BaseVehicle;

@UsedFromLua
public class ClimateManager {
    private boolean disableSimulation;
    private boolean disableFxUpdate;
    private boolean disableWeatherGeneration;
    public static final int FRONT_COLD = -1;
    public static final int FRONT_STATIONARY = 0;
    public static final int FRONT_WARM = 1;
    public static final float MAX_WINDSPEED_KPH = 120.0f;
    public static final float MAX_WINDSPEED_MPH = 74.5645f;
    private ErosionSeason season;
    private long lastMinuteStamp = -1L;
    private KahluaTable modDataTable;
    private float airMass;
    private float airMassDaily;
    private float airMassTemperature;
    private float baseTemperature;
    private float snowFall;
    private float snowStrength;
    private float snowMeltStrength;
    private float snowFracNow;
    boolean canDoWinterSprites;
    boolean wasForceSnow;
    private float windPower;
    private final WeatherPeriod weatherPeriod;
    private final ThunderStorm thunderStorm;
    private double simplexOffsetA;
    private double simplexOffsetB;
    private double simplexOffsetC;
    private double simplexOffsetD;
    private boolean dayDoFog;
    private float dayFogStrength;
    private GameTime gt;
    private double worldAgeHours;
    private boolean tickIsClimateTick;
    private boolean tickIsDayChange;
    private int lastHourStamp = -1;
    private boolean tickIsHourChange;
    private boolean tickIsTenMins;
    private final AirFront currentFront = new AirFront();
    private ClimateColorInfo colDay;
    private ClimateColorInfo colDusk;
    private ClimateColorInfo colDawn;
    private ClimateColorInfo colNight;
    private final ClimateColorInfo colNightNoMoon;
    private ClimateColorInfo colNightMoon;
    private ClimateColorInfo colTemp;
    private ClimateColorInfo colFog;
    private final ClimateColorInfo colFogLegacy;
    private final ClimateColorInfo colFogNew;
    private final ClimateColorInfo fogTintStorm;
    private final ClimateColorInfo fogTintTropical;
    private static ClimateManager instance = new ClimateManager();
    public static boolean winterIsComing;
    public static boolean theDescendingFog;
    public static boolean aStormIsComing;
    private ClimateValues climateValues;
    private final ClimateForecaster climateForecaster;
    private final ClimateHistory climateHistory;
    float dayLightLagged;
    float nightLagged;
    protected ClimateFloat desaturation;
    protected ClimateFloat globalLightIntensity;
    protected ClimateFloat nightStrength;
    protected ClimateFloat precipitationIntensity;
    protected ClimateFloat temperature;
    protected ClimateFloat fogIntensity;
    protected ClimateFloat windIntensity;
    protected ClimateFloat windAngleIntensity;
    protected ClimateFloat cloudIntensity;
    protected ClimateFloat ambient;
    protected ClimateFloat viewDistance;
    protected ClimateFloat dayLightStrength;
    protected ClimateFloat humidity;
    protected ClimateColor globalLight;
    protected ClimateColor colorNewFog;
    protected ClimateBool precipitationIsSnow;
    public static final int FLOAT_DESATURATION = 0;
    public static final int FLOAT_GLOBAL_LIGHT_INTENSITY = 1;
    public static final int FLOAT_NIGHT_STRENGTH = 2;
    public static final int FLOAT_PRECIPITATION_INTENSITY = 3;
    public static final int FLOAT_TEMPERATURE = 4;
    public static final int FLOAT_FOG_INTENSITY = 5;
    public static final int FLOAT_WIND_INTENSITY = 6;
    public static final int FLOAT_WIND_ANGLE_INTENSITY = 7;
    public static final int FLOAT_CLOUD_INTENSITY = 8;
    public static final int FLOAT_AMBIENT = 9;
    public static final int FLOAT_VIEW_DISTANCE = 10;
    public static final int FLOAT_DAYLIGHT_STRENGTH = 11;
    public static final int FLOAT_HUMIDITY = 12;
    public static final int FLOAT_MAX = 13;
    private final ClimateFloat[] climateFloats = new ClimateFloat[13];
    public static final int COLOR_GLOBAL_LIGHT = 0;
    public static final int COLOR_NEW_FOG = 1;
    public static final int COLOR_MAX = 2;
    private final ClimateColor[] climateColors = new ClimateColor[2];
    public static final int BOOL_IS_SNOW = 0;
    public static final int BOOL_MAX = 1;
    private final ClimateBool[] climateBooleans = new ClimateBool[1];
    public static final float AVG_FAV_AIR_TEMPERATURE = 22.0f;
    private int weatherOverride;
    private int fogOverride;
    private static double windNoiseOffset;
    private static double windNoiseBase;
    private static double windNoiseFinal;
    private static double windTickFinal;
    private final ClimateColorInfo colFlare = new ClimateColorInfo(1.0f, 0.0f, 0.0f, 1.0f, 1.0f, 0.0f, 0.0f, 1.0f);
    private boolean flareLaunched;
    private final SteppedUpdateFloat flareIntensity = new SteppedUpdateFloat(0.0f, 0.01f, 0.0f, 1.0f);
    private float flareIntens;
    private float flareMaxLifeTime;
    private float flareLifeTime;
    private int nextRandomTargetIntens = 10;
    float fogLerpValue;
    private SeasonColor seasonColorDawn;
    private SeasonColor seasonColorDay;
    private SeasonColor seasonColorDusk;
    private DayInfo previousDay;
    private DayInfo currentDay;
    private DayInfo nextDay;
    public static final byte PacketUpdateClimateVars = 0;
    public static final byte PacketWeatherUpdate = 1;
    public static final byte PacketThunderEvent = 2;
    public static final byte PacketFlare = 3;
    public static final byte PacketAdminVarsUpdate = 4;
    public static final byte PacketRequestAdminVars = 5;
    public static final byte PacketClientChangedAdminVars = 6;
    public static final byte PacketClientChangedWeather = 7;
    private float networkLerp;
    private long networkUpdateStamp;
    private float networkLerpTime = 5000.0f;
    private final float networkLerpTimeBase = 5000.0f;
    private float networkAdjustVal;
    private final boolean networkPrint = false;
    private final ClimateNetInfo netInfo = new ClimateNetInfo();
    private ClimateValues climateValuesFronts;
    private static final float[] windAngles;
    private static final String[] windAngleStr;

    public float getMaxWindspeedKph() {
        return 120.0f;
    }

    public float getMaxWindspeedMph() {
        return 74.5645f;
    }

    public static float ToKph(float val) {
        return val * 120.0f;
    }

    public static float ToMph(float val) {
        return val * 74.5645f;
    }

    public static ClimateManager getInstance() {
        return instance;
    }

    public static void setInstance(ClimateManager inst) {
        instance = inst;
    }

    public ClimateManager() {
        this.colDay = new ClimateColorInfo();
        this.colDawn = new ClimateColorInfo();
        this.colDusk = new ClimateColorInfo();
        this.colNight = new ClimateColorInfo();
        this.colNightMoon = new ClimateColorInfo();
        this.colFog = new ClimateColorInfo();
        this.colTemp = new ClimateColorInfo();
        this.colDay = new ClimateColorInfo();
        this.colDawn = new ClimateColorInfo();
        this.colDusk = new ClimateColorInfo();
        this.colNight = new ClimateColorInfo(0.33f, 0.33f, 0.33f, 0.4f, 0.33f, 0.33f, 0.33f, 0.4f);
        this.colNightNoMoon = new ClimateColorInfo(0.33f, 0.33f, 0.33f, 0.4f, 0.33f, 0.33f, 0.33f, 0.4f);
        this.colNightMoon = new ClimateColorInfo(0.33f, 0.33f, 0.33f, 0.4f, 0.33f, 0.33f, 0.33f, 0.4f);
        this.colFog = new ClimateColorInfo(0.4f, 0.4f, 0.4f, 0.8f, 0.4f, 0.4f, 0.4f, 0.8f);
        this.colFogLegacy = new ClimateColorInfo(0.3f, 0.3f, 0.3f, 0.8f, 0.3f, 0.3f, 0.3f, 0.8f);
        this.colFogNew = new ClimateColorInfo(0.5f, 0.5f, 0.55f, 0.4f, 0.5f, 0.5f, 0.55f, 0.8f);
        this.fogTintStorm = new ClimateColorInfo(0.5f, 0.45f, 0.4f, 1.0f, 0.5f, 0.45f, 0.4f, 1.0f);
        this.fogTintTropical = new ClimateColorInfo(0.8f, 0.75f, 0.55f, 1.0f, 0.8f, 0.75f, 0.55f, 1.0f);
        this.colTemp = new ClimateColorInfo();
        this.simplexOffsetA = Rand.Next(0, 8000);
        this.simplexOffsetB = Rand.Next(8000, 16000);
        this.simplexOffsetC = Rand.Next(0, -8000);
        this.simplexOffsetD = Rand.Next(-8000, -16000);
        this.initSeasonColors();
        this.setup();
        this.climateValues = new ClimateValues(this);
        this.thunderStorm = new ThunderStorm(this);
        this.weatherPeriod = new WeatherPeriod(this, this.thunderStorm);
        this.climateForecaster = new ClimateForecaster();
        this.climateHistory = new ClimateHistory();
        try {
            LuaEventManager.triggerEvent("OnClimateManagerInit", this);
        }
        catch (Exception e) {
            DebugType.General.printException(e, e.getMessage(), LogSeverity.Error);
        }
    }

    public ClimateColorInfo getColNight() {
        return this.colNight;
    }

    public ClimateColorInfo getColNightNoMoon() {
        return this.colNightNoMoon;
    }

    public ClimateColorInfo getColNightMoon() {
        return this.colNightMoon;
    }

    public ClimateColorInfo getColFog() {
        return this.colFog;
    }

    public ClimateColorInfo getColFogLegacy() {
        return this.colFogLegacy;
    }

    public ClimateColorInfo getColFogNew() {
        return this.colFogNew;
    }

    public ClimateColorInfo getFogTintStorm() {
        return this.fogTintStorm;
    }

    public ClimateColorInfo getFogTintTropical() {
        return this.fogTintTropical;
    }

    private void setup() {
        int i;
        for (i = 0; i < this.climateFloats.length; ++i) {
            this.climateFloats[i] = new ClimateFloat();
        }
        for (i = 0; i < this.climateColors.length; ++i) {
            this.climateColors[i] = new ClimateColor();
        }
        for (i = 0; i < this.climateBooleans.length; ++i) {
            this.climateBooleans[i] = new ClimateBool();
        }
        this.desaturation = this.initClimateFloat(0, "DESATURATION");
        this.globalLightIntensity = this.initClimateFloat(1, "GLOBAL_LIGHT_INTENSITY");
        this.nightStrength = this.initClimateFloat(2, "NIGHT_STRENGTH");
        this.precipitationIntensity = this.initClimateFloat(3, "PRECIPITATION_INTENSITY");
        this.temperature = this.initClimateFloat(4, "TEMPERATURE");
        this.temperature.min = -80.0f;
        this.temperature.max = 80.0f;
        this.fogIntensity = this.initClimateFloat(5, "FOG_INTENSITY");
        this.windIntensity = this.initClimateFloat(6, "WIND_INTENSITY");
        this.windAngleIntensity = this.initClimateFloat(7, "WIND_ANGLE_INTENSITY");
        this.windAngleIntensity.min = -1.0f;
        this.cloudIntensity = this.initClimateFloat(8, "CLOUD_INTENSITY");
        this.ambient = this.initClimateFloat(9, "AMBIENT");
        this.viewDistance = this.initClimateFloat(10, "VIEW_DISTANCE");
        this.viewDistance.min = 0.0f;
        this.viewDistance.max = 100.0f;
        this.dayLightStrength = this.initClimateFloat(11, "DAYLIGHT_STRENGTH");
        this.humidity = this.initClimateFloat(12, "HUMIDITY");
        this.globalLight = this.initClimateColor(0, "GLOBAL_LIGHT");
        this.colorNewFog = this.initClimateColor(1, "COLOR_NEW_FOG");
        this.colorNewFog.internalValue.setExterior(0.9f, 0.9f, 0.95f, 1.0f);
        this.colorNewFog.internalValue.setInterior(0.9f, 0.9f, 0.95f, 1.0f);
        this.precipitationIsSnow = this.initClimateBool(0, "IS_SNOW");
    }

    public int getFloatMax() {
        return 13;
    }

    private ClimateFloat initClimateFloat(int id, String name) {
        if (id >= 0 && id < 13) {
            return this.climateFloats[id].init(id, name);
        }
        DebugLog.log("Climate: cannot get float override id.");
        return null;
    }

    public ClimateFloat getClimateFloat(int id) {
        if (id >= 0 && id < 13) {
            return this.climateFloats[id];
        }
        DebugLog.log("Climate: cannot get float override id.");
        return null;
    }

    public int getColorMax() {
        return 2;
    }

    private ClimateColor initClimateColor(int id, String name) {
        if (id >= 0 && id < 2) {
            return this.climateColors[id].init(id, name);
        }
        DebugLog.log("Climate: cannot get float override id.");
        return null;
    }

    public ClimateColor getClimateColor(int id) {
        if (id >= 0 && id < 2) {
            return this.climateColors[id];
        }
        DebugLog.log("Climate: cannot get float override id.");
        return null;
    }

    public int getBoolMax() {
        return 1;
    }

    private ClimateBool initClimateBool(int id, String name) {
        if (id >= 0 && id < 1) {
            return this.climateBooleans[id].init(id, name);
        }
        DebugLog.log("Climate: cannot get boolean id.");
        return null;
    }

    public ClimateBool getClimateBool(int id) {
        if (id >= 0 && id < 1) {
            return this.climateBooleans[id];
        }
        DebugLog.log("Climate: cannot get boolean id.");
        return null;
    }

    public void setEnabledSimulation(boolean b) {
        this.disableSimulation = !GameClient.client && !GameServer.server ? !b : false;
    }

    public boolean getEnabledSimulation() {
        return !this.disableSimulation;
    }

    public boolean getEnabledFxUpdate() {
        return !this.disableFxUpdate;
    }

    public void setEnabledFxUpdate(boolean b) {
        this.disableFxUpdate = !GameClient.client && !GameServer.server ? !b : false;
    }

    public boolean getEnabledWeatherGeneration() {
        return this.disableWeatherGeneration;
    }

    public void setEnabledWeatherGeneration(boolean b) {
        this.disableWeatherGeneration = !b;
    }

    public Color getGlobalLightInternal() {
        return this.globalLight.internalValue.getExterior();
    }

    public ClimateColorInfo getGlobalLight() {
        return this.globalLight.finalValue;
    }

    public float getGlobalLightIntensity() {
        return this.globalLightIntensity.finalValue;
    }

    public ClimateColorInfo getColorNewFog() {
        return this.colorNewFog.finalValue;
    }

    public void setNightStrength(float b) {
        this.nightStrength.finalValue = ClimateManager.clamp(0.0f, 1.0f, b);
    }

    public float getDesaturation() {
        return this.desaturation.finalValue;
    }

    public void setDesaturation(float desaturation) {
        this.desaturation.finalValue = desaturation;
    }

    public float getAirMass() {
        return this.airMass;
    }

    public float getAirMassDaily() {
        return this.airMassDaily;
    }

    public float getAirMassTemperature() {
        return this.airMassTemperature;
    }

    public float getDayLightStrength() {
        return this.dayLightStrength.finalValue;
    }

    public float getNightStrength() {
        return this.nightStrength.finalValue;
    }

    public float getDayMeanTemperature() {
        return this.currentDay.season.getDayMeanTemperature();
    }

    public float getTemperature() {
        return this.temperature.finalValue;
    }

    public float getBaseTemperature() {
        return this.baseTemperature;
    }

    public float getSnowStrength() {
        return this.snowStrength;
    }

    public boolean getPrecipitationIsSnow() {
        return this.precipitationIsSnow.finalValue;
    }

    public float getPrecipitationIntensity() {
        return this.precipitationIntensity.finalValue;
    }

    public float getFogIntensity() {
        return this.fogIntensity.finalValue;
    }

    public float getWindIntensity() {
        return this.windIntensity.finalValue;
    }

    public float getWindAngleIntensity() {
        return this.windAngleIntensity.finalValue;
    }

    public float getCorrectedWindAngleIntensity() {
        return (this.windAngleIntensity.finalValue + 1.0f) * 0.5f;
    }

    public float getWindPower() {
        return this.windPower;
    }

    public float getWindspeedKph() {
        return this.windPower * 120.0f;
    }

    public float getCloudIntensity() {
        return this.cloudIntensity.finalValue;
    }

    public float getAmbient() {
        return this.ambient.finalValue;
    }

    public float getViewDistance() {
        return this.viewDistance.finalValue;
    }

    public float getHumidity() {
        return this.humidity.finalValue;
    }

    public float getWindAngleDegrees() {
        float windAngle = this.windAngleIntensity.finalValue > 0.0f ? ClimateManager.lerp(this.windAngleIntensity.finalValue, 45.0f, 225.0f) : (this.windAngleIntensity.finalValue > -0.25f ? ClimateManager.lerp(Math.abs(this.windAngleIntensity.finalValue), 45.0f, 0.0f) : ClimateManager.lerp(Math.abs(this.windAngleIntensity.finalValue) - 0.25f, 360.0f, 180.0f));
        if (windAngle > 360.0f) {
            windAngle -= 360.0f;
        }
        if (windAngle < 0.0f) {
            windAngle += 360.0f;
        }
        return windAngle;
    }

    public float getWindAngleRadians() {
        return (float)Math.toRadians(this.getWindAngleDegrees());
    }

    public float getWindSpeedMovement() {
        float windspeed = this.getWindIntensity();
        windspeed = windspeed < 0.15f ? 0.0f : (windspeed - 0.15f) / 0.85f;
        return windspeed;
    }

    public float getWindForceMovement(IsoGameCharacter character, float angle) {
        if (character.square != null && !character.square.isInARoom()) {
            float windforce = angle - this.getWindAngleRadians();
            if ((double)windforce > Math.PI * 2) {
                windforce = (float)((double)windforce - Math.PI * 2);
            }
            if (windforce < 0.0f) {
                windforce = (float)((double)windforce + Math.PI * 2);
            }
            if ((double)windforce > Math.PI) {
                windforce = (float)(Math.PI - ((double)windforce - Math.PI));
            }
            windforce = (float)((double)windforce / Math.PI);
            return windforce;
        }
        return 0.0f;
    }

    public boolean isRaining() {
        return this.getPrecipitationIntensity() > 0.0f && !this.getPrecipitationIsSnow();
    }

    public float getRainIntensity() {
        return this.isRaining() ? this.getPrecipitationIntensity() : 0.0f;
    }

    public boolean isSnowing() {
        return this.getPrecipitationIntensity() > 0.0f && this.getPrecipitationIsSnow();
    }

    public float getSnowIntensity() {
        return this.isSnowing() ? this.getPrecipitationIntensity() : 0.0f;
    }

    public void setAmbient(float f) {
        this.ambient.finalValue = f;
    }

    public void setViewDistance(float f) {
        this.viewDistance.finalValue = f;
    }

    public void setDayLightStrength(float f) {
        this.dayLightStrength.finalValue = f;
    }

    public void setPrecipitationIsSnow(boolean b) {
        this.precipitationIsSnow.finalValue = b;
    }

    public DayInfo getCurrentDay() {
        return this.currentDay;
    }

    public DayInfo getPreviousDay() {
        return this.previousDay;
    }

    public DayInfo getNextDay() {
        return this.nextDay;
    }

    public ErosionSeason getSeason() {
        return this.currentDay != null && this.currentDay.getSeason() != null ? this.currentDay.getSeason() : this.season;
    }

    public float getFrontStrength() {
        if (this.currentFront == null) {
            return 0.0f;
        }
        if (Core.debug) {
            this.CalculateWeatherFrontStrength(this.gt.getYear(), this.gt.getMonth(), this.gt.getDayPlusOne(), this.currentFront);
        }
        return this.currentFront.strength;
    }

    public void stopWeatherAndThunder() {
        if (GameClient.client) {
            return;
        }
        this.weatherPeriod.stopWeatherPeriod();
        this.thunderStorm.stopAllClouds();
        if (GameServer.server) {
            this.transmitClimatePacket(ClimateNetAuth.ServerOnly, (byte)1, null);
        }
    }

    public ThunderStorm getThunderStorm() {
        return this.thunderStorm;
    }

    public WeatherPeriod getWeatherPeriod() {
        return this.weatherPeriod;
    }

    public boolean getIsThunderStorming() {
        return this.weatherPeriod.isRunning() && (this.weatherPeriod.isThunderStorm() || this.weatherPeriod.isTropicalStorm());
    }

    public float getWeatherInterference() {
        if (this.weatherPeriod.isRunning()) {
            if (this.weatherPeriod.isThunderStorm() || this.weatherPeriod.isTropicalStorm() || this.weatherPeriod.isBlizzard()) {
                return 0.7f * this.weatherPeriod.getCurrentStrength();
            }
            return 0.35f * this.weatherPeriod.getCurrentStrength();
        }
        return 0.0f;
    }

    public KahluaTable getModData() {
        if (this.modDataTable == null) {
            this.modDataTable = LuaManager.platform.newTable();
        }
        return this.modDataTable;
    }

    public float getAirTemperatureForCharacter(IsoGameCharacter plr) {
        return this.getAirTemperatureForCharacter(plr, false);
    }

    public float getAirTemperatureForCharacter(IsoGameCharacter plr, boolean doWindChill) {
        if (plr.square != null) {
            if (plr.getVehicle() != null) {
                return this.getAirTemperatureForSquare(plr.square, plr.getVehicle(), doWindChill);
            }
            return this.getAirTemperatureForSquare(plr.square, null, doWindChill);
        }
        return this.getTemperature();
    }

    public float getAirTemperatureForSquare(IsoGridSquare square) {
        return this.getAirTemperatureForSquare(square, null);
    }

    public float getAirTemperatureForSquare(IsoGridSquare square, BaseVehicle vehicle) {
        return this.getAirTemperatureForSquare(square, vehicle, false);
    }

    public float getAirTemperatureForSquare(IsoGridSquare square, BaseVehicle vehicle, boolean doWindChill) {
        float temp = this.getTemperature();
        if (square != null) {
            boolean isInside = square.isInARoom();
            if (isInside || vehicle != null) {
                boolean electricity = IsoWorld.instance.isHydroPowerOn();
                if (temp <= 22.0f) {
                    if (isInside && electricity) {
                        temp = 22.0f;
                    }
                    float mod = 22.0f - temp;
                    if (square.getZ() < 1) {
                        temp += mod * (0.4f + 0.2f * this.dayLightLagged);
                    } else {
                        mod = (float)((double)mod * 0.85);
                        temp += mod * (0.4f + 0.2f * this.dayLightLagged);
                    }
                } else {
                    if (isInside && electricity) {
                        temp = 22.0f;
                    }
                    float mod = temp - 22.0f;
                    if (square.getZ() < 1) {
                        mod = (float)((double)mod * 0.85);
                        temp -= mod * (0.4f + 0.2f * this.dayLightLagged);
                    } else {
                        temp -= mod * (0.4f + 0.2f * this.dayLightLagged + 0.2f * this.nightLagged);
                    }
                    if (!isInside && vehicle != null) {
                        temp = temp + mod + mod * this.dayLightLagged;
                    }
                }
            } else if (doWindChill) {
                temp = Temperature.WindchillCelsiusKph(temp, this.getWindspeedKph());
            }
            float heatsourceTemp = IsoWorld.instance.getCell().getHeatSourceHighestTemperature(temp, square.getX(), square.getY(), square.getZ());
            if (heatsourceTemp > temp) {
                temp = heatsourceTemp;
            }
            if (vehicle != null) {
                temp = !isInside ? (temp += vehicle.getInsideTemperature()) : (temp += vehicle.getInsideTemperature() > 0.0f ? vehicle.getInsideTemperature() : 0.0f);
            }
        }
        return temp;
    }

    public String getSeasonName() {
        if (this.season == null || this.season.getSeasonName() == null) {
            return null;
        }
        return this.season.getSeasonName();
    }

    public String getSeasonNameTranslated() {
        if (this.season == null || this.season.getSeasonNameTranslated() == null) {
            return null;
        }
        return this.season.getSeasonNameTranslated();
    }

    public byte getSeasonId() {
        return (byte)this.season.getSeason();
    }

    public float getSeasonProgression() {
        return this.season.getSeasonProgression();
    }

    public float getSeasonStrength() {
        return this.season.getSeasonStrength();
    }

    public void init(IsoMetaGrid metaGrid) {
        WorldFlares.Clear();
        this.season = ErosionMain.getInstance().getSeasons();
        ThunderStorm.mapMinX = metaGrid.minX * 256 - 4000;
        ThunderStorm.mapMaxX = metaGrid.maxX * 256 + 4000;
        ThunderStorm.mapMinY = metaGrid.minY * 256 - 4000;
        ThunderStorm.mapMaxY = metaGrid.maxY * 256 + 4000;
        windNoiseOffset = 0.0;
        winterIsComing = IsoWorld.instance.getGameMode().equals("Winter is Coming");
        theDescendingFog = IsoWorld.instance.getGameMode().equals("The Descending Fog");
        aStormIsComing = IsoWorld.instance.getGameMode().equals("A Storm is Coming");
        this.climateForecaster.init(this);
        this.climateHistory.init(this);
    }

    public void updateEveryTenMins() {
        this.tickIsTenMins = true;
    }

    public void update() {
        int i;
        this.tickIsClimateTick = false;
        this.tickIsHourChange = false;
        this.tickIsDayChange = false;
        this.gt = GameTime.getInstance();
        this.worldAgeHours = this.gt.getWorldAgeHours();
        if (this.lastMinuteStamp != this.gt.getMinutesStamp()) {
            this.lastMinuteStamp = this.gt.getMinutesStamp();
            this.tickIsClimateTick = true;
            this.updateDayInfo(this.gt.getDayPlusOne(), this.gt.getMonth(), this.gt.getYear());
            this.currentDay.hour = this.gt.getHour();
            this.currentDay.minutes = this.gt.getMinutes();
            if (this.gt.getHour() != this.lastHourStamp) {
                this.tickIsHourChange = true;
                this.lastHourStamp = this.gt.getHour();
            }
            ClimateMoon.getInstance().updatePhase(this.currentDay.getYear(), this.currentDay.getMonth(), this.currentDay.getDay());
        }
        if (this.disableSimulation) {
            IsoPlayer[] players = IsoPlayer.players;
            for (int i2 = 0; i2 < players.length; ++i2) {
                IsoPlayer player = players[i2];
                if (player == null) continue;
                player.dirtyRecalcGridStackTime = 1.0f;
            }
            return;
        }
        if (this.tickIsDayChange && !GameClient.client) {
            this.climateForecaster.updateDayChange(this);
            this.climateHistory.updateDayChange(this);
        }
        if (GameClient.client) {
            int i3;
            this.networkLerp = 1.0f;
            long curtime = System.currentTimeMillis();
            if ((float)curtime < (float)this.networkUpdateStamp + this.networkLerpTime) {
                this.networkLerp = (float)(curtime - this.networkUpdateStamp) / this.networkLerpTime;
                if (this.networkLerp < 0.0f) {
                    this.networkLerp = 0.0f;
                }
            }
            for (i3 = 0; i3 < this.climateFloats.length; ++i3) {
                this.climateFloats[i3].interpolate = this.networkLerp;
            }
            for (i3 = 0; i3 < this.climateColors.length; ++i3) {
                this.climateColors[i3].interpolate = this.networkLerp;
            }
        }
        if (this.tickIsClimateTick && !GameClient.client) {
            this.updateSandboxOverrides();
            this.updateValues();
            this.weatherPeriod.update(this.worldAgeHours);
        }
        if (this.tickIsClimateTick) {
            LuaEventManager.triggerEvent("OnClimateTick", this);
        }
        for (i = 0; i < this.climateColors.length; ++i) {
            this.climateColors[i].calculate();
        }
        for (i = 0; i < this.climateFloats.length; ++i) {
            this.climateFloats[i].calculate();
        }
        for (i = 0; i < this.climateBooleans.length; ++i) {
            this.climateBooleans[i].calculate();
        }
        this.updateWindTick();
        this.windPower = this.windIntensity.finalValue;
        this.updateTestFlare();
        this.thunderStorm.update(this.worldAgeHours);
        if (GameClient.client) {
            this.updateSnow();
        } else if (this.tickIsClimateTick && !GameClient.client) {
            this.updateSnow();
        }
        if (!GameClient.client) {
            this.updateViewDistance();
        }
        if (this.tickIsClimateTick && Core.debug && !GameServer.server) {
            LuaEventManager.triggerEvent("OnClimateTickDebug", this);
        }
        if (this.tickIsClimateTick && GameServer.server && this.tickIsTenMins) {
            this.transmitClimatePacket(ClimateNetAuth.ServerOnly, (byte)0, null);
            this.tickIsTenMins = false;
        }
        if (!this.disableFxUpdate) {
            this.updateFx();
        }
    }

    private void updateSandboxOverrides() {
        boolean isEndlessFog;
        boolean isFreezingTemp;
        boolean weatherSettingChanged = false;
        float minuteLerp = GameTime.getInstance().getTimeOfDay() % 1.0f;
        int climateCycle = SandboxOptions.getInstance().climateCycle.getValue();
        boolean isOverridingWeather = climateCycle > 1;
        boolean isEndlessWeather = climateCycle > 2;
        boolean isNoWeather = climateCycle == 2;
        boolean isEndlessRain = climateCycle == 3;
        boolean isEndlessSnow = climateCycle == 5;
        boolean isEndlessBlizzard = climateCycle == 6;
        boolean bl = isFreezingTemp = isEndlessSnow || isEndlessBlizzard;
        if (this.weatherOverride != climateCycle) {
            this.weatherOverride = climateCycle;
            this.setEnabledWeatherGeneration(this.weatherOverride == 1);
            this.precipitationIntensity.setOverrideValue(isOverridingWeather);
            this.windIntensity.setOverrideValue(isOverridingWeather);
            this.temperature.setOverrideValue(isFreezingTemp);
            Core.getInstance().setForceSnow(isFreezingTemp);
            if (isNoWeather) {
                Core.getInstance().setForceSnow(false);
                this.precipitationIntensity.setOverride(0.0f, 1.0f);
                this.windIntensity.setOverride(0.0f, 1.0f);
                this.temperature.setOverrideValue(false);
            } else if (isEndlessWeather) {
                this.precipitationIntensity.overrideInternal = 0.5f;
                this.windIntensity.overrideInternal = 0.3f;
                if (isFreezingTemp) {
                    this.temperature.overrideInternal = Rand.Next(-20.0f, 0.0f);
                }
            }
            weatherSettingChanged = isOverridingWeather;
        }
        if (isEndlessWeather) {
            this.precipitationIntensity.interpolate = minuteLerp;
            this.windIntensity.interpolate = minuteLerp;
            this.temperature.interpolate = minuteLerp;
            if (this.tickIsHourChange || weatherSettingChanged) {
                Core.getInstance().setForceSnow(isFreezingTemp);
                this.precipitationIsSnow.setOverride(isFreezingTemp);
                boolean isCalmWeather = isEndlessRain || isEndlessSnow;
                this.precipitationIntensity.overrideInternal = this.precipitationIntensity.override;
                this.precipitationIntensity.setOverride(Rand.Next(isCalmWeather ? 0.1f : 0.5f, isCalmWeather ? 0.5f : 1.0f), minuteLerp);
                this.windIntensity.overrideInternal = this.windIntensity.override;
                this.windIntensity.setOverride(Rand.Next(isCalmWeather ? 0.0f : 0.3f, isCalmWeather ? 0.3f : 1.0f), minuteLerp);
                if (isEndlessSnow || isEndlessBlizzard) {
                    this.temperature.overrideInternal = this.temperature.override;
                    this.temperature.setOverride(Rand.Next(-20.0f, 0.0f), minuteLerp);
                }
            }
        }
        boolean blizzardFog = isEndlessBlizzard && SandboxOptions.getInstance().fogCycle.getValue() != 2;
        int fogCycle = blizzardFog ? 4 : SandboxOptions.getInstance().fogCycle.getValue();
        boolean isOverridingFog = fogCycle > 1;
        boolean isNoFog = fogCycle == 2;
        boolean bl2 = isEndlessFog = fogCycle >= 3;
        if (this.fogOverride != fogCycle) {
            this.fogOverride = fogCycle;
            this.fogIntensity.setEnableOverride(isOverridingFog);
            this.fogIntensity.setOverrideValue(isOverridingFog);
            if (isNoFog) {
                this.fogIntensity.setOverride(0.0f, 1.0f);
            } else if (isEndlessFog) {
                this.fogIntensity.overrideInternal = 0.5f;
            }
            weatherSettingChanged = isOverridingFog;
        }
        if (isEndlessFog) {
            this.fogIntensity.interpolate = minuteLerp;
            if (this.tickIsHourChange || weatherSettingChanged) {
                this.fogIntensity.overrideInternal = this.fogIntensity.override;
                this.fogIntensity.setOverride(Rand.Next(0.1f, 1.0f), minuteLerp);
            }
        }
    }

    public static double getWindNoiseBase() {
        return windNoiseBase;
    }

    public static double getWindNoiseFinal() {
        return windNoiseFinal;
    }

    public static double getWindTickFinal() {
        return windTickFinal;
    }

    private void updateWindTick() {
        if (GameServer.server) {
            return;
        }
        float wind = this.windIntensity.finalValue;
        windNoiseBase = SimplexNoise.noise(0.0, windNoiseOffset += (4.0E-4 + 6.0E-4 * (double)wind) * (double)GameTime.getInstance().getMultiplier());
        windNoiseFinal = windNoiseBase;
        windNoiseFinal = windNoiseFinal > 0.0 ? (windNoiseFinal *= 0.04 + 0.1 * (double)wind) : (windNoiseFinal *= 0.04 + 0.1 * (double)wind + (double)(0.05f * (wind * wind)));
        wind = ClimateManager.clamp01(wind + (float)windNoiseFinal);
        windTickFinal = wind;
    }

    public void updateOLD() {
        this.tickIsClimateTick = false;
        this.tickIsHourChange = false;
        this.tickIsDayChange = false;
        this.gt = GameTime.getInstance();
        this.worldAgeHours = this.gt.getWorldAgeHours();
        if (this.lastMinuteStamp != this.gt.getMinutesStamp()) {
            this.lastMinuteStamp = this.gt.getMinutesStamp();
            this.tickIsClimateTick = true;
            this.updateDayInfo(this.gt.getDay(), this.gt.getMonth(), this.gt.getYear());
            this.currentDay.hour = this.gt.getHour();
            this.currentDay.minutes = this.gt.getMinutes();
            if (this.gt.getHour() != this.lastHourStamp) {
                this.tickIsHourChange = true;
                this.lastHourStamp = this.gt.getHour();
            }
        }
        if (GameClient.client) {
            if (!this.disableSimulation) {
                int i;
                this.networkLerp = 1.0f;
                long curtime = System.currentTimeMillis();
                if ((float)curtime < (float)this.networkUpdateStamp + this.networkLerpTime) {
                    this.networkLerp = (float)(curtime - this.networkUpdateStamp) / this.networkLerpTime;
                    if (this.networkLerp < 0.0f) {
                        this.networkLerp = 0.0f;
                    }
                }
                for (i = 0; i < this.climateFloats.length; ++i) {
                    this.climateFloats[i].interpolate = this.networkLerp;
                }
                for (i = 0; i < this.climateColors.length; ++i) {
                    this.climateColors[i].interpolate = this.networkLerp;
                }
                if (this.tickIsClimateTick) {
                    LuaEventManager.triggerEvent("OnClimateTick", this);
                }
                this.updateOnTick();
                this.updateTestFlare();
                this.thunderStorm.update(this.worldAgeHours);
                this.updateSnow();
                if (this.tickIsTenMins) {
                    this.tickIsTenMins = false;
                }
            }
            this.updateFx();
        } else {
            if (!this.disableSimulation) {
                if (this.tickIsClimateTick) {
                    this.updateValues();
                    this.weatherPeriod.update(this.gt.getWorldAgeHours());
                }
                this.updateOnTick();
                this.updateTestFlare();
                this.thunderStorm.update(this.worldAgeHours);
                if (this.tickIsClimateTick) {
                    this.updateSnow();
                    LuaEventManager.triggerEvent("OnClimateTick", this);
                }
                this.updateViewDistance();
                if (this.tickIsClimateTick && this.tickIsTenMins) {
                    if (GameServer.server) {
                        this.transmitClimatePacket(ClimateNetAuth.ServerOnly, (byte)0, null);
                    }
                    this.tickIsTenMins = false;
                }
            }
            if (!this.disableFxUpdate && this.tickIsClimateTick) {
                this.updateFx();
            }
            if (this.disableSimulation) {
                IsoPlayer[] players = IsoPlayer.players;
                for (int i = 0; i < players.length; ++i) {
                    IsoPlayer player = players[i];
                    if (player == null) continue;
                    player.dirtyRecalcGridStackTime = 1.0f;
                }
            }
        }
    }

    private void updateFx() {
        IsoWeatherFX weatherFX = IsoWorld.instance.getCell().getWeatherFX();
        if (weatherFX == null) {
            return;
        }
        weatherFX.setPrecipitationIntensity(this.precipitationIntensity.finalValue);
        weatherFX.setWindIntensity(this.windIntensity.finalValue);
        weatherFX.setWindPrecipIntensity((float)windTickFinal * (float)windTickFinal);
        weatherFX.setWindAngleIntensity(this.windAngleIntensity.finalValue);
        weatherFX.setFogIntensity(this.fogIntensity.finalValue);
        weatherFX.setCloudIntensity(this.cloudIntensity.finalValue);
        weatherFX.setPrecipitationIsSnow(this.precipitationIsSnow.finalValue);
        SkyBox.getInstance().update(this);
        IsoWater.getInstance().update(this);
        IsoPuddles.getInstance().update(this);
    }

    private void updateSnow() {
        if (GameClient.client) {
            IsoWorld.instance.currentCell.setSnowTarget((int)(this.snowFracNow * 100.0f));
            ErosionIceQueen.instance.setSnow(this.canDoWinterSprites && this.snowFracNow > 0.2f);
            return;
        }
        if (Core.getInstance().isForceSnow()) {
            this.snowFracNow = 0.7f;
            IsoWorld.instance.currentCell.setSnowTarget((int)(this.snowFracNow * 100.0f));
            ErosionIceQueen.instance.setSnow(this.snowFracNow > 0.2f);
            this.wasForceSnow = true;
            return;
        }
        if (this.wasForceSnow) {
            this.snowFracNow = this.snowStrength > 7.5f ? 1.0f : this.snowStrength / 7.5f;
            IsoWorld.instance.currentCell.setSnowTarget((int)(this.snowFracNow * 100.0f));
            ErosionIceQueen.instance.setSnow(this.canDoWinterSprites && this.snowFracNow > 0.2f);
            this.wasForceSnow = false;
        }
        if (!this.tickIsHourChange) {
            boolean bl = this.canDoWinterSprites = this.season.isSeason(5) || winterIsComing;
            if (this.precipitationIsSnow.finalValue && this.precipitationIntensity.finalValue > this.snowFall) {
                this.snowFall = this.precipitationIntensity.finalValue;
            }
            if (this.temperature.finalValue > 0.0f) {
                float melt = this.temperature.finalValue / 10.0f;
                if ((melt = melt * 0.2f + melt * 0.8f * this.dayLightStrength.finalValue) > this.snowMeltStrength) {
                    this.snowMeltStrength = melt;
                }
            }
            if (!this.precipitationIsSnow.finalValue && this.precipitationIntensity.finalValue > 0.0f) {
                this.snowMeltStrength += this.precipitationIntensity.finalValue;
            }
        } else {
            this.snowStrength += this.snowFall;
            this.snowStrength -= this.snowMeltStrength;
            this.snowStrength = ClimateManager.clamp(0.0f, 10.0f, this.snowStrength);
            this.snowFracNow = this.snowStrength > 7.5f ? 1.0f : this.snowStrength / 7.5f;
            IsoWorld.instance.currentCell.setSnowTarget((int)(this.snowFracNow * 100.0f));
            ErosionIceQueen.instance.setSnow(this.canDoWinterSprites && this.snowFracNow > 0.2f);
            this.snowFall = 0.0f;
            this.snowMeltStrength = 0.0f;
        }
    }

    private void updateSnowOLD() {
    }

    public float getSnowFracNow() {
        return this.snowFracNow;
    }

    public void resetOverrides() {
        int i;
        for (i = 0; i < this.climateColors.length; ++i) {
            this.climateColors[i].setEnableOverride(false);
        }
        for (i = 0; i < this.climateFloats.length; ++i) {
            this.climateFloats[i].setEnableOverride(false);
        }
        for (i = 0; i < this.climateBooleans.length; ++i) {
            this.climateBooleans[i].setEnableOverride(false);
        }
    }

    public void resetModded() {
        int i;
        for (i = 0; i < this.climateColors.length; ++i) {
            this.climateColors[i].setEnableModded(false);
        }
        for (i = 0; i < this.climateFloats.length; ++i) {
            this.climateFloats[i].setEnableModded(false);
        }
        for (i = 0; i < this.climateBooleans.length; ++i) {
            this.climateBooleans[i].setEnableModded(false);
        }
    }

    public void resetAdmin() {
        int i;
        for (i = 0; i < this.climateColors.length; ++i) {
            this.climateColors[i].setEnableAdmin(false);
        }
        for (i = 0; i < this.climateFloats.length; ++i) {
            this.climateFloats[i].setEnableAdmin(false);
        }
        for (i = 0; i < this.climateBooleans.length; ++i) {
            this.climateBooleans[i].setEnableAdmin(false);
        }
    }

    public void triggerWinterIsComingStorm() {
        if (!GameClient.client && !this.weatherPeriod.isRunning()) {
            AirFront front = new AirFront();
            front.copyFrom(this.currentFront);
            front.strength = 0.95f;
            front.type = 1;
            GameTime gt = GameTime.getInstance();
            this.weatherPeriod.init(front, this.worldAgeHours, gt.getYear(), gt.getMonth(), gt.getDayPlusOne());
        }
    }

    public boolean triggerCustomWeather(float strength, boolean warmFront) {
        if (!GameClient.client && !this.weatherPeriod.isRunning()) {
            AirFront front = new AirFront();
            front.strength = strength;
            front.type = warmFront ? 1 : -1;
            GameTime gt = GameTime.getInstance();
            this.weatherPeriod.init(front, this.worldAgeHours, gt.getYear(), gt.getMonth(), gt.getDayPlusOne());
            return true;
        }
        return false;
    }

    public boolean triggerCustomWeatherStage(int stage, float duration) {
        if (!GameClient.client && !this.weatherPeriod.isRunning()) {
            AirFront front = new AirFront();
            front.strength = 0.95f;
            front.type = 1;
            GameTime gt = GameTime.getInstance();
            this.weatherPeriod.init(front, this.worldAgeHours, gt.getYear(), gt.getMonth(), gt.getDayPlusOne(), stage, duration);
            return true;
        }
        return false;
    }

    private void updateOnTick() {
        int i;
        for (i = 0; i < this.climateColors.length; ++i) {
            this.climateColors[i].calculate();
        }
        for (i = 0; i < this.climateFloats.length; ++i) {
            this.climateFloats[i].calculate();
        }
        for (i = 0; i < this.climateBooleans.length; ++i) {
            this.climateBooleans[i].calculate();
        }
    }

    private void updateTestFlare() {
        WorldFlares.update();
    }

    public void launchFlare() {
        DebugLog.log("Launching improved flare.");
        IsoPlayer player = IsoPlayer.getInstance();
        float windspeed = 0.0f;
        WorldFlares.launchFlare(7200.0f, (int)player.getX(), (int)player.getY(), 50, 0.0f, 1.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f);
        if (IsoPlayer.getInstance() != null && !this.flareLaunched) {
            this.flareLaunched = true;
            this.flareLifeTime = 0.0f;
            this.flareMaxLifeTime = 7200.0f;
            this.flareIntensity.overrideCurrentValue(1.0f);
            this.flareIntens = 1.0f;
            this.nextRandomTargetIntens = 10;
        }
    }

    protected double getAirMassNoiseFrequencyMod(int sandboxRain) {
        if (sandboxRain == 1) {
            return 300.0;
        }
        if (sandboxRain == 2) {
            return 240.0;
        }
        if (sandboxRain != 3) {
            if (sandboxRain == 4) {
                return 145.0;
            }
            if (sandboxRain == 5) {
                return 120.0;
            }
        }
        return 166.0;
    }

    protected float getRainTimeMultiplierMod(int sandboxRain) {
        if (sandboxRain == 1) {
            return 0.5f;
        }
        if (sandboxRain == 2) {
            return 0.75f;
        }
        if (sandboxRain == 4) {
            return 1.25f;
        }
        if (sandboxRain == 5) {
            return 1.5f;
        }
        return 1.0f;
    }

    private void updateValues() {
        if (this.tickIsDayChange && Core.debug && !GameClient.client && !GameServer.server) {
            ErosionMain.getInstance().DebugUpdateMapNow();
        }
        this.climateValues.updateValues(this.worldAgeHours, this.gt.getTimeOfDay(), this.currentDay, this.nextDay);
        this.airMass = this.climateValues.getNoiseAirmass();
        this.airMassTemperature = this.climateValues.getAirMassTemperature();
        if (this.tickIsHourChange) {
            int airType;
            int n = airType = this.airMass < 0.0f ? -1 : 1;
            if (this.currentFront.type != airType) {
                if (!this.disableWeatherGeneration && (!winterIsComing || winterIsComing && GameTime.instance.getWorldAgeHours() > 96.0)) {
                    if (theDescendingFog) {
                        this.currentFront.type = -1;
                        this.currentFront.strength = Rand.Next(0.2f, 0.45f);
                        this.weatherPeriod.init(this.currentFront, this.worldAgeHours, this.gt.getYear(), this.gt.getMonth(), this.gt.getDayPlusOne());
                    } else {
                        this.CalculateWeatherFrontStrength(this.gt.getYear(), this.gt.getMonth(), this.gt.getDayPlusOne(), this.currentFront);
                        this.weatherPeriod.init(this.currentFront, this.worldAgeHours, this.gt.getYear(), this.gt.getMonth(), this.gt.getDayPlusOne());
                    }
                }
                this.currentFront.setFrontType(airType);
            }
            if (winterIsComing || theDescendingFog || !(GameTime.instance.getWorldAgeHours() >= 72.0) || !(GameTime.instance.getWorldAgeHours() <= 96.0) || this.disableWeatherGeneration || this.weatherPeriod.isRunning() || Rand.Next(0, 1000) < 50) {
                // empty if block
            }
            if (this.tickIsDayChange) {
                // empty if block
            }
        }
        this.dayDoFog = this.climateValues.isDayDoFog();
        this.dayFogStrength = this.climateValues.getDayFogStrength();
        this.dayFogStrength = PerformanceSettings.fogQuality == 2 ? 0.5f + 0.5f * this.dayFogStrength : 0.2f + 0.8f * this.dayFogStrength;
        this.baseTemperature = this.climateValues.getBaseTemperature();
        this.dayLightLagged = this.climateValues.getDayLightLagged();
        this.nightLagged = this.climateValues.getDayLightLagged();
        this.temperature.internalValue = this.climateValues.getTemperature();
        this.precipitationIsSnow.internalValue = this.climateValues.isTemperatureIsSnow();
        this.humidity.internalValue = this.climateValues.getHumidity();
        this.windIntensity.internalValue = this.climateValues.getWindIntensity();
        this.windAngleIntensity.internalValue = this.climateValues.getWindAngleIntensity();
        this.windPower = this.windIntensity.internalValue;
        this.currentFront.setFrontWind(this.climateValues.getWindAngleDegrees());
        this.cloudIntensity.internalValue = this.climateValues.getCloudIntensity();
        this.precipitationIntensity.internalValue = 0.0f;
        this.nightStrength.internalValue = this.climateValues.getNightStrength();
        this.dayLightStrength.internalValue = this.climateValues.getDayLightStrength();
        this.ambient.internalValue = this.climateValues.getAmbient();
        this.desaturation.internalValue = this.climateValues.getDesaturation();
        int curSeason = this.season.getSeason();
        float seasonProg = this.season.getSeasonProgression();
        float tval = 0.0f;
        int lerpFromSeason = 0;
        int lerpToSeason = 0;
        if (curSeason == 2) {
            lerpFromSeason = 3;
            lerpToSeason = 0;
            tval = 0.5f + seasonProg * 0.5f;
        } else if (curSeason == 3) {
            lerpFromSeason = 0;
            lerpToSeason = 1;
            tval = seasonProg * 0.5f;
        } else if (curSeason == 4) {
            if (seasonProg < 0.5f) {
                lerpFromSeason = 0;
                lerpToSeason = 1;
                tval = 0.5f + seasonProg;
            } else {
                lerpFromSeason = 1;
                lerpToSeason = 2;
                tval = seasonProg - 0.5f;
            }
        } else if (curSeason == 5) {
            if (seasonProg < 0.5f) {
                lerpFromSeason = 1;
                lerpToSeason = 2;
                tval = 0.5f + seasonProg;
            } else {
                lerpFromSeason = 2;
                lerpToSeason = 3;
                tval = seasonProg - 0.5f;
            }
        } else if (curSeason == 1) {
            if (seasonProg < 0.5f) {
                lerpFromSeason = 2;
                lerpToSeason = 3;
                tval = 0.5f + seasonProg;
            } else {
                lerpFromSeason = 3;
                lerpToSeason = 0;
                tval = seasonProg - 0.5f;
            }
        }
        float cloudyT = this.climateValues.getCloudyT();
        this.colDawn = this.seasonColorDawn.update(cloudyT, tval, lerpFromSeason, lerpToSeason);
        this.colDay = this.seasonColorDay.update(cloudyT, tval, lerpFromSeason, lerpToSeason);
        this.colDusk = this.seasonColorDusk.update(cloudyT, tval, lerpFromSeason, lerpToSeason);
        float time = this.climateValues.getTime();
        float dawn = this.climateValues.getDawn();
        float dusk = this.climateValues.getDusk();
        float noon = this.climateValues.getNoon();
        float fogDuration = this.climateValues.getDayFogDuration();
        if (!theDescendingFog) {
            if (this.dayDoFog && this.dayFogStrength > 0.0f && time > dawn - 2.0f && time < dawn + fogDuration) {
                float lerpfog = this.getTimeLerpHours(time, dawn - 2.0f, dawn + fogDuration, true);
                this.fogLerpValue = lerpfog = ClimateManager.clamp(0.0f, 1.0f, lerpfog * (fogDuration / 3.0f));
                this.cloudIntensity.internalValue = ClimateManager.lerp(lerpfog, this.cloudIntensity.internalValue, 0.0f);
                float fogVal = this.dayFogStrength;
                this.fogIntensity.internalValue = ClimateManager.clerp(lerpfog, 0.0f, fogVal);
                this.desaturation.internalValue = SceneShaderStore.weatherShader != null && Core.getInstance().getOffscreenBuffer() != null ? (PerformanceSettings.fogQuality == 2 ? ClimateManager.clerp(lerpfog, this.desaturation.internalValue, 0.8f * fogVal) : ClimateManager.clerp(lerpfog, this.desaturation.internalValue, 0.65f * fogVal)) : ClimateManager.clerp(lerpfog, this.desaturation.internalValue, 0.8f * fogVal);
            } else {
                this.fogIntensity.internalValue = 0.0f;
            }
        } else {
            this.fogIntensity.internalValue = this.gt.getWorldAgeHours() < 72.0 ? (float)this.gt.getWorldAgeHours() / 72.0f : 1.0f;
            this.cloudIntensity.internalValue = Math.min(this.cloudIntensity.internalValue, 1.0f - this.fogIntensity.internalValue);
            if (this.weatherPeriod.isRunning()) {
                this.fogIntensity.internalValue = Math.min(this.fogIntensity.internalValue, 0.6f);
            }
            if (PerformanceSettings.fogQuality == 2) {
                this.fogIntensity.internalValue *= 0.93f;
                this.desaturation.internalValue = 0.8f * this.fogIntensity.internalValue;
            } else {
                this.desaturation.internalValue = 0.65f * this.fogIntensity.internalValue;
            }
        }
        this.humidity.internalValue = ClimateManager.clamp01(this.humidity.internalValue + this.fogIntensity.internalValue * 0.6f);
        float dayMax = this.climateValues.getDayLightStrengthBase();
        float nightMax = 0.4f;
        float duskDawnMin = 0.25f * this.climateValues.getDayLightStrengthBase();
        if (SceneShaderStore.weatherShader != null && Core.getInstance().getOffscreenBuffer() != null) {
            duskDawnMin = 0.8f * this.climateValues.getDayLightStrengthBase();
        }
        if (time < dawn || time > dusk) {
            float total = 24.0f - dusk + dawn;
            if (time > dusk) {
                t = (time - dusk) / total;
                this.colDusk.interp(this.colDawn, t, this.globalLight.internalValue);
            } else {
                t = (24.0f - dusk + time) / total;
                this.colDusk.interp(this.colDawn, t, this.globalLight.internalValue);
            }
            this.globalLightIntensity.internalValue = ClimateManager.lerp(this.climateValues.getLerpNight(), duskDawnMin, 0.4f);
        } else if (time < noon + 2.0f) {
            t = (time - dawn) / (noon + 2.0f - dawn);
            this.colDawn.interp(this.colDay, t, this.globalLight.internalValue);
            this.globalLightIntensity.internalValue = ClimateManager.lerp(t, duskDawnMin, dayMax);
        } else {
            t = (time - (noon + 2.0f)) / (dusk - (noon + 2.0f));
            this.colDay.interp(this.colDusk, t, this.globalLight.internalValue);
            this.globalLightIntensity.internalValue = ClimateManager.lerp(t, dayMax, duskDawnMin);
        }
        if (this.fogIntensity.internalValue > 0.0f) {
            if (SceneShaderStore.weatherShader != null && Core.getInstance().getOffscreenBuffer() != null) {
                if (PerformanceSettings.fogQuality == 2) {
                    this.globalLight.internalValue.interp(this.colFog, this.fogIntensity.internalValue, this.globalLight.internalValue);
                } else {
                    this.globalLight.internalValue.interp(this.colFogNew, this.fogIntensity.internalValue, this.globalLight.internalValue);
                }
            } else {
                this.globalLight.internalValue.interp(this.colFogLegacy, this.fogIntensity.internalValue, this.globalLight.internalValue);
            }
            this.globalLightIntensity.internalValue = ClimateManager.clerp(this.fogLerpValue, this.globalLightIntensity.internalValue, 0.8f);
        }
        this.colNightNoMoon.interp(this.colNightMoon, ClimateMoon.getInstance().getMoonFloat(), this.colNight);
        this.globalLight.internalValue.interp(this.colNight, this.nightStrength.internalValue, this.globalLight.internalValue);
        IsoPlayer[] players = IsoPlayer.players;
        for (int i = 0; i < players.length; ++i) {
            IsoPlayer player = players[i];
            if (player == null) continue;
            player.dirtyRecalcGridStackTime = 1.0f;
        }
    }

    private void updateViewDistance() {
        float viewMod = this.dayLightStrength.finalValue;
        float fogMod = this.fogIntensity.finalValue;
        float min = 19.0f - fogMod * 8.0f;
        float max = min + 4.0f + 7.0f * viewMod * (1.0f - fogMod);
        this.gt.setViewDistMin(min *= 3.0f);
        this.gt.setViewDistMax(max *= 3.0f);
        this.viewDistance.finalValue = this.viewDistance.internalValue = min + (max - min) * viewMod;
    }

    public void setSeasonColorDawn(int temperature, int season, float r, float g, float b, float a, boolean exterior) {
        if (exterior) {
            this.seasonColorDawn.setColorExterior(temperature, season, r, g, b, a);
        } else {
            this.seasonColorDawn.setColorInterior(temperature, season, r, g, b, a);
        }
    }

    public void setSeasonColorDay(int temperature, int season, float r, float g, float b, float a, boolean exterior) {
        if (exterior) {
            this.seasonColorDay.setColorExterior(temperature, season, r, g, b, a);
        } else {
            this.seasonColorDay.setColorInterior(temperature, season, r, g, b, a);
        }
    }

    public void setSeasonColorDusk(int temperature, int season, float r, float g, float b, float a, boolean exterior) {
        if (exterior) {
            this.seasonColorDusk.setColorExterior(temperature, season, r, g, b, a);
        } else {
            this.seasonColorDusk.setColorInterior(temperature, season, r, g, b, a);
        }
    }

    public ClimateColorInfo getSeasonColor(int segment, int temperature, int season) {
        SeasonColor s = null;
        if (segment == 0) {
            s = this.seasonColorDawn;
        } else if (segment == 1) {
            s = this.seasonColorDay;
        } else if (segment == 2) {
            s = this.seasonColorDusk;
        }
        if (s != null) {
            return s.getColor(temperature, season);
        }
        return null;
    }

    private void initSeasonColors() {
        SeasonColor s = new SeasonColor();
        s.setIgnoreNormal(true);
        this.seasonColorDawn = s;
        s = new SeasonColor();
        s.setIgnoreNormal(true);
        this.seasonColorDay = s;
        s = new SeasonColor();
        s.setIgnoreNormal(false);
        this.seasonColorDusk = s;
    }

    public void save(DataOutputStream output) throws IOException {
        if (!GameClient.client || GameServer.server) {
            output.writeByte(1);
            output.writeDouble(this.simplexOffsetA);
            output.writeDouble(this.simplexOffsetB);
            output.writeDouble(this.simplexOffsetC);
            output.writeDouble(this.simplexOffsetD);
            this.currentFront.save(output);
            output.writeFloat(this.snowFracNow);
            output.writeFloat(this.snowStrength);
            output.writeBoolean(this.canDoWinterSprites);
            output.writeBoolean(this.dayDoFog);
            output.writeFloat(this.dayFogStrength);
        } else {
            output.writeByte(0);
        }
        this.weatherPeriod.save(output);
        this.thunderStorm.save(output);
        if (GameServer.server) {
            this.desaturation.saveAdmin(output);
            this.globalLightIntensity.saveAdmin(output);
            this.nightStrength.saveAdmin(output);
            this.precipitationIntensity.saveAdmin(output);
            this.temperature.saveAdmin(output);
            this.fogIntensity.saveAdmin(output);
            this.windIntensity.saveAdmin(output);
            this.windAngleIntensity.saveAdmin(output);
            this.cloudIntensity.saveAdmin(output);
            this.ambient.saveAdmin(output);
            this.viewDistance.saveAdmin(output);
            this.dayLightStrength.saveAdmin(output);
            this.globalLight.saveAdmin(output);
            this.precipitationIsSnow.saveAdmin(output);
        }
        if (this.modDataTable != null) {
            output.writeByte(1);
            this.modDataTable.save(output);
        } else {
            output.writeByte(0);
        }
        if (GameServer.server) {
            this.humidity.saveAdmin(output);
        }
    }

    public void load(DataInputStream input, int worldVersion) throws IOException {
        boolean hasstuff;
        boolean bl = hasstuff = input.readByte() == 1;
        if (hasstuff) {
            this.simplexOffsetA = input.readDouble();
            this.simplexOffsetB = input.readDouble();
            this.simplexOffsetC = input.readDouble();
            this.simplexOffsetD = input.readDouble();
            this.currentFront.load(input);
            this.snowFracNow = input.readFloat();
            this.snowStrength = input.readFloat();
            this.canDoWinterSprites = input.readBoolean();
            this.dayDoFog = input.readBoolean();
            this.dayFogStrength = input.readFloat();
        }
        this.weatherPeriod.load(input, worldVersion);
        this.thunderStorm.load(input);
        if (GameServer.server) {
            this.desaturation.loadAdmin(input, worldVersion);
            this.globalLightIntensity.loadAdmin(input, worldVersion);
            this.nightStrength.loadAdmin(input, worldVersion);
            this.precipitationIntensity.loadAdmin(input, worldVersion);
            this.temperature.loadAdmin(input, worldVersion);
            this.fogIntensity.loadAdmin(input, worldVersion);
            this.windIntensity.loadAdmin(input, worldVersion);
            this.windAngleIntensity.loadAdmin(input, worldVersion);
            this.cloudIntensity.loadAdmin(input, worldVersion);
            this.ambient.loadAdmin(input, worldVersion);
            this.viewDistance.loadAdmin(input, worldVersion);
            this.dayLightStrength.loadAdmin(input, worldVersion);
            this.globalLight.loadAdmin(input, worldVersion);
            this.precipitationIsSnow.loadAdmin(input, worldVersion);
        }
        if (input.readByte() == 1) {
            if (this.modDataTable == null) {
                this.modDataTable = LuaManager.platform.newTable();
            }
            this.modDataTable.load(input, worldVersion);
        }
        if (GameServer.server) {
            this.humidity.loadAdmin(input, worldVersion);
        }
        this.climateValues = new ClimateValues(this);
    }

    public void postCellLoadSetSnow() {
        IsoWorld.instance.currentCell.setSnowTarget((int)(this.snowFracNow * 100.0f));
        ErosionIceQueen.instance.setSnow(this.canDoWinterSprites && this.snowFracNow > 0.2f);
    }

    public void forceDayInfoUpdate() {
        this.currentDay.day = -1;
        this.currentDay.month = -1;
        this.currentDay.year = -1;
        this.gt = GameTime.getInstance();
        this.updateDayInfo(this.gt.getDayPlusOne(), this.gt.getMonth(), this.gt.getYear());
        this.currentDay.hour = this.gt.getHour();
        this.currentDay.minutes = this.gt.getMinutes();
    }

    private void updateDayInfo(int day, int month, int year) {
        this.tickIsDayChange = false;
        if (this.currentDay == null || this.currentDay.day != day || this.currentDay.month != month || this.currentDay.year != year) {
            boolean bl = this.tickIsDayChange = this.currentDay != null;
            if (this.currentDay == null) {
                this.currentDay = new DayInfo();
            }
            this.setDayInfo(this.currentDay, day, month, year, 0);
            if (this.previousDay == null) {
                this.previousDay = new DayInfo();
                this.previousDay.season = this.season.clone();
            }
            this.setDayInfo(this.previousDay, day, month, year, -1);
            if (this.nextDay == null) {
                this.nextDay = new DayInfo();
                this.nextDay.season = this.season.clone();
            }
            this.setDayInfo(this.nextDay, day, month, year, 1);
        }
    }

    protected void setDayInfo(DayInfo dayInfo, int day, int month, int year, int dayOffset) {
        dayInfo.calendar = new GregorianCalendar(year, month, day, 0, 0);
        dayInfo.calendar.add(5, dayOffset);
        dayInfo.day = dayInfo.calendar.get(5);
        dayInfo.month = dayInfo.calendar.get(2);
        dayInfo.year = dayInfo.calendar.get(1);
        dayInfo.dateValue = dayInfo.calendar.getTime().getTime();
        if (dayInfo.season == null) {
            dayInfo.season = this.season.clone();
        }
        dayInfo.season.setDay(dayInfo.day, dayInfo.month, dayInfo.year);
    }

    protected final void transmitClimatePacket(ClimateNetAuth auth, byte type, UdpConnection ignoreConnection) {
        if (!GameClient.client && !GameServer.server) {
            return;
        }
        if (auth == ClimateNetAuth.Denied) {
            DebugLog.log("Denied ClimatePacket, id = " + type + ", isClient = " + GameClient.client);
            return;
        }
        if (GameClient.client && (auth == ClimateNetAuth.ClientOnly || auth == ClimateNetAuth.ClientAndServer)) {
            try {
                if (this.writePacketContents(GameClient.connection, type)) {
                    PacketTypes.PacketType.ClimateManagerPacket.send(GameClient.connection);
                } else {
                    GameClient.connection.cancelPacket();
                }
            }
            catch (Exception e) {
                DebugLog.log(e.getMessage());
            }
        }
        if (GameServer.server && (auth == ClimateNetAuth.ServerOnly || auth == ClimateNetAuth.ClientAndServer)) {
            try {
                for (int n = 0; n < GameServer.udpEngine.connections.size(); ++n) {
                    UdpConnection c = GameServer.udpEngine.connections.get(n);
                    if (ignoreConnection != null && ignoreConnection == c) continue;
                    if (this.writePacketContents(c, type)) {
                        PacketTypes.PacketType.ClimateManagerPacket.send(c);
                        continue;
                    }
                    c.cancelPacket();
                }
            }
            catch (Exception e) {
                DebugLog.log(e.getMessage());
            }
        }
    }

    private boolean writePacketContents(IConnection connection, byte type) throws IOException {
        if (!GameClient.client && !GameServer.server) {
            return false;
        }
        ByteBufferWriter bbw = connection.startPacket();
        PacketTypes.PacketType.ClimateManagerPacket.doPacket(bbw);
        ByteBufferWriter output = bbw;
        output.putByte(type);
        switch (type) {
            case 0: {
                int i;
                for (i = 0; i < this.climateFloats.length; ++i) {
                    output.putFloat(this.climateFloats[i].finalValue);
                }
                for (i = 0; i < this.climateColors.length; ++i) {
                    this.climateColors[i].finalValue.write(output);
                }
                for (i = 0; i < this.climateBooleans.length; ++i) {
                    output.putBoolean(this.climateBooleans[i].finalValue);
                }
                output.putFloat(this.airMass);
                output.putFloat(this.airMassDaily);
                output.putFloat(this.airMassTemperature);
                output.putFloat(this.snowFracNow);
                output.putFloat(this.snowStrength);
                output.putFloat(this.windPower);
                output.putBoolean(this.dayDoFog);
                output.putFloat(this.dayFogStrength);
                output.putBoolean(this.canDoWinterSprites);
                this.weatherPeriod.writeNetWeatherData(output);
                return true;
            }
            case 1: {
                this.weatherPeriod.writeNetWeatherData(output);
                return true;
            }
            case 2: {
                this.thunderStorm.writeNetThunderEvent(output);
                return true;
            }
            case 3: {
                return true;
            }
            case 5: {
                if (!GameClient.client) {
                    return false;
                }
                output.putByte(1);
                return true;
            }
            case 6: {
                int i;
                if (!GameClient.client) {
                    return false;
                }
                for (i = 0; i < this.climateFloats.length; ++i) {
                    this.climateFloats[i].writeAdmin(output);
                }
                for (i = 0; i < this.climateColors.length; ++i) {
                    this.climateColors[i].writeAdmin(output);
                }
                for (i = 0; i < this.climateBooleans.length; ++i) {
                    this.climateBooleans[i].writeAdmin(output);
                }
                return true;
            }
            case 4: {
                int i;
                if (!GameServer.server) {
                    return false;
                }
                for (i = 0; i < this.climateFloats.length; ++i) {
                    this.climateFloats[i].writeAdmin(output);
                }
                for (i = 0; i < this.climateColors.length; ++i) {
                    this.climateColors[i].writeAdmin(output);
                }
                for (i = 0; i < this.climateBooleans.length; ++i) {
                    this.climateBooleans[i].writeAdmin(output);
                }
                return true;
            }
            case 7: {
                if (!GameClient.client) {
                    return false;
                }
                output.putBoolean(this.netInfo.isStopWeather);
                output.putBoolean(this.netInfo.isTrigger);
                output.putBoolean(this.netInfo.isGenerate);
                output.putFloat(this.netInfo.triggerDuration);
                output.putBoolean(this.netInfo.triggerStorm);
                output.putBoolean(this.netInfo.triggerTropical);
                output.putBoolean(this.netInfo.triggerBlizzard);
                output.putFloat(this.netInfo.generateStrength);
                output.putInt(this.netInfo.generateFront);
                return true;
            }
        }
        return false;
    }

    public final void receiveClimatePacket(ByteBufferReader bb, UdpConnection ignoreConnection) throws IOException {
        if (!GameClient.client && !GameServer.server) {
            return;
        }
        byte packetType = bb.getByte();
        this.readPacketContents(bb, packetType, ignoreConnection);
    }

    private boolean readPacketContents(ByteBufferReader input, byte type, UdpConnection ignoreConnection) throws IOException {
        switch (type) {
            case 0: {
                if (!GameClient.client) {
                    return false;
                }
                for (int i = 0; i < this.climateFloats.length; ++i) {
                    ClimateFloat fo = this.climateFloats[i];
                    fo.internalValue = fo.finalValue;
                    fo.setOverride(input.getFloat(), 0.0f);
                }
                for (int i = 0; i < this.climateColors.length; ++i) {
                    ClimateColor co = this.climateColors[i];
                    co.internalValue.setTo(co.finalValue);
                    co.setOverride(input, 0.0f);
                }
                for (int i = 0; i < this.climateBooleans.length; ++i) {
                    ClimateBool bo = this.climateBooleans[i];
                    bo.setOverride(input.getBoolean());
                }
                this.airMass = input.getFloat();
                this.airMassDaily = input.getFloat();
                this.airMassTemperature = input.getFloat();
                this.snowFracNow = input.getFloat();
                this.snowStrength = input.getFloat();
                this.windPower = input.getFloat();
                this.dayDoFog = input.getBoolean();
                this.dayFogStrength = input.getFloat();
                this.canDoWinterSprites = input.getBoolean();
                long curtime = System.currentTimeMillis();
                if ((float)(curtime - this.networkUpdateStamp) < this.networkLerpTime) {
                    this.networkAdjustVal += 1.0f;
                    if (this.networkAdjustVal > 10.0f) {
                        this.networkAdjustVal = 10.0f;
                    }
                } else {
                    this.networkAdjustVal -= 1.0f;
                    if (this.networkAdjustVal < 0.0f) {
                        this.networkAdjustVal = 0.0f;
                    }
                }
                this.networkLerpTime = this.networkAdjustVal > 0.0f ? 5000.0f / this.networkAdjustVal : 5000.0f;
                this.networkUpdateStamp = curtime;
                this.weatherPeriod.readNetWeatherData(input);
                return true;
            }
            case 1: {
                this.weatherPeriod.readNetWeatherData(input);
                return true;
            }
            case 2: {
                this.thunderStorm.readNetThunderEvent(input);
                return true;
            }
            case 3: {
                return true;
            }
            case 5: {
                if (!GameServer.server) {
                    return false;
                }
                input.getByte();
                this.transmitClimatePacket(ClimateNetAuth.ServerOnly, (byte)4, null);
                return true;
            }
            case 6: {
                int i;
                if (!GameServer.server) {
                    return false;
                }
                for (i = 0; i < this.climateFloats.length; ++i) {
                    this.climateFloats[i].readAdmin(input);
                }
                for (i = 0; i < this.climateColors.length; ++i) {
                    this.climateColors[i].readAdmin(input);
                }
                for (i = 0; i < this.climateBooleans.length; ++i) {
                    this.climateBooleans[i].readAdmin(input);
                    if (i != 0) continue;
                    DebugLog.log("Snow = " + this.climateBooleans[i].adminValue + ", enabled = " + this.climateBooleans[i].isAdminOverride);
                }
                this.serverReceiveClientChangeAdminVars();
                return true;
            }
            case 4: {
                int i;
                if (!GameClient.client) {
                    return false;
                }
                for (i = 0; i < this.climateFloats.length; ++i) {
                    this.climateFloats[i].readAdmin(input);
                }
                for (i = 0; i < this.climateColors.length; ++i) {
                    this.climateColors[i].readAdmin(input);
                }
                for (i = 0; i < this.climateBooleans.length; ++i) {
                    this.climateBooleans[i].readAdmin(input);
                }
                return true;
            }
            case 7: {
                if (!GameServer.server) {
                    return false;
                }
                this.netInfo.isStopWeather = input.getBoolean();
                this.netInfo.isTrigger = input.getBoolean();
                this.netInfo.isGenerate = input.getBoolean();
                this.netInfo.triggerDuration = input.getFloat();
                this.netInfo.triggerStorm = input.getBoolean();
                this.netInfo.triggerTropical = input.getBoolean();
                this.netInfo.triggerBlizzard = input.getBoolean();
                this.netInfo.generateStrength = input.getFloat();
                this.netInfo.generateFront = input.getInt();
                this.serverReceiveClientChangeWeather();
                return true;
            }
        }
        return false;
    }

    private void serverReceiveClientChangeAdminVars() {
        if (!GameServer.server) {
            return;
        }
        this.transmitClimatePacket(ClimateNetAuth.ServerOnly, (byte)4, null);
        this.updateOnTick();
        this.transmitClimatePacket(ClimateNetAuth.ServerOnly, (byte)0, null);
    }

    private void serverReceiveClientChangeWeather() {
        if (!GameServer.server) {
            return;
        }
        if (this.netInfo.isStopWeather) {
            this.stopWeatherAndThunder();
        } else if (this.netInfo.isTrigger) {
            this.stopWeatherAndThunder();
            if (this.netInfo.triggerStorm) {
                this.triggerCustomWeatherStage(3, this.netInfo.triggerDuration);
            } else if (this.netInfo.triggerTropical) {
                this.triggerCustomWeatherStage(8, this.netInfo.triggerDuration);
            } else if (this.netInfo.triggerBlizzard) {
                this.triggerCustomWeatherStage(7, this.netInfo.triggerDuration);
            }
        } else if (this.netInfo.isGenerate) {
            this.stopWeatherAndThunder();
            this.triggerCustomWeather(this.netInfo.generateStrength, this.netInfo.generateFront == 0);
        }
        this.updateOnTick();
        this.transmitClimatePacket(ClimateNetAuth.ServerOnly, (byte)0, null);
    }

    public void transmitServerStopWeather() {
        if (!GameServer.server) {
            return;
        }
        this.stopWeatherAndThunder();
        this.updateOnTick();
        this.transmitClimatePacket(ClimateNetAuth.ServerOnly, (byte)0, null);
    }

    public void transmitServerTriggerStorm(float duration) {
        if (!GameServer.server) {
            return;
        }
        this.netInfo.triggerDuration = duration;
        this.triggerCustomWeatherStage(3, this.netInfo.triggerDuration);
        this.updateOnTick();
        this.transmitClimatePacket(ClimateNetAuth.ServerOnly, (byte)0, null);
    }

    public void transmitServerTriggerLightning(int x, int y, boolean doStrike, boolean doLightning, boolean doRumble) {
        if (!GameServer.server) {
            return;
        }
        this.thunderStorm.triggerThunderEvent(x, y, doStrike, doLightning, doRumble);
    }

    public void transmitServerStartRain(float intensity) {
        if (!GameServer.server) {
            return;
        }
        this.precipitationIntensity.setAdminValue(ClimateManager.clamp01(intensity));
        this.precipitationIntensity.setEnableAdmin(true);
        this.updateOnTick();
        this.transmitClimatePacket(ClimateNetAuth.ServerOnly, (byte)0, null);
    }

    public void transmitServerStopRain() {
        if (!GameServer.server) {
            return;
        }
        this.precipitationIntensity.setEnableAdmin(false);
        this.updateOnTick();
        this.transmitClimatePacket(ClimateNetAuth.ServerOnly, (byte)0, null);
    }

    public void transmitRequestAdminVars() {
        this.transmitClimatePacket(ClimateNetAuth.ClientOnly, (byte)5, null);
    }

    public void transmitClientChangeAdminVars() {
        this.transmitClimatePacket(ClimateNetAuth.ClientOnly, (byte)6, null);
    }

    public void transmitStopWeather() {
        this.netInfo.reset();
        this.netInfo.isStopWeather = true;
        this.transmitClimatePacket(ClimateNetAuth.ClientOnly, (byte)7, null);
    }

    public void transmitTriggerStorm(float duration) {
        this.netInfo.reset();
        this.netInfo.isTrigger = true;
        this.netInfo.triggerStorm = true;
        this.netInfo.triggerDuration = duration;
        this.transmitClimatePacket(ClimateNetAuth.ClientOnly, (byte)7, null);
    }

    public void transmitTriggerTropical(float duration) {
        this.netInfo.reset();
        this.netInfo.isTrigger = true;
        this.netInfo.triggerTropical = true;
        this.netInfo.triggerDuration = duration;
        this.transmitClimatePacket(ClimateNetAuth.ClientOnly, (byte)7, null);
    }

    public void transmitTriggerBlizzard(float duration) {
        this.netInfo.reset();
        this.netInfo.isTrigger = true;
        this.netInfo.triggerBlizzard = true;
        this.netInfo.triggerDuration = duration;
        this.transmitClimatePacket(ClimateNetAuth.ClientOnly, (byte)7, null);
    }

    public void transmitGenerateWeather(float strength, int front) {
        this.netInfo.reset();
        this.netInfo.isGenerate = true;
        this.netInfo.generateStrength = ClimateManager.clamp01(strength);
        this.netInfo.generateFront = front;
        if (this.netInfo.generateFront < 0 || this.netInfo.generateFront > 1) {
            this.netInfo.generateFront = 0;
        }
        this.transmitClimatePacket(ClimateNetAuth.ClientOnly, (byte)7, null);
    }

    protected float getTimeLerpHours(float cur, float min, float max) {
        return this.getTimeLerpHours(cur, min, max, false);
    }

    protected float getTimeLerpHours(float cur, float min, float max, boolean doClerp) {
        return this.getTimeLerp(ClimateManager.clamp(0.0f, 1.0f, cur / 24.0f), ClimateManager.clamp(0.0f, 1.0f, min / 24.0f), ClimateManager.clamp(0.0f, 1.0f, max / 24.0f), doClerp);
    }

    protected float getTimeLerp(float cur, float min, float max) {
        return this.getTimeLerp(cur, min, max, false);
    }

    protected float getTimeLerp(float cur, float min, float max, boolean doClerp) {
        float len;
        float mid;
        float minoffset;
        boolean adjust;
        boolean bl = adjust = min > max;
        if (!adjust) {
            if (cur < min || cur > max) {
                return 0.0f;
            }
            float c = cur - min;
            float len2 = max - min;
            float mid2 = len2 * 0.5f;
            if (c < mid2) {
                return doClerp ? ClimateManager.clerp(c / mid2, 0.0f, 1.0f) : ClimateManager.lerp(c / mid2, 0.0f, 1.0f);
            }
            return doClerp ? ClimateManager.clerp((c - mid2) / mid2, 1.0f, 0.0f) : ClimateManager.lerp((c - mid2) / mid2, 1.0f, 0.0f);
        }
        if (cur < min && cur > max) {
            return 0.0f;
        }
        float c = cur >= min ? cur - min : cur + minoffset;
        if (c < (mid = (len = max + (minoffset = 1.0f - min)) * 0.5f)) {
            return doClerp ? ClimateManager.clerp(c / mid, 0.0f, 1.0f) : ClimateManager.lerp(c / mid, 0.0f, 1.0f);
        }
        return doClerp ? ClimateManager.clerp((c - mid) / mid, 1.0f, 0.0f) : ClimateManager.lerp((c - mid) / mid, 1.0f, 0.0f);
    }

    public static float clamp01(float val) {
        return ClimateManager.clamp(0.0f, 1.0f, val);
    }

    public static float clamp(float min, float max, float val) {
        val = Math.min(max, val);
        val = Math.max(min, val);
        return val;
    }

    public static int clamp(int min, int max, int val) {
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

    public static float normalizeRange(float v, float n) {
        return ClimateManager.clamp(0.0f, 1.0f, v / n);
    }

    public static float posToPosNegRange(float v) {
        if (v > 0.5f) {
            return (v - 0.5f) * 2.0f;
        }
        if (v < 0.5f) {
            return -((0.5f - v) * 2.0f);
        }
        return 0.0f;
    }

    public void execute_Simulation() {
        if (Core.debug) {
            ClimMngrDebug dbg = new ClimMngrDebug();
            int days = 365;
            int simulations = 5000;
            dbg.SimulateDays(365, 5000);
        }
    }

    public void execute_Simulation(int rainModOverride) {
        if (Core.debug) {
            ClimMngrDebug dbg = new ClimMngrDebug();
            dbg.setRainModOverride(rainModOverride);
            int days = 365;
            int simulations = 5000;
            dbg.SimulateDays(365, 5000);
        }
    }

    public void triggerKateBobIntroStorm(int centerX, int centerY, double duration, float strength, float initialProgress, float angle, float initialPuddles) {
        this.triggerKateBobIntroStorm(centerX, centerY, duration, strength, initialProgress, angle, initialPuddles, null);
    }

    public void triggerKateBobIntroStorm(int centerX, int centerY, double duration, float strength, float initialProgress, float angle, float initialPuddles, ClimateColorInfo cloudcolor) {
        if (!GameClient.client) {
            this.stopWeatherAndThunder();
            if (this.weatherPeriod.startCreateModdedPeriod(true, strength, angle)) {
                this.weatherPeriod.setKateBobStormProgress(initialProgress);
                this.weatherPeriod.setKateBobStormCoords(centerX, centerY);
                this.weatherPeriod.createAndAddStage(11, duration);
                this.weatherPeriod.createAndAddStage(2, duration / 2.0);
                this.weatherPeriod.createAndAddStage(4, duration / 4.0);
                this.weatherPeriod.endCreateModdedPeriod();
                if (cloudcolor != null) {
                    this.weatherPeriod.setCloudColor(cloudcolor);
                } else {
                    this.weatherPeriod.setCloudColor(this.weatherPeriod.getCloudColorBlueish());
                }
                IsoPuddles.PuddlesFloat pfloat = IsoPuddles.getInstance().getPuddlesFloat(3);
                pfloat.setFinalValue(initialPuddles);
                pfloat = IsoPuddles.getInstance().getPuddlesFloat(1);
                pfloat.setFinalValue(PZMath.clamp_01(initialPuddles * 1.2f));
            }
        }
    }

    public double getSimplexOffsetA() {
        return this.simplexOffsetA;
    }

    public double getSimplexOffsetB() {
        return this.simplexOffsetB;
    }

    public double getSimplexOffsetC() {
        return this.simplexOffsetC;
    }

    public double getSimplexOffsetD() {
        return this.simplexOffsetD;
    }

    public double getWorldAgeHours() {
        return this.worldAgeHours;
    }

    public ClimateValues getClimateValuesCopy() {
        return this.climateValues.getCopy();
    }

    public void CopyClimateValues(ClimateValues copy) {
        this.climateValues.CopyValues(copy);
    }

    public ClimateForecaster getClimateForecaster() {
        return this.climateForecaster;
    }

    public ClimateHistory getClimateHistory() {
        return this.climateHistory;
    }

    public void CalculateWeatherFrontStrength(int year, int month, int day, AirFront front) {
        GregorianCalendar calendar = new GregorianCalendar(year, month, day, 0, 0);
        calendar.add(5, -3);
        if (this.climateValuesFronts == null) {
            this.climateValuesFronts = this.climateValues.getCopy();
        }
        int targetType = front.type;
        for (int i = 0; i < 4; ++i) {
            int type;
            this.climateValuesFronts.pollDate(calendar);
            float airmass = this.climateValuesFronts.getAirFrontAirmass();
            int n = type = airmass < 0.0f ? -1 : 1;
            if (type == targetType) {
                front.addDaySample(airmass);
            }
            calendar.add(5, 1);
        }
    }

    public static String getWindAngleString(float angle) {
        for (int i = 0; i < windAngles.length; ++i) {
            if (!(angle < windAngles[i])) continue;
            return windAngleStr[i];
        }
        return windAngleStr[windAngleStr.length - 1];
    }

    public void sendInitialState(IConnection connection) throws IOException {
        if (!GameServer.server) {
            return;
        }
        if (this.writePacketContents(connection, (byte)0)) {
            PacketTypes.PacketType.ClimateManagerPacket.send(connection);
        } else {
            connection.cancelPacket();
        }
    }

    public boolean isUpdated() {
        return this.lastMinuteStamp != -1L;
    }

    public void Reset() {
        this.lastHourStamp = -1;
        this.lastMinuteStamp = -1L;
        if (this.currentDay != null) {
            this.currentDay.year = -1;
            this.currentDay.month = -1;
            this.currentDay.day = -1;
        }
    }

    static {
        windAngles = new float[]{22.5f, 67.5f, 112.5f, 157.5f, 202.5f, 247.5f, 292.5f, 337.5f, 382.5f};
        windAngleStr = new String[]{"SE", "S", "SW", "W", "NW", "N", "NE", "E", "SE"};
    }

    @UsedFromLua
    public static class AirFront {
        private float days;
        private float maxNoise;
        private float totalNoise;
        private int type = 0;
        private float strength;
        private float tmpNoiseAbs;
        private final float[] noiseCache = new float[2];
        private float noiseCacheValue;
        private float frontWindAngleDegrees;

        public float getDays() {
            return this.days;
        }

        public float getMaxNoise() {
            return this.maxNoise;
        }

        public float getTotalNoise() {
            return this.totalNoise;
        }

        public int getType() {
            return this.type;
        }

        public float getStrength() {
            return this.strength;
        }

        public float getAngleDegrees() {
            return this.frontWindAngleDegrees;
        }

        public AirFront() {
            this.reset();
        }

        public void setFrontType(int type) {
            this.reset();
            this.type = type;
        }

        protected void setFrontWind(float windangledegrees) {
            this.frontWindAngleDegrees = windangledegrees;
        }

        public void setStrength(float str) {
            this.strength = str;
        }

        protected void reset() {
            this.days = 0.0f;
            this.maxNoise = 0.0f;
            this.totalNoise = 0.0f;
            this.type = 0;
            this.strength = 0.0f;
            this.frontWindAngleDegrees = 0.0f;
            for (int i = 0; i < this.noiseCache.length; ++i) {
                this.noiseCache[i] = -1.0f;
            }
        }

        public void save(DataOutputStream output) throws IOException {
            output.writeFloat(this.days);
            output.writeFloat(this.maxNoise);
            output.writeFloat(this.totalNoise);
            output.writeInt(this.type);
            output.writeFloat(this.strength);
            output.writeFloat(this.frontWindAngleDegrees);
            output.writeInt(this.noiseCache.length);
            for (int i = 0; i < this.noiseCache.length; ++i) {
                output.writeFloat(this.noiseCache[i]);
            }
        }

        public void load(DataInputStream input) throws IOException {
            this.days = input.readFloat();
            this.maxNoise = input.readFloat();
            this.totalNoise = input.readFloat();
            this.type = input.readInt();
            this.strength = input.readFloat();
            this.frontWindAngleDegrees = input.readFloat();
            int len = input.readInt();
            int max = len > this.noiseCache.length ? len : this.noiseCache.length;
            for (int i = 0; i < max; ++i) {
                if (i < len) {
                    float val = input.readFloat();
                    if (i >= this.noiseCache.length) continue;
                    this.noiseCache[i] = val;
                    continue;
                }
                if (i >= this.noiseCache.length) continue;
                this.noiseCache[i] = -1.0f;
            }
        }

        public void addDaySample(float noiseval) {
            this.days += 1.0f;
            if (this.type == 1 && noiseval <= 0.0f || this.type == -1 && noiseval >= 0.0f) {
                this.strength = 0.0f;
                return;
            }
            this.tmpNoiseAbs = Math.abs(noiseval);
            if (this.tmpNoiseAbs > this.maxNoise) {
                this.maxNoise = this.tmpNoiseAbs;
            }
            this.totalNoise += this.tmpNoiseAbs;
            this.noiseCacheValue = 0.0f;
            for (int i = this.noiseCache.length - 1; i >= 0; --i) {
                if (this.noiseCache[i] > this.noiseCacheValue) {
                    this.noiseCacheValue = this.noiseCache[i];
                }
                if (i >= this.noiseCache.length - 1) continue;
                this.noiseCache[i + 1] = this.noiseCache[i];
            }
            this.noiseCache[0] = this.tmpNoiseAbs;
            if (this.tmpNoiseAbs > this.noiseCacheValue) {
                this.noiseCacheValue = this.tmpNoiseAbs;
            }
            this.strength = this.noiseCacheValue * 0.75f + this.maxNoise * 0.25f;
        }

        public void copyFrom(AirFront other) {
            this.days = other.days;
            this.maxNoise = other.maxNoise;
            this.totalNoise = other.totalNoise;
            this.type = other.type;
            this.strength = other.strength;
            this.frontWindAngleDegrees = other.frontWindAngleDegrees;
        }
    }

    @UsedFromLua
    public static class ClimateFloat {
        protected float internalValue;
        protected float finalValue;
        protected boolean isOverride;
        protected float override;
        protected boolean isOverrideValue;
        protected float overrideInternal;
        protected float interpolate;
        private boolean isModded;
        private float moddedValue;
        private float modInterpolate;
        private boolean isAdminOverride;
        private float adminValue;
        private float min;
        private float max = 1.0f;
        private int id;
        private String name;

        public ClimateFloat init(int id, String name) {
            this.id = id;
            this.name = name;
            return this;
        }

        public int getID() {
            return this.id;
        }

        public String getName() {
            return this.name;
        }

        public float getMin() {
            return this.min;
        }

        public float getMax() {
            return this.max;
        }

        public float getInternalValue() {
            return this.internalValue;
        }

        public float getOverride() {
            return this.override;
        }

        public float getOverrideInterpolate() {
            return this.interpolate;
        }

        public void setOverride(float targ, float inter) {
            this.override = targ;
            this.interpolate = inter;
            this.isOverride = true;
        }

        public void setOverrideValue(boolean overrideValue) {
            this.isOverrideValue = overrideValue;
            this.isOverride = overrideValue;
        }

        public void setEnableOverride(boolean b) {
            this.isOverride = b;
        }

        public boolean isEnableOverride() {
            return this.isOverride;
        }

        public void setEnableAdmin(boolean b) {
            this.isAdminOverride = b;
        }

        public boolean isEnableAdmin() {
            return this.isAdminOverride;
        }

        public void setAdminValue(float f) {
            this.adminValue = ClimateManager.clamp(this.min, this.max, f);
        }

        public float getAdminValue() {
            return this.adminValue;
        }

        public void setEnableModded(boolean b) {
            this.isModded = b;
        }

        public void setModdedValue(float f) {
            this.moddedValue = ClimateManager.clamp(this.min, this.max, f);
        }

        public float getModdedValue() {
            return this.moddedValue;
        }

        public void setModdedInterpolate(float f) {
            this.modInterpolate = ClimateManager.clamp01(f);
        }

        public void setFinalValue(float f) {
            this.finalValue = f;
        }

        public float getFinalValue() {
            return this.finalValue;
        }

        private void calculate() {
            if (this.isAdminOverride && !GameClient.client) {
                this.finalValue = this.adminValue;
                return;
            }
            if (this.isModded && this.modInterpolate > 0.0f) {
                this.internalValue = ClimateManager.lerp(this.modInterpolate, this.internalValue, this.moddedValue);
            }
            this.finalValue = this.isOverride && this.interpolate > 0.0f ? (this.isOverrideValue ? ClimateManager.lerp(this.interpolate, this.overrideInternal, this.override) : ClimateManager.lerp(this.interpolate, this.internalValue, this.override)) : this.internalValue;
        }

        private void writeAdmin(ByteBufferWriter output) {
            output.putBoolean(this.isAdminOverride);
            output.putFloat(this.adminValue);
        }

        private void readAdmin(ByteBufferReader input) {
            this.isAdminOverride = input.getBoolean();
            this.adminValue = input.getFloat();
        }

        private void saveAdmin(DataOutputStream output) throws IOException {
            output.writeBoolean(this.isAdminOverride);
            output.writeFloat(this.adminValue);
        }

        private void loadAdmin(DataInputStream input, int worldVersion) throws IOException {
            this.isAdminOverride = input.readBoolean();
            this.adminValue = input.readFloat();
        }
    }

    @UsedFromLua
    public static class ClimateColor {
        protected ClimateColorInfo internalValue = new ClimateColorInfo();
        protected ClimateColorInfo finalValue = new ClimateColorInfo();
        protected boolean isOverride;
        protected ClimateColorInfo override = new ClimateColorInfo();
        protected float interpolate;
        private boolean isModded;
        private final ClimateColorInfo moddedValue = new ClimateColorInfo();
        private float modInterpolate;
        private boolean isAdminOverride;
        private final ClimateColorInfo adminValue = new ClimateColorInfo();
        private int id;
        private String name;

        public ClimateColor init(int id, String name) {
            this.id = id;
            this.name = name;
            return this;
        }

        public int getID() {
            return this.id;
        }

        public String getName() {
            return this.name;
        }

        public ClimateColorInfo getInternalValue() {
            return this.internalValue;
        }

        public ClimateColorInfo getOverride() {
            return this.override;
        }

        public float getOverrideInterpolate() {
            return this.interpolate;
        }

        public void setOverride(ClimateColorInfo targ, float inter) {
            this.override.setTo(targ);
            this.interpolate = inter;
            this.isOverride = true;
        }

        public void setOverride(ByteBufferReader input, float interp) {
            this.override.read(input);
            this.interpolate = interp;
            this.isOverride = true;
        }

        public void setEnableOverride(boolean b) {
            this.isOverride = b;
        }

        public boolean isEnableOverride() {
            return this.isOverride;
        }

        public void setEnableAdmin(boolean b) {
            this.isAdminOverride = b;
        }

        public boolean isEnableAdmin() {
            return this.isAdminOverride;
        }

        public void setAdminValue(float r, float g, float b, float a, float r1, float g1, float b1, float a1) {
            this.adminValue.getExterior().r = r;
            this.adminValue.getExterior().g = g;
            this.adminValue.getExterior().b = b;
            this.adminValue.getExterior().a = a;
            this.adminValue.getInterior().r = r1;
            this.adminValue.getInterior().g = g1;
            this.adminValue.getInterior().b = b1;
            this.adminValue.getInterior().a = a1;
        }

        public void setAdminValueExterior(float r, float g, float b, float a) {
            this.adminValue.getExterior().r = r;
            this.adminValue.getExterior().g = g;
            this.adminValue.getExterior().b = b;
            this.adminValue.getExterior().a = a;
        }

        public void setAdminValueInterior(float r, float g, float b, float a) {
            this.adminValue.getInterior().r = r;
            this.adminValue.getInterior().g = g;
            this.adminValue.getInterior().b = b;
            this.adminValue.getInterior().a = a;
        }

        public void setAdminValue(ClimateColorInfo targ) {
            this.adminValue.setTo(targ);
        }

        public ClimateColorInfo getAdminValue() {
            return this.adminValue;
        }

        public void setEnableModded(boolean b) {
            this.isModded = b;
        }

        public void setModdedValue(ClimateColorInfo targ) {
            this.moddedValue.setTo(targ);
        }

        public ClimateColorInfo getModdedValue() {
            return this.moddedValue;
        }

        public void setModdedInterpolate(float f) {
            this.modInterpolate = ClimateManager.clamp01(f);
        }

        public void setFinalValue(ClimateColorInfo targ) {
            this.finalValue.setTo(targ);
        }

        public ClimateColorInfo getFinalValue() {
            return this.finalValue;
        }

        private void calculate() {
            if (this.isAdminOverride && !GameClient.client) {
                this.finalValue.setTo(this.adminValue);
                return;
            }
            if (this.isModded && this.modInterpolate > 0.0f) {
                this.internalValue.interp(this.moddedValue, this.modInterpolate, this.internalValue);
            }
            if (this.isOverride && this.interpolate > 0.0f) {
                this.internalValue.interp(this.override, this.interpolate, this.finalValue);
            } else {
                this.finalValue.setTo(this.internalValue);
            }
        }

        private void writeAdmin(ByteBufferWriter output) {
            output.putBoolean(this.isAdminOverride);
            this.adminValue.write(output);
        }

        private void readAdmin(ByteBufferReader input) {
            this.isAdminOverride = input.getBoolean();
            this.adminValue.read(input);
        }

        private void saveAdmin(DataOutputStream output) throws IOException {
            output.writeBoolean(this.isAdminOverride);
            this.adminValue.save(output);
        }

        private void loadAdmin(DataInputStream input, int worldVersion) throws IOException {
            this.isAdminOverride = input.readBoolean();
            this.adminValue.load(input, worldVersion);
        }
    }

    @UsedFromLua
    public static class ClimateBool {
        protected boolean internalValue;
        protected boolean finalValue;
        protected boolean isOverride;
        protected boolean override;
        private boolean isModded;
        private boolean moddedValue;
        private boolean isAdminOverride;
        private boolean adminValue;
        private int id;
        private String name;

        public ClimateBool init(int id, String name) {
            this.id = id;
            this.name = name;
            return this;
        }

        public int getID() {
            return this.id;
        }

        public String getName() {
            return this.name;
        }

        public boolean getInternalValue() {
            return this.internalValue;
        }

        public boolean getOverride() {
            return this.override;
        }

        public void setOverride(boolean b) {
            this.isOverride = true;
            this.override = b;
        }

        public void setEnableOverride(boolean b) {
            this.isOverride = b;
        }

        public boolean isEnableOverride() {
            return this.isOverride;
        }

        public void setEnableAdmin(boolean b) {
            this.isAdminOverride = b;
        }

        public boolean isEnableAdmin() {
            return this.isAdminOverride;
        }

        public void setAdminValue(boolean b) {
            this.adminValue = b;
        }

        public boolean getAdminValue() {
            return this.adminValue;
        }

        public void setEnableModded(boolean b) {
            this.isModded = b;
        }

        public void setModdedValue(boolean b) {
            this.moddedValue = b;
        }

        public boolean getModdedValue() {
            return this.moddedValue;
        }

        public void setFinalValue(boolean b) {
            this.finalValue = b;
        }

        private void calculate() {
            if (this.isAdminOverride && !GameClient.client) {
                this.finalValue = this.adminValue;
                return;
            }
            if (this.isModded) {
                this.finalValue = this.moddedValue;
                return;
            }
            this.finalValue = this.isOverride ? this.override : this.internalValue;
        }

        private void writeAdmin(ByteBufferWriter output) {
            output.putBoolean(this.isAdminOverride);
            output.putBoolean(this.adminValue);
        }

        private void readAdmin(ByteBufferReader input) {
            this.isAdminOverride = input.getBoolean();
            this.adminValue = input.getBoolean();
        }

        private void saveAdmin(DataOutputStream output) throws IOException {
            output.writeBoolean(this.isAdminOverride);
            output.writeBoolean(this.adminValue);
        }

        private void loadAdmin(DataInputStream input, int worldVersion) throws IOException {
            this.isAdminOverride = input.readBoolean();
            this.adminValue = input.readBoolean();
        }
    }

    private static class ClimateNetInfo {
        public boolean isStopWeather;
        public boolean isTrigger;
        public boolean isGenerate;
        public float triggerDuration;
        public boolean triggerStorm;
        public boolean triggerTropical;
        public boolean triggerBlizzard;
        public float generateStrength;
        public int generateFront;

        private ClimateNetInfo() {
        }

        private void reset() {
            this.isStopWeather = false;
            this.isTrigger = false;
            this.isGenerate = false;
            this.triggerDuration = 0.0f;
            this.triggerStorm = false;
            this.triggerTropical = false;
            this.triggerBlizzard = false;
            this.generateStrength = 0.0f;
            this.generateFront = 0;
        }
    }

    @UsedFromLua
    public static class DayInfo {
        public int day;
        public int month;
        public int year;
        public int hour;
        public int minutes;
        public long dateValue;
        public GregorianCalendar calendar;
        public ErosionSeason season;

        public void set(int day, int month, int year) {
            this.calendar = new GregorianCalendar(year, month, day, 0, 0);
            this.dateValue = this.calendar.getTime().getTime();
            this.day = day;
            this.month = month;
            this.year = year;
        }

        public int getDay() {
            return this.day;
        }

        public int getMonth() {
            return this.month;
        }

        public int getYear() {
            return this.year;
        }

        public int getHour() {
            return this.hour;
        }

        public int getMinutes() {
            return this.minutes;
        }

        public long getDateValue() {
            return this.dateValue;
        }

        public ErosionSeason getSeason() {
            return this.season;
        }
    }

    public static enum ClimateNetAuth {
        Denied,
        ClientOnly,
        ServerOnly,
        ClientAndServer;

    }

    protected static class SeasonColor {
        public static final int WARM = 0;
        public static final int NORMAL = 1;
        public static final int CLOUDY = 2;
        public static final int SUMMER = 0;
        public static final int FALL = 1;
        public static final int WINTER = 2;
        public static final int SPRING = 3;
        private final ClimateColorInfo finalCol = new ClimateColorInfo();
        private final ClimateColorInfo[] tempCol = new ClimateColorInfo[3];
        private final ClimateColorInfo[][] colors = new ClimateColorInfo[3][4];
        private boolean ignoreNormal = true;

        public SeasonColor() {
            for (int j = 0; j < 3; ++j) {
                for (int i = 0; i < 4; ++i) {
                    this.colors[j][i] = new ClimateColorInfo();
                }
                this.tempCol[j] = new ClimateColorInfo();
            }
        }

        public void setIgnoreNormal(boolean b) {
            this.ignoreNormal = b;
        }

        public ClimateColorInfo getColor(int temperature, int season) {
            return this.colors[temperature][season];
        }

        public void setColorInterior(int temperature, int season, float r, float g, float b, float a) {
            this.colors[temperature][season].getInterior().r = r;
            this.colors[temperature][season].getInterior().g = g;
            this.colors[temperature][season].getInterior().b = b;
            this.colors[temperature][season].getInterior().a = a;
        }

        public void setColorExterior(int temperature, int season, float r, float g, float b, float a) {
            this.colors[temperature][season].getExterior().r = r;
            this.colors[temperature][season].getExterior().g = g;
            this.colors[temperature][season].getExterior().b = b;
            this.colors[temperature][season].getExterior().a = a;
        }

        public ClimateColorInfo update(float temperatureLerp, float seasonLerp, int seasonFrom, int seasonTo) {
            for (int i = 0; i < 3; ++i) {
                if (this.ignoreNormal && i == 1) continue;
                this.colors[i][seasonFrom].interp(this.colors[i][seasonTo], seasonLerp, this.tempCol[i]);
            }
            if (!this.ignoreNormal) {
                if (temperatureLerp < 0.5f) {
                    float cf = temperatureLerp * 2.0f;
                    this.tempCol[0].interp(this.tempCol[1], cf, this.finalCol);
                } else {
                    float cf = 1.0f - (temperatureLerp - 0.5f) * 2.0f;
                    this.tempCol[2].interp(this.tempCol[1], cf, this.finalCol);
                }
            } else {
                this.tempCol[0].interp(this.tempCol[2], temperatureLerp, this.finalCol);
            }
            return this.finalCol;
        }
    }
}

