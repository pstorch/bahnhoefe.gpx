package org.railwaystations.rsapi.writer;

import org.junit.jupiter.api.Test;
import org.springframework.mock.http.MockHttpOutputMessage;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class PhotographersTxtWriterTest {

    @Test
    public void test() throws IOException {
        final Map<String, Long> photographers = new HashMap<>();
        photographers.put("@foo", 10L);
        photographers.put("@bar", 5L);

        final MockHttpOutputMessage outputMessage = new MockHttpOutputMessage();
        new PhotographersTxtWriter().writeInternal(photographers, outputMessage);

        final String txt = outputMessage.getBodyAsString(StandardCharsets.UTF_8);
        final String[] lines = txt.split("\n");
        assertThat(lines[0], is("count\tphotographer"));
        assertThat(lines[1], is("10\t@foo"));
        assertThat(lines[2], is("5\t@bar"));
    }

}
