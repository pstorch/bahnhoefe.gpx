package org.railwaystations.rsapi;

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
import org.railwaystations.rsapi.model.InboxEntry;
import org.railwaystations.rsapi.model.Station;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Service
@SuppressWarnings("PMD.BeanMembersShouldSerialize")
public class MastodonBot {

    private static final Logger LOG = LoggerFactory.getLogger(MastodonBot.class);

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final String token;

    private final String stationUrl;

    private final String instanceUrl;

    private final CloseableHttpClient httpclient;

    public MastodonBot(@Value("${mastodonBot.token}") final String token, @Value("${mastodonBot.stationUrl}") final String stationUrl, @Value("${mastodonBot.instanceUrl}") final String instanceUrl) {
        super();
        this.token = token;
        this.stationUrl = stationUrl;
        this.instanceUrl = instanceUrl;
        this.httpclient = HttpClients.custom().setDefaultRequestConfig(
                RequestConfig.custom()
                        .setSocketTimeout(5000)
                        .setConnectTimeout(5000)
                        .setConnectionRequestTimeout(5000).build()
        ).build();
    }

    public void tootNewPhoto(final Station station, final InboxEntry inboxEntry) {
        if (StringUtils.isBlank(instanceUrl) || StringUtils.isBlank(token) || StringUtils.isBlank(stationUrl)) {
            LOG.info("New photo for Station {} not tooted, {}", station.getKey(), this);
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

    @Override
    public String toString() {
        return "MastodonBot{" +
                "token='" + token + '\'' +
                ", stationUrl='" + stationUrl + '\'' +
                ", instanceUrl='" + instanceUrl + '\'' +
                '}';
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
