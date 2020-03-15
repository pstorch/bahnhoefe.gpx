package org.railwaystations.api.model;

import com.fasterxml.jackson.annotation.*;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class InboxEntry extends PublicInboxEntry {

    @JsonProperty
    private final int id;

    @JsonIgnore
    private final int photographerId;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private final String photographerNickname;

    @JsonIgnore
    private final String extension;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private final String comment;

    @JsonProperty
    private final String rejectReason;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private final Long createdAt;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private final boolean done;

    @JsonProperty
    private final Command command;

    @JsonProperty(value = "hasPhoto", access = JsonProperty.Access.READ_ONLY)
    private final boolean hasPhoto;

    @JsonProperty(value = "hasConflict", access = JsonProperty.Access.READ_ONLY)
    private boolean conflict;

    @JsonProperty(value = "problemReportType", access = JsonProperty.Access.READ_ONLY)
    private final ProblemReportType problemReportType;

    @JsonProperty(value = "isProcessed", access = JsonProperty.Access.READ_ONLY)
    private boolean processed;

    @JsonProperty(value = "inboxUrl", access = JsonProperty.Access.READ_ONLY)
    private String inboxUrl;

    @JsonProperty(value = "DS100")
    private String ds100;

    @JsonProperty(value = "active")
    private Boolean active;

    @JsonProperty(value = "ignoreConflict")
    private Boolean ignoreConflict;

    @JsonProperty(value = "createStation")
    private Boolean createStation;

    /**
     * Constructor with all values from database
     */
    public InboxEntry(final int id, final String countryCode, final String stationId, final String title,
                      final Coordinates coordinates, final int photographerId, final String photographerNickname,
                      final String extension, final String comment, final String rejectReason,
                      final Long createdAt, final boolean done, final Command command, final boolean hasPhoto,
                      final boolean conflict, final ProblemReportType problemReportType) {
        super(countryCode, stationId, title, coordinates);
        this.id = id;
        this.photographerId = photographerId;
        this.photographerNickname = photographerNickname;
        this.extension = extension;
        this.comment = comment;
        this.rejectReason = rejectReason;
        this.createdAt = createdAt;
        this.done = done;
        this.command = command;
        this.hasPhoto = hasPhoto;
        this.conflict = conflict;
        this.problemReportType = problemReportType;
    }

    /**
     * Constructor to insert new record from photoUpload
     */
    public InboxEntry(final String countryCode, final String stationId, final String title,
                      final Coordinates coordinates, final int photographerId,
                      final String extension, final String comment, final ProblemReportType problemReportType) {
        this(0, countryCode, stationId, title, coordinates, photographerId, null, extension,
                comment, null, System.currentTimeMillis(), false, null, false,
                false, problemReportType);
    }

    /**
     * Constructor to deserialize json for updating the records
     */
    @SuppressWarnings("PMD.SimplifiedTernary")
    public InboxEntry(@JsonProperty("id") final int id,
                      @JsonProperty("countryCode") final String countryCode,
                      @JsonProperty("stationId") final String stationId,
                      @JsonProperty("rejectReason") final String rejectReason,
                      @JsonProperty("command") final Command command,
                      @JsonProperty("DS100") final String ds100,
                      @JsonProperty("active") final Boolean active,
                      @JsonProperty("ignoreConflict") final Boolean ignoreConflict,
                      @JsonProperty(value = "createStation") final Boolean createStation) {
        this(id, countryCode, stationId, null, null, 0, null,
                null, null, rejectReason, null, false, command, false,
                false, null);
        this.ds100 = ds100;
        this.active = active != null ? active : true;
        this.ignoreConflict = ignoreConflict;
        this.createStation = createStation;
    }

    public int getId() {
        return id;
    }

    public int getPhotographerId() {
        return photographerId;
    }

    public String getPhotographerNickname() {
        return photographerNickname;
    }

    public String getComment() {
        return comment;
    }

    public String getRejectReason() {
        return rejectReason;
    }

    public Long getCreatedAt() {
        return createdAt;
    }

    public boolean isDone() {
        return done;
    }

    public Command getCommand() {
        return command;
    }

    public String getExtension() {
        return extension;
    }

    public boolean hasPhoto() {
        return hasPhoto;
    }

    public boolean hasConflict() {
        return conflict;
    }

    public ProblemReportType getProblemReportType() {
        return problemReportType;
    }

    public String getFilename() {
        return getFilename(id, extension);
    }

    public static String getFilename(final Integer id, final String extension) {
        if (id == null || extension == null) {
            return null;
        }
        return String.format("%d.%s", id, extension);
    }

    public void isProcessed(final boolean processed) {
        this.processed = processed;
    }

    public boolean isProcessed() {
        return processed;
    }

    public String getInboxUrl() {
        return inboxUrl;
    }

    public void setInboxUrl(final String inboxUrl) {
        this.inboxUrl = inboxUrl;
    }

    public boolean isProblemReport() {
        return problemReportType != null;
    }

    public String getDS100() {
        return ds100;
    }

    public Boolean isActive() {
        return active;
    }

    public Boolean ignoreConflict() {
        return ignoreConflict;
    }

    public Boolean createStation() {
        return createStation;
    }

    public void setConflict(final boolean conflict) {
        this.conflict = conflict;
    }

    public enum Command {
        IMPORT,
        DEACTIVATE_STATION,
        DELETE_STATION,
        DELETE_PHOTO,
        MARK_SOLVED,
        REJECT
    }

}
