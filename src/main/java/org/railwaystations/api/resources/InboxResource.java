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
import org.railwaystations.api.MastodonBot;
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
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import java.util.zip.CRC32;
import java.util.zip.CheckedOutputStream;

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
    private final MastodonBot mastodonBot;

    public InboxResource(final StationsRepository repository, final String inboxDir,
                         final String inboxToProcessDir, final String inboxProcessedDir, final String photoDir,
                         final Monitor monitor, final UploadTokenAuthenticator authenticator,
                         final InboxDao inboxDao, final UserDao userDao, final CountryDao countryDao,
                         final PhotoDao photoDao, final String inboxBaseUrl, final MastodonBot mastodonBot) {
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
        this.mastodonBot = mastodonBot;
    }

    /**
     * Not part of the "official" API.
     * Supports upload of photos via the website.
     */
    @POST
    @Path("photoUpload")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    public String photoUpload(@HeaderParam("User-Agent") final String userAgent,
                              @FormDataParam("email") final String email,
                              @FormDataParam("uploadToken") final String uploadToken,
                              @FormDataParam("stationId") final String stationId,
                              @FormDataParam("countryCode") final String countryCode,
                              @FormDataParam("stationTitle") final String stationTitle,
                              @FormDataParam("latitude") final Double latitude,
                              @FormDataParam("longitude") final Double longitude,
                              @FormDataParam("comment") final String comment,
                              @FormDataParam("active") final Boolean active,
                              @FormDataParam("file") final InputStream file,
                              @FormDataParam("file") final FormDataContentDisposition fd,
                              @HeaderParam("Referer") final String referer) throws JsonProcessingException {
        LOG.info("MultipartFormData: email={}, station={}, country={}, file={}", email, stationId, countryCode, fd.getFileName());

        try {
            final Optional<AuthUser> authUser = authenticator.authenticate(new UploadTokenCredentials(email, uploadToken));
            if (authUser.isEmpty() || !authUser.get().getUser().isEmailVerified()) {
                final InboxResponse response = consumeBodyAndReturn(file, new InboxResponse(InboxResponse.InboxResponseState.UNAUTHORIZED));
                return createIFrameAnswer(response, referer);
            }

            final String contentType = MimetypesFileTypeMap.getDefaultFileTypeMap().getContentType(fd.getFileName());
            final InboxResponse response = uploadPhoto(userAgent, file, StringUtils.trimToNull(stationId),
                    StringUtils.trimToNull(countryCode), contentType, stationTitle, latitude, longitude, comment, active, authUser.get());
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
    public Response photoUpload(final InputStream body,
                                @HeaderParam("User-Agent") final String userAgent,
                                @HeaderParam("Station-Id") final String stationId,
                                @HeaderParam("Country") final String country,
                                @HeaderParam("Content-Type") final String contentType,
                                @HeaderParam("Station-Title") final String encStationTitle,
                                @HeaderParam("Latitude") final Double latitude,
                                @HeaderParam("Longitude") final Double longitude,
                                @HeaderParam("Comment") final String encComment,
                                @HeaderParam("Active") final Boolean active,
                                @Auth final AuthUser user) {
        if (!user.getUser().isEmailVerified()) {
            LOG.info("Photo upload failed for user {}, email not verified", user.getName());
            final InboxResponse response = consumeBodyAndReturn(body, new InboxResponse(InboxResponse.InboxResponseState.UNAUTHORIZED,"Email not verified"));
            return Response.status(Response.Status.UNAUTHORIZED).entity(response).build();
        }
        final String stationTitle = encStationTitle != null ? URLDecoder.decode(encStationTitle, StandardCharsets.UTF_8) : null;
        final String comment = encComment != null ? URLDecoder.decode(encComment, StandardCharsets.UTF_8) : null;
        LOG.info("Photo upload from Nickname: {}; Country: {}; Station-Id: {}; Coords: {},{}; Title: {}; Content-Type: {}",
                user.getName(), country, stationId, latitude, longitude, stationTitle, contentType);
        final InboxResponse inboxResponse = uploadPhoto(userAgent, body, StringUtils.trimToNull(stationId),
                StringUtils.trimToNull(country), contentType, stationTitle, latitude, longitude, comment, active, user);
        return Response.status(inboxResponse.getState().getResponseStatus()).entity(inboxResponse).build();
    }

    @POST
    @Path("reportProblem")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public InboxResponse reportProblem(@HeaderParam("User-Agent") final String userAgent,
                                       @NotNull() final ProblemReport problemReport,
                                       @Auth final AuthUser user) {
        if (!user.getUser().isEmailVerified()) {
            LOG.info("New problem report failed for user {}, email not verified", user.getName());
            return new InboxResponse(InboxResponse.InboxResponseState.UNAUTHORIZED, "Email not verified");
        }

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
                null, problemReport.getCoordinates(), user.getUser().getId(), null, problemReport.getComment(),
                problemReport.getType(), null);
        monitor.sendMessage(String.format("New problem report for %s - %s:%s%n%s: %s%nby %s%nvia %s",
                station.getTitle(), station.getKey().getCountry(), station.getKey().getId(), problemReport.getType(),
                StringUtils.trimToEmpty(problemReport.getComment()), user.getUser().getName(), userAgent));
        return new InboxResponse(InboxResponse.InboxResponseState.REVIEW, inboxDao.insert(inboxEntry));
    }

    @GET
    @Path("publicInbox")
    @Produces(MediaType.APPLICATION_JSON)
    public List<PublicInboxEntry> publicInbox() {
        return inboxDao.findPublicInboxEntries();
    }

    @POST
    @Path("userInbox")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @SuppressWarnings("PMD.UselessParentheses")
    public List<InboxStateQuery> userInbox(@Auth final AuthUser user, @NotNull final List<InboxStateQuery> queries) {
        LOG.info("Query uploadStatus for Nickname: {}", user.getName());

        for (final InboxStateQuery query : queries) {
            query.setState(InboxStateQuery.InboxState.UNKNOWN);
            InboxEntry inboxEntry = null;
            if (query.getId() != null) {
                inboxEntry = inboxDao.findById(query.getId());
            } else if (query.getCountryCode() != null && query.getStationId() != null) {
                inboxEntry = inboxDao.findNewestPendingByCountryAndStationIdAndPhotographerId(query.getCountryCode(), query.getStationId(), user.getUser().getId());
            }

            if (inboxEntry != null && inboxEntry.getPhotographerId() == user.getUser().getId()) {
                query.setId(inboxEntry.getId());
                query.setRejectedReason(inboxEntry.getRejectReason());
                query.setCountryCode(inboxEntry.getCountryCode());
                query.setStationId(inboxEntry.getStationId());
                query.setCoordinates(inboxEntry.getCoordinates());
                query.setFilename(inboxEntry.getFilename());
                query.setInboxUrl(getInboxUrl(inboxEntry.getFilename(), isProcessed(inboxEntry.getFilename())));
                query.setCrc32(inboxEntry.getCrc32());


                if (inboxEntry.isDone()) {
                    if (inboxEntry.getRejectReason() == null) {
                        query.setState(InboxStateQuery.InboxState.ACCEPTED);
                    } else {
                        query.setState(InboxStateQuery.InboxState.REJECTED);
                    }
                } else {
                    if (hasConflict(inboxEntry.getId(),
                            repository.findByCountryAndId(query.getCountryCode(), query.getStationId()))
                            || (inboxEntry.getStationId() == null && hasConflict(inboxEntry.getId(), inboxEntry.getCoordinates()))) {
                        query.setState(InboxStateQuery.InboxState.CONFLICT);
                    } else {
                        query.setState(InboxStateQuery.InboxState.REVIEW);
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
            if (!inboxEntry.isProblemReport()) {
                inboxEntry.setInboxUrl(getInboxUrl(filename, inboxEntry.isProcessed()));
            }
            if (inboxEntry.getStationId() == null && !inboxEntry.getCoordinates().hasZeroCoords()) {
                inboxEntry.setConflict(hasConflict(inboxEntry.getId(), inboxEntry.getCoordinates()));
            }
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
                importUpload(inboxEntry, command);
                break;
            case ACTIVATE_STATION:
                updateStationActiveState(inboxEntry, true);
                break;
            case DEACTIVATE_STATION:
                updateStationActiveState(inboxEntry, false);
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
            case CHANGE_NAME:
                if (StringUtils.isBlank(command.getTitle())) {
                    throw new WebApplicationException("Empty new title: " + command.getCommand(), Response.Status.BAD_REQUEST);
                }
                changeStationTitle(inboxEntry, command.getTitle());
                break;
            case UPDATE_LOCATION:
                updateLocation(inboxEntry, command);
                break;
            default:
                throw new WebApplicationException("Unexpected command value: " + command.getCommand(), Response.Status.BAD_REQUEST);
        }

        return Response.ok().build();
    }

    private void updateLocation(final InboxEntry inboxEntry, final InboxEntry command) {
        Coordinates coordinates = inboxEntry.getCoordinates();
        if (command.hasCoords()) {
            coordinates = command.getCoordinates();
        }
        if (coordinates == null || !coordinates.isValid()) {
            throw new WebApplicationException("Can't update location, coordinates: " + command.getCommand(), Response.Status.BAD_REQUEST);
        }

        final Station station = assertStationExists(inboxEntry);
        repository.updateLocation(station, coordinates);
        inboxDao.done(inboxEntry.getId());
    }

    @GET
    @RolesAllowed("ADMIN")
    @Path("adminInboxCount")
    @Produces(MediaType.APPLICATION_JSON)
    public InboxCountResponse adminInboxCount(@Auth final AuthUser user) {
        return new InboxCountResponse(inboxDao.countPendingInboxEntries());
    }

    @GET
    @Path("nextZ")
    @Produces(MediaType.APPLICATION_JSON)
    public NextZResponse nextZ() {
        return new NextZResponse(repository.getNextZ());
    }

    private void updateStationActiveState(final InboxEntry inboxEntry, final boolean active) {
        final Station station = assertStationExists(inboxEntry);
        station.setActive(active);
        repository.updateActive(station);
        inboxDao.done(inboxEntry.getId());
        LOG.info("Problem report {} station {} set active to {}", inboxEntry.getId(), station.getKey(), active);
    }

    private void changeStationTitle(final InboxEntry inboxEntry, final String newTitle) {
        final Station station = assertStationExists(inboxEntry);
        repository.changeStationTitle(station, newTitle);
        inboxDao.done(inboxEntry.getId());
        LOG.info("Problem report {} station {} change name to {}", inboxEntry.getId(), station.getKey(), newTitle);
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

    private void importUpload(final InboxEntry inboxEntry, final InboxEntry command) {
        final File originalFile = getUploadFile(inboxEntry.getFilename());
        final File processedFile = new File(inboxProcessedDir, inboxEntry.getFilename());
        final File fileToImport = processedFile.exists() ? processedFile : originalFile;

        LOG.info("Importing upload {}, {}", inboxEntry.getId(), fileToImport);

        Station station = repository.findByCountryAndId(inboxEntry.getCountryCode(), inboxEntry.getStationId());
        if (station == null && command.createStation()) {
            station = repository.findByCountryAndId(command.getCountryCode(), command.getStationId());
            if (station != null) {
                LOG.info("Importing missing station upload {} to existing station {}", inboxEntry.getId(), station.getKey());
            }
        }
        if (station == null) {
            if (!command.createStation() || StringUtils.isNotBlank(inboxEntry.getStationId())) {
                throw new WebApplicationException("Station not found", Response.Status.BAD_REQUEST);
            }

            // create station
            final Optional<Country> country = countryDao.findById(StringUtils.lowerCase(command.getCountryCode()));
            if (country.isEmpty()) {
                throw new WebApplicationException("Country not found", Response.Status.BAD_REQUEST);
            }
            if (StringUtils.isBlank(command.getStationId())) {
                throw new WebApplicationException("Station ID can't be empty", Response.Status.BAD_REQUEST);
            }
            if (hasConflict(inboxEntry.getId(), inboxEntry.getCoordinates()) && !command.ignoreConflict()) {
                throw new WebApplicationException("There is a conflict with a nearby station", Response.Status.BAD_REQUEST);
            }
            if (command.hasCoords() && !command.getCoordinates().isValid()) {
                throw new WebApplicationException("Lat/Lon out of range", Response.Status.BAD_REQUEST);
            }

            Coordinates coordinates = inboxEntry.getCoordinates();
            if (command.hasCoords()) {
                coordinates = command.getCoordinates();
            }

            final String title = command.getTitle() != null ? command.getTitle() : inboxEntry.getTitle();

            station = new Station(new Station.Key(command.getCountryCode(), command.getStationId()), title, coordinates, command.getDS100(), null, command.getActive());
            repository.insert(station);
        }

        if (station.hasPhoto() && !command.ignoreConflict()) {
            throw new WebApplicationException("Station already has a photo", Response.Status.BAD_REQUEST);
        }
        if (hasConflict(inboxEntry.getId(), station) && !command.ignoreConflict()) {
            throw new WebApplicationException("There is a conflict with another upload", Response.Status.BAD_REQUEST);
        }

        final Optional<User> user = userDao.findById(inboxEntry.getPhotographerId());
        final Optional<Country> country = countryDao.findById(StringUtils.lowerCase(station.getKey().getCountry()));
        if (country.isEmpty()) {
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
            station.setPhoto(photo);

            if (processedFile.exists()) {
                PhotoImporter.moveFile(processedFile, countryDir, station.getKey().getId(), inboxEntry.getExtension());
            } else {
                PhotoImporter.copyFile(originalFile, countryDir, station.getKey().getId(), inboxEntry.getExtension());
            }
            FileUtils.moveFileToDirectory(originalFile, new File(inboxDir, "done"), true);
            inboxDao.done(inboxEntry.getId());
            LOG.info("Upload {} accepted: {}", inboxEntry.getId(), fileToImport);
            mastodonBot.tootNewPhoto(repository.findByKey(station.getKey()), inboxEntry);
        } catch (final Exception e) {
            LOG.error("Error importing upload {} photo {}", inboxEntry.getId(), fileToImport);
            throw new WebApplicationException("Error moving file: " + e.getMessage());
        }
    }

    private void rejectInboxEntry(final InboxEntry inboxEntry, final String rejectReason) {
        inboxDao.reject(inboxEntry.getId(), rejectReason);
        if (inboxEntry.isProblemReport()) {
            LOG.info("Rejecting problem report {}, {}", inboxEntry.getId(), rejectReason);
            return;
        }

        final File file = getUploadFile(inboxEntry.getFilename());
        LOG.info("Rejecting upload {}, {}, {}", inboxEntry.getId(), rejectReason, file);

        try {
            final File rejectDir = new File(inboxDir, "rejected");
            FileUtils.moveFileToDirectory(file, rejectDir, true);
            FileUtils.deleteQuietly(new File(inboxToProcessDir, inboxEntry.getFilename()));
            FileUtils.deleteQuietly(new File(inboxProcessedDir, inboxEntry.getFilename()));
        } catch (final IOException e) {
            LOG.warn("Unable to move rejected file {}", file, e);
        }
    }

    private InboxResponse uploadPhoto(final String userAgent, final InputStream body, final String stationId,
                                      final String country, final String contentType, final String stationTitle,
                                      final Double latitude, final Double longitude, final String comment,
                                      final Boolean active, final AuthUser user) {
        final Station station = repository.findByCountryAndId(country, stationId);
        Coordinates coordinates = null;
        if (station == null) {
            LOG.warn("Station not found");
            if (StringUtils.isBlank(stationTitle) || latitude == null || longitude == null) {
                LOG.warn("Not enough data for missing station: title={}, latitude={}, longitude={}", stationTitle, latitude, longitude);
                return consumeBodyAndReturn(body, new InboxResponse(InboxResponse.InboxResponseState.NOT_ENOUGH_DATA, "Not enough data: either 'country' and 'stationId' or 'title', 'latitude' and 'longitude' have to be provided"));
            }
            coordinates = new Coordinates(latitude, longitude);
            if (!coordinates.isValid()) {
                LOG.warn("Lat/Lon out of range: latitude={}, longitude={}", latitude, longitude);
                return consumeBodyAndReturn(body, new InboxResponse(InboxResponse.InboxResponseState.LAT_LON_OUT_OF_RANGE, "'latitude' and/or 'longitude' out of range"));
            }
        }

        final String extension = mimeToExtension(contentType);
        if (extension == null) {
            LOG.warn("Unknown contentType '{}'", contentType);
            return consumeBodyAndReturn(body, new InboxResponse(InboxResponse.InboxResponseState.UNSUPPORTED_CONTENT_TYPE, "unsupported content type (only jpg and png are supported)"));
        }

        final boolean conflict = hasConflict(null, station) || hasConflict(null, coordinates);
        File file = null;
        final String inboxUrl;
        final Integer id;
        Long crc32 = null;
        try {
            if (station != null) {
                // existing station
                id = inboxDao.insert(new InboxEntry(station.getKey().getCountry(), station.getKey().getId(), stationTitle,
                        null, user.getUser().getId(), extension, comment, null, active));
            } else {
                // missing station
                id = inboxDao.insert(new InboxEntry(country, null, stationTitle,
                        coordinates, user.getUser().getId(), extension, comment, null, active));
            }
            file = getUploadFile(InboxEntry.getFilename(id, extension));
            LOG.info("Writing photo to {}", file);

            // write the file to the inbox directory
            FileUtils.forceMkdir(inboxDir);
            final CheckedOutputStream cos = new CheckedOutputStream(new FileOutputStream(file), new CRC32());
            final long bytesRead = IOUtils.copyLarge(body, cos, 0L, MAX_SIZE);
            if (bytesRead == MAX_SIZE) {
                FileUtils.deleteQuietly(file);
                return consumeBodyAndReturn(body, new InboxResponse(InboxResponse.InboxResponseState.PHOTO_TOO_LARGE, "Photo too large, max " + MAX_SIZE + " bytes allowed"));
            }
            cos.close();
            crc32 = cos.getChecksum().getValue();
            inboxDao.updateCrc32(id, crc32);

            // additionally write the file to the input directory for Vsion.AI
            FileUtils.copyFileToDirectory(file, inboxToProcessDir, true);

            String duplicateInfo = "";
            if (conflict) {
                duplicateInfo = " (possible duplicate!)";
            }
            inboxUrl = inboxBaseUrl + "/" + URIUtil.encodePath(file.getName());
            if (station != null) {
                monitor.sendMessage(String.format("New photo upload for %s - %s:%s%n%s%n%s%s%nby %s%nvia %s",
                        station.getTitle(), station.getKey().getCountry(), station.getKey().getId(),
                        StringUtils.trimToEmpty(comment), inboxUrl, duplicateInfo, user.getUser().getName(), userAgent));
            } else {
                monitor.sendMessage(String.format("Photo upload for missing station %s at https://map.railway-stations.org/index.php?mlat=%s&mlon=%s&zoom=18&layers=M%n%s%n%s%s%nby %s%nvia %s",
                        stationTitle, latitude, longitude,
                        StringUtils.trimToEmpty(comment), inboxUrl, duplicateInfo, user.getUser().getName(), userAgent));
            }
        } catch (final IOException e) {
            LOG.error("Error copying the uploaded file to {}", file, e);
            return consumeBodyAndReturn(body, new InboxResponse(InboxResponse.InboxResponseState.ERROR));
        }

        return new InboxResponse(conflict ? InboxResponse.InboxResponseState.CONFLICT : InboxResponse.InboxResponseState.REVIEW, id, file.getName(), inboxUrl, crc32);
    }

    private File getUploadFile(final String filename) {
        return new File(inboxDir, filename);
    }

    private String createIFrameAnswer(final InboxResponse response, final String referer) throws JsonProcessingException {
        return "<script language=\"javascript\" type=\"text/javascript\">" +
                " window.top.window.postMessage('" + MAPPER.writeValueAsString(response) + "', '" + referer + "');" +
                "</script>";
    }

    private boolean hasConflict(final Integer id, final Station station) {
        if (station == null) {
            return false;
        }
        if (station.hasPhoto()) {
            return true;
        }
        return inboxDao.countPendingInboxEntriesForStation(id, station.getKey().getCountry(), station.getKey().getId()) > 0;
    }

    private boolean hasConflict(final Integer id, final Coordinates coordinates) {
        if (coordinates == null || coordinates.hasZeroCoords()) {
            return false;
        }
        return inboxDao.countPendingInboxEntriesForNearbyCoordinates(id, coordinates) > 0 || repository.countNearbyCoordinates(coordinates) > 0;
    }

    private InboxResponse consumeBodyAndReturn(final InputStream body, final InboxResponse response) {
        if (body != null) {
            final InputStreamEntity inputStreamEntity = new InputStreamEntity(body);
            try {
                inputStreamEntity.writeTo(NullOutputStream.NULL_OUTPUT_STREAM);
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

    private static class NextZResponse {
        private final String nextZ;

        public NextZResponse(final String nextZ) {
            this.nextZ = nextZ;
        }

        public String getNextZ() {
            return nextZ;
        }

    }

}
