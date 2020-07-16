package org.railwaystations.api.resources;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.railwaystations.api.StationsRepository;
import org.railwaystations.api.model.Coordinates;
import org.railwaystations.api.model.Photo;
import org.railwaystations.api.model.Station;
import org.railwaystations.api.model.User;

import java.util.*;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

public class StationsResourceTest {

    private StationsResource resource;

    @BeforeEach
    public void setUp() {
        final Map<Station.Key, Station> stationsXY = new HashMap<>(2);
        final Station.Key key5 = new Station.Key("xy", "5");
        stationsXY.put(key5, new Station(key5, "Lummerland", new Coordinates(50.0, 9.0), "XYZ", new Photo(key5, "/fotos/xy/5.jpg", createTestPhotographer("Jim Knopf", "photographerUrl", "CC0"), null, "CC0"), false));

        final Map<Station.Key, Station> stationsAB = new HashMap<>(2);
        final Station.Key key3 = new Station.Key("ab", "3");
        stationsAB.put(key3, new Station(key3, "Nimmerland", new Coordinates(40.0, 6.0), "ABC", new Photo(key3, "/fotos/ab/3.jpg", createTestPhotographer("Peter Pan", "photographerUrl2", "CC0 by SA"), null, "CC0 by SA"), true));

        final Map<Station.Key, Station> stationsAll = new HashMap<>(2);
        stationsAll.putAll(stationsAB);
        stationsAll.putAll(stationsXY);

        final StationsRepository repository = Mockito.mock(StationsRepository.class);
        Mockito.when(repository.getStationsByCountry(Collections.singleton("xy"))).thenReturn(stationsXY);
        Mockito.when(repository.getStationsByCountry(Collections.singleton("ab"))).thenReturn(stationsAB);
        Mockito.when(repository.getStationsByCountry(null)).thenReturn(stationsAll);
        Mockito.when(repository.getStationsByCountry(allCountries())).thenReturn(stationsAll);

        resource = new StationsResource(repository);
    }

    private Set<String> allCountries() {
        final Set<String> countries = new HashSet<>();
        countries.add("ab");
        countries.add("xy");
        return countries;
    }

    @Test
    public void testGetXY() {
        final List<Station> resultXY = resource.get(Collections.singleton("xy"), null, null, null, null, null, null);
        final Station stationXY = resultXY.get(0);
        assertThat(stationXY, notNullValue());
        assertThat(stationXY.getKey(), equalTo(new Station.Key("xy", "5")));
        assertThat(stationXY.getTitle(), equalTo("Lummerland"));
        assertThat(stationXY.getCoordinates().getLat(), equalTo(50.0));
        assertThat(stationXY.getCoordinates().getLon(), equalTo(9.0));
        assertThat(stationXY.getPhotographer(), equalTo("Jim Knopf"));
        assertThat(stationXY.getDS100(), equalTo("XYZ"));
        assertThat(stationXY.getPhotoUrl(), equalTo("/fotos/xy/5.jpg"));
        assertThat(stationXY.getLicense(), equalTo("CC0"));
        assertThat(stationXY.getPhotographerUrl(), equalTo("photographerUrl"));
        assertThat(stationXY.isActive(), equalTo(false));
    }

    @Test
    public void testGetXYWithFilterActive() {
        final List<Station> resultXY = resource.get(Collections.singleton("xy"), null, null, null, null, null, true);
        assertThat(resultXY.isEmpty(), equalTo(true));
    }

    @Test
    public void testGetAB() {
        final List<Station> resultAB = resource.get(Collections.singleton("ab"), null, null, null, null, null, null);
        final Station station = resultAB.get(0);
        assertNimmerland(station);
    }

    @Test
    public void testGetABXY() {
        final List<Station> resultAB = resource.get(allCountries(), null, null, null, null, null, null);
        assertThat(resultAB.size(), equalTo(2));
    }

    private void assertNimmerland(final Station station) {
        assertThat(station, notNullValue());
        assertThat(station.getKey(), equalTo(new Station.Key("ab", "3")));
        assertThat(station.getTitle(), equalTo("Nimmerland"));
        assertThat(station.getCoordinates().getLat(), equalTo(40.0));
        assertThat(station.getCoordinates().getLon(), equalTo(6.0));
        assertThat(station.getPhotographer(), equalTo("Peter Pan"));
        assertThat(station.getPhotoUrl(), equalTo("/fotos/ab/3.jpg"));
        assertThat(station.getDS100(), equalTo("ABC"));
        assertThat(station.getLicense(), equalTo("CC0 by SA"));
        assertThat(station.getPhotographerUrl(), equalTo("photographerUrl2"));
        assertThat(station.isActive(), equalTo(true));
    }

    @Test
    public void testGetById() {
        final Station station = resource.getById("ab", "3");
        assertNimmerland(station);
    }

    @Test
    public void testGetAll() {
        final List<Station> resultAll = resource.get(null, null, null, null, null, null, null);
        assertThat(resultAll.size(), equalTo(2));
    }

    private User createTestPhotographer(final String name, final String url, final String license) {
        return new User(name, url, license, 0, null, true, false, null, null, false, null);
    }
}
