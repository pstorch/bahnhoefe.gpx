package org.railwaystations.api.model;

import com.fasterxml.jackson.annotation.JsonInclude;

import javax.ws.rs.core.Response;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class UploadResponse {
    private final UploadResponseState state;
    private final String message;
    private final Integer uploadId;
    private final String filename;

    public UploadResponse(final UploadResponseState state, final String message, final Integer uploadId, final String filename) {
        this.state = state;
        this.message = message;
        this.uploadId = uploadId;
        this.filename = filename;
    }

    public UploadResponse(final UploadResponseState state, final String message) {
        this(state, message, null, null);
    }

    public UploadResponse(final UploadResponseState state) {
        this(state, state.responseStatus.getReasonPhrase());
    }

    public UploadResponse(final UploadResponseState state, final Integer id, final String filename) {
        this(state, state.responseStatus.getReasonPhrase(), id, filename);
    }

    public UploadResponseState getState() {
        return state;
    }

    public String getMessage() {
        return message;
    }

    public Integer getUploadId() {
        return uploadId;
    }

    public String getFilename() {
        return filename;
    }

    public enum UploadResponseState {
        REVIEW(Response.Status.ACCEPTED),
        LAT_LON_OUT_OF_RANGE(Response.Status.BAD_REQUEST),
        NOT_ENOUGH_DATA(Response.Status.BAD_REQUEST),
        UNSUPPORTED_CONTENT_TYPE(Response.Status.BAD_REQUEST),
        PHOTO_TOO_LARGE(Response.Status.REQUEST_ENTITY_TOO_LARGE),
        CONFLICT(Response.Status.CONFLICT),
        UNAUTHORIZED(Response.Status.UNAUTHORIZED),
        ERROR(Response.Status.INTERNAL_SERVER_ERROR);

        final Response.Status responseStatus;

        UploadResponseState(Response.Status responseStatus) {
            this.responseStatus = responseStatus;
        }

        public Response.Status getResponseStatus() {
            return responseStatus;
        }
    }

}
