package github.pstorch.bahnhoefe.service.resources;

import github.pstorch.bahnhoefe.service.BahnhoefeRepository;
import github.pstorch.bahnhoefe.service.model.Bahnhof;
import github.pstorch.bahnhoefe.service.loader.BahnhoefeLoaderDe;
import github.pstorch.bahnhoefe.service.writer.BahnhoefeGpxWriter;
import github.pstorch.bahnhoefe.service.writer.BahnhoefeTxtWriter;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Path("/")
@Produces(MediaType.APPLICATION_JSON)
public class BahnhoefeResource {

    private static final String DEFAULT_COUNTRY = BahnhoefeLoaderDe.COUNTRY_CODE;
    private static final String COUNTRY = "country";
    private static final String PHOTOGRAPHER = "photographer";
    private static final String HAS_PHOTO = "hasPhoto";
    private static final String MAX_DISTANCE = "maxDistance";
    private static final String LAT = "lat";
    private static final String LON = "lon";

    private final BahnhoefeRepository repository;

    public BahnhoefeResource(final BahnhoefeRepository repository) {
        this.repository = repository;
    }

    @GET
    @Path("{a:bahnhoefe|stations}")
    @Produces({MediaType.APPLICATION_JSON, BahnhoefeGpxWriter.GPX_MIME_TYPE,
            BahnhoefeTxtWriter.TEXT_PLAIN})
    public List<Bahnhof> get(@QueryParam(BahnhoefeResource.HAS_PHOTO) final Boolean hasPhoto,
                             @QueryParam(BahnhoefeResource.PHOTOGRAPHER) final String photographer,
                             @QueryParam(BahnhoefeResource.MAX_DISTANCE) final Integer maxDistance,
                             @QueryParam(BahnhoefeResource.LAT) final Double lat,
                             @QueryParam(BahnhoefeResource.LON) final Double lon) throws IOException {
        return get(BahnhoefeResource.DEFAULT_COUNTRY, hasPhoto, photographer, maxDistance, lat, lon);
    }

    @GET
    @Path("{country}/{a:bahnhoefe|stations}")
    @Produces({MediaType.APPLICATION_JSON, BahnhoefeGpxWriter.GPX_MIME_TYPE,
            BahnhoefeTxtWriter.TEXT_PLAIN})
    public List<Bahnhof> get(@PathParam(BahnhoefeResource.COUNTRY) final String country,
                             @QueryParam(BahnhoefeResource.HAS_PHOTO) final Boolean hasPhoto,
                             @QueryParam(BahnhoefeResource.PHOTOGRAPHER) final String photographer,
                             @QueryParam(BahnhoefeResource.MAX_DISTANCE) final Integer maxDistance,
                             @QueryParam(BahnhoefeResource.LAT) final Double lat,
                             @QueryParam(BahnhoefeResource.LON) final Double lon) throws IOException {
        return getBahnhoefeMap(country)
                .values().stream().filter(bahnhof -> bahnhof.appliesTo(hasPhoto, photographer, maxDistance, lat, lon)).collect(Collectors.toList());
    }

    @GET
    @Path("bahnhoefe-withPhoto")
    @Produces({MediaType.APPLICATION_JSON, BahnhoefeGpxWriter.GPX_MIME_TYPE,
            BahnhoefeTxtWriter.TEXT_PLAIN})
    @Deprecated
    public List<Bahnhof> getWithPhoto(@QueryParam(BahnhoefeResource.PHOTOGRAPHER) final String photographer,
                                      @QueryParam(BahnhoefeResource.MAX_DISTANCE) final Integer maxDistance,
                                      @QueryParam(BahnhoefeResource.LAT) final Double lat,
                                      @QueryParam(BahnhoefeResource.LON) final Double lon) throws IOException {
        return get(BahnhoefeResource.DEFAULT_COUNTRY, true, photographer, maxDistance, lat, lon);
    }

    @GET
    @Path("{country}/bahnhoefe-withPhoto")
    @Produces({MediaType.APPLICATION_JSON, BahnhoefeGpxWriter.GPX_MIME_TYPE,
            BahnhoefeTxtWriter.TEXT_PLAIN})
    @Deprecated
    public List<Bahnhof> getWithPhoto(@PathParam(BahnhoefeResource.COUNTRY) final String country,
                                      @QueryParam(BahnhoefeResource.PHOTOGRAPHER) final String photographer,
                                      @QueryParam(BahnhoefeResource.MAX_DISTANCE) final Integer maxDistance,
                                      @QueryParam(BahnhoefeResource.LAT) final Double lat,
                                      @QueryParam(BahnhoefeResource.LON) final Double lon) throws IOException {
        return get(country, true, photographer, maxDistance, lat, lon);
    }

    @GET
    @Path("bahnhoefe-withoutPhoto")
    @Produces({MediaType.APPLICATION_JSON, BahnhoefeGpxWriter.GPX_MIME_TYPE,
            BahnhoefeTxtWriter.TEXT_PLAIN})
    @Deprecated
    public List<Bahnhof> getWithoutPhoto(@QueryParam(BahnhoefeResource.PHOTOGRAPHER) final String photographer,
                                         @QueryParam(BahnhoefeResource.MAX_DISTANCE) final Integer maxDistance,
                                         @QueryParam(BahnhoefeResource.LAT) final Double lat,
                                         @QueryParam(BahnhoefeResource.LON) final Double lon) throws IOException {
        return get(BahnhoefeResource.DEFAULT_COUNTRY, false, photographer, maxDistance, lat, lon);
    }

    @GET
    @Path("{country}/bahnhoefe-withoutPhoto")
    @Produces({MediaType.APPLICATION_JSON, BahnhoefeGpxWriter.GPX_MIME_TYPE,
            BahnhoefeTxtWriter.TEXT_PLAIN})
    @Deprecated
    public List<Bahnhof> getWithoutPhoto(@PathParam(BahnhoefeResource.COUNTRY) final String country,
                                         @QueryParam(BahnhoefeResource.PHOTOGRAPHER) final String photographer,
                                         @QueryParam(BahnhoefeResource.MAX_DISTANCE) final Integer maxDistance,
                                         @QueryParam(BahnhoefeResource.LAT) final Double lat,
                                         @QueryParam(BahnhoefeResource.LON) final Double lon) throws IOException {
        return get(country, false, photographer, maxDistance, lat, lon);
    }

    private Map<Integer, Bahnhof> getBahnhoefeMap(@PathParam(BahnhoefeResource.COUNTRY) final String country) {
        final Map<Integer, Bahnhof> bahnhofMap = repository.get(country);
        if (bahnhofMap.isEmpty()) {
            throw new WebApplicationException(404);
        }
        return bahnhofMap;
    }

}
