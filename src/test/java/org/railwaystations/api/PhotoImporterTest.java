package org.railwaystations.api;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.railwaystations.api.db.CountryDao;
import org.railwaystations.api.db.PhotoDao;
import org.railwaystations.api.db.StationDao;
import org.railwaystations.api.db.UserDao;
import org.railwaystations.api.model.Country;
import org.railwaystations.api.model.Photo;
import org.railwaystations.api.model.Station;
import org.railwaystations.api.model.User;
import org.railwaystations.api.monitoring.LoggingMonitor;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.*;

@SuppressFBWarnings("UWF_FIELD_NOT_INITIALIZED_IN_CONSTRUCTOR")
public class PhotoImporterTest {

    private PhotoImporter importer;
    private Path uploadDir;
    private Path photoDir;
    private StationsRepository repository;
    private PhotoDao photoDao;

    @BeforeEach
    public void setUp() throws IOException {
        uploadDir = Files.createTempDirectory("rsapiUpload");
        photoDir = Files.createTempDirectory("rsapiPhoto");

        final CountryDao countryDao = mock(CountryDao.class);
        when(countryDao.findById("de")).thenReturn(Optional.of(new Country("de")));
        when(countryDao.findById("fr")).thenReturn(Optional.of(new Country("fr", "France", null, null, null, "CC BY-NC 4.0 International")));

        final UserDao userDao = mock(UserDao.class);
        when(userDao.findByNormalizedName("anonym")).thenReturn(Optional.of(new User("Anonym", null, "CC0 1.0 Universell (CC0 1.0)", 0, null, true, true, null)));
        when(userDao.findByNormalizedName("someuser")).thenReturn(Optional.of(new User("Some User", null, "CC0 1.0 Universell (CC0 1.0)", 1, null, true, true, null)));
        when(userDao.findByNormalizedName("gabybecker")).thenReturn(Optional.of(new User("@Gaby Becker", null, "CC0 1.0 Universell (CC0 1.0)", 1, null, true, true, null)));
        when(userDao.findByNormalizedName("storchp")).thenReturn(Optional.of(new User("@storchp", null, "CC0 1.0 Universell (CC0 1.0)", 2, null, true, false, null)));

        photoDao = mock(PhotoDao.class);

        final StationDao stationDao = mock(StationDao.class);
        final Station felde = new Station(new Station.Key("de", "8009"), "Felde", null, null);
        when(stationDao.findByKey(felde.getKey().getCountry(), felde.getKey().getId())).thenReturn(Collections.singleton(felde));

        final Station.Key hannoverKey = new Station.Key("de", "6913");
        final Station hannover = new Station(hannoverKey, "Hannover", null, new Photo(hannoverKey, "", new User("", "", ""), 0L, ""));
        when(stationDao.findByKey(hannoverKey.getCountry(), hannoverKey.getId())).thenReturn(Collections.singleton(hannover));

        final Station wangerooge = new Station(new Station.Key("de", "DE20763"), "Wangerooge Westanleger", null, null);
        when(stationDao.findByKey(wangerooge.getKey().getCountry(), wangerooge.getKey().getId())).thenReturn(Collections.singleton(wangerooge));

        final Station paris = new Station(new Station.Key("fr", "8768600"), "Paris-Gare-de-Lyon", null, null);
        when(stationDao.findByKey(paris.getKey().getCountry(), paris.getKey().getId())).thenReturn(Collections.singleton(paris));

        repository = new StationsRepository(countryDao, stationDao);

        importer = new PhotoImporter(repository, userDao, photoDao, countryDao, new LoggingMonitor(), uploadDir.toString(), photoDir.toString());
    }

    private File createFile(final String countryCode, final String photographer, final String stationId) throws IOException {
        return createFile(countryCode, photographer, stationId, ".jpg");
    }

    private File createFile(final String countryCode, final String photographer, final String stationId, final String extension) throws IOException {
        final File importFile = new File(uploadDir.toFile(), countryCode + "/import/" + photographer + "-" + stationId + extension);
        FileUtils.write(importFile, "test", Charset.forName("UTF-8"));
        return importFile;
    }

    @Test
    public void testImportHappyPath() throws IOException {
        final File importFile = createFile("de", "@storchp", "8009");
        final Station.Key key = new Station.Key("de", "8009");
        assertThat(repository.findByKey(key).hasPhoto(), is(false));
        final ArgumentCaptor<Photo> argument = ArgumentCaptor.forClass(Photo.class);
        final List<PhotoImporter.ReportEntry> result = importer.importPhotos();
        verify(photoDao).insert(argument.capture());
        assertThat(result.get(0).getMessage(), is("imported Felde for @storchp"));
        assertPostedPhoto(argument.getValue(), 2,"de", "8009");
        assertThat(importFile.exists(), is(false));
        assertThat(new File(photoDir.toFile(), "de/8009.jpg").exists(), is(true));
    }

    @Test
    public void testImportAlphanumericId() throws IOException {
        final File importFile = createFile("de", "@storchp", "DE20763");
        final Station.Key key = new Station.Key("de", "DE20763");
        assertThat(repository.findByKey(key).hasPhoto(), is(false));
        final ArgumentCaptor<Photo> argument = ArgumentCaptor.forClass(Photo.class);
        final List<PhotoImporter.ReportEntry> result = importer.importPhotos();
        verify(photoDao).insert(argument.capture());
        assertThat(result.get(0).getMessage(), is("imported Wangerooge Westanleger for @storchp"));
        assertPostedPhoto(argument.getValue(), 2,"de", "DE20763");
        assertThat(importFile.exists(), is(false));
        assertThat(new File(photoDir.toFile(), "de/DE20763.jpg").exists(), is(true));
    }

    @Test
    public void testImportStationHasPhoto() throws IOException {
        final File importFile = createFile("de", "@storchp", "6913");
        verify(photoDao, never()).insert(any(Photo.class));
        final List<PhotoImporter.ReportEntry> result = importer.importPhotos();
        assertThat(result.get(0).getMessage(), is("Station 6913 has already a photo"));
        assertThat(importFile.exists(), is(true));
        verifyZeroInteractions(photoDao);
    }

    @Test
    public void testImportDuplicates() throws IOException {
        final File importFile1 = createFile("de", "@storchp", "8009", ".Jpg");
        final File importFile2 = createFile("de", "Anonym", "8009", ".jpeg");
        verify(photoDao, never()).insert(any(Photo.class));
        final List<PhotoImporter.ReportEntry> result = importer.importPhotos();
        assertThat(result.get(0).getMessage(), is("conflict with another photo in inbox"));
        assertThat(result.get(1).getMessage(), is("conflict with another photo in inbox"));
        assertThat(importFile1.exists(), is(true));
        assertThat(importFile2.exists(), is(true));
        verifyZeroInteractions(photoDao);
    }

    @Test
    public void testImportNoStationData() throws IOException {
        final File importFile = createFile("cz", "@storchp", "4711");
        final ArgumentCaptor<Photo> argument = ArgumentCaptor.forClass(Photo.class);
        final List<PhotoImporter.ReportEntry> result = importer.importPhotos();
        verify(photoDao).insert(argument.capture());
        assertThat(result.get(0).getMessage(), is("imported unknown station for @storchp"));
        assertPostedPhoto(argument.getValue(), 2,"cz", "4711");
        assertThat(importFile.exists(), is(false));
        assertThat(new File(photoDir.toFile(), "cz/4711.jpg").exists(), is(true));
    }

    @Test
    public void testImportAnonymous() throws IOException {
        final File importFile = createFile("de", "Some User", "8009");
        final ArgumentCaptor<Photo> argument = ArgumentCaptor.forClass(Photo.class);
        final List<PhotoImporter.ReportEntry> result = importer.importPhotos();
        verify(photoDao).insert(argument.capture());
        assertThat(result.get(0).getMessage(), is("imported Felde for Some User (anonymous)"));
        assertPostedPhoto(argument.getValue(), 1, "de", "8009");
        assertThat(importFile.exists(), is(false));
        assertThat(new File(photoDir.toFile(), "de/8009.jpg").exists(), is(true));
    }

    private void assertPostedPhoto(final Photo photo, final Integer photographerId, final String countryCode, final String stationId) {
        assertPostedPhoto(photo, photographerId, countryCode, stationId, "CC0 1.0 Universell (CC0 1.0)");
    }

    private void assertPostedPhoto(final Photo photo, final Integer photographerId, final String countryCode, final String stationId, final String license) {
        assertThat(photo.getPhotographer().getId(), is(photographerId));
        assertThat(photo.getStationKey().getCountry(), is(countryCode));
        assertThat(photo.getLicense(), is(license));
        assertThat(photo.getUrl(), is("/fotos/" + countryCode + "/" + stationId + ".jpg"));
        assertThat(photo.getStationKey().getId(), is(stationId));
        assertThat(photo.getCreatedAt() / 10000, is(System.currentTimeMillis() / 10000));
    }

    @Test
    public void testImportPhotograferNotFound() throws IOException {
        final File importFile = createFile("de", "@unknown", "8009");
        final Station.Key key = new Station.Key("de", "8009");
        assertThat(repository.findByKey(key).hasPhoto(), is(false));
        verify(photoDao, never()).insert(any(Photo.class));

        final List<PhotoImporter.ReportEntry> result = importer.importPhotos();
        assertThat(result.get(0).getMessage(), is("Photographer @unknown not found"));
        assertThat(importFile.exists(), is(true));
        assertThat(repository.findByKey(key).hasPhoto(), is(false));
        verifyZeroInteractions(photoDao);
    }

    @Test
    public void testImportStationNotFound() throws IOException {
        final File importFile = createFile("de", "@GabyBecker", "99999999");
        verify(photoDao, never()).insert(any(Photo.class));
        final List<PhotoImporter.ReportEntry> result = importer.importPhotos();
        assertThat(result.get(0).getMessage(), is("Station 99999999 not found"));
        assertThat(importFile.exists(), is(true));
        verifyZeroInteractions(photoDao);
    }

    @Test
    public void testReportToMessage() {
        final List<PhotoImporter.ReportEntry> report = new ArrayList<>(2);
        report.add(new PhotoImporter.ReportEntry(false, "de","path1", "success message"));
        report.add(new PhotoImporter.ReportEntry(false, "de","path2", "success message"));
        report.add(new PhotoImporter.ReportEntry(false, "ch","path3", "success message"));
        report.add(new PhotoImporter.ReportEntry(true, "de","path4", "error message"));

        final String message = PhotoImporter.reportToMessage(report);
        assertThat(message, is ("Imported:\n- de: 2\n- ch: 1\n\n- path1: success message\n- path2: success message\n- path3: success message\n\nErrors:\n- path4: error message\n"));
    }

    @Test
    public void testImportOverrideLicense() throws IOException {
        final File importFile = createFile("fr", "@storchp", "8768600");
        final Station.Key key = new Station.Key("fr", "8768600");
        assertThat(repository.findByKey(key).hasPhoto(), is(false));
        final ArgumentCaptor<Photo> argument = ArgumentCaptor.forClass(Photo.class);
        final List<PhotoImporter.ReportEntry> result = importer.importPhotos();
        verify(photoDao).insert(argument.capture());
        assertThat(result.get(0).getMessage(), is("imported Paris-Gare-de-Lyon for @storchp"));
        assertPostedPhoto(argument.getValue(), 2,"fr", "8768600", "CC BY-NC 4.0 International");
        assertThat(importFile.exists(), is(false));
        assertThat(new File(photoDir.toFile(), "fr/8768600.jpg").exists(), is(true));
    }

}
