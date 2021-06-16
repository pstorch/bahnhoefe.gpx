package org.railwaystations.rsapi.auth;

import io.dropwizard.auth.Authorizer;

public class UserAuthorizer implements Authorizer<AuthUser> {
    @Override
    @SuppressWarnings( "deprecation" )
    public boolean authorize(final AuthUser user, final String role) {
        return user.getUser().isAdmin() && role.equals("ADMIN");
    }
}
