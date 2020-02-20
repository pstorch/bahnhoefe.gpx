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
import org.railwaystations.api.db.InboxDao;
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
public class InboxResource {

    private static final Logger LOG = LoggerFactory.getLogger(InboxResource.class);

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static final long MAX_SIZE = 20_000_000L;
    private static final String IMAGE_PNG = "image/png";
    private static final String IMAGE_JPEG = "image/jpeg";

    private final StationsRepository repository;
    private final File inboxDir;
    private final File photoDir;
    private final Monitor monitor;
    private final UploadTokenAuthenticator authenticator;
    private final InboxDao inboxDao;
    private final UserDao userDao;
    private final CountryDao countryDao;
    private final PhotoDao photoDao;
    private final String inboxBaseUrl;
    private final File inboxToProcessDir;
    private final File inboxProcessedDir;

    public InboxResource(final StationsRepository repository, final String inboxDir,
                         final String inboxToProcessDir, final String inboxProcessedDir, final String photoDir,
                         final Monitor monitor, final UploadTokenAuthenticator authenticator,
                         final InboxDao inboxDao, final UserDao userDao, final CountryDao countryDao,
                         final PhotoDao photoDao, final String inboxBaseUrl) {
        this.repository = repository;
        this.inboxDir = new File(inboxDir);
        this.inboxToProcessDir = new File(inboxToProcessDir);
        this.inboxProcessedDir = new File(inboxProcessedDir);
        this.photoDir = new File(photoDir);
        this.monitor = monitor;
        this.authenticator = authenticator;
        this.inboxDao = inboxDao;
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
                final InboxResponse response = consumeBodyAndReturn(file, new InboxResponse(InboxResponse.InboxResponseState.UNAUTHORIZED));
                return createIFrameAnswer(response, referer);
            }

            final String contentType = MimetypesFileTypeMap.getDefaultFileTypeMap().getContentType(fd.getFileName());
            final InboxResponse response = uploadPhoto(userAgent, file, StringUtils.trimToNull(stationId), StringUtils.trimToNull(countryCode), contentType, stationTitle, latitude, longitude, comment, authUser.get());
            return createIFrameAnswer(response, referer);
        } catch (final Exception e) {
            LOG.error("FormUpload error", e);
            return createIFrameAnswer(new InboxResponse(InboxResponse.InboxResponseState.ERROR), referer);
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
        final InboxResponse inboxResponse = uploadPhoto(userAgent, body, StringUtils.trimToNull(stationId), StringUtils.trimToNull(country), contentType, stationTitle, latitude, longitude, comment, user);
        return Response.status(inboxResponse.getState().getResponseStatus()).entity(inboxResponse).build();
    }

    @POST
    @Path("reportProblem")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public InboxResponse reportProblem(@HeaderParam("User-Agent") final String userAgent,
                                       @NotNull() final ProblemReport problemReport,
                                       @Auth final AuthUser user) {
        LOG.info("New problem report: Nickname: {}; Country: {}; Station-Id: {}",
                user.getName(), problemReport.getCountryCode(), problemReport.getStationId());
        final Station station = repository.findByCountryAndId(problemReport.getCountryCode(), problemReport.getStationId());
        if (station == null) {
            return new InboxResponse(InboxResponse.InboxResponseState.NOT_ENOUGH_DATA, "Station not found");
        }
        if (StringUtils.isBlank(problemReport.getComment())) {
            return new InboxResponse(InboxResponse.InboxResponseState.NOT_ENOUGH_DATA, "Comment is mandatory");
        }
        if (problemReport.getType() == null) {
            return new InboxResponse(InboxResponse.InboxResponseState.NOT_ENOUGH_DATA, "Problem type is mandatory");
        }
        final InboxEntry inboxEntry = new InboxEntry(problemReport.getCountryCode(), problemReport.getStationId(),
                null, null, user.getUser().getId(), null, problemReport.getComment(),
                problemReport.getType());
        monitor.sendMessage(String.format("New problem report for %s%n%s: %s%nvia %s",
                station.getTitle(), problemReport.getType(), StringUtils.trimToEmpty(problemReport.getComment()), userAgent));
        return new InboxResponse(InboxResponse.InboxResponseState.REVIEW, inboxDao.insert(inboxEntry));
    }

    @POST
    @Path("userInbox")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public List<InboxStateQuery> userInbox(@NotNull final List<InboxStateQuery> queries, @Auth final AuthUser user) {
        LOG.info("Query uploadStatus for Nickname: {}", user.getName());

        for (final InboxStateQuery query : queries) {
            query.setState(InboxStateQuery.InboxState.UNKNOWN);
            if (query.getId() != null) {
                final InboxEntry inboxEntry = inboxDao.findById(query.getId());
                if (inboxEntry != null && inboxEntry.getPhotographerId() == user.getUser().getId()) {
                    query.setRejectedReason(inboxEntry.getRejectReason());
                    query.setCountryCode(inboxEntry.getCountryCode());
                    query.setStationId(inboxEntry.getStationId());
                    query.setCoordinates(inboxEntry.getCoordinates());
                    query.setFilename(inboxEntry.getFilename());
                    query.setInboxUrl(getInboxUrl(inboxEntry.getFilename(), isProcessed(inboxEntry.getFilename())));


                    if (inboxEntry.isDone()) {
                        if (inboxEntry.getRejectReason() == null) {
                            query.setState(InboxStateQuery.InboxState.ACCEPTED);
                        } else {
                            query.setState(InboxStateQuery.InboxState.REJECTED);
                        }
                    } else {
                        if (hasConflict(repository.findByCountryAndId(query.getCountryCode(), query.getStationId()), user.getUser().getId())) {
                            query.setState(InboxStateQuery.InboxState.CONFLICT);
                        } else {
                            query.setState(InboxStateQuery.InboxState.REVIEW);
                        }
                    }
                }
            } else {
                // legacy upload, try to find station
                final Station station = repository.findByCountryAndId(query.getCountryCode(), query.getStationId());
                if (station != null && station.hasPhoto()) {
                    if (station.getPhotographerId() == user.getUser().getId()) {
                        query.setState(InboxStateQuery.InboxState.ACCEPTED);
                    } else {
                        query.setState(InboxStateQuery.InboxState.OTHER_USER);
                    }
                }
            }
        }

        return queries;
    }

    @GET
    @RolesAllowed("ADMIN")
    @Path("adminInbox")
    @Produces(MediaType.APPLICATION_JSON)
    public List<InboxEntry> adminInbox(@Auth final AuthUser user) {
        final List<InboxEntry> pendingInboxEntries = inboxDao.findPendingInboxEntries();
        for (final InboxEntry inboxEntry : pendingInboxEntries) {
            final String filename = inboxEntry.getFilename();
            inboxEntry.isProcessed(isProcessed(filename));
            inboxEntry.setInboxUrl(getInboxUrl(filename, inboxEntry.isProcessed()));
        }
        return pendingInboxEntries;
    }

    private String getInboxUrl(final String filename, final boolean processed) {
        return inboxBaseUrl + (processed ? "/processed/" : "/") + filename;
    }

    private boolean isProcessed(final String filename) {
        return filename != null && new File(inboxProcessedDir, filename).exists();
    }

    @POST
    @RolesAllowed("ADMIN")
    @Path("adminInbox")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response adminInbox(@Auth final AuthUser user, final InboxEntry command) {
        final InboxEntry inboxEntry = inboxDao.findById(command.getId());
        if (inboxEntry == null || inboxEntry.isDone()) {
            throw new WebApplicationException("No pending inbox entry found", Response.Status.BAD_REQUEST);
        }
        switch (command.getCommand()) {
            case REJECT :
                rejectInboxEntry(inboxEntry, command.getRejectReason());
                break;
            case IMPORT :
                importUpload(inboxEntry, command.getCountryCode(), command.getStationId(), false);
                break;
            case FORCE_IMPORT :
                importUpload(inboxEntry, command.getCountryCode(), command.getStationId(), true);
                break;
            case DEACTIVATE_STATION:
                deactivateStation(inboxEntry);
                break;
            case DELETE_STATION:
                deleteStation(inboxEntry);
                break;
            case DELETE_PHOTO:
                deletePhoto(inboxEntry);
                break;
            case MARK_SOLVED:
                markProblemReportSolved(inboxEntry);
                break;
            default:
                throw new WebApplicationException("Unexpected command value: " + command.getCommand(), Response.Status.BAD_REQUEST);
        }

        return Response.ok().build();
    }

    @GET
    @RolesAllowed("ADMIN")
    @Path("adminInboxCount")
    @Produces(MediaType.APPLICATION_JSON)
    public InboxCountResponse adminInboxCount(@Auth final AuthUser user) {
        return new InboxCountResponse(inboxDao.countPendingInboxEntries());
    }

    private void deactivateStation(final InboxEntry inboxEntry) {
        final Station station = assertStationExists(inboxEntry);
        repository.deactivate(station);
        inboxDao.done(inboxEntry.getId());
        LOG.info("Problem report {} station {} deactivated", inboxEntry.getId(), station.getKey());
    }

    private void deleteStation(final InboxEntry inboxEntry) {
        final Station station = assertStationExists(inboxEntry);
        photoDao.delete(station.getKey());
        repository.delete(station);
        inboxDao.done(inboxEntry.getId());
        LOG.info("Problem report {} station {} deleted", inboxEntry.getId(), station.getKey());
    }

    private void deletePhoto(final InboxEntry inboxEntry) {
        final Station station = assertStationExists(inboxEntry);
        photoDao.delete(station.getKey());
        inboxDao.done(inboxEntry.getId());
        LOG.info("Problem report {} photo of station {} deleted", inboxEntry.getId(), station.getKey());
    }

    private void markProblemReportSolved(final InboxEntry inboxEntry) {
        assertStationExists(inboxEntry);
        inboxDao.done(inboxEntry.getId());
        LOG.info("Problem report {} accepted", inboxEntry.getId());
    }

    private Station assertStationExists(final InboxEntry inboxEntry) {
        final Station station = repository.findByCountryAndId(inboxEntry.getCountryCode(), inboxEntry.getStationId());
        if (station == null) {
           throw new WebApplicationException("Station not found", Response.Status.BAD_REQUEST);
        }
        return station;
    }

    private void importUpload(final InboxEntry inboxEntry, final String countryCode, final String stationId, final boolean force) {
        final File originalFile = getUploadFile(inboxEntry.getFilename());
        final File processedFile = new File(inboxProcessedDir, inboxEntry.getFilename());
        final File fileToImport = processedFile.exists() ? processedFile : originalFile;

        LOG.info("Importing upload {}, {}", inboxEntry.getId(), fileToImport);
        boolean updateStationKey = false;

        Station station = repository.findByCountryAndId(inboxEntry.getCountryCode(), inboxEntry.getStationId());
        if (station == null) {
            station = repository.findByCountryAndId(countryCode, stationId);
            updateStationKey = true;
        }
        if (station == null) {
            if (!force || StringUtils.isNotBlank(inboxEntry.getCountryCode()) || StringUtils.isNotBlank(inboxEntry.getStationId())) {
                throw new WebApplicationException("Station not found", Response.Status.BAD_REQUEST);
            }

            // create station
            final Optional<Country> country = countryDao.findById(StringUtils.lowerCase(countryCode));
            if (!country.isPresent()) {
                throw new WebApplicationException("Country not found", Response.Status.BAD_REQUEST);
            }
            if (StringUtils.isBlank(stationId)) {
                throw new WebApplicationException("Station ID can't be empty", Response.Status.BAD_REQUEST);
            }
            station = new Station(new Station.Key(countryCode, stationId), inboxEntry.getTitle(), inboxEntry.getCoordinates(), null, true);
            repository.insert(station);
        }

        if (station.hasPhoto() && !force) {
            throw new WebApplicationException("Station already has a photo", Response.Status.BAD_REQUEST);
        }
        if (hasConflict(station, inboxEntry.getPhotographerId()) && !force) {
            throw new WebApplicationException("There is a conflict with another upload", Response.Status.BAD_REQUEST);
        }

        final Optional<User> user = userDao.findById(inboxEntry.getPhotographerId());
        final Optional<Country> country = countryDao.findById(StringUtils.lowerCase(station.getKey().getCountry()));
        if (!country.isPresent()) {
            throw new WebApplicationException("Country not found", Response.Status.BAD_REQUEST);
        }

        try {
            final File countryDir = new File(photoDir, station.getKey().getCountry());
            final Photo photo = PhotoImporter.createPhoto(station.getKey().getCountry(), country, station.getKey().getId(), user.get(), inboxEntry.getExtension());
            if (station.hasPhoto()) {
                photoDao.update(photo);
                FileUtils.deleteQuietly(new File(countryDir, station.getKey().getId() + "." + inboxEntry.getExtension()));
            } else {
                photoDao.insert(photo);
            }

            if (processedFile.exists()) {
                PhotoImporter.moveFile(processedFile, countryDir, station.getKey().getId(), inboxEntry.getExtension());
            } else {
                PhotoImporter.copyFile(originalFile, countryDir, station.getKey().getId(), inboxEntry.getExtension());
            }
            FileUtils.moveFileToDirectory(originalFile, new File(inboxDir, "done"), true);

            if (updateStationKey) {
                inboxDao.done(inboxEntry.getId(), countryCode, stationId);
            } else {
                inboxDao.done(inboxEntry.getId());
            }
            LOG.info("Upload {} accepted: {}", inboxEntry.getId(), fileToImport);
        } catch (final Exception e) {
            LOG.error("Error importing upload {} photo {}", inboxEntry.getId(), fileToImport);
            throw new WebApplicationException("Error moving file: " + e.getMessage());
        }
    }

    private void rejectInboxEntry(final InboxEntry inboxEntry, final String rejectReason) {
        inboxDao.reject(inboxEntry.getId(), rejectReason);
        if (inboxEntry.getProblemReportType() != null) {
            LOG.info("Rejecting problem report {}, {}", inboxEntry.getId(), rejectReason);
            return;
        }

        final File file = getUploadFile(inboxEntry.getFilename());
        LOG.info("Rejecting upload {}, {}, {}", inboxEntry.getId(), rejectReason, file);

        try {
            final File rejectDir = new File(inboxDir, "rejected");
            FileUtils.moveFileToDirectory(file, rejectDir, true);
        } catch (final IOException e) {
            LOG.warn("Unable to move rejected file {}", file, e);
        }
    }

    private InboxResponse uploadPhoto(final String userAgent, final InputStream body, final String stationId,
                                      final String country, final String contentType, final String stationTitle,
                                      final Double latitude, final Double longitude, final String comment,
                                      final AuthUser user) {
        final Station station = repository.findByCountryAndId(country, stationId);
        Coordinates coordinates = null;
        if (station == null) {
            LOG.warn("Station not found");
            if (StringUtils.isBlank(stationTitle) || latitude == null || longitude == null) {
                LOG.warn("Not enough data for missing station: title={}, latitude={}, longitude={}", stationTitle, latitude, longitude);
                return consumeBodyAndReturn(body, new InboxResponse(InboxResponse.InboxResponseState.NOT_ENOUGH_DATA, "Not enough data: either 'country' and 'stationId' or 'title', 'latitude' and 'longitude' have to be provided"));
            }
            if (Math.abs(latitude) > 90 || Math.abs(longitude) > 180) {
                LOG.warn("Lat/Lon out of range: latitude={}, longitude={}", latitude, longitude);
                return consumeBodyAndReturn(body, new InboxResponse(InboxResponse.InboxResponseState.LAT_LON_OUT_OF_RANGE, "'latitude' and/or 'longitude' out of range"));
            }
            coordinates = new Coordinates(latitude, longitude);
        }

        final String extension = mimeToExtension(contentType);
        if (extension == null) {
            LOG.warn("Unknown contentType '{}'", contentType);
            return consumeBodyAndReturn(body, new InboxResponse(InboxResponse.InboxResponseState.UNSUPPORTED_CONTENT_TYPE, "unsupported content type (only jpg and png are supported)"));
        }

        final boolean duplicate = hasConflict(station, user.getUser().getId());
        File file = null;
        final String inboxUrl;
        final Integer id;
        try {
            id = inboxDao.insert(new InboxEntry(country, stationId, stationTitle, coordinates, user.getUser().getId(), extension, comment, null));
            file = getUploadFile(InboxEntry.getFilename(id, extension));
            LOG.info("Writing photo to {}", file);

            // write the file to the inbox directory
            FileUtils.forceMkdir(inboxDir);
            final long bytesRead = IOUtils.copyLarge(body, new FileOutputStream(file), 0L, MAX_SIZE);
            if (bytesRead == MAX_SIZE) {
                FileUtils.deleteQuietly(file);
                return consumeBodyAndReturn(body, new InboxResponse(InboxResponse.InboxResponseState.PHOTO_TOO_LARGE, "Photo too large, max " + MAX_SIZE + " bytes allowed"));
            }

            // additionally write the file to the input directory for Vsion.AI
            FileUtils.copyFileToDirectory(file, inboxToProcessDir, true);

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
            return consumeBodyAndReturn(body, new InboxResponse(InboxResponse.InboxResponseState.ERROR));
        }

        return new InboxResponse(duplicate ? InboxResponse.InboxResponseState.CONFLICT : InboxResponse.InboxResponseState.REVIEW, id, file.getName(), inboxUrl);
    }

    private File getUploadFile(final String filename) {
        return new File(inboxDir, filename);
    }

    private String createIFrameAnswer(final InboxResponse response, final String referer) throws JsonProcessingException {
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
        return inboxDao.countPendingInboxEntriesForStationOfOtherUser(station.getKey().getCountry(), station.getKey().getId(), userId) > 0;
    }

    private InboxResponse consumeBodyAndReturn(final InputStream body, final InboxResponse response) {
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

    private static class InboxCountResponse {
        private final int pendingInboxEntries;

        public InboxCountResponse(final int pendingInboxEntries) {
            this.pendingInboxEntries = pendingInboxEntries;
        }

        public int getPendingInboxEntries() {
            return pendingInboxEntries;
        }
    }

}
