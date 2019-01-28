package org.railwaystations.api;

import io.dropwizard.Application;
import io.dropwizard.auth.AuthDynamicFeature;
import io.dropwizard.auth.AuthValueFactoryProvider;
import io.dropwizard.configuration.EnvironmentVariableSubstitutor;
import io.dropwizard.configuration.SubstitutingSourceProvider;
import io.dropwizard.db.DataSourceFactory;
import io.dropwizard.forms.MultiPartBundle;
import io.dropwizard.jdbi3.JdbiFactory;
import io.dropwizard.migrations.MigrationsBundle;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import org.eclipse.jetty.servlets.CrossOriginFilter;
import org.glassfish.jersey.server.filter.RolesAllowedDynamicFeature;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.core.h2.H2DatabasePlugin;
import org.railwaystations.api.auth.AuthUser;
import org.railwaystations.api.auth.UploadTokenAuthFilter;
import org.railwaystations.api.auth.UploadTokenAuthenticator;
import org.railwaystations.api.db.CountryDao;
import org.railwaystations.api.db.PhotoDao;
import org.railwaystations.api.db.StationDao;
import org.railwaystations.api.db.UserDao;
import org.railwaystations.api.resources.*;
import org.railwaystations.api.writer.PhotographersTxtWriter;
import org.railwaystations.api.writer.StationsGpxWriter;
import org.railwaystations.api.writer.StationsTxtWriter;
import org.railwaystations.api.writer.StatisticTxtWriter;

import javax.servlet.DispatcherType;
import javax.servlet.FilterRegistration;
import java.util.EnumSet;

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

        bootstrap.addBundle(new MultiPartBundle());
        bootstrap.addBundle(new RsApiConfigurationMigrationsBundle());
    }

    @Override
    public void run(final RsApiConfiguration config, final Environment environment) {
        config.getMonitor().sendMessage("RSAPI starting up");

        // Enable CORS headers
        final FilterRegistration.Dynamic cors =
                environment.servlets().addFilter("CORS", CrossOriginFilter.class);

        // Configure CORS parameters
        cors.setInitParameter("allowedOrigins", "*");
        cors.setInitParameter("allowedHeaders", "X-Requested-With,Content-Type,Accept,Origin");
        cors.setInitParameter("allowedMethods", "OPTIONS,GET,PUT,POST,DELETE,HEAD");

        // Add URL mapping
        cors.addMappingForUrlPatterns(EnumSet.allOf(DispatcherType.class), true, "/*");

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

        final UploadTokenAuthenticator authenticator = new UploadTokenAuthenticator(userDao, config.getTokenGenerator());
        environment.jersey().register(new AuthDynamicFeature(
                                        new UploadTokenAuthFilter.Builder<AuthUser>()
                                                .setAuthenticator(authenticator).setRealm("RSAPI").buildAuthFilter()));
        environment.jersey().register(RolesAllowedDynamicFeature.class);
        environment.jersey().register(new AuthValueFactoryProvider.Binder<>(AuthUser.class));

        environment.jersey().register(new StationsResource(repository));
        environment.jersey().register(new PhotographersResource(repository));
        environment.jersey().register(new CountriesResource(countryDao));
        environment.jersey().register(new StatisticResource(repository));
        environment.jersey().register(new PhotoUploadResource(repository, config.getWorkDir(),
                                                            config.getMonitor(), authenticator));
        environment.jersey().register(new ProfileResource(
                config.getTokenGenerator(), config.getMonitor(), config.getMailer(), userDao, config.getGoogleClientId()));
        environment.jersey().register(new SlackCommandResource(repository, config.getSlackVerificationToken(),
                new PhotoImporter(repository, userDao, photoDao, countryDao, config.getMonitor(), config.getWorkDir(), config.getPhotoDir())));
        environment.jersey().register(new StationsGpxWriter());
        environment.jersey().register(new StationsTxtWriter());
        environment.jersey().register(new StatisticTxtWriter());
        environment.jersey().register(new PhotographersTxtWriter());
        environment.jersey().register(new RootResource());
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
