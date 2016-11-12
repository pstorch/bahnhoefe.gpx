package github.pstorch.bahnhoefe.service;

public class Bahnhof {

    private int id;

    private String title;

    private double lat;

    private double lon;

    private String fotograf;

    public Bahnhof(final int id, final String title, final double lat, final double lon, final String fotograf) {
        super();
        this.id = id;
        this.title = title;
        this.lat = lat;
        this.lon = lon;
        this.fotograf = fotograf;
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
        return this.fotograf != null;
    }

    public void setFotograf(final String fotograf) {
        this.fotograf = fotograf;
    }

    public String getFotograf() { return this.fotograf; }
}
