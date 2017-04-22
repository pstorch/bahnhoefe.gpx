package github.pstorch.bahnhoefe.service.resources;

import github.pstorch.bahnhoefe.service.BahnhoefeRepository;
import github.pstorch.bahnhoefe.service.model.Statistic;
import github.pstorch.bahnhoefe.service.loader.BahnhoefeLoaderDe;
import github.pstorch.bahnhoefe.service.writer.StatisticTxtWriter;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

@Path("/")
public class StatisticResource {

    private static final String DEFAULT_COUNTRY = BahnhoefeLoaderDe.COUNTRY_CODE;
    private static final String COUNTRY = "country";

    private final BahnhoefeRepository repository;

    public StatisticResource(final BahnhoefeRepository repository) {
        this.repository = repository;
    }

    @GET
    @Path("stats")
    @Produces({MediaType.APPLICATION_JSON, StatisticTxtWriter.TEXT_PLAIN})
    public Statistic get() throws IOException {
        return get(StatisticResource.DEFAULT_COUNTRY);
    }

    @GET
    @Path("{country}/stats")
    @Produces({MediaType.APPLICATION_JSON, StatisticTxtWriter.TEXT_PLAIN})
    public Statistic get(@PathParam(StatisticResource.COUNTRY) final String country) throws IOException {
        return getStatisticMap(country);
    }

    private Statistic getStatisticMap(@PathParam(StatisticResource.COUNTRY) final String country) {
        final AtomicInteger total = new AtomicInteger();
        final AtomicInteger withPhoto = new AtomicInteger();
        final AtomicInteger withoutPhoto = new AtomicInteger();
        final Set<String> photographers = new HashSet<>();
        repository.get(country).values()
            .forEach(b -> {
                total.incrementAndGet();
                if (b.hasPhoto()) {
                    withPhoto.incrementAndGet();
                    photographers.add(b.getPhotographer());
                } else {
                    withoutPhoto.incrementAndGet();
                }
            });

        return new Statistic(total.intValue(), withPhoto.intValue(), withoutPhoto.intValue(), photographers.size());
    }

}
