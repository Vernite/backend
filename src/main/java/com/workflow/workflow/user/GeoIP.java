package com.workflow.workflow.user;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class GeoIP {
    private String city;
    private String country;
    @JsonIgnore
    private long cache;

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public long getCache() {
        return cache;
    }

    public void setCache(long cache) {
        this.cache = cache;
    }

}
