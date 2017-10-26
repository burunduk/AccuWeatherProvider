/*
 *  Copyright (C) 2016 The CyanogenMod Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package de.torbenhansing.accuweatherprovider.accuweather;

import android.util.SparseArray;
import android.util.SparseIntArray;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import cyanogenmod.providers.WeatherContract;

public class CurrentWeatherResponse implements Serializable {
    public CurrentWeatherResponse() {}

    private long EpochTime;
    private int WeatherIcon;
    private Values Temperature;
    private long RelativeHumidity;
    private Wind Wind;

    static class Values {
        public Values() {};
        private Unit Metric;
        private Unit Imperial;

        static class Unit {
            public Unit() {};
            private double Value;
            private String Unit;
            private long UnitType;
        }
    }

    static class Wind {
        public Wind() {}
        private Direction Direction;
        private Values Speed;

        static class Direction {
            public Direction() {}
            private long Degrees;
        }
    }

    public long getEpochTime() {
        return EpochTime;
    }

    private static final SparseIntArray weatherIconMap = new SparseIntArray();
    static {
        // Sunny
        weatherIconMap.append(1, WeatherContract.WeatherColumns.WeatherCode.SUNNY);
        // Mostly Sunny
        weatherIconMap.append(2, WeatherContract.WeatherColumns.WeatherCode.SUNNY);
        // Partly Sunny
        weatherIconMap.append(3, WeatherContract.WeatherColumns.WeatherCode.PARTLY_CLOUDY_DAY);
        // Intermittent Clouds
        weatherIconMap.append(4, WeatherContract.WeatherColumns.WeatherCode.PARTLY_CLOUDY_DAY);
        // Hazy Sunshine
        weatherIconMap.append(5, WeatherContract.WeatherColumns.WeatherCode.HAZE);
        // Mostly Cloudy
        weatherIconMap.append(6, WeatherContract.WeatherColumns.WeatherCode.MOSTLY_CLOUDY_DAY);
        // Cloudy
        weatherIconMap.append(7, WeatherContract.WeatherColumns.WeatherCode.CLOUDY);
        // Dreary
        weatherIconMap.append(8, WeatherContract.WeatherColumns.WeatherCode.CLOUDY);
        // Fog
        weatherIconMap.append(11, WeatherContract.WeatherColumns.WeatherCode.FOGGY);
        // Showers
        weatherIconMap.append(12, WeatherContract.WeatherColumns.WeatherCode.SHOWERS);
        // Mostly Cloudy w/ Showers
        weatherIconMap.append(13, WeatherContract.WeatherColumns.WeatherCode.SCATTERED_SHOWERS);
        // Partly Sunny w/ Showers
        weatherIconMap.append(14, WeatherContract.WeatherColumns.WeatherCode.SCATTERED_SHOWERS);
        // Thunderstorms
        weatherIconMap.append(15, WeatherContract.WeatherColumns.WeatherCode.THUNDERSTORMS);
        // Mostly Cloudy w/ Thunderstorms
        weatherIconMap.append(16, WeatherContract.WeatherColumns.WeatherCode.SCATTERED_THUNDERSTORMS);
        // Partly Sunny w/ Thunderstorms
        weatherIconMap.append(17, WeatherContract.WeatherColumns.WeatherCode.ISOLATED_THUNDERSTORMS);
        // Rain
        weatherIconMap.append(18, WeatherContract.WeatherColumns.WeatherCode.SHOWERS);
        // Flurries
        weatherIconMap.append(19, WeatherContract.WeatherColumns.WeatherCode.SNOW_FLURRIES);
        // Mostly Cloudy w/ Flurries
        weatherIconMap.append(20, WeatherContract.WeatherColumns.WeatherCode.SNOW_FLURRIES);
        // Partly Sunny w/ Flurries
        weatherIconMap.append(21, WeatherContract.WeatherColumns.WeatherCode.SNOW_FLURRIES);
        // Snow
        weatherIconMap.append(22, WeatherContract.WeatherColumns.WeatherCode.SNOW);
        // Mostly Cloudy w/ Snow
        weatherIconMap.append(23, WeatherContract.WeatherColumns.WeatherCode.SNOW);
        // Ics
        weatherIconMap.append(24, WeatherContract.WeatherColumns.WeatherCode.SNOW_SHOWERS);
        // Sleet
        weatherIconMap.append(25, WeatherContract.WeatherColumns.WeatherCode.SLEET);
        // Freezing Rain
        weatherIconMap.append(26, WeatherContract.WeatherColumns.WeatherCode.FREEZING_RAIN);
        // Rain and Snow
        weatherIconMap.append(29, WeatherContract.WeatherColumns.WeatherCode.MIXED_RAIN_AND_SNOW);
        // Hot
        weatherIconMap.append(30, WeatherContract.WeatherColumns.WeatherCode.HOT);
        // Cold
        weatherIconMap.append(31, WeatherContract.WeatherColumns.WeatherCode.COLD);
        // Windy
        weatherIconMap.append(32, WeatherContract.WeatherColumns.WeatherCode.WINDY);
        // Clear
        weatherIconMap.append(33, WeatherContract.WeatherColumns.WeatherCode.CLEAR_NIGHT);
        // Mostly Clear
        weatherIconMap.append(34, WeatherContract.WeatherColumns.WeatherCode.CLEAR_NIGHT);
        // Partly Cloudy
        weatherIconMap.append(35, WeatherContract.WeatherColumns.WeatherCode.PARTLY_CLOUDY_NIGHT);
        // Intermittent Clouds
        weatherIconMap.append(36, WeatherContract.WeatherColumns.WeatherCode.PARTLY_CLOUDY_NIGHT);
        // Hazy Moonlight
        // Not available
        // Mostly Cloudy
        weatherIconMap.append(38, WeatherContract.WeatherColumns.WeatherCode.MOSTLY_CLOUDY_NIGHT);
        // Partly Cloudy w/ Showers
        //   not available
        // Mostly Cloudy w/ Showers
        //   not available
        // Partly Cloudy w/ T-Storms
        //   not available
        // Mostly Cloudy w/ T-Storms
        //   not available
        // Mostly Cloudy w/ Flurries
        //   not available
        // 	Mostly Cloudy w/ Snow
        //   not available
    }


    public int getConditionCode() {
        return weatherIconMap.get(WeatherIcon,
                WeatherContract.WeatherColumns.WeatherCode.NOT_AVAILABLE);
    }

    public double getTemperature(int tempUnit) {
        if(tempUnit == WeatherContract.WeatherColumns.TempUnit.CELSIUS) {
            return Temperature.Metric.Value;
        } else {
            return Temperature.Imperial.Value;
        }
    }

    public double getHumidity() {
        return RelativeHumidity / 100;
    }

    public double getWindSpeed(int windUnit) {
        if (windUnit == WeatherContract.WeatherColumns.WindSpeedUnit.KPH) {
            return Wind.Speed.Metric.Value;
        } else {
            return Wind.Speed.Imperial.Value;
        }
    }

    public double getWindDirection() {
        return Wind.Direction.Degrees;
    }
}
