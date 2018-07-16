package org.railwaystations.api.loader;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import org.railwaystations.api.ElasticBackend;
import org.railwaystations.api.model.Country;
import org.railwaystations.api.monitoring.Monitor;

@JsonTypeName("baseLoader")
public class BaseStationLoaderFactory implements StationLoaderFactory {

    protected final Country country;

    protected final String photosUrl;

    protected final String stationsUrl;

    public BaseStationLoaderFactory(@JsonProperty("country") final Country country,
                                    @JsonProperty("photosUrl") final String photosUrl,
                                    @JsonProperty("stationsUrl") final String stationsUrl) {
        this.country = country;
        this.photosUrl = photosUrl;
        this.stationsUrl = stationsUrl;
    }

    @Override
    public StationLoader createLoader(final Monitor monitor, final ElasticBackend elasticBackend) {
        return new BaseStationLoader(country, photosUrl, stationsUrl, monitor, elasticBackend);
    }

}
