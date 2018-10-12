package org.railwaystations.api.resources;

import javax.ws.rs.core.HttpHeaders;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.railwaystations.api.ElasticBackend;
import org.railwaystations.api.StationsRepository;
import org.railwaystations.api.loader.PhotographerLoader;
import org.railwaystations.api.loader.StationLoader;
import org.railwaystations.api.model.Coordinates;
import org.railwaystations.api.model.Country;
import org.railwaystations.api.model.Photo;
import org.railwaystations.api.model.Station;
import org.railwaystations.api.model.Station.Key;
import org.railwaystations.api.monitoring.LoggingMonitor;

import java.net.MalformedURLException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class StationsResourceTest {

    private StationsResource resource;
    private HttpHeaders httpHeaders;

    @BeforeEach
    public void setUp() throws MalformedURLException {
        final PhotographerLoader photographerLoader = new PhotographerLoader( "file:./src/test/resources/photographers.json", new ElasticBackend(""));

        final StationLoader loaderXY = Mockito.mock(StationLoader.class);
        final Map<Station.Key, Station> stationsXY = new HashMap<>(2);
        final Station.Key key5 = new Station.Key("xy", "5");
        stationsXY.put(key5, new Station(key5, "Lummerland", new Coordinates(50.0, 9.0), "XYZ", new Photo(key5, "URL", "Jim Knopf", "photographerUrl", null, "CC BY 3.0")));
        Mockito.when(loaderXY.loadStations(Mockito.anyMap(), Mockito.anyString())).thenReturn(stationsXY);
        Mockito.when(loaderXY.getCountry()).thenReturn(new Country("xy", null, null, null, null));

        final StationLoader loaderAB = Mockito.mock(StationLoader.class);
        final Map<Station.Key, Station> stations = new HashMap<>(2);
        final Station.Key key3 = new Station.Key("ab", "3");
        stations.put(key3, new Station(key3, "Nimmerland", new Coordinates(40.0, 6.0), "ABC", new Photo(key3, "URL2", "Peter Pan", "photographerUrl2", null, "CC BY-NC 4.0 International")));
        Mockito.when(loaderAB.loadStations(Mockito.anyMap(), Mockito.anyString())).thenReturn(stations);
        Mockito.when(loaderAB.getCountry()).thenReturn(new Country("ab", null, null, null, null));

        resource = new StationsResource(new StationsRepository(new LoggingMonitor(), Arrays.asList(loaderAB, loaderXY), photographerLoader, ""));
        
        httpHeaders = Mockito.mock(HttpHeaders.class);
        Mockito.when(httpHeaders.getAcceptableLanguages()).thenReturn(Collections.singletonList(Locale.GERMAN));
    }

    @Test
    public void testGetXY() {
        final List<Station> resultXY = resource.get(httpHeaders, "xy", null, null, null, null, null);
        final Station stationXY = resultXY.get(0);
        assertThat(stationXY, notNullValue());
        assertThat(stationXY.getKey(), equalTo(new Station.Key("xy", "5")));
        assertThat(stationXY.getTitle(), equalTo("Lummerland"));
        assertThat(stationXY.getCoordinates().getLat(), equalTo(50.0));
        assertThat(stationXY.getCoordinates().getLon(), equalTo(9.0));
        assertThat(stationXY.getPhotographer(), equalTo("Jim Knopf"));
        assertThat(stationXY.getDS100(), equalTo("XYZ"));
        assertThat(stationXY.getPhotoUrl(), equalTo("URL"));
        assertThat(stationXY.getLicense(), equalTo("CC BY 3.0"));
        assertThat(stationXY.getLicenseUrl(), equalTo("https://creativecommons.org/licenses/by/3.0/deed.de"));
        assertThat(stationXY.getPhotographerUrl(), equalTo("photographerUrl"));
    }

    @Test
    public void testGetAB() {
        final List<Station> resultAB = resource.get(httpHeaders, "ab", null, null, null, null, null);
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
        assertThat(station.getLicense(), equalTo("CC BY-NC 4.0 International"));
        assertThat(station.getLicenseUrl(), equalTo("https://creativecommons.org/licenses/by-nc/4.0/deed.de"));
        assertThat(station.getPhotographerUrl(), equalTo("photographerUrl2"));
    }

    @Test
    public void testGetById() {
        final Station station = resource.getById(httpHeaders, "ab", "3");
        assertNimmerland(station);
    }

    @Test
    public void testGetAll() {
        final List<Station> resultAll = resource.get(httpHeaders, null, null, null, null, null, null);
        assertThat(resultAll.size(), equalTo(2));
    }
    
    /**
     * No language set in HttpHeader, should return the default language (en).
     */
    @Test
    public void testGetById_NoLanguage() {
        HttpHeaders httpHeaders = Mockito.mock(HttpHeaders.class);
        Mockito.when(httpHeaders.getAcceptableLanguages()).thenReturn(Collections.emptyList());
        
        Station station = resource.getById(httpHeaders, "ab", "3");
        assertEquals("https://creativecommons.org/licenses/by-nc/4.0/deed.en", station.getLicenseUrl());
    }
    
    @Test
    public void testGetWithCountry_NoLanguage() {
        HttpHeaders httpHeaders = Mockito.mock(HttpHeaders.class);
        Mockito.when(httpHeaders.getAcceptableLanguages()).thenReturn(Collections.emptyList());
        
        List<Station> stations = resource.getWithCountry(httpHeaders, null, null, null, null, null, null);
        
        assertTrue(stations.stream().anyMatch(station -> "https://creativecommons.org/licenses/by/3.0/deed.en".equals(station.getLicenseUrl())));
        assertTrue(stations.stream().anyMatch(station -> "https://creativecommons.org/licenses/by-nc/4.0/deed.en".equals(station.getLicenseUrl())));
    }
    
    @Test
    public void testGetById_UnknownLicenseName() {
        StationsRepository repository = Mockito.mock(StationsRepository.class);
        
        Map<Key, Station> stationMap = Collections.singletonMap(new Station.Key("ab", "3"), new Station());
        Mockito.when(repository.get(ArgumentMatchers.anyString())).thenReturn(stationMap);
        
        Station station = new StationsResource(repository).getById(httpHeaders, "ab", "3");
        assertEquals("https://creativecommons.org/licenses/", station.getLicenseUrl());
    }
    
    @Test
    public void testGetWithCountry_UnknownLicenseName() {
        StationsRepository repository = Mockito.mock(StationsRepository.class);
        
        Map<Key, Station> stationMap = Collections.singletonMap(new Station.Key("ab", "3"), new Station());
        Mockito.when(repository.get(ArgumentMatchers.anyString())).thenReturn(stationMap);
        
        List<Station> stations = new StationsResource(repository).getWithCountry(httpHeaders, "ab", null, null, null, null, null);
        assertEquals("https://creativecommons.org/licenses/", stations.get(0).getLicenseUrl());
    }

}
