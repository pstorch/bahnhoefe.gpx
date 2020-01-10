package org.railwaystations.api.resources;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.railwaystations.api.StationsRepository;
import org.railwaystations.api.auth.AuthUser;
import org.railwaystations.api.db.UploadDao;
import org.railwaystations.api.db.UserDao;
import org.railwaystations.api.model.*;
import org.railwaystations.api.monitoring.MockMonitor;

import javax.ws.rs.core.Response;
import java.io.*;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.*;

@SuppressFBWarnings("UWF_FIELD_NOT_INITIALIZED_IN_CONSTRUCTOR")
@SuppressWarnings("PMD.TooManyStaticImports")
public class PhotoUploadResourceTest {

    private final MockMonitor monitor = new MockMonitor();

    private Path tempDir;
    private PhotoUploadResource resource;
    private UploadDao uploadDao = null;

    @BeforeEach
    public void setUp() throws IOException {
        final Station.Key key4711 = new Station.Key("de", "4711");
        final Station station4711 = new Station(key4711, "Lummerland", new Coordinates(50.0, 9.0), "XYZ", null, true);
        final Station.Key key1234 = new Station.Key("de", "1234");
        final Station station1234 = new Station(key1234, "Neverland", new Coordinates(51.0, 10.0), "ABC", new Photo(key1234, "URL", new User("Jim Knopf", "photographerUrl", "CC0"), null, "CC0"), true);
        final Station.Key key5678 = new Station.Key("de", "5678");
        final Station station5678 = new Station(key5678, "Phantasia", new Coordinates(51.0, 10.0), "DEF", new Photo(key5678, "URL", new User("nickname", "photographerUrl", "CC0"), null, "CC0"), true);
        final UserDao userDao = mock(UserDao.class);
        final User userNickname = new User("nickname", "nickname@example.com", "CC0", true, null, true);
        when(userDao.findByEmail("nickname@example.com")).thenReturn(Optional.of(userNickname));
        final User userSomeuser = new User("someuser", "someuser@example.com", "CC0", true, null, true);
        userSomeuser.setUploadTokenSalt(123456L);
        when(userDao.findByEmail("someuser@example.com")).thenReturn(Optional.of(userSomeuser));
        uploadDao = mock(UploadDao.class);

        tempDir = Files.createTempDirectory("rsapi");
        final StationsRepository repository = mock(StationsRepository.class);
        when(repository.findByCountryAndId(key4711.getCountry(), key4711.getId())).thenReturn(station4711);
        when(repository.findByCountryAndId(key1234.getCountry(), key1234.getId())).thenReturn(station1234);
        when(repository.findByCountryAndId(key5678.getCountry(), key5678.getId())).thenReturn(station5678);

        resource = new PhotoUploadResource(repository, tempDir.toString(), monitor, null, uploadDao);
    }

    private PhotoUploadResource.UploadResponse whenPostImage(final String content, final String nickname, final int userId, final String email, final String stationId, final String country,
                                                             final String stationTitle, final Double latitude, final Double longitude, final String comment) throws UnsupportedEncodingException {
        final byte[] inputBytes = content.getBytes(Charset.defaultCharset());
        final InputStream is = new ByteArrayInputStream(inputBytes);
        final Response response = resource.post(is, "UserAgent", stationId, country, "image/jpeg",
                stationTitle, latitude, longitude, comment,
                new AuthUser(new User(nickname, null, "CC0", userId, email, true, false, null, null, false)));
        return (PhotoUploadResource.UploadResponse) response.getEntity();
    }

    @Test
    public void testPost() throws IOException {
        ArgumentCaptor<Upload> uploadCaptor = ArgumentCaptor.forClass(Upload.class);
        when(uploadDao.insert(any())).thenReturn(1);
        final PhotoUploadResource.UploadResponse response = whenPostImage("image-content", "@nick name", 42, "nickname@example.com","4711", "de", null, null, null, "Some Comment");

        assertThat(response.getCode(), equalTo(202));
        assertThat(response.getUploadId(), equalTo(1));
        assertThat(response.getInboxUrl(), equalTo("http://inbox.railway-stations.org/1.jpg"));
        assertFileWithContentExistsInInbox("image-content", "1.jpg");
        verify(uploadDao).insert(uploadCaptor.capture());
        assertUpload(uploadCaptor.getValue(), "de","4711", null, null);

        assertThat(monitor.getMessages().get(0), equalTo("New photo upload for Lummerland\nSome Comment\nhttp://inbox.railway-stations.org/1.jpg\nvia UserAgent"));
    }

    private void assertUpload(final Upload upload, final String countryCode, final String stationId, final String title, final Coordinates coordinates) {
        assertThat(upload.getCountryCode(), equalTo(countryCode));
        assertThat(upload.getStationId(), equalTo(stationId));
        assertThat(upload.getTitle(), equalTo(title));
        assertThat(upload.getPhotographerId(), equalTo(42));
        assertThat(upload.getUploadComment(), equalTo("Some Comment"));
        assertThat(upload.getCreatedAt() / 1000, equalTo(System.currentTimeMillis() / 1000));
        if (coordinates != null) {
            assertThat(upload.getCoordinates().getLat(), equalTo(coordinates.getLat()));
            assertThat(upload.getCoordinates().getLon(), equalTo(coordinates.getLon()));
        } else {
            assertThat(upload.getCoordinates(), nullValue());
        }
        assertThat(upload.getDone(), equalTo(false));
    }

    @Test
    public void testPostMissingStation() throws IOException {
        when(uploadDao.insert(any())).thenReturn(4);
        ArgumentCaptor<Upload> uploadCaptor = ArgumentCaptor.forClass(Upload.class);
        final PhotoUploadResource.UploadResponse response = whenPostImage("image-content", "@nick name", 42, "nickname@example.com",null, null, "Missing Station", 50.9876d, 9.1234d, "Some Comment");

        assertThat(response.getCode(), equalTo(202));
        assertThat(response.getUploadId(), equalTo(4));
        assertThat(response.getInboxUrl(), equalTo("http://inbox.railway-stations.org/4.jpg"));
        assertFileWithContentExistsInInbox("image-content", "4.jpg");
        verify(uploadDao).insert(uploadCaptor.capture());
        assertUpload(uploadCaptor.getValue(), null,null, "Missing Station", new Coordinates(50.9876, 9.1234));

        assertThat(monitor.getMessages().get(0), equalTo("Photo upload for missing station Missing Station at http://www.openstreetmap.org/?mlat=50.9876&mlon=9.1234&zoom=18&layers=M\nSome Comment\nhttp://inbox.railway-stations.org/4.jpg\nvia UserAgent"));
    }

    @ParameterizedTest
    @CsvSource({"-91d, 9.1234d",
                "91d, 9.1234d",
                "50.9876d, -181d",
                "50.9876d, 181d",
    })
    public void testPostMissingStationLatLonOutOfRange(final Double latitude, final Double longitude) throws IOException {
        final PhotoUploadResource.UploadResponse response = whenPostImage("image-content", "@nick name", 42, "nickname@example.com",null, null, "Missing Station", latitude, longitude, null);

        assertThat(response.getCode(), equalTo(400));
        assertThat(response.getUploadId(), nullValue());
        assertThat(response.getInboxUrl(), nullValue());
    }

    @Test
    public void testPostSomeUserWithTokenSalt() throws IOException {
        when(uploadDao.insert(any())).thenReturn(3);
        final PhotoUploadResource.UploadResponse response = whenPostImage("image-content", "@someuser", 11, "someuser@example.com","4711", "de", null, null, null, null);

        assertThat(response.getCode(), equalTo(202));
        assertThat(response.getUploadId(), equalTo(3));
        assertThat(response.getInboxUrl(), equalTo("http://inbox.railway-stations.org/3.jpg"));
        assertFileWithContentExistsInInbox("image-content", "3.jpg");
        assertThat(monitor.getMessages().get(0), equalTo("New photo upload for Lummerland\n\nhttp://inbox.railway-stations.org/3.jpg\nvia UserAgent"));
    }

    @Test
    public void testPostDuplicateInbox() throws IOException {
        when(uploadDao.insert(any())).thenReturn(2);
        when(uploadDao.countPendingUploadsForStation("de", "4711")).thenReturn(1);

        final PhotoUploadResource.UploadResponse response = whenPostImage("image-content", "@nick name", 42, "nickname@example.com","4711", "de", null, null, null, null);

        assertThat(response.getCode(), equalTo(409));
        assertThat(response.getUploadId(), equalTo(2));
        assertThat(response.getInboxUrl(), equalTo("http://inbox.railway-stations.org/2.jpg"));
        assertFileWithContentExistsInInbox("image-content", "2.jpg");
        assertThat(monitor.getMessages().get(0), equalTo("New photo upload for Lummerland\n\nhttp://inbox.railway-stations.org/2.jpg (possible duplicate!)\nvia UserAgent"));
    }

    @Test
    public void testQueryState() throws IOException {
        givenFileExistsInInbox("de/nickname-4711.jpg");
        givenFileExistsInInbox("ch/othernick-0815.jpg");
        givenFileExistsInInbox("missing/nickname-1.jpg");
        givenFileExistsInInbox("missing/nickname-1.jpg.txt", "Missing Station\n50.9876,9.1234\nSome Comment");

        final List<PhotoUploadResource.UploadStateQuery> uploadStateQueries = new ArrayList<>();
        uploadStateQueries.add(new PhotoUploadResource.UploadStateQuery("de", "4711", null, null));
        uploadStateQueries.add(new PhotoUploadResource.UploadStateQuery("de", "1234", null, null));
        uploadStateQueries.add(new PhotoUploadResource.UploadStateQuery("de", "5678", null, null));
        uploadStateQueries.add(new PhotoUploadResource.UploadStateQuery("ch", "0815", null, null));
        uploadStateQueries.add(new PhotoUploadResource.UploadStateQuery(null, null, 50.9876, 9.1234));

        final List<PhotoUploadResource.UploadStateQuery> uploadStateQueriesResult = resource.queryState(uploadStateQueries, new AuthUser(new User("nickname", "nickname@example.com", "CC0", true, null, false)));

        assertThat(uploadStateQueriesResult.get(0).getState(), is(PhotoUploadResource.UploadStateQuery.UploadStateState.IN_REVIEW));
        assertThat(uploadStateQueriesResult.get(1).getState(), is(PhotoUploadResource.UploadStateQuery.UploadStateState.OTHER_USER));
        assertThat(uploadStateQueriesResult.get(2).getState(), is(PhotoUploadResource.UploadStateQuery.UploadStateState.ACCEPTED));
        assertThat(uploadStateQueriesResult.get(3).getState(), is(PhotoUploadResource.UploadStateQuery.UploadStateState.UNKNOWN));
        assertThat(uploadStateQueriesResult.get(4).getState(), is(PhotoUploadResource.UploadStateQuery.UploadStateState.IN_REVIEW));
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
        givenFileExistsInInbox(filename, "dummy");
    }

    private void givenFileExistsInInbox(final String filename, final String content) throws IOException {
        final File existing = new File(tempDir.toFile(), filename);
        FileUtils.write(existing, content, "UTF-8");
    }

    @Test
    public void testPostDuplicate() throws IOException {
        when(uploadDao.insert(any())).thenReturn(5);
        final PhotoUploadResource.UploadResponse response = whenPostImage("image-content", "@nick name", 42, "nickname@example.com","1234", "de", null, null, null, null);

        assertThat(response.getCode(), equalTo(409));
        assertThat(response.getUploadId(), equalTo(5));
        assertThat(response.getInboxUrl(), equalTo("http://inbox.railway-stations.org/5.jpg"));
        assertFileWithContentExistsInInbox("image-content", "5.jpg");
        assertThat(monitor.getMessages().get(0), equalTo("New photo upload for Neverland\n\nhttp://inbox.railway-stations.org/5.jpg (possible duplicate!)\nvia UserAgent"));
    }

    @Test
    public void testPostInvalidCountry() throws IOException {
        final Response response = resource.post(null, "UserAgent", "4711", "xy", "image/jpeg",
                null, null, null, null,
                new AuthUser(new User("nickname", "nickname@example.com", "CC0", true, null, false)));
        final PhotoUploadResource.UploadResponse uploadResponse = (PhotoUploadResource.UploadResponse) response.getEntity();
        assertThat(uploadResponse.getCode(), equalTo(400));
        assertThat(uploadResponse.getUploadId(), nullValue());
        assertThat(uploadResponse.getInboxUrl(), nullValue());
    }

}
