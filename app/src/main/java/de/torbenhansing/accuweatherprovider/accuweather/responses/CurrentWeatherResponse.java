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

import lineageos.providers.WeatherContract;

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
        //EpochTime convert from ms to ns
        return EpochTime * 1000;
    }

    public int getWeatherIconId() {
        return WeatherIcon;
    }

    public double getTemperature(int tempUnit) {
        if(Temperature == null) {
            return Double.NaN;
        }
        if(tempUnit == WeatherContract.WeatherColumns.TempUnit.CELSIUS) {
            return Temperature.Metric != null ? Temperature.Metric.Value : Double.NaN;
        } else {
            return Temperature.Imperial != null ? Temperature.Imperial.Value : Double.NaN;
        }
    }

    public double getHumidity() {
        return RelativeHumidity ;
    }

    public double getWindSpeed(int windUnit) {
        if(Wind == null || Wind.Speed == null) {
            return Double.NaN;
        }
        if (windUnit == WeatherContract.WeatherColumns.WindSpeedUnit.KPH) {
            return Wind.Speed.Metric != null ? Wind.Speed.Metric.Value : Double.NaN;
        } else {
            return Wind.Speed.Imperial != null ? Wind.Speed.Imperial.Value : Double.NaN;
        }
    }

    public double getWindDirection() {
        if(Wind == null || Wind.Direction == null) {
            return Double.NaN;
        }
        return Wind.Direction.Degrees;
    }
}
