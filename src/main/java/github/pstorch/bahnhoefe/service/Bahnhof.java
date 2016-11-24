package github.pstorch.bahnhoefe.service;

public class Bahnhof {

    private static final int EARTH_RADIUS = 6371;

    private int id;

    private String title;

    private double lat;

    private double lon;

    private String photographer;

    public Bahnhof(final int id, final String title, final double lat, final double lon, final String photographer) {
        super();
        this.id = id;
        this.title = title;
        this.lat = lat;
        this.lon = lon;
        this.photographer = photographer;
    }

    public Bahnhof(final int id, final String title, final double lat, final double lon) {
        this(id, title, lat, lon, null);
    }

    public int getId() {
        return this.id;
    }

    public void setId(final int id) {
        this.id = id;
    }

    public String getTitle() {
        return this.title;
    }

    public void setTitle(final String title) {
        this.title = title;
    }

    public double getLat() {
        return this.lat;
    }

    public void setLat(final double lat) {
        this.lat = lat;
    }

    public double getLon() {
        return this.lon;
    }

    public void setLon(final double lon) {
        this.lon = lon;
    }

    public boolean hasPhoto() {
        return this.photographer != null;
    }

    public void setPhotographer(final String photographer) {
        this.photographer = photographer;
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
}
