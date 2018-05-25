package org.railwaystations.api.loader;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import org.railwaystations.api.model.Country;
import org.railwaystations.api.monitoring.Monitor;

import java.net.URL;

@JsonTypeName("baseLoader")
public class BaseStationLoaderFactory implements StationLoaderFactory {

    @JsonProperty
    protected Country country;

    @JsonProperty
    protected URL photosUrl;

    @JsonProperty
    protected URL stationsUrl;

    @Override
    public StationLoader createLoader(final Monitor monitor) {
        return new BaseStationLoader(country, photosUrl, stationsUrl, monitor);
    }

}
