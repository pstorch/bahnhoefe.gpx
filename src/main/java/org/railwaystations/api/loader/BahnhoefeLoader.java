package org.railwaystations.api.loader;

import org.railwaystations.api.model.Station;
import org.railwaystations.api.model.Country;
import org.railwaystations.api.model.Photographer;

import java.util.Map;

public interface BahnhoefeLoader {

    Map<Integer, Station> loadBahnhoefe(final Map<String, Photographer> photographers, String photoBaseUrl);

    Country getCountry();

}
