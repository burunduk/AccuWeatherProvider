package de.torbenhansing.accuweatherprovider.accuweather;

import java.io.Serializable;

@SuppressWarnings("FieldCanBeLocal")
class CityInfoResponse implements Serializable {
    private String Key = "";
    private String LocalizedName = "";
    private Country Country = null;

    private static class Country {
        private String LocalizedName = "";
    }

    String getCityId() {
        return Key;
    }

    String getCityName() {
        return LocalizedName;
    }

    String getCountryName() {
        return Country.LocalizedName;
    }
}
