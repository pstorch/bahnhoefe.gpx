package org.railwaystations.api.resources;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.railwaystations.api.BahnhoefeRepository;
import org.railwaystations.api.TokenGenerator;
import org.railwaystations.api.loader.BahnhoefeLoader;
import org.railwaystations.api.model.Bahnhof;
import org.railwaystations.api.model.Photo;
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

    @Before
    public void setUp() throws IOException {
        final BahnhoefeLoader loader = Mockito.mock(BahnhoefeLoader.class);
        final Map<Integer, Bahnhof> stationsMap = new HashMap<>(2);
        stationsMap.put(4711, new Bahnhof(4711, "de", "Lummerland", 50.0, 9.0, "XYZ", new Photo(4711, "Jim Knopf", "URL", "CC0")));
        Mockito.when(loader.loadBahnhoefe()).thenReturn(stationsMap);
        Mockito.when(loader.getCountryCode()).thenReturn("de");

        tempDir = Files.createTempDirectory("rsapi");
        resource = new PhotoUploadResource(new BahnhoefeRepository(monitor, loader), "apiKey", tokenGenerator, tempDir.toString(), monitor);
    }

    @Test
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
