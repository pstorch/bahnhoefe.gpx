package github.pstorch.bahnhoefe.service.loader;

import com.fasterxml.jackson.databind.JsonNode;
import github.pstorch.bahnhoefe.service.model.Bahnhof;

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
    @SuppressWarnings("PMD.AvoidInstantiatingObjectsInLoops")
    protected Map<Integer, Bahnhof> loadBahnhoefe(final Map<Integer, String> photoFlags) throws Exception {
        final Map<Integer, Bahnhof> bahnhoefe = new HashMap<>();

        final JsonNode hits = readJsonFromUrl(bahnhoefeUrl)
                                .get(BahnhoefeLoaderCh.HITS_ELEMENT)
                                .get(BahnhoefeLoaderCh.HITS_ELEMENT);
        for (int i = 0; i < hits.size(); i++) {
            final JsonNode bahnhofJson = hits.get(i).get("_source").get("fields");
            final JsonNode geopos = bahnhofJson.get("geopos");
            final Integer id = bahnhofJson.get("nummer").asInt();
            final Bahnhof bahnhof = new Bahnhof(id,
                    getCountryCode(),
                    bahnhofJson.get("name").asText(),
                    geopos.get(0).asDouble(),
                    geopos.get(1).asDouble(),
                    photoFlags.get(id));
            bahnhoefe.put(bahnhof.getId(), bahnhof);
        }
        return bahnhoefe;
    }

}
