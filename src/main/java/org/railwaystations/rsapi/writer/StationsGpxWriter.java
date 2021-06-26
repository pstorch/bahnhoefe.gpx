package org.railwaystations.rsapi.writer;

import org.railwaystations.rsapi.model.Station;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.AbstractHttpMessageConverter;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import java.io.IOException;
import java.util.List;
import java.util.Map;

public class StationsGpxWriter extends AbstractHttpMessageConverter<List<Station>> {

    private static final String UTF_8 = "UTF-8";

    private static final String NAME_ELEMENT = "name";

    private static final String WPT_ELEMENT = "wpt";

    private static final String LON_ELEMENT = "lon";

    private static final String LAT_ELEMENT = "lat";

    public StationsGpxWriter() {
        super(new MediaType("application", "gpx+xml"));
    }

    @Override
    protected boolean supports(final Class<?> clazz) {
        return List.class.isAssignableFrom(clazz);
    }

    @Override
    protected List<Station> readInternal(final Class<? extends List<Station>> clazz, final HttpInputMessage inputMessage) throws IOException, HttpMessageNotReadableException {
        return null;
    }

    @Override
    protected void writeInternal(final List<Station> stations, final HttpOutputMessage outputMessage) throws IOException, HttpMessageNotWritableException {
        final XMLStreamWriter xmlw;
        try {
            xmlw = XMLOutputFactory.newInstance().createXMLStreamWriter(outputMessage.getBody(), StationsGpxWriter.UTF_8);
            xmlw.writeStartDocument(StationsGpxWriter.UTF_8, "1.0");
            xmlw.writeCharacters("\n");
            xmlw.writeStartElement("gpx");
            xmlw.writeDefaultNamespace("http://www.topografix.com/GPX/1/1");
            xmlw.writeAttribute("version", "1.1");
            xmlw.writeCharacters("\n");
            stations.forEach(station -> stationToXml(xmlw, station));
            xmlw.writeEndElement();
            xmlw.flush();
        } catch (final XMLStreamException e) {
            throw new HttpMessageNotWritableException("Error converting a Station to gpx", e);
        }
    }

    private static void stationToXml(final XMLStreamWriter xmlw, final Station station) {
        try {
            xmlw.writeStartElement(StationsGpxWriter.WPT_ELEMENT);
            xmlw.writeAttribute(StationsGpxWriter.LAT_ELEMENT, Double.toString(station.getCoordinates().getLat()));
            xmlw.writeAttribute(StationsGpxWriter.LON_ELEMENT, Double.toString(station.getCoordinates().getLon()));
            xmlw.writeStartElement(StationsGpxWriter.NAME_ELEMENT);
            xmlw.writeCharacters(station.getTitle());
            xmlw.writeEndElement();
            xmlw.writeEndElement();
            xmlw.writeCharacters("\n");
        } catch (final XMLStreamException e) {
            throw new HttpMessageNotWritableException("Error converting a Station to gpx", e);
        }
    }

}
