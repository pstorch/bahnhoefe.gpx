package org.railwaystations.api.loader;

import org.railwaystations.api.model.Country;

import java.net.URL;

public class BahnhoefeLoaderUk extends AbstractBahnhoefeLoader {

    public BahnhoefeLoaderUk() {
        this(null, null);
    }

    public BahnhoefeLoaderUk(final URL bahnhoefeUrl, final URL photosUrl) {
        super(new Country("uk", "United Kingdom",
                "bahnhofsfotos@deutschlands-bahnhoefe.de",
                "@android_oma, #dbHackathon, #dbOpendata, #Bahnhofsfoto, @khgdrn",
                "http://ojp.nationalrail.co.uk/service/ldbboard/dep/{DS100}"),
                bahnhoefeUrl, photosUrl);
    }

}
