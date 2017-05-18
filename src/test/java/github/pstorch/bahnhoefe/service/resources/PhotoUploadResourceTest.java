package github.pstorch.bahnhoefe.service.resources;

import github.pstorch.bahnhoefe.service.monitoring.MockMonitor;
import org.apache.commons.io.IOUtils;
import org.junit.Test;

import javax.ws.rs.core.Response;
import java.io.*;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

public class PhotoUploadResourceTest {

    private final Set<String> countries = new HashSet<>();

    @Test
    public void testPost() throws IOException {
        final Path tempDir = Files.createTempDirectory("rsapi");
        final MockMonitor monitor = new MockMonitor();
        countries.add("de");
        final PhotoUploadResource photoUpload = new PhotoUploadResource("apiKey", tempDir.toString(), countries, monitor);
        final byte[] inputBytes = "image-content".getBytes(Charset.defaultCharset());
        final InputStream is = new ByteArrayInputStream(inputBytes);

        final Response response = photoUpload.post(is, "apiKey", "userName", "4711", "de", "image/jpeg");

        assertThat(response.getStatus(), equalTo(202));

        final File image = new File(tempDir.toFile(), "de/userName-4711.jpeg");
        assertThat(image.exists(), equalTo(true));

        final byte[] outputBytes = new byte[inputBytes.length];
        IOUtils.readFully(new FileInputStream(image), outputBytes);
        assertThat(outputBytes, equalTo(inputBytes));

        assertThat(monitor.getMessages().get(0), equalTo("New photo upload: de/userName-4711.jpeg"));
    }

    @Test
    public void testPostInvalidAPIKey() throws IOException {
        final Path tempDir = Files.createTempDirectory("rsapi");
        final PhotoUploadResource photoUpload = new PhotoUploadResource("wrongApiKey", tempDir.toString(), countries, null);
        final Response response = photoUpload.post(null, "apiKey", "userName", "4711", "de", "image/jpeg");
        assertThat(response.getStatus(), equalTo(403));
    }

    @Test
    public void testPostInvalidUserName() throws IOException {
        final Path tempDir = Files.createTempDirectory("rsapi");
        final PhotoUploadResource photoUpload = new PhotoUploadResource("apiKey", tempDir.toString(), countries, null);
        final Response response = photoUpload.post(null, "apiKey", "../../someName", "4711", "de", "image/jpeg");
        assertThat(response.getStatus(), equalTo(400));
    }

    @Test
    public void testPostInvalidCountry() throws IOException {
        final Path tempDir = Files.createTempDirectory("rsapi");
        countries.add("de");
        final PhotoUploadResource photoUpload = new PhotoUploadResource("apiKey", tempDir.toString(), countries, null);
        final Response response = photoUpload.post(null, "apiKey", "userName", "4711", "xy", "image/jpeg");
        assertThat(response.getStatus(), equalTo(400));
    }

}
