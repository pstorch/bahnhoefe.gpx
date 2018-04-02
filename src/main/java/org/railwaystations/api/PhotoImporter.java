package org.railwaystations.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.commons.io.FileUtils;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.railwaystations.api.model.Bahnhof;
import org.railwaystations.api.model.Country;
import org.railwaystations.api.model.Photographer;
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

    private final BahnhoefeRepository repository;
    private final File uploadDir;
    private final File photoDir;
    private final BackendHttpClient httpClient;

    public PhotoImporter(final BahnhoefeRepository repository, final String uploadDir, final String photoDir) {
        this.repository = repository;
        this.uploadDir = new File(uploadDir);
        this.photoDir = new File(photoDir);
        this.httpClient = new BackendHttpClient();
    }

    @SuppressWarnings("PMD.AvoidInstantiatingObjectsInLoops")
    public Map<String, String> importPhotos() {
        LOG.info("starting import");

        final Map<String, String> report = new HashMap<>();

        final File[] files = uploadDir.listFiles();
        if (files != null) {
            for (final File countryDir : files) {
                if (!countryDir.isDirectory()) {
                    break;
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
                if (country.isPresent()) {
                    final Bahnhof station = repository.get(countryCode).get(stationId);
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

                final Photographer photographer = repository.getPhotographer(photographerName);
                if (photographer == null) {
                    report.put(importFile.getAbsolutePath(), "Photographer " + photographerName + " not found");
                    break;
                }

                final Optional<StatusLine> status = postToElastic(stationId, countryCode, photographer, flag);
                if (status.isPresent() && status.get().getStatusCode() != 201) {
                    report.put(importFile.getAbsolutePath(), "Elastic error response: " + status.get().toString());
                    break;
                }

                moveFile(importFile, countryDir, stationId);
                LOG.info("Photo " + importFile.getAbsolutePath() + " imported");
                importCount++;

                report.put(importFile.getAbsolutePath(), "imported");
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

    private Optional<StatusLine> postToElastic(final Integer stationId, final String countryCode, final Photographer photographer, final String flag) throws Exception {
        final ObjectNode fotoJson = MAPPER.createObjectNode();
        fotoJson.put("BahnhofsID", stationId);
        fotoJson.put("bahnhofsfoto", "/fotos/" + countryCode + "/" + stationId + ".jpg");
        fotoJson.put("fotolizenz", photographer.getLicense());
        fotoJson.put("fotografenname", photographer.getName());
        fotoJson.put("erfasst", System.currentTimeMillis());
        fotoJson.put("flag", flag);
        fotoJson.put("laenderkennzeichen", countryCode);
        StatusLine statusLine = null;
        try (final CloseableHttpResponse response = httpClient.post(getPhotoUrl(countryCode), fotoJson.toString())) {
            statusLine = response.getStatusLine();
        }
        return Optional.of(statusLine);
    }

    private URL getPhotoUrl(final String countryCode) throws MalformedURLException {
        // TODO: make this configurable and testable
        return new URL("http://localhost:9200/bahnhofsfotos" + countryCode + "/bahnhofsfoto");
    }

    private void moveFile(final File importFile, final File countryDir, final Integer stationId) throws IOException {
        FileUtils.moveFile(importFile, new File(countryDir, stationId + ".jpg"));
    }

}
