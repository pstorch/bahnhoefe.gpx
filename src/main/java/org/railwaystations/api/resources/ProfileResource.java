package org.railwaystations.api.resources;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.apache.ApacheHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import io.dropwizard.auth.Auth;
import org.eclipse.jetty.http.HttpStatus;
import org.railwaystations.api.TokenGenerator;
import org.railwaystations.api.auth.AuthUser;
import org.railwaystations.api.db.UserDao;
import org.railwaystations.api.mail.Mailer;
import org.railwaystations.api.model.User;
import org.railwaystations.api.monitoring.Monitor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import javax.validation.constraints.NotNull;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.Hashtable;
import java.util.Optional;

@Path("/")
public class ProfileResource {

    private static final Logger LOG = LoggerFactory.getLogger(ProfileResource.class);
    private static final JacksonFactory JACKSON_FACTORY = new JacksonFactory();

    private final TokenGenerator tokenGenerator;
    private final Monitor monitor;
    private final Mailer mailer;
    private final UserDao userDao;
    private final String googleClientId;

    public ProfileResource(final TokenGenerator tokenGenerator, final Monitor monitor, final Mailer mailer, final UserDao userDao, final String googleClientId) {
        this.tokenGenerator = tokenGenerator;
        this.monitor = monitor;
        this.mailer = mailer;
        this.userDao = userDao;
        this.googleClientId = googleClientId;
    }

    @POST
    @Path("registration")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response register(@NotNull final User registration) {
        LOG.info("New registration for '{}' with '{}'", registration.getName(), registration.getEmail());

        if (!registration.isValidForRegistration()) {
            LOG.warn("Registration for '{}' with '{}' invalid", registration.getName(), registration.getEmail());
            return Response.status(Response.Status.BAD_REQUEST).build();
        }

        return register(registration, false);
    }

    private void createNewUploadToken(@NotNull final User user) {
        user.setUploadTokenSalt(System.currentTimeMillis());
        user.setUploadToken(tokenGenerator.buildFor(user.getEmail(), user.getUploadTokenSalt()));
    }

    @POST
    @Path("registration/withGoogleIdToken")
    @Produces(MediaType.APPLICATION_JSON)
    public Response registerWithGoogleIdToken(@HeaderParam("Google-Id-Token") final String idToken) {
        LOG.info("New GoogleIdToken registration");

        final GoogleIdToken.Payload googleLogin = verifyGoogleIdToken(idToken);
        if (googleLogin == null) {
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }

        return register(new User(googleLogin.get("name").toString(), googleLogin.getEmail(), null, false, null, false), googleLogin.getEmailVerified());
    }

    private Response register(final User registration, final boolean eMailVerified) {
        final Optional<User> existingName = userDao.findByNormalizedName(registration.getNormalizedName());
        if (existingName.isPresent() && !registration.getEmail().equals(existingName.get().getEmail())) {
            monitor.sendMessage(
                    String.format("Registration for user '%s' with eMail '%s' failed, because name is already taken by different eMail '%s'",
                            registration.getName(), registration.getEmail(), existingName.get().getEmail()));
            return Response.status(Response.Status.CONFLICT).build();
        }

        createNewUploadToken(registration);
        final Optional<User> existing = userDao.findByEmail(registration.getEmail());
        saveRegistration(registration, existing.orElse(null));

        if (eMailVerified) {
            LOG.info("Email verified, returning profile");
            return Response.accepted().entity(existing.orElse(registration)).build();
        }

        sendTokenByMail(registration);
        return Response.accepted().build();
    }

    @GET
    @Path("myProfile")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getMyProfile(@Auth final AuthUser authUser) {
        final User user = authUser.getUser();
        LOG.info("Get profile for '{}'", user.getEmail());
        return Response.ok(user).build();
    }

    @POST
    @Path("myProfile")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response updateMyProfile(@NotNull final User newProfile, @Auth final AuthUser authUser) {
        final User user = authUser.getUser();
        LOG.info("Update profile for '{}'", user.getEmail());

        if (!newProfile.isValid()) {
            LOG.info("User invalid {}", newProfile);
            throw new WebApplicationException(HttpStatus.BAD_REQUEST_400);
        }

        if (!newProfile.getEmail().equals(user.getEmail())) {
            if (userDao.findByEmail(newProfile.getEmail()).isPresent()) {
                LOG.info("Email conflict '{}'", newProfile.getEmail());
                return Response.status(Response.Status.CONFLICT).build();
            }
            createNewUploadToken(newProfile);
            userDao.updateEmailAndTokenSalt(user.getId(), newProfile.getEmail(), newProfile.getUploadTokenSalt());
            monitor.sendMessage(
                    String.format("Update email for user '%s' from email '%s' to '%s'",
                            user.getName(), user.getEmail(), newProfile.getEmail()));
            sendTokenByMail(newProfile);
            return Response.accepted().build();
        }

        if (!newProfile.getNormalizedName().equals(user.getNormalizedName())
                && userDao.findByNormalizedName(newProfile.getNormalizedName()).isPresent()) {
            LOG.info("Name conflict '{}'", newProfile.getName());
            return Response.status(Response.Status.CONFLICT).build();
        }

        newProfile.setId(user.getId());
        userDao.update(newProfile);
        return Response.ok().build();
    }

    private void sendTokenByMail(@NotNull final User registration) {
        final String url = "http://railway-stations.org/uploadToken/" + registration.getUploadToken();

        final String text = String.format("Hallo %1s,%n%n" +
                        "vielen Dank für Deine Registrierung.%n" +
                        "Dein Upload Token lautet: %2s%n" +
                        "Klicke bitte auf %3s um ihn in die App zu übernehmen.%n" +
                        "Alternativ kannst Du auch mit Deinem Smartphone den angehängten QR-Code scannen oder den Code manuell in der Bahnhofsfoto App unter 'Meine Daten' eintragen.%n%n" +
                        "Viele Grüße%n" +
                        "Dein Bahnhofsfoto-Team",
                registration.getName(), registration.getUploadToken(), url);
        mailer.send(registration.getEmail(), "Bahnhofsfotos upload token", text, generateComZXing(url));
        LOG.info("UploadToken sent to {}", registration.getEmail());
    }

    private void saveRegistration(final User registration, final User existing) {
        if (existing != null) {
            existing.setUploadToken(registration.getUploadToken());
            existing.setUploadTokenSalt(registration.getUploadTokenSalt());
            userDao.updateTokenSalt(existing.getId(), existing.getUploadTokenSalt());
            monitor.sendMessage(
                    String.format("New UploadToken{nickname='%s', email='%s'}",
                            registration.getName(), registration.getEmail()));
            return;
        }

        if (!registration.isValid()) {
            LOG.info("User invalid {}", registration);
            throw new WebApplicationException(HttpStatus.BAD_REQUEST_400);
        }
        final Integer id = userDao.insert(registration);
        monitor.sendMessage(
                String.format("New Registration{nickname='%s', email='%s', license='%s', photoOwner=%s, link='%s', anonymous=%s}",
                        registration.getName(), registration.getEmail(), registration.getLicense(), registration.isOwnPhotos(), registration.getUrl(), registration.isAnonymous()));

        LOG.info("User '{}' created with id {}", registration.getName(), id);
    }

    private File generateComZXing(final String url) {
        try {
            final File file = File.createTempFile("rsapi", "qrcode.png");
            final Hashtable<EncodeHintType, ErrorCorrectionLevel> hintMap = new Hashtable<>();
            hintMap.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.L);
            final QRCodeWriter qrCodeWriter = new QRCodeWriter();
            final BitMatrix byteMatrix = qrCodeWriter.encode(url, BarcodeFormat.QR_CODE, 200, 200, hintMap);
            final int crunchifySize = byteMatrix.getWidth();
            final BufferedImage image = new BufferedImage(crunchifySize, crunchifySize, BufferedImage.TYPE_INT_RGB);
            image.createGraphics();
            final Graphics2D graphics = (Graphics2D) image.getGraphics();
            graphics.setColor(Color.WHITE);
            graphics.fillRect(0, 0, crunchifySize, crunchifySize);
            graphics.setColor(Color.BLACK);

            for (int i = 0; i < crunchifySize; i++) {
                for (int j = 0; j < crunchifySize; j++) {
                    if (byteMatrix.get(i, j)) {
                        graphics.fillRect(i, j, 1, 1);
                    }
                }
            }

            ImageIO.write(image, "png", file);
            LOG.info("qr-code generated at: " + file.getAbsolutePath());
            return file;
        } catch (final IOException | WriterException e) {
            throw new RuntimeException("Error creating QR-Code", e);
        }
    }

    private GoogleIdToken.Payload verifyGoogleIdToken(final String idTokenString) {
        final GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(new ApacheHttpTransport(), JACKSON_FACTORY)
                .setAudience(Collections.singletonList(googleClientId))
                .build();
        final GoogleIdToken idToken;
        try {
            idToken = verifier.verify(idTokenString);
        } catch (final Exception e) {
            LOG.error("Unable to verify google idToken", e);
            return null;
        }
        if (idToken != null) {
            final GoogleIdToken.Payload payload = idToken.getPayload();
            LOG.info("Google Login for {} with email {} (verified = {})",
                    payload.get("name"), payload.getEmail(), payload.getEmailVerified());
            return payload;
        } else {
            LOG.warn("Invalid ID token.");
            return null;
        }
    }

}
