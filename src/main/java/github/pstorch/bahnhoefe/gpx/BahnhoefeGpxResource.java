package github.pstorch.bahnhoefe.gpx;

import java.io.IOException;
import java.io.OutputStream;
import java.util.stream.Stream;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;
import javax.xml.stream.FactoryConfigurationError;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

@Path("/")
@Produces(MediaType.APPLICATION_ATOM_XML)
@SuppressWarnings({"PMD.AvoidThrowingRawExceptionTypes", "PMD.AvoidCatchingGenericException"})
public class BahnhoefeGpxResource {

	private static final String UTF_8 = "UTF-8";

	private static final String NAME_ELEMENT = "name";

	private static final String WPT_ELEMENT = "wpt";

	private static final String LON_ELEMENT = "lon";

	private static final String LAT_ELEMENT = "lat";

	private final BahnhoefeLoader loader;

	public BahnhoefeGpxResource(final BahnhoefeLoader loader) {
		this.loader = loader;
	}

	@GET
	@Path("bahnhoefe.gpx")
	public Response getAll() {
		final StreamingOutput stream = new StreamingOutput() {
			@Override
			public void write(final OutputStream os) throws IOException, WebApplicationException {
				writeGpx(loader.loadBahnhoefe().values().stream(), os);
			}
		};
		return Response.ok(stream).build();
	}

	@GET
	@Path("bahnhoefe-withPhoto.gpx")
	public Response getWithPhoto() {
		final StreamingOutput stream = new StreamingOutput() {
			@Override
			public void write(final OutputStream os) throws IOException, WebApplicationException {
				writeGpx(loader.loadBahnhoefe().values().stream().filter(bahnhof -> bahnhof.hasPhoto()), os);
			}
		};
		return Response.ok(stream).build();
	}

	@GET
	@Path("bahnhoefe-withoutPhoto.gpx")
	public Response getWithoutPhoto() {
		final StreamingOutput stream = new StreamingOutput() {
			@Override
			public void write(final OutputStream os) throws IOException, WebApplicationException {
				writeGpx(loader.loadBahnhoefe().values().stream().filter(bahnhof -> !bahnhof.hasPhoto()), os);
			}
		};
		return Response.ok(stream).build();
	}

	protected void writeGpx(final Stream<Bahnhof> bahnhoefeStream, final OutputStream os) throws IOException {
		XMLStreamWriter xmlw;
		try {
			xmlw = XMLOutputFactory.newInstance().createXMLStreamWriter(os, BahnhoefeGpxResource.UTF_8);
			xmlw.writeStartDocument(BahnhoefeGpxResource.UTF_8, "1.0");
			xmlw.writeStartElement("gpx");
			xmlw.writeDefaultNamespace("http://www.topografix.com/GPX/1/1");
			xmlw.writeAttribute("version", "1.1");
			bahnhoefeStream.forEach(bahnhof -> bahnhofToXml(xmlw, bahnhof));
			xmlw.writeEndElement();
			xmlw.flush();
		} catch (final XMLStreamException | FactoryConfigurationError e) {
			throw new WebApplicationException(e);
		}
	}


	private static void bahnhofToXml(final XMLStreamWriter xmlw, final Bahnhof bahnhof) {
		try {
			xmlw.writeStartElement(BahnhoefeGpxResource.WPT_ELEMENT);
			xmlw.writeAttribute(BahnhoefeGpxResource.LAT_ELEMENT, Double.toString(bahnhof.getLat()));
			xmlw.writeAttribute(BahnhoefeGpxResource.LON_ELEMENT, Double.toString(bahnhof.getLon()));
			xmlw.writeStartElement(BahnhoefeGpxResource.NAME_ELEMENT);
			xmlw.writeCharacters(bahnhof.getTitle());
			xmlw.writeEndElement();
			xmlw.writeEndElement();
		} catch (final XMLStreamException e) {
			throw new RuntimeException(e);
		}
	}

}
