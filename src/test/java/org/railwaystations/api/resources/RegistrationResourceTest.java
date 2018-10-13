package org.railwaystations.api.resources;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.railwaystations.api.TokenGenerator;
import org.railwaystations.api.db.UserDao;
import org.railwaystations.api.mail.MockMailer;
import org.railwaystations.api.model.User;
import org.railwaystations.api.monitoring.MockMonitor;

import javax.ws.rs.core.Response;
import java.util.Optional;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;

@SuppressFBWarnings("UWF_FIELD_NOT_INITIALIZED_IN_CONSTRUCTOR")
@SuppressWarnings("PMD.TooManyStaticImports")
public class RegistrationResourceTest {

    private MockMonitor monitor;
    private MockMailer mailer;
    private RegistrationResource resource;
    private UserDao userDao;

    @BeforeEach
    public void setUp() {
        monitor = new MockMonitor();
        mailer = new MockMailer();
        userDao = mock(UserDao.class);

        resource = new RegistrationResource("apiKey", new TokenGenerator("dummy"), monitor, mailer, userDao);
    }

    @Test
    public void testInvalid() {
        final User registration = new User("nickname", "email", null, true, "link", false);
        final Response response = resource.post("apiKey", registration);

        assertThat(response.getStatus(), equalTo(400));
    }

    @Test
    public void testNewUser() {
        final User registration = new User("nickname", "nickname@example.com", "license", true, "link", false);
        final Response response = resource.post("apiKey", registration);
        verify(userDao).findByNormalizedName("nickname");
        verify(userDao).findByEmail("nickname@example.com");
        verify(userDao).insert(any(User.class));
        verify(userDao, never()).updateTokenSalt(anyInt(), any(Long.class));

        assertThat(response.getStatus(), equalTo(202));
        assertThat(monitor.getMessages().get(0), equalTo("New Registration{nickname='nickname', email='nickname@example.com', license='license', photoOwner=true, link='link', anonymous=false}"));
        assertThat(mailer.getText().matches("Hallo nickname,\n\n" +
                "vielen Dank für Deine Registrierung.\n" +
                "Dein Upload Token lautet: .*\n" +
                "Klicke bitte auf http://railway-stations.org/uploadToken/.* um ihn in die App zu übernehmen.\n" +
                "Alternativ kannst Du auch mit Deinem Smartphone den angehängten QR-Code scannen oder den Code manuell in der Bahnhofsfoto App unter 'Meine Daten' eintragen.\n\n" +
                "Viele Grüße\n" +
                "Dein Bahnhofsfoto-Team"), is(true));
        assertThat(mailer.getQrCode(), notNullValue());

        verifyNoMoreInteractions(userDao);
    }

    @Test
    public void testNewUserAnonymous() {
        final User registration = new User("nickname", "nickname@example.com", "license", true, "link", true);
        final Response response = resource.post("apiKey", registration);

        assertThat(response.getStatus(), equalTo(202));
        assertThat(monitor.getMessages().get(0), equalTo("New Registration{nickname='nickname', email='nickname@example.com', license='license', photoOwner=true, link='link', anonymous=true}"));
    }

    @Test
    public void testNewUserEmailTaken() {
        when(userDao.findByEmail("existing@example.com")).thenReturn(Optional.of(new User("other", "existing@example.com", "license", true, "link", false)));
        final User registration = new User("existing", "existing@example.com", "license", true, "link", false);
        final Response response = resource.post("apiKey", registration);

        assertThat(response.getStatus(), equalTo(409));
    }

    @Test
    public void testExistingUserOk() {
        when(userDao.findByNormalizedName("existing")).thenReturn(Optional.of(new User("existing", "existing@example.com", "license", true, "link", false)));
        final User registration = new User("existing", "existing@example.com", "license", true, "link", false);
        final Response response = resource.post("apiKey", registration);

        assertThat(response.getStatus(), equalTo(202));
        assertThat(monitor.getMessages().get(0), equalTo("New Registration{nickname='existing', email='existing@example.com', license='license', photoOwner=true, link='link', anonymous=false}"));
    }

    @Test
    public void testExistingUserWrongEmail() {
        when(userDao.findByNormalizedName("existing")).thenReturn(Optional.of(new User("existing", "other@example.com", "license", true, "link", false)));
        final User registration = new User("existing", "existing@example.com", "license", true, "link", false);
        final Response response = resource.post("apiKey", registration);

        assertThat(response.getStatus(), equalTo(409));
    }

}
