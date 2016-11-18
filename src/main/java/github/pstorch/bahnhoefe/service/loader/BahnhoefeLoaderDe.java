package github.pstorch.bahnhoefe.service.loader;

import com.fasterxml.jackson.databind.JsonNode;
import github.pstorch.bahnhoefe.service.Bahnhof;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

public class BahnhoefeLoaderDe extends AbstractBahnhoefeLoader {

    public static final String COUNTRY_CODE = "de";

    private static final String TITLE_ELEMENT = "title";

    private static final String LON_ELEMENT = "lon";

    private static final String LAT_ELEMENT = "lat";

    private static final String HITS_ELEMENT = "hits";

    public BahnhoefeLoaderDe() {
        this(null, null);
    }

    public BahnhoefeLoaderDe(final URL bahnhoefeUrl, final URL photosUrl) {
        super(photosUrl, bahnhoefeUrl);
    }

    @Override
    public String getCountryCode() {
        return BahnhoefeLoaderDe.COUNTRY_CODE;
    }

    @Override
    protected void loadPhotoFlags(final Map<Integer, Bahnhof> bahnhoefe) throws IOException {
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
    @Override
    protected Map<Integer, Bahnhof> loadAllBahnhoefe() throws IOException {
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
