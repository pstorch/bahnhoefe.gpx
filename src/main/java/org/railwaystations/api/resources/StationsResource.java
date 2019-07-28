package org.railwaystations.api.resources;

import org.railwaystations.api.StationsRepository;
import org.railwaystations.api.model.Station;
import org.railwaystations.api.writer.StationsGpxWriter;
import org.railwaystations.api.writer.StationsTxtWriter;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
    private static final String ACTIVE = "active";

    private final StationsRepository repository;

    public StationsResource(final StationsRepository repository) {
        this.repository = repository;
    }

    @GET
    @Path("stations")
    @Produces({MediaType.APPLICATION_JSON + ";charset=UTF-8", StationsGpxWriter.GPX_MIME_TYPE,
            StationsTxtWriter.TEXT_PLAIN + ";charset=UTF-8"})
    public List<Station> get(@QueryParam(StationsResource.COUNTRY) final Set<String> countries,
                             @QueryParam(StationsResource.HAS_PHOTO) final Boolean hasPhoto,
                             @QueryParam(StationsResource.PHOTOGRAPHER) final String photographer,
                             @QueryParam(StationsResource.MAX_DISTANCE) final Integer maxDistance,
                             @QueryParam(StationsResource.LAT) final Double lat,
                             @QueryParam(StationsResource.LON) final Double lon,
                             @QueryParam(StationsResource.ACTIVE) final Boolean active) {
        // TODO: can we search this on the DB?
        return getStationsMap(countries)
                .values().stream().filter(station -> station.appliesTo(hasPhoto, photographer, maxDistance, lat, lon, active)).collect(Collectors.toList());
    }

    @GET
    @Path("{country}/stations")
    @Produces({MediaType.APPLICATION_JSON + ";charset=UTF-8", StationsGpxWriter.GPX_MIME_TYPE,
            StationsTxtWriter.TEXT_PLAIN + ";charset=UTF-8"})
    public List<Station> getWithCountry(@PathParam(StationsResource.COUNTRY) final String country,
                                        @QueryParam(StationsResource.HAS_PHOTO) final Boolean hasPhoto,
                                        @QueryParam(StationsResource.PHOTOGRAPHER) final String photographer,
                                        @QueryParam(StationsResource.MAX_DISTANCE) final Integer maxDistance,
                                        @QueryParam(StationsResource.LAT) final Double lat,
                                        @QueryParam(StationsResource.LON) final Double lon,
                                        @QueryParam(StationsResource.ACTIVE) final Boolean active) {
        return get(Collections.singleton(country), hasPhoto, photographer, maxDistance, lat, lon, active);
    }

    @GET
    @Path("{country}/stations/{id}")
    @Produces({MediaType.APPLICATION_JSON + ";charset=UTF-8"})
    public Station getById(@PathParam(StationsResource.COUNTRY) final String country,
                           @PathParam(StationsResource.ID) final String id) {
        return getStationsMap(Collections.singleton(country)).get(new Station.Key(country, id));
    }

    private Map<Station.Key, Station> getStationsMap(final Set<String> countries) {
        final Map<Station.Key, Station> stationMap = repository.getStationsByCountry(countries);
        if (stationMap.isEmpty()) {
            throw new WebApplicationException(404);
        }
        return stationMap;
    }

}
