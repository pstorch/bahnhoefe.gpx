package org.railwaystations.rsapi.writer;

import org.railwaystations.rsapi.model.Statistic;
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

public class StatisticTxtWriter extends AbstractHttpMessageConverter<Statistic> {

    public StatisticTxtWriter() {
        super(MediaType.TEXT_PLAIN);
    }

    @Override
    protected boolean supports(final Class<?> clazz) {
        return Statistic.class.isAssignableFrom(clazz);
    }

    @Override
    protected Statistic readInternal(final Class<? extends Statistic> clazz, final HttpInputMessage inputMessage) throws HttpMessageNotReadableException {
        return null;
    }

    @Override
    protected void writeInternal(final Statistic statistic, final HttpOutputMessage outputMessage) throws IOException, HttpMessageNotWritableException {
        final PrintWriter pw = new PrintWriter(new OutputStreamWriter(outputMessage.getBody(), StandardCharsets.UTF_8));
        pw.println("name\tvalue");
        statisticToCsv(pw, "total", statistic.getTotal());
        statisticToCsv(pw, "withPhoto", statistic.getWithPhoto());
        statisticToCsv(pw, "withoutPhoto", statistic.getWithoutPhoto());
        statisticToCsv(pw, "photographers", statistic.getPhotographers());
        pw.flush();
    }

    private static void statisticToCsv(final PrintWriter pw, final String name, final int value) {
        pw.println(String.format("%s\t%s", name, value));
    }

}
