package com.workflow.workflow.user.session;

import java.util.Objects;

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

    @Override
    public int hashCode() {
        return Objects.hash(getCity(), getCountry());
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (!(obj instanceof GeoIP))
            return false;
        GeoIP other = (GeoIP) obj;
        return Objects.equals(getCity(), other.getCity()) && Objects.equals(getCountry(), other.getCountry());
    }

}
