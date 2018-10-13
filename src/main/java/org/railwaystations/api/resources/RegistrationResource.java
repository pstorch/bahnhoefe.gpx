package org.railwaystations.api.resources;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import org.apache.commons.lang3.StringUtils;
import org.railwaystations.api.TokenGenerator;
import org.railwaystations.api.db.UserDao;
import org.railwaystations.api.mail.Mailer;
import org.railwaystations.api.model.User;
import org.railwaystations.api.monitoring.Monitor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Hashtable;
import java.util.Optional;

@Path("/registration")
public class RegistrationResource {

    private static final Logger LOG = LoggerFactory.getLogger(RegistrationResource.class);

    private final String apiKey;
    private final TokenGenerator tokenGenerator;
    private final Monitor monitor;
    private final Mailer mailer;
    private final UserDao userDao;

    public RegistrationResource(final String apiKey, final TokenGenerator tokenGenerator, final Monitor monitor, final Mailer mailer, final UserDao userDao) {
        this.apiKey = apiKey;
        this.tokenGenerator = tokenGenerator;
        this.monitor = monitor;
        this.mailer = mailer;
        this.userDao = userDao;
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response post(@HeaderParam("API-Key") final String apiKey, @NotNull final User registration) {
        LOG.info("New registration for '{}' with '{}'", registration.getName(), registration.getEmail());
        if (!this.apiKey.equals(apiKey)) {
            return Response.status(Response.Status.FORBIDDEN).build();
        }

        if (!isValid(registration)) {
            return Response.status(Response.Status.BAD_REQUEST).build();
        }

        boolean conflict = false;
        final Optional<User> user = userDao.findByNormalizedName(registration.getNormalizedName());
        User existing = null;
        if (user.isPresent()) {
            existing = user.get();
            if (!registration.getEmail().equals(existing.getEmail())) {
                LOG.info("Registration for user '{}' with eMail '{}' failed, because existing eMail '{}' is different", registration.getName(), registration.getEmail(), existing.getEmail());
                conflict = true;
            }
        } else if (userDao.findByEmail(registration.getEmail()).isPresent()) {
            LOG.info("Registration for user '{}' failed, because eMail '{}' is already taken", registration.getName(), registration.getEmail());
            conflict = true;
        }

        if (conflict) {
            return Response.status(Response.Status.CONFLICT).build();
        }

        sendTokenByMail(registration);
        saveRegistration(registration, existing);
        monitor.sendMessage(
                String.format("New Registration{nickname='%s', email='%s', license='%s', photoOwner=%s, link='%s', anonymous=%s}",
                        registration.getName(), registration.getEmail(), registration.getLicense(),
                        registration.isOwnPhotos(), registration.getUrl(), registration.isAnonymous()));

        return Response.accepted().build();
    }

    private boolean isValid(final User registration) {
        return !StringUtils.isBlank(registration.getName()) &&
                !StringUtils.isBlank(registration.getLicense()) &&
                !StringUtils.isBlank(registration.getEmail());
    }

    private void sendTokenByMail(@NotNull @Valid final User registration) {
        registration.setUploadTokenSalt(System.currentTimeMillis());
        final String token = tokenGenerator.buildFor(registration.getName(), registration.getEmail(), registration.getUploadTokenSalt());
        LOG.info("Token " + token);
        final String url = "http://railway-stations.org/uploadToken/" + token;

        final String text = String.format("Hallo %1s,%n%n" +
                        "vielen Dank für Deine Registrierung.%n" +
                        "Dein Upload Token lautet: %2s%n" +
                        "Klicke bitte auf %3s um ihn in die App zu übernehmen.%n" +
                        "Alternativ kannst Du auch mit Deinem Smartphone den angehängten QR-Code scannen oder den Code manuell in der Bahnhofsfoto App unter 'Meine Daten' eintragen.%n%n" +
                        "Viele Grüße%n" +
                        "Dein Bahnhofsfoto-Team",
                registration.getName(), token, url);
        mailer.send(registration.getEmail(), "Bahnhofsfotos upload token", text, generateComZXing(url));
    }

    private void saveRegistration(final User registration, final User existing) {
        if (existing == null) {
            final Integer id = userDao.insert(registration);
            LOG.info("User '{}' created with id {}", registration.getName(), id);
        } else {
            userDao.updateTokenSalt(existing.getId(), registration.getUploadTokenSalt());
        }
    }

    private File generateComZXing(final String url) {
        try {
            final File file = File.createTempFile("rsapi", "qrcode.png");
            final Hashtable<EncodeHintType, ErrorCorrectionLevel> hintMap = new Hashtable<>();
            hintMap.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.L);
            final QRCodeWriter qrCodeWriter = new QRCodeWriter();
            final BitMatrix byteMatrix = qrCodeWriter.encode(url, BarcodeFormat.QR_CODE, 200, 200, hintMap);
            final int crunchifyWidth = byteMatrix.getWidth();
            final BufferedImage image = new BufferedImage(crunchifyWidth, crunchifyWidth, BufferedImage.TYPE_INT_RGB);
            image.createGraphics();
            final Graphics2D graphics = (Graphics2D) image.getGraphics();
            graphics.setColor(Color.WHITE);
            graphics.fillRect(0, 0, crunchifyWidth, crunchifyWidth);
            graphics.setColor(Color.BLACK);

            for (int i = 0; i < crunchifyWidth; i++) {
                for (int j = 0; j < crunchifyWidth; j++) {
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

}
