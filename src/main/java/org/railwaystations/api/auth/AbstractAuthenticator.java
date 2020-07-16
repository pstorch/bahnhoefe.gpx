package org.railwaystations.api.auth;

import org.railwaystations.api.PasswordUtil;
import org.railwaystations.api.TokenGenerator;
import org.railwaystations.api.db.UserDao;
import org.railwaystations.api.model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

abstract class AbstractAuthenticator {

    private static final Logger LOG = LoggerFactory.getLogger(AbstractAuthenticator.class);

    private final UserDao userDao;
    private final TokenGenerator tokenGenerator;

    AbstractAuthenticator(final UserDao userDao, final TokenGenerator tokenGenerator) {
        this.userDao = userDao;
        this.tokenGenerator = tokenGenerator;
    }

    User findUser(final String username) {
        return userDao.findByEmail(User.normalizeEmail(username))
                .orElse(userDao.findByNormalizedName(User.normalizeName(username)).orElse(null));
    }

    Optional<AuthUser> authenticate(final String secret, final User user) {
        // try to verify user defined password
        if (PasswordUtil.verifyPassword(secret, user.getKey())) {
            LOG.info("User verified by password '{}'", user.getEmail());
            updateEmailVerification(user);
            return Optional.of(new AuthUser(user));
        }

        // fallback to token
        final Long tokenSalt = user.getUploadTokenSalt();
        if (tokenSalt != null && tokenSalt > 0 && tokenGenerator.buildFor(user.getEmail(), user.getUploadTokenSalt()).equals(secret)) {
            LOG.info("User verified by UploadToken '{}'", user.getEmail());
            updateEmailVerification(user);
            return Optional.of(new AuthUser(user));
        }

        LOG.info("Password failed and UploadToken doesn't fit to email '{}'", user.getEmail());
        return Optional.empty();
    }

    private void updateEmailVerification(final User user) {
        if (user.isEmailVerifiedWithNextLogin()) {
            LOG.info("User eMail verified by successful login '{}'", user.getEmail());
            userDao.updateEmailVerification(user.getId(), User.EMAIL_VERIFIED);
        }
    }


}
