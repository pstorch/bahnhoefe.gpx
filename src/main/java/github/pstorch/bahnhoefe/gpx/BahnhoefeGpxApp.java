package github.pstorch.bahnhoefe.gpx;

import io.dropwizard.Application;
import io.dropwizard.configuration.EnvironmentVariableSubstitutor;
import io.dropwizard.configuration.SubstitutingSourceProvider;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;

/**
 * Bahnhoefe GPX Dropwizard App
 */
public class BahnhoefeGpxApp extends Application<BahnhoefeGpxConfiguration> {

	public static void main(String[] args) throws Exception {
		new BahnhoefeGpxApp().run(args);
	}

	@Override
	public String getName() {
		return "Bahnhoefe GPX";
	}

	@Override
	public void initialize(Bootstrap<BahnhoefeGpxConfiguration> bootstrap) {
		bootstrap.setConfigurationSourceProvider(new SubstitutingSourceProvider(
				bootstrap.getConfigurationSourceProvider(), new EnvironmentVariableSubstitutor(false)));

	}

	@Override
	public void run(BahnhoefeGpxConfiguration configuration, Environment environment) {
		final BahnhoefeGpxResource resource = new BahnhoefeGpxResource(configuration.getBahnhoefeUrl(),
				configuration.getPhotosUrl());
		environment.jersey().register(resource);
	}

}
