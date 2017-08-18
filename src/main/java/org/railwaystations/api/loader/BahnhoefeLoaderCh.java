package org.railwaystations.api.loader;

import com.fasterxml.jackson.databind.JsonNode;
import org.railwaystations.api.model.Bahnhof;
import org.railwaystations.api.model.Country;
import org.railwaystations.api.model.Photo;

import java.net.URL;
import java.util.HashMap;
import java.util.Map;

public class BahnhoefeLoaderCh extends AbstractBahnhoefeLoader {

    public BahnhoefeLoaderCh() {
        this(null, null);
    }

    public BahnhoefeLoaderCh(final URL bahnhoefeUrl, final URL photosUrl) {
        super(new Country("ch", "Schweiz",
                "fotos@schweizer-bahnhoefe.ch",
                "@BahnhoefeCH, @android_oma, #BahnhofsfotoCH",
                "http://fahrplan.sbb.ch/bin/stboard.exe/dn?input={title}&REQTrain_name=&boardType=dep&time=now&maxJourneys=20&selectDate=today&productsFilter=1111111111&start=yes"),
                bahnhoefeUrl, photosUrl);
    }

    @Override
    @SuppressWarnings("PMD.AvoidInstantiatingObjectsInLoops")
    protected Map<Integer, Bahnhof> loadBahnhoefe(final Map<Integer, Photo> photos) throws Exception {
        final Map<Integer, Bahnhof> bahnhoefe = new HashMap<>();

        final JsonNode hits = readJsonFromUrl(getBahnhoefeUrl())
                                .get(AbstractBahnhoefeLoader.HITS_ELEMENT)
                                .get(AbstractBahnhoefeLoader.HITS_ELEMENT);
        for (int i = 0; i < hits.size(); i++) {
            final JsonNode sourceJson = hits.get(i).get("_source");
            final JsonNode fieldsJson = sourceJson.get("fields");
            final Integer id = fieldsJson.get("nummer").asInt();
            final JsonNode abkuerzung = fieldsJson.get("abkuerzung");
            final Bahnhof bahnhof = new Bahnhof(id,
                    getCountry().getCode(),
                    fieldsJson.get("name").asText(),
                    readCoordinates(sourceJson),
                    abkuerzung != null ? abkuerzung.asText() : null,
                    photos.get(id));
            bahnhoefe.put(bahnhof.getId(), bahnhof);
        }
        return bahnhoefe;
    }

}
