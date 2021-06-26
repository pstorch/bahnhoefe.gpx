package org.railwaystations.rsapi.writer;

import org.railwaystations.rsapi.model.Station;
import org.reactivestreams.Publisher;
import org.springframework.core.ResolvableType;
import org.springframework.core.codec.Encoder;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.http.MediaType;
import org.springframework.util.MimeType;
import reactor.core.publisher.Flux;

import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

public class StationsTxtWriter implements Encoder<Station> {

    private static void stationToTxt(final PrintWriter pw, final Station station) {
        pw.println(String.format("%s\t%s\t%s\t%s\t%s\t10,10\t0,-10", station.getCoordinates().getLat(),
                station.getCoordinates().getLon(), station.getTitle(), station.getTitle(),
                station.hasPhoto() ? "gruenpunkt.png" : "rotpunkt.png"));
    }

    @Override
    public boolean canEncode(final ResolvableType elementType, final MimeType mimeType) {
        return getEncodableMimeTypes().contains(mimeType) && elementType.;
    }

    @Override
    public Flux<DataBuffer> encode(final Publisher<? extends Station> inputStream, final DataBufferFactory bufferFactory, final ResolvableType elementType, final MimeType mimeType, final Map<String, Object> hints) {
        final PrintWriter pw = new PrintWriter(new OutputStreamWriter(entityStream, StandardCharsets.UTF_8));
        pw.println("lat	lon	title	description	icon	iconSize	iconOffset");
        t.forEach(station -> stationToTxt(pw, station));
        pw.flush();

        return null;
    }

    @Override
    public List<MimeType> getEncodableMimeTypes() {
        return List.of(MediaType.TEXT_PLAIN);
    }
}
