package org.railwaystations.rsapi.writer;

import org.junit.jupiter.api.Test;
import org.railwaystations.rsapi.model.Coordinates;
import org.railwaystations.rsapi.model.Station;

import javax.ws.rs.WebApplicationException;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class StationsGpxWriterTest {

	@Test
	public void testWriteTo() throws WebApplicationException {
		final List<Station> stations = new ArrayList<>();
		stations.add(new Station(new Station.Key("de", "4711"), "Test", new Coordinates(50d, 9d), null, null, true));
		stations.add(new Station(new Station.Key("de", "4712"), "Foo", new Coordinates(51d, 8d), null, null, true));
		
		final StationsGpxWriter writer = new StationsGpxWriter();
		final ByteArrayOutputStream entityStream = new ByteArrayOutputStream();
		writer.writeTo(stations, null, null, null, null, null, entityStream);
		
		final String gpx = entityStream.toString(StandardCharsets.UTF_8);
		assertThat(gpx,
				is("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<gpx xmlns=\"http://www.topografix.com/GPX/1/1\" version=\"1.1\">\n<wpt lat=\"50.0\" lon=\"9.0\"><name>Test</name></wpt>\n<wpt lat=\"51.0\" lon=\"8.0\"><name>Foo</name></wpt>\n</gpx>"));
	}

}
