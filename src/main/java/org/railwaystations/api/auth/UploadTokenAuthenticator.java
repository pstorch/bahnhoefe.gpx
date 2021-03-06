package org.railwaystations.api.auth;

import io.dropwizard.auth.Authenticator;
import org.railwaystations.api.db.UserDao;
import org.railwaystations.api.model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

public class UploadTokenAuthenticator extends AbstractAuthenticator implements Authenticator<UploadTokenCredentials, AuthUser> {

    private static final Logger LOG = LoggerFactory.getLogger(UploadTokenAuthenticator.class);

    public UploadTokenAuthenticator(final UserDao userDao, final TokenGenerator tokenGenerator) {
        super(userDao, tokenGenerator);
    }

    @Override
    public Optional<AuthUser> authenticate(final UploadTokenCredentials credentials) {
        final User user = findUser(credentials.getEmail());
        if (user == null) {
            LOG.info("User with email or name '{}' not found", credentials.getEmail());
            return Optional.empty();
        }

        return authenticate(credentials.getUploadToken(), user);
    }

}
