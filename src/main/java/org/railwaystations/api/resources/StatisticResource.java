package org.railwaystations.api.resources;

import org.railwaystations.api.BahnhoefeRepository;
import org.railwaystations.api.model.Statistic;
import org.railwaystations.api.writer.StatisticTxtWriter;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.io.IOException;

@Path("/")
public class StatisticResource {

    private static final String COUNTRY = "country";

    private final BahnhoefeRepository repository;

    public StatisticResource(final BahnhoefeRepository repository) {
        this.repository = repository;
    }

    @GET
    @Path("stats")
    @Produces({MediaType.APPLICATION_JSON, StatisticTxtWriter.TEXT_PLAIN})
    public Statistic get(@QueryParam(StatisticResource.COUNTRY) final String country) throws IOException {
        return getWithCountry(country);
    }

    @GET
    @Path("{country}/stats")
    @Produces({MediaType.APPLICATION_JSON, StatisticTxtWriter.TEXT_PLAIN})
    public Statistic getWithCountry(@PathParam(StatisticResource.COUNTRY) final String country) throws IOException {
        return getStatisticMap(country);
    }

    private Statistic getStatisticMap(final String country) {
        return repository.getStatistic(country);
    }

}
