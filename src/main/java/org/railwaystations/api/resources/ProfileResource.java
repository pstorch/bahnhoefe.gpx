package org.railwaystations.api.resources;

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

import javax.validation.constraints.NotNull;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.Optional;
import java.util.UUID;

@Path("/")
public class ProfileResource {

    private static final Logger LOG = LoggerFactory.getLogger(ProfileResource.class);

    private final Monitor monitor;
    private final Mailer mailer;
    private final UserDao userDao;
    private final String eMailVerificationUrl;

    public ProfileResource(final Monitor monitor, final Mailer mailer, final UserDao userDao, final String eMailVerificationUrl) {
        this.monitor = monitor;
        this.mailer = mailer;
        this.userDao = userDao;
        this.eMailVerificationUrl = eMailVerificationUrl;
    }

    @POST
    @Path("changePassword")
    @Produces(MediaType.APPLICATION_JSON)
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
    @Produces(MediaType.APPLICATION_JSON)
    public Response newUploadToken(@HeaderParam("User-Agent") final String userAgent, @NotNull @HeaderParam("Email") final String email) {
        return resetPassword(userAgent, email);
    }

    @POST
    @Path("resetPassword")
    @Produces(MediaType.APPLICATION_JSON)
    public Response resetPassword(@HeaderParam("User-Agent") final String userAgent, @NotNull @HeaderParam("NameOrEmail") final String nameOrEmail) {
        LOG.info("Password reset requested for '{}'", nameOrEmail);

        final User user = userDao.findByEmail(User.normalizeEmail(nameOrEmail))
                .orElse(userDao.findByNormalizedName(User.normalizeName(nameOrEmail)).orElse(null));

        if (user == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }

        if (StringUtils.isBlank(user.getEmail())) {
            monitor.sendMessage(
                    String.format("Password reset for '%s' failed, because Email is empty: '%s'%nvia %s",
                            nameOrEmail, user, userAgent));
            return Response.status(Response.Status.BAD_REQUEST).build();
        }

        user.setNewPassword(createNewPassword());
        encryptPassword(user);
        saveNewPassword(userAgent, user);
        sendPasswordMail(user);
        if (!user.isEmailVerified()) {
            // if the email is not yet verified, we can verify it with the next login
            userDao.updateEmailVerification(user.getId(), User.EMAIL_VERIFIED_AT_NEXT_LOGIN);
        }
        return Response.accepted().build();
    }

    private void saveNewPassword(final String userAgent, final User user) {
        userDao.updateCredentials(user.getId(), user.getKey());
        monitor.sendMessage(
                String.format("Reset Password for '%s', email='%s'%nvia %s",
                        user.getName(), user.getEmail(), userAgent));

        LOG.info("Reset Password for '{}'", user.getName());
    }

    @POST
    @Path("registration")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response register(@HeaderParam("User-Agent") final String userAgent, @NotNull final User registration) {
        LOG.info("New registration for '{}' with '{}'", registration.getName(), registration.getEmail());

        if (!registration.isValidForRegistration()) {
            LOG.warn("Registration for '{}' with '{}' invalid", registration.getName(), registration.getEmail());
            return Response.status(Response.Status.BAD_REQUEST).build();
        }

        final Optional<User> existingName = userDao.findByNormalizedName(registration.getNormalizedName());
        if (existingName.isPresent() && !registration.getEmail().equals(existingName.get().getEmail())) {
            monitor.sendMessage(
                    String.format("Registration for user '%s' with eMail '%s' failed, because name is already taken by different eMail '%s'%nvia %s",
                            registration.getName(), registration.getEmail(), existingName.get().getEmail(), userAgent));
            return Response.status(Response.Status.CONFLICT).build();
        }

        if (userDao.findByEmail(registration.getEmail()).isPresent()) {
            monitor.sendMessage(
                    String.format("Registration for user '%s' with eMail '%s' failed, because eMail is already taken%nvia %s",
                            registration.getName(), registration.getEmail(), userAgent));
            return Response.status(Response.Status.CONFLICT).build();
        }

        final boolean passwordProvided = StringUtils.isNotBlank(registration.getNewPassword());
        if (!passwordProvided) {
            registration.setNewPassword(createNewPassword());
            registration.setEmailVerification(User.EMAIL_VERIFIED_AT_NEXT_LOGIN);
        } else {
            registration.setEmailVerificationToken(UUID.randomUUID().toString());
        }

        encryptPassword(registration);
        saveRegistration(userAgent, registration);

        if (passwordProvided) {
            sendEmailVerification(registration);
        } else {
            sendPasswordMail(registration);
        }
        return Response.accepted().build();
    }

    private String createNewPassword() {
        return RandomStringUtils.randomAlphanumeric(12);
    }

    private void encryptPassword(@NotNull final User user) {
        user.setKey(PasswordUtil.hashPassword(user.getNewPassword()));
        user.setUploadTokenSalt(null);
        user.setUploadToken(null);
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
    public Response updateMyProfile(@HeaderParam("User-Agent") final String userAgent, @NotNull final User newProfile, @Auth final AuthUser authUser) {
        final User user = authUser.getUser();
        LOG.info("Update profile for '{}'", user.getEmail());

        if (!newProfile.isValid()) {
            LOG.info("User invalid {}", newProfile);
            throw new WebApplicationException(HttpStatus.BAD_REQUEST_400);
        }

        if (!newProfile.getNormalizedName().equals(user.getNormalizedName())) {
            if (userDao.findByNormalizedName(newProfile.getNormalizedName()).isPresent()) {
                LOG.info("Name conflict '{}'", newProfile.getName());
                return Response.status(Response.Status.CONFLICT).build();
            }
            monitor.sendMessage(
                    String.format("Update nickname for user '%s' to '%s'%n%s",
                            user.getName(), newProfile.getName(), userAgent));
        }

        if (!newProfile.getEmail().equals(user.getEmail())) {
            if (userDao.findByEmail(newProfile.getEmail()).isPresent()) {
                LOG.info("Email conflict '{}'", newProfile.getEmail());
                return Response.status(Response.Status.CONFLICT).build();
            }
            newProfile.setEmailVerificationToken(UUID.randomUUID().toString());
            monitor.sendMessage(
                    String.format("Update email for user '%s' from email '%s' to '%s'%n%s",
                            user.getName(), user.getEmail(), newProfile.getEmail(), userAgent));
            sendEmailVerification(newProfile);
        } else {
            // keep email verification status
            newProfile.setEmailVerification(user.getEmailVerification());
        }

        newProfile.setId(user.getId());
        userDao.update(newProfile);
        return Response.ok().build();
    }

    @POST
    @Path("resendEmailVerification")
    public Response resendEmailVerification(@HeaderParam("User-Agent") final String userAgent, @Auth final AuthUser authUser) {
        final User user = authUser.getUser();
        LOG.info("Resend EmailVerification for '{}'", user.getEmail());

        user.setEmailVerificationToken(UUID.randomUUID().toString());
        userDao.updateEmailVerification(user.getId(), user.getEmailVerification());
        sendEmailVerification(user);
        return Response.ok().build();
    }

    @GET
    @Path("emailVerification/{token}")
    public Response emailVerification(@HeaderParam("User-Agent") final String userAgent, @PathParam("token") final String token) {
        final Optional<User> userByToken = userDao.findByEmailVerification(User.EMAIL_VERIFICATION_TOKEN + token);
        if (userByToken.isPresent()) {
            final User user = userByToken.get();
            userDao.updateEmailVerification(user.getId(), User.EMAIL_VERIFIED);
            monitor.sendMessage(
                    String.format("Email verified {nickname='%s', email='%s'}%nvia %s", user.getName(), user.getEmail(), userAgent));
            return Response.ok("Email successfully verified!").build();
        }
        return Response.status(Response.Status.NOT_FOUND).build();
    }

    private void sendPasswordMail(@NotNull final User user) {
        final String text = String.format("Hello %1$s,%n%n" +
                        "your new password is: %2$s%n%n" +
                        "Cheers%n" +
                        "Your Railway-Stations-Team%n" +
                        "%n---%n" +
                        "Hallo %1$s,%n%n" +
                        "Dein neues Passwort lautet: %2$s%n%n" +
                        "Viele Grüße%n" +
                        "Dein Bahnhofsfoto-Team",
                user.getName(), user.getNewPassword());
        mailer.send(user.getEmail(), "Railway-Stations.org new password", text);
        LOG.info("Password sent to {}", user.getEmail());
    }

    private void sendEmailVerification(@NotNull final User user) {
        final String url = eMailVerificationUrl + user.getEmailVerificationToken();
        final String text = String.format("Hello %1$s,%n%n" +
                        "please click on %2$s to verify your eMail-Address.%n%n" +
                        "Cheers%n" +
                        "Your Railway-Stations-Team%n" +
                        "%n---%n" +
                        "Hallo %1$s,%n%n" +
                        "bitte klicke auf %2$s, um Deine eMail-Adresse zu verifizieren%n%n" +
                        "Viele Grüße%n" +
                        "Dein Bahnhofsfoto-Team",
                user.getName(), url);
        mailer.send(user.getEmail(), "Railway-Stations.org eMail verification", text);
        LOG.info("Email verification sent to {}", user.getEmail());
    }

    private void saveRegistration(final String userAgent, final User registration) {
        final Integer id = userDao.insert(registration);
        monitor.sendMessage(
                String.format("New registration{nickname='%s', email='%s', license='%s', photoOwner=%s, link='%s', anonymous=%s}%nvia %s",
                        registration.getName(), registration.getEmail(), registration.getLicense(), registration.isOwnPhotos(),
                        registration.getUrl(), registration.isAnonymous(), userAgent));

        LOG.info("User '{}' created with id {}", registration.getName(), id);
    }

}
