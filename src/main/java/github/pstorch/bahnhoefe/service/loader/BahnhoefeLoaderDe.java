package github.pstorch.bahnhoefe.service.loader;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import github.pstorch.bahnhoefe.service.Bahnhof;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class BahnhoefeLoaderDe implements BahnhoefeLoader {

    public static final String COUNTRY_CODE = "de";

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static final String TITLE_ELEMENT = "title";

    private static final String LON_ELEMENT = "lon";

    private static final String LAT_ELEMENT = "lat";

    private static final String HITS_ELEMENT = "hits";

    private static final Logger LOG = LoggerFactory.getLogger(BahnhoefeLoaderDe.class);

    private final Supplier<Map<Integer, Bahnhof>> cache;

    private URL bahnhoefeUrl;
    private URL photosUrl;

    public BahnhoefeLoaderDe() {
        this(null, null);
    }

    public BahnhoefeLoaderDe(final URL bahnhoefeUrl, final URL photosUrl) {
        super();
        this.cache = Suppliers.memoizeWithExpiration(bahnhoefeSupplier(), 5, TimeUnit.MINUTES);
        this.bahnhoefeUrl = bahnhoefeUrl;
        this.photosUrl = photosUrl;
    }

    public void setBahnhoefeUrl(final String bahnhoefeUrl) throws MalformedURLException {
        this.bahnhoefeUrl = new URL(bahnhoefeUrl);
    }

    public void setPhotosUrl(final String photosUrl) throws MalformedURLException {
        this.photosUrl = new URL(photosUrl);
    }

    @Override
    public String getCountryCode() {
        return BahnhoefeLoaderDe.COUNTRY_CODE;
    }

    @SuppressWarnings("PMD.AvoidThrowingRawExceptionTypes")
    private Supplier<Map<Integer, Bahnhof>> bahnhoefeSupplier() {
        return () -> {
            LOG.info("Loading Bahnhoefe from bahnhoefe={}, photos={}", bahnhoefeUrl, photosUrl);
            Map<Integer, Bahnhof> bahnhoefe = null;
            try {
                bahnhoefe = loadAllBahnhoefe();
                loadPhotoFlags(bahnhoefe);
            } catch (final IOException e) {
                throw new RuntimeException("Unable to load all Bahnhoefe", e);
            }
            return bahnhoefe;
        };
    }

    @Override
    public Map<Integer, Bahnhof> loadBahnhoefe() {
        return cache.get();
    }

    private void loadPhotoFlags(final Map<Integer, Bahnhof> bahnhoefe) throws IOException {
        try (InputStream is = photosUrl.openStream()) {
            final JsonNode tree = BahnhoefeLoaderDe.MAPPER.readTree(is);
            for (int i = 0; i < tree.size(); i++) {
                final JsonNode bahnhofPhoto = tree.get(i);
                final int bahnhofsNr = bahnhofPhoto.get("bahnhofsnr").asInt();
                final Bahnhof bahnhof = bahnhoefe.get(bahnhofsNr);
                if (bahnhof != null) {
                    bahnhof.setPhotographer(bahnhofPhoto.get("fotograf-title").asText());
                }
            }
        }
    }

    @SuppressWarnings("PMD.AvoidInstantiatingObjectsInLoops")
    private Map<Integer, Bahnhof> loadAllBahnhoefe() throws IOException {
        final Map<Integer, Bahnhof> bahnhoefe = new HashMap<>();

        try (InputStream is = bahnhoefeUrl.openStream()) {
            final JsonNode tree = BahnhoefeLoaderDe.MAPPER.readTree(is);
            final JsonNode hits = tree.get(BahnhoefeLoaderDe.HITS_ELEMENT).get(BahnhoefeLoaderDe.HITS_ELEMENT);
            for (int i = 0; i < hits.size(); i++) {
                final JsonNode hit = hits.get(i);
                final JsonNode bahnhofJson = hit.get("_source");
                final Bahnhof bahnhof = new Bahnhof(bahnhofJson.get("BahnhofNr").asInt(),
                        bahnhofJson.get(BahnhoefeLoaderDe.TITLE_ELEMENT).asText(),
                        bahnhofJson.get(BahnhoefeLoaderDe.LAT_ELEMENT).asDouble(),
                        bahnhofJson.get(BahnhoefeLoaderDe.LON_ELEMENT).asDouble());
                bahnhoefe.put(bahnhof.getId(), bahnhof);
            }
        }
        return bahnhoefe;
    }

}
