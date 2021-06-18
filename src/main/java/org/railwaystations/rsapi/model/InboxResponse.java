package org.railwaystations.rsapi.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.springframework.http.HttpStatus;

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
        REVIEW(HttpStatus.ACCEPTED),
        LAT_LON_OUT_OF_RANGE(HttpStatus.BAD_REQUEST),
        NOT_ENOUGH_DATA(HttpStatus.BAD_REQUEST),
        UNSUPPORTED_CONTENT_TYPE(HttpStatus.BAD_REQUEST),
        PHOTO_TOO_LARGE(HttpStatus.PAYLOAD_TOO_LARGE),
        CONFLICT(HttpStatus.CONFLICT),
        UNAUTHORIZED(HttpStatus.UNAUTHORIZED),
        ERROR(HttpStatus.INTERNAL_SERVER_ERROR);

        final HttpStatus responseStatus;

        InboxResponseState(final HttpStatus responseStatus) {
            this.responseStatus = responseStatus;
        }

        public HttpStatus getResponseStatus() {
            return responseStatus;
        }
    }

}
