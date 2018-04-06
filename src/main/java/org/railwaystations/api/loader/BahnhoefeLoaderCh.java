package org.railwaystations.api.loader;

import com.fasterxml.jackson.databind.JsonNode;
import org.railwaystations.api.model.Station;
import org.railwaystations.api.model.Country;
import org.railwaystations.api.model.Photo;

import java.net.URL;
import java.util.Map;

public class BahnhoefeLoaderCh extends BaseBahnhoefeLoader {

    public BahnhoefeLoaderCh(final Country country, final URL photosUrl, final URL bahnhoefeUrl) {
        super(country, photosUrl, bahnhoefeUrl);
    }

    @Override
    protected Station createBahnhofFromElasticSourceElement(final Map<Integer, Photo> photos, final JsonNode sourceJson) {
        final JsonNode fieldsJson = sourceJson.get("fields");
        final Integer id = fieldsJson.get("nummer").asInt();
        final JsonNode abkuerzung = fieldsJson.get("abkuerzung");
        return new Station(id,
                getCountry().getCode(),
                fieldsJson.get("name").asText(),
                readCoordinates(sourceJson),
                abkuerzung != null ? abkuerzung.asText() : null,
                photos.get(id));
    }

}
