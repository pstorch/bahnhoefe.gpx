package org.railwaystations.api.resources;

import org.railwaystations.api.StationsRepository;
import org.railwaystations.api.writer.PhotographersTxtWriter;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.util.Map;

@Path("/")
public class PhotographersResource {

    private static final String COUNTRY = "country";

    private final StationsRepository repository;

    public PhotographersResource(final StationsRepository repository) {
        this.repository = repository;
    }

    @GET
    @Path("photographers")
    @Produces({MediaType.APPLICATION_JSON + ";charset=UTF-8", PhotographersTxtWriter.TEXT_PLAIN + ";charset=UTF-8"})
    public Map<String, Long> get(@QueryParam(PhotographersResource.COUNTRY) final String country) {
        return getWithCountry(country);
    }

    @GET
    @Path("{country}/photographers")
    @Produces({MediaType.APPLICATION_JSON + ";charset=UTF-8", PhotographersTxtWriter.TEXT_PLAIN + ";charset=UTF-8"})
    public Map<String, Long> getWithCountry(@PathParam(PhotographersResource.COUNTRY) final String country) {
        return repository.getPhotographerMap(country);
    }

}
