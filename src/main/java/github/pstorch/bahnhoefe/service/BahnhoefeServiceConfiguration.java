package github.pstorch.bahnhoefe.service;

import github.pstorch.bahnhoefe.service.loader.BahnhoefeLoader;
import github.pstorch.bahnhoefe.service.loader.BahnhoefeLoaderCh;
import github.pstorch.bahnhoefe.service.loader.BahnhoefeLoaderDe;
import io.dropwizard.Configuration;

import java.util.HashMap;
import java.util.Map;

public class BahnhoefeServiceConfiguration extends Configuration {

    private BahnhoefeLoaderDe loaderDe = new BahnhoefeLoaderDe();
    private BahnhoefeLoaderCh loaderCh = new BahnhoefeLoaderCh();

    public BahnhoefeLoaderDe getLoaderDe() {
        return loaderDe;
    }

    public BahnhoefeLoaderCh getLoaderCh() {
        return loaderCh;
    }

    public Map<String, BahnhoefeLoader> getLoaderMap() {
        final Map<String, BahnhoefeLoader> map = new HashMap<>(10);
        addLoaderToMap(map, loaderDe);
        addLoaderToMap(map, loaderCh);
        return map;
    }

    private void addLoaderToMap(final Map<String, BahnhoefeLoader> map, final BahnhoefeLoader loader) {
        if (loader != null) {
            map.put(loader.getCountryCode(), loader);
        }
    }
}
