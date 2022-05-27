package com.workflow.workflow.user.session;

import java.util.HashMap;
import java.util.Map;

public class MaxmindResponse {
    private City city;
    private Country country;

    public City getCity() {
        return city;
    }

    public void setCity(City city) {
        this.city = city;
    }

    public Country getCountry() {
        return country;
    }

    public void setCountry(Country country) {
        this.country = country;
    }

    public static class City {
        private Map<String, String> names = new HashMap<>();

        public Map<String, String> getNames() {
            return names;
        }

        public void setNames(Map<String, String> names) {
            this.names = names;
        }
    }

    public static class Country {
        private Map<String, String> names = new HashMap<>();

        public Map<String, String> getNames() {
            return names;
        }

        public void setNames(Map<String, String> names) {
            this.names = names;
        }
    }
}
