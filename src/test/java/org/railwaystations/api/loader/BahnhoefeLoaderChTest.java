package org.railwaystations.api.loader;

import com.jcabi.http.mock.MkAnswer;
import com.jcabi.http.mock.MkContainer;
import com.jcabi.http.mock.MkGrizzlyContainer;
import org.railwaystations.api.model.Bahnhof;
import org.hamcrest.CoreMatchers;
import org.junit.Test;

import java.io.IOException;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;

public class BahnhoefeLoaderChTest {

	@Test
	public void test() throws IOException {
		try (final MkContainer container = new MkGrizzlyContainer()) {
			container.next(
				new MkAnswer.Simple(
					"[{\"bahnhofsname\":\"Küngoldingen\",\"ibnr\":\"8502120\",\"bahnhofsfoto\":\"http:\\/\\/www.deutschlands-bahnhoefe.org\\/sites\\/default\\/files\\/previewbig\\/8503006.jpg\",\"fotograf\":\"http:\\/\\/www.deutschlands-bahnhoefe.org\\/node\\/1473 \",\"lizenz\":\"CC0 1.0 Universell (CC0 1.0) \",\"bahnhofsfoto480\":\"\\/sites\\/default\\/files\\/styles\\/large\\/public\\/previewbig\\/8503006.jpg?itok=cGcfrbBm\",\"flagr\":\"0\",\"fotograf-title\":\"@pokipsie\"}]}"
				).withHeader("Content-Type", "application/json; charset=UTF-8")
			);
			container.next(
					new MkAnswer.Simple(
							"[]"
					).withHeader("Content-Type", "application/json; charset=UTF-8")
			);
			container.next(
				new MkAnswer.Simple(
					"{\"took\":19,\"timed_out\":false,\"_shards\":{\"total\":5,\"successful\":5,\"failed\":0},\"hits\":{\"total\":1733,\"max_score\":1.0,\"hits\":[{\"_index\":\"bahnhoefech\",\"_type\":\"bahnhofch\",\"_id\":\"AVfycusBwbtQZche7cDc\",\"_score\":1.0,\"_source\":{\"datasetid\":\"didok-liste\",\"recordid\":\"7a41451eba36a7b6ee9270d632f4f56cafb2a25b\",\"fields\":{\"geopos\":[46.47515995026246,6.427139085981211],\"betriebspunkttyp\":\"Haltestelle\",\"abkuerzung\":\"ETOY\",\"didok85\":8501042,\"didok\":1042,\"lod\":\"http://lod.opentransportdata.swiss/didok/8501042\",\"x_koord_nord\":147595,\"xtf_id\":\"ch14uvag00045514\",\"beginngueltigkeit\":\"2014-12-01\",\"bearbeitungsdatum\":20151204,\"tunummer\":1,\"dst_abk\":\"ETOY\",\"gdenummer\":5636,\"y_koord_ost\":522318,\"nummer\":8501042,\"verkehrsmittel\":\"Zug\",\"gdename\":\"Etoy\",\"name\":\"Etoy\",\"tuabkuerzung\":\"SBBCFFFFS\",\"datenherrabkuerzung\":\"SBBCFFFFS\",\"stand\":\"2015-12-13\",\"hoehe\":406},\"geometry\":{\"type\":\"Point\",\"coordinates\":[46.47515995026246, 6.427139085981211]},\"record_timestamp\":\"2016-05-03T22:14:34+02:00\"}},{\"_index\":\"bahnhoefech\",\"_type\":\"bahnhofch\",\"_id\":\"AVfycusBwbtQZche7cDg\",\"_score\":1.0,\"_source\":{\"datasetid\":\"didok-liste\",\"recordid\":\"053738e5c620d6dc0e40d70c6c18711afea83998\",\"fields\":{\"geopos\":[47.30746905671105,7.941984790513125],\"betriebspunkttyp\":\"Haltestelle\",\"abkuerzung\":\"KGD\",\"didok85\":8502120,\"didok\":2120,\"lod\":\"http://lod.opentransportdata.swiss/didok/8502120\",\"x_koord_nord\":239743,\"xtf_id\":\"ch14uvag00050130\",\"beginngueltigkeit\":\"2014-12-01\",\"bearbeitungsdatum\":20151204,\"tunummer\":1,\"dst_abk\":\"KGD\",\"gdenummer\":4280,\"y_koord_ost\":638064,\"nummer\":8502120,\"verkehrsmittel\":\"Zug\",\"gdename\":\"Oftringen\",\"name\":\"Küngoldingen\",\"tuabkuerzung\":\"SBBCFFFFS\",\"datenherrabkuerzung\":\"SBBCFFFFS\",\"stand\":\"2015-12-13\",\"hoehe\":438},\"geometry\":{\"type\":\"Point\",\"coordinates\":[47.30746905671105,7.941984790513125]},\"record_timestamp\":\"2016-05-03T22:14:34+02:00\"}}]}}"
				).withHeader("Content-Type", "application/json; charset=UTF-8")
			);
			container.start();

			final BahnhoefeLoader loader = new BahnhoefeLoaderCh(container.home().toURL(),
					container.home().toURL());

			final Map<Integer, Bahnhof> bahnhoefe = loader.loadBahnhoefe();

			final Bahnhof etoy = bahnhoefe.get(8501042);
			assertThat(etoy.getId(), CoreMatchers.is(8501042));
			assertThat(etoy.getTitle(), CoreMatchers.is("Etoy"));
			assertThat(etoy.getLat(), CoreMatchers.is(6.427139085981211));
			assertThat(etoy.getLon(), CoreMatchers.is(46.47515995026246));
			assertThat(etoy.hasPhoto(), CoreMatchers.is(false));

			final Bahnhof kuengoldingen = bahnhoefe.get(8502120);
			assertThat(kuengoldingen.getId(), CoreMatchers.is(8502120));
			assertThat(kuengoldingen.getTitle(), CoreMatchers.is("Küngoldingen"));
			assertThat(kuengoldingen.getLat(), CoreMatchers.is(7.941984790513125));
			assertThat(kuengoldingen.getLon(), CoreMatchers.is(47.30746905671105));
			assertThat(kuengoldingen.hasPhoto(), CoreMatchers.is(true));
			assertThat(kuengoldingen.getPhotographer(), CoreMatchers.is("@pokipsie"));
			assertThat(kuengoldingen.getPhotoUrl(), CoreMatchers.is("http://www.deutschlands-bahnhoefe.org/sites/default/files/previewbig/8503006.jpg"));
			assertThat(kuengoldingen.getLicense(), CoreMatchers.is("CC0 1.0 Universell (CC0 1.0) "));
		}
	}

}
