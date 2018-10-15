package org.railwaystations.api;

import org.apache.commons.lang3.StringUtils;
import org.railwaystations.api.db.CountryDao;
import org.railwaystations.api.db.UserDao;
import org.railwaystations.api.model.Country;
import org.railwaystations.api.model.Station;
import org.railwaystations.api.model.Statistic;
import org.railwaystations.api.model.User;
import org.railwaystations.api.monitoring.Monitor;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class StationsRepository {

    private final CountryDao countryDao;
    private final UserDao userDao;
    private final Monitor monitor;
    private final String photoBaseUrl;

    public StationsRepository(final Monitor monitor, final CountryDao countryDao, final UserDao userDao, final String photoBaseUrl) {
        super();
        this.monitor = monitor;
        this.countryDao = countryDao;
        this.userDao = userDao;
        this.photoBaseUrl = photoBaseUrl;
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
        return Collections.unmodifiableSet(countryDao.list());
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

    public Station findByKey(final Station.Key key) {
        final Map<Station.Key, Station> keyStationMap = get(key.getCountry());
        if (keyStationMap != null) {
            return keyStationMap.get(key);
        }
        return null;
    }

}
