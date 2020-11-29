package org.railwaystations.api.auth;

import io.dropwizard.auth.AuthenticationException;
import io.dropwizard.auth.Authenticator;
import org.railwaystations.api.TokenGenerator;
import org.railwaystations.api.db.UserDao;

import java.security.Principal;
import java.util.Optional;

public class OAuthAuthenticator implements Authenticator<String, AuthUser> {
    private final UserDao userDao;

    public OAuthAuthenticator(final UserDao userDao) {
        this.userDao = userDao;
    }

    @Override
    public Optional<AuthUser> authenticate(final String token) throws AuthenticationException {
        return Optional.empty();
    }
}
