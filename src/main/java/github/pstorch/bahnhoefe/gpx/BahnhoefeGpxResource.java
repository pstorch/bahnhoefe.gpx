package github.pstorch.bahnhoefe.gpx;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.Iterator;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

import com.fasterxml.jackson.core.JsonProcessingException;

@Path("/")
public class BahnhoefeGpxResource {

    private static final String APPLICATION_JSON = "application/json";
    private final BahnhoefeLoader loader;

    public BahnhoefeGpxResource(final BahnhoefeLoader loader) {
	this.loader = loader;
    }

    @GET
    @Path("bahnhoefe")
    @Produces({ BahnhoefeGpxResource.APPLICATION_JSON, BahnhoefeGpxWriter.APPLICATION_GPX_XML,
	    BahnhoefeTxtWriter.TEXT_PLAIN })
    public Iterator<Bahnhof> getAll() throws MalformedURLException, JsonProcessingException, IOException {
	return loader.loadBahnhoefe().values().iterator();
    }

    @GET
    @Path("bahnhoefe-withPhoto")
    @Produces({ BahnhoefeGpxResource.APPLICATION_JSON, BahnhoefeGpxWriter.APPLICATION_GPX_XML,
	    BahnhoefeTxtWriter.TEXT_PLAIN })
    public Iterator<Bahnhof> getWithPhoto() throws MalformedURLException, JsonProcessingException, IOException {
	return loader.loadBahnhoefe().values().stream().filter(bahnhof -> bahnhof.hasPhoto()).iterator();
    }

    @GET
    @Path("bahnhoefe-withoutPhoto")
    @Produces({ BahnhoefeGpxResource.APPLICATION_JSON, BahnhoefeGpxWriter.APPLICATION_GPX_XML,
	    BahnhoefeTxtWriter.TEXT_PLAIN })
    public Iterator<Bahnhof> getWithoutPhoto() throws MalformedURLException, JsonProcessingException, IOException {
	return loader.loadBahnhoefe().values().stream().filter(bahnhof -> !bahnhof.hasPhoto()).iterator();
    }

}
