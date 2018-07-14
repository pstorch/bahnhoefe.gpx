package org.railwaystations.api.loader;

import com.fasterxml.jackson.databind.JsonNode;
import org.railwaystations.api.ElasticBackend;
import org.railwaystations.api.model.Coordinates;
import org.railwaystations.api.model.Country;
import org.railwaystations.api.model.Photo;
import org.railwaystations.api.model.Station;
import org.railwaystations.api.monitoring.Monitor;

import java.util.Map;

public class StationLoaderDe extends BaseStationLoader {

    public StationLoaderDe(final Country country, final String photosUrl, final String stationsUrl, final Monitor monitor, final ElasticBackend elasticBackend) {
        super(country, photosUrl, stationsUrl, monitor, elasticBackend);
    }

    @Override
    protected Station createStationFromElastic(final Map<Station.Key, Photo> photos, final JsonNode sourceJson) {
        final String id = sourceJson.get("BahnhofNr").asText();
        final Station.Key key = new Station.Key(getCountry().getCode(), id);
        return new Station(key,
                sourceJson.get("title").asText(),
                new Coordinates(sourceJson.get("lat").asDouble(),
                        sourceJson.get("lon").asDouble()),
                sourceJson.get("DS100").asText(),
                photos.get(key));
    }

}
