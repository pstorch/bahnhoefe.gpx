package org.railwaystations.rsapi;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.railwaystations.rsapi.mail.Mailer;
import org.railwaystations.rsapi.mail.MockMailer;
import org.railwaystations.rsapi.model.Station;
import org.railwaystations.rsapi.utils.ImageUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.http.ResponseEntity;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xmlunit.builder.Input;

import javax.imageio.ImageIO;
import javax.xml.bind.DatatypeConverter;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Base64;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class RsapiApplicationTests {

	private static final ObjectMapper MAPPER = new ObjectMapper();

	@LocalServerPort
	private int port;

	@Autowired
	private TestRestTemplate restTemplate;

	@MockBean
	private Mailer mailer;

	@Test
	void contextLoads() {
	}

	@Test
	public void stationsAllCountries() {
		final Station[] stations = assertLoadStations("/stations", 200);
		assertThat(stations.length, is(954));
		assertThat(findByKey(stations, new Station.Key("de", "6721")), notNullValue());
		assertThat(findByKey(stations, new Station.Key("ch", "8500126")), notNullValue());
	}

	@Test
	public void stationById() {
		final Station station = getStation("/de/stations/6932");
		assertThat(station.getKey().getId(), is("6932"));
		assertThat(station.getTitle(), is( "Wuppertal-Ronsdorf"));
		assertThat(station.getPhotoUrl(), is("https://api.railway-stations.org/photos/de/6932.jpg"));
		assertThat(station.getPhotographer(), is("@khgdrn"));
		assertThat(station.getLicense(), is("CC0 1.0 Universell (CC0 1.0)"));
		assertThat(station.isActive(), is(true));
	}

	@Test
	public void stationByIdNotFound() {
		loadRaw("/de/stations/11111111111", 404, String.class);
	}

	@Test
	public void stationsDe() {
		final Station[] stations = assertLoadStations(String.format("/de/%s", "stations"), 200);
		assertThat(findByKey(stations, new Station.Key("de", "6721")), notNullValue());
		assertThat(findByKey(stations, new Station.Key("ch", "8500126")), nullValue());
	}

	@Test
	public void stationsDeQueryParam() {
		final Station[] stations = assertLoadStations(String.format("/%s?country=de", "stations"), 200);
		assertThat(findByKey(stations, new Station.Key("de", "6721")), notNullValue());
		assertThat(findByKey(stations, new Station.Key("ch", "8500126")), nullValue());
	}

	@Test
	public void stationsDeChQueryParam() {
		final Station[] stations = assertLoadStations(String.format("/%s?country=de&country=ch", "stations"), 200);
		assertThat(findByKey(stations, new Station.Key("de", "6721")), notNullValue());
		assertThat(findByKey(stations, new Station.Key("ch", "8500126")), notNullValue());
	}

	@Test
	public void stationsDePhotograph() {
		final Station[] stations = assertLoadStations(String.format("/de/%s?photographer=@khgdrn", "stations"), 200);
		assertThat(findByKey(stations, new Station.Key("de", "6966")), notNullValue());
	}

	@Test
	public void stationsCh() {
		final Station[] stations = assertLoadStations(String.format("/ch/%s", "stations"), 200);
		assertThat(findByKey(stations, new Station.Key("ch", "8500126")), notNullValue());
		assertThat(findByKey(stations, new Station.Key("de", "6721")), nullValue());
	}

	@Test
	public void stationsUnknownCountry() {
		assertLoadStations("/jp/stations", 404);
	}

	@Test
	public void stationsDeFromAnonym() {
		final Station[] stations = assertLoadStations("/de/stations?photographer=Anonym", 200);
		assertThat(stations.length, is(9));
	}

	@Test
	public void stationsDeFromDgerkrathWithinMax5km() {
		final Station[] stations = assertLoadStations("/de/stations?maxDistance=5&lat=49.0065325041363&lon=13.2770955562592&photographer=@stefanopitz", 200);
		assertThat(stations.length, is(2));
	}

	@Test
	public void stationsJson() throws IOException {
		final ResponseEntity<InputStream> response = loadRaw("/de/stations.json", 200, InputStream.class);
		final JsonNode jsonNode = MAPPER.readTree((InputStream) response.getBody());
		assertThat(jsonNode, notNullValue());
		assertThat(jsonNode.isArray(), is(true));
		assertThat(jsonNode.size(), is(729));
	}

	@Test
	public void stationsGpx() throws IOException, ParserConfigurationException, SAXException {
		final ResponseEntity<String> response = loadRaw(String.format("/ch/%s.gpx?hasPhoto=true", "stations"), 200, String.class);
		final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		final DocumentBuilder builder = factory.newDocumentBuilder();
		final String content = readSaveStringEntity(response);
		final Document doc = builder.parse(new InputSource(new StringReader(content)));
		final Element gpx = doc.getDocumentElement();
		assertThat(response.getHeaders().getFirst("Content-Type"), is("application/gpx+xml"));
		assertThat(gpx.getTagName(), is("gpx"));
		assertThat(gpx.getAttribute("xmlns"), is("http://www.topografix.com/GPX/1/1"));
		assertThat(gpx.getAttribute("version"), is("1.1"));
		final NodeList wpts = gpx.getElementsByTagName("wpt");
		assertThat(wpts.getLength(), is(7));
	}

	private String readSaveStringEntity(final ResponseEntity<String> response) {
		return response.getBody();
	}

	private Station[] assertLoadStations(final String path, final int expectedStatus) {
		final ResponseEntity<Station[]> response = loadRaw(path, expectedStatus, Station[].class);

		if (response.getStatusCodeValue() != 200) {
			return new Station[0];
		}
		return response.getBody();
	}

	private <T> ResponseEntity<T>  loadRaw(final String path, final int expectedStatus, final Class<T> responseType) {
		final ResponseEntity<T> response = restTemplate.getForEntity(String.format("http://localhost:%d%s", port, path),
				responseType);

		assertThat(response.getStatusCodeValue(), is(expectedStatus));
		return response;
	}

	private Station findByKey(final Station[] stations, final Station.Key key) {
		for (final Station station : stations) {
			if (station.getKey().equals(key)) {
				return station;
			}
		}
		return null;
	}

	@Test
	public void photographersJson() throws IOException {
		final ResponseEntity<String> response = loadRaw(String.format("/de/%s.json", "photographers"), 200, String.class);
		final JsonNode jsonNode = MAPPER.readTree(response.getBody());
		assertThat(jsonNode, notNullValue());
		assertThat(jsonNode.isObject(), is(true));
		assertThat(jsonNode.size(), is(4));
	}

	@Test
	public void photographersAllJson() throws IOException {
		final ResponseEntity<String> response = loadRaw("/photographers.json", 200, String.class);
		final JsonNode jsonNode = MAPPER.readTree(response.getBody());
		assertThat(jsonNode, notNullValue());
		assertThat(jsonNode.isObject(), is(true));
		assertThat(jsonNode.size(), is(6));
	}

	@Test
	public void photographersTxt() throws IOException {
		final ResponseEntity<InputStream> response = loadRaw("/de/photographers.txt", 200, InputStream.class);
		try (final BufferedReader br = new BufferedReader(new InputStreamReader(response.getBody(), StandardCharsets.UTF_8))) {
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
			assertThat(count, is(4));
		}
	}

	private Station getStation(final String url) {
		return loadRaw(url, 200, Station.class).getBody();
	}

	@Test
	public void statisticJson() throws IOException {
		final ResponseEntity<String> response = loadRaw("/de/stats.json", 200, String.class);
		final JsonNode jsonNode = MAPPER.readTree(response.getBody());
		assertThat(jsonNode, notNullValue());
		assertThat(jsonNode.isObject(), is(true));
		assertThat(jsonNode.size(), is(4));
	}

	@Test
	public void statisticTxt() throws IOException {
		final ResponseEntity<InputStream> response = loadRaw("/de/stats.txt", 200, InputStream.class);
		try (final BufferedReader br = new BufferedReader(new InputStreamReader(response.getBody(), StandardCharsets.UTF_8))) {
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
		final ResponseEntity<String> response = restTemplate.postForEntity(
				String.format("http://localhost:%d%s", port, "/registration"), "{\n" +
						"\t\"nickname\": \"nickname \", \n" +
						"\t\"email\": \"nick.name@example.com\", \n" +
						"\t\"license\": \"CC0\",\n" +
						"\t\"photoOwner\": true, \n" +
						"\t\"linking\": \"linking\", \n" +
						"\t\"link\": \"\"\n" +
						"}", String.class);

		assertThat(response.getStatusCodeValue(), is(202));

		Mockito.verify(mailer, Mockito.times(1))
				.send("nick.name@example.com",
						"Railway-Stations.org new password",
						"Hello,\n\n" +
				"your new password is: .*\n\n" +
				"Cheers\n" +
				"Your Railway-Stations-Team\n" +
				"\n---\n" +
				"Hallo,\n\n" +
				"Dein neues Passwort lautet: .*\n\n" +
				"Viele Grüße\n" +
				"Dein Bahnhofsfoto-Team");
	}

	@Test
	public void registerDifferentEmail() {
		final Response response = client.target(
				String.format("http://localhost:%d%s", port, "/registration"))
				.request()
				.post(Entity.entity("{\n" +
						"\t\"nickname\": \"storchp\", \n" +
						"\t\"email\": \"other@example.com\", \n" +
						"\t\"license\": \"CC0\",\n" +
						"\t\"photoOwner\": true, \n" +
						"\t\"linking\": \"linking\", \n" +
						"\t\"link\": \"link\"\n" +
						"}", "application/json"));

		assertThat(response.getStatus(), is(409));
	}

	@Test
	public void photoUploadForbidden() {
		final Response response = client.target(
				String.format("http://localhost:%d%s", port, "/photoUpload"))
				.request()
				.header("Upload-Token", "edbfc44727a6fd4f5b029aff21861a667a6b4195")
				.header("Nickname", "nickname")
				.header("Email", "nickname@example.com")
				.header("Station-Id", "4711")
				.header("Country", "de")
				.post(Entity.entity("", "image/png"));

		assertThat(response.getStatus(), is(401));
	}

	private final byte[] IMAGE = Base64.getDecoder().decode("/9j/4AAQSkZJRgABAQEASABIAAD/2wBDAP//////////////////////////////////////////////////////////////////////////////////////wgALCAABAAEBAREA/8QAFBABAAAAAAAAAAAAAAAAAAAAAP/aAAgBAQABPxA=");

	@Test
	public void photoUploadUnknownStation() throws IOException {
		final Response response = client.target(
				String.format("http://localhost:%d%s", port, "/photoUpload"))
				.request()
				.header("Authorization", getBasicAuthentication("@khgdrn", "154a0dc31376d7620249fe089fb3ad417363f2f8"))
				.header("Station-Title", URLEncoder.encode("Achères-Grand-Cormier", StandardCharsets.UTF_8.toString()))
				.header("Latitude", "50.123")
				.header("Longitude", "10.123")
				.header("Comment", "Missing Station")
				.post(Entity.entity(IMAGE, ImageUtil.IMAGE_JPEG_MIME_TYPE));

		assertThat(response.getStatus(), is(202));
		final JsonNode inboxResponse = MAPPER.readTree((InputStream) response.getEntity());
		assertThat(inboxResponse.get("id"), notNullValue());
		assertThat(inboxResponse.get("filename"), notNullValue());
		assertThat(inboxResponse.get("crc32").asLong(), is(312729961L));

		// download uploaded photo from inbox
		final Response photoResponse = client.target(
				String.format("http://localhost:%d%s%s", port, "/inbox/", inboxResponse.get("filename").asText()))
				.request().get();
		final BufferedImage inputImage = ImageIO.read((InputStream)photoResponse.getEntity());
		assertThat(inputImage, notNullValue());
		// we cannot binary compare the result anymore, the photos are re-encoded
		// assertThat(IOUtils.readFully((InputStream)photoResponse.getEntity(), IMAGE.length), is(IMAGE));
	}

	@Test
	public void getProfileForbidden() {
		final Response response = client.target(
				String.format("http://localhost:%d%s", port, "/myProfile"))
				.request()
				.header("Nickname", "nickname")
				.header("Email", "nickname@example.com")
				.get();

		assertThat(response.getStatus(), is(401));
	}

	@Test
	public void getMyProfileWithEmail() throws IOException {
		final Response response = client.target(
				String.format("http://localhost:%d%s", port, "/myProfile"))
				.request()
				.header("Upload-Token", "154a0dc31376d7620249fe089fb3ad417363f2f8")
				.header("Email", "khgdrn@example.com")
				.get();

		assertThat(response.getStatus(), is(200));
		assertProfile(response, "@khgdrn", "https://www.twitter.com/khgdrn", "CC0 1.0 Universell (CC0 1.0)", false, "khgdrn@example.com");
	}

	@Test
	public void getMyProfileWithName() throws IOException {
		final Response response = client.target(
				String.format("http://localhost:%d%s", port, "/myProfile"))
				.request()
				.header("Upload-Token", "154a0dc31376d7620249fe089fb3ad417363f2f8")
				.header("Email", "@khgdrn")
				.get();

		assertThat(response.getStatus(), is(200));
		assertProfile(response, "@khgdrn", "https://www.twitter.com/khgdrn", "CC0 1.0 Universell (CC0 1.0)", false, "khgdrn@example.com");
	}

	@Test
	public void getMyProfileWithBasicAuthUploadToken() throws IOException {
		final Response response = client.target(
				String.format("http://localhost:%d%s", port, "/myProfile"))
				.request()
				.header("Authorization", getBasicAuthentication("@khgdrn", "154a0dc31376d7620249fe089fb3ad417363f2f8"))
				.get();

		assertThat(response.getStatus(), is(200));
		assertProfile(response, "@khgdrn", "https://www.twitter.com/khgdrn", "CC0 1.0 Universell (CC0 1.0)", false, "khgdrn@example.com");
	}

	@Test
	public void getMyProfileWithBasicAuthPassword() throws IOException {
		final Response response = client.target(
				String.format("http://localhost:%d%s", port, "/myProfile"))
				.request()
				.header("Authorization", getBasicAuthentication("@stefanopitz", "y89zFqkL6hro"))
				.get();

		assertThat(response.getStatus(), is(200));
		assertProfile(response, "@stefanopitz", "https://twitter.com/stefanopitz", "CC0 1.0 Universell (CC0 1.0)", false, "");
	}

	@Test
	public void getMyProfileWithBasicAuthPasswordFail() {
		final Response response = client.target(
				String.format("http://localhost:%d%s", port, "/myProfile"))
				.request()
				.header("Authorization", getBasicAuthentication("@stefanopitz", "blahblubb"))
				.get();

		assertThat(response.getStatus(), is(401));
	}

	@Test
	public void getInboxWithBasicAuthPasswordFail() {
		final Response response = client.target(
				String.format("http://localhost:%d%s", port, "/adminInbox"))
				.request()
				.header("Authorization", getBasicAuthentication("@stefanopitz", "blahblubb"))
				.get();

		assertThat(response.getStatus(), is(401));
	}

	@Test
	public void getInboxWithBasicAuthNotAuthorized() {
		final Response response = client.target(
				String.format("http://localhost:%d%s", port, "/adminInbox"))
				.request()
				.header("Authorization", getBasicAuthentication("@stefanopitz", "y89zFqkL6hro"))
				.get();

		assertThat(response.getStatus(), is(403));
	}

	@Test
	public void getInboxWithBasicAuth() {
		final Response response = client.target(
				String.format("http://localhost:%d%s", port, "/adminInbox"))
				.request()
				.header("Authorization", getBasicAuthentication("@khgdrn", "154a0dc31376d7620249fe089fb3ad417363f2f8"))
				.get();

		assertThat(response.getStatus(), is(200));
		// TODO: assert response body
	}

	private String getBasicAuthentication(final String user, final String password) {
		final String token = user + ":" + password;
		return "BASIC " + DatatypeConverter.printBase64Binary(token.getBytes(StandardCharsets.UTF_8));
	}

	private void assertProfile(final Response response, final String name, final String link, final String license, final boolean anonymous, final String email) throws IOException {
		final JsonNode jsonNode = MAPPER.readTree((InputStream) response.getEntity());
		assertThat(jsonNode.get("nickname").asText(), is(name));
		assertThat(jsonNode.get("email").asText(), is(email));
		assertThat(jsonNode.get("link").asText(), is(link));
		assertThat(jsonNode.get("license").asText(), is(license));
		assertThat(jsonNode.get("photoOwner").asBoolean(), is(true));
		assertThat(jsonNode.get("anonymous").asBoolean(), is(anonymous));
		assertThat(jsonNode.has("uploadToken"), is(false));
	}

	@Test
	public void updateMyProfileAndChangePassword() throws IOException {
		final Response responseGetBefore = client.target(
				String.format("http://localhost:%d%s", port, "/myProfile"))
				.request()
				.header("Upload-Token", "0ae7d6de822259da274581d9932052222b874016")
				.header("Email", "storchp@example.com")
				.get();

		assertThat(responseGetBefore.getStatus(), is(200));
		assertThat(responseGetBefore.getEntity(), notNullValue());
		assertProfile(responseGetBefore, "@storchp", "https://www.twitter.com/storchp", "CC0 1.0 Universell (CC0 1.0)", false, "storchp@example.com");

		final Response responsePostUpdate = client.target(
				String.format("http://localhost:%d%s", port, "/myProfile"))
				.request()
				.header("Upload-Token", "0ae7d6de822259da274581d9932052222b874016")
				.header("Email", "storchp@example.com")
				.post(Entity.entity("{\n" +
						"\t\"nickname\": \"storchp\", \n" +
						"\t\"email\": \"storchp@example.com\", \n" +
						"\t\"license\": \"CC0\",\n" +
						"\t\"photoOwner\": true, \n" +
						"\t\"link\": null,\n" +
						"\t\"anonymous\": true\n" +
						"}", "application/json"));

		assertThat(responsePostUpdate.getStatus(), is(200));
		assertThat(responsePostUpdate.getEntity(), notNullValue());

		final Response responseGetAfter = client.target(
				String.format("http://localhost:%d%s", port, "/myProfile"))
				.request()
				.header("Upload-Token", "0ae7d6de822259da274581d9932052222b874016")
				.header("Email", "storchp@example.com")
				.get();

		assertThat(responseGetAfter.getStatus(), is(200));
		assertThat(responseGetAfter.getEntity(), notNullValue());
		assertProfile(responseGetAfter, "storchp", "", "CC0 1.0 Universell (CC0 1.0)", true, "storchp@example.com");

		final Response responseChangePassword = client.target(
				String.format("http://localhost:%d%s", port, "/changePassword"))
				.request()
				.header("Upload-Token", "0ae7d6de822259da274581d9932052222b874016")
				.header("Email", "storchp@example.com")
				.header("New-Password", URLEncoder.encode("\uD83D\uDE0E-1234567890", StandardCharsets.UTF_8.toString()))
				.post(null);
		assertThat(responseChangePassword.getStatus(), is(200));

		final Response responseAfterChangedPassword = client.target(
				String.format("http://localhost:%d%s", port, "/myProfile"))
				.request()
				.header("Authorization", getBasicAuthentication("storchp@example.com", "\uD83D\uDE0E-1234567890"))
				.get();

		assertThat(responseAfterChangedPassword.getStatus(), is(200));

		final Response responseWithOldPassword = client.target(
				String.format("http://localhost:%d%s", port, "/myProfile"))
				.request()
				.header("Upload-Token", "0ae7d6de822259da274581d9932052222b874016")
				.header("Email", "storchp@example.com")
				.get();

		assertThat(responseWithOldPassword.getStatus(), is(401));
	}

	@Test
	public void countries() throws IOException {
		final Response response = loadRaw("/countries", 200, String.class);
		final JsonNode jsonNode = MAPPER.readTree((InputStream) response.getEntity());
		assertThat(jsonNode, notNullValue());
		assertThat(jsonNode.isArray(), is(true));
		assertThat(jsonNode.size(), is(2));

		final AtomicInteger foundCountries = new AtomicInteger();
		jsonNode.forEach(node->{
			final String country = node.get("code").asText();
			switch (country) {
				case "de" :
					assertThat(node.get("code").asText(), is("de"));
					assertThat(node.get("name").asText(), is("Deutschland"));
					assertThat(node.get("providerApps").size(), is(3));
					assertProviderApp(node, 0, "android", "DB Navigator", "https://play.google.com/store/apps/details?id=de.hafas.android.db");
					assertProviderApp(node, 1, "android", "FlixTrain", "https://play.google.com/store/apps/details?id=de.meinfernbus");
					assertProviderApp(node, 2, "ios", "DB Navigator", "https://apps.apple.com/app/db-navigator/id343555245");
					foundCountries.getAndIncrement();
					break;
				case "ch" :
					assertThat(node.get("name").asText(), is("Schweiz"));
					assertThat(node.get("providerApps").size(), is(2));
					assertProviderApp(node, 0, "android", "SBB Mobile", "https://play.google.com/store/apps/details?id=ch.sbb.mobile.android.b2c");
					assertProviderApp(node, 1, "ios", "SBB Mobile", "https://apps.apple.com/app/sbb-mobile/id294855237");
					foundCountries.getAndIncrement();
					break;
			}
		});

		assertThat(foundCountries.get(), is(2));
	}

	@Test
	public void countriesAll() throws IOException {
		final Response response = loadRaw("/countries?onlyActive=false", 200, String.class);
		final JsonNode jsonNode = MAPPER.readTree((InputStream) response.getEntity());
		assertThat(jsonNode, notNullValue());
		assertThat(jsonNode.isArray(), is(true));
		assertThat(jsonNode.size(), is(4));
	}

	private void assertProviderApp(final JsonNode de, final int i, final String type, final String name, final String url) {
		final JsonNode app = de.get("providerApps").get(i);
		assertThat(app.get("type").asText(), is(type));
		assertThat(app.get("name").asText(), is(name));
		assertThat(app.get("url").asText(), is(url));
	}

	public static final class MySuite {
		private static final String TMP_FILE = createTempFile();
		private static final String TMP_WORK_DIR = createTempDir("workDir");
		private static final String CONFIG_PATH = ResourceHelpers.resourceFilePath("test-config.yml");

		public static final DropwizardAppExtension<RsApiConfiguration> DROPWIZARD
				= new DropwizardAppExtension<>(RsApiApp.class, CONFIG_PATH,
				ConfigOverride.config("database.url", "jdbc:h2:" + TMP_FILE),
				ConfigOverride.config("workDir", TMP_WORK_DIR));

		static {
			DROPWIZARD.addListener(new DropwizardAppExtension.ServiceListener<>() {
				@Override
				public void onRun(final RsApiConfiguration config, final Environment environment, final DropwizardAppExtension<RsApiConfiguration> rule) throws Exception {
					rule.getApplication().run("db", "migrate", "-i", "junit", CONFIG_PATH);
				}
			});
		}

		private static String createTempFile() {
			try {
				return File.createTempFile("rsapi-test", null).getAbsolutePath();
			} catch (final IOException e) {
				throw new IllegalStateException(e);
			}
		}

		private static String createTempDir(final String name) {
			try {
				return Files.createTempDirectory(name + "-" + System.currentTimeMillis()).toFile().getAbsolutePath();
			} catch (final IOException e) {
				throw new IllegalStateException(e);
			}
		}

	}

}
