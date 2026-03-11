/*
 * Decompiled with CFR 0.152.
 */
package zombie.iso.weather;

import zombie.SandboxOptions;
import zombie.UsedFromLua;
import zombie.characters.IsoPlayer;
import zombie.core.Color;
import zombie.core.Core;
import zombie.core.math.PZMath;
import zombie.iso.weather.ClimateManager;

@UsedFromLua
public class Temperature {
    public static final boolean DO_DEFAULT_BASE = false;
    public static final boolean DO_DAYLEN_MOD = true;
    public static final String CELSIUS_POSTFIX = "\u00b0C";
    public static final String FAHRENHEIT_POSTFIX = "\u00b0F";
    public static final float skinCelciusMin = 20.0f;
    public static final float skinCelciusFavorable = 33.0f;
    public static final float skinCelciusMax = 42.0f;
    public static final float homeostasisDefault = 37.0f;
    public static final float FavorableNakedTemp = 27.0f;
    public static final float FavorableRoomTemp = 22.0f;
    public static final float coreCelciusMin = 20.0f;
    public static final float coreCelciusMax = 42.0f;
    public static final float neutralZone = 27.0f;
    public static final float Hypothermia_1 = 36.5f;
    public static final float Hypothermia_2 = 35.0f;
    public static final float Hypothermia_3 = 30.0f;
    public static final float Hypothermia_4 = 25.0f;
    public static final float Hyperthermia_1 = 37.5f;
    public static final float Hyperthermia_2 = 39.0f;
    public static final float Hyperthermia_3 = 40.0f;
    public static final float Hyperthermia_4 = 41.0f;
    public static final float TrueInsulationMultiplier = 2.0f;
    public static final float TrueWindresistMultiplier = 1.0f;
    public static final float BodyMinTemp = 20.0f;
    public static final float BodyMaxTemp = 42.0f;
    private static String cacheTempString = "";
    private static float cacheTemp = -9000.0f;
    private static final Color tempColor = new Color(1.0f, 1.0f, 1.0f, 1.0f);
    private static final Color col_0 = new Color(29, 34, 237);
    private static final Color col_25 = new Color(0, 255, 234);
    private static final Color col_50 = new Color(84, 255, 55);
    private static final Color col_75 = new Color(255, 246, 0);
    private static final Color col_100 = new Color(255, 0, 0);

    public static String getCelsiusPostfix() {
        return CELSIUS_POSTFIX;
    }

    public static String getFahrenheitPostfix() {
        return FAHRENHEIT_POSTFIX;
    }

    public static String getTemperaturePostfix() {
        return Core.getInstance().getOptionTemperatureDisplayCelsius() ? CELSIUS_POSTFIX : FAHRENHEIT_POSTFIX;
    }

    public static String getTemperatureString(float celsius) {
        float v = Core.getInstance().getOptionTemperatureDisplayCelsius() ? celsius : Temperature.CelsiusToFahrenheit(celsius);
        if (cacheTemp != (v = (float)Math.round(v * 10.0f) / 10.0f)) {
            cacheTemp = v;
            cacheTempString = v + " " + Temperature.getTemperaturePostfix();
        }
        return cacheTempString;
    }

    public static int getRoundedDisplayTemperature(float celsius) {
        return Core.getInstance().getOptionTemperatureDisplayCelsius() ? PZMath.roundToInt(celsius) : PZMath.roundToInt(Temperature.CelsiusToFahrenheit(celsius));
    }

    public static float CelsiusToFahrenheit(float celsius) {
        return celsius * 1.8f + 32.0f;
    }

    public static float FahrenheitToCelsius(float fahrenheit) {
        return (fahrenheit - 32.0f) / 1.8f;
    }

    public static float WindchillCelsiusKph(float t, float v) {
        float w = 13.12f + 0.6215f * t - 11.37f * (float)Math.pow(v, 0.16f) + 0.3965f * t * (float)Math.pow(v, 0.16f);
        return w < t ? w : t;
    }

    public static float getTrueInsulationValue(float insulation) {
        return insulation * 2.0f + 0.5f * insulation * insulation * insulation;
    }

    public static float getTrueWindresistanceValue(float windresist) {
        return windresist * 1.0f + 0.5f * windresist * windresist;
    }

    public static void reset() {
    }

    public static float getFractionForRealTimeRatePerMin(float rate) {
        float mod = (float)SandboxOptions.instance.getDayLengthMinutes() / (float)SandboxOptions.instance.getDayLengthMinutesDefault();
        if (mod < 1.0f) {
            mod = 0.5f + 0.5f * mod;
        } else if (mod > 1.0f) {
            mod = 1.0f + mod / 16.0f;
        }
        return rate / (1440.0f / (float)SandboxOptions.instance.getDayLengthMinutes()) * mod;
    }

    public static Color getValueColor(float val) {
        val = ClimateManager.clamp(0.0f, 1.0f, val);
        tempColor.set(0.0f, 0.0f, 0.0f, 1.0f);
        if (val < 0.25f) {
            col_0.interp(col_25, val / 0.25f, tempColor);
        } else if (val < 0.5f) {
            col_25.interp(col_50, (val - 0.25f) / 0.25f, tempColor);
        } else if (val < 0.75f) {
            col_50.interp(col_75, (val - 0.5f) / 0.25f, tempColor);
        } else {
            col_75.interp(col_100, (val - 0.75f) / 0.25f, tempColor);
        }
        return tempColor;
    }

    public static float getWindChillAmountForPlayer(IsoPlayer player) {
        if (player.getVehicle() != null || player.getSquare() != null && player.getSquare().isInARoom()) {
            return 0.0f;
        }
        ClimateManager clim = ClimateManager.getInstance();
        float airTemperature = clim.getAirTemperatureForCharacter(player, true);
        float windChillAmount = 0.0f;
        if (airTemperature < clim.getTemperature()) {
            windChillAmount = clim.getTemperature() - airTemperature;
        }
        return windChillAmount;
    }
}

