package org.railwaystations.api;

import org.apache.commons.io.FileUtils;
import org.apache.http.ProtocolVersion;
import org.apache.http.StatusLine;
import org.apache.http.message.BasicStatusLine;
import org.junit.jupiter.api.BeforeEach;
import org.railwaystations.api.loader.PhotographerLoader;
import org.railwaystations.api.loader.StationLoaderDe;
import org.railwaystations.api.model.Country;
import org.railwaystations.api.model.elastic.Bahnhofsfoto;
import org.railwaystations.api.monitoring.LoggingMonitor;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

public class PhotoImporterTest {

    private PhotoImporter importer;
    private Path uploadDir;
    private Path photoDir;
    private Bahnhofsfoto postedBahnhofsfoto;

    @BeforeEach
    public void setUp() throws IOException {
        final PhotographerLoader photographerLoader = new PhotographerLoader( new URL("file:./src/test/resources/photographers.json"));
        final StationLoaderDe loaderDe = new StationLoaderDe(new Country("de"), new URL("file:./src/test/resources/photosDe.json"), new URL("file:./src/test/resources/stationsDe.json"));
        uploadDir = Files.createTempDirectory("rsapiUpload");
        photoDir = Files.createTempDirectory("rsapiPhoto");
        importer = new PhotoImporter(new StationsRepository(new LoggingMonitor(), Collections.singletonList(loaderDe), photographerLoader, ""), uploadDir.toString(), photoDir.toString()) {

            @Override
            protected Optional<StatusLine> postToElastic(final Bahnhofsfoto bahnhofsfoto) {
                postedBahnhofsfoto = bahnhofsfoto;
                return Optional.of(new BasicStatusLine(new ProtocolVersion("http", 1, 0), 201, "OK"));
            }

        };
    }

    private File createFile(final String countryCode, final String photographer, final int stationId) throws IOException {
        final File importFile = new File(uploadDir.toFile(), countryCode + "/import/" + photographer + "-" + stationId + ".jpg");
        FileUtils.write(importFile, "test", Charset.forName("UTF-8"));
        return importFile;
    }

    @org.junit.jupiter.api.Test
    public void testImportHappyPath() throws IOException {
        final File importFile = createFile("de", "@storchp", 8009);
        final Map<String, String> result = importer.importPhotos();
        assertThat(result.get(importFile.getAbsolutePath()), is("imported Felde for @storchp"));
        assertPostedPhoto("@storchp","de", 8009, "0");
        assertThat(importFile.exists(), is(false));
        assertThat(new File(photoDir.toFile(), "de/8009.jpg").exists(), is(true));
    }

    @org.junit.jupiter.api.Test
    public void testImportNoStationData() throws IOException {
        final File importFile = createFile("cz", "@storchp", 4711);
        final Map<String, String> result = importer.importPhotos();
        assertThat(result.get(importFile.getAbsolutePath()), is("imported unknown station for @storchp"));
        assertPostedPhoto("@storchp","cz", 4711, "0");
        assertThat(importFile.exists(), is(false));
        assertThat(new File(photoDir.toFile(), "cz/4711.jpg").exists(), is(true));
    }

    @org.junit.jupiter.api.Test
    public void testImportPhotographerLevenshteinMatch() throws IOException {
        final File importFile = createFile("de", "@GabyBecker", 8009);
        final Map<String, String> result = importer.importPhotos();
        assertThat(result.get(importFile.getAbsolutePath()), is("imported Felde for Gaby Becker"));
        assertPostedPhoto("Gaby Becker", "de", 8009, "0");
        assertThat(importFile.exists(), is(false));
        assertThat(new File(photoDir.toFile(), "de/8009.jpg").exists(), is(true));
    }

    @org.junit.jupiter.api.Test
    public void testImportFlag() throws IOException {
        final File importFile = createFile("de", "@RecumbentTravel", 8009);
        final Map<String, String> result = importer.importPhotos();
        assertThat(result.get(importFile.getAbsolutePath()), is("imported Felde for Anonym"));
        assertPostedPhoto("Anonym", "de", 8009, "1");
        assertThat(importFile.exists(), is(false));
        assertThat(new File(photoDir.toFile(), "de/8009.jpg").exists(), is(true));
    }

    private void assertPostedPhoto(final String photographerName, final String countryCode, final int stationId, final String flag) {
        assertThat(postedBahnhofsfoto.getPhotographer(), is(photographerName));
        assertThat(postedBahnhofsfoto.getCountryCode(), is(countryCode));
        assertThat(postedBahnhofsfoto.getLicense(), is("CC0 1.0 Universell (CC0 1.0)"));
        assertThat(postedBahnhofsfoto.getUrl(), is("/fotos/" + countryCode + "/" + stationId + ".jpg"));
        assertThat(postedBahnhofsfoto.getId(), is(stationId));
        assertThat(postedBahnhofsfoto.getFlag(), is(flag));
        assertThat(postedBahnhofsfoto.getCreatedAt() / 10000, is(System.currentTimeMillis() / 10000));
    }

    @org.junit.jupiter.api.Test
    public void testImportPhotograferNotFound() throws IOException {
        final File importFile = createFile("de", "@unknown", 8009);
        final Map<String, String> result = importer.importPhotos();
        assertThat(result.get(importFile.getAbsolutePath()), is("Photographer @unknown not found"));
        assertThat(postedBahnhofsfoto, nullValue());
        assertThat(importFile.exists(), is(true));
    }

    @org.junit.jupiter.api.Test
    public void testImportStationNotFound() throws IOException {
        final File importFile = createFile("de", "@unknown", 99999999);
        final Map<String, String> result = importer.importPhotos();
        assertThat(result.get(importFile.getAbsolutePath()), is("Station 99999999 not found"));
        assertThat(postedBahnhofsfoto, nullValue());
        assertThat(importFile.exists(), is(true));
    }

}
