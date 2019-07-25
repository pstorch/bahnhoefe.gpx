package org.railwaystations.api.auth;

import io.dropwizard.auth.Authenticator;
import org.railwaystations.api.TokenGenerator;
import org.railwaystations.api.db.UserDao;
import org.railwaystations.api.model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

public class UploadTokenAuthenticator implements Authenticator<UploadTokenCredentials, AuthUser> {

    private static final Logger LOG = LoggerFactory.getLogger(UploadTokenAuthenticator.class);

    private final UserDao userDao;
    private final TokenGenerator tokenGenerator;

    public UploadTokenAuthenticator(final UserDao userDao, final TokenGenerator tokenGenerator) {
        this.userDao = userDao;
        this.tokenGenerator = tokenGenerator;
    }

    @Override
    public Optional<AuthUser> authenticate(final UploadTokenCredentials credentials) {
        final User user = userDao.findByEmail(User.normalizeEmail(credentials.getEmail()))
                        .orElse(userDao.findByNormalizedName(User.normalizeName(credentials.getEmail())).orElse(null));
        if (user == null) {
            LOG.info("User with email or name '{}' not found", credentials.getEmail());
            return Optional.empty();
        }

        if (!tokenGenerator.buildFor(user.getEmail(), user.getUploadTokenSalt()).equals(credentials.getUploadToken())) {
            LOG.info("Token doesn't fit to email '{}'", user.getEmail());
            return Optional.empty();
        }

        return Optional.of(new AuthUser(user));
    }

}
