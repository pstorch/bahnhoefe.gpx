package org.railwaystations.api.monitoring;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.concurrent.Executors;

public class MatrixMonitor implements Monitor {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final Logger LOG = LoggerFactory.getLogger(MatrixMonitor.class);

    private final CloseableHttpClient httpclient;
    private final String url;


    public MatrixMonitor(final String url) {
        super();
        this.httpclient = HttpClients.custom().setDefaultRequestConfig(
                RequestConfig.custom()
                        .setSocketTimeout(5000)
                        .setConnectTimeout(5000)
                        .setConnectionRequestTimeout(10000).build()
        ).build();
        this.url = url;
    }

    public void sendMessage(final String message) {
        Executors.newSingleThreadExecutor().execute(() -> sendMessageInternal(message, url));
    }

    protected void sendMessageInternal(final String message, final String url) {
        LOG.info("Sending message: {}", message);
        try {
            final String json = MAPPER.writeValueAsString(new MatrixMessage(message));
            final HttpPost httpPost = new HttpPost(url);
            httpPost.setEntity(new StringEntity(json, ContentType.APPLICATION_JSON.withCharset("UTF-8")));
            final CloseableHttpResponse response = httpclient.execute(httpPost);
            final int status = response.getStatusLine().getStatusCode();
            final String content = EntityUtils.toString(response.getEntity());
            if (status >= 200 && status < 300) {
                LOG.info("Got json response: {}", content);
            } else {
                LOG.error("Error reading json, status {}: {}", status, content);
            }
        } catch (final RuntimeException | IOException e) {
            LOG.warn("Error sending MatrixMonitor message", e);
        }
    }

    private static class MatrixMessage {

        private final String msgtype = "m.text";
        private final String body;

        private MatrixMessage(final String body) {
            this.body = body;
        }

        public String getBody() {
            return body;
        }

        public String getMsgtype() {
            return msgtype;
        }

    }

}
