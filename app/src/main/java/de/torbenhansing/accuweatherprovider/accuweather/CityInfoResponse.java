package de.torbenhansing.accuweatherprovider.accuweather;

import java.io.Serializable;

/**
 * Created by tha on 26.10.2017.
 */

public class CityInfoResponse implements Serializable {
    private String id = "";
    private String name = "";
    private Sys sys;

    static class Sys {
        private String country = "";
    }

    public String getCityId() {
        return id;
    }

    public String getCityName() {
        return name;
    }

    public String getCountry() {
        if (sys != null) {
            return sys.country;
        } else {
            return "";
        }
    }
}
