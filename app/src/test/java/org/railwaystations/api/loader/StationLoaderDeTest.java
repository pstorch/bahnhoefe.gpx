package org.railwaystations.api.loader;

import com.jcabi.http.mock.MkAnswer;
import com.jcabi.http.mock.MkContainer;
import com.jcabi.http.mock.MkGrizzlyContainer;
import org.apache.commons.io.IOUtils;
import org.hamcrest.CoreMatchers;
import org.railwaystations.api.model.Country;
import org.railwaystations.api.model.Station;

import java.io.IOException;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;

public class StationLoaderDeTest {

	@org.junit.jupiter.api.Test
	public void test() throws IOException {
		try (final MkContainer container = new MkGrizzlyContainer()) {
			container.next(
				new MkAnswer.Simple(
						IOUtils.toString(new URL("file:./src/test/resources/photosDe.json"), Charset.forName("UTF-8"))
				).withHeader("Content-Type", "application/json; charset=UTF-8")
			);
			container.next(
				new MkAnswer.Simple(
						IOUtils.toString(new URL("file:./src/test/resources/stationsDe.json"), Charset.forName("UTF-8"))
				).withHeader("Content-Type", "application/json; charset=UTF-8")
			);
			container.start();

			final StationLoader loader = new StationLoaderDe(new Country("de"), container.home().toURL(),
					container.home().toURL());

			final Map<Integer, Station> stations = loader.loadStations(new HashMap<>(), "http://www.deutschlands-bahnhoefe.org");

			final Station zweibruecken = stations.get(7066);
			assertThat(zweibruecken.getId(), CoreMatchers.is(7066));
			assertThat(zweibruecken.getTitle(), CoreMatchers.is("Zweibrücken Hbf"));
			assertThat(zweibruecken.getLat(), CoreMatchers.is(49.2467252285295));
			assertThat(zweibruecken.getLon(), CoreMatchers.is(7.35692381858826));
			assertThat(zweibruecken.hasPhoto(), CoreMatchers.is(true));
			assertThat(zweibruecken.getPhotographer(), CoreMatchers.is("@hessenpfaelzer"));
			assertThat(zweibruecken.getDS100(), CoreMatchers.is("SZW"));
			assertThat(zweibruecken.getPhotoUrl(), CoreMatchers.is("http://www.deutschlands-bahnhoefe.org/sites/default/files/previewbig/7066_1.jpg"));
			assertThat(zweibruecken.getLicense(), CoreMatchers.is("CC0 1.0 Universell (CC0 1.0)"));
			assertThat(zweibruecken.getCreatedAt(), CoreMatchers.is(1523044367000L));

			final Station albersdorf = stations.get(41);
			assertThat(albersdorf.getId(), CoreMatchers.is(41));
			assertThat(albersdorf.getTitle(), CoreMatchers.is("Albersdorf"));
			assertThat(albersdorf.getLat(), CoreMatchers.is(54.1461697552048));
			assertThat(albersdorf.getLon(), CoreMatchers.is(9.29245591163636));
			assertThat(albersdorf.hasPhoto(), CoreMatchers.is(false));
			assertThat(albersdorf.getDS100(), CoreMatchers.is("AAL"));
		}
	}

}
