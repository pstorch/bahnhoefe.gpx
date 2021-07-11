package org.railwaystations.rsapi.auth;

import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class RSAuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    protected void handle(final HttpServletRequest request, final HttpServletResponse response,
                          final Authentication authentication) {
    }

}