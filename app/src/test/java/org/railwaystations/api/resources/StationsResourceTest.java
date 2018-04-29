package org.railwaystations.api.resources;

import org.junit.jupiter.api.BeforeEach;
import org.mockito.Mockito;
import org.railwaystations.api.StationsRepository;
import org.railwaystations.api.loader.PhotographerLoader;
import org.railwaystations.api.loader.StationLoader;
import org.railwaystations.api.model.Coordinates;
import org.railwaystations.api.model.Country;
import org.railwaystations.api.model.Photo;
import org.railwaystations.api.model.Station;
import org.railwaystations.api.monitoring.LoggingMonitor;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

public class StationsResourceTest {

    private StationsResource resource;

    @BeforeEach
    public void setUp() throws MalformedURLException {
        final PhotographerLoader photographerLoader = new PhotographerLoader( new URL("file:./src/test/resources/photographers.json"));

        final StationLoader loaderXY = Mockito.mock(StationLoader.class);
        final Map<Integer, Station> stationsXY = new HashMap<>(2);
        stationsXY.put(5, new Station(5, "xy", "Lummerland", new Coordinates(50.0, 9.0), "XYZ", new Photo(5, "URL", "Jim Knopf", "photographerUrl", null, "CC0", "licenseUrl")));
        Mockito.when(loaderXY.loadStations(Mockito.anyMap(), Mockito.anyString())).thenReturn(stationsXY);
        Mockito.when(loaderXY.getCountry()).thenReturn(new Country("xy", null, null, null, null));

        final StationLoader loaderAB = Mockito.mock(StationLoader.class);
        final Map<Integer, Station> stations = new HashMap<>(2);
        stations.put(3, new Station(3, "ab", "Nimmerland", new Coordinates(40.0, 6.0), "ABC", new Photo(3, "URL2", "Peter Pan", "photographerUrl2", null, "CC0 by SA", "licenseUrl2")));
        Mockito.when(loaderAB.loadStations(Mockito.anyMap(), Mockito.anyString())).thenReturn(stations);
        Mockito.when(loaderAB.getCountry()).thenReturn(new Country("ab", null, null, null, null));

        resource = new StationsResource(new StationsRepository(new LoggingMonitor(), Arrays.asList(loaderAB, loaderXY), photographerLoader, ""));
    }

    @org.junit.jupiter.api.Test
    public void testGetXY() {
        final List<Station> resultXY = resource.get("xy", null, null, null, null, null);
        final Station stationXY = resultXY.get(0);
        assertThat(stationXY, notNullValue());
        assertThat(stationXY.getId(), equalTo(5));
        assertThat(stationXY.getTitle(), equalTo("Lummerland"));
        assertThat(stationXY.getLat(), equalTo(50.0));
        assertThat(stationXY.getLon(), equalTo(9.0));
        assertThat(stationXY.getPhotographer(), equalTo("Jim Knopf"));
        assertThat(stationXY.getDS100(), equalTo("XYZ"));
        assertThat(stationXY.getPhotoUrl(), equalTo("URL"));
        assertThat(stationXY.getLicense(), equalTo("CC0"));
        assertThat(stationXY.getPhotographerUrl(), equalTo("photographerUrl"));
    }

    @org.junit.jupiter.api.Test
    public void testGetAB() {
        final List<Station> resultAB = resource.get("ab", null, null, null, null, null);
        final Station station = resultAB.get(0);
        assertNimmerland(station);
    }

    private void assertNimmerland(final Station station) {
        assertThat(station, notNullValue());
        assertThat(station.getId(), equalTo(3));
        assertThat(station.getTitle(), equalTo("Nimmerland"));
        assertThat(station.getLat(), equalTo(40.0));
        assertThat(station.getLon(), equalTo(6.0));
        assertThat(station.getPhotographer(), equalTo("Peter Pan"));
        assertThat(station.getPhotoUrl(), equalTo("URL2"));
        assertThat(station.getDS100(), equalTo("ABC"));
        assertThat(station.getLicense(), equalTo("CC0 by SA"));
        assertThat(station.getPhotographerUrl(), equalTo("photographerUrl2"));
    }

    @org.junit.jupiter.api.Test
    public void testGetById() {
        final Station station = resource.getById("ab", 3);
        assertNimmerland(station);
    }

    @org.junit.jupiter.api.Test
    public void testGetAll() {
        final List<Station> resultAll = resource.get(null, null, null, null, null, null);
        assertThat(resultAll.size(), equalTo(2));
    }

}
