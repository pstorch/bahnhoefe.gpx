package org.railwaystations.api.loader;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.railwaystations.api.model.Bahnhof;
import org.railwaystations.api.model.Coordinates;
import org.railwaystations.api.model.Country;
import org.railwaystations.api.model.Photo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;
import java.util.HashMap;
import java.util.Map;

public class BaseBahnhoefeLoader implements BahnhoefeLoader {

    private static final String HITS_ELEMENT = "hits";
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final JsonFactory FACTORY = MAPPER.getFactory();

    private static final Logger LOG = LoggerFactory.getLogger(BaseBahnhoefeLoader.class);

    private final URL bahnhoefeUrl;
    private final URL photosUrl;
    private final Country country;

    private final CloseableHttpClient httpclient;

    BaseBahnhoefeLoader(final Country country, final URL photosUrl, final URL bahnhoefeUrl) {
        super();
        this.country = country;
        this.photosUrl = photosUrl;
        this.bahnhoefeUrl = bahnhoefeUrl;
        this.httpclient = HttpClients.custom().setDefaultRequestConfig(
                RequestConfig.custom()
                        .setSocketTimeout(5000)
                        .setConnectTimeout(5000)
                        .setConnectionRequestTimeout(15000).build()
        ).build();
    }

    public Country getCountry() {
        return country;
    }

    @Override
    public final Map<Integer, Bahnhof> loadBahnhoefe() {
        try {
            return loadBahnhoefe(loadPhotos(new HashMap<>(), 0));
        } catch (final Exception e) {
            throw new RuntimeException(e);
        }
    }

    private JsonNode readJsonFromUrl(final URL url) throws Exception {
        // shortcut for testing, need to find a better way
        if ("file".equals(url.getProtocol())) {
            return MAPPER.readTree(url);
        }

        // use Apache HTTP Client to retrieve remote content
        final HttpGet httpGet = new HttpGet(url.toURI());
        return httpclient.execute(httpGet, response -> {
            final int status = response.getStatusLine().getStatusCode();
            if (status >= 200 && status < 300) {
                LOG.info("Got json response from {}", url);
                return MAPPER.readTree(FACTORY.createParser(EntityUtils.toString(response.getEntity(), "UTF-8")));
            } else {
                LOG.error("Error reading json from {}", url);
                throw new ClientProtocolException(String.format("Unexpected response status: %d", status));
            }
        });
    }

    @SuppressWarnings("PMD.AvoidInstantiatingObjectsInLoops")
    private Map<Integer, Photo> loadPhotos(final Map<Integer, Photo> photos, final int page) throws Exception {
        final JsonNode tree = readJsonFromUrl(photosUrl.getProtocol().startsWith("http")?new URL(photosUrl.toString() + "?page=" + page):photosUrl);
        for (int i = 0; i < tree.size(); i++) {
            final JsonNode bahnhofPhoto = tree.get(i);
            JsonNode id = bahnhofPhoto.get("ibnr");
            if (id == null || StringUtils.isBlank(id.asText())) {
                id = bahnhofPhoto.get("bahnhofsnr");
            }
            final String photograph = bahnhofPhoto.get("fotograf-title").asText();
            final String photographerUrl = bahnhofPhoto.get("fotograf").asText();
            final String statUser = "1".equals(bahnhofPhoto.get("flagr").asText()) ? "@RecumbentTravel" : photograph;
            final String url = bahnhofPhoto.get("bahnhofsfoto").asText();
            final String license = bahnhofPhoto.get("lizenz").asText();
            photos.put(id.asInt(), new Photo(id.asInt(), photograph, url, license, photographerUrl, statUser));
        }
        if (photosUrl.getProtocol().startsWith("http") && tree.size() > 0) {
            loadPhotos(photos, page + 1);
        }
        return photos;
    }

    private Map<Integer, Bahnhof> loadBahnhoefe(final Map<Integer, Photo> photos) throws Exception {
        final Map<Integer, Bahnhof> bahnhoefe = new HashMap<>();

        final JsonNode hits = readJsonFromUrl(bahnhoefeUrl)
                                .get(BaseBahnhoefeLoader.HITS_ELEMENT)
                                .get(BaseBahnhoefeLoader.HITS_ELEMENT);
        for (int i = 0; i < hits.size(); i++) {
            final JsonNode sourceJson = hits.get(i).get("_source");
            final Bahnhof bahnhof = createBahnhofFromElasticSourceElement(photos, sourceJson);
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
