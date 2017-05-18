package github.pstorch.bahnhoefe.service;

import github.pstorch.bahnhoefe.service.resources.BahnhoefeResource;
import github.pstorch.bahnhoefe.service.resources.PhotoUploadResource;
import github.pstorch.bahnhoefe.service.resources.PhotographersResource;
import github.pstorch.bahnhoefe.service.resources.StatisticResource;
import github.pstorch.bahnhoefe.service.writer.BahnhoefeGpxWriter;
import github.pstorch.bahnhoefe.service.writer.BahnhoefeTxtWriter;
import github.pstorch.bahnhoefe.service.writer.PhotographersTxtWriter;
import github.pstorch.bahnhoefe.service.writer.StatisticTxtWriter;
import io.dropwizard.Application;
import io.dropwizard.configuration.EnvironmentVariableSubstitutor;
import io.dropwizard.configuration.SubstitutingSourceProvider;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;

import java.net.MalformedURLException;

/**
 * RailwayStations API Dropwizard App
 */
public class BahnhoefeServiceApp extends Application<BahnhoefeServiceConfiguration> {

    public static void main(final String... args) throws Exception {
        new BahnhoefeServiceApp().run(args);
    }

    @Override
    public String getName() {
        return "BahnhoefeRepository Service App";
    }

    @Override
    public void initialize(final Bootstrap<BahnhoefeServiceConfiguration> bootstrap) {
        bootstrap.setConfigurationSourceProvider(new SubstitutingSourceProvider(
                bootstrap.getConfigurationSourceProvider(), new EnvironmentVariableSubstitutor(false)));
    }

    @Override
    public void run(final BahnhoefeServiceConfiguration config, final Environment environment)
            throws MalformedURLException {
        config.getMonitor().sendMessage("RSAPI starting up");
        environment.jersey().register(new BahnhoefeResource(config.getRepository()));
        environment.jersey().register(new PhotographersResource(config.getRepository()));
        environment.jersey().register(new StatisticResource(config.getRepository()));
        environment.jersey().register(new PhotoUploadResource(
                config.getApiKey(), config.getUploadDir(),
                config.getRepository().getCountries(), config.getMonitor()));
        environment.jersey().register(new BahnhoefeGpxWriter());
        environment.jersey().register(new BahnhoefeTxtWriter());
        environment.jersey().register(new StatisticTxtWriter());
        environment.jersey().register(new PhotographersTxtWriter());
        environment.jersey().property("jersey.config.server.mediaTypeMappings",
                "gpx : application/service+xml, json : application/json, txt : text/plain");
    }

}
