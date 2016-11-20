package github.pstorch.bahnhoefe.service;

import github.pstorch.bahnhoefe.service.loader.BahnhoefeLoader;
import org.junit.Test;
import org.mockito.Mockito;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

@SuppressWarnings("PMD.AvoidDuplicateLiterals")
public class BahnhoefeResourceTest {

    @Test
    public void testGet() throws IOException {
        final Map<String, BahnhoefeLoader> loaders = new HashMap<>(2);
        final BahnhoefeLoader loader = Mockito.mock(BahnhoefeLoader.class);
        loaders.put("de", loader);
        final BahnhoefeResource resource = new BahnhoefeResource(loaders);
        final Map<Integer, Bahnhof> bahnhoefe = new HashMap<>(2);
        bahnhoefe.put(5, new Bahnhof(5, "Lummerland", 50.0, 9.0, "Jim Knopf"));
        Mockito.when(loader.loadBahnhoefe()).thenReturn(bahnhoefe);

        final Iterator<Bahnhof> result = resource.get(null, null, null, null, null);
        final Bahnhof bahnhof = result.next();
        assertThat(bahnhof, notNullValue());
        assertThat(bahnhof.getId(), equalTo(5));
        assertThat(bahnhof.getTitle(), equalTo("Lummerland"));
        assertThat(bahnhof.getLat(), equalTo(50.0));
        assertThat(bahnhof.getLon(), equalTo(9.0));
        assertThat(bahnhof.getPhotographer(), equalTo("Jim Knopf"));
    }
}
