package org.railwaystations.rsapi.resources;

import org.railwaystations.rsapi.StationsRepository;
import org.railwaystations.rsapi.model.Station;
import org.railwaystations.rsapi.writer.StationsGpxWriter;
import org.railwaystations.rsapi.writer.StationsTxtWriter;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
public class StationsResource {

    private static final String COUNTRY = "country";
    private static final String PHOTOGRAPHER = "photographer";
    private static final String HAS_PHOTO = "hasPhoto";
    private static final String MAX_DISTANCE = "maxDistance";
    private static final String LAT = "lat";
    private static final String LON = "lon";
    private static final String ID = "id";
    private static final String ACTIVE = "active";
    private static final String SINCE_HOURS = "sinceHours";

    private static final int HOURS_IN_MILLIS = 1000 * 60 * 60;

    private final StationsRepository repository;

    public StationsResource(final StationsRepository repository) {
        this.repository = repository;
    }

    @GetMapping(produces = {MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8", StationsGpxWriter.GPX_MEDIA_TYPE_VALUE,
            MediaType.TEXT_PLAIN_VALUE + ";charset=UTF-8"}, value = "/stations")
    public List<Station> get(@RequestParam(value = StationsResource.COUNTRY, required = false) final Set<String> countries,
                             @RequestParam(value = StationsResource.HAS_PHOTO, required = false) final Boolean hasPhoto,
                             @RequestParam(value = StationsResource.PHOTOGRAPHER, required = false) final String photographer,
                             @RequestParam(value = StationsResource.MAX_DISTANCE, required = false) final Integer maxDistance,
                             @RequestParam(value = StationsResource.LAT, required = false) final Double lat,
                             @RequestParam(value = StationsResource.LON, required = false) final Double lon,
                             @RequestParam(value = StationsResource.ACTIVE, required = false) final Boolean active) {
        // TODO: can we search this on the DB?
        return getStationsMap(countries)
                .values().stream().filter(station -> station.appliesTo(hasPhoto, photographer, maxDistance, lat, lon, active)).collect(Collectors.toList());
    }

    @GetMapping(produces = {MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8"}, value = "/stations.json")
    public List<Station> getAsJson(@RequestParam(value = StationsResource.COUNTRY, required = false) final Set<String> countries,
                                  @RequestParam(value = StationsResource.HAS_PHOTO, required = false) final Boolean hasPhoto,
                                  @RequestParam(value = StationsResource.PHOTOGRAPHER, required = false) final String photographer,
                                  @RequestParam(value = StationsResource.MAX_DISTANCE, required = false) final Integer maxDistance,
                                  @RequestParam(value = StationsResource.LAT, required = false) final Double lat,
                                  @RequestParam(value = StationsResource.LON, required = false) final Double lon,
                                  @RequestParam(value = StationsResource.ACTIVE, required = false) final Boolean active) {
        return get(countries, hasPhoto, photographer, maxDistance, lat, lon, active);
    }

    @GetMapping(produces = {MediaType.TEXT_PLAIN_VALUE + ";charset=UTF-8"}, value = "/stations.txt")
    public List<Station> getAsTxt(@RequestParam(value = StationsResource.COUNTRY, required = false) final Set<String> countries,
                             @RequestParam(value = StationsResource.HAS_PHOTO, required = false) final Boolean hasPhoto,
                             @RequestParam(value = StationsResource.PHOTOGRAPHER, required = false) final String photographer,
                             @RequestParam(value = StationsResource.MAX_DISTANCE, required = false) final Integer maxDistance,
                             @RequestParam(value = StationsResource.LAT, required = false) final Double lat,
                             @RequestParam(value = StationsResource.LON, required = false) final Double lon,
                             @RequestParam(value = StationsResource.ACTIVE, required = false) final Boolean active) {
        return get(countries, hasPhoto, photographer, maxDistance, lat, lon, active);
    }

    @GetMapping(produces = {StationsGpxWriter.GPX_MEDIA_TYPE_VALUE}, value = "/stations.gpx")
    public List<Station> getAsGpx(@RequestParam(value = StationsResource.COUNTRY, required = false) final Set<String> countries,
                             @RequestParam(value = StationsResource.HAS_PHOTO, required = false) final Boolean hasPhoto,
                             @RequestParam(value = StationsResource.PHOTOGRAPHER, required = false) final String photographer,
                             @RequestParam(value = StationsResource.MAX_DISTANCE, required = false) final Integer maxDistance,
                             @RequestParam(value = StationsResource.LAT, required = false) final Double lat,
                             @RequestParam(value = StationsResource.LON, required = false) final Double lon,
                             @RequestParam(value = StationsResource.ACTIVE, required = false) final Boolean active) {
        return get(countries, hasPhoto, photographer, maxDistance, lat, lon, active);
    }

    @GetMapping(produces = {MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8", StationsGpxWriter.GPX_MEDIA_TYPE_VALUE,
            MediaType.TEXT_PLAIN_VALUE + ";charset=UTF-8"}, value = "/{country}/stations")
    public List<Station> getWithCountry(@PathVariable(StationsResource.COUNTRY) final String country,
                                        @RequestParam(value = StationsResource.HAS_PHOTO, required = false) final Boolean hasPhoto,
                                        @RequestParam(value = StationsResource.PHOTOGRAPHER, required = false) final String photographer,
                                        @RequestParam(value = StationsResource.MAX_DISTANCE, required = false) final Integer maxDistance,
                                        @RequestParam(value = StationsResource.LAT, required = false) final Double lat,
                                        @RequestParam(value = StationsResource.LON, required = false) final Double lon,
                                        @RequestParam(value = StationsResource.ACTIVE, required = false) final Boolean active) {
        return get(Collections.singleton(country), hasPhoto, photographer, maxDistance, lat, lon, active);
    }

    @GetMapping(produces = {MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8"}, value = "/{country}/stations.json")
    public List<Station> getWithCountryAsJson(@PathVariable(StationsResource.COUNTRY) final String country,
                                              @RequestParam(value = StationsResource.HAS_PHOTO, required = false) final Boolean hasPhoto,
                                              @RequestParam(value = StationsResource.PHOTOGRAPHER, required = false) final String photographer,
                                              @RequestParam(value = StationsResource.MAX_DISTANCE, required = false) final Integer maxDistance,
                                              @RequestParam(value = StationsResource.LAT, required = false) final Double lat,
                                              @RequestParam(value = StationsResource.LON, required = false) final Double lon,
                                              @RequestParam(value = StationsResource.ACTIVE, required = false) final Boolean active) {
        return getWithCountry(country, hasPhoto, photographer, maxDistance, lat, lon, active);
    }

    @GetMapping(produces = {MediaType.TEXT_PLAIN_VALUE + ";charset=UTF-8"}, value = "/{country}/stations.txt")
    public List<Station> getWithCountryAsText(@PathVariable(StationsResource.COUNTRY) final String country,
                                        @RequestParam(value = StationsResource.HAS_PHOTO, required = false) final Boolean hasPhoto,
                                        @RequestParam(value = StationsResource.PHOTOGRAPHER, required = false) final String photographer,
                                        @RequestParam(value = StationsResource.MAX_DISTANCE, required = false) final Integer maxDistance,
                                        @RequestParam(value = StationsResource.LAT, required = false) final Double lat,
                                        @RequestParam(value = StationsResource.LON, required = false) final Double lon,
                                        @RequestParam(value = StationsResource.ACTIVE, required = false) final Boolean active) {
        return getWithCountry(country, hasPhoto, photographer, maxDistance, lat, lon, active);
    }

    @GetMapping(produces = {StationsGpxWriter.GPX_MEDIA_TYPE_VALUE}, value = "/{country}/stations.gpx")
    public List<Station> getWithCountryAsGpx(@PathVariable(StationsResource.COUNTRY) final String country,
                                              @RequestParam(value = StationsResource.HAS_PHOTO, required = false) final Boolean hasPhoto,
                                              @RequestParam(value = StationsResource.PHOTOGRAPHER, required = false) final String photographer,
                                              @RequestParam(value = StationsResource.MAX_DISTANCE, required = false) final Integer maxDistance,
                                              @RequestParam(value = StationsResource.LAT, required = false) final Double lat,
                                              @RequestParam(value = StationsResource.LON, required = false) final Double lon,
                                              @RequestParam(value = StationsResource.ACTIVE, required = false) final Boolean active) {
        return getWithCountry(country, hasPhoto, photographer, maxDistance, lat, lon, active);
    }

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8", value = "/{country}/stations/{id}")
    public Station getById(@PathVariable(StationsResource.COUNTRY) final String country,
                           @PathVariable(StationsResource.ID) final String id) {
        final Station station = getStationsMap(Collections.singleton(country)).get(new Station.Key(country, id));
        if (station == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        return station;
    }

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8", value = "/recentPhotoImports")
    public List<Station> recentPhotoImports(@RequestParam(value = StationsResource.SINCE_HOURS, required = false)  @DefaultValue("10") final long sinceHours) {
        return repository.findRecentImports(System.currentTimeMillis() - (HOURS_IN_MILLIS * sinceHours));
    }

    private Map<Station.Key, Station> getStationsMap(final Set<String> countries) {
        final Map<Station.Key, Station> stationMap = repository.getStationsByCountry(countries);
        if (stationMap.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        return stationMap;
    }

}
