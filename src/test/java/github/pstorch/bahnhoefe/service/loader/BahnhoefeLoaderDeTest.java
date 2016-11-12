package github.pstorch.bahnhoefe.service.loader;

import com.jcabi.http.mock.MkAnswer;
import com.jcabi.http.mock.MkContainer;
import com.jcabi.http.mock.MkGrizzlyContainer;
import github.pstorch.bahnhoefe.service.Bahnhof;
import org.hamcrest.CoreMatchers;
import org.junit.Test;

import java.io.IOException;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;

@SuppressWarnings({"PMD.JUnitTestContainsTooManyAsserts", "PMD.JUnitAssertionsShouldIncludeMessage", "PMD.ProhibitPlainJunitAssertionsRule"})
public class BahnhoefeLoaderDeTest {

	@Test
	public void test() throws IOException {
		try (final MkContainer container = new MkGrizzlyContainer()) {
			container.next(new MkAnswer.Simple(
					"{\"took\":2,\"timed_out\":false,\"_shards\":{\"total\":5,\"successful\":5,\"failed\":0},\"hits\":{\"total\":5652,\"max_score\":1.0,\"hits\":[{\"_index\":\"bahnhoefe\",\"_type\":\"bahnhof\",\"_id\":\"AVLb3n3hwbtQZche7aaV\",\"_score\":1.0,\"_source\":{\"Bundesland\":\"Schleswig-Holstein\",\"Bahnhofsmanagement\":\"Schleswig-Holstein\",\"BahnhofNr\":41,\"title\":\"Albersdorf\",\"DS100\":\"AAL\",\"Bahnhofskategorie\":7,\"Strasse\":\"Bahnhofstr. 1\",\"PLZ\":25767,\"Ort\":\"Albersdorf\",\"Aufgabenträger\":\"LVS Schleswig-Holstein Landesweite Verkehrsservicegesellschaft mbH\",\"Verkehrsverb\":\"0\",\"Fernverkehr\":\"nein\",\"Nahverkehr\":\"ja\",\"lat\":54.1461697552048,\"lon\":9.29245591163636}},{\"_index\":\"bahnhoefe\",\"_type\":\"bahnhof\",\"_id\":\"AVLb3n5QwbtQZche7bja\",\"_score\":1.0,\"_source\":{\"Bundesland\":\"Rheinland-Pfalz\",\"Bahnhofsmanagement\":\"Kaiserslautern\",\"BahnhofNr\":7066,\"title\":\"Zweibrücken Hbf\",\"DS100\":\"SZW\",\"Bahnhofskategorie\":5,\"Strasse\":\"Poststr. 37\",\"PLZ\":66482,\"Ort\":\"Zweibrücken\",\"Aufgabenträger\":\"Zweckverband Schienenpersonennahverkehr Rheinland-Pfalz Süd\",\"Verkehrsverb\":\"VRN\",\"Fernverkehr\":\"nein\",\"Nahverkehr\":\"ja\",\"lat\":49.2467252285295,\"lon\":7.35692381858826}}]}}"));
			container.next(new MkAnswer.Simple(
					"[{\"bahnhofsname\":\"Zweibr\\u00fccken Hbf\",\"bahnhofsnr\":\"7066\",\"bahnhofsfoto\":\" http:\\/\\/www.deutschlands-bahnhoefe.org\\/sites\\/default\\/files\\/previewbig\\/7066_1.jpg\",\"fotograf\":\"http:\\/\\/www.deutschlands-bahnhoefe.org\\/node\\/278 \",\"lizenz\":\"CC0 1.0 Universell (CC0 1.0) \",\"bahnhofsfoto480\":\"  http:\\/\\/www.deutschlands-bahnhoefe.org\\/sites\\/default\\/files\\/styles\\/large\\/public\\/previewbig\\/7066_1.jpg?itok=1zGEsvom\\n\", \"fotograf-title\": \"@hessenpfaelzer\"},{\"bahnhofsname\":\"Z\\u00fcssow\",\"bahnhofsnr\":\"7063\",\"bahnhofsfoto\":\" http:\\/\\/www.deutschlands-bahnhoefe.org\\/sites\\/default\\/files\\/previewbig\\/7063_0.jpg\",\"fotograf\":\"http:\\/\\/www.deutschlands-bahnhoefe.org\\/node\\/157 \",\"lizenz\":\"CC0 1.0 Universell (CC0 1.0)\",\"bahnhofsfoto480\":\"  http:\\/\\/www.deutschlands-bahnhoefe.org\\/sites\\/default\\/files\\/styles\\/large\\/public\\/previewbig\\/7063_0.jpg?itok=PG48v2wy\\n\"}]"));
			container.start();

			final BahnhoefeLoader loader = new BahnhoefeLoaderDe(container.home().toURL(),
					container.home().toURL());

			final Map<Integer, Bahnhof> bahnhoefe = loader.loadBahnhoefe();

			final Bahnhof zweibruecken = bahnhoefe.get(7066);
			assertThat(zweibruecken.getId(), CoreMatchers.is(7066));
			assertThat(zweibruecken.getTitle(), CoreMatchers.is("Zweibrücken Hbf"));
			assertThat(zweibruecken.getLat(), CoreMatchers.is(49.2467252285295));
			assertThat(zweibruecken.getLon(), CoreMatchers.is(7.35692381858826));
			assertThat(zweibruecken.hasPhoto(), CoreMatchers.is(true));
			assertThat(zweibruecken.getFotograf(), CoreMatchers.is("@hessenpfaelzer"));

			final Bahnhof albersdorf = bahnhoefe.get(41);
			assertThat(albersdorf.getId(), CoreMatchers.is(41));
			assertThat(albersdorf.getTitle(), CoreMatchers.is("Albersdorf"));
			assertThat(albersdorf.getLat(), CoreMatchers.is(54.1461697552048));
			assertThat(albersdorf.getLon(), CoreMatchers.is(9.29245591163636));
			assertThat(albersdorf.hasPhoto(), CoreMatchers.is(false));
		}
	}

}
