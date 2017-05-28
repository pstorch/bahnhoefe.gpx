package org.railwaystations.api.resources;

import org.railwaystations.api.TokenGenerator;
import org.railwaystations.api.monitoring.Monitor;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.validation.constraints.NotNull;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.*;
import java.util.Set;

@Path("/photoUpload")
public class PhotoUploadResource {

    private static final Logger LOG = LoggerFactory.getLogger(PhotoUploadResource.class);

    private static final long MAX_SIZE = 20_000_000L;

    private final String apiKey;
    private final TokenGenerator tokenGenerator;
    private final File uploadDir;
    private final Monitor monitor;
    private final Set<String> countries;

    public PhotoUploadResource(final String apiKey, final TokenGenerator tokenGenerator, final String uploadDir, final Set<String> countries, final Monitor monitor) {
        this.apiKey = apiKey;
        this.tokenGenerator = tokenGenerator;
        this.uploadDir = new File(uploadDir);
        this.countries = countries;
        this.monitor = monitor;
    }

    @POST
    @Consumes({"image/png", "image/jpeg"})
    @Produces(MediaType.APPLICATION_JSON)
    public Response post(final InputStream body,
                         @NotNull @HeaderParam("API-Key") final String apiKey,
                         @NotNull @HeaderParam("Upload-Token") final String uploadToken,
                         @NotNull @HeaderParam("Nickname") final String nickname,
                         @NotNull @HeaderParam("Email") final String email,
                         @NotNull @HeaderParam("Station-Id") final String stationId,
                         @NotNull @HeaderParam("Country") final String country,
                         @HeaderParam("Content-Type") final String contentType)
            throws UnsupportedEncodingException {
        LOG.info("Nickname: {}, Email: {}, Country: {}, Station-Id: {}, Content-Type: {}", nickname, email, country, stationId, contentType);

        // TODO: add upload-token and verify

        if (!this.apiKey.equals(apiKey)) {
            LOG.info("invalid API key");
            return Response.status(Response.Status.FORBIDDEN).build();
        }

        if (nickname.contains("/")) {
            return Response.status(Response.Status.BAD_REQUEST).build();
        }

        if (!tokenGenerator.buildFor(nickname, email).equals(uploadToken)) {
            LOG.info("Token doesn't fit to nickname {} and email {}", nickname, email);
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }

        if (!countries.contains(country)) {
            return Response.status(Response.Status.BAD_REQUEST).build();
        }

        final File uploadCountryDir = new File(uploadDir, country);
        final String fileName = String.format("%s-%s.%s", nickname, stationId, mimeToExtension(contentType));
        final File file = new File(uploadCountryDir, fileName);
        LOG.info("Writing photo to {}", file);
        try {
            FileUtils.forceMkdir(uploadCountryDir);
            final long bytesRead = IOUtils.copyLarge(body, new FileOutputStream(file), 0L, MAX_SIZE);
            if (bytesRead == MAX_SIZE) {
                FileUtils.deleteQuietly(file);
                return Response.status(Response.Status.REQUEST_ENTITY_TOO_LARGE).build();
            }
            monitor.sendMessage(String.format("New photo upload: %s/%s", country, fileName));
        } catch (final IOException e) {
            LOG.error("Error copying the uploaded file to {}", file, e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }

        return Response.accepted().build();
    }

    private String mimeToExtension(final String contentType) {
        return contentType.substring(contentType.lastIndexOf('/') + 1);
    }

}
