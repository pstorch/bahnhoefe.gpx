package github.pstorch.bahnhoefe.service.loader;

import github.pstorch.bahnhoefe.service.Bahnhof;

import java.net.MalformedURLException;
import java.util.Map;

public interface BahnhoefeLoader {
    Map<Integer, Bahnhof> loadBahnhoefe();

    void setBahnhoefeUrl(final String bahnhoefeUrl) throws MalformedURLException;

    void setPhotosUrl(final String photosUrl) throws MalformedURLException;

    String getCountryCode();

}
