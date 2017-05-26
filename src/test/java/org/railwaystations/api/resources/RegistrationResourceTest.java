package org.railwaystations.api.resources;

import org.junit.Test;
import org.railwaystations.api.UploadTokenGenerator;
import org.railwaystations.api.mail.MockMailer;
import org.railwaystations.api.model.Registration;
import org.railwaystations.api.monitoring.MockMonitor;

import javax.ws.rs.core.Response;
import java.io.IOException;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class RegistrationResourceTest {

    @Test
    public void testPost() throws IOException {
        final MockMonitor monitor = new MockMonitor();
        final MockMailer mailer = new MockMailer();
        final RegistrationResource registrationResource = new RegistrationResource("apiKey", new UploadTokenGenerator("dummy"), monitor, mailer);
        final Registration registration = new Registration("nickname", "email", "license", true, "linking", "link");

        final Response response = registrationResource.post("apiKey", registration);

        assertThat(response.getStatus(), equalTo(202));
        assertThat(monitor.getMessages().get(0), equalTo("New Registration{nickname='nickname', email='email', license='license', photoOwner=true, linking='linking', link='link'}"));
        assertThat(mailer.getText(), is("Hallo nickname,\n\n" +
                "vielen Dank für Deine Registrierung.\n" +
                "Dein Upload Token lautet: e0365631b58cee86711cf35c5d00bed37df926b6\n" +
                "Klicke bitte auf http://railway-stations.org/uploadToken/e0365631b58cee86711cf35c5d00bed37df926b6 um ihn in die App zu übernehmen.\n" +
                "Alternativ kannst Du ihn auch manuell in der Bahnhofsfoto App unter Meine Daten eintragen.\n\n" +
                "Viele Grüße\n" +
                "Dein Bahnhofsfoto-Team"));
    }

}
