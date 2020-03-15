package org.railwaystations.api.model;

public class Coordinates {

    public static final double ZERO = 0.0;
    private final double lat;

    private final double lon;

    public Coordinates() {
        this(ZERO,ZERO);
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

    public boolean hasNullCoords() {
        return lat == ZERO && lon == ZERO;
    }
}
