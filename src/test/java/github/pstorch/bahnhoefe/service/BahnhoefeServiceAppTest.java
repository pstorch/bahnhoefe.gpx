package github.pstorch.bahnhoefe.service;

import io.dropwizard.testing.ConfigOverride;
import io.dropwizard.testing.ResourceHelpers;
import io.dropwizard.testing.junit.DropwizardAppRule;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.Response;
import java.io.IOException;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

public class BahnhoefeServiceAppTest {

    @ClassRule
    public static final DropwizardAppRule<BahnhoefeServiceConfiguration> RULE =
            new DropwizardAppRule<>(BahnhoefeServiceApp.class, ResourceHelpers.resourceFilePath("config.yml"),
                    ConfigOverride.config("loaderDe.bahnhoefeUrl", BahnhoefeServiceAppTest.class.getResource("/bahnhoefeDe.json").toString()),
                    ConfigOverride.config("loaderDe.photosUrl", BahnhoefeServiceAppTest.class.getResource("/photosDe.json").toString()),
                    ConfigOverride.config("loaderCh.bahnhoefeUrl", BahnhoefeServiceAppTest.class.getResource("/bahnhoefeCh.json").toString()),
                    ConfigOverride.config("loaderCh.photosUrl", BahnhoefeServiceAppTest.class.getResource("/photosCh.json").toString()));

    private Client client;

    @Before
    public void setUp() throws Exception {
        client = ClientBuilder.newClient();
    }

    @After
    public void tearDown() throws Exception {
        client.close();
    }

    @Test
    public void bahnhoefeDefaultCountry() throws IOException {
        final Bahnhof[] bahnhoefe = loadBahnhoefe("/bahnhoefe", 200);
        assertThat(findById(bahnhoefe, 41), notNullValue());
    }

    @Test
    public void bahnhoefeDe() throws IOException {
        final Bahnhof[] bahnhoefe = loadBahnhoefe("/de/bahnhoefe", 200);
        assertThat(findById(bahnhoefe, 41), notNullValue());
    }

    @Test
    public void bahnhoefeDePhotograph() throws IOException {
        final Bahnhof[] bahnhoefe = loadBahnhoefe("/de/bahnhoefe?photographer=@hessenpfaelzer", 200);
        assertThat(findById(bahnhoefe, 7066), notNullValue());
    }

    @Test
    public void bahnhoefeCh() throws IOException {
        final Bahnhof[] bahnhoefe = loadBahnhoefe("/ch/bahnhoefe", 200);
        assertThat(findById(bahnhoefe, 8501042), notNullValue());
    }

    @Test
    public void bahnhoefeUnknownCountry() throws IOException {
        loadBahnhoefe("/jp/bahnhoefe", 404);
    }

    @Test
    public void bahnhoefeWithPhotoDefaultCountry() throws IOException {
        final Bahnhof[] bahnhoefe = loadBahnhoefe("/bahnhoefe-withPhoto", 200);
        assertThat(findById(bahnhoefe, 7066), notNullValue());
    }

    @Test
    public void bahnhoefeWithPhotoDe() throws IOException {
        final Bahnhof[] bahnhoefe = loadBahnhoefe("/de/bahnhoefe-withPhoto", 200);
        assertThat(findById(bahnhoefe, 7066), notNullValue());
    }

    @Test
    public void bahnhoefeWithPhotoCh() throws IOException {
        final Bahnhof[] bahnhoefe = loadBahnhoefe("/ch/bahnhoefe-withPhoto", 200);
        assertThat(findById(bahnhoefe, 8509195), notNullValue());
    }

    @Test
    public void bahnhoefeWithPhotoUnknownCountry() throws IOException {
        loadBahnhoefe("/jp/bahnhoefe-withPhoto", 404);
    }

    @Test
    public void bahnhoefeWithoutPhotoDefaultCountry() throws IOException {
        final Bahnhof[] bahnhoefe = loadBahnhoefe("/bahnhoefe-withoutPhoto", 200);
        assertThat(findById(bahnhoefe, 41), notNullValue());
    }

    @Test
    public void bahnhoefeWithoutPhotoDe() throws IOException {
        final Bahnhof[] bahnhoefe = loadBahnhoefe("/de/bahnhoefe-withoutPhoto", 200);
        assertThat(findById(bahnhoefe, 41), notNullValue());
    }

    @Test
    public void bahnhoefeWithoutPhotoCh() throws IOException {
        final Bahnhof[] bahnhoefe = loadBahnhoefe("/ch/bahnhoefe-withoutPhoto", 200);
        assertThat(findById(bahnhoefe, 8501042), notNullValue());
    }

    @Test
    public void bahnhoefeWithoutPhotoUnknownCountry() throws IOException {
        loadBahnhoefe("/jp/bahnhoefe-withoutPhoto", 404);
    }

    @Test
    public void bahnhoefeDeFromAndroidOma() throws IOException {
        final Bahnhof[] bahnhoefe = loadBahnhoefe("/de/bahnhoefe?photographer=@android_oma", 200);
        assertThat(bahnhoefe.length, is(31));
    }

    @Test
    public void bahnhoefeDeFromAndroidOmaWithinMax30kmFromFfmHbf() throws IOException {
        final Bahnhof[] bahnhoefe = loadBahnhoefe("/de/bahnhoefe?maxDistance=30&lat=50.1060866&lon=8.6615762&photographer=@android_oma", 200);
        assertThat(bahnhoefe.length, is(17));
    }

    private Bahnhof[] loadBahnhoefe(final String path, final int expectedStatus) throws IOException {
        final Response response = client.target(
                String.format("http://localhost:%d%s", RULE.getLocalPort(), path))
                .request()
                .get();

        assertThat(response.getStatus(), is(expectedStatus));
        if (expectedStatus == 200) {
            return response.readEntity(Bahnhof[].class);
        }
        return null;
    }

    private Bahnhof findById(final Bahnhof[] bahnhoefe, int id) {
        for (final Bahnhof bahnhof : bahnhoefe) {
            if (bahnhof.getId() == id) {
                return bahnhof;
            }
        }
        return null;
    }

}
