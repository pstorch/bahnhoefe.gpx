package org.railwaystations.api;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import org.railwaystations.api.loader.BahnhoefeLoader;
import org.railwaystations.api.model.Bahnhof;
import org.railwaystations.api.monitoring.Monitor;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class BahnhoefeRepository {

    private final LoadingCache<String, Map<Integer, Bahnhof>> cache;
    private final Set<String> countries;

    public BahnhoefeRepository(final Monitor monitor, final BahnhoefeLoader ... loaders) {
        super();
        this.cache = CacheBuilder.newBuilder().refreshAfterWrite(5, TimeUnit.MINUTES).build(
                new BahnhoefeCacheLoader(monitor, loaders));
        this.countries = Arrays.stream(loaders).map(BahnhoefeLoader::getCountryCode).collect(Collectors.toSet());
    }

    public Map<Integer, Bahnhof> get(final String country) {
        if (country == null) {
            final Map<Integer, Bahnhof> map = new HashMap<>();
            for (final String aCountry : countries) {
                map.putAll(cache.getUnchecked(aCountry));
            }
            return map;
        }
        return cache.getUnchecked(country);
    }

    public Set<String> getCountries() {
        return Collections.unmodifiableSet(countries);
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
