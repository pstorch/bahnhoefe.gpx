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
    protected void loadPhotoFlags(final Map<Integer, Bahnhof> bahnhoefe) throws IOException {
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

    @Override
    protected Map<Integer, Bahnhof> loadAllBahnhoefe() throws IOException {
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
