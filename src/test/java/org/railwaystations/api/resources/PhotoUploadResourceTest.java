package org.railwaystations.api.resources;

import org.railwaystations.api.TokenGenerator;
import org.railwaystations.api.monitoring.MockMonitor;
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
    private final TokenGenerator tokenGenerator = new TokenGenerator("dummy");

    @Test
    public void testPost() throws IOException {
        final Path tempDir = Files.createTempDirectory("rsapi");
        final MockMonitor monitor = new MockMonitor();
        countries.add("de");
        final PhotoUploadResource resource = new PhotoUploadResource("apiKey", tokenGenerator, tempDir.toString(), countries, monitor);
        final byte[] inputBytes = "image-content".getBytes(Charset.defaultCharset());
        final InputStream is = new ByteArrayInputStream(inputBytes);

        final Response response = resource.post(is, "apiKey", "d3d0f89efee21abcaaa58900ede61ab805ffba34", "nickname", "nickname@example.com","4711", "de", "image/jpeg");

        assertThat(response.getStatus(), equalTo(202));

        final File image = new File(tempDir.toFile(), "de/nickname-4711.jpg");
        assertThat(image.exists(), equalTo(true));

        final byte[] outputBytes = new byte[inputBytes.length];
        IOUtils.readFully(new FileInputStream(image), outputBytes);
        assertThat(outputBytes, equalTo(inputBytes));

        assertThat(monitor.getMessages().get(0), equalTo("New photo upload: de/nickname-4711.jpg"));
    }

    @Test
    public void testPostInvalidAPIKey() throws IOException {
        final Path tempDir = Files.createTempDirectory("rsapi");
        final PhotoUploadResource photoUpload = new PhotoUploadResource("wrongApiKey", tokenGenerator, tempDir.toString(), countries, null);
        final Response response = photoUpload.post(null, "apiKey", "d3d0f89efee21abcaaa58900ede61ab805ffba34", "nickname", "nickname@example.com", "4711", "de", "image/jpeg");
        assertThat(response.getStatus(), equalTo(403));
    }

    @Test
    public void testPostInvalidToken() throws IOException {
        final Path tempDir = Files.createTempDirectory("rsapi");
        final PhotoUploadResource photoUpload = new PhotoUploadResource("apiKey", tokenGenerator, tempDir.toString(), countries, null);
        final Response response = photoUpload.post(null, "apiKey", "edbfc44727a6fd4f5b029aff21861a667a6b4195", "nickname", "nickname@example.com", "4711", "de", "image/jpeg");
        assertThat(response.getStatus(), equalTo(401));
    }

    @Test
    public void testPostInvalidUserName() throws IOException {
        final Path tempDir = Files.createTempDirectory("rsapi");
        final PhotoUploadResource photoUpload = new PhotoUploadResource("apiKey", tokenGenerator, tempDir.toString(), countries, null);
        final Response response = photoUpload.post(null, "apiKey", "d3d0f89efee21abcaaa58900ede61ab805ffba34", "../../nickname", "nickname@example.com", "4711", "de", "image/jpeg");
        assertThat(response.getStatus(), equalTo(400));
    }

    @Test
    public void testPostInvalidCountry() throws IOException {
        final Path tempDir = Files.createTempDirectory("rsapi");
        countries.add("de");
        final PhotoUploadResource photoUpload = new PhotoUploadResource("apiKey", tokenGenerator, tempDir.toString(), countries, null);
        final Response response = photoUpload.post(null, "apiKey", "d3d0f89efee21abcaaa58900ede61ab805ffba34", "nickname", "nickname@example.com", "4711", "xy", "image/jpeg");
        assertThat(response.getStatus(), equalTo(400));
    }

}
