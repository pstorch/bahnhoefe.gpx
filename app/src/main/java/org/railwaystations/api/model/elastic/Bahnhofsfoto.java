package org.railwaystations.api.model.elastic;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Bahnhofsfoto {

    @JsonProperty("BahnhofsID")
    private int id;

    @JsonProperty("bahnhofsfoto")
    private String url;

    @JsonProperty("fotolizenz")
    private String license;

    @JsonProperty("fotografenname")
    private String photographer;

    @JsonProperty("erfasst")
    private long createdAt;

    @JsonProperty("flag")
    private String flag;

    @JsonProperty("laenderkennzeichen")
    private String countryCode;

    public Bahnhofsfoto() {
        super();
    }

    public Bahnhofsfoto(final int id, final String url, final String license, final String photographer, final long createdAt, final String flag, final String countryCode) {
        this.id = id;
        this.url = url;
        this.license = license;
        this.photographer = photographer;
        this.createdAt = createdAt;
        this.flag = flag;
        this.countryCode = countryCode;
    }

    public int getId() {
        return id;
    }

    public void setId(final int id) {
        this.id = id;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(final String url) {
        this.url = url;
    }

    public String getLicense() {
        return license;
    }

    public void setLicense(final String license) {
        this.license = license;
    }

    public String getPhotographer() {
        return photographer;
    }

    public void setPhotographer(final String photographer) {
        this.photographer = photographer;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(final long createdAt) {
        this.createdAt = createdAt;
    }

    public String getFlag() {
        return flag;
    }

    public void setFlag(final String flag) {
        this.flag = flag;
    }

    public String getCountryCode() {
        return countryCode;
    }

    public void setCountryCode(final String countryCode) {
        this.countryCode = countryCode;
    }
}
