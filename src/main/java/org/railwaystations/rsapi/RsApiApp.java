package org.railwaystations.rsapi;

import io.dropwizard.Application;
import io.dropwizard.auth.AuthDynamicFeature;
import io.dropwizard.auth.AuthFilter;
import io.dropwizard.auth.AuthValueFactoryProvider;
import io.dropwizard.auth.basic.BasicCredentialAuthFilter;
import io.dropwizard.auth.basic.BasicCredentials;
import io.dropwizard.auth.chained.ChainedAuthFilter;
import io.dropwizard.configuration.EnvironmentVariableSubstitutor;
import io.dropwizard.configuration.SubstitutingSourceProvider;
import io.dropwizard.db.DataSourceFactory;
import io.dropwizard.forms.MultiPartBundle;
import io.dropwizard.jdbi3.JdbiFactory;
import io.dropwizard.migrations.MigrationsBundle;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import jersey.repackaged.com.google.common.collect.Lists;
import org.eclipse.jetty.servlets.CrossOriginFilter;
import org.glassfish.jersey.server.filter.RolesAllowedDynamicFeature;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.core.h2.H2DatabasePlugin;
import org.railwaystations.rsapi.auth.*;
import org.railwaystations.rsapi.db.*;
import org.railwaystations.rsapi.resources.*;
import org.railwaystations.rsapi.writer.PhotographersTxtWriter;
import org.railwaystations.rsapi.writer.StationsGpxWriter;
import org.railwaystations.rsapi.writer.StationsTxtWriter;
import org.railwaystations.rsapi.writer.StatisticTxtWriter;

import javax.servlet.DispatcherType;
import javax.servlet.FilterRegistration;
import java.util.EnumSet;
import java.util.List;

import static org.eclipse.jetty.servlets.CrossOriginFilter.*;

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
        cors.setInitParameter(ALLOWED_ORIGINS_PARAM, "*");
        cors.setInitParameter(ALLOWED_HEADERS_PARAM, "X-Requested-With,Content-Type,Accept,Origin,Authorization,Comment,Country,Station-Id,NameOrEmail,New-Password");
        cors.setInitParameter(ALLOWED_METHODS_PARAM, "OPTIONS,GET,PUT,POST,DELETE,HEAD");

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
        final InboxDao inboxDao = jdbi.onDemand(InboxDao.class);

        final StationsRepository repository = new StationsRepository(countryDao,
                stationDao);

        final UploadTokenAuthenticator authenticator = registerAuthFilter(config, environment, userDao);

        environment.admin().addTask(new NotifyUsersTask(userDao, inboxDao, config.getMailer()));
        environment.jersey().register(new StationsResource(repository));
        environment.jersey().register(new PhotographersResource(repository));
        environment.jersey().register(new CountriesResource(countryDao));
        environment.jersey().register(new StatisticResource(repository));
        environment.jersey().register(new PhotoDownloadResource(config.getPhotosDir(), config.getInboxDir(), config.getInboxProcessedDir()));
        environment.jersey().register(new InboxResource(repository, config.getInboxDir(), config.getInboxToProcessDir(),
                config.getInboxProcessedDir(), config.getPhotosDir(), config.getMonitor(), authenticator,
                inboxDao, userDao, countryDao, photoDao, config.getInboxBaseUrl(), config.getMastodonBot()));
        environment.jersey().register(new ProfileResource(config.getMonitor(), config.getMailer(), userDao, config.getMailVerificationUrl()));
        environment.jersey().register(new StationsGpxWriter());
        environment.jersey().register(new StationsTxtWriter());
        environment.jersey().register(new StatisticTxtWriter());
        environment.jersey().register(new PhotographersTxtWriter());
        environment.jersey().register(new RootResource());
        environment.jersey().property("jersey.config.server.mediaTypeMappings",
                "gpx : application/gpx+xml, json : application/json, txt : text/plain");
        config.getMonitor().sendMessage(repository.getCountryStatisticMessage());
    }

    @SuppressWarnings({"unchecked","rawtypes"})
    private UploadTokenAuthenticator registerAuthFilter(final RsApiConfiguration config, final Environment environment, final UserDao userDao) {
        final AuthFilter<BasicCredentials, AuthUser> basicCredentialAuthFilter = new BasicCredentialAuthFilter.Builder<AuthUser>()
                .setAuthenticator(new BasicAuthenticator(userDao, config.getTokenGenerator()))
                .setAuthorizer(new UserAuthorizer())
                .setRealm("RSAPI").setPrefix("Basic").buildAuthFilter();

        final UploadTokenAuthenticator authenticator = new UploadTokenAuthenticator(userDao, config.getTokenGenerator());
        final UploadTokenAuthenticationFilter<AuthUser> uploadTokenAuthenticationFilter = new UploadTokenAuthenticationFilter.Builder<AuthUser>()
                .setAuthorizer(new UserAuthorizer())
                .setAuthenticator(authenticator).setRealm("RSAPI").buildAuthFilter();

        final List<AuthFilter<?, AuthUser>> filters = Lists.newArrayList(basicCredentialAuthFilter, uploadTokenAuthenticationFilter);
        environment.jersey().register(new AuthDynamicFeature(new ChainedAuthFilter(filters)));
        environment.jersey().register(RolesAllowedDynamicFeature.class);
        environment.jersey().register(new AuthValueFactoryProvider.Binder<>(AuthUser.class));
        return authenticator;
    }

    private static class RsApiConfigurationMigrationsBundle extends MigrationsBundle<RsApiConfiguration> {
        @Override
        public DataSourceFactory getDataSourceFactory(final RsApiConfiguration configuration) {
            return configuration.getDataSourceFactory();
        }
    }

}
