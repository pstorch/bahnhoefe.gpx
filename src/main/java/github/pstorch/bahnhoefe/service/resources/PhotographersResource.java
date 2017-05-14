package github.pstorch.bahnhoefe.service.resources;

import github.pstorch.bahnhoefe.service.BahnhoefeRepository;
import github.pstorch.bahnhoefe.service.model.Bahnhof;
import github.pstorch.bahnhoefe.service.writer.PhotographersTxtWriter;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.io.IOException;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Path("/")
public class PhotographersResource {

    private static final String COUNTRY = "country";

    private final BahnhoefeRepository repository;

    public PhotographersResource(final BahnhoefeRepository repository) {
        this.repository = repository;
    }

    @GET
    @Path("photographers")
    @Produces({MediaType.APPLICATION_JSON + ";charset=UTF-8", PhotographersTxtWriter.TEXT_PLAIN + ";charset=UTF-8"})
    public Map<String, Long> get(@QueryParam(PhotographersResource.COUNTRY) final String country) throws IOException {
        return getWithCountry(country);
    }

    @GET
    @Path("{country}/photographers")
    @Produces({MediaType.APPLICATION_JSON + ";charset=UTF-8", PhotographersTxtWriter.TEXT_PLAIN + ";charset=UTF-8"})
    public Map<String, Long> getWithCountry(@PathParam(PhotographersResource.COUNTRY) final String country) throws IOException {
        return getPhotographerMap(country);
    }

    private Map<String, Long> getPhotographerMap(final String country) {
        final Map<String, Long> photographerMap = repository.get(country).values().stream()
                .filter(Bahnhof::hasPhoto)
                .map(Bahnhof::getPhotographer)
                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));
        if (photographerMap.isEmpty()) {
            throw new WebApplicationException(404);
        }
        return photographerMap.entrySet()
                        .stream()
                        .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
                        .collect(Collectors.toMap(
                                Map.Entry::getKey,
                                Map.Entry::getValue,
                                (v1, v2) -> { throw new IllegalStateException(); },
                                LinkedHashMap::new
                        ));
    }

}
