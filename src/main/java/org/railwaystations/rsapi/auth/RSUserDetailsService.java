package org.railwaystations.rsapi.auth;

import org.railwaystations.rsapi.db.UserDao;
import org.railwaystations.rsapi.model.User;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class RSUserDetailsService implements UserDetailsService {

    private final UserDao userDao;

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
        final List<GrantedAuthority> authorities = new ArrayList<>();
        for (final String role : user.getRoles()) {
            authorities.add(new SimpleGrantedAuthority(role));
        }
        return new AuthUser(user, authorities);
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