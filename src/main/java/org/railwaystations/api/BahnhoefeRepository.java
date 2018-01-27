package org.railwaystations.api;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import org.apache.commons.lang3.StringUtils;
import org.railwaystations.api.loader.BahnhoefeLoader;
import org.railwaystations.api.model.Bahnhof;
import org.railwaystations.api.model.Country;
import org.railwaystations.api.model.Statistic;
import org.railwaystations.api.monitoring.Monitor;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class BahnhoefeRepository {

    private final LoadingCache<String, Map<Integer, Bahnhof>> cache;
    private final Set<Country> countries;
    private final Monitor monitor;

    public BahnhoefeRepository(final Monitor monitor, final List<BahnhoefeLoader> loaders) {
        super();
        this.monitor = monitor;
        this.cache = CacheBuilder.newBuilder().refreshAfterWrite(60, TimeUnit.MINUTES).build(
                new BahnhoefeCacheLoader(monitor, loaders));
        this.countries = loaders.stream().map(BahnhoefeLoader::getCountry).collect(Collectors.toSet());
    }

    public Map<Integer, Bahnhof> get(final String countryCode) {
        if (countryCode == null) {
            final Map<Integer, Bahnhof> map = new HashMap<>();
            for (final Country aCountry : countries) {
                map.putAll(cache.getUnchecked(aCountry.getCode()));
            }
            return map;
        }
        return cache.getUnchecked(countryCode);
    }

    public Set<Country> getCountries() {
        return Collections.unmodifiableSet(countries);
    }

    public void refresh(final String responseUrl) {
        cache.invalidateAll();
        final Thread refresher = new Thread(() -> {
            final StringBuilder message = new StringBuilder("Data loaded: \n");
            for (final Country aCountry : countries) {
                final Statistic stat = getStatistic(aCountry.getCode());
                message.append("- ")
                        .append(aCountry.getCode())
                        .append(": ")
                        .append(stat.getWithPhoto())
                        .append(" of ")
                        .append(stat.getTotal())
                        .append("\n");
            }
            monitor.sendMessage(responseUrl, message.toString());
        });
        refresher.start();
    }

    public Bahnhof findById(final Integer id) {
        for (final Country country : getCountries()) {
            final Bahnhof bahnhof = get(country.getCode()).get(id);
            if (bahnhof != null) {
                return bahnhof;
            }
        }
        return null;
    }

    public List<Bahnhof> findByName(final String name) {
        final List<Bahnhof> found = new ArrayList<>();
        for (final Country country : getCountries()) {
            found.addAll(get(country.getCode())
                    .values()
                    .stream()
                    .filter(bahnhof -> StringUtils.containsIgnoreCase(bahnhof.getTitle(), name))
                    .collect(Collectors.toList()));
        }
        return found;
    }

    public Statistic getStatistic(final String country) {
        final AtomicInteger total = new AtomicInteger();
        final AtomicInteger withPhoto = new AtomicInteger();
        final AtomicInteger withoutPhoto = new AtomicInteger();
        final Set<String> photographers = new HashSet<>();
        get(country).values()
                .forEach(b -> {
                    total.incrementAndGet();
                    if (b.hasPhoto()) {
                        withPhoto.incrementAndGet();
                        photographers.add(b.getStatUser());
                    } else {
                        withoutPhoto.incrementAndGet();
                    }
                });

        return new Statistic(total.intValue(), withPhoto.intValue(), withoutPhoto.intValue(), photographers.size());
    }

    private static class BahnhoefeCacheLoader extends CacheLoader<String, Map<Integer, Bahnhof>> {
        private final Monitor monitor;
        private final List<BahnhoefeLoader> loaders;

        public BahnhoefeCacheLoader(final Monitor slack, final List<BahnhoefeLoader> loaders) {
            this.monitor = slack;
            this.loaders = loaders;
        }

        public Map<Integer, Bahnhof> load(final String countryCode) throws Exception {
            try {
                for (final BahnhoefeLoader loader : loaders) {
                    if (loader.getCountry().getCode().equals(countryCode)) {
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
