package org.railwaystations.api.loader;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.dropwizard.jackson.Discoverable;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
public interface BahnhoefeLoaderFactory extends Discoverable {
    BahnhoefeLoader createLoader();
}
