package org.railwaystations.rsapi.auth;

import org.railwaystations.rsapi.db.UserDao;
import org.railwaystations.rsapi.model.User;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Optional;

@Service
public class RSUserDetailsService implements UserDetailsService {

    private UserDao userDao;

    public RSUserDetailsService(final UserDao userDao) {
        this.userDao = userDao;
    }

    @Override
    public AuthUser loadUserByUsername(final String username) throws UsernameNotFoundException {
        final User user = userDao.findByEmail(User.normalizeEmail(username))
                .orElse(userDao.findByNormalizedName(User.normalizeName(username)).orElse(null));
        if (user == null) {
            throw new UsernameNotFoundException(String.format("User '%s' not found", username));
        }
        final GrantedAuthority grantedAuthority = new SimpleGrantedAuthority(user.getRole());
        return new AuthUser(user, Collections.singletonList(grantedAuthority));
    }

    public Optional<User> findById(final int id) {
        return userDao.findById(id);
    }

    public void updateEmailVerification(final User user) {
        if (user.isEmailVerifiedWithNextLogin()) {
            userDao.updateEmailVerification(user.getId(), User.EMAIL_VERIFIED);
        }
    }

}