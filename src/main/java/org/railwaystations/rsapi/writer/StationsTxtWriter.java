package org.railwaystations.rsapi.writer;

import org.railwaystations.rsapi.model.Station;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.AbstractHttpMessageConverter;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class StationsTxtWriter extends AbstractHttpMessageConverter<List<Station>> {

    public StationsTxtWriter() {
        super(MediaType.TEXT_PLAIN);
    }

    private static void stationToTxt(final PrintWriter pw, final Station station) {
        pw.println(String.format("%s\t%s\t%s\t%s\t%s\t10,10\t0,-10", station.getCoordinates().getLat(),
                station.getCoordinates().getLon(), station.getTitle(), station.getTitle(),
                station.hasPhoto() ? "gruenpunkt.png" : "rotpunkt.png"));
    }

    @Override
    protected boolean supports(final Class<?> clazz) {
        return List.class.isAssignableFrom(clazz);
    }

    @Override
    protected List<Station> readInternal(final Class<? extends List<Station>> clazz, final HttpInputMessage inputMessage) throws HttpMessageNotReadableException {
        return null;
    }

    @Override
    protected void writeInternal(final List<Station> stations, final HttpOutputMessage outputMessage) throws IOException, HttpMessageNotWritableException {
        final PrintWriter pw = new PrintWriter(new OutputStreamWriter(outputMessage.getBody(), StandardCharsets.UTF_8));
        pw.println("lat	lon	title	description	icon	iconSize	iconOffset");
        stations.forEach(station -> stationToTxt(pw, station));
        pw.flush();
    }

}
