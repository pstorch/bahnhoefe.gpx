package org.railwaystations.api.loader;

import com.fasterxml.jackson.databind.JsonNode;
import org.apache.commons.lang3.math.NumberUtils;
import org.joda.time.DateTime;
import org.railwaystations.api.BackendHttpClient;
import org.railwaystations.api.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;
import java.util.HashMap;
import java.util.Map;

public class BaseBahnhoefeLoader implements BahnhoefeLoader {

    private static final String HITS_ELEMENT = "hits";
    private static final String SOURCE_ELEMENT = "_source";
    private static final Logger LOG = LoggerFactory.getLogger(BaseBahnhoefeLoader.class);

    private final URL bahnhoefeUrl;
    private final URL photosUrl;
    private final Country country;

    private final BackendHttpClient httpclient;

    BaseBahnhoefeLoader(final Country country, final URL photosUrl, final URL bahnhoefeUrl) {
        super();
        this.country = country;
        this.photosUrl = photosUrl;
        this.bahnhoefeUrl = bahnhoefeUrl;
        this.httpclient = new BackendHttpClient();
    }

    public Country getCountry() {
        return country;
    }

    @Override
    public final Map<Integer, Bahnhof> loadBahnhoefe(final Map<String, Photographer> photographers, final String photoBaseUrl) {
        try {
            return fetchBahnhoefe(fetchPhotos(new HashMap<>(), photographers, photoBaseUrl));
        } catch (final Exception e) {
            throw new RuntimeException(e);
        }
    }

    private Map<Integer, Photo> fetchPhotos(final Map<Integer, Photo> photos, final Map<String, Photographer> photographers, final String photoBaseUrl) throws Exception {
        final JsonNode hits = httpclient.readJsonFromUrl(photosUrl)
                .get(BaseBahnhoefeLoader.HITS_ELEMENT)
                .get(BaseBahnhoefeLoader.HITS_ELEMENT);
        for (int i = 0; i < hits.size(); i++) {
            final Photo photo = createPhotoFromElasticSourceElement(hits.get(i).get(BaseBahnhoefeLoader.SOURCE_ELEMENT), photographers, photoBaseUrl);
            if (photos.get(photo.getStationId()) != null) {
                LOG.info("Photo for Station " + photo.getStationId() + " has duplicates");
            }
            photos.put(photo.getStationId(), photo);
        }
        return photos;
    }

    private Photo createPhotoFromElasticSourceElement(final JsonNode photoJson, final Map<String, Photographer> photographers, final String photoBaseUrl) {
        final JsonNode id = photoJson.get("BahnhofsID");
        final String photographer = photoJson.get("fotografenname").asText();
        final String statUser = "1".equals(photoJson.get("flag").asText()) ? "@RecumbentTravel" : photographer;
        final String url = photoBaseUrl + photoJson.get("bahnhofsfoto").asText();
        final String license = photoJson.get("fotolizenz").asText();
        final String erfasst = photoJson.get("erfasst").asText();
        final long createdAt = NumberUtils.isDigits(erfasst)?Long.parseLong(erfasst):DateTime.parse(erfasst).toDate().getTime();

        return new Photo(id.asInt(), url, photographer, getPhotographerUrl(photographer, photographers), createdAt, license, statUser);
    }

    private String getPhotographerUrl(final String nickname, final Map<String, Photographer> photographers) {
        final Photographer photographer = photographers.get(nickname);
        return photographer != null ? photographer.getUrl() : null;
    }

    private Map<Integer, Bahnhof> fetchBahnhoefe(final Map<Integer, Photo> photos) throws Exception {
        final Map<Integer, Bahnhof> bahnhoefe = new HashMap<>();

        final JsonNode hits = httpclient.readJsonFromUrl(bahnhoefeUrl)
                                .get(BaseBahnhoefeLoader.HITS_ELEMENT)
                                .get(BaseBahnhoefeLoader.HITS_ELEMENT);
        for (int i = 0; i < hits.size(); i++) {
            final Bahnhof bahnhof = createBahnhofFromElasticSourceElement(photos, hits.get(i).get(BaseBahnhoefeLoader.SOURCE_ELEMENT));
            bahnhoefe.put(bahnhof.getId(), bahnhof);
        }
        return bahnhoefe;
    }

    protected Bahnhof createBahnhofFromElasticSourceElement(final Map<Integer, Photo> photos, final JsonNode sourceJson) {
        final JsonNode propertiesJson = sourceJson.get("properties");
        final Integer id = propertiesJson.get("UICIBNR").asInt();
        final JsonNode abkuerzung = propertiesJson.get("abkuerzung");
        return new Bahnhof(id,
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
