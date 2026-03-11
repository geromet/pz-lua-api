/*
 * Decompiled with CFR 0.152.
 */
package zombie.entity.components.fluids;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;
import zombie.UsedFromLua;
import zombie.core.math.PZMath;

@UsedFromLua
public class FluidUtil {
    public static final float UNIT_L = 1.0f;
    public static final float UNIT_dL = 0.1f;
    public static final float UNIT_cL = 0.01f;
    public static final float UNIT_mL = 0.001f;
    public static final float UNIT_dmL = 1.0E-4f;
    public static final float UNIT_cmL = 1.0E-5f;
    public static final float UNIT_uL = 1.0E-6f;
    public static final float MIN_UNIT = 1.0E-4f;
    public static final float MIN_CONTAINER_CAPACITY = 0.05f;
    private static final DecimalFormat df_liter = new DecimalFormat("#.##", DecimalFormatSymbols.getInstance(Locale.ENGLISH));
    private static final DecimalFormat df_liter10 = new DecimalFormat("#.#", DecimalFormatSymbols.getInstance(Locale.ENGLISH));
    private static final DecimalFormat df_liter1000 = new DecimalFormat("#", DecimalFormatSymbols.getInstance(Locale.ENGLISH));
    public static final float TRANSFER_ACTION_TIME_PER_LITER = 40.0f;
    public static final float MIN_TRANSFER_ACTION_TIME = 20.0f;

    public static float getUnitLiter() {
        return 1.0f;
    }

    public static float getUnitDeciLiter() {
        return 0.1f;
    }

    public static float getUnitCentiLiter() {
        return 0.01f;
    }

    public static float getUnitMilliLiter() {
        return 0.001f;
    }

    public static float getUnitDeciMilliLiter() {
        return 1.0E-4f;
    }

    public static float getUnitCentiMilliLiter() {
        return 1.0E-5f;
    }

    public static float getUnitMicroLiter() {
        return 1.0E-6f;
    }

    public static float getMinUnit() {
        return 1.0E-4f;
    }

    public static float getMinContainerCapacity() {
        return 0.05f;
    }

    public static String getAmountFormatted(float amount) {
        if (amount >= 1000.0f) {
            return FluidUtil.getAmountLiter1000(amount);
        }
        if (amount >= 10.0f) {
            return FluidUtil.getAmountLiter10(amount);
        }
        if (amount >= 1.0f) {
            return FluidUtil.getAmountLiter(amount);
        }
        return FluidUtil.getAmountMilli(amount);
    }

    public static String getFractionFormatted(float numerator, float denominator) {
        float amount = PZMath.max(numerator, denominator);
        if (amount >= 1000.0f) {
            return String.format("%s / %s", df_liter1000.format(numerator), df_liter1000.format(denominator) + " L");
        }
        if (amount >= 10.0f) {
            return String.format("%s / %s", df_liter10.format(numerator), df_liter10.format(denominator) + " L");
        }
        if (amount >= 1.0f) {
            return String.format("%s / %s", df_liter.format(numerator), df_liter.format(denominator) + " L");
        }
        return String.format("%s / %s", Math.round(numerator * 1000.0f), Math.round(denominator * 1000.0f) + " mL");
    }

    public static String getAmountLiter1000(float amount) {
        return df_liter1000.format(amount) + " L";
    }

    public static String getAmountLiter10(float amount) {
        return df_liter10.format(amount) + " L";
    }

    public static String getAmountLiter(float amount) {
        return df_liter.format(amount) + " L";
    }

    public static String getAmountMilli(float amount) {
        int ml = Math.round(amount * 1000.0f);
        return ml + " mL";
    }

    public static float roundTransfer(float amount) {
        return (float)Math.round(amount * 100.0f) / 100.0f;
    }

    public static float getTransferActionTimePerLiter() {
        return 40.0f;
    }

    public static float getMinTransferActionTime() {
        return 20.0f;
    }
}

