package org.railwaystations.rsapi.auth;

import org.apache.commons.lang3.StringUtils;
import org.railwaystations.rsapi.model.User;
import org.springframework.security.core.GrantedAuthority;

import javax.validation.constraints.NotNull;
import java.util.Collection;

public class AuthUser extends org.springframework.security.core.userdetails.User {

    private final User user;

    public AuthUser(@NotNull final User user, final Collection<? extends GrantedAuthority> authorities) {
        super(user.getDisplayName(), StringUtils.defaultString(user.getKey()), authorities);
        this.user = user;
    }

    public User getUser() {
        return user;
    }
}
