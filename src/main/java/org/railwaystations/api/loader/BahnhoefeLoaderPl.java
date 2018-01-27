package org.railwaystations.api.loader;

import org.railwaystations.api.model.Country;

import java.net.URL;

public class BahnhoefeLoaderPl extends AbstractBahnhoefeLoader {

    public BahnhoefeLoaderPl() {
        this(null, null);
    }

    public BahnhoefeLoaderPl(final URL bahnhoefeUrl, final URL photosUrl) {
        super(new Country("pl", "Polska",
                "bahnhofsfotos@deutschlands-bahnhoefe.de",
                "@android_oma, #dbHackathon, #dbOpendata, #Bahnhofsfoto, @khgdrn",
                "http://mt.rozklad-pkp.pl/de/sq?input={id}&maxJourneys=15&start=start"),
                bahnhoefeUrl, photosUrl);
    }

}
