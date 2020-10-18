package org.railwaystations.api.model;

import com.fasterxml.jackson.annotation.JsonInclude;

import javax.ws.rs.core.Response;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class InboxResponse {
    private final InboxResponseState state;
    private final String message;
    private final Integer id;
    private final String filename;
    private final String inboxUrl;
    private final Long crc32;

    public InboxResponse(final InboxResponseState state, final String message, final Integer id, final String filename, final String inboxUrl, final Long crc32) {
        this.state = state;
        this.message = message;
        this.id = id;
        this.filename = filename;
        this.inboxUrl = inboxUrl;
        this.crc32 = crc32;
    }

    public InboxResponse(final InboxResponseState state, final Integer id, final String filename, final String inboxUrl, final Long crc32) {
        this(state, state.responseStatus.getReasonPhrase(), id, filename, inboxUrl, crc32);
    }

    public InboxResponse(final InboxResponseState state, final Integer id) {
        this(state, state.responseStatus.getReasonPhrase(), id, null, null, null);
    }

    public InboxResponse(final InboxResponseState state, final String message) {
        this(state, message, null, null, null, null);
    }

    public InboxResponse(final InboxResponseState state) {
        this(state, state.responseStatus.getReasonPhrase());
    }

    public InboxResponseState getState() {
        return state;
    }

    public String getMessage() {
        return message;
    }

    public Integer getId() {
        return id;
    }

    public String getFilename() {
        return filename;
    }

    public String getInboxUrl() {
        return inboxUrl;
    }

    public Long getCrc32() {
        return crc32;
    }

    public enum InboxResponseState {
        REVIEW(Response.Status.ACCEPTED),
        LAT_LON_OUT_OF_RANGE(Response.Status.BAD_REQUEST),
        NOT_ENOUGH_DATA(Response.Status.BAD_REQUEST),
        UNSUPPORTED_CONTENT_TYPE(Response.Status.BAD_REQUEST),
        PHOTO_TOO_LARGE(Response.Status.REQUEST_ENTITY_TOO_LARGE),
        CONFLICT(Response.Status.CONFLICT),
        UNAUTHORIZED(Response.Status.UNAUTHORIZED),
        ERROR(Response.Status.INTERNAL_SERVER_ERROR);

        final Response.Status responseStatus;

        InboxResponseState(final Response.Status responseStatus) {
            this.responseStatus = responseStatus;
        }

        public Response.Status getResponseStatus() {
            return responseStatus;
        }
    }

}
