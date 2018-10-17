package org.railwaystations.api;

import io.dropwizard.Application;
import io.dropwizard.configuration.EnvironmentVariableSubstitutor;
import io.dropwizard.configuration.SubstitutingSourceProvider;
import io.dropwizard.db.DataSourceFactory;
import io.dropwizard.jdbi3.JdbiFactory;
import io.dropwizard.migrations.MigrationsBundle;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.core.h2.H2DatabasePlugin;
import org.railwaystations.api.db.CountryDao;
import org.railwaystations.api.db.PhotoDao;
import org.railwaystations.api.db.StationDao;
import org.railwaystations.api.db.UserDao;
import org.railwaystations.api.resources.*;
import org.railwaystations.api.writer.PhotographersTxtWriter;
import org.railwaystations.api.writer.StationsGpxWriter;
import org.railwaystations.api.writer.StationsTxtWriter;
import org.railwaystations.api.writer.StatisticTxtWriter;

/**
 * RailwayStations API Dropwizard App
 */
public class RsApiApp extends Application<RsApiConfiguration> {

    public static void main(final String... args) throws Exception {
        new RsApiApp().run(args);
    }

    @Override
    public String getName() {
        return "RailwayStations API App";
    }

    @Override
    public void initialize(final Bootstrap<RsApiConfiguration> bootstrap) {
        bootstrap.setConfigurationSourceProvider(new SubstitutingSourceProvider(
                bootstrap.getConfigurationSourceProvider(), new EnvironmentVariableSubstitutor(false)));

        bootstrap.addBundle(new RsApiConfigurationMigrationsBundle());
    }

    @Override
    public void run(final RsApiConfiguration config, final Environment environment) {
        config.getMonitor().sendMessage("RSAPI starting up");

        final JdbiFactory factory = new JdbiFactory();
        final Jdbi jdbi = factory.build(environment, config.getDataSourceFactory(), "mariadb");
        jdbi.installPlugin(new H2DatabasePlugin());

        final CountryDao countryDao = jdbi.onDemand(CountryDao.class);
        final UserDao userDao = jdbi.onDemand(UserDao.class);
        final PhotoDao photoDao = jdbi.onDemand(PhotoDao.class);
        final StationDao stationDao = jdbi.onDemand(StationDao.class);
        StationDao.StationMapper.setPhotoBaseUrl(config.getPhotoBaseUrl());

        final StationsRepository repository = new StationsRepository(countryDao,
                stationDao);

        environment.jersey().register(new StationsResource(repository));
        environment.jersey().register(new PhotographersResource(repository));
        environment.jersey().register(new CountriesResource(countryDao));
        environment.jersey().register(new StatisticResource(repository));
        environment.jersey().register(new PhotoUploadResource(repository, config.getApiKey(),
                config.getTokenGenerator(), config.getWorkDir(), config.getMonitor(), userDao));
        environment.jersey().register(new RegistrationResource(
                config.getApiKey(), config.getTokenGenerator(), config.getMonitor(), config.getMailer(), userDao));
        environment.jersey().register(new SlackCommandResource(repository, config.getSlackVerificationToken(),
                new PhotoImporter(repository, userDao, photoDao, countryDao, config.getMonitor(), config.getWorkDir(), config.getPhotoDir())));
        environment.jersey().register(new StationsGpxWriter());
        environment.jersey().register(new StationsTxtWriter());
        environment.jersey().register(new StatisticTxtWriter());
        environment.jersey().register(new PhotographersTxtWriter());
        environment.jersey().property("jersey.config.server.mediaTypeMappings",
                "gpx : application/gpx+xml, json : application/json, txt : text/plain");
        config.getMonitor().sendMessage(repository.getCountryStatisticMessage());
    }

    private static class RsApiConfigurationMigrationsBundle extends MigrationsBundle<RsApiConfiguration> {
        @Override
        public DataSourceFactory getDataSourceFactory(final RsApiConfiguration configuration) {
            return configuration.getDataSourceFactory();
        }
    }

}
