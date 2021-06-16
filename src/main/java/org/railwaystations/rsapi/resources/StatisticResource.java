package org.railwaystations.rsapi.resources;

import org.railwaystations.rsapi.StationsRepository;
import org.railwaystations.rsapi.model.Statistic;
import org.railwaystations.rsapi.writer.StatisticTxtWriter;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;

@Path("/")
public class StatisticResource {

    private static final String COUNTRY = "country";

    private final StationsRepository repository;

    public StatisticResource(final StationsRepository repository) {
        this.repository = repository;
    }

    @GET
    @Path("stats")
    @Produces({MediaType.APPLICATION_JSON, StatisticTxtWriter.TEXT_PLAIN})
    public Statistic get(@QueryParam(StatisticResource.COUNTRY) final String country) {
        return getWithCountry(country);
    }

    @GET
    @Path("{country}/stats")
    @Produces({MediaType.APPLICATION_JSON, StatisticTxtWriter.TEXT_PLAIN})
    public Statistic getWithCountry(@PathParam(StatisticResource.COUNTRY) final String country) {
        return getStatisticMap(country);
    }

    private Statistic getStatisticMap(final String country) {
        return repository.getStatistic(country);
    }

}
