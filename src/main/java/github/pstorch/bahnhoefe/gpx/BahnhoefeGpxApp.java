package github.pstorch.bahnhoefe.gpx;

import io.dropwizard.Application;
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
	}

	@Override
	public void run(BahnhoefeGpxConfiguration configuration, Environment environment) {
		final BahnhoefeGpxResource resource = new BahnhoefeGpxResource(configuration.getBahnhoefeUrl(),
				configuration.getPhotosUrl());
		environment.jersey().register(resource);
	}

}
