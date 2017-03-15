package github.pstorch.bahnhoefe.service.writer;

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
import java.util.Map;

@Produces(PhotographersTxtWriter.TEXT_PLAIN)
public class PhotographersTxtWriter implements MessageBodyWriter<Map<String, Long>> {

    public static final String TEXT_PLAIN = "text/plain";

    private static void photographerToCsv(final PrintWriter pw, final Map.Entry<String, Long> photographer) {
        pw.println(String.format("%s\t%s", Long.toString(photographer.getValue()),
                photographer.getKey()));
    }

    @Override
    public boolean isWriteable(final Class<?> type, final Type genericType, final Annotation[] annotations,
                               final MediaType mediaType) {
        return true;
    }

    @Override
    public long getSize(final Map<String, Long> photographers, final Class<?> type, final Type genericType,
                        final Annotation[] annotations, final MediaType mediaType) {
        return -1;
    }

    @Override
    public void writeTo(final Map<String, Long> t, final Class<?> type, final Type genericType,
                        final Annotation[] annotations, final MediaType mediaType, final MultivaluedMap<String, Object> httpHeaders,
                        final OutputStream entityStream) throws IOException, WebApplicationException {
        final PrintWriter pw = new PrintWriter(new OutputStreamWriter(entityStream, "UTF-8"));
        pw.println("count\tphotographer");
        t.entrySet().forEach(photographer -> photographerToCsv(pw, photographer));
        pw.flush();
    }

}
