package org.railwaystations.api.loader;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.StringUtils;
import org.railwaystations.api.BackendHttpClient;
import org.railwaystations.api.model.*;
import org.railwaystations.api.model.elastic.Bahnhofsfoto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

public class BaseStationLoader implements StationLoader {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final String HITS_ELEMENT = "hits";
    private static final String SOURCE_ELEMENT = "_source";
    private static final Logger LOG = LoggerFactory.getLogger(BaseStationLoader.class);

    private final URL stationsUrl;
    private final URL photosUrl;
    private final Country country;

    private final BackendHttpClient httpclient;

    BaseStationLoader(final Country country, final URL photosUrl, final URL stationsUrl) {
        super();
        this.country = country;
        this.photosUrl = photosUrl;
        this.stationsUrl = stationsUrl;
        this.httpclient = new BackendHttpClient();
    }

    public Country getCountry() {
        return country;
    }

    @Override
    public final Map<Integer, Station> loadStations(final Map<String, Photographer> photographers, final String photoBaseUrl) {
        try {
            return fetchStations(fetchPhotos(new HashMap<>(), photographers, photoBaseUrl));
        } catch (final Exception e) {
            throw new RuntimeException(e);
        }
    }

    private Map<Integer, Photo> fetchPhotos(final Map<Integer, Photo> photos, final Map<String, Photographer> photographers, final String photoBaseUrl) throws Exception {
        final JsonNode hits = httpclient.readJsonFromUrl(photosUrl)
                .get(BaseStationLoader.HITS_ELEMENT)
                .get(BaseStationLoader.HITS_ELEMENT);
        for (int i = 0; i < hits.size(); i++) {
            final Photo photo = createPhoto(hits.get(i).get(BaseStationLoader.SOURCE_ELEMENT), photographers, photoBaseUrl);
            if (photos.get(photo.getStationId()) != null) {
                LOG.info("Photo for Station " + photo.getStationId() + " has duplicates");
            }
            photos.put(photo.getStationId(), photo);
        }
        return photos;
    }

    private Photo createPhoto(final JsonNode photoJson, final Map<String, Photographer> photographers, final String photoBaseUrl) throws IOException {
        final Bahnhofsfoto bahnhofsfoto = MAPPER.treeToValue(photoJson, Bahnhofsfoto.class);
        final String statUser = "1".equals(photoJson.get("flag").asText()) ? "@RecumbentTravel" : bahnhofsfoto.getPhotographer();
        return new Photo(bahnhofsfoto.getId(), photoBaseUrl + bahnhofsfoto.getUrl(),
                bahnhofsfoto.getPhotographer(), getPhotographerUrl(bahnhofsfoto.getPhotographer(), photographers),
                bahnhofsfoto.getCreatedAt(), StringUtils.trimToEmpty(bahnhofsfoto.getLicense()), statUser);
    }

    private String getPhotographerUrl(final String nickname, final Map<String, Photographer> photographers) {
        final Photographer photographer = photographers.get(nickname);
        return photographer != null ? photographer.getUrl() : null;
    }

    private Map<Integer, Station> fetchStations(final Map<Integer, Photo> photos) throws Exception {
        final Map<Integer, Station> stations = new HashMap<>();

        final JsonNode hits = httpclient.readJsonFromUrl(stationsUrl)
                                .get(BaseStationLoader.HITS_ELEMENT)
                                .get(BaseStationLoader.HITS_ELEMENT);
        for (int i = 0; i < hits.size(); i++) {
            final Station station = createStationFromElastic(photos, hits.get(i).get(BaseStationLoader.SOURCE_ELEMENT));
            stations.put(station.getId(), station);
        }
        return stations;
    }

    protected Station createStationFromElastic(final Map<Integer, Photo> photos, final JsonNode sourceJson) {
        final JsonNode propertiesJson = sourceJson.get("properties");
        final Integer id = propertiesJson.get("UICIBNR").asInt();
        final JsonNode abkuerzung = propertiesJson.get("abkuerzung");
        return new Station(id,
                getCountry().getCode(),
                propertiesJson.get("name").asText(),
                readCoordinates(sourceJson),
                abkuerzung != null ? abkuerzung.asText() : "",
                photos.get(id));
    }

    Coordinates readCoordinates(final JsonNode json) {
        final JsonNode coordinates = json.get("geometry").get("coordinates");
        return new Coordinates(coordinates.get(1).asDouble(), coordinates.get(0).asDouble());
    }
}
