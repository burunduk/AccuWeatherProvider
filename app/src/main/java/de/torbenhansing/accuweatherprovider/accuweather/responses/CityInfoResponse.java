package de.torbenhansing.accuweatherprovider.accuweather.responses;

import java.io.Serializable;

@SuppressWarnings("FieldCanBeLocal")
public class CityInfoResponse implements Serializable {
    private String Key = "";
    private String LocalizedName = "";
    private String EnglishName = "";
    private Country Country = null;
    private String PrimaryPostalCode = "";
    private AdministrativeArea AdministrativeArea = null;

    private static class Country {
        private String ID = "";
        private String LocalizedName = "";
        private String EnglishName = "";
    }

    private static class AdministrativeArea {
        private String LocalizedName = "";
        private String EnglishName = "";
    }

    public String getCityId() {
        return Key;
    }

    public String getCityName() {
        return !LocalizedName.isEmpty() ? LocalizedName : EnglishName;
    }

    public String getPostalCode() {
        return PrimaryPostalCode;
    }

    public String getState() {
        if (AdministrativeArea != null) {
            return !AdministrativeArea.LocalizedName.isEmpty() ? AdministrativeArea.LocalizedName :
                    AdministrativeArea.EnglishName;
        }
        return "";
    }

    public String getCountryId() {
        return Country != null ? Country.ID : "";
    }

    public String getCountryName() {
        if (Country != null) {
            return !Country.LocalizedName.isEmpty() ? Country.LocalizedName : Country.EnglishName;
        }
        return "";
    }
}
