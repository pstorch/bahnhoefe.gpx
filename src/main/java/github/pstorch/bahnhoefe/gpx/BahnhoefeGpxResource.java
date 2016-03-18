package github.pstorch.bahnhoefe.gpx;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Path("/")
@Produces(MediaType.APPLICATION_ATOM_XML)
public class BahnhoefeGpxResource {

	private String bahnhoefeUrl;

	private String photosUrl;

	public BahnhoefeGpxResource(String bahnhoefeUrl, String photosUrl) {
		this.bahnhoefeUrl = bahnhoefeUrl;
		this.photosUrl = photosUrl;
	}

	@GET
	@Path("bahnhoefe.gpx")
	public Response getAll() {
		StreamingOutput stream = new StreamingOutput() {
			@Override
			public void write(OutputStream os) throws IOException, WebApplicationException {
				writeGpx(loadBahnhoefe().values().stream(), os);
			}
		};
		return Response.ok(stream).build();
	}

	@GET
	@Path("bahnhoefe-withPhoto.gpx")
	public Response getWithPhoto() {
		StreamingOutput stream = new StreamingOutput() {
			@Override
			public void write(OutputStream os) throws IOException, WebApplicationException {
				writeGpx(loadBahnhoefe().values().stream().filter(bahnhof -> bahnhof.hasPhoto()), os);
			}
		};
		return Response.ok(stream).build();
	}

	@GET
	@Path("bahnhoefe-withoutPhoto.gpx")
	public Response getWithoutPhoto() {
		StreamingOutput stream = new StreamingOutput() {
			@Override
			public void write(OutputStream os) throws IOException, WebApplicationException {
				writeGpx(loadBahnhoefe().values().stream().filter(bahnhof -> !bahnhof.hasPhoto()), os);
			}
		};
		return Response.ok(stream).build();
	}

	protected void writeGpx(Stream<Bahnhof> bahnhoefeStream, OutputStream os) throws IOException {
		XMLStreamWriter xmlw;
		try {
			xmlw = XMLOutputFactory.newInstance().createXMLStreamWriter(os, "UTF-8");
			xmlw.writeStartDocument("UTF-8", "1.0");
			xmlw.writeStartElement("gpx");
			xmlw.writeDefaultNamespace("http://www.topografix.com/GPX/1/1");
			xmlw.writeAttribute("version", "1.1");
			bahnhoefeStream.forEach(bahnhof -> bahnhofToXml(xmlw, bahnhof));
			xmlw.writeEndElement();
			xmlw.flush();
		} catch (Exception e) {
			throw new WebApplicationException(e);
		}
	}

	private Map<Integer, Bahnhof> loadBahnhoefe() throws MalformedURLException, IOException, JsonProcessingException {
		Map<Integer, Bahnhof> bahnhoefe = new HashMap<>();

		ObjectMapper mapper = new ObjectMapper();
		URL url = new URL(bahnhoefeUrl);
		try (InputStream is = url.openStream()) {
			JsonNode tree = mapper.readTree(is);
			JsonNode hits = tree.get("hits").get("hits");
			for (int i = 0; i < hits.size(); i++) {
				JsonNode hit = hits.get(i);
				JsonNode bahnhofJson = hit.get("_source");
				Bahnhof bahnhof = new Bahnhof(bahnhofJson.get("BahnhofNr").asInt(), bahnhofJson.get("title").asText(),
						bahnhofJson.get("lat").asDouble(), bahnhofJson.get("lon").asDouble());
				bahnhoefe.put(bahnhof.getId(), bahnhof);
			}
		}

		url = new URL(photosUrl);
		try (InputStream is = url.openStream()) {
			JsonNode tree = mapper.readTree(is);
			for (int i = 0; i < tree.size(); i++) {
				JsonNode bahnhofPhoto = tree.get(i);
				int bahnhofsNr = bahnhofPhoto.get("bahnhofsnr").asInt();
				Bahnhof bahnhof = bahnhoefe.get(bahnhofsNr);
				if (bahnhof != null) {
					bahnhof.setHasPhoto(true);
				}
			}
		}
		return bahnhoefe;
	}

	private static void bahnhofToXml(XMLStreamWriter xmlw, Bahnhof bahnhof) {
		try {
			xmlw.writeStartElement("wpt");
			xmlw.writeAttribute("lat", "" + bahnhof.getLat());
			xmlw.writeAttribute("lon", "" + bahnhof.getLon());
			xmlw.writeStartElement("name");
			xmlw.writeCharacters(bahnhof.getTitle());
			xmlw.writeEndElement();
			xmlw.writeEndElement();
		} catch (XMLStreamException e) {
			throw new RuntimeException(e);
		}
	}

}
