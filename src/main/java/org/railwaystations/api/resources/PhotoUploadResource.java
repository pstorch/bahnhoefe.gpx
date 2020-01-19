package org.railwaystations.api.resources;

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
import org.railwaystations.api.PhotoImporter;
import org.railwaystations.api.StationsRepository;
import org.railwaystations.api.auth.AuthUser;
import org.railwaystations.api.auth.UploadTokenAuthenticator;
import org.railwaystations.api.auth.UploadTokenCredentials;
import org.railwaystations.api.db.CountryDao;
import org.railwaystations.api.db.PhotoDao;
import org.railwaystations.api.db.UploadDao;
import org.railwaystations.api.db.UserDao;
import org.railwaystations.api.model.*;
import org.railwaystations.api.monitoring.Monitor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.activation.MimetypesFileTypeMap;
import javax.annotation.security.RolesAllowed;
import javax.validation.constraints.NotNull;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.*;
import java.net.URLDecoder;
import java.util.List;
import java.util.Optional;

@Path("/")
public class PhotoUploadResource {

    private static final Logger LOG = LoggerFactory.getLogger(PhotoUploadResource.class);

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static final long MAX_SIZE = 20_000_000L;
    private static final String IMAGE_PNG = "image/png";
    private static final String IMAGE_JPEG = "image/jpeg";

    private final StationsRepository repository;
    private final File uploadDir;
    private final File photoDir;
    private final Monitor monitor;
    private final UploadTokenAuthenticator authenticator;
    private final UploadDao uploadDao;
    private final UserDao userDao;
    private final CountryDao countryDao;
    private final PhotoDao photoDao;
    private final String inboxBaseUrl;

    public PhotoUploadResource(final StationsRepository repository, final String uploadDir, final String photoDir,
                               final Monitor monitor, final UploadTokenAuthenticator authenticator, final UploadDao uploadDao,
                               final UserDao userDao, final CountryDao countryDao, final PhotoDao photoDao,
                               final String inboxBaseUrl) {
        this.repository = repository;
        this.uploadDir = new File(uploadDir);
        this.photoDir = new File(photoDir);
        this.monitor = monitor;
        this.authenticator = authenticator;
        this.uploadDao = uploadDao;
        this.userDao = userDao;
        this.countryDao = countryDao;
        this.photoDao = photoDao;
        this.inboxBaseUrl = inboxBaseUrl;
    }

    /**
     * Not part of the "official" API.
     * Supports upload of photos via the website.
     */
    @POST
    @Path("photoUpload")
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
            final Optional<AuthUser> authUser = authenticator.authenticate(new UploadTokenCredentials(email, uploadToken));
            if (!authUser.isPresent()) {
                final UploadResponse response = consumeBodyAndReturn(file, new UploadResponse(UploadResponse.UploadResponseState.UNAUTHORIZED));
                return createIFrameAnswer(response, referer);
            }

            final String contentType = MimetypesFileTypeMap.getDefaultFileTypeMap().getContentType(fd.getFileName());
            final UploadResponse response = uploadPhoto(userAgent, file, StringUtils.trimToNull(stationId), StringUtils.trimToNull(countryCode), contentType, stationTitle, latitude, longitude, comment, authUser.get());
            return createIFrameAnswer(response, referer);
        } catch (final Exception e) {
            LOG.error("FormUpload error", e);
            return createIFrameAnswer(new UploadResponse(UploadResponse.UploadResponseState.ERROR), referer);
        }
    }

    @POST
    @Path("photoUpload")
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
        final UploadResponse uploadResponse = uploadPhoto(userAgent, body, StringUtils.trimToNull(stationId), StringUtils.trimToNull(country), contentType, stationTitle, latitude, longitude, comment, user);
        return Response.status(uploadResponse.getState().getResponseStatus()).entity(uploadResponse).build();
    }

    @POST
    @Path("reportGhostStation")
    @Consumes({IMAGE_PNG, IMAGE_JPEG})
    @Produces(MediaType.APPLICATION_JSON)
    public UploadResponse reportGhostStation(@HeaderParam("User-Agent") final String userAgent,
                                             @HeaderParam("Station-Id") final String stationId,
                                             @HeaderParam("Country") final String country,
                                             @HeaderParam("Comment") final String encComment,
                                             @Auth final AuthUser user) throws UnsupportedEncodingException {
        final String comment = encComment != null ? URLDecoder.decode(encComment, "UTF-8") : null;
        LOG.info("Nickname: {}; Email: {}; Country: {}; Station-Id: {}",
                user.getName(), user.getUser().getEmail(), country, stationId);
        final Station station = repository.findByCountryAndId(country, stationId);
        if (station == null) {
            return new UploadResponse(UploadResponse.UploadResponseState.NOT_ENOUGH_DATA, "Station not found");
        }
        if (StringUtils.isBlank(comment)) {
            return new UploadResponse(UploadResponse.UploadResponseState.NOT_ENOUGH_DATA, "Comment is mandatory");
        }
        final Upload upload = new Upload(country, stationId, null, null, user.getUser().getId(), null, comment, true);
        final int id = uploadDao.insert(upload);
        return new UploadResponse(UploadResponse.UploadResponseState.REVIEW, id, null);
    }

    @POST
    @Path("photoUpload/queryState")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public List<UploadStateQuery> queryState(@NotNull final List<UploadStateQuery> queries, @Auth final AuthUser user) {
        LOG.info("Query uploadStatus for Nickname: {}", user.getName());

        for (final UploadStateQuery query : queries) {
            query.setState(UploadStateQuery.UploadState.UNKNOWN);
            if (query.getUploadId() != null) {
                final Upload upload = uploadDao.findById(query.getUploadId());
                if (upload != null && upload.getPhotographerId() == user.getUser().getId()) {
                    query.setRejectedReason(upload.getRejectReason());
                    query.setCountryCode(upload.getCountryCode());
                    query.setStationId(upload.getStationId());
                    query.setCoordinates(upload.getCoordinates());
                    query.setInboxUrl(upload.getInboxUrl());

                    if (upload.isDone()) {
                        if (upload.getRejectReason() == null) {
                            query.setState(UploadStateQuery.UploadState.ACCEPTED);
                        } else {
                            query.setState(UploadStateQuery.UploadState.REJECTED);
                        }
                    } else {
                        if (hasConflict(repository.findByCountryAndId(query.getCountryCode(), query.getStationId()), user.getUser().getId())) {
                            query.setState(UploadStateQuery.UploadState.CONFLICT);
                        } else {
                            query.setState(UploadStateQuery.UploadState.REVIEW);
                        }
                    }
                }
            } else {
                // legacy upload, try to find station
                final Station station = repository.findByCountryAndId(query.getCountryCode(), query.getStationId());
                if (station != null && station.hasPhoto()) {
                    if (station.getPhotographerId() == user.getUser().getId()) {
                        query.setState(UploadStateQuery.UploadState.ACCEPTED);
                    } else {
                        query.setState(UploadStateQuery.UploadState.OTHER_USER);
                    }
                }
            }
        }

        return queries;
    }

    @GET
    @RolesAllowed("ADMIN")
    @Path("photoUpload/inbox")
    @Produces(MediaType.APPLICATION_JSON)
    public List<Upload> inbox(@Auth final AuthUser user) {
        return uploadDao.findPendingUploads();
    }

    @POST
    @RolesAllowed("ADMIN")
    @Path("photoUpload/inbox")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response inboxCommand(@Auth final AuthUser user, final Upload command) {
        final Upload upload = uploadDao.findById(command.getId());
        if (upload == null || upload.isDone()) {
            throw new WebApplicationException("No pending upload found", Response.Status.BAD_REQUEST);
        }
        switch (command.getCommand()) {
            case REJECT :
                rejectUpload(upload, command.getRejectReason());
                break;
            case IMPORT :
                if (upload.isGhost()) {
                    processGhost(upload, false);
                } else {
                    importUpload(upload, command.getCountryCode(), command.getStationId(), false);
                }
                break;
            case FORCE_IMPORT :
                if (upload.isGhost()) {
                    processGhost(upload, true);
                } else {
                    importUpload(upload, command.getCountryCode(), command.getStationId(), true);
                }
                break;
            default:
                throw new WebApplicationException("Unexpected command value: " + command.getCommand(), Response.Status.BAD_REQUEST);
        }

        return Response.ok().build();
    }

    private void processGhost(final Upload upload, final boolean force) {
        final Station station = repository.findByCountryAndId(upload.getCountryCode(), upload.getStationId());
        if (station == null) {
           throw new WebApplicationException("Station not found", Response.Status.BAD_REQUEST);
        }
        if (force) {
            repository.delete(station);
        } else {
            repository.deactivate(station);
        }
        uploadDao.done(upload.getId());
    }

    private void importUpload(final Upload upload, final String countryCode, final String stationId, final boolean force) {
        final File file = getUploadFile(upload.getId(), upload.getExtension());
        LOG.info("Importing upload {}, {}", upload.getId(), file);
        boolean updateStationKey = false;

        Station station = repository.findByCountryAndId(upload.getCountryCode(), upload.getStationId());
        if (station == null) {
            station = repository.findByCountryAndId(countryCode, stationId);
            updateStationKey = true;
        }
        if (station == null) {
            if (!force) {
                throw new WebApplicationException("Station not found", Response.Status.BAD_REQUEST);
            }

            // create station
            final Optional<Country> country = countryDao.findById(StringUtils.lowerCase(countryCode));
            if (!country.isPresent()) {
                throw new WebApplicationException("Country not found", Response.Status.BAD_REQUEST);
            }
            station = new Station(new Station.Key(countryCode, stationId), upload.getTitle(), upload.getCoordinates(), null, true);
            repository.insert(station);
        }

        if (station.hasPhoto() && !force) {
            throw new WebApplicationException("Station already has a photo", Response.Status.BAD_REQUEST);
        }
        if (hasConflict(station, upload.getPhotographerId()) && !force) {
            throw new WebApplicationException("There is a conflict with another upload", Response.Status.BAD_REQUEST);
        }

        final Optional<User> user = userDao.findById(upload.getPhotographerId());
        final Optional<Country> country = countryDao.findById(StringUtils.lowerCase(station.getKey().getCountry()));
        if (!country.isPresent()) {
            throw new WebApplicationException("Country not found", Response.Status.BAD_REQUEST);
        }

        try {
            final File countryDir = new File(photoDir, station.getKey().getCountry());
            PhotoImporter.moveFile(file, countryDir, station.getKey().getId(), upload.getExtension());

            final Photo photo = PhotoImporter.createPhoto(station.getKey().getCountry(), country, station.getKey().getId(), user.get(), upload.getExtension());
            if (station.hasPhoto()) {
                photoDao.update(photo);
                FileUtils.deleteQuietly(new File(countryDir, station.getKey().getId() + "." + upload.getExtension()));
            } else {
                photoDao.insert(photo);
            }

            if (updateStationKey) {
                uploadDao.done(upload.getId(), countryCode, stationId);
            } else {
                uploadDao.done(upload.getId());
            }
        } catch (final Exception e) {
            LOG.error("Error importing upload {} photo {}", upload.getId(), file);
            throw new WebApplicationException("Error moving file: " + e.getMessage());
        }
    }

    private void rejectUpload(final Upload upload, final String rejectReason) {
        uploadDao.reject(upload.getId(), rejectReason);
        if (upload.isGhost()) {
            LOG.info("Rejecting Ghoststation report {}, {}", upload.getId(), rejectReason);
            return;
        }

        final File file = getUploadFile(upload.getId(), upload.getExtension());
        LOG.info("Rejecting upload {}, {}, {}", upload.getId(), rejectReason, file);

        try {
            final File rejectDir = new File(uploadDir, "rejected");
            FileUtils.moveFileToDirectory(file, rejectDir, true);
        } catch (final IOException e) {
            LOG.warn("Unable to move rejected file {}", file, e);
        }
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
                return consumeBodyAndReturn(body, new UploadResponse(UploadResponse.UploadResponseState.NOT_ENOUGH_DATA, "Not enough data: either 'country' and 'stationId' or 'title', 'latitude' and 'longitude' have to be provided"));
            }
            if (Math.abs(latitude) > 90 || Math.abs(longitude) > 180) {
                LOG.warn("Lat/Lon out of range: latitude={}, longitude={}", latitude, longitude);
                return consumeBodyAndReturn(body, new UploadResponse(UploadResponse.UploadResponseState.LAT_LON_OUT_OF_RANGE, "'latitude' and/or 'longitude' out of range"));
            }
            coordinates = new Coordinates(latitude, longitude);
        }

        final String extension = mimeToExtension(contentType);
        if (extension == null) {
            LOG.warn("Unknown contentType '{}'", contentType);
            return consumeBodyAndReturn(body, new UploadResponse(UploadResponse.UploadResponseState.UNSUPPORTED_CONTENT_TYPE, "unsupported content type (only jpg and png are supported)"));
        }

        final boolean duplicate = hasConflict(station, user.getUser().getId());
        File file = null;
        final String inboxUrl;
        final Integer id;
        try {
            id = uploadDao.insert(new Upload(country, stationId, stationTitle, coordinates, user.getUser().getId(), extension, comment, false));
            file = getUploadFile(id, extension);
            LOG.info("Writing photo to {}", file);

            FileUtils.forceMkdir(uploadDir);
            final long bytesRead = IOUtils.copyLarge(body, new FileOutputStream(file), 0L, MAX_SIZE);
            if (bytesRead == MAX_SIZE) {
                FileUtils.deleteQuietly(file);
                return consumeBodyAndReturn(body, new UploadResponse(UploadResponse.UploadResponseState.PHOTO_TOO_LARGE, "Photo too large, max " + MAX_SIZE + " bytes allowed"));
            }
            String duplicateInfo = "";
            if (duplicate) {
                duplicateInfo = " (possible duplicate!)";
            }
            inboxUrl = inboxBaseUrl + "/" + URIUtil.encodePath(file.getName());
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
            return consumeBodyAndReturn(body, new UploadResponse(UploadResponse.UploadResponseState.ERROR));
        }

        return new UploadResponse(duplicate ? UploadResponse.UploadResponseState.CONFLICT : UploadResponse.UploadResponseState.REVIEW, id, inboxUrl);
    }

    private File getUploadFile(final Integer id, final String extension) {
        return new File(uploadDir, String.format("%d.%s", id, extension));
    }

    private String createIFrameAnswer(final UploadResponse response, final String referer) throws JsonProcessingException {
        return "<script language=\"javascript\" type=\"text/javascript\">" +
                " window.top.window.postMessage('" + MAPPER.writeValueAsString(response) + "', '" + referer + "');" +
                "</script>";
    }

    private boolean hasConflict(final Station station, final int userId) {
        if (station == null) {
            return false;
        }
        if (station.hasPhoto() && station.getPhotographerId() != userId) {
            return true;
        }
        return uploadDao.countPendingUploadsForStationOfOtherUser(station.getKey().getCountry(), station.getKey().getId(), userId) > 0;
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
            case IMAGE_PNG:
                return "png";
            case IMAGE_JPEG:
                return "jpg";
            default:
                return null;
        }
    }

}
