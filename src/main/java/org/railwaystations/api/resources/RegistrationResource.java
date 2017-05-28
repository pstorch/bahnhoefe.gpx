package org.railwaystations.api.resources;

import org.railwaystations.api.TokenGenerator;
import org.railwaystations.api.mail.Mailer;
import org.railwaystations.api.model.Registration;
import org.railwaystations.api.monitoring.Monitor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.UnsupportedEncodingException;

@Path("/registration")
public class RegistrationResource {

    private static final Logger LOG = LoggerFactory.getLogger(RegistrationResource.class);

    private final String apiKey;
    private final TokenGenerator tokenGenerator;
    private final Monitor monitor;
    private final Mailer mailer;

    public RegistrationResource(final String apiKey, final TokenGenerator tokenGenerator, final Monitor monitor, final Mailer mailer) {
        this.apiKey = apiKey;
        this.tokenGenerator = tokenGenerator;
        this.monitor = monitor;
        this.mailer = mailer;
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response post(@HeaderParam("API-Key") final String apiKey, @NotNull @Valid final Registration registration)
            throws UnsupportedEncodingException {
        LOG.info("New " + registration);
        if (!this.apiKey.equals(apiKey)) {
            return Response.status(Response.Status.FORBIDDEN).build();
        }

        final String token = tokenGenerator.buildFor(registration.getNickname());
        LOG.info("Token " + token);

        final String text = String.format("Hallo %1s,%n%n" +
                        "vielen Dank für Deine Registrierung.%n" +
                        "Dein Upload Token lautet: %2s%n" +
                        "Klicke bitte auf http://railway-stations.org/uploadToken/%3s um ihn in die App zu übernehmen.%n" +
                        "Alternativ kannst Du ihn auch manuell in der Bahnhofsfoto App unter Meine Daten eintragen.%n%n" +
                        "Viele Grüße%n" +
                        "Dein Bahnhofsfoto-Team", registration.getNickname(), token, token);
        mailer.send(registration.getEmail(), "Bahnhofsfotos upload token", text);

        monitor.sendMessage(String.format("New %s", registration));

        return Response.accepted().build();
    }


}
