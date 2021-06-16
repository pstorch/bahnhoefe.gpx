package org.railwaystations.rsapi.auth;

import org.railwaystations.rsapi.model.User;

import javax.validation.constraints.NotNull;
import java.security.Principal;

public class AuthUser implements Principal {

    private final User user;

    public AuthUser(@NotNull final User user) {
        this.user = user;
    }

    @Override
    public String getName() {
        return user.getDisplayName();
    }

    public User getUser() {
        return user;
    }
}
