package org.railwaystations.rsapi;

import org.apache.commons.lang3.StringUtils;
import org.railwaystations.rsapi.db.CountryDao;
import org.railwaystations.rsapi.db.StationDao;
import org.railwaystations.rsapi.model.Coordinates;
import org.railwaystations.rsapi.model.Country;
import org.railwaystations.rsapi.model.Station;
import org.railwaystations.rsapi.model.Statistic;
import org.springframework.stereotype.Repository;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Repository
@SuppressWarnings("PMD.BeanMembersShouldSerialize")
public class StationsRepository {

    private final CountryDao countryDao;
    private final StationDao stationDao;

    public StationsRepository(final CountryDao countryDao, final StationDao stationDao) {
        super();
        this.countryDao = countryDao;
        this.stationDao = stationDao;
    }

    public Map<Station.Key, Station> getStationsByCountry(final Set<String> countryCodes) {
        final Set<Station> stations;
        if (countryCodes == null || countryCodes.isEmpty()) {
            stations = stationDao.all();
        } else {
            stations = stationDao.findByCountryCodes(countryCodes);
        }
        return stations.stream().collect(Collectors.toMap(Station::getKey, Function.identity()));
    }

    public Set<Country> getCountries() {
        return Collections.unmodifiableSet(countryDao.list(true));
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

    public Map<Station.Key, String> findByName(final String name) {
        return stationDao.findByName(name);
    }

    public Statistic getStatistic(final String country) {
        return stationDao.getStatistic(country);
    }

    @SuppressWarnings("PMD.AvoidLiteralsInIfCondition")
    public Station findByCountryAndId(final String country, final String stationId) {
        if (StringUtils.isBlank(stationId)) {
            return null;
        }
        if (StringUtils.isNotBlank(country)) {
            return findByKey(new Station.Key(country, stationId));
        }
        final Set<Station> stations = stationDao.findById(stationId);
        if (stations.size() > 1) {
            return null; // id is not unique
        }
        return stations.stream().findFirst().orElse(null);
    }

    public Station findByKey(final Station.Key key) {
        return stationDao.findByKey(key.getCountry(), key.getId()).stream().findFirst().orElse(null);
    }

    public Map<String, Long> getPhotographerMap(final String country) {
        return stationDao.getPhotographerMap(country);
    }

    public void insert(final Station station) {
        stationDao.insert(station);
    }

    public void delete(final Station station) {
        stationDao.delete(station);
    }

    public void updateActive(final Station station) {
        stationDao.updateActive(station);
    }

    public List<Station> findRecentImports(final long fromTimestampMillis) {
        return stationDao.findRecentImports(fromTimestampMillis);
    }

    public int countNearbyCoordinates(final Coordinates coordinates) {
        return stationDao.countNearbyCoordinates(coordinates);
    }

    public String getNextZ() {
        return "Z" + (stationDao.getMaxZ() + 1);
    }

    public void changeStationTitle(final Station station, final String newTitle) {
        stationDao.changeStationTitle(station, newTitle);
    }

    public void updateLocation(final Station station, final Coordinates coordinates) {
        stationDao.updateLocation(station, coordinates);
    }
}
