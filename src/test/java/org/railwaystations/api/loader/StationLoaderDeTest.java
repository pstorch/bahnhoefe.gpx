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

public class StationLoaderDeTest {

	@Test
	public void test() throws IOException {
		try (final MkContainer container = new MkGrizzlyContainer()) {
			container.next(
				new MkAnswer.Simple(
						IOUtils.resourceToString("/photosDe.json", Charset.forName("UTF-8"))
				).withHeader("Content-Type", "application/json; charset=UTF-8")
			);
			container.next(
				new MkAnswer.Simple(
						IOUtils.resourceToString("/stationsDe.json", Charset.forName("UTF-8"))
				).withHeader("Content-Type", "application/json; charset=UTF-8")
			);
			container.next(
					new MkAnswer.Simple(
							IOUtils.resourceToString("/stationsDe2.json", Charset.forName("UTF-8"))
					).withHeader("Content-Type", "application/json; charset=UTF-8")
			);
			container.start();

			final StationLoader loader = new StationLoaderDe(new Country("de"), container.home().toURL().toString(),
					container.home().toURL().toString(), new LoggingMonitor(), new ElasticBackend(""));

			final Map<Station.Key, Station> stations = loader.loadStations(new HashMap<>(), "http://www.deutschlands-bahnhoefe.org");

			final Station zweibruecken = stations.get(new Station.Key("de", "7066"));
			assertThat(zweibruecken.getKey().getId(), CoreMatchers.is("7066"));
			assertThat(zweibruecken.getTitle(), CoreMatchers.is("Zweibrücken Hbf"));
			assertThat(zweibruecken.getCoordinates().getLat(), CoreMatchers.is(49.2467252285295));
			assertThat(zweibruecken.getCoordinates().getLon(), CoreMatchers.is(7.35692381858826));
			assertThat(zweibruecken.hasPhoto(), CoreMatchers.is(true));
			assertThat(zweibruecken.getPhotographer(), CoreMatchers.is("@hessenpfaelzer"));
			assertThat(zweibruecken.getDS100(), CoreMatchers.is("SZW"));
			assertThat(zweibruecken.getPhotoUrl(), CoreMatchers.is("http://www.deutschlands-bahnhoefe.org/sites/default/files/previewbig/7066_1.jpg"));
			assertThat(zweibruecken.getLicense(), CoreMatchers.is("CC0 1.0 Universell (CC0 1.0)"));
			assertThat(zweibruecken.getCreatedAt(), CoreMatchers.is(1523044367000L));

			final Station albersdorf = stations.get(new Station.Key("de", "41"));
			assertThat(albersdorf.getKey().getId(), CoreMatchers.is("41"));
			assertThat(albersdorf.getTitle(), CoreMatchers.is("Albersdorf"));
			assertThat(albersdorf.getCoordinates().getLat(), CoreMatchers.is(54.1461697552048));
			assertThat(albersdorf.getCoordinates().getLon(), CoreMatchers.is(9.29245591163636));
			assertThat(albersdorf.hasPhoto(), CoreMatchers.is(false));
			assertThat(albersdorf.getDS100(), CoreMatchers.is("AAL"));
		}
	}

}
