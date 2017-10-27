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

package de.torbenhansing.accuweatherprovider.accuweather.responses;

import java.io.Serializable;

import cyanogenmod.providers.WeatherContract;

@SuppressWarnings("FieldCanBeLocal")
public class CurrentWeatherResponse implements Serializable {
    private long EpochTime = -1L;
    private int WeatherIcon = 0;
    private Values Temperature = null;
    private long RelativeHumidity = -1;
    private Wind Wind = null;

    private static class Values {
        private Unit Metric = null;
        private Unit Imperial = null;

        static class Unit {
            private double Value = Double.NaN;
        }
    }

    private static class Wind {
        private Direction Direction = null;
        private Values Speed = null;

        static class Direction {
            private long Degrees = -1;
        }
    }

    public long getEpochTime() {
        return EpochTime;
    }

    public int getWeatherIconId() {
        return WeatherIcon;
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
