package org.railwaystations.rsapi.resources;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.output.NullOutputStream;
import org.apache.commons.lang3.StringUtils;
import org.railwaystations.rsapi.MastodonBot;
import org.railwaystations.rsapi.PhotoImporter;
import org.railwaystations.rsapi.StationsRepository;
import org.railwaystations.rsapi.WorkDir;
import org.railwaystations.rsapi.auth.AuthUser;
import org.railwaystations.rsapi.auth.RSAuthenticationProvider;
import org.railwaystations.rsapi.auth.RSUserDetailsService;
import org.railwaystations.rsapi.db.CountryDao;
import org.railwaystations.rsapi.db.InboxDao;
import org.railwaystations.rsapi.db.PhotoDao;
import org.railwaystations.rsapi.model.*;
import org.railwaystations.rsapi.monitoring.Monitor;
import org.railwaystations.rsapi.utils.ImageUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.util.UriUtils;

import javax.activation.MimetypesFileTypeMap;
import javax.annotation.security.RolesAllowed;
import javax.validation.constraints.NotNull;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import java.util.zip.CRC32;
import java.util.zip.CheckedOutputStream;

@RestController
public class InboxResource {

    private static final Logger LOG = LoggerFactory.getLogger(InboxResource.class);

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static final long MAX_SIZE = 20_000_000L;

    private final StationsRepository repository;
    private final WorkDir workDir;
    private final RSAuthenticationProvider authenticator;
    private final InboxDao inboxDao;
    private final RSUserDetailsService userDetailsService;
    private final CountryDao countryDao;
    private final PhotoDao photoDao;
    private final String inboxBaseUrl;
    private final MastodonBot mastodonBot;
    private final Monitor monitor;

    public InboxResource(final StationsRepository repository, final WorkDir workDir,
                         final Monitor monitor, final RSAuthenticationProvider authenticator,
                         final InboxDao inboxDao, final RSUserDetailsService userDetailsService, final CountryDao countryDao,
                         final PhotoDao photoDao, @Value("${inboxBaseUrl}") final String inboxBaseUrl, final MastodonBot mastodonBot) {
        this.repository = repository;
        this.workDir = workDir;
        this.monitor = monitor;
        this.authenticator = authenticator;
        this.inboxDao = inboxDao;
        this.userDetailsService = userDetailsService;
        this.countryDao = countryDao;
        this.photoDao = photoDao;
        this.inboxBaseUrl = inboxBaseUrl;
        this.mastodonBot = mastodonBot;
    }

    /**
     * Not part of the "official" API.
     * Supports upload of photos via the website.
     */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE, value = "/photoUpload")
    public String photoUpload(@RequestHeader("User-Agent") final String userAgent,
                              @RequestPart("email") final String email,
                              @RequestPart("uploadToken") final String uploadToken,
                              @RequestPart("stationId") final String stationId,
                              @RequestPart("countryCode") final String countryCode,
                              @RequestPart("stationTitle") final String stationTitle,
                              @RequestPart("latitude") final Double latitude,
                              @RequestPart("longitude") final Double longitude,
                              @RequestPart("comment") final String comment,
                              @RequestPart("active") final Boolean active,
                              @RequestPart("file") final MultipartFile file,
                              @RequestHeader("Referer") final String referer) throws JsonProcessingException {
        LOG.info("MultipartFormData: email={}, station={}, country={}, file={}", email, stationId, countryCode, file.getName());

        try {
            final Authentication authentication = authenticator.authenticate(new UsernamePasswordAuthenticationToken(email, uploadToken));
            if (authentication != null && authentication.isAuthenticated()) {
                final InboxResponse response = consumeBodyAndReturn(file.getInputStream(), new InboxResponse(InboxResponse.InboxResponseState.UNAUTHORIZED));
                return createIFrameAnswer(response, referer);
            }

            final String contentType = MimetypesFileTypeMap.getDefaultFileTypeMap().getContentType(file.getName());
            final InboxResponse response = uploadPhoto(userAgent, file.getInputStream(), StringUtils.trimToNull(stationId),
                    StringUtils.trimToNull(countryCode), contentType, stationTitle, latitude, longitude, comment, active, userDetailsService.loadUserByUsername(email));
            return createIFrameAnswer(response, referer);
        } catch (final Exception e) {
            LOG.error("FormUpload error", e);
            return createIFrameAnswer(new InboxResponse(InboxResponse.InboxResponseState.ERROR), referer);
        }
    }

    @PostMapping(consumes = {MediaType.IMAGE_PNG_VALUE, MediaType.IMAGE_JPEG_VALUE}, produces = MediaType.APPLICATION_JSON_VALUE, value = "/photoUpload")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<InboxResponse> photoUpload(@RequestBody final InputStream body,
                                         @RequestHeader("User-Agent") final String userAgent,
                                         @RequestHeader("Station-Id") final String stationId,
                                         @RequestHeader("Country") final String country,
                                         @RequestHeader("Content-Type") final String contentType,
                                         @RequestHeader("Station-Title") final String encStationTitle,
                                         @RequestHeader("Latitude") final Double latitude,
                                         @RequestHeader("Longitude") final Double longitude,
                                         @RequestHeader("Comment") final String encComment,
                                         @RequestHeader("Active") final Boolean active,
                                         @AuthenticationPrincipal final AuthUser user) {
        if (!user.getUser().isEmailVerified()) {
            LOG.info("Photo upload failed for user {}, email not verified", user.getUsername());
            final InboxResponse response = consumeBodyAndReturn(body, new InboxResponse(InboxResponse.InboxResponseState.UNAUTHORIZED,"Email not verified"));
            return new ResponseEntity<>(response, HttpStatus.UNAUTHORIZED);
        }
        final String stationTitle = encStationTitle != null ? URLDecoder.decode(encStationTitle, StandardCharsets.UTF_8) : null;
        final String comment = encComment != null ? URLDecoder.decode(encComment, StandardCharsets.UTF_8) : null;
        LOG.info("Photo upload from Nickname: {}; Country: {}; Station-Id: {}; Coords: {},{}; Title: {}; Content-Type: {}",
                user.getUsername(), country, stationId, latitude, longitude, stationTitle, contentType);
        final InboxResponse inboxResponse = uploadPhoto(userAgent, body, StringUtils.trimToNull(stationId),
                StringUtils.trimToNull(country), contentType, stationTitle, latitude, longitude, comment, active, user);
        return new ResponseEntity<>(inboxResponse, inboxResponse.getState().getResponseStatus());
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE, value = "/reportProblem")
    @PreAuthorize("isAuthenticated()")
    public InboxResponse reportProblem(@RequestHeader("User-Agent") final String userAgent,
                                       @RequestBody @NotNull() final ProblemReport problemReport,
                                       @AuthenticationPrincipal final AuthUser user) {
        if (!user.getUser().isEmailVerified()) {
            LOG.info("New problem report failed for user {}, email not verified", user.getUsername());
            return new InboxResponse(InboxResponse.InboxResponseState.UNAUTHORIZED, "Email not verified");
        }

        LOG.info("New problem report: Nickname: {}; Country: {}; Station-Id: {}",
                user.getUsername(), problemReport.getCountryCode(), problemReport.getStationId());
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

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE, value = "/publicInbox")
    public List<PublicInboxEntry> publicInbox() {
        return inboxDao.findPublicInboxEntries();
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE, value = "/userInbox")
    @SuppressWarnings("PMD.UselessParentheses")
    @PreAuthorize("isAuthenticated()")
    public List<InboxStateQuery> userInbox(@AuthenticationPrincipal final AuthUser user, @RequestBody @NotNull final List<InboxStateQuery> queries) {
        LOG.info("Query uploadStatus for Nickname: {}", user.getUsername());

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

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE, value = "/adminInbox")
    @PreAuthorize("hasRole('ADMIN')")
    public List<InboxEntry> adminInbox(@AuthenticationPrincipal final AuthUser user) {
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
        return filename != null && new File(workDir.getInboxProcessedDir(), filename).exists();
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, value = "/adminInbox")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> adminInbox(@AuthenticationPrincipal final AuthUser user, @RequestBody final InboxEntry command) {
        final InboxEntry inboxEntry = inboxDao.findById(command.getId());
        if (inboxEntry == null || inboxEntry.isDone()) {
            return new ResponseEntity<>("No pending inbox entry found", HttpStatus.BAD_REQUEST);
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
                    return new ResponseEntity<>("Empty new title: " + command.getTitle(), HttpStatus.BAD_REQUEST);
                }
                changeStationTitle(inboxEntry, command.getTitle());
                break;
            case UPDATE_LOCATION:
                updateLocation(inboxEntry, command);
                break;
            default:
                return new ResponseEntity<>("Unexpected command value: " + command.getCommand(), HttpStatus.BAD_REQUEST);
        }

        return new ResponseEntity<>(HttpStatus.OK);
    }

    private void updateLocation(final InboxEntry inboxEntry, final InboxEntry command) {
        Coordinates coordinates = inboxEntry.getCoordinates();
        if (command.hasCoords()) {
            coordinates = command.getCoordinates();
        }
        if (coordinates == null || !coordinates.isValid()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Can't update location, coordinates: " + command.getCommand());
        }

        final Station station = assertStationExists(inboxEntry);
        repository.updateLocation(station, coordinates);
        inboxDao.done(inboxEntry.getId());
    }

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE, value = "/adminInboxCount")
    @PreAuthorize("hasRole('ADMIN')")
    public InboxCountResponse adminInboxCount(@AuthenticationPrincipal final AuthUser user) {
        return new InboxCountResponse(inboxDao.countPendingInboxEntries());
    }

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE, value = "/nextZ")
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
           throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Station not found");
        }
        return station;
    }

    private void importUpload(final InboxEntry inboxEntry, final InboxEntry command) {
        final File originalFile = getUploadFile(inboxEntry.getFilename());
        final File processedFile = new File(workDir.getInboxProcessedDir(), inboxEntry.getFilename());
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
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Station not found");
            }

            // create station
            final Optional<Country> country = countryDao.findById(StringUtils.lowerCase(command.getCountryCode()));
            if (country.isEmpty()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Country not found");
            }
            if (StringUtils.isBlank(command.getStationId())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Station ID can't be empty");
            }
            if (hasConflict(inboxEntry.getId(), inboxEntry.getCoordinates()) && !command.ignoreConflict()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "There is a conflict with a nearby station");
            }
            if (command.hasCoords() && !command.getCoordinates().isValid()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Lat/Lon out of range");
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
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Station already has a photo");
        }
        if (hasConflict(inboxEntry.getId(), station) && !command.ignoreConflict()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "There is a conflict with another upload");
        }

        final Optional<User> user = userDetailsService.findById(inboxEntry.getPhotographerId());
        final Optional<Country> country = countryDao.findById(StringUtils.lowerCase(station.getKey().getCountry()));
        if (country.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Country not found");
        }

        try {
            final File countryDir = new File(workDir.getPhotosDir(), station.getKey().getCountry());
            final Photo photo = PhotoImporter.createPhoto(station.getKey().getCountry(), country.orElse(null), station.getKey().getId(), user.get(), inboxEntry.getExtension());
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
            FileUtils.moveFileToDirectory(originalFile, new File(workDir.getInboxDir(), "done"), true);
            inboxDao.done(inboxEntry.getId());
            LOG.info("Upload {} accepted: {}", inboxEntry.getId(), fileToImport);
            mastodonBot.tootNewPhoto(repository.findByKey(station.getKey()), inboxEntry);
        } catch (final Exception e) {
            LOG.error("Error importing upload {} photo {}", inboxEntry.getId(), fileToImport);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error moving file: " + e.getMessage());
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
            final File rejectDir = new File(workDir.getInboxDir(), "rejected");
            FileUtils.moveFileToDirectory(file, rejectDir, true);
            FileUtils.deleteQuietly(new File(workDir.getInboxToProcessDir(), inboxEntry.getFilename()));
            FileUtils.deleteQuietly(new File(workDir.getInboxProcessedDir(), inboxEntry.getFilename()));
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

        final String extension = ImageUtil.mimeToExtension(contentType);
        if (extension == null) {
            LOG.warn("Unknown contentType '{}'", contentType);
            return consumeBodyAndReturn(body, new InboxResponse(InboxResponse.InboxResponseState.UNSUPPORTED_CONTENT_TYPE, "unsupported content type (only jpg and png are supported)"));
        }

        final boolean conflict = hasConflict(null, station) || hasConflict(null, coordinates);
        File file = null;
        final String inboxUrl;
        final Integer id;
        final Long crc32;
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
            FileUtils.forceMkdir(workDir.getInboxDir());
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
            FileUtils.copyFileToDirectory(file, workDir.getInboxToProcessDir(), true);

            String duplicateInfo = "";
            if (conflict) {
                duplicateInfo = " (possible duplicate!)";
            }
            inboxUrl = inboxBaseUrl + "/" + UriUtils.encodePath(file.getName(), StandardCharsets.UTF_8);
            if (station != null) {
                monitor.sendMessage(String.format("New photo upload for %s - %s:%s%n%s%n%s%s%nby %s%nvia %s",
                        station.getTitle(), station.getKey().getCountry(), station.getKey().getId(),
                        StringUtils.trimToEmpty(comment), inboxUrl, duplicateInfo, user.getUser().getName(), userAgent), file);
            } else {
                monitor.sendMessage(String.format("Photo upload for missing station %s at https://map.railway-stations.org/index.php?mlat=%s&mlon=%s&zoom=18&layers=M%n%s%n%s%s%nby %s%nvia %s",
                        stationTitle, latitude, longitude,
                        StringUtils.trimToEmpty(comment), inboxUrl, duplicateInfo, user.getUser().getName(), userAgent), file);
            }
        } catch (final IOException e) {
            LOG.error("Error copying the uploaded file to {}", file, e);
            return consumeBodyAndReturn(body, new InboxResponse(InboxResponse.InboxResponseState.ERROR));
        }

        return new InboxResponse(conflict ? InboxResponse.InboxResponseState.CONFLICT : InboxResponse.InboxResponseState.REVIEW, id, file.getName(), inboxUrl, crc32);
    }

    private File getUploadFile(final String filename) {
        return new File(workDir.getInboxDir(), filename);
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
            try {
                IOUtils.copy(body, NullOutputStream.NULL_OUTPUT_STREAM);
            } catch (final IOException e) {
                LOG.warn("Unable to consume body", e);
            }
        }
        return response;
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
