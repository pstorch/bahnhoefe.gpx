package org.railwaystations.api.writer;

import org.junit.jupiter.api.Test;
import org.railwaystations.api.model.Station;
import org.railwaystations.api.model.Coordinates;
import org.railwaystations.api.model.Photo;

import javax.ws.rs.WebApplicationException;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class StationsTxtWriterTest {

    @Test
    public void test() throws WebApplicationException, IOException {
        final List<Station> stations = new ArrayList<>();
        final Station.Key key4711 = new Station.Key("", "4711");
        stations.add(new Station(key4711, "Test", new Coordinates(50d, 9d), new Photo(key4711, null, "@pstorch", null, null, null)));
        stations.add(new Station(new Station.Key("","4712"), "Foo", new Coordinates(51d, 8d), null, null));

        final StationsTxtWriter writer = new StationsTxtWriter();
        final ByteArrayOutputStream entityStream = new ByteArrayOutputStream();
        writer.writeTo(stations, null, null, null, null, null, entityStream);

        final String txt = entityStream.toString("UTF-8");
        final String[] lines = txt.split("\n");
        assertThat(lines[0], is("lat\tlon\ttitle\tdescription\ticon\ticonSize\ticonOffset"));
        assertThat(lines[1], is("50.0\t9.0\tTest\tTest\tgruenpunkt.png\t10,10\t0,-10"));
        assertThat(lines[2], is("51.0\t8.0\tFoo\tFoo\trotpunkt.png\t10,10\t0,-10"));
    }

}
