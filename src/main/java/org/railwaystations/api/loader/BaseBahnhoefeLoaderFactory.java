package org.railwaystations.api.loader;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import org.railwaystations.api.model.Country;

import java.net.URL;

@JsonTypeName("baseLoader")
public class BaseBahnhoefeLoaderFactory implements BahnhoefeLoaderFactory {

    @JsonProperty
    protected Country country;

    @JsonProperty
    protected URL photosUrl;

    @JsonProperty
    protected URL bahnhoefeUrl;

    @Override
    public BahnhoefeLoader createLoader() {
        return new BaseBahnhoefeLoader(country, photosUrl, bahnhoefeUrl);
    }

}
