package org.railwaystations.api.auth;

import io.dropwizard.auth.Authenticator;
import io.dropwizard.auth.basic.BasicCredentials;
import org.railwaystations.api.db.UserDao;
import org.railwaystations.api.model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

public class BasicAuthenticator extends AbstractAuthenticator implements Authenticator<BasicCredentials, AuthUser> {

    private static final Logger LOG = LoggerFactory.getLogger(BasicAuthenticator.class);

    public BasicAuthenticator(final UserDao userDao, final TokenGenerator tokenGenerator) {
        super(userDao, tokenGenerator);
    }

    @Override
    public Optional<AuthUser> authenticate(final BasicCredentials credentials) {
        final User user = findUser(credentials.getUsername());
        if (user == null) {
            LOG.info("User with email or name '{}' not found", credentials.getUsername());
            return Optional.empty();
        }

        return authenticate(credentials.getPassword(), user);
    }

}
