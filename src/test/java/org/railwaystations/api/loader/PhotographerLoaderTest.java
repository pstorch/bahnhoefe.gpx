package org.railwaystations.api.loader;

import com.jcabi.http.mock.MkAnswer;
import com.jcabi.http.mock.MkContainer;
import com.jcabi.http.mock.MkGrizzlyContainer;
import org.hamcrest.CoreMatchers;
import org.junit.jupiter.api.Test;
import org.railwaystations.api.ElasticBackend;
import org.railwaystations.api.model.Photographer;

import java.io.IOException;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;

public class PhotographerLoaderTest {

	@Test
	public void test() throws IOException {
		try (final MkContainer container = new MkGrizzlyContainer()) {
			container.next(
				new MkAnswer.Simple(
					"{\"took\":1,\"timed_out\":false,\"_shards\":{\"total\":5,\"successful\":5,\"failed\":0},\"hits\":{\"total\":2,\"max_score\":1.0,\"hits\":[\n" +
							"  {\"_index\":\"elastictest\",\"_type\":\"fotograf\",\"_id\":\"AWIWHFHsRqXmo6ccruCX\",\"_score\":1.0,\"_source\":{\n" +
							"\"fotografenname\": \"SimoneDoerfling\",\n" +
							"\"fotografenURL\": \"http://www.deutschlands-bahnhoefe.de/\",\n" +
							"\"fotografenlizenz\": \"CC0 1.0 Universell (CC0 1.0) \",\n" +
							"\"fotografenlizenzURL\": \"http://creativecommons.org/publicdomain/zero/1.0/deed.de\"\n" +
							"}}]}}"
				).withHeader("Content-Type", "application/json; charset=UTF-8")
			);
			container.start();

			final PhotographerLoader loader = new PhotographerLoader(container.home().toURL().toString(), new ElasticBackend(""));

			final Map<String, Photographer> photographers = loader.loadPhotographers();
			assertThat(photographers.size(), CoreMatchers.is(1));

			final Photographer photographer = photographers.get("SimoneDoerfling");
			assertThat(photographer.getName(), CoreMatchers.is("SimoneDoerfling"));
			assertThat(photographer.getUrl(), CoreMatchers.is("http://www.deutschlands-bahnhoefe.de/"));
			assertThat(photographer.getLicense(), CoreMatchers.is("CC0 1.0 Universell (CC0 1.0)"));
		}
	}

}
