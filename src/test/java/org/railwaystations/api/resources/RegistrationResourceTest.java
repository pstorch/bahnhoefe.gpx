package org.railwaystations.api.resources;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.assertj.core.util.Files;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.railwaystations.api.TokenGenerator;
import org.railwaystations.api.mail.MockMailer;
import org.railwaystations.api.model.Registration;
import org.railwaystations.api.model.elastic.Fotograf;
import org.railwaystations.api.monitoring.MockMonitor;

import javax.ws.rs.core.Response;
import java.io.File;
import java.io.IOException;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;

@SuppressFBWarnings("UWF_FIELD_NOT_INITIALIZED_IN_CONSTRUCTOR")
public class RegistrationResourceTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private MockMonitor monitor;
    private MockMailer mailer;
    private RegistrationResource resource;
    private String workDir;

    @BeforeEach
    public void setUp() {
        monitor = new MockMonitor();
        mailer = new MockMailer();
        workDir = Files.temporaryFolderPath();
        resource = new RegistrationResource("apiKey", new TokenGenerator("dummy"), monitor, mailer, workDir);
    }

    @Test
    public void testPost() throws IOException {
        final Registration registration = new Registration("nickname", "nickname@example.com", "license", true, "linking", "link");
        final Response response = resource.post("apiKey", registration);

        assertThat(response.getStatus(), equalTo(202));
        assertThat(monitor.getMessages().get(0), equalTo("New Registration{nickname='nickname', email='nickname@example.com', license='license', photoOwner=true, linking='linking', link='link'}"));
        assertThat(mailer.getText(), is("Hallo nickname,\n\n" +
                "vielen Dank für Deine Registrierung.\n" +
                "Dein Upload Token lautet: d3d0f89efee21abcaaa58900ede61ab805ffba34\n" +
                "Klicke bitte auf http://railway-stations.org/uploadToken/d3d0f89efee21abcaaa58900ede61ab805ffba34 um ihn in die App zu übernehmen.\n" +
                "Alternativ kannst Du auch mit Deinem Smartphone den angehängten QR-Code scannen oder den Code manuell in der Bahnhofsfoto App unter 'Meine Daten' eintragen.\n\n" +
                "Viele Grüße\n" +
                "Dein Bahnhofsfoto-Team"));
        assertThat(mailer.getQrCode(), notNullValue());

        final File regFile = new File(new File(workDir, "registrations"), "nickname.json");
        assertThat(regFile.exists(), is(true));
        final Fotograf fotograf = MAPPER.readValue(regFile, Fotograf.class);
        assertThat(fotograf.getName(), is("nickname"));
        assertThat(fotograf.getUrl(), is("link"));
        assertThat(fotograf.getLicense(), is("license"));
    }

}
