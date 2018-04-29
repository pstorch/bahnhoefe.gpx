package org.railwaystations.api.loader;

import com.fasterxml.jackson.annotation.JsonTypeName;

@JsonTypeName("chLoader")
public class ChStationLoaderFactory extends BaseStationLoaderFactory {

    @Override
    public StationLoader createLoader() {
        return new StationLoaderCh(country, photosUrl, stationsUrl);
    }

}
