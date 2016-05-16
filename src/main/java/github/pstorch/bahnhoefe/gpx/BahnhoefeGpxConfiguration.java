package github.pstorch.bahnhoefe.gpx;

import io.dropwizard.Configuration;

public class BahnhoefeGpxConfiguration extends Configuration {

    private String bahnhoefeUrl;

    private String photosUrl;

    public String getBahnhoefeUrl() {
	return bahnhoefeUrl;
    }

    public void setBahnhoefeUrl(final String bahnhoefeUrl) {
	this.bahnhoefeUrl = bahnhoefeUrl;
    }

    public String getPhotosUrl() {
	return photosUrl;
    }

    public void setPhotosUrl(final String photosUrl) {
	this.photosUrl = photosUrl;
    }

}
