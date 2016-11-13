package github.pstorch.bahnhoefe.service;

import github.pstorch.bahnhoefe.service.loader.BahnhoefeLoader;
import github.pstorch.bahnhoefe.service.loader.BahnhoefeLoaderDe;

import javax.ws.rs.*;
import java.io.IOException;
import java.util.Iterator;
import java.util.Map;

@Path("/")
public class BahnhoefeResource {

    private static final String APPLICATION_JSON = "application/json";
    private static final String DEFAULT_COUNTRY = BahnhoefeLoaderDe.COUNTRY_CODE;
    private static final String COUNTRY = "country";
    private final Map<String, BahnhoefeLoader> loaderMap;

    public BahnhoefeResource(final Map<String, BahnhoefeLoader> loaders) {
        this.loaderMap = loaders;
    }

    @GET
    @Path("bahnhoefe")
    @Produces({BahnhoefeResource.APPLICATION_JSON, BahnhoefeGpxWriter.APPLICATION_GPX_XML,
            BahnhoefeTxtWriter.TEXT_PLAIN})
    public Iterator<Bahnhof> getAll() throws IOException {
        return getAll(BahnhoefeResource.DEFAULT_COUNTRY);
    }

    @GET
    @Path("{country}/bahnhoefe")
    @Produces({BahnhoefeResource.APPLICATION_JSON, BahnhoefeGpxWriter.APPLICATION_GPX_XML,
            BahnhoefeTxtWriter.TEXT_PLAIN})
    public Iterator<Bahnhof> getAll(@PathParam(BahnhoefeResource.COUNTRY) final String country) throws IOException {
        return getBahnhoefeLoader(country).loadBahnhoefe().values().iterator();
    }

    @GET
    @Path("bahnhoefe-withPhoto")
    @Produces({BahnhoefeResource.APPLICATION_JSON, BahnhoefeGpxWriter.APPLICATION_GPX_XML,
            BahnhoefeTxtWriter.TEXT_PLAIN})
    public Iterator<Bahnhof> getWithPhoto() throws IOException {
        return getWithPhoto(BahnhoefeResource.DEFAULT_COUNTRY);
    }

    @GET
    @Path("{country}/bahnhoefe-withPhoto")
    @Produces({BahnhoefeResource.APPLICATION_JSON, BahnhoefeGpxWriter.APPLICATION_GPX_XML,
            BahnhoefeTxtWriter.TEXT_PLAIN})
    public Iterator<Bahnhof> getWithPhoto(@PathParam(BahnhoefeResource.COUNTRY) final String country) throws IOException {
        return loaderMap.get(country).loadBahnhoefe().values().stream().filter(Bahnhof::hasPhoto).iterator();
    }

    @GET
    @Path("bahnhoefe-withoutPhoto")
    @Produces({BahnhoefeResource.APPLICATION_JSON, BahnhoefeGpxWriter.APPLICATION_GPX_XML,
            BahnhoefeTxtWriter.TEXT_PLAIN})
    public Iterator<Bahnhof> getWithoutPhoto() throws IOException {
        return getWithoutPhoto(BahnhoefeResource.DEFAULT_COUNTRY);
    }

    @GET
    @Path("{country}/bahnhoefe-withoutPhoto")
    @Produces({BahnhoefeResource.APPLICATION_JSON, BahnhoefeGpxWriter.APPLICATION_GPX_XML,
            BahnhoefeTxtWriter.TEXT_PLAIN})
    public Iterator<Bahnhof> getWithoutPhoto(@PathParam(BahnhoefeResource.COUNTRY) final String country) throws IOException {
        return getBahnhoefeLoader(country).loadBahnhoefe().values().stream().filter(bahnhof -> !bahnhof.hasPhoto()).iterator();
    }

    private BahnhoefeLoader getBahnhoefeLoader(@PathParam(BahnhoefeResource.COUNTRY) final String country) {
        if (!loaderMap.containsKey(country)) {
            throw new WebApplicationException(404);
        }
        return loaderMap.get(country);
    }

}
