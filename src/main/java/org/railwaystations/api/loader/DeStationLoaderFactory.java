package org.railwaystations.api.loader;

import com.fasterxml.jackson.annotation.JsonTypeName;

@JsonTypeName("deLoader")
public class DeStationLoaderFactory extends BaseStationLoaderFactory {

    @Override
    public StationLoader createLoader() {
        return new StationLoaderDe(country, photosUrl, stationsUrl);
    }

}
