package org.railwaystations.rsapi;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.railwaystations.rsapi.db.CountryDao;
import org.railwaystations.rsapi.db.InboxDao;
import org.railwaystations.rsapi.db.PhotoDao;
import org.railwaystations.rsapi.db.UserDao;
import org.railwaystations.rsapi.model.Country;
import org.railwaystations.rsapi.model.Photo;
import org.railwaystations.rsapi.model.Station;
import org.railwaystations.rsapi.model.User;
import org.railwaystations.rsapi.monitoring.Monitor;
import org.railwaystations.rsapi.utils.ImageUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Photo importer from legacy inbox directory, which is still being used by batch imports
 */
@SuppressWarnings("PMD.BeanMembersShouldSerialize")
public class PhotoImporter {

    private static final Logger LOG = LoggerFactory.getLogger(PhotoImporter.class);
    private static final Pattern IMPORT_FILE_PATTERN = Pattern.compile("([^-]+)-([A-Z\\d]+).jpe?g", Pattern.CASE_INSENSITIVE);
    private static final Executor EXECUTOR = Executors.newSingleThreadExecutor();

    private final StationsRepository repository;
    private final UserDao userDao;
    private final PhotoDao photoDao;
    private final CountryDao countryDao;
    private final Monitor monitor;
    private final File uploadDir;
    private final File photoDir;
    private final InboxDao inboxDao;

    public PhotoImporter(final StationsRepository repository, final UserDao userDao, final PhotoDao photoDao,
                         final CountryDao countryDao, final Monitor monitor, final String uploadDir,
                         final String photoDir, final InboxDao inboxDao) {
        this.repository = repository;
        this.userDao = userDao;
        this.photoDao = photoDao;
        this.countryDao = countryDao;
        this.monitor = monitor;
        this.uploadDir = new File(uploadDir);
        this.photoDir = new File(photoDir);
        this.inboxDao = inboxDao;
    }

    public void importPhotosAsync() {
        EXECUTOR.execute(() -> {
            final List<ReportEntry> report = importPhotos();
            monitor.sendMessage(reportToMessage(report) + "\n" + repository.getCountryStatisticMessage());
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
        report.stream().filter(e -> !e.isError()).collect(
                Collectors.groupingBy(ReportEntry::getCountryCode, Collectors.counting())
        ).forEach((key, value) -> builder.append("- ").append(key).append(": ").append(value).append('\n'));
        builder.append("\n");
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
            report.add(new ReportEntry(true, countryCode, countryDir.getAbsolutePath(), "does not exist and could not be created"));
            return;
        }
        final Optional<Country> country = countryDao.findById(countryCode);
        final Collection<File> files = FileUtils.listFiles(importDir, null, false);

        final Map<File, Photo> photosToImport = new HashMap<>();
        for (final File importFile : files) {
            try {
                final Matcher matcher = IMPORT_FILE_PATTERN.matcher(importFile.getName());
                if (!matcher.find()) {
                    report.add(new ReportEntry(true, countryCode, importFile.getAbsolutePath(), "File doesn't match pattern: $photographer-$stationId.jpg"));
                    continue;
                }

                final String stationId = matcher.group(2);
                final String photographerName = matcher.group(1);

                final Optional<User> user = userDao.findByNormalizedName(User.normalizeName(photographerName));
                if (user.isEmpty()) {
                    report.add(new ReportEntry(true, countryCode, importFile.getAbsolutePath(), "Photographer " + photographerName + " not found"));
                    continue;
                }
                final Photo photo = createPhoto(countryCode, country.orElse(null), stationId, user.get(), ImageUtil.JPG);
                photosToImport.put(importFile, photo);
            } catch (final Exception e) {
                LOG.error("Error importing photo " + importFile, e);
                report.add(new ReportEntry(true, countryCode, importFile.getAbsolutePath(), "Exception: " + e.getMessage()));
            }
        }

        int importCount = 0;
        for (final Map.Entry<File, Photo> photoToImport : photosToImport.entrySet()) {
            final File importFile = photoToImport.getKey();
            final Photo photo = photoToImport.getValue();
            try {
                Station station = null;
                if (country.isPresent()) {
                    station = repository.findByKey(photo.getStationKey());
                    if (station == null) {
                        report.add(new ReportEntry(true, countryCode, importFile.getAbsolutePath(), "Station " + photo.getStationKey().getId() + " not found"));
                        continue;
                    }
                    if (station.hasPhoto()) {
                        report.add(new ReportEntry(true, countryCode, importFile.getAbsolutePath(), "Station " + photo.getStationKey().getId() + " has already a photo"));
                        continue;
                    }
                }

                if (photosToImport.entrySet().stream().anyMatch(e -> e.getKey() != importFile && e.getValue().getStationKey().equals(photo.getStationKey()))
                    || inboxDao.countPendingInboxEntriesForStation(null, photo.getStationKey().getCountry(), photo.getStationKey().getId()) > 0) {
                    report.add(new ReportEntry(true, countryCode, importFile.getAbsolutePath(), "conflict with another photo in inbox"));
                    continue;
                }

                photoDao.insert(photo);

                moveFile(importFile, countryDir, photo.getStationKey().getId(), ImageUtil.JPG);
                LOG.info("Photo " + importFile.getAbsolutePath() + " imported");
                importCount++;

                report.add(new ReportEntry(false, countryCode, importFile.getAbsolutePath(),
                        "imported " + (station != null ? station.getTitle() : "unknown station") + " for " + photo.getPhotographer().getName() + (photo.getPhotographer().isAnonymous() ? " (anonymous)": "")));
            } catch (final Exception e) {
                LOG.error("Error importing photo " + importFile, e);
                report.add(new ReportEntry(true, countryCode, importFile.getAbsolutePath(), "Exception: " + e.getMessage()));
            }
        }

        LOG.info("Imported " + importCount + " for " + countryCode);
    }

    public static Photo createPhoto(final String countryCode, final Country country, final String stationId, final User user, final String extension) {
        return new Photo(new Station.Key(countryCode, stationId), "/" + countryCode + "/" + stationId + "." + extension, user, System.currentTimeMillis(), getLicense(user.getLicense(), country));
    }

    /**
     * Gets the applicable license for the given country.
     * We need to override the license for some countries, because of limitations of the "Freedom of panorama".
     */
    public static String getLicense(final String photographerLicense, final Country country) {
        if (country != null && StringUtils.isNotBlank(country.getOverrideLicense())) {
            return country.getOverrideLicense();
        }
        return photographerLicense;
    }

    public static void moveFile(final File importFile, final File countryDir, final String stationId, final String extension) throws IOException {
        final File destFile = getCleanDestFile(countryDir, stationId, extension);
        FileUtils.moveFile(importFile, destFile);
    }

    public static void copyFile(final File importFile, final File countryDir, final String stationId, final String extension) throws IOException {
        final File destFile = getCleanDestFile(countryDir, stationId, extension);
        FileUtils.copyFile(importFile, destFile);
    }

    private static File getCleanDestFile(final File countryDir, final String stationId, final String extension) {
        final File destFile = new File(countryDir, stationId + "." + extension);
        FileUtils.deleteQuietly(destFile);
        return destFile;
    }

    public static final class ReportEntry {
        private final boolean error;
        private final String countryCode;
        private final String path;
        private final String message;

        public ReportEntry(final boolean error, final String countryCode, final String path, final String message) {
            this.error = error;
            this.countryCode = countryCode;
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

        public String getCountryCode() {
            return countryCode;
        }
    }

}
