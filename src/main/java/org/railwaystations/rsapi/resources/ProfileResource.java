package org.railwaystations.rsapi.resources;

import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.railwaystations.rsapi.auth.AuthUser;
import org.railwaystations.rsapi.db.UserDao;
import org.railwaystations.rsapi.mail.Mailer;
import org.railwaystations.rsapi.model.User;
import org.railwaystations.rsapi.monitoring.Monitor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import javax.validation.constraints.NotNull;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.UUID;

@RestController
public class ProfileResource {

    private static final Logger LOG = LoggerFactory.getLogger(ProfileResource.class);

    private final Monitor monitor;
    private final Mailer mailer;
    private final UserDao userDao;
    private final String eMailVerificationUrl;
    private final PasswordEncoder passwordEncoder;

    public ProfileResource(final Monitor monitor, final Mailer mailer, final UserDao userDao, final String eMailVerificationUrl, final PasswordEncoder passwordEncoder) {
        this.monitor = monitor;
        this.mailer = mailer;
        this.userDao = userDao;
        this.eMailVerificationUrl = eMailVerificationUrl;
        this.passwordEncoder = passwordEncoder;
    }

    @PostMapping(produces = MediaType.APPLICATION_JSON_VALUE,value = "/changePassword")
    public ResponseEntity<?> changePassword(@AuthenticationPrincipal final AuthUser authUser, @NotNull @RequestHeader("New-Password") final String newPassword) {
        final String decodedPassword = URLDecoder.decode(newPassword, StandardCharsets.UTF_8);
        final User user = authUser.getUser();
        LOG.info("Password change for '{}'", user.getEmail());
        final String trimmedPassword = StringUtils.trimToEmpty(decodedPassword);
        if (trimmedPassword.length() < 8 ) {
            LOG.warn("Password too short");
            return new ResponseEntity<>("Password too short", HttpStatus.BAD_REQUEST);
        }
        final String key = passwordEncoder.encode(trimmedPassword);
        userDao.updateCredentials(user.getId(), key);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @PostMapping(produces = MediaType.APPLICATION_JSON_VALUE,value = "/newUploadToken")
    public ResponseEntity<?> newUploadToken(@RequestHeader("User-Agent") final String userAgent, @NotNull @RequestHeader("Email") final String email) {
        return resetPassword(userAgent, email);
    }

    @PostMapping(produces = MediaType.APPLICATION_JSON_VALUE,value = "/resetPassword")
    public ResponseEntity<?> resetPassword(@RequestHeader("User-Agent") final String userAgent, @NotNull @RequestHeader("NameOrEmail") final String nameOrEmail) {
        LOG.info("Password reset requested for '{}'", nameOrEmail);

        final User user = userDao.findByEmail(User.normalizeEmail(nameOrEmail))
                .orElse(userDao.findByNormalizedName(User.normalizeName(nameOrEmail)).orElse(null));

        if (user == null) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }

        if (StringUtils.isBlank(user.getEmail())) {
            monitor.sendMessage(
                    String.format("Password reset for '%s' failed, because Email is empty: '%s'%nvia %s",
                            nameOrEmail, user, userAgent));
            return new ResponseEntity<>("Email is empty", HttpStatus.BAD_REQUEST);
        }

        user.setNewPassword(createNewPassword());
        encryptPassword(user);
        saveNewPassword(userAgent, user);
        sendPasswordMail(user);
        if (!user.isEmailVerified()) {
            // if the email is not yet verified, we can verify it with the next login
            userDao.updateEmailVerification(user.getId(), User.EMAIL_VERIFIED_AT_NEXT_LOGIN);
        }
        return new ResponseEntity<>(HttpStatus.ACCEPTED);
    }

    private void saveNewPassword(final String userAgent, final User user) {
        userDao.updateCredentials(user.getId(), user.getKey());
        monitor.sendMessage(
                String.format("Reset Password for '%s', email='%s'%nvia %s",
                        user.getName(), user.getEmail(), userAgent));

        LOG.info("Reset Password for '{}'", user.getName());
    }

    @PostMapping(produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE, value = "/registration")
    public ResponseEntity<?> register(@RequestHeader("User-Agent") final String userAgent, @NotNull final User registration) {
        LOG.info("New registration for '{}' with '{}'", registration.getName(), registration.getEmail());

        if (!registration.isValidForRegistration()) {
            LOG.warn("Registration for '{}' with '{}' invalid", registration.getName(), registration.getEmail());
            return new ResponseEntity<>("Invalid data", HttpStatus.BAD_REQUEST);
        }

        final Optional<User> existingName = userDao.findByNormalizedName(registration.getNormalizedName());
        if (existingName.isPresent() && !registration.getEmail().equals(existingName.get().getEmail())) {
            monitor.sendMessage(
                    String.format("Registration for user '%s' with eMail '%s' failed, because name is already taken by different eMail '%s'%nvia %s",
                            registration.getName(), registration.getEmail(), existingName.get().getEmail(), userAgent));
            return new ResponseEntity<>("Conflict with other user or email", HttpStatus.CONFLICT);
        }

        if (userDao.findByEmail(registration.getEmail()).isPresent()) {
            monitor.sendMessage(
                    String.format("Registration for user '%s' with eMail '%s' failed, because eMail is already taken%nvia %s",
                            registration.getName(), registration.getEmail(), userAgent));
            return new ResponseEntity<>("Conflict with other user or email", HttpStatus.CONFLICT);
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
        return new ResponseEntity<>(HttpStatus.ACCEPTED);
    }

    private String createNewPassword() {
        return RandomStringUtils.randomAlphanumeric(12);
    }

    private void encryptPassword(@NotNull final User user) {
        user.setKey(passwordEncoder.encode(user.getNewPassword()));
        user.setUploadTokenSalt(null);
        user.setUploadToken(null);
    }

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE, value = "/myProfile")
    public ResponseEntity<?> getMyProfile(@AuthenticationPrincipal final AuthUser authUser) {
        final User user = authUser.getUser();
        LOG.info("Get profile for '{}'", user.getEmail());
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @PostMapping(produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE, value = "/myProfile")
    public ResponseEntity<?> updateMyProfile(@RequestHeader("User-Agent") final String userAgent, @NotNull final User newProfile, @AuthenticationPrincipal final AuthUser authUser) {
        final User user = authUser.getUser();
        LOG.info("Update profile for '{}'", user.getEmail());

        if (!newProfile.isValid()) {
            LOG.info("User invalid {}", newProfile);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        }

        if (!newProfile.getNormalizedName().equals(user.getNormalizedName())) {
            if (userDao.findByNormalizedName(newProfile.getNormalizedName()).isPresent()) {
                LOG.info("Name conflict '{}'", newProfile.getName());
                return new ResponseEntity<>("Conflict with other user or email", HttpStatus.CONFLICT);
            }
            monitor.sendMessage(
                    String.format("Update nickname for user '%s' to '%s'%n%s",
                            user.getName(), newProfile.getName(), userAgent));
        }

        if (!newProfile.getEmail().equals(user.getEmail())) {
            if (userDao.findByEmail(newProfile.getEmail()).isPresent()) {
                LOG.info("Email conflict '{}'", newProfile.getEmail());
                return new ResponseEntity<>("Conflict with other user or email", HttpStatus.CONFLICT);
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
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @PostMapping("/resendEmailVerification")
    public ResponseEntity<?> resendEmailVerification(@AuthenticationPrincipal final AuthUser authUser) {
        final User user = authUser.getUser();
        LOG.info("Resend EmailVerification for '{}'", user.getEmail());

        user.setEmailVerificationToken(UUID.randomUUID().toString());
        userDao.updateEmailVerification(user.getId(), user.getEmailVerification());
        sendEmailVerification(user);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @GetMapping("/emailVerification/{token}")
    public ResponseEntity<?> emailVerification(@RequestHeader("User-Agent") final String userAgent, @PathVariable("token") final String token) {
        final Optional<User> userByToken = userDao.findByEmailVerification(User.EMAIL_VERIFICATION_TOKEN + token);
        if (userByToken.isPresent()) {
            final User user = userByToken.get();
            userDao.updateEmailVerification(user.getId(), User.EMAIL_VERIFIED);
            monitor.sendMessage(
                    String.format("Email verified {nickname='%s', email='%s'}%nvia %s", user.getName(), user.getEmail(), userAgent));
            return new ResponseEntity<>("Email successfully verified!", HttpStatus.OK);
        }
        return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    }

    private void sendPasswordMail(@NotNull final User user) {
        final String text = String.format("Hello,%n%n" +
                        "your new password is: %1$s%n%n" +
                        "Cheers%n" +
                        "Your Railway-Stations-Team%n" +
                        "%n---%n" +
                        "Hallo,%n%n" +
                        "Dein neues Passwort lautet: %1$s%n%n" +
                        "Viele Grüße%n" +
                        "Dein Bahnhofsfoto-Team", user.getNewPassword());
        mailer.send(user.getEmail(), "Railway-Stations.org new password", text);
        LOG.info("Password sent to {}", user.getEmail());
    }

    private void sendEmailVerification(@NotNull final User user) {
        final String url = eMailVerificationUrl + user.getEmailVerificationToken();
        final String text = String.format("Hello,%n%n" +
                        "please click on %1$s to verify your eMail-Address.%n%n" +
                        "Cheers%n" +
                        "Your Railway-Stations-Team%n" +
                        "%n---%n" +
                        "Hallo,%n%n" +
                        "bitte klicke auf %1$s, um Deine eMail-Adresse zu verifizieren%n%n" +
                        "Viele Grüße%n" +
                        "Dein Bahnhofsfoto-Team", url);
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
