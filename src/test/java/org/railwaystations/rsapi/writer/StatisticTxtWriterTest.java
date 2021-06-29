package org.railwaystations.rsapi.writer;

import org.junit.jupiter.api.Test;
import org.railwaystations.rsapi.model.Statistic;
import org.springframework.mock.http.MockHttpOutputMessage;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class StatisticTxtWriterTest {

    @Test
    public void test() throws IOException {
        final Statistic stat = new Statistic(1500, 500, 20);


        final MockHttpOutputMessage outputMessage = new MockHttpOutputMessage();
        new StatisticTxtWriter().writeInternal(stat, outputMessage);

        final String txt = outputMessage.getBodyAsString(StandardCharsets.UTF_8);
        final String[] lines = txt.split("\n");
        assertThat(lines[0], is("name\tvalue"));
        assertThat(lines[1], is("total\t1500"));
        assertThat(lines[2], is("withPhoto\t500"));
        assertThat(lines[3], is("withoutPhoto\t1000"));
        assertThat(lines[4], is("photographers\t20"));
    }

}
