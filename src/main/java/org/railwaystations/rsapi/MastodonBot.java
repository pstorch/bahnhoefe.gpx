package org.railwaystations.rsapi;

import com.fasterxml.jackson.databind.JsonNode;
import org.apache.commons.lang3.StringUtils;
import org.railwaystations.rsapi.model.InboxEntry;
import org.railwaystations.rsapi.model.Station;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

@Service
public class MastodonBot {

    private static final Logger LOG = LoggerFactory.getLogger(MastodonBot.class);

    @Value("${mastodonBot.token}")
    private String token;

    @Value("${mastodonBot.stationUrl}")
    private String stationUrl;

    private final WebClient webClient;

    public MastodonBot(final WebClient.Builder webClientBuilder, @Value("${mastodonBot.instanceUrl}") final String instanceUrl) {
        super();
        this.webClient = webClientBuilder.clone().baseUrl(instanceUrl).build();
    }

    public void tootNewPhoto(final Station station, final InboxEntry inboxEntry) {
        if (StringUtils.isBlank(token)) {
            LOG.info("New photo for Station {} not tooted", station.getKey());
            return;
        }
        LOG.info("Sending toot for new photo of : {}", station.getKey());
        String status = String.format("%s%nby %s%n%s?countryCode=%s&stationId=%s",
                station.getTitle(), station.getPhotographer(), stationUrl,
                station.getKey().getCountry(), station.getKey().getId());
        if (StringUtils.isNotBlank(inboxEntry.getComment())) {
            status += String.format("%n%s", inboxEntry.getComment());
        }
        this.webClient.post()
                .uri("/api/v1/statuses")
                .contentType(MediaType.APPLICATION_JSON_UTF8)
                .header("Authorization", "Bearer " + token)
                .bodyValue(new Toot(status))
                .retrieve()
                .bodyToMono(JsonNode.class)
                .doOnError(error -> {
                    LOG.error("Error sending Toot", error);
                })
                .subscribe(response -> {
                    LOG.info("Got json response: {}", response.toString());
                });
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
