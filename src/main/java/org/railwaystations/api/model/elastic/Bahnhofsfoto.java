package org.railwaystations.api.model.elastic;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.railwaystations.api.model.Photo;

import java.net.MalformedURLException;
import java.net.URL;

public class Bahnhofsfoto {

    @JsonProperty("BahnhofsID")
    private String id;

    @JsonProperty("bahnhofsfoto")
    private String url;

    @JsonProperty("fotolizenz")
    private String license;

    @JsonProperty("fotografenid")
    private Integer photographerId;

    @JsonProperty("erfasst")
    private long createdAt;

    @JsonProperty("laenderkennzeichen")
    private String countryCode;

    public Bahnhofsfoto() {
        super();
    }

    public Bahnhofsfoto(final Photo photo) throws MalformedURLException {
        this(photo.getStationKey().getId(), new URL(photo.getUrl()).getPath(),
                photo.getLicense(), photo.getPhotographer().getId(), photo.getCreatedAt(), photo.getStationKey().getCountry());
    }

    public Bahnhofsfoto(final String id, final String url, final String license, final Integer photographerId, final long createdAt, final String countryCode) {
        this.id = id;
        this.url = url;
        this.license = license;
        this.photographerId = photographerId;
        this.createdAt = createdAt;
        this.countryCode = countryCode;
    }

    public String getId() {
        return id;
    }

    public void setId(final String id) {
        this.id = id;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(final String url) {
        this.url = url;
    }

    public String getLicense() {
        return license;
    }

    public void setLicense(final String license) {
        this.license = license;
    }

    public Integer getPhotographerId() {
        return photographerId;
    }

    public void setPhotographer(final Integer photographerId) {
        this.photographerId = photographerId;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(final long createdAt) {
        this.createdAt = createdAt;
    }

    public String getCountryCode() {
        return countryCode;
    }

    public void setCountryCode(final String countryCode) {
        this.countryCode = countryCode;
    }
}
