package org.railwaystations.api.loader;

import com.fasterxml.jackson.annotation.JsonTypeName;
import org.railwaystations.api.monitoring.Monitor;

@JsonTypeName("deLoader")
public class DeStationLoaderFactory extends BaseStationLoaderFactory {

    @Override
    public StationLoader createLoader(final Monitor monitor) {
        return new StationLoaderDe(country, photosUrl, stationsUrl, monitor);
    }

}
