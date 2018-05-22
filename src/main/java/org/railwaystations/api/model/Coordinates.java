package org.railwaystations.api.model;

public class Coordinates {

    private final double lat;

    private final double lon;

    public Coordinates() {
        this(0.0,0.0);
    }

    public Coordinates(final double lat, final double lon) {
        this.lat = lat;
        this.lon = lon;
    }

    public double getLat() {
        return lat;
    }

    public double getLon() {
        return lon;
    }
}
