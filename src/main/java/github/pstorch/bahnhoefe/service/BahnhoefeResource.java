package github.pstorch.bahnhoefe.service;

import github.pstorch.bahnhoefe.service.loader.BahnhoefeLoader;
import github.pstorch.bahnhoefe.service.loader.BahnhoefeLoaderDe;
import org.apache.commons.lang3.StringUtils;

import javax.ws.rs.*;
import java.io.IOException;
import java.util.Iterator;
import java.util.Map;

@Path("/")
public class BahnhoefeResource {

    private static final String APPLICATION_JSON = "application/json";
    private static final String DEFAULT_COUNTRY = BahnhoefeLoaderDe.COUNTRY_CODE;
    private static final String COUNTRY = "country";
    private static final String PHOTOGRAPHER = "photographer";
    private static final String HAS_PHOTO = "hasPhoto";
    private static final String MAX_DISTANCE = "maxDistance";
    private static final String LAT = "lat";
    private static final String LON = "lon";
    private static final int EARTH_RADIUS = 6371;

    private final Map<String, BahnhoefeLoader> loaderMap;

    public BahnhoefeResource(final Map<String, BahnhoefeLoader> loaders) {
        this.loaderMap = loaders;
    }

    @GET
    @Path("bahnhoefe")
    @Produces({BahnhoefeResource.APPLICATION_JSON, BahnhoefeGpxWriter.APPLICATION_GPX_XML,
            BahnhoefeTxtWriter.TEXT_PLAIN})
    public Iterator<Bahnhof> get(@QueryParam(BahnhoefeResource.HAS_PHOTO) final Boolean hasPhoto,
                                 @QueryParam(BahnhoefeResource.PHOTOGRAPHER) final String photographer,
                                 @QueryParam(BahnhoefeResource.MAX_DISTANCE) final Integer maxDistance,
                                 @QueryParam(BahnhoefeResource.LAT) final Double lat,
                                 @QueryParam(BahnhoefeResource.LON) final Double lon) throws IOException {
        return get(BahnhoefeResource.DEFAULT_COUNTRY, hasPhoto, photographer, maxDistance, lat, lon);
    }

    @GET
    @Path("{country}/bahnhoefe")
    @Produces({BahnhoefeResource.APPLICATION_JSON, BahnhoefeGpxWriter.APPLICATION_GPX_XML,
            BahnhoefeTxtWriter.TEXT_PLAIN})
    public Iterator<Bahnhof> get(@PathParam(BahnhoefeResource.COUNTRY) final String country,
                                 @QueryParam(BahnhoefeResource.HAS_PHOTO) final Boolean hasPhoto,
                                 @QueryParam(BahnhoefeResource.PHOTOGRAPHER) final String photographer,
                                 @QueryParam(BahnhoefeResource.MAX_DISTANCE) final Integer maxDistance,
                                 @QueryParam(BahnhoefeResource.LAT) final Double lat,
                                 @QueryParam(BahnhoefeResource.LON) final Double lon) throws IOException {
        return getBahnhoefeLoader(country).loadBahnhoefe().values().stream().filter(bahnhof -> {
            boolean result = true;
            if (hasPhoto != null) {
                result = bahnhof.hasPhoto() == hasPhoto;
            }
            if (photographer != null) {
                result &= StringUtils.equals(photographer, bahnhof.getPhotographer());
            }
            if (maxDistance != null && lat != null && lon != null) {
                result &= distance(lat, lon, bahnhof.getLat(), bahnhof.getLon()) < maxDistance;
            }
            return result;
        }).iterator();

    }

    @GET
    @Path("bahnhoefe-withPhoto")
    @Produces({BahnhoefeResource.APPLICATION_JSON, BahnhoefeGpxWriter.APPLICATION_GPX_XML,
            BahnhoefeTxtWriter.TEXT_PLAIN})
    public Iterator<Bahnhof> getWithPhoto(@QueryParam(BahnhoefeResource.PHOTOGRAPHER) final String photographer,
                                          @QueryParam(BahnhoefeResource.MAX_DISTANCE) final Integer maxDistance,
                                          @QueryParam(BahnhoefeResource.LAT) final Double lat,
                                          @QueryParam(BahnhoefeResource.LON) final Double lon) throws IOException {
        return get(BahnhoefeResource.DEFAULT_COUNTRY, true, photographer, maxDistance, lat, lon);
    }

    @GET
    @Path("{country}/bahnhoefe-withPhoto")
    @Produces({BahnhoefeResource.APPLICATION_JSON, BahnhoefeGpxWriter.APPLICATION_GPX_XML,
            BahnhoefeTxtWriter.TEXT_PLAIN})
    public Iterator<Bahnhof> getWithPhoto(@PathParam(BahnhoefeResource.COUNTRY) final String country,
                                          @QueryParam(BahnhoefeResource.PHOTOGRAPHER) final String photographer,
                                          @QueryParam(BahnhoefeResource.MAX_DISTANCE) final Integer maxDistance,
                                          @QueryParam(BahnhoefeResource.LAT) final Double lat,
                                          @QueryParam(BahnhoefeResource.LON) final Double lon) throws IOException {
        return get(country, true, photographer, maxDistance, lat, lon);
    }

    @GET
    @Path("bahnhoefe-withoutPhoto")
    @Produces({BahnhoefeResource.APPLICATION_JSON, BahnhoefeGpxWriter.APPLICATION_GPX_XML,
            BahnhoefeTxtWriter.TEXT_PLAIN})
    public Iterator<Bahnhof> getWithoutPhoto(@QueryParam(BahnhoefeResource.PHOTOGRAPHER) final String photographer,
                                             @QueryParam(BahnhoefeResource.MAX_DISTANCE) final Integer maxDistance,
                                             @QueryParam(BahnhoefeResource.LAT) final Double lat,
                                             @QueryParam(BahnhoefeResource.LON) final Double lon) throws IOException {
        return get(BahnhoefeResource.DEFAULT_COUNTRY, false, photographer, maxDistance, lat, lon);
    }

    @GET
    @Path("{country}/bahnhoefe-withoutPhoto")
    @Produces({BahnhoefeResource.APPLICATION_JSON, BahnhoefeGpxWriter.APPLICATION_GPX_XML,
            BahnhoefeTxtWriter.TEXT_PLAIN})
    public Iterator<Bahnhof> getWithoutPhoto(@PathParam(BahnhoefeResource.COUNTRY) final String country,
                                             @QueryParam(BahnhoefeResource.PHOTOGRAPHER) final String photographer,
                                             @QueryParam(BahnhoefeResource.MAX_DISTANCE) final Integer maxDistance,
                                             @QueryParam(BahnhoefeResource.LAT) final Double lat,
                                             @QueryParam(BahnhoefeResource.LON) final Double lon) throws IOException {
        return get(BahnhoefeResource.DEFAULT_COUNTRY, false, photographer, maxDistance, lat, lon);
    }

    private BahnhoefeLoader getBahnhoefeLoader(@PathParam(BahnhoefeResource.COUNTRY) final String country) {
        if (!loaderMap.containsKey(country)) {
            throw new WebApplicationException(404);
        }
        return loaderMap.get(country);
    }

    /*
     * Calculate distance in km between two points in latitude and longitude. Uses Haversine method as its base.
     * lat1, lon1 Start point lat2, lon2 End point.
     *
     * @returns Distance in Meters
     */
    private static double distance(final double lat1, final double lon1,
                                   final double lat2, final double lon2) {
        final Double latDistance = Math.toRadians(lat2 - lat1);
        final Double lonDistance = Math.toRadians(lon2 - lon1);
        final Double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        final Double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return BahnhoefeResource.EARTH_RADIUS * c;
    }

}
