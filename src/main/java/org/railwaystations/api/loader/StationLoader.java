package org.railwaystations.api.loader;

import org.railwaystations.api.model.Station;
import org.railwaystations.api.model.Country;
import org.railwaystations.api.model.Photographer;

import java.util.Map;

public interface StationLoader {

    Map<Station.Key, Station> loadStations(final Map<String, Photographer> photographers, String photoBaseUrl);

    Country getCountry();

}
