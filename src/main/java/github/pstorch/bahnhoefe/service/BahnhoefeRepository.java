package github.pstorch.bahnhoefe.service;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import github.pstorch.bahnhoefe.service.loader.BahnhoefeLoader;
import github.pstorch.bahnhoefe.service.model.Bahnhof;
import github.pstorch.bahnhoefe.service.monitoring.Monitor;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class BahnhoefeRepository {

    private final LoadingCache<String, Map<Integer, Bahnhof>> cache;

    public BahnhoefeRepository(final Monitor monitor, final BahnhoefeLoader ... loaders) {
        super();
        this.cache = CacheBuilder.newBuilder().refreshAfterWrite(5, TimeUnit.MINUTES).build(
                new BahnhoefeCacheLoader(monitor, loaders));
    }

    public Map<Integer, Bahnhof> get(final String country) {
        return cache.getUnchecked(country);
    }

    private static class BahnhoefeCacheLoader extends CacheLoader<String, Map<Integer, Bahnhof>> {
        private final Monitor monitor;
        private final BahnhoefeLoader[] loaders;

        public BahnhoefeCacheLoader(final Monitor slack, final BahnhoefeLoader... loaders) {
            this.monitor = slack;
            this.loaders = loaders;
        }

        public Map<Integer, Bahnhof> load(final String country) throws Exception {
            try {
                for (final BahnhoefeLoader loader : loaders) {
                    if (loader.getCountryCode().equals(country)) {
                        return loader.loadBahnhoefe();
                    }
                }
            } catch (final Exception e) {
                monitor.sendMessage(e.getMessage());
                throw e;
            }
            return Collections.emptyMap();
        }
    }
}
