package github.pstorch.bahnhoefe.service.loader;

import com.fasterxml.jackson.databind.JsonNode;
import github.pstorch.bahnhoefe.service.Bahnhof;

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
    protected Map<Integer, String> loadPhotos() throws Exception {
        final Map<Integer, String> photoFlags = new HashMap<>();
        final JsonNode tree = readJsonFromUrl(photosUrl);
        for (int i = 0; i < tree.size(); i++) {
            final JsonNode bahnhofPhoto = tree.get(i);
            photoFlags.put(bahnhofPhoto.get("bahnhofsnr").asInt(), bahnhofPhoto.get("fotograf-title").asText());
        }
        return photoFlags;
    }

    @Override
    @SuppressWarnings("PMD.AvoidInstantiatingObjectsInLoops")
    protected Map<Integer, Bahnhof> loadBahnhoefe(final Map<Integer, String> photoFlags) throws Exception {
        final Map<Integer, Bahnhof> bahnhoefe = new HashMap<>();
        final JsonNode hits = readJsonFromUrl(bahnhoefeUrl)
                                    .get(BahnhoefeLoaderDe.HITS_ELEMENT)
                                    .get(BahnhoefeLoaderDe.HITS_ELEMENT);
        for (int i = 0; i < hits.size(); i++) {
            final JsonNode bahnhofJson = hits.get(i).get("_source");
            final Integer id = bahnhofJson.get("BahnhofNr").asInt();
            final Bahnhof bahnhof = new Bahnhof(id,
                    getCountryCode(),
                    bahnhofJson.get(BahnhoefeLoaderDe.TITLE_ELEMENT).asText(),
                    bahnhofJson.get(BahnhoefeLoaderDe.LAT_ELEMENT).asDouble(),
                    bahnhofJson.get(BahnhoefeLoaderDe.LON_ELEMENT).asDouble(),
                    photoFlags.get(id));
            bahnhoefe.put(bahnhof.getId(), bahnhof);
        }
        return bahnhoefe;
    }

}
