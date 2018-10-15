package org.railwaystations.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.io.FileUtils;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.entity.ContentType;
import org.railwaystations.api.model.Country;
import org.railwaystations.api.model.Photo;
import org.railwaystations.api.model.User;
import org.railwaystations.api.model.Station;
import org.railwaystations.api.model.elastic.Bahnhofsfoto;
import org.railwaystations.api.monitoring.Monitor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PhotoImporter {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final Logger LOG = LoggerFactory.getLogger(PhotoImporter.class);
    private static final Pattern IMPORT_FILE_PATTERN = Pattern.compile("([^-]+)-(\\d+).jpe?g", Pattern.CASE_INSENSITIVE);
    private static final Executor EXECUTOR = Executors.newSingleThreadExecutor();

    private final StationsRepository repository;
    private final Monitor monitor;
    private final File uploadDir;
    private final File photoDir;
    private final ElasticBackend elasticBackend;
    private final String photoBaseUrl;

    public PhotoImporter(final StationsRepository repository, final Monitor monitor, final String uploadDir, final String photoDir, final ElasticBackend elasticBackend, final String photoBaseUrl) {
        this.repository = repository;
        this.monitor = monitor;
        this.uploadDir = new File(uploadDir);
        this.photoDir = new File(photoDir);
        this.elasticBackend = elasticBackend;
        this.photoBaseUrl = photoBaseUrl;
    }

    public void importPhotosAsync() {
        EXECUTOR.execute(() -> {
            final List<ReportEntry> report = importPhotos();
            monitor.sendMessage(reportToMessage(report));
            monitor.sendMessage(repository.getCountryStatisticMessage());
        });
    }

    @SuppressWarnings("PMD.AvoidInstantiatingObjectsInLoops")
    public List<ReportEntry> importPhotos() {
        LOG.info("starting import");

        final List<ReportEntry> report = new ArrayList<>();

        final File[] files = uploadDir.listFiles();
        if (files != null) {
            for (final File countryDir : files) {
                if (!countryDir.isDirectory()) {
                    LOG.info("skipping '" + countryDir + " for import");
                    continue;
                }
                final String countryCode = countryDir.getName();
                final File importDir = new File(countryDir, "import");
                if (importDir.exists()) {
                    importPhotosByCountry(importDir, countryCode, report);
                }
            }
        }

        LOG.info(report.toString());
        return report;
    }

    protected static String reportToMessage(final List<ReportEntry> report) {
        final StringBuilder builder = new StringBuilder("Imported:\n");
        report.stream().filter(e -> !e.isError()).forEach(e ->
                builder.append("- ").append(e.getPath()).append(": ").append(e.getMessage()).append('\n')
        );
        builder.append("\nErrors:\n");
        report.stream().filter(ReportEntry::isError).forEach(e ->
                builder.append("- ").append(e.getPath()).append(": ").append(e.getMessage()).append('\n')
        );
        return builder.toString();
    }

    @SuppressWarnings("PMD.AvoidInstantiatingObjectsInLoops")
    private void importPhotosByCountry(final File importDir, final String countryCode, final List<ReportEntry> report) {
        final File countryDir = new File(photoDir, countryCode);
        if (!countryDir.exists() && !countryDir.mkdirs()) {
            report.add(new ReportEntry(true, countryDir.getAbsolutePath(), "does not exist and could not be created"));
            return;
        }
        final Optional<Country> country = repository.getCountry(countryCode);
        final Collection<File> files = FileUtils.listFiles(importDir, null, false);

        final Map<File, Photo> photosToImport = new HashMap<>();
        for (final File importFile : files) {
            try {
                final Matcher matcher = IMPORT_FILE_PATTERN.matcher(importFile.getName());
                if (!matcher.find()) {
                    report.add(new ReportEntry(true, importFile.getAbsolutePath(), "File doesn't match pattern: $photographer-$stationid.jpg"));
                    continue;
                }

                final String stationId = matcher.group(2);
                final String photographerName = matcher.group(1);

                Optional<User> user = repository.getPhotographer(photographerName);
                if (!user.isPresent()) {
                    user = repository.findPhotographerByLevenshtein(photographerName);
                }
                if (!user.isPresent()) {
                    report.add(new ReportEntry(true, importFile.getAbsolutePath(), "Photographer " + photographerName + " not found"));
                    continue;
                }
                final Photo photo = new Photo(new Station.Key(countryCode, stationId), photoBaseUrl + "/fotos/" + countryCode + "/" + stationId + ".jpg", user.get(), System.currentTimeMillis(), getLicense(user.get().getLicense(), countryCode));
                photosToImport.put(importFile, photo);
            } catch (final Exception e) {
                LOG.error("Error importing photo " + importFile, e);
                report.add(new ReportEntry(true, importFile.getAbsolutePath(), "Exception: " + e.getMessage()));
            }
        }

        int importCount = 0;
        for (final Map.Entry<File, Photo> photoToImport : photosToImport.entrySet()) {
            final File importFile = photoToImport.getKey();
            final Photo photo = photoToImport.getValue();
            final Optional<StatusLine> status;
            try {
                Station station = null;
                if (country.isPresent()) {
                    station = repository.findByKey(photo.getStationKey());
                    if (station == null) {
                        report.add(new ReportEntry(true, importFile.getAbsolutePath(), "Station " + photo.getStationKey().getId() + " not found"));
                        continue;
                    }
                    if (station.hasPhoto()) {
                        report.add(new ReportEntry(true, importFile.getAbsolutePath(), "Station " + photo.getStationKey().getId() + " has already a photo"));
                        continue;
                    }
                }

                if (photosToImport.entrySet().stream().anyMatch(e -> e.getKey() != importFile && e.getValue().getStationKey().equals(photo.getStationKey()))) {
                    report.add(new ReportEntry(true, importFile.getAbsolutePath(), "conflict with another photo in inbox"));
                    continue;
                }

                status = postToElastic(new Bahnhofsfoto(photo));
                if (status.isPresent() && status.get().getStatusCode() != 201) {
                    report.add(new ReportEntry(true, importFile.getAbsolutePath(), "Elastic error response: " + status.get().toString()));
                    continue;
                }

                moveFile(importFile, countryDir, photo.getStationKey().getId());
                LOG.info("Photo " + importFile.getAbsolutePath() + " imported");
                importCount++;

                if (station != null) {
                    station.setPhoto(photo);
                }

                report.add(new ReportEntry(false, importFile.getAbsolutePath(),
                        "imported " + (station != null ? station.getTitle() : "unknown station") + " for " + photo.getPhotographer().getName() + (photo.getPhotographer().isAnonymous() ? " (anonymous)": "")));
            } catch (final Exception e) {
                LOG.error("Error importing photo " + importFile, e);
                report.add(new ReportEntry(true, importFile.getAbsolutePath(), "Exception: " + e.getMessage()));
            }
        }

        LOG.info("Imported " + importCount + " for " + countryCode);
    }

    protected Optional<StatusLine> postToElastic(final Bahnhofsfoto bahnhofsfoto) throws Exception {
        final StatusLine statusLine;
        try (final CloseableHttpResponse response = elasticBackend.post(getPhotoUrl(bahnhofsfoto.getCountryCode()), MAPPER.writeValueAsString(bahnhofsfoto), ContentType.APPLICATION_JSON)) {
            statusLine = response.getStatusLine();
        }
        return Optional.of(statusLine);
    }

    private String getLicense(final String photographerLicense, final String countryCode) {
        return "fr".equals(countryCode) ? "CC BY-NC 4.0 International" : photographerLicense;
    }

    private String getPhotoUrl(final String countryCode) {
        return "/bahnhofsfotos" + countryCode + "/bahnhofsfoto";
    }

    private void moveFile(final File importFile, final File countryDir, final String stationId) throws IOException {
        FileUtils.moveFile(importFile, new File(countryDir, stationId + ".jpg"));
    }

    public static final class ReportEntry {
        private final boolean error;
        private final String path;
        private final String message;

        public ReportEntry(final boolean error, final String path, final String message) {
            this.error = error;
            this.path = path;
            this.message = message;
        }

        public boolean isError() {
            return error;
        }

        public String getPath() {
            return path;
        }

        public String getMessage() {
            return message;
        }
    }

}
