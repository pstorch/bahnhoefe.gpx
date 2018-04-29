package org.railwaystations.api.resources;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.railwaystations.api.StationsRepository;
import org.railwaystations.api.TokenGenerator;
import org.railwaystations.api.loader.PhotographerLoader;
import org.railwaystations.api.loader.StationLoader;
import org.railwaystations.api.model.Coordinates;
import org.railwaystations.api.model.Country;
import org.railwaystations.api.model.Photo;
import org.railwaystations.api.model.Station;
import org.railwaystations.api.monitoring.MockMonitor;

import javax.ws.rs.core.Response;
import java.io.*;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

public class PhotoUploadResourceTest {

    private final TokenGenerator tokenGenerator = new TokenGenerator("dummy");
    private final MockMonitor monitor = new MockMonitor();

    private Path tempDir;
    private PhotoUploadResource resource;

    @BeforeEach
    public void setUp() throws IOException {
        final PhotographerLoader photographerLoader = new PhotographerLoader( new URL("file:./src/test/resources/photographers.json"));
        final StationLoader loader = Mockito.mock(StationLoader.class);
        final Map<Integer, Station> stationsMap = new HashMap<>(2);
        stationsMap.put(4711, new Station(4711, "de", "Lummerland", new Coordinates(50.0, 9.0), "XYZ", null));
        stationsMap.put(1234, new Station(1234, "de", "Neverland", new Coordinates(51.0, 10.0), "ABC", new Photo(4711, "URL", "Jim Knopf", "photographerUrl", null, "CC0", null)));
        Mockito.when(loader.loadStations(Mockito.anyMap(), Mockito.anyString())).thenReturn(stationsMap);
        Mockito.when(loader.getCountry()).thenReturn(new Country("de", null, null, null, null));

        tempDir = Files.createTempDirectory("rsapi");
        resource = new PhotoUploadResource(new StationsRepository(monitor, Collections.singletonList(loader), photographerLoader, ""), "apiKey", tokenGenerator, tempDir.toString(), monitor);
    }

    @org.junit.jupiter.api.Test
    public void testPost() throws IOException {
        final byte[] inputBytes = "image-content".getBytes(Charset.defaultCharset());
        final InputStream is = new ByteArrayInputStream(inputBytes);

        final Response response = resource.post(is, "apiKey", "d913467f14e7f1c2bbbc74c07d5ab0689f4b4759", "@nick name", "nickname@example.com","4711", "de", "image/jpeg");

        assertThat(response.getStatus(), equalTo(202));

        final File image = new File(tempDir.toFile(), "de/@nick name-4711.jpg");
        assertThat(image.exists(), equalTo(true));

        final byte[] outputBytes = new byte[inputBytes.length];
        IOUtils.readFully(new FileInputStream(image), outputBytes);
        assertThat(outputBytes, equalTo(inputBytes));

        assertThat(monitor.getMessages().get(0), equalTo("New photo upload for Lummerland: http://inbox.railway-stations.org/de/@nick%20name-4711.jpg"));
    }

    @org.junit.jupiter.api.Test
    public void testPostDuplicateInbox() throws IOException {
        final byte[] inputBytes = "image-content".getBytes(Charset.defaultCharset());
        final InputStream is = new ByteArrayInputStream(inputBytes);
        final File existing = new File(tempDir.toFile(), "de/@other_nick-4711.jpg");
        FileUtils.write(existing, "dummy", "UTF-8");

        final Response response = resource.post(is, "apiKey", "d913467f14e7f1c2bbbc74c07d5ab0689f4b4759", "@nick name", "nickname@example.com","4711", "de", "image/jpeg");

        assertThat(response.getStatus(), equalTo(409));

        final File image = new File(tempDir.toFile(), "de/@nick name-4711.jpg");
        assertThat(image.exists(), equalTo(true));

        final byte[] outputBytes = new byte[inputBytes.length];
        IOUtils.readFully(new FileInputStream(image), outputBytes);
        assertThat(outputBytes, equalTo(inputBytes));

        assertThat(monitor.getMessages().get(0), equalTo("New photo upload for Lummerland: http://inbox.railway-stations.org/de/@nick%20name-4711.jpg (possible duplicate!)"));
    }

    @org.junit.jupiter.api.Test
    public void testPostDuplicate() throws IOException {
        final byte[] inputBytes = "image-content".getBytes(Charset.defaultCharset());
        final InputStream is = new ByteArrayInputStream(inputBytes);

        final Response response = resource.post(is, "apiKey", "d913467f14e7f1c2bbbc74c07d5ab0689f4b4759", "@nick name", "nickname@example.com","1234", "de", "image/jpeg");

        assertThat(response.getStatus(), equalTo(409));

        final File image = new File(tempDir.toFile(), "de/@nick name-1234.jpg");
        assertThat(image.exists(), equalTo(true));

        final byte[] outputBytes = new byte[inputBytes.length];
        IOUtils.readFully(new FileInputStream(image), outputBytes);
        assertThat(outputBytes, equalTo(inputBytes));

        assertThat(monitor.getMessages().get(0), equalTo("New photo upload for Neverland: http://inbox.railway-stations.org/de/@nick%20name-1234.jpg (possible duplicate!)"));
    }

    @Test
    public void testPostInvalidAPIKey() throws IOException {
        final Response response = resource.post(null, "wrongApiKey", "d3d0f89efee21abcaaa58900ede61ab805ffba34", "nickname", "nickname@example.com", "4711", "de", "image/jpeg");
        assertThat(response.getStatus(), equalTo(403));
    }

    @org.junit.jupiter.api.Test
    public void testPostInvalidToken() throws IOException {
        final Response response = resource.post(null, "apiKey", "edbfc44727a6fd4f5b029aff21861a667a6b4195", "nickname", "nickname@example.com", "4711", "de", "image/jpeg");
        assertThat(response.getStatus(), equalTo(401));
    }

    @org.junit.jupiter.api.Test
    public void testPostInvalidUserName() throws IOException {
        final Response response = resource.post(null, "apiKey", "d3d0f89efee21abcaaa58900ede61ab805ffba34", "../../nickname", "nickname@example.com", "4711", "de", "image/jpeg");
        assertThat(response.getStatus(), equalTo(400));
    }

    @org.junit.jupiter.api.Test
    public void testPostInvalidCountry() throws IOException {
        final Response response = resource.post(null, "apiKey", "d3d0f89efee21abcaaa58900ede61ab805ffba34", "nickname", "nickname@example.com", "4711", "xy", "image/jpeg");
        assertThat(response.getStatus(), equalTo(400));
    }

}
