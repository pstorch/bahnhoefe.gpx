package org.railwaystations.api;

import io.dropwizard.Application;
import io.dropwizard.configuration.EnvironmentVariableSubstitutor;
import io.dropwizard.configuration.SubstitutingSourceProvider;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import org.railwaystations.api.resources.*;
import org.railwaystations.api.writer.StationsGpxWriter;
import org.railwaystations.api.writer.StationsTxtWriter;
import org.railwaystations.api.writer.PhotographersTxtWriter;
import org.railwaystations.api.writer.StatisticTxtWriter;

/**
 * RailwayStations API Dropwizard App
 */
public class RsApiApp extends Application<RsApiConfiguration> {

    public static void main(final String... args) throws Exception {
        new RsApiApp().run(args);
    }

    @Override
    protected void bootstrapLogging() {
    }

    @Override
    public String getName() {
        return "RailwayStations API App";
    }

    @Override
    public void initialize(final Bootstrap<RsApiConfiguration> bootstrap) {
        bootstrap.setConfigurationSourceProvider(new SubstitutingSourceProvider(
                bootstrap.getConfigurationSourceProvider(), new EnvironmentVariableSubstitutor(false)));
    }

    @Override
    public void run(final RsApiConfiguration config, final Environment environment) {
        config.getMonitor().sendMessage("RSAPI starting up");
        final StationsRepository repository = config.getRepository();
        environment.jersey().register(new StationsResource(repository));
        environment.jersey().register(new PhotographersResource(repository));
        environment.jersey().register(new CountriesResource(repository));
        environment.jersey().register(new StatisticResource(repository));
        environment.jersey().register(new PhotoUploadResource(repository, config.getApiKey(),
                config.getTokenGenerator(), config.getWorkDir(), config.getMonitor()));
        environment.jersey().register(new RegistrationResource(
                config.getApiKey(), config.getTokenGenerator(), config.getMonitor(), config.getMailer(), config.getWorkDir()));
        environment.jersey().register(new SlackCommandResource(repository, config.getSlackVerificationToken(), new PhotoImporter(repository, config.getWorkDir(), config.getPhotoDir())));
        environment.jersey().register(new StationsGpxWriter());
        environment.jersey().register(new StationsTxtWriter());
        environment.jersey().register(new StatisticTxtWriter());
        environment.jersey().register(new PhotographersTxtWriter());
        environment.jersey().property("jersey.config.server.mediaTypeMappings",
                "gpx : application/service+xml, json : application/json, txt : text/plain");
        repository.refresh(null);
    }

}
