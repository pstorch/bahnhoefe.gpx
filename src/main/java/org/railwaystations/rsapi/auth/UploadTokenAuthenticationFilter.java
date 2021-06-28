package org.railwaystations.rsapi.auth;

import org.apache.commons.lang3.StringUtils;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AbstractAuthenticationProcessingFilter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class UploadTokenAuthenticationFilter extends AbstractAuthenticationProcessingFilter {

    public UploadTokenAuthenticationFilter () {
        super("/");
    }

    @Override
    public Authentication attemptAuthentication(final HttpServletRequest request, final HttpServletResponse response)
            throws AuthenticationException {
        final String email = request.getHeader("Email");
        final String uploadToken = request.getHeader("Upload-Token");
        if (StringUtils.isBlank(email) || StringUtils.isBlank(uploadToken)) {
            return null;
        }

        final UsernamePasswordAuthenticationToken token =
                new UsernamePasswordAuthenticationToken(email, uploadToken);

        return getAuthenticationManager().authenticate(token);
    }

}
