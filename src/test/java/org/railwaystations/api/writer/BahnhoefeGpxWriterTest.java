package org.railwaystations.api.writer;

import org.railwaystations.api.model.Station;
import org.junit.Test;
import org.railwaystations.api.model.Coordinates;

import javax.ws.rs.WebApplicationException;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class BahnhoefeGpxWriterTest {

	@Test
	public void testWriteTo() throws WebApplicationException, IOException {
		final List<Station> bahnhoefe = new ArrayList<>();
		bahnhoefe.add(new Station(4711, "", "Test", new Coordinates(50d, 9d), null, null));
		bahnhoefe.add(new Station(4712, "", "Foo", new Coordinates(51d, 8d), null, null));
		
		final BahnhoefeGpxWriter writer = new BahnhoefeGpxWriter();
		final ByteArrayOutputStream entityStream = new ByteArrayOutputStream();
		writer.writeTo(bahnhoefe, null, null, null, null, null, entityStream);
		
		final String gpx = entityStream.toString("UTF-8");
		assertThat(gpx,
				is("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<service xmlns=\"http://www.topografix.com/GPX/1/1\" version=\"1.1\">\n<wpt lat=\"50.0\" lon=\"9.0\"><name>Test</name></wpt>\n<wpt lat=\"51.0\" lon=\"8.0\"><name>Foo</name></wpt>\n</service>"));
	}

}
