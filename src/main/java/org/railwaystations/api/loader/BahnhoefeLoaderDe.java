package org.railwaystations.api.loader;

import com.fasterxml.jackson.databind.JsonNode;
import org.railwaystations.api.model.Bahnhof;
import org.railwaystations.api.model.Coordinates;
import org.railwaystations.api.model.Country;
import org.railwaystations.api.model.Photo;

import java.net.URL;
import java.util.Map;

public class BahnhoefeLoaderDe extends BaseBahnhoefeLoader {

    public BahnhoefeLoaderDe(final Country country, final URL photosUrl, final URL bahnhoefeUrl) {
        super(country, photosUrl, bahnhoefeUrl);
    }

    @Override
    protected Bahnhof createBahnhofFromElasticSourceElement(final Map<Integer, Photo> photos, final JsonNode sourceJson) {
        final Integer id = sourceJson.get("BahnhofNr").asInt();
        return new Bahnhof(id,
                getCountry().getCode(),
                sourceJson.get("title").asText(),
                new Coordinates(sourceJson.get("lat").asDouble(),
                        sourceJson.get("lon").asDouble()),
                sourceJson.get("DS100").asText(),
                photos.get(id));
    }

}
