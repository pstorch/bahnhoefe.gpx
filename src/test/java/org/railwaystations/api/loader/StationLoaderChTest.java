package org.railwaystations.api.loader;

import com.jcabi.http.mock.MkAnswer;
import com.jcabi.http.mock.MkContainer;
import com.jcabi.http.mock.MkGrizzlyContainer;
import org.apache.commons.io.IOUtils;
import org.hamcrest.CoreMatchers;
import org.junit.jupiter.api.Test;
import org.railwaystations.api.model.Station;
import org.railwaystations.api.model.Country;
import org.railwaystations.api.model.User;
import org.railwaystations.api.monitoring.LoggingMonitor;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;

public class StationLoaderChTest {

	@Test
	public void test() throws IOException {
		try (final MkContainer container = new MkGrizzlyContainer()) {
			container.next(
				new MkAnswer.Simple(
						IOUtils.resourceToString("/photosCh.json", Charset.forName("UTF-8"))
				).withHeader("Content-Type", "application/json; charset=UTF-8")
			);
			container.next(
				new MkAnswer.Simple(
						IOUtils.resourceToString("/stationsCh.json", Charset.forName("UTF-8"))
				).withHeader("Content-Type", "application/json; charset=UTF-8")
			);
			container.start();

			final StationLoader loader = new StationLoader(
					new Country("ch", "Schweiz", null, null, null,
						container.home().toURL().toString(), container.home().toURL().toString()),
						new LoggingMonitor(), new ElasticBackend(""));

			final Map<Integer, User> users = new HashMap<>(2);
			users.put(3, new User("@Mac4Me","http://www.twitter.com/Mac4Me", "CC0 1.0 Universell (CC0 1.0)", 3, null, null, true, true, null));
			users.put(5, new User("@pokipsie","http://www.martinrechsteiner.ch", "CC0 1.0 Universell (CC0 1.0)", 5, null, null, true, false, null));

			final Map<Station.Key, Station> stations = loader.loadStations(users, "https://railway-stations.org");

			final Station oulunkylae = stations.get(new Station.Key("ch", "8503501"));
			assertThat(oulunkylae.getKey().getId(), CoreMatchers.is("8503501"));
			assertThat(oulunkylae.getTitle(), CoreMatchers.is("Döttingen"));
			assertThat(oulunkylae.getCoordinates().getLat(), CoreMatchers.is(47.5752638935));
			assertThat(oulunkylae.getCoordinates().getLon(), CoreMatchers.is(8.25634387912));
			assertThat(oulunkylae.hasPhoto(), CoreMatchers.is(false));

			final Station tornio = stations.get(new Station.Key("ch", "8503006"));
			assertThat(tornio.getKey().getId(), CoreMatchers.is("8503006"));
			assertThat(tornio.getTitle(), CoreMatchers.is("Zürich Oerlikon"));
			assertThat(tornio.getCoordinates().getLat(), CoreMatchers.is(47.4115288802));
			assertThat(tornio.getCoordinates().getLon(), CoreMatchers.is(8.54411523121));
			assertThat(tornio.hasPhoto(), CoreMatchers.is(true));
			assertThat(tornio.getPhotographer(), CoreMatchers.is("@pokipsie"));
			assertThat(tornio.getPhotoUrl(), CoreMatchers.is("https://railway-stations.org/sites/default/files/previewbig/8503006_0.jpg"));
			assertThat(tornio.getLicense(), CoreMatchers.is("CC0 1.0 Universell (CC0 1.0)"));
		}
	}

}
