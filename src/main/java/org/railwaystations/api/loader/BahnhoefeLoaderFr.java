package org.railwaystations.api.loader;

import org.railwaystations.api.model.Country;

import java.net.URL;

public class BahnhoefeLoaderFr extends AbstractBahnhoefeLoader {

    public BahnhoefeLoaderFr() {
        this(null, null);
    }

    public BahnhoefeLoaderFr(final URL bahnhoefeUrl, final URL photosUrl) {
        super(new Country("fr", "France",
                "bahnhofsfotos@deutschlands-bahnhoefe.de",
                "@android_oma, #dbHackathon, #dbOpendata, #Bahnhofsfoto, @khgdrn",
                "http://m.sncf.com/sncf/gare?libelleGare={title}"),
                bahnhoefeUrl, photosUrl);
    }

}
