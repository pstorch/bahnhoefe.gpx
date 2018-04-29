package org.railwaystations.api.model;

public class Coordinates {

    private final double latitude;

    private final double longitude;

    public Coordinates(final double latitude, final double longitude) {
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }
}
