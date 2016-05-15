package github.pstorch.bahnhoefe.gpx;

import java.net.MalformedURLException;

import io.dropwizard.Application;
import io.dropwizard.configuration.EnvironmentVariableSubstitutor;
import io.dropwizard.configuration.SubstitutingSourceProvider;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;

/**
 * Bahnhoefe GPX Dropwizard App
 */
public class BahnhoefeGpxApp extends Application<BahnhoefeGpxConfiguration> {

	public static void main(final String... args) throws Exception {
		new BahnhoefeGpxApp().run(args);
	}

	@Override
	public String getName() {
		return "Bahnhoefe GPX";
	}

	@Override
	public void initialize(final Bootstrap<BahnhoefeGpxConfiguration> bootstrap) {
		bootstrap.setConfigurationSourceProvider(new SubstitutingSourceProvider(
				bootstrap.getConfigurationSourceProvider(), new EnvironmentVariableSubstitutor(false)));

	}

	@Override
	public void run(final BahnhoefeGpxConfiguration configuration, final Environment environment) throws MalformedURLException {
		final BahnhoefeGpxResource resource = new BahnhoefeGpxResource(new BahnhoefeLoader(configuration.getBahnhoefeUrl(), configuration.getPhotosUrl()));
		environment.jersey().register(resource);
	}

}
