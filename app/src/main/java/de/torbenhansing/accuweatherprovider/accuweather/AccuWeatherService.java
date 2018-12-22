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
import android.util.Pair;
import android.util.SparseIntArray;

import com.google.gson.JsonParseException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import lineageos.providers.LineageSettings;
import lineageos.providers.WeatherContract;
import lineageos.weather.WeatherInfo;
import lineageos.weather.WeatherLocation;
import de.torbenhansing.accuweatherprovider.accuweather.responses.CityInfoResponse;
import de.torbenhansing.accuweatherprovider.accuweather.responses.CurrentWeatherResponse;
import de.torbenhansing.accuweatherprovider.accuweather.responses.ForecastResponse;
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
    // we always get the KM/H unit for the wind. The app will make the calculations
    private static final int WIND_UNIT = WeatherContract.WeatherColumns.WindSpeedUnit.KPH;

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

    private WeatherInfo getWeatherForCity(String cityId, String cityName) {
        String language = getLanguageCode();

        Call<CurrentWeatherResponse> weatherResponseCall
                = mAccuWeatherInterface.queryCurrentWeather(cityId, mApiKey, language, DETAILS);
        Response<CurrentWeatherResponse> currentWeatherResponse;
        try {
            Logging.logd(weatherResponseCall.request().toString());
            currentWeatherResponse = weatherResponseCall.execute();
            Logging.logd("CurrentWeatherResponse: " + currentWeatherResponse.raw().toString());
            if(!currentWeatherResponse.isSuccessful()) {
                throw new IOException(currentWeatherResponse.message());
            }
        } catch (IOException | JsonParseException e) {
            Logging.loge("Exception while requesting current weather: " + e);
            return null;
        }

        if (currentWeatherResponse.code() == 200) {
            //Query the forecast now. We can return a valid WeatherInfo object without the forecast
            //but the user is expecting both the current weather and the forecast
            final int tempUnit = getTempUnitFromSettings();
            final boolean metric = (tempUnit == WeatherContract.WeatherColumns.TempUnit.CELSIUS);
            Call<ForecastResponse> forecastResponseCall
                    = mAccuWeatherInterface.queryForecast(FORECAST_DAYS, cityId, mApiKey,
                    language, DETAILS, metric);
            ForecastResponse forecastResponse = null;
            try {
                Logging.logd(forecastResponseCall.request().toString());
                Response<ForecastResponse> r = forecastResponseCall.execute();
                Logging.logd("ForcastResponse: " + r.raw().toString());
                if(!r.isSuccessful()) {
                    throw new IOException(r.message());
                }
                forecastResponse = r.body();
            } catch (IOException | JsonParseException e) {
                //this is an error we can live with
                Logging.loge("Exception while requesting forecast " + e);
            }
            return processWeatherResponse(cityName, currentWeatherResponse.body(),
                    forecastResponse, tempUnit);
        } else {
            return null;
        }

    }

    private Pair<String, String> getCityForLocation(Location location) {
        String language = getLanguageCode();
        @SuppressLint("DefaultLocale")
        String lat_long = String.format(Locale.ROOT, "%f,%f",
                location.getLatitude(), location.getLongitude());
        // First determine the City code for this location
        Call<CityInfoResponse> cityLookupCall = mAccuWeatherInterface.lookupCity(mApiKey,
                lat_long, language, false, TOPLEVEL);
        Response<CityInfoResponse> currentCityResponse;
        try {
            Logging.logd(cityLookupCall.request().toString());
            currentCityResponse = cityLookupCall.execute();
            Logging.logd("CurrentCityResponse: " + currentCityResponse.raw().toString());
            if(!currentCityResponse.isSuccessful()) {
                throw new IOException(currentCityResponse.message());
            }
        } catch (IOException | JsonParseException e) {
            Logging.loge("Exception while requesting the current city: " + e);
            return null;
        }
        // Now check the weather for this city
        return new Pair<>(currentCityResponse.body().getCityId(),
                currentCityResponse.body().getCityName());
    }

    private WeatherInfo processWeatherResponse(String cityName,
                                               CurrentWeatherResponse currentWeatherResponse,
                                               ForecastResponse forecastResponse, int tempUnit) {

        final double temperature = currentWeatherResponse.getTemperature(tempUnit);
        //We need at least the city name and current temperature
        if (cityName == null || Double.isNaN(temperature)) return null;

        WeatherInfo.Builder builder = new WeatherInfo.Builder(cityName, temperature, tempUnit)
                .setTimestamp(currentWeatherResponse.getEpochTime());

        builder.setWeatherCondition(mapConditionIconToCode(
                currentWeatherResponse.getWeatherIconId()));

        final double humidity = currentWeatherResponse.getHumidity();
        if (!Double.isNaN(humidity)) {
            builder.setHumidity(humidity);
        }

        // The first forecast is the current day
        final double todaysHigh = forecastResponse.getForecastList().get(0).getMaxTemp();
        if (!Double.isNaN(todaysHigh)) {
            builder.setTodaysHigh(todaysHigh);
        }

        // The first forecast is the current day
        final double todaysLow = forecastResponse.getForecastList().get(0).getMinTemp();
        if (!Double.isNaN(todaysLow)) {
            builder.setTodaysLow(todaysLow);
        }

        final double windDir = currentWeatherResponse.getWindDirection();
        final double windSpeed = currentWeatherResponse.getWindSpeed(WIND_UNIT);
        if (!Double.isNaN(windDir) && !Double.isNaN(windSpeed)) {
            builder.setWind(windSpeed, windDir, WIND_UNIT);
        }

        List<WeatherInfo.DayForecast> forecastList = new ArrayList<>();
        for (ForecastResponse.DailyForecast forecast : forecastResponse.getForecastList()) {
            WeatherInfo.DayForecast.Builder forecastBuilder
                    = new WeatherInfo.DayForecast.Builder(
                    mapConditionIconToCode(forecast.getWeatherIconId()));

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
        return builder.build();
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
        return getWeatherForCity(weatherLocation.getCityId(), weatherLocation.getCity());
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
        // First get the city for the given location
        Pair<String, String> city = getCityForLocation(location);
        if (city == null) {
            return null;
        }
        String currentCityId = city.first;
        String currentCityName = city.second;
        // Now query the weather for this city
        return getWeatherForCity(currentCityId, currentCityName);
    }

    /**
     * This is a synchronous call and should never be called from the UI thread
     * @param cityName The name of the city to search
     * @return Array of {@link WeatherLocation} weather locations. This method will always return a
     * list, but the list might be empty if no match was found
     * @throws InvalidApiKeyException If the application ID has not been set
     */
    public List<WeatherLocation> lookupCity(String cityName) throws InvalidApiKeyException {
        if (!maybeValidApiKey(mApiKey)) {
            throw new InvalidApiKeyException();
        }

        Call<List<CityInfoResponse>> lookupCityCall = mAccuWeatherInterface.lookupCity(mApiKey,
                cityName, getLanguageCode(), false);

        Response<List<CityInfoResponse>> lookupResponse;
        try {
            Logging.logd(lookupCityCall.request().toString());
            lookupResponse = lookupCityCall.execute();
            Logging.logd("LookupCityResponse: " + lookupResponse.raw().toString());
            if(!lookupResponse.isSuccessful()) {
                throw new IOException(lookupResponse.message());
            }
        } catch (IOException | JsonParseException e) {
            Logging.loge("IOException while looking up city name " + e);
            //Return empty list to prevent NPE
            return new ArrayList<>();
        }

        List<WeatherLocation> weatherLocations = new ArrayList<>();
        for (CityInfoResponse cityInfo: lookupResponse.body()) {
            WeatherLocation location = new WeatherLocation.Builder(cityInfo.getCityId(),
                    cityInfo.getCityName())
                    .setCountry(cityInfo.getCountryName())
                    .setCountryId(cityInfo.getCountryId())
                    .setPostalCode(cityInfo.getPostalCode())
                    .setState(cityInfo.getState()).build();
            weatherLocations.add(location);
        }
        return weatherLocations;
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
        return (language + "-" + country).toLowerCase();
    }

    public final static class InvalidApiKeyException extends Exception {

        InvalidApiKeyException() {
            super("A valid API key is required to process the request");
        }
    }

    private boolean maybeValidApiKey(String key) {
        return key != null && !TextUtils.equals(key, "");
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

    private int mapConditionIconToCode(int weatherIcon) {
        return weatherIconMap.get(weatherIcon,
                WeatherContract.WeatherColumns.WeatherCode.NOT_AVAILABLE);
    }

    private int getTempUnitFromSettings() {
        try {
            return LineageSettings.Global.getInt(mContext.getContentResolver(),
                    LineageSettings.Global.WEATHER_TEMPERATURE_UNIT);
        } catch (LineageSettings.LineageSettingNotFoundException e) {
            //Default to metric
            return WeatherContract.WeatherColumns.TempUnit.CELSIUS;
        }
    }
}
