package org.railwaystations.api.resources;

import io.dropwizard.auth.Auth;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.output.NullOutputStream;
import org.apache.http.entity.InputStreamEntity;
import org.eclipse.jetty.util.URIUtil;
import org.railwaystations.api.StationsRepository;
import org.railwaystations.api.auth.AuthUser;
import org.railwaystations.api.model.Station;
import org.railwaystations.api.monitoring.Monitor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.validation.constraints.NotNull;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

@Path("/photoUpload")
public class PhotoUploadResource {

    private static final Logger LOG = LoggerFactory.getLogger(PhotoUploadResource.class);

    private static final long MAX_SIZE = 20_000_000L;
    private static final String IMAGE_PNG = "image/png";
    private static final String IMAGE_JPEG = "image/jpeg";

    private final StationsRepository repository;
    private final File uploadDir;
    private final Monitor monitor;

    public PhotoUploadResource(final StationsRepository repository, final String uploadDir, final Monitor monitor) {
        this.repository = repository;
        this.uploadDir = new File(uploadDir);
        this.monitor = monitor;
    }

    @POST
    @Consumes({IMAGE_PNG, IMAGE_JPEG})
    @Produces(MediaType.APPLICATION_JSON)
    public Response post(final InputStream body,
                         @NotNull @HeaderParam("Station-Id") final String stationId,
                         @NotNull @HeaderParam("Country") final String country,
                         @HeaderParam("Content-Type") final String contentType,
                         @Auth final AuthUser user) {
        LOG.info("Nickname: {}, Email: {}, Country: {}, Station-Id: {}, Content-Type: {}", user.getName(), user.getUser().getEmail(), country, stationId, contentType);

        final Map<Station.Key, Station> stationsMap = repository.getStationsByCountry(country);
        if (stationsMap.isEmpty()) {
            return consumeBodyAndReturn(body, Response.Status.BAD_REQUEST);
        }

        final Station station = stationsMap.get(new Station.Key(country, stationId));
        if (station == null) {
            return consumeBodyAndReturn(body, Response.Status.BAD_REQUEST);
        }

        final File uploadCountryDir = new File(uploadDir, country);
        final String filename = toFilename(stationId, contentType, user.getUser().getNormalizedName());
        final boolean duplicate = isDuplicate(station, uploadCountryDir, filename);
        final File file = new File(uploadCountryDir, filename);
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
            monitor.sendMessage(String.format("New photo upload for %s: http://inbox.railway-stations.org/%s/%s%s", station.getTitle(), country, URIUtil.encodePath(filename), duplicateInfo));
        } catch (final IOException e) {
            LOG.error("Error copying the uploaded file to {}", file, e);
            return consumeBodyAndReturn(body, Response.Status.INTERNAL_SERVER_ERROR);
        }

        return duplicate ? Response.status(Response.Status.CONFLICT).build() : Response.accepted().build();
    }

    private String toFilename(final String stationId, final String contentType, final String nickname) {
        return String.format("%s-%s.%s", nickname, stationId, mimeToExtension(contentType));
    }

    private boolean isDuplicate(final Station station, final File uploadCountryDir, final String filename) {
        boolean duplicate = station.hasPhoto();
        if (!duplicate) {
            final File[] listFiles = uploadCountryDir.listFiles(pathname -> {
                final String stationIdentifier = "-" + station.getKey().getId() + ".";
                return pathname.getName().contains(stationIdentifier) && !pathname.getName().equals(filename);
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
