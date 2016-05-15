package github.pstorch.bahnhoefe.gpx;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.WebApplicationException;

import org.hamcrest.CoreMatchers;
import org.hamcrest.MatcherAssert;
import org.junit.Test;

public class BahnhoefeTxtWriterTest {

	@Test
	public void test() throws WebApplicationException, IOException {
		final List<Bahnhof> bahnhoefe = new ArrayList<>();
		bahnhoefe.add(new Bahnhof(4711, "Test", 50d, 9d));
		bahnhoefe.add(new Bahnhof(4712, "Foo", 51d, 8d));
		
		final BahnhoefeTxtWriter writer = new BahnhoefeTxtWriter();
		final ByteArrayOutputStream entityStream = new ByteArrayOutputStream();
		writer.writeTo(bahnhoefe.iterator(), null, null, null, null, null, entityStream);
		
		final String txt = entityStream.toString();
		final String[] lines = txt.split("\n");
		MatcherAssert.assertThat(lines[0], CoreMatchers.is("lat\tlon\ttitle\tdescription\ticon\ticonSize\ticonOffset"));
		MatcherAssert.assertThat(lines[1], CoreMatchers.is("50.0\t9.0\tTest\tTest\trotpunkt.png\t10,10\t0,-10"));
		MatcherAssert.assertThat(lines[2], CoreMatchers.is("51.0\t8.0\tFoo\tFoo\trotpunkt.png\t10,10\t0,-10"));
	}

}
