package de.torbenhansing.accuweatherprovider.accuweather.responses;

import java.io.Serializable;

@SuppressWarnings("FieldCanBeLocal")
public class CityInfoResponse implements Serializable {
    private String Key = "";
    private String LocalizedName = "";
    private Country Country = null;
    private String PrimaryPostalCode = "";
    private AdministrativeArea AdministrativeArea = null;

    private static class Country {
        private String ID = "";
        private String LocalizedName = "";
    }

    private static class AdministrativeArea {
        private String LocalizedName = "";
    }

    public String getCityId() {
        return Key;
    }

    public String getCityName() {
        return LocalizedName;
    }

    public String getPostalCode() {
        return PrimaryPostalCode;
    }

    public String getState() {
        return AdministrativeArea.LocalizedName != null ? AdministrativeArea.LocalizedName : "";
    }

    public String getCountryId() {
        return Country != null ? Country.ID : "";
    }

    public String getCountryName() {
        return Country != null ? Country.LocalizedName : "";
    }
}
