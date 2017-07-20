package org.railwaystations.api.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Bahnhof {

    private static final int EARTH_RADIUS = 6371;

    @JsonProperty
    private final int id;

    @JsonProperty
    private final String country;

    @JsonProperty
    private final String title;

    @JsonProperty
    private final double lat;

    @JsonProperty
    private final double lon;

    @JsonProperty
    private final String photographer;

    @JsonProperty
    private final String photographerUrl;

    @JsonProperty("DS100")
    private final String ds100;

    @JsonProperty
    private final String photoUrl;

    @JsonProperty
    private final String license;

    public Bahnhof() {
        this(0, null, null, new Coordinates(0.0, 0.0), null);
    }

    public Bahnhof(final int id, final String country, final String title, final Coordinates coordinates, final Photo photo) {
        this(id, country, title, coordinates, null, photo);
    }

    public Bahnhof(final int id, final String country, final String title, final Coordinates coordinates, final String ds100, final Photo photo) {
        super();
        this.id = id;
        this.country = country;
        this.title = title;
        this.lat = coordinates.getLatitude();
        this.lon = coordinates.getLongitude();
        this.ds100 = ds100;
        if (photo != null) {
            this.photographer = photo.getPhotographer();
            this.photoUrl = photo.getUrl();
            this.license = photo.getLicense();
            this.photographerUrl = photo.getPhotographerUrl();
        } else {
            this.photographer = null;
            this.photoUrl = null;
            this.license = null;
            this.photographerUrl = null;
        }
    }

    public int getId() {
        return this.id;
    }

    public String getCountry() { return this.country; }

    public String getTitle() {
        return this.title;
    }

    public double getLat() {
        return this.lat;
    }

    public double getLon() {
        return this.lon;
    }

    public boolean hasPhoto() {
        return this.photographer != null;
    }

    public String getPhotographer() { return this.photographer; }

    /*
     * Calculate distance in km between this objects position and the given latitude and longitude.
     * Uses Haversine method as its base.
     *
     * @returns Distance in km
     */
    public double distanceTo(final double latitude, final double longitude) {
        final Double latDistance = Math.toRadians(latitude - this.lat);
        final Double lonDistance = Math.toRadians(longitude - this.lon);
        final Double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(this.lat)) * Math.cos(Math.toRadians(latitude))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        final Double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return Bahnhof.EARTH_RADIUS * c;
    }

    public boolean appliesTo(final Boolean hasPhoto, final String photographer, final Integer maxDistance, final Double lat, final Double lon) {
        boolean result = true;
        if (hasPhoto != null) {
            result = this.hasPhoto() == hasPhoto;
        }
        if (photographer != null) {
            result &= photographer.equals(this.getPhotographer());
        }
        if (maxDistance != null && lat != null && lon != null) {
            result &= this.distanceTo(lat, lon) < maxDistance;
        }
        return result;
    }

    public String getDS100() {
        return ds100;
    }

    public String getPhotoUrl() {
        return photoUrl;
    }

    public String getLicense() {
        return license;
    }

    public String getPhotographerUrl() {
        return photographerUrl;
    }
}
