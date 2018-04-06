package org.railwaystations.api.model.elastic;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Fotograf {

    @JsonProperty("fotografenname")
    private String name;

    @JsonProperty("fotografenURL")
    private String url;

    @JsonProperty("fotografenlizenz")
    private String license;

    public Fotograf() {
        super();
    }

    public Fotograf(final String name, final String url, final String license) {
        this.name = name;
        this.url = url;
        this.license = license;
    }

    public String getLicense() {
        return license;
    }

    public void setLicense(final String license) {
        this.license = license;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(final String url) {
        this.url = url;
    }

    public String getName() {
        return name;
    }

    public void setName(final String name) {
        this.name = name;
    }
}
