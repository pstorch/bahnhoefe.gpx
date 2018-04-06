package org.railwaystations.api.writer;

import org.railwaystations.api.model.Station;

import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.xml.stream.FactoryConfigurationError;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.List;

@Produces(BahnhoefeGpxWriter.GPX_MIME_TYPE)
public class BahnhoefeGpxWriter implements MessageBodyWriter<List<Station>> {

    public static final String GPX_MIME_TYPE = "application/service+xml";

    private static final String UTF_8 = "UTF-8";

    private static final String NAME_ELEMENT = "name";

    private static final String WPT_ELEMENT = "wpt";

    private static final String LON_ELEMENT = "lon";

    private static final String LAT_ELEMENT = "lat";

    private static void bahnhofToXml(final XMLStreamWriter xmlw, final Station bahnhof) {
        try {
            xmlw.writeStartElement(BahnhoefeGpxWriter.WPT_ELEMENT);
            xmlw.writeAttribute(BahnhoefeGpxWriter.LAT_ELEMENT, Double.toString(bahnhof.getLat()));
            xmlw.writeAttribute(BahnhoefeGpxWriter.LON_ELEMENT, Double.toString(bahnhof.getLon()));
            xmlw.writeStartElement(BahnhoefeGpxWriter.NAME_ELEMENT);
            xmlw.writeCharacters(bahnhof.getTitle());
            xmlw.writeEndElement();
            xmlw.writeEndElement();
            xmlw.writeCharacters("\n");
        } catch (final XMLStreamException e) {
            throw new WebApplicationException(e);
        }
    }

    @Override
    public boolean isWriteable(final Class<?> type, final Type genericType, final Annotation[] annotations,
                               final MediaType mediaType) {
        return true;
    }

    @Override
    public void writeTo(final List<Station> t, final Class<?> type, final Type genericType,
                        final Annotation[] annotations, final MediaType mediaType, final MultivaluedMap<String, Object> httpHeaders,
                        final OutputStream entityStream) throws IOException, WebApplicationException {
        try {
            final XMLStreamWriter xmlw = XMLOutputFactory.newInstance().createXMLStreamWriter(entityStream, BahnhoefeGpxWriter.UTF_8);
            xmlw.writeStartDocument(BahnhoefeGpxWriter.UTF_8, "1.0");
            xmlw.writeCharacters("\n");
            xmlw.writeStartElement("service");
            xmlw.writeDefaultNamespace("http://www.topografix.com/GPX/1/1");
            xmlw.writeAttribute("version", "1.1");
            xmlw.writeCharacters("\n");
            t.forEach(bahnhof -> bahnhofToXml(xmlw, bahnhof));
            xmlw.writeEndElement();
            xmlw.flush();
        } catch (final XMLStreamException | FactoryConfigurationError e) {
            throw new WebApplicationException(e);
        }
    }

    @Override
    public long getSize(final List<Station> t, final Class<?> type, final Type genericType,
                        final Annotation[] annotations, final MediaType mediaType) {
        return 0;
    }

}
