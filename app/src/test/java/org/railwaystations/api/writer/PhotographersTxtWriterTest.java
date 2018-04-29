package org.railwaystations.api.writer;

import org.junit.jupiter.api.Test;

import javax.ws.rs.WebApplicationException;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class PhotographersTxtWriterTest {

    @org.junit.jupiter.api.Test
    public void test() throws WebApplicationException, IOException {
        final Map<String, Long> photographers = new HashMap<>();
        photographers.put("@foo", 10L);
        photographers.put("@bar", 5L);

        final PhotographersTxtWriter writer = new PhotographersTxtWriter();
        final ByteArrayOutputStream entityStream = new ByteArrayOutputStream();
        writer.writeTo(photographers, null, null, null, null, null, entityStream);

        final String txt = entityStream.toString("UTF-8");
        final String[] lines = txt.split("\n");
        assertThat(lines[0], is("count\tphotographer"));
        assertThat(lines[1], is("10\t@foo"));
        assertThat(lines[2], is("5\t@bar"));
    }

}
