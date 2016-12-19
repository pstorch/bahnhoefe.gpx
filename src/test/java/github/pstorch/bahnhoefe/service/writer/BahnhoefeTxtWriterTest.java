package github.pstorch.bahnhoefe.service.writer;

import github.pstorch.bahnhoefe.service.Bahnhof;
import org.junit.Test;

import javax.ws.rs.WebApplicationException;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class BahnhoefeTxtWriterTest {

    @Test
    public void test() throws WebApplicationException, IOException {
        final List<Bahnhof> bahnhoefe = new ArrayList<>();
        bahnhoefe.add(new Bahnhof(4711, "Test", 50d, 9d, "@pstorch"));
        bahnhoefe.add(new Bahnhof(4712, "Foo", 51d, 8d, null));

        final BahnhoefeTxtWriter writer = new BahnhoefeTxtWriter();
        final ByteArrayOutputStream entityStream = new ByteArrayOutputStream();
        writer.writeTo(bahnhoefe.iterator(), null, null, null, null, null, entityStream);

        final String txt = entityStream.toString("UTF-8");
        final String[] lines = txt.split("\n");
        assertThat(lines[0], is("lat\tlon\ttitle\tdescription\ticon\ticonSize\ticonOffset"));
        assertThat(lines[1], is("50.0\t9.0\tTest\tTest\tgruenpunkt.png\t10,10\t0,-10"));
        assertThat(lines[2], is("51.0\t8.0\tFoo\tFoo\trotpunkt.png\t10,10\t0,-10"));
    }

}