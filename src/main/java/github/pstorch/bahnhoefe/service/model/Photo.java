package github.pstorch.bahnhoefe.service.model;

public class Photo {

    private final int stationId;

    private final String photographer;

    private final String url;

    public Photo(final int stationId, final String photographer, final String url) {
        this.stationId = stationId;
        this.photographer = photographer;
        this.url = url;
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

}
