package org.railwaystations.api.loader;

import com.fasterxml.jackson.annotation.JsonTypeName;

@JsonTypeName("chLoader")
public class ChBahnhoefeLoaderFactory extends BaseBahnhoefeLoaderFactory {

    @Override
    public BahnhoefeLoader createLoader() {
        return new BahnhoefeLoaderCh(country, photosUrl, bahnhoefeUrl);
    }

}
