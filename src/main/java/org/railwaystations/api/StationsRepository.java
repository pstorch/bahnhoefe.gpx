package org.railwaystations.api;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.similarity.LevenshteinDistance;
import org.railwaystations.api.db.CountryDao;
import org.railwaystations.api.loader.StationLoader;
import org.railwaystations.api.loader.PhotographerLoader;
import org.railwaystations.api.model.Country;
import org.railwaystations.api.model.Photographer;
import org.railwaystations.api.model.Station;
import org.railwaystations.api.model.Statistic;
import org.railwaystations.api.monitoring.Monitor;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class StationsRepository {

    private static final LevenshteinDistance LEVENSHTEIN_DISTANCE = new LevenshteinDistance();

    private final CountryDao countryDao;
    private final Monitor monitor;
    private final PhotographerLoader photographerLoader;
    private final String photoBaseUrl;
    private final ElasticBackend elasticBackend;

    private LoadingCache<String, Map<Station.Key, Station>> cache;
    private Set<Country> countries;

    public StationsRepository(final Monitor monitor, final CountryDao countryDao, final ElasticBackend elasticBackend, final PhotographerLoader photographerLoader, final String photoBaseUrl) {
        super();
        this.monitor = monitor;
        this.countryDao = countryDao;
        this.photographerLoader = photographerLoader;
        this.elasticBackend = elasticBackend;
        this.photoBaseUrl = photoBaseUrl;

        createNewStationsCache();
    }

    private void createNewStationsCache() {
        this.countries = countryDao.list();
        this.cache = CacheBuilder.newBuilder().refreshAfterWrite(60, TimeUnit.MINUTES).build(
                new StationsCacheLoader(monitor, countries, elasticBackend, photographerLoader, photoBaseUrl));
    }

    public Map<Station.Key, Station> get(final String countryCode) {
        if (countryCode == null) {
            final Map<Station.Key, Station> map = new HashMap<>();
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
        photographerLoader.refresh();
        final Thread refresher = new Thread(() -> {
            createNewStationsCache();
            monitor.sendMessage(responseUrl, getCountryStatisticMessage());
        });
        refresher.start();
    }

    public String getCountryStatisticMessage() {
        final StringBuilder message = new StringBuilder("Countries statistic: \n");
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
        return message.toString();
    }

    @SuppressWarnings("PMD.AvoidInstantiatingObjectsInLoops")
    public Station findById(final String id) {
        for (final Country country : getCountries()) {
            final Station station = get(country.getCode()).get(new Station.Key(country.getCode(), id));
            if (station != null) {
                return station;
            }
        }
        return null;
    }

    public List<Station> findByName(final String name) {
        final List<Station> found = new ArrayList<>();
        for (final Country country : getCountries()) {
            found.addAll(get(country.getCode())
                    .values()
                    .stream()
                    .filter(station -> StringUtils.containsIgnoreCase(station.getTitle(), name))
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

    public Photographer getPhotographer(final String photographerName) {
        return photographerLoader.loadPhotographers().get(photographerName);
    }

    public Optional<Country> getCountry(final String countryCode) {
        return countries.stream().filter(c -> c.getCode().equals(countryCode)).findFirst();
    }

    public Optional<Photographer> findPhotographerByLevenshtein(final String photographerName) {
        return photographerLoader.loadPhotographers()
                .values().stream().filter(p -> LEVENSHTEIN_DISTANCE.apply(p.getName(), photographerName) < 3)
                .sorted((p1, p2) -> LEVENSHTEIN_DISTANCE.apply(p1.getName(), photographerName).compareTo(LEVENSHTEIN_DISTANCE.apply(p2.getName(), photographerName)))
                .findFirst();
    }

    public Station findByKey(final Station.Key key) {
        final Map<Station.Key, Station> keyStationMap = get(key.getCountry());
        if (keyStationMap != null) {
            return keyStationMap.get(key);
        }
        return null;
    }

    private static class StationsCacheLoader extends CacheLoader<String, Map<Station.Key, Station>> {
        private final Monitor monitor;
        private final Set<Country> countries;
        private final ElasticBackend elasticBackend;
        private final PhotographerLoader photographerLoader;
        private final String photoBaseUrl;

        public StationsCacheLoader(final Monitor slack, final Set<Country> countries, final ElasticBackend elasticBackend, final PhotographerLoader photographerLoader, final String photoBaseUrl) {
            this.monitor = slack;
            this.countries = countries;
            this.elasticBackend = elasticBackend;
            this.photographerLoader = photographerLoader;
            this.photoBaseUrl = photoBaseUrl;
        }

        @SuppressWarnings("PMD.AvoidInstantiatingObjectsInLoops")
        public Map<Station.Key, Station> load(final String countryCode) {
            try {
                for (final Country country : countries) {
                    final StationLoader loader = new StationLoader(country, monitor, elasticBackend);
                    if (loader.getCountry().getCode().equals(countryCode)) {
                        return loader.loadStations(photographerLoader.loadPhotographers(), photoBaseUrl);
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
