package org.railwaystations.api.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ProblemReport {

    @JsonProperty
    private String countryCode;

    @JsonProperty
    private String stationId;

    @JsonProperty
    private ProblemReportType type;

    @JsonProperty
    private String comment;

    public ProblemReport() {
    }

    public ProblemReport(final String countryCode, final String stationId, final ProblemReportType type, final String comment) {
        super();
        this.countryCode = countryCode;
        this.stationId = stationId;
        this.type = type;
        this.comment = comment;
    }

    public String getCountryCode() {
        return countryCode;
    }

    public void setCountryCode(final String countryCode) {
        this.countryCode = countryCode;
    }

    public String getStationId() {
        return stationId;
    }

    public void setStationId(final String stationId) {
        this.stationId = stationId;
    }

    public ProblemReportType getType() {
        return type;
    }

    public void setType(final ProblemReportType type) {
        this.type = type;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(final String comment) {
        this.comment = comment;
    }

}
