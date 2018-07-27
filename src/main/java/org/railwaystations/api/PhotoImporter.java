package org.railwaystations.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.io.FileUtils;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.entity.ContentType;
import org.railwaystations.api.model.Country;
import org.railwaystations.api.model.Photographer;
import org.railwaystations.api.model.Station;
import org.railwaystations.api.model.elastic.Bahnhofsfoto;
import org.railwaystations.api.monitoring.Monitor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
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

    public PhotoImporter(final StationsRepository repository, final Monitor monitor, final String uploadDir, final String photoDir, final ElasticBackend elasticBackend) {
        this.repository = repository;
        this.monitor = monitor;
        this.uploadDir = new File(uploadDir);
        this.photoDir = new File(photoDir);
        this.elasticBackend = elasticBackend;
    }

    public void importPhotosAsync() {
        EXECUTOR.execute(() -> {
            final Map<String, String> report = importPhotos();
            monitor.sendMessage(reportToMessage(report));
        });
    }

    @SuppressWarnings("PMD.AvoidInstantiatingObjectsInLoops")
    public Map<String, String> importPhotos() {
        LOG.info("starting import");

        final Map<String, String> report = new HashMap<>();

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

    public String reportToMessage(final Map<String, String> report) {
        final StringBuilder builder = new StringBuilder("Imported:\n");
        for (final Map.Entry<String, String> entry : report.entrySet()) {
            builder.append("- ").append(entry.getKey()).append(": ").append(entry.getValue()).append('\n');
        }
        return builder.toString();
    }

    @SuppressWarnings("PMD.AvoidInstantiatingObjectsInLoops")
    private void importPhotosByCountry(final File importDir, final String countryCode, final Map<String, String> report) {
        final File countryDir = new File(photoDir, countryCode);
        if (!countryDir.exists() && !countryDir.mkdirs()) {
            report.put(countryDir.getAbsolutePath(), "does not exist and could not be created");
            return;
        }
        final Optional<Country> country = repository.getCountry(countryCode);
        final Collection<File> files = FileUtils.listFiles(importDir, null, false);
        if (country.isPresent() && files.size() > 0) {
            repository.refreshCountry(countryCode);
        }

        final Map<File, Bahnhofsfoto> fotosToImport = new HashMap<>();
        for (final File importFile : files) {
            try {
                final Matcher matcher = IMPORT_FILE_PATTERN.matcher(importFile.getName());
                if (!matcher.find()) {
                    report.put(importFile.getAbsolutePath(), "File doesn't match pattern: $photographer-$stationid.jpg");
                    continue;
                }

                final String stationId = matcher.group(2);

                String photographerName = matcher.group(1);
                final String flag = photographerName.equals("@RecumbentTravel") ? "1" : "0";
                if (!flag.equals("0")) {
                    photographerName = "Anonym";
                }

                Photographer photographer = repository.getPhotographer(photographerName);
                if (photographer == null) {
                    photographer = repository.findPhotographerByLevenshtein(photographerName).orElse(null);
                }
                if (photographer == null) {
                    report.put(importFile.getAbsolutePath(), "Photographer " + photographerName + " not found");
                    continue;
                }

                final Bahnhofsfoto bahnhofsfoto = new Bahnhofsfoto(stationId, "/fotos/" + countryCode + "/" + stationId + ".jpg",
                        getLicense(photographer.getLicense(), countryCode), photographer.getName(), System.currentTimeMillis(), flag, countryCode);
                fotosToImport.put(importFile, bahnhofsfoto);
            } catch (final Exception e) {
                LOG.error("Error importing photo " + importFile, e);
                report.put(importFile.getAbsolutePath(), "Import Error: " + e.getMessage());
            }
        }

        int importCount = 0;
        for (final Map.Entry<File, Bahnhofsfoto> fotoToImport : fotosToImport.entrySet()) {
            final File importFile = fotoToImport.getKey();
            final Bahnhofsfoto bahnhofsfoto = fotoToImport.getValue();
            final Optional<StatusLine> status;
            try {
                final Station.Key key = new Station.Key(countryCode, bahnhofsfoto.getId());
                Station station = null;
                if (country.isPresent()) {
                    station = repository.get(countryCode).get(key);
                    if (station == null) {
                        report.put(importFile.getAbsolutePath(), "Station " + key.getId() + " not found");
                        continue;
                    }
                    if (station.hasPhoto()) {
                        report.put(importFile.getAbsolutePath(), "Station " + key.getId() + " has already a photo");
                        continue;
                    }
                }

                if (fotosToImport.entrySet().stream().anyMatch(e -> e.getKey() != importFile && e.getValue().getId().equals(bahnhofsfoto.getId()))) {
                    report.put(importFile.getAbsolutePath(), "conflict with another photo in inbox");
                    continue;
                }

                status = postToElastic(bahnhofsfoto);
                if (status.isPresent() && status.get().getStatusCode() != 201) {
                    report.put(importFile.getAbsolutePath(), "Elastic error response: " + status.get().toString());
                    continue;
                }

                moveFile(importFile, countryDir, bahnhofsfoto.getId());
                LOG.info("Photo " + importFile.getAbsolutePath() + " imported");
                importCount++;

                report.put(importFile.getAbsolutePath(), "imported " + (station != null ? station.getTitle() : "unknown station") + " for " + bahnhofsfoto.getPhotographer());
            } catch (Exception e) {
                LOG.error("Error importing photo " + importFile, e);
                report.put(importFile.getAbsolutePath(), "Import Error: " + e.getMessage());
            }
        }

        if (importCount > 0) {
            LOG.info("Imported " + importCount + " for " + countryCode);
            repository.refreshCountry(countryCode);
        }
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

}
