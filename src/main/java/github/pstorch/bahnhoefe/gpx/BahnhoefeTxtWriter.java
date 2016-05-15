package github.pstorch.bahnhoefe.gpx;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Iterator;

import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyWriter;

@Produces(BahnhoefeTxtWriter.TEXT_PLAIN)
public class BahnhoefeTxtWriter implements MessageBodyWriter<Iterator<Bahnhof>> {

	public static final String TEXT_PLAIN = "text/plain";

	@Override
	public boolean isWriteable(final Class<?> type, final Type genericType, final Annotation[] annotations, final MediaType mediaType) {
		return true;
	}

	@Override
	public long getSize(final Iterator<Bahnhof> t, final Class<?> type, final Type genericType, final Annotation[] annotations,
			final MediaType mediaType) {
		return -1;
	}

	@Override
	public void writeTo(final Iterator<Bahnhof> t, final Class<?> type, final Type genericType, final Annotation[] annotations,
			final MediaType mediaType, final MultivaluedMap<String, Object> httpHeaders, final OutputStream entityStream)
					throws IOException, WebApplicationException {
		final PrintWriter pw = new PrintWriter(new OutputStreamWriter(entityStream, "UTF-8"));
		pw.println("lat	lon	title	description	icon	iconSize	iconOffset");
		t.forEachRemaining(bahnhof -> bahnhofToTxt(pw, bahnhof));
		pw.flush();
	}

	private static void bahnhofToTxt(final PrintWriter pw, final Bahnhof bahnhof) {
		pw.println(String.format("%s\t%s\t%s\t%s\trotpunkt.png	10,10	0,-10", Double.toString(bahnhof.getLat()), Double.toString(bahnhof.getLon()), bahnhof.getTitle(), bahnhof.getTitle()));
	}

}