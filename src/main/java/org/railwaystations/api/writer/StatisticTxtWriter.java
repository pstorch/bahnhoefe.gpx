package org.railwaystations.api.writer;

import org.railwaystations.api.model.Statistic;

import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

@Produces(StatisticTxtWriter.TEXT_PLAIN)
public class StatisticTxtWriter implements MessageBodyWriter<Statistic> {

    public static final String TEXT_PLAIN = "text/plain";

    private static void statisticToCsv(final PrintWriter pw, final String name, final int value) {
        pw.println(String.format("%s\t%s", name, value));
    }

    @Override
    public boolean isWriteable(final Class<?> type, final Type genericType, final Annotation[] annotations,
                               final MediaType mediaType) {
        return true;
    }

    @Override
    public long getSize(final Statistic statistic, final Class<?> type, final Type genericType,
                        final Annotation[] annotations, final MediaType mediaType) {
        return -1;
    }

    @Override
    public void writeTo(final Statistic t, final Class<?> type, final Type genericType,
                        final Annotation[] annotations, final MediaType mediaType, final MultivaluedMap<String, Object> httpHeaders,
                        final OutputStream entityStream) throws IOException, WebApplicationException {
        final PrintWriter pw = new PrintWriter(new OutputStreamWriter(entityStream, "UTF-8"));
        pw.println("name\tvalue");
        statisticToCsv(pw, "total", t.getTotal());
        statisticToCsv(pw, "withPhoto", t.getWithPhoto());
        statisticToCsv(pw, "withoutPhoto", t.getWithoutPhoto());
        statisticToCsv(pw, "photographers", t.getPhotographers());
        pw.flush();
    }

}
