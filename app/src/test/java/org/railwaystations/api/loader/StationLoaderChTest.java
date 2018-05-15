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

public class StationLoaderChTest {

	@org.junit.jupiter.api.Test
	public void test() throws IOException {
		try (final MkContainer container = new MkGrizzlyContainer()) {
			container.next(
				new MkAnswer.Simple(
						IOUtils.toString(new URL("file:./src/test/resources/photosCh.json"), Charset.forName("UTF-8"))
				).withHeader("Content-Type", "application/json; charset=UTF-8")
			);
			container.next(
				new MkAnswer.Simple(
						IOUtils.toString(new URL("file:./src/test/resources/stationsCh.json"), Charset.forName("UTF-8"))
				).withHeader("Content-Type", "application/json; charset=UTF-8")
			);
			container.start();

			final StationLoader loader = new StationLoaderCh(new Country("ch"), container.home().toURL(),
					container.home().toURL());

			final Map<Integer, Station> stations = loader.loadStations(new HashMap<>(), "https://railway-stations.org");

			final Station etoy = stations.get(8501042);
			assertThat(etoy.getId(), CoreMatchers.is(8501042));
			assertThat(etoy.getTitle(), CoreMatchers.is("Etoy"));
			assertThat(etoy.getLat(), CoreMatchers.is(46.47515995026246));
			assertThat(etoy.getLon(), CoreMatchers.is(6.427139085981211));
			assertThat(etoy.hasPhoto(), CoreMatchers.is(false));

			final Station zweidlen = stations.get(8503405);
			assertThat(zweidlen.getId(), CoreMatchers.is(8503405));
			assertThat(zweidlen.getTitle(), CoreMatchers.is("Zweidlen"));
			assertThat(zweidlen.getLat(), CoreMatchers.is(47.57044026489993));
			assertThat(zweidlen.getLon(), CoreMatchers.is(8.467892062115798));
			assertThat(zweidlen.hasPhoto(), CoreMatchers.is(true));
			assertThat(zweidlen.getPhotographer(), CoreMatchers.is("@Mac4Me"));
			assertThat(zweidlen.getPhotoUrl(), CoreMatchers.is("https://railway-stations.org/sites/default/files/previewbig/%40Mac4Me-8503405_BF.jpg"));
			assertThat(zweidlen.getLicense(), CoreMatchers.is("CC0 1.0 Universell (CC0 1.0)"));
		}
	}

}
