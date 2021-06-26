package org.railwaystations.rsapi.auth;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;

public class RSAuthenticationProvider implements AuthenticationProvider {

    private static final Logger LOG = LoggerFactory.getLogger(RSAuthenticationProvider.class);

    @Autowired
    private LazySodiumPasswordEncoder passwordEncoder;

    @Autowired
    private TokenGenerator tokenGenerator;

    @Autowired
    private RSUserDetailsService userDetailsService;

    @Override
    public boolean supports(final Class<?> authentication) {
        return (UsernamePasswordAuthenticationToken.class.isAssignableFrom(authentication));
    }

    @Override
    public Authentication authenticate(final Authentication authentication)
            throws AuthenticationException {
        final UsernamePasswordAuthenticationToken token = (UsernamePasswordAuthenticationToken) authentication;

        final AuthUser user = userDetailsService.loadUserByUsername(String.valueOf(token.getPrincipal()));
        if (user == null) {
            LOG.info("User with email or name '{}' not found", token.getName());
            return null;
        }

        // try to verify user defined password
        if (passwordEncoder.matches(token.getCredentials().toString(), user.getUser().getKey())) {
            LOG.info("User verified by password '{}'", user.getUsername());
            userDetailsService.updateEmailVerification(user.getUser());

            return new UsernamePasswordAuthenticationToken(
                    token.getPrincipal(), token.getCredentials(), user.getAuthorities());
        }

        // fallback to token
        final Long tokenSalt = user.getUser().getUploadTokenSalt();
        if (tokenSalt != null && tokenSalt > 0 &&
                tokenGenerator.buildFor(String.valueOf(token.getPrincipal()), tokenSalt).equals(token.getCredentials())) {
            LOG.info("User verified by UploadToken '{}'", user.getUsername());
            userDetailsService.updateEmailVerification(user.getUser());
            return new UsernamePasswordAuthenticationToken(
                    token.getPrincipal(), token.getCredentials(), user.getAuthorities());
        }

        LOG.info("Password failed and UploadToken doesn't fit to user '{}'", token.getPrincipal());
        return null;
    }

}
