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

public class BahnhoefeLoaderCh implements BahnhoefeLoader {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static final String HITS_ELEMENT = "hits";

    private static final Logger LOG = LoggerFactory.getLogger(BahnhoefeLoaderCh.class);

    private final Supplier<Map<Integer, Bahnhof>> cache;

    private URL bahnhoefeUrl;
    private URL photosUrl;

    public BahnhoefeLoaderCh() {
        this(null, null);
    }

    public BahnhoefeLoaderCh(final URL bahnhoefeUrl, final URL photosUrl) {
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
        return "ch";
    }

    @SuppressWarnings("PMD.AvoidThrowingRawExceptionTypes")
    private Supplier<Map<Integer, Bahnhof>> bahnhoefeSupplier() {
        return () -> {
            BahnhoefeLoaderCh.LOG.info("Loading Bahnhoefe from bahnhoefe={}, photos={}", bahnhoefeUrl, photosUrl);
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
            final JsonNode tree = BahnhoefeLoaderCh.MAPPER.readTree(is);
            for (int i = 0; i < tree.size(); i++) {
                final JsonNode bahnhofPhoto = tree.get(i);
                final int bahnhofsNr = bahnhofPhoto.get("uic_ref").asInt();
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
            final JsonNode tree = BahnhoefeLoaderCh.MAPPER.readTree(is);
            final JsonNode hits = tree.get(BahnhoefeLoaderCh.HITS_ELEMENT).get(BahnhoefeLoaderCh.HITS_ELEMENT);
            for (int i = 0; i < hits.size(); i++) {
                final JsonNode hit = hits.get(i);
                final JsonNode bahnhofJson = hit.get("_source").get("fields");
                final JsonNode geopos = bahnhofJson.get("geopos");
                final Bahnhof bahnhof = new Bahnhof(bahnhofJson.get("nummer").asInt(),
                        bahnhofJson.get("name").asText(),
                        geopos.get(0).asDouble(),
                        geopos.get(1).asDouble());
                bahnhoefe.put(bahnhof.getId(), bahnhof);
            }
        }
        return bahnhoefe;
    }

}
