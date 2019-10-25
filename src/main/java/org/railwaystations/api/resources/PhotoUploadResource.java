package org.railwaystations.api.resources;

import io.dropwizard.auth.Auth;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.output.NullOutputStream;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.entity.InputStreamEntity;
import org.eclipse.jetty.util.URIUtil;
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataParam;
import org.railwaystations.api.StationsRepository;
import org.railwaystations.api.auth.AuthUser;
import org.railwaystations.api.auth.UploadTokenAuthenticator;
import org.railwaystations.api.auth.UploadTokenCredentials;
import org.railwaystations.api.model.Station;
import org.railwaystations.api.monitoring.Monitor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.activation.MimetypesFileTypeMap;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.*;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Optional;

@Path("/photoUpload")
public class PhotoUploadResource {

    private static final Logger LOG = LoggerFactory.getLogger(PhotoUploadResource.class);

    private static final long MAX_SIZE = 20_000_000L;
    private static final String IMAGE_PNG = "image/png";
    private static final String IMAGE_JPEG = "image/jpeg";

    private final StationsRepository repository;
    private final File uploadDir;
    private final Monitor monitor;
    private final UploadTokenAuthenticator authenticator;

    public PhotoUploadResource(final StationsRepository repository, final String uploadDir, final Monitor monitor, final UploadTokenAuthenticator authenticator) {
        this.repository = repository;
        this.uploadDir = new File(uploadDir);
        this.monitor = monitor;
        this.authenticator = authenticator;
    }

    /**
     * Not part of the "official" API.
     * Supports upload of photos via the website.
     */
    @POST
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    public String post(@FormDataParam("email") final String email,
                       @FormDataParam("uploadToken") final String uploadToken,
                       @FormDataParam("stationId") final String stationId,
                       @FormDataParam("countryCode") final String countryCode,
                       @FormDataParam("stationTitle") final String stationTitle,
                       @FormDataParam("latitude") final Double latitude,
                       @FormDataParam("longitude") final Double longitude,
                       @FormDataParam("comment") final String comment,
                       @FormDataParam("file") final InputStream file,
                       @FormDataParam("file") final FormDataContentDisposition fd,
                       @HeaderParam("Referer") final String referer,
                       @Auth final Optional<AuthUser> user) {
        LOG.info("MultipartFormData: email={}, station={}, country={}, file={}", email, stationId, countryCode, fd.getFileName());

        try {
            Optional<AuthUser> authUser = user;
            if (!authUser.isPresent()) { // fallback to UploadToken
                authUser = authenticator.authenticate(new UploadTokenCredentials(email, uploadToken));
                if (!authUser.isPresent()) {
                    final Response response = consumeBodyAndReturn(file, Response.Status.UNAUTHORIZED);
                    return createIFrameAnswer(response.getStatusInfo(), referer);
                }
            }

            final String contentType = MimetypesFileTypeMap.getDefaultFileTypeMap().getContentType(fd.getFileName());
            final Response response = uploadPhoto(file, stationId, countryCode, contentType, stationTitle, latitude, longitude, comment, authUser.get());

            return createIFrameAnswer(response.getStatusInfo(), referer);
        } catch (final Exception e) {
            LOG.error("FormUpload error", e);
            return createIFrameAnswer(Response.Status.INTERNAL_SERVER_ERROR, referer);
        }
    }

    @POST
    @Consumes({IMAGE_PNG, IMAGE_JPEG})
    @Produces(MediaType.APPLICATION_JSON)
    public Response post(final InputStream body,
                         @HeaderParam("Station-Id") final String stationId,
                         @HeaderParam("Country") final String country,
                         @HeaderParam("Content-Type") final String contentType,
                         @HeaderParam("Station-Title") final String encStationTitle,
                         @HeaderParam("Latitude") final Double latitude,
                         @HeaderParam("Longitude") final Double longitude,
                         @HeaderParam("Comment") final String encComment,
                         @Auth final AuthUser user) throws UnsupportedEncodingException {
        final String stationTitle = encStationTitle != null ? URLDecoder.decode(encStationTitle, "UTF-8") : null;
        final String comment = encComment != null ? URLDecoder.decode(encComment, "UTF-8") : null;
        LOG.info("Nickname: {}; Email: {}; Country: {}; Station-Id: {}; Koords: {},{}; Title: {}; Content-Type: {}",
                user.getName(), user.getUser().getEmail(), country, stationId, latitude, longitude, stationTitle, contentType);
        return uploadPhoto(body, stationId, country, contentType, stationTitle, latitude, longitude, comment, user);
    }

    private Response uploadPhoto(final InputStream body,
                         final String stationId,
                         final String country,
                         final String contentType,
                         final String stationTitle,
                         final Double latitude,
                         final Double longitude,
                         final String comment,
                         final AuthUser user) {
        final Station station = repository.findByCountryAndId(country, stationId);
        if (station == null) {
            LOG.warn("Station not found");
            if (StringUtils.isBlank(stationTitle) || latitude == null || longitude == null) {
                LOG.warn("Not enough data for missing station: title={}, latitude={}, longitude={}", stationTitle, latitude, longitude);
                return consumeBodyAndReturn(body, Response.Status.BAD_REQUEST);
            }
            if (latitude < -90 || latitude > 90 || longitude < -180 || longitude > 180) {
                LOG.warn("Lat/Lon out of range: latitude={}, longitude={}", latitude, longitude);
                return consumeBodyAndReturn(body, Response.Status.BAD_REQUEST);
            }
        }

        final String extension = mimeToExtension(contentType);
        if (extension == null) {
            LOG.warn("Unknown contentType '{}'", contentType);
            return consumeBodyAndReturn(body, Response.Status.BAD_REQUEST);
        }

        final File uploadDir = new File(this.uploadDir, StringUtils.isNotBlank(country) ? country : "missing");
        final String filename = toFilename(uploadDir, station, user.getUser().getNormalizedName(), extension);
        final boolean duplicate = station != null && isDuplicate(station, uploadDir, filename);
        final File file = new File(uploadDir, filename);
        LOG.info("Writing photo to {}", file);
        try {
            FileUtils.forceMkdir(uploadDir);
            final long bytesRead = IOUtils.copyLarge(body, new FileOutputStream(file), 0L, MAX_SIZE);
            if (bytesRead == MAX_SIZE) {
                FileUtils.deleteQuietly(file);
                return consumeBodyAndReturn(body, Response.Status.REQUEST_ENTITY_TOO_LARGE);
            }
            writeInfoFile(stationTitle, latitude, longitude, comment, station, uploadDir, filename);
            String duplicateInfo = "";
            if (duplicate) {
                duplicateInfo = " (possible duplicate!)";
            }
            if (station != null) {
                monitor.sendMessage(String.format("New photo upload for %s%n%s%nhttp://inbox.railway-stations.org/%s/%s%s",
                        station.getTitle(), StringUtils.trimToEmpty(comment), uploadDir.getName(), URIUtil.encodePath(filename), duplicateInfo));
            } else {
                monitor.sendMessage(String.format("Photo upload for missing station %s at %s,%s%n%s%nhttp://inbox.railway-stations.org/%s/%s%s",
                        stationTitle, latitude, longitude,
                        StringUtils.trimToEmpty(comment), uploadDir.getName(), URIUtil.encodePath(filename), duplicateInfo));
            }
        } catch (final IOException e) {
            LOG.error("Error copying the uploaded file to {}", file, e);
            return consumeBodyAndReturn(body, Response.Status.INTERNAL_SERVER_ERROR);
        }

        return duplicate ? Response.status(Response.Status.CONFLICT).build() : Response.accepted().build();
    }

    private void writeInfoFile(final String stationTitle, final Double latitude, final Double longitude, final String comment,
                               final Station station, final File uploadDir, final String filename) throws IOException {
        if (StringUtils.isNotBlank(comment) || station == null) {
            final File txt = new File(uploadDir, filename + ".txt");
            final FileOutputStream txtOut = new FileOutputStream(txt);
            final Collection<String> line = new ArrayList<>();
            if (station == null) {
                line.add(stationTitle);
                line.add(latitude + "," + longitude);
            }
            if (StringUtils.isNotBlank(comment)) {
                line.add(comment);
            }
            IOUtils.writeLines(line, null, txtOut, "UTF-8");
            txtOut.close();
        }
    }

    private String createIFrameAnswer(final Response.StatusType status, final String referer) {
        return "<script language=\"javascript\" type=\"text/javascript\">" +
               " window.top.window.postMessage('" + status.getStatusCode() + ": " + status.getReasonPhrase() + "', '" + referer + "');" +
               "</script>";
    }

    private String toFilename(final File uploadDir, final Station station, final String nickname, final String extension) {
        if (station != null) {
            return String.format("%s-%s.%s", nickname, station.getKey().getId(), extension);
        }
        for (int index = 1; index < 1024; index++) {
            File file = new File(uploadDir, String.format("%s-%s.%s", nickname, index, extension));
            if (!file.exists()) {
                return file.getName();
            }
        }
        throw new RuntimeException("More than 1024 missing photos from one nickname");
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
                return null;
        }
    }

}
