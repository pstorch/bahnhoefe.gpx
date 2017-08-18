package org.railwaystations.api.loader;

import com.fasterxml.jackson.databind.JsonNode;
import org.railwaystations.api.model.Bahnhof;
import org.railwaystations.api.model.Coordinates;
import org.railwaystations.api.model.Country;
import org.railwaystations.api.model.Photo;

import java.net.URL;
import java.util.HashMap;
import java.util.Map;

public class BahnhoefeLoaderDe extends AbstractBahnhoefeLoader {

    private static final String TITLE_ELEMENT = "title";

    private static final String LON_ELEMENT = "lon";

    private static final String LAT_ELEMENT = "lat";

    private static final String DS100_ELEMENT = "DS100";

    public BahnhoefeLoaderDe() {
        this(null, null);
    }

    public BahnhoefeLoaderDe(final URL bahnhoefeUrl, final URL photosUrl) {
        super(new Country("de", "Deutschland",
                "bahnhofsfotos@deutschlands-bahnhoefe.de",
                "@android_oma, #dbHackathon, #dbOpendata, #Bahnhofsfoto, @khgdrn",
                "https://mobile.bahn.de/bin/mobil/bhftafel.exe/dox?bt=dep&max=10&rt=1&use_realtime_filter=1&start=yes&input={title}"),
                photosUrl, bahnhoefeUrl);
    }

    @Override
    @SuppressWarnings("PMD.AvoidInstantiatingObjectsInLoops")
    protected Map<Integer, Bahnhof> loadBahnhoefe(final Map<Integer, Photo> photos) throws Exception {
        final Map<Integer, Bahnhof> bahnhoefe = new HashMap<>();
        final JsonNode hits = readJsonFromUrl(getBahnhoefeUrl())
                                    .get(AbstractBahnhoefeLoader.HITS_ELEMENT)
                                    .get(AbstractBahnhoefeLoader.HITS_ELEMENT);
        for (int i = 0; i < hits.size(); i++) {
            final JsonNode bahnhofJson = hits.get(i).get("_source");
            final Integer id = bahnhofJson.get("BahnhofNr").asInt();
            final Bahnhof bahnhof = new Bahnhof(id,
                    getCountry().getCode(),
                    bahnhofJson.get(BahnhoefeLoaderDe.TITLE_ELEMENT).asText(),
                    new Coordinates(bahnhofJson.get(BahnhoefeLoaderDe.LAT_ELEMENT).asDouble(),
                                    bahnhofJson.get(BahnhoefeLoaderDe.LON_ELEMENT).asDouble()),
                    bahnhofJson.get(BahnhoefeLoaderDe.DS100_ELEMENT).asText(),
                    photos.get(id));
            bahnhoefe.put(bahnhof.getId(), bahnhof);
        }
        return bahnhoefe;
    }

}
