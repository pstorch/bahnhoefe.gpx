package org.railwaystations.api.resources;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import io.dropwizard.auth.Auth;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.jetty.http.HttpStatus;
import org.railwaystations.api.PasswordUtil;
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
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.Collections;
import java.util.Hashtable;
import java.util.Optional;

@Path("/")
public class ProfileResource {

    private static final Logger LOG = LoggerFactory.getLogger(ProfileResource.class);
    private static final JacksonFactory JACKSON_FACTORY = new JacksonFactory();

    private final Monitor monitor;
    private final Mailer mailer;
    private final UserDao userDao;
    private final String googleClientId;

    public ProfileResource(final Monitor monitor, final Mailer mailer, final UserDao userDao, final String googleClientId) {
        this.monitor = monitor;
        this.mailer = mailer;
        this.userDao = userDao;
        this.googleClientId = googleClientId;
    }

    @POST
    @Path("changePassword")
    public Response changePassword(@Auth final AuthUser authUser, @NotNull @HeaderParam("New-Password") final String newPassword) throws UnsupportedEncodingException {
        final String decodedPassword = URLDecoder.decode(newPassword, "UTF-8");
        final User user = authUser.getUser();
        LOG.info("Password change for '{}'", user.getEmail());
        final String trimmedPassword = StringUtils.trimToEmpty(decodedPassword);
        if (trimmedPassword.length() < 8 ) {
            LOG.warn("Password too short");
            return Response.status(Response.Status.BAD_REQUEST).build();
        }
        final String key = PasswordUtil.hashPassword(trimmedPassword);
        userDao.updateCredentials(user.getId(), key);
        return Response.ok().build();
    }

    @POST
    @Path("newUploadToken")
    public Response newUploadToken(@NotNull @HeaderParam("Email") final String email) {
        return resetPassword(email);
    }

    @POST
    @Path("resetPassword")
    public Response resetPassword(@NotNull @HeaderParam("NameOrEmail") final String nameOrEmail) {
        LOG.info("Password reset requested for '{}'", nameOrEmail);

        final User user = userDao.findByEmail(User.normalizeEmail(nameOrEmail))
                .orElse(userDao.findByNormalizedName(User.normalizeName(nameOrEmail)).orElse(null));

        if (user == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }

        if (StringUtils.isBlank(user.getEmail())) {
            monitor.sendMessage(
                    String.format("Password reset for '%s' failed, because Email is empty: '%s'",
                            nameOrEmail, user));
            return Response.status(Response.Status.BAD_REQUEST).build();
        }

        final String initialPassword = createNewPassword(user);
        saveRegistration(user, user);
        sendPasswordMail(user, initialPassword);
        return Response.accepted().build();
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

    private String createNewPassword(@NotNull final User user) {
        final String initialPassword = RandomStringUtils.randomAlphanumeric(12);
        user.setKey(PasswordUtil.hashPassword(initialPassword));
        user.setUploadTokenSalt(null);
        user.setUploadToken(null);

        return initialPassword;
    }

    @POST
    @Path("registration/withGoogleIdToken")
    @Produces(MediaType.APPLICATION_JSON)
    public Response registerWithGoogleIdToken(@HeaderParam("Google-Id-Token") final String idToken, @NotNull final User registration) {
        LOG.info("New GoogleIdToken registration");

        final GoogleIdToken.Payload googleLogin = verifyGoogleIdToken(idToken);
        if (googleLogin == null) {
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }

        return register(new User(googleLogin.get("name").toString(), googleLogin.getEmail(), registration.getLicense(),
                registration.isOwnPhotos(), registration.getUrl(), registration.isAnonymous()), googleLogin.getEmailVerified());
    }

    private Response register(final User registration, final boolean eMailVerified) {
        final Optional<User> existingName = userDao.findByNormalizedName(registration.getNormalizedName());
        if (existingName.isPresent() && !registration.getEmail().equals(existingName.get().getEmail())) {
            monitor.sendMessage(
                    String.format("Registration for user '%s' with eMail '%s' failed, because name is already taken by different eMail '%s'",
                            registration.getName(), registration.getEmail(), existingName.get().getEmail()));
            return Response.status(Response.Status.CONFLICT).build();
        }

        final String initialPassword = createNewPassword(registration);
        final Optional<User> existing = userDao.findByEmail(registration.getEmail());
        saveRegistration(registration, existing.orElse(null));

        if (eMailVerified) {
            LOG.info("Email verified, returning profile");
            return Response.accepted().entity(existing.orElse(registration)).build();
        }

        sendPasswordMail(registration, initialPassword);
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
            final String initialPassword = createNewPassword(newProfile);
            userDao.updateEmailAndKey(user.getId(), newProfile.getEmail(), newProfile.getKey());
            monitor.sendMessage(
                    String.format("Update email for user '%s' from email '%s' to '%s'",
                            user.getName(), user.getEmail(), newProfile.getEmail()));
            sendPasswordMail(newProfile, initialPassword);
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

    private void sendPasswordMail(@NotNull final User registration, final String initialPassword) {
        final String url = "http://railway-stations.org/uploadToken/" + initialPassword;

        final String text = String.format("Hello %1s,%n%n" +
                        "thank you for your registration.%n" +
                        "Your initial password (formerly Upload-Token) is: %2s%n" +
                        "Please click on %3s to transfer it into the App.%n" +
                        "Alternatively you can scan this QR-Code or log in manually.%n%n" +
                        "Cheers%n" +
                        "Your Railway-Stations-Team%n" +
                        "%n---%n" +
                        "Hallo %1s,%n%n" +
                        "vielen Dank für Deine Registrierung.%n" +
                        "Dein Initial-Passwort (ehemals Upload-Token) lautet: %2s%n" +
                        "Klicke bitte auf %3s, um es in die App zu übernehmen.%n" +
                        "Alternativ kannst Du auch mit Deinem Smartphone den QR-Code scannen oder Dich manuell einloggen.%n%n" +
                        "Viele Grüße%n" +
                        "Dein Bahnhofsfoto-Team",
                registration.getName(), initialPassword, url,
                registration.getName(), initialPassword, url);
        mailer.send(registration.getEmail(), "Railway-Stations.org initial password (Upload-Token)", text, generateComZXing(url));
        LOG.info("Password sent to {}", registration.getEmail());
    }

    private void saveRegistration(final User registration, final User existing) {
        if (existing != null) {
            userDao.updateCredentials(existing.getId(), registration.getKey());
            monitor.sendMessage(
                    String.format("New Password{nickname='%s', email='%s'}",
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
        final GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(new NetHttpTransport(), JACKSON_FACTORY)
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
