package org.railwaystations.api.model;

public class Photo {

    private final int stationId;

    private final String photographer;

    private final String url;

    private final String license;

    private final String photographerUrl;

    public Photo(final int stationId, final String photographer, final String url, final String license, final String photographerUrl) {
        this.stationId = stationId;
        this.photographer = photographer;
        this.url = url;
        this.license = license;
        this.photographerUrl = photographerUrl;
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
}
