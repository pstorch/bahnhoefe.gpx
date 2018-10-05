package org.railwaystations.api.loader;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.StringUtils;
import org.railwaystations.api.ElasticBackend;
import org.railwaystations.api.model.*;
import org.railwaystations.api.model.elastic.Bahnhofsfoto;
import org.railwaystations.api.monitoring.Monitor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class StationLoader {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final String SOURCE_ELEMENT = "_source";

    private static final Logger LOG = LoggerFactory.getLogger(StationLoader.class);


    private final Country country;
    private final Monitor monitor;

    private final ElasticBackend elasticBackend;

    public StationLoader(final Country country, final Monitor monitor, final ElasticBackend elasticBackend) {
        super();
        this.country = country;
        this.elasticBackend = elasticBackend;
        this.monitor = monitor;
    }

    public Country getCountry() {
        return country;
    }

    public final Map<Station.Key, Station> loadStations(final Map<String, Photographer> photographers, final String photoBaseUrl) {
        try {
            return fetchStations(fetchPhotos(new HashMap<>(), photographers, photoBaseUrl));
        } catch (final Exception e) {
            monitor.sendMessage("Error loading stations for " + country.getCode() + ": " + e.getMessage());
            LOG.error("Error loading stations for {}", country.getCode(), e);
            return Collections.emptyMap();
        }
    }

    private Map<Station.Key, Photo> fetchPhotos(final Map<Station.Key, Photo> photos, final Map<String, Photographer> photographers, final String photoBaseUrl) throws Exception {
        elasticBackend.fetchAll(country.getPhotosIndex(), 0, hits -> fetchPhotos(photos, photographers, photoBaseUrl, hits));
        return photos;
    }

    private Void fetchPhotos(final Map<Station.Key, Photo> photos, final Map<String, Photographer> photographers, final String photoBaseUrl, final JsonNode hits) {
        for (int i = 0; i < hits.size(); i++) {
            final Photo photo = createPhoto(getMandatoryAttribute(hits.get(i), StationLoader.SOURCE_ELEMENT), photographers, photoBaseUrl);
            if (photos.get(photo.getStationKey()) != null) {
                monitor.sendMessage("Station " + photo.getStationKey() + " has duplicate photos");
            }
            photos.put(photo.getStationKey(), photo);
        }
        return null;
    }

    private Photo createPhoto(final JsonNode photoJson, final Map<String, Photographer> photographers, final String photoBaseUrl) {
        final Bahnhofsfoto bahnhofsfoto;
        try {
            bahnhofsfoto = MAPPER.treeToValue(photoJson, Bahnhofsfoto.class);
        } catch (final JsonProcessingException e) {
            throw new RuntimeException(e);
        }
        return new Photo(new Station.Key(bahnhofsfoto.getCountryCode().toLowerCase(Locale.ENGLISH), bahnhofsfoto.getId()), photoBaseUrl + bahnhofsfoto.getUrl(),
                bahnhofsfoto.getPhotographer(), getPhotographerUrl(bahnhofsfoto.getPhotographer(), photographers),
                bahnhofsfoto.getCreatedAt(), StringUtils.trimToEmpty(bahnhofsfoto.getLicense()), getMandatoryAttribute(photoJson, "flag").asText());
    }

    private String getPhotographerUrl(final String nickname, final Map<String, Photographer> photographers) {
        final Photographer photographer = photographers.get(nickname);
        return photographer != null ? photographer.getUrl() : null;
    }

    private Map<Station.Key, Station> fetchStations(final Map<Station.Key, Photo> photos) throws Exception {
        final Map<Station.Key, Station> stations = new HashMap<>();
        elasticBackend.fetchAll(country.getStationsIndex(), 0, hits -> fetchStations(photos, stations, hits));
        return stations;
    }

    private Void fetchStations(final Map<Station.Key,Photo> photos, final Map<Station.Key, Station> stations, final JsonNode hits) {
        for (int i = 0; i < hits.size(); i++) {
            final Station station = createStationFromElastic(photos, getMandatoryAttribute(hits.get(i), StationLoader.SOURCE_ELEMENT));
            stations.put(station.getKey(), station);
        }
        return null;
    }

    protected Station createStationFromElastic(final Map<Station.Key, Photo> photos, final JsonNode sourceJson) {
        if (sourceJson.has("properties")) {
            final JsonNode propertiesJson = getMandatoryAttribute(sourceJson, "properties");
            final String id = getMandatoryAttribute(propertiesJson, "UICIBNR").asText();
            final JsonNode abkuerzung = propertiesJson.get("abkuerzung");
            final Station.Key key = new Station.Key(getCountry().getCode(), id);
            return new Station(key,
                    getMandatoryAttribute(propertiesJson, "name").asText(),
                    readCoordinates(sourceJson),
                    abkuerzung != null ? abkuerzung.asText() : "",
                    photos.get(key));
        } else {
            // DE Schema
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

    protected static JsonNode getMandatoryAttribute(final JsonNode sourceJson, final String name) {
        final JsonNode jsonNode = sourceJson.get(name);
        if (jsonNode == null) {
            throw new IllegalArgumentException("Json attribute '" + name + "' is missing.");
        }
        return jsonNode;
    }

    protected static Coordinates readCoordinates(final JsonNode json) {
        final JsonNode coordinates = getMandatoryAttribute(getMandatoryAttribute(json,"geometry"), "coordinates");
        return new Coordinates(coordinates.get(1).asDouble(), coordinates.get(0).asDouble());
    }
}
