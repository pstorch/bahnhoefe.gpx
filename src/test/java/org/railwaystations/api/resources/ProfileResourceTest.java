package org.railwaystations.api.resources;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.railwaystations.api.PasswordUtil;
import org.railwaystations.api.auth.AuthUser;
import org.railwaystations.api.db.UserDao;
import org.railwaystations.api.mail.MockMailer;
import org.railwaystations.api.model.User;
import org.railwaystations.api.monitoring.MockMonitor;

import javax.ws.rs.core.Response;
import java.io.UnsupportedEncodingException;
import java.util.Optional;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@SuppressFBWarnings("UWF_FIELD_NOT_INITIALIZED_IN_CONSTRUCTOR")
@SuppressWarnings("PMD.TooManyStaticImports")
public class ProfileResourceTest {

    private MockMonitor monitor;
    private MockMailer mailer;
    private ProfileResource resource;
    private UserDao userDao;

    @BeforeEach
    public void setUp() {
        monitor = new MockMonitor();
        mailer = new MockMailer();
        userDao = mock(UserDao.class);

        resource = new ProfileResource(monitor, mailer, userDao);
    }

    @Test
    public void testInvalid() {
        final User registration = new User("nickname", null, null, true, "https://link@example.com", false);
        final Response response = resource.register("UserAgent", registration);

        assertThat(response.getStatus(), equalTo(400));
    }

    @Test
    public void testNewUser() {
        final User registration = new User("nickname", "nickname@example.com", "CC0", true, "https://link@example.com", false);
        final Response response = resource.register("UserAgent", registration);
        verify(userDao).findByNormalizedName("nickname");
        verify(userDao).findByEmail("nickname@example.com");
        verify(userDao).insert(any(User.class));
        verify(userDao, never()).updateCredentials(anyInt(), anyString());

        assertThat(response.getStatus(), equalTo(202));
        assertThat(monitor.getMessages().get(0), equalTo("New registration{nickname='nickname', email='nickname@example.com', license='CC0 1.0 Universell (CC0 1.0)', photoOwner=true, link='https://link@example.com', anonymous=false}\nvia UserAgent"));
        assertEmail("nickname");

        verifyNoMoreInteractions(userDao);
    }

    private void assertEmail(final String nickname) {
        assertThat(mailer.getText().matches("Hello " + nickname + ",\n\n" +
                "thank you for your registration.\n" +
                "Your initial password \\(formerly Upload-Token\\) is: .*\n" +
                "Please click on http://railway-stations.org/uploadToken/.* to transfer it into the App.\n" +
                "Alternatively you can scan this QR-Code or log in manually.\n\n" +
                "Cheers\n" +
                "Your Railway-Stations-Team\n" +
                "\n---\n" +
                "Hallo " + nickname + ",\n\n" +
                "vielen Dank für Deine Registrierung.\n" +
                "Dein Initial-Passwort \\(ehemals Upload-Token\\) lautet: .*\n" +
                "Klicke bitte auf http://railway-stations.org/uploadToken/.*, um es in die App zu übernehmen.\n" +
                "Alternativ kannst Du auch mit Deinem Smartphone den QR-Code scannen oder Dich manuell einloggen.\n\n" +
                "Viele Grüße\n" +
                "Dein Bahnhofsfoto-Team"), is(true));
        assertThat(mailer.getQrCode(), notNullValue());
    }

    @Test
    public void testNewUserAnonymous() {
        final User registration = new User("nickname", "nickname@example.com", "CC0", true, "https://link@example.com", true);
        final Response response = resource.register("UserAgent", registration);

        assertThat(response.getStatus(), equalTo(202));
        assertThat(monitor.getMessages().get(0), equalTo("New registration{nickname='nickname', email='nickname@example.com', license='CC0 1.0 Universell (CC0 1.0)', photoOwner=true, link='https://link@example.com', anonymous=true}\nvia UserAgent"));
    }

    @Test
    public void testNewUserNameTaken() {
        when(userDao.findByNormalizedName("existing")).thenReturn(Optional.of(new User("existing", "existing@example.com", "CC0", true, "https://link@example.com", false)));
        final User registration = new User("existing", "other@example.com", "CC0", true, "https://link@example.com", false);
        final Response response = resource.register("UserAgent", registration);

        assertThat(response.getStatus(), equalTo(409));
    }

    @Test
    public void testExistingUserOk() {
        final User user = new User("existing", "existing@example.com", "CC0", true, "https://link@example.com", false);
        when(userDao.findByNormalizedName(user.getName())).thenReturn(Optional.of(user));
        when(userDao.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        final Response response = resource.register("UserAgent", user);

        assertThat(response.getStatus(), equalTo(202));
        assertThat(monitor.getMessages().get(0), equalTo("Re-registration: sending new password{nickname='existing', email='existing@example.com'}\nvia UserAgent"));
    }

    @Test
    public void testExistingUserWrongEmail() {
        when(userDao.findByNormalizedName("existing")).thenReturn(Optional.of(new User("existing", "other@example.com", "CC0", true, "https://link@example.com", false)));
        final User registration = new User("existing", "existing@example.com", "CC0", true, "https://link@example.com", false);
        final Response response = resource.register("UserAgent", registration);

        assertThat(response.getStatus(), equalTo(409));
    }

    @Test
    public void testExistingUserEmptyName() {
        when(userDao.findByNormalizedName("existing")).thenReturn(Optional.of(new User("existing", "other@example.com", "CC0", true, "https://link@example.com", false)));
        final User registration = new User("", "existing@example.com", "CC0", true, "https://link@example.com", false);
        final Response response = resource.register("UserAgent", registration);

        assertThat(response.getStatus(), equalTo(400));
    }

    @Test
    public void testGetMyProfile() {
        final User user = new User("existing", "existing@example.com", null, true, null, false);
        final Response response = resource.getMyProfile(new AuthUser(user));

        assertThat(response.getStatus(), equalTo(200));
        assertThat(response.getEntity(), sameInstance(user));
    }

    @Test
    public void testChangePasswordTooShort() throws UnsupportedEncodingException {
        final User user = new User("existing", "existing@example.com", null, true, null, false);
        final Response response = resource.changePassword(new AuthUser(user), "secret");
        verify(userDao, never()).updateCredentials(anyInt(), anyString());

        assertThat(response.getStatus(), equalTo(400));
    }

    @Test
    public void testChangePassword() throws UnsupportedEncodingException {
        final User user = new User("existing", "existing@example.com", null, true, null, false);
        user.setId(4711);
        ArgumentCaptor<Integer> idCaptor = ArgumentCaptor.forClass(Integer.class);
        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        final Response response = resource.changePassword(new AuthUser(user), "secretlong");
        verify(userDao).updateCredentials(idCaptor.capture(), keyCaptor.capture());

        assertThat(response.getStatus(), equalTo(200));
        assertThat(idCaptor.getValue(), equalTo(4711));
        assertThat(PasswordUtil.verifyPassword("secretlong", keyCaptor.getValue()), is(true));
    }

    @Test
    public void testUpdateMyProfile() {
        when(userDao.findByNormalizedName("newname")).thenReturn(Optional.empty());
        final User user = new User("existing", "existing@example.com", null, true, null, false);
        final User newProfile = new User("new_name", "existing@example.com", "CC0", true, "http://twitter.com/", true);
        final Response response = resource.updateMyProfile("UserAgent", newProfile, new AuthUser(user));

        assertThat(response.getStatus(), equalTo(200));
        verify(userDao).update(newProfile);
    }

    @Test
    public void testUpdateMyProfileConflict() {
        when(userDao.findByNormalizedName("newname")).thenReturn(Optional.of(new User("@New name", "newname@example.com", null, true, null, false)));
        final User user = new User("existing", "existing@example.com", null, true, null, false);
        final User newProfile = new User("new_name", "existing@example.com", "CC0", true, "http://twitter.com/", true);
        final Response response = resource.updateMyProfile("UserAgent", newProfile, new AuthUser(user));

        assertThat(response.getStatus(), equalTo(409));
        verify(userDao, never()).update(newProfile);
    }

    @Test
    public void testUpdateMyProfileNewMail() {
        when(userDao.findByEmail("newname@example.com")).thenReturn(Optional.empty());
        final User user = new User("existing", "existing@example.com", null, true, null, false);
        final User newProfile = new User("existing", "newname@example.com", "CC0", true, "http://twitter.com/", true);
        final Response response = resource.updateMyProfile("UserAgent", newProfile, new AuthUser(user));

        assertThat(response.getStatus(), equalTo(202));
        assertEmail("existing");
        verify(userDao).updateEmailAndKey(user.getId(), newProfile.getEmail(), newProfile.getKey());
        verify(userDao, never()).update(newProfile);
    }

    @Test
    public void testNewUploadTokenViaEmail() {
        final User user = new User("existing", "existing@example.com", "CC0", true, "https://link@example.com", false);
        when(userDao.findByNormalizedName(user.getName())).thenReturn(Optional.of(user));
        when(userDao.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        final Response response = resource.newUploadToken("UserAgent", "existing@example.com");

        assertThat(response.getStatus(), equalTo(202));
        assertThat(monitor.getMessages().get(0), equalTo("Re-registration: sending new password{nickname='existing', email='existing@example.com'}\nvia UserAgent"));
    }

    @Test
    public void testNewUploadTokenViaName() {
        final User user = new User("existing", "existing@example.com", "CC0", true, "https://link@example.com", false);
        when(userDao.findByNormalizedName(user.getName())).thenReturn(Optional.of(user));
        when(userDao.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        final Response response = resource.newUploadToken("UserAgent", "existing");

        assertThat(response.getStatus(), equalTo(202));
        assertThat(monitor.getMessages().get(0), equalTo("Re-registration: sending new password{nickname='existing', email='existing@example.com'}\nvia UserAgent"));
    }

    @Test
    public void testNewUploadTokenNotFound() {
        final Response response = resource.newUploadToken("UserAgent", "doesnt-exist");

        assertThat(response.getStatus(), equalTo(404));
    }

    @Test
    public void testNewUploadTokenEmailMissing() {
        final User user = new User("existing", "", "CC0", true, "https://link@example.com", false);
        when(userDao.findByNormalizedName(user.getName())).thenReturn(Optional.of(user));
        when(userDao.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        final Response response = resource.newUploadToken("UserAgent", "existing");

        assertThat(response.getStatus(), equalTo(400));
    }

}
