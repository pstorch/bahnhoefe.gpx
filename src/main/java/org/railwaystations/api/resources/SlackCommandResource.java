package org.railwaystations.api.resources;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.commons.lang3.StringUtils;
import org.railwaystations.api.PhotoImporter;
import org.railwaystations.api.StationsRepository;
import org.railwaystations.api.model.Station;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Path("/")
public class SlackCommandResource {

    private static final Pattern PATTERN_SEARCH = Pattern.compile("\\s*search\\s*(.*)");
    private static final Pattern PATTERN_SHOW = Pattern.compile("\\s*show\\s\\s*([a-z]{1,3})\\s\\s*(\\w*)");
    private static final Pattern PATTERN_SHOW_LEGACY = Pattern.compile("\\s*show\\s\\s*(\\d*)");

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
        if (StringUtils.equals("import", text)) {
            photoImporter.importPhotosAsync();
            return new SlackResponse(ResponseType.in_channel, "Importing photos");
        }
        final Matcher matcherSearch = PATTERN_SEARCH.matcher(text);
        if (matcherSearch.matches()) {
            final Map<Station.Key, String> stations = repository.findByName(matcherSearch.group(1));
            if (!stations.isEmpty()){
                return new SlackResponse(ResponseType.in_channel, toMessage(stations));
            }
            return new SlackResponse(ResponseType.in_channel, "No stations found");
        }
        final Matcher matcherShow = PATTERN_SHOW.matcher(text);
        if (matcherShow.matches()) {
            return showStation(matcherShow.group(1).toLowerCase(Locale.ENGLISH), matcherShow.group(2));
        }
        final Matcher matcherShowLegacy = PATTERN_SHOW_LEGACY.matcher(text);
        if (matcherShowLegacy.matches()) {
            return showStation(null, matcherShowLegacy.group(1));
        }
        return new SlackResponse(ResponseType.ephimeral, String.format("I understand:%n- '/rsapi search <station-name>'%n- '/rsapi show <country-code> <station-id>%n- '/rsapi import'%n"));
    }

    private SlackResponse showStation(final String country, final String id) {
        final Station.Key key = new Station.Key(country, id);
        final Station station = repository.findByKey(key);
        if (station == null) {
            return new SlackResponse(ResponseType.in_channel, "Station with " + key + " not found");
        } else {
            return new SlackResponse(ResponseType.in_channel, toMessage(station));
        }
    }

    private String toMessage(final Map<Station.Key, String> stations) {
        final StringBuilder sb = new StringBuilder(String.format("Found:%n"));
        stations.keySet().forEach(station -> sb.append(String.format("- %s: %s%n", stations.get(station), station)));
        return sb.toString();
    }

    private String toMessage(final Station station) {
        return String.format("Station: %s - %s%nCountry: %s%nLocation: %f,%f%nPhotographer: %s%nLicense: %s%nPhoto: %s%n", station.getKey().getId(), station.getTitle(), station.getKey().getCountry(), station.getCoordinates().getLat(), station.getCoordinates().getLon(), station.getPhotographer(), station.getLicense(), station.getPhotoUrl());
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
     *          “text”:”To be fed, use `/please feed` to request food. We hear the elf needs food badly.\nTo tease, use `/please tease` &mdash; we always knew you liked noogies.\nYou’ve already learned how to getStationsByCountry help with `/please help`.”
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
