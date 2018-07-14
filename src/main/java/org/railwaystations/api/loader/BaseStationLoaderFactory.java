package org.railwaystations.api.loader;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import org.railwaystations.api.ElasticBackend;
import org.railwaystations.api.model.Country;
import org.railwaystations.api.monitoring.Monitor;

@JsonTypeName("baseLoader")
public class BaseStationLoaderFactory implements StationLoaderFactory {

    @JsonProperty
    protected Country country;

    @JsonProperty
    protected String photosUrl;

    @JsonProperty
    protected String stationsUrl;

    @Override
    public StationLoader createLoader(final Monitor monitor, final ElasticBackend elasticBackend) {
        return new BaseStationLoader(country, photosUrl, stationsUrl, monitor, elasticBackend);
    }

}
