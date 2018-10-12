package org.railwaystations.api.resources;

import org.railwaystations.api.StationsRepository;
import org.railwaystations.api.model.Station;
import org.railwaystations.api.writer.StationsGpxWriter;
import org.railwaystations.api.writer.StationsTxtWriter;

import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

@Path("/")
public class StationsResource {

    private static final String COUNTRY = "country";
    private static final String PHOTOGRAPHER = "photographer";
    private static final String HAS_PHOTO = "hasPhoto";
    private static final String MAX_DISTANCE = "maxDistance";
    private static final String LAT = "lat";
    private static final String LON = "lon";
    private static final String ID = "id";
    
    private static final String DEFAULT_LICENSE_LANGUAGE = "en";
    private static final String DEFAULT_LICENSE_URL = "https://creativecommons.org/licenses/";
    private static final Map<String, String> LICENSES = new HashMap<>();

    static {
        LICENSES.put("CC BY 3.0", "https://creativecommons.org/licenses/by/3.0/deed.");
        LICENSES.put("CC BY-NC 4.0 International", "https://creativecommons.org/licenses/by-nc/4.0/deed.");
        LICENSES.put("CC BY-NC-SA 3.0 DE", "https://creativecommons.org/licenses/by-nc-sa/3.0/de/deed.");
        LICENSES.put("CC BY-SA 4.0", "https://creativecommons.org/licenses/by-sa/4.0/deed.");
        LICENSES.put("CC0 1.0 Universell (CC0 1.0)", "https://creativecommons.org/publicdomain/zero/1.0/deed.");
    }

    private final StationsRepository repository;

    public StationsResource(final StationsRepository repository) {
        this.repository = repository;
    }

    @GET
    @Path("stations")
    @Produces({MediaType.APPLICATION_JSON + ";charset=UTF-8", StationsGpxWriter.GPX_MIME_TYPE,
            StationsTxtWriter.TEXT_PLAIN + ";charset=UTF-8"})
    public List<Station> get(@Context final HttpHeaders httpHeaders,
                             @QueryParam(StationsResource.COUNTRY) final String country,
                             @QueryParam(StationsResource.HAS_PHOTO) final Boolean hasPhoto,
                             @QueryParam(StationsResource.PHOTOGRAPHER) final String photographer,
                             @QueryParam(StationsResource.MAX_DISTANCE) final Integer maxDistance,
                             @QueryParam(StationsResource.LAT) final Double lat,
                             @QueryParam(StationsResource.LON) final Double lon) {
        return getWithCountry(httpHeaders, country, hasPhoto, photographer, maxDistance, lat, lon);
    }

    @GET
    @Path("{country}/stations")
    @Produces({MediaType.APPLICATION_JSON + ";charset=UTF-8", StationsGpxWriter.GPX_MIME_TYPE,
            StationsTxtWriter.TEXT_PLAIN + ";charset=UTF-8"})
    public List<Station> getWithCountry(@Context final HttpHeaders httpHeaders,
                                        @PathParam(StationsResource.COUNTRY) final String country,
                                        @QueryParam(StationsResource.HAS_PHOTO) final Boolean hasPhoto,
                                        @QueryParam(StationsResource.PHOTOGRAPHER) final String photographer,
                                        @QueryParam(StationsResource.MAX_DISTANCE) final Integer maxDistance,
                                        @QueryParam(StationsResource.LAT) final Double lat,
                                        @QueryParam(StationsResource.LON) final Double lon) {
        final String language = getClientLanguage(httpHeaders);
        return getStationsMap(country)
                .values()
                .stream()
                .filter(station -> station.appliesTo(hasPhoto, photographer, maxDistance, lat, lon))
                .map(station-> {
                    String licenseUrl = getLicenseUrl(language, station.getLicense());
                    station.setLicenseUrl(licenseUrl);
                    return station;
                })
                .collect(Collectors.toList());
    }

    @GET
    @Path("{country}/stations/{id}")
    @Produces({MediaType.APPLICATION_JSON + ";charset=UTF-8"})
    public Station getById(@Context final HttpHeaders httpHeaders,
                           @PathParam(StationsResource.COUNTRY) final String country,
                           @PathParam(StationsResource.ID) final String id) {
        final Station station = getStationsMap(country).get(new Station.Key(country, id));
        final String language = getClientLanguage(httpHeaders);
        final String licenseUrl = getLicenseUrl(language, station.getLicense());
        station.setLicenseUrl(licenseUrl);
        return station;
    }

    private Map<Station.Key, Station> getStationsMap(final String country) {
        final Map<Station.Key, Station> stationMap = repository.get(country);
        if (stationMap.isEmpty()) {
            throw new WebApplicationException(404);
        }
        return stationMap;
    }

    private String getClientLanguage(final HttpHeaders httpHeaders) {
        if (httpHeaders.getAcceptableLanguages().isEmpty()) {
            return DEFAULT_LICENSE_LANGUAGE;
        } 
        final Locale locale = httpHeaders.getAcceptableLanguages().get(0);
        return locale.getLanguage();
    }
    
    private String getLicenseUrl(final String language, final String licenseName) {
        String licenseUrl = LICENSES.get(licenseName);
        if (licenseUrl == null) {
            licenseUrl = DEFAULT_LICENSE_URL;
        } else {
            licenseUrl += language;
        }
        return licenseUrl;
    }
}
