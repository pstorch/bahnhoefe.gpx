package github.pstorch.bahnhoefe.service;

import github.pstorch.bahnhoefe.service.writer.BahnhoefeGpxWriter;
import github.pstorch.bahnhoefe.service.writer.BahnhoefeTxtWriter;
import io.dropwizard.Application;
import io.dropwizard.configuration.EnvironmentVariableSubstitutor;
import io.dropwizard.configuration.SubstitutingSourceProvider;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;

import java.net.MalformedURLException;

/**
 * BahnhoefeRepository GPX Dropwizard App
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
    public void run(final BahnhoefeServiceConfiguration configuration, final Environment environment)
            throws MalformedURLException {
        configuration.getMonitor().sendMessage("RSAPI starting up");
        final BahnhoefeResource resource = new BahnhoefeResource(
                configuration.getRepository());
        environment.jersey().register(resource);
        environment.jersey().register(new BahnhoefeGpxWriter());
        environment.jersey().register(new BahnhoefeTxtWriter());
        environment.jersey().property("jersey.config.server.mediaTypeMappings",
                "gpx : application/service+xml, json : application/json, txt : text/plain");
    }

}
