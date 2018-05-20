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
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PhotoImporter {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final Logger LOG = LoggerFactory.getLogger(PhotoImporter.class);
    private static final Pattern IMPORT_FILE_PATTERN = Pattern.compile("([^-]+)-(\\d+).jpg");

    private final StationsRepository repository;
    private final Monitor monitor;
    private final File uploadDir;
    private final File photoDir;
    private final BackendHttpClient httpClient;

    public PhotoImporter(final StationsRepository repository, final Monitor monitor, final String uploadDir, final String photoDir) {
        this.repository = repository;
        this.monitor = monitor;
        this.uploadDir = new File(uploadDir);
        this.photoDir = new File(photoDir);
        this.httpClient = new BackendHttpClient();
    }

    public void importPhotosAsync(final String responseUrl) {
        final Thread importer = new Thread(() -> {
            final Map<String, String> report = importPhotos();
            monitor.sendMessage(responseUrl, reportToMessage(report));
        });
        importer.start();
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
        int importCount = 0;
        final Collection<File> files = FileUtils.listFiles(importDir, new String[]{"jpg"}, false);
        if (country.isPresent() && files.size() > 0) {
            repository.refreshCountry(countryCode);
        }
        for (final File importFile : files) {
            try {
                final Matcher matcher = IMPORT_FILE_PATTERN.matcher(importFile.getName());
                if (!matcher.find()) {
                    report.put(importFile.getAbsolutePath(), "File doesn't match pattern: $photographer-$stationid.jpg");
                    break;
                }

                final Integer stationId = Integer.valueOf(matcher.group(2));
                Station station = null;
                if (country.isPresent()) {
                    station = repository.get(countryCode).get(stationId);
                    if (station == null) {
                        report.put(importFile.getAbsolutePath(), "Station " + stationId + " not found");
                        break;
                    }
                    if (station.hasPhoto()) {
                        report.put(importFile.getAbsolutePath(), "Station " + stationId + " has already a photo");
                        break;
                    }
                }

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
                    break;
                }

                final Bahnhofsfoto bahnhofsfoto = new Bahnhofsfoto(stationId, "/fotos/" + countryCode + "/" + stationId + ".jpg",
                        getLicense(photographer.getLicense(), countryCode), photographer.getName(), System.currentTimeMillis(), flag, countryCode);
                final Optional<StatusLine> status = postToElastic(bahnhofsfoto);
                if (status.isPresent() && status.get().getStatusCode() != 201) {
                    report.put(importFile.getAbsolutePath(), "Elastic error response: " + status.get().toString());
                    break;
                }

                moveFile(importFile, countryDir, stationId);
                LOG.info("Photo " + importFile.getAbsolutePath() + " imported");
                importCount++;

                report.put(importFile.getAbsolutePath(), "imported " + (station != null ? station.getTitle() : "unknown station") + " for " + photographer.getName());
            } catch (final Exception e) {
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
        try (final CloseableHttpResponse response = httpClient.post(getPhotoUrl(bahnhofsfoto.getCountryCode()), MAPPER.writeValueAsString(bahnhofsfoto), ContentType.APPLICATION_JSON)) {
            statusLine = response.getStatusLine();
        }
        return Optional.of(statusLine);
    }

    private String getLicense(final String photographerLicense, final String countryCode) {
        return "fr".equals(countryCode) ? "CC BY-NC 4.0 International" : photographerLicense;
    }

    private URL getPhotoUrl(final String countryCode) throws MalformedURLException {
        // TODO: make this configurable and testable
        return new URL("http://localhost:9200/bahnhofsfotos" + countryCode + "/bahnhofsfoto");
        //return new URL("http://localhost:9200/elastictest/bahnhofsfoto");
    }

    private void moveFile(final File importFile, final File countryDir, final Integer stationId) throws IOException {
        FileUtils.moveFile(importFile, new File(countryDir, stationId + ".jpg"));
    }

}
