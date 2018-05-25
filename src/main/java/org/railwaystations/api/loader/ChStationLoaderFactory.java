package org.railwaystations.api.loader;

import com.fasterxml.jackson.annotation.JsonTypeName;
import org.railwaystations.api.monitoring.Monitor;

@JsonTypeName("chLoader")
public class ChStationLoaderFactory extends BaseStationLoaderFactory {

    @Override
    public StationLoader createLoader(final Monitor monitor) {
        return new StationLoaderCh(country, photosUrl, stationsUrl, monitor);
    }

}
