package org.railwaystations.api.resources;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.commons.lang3.StringUtils;
import org.railwaystations.api.BahnhoefeRepository;
import org.railwaystations.api.PhotoImporter;
import org.railwaystations.api.model.Bahnhof;

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

    private final BahnhoefeRepository repository;
    private final String verificationToken;
    private final PhotoImporter photoImporter;

    public SlackCommandResource(final BahnhoefeRepository repository, final String verificationToken, final PhotoImporter photoImporter) {
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
            final List<Bahnhof> bahnhofList = repository.findByName(matcherSearch.group(1));
            if (bahnhofList.size() == 1) {
                return new SlackResponse(ResponseType.in_channel, toMessage(bahnhofList.get(0)));
            } else if (bahnhofList.size() > 1){
                return new SlackResponse(ResponseType.in_channel, toMessage(bahnhofList));
            }
            return new SlackResponse(ResponseType.in_channel, "No stations found");
        }
        final Matcher matcherShow = PATTERN_SHOW.matcher(text);
        if (matcherShow.matches()) {
            final Integer id = Integer.valueOf(matcherShow.group(1));
            final Bahnhof bahnhof = repository.findById(id);
            if (bahnhof == null) {
                return new SlackResponse(ResponseType.in_channel, "Station with id " + id + " not found");
            } else {
                return new SlackResponse(ResponseType.in_channel, toMessage(bahnhof));
            }
        }
        return new SlackResponse(ResponseType.ephimeral, String.format("I understand:%n- '/rsapi refresh'%n- '/rsapi search <station-name>'%n- '/rsapi show <station-id>%n- '/rsapi import'%n"));
    }

    private String toMessage(final List<Bahnhof> bahnhofList) {
        final StringBuilder sb = new StringBuilder(String.format("Found:%n"));
        bahnhofList.forEach(bahnhof -> sb.append(String.format("- %s: %d%n", bahnhof.getTitle(), bahnhof.getId())));
        return sb.toString();
    }

    private String toMessage(final Bahnhof bahnhof) {
        return String.format("Station: %d - %s%nCountry: %s%nLocation: %f,%f%nPhotographer: %s%nLicense: %s%nPhoto: %s%n", bahnhof.getId(), bahnhof.getTitle(), bahnhof.getCountry(), bahnhof.getLat(), bahnhof.getLon(), bahnhof.getPhotographer(), bahnhof.getLicense(), bahnhof.getPhotoUrl());
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
