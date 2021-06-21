package org.railwaystations.rsapi.monitoring;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.railwaystations.rsapi.utils.ImageUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.Executors;

@Service
public class MatrixMonitor implements Monitor {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final Logger LOG = LoggerFactory.getLogger(MatrixMonitor.class);

    private final WebClient webClient;

    @Value("${matrix.roomUrl}")
    private String roomUrl;
    @Value("${matrix.uploadUrl}")
    private String uploadUrl;
    @Value("${matrix.accessToken}")
    private String accessToken;


    public MatrixMonitor(final WebClient.Builder webClientBuilder) {
        super();
        this.webClient = webClientBuilder.clone().build();
    }

    @Override
    public void sendMessage(final String message) {
        sendMessage(message, null);
    }

    public void sendMessage(final String message, final File photo) {
        Executors.newSingleThreadExecutor().execute(() -> sendMessageInternal(message, photo));
    }

    protected void sendMessageInternal(final String message, final File photo) {
        LOG.info("Sending message: {}", message);
        try {
            final JsonNode response = sendRoomMessage(new MatrixTextMessage(message));
            LOG.info("Got json response: {}", response.toString());

            if (photo != null) {
                sendPhoto(photo);
            }

        } catch (final RuntimeException | IOException e) {
            LOG.warn("Error sending MatrixMonitor message", e);
        }
    }

    private JsonNode sendRoomMessage(final Object message) throws IOException {
        return this.webClient.post()
                .uri(roomUrl + "?access_token=" + accessToken)
                .contentType(MediaType.APPLICATION_JSON_UTF8)
                .bodyValue(message)
                .retrieve()
                .bodyToMono(JsonNode.class)
                .block();
    }

    private void sendPhoto(final File photo) throws IOException {
        final MatrixUploadResponse matrixUploadResponse = this.webClient.post()
                .uri(uploadUrl + "?filename" + photo.getName() + "&access_token=" + accessToken)
                .contentType(ContentType.getByMimeType(ImageUtil.extensionToMimeType(ImageUtil.getExtension(photo.getName())))
                .bodyValue(ImageUtil.scalePhoto(photo, 300))
                .retrieve()
                .bodyToMono(MatrixUploadResponse.class)
                .block();
        LOG.info("Got json response: {}", matrixUploadResponse.contentUri);

        final JsonNode responseImage = sendRoomMessage(new MatrixImageMessage(photo.getName(), matrixUploadResponse.contentUri));
        LOG.info("Got json response: {}", responseImage);
    }

    public void setRoomUrl(final String roomUrl) {
        this.roomUrl = roomUrl;
    }

    public void setUploadUrl(final String uploadUrl) {
        this.uploadUrl = uploadUrl;
    }

    public void setAccessToken(final String accessToken) {
        this.accessToken = accessToken;
    }

    private static class MatrixTextMessage {

        private final String msgtype = "m.text";
        private final String body;

        private MatrixTextMessage(final String body) {
            this.body = body;
        }

        public String getBody() {
            return body;
        }

        public String getMsgtype() {
            return msgtype;
        }

    }

    private static class MatrixImageMessage {

        private final String msgtype = "m.image";
        private final String body;
        private final String url;

        private MatrixImageMessage(final String body, final String url) {
            this.body = body;
            this.url = url;
        }

        public String getBody() {
            return body;
        }

        public String getMsgtype() {
            return msgtype;
        }

        public String getUrl() {
            return url;
        }
    }

    private static class MatrixUploadResponse {
        @JsonProperty("content_uri")
        private String contentUri;
    }

}
