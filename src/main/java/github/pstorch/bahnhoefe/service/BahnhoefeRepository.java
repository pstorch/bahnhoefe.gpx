package github.pstorch.bahnhoefe.service;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import github.pstorch.bahnhoefe.service.loader.BahnhoefeLoader;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class BahnhoefeRepository {

    private final LoadingCache<String, Map<Integer, Bahnhof>> cache;

    public BahnhoefeRepository(final BahnhoefeLoader ... loaders) {
        super();
        this.cache = CacheBuilder.newBuilder().refreshAfterWrite(5, TimeUnit.MINUTES).build(
                new BahnhoefeCacheLoader(loaders));
    }

    public Map<Integer, Bahnhof> get(final String country) {
        return cache.getUnchecked(country);
    }

    private static class BahnhoefeCacheLoader extends CacheLoader<String, Map<Integer, Bahnhof>> {
        private final BahnhoefeLoader[] loaders;

        public BahnhoefeCacheLoader(final BahnhoefeLoader... loaders) {
            this.loaders = loaders;
        }

        public Map<Integer, Bahnhof> load(final String country) throws Exception {
            for (final BahnhoefeLoader loader : loaders) {
                if (loader.getCountryCode().equals(country)) {
                    return loader.loadBahnhoefe();
                }
            }
            return Collections.emptyMap();
        }
    }
}
