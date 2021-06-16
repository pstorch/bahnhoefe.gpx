package org.railwaystations.rsapi.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonUnwrapped;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class InboxStateQuery {
    private Integer id;
    private String countryCode;
    private String stationId;
    @JsonUnwrapped
    private Coordinates coordinates;
    private InboxState state = InboxState.UNKNOWN;
    private String rejectedReason;
    private String filename;
    private String inboxUrl;
    private Long crc32;

    public InboxStateQuery() {
    }

    public InboxStateQuery(final Integer id, final String countryCode, final String stationId, final Coordinates coordinates, final String rejectedReason, final String filename) {
        super();
        this.id = id;
        this.countryCode = countryCode;
        this.stationId = stationId;
        this.coordinates = coordinates;
        this.rejectedReason = rejectedReason;
        this.filename = filename;
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

    public InboxState getState() {
        return state;
    }

    public Integer getId() {
        return id;
    }

    public void setId(final Integer id) {
        this.id = id;
    }

    public String getRejectedReason() {
        return rejectedReason;
    }

    public void setState(final InboxState state) {
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

    public void setFilename(final String filename) {
        this.filename = filename;
    }

    public String getFilename() {
        return filename;
    }

    public void setInboxUrl(final String inboxUrl) {
        this.inboxUrl = inboxUrl;
    }

    public String getInboxUrl() {
        return inboxUrl;
    }

    public void setCrc32(final Long crc32) {
        this.crc32 = crc32;
    }

    public Long getCrc32() {
        return crc32;
    }

    public enum InboxState {
        UNKNOWN,
        REVIEW,
        CONFLICT,
        ACCEPTED,
        REJECTED
    }

}
