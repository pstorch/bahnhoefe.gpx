package org.railwaystations.api.resources;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.railwaystations.api.BahnhoefeRepository;
import org.railwaystations.api.loader.BahnhoefeLoader;
import org.railwaystations.api.loader.PhotographerLoader;
import org.railwaystations.api.model.Station;
import org.railwaystations.api.model.Coordinates;
import org.railwaystations.api.model.Country;
import org.railwaystations.api.model.Photo;
import org.railwaystations.api.monitoring.LoggingMonitor;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

public class BahnhoefeResourceTest {

    private BahnhoefeResource resource;

    @Before
    public void setUp() throws MalformedURLException {
        final PhotographerLoader photographerLoader = new PhotographerLoader( new URL("file:./src/test/resources/photographers.json"));

        final BahnhoefeLoader loaderXY = Mockito.mock(BahnhoefeLoader.class);
        final Map<Integer, Station> bahnhoefeXY = new HashMap<>(2);
        bahnhoefeXY.put(5, new Station(5, "xy", "Lummerland", new Coordinates(50.0, 9.0), "XYZ", new Photo(5, "URL", "Jim Knopf", "photographerUrl", null, "CC0", "licenseUrl")));
        Mockito.when(loaderXY.loadBahnhoefe(Mockito.anyMap(), Mockito.anyString())).thenReturn(bahnhoefeXY);
        Mockito.when(loaderXY.getCountry()).thenReturn(new Country("xy", null, null, null, null));

        final BahnhoefeLoader loaderAB = Mockito.mock(BahnhoefeLoader.class);
        final Map<Integer, Station> bahnhoefe = new HashMap<>(2);
        bahnhoefe.put(3, new Station(3, "ab", "Nimmerland", new Coordinates(40.0, 6.0), "ABC", new Photo(3, "URL2", "Peter Pan", "photographerUrl2", null, "CC0 by SA", "licenseUrl2")));
        Mockito.when(loaderAB.loadBahnhoefe(Mockito.anyMap(), Mockito.anyString())).thenReturn(bahnhoefe);
        Mockito.when(loaderAB.getCountry()).thenReturn(new Country("ab", null, null, null, null));

        resource = new BahnhoefeResource(new BahnhoefeRepository(new LoggingMonitor(), Arrays.asList(loaderAB, loaderXY), photographerLoader, ""));
    }

    @Test
    public void testGetXY() throws IOException {
        final List<Station> resultXY = resource.get("xy", null, null, null, null, null);
        final Station bahnhofXY = resultXY.get(0);
        assertThat(bahnhofXY, notNullValue());
        assertThat(bahnhofXY.getId(), equalTo(5));
        assertThat(bahnhofXY.getTitle(), equalTo("Lummerland"));
        assertThat(bahnhofXY.getLat(), equalTo(50.0));
        assertThat(bahnhofXY.getLon(), equalTo(9.0));
        assertThat(bahnhofXY.getPhotographer(), equalTo("Jim Knopf"));
        assertThat(bahnhofXY.getDS100(), equalTo("XYZ"));
        assertThat(bahnhofXY.getPhotoUrl(), equalTo("URL"));
        assertThat(bahnhofXY.getLicense(), equalTo("CC0"));
        assertThat(bahnhofXY.getPhotographerUrl(), equalTo("photographerUrl"));
    }

    @Test
    public void testGetAB() throws IOException {
        final List<Station> resultAB = resource.get("ab", null, null, null, null, null);
        final Station bahnhof = resultAB.get(0);
        assertNimmerland(bahnhof);
    }

    private void assertNimmerland(final Station bahnhof) {
        assertThat(bahnhof, notNullValue());
        assertThat(bahnhof.getId(), equalTo(3));
        assertThat(bahnhof.getTitle(), equalTo("Nimmerland"));
        assertThat(bahnhof.getLat(), equalTo(40.0));
        assertThat(bahnhof.getLon(), equalTo(6.0));
        assertThat(bahnhof.getPhotographer(), equalTo("Peter Pan"));
        assertThat(bahnhof.getPhotoUrl(), equalTo("URL2"));
        assertThat(bahnhof.getDS100(), equalTo("ABC"));
        assertThat(bahnhof.getLicense(), equalTo("CC0 by SA"));
        assertThat(bahnhof.getPhotographerUrl(), equalTo("photographerUrl2"));
    }

    @Test
    public void testGetById() throws IOException {
        final Station bahnhof = resource.getById("ab", 3);
        assertNimmerland(bahnhof);
    }

    @Test
    public void testGetAll() throws IOException {
        final List<Station> resultAll = resource.get(null, null, null, null, null, null);
        assertThat(resultAll.size(), equalTo(2));
    }

}
