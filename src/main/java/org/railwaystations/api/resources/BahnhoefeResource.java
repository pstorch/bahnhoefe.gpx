package org.railwaystations.api.resources;

import org.railwaystations.api.BahnhoefeRepository;
import org.railwaystations.api.model.Bahnhof;
import org.railwaystations.api.writer.BahnhoefeGpxWriter;
import org.railwaystations.api.writer.BahnhoefeTxtWriter;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Path("/")
public class BahnhoefeResource {

    private static final String COUNTRY = "country";
    private static final String PHOTOGRAPHER = "photographer";
    private static final String HAS_PHOTO = "hasPhoto";
    private static final String MAX_DISTANCE = "maxDistance";
    private static final String LAT = "lat";
    private static final String LON = "lon";
    private static final String ID = "id";

    private final BahnhoefeRepository repository;

    public BahnhoefeResource(final BahnhoefeRepository repository) {
        this.repository = repository;
    }

    @GET
    @Path("stations")
    @Produces({MediaType.APPLICATION_JSON + ";charset=UTF-8", BahnhoefeGpxWriter.GPX_MIME_TYPE,
            BahnhoefeTxtWriter.TEXT_PLAIN + ";charset=UTF-8"})
    public List<Bahnhof> get(@QueryParam(BahnhoefeResource.COUNTRY) final String country,
                             @QueryParam(BahnhoefeResource.HAS_PHOTO) final Boolean hasPhoto,
                             @QueryParam(BahnhoefeResource.PHOTOGRAPHER) final String photographer,
                             @QueryParam(BahnhoefeResource.MAX_DISTANCE) final Integer maxDistance,
                             @QueryParam(BahnhoefeResource.LAT) final Double lat,
                             @QueryParam(BahnhoefeResource.LON) final Double lon) throws IOException {
        return getWithCountry(country, hasPhoto, photographer, maxDistance, lat, lon);
    }

    @GET
    @Path("{country}/stations")
    @Produces({MediaType.APPLICATION_JSON + ";charset=UTF-8", BahnhoefeGpxWriter.GPX_MIME_TYPE,
            BahnhoefeTxtWriter.TEXT_PLAIN + ";charset=UTF-8"})
    public List<Bahnhof> getWithCountry(@PathParam(BahnhoefeResource.COUNTRY) final String country,
                             @QueryParam(BahnhoefeResource.HAS_PHOTO) final Boolean hasPhoto,
                             @QueryParam(BahnhoefeResource.PHOTOGRAPHER) final String photographer,
                             @QueryParam(BahnhoefeResource.MAX_DISTANCE) final Integer maxDistance,
                             @QueryParam(BahnhoefeResource.LAT) final Double lat,
                             @QueryParam(BahnhoefeResource.LON) final Double lon) throws IOException {
        return getBahnhoefeMap(country)
                .values().stream().filter(bahnhof -> bahnhof.appliesTo(hasPhoto, photographer, maxDistance, lat, lon)).collect(Collectors.toList());
    }

    @GET
    @Path("{country}/stations/{id}")
    @Produces({MediaType.APPLICATION_JSON + ";charset=UTF-8"})
    public Bahnhof getById(@PathParam(BahnhoefeResource.COUNTRY) final String country,
                                        @PathParam(BahnhoefeResource.ID) final Integer id) throws IOException {
        return getBahnhoefeMap(country).get(id);
    }

    private Map<Integer, Bahnhof> getBahnhoefeMap(final String country) {
        final Map<Integer, Bahnhof> bahnhofMap = repository.get(country);
        if (bahnhofMap.isEmpty()) {
            throw new WebApplicationException(404);
        }
        return bahnhofMap;
    }

}
