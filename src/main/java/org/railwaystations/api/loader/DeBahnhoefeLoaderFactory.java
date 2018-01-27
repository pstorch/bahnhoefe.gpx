package org.railwaystations.api.loader;

import com.fasterxml.jackson.annotation.JsonTypeName;

@JsonTypeName("deLoader")
public class DeBahnhoefeLoaderFactory extends BaseBahnhoefeLoaderFactory {

    @Override
    public BahnhoefeLoader createLoader() {
        return new BahnhoefeLoaderDe(country, photosUrl, bahnhoefeUrl);
    }

}
