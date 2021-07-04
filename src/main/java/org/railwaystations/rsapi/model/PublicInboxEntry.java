package org.railwaystations.rsapi.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonUnwrapped;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PublicInboxEntry {

    @JsonProperty
    protected String countryCode;

    @JsonProperty
    protected String stationId;

    @JsonProperty
    protected String title;

    @JsonUnwrapped
    protected Coordinates coordinates;

    public PublicInboxEntry() {
    }

    public PublicInboxEntry(final String countryCode, final String stationId, final String title, final Coordinates coordinates) {
        this.countryCode = countryCode;
        this.stationId = stationId;
        this.title = title;
        this.coordinates = coordinates;
    }

    public String getCountryCode() {
        return countryCode;
    }

    public String getStationId() {
        return stationId;
    }

    public String getTitle() {
        return title;
    }

    public Coordinates getCoordinates() {
        return coordinates;
    }

    public void setCountryCode(final String countryCode) {
        this.countryCode = countryCode;
    }

    public void setStationId(final String stationId) {
        this.stationId = stationId;
    }

    public void setTitle(final String title) {
        this.title = title;
    }

    public void setCoordinates(final Coordinates coordinates) {
        this.coordinates = coordinates;
    }
}
