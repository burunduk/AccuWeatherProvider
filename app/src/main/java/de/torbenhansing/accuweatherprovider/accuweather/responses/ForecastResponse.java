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
import java.util.List;

public class ForecastResponse implements Serializable {
    private List<DailyForecast> DailyForecasts = null;

    public static class DailyForecast {
        private Temperature Temperature = null;
        private Day Day = null;

        static class Value {
            private double Value = Double.NaN;
        }

        static class Temperature {
            private Value Minimum = null;
            private Value Maximum = null;
        }

        static class Day {
            private int Icon = 0;
        }

        public int getWeatherIconId() {
            return Day.Icon;
        }

        public double getMaxTemp() {
            return Temperature.Maximum.Value;
        }

        public double getMinTemp() {
            return Temperature.Minimum.Value;
        }

    }

    public List<DailyForecast> getForecastList() {
        return DailyForecasts;
    }
}
