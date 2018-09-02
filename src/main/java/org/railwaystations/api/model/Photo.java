package org.railwaystations.api.model;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import org.apache.commons.lang3.StringUtils;

import java.util.Locale;

public class Photo {

    public static final String FLAG_DEFAULT = "0";
    private static final BiMap<String, String> FLAGS = HashBiMap.create();

    static {
        FLAGS.put("1", "recumbenttravel");
    }

    private final Station.Key stationKey;
    private final String url;
    private final String photographer;
    private final String photographerUrl;
    private final Long createdAt;
    private final String license;
    private final String statUser;
    private final String flag;

    public Photo(final Station.Key stationKey, final String url, final String photographer, final String photographerUrl, final Long createdAt, final String license) {
        this(stationKey, url, photographer, photographerUrl, createdAt, license, FLAG_DEFAULT);
    }

    public Photo(final Station.Key stationKey, final String url, final String photographer, final String photographerUrl, final Long createdAt, final String license, final String flag) {
        this.stationKey = stationKey;
        this.url = url;
        this.photographer = photographer;
        this.photographerUrl = photographerUrl;
        this.createdAt = createdAt;
        this.license = license;
        this.flag = flag;
        this.statUser = getStatUser(flag, photographer);
    }

    /**
     * Gets the user for the statistic, deanonymizes if mapping exists
     */
    public static String getStatUser(final String flag, final String photographer) {
        return FLAGS.getOrDefault(flag, photographer);
    }

    public static String getFlag(final String photographerName) {
        return FLAGS.inverse().getOrDefault(StringUtils.trimToEmpty(photographerName).replace("@", "").toLowerCase(Locale.GERMAN), FLAG_DEFAULT);
    }

    public String getUrl() {
        return url;
    }

    public String getPhotographer() {
        return photographer;
    }

    public Station.Key getStationKey() {
        return stationKey;
    }

    public String getLicense() {
        return license;
    }

    public String getPhotographerUrl() {
        return photographerUrl;
    }

    public String getStatUser() {
        return statUser;
    }

    public Long getCreatedAt() {
        return createdAt;
    }

    public String getFlag() {
        return flag;
    }

}
