package github.pstorch.bahnhoefe.service;

import io.dropwizard.Application;
import io.dropwizard.configuration.EnvironmentVariableSubstitutor;
import io.dropwizard.configuration.SubstitutingSourceProvider;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;

import java.net.MalformedURLException;

/**
 * Bahnhoefe GPX Dropwizard App
 */
public class BahnhoefeServiceApp extends Application<BahnhoefeServiceConfiguration> {

    public static void main(final String... args) throws Exception {
        new BahnhoefeServiceApp().run(args);
    }

    @Override
    public String getName() {
        return "Bahnhoefe GPX";
    }

    @Override
    public void initialize(final Bootstrap<BahnhoefeServiceConfiguration> bootstrap) {
        bootstrap.setConfigurationSourceProvider(new SubstitutingSourceProvider(
                bootstrap.getConfigurationSourceProvider(), new EnvironmentVariableSubstitutor(false)));

    }

    @Override
    public void run(final BahnhoefeServiceConfiguration configuration, final Environment environment)
            throws MalformedURLException {
        final BahnhoefeResource resource = new BahnhoefeResource(
                configuration.getLoaderMap());
        environment.jersey().register(resource);
        environment.jersey().register(new BahnhoefeGpxWriter());
        environment.jersey().register(new BahnhoefeTxtWriter());
        environment.jersey().property("jersey.config.server.mediaTypeMappings",
                "service : application/service+xml, json : application/json, txt : text/plain");
    }

}
