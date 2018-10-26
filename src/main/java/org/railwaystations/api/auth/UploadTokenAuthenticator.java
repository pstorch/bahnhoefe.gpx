package org.railwaystations.api.auth;

import org.railwaystations.api.TokenGenerator;
import org.railwaystations.api.db.UserDao;
import org.railwaystations.api.model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

public class UploadTokenAuthenticator implements io.dropwizard.auth.Authenticator<UploadTokenCredentials, AuthUser> {

    private static final Logger LOG = LoggerFactory.getLogger(UploadTokenAuthenticator.class);

    private final UserDao userDao;
    private final TokenGenerator tokenGenerator;

    public UploadTokenAuthenticator(final UserDao userDao, final TokenGenerator tokenGenerator) {
        this.userDao = userDao;
        this.tokenGenerator = tokenGenerator;
    }

    @Override
    public Optional<AuthUser> authenticate(final UploadTokenCredentials credentials) {
        final User user = userDao.findByEmail(User.normalizeEmail(credentials.getEmail())).orElse(null);
        if (user == null) {
            LOG.info("User with email '{}' not found", credentials.getEmail());
            return Optional.empty();
        }

        if (!tokenGenerator.buildFor(credentials.getNickname(), credentials.getEmail(), user.getUploadTokenSalt()).equals(credentials.getUploadToken())) {
            LOG.info("Token doesn't fit to nickname '{}' and email '{}'", credentials.getNickname(), credentials.getEmail());
            return Optional.empty();
        }

        return Optional.of(new AuthUser(user));
    }

}
