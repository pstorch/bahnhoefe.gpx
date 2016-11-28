package github.pstorch.bahnhoefe.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jcabi.http.mock.MkAnswer;
import com.jcabi.http.mock.MkContainer;
import com.jcabi.http.mock.MkGrizzlyContainer;
import io.dropwizard.testing.ConfigOverride;
import io.dropwizard.testing.DropwizardTestSupport;
import io.dropwizard.testing.ResourceHelpers;
import org.junit.*;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.Response;
import java.io.IOException;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class BahnhofServiceAppTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();


    private static DropwizardTestSupport<BahnhoefeServiceConfiguration> SUPPORT = null;

    private static final MkContainer containerDe = new MkGrizzlyContainer();

    private static final MkContainer containerCh = new MkGrizzlyContainer();

    private Client client;

    @BeforeClass
    public static void setUpClass() throws Exception {
        containerCh.next(new MkAnswer.Simple(
                "[{\"bahnhofsname\":\"Küngoldingen\",\"uic_ref\":\"8502120\",\"bahnhofsfoto\":\"http:\\/\\/www.deutschlands-bahnhoefe.org\\/sites\\/default\\/files\\/previewbig\\/8503006.jpg\",\"fotograf\":\"http:\\/\\/www.deutschlands-bahnhoefe.org\\/node\\/1473 \",\"lizenz\":\"CC0 1.0 Universell (CC0 1.0) \",\"bahnhofsfoto480\":\"\\/sites\\/default\\/files\\/styles\\/large\\/public\\/previewbig\\/8503006.jpg?itok=cGcfrbBm\",\"fotograf-title\":\"@pokipsie\"}]}"));
        containerCh.next(new MkAnswer.Simple(
                "{\"took\":19,\"timed_out\":false,\"_shards\":{\"total\":5,\"successful\":5,\"failed\":0},\"hits\":{\"total\":1733,\"max_score\":1.0,\"hits\":[{\"_index\":\"bahnhoefech\",\"_type\":\"bahnhofch\",\"_id\":\"AVfycusBwbtQZche7cDc\",\"_score\":1.0,\"_source\":{\"datasetid\":\"didok-liste\",\"recordid\":\"7a41451eba36a7b6ee9270d632f4f56cafb2a25b\",\"fields\":{\"geopos\":[46.47515995026246,6.427139085981211],\"betriebspunkttyp\":\"Haltestelle\",\"abkuerzung\":\"ETOY\",\"didok85\":8501042,\"didok\":1042,\"lod\":\"http://lod.opentransportdata.swiss/didok/8501042\",\"x_koord_nord\":147595,\"xtf_id\":\"ch14uvag00045514\",\"beginngueltigkeit\":\"2014-12-01\",\"bearbeitungsdatum\":20151204,\"tunummer\":1,\"dst_abk\":\"ETOY\",\"gdenummer\":5636,\"y_koord_ost\":522318,\"nummer\":8501042,\"verkehrsmittel\":\"Zug\",\"gdename\":\"Etoy\",\"name\":\"Etoy\",\"tuabkuerzung\":\"SBBCFFFFS\",\"datenherrabkuerzung\":\"SBBCFFFFS\",\"stand\":\"2015-12-13\",\"hoehe\":406},\"geometry\":{\"type\":\"Point\",\"coordinates\":[6.427139085981211,46.47515995026246]},\"record_timestamp\":\"2016-05-03T22:14:34+02:00\"}},{\"_index\":\"bahnhoefech\",\"_type\":\"bahnhofch\",\"_id\":\"AVfycusBwbtQZche7cDg\",\"_score\":1.0,\"_source\":{\"datasetid\":\"didok-liste\",\"recordid\":\"053738e5c620d6dc0e40d70c6c18711afea83998\",\"fields\":{\"geopos\":[47.30746905671105,7.941984790513125],\"betriebspunkttyp\":\"Haltestelle\",\"abkuerzung\":\"KGD\",\"didok85\":8502120,\"didok\":2120,\"lod\":\"http://lod.opentransportdata.swiss/didok/8502120\",\"x_koord_nord\":239743,\"xtf_id\":\"ch14uvag00050130\",\"beginngueltigkeit\":\"2014-12-01\",\"bearbeitungsdatum\":20151204,\"tunummer\":1,\"dst_abk\":\"KGD\",\"gdenummer\":4280,\"y_koord_ost\":638064,\"nummer\":8502120,\"verkehrsmittel\":\"Zug\",\"gdename\":\"Oftringen\",\"name\":\"Küngoldingen\",\"tuabkuerzung\":\"SBBCFFFFS\",\"datenherrabkuerzung\":\"SBBCFFFFS\",\"stand\":\"2015-12-13\",\"hoehe\":438},\"geometry\":{\"type\":\"Point\",\"coordinates\":[7.941984790513125,47.30746905671105]},\"record_timestamp\":\"2016-05-03T22:14:34+02:00\"}}]}}"));
        containerCh.start();

        containerDe.next(new MkAnswer.Simple(
                "[{\"bahnhofsname\":\"Zweibr\\u00fccken Hbf\",\"bahnhofsnr\":\"7066\",\"bahnhofsfoto\":\" http:\\/\\/www.deutschlands-bahnhoefe.org\\/sites\\/default\\/files\\/previewbig\\/7066_1.jpg\",\"fotograf\":\"http:\\/\\/www.deutschlands-bahnhoefe.org\\/node\\/278 \",\"lizenz\":\"CC0 1.0 Universell (CC0 1.0) \",\"bahnhofsfoto480\":\"  http:\\/\\/www.deutschlands-bahnhoefe.org\\/sites\\/default\\/files\\/styles\\/large\\/public\\/previewbig\\/7066_1.jpg?itok=1zGEsvom\\n\", \"fotograf-title\": \"@hessenpfaelzer\"},{\"bahnhofsname\":\"Z\\u00fcssow\",\"bahnhofsnr\":\"7063\",\"bahnhofsfoto\":\" http:\\/\\/www.deutschlands-bahnhoefe.org\\/sites\\/default\\/files\\/previewbig\\/7063_0.jpg\",\"fotograf\":\"http:\\/\\/www.deutschlands-bahnhoefe.org\\/node\\/157 \",\"lizenz\":\"CC0 1.0 Universell (CC0 1.0)\",\"bahnhofsfoto480\":\"  http:\\/\\/www.deutschlands-bahnhoefe.org\\/sites\\/default\\/files\\/styles\\/large\\/public\\/previewbig\\/7063_0.jpg?itok=PG48v2wy\\n\", \"fotograf-title\": \"@android_oma\"}]"));
        containerDe.next(new MkAnswer.Simple(
                "{\"took\":2,\"timed_out\":false,\"_shards\":{\"total\":5,\"successful\":5,\"failed\":0},\"hits\":{\"total\":5652,\"max_score\":1.0,\"hits\":[{\"_index\":\"bahnhoefe\",\"_type\":\"bahnhof\",\"_id\":\"AVLb3n3hwbtQZche7aaV\",\"_score\":1.0,\"_source\":{\"Bundesland\":\"Schleswig-Holstein\",\"Bahnhofsmanagement\":\"Schleswig-Holstein\",\"BahnhofNr\":41,\"title\":\"Albersdorf\",\"DS100\":\"AAL\",\"Bahnhofskategorie\":7,\"Strasse\":\"Bahnhofstr. 1\",\"PLZ\":25767,\"Ort\":\"Albersdorf\",\"Aufgabenträger\":\"LVS Schleswig-Holstein Landesweite Verkehrsservicegesellschaft mbH\",\"Verkehrsverb\":\"0\",\"Fernverkehr\":\"nein\",\"Nahverkehr\":\"ja\",\"lat\":54.1461697552048,\"lon\":9.29245591163636}},{\"_index\":\"bahnhoefe\",\"_type\":\"bahnhof\",\"_id\":\"AVLb3n5QwbtQZche7bja\",\"_score\":1.0,\"_source\":{\"Bundesland\":\"Rheinland-Pfalz\",\"Bahnhofsmanagement\":\"Kaiserslautern\",\"BahnhofNr\":7066,\"title\":\"Zweibrücken Hbf\",\"DS100\":\"SZW\",\"Bahnhofskategorie\":5,\"Strasse\":\"Poststr. 37\",\"PLZ\":66482,\"Ort\":\"Zweibrücken\",\"Aufgabenträger\":\"Zweckverband Schienenpersonennahverkehr Rheinland-Pfalz Süd\",\"Verkehrsverb\":\"VRN\",\"Fernverkehr\":\"nein\",\"Nahverkehr\":\"ja\",\"lat\":49.2467252285295,\"lon\":7.35692381858826}}]}}"));
        containerDe.start();

        SUPPORT = new DropwizardTestSupport<>(BahnhoefeServiceApp.class,
                ResourceHelpers.resourceFilePath("config.yml"),
                ConfigOverride.config("loaderDe.bahnhoefeUrl", containerDe.home().toString() + "/bahnhoefeDe"),
                ConfigOverride.config("loaderDe.photosUrl", containerDe.home().toString() + "/photosDe"),
                ConfigOverride.config("loaderCh.bahnhoefeUrl", containerCh.home().toString() + "/bahnhoefeCh"),
                ConfigOverride.config("loaderCh.photosUrl", containerCh.home().toString() + "/photosCh"));
        SUPPORT.before();
    }

    @AfterClass
    public static void tearDownClass() {
        SUPPORT.after();
        containerDe.stop();
        containerCh.stop();
    }

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
        assertThat(bahnhoefe[0].getId(), is(41));
    }

    @Test
    public void bahnhoefeDe() throws IOException {
        final Bahnhof[] bahnhoefe = loadBahnhoefe("/de/bahnhoefe", 200);
        assertThat(bahnhoefe[0].getId(), is(41));
    }

    @Test
    public void bahnhoefeDePhotograph() throws IOException {
        final Bahnhof[] bahnhoefe = loadBahnhoefe("/de/bahnhoefe?photographer=@hessenpfaelzer", 200);
        assertThat(bahnhoefe[0].getId(), is(7066));
    }

    @Test
    public void bahnhoefeCh() throws IOException {
        final Bahnhof[] bahnhoefe = loadBahnhoefe("/ch/bahnhoefe", 200);
        assertThat(bahnhoefe[0].getId(), is(8501042));
    }

    @Test
    public void bahnhoefeUnknownCountry() throws IOException {
        loadBahnhoefe("/jp/bahnhoefe", 404);
    }

    @Test
    public void bahnhoefeWithPhotoDefaultCountry() throws IOException {
        final Bahnhof[] bahnhoefe = loadBahnhoefe("/bahnhoefe-withPhoto", 200);
        assertThat(bahnhoefe[0].getId(), is(7066));
    }

    @Test
    public void bahnhoefeWithPhotoDe() throws IOException {
        final Bahnhof[] bahnhoefe = loadBahnhoefe("/de/bahnhoefe-withPhoto", 200);
        assertThat(bahnhoefe[0].getId(), is(7066));
    }

    @Test
    public void bahnhoefeWithPhotoCh() throws IOException {
        final Bahnhof[] bahnhoefe = loadBahnhoefe("/ch/bahnhoefe-withPhoto", 200);
        assertThat(bahnhoefe[0].getId(), is(8502120));
    }

    @Test
    public void bahnhoefeWithPhotoUnknownCountry() throws IOException {
        loadBahnhoefe("/jp/bahnhoefe-withPhoto", 404);
    }

    @Test
    public void bahnhoefeWithoutPhotoDefaultCountry() throws IOException {
        final Bahnhof[] bahnhoefe = loadBahnhoefe("/bahnhoefe-withoutPhoto", 200);
        assertThat(bahnhoefe[0].getId(), is(41));
    }

    @Test
    public void bahnhoefeWithoutPhotoDe() throws IOException {
        final Bahnhof[] bahnhoefe = loadBahnhoefe("/de/bahnhoefe-withoutPhoto", 200);
        assertThat(bahnhoefe[0].getId(), is(41));
    }

    @Test
    public void bahnhoefeWithoutPhotoCh() throws IOException {
        final Bahnhof[] bahnhoefe = loadBahnhoefe("/ch/bahnhoefe-withoutPhoto", 200);
        assertThat(bahnhoefe[0].getId(), is(8501042));
    }

    @Test
    public void bahnhoefeWithoutPhotoUnknownCountry() throws IOException {
        loadBahnhoefe("/jp/bahnhoefe-withoutPhoto", 404);
    }

    private Bahnhof[] loadBahnhoefe(final String path, final int expectedStatus) throws IOException {
        Response response = client.target(
                String.format("http://localhost:%d%s", SUPPORT.getLocalPort(), path))
                .request()
                .get();

        assertThat(response.getStatus(), is(expectedStatus));
        if (expectedStatus == 200) {
            return response.readEntity(Bahnhof[].class);
        }
        return null;
    }

}
