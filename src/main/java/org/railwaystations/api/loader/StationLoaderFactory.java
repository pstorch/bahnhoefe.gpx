package org.railwaystations.api.loader;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.dropwizard.jackson.Discoverable;
import org.railwaystations.api.ElasticBackend;
import org.railwaystations.api.monitoring.Monitor;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
public interface StationLoaderFactory extends Discoverable {
    StationLoader createLoader(final Monitor monitor, final ElasticBackend elasticBackend);
}
