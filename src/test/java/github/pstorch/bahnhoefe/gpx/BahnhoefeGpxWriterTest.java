package github.pstorch.bahnhoefe.gpx;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.WebApplicationException;

import org.hamcrest.CoreMatchers;
import org.hamcrest.MatcherAssert;
import org.junit.Test;

public class BahnhoefeGpxWriterTest {

	@Test
	public void testWriteTo() throws WebApplicationException, IOException {
		final List<Bahnhof> bahnhoefe = new ArrayList<>();
		bahnhoefe.add(new Bahnhof(4711, "Test", 50d, 9d));
		bahnhoefe.add(new Bahnhof(4712, "Foo", 51d, 8d));
		
		final BahnhoefeGpxWriter writer = new BahnhoefeGpxWriter();
		final ByteArrayOutputStream entityStream = new ByteArrayOutputStream();
		writer.writeTo(bahnhoefe.iterator(), null, null, null, null, null, entityStream);
		
		final String gpx = entityStream.toString();
		MatcherAssert.assertThat(gpx, 
				CoreMatchers.is("<?xml version=\"1.0\" encoding=\"UTF-8\"?><gpx xmlns=\"http://www.topografix.com/GPX/1/1\" version=\"1.1\"><wpt lat=\"50.0\" lon=\"9.0\"><name>Test</name></wpt><wpt lat=\"51.0\" lon=\"8.0\"><name>Foo</name></wpt></gpx>"));
	}

}
