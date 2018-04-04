package org.railwaystations.api;

import io.dropwizard.Application;
import io.dropwizard.configuration.EnvironmentVariableSubstitutor;
import io.dropwizard.configuration.SubstitutingSourceProvider;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import org.railwaystations.api.resources.*;
import org.railwaystations.api.writer.BahnhoefeGpxWriter;
import org.railwaystations.api.writer.BahnhoefeTxtWriter;
import org.railwaystations.api.writer.PhotographersTxtWriter;
import org.railwaystations.api.writer.StatisticTxtWriter;

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
    public void run(final BahnhoefeServiceConfiguration config, final Environment environment) {
        config.getMonitor().sendMessage("RSAPI starting up");
        final BahnhoefeRepository repository = config.getRepository();
        environment.jersey().register(new BahnhoefeResource(repository));
        environment.jersey().register(new PhotographersResource(repository));
        environment.jersey().register(new CountriesResource(repository));
        environment.jersey().register(new StatisticResource(repository));
        environment.jersey().register(new PhotoUploadResource(repository, config.getApiKey(),
                config.getTokenGenerator(), config.getWorkDir(), config.getMonitor()));
        environment.jersey().register(new RegistrationResource(
                config.getApiKey(), config.getTokenGenerator(), config.getMonitor(), config.getMailer(), config.getWorkDir()));
        environment.jersey().register(new SlackCommandResource(repository, config.getSlackVerificationToken(), new PhotoImporter(repository, config.getWorkDir(), config.getPhotoDir())));
        environment.jersey().register(new BahnhoefeGpxWriter());
        environment.jersey().register(new BahnhoefeTxtWriter());
        environment.jersey().register(new StatisticTxtWriter());
        environment.jersey().register(new PhotographersTxtWriter());
        environment.jersey().property("jersey.config.server.mediaTypeMappings",
                "gpx : application/service+xml, json : application/json, txt : text/plain");
        repository.refresh(null);
    }

}
