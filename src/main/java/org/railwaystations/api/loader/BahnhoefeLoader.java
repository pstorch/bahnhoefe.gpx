package org.railwaystations.api.loader;

import org.railwaystations.api.model.Bahnhof;
import org.railwaystations.api.model.Country;

import java.util.Map;

public interface BahnhoefeLoader {

    Map<Integer, Bahnhof> loadBahnhoefe();

    Country getCountry();

}
