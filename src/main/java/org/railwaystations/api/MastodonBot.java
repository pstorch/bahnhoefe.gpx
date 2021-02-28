package org.railwaystations.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.railwaystations.api.model.InboxEntry;
import org.railwaystations.api.model.Station;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class MastodonBot {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static final Logger LOG = LoggerFactory.getLogger(MastodonBot.class);

    private String instanceUrl;
    private String token;
    private String stationUrl;

    private final CloseableHttpClient httpclient;

    public MastodonBot() {
        super();
        this.httpclient = HttpClients.custom().setDefaultRequestConfig(
                RequestConfig.custom()
                        .setSocketTimeout(5000)
                        .setConnectTimeout(5000)
                        .setConnectionRequestTimeout(5000).build()
        ).build();
    }

    public String getInstanceUrl() {
        return instanceUrl;
    }

    public void setInstanceUrl(final String instanceUrl) {
        this.instanceUrl = instanceUrl;
    }

    public String getToken() {
        return token;
    }

    public void setToken(final String token) {
        this.token = token;
    }

    public void tootNewPhoto(final Station station, final InboxEntry inboxEntry) {
        if (StringUtils.isBlank(instanceUrl) || StringUtils.isBlank(token)) {
            LOG.info("New photo for Station {} not tooted", station.getKey());
            return;
        }
        LOG.info("Sending toot for new photo of : {}", station.getKey());
        new Thread(() -> {
            try {
                String status = String.format("%s%nby %s%n%s?countryCode=%s&stationId=%s",
                        station.getTitle(), station.getPhotographer(), stationUrl,
                        station.getKey().getCountry(), station.getKey().getId());
                if (StringUtils.isNotBlank(inboxEntry.getComment())) {
                    status += String.format("%n%s", inboxEntry.getComment());
                }
                final String json = MAPPER.writeValueAsString(new Toot(status));
                final HttpPost httpPost = new HttpPost(instanceUrl + "/api/v1/statuses");
                httpPost.setEntity(new StringEntity(json, ContentType.APPLICATION_JSON.withCharset("UTF-8")));
                httpPost.setHeader("Authorization", "Bearer " + token);
                final CloseableHttpResponse response = httpclient.execute(httpPost);
                final int statusCode = response.getStatusLine().getStatusCode();
                final String content = EntityUtils.toString(response.getEntity());
                if (statusCode >= 200 && statusCode < 300) {
                    LOG.info("Got json response from {}: {}", httpPost.getURI(), content);
                } else {
                    LOG.error("Error reading json from {}, status {}: {}", httpPost.getURI(), status, content);
                }
            } catch (final RuntimeException | IOException e) {
                LOG.error("Error sending Toot", e);
            }
        }).start();
    }

    public String getStationUrl() {
        return stationUrl;
    }

    public void setStationUrl(final String stationUrl) {
        this.stationUrl = stationUrl;
    }

    public static class Toot {
        private final String status;

        public Toot(final String status) {
            this.status = status;
        }

        public String getStatus() {
            return status;
        }
    }

}
