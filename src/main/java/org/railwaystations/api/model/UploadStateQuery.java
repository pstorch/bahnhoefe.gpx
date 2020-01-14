package org.railwaystations.api.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonUnwrapped;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UploadStateQuery {
    private Integer uploadId;
    private String countryCode;
    private String stationId;
    @JsonUnwrapped
    private Coordinates coordinates;
    private UploadState state = UploadState.UNKNOWN;
    private String rejectedReason;
    private String inboxUrl;

    public UploadStateQuery() {
    }

    public UploadStateQuery(final Integer uploadId, final String countryCode, final String stationId, final Coordinates coordinates, final String rejectedReason, final String inboxUrl) {
        super();
        this.uploadId = uploadId;
        this.countryCode = countryCode;
        this.stationId = stationId;
        this.coordinates = coordinates;
        this.rejectedReason = rejectedReason;
        this.inboxUrl = inboxUrl;
    }

    public String getCountryCode() {
        return countryCode;
    }

    public String getStationId() {
        return stationId;
    }

    public Coordinates getCoordinates() {
        return coordinates;
    }

    public UploadState getState() {
        return state;
    }

    public Integer getUploadId() {
        return uploadId;
    }

    public String getRejectedReason() {
        return rejectedReason;
    }

    public String getInboxUrl() {
        return inboxUrl;
    }

    public void setState(final UploadState state) {
        this.state = state;
    }

    public void setRejectedReason(final String rejectReason) {
        this.rejectedReason = rejectReason;
    }

    public void setCountryCode(final String countryCode) {
        this.countryCode = countryCode;
    }

    public void setStationId(final String stationId) {
        this.stationId = stationId;
    }

    public void setCoordinates(final Coordinates coordinates) {
        this.coordinates = coordinates;
    }

    public void setInboxUrl(final String inboxUrl) {
        this.inboxUrl = inboxUrl;
    }

    public enum UploadState {
        UNKNOWN,
        REVIEW,
        CONFLICT,
        ACCEPTED,
        REJECTED,
        OTHER_USER;
    }

}
