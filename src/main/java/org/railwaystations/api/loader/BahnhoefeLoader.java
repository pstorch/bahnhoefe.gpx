package org.railwaystations.api.loader;

import org.railwaystations.api.model.Bahnhof;
import org.railwaystations.api.model.Country;

import java.net.MalformedURLException;
import java.util.Map;

public interface BahnhoefeLoader {
    Map<Integer, Bahnhof> loadBahnhoefe();

    void setBahnhoefeUrl(final String bahnhoefeUrl) throws MalformedURLException;

    void setPhotosUrl(final String photosUrl) throws MalformedURLException;

    Country getCountry();

}
