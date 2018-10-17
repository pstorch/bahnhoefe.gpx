package org.railwaystations.api;

import org.apache.commons.lang3.StringUtils;
import org.railwaystations.api.db.CountryDao;
import org.railwaystations.api.db.StationDao;
import org.railwaystations.api.model.Country;
import org.railwaystations.api.model.Station;
import org.railwaystations.api.model.Statistic;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public class StationsRepository {

    private final CountryDao countryDao;
    private final StationDao stationDao;

    public StationsRepository(final CountryDao countryDao, final StationDao stationDao) {
        super();
        this.countryDao = countryDao;
        this.stationDao = stationDao;
    }

    public Map<Station.Key, Station> get(final String countryCode) {
        final Set<Station> stations;
        if (countryCode == null) {
            stations = stationDao.list();
        } else {
            stations = stationDao.findByCountry(countryCode);
        }
        return stations.stream().collect(Collectors.toMap(Station::getKey, Function.identity()));
    }

    public Set<Country> getCountries() {
        return Collections.unmodifiableSet(countryDao.list());
    }

    public String getCountryStatisticMessage() {
        final StringBuilder message = new StringBuilder("Countries statistic: \n");
        for (final Country aCountry : getCountries()) {
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

    public Station findById(final String id) {
        return stationDao.findById(id).stream().findFirst().orElse(null);
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
        return stationDao.getStatistic(country);
    }

    public Station findByKey(final Station.Key key) {
        return stationDao.findByKey(key.getCountry(), key.getId()).orElse(null);
    }

}
