package org.railwaystations.api.model;

public class Photo {

    private final int stationId;
    private final String url;
    private final String photographer;
    private final String photographerUrl;
    private final Long createdAt;
    private final String license;
    private final String statUser;

    public Photo(final int stationId, final String url, final String photographer, final String photographerUrl, final Long createdAt, final String license) {
        this(stationId, url, photographer, photographerUrl, createdAt, license, photographer);
    }

    public Photo(final int stationId, final String url, final String photographer, final String photographerUrl, final Long createdAt, final String license, final String statUser) {
        this.stationId = stationId;
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

    public int getStationId() {
        return stationId;
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
