package org.railwaystations.api.resources;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.railwaystations.api.StationsRepository;
import org.railwaystations.api.auth.AuthUser;
import org.railwaystations.api.db.CountryDao;
import org.railwaystations.api.db.PhotoDao;
import org.railwaystations.api.db.InboxDao;
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
public class PhotoInboxEntryResourceTest {

    private final MockMonitor monitor = new MockMonitor();

    private Path tempDir;
    private Path photoDir;
    private InboxResource resource;
    private InboxDao inboxDao = null;

    @BeforeEach
    public void setUp() throws IOException {
        final Station.Key key0815 = new Station.Key("ch", "0815");
        final Station station0815 = new Station(key0815, "Station 0815", new Coordinates(40.1, 7.0), "LAL", new Photo(key0815, "URL", new User("Jim Knopf", "photographerUrl", "CC0", 18, false), null, "CC0"), true);
        final Station.Key key4711 = new Station.Key("de", "4711");
        final Station station4711 = new Station(key4711, "Lummerland", new Coordinates(50.0, 9.0), "XYZ", null, true);
        final Station.Key key1234 = new Station.Key("de", "1234");
        final Station station1234 = new Station(key1234, "Neverland", new Coordinates(51.0, 10.0), "ABC", new Photo(key1234, "URL", new User("Jim Knopf", "photographerUrl", "CC0"), null, "CC0"), true);
        final Station.Key key5678 = new Station.Key("de", "5678");
        final Station station5678 = new Station(key5678, "Phantasia", new Coordinates(51.0, 10.0), "DEF", new Photo(key5678, "URL", new User("nickname", "photographerUrl", "CC0"), null, "CC0"), true);
        final Station.Key key9876 = new Station.Key("de", "9876");
        final Station station9876 = new Station(key9876, "Station 9876", new Coordinates(52.0, 8.0), "EFF", new Photo(key9876, "URL", new User("nickname", "photographerUrl", "CC0", 42, false), null, "CC0"), true);

        final UserDao userDao = mock(UserDao.class);
        final User userNickname = new User("nickname", "nickname@example.com", "CC0", true, null, true);
        when(userDao.findByEmail("nickname@example.com")).thenReturn(Optional.of(userNickname));
        final User userSomeuser = new User("someuser", "someuser@example.com", "CC0", true, null, true);
        userSomeuser.setUploadTokenSalt(123456L);
        when(userDao.findByEmail("someuser@example.com")).thenReturn(Optional.of(userSomeuser));
        inboxDao = mock(InboxDao.class);
        final CountryDao countryDao = mock(CountryDao.class);
        final PhotoDao photoDao = mock(PhotoDao.class);

        tempDir = Files.createTempDirectory("rsapi");
        photoDir = Files.createTempDirectory("rsapi-photos");
        final StationsRepository repository = mock(StationsRepository.class);
        when(repository.findByCountryAndId(key4711.getCountry(), key4711.getId())).thenReturn(station4711);
        when(repository.findByCountryAndId(key1234.getCountry(), key1234.getId())).thenReturn(station1234);
        when(repository.findByCountryAndId(key5678.getCountry(), key5678.getId())).thenReturn(station5678);
        when(repository.findByCountryAndId(key0815.getCountry(), key0815.getId())).thenReturn(station0815);
        when(repository.findByCountryAndId(key9876.getCountry(), key9876.getId())).thenReturn(station9876);

        resource = new InboxResource(repository, tempDir.toString(), tempDir.resolve( "toprocess").toString(),
                tempDir.resolve("processed").toString(), photoDir.toString(), monitor, null,
                inboxDao, userDao, countryDao, photoDao, "http://inbox.railway-stations.org");
    }

    private InboxResponse whenPostImage(final String content, final String nickname, final int userId, final String email, final String stationId, final String country,
                                        final String stationTitle, final Double latitude, final Double longitude, final String comment) throws UnsupportedEncodingException {
        final byte[] inputBytes = content.getBytes(Charset.defaultCharset());
        final InputStream is = new ByteArrayInputStream(inputBytes);
        final Response response = resource.post(is, "UserAgent", stationId, country, "image/jpeg",
                stationTitle, latitude, longitude, comment,
                new AuthUser(new User(nickname, null, "CC0", userId, email, true, false, null, null, false)));
        return (InboxResponse) response.getEntity();
    }

    @Test
    public void testPost() throws IOException {
        final ArgumentCaptor<InboxEntry> uploadCaptor = ArgumentCaptor.forClass(InboxEntry.class);
        when(inboxDao.insert(any())).thenReturn(1);
        final InboxResponse response = whenPostImage("image-content", "@nick name", 42, "nickname@example.com","4711", "de", null, null, null, "Some Comment");

        assertThat(response.getState(), equalTo(InboxResponse.InboxResponseState.REVIEW));
        assertThat(response.getId(), equalTo(1));
        assertThat(response.getFilename(), equalTo("1.jpg"));
        assertFileWithContentExistsInInbox("image-content", "1.jpg");
        verify(inboxDao).insert(uploadCaptor.capture());
        assertUpload(uploadCaptor.getValue(), "de","4711", null, null);

        assertThat(monitor.getMessages().get(0), equalTo("New photo upload for Lummerland - de:4711\nSome Comment\nhttp://inbox.railway-stations.org/1.jpg\nby @nick name\nvia UserAgent"));
    }

    private void assertUpload(final InboxEntry inboxEntry, final String countryCode, final String stationId, final String title, final Coordinates coordinates) {
        assertThat(inboxEntry.getCountryCode(), equalTo(countryCode));
        assertThat(inboxEntry.getStationId(), equalTo(stationId));
        assertThat(inboxEntry.getTitle(), equalTo(title));
        assertThat(inboxEntry.getPhotographerId(), equalTo(42));
        assertThat(inboxEntry.getComment(), equalTo("Some Comment"));
        assertThat(inboxEntry.getCreatedAt() / 1000, equalTo(System.currentTimeMillis() / 1000));
        if (coordinates != null) {
            assertThat(inboxEntry.getCoordinates().getLat(), equalTo(coordinates.getLat()));
            assertThat(inboxEntry.getCoordinates().getLon(), equalTo(coordinates.getLon()));
        } else {
            assertThat(inboxEntry.getCoordinates(), nullValue());
        }
        assertThat(inboxEntry.isDone(), equalTo(false));
    }

    @Test
    public void testPostMissingStation() throws IOException {
        when(inboxDao.insert(any())).thenReturn(4);
        final ArgumentCaptor<InboxEntry> uploadCaptor = ArgumentCaptor.forClass(InboxEntry.class);
        final InboxResponse response = whenPostImage("image-content", "@nick name", 42, "nickname@example.com",null, null, "Missing Station", 50.9876d, 9.1234d, "Some Comment");

        assertThat(response.getState(), equalTo(InboxResponse.InboxResponseState.REVIEW));
        assertThat(response.getId(), equalTo(4));
        assertThat(response.getFilename(), equalTo("4.jpg"));
        assertFileWithContentExistsInInbox("image-content", "4.jpg");
        verify(inboxDao).insert(uploadCaptor.capture());
        assertUpload(uploadCaptor.getValue(), null,null, "Missing Station", new Coordinates(50.9876, 9.1234));

        assertThat(monitor.getMessages().get(0), equalTo("Photo upload for missing station Missing Station at https://map.railway-stations.org/index.php?mlat=50.9876&mlon=9.1234&zoom=18&layers=M\nSome Comment\nhttp://inbox.railway-stations.org/4.jpg\nby @nick name\nvia UserAgent"));
    }

    @ParameterizedTest
    @CsvSource({"-91d, 9.1234d",
                "91d, 9.1234d",
                "50.9876d, -181d",
                "50.9876d, 181d",
    })
    public void testPostMissingStationLatLonOutOfRange(final Double latitude, final Double longitude) throws IOException {
        final InboxResponse response = whenPostImage("image-content", "@nick name", 42, "nickname@example.com",null, null, "Missing Station", latitude, longitude, null);

        assertThat(response.getState(), equalTo(InboxResponse.InboxResponseState.LAT_LON_OUT_OF_RANGE));
        assertThat(response.getId(), nullValue());
        assertThat(response.getFilename(), nullValue());
    }

    @Test
    public void testPostSomeUserWithTokenSalt() throws IOException {
        when(inboxDao.insert(any())).thenReturn(3);
        final InboxResponse response = whenPostImage("image-content", "@someuser", 11, "someuser@example.com","4711", "de", null, null, null, null);

        assertThat(response.getState(), equalTo(InboxResponse.InboxResponseState.REVIEW));
        assertThat(response.getId(), equalTo(3));
        assertThat(response.getFilename(), equalTo("3.jpg"));
        assertFileWithContentExistsInInbox("image-content", "3.jpg");
        assertThat(monitor.getMessages().get(0), equalTo("New photo upload for Lummerland - de:4711\n\nhttp://inbox.railway-stations.org/3.jpg\nby @someuser\nvia UserAgent"));
    }

    @Test
    public void testPostDuplicateInbox() throws IOException {
        when(inboxDao.insert(any())).thenReturn(2);
        when(inboxDao.countPendingInboxEntriesForStation(null, "de", "4711")).thenReturn(1);

        final InboxResponse response = whenPostImage("image-content", "@nick name", 42, "nickname@example.com","4711", "de", null, null, null, null);

        assertThat(response.getState(), equalTo(InboxResponse.InboxResponseState.CONFLICT));
        assertThat(response.getId(), equalTo(2));
        assertThat(response.getFilename(), equalTo("2.jpg"));
        assertFileWithContentExistsInInbox("image-content", "2.jpg");
        assertThat(monitor.getMessages().get(0), equalTo("New photo upload for Lummerland - de:4711\n\nhttp://inbox.railway-stations.org/2.jpg (possible duplicate!)\nby @nick name\nvia UserAgent"));
    }

    @Test
    public void testUserInbox() throws IOException {
        final User user = new User("nickname", null, "CC0", 42, "nickname@example.com", true, false, null, null, false);

        when(inboxDao.findById(1)).thenReturn(new InboxEntry(1, "de", "4711", "Station 4711", new Coordinates(50.1,9.2), user.getId(), user.getName(), "jpg", null, null, 0l, false, null, false, false, null));
        when(inboxDao.findById(2)).thenReturn(new InboxEntry(2, "de", "1234", "Station 1234", new Coordinates(50.1,9.2), user.getId(), user.getName(), "jpg", null, null, 0l, true, null, false, false, null));
        when(inboxDao.findById(3)).thenReturn(new InboxEntry(3, "de", "5678", "Station 5678", new Coordinates(50.1,9.2), user.getId(), user.getName(), "jpg", null, "rejected", 0l, true, null, false, false, null));
        when(inboxDao.findById(4)).thenReturn(new InboxEntry(4, "ch", "0815", "Station 0815", new Coordinates(50.1,9.2), user.getId(), user.getName(), "jpg", null, null, 0l, false, null, false, false, null));

        final List<InboxStateQuery> inboxStateQueries = new ArrayList<>();
        inboxStateQueries.add(new InboxStateQuery(1, "de", "4711", null, null, null));
        inboxStateQueries.add(new InboxStateQuery(2, "de", "1234", null, null, null));
        inboxStateQueries.add(new InboxStateQuery(3, "de", "5678", null, null, null));
        inboxStateQueries.add(new InboxStateQuery(4,"ch", "0815", null, null, null));

        final List<InboxStateQuery> uploadStateQueriesResult = resource.userInbox(new AuthUser(user), inboxStateQueries);

        assertThat(uploadStateQueriesResult.get(0).getState(), is(InboxStateQuery.InboxState.REVIEW));
        assertThat(uploadStateQueriesResult.get(0).getFilename(), is("1.jpg"));
        assertThat(uploadStateQueriesResult.get(1).getState(), is(InboxStateQuery.InboxState.ACCEPTED));
        assertThat(uploadStateQueriesResult.get(2).getState(), is(InboxStateQuery.InboxState.REJECTED));
        assertThat(uploadStateQueriesResult.get(3).getState(), is(InboxStateQuery.InboxState.CONFLICT));
    }

    private void assertFileWithContentExistsInInbox(final String content, final String filename) throws IOException {
        final File image = new File(tempDir.toFile(), filename);
        assertThat(image.exists(), equalTo(true));

        final byte[] inputBytes = content.getBytes(Charset.defaultCharset());
        final byte[] outputBytes = new byte[inputBytes.length];
        IOUtils.readFully(new FileInputStream(image), outputBytes);
        assertThat(outputBytes, equalTo(inputBytes));
    }

    @Test
    public void testPostDuplicate() throws IOException {
        when(inboxDao.insert(any())).thenReturn(5);
        final InboxResponse response = whenPostImage("image-content", "@nick name", 42, "nickname@example.com","1234", "de", null, null, null, null);

        assertThat(response.getState(), equalTo(InboxResponse.InboxResponseState.CONFLICT));
        assertThat(response.getId(), equalTo(5));
        assertThat(response.getFilename(), equalTo("5.jpg"));
        assertFileWithContentExistsInInbox("image-content", "5.jpg");
        assertThat(monitor.getMessages().get(0), equalTo("New photo upload for Neverland - de:1234\n\nhttp://inbox.railway-stations.org/5.jpg (possible duplicate!)\nby @nick name\nvia UserAgent"));
    }

    @Test
    public void testPostInvalidCountry() throws IOException {
        final Response response = resource.post(null, "UserAgent", "4711", "xy", "image/jpeg",
                null, null, null, null,
                new AuthUser(new User("nickname", "nickname@example.com", "CC0", true, null, false)));
        final InboxResponse inboxResponse = (InboxResponse) response.getEntity();
        assertThat(inboxResponse.getState(), equalTo(InboxResponse.InboxResponseState.NOT_ENOUGH_DATA));
        assertThat(inboxResponse.getId(), nullValue());
        assertThat(inboxResponse.getFilename(), nullValue());
    }

    @Test
    public void testPostProblemReport() throws IOException {
        when(inboxDao.insert(any())).thenReturn(6);
        final InboxResponse response = resource.reportProblem("UserAgent", new ProblemReport("de", "1234", ProblemReportType.OTHER, "something is wrong"),
                new AuthUser(new User("@nick name", null, "CC0", 42, "nickname@example.com", true, false, null, null, false)));

        assertThat(response.getState(), equalTo(InboxResponse.InboxResponseState.REVIEW));
        assertThat(response.getId(), equalTo(6));
        assertThat(response.getFilename(), nullValue());
        assertThat(monitor.getMessages().get(0), equalTo("New problem report for Neverland - de:1234\nOTHER: something is wrong\nby @nick name\nvia UserAgent"));
    }

}
