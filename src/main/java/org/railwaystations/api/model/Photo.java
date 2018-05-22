package org.railwaystations.api.model;

public class Photo {

    private final Station.Key stationKey;
    private final String url;
    private final String photographer;
    private final String photographerUrl;
    private final Long createdAt;
    private final String license;
    private final String statUser;

    public Photo(final Station.Key stationKey, final String url, final String photographer, final String photographerUrl, final Long createdAt, final String license) {
        this(stationKey, url, photographer, photographerUrl, createdAt, license, photographer);
    }

    public Photo(final Station.Key stationKey, final String url, final String photographer, final String photographerUrl, final Long createdAt, final String license, final String statUser) {
        this.stationKey = stationKey;
        this.url = url;
        this.photographer = photographer;
        this.photographerUrl = photographerUrl;
        this.createdAt = createdAt;
        this.license = license;
        this.statUser = statUser;
    }

    public String getUrl() {
        return url;
    }

    public String getPhotographer() {
        return photographer;
    }

    public Station.Key getStationKey() {
        return stationKey;
    }

    public String getLicense() {
        return license;
    }

    public String getPhotographerUrl() {
        return photographerUrl;
    }

    public String getStatUser() {
        return statUser;
    }

    public Long getCreatedAt() {
        return createdAt;
    }
}
