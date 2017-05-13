package github.pstorch.bahnhoefe.service.resources;

import github.pstorch.bahnhoefe.service.BahnhoefeRepository;
import github.pstorch.bahnhoefe.service.loader.BahnhoefeLoader;
import github.pstorch.bahnhoefe.service.model.Bahnhof;
import github.pstorch.bahnhoefe.service.model.Photo;
import github.pstorch.bahnhoefe.service.monitoring.LoggingMonitor;
import org.junit.Test;
import org.mockito.Mockito;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

public class BahnhoefeResourceTest {

    @Test
    public void testGet() throws IOException {
        final BahnhoefeLoader loaderXY = Mockito.mock(BahnhoefeLoader.class);
        final Map<Integer, Bahnhof> bahnhoefeXY = new HashMap<>(2);
        bahnhoefeXY.put(5, new Bahnhof(5, "xy", "Lummerland", 50.0, 9.0, "XYZ", new Photo(5, "Jim Knopf", "URL")));
        Mockito.when(loaderXY.loadBahnhoefe()).thenReturn(bahnhoefeXY);
        Mockito.when(loaderXY.getCountryCode()).thenReturn("xy");

        final BahnhoefeLoader loaderAB = Mockito.mock(BahnhoefeLoader.class);
        final Map<Integer, Bahnhof> bahnhoefe = new HashMap<>(2);
        bahnhoefe.put(3, new Bahnhof(3, "ab", "Nimmerland", 40.0, 6.0, "ABC", new Photo(3, "Peter Pan", "URL2")));
        Mockito.when(loaderAB.loadBahnhoefe()).thenReturn(bahnhoefe);
        Mockito.when(loaderAB.getCountryCode()).thenReturn("ab");

        final BahnhoefeResource resource = new BahnhoefeResource(new BahnhoefeRepository(new LoggingMonitor(), loaderAB, loaderXY));

        final List<Bahnhof> resultXY = resource.get("xy", null, null, null, null, null);
        final Bahnhof bahnhofXY = resultXY.get(0);
        assertThat(bahnhofXY, notNullValue());
        assertThat(bahnhofXY.getId(), equalTo(5));
        assertThat(bahnhofXY.getTitle(), equalTo("Lummerland"));
        assertThat(bahnhofXY.getLat(), equalTo(50.0));
        assertThat(bahnhofXY.getLon(), equalTo(9.0));
        assertThat(bahnhofXY.getPhotographer(), equalTo("Jim Knopf"));
        assertThat(bahnhofXY.getDS100(), equalTo("XYZ"));
        assertThat(bahnhofXY.getPhotoUrl(), equalTo("URL"));

        final List<Bahnhof> resultAB = resource.get("ab", null, null, null, null, null);
        final Bahnhof bahnhof = resultAB.get(0);
        assertThat(bahnhof, notNullValue());
        assertThat(bahnhof.getId(), equalTo(3));
        assertThat(bahnhof.getTitle(), equalTo("Nimmerland"));
        assertThat(bahnhof.getLat(), equalTo(40.0));
        assertThat(bahnhof.getLon(), equalTo(6.0));
        assertThat(bahnhof.getPhotographer(), equalTo("Peter Pan"));
        assertThat(bahnhof.getPhotoUrl(), equalTo("URL2"));
        assertThat(bahnhof.getDS100(), equalTo("ABC"));

        final List<Bahnhof> resultAll = resource.get(null, null, null, null, null, null);
        assertThat(resultAll.size(), equalTo(2));
    }

}
