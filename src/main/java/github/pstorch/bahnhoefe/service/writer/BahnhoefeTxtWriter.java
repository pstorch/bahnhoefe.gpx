package github.pstorch.bahnhoefe.service.writer;

import github.pstorch.bahnhoefe.service.model.Bahnhof;

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
import java.util.List;

@Produces(BahnhoefeTxtWriter.TEXT_PLAIN)
public class BahnhoefeTxtWriter implements MessageBodyWriter<List<Bahnhof>> {

    public static final String TEXT_PLAIN = "text/plain";

    private static void bahnhofToTxt(final PrintWriter pw, final Bahnhof bahnhof) {
        pw.println(String.format("%s\t%s\t%s\t%s\t%s\t10,10\t0,-10", Double.toString(bahnhof.getLat()),
                Double.toString(bahnhof.getLon()), bahnhof.getTitle(), bahnhof.getTitle(),
                bahnhof.hasPhoto() ? "gruenpunkt.png" : "rotpunkt.png"));
    }

    @Override
    public boolean isWriteable(final Class<?> type, final Type genericType, final Annotation[] annotations,
                               final MediaType mediaType) {
        return true;
    }

    @Override
    public long getSize(final List<Bahnhof> t, final Class<?> type, final Type genericType,
                        final Annotation[] annotations, final MediaType mediaType) {
        return -1;
    }

    @Override
    public void writeTo(final List<Bahnhof> t, final Class<?> type, final Type genericType,
                        final Annotation[] annotations, final MediaType mediaType, final MultivaluedMap<String, Object> httpHeaders,
                        final OutputStream entityStream) throws IOException, WebApplicationException {
        final PrintWriter pw = new PrintWriter(new OutputStreamWriter(entityStream, "UTF-8"));
        pw.println("lat	lon	title	description	icon	iconSize	iconOffset");
        t.forEach(bahnhof -> bahnhofToTxt(pw, bahnhof));
        pw.flush();
    }

}
