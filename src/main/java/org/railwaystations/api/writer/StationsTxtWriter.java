package org.railwaystations.api.writer;

import org.railwaystations.api.model.Station;

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

@Produces(StationsTxtWriter.TEXT_PLAIN)
public class StationsTxtWriter implements MessageBodyWriter<List<Station>> {

    public static final String TEXT_PLAIN = "text/plain";

    private static void stationToTxt(final PrintWriter pw, final Station station) {
        pw.println(String.format("%s\t%s\t%s\t%s\t%s\t10,10\t0,-10", Double.toString(station.getCoordinates().getLat()),
                Double.toString(station.getCoordinates().getLon()), station.getTitle(), station.getTitle(),
                station.hasPhoto() ? "gruenpunkt.png" : "rotpunkt.png"));
    }

    @Override
    public boolean isWriteable(final Class<?> type, final Type genericType, final Annotation[] annotations,
                               final MediaType mediaType) {
        return true;
    }

    @Override
    public long getSize(final List<Station> t, final Class<?> type, final Type genericType,
                        final Annotation[] annotations, final MediaType mediaType) {
        return -1;
    }

    @Override
    public void writeTo(final List<Station> t, final Class<?> type, final Type genericType,
                        final Annotation[] annotations, final MediaType mediaType, final MultivaluedMap<String, Object> httpHeaders,
                        final OutputStream entityStream) throws IOException, WebApplicationException {
        final PrintWriter pw = new PrintWriter(new OutputStreamWriter(entityStream, "UTF-8"));
        pw.println("lat	lon	title	description	icon	iconSize	iconOffset");
        t.forEach(station -> stationToTxt(pw, station));
        pw.flush();
    }

}
