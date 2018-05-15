package org.railwaystations.api.loader;

import com.fasterxml.jackson.databind.JsonNode;
import org.railwaystations.api.model.Station;
import org.railwaystations.api.model.Coordinates;
import org.railwaystations.api.model.Country;
import org.railwaystations.api.model.Photo;

import java.net.URL;
import java.util.Map;

public class StationLoaderDe extends BaseStationLoader {

    public StationLoaderDe(final Country country, final URL photosUrl, final URL stationsUrl) {
        super(country, photosUrl, stationsUrl);
    }

    @Override
    protected Station createStationFromElastic(final Map<Integer, Photo> photos, final JsonNode sourceJson) {
        final Integer id = sourceJson.get("BahnhofNr").asInt();
        return new Station(id,
                getCountry().getCode(),
                sourceJson.get("title").asText(),
                new Coordinates(sourceJson.get("lat").asDouble(),
                        sourceJson.get("lon").asDouble()),
                sourceJson.get("DS100").asText(),
                photos.get(id));
    }

}
