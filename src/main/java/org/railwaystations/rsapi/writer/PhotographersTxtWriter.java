package org.railwaystations.rsapi.writer;

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
import java.util.Map;

public class PhotographersTxtWriter extends AbstractHttpMessageConverter<Map<String, Long>> {

    public PhotographersTxtWriter() {
        super(MediaType.TEXT_PLAIN);
    }

    private static void photographerToCsv(final PrintWriter pw, final Map.Entry<String, Long> photographer) {
        pw.println(String.format("%s\t%s", photographer.getValue(),
                photographer.getKey()));
    }

    @Override
    protected boolean supports(final Class<?> clazz) {
        return Map.class.isAssignableFrom(clazz);
    }

    @Override
    protected Map<String, Long> readInternal(final Class<? extends Map<String, Long>> clazz, final HttpInputMessage inputMessage) throws IOException, HttpMessageNotReadableException {
        return null;
    }

    @Override
    protected void writeInternal(final Map<String, Long> stringLongMap, final HttpOutputMessage outputMessage)
            throws IOException, HttpMessageNotWritableException {
        final PrintWriter pw = new PrintWriter(new OutputStreamWriter(outputMessage.getBody(), StandardCharsets.UTF_8));
        pw.println("count\tphotographer");
        stringLongMap.entrySet().forEach(photographer -> photographerToCsv(pw, photographer));
        pw.flush();
    }

}
