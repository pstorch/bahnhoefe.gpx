package org.railwaystations.api.auth;

import io.dropwizard.auth.AuthFilter;
import org.apache.commons.io.output.NullOutputStream;
import org.apache.http.entity.InputStreamEntity;
import org.eclipse.jetty.http.HttpStatus;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.SecurityContext;
import java.io.IOException;
import java.io.InputStream;
import java.security.Principal;

public class UploadTokenAuthFilter<P extends Principal> extends AuthFilter<UploadTokenCredentials, P> {

    @Override
    public void filter(final ContainerRequestContext requestContext) {
        final UploadTokenCredentials credentials =
                new UploadTokenCredentials(requestContext.getHeaders().getFirst("Email"),
                        requestContext.getHeaders().getFirst("Upload-Token"));
        if (!authenticate(requestContext, credentials, SecurityContext.BASIC_AUTH)) {
            consumeBody(requestContext.getEntityStream());
            throw new WebApplicationException(HttpStatus.UNAUTHORIZED_401);
        }
    }

    private void consumeBody(final InputStream body) {
        if (body != null) {
            final InputStreamEntity inputStreamEntity = new InputStreamEntity(body);
            try {
                inputStreamEntity.writeTo(new NullOutputStream());
            } catch (final IOException ignored) {
            }
        }
    }


    public static class Builder<P extends Principal>
            extends AuthFilterBuilder<UploadTokenCredentials, P, UploadTokenAuthFilter<P>> {

        @Override
        protected UploadTokenAuthFilter<P> newInstance() {
            return new UploadTokenAuthFilter<>();
        }
    }
}
