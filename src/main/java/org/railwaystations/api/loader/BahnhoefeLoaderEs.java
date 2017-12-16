package org.railwaystations.api.loader;

import org.railwaystations.api.model.Country;

import java.net.URL;

public class BahnhoefeLoaderEs extends AbstractBahnhoefeLoader {

    public BahnhoefeLoaderEs() {
        this(null, null);
    }

    public BahnhoefeLoaderEs(final URL bahnhoefeUrl, final URL photosUrl) {
        super(new Country("es", "España",
                "bahnhofsfotos@deutschlands-bahnhoefe.de",
                "@android_oma, #dbHackathon, #dbOpendata, #Bahnhofsfoto, @khgdrn",
                "http://www.adif.es/AdifWeb/estacionesMapa.jsp?i=en_US&dest=&pes=informacion&e={DS100}"),
                bahnhoefeUrl, photosUrl);
    }

}
