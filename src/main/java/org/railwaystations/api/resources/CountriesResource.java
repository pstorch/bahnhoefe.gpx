package org.railwaystations.api.resources;

import org.railwaystations.api.BahnhoefeRepository;
import org.railwaystations.api.model.Country;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.util.Set;

@Path("/")
public class CountriesResource {

    private final BahnhoefeRepository repository;

    public CountriesResource(final BahnhoefeRepository repository) {
        this.repository = repository;
    }

    @GET
    @Path("countries")
    @Produces({MediaType.APPLICATION_JSON})
    public Set<Country> list() {
        return repository.getCountries();
    }

}
