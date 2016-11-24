package github.pstorch.bahnhoefe.service.loader;

import github.pstorch.bahnhoefe.service.Bahnhof;

import java.net.MalformedURLException;
import java.util.Iterator;
import java.util.Map;
import java.util.function.Predicate;

public interface BahnhoefeLoader {
    Map<Integer, Bahnhof> loadBahnhoefe();

    void setBahnhoefeUrl(final String bahnhoefeUrl) throws MalformedURLException;

    void setPhotosUrl(final String photosUrl) throws MalformedURLException;

    String getCountryCode();

    Iterator<Bahnhof> filter(final Predicate<Bahnhof> predicate);

}
