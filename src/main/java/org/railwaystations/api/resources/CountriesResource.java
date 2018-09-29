package org.railwaystations.api.resources;

import org.railwaystations.api.db.CountryDao;
import org.railwaystations.api.model.Country;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.util.Collection;

@Path("/")
public class CountriesResource {

    private final CountryDao countryDao;

    public CountriesResource(final CountryDao countryDao) {
        this.countryDao = countryDao;
    }

    @GET
    @Path("countries")
    @Produces({MediaType.APPLICATION_JSON})
    public Collection<Country> list() {
        return countryDao.list();
    }

}
