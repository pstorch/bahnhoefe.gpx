package org.railwaystations.api.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.apache.commons.lang3.StringUtils;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

public class User {

    private final String name;
    private final String url;
    private final String license;

    @JsonIgnore
    private int id;
    @JsonIgnore
    private String email;
    @JsonIgnore
    private String normalizedName;
    @JsonIgnore
    private boolean ownPhotos;
    @JsonIgnore
    private boolean anonymous;
    @JsonIgnore
    private Long uploadTokenSalt;

    public User(final String name, final String url, final String license, final int id, final String email, final String normalizedName, boolean ownPhotos, boolean anonymous, final Long uploadTokenSalt) {
        this.name = name;
        this.url = url;
        this.license = license;
        this.id = id;
        this.email = email;
        this.normalizedName = normalizedName;
        this.ownPhotos = ownPhotos;
        this.anonymous = anonymous;
        this.uploadTokenSalt = uploadTokenSalt;
    }

    public User(final String name, final String url, final String license) {
        this(name, url, license, 0, null, normalize(name), true, false, null);
    }

    public static Map<String, User> toNameMap(final List<User> list) {
        return list.stream().collect(Collectors.toMap (User::getNormalizedName, i -> i));
    }

    public static Map<Integer, User> toIdMap(final List<User> list) {
        return list.stream().collect(Collectors.toMap (User::getId, i -> i));
    }

    public static String normalize(final String name) {
        return StringUtils.trimToEmpty(name).toLowerCase(Locale.ENGLISH).replaceAll("[^a-z0-9]","");
    }

    public String getName() {
        return name;
    }

    public String getUrl() {
        return url;
    }

    public String getDisplayUrl() {
        return anonymous || StringUtils.isBlank(url) ? "https://railway-stations.org" : url;
    }

    public String getLicense() {
        return license;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final User that = (User) o;

        return name != null ? name.equals(that.name) : that.name == null;
    }

    @Override
    public int hashCode() {
        return name != null ? name.hashCode() : 0;
    }

    public int getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public String getNormalizedName() {
        return normalizedName;
    }

    public boolean isOwnPhotos() {
        return ownPhotos;
    }

    public boolean isAnonymous() {
        return anonymous;
    }

    public Long getUploadTokenSalt() {
        return uploadTokenSalt;
    }

    public String getDisplayName() {
        return anonymous ? "Anonym" : getName();
    }

}
