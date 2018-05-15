package org.railwaystations.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.dropwizard.testing.ResourceHelpers;
import io.dropwizard.testing.junit5.DropwizardAppExtension;
import io.dropwizard.testing.junit5.DropwizardExtensionsSupport;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.railwaystations.api.mail.MockMailer;
import org.railwaystations.api.model.Photographer;
import org.railwaystations.api.model.Station;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Response;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.*;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;

@ExtendWith(DropwizardExtensionsSupport.class)
public class RsApiAppTest {

    public static final DropwizardAppExtension<RsApiConfiguration> RULE = MySuite.DROPWIZARD;

    private Client client;

    @BeforeEach
    public void setUp() {
        client = ClientBuilder.newClient();
    }

    @AfterEach
    public void tearDown() {
        client.close();
    }

    @Test
    public void stationsAllCountries() throws IOException {
        final Station[] stations = assertLoadStations("/stations", 200);
        assertThat(findById(stations, 41), notNullValue());
        assertThat(findById(stations, 8501042), notNullValue());
    }

    @Test
    public void stationById() throws IOException {
        final Response response = loadRaw("/de/stations/41", 200);
        final Station station = response.readEntity(Station.class);
        assertThat(station.getId(), is(41));
        assertThat(station.getTitle(), is( "Albersdorf"));
    }

    @Test
    public void stationsDe() throws IOException {
        final Station[] stations = assertLoadStations(String.format("/de/%s", "stations"), 200);
        assertThat(findById(stations, 41), notNullValue());
        assertThat(findById(stations, 8501042), nullValue());
    }

    @Test
    public void stationsDeQueryParam() throws IOException {
        final Station[] stations = assertLoadStations(String.format("/%s?country=de", "stations"), 200);
        assertThat(findById(stations, 41), notNullValue());
        assertThat(findById(stations, 8501042), nullValue());
    }

    @org.junit.jupiter.api.Test
    public void stationsDePhotograph() throws IOException {
        final Station[] stations = assertLoadStations(String.format("/de/%s?photographer=@hessenpfaelzer", "stations"), 200);
        assertThat(findById(stations, 7066), notNullValue());
    }

    @Test
    public void stationsCh() throws IOException {
        final Station[] stations = assertLoadStations(String.format("/ch/%s", "stations"), 200);
        assertThat(findById(stations, 8501042), notNullValue());
        assertThat(findById(stations, 41), nullValue());
    }

    @Test
    public void stationsUnknownCountry() throws IOException {
        assertLoadStations(String.format("/jp/%s", "stations"), 404);
    }

    @Test
    public void stationsDeFromAnonym() throws IOException {
        final Station[] stations = assertLoadStations(String.format("/de/%s?photographer=Anonym", "stations"), 200);
        assertThat(stations.length, is(9));
    }

    @Test
    public void stationsDeFromDgerkrathWithinMax5km() throws IOException {
        final Station[] stations = assertLoadStations(String.format("/de/%s?maxDistance=5&lat=51.2670337567818&lon=7.19520717859267&photographer=@Dgerkrath", "stations"), 200);
        assertThat(stations.length, is(3));
    }

    @Test
    public void stationsJson() throws IOException {
        final Response response = loadRaw(String.format("/de/%s.json", "stations"), 200);
        final ObjectMapper mapper = new ObjectMapper();
        final JsonNode jsonNode = mapper.readTree((InputStream) response.getEntity());
        assertThat(jsonNode, notNullValue());
        assertThat(jsonNode.isArray(), is(true));
        assertThat(jsonNode.size(), is(5652));
    }

    @Test
    public void stationsTxt() throws IOException {
        final Response response = loadRaw(String.format("/de/%s.txt", "stations"), 200);
        try (final BufferedReader br = new BufferedReader(new InputStreamReader((InputStream)response.getEntity(), "UTF-8"))) {
            final String header = br.readLine();
            assertThat(header, is("lat\tlon\ttitle\tdescription\ticon\ticonSize\ticonOffset"));
            int count = 0;
            final Pattern pattern = Pattern.compile("[\\d.]*\t[\\d.]*\t[^\t]*\t[^\t]*\t(gruen|rot)punkt\\.png\t10,10\t0,-10");
            while (br.ready()) {
                final String line = br.readLine();
                count++;
                final Matcher matcher = pattern.matcher(line);
                assertThat(matcher.matches(), is(true));
            }
            assertThat(count, is(5652));
        }
    }

    @Test
    public void stationsGpx() throws IOException, ParserConfigurationException, SAXException {
        final Response response = loadRaw(String.format("/ch/%s.gpx?hasPhoto=true", "stations"), 200);
        final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        final DocumentBuilder builder = factory.newDocumentBuilder();
        final String content = readSaveStringEntity(response);
        final Document doc = builder.parse(new InputSource(new StringReader(content)));
        final Element gpx = doc.getDocumentElement();
        assertThat(gpx.getTagName(), is("service"));
        assertThat(gpx.getAttribute("xmlns"), is("http://www.topografix.com/GPX/1/1"));
        assertThat(gpx.getAttribute("version"), is("1.1"));
        final NodeList wpts = gpx.getElementsByTagName("wpt");
        assertThat(wpts.getLength(), is(8));
    }

    private String readSaveStringEntity(final Response response) throws IOException {
        final byte[] buffer = new byte[16000];
        IOUtils.read((InputStream)response.getEntity(), buffer);
        return new String(buffer, "UTF-8").trim();
    }

    private Station[] assertLoadStations(final String path, final int expectedStatus) throws IOException {
        final Response response = loadRaw(path, expectedStatus);

        if (response == null) {
            return new Station[0];
        }
        return response.readEntity(Station[].class);
    }

    private Response loadRaw(final String path, final int expectedStatus) throws IOException {
        final Response response = client.target(
                String.format("http://localhost:%d%s", RULE.getLocalPort(), path))
                .request()
                .get();

        assertThat(response.getStatus(), is(expectedStatus));
        if (expectedStatus == 200) {
            return response;
        }
        return null;
    }

    private Station findById(final Station[] stations, final int id) {
        for (final Station station : stations) {
            if (station.getId() == id) {
                return station;
            }
        }
        return null;
    }

    @Test
    public void photographersJson() throws IOException {
        final Response response = loadRaw(String.format("/de/%s.json", "photographers"), 200);
        final ObjectMapper mapper = new ObjectMapper();
        final JsonNode jsonNode = mapper.readTree((InputStream) response.getEntity());
        assertThat(jsonNode, notNullValue());
        assertThat(jsonNode.isObject(), is(true));
        assertThat(jsonNode.size(), is(31));
    }

    @Test
    public void photographersTxt() throws IOException {
        final Response response = loadRaw(String.format("/de/%s.txt", "photographers"), 200);
        try (final BufferedReader br = new BufferedReader(new InputStreamReader((InputStream)response.getEntity(), "UTF-8"))) {
            final String header = br.readLine();
            assertThat(header, is("count\tphotographer"));
            int count = 0;
            final Pattern pattern = Pattern.compile("\\d[\\d]*\t[^\t]*");
            while (br.ready()) {
                final String line = br.readLine();
                count++;
                final Matcher matcher = pattern.matcher(line);
                assertThat(matcher.matches(), is(true));
            }
            assertThat(count, is(31));
        }
    }

    @Test
    public void statisticJson() throws IOException {
        final Response response = loadRaw(String.format("/de/%s.json", "stats"), 200);
        final ObjectMapper mapper = new ObjectMapper();
        final JsonNode jsonNode = mapper.readTree((InputStream) response.getEntity());
        assertThat(jsonNode, notNullValue());
        assertThat(jsonNode.isObject(), is(true));
        assertThat(jsonNode.size(), is(4));
    }

    @Test
    public void statisticTxt() throws IOException {
        final Response response = loadRaw(String.format("/de/%s.txt", "stats"), 200);
        try (final BufferedReader br = new BufferedReader(new InputStreamReader((InputStream)response.getEntity(), "UTF-8"))) {
            final String header = br.readLine();
            assertThat(header, is("name\tvalue"));
            int count = 0;
            final Pattern pattern = Pattern.compile("[^\t]*\t\\d[\\d]*");
            while (br.ready()) {
                final String line = br.readLine();
                count++;
                final Matcher matcher = pattern.matcher(line);
                assertThat(matcher.matches(), is(true));
            }
            assertThat(count, is(4));
        }
    }

    @Test
    public void register() {
        final MockMailer mailer = (MockMailer) RULE.getConfiguration().getMailer();

        final Response response = client.target(
                String.format("http://localhost:%d%s", RULE.getLocalPort(), "/registration"))
                .request()
                .header("API-Key", "dummy")
                .post(Entity.entity("{\n" +
                        "\t\"nickname\": \"nickname \", \n" +
                        "\t\"email\": \"nick.name@example.com\", \n" +
                        "\t\"license\": \"license\",\n" +
                        "\t\"photoOwner\": true, \n" +
                        "\t\"linking\": \"linking\", \n" +
                        "\t\"link\": \"\"\n" +
                        "}", "application/json"));

        assertThat(response.getStatus(), is(202));
        assertThat(mailer.getTo(), is("nick.name@example.com"));
        assertThat(mailer.getSubject(), is("Bahnhofsfotos upload token"));
        assertThat(mailer.getText(), is("Hallo nickname,\n\n" +
                "vielen Dank für Deine Registrierung.\n" +
                "Dein Upload Token lautet: edbfc44727a6fd4f5b029aff21861a667a6b4195\n" +
                "Klicke bitte auf http://railway-stations.org/uploadToken/edbfc44727a6fd4f5b029aff21861a667a6b4195 um ihn in die App zu übernehmen.\n" +
                "Alternativ kannst Du auch mit Deinem Smartphone den angehängten QR-Code scannen oder den Code manuell in der Bahnhofsfoto App unter 'Meine Daten' eintragen.\n\n" +
                "Viele Grüße\n" +
                "Dein Bahnhofsfoto-Team"));
    }

    @Test
    public void registerForbidden() {
        final Response response = client.target(
                String.format("http://localhost:%d%s", RULE.getLocalPort(), "/registration"))
                .request()
                .header("API-Key", "yummy")
                .post(Entity.entity("{\n" +
                        "\t\"nickname\": \"nickname\", \n" +
                        "\t\"email\": \"nick.name@example.com\", \n" +
                        "\t\"license\": \"license\",\n" +
                        "\t\"photoOwner\": true, \n" +
                        "\t\"linking\": \"linking\", \n" +
                        "\t\"link\": \"link\"\n" +
                        "}", "application/json"));

        assertThat(response.getStatus(), is(403));
    }

    @Test
    public void registerInvalid() {
        final Response response = client.target(
                String.format("http://localhost:%d%s", RULE.getLocalPort(), "/registration"))
                .request()
                .header("API-Key", "dummy")
                .post(Entity.entity("{\n" +
                        "\t\"nickname\": \"nickname\", \n" +
                        "\t\"email\": \"invalid email\", \n" +
                        "\t\"license\": \"license\",\n" +
                        "\t\"photoOwner\": true, \n" +
                        "\t\"linking\": \"linking\", \n" +
                        "\t\"link\": \"link\"\n" +
                        "}", "application/json"));

        assertThat(response.getStatus(), is(422));
    }

    @Test
    public void photoUploadForbidden() {
        final Response response = client.target(
                String.format("http://localhost:%d%s", RULE.getLocalPort(), "/photoUpload"))
                .request()
                .header("API-Key", "yummy")
                .header("Upload-Token", "edbfc44727a6fd4f5b029aff21861a667a6b4195")
                .header("Nickname", "nickname")
                .header("Email", "nickname@example.com")
                .header("Station-Id", "4711")
                .header("Country", "de")
                .post(Entity.entity("", "image/png"));

        assertThat(response.getStatus(), is(403));
    }

    @Test
    public void photographersLoader() {
        final Map<String, Photographer> photographers = RULE.getConfiguration().getPhotographerLoader().loadPhotographers();
        final Photographer photographer = photographers.get("SimoneDoerfling");
        assertThat(photographer.getName(), is("SimoneDoerfling"));
        assertThat(photographer.getUrl(), is("http://www.deutschlands-bahnhoefe.de/"));
        assertThat(photographer.getLicense(), is("CC0 1.0 Universell (CC0 1.0)"));
    }

    public static final class MySuite {
        public static final DropwizardAppExtension<RsApiConfiguration> DROPWIZARD = new DropwizardAppExtension<>(RsApiApp.class, ResourceHelpers.resourceFilePath("config.yml"));
    }
}
