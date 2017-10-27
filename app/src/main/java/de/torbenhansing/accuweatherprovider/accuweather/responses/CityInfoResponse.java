package de.torbenhansing.accuweatherprovider.accuweather.responses;

import java.io.Serializable;

@SuppressWarnings("FieldCanBeLocal")
public class CityInfoResponse implements Serializable {
    private String Key = "";
    private String LocalizedName = "";
    private Country Country = null;

    private static class Country {
        private String LocalizedName = "";
    }

    public String getCityId() {
        return Key;
    }

    public String getCityName() {
        return LocalizedName;
    }

    public String getCountryName() {
        return Country.LocalizedName;
    }
}
