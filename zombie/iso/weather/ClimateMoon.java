/*
 * Decompiled with CFR 0.152.
 */
package zombie.iso.weather;

import zombie.UsedFromLua;

@UsedFromLua
public final class ClimateMoon {
    private static final int[] day_year = new int[]{-1, -1, 30, 58, 89, 119, 150, 180, 211, 241, 272, 303, 333};
    private static final String[] moon_phase_name = new String[]{"New", "Waxing crescent", "First quarter", "Waxing gibbous", "Full", "Waning gibbous", "Third quarter", "Waning crescent"};
    private static final float[] units = new float[]{0.0f, 0.25f, 0.5f, 0.75f, 1.0f, 0.75f, 0.5f, 0.25f};
    private int lastYear;
    private int lastMonth;
    private int lastDay;
    private int currentPhase;
    private float currentFloat;
    private static final ClimateMoon instance = new ClimateMoon();

    public static ClimateMoon getInstance() {
        return instance;
    }

    public void updatePhase(int year, int month, int day) {
        if (year != this.lastYear || month != this.lastMonth || day != this.lastDay) {
            this.lastYear = year;
            this.lastMonth = month;
            this.lastDay = day;
            this.currentPhase = this.getMoonPhase(year, month, day);
            if (this.currentPhase > 7) {
                this.currentPhase = 7;
            }
            if (this.currentPhase < 0) {
                this.currentPhase = 0;
            }
            this.currentFloat = units[this.currentPhase];
        }
    }

    public String getPhaseName() {
        return moon_phase_name[this.currentPhase];
    }

    public float getMoonFloat() {
        return this.currentFloat;
    }

    public int getCurrentMoonPhase() {
        return this.currentPhase;
    }

    private int getMoonPhase(int year, int month, int day) {
        int cent;
        int golden;
        int epact;
        if (month < 0 || month > 12) {
            month = 0;
        }
        int diy = day + day_year[month];
        if (month > 2 && this.isLeapYearP(year)) {
            ++diy;
        }
        if ((epact = (11 * (golden = year % 19 + 1) + 20 + (8 * (cent = year / 100 + 1) + 5) / 25 - 5 - (3 * cent / 4 - 12)) % 30) <= 0) {
            epact += 30;
        }
        if (epact == 25 && golden > 11 || epact == 24) {
            ++epact;
        }
        int phase = ((diy + epact) * 6 + 11) % 177 / 22 & 7;
        return phase;
    }

    private int daysInMonth(int month, int year) {
        int result = 31;
        switch (month) {
            case 4: 
            case 6: 
            case 9: 
            case 11: {
                result = 30;
                break;
            }
            case 2: {
                result = this.isLeapYearP(year) ? 29 : 28;
            }
        }
        return result;
    }

    private boolean isLeapYearP(int year) {
        return year % 4 == 0 && (year % 400 == 0 || year % 100 != 0);
    }

    public void Reset() {
        this.currentFloat = 0.0f;
        this.currentPhase = 0;
        this.lastDay = 0;
        this.lastMonth = 0;
        this.lastYear = 0;
    }
}

