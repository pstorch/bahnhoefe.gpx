package org.railwaystations.api.resources;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import org.railwaystations.api.db.UploadDao;
import org.railwaystations.api.model.Coordinates;
import org.railwaystations.api.model.Station;
import org.railwaystations.api.model.Upload;
import org.railwaystations.api.monitoring.Monitor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.activation.MimetypesFileTypeMap;
import javax.validation.constraints.NotNull;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.*;
import java.net.URLDecoder;
import java.util.List;
import java.util.Optional;

@Path("/photoUpload")
public class PhotoUploadResource {

    private static final Logger LOG = LoggerFactory.getLogger(PhotoUploadResource.class);

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static final long MAX_SIZE = 20_000_000L;
    private static final String IMAGE_PNG = "image/png";
    private static final String IMAGE_JPEG = "image/jpeg";
    private static final String MISSING = "missing";

    private final StationsRepository repository;
    private final File uploadDir;
    private final Monitor monitor;
    private final UploadTokenAuthenticator authenticator;
    private final UploadDao uploadDao;

    public PhotoUploadResource(final StationsRepository repository, final String uploadDir, final Monitor monitor, final UploadTokenAuthenticator authenticator, final UploadDao uploadDao) {
        this.repository = repository;
        this.uploadDir = new File(uploadDir);
        this.monitor = monitor;
        this.authenticator = authenticator;
        this.uploadDao = uploadDao;
    }

    /**
     * Not part of the "official" API.
     * Supports upload of photos via the website.
     */
    @POST
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    public String post(@HeaderParam("User-Agent") final String userAgent,
                       @FormDataParam("email") final String email,
                       @FormDataParam("uploadToken") final String uploadToken,
                       @FormDataParam("stationId") final String stationId,
                       @FormDataParam("countryCode") final String countryCode,
                       @FormDataParam("stationTitle") final String stationTitle,
                       @FormDataParam("latitude") final Double latitude,
                       @FormDataParam("longitude") final Double longitude,
                       @FormDataParam("comment") final String comment,
                       @FormDataParam("file") final InputStream file,
                       @FormDataParam("file") final FormDataContentDisposition fd,
                       @HeaderParam("Referer") final String referer) throws JsonProcessingException {
        LOG.info("MultipartFormData: email={}, station={}, country={}, file={}", email, stationId, countryCode, fd.getFileName());

        try {
            Optional<AuthUser> authUser = authenticator.authenticate(new UploadTokenCredentials(email, uploadToken));
            if (!authUser.isPresent()) {
                final UploadResponse response = consumeBodyAndReturn(file, new UploadResponse(Response.Status.UNAUTHORIZED));
                return createIFrameAnswer(response, referer);
            }

            final String contentType = MimetypesFileTypeMap.getDefaultFileTypeMap().getContentType(fd.getFileName());
            final UploadResponse response = uploadPhoto(userAgent, file, stationId, countryCode, contentType, stationTitle, latitude, longitude, comment, authUser.get());
            return createIFrameAnswer(response, referer);
        } catch (final Exception e) {
            LOG.error("FormUpload error", e);
            return createIFrameAnswer(new UploadResponse(Response.Status.INTERNAL_SERVER_ERROR), referer);
        }
    }

    @POST
    @Consumes({IMAGE_PNG, IMAGE_JPEG})
    @Produces(MediaType.APPLICATION_JSON)
    public Response post(final InputStream body,
                         @HeaderParam("User-Agent") final String userAgent,
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
        LOG.info("Nickname: {}; Email: {}; Country: {}; Station-Id: {}; Coords: {},{}; Title: {}; Content-Type: {}",
                user.getName(), user.getUser().getEmail(), country, stationId, latitude, longitude, stationTitle, contentType);
        final UploadResponse uploadResponse = uploadPhoto(userAgent, body, stationId, country, contentType, stationTitle, latitude, longitude, comment, user);
        return Response.status(uploadResponse.getStatus()).entity(uploadResponse).build();
    }

    @POST
    @Path("queryState")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public List<UploadStateQuery> queryState(@NotNull final List<UploadStateQuery> uploadStateQueries, @Auth final AuthUser user) {
        LOG.info("Query uploadStatus for Nickname: {}", user.getName());

        final String nickname = user.getUser().getNormalizedName();
        for (final UploadStateQuery uploadStateQuery : uploadStateQueries) {
            uploadStateQuery.state = UploadStateQuery.UploadStateState.UNKNOWN;
            final Station station = repository.findByCountryAndId(uploadStateQuery.countryCode, uploadStateQuery.id);
            if (station != null) {
                final File[] listFiles = new File(uploadDir, uploadStateQuery.countryCode).listFiles(pathname -> {
                    final String stationIdentifier = nickname + "-" + station.getKey().getId() + ".";
                    return pathname.getName().startsWith(stationIdentifier);
                });
                if (listFiles != null && listFiles.length > 0) {
                    uploadStateQuery.state = UploadStateQuery.UploadStateState.IN_REVIEW;
                } else if (station.hasPhoto()) {
                    if (station.getPhotographer().equals(user.getUser().getDisplayName())) {
                        uploadStateQuery.state = UploadStateQuery.UploadStateState.ACCEPTED;
                    } else {
                        uploadStateQuery.state = UploadStateQuery.UploadStateState.OTHER_USER;
                    }
                }
            } else if (uploadStateQuery.lat != null && uploadStateQuery.lon != null) {
                final File[] listFiles = new File(uploadDir, MISSING).listFiles(pathname -> pathname.getName().startsWith(nickname));
                final String coords = uploadStateQuery.lat + "," + uploadStateQuery.lon;
                if (listFiles != null) {
                    for (final File imageFile : listFiles) {
                        final File txtFile = new File(imageFile.getParent(), imageFile.getName() + ".txt");
                        if (txtFile.exists()) {
                            try {
                                if (FileUtils.readLines(txtFile, "UTF-8").stream().anyMatch(line -> line.contains(coords))) {
                                    uploadStateQuery.state = UploadStateQuery.UploadStateState.IN_REVIEW;
                                }
                            } catch (IOException e) {
                                LOG.error("Couldn't read text file {}", txtFile, e);
                            }
                        }
                    }
                }
            }
        }

        return uploadStateQueries;
    }

    private UploadResponse uploadPhoto(final String userAgent, final InputStream body, final String stationId,
                                 final String country, final String contentType, final String stationTitle,
                                 final Double latitude, final Double longitude, final String comment,
                                 final AuthUser user) {
        final Station station = repository.findByCountryAndId(country, stationId);
        Coordinates coordinates = null;
        if (station == null) {
            LOG.warn("Station not found");
            if (StringUtils.isBlank(stationTitle) || latitude == null || longitude == null) {
                LOG.warn("Not enough data for missing station: title={}, latitude={}, longitude={}", stationTitle, latitude, longitude);
                return consumeBodyAndReturn(body, new UploadResponse(Response.Status.BAD_REQUEST, "Not enough data for missing station, at least 'title', 'latitude' and 'longitude' have to be provided"));
            }
            if (Math.abs(latitude) > 90 || Math.abs(longitude) > 180) {
                LOG.warn("Lat/Lon out of range: latitude={}, longitude={}", latitude, longitude);
                return consumeBodyAndReturn(body, new UploadResponse(Response.Status.BAD_REQUEST, "'latitude' and/or 'longitude' out of range"));
            }
            coordinates = new Coordinates(latitude, longitude);
        }

        final String extension = mimeToExtension(contentType);
        if (extension == null) {
            LOG.warn("Unknown contentType '{}'", contentType);
            return consumeBodyAndReturn(body, new UploadResponse(Response.Status.BAD_REQUEST, "unsupported content type (only jpg and png are supported)"));
        }

        final boolean duplicate = isDuplicate(station);
        File file = null;
        String inboxUrl = null;
        Integer id = null;
        try {
            id = uploadDao.insert(new Upload(country, stationId, stationTitle, coordinates, user.getUser().getId(), extension, comment));
            final String filename = String.format("%d.%s", id, extension);
            file = new File(uploadDir, filename);
            LOG.info("Writing photo to {}", file);

            FileUtils.forceMkdir(uploadDir);
            final long bytesRead = IOUtils.copyLarge(body, new FileOutputStream(file), 0L, MAX_SIZE);
            if (bytesRead == MAX_SIZE) {
                FileUtils.deleteQuietly(file);
                return consumeBodyAndReturn(body, new UploadResponse(Response.Status.REQUEST_ENTITY_TOO_LARGE, "Photo too large, max " + MAX_SIZE + " bytes allowed"));
            }
            String duplicateInfo = "";
            if (duplicate) {
                duplicateInfo = " (possible duplicate!)";
            }
            inboxUrl = String.format("http://inbox.railway-stations.org/%s", URIUtil.encodePath(filename));
            if (station != null) {
                monitor.sendMessage(String.format("New photo upload for %s%n%s%n%s%s%nvia %s",
                        station.getTitle(), StringUtils.trimToEmpty(comment), inboxUrl, duplicateInfo, userAgent));
            } else {
                monitor.sendMessage(String.format("Photo upload for missing station %s at http://www.openstreetmap.org/?mlat=%s&mlon=%s&zoom=18&layers=M%n%s%n%s%s%nvia %s",
                        stationTitle, latitude, longitude,
                        StringUtils.trimToEmpty(comment), inboxUrl, duplicateInfo, userAgent));
            }
        } catch (final IOException e) {
            LOG.error("Error copying the uploaded file to {}", file, e);
            return consumeBodyAndReturn(body, new UploadResponse(Response.Status.INTERNAL_SERVER_ERROR));
        }

        return new UploadResponse(duplicate ? Response.Status.CONFLICT : Response.Status.ACCEPTED, id, inboxUrl);
    }

    private String createIFrameAnswer(final UploadResponse response, final String referer) throws JsonProcessingException {
        return "<script language=\"javascript\" type=\"text/javascript\">" +
               " window.top.window.postMessage('" + MAPPER.writeValueAsString(response) + "', '" + referer + "');" +
               "</script>";
    }

    private boolean isDuplicate(final Station station) {
        return station != null && (station.hasPhoto() ||  uploadDao.countPendingUploadsForStation(station.getKey().getCountry(), station.getKey().getId()) > 0);
    }

    private UploadResponse consumeBodyAndReturn(final InputStream body, final UploadResponse response) {
        if (body != null) {
            final InputStreamEntity inputStreamEntity = new InputStreamEntity(body);
            try {
                inputStreamEntity.writeTo(new NullOutputStream());
            } catch (final IOException e) {
                LOG.warn("Unable to consume body", e);
            }
        }
        return response;
    }

    private String mimeToExtension(final String contentType) {
        switch (contentType) {
            case IMAGE_PNG: return "png";
            case IMAGE_JPEG: return "jpg";
            default:
                return null;
        }
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class UploadResponse {
        private final Response.Status status;
        private final String message;
        private final Integer uploadId;
        private final String inboxUrl;

        public UploadResponse(final Response.Status status, final String message, final Integer uploadId, final String inboxUrl) {
            this.status = status;
            this.message = message;
            this.uploadId = uploadId;
            this.inboxUrl = inboxUrl;
        }

        public UploadResponse(final Response.Status status, final String message) {
            this(status, message, null, null);
        }

        public UploadResponse(Response.Status status) {
            this(status, status.getReasonPhrase());
        }

        public UploadResponse(final Response.Status status, final Integer id, final String inboxUrl) {
            this(status, status.getReasonPhrase(), id, inboxUrl);
        }

        @JsonIgnore
        public Response.Status getStatus() {
            return status;
        }

        public int getCode() {
            return status.getStatusCode();
        }

        public String getMessage() {
            return message;
        }

        public Integer getUploadId() {
            return uploadId;
        }

        public String getInboxUrl() {
            return inboxUrl;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class UploadStateQuery {
        private String countryCode;
        private String id;
        private Double lat;
        private Double lon;
        private UploadStateState state = UploadStateState.UNKNOWN;

        public UploadStateQuery() {
        }

        public UploadStateQuery(String countryCode, String id, Double lat, Double lon) {
            super();
            this.countryCode = countryCode;
            this.id = id;
            this.lat = lat;
            this.lon = lon;
        }

        public String getCountryCode() {
            return countryCode;
        }

        public String getId() {
            return id;
        }

        public Double getLat() {
            return lat;
        }

        public Double getLon() {
            return lon;
        }

        public UploadStateState getState() {
            return state;
        }

        public enum UploadStateState {
            UNKNOWN,
            IN_REVIEW,
            ACCEPTED,
            OTHER_USER;
        }

    }
}
