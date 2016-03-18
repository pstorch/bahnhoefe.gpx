package github.pstorch.bahnhoefe.gpx;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Bahnhoefe GPX
 */
public class App {

	public static void main(String[] args) throws Exception {
		Map<Integer, Bahnhof> bahnhoefe = new HashMap<>();

		ObjectMapper mapper = new ObjectMapper();
		URL url = new URL("http://www.xn--deutschlands-bahnhfe-lbc.de/bahnhoefe/_search?size=6000");
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

		url = new URL("http://www.deutschlands-bahnhoefe.org/bahnhofsfotos.json");
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

		File gpxFile = new File("out.gpx");
		XMLStreamWriter xmlw = XMLOutputFactory.newInstance().createXMLStreamWriter(new FileOutputStream(gpxFile));
		xmlw.writeStartElement("gpx");
		xmlw.writeDefaultNamespace("http://www.topografix.com/GPX/1/1");
		xmlw.writeAttribute("version", "1.1");
		bahnhoefe.values().stream().forEach(bahnhof -> toXml(xmlw, bahnhof));
		xmlw.writeEndElement(); 
		xmlw.flush();
	}
	
	private static void toXml(XMLStreamWriter xmlw, Bahnhof bahnhof) {
		try {
			xmlw.writeStartElement("wpt");
			xmlw.writeAttribute("name", bahnhof.getTitle());
			xmlw.writeAttribute("lat", "" + bahnhof.getLat());
			xmlw.writeAttribute("lon", "" + bahnhof.getLon());
			xmlw.writeEndElement();
		} catch (XMLStreamException e) {
			throw new RuntimeException(e);
		}
	}
}
