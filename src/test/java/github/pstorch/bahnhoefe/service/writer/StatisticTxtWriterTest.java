package github.pstorch.bahnhoefe.service.writer;

import github.pstorch.bahnhoefe.service.model.Statistic;
import org.junit.Test;

import javax.ws.rs.WebApplicationException;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class StatisticTxtWriterTest {

    @Test
    public void test() throws WebApplicationException, IOException {
        final Statistic stat = new Statistic(1500, 500, 1000, 20);

        final StatisticTxtWriter writer = new StatisticTxtWriter();
        final ByteArrayOutputStream entityStream = new ByteArrayOutputStream();
        writer.writeTo(stat, null, null, null, null, null, entityStream);

        final String txt = entityStream.toString("UTF-8");
        final String[] lines = txt.split("\n");
        assertThat(lines[0], is("name\tvalue"));
        assertThat(lines[1], is("total\t1500"));
        assertThat(lines[2], is("withPhoto\t500"));
        assertThat(lines[3], is("withoutPhoto\t1000"));
        assertThat(lines[4], is("photographers\t20"));
    }

}
