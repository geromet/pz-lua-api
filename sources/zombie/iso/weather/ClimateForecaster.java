/*
 * Decompiled with CFR 0.152.
 */
package zombie.iso.weather;

import java.util.ArrayList;
import java.util.GregorianCalendar;
import zombie.GameTime;
import zombie.UsedFromLua;
import zombie.core.Translator;
import zombie.iso.weather.ClimateManager;
import zombie.iso.weather.ClimateValues;
import zombie.iso.weather.WeatherPeriod;

@UsedFromLua
public class ClimateForecaster {
    private static final int OffsetToday = 10;
    private ClimateValues climateValues;
    private final DayForecast[] forecasts = new DayForecast[40];
    private final ArrayList<DayForecast> forecastList = new ArrayList(40);

    public ArrayList<DayForecast> getForecasts() {
        return this.forecastList;
    }

    public DayForecast getForecast() {
        return this.getForecast(0);
    }

    public DayForecast getForecast(int offset) {
        int target = 10 + offset;
        if (target >= 0 && target < this.forecasts.length) {
            return this.forecasts[target];
        }
        return null;
    }

    private void populateForecastList() {
        this.forecastList.clear();
        for (int i = 0; i < this.forecasts.length; ++i) {
            this.forecastList.add(this.forecasts[i]);
        }
    }

    protected void init(ClimateManager climateManager) {
        this.climateValues = climateManager.getClimateValuesCopy();
        for (int i = 0; i < this.forecasts.length; ++i) {
            int offset = i - 10;
            DayForecast dayForecast = new DayForecast();
            dayForecast.weatherPeriod = new WeatherPeriod(climateManager, climateManager.getThunderStorm());
            dayForecast.weatherPeriod.setDummy(true);
            dayForecast.indexOffset = offset;
            dayForecast.airFront = new ClimateManager.AirFront();
            this.sampleDay(climateManager, dayForecast, offset);
            this.forecasts[i] = dayForecast;
        }
        this.populateForecastList();
    }

    protected void updateDayChange(ClimateManager climateManager) {
        DayForecast first = this.forecasts[0];
        for (int i = 0; i < this.forecasts.length; ++i) {
            if (i <= 0 || i >= this.forecasts.length) continue;
            this.forecasts[i].indexOffset = i - 1 - 10;
            this.forecasts[i - 1] = this.forecasts[i];
        }
        first.reset();
        this.sampleDay(climateManager, first, this.forecasts.length - 1 - 10);
        first.indexOffset = this.forecasts.length - 1 - 10;
        this.forecasts[this.forecasts.length - 1] = first;
        this.populateForecastList();
    }

    protected void sampleDay(ClimateManager climateManager, DayForecast dayForecast, int dayOffset) {
        GameTime gt = GameTime.getInstance();
        int year = gt.getYear();
        int month = gt.getMonth();
        int day = gt.getDayPlusOne();
        GregorianCalendar calendar = new GregorianCalendar(year, month, day, 0, 0);
        calendar.add(5, dayOffset);
        boolean lastFrontWarm = true;
        dayForecast.weatherOverlap = this.getWeatherOverlap(dayOffset + 10, 0.0f);
        dayForecast.weatherPeriod.stopWeatherPeriod();
        dayForecast.name = Translator.getText("IGUI_Forecaster_Day") + ": " + calendar.get(1) + " - " + (calendar.get(2) + 1) + " - " + calendar.get(5);
        for (int i = 0; i < 24; ++i) {
            if (i != 0) {
                calendar.add(11, 1);
            }
            this.climateValues.pollDate(calendar);
            if (i == 0) {
                lastFrontWarm = this.climateValues.getNoiseAirmass() >= 0.0f;
                dayForecast.airFrontString = lastFrontWarm ? "WARM" : "COLD";
                dayForecast.dawn = this.climateValues.getDawn();
                dayForecast.dusk = this.climateValues.getDusk();
                dayForecast.dayLightHours = dayForecast.dusk - dayForecast.dawn;
            }
            if (!dayForecast.weatherStarts && (lastFrontWarm && this.climateValues.getNoiseAirmass() < 0.0f || !lastFrontWarm && this.climateValues.getNoiseAirmass() >= 0.0f)) {
                int lastType = this.climateValues.getNoiseAirmass() >= 0.0f ? -1 : 1;
                dayForecast.airFront.setFrontType(lastType);
                climateManager.CalculateWeatherFrontStrength(calendar.get(1), calendar.get(2), calendar.get(5), dayForecast.airFront);
                dayForecast.airFront.setFrontWind(this.climateValues.getWindAngleDegrees());
                if (dayForecast.airFront.getStrength() >= 0.1f) {
                    float overlapStrength;
                    DayForecast overlap = this.getWeatherOverlap(dayOffset + 10, i);
                    float f = overlapStrength = overlap != null ? overlap.weatherPeriod.getTotalStrength() : -1.0f;
                    if (overlapStrength < 0.1f) {
                        dayForecast.weatherStarts = true;
                        dayForecast.weatherStartTime = i;
                        dayForecast.weatherPeriod.init(dayForecast.airFront, this.climateValues.getCacheWorldAgeHours(), calendar.get(1), calendar.get(2), calendar.get(5));
                    }
                }
                if (!dayForecast.weatherStarts) {
                    lastFrontWarm = !lastFrontWarm;
                }
            }
            boolean isDayTime = (float)i > this.climateValues.getDawn() && (float)i <= this.climateValues.getDusk();
            float temperature = this.climateValues.getTemperature();
            float humidity = this.climateValues.getHumidity();
            float windDirection = this.climateValues.getWindAngleDegrees();
            float windPower = this.climateValues.getWindIntensity();
            float cloudiness = this.climateValues.getCloudIntensity();
            if (dayForecast.weatherStarts || dayForecast.weatherOverlap != null) {
                WeatherPeriod weatherPeriod;
                WeatherPeriod weatherPeriod2 = weatherPeriod = dayForecast.weatherStarts ? dayForecast.weatherPeriod : dayForecast.weatherOverlap.weatherPeriod;
                if (weatherPeriod != null) {
                    windDirection = weatherPeriod.getWindAngleDegrees();
                    WeatherPeriod.WeatherStage stage = weatherPeriod.getStageForWorldAge(this.climateValues.getCacheWorldAgeHours());
                    if (stage != null) {
                        if (!dayForecast.weatherStages.contains(stage.getStageID())) {
                            dayForecast.weatherStages.add(stage.getStageID());
                        }
                        switch (stage.getStageID()) {
                            case 7: {
                                dayForecast.chanceOnSnow = true;
                                windPower = 0.75f + 0.25f * weatherPeriod.getTotalStrength();
                                temperature -= WeatherPeriod.getMaxTemperatureInfluence() * windPower;
                                cloudiness = 0.5f + 0.5f * windPower;
                                dayForecast.hasBlizzard = true;
                                break;
                            }
                            case 2: {
                                windPower = 0.5f * weatherPeriod.getTotalStrength();
                                temperature -= WeatherPeriod.getMaxTemperatureInfluence() * windPower;
                                cloudiness = 0.5f + 0.5f * windPower;
                                dayForecast.hasHeavyRain = true;
                                break;
                            }
                            case 3: {
                                windPower = 0.2f + 0.5f * weatherPeriod.getTotalStrength();
                                temperature -= WeatherPeriod.getMaxTemperatureInfluence() * windPower;
                                cloudiness = 0.5f + 0.5f * windPower;
                                dayForecast.hasStorm = true;
                                break;
                            }
                            case 8: {
                                windPower = 0.4f + 0.6f * weatherPeriod.getTotalStrength();
                                temperature -= WeatherPeriod.getMaxTemperatureInfluence() * windPower;
                                cloudiness = 0.5f + 0.5f * windPower;
                                dayForecast.hasTropicalStorm = true;
                                break;
                            }
                            case 1: {
                                dayForecast.hasHeavyRain = true;
                            }
                            default: {
                                temperature -= WeatherPeriod.getMaxTemperatureInfluence() * 0.25f;
                                cloudiness = 0.35f + 0.5f * weatherPeriod.getTotalStrength();
                                break;
                            }
                        }
                    } else if (dayForecast.weatherOverlap != null && (float)i < dayForecast.weatherEndTime) {
                        dayForecast.weatherEndTime = i;
                    }
                }
                if (temperature < 0.0f) {
                    dayForecast.chanceOnSnow = true;
                }
            }
            dayForecast.temperature.add(temperature, isDayTime);
            dayForecast.humidity.add(humidity, isDayTime);
            dayForecast.windDirection.add(windDirection, isDayTime);
            dayForecast.windPower.add(windPower, isDayTime);
            dayForecast.cloudiness.add(cloudiness, isDayTime);
        }
        dayForecast.temperature.calculate();
        dayForecast.humidity.calculate();
        dayForecast.windDirection.calculate();
        dayForecast.windPower.calculate();
        dayForecast.cloudiness.calculate();
        dayForecast.hasFog = this.climateValues.isDayDoFog();
        dayForecast.fogStrength = this.climateValues.getDayFogStrength();
        dayForecast.fogDuration = this.climateValues.getDayFogDuration();
    }

    private DayForecast getWeatherOverlap(int index, float hour) {
        int start = Math.max(0, index - 10);
        if (start == index) {
            return null;
        }
        for (int i = start; i < index; ++i) {
            if (!this.forecasts[i].weatherStarts) continue;
            float days = (float)this.forecasts[i].weatherPeriod.getDuration() / 24.0f;
            float end = (float)i + this.forecasts[i].weatherStartTime / 24.0f;
            float stamptoday = (float)index + hour / 24.0f;
            if (!((end += days) > stamptoday)) continue;
            return this.forecasts[i];
        }
        return null;
    }

    public int getDaysTillFirstWeather() {
        int first = -1;
        for (int i = 10; i < this.forecasts.length - 1; ++i) {
            if (!this.forecasts[i].weatherStarts || first >= 0) continue;
            first = i;
        }
        return first;
    }

    @UsedFromLua
    public static class DayForecast {
        private int indexOffset;
        private String name = "Day x";
        private WeatherPeriod weatherPeriod;
        private final ForecastValue temperature = new ForecastValue();
        private final ForecastValue humidity = new ForecastValue();
        private final ForecastValue windDirection = new ForecastValue();
        private final ForecastValue windPower = new ForecastValue();
        private final ForecastValue cloudiness = new ForecastValue();
        private boolean weatherStarts;
        private float weatherStartTime;
        private float weatherEndTime = 24.0f;
        private boolean chanceOnSnow;
        private String airFrontString = "";
        private boolean hasFog;
        private float fogStrength;
        private float fogDuration;
        private ClimateManager.AirFront airFront;
        private DayForecast weatherOverlap;
        private boolean hasHeavyRain;
        private boolean hasStorm;
        private boolean hasTropicalStorm;
        private boolean hasBlizzard;
        private float dawn;
        private float dusk;
        private float dayLightHours;
        private final ArrayList<Integer> weatherStages = new ArrayList();

        public int getIndexOffset() {
            return this.indexOffset;
        }

        public String getName() {
            return this.name;
        }

        public ForecastValue getTemperature() {
            return this.temperature;
        }

        public ForecastValue getHumidity() {
            return this.humidity;
        }

        public ForecastValue getWindDirection() {
            return this.windDirection;
        }

        public ForecastValue getWindPower() {
            return this.windPower;
        }

        public ForecastValue getCloudiness() {
            return this.cloudiness;
        }

        public WeatherPeriod getWeatherPeriod() {
            return this.weatherPeriod;
        }

        public boolean isWeatherStarts() {
            return this.weatherStarts;
        }

        public float getWeatherStartTime() {
            return this.weatherStartTime;
        }

        public float getWeatherEndTime() {
            return this.weatherEndTime;
        }

        public boolean isChanceOnSnow() {
            return this.chanceOnSnow;
        }

        public String getAirFrontString() {
            return this.airFrontString;
        }

        public boolean isHasFog() {
            return this.hasFog;
        }

        public ClimateManager.AirFront getAirFront() {
            return this.airFront;
        }

        public DayForecast getWeatherOverlap() {
            return this.weatherOverlap;
        }

        public String getMeanWindAngleString() {
            return ClimateManager.getWindAngleString(this.windDirection.getTotalMean());
        }

        public float getFogStrength() {
            return this.fogStrength;
        }

        public float getFogDuration() {
            return this.fogDuration;
        }

        public boolean isHasHeavyRain() {
            return this.hasHeavyRain;
        }

        public boolean isHasStorm() {
            return this.hasStorm;
        }

        public boolean isHasTropicalStorm() {
            return this.hasTropicalStorm;
        }

        public boolean isHasBlizzard() {
            return this.hasBlizzard;
        }

        public ArrayList<Integer> getWeatherStages() {
            return this.weatherStages;
        }

        public float getDawn() {
            return this.dawn;
        }

        public float getDusk() {
            return this.dusk;
        }

        public float getDayLightHours() {
            return this.dayLightHours;
        }

        private void reset() {
            this.weatherPeriod.stopWeatherPeriod();
            this.temperature.reset();
            this.humidity.reset();
            this.windDirection.reset();
            this.windPower.reset();
            this.cloudiness.reset();
            this.weatherStarts = false;
            this.weatherStartTime = 0.0f;
            this.weatherEndTime = 24.0f;
            this.chanceOnSnow = false;
            this.hasFog = false;
            this.fogStrength = 0.0f;
            this.fogDuration = 0.0f;
            this.weatherOverlap = null;
            this.hasHeavyRain = false;
            this.hasStorm = false;
            this.hasTropicalStorm = false;
            this.hasBlizzard = false;
            this.weatherStages.clear();
        }
    }

    @UsedFromLua
    public static class ForecastValue {
        private float dayMin;
        private float dayMax;
        private float dayMean;
        private int dayMeanTicks;
        private float nightMin;
        private float nightMax;
        private float nightMean;
        private int nightMeanTicks;
        private float totalMin;
        private float totalMax;
        private float totalMean;
        private int totalMeanTicks;

        public ForecastValue() {
            this.reset();
        }

        public float getDayMin() {
            return this.dayMin;
        }

        public float getDayMax() {
            return this.dayMax;
        }

        public float getDayMean() {
            return this.dayMean;
        }

        public float getNightMin() {
            return this.nightMin;
        }

        public float getNightMax() {
            return this.nightMax;
        }

        public float getNightMean() {
            return this.nightMean;
        }

        public float getTotalMin() {
            return this.totalMin;
        }

        public float getTotalMax() {
            return this.totalMax;
        }

        public float getTotalMean() {
            return this.totalMean;
        }

        protected void add(float val, boolean isDay) {
            if (isDay) {
                if (val < this.dayMin) {
                    this.dayMin = val;
                }
                if (val > this.dayMax) {
                    this.dayMax = val;
                }
                this.dayMean += val;
                ++this.dayMeanTicks;
            } else {
                if (val < this.nightMin) {
                    this.nightMin = val;
                }
                if (val > this.nightMax) {
                    this.nightMax = val;
                }
                this.nightMean += val;
                ++this.nightMeanTicks;
            }
            if (val < this.totalMin) {
                this.totalMin = val;
            }
            if (val > this.totalMax) {
                this.totalMax = val;
            }
            this.totalMean += val;
            ++this.totalMeanTicks;
        }

        protected void calculate() {
            this.totalMean = this.totalMeanTicks <= 0 ? 0.0f : (this.totalMean /= (float)this.totalMeanTicks);
            if (this.dayMeanTicks <= 0) {
                this.dayMin = this.totalMin;
                this.dayMax = this.totalMax;
                this.dayMean = this.totalMean;
            } else {
                this.dayMean /= (float)this.dayMeanTicks;
            }
            if (this.nightMeanTicks <= 0) {
                this.nightMin = this.totalMin;
                this.nightMax = this.totalMax;
                this.nightMean = this.totalMean;
            } else {
                this.nightMean /= (float)this.nightMeanTicks;
            }
        }

        protected void reset() {
            this.dayMin = 10000.0f;
            this.dayMax = -10000.0f;
            this.dayMean = 0.0f;
            this.dayMeanTicks = 0;
            this.nightMin = 10000.0f;
            this.nightMax = -10000.0f;
            this.nightMean = 0.0f;
            this.nightMeanTicks = 0;
            this.totalMin = 10000.0f;
            this.totalMax = -10000.0f;
            this.totalMean = 0.0f;
            this.totalMeanTicks = 0;
        }
    }
}

