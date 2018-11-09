package org.railwaystations.api.model;

import java.util.HashMap;
import java.util.Map;

public class Photo {

    private static final Map<String, String> LICENSES = new HashMap<>();

    static {
        LICENSES.put("CC BY 3.0", "https://creativecommons.org/licenses/by/3.0/");
        LICENSES.put("CC BY-NC 4.0 International", "https://creativecommons.org/licenses/by-nc/4.0/");
        LICENSES.put("CC BY-NC-SA 3.0 DE", "https://creativecommons.org/licenses/by-nc-sa/3.0/de/");
        LICENSES.put("CC BY-SA 4.0", "https://creativecommons.org/licenses/by-sa/4.0/");
        LICENSES.put("CC0 1.0 Universell (CC0 1.0)", "https://creativecommons.org/publicdomain/zero/1.0/");
    }

    private final Station.Key stationKey;
    private final String url;
    private final User photographer;
    private final Long createdAt;
    private final String license;
    private final String licenseUrl;

    public Photo(final Station.Key stationKey, final String url, final User photographer, final Long createdAt, final String license) {
        this.stationKey = stationKey;
        this.url = url;
        this.photographer = photographer;
        this.createdAt = createdAt;
        this.license = license;
        this.licenseUrl = LICENSES.get(license);
    }

    public String getUrl() {
        return url;
    }

    public User getPhotographer() {
        return photographer;
    }

    public Station.Key getStationKey() {
        return stationKey;
    }

    public String getLicense() {
        return license;
    }

    public String getLicenseUrl() {
        return licenseUrl;
    }

    public Long getCreatedAt() {
        return createdAt;
    }

}
