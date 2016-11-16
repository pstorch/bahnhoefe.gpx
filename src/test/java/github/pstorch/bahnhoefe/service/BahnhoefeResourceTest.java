package github.pstorch.bahnhoefe.service;

import github.pstorch.bahnhoefe.service.loader.BahnhoefeLoader;
import org.hamcrest.CoreMatchers;
import org.hamcrest.MatcherAssert;
import org.junit.Test;
import org.mockito.Mockito;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

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
        MatcherAssert.assertThat(bahnhof, CoreMatchers.notNullValue());
        MatcherAssert.assertThat(bahnhof.getId(), CoreMatchers.equalTo(5));
        MatcherAssert.assertThat(bahnhof.getTitle(), CoreMatchers.equalTo("Lummerland"));
        MatcherAssert.assertThat(bahnhof.getLat(), CoreMatchers.equalTo(50.0));
        MatcherAssert.assertThat(bahnhof.getLon(), CoreMatchers.equalTo(9.0));
        MatcherAssert.assertThat(bahnhof.getPhotographer(), CoreMatchers.equalTo("Jim Knopf"));
    }
}
