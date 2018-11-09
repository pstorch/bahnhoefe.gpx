package org.railwaystations.api.resources;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.railwaystations.api.StationsRepository;
import org.railwaystations.api.auth.AuthUser;
import org.railwaystations.api.db.UserDao;
import org.railwaystations.api.model.Coordinates;
import org.railwaystations.api.model.Photo;
import org.railwaystations.api.model.Station;
import org.railwaystations.api.model.User;
import org.railwaystations.api.monitoring.MockMonitor;

import javax.ws.rs.core.Response;
import java.io.*;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@SuppressFBWarnings("UWF_FIELD_NOT_INITIALIZED_IN_CONSTRUCTOR")
public class PhotoUploadResourceTest {

    private final MockMonitor monitor = new MockMonitor();

    private Path tempDir;
    private PhotoUploadResource resource;

    @BeforeEach
    public void setUp() throws IOException {
        final Map<Station.Key, Station> stationsMap = new HashMap<>(2);
        final Station.Key key4711 = new Station.Key("de", "4711");
        stationsMap.put(key4711, new Station(key4711, "Lummerland", new Coordinates(50.0, 9.0), "XYZ", null));
        final Station.Key key1234 = new Station.Key("de", "1234");
        stationsMap.put(key1234, new Station(key1234, "Neverland", new Coordinates(51.0, 10.0), "ABC", new Photo(key1234, "URL", new User("Jim Knopf", "photographerUrl", "CC0"), null, "CC0")));
        final UserDao userDao = mock(UserDao.class);
        final User userNickname = new User("nickname", "", "");
        when(userDao.findByEmail("nickname@example.com")).thenReturn(Optional.of(userNickname));
        final User userSomeuser = new User("someuser", "", "");
        userSomeuser.setUploadTokenSalt(123456L);
        when(userDao.findByEmail("someuser@example.com")).thenReturn(Optional.of(userSomeuser));

        tempDir = Files.createTempDirectory("rsapi");
        final StationsRepository repository = mock(StationsRepository.class);
        when(repository.getStationsByCountry("de")).thenReturn(stationsMap);

        resource = new PhotoUploadResource(repository, tempDir.toString(), monitor);
    }

    private Response whenPostImage(final String content, final String nickname, final String email, final String stationId, final String country) {
        final byte[] inputBytes = content.getBytes(Charset.defaultCharset());
        final InputStream is = new ByteArrayInputStream(inputBytes);
        return resource.post(is, stationId, country, "image/jpeg", new AuthUser(new User(nickname, email, "CC0", true, null, false)));
    }

    @Test
    public void testPost() throws IOException {
        final Response response = whenPostImage("image-content", "@nick name", "nickname@example.com","4711", "de");

        assertThat(response.getStatus(), equalTo(202));
        assertFileWithContentExistsInInbox("image-content", "de/nickname-4711.jpg");
        assertThat(monitor.getMessages().get(0), equalTo("New photo upload for Lummerland: http://inbox.railway-stations.org/de/nickname-4711.jpg"));
    }

    @Test
    public void testPostSomeUserWithTokenSalt() throws IOException {
        final Response response = whenPostImage("image-content", "@someuser", "someuser@example.com","4711", "de");

        assertThat(response.getStatus(), equalTo(202));
        assertFileWithContentExistsInInbox("image-content", "de/someuser-4711.jpg");
        assertThat(monitor.getMessages().get(0), equalTo("New photo upload for Lummerland: http://inbox.railway-stations.org/de/someuser-4711.jpg"));
    }

    @Test
    public void testPostDuplicateInbox() throws IOException {
        givenFileExistsInInbox("de/@other_nick-4711.jpg");

        final Response response = whenPostImage("image-content", "@nick name", "nickname@example.com","4711", "de");

        assertThat(response.getStatus(), equalTo(409));
        assertFileWithContentExistsInInbox("image-content", "de/nickname-4711.jpg");
        assertThat(monitor.getMessages().get(0), equalTo("New photo upload for Lummerland: http://inbox.railway-stations.org/de/nickname-4711.jpg (possible duplicate!)"));
    }

    @Test
    public void testPostDuplicateInboxSameUser() throws IOException {
        givenFileExistsInInbox("de/nickname-4711.jpg");

        final Response response = whenPostImage("image-content", "@nick name", "nickname@example.com","4711", "de");

        assertThat(response.getStatus(), equalTo(202));
        assertFileWithContentExistsInInbox("image-content", "de/nickname-4711.jpg");
        assertThat(monitor.getMessages().get(0), equalTo("New photo upload for Lummerland: http://inbox.railway-stations.org/de/nickname-4711.jpg"));
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
        final Response response = whenPostImage("image-content", "@nick name", "nickname@example.com","1234", "de");

        assertThat(response.getStatus(), equalTo(409));
        assertFileWithContentExistsInInbox("image-content", "de/nickname-1234.jpg");
        assertThat(monitor.getMessages().get(0), equalTo("New photo upload for Neverland: http://inbox.railway-stations.org/de/nickname-1234.jpg (possible duplicate!)"));
    }

    @Test
    public void testPostInvalidCountry() {
        final Response response = resource.post(null, "4711", "xy", "image/jpeg", new AuthUser(new User("nickname", "nickname@example.com", "CC0", true, null, false)));
        assertThat(response.getStatus(), equalTo(400));
    }

}
