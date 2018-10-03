package org.railwaystations.api.resources;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.railwaystations.api.StationsRepository;
import org.railwaystations.api.TokenGenerator;
import org.railwaystations.api.model.Coordinates;
import org.railwaystations.api.model.Photo;
import org.railwaystations.api.model.Station;
import org.railwaystations.api.monitoring.MockMonitor;

import javax.ws.rs.core.Response;
import java.io.*;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

@SuppressFBWarnings("UWF_FIELD_NOT_INITIALIZED_IN_CONSTRUCTOR")
public class PhotoUploadResourceTest {

    private final TokenGenerator tokenGenerator = new TokenGenerator("dummy");
    private final MockMonitor monitor = new MockMonitor();

    private Path tempDir;
    private PhotoUploadResource resource;

    @BeforeEach
    public void setUp() throws IOException {
        final Map<Station.Key, Station> stationsMap = new HashMap<>(2);
        final Station.Key key4711 = new Station.Key("de", "4711");
        stationsMap.put(key4711, new Station(key4711, "Lummerland", new Coordinates(50.0, 9.0), "XYZ", null));
        final Station.Key key1234 = new Station.Key("de", "1234");
        stationsMap.put(key1234, new Station(key1234, "Neverland", new Coordinates(51.0, 10.0), "ABC", new Photo(key1234, "URL", "Jim Knopf", "photographerUrl", null, "CC0")));

        tempDir = Files.createTempDirectory("rsapi");
        final StationsRepository repository = Mockito.mock(StationsRepository.class);
        Mockito.when(repository.get("de")).thenReturn(stationsMap);

        resource = new PhotoUploadResource(repository, "apiKey", tokenGenerator, tempDir.toString(), monitor);
    }

    private Response whenPostImage(final String content, final String uploadToken, final String nickname, final String email, final String stationId, final String country) throws UnsupportedEncodingException {
        final byte[] inputBytes = content.getBytes(Charset.defaultCharset());
        final InputStream is = new ByteArrayInputStream(inputBytes);
        return resource.post(is, "apiKey", uploadToken, nickname, email,stationId, country, "image/jpeg");
    }

    @Test
    public void testPost() throws IOException {
        final Response response = whenPostImage("image-content", "d913467f14e7f1c2bbbc74c07d5ab0689f4b4759", "@nick name", "nickname@example.com","4711", "de");

        assertThat(response.getStatus(), equalTo(202));
        assertFileWithContentExistsInInbox("image-content", "de/@nick name-4711.jpg");
        assertThat(monitor.getMessages().get(0), equalTo("New photo upload for Lummerland: http://inbox.railway-stations.org/de/@nick%20name-4711.jpg"));
    }

    @Test
    public void testPostDuplicateInbox() throws IOException {
        givenFileExistsInInbox("de/@other_nick-4711.jpg");

        final Response response = whenPostImage("image-content", "d913467f14e7f1c2bbbc74c07d5ab0689f4b4759", "@nick name", "nickname@example.com","4711", "de");

        assertThat(response.getStatus(), equalTo(409));
        assertFileWithContentExistsInInbox("image-content", "de/@nick name-4711.jpg");
        assertThat(monitor.getMessages().get(0), equalTo("New photo upload for Lummerland: http://inbox.railway-stations.org/de/@nick%20name-4711.jpg (possible duplicate!)"));
    }

    @Test
    public void testPostDuplicateInboxSameUser() throws IOException {
        givenFileExistsInInbox("de/@nick name-4711.jpg");

        final Response response = whenPostImage("image-content", "d913467f14e7f1c2bbbc74c07d5ab0689f4b4759", "@nick name", "nickname@example.com","4711", "de");

        assertThat(response.getStatus(), equalTo(202));
        assertFileWithContentExistsInInbox("image-content", "de/@nick name-4711.jpg");
        assertThat(monitor.getMessages().get(0), equalTo("New photo upload for Lummerland: http://inbox.railway-stations.org/de/@nick%20name-4711.jpg"));
    }

    private void assertFileWithContentExistsInInbox(final String content, final String filename) throws IOException {
        final File image = new File(tempDir.toFile(), filename);
        assertThat(image.exists(), equalTo(true));

        final byte[] inputBytes = content.getBytes(Charset.defaultCharset());
        final byte[] outputBytes = new byte[inputBytes.length];
        IOUtils.readFully(new FileInputStream(image), outputBytes);
        assertThat(outputBytes, equalTo(inputBytes));
    }

    private void givenFileExistsInInbox(final String filename) throws IOException {
        final File existing = new File(tempDir.toFile(), filename);
        FileUtils.write(existing, "dummy", "UTF-8");
    }

    @Test
    public void testPostDuplicate() throws IOException {
        final Response response = whenPostImage("image-content", "d913467f14e7f1c2bbbc74c07d5ab0689f4b4759", "@nick name", "nickname@example.com","1234", "de");

        assertThat(response.getStatus(), equalTo(409));
        assertFileWithContentExistsInInbox("image-content", "de/@nick name-1234.jpg");
        assertThat(monitor.getMessages().get(0), equalTo("New photo upload for Neverland: http://inbox.railway-stations.org/de/@nick%20name-1234.jpg (possible duplicate!)"));
    }

    @Test
    public void testPostInvalidAPIKey() throws IOException {
        final Response response = resource.post(null, "wrongApiKey", "d3d0f89efee21abcaaa58900ede61ab805ffba34", "nickname", "nickname@example.com", "4711", "de", "image/jpeg");
        assertThat(response.getStatus(), equalTo(403));
    }

    @Test
    public void testPostInvalidToken() throws IOException {
        final Response response = resource.post(null, "apiKey", "edbfc44727a6fd4f5b029aff21861a667a6b4195", "nickname", "nickname@example.com", "4711", "de", "image/jpeg");
        assertThat(response.getStatus(), equalTo(401));
    }

    @Test
    public void testPostInvalidUserName() throws IOException {
        final Response response = resource.post(null, "apiKey", "d3d0f89efee21abcaaa58900ede61ab805ffba34", "../../nickname", "nickname@example.com", "4711", "de", "image/jpeg");
        assertThat(response.getStatus(), equalTo(400));
    }

    @Test
    public void testPostInvalidCountry() throws IOException {
        final Response response = resource.post(null, "apiKey", "d3d0f89efee21abcaaa58900ede61ab805ffba34", "nickname", "nickname@example.com", "4711", "xy", "image/jpeg");
        assertThat(response.getStatus(), equalTo(400));
    }

}
