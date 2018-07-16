package org.railwaystations.api.loader;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import org.railwaystations.api.ElasticBackend;
import org.railwaystations.api.model.Country;
import org.railwaystations.api.monitoring.Monitor;

@JsonTypeName("deLoader")
public class DeStationLoaderFactory extends BaseStationLoaderFactory {

    public DeStationLoaderFactory(@JsonProperty("country") final Country country,
                                  @JsonProperty("photosUrl") final String photosUrl,
                                  @JsonProperty("stationsUrl") final String stationsUrl) {
        super(country, photosUrl, stationsUrl);
    }

    @Override
    public StationLoader createLoader(final Monitor monitor, final ElasticBackend elasticBackend) {
        return new StationLoaderDe(country, photosUrl, stationsUrl, monitor, elasticBackend);
    }

}
