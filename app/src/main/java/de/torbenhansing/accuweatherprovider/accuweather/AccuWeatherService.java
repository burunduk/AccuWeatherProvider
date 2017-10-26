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

import android.annotation.SuppressLint;
import android.content.Context;
import android.location.Location;
import android.text.TextUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

import cyanogenmod.providers.CMSettings;
import cyanogenmod.providers.WeatherContract;
import cyanogenmod.weather.WeatherInfo;
import cyanogenmod.weather.WeatherLocation;
import de.torbenhansing.accuweatherprovider.utils.Logging;
import okhttp3.OkHttpClient;
import retrofit2.Call;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class AccuWeatherService {
    private static final String BASE_URL = "http://dataservice.accuweather.com";
    // TODO Add an preference in settings to customize this
    private static final int FORECAST_DAYS = 5;
    // Our requests should always include all details possible
    private static final boolean DETAILS = true;
    // Do we wan't to get the larger city when doing a lookup for locations?
    private  static final boolean TOPLEVEL = false;

    private final AccuWeatherInterface mAccuWeatherInterface;
    private volatile String mApiKey;
    private Context mContext;

    public AccuWeatherService(Context context) {
        mContext = context;
        Retrofit mRetrofit = buildRestAdapter();
        mAccuWeatherInterface = mRetrofit.create(AccuWeatherInterface.class);
    }

    public void setApiKey(String apiKey) {
        mApiKey = apiKey;
    }

    /**
     * This is a synchronous call and should never be called from the UI thread
     * @param weatherLocation The location for which the weather should be requested
     * @throws InvalidApiKeyException If the application ID has not been set
     */
    public WeatherInfo queryWeather(WeatherLocation weatherLocation)
            throws InvalidApiKeyException {

        if (!maybeValidApiKey(mApiKey)) {
            throw new InvalidApiKeyException();
        }

        String language = getLanguageCode();

        Call<CurrentWeatherResponse> weatherResponseCall
                = mAccuWeatherInterface.queryCurrentWeather(mApiKey, weatherLocation.getCityId(),
                                                            language, DETAILS);
        Response<CurrentWeatherResponse> currentWeatherResponse;
        try {
            Logging.logd(weatherResponseCall.request().toString());
            currentWeatherResponse = weatherResponseCall.execute();
        } catch (IOException e) {
            //An error occurred while talking to the server
            return null;
        }

        if (currentWeatherResponse.code() == 200) {
            //Query the forecast now. We can return a valid WeatherInfo object without the forecast
            //but the user is expecting both the current weather and the forecast
            final int tempUnit = getTempUnitFromSettings();
            final boolean metric = (tempUnit == WeatherContract.WeatherColumns.TempUnit.CELSIUS);
            Call<ForecastResponse> forecastResponseCall
                    = mAccuWeatherInterface.queryForecast(mApiKey, weatherLocation.getCityId(),
                    FORECAST_DAYS, language, DETAILS, metric);
            ForecastResponse forecastResponse = null;
            try {
                Logging.logd(forecastResponseCall.request().toString());
                Response<ForecastResponse> r = forecastResponseCall.execute();
                if (r.code() == 200) forecastResponse = r.body();
            } catch (IOException e) {
                //this is an error we can live with
                Logging.logd("IOException while requesting forecast " + e);
            }
            return processWeatherResponse(currentWeatherResponse.body(), forecastResponse,
                    tempUnit);
        } else {
            return null;
        }
    }

    /**
     * This is a synchronous call and should never be called from the UI thread
     * @param location A {@link WeatherInfo} weather info object if the call was successfully
     *                 processed by the end point, null otherwise
     * @throws InvalidApiKeyException If the application ID has not been set
     */
    public WeatherInfo queryWeather(Location location) throws InvalidApiKeyException {
        if (!maybeValidApiKey(mApiKey)) {
            throw new InvalidApiKeyException();
        }

        String language = getLanguageCode();
        @SuppressLint("DefaultLocale")
        String lat_long = String.format("%f,%f", location.getLatitude(), location.getLongitude());
        // First determine the City code for this location
        Call<CityInfoResponse> cityLookupCall = mAccuWeatherInterface.lookupCity(mApiKey,
                lat_long, language, DETAILS, TOPLEVEL);
        Response<CityInfoResponse> currentCityResponse;
        try {
            Logging.logd(cityLookupCall.request().toString());
            currentCityResponse = cityLookupCall.execute();
        } catch (IOException e) {
            Logging.loge("IOException while requesting the current city: " + e);
            return null;
        }
        // Now check the weather for this city
        final String currentCityId = currentCityResponse.body().getCityId();
        Call<CurrentWeatherResponse> weatherResponseCall
                = mAccuWeatherInterface.queryCurrentWeather(mApiKey, currentCityId,
                language, DETAILS);
        Response<CurrentWeatherResponse> currentWeatherResponse;
        try {
            Logging.logd(weatherResponseCall.request().toString());
            currentWeatherResponse = weatherResponseCall.execute();
        } catch (IOException e) {
            //An error occurred while talking to the server
            Logging.logd("IOException while requesting weather " + e);
            return null;
        }

        if (currentWeatherResponse.code() == 200) {
            //Query the forecast now. We can return a valid WeatherInfo object without the forecast
            //but the user is expecting both the current weather and the forecast
            final int tempUnit = getTempUnitFromSettings();
            final boolean metric = (tempUnit == WeatherContract.WeatherColumns.TempUnit.CELSIUS);
            Call<ForecastResponse> forecastResponseCall
                    = mAccuWeatherInterface.queryForecast(mApiKey, currentCityId,
                    FORECAST_DAYS, language, DETAILS, metric);
            ForecastResponse forecastResponse = null;
            try {
                Logging.logd(forecastResponseCall.request().toString());
                Response<ForecastResponse> r = forecastResponseCall.execute();
                if (r.code() == 200) forecastResponse = r.body();
            } catch (IOException e) {
                //this is an error we can live with
                Logging.logd("IOException while requesting forecast " + e);
            }
            return processWeatherResponse(currentWeatherResponse.body(), forecastResponse,
                    tempUnit);
        } else {
            return null;
        }
    }

    private WeatherInfo processWeatherResponse(CurrentWeatherResponse currentWeatherResponse,
            ForecastResponse forecastResponse, int tempUnit) {

        final String cityName = currentWeatherResponse.getCityName();
        final double temperature = currentWeatherResponse.getTemperature();

        //We need at least the city name and current temperature
        if (cityName == null || Double.isNaN(temperature)) return null;

        WeatherInfo.Builder builder = new WeatherInfo.Builder(cityName, temperature, tempUnit)
                        .setTimestamp(currentWeatherResponse.getEpochTime());

        builder.setWeatherCondition(mapConditionIconToCode(
                currentWeatherResponse.getWeatherIconId(),
                        currentWeatherResponse.getConditionCode()));

        final double humidity = currentWeatherResponse.getHumidity();
        if (!Double.isNaN(humidity)) {
            builder.setHumidity(humidity);
        }

        final double todaysHigh = currentWeatherResponse.getTodaysMaxTemp();
        if (!Double.isNaN(todaysHigh)) {
            builder.setTodaysHigh(todaysHigh);
        }

        final double todaysLow = currentWeatherResponse.getTodaysMinTemp();
        if (!Double.isNaN(todaysLow)) {
            builder.setTodaysLow(todaysLow);
        }

        final double windDir = currentWeatherResponse.getWindDirection();
        final double windSpeed = currentWeatherResponse.getWindSpeed();
        if (!Double.isNaN(windDir) && !Double.isNaN(windSpeed)) {
            builder.setWind(windSpeed, windDir, WeatherContract.WeatherColumns.WindSpeedUnit.KPH);
        }

        if (forecastResponse != null) {
            List<WeatherInfo.DayForecast> forecastList = new ArrayList<>();
            for (ForecastResponse.DayForecast forecast : forecastResponse.getForecastList()) {
                WeatherInfo.DayForecast.Builder forecastBuilder
                        = new WeatherInfo.DayForecast.Builder(mapConditionIconToCode(
                                forecast.getWeatherIconId(), forecast.getConditionCode()));

                final double max = forecast.getMaxTemp();
                if (!Double.isNaN(max)) {
                    forecastBuilder.setHigh(max);
                }

                final double min = forecast.getMinTemp();
                if (!Double.isNaN(min)) {
                    forecastBuilder.setLow(min);
                }

                forecastList.add(forecastBuilder.build());
            }
            builder.setForecast(forecastList);
        }
        return builder.build();
    }

    /**
     * This is a synchronous call and should never be called from the UI thread
     * @param cityName
     * @return Array of {@link WeatherLocation} weather locations. This method will always return a
     * list, but the list might be empty if no match was found
     * @throws InvalidApiKeyException If the application ID has not been set
     */
    public List<WeatherLocation> lookupCity(String cityName) throws InvalidApiKeyException {
        if (!maybeValidApiKey(mApiKey)) {
            throw new InvalidApiKeyException();
        }

        Call<LookupCityResponse> lookupCityCall
                = mAccuWeatherInterface.lookupCity(cityName, RESULT_FORMAT, getLanguageCode(),
                        SEARCH_CITY_TYPE, mApiKey);

        Response<LookupCityResponse> lookupResponse;
        try {
            Logging.logd(lookupCityCall.request().toString());
            lookupResponse = lookupCityCall.execute();
        } catch (IOException e) {
            Logging.logd("IOException while looking up city name " + e);
            //Return empty list to prevent NPE
            return new ArrayList<>();
        }

        if (lookupResponse != null && lookupResponse.code() == 200) {
            List<WeatherLocation> weatherLocations = new ArrayList<>();
            for (LookupCityResponse.CityInfo cityInfo: lookupResponse.body().getCityInfoList()) {
                WeatherLocation location
                        = new WeatherLocation.Builder(cityInfo.getCityId(),
                                cityInfo.getCityName()).setCountry(cityInfo.getCountry()).build();
                weatherLocations.add(location);
            }
            return weatherLocations;
        } else {
            //Return empty list to prevent NPE
            return new ArrayList<>();
        }
    }

    private Retrofit buildRestAdapter() {
        final OkHttpClient httpClient = new OkHttpClient().newBuilder().build();

        return new Retrofit.Builder().baseUrl(BASE_URL).client(httpClient)
                .addConverterFactory(GsonConverterFactory.create()).build();
    }

    private String getLanguageCode() {
        Locale locale = mContext.getResources().getConfiguration().locale;
        String country = locale.getCountry();
        String language = locale.getLanguage();
        return language + "_" + country;
    }

    public final static class InvalidApiKeyException extends Exception {

        public InvalidApiKeyException() {
            super("A valid API key is required to process the request");
        }
    }

    private boolean maybeValidApiKey(String key) {
        return key != null && !TextUtils.equals(key, "");
    }

    private int getTempUnitFromSettings() {
        try {
            final int tempUnit = CMSettings.Global.getInt(mContext.getContentResolver(),
                    CMSettings.Global.WEATHER_TEMPERATURE_UNIT);
            return tempUnit;
        } catch (CMSettings.CMSettingNotFoundException e) {
            //Default to metric
            return WeatherContract.WeatherColumns.TempUnit.CELSIUS;
        }
    }
}
