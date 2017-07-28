package org.railwaystations.api.loader;

import com.jcabi.http.mock.MkAnswer;
import com.jcabi.http.mock.MkContainer;
import com.jcabi.http.mock.MkGrizzlyContainer;
import org.hamcrest.CoreMatchers;
import org.junit.Test;
import org.railwaystations.api.model.Bahnhof;

import java.io.IOException;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;

public class BahnhoefeLoaderFiTest {

	@Test
	public void test() throws IOException {
		try (final MkContainer container = new MkGrizzlyContainer()) {
			container.next(
				new MkAnswer.Simple(
					"[{\"bahnhofsname\":\"Tornio It\\u00e4inen\",\"bahnhofsnr\":\"\",\"ibnr\":\"1001318\",\"bahnhofsfoto\":\"https:\\/\\/railway-stations.org\\/sites\\/default\\/files\\/previewbig\\/1000274.jpg\",\"fotograf\":\"https:\\/\\/railway-stations.org\\/node\\/157\",\"lizenz\":\"CC0 1.0 Universell (CC0 1.0) \",\"bahnhofsfoto480\":\"\\/sites\\/default\\/files\\/styles\\/large\\/public\\/previewbig\\/1000274.jpg?itok=yO-Tk2cN\",\"fotograf-title\":\"Anonym\",\"flagr\":\"1\",\"genrische_bahnhofsnr\":\"\"},{\"bahnhofsname\":\"Rovaniemi\",\"bahnhofsnr\":\"\",\"ibnr\":\"1000364\",\"bahnhofsfoto\":\"https:\\/\\/railway-stations.org\\/sites\\/default\\/files\\/previewbig\\/1000086.jpg\",\"fotograf\":\"https:\\/\\/railway-stations.org\\/node\\/290\",\"lizenz\":\"CC0 1.0 Universell (CC0 1.0) \",\"bahnhofsfoto480\":\"\\/sites\\/default\\/files\\/styles\\/large\\/public\\/previewbig\\/1000086.jpg?itok=8Hrfet5d\",\"fotograf-title\":\"@harald_legner\",\"flagr\":\"0\",\"genrische_bahnhofsnr\":\"\"},{\"bahnhofsname\":\"Keuruu\",\"bahnhofsnr\":\"\",\"ibnr\":\"1000235\",\"bahnhofsfoto\":\"https:\\/\\/railway-stations.org\\/sites\\/default\\/files\\/previewbig\\/1000022.jpg\",\"fotograf\":\"https:\\/\\/railway-stations.org\\/node\\/290\",\"lizenz\":\"CC0 1.0 Universell (CC0 1.0) \",\"bahnhofsfoto480\":\"\\/sites\\/default\\/files\\/styles\\/large\\/public\\/previewbig\\/1000022.jpg?itok=9fxq71EO\",\"fotograf-title\":\"@harald_legner\",\"flagr\":\"0\",\"genrische_bahnhofsnr\":\"\"},{\"bahnhofsname\":\"Kemi\",\"bahnhofsnr\":\"\",\"ibnr\":\"1000347\",\"bahnhofsfoto\":\"https:\\/\\/railway-stations.org\\/sites\\/default\\/files\\/previewbig\\/1000020.jpg\",\"fotograf\":\"https:\\/\\/railway-stations.org\\/node\\/157\",\"lizenz\":\"CC0 1.0 Universell (CC0 1.0) \",\"bahnhofsfoto480\":\"\\/sites\\/default\\/files\\/styles\\/large\\/public\\/previewbig\\/1000020.jpg?itok=VUs50cuE\",\"fotograf-title\":\"Anonym\",\"flagr\":\"1\",\"genrische_bahnhofsnr\":\"\"},{\"bahnhofsname\":\"Helsinki\",\"bahnhofsnr\":\"\",\"ibnr\":\"1000001\",\"bahnhofsfoto\":\"https:\\/\\/railway-stations.org\\/sites\\/default\\/files\\/previewbig\\/1096001.jpg\",\"fotograf\":\"https:\\/\\/railway-stations.org\\/node\\/157\",\"lizenz\":\"CC0 1.0 Universell (CC0 1.0) \",\"bahnhofsfoto480\":\"\\/sites\\/default\\/files\\/styles\\/large\\/public\\/previewbig\\/1096001.jpg?itok=wStsttsP\",\"fotograf-title\":\"Anonym\",\"flagr\":\"1\",\"genrische_bahnhofsnr\":\"\"}]"
				).withHeader("Content-Type", "application/json; charset=UTF-8")
			);
			container.next(
					new MkAnswer.Simple(
							"[]"
					).withHeader("Content-Type", "application/json; charset=UTF-8")
			);
			container.next(
				new MkAnswer.Simple(
					"{\"took\":9,\"timed_out\":false,\"_shards\":{\"total\":5,\"successful\":5,\"failed\":0},\"hits\":{\"total\":207,\"max_score\":1.0,\"hits\":[{\"_index\":\"bahnhoefefi\",\"_type\":\"bahnhoffi\",\"_id\":\"AV1LocGG51DZm_biJv7z\",\"_score\":1.0,\"_source\":{\"type\":\"Feature\",\"geometry\":{\"type\":\"Point\",\"coordinates\":[24.96850881268564,60.22926457931425]},\"properties\":{\"name\":\"Oulunkylä\",\"ID\":1000015,\"passengerTraffic\":\"True\",\"type\":\"STATION\",\"abkuerzung\":\"OLK\",\"UICIBNR\":1000015,\"stationUICCode\":15,\"countryCode\":\"FI\",\"extid\":28855,\"slug\":\"oulunkyla\",\"DBIBNR\":1000199,\"PLZ\":\"\",\"Ort\":\"\",\"Strasse\":\"\",\"Betreiber\":\"\"}}},{\"_index\":\"bahnhoefefi\",\"_type\":\"bahnhoffi\",\"_id\":\"AV1LocGG51DZm_biJv73\",\"_score\":1.0,\"_source\":{\"type\":\"Feature\",\"geometry\":{\"type\":\"Point\",\"coordinates\":[25.10650923336211,60.40445695324774]},\"properties\":{\"name\":\"Kerava asema\",\"ID\":1000020,\"passengerTraffic\":\"True\",\"type\":\"STATION\",\"abkuerzung\":\"KE\",\"UICIBNR\":1000020,\"stationUICCode\":20,\"countryCode\":\"FI\",\"extid\":28782,\"slug\":\"kerava-asema\",\"DBIBNR\":1000098,\"PLZ\":\"\",\"Ort\":\"\",\"Strasse\":\"\",\"Betreiber\":\"\"}}},{\"_index\":\"bahnhoefefi\",\"_type\":\"bahnhoffi\",\"_id\":\"AV1LocGK51DZm_biJv-4\",\"_score\":1.0,\"_source\":{\"type\":\"Feature\",\"geometry\":{\"type\":\"Point\",\"coordinates\":[24.9358411564204,60.20741926594994]},\"properties\":{\"name\":\"Pasila autojuna-asema\",\"ID\":1001328,\"passengerTraffic\":\"True\",\"type\":\"STATION\",\"abkuerzung\":\"PAU\",\"UICIBNR\":1001328,\"stationUICCode\":1328,\"countryCode\":\"FI\",\"extid\":28862,\"slug\":\"pasila-autojuna-asema\",\"DBIBNR\":null,\"PLZ\":\"\",\"Ort\":\"\",\"Strasse\":\"\",\"Betreiber\":\"\"}}},{\"_index\":\"bahnhoefefi\",\"_type\":\"bahnhoffi\",\"_id\":\"AV1LocGK51DZm_biJv-3\",\"_score\":1.0,\"_source\":{\"type\":\"Feature\",\"geometry\":{\"type\":\"Point\",\"coordinates\":[24.18528739655337,65.85313472639608]},\"properties\":{\"name\":\"Tornio-Itäinen\",\"ID\":1001318,\"passengerTraffic\":\"True\",\"type\":\"STOPPING_POINT\",\"abkuerzung\":\"TRI\",\"UICIBNR\":1001318,\"stationUICCode\":1318,\"countryCode\":\"FI\",\"extid\":28906,\"slug\":\"tornio-itainen\",\"DBIBNR\":1000274,\"PLZ\":\"\",\"Ort\":\"\",\"Strasse\":\"\",\"Betreiber\":\"\"}}}]}}"
				).withHeader("Content-Type", "application/json; charset=UTF-8")
			);
			container.start();

			final BahnhoefeLoader loader = new BahnhoefeLoaderFi(container.home().toURL(),
					container.home().toURL());

			final Map<Integer, Bahnhof> bahnhoefe = loader.loadBahnhoefe();

			final Bahnhof oulunkylae = bahnhoefe.get(1000015);
			assertThat(oulunkylae.getId(), CoreMatchers.is(1000015));
			assertThat(oulunkylae.getTitle(), CoreMatchers.is("Oulunkylä"));
			assertThat(oulunkylae.getLat(), CoreMatchers.is(60.22926457931425));
			assertThat(oulunkylae.getLon(), CoreMatchers.is(24.96850881268564));
			assertThat(oulunkylae.hasPhoto(), CoreMatchers.is(false));

			final Bahnhof tornio = bahnhoefe.get(1001318);
			assertThat(tornio.getId(), CoreMatchers.is(1001318));
			assertThat(tornio.getTitle(), CoreMatchers.is("Tornio-Itäinen"));
			assertThat(tornio.getLat(), CoreMatchers.is(65.85313472639608));
			assertThat(tornio.getLon(), CoreMatchers.is(24.18528739655337));
			assertThat(tornio.hasPhoto(), CoreMatchers.is(true));
			assertThat(tornio.getPhotographer(), CoreMatchers.is("Anonym"));
			assertThat(tornio.getPhotoUrl(), CoreMatchers.is("https://railway-stations.org/sites/default/files/previewbig/1000274.jpg"));
			assertThat(tornio.getLicense(), CoreMatchers.is("CC0 1.0 Universell (CC0 1.0) "));
		}
	}

}
