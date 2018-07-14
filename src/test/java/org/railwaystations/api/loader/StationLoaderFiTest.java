package org.railwaystations.api.loader;

import com.jcabi.http.mock.MkAnswer;
import com.jcabi.http.mock.MkContainer;
import com.jcabi.http.mock.MkGrizzlyContainer;
import org.apache.commons.io.IOUtils;
import org.hamcrest.CoreMatchers;
import org.junit.jupiter.api.Test;
import org.railwaystations.api.ElasticBackend;
import org.railwaystations.api.model.Station;
import org.railwaystations.api.model.Country;
import org.railwaystations.api.monitoring.LoggingMonitor;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;

public class StationLoaderFiTest {

	@Test
	public void test() throws IOException {
		try (final MkContainer container = new MkGrizzlyContainer()) {
			container.next(
				new MkAnswer.Simple(
						IOUtils.resourceToString("/photosFi.json", Charset.forName("UTF-8"))
				).withHeader("Content-Type", "application/json; charset=UTF-8")
			);
			container.next(
				new MkAnswer.Simple(
						IOUtils.resourceToString("/stationsFi.json", Charset.forName("UTF-8"))
				).withHeader("Content-Type", "application/json; charset=UTF-8")
			);
			container.start();

			final StationLoader loader = new BaseStationLoader(new Country("fi"), container.home().toURL().toString(),
					container.home().toURL().toString(), new LoggingMonitor(), new ElasticBackend(""));

			final Map<Station.Key, Station> stations = loader.loadStations(new HashMap<>(), "https://railway-stations.org");

			final Station oulunkylae = stations.get(new Station.Key("fi", "1000015"));
			assertThat(oulunkylae.getKey().getId(), CoreMatchers.is("1000015"));
			assertThat(oulunkylae.getTitle(), CoreMatchers.is("Oulunkylä"));
			assertThat(oulunkylae.getCoordinates().getLat(), CoreMatchers.is(60.22926457931425));
			assertThat(oulunkylae.getCoordinates().getLon(), CoreMatchers.is(24.96850881268564));
			assertThat(oulunkylae.hasPhoto(), CoreMatchers.is(false));

			final Station tornio = stations.get(new Station.Key("fi", "1001318"));
			assertThat(tornio.getKey().getId(), CoreMatchers.is("1001318"));
			assertThat(tornio.getTitle(), CoreMatchers.is("Tornio-Itäinen"));
			assertThat(tornio.getCoordinates().getLat(), CoreMatchers.is(65.85313472639608));
			assertThat(tornio.getCoordinates().getLon(), CoreMatchers.is(24.18528739655337));
			assertThat(tornio.hasPhoto(), CoreMatchers.is(true));
			assertThat(tornio.getPhotographer(), CoreMatchers.is("Anonym"));
			assertThat(tornio.getPhotoUrl(), CoreMatchers.is("https://railway-stations.org/sites/default/files/previewbig/1000274.jpg"));
			assertThat(tornio.getLicense(), CoreMatchers.is("CC0 1.0 Universell (CC0 1.0)"));
		}
	}

}
