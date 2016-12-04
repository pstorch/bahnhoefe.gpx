package github.pstorch.bahnhoefe.service.loader;

import com.fasterxml.jackson.databind.JsonNode;
import github.pstorch.bahnhoefe.service.Bahnhof;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

public class BahnhoefeLoaderCh extends AbstractBahnhoefeLoader {

    private static final String HITS_ELEMENT = "hits";

    public BahnhoefeLoaderCh() {
        this(null, null);
    }

    public BahnhoefeLoaderCh(final URL bahnhoefeUrl, final URL photosUrl) {
        super(bahnhoefeUrl, photosUrl);
    }

    @Override
    public String getCountryCode() {
        return "ch";
    }

    @Override
    protected Map<Integer, String> loadPhotos() throws IOException {
        final Map<Integer, String> photoFlags = new HashMap<>();
        try (InputStream is = photosUrl.openStream()) {
            final JsonNode tree = BahnhoefeLoaderCh.MAPPER.readTree(is);
            for (int i = 0; i < tree.size(); i++) {
                final JsonNode bahnhofPhoto = tree.get(i);
                photoFlags.put(bahnhofPhoto.get("ibnr").asInt(), bahnhofPhoto.get("fotograf-title").asText());
            }
        }
        return photoFlags;
    }

    @Override
    @SuppressWarnings("PMD.AvoidInstantiatingObjectsInLoops")
    protected Map<Integer, Bahnhof> loadBahnhoefe(final Map<Integer, String> photoFlags) throws IOException {
        final Map<Integer, Bahnhof> bahnhoefe = new HashMap<>();
        try (final InputStream is = bahnhoefeUrl.openStream()) {
            final JsonNode hits = BahnhoefeLoaderCh.MAPPER.readTree(is)
                                    .get(BahnhoefeLoaderCh.HITS_ELEMENT)
                                    .get(BahnhoefeLoaderCh.HITS_ELEMENT);
            for (int i = 0; i < hits.size(); i++) {
                final JsonNode bahnhofJson = hits.get(i).get("_source").get("fields");
                final JsonNode geopos = bahnhofJson.get("geopos");
                final Integer id = bahnhofJson.get("nummer").asInt();
                final Bahnhof bahnhof = new Bahnhof(id,
                        bahnhofJson.get("name").asText(),
                        geopos.get(0).asDouble(),
                        geopos.get(1).asDouble(),
                        photoFlags.get(id));
                bahnhoefe.put(bahnhof.getId(), bahnhof);
            }
        }
        return bahnhoefe;
    }

}
