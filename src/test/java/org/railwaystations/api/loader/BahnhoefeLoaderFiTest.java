package org.railwaystations.api.loader;

import com.jcabi.http.mock.MkAnswer;
import com.jcabi.http.mock.MkContainer;
import com.jcabi.http.mock.MkGrizzlyContainer;
import org.apache.commons.io.IOUtils;
import org.hamcrest.CoreMatchers;
import org.junit.Test;
import org.railwaystations.api.model.Station;
import org.railwaystations.api.model.Country;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;

public class BahnhoefeLoaderFiTest {

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
						IOUtils.resourceToString("/bahnhoefeFi.json", Charset.forName("UTF-8"))
				).withHeader("Content-Type", "application/json; charset=UTF-8")
			);
			container.start();

			final BahnhoefeLoader loader = new BaseBahnhoefeLoader(new Country("fi"), container.home().toURL(),
					container.home().toURL());

			final Map<Integer, Station> bahnhoefe = loader.loadBahnhoefe(new HashMap<>(), "https://railway-stations.org");

			final Station oulunkylae = bahnhoefe.get(1000015);
			assertThat(oulunkylae.getId(), CoreMatchers.is(1000015));
			assertThat(oulunkylae.getTitle(), CoreMatchers.is("Oulunkylä"));
			assertThat(oulunkylae.getLat(), CoreMatchers.is(60.22926457931425));
			assertThat(oulunkylae.getLon(), CoreMatchers.is(24.96850881268564));
			assertThat(oulunkylae.hasPhoto(), CoreMatchers.is(false));

			final Station tornio = bahnhoefe.get(1001318);
			assertThat(tornio.getId(), CoreMatchers.is(1001318));
			assertThat(tornio.getTitle(), CoreMatchers.is("Tornio-Itäinen"));
			assertThat(tornio.getLat(), CoreMatchers.is(65.85313472639608));
			assertThat(tornio.getLon(), CoreMatchers.is(24.18528739655337));
			assertThat(tornio.hasPhoto(), CoreMatchers.is(true));
			assertThat(tornio.getPhotographer(), CoreMatchers.is("Anonym"));
			assertThat(tornio.getPhotoUrl(), CoreMatchers.is("https://railway-stations.org/sites/default/files/previewbig/1000274.jpg"));
			assertThat(tornio.getLicense(), CoreMatchers.is("CC0 1.0 Universell (CC0 1.0)"));
		}
	}

}
