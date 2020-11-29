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
import java.util.Optional;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@SuppressFBWarnings("UWF_FIELD_NOT_INITIALIZED_IN_CONSTRUCTOR")
@SuppressWarnings("PMD.TooManyStaticImports")
public class ProfileResourceTest {

    private static final String EMAIL_VERIFICATION_URL = "EMAIL_VERIFICATION_URL";

    private MockMonitor monitor;
    private MockMailer mailer;
    private ProfileResource resource;
    private UserDao userDao;

    @BeforeEach
    public void setUp() {
        monitor = new MockMonitor();
        mailer = new MockMailer();
        userDao = mock(UserDao.class);

        resource = new ProfileResource(monitor, mailer, userDao, EMAIL_VERIFICATION_URL, null);
    }

    @Test
    public void testRegisterInvalidData() {
        final User registration = new User("nickname", null, null, true, "https://link@example.com", false, null);
        final Response response = resource.register("UserAgent", registration);

        assertThat(response.getStatus(), equalTo(400));
    }

    @Test
    public void testRegisterNewUser() {
        final User registration = new User("nickname", "nickname@example.com", "CC0", true, "https://link@example.com", false, null);
        final Response response = resource.register("UserAgent", registration);
        verify(userDao).findByNormalizedName("nickname");
        verify(userDao).findByEmail("nickname@example.com");
        verify(userDao).insert(any(User.class));
        verify(userDao, never()).updateCredentials(anyInt(), anyString());

        assertThat(response.getStatus(), equalTo(202));
        assertThat(monitor.getMessages().get(0), equalTo("New registration{nickname='nickname', email='nickname@example.com', license='CC0 1.0 Universell (CC0 1.0)', photoOwner=true, link='https://link@example.com', anonymous=false}\nvia UserAgent"));
        assertNewPasswordEmail();

        verifyNoMoreInteractions(userDao);
    }

    @Test
    public void testRegisterNewUserWithPassword() {
        final User registration = new User("nickname", "nickname@example.com", "CC0", true, "https://link@example.com", false, "verySecretPassword");
        final Response response = resource.register("UserAgent", registration);
        verify(userDao).findByNormalizedName("nickname");
        verify(userDao).findByEmail("nickname@example.com");
        verify(userDao).insert(any(User.class));
        verify(userDao, never()).updateCredentials(anyInt(), anyString());

        assertThat(response.getStatus(), equalTo(202));
        assertThat(monitor.getMessages().get(0), equalTo("New registration{nickname='nickname', email='nickname@example.com', license='CC0 1.0 Universell (CC0 1.0)', photoOwner=true, link='https://link@example.com', anonymous=false}\nvia UserAgent"));
        assertVerificationEmail();

        verifyNoMoreInteractions(userDao);
    }

    private void assertVerificationEmail() {
        assertThat(mailer.getText().matches("Hello,\n\n" +
                "please click on EMAIL_VERIFICATION_URL.* to verify your eMail-Address.\n\n" +
                "Cheers\n" +
                "Your Railway-Stations-Team\n" +
                "\n---\n" +
                "Hallo,\n\n" +
                "bitte klicke auf EMAIL_VERIFICATION_URL.*, um Deine eMail-Adresse zu verifizieren\n\n" +
                "Viele Grüße\n" +
                "Dein Bahnhofsfoto-Team"), is(true));
    }

    private void assertNewPasswordEmail() {
        assertThat(mailer.getText().matches("Hello,\n\n" +
                "your new password is: .*\n\n" +
                "Cheers\n" +
                "Your Railway-Stations-Team\n" +
                "\n---\n" +
                "Hallo,\n\n" +
                "Dein neues Passwort lautet: .*\n\n" +
                "Viele Grüße\n" +
                "Dein Bahnhofsfoto-Team"), is(true));
    }

    @Test
    public void testRegisterNewUserAnonymous() {
        final User registration = new User("nickname", "nickname@example.com", "CC0", true, "https://link@example.com", true, null);
        final Response response = resource.register("UserAgent", registration);

        assertThat(response.getStatus(), equalTo(202));
        assertThat(monitor.getMessages().get(0), equalTo("New registration{nickname='nickname', email='nickname@example.com', license='CC0 1.0 Universell (CC0 1.0)', photoOwner=true, link='https://link@example.com', anonymous=true}\nvia UserAgent"));
    }

    @Test
    public void testRegisterNewUserNameTaken() {
        when(userDao.findByNormalizedName("existing")).thenReturn(Optional.of(new User("existing", "existing@example.com", "CC0", true, "https://link@example.com", false, null)));
        final User registration = new User("existing", "other@example.com", "CC0", true, "https://link@example.com", false, null);
        final Response response = resource.register("UserAgent", registration);

        assertThat(response.getStatus(), equalTo(409));
    }

    @Test
    public void testRegisterExistingUserEmailTaken() {
        final User user = new User("existing", "existing@example.com", "CC0", true, "https://link@example.com", false, null);
        when(userDao.findByNormalizedName(user.getName())).thenReturn(Optional.of(user));
        when(userDao.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        final Response response = resource.register("UserAgent", user);

        assertThat(response.getStatus(), equalTo(409));
        assertThat(monitor.getMessages().get(0), equalTo("Registration for user 'existing' with eMail 'existing@example.com' failed, because eMail is already taken\nvia UserAgent"));
    }

    @Test
    public void testRegisterExistingUserNameTaken() {
        when(userDao.findByNormalizedName("existing")).thenReturn(Optional.of(new User("existing", "other@example.com", "CC0", true, "https://link@example.com", false, null)));
        final User registration = new User("existing", "existing@example.com", "CC0", true, "https://link@example.com", false, null);
        final Response response = resource.register("UserAgent", registration);

        assertThat(response.getStatus(), equalTo(409));
        assertThat(monitor.getMessages().get(0), equalTo("Registration for user 'existing' with eMail 'existing@example.com' failed, because name is already taken by different eMail 'other@example.com'\nvia UserAgent"));
    }

    @Test
    public void testRegisterExistingUserEmptyName() {
        when(userDao.findByNormalizedName("existing")).thenReturn(Optional.of(new User("existing", "other@example.com", "CC0", true, "https://link@example.com", false, null)));
        final User registration = new User("", "existing@example.com", "CC0", true, "https://link@example.com", false, null);
        final Response response = resource.register("UserAgent", registration);

        assertThat(response.getStatus(), equalTo(400));
    }

    @Test
    public void testGetMyProfile() {
        final User user = new User("existing", "existing@example.com", null, true, null, false, null);
        final Response response = resource.getMyProfile(new AuthUser(user));

        assertThat(response.getStatus(), equalTo(200));
        assertThat(response.getEntity(), sameInstance(user));
    }

    @Test
    public void testChangePasswordTooShort() {
        final User user = new User("existing", "existing@example.com", null, true, null, false, null);
        final Response response = resource.changePassword(new AuthUser(user), "secret");
        verify(userDao, never()).updateCredentials(anyInt(), anyString());

        assertThat(response.getStatus(), equalTo(400));
    }

    @Test
    public void testChangePassword() {
        final User user = new User("existing", "existing@example.com", null, true, null, false, null);
        user.setId(4711);
        final ArgumentCaptor<Integer> idCaptor = ArgumentCaptor.forClass(Integer.class);
        final ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        final Response response = resource.changePassword(new AuthUser(user), "secretlong");
        verify(userDao).updateCredentials(idCaptor.capture(), keyCaptor.capture());

        assertThat(response.getStatus(), equalTo(200));
        assertThat(idCaptor.getValue(), equalTo(4711));
        assertThat(PasswordUtil.verifyPassword("secretlong", keyCaptor.getValue()), is(true));
    }

    @Test
    public void testUpdateMyProfile() {
        when(userDao.findByNormalizedName("newname")).thenReturn(Optional.empty());
        final User user = new User("existing", "existing@example.com", null, true, null, false, null);
        final User newProfile = new User("new_name", "existing@example.com", "CC0", true, "http://twitter.com/", true, null);
        final Response response = resource.updateMyProfile("UserAgent", newProfile, new AuthUser(user));

        assertThat(response.getStatus(), equalTo(200));
        verify(userDao).update(newProfile);
    }

    @Test
    public void testUpdateMyProfileConflict() {
        when(userDao.findByNormalizedName("newname")).thenReturn(Optional.of(new User("@New name", "newname@example.com", null, true, null, false, null)));
        final User user = new User("existing", "existing@example.com", null, true, null, false, null);
        final User newProfile = new User("new_name", "existing@example.com", "CC0", true, "http://twitter.com/", true, null);
        final Response response = resource.updateMyProfile("UserAgent", newProfile, new AuthUser(user));

        assertThat(response.getStatus(), equalTo(409));
        verify(userDao, never()).update(newProfile);
    }

    @Test
    public void testUpdateMyProfileNewMail() {
        when(userDao.findByEmail("newname@example.com")).thenReturn(Optional.empty());
        final User user = new User("existing", "existing@example.com", null, true, null, false, null);
        final User newProfile = new User("existing", "newname@example.com", "CC0", true, "http://twitter.com/", true, null);
        final Response response = resource.updateMyProfile("UserAgent", newProfile, new AuthUser(user));

        assertThat(response.getStatus(), equalTo(200));
        assertVerificationEmail();
        verify(userDao).update(newProfile);
    }

    @Test
    public void testNewUploadTokenViaEmail() {
        final User user = new User("existing", "existing@example.com", "CC0", true, "https://link@example.com", false, null);
        when(userDao.findByNormalizedName(user.getName())).thenReturn(Optional.of(user));
        when(userDao.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        final Response response = resource.newUploadToken("UserAgent", "existing@example.com");

        assertThat(response.getStatus(), equalTo(202));
        assertThat(monitor.getMessages().get(0), equalTo("Reset Password for 'existing', email='existing@example.com'\nvia UserAgent"));
    }

    @Test
    public void testNewUploadTokenViaName() {
        final User user = new User("existing", "existing@example.com", "CC0", true, "https://link@example.com", false, null);
        when(userDao.findByNormalizedName(user.getName())).thenReturn(Optional.of(user));
        when(userDao.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        final Response response = resource.newUploadToken("UserAgent", "existing");

        assertThat(response.getStatus(), equalTo(202));
        assertThat(monitor.getMessages().get(0), equalTo("Reset Password for 'existing', email='existing@example.com'\nvia UserAgent"));
    }

    @Test
    public void testNewUploadTokenNotFound() {
        final Response response = resource.newUploadToken("UserAgent", "doesnt-exist");

        assertThat(response.getStatus(), equalTo(404));
    }

    @Test
    public void testNewUploadTokenEmailMissing() {
        final User user = new User("existing", "", "CC0", true, "https://link@example.com", false, null);
        when(userDao.findByNormalizedName(user.getName())).thenReturn(Optional.of(user));
        when(userDao.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        final Response response = resource.newUploadToken("UserAgent", "existing");

        assertThat(response.getStatus(), equalTo(400));
    }

    @Test
    public void testVerifyEmailSuccess() {
        final String token = "verification";
        final String emailVerification = User.EMAIL_VERIFICATION_TOKEN + token;
        final User user = new User("existing","https://link@example.com", "CC0", 42, "existing@example.com", true, false, null, null, false, emailVerification);
        when(userDao.findByEmailVerification(emailVerification)).thenReturn(Optional.of(user));
        final Response response = resource.emailVerification("UserAgent", token);

        assertThat(response.getStatus(), equalTo(200));
        assertThat(monitor.getMessages().get(0), equalTo("Email verified {nickname='existing', email='existing@example.com'}\nvia UserAgent"));
        verify(userDao).updateEmailVerification(42, User.EMAIL_VERIFIED);
    }

    @Test
    public void testVerifyEmailFailed() {
        final String token = "verification";
        final String emailVerification = User.EMAIL_VERIFICATION_TOKEN + token;
        final User user = new User("existing","https://link@example.com", "CC0", 42, "existing@example.com", true, false, null, null, false, emailVerification);
        when(userDao.findByEmailVerification(emailVerification)).thenReturn(Optional.of(user));
        final Response response = resource.emailVerification("UserAgent", "wrong_token");

        assertThat(response.getStatus(), equalTo(404));
        assertThat(monitor.getMessages().isEmpty(), equalTo(true));
        verify(userDao, never()).updateEmailVerification(42, User.EMAIL_VERIFIED);
    }

    @Test
    public void testResendEmailVerification() {
        when(userDao.findByEmail("newname@example.com")).thenReturn(Optional.empty());
        final User user = new User("existing","https://link@example.com", "CC0", 42, "existing@example.com", true, false, null, null, false, User.EMAIL_VERIFIED_AT_NEXT_LOGIN);
        final Response response = resource.resendEmailVerification("UserAgent", new AuthUser(user));

        assertThat(response.getStatus(), equalTo(200));
        assertThat(user.getEmailVerification().startsWith(User.EMAIL_VERIFICATION_TOKEN), is(true));
        assertVerificationEmail();
        verify(userDao).updateEmailVerification(user.getId(), user.getEmailVerification());
    }

}
