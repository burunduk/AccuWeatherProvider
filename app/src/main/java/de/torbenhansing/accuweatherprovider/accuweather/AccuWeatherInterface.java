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

import java.util.List;

import de.torbenhansing.accuweatherprovider.accuweather.responses.CityInfoResponse;
import de.torbenhansing.accuweatherprovider.accuweather.responses.CurrentWeatherResponse;
import de.torbenhansing.accuweatherprovider.accuweather.responses.ForecastResponse;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface AccuWeatherInterface {
    @GET("/currentconditions/v1/{locationKey}")
    Call<CurrentWeatherResponse> queryCurrentWeather(@Path("locationKey") String cityId,
                                                     @Query("apikey") String apikey,
                                                     @Query("language") String lang,
                                                     @Query("details") boolean details
    );

    @GET("/forecasts/v1/daily/{days}day/{locationKey}")
    Call<ForecastResponse> queryForecast(@Path("days") int daysCount,
                                         @Path("locationKey") String cityId,
                                         @Query("apikey") String apiKey,
                                         @Query("language") String lang,
                                         @Query("details") boolean details,
                                         @Query("metric") boolean metric
    );

    @GET("/locations/v1/cities/search")
    Call<List<CityInfoResponse>> lookupCity(@Query("apikey") String apiKey,
                                            @Query("q") String cityName,
                                            @Query("language") String lang,
                                            @Query("details") boolean details
    );

    @GET("/locations/v1/cities/geoposition/search")
    Call<CityInfoResponse> lookupCity(@Query("apikey") String apiKey,
                                      @Query("q") String lat_long,
                                      @Query("language") String lang,
                                      @Query("details") boolean details,
                                      @Query("toplevel") boolean toplevel
    );
}
