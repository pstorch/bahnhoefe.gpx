package org.railwaystations.api.resources;

import org.junit.Before;
import org.junit.Test;
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

    @Before
    public void setUp() throws MalformedURLException {
        final PhotographerLoader photographerLoader = new PhotographerLoader( new URL("file:./src/test/resources/photographers.json"));

        final StationLoader loaderXY = Mockito.mock(StationLoader.class);
        final Map<Station.Key, Station> stationsXY = new HashMap<>(2);
        final Station.Key key5 = new Station.Key("xy", "5");
        stationsXY.put(key5, new Station(key5, "Lummerland", new Coordinates(50.0, 9.0), "XYZ", new Photo(key5, "URL", "Jim Knopf", "photographerUrl", null, "CC0", "licenseUrl")));
        Mockito.when(loaderXY.loadStations(Mockito.anyMap(), Mockito.anyString())).thenReturn(stationsXY);
        Mockito.when(loaderXY.getCountry()).thenReturn(new Country("xy", null, null, null, null));

        final StationLoader loaderAB = Mockito.mock(StationLoader.class);
        final Map<Station.Key, Station> stations = new HashMap<>(2);
        final Station.Key key3 = new Station.Key("ab", "3");
        stations.put(key3, new Station(key3, "Nimmerland", new Coordinates(40.0, 6.0), "ABC", new Photo(key3, "URL2", "Peter Pan", "photographerUrl2", null, "CC0 by SA", "licenseUrl2")));
        Mockito.when(loaderAB.loadStations(Mockito.anyMap(), Mockito.anyString())).thenReturn(stations);
        Mockito.when(loaderAB.getCountry()).thenReturn(new Country("ab", null, null, null, null));

        resource = new StationsResource(new StationsRepository(new LoggingMonitor(), Arrays.asList(loaderAB, loaderXY), photographerLoader, ""));
    }

    @Test
    public void testGetXY() {
        final List<Station> resultXY = resource.get("xy", null, null, null, null, null);
        final Station stationXY = resultXY.get(0);
        assertThat(stationXY, notNullValue());
        assertThat(stationXY.getKey(), equalTo(new Station.Key("xy", "5")));
        assertThat(stationXY.getTitle(), equalTo("Lummerland"));
        assertThat(stationXY.getCoordinates().getLat(), equalTo(50.0));
        assertThat(stationXY.getCoordinates().getLon(), equalTo(9.0));
        assertThat(stationXY.getPhotographer(), equalTo("Jim Knopf"));
        assertThat(stationXY.getDS100(), equalTo("XYZ"));
        assertThat(stationXY.getPhotoUrl(), equalTo("URL"));
        assertThat(stationXY.getLicense(), equalTo("CC0"));
        assertThat(stationXY.getPhotographerUrl(), equalTo("photographerUrl"));
    }

    @Test
    public void testGetAB() {
        final List<Station> resultAB = resource.get("ab", null, null, null, null, null);
        final Station station = resultAB.get(0);
        assertNimmerland(station);
    }

    private void assertNimmerland(final Station station) {
        assertThat(station, notNullValue());
        assertThat(station.getKey(), equalTo(new Station.Key("ab", "3")));
        assertThat(station.getTitle(), equalTo("Nimmerland"));
        assertThat(station.getCoordinates().getLat(), equalTo(40.0));
        assertThat(station.getCoordinates().getLon(), equalTo(6.0));
        assertThat(station.getPhotographer(), equalTo("Peter Pan"));
        assertThat(station.getPhotoUrl(), equalTo("URL2"));
        assertThat(station.getDS100(), equalTo("ABC"));
        assertThat(station.getLicense(), equalTo("CC0 by SA"));
        assertThat(station.getPhotographerUrl(), equalTo("photographerUrl2"));
    }

    @Test
    public void testGetById() {
        final Station station = resource.getById("ab", "3");
        assertNimmerland(station);
    }

    @Test
    public void testGetAll() {
        final List<Station> resultAll = resource.get(null, null, null, null, null, null);
        assertThat(resultAll.size(), equalTo(2));
    }

}
