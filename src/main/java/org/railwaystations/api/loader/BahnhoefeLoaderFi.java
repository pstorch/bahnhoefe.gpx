package org.railwaystations.api.loader;

import com.fasterxml.jackson.databind.JsonNode;
import org.railwaystations.api.model.Bahnhof;
import org.railwaystations.api.model.Country;
import org.railwaystations.api.model.Photo;

import java.net.URL;
import java.util.HashMap;
import java.util.Map;

public class BahnhoefeLoaderFi extends AbstractBahnhoefeLoader {

    private static final String HITS_ELEMENT = "hits";

    public BahnhoefeLoaderFi() {
        this(null, null);
    }

    public BahnhoefeLoaderFi(final URL bahnhoefeUrl, final URL photosUrl) {
        super(new Country("fi", "Finnland",
                "bahnhofsfotos@deutschlands-bahnhoefe.de",
                "@android_oma, #dbHackathon, #dbOpendata, #Bahnhofsfoto, @khgdrn",
                "http://timetable-fi.railway-stations.org/index.php?stationshortcode={DS100}"),
                bahnhoefeUrl, photosUrl);
    }

    @Override
    @SuppressWarnings("PMD.AvoidInstantiatingObjectsInLoops")
    protected Map<Integer, Bahnhof> loadBahnhoefe(final Map<Integer, Photo> photos) throws Exception {
        final Map<Integer, Bahnhof> bahnhoefe = new HashMap<>();

        final JsonNode hits = readJsonFromUrl(getBahnhoefeUrl())
                                .get(BahnhoefeLoaderFi.HITS_ELEMENT)
                                .get(BahnhoefeLoaderFi.HITS_ELEMENT);
        for (int i = 0; i < hits.size(); i++) {
            final JsonNode sourceJson = hits.get(i).get("_source");
            final JsonNode propertiesJson = sourceJson.get("properties");
            final Integer id = propertiesJson.get("UICIBNR").asInt();
            final Bahnhof bahnhof = new Bahnhof(id,
                    getCountry().getCode(),
                    propertiesJson.get("name").asText(),
                    readCoordinates(sourceJson),
                    propertiesJson.get("abkuerzung").asText(),
                    photos.get(id));
            bahnhoefe.put(bahnhof.getId(), bahnhof);
        }
        return bahnhoefe;
    }

}
