package org.railwaystations.api.resources;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.output.NullOutputStream;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.entity.InputStreamEntity;
import org.eclipse.jetty.util.URIUtil;
import org.railwaystations.api.StationsRepository;
import org.railwaystations.api.TokenGenerator;
import org.railwaystations.api.model.Station;
import org.railwaystations.api.monitoring.Monitor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.validation.constraints.NotNull;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.*;
import java.util.Map;

@Path("/photoUpload")
public class PhotoUploadResource {

    private static final Logger LOG = LoggerFactory.getLogger(PhotoUploadResource.class);

    private static final long MAX_SIZE = 20_000_000L;
    private static final String IMAGE_PNG = "image/png";
    private static final String IMAGE_JPEG = "image/jpeg";

    private final StationsRepository repository;
    private final String apiKey;
    private final TokenGenerator tokenGenerator;
    private final File uploadDir;
    private final Monitor monitor;

    public PhotoUploadResource(final StationsRepository repository, final String apiKey, final TokenGenerator tokenGenerator, final String uploadDir, final Monitor monitor) {
        this.repository = repository;
        this.apiKey = apiKey;
        this.tokenGenerator = tokenGenerator;
        this.uploadDir = new File(uploadDir);
        this.monitor = monitor;
    }

    @POST
    @Consumes({IMAGE_PNG, IMAGE_JPEG})
    @Produces(MediaType.APPLICATION_JSON)
    public Response post(final InputStream body,
                         @NotNull @HeaderParam("API-Key") final String apiKey,
                         @NotNull @HeaderParam("Upload-Token") final String inUploadToken,
                         @NotNull @HeaderParam("Nickname") final String inNickname,
                         @NotNull @HeaderParam("Email") final String inEmail,
                         @NotNull @HeaderParam("Station-Id") final String stationId,
                         @NotNull @HeaderParam("Country") final String country,
                         @HeaderParam("Content-Type") final String contentType)
            throws UnsupportedEncodingException {
        // trim user input
        final String nickname = StringUtils.trimToEmpty(inNickname);
        final String email = StringUtils.trimToEmpty(inEmail);

        LOG.info("Nickname: {}, Email: {}, Country: {}, Station-Id: {}, Content-Type: {}", nickname, email, country, stationId, contentType);

        if (!this.apiKey.equals(apiKey)) {
            LOG.info("invalid API key");
            return consumeBodyAndReturn(body, Response.Status.FORBIDDEN);
        }

        if (nickname.contains("/")) {
            return consumeBodyAndReturn(body, Response.Status.BAD_REQUEST);
        }

        final String uploadToken = StringUtils.trimToEmpty(inUploadToken);
        if (!tokenGenerator.buildFor(nickname, email).equals(uploadToken)) {
            LOG.info("Token doesn't fit to nickname {} and email {}", nickname, email);
            return consumeBodyAndReturn(body, Response.Status.UNAUTHORIZED);
        }

        final Map<Integer, Station> stationsMap = repository.get(country);
        if (stationsMap.isEmpty()) {
            return consumeBodyAndReturn(body, Response.Status.BAD_REQUEST);
        }

        final Station station = stationsMap.get(Integer.valueOf(stationId));
        if (station == null) {
            return consumeBodyAndReturn(body, Response.Status.BAD_REQUEST);
        }

        final File uploadCountryDir = new File(uploadDir, country);
        final boolean duplicate = isDuplicate(stationId, station, uploadCountryDir);
        final String fileName = String.format("%s-%s.%s", nickname, stationId, mimeToExtension(contentType));
        final File file = new File(uploadCountryDir, fileName);
        LOG.info("Writing photo to {}", file);
        try {
            FileUtils.forceMkdir(uploadCountryDir);
            final long bytesRead = IOUtils.copyLarge(body, new FileOutputStream(file), 0L, MAX_SIZE);
            if (bytesRead == MAX_SIZE) {
                FileUtils.deleteQuietly(file);
                return consumeBodyAndReturn(body, Response.Status.REQUEST_ENTITY_TOO_LARGE);
            }
            String duplicateInfo = "";
            if (duplicate) {
                duplicateInfo = " (possible duplicate!)";
            }
            monitor.sendMessage(String.format("New photo upload for %s: http://inbox.railway-stations.org/%s/%s%s", station.getTitle(), country, URIUtil.encodePath(fileName), duplicateInfo));
        } catch (final IOException e) {
            LOG.error("Error copying the uploaded file to {}", file, e);
            return consumeBodyAndReturn(body, Response.Status.INTERNAL_SERVER_ERROR);
        }

        return duplicate ? Response.status(Response.Status.CONFLICT).build() : Response.accepted().build();
    }

    private boolean isDuplicate(@NotNull @HeaderParam("Station-Id") final String stationId, final Station station, final File uploadCountryDir) {
        boolean duplicate = station.hasPhoto();
        if (!duplicate) {
            final File[] listFiles = uploadCountryDir.listFiles(pathname -> {
                final String stationIdentifier = "-" + stationId + ".";
                return pathname.getName().contains(stationIdentifier);
            });
            duplicate = listFiles != null && listFiles.length > 0;
        }
        return duplicate;
    }

    private Response consumeBodyAndReturn(final InputStream body, final Response.Status status) {
        if (body != null) {
            final InputStreamEntity inputStreamEntity = new InputStreamEntity(body);
            try {
                inputStreamEntity.writeTo(new NullOutputStream());
            } catch (final IOException e) {
                LOG.warn("Unable to consume body", e);
            }
        }
        return Response.status(status).build();
    }

    private String mimeToExtension(final String contentType) {
        switch (contentType) {
            case IMAGE_PNG: return "png";
            case IMAGE_JPEG: return "jpg";
            default:
                throw new IllegalArgumentException("Unknown contentType " + contentType);
        }
    }

}
