package org.railwaystations.api.resources;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.commons.lang3.StringUtils;
import org.railwaystations.api.StationsRepository;
import org.railwaystations.api.PhotoImporter;
import org.railwaystations.api.model.Station;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Path("/")
public class SlackCommandResource {

    private static final Pattern PATTERN_SEARCH = Pattern.compile("\\s*search\\s*(.*)");
    private static final Pattern PATTERN_SHOW = Pattern.compile("\\s*show\\s\\s*(\\d*)");

    private final StationsRepository repository;
    private final String verificationToken;
    private final PhotoImporter photoImporter;

    public SlackCommandResource(final StationsRepository repository, final String verificationToken, final PhotoImporter photoImporter) {
        this.repository = repository;
        this.verificationToken = verificationToken;
        this.photoImporter = photoImporter;
    }

    @POST
    @Path("slack")
    @Produces({MediaType.APPLICATION_JSON + ";charset=UTF-8"})
    public SlackResponse command(@FormParam("token") final String token,
                                 @FormParam("text") final String text,
                                 @FormParam("response_url") final String responseUrl) {
        if (!StringUtils.equals(verificationToken, token)) {
            throw new WebApplicationException(Response.Status.UNAUTHORIZED);
        }
        if (StringUtils.equals("refresh", text)) {
            repository.refresh(responseUrl);
            return new SlackResponse(ResponseType.in_channel, "Refreshing caches");
        }
        if (StringUtils.equals("import", text)) {
            final Map<String, String> report = photoImporter.importPhotos();
            return new SlackResponse(ResponseType.in_channel, photoImporter.reportToMessage(report));
        }
        final Matcher matcherSearch = PATTERN_SEARCH.matcher(text);
        if (matcherSearch.matches()) {
            final List<Station> stationList = repository.findByName(matcherSearch.group(1));
            if (stationList.size() == 1) {
                return new SlackResponse(ResponseType.in_channel, toMessage(stationList.get(0)));
            } else if (stationList.size() > 1){
                return new SlackResponse(ResponseType.in_channel, toMessage(stationList));
            }
            return new SlackResponse(ResponseType.in_channel, "No stations found");
        }
        final Matcher matcherShow = PATTERN_SHOW.matcher(text);
        if (matcherShow.matches()) {
            final Integer id = Integer.valueOf(matcherShow.group(1));
            final Station station = repository.findById(id);
            if (station == null) {
                return new SlackResponse(ResponseType.in_channel, "Station with id " + id + " not found");
            } else {
                return new SlackResponse(ResponseType.in_channel, toMessage(station));
            }
        }
        return new SlackResponse(ResponseType.ephimeral, String.format("I understand:%n- '/rsapi refresh'%n- '/rsapi search <station-name>'%n- '/rsapi show <station-id>%n- '/rsapi import'%n"));
    }

    private String toMessage(final List<Station> stationList) {
        final StringBuilder sb = new StringBuilder(String.format("Found:%n"));
        stationList.forEach(station -> sb.append(String.format("- %s: %d%n", station.getTitle(), station.getId())));
        return sb.toString();
    }

    private String toMessage(final Station station) {
        return String.format("Station: %d - %s%nCountry: %s%nLocation: %f,%f%nPhotographer: %s%nLicense: %s%nPhoto: %s%n", station.getId(), station.getTitle(), station.getCountry(), station.getLat(), station.getLon(), station.getPhotographer(), station.getLicense(), station.getPhotoUrl());
    }


    public enum ResponseType {
        ephimeral,
        in_channel
    }

    /**
     * <div>
     *    {
     *     “response_type”: “ephemeral”,
     *     “text”: “How to use /please”,
     *     “attachments”:[
     *          {
     *          “text”:”To be fed, use `/please feed` to request food. We hear the elf needs food badly.\nTo tease, use `/please tease` &mdash; we always knew you liked noogies.\nYou’ve already learned how to get help with `/please help`.”
     *          }
     *      ]
     *    }
     * </div>
     */
    private static class SlackResponse {
        @JsonProperty("response_type")
        private final ResponseType responseType;

        @JsonProperty("text")
        private final String text;

        private SlackResponse(final ResponseType responseType, final String text) {
            this.responseType = responseType;
            this.text = text;
        }

        public String getText() {
            return text;
        }

        public ResponseType getResponseType() {
            return responseType;
        }
    }

}
